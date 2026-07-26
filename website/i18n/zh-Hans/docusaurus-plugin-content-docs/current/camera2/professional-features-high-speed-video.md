---
sidebar_position: 14
title: "第14章：专业功能 - 高速视频"
description: 学习如何在 Camera2 中拍摄高速视频以及使用多相机设置。
keywords: [高速视频, 多相机, Camera2, 受限高速, 逻辑相机]
---

专业功能开启了新的创作可能性。让我们来探索高速视频和多相机。

## 简介

Camera2 支持许多超越基本拍照的专业功能。在本章中，我们将学习：

1. **高速视频** — 拍摄慢动作视频
2. **多相机** — 使用逻辑相机和物理相机

## 高速视频

高速视频让你能够以高于标准 30fps 的帧率拍摄视频：
- 120fps — 流畅的慢动作
- 240fps — 标准慢动作
- 480fps — 极致慢动作
- 960fps — 超级慢动作

### 要求

要拍摄高速视频，你的相机必须：
1. 支持 `CONSTRAINED_HIGH_SPEED_VIDEO` 能力
2. 具备相应的硬件级别

检查 CameraCharacteristics：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### 高速视频尺寸

高速视频使用的分辨率与标准视频不同：

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### 拍摄高速视频

高速视频需要特殊的拍摄会话：

```kotlin
// 创建高速拍摄会话
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "高速会话创建失败", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### 启动高速预览

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // 获取支持的高速 fps 范围
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // 查找高速范围（例如 120fps）
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

## 多相机

现代手机配备了多个相机。Camera2 将它们分为：
- **物理相机** — 独立的相机传感器
- **逻辑相机** — 多个物理相机的组合

### 逻辑相机与物理相机

| 类型 | 描述 |
| --- | --- |
| **物理** | 单个相机传感器（广角、长焦、超广角） |
| **逻辑** | 组合多个物理相机的虚拟相机 |

### 识别相机类型

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// 检查是否为逻辑相机
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// 获取物理相机 ID（适用于逻辑相机）
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### 切换相机

要切换相机，你需要：
1. 关闭当前相机
2. 打开新相机
3. 创建新的拍摄会话

```kotlin
private fun switchCamera(newCameraId: String) {
    // 关闭当前相机
    captureSession?.close()
    cameraDevice?.close()
    
    // 打开新相机
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### 并发相机

某些设备支持同时打开多个相机：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## 相机扩展

相机扩展让你能够使用厂商特定的相机功能：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// 获取可用的扩展
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

常见扩展：
- `EXTENSION_BOKEH` — 人像模式
- `EXTENSION_HDR` — HDR 模式
- `EXTENSION_NIGHT` — 夜景模式
- `EXTENSION_AUTO` — 自动模式

## 多相机示例

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
                CameraCharacteristics.LENS_FACING_FRONT -> "前置摄像头 ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "后置摄像头 ($id)"
                else -> "摄像头 $id"
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
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "会话创建失败", Toast.LENGTH_SHORT).show()
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

## 最佳实践

1. **检查能力** — 使用专业功能前始终进行验证
2. **处理过渡** — 切换相机时平滑地关闭/打开
3. **管理资源** — 高速视频消耗更多资源
4. **优雅降级** — 当功能不可用时提供替代方案

## 下一章

在最后一章中，我们将探索 CameraCharacteristics 百科全书 —— 深入了解最重要的相机参数。

## 总结

专业功能扩展了你的创作可能性：

1. **高速视频** — 以 120-960fps 拍摄慢动作
2. **多相机** — 使用逻辑相机和物理相机
3. **相机扩展** — 使用厂商特定的功能
4. **并发相机** — 同时打开多个相机

在下一章中，我们将深入探讨 CameraCharacteristics —— 相机参数的百科全书。
