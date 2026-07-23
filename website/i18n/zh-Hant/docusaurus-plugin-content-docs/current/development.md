---
sidebar_position: 2
description: Android 相機參數應用程式的技術架構，包括 MVVM 模式細節、Jetpack Compose UI 結構和開發指南。
keywords: [android 開發, mvvm, jetpack compose, camera2 api 教程]
---

# 開發者文件

本文件提供了 **Android 相機參數 (Android Camera Parameters)** 應用程式的技術概覽、其架構及開發指南。

## 專案概覽

該應用程式是一款用於檢查 Android Camera2 `CameraCharacteristics` 的診斷工具。它提供了一個現代化且使用者友好的介ı，用於探索設備上所有相機鏡頭的硬體層級、功能和原始參數值。

## 架構

本專案遵循 **MVVM (Model-View-ViewModel)** 架構模式，並使用 **Jetpack Compose** 建置 UI 層。

### 核心元件

#### `CameraParamsActivity`
應用程式的單一進入點。
- 處理執行階段權限 (CAMERA)。
- 透過 `setContent` 初始化 Compose UI。
- 託管 `CameraParamsTheme`。

#### `CameraViewModel`
UI 的中央狀態管理員。
- 維護 `UiState`，包括相機列表、選定索引、分類參數和搜尋查詢。
- **特性檢測**：在 `detectFeatureFlags()` 中包含邏輯，以動態判斷硬體功能，如 RAW 支援、OIS 和手動曝光。
- **分類**：將數百個 Camera2 鍵歸類到邏輯區段（感測器、鏡頭等），以提高可讀性。

#### `CameraParamsHelper`
Android `CameraManager` 的公用程式封裝。
- 擷取特定 ID 的 `CameraCharacteristics`。
- 為複雜的相機類型提供專門的格式化（例如，將 `IntArray` 模式轉換為易於閱讀的字串）。

## UI 層 (Jetpack Compose)

UI 使用 **Material 3** 建置，並嚴格執行深色主題。

### 導覽結構

應用程式使用 `MainScreen.kt` 中管理的 `androidx.navigation.compose`。

| 螢幕 | 職責 |
| :--- | :--- |
| **[概覽](overview.md)** | 顯示摘要卡、硬體層級和關鍵特性標籤的高階儀表板。 |
| **分類** | 所有參數按區段分組的可展開列表，帶有搜尋過濾功能。 |
| **原始 (JSON)** | 所有相機屬性的語法醒目提示 JSON 表示。 |
| **詳細資訊** | 單個參數的焦點檢視，顯示格式化值和原始資料。 |

### 樣式

- **主題**：在 `Theme.kt` 中定義。
- **顏色**：主要顏色 `#7B61FF`（紫色）用於醒目提示和主要動作。
- **表面**：深色背景 `#121417`，卡片使用 `#1E1F23` 變體。

## 關鍵邏輯

### 動態特性檢測

儀表板上的「核心特性」標籤不是靜態的。它們是在 `CameraViewModel.detectFeatureFlags()` 中計算的：

- **RAW**：透過 `REQUEST_AVAILABLE_CAPABILITIES_RAW` 檢查。
- **OIS**：如果 `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` 包含 `ON` 則檢測到。
- **手動曝光**：如果支援 `CONTROL_AE_MODE_OFF` 則可用。
- **手動對焦**：如果 `LENS_INFO_MINIMUM_FOCUS_DISTANCE` 大於 0 則啟用。

## 開發指南

### 前提條件
- Android Studio Ladybug (或更高版本)。
- Kotlin 2.0+ (本專案使用新的 Compose Compiler Gradle 外掛)。
- 最低 SDK：21 (Android 5.0)。

### 新增分類
要新增或修改參數分組，請更新 `CameraViewModel.kt` 中的 `getCategoryForKey()` 方法。它對相機鍵名稱使用字串比對，將其分配給各個分類。

### 更新主題
顏色可以在 `Color.kt` 中調整。應用程式設計為在深色模式下效果最佳；對淺色調色盤的任何更改都應仔細測試。
