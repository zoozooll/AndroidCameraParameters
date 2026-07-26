---
sidebar_position: 10
title: "10장: ImageReader로 사진 촬영하기"
description: Camera2에서 이미지 데이터를 수신하는 핵심 구성 요소인 ImageReader를 사용하여 정지 사진을 촬영하는 방법을 알아봅니다.
keywords: [ImageReader, 사진 촬영, JPEG, Camera2, 캡처 요청]
---

이제 프리뷰를 표시할 수 있으니, 사진을 촬영할 차례입니다. ImageReader에 대해 알아봅시다.

## 소개

Camera2로 사진을 촬영하려면 이미지 데이터를 수신할 방법이 필요합니다. 그 역할을 하는 것이 **ImageReader**입니다.

ImageReader는 카메라와 애플리케이션 사이의 버퍼 역할을 합니다. 카메라에서 이미지 데이터를 받아 앱이 처리하거나 저장할 수 있도록 제공합니다.

## ImageReader란 무엇인가요?

ImageReader는 다음과 같은 작업을 할 수 있는 Android 클래스입니다:
- 카메라에서 이미지 데이터 수신
- 최근에 촬영된 이미지에 접근
- 이미지 형식과 크기 구성
- 버퍼에 저장할 최대 이미지 개수 설정

특정 형식과 크기로 ImageReader를 생성합니다:

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // 이미지 너비
    height,     // 이미지 높이
    format,     // 이미지 형식 (예: ImageFormat.JPEG)
    maxImages   // 버퍼에 저장할 최대 이미지 개수
)
```

## 이미지 형식

Camera2는 여러 이미지 형식을 지원합니다:

| 형식 | 설명 |
| --- | --- |
| `ImageFormat.JPEG` | 표준 압축 이미지 형식 |
| `ImageFormat.RAW_SENSOR` | 원본 센서 데이터 (ISP 처리 전) |
| `ImageFormat.YUV_420_888` | 비압축 YUV 형식 |
| `ImageFormat.RAW10` | 10비트 원본 형식 |
| `ImageFormat.RAW12` | 12비트 원본 형식 |

대부분의 애플리케이션에서 사진 촬영에는 JPEG가 최선의 선택입니다.

## ImageReader 생성하기

JPEG 촬영을 위한 ImageReader를 생성하는 방법은 다음과 같습니다:

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // 버퍼에 최대 2개의 이미지 유지
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // 이미지 처리
    image.close()
}, null)
```

## 사진 촬영하기

사진을 촬영하려면 다음 단계를 따라야 합니다:
1. ImageReader 생성
2. 캡처 세션에 해당 Surface 추가
3. `TEMPLATE_STILL_CAPTURE`로 캡처 요청 생성
4. 카메라에 요청 전송

## 완전한 사진 촬영 예제

프리뷰 앱을 확장하여 사진을 촬영해 봅시다:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoCaptureActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null
    private var pictureSize: Size? = null
    private var imageReader: ImageReader? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_capture)
        
        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.captureButton)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) return

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
        
        previewSize = previewSizes?.get(0)
        pictureSize = pictureSizes?.get(0)

        // 사진 촬영을 위한 ImageReader 생성
        imageReader = ImageReader.newInstance(
            pictureSize?.width ?: 0,
            pictureSize?.height ?: 0,
            ImageFormat.JPEG,
            2
        )
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            saveImage(image)
            image.close()
        }, null)

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
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val previewSurface = Surface(texture)
        val readerSurface = imageReader?.surface ?: return
        
        val surfaces = listOf(previewSurface, readerSurface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@PhotoCaptureActivity, "세션 구성에 실패했습니다", Toast.LENGTH_SHORT).show()
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

    private fun takePhoto() {
        val readerSurface = imageReader?.surface ?: return
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder?.addTarget(readerSurface)
        
        // 자동 초점을 단일 촬영으로 설정
        captureRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        
        captureRequestBuilder?.let { builder ->
            captureSession?.stopRepeating()
            captureSession?.capture(builder.build(), captureCallback, null)
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            startPreview() // 촬영 후 프리뷰 재개
        }
    }

    private fun saveImage(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, fileName)
        
        try {
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            Toast.makeText(this, "사진이 저장되었습니다: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "사진 저장에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
        imageReader?.close()
    }
}
```

## 레이아웃

레이아웃에 촬영 버튼을 추가하세요:

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

    <Button
        android:id="@+id/captureButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="촬영"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## 작동 방식

1. **ImageReader 생성** — JPEG 이미지를 수신하도록 설정
2. **세션에 Surface 추가** — 카메라가 이 Surface로 사진을 전송
3. **캡처 요청 생성** — 사진에 `TEMPLATE_STILL_CAPTURE` 사용
4. **캡처 요청 전송** — 프리뷰 중지, 사진 촬영, 프리뷰 재개
5. **이미지 저장** — JPEG 데이터를 파일로 저장

## 사진을 위한 CaptureRequest

정지 촬영에는 `TEMPLATE_STILL_CAPTURE`를 사용하세요. 이 템플릿은 다음을 위해 설정을 최적화합니다:
- 더 높은 해상도
- 더 나은 이미지 품질
- 단일 촬영 자동 초점

## 이미지 처리하기

항상 다음을 기억하세요:
1. **이미지 가져오기** — `acquireLatestImage()` 사용
2. **처리하기** — 이미지를 저장하거나 표시
3. **닫기** — 리소스를 해제하기 위해 항상 `image.close()` 호출

## 다음 장

다음 장에서는 RAW 촬영과 다양한 이미지 형식을 다루는 방법에 대해 알아봅니다.

## 요약

Camera2로 사진을 촬영하는 방법:

1. **ImageReader** — 카메라에서 이미지 데이터 수신
2. **Surface** — 사진 출력을 위해 캡처 세션에 추가
3. **TEMPLATE_STILL_CAPTURE** — 최적화된 캡처 요청 템플릿
4. **CaptureCallback** — 촬영 완료 시 알림
5. **이미지 처리** — 촬영된 이미지 저장 또는 표시

이제 기본적인 사진 촬영 방법을 배웠습니다. 다음 장에서는 RAW 촬영과 고급 사진 기능을 살펴보겠습니다.
