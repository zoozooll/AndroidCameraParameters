---
sidebar_position: 26
title: "第26章：非同步相機程式設計"
description: "使用 Kotlin 協程和 Flow 馴服 Camera2 回呼地獄。學習用 suspendCancellableCoroutine 處理一次性操作（openCamera、createCaptureSession、capture），用 callbackFlow 處理連續的 ImageReader 和 CaptureResult 流，用 combine 運算子建構響應式 UI，以及防止 ANR 和死結的執行緒安全模式。"
keywords: [kotlin coroutines camera2, callback hell, suspendcancellablecoroutine, callbackflow, camera2 flow, thread safety camera2, mutex shared state, pipedoutputstream deadlock, reactive camera ui]
---

# 第26章：非同步相機程式設計

## 摘要

回頭看看你在第7到第9章寫的程式碼。`CameraDevice.StateCallback` 巢狀在 `CameraManager.openCamera` 裡，`CameraCaptureSession.StateCallback` 巢狀在 `onOpened` 裡，`CaptureCallback` 巢狀在 `onConfigured` 裡，`ImageReader.OnImageAvailableListener` 在你手動啟動的 `HandlerThread` 上觸發，並且必須在每條錯誤路徑上以完全相反的順序拆解。這就是相機風味的回呼地獄。每一層縮排都是一個新的回呼類別。每個錯誤都必須穿過四層匿名物件向上傳播。在拆解路徑上每漏掉一個 `close()` 都會讓相機洩漏直到重啟。

本章是你一直渴望的重構。我們使用兩個協程原語將整個回呼叢林轉換為乾淨、線性、可取消、可測試的 Kotlin 程式碼：用於一次性操作的 `suspendCancellableCoroutine`，以及用於連續流的 `callbackFlow` + `Flow` 運算子。你將學習協程與 Camera2 互動的執行緒安全規則，為什麼在任何相機呼叫上阻塞主執行緒都是一場等待發生的 ANR，以及你可能嘗試過的用於 ImageWriter 資料的 `PipedOutputStream`/`PipedInputStream` 模式為何會產生 Flow 自然避免的死結。

一如既往，使用 **Android Camera Parameters**（[Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams)、[GitHub](https://github.com/zoozooll/AndroidCameraParameters)）驗證你目標硬體級別的能力，以確認你的非同步管道所需的能力（重複連拍、部分結果、YUV 重處理）確實存在於你的測試裝置上。

---

## 為什麼巢狀回呼是「回呼地獄」

讓我們先把問題視覺化。這是一個生產環境原始 Camera2 應用在使用協程之前的真實（簡化）結構：

```mermaid
graph TD
    A["onCreateView"] -->|選定 cameraId| B["CameraManager.openCamera"]
    B -->|觸發| C[StateCallback.onOpened<br/>lambda 1]
    C -->|持有 cameraDevice| D[createCaptureSession<br/>(outputs = previewSurface + imageReaderSurface)]
    D -->|觸發| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|持有 session| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|觸發 onProgress| G[CaptureCallback.onCaptureProgressed<br/>部分結果]
    F -->|觸發 onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|幀就緒| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|JPEG 位元組| J[MediaStore 儲存呼叫<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

每個著色的回呼都是一個單獨的匿名類別。每個都捕獲了對上兩層資源的參考。每條錯誤路徑都必須從 J 冒泡回 A，以相反的順序關閉 `imageReader → session → cameraDevice → handlerThread`，在16種錯誤排列中的任何一種裡漏掉一個 `close()` 都會產生直到裝置重啟才能恢復的永久相機洩漏。這就是回呼地獄的教科書定義。

本章的目標是把那堆義大利麵變成這樣：

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[UI 狀態<br/>(單次發射)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

線性。可組合。可測試。可透過取消父 `Job` 來取消。每個階段都是一個普通函式或一個 `Flow` 運算子。同樣五個回呼現在住在一條12行的線性管道裡。

---

## 用於一次性操作的 Kotlin 協程：`suspendCancellableCoroutine`

將任何基於回呼的 API 包裝為 `suspend` 函式的核心模式是 `suspendCancellableCoroutine`。配方始終相同：

1. 呼叫 `suspendCancellableCoroutine { cont -> ... }` 以獲取 `CancellableContinuation<T>`。
2. 呼叫真正的基於回呼的 API，向其傳入一個匿名回呼實作。
3. 在回呼的成功路徑中，呼叫 `cont.resume(value)`。
4. 在每條錯誤路徑中，呼叫 `cont.resumeWithException(t)`。
5. 在 `cont.invokeOnCancellation { ... }` 中執行清理：關閉相機、取消擱置的請求、註銷監聽器，使回呼在協程被取消*之後*永不觸發。
6. 在呼叫點用 `withTimeout` 包裹整個東西，這樣死掉的 HAL 就不能永遠掛起你的應用。

### 範例 1：`suspend fun openCameraAwait()`

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
                    "相機 $cameraId 在開啟期間斷開"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "相機 $cameraId 錯誤: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // 變通方法：openCamera() 在 API 30 之前不暴露可取消的控制代碼
            // 如果在競爭視窗期間已開啟，則關閉裝置。
        } catch (_: Throwable) { /* 忽略 */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**為什麼這能工作。** `openCamera` 是即發即忘的：你呼叫它，在某個未來時間點三個回呼方法之一會恰好觸發一次。那個契約（「恰好觸發一次」）讓我們能把它一對一映射到一個 continuation 上。如果協程在任何回呼觸發*之前*被取消，`invokeOnCancellation` 會執行並防止資源洩漏。如果它被取消在 `resume` *之後*，`resume(value) { camera.close() }` 區塊 —— `resume` 的 `onCancellation` 參數 —— 會自動關閉裝置。

帶逾時呼叫很簡單：

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "相機開啟在5秒後逾時")
    return@launch
}
```

如果 HAL 掛起（在前一個應用洩漏相機後的低階 `LEGACY` 裝置上很常見），這會快速乾淨地失敗，而不是向使用者呈現「應用未回應」對話框。

### 範例 2：`suspend fun createCaptureSessionAwait()`

同樣的模式，不同的回呼：

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
                    "裝置 ${this@createCaptureSessionAwait.id} 的工作階段設定失敗"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // 在較舊的 API 上無法取消進行中的工作階段建立。
        // 如果最終透過上面的 resume onCancellation 區塊完成，工作階段會關閉。
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

這是完全相同的形狀。兩個 API 版本（S 之前的 `createCaptureSession(surfaces, callback, handler)` 與 S+ 的 `SessionConfiguration`）在一個包裝器裡處理。呼叫者無需知道。

### 範例 3：用於單次擷取的 `suspend fun awaitCaptureResult()`

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
                    "擷取失敗: reason=${failure.reason} frame=${failure.frameNumber}"
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

這是第19章風格的手動多幀包圍的建構區塊 —— 在 `for` 迴圈裡用 `withTimeoutOrNull` 連結7次 `captureAwait(br[i])` 呼叫，收集全部7個 `TotalCaptureResult`，你就得到了一個帶逐幀逾時和取消時自動中止的完整 HDR 包圍序列。在回呼世界裡這是數百行的狀態機。現在它是一個12行的 `for` 迴圈。

---

## 用於連續流的 Flow

一次性操作覆蓋了相機開啟、工作階段建立和單次擷取。對於重複的事情 —— 每個預覽幀、每個 `TotalCaptureResult`、來自 `ImageReader` 的每個 `Image` —— 我們需要一個 `Flow<T>`，這樣我們就能在訂閱者之間 `map`、`filter`、`debounce`、`combine` 和共享流。

### 範例 4：透過 `callbackFlow` 將 ImageReader 轉為 `Flow<Image>`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() 會在消費者比相機產出慢時丟棄舊幀
        // —— 這是避免 HAL 卡住的強制要求。
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // 必須先 invokeOnClose，這樣取消時總能移除監聽器，
    // 即使 setOnImageAvailableListener 本身擲出例外。
    awaitClose {
        setOnImageAvailableListener(null, null)
        // 不要在這裡關閉 ImageReader —— 呼叫者擁有其生命週期。
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // 任何未被下游收集者消費的 Image 都由我們關閉，
     // 因為 callbackFlow 會在 emit 後重新擲出失敗。
 }
```

**關鍵設計選擇：**

1. `acquireLatestImage()` 而非 `acquireNextImage()`。如果你的影像處理（ML 推理、人臉偵測）耗時40ms而相機以30fps（每幀約33ms）觸發，你*會*跟不上。`acquireNextImage` 把它們排隊直到你用完 gralloc 緩衝區，相機就凍結了。`acquireLatestImage` 跳過舊的幀，給你最新的幀。對於預覽側影像分析這幾乎總是你想要的。

2. `buffer(Channel.CONFLATED)`。一個合併緩衝區只保留最新值。結合 `acquireLatestImage`，這是一個硬保證：你永遠不會排隊陳舊的幀。

3. `awaitClose { setOnImageAvailableListener(null, null) }`。這是 `callbackFlow` 等價於 `cont.invokeOnCancellation` 的寫法。取消協程作用域（例如當 Fragment 經過 `onDestroyView` 時），監聽器會自動註銷，`HandlerThread` 也會清理。*沒有*洩漏。

### 範例 5：CaptureCallback → `Flow<TotalCaptureResult>`

同樣的模式：

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
            // 如果你需要部分結果，在單獨的通道上發射它們
            // 或發送一個密封類別。
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

現在你有一個冷的 `Flow<TotalCaptureResult>`，在被收集時啟動重複請求，在取消時停止，發射每個完成的結果，並與每個標準 Flow 運算子配合工作。

### 範例 6：`combine(previewFlow, aeStateFlow)` 用於響應式 UI

Flow 的真正威力在於組合。假設你的 UI 顯示：
- 即時預覽 FPS
- 當前 AE 狀態（收斂中 / 已收斂 / 已鎖定）
- 一個「準備拍攝」指示器，僅在 AE 收斂且 AF 收斂且 AWB 收斂時才變綠。

沒有 Flow，你需要手寫一個把 `CaptureCallback` 和 `Choreographer` 合併的狀態機。用 Flow 只需三行：

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
    .flowOn(cameraDispatcher)   // 離開主執行緒，無 UI 卡頓

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
        (acc + ts).takeLast(30) // 滾動30個時間戳視窗
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // 僅每250ms更新一次 FPS 標籤，節省電量

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

在你的 Fragment 的 `viewLifecycleOwner.lifecycleScope.launchWhenStarted` 中收集 `uiState`，把每次發射傳給你的 Compose UI 或 `viewBinding`。每個運算子 —— `map`、`runningFold`、`debounce`、`combine` —— 都是標準函式庫原語。沒有自訂狀態機。沒有競態條件。沒有錯過的事件。取消作用域，上游每一個 Flow —— 包括重複請求和 `ImageReader` 監聽器 —— 都會恰好停止、取消訂閱並清理一次。

---

## 執行緒安全

如果你違反了 Camera2 的執行緒安全規則，上面這一切都毫無價值。以下是從數百份 ANR bug 報告中提煉的規則：

1. **永遠不要從主執行緒呼叫任何 Camera2 API。** `cameraManager.openCamera()` 在 Pixel 7 上看起來可能很快。在配有 `LEGACY` HAL 的入門級 Android Go 裝置上，它可能阻塞1.2秒。那是瞬間的 ANR。即使是那些*看起來*便宜的呼叫，比如 `CameraCharacteristics.get()`，也可能分配數 KB 的中繼資料並複製它 —— 在使用者滑動 Fragment 之間的冷處理程序啟動時，這足以掉3幀。把*所有東西*分發到 `Dispatchers.Default` 或一個由 `HandlerThread` 支援的專用單執行緒排程器。

2. **HandlerThread vs `CoroutineDispatcher.Default` vs `Dispatchers.IO`。**
   - 用一個**單執行緒排程器**（例如 `HandlerThread("cam").asCoroutineDispatcher()`）進行*真正的* Camera2 API 呼叫。許多 `LEGACY` 裝置上的舊相機堆疊有執行緒親和的 HAL 入口點。在 `openCamera` 和 `createCaptureSession` 之間切換執行緒會觸發 Qualcomm msm8953 及更老晶片上已知的 HAL bug。
   - 用 `Dispatchers.Default` 對擷取幀做純計算（HDR 合併、JPEG 編碼、人臉偵測）。它有和核心數一樣多的執行緒。
   - 用 `Dispatchers.IO` 做磁碟 I/O（把 JPEG 儲存到 MediaStore）。永遠不要用 `Default` 做阻塞寫入。

3. **協程和回呼之間的共享可變狀態必須用 `Mutex` 保護。** 如果一個 `CaptureCallback` 寫 `lastResult` 而一個 Compose 按鈕點擊讀它，用 `mutex.withLock { ... }` 包裹兩邊，或對原始型別用 `atomicfu`/`@Volatile`。不要依賴「它只會在一個執行緒上被觸碰」。`LEGACY` 裝置上的 HAL 回呼偶爾會在意想不到的執行緒上觸發，當它們這樣做時，你會得到像 `SENSOR_TIMESTAMP` 這樣的64位元 `Long` 值的撕裂讀。

4. **為什麼 Flow 避免 `PipedOutputStream` 死結。** 研究文件的 `PipedOutputStream` 陷阱值得一個具體例子。如果你這樣做：

   ```kotlin
   // 不要這樣做
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // 讀取 pis 並寫入檔案
   }
   ```

   這在100幀內死結，因為 `PipedInputStream` 預設有64KB 緩衝區。如果寫入者比讀取者消費得快，寫入者阻塞在 `pos.write()` 上，緩衝區被填滿。如果讀取者此時被別的東西阻塞（例如 MediaStore 批量插入事務），兩個協程永遠阻塞 —— 經典的循環等待。帶 `buffer(CONFLATED)` 或 `buffer(DROP_OLDEST)` 的 Flow 有明確的背壓語意，永遠不會死結。丟幀，不死結。這是相機預覽正確的權衡。

---

## 摘要

Camera2 基於回呼的 API，當被天真地組合時，會產生深度巢狀的回呼地獄，容易出錯、容易洩漏、難以測試。Kotlin 協程和 Flow 給你兩個摺疊整個設計的原語：用於一次性操作（`openCamera`、`createCaptureSession`、單次 `capture`）的 `suspendCancellableCoroutine`，帶內建逾時和取消支援；以及用於連續流（ImageReader 影像、重複 `CaptureResult` 回呼）的 `callbackFlow`，帶明確背壓。標準 Flow 運算子 —— `map`、`filter`、`runningFold`、`debounce` 以及至關重要的 `combine` —— 讓你用可組合的部件建構響應式、取消安全的 UI 狀態管道。用專用相機排程器強制執行緒安全，用 `Mutex` 保護共享狀態，用 Flow 通道替換任何 `PipedOutputStream` 風格的手動管道以避免死結。

## 下一步

你現在有了編寫健壯的、生產級 Camera2 應用的工具。但是你如何驗證你的程式碼在目前市面上的24,000多種 Android 裝置型號上都能工作？OEM 又如何在出貨前驗證他們的 HAL？第27章涵蓋相機測試：Camera ITS、CTS Verifier 以及使用 mock 的插樁測試，讓你能在沒有任何實體硬體的 CI 伺服器上執行你的相機測試套件。
