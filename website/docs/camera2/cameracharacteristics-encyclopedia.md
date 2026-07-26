---
sidebar_position: 15
title: "Chapter 15: CameraCharacteristics Encyclopedia"
description: A comprehensive guide to the most important Camera2 characteristics, including what they mean, why they exist, and how to use them.
keywords: [CameraCharacteristics, camera parameters, camera capabilities, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

Welcome to the CameraCharacteristics Encyclopedia — your guide to understanding every camera parameter.

## Introduction

CameraCharacteristics contains hundreds of parameters that describe a camera's capabilities. In this chapter, we'll explore the most important ones in depth:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — What can the camera do?
2. `REQUEST_AVAILABLE_CAPABILITIES` — What features are available?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — What's the sensor size?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — How much zoom?
5. `CONTROL_AE_AVAILABLE_MODES` — What exposure modes?

And many more...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**What does it mean?**  
This is the most important characteristic. It defines the overall capability level of the camera device.

**Why does it exist?**  
Different Android devices have different camera capabilities. This parameter helps applications understand what they can do.

**Supported values:**

| Value | API Level | Description |
| --- | --- | --- |
| `LEGACY` | 21 | Old devices, Camera2 API is a wrapper over the old Camera API |
| `LIMITED` | 21 | Basic Camera2 features, no manual controls |
| `FULL` | 21 | Full manual controls, RAW capture, burst capture |
| `LEVEL_3` | 24 | Advanced features like YUV reprocessing, 10-bit HDR |

**How is it used?**  
Check this before attempting any advanced operations:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // Limited functionality
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // Basic features only
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // Full manual controls available
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // Advanced features available
    }
}
```

**How to verify with Android Camera Parameters:**  
Open the app and look for "Hardware Level" in the Camera Info section.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**What does it mean?**  
This array lists all the capabilities supported by the camera.

**Why does it exist?**  
Even within the same hardware level, different devices may support different features.

**Common capabilities:**

| Capability | Description |
| --- | --- |
| `BACKWARD_COMPATIBLE` | Basic compatibility mode |
| `MANUAL_SENSOR` | Manual ISO and exposure control |
| `MANUAL_POST_PROCESSING` | Manual color correction and noise reduction |
| `RAW` | RAW image capture |
| `BURST_CAPTURE` | High-speed burst capture |
| `YUV_REPROCESSING` | YUV image reprocessing |
| `DEPTH_OUTPUT` | Depth map output |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | High-speed video capture |
| `LOGICAL_MULTI_CAMERA` | Logical camera combining multiple physical cameras |
| `CONCURRENT_CAMERA` | Multiple cameras can be opened simultaneously |
| `CAMERA_EXTENSION` | Manufacturer-specific extensions (portrait, night mode) |

**How is it used?**  
Check capabilities before using a feature:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // Enable RAW capture
}
```

**How to verify with Android Camera Parameters:**  
Look for "Available Capabilities" in the Camera Info section.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**What does it mean?**  
The active array is the actual area of the sensor used for capturing images.

**Why does it exist?**  
The sensor may have pixels around the edges that are reserved for calibration. The active array represents the usable area.

**How is it used?**  
This tells you the maximum resolution available for capture:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**Related characteristics:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — Total pixels on the sensor (may be larger than active array)
- `SENSOR_INFO_SENSOR_SIZE` — Physical dimensions in millimeters

**How to verify with Android Camera Parameters:**  
Look for "Active Array Size" and "Sensor Size" in the Sensor section.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**What does it mean?**  
The maximum digital zoom factor supported by the camera.

**Why does it exist?**  
Digital zoom crops and enlarges the image, reducing quality. Knowing the maximum helps manage user expectations.

**How is it used?**  
Set zoom level in capture requests:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// Set zoom (1.0 = no zoom, maxZoom = maximum zoom)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**Related characteristics:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — Physical focal lengths (for optical zoom)

**How to verify with Android Camera Parameters:**  
Look for "Max Digital Zoom" in the Scaler section.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**What does it mean?**  
Available auto-exposure modes.

**Why does it exist?**  
Different devices support different AE strategies.

**Common modes:**

| Mode | Description |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | Manual exposure control |
| `CONTROL_AE_MODE_ON` | Auto-exposure |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | Auto-exposure with flash always on |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | Auto-exposure with automatic flash |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | Auto-exposure with red-eye reduction |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | Auto-exposure with external flash |

**How is it used?**  
Set the AE mode in capture requests:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**How to verify with Android Camera Parameters:**  
Look for "AE Available Modes" in the Control section.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**What does it mean?**  
Available autofocus modes.

**Why does it exist?**  
Different focus strategies for different scenarios.

**Common modes:**

| Mode | Description |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Manual focus |
| `CONTROL_AF_MODE_AUTO` | Single-shot autofocus |
| `CONTROL_AF_MODE_MACRO` | Macro focus |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Continuous autofocus for video |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Continuous autofocus for photos |
| `CONTROL_AF_MODE_EDGE` | Edge autofocus |
| `CONTROL_AF_MODE_FIXED` | Fixed focus (no AF) |

**How is it used?**  
Set the AF mode based on your use case:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**How to verify with Android Camera Parameters:**  
Look for "AF Available Modes" in the Control section.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**What does it mean?**  
Available auto-white balance modes.

**Common modes:**

| Mode | Description |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Manual white balance |
| `CONTROL_AWB_MODE_AUTO` | Automatic |
| `CONTROL_AWB_MODE_INCANDESCENT` | Tungsten lighting |
| `CONTROL_AWB_MODE_FLUORESCENT` | Fluorescent lighting |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Warm fluorescent |
| `CONTROL_AWB_MODE_DAYLIGHT` | Daylight |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Cloudy |

**How to verify with Android Camera Parameters:**  
Look for "AWB Available Modes" in the Control section.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**What does it mean?**  
Available focal lengths for the lens(es).

**Why does it exist?**  
Multiple values indicate optical zoom capabilities.

**How is it used?**  
Determine what lenses are available:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**Common focal lengths:**
- 2.4mm — Wide-angle (common)
- 4.8mm — Telephoto (2x optical zoom)
- 1.8mm — Ultra-wide

**How to verify with Android Camera Parameters:**  
Look for "Available Focal Lengths" in the Lens section.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**What does it mean?**  
The closest distance at which the lens can focus.

**Why does it exist?**  
Lower values mean better macro capability.

**How is it used?**  
Check if macro photography is possible:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// A value of 0.1m (10cm) or less indicates good macro capability
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**How to verify with Android Camera Parameters:**  
Look for "Minimum Focus Distance" in the Lens section.

---

## 10. FLASH_INFO_AVAILABLE

**What does it mean?**  
Whether the camera has a flash.

**Why does it exist?**  
Not all cameras have flash (especially front cameras).

**How is it used?**  
Check before using flash:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // Enable flash features
}
```

**How to verify with Android Camera Parameters:**  
Look for "Flash Available" in the Flash section.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**What does it mean?**  
The minimum and maximum exposure time supported.

**Why does it exist?**  
Determines low-light capability and motion freeze ability.

**How is it used?**  
Check exposure range for manual control:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// Convert to seconds for display
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**How to verify with Android Camera Parameters:**  
Look for "Exposure Time Range" in the Sensor section.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**What does it mean?**  
The minimum and maximum ISO values supported.

**Why does it exist?**  
Determines low-light capability and noise performance.

**How is it used?**  
Check ISO range for manual control:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**How to verify with Android Camera Parameters:**  
Look for "Sensitivity Range" in the Sensor section.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**What does it mean?**  
All supported output sizes and formats.

**Why does it exist?**  
Determines what resolutions and formats you can use.

**How is it used?**  
Get supported sizes for different use cases:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// Preview sizes
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// Photo sizes
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// Video sizes
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// RAW sizes
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**How to verify with Android Camera Parameters:**  
Look for "Preview Sizes", "Picture Sizes", etc. in the Scaler section.

---

## 14. LENS_FACING

**What does it mean?**  
Which direction the lens is facing.

**Why does it exist?**  
Determines if it's a front, back, or external camera.

**Values:**
- `LENS_FACING_FRONT` — Selfie camera
- `LENS_FACING_BACK` — Rear camera  
- `LENS_FACING_EXTERNAL` — External camera

**How to verify with Android Camera Parameters:**  
Look for "Lens Facing" in the Camera Info section.

---

## 15. CONTROL_MAX_REGIONS_AE

**What does it mean?**  
Maximum number of AE metering regions.

**Why does it exist?**  
Determines how precise exposure metering can be.

**How is it used?**  
Limit the number of AE regions you create:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// Create no more than maxAERegions
```

**How to verify with Android Camera Parameters:**  
Look for "Max Regions AE" in the Control section.

---

## Conclusion

CameraCharacteristics is your window into the camera's capabilities. By understanding these parameters, you can:

1. **Build device-agnostic apps** — Check capabilities before using features
2. **Provide better user experiences** — Show only available features
3. **Optimize performance** — Choose appropriate resolutions and formats
4. **Create professional applications** — Unlock the full potential of the camera

## How to Learn More

1. **Android Camera Parameters app** — Explore real data from your device
2. **Android documentation** — Read the official CameraCharacteristics docs
3. **Experiment** — Write small test apps to try different parameters
4. **Source code** — Look at the Camera2 source code for deeper understanding

## Summary

This chapter covered the most important CameraCharacteristics:

1. **Hardware Level** — Overall capability
2. **Capabilities** — Specific features available
3. **Active Array** — Sensor resolution
4. **Digital Zoom** — Zoom capabilities
5. **AE/AF/AWB Modes** — Auto-control modes
6. **Focal Lengths** — Lens capabilities
7. **Focus Distance** — Macro capability
8. **Flash** — Flash availability
9. **Exposure/ISO Range** — Manual control limits
10. **Stream Configuration** — Supported sizes and formats

With this knowledge, you're ready to build advanced Camera2 applications!

---

## Final Words

Congratulations! You've completed this Android Camera2 series. You now understand:

- **How smartphone cameras work** — Lenses, sensors, ISP
- **How Camera2 works** — CameraManager, CameraDevice, CaptureSession
- **How to capture photos** — JPEG, RAW, ImageReader
- **How to control the camera** — ISO, exposure, focus, white balance
- **Professional features** — High-speed video, multi-camera
- **Camera characteristics** — The encyclopedia of camera capabilities

The Android Camera Parameters app is a great tool to continue learning. Explore your device's capabilities and experiment with different settings.

Happy coding! 📸