---
sidebar_position: 11
title: "第11章: RAWキャプチャとCaptureRequest"
description: RAW画像のキャプチャ方法を学び、高度なカメラ制御のためのCaptureRequestとCaptureResultについて理解します。
keywords: [RAWキャプチャ, CaptureRequest, CaptureResult, Camera2, マニュアルコントロール]
---

RAWキャプチャにより、画像処理を完全に制御できます。CaptureRequestとCaptureResultと共に探索していきましょう。

## はじめに

前の章では、JPEG写真のキャプチャ方法を学びました。今回は以下の内容を探ります：

1. **RAWキャプチャ** — 未処理のセンサーデータをキャプチャする
2. **CaptureRequest** — 各キャプチャのカメラ設定を構成する
3. **CaptureResult** — 完了したキャプチャに関するメタデータを取得する

## RAWとは？

RAW画像には、ISP処理前にセンサーがキャプチャしたすべてのデータが含まれています。これは以下を意味します：

- ノイズリダクションが適用されていない
- ホワイトバランス補正がされていない
- シャープネス処理がされていない
- フルダイナミックレンジ

RAWファイルはサイズが大きくなりますが、比類のない編集の柔軟性を提供します。

## RAWキャプチャの要件

RAW画像をキャプチャするには、使用するカメラが以下の条件を満たしている必要があります：
1. **FULL**または**LEVEL_3**のハードウェアレベルを持っている
2. `RAW`機能をサポートしている

CameraCharacteristicsを確認します：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## RAW画像のキャプチャ

RAWフォーマットでImageReaderを作成します：

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // または ImageFormat.RAW10/RAW12
    2
)
```

次に、同時キャプチャのためにJPEGとRAWの両方のサーフェスをキャプチャセッションに追加します：

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequestは、単一のキャプチャに対するすべての設定を定義します。以下を構成できます：

### オートコントロール
- `CONTROL_AF_MODE` — オートフォーカスモード
- `CONTROL_AE_MODE` — 自動露出モード
- `CONTROL_AWB_MODE` — オートホワイトバランスモード

### マニュアルコントロール
- `SENSOR_SENSITIVITY` — ISO値
- `SENSOR_EXPOSURE_TIME` — 露出時間（ナノ秒）
- `LENS_FOCUS_DISTANCE` — フォーカス距離
- `LENS_APERTURE` — 絞り（利用可能な場合）

### 出力設定
- `JPEG_QUALITY` — JPEG圧縮品質
- `JPEG_ORIENTATION` — 画像の向き
- `COLOR_CORRECTION_MODE` — 色補正モード

### CaptureRequestの作成

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// オートコントロールを設定
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// JPEG品質を設定
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// ターゲットを追加
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// リクエストをビルド
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResultには、完了したキャプチャに関するメタデータが含まれています。以下が含まれます：

- **実際に使用された設定** — カメラが実際に適用した内容
- **統計情報** — 露出、フォーカス、色情報
- **タイムスタンプ** — キャプチャが発生した時刻

### CaptureResultの取得

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // 実際の露出時間を取得
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // 実際のISOを取得
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // フォーカス状態を取得
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // AE状態を取得
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "露出: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### 一般的なCaptureResultキー

| キー | 説明 |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | 実際に使用された露出時間 |
| `SENSOR_SENSITIVITY` | 実際に使用されたISO |
| `CONTROL_AF_STATE` | オートフォーカス状態 |
| `CONTROL_AE_STATE` | 自動露出状態 |
| `CONTROL_AWB_STATE` | オートホワイトバランス状態 |
| `SCALER_CROP_REGION` | 使用されたクロップ領域 |
| `COLOR_CORRECTION_GAINS` | 色補正ゲイン |

## 完全なRAWキャプチャの例

```kotlin
private fun configureDualCapture() {
    // JPEG ImageReaderを作成
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // RAW ImageReaderを作成
    val rawReader = ImageReader.newInstance(
        rawSize.width, rawSize.height,
        ImageFormat.RAW_SENSOR, 2
    )
    
    jpegReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveJpeg(image)
        image.close()
    }, null)
    
    rawReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveRaw(image)
        image.close()
    }, null)
    
    // サーフェスを作成
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // すべてのサーフェスでキャプチャセッションを作成
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // 両方のサーフェスをターゲットとして追加
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // コントロールを構成
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.JPEG_QUALITY, 100)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.capture(builder.build(), captureCallback, null)
    }
}

private fun saveRaw(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val fileName = "RAW_${System.currentTimeMillis()}.dng"
    val file = File(externalCacheDir, fileName)
    
    FileOutputStream(file).use { fos ->
        fos.write(bytes)
    }
}
```

## RAW vs JPEG

| 機能 | RAW | JPEG |
| --- | --- | --- |
| ファイルサイズ | 大きい (20-50MB) | 小さい (2-10MB) |
| 編集の柔軟性 | 最大 | 限定的 |
| ノイズ | 保持される | 低減される |
| ホワイトバランス | 調整可能 | 固定 |
| ダイナミックレンジ | フル | 圧縮される |

## ベストプラクティス

1. **RAWサポートを確認する** — RAWキャプチャを試みる前に常に確認してください
2. **デュアルキャプチャ** — 柔軟性のためにJPEGとRAWの両方をキャプチャします
3. **画像をクローズする** — 処理後は常に`image.close()`を呼び出してください
4. **異なるフォーマットを処理する** — RAW_SENSOR、RAW10、RAW12はバイトレイアウトが異なります

## 次の章

次の章では、パートIIIで学んだことをまとめ、パートIV：マニュアルカメラコントロールの準備をします。

## まとめ

この章では、以下の内容について学びました：

1. **RAWキャプチャ** — 最大限の編集の柔軟性のために未処理のセンサーデータをキャプチャする
2. **CaptureRequest** — 各キャプチャのカメラ設定を構成する
3. **CaptureResult** — 完了したキャプチャに関するメタデータを取得する

RAWキャプチャには、FULLまたはLEVEL_3のハードウェアレベルが必要です。両方のサーフェスをキャプチャセッションに追加することで、JPEGとRAWを同時にキャプチャできます。

CaptureRequestを使用すると、オートフォーカス、自動露出、ホワイトバランス、およびISOや露出時間などのマニュアルコントロールを構成できます。CaptureResultは、カメラが実際に使用した設定を通知します。

パートIVでは、ISO、露出、フォーカス、ホワイトバランスといったマニュアルカメラコントロールを深く掘り下げていきます。
