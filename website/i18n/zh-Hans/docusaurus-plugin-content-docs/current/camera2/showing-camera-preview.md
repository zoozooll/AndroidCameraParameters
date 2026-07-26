---
sidebar_position: 9
title: "第 9 章：显示相机预览"
description: 学习如何在 Camera2 中使用 TextureView、Surface 和 CameraCaptureSession 显示相机预览。
keywords: [相机预览, TextureView, Surface, CameraCaptureSession, Camera2]
---

终于，你将看到相机预览了！让我们把所有内容连接起来。

## 简介

打开相机很棒，但你还看不到任何东西。要显示相机看到的内容，你需要：

1. 创建一个 TextureView 来显示预览
2. 从 TextureView 获取一个 Surface
3. 创建一个 CameraCaptureSession
4. 启动预览

这就是各个部分组合在一起的地方。

## TextureView

TextureView 是一个可以显示 `SurfaceTexture` 的视图。它非常适合显示相机预览，因为：
- 它可以进行变换（缩放、旋转）
- 它支持硬件加速
- 它与动画和过渡效果配合良好

在你的布局中添加一个 TextureView：

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

Surface 是一个可以接收图像数据的缓冲区。要显示相机预览：
1. 从 TextureView 获取 SurfaceTexture
2. 从 SurfaceTexture 创建一个 Surface
3. 将 Surface 传递给 CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession 管理捕获过程。它将相机设备连接到一个或多个 Surface。

要创建会话：
1. 准备一个 Surface 列表（用于预览、拍照等）
2. 在 CameraDevice 上调用 `createCaptureSession()`
3. 处理回调

## 完整的预览示例

让我们创建一个显示相机预览的 Activity：

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
            // 如需处理尺寸变化
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // 预览更新时调用
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
            Toast.makeText(this, "没有可用的相机", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // 获取预览尺寸
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // 选择预览尺寸
        previewSize = previewSizes?.get(0) // 使用第一个可用尺寸

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
            Toast.makeText(this@CameraPreviewActivity, "相机错误", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "会话配置失败", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
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

## 工作原理

让我们追踪一下流程：

1. **TextureView 可用** — 调用 `onSurfaceTextureAvailable()`
2. **打开相机** — 我们获得一个 CameraDevice
3. **创建捕获会话** — 将相机连接到 Surface
4. **启动预览** — 发送重复的捕获请求

## CaptureRequest

CaptureRequest 定义了相机应该捕获什么：
- `TEMPLATE_PREVIEW` — 用于预览模式
- `TEMPLATE_STILL_CAPTURE` — 用于静态照片
- `TEMPLATE_RECORD` — 用于视频录制
- `TEMPLATE_VIDEO_SNAPSHOT` — 用于视频中的快照

## 预览循环

当你调用 `setRepeatingRequest()` 时，相机会持续向 Surface 发送帧。这就创建了实时预览。

## 注意事项

1. **Surface 必须可用** — 在打开相机之前等待 `onSurfaceTextureAvailable()`
2. **关闭资源** — 始终关闭捕获会话和相机设备
3. **处理方向** — 根据设备方向，预览可能需要旋转
4. **尺寸很重要** — 选择与 TextureView 尺寸匹配的预览尺寸

## 成功！

当你运行这个应用时，你应该在屏幕上看到一个实时相机预览。恭喜！你已经构建了你的第一个 Camera2 预览应用。

## 下一章

既然你可以显示预览了，下一步就是拍摄照片。在第三部分中，我们将学习：

1. 用于拍摄照片的 ImageReader
2. JPEG 和 RAW 捕获
3. CaptureRequest 和 CaptureResult

## 总结

显示相机预览涉及：

1. **TextureView** — 用于显示预览的 UI 组件
2. **Surface** — 接收相机帧的缓冲区
3. **CameraCaptureSession** — 管理捕获过程
4. **CaptureRequest** — 定义要捕获的内容
5. **setRepeatingRequest()** — 启动连续预览循环

你现在已经完成了本系列的第二部分。你可以：
- 发现相机
- 检查相机特性
- 打开相机
- 显示预览

在第三部分中，我们将学习如何使用 Camera2 拍摄照片。
