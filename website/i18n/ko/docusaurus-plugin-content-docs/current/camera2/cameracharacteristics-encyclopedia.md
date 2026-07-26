---
sidebar_position: 15
title: "제15장: CameraCharacteristics 백과사전"
description: 가장 중요한 Camera2 특성에 대한 종합 가이드 — 의미, 존재 이유, 사용 방법을 포함합니다.
keywords: [CameraCharacteristics, 카메라 파라미터, 카메라 기능, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

CameraCharacteristics 백과사전에 오신 것을 환영합니다 — 모든 카메라 파라미터를 이해하기 위한 가이드입니다.

## 소개

CameraCharacteristics에는 카메라의 기능을 설명하는 수백 개의 파라미터가 포함되어 있습니다. 이 장에서는 가장 중요한 파라미터들을 심도 있게 살펴보겠습니다:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — 카메라는 무엇을 할 수 있나요?
2. `REQUEST_AVAILABLE_CAPABILITIES` — 어떤 기능들을 사용할 수 있나요?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — 센서 크기는 얼마인가요?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — 줌은 얼마나 가능한가요?
5. `CONTROL_AE_AVAILABLE_MODES` — 어떤 노출 모드가 있나요?

그리고 더 많은 것들...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**무슨 뜻인가요?**  
이것이 가장 중요한 특성입니다. 카메라 장치의 전반적인 기능 레벨을 정의합니다.

**왜 존재하나요?**  
Android 기기마다 카메라 기능이 다릅니다. 이 파라미터는 애플리케이션이 무엇을 할 수 있는지 이해하는 데 도움을 줍니다.

**지원되는 값:**

| 값 | API 레벨 | 설명 |
| --- | --- | --- |
| `LEGACY` | 21 | 구형 기기, Camera2 API는 구형 Camera API의 래퍼입니다 |
| `LIMITED` | 21 | 기본적인 Camera2 기능, 수동 제어 없음 |
| `FULL` | 21 | 완전한 수동 제어, RAW 캡처, 버스트 캡처 |
| `LEVEL_3` | 24 | YUV 재처리, 10-bit HDR 같은 고급 기능 |

**어떻게 사용하나요?**  
고급 작업을 시도하기 전에 이것을 확인하세요:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // 제한된 기능
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // 기본 기능만
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // 완전한 수동 제어 가능
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // 고급 기능 사용 가능
    }
}
```

**Android Camera Parameters로 확인하는 방법:**  
앱을 열고 Camera Info 섹션에서 "Hardware Level"을 찾으세요.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**무슨 뜻인가요?**  
이 배열은 카메라가 지원하는 모든 기능을 나열합니다.

**왜 존재하나요?**  
같은 하드웨어 레벨 내에서도 기기마다 지원하는 기능이 다를 수 있습니다.

**일반적인 기능:**

| 기능 | 설명 |
| --- | --- |
| `BACKWARD_COMPATIBLE` | 기본 호환성 모드 |
| `MANUAL_SENSOR` | 수동 ISO 및 노출 제어 |
| `MANUAL_POST_PROCESSING` | 수동 색상 보정 및 노이즈 감소 |
| `RAW` | RAW 이미지 캡처 |
| `BURST_CAPTURE` | 고속 버스트 캡처 |
| `YUV_REPROCESSING` | YUV 이미지 재처리 |
| `DEPTH_OUTPUT` | 깊이 맵 출력 |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | 고속 비디오 캡처 |
| `LOGICAL_MULTI_CAMERA` | 여러 물리적 카메라를 결합한 논리적 카메라 |
| `CONCURRENT_CAMERA` | 여러 카메라를 동시에 열 수 있음 |
| `CAMERA_EXTENSION` | 제조사별 확장 기능(인물, 야간 모드) |

**어떻게 사용하나요?**  
기능을 사용하기 전에 지원 여부를 확인하세요:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // RAW 캡처 활성화
}
```

**Android Camera Parameters로 확인하는 방법:**  
Camera Info 섹션에서 "Available Capabilities"를 찾으세요.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**무슨 뜻인가요?**  
활성 어레이는 이미지 캡처에 사용되는 센서의 실제 영역입니다.

**왜 존재하나요?**  
센서 가장자리에는 보정용으로 예약된 픽셀이 있을 수 있습니다. 활성 어레이는 사용 가능한 영역을 나타냅니다.

**어떻게 사용하나요?**  
캡처에 사용 가능한 최대 해상도를 알려줍니다:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "활성 어레이: ${width}x$height")
```

**관련 특성:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — 센서의 총 픽셀 수(활성 어레이보다 클 수 있음)
- `SENSOR_INFO_SENSOR_SIZE` — 밀리미터 단위의 물리적 크기

**Android Camera Parameters로 확인하는 방법:**  
Sensor 섹션에서 "Active Array Size"와 "Sensor Size"를 찾으세요.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**무슨 뜻인가요?**  
카메라가 지원하는 최대 디지털 줌 배율입니다.

**왜 존재하나요?**  
디지털 줌은 이미지를 자르고 확대하므로 품질이 저하됩니다. 최대값을 알면 사용자 기대치를 관리하는 데 도움이 됩니다.

**어떻게 사용하나요?**  
캡처 요청에서 줌 레벨을 설정하세요:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// 줌 설정 (1.0 = 줌 없음, maxZoom = 최대 줌)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**관련 특성:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — 물리적 초점 거리(광학 줌용)

**Android Camera Parameters로 확인하는 방법:**  
Scaler 섹션에서 "Max Digital Zoom"을 찾으세요.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**무슨 뜻인가요?**  
사용 가능한 자동 노출 모드입니다.

**왜 존재하나요?**  
기기마다 지원하는 AE 전략이 다릅니다.

**일반적인 모드:**

| 모드 | 설명 |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | 수동 노출 제어 |
| `CONTROL_AE_MODE_ON` | 자동 노출 |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | 플래시가 항상 켜진 자동 노출 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | 자동 플래시가 있는 자동 노출 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | 적목 현상 감소가 있는 자동 노출 |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | 외장 플래시가 있는 자동 노출 |

**어떻게 사용하나요?**  
캡처 요청에서 AE 모드를 설정하세요:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Android Camera Parameters로 확인하는 방법:**  
Control 섹션에서 "AE Available Modes"를 찾으세요.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**무슨 뜻인가요?**  
사용 가능한 자동 초점 모드입니다.

**왜 존재하나요?**  
상황에 따라 다른 초점 전략이 필요합니다.

**일반적인 모드:**

| 모드 | 설명 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | 수동 초점 |
| `CONTROL_AF_MODE_AUTO` | 단일 촬영 자동 초점 |
| `CONTROL_AF_MODE_MACRO` | 매크로 초점 |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | 동영상용 연속 자동 초점 |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 사진용 연속 자동 초점 |
| `CONTROL_AF_MODE_EDGE` | 엣지 자동 초점 |
| `CONTROL_AF_MODE_FIXED` | 고정 초점(AF 없음) |

**어떻게 사용하나요?**  
사용 사례에 따라 AF 모드를 설정하세요:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Android Camera Parameters로 확인하는 방법:**  
Control 섹션에서 "AF Available Modes"를 찾으세요.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**무슨 뜻인가요?**  
사용 가능한 자동 화이트 밸런스 모드입니다.

**일반적인 모드:**

| 모드 | 설명 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | 수동 화이트 밸런스 |
| `CONTROL_AWB_MODE_AUTO` | 자동 |
| `CONTROL_AWB_MODE_INCANDESCENT` | 백열등 조명 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 형광등 조명 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 따뜻한 형광등 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 주광 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 흐린 날 |

**Android Camera Parameters로 확인하는 방법:**  
Control 섹션에서 "AWB Available Modes"를 찾으세요.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**무슨 뜻인가요?**  
렌즈의 사용 가능한 초점 거리입니다.

**왜 존재하나요?**  
여러 값이 있으면 광학 줌 기능이 있음을 나타냅니다.

**어떻게 사용하나요?**  
어떤 렌즈를 사용할 수 있는지 확인하세요:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "초점 거리: ${fl}mm")
}
```

**일반적인 초점 거리:**
- 2.4mm — 광각(일반적)
- 4.8mm — 망원(2배 광학 줌)
- 1.8mm — 초광각

**Android Camera Parameters로 확인하는 방법:**  
Lens 섹션에서 "Available Focal Lengths"를 찾으세요.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**무슨 뜻인가요?**  
렌즈가 초점을 맞출 수 있는 가장 가까운 거리입니다.

**왜 존재하나요?**  
값이 낮을수록 매크로 기능이 더 좋습니다.

**어떻게 사용하나요?**  
매크로 사진 촬영이 가능한지 확인하세요:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// 0.1m(10cm) 이하 값은 좋은 매크로 기능을 나타냅니다
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Android Camera Parameters로 확인하는 방법:**  
Lens 섹션에서 "Minimum Focus Distance"를 찾으세요.

---

## 10. FLASH_INFO_AVAILABLE

**무슨 뜻인가요?**  
카메라에 플래시가 있는지 여부입니다.

**왜 존재하나요?**  
모든 카메라에 플래시가 있는 것은 아닙니다(특히 전면 카메라).

**어떻게 사용하나요?**  
플래시를 사용하기 전에 확인하세요:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // 플래시 기능 활성화
}
```

**Android Camera Parameters로 확인하는 방법:**  
Flash 섹션에서 "Flash Available"을 찾으세요.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**무슨 뜻인가요?**  
지원되는 최소 및 최대 노출 시간입니다.

**왜 존재하나요?**  
저조도 성능과 움직임 정지 능력을 결정합니다.

**어떻게 사용하나요?**  
수동 제어를 위해 노출 범위를 확인하세요:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// 표시를 위해 초로 변환
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Android Camera Parameters로 확인하는 방법:**  
Sensor 섹션에서 "Exposure Time Range"를 찾으세요.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**무슨 뜻인가요?**  
지원되는 최소 및 최대 ISO 값입니다.

**왜 존재하나요?**  
저조도 성능과 노이즈 성능을 결정합니다.

**어떻게 사용하나요?**  
수동 제어를 위해 ISO 범위를 확인하세요:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Android Camera Parameters로 확인하는 방법:**  
Sensor 섹션에서 "Sensitivity Range"를 찾으세요.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**무슨 뜻인가요?**  
지원되는 모든 출력 크기와 형식입니다.

**왜 존재하나요?**  
사용할 수 있는 해상도와 형식을 결정합니다.

**어떻게 사용하나요?**  
다양한 사용 사례에 대한 지원 크기를 가져오세요:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// 프리뷰 크기
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// 사진 크기
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// 동영상 크기
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// RAW 크기
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Android Camera Parameters로 확인하는 방법:**  
Scaler 섹션에서 "Preview Sizes", "Picture Sizes" 등을 찾으세요.

---

## 14. LENS_FACING

**무슨 뜻인가요?**  
렌즈가 어느 방향을 향하고 있는지입니다.

**왜 존재하나요?**  
전면, 후면 또는 외장 카메라인지 결정합니다.

**값:**
- `LENS_FACING_FRONT` — 셀카 카메라
- `LENS_FACING_BACK` — 후면 카메라  
- `LENS_FACING_EXTERNAL` — 외장 카메라

**Android Camera Parameters로 확인하는 방법:**  
Camera Info 섹션에서 "Lens Facing"을 찾으세요.

---

## 15. CONTROL_MAX_REGIONS_AE

**무슨 뜻인가요?**  
AE 측광 영역의 최대 개수입니다.

**왜 존재하나요?**  
노출 측광이 얼마나 정밀할 수 있는지 결정합니다.

**어떻게 사용하나요?**  
생성하는 AE 영역의 수를 제한하세요:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// maxAERegions를 초과하지 않도록 생성
```

**Android Camera Parameters로 확인하는 방법:**  
Control 섹션에서 "Max Regions AE"를 찾으세요.

---

## 결론

CameraCharacteristics는 카메라 기능을 들여다보는 창입니다. 이러한 파라미터를 이해하면 다음을 할 수 있습니다:

1. **기기 독립적인 앱 구축** — 기능을 사용하기 전에 지원 여부 확인
2. **더 나은 사용자 경험 제공** — 사용 가능한 기능만 표시
3. **성능 최적화** — 적절한 해상도와 형식 선택
4. **전문가용 애플리케이션 제작** — 카메라의 잠재력을 최대한 발휘

## 더 알아보는 방법

1. **Android Camera Parameters 앱** — 기기의 실제 데이터 탐색
2. **Android 문서** — 공식 CameraCharacteristics 문서 읽기
3. **실험** — 작은 테스트 앱을 작성하여 다양한 파라미터 시도
4. **소스 코드** — 더 깊은 이해를 위해 Camera2 소스 코드 살펴보기

## 요약

이 장에서는 가장 중요한 CameraCharacteristics를 다루었습니다:

1. **하드웨어 레벨** — 전반적인 기능
2. **기능** — 사용 가능한 특정 기능들
3. **활성 어레이** — 센서 해상도
4. **디지털 줌** — 줌 기능
5. **AE/AF/AWB 모드** — 자동 제어 모드
6. **초점 거리** — 렌즈 기능
7. **최소 초점 거리** — 매크로 기능
8. **플래시** — 플래시 가용성
9. **노출/ISO 범위** — 수동 제어 한계
10. **스트림 구성** — 지원되는 크기와 형식

이 지식으로 이제 고급 Camera2 애플리케이션을 구축할 준비가 되었습니다!

---

## 마무리

축하합니다! 이 Android Camera2 시리즈를 완료했습니다. 이제 다음을 이해했습니다:

- **스마트폰 카메라가 작동하는 방식** — 렌즈, 센서, ISP
- **Camera2가 작동하는 방식** — CameraManager, CameraDevice, CaptureSession
- **사진을 캡처하는 방법** — JPEG, RAW, ImageReader
- **카메라를 제어하는 방법** — ISO, 노출, 초점, 화이트 밸런스
- **전문가 기능** — 고속 동영상, 멀티 카메라
- **카메라 특성** — 카메라 기능의 백과사전

Android Camera Parameters 앱은 계속 학습하기에 훌륭한 도구입니다. 기기의 기능을 탐색하고 다양한 설정을 실험해 보세요.

행복한 코딩! 📸
