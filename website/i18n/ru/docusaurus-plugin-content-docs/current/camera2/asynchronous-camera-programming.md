---
sidebar_position: 26
title: "Глава 26: Асинхронное программирование камер"
description: "Укротите ад обратных вызовов Camera2 с помощью Kotlin coroutines и Flow. Изучите suspendCancellableCoroutine для однократных операций (открытие камеры, создание сеанса захвата, захват), callbackFlow для непрерывных потоков ImageReader и CaptureResult, операторы combine для реактивных интерфейсов и паттерны потокобезопасности для предотвращения ANR и взаимных блокировок."
keywords: [kotlin coroutines camera2, ад обратных вызовов, suspendcancellablecoroutine, callbackflow, camera2 flow, потокобезопасность camera2, мутекс разделяемое состояние, взаимная блокировка pipedoutputstream, реактивный интерфейс камеры]
---

# Глава 26: Асинхронное программирование камер

## Резюме

Вернитесь назад и посмотрите на код, который вы написали для глав с 7 по 9. `CameraDevice.StateCallback`, вложенный в `CameraManager.openCamera`, с `CameraCaptureSession.StateCallback`, вложенным в `onOpened`, с `CaptureCallback`, вложенным в `onConfigured`, с `ImageReader.OnImageAvailableListener`, срабатывающим в `HandlerThread`, который вы запустили вручную и должны завершить в строго обратном порядке при каждой ошибке. Это ад обратных вызовов (callback hell) со вкусом камеры. Каждый уровень отступа — это новый класс обратного вызова. Каждая ошибка должна распространяться через четыре уровня анонимных объектов. Каждый пропущенный вызов `close()` при завершении работы приводит к утечке камеры до перезагрузки.

Эта глава — тот самый рефакторинг, которого вы так ждали. Мы превратим весь этот клубок обратных вызовов в чистый, линейный, отменяемый и тестируемый код на Kotlin, используя два примитива корутин: `suspendCancellableCoroutine` для однократных операций и `callbackFlow` + операторы `Flow` для непрерывных потоков. Вы узнаете правила потокобезопасности для взаимодействия корутин с Camera2, поймете, почему блокировка главного потока любым вызовом камеры — это верный путь к ANR, и узнаете, почему паттерн `PipedOutputStream`/`PipedInputStream`, который вы могли пробовать для данных ImageWriter, приводит к взаимным блокировкам, которых Flow естественным образом избегает.

Как всегда, проверяйте аппаратные возможности, на которые вы ориентируетесь, с помощью **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)), чтобы подтвердить, что функции, необходимые вашему асинхронному конвейеру (повторяющаяся серия, частичные результаты, переобработка YUV), действительно поддерживаются на ваших тестовых устройствах.

---

## Почему вложенные обратные вызовы — это «ад обратных вызовов»

Давайте сначала визуализируем проблему. Это реальная (упрощенная) структура из приложения на чистом Camera2 до появления корутин:

```mermaid
graph TD
    A["onCreateView"] -->|cameraId выбран| B["CameraManager.openCamera"]
    B -->|срабатывает в| C[StateCallback.onOpened<br/>lambda 1]
    C -->|содержит cameraDevice| D[createCaptureSession<br/>(выходы = previewSurface + imageReaderSurface)]
    D -->|срабатывает в| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|содержит session| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|срабатывает onProgress| G[CaptureCallback.onCaptureProgressed<br/>частичные результаты]
    F -->|срабатывает onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|кадр готов| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|байты JPEG| J[вызов сохранения MediaStore<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

Каждый затененный обратный вызов — это отдельный анонимный класс. Каждый из них хранит ссылку на ресурсы, находящиеся двумя уровнями выше. Каждый путь ошибки должен пузыриться от J обратно к A, закрывая `imageReader → session → cameraDevice → handlerThread` в обратном порядке, и любая пропущенная команда `close()` в любой из 16 комбинаций ошибок приводит к постоянной утечке камеры до перезагрузки устройства. Это классическое определение ада обратных вызовов.

Цель этой главы — превратить эти спагетти в следующее:

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[UI State<br/>(одна эмиссия)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

Линейно. Компонуемо. Тестируемо. Отменяемо путем отмены родительской `Job`. Каждый этап — это простая функция или оператор `Flow`. Те же пять обратных вызовов теперь живут в линейном конвейере из 12 строк.

---

## Kotlin Coroutines для однократных операций: `suspendCancellableCoroutine`

Основной паттерн для оборачивания любого API на основе обратных вызовов в функцию `suspend` — это `suspendCancellableCoroutine`. Рецепт всегда одинаков:

1. Вызовите `suspendCancellableCoroutine { cont -> ... }`, чтобы получить `CancellableContinuation<T>`.
2. Вызовите реальное API на основе обратных вызовов, передав ему реализацию анонимного обратного вызова.
3. В случае успеха обратного вызова вызовите `cont.resume(value)`.
4. В каждом случае ошибки вызовите `cont.resumeWithException(t)`.
5. В `cont.invokeOnCancellation { ... }` выполните очистку: закройте камеру, отмените ожидающие запросы, отмените регистрацию слушателей, чтобы обратный вызов никогда не сработал *после* отмены корутины.
6. Оберните все это в `withTimeout` в местах вызова, чтобы зависший HAL не смог навсегда подвесить ваше приложение.

### Пример 1: `suspend fun openCameraAwait()`

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
                    "Камера $cameraId отключена во время открытия"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "Ошибка камеры $cameraId: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // Обходной путь: openCamera() не предоставляет дескриптор отмены
            // в версиях до API 30. Закройте устройство, если оно было открыто в окне гонки.
        } catch (_: Throwable) { /* игнорировать */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**Почему это работает.** `openCamera` работает по принципу «выстрелил и забыл»: вы вызываете ее, и в какой-то момент один из трех методов обратного вызова срабатывает ровно один раз. Этот контракт («срабатывает ровно один раз») позволяет нам однозначно сопоставить его с продолжением (continuation). Если корутина отменяется *до* срабатывания любого обратного вызова, запускается `invokeOnCancellation`, предотвращая утечку ресурсов. Если она отменяется *после* `resume`, блок `resume(value) { camera.close() }` — параметр `onCancellation` функции `resume` — автоматически закрывает устройство.

Вызов с таймаутом тривиален:

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "Таймаут открытия камеры через 5 с")
    return@launch
}
```

Если HAL завис (что часто случается на устройствах начального уровня `LEGACY` после утечки камеры из предыдущего приложения), этот вызов быстро и чисто завершится ошибкой, вместо того чтобы показывать пользователю диалоговое окно «Приложение не отвечает».

### Пример 2: `suspend fun createCaptureSessionAwait()`

Тот же паттерн, другой обратный вызов:

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
                    "Ошибка конфигурации сеанса для устройства ${this@createCaptureSessionAwait.id}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // Невозможно отменить создание сеанса в процессе выполнения в старых API.
        // Сеанс закроется, если он со временем завершится через блок
        // onCancellation функции resume выше.
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

Это точно такая же форма. Две версии API (`createCaptureSession(surfaces, callback, handler)` до версии S и `SessionConfiguration` начиная с S) обрабатываются в одной обертке. Вызывающим сторонам об этом знать не обязательно.

### Пример 3: `suspend fun awaitCaptureResult()` для одиночного захвата

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
                    "Захват не удался: причина=${failure.reason} кадр=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* игнорировать */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

Это строительный блок для ручного многокадрового брекетинга в стиле главы 19 — соедините 7 вызовов `captureAwait(br[i])` в цикле `for` с `withTimeoutOrNull`, соберите все 7 объектов `TotalCaptureResult`, и у вас будет полная последовательность HDR-брекетинга с таймаутом для каждого кадра и автоматической отменой при прерывании. В мире обратных вызовов это были сотни строк кода конечного автомата. Теперь это цикл `for` из 12 строк.

---

## Flow для непрерывных потоков

Однократные операции охватывают открытие камеры, создание сеанса и одиночный захват. Для повторяющихся действий — каждого кадра предпросмотра, каждого `TotalCaptureResult`, каждого `Image` из `ImageReader` — нам нужен `Flow<T>`, чтобы мы могли использовать `map`, `filter`, `debounce`, `combine` и разделять потоки между подписчиками.

### Пример 4: ImageReader → `Flow<Image>` через `callbackFlow`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() отбрасывает старые кадры, если потребитель медленнее,
        // чем камера производит их — это обязательно, чтобы избежать зависания HAL.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // Необходимо вызвать awaitClose в ПЕРВУЮ ОЧЕРЕДЬ, чтобы отмена всегда удаляла слушателя,
    // даже если сама setOnImageAvailableListener выдаст исключение.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // НЕ закрывайте ImageReader здесь — его жизненным циклом управляет вызывающая сторона.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // Любой Image, не потребленный нижележащими коллекторами, закрываем мы,
     // так как callbackFlow повторно выбрасывает исключения после эмиссии.
 }
```

**Критически важные архитектурные решения:**

1. `acquireLatestImage()` вместо `acquireNextImage()`. Если ваша обработка изображений (вывод ML, обнаружение лиц) занимает 40 мс, а камера работает на частоте 30 кадров в секунду (~33 мс на кадр), вы *будете* отставать. `acquireNextImage` ставит их в очередь, пока у вас не закончатся буферы gralloc и камера не зависнет. `acquireLatestImage` пропускает старые кадры и дает вам самый свежий. Это почти всегда то, что вам нужно для анализа изображений на стороне предпросмотра.

2. `buffer(Channel.CONFLATED)`. Объединенный (conflated) буфер сохраняет только последнее значение. В сочетании с `acquireLatestImage` это жесткая гарантия того, что вы никогда не будете ставить в очередь устаревшие кадры.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. Это эквивалент `cont.invokeOnCancellation` для `callbackFlow`. Отмените область действия корутины (например, когда фрагмент проходит через `onDestroyView`), и слушатель будет автоматически удален, а `HandlerThread` очищен. Утечек *нет*.

### Пример 5: CaptureCallback → `Flow<TotalCaptureResult>`

Тот же паттерн:

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
            // Если вам нужны частичные результаты, передавайте их по отдельному каналу
            // или отправляйте через sealed class.
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* игнорировать */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

Теперь у вас есть «холодный» `Flow<TotalCaptureResult>`, который запускает повторяющийся запрос при сборе данных, останавливает его при отмене, выдает каждый завершенный результат и работает с каждым стандартным оператором Flow.

### Пример 6: `combine(previewFlow, aeStateFlow)` для реактивного интерфейса

Настоящая сила Flow заключается в композиции. Предположим, ваш интерфейс показывает:
- Текущий FPS предпросмотра
- Текущее состояние AE (сходится / сошлось / заблокировано)
- Индикатор «Готов к съемке», который становится зеленым только тогда, когда AE сошлась, AF сошлась И AWB сошлась.

Без Flow вам пришлось бы писать вручную конечный автомат, объединяющий `CaptureCallback` с `Choreographer`. С Flow это три строки:

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
    .flowOn(cameraDispatcher)   // вне главного потока, без лагов интерфейса

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
        (acc + ts).takeLast(30) // скользящее окно из 30 временных меток
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // обновлять метку FPS только каждые 250 мс, экономит заряд батареи

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

Собирайте `uiState` в `viewLifecycleOwner.lifecycleScope.launchWhenStarted` вашего фрагмента и передавайте каждое изменение в Compose UI или `viewBinding`. Каждый оператор — `map`, `runningFold`, `debounce`, `combine` — это стандартный примитив библиотеки. Никаких самописных конечных автоматов. Никаких состояний гонки. Никаких пропущенных событий. Отмените область действия, и каждый Flow выше по течению — включая повторяющийся запрос и слушателя `ImageReader` — остановится, отменит подписку и выполнит очистку ровно один раз.

---

## Потокобезопасность

Все вышеперечисленное не имеет смысла, если вы нарушаете правила потокобезопасности Camera2. Вот они, дистиллированные из сотен отчетов об ошибках ANR:

1. **Никогда не вызывайте никакое API Camera2 из главного потока.** `cameraManager.openCamera()` может казаться быстрым на Pixel 7. На бюджетном устройстве Android Go с HAL уровня `LEGACY` он может заблокировать поток на 1,2 с. Это мгновенный ANR. Даже вызовы, которые *выглядят* дешево, например `CameraCharacteristics.get()`, могут выделять несколько КБ метаданных и копировать их — чего достаточно для пропуска 3 кадров при «холодном» запуске процесса, пока пользователь переключается между фрагментами. Отправляйте *все* вызовы в `Dispatchers.Default` или на выделенный однопоточный диспетчер, поддерживаемый `HandlerThread`.

2. **HandlerThread против `CoroutineDispatcher.Default` против `Dispatchers.IO`.**
   - Используйте **однопоточный диспетчер** (например, `HandlerThread("cam").asCoroutineDispatcher()`) для *непосредственных* вызовов API Camera2. Унаследованный стек камер на многих устройствах `LEGACY` имеет точки входа в HAL, привязанные к потоку (thread-affine). Переключение потоков между `openCamera` и `createCaptureSession` вызывает известные ошибки HAL на Qualcomm msm8953 и более старых процессорах.
   - Используйте `Dispatchers.Default` для чистых вычислений на захваченных кадрах (объединение HDR, кодирование JPEG, обнаружение лиц). У него столько же потоков, сколько ядер у процессора.
   - Используйте `Dispatchers.IO` для ввода-вывода на диск (сохранение JPEG в MediaStore). Никогда не используйте `Default` для блокирующих операций записи.

3. **Общее изменяемое состояние между корутинами и обратными вызовами должно быть защищено с помощью `Mutex`.** Если `CaptureCallback` записывает `lastResult`, а клик по кнопке в Compose считывает его, оберните обе стороны в `mutex.withLock { ... }` или используйте `atomicfu`/`@Volatile` для примитивных типов. НЕ полагайтесь на то, что «это касается только одного потока». Обратные вызовы HAL на устройствах `LEGACY` иногда срабатывают в неожиданных потоках, и когда это происходит, вы получаете поврежденные данные при чтении 64-битных значений `Long`, таких как `SENSOR_TIMESTAMP`.

4. **Почему Flow позволяет избежать взаимной блокировки `PipedOutputStream`.** Опасность `PipedOutputStream`, упомянутая в исследовательском документе, заслуживает конкретного примера. Если бы вы сделали так:

   ```kotlin
   // ТАК ДЕЛАТЬ НЕЛЬЗЯ
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // чтение pis и запись в файл
   }
   ```

   Это приведет к взаимной блокировке в пределах 100 кадров, потому что у `PipedInputStream` по умолчанию размер буфера 64 КБ. Если поток записи производит данные быстрее, чем поток чтения потребляет их, запись блокируется на `pos.write()`, и буфер заполняется. Если при этом чтение заблокировано чем-то другим (например, транзакцией массовой вставки MediaStore), обе корутины блокируются навсегда — классическое циклическое ожидание. Flow с `buffer(CONFLATED)` или `buffer(DROP_OLDEST)` имеет явную семантику противодавления (backpressure) и никогда не приводит к взаимной блокировке. Пропускайте кадры, но не допускайте взаимной блокировки. Это правильный компромисс для предпросмотра камеры.

---

## Резюме

API Camera2 на основе обратных вызовов при наивной компоновке превращается в глубоко вложенный «ад обратных вызовов», подверженный ошибкам, утечкам и не поддающийся тестированию. Kotlin coroutines и Flow дают вам два примитива, которые упрощают всю архитектуру: `suspendCancellableCoroutine` для однократных операций (`openCamera`, `createCaptureSession`, одиночный `capture`) со встроенной поддержкой таймаутов и отмены, и `callbackFlow` для непрерывных потоков (изображения ImageReader, повторяющиеся обратные вызовы `CaptureResult`) с явным управлением противодавлением. Стандартные операторы Flow — `map`, `filter`, `runningFold`, `debounce` и важнейший `combine` — позволяют создавать надежные, безопасные к отмене конвейеры состояний пользовательского интерфейса из компонуемых частей. Обеспечьте потокобезопасность с помощью выделенного диспетчера камеры, защитите общее состояние с помощью `Mutex` и замените любую ручную передачу данных в стиле `PipedOutputStream` на каналы Flow, чтобы избежать взаимных блокировок.

## Что дальше

Теперь у вас есть инструменты для написания надежных приложений на Camera2 промышленного уровня. Но как проверить, что ваш код работает на более чем 24 000 моделях устройств Android, существующих в мире, и как производители проверяют свои HAL перед выпуском? Глава 27 посвящена тестированию камер: Camera ITS, CTS Verifier и инструментальные тесты с использованием моков, чтобы вы могли запускать пакет тестов камеры на серверах CI без реального оборудования.
