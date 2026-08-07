---
sidebar_position: 2
description: Android Camera Parameters 应用的技术架构，包括 MVVM 模式详情、Jetpack Compose UI 结构和开发指南。
keywords: [android 开发, mvvm, jetpack compose, camera2 api 教程]
---

# 开发者文档

本文档提供了 **Android Camera Parameters** 应用程序的技术概览、架构以及开发指南。

## 项目概览

该应用程序是一款用于检查 Android Camera2 `CameraCharacteristics` 的诊断工具。它提供了一个现代且用户友好的界面，用于探索设备上所有相机镜头的硬件级别、功能和原始参数值。

## 架构

项目遵循 **MVVM (Model-View-ViewModel)** 架构模式，并使用 **Jetpack Compose** 构建 UI 层。

### 核心组件

#### `CameraParamsActivity`
应用程序的单一入口点。
- 处理运行时权限 (CAMERA)。
- 通过 `setContent` 初始化 Compose UI。
- 托管 `CameraParamsTheme`。

#### `CameraViewModel`
UI 的中央状态管理器。
- 维护 `UiState`，其中包括相机列表、选定索引、分类参数和搜索查询。
- **特性检测**：在 `detectFeatureFlags()` 中包含逻辑，以动态确定 RAW 支持、OIS 和手动曝光等硬件能力。
- **分类**：将数百个 Camera2 键分组到逻辑部分（传感器、镜头等），以提高可读性。

#### `CameraParamsHelper`
Android `CameraManager` 的实用程序封装。
- 检索特定 ID 的 `CameraCharacteristics`。
- 为复杂的相机类型提供专门的格式化（例如，将 `IntArray` 模式转换为人类可读的字符串）。

## UI 层 (Jetpack Compose)

UI 使用 **Material 3** 构建，并严格执行深色主题。

### 导航结构

应用使用在 `MainScreen.kt` 中管理的 `androidx.navigation.compose`。

| 屏幕 | 职责 |
| :--- | :--- |
| **[概览](overview.md)** | 显示摘要卡、硬件级别和关键特性芯片的高级仪表板。 |
| **分类 (Categories)** | 按部分分组的所有参数的可扩展列表，支持搜索过滤。 |
| **原生 JSON (Raw)** | 所有相机属性的语法高亮 JSON 表示。 |
| **详情 (Detail)** | 单个参数的焦点视图，显示格式化值和原始数据。 |

### 样式

- **主题**：定义在 `Theme.kt` 中。
- **颜色**：主色调 `#7B61FF`（紫色）用于高亮和主要操作。
- **表面**：深色背景 `#121417`，卡片使用 `#1E1F23` 变体。

## 关键逻辑

### 动态特性检测

仪表板上的"关键特性"芯片不是静态的。它们在 `CameraViewModel.detectFeatureFlags()` 中计算：

- **RAW**：通过 `REQUEST_AVAILABLE_CAPABILITIES_RAW` 检查。
- **OIS**：如果 `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` 包含 `ON` 则检测到。
- **手动曝光**：如果支持 `CONTROL_AE_MODE_OFF` 则可用。
- **手动对焦**：如果 `LENS_INFO_MINIMUM_FOCUS_DISTANCE` 大于 0 则启用。

## 开发指南

### 前置条件
- Android Studio Ladybug (或更高版本)。
- Kotlin 2.0+ (项目使用新的 Compose Compiler Gradle 插件)。
- 最低 SDK：21 (Android 5.0)。

### 添加新类别
要添加或修改参数分组，请更新 `CameraViewModel.kt` 中的 `getCategoryForKey()` 方法。它对相机键名使用字符串匹配来将它们分配到类别。

### 更新主题
可以在 `Color.kt` 中调整颜色。应用旨在深色模式下呈现最佳效果；对浅色调色板的任何更改都应仔细测试。
