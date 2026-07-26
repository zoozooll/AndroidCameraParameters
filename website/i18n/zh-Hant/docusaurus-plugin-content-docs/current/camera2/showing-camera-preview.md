---
sidebar_position: 9
title: "第 9 章：顯示攝像頭預覽"
description: 學習如何在 Camera2 中使用 TextureView、Surface 和 CameraCaptureSession 顯示攝像頭預覽。
keywords: [攝像頭預覽, TextureView, Surface, CameraCaptureSession, Camera2]
---

終於，你將看到攝像頭預覽了！讓我們把所有東西連接起來。

## 簡介

打開攝像頭很棒，但你還看不到任何東西。要顯示攝像頭看到的畫面，你需要：

1. 建立一個 TextureView 來顯示預覽
2. 從 TextureView 獲取 Surface
3. 建立 CameraCaptureSession
4. 啟動預覽

這就是各個部分組合在一起的地方。

## TextureView

TextureView 是一個可以顯示 `SurfaceTexture` 的視圖。它非常適合用於顯示攝像頭預覽，因為：
- 它可以被變換（縮放、旋轉）
- 它支援硬體加速
- 它與動畫和轉場效果搭配良好

在你的佈局中添加一個 TextureView：

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

Surface 是一個可以接收圖像數據的緩衝區。要顯示攝像頭預覽：
1. 從 TextureView 獲取 SurfaceTexture
2. 從 SurfaceTexture 建立 Surface
3. 將 Surface 傳遞給 CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession 管理捕獲過程。它將攝像頭設備連接到一個或多個 Surface。

要建立會話：
1. 準備 Surface 列表（用於預覽、照片拍攝等）
2. 在 CameraDevice 上呼叫 `createCaptureSession()`
3. 處理回調

## 完整的預覽範例

讓我們建立一個顯示攝像頭預覽的 Activity：

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
            // 如需處理尺寸變化
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // 預覽更新時呼叫
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
            Toast.makeText(this, "沒有可用的攝像頭", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // 取得預覽尺寸
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // 選擇預覽尺寸
        previewSize = previewSizes?.get(0) // 使用第一個可用尺寸

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "攝像頭權限被拒絕", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "攝像頭錯誤", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "會話設定失敗", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "需要攝像頭權限", Toast.LENGTH_SHORT).show()
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

## 運作原理

讓我們追蹤流程：

1. **TextureView 就緒** — 呼叫 `onSurfaceTextureAvailable()`
2. **打開攝像頭** — 我們獲得一個 CameraDevice
3. **建立捕獲會話** — 將攝像頭連接到 Surface
4. **啟動預覽** — 發送重複的捕獲請求

## CaptureRequest

CaptureRequest 定義攝像頭應該捕獲什麼：
- `TEMPLATE_PREVIEW` — 用於預覽模式
- `TEMPLATE_STILL_CAPTURE` — 用於靜態照片
- `TEMPLATE_RECORD` — 用於影片錄製
- `TEMPLATE_VIDEO_SNAPSHOT` — 用於影片期間的快照

## 預覽循環

當你呼叫 `setRepeatingRequest()` 時，攝像頭會不斷地將畫面發送到 Surface。這就建立了即時預覽。

## 重要注意事項

1. **Surface 必須就緒** — 在打開攝像頭之前等待 `onSurfaceTextureAvailable()`
2. **關閉資源** — 始終關閉捕獲會話和攝像頭設備
3. **處理方向** — 根據設備方向，預覽可能需要旋轉
4. **尺寸很重要** — 選擇與你的 TextureView 尺寸匹配的預覽尺寸

## 成功！

當你執行這個應用程式時，你應該會在螢幕上看到即時攝像頭預覽。恭喜你！你已經建立了你的第一個 Camera2 預覽應用程式。

## 下一章

既然你已經可以顯示預覽，下一步就是拍攝照片。在第三部分中，我們將學習：

1. 用於拍攝照片的 ImageReader
2. JPEG 和 RAW 捕獲
3. CaptureRequest 和 CaptureResult

## 總結

顯示攝像頭預覽涉及：

1. **TextureView** — 用於顯示預覽的 UI 元件
2. **Surface** — 接收攝像頭畫面的緩衝區
3. **CameraCaptureSession** — 管理捕獲過程
4. **CaptureRequest** — 定義要捕獲的內容
5. **setRepeatingRequest()** — 啟動連續預覽循環

你現在已經完成了本系列的第二部分。你可以：
- 發現攝像頭
- 檢查攝像頭特性
- 打開攝像頭
- 顯示預覽

在第三部分中，我們將學習如何使用 Camera2 拍攝照片。
