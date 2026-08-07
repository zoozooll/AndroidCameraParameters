---
sidebar_position: 26
title: "Capítulo 26: Programación de Cámara Asíncrona"
description: "Dome el infierno de las devoluciones de llamada (callbacks) de Camera2 utilizando las corrutinas de Kotlin y Flow. Aprenda suspendCancellableCoroutine para operaciones de un solo disparo (openCamera, createCaptureSession, capture), callbackFlow para flujos continuos de ImageReader y CaptureResult, operadores de combinación para interfaces de usuario reactivas y patrones de seguridad de hilos para evitar ANR y bloqueos (deadlocks)."
keywords: [corrutinas de kotlin camera2, callback hell, suspendcancellablecoroutine, callbackflow, camera2 flow, seguridad de hilos camera2, mutext shared state, pipedoutputstream deadlock, ui de cámara reactiva]
---

# Capítulo 26: Programación de Cámara Asíncrona

## Resumen

Vuelva a mirar el código que escribió para los Capítulos 7 a 9. `CameraDevice.StateCallback` anidado dentro de `CameraManager.openCamera`, con `CameraCaptureSession.StateCallback` anidado dentro de `onOpened`, con `CaptureCallback` anidado dentro de `onConfigured`, con `ImageReader.OnImageAvailableListener` disparándose en un `HandlerThread` que usted mismo creó y debe desmontar exactamente en el orden inverso en cada ruta de error. Esto es el infierno de las devoluciones de llamada (callback hell), con sabor a cámara. Cada nivel de sangría es una nueva clase de devolución de llamada. Cada error debe propagarse a través de cuatro capas de objetos anónimos. Cada `close()` omitido en la ruta de desmontaje filtra la cámara hasta el reinicio.

Este capítulo es la refactorización que tanto ha deseado. Convertimos todo el laberinto de devoluciones de llamada en código Kotlin limpio, lineal, cancelable y comprobable utilizando dos primitivas de corrutina: `suspendCancellableCoroutine` para operaciones de un solo disparo y `callbackFlow` + operadores `Flow` para flujos continuos. Aprenderá las reglas de seguridad de hilos para las corrutinas que interactúan con Camera2, por qué bloquear el hilo principal en cualquier llamada de cámara es un ANR a punto de ocurrir y por qué el patrón `PipedOutputStream`/`PipedInputStream` que quizás haya probado para los datos de `ImageWriter` produce bloqueos (deadlocks) que Flow evita de forma natural.

Como siempre, valide las capacidades a nivel de hardware a las que se dirige con **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) para confirmar que las capacidades que su canalización asíncrona necesita (ráfaga repetitiva, resultados parciales, reprocesamiento YUV) realmente se incluyen en sus dispositivos de prueba.

---

## Por qué las devoluciones de llamada anidadas son el "infierno de las devoluciones de llamada"

Visualicemos primero el problema. Esta es una estructura real (simplificada) de una aplicación Camera2 pura de producción antes de las corrutinas:

```mermaid
graph TD
    A["onCreateView"] -->|cameraId elegido| B["CameraManager.openCamera"]
    B -->|dispara en| C[StateCallback.onOpened<br/>lambda 1]
    C -->|mantiene cameraDevice| D[createCaptureSession<br/>(salidas = previewSurface + imageReaderSurface)]
    D -->|dispara en| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|mantiene session| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|dispara onProgress| G[CaptureCallback.onCaptureProgressed<br/>resultados parciales]
    F -->|dispara onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|fotograma listo| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|bytes JPEG| J[llamada para guardar en MediaStore<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

Cada devolución de llamada sombreada es una clase anónima separada. Cada una captura una referencia a recursos dos niveles superiores. Cada ruta de error debe burbujear desde J hasta A, cerrando `imageReader → session → cameraDevice → handlerThread` en orden inverso, y cualquier `close()` que falte en cualquiera de las 16 permutaciones de error produce una fuga permanente de la cámara hasta que el dispositivo se reinicie. Esta es la definición de libro de texto del infierno de las devoluciones de llamada.

El objetivo de este capítulo es convertir ese espagueti en esto:

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[Estado de la UI<br/>(emisión única)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

Lineal. Componible. Comprobable. Cancelable cancelando el `Job` padre. Cada etapa es una función simple o un operador `Flow`. Las mismas cinco devoluciones de llamada ahora viven en una canalización lineal de 12 líneas.

---

## Corrutinas de Kotlin para operaciones de un solo disparo: `suspendCancellableCoroutine`

El patrón central para envolver cualquier API basada en devoluciones de llamada como una función `suspend` es `suspendCancellableCoroutine`. La receta es siempre idéntica:

1. Llamar a `suspendCancellableCoroutine { cont -> ... }` para obtener una `CancellableContinuation<T>`.
2. Llamar a la API real basada en devoluciones de llamada, pasándole una implementación de devolución de llamada anónima.
3. En la ruta de éxito de la devolución de llamada, llamar a `cont.resume(value)`.
4. En cada ruta de error, llamar a `cont.resumeWithException(t)`.
5. En `cont.invokeOnCancellation { ... }`, realizar la limpieza: cerrar la cámara, cancelar solicitudes pendientes, anular el registro de los listeners para que la devolución de llamada nunca se dispare *después* de que se cancelara la corrutina.
6. Envolver todo en `withTimeout` en los lugares de llamada para que un HAL muerto no cuelgue su aplicación para siempre.

### Ejemplo 1: `suspend fun openCameraAwait()`

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
                    "Cámara $cameraId desconectada durante la apertura"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "Error en cámara $cameraId: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // Solución: openCamera() no expone un manejador cancelable
            // en APIs anteriores a la 30. Cerrar el dispositivo si se abrió en la ventana de carrera.
        } catch (_: Throwable) { /* ignorar */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**Por qué funciona.** `openCamera` es de tipo "disparar y olvidar": usted lo llama y, en algún momento futuro, uno de los tres métodos de devolución de llamada se dispara exactamente una vez. Ese contrato ("se dispara exactamente una vez") es lo que nos permite mapearlo uno a uno en una continuación. Si la corrutina se cancela *antes* de que se dispare cualquier devolución de llamada, se ejecuta `invokeOnCancellation` y evita una fuga de recursos. Si se cancela *después* de `resume`, el bloque `resume(value) { camera.close() }` (el parámetro `onCancellation` de `resume`) cierra el dispositivo automáticamente.

Llamarlo con un tiempo de espera es trivial:

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "La apertura de la cámara agotó el tiempo de espera después de 5s")
    return@launch
}
```

Si el HAL está colgado (común en dispositivos `LEGACY` de gama baja después de una fuga de cámara de una aplicación anterior), esto falla rápida y limpiamente en lugar de presentar al usuario un diálogo de "La aplicación no responde".

### Ejemplo 2: `suspend fun createCaptureSessionAwait()`

Mismo patrón, diferente devolución de llamada:

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
                    "Fallo en la configuración de la sesión para el dispositivo ${this@createCaptureSessionAwait.id}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // No se puede cancelar la creación de una sesión en curso en APIs antiguas.
        // La sesión se cerrará si finalmente se completa a través del bloque
        // resume onCancellation anterior.
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

Esta es exactamente la misma forma. Las dos versiones de la API (`createCaptureSession(surfaces, callback, handler)` antes de S vs `SessionConfiguration` en S+) se manejan en un solo envoltorio. Los llamadores nunca necesitan saberlo.

### Ejemplo 3: `suspend fun awaitCaptureResult()` para captura única

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
                    "Captura fallida: motivo=${failure.reason} fotograma=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* ignorar */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

Este es el bloque de construcción para el horquillado (bracketing) manual de múltiples fotogramas al estilo del Capítulo 19: encadene 7 llamadas a `captureAwait(br[i])` en un bucle `for` con `withTimeoutOrNull`, recopile los 7 `TotalCaptureResult` y tendrá una secuencia completa de horquillado HDR con tiempo de espera por fotograma y aborto automático por cancelación. En el mundo de las devoluciones de llamada, esto suponía cientos de líneas de máquina de estados. Ahora es un bucle `for` de 12 líneas.

---

## Flow para flujos continuos

Las operaciones de un solo disparo cubren la apertura de la cámara, la creación de la sesión y la captura única. Para las cosas repetitivas (cada fotograma de vista previa, cada `TotalCaptureResult`, cada `Image` de un `ImageReader`) queremos un `Flow<T>` para poder usar `map`, `filter`, `debounce`, `combine` y compartir flujos entre suscriptores.

### Ejemplo 4: ImageReader → `Flow&lt;Image&gt;` a través de `callbackFlow`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() descarta los fotogramas antiguos si el consumidor es más lento que
        // la producción de la cámara: obligatorio para evitar estancar el HAL.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // Debe invocar onClose PRIMERO para que la cancelación siempre elimine el listener
    // incluso si el propio setOnImageAvailableListener lanza excepción.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // NO cierre el ImageReader aquí; el llamador es el propietario de su ciclo de vida.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // Cualquier imagen no consumida por los recolectores de flujo es nuestra responsabilidad cerrarla,
     // porque callbackFlow vuelve a lanzar los fallos después de emitir.
 }
```

**Decisiones de diseño críticas:**

1. `acquireLatestImage()` frente a `acquireNextImage()`. Si el procesamiento de su imagen (inferencia de ML, detección de rostros) tarda 40 ms y la cámara dispara a 30 fps (~33 ms por fotograma), se quedará atrás. `acquireNextImage` las pone en cola hasta que se quede sin búferes gralloc y la cámara se congele. `acquireLatestImage` se salta las antiguas y le da el fotograma más fresco. Esto es casi siempre lo que desea para el análisis de imágenes en el lado de la vista previa.

2. `buffer(Channel.CONFLATED)`. Un búfer combinado (conflated) mantiene solo el último valor. Combinado con `acquireLatestImage`, esta es una garantía absoluta de que nunca pondrá en cola fotogramas obsoletos.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. Este es el equivalente en `callbackFlow` a `cont.invokeOnCancellation`. Cancele el alcance de la corrutina (por ejemplo, cuando el Fragmento pase por `onDestroyView`) y el listener se anulará automáticamente y el `HandlerThread` se limpiará. *Sin* fugas.

### Ejemplo 5: CaptureCallback → `Flow&lt;TotalCaptureResult&gt;`

Mismo patrón:

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
            // Si necesita resultados parciales, emítalos en un canal separado
            // o envíe una clase sellada (sealed class).
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* ignorar */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

Ahora tiene un `Flow&lt;TotalCaptureResult&gt;` frío que inicia una solicitud repetitiva cuando se recolecta, se detiene por cancelación, emite cada resultado completado y funciona con cada operador estándar de Flow.

### Ejemplo 6: `combine(previewFlow, aeStateFlow)` para una UI reactiva

El verdadero poder de Flow es la composición. Suponga que su interfaz de usuario muestra:
- FPS de vista previa en vivo
- Estado actual de AE (convergiendo / convergida / bloqueada)
- Un indicador de "Listo para disparar" que se pone verde solo cuando la AE ha convergido Y el AF ha convergido Y el AWB ha convergido.

Sin Flow, usted escribe a mano una máquina de estados que fusiona `CaptureCallback` con `Choreographer`. Con Flow son tres líneas:

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
    .flowOn(cameraDispatcher)   // fuera del hilo principal, sin latencia de UI

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
        (acc + ts).takeLast(30) // ventana rodante de 30 marcas de tiempo
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // solo actualiza la etiqueta de FPS cada 250ms, ahorra batería

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

Recolecte `uiState` en el `viewLifecycleOwner.lifecycleScope.launchWhenStarted` de su Fragmento y pase cada emisión a su interfaz de usuario de Compose o `viewBinding`. Cada operador (`map`, `runningFold`, `debounce`, `combine`) es una primitiva de la biblioteca estándar. Sin máquina de estados personalizada. Sin condiciones de carrera. Sin eventos perdidos. Cancele el alcance y cada Flow ascendente, incluyendo la solicitud repetitiva y el listener del `ImageReader`, se detiene, cancela la suscripción y se limpia exactamente una vez.

---

## Seguridad de hilos

Todo lo anterior no sirve de nada si viola las reglas de seguridad de hilos de Camera2. Aquí las tiene, extraídas de cientos de informes de errores de ANR:

1. **Nunca llame a ninguna API de Camera2 desde el hilo principal.** `cameraManager.openCamera()` puede parecer rápido en un Pixel 7. En un dispositivo Android Go económico con un HAL `LEGACY`, puede bloquearse durante 1.2s. Eso es un ANR instantáneo. Incluso las llamadas que *parecen* baratas, como `CameraCharacteristics.get()`, pueden asignar varios KB de metadatos y copiarlos, lo que en el inicio de un proceso en frío mientras el usuario desliza entre Fragmentos es suficiente para perder 3 fotogramas. Envíe *todo* a `Dispatchers.Default` o a un despachador de un solo hilo respaldado por un `HandlerThread`.

2. **HandlerThread vs `CoroutineDispatcher.Default` vs `Dispatchers.IO`.**
   - Use un **despachador de un solo hilo** (por ejemplo, `HandlerThread("cam").asCoroutineDispatcher()`) para las llamadas *reales* a la API de Camera2. La pila de cámara heredada en muchos dispositivos `LEGACY` tiene puntos de entrada de HAL con afinidad de hilos. Cambiar de hilo entre `openCamera` y `createCaptureSession` activa errores de HAL conocidos en Qualcomm msm8953 y anteriores.
   - Use `Dispatchers.Default` para el cálculo puro en los fotogramas capturados (fusión HDR, codificación JPEG, detección de rostros). Tiene tantos hilos como núcleos.
   - Use `Dispatchers.IO` para la E/S de disco (guardar el JPEG en MediaStore). Nunca use `Default` para escrituras bloqueantes.

3. **El estado compartido mutable entre corrutinas y devoluciones de llamada debe estar protegido por un `Mutex`.** Si un `CaptureCallback` escribe `lastResult` y un clic de botón de Compose lo lee, envuelva ambos lados con `mutex.withLock { ... }` o use `atomicfu`/`@Volatile` para tipos primitivos. NO confíe en que "solo toca un hilo". Las devoluciones de llamada de HAL en dispositivos `LEGACY` ocasionalmente se disparan en hilos inesperados y, cuando lo hacen, se obtienen lecturas incompletas de valores `Long` de 64 bits como `SENSOR_TIMESTAMP`.

4. **Por qué Flow evita el bloqueo (deadlock) de `PipedOutputStream`.** La trampa del `PipedOutputStream` del documento de investigación merece un ejemplo concreto. Si hiciera esto:

   ```kotlin
   // NO HAGA ESTO
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // leer pis y escribir en archivo
   }
   ```

   Esto se bloquea en menos de 100 fotogramas porque `PipedInputStream` tiene un búfer predeterminado de 64 KB. Si el escritor produce más rápido de lo que el lector consume, el escritor se bloquea en `pos.write()` y el búfer se llena. Si el lector mientras tanto está bloqueado en otra cosa (por ejemplo, una transacción de inserción masiva en MediaStore), ambas corrutinas se bloquean para siempre: una espera circular clásica. Flow con `buffer(CONFLATED)` o `buffer(DROP_OLDEST)` tiene una semántica de contrapresión explícita y nunca se bloquea. Pierda fotogramas, pero nunca se bloquee. Esa es la decisión correcta para la vista previa de la cámara.

---

## Resumen

La API de Camera2 basada en devoluciones de llamada, cuando se compone de forma ingenua, produce un profundo infierno de devoluciones de llamada que es propenso a errores, fugas y es difícil de probar. Las corrutinas de Kotlin y Flow le brindan dos primitivas que simplifican todo el diseño: `suspendCancellableCoroutine` para operaciones de un solo disparo (`openCamera`, `createCaptureSession`, `capture` única) con soporte integrado para tiempo de espera y cancelación, y `callbackFlow` para flujos continuos (imágenes de ImageReader, devoluciones de llamada `CaptureResult` repetitivas) con contrapresión explícita. Los operadores estándar de Flow (`map`, `filter`, `runningFold`, `debounce` y el importantísimo `combine`) le permiten construir canalizaciones de estado de UI reactivas y seguras contra cancelaciones a partir de piezas componibles. Aplique la seguridad de hilos con un despachador de cámara dedicado, proteja el estado compartido con `Mutex` y reemplace cualquier tubería manual al estilo de `PipedOutputStream` con canales de Flow para evitar bloqueos.

## Qué sigue

Ahora tiene las herramientas para escribir aplicaciones Camera2 robustas y de calidad de producción. Pero, ¿cómo verifica que su código funciona en los más de 24,000 modelos de dispositivos Android que existen actualmente y cómo validan los OEM sus HAL antes del envío? El Capítulo 27 cubre las pruebas de cámara: Camera ITS, CTS Verifier y pruebas de instrumentación que usan simulacros (mocks) para que pueda ejecutar su conjunto de pruebas de cámara en servidores de CI sin necesidad de hardware de cámara físico.
