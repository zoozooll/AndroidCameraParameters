---
sidebar_position: 12
title: "第12章: マニュアルカメラ - ISOと露出"
description: Camera2を使用したプロフェッショナル写真撮影のために、ISOと露出時間を手動で制御する方法を学びます。
keywords: [ISO, 露出時間, マニュアルカメラ, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

マニュアルコントロールは、Camera2の真の力が発揮される部分です。ISOと露出について学びましょう。

## はじめに

これまでは自動制御を使用してきました。今回は、以下の項目を完全に制御します：

1. **ISO** — 光に対するセンサーの感度
2. **露出時間** — センサーが光を集める時間

これら2つの設定は、画像の明るさと品質に直接影響を与えます。

## ISOとは？

ISOは、センサーの光に対する感度を測定します。ISOが低いほど：
- 光に対する感度が低い
- ノイズが少ない
- 画像品質が良い

ISOが高いほど：
- 光に対する感度が高い
- ノイズ（粒状感）が多い
- 画像品質が低い

一般的なISO値：100, 200, 400, 800, 1600, 3200, 6400

## 露出時間とは？

露出時間（シャッタースピードとも呼ばれます）は、センサーが光を集める時間です。露出が短いほど：
- 取り込まれる光が少ない
- 動きを静止させる
- 速い動作に対応

露出が長いほど：
- 取り込まれる光が多い
- 動きのブレが生じる
- 低光量での性能が向上

露出時間は秒または秒の分数で測定されます：
- 1/1000秒 — 速い動作
- 1/125秒 — 通常
- 1/30秒 — 低速
- 1秒 — 長時間露出

## マニュアルコントロールの要件

マニュアルコントロールを使用するには、カメラが以下を備えている必要があります：
1. **FULL** または **LEVEL_3** のハードウェアレベル
2. **MANUAL_SENSOR** 機能

CameraCharacteristicsを確認します：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## サポートされている範囲の取得

マニュアル値を設定する前に、カメラがサポートしている範囲を確認します：

```kotlin
// ISO範囲を取得
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// 露出範囲を取得
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## マニュアルISOと露出の設定

マニュアルコントロールを使用するには、以下の手順が必要です：
1. 自動露出（AE）を無効にする
2. マニュアルISOを設定する
3. マニュアル露出時間を設定する

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// AEを無効化
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// マニュアルISOを設定
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// マニュアル露出時間を設定（ナノ秒単位）
// 1/125秒 = 8,000,000ナノ秒
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// ターゲットを追加
captureRequestBuilder.addTarget(surface)

// マニュアルコントロールでプレビューを開始
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## 露出の三角関係

ISO、露出時間、絞りは「露出の三角関係」を形成します：

- **ISO** — 光に対する感度
- **露出時間** — 光を取り込む時間
- **絞り** — 入る光の量（スマートフォンでは調整できないことがほとんど）

1つを変更すると、他の2つに影響します。例えば：
- ISOを上げると、より速いシャッタースピードを使用できます
- 露出時間を短くすると、ISOを上げる必要があるかもしれません

## 完全なマニュアルコントロールの例

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
        
        isoSeekBar.max = 31 // ISO: 100〜3200、100刻み
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // 露出: 1/1000秒 〜 1/10秒
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
                // プログレスを露出時間に変換（ナノ秒単位）
                // 0: 1/1000秒 = 1,000,000ナノ秒
                // 9: 1/10秒 = 100,000,000ナノ秒
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
            
            // マニュアルコントロールのためAEを無効化
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // マニュアルISOを設定
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // マニュアル露出時間を設定
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // AWBはオンのまま
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ...（カメラセットアップコードの残り）
    
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
    
    // ...（カメラオープン、セッション作成など）
}
```

## レイアウト

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
                android:text="露出:"/>
            
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

## ベストプラクティス

1. **サポートを確認する** — 常にマニュアルセンサーのサポートを確認してください
2. **AEから始める** — 自動露出で初期値を設定してからマニュアルに切り替えましょう
3. **極端なISOを避ける** — 高ISOはノイズを引き起こします
4. **最短の露出を使用する** — 可能な場合は動きのブレを避けましょう
5. **ヒストグラムを監視する** — CaptureResultを使用して露出を確認しましょう

## 次の章

次の章では、マニュアルフォーカスとホワイトバランス制御について学びます。

## まとめ

ISOと露出のマニュアルコントロールにより、プロレベルの写真撮影が可能になります：

1. **ISO** — センサー感度を制御（100〜3200以上の範囲）
2. **露出時間** — 光を集める時間を制御（ナノ秒単位）
3. **AEを無効化** — マニュアルコントロールのためには自動露出をオフにする必要があります
4. **範囲を確認** — 常にサポートされているISOと露出の範囲を確認しましょう

露出の三角関係（ISO、露出時間、絞り）が画像の明るさと品質を決定します。次の章では、マニュアルフォーカスとホワイトバランスについて詳しく見ていきます。
