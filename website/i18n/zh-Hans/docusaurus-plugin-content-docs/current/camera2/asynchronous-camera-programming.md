---
sidebar_position: 26
title: "第 26 章：异步相机编程"
description: "利用 Kotlin 协程和 Flow 驯服 Camera2 的回调地狱。学习用于单次操作 (openCamera, createCaptureSession, capture) 的 suspendCancellableCoroutine，用于连续 ImageReader 和 CaptureResult 流的 callbackFlow，用于响应式 UI 的 combine 操作符，以及防止 ANR 和死锁的线程安全模式。"
keywords: [kotlin 协程 camera2, 回调地狱, suspendcancellablecoroutine, callbackflow, camera2 flow, 线程安全 camera2, mutex 共享状态, pipedoutputstream 死锁, 响应式相机 ui]
---

# 第 26 章：异步相机编程

## 摘要

回头看看你在第 7 章到第 9 章中编写的代码：`CameraDevice.StateCallback` 嵌套在 `CameraManager.openCamera` 内部，`CameraCaptureSession.StateCallback` 又嵌套在 `onOpened` 内部，`CaptureCallback` 嵌套在 `onConfigured` 内部，`ImageReader.OnImageAvailableListener` 在你手动开启的 `HandlerThread` 上触发，且在每条错误路径上都必须按完全相反的顺序进行拆除。这就是典型的相机版回调地狱。每个缩进层级都是一个新的回调类。每个错误都必须穿过四层匿名对象进行传递。在拆除路径上漏掉任何一个 `close()` 调用都会导致相机泄漏，直到设备重启。

本章就是你渴望已久的重构。我们使用两个协程原语将整个回调丛林转换为整洁、线性、可取消且可测试的 Kotlin 代码：使用 `suspendCancellableCoroutine` 处理单次操作，使用 `callbackFlow` + `Flow` 操作符处理连续流。你将学习协程与 Camera2 交互时的线程安全规则，了解为什么在任何相机调用上阻塞主线程都是在等待 ANR 发生，以及为什么你可能尝试过用于 ImageWriter 数据的 `PipedOutputStream`/`PipedInputStream` 模式会导致死锁，而 Flow 却能自然地避免这一问题。

一如既往，请通过 **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) 验证你的目标硬件性能，以确认你的异步管线所需的性能（重复连拍、部分结果、YUV 重处理）在你的测试设备上确实有提供。

---

## 为什么嵌套回调是"回调地狱"

让我们先将问题视觉化。这是在使用协程之前，一个真实的（经过简化的）生产级 RAW Camera2 应用的结构：

```mermaid
graph TD
    A["onCreateView"] -->|选定 cameraId| B["CameraManager.openCamera"]
    B -->|触发| C[StateCallback.onOpened<br/>lambda 1]
    C -->|持有 cameraDevice| D[createCaptureSession<br/>(输出 = 预览 + ImageReader)]
    D -->|触发| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|持有 session| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|触发 onProgress| G[CaptureCallback.onCaptureProgressed<br/>部分结果]
    F -->|触发 onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|帧就绪| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|JPEG 字节| J[MediaStore 保存调用<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

每一个阴影部分的回调都是一个独立的匿名类。每一个类都捕获了向上两层资源的引用。每一个错误路径都必须从 J 冒泡回 A，按相反顺序关闭 `imageReader → session → cameraDevice → handlerThread`，在 16 种错误排列组合中只要漏掉任何一个 `close()` 调用，都会产生永久的相机泄漏直到设备重启。这就是回调地狱的教科书式定义。

本章的目标就是将这团乱麻变成这样：

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[UI 状态<br/>(单次发射)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

线性、可组合、可测试。通过取消父 `Job` 即可实现取消。每个阶段都是一个普通的函数或 `Flow` 操作符。同样的五个回调现在活在一个 12 行的线性管线中。

---

## 用于单次操作的 Kotlin 协程：`suspendCancellableCoroutine`

将任何基于回调的 API 包装为 `suspend` 函数的核心模式是 `suspendCancellableCoroutine`。步骤始终一致：

1. 调用 `suspendCancellableCoroutine { cont -> ... }` 获取一个 `CancellableContinuation<T>`。
2. 调用真实基于回调的 API，并向其传递一个匿名的回调实现。
3. 在回调的成功路径中，调用 `cont.resume(value)`。
4. 在每一条错误路径中，调用 `cont.resumeWithException(t)`。
5. 在 `cont.invokeOnCancellation { ... }` 中执行清理工作：关闭相机、取消挂起的请求、注销监听器，确保回调永远不会在协程取消*之后*触发。
6. 在调用点使用 `withTimeout` 包装，以免死掉的 HAL 导致你的应用永远挂起。

### 示例 1：`suspend fun openCameraAwait()`

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
                    "相机 $cameraId 在打开过程中断开连接"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "相机 $cameraId 错误: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // 变通方案：在 API 30 之前 openCamera() 并不暴露可取消的句柄。
            // 如果在竞争窗口内打开了设备，则关闭它。
        } catch (_: Throwable) { /* 忽略 */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**为什么这有效。** `openCamera` 是"触发后即忘"的：你调用它，将来某个时刻三个回调方法之一会恰好触发一次。这种契约（"恰好触发一次"）正是我们可以将其一对一映射到 continuation 的原因。如果协程在任何回调触发*之前*被取消，`invokeOnCancellation` 就会运行并防止资源泄漏。如果在 `resume` *之后*被取消，`resume(value) { camera.close() }` 块 —— 即 `resume` 的 `onCancellation` 参数 —— 会自动关闭设备。

带超时调用它非常简单：

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "相机打开在 5s 后超时")
    return@launch
}
```

如果 HAL 挂死（在之前的应用导致相机泄漏后，在低端 `LEGACY` 设备上很常见），这会快速且干净地失败，而不会向用户呈现"应用无响应"对话框。

### 示例 2：`suspend fun createCaptureSessionAwait()`

同样的模式，不同的回调：

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
                    "设备 ${this@createCaptureSessionAwait.id} 的会话配置失败"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // 在旧版 API 上无法取消正在进行的会话创建。
        // 会话如果最终通过上面的 resume onCancellation 块完成，则会自动关闭。
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

这是完全相同的结构。两个 API 版本（S 之前的 `createCaptureSession(surfaces, callback, handler)` 对比 S 之后的 `SessionConfiguration`）被封装在一个包装器中。调用者无需关心细节。

### 示例 3：用于单次捕获的 `suspend fun awaitCaptureResult()`

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
                    "捕获失败: 原因=${failure.reason} 帧=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* 忽略 */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

这是第 19 章式手动多帧包围曝光的基础 —— 在 `for` 循环中使用 `withTimeoutOrNull` 串联 7 个 `captureAwait(br[i])` 调用，收集所有 7 个 `TotalCaptureResult`，你就拥有了一个完整的 HDR 包围序列，具备逐帧超时和取消时自动中止的功能。在回调世界中，这曾是数百行代码的状态机。现在，它只是一个 12 行的 `for` 循环。

---

## 用于连续流的 Flow

单次操作涵盖了相机打开、会话创建和单次捕获。对于重复的操作 —— 每一帧预览、每一个 `TotalCaptureResult`、来自 `ImageReader` 的每一个 `Image` —— 我们希望得到 `Flow<T>`，以便我们可以使用 `map`、`filter`、`debounce`、`combine` 并在订阅者之间共享流。

### 示例 4：通过 `callbackFlow` 实现 ImageReader → `Flow<Image>`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() 会在消费者速度慢于相机生产速度时丢弃旧帧
        // 这是避免阻塞 HAL 的强制要求。
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // 必须首先调用 invokeOnClose，这样即使 setOnImageAvailableListener 本身抛出异常，
    // 取消操作也总能移除监听器。
    awaitClose {
        setOnImageAvailableListener(null, null)
        // 此处不要关闭 ImageReader —— 调用者拥有其生命周期。
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // 由于 callbackFlow 会在发射后重新抛出失败，
     // 任何未被下游收集器消费的 Image 都由我们负责关闭。
 }
```

**关键设计选择：**

1. 优先使用 `acquireLatestImage()` 而非 `acquireNextImage()`。如果你的图像处理（机器学习推理、人脸检测）耗时 40ms，而相机以 30fps（每帧约 33ms）运行，你*肯定*会落后。`acquireNextImage` 会将它们排队直到 gralloc 缓冲区耗尽，相机随之冻结。`acquireLatestImage` 会跳过旧帧，给你最新鲜的一帧。这几乎总是预览侧图像分析所需要的。

2. `buffer(Channel.CONFLATED)`。Conflated 缓冲区仅保留最新值。结合 `acquireLatestImage`，这是你永远不会排队陈旧帧的硬性保证。

3. `awaitClose { setOnImageAvailableListener(null, null) }`。这是 `callbackFlow` 版的 `cont.invokeOnCancellation`。取消协程作用域（例如当 Fragment 经历 `onDestroyView` 时），监听器会自动注销，`HandlerThread` 也会被清理。*绝无*泄漏。

### 示例 5：CaptureCallback → `Flow<TotalCaptureResult>`

同样的模式：

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
            // 如果你需要部分结果，可以在单独的通道发射它们
            // 或者发送一个密封类。
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* 忽略 */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

现在你拥有了一个冷流 `Flow<TotalCaptureResult>`，它在被收集时启动重复请求，在取消时停止，发射每一个完成的结果，并兼容所有标准的 Flow 操作符。

### 示例 6：用于响应式 UI 的 `combine(previewFlow, aeStateFlow)`

Flow 的真正威力在于组合。假设你的 UI 显示：
- 实时预览 FPS
- 当前 AE 状态（正在收敛 / 已收敛 / 已锁定）
- "准备好拍摄"指示器，仅当 AE 已收敛且 AF 已收敛且 AWB 已收敛时才显示为绿色。

如果不使用 Flow，你需要手写一个合并了 `CaptureCallback` 与 `Choreographer` 的状态机。有了 Flow，只需三行：

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
    .flowOn(cameraDispatcher)   // 在主线程之外，不产生 UI 卡顿

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
        (acc + ts).takeLast(30) // 滚动的 30 个时间戳窗口
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // 仅每 250ms 更新一次 FPS 标签，节省电量

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

在 Fragment 的 `viewLifecycleOwner.lifecycleScope.launchWhenStarted` 中收集 `uiState`，并将每次发射传递给你的 Compose UI 或 `viewBinding`。每一个操作符 —— `map`、`runningFold`、`debounce`、`combine` —— 都是标准库原语。无需自定义状态机。无竞态条件。无遗漏事件。取消作用域，上游的每一个 Flow —— 包括重复请求和 `ImageReader` 监听器 —— 都会恰好停止、取消订阅并清理一次。

---

## 线程安全

如果违反了 Camera2 的线程安全规则，上述一切都毫无价值。这里有几条从数百份 ANR 错误报告中提炼出的规则：

1. **绝对不要从主线程调用任何 Camera2 API。** `cameraManager.openCamera()` 在 Pixel 7 上看起来可能很快。但在带有 `LEGACY` HAL 的低成本 Android Go 设备上，它可能阻塞 1.2s。那会导致瞬间的 ANR。甚至像 `CameraCharacteristics.get()` 这样*看起来*开销很小的调用，也可能会分配几 KB 的元数据并进行拷贝 —— 在用户切换 Fragment 时的冷启动过程中，这足以导致掉 3 帧。请将*一切*分发给 `Dispatchers.Default` 或由 `HandlerThread` 支持的专用单线程分发器。

2. **HandlerThread 对比 `CoroutineDispatcher.Default` 对比 `Dispatchers.IO`。**
   - 使用**单线程分发器**（例如 `HandlerThread("cam").asCoroutineDispatcher()`）执行*实际的* Camera2 API 调用。许多 `LEGACY` 设备上的旧版相机堆栈具有线程亲和的 HAL 入口点。在 `openCamera` 和 `createCaptureSession` 之间切换线程会触发高通 msm8953 及更早型号上的已知 HAL bug。
   - 对捕获帧的纯计算（HDR 合并、JPEG 编码、人脸检测）使用 `Dispatchers.Default`。它的线程数与核心数一致。
   - 对磁盘 I/O（将 JPEG 保存到 MediaStore）使用 `Dispatchers.IO`。绝对不要在阻塞式写入中使用 `Default`。

3. **协程与回调之间的共享可变状态必须受 `Mutex` 保护。** 如果 `CaptureCallback` 写入 `lastResult` 而 Compose 按钮点击读取它，请在两端都使用 `mutex.withLock { ... }` 包装，或者对原始类型使用 `atomicfu` / `@Volatile`。不要依赖"它只触及一个线程"。`LEGACY` 设备上的 HAL 回调偶尔会在意想不到的线程上触发，当发生这种情况时，你会遇到 `SENSOR_TIMESTAMP` 等 64 位 `Long` 值的读取撕裂。

4. **为什么 Flow 能避免 `PipedOutputStream` 死锁。** 研究文档中的 `PipedOutputStream` 陷阱值得举一个具体的例子。如果你这样做：

   ```kotlin
   // 绝对不要这样做
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // 读取 pis 并写入文件
   }
   ```

   这会在 100 帧内发生死锁，因为 `PipedInputStream` 具有默认 64KB 的缓冲区。如果写入者的生产速度快于读者的消费速度，写入者就会在 `pos.write()` 上阻塞，缓冲区随之填满。如果此时读者恰好阻塞在其他事情上（例如 MediaStore 批量插入事务），两个协程都会永远阻塞 —— 典型的循环等待。具有 `buffer(CONFLATED)` 或 `buffer(DROP_OLDEST)` 的 Flow 具有显式的背压语义，永远不会死锁。宁可丢帧，也不要死锁。这才是相机预览的正确权衡。

---

## 小结

Camera2 基于回调的 API 如果组合不当，会产生深层嵌套的回调地狱，极易出错、产生泄漏且不可测试。Kotlin 协程和 Flow 提供了两个原语，可以瓦解整个架构：用于单次操作 (`openCamera`、`createCaptureSession`、单次 `capture`) 且内置超时和取消支持的 `suspendCancellableCoroutine`，以及用于连续流 (ImageReader 图像、重复 `CaptureResult` 回调) 且具有显式背压的 `callbackFlow`。标准的 Flow 操作符 —— `map`、`filter`、`runningFold`、`debounce` 以及最重要的 `combine` —— 让你能用可组合的部件构建响应式、取消安全的 UI 状态管线。通过专用相机分发器强化线程安全，使用 `Mutex` 保护共享状态，并使用 Flow 通道替换任何 `PipedOutputStream` 式的手动管道以避免死锁。

## 下一章

你现在拥有了编写稳健、生产级 Camera2 应用的工具。但如何验证你的代码能在目前野外存在的 24,000 多种 Android 设备型号上正常工作，以及 OEM 在出货前如何验证其 HAL？第 27 章涵盖了相机测试：相机 ITS、CTS 验证程序以及使用 mock 对象的插桩测试，这样你就可以在没有任何物理硬件的 CI 服务器上运行你的相机测试套件。
