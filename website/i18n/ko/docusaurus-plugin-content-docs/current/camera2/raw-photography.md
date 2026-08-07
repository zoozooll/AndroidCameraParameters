---
sidebar_position: 18
title: "제18장: RAW 사진 촬영"
description: "안드로이드 Camera2의 ImageFormat.RAW_SENSOR를 마스터하세요. 처리되지 않은 Bayer 데이터를 캡처하고, DngCreator를 사용하여 전문가 수준의 Adobe DNG 파일을 생성하며, RAW+JPEG 동시 캡처 파이프라인을 구축하는 방법을 배웁니다."
keywords: [안드로이드 camera2 RAW, RAW_SENSOR, DngCreator, DNG 파일 저장, Bayer 패턴, RAW+JPEG 동시 캡처, 계산 사진학, 안드로이드 전문가용 카메라 개발]
---

# 제18장: RAW 사진 촬영

스마트폰의 ISP(이미지 신호 프로세서)는 마법과도 같습니다. 셔터를 누르는 수 밀리초 동안 노이즈를 제거하고, 색상을 보정하고, 톤을 매핑하여 즉시 공유할 수 있는 예쁜 JPEG를 만들어냅니다. 하지만 전문 사진작가나 고급 보정 앱을 만드는 개발자에게 이 마법은 가끔 방해가 됩니다. ISP가 "이미 결정해버린" 정보들(화이트 밸런스, 노이즈 감소 수준 등)을 되돌릴 수 없기 때문입니다.

**RAW 사진**은 이 모든 과정을 건너뛰고 **이미지 센서가 포착한 가공되지 않은 빛의 데이터(Bayer 데이터)**를 직접 가져오는 방식입니다. 이를 통해 후보정에서 8비트 JPEG보다 수십 배 많은 정보(10~16비트)를 활용하여 노출을 복구하거나 색상을 정밀하게 조절할 수 있습니다.

이 장에서는 안드로이드 Camera2 API를 통해 `RAW_SENSOR` 데이터를 캡처하고, 이를 표준 포맷인 Adobe DNG 파일로 변환하여 저장하는 과정을 배웁니다. [Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Sensor** 탭에서 여러분의 기기가 RAW 촬영을 지원하는지(`REQUEST_AVAILABLE_CAPABILITIES_RAW`), 그리고 센서의 Bayer 패턴이 무엇인지 실시간으로 확인할 수 있습니다.

---

## 1. RAW 캡처의 핵심 개념: Bayer 패턴

대부분의 카메라 센서는 각 픽셀이 모든 색을 감지하는 것이 아니라, 빨강(R), 초록(G), 파랑(B) 중 한 가지 색의 필터만 덮고 있습니다. 이를 **Bayer 필터 어레이**라고 합니다. 일반적인 패턴은 초록색 픽셀이 50%, 빨강과 파랑이 각각 25%를 차지하는 **RGGB** 패턴입니다.

`RAW_SENSOR` 데이터를 가져오면 이 모자이크 형태의 데이터를 받게 됩니다. 이를 우리가 아는 색상 사진으로 만드는 과정을 **데모자이킹(Demosaicing)**이라고 합니다. Camera2를 사용하면 우리가 직접 데모자이킹을 할 필요 없이, 구글이 제공하는 `DngCreator`를 사용하여 이 원본 데이터와 필수 메타데이터를 하나의 DNG 파일로 묶어 저장할 수 있습니다.

---

## 2. RAW 촬영 지원 여부 확인하기

모든 안드로이드 폰이 RAW 촬영을 지원하는 것은 아닙니다. 코드를 작성하기 전에 반드시 확인해야 합니다.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val isRawSupported = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
) ?: false

if (!isRawSupported) {
    Log.w("RAW", "이 카메라는 RAW 촬영을 지원하지 않습니다.")
}
```

---

## 3. RAW_SENSOR용 ImageReader 설정

RAW 데이터는 JPEG보다 훨씬 큽니다. 예를 들어 12MP 사진의 경우 JPEG는 3~5MB지만, RAW16 데이터는 20MB를 훌쩍 넘습니다. 따라서 전용 `ImageReader`를 준비해야 합니다.

```kotlin
val rawSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    ?.getOutputSizes(ImageFormat.RAW_SENSOR)?.firstOrNull() // 가장 큰 해상도 선택

val rawImageReader = ImageReader.newInstance(
    rawSize!!.width, rawSize.height,
    ImageFormat.RAW_SENSOR, 
    2 /* maxImages */
)

rawImageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
    // 5단계에서 DngCreator로 저장하는 코드 작성
    saveAsDng(image)
    image.close()
}, backgroundHandler)
```

---

## 4. RAW + JPEG 동시 캡처 요청

사용자가 고화질 미리보기를 보면서 RAW 결과물을 얻길 원한다면, 세션에 RAW와 JPEG(또는 SurfaceTexture) 출력을 동시에 연결해야 합니다.

```kotlin
// 세션 생성 시 RAW 리더의 Surface 포함
val surfaces = listOf(previewSurface, rawImageReader.surface, jpegImageReader.surface)
device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    // ... 세션 설정 ...
}, backgroundHandler)

// 촬영 시 두 타겟을 모두 추가
val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
    addTarget(jpegImageReader.surface) // 인스턴트 확인용
    addTarget(rawImageReader.surface)  // 고화질 보정용
}
session.capture(captureBuilder.build(), null, backgroundHandler)
```

---

## 5. DngCreator를 사용하여 파일 저장하기

`DngCreator`는 `RAW_SENSOR` 이미지와 해당 시점의 `CaptureResult`(노출 시간, ISO, 화이트 밸런스 등)를 결합하여 완벽한 DNG 파일을 만들어줍니다.

```kotlin
private fun saveAsDng(image: Image, result: TotalCaptureResult) {
    val file = File(getExternalFilesDir(null), "PHOTO_${System.currentTimeMillis()}.dng")
    val dngCreator = DngCreator(characteristics, result)
    
    FileOutputStream(file).use { output ->
        // 센서 방향 메타데이터 설정 (중요: 그래야 사진이 똑바로 보입니다)
        val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        dngCreator.setOrientation(orientation)
        
        // 이미지 쓰기
        dngCreator.writeImage(output, image)
        Log.d("RAW", "DNG 저장 완료: ${file.absolutePath}")
    }
}
```

---

## RAW 촬영 시 주의사항

1. **파일 크기:** RAW 파일은 엄청나게 큽니다. 저장 공간이 부족하지 않은지 확인하고, 파일 쓰기 작업은 반드시 백그라운드 스레드에서 수행하세요.
2. **저장 속도:** 데이터를 파일로 쓰는 데 수 초가 걸릴 수 있습니다. `DngCreator.writeImage()`는 CPU 집약적인 작업입니다.
3. **노이즈 감소 없음:** RAW 파일에는 ISP의 노이즈 감소가 전혀 적용되지 않았습니다. 따라서 원본 데이터를 PC의 어도비 라이트룸(Adobe Lightroom)이나 캡처 원(Capture One) 같은 전문 툴로 열어보면 JPEG보다 노이즈가 훨씬 많아 보일 수 있습니다. 이는 "망가진 사진"이 아니라 "보정의 여지가 많은 사진"임을 의미합니다.
4. **메타데이터 일치:** `DngCreator`에 전달하는 `TotalCaptureResult`는 반드시 **해당 RAW 이미지가 캡처된 프레임의 결과물**이어야 합니다. 타임스탬프를 사용하여 정확히 매칭하는 로직이 필요합니다.

---

## 요약

- **RAW_SENSOR**는 센서의 원시 데이터를 가져오는 형식입니다.
- **Bayer 패턴**으로 구성되어 있으며, 후보정 관용도가 매우 높습니다.
- **DngCreator**를 사용하면 안드로이드 시스템이 원본 데이터와 메타데이터를 묶어 표준 DNG 파일을 만들어줍니다.
- 전문가용 카메라 앱이나 계산 사진학 연구를 위해서는 필수적인 기능입니다.

## 다음 단계

RAW 촬영을 마스터했다면 이제 더 빠른 영역으로 가보겠습니다. **제19장: 고속 비디오와 슬로우 모션**에서는 120fps, 240fps 이상의 프레임 속도로 비디오를 캡처하여 극적인 슬로우 모션 영상을 만드는 방법을 알아봅니다.
