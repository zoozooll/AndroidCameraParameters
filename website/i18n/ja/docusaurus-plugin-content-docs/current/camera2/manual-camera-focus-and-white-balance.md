---
sidebar_position: 13
title: "第13章：マニュアルカメラ - フォーカスとホワイトバランス"
description: Camera2を使用したプロフェッショナル写真撮影のために、フォーカス距離とホワイトバランスを手動で制御する方法を学びます。
keywords: [フォーカス, ホワイトバランス, マニュアルカメラ, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

フォーカスとホワイトバランスによる完全な手動制御。

## はじめに

前の章では、ISOと露出を制御する方法を学びました。今回は以下を追加します：

1. **フォーカス** — 手動フォーカス距離制御
2. **ホワイトバランス** — 手動色温度制御

これらにより、写真に対する完全なクリエイティブ制御が可能になります。

## マニュアルフォーカス

フォーカスは、シーンのどの部分が鮮明になるかを決定します。マニュアルフォーカスにより、以下のことが可能になります：
- 特定のオブジェクトにフォーカスを合わせる
- 意図的なボケ（ボケ味）を作成する
- マクロ写真でクリティカルなフォーカスを確保する

### フォーカスモード

Camera2は、いくつかのフォーカスモードをサポートしています：

| モード | 説明 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | マニュアルフォーカス |
| `CONTROL_AF_MODE_AUTO` | シングルオートフォーカス |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 写真用コンティニュアスオートフォーカス |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | 動画用コンティニュアスオートフォーカス |
| `CONTROL_AF_MODE_MACRO` | マクロフォーカス |

### フォーカス距離

フォーカス距離はジオプター（1/メートル）で測定されます。値0は無限大を意味します。

```kotlin
// フォーカス距離範囲を取得
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### マニュアルフォーカスの設定

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// マニュアルフォーカスのためAFを無効化
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// マニュアルフォーカス距離を設定（ジオプター単位）
// 0.0 = 無限大
// 1.0 = 1メートル
// 2.0 = 0.5メートル
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### フォーカス領域

選択的オートフォーカスのために、AF領域を指定することもできます：

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // センサー座標系での中心X（0-1000）
        centerY,    // センサー座標系での中心Y（0-1000）
        width,      // 領域の幅
        height,     // 領域の高さ
        weight      // 優先度（0-1000）
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## マニュアルホワイトバランス

ホワイトバランス（WB）は、画像の色温度を調整します。光源によって色温度は異なります：

- **昼光** — 約5500K（青みがかっている）
- **曇り** — 約6500K（よりクール）
- **タングステン** — 約2800K（暖かい/黄色）
- **蛍光** — 約4000K（緑がかっている）

### ホワイトバランスモード

| モード | 説明 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | マニュアルホワイトバランス |
| `CONTROL_AWB_MODE_AUTO` | オートホワイトバランス |
| `CONTROL_AWB_MODE_INCANDESCENT` | タングステン照明 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 蛍光照明 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 暖色系蛍光 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 昼光 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 曇り |

### マニュアルホワイトバランスの設定

マニュアルホワイトバランスを設定するには、色補正ゲインを設定する必要があります：

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 手動制御のためAWBを無効化
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// 色補正ゲインを設定（R, G, B）
// 値は正規化されています（1.0 = 補正なし）
// 値を高くすると、その色がより強くなります
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### 色温度

色温度を使用してホワイトバランスを設定することもできます：

```kotlin
// サポートされている色温度範囲を取得
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// 色温度を設定（ケルビン単位）
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // 昼光
```

## 完全なマニュアルカメラの例

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // マニュアルコントロール
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60秒
    private var currentFocus = 0.0f // 無限大
    private var currentWhiteBalance = 5500 // 昼光

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // ISOコントロール
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 露出コントロール
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // フォーカスコントロール
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // プログレス（0-100）をフォーカス距離（0.0〜2.0ジオプター）に変換
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // ホワイトバランスコントロール
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K（タングステン）〜6500K（曇り）
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
            
            // マニュアル露出
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // マニュアルフォーカス
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // マニュアルホワイトバランス
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ...（カメラ設定コード）
}
```

## ベストプラクティス

1. **サポートを確認する** — すべてのカメラがマニュアルフォーカスやWBをサポートしているわけではありません
2. **自動から始める** — オートコントロールでベースラインを確立します
3. **フォーカスピーキングを使用する** — フォーカス精度の視覚的フィードバックを追加します
4. **WBをキャリブレーションする** — 正確なホワイトバランスのためにグレーカードを使用します
5. **コントロールを組み合わせる** — マニュアル設定は一緒に使用すると最も効果的です

## 次の章

次の章では、高速動画やマルチカメラなどのプロフェッショナル機能について説明します。

## まとめ

フォーカスとホワイトバランスの手動制御により、カメラツールキットが完成します：

1. **フォーカス** — 画像内で鮮明にする部分を制御
2. **ホワイトバランス** — 色温度を制御
3. **マニュアルモード** — AF/AWBを無効にして値を直接設定
4. **フォーカス領域** — オートフォーカスの対象領域を指定

ISO、露出、フォーカス、ホワイトバランスを制御下に置くことで、プロ品質の写真を作成できます。第V部では、高速動画やマルチカメラサポートなどの高度な機能について説明します。
