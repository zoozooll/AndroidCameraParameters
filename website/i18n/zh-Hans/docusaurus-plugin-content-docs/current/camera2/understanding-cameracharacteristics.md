---
sidebar_position: 7
title: "第 7 章：理解 CameraCharacteristics"
description: 探索 CameraCharacteristics，了解镜头朝向、硬件级别、传感器尺寸和其他重要的相机功能。
keywords: [CameraCharacteristics, 镜头朝向, 硬件级别, 传感器尺寸, 相机功能]
---

CameraCharacteristics 是了解相机本质的窗口。让我们一起来探索它。

## 简介

在上一章中，你学习了如何列出相机并获取基本信息。现在我们将深入探讨 **CameraCharacteristics**——相机功能的全面描述。

CameraCharacteristics 包含数百个参数。在本章中，我们将重点介绍最重要的参数。

## 什么是 CameraCharacteristics？

CameraCharacteristics 是一个不可变对象，包含有关相机设备的所有元数据。它描述了：

- **硬件属性** — 传感器尺寸、镜头特性
- **功能** — 相机能做什么
- **模式** — 可用的对焦、曝光和白平衡模式
- **输出选项** — 支持的分辨率和格式
- **性能** — 帧率、曝光范围

你可以从 CameraManager 获取 CameraCharacteristics：

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## 关键 CameraCharacteristics 键

让我们探索最重要的特性。

### 1. 镜头朝向

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

可能的值：
- `LENS_FACING_FRONT` — 前置摄像头（自拍）
- `LENS_FACING_BACK` — 后置摄像头
- `LENS_FACING_EXTERNAL` — 外接摄像头

### 2. 硬件级别

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

这是最重要的特性之一：

| 级别 | API 级别 | 功能 |
| --- | --- | --- |
| **LEGACY** | 21 | 有限的 Camera2 支持，封装了旧的 Camera API |
| **LIMITED** | 21 | 基本 Camera2 功能，无手动控制 |
| **FULL** | 21 | 完整手动控制，RAW 捕获 |
| **LEVEL_3** | 24 | 高级功能，如 YUV 再处理 |

### 3. 传感器尺寸

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width 和 sensorSize.height 给出尺寸
```

传感器尺寸告诉你传感器有多少像素。这与图像分辨率不同——传感器的像素可能多于单次捕获所使用的像素。

### 4. 活动阵列尺寸

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

活动阵列是传感器实际用于捕获图像的区域。这通常略小于像素阵列，因为一些像素被保留用于校准。

### 5. 可用功能

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

这个数组告诉你相机支持哪些功能：
- `BACKWARD_COMPATIBLE` — 基本兼容性
- `MANUAL_SENSOR` — 手动传感器控制
- `MANUAL_POST_PROCESSING` — 手动后处理
- `RAW` — RAW 捕获支持
- `BURST_CAPTURE` — 连拍
- `YUV_REPROCESSING` — YUV 再处理
- `DEPTH_OUTPUT` — 深度输出
- `CONSTRAINED_HIGH_SPEED_VIDEO` — 高速视频

### 6. 输出格式

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

流配置映射包含相机支持的所有输出格式和尺寸：
- `ImageFormat.JPEG` — 标准 JPEG
- `ImageFormat.RAW_SENSOR` — RAW 传感器数据
- `ImageFormat.YUV_420_888` — YUV 格式
- `ImageFormat.RAW10` — 10 位 RAW
- `ImageFormat.RAW12` — 12 位 RAW

### 7. 支持的预览尺寸

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

这会给你相机所有可用的预览分辨率。

### 8. 支持的图片尺寸

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

这些是可用于静态图像捕获的分辨率。

### 9. 焦距

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

这个数组包含镜头的焦距（以毫米为单位）。多个值表示光学变焦能力。

### 10. 对焦距离范围

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

最小对焦距离告诉你相机可以对焦的最近距离。值越小表示微距能力越好。

## 实际示例

让我们创建一个更详细的相机信息应用：

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // 镜头朝向
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "前置"
        CameraCharacteristics.LENS_FACING_BACK -> "后置"
        else -> "外接"
    }
    
    // 硬件级别
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "未知"
    }
    
    // 传感器尺寸
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // 活动阵列尺寸
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // 焦距
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "未知"
    
    // 可用功能
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "向后兼容"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "手动传感器"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW 捕获"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "连拍"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV 再处理"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "深度输出"
            else -> "未知功能"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== 相机 $cameraId ===")
    Log.d("CameraDetails", "镜头朝向: $lensFacingStr")
    Log.d("CameraDetails", "硬件级别: $hardwareLevelStr")
    Log.d("CameraDetails", "传感器尺寸: $sensorSizeStr")
    Log.d("CameraDetails", "活动阵列: $activeArrayStr")
    Log.d("CameraDetails", "焦距: $focalLengthsStr")
    Log.d("CameraDetails", "功能: ${capabilitiesList.joinToString(", ")}")
}
```

## 输出示例

```
=== 相机 0 ===
镜头朝向: 后置
硬件级别: FULL
传感器尺寸: 4032 x 3024
活动阵列: 4000 x 3000
焦距: 2.4mm, 4.8mm
功能: 向后兼容, 手动传感器, RAW 捕获, 连拍
```

## 为什么 CameraCharacteristics 很重要

在打开相机或创建捕获会话之前，你**必须**检查 CameraCharacteristics：

1. **验证功能** — 不要假设某个功能受支持
2. **选择合适的相机** — 根据镜头朝向、硬件级别等进行选择
3. **配置输出** — 使用支持的分辨率和格式
4. **处理设备差异** — 在一个设备上有效的功能在另一个设备上可能无效

## 使用 Android Camera Parameters 探索

打开 Android Camera Parameters 应用，浏览各种特性。你会看到按类别组织的数百个参数：

- **相机信息** — 基本相机信息
- **传感器** — 传感器特性
- **镜头** — 镜头属性
- **控制** — 自动曝光、自动对焦、白平衡
- **缩放器** — 输出尺寸和格式
- **闪光灯** — 闪光灯功能
- **统计** — 统计输出

这让你全面了解相机的功能。

## 下一章

既然你已经理解了 CameraCharacteristics，就可以打开你的第一个相机了！在下一章中，我们将：

1. 了解 CameraDevice
2. 使用 CameraManager 打开相机
3. 处理相机状态回调
4. 了解相机生命周期

## 总结

CameraCharacteristics 包含了解相机功能所需的所有信息：

- **镜头朝向** — 前置、后置或外接
- **硬件级别** — LEGACY、LIMITED、FULL、LEVEL_3
- **传感器尺寸** — 物理尺寸
- **活动阵列** — 捕获区域
- **焦距** — 镜头能力
- **功能** — 支持的特性
- **输出格式** — 可用的图像格式

使用相机前务必检查 CameraCharacteristics。这可以确保你的应用在不同设备上都能正常工作。

在下一章中，我们将使用 CameraDevice 打开我们的第一个相机。
