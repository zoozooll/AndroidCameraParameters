---
sidebar_position: 22
title: "제22장: 카메라 익스텐션"
description: "Camera2 익스텐션을 사용하여 야간, 보케, HDR, 얼굴 보정과 같은 OEM 전용 카메라 기능을 활용하세요. CameraExtensionCharacteristics를 통해 지원 여부를 쿼리하고, 익스텐션 세션을 구성하며, 복잡한 3A 관리 없이 제조사 수준의 이미지 품질을 구현하는 방법을 배웁니다."
keywords: [안드로이드 camera2 익스텐션, OEM 카메라 기능, 보케 모드, 야간 모드, 카메라 HDR, CameraExtensionSession, CameraExtensionCharacteristics, 안드로이드 12 카메라]
---

# 제22장: 카메라 익스텐션

안드로이드의 고질적인 문제는 "기본 카메라 앱은 사진이 잘 나오는데, 내가 만든 앱이나 인스타그램 앱은 왜 사진이 별로일까?"였습니다. 제조사(삼성, 구글 등)는 자신의 폰에 최적화된 야간 모드나 인물 사진 알고리즘을 숨겨두고 타사 앱에는 공개하지 않았기 때문입니다.

구글은 이를 해결하기 위해 안드로이드 10에서 **카메라 익스텐션(Camera Extensions)**을 도입했고, 안드로이드 12(API 31)에서 지금의 완성된 API로 정립했습니다. 익스텐션을 사용하면 복잡한 멀티 프레임 합성 로직을 직접 짤 필요 없이, **제조사가 미리 만들어둔 AI 파이프라인**을 함수 호출 한 번으로 빌려 쓸 수 있습니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Extensions** 탭에서 여러분의 폰이 어떤 익스텐션(야간, 보케, HDR 등)을 지원하는지 실시간으로 확인하고 직접 촬영해 볼 수 있습니다.

---

## 1. 지원되는 5가지 익스텐션 유형

제조사는 다음 5가지 표준 익스텐션을 제공할 수 있습니다.

| 익스텐션 | 설명 |
|:---|:---|
| `EXTENSION_NIGHT` | 저조도에서 여러 장을 합성하여 밝고 노이즈 없는 야경 사진을 만듭니다. |
| `EXTENSION_BOKEH` | 인물 사진 모드. 배경을 흐리게 처리하여 피사체를 돋보이게 합니다. |
| `EXTENSION_HDR` | 밝은 곳과 어두운 곳의 디테일을 모두 살리는 고다이내믹 레인지 촬영을 수행합니다. |
| `EXTENSION_FACE_RETOUCH` | 피부 톤을 매끄럽게 하거나 얼굴 윤곽을 보정합니다. |
| `EXTENSION_AUTOMATIC` | 장면에 따라 위 4가지 중 최적의 모드를 시스템이 알아서 선택합니다. |

---

## 2. 익스텐션 지원 여부 확인하기

익스텐션은 선택 사항입니다. 모든 폰이 모든 모드를 지원하지 않으므로 `CameraExtensionCharacteristics`를 통해 먼저 물어봐야 합니다.

```kotlin
val extensionChars = cameraManager.getCameraExtensionCharacteristics(cameraId)
val supportedExtensions = extensionChars.supportedExtensions

if (supportedExtensions.contains(CameraExtensionCharacteristics.EXTENSION_NIGHT)) {
    Log.d("Extension", "이 기기는 야간 모드를 지원합니다!")
}
```

---

## 3. 익스텐션용 해상도 선택

익스텐션은 일반 미리보기보다 훨씬 무거운 연산을 수행하므로, 지원되는 해상도가 매우 제한적일 수 있습니다. 반드시 전용 메서드로 해상도를 확인해야 합니다.

```kotlin
// 야간 모드에서 사용할 수 있는 JPEG 해상도 목록 가져오기
val extensionSizes = extensionChars.getExtensionSupportedSizes(
    CameraExtensionCharacteristics.EXTENSION_NIGHT, ImageFormat.JPEG
)
val bestSize = extensionSizes.first() // 가장 큰 크기 선택
```

---

## 4. 익스텐션 세션 생성 및 촬영

익스텐션은 일반 `CameraCaptureSession` 대신 `CameraExtensionSession`을 사용합니다.

```kotlin
// 1. 출력 구성 (OutputConfiguration) 생성
val outputConfig = ExtensionSessionConfiguration(
    CameraExtensionCharacteristics.EXTENSION_NIGHT,
    listOf(OutputConfiguration(previewSurface), OutputConfiguration(jpegSurface)),
    executor,
    object : CameraExtensionSession.StateCallback() {
        override fun onConfigured(session: CameraExtensionSession) {
            // 2. 익스텐션 전용 반복 요청 시작
            session.setRepeatingRequest(previewRequestBuilder.build(), executor, null)
        }
        // ... 생략 ...
    }
)

// 3. 세션 생성 명령
cameraDevice.createExtensionSession(outputConfig)
```

촬영 시에도 일반 `capture()` 대신 익스텐션 세션의 메서드를 사용합니다. 이때 시스템은 제조사의 야간 모드 합성 로직을 실행하므로, 촬영 완료까지 수 초가 걸릴 수 있습니다.

---

## 익스텐션 사용 시 주의사항

1. **지연 시간:** 익스텐션은 여러 장의 사진을 찍어 합성하는 "계산 사진학"의 결정체입니다. 따라서 일반 촬영보다 셔터 랙(Shutter Lag)이 길고 저장 속도가 느릴 수 있습니다.
2. **배터리 소모:** 하드웨어 가속기와 NPU를 풀가동하므로 배터리 소모가 큽니다.
3. **제한된 스트림:** 익스텐션 모드에서는 동시에 띄울 수 있는 미리보기/녹화 화면의 개수가 보통 2개로 제한됩니다.
4. **결과물 차이:** 동일한 '야간 모드'라도 삼성 폰에서 찍은 사진과 구글 폰에서 찍은 사진의 느낌이 다를 수 있습니다. 이는 각 제조사의 튜닝 방식이 다르기 때문입니다.

---

## 요약

- **카메라 익스텐션**은 제조사의 전용 이미지 처리 기술을 타사 앱에서도 쓸 수 있게 해주는 API입니다.
- **야간, 보케, HDR, 얼굴 보정** 등 5가지 표준 모드를 제공합니다.
- `CameraExtensionSession`을 통해 작동하며, 복잡한 로우 레벨 수동 제어 없이도 **최고의 화질**을 얻을 수 있는 지름길입니다.

## 다음 단계

익스텐션이 제조사의 마법을 빌려 쓰는 것이라면, 최신 안드로이드 표준은 어떤 방향으로 가고 있을까요? **제21장: HDR과 Ultra HDR**에서는 안드로이드 14에서 도입된 혁신적인 이미지 포맷인 Ultra HDR(JPEG_R)과 HDR 비디오 촬영에 대해 알아봅니다.
