---
sidebar_position: 9
title: "Chapter 9: Showing Camera Preview"
description: Learn how to display camera preview using TextureView, Surface, and CameraCaptureSession in Camera2.
keywords: [camera preview, TextureView, Surface, CameraCaptureSession, Camera2]
---

Finally, you'll see the camera preview! Let's connect everything together.

## Introduction

Opening a camera is great, but you can't see anything yet. To display what the camera sees, you need to:

1. Create a TextureView to display the preview
2. Get a Surface from the TextureView
3. Create a CameraCaptureSession
4. Start the preview

This is where the pieces come together.

## TextureView

TextureView is a view that can display a `SurfaceTexture`. It's perfect for showing camera previews because:
- It can be transformed (scaled, rotated)
- It supports hardware acceleration
- It works well with animations and transitions

Add a TextureView to your layout:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</FrameLayout>
```

## Surface

A Surface is a buffer that can receive image data. To display a camera preview:
1. Get the SurfaceTexture from the TextureView
2. Create a Surface from the SurfaceTexture
3. Pass the Surface to the CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession manages the capture process. It connects the camera device to one or more Surfaces.

To create a session:
1. Prepare a list of Surfaces (for preview, photo capture, etc.)
2. Call `createCaptureSession()` on CameraDevice
3. Handle the callback

## A Complete Preview Example

Let's create an activity that shows a camera preview:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Collections

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            // Handle size changes if needed
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Called when the preview is updated
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) {
            Toast.makeText(this, "No cameras available", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // Get preview sizes
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // Choose a preview size
        previewSize = previewSizes?.get(0) // Use the first available size

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
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
            Toast.makeText(this@CameraPreviewActivity, "Camera error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val surface = Surface(texture)
        val surfaces = listOf(surface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@CameraPreviewActivity, "Session configuration failed", Toast.LENGTH_SHORT).show()
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.stopRepeating()
        captureSession?.close()
        cameraDevice?.close()
    }
}
```

## How It Works

Let's trace the flow:

1. **TextureView available** — `onSurfaceTextureAvailable()` is called
2. **Open camera** — We get a CameraDevice
3. **Create capture session** — Connect the camera to the Surface
4. **Start preview** — Send a repeating capture request

## CaptureRequest

CaptureRequest defines what the camera should capture:
- `TEMPLATE_PREVIEW` — For preview mode
- `TEMPLATE_STILL_CAPTURE` — For still photos
- `TEMPLATE_RECORD` — For video recording
- `TEMPLATE_VIDEO_SNAPSHOT` — For snapshot during video

## The Preview Loop

When you call `setRepeatingRequest()`, the camera continuously sends frames to the Surface. This creates the live preview.

## Important Notes

1. **Surface must be available** — Wait for `onSurfaceTextureAvailable()` before opening the camera
2. **Close resources** — Always close the capture session and camera device
3. **Handle orientation** — The preview may need rotation depending on device orientation
4. **Size matters** — Choose a preview size that matches your TextureView dimensions

## Success!

When you run this app, you should see a live camera preview on your screen. Congratulations! You've built your first Camera2 preview app.

## Next Chapter

Now that you can display a preview, the next step is to capture photos. In Part III, we'll learn about:

1. ImageReader for capturing photos
2. JPEG and RAW capture
3. CaptureRequest and CaptureResult

## Summary

Displaying a camera preview involves:

1. **TextureView** — The UI component for displaying the preview
2. **Surface** — The buffer that receives camera frames
3. **CameraCaptureSession** — Manages the capture process
4. **CaptureRequest** — Defines what to capture
5. **setRepeatingRequest()** — Starts the continuous preview loop

You've now completed Part II of this series. You can:
- Discover cameras
- Examine camera characteristics
- Open a camera
- Display a preview

In Part III, we'll learn how to capture photos with Camera2.