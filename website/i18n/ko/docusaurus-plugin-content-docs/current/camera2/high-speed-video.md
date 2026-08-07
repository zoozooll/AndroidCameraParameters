---
sidebar_position: 19
title: "제19장: 고속 비디오와 슬로우 모션"
description: "안드로이드 Camera2의 고속 비디오 캡처 기능을 마스터하세요. CameraConstrainedHighSpeedCaptureSession을 사용하여 120fps 및 240fps 비디오를 녹화하는 방법, 하드웨어 제약 사항 이해, 그리고 매끄러운 슬로우 모션 파이프라인 구축 방법을 배웁니다."
keywords: [안드로이드 camera2 고속 비디오, 120fps 녹화, 240fps 슬로우 모션, CameraConstrainedHighSpeedCaptureSession, createHighSpeedRequestList, 고속 비디오 제약 사항, 안드로이드 카메라 개발]
---

# 제19장: 고속 비디오와 슬로우 모션

현대의 플래그십 스마트폰은 눈으로 볼 수 없는 빠른 순간을 포착할 수 있습니다. 물방울이 튀는 찰나나 운동선수의 역동적인 움직임을 부드러운 슬로우 모션으로 담아내는 것은 매우 매력적인 기능입니다. 이를 위해 카메라는 일반적인 30fps보다 훨씬 빠른 **120fps, 240fps, 심지어 960fps**의 속도로 프레임을 캡처해야 합니다.

안드로이드 Camera2 API는 이를 위해 일반 세션과는 다른 **"제한된 고속 캡처 세션(Constrained High Speed Capture Session)"**이라는 특수한 모드를 제공합니다. 이 장에서는 고속 촬영을 위해 세션을 구성하는 법과 하드웨어의 한계를 다루는 법을 배웁니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Streams** 탭에서 여러분의 기기가 어떤 고속 해상도와 FPS 조합을 지원하는지(`getHighSpeedVideoFpsRanges()`) 즉시 확인할 수 있습니다.

---

## 1. 고속 비디오의 원리: 대역폭과의 싸움

고속 비디오 촬영의 가장 큰 적은 **데이터 대역폭**입니다. 4K 해상도로 240fps를 촬영하려면 1초에 약 20억 개 이상의 픽셀 데이터를 처리해야 합니다. 이는 현대 스마트폰 ISP(이미지 신호 프로세서)의 한계를 넘어서는 경우가 많습니다.

따라서 안드로이드의 고속 모드에는 몇 가지 엄격한 **제약 사항**이 따릅니다.

- **해상도 제한:** 보통 720p나 1080p로 제한됩니다. 4K 고속 촬영은 최신 플래그십 기기에서만 가능할 수 있습니다.
- **스트림 수 제한:** 미리보기용 서피스(Surface) 하나와 녹화용 서피스 하나, 딱 **두 개**만 사용할 수 있습니다. `ImageReader`를 동시에 돌려 분석하는 등의 작업은 불가능합니다.
- **자동 기능 제한:** 3A(노출, 초점, 화이트 밸런스) 알고리즘이 고속 모드에 맞춰 단순화되거나 제한적으로 작동합니다.

---

## 2. 지원 여부 및 FPS 범위 확인

코드를 작성하기 전에 기기가 고속 촬영을 지원하는지, 어떤 속도가 가능한지 확인해야 합니다.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// 고속 촬영 지원 여부 확인
val isHighSpeedSupported = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) ?: false

if (isHighSpeedSupported) {
    // 지원하는 FPS 범위 목록 출력 (예: [120, 120], [240, 240])
    val ranges = configMap?.highSpeedVideoFpsRanges
    ranges?.forEach { range ->
        Log.d("HighSpeed", "지원 FPS 범위: ${range.lower} - ${range.upper}")
    }
}
```

---

## 3. 고속 캡처 세션 생성하기

고속 촬영을 위해서는 전용 메서드인 `createConstrainedHighSpeedCaptureSession`을 호출해야 합니다.

```kotlin
// 1. 녹화 및 미리보기용 서피스 준비
val surfaces = listOf(previewSurface, recorderSurface)

// 2. 고속 전용 세션 생성
cameraDevice.createConstrainedHighSpeedCaptureSession(
    surfaces,
    object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            val highSpeedSession = session as CameraConstrainedHighSpeedCaptureSession
            startHighSpeedRecording(highSpeedSession)
        }
        // ... 생략 ...
    }, backgroundHandler
)
```

---

## 4. 고속 요청 리스트 생성 (핵심 단계)

고속 모드에서는 단일 요청(`capture`)이나 반복 요청(`setRepeatingRequest`) 대신, 여러 개의 요청을 묶은 **리스트**를 제출해야 합니다. 이는 하드웨어가 초당 수백 프레임을 처리할 수 있도록 미리 "배치 처리" 지시를 내리는 것과 같습니다.

```kotlin
private fun startHighSpeedRecording(session: CameraConstrainedHighSpeedCaptureSession) {
    // TEMPLATE_RECORD를 기반으로 요청 빌더 생성
    val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
        addTarget(previewSurface)
        addTarget(recorderSurface)
        // 240fps 범위를 선택했다고 가정
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(240, 240))
    }

    // ★ 중요: 고속 전용 메서드를 사용하여 요청 리스트를 생성합니다.
    // 이 메서드는 하드웨어 요구사항에 맞춰 요청을 자동으로 복제/최적화합니다.
    val highSpeedRequestList = session.createHighSpeedRequestList(builder.build())

    // 단일 요청이 아닌 리스트를 반복 요청으로 제출합니다.
    session.setRepeatingBurst(highSpeedRequestList, null, backgroundHandler)
}
```

---

## 5. 슬로우 모션 구현 시 주의사항

1. **조명(Lighting):** 240fps로 촬영한다는 것은 한 프레임을 찍는 시간이 1/240초보다 짧다는 뜻입니다. 빛을 모을 시간이 매우 짧기 때문에 일반적인 실내등 아래서는 영상이 매우 어둡고 노이즈가 많게 나옵니다. **밝은 야외 햇빛** 아래서 촬영하는 것이 가장 좋습니다.
2. **플리커(Flicker) 현상:** 초당 60회 깜빡이는 형광등 아래서 고속 촬영을 하면 줄무늬가 가거나 화면이 번쩍거리는 현상이 발생합니다. 이는 고속 촬영의 물리적 한계입니다.
3. **저장 방식:** 카메라 API는 프레임을 *빨리 찍어줄 뿐*입니다. 이를 슬로우 모션 비디오 파일로 만드는 것은 `MediaRecorder`나 `MediaMuxer`의 몫입니다. 예를 들어 240fps로 찍은 프레임들을 헤더 파일에 30fps로 재생하라고 기록하면 8배 느린 슬로우 모션 영상이 됩니다.

---

## 요약

- 고속 촬영은 **Constrained High Speed Capture Session**을 사용합니다.
- 미리보기와 녹화 서피스, 딱 **두 개**만 사용할 수 있는 강력한 제약이 있습니다.
- `createHighSpeedRequestList()`를 통해 생성된 **요청 리스트**를 `setRepeatingBurst()`로 실행해야 합니다.
- 짧은 노출 시간 때문에 **풍부한 광량**이 필수적입니다.

## 다음 단계

비디오의 속도를 높여봤으니 이제 카메라의 "눈"을 늘려보겠습니다. **제20장: 멀티 카메라와 렌즈 전환**에서는 요즘 폰들의 특징인 여러 개의 렌즈(광각, 망원 등) 사이를 매끄럽게 전환하고 동시에 사용하는 방법을 알아봅니다.
