---
sidebar_position: 8
title: "Chapter 8: Opening Your First Camera"
description: Learn how to open a CameraDevice using CameraManager and handle the camera lifecycle with state callbacks.
keywords: [CameraDevice, openCamera, camera lifecycle, CameraManager]
---

It's time to open your first camera! Let's learn about CameraDevice.

## Introduction

So far, we've learned how to discover cameras and examine their characteristics. Now we'll take the next step: **opening a camera**.

Opening a camera gives you access to the actual camera hardware. Once opened, you can create capture sessions, display previews, and capture photos.

## What is CameraDevice?

CameraDevice represents a single camera connected to the Android device. It provides methods to:
- Create capture sessions
- Capture still images
- Start and stop preview

You don't create CameraDevice directly. Instead, you get it from CameraManager by calling `openCamera()`.

## Opening a Camera

Here's how to open a camera:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // Camera is ready to use
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // Camera was disconnected
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // Camera error occurred
        camera.close()
    }
}, null)
```

Let's break this down.

### The StateCallback

CameraDevice uses a callback pattern because opening a camera is asynchronous. The callback has three main methods:

#### 1. `onOpened(camera: CameraDevice)`

Called when the camera is successfully opened. This is where you get your CameraDevice instance.

#### 2. `onDisconnected(camera: CameraDevice)`

Called when the camera is disconnected. This can happen if the camera is used by another app or if the device is shut down. Always close the camera in this callback.

#### 3. `onError(camera: CameraDevice, error: Int)`

Called when an error occurs. Common error codes:
- `ERROR_CAMERA_IN_USE` — Camera is already in use
- `ERROR_MAX_CAMERAS_IN_USE` — Too many cameras open
- `ERROR_CAMERA_DISABLED` — Camera is disabled
- `ERROR_CAMERA_DEVICE` — Camera hardware error
- `ERROR_CAMERA_SERVICE` — Camera service error

### The Handler

The third parameter is a `Handler`. If you pass `null`, the callback will run on the calling thread's looper. For UI updates, you might want to pass a handler that runs on the main thread.

## A Complete Example

Let's create an activity that opens a camera:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraOpenActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            openCamera()
        } else {
            requestCameraPermission()
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
        
        val cameraId = cameraIds[0] // Open the first camera
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Invalid camera ID", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "Camera opened successfully!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "Camera ${camera.id} opened")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "Camera disconnected", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "Camera is in use"
                ERROR_MAX_CAMERAS_IN_USE -> "Too many cameras open"
                ERROR_CAMERA_DISABLED -> "Camera is disabled"
                ERROR_CAMERA_DEVICE -> "Camera hardware error"
                ERROR_CAMERA_SERVICE -> "Camera service error"
                else -> "Unknown error"
            }
            
            Toast.makeText(this@CameraOpenActivity, "Camera error: $errorMessage", Toast.LENGTH_SHORT).show()
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
        cameraDevice?.close()
    }
}
```

## The Camera Lifecycle

Understanding the camera lifecycle is crucial:

1. **Open** — Call `openCamera()` to get a CameraDevice
2. **Use** — Create capture sessions, capture photos
3. **Close** — Call `close()` when done
4. **Release** — The camera is available for other apps

Always close the camera when your activity is destroyed to avoid resource leaks.

## Best Practices

1. **Close when done** — Always close the camera in `onDestroy()`
2. **Handle errors** — Don't ignore `onError()` callbacks
3. **Check permissions** — Always verify permissions before opening
4. **Use try-catch** — Handle `SecurityException` and `IllegalArgumentException`
5. **Don't hold references** — Release the CameraDevice reference when closed

## Common Issues

### Camera is in use
- Make sure no other app is using the camera
- Check that you're closing the camera properly

### Permission denied
- Verify permissions in manifest
- Check runtime permission is granted

### Camera ID not found
- Always get camera IDs from `getCameraIdList()`
- Don't hardcode camera IDs

## Next Chapter

Now that you can open a camera, the next step is to display a preview. In the next chapter, we'll:

1. Learn about TextureView
2. Create a Surface for preview
3. Create a CameraCaptureSession
4. Display the camera preview on screen

## Summary

Opening a camera is the first step toward capturing images:

1. Use `CameraManager.openCamera()` to get a CameraDevice
2. Handle the StateCallback for `onOpened()`, `onDisconnected()`, and `onError()`
3. Always close the camera when done
4. Follow the camera lifecycle: open → use → close → release

In the next chapter, we'll create a camera preview so you can see what the camera sees.