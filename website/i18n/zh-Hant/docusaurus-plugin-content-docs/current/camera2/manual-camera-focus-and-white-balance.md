---
sidebar_position: 13
title: "第十三章：手動相機 - 對焦與白平衡"
description: 學習如何使用 Camera2 手動控制對焦距離和白平衡，實現專業攝影效果。
keywords: [對焦, 白平衡, 手動相機, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

完整的手動控制，包含對焦與白平衡。

## 簡介

在上一章中，你學習了如何控制 ISO 和曝光。現在我們將加入：

1. **對焦** — 手動對焦距離控制
2. **白平衡** — 手動色溫控制

有了這些，你就可以完全創意地掌控你的照片。

## 手動對焦

對焦決定場景中哪個部分是清晰的。手動對焦讓你能夠：
- 對焦在特定物體上
- 創造有意的模糊（散景）
- 在微距攝影中確保關鍵對焦

### 對焦模式

Camera2 支援多種對焦模式：

| 模式 | 描述 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | 手動對焦 |
| `CONTROL_AF_MODE_AUTO` | 單次自動對焦 |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 照片連續自動對焦 |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | 影片連續自動對焦 |
| `CONTROL_AF_MODE_MACRO` | 微距對焦 |

### 對焦距離

對焦距離以屈光度（1/公尺）為單位測量。值為 0 表示無限遠。

```kotlin
// 取得對焦距離範圍
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### 設定手動對焦

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 停用 AF 以進行手動對焦
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// 設定手動對焦距離（屈光度）
// 0.0 = 無限遠
// 1.0 = 1 公尺
// 2.0 = 0.5 公尺
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### 對焦區域

你也可以指定 AF 區域來進行選擇性自動對焦：

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // 感應器座標中的中心 X（0-1000）
        centerY,    // 感應器座標中的中心 Y（0-1000）
        width,      // 區域寬度
        height,     // 區域高度
        weight      // 優先權（0-1000）
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## 手動白平衡

白平衡（WB）調整影像的色溫。不同的光源有不同的色溫：

- **日光** — 約 5500K（偏藍）
- **陰天** — 約 6500K（較冷）
- **鎢絲燈** — 約 2800K（暖/黃）
- **螢光燈** — 約 4000K（偏綠）

### 白平衡模式

| 模式 | 描述 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | 手動白平衡 |
| `CONTROL_AWB_MODE_AUTO` | 自動白平衡 |
| `CONTROL_AWB_MODE_INCANDESCENT` | 白熾燈光 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 螢光燈光 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 暖螢光 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 日光 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 陰天 |

### 設定手動白平衡

要設定手動白平衡，你需要設定色彩校正增益：

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 停用 AWB 以進行手動控制
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// 設定色彩校正增益（R, G, B）
// 值為正規化（1.0 = 無校正）
// 較高的值使該顏色更突出
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### 色溫

你也可以使用色溫來設定白平衡：

```kotlin
// 取得支援的色溫範圍
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// 設定色溫（凱氏溫度）
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // 日光
```

## 完整的手動相機範例

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // 手動控制
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // 無限遠
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
        
        // 對焦控制
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 將進度（0-100）轉換為對焦距離（0.0 到 2.0 屈光度）
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 白平衡控制
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K（鎢絲燈）到 6500K（陰天）
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
            
            // 手動曝光
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // 手動對焦
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // 手動白平衡
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ...（相機設定程式碼）
}
```

## 最佳實務

1. **檢查支援** — 並非所有相機都支援手動對焦或 WB
2. **從自動開始** — 讓自動控制建立基準
3. **使用對焦峰值** — 加入視覺回饋以提高對焦準確度
4. **校準 WB** — 使用灰卡獲得準確的白平衡
5. **結合控制** — 手動設定一起使用效果最佳

## 下一章

在下一章中，我們將探索專業功能，如高速錄影和多相機。

## 總結

手動控制對焦和白平衡完善了你的相機工具包：

1. **對焦** — 控制影像中什麼是清晰的
2. **白平衡** — 控制色溫
3. **手動模式** — 停用 AF/AWB 並直接設定值
4. **對焦區域** — 針對特定區域進行自動對焦

透過控制 ISO、曝光、對焦和白平衡，你可以創造專業品質的照片。在第五部分中，我們將探索進階功能，如高速錄影和多相機支援。
