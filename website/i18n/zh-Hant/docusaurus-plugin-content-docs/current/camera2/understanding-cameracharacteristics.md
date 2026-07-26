---
sidebar_position: 7
title: "第七章：認識 CameraCharacteristics"
description: "探索 CameraCharacteristics，瞭解鏡頭方向、硬體等級、感光元件尺寸以及其他重要的相機功能。"
keywords: [CameraCharacteristics, 鏡頭方向, 硬體等級, 感光元件尺寸, 相機功能]
---

CameraCharacteristics 是你進入相機內在世界的窗口。讓我們一起探索它。

## 簡介

在上一章中，你學習了如何列出相機並取得基本資訊。現在我們將深入探討 **CameraCharacteristics**——它是相機功能的完整描述。

CameraCharacteristics 包含數百個參數。在本章中，我們將著重介紹最重要的參數。

## 什麼是 CameraCharacteristics？

CameraCharacteristics 是一個不可變的物件，包含有關相機裝置的所有中繼資料。它描述了：

- **硬體屬性** — 感光元件尺寸、鏡頭特性
- **功能** — 相機可以做什麼
- **模式** — 可用的對焦、曝光和白平衡模式
- **輸出選項** — 支援的解析度和格式
- **效能** — 幀率、曝光範圍

你可以從 CameraManager 取得 CameraCharacteristics：

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## 主要的 CameraCharacteristics 鍵值

讓我們探索最重要的特性。

### 1. 鏡頭方向

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

可能的值：
- `LENS_FACING_FRONT` — 前置相機（自拍）
- `LENS_FACING_BACK` — 後置相機
- `LENS_FACING_EXTERNAL` — 外接相機

### 2. 硬體等級

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

這是最重要的特性之一：

| 等級 | API 等級 | 功能 |
| --- | --- | --- |
| **LEGACY** | 21 | 有限的 Camera2 支援，包裝了舊的 Camera API |
| **LIMITED** | 21 | 基本 Camera2 功能，無手動控制 |
| **FULL** | 21 | 完整手動控制，RAW 拍攝 |
| **LEVEL_3** | 24 | 進階功能，如 YUV 再處理 |

### 3. 感光元件尺寸

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width 和 sensorSize.height 提供尺寸
```

感光元件尺寸告訴你感光元件有多少像素。這與圖片解析度不同——感光元件的像素可能比單次拍攝使用的像素更多。

### 4. 作用中陣列尺寸

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

作用中陣列是感光元件實際用於拍攝影像的區域。這通常比像素陣列稍小，因為一些像素被保留用於校準。

### 5. 可用功能

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

這個陣列告訴你相機支援哪些功能：
- `BACKWARD_COMPATIBLE` — 基本相容性
- `MANUAL_SENSOR` — 手動感光元件控制
- `MANUAL_POST_PROCESSING` — 手動後處理
- `RAW` — RAW 拍攝支援
- `BURST_CAPTURE` — 連拍
- `YUV_REPROCESSING` — YUV 再處理
- `DEPTH_OUTPUT` — 深度輸出
- `CONSTRAINED_HIGH_SPEED_VIDEO` — 高速錄影

### 6. 輸出格式

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

串流設定對應包含相機支援的所有輸出格式和尺寸：
- `ImageFormat.JPEG` — 標準 JPEG
- `ImageFormat.RAW_SENSOR` — RAW 感光元件資料
- `ImageFormat.YUV_420_888` — YUV 格式
- `ImageFormat.RAW10` — 10 位元 RAW
- `ImageFormat.RAW12` — 12 位元 RAW

### 7. 支援的預覽尺寸

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

這會提供相機所有可用的預覽解析度。

### 8. 支援的相片尺寸

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

這些是靜態影像拍攝的可用解析度。

### 9. 焦距

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

這個陣列包含鏡頭的焦距（以毫米為單位）。多個值表示光學變焦功能。

### 10. 對焦距離範圍

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

最小對焦距離告訴你相機可以對焦的最近距離。值越小表示微距能力越好。

## 實用範例

讓我們建立一個更詳細的相機資訊應用程式：

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // 鏡頭方向
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "前置"
        CameraCharacteristics.LENS_FACING_BACK -> "後置"
        else -> "外接"
    }
    
    // 硬體等級
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "未知"
    }
    
    // 感光元件尺寸
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // 作用中陣列尺寸
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // 焦距
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "未知"
    
    // 可用功能
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "向下相容"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "手動感光元件"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW 拍攝"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "連拍"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV 再處理"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "深度輸出"
            else -> "未知功能"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== 相機 $cameraId ===")
    Log.d("CameraDetails", "鏡頭方向：$lensFacingStr")
    Log.d("CameraDetails", "硬體等級：$hardwareLevelStr")
    Log.d("CameraDetails", "感光元件尺寸：$sensorSizeStr")
    Log.d("CameraDetails", "作用中陣列：$activeArrayStr")
    Log.d("CameraDetails", "焦距：$focalLengthsStr")
    Log.d("CameraDetails", "功能：${capabilitiesList.joinToString(", ")}")
}
```

## 輸出範例

```
=== 相機 0 ===
鏡頭方向：後置
硬體等級：FULL
感光元件尺寸：4032 x 3024
作用中陣列：4000 x 3000
焦距：2.4mm, 4.8mm
功能：向下相容, 手動感光元件, RAW 拍攝, 連拍
```

## 為什麼 CameraCharacteristics 很重要

在開啟相機或建立拍攝工作階段之前，你**必須**檢查 CameraCharacteristics：

1. **驗證功能** — 不要假設某項功能有支援
2. **選擇合適的相機** — 根據鏡頭方向、硬體等級等進行選擇
3. **設定輸出** — 使用支援的解析度和格式
4. **處理裝置差異** — 在某個裝置上可行的操作，在另一個裝置上可能不可行

## 使用 Android Camera Parameters 探索

開啟 Android Camera Parameters 應用程式，瀏覽各項特性。你會看到數百個參數按類別組織：

- **相機資訊** — 基本相機資訊
- **感光元件** — 感光元件特性
- **鏡頭** — 鏡頭屬性
- **控制** — 自動曝光、自動對焦、白平衡
- **縮放器** — 輸出尺寸和格式
- **閃光燈** — 閃光燈功能
- **統計** — 統計輸出

這讓你完整了解相機的功能。

## 下一章

現在你已經了解 CameraCharacteristics，準備好開啟你的第一台相機了！在下一章中，我們將：

1. 瞭解 CameraDevice
2. 使用 CameraManager 開啟相機
3. 處理相機狀態回調
4. 瞭解相機生命週期

## 總結

CameraCharacteristics 包含你瞭解相機功能所需的所有資訊：

- **鏡頭方向** — 前置、後置或外接
- **硬體等級** — LEGACY、LIMITED、FULL、LEVEL_3
- **感光元件尺寸** — 實體尺寸
- **作用中陣列** — 拍攝區域
- **焦距** — 鏡頭功能
- **功能** — 支援的特性
- **輸出格式** — 可用的影像格式

使用相機前請務必先檢查 CameraCharacteristics。這可確保你的應用程式可在不同裝置上執行。

在下一章中，我們將使用 CameraDevice 開啟我們的第一台相機。
