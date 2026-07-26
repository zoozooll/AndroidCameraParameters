---
sidebar_position: 11
title: "第 11 章：RAW 捕獲與 CaptureRequest"
description: 學習如何捕獲 RAW 影像，以及了解 CaptureRequest 和 CaptureResult 以進階控制相機。
keywords: [RAW 捕獲, CaptureRequest, CaptureResult, Camera2, 手動控制]
---

RAW 捕獲讓你可以完全控制影像處理。讓我們一起探索它以及 CaptureRequest 和 CaptureResult。

## 介紹

在上一章中，你學習了如何拍攝 JPEG 照片。現在我們將探索：

1. **RAW 捕獲** — 捕獲未經處理的感測器數據
2. **CaptureRequest** — 為每次捕獲設定相機參數
3. **CaptureResult** — 取得已完成捕獲的元數據

## 什麼是 RAW？

RAW 影像包含感測器在 ISP 處理之前捕獲的所有數據。這意味著：

- 未應用降噪
- 未進行白平衡校正
- 未進行銳化
- 完整動態範圍

RAW 檔案較大，但提供了無與倫比的編輯彈性。

## RAW 捕獲需求

若要捕獲 RAW 影像，你的相機必須：
1. 具備 **FULL** 或 **LEVEL_3** 硬體等級
2. 支援 `RAW` 功能

檢查 CameraCharacteristics：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## 捕獲 RAW 影像

建立一個具有 RAW 格式的 ImageReader：

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // 或 ImageFormat.RAW10/RAW12
    2
)
```

然後將 JPEG 和 RAW 表面都加入捕獲工作階段，以進行同時捕獲：

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest 定義單次捕獲的所有設定。你可以設定：

### 自動控制
- `CONTROL_AF_MODE` — 自動對焦模式
- `CONTROL_AE_MODE` — 自動曝光模式
- `CONTROL_AWB_MODE` — 自動白平衡模式

### 手動控制
- `SENSOR_SENSITIVITY` — ISO 值
- `SENSOR_EXPOSURE_TIME` — 曝光時間（奈秒）
- `LENS_FOCUS_DISTANCE` — 對焦距離
- `LENS_APERTURE` — 光圈（如果可用）

### 輸出設定
- `JPEG_QUALITY` — JPEG 壓縮品質
- `JPEG_ORIENTATION` — 影像方向
- `COLOR_CORRECTION_MODE` — 色彩校正模式

### 建立 CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// 設定自動控制
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// 設定 JPEG 品質
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// 加入目標
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// 建立請求
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult 包含已完成捕獲的元數據。它包括：

- **實際使用的設定** — 相機實際套用的設定
- **統計數據** — 曝光、對焦和色彩資訊
- **時間戳記** — 捕獲發生的時間

### 取得 CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // 取得實際曝光時間
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // 取得實際 ISO
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // 取得對焦狀態
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // 取得 AE 狀態
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "曝光：${exposureTime ?: 0}ns, ISO：$iso")
    }
}
```

### 常見的 CaptureResult 鍵值

| 鍵值 | 說明 |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | 實際使用的曝光時間 |
| `SENSOR_SENSITIVITY` | 實際使用的 ISO |
| `CONTROL_AF_STATE` | 自動對焦狀態 |
| `CONTROL_AE_STATE` | 自動曝光狀態 |
| `CONTROL_AWB_STATE` | 自動白平衡狀態 |
| `SCALER_CROP_REGION` | 使用的裁切區域 |
| `COLOR_CORRECTION_GAINS` | 色彩校正增益 |

## 完整的 RAW 捕獲範例

```kotlin
private fun configureDualCapture() {
    // 建立 JPEG ImageReader
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // 建立 RAW ImageReader
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
    
    // 建立表面
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // 建立包含所有表面的捕獲工作階段
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // 加入兩個表面作為目標
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // 設定控制項
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

## RAW 與 JPEG 比較

| 功能 | RAW | JPEG |
| --- | --- | --- |
| 檔案大小 | 大（20-50MB） | 小（2-10MB） |
| 編輯彈性 | 最大 | 有限 |
| 雜訊 | 保留 | 已降低 |
| 白平衡 | 可調整 | 固定 |
| 動態範圍 | 完整 | 已壓縮 |

## 最佳實務

1. **檢查 RAW 支援** — 在嘗試 RAW 捕獲前務必驗證
2. **雙重捕獲** — 同時捕獲 JPEG 和 RAW 以獲得彈性
3. **關閉影像** — 處理後務必呼叫 `image.close()`
4. **處理不同格式** — RAW_SENSOR、RAW10 和 RAW12 有不同的位元組配置

## 下一章

在下一章中，我們將總結你在第三部分學到的內容，並為第四部分做準備：手動相機控制。

## 總結

在本章中，你學習了：

1. **RAW 捕獲** — 捕獲未經處理的感測器數據以獲得最大的編輯彈性
2. **CaptureRequest** — 為每次捕獲設定相機參數
3. **CaptureResult** — 取得已完成捕獲的元數據

RAW 捕獲需要 FULL 或 LEVEL_3 硬體等級。你可以透過將兩個表面都加入捕獲工作階段來同時捕獲 JPEG 和 RAW。

CaptureRequest 讓你可以設定自動對焦、自動曝光、白平衡以及手動控制（如 ISO 和曝光時間）。CaptureResult 告訴你相機實際使用了哪些設定。

在第四部分中，我們將深入探討手動相機控制：ISO、曝光、對焦和白平衡。
