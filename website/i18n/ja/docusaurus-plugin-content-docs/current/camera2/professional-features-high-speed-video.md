---
sidebar_position: 14
title: "第14章：プロフェッショナル機能 - ハイスピードビデオ"
description: Camera2でハイスピードビデオを撮影し、マルチカメラ設定を操作する方法を学びます。
keywords: [ハイスピードビデオ, マルチカメラ, Camera2, コンストレインドハイスピード, ロジカルカメラ]
---

プロフェッショナル機能は、新しい創造的な可能性を開きます。ハイスピードビデオとマルチカメラについて探ってみましょう。

## はじめに

Camera2は、基本的な写真撮影を超える多くのプロフェッショナル機能をサポートしています。この章では、以下の内容について学びます：

1. **ハイスピードビデオ** — スローモーションビデオの撮影
2. **マルチカメラ** — ロジカルカメラと物理カメラの操作

## ハイスピードビデオ

ハイスピードビデオでは、標準の30fpsを超えるフレームレートでビデオを撮影できます：
- 120fps — スムーズなスローモーション
- 240fps — 標準的なスローモーション
- 480fps — 極限のスローモーション
- 960fps — スーパースローモーション

### 要件

ハイスピードビデオを撮影するには、使用するカメラが以下の条件を満たしている必要があります：
1. `CONSTRAINED_HIGH_SPEED_VIDEO` 機能をサポートしていること
2. 適切なハードウェアレベルを持っていること

CameraCharacteristicsを確認します：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### ハイスピードビデオのサイズ

ハイスピードビデオでは、標準ビデオとは異なる解像度を使用します：

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### ハイスピードビデオの撮影

ハイスピードビデオには、特殊なキャプチャセッションが必要です：

```kotlin
// ハイスピードキャプチャセッションを作成する
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "ハイスピードセッションの作成に失敗しました", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### ハイスピードプレビューの開始

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // サポートされているハイスピードfps範囲を取得する
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // ハイスピード範囲を見つける（例：120fps）
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

## マルチカメラ

最新のスマートフォンには複数のカメラが搭載されています。Camera2では、それらを以下のように扱います：
- **物理カメラ** — 個々のカメラセンサー
- **ロジカルカメラ** — 複数の物理カメラの組み合わせ

### ロジカルカメラと物理カメラ

| 種類 | 説明 |
| --- | --- |
| **物理** | 単一のカメラセンサー（広角、望遠、超広角） |
| **ロジカル** | 複数の物理カメラを組み合わせた仮想カメラ |

### カメラタイプの識別

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// ロジカルカメラかどうかを確認する
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// 物理カメラIDを取得する（ロジカルカメラの場合）
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### カメラの切り替え

カメラを切り替えるには、以下の手順が必要です：
1. 現在のカメラを閉じる
2. 新しいカメラを開く
3. 新しいキャプチャセッションを作成する

```kotlin
private fun switchCamera(newCameraId: String) {
    // 現在のカメラを閉じる
    captureSession?.close()
    cameraDevice?.close()
    
    // 新しいカメラを開く
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### コンカレントカメラ

デバイスによっては、複数のカメラを同時に開くことをサポートしています：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## カメラエクステンション

カメラエクステンションを使用すると、メーカー固有のカメラ機能を利用できます：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// 利用可能なエクステンションを取得する
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

一般的なエクステンション：
- `EXTENSION_BOKEH` — ポートレートモード
- `EXTENSION_HDR` — HDRモード
- `EXTENSION_NIGHT` — ナイトモード
- `EXTENSION_AUTO` — オートモード

## マルチカメラの例

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
                CameraCharacteristics.LENS_FACING_FRONT -> "フロントカメラ ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "バックカメラ ($id)"
                else -> "カメラ $id"
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
            Toast.makeText(this, "カメラ権限が拒否されました", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@MultiCameraActivity, "セッションの作成に失敗しました", Toast.LENGTH_SHORT).show()
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

## ベストプラクティス

1. **機能を確認する** — プロフェッショナル機能を使用する前に、常にサポート状況を確認してください
2. **切り替えを処理する** — カメラを切り替えるときは、スムーズに閉じて開いてください
3. **リソースを管理する** — ハイスピードビデオはより多くのリソースを消費します
4. **適切にフォールバックする** — 機能が利用できない場合は代替手段を提供してください

## 次の章

最終章では、CameraCharacteristics百科事典 — 最も重要なカメラパラメータの詳細な解説 — について探ります。

## まとめ

プロフェッショナル機能は、創造的な可能性を広げます：

1. **ハイスピードビデオ** — 120〜960fpsでスローモーションを撮影
2. **マルチカメラ** — ロジカルカメラと物理カメラの操作
3. **カメラエクステンション** — メーカー固有の機能を使用
4. **コンカレントカメラ** — 複数のカメラを同時に開く

次の章では、カメラパラメータの百科事典であるCameraCharacteristicsについて詳しく掘り下げます。
