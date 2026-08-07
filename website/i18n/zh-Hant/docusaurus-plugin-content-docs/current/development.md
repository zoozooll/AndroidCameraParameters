---
sidebar_position: 2
description: Android Camera Parameters 應用的技術架構，包括 MVVM 模式詳情、Jetpack Compose UI 結構和開發指南。
keywords: [android 開發, mvvm, jetpack compose, camera2 api 教程]
---

# 開發者文件

本文件提供了 **Android Camera Parameters** 應用程式的技術概覽、架構以及開發指南。

## 專案概覽

該應用程式是一款用於檢查 Android Camera2 `CameraCharacteristics` 的診斷工具。它提供了一個現代且用戶友好的界面，用於探索設備上所有相機鏡頭的硬體層級、功能和原始參數值。

## 架構

專案遵循 **MVVM (Model-View-ViewModel)** 架構模式，並使用 **Jetpack Compose** 建構 UI 層。

### 核心組件

#### `CameraParamsActivity`
應用程式的單一進入點。
- 處理執行階段權限 (CAMERA)。
- 透過 `setContent` 初始化 Compose UI。
- 託管 `CameraParamsTheme`。

#### `CameraViewModel`
UI 的中央狀態管理器。
- 維護 `UiState`，其中包括相機列表、選定索引、分類參數和搜尋查詢。
- **特性檢測**：在 `detectFeatureFlags()` 中包含邏輯，以動態確定 RAW 支援、OIS 和手動曝光等硬體能力。
- **分類**：將數百個 Camera2 鍵分組到邏輯部分（感光元件、鏡頭等），以提高可讀性。

#### `CameraParamsHelper`
Android `CameraManager` 的實用程式封裝。
- 檢索特定 ID 的 `CameraCharacteristics`。
- 為複雜的相機類型提供專門的格式化（例如，將 `IntArray` 模式轉換為人類可讀的字串）。

## UI 層 (Jetpack Compose)

UI 使用 **Material 3** 建構，並嚴格執行深色主題。

### 導覽結構

應用使用在 `MainScreen.kt` 中管理的 `androidx.navigation.compose`。

| 螢幕 | 職責 |
| :--- | :--- |
| **[概覽](overview.md)** | 顯示摘要卡、硬體層級和關鍵特性標籤的高級儀表板。 |
| **分類 (Categories)** | 按部分分組的所有參數的可擴展列表，支援搜尋過濾。 |
| **原生 JSON (Raw)** | 所有相機屬性的語法高亮 JSON 表示。 |
| **詳情 (Detail)** | 單個參數的焦點視圖，顯示格式化值和原始數據。 |

### 樣式

- **主題**：定義在 `Theme.kt` 中。
- **顏色**：主色調 `#7B61FF`（紫色）用於高亮和主要操作。
- **表面**：深色背景 `#121417`，卡片使用 `#1E1F23` 變體。

## 關鍵邏輯

### 動態特性檢測

儀表板上的「關鍵特性」標籤不是靜態的。它們在 `CameraViewModel.detectFeatureFlags()` 中計算：

- **RAW**：透過 `REQUEST_AVAILABLE_CAPABILITIES_RAW` 檢查。
- **OIS**：如果 `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` 包含 `ON` 則檢測到。
- **手動曝光**：如果支援 `CONTROL_AE_MODE_OFF` 則可用。
- **手動對焦**：如果 `LENS_INFO_MINIMUM_FOCUS_DISTANCE` 大於 0 則啟用。

## 開發指南

### 前提條件
- Android Studio Ladybug (或更高版本)。
- Kotlin 2.0+ (專案使用新的 Compose Compiler Gradle 插件)。
- 最低 SDK：21 (Android 5.0)。

### 添加新類別
要添加或修改參數分組，請更新 `CameraViewModel.kt` 中的 `getCategoryForKey()` 方法。它對相機鍵名使用字串比對來將它們分配到類別。

### 更新主題
可以在 `Color.kt` 中調整顏色。應用旨在深色模式下呈現最佳效果；對淺色調色盤的任何更改都應仔細測試。
