---
sidebar_position: 5
title: "第5章：开始使用Camera2"
description: 学习CameraManager，这是Android Camera2 API的入口点，允许你枚举相机并访问它们的特性。
keywords: [CameraManager, Camera2 API, Android相机, 相机枚举]
---

欢迎来到本系列的编码部分。让我们从基础开始：CameraManager。

## 简介

在使用任何相机之前，你需要一种发现和访问它的方法。这就是**CameraManager**的用途。

CameraManager是Camera2 API的网关。它是你在任何Camera2应用中使用的第一个类。

## CameraManager是什么？

CameraManager是一个管理Android设备上所有相机设备的系统服务。可以将其视为相机的目录或注册表。

它的主要职责是：
1. **枚举相机** — 列出所有可用的相机
2. **获取相机特性** — 检索每个相机的详细信息
3. **打开相机** — 创建一个用于捕捉的CameraDevice

## 获取CameraManager

在Android中，系统服务通过`Context`获得。以下是获取CameraManager的方法：

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

就这样。一行代码即可访问设备上的所有相机。

## 首先需要权限

在使用CameraManager之前，你需要请求相机权限。在`AndroidManifest.xml`中添加这些：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

并在你的活动中请求运行时权限：

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

始终在访问相机之前检查权限。

## CameraManager方法

CameraManager有三个主要方法你会用到：

### 1. `getCameraIdList()`

返回相机ID字符串数组。每个ID代表一个相机设备。

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Found camera: $id")
}
```

这可能输出：
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

返回包含特定相机所有详细信息的`CameraCharacteristics`对象。

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics包含数百个描述相机功能的参数。

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

打开相机并通过回调返回`CameraDevice`。我们稍后将详细介绍这一点。

## 相机ID回顾

从第4章记得，Android为相机分配数字ID。ID不保证在设备之间或甚至在重启之间保持一致。

常见模式：
- **Camera 0** — 通常是后置广角相机
- **Camera 1** — 通常是前置相机
- **Camera 2** — 通常是超广角或长焦相机
- 更高的数字 — 额外的相机（微距、深度等）

但**永远不要假设**相机ID的含义。始终检查相机特性以确定：
- 镜头朝向（前置/后置/外部）
- 焦距
- 功能

## 为什么CameraManager很重要

CameraManager是我们使用Camera2做一切事情的基础：

1. **发现** — 在使用相机之前，你需要找到它
2. **信息** — 在打开相机之前，你需要知道它的功能
3. **访问** — CameraManager提供打开相机设备的唯一方法

## 一个简单的例子

让我们把所有内容放在一个简单的例子中：

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

这个简单的活动发现所有相机并记录它们的ID和镜头朝向。

## 关键要点

- **CameraManager**是Camera2的入口点
- 使用`getCameraIdList()`查找所有相机
- 使用`getCameraCharacteristics()`获取详细信息
- 始终首先请求相机权限
- 永远不要假设相机ID的含义 — 检查特性

## 下一章

现在你理解了CameraManager，是时候编写你的第一个真正的Camera2程序了。在下一章中，我们将：

1. 创建一个简单的Android应用
2. 列出所有可用的相机
3. 向用户显示相机信息

你将编写你的第一个Camera2代码并看到真实的结果！

## 总结

CameraManager是Camera2的基础。它提供对以下内容的访问：
- 相机枚举
- 相机特性
- 相机打开

有了CameraManager，你可以发现有哪些相机可用，并在打开它们之前了解它们的功能。

在下一章中，我们将编写我们的第一个Camera2程序，列出设备上的所有相机。