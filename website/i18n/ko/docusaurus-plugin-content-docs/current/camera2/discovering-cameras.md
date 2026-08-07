---
sidebar_position: 6
title: "제6장: 카메라 탐색하기"
description: "기기의 카메라들을 열거하고 특징을 파악하는 방법을 알아보세요. 논리적/물리적 카메라 ID의 차이, LENS_FACING을 통한 방향 구분, 그리고 가장 중요한 INFO_SUPPORTED_HARDWARE_LEVEL을 확인하여 기기의 성능 한계를 파악하는 법을 배웁니다."
keywords: [안드로이드 camera2 CameraCharacteristics, cameraIdList, LENS_FACING, 하드웨어 레벨, LEGACY, LIMITED, FULL, LEVEL_3, 카메라 메타데이터 탐색]
---

# 제6장: 카메라 탐색하기

5장에서 프로젝트 설정을 마쳤다면, 이제 우리 앱이 돌아가고 있는 기기에 어떤 카메라들이 달려 있는지 살펴볼 차례입니다. 현대의 스마트폰은 뒷면에만 3-4개의 렌즈가 달려 있는 경우가 흔합니다. Camera2 API는 이 카메라들을 어떻게 구분하고, 각각 어떤 기능을 가지고 있는지 알려줄까요?

이 장에서는 카메라의 "신분증"과도 같은 `CameraCharacteristics`를 읽어오는 법을 배웁니다. 코드를 작성하기 전에 [Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)을 실행해 보세요. 이 앱의 첫 화면이 바로 이번 장에서 우리가 구현할 내용의 완성본입니다.

---

## 1. 카메라 ID 목록 가져오기

모든 카메라는 고유한 문자열 ID를 가집니다. `CameraManager`에게 이 목록을 물어보는 것으로 시작합니다.

```kotlin
val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
val cameraIdList = manager.cameraIdList // 보통 ["0", "1", "2", ...] 형태로 반환됩니다.
```

- **ID "0"**: 관습적으로 메인 후면 카메라를 의미합니다.
- **ID "1"**: 관습적으로 메인 전면 카메라를 의미합니다.
- **그 외**: 초광각, 망원, 혹은 여러 렌즈를 하나로 묶은 "논리적 카메라" ID가 뒤따릅니다.

---

## 2. 방향 구분하기 (LENS_FACING)

각 ID가 가리키는 카메라가 앞을 보는지 뒤를 보는지 확인해야 합니다.

```kotlin
for (cameraId in cameraIdList) {
    val characteristics = manager.getCameraCharacteristics(cameraId)
    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

    when (facing) {
        CameraCharacteristics.LENS_FACING_FRONT -> Log.d("Camera", "ID $cameraId: 전면 카메라")
        CameraCharacteristics.LENS_FACING_BACK -> Log.d("Camera", "ID $cameraId: 후면 카메라")
        CameraCharacteristics.LENS_FACING_EXTERNAL -> Log.d("Camera", "ID $cameraId: 외부 USB 카메라")
    }
}
```

---

## 3. 핵심: 하드웨어 레벨 (Hardware Level)

Camera2 API를 사용할 때 가장 먼저 확인해야 할 **가장 중요한 값**입니다. 기기가 Camera2 API를 얼마나 제대로 지원하는지를 나타내는 척도입니다.

| 레벨 | 설명 |
|:---|:---|
| **LEGACY** | 아주 오래된 기기. Camera2 API를 흉내만 낼 뿐, 내부적으로는 이전 API(Camera1)로 작동합니다. 수동 조절이 거의 불가능합니다. |
| **LIMITED** | 기본적인 Camera2 기능을 지원하지만, 고속 연사나 일부 수동 조절 기능이 빠져 있습니다. |
| **FULL** | Camera2의 핵심 기능을 모두 지원합니다. 프레임별 수동 노출, ISO 조절 등이 완벽하게 작동합니다. |
| **LEVEL_3** | 최상급 플래그십 기기. RAW 캡처, YUV 재처리 등 전문가용 고급 기능을 모두 포함합니다. |

```kotlin
val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
// 이 레벨에 따라 여러분의 앱에서 '수동 모드' 버튼을 보여줄지 말지 결정해야 합니다.
```

---

## 4. 기타 유용한 정보들

`CameraCharacteristics` 객체에는 수백 가지의 정보가 담겨 있습니다. 몇 가지만 더 살펴볼까요?

- **센서 해상도**: `SENSOR_INFO_PIXEL_ARRAY_SIZE`를 통해 48MP인지 12MP인지 알 수 있습니다.
- **초점 거리**: `LENS_INFO_AVAILABLE_FOCAL_LENGTHS`를 통해 광각 렌즈인지 망원 렌즈인지 구분합니다.
- **플래시 유무**: `FLASH_INFO_AVAILABLE`이 true인 카메라에만 플래시 버튼을 보여줘야 합니다.

---

## 요약

1. `manager.cameraIdList`로 기기에 있는 카메라들을 찾습니다.
2. `LENS_FACING`으로 앞뒤 방향을 구분합니다.
3. `INFO_SUPPORTED_HARDWARE_LEVEL`을 확인하여 우리 앱의 고급 기능을 활성화할지 결정합니다.

## 다음 단계

이제 어떤 카메라를 쓸지 결정했습니다. 이제 그 카메라를 진짜로 "열어서" 사용할 준비를 해야 합니다. **제7장: 카메라 열기**에서는 비동기 방식으로 카메라 장치에 연결하고, 다른 앱과의 충돌을 방지하는 안전한 방법을 배웁니다.
