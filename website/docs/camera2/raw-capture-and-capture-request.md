---
sidebar_position: 11
title: "Chapter 11: RAW Capture and CaptureRequest"
description: Learn how to capture RAW images and understand CaptureRequest and CaptureResult for advanced camera control.
keywords: [RAW capture, CaptureRequest, CaptureResult, Camera2, manual controls]
---

RAW capture gives you complete control over image processing. Let's explore it along with CaptureRequest and CaptureResult.

## Introduction

In the previous chapter, you learned how to capture JPEG photos. Now we'll explore:

1. **RAW capture** — Capturing unprocessed sensor data
2. **CaptureRequest** — Configuring camera settings for each capture
3. **CaptureResult** — Getting metadata about completed captures

## What is RAW?

RAW images contain all the data captured by the sensor before ISP processing. This means:

- No noise reduction applied
- No white balance correction
- No sharpening
- Full dynamic range

RAW files are larger but offer unparalleled editing flexibility.

## RAW Capture Requirements

To capture RAW images, your camera must:
1. Have **FULL** or **LEVEL_3** hardware level
2. Support the `RAW` capability

Check CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## Capturing RAW Images

Create an ImageReader with a RAW format:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // or ImageFormat.RAW10/RAW12
    2
)
```

Then add both JPEG and RAW surfaces to the capture session for simultaneous capture:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest defines all the settings for a single capture. You can configure:

### Auto Controls
- `CONTROL_AF_MODE` — Autofocus mode
- `CONTROL_AE_MODE` — Auto-exposure mode
- `CONTROL_AWB_MODE` — Auto-white balance mode

### Manual Controls
- `SENSOR_SENSITIVITY` — ISO value
- `SENSOR_EXPOSURE_TIME` — Exposure time in nanoseconds
- `LENS_FOCUS_DISTANCE` — Focus distance
- `LENS_APERTURE` — Aperture (if available)

### Output Settings
- `JPEG_QUALITY` — JPEG compression quality
- `JPEG_ORIENTATION` — Image orientation
- `COLOR_CORRECTION_MODE` — Color correction mode

### Creating a CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// Set auto controls
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// Set JPEG quality
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// Add targets
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// Build the request
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult contains metadata about a completed capture. It includes:

- **Actual settings used** — What the camera actually applied
- **Statistics** — Exposure, focus, and color information
- **Timestamp** — When the capture occurred

### Getting CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // Get actual exposure time
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // Get actual ISO
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // Get focus state
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // Get AE state
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "Exposure: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### Common CaptureResult Keys

| Key | Description |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | Actual exposure time used |
| `SENSOR_SENSITIVITY` | Actual ISO used |
| `CONTROL_AF_STATE` | Autofocus state |
| `CONTROL_AE_STATE` | Auto-exposure state |
| `CONTROL_AWB_STATE` | Auto-white balance state |
| `SCALER_CROP_REGION` | Crop region used |
| `COLOR_CORRECTION_GAINS` | Color correction gains |

## A Complete RAW Capture Example

```kotlin
private fun configureDualCapture() {
    // Create JPEG ImageReader
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // Create RAW ImageReader
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
    
    // Create surfaces
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // Create capture session with all surfaces
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // Add both surfaces as targets
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // Configure controls
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

| Feature | RAW | JPEG |
| --- | --- | --- |
| File size | Large (20-50MB) | Small (2-10MB) |
| Editing flexibility | Maximum | Limited |
| Noise | Preserved | Reduced |
| White balance | Adjustable | Fixed |
| Dynamic range | Full | Compressed |

## Best Practices

1. **Check RAW support** — Always verify before attempting RAW capture
2. **Dual capture** — Capture both JPEG and RAW for flexibility
3. **Close images** — Always call `image.close()` after processing
4. **Handle different formats** — RAW_SENSOR, RAW10, and RAW12 have different byte layouts

## Next Chapter

In the next chapter, we'll summarize what you've learned in Part III and prepare for Part IV: Manual Camera controls.

## Summary

In this chapter, you learned about:

1. **RAW capture** — Capturing unprocessed sensor data for maximum editing flexibility
2. **CaptureRequest** — Configuring camera settings for each capture
3. **CaptureResult** — Getting metadata about completed captures

RAW capture requires FULL or LEVEL_3 hardware level. You can capture both JPEG and RAW simultaneously by adding both surfaces to the capture session.

CaptureRequest allows you to configure autofocus, auto-exposure, white balance, and manual controls like ISO and exposure time. CaptureResult tells you what settings were actually used by the camera.

In Part IV, we'll dive deep into manual camera controls: ISO, exposure, focus, and white balance.