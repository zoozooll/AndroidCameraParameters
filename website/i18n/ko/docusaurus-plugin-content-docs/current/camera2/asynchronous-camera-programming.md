---
sidebar_position: 26
title: "제26장: 비동기 카메라 프로그래밍"
description: "Kotlin 코루틴과 Flow를 사용하여 Camera2의 콜백 지옥을 길들이세요. 일회성 작업(openCamera, createCaptureSession, capture)을 위한 suspendCancellableCoroutine, 지속적인 ImageReader 및 CaptureResult 스트림을 위한 callbackFlow, 반응형 UI를 위한 combine 연산자, 그리고 ANR과 데드락을 방지하기 위한 스레드 안전 패턴을 배웁니다."
keywords: [코틀린 코루틴 camera2, 콜백 지옥, suspendcancellablecoroutine, callbackflow, camera2 flow, 스레드 안전 camera2, mutext 공유 상태, pipedoutputstream 데드락, 반응형 카메라 ui]
---

# 제26장: 비동기 카메라 프로그래밍

## 요약

7장에서 9장까지 작성했던 코드를 다시 돌아보세요. `CameraManager.openCamera` 내부에 중첩된 `CameraDevice.StateCallback`, `onOpened` 내부에 중첩된 `CameraCaptureSession.StateCallback`, `onConfigured` 내부에 중첩된 `CaptureCallback`, 그리고 직접 만들어 매 프레임 실행한 뒤 모든 에러 경로에서 정확히 역순으로 해제해야 하는 `HandlerThread`에서 돌아가는 `ImageReader.OnImageAvailableListener`까지. 이것이 바로 카메라 스타일의 콜백 지옥입니다. 들여쓰기 단계마다 새로운 콜백 클래스가 생깁니다. 모든 에러는 네 단계의 익명 객체를 거쳐 전파되어야 합니다. 해제 경로에서 `close()`를 하나라도 놓치면 재부팅 전까지 카메라가 유출(leak)됩니다.

이 장은 여러분이 갈망해 온 리팩토링 과정입니다. 두 가지 코루틴 프리미티브를 사용하여 이 복잡한 콜백 정글을 깨끗하고 선형적이며 취소 가능하고 테스트 가능한 코틀린 코드로 변환합니다. 일회성 작업에는 `suspendCancellableCoroutine`을, 지속적인 스트림에는 `callbackFlow` + `Flow` 연산자를 사용합니다. 코루틴과 Camera2가 상호작용할 때의 스레드 안전 규칙, 카메라 호출 시 메인 스레드를 차단하는 것이 왜 ANR을 유발하는지, 그리고 ImageWriter 데이터 전송을 위해 시도했을 법한 `PipedOutputStream`/`PipedInputStream` 패턴이 왜 Flow가 자연스럽게 피하는 데드락을 만드는지 배우게 될 것입니다.

항상 그렇듯이, 여러분의 비동기 파이프라인이 필요로 하는 기능(반복 연사, 부분 결과, YUV 재처리 등)이 실제 테스트 기기에서 지원되는지 **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters))를 통해 하드웨어 레벨의 기능을 검증하세요.

---

## 왜 중첩된 콜백이 "콜백 지옥"인가

먼저 문제를 시각화해 봅시다. 코루틴 도입 전 프로덕션 급 RAW Camera2 앱의 실제(간소화된) 구조입니다.

```mermaid
graph TD
    A["onCreateView"] -->|cameraId 선택됨| B["CameraManager.openCamera"]
    B -->|발생| C[StateCallback.onOpened<br/>람다 1]
    C -->|cameraDevice 보유| D[createCaptureSession<br/>(출력 = 미리보기Surface + imageReaderSurface)]
    D -->|발생| E[Session.StateCallback.onConfigured<br/>람다 2]
    E -->|session 보유| F[session.setRepeatingRequest<br/>+ CaptureCallback 람다 3]
    F -->|onProgress 발생| G[CaptureCallback.onCaptureProgressed<br/>부분 결과]
    F -->|onCompleted 발생| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|프레임 준비됨| I[ImageReader.OnImageAvailableListener<br/>람다 4]
    I -->|JPEG 바이트| J[MediaStore 저장 호출<br/>람다 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

색칠된 모든 콜백은 별도의 익명 클래스입니다. 각 콜백은 두 단계 상위의 리소스에 대한 참조를 캡처합니다. 모든 에러 경로는 J에서 A로 거꾸로 올라가며 `imageReader → session → cameraDevice → handlerThread`를 역순으로 닫아야 하며, 16가지 에러 조합 중 단 하나의 `close()`라도 누락되면 기기를 재부팅할 때까지 카메라 유출이 발생합니다. 이것이 바로 콜백 지옥의 정석입니다.

이 장의 목표는 이 스파게티 코드를 다음과 같이 바꾸는 것입니다.

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[UI 상태<br/>(단일 방출)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

선형적이고, 조합 가능하며, 테스트 가능합니다. 부모 `Job`을 취소하면 취소됩니다. 모든 단계는 평범한 함수나 `Flow` 연산자입니다. 기존의 5가지 콜백이 이제 12줄의 선형 파이프라인 안에 담깁니다.

---

## 일회성 작업을 위한 코틀린 코루틴: `suspendCancellableCoroutine`

콜백 기반 API를 `suspend` 함수로 래핑하는 핵심 패턴은 `suspendCancellableCoroutine`입니다. 레시피는 항상 동일합니다.

1. `suspendCancellableCoroutine { cont -> ... }`를 호출하여 `CancellableContinuation<T>`을 얻습니다.
2. 익명 콜백 구현을 전달하여 실제 콜백 기반 API를 호출합니다.
3. 콜백의 성공 경로에서 `cont.resume(value)`를 호출합니다.
4. 모든 에러 경로에서 `cont.resumeWithException(t)`를 호출합니다.
5. `cont.invokeOnCancellation { ... }`에서 정리를 수행합니다. 카메라를 닫고, 대기 중인 요청을 취소하고, 리스너를 해제하여 코루틴이 취소된 *후*에 콜백이 발생하는 것을 방지합니다.
6. 호출 측에서 전체를 `withTimeout`으로 감싸 죽은 HAL이 앱을 영원히 멈추게 하지 않도록 합니다.

### 예제 1: `suspend fun openCameraAwait()`

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
                    "카메라 $cameraId 가 여는 도중 연결 끊김"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "카메라 $cameraId 에러: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // 해결 방법: API 30 미만에서는 openCamera()가 취소 가능한 핸들을 노출하지 않음.
            // 경쟁 상태 창에서 장치가 열렸다면 닫음.
        } catch (_: Throwable) { /* 무시 */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**작동 원리:** `openCamera`는 한 번 실행하고 잊어버리는 방식입니다. 이를 호출하면 미래의 어느 시점에 세 가지 콜백 메서드 중 하나가 정확히 한 번 실행됩니다. 이 계약("정확히 한 번 실행")이 우리가 이를 컨티뉴에이션(continuation)과 일대일로 매칭할 수 있게 해줍니다. 콜백이 실행되기 *전*에 코루틴이 취소되면 `invokeOnCancellation`이 실행되어 리소스 유출을 방지합니다. `resume` *후*에 취소되면, `resume(value) { camera.close() }` 블록(즉, `resume`의 `onCancellation` 파라미터)이 자동으로 장치를 닫습니다.

타임아웃과 함께 호출하는 것은 매우 간단합니다.

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "5초 후 카메라 열기 타임아웃")
    return@launch
}
```

HAL이 멈춘 경우(이전 앱의 카메라 유출 후 저가형 `LEGACY` 장치에서 흔함), 사용자에게 "앱이 응답하지 않음" 대화상자를 보여주는 대신 빠르고 깨끗하게 실패 처리를 할 수 있습니다.

### 예제 2: `suspend fun createCaptureSessionAwait()`

동일한 패턴, 다른 콜백입니다.

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
                    "장치 ${this@createCaptureSessionAwait.id} 세션 구성 실패"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // 이전 API에서는 진행 중인 세션 생성을 취소할 수 없음.
        // 위의 resume onCancellation 블록을 통해 결국 완료되면 세션이 닫힘.
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

정확히 같은 형태입니다. 두 API 버전(S 미만의 `createCaptureSession(surfaces, callback, handler)`와 S 이상의 `SessionConfiguration`)이 하나의 래퍼 안에서 처리됩니다. 호출자는 알 필요가 없습니다.

### 예제 3: 단일 캡처를 위한 `suspend fun awaitCaptureResult()`

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
                    "캡처 실패: reason=${failure.reason} frame=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* 무시 */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

이것은 19장의 수동 다중 프레임 브래키팅을 위한 빌딩 블록이 됩니다. `for` 루프 안에서 `withTimeoutOrNull`과 함께 7번의 `captureAwait(br[i])` 호출을 연결하고 7개의 `TotalCaptureResult`를 모두 수집하면, 프레임별 타임아웃과 취소 시 자동 중단 기능이 포함된 완벽한 HDR 브래키팅 시퀀스가 완성됩니다. 콜백 세상에서는 수백 줄의 상태 머신이었던 것이 이제 12줄의 `for` 루프가 됩니다.

---

## 지속적인 스트림을 위한 Flow

일회성 작업은 카메라 열기, 세션 생성, 단일 캡처를 처리합니다. 반복되는 작업(모든 미리보기 프레임, 모든 `TotalCaptureResult`, `ImageReader`의 모든 `Image`)의 경우, 스트림 간에 `map`, `filter`, `debounce`, `combine` 및 구독자 간 스트림 공유를 할 수 있도록 `Flow<T>`를 원하게 됩니다.

### 예제 4: `callbackFlow`를 통한 ImageReader → `Flow&lt;Image&gt;`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // 소비자가 카메라가 생산하는 속도보다 느린 경우 오래된 프레임을 버림.
        // HAL 정지를 피하기 위해 필수적임.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // setOnImageAvailableListener 자체가 예외를 던지더라도 취소 시 항상
    // 리스너가 제거되도록 invokeOnClose를 먼저 호출해야 함.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // 여기서 ImageReader를 닫지 마세요 — 호출자가 수명 주기를 소유함.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // callbackFlow는 방출 후 실패 시 다시 던지므로(rethrow),
     // 다운스트림 수집자가 소비하지 않은 모든 Image는 우리가 닫아야 함.
 }
```

**중요한 설계 선택:**

1. `acquireNextImage()` 대신 `acquireLatestImage()`. 이미지 처리(ML 추론, 얼굴 인식)에 40ms가 걸리고 카메라가 30fps(~33ms/프레임)로 촬영한다면, 여러분은 *반드시* 뒤처지게 됩니다. `acquireNextImage`는 gralloc 버퍼가 바닥나 카메라가 얼어버릴 때까지 프레임을 큐에 쌓습니다. `acquireLatestImage`는 오래된 것을 건너뛰고 가장 신선한 프레임을 줍니다. 미리보기 측 이미지 분석에서는 거의 항상 이것을 원할 것입니다.

2. `buffer(Channel.CONFLATED)`. 합쳐진(conflated) 버퍼는 최신 값만 유지합니다. `acquireLatestImage`와 결합하여 오래된 프레임이 큐에 쌓이지 않도록 강력하게 보장합니다.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. 이것은 `cont.invokeOnCancellation`에 해당하는 `callbackFlow`의 기능입니다. 코루틴 스코프를 취소하면(예: 프래그먼트가 `onDestroyView`를 거칠 때) 리스너가 자동으로 해제되고 `HandlerThread`가 정리됩니다. 유출이 *전혀* 없습니다.

### 예제 5: CaptureCallback → `Flow&lt;TotalCaptureResult&gt;`

동일한 패턴입니다.

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
            // 부분 결과가 필요하다면 별도의 채널로 방출하거나
            // sealed class를 전송하세요.
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* 무시 */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

이제 수집(collect)될 때 반복 요청을 시작하고, 취소 시 중지하며, 모든 완료된 결과를 방출하고, 모든 표준 Flow 연산자와 호환되는 차가운(cold) `Flow&lt;TotalCaptureResult&gt;`를 갖게 되었습니다.

### 예제 6: 반응형 UI를 위한 `combine(previewFlow, aeStateFlow)`

Flow의 진짜 힘은 조합(composition)에 있습니다. UI에 다음을 표시한다고 가정해 봅시다.
- 라이브 미리보기 FPS
- 현재 AE 상태 (수렴 중 / 수렴됨 / 잠김)
- AE와 AF와 AWB가 모두 수렴되었을 때만 초록색이 되는 "촬영 준비 완료" 표시등.

Flow가 없다면 `CaptureCallback`과 `Choreographer`를 병합하는 상태 머신을 직접 짜야 합니다. Flow를 쓰면 세 줄입니다.

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
    .flowOn(cameraDispatcher)   // 메인 스레드 밖에서, UI 버벅임 없음

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
        (acc + ts).takeLast(30) // 이동하는 30개 타임스탬프 창
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // 250ms마다 FPS 레이블 업데이트, 배터리 절약

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

프래그먼트의 `viewLifecycleOwner.lifecycleScope.launchWhenStarted`에서 `uiState`를 수집하고 모든 방출값을 Compose UI나 `viewBinding`에 전달하세요. `map`, `runningFold`, `debounce`, `combine` 등 모든 연산자는 표준 라이브러리 프리미티브입니다. 커스텀 상태 머신도, 경쟁 상태(race conditions)도, 누락된 이벤트도 없습니다. 스코프를 취소하면 반복 요청과 `ImageReader` 리스너를 포함한 업스트림의 모든 Flow가 정확히 한 번 중지, 구독 해제 및 정리됩니다.

---

## 스레드 안전 (Thread Safety)

Camera2의 스레드 안전 규칙을 어긴다면 위의 모든 코드는 가치가 없습니다. 수백 개의 ANR 버그 리포트를 통해 정제된 규칙들입니다.

1. **메인 스레드에서 어떠한 Camera2 API도 호출하지 마세요.** `cameraManager.openCamera()`는 픽셀 7에서는 빨라 보일 수 있습니다. 하지만 `LEGACY` HAL을 가진 저가형 안드로이드 Go 기기에서는 1.2초 동안 차단될 수 있습니다. 이는 즉각적인 ANR입니다. `CameraCharacteristics.get()`처럼 가벼워 보이는 호출조차 수 KB의 메타데이터를 할당하고 복사할 수 있는데, 사용자가 프래그먼트 사이를 스와이프하는 동안 프로세스가 처음 시작될 때 실행된다면 프레임 3개를 드롭하기에 충분한 부하입니다. *모든 것*을 `Dispatchers.Default`나 `HandlerThread` 기반의 전용 싱글 스레드 디스패처로 전달하세요.

2. **HandlerThread 대 `CoroutineDispatcher.Default` 대 `Dispatchers.IO`.**
   - **실제 Camera2 API 호출**에는 **싱글 스레드 디스패처**(예: `HandlerThread("cam").asCoroutineDispatcher()`)를 사용하세요. 많은 `LEGACY` 기기의 레거시 카메라 스택은 스레드에 의존적인 HAL 엔트리 포인트를 가집니다. `openCamera`와 `createCaptureSession` 사이에서 스레드를 바꾸면 퀄컴 msm8953 이하 칩셋에서 알려진 HAL 버그를 유발합니다.
   - 캡처된 프레임에 대한 **순수 연산**(HDR 병합, JPEG 인코딩, 얼굴 인식)에는 `Dispatchers.Default`를 사용하세요. 코어 수만큼 스레드를 가집니다.
   - **디스크 I/O**(JPEG를 MediaStore에 저장)에는 `Dispatchers.IO`를 사용하세요. 차단(blocking)되는 쓰기 작업에 `Default`를 절대 사용하지 마세요.

3. **코루틴과 콜백 사이의 공유 가변 상태는 `Mutex`로 보호해야 합니다.** 만약 `CaptureCallback`이 `lastResult`를 쓰고 Compose 버튼 클릭이 이를 읽는다면, 양쪽을 `mutex.withLock { ... }`으로 감싸거나 기본 타입에 `atomicfu` / `@Volatile`을 사용하세요. "어차피 한 스레드에서만 만진다"는 말을 믿지 마세요. `LEGACY` 기기의 HAL 콜백은 가끔 예상치 못한 스레드에서 발생하며, 이때 `SENSOR_TIMESTAMP`와 같은 64비트 `Long` 값을 읽으면 데이터가 깨지는 현상(torn read)이 발생할 수 있습니다.

4. **Flow가 `PipedOutputStream` 데드락을 피하는 이유.** 연구 문서의 `PipedOutputStream` 함정은 구체적인 예시를 볼 가치가 있습니다. 만약 이렇게 했다면:

   ```kotlin
   // 이렇게 하지 마세요
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // pis를 읽어서 파일에 기록
   }
   ```

   이는 100프레임 이내에 데드락이 발생합니다. `PipedInputStream`의 기본 버퍼가 64KB이기 때문입니다. 쓰는 쪽이 읽는 쪽보다 빠르면 `pos.write()`에서 차단되고 버퍼가 가득 찹니다. 그 와중에 읽는 쪽이 다른 일(예: MediaStore 벌크 인서트 트랜잭션)로 차단되면 두 코루틴은 영원히 서로를 기다리는 전형적인 순환 대기 상태에 빠집니다. `buffer(CONFLATED)`나 `buffer(DROP_OLDEST)`를 사용하는 Flow는 명시적인 백프레셔(backpressure) 의미 체계를 가지므로 절대 데드락이 발생하지 않습니다. 프레임을 버릴지언정 데드락은 만들지 마세요. 그것이 카메라 미리보기에 적합한 트레이드오프입니다.

---

## 요약

Camera2의 콜백 기반 API를 나이브하게 구성하면 에러가 발생하기 쉽고, 리소스 유출 위험이 크며, 테스트 불가능한 깊게 중첩된 콜백 지옥이 만들어집니다. 코틀린 코루틴과 Flow는 전체 설계를 간소화하는 두 가지 프리미티브를 제공합니다. 내장된 타임아웃과 취소 지원 기능을 갖춘 일회성 작업(`openCamera`, `createCaptureSession`, 단일 `capture`)을 위한 `suspendCancellableCoroutine`, 그리고 명시적인 백프레셔를 갖춘 지속적인 스트림(`ImageReader` 이미지, 반복 `CaptureResult` 콜백)을 위한 `callbackFlow`입니다. `map`, `filter`, `runningFold`, `debounce` 그리고 무엇보다 중요한 `combine`과 같은 표준 Flow 연산자를 통해 조합 가능하고 취소 안전한 UI 상태 파이프라인을 구축할 수 있습니다. 전용 카메라 디스패처로 스레드 안전을 강제하고, 공유 상태를 `Mutex`로 보호하며, 데드락을 방지하기 위해 `PipedOutputStream` 스타일의 수동 파이핑 대신 Flow 채널을 사용하세요.

## 다음 단계

이제 견고하고 상용 수준인 Camera2 앱을 작성할 수 있는 도구를 갖추게 되었습니다. 하지만 여러분의 코드가 시중에 나와 있는 24,000개 이상의 안드로이드 기기 모델에서 잘 작동하는지 어떻게 확인할 수 있을까요? 그리고 OEM은 제품 출시 전에 자신의 HAL을 어떻게 검증할까요? 27장에서는 카메라 테스트를 다룹니다. 카메라 ITS, CTS Verifier, 그리고 물리적 하드웨어 없이도 CI 서버에서 카메라 테스트 스위트를 실행할 수 있는 모킹(mock)을 이용한 계측 테스트를 살펴봅니다.
