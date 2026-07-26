---
sidebar_position: 14
title: "Chapter 14: Professional Features - High-Speed Video"
description: Learn how to capture high-speed video and work with multi-camera setups in Camera2.
keywords: [high-speed video, multi-camera, Camera2, constrained high speed, logical camera]
---

Professional features open up new creative possibilities. Let's explore high-speed video and multi-camera.

## Introduction

Camera2 supports many professional features beyond basic photo capture. In this chapter, we'll learn about:

1. **High-speed video** — Capturing slow-motion video
2. **Multi-camera** — Working with logical and physical cameras

## High-Speed Video

High-speed video allows you to capture video at frame rates higher than the standard 30fps:
- 120fps — Smooth slow motion
- 240fps — Standard slow motion  
- 480fps — Extreme slow motion
- 960fps — Super slow motion

### Requirements

To capture high-speed video, your camera must:
1. Support `CONSTRAINED_HIGH_SPEED_VIDEO` capability
2. Have the right hardware level

Check CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### High-Speed Video Sizes

High-speed video uses different resolutions than standard video:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### Capturing High-Speed Video

High-speed video requires a special capture session:

```kotlin
// Create a high-speed capture session
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "High-speed session failed", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### Starting High-Speed Preview

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // Get supported high-speed fps ranges
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // Find a high-speed range (e.g., 120fps)
    val highSpeedFpsRange = fpsRanges?.find { it.upper >= 120 }
    
    captureRequestBuilder?.apply {
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange)
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.setRepeatingRequest(builder.build(), null, null)
    }
}
```

## Multi-Camera

Modern phones have multiple cameras. Camera2 treats them as:
- **Physical cameras** — Individual camera sensors
- **Logical cameras** — Combinations of physical cameras

### Logical vs Physical Cameras

| Type | Description |
| --- | --- |
| **Physical** | Single camera sensor (wide, telephoto, ultra-wide) |
| **Logical** | Virtual camera that combines multiple physical cameras |

### Identifying Camera Types

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Check if it's a logical camera
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// Get physical camera IDs (for logical cameras)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### Switching Between Cameras

To switch cameras, you need to:
1. Close the current camera
2. Open the new camera
3. Create a new capture session

```kotlin
private fun switchCamera(newCameraId: String) {
    // Close current camera
    captureSession?.close()
    cameraDevice?.close()
    
    // Open new camera
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### Concurrent Camera

Some devices support opening multiple cameras simultaneously:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## Camera Extension

Camera extension allows you to use manufacturer-specific camera features:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// Get available extensions
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

Common extensions:
- `EXTENSION_BOKEH` — Portrait mode
- `EXTENSION_HDR` — HDR mode
- `EXTENSION_NIGHT` — Night mode
- `EXTENSION_AUTO` — Automatic mode

## A Multi-Camera Example

```kotlin
class MultiCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_camera)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupCameraSelector()
    }

    private fun setupCameraSelector() {
        val cameraIds = cameraManager.cameraIdList
        val spinner = findViewById<Spinner>(R.id.cameraSpinner)
        
        val cameraNames = cameraIds.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "Back Camera ($id)"
                else -> "Camera $id"
            }
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newCameraId = cameraIds[position]
                if (newCameraId != currentCameraId) {
                    switchCamera(newCameraId)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun switchCamera(newCameraId: String) {
        captureSession?.close()
        cameraDevice?.close()
        currentCameraId = newCameraId
        openCamera()
    }

    private fun openCamera() {
        try {
            cameraManager.openCamera(currentCameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "Session failed", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun startPreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(surface)
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
        }
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }
}
```

## Best Practices

1. **Check capabilities** — Always verify before using professional features
2. **Handle transitions** — Smoothly close/open when switching cameras
3. **Manage resources** — High-speed video consumes more resources
4. **Fall back gracefully** — Provide alternatives when features aren't available

## Next Chapter

In the final chapter, we'll explore the CameraCharacteristics Encyclopedia — a deep dive into the most important camera parameters.

## Summary

Professional features expand your creative possibilities:

1. **High-speed video** — Capture slow-motion at 120-960fps
2. **Multi-camera** — Work with logical and physical cameras
3. **Camera extension** — Use manufacturer-specific features
4. **Concurrent camera** — Open multiple cameras simultaneously

In the next chapter, we'll dive deep into CameraCharacteristics — the encyclopedia of camera parameters.