---
sidebar_position: 2
description: Android Camera Parameters 应用的技术架构，包括 MVVM 模式详情、Jetpack Compose UI 结构和开发指南。
keywords: [安卓开发, mvvm, jetpack compose, camera2 api 教程]
---

# 开发者文档

本文档提供了 **Android Camera Parameters** 应用程序的技术概述、架构和开发指南。

## 项目概览

该应用程序是一个用于检查 Android Camera2 `CameraCharacteristics` 的诊断工具。它提供了一个现代化的、用户友好的界面，用于探索设备上所有摄像头镜头的硬件级别、功能和原始参数值。

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
- 维护 `UiState`，包括摄像头列表、所选索引、分类参数和搜索查询。
- **功能检测**：在 `detectFeatureFlags()` 中包含动态确定硬件功能（如 RAW 支持、OIS 和手动曝光）的逻辑。
- **分类**：将数百个 Camera2 键分组成逻辑部分（传感器、镜头等），以提高可读性。

#### `CameraParamsHelper`
Android `CameraManager` 的实用包装器。
- 检索特定 ID 的 `CameraCharacteristics`。
- 为复杂的摄像头类型提供专门的格式化（例如，将 `IntArray` 模式转换为人类可读的字符串）。

## UI 层 (Jetpack Compose)

UI 使用 **Material 3** 构建，并强制执行深色主题。

### 导航结构

应用使用在 `MainScreen.kt` 中管理的 `androidx.navigation.compose`。

| 屏幕 | 职责 |
| :--- | :--- |
| **[概览](overview.md)** | 显示摘要卡、硬件级别和关键功能芯片的高级仪表板。 |
| **类别** | 按部分分组的所有参数的可展开列表，带有搜索过滤功能。 |
| **原始 (JSON)** | 所有摄像头属性的语法高亮 JSON 表示。 |
| **详情** | 单个参数的焦点视图，显示格式化的值和原始数据。 |

### 样式

- **主题**：定义在 `Theme.kt` 中。
- **颜色**：主色调 `#7B61FF` (紫色) 用于高亮和主要操作。
- **表面**：深色背景 `#121417`，卡片使用 `#1E1F23` 变体。

## 关键逻辑

### 动态功能检测

仪表板上的“关键功能”芯片不是静态的。它们在 `CameraViewModel.detectFeatureFlags()` 中计算：

- **RAW**：通过 `REQUEST_AVAILABLE_CAPABILITIES_RAW` 检查。
- **OIS**：如果 `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` 包含 `ON` 则检测到。
- **手动曝光**：如果支持 `CONTROL_AE_MODE_OFF` 则可用。
- **手动聚焦**：如果 `LENS_INFO_MINIMUM_FOCUS_DISTANCE` 大于 0 则启用。

## 开发指南

### 先决条件
- Android Studio Ladybug (或更新版本)。
- Kotlin 2.0+ (项目使用新的 Compose Compiler Gradle 插件)。
- 最低 SDK：21 (Android 5.0)。

### 添加新类别
要添加或修改参数分组，请更新 `CameraViewModel.kt` 中的 `getCategoryForKey()` 方法。它使用摄像头键名的字符串匹配将其分配给类别。

### 更新主题
颜色可以在 `Color.kt` 中调整。应用旨在深色模式下呈现最佳效果；对浅色调色板的任何更改都应仔细测试。
