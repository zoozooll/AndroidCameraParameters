---
sidebar_position: 8
title: "제8장: 미리보기 보여주기"
description: "안드로이드 TextureView와 CameraCaptureSession을 사용하여 실시간 카메라 미리보기를 구현하세요. Surface 준비, 캡처 세션 구성, 그리고 TEMPLATE_PREVIEW를 사용하여 반복적인 프레임 스트리밍을 설정하는 방법을 배웁니다."
keywords: [안드로이드 camera2 미리보기, TextureView, CameraCaptureSession, Surface, TEMPLATE_PREVIEW, setRepeatingRequest, 카메라 뷰파인더]
---

# 제8장: 미리보기 보여주기

카메라를 열었다면 이제 사용자가 카메라가 무엇을 보는지 확인할 수 있도록 **미리보기(뷰파인더)**를 띄울 차례입니다. Camera2 API에서 미리보기를 보여주는 과정은 "수도꼭지를 틀어 물을 대야에 담는 것"과 비슷합니다. 카메라(수도꼭지)에서 나오는 데이터를 화면(대야)으로 연결해주는 **세션(Session)**을 만들어야 합니다.

이 장에서는 가장 널리 쓰이는 `TextureView`를 사용하여 라이브 화면을 구성하는 방법을 배웁니다.

---

## 1. 레이아웃에 TextureView 추가하기

먼저 프레임을 그려낼 도화지가 필요합니다. `activity_main.xml`에 `TextureView`를 넣습니다.

```xml
<TextureView
    android:id="@+id/textureView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

---

## 2. Surface 준비하기

카메라 하드웨어는 데이터를 `Surface`라는 그릇에 담아 보냅니다. `TextureView`가 준비되면 그로부터 `Surface`를 추출해야 합니다.

```kotlin
private fun startPreview() {
    val texture = textureView.surfaceTexture
    // 미리보기 크기를 설정합니다 (보통 1080p나 720p).
    texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
    
    val previewSurface = Surface(texture)
    // 이제 이 previewSurface가 카메라 데이터의 목적지가 됩니다.
}
```

---

## 3. CameraCaptureSession 생성

이제 카메라와 화면을 잇는 통로인 세션을 만듭니다.

```kotlin
cameraDevice?.createCaptureSession(
    listOf(previewSurface), // 이 세션에서 사용할 모든 목적지 리스트
    object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            // 통로가 성공적으로 만들어졌습니다!
            // 이제 실제로 데이터를 흘려보냅니다.
            val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(previewSurface)
            
            // 멈추라고 할 때까지 계속 프레임을 보내라고 명령합니다.
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e("Camera", "세션 생성 실패")
        }
    }, backgroundHandler
)
```

---

## 4. TEMPLATE_PREVIEW의 의미

`createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)`는 안드로이드 시스템이 제공하는 "미리보기 최적화 설정 세트"입니다.
- **프레임 속도 우선:** 끊김 없는 화면을 위해 노출 시간을 자동으로 조절합니다.
- **전력 효율:** 배터리를 아끼기 위해 이미지 품질을 적절히 타협합니다.
- **자동 초점:** 사용자가 별도로 조작하지 않아도 계속해서 초점을 맞추려고 노력합니다.

---

## 흔히 발생하는 실수: 화면 비율(Aspect Ratio)

미리보기를 띄웠는데 내 얼굴이 너무 길어 보이거나 뚱뚱해 보인다면? 그것은 `TextureView`의 크기와 카메라가 보내주는 `previewSize`의 비율이 맞지 않기 때문입니다. 이를 해결하려면 `TextureView`에 `Matrix` 변환을 적용하여 비율을 맞춰야 합니다. (이 테크닉은 9장 사진 촬영 이후 상세히 다룹니다.)

---

## 요약

1. **TextureView**로 화면에 그릴 준비를 합니다.
2. `Surface`를 만들어 카메라 데이터의 **목적지**로 지정합니다.
3. `createCaptureSession`으로 **데이터 통로**를 만듭니다.
4. `setRepeatingRequest`로 **끊임없이 프레임을 생성**하도록 명령합니다.

## 다음 단계

화면이 나오기 시작하니 이제 진짜 카메라 앱 같습니다! 이제 결정적인 순간을 영원히 기록할 차례입니다. **제9장: 사진 촬영하기**에서는 `ImageReader`를 추가하여 고해상도 JPEG 파일을 저장하는 법을 배웁니다.
