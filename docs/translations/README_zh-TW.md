# Android 相機參數 (Android Camera Parameters)

[English](../../README.md) | [简体中文](README_zh-CN.md) | 繁體中文 | [日本語](README_ja.md) | [한국어](README_ko.md)

Android 相機參數是一款強大的診斷工具，供開發人員和愛好者探索其設備相機的深層技術能力。它利用 Android Camera2 API 提供有關設備上每個鏡頭的詳細洞察。

![應用概覽](../designer/page1.png)

## 核心特性

*   **詳細診斷**：檢查所有鏡頭（後置、前置、外置）的 `CameraCharacteristics`。
*   **硬體層級檢測**：即時查看您的設備是否支援 `LEGACY`、`LIMITED`、`FULL` 或 `LEVEL_3` 特性。
*   **即時特性跟蹤**：檢查對 RAW 擷取、光學防手震 (OIS)、手動曝光、手動對焦等功能的支援。
*   **分類探索**：數百個參數按感測器、鏡頭、AE/AF/AWB 和處理類別組織。
*   **原始資料匯出**：以結構化 JSON 形式查看完整的相機設定檔。
*   **多語言支援**：完全在地化為 12 種以上語言，包括中文、西班牙語、日語等。

## 支援語言

該應用已在地化以支援全球使用者：
- 🇺🇸 英語 (English)
- 🇨🇳 中文 (簡體)
- 🇹🇼/🇭🇰 中文 (繁體)
- 🇪🇸 西班牙語 (Spanish)
- 🇧🇷 葡萄牙語 (巴西)
- 🇫🇷 法語 (French)
- 🇩🇪 德語 (German)
- 🇷🇺 俄語 (Russian)
- 🇮🇳 印地語 (Hindi)
- 🇮🇩 印度尼西亞語 (Indonesian)
- 🇯🇵 日語 (Japanese)
- 🇰🇷 韓語 (Korean)

## 技術棧

- **語言**: Kotlin
- **UI 框架**: Jetpack Compose
- **設計系統**: Material 3
- **架構**: MVVM
- **庫**:
    - [Camera2 API](https://developer.android.com/training/camera2): 核心相機互動。
    - [Gson](https://github.com/google/gson): 用於原始資料匯出的 JSON 序列化。
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): 應用導覽。
    - [Glide](https://github.com/bumptech/glide): 圖片載入。

## 專案結構

- `app/`: 主 Android 應用程式模組。
    - `com.aaron.cameraparams.ui`: 基於 Compose 的 UI 螢幕和元件。
    - `com.aaron.cameraparams.camera`: 與 CameraManager 互動並檢索特性的邏輯。
- `camera_parameters/`: 來自各種設備（Pixel 3、Samsung S10+ 等）的相機參數範例 JSON 傾印。
- `docs/`: 補充文件和螢幕截圖。

## 入門指南

### 前提條件

- Android Studio Koala 或更高版本。
- Android SDK 37 (編譯/目標)。
- 物理 Android 設備（推薦）或支援 Camera2 的模擬器。

### 建置與執行

1. 克隆倉庫：
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. 在 Android Studio 中開啟專案。
3. 建置專案：
   ```bash
   ./gradlew assembleDebug
   ```
4. 安裝並在您的設備上執行。

## 授權

本專案根據 **MIT 授權** 授權。有關詳細資訊，請參閱 [LICENSE](../../LICENSE) 檔案。

## 支援與聯繫

電子郵件: kangkang365@gmail.com
專案網站: [GitHub Pages](https://zoozooll.github.io/AndroidCameraParameters/)
