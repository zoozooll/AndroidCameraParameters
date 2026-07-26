---
sidebar_position: 13
title: "第 13 章：手动相机 - 对焦与白平衡"
description: 学习如何使用 Camera2 手动控制对焦距离和白平衡，实现专业摄影效果。
keywords: [对焦, 白平衡, 手动相机, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

通过对焦和白平衡实现完全手动控制。

## 简介

在上一章中，你学习了如何控制 ISO 和曝光。现在我们将添加：

1. **对焦** — 手动对焦距离控制
2. **白平衡** — 手动色温控制

有了这些，你就可以对照片进行完全的创意控制。

## 手动对焦

对焦决定了场景中哪个部分是清晰的。手动对焦让你能够：
- 对焦特定物体
- 创造有意的虚化效果（散景）
- 在微距摄影中确保关键对焦

### 对焦模式

Camera2 支持多种对焦模式：

| 模式 | 描述 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | 手动对焦 |
| `CONTROL_AF_MODE_AUTO` | 单次自动对焦 |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 照片连续自动对焦 |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | 视频连续自动对焦 |
| `CONTROL_AF_MODE_MACRO` | 微距对焦 |

### 对焦距离

对焦距离以屈光度（1/米）为单位测量。值为 0 表示无穷远。

```kotlin
// 获取对焦距离范围
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### 设置手动对焦

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 禁用 AF 以启用手动对焦
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// 设置手动对焦距离（屈光度）
// 0.0 = 无穷远
// 1.0 = 1 米
// 2.0 = 0.5 米
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### 对焦区域

你还可以指定 AF 区域进行选择性自动对焦：

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // 传感器坐标中的中心 X（0-1000）
        centerY,    // 传感器坐标中的中心 Y（0-1000）
        width,      // 区域宽度
        height,     // 区域高度
        weight      // 优先级（0-1000）
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## 手动白平衡

白平衡（WB）调整图像的色温。不同的光源具有不同的色温：

- **日光** — 约 5500K（偏蓝）
- **阴天** — 约 6500K（更冷）
- **钨丝灯** — 约 2800K（暖/黄色）
- **荧光灯** — 约 4000K（偏绿）

### 白平衡模式

| 模式 | 描述 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | 手动白平衡 |
| `CONTROL_AWB_MODE_AUTO` | 自动白平衡 |
| `CONTROL_AWB_MODE_INCANDESCENT` | 钨丝灯照明 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 荧光灯照明 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 暖荧光灯 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 日光 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 阴天 |

### 设置手动白平衡

要设置手动白平衡，你需要设置色彩校正增益：

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 禁用 AWB 以启用手动控制
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// 设置色彩校正增益（R, G, B）
// 值已归一化（1.0 = 无校正）
// 值越高，该颜色越突出
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### 色温

你也可以使用色温来设置白平衡：

```kotlin
// 获取支持的色温范围
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// 设置色温（开尔文）
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // 日光
```

## 完整的手动相机示例

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // 手动控制
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60秒
    private var currentFocus = 0.0f // 无穷远
    private var currentWhiteBalance = 5500 // 日光

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // ISO 控制
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 曝光控制
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 对焦控制
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 将进度（0-100）转换为对焦距离（0.0 到 2.0 屈光度）
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 白平衡控制
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K（钨丝灯）到 6500K（阴天）
                currentWhiteBalance = 2800 + (progress * 37)
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
            
            // 手动曝光
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // 手动对焦
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // 手动白平衡
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ...（相机设置代码）
}
```

## 最佳实践

1. **检查支持** — 并非所有相机都支持手动对焦或 WB
2. **从自动开始** — 让自动控制建立基准
3. **使用峰值对焦** — 添加视觉反馈以提高对焦精度
4. **校准 WB** — 使用灰卡获得准确的白平衡
5. **组合控制** — 手动设置配合使用效果最佳

## 下一章

在下一章中，我们将探索高速视频和多摄像头等专业功能。

## 总结

手动控制对焦和白平衡完善了你的相机工具包：

1. **对焦** — 控制图像中清晰的部分
2. **白平衡** — 控制色温
3. **手动模式** — 禁用 AF/AWB 并直接设置值
4. **对焦区域** — 针对特定区域进行自动对焦

通过控制 ISO、曝光、对焦和白平衡，你可以创作出专业品质的照片。在第五部分中，我们将探索高级功能，如高速视频和多摄像头支持。
