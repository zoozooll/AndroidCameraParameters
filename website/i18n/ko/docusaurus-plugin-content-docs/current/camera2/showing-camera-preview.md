---
sidebar_position: 9
title: "9장: 카메라 미리보기 표시하기"
description: Camera2에서 TextureView, Surface, CameraCaptureSession을 사용하여 카메라 미리보기를 표시하는 방법을 알아보세요.
keywords: [카메라 미리보기, TextureView, Surface, CameraCaptureSession, Camera2]
---

드디어 카메라 미리보기를 볼 수 있습니다! 모든 것을 함께 연결해 봅시다.

## 소개

카메라를 여는 것은 좋지만, 아직 아무것도 볼 수 없습니다. 카메라가 보는 것을 표시하려면 다음이 필요합니다:

1. 미리보기를 표시할 TextureView 만들기
2. TextureView에서 Surface 가져오기
3. CameraCaptureSession 만들기
4. 미리보기 시작하기

이것이 모든 조각이 합쳐지는 부분입니다.

## TextureView

TextureView는 `SurfaceTexture`를 표시할 수 있는 뷰입니다. 다음과 같은 이유로 카메라 미리보기를 보여주는 데 완벽합니다:
- 변형할 수 있습니다(확대/축소, 회전)
- 하드웨어 가속을 지원합니다
- 애니메이션 및 전환과 잘 작동합니다

레이아웃에 TextureView를 추가하세요:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</FrameLayout>
```

## Surface

Surface는 이미지 데이터를 받을 수 있는 버퍼입니다. 카메라 미리보기를 표시하려면:
1. TextureView에서 SurfaceTexture 가져오기
2. SurfaceTexture에서 Surface 만들기
3. Surface를 CameraCaptureSession에 전달하기

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession은 캡처 프로세스를 관리합니다. 카메라 장치를 하나 이상의 Surface에 연결합니다.

세션을 만들려면:
1. Surface 목록 준비하기(미리보기, 사진 촬영 등)
2. CameraDevice에서 `createCaptureSession()` 호출하기
3. 콜백 처리하기

## 완전한 미리보기 예제

카메라 미리보기를 보여주는 액티비티를 만들어 봅시다:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Collections

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            // 필요한 경우 크기 변경 처리
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // 미리보기가 업데이트될 때 호출됨
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) {
            Toast.makeText(this, "사용 가능한 카메라가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // 미리보기 크기 가져오기
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // 미리보기 크기 선택
        previewSize = previewSizes?.get(0) // 첫 번째 사용 가능한 크기 사용

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "카메라 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
            Toast.makeText(this@CameraPreviewActivity, "카메라 오류", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val surface = Surface(texture)
        val surfaces = listOf(surface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@CameraPreviewActivity, "세션 구성에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(surface)
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.stopRepeating()
        captureSession?.close()
        cameraDevice?.close()
    }
}
```

## 작동 방식

흐름을 따라가 봅시다:

1. **TextureView 사용 가능** — `onSurfaceTextureAvailable()`가 호출됨
2. **카메라 열기** — CameraDevice를 얻음
3. **캡처 세션 만들기** — 카메라를 Surface에 연결
4. **미리보기 시작** — 반복 캡처 요청 보내기

## CaptureRequest

CaptureRequest는 카메라가 무엇을 캡처할지 정의합니다:
- `TEMPLATE_PREVIEW` — 미리보기 모드용
- `TEMPLATE_STILL_CAPTURE` — 정지 사진용
- `TEMPLATE_RECORD` — 동영상 녹화용
- `TEMPLATE_VIDEO_SNAPSHOT` — 동영상 중 스냅샷용

## 미리보기 루프

`setRepeatingRequest()`를 호출하면 카메라는 지속적으로 프레임을 Surface로 보냅니다. 이것이 라이브 미리보기를 만듭니다.

## 중요 사항

1. **Surface가 사용 가능해야 함** — 카메라를 열기 전에 `onSurfaceTextureAvailable()`를 기다리세요
2. **리소스 닫기** — 항상 캡처 세션과 카메라 장치를 닫으세요
3. **방향 처리** — 장치 방향에 따라 미리보기에 회전이 필요할 수 있습니다
4. **크기가 중요함** — TextureView 치수와 일치하는 미리보기 크기를 선택하세요

## 성공!

이 앱을 실행하면 화면에 라이브 카메라 미리보기가 보일 것입니다. 축하합니다! 첫 번째 Camera2 미리보기 앱을 만들었습니다.

## 다음 장

이제 미리보기를 표시할 수 있으므로, 다음 단계는 사진을 캡처하는 것입니다. 3부에서는 다음에 대해 배울 것입니다:

1. 사진 캡처를 위한 ImageReader
2. JPEG 및 RAW 캡처
3. CaptureRequest와 CaptureResult

## 요약

카메라 미리보기를 표시하는 방법은 다음과 같습니다:

1. **TextureView** — 미리보기를 표시하는 UI 컴포넌트
2. **Surface** — 카메라 프레임을 받는 버퍼
3. **CameraCaptureSession** — 캡처 프로세스 관리
4. **CaptureRequest** — 무엇을 캡처할지 정의
5. **setRepeatingRequest()** — 연속 미리보기 루프 시작

이제 이 시리즈의 2부를 완료했습니다. 다음을 할 수 있습니다:
- 카메라 찾기
- 카메라 특성 검사하기
- 카메라 열기
- 미리보기 표시하기

3부에서는 Camera2로 사진을 캡처하는 방법을 배울 것입니다.
