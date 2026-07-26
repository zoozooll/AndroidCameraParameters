---
sidebar_position: 7
title: "7장: CameraCharacteristics 이해하기"
description: CameraCharacteristics를 탐색하여 렌즈 방향, 하드웨어 레벨, 센서 크기 및 기타 중요한 카메라 기능에 대해 알아봅니다.
keywords: [CameraCharacteristics, 렌즈 방향, 하드웨어 레벨, 센서 크기, 카메라 기능]
---

CameraCharacteristics는 카메라의 본질을 들여다볼 수 있는 창입니다. 함께 탐색해 봅시다.

## 소개

이전 장에서는 카메라를 나열하고 기본 정보를 얻는 방법을 배웠습니다. 이제 카메라 기능에 대한 포괄적인 설명인 **CameraCharacteristics**에 대해 더 깊이 알아보겠습니다.

CameraCharacteristics에는 수백 개의 매개변수가 포함되어 있습니다. 이 장에서는 가장 중요한 것들에 집중할 것입니다.

## CameraCharacteristics란 무엇인가요?

CameraCharacteristics는 카메라 장치에 대한 모든 메타데이터를 포함하는 불변 객체입니다. 다음을 설명합니다:

- **하드웨어 속성** — 센서 크기, 렌즈 특성
- **기능** — 카메라가 할 수 있는 것
- **모드** — 사용 가능한 초점, 노출, 화이트 밸런스 모드
- **출력 옵션** — 지원되는 해상도 및 형식
- **성능** — 프레임 레이트, 노출 범위

CameraManager에서 CameraCharacteristics를 얻습니다:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## 주요 CameraCharacteristics 키

가장 중요한 특성들을 살펴봅시다.

### 1. 렌즈 방향

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

가능한 값:
- `LENS_FACING_FRONT` — 전면 카메라 (셀카)
- `LENS_FACING_BACK` — 후면 카메라
- `LENS_FACING_EXTERNAL` — 외부 카메라

### 2. 하드웨어 레벨

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

이것은 가장 중요한 특성 중 하나입니다:

| 레벨 | API 레벨 | 기능 |
| --- | --- | --- |
| **LEGACY** | 21 | 제한된 Camera2 지원, 구식 Camera API 래핑 |
| **LIMITED** | 21 | 기본 Camera2 기능, 수동 제어 없음 |
| **FULL** | 21 | 완전한 수동 제어, RAW 캡처 |
| **LEVEL_3** | 24 | YUV 재처리 같은 고급 기능 |

### 3. 센서 크기

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width와 sensorSize.height가 치수를 알려줍니다
```

센서 크기는 센서에 픽셀이 몇 개 있는지 알려줍니다. 이것은 이미지 해상도와 다릅니다 — 센서에는 단일 캡처에 사용되는 것보다 더 많은 픽셀이 있을 수 있습니다.

### 4. 활성 배열 크기

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

활성 배열은 이미지를 캡처하는 데 사용되는 센서의 실제 영역입니다. 일부 픽셀은 캘리브레이션을 위해 예약되어 있기 때문에 보통 픽셀 배열보다 약간 작습니다.

### 5. 사용 가능한 기능

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

이 배열은 카메라가 어떤 기능을 지원하는지 알려줍니다:
- `BACKWARD_COMPATIBLE` — 기본 호환성
- `MANUAL_SENSOR` — 수동 센서 제어
- `MANUAL_POST_PROCESSING` — 수동 후처리
- `RAW` — RAW 캡처 지원
- `BURST_CAPTURE` — 연속 촬영
- `YUV_REPROCESSING` — YUV 재처리
- `DEPTH_OUTPUT` — 깊이 출력
- `CONSTRAINED_HIGH_SPEED_VIDEO` — 고속 비디오

### 6. 출력 형식

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

스트림 구성 맵에는 카메라가 지원하는 모든 출력 형식과 크기가 포함되어 있습니다:
- `ImageFormat.JPEG` — 표준 JPEG
- `ImageFormat.RAW_SENSOR` — RAW 센서 데이터
- `ImageFormat.YUV_420_888` — YUV 형식
- `ImageFormat.RAW10` — 10비트 RAW
- `ImageFormat.RAW12` — 12비트 RAW

### 7. 지원되는 미리보기 크기

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

이것은 카메라의 모든 사용 가능한 미리보기 해상도를 제공합니다.

### 8. 지원되는 사진 크기

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

이것들은 정지 이미지 캡처에 사용 가능한 해상도입니다.

### 9. 초점 거리

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

이 배열에는 렌즈의 초점 거리(밀리미터 단위)가 포함되어 있습니다. 여러 값은 광학 줌 기능을 나타냅니다.

### 10. 초점 거리 범위

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

최소 초점 거리는 카메라가 얼마나 가까이에서 초점을 맞출 수 있는지 알려줍니다. 값이 작을수록 매크로 기능이 더 좋습니다.

## 실제 예제

더 자세한 카메라 정보 앱을 만들어 봅시다:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // 렌즈 방향
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        else -> "External"
    }
    
    // 하드웨어 레벨
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Unknown"
    }
    
    // 센서 크기
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // 활성 배열 크기
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // 초점 거리
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Unknown"
    
    // 사용 가능한 기능
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
            else -> "Unknown capability"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Camera $cameraId ===")
    Log.d("CameraDetails", "Lens Facing: $lensFacingStr")
    Log.d("CameraDetails", "Hardware Level: $hardwareLevelStr")
    Log.d("CameraDetails", "Sensor Size: $sensorSizeStr")
    Log.d("CameraDetails", "Active Array: $activeArrayStr")
    Log.d("CameraDetails", "Focal Lengths: $focalLengthsStr")
    Log.d("CameraDetails", "Capabilities: ${capabilitiesList.joinToString(", ")}")
}
```

## 출력 예제

```
=== Camera 0 ===
Lens Facing: Back
Hardware Level: FULL
Sensor Size: 4032 x 3024
Active Array: 4000 x 3000
Focal Lengths: 2.4mm, 4.8mm
Capabilities: Backward Compatible, Manual Sensor, RAW Capture, Burst Capture
```

## CameraCharacteristics가 중요한 이유

카메라를 열거나 캡처 세션을 만들기 전에 **반드시** CameraCharacteristics를 확인해야 합니다:

1. **기능 확인** — 기능이 지원된다고 가정하지 마세요
2. **올바른 카메라 선택** — 렌즈 방향, 하드웨어 레벨 등을 기준으로 선택하세요
3. **출력 구성** — 지원되는 해상도와 형식을 사용하세요
4. **기기 차이 처리** — 한 기기에서 작동하는 것이 다른 기기에서는 작동하지 않을 수 있습니다

## Android Camera Parameters로 탐색하기

Android Camera Parameters 앱을 열고 특성을 살펴보세요. 카테고리별로 구성된 수백 개의 매개변수를 볼 수 있습니다:

- **카메라 정보** — 기본 카메라 정보
- **센서** — 센서 특성
- **렌즈** — 렌즈 속성
- **제어** — 자동 노출, 자동 초점, 화이트 밸런스
- **스케일러** — 출력 크기 및 형식
- **플래시** — 플래시 기능
- **통계** — 통계 출력

이것은 카메라 기능에 대한 완전한 그림을 제공합니다.

## 다음 장

이제 CameraCharacteristics를 이해했으니, 첫 번째 카메라를 열 준비가 되었습니다! 다음 장에서는 다음을 수행합니다:

1. CameraDevice에 대해 배우기
2. CameraManager를 사용하여 카메라 열기
3. 카메라 상태 콜백 처리하기
4. 카메라 수명 주기 이해하기

## 요약

CameraCharacteristics에는 카메라의 기능을 이해하는 데 필요한 모든 정보가 포함되어 있습니다:

- **렌즈 방향** — 전면, 후면, 또는 외부
- **하드웨어 레벨** — LEGACY, LIMITED, FULL, LEVEL_3
- **센서 크기** — 물리적 치수
- **활성 배열** — 캡처 영역
- **초점 거리** — 렌즈 기능
- **기능** — 지원되는 기능
- **출력 형식** — 사용 가능한 이미지 형식

카메라를 사용하기 전에 항상 CameraCharacteristics를 확인하세요. 이것은 앱이 다양한 기기에서 작동하도록 보장합니다.

다음 장에서는 CameraDevice를 사용하여 첫 번째 카메라를 열어 보겠습니다.
