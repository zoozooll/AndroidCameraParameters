---
sidebar_position: 10
title: "第十章：使用 ImageReader 拍攝照片"
description: 學習如何使用 ImageReader 拍攝靜態照片，ImageReader 是從 Camera2 接收影像資料的關鍵元件。
keywords: [ImageReader, 照片拍攝, JPEG, Camera2, 拍攝請求]
---

既然你已經能夠顯示預覽畫面，現在是時候拍攝照片了。讓我們來了解 ImageReader。

## 簡介

要使用 Camera2 拍攝照片，你需要一種接收影像資料的方式。這就是 **ImageReader** 的作用。

ImageReader 充當相機與應用程式之間的緩衝區。它從相機接收影像資料，並將其提供給你的應用程式進行處理或儲存。

## 什麼是 ImageReader？

ImageReader 是一個 Android 類別，可讓你：
- 從相機接收影像資料
- 存取最新拍攝的影像
- 設定影像格式和尺寸
- 設定要緩衝的最大影像數量

你可以使用特定的格式和尺寸建立 ImageReader：

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // 影像寬度
    height,     // 影像高度
    format,     // 影像格式（例如 ImageFormat.JPEG）
    maxImages   // 要緩衝的最大影像數量
)
```

## 影像格式

Camera2 支援多種影像格式：

| 格式 | 說明 |
| --- | --- |
| `ImageFormat.JPEG` | 標準壓縮影像格式 |
| `ImageFormat.RAW_SENSOR` | 原始感測器資料（ISP 處理前） |
| `ImageFormat.YUV_420_888` | 未壓縮的 YUV 格式 |
| `ImageFormat.RAW10` | 10 位元原始格式 |
| `ImageFormat.RAW12` | 12 位元原始格式 |

對於大多數應用程式，JPEG 是照片拍攝的最佳選擇。

## 建立 ImageReader

以下是建立用於 JPEG 拍攝的 ImageReader 的方法：

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // 在緩衝區中最多保留 2 張影像
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // 處理影像
    image.close()
}, null)
```

## 拍攝照片

要拍攝照片，你需要：
1. 建立一個 ImageReader
2. 將其 Surface 加入拍攝會話
3. 使用 `TEMPLATE_STILL_CAPTURE` 建立拍攝請求
4. 將請求發送到相機

## 完整的照片拍攝範例

讓我們擴展預覽應用程式來拍攝照片：

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

        // 建立用於照片拍攝的 ImageReader
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
            Toast.makeText(this, "相機權限被拒絕", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@PhotoCaptureActivity, "會話設定失敗", Toast.LENGTH_SHORT).show()
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
        
        // 設定自動對焦為單次拍攝
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
            startPreview() // 拍攝後恢復預覽
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
            Toast.makeText(this, "照片已儲存：$fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "儲存照片失敗", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "需要相機權限", Toast.LENGTH_SHORT).show()
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

## 佈局檔案

在你的佈局中加入一個拍攝按鈕：

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
        android:text="拍攝"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## 運作原理

1. **建立 ImageReader** — 設定為接收 JPEG 影像
2. **將 Surface 加入會話** — 相機將照片發送到這個 surface
3. **建立拍攝請求** — 使用 `TEMPLATE_STILL_CAPTURE` 進行照片拍攝
4. **發送拍攝請求** — 停止預覽、拍攝照片、恢復預覽
5. **儲存影像** — 將 JPEG 資料寫入檔案

## 用於照片的 CaptureRequest

對於靜態拍攝，請使用 `TEMPLATE_STILL_CAPTURE`。這個範本會針對以下項目最佳化設定：
- 更高的解析度
- 更好的影像品質
- 單次拍攝自動對焦

## 處理影像

請永遠記得：
1. **取得影像** — 使用 `acquireLatestImage()`
2. **處理它** — 儲存或顯示影像
3. **關閉它** — 永遠呼叫 `image.close()` 來釋放資源

## 下一章

在下一章中，我們將學習 RAW 拍攝以及如何處理不同的影像格式。

## 總結

使用 Camera2 拍攝照片包含：

1. **ImageReader** — 從相機接收影像資料
2. **Surface** — 加入拍攝會話以輸出照片
3. **TEMPLATE_STILL_CAPTURE** — 最佳化的拍攝請求範本
4. **CaptureCallback** — 拍攝完成時發出通知
5. **影像處理** — 儲存或顯示拍攝的影像

你現在已經學會如何拍攝基本的照片。在下一章中，我們將探索 RAW 拍攝和進階照片功能。
