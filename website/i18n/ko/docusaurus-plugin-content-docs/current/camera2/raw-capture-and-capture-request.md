---
sidebar_position: 11
title: "11장: RAW 촬영과 CaptureRequest"
description: RAW 이미지를 촬영하는 방법을 배우고 고급 카메라 제어를 위한 CaptureRequest와 CaptureResult를 이해합니다.
keywords: [RAW 촬영, CaptureRequest, CaptureResult, Camera2, 수동 제어]
---

RAW 촬영은 이미지 처리에 대한 완전한 제어권을 제공합니다. CaptureRequest와 CaptureResult와 함께 살펴봅시다.

## 소개

이전 장에서는 JPEG 사진을 촬영하는 방법을 배웠습니다. 이제 다음을 살펴보겠습니다:

1. **RAW 촬영** — 처리되지 않은 센서 데이터 캡처
2. **CaptureRequest** — 각 캡처에 대한 카메라 설정 구성
3. **CaptureResult** — 완료된 캡처에 대한 메타데이터 가져오기

## RAW란 무엇인가요?

RAW 이미지는 ISP 처리 전 센서가 캡처한 모든 데이터를 포함합니다. 즉:

- 노이즈 감소가 적용되지 않음
- 화이트 밸런스 보정이 적용되지 않음
- 선명화가 적용되지 않음
- 전체 다이나믹 레인지

RAW 파일은 더 크지만 비교할 수 없는 편집 유연성을 제공합니다.

## RAW 촬영 요구 사항

RAW 이미지를 촬영하려면 카메라가 다음을 만족해야 합니다:
1. **FULL** 또는 **LEVEL_3** 하드웨어 레벨 보유
2. `RAW` 기능 지원

CameraCharacteristics를 확인하세요:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## RAW 이미지 촬영하기

RAW 형식으로 ImageReader를 생성하세요:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // 또는 ImageFormat.RAW10/RAW12
    2
)
```

그런 다음 동시 촬영을 위해 JPEG와 RAW 표면을 모두 캡처 세션에 추가하세요:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest는 단일 캡처에 대한 모든 설정을 정의합니다. 다음을 구성할 수 있습니다:

### 자동 제어
- `CONTROL_AF_MODE` — 자동 초점 모드
- `CONTROL_AE_MODE` — 자동 노출 모드
- `CONTROL_AWB_MODE` — 자동 화이트 밸런스 모드

### 수동 제어
- `SENSOR_SENSITIVITY` — ISO 값
- `SENSOR_EXPOSURE_TIME` — 노출 시간 (나노초 단위)
- `LENS_FOCUS_DISTANCE` — 초점 거리
- `LENS_APERTURE` — 조리개 (지원하는 경우)

### 출력 설정
- `JPEG_QUALITY` — JPEG 압축 품질
- `JPEG_ORIENTATION` — 이미지 방향
- `COLOR_CORRECTION_MODE` — 색상 보정 모드

### CaptureRequest 생성하기

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// 자동 제어 설정
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// JPEG 품질 설정
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// 타겟 추가
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// 요청 빌드
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult는 완료된 캡처에 대한 메타데이터를 포함합니다. 다음을 포함합니다:

- **실제 사용된 설정** — 카메라가 실제로 적용한 값
- **통계** — 노출, 초점, 색상 정보
- **타임스탬프** — 캡처가 발생한 시점

### CaptureResult 가져오기

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // 실제 노출 시간 가져오기
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // 실제 ISO 가져오기
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // 초점 상태 가져오기
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // AE 상태 가져오기
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "노출: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### 주요 CaptureResult 키

| 키 | 설명 |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | 사용된 실제 노출 시간 |
| `SENSOR_SENSITIVITY` | 사용된 실제 ISO |
| `CONTROL_AF_STATE` | 자동 초점 상태 |
| `CONTROL_AE_STATE` | 자동 노출 상태 |
| `CONTROL_AWB_STATE` | 자동 화이트 밸런스 상태 |
| `SCALER_CROP_REGION` | 사용된 크롭 영역 |
| `COLOR_CORRECTION_GAINS` | 색상 보정 게인 |

## 완전한 RAW 촬영 예제

```kotlin
private fun configureDualCapture() {
    // JPEG ImageReader 생성
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // RAW ImageReader 생성
    val rawReader = ImageReader.newInstance(
        rawSize.width, rawSize.height,
        ImageFormat.RAW_SENSOR, 2
    )
    
    jpegReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveJpeg(image)
        image.close()
    }, null)
    
    rawReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveRaw(image)
        image.close()
    }, null)
    
    // 표면 생성
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // 모든 표면으로 캡처 세션 생성
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // 두 표면을 모두 타겟으로 추가
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // 제어 구성
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.JPEG_QUALITY, 100)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.capture(builder.build(), captureCallback, null)
    }
}

private fun saveRaw(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val fileName = "RAW_${System.currentTimeMillis()}.dng"
    val file = File(externalCacheDir, fileName)
    
    FileOutputStream(file).use { fos ->
        fos.write(bytes)
    }
}
```

## RAW vs JPEG

| 기능 | RAW | JPEG |
| --- | --- | --- |
| 파일 크기 | 큼 (20-50MB) | 작음 (2-10MB) |
| 편집 유연성 | 최대 | 제한적 |
| 노이즈 | 보존됨 | 감소됨 |
| 화이트 밸런스 | 조정 가능 | 고정됨 |
| 다이나믹 레인지 | 전체 | 압축됨 |

## 모범 사례

1. **RAW 지원 확인** — RAW 촬영을 시도하기 전에 항상 확인하세요
2. **듀얼 촬영** — 유연성을 위해 JPEG와 RAW를 모두 촬영하세요
3. **이미지 닫기** — 처리 후 항상 `image.close()`를 호출하세요
4. **다른 형식 처리** — RAW_SENSOR, RAW10, RAW12는 바이트 레이아웃이 다릅니다

## 다음 장

다음 장에서는 제III부에서 배운 내용을 요약하고 제IV부: 수동 카메라 제어를 준비하겠습니다.

## 요약

이 장에서는 다음에 대해 배웠습니다:

1. **RAW 촬영** — 최대 편집 유연성을 위해 처리되지 않은 센서 데이터 캡처
2. **CaptureRequest** — 각 캡처에 대한 카메라 설정 구성
3. **CaptureResult** — 완료된 캡처에 대한 메타데이터 가져오기

RAW 촬영에는 FULL 또는 LEVEL_3 하드웨어 레벨이 필요합니다. 캡처 세션에 두 표면을 모두 추가하여 JPEG와 RAW를 동시에 촬영할 수 있습니다.

CaptureRequest를 사용하면 자동 초점, 자동 노출, 화이트 밸런스 및 ISO, 노출 시간과 같은 수동 제어를 구성할 수 있습니다. CaptureResult는 카메라가 실제로 어떤 설정을 사용했는지 알려줍니다.

제IV부에서는 ISO, 노출, 초점, 화이트 밸런스 등 수동 카메라 제어에 대해 자세히 알아보겠습니다.
