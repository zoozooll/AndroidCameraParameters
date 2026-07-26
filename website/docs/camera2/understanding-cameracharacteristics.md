---
sidebar_position: 7
title: "Chapter 7: Understanding CameraCharacteristics"
description: Explore CameraCharacteristics to learn about lens facing, hardware level, sensor size, and other important camera capabilities.
keywords: [CameraCharacteristics, lens facing, hardware level, sensor size, camera capabilities]
---

CameraCharacteristics is your window into the camera's soul. Let's explore it.

## Introduction

In the previous chapter, you learned how to list cameras and get basic information. Now we'll dive deeper into **CameraCharacteristics** — the comprehensive description of a camera's capabilities.

CameraCharacteristics contains hundreds of parameters. In this chapter, we'll focus on the most important ones.

## What is CameraCharacteristics?

CameraCharacteristics is an immutable object that contains all the metadata about a camera device. It describes:

- **Hardware properties** — Sensor size, lens characteristics
- **Capabilities** — What the camera can do
- **Modes** — Available focus, exposure, and white balance modes
- **Output options** — Supported resolutions and formats
- **Performance** — Frame rates, exposure ranges

You get CameraCharacteristics from CameraManager:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## Key CameraCharacteristics Keys

Let's explore the most important characteristics.

### 1. Lens Facing

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

Possible values:
- `LENS_FACING_FRONT` — Front-facing camera (selfie)
- `LENS_FACING_BACK` — Rear-facing camera
- `LENS_FACING_EXTERNAL` — External camera

### 2. Hardware Level

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

This is one of the most important characteristics:

| Level | API Level | Features |
| --- | --- | --- |
| **LEGACY** | 21 | Limited Camera2 support, wrapped old Camera API |
| **LIMITED** | 21 | Basic Camera2 features, no manual controls |
| **FULL** | 21 | Full manual controls, RAW capture |
| **LEVEL_3** | 24 | Advanced features like YUV reprocessing |

### 3. Sensor Size

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width and sensorSize.height give the dimensions
```

The sensor size tells you how many pixels the sensor has. This is different from the image resolution — the sensor may have more pixels than are used in a single capture.

### 4. Active Array Size

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

The active array is the actual area of the sensor used for capturing images. This is usually slightly smaller than the pixel array because some pixels are reserved for calibration.

### 5. Available Capabilities

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

This array tells you what features the camera supports:
- `BACKWARD_COMPATIBLE` — Basic compatibility
- `MANUAL_SENSOR` — Manual sensor controls
- `MANUAL_POST_PROCESSING` — Manual post-processing
- `RAW` — RAW capture support
- `BURST_CAPTURE` — Burst capture
- `YUV_REPROCESSING` — YUV reprocessing
- `DEPTH_OUTPUT` — Depth output
- `CONSTRAINED_HIGH_SPEED_VIDEO` — High-speed video

### 6. Output Formats

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

The stream configuration map contains all the output formats and sizes supported by the camera:
- `ImageFormat.JPEG` — Standard JPEG
- `ImageFormat.RAW_SENSOR` — RAW sensor data
- `ImageFormat.YUV_420_888` — YUV format
- `ImageFormat.RAW10` — 10-bit RAW
- `ImageFormat.RAW12` — 12-bit RAW

### 7. Supported Preview Sizes

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

This gives you all available preview resolutions for the camera.

### 8. Supported Picture Sizes

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

These are the available resolutions for still image capture.

### 9. Focal Lengths

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

This array contains the focal lengths (in millimeters) of the lens. Multiple values indicate optical zoom capabilities.

### 10. Focus Distance Range

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

The minimum focus distance tells you how close the camera can focus. A smaller value means better macro capability.

## A Practical Example

Let's create a more detailed camera info app:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Lens facing
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        else -> "External"
    }
    
    // Hardware level
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Unknown"
    }
    
    // Sensor size
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Active array size
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Focal lengths
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Unknown"
    
    // Available capabilities
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
            else -> "Unknown capability"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Camera $cameraId ===")
    Log.d("CameraDetails", "Lens Facing: $lensFacingStr")
    Log.d("CameraDetails", "Hardware Level: $hardwareLevelStr")
    Log.d("CameraDetails", "Sensor Size: $sensorSizeStr")
    Log.d("CameraDetails", "Active Array: $activeArrayStr")
    Log.d("CameraDetails", "Focal Lengths: $focalLengthsStr")
    Log.d("CameraDetails", "Capabilities: ${capabilitiesList.joinToString(", ")}")
}
```

## Output Example

```
=== Camera 0 ===
Lens Facing: Back
Hardware Level: FULL
Sensor Size: 4032 x 3024
Active Array: 4000 x 3000
Focal Lengths: 2.4mm, 4.8mm
Capabilities: Backward Compatible, Manual Sensor, RAW Capture, Burst Capture
```

## Why CameraCharacteristics Matter

Before opening a camera or creating a capture session, you **must** check CameraCharacteristics:

1. **Verify capabilities** — Don't assume a feature is supported
2. **Choose the right camera** — Select based on lens facing, hardware level, etc.
3. **Configure outputs** — Use supported resolutions and formats
4. **Handle device differences** — What works on one device may not work on another

## Explore with Android Camera Parameters

Open the Android Camera Parameters app and browse through the characteristics. You'll see hundreds of parameters organized by category:

- **Camera Info** — Basic camera information
- **Sensor** — Sensor characteristics
- **Lens** — Lens properties
- **Control** — Auto-exposure, auto-focus, white balance
- **Scaler** — Output sizes and formats
- **Flash** — Flash capabilities
- **Statistics** — Statistics output

This gives you a complete picture of your camera's capabilities.

## Next Chapter

Now that you understand CameraCharacteristics, you're ready to open your first camera! In the next chapter, we'll:

1. Learn about CameraDevice
2. Open a camera using CameraManager
3. Handle camera state callbacks
4. Understand the camera lifecycle

## Summary

CameraCharacteristics contains all the information you need to understand a camera's capabilities:

- **Lens facing** — Front, back, or external
- **Hardware level** — LEGACY, LIMITED, FULL, LEVEL_3
- **Sensor size** — Physical dimensions
- **Active array** — Capture area
- **Focal lengths** — Lens capabilities
- **Capabilities** — Supported features
- **Output formats** — Available image formats

Always check CameraCharacteristics before using a camera. This ensures your app works across different devices.

In the next chapter, we'll open our first camera using CameraDevice.