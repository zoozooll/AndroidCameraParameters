---
sidebar_position: 15
title: "第十五章：CameraCharacteristics 百科全書"
description: 最重要的 Camera2 特性完整指南，包括它們的意義、存在原因以及如何使用。
keywords: [CameraCharacteristics, 相機參數, 相機功能, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

歡迎來到 CameraCharacteristics 百科全書——這是你瞭解每個相機參數的指南。

## 簡介

CameraCharacteristics 包含數百個描述相機功能的參數。在本章中，我們將深入探討最重要的參數：

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — 相機能做什麼？
2. `REQUEST_AVAILABLE_CAPABILITIES` — 有哪些功能可用？
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — 感光元件尺寸是多少？
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — 變焦倍率是多少？
5. `CONTROL_AE_AVAILABLE_MODES` — 有哪些曝光模式？

還有更多...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**這是什麼意思？**  
這是最重要的特性。它定義了相機裝置的整體功能等級。

**為什麼存在？**  
不同的 Android 裝置有不同的相機功能。這個參數幫助應用程式了解它們能做什麼。

**支援的值：**

| 值 | API 等級 | 說明 |
| --- | --- | --- |
| `LEGACY` | 21 | 舊裝置，Camera2 API 是舊 Camera API 的包裝層 |
| `LIMITED` | 21 | 基本 Camera2 功能，無手動控制 |
| `FULL` | 21 | 完整手動控制、RAW 拍攝、連拍 |
| `LEVEL_3` | 24 | 進階功能，如 YUV 再處理、10 位元 HDR |

**如何使用？**  
在嘗試任何進階操作之前先檢查這個值：

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // 功能有限
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // 僅基本功能
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // 可使用完整手動控制
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // 可使用進階功能
    }
}
```

**如何使用 Android Camera Parameters 驗證：**  
開啟應用程式，在「相機資訊」區段尋找「硬體等級」。

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**這是什麼意思？**  
這個陣列列出了相機支援的所有功能。

**為什麼存在？**  
即使在相同的硬體等級內，不同的裝置可能支援不同的功能。

**常見功能：**

| 功能 | 說明 |
| --- | --- |
| `BACKWARD_COMPATIBLE` | 基本相容模式 |
| `MANUAL_SENSOR` | 手動 ISO 和曝光控制 |
| `MANUAL_POST_PROCESSING` | 手動色彩校正和雜訊抑制 |
| `RAW` | RAW 影像拍攝 |
| `BURST_CAPTURE` | 高速連拍 |
| `YUV_REPROCESSING` | YUV 影像再處理 |
| `DEPTH_OUTPUT` | 深度圖輸出 |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | 高速錄影 |
| `LOGICAL_MULTI_CAMERA` | 結合多個實體相機的邏輯相機 |
| `CONCURRENT_CAMERA` | 可同時開啟多個相機 |
| `CAMERA_EXTENSION` | 製造商特定擴充功能（人像、夜間模式） |

**如何使用？**  
在使用某項功能之前先檢查功能清單：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // 啟用 RAW 拍攝
}
```

**如何使用 Android Camera Parameters 驗證：**  
在「相機資訊」區段尋找「可用功能」。

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**這是什麼意思？**  
作用中陣列是感光元件實際用於拍攝影像的區域。

**為什麼存在？**  
感光元件邊緣可能有保留用於校準的像素。作用中陣列代表可用區域。

**如何使用？**  
這告訴你拍攝可用的最大解析度：

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "作用中陣列：${width}x$height")
```

**相關特性：**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — 感光元件上的總像素數（可能比作用中陣列大）
- `SENSOR_INFO_SENSOR_SIZE` — 實體尺寸（以毫米為單位）

**如何使用 Android Camera Parameters 驗證：**  
在「感光元件」區段尋找「作用中陣列尺寸」和「感光元件尺寸」。

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**這是什麼意思？**  
相機支援的最大數位變焦倍率。

**為什麼存在？**  
數位變焦會裁剪並放大影像，降低畫質。了解最大值有助於管理使用者期望。

**如何使用？**  
在拍攝要求中設定變焦等級：

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// 設定變焦（1.0 = 無變焦，maxZoom = 最大變焦）
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**相關特性：**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — 實體焦距（用於光學變焦）

**如何使用 Android Camera Parameters 驗證：**  
在「縮放器」區段尋找「最大數位變焦」。

---

## 5. CONTROL_AE_AVAILABLE_MODES

**這是什麼意思？**  
可用的自動曝光模式。

**為什麼存在？**  
不同的裝置支援不同的 AE 策略。

**常見模式：**

| 模式 | 說明 |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | 手動曝光控制 |
| `CONTROL_AE_MODE_ON` | 自動曝光 |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | 自動曝光，閃光燈恆亮 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | 自動曝光，自動閃光 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | 自動曝光，紅眼減輕 |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | 自動曝光，外接閃光燈 |

**如何使用？**  
在拍攝要求中設定 AE 模式：

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**如何使用 Android Camera Parameters 驗證：**  
在「控制」區段尋找「AE 可用模式」。

---

## 6. CONTROL_AF_AVAILABLE_MODES

**這是什麼意思？**  
可用的自動對焦模式。

**為什麼存在？**  
不同場景適用不同的對焦策略。

**常見模式：**

| 模式 | 說明 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | 手動對焦 |
| `CONTROL_AF_MODE_AUTO` | 單次自動對焦 |
| `CONTROL_AF_MODE_MACRO` | 微距對焦 |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | 影片連續自動對焦 |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 相片連續自動對焦 |
| `CONTROL_AF_MODE_EDGE` | 邊緣自動對焦 |
| `CONTROL_AF_MODE_FIXED` | 固定對焦（無 AF） |

**如何使用？**  
根據你的使用場景設定 AF 模式：

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**如何使用 Android Camera Parameters 驗證：**  
在「控制」區段尋找「AF 可用模式」。

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**這是什麼意思？**  
可用的自動白平衡模式。

**常見模式：**

| 模式 | 說明 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | 手動白平衡 |
| `CONTROL_AWB_MODE_AUTO` | 自動 |
| `CONTROL_AWB_MODE_INCANDESCENT` | 鎢絲燈光 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 螢光燈光 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 暖螢光 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 日光 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 陰天 |

**如何使用 Android Camera Parameters 驗證：**  
在「控制」區段尋找「AWB 可用模式」。

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**這是什麼意思？**  
鏡頭的可用焦距。

**為什麼存在？**  
多個值表示有光學變焦功能。

**如何使用？**  
判斷有哪些鏡頭可用：

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "焦距：${fl}mm")
}
```

**常見焦距：**
- 2.4mm — 廣角（常見）
- 4.8mm — 望遠（2 倍光學變焦）
- 1.8mm — 超廣角

**如何使用 Android Camera Parameters 驗證：**  
在「鏡頭」區段尋找「可用焦距」。

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**這是什麼意思？**  
鏡頭可以對焦的最近距離。

**為什麼存在？**  
值越小表示微距能力越好。

**如何使用？**  
檢查是否可以進行微距攝影：

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// 0.1m（10cm）或更小的值表示良好的微距能力
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**如何使用 Android Camera Parameters 驗證：**  
在「鏡頭」區段尋找「最小對焦距離」。

---

## 10. FLASH_INFO_AVAILABLE

**這是什麼意思？**  
相機是否有閃光燈。

**為什麼存在？**  
並非所有相機都有閃光燈（尤其是前相機）。

**如何使用？**  
使用閃光燈之前先檢查：

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // 啟用閃光燈功能
}
```

**如何使用 Android Camera Parameters 驗證：**  
在「閃光燈」區段尋找「閃光燈可用」。

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**這是什麼意思？**  
支援的最小和最大曝光時間。

**為什麼存在？**  
決定低光能力和凍結動態的能力。

**如何使用？**  
檢查曝光範圍以進行手動控制：

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// 轉換為秒以供顯示
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**如何使用 Android Camera Parameters 驗證：**  
在「感光元件」區段尋找「曝光時間範圍」。

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**這是什麼意思？**  
支援的最小和最大 ISO 值。

**為什麼存在？**  
決定低光能力和雜訊表現。

**如何使用？**  
檢查 ISO 範圍以進行手動控制：

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**如何使用 Android Camera Parameters 驗證：**  
在「感光元件」區段尋找「靈敏度範圍」。

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**這是什麼意思？**  
所有支援的輸出尺寸和格式。

**為什麼存在？**  
決定你可以使用哪些解析度和格式。

**如何使用？**  
取得不同使用場景的支援尺寸：

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// 預覽尺寸
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// 相片尺寸
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// 影片尺寸
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// RAW 尺寸
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**如何使用 Android Camera Parameters 驗證：**  
在「縮放器」區段尋找「預覽尺寸」、「相片尺寸」等。

---

## 14. LENS_FACING

**這是什麼意思？**  
鏡頭面朝的方向。

**為什麼存在？**  
判斷是前相機、後相機還是外接相機。

**值：**
- `LENS_FACING_FRONT` — 前相機（自拍）
- `LENS_FACING_BACK` — 後相機  
- `LENS_FACING_EXTERNAL` — 外接相機

**如何使用 Android Camera Parameters 驗證：**  
在「相機資訊」區段尋找「鏡頭方向」。

---

## 15. CONTROL_MAX_REGIONS_AE

**這是什麼意思？**  
AE 測光區域的最大數量。

**為什麼存在？**  
決定曝光測光可以有多精確。

**如何使用？**  
限制你建立的 AE 區域數量：

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// 建立不超過 maxAERegions 的區域
```

**如何使用 Android Camera Parameters 驗證：**  
在「控制」區段尋找「AE 最大區域數」。

---

## 結論

CameraCharacteristics 是你了解相機功能的窗口。透過瞭解這些參數，你可以：

1. **建構與裝置無關的應用程式** — 使用功能前先檢查
2. **提供更好的使用者體驗** — 只顯示可用的功能
3. **最佳化效能** — 選擇適當的解析度和格式
4. **建立專業應用程式** — 釋放相機的全部潛力

## 如何進一步學習

1. **Android Camera Parameters 應用程式** — 探索你裝置的真實資料
2. **Android 文件** — 閱讀官方 CameraCharacteristics 文件
3. **實驗** — 撰寫小型測試應用程式來嘗試不同的參數
4. **原始碼** — 查看 Camera2 原始碼以深入瞭解

## 總結

本章涵蓋了最重要的 CameraCharacteristics：

1. **硬體等級** — 整體功能
2. **功能** — 可用的特定功能
3. **作用中陣列** — 感光元件解析度
4. **數位變焦** — 變焦能力
5. **AE/AF/AWB 模式** — 自動控制模式
6. **焦距** — 鏡頭功能
7. **對焦距離** — 微距能力
8. **閃光燈** — 閃光燈可用性
9. **曝光/ISO 範圍** — 手動控制限制
10. **串流設定** — 支援的尺寸和格式

有了這些知識，你就可以建構進階的 Camera2 應用程式了！

---

## 最後的話

恭喜！你已經完成了這個 Android Camera2 系列。現在你了解：

- **智慧型手機相機如何運作** — 鏡頭、感光元件、ISP
- **Camera2 如何運作** — CameraManager、CameraDevice、CaptureSession
- **如何拍攝相片** — JPEG、RAW、ImageReader
- **如何控制相機** — ISO、曝光、對焦、白平衡
- **專業功能** — 高速錄影、多相機
- **相機特性** — 相機功能的百科全書

Android Camera Parameters 應用程式是繼續學習的好工具。探索你裝置的功能並嘗試不同的設定。

寫作愉快！📸
