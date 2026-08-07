---
sidebar_position: 21
title: "제21장: HDR 및 Ultra HDR"
description: "안드로이드 Camera2의 HDR 기능을 마스터하세요. HDR10 및 HLG 비디오를 위한 10비트 다이내믹 레인지 프로필과 안드로이드 14의 혁신적인 Ultra HDR (JPEG_R) 사진 포맷을 사용하는 방법을 배웁니다."
keywords: [안드로이드 camera2 HDR, Ultra HDR, JPEG_R, HDR10, HLG, 10비트 카메라, DynamicRangeProfiles, 안드로이드 14 카메라]
---

# 제21장: HDR 및 Ultra HDR

우리는 3장에서 계산 사진학으로서의 HDR(여러 장의 노출을 합치는 기법)에 대해 배웠습니다. 이제는 이를 넘어, 안드로이드 시스템이 하드웨어 차원에서 지원하는 최신 **HDR(High Dynamic Range)** 표준을 코드로 다루는 법을 배웁니다.

특히 안드로이드 14(API 34)에서 도입된 **Ultra HDR**은 카메라 앱 개발자에게 혁명과도 같습니다. 기존 JPEG의 호환성을 유지하면서도, HDR 디스플레이에서 눈이 부실 정도로 밝은 하이라이트를 표현할 수 있기 때문입니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Streams** 탭에서 여러분의 기기가 `HLG`, `HDR10`, `JPEG_R` 등의 다이내믹 레인지 프로필을 지원하는지 지금 확인해 보세요.

---

## 1. 10비트 HDR 비디오 (HLG, HDR10)

기본적인 카메라 스트림은 8비트(채널당 256단계)의 SDR 색상을 사용합니다. 안드로이드 13(API 33)부터는 하드웨어가 지원하는 경우 10비트(1024단계) HDR 스트림을 공식적으로 요청할 수 있습니다.

### 지원 프로필 확인하기

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
val profiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)

if (profiles?.supportedProfiles?.contains(DynamicRangeProfiles.HLG10) == true) {
    Log.d("HDR", "이 카메라는 HLG10 HDR 비디오 촬영을 지원합니다.")
}
```

- **HLG10:** 방송 표준 HDR. SDR TV에서도 어느 정도 잘 보이며, HDR TV에서는 더 밝게 보입니다.
- **HDR10 / HDR10+:** 영화 표준 HDR. 메타데이터를 사용하여 장면별로 밝기를 최적화합니다.

### 10비트 스트림 설정

세션 구성 시 `OutputConfiguration`에 프로필을 지정합니다.

```kotlin
val outputConfig = OutputConfiguration(surface).apply {
    dynamicRangeProfile = DynamicRangeProfiles.HLG10
}
val sessionConfig = SessionConfiguration(..., listOf(outputConfig), ...)
cameraDevice.createCaptureSession(sessionConfig)
```

---

## 2. Ultra HDR 사진 (ImageFormat.JPEG_R)

안드로이드 14의 핵심 기능인 **Ultra HDR**은 파일 확장자는 `.jpg`로 똑같지만, 내부에 **게인 맵(Gain Map)**이라는 추가 정보를 담고 있습니다.

- **호환성:** HDR을 모르는 앱이나 기기에서는 평범한 8비트 사진으로 보입니다.
- **HDR 효과:** HDR 디스플레이를 갖춘 기기에서 사진을 열면, 하이라이트 부분의 밝기를 디스플레이의 최대 성능(예: 1000니트)까지 끌어올려 실제 현장에 있는 듯한 생동감을 줍니다.

### Ultra HDR 지원 확인 및 설정

Ultra HDR은 `ImageFormat.JPEG_R`이라는 새로운 포맷을 사용합니다.

```kotlin
val isUltraHdrSupported = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    ?.getOutputSizes(ImageFormat.JPEG_R)?.isNotEmpty() ?: false

if (isUltraHdrSupported) {
    val jpegRReader = ImageReader.newInstance(width, height, ImageFormat.JPEG_R, 2)
    // 이제 일반 JPEG 찍듯이 촬영하면 시스템이 자동으로 Ultra HDR 파일을 생성합니다.
}
```

---

## 3. HDR 촬영 시 고려사항

1. **디스플레이 제어:** 앱에서 HDR 사진이나 비디오를 제대로 보여주려면 창(Window) 설정에서 HDR 모드를 활성화해야 합니다.
   ```kotlin
   window.setColorMode(ActivityInfo.COLOR_MODE_HDR)
   ```
2. **성능 부하:** 10비트 데이터는 8비트보다 데이터량이 25% 많고 ISP 연산량도 늘어납니다. 발열과 배터리 소모에 주의하세요.
3. **편집의 어려움:** 일반적인 `Bitmap` 라이브러리나 필터 효과들은 8비트 SDR을 기준으로 설계되어 있습니다. HDR 데이터를 처리하려면 `Vulkan`이나 `OpenGL ES`에서 10비트 텍스처를 직접 다뤄야 할 수도 있습니다.

---

## 요약

- **HLG / HDR10**은 비디오를 위한 10비트 표준이며 `DynamicRangeProfiles`를 통해 설정합니다.
- **Ultra HDR (JPEG_R)**은 안드로이드 14의 혁신적인 사진 표준으로, 호환성과 화질을 모두 잡았습니다.
- HDR 기능을 사용하면 일반 앱과는 차원이 다른 시각적 경험을 사용자에게 제공할 수 있습니다.

## 다음 단계

이제 하드웨어가 제공하는 고화질 기능을 모두 섭렵했습니다. 하지만 이 기능들을 "수동"으로 조절하는 게 아니라, 제조사가 튜닝한 "자동" 모드로 편하게 쓰고 싶다면 어떻게 할까요? **제22장: 카메라 익스텐션**에서는 제조사의 야간 모드나 보케 모드를 그대로 빌려 쓰는 방법을 알아봅니다.
