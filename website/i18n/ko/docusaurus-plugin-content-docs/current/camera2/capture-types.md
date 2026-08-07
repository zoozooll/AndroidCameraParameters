---
sidebar_position: 11
title: "제11장: 캡처 유형"
description: "Camera2의 세 가지 요청 제출 모드인 반복(Repeating), 단일(One-shot), 연사(Burst) 요청을 마스터하세요. 각 유형의 사용 시점과 템플릿(PREVIEW, STILL_CAPTURE, RECORD)을 통한 파이프라인 최적화 방법을 알아봅니다."
keywords: [안드로이드 camera2 캡처 유형, setRepeatingRequest, capture, captureBurst, 캡처 템플릿, TEMPLATE_PREVIEW, TEMPLATE_STILL_CAPTURE, TEMPLATE_RECORD, 카메라 파이프라인 최적화]
---

# 제11장: 캡처 유형

Camera2 파이프라인(10장)은 거대한 컨베이어 벨트와 같으며, `CaptureRequest`는 하드웨어가 각 프레임을 어떻게 처리해야 하는지 알려주는 작업 지침서입니다. 하지만 이 벨트에 지침서를 *어떻게* 올릴까요?

모든 프레임에 대해 `capture()`를 수동으로 호출해야 할까요? 아니면 한 번만 설정하면 알아서 돌아가게 할 수 있을까요? 고속 연사 촬영은 어떻게 처리할까요? 이 장에서는 요청을 제출하는 세 가지 방식과 안드로이드 프레임워크에서 제공하는 미리 구성된 "템플릿"에 대해 알아봅니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Capture** 탭에서 이 세 가지 모드가 실제로 작동하는 모습을 확인할 수 있습니다. 앱은 각 모드에서 발생하는 프레임 속도와 메타데이터 변화를 실시간으로 보여줍니다.

---

## 1. 반복 요청 (Repeating Requests)

**반복 요청**은 카메라 미리보기의 심장박동입니다. 한 번 제출하면 명시적으로 중단하거나 다른 요청으로 교체할 때까지 카메라가 가능한 한 빨리(보통 30fps 또는 60fps) 동일한 지침으로 프레임을 계속 생성하도록 합니다.

- **메서드:** `CameraCaptureSession.setRepeatingRequest()`
- **주요 용도:** 라이브 미리보기 표시, 실시간 QR 코드 스캔, 비디오 녹화 중 프레임 스트리밍.
- **작동 방식:** 프레임 N이 완료되면 프레임워크가 자동으로 동일한 `CaptureRequest`를 대기열의 끝에 다시 넣습니다.

```kotlin
// 일반적인 미리보기 설정
val previewRequest = previewRequestBuilder.build()
captureSession.setRepeatingRequest(
    previewRequest,
    captureCallback, // 매 프레임마다 메타데이터를 받기 위한 콜백
    backgroundHandler
)
```

**중요:** 반복 요청은 가장 낮은 우선순위를 갖습니다. 단일 요청이나 연사 요청이 들어오면 반복 요청은 일시적으로 "대기" 상태가 되고, 우선순위가 높은 요청들이 모두 처리된 후에 다시 재개됩니다.

---

## 2. 단일 요청 (One-shot / Single Captures)

**단일 요청**은 컨베이어 벨트에 단 하나의 지침서만 올리는 것입니다. 하드웨어는 그 지침에 따라 정확히 한 프레임을 처리하고 멈춥니다.

- **메서드:** `CameraCaptureSession.capture()`
- **주요 용도:** 고해상도 사진 촬영, AF(자동 초점) 트리거 실행, 수동 노출 설정 한 번 적용.
- **작동 방식:** 현재 진행 중인 반복 요청 바로 다음에 삽입됩니다.

```kotlin
// 고해상도 사진 한 장 촬영
val stillRequest = stillRequestBuilder.build()
captureSession.capture(
    stillRequest,
    object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(...) {
            // 사진 촬영 완료!
        }
    },
    backgroundHandler
)
```

**팁:** 단일 요청을 보낼 때 반복 요청을 멈출 필요가 없습니다. 시스템이 알아서 단일 요청을 먼저 처리하고 미리보기를 계속 이어갑니다.

---

## 3. 연사 요청 (Burst Requests)

**연사 요청**은 순서가 정해진 여러 장의 지침서를 묶어서 한꺼번에 제출하는 것입니다. 이 요청들은 중간에 다른 요청(반복 요청 포함)이 끼어들지 않고 반드시 연속적으로 처리됩니다.

- **메서드:** `CameraCaptureSession.captureBurst()`
- **주요 용도:** HDR 촬영(서로 다른 노출로 3~5장 촬영), 제로 셔터 랙(ZSL), 고속 연사 모드.
- **작동 방식:** 리스트에 담긴 모든 `CaptureRequest`가 원자적(atomic)으로 대기열에 추가됩니다.

```kotlin
// 노출값이 다른 3장의 사진을 연사로 촬영 (HDR용)
val burstRequests = listOf(requestEvMinus, requestEvZero, requestEvPlus)
captureSession.captureBurst(burstRequests, burstCallback, backgroundHandler)
```

**주의:** 연사 촬영 중에는 미리보기 프레임이 일시 정지될 수 있습니다(블랙아웃 현상). 하드웨어가 연사 요청을 처리하는 데 모든 자원을 집중하기 때문입니다.

---

## 요청 템플릿 (Capture Templates)

`CaptureRequest`에는 설정해야 할 파라미터가 수백 개나 됩니다. 이를 처음부터 하나씩 설정하는 것은 고통스러운 일입니다. 안드로이드는 일반적인 유스케이스에 맞춰 최적화된 파라미터 세트인 **템플릿**을 제공합니다.

`cameraDevice.createCaptureRequest(Template_ID)`를 호출하여 시작점을 선택할 수 있습니다.

| 템플릿 ID | 최적화 대상 | 특징 |
|:---|:---|:---|
| `TEMPLATE_PREVIEW` | 화면 표시용 | 부드러운 프레임 속도 우선, 전력 소모 절감, 중간 품질의 노이즈 감소. |
| `TEMPLATE_STILL_CAPTURE` | 고품질 사진 | 최고 품질의 이미지 처리(노이즈 감소, 샤프닝) 우선, 프레임 속도는 느려질 수 있음. |
| `TEMPLATE_RECORD` | 비디오 녹화 | 일정한 프레임 속도와 노출 유지, 비디오 흔들림 보정(EIS) 활성화 가능성 높음. |
| `TEMPLATE_VIDEO_SNAPSHOT` | 비디오 중 사진 | 녹화 중 프레임 품질을 높이되, 비디오 스트림이 끊기지 않도록 속도 조절. |
| `TEMPLATE_MANUAL` | 수동 제어 | 자동 기능(3A)이 대부분 꺼진 상태로 시작. 개발자가 모든 값을 직접 설정할 때 유용. |
| `TEMPLATE_ZERO_SHUTTER_LAG` | 무지연 촬영 | 셔터를 누르는 순간의 프레임을 즉시 캡처하기 위해 백그라운드에서 항상 고해상도로 구동. |

### 템플릿 사용 예시

```kotlin
// 사진 촬영용 요청 생성 시
val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
// 이제 템플릿 설정 위에 필요한 값만 살짝 수정합니다.
builder.set(CaptureRequest.JPEG_QUALITY, 100)
val request = builder.build()
```

---

## 캡처 유형 선택 가이드

| 상황 | 권장 유형 | 권장 템플릿 |
|:---|:---|:---|
| 카메라를 켜고 화면을 보여줄 때 | `setRepeatingRequest` | `TEMPLATE_PREVIEW` |
| "찰칵" 버튼을 눌러 사진을 저장할 때 | `capture` | `TEMPLATE_STILL_CAPTURE` |
| 비디오 녹화 버튼을 눌렀을 때 | `setRepeatingRequest` | `TEMPLATE_RECORD` |
| HDR을 위해 밝기가 다른 사진 3장을 찍을 때 | `captureBurst` | `TEMPLATE_STILL_CAPTURE` |
| 사용자가 초점을 맞추려고 화면을 터치했을 때 | `capture` (AF 트리거 전용) | `TEMPLATE_PREVIEW` |

---

## 요약

1. **반복(Repeating):** 멈추라고 할 때까지 계속 프레임을 생성합니다. (미리보기용)
2. **단일(One-shot):** 요청한 프레임을 딱 한 번 생성하고 끝냅니다. (사진 촬영용)
3. **연사(Burst):** 여러 프레임을 순서대로 묶어서 생성합니다. (HDR/고속 촬영용)
4. **템플릿:** 수백 개의 설정을 직접 할 필요 없이, 용도에 맞는 기본값 세트를 선택해서 시작할 수 있습니다.

## 다음 단계

이제 요청을 보내는 방법을 알았습니다. 하지만 캡처된 이미지는 어디로 갈까요? **제12장: ImageReader와 데이터 처리**에서는 하드웨어에서 생성된 픽셀 데이터를 앱에서 받아보고 파일로 저장하는 실제적인 방법을 배웁니다.
