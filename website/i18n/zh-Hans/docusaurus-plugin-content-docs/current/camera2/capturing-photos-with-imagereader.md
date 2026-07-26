---
sidebar_position: 10
title: "第 10 章：使用 ImageReader 捕获照片"
description: 学习如何使用 ImageReader 捕获静态照片，它是从 Camera2 接收图像数据的关键组件。
keywords: [ImageReader, 照片捕获, JPEG, Camera2, 捕获请求]
---

现在你已经能够显示预览了，是时候捕获照片了。让我们来了解一下 ImageReader。

## 简介

要使用 Camera2 捕获照片，你需要一种接收图像数据的方法。这就是 **ImageReader** 的作用。

ImageReader 充当相机和应用程序之间的缓冲区。它从相机接收图像数据，并将其提供给你的应用程序进行处理或保存。

## 什么是 ImageReader？

ImageReader 是一个 Android 类，它允许你：
- 从相机接收图像数据
- 访问最新捕获的图像
- 配置图像格式和大小
- 设置要缓冲的最大图像数量

你可以创建具有特定格式和大小的 ImageReader：

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // 图像宽度
    height,     // 图像高度
    format,     // 图像格式（例如 ImageFormat.JPEG）
    maxImages   // 要缓冲的最大图像数量
)
```

## 图像格式

Camera2 支持多种图像格式：

| 格式 | 描述 |
| --- | --- |
| `ImageFormat.JPEG` | 标准压缩图像格式 |
| `ImageFormat.RAW_SENSOR` | 原始传感器数据（ISP 处理之前） |
| `ImageFormat.YUV_420_888` | 未压缩的 YUV 格式 |
| `ImageFormat.RAW10` | 10 位原始格式 |
| `ImageFormat.RAW12` | 12 位原始格式 |

对于大多数应用程序，JPEG 是照片捕获的最佳选择。

## 创建 ImageReader

以下是如何创建用于 JPEG 捕获的 ImageReader：

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // 在缓冲区中最多保留 2 张图像
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // 处理图像
    image.close()
}, null)
```

## 捕获照片

要捕获照片，你需要：
1. 创建一个 ImageReader
2. 将其 Surface 添加到捕获会话
3. 使用 `TEMPLATE_STILL_CAPTURE` 创建捕获请求
4. 将请求发送到相机

## 完整的照片捕获示例

让我们扩展我们的预览应用程序来捕获照片：

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

        // 创建用于照片捕获的 ImageReader
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
            Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@PhotoCaptureActivity, "会话配置失败", Toast.LENGTH_SHORT).show()
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
        
        // 将自动对焦设置为单次拍摄
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
            startPreview() // 捕获后恢复预览
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
            Toast.makeText(this, "照片已保存：$fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "保存照片失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
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

## 布局文件

在布局中添加一个捕获按钮：

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
        android:text="拍照"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## 工作原理

1. **创建 ImageReader** — 设置为接收 JPEG 图像
2. **将 Surface 添加到会话** — 相机将照片发送到此 surface
3. **创建捕获请求** — 使用 `TEMPLATE_STILL_CAPTURE` 进行拍照
4. **发送捕获请求** — 停止预览，捕获照片，恢复预览
5. **保存图像** — 将 JPEG 数据写入文件

## 照片的 CaptureRequest

对于静态捕获，请使用 `TEMPLATE_STILL_CAPTURE`。此模板优化了以下设置：
- 更高的分辨率
- 更好的图像质量
- 单次自动对焦

## 处理图像

始终记住：
1. **获取图像** — 使用 `acquireLatestImage()`
2. **处理图像** — 保存或显示图像
3. **关闭图像** — 始终调用 `image.close()` 来释放资源

## 下一章

在下一章中，我们将学习 RAW 捕获以及如何使用不同的图像格式。

## 总结

使用 Camera2 捕获照片涉及：

1. **ImageReader** — 从相机接收图像数据
2. **Surface** — 添加到捕获会话以用于照片输出
3. **TEMPLATE_STILL_CAPTURE** — 优化的捕获请求模板
4. **CaptureCallback** — 捕获完成时通知
5. **图像处理** — 保存或显示捕获的图像

你现在已经学会了如何捕获基本照片。在下一章中，我们将探索 RAW 捕获和高级照片功能。
