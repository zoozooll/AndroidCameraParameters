---
sidebar_position: 15
title: "第15章：CameraCharacteristics 百科全书"
description: 一份关于最重要的 Camera2 特性的综合指南，包括它们的含义、存在原因以及如何使用。
keywords: [CameraCharacteristics, 相机参数, 相机能力, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

欢迎来到 CameraCharacteristics 百科全书——这是你理解每个相机参数的指南。

## 简介

CameraCharacteristics 包含数百个描述相机能力的参数。在本章中，我们将深入探讨最重要的几个：

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — 相机能做什么？
2. `REQUEST_AVAILABLE_CAPABILITIES` — 有哪些可用功能？
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — 传感器尺寸是多少？
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — 有多少倍变焦？
5. `CONTROL_AE_AVAILABLE_MODES` — 有哪些曝光模式？

以及更多……

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**它是什么意思？**  
这是最重要的特性。它定义了相机设备的整体能力等级。

**为什么存在？**  
不同的 Android 设备具有不同的相机能力。这个参数帮助应用了解它们能做什么。

**支持的值：**

| 值 | API 级别 | 描述 |
| --- | --- | --- |
| `LEGACY` | 21 | 旧设备，Camera2 API 是旧 Camera API 的包装器 |
| `LIMITED` | 21 | 基本 Camera2 功能，无手动控制 |
| `FULL` | 21 | 完整手动控制、RAW 拍摄、连拍 |
| `LEVEL_3` | 24 | 高级功能，如 YUV 再处理、10-bit HDR |

**如何使用？**  
在尝试任何高级操作之前检查此项：

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // 功能有限
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // 仅有基本功能
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // 完整手动控制可用
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // 高级功能可用
    }
}
```

**如何用 Android Camera Parameters 验证：**  
打开应用，在相机信息部分查找"Hardware Level"。

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**它是什么意思？**  
此数组列出了相机支持的所有能力。

**为什么存在？**  
即使在相同的硬件级别内，不同的设备也可能支持不同的功能。

**常见能力：**

| 能力 | 描述 |
| --- | --- |
| `BACKWARD_COMPATIBLE` | 基本兼容模式 |
| `MANUAL_SENSOR` | 手动 ISO 和曝光控制 |
| `MANUAL_POST_PROCESSING` | 手动色彩校正和降噪 |
| `RAW` | RAW 图像拍摄 |
| `BURST_CAPTURE` | 高速连拍 |
| `YUV_REPROCESSING` | YUV 图像再处理 |
| `DEPTH_OUTPUT` | 深度图输出 |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | 高速视频拍摄 |
| `LOGICAL_MULTI_CAMERA` | 组合多个物理相机的逻辑相机 |
| `CONCURRENT_CAMERA` | 可同时打开多个相机 |
| `CAMERA_EXTENSION` | 厂商特定扩展（人像、夜景模式） |

**如何使用？**  
在使用功能前检查能力：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // 启用 RAW 拍摄
}
```

**如何用 Android Camera Parameters 验证：**  
在相机信息部分查找"Available Capabilities"。

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**它是什么意思？**  
活动阵列是传感器用于拍摄图像的实际区域。

**为什么存在？**  
传感器边缘可能有保留用于校准的像素。活动阵列代表可用区域。

**如何使用？**  
这告诉你可用于拍摄的最大分辨率：

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**相关特性：**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — 传感器上的总像素（可能大于活动阵列）
- `SENSOR_INFO_SENSOR_SIZE` — 物理尺寸（以毫米为单位）

**如何用 Android Camera Parameters 验证：**  
在传感器部分查找"Active Array Size"和"Sensor Size"。

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**它是什么意思？**  
相机支持的最大数码变焦倍率。

**为什么存在？**  
数码变焦会裁剪并放大图像，降低画质。了解最大值有助于管理用户期望。

**如何使用？**  
在拍摄请求中设置变焦级别：

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// 设置变焦（1.0 = 无变焦，maxZoom = 最大变焦）
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**相关特性：**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — 物理焦距（用于光学变焦）

**如何用 Android Camera Parameters 验证：**  
在 Scaler 部分查找"Max Digital Zoom"。

---

## 5. CONTROL_AE_AVAILABLE_MODES

**它是什么意思？**  
可用的自动曝光模式。

**为什么存在？**  
不同的设备支持不同的 AE 策略。

**常见模式：**

| 模式 | 描述 |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | 手动曝光控制 |
| `CONTROL_AE_MODE_ON` | 自动曝光 |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | 闪光灯常亮的自动曝光 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | 自动闪光灯的自动曝光 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | 带红眼消除的自动曝光 |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | 带外接闪光灯的自动曝光 |

**如何使用？**  
在拍摄请求中设置 AE 模式：

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**如何用 Android Camera Parameters 验证：**  
在 Control 部分查找"AE Available Modes"。

---

## 6. CONTROL_AF_AVAILABLE_MODES

**它是什么意思？**  
可用的自动对焦模式。

**为什么存在？**  
不同场景需要不同的对焦策略。

**常见模式：**

| 模式 | 描述 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | 手动对焦 |
| `CONTROL_AF_MODE_AUTO` | 单次自动对焦 |
| `CONTROL_AF_MODE_MACRO` | 微距对焦 |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | 视频连续自动对焦 |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 照片连续自动对焦 |
| `CONTROL_AF_MODE_EDGE` | 边缘自动对焦 |
| `CONTROL_AF_MODE_FIXED` | 固定对焦（无 AF） |

**如何使用？**  
根据你的用例设置 AF 模式：

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**如何用 Android Camera Parameters 验证：**  
在 Control 部分查找"AF Available Modes"。

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**它是什么意思？**  
可用的自动白平衡模式。

**常见模式：**

| 模式 | 描述 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | 手动白平衡 |
| `CONTROL_AWB_MODE_AUTO` | 自动 |
| `CONTROL_AWB_MODE_INCANDESCENT` | 白炽灯照明 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 荧光灯照明 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 暖荧光灯 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 日光 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 阴天 |

**如何用 Android Camera Parameters 验证：**  
在 Control 部分查找"AWB Available Modes"。

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**它是什么意思？**  
镜头可用的焦距。

**为什么存在？**  
多个值表示光学变焦能力。

**如何使用？**  
确定可用的镜头：

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**常见焦距：**
- 2.4mm — 广角（常见）
- 4.8mm — 长焦（2倍光学变焦）
- 1.8mm — 超广角

**如何用 Android Camera Parameters 验证：**  
在 Lens 部分查找"Available Focal Lengths"。

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**它是什么意思？**  
镜头能够对焦的最近距离。

**为什么存在？**  
值越低表示微距能力越好。

**如何使用？**  
检查是否可以进行微距摄影：

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// 0.1m（10cm）或更小的值表示良好的微距能力
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**如何用 Android Camera Parameters 验证：**  
在 Lens 部分查找"Minimum Focus Distance"。

---

## 10. FLASH_INFO_AVAILABLE

**它是什么意思？**  
相机是否有闪光灯。

**为什么存在？**  
并非所有相机都有闪光灯（尤其是前置摄像头）。

**如何使用？**  
使用闪光灯前检查：

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // 启用闪光灯功能
}
```

**如何用 Android Camera Parameters 验证：**  
在 Flash 部分查找"Flash Available"。

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**它是什么意思？**  
支持的最小和最大曝光时间。

**为什么存在？**  
决定低光能力和运动定格能力。

**如何使用？**  
检查曝光范围以进行手动控制：

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// 转换为秒用于显示
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**如何用 Android Camera Parameters 验证：**  
在 Sensor 部分查找"Exposure Time Range"。

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**它是什么意思？**  
支持的最小和最大 ISO 值。

**为什么存在？**  
决定低光能力和噪点表现。

**如何使用？**  
检查 ISO 范围以进行手动控制：

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**如何用 Android Camera Parameters 验证：**  
在 Sensor 部分查找"Sensitivity Range"。

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**它是什么意思？**  
所有支持的输出尺寸和格式。

**为什么存在？**  
决定你可以使用哪些分辨率和格式。

**如何使用？**  
获取不同用例的支持尺寸：

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// 预览尺寸
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// 照片尺寸
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// 视频尺寸
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// RAW 尺寸
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**如何用 Android Camera Parameters 验证：**  
在 Scaler 部分查找"Preview Sizes"、"Picture Sizes"等。

---

## 14. LENS_FACING

**它是什么意思？**  
镜头朝向哪个方向。

**为什么存在？**  
决定它是前置、后置还是外接相机。

**值：**
- `LENS_FACING_FRONT` — 自拍相机
- `LENS_FACING_BACK` — 后置相机
- `LENS_FACING_EXTERNAL` — 外接相机

**如何用 Android Camera Parameters 验证：**  
在相机信息部分查找"Lens Facing"。

---

## 15. CONTROL_MAX_REGIONS_AE

**它是什么意思？**  
AE 测光区域的最大数量。

**为什么存在？**  
决定曝光测光的精确程度。

**如何使用？**  
限制你创建的 AE 区域数量：

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// 创建不超过 maxAERegions 个区域
```

**如何用 Android Camera Parameters 验证：**  
在 Control 部分查找"Max Regions AE"。

---

## 结论

CameraCharacteristics 是你了解相机能力的窗口。通过理解这些参数，你可以：

1. **构建设备无关的应用** — 使用功能前检查能力
2. **提供更好的用户体验** — 只显示可用功能
3. **优化性能** — 选择合适的分辨率和格式
4. **创建专业应用** — 释放相机的全部潜力

## 如何了解更多

1. **Android Camera Parameters 应用** — 探索你设备上的真实数据
2. **Android 文档** — 阅读官方 CameraCharacteristics 文档
3. **实验** — 编写小型测试应用来尝试不同的参数
4. **源代码** — 查看 Camera2 源代码以获得更深入的理解

## 总结

本章涵盖了最重要的 CameraCharacteristics：

1. **硬件级别** — 整体能力
2. **能力** — 可用的特定功能
3. **活动阵列** — 传感器分辨率
4. **数码变焦** — 变焦能力
5. **AE/AF/AWB 模式** — 自动控制模式
6. **焦距** — 镜头能力
7. **对焦距离** — 微距能力
8. **闪光灯** — 闪光灯可用性
9. **曝光/ISO 范围** — 手动控制限制
10. **流配置** — 支持的尺寸和格式

有了这些知识，你就可以构建高级的 Camera2 应用了！

---

## 最后寄语

恭喜！你已经完成了这个 Android Camera2 系列。你现在理解了：

- **智能手机相机如何工作** — 镜头、传感器、ISP
- **Camera2 如何工作** — CameraManager、CameraDevice、CaptureSession
- **如何拍摄照片** — JPEG、RAW、ImageReader
- **如何控制相机** — ISO、曝光、对焦、白平衡
- **专业功能** — 高速视频、多相机
- **相机特性** — 相机能力的百科全书

Android Camera Parameters 应用是继续学习的好工具。探索你设备的能力，尝试不同的设置。

编码快乐！📸
