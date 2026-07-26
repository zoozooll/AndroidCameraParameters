---
sidebar_position: 11
title: "第 11 章：RAW 捕获与 CaptureRequest"
description: 学习如何捕获 RAW 图像，以及理解 CaptureRequest 和 CaptureResult 以实现高级相机控制。
keywords: [RAW 捕获, CaptureRequest, CaptureResult, Camera2, 手动控制]
---

RAW 捕获让你可以完全控制图像处理。让我们一起探索它以及 CaptureRequest 和 CaptureResult。

## 简介

在上一章中，你学习了如何捕获 JPEG 照片。现在我们将探索：

1. **RAW 捕获** — 捕获未处理的传感器数据
2. **CaptureRequest** — 为每次捕获配置相机设置
3. **CaptureResult** — 获取已完成捕获的元数据

## 什么是 RAW？

RAW 图像包含传感器在 ISP 处理之前捕获的所有数据。这意味着：

- 未应用降噪
- 未进行白平衡校正
- 未进行锐化
- 完整的动态范围

RAW 文件更大，但提供了无与伦比的编辑灵活性。

## RAW 捕获要求

要捕获 RAW 图像，你的相机必须：
1. 具有 **FULL** 或 **LEVEL_3** 硬件级别
2. 支持 `RAW` 能力

检查 CameraCharacteristics：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## 捕获 RAW 图像

创建一个 RAW 格式的 ImageReader：

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // 或 ImageFormat.RAW10/RAW12
    2
)
```

然后将 JPEG 和 RAW surface 都添加到捕获会话以进行同时捕获：

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest 定义了单次捕获的所有设置。你可以配置：

### 自动控制
- `CONTROL_AF_MODE` — 自动对焦模式
- `CONTROL_AE_MODE` — 自动曝光模式
- `CONTROL_AWB_MODE` — 自动白平衡模式

### 手动控制
- `SENSOR_SENSITIVITY` — ISO 值
- `SENSOR_EXPOSURE_TIME` — 曝光时间（纳秒）
- `LENS_FOCUS_DISTANCE` — 对焦距离
- `LENS_APERTURE` — 光圈（如果可用）

### 输出设置
- `JPEG_QUALITY` — JPEG 压缩质量
- `JPEG_ORIENTATION` — 图像方向
- `COLOR_CORRECTION_MODE` — 色彩校正模式

### 创建 CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// 设置自动控制
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// 设置 JPEG 质量
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// 添加目标
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// 构建请求
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult 包含已完成捕获的元数据。它包括：

- **实际使用的设置** — 相机实际应用的设置
- **统计信息** — 曝光、对焦和色彩信息
- **时间戳** — 捕获发生的时间

### 获取 CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // 获取实际曝光时间
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // 获取实际 ISO
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // 获取对焦状态
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // 获取 AE 状态
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "曝光：${exposureTime ?: 0}ns，ISO：$iso")
    }
}
```

### 常用 CaptureResult 键

| 键 | 描述 |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | 实际使用的曝光时间 |
| `SENSOR_SENSITIVITY` | 实际使用的 ISO |
| `CONTROL_AF_STATE` | 自动对焦状态 |
| `CONTROL_AE_STATE` | 自动曝光状态 |
| `CONTROL_AWB_STATE` | 自动白平衡状态 |
| `SCALER_CROP_REGION` | 使用的裁剪区域 |
| `COLOR_CORRECTION_GAINS` | 色彩校正增益 |

## 完整的 RAW 捕获示例

```kotlin
private fun configureDualCapture() {
    // 创建 JPEG ImageReader
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // 创建 RAW ImageReader
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
    
    // 创建 surfaces
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // 使用所有 surface 创建捕获会话
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // 将两个 surface 都添加为目标
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // 配置控制
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

## RAW vs JPEG

| 特性 | RAW | JPEG |
| --- | --- | --- |
| 文件大小 | 大（20-50MB） | 小（2-10MB） |
| 编辑灵活性 | 最大 | 有限 |
| 噪点 | 保留 | 减少 |
| 白平衡 | 可调整 | 固定 |
| 动态范围 | 完整 | 压缩 |

## 最佳实践

1. **检查 RAW 支持** — 在尝试 RAW 捕获前始终验证
2. **双重捕获** — 同时捕获 JPEG 和 RAW 以获得灵活性
3. **关闭图像** — 处理后始终调用 `image.close()`
4. **处理不同格式** — RAW_SENSOR、RAW10 和 RAW12 有不同的字节布局

## 下一章

在下一章中，我们将总结第三部分所学内容，并为第四部分做准备：手动相机控制。

## 总结

在本章中，你学习了：

1. **RAW 捕获** — 捕获未处理的传感器数据以获得最大的编辑灵活性
2. **CaptureRequest** — 为每次捕获配置相机设置
3. **CaptureResult** — 获取已完成捕获的元数据

RAW 捕获需要 FULL 或 LEVEL_3 硬件级别。你可以通过将两个 surface 都添加到捕获会话来同时捕获 JPEG 和 RAW。

CaptureRequest 允许你配置自动对焦、自动曝光、白平衡以及 ISO 和曝光时间等手动控制。CaptureResult 告诉你相机实际使用了哪些设置。

在第四部分中，我们将深入探讨手动相机控制：ISO、曝光、对焦和白平衡。
