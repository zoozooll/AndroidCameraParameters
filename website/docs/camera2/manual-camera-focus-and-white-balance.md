---
sidebar_position: 13
title: "Chapter 13: Manual Camera - Focus and White Balance"
description: Learn how to manually control focus distance and white balance for professional photography with Camera2.
keywords: [focus, white balance, manual camera, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

Complete manual control with focus and white balance.

## Introduction

In the previous chapter, you learned to control ISO and exposure. Now we'll add:

1. **Focus** — Manual focus distance control
2. **White balance** — Manual color temperature control

With these, you have complete creative control over your photos.

## Manual Focus

Focus determines which part of the scene is sharp. Manual focus allows you to:
- Focus on specific objects
- Create intentional blur (bokeh)
- Ensure critical focus in macro photography

### Focus Modes

Camera2 supports several focus modes:

| Mode | Description |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Manual focus |
| `CONTROL_AF_MODE_AUTO` | Single auto-focus |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Continuous autofocus for photos |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Continuous autofocus for video |
| `CONTROL_AF_MODE_MACRO` | Macro focus |

### Focus Distance

Focus distance is measured in diopters (1/meter). A value of 0 means infinity.

```kotlin
// Get focus distance range
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### Setting Manual Focus

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Disable AF for manual focus
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// Set manual focus distance (in diopters)
// 0.0 = infinity
// 1.0 = 1 meter
// 2.0 = 0.5 meters
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Focus Regions

You can also specify AF regions for selective autofocus:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // Center X in sensor coordinates (0-1000)
        centerY,    // Center Y in sensor coordinates (0-1000)
        width,      // Region width
        height,     // Region height
        weight      // Priority (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## Manual White Balance

White balance (WB) adjusts the color temperature of the image. Different light sources have different color temperatures:

- **Daylight** — ~5500K (blueish)
- **Cloudy** — ~6500K (cooler)
- **Tungsten** — ~2800K (warm/yellow)
- **Fluorescent** — ~4000K (greenish)

### White Balance Modes

| Mode | Description |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Manual white balance |
| `CONTROL_AWB_MODE_AUTO` | Automatic white balance |
| `CONTROL_AWB_MODE_INCANDESCENT` | Tungsten lighting |
| `CONTROL_AWB_MODE_FLUORESCENT` | Fluorescent lighting |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Warm fluorescent |
| `CONTROL_AWB_MODE_DAYLIGHT` | Daylight |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Cloudy |

### Setting Manual White Balance

To set manual white balance, you need to set color correction gains:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Disable AWB for manual control
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// Set color correction gains (R, G, B)
// Values are normalized (1.0 = no correction)
// Higher values make that color more prominent
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Color Temperature

You can also set white balance using color temperature:

```kotlin
// Get supported color temperature range
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// Set color temperature (in Kelvin)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // Daylight
```

## A Complete Manual Camera Example

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // Manual controls
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // Infinity
    private var currentWhiteBalance = 5500 // Daylight

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // ISO control
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Exposure control
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Focus control
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Convert progress (0-100) to focus distance (0.0 to 2.0 diopters)
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // White balance control
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (tungsten) to 6500K (cloudy)
                currentWhiteBalance = 2800 + (progress * 37)
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
            
            // Manual exposure
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Manual focus
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // Manual white balance
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (camera setup code)
}
```

## Best Practices

1. **Check support** — Not all cameras support manual focus or WB
2. **Start automatic** — Let auto-controls establish a baseline
3. **Use focus peaking** — Add visual feedback for focus accuracy
4. **Calibrate WB** — Use a gray card for accurate white balance
5. **Combine controls** — Manual settings work best together

## Next Chapter

In the next chapter, we'll explore professional features like high-speed video and multi-camera.

## Summary

Manual control of focus and white balance completes your camera toolkit:

1. **Focus** — Control what's sharp in the image
2. **White balance** — Control color temperature
3. **Manual modes** — Disable AF/AWB and set values directly
4. **Focus regions** — Target specific areas for autofocus

With ISO, exposure, focus, and white balance under your control, you can create professional-quality photos. In Part V, we'll explore advanced features like high-speed video and multi-camera support.