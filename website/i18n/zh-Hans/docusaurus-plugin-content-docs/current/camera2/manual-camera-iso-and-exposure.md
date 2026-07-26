---
sidebar_position: 12
title: "第12章：手动相机 - ISO 和曝光"
description: 学习如何使用 Camera2 手动控制 ISO 和曝光时间，实现专业摄影效果。
keywords: [ISO, 曝光时间, 手动相机, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

手动控制是 Camera2 真正威力的体现。让我们来学习 ISO 和曝光相关知识。

## 简介

到目前为止，我们一直在使用自动控制。现在我们将完全控制以下参数：

1. **ISO** — 传感器对光线的敏感度
2. **曝光时间** — 传感器收集光线的时长

这两个设置直接影响图像的亮度和质量。

## 什么是 ISO？

ISO 衡量传感器对光线的敏感程度。较低的 ISO 意味着：
- 对光线较不敏感
- 噪点更少
- 图像质量更好

较高的 ISO 意味着：
- 对光线更敏感
- 噪点更多（颗粒感）
- 图像质量较差

常见 ISO 值：100、200、400、800、1600、3200、6400

## 什么是曝光时间？

曝光时间（也称为快门速度）是传感器收集光线的时长。较短的曝光意味着：
- 捕捉的光线更少
- 可以凝固运动
- 动作更清晰

较长的曝光意味着：
- 捕捉的光线更多
- 会产生运动模糊
- 低光性能更好

曝光时间以秒或秒的分数来衡量：
- 1/1000秒 — 快速动作
- 1/125秒 — 正常
- 1/30秒 — 慢速
- 1秒 — 长曝光

## 手动控制要求

要使用手动控制，你的相机必须具备：
1. **FULL** 或 **LEVEL_3** 硬件级别
2. **MANUAL_SENSOR** 能力

检查 CameraCharacteristics：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## 获取支持的范围

在设置手动值之前，请先检查相机支持的范围：

```kotlin
// 获取 ISO 范围
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// 获取曝光范围
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## 设置手动 ISO 和曝光

要使用手动控制，你需要：
1. 禁用自动曝光（AE）
2. 设置手动 ISO
3. 设置手动曝光时间

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 禁用 AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// 设置手动 ISO
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// 设置手动曝光时间（单位：纳秒）
// 1/125秒 = 8,000,000纳秒
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// 添加目标
captureRequestBuilder.addTarget(surface)

// 使用手动控制启动预览
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## 曝光三角

ISO、曝光时间和光圈构成了"曝光三角"：

- **ISO** — 对光线的敏感度
- **曝光时间** — 光线捕捉的持续时间
- **光圈** — 进入的光线量（在手机上很少可调节）

改变其中一个会影响其他参数。例如：
- 如果增加 ISO，你可以使用更快的快门速度
- 如果减少曝光时间，你可能需要增加 ISO

## 完整的手动控制示例

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60秒

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO: 100-3200，步长为100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // 曝光: 1/1000秒 到 1/10秒
        exposureSeekBar.progress = 5
        
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 将进度转换为曝光时间（单位：纳秒）
                // 0: 1/1000秒 = 1,000,000纳秒
                // 9: 1/10秒 = 100,000,000纳秒
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // 禁用 AE 以进行手动控制
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // 设置手动 ISO
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // 设置手动曝光时间
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // 保持 AWB 开启
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ...（其余相机设置代码）
    
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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
    
    // ...（相机打开、会话创建等）
}
```

## 布局文件

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="ISO:"/>
            
            <SeekBar
                android:id="@+id/isoSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/isoValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="100"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="16dp">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="曝光:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60秒"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## 最佳实践

1. **检查支持** — 始终验证手动传感器支持
2. **从 AE 开始** — 让自动曝光设置初始值，然后切换到手动
3. **避免极端 ISO** — 高 ISO 会引入噪点
4. **使用最短曝光** — 尽可能避免运动模糊
5. **监控直方图** — 使用 CaptureResult 检查曝光情况

## 下一章

在下一章中，我们将学习手动对焦和白平衡控制。

## 总结

手动控制 ISO 和曝光为你提供专业级的摄影能力：

1. **ISO** — 控制传感器敏感度（100-3200+ 范围）
2. **曝光时间** — 控制光线收集的时长（单位：纳秒）
3. **禁用 AE** — 必须关闭自动曝光才能进行手动控制
4. **检查范围** — 始终验证支持的 ISO 和曝光范围

曝光三角（ISO、曝光时间、光圈）决定了图像的亮度和质量。在下一章中，我们将探索手动对焦和白平衡。
