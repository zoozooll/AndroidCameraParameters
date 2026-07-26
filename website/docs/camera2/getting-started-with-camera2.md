---
sidebar_position: 5
title: "Chapter 5: Getting Started with Camera2"
description: Learn about CameraManager, the entry point to the Android Camera2 API that allows you to enumerate cameras and access their characteristics.
keywords: [CameraManager, Camera2 API, Android camera, camera enumeration]
---

Welcome to the coding part of this series. Let's start with the foundation: CameraManager.

## Introduction

Before you can use any camera, you need a way to discover and access it. That's where **CameraManager** comes in.

CameraManager is the gateway to the Camera2 API. It's the first class you'll use in any Camera2 application.

## What is CameraManager?

CameraManager is a system service that manages all camera devices on an Android device. Think of it as a directory or registry of cameras.

Its main responsibilities are:
1. **Enumerate cameras** — List all available cameras
2. **Get camera characteristics** — Retrieve detailed information about each camera
3. **Open cameras** — Create a CameraDevice for capturing

## Getting CameraManager

In Android, system services are obtained through the `Context`. Here's how to get CameraManager:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

That's it. One line of code to get access to all cameras on the device.

## Permissions First

Before using CameraManager, you need to request camera permissions. Add these to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

And request runtime permission in your activity:

```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED
) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        CAMERA_PERMISSION_REQUEST
    )
}
```

Always check permissions before accessing the camera.

## CameraManager Methods

CameraManager has three main methods you'll use:

### 1. `getCameraIdList()`

Returns an array of camera ID strings. Each ID represents a camera device.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Found camera: $id")
}
```

This might output:
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

Returns a `CameraCharacteristics` object containing all the details about a specific camera.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics contains hundreds of parameters describing the camera's capabilities.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

Opens a camera and returns a `CameraDevice` through the callback. We'll cover this in detail later.

## Camera IDs Revisited

Remember from Chapter 4 that Android assigns numeric IDs to cameras. The IDs are not guaranteed to be consistent across devices or even across reboots.

Common patterns:
- **Camera 0** — Typically the rear wide camera
- **Camera 1** — Often the front camera
- **Camera 2** — Usually an ultra-wide or telephoto camera
- Higher numbers — Additional cameras (macro, depth, etc.)

But **never assume** the meaning of a camera ID. Always check the camera characteristics to determine:
- Lens facing (front/rear/external)
- Focal length
- Capabilities

## Why CameraManager is Important

CameraManager is the foundation for everything we'll do with Camera2:

1. **Discovery** — Before using a camera, you need to find it
2. **Information** — Before opening a camera, you need to know its capabilities
3. **Access** — CameraManager provides the only way to open a camera device

## A Simple Example

Let's put it all together in a simple example:

```kotlin
class CameraDiscoveryActivity : AppCompatActivity() {
    
    private lateinit var cameraManager: CameraManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            discoverCameras()
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
    
    private fun discoverCameras() {
        val cameraIds = cameraManager.cameraIdList
        
        Log.d("CameraDiscovery", "Found ${cameraIds.size} camera(s)")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Back"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }
            
            Log.d("CameraDiscovery", "Camera $cameraId: $lensFacingStr")
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
                discoverCameras()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

This simple activity discovers all cameras and logs their IDs and lens facing directions.

## Key Takeaways

- **CameraManager** is the entry point to Camera2
- Use `getCameraIdList()` to find all cameras
- Use `getCameraCharacteristics()` to get detailed information
- Always request camera permissions first
- Never assume camera ID meanings — check characteristics

## Next Chapter

Now that you understand CameraManager, it's time to write your first real Camera2 program. In the next chapter, we'll:

1. Create a simple Android app
2. List all available cameras
3. Display camera information to the user

You'll write your first Camera2 code and see real results!

## Summary

CameraManager is the foundation of Camera2. It provides access to:
- Camera enumeration
- Camera characteristics
- Camera opening

With CameraManager, you can discover what cameras are available and learn about their capabilities before opening them.

In the next chapter, we'll write our first Camera2 program that lists all cameras on the device.