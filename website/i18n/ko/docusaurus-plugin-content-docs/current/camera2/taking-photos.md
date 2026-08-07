---
sidebar_position: 9
title: "제9장: 사진 촬영하기"
description: "Camera2를 사용하여 사진을 찍고 저장하는 전체 과정을 마스터하세요. ImageReader 설정, TEMPLATE_STILL_CAPTURE를 이용한 캡처 요청 빌드, JPEG 인코딩 처리, 그리고 안드로이드 MediaStore를 이용한 갤러리 저장 방법을 배웁니다."
keywords: [안드로이드 camera2 사진 촬영, ImageReader, TEMPLATE_STILL_CAPTURE, JPEG 저장, MediaStore 저장, 사진 앱 개발, 안드로이드 카메라 튜토리얼]
---

# 제9장: 사진 촬영하기

드디어 카메라 개발의 "꽃"인 사진 촬영 단계에 왔습니다! 8장에서 라이브 미리보기를 구현했다면, 이제는 그 결정적인 순간을 고해상도 파일로 영구히 기록할 차례입니다.

Camera2에서 사진을 찍는 것은 단순히 함수 하나를 호출하는 것보다 조금 더 복잡합니다. 픽셀 데이터를 받아낼 **그릇(ImageReader)**을 준비하고, 하드웨어에 **"지금 찍어!"(CaptureRequest)**라고 명령한 뒤, 결과물인 **바이트 데이터를 파일로** 옮겨 적어야 합니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Capture** 탭을 보면 이 과정이 어떻게 시각화되는지 확인할 수 있습니다. 셔터를 누를 때 파이프라인의 상태가 어떻게 변하는지 관찰해 보세요.

---

## 사진 촬영의 4단계 프로세스

Camera2에서 사진을 성공적으로 찍으려면 다음 네 단계를 거쳐야 합니다.

1. **ImageReader 준비:** 카메라가 생성한 이미지를 받아낼 버퍼를 만듭니다.
2. **출력 타겟 추가:** 8장에서 만든 미리보기용 세션에 이 `ImageReader`의 `Surface`를 포함시킵니다.
3. **캡처 요청 제출:** `TEMPLATE_STILL_CAPTURE`를 사용하여 고화질 촬영 명령을 보냅니다.
4. **이미지 저장:** `ImageReader`를 통해 들어온 데이터를 파일(JPEG 등)로 저장합니다.

---

## 1단계: ImageReader 설정하기

`ImageReader`는 카메라 하드웨어가 생성한 이미지 데이터에 직접 접근할 수 있게 해주는 클래스입니다.

```kotlin
private var imageReader: ImageReader? = null

private fun setupImageReader(width: Int, height: Int) {
    // 최대 해상도로 설정하는 것이 일반적입니다.
    // 여기서는 4:3 비율의 12MP(4000x3000)를 예로 듭니다.
    imageReader = ImageReader.newInstance(
        width, height, 
        ImageFormat.JPEG, /* format */
        2                /* maxImages: 버퍼에 동시에 담길 수 있는 최대 이미지 수 */
    )

    // 이미지가 준비되었을 때 호출될 리스너를 연결합니다.
    imageReader?.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
        
        // 여기서 이미지를 처리(저장)합니다. (4단계에서 상술)
        saveImage(image)
        
        image.close() // 중요! 처리가 끝나면 반드시 닫아야 합니다.
    }, backgroundHandler)
}
```

---

## 2단계: 세션에 ImageReader 추가하기

미리보기만 할 때와 달리, 세션을 만들 때 사진 촬영용 `Surface`도 같이 등록해야 합니다.

```kotlin
private fun createCaptureSession() {
    val previewSurface = Surface(textureView.surfaceTexture)
    val captureSurface = imageReader?.surface // 사진용 Surface

    // 세션을 생성할 때 두 개의 Surface를 모두 알려줍니다.
    cameraDevice?.createCaptureSession(
        listOf(previewSurface, captureSurface),
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                // 미리보기 시작 (8장 내용)
                startPreview(session)
            }
            // ... 생략 ...
        }, backgroundHandler
    )
}
```

---

## 3단계: 셔터 버튼 누르기 (캡처 요청)

사용자가 화면의 촬영 버튼을 눌렀을 때 실행될 코드입니다. `capture()` 메서드를 사용합니다.

```kotlin
private fun takePhoto() {
    val session = captureSession ?: return
    val device = cameraDevice ?: return

    // 1. 사진 촬영용 템플릿으로 빌더 생성
    val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    // 2. 어디로 데이터를 보낼지 지정 (ImageReader의 Surface)
    captureBuilder.addTarget(imageReader!!.surface)

    // 3. 사진 품질 및 설정 (필요 시)
    captureBuilder.set(CaptureRequest.JPEG_QUALITY, 95)
    
    // 4. 단발성(One-shot) 요청 제출
    session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureStarted(...) {
            // 셔터 소리를 내거나 화면을 깜빡이는 효과를 줄 수 있습니다.
        }
        override fun onCaptureCompleted(...) {
            // 촬영 명령이 하드웨어에 전달 완료됨
            Log.d("Camera", "Capture Completed")
        }
    }, backgroundHandler)
}
```

---

## 4단계: 이미지 파일로 저장하기 (MediaStore)

이제 `ImageReader`의 리스너에서 받은 데이터를 실제 파일로 저장할 차례입니다. 최신 안드로이드 가이드에 따라 `MediaStore`를 사용하여 갤러리에 저장하는 방법을 알아보겠습니다.

```kotlin
private fun saveImage(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/MyCameraApp")
    }

    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    
    uri?.let { 
        contentResolver.openOutputStream(it)?.use { outputStream ->
            outputStream.write(bytes)
            Log.d("Camera", "사진 저장 완료: $filename")
        }
    }
}
```

---

## 주의사항: 방향(Orientation) 문제

사진을 찍고 갤러리에서 확인해 보면 사진이 옆으로 누워 있는 경우가 많습니다. 이는 센서의 물리적인 설치 방향 때문입니다. 이를 해결하려면 캡처 요청 시 장치의 회전 상태를 계산하여 `JPEG_ORIENTATION`을 설정해야 합니다. (이 내용은 부록: 카메라 회전 마스터하기에서 자세히 다룹니다.)

---

## 요약

1. **ImageReader**를 만들어 결과물을 받을 준비를 합니다.
2. **세션**을 만들 때 `ImageReader`의 `Surface`를 포함시킵니다.
3. `TEMPLATE_STILL_CAPTURE`로 **사진 촬영 명령**을 내립니다.
4. 이미지가 도착하면 **바이트 데이터를 파일로 저장**합니다.

## 다음 단계

이제 사진을 찍을 수 있게 되었습니다! 하지만 단순히 찍는 것을 넘어, 더 밝게 혹은 더 선명하게 찍고 싶지 않으신가요? **제10장: 수동 컨트롤 (노출과 초점)**에서는 ISO, 셔터 속도, 초점을 직접 조절하여 전문가용 카메라 앱을 만드는 방법을 알아봅니다.
