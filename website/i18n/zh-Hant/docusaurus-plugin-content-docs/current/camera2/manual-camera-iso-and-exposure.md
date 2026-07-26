---
sidebar_position: 12
title: "第十二章：手動相機 - ISO 與曝光"
description: "學習如何使用 Camera2 手動控制 ISO 和曝光時間，實現專業攝影效果。"
keywords: [ISO, 曝光時間, 手動相機, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

手動控制是 Camera2 真正強大之處。讓我們來學習 ISO 和曝光。

## 簡介

到目前為止，我們一直在使用自動控制。現在我們將完全掌控：

1. **ISO** — 感光元件對光線的敏感度
2. **曝光時間** — 感光元件收集光線的時間長度

這兩項設定直接影響影像的亮度和品質。

## 什麼是 ISO？

ISO 衡量感光元件對光線的敏感度。較低的 ISO 意味著：
- 對光線較不敏感
- 雜訊較少
- 影像品質較佳

較高的 ISO 意味著：
- 對光線較敏感
- 雜訊較多（顆粒感）
- 影像品質較差

常見的 ISO 值：100, 200, 400, 800, 1600, 3200, 6400

## 什麼是曝光時間？

曝光時間（也稱為快門速度）是感光元件收集光線的時間長度。較短的曝光意味著：
- 捕捉的光線較少
- 凍結動作
- 更快的動作拍攝

較長的曝光意味著：
- 捕捉的光線較多
- 動態模糊
- 低光拍攝效果更佳

曝光時間以秒或秒的分數計算：
- 1/1000s — 快速動作
- 1/125s — 一般
- 1/30s — 慢速
- 1s — 長曝光

## 手動控制需求

要使用手動控制，你的相機必須具備：
1. **FULL** 或 **LEVEL_3** 硬體等級
2. **MANUAL_SENSOR** 功能

檢查 CameraCharacteristics：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## 取得支援的範圍

設定手動值之前，請先檢查相機支援的範圍：

```kotlin
// 取得 ISO 範圍
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// 取得曝光時間範圍
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## 設定手動 ISO 與曝光

要使用手動控制，你需要：
1. 停用自動曝光（AE）
2. 設定手動 ISO
3. 設定手動曝光時間

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 停用 AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// 設定手動 ISO
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// 設定手動曝光時間（以奈秒為單位）
// 1/125s = 8,000,000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// 加入目標
captureRequestBuilder.addTarget(surface)

// 使用手動控制開始預覽
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## 曝光三角

ISO、曝光時間和光圈構成了「曝光三角」：

- **ISO** — 對光線的敏感度
- **曝光時間** — 光線捕捉的持續時間
- **光圈** — 進入的光線量（在手機上很少可調整）

改變其中一項會影響其他項。例如：
- 如果你提高 ISO，就可以使用更快的快門速度
- 如果你減少曝光時間，可能需要提高 ISO

## 完整的手動控制範例

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s

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
        
        isoSeekBar.max = 31 // ISO：100-3200，每步 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // 曝光時間：1/1000s 到 1/10s
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
                // 將進度轉換為曝光時間（以奈秒為單位）
                // 0: 1/1000s = 1,000,000ns
                // 9: 1/10s = 100,000,000ns
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
            
            // 停用 AE 以進行手動控制
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // 設定手動 ISO
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // 設定手動曝光時間
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // 保持 AWB 開啟
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ...（其餘相機設定程式碼）
    
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
    
    // ...（相機開啟、工作階段建立等）
}
```

## 版面配置

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
                android:text="1/60s"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## 最佳實務

1. **檢查支援** — 務必驗證手動感光元件支援
2. **從 AE 開始** — 讓自動曝光設定初始值，然後切換到手動
3. **避免極端 ISO** — 高 ISO 會引入雜訊
4. **使用最短曝光** — 儘可能避免動態模糊
5. **監控直方圖** — 使用 CaptureResult 檢查曝光

## 下一章

在下一章中，我們將學習手動對焦和白平衡控制。

## 總結

手動控制 ISO 和曝光為你帶來專業等級的攝影能力：

1. **ISO** — 控制感光元件敏感度（範圍 100-3200+）
2. **曝光時間** — 控制光線收集的時間長度（以奈秒為單位）
3. **停用 AE** — 必須關閉自動曝光才能進行手動控制
4. **檢查範圍** — 務必驗證支援的 ISO 和曝光時間範圍

曝光三角（ISO、曝光時間、光圈）決定了影像的亮度和品質。在下一章中，我們將探索手動對焦和白平衡。
