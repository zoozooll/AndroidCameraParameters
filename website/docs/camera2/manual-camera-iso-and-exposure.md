---
sidebar_position: 12
title: "Chapter 12: Manual Camera - ISO and Exposure"
description: Learn how to manually control ISO and exposure time for professional photography with Camera2.
keywords: [ISO, exposure time, manual camera, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

Manual control is where the real power of Camera2 shines. Let's learn about ISO and exposure.

## Introduction

So far, we've been using automatic controls. Now we'll take full control over:

1. **ISO** — Sensor sensitivity to light
2. **Exposure time** — How long the sensor collects light

These two settings directly affect image brightness and quality.

## What is ISO?

ISO measures the sensor's sensitivity to light. Lower ISO means:
- Less sensitive to light
- Lower noise
- Better image quality

Higher ISO means:
- More sensitive to light
- Higher noise (grain)
- Lower image quality

Common ISO values: 100, 200, 400, 800, 1600, 3200, 6400

## What is Exposure Time?

Exposure time (also called shutter speed) is how long the sensor collects light. Shorter exposure means:
- Less light captured
- Freezes motion
- Faster action

Longer exposure means:
- More light captured
- Motion blur
- Better low-light performance

Exposure time is measured in seconds or fractions of a second:
- 1/1000s — Fast action
- 1/125s — Normal
- 1/30s — Slow
- 1s — Long exposure

## Manual Control Requirements

To use manual controls, your camera must have:
1. **FULL** or **LEVEL_3** hardware level
2. **MANUAL_SENSOR** capability

Check CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## Getting Supported Ranges

Before setting manual values, check what the camera supports:

```kotlin
// Get ISO range
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// Get exposure range
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## Setting Manual ISO and Exposure

To use manual controls, you need to:
1. Disable auto-exposure (AE)
2. Set manual ISO
3. Set manual exposure time

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Disable AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// Set manual ISO
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// Set manual exposure time (in nanoseconds)
// 1/125s = 8,000,000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// Add target
captureRequestBuilder.addTarget(surface)

// Start preview with manual controls
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## The Exposure Triangle

ISO, exposure time, and aperture form the "exposure triangle":

- **ISO** — Sensitivity to light
- **Exposure time** — Duration of light capture
- **Aperture** — Amount of light entering (rarely adjustable on phones)

Changing one affects the others. For example:
- If you increase ISO, you can use a faster shutter speed
- If you decrease exposure time, you may need to increase ISO

## A Complete Manual Control Example

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO: 100-3200 in steps of 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // Exposure: 1/1000s to 1/10s
        exposureSeekBar.progress = 5
        
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Convert progress to exposure time (in nanoseconds)
                // 0: 1/1000s = 1,000,000ns
                // 9: 1/10s = 100,000,000ns
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // Disable AE for manual control
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // Set manual ISO
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // Set manual exposure time
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Keep AWB on
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (rest of the camera setup code)
    
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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
    
    // ... (camera opening, session creation, etc.)
}
```

## The Layout

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="ISO:"/>
            
            <SeekBar
                android:id="@+id/isoSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/isoValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="100"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="16dp">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="Exp:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60s"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## Best Practices

1. **Check support** — Always verify manual sensor support
2. **Start with AE** — Let auto-exposure set initial values, then switch to manual
3. **Avoid extreme ISO** — High ISO introduces noise
4. **Use shortest exposure** — Avoid motion blur when possible
5. **Monitor histogram** — Use CaptureResult to check exposure

## Next Chapter

In the next chapter, we'll learn about manual focus and white balance control.

## Summary

Manual control of ISO and exposure gives you professional-level photography:

1. **ISO** — Controls sensor sensitivity (100-3200+ range)
2. **Exposure time** — Controls how long light is collected (in nanoseconds)
3. **Disable AE** — Must turn off auto-exposure for manual control
4. **Check ranges** — Always verify supported ISO and exposure ranges

The exposure triangle (ISO, exposure time, aperture) determines image brightness and quality. In the next chapter, we'll explore manual focus and white balance.