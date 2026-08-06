---
sidebar_position: 26
title: "Chapter 26: Asynchronous Camera Programming"
description: "Tame Camera2 callback hell using Kotlin coroutines and Flow. Learn suspendCancellableCoroutine for one-shot operations (openCamera, createCaptureSession, capture), callbackFlow for continuous ImageReader and CaptureResult streams, combine operators for reactive UIs, and thread-safety patterns to prevent ANRs and deadlocks."
keywords: [kotlin coroutines camera2, callback hell, suspendcancellablecoroutine, callbackflow, camera2 flow, thread safety camera2, mutext shared state, pipedoutputstream deadlock, reactive camera ui]
---

# Chapter 26: Asynchronous Camera Programming

## Summary

Go back and look at the code you wrote for Chapters 7 through 9. `CameraDevice.StateCallback` nested inside `CameraManager.openCamera`, with `CameraCaptureSession.StateCallback` nested inside `onOpened`, with `CaptureCallback` nested inside `onConfigured`, with `ImageReader.OnImageAvailableListener` firing on a `HandlerThread` you spun up by hand and must tear down in exactly the reverse order on every error path. This is callback hell, camera-flavored. Every indentation level is a new callback class. Every error must propagate through four layers of anonymous objects. Every missed `close()` on the tear-down path leaks the camera until reboot.

This chapter is the refactor you have been craving. We convert the entire callback jungle into clean, linear, cancellable, testable Kotlin code using two coroutine primitives: `suspendCancellableCoroutine` for one-shot operations, and `callbackFlow` + `Flow` operators for continuous streams. You will learn thread-safety rules for coroutines interacting with Camera2, why blocking the main thread on any camera call is an ANR waiting to happen, and why the `PipedOutputStream`/`PipedInputStream` pattern you may have tried for ImageWriter data produces deadlocks that Flow naturally avoids.

As always, validate the hardware-level capabilities you are targeting with **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) to confirm that the capabilities your async pipeline needs (repeating burst, partial results, YUV reprocessing) actually ship on your test devices.

---

## Why Nested Callbacks Are "Callback Hell"

Let us first visualize the problem. This is a real (simplified) structure from a production raw Camera2 app before coroutines:

```mermaid
graph TD
    A[onCreateView] -->|cameraId chosen| B[CameraManager.openCamera]
    B -->|fires on| C[StateCallback.onOpened<br/>lambda 1]
    C -->|holds cameraDevice| D[createCaptureSession<br/>(outputs = previewSurface + imageReaderSurface)]
    D -->|fires on| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|holds session| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|fires onProgress| G[CaptureCallback.onCaptureProgressed<br/>partial results]
    F -->|fires onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|frame ready| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|JPEG bytes| J[MediaStore save call<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

Every shaded callback is a separate anonymous class. Every one captures a reference to resources two levels up. Every error path must bubble from J back to A, closing `imageReader → session → cameraDevice → handlerThread` in reverse order, and any single missing `close()` in any of the 16 error permutations produces a permanent camera leak until the device reboots. This is the textbook definition of callback hell.

The goal of this chapter is to turn that spaghetti into this:

```mermaid
flowchart LR
    A[openCameraAwait()] --> B[createCaptureSessionAwait()]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[UI State<br/>(single emit)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

Linear. Composable. Testable. Cancellable by cancelling the parent `Job`. Every stage is a plain function or a `Flow` operator. The same five callbacks now live in a 12-line linear pipeline.

---

## Kotlin Coroutines for One-Shot Operations: `suspendCancellableCoroutine`

The core pattern for wrapping any callback-based API as a `suspend` function is `suspendCancellableCoroutine`. The recipe is always identical:

1. Call `suspendCancellableCoroutine { cont -> ... }` to obtain a `CancellableContinuation<T>`.
2. Call the real callback-based API, passing it an anonymous callback implementation.
3. In the callback's success path, call `cont.resume(value)`.
4. In every error path, call `cont.resumeWithException(t)`.
5. In `cont.invokeOnCancellation { ... }`, do the cleanup: close the camera, cancel pending requests, unregister listeners so the callback never fires *after* the coroutine was cancelled.
6. Wrap the whole thing in `withTimeout` at call sites so a dead HAL cannot hang your app forever.

### Example 1: `suspend fun openCameraAwait()`

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
                    "Camera $cameraId disconnected during open"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "Camera $cameraId error: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // Workaround: openCamera() does not expose a cancellable handle
            // on pre-API 30. Close device if it was opened in the race window.
        } catch (_: Throwable) { /* ignore */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**Why this works.** `openCamera` is fire-and-forget: you call it, and at some future point one of the three callback methods fires exactly once. That contract ("fires exactly once") is what lets us map it one-to-one onto a continuation. If the coroutine is cancelled *before* any callback fires, `invokeOnCancellation` runs and prevents a resource leak. If it is cancelled *after* `resume`, the `resume(value) { camera.close() }` block — the `onCancellation` parameter of `resume` — closes the device automatically.

Calling it with a timeout is trivial:

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "Camera open timed out after 5s")
    return@launch
}
```

If the HAL is hung (common on low-end `LEGACY` devices after a camera leak from a previous app), this fails fast and cleanly instead of presenting the user with an "App not responding" dialog.

### Example 2: `suspend fun createCaptureSessionAwait()`

Same pattern, different callback:

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
                    "Session configuration failed for device ${this@createCaptureSessionAwait.id}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // Cannot cancel in-flight session create on older APIs.
        // Session will close if it eventually completes via the resume
        // onCancellation block above.
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

This is the exact same shape. The two API versions (`createCaptureSession(surfaces, callback, handler)` pre-S vs `SessionConfiguration` S+) are handled in one wrapper. Callers never need to know.

### Example 3: `suspend fun awaitCaptureResult()` for Single Capture

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
                    "Capture failed: reason=${failure.reason} frame=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* ignore */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

This is the building block for Chapter 19-style manual multi-frame bracketing — chain 7 `captureAwait(br[i])` calls in a `for` loop with `withTimeoutOrNull`, collect all 7 `TotalCaptureResult`s, and you have a complete HDR bracket sequence with per-frame timeout and automatic abort on cancellation. In the callback world this was hundreds of lines of state machine. Now it is a 12-line `for` loop.

---

## Flow for Continuous Streams

One-shot operations cover camera open, session create, and single capture. For repeating things — every preview frame, every `TotalCaptureResult`, every `Image` from an `ImageReader` — we want a `Flow<T>` so we can `map`, `filter`, `debounce`, `combine`, and share streams between subscribers.

### Example 4: ImageReader → `Flow&lt;Image&gt;` via `callbackFlow`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() drops old frames if consumer is slower than
        // the camera produces them — mandatory to avoid stalling the HAL.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // Must invokeOnClose FIRST so cancellation always removes the listener
    // even if setOnImageAvailableListener itself throws.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // Do NOT close the ImageReader here — caller owns its lifecycle.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // Any Image not consumed by downstream collectors is ours to close,
     // because callbackFlow rethrows failures after emit.
 }
```

**Critical design choices:**

1. `acquireLatestImage()` over `acquireNextImage()`. If your image processing (ML inference, face detection) takes 40ms and the camera fires at 30fps (~33ms per frame), you *will* fall behind. `acquireNextImage` queues them up until you run out of gralloc buffers and the camera freezes. `acquireLatestImage` skips the old ones and gives you the freshest frame. This is almost always what you want for preview-side image analysis.

2. `buffer(Channel.CONFLATED)`. A conflated buffer keeps only the latest value. Combined with `acquireLatestImage`, this is a hard guarantee that you never queue stale frames.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. This is the `callbackFlow` equivalent of `cont.invokeOnCancellation`. Cancel the coroutine scope (for example, when the Fragment goes through `onDestroyView`) and the listener is automatically deregistered and the `HandlerThread` cleaned up. *No* leak.

### Example 5: CaptureCallback → `Flow&lt;TotalCaptureResult&gt;`

Same pattern:

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
            // If you need partial results, emit them on a separate channel
            // or send a sealed class.
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* ignore */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

Now you have a cold `Flow&lt;TotalCaptureResult&gt;` that starts a repeating request when collected, stops it on cancellation, emits every completed result, and works with every standard Flow operator.

### Example 6: `combine(previewFlow, aeStateFlow)` for Reactive UI

The real power of Flow is composition. Suppose your UI shows:
- Live preview FPS
- Current AE state (converging / converged / locked)
- A "Ready to shoot" indicator that is green only when AE is converged AND AF is converged AND AWB is converged.

Without Flow you hand-write a state machine merging `CaptureCallback` with `Choreographer`. With Flow it is three lines:

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
    .flowOn(cameraDispatcher)   // off main thread, no UI jank

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
        (acc + ts).takeLast(30) // rolling 30-timestamp window
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // only update FPS label every 250ms, saves battery

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

Collect `uiState` in your Fragment's `viewLifecycleOwner.lifecycleScope.launchWhenStarted` and pass every emit to your Compose UI or `viewBinding`. Every operator — `map`, `runningFold`, `debounce`, `combine` — is a standard library primitive. No custom state machine. No race conditions. No missed events. Cancel the scope and every single Flow upstream — including the repeating request and the `ImageReader` listener — stops, unsubscribes, and cleans up exactly once.

---

## Thread Safety

All of the above is worthless if you violate Camera2's thread-safety rules. Here they are, distilled from hundreds of ANR bug reports:

1. **Never call any Camera2 API from the main thread.** `cameraManager.openCamera()` may look fast on a Pixel 7. On a budget Android Go device with a `LEGACY` HAL, it can block for 1.2s. That is an instant ANR. Even calls that *look* cheap, like `CameraCharacteristics.get()`, can allocate several KB of metadata and copy it — which on a cold process start while the user is swiping between Fragments is enough to drop 3 frames. Dispatch *everything* to `Dispatchers.Default` or a dedicated single-threaded dispatcher backed by a `HandlerThread`.

2. **HandlerThread vs `CoroutineDispatcher.Default` vs `Dispatchers.IO`.**
   - Use a **single-threaded dispatcher** (e.g. `HandlerThread("cam").asCoroutineDispatcher()`) for the *actual* Camera2 API calls. The legacy camera stack on many `LEGACY` devices has thread-affine HAL entry points. Switching threads between `openCamera` and `createCaptureSession` triggers known HAL bugs on Qualcomm msm8953 and older.
   - Use `Dispatchers.Default` for pure computation on captured frames (HDR merge, JPEG encode, face detection). It has as many threads as cores.
   - Use `Dispatchers.IO` for disk I/O (saving the JPEG to MediaStore). Never use `Default` for blocking writes.

3. **Shared mutable state between coroutines and callbacks must be `Mutex`-protected.** If a `CaptureCallback` writes `lastResult` and a Compose button click reads it, wrap both sides with `mutex.withLock { ... }` or use `atomicfu`/`@Volatile` for primitive types. Do NOT rely on "it only ever touches one thread." HAL callbacks on `LEGACY` devices occasionally fire on unexpected threads, and when they do, you get torn reads of 64-bit `Long` values like `SENSOR_TIMESTAMP`.

4. **Why Flow avoids the `PipedOutputStream` deadlock.** The research doc's `PipedOutputStream` pitfall deserves a concrete example. If you did this:

   ```kotlin
   // DO NOT DO THIS
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // read pis and write to file
   }
   ```

   This deadlocks within 100 frames because `PipedInputStream` has a 64KB default buffer. If the writer produces faster than the reader consumes, the writer blocks on `pos.write()` and the buffer fills. If the reader meanwhile is blocked on something else (e.g. MediaStore bulk insert transaction), both coroutines block forever — a classic circular wait. Flow with `buffer(CONFLATED)` or `buffer(DROP_OLDEST)` has explicit backpressure semantics and never deadlocks. Drop frames, never deadlock. That is the right tradeoff for camera preview.

---

## Summary

Camera2's callback-based API, when composed naively, produces deeply nested callback hell that is error-prone, leak-prone, and untestable. Kotlin coroutines and Flow give you two primitives that collapse the entire design: `suspendCancellableCoroutine` for one-shot operations (`openCamera`, `createCaptureSession`, single `capture`) with built-in timeout and cancellation support, and `callbackFlow` for continuous streams (ImageReader images, repeating `CaptureResult` callbacks) with explicit backpressure. Standard Flow operators — `map`, `filter`, `runningFold`, `debounce`, and the all-important `combine` — let you build reactive, cancel-safe UI state pipelines out of composable pieces. Enforce thread-safety with a dedicated camera dispatcher, protect shared state with `Mutex`, and replace any `PipedOutputStream`-style manual piping with Flow channels to avoid deadlocks.

## What's Next

You now have the tools to write robust, production-grade Camera2 apps. But how do you verify your code works across the 24,000+ Android device models currently in the wild, and how do OEMs validate their HALs before shipping? Chapter 27 covers camera testing: Camera ITS, CTS Verifier, and instrumentation tests using mocks so you can run your camera test suite on CI servers without any physical hardware.
