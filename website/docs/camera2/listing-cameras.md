---
sidebar_position: 6
title: "Chapter 6: Listing Cameras"
description: Write your first Camera2 program that discovers and lists all available cameras on an Android device.
keywords: [list cameras, CameraManager, camera enumeration, Android Camera2]
---

It's time to write your first Camera2 program! Let's create an app that lists all cameras.

## Introduction

In this chapter, you'll write your first real Camera2 application. The goal is simple:

> Discover all cameras on the device and display their information.

This is a small but important step. Before you can use a camera, you need to find it.

## Creating the Project

Let's start by creating a new Android project:

1. Open Android Studio
2. Create a new project with "Empty Activity"
3. Name it "Camera2List"
4. Select Kotlin as the language
5. Set minimum SDK to API 21 (Camera2 was introduced in API 21)

## Adding Permissions

Add camera permission to `AndroidManifest.xml`:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## The Layout

Create a simple layout that displays a list of cameras. Update `activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Available Cameras"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## The Activity

Now let's write the main activity. This is where the Camera2 code goes:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraListView: ListView
    private val cameraInfoList = mutableListOf<String>()

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraListView = findViewById(R.id.cameraListView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (hasCameraPermission()) {
            listCameras()
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

    private fun listCameras() {
        cameraInfoList.clear()
        
        try {
            val cameraIds = cameraManager.cameraIdList
            
            if (cameraIds.isEmpty()) {
                cameraInfoList.add("No cameras found")
            } else {
                cameraInfoList.add("Found ${cameraIds.size} camera(s):")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                        CameraCharacteristics.LENS_FACING_BACK -> "Back"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                        else -> "Unknown"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "Unknown"
                    }
                    
                    cameraInfoList.add("Camera $index (ID: $cameraId)")
                    cameraInfoList.add("  - Lens: $lensFacingStr")
                    cameraInfoList.add("  - Hardware Level: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("Error: Camera permission denied")
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
                listCameras()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("Camera permission denied")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## What This Code Does

Let's break down what's happening:

1. **Get CameraManager** — We obtain the CameraManager system service
2. **Check Permissions** — We check if camera permission is granted
3. **List Cameras** — We use `getCameraIdList()` to get all camera IDs
4. **Get Characteristics** — For each camera, we get its characteristics
5. **Display Information** — We show the camera ID, lens facing, and hardware level

## Expected Output

When you run the app, you should see something like:

```
Found 3 camera(s):

Camera 0 (ID: 0)
  - Lens: Back
  - Hardware Level: FULL

Camera 1 (ID: 1)
  - Lens: Front
  - Hardware Level: LIMITED

Camera 2 (ID: 2)
  - Lens: Back
  - Hardware Level: FULL
```

## Success!

You've just written your first Camera2 program! It may seem simple, but this is the foundation for everything we'll do next.

## Troubleshooting

If you encounter issues:

1. **Permission denied** — Make sure you granted camera permission
2. **No cameras found** — Check if your device has a camera
3. **SecurityException** — Ensure permissions are declared in manifest
4. **API level too low** — Camera2 requires API 21 or higher

## What's Next?

Now that you can list cameras, the next step is to examine their characteristics in more detail. In the next chapter, we'll:

1. Explore CameraCharacteristics
2. Learn about lens facing
3. Understand hardware levels
4. Check sensor information

## Summary

In this chapter, you wrote your first Camera2 program. The app:

1. Requests camera permissions
2. Uses CameraManager to enumerate cameras
3. Displays camera ID, lens facing, and hardware level

This is the first step toward building full Camera2 applications. In the next chapter, we'll dive deeper into CameraCharacteristics to understand what each camera can do.