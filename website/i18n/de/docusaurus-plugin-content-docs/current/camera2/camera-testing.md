---
sidebar_position: 27
title: "Kapitel 27: Kameratests"
description: "Der umfassende Leitfaden für Android-Kameratests. Verstehen Sie Camera ITS (Image Test Suite), was OEMs vor der Auslieferung validieren (Funktionskombinationen, Szenentests, Sensor-Fusion), manuelle CTS-Verifier-Tests und wie Sie Ihre eigenen Instrumentierungstests mit Mockito-Mocks und parametrisierten Hardware-Level-Tests schreiben, die in der CI laufen."
keywords: [Camera ITS, Camera Image Test Suite, CTS Camera, CTS Verifier, Android Kameratests, Instrumentierungstest Kamera, Mockito CameraDevice, Mock CameraManager, parametrisiertes Hardware-Level, Sensor-Fusion-Test]
---

# Kapitel 27: Kameratests

## Zusammenfassung

Sie haben eine Kamera-App gebaut. Sie funktioniert auf Ihrem Pixel. Sie funktioniert auf Ihrem Galaxy. Funktioniert sie auch auf dem 99-Euro-Android-Go-Gerät mit einem `LEGACY`-HAL, dessen Hersteller den `CONTROL_AF_TRIGGER_START` fehlerhaft implementiert hat und jede `SENSOR_EXPOSURE_TIME` in *Mikrosekunden* statt Nanosekunden zurückgibt?

Das Testen von Kamerasoftware ist ein zweiteiliges Problem: **OEM-Validierung auf HAL-Ebene** (die Tests, die Google jeden Hersteller bestehen *lässt*, bevor ein Gerät mit Google Play ausgeliefert werden darf) und **Tests auf App-Ebene in der CI** (die Tests, die Sie gegen Ihren eigenen Code ausführen, ohne dass physische Kamerahardware erforderlich ist). Dieses Kapitel behandelt beides. Zuerst erfahren Sie, was Camera ITS (die Image Test Suite, Teil der CTS) tatsächlich an physischen Testständen validiert: Aufzählung von Stream-Kombinationen, Linearität der Luminanz in physischen Szenen und die Verschmelzung von Sensor- und Gyro-Zeitstempeln. Dann lernen Sie, wie Sie Ihre eigenen Instrumentierungstests unter Verwendung von Mockito-Mocks für `CameraManager`/`CameraDevice`/`CaptureSession` schreiben, damit Ihr gesamter Kamera-Stack auf Headless-CI-Servern läuft, sowie ein Muster für parametrisierte Tests, die sicherstellen, dass Ihr Code auf `LEGACY`-Hardware ordnungsgemäß herunterstufbar ist, anstatt abzustürzen.

Verwenden Sie **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) als Referenzwerkzeug, um die exakten Fähigkeiten zu inspizieren, die Ihre Tests prüfen sollten – die App zeigt jeden `CameraCharacteristics`-Schlüssel an, den auch ITS an echten Testständen validiert.

---

## Teil eins: Wie OEMs Kameras validieren — Camera ITS und CTS

Bevor ein Gerät mit Google Mobile Services (GMS) ausgeliefert werden kann, muss es die Android Compatibility Test Suite (CTS) bestehen. Die Camera-CTS besteht aus zwei Teilen: programmatischen CTS-Tests, die über `Tradefed` laufen, und der Camera ITS (Image Test Suite), die ein physisches Testlabor mit automatisierten Messständen erfordert.

### Testkategorien

```mermaid
graph TB
    subgraph CTS[Android CTS — Kamera-Abschnitt]
        direction TB
        CTS_API[API-Tests<br/>— CameraCharacteristics-Schlüssel<br/>— isSessionConfigurationSupported<br/>— Korrekte Aufzählung aller Use-Cases]
        CTS_FLOW[Ablauf-Tests<br/>— öffnen -> schließen<br/>— öffnen -> Sitzung -> Aufnahme -> schließen<br/>— Stresstest für schnelles Öffnen/Schließen]
        CTS_V[CTS Verifier<br/>Manuelle Tests auf dem Gerät<br/>— Flüssigkeit der Vorschau<br/>— Aufnahmequalität<br/>— Wechsel zwischen mehreren Kameras]
    end
    subgraph ITS[Camera ITS — Image Test Suite]
        direction TB
        ITS_COMBI[test_feature_combination<br/>Stream-Permutationen x FPS x HDR<br/>Tausende Aufrufe von<br/>isSessionConfigurationSupported]
        ITS_SCENE[Physische Szenentests<br/>scene0 (gleichmäßiges Grau)<br/>scene1_1 (Farbtafel)<br/>Automatisiertes Tablet-Display -> DUT]
        ITS_FUSION[sensor_fusion Test<br/>Gyro-Zeitstempel müssen mit<br/>SENSOR_TIMESTAMP in CaptureResult<br/>±1 ms Toleranz übereinstimmen]
        ITS_3A[3A-Konvergenztests<br/>AE/AF/AWB müssen innerhalb von<br/>N Frames unter Standardlicht konvergieren]
        ITS_HDR[HDR / Ultra HDR Tests<br/>Gültigkeit der JPEG_R-Gainmap<br/>Messung des Dynamikumfangs]
    end
    CTS --> SHIP[(Auslieferung, wenn ALLE bestanden)]
    ITS --> SHIP

    style ITS_COMBI fill:#d9e6f2
    style ITS_SCENE fill:#d9f2e6
    style ITS_FUSION fill:#f2d9e6
    style CTS_API fill:#fff3cd
    style CTS_V fill:#fff3cd
```

Alles im Diagramm ist obligatorisch. Wenn auch nur *einer* dieser Tests bei einer einzelnen Kamera-ID fehlschlägt, wird das Gerät nicht ausgeliefert. Deshalb hilft das Verständnis von ITS Ihrer App: Es garantiert eine Basislinie, unter die kein HAL fallen darf, und es dokumentiert genau, auf welche Verhaltensweisen Sie sich verlassen können.

### Die Architektur eines Camera-ITS-Teststands

Ein echtes Camera-ITS-Labor sieht so aus:

```mermaid
graph LR
    TC["Test Controller PC<br/>Linux + Tradefed CLI<br/>Führt python3 its/scripts aus"]
    TC -->|USB 3.x ADB| DUT[DUT Smartphone oder Tablet<br/>Device Under Test<br/>Kamera auf Tablet-Display gerichtet]
    TC -->|USB 3.x| TPD[Tablet-Display<br/>~10" kalibriertes 4K-Panel<br/>Führt ITS tabletd APK aus]
    TC -->|GPIO / USB-Relais| LIGHT[Gesteuerte Beleuchtung<br/>CCT-einstellbare LED-Panels<br/>2700 K–6500 K ±2 %]
    TPD -->|projiziert scene0 / scene1_1<br/>über HDMI/internes Display| DUT_CAM[DUT-Rückkamerasensor]
    DUT_CAM -->|erfasst Frames über MIPI -> HAL| DUT
    DUT -->|DNG/JPEG + CaptureResults<br/>abgerufen über adb pull| TC
    TC -->|führt numpy / scipy Analyse aus<br/>Luminanz-Linearität, Farbfehler, Schärfe| RESULT[(PASS / FAIL Bericht + JSON)]
```

Das entscheidende Detail ist der *geschlossene Regelkreis*. Der Testcontroller weiß *exakt*, welche Pixelwerte er dem Tablet-Display befohlen hat (z. B. ein gleichmäßiges Grau bei 50 % Intensität mit einer präzise bekannten Farbtemperatur von 6500 K), und verifiziert dann numerisch, dass die Ausgabe der Kamera des Prüflings (DUT) – sowohl die Pixelluminanz im JPEG/DNG *als auch* die gemeldete `SENSOR_EXPOSURE_TIME` × `SENSOR_SENSITIVITY` im `CaptureResult` – innerhalb der zulässigen Toleranzen mit der physischen Eingabe übereinstimmt.

#### `test_feature_combination`: Der Spießrutenlauf der Aufzählung

Der zeitlich aufwendigste ITS-Test ist `test_feature_combination`. Er zählt jede legale Stream-Größe aus `SCALER_STREAM_CONFIGURATION_MAP`, jedes Format (`PRIV`, `YUV`, `JPEG`, `RAW_SENSOR`, `JPEG_R`, `DEPTH`), jeden FPS-Bereich aus `CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES`, jede Kombination der Anzahl von Ausgabe-Surfaces (Sitzungskonfigurationen mit 1, 2 oder 3 Ausgaben) und jedes HDR-Modus-Flag auf. Dann ruft er `isSessionConfigurationSupported` für die resultierende Sitzungskonfiguration auf, erfasst einen Frame pro unterstützter Konfiguration und stellt sicher, dass der Frame nicht beschädigt ist. Die Gesamtzahl der Kombinationen liegt oft bei 30.000–100.000 pro Kamera-ID.

Für Sie als App-Entwickler ist die Erkenntnis einfach: **Wenn `isSessionConfigurationSupported` auf einem Gerät, das die CTS bestanden hat, `true` zurückgibt, funktioniert diese Stream-Kombination tatsächlich in beide Richtungen.** Wenn sie `false` zurückgibt, versuchen Sie es erst gar nicht. Verlassen Sie sich auf diesen Aufruf, bevor Sie auf kleinere Größen zurückgreifen. Dies ist exakt dieselbe Abfrage, die CameraX intern in seinem Auflösungs-Selektor verwendet.

#### Physische Szenentests: Linearität der Belichtung

Die Tests scene0 und scene1_1 validieren, dass die *gemeldete* Belichtungsberechnung der Kamera mit ihrer *gemessenen* Pixelausgabe übereinstimmt. Der Teststand projiziert ein gleichmäßiges graues Feld (scene0) mit bekannter Luminanz `L` auf den Prüfling. Er befiehlt dann einen Durchlauf von N verschiedenen Paaren aus `(SENSOR_EXPOSURE_TIME, SENSOR_SENSITIVITY)` aus dem gesamten verfügbaren Bereich, erfasst einen DNG-Frame pro Paar und berechnet den arithmetischen Mittelwert der Pixelwerte `Y` über das aktive Array des Sensors.

Die Bedingung ist strikt linear:

```
Y_i / (EXPOSURE_TIME_i × SENSITIVITY_i) = konstant ± Toleranz
```

über *alle* erfassten Paare hinweg. Wenn sich das Produkt verdoppelt, muss sich die Pixelluminanz verdoppeln. Wenn es sich halbiert, muss sich die Luminanz halbieren. Jede Abweichung von mehr als ca. 1 % in den Mitteltönen führt zum Nichtbestehen des Tests.

Warum das für Sie wichtig ist: Es ist die Garantie dafür, dass Ihr manueller Belichtungsregler (Kapitel 14) auf einem CTS-konformen Gerät mathematisch vorhersehbare Ergebnisse liefert. Wenn Ihre App das "nächste ISO/Belichtungs-Paar" für eine Belichtungsänderung um +1 EV berechnet, wird das Ausgabebild wirklich um eine Stufe heller sein. Auf nicht CTS-konformen Geräten (importierte Grauimport-Handys, Custom-ROMs ohne CTS) gilt diese Garantie nicht, und Ihre Benutzeroberfläche für die manuelle Belichtung wird fehlerhaft erscheinen.

#### `sensor_fusion`: Der Zeitstempel-Fusionstest

EIS (elektronische Bildstabilisierung) und AR-Tracking stehen und fallen mit diesem Test. Während der Prüfling ein Video aufnimmt, rotiert der Testcontroller das Telefon physisch auf einer motorisierten kardanischen Aufhängung (Gimbal) mit bekannter Winkelgeschwindigkeit. Gleichzeitig fragt er den Gyroskopsensor des Prüflings über den `SensorManager` mit 400 Hz+ und den `CaptureResult.SENSOR_TIMESTAMP` der Kamera mit 30/60 fps ab.

Die Erfolgsbedingung: Jeder Gyro-Zeitstempel und jeder Frame-`SENSOR_TIMESTAMP` müssen in exakt derselben Zeitbasis `CLOCK_MONOTONIC` liegen, wobei die zum Zeitpunkt der Kameraabtastung interpolierten Gyro-Proben mit der befohlenen Winkelgeschwindigkeit des Gimbals innerhalb von ±0,1 rad/s und ±1 ms übereinstimmen müssen.

Wenn dieser Test fehlschlägt, ist die Zeitstempelquelle des HAL falsch – typischerweise wurden `CLOCK_REALTIME` (Wandzeit, die bei einer NTP-Synchronisation springt) und `CLOCK_MONOTONIC` (stetig, monoton steigend) vermischt. Google lehnt das Gerät ab. Für Sie bedeutet dies, dass Sie `CaptureResult.SENSOR_TIMESTAMP` sicher direkt in den Aufruf zur Aktualisierung des Kamerabildes von ARCore einspeisen können, ohne einen benutzerdefinierten Zeitstempel-Offset anzuwenden, und das auf jedem GMS-zertifizierten Gerät.

### CTS Verifier: Manuelle Benutzertests

Nicht alles kann automatisiert werden. Der CTS Verifier ist eine APK auf dem Gerät, die ein menschlicher QA-Tester für subjektive Tests verwendet:

- **Flüssigkeit der Vorschau:** 30 Sekunden langes Schwenken des Geräts; der Tester bewertet die wahrgenommene Flüssigkeit auf einer Skala von 1–5. (Objektive Telemetriedaten werden ebenfalls über Choreographer-Dumpsys erfasst.)
- **Aufnahmequalität:** 5 Fotos von Standardszenen unter Standardbeleuchtung; der Tester vergleicht sie mit einem Gold-Referenzgerät.
- **Zoom-Übergang bei mehreren Kameras:** Während des kontinuierlichen Zoomens von 0,5×–10× darf es keine sichtbaren Sprünge, Glitches oder schwarzen Frames zwischen den Umschaltvorgängen der physischen Kameras geben.
- **HDR-/JPEG_R-Qualität:** SDR- und HDR-Aufnahmen werden nebeneinander mit bekannten Referenzbildern verglichen.

Diese Tests sind subjektiv, aber die Messlatte ist öffentlich. Wenn Ihre App ähnliche UX-Ziele verfolgt (flüssige Zoom-Übergänge, HDR-Aufnahmen), können Sie dieselben Testverfahren in Ihrem internen QA-Labor mit demselben Tablet-Messstand für scene0/scene1_1 nachbilden.

---

## Teil zwei: Testen Ihrer eigenen App — Instrumentierung und Mocking

OEM-Tests validieren den HAL. Sie müssen *Ihren* Code validieren. Der klassische Fehler, den Teams machen, ist die Anforderung eines echten Telefons mit einer funktionierenden Kamera auf ihrem CI-Server. Tun Sie das nicht. Mit `mock()` + `ArgumentCaptor` von Mockito lässt sich jede einzelne Camera2-Klasse – `CameraManager`, `CameraDevice`, `CameraCaptureSession`, `CaptureResult` – sauber nachbilden (mocken), da es sich um Schnittstellen oder nicht finale Klassen handelt. Sie können Ihre gesamte Kamera-Pipeline in der CI auf einem Headless-Linux-x86-Emulator ohne jegliche Kamerahardware ausführen.

### Beispiel 1: Einen "Frame" erfassen und verifizieren, dass CaptureResult die erwartete EXPOSURE_TIME enthält (AndroidTest mit Mockito)

```kotlin
// app/src/androidTest/java/com/example/camera/CameraCaptureTest.kt
@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraCaptureTest {

    @Test
    fun captureRequest_containsManualExposureTime_andResultEchoesIt() = runTest {
        // ---- Vorbereitung (Arrange) ----
        val mockCameraManager = mock<CameraManager>()
        val mockCameraDevice = mock<CameraDevice>()
        val mockSession = mock<CameraCaptureSession>()
        val mockSurface = mock<Surface>()
        val testHandler = Handler(Looper.getMainLooper())

        val expectedExposureNs = 16_666_666L // 1/60 s
        val expectedIso = 400

        // Den StateCallback erfassen, der an openCamera übergeben wird
        val deviceCallbackCaptor =
            argumentCaptor<CameraDevice.StateCallback>()
        whenever(mockCameraManager.openCamera(
            anyString(), deviceCallbackCaptor.capture(), any()
        )).thenAnswer { /* no-op; wir lösen den Callback manuell aus */ }

        // Den Sitzungs-Zustands-Callback erfassen
        val sessionCallbackCaptor =
            argumentCaptor<CameraCaptureSession.StateCallback>()
        whenever(mockCameraDevice.createCaptureSession(
            anyList<Surface>(),
            sessionCallbackCaptor.capture(),
            any()
        )).thenAnswer { /* no-op */ }

        // Den CaptureCallback erfassen, der an capture() übergeben wird
        val captureCallbackCaptor =
            argumentCaptor<CameraCaptureSession.CaptureCallback>()
        whenever(mockSession.capture(
            any(), captureCallbackCaptor.capture(), any()
        )).thenReturn(1)

        // Die zu testende Kamera unter Verwendung Ihres Wrappers erstellen
        val cameraWrapper = YourCameraWrapper(mockCameraManager, testHandler)

        // ---- Ausführung (Act): Kette öffnen → konfigurieren → Aufnahme auslösen ----
        cameraWrapper.open("0")
        // (Innerhalb von YourCameraWrapper.open() wurde
        //  mockCameraManager.openCamera aufgerufen, was den Callback erfasst hat.)
        // Erfolg des HAL simulieren:
        deviceCallbackCaptor.lastValue.onOpened(mockCameraDevice)

        cameraWrapper.createSession(listOf(mockSurface))
        sessionCallbackCaptor.lastValue.onConfigured(mockSession)

        val requestBuilder: CaptureRequest.Builder =
            mockCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        // (Ihr Wrapper wendet hier manuelle Einstellungen an)
        requestBuilder.set(CaptureRequest.CONTROL_MODE,
                           CaptureRequest.CONTROL_MODE_OFF)
        requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                           expectedExposureNs)
        requestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,
                           expectedIso)

        val resultDeferred = async { cameraWrapper.capture(requestBuilder.build()) }

        // ---- Prüfung (Assert) 1: Der an den HAL gesendete CaptureRequest hatte die richtigen Schlüssel ----
        val sentRequest: CaptureRequest = captureArg(mockSession, 0) {
            capture(any(), any(), any())
        }
        assertThat(sentRequest[CaptureRequest.SENSOR_EXPOSURE_TIME])
            .isEqualTo(expectedExposureNs)
        assertThat(sentRequest[CaptureRequest.SENSOR_SENSITIVITY])
            .isEqualTo(expectedIso)

        // ---- Ausführung 2: Simulieren, dass der HAL ein CaptureResult zurückgibt ----
        val mockResult = mock<TotalCaptureResult>().apply {
            whenever(get(CaptureResult.SENSOR_EXPOSURE_TIME))
                .thenReturn(expectedExposureNs)
            whenever(get(CaptureResult.SENSOR_SENSITIVITY))
                .thenReturn(expectedIso)
            whenever(frameNumber).thenReturn(1L)
        }
        captureCallbackCaptor.lastValue
            .onCaptureCompleted(mockSession, sentRequest, mockResult)

        // ---- Prüfung 2: Das vom Wrapper zurückgegebene Aufnahmeergebnis spiegelt die Belichtung wider ----
        val actual = resultDeferred.await()
        assertThat(actual.exposureTimeNanos).isEqualTo(expectedExposureNs)
        assertThat(actual.iso).isEqualTo(expectedIso)
    }
}
```

Das Muster ist immer das gleiche:
1. Den Callback per `argumentCaptor` erfassen, der an den HAL gehen würde.
2. Ihren Wrapper aufrufen.
3. Die Erfolgsmethode des Callbacks auslösen, *als ob der HAL geantwortet hätte*.
4. Prüfungen sowohl für die Eingaben (was Ihr Wrapper an den HAL gesendet hat) als auch für die Ausgaben (was Ihr Wrapper an den Aufrufer zurückgegeben hat) durchführen.

Dies läuft auf einem Emulator ohne Kamera. Keine Hardware. Kein Flackern durch die Beleuchtung. 10.000 Durchläufe ergeben dieselben 10.000 Erfolge.

### Beispiel 2: Parametrisierter Test — LEGACY-Geräte müssen ordnungsgemäß herunterstufen, niemals abstürzen

Jede produktionsreife Kamera-App muss auf `LEGACY`-HALs laufen. Der häufigste Fehler ist der Aufruf von `CaptureRequest.CONTROL_MODE_OFF` auf einem `LEGACY`-Gerät: Der HAL ignoriert dies, aber Ihr Wrapper interpretiert das resultierende `CaptureResult.CONTROL_AE_STATE == SEARCHING` als vorübergehenden Fehler und versucht, die AE in einer Endlosschleife neu zu starten, was schließlich zu einem ANR führt.

Parametrisieren Sie Ihre Tests nach `INFO_SUPPORTED_HARDWARE_LEVEL`:

```kotlin
@RunWith(Parameterized::class)
class HardwareLevelGracefulDegradationTest(
    private val hardwareLevel: Int,
    private val hardwareLevelName: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun data() = listOf(
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                    "LEGACY"),
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                    "LIMITED"),
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                    "FULL"),
            arrayOf(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                    "LEVEL_3"),
        )
    }

    @Test
    fun requestManualExposure_onAnyHardwareLevel_doesNotCrash_orHang() = runTest {
        val mockCameraManager = mock<CameraManager>()
        val mockChars = mock<CameraCharacteristics>()
        whenever(mockChars.get<Int>(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        )).thenReturn(hardwareLevel)
        whenever(mockCameraManager.getCameraCharacteristics("0"))
            .thenReturn(mockChars)

        val wrapper = YourCameraWrapper(mockCameraManager, testHandler)
        wrapper.open("0")
        // ... (Boilerplate-Sitzungsaufbau wie zuvor, weggelassen)

        val job = launch {
            wrapper.setManualExposure(iso = 400, exposureNs = 8_000_000L)
        }

        // Kein Hängenbleiben — muss innerhalb des Timeouts abgeschlossen sein, selbst auf LEGACY
        withTimeoutOrNull(2_000) { job.join() }
            ?: fail("setManualExposure blieb auf $hardwareLevelName Hardware hängen")

        // Keine Exception nach außen gedrungen
        assertThat(job.isCancelled).isFalse()
    }
}
```

Führen Sie dies bei jedem neuen Build aus. Es dauert 400 ms. Es fängt genau die Klasse von Hängern bei `LEGACY`-HALs ab, die andernfalls erst Monate später in den Play Console Absturzberichten auftauchen würden.

### AndroidTest auf echter Hardware: Sanitäts-Aufnahme zur Überprüfung der EXPOSURE_TIME

Schreiben Sie für nächtliche Durchläufe auf einer kleinen Flotte echter Telefone einen kurzen AndroidTest, der die *tatsächliche* Kamera öffnet, einen RAW-Frame erfasst und sicherstellt, dass `CaptureResult.SENSOR_EXPOSURE_TIME` innerhalb von 5 % des angeforderten Werts lag. Dies schützt vor HAL-Regresssionen bei spezifischen Betriebssystem-Builds:

```kotlin
@RunWith(AndroidJUnit4::class)
@RequiresDevice
@LargeTest
class RealHardwareCaptureSanityTest {

    @Test
    fun realCapture_exposureTimeIsWithin5PercentOfRequested() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().context
        val camManager = context.getSystemService(Context.CAMERA_SERVICE)
            as CameraManager
        val chars = camManager.getCameraCharacteristics("0")
        assumeTrue(
            "Erfordert FULL oder LEVEL_3 für manuelle Belichtung",
            chars[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL] in
            setOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            )
        )

        // ... Kamera öffnen, ImageReader-Sitzung (PRIVATE oder YUV) erstellen, einen
        // einzelnen manuellen Frame mit einer bekannten Belichtung unter Verwendung der
        // Wrapper aus Kapitel 26 (Coroutinen) aufnehmen ...

        val requestedNs = 10_000_000L // 1/100 s
        val result: TotalCaptureResult =
            cameraWrapper.captureManualExposure(requestedNs, iso = 200)

        val actualNs = result[CaptureResult.SENSOR_EXPOSURE_TIME]!!
        val tolerancePct = abs(actualNs - requestedNs) * 100.0 / requestedNs
        assertThat(tolerancePct)
            .withFailMessage(
                "Angefordert %d ns, erhalten %d ns (%$.1f%% Abweichung > 5%%)",
                requestedNs, actualNs, tolerancePct
            )
            .isLessThan(5.0)
    }
}
```

Dieser Test ist von Natur aus unzuverlässig (flaky) – er hängt von echter Hardware ab. Aber er fängt genau die Klasse von Vendor-OTA-Updates ab, die stillschweigend die manuelle Belichtung auf Flaggschiff-Geräten beeinträchtigen. Lassen Sie ihn jede Nacht auf Ihrer Flotte von 5–10 Geräten laufen; das Signal ist das Rauschen wert.

---

## Zusammenfassung

Kameratests unterteilen sich in OEM-Validierung und App-Validierung. OEMs müssen die CTS und die Camera ITS bestehen, eine physisch aufgebaute Testsuite, die die Unterstützung von Stream-Kombinationen (über tausende Aufrufe von `isSessionConfigurationSupported`), die Linearität der Luminanz über die Belichtungs- und Empfindlichkeitsebene sowie die Ausrichtung der Zeitstempel von Sensor und Gyro für EIS und AR erzwingt. Apps testen über Instrumentierung: Mockito simuliert jede Camera2-Klasse, `ArgumentCaptor` fängt HAL-Callbacks ab, Sie lösen diese manuell aus und prüfen sowohl die Anforderung als auch das Ergebnis, ohne echte Hardware zu berühren – was CI-Läufe auf Headless-Emulatoren ermöglicht. Parametrisieren Sie Ihre Wrapper-Tests gegen jeden `INFO_SUPPORTED_HARDWARE_LEVEL` (besonders `LEGACY`), um ein ordnungsgemäßes Herunterstufen zu garantieren, und führen Sie eine kleine Suite von `@RequiresDevice @LargeTest` Sanitäts-Aufnahmen auf einer echten Geräteflotte aus, um OTA-Regressionen abzufangen.

## Wie geht es weiter?

Sie haben die öffentliche Camera2-API von Kotlin bis hin zum nativen NDK gemeistert, sie in Coroutinen gehüllt und sie mit Tests verifiziert. Aber was passiert eigentlich *unter der Haube*, wenn Sie `CameraManager.openCamera` aufrufen? Was ist HAL3? Wo genau verläuft die Binder-IPC-Grenze? Und wie verändert `CameraDeviceSetup` (Android 15, API 35) die Architektur durch die Entkopplung von Fähigkeitsabfragen von der Sensorstromversorgung? Kapitel 28 ist das große Architektur-Finale: der gesamte Stack vom App-Code bis zum VCM-Fokusmotor im Objektivtubus.
