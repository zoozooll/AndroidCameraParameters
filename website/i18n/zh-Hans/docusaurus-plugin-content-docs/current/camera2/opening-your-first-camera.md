---
sidebar_position: 8
title: "第 8 章：打开你的第一个相机"
description: 学习如何使用 CameraManager 打开 CameraDevice，并通过状态回调处理相机生命周期。
keywords: [CameraDevice, openCamera, 相机生命周期, CameraManager]
---

是时候打开你的第一个相机了！让我们来了解 CameraDevice。

## 简介

到目前为止，我们已经学习了如何发现相机并检查它们的特性。现在我们将迈出下一步：**打开相机**。

打开相机后，你就可以访问实际的相机硬件。一旦打开，你就可以创建捕获会话、显示预览和拍摄照片。

## 什么是 CameraDevice？

CameraDevice 代表连接到 Android 设备的单个相机。它提供以下方法：
- 创建捕获会话
- 拍摄静态图片
- 启动和停止预览

你不需要直接创建 CameraDevice。相反，你可以通过调用 CameraManager 的 `openCamera()` 来获取它。

## 打开相机

以下是打开相机的方法：

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // 相机已准备就绪
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // 相机已断开连接
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // 发生相机错误
        camera.close()
    }
}, null)
```

让我们来分解一下。

### StateCallback

CameraDevice 使用回调模式，因为打开相机是异步的。回调有三个主要方法：

#### 1. `onOpened(camera: CameraDevice)`

当相机成功打开时调用。这是你获取 CameraDevice 实例的地方。

#### 2. `onDisconnected(camera: CameraDevice)`

当相机断开连接时调用。如果相机被另一个应用使用或设备关闭，可能会发生这种情况。始终在此回调中关闭相机。

#### 3. `onError(camera: CameraDevice, error: Int)`

当发生错误时调用。常见的错误代码：
- `ERROR_CAMERA_IN_USE` — 相机已被使用
- `ERROR_MAX_CAMERAS_IN_USE` — 打开的相机过多
- `ERROR_CAMERA_DISABLED` — 相机已被禁用
- `ERROR_CAMERA_DEVICE` — 相机硬件错误
- `ERROR_CAMERA_SERVICE` — 相机服务错误

### Handler

第三个参数是 `Handler`。如果你传递 `null`，回调将在调用线程的 looper 上运行。对于 UI 更新，你可能希望传递一个在主线程上运行的 handler。

## 完整示例

让我们创建一个打开相机的 Activity：

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
            Toast.makeText(this, "没有可用的相机", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // 打开第一个相机
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "无效的相机 ID", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "相机打开成功！", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "相机 ${camera.id} 已打开")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "相机已断开连接", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "相机正在使用中"
                ERROR_MAX_CAMERAS_IN_USE -> "打开的相机过多"
                ERROR_CAMERA_DISABLED -> "相机已被禁用"
                ERROR_CAMERA_DEVICE -> "相机硬件错误"
                ERROR_CAMERA_SERVICE -> "相机服务错误"
                else -> "未知错误"
            }
            
            Toast.makeText(this@CameraOpenActivity, "相机错误：$errorMessage", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## 相机生命周期

了解相机生命周期至关重要：

1. **打开** — 调用 `openCamera()` 获取 CameraDevice
2. **使用** — 创建捕获会话，拍摄照片
3. **关闭** — 完成后调用 `close()`
4. **释放** — 相机可供其他应用使用

当 Activity 被销毁时，始终关闭相机以避免资源泄漏。

## 最佳实践

1. **用完即关** — 始终在 `onDestroy()` 中关闭相机
2. **处理错误** — 不要忽略 `onError()` 回调
3. **检查权限** — 打开前始终验证权限
4. **使用 try-catch** — 处理 `SecurityException` 和 `IllegalArgumentException`
5. **不要持有引用** — 关闭时释放 CameraDevice 引用

## 常见问题

### 相机正在使用中
- 确保没有其他应用正在使用相机
- 检查你是否正确关闭了相机

### 权限被拒绝
- 验证清单中的权限
- 检查运行时权限是否已授予

### 找不到相机 ID
- 始终从 `getCameraIdList()` 获取相机 ID
- 不要硬编码相机 ID

## 下一章

既然你可以打开相机了，下一步就是显示预览。在下一章中，我们将：

1. 了解 TextureView
2. 创建用于预览的 Surface
3. 创建 CameraCaptureSession
4. 在屏幕上显示相机预览

## 总结

打开相机是捕获图像的第一步：

1. 使用 `CameraManager.openCamera()` 获取 CameraDevice
2. 处理 StateCallback 的 `onOpened()`、`onDisconnected()` 和 `onError()`
3. 用完后始终关闭相机
4. 遵循相机生命周期：打开 → 使用 → 关闭 → 释放

在下一章中，我们将创建相机预览，让你看到相机所看到的内容。
