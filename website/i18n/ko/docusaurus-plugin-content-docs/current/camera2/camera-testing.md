---
sidebar_position: 27
title: "제27장: 카메라 테스트"
description: "안드로이드 카메라 구현을 위한 테스트 전략을 수립하세요. 구글의 CTS(Compatibility Test Suite)와 ITS(Imaging Test Suite)가 HAL을 검증하는 방법, 기기별 특이사항 처리를 위한 테스트 하네스 구축, 그리고 실제 하드웨어 없이 CI에서 카메라 코드를 검증하기 위한 모킹(Mocking) 기법을 배웁니다."
keywords: [안드로이드 카메라 테스트, CTS, ITS, 카메라 모킹, Camera2 테스트 하네스, 기기 호환성 테스트, Robolectric 카메라, 카메라 계측 테스트]
---

# 제27장: 카메라 테스트

## 요약

안드로이드 카메라 개발에서 가장 어려운 점은 코드를 작성하는 것이 아니라, 그 코드가 출시되어 수천 개의 다른 기기 모델에서 실행될 때 제대로 작동하는지 확인하는 것입니다. 한 제조사에서는 완벽하게 작동하는 `CaptureRequest`가 다른 제조사에서는 `onConfigureFailed`를 발생시키거나 조용히 프레임을 드롭할 수 있습니다.

이 장에서는 안드로이드 카메라 생태계를 지탱하는 테스트 인프라를 살펴봅니다. 먼저, 구글과 OEM이 폰을 출시하기 전에 통과해야 하는 **CTS(Compatibility Test Suite)**와 **ITS(Imaging Test Suite)**에 대해 배웁니다. 이를 통해 안드로이드가 카메라 하드웨어에 대해 어떤 보장을 하는지 이해할 수 있습니다. 그 다음, 여러분의 앱을 위한 실용적인 테스트 전략을 구축합니다. 실제 카메라 하드웨어를 추상화하여 CI(지속적 통합) 서버에서 로직을 테스트하는 방법, **Robolectric**과 커스텀 **Shadows**를 사용한 카메라 유닛 테스트, 그리고 실제 장치에서 `CameraCharacteristics` 데이터를 수집하여 엣지 케이스를 재현하는 기기별 테스트 하네스 구축 방법을 다룹니다.

여러분의 기기가 구글의 호환성 기준을 얼마나 잘 따르는지 확인하려면, [Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams)에서 **Android Camera Parameters**를 설치하세요. 이 앱은 많은 CTS 체크 항목을 일반 사용자가 읽을 수 있는 보고서로 변환하여 보여줍니다.

---

## 안드로이드 카메라 품질의 수호자: CTS와 ITS

안드로이드 앱 개발자로서 여러분은 구글이 이미 하드웨어 수준에서 많은 테스트를 수행했다는 사실을 알고 안심해도 됩니다. 모든 안드로이드 기기는 출시 전 두 가지 엄격한 테스트 제품군을 통과해야 합니다.

### 1. CTS (Compatibility Test Suite)

CTS는 **API 계약**을 테스트합니다. `CameraDevice.open()`이 성공을 보고하면 실제로 장치가 열리는가? `SESSION_REGULAR` 세션에 2개의 YUV 스트림을 추가하면 HAL이 이를 거부하지 않는가? `SENSOR_EXPOSURE_TIME`을 설정하면 결과 메타데이터에 해당 값이 반영되는가?

- **테스트 방식:** 자동화된 안드로이드 계측 테스트(Instrumentation tests).
- **개발자에게 주는 의미:** CTS 덕분에 우리는 `CameraCharacteristics`에 명시된 기능이 실제로 작동할 것이라고 믿고 코드를 짤 수 있습니다. 예를 들어, 하드웨어 레벨이 `FULL`이라면 모든 `FULL` 레벨 API 호출이 에러 없이 작동함을 CTS가 보장합니다.

### 2. ITS (Imaging Test Suite)

CTS가 "코드가 돌아가는가"를 확인한다면, ITS는 **"사진이 잘 나오는가"**를 확인합니다. 이는 폰을 특수 제작된 "ITS 박스"(조명과 차트가 있는 통) 안에 넣고 수행하는 물리적 테스트입니다.

- **체크 항목:** 색상 정확도(차트의 빨간색이 사진에서도 빨간색인가?), 노이즈 레벨, 렌즈 왜곡 보정 정도, 프레임 타임스탬프의 정밀도(지터 확인).
- **개발자에게 주는 의미:** ITS 덕분에 서로 다른 제조사의 폰에서 찍은 사진들도 어느 정도 일관된 품질을 유지합니다. 특히 `CALIBRATED` 타임스탬프 동기화(20장 참고)는 ITS의 엄격한 검증을 통과해야만 선언할 수 있는 신뢰도 높은 데이터입니다.

---

## 앱 개발자를 위한 카메라 테스트 전략

모든 기기를 직접 사서 테스트할 수는 없습니다. 따라서 계층화된 테스트 접근 방식이 필요합니다.

### 1단계: 비즈니스 로직과 카메라 API 분리

가장 흔한 실수는 `Fragment`나 `Activity` 안에 모든 카메라 코드를 넣는 것입니다. 대신 카메라 로직을 인터페이스 뒤로 숨기세요.

```kotlin
interface CameraEngine {
    fun openCamera(id: String)
    fun setZoom(ratio: Float)
    fun takePicture(callback: (ByteArray) -> Unit)
}
```

이렇게 하면 실제 카메라 없이도 여러분의 UI가 줌 슬라이더 조작에 어떻게 반응하는지 테스트할 수 있습니다.

### 2단계: Robolectric과 ShadowCameraCharacteristics 사용하기

**Robolectric**은 JVM에서 안드로이드 코드를 실행할 수 있게 해줍니다. 하지만 원시 `CameraManager`는 JVM에서 항상 빈 결과를 반환합니다. 이를 해결하기 위해 실제 기기에서 수집한 `CameraCharacteristics` 데이터를 테스트에 주입하는 **Shadow**를 만들 수 있습니다.

**Android Camera Parameters** 앱의 장점 중 하나는 기기의 모든 메타데이터를 **JSON**으로 내보낼 수 있다는 점입니다. 픽셀 8의 JSON 데이터를 가져와서 유닛 테스트의 입력으로 넣으면, 실제 픽셀 8 하드웨어 없이도 여러분의 앱이 해당 기기의 해상도 목록을 어떻게 처리하는지 검증할 수 있습니다.

### 3단계: 가상 비디오 소스를 이용한 테스트

에뮬레이터의 카메라는 매우 제한적입니다. 하지만 고정된 이미지를 보여주는 대신 비디오 파일을 스트리밍하도록 설정할 수 있습니다.

- **방법:** `adb push video.mp4 /data/local/tmp/` 명령을 사용하고 에뮬레이터 설정에서 카메라 소스를 해당 파일로 지정합니다.
- **용도:** QR 코드 인식 로직이나 객체 감지 파이프라인이 움직이는 피사체를 대상으로 잘 작동하는지 테스트할 때 유용합니다.

---

## 기기별 특이사항(Quirk) 테스트 하네스 구축

Camera2 개발의 가장 큰 적은 "특이사항(Quirks)"입니다. 예를 들어 "특정 제조사의 폰은 60fps 녹화 중에 줌을 바꾸면 충돌한다"와 같은 현상입니다. 이를 위해 프로젝트 내에 **Quirk Database**와 연동된 테스트 케이스를 만드세요.

```kotlin
@Test
fun testZoomStabilityDuringRecording() {
    // Android Camera Parameters JSON에서 수집한 Quirk 플래그 확인
    if (DeviceQuirks.HAS_ZOOM_RECONFIG_BUG) {
        // 이 기기에서는 줌 변경 시 세션을 재시작하지 않는지 검증
    }
}
```

이러한 특이사항 데이터는 커뮤니티에서 공유되기도 합니다. **Jetpack CameraX** 라이브러리 내부에도 수백 개의 이러한 기기별 해결 방법(Workaround)이 포함되어 있는데, Camera2를 직접 사용한다면 이를 참고하여 여러분만의 테스트 세트를 구축해야 합니다.

---

## 실전: 하드웨어 모킹(Mocking) 예제

테스트 코드에서 `CameraCaptureSession`을 모킹하여 콜백이 올바른 순서로 들어오는지 확인하는 예제입니다.

```kotlin
@Test
fun `사진 촬영 시 AF 트리거가 먼저 실행되어야 한다`() {
    val mockSession = mock<CameraCaptureSession>()
    val engine = MyCameraEngine(mockSession)

    engine.takeStillPicture()

    // AF 트리거가 포함된 capture 호출이 일어났는지 검증
    verify(mockSession).capture(
        argThat { get(CaptureRequest.CONTROL_AF_TRIGGER) == CONTROL_AF_TRIGGER_START },
        any(),
        any()
    )
}
```

이러한 스타일의 테스트는 17장에서 배운 복잡한 **3A 파이프라인**이 실제 기기에서 예상치 못한 순서로 꼬이지 않도록 방지해 줍니다.

---

## 요약

1. **CTS와 ITS:** 안드로이드 생태계의 기본 품질 보증 수단입니다. `CameraCharacteristics`의 신뢰도는 여기서 나옵니다.
2. **인터페이스 분리:** 카메라 제어 로직을 UI와 분리하여 테스트 가능하게 만드세요.
3. **메타데이터 테스트:** **Android Camera Parameters** 앱에서 추출한 JSON 데이터를 사용하여 다양한 기기 환경을 시뮬레이션하세요.
4. **Quirk 관리:** 기기별로 다른 동작을 기록하고 이를 검증하는 테스트 자동화가 장기적인 유지보수의 핵심입니다.
5. **모킹:** Mockito 등의 도구를 사용하여 Camera2의 복잡한 콜백 시퀀스를 단위 테스트하세요.

## 다음 단계

축하합니다! 이제 여러분은 안드로이드 Camera2의 아키텍처부터 고급 기능, 그리고 안정적인 출시를 위한 테스트 전략까지 모두 섭렵했습니다. 이 시리즈의 마지막인 **제28장: 카메라 아키텍처의 미래**에서는 안드로이드 15에서 도입된 `CameraDeviceSetup` API와, 더욱 강력해질 계산 사진학(Computational Photography)의 미래, 그리고 여러분이 배운 지식을 실무에서 어떻게 확장해 나갈지에 대해 이야기하며 마무리하겠습니다.
