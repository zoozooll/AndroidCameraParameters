---
sidebar_position: 14
title: "제14장: Camera2에서의 수동 노출"
description: "안드로이드 Camera2 API를 사용하여 수동 노출을 구현하세요. 3A 자동화를 비활성화하고, SENSOR_SENSITIVITY와 SENSOR_EXPOSURE_TIME을 제어하며, 노출 브래키팅 시리즈를 캡처하고 OEM 노즈 감소 최적화 손실과 같은 중요한 트레이드오프를 이해합니다."
keywords: [안드로이드 camera2 수동 노출, SENSOR_EXPOSURE_TIME, SENSOR_SENSITIVITY, CONTROL_AE_MODE_OFF, 노출 브래키팅, 장노출 안드로이드, 수동 카메라 Kotlin]
---

# 제14장: Camera2에서의 수동 노출

13장에서 노출 삼각형(ISO, 셔터 속도, 조리개)에 대한 이론적 기반을 다졌습니다. 이제 그 지식을 활용해 안드로이드 폰을 진정한 수동 카메라로 바꿔볼 시간입니다.

Camera2 API를 사용하면 셔터를 1/8000초로 고정해 날아가는 벌새의 날개를 멈추거나, 30초 동안 열어두어 밤하늘의 별 궤적을 담을 수 있습니다. 하지만 하드웨어를 직접 제어하는 만큼, 안드로이드 시스템이 제공하던 "자동 보정 마법"을 일부 포기해야 하는 중요한 주의사항도 있습니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Sensor** 탭에서 여러분의 기기가 수동 노출을 지원하는지(`MANUAL_SENSOR` 기능) 지금 확인해 보세요.

---

## 1. 3A 자동화 비활성화하기

수동 노출을 하려면 먼저 안드로이드의 "지능형 뇌"인 **3A 파이프라인**(Auto Exposure, Auto Focus, Auto White-balance) 중 노출 부분을 꺼야 합니다.

```kotlin
// 캡처 요청 빌더에서 AE(자동 노출)를 OFF로 설정
builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
```

**중요:** 일부 기기에서는 `CONTROL_AE_MODE`를 끄는 것만으로는 부족할 수 있습니다. `CONTROL_MODE` 자체를 `OFF`로 설정하여 모든 자동 기능을 수동으로 전환해야 할 수도 있습니다.

---

## 2. ISO 설정: SENSOR_SENSITIVITY

Camera2에서 ISO는 **Sensitivity**라고 불립니다. 단위는 정수이며, 일반적인 범위는 100에서 6400 사이입니다.

```kotlin
val isoValue = 400
builder.set(CaptureRequest.SENSOR_SENSITIVITY, isoValue)
```

기기가 지원하는 정확한 범위를 확인하려면 `CameraCharacteristics`에서 `SENSOR_INFO_SENSITIVITY_RANGE`를 쿼리해야 합니다. 범위를 벗어난 값을 넣으면 무시되거나 충돌이 발생할 수 있습니다.

---

## 3. 셔터 속도 설정: SENSOR_EXPOSURE_TIME

이 부분이 처음 접하는 개발자들을 가장 당황스럽게 만듭니다. Camera2에서 노출 시간은 "초"가 아니라 **나노초(Nanoseconds)** 단위를 사용합니다.

- 1초 = 1,000,000,000 나노초 (10억 ns)
- 1/60초 ≈ 16,666,666 나노초
- 1/1000초 = 1,000,000 나노초

```kotlin
// 1/125초 노출 설정 예시
val shutterSpeedNanos = 1_000_000_000L / 125
builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedNanos)
```

**팁:** 64비트 정수인 `Long` 타입을 사용해야 오버플로우를 방지할 수 있습니다.

---

## 4. 수동 노출 시 잃게 되는 것 (중요!)

이것은 연구 프로젝트와 실무에서 매우 중요한 포인트입니다. `CONTROL_AE_MODE_OFF`를 설정하는 순간, 삼성이나 구글 같은 제조사가 공들여 만든 **"다중 프레임 노이즈 감소"**와 **"HDR 합성"** 기능이 비활성화됩니다.

- **자동 모드:** 셔터를 누르면 폰이 몰래 10장을 찍어 노이즈를 지우고 예쁜 JPEG를 만듭니다.
- **수동 모드:** 정직하게 딱 한 장만 찍습니다. 따라서 동일한 ISO 1600이라도 자동 모드보다 수동 모드 결과물에 노이즈가 훨씬 더 많아 보일 수 있습니다.

이를 보완하려면 직접 여러 장을 찍어 합성하는 로직을 짜거나(계산 사진학), RAW 파일로 저장하여 후처리를 해야 합니다.

---

## 5. 실전 예제: 노출 브래키팅 (Exposure Bracketing)

HDR 합성을 위해 서로 다른 밝기의 사진 3장을 연속으로 찍는 코드의 핵심 부분입니다.

```kotlin
val requests = mutableListOf<CaptureRequest>()

// 1. 어둡게 (-2 EV)
requests.add(createRequest(iso = 100, shutter = 1_000_000_000L / 1000))
// 2. 중간 (0 EV)
requests.add(createRequest(iso = 100, shutter = 1_000_000_000L / 250))
// 3. 밝게 (+2 EV)
requests.add(createRequest(iso = 100, shutter = 1_000_000_000L / 60))

// 연사 촬영 실행
captureSession.captureBurst(requests, null, backgroundHandler)
```

이렇게 찍은 3장의 사진을 나중에 서버나 라이브러리로 보내 합치면 나만의 HDR 사진이 됩니다.

---

## 요약

- **수동 노출**을 위해 `CONTROL_AE_MODE`를 `OFF`로 설정합니다.
- **ISO**는 `SENSOR_SENSITIVITY`이며 정수 값을 씁니다.
- **셔터 속도**는 `SENSOR_EXPOSURE_TIME`이며 **나노초(ns)** 단위를 씁니다.
- 수동 모드에서는 제조사의 **자동 보정 기능이 꺼지므로** 노이즈에 주의해야 합니다.

## 다음 단계

밝기를 조절했으니 이제 선명함을 잡을 차례입니다. **제15장: 초점(Focus) 마스터하기**에서는 AF(자동 초점)의 원리와 수동 초점 슬라이더를 만드는 법, 그리고 "무한대 초점"으로 풍경 사진을 찍는 법을 알아봅니다.
