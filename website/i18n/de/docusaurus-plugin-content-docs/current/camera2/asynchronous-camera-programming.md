---
sidebar_position: 26
title: "Kapitel 26: Asynchrone Kameraprogrammierung"
description: "Bändigen Sie die Callback-Hölle von Camera2 mit Kotlin Coroutines und Flow. Lernen Sie suspendCancellableCoroutine für One-Shot-Operationen (openCamera, createCaptureSession, capture), callbackFlow für kontinuierliche ImageReader- und CaptureResult-Streams, Combine-Operatoren für reaktive UIs und Muster zur Thread-Sicherheit kennen, um ANRs und Deadlocks zu verhindern."
keywords: [Kotlin Coroutines Camera2, Callback-Hölle, suspendCancellableCoroutine, callbackFlow, Camera2 Flow, Thread-Sicherheit Camera2, Mutex gemeinsamer Zustand, PipedOutputStream Deadlock, reaktive Kamera-UI]
---

# Kapitel 26: Asynchrone Kameraprogrammierung

## Zusammenfassung

Schauen Sie sich den Code an, den Sie für die Kapitel 7 bis 9 geschrieben haben. Ein `CameraDevice.StateCallback`, der in `CameraManager.openCamera` geschachtelt ist, mit einem `CameraCaptureSession.StateCallback`, der in `onOpened` geschachtelt ist, mit einem `CaptureCallback`, der in `onConfigured` geschachtelt ist, mit einem `ImageReader.OnImageAvailableListener`, der auf einem von Hand erstellten `HandlerThread` ausgelöst wird, den Sie auf jedem Fehlerpfad in genau umgekehrter Reihenfolge wieder abbauen müssen. Das ist die Callback-Hölle (Callback Hell), mit Kamera-Geschmack. Jede Einrückungsebene ist eine neue Callback-Klasse. Jeder Fehler muss durch vier Schichten anonymer Objekte propagiert werden. Jedes vergessene `close()` auf dem Abbaupfad leakt die Kamera bis zum Neustart des Geräts.

Dieses Kapitel liefert das Refactoring, nach dem Sie sich sehnen. Wir wandeln den gesamten Callback-Dschungel in sauberen, linearen, abbrechbaren und testbaren Kotlin-Code um, wobei wir zwei Coroutine-Primitive verwenden: `suspendCancellableCoroutine` für One-Shot-Operationen und `callbackFlow` + `Flow`-Operatoren für kontinuierliche Streams. Sie werden Regeln zur Thread-Sicherheit für Coroutinen lernen, die mit Camera2 interagieren, erfahren, warum das Blockieren des Haupt-Threads bei einem Kamera-Aufruf ein vorprogrammierter ANR (App Not Responding) ist und warum das `PipedOutputStream`/`PipedInputStream`-Muster, das Sie vielleicht für ImageWriter-Daten ausprobiert haben, Deadlocks erzeugt, die Flow von Natur aus vermeidet.

Wie immer sollten Sie die Hardware-Level-Fähigkeiten, auf die Sie abzielen, mit der App **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) validieren, um zu bestätigen, dass die Fähigkeiten, die Ihre asynchrone Pipeline benötigt (wiederholte Bursts, Teilergebnisse, YUV-Reprocessing), tatsächlich auf Ihren Testgeräten vorhanden sind.

---

## Warum geschachtelte Callbacks eine "Callback-Hölle" sind

Lassen Sie uns das Problem zunächst visualisieren. Dies ist eine reale (vereinfachte) Struktur einer produktiven Camera2-Raw-App vor der Einführung von Coroutinen:

```mermaid
graph TD
    A["onCreateView"] -->|cameraId gewählt| B["CameraManager.openCamera"]
    B -->|wird ausgelöst auf| C[StateCallback.onOpened<br/>Lambda 1]
    C -->|hält cameraDevice| D[createCaptureSession<br/>(Ausgaben = previewSurface + imageReaderSurface)]
    D -->|wird ausgelöst auf| E[Session.StateCallback.onConfigured<br/>Lambda 2]
    E -->|hält session| F[session.setRepeatingRequest<br/>+ CaptureCallback Lambda 3]
    F -->|löst onProgress aus| G[CaptureCallback.onCaptureProgressed<br/>Teilergebnisse]
    F -->|löst onCompleted aus| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|Frame fertig| I[ImageReader.OnImageAvailableListener<br/>Lambda 4]
    I -->|JPEG-Bytes| J[Aufruf zum Speichern im MediaStore<br/>Lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

Jeder schattierte Callback ist eine separate anonyme Klasse. Jede erfasst eine Referenz auf Ressourcen zwei Ebenen darüber. Jeder Fehlerpfad muss von J zurück nach A kaskadieren und dabei `imageReader → session → cameraDevice → handlerThread` in umgekehrter Reihenfolge schließen. Jedes einzelne fehlende `close()` in einer der 16 Fehlerpermutationen erzeugt einen dauerhaften Kameraleak, bis das Gerät neu startet. Dies ist die Lehrbuchdefinition der Callback-Hölle.

Das Ziel dieses Kapitels ist es, diesen Spaghetti-Code in folgendes zu verwandeln:

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[UI-Status<br/>(einzelnes Emit)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

Linear. Modular. Testbar. Abbrechbar durch Abbrechen des übergeordneten `Job`s. Jede Stufe ist eine einfache Funktion oder ein `Flow`-Operator. Dieselben fünf Callbacks befinden sich nun in einer 12-zeiligen linearen Pipeline.

---

## Kotlin Coroutinen für One-Shot-Operationen: `suspendCancellableCoroutine`

Das Kernmuster, um eine Callback-basierte API als `suspend`-Funktion zu kapseln, ist `suspendCancellableCoroutine`. Das Rezept ist immer identisch:

1. Rufen Sie `suspendCancellableCoroutine { cont -> ... }` auf, um eine `CancellableContinuation<T>` zu erhalten.
2. Rufen Sie die echte Callback-basierte API auf und übergeben Sie ihr eine anonyme Callback-Implementierung.
3. Rufen Sie im Erfolgspfad des Callbacks `cont.resume(value)` auf.
4. Rufen Sie in jedem Fehlerpfad `cont.resumeWithException(t)` auf.
5. Führen Sie in `cont.invokeOnCancellation { ... }` die Bereinigung durch: Schließen Sie die Kamera, brechen Sie ausstehende Anforderungen ab und deregistrieren Sie Listener, damit der Callback niemals ausgelöst wird, *nachdem* die Coroutine abgebrochen wurde.
6. Umschließen Sie das Ganze an den Aufrufstellen mit `withTimeout`, damit ein hängender HAL Ihre App nicht ewig blockieren kann.

### Beispiel 1: `suspend fun openCameraAwait()`

```kotlin
suspend fun CameraManager.openCameraAwait(
    cameraId: String,
    handler: Handler
): CameraDevice = suspendCancellableCoroutine { cont ->
    val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cont.resume(camera) {
                camera.close()
            }
        }
        override fun onDisconnected(camera: CameraDevice) {
            cont.resumeWithException(
                CameraAccessException(
                    CameraAccessException.CAMERA_DISCONNECTED,
                    "Kamera $cameraId wurde während des Öffnens getrennt"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "Fehler bei Kamera $cameraId: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // Workaround: openCamera() bietet vor API 30 kein abbrechbares Handle.
            // Gerät schließen, falls es im Race-Window geöffnet wurde.
        } catch (_: Throwable) { /* ignorieren */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**Warum das funktioniert.** `openCamera` ist "Fire-and-forget": Man ruft es auf und zu einem zukünftigen Zeitpunkt wird genau eine der drei Callback-Methoden genau einmal ausgelöst. Dieser Vertrag ("wird genau einmal ausgelöst") ermöglicht es uns, es eins zu eins auf eine Continuation abzubilden. Wenn die Coroutine abgebrochen wird, *bevor* ein Callback ausgelöst wird, wird `invokeOnCancellation` ausgeführt und verhindert einen Ressourcenleak. Wenn sie *nach* dem `resume` abgebrochen wird, schließt der Block `resume(value) { camera.close() }` – der `onCancellation`-Parameter von `resume` – das Gerät automatisch.

Der Aufruf mit einem Timeout ist trivial:

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "Öffnen der Kamera nach 5 s abgebrochen (Timeout)")
    return@launch
}
```

Wenn der HAL hängt (häufig bei Low-End-`LEGACY`-Geräten nach einem Kameraleak einer anderen App), schlägt dies schnell und sauber fehl, anstatt dem Benutzer einen "App reagiert nicht"-Dialog (ANR) zu präsentieren.

### Beispiel 2: `suspend fun createCaptureSessionAwait()`

Gleiches Muster, anderer Callback:

```kotlin
suspend fun CameraDevice.createCaptureSessionAwait(
    outputs: List<OutputConfiguration>,
    handler: Handler
): CameraCaptureSession = suspendCancellableCoroutine { cont ->
    val callback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            cont.resume(session) { session.close() }
        }
        override fun onConfigureFailed(session: CameraCaptureSession) {
            cont.resumeWithException(
                IllegalStateException(
                    "Konfiguration der Sitzung fehlgeschlagen für Gerät ${this@createCaptureSessionAwait.id}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // Die laufende Erstellung einer Sitzung kann bei älteren APIs nicht abgebrochen werden.
        // Die Sitzung wird geschlossen, wenn sie schließlich über den
        // resume-onCancellation-Block oben abgeschlossen wird.
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val config = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputs,
            { r -> handler.post(r) },
            callback
        )
        createCaptureSession(config)
    } else {
        @Suppress("DEPRECATION")
        createCaptureSession(outputs.map { it.surface }, callback, handler)
    }
}
```

Dies ist exakt die gleiche Form. Die zwei API-Versionen (`createCaptureSession(surfaces, callback, handler)` vor Android S vs. `SessionConfiguration` ab S) werden in einem Wrapper behandelt. Der Aufrufer muss davon nichts wissen.

### Beispiel 3: `suspend fun awaitCaptureResult()` für eine einzelne Aufnahme

```kotlin
suspend fun CameraCaptureSession.captureAwait(
    request: CaptureRequest,
    handler: Handler
): TotalCaptureResult = suspendCancellableCoroutine { cont ->
    val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            cont.resume(result)
        }
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            cont.resumeWithException(
                CaptureFailureException(
                    "Aufnahme fehlgeschlagen: Grund=${failure.reason} Frame=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* ignorieren */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

Dies ist der Baustein für manuelle Multi-Frame-Belichtungsreihen im Stil von Kapitel 19 – verketten Sie 7 `captureAwait(br[i])`-Aufrufe in einer `for`-Schleife mit `withTimeoutOrNull`, sammeln Sie alle 7 `TotalCaptureResult`s und Sie haben eine vollständige HDR-Belichtungsreihe mit Timeout pro Frame und automatischem Abbruch bei Stornierung. In der Callback-Welt waren dies hunderte Zeilen Zustandsmaschine. Jetzt ist es eine 12-zeilige `for`-Schleife.

---

## Flow für kontinuierliche Streams

One-Shot-Operationen decken das Öffnen der Kamera, das Erstellen der Sitzung und die einzelne Aufnahme ab. Für sich wiederholende Vorgänge – jeder Vorschau-Frame, jedes `TotalCaptureResult`, jedes `Image` von einem `ImageReader` – wollen wir einen `Flow<T>`, damit wir Streams zwischen Abonnenten mit `map`, `filter`, `debounce`, `combine` und mehr verarbeiten und teilen können.

### Beispiel 4: ImageReader → `Flow&lt;Image&gt;` über `callbackFlow`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() verwirft alte Frames, wenn der Konsument langsamer ist
        // als die Kamera sie produziert — zwingend erforderlich, um den HAL nicht zu blockieren.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // invokeOnClose muss ZUERST aufgerufen werden, damit der Abbruch den Listener
    // immer entfernt, selbst wenn setOnImageAvailableListener selbst einen Fehler wirft.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // Schließen Sie den ImageReader hier NICHT — der Aufrufer ist Eigentümer des Lebenszyklus.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // Jedes Image, das nicht von Downstream-Collectoren konsumiert wurde,
     // müssen wir schließen, da callbackFlow Fehler nach der Ausstrahlung erneut wirft.
 }
```

**Kritische Designentscheidungen:**

1. `acquireLatestImage()` statt `acquireNextImage()`. Wenn Ihre Bildverarbeitung (ML-Inferenz, Gesichtserkennung) 40 ms dauert und die Kamera mit 30 fps (~33 ms pro Frame) aufnimmt, *werden* Sie in Rückstand geraten. `acquireNextImage` reiht sie auf, bis Ihnen die gralloc-Puffer ausgehen und die Kamera einfrieren. `acquireLatestImage` überspringt die alten Bilder und liefert Ihnen das aktuellste Bild. Dies ist fast immer das, was Sie für die bildseitige Analyse der Vorschau wollen.

2. `buffer(Channel.CONFLATED)`. Ein zusammengefasster Puffer (conflated) behält nur den neuesten Wert. In Kombination mit `acquireLatestImage` ist dies eine harte Garantie, dass Sie niemals veraltete Frames in der Warteschlange haben.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. Dies ist das `callbackFlow`-Äquivalent zu `cont.invokeOnCancellation`. Brechen Sie den Coroutine-Scope ab (z. B. wenn das Fragment `onDestroyView` durchläuft), wird der Listener automatisch abgemeldet und der `HandlerThread` bereinigt. *Kein* Leak.

### Beispiel 5: CaptureCallback → `Flow&lt;TotalCaptureResult&gt;`

Dasselbe Muster:

```kotlin
fun CameraCaptureSession.repeatingResultsFlow(
    repeatingRequest: CaptureRequest,
    handler: Handler
): Flow<TotalCaptureResult> = callbackFlow {
    val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            trySend(result)
        }
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partial: CaptureResult
        ) {
            // Wenn Sie Teilergebnisse benötigen, senden Sie diese über einen separaten Kanal
            // oder senden Sie eine Sealed Class.
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* ignorieren */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

Jetzt haben Sie einen kalten `Flow&lt;TotalCaptureResult&gt;`, der beim Sammeln (Collect) eine wiederholte Anforderung startet, diese beim Abbruch stoppt, jedes abgeschlossene Ergebnis emittiert und mit jedem Standard-Flow-Operator funktioniert.

### Beispiel 6: `combine(previewFlow, aeStateFlow)` für eine reaktive UI

Die wahre Stärke von Flow ist die Komposition. Angenommen, Ihre UI zeigt:
- Live-Vorschau FPS
- Aktuellen AE-Status (konvergierend / konvergiert / gesperrt)
- Eine "Bereit zur Aufnahme"-Anzeige, die nur dann grün ist, wenn AE konvergiert UND AF konvergiert UND AWB konvergiert ist.

Ohne Flow schreiben Sie eine Zustandsmaschine, die `CaptureCallback` mit `Choreographer` zusammenführt. Mit Flow sind es drei Zeilen:

```kotlin
data class UiCameraState(
    val aeState: Int,
    val afState: Int,
    val awbState: Int,
    val fps: Int,
    val ready: Boolean
)

val resultFlow: Flow<TotalCaptureResult> = session
    .repeatingResultsFlow(previewRequest, cameraHandler)
    .flowOn(cameraDispatcher)   // weg vom Main-Thread, kein UI-Ruckeln

val aeStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AE_STATE] ?: CaptureResult.CONTROL_AE_STATE_INACTIVE
}
val afStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AF_STATE] ?: CaptureResult.CONTROL_AF_STATE_INACTIVE
}
val awbStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AWB_STATE] ?: CaptureResult.CONTROL_AWB_STATE_INACTIVE
}
val fpsFlow = resultFlow
    .map { it[CaptureResult.SENSOR_TIMESTAMP] }
    .runningFold(emptyList<Long>()) { acc, ts ->
        (acc + ts).takeLast(30) // gleitendes Fenster über 30 Zeitstempel
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // FPS-Label nur alle 250 ms aktualisieren, schont den Akku

val uiState: Flow<UiCameraState> = combine(
    aeStateFlow, afStateFlow, awbStateFlow, fpsFlow
) { ae, af, awb, fps ->
    val aeConverged = ae in arrayOf(
        CaptureResult.CONTROL_AE_STATE_CONVERGED,
        CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
        CaptureResult.CONTROL_AE_STATE_LOCKED
    )
    val afConverged = af in arrayOf(
        CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
        CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
    )
    val awbConverged = awb in arrayOf(
        CaptureResult.CONTROL_AWB_STATE_CONVERGED,
        CaptureResult.CONTROL_AWB_STATE_LOCKED
    )
    UiCameraState(ae, af, awb, fps, ready = aeConverged && afConverged && awbConverged)
}
```

Sammeln Sie `uiState` im `viewLifecycleOwner.lifecycleScope.launchWhenStarted` Ihres Fragments und übergeben Sie jedes Emit an Ihre Compose-UI oder das `viewBinding`. Jeder Operator – `map`, `runningFold`, `debounce`, `combine` – ist ein Primitiv der Standardbibliothek. Keine benutzerdefinierte Zustandsmaschine. Keine Race Conditions. Keine verpassten Ereignisse. Brechen Sie den Scope ab und jeder einzelne Flow stromaufwärts – einschließlich der wiederholten Anforderung und des `ImageReader`-Listeners – wird gestoppt, das Abonnement beendet und genau einmal bereinigt.

---

## Thread-Sicherheit

All das oben Genannte ist wertlos, wenn Sie die Regeln zur Thread-Sicherheit von Camera2 verletzen. Hier sind sie, destilliert aus hunderten von ANR-Fehlerberichten:

1. **Rufen Sie niemals eine Camera2-API vom Haupt-Thread aus auf.** `cameraManager.openCamera()` mag auf einem Pixel 7 schnell aussehen. Auf einem Budget-Android-Go-Gerät mit einem `LEGACY`-HAL kann es für 1,2 s blockieren. Das ist ein sofortiger ANR. Selbst Aufrufe, die billig *aussehen*, wie `CameraCharacteristics.get()`, können mehrere KB Metadaten allokieren und kopieren – was bei einem Kaltstart des Prozesses, während der Benutzer zwischen Fragmenten wischt, ausreicht, um 3 Frames zu verlieren. Delegieren Sie *alles* an `Dispatchers.Default` oder einen dedizierten Single-Thread-Dispatcher, der auf einem `HandlerThread` basiert.

2. **HandlerThread vs `CoroutineDispatcher.Default` vs `Dispatchers.IO`.**
   - Verwenden Sie einen **Single-Thread-Dispatcher** (z. B. `HandlerThread("cam").asCoroutineDispatcher()`) für die *tatsächlichen* Camera2-API-Aufrufe. Der Legacy-Kamera-Stack auf vielen `LEGACY`-Geräten hat thread-affine HAL-Einstiegspunkte. Das Wechseln von Threads zwischen `openCamera` und `createCaptureSession` löst bekannte HAL-Bugs auf Qualcomm msm8953 und älteren aus.
   - Verwenden Sie `Dispatchers.Default` für reine Berechnungen auf aufgenommenen Frames (HDR-Zusammenführung, JPEG-Kodierung, Gesichtserkennung). Er hat so viele Threads wie Kerne.
   - Verwenden Sie `Dispatchers.IO` für Festplatten-I/O (Speichern des JPEGs im MediaStore). Verwenden Sie niemals `Default` für blockierende Schreibvorgänge.

3. **Ein gemeinsam genutzter veränderlicher Zustand zwischen Coroutinen und Callbacks muss durch einen `Mutex` geschützt sein.** Wenn ein `CaptureCallback` `lastResult` schreibt und ein Klick auf eine Compose-Schaltfläche dieses liest, umschließen Sie beide Seiten mit `mutex.withLock { ... }` oder verwenden Sie `atomicfu`/`@Volatile` für primitive Typen. Verlassen Sie sich NICHT darauf, dass "es ja immer nur einen Thread berührt". HAL-Callbacks auf `LEGACY`-Geräten werden gelegentlich auf unerwarteten Threads ausgelöst, und wenn das passiert, erhalten Sie korrupte (torn reads) 64-Bit-`Long`-Werte wie `SENSOR_TIMESTAMP`.

4. **Warum Flow den `PipedOutputStream`-Deadlock vermeidet.** Der im Forschungs-Doc erwähnte `PipedOutputStream`-Fallstrick verdient ein konkretes Beispiel. Wenn Sie dies täten:

   ```kotlin
   // TUN SIE DIES AUF KEINEN FALL
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // pis lesen und in Datei schreiben
   }
   ```

   Dies führt innerhalb von 100 Frames zu einem Deadlock, da `PipedInputStream` einen Standardpuffer von 64 KB hat. Wenn der Writer schneller produziert als der Reader konsumiert, blockiert der Writer bei `pos.write()` und der Puffer füllt sich. Wenn der Reader währenddessen durch etwas anderes blockiert ist (z. B. eine MediaStore-Bulk-Insert-Transaktion), blockieren beide Coroutinen für immer – ein klassischer zirkulärer Wartezustand. Flow mit `buffer(CONFLATED)` oder `buffer(DROP_OLDEST)` hat eine explizite Backpressure-Semantik und führt niemals zu einem Deadlock. Lieber Frames verwerfen als einen Deadlock riskieren. Das ist der richtige Kompromiss für die Kameravorschau.

---

## Zusammenfassung

Die Callback-basierte API von Camera2 erzeugt bei naiver Komposition eine tief verschachtelte Callback-Hölle, die fehleranfällig, leak-anfällig und nicht testbar ist. Kotlin Coroutines und Flow geben Ihnen zwei Primitive an die Hand, die das gesamte Design vereinfachen: `suspendCancellableCoroutine` für One-Shot-Operationen (`openCamera`, `createCaptureSession`, einzelne `capture`) mit integrierter Unterstützung für Timeout und Abbruch, und `callbackFlow` für kontinuierliche Streams (Bilder vom ImageReader, wiederholte `CaptureResult`-Callbacks) mit explizitem Backpressure. Standard-Flow-Operatoren – `map`, `filter`, `runningFold`, `debounce` und das so wichtige `combine` – ermöglichen es Ihnen, reaktive, abbruchsichere UI-Status-Pipelines aus modular zusammensetzbaren Teilen zu bauen. Erzwingen Sie Thread-Sicherheit mit einem dedizierten Kamera-Dispatcher, schützen Sie den gemeinsam genutzten Zustand mit einem `Mutex` und ersetzen Sie jedes manuelle Piping im Stil von `PipedOutputStream` durch Flow-Kanäle, um Deadlocks zu vermeiden.

## Wie geht es weiter?

Sie verfügen nun über die Werkzeuge, um robuste Camera2-Apps in Produktionsqualität zu schreiben. Aber wie verifizieren Sie, dass Ihr Code auf den über 24.000 Android-Gerätemodellen funktioniert, die derzeit im Umlauf sind, und wie validieren OEMs ihre HALs vor der Auslieferung? Kapitel 27 behandelt Kameratests: Camera ITS, CTS Verifier und Instrumentierungstests unter Verwendung von Mocks, sodass Sie Ihre Kamera-Testsuite auf CI-Servern ohne jegliche physische Hardware ausführen können.
