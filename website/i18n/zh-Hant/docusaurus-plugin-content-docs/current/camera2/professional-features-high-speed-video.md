---
sidebar_position: 14
title: "第 14 章：專業功能 - 高速錄影"
description: 了解如何在 Camera2 中拍攝高速錄影以及使用多鏡頭設定。
keywords: [高速錄影, 多鏡頭, Camera2, 受限高速, 邏輯相機]
---

專業功能開啟了新的創意可能性。讓我們來探索高速錄影和多鏡頭功能。

## 介紹

Camera2 支援許多超越基本拍照功能的專業功能。在本章中，我們將學習：

1. **高速錄影** — 拍攝慢動作影片
2. **多鏡頭** — 使用邏輯和實體相機

## 高速錄影

高速錄影讓你能夠以高於標準 30fps 的影格速率拍攝影片：
- 120fps — 流暢的慢動作
- 240fps — 標準慢動作
- 480fps — 極限慢動作
- 960fps — 超級慢動作

### 需求

要拍攝高速錄影，你的相機必須：
1. 支援 `CONSTRAINED_HIGH_SPEED_VIDEO` 功能
2. 具備正確的硬體等級

檢查 CameraCharacteristics：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### 高速錄影解析度

高速錄影使用與標準錄影不同的解析度：

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### 拍攝高速錄影

高速錄影需要特殊的擷取工作階段：

```kotlin
// 建立高速擷取工作階段
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "高速工作階段失敗", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### 啟動高速預覽

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // 取得支援的高速 fps 範圍
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // 尋找高速範圍（例如 120fps）
    val highSpeedFpsRange = fpsRanges?.find { it.upper >= 120 }
    
    captureRequestBuilder?.apply {
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange)
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.setRepeatingRequest(builder.build(), null, null)
    }
}
```

## 多鏡頭

現代手機擁有多個相機。Camera2 將它們分為：
- **實體相機** — 個別的相機感應器
- **邏輯相機** — 多個實體相機的組合

### 邏輯與實體相機

| 類型 | 說明 |
| --- | --- |
| **實體** | 單一相機感應器（廣角、望遠、超廣角） |
| **邏輯** | 結合多個實體相機的虛擬相機 |

### 識別相機類型

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// 檢查是否為邏輯相機
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// 取得實體相機 ID（適用於邏輯相機）
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### 切換相機

要切換相機，你需要：
1. 關閉目前的相機
2. 開啟新的相機
3. 建立新的擷取工作階段

```kotlin
private fun switchCamera(newCameraId: String) {
    // 關閉目前的相機
    captureSession?.close()
    cameraDevice?.close()
    
    // 開啟新的相機
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### 並行相機

部分裝置支援同時開啟多個相機：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## 相機擴充功能

相機擴充功能讓你能夠使用製造商專屬的相機功能：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// 取得可用的擴充功能
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

常見的擴充功能：
- `EXTENSION_BOKEH` — 人像模式
- `EXTENSION_HDR` — HDR 模式
- `EXTENSION_NIGHT` — 夜拍模式
- `EXTENSION_AUTO` — 自動模式

## 多鏡頭範例

```kotlin
class MultiCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_camera)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupCameraSelector()
    }

    private fun setupCameraSelector() {
        val cameraIds = cameraManager.cameraIdList
        val spinner = findViewById<Spinner>(R.id.cameraSpinner)
        
        val cameraNames = cameraIds.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "前鏡頭 ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "後鏡頭 ($id)"
                else -> "相機 $id"
            }
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newCameraId = cameraIds[position]
                if (newCameraId != currentCameraId) {
                    switchCamera(newCameraId)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun switchCamera(newCameraId: String) {
        captureSession?.close()
        cameraDevice?.close()
        currentCameraId = newCameraId
        openCamera()
    }

    private fun openCamera() {
        try {
            cameraManager.openCamera(currentCameraId, cameraStateCallback, null)
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
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "工作階段失敗", Toast.LENGTH_SHORT).show()
            }
        }, null)
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

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
        }
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }
}
```

## 最佳實務

1. **檢查功能** — 使用專業功能前務必先驗證
2. **處理轉換** — 切換相機時平順地關閉/開啟
3. **管理資源** — 高速錄影會消耗更多資源
4. **優雅降級** — 當功能無法使用時提供替代方案

## 下一章

在最後一章中，我們將探索 CameraCharacteristics 百科全書 — 深入了解最重要的相機參數。

## 總結

專業功能擴展了你的創意可能性：

1. **高速錄影** — 以 120-960fps 拍攝慢動作
2. **多鏡頭** — 使用邏輯和實體相機
3. **相機擴充功能** — 使用製造商專屬功能
4. **並行相機** — 同時開啟多個相機

在下一章中，我們將深入探討 CameraCharacteristics — 相機參數的百科全書。
