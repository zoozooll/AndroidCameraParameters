---
sidebar_position: 16
title: "제16장: 화이트 밸런스 및 색상"
description: "안드로이드 Camera2의 자동 화이트 밸런스(AWB) 프리셋과 수동 색상 보정을 마스터하세요. COLOR_CORRECTION_GAINS, COLOR_CORRECTION_TRANSFORM, 그리고 켈빈 색온도를 카메라 ISP 하드웨어 파라미터로 변환하는 방법을 배웁니다."
keywords: [안드로이드 camera2 화이트 밸런스, AWB 모드, COLOR_CORRECTION_GAINS, COLOR_CORRECTION_TRANSFORM, 켈빈 색온도, 수동 화이트 밸런스, 3x3 색상 행렬, 카메라 앱 개발]
---

# 제16장: 화이트 밸런스 및 색상

밝기(노출)와 선명도(초점)를 잡았다면, 마지막 퍼즐 조각은 **색상**입니다. 흰색 셔츠가 전구 아래서는 노랗게, 그늘 아래서는 파랗게 보이는 현상을 해결하는 것이 바로 **화이트 밸런스(White Balance)**입니다.

Camera2 API는 우리에게 세 가지 수준의 제어권을 줍니다: 단순한 자동 모드(AWB), 제조사가 설정한 프리셋(태양광, 형광등 등), 그리고 R/G/B 채널의 증폭값을 직접 조절하는 완전 수동 모드입니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Control** 탭에서 AWB 모드를 바꾸거나, 수동 모드에서 R/G/B 게인(Gain) 값이 어떻게 변하는지 실시간으로 관찰할 수 있습니다.

---

## 1. 자동 화이트 밸런스 (AWB) 모드

가장 쉬운 방법은 `CONTROL_AWB_MODE`를 사용하는 것입니다.

| 모드 | 설명 |
|:---|:---|
| `AUTO` | 시스템이 장면을 분석하여 실시간으로 흰색을 찾습니다. (기본값) |
| `INCANDESCENT` | 백열등(전구) 아래. 노란 끼를 제거하기 위해 푸른색을 보강합니다. (~2700K) |
| `FLUORESCENT` | 일반 형광등 아래. 초록색 끼를 제거합니다. (~4500K) |
| `DAYLIGHT` | 맑은 날 야외. 표준적인 색감을 유지합니다. (~5500K) |
| `CLOUDY_DAYLIGHT` | 구름 낀 날. 따뜻한 느낌을 더합니다. (~6500K) |
| `SHADE` | 그늘진 곳. 파란 끼를 제거하기 위해 주황색을 보강합니다. (~7500K) |
| `OFF` | 모든 자동 보정을 끄고, 우리가 직접 색상 게인을 설정합니다. |

```kotlin
// 태양광 모드로 설정 예시
builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT)
```

---

## 2. 수동 색상 제어 (Manual Color Correction)

전문가용 앱을 만든다면 "켈빈(Kelvin) 슬라이더" 기능이 필요할 것입니다. 이를 구현하려면 `CONTROL_AWB_MODE`를 `OFF`로 설정하고 두 가지 핵심 파라미터를 건드려야 합니다.

### A. COLOR_CORRECTION_GAINS (색상 게인)
이미지의 빨강(R), 초록(G1, G2), 파랑(B) 채널 각각을 얼마나 증폭할지 결정하는 4개의 숫자입니다.
- 예를 들어, `[2.0, 1.0, 1.0, 1.0]`으로 설정하면 사진에서 빨간색만 2배 더 밝아집니다.

### B. COLOR_CORRECTION_TRANSFORM (색상 변환 행렬)
색 공간을 변환하는 3×3 행렬입니다. 단순히 빨간색을 밝게 하는 것을 넘어, "빨간색을 약간 주황색 쪽으로 틀어줘" 같은 정밀한 보정이 가능합니다.

---

## 3. 켈빈(K) 온도를 게인값으로 변환하기

사용자는 "2700K" 같은 온도를 원하지, "빨강 1.8, 파랑 2.2" 같은 게인값을 알고 싶어 하지 않습니다. 안타깝게도 Camera2 API는 켈빈 값을 직접 받는 함수가 없습니다. 개발자가 직접 변환 공식을 구현해야 합니다.

```kotlin
// 단순화된 켈빈 -> RGB 게인 변환 개념 (참고용)
fun kelvinToRgbGains(kelvin: Int): RgbGains {
    val temp = kelvin / 100.0
    val red = if (temp <= 66) 255.0 else 329.69 * Math.pow(temp - 60, -0.1332)
    val green = if (temp <= 66) 99.47 * Math.log(temp) - 161.11 else 288.12 * Math.pow(temp - 60, -0.0755)
    val blue = if (temp >= 66) 255.0 else if (temp <= 19) 0.0 else 138.51 * Math.log(temp - 10) - 305.04
    
    // 이 RGB 값들을 1.0 ~ 4.0 사이의 게인값으로 정규화하여 사용합니다.
    return normalizeToGains(red, green, blue)
}
```

---

## 4. 수동 모드 구현 시 주의사항

1. **MANUAL_POST_PROCESSING 지원 확인:** 모든 폰이 수동 색상 보정을 지원하지는 않습니다. `REQUEST_AVAILABLE_CAPABILITIES`에 `MANUAL_POST_PROCESSING` 플래그가 있는지 확인하세요.
2. **초록색 채널:** 게인값은 4개(`R, G_even, G_odd, B`)인데, 초록색 두 개는 보통 같은 값을 줍니다.
3. **복잡성:** 행렬(Transform)까지 건드리는 것은 매우 어렵습니다. 대부분의 앱은 게인(Gains) 값만 조절해도 충분히 훌륭한 화이트 밸런스 기능을 구현할 수 있습니다.

---

## 요약

- **AWB 프리셋**은 가장 빠르고 안전하게 색상을 맞추는 방법입니다.
- **Kelvin 온도** 기능을 만들려면 `AWB_MODE_OFF`와 `COLOR_CORRECTION_GAINS`를 조합해야 합니다.
- 수동 모드를 사용하면 전구 아래의 노란 사진을 눈으로 보는 것과 똑같이 하얗게, 혹은 의도적으로 더 따뜻하게 만들 수 있습니다.

## 다음 단계

이제 노출, 초점, 색상이라는 수동 제어의 3대 요소를 모두 배웠습니다. **제17장: 3A 파이프라인과 오케스트레이션**에서는 이 세 가지(AE, AF, AWB)가 서로 충돌하지 않고 조화롭게 작동하게 하여, 어떤 상황에서도 완벽한 사진을 찍는 "마스터 시퀀스"를 구축해 봅니다.
