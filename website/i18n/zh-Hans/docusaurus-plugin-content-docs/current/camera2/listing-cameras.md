---
sidebar_position: 6
title: "第6章：列出相机"
description: 编写你的第一个Camera2程序，发现并列出Android设备上所有可用的相机。
keywords: [列出相机, CameraManager, 相机枚举, Android Camera2]
---

是时候编写你的第一个Camera2程序了！让我们创建一个列出所有相机的应用。

## 简介

在本章中，你将编写第一个真正的Camera2应用程序。目标很简单：

> 发现设备上的所有相机并显示它们的信息。

这是一个小而重要的步骤。在使用相机之前，你需要先找到它。

## 创建项目

让我们从创建一个新的Android项目开始：

1. 打开Android Studio
2. 使用"Empty Activity"创建新项目
3. 将其命名为"Camera2List"
4. 选择Kotlin作为语言
5. 将最低SDK设置为API 21（Camera2在API 21中引入）

## 添加权限

将相机权限添加到`AndroidManifest.xml`：

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## 布局

创建一个显示相机列表的简单布局。更新`activity_main.xml`：

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
        android:text="可用相机"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## 活动

现在让我们编写主活动。这是Camera2代码的位置：

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
                cameraInfoList.add("未找到相机")
            } else {
                cameraInfoList.add("找到 ${cameraIds.size} 个相机：")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "前置"
                        CameraCharacteristics.LENS_FACING_BACK -> "后置"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "外接"
                        else -> "未知"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "未知"
                    }
                    
                    cameraInfoList.add("相机 $index (ID: $cameraId)")
                    cameraInfoList.add("  - 镜头：$lensFacingStr")
                    cameraInfoList.add("  - 硬件级别：$hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("错误：相机权限被拒绝")
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
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("相机权限被拒绝")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## 代码做了什么

让我们分析一下发生了什么：

1. **获取CameraManager** — 我们获取CameraManager系统服务
2. **检查权限** — 我们检查相机权限是否已授予
3. **列出相机** — 我们使用`getCameraIdList()`获取所有相机ID
4. **获取特性** — 对于每个相机，我们获取其特性
5. **显示信息** — 我们显示相机ID、镜头朝向和硬件级别

## 预期输出

当你运行应用时，你应该看到类似这样的内容：

```
找到 3 个相机：

相机 0 (ID: 0)
  - 镜头：后置
  - 硬件级别：FULL

相机 1 (ID: 1)
  - 镜头：前置
  - 硬件级别：LIMITED

相机 2 (ID: 2)
  - 镜头：后置
  - 硬件级别：FULL
```

## 成功！

你刚刚编写了第一个Camera2程序！这可能看起来很简单，但这是我们接下来要做的一切的基础。

## 故障排除

如果遇到问题：

1. **权限被拒绝** — 确保你已授予相机权限
2. **未找到相机** — 检查你的设备是否有相机
3. **SecurityException** — 确保权限在清单中声明
4. **API级别太低** — Camera2需要API 21或更高版本

## 下一步是什么？

现在你可以列出相机了，下一步是更详细地检查它们的特性。在下一章中，我们将：

1. 探索CameraCharacteristics
2. 了解镜头朝向
3. 理解硬件级别
4. 检查传感器信息

## 总结

在本章中，你编写了第一个Camera2程序。该应用：

1. 请求相机权限
2. 使用CameraManager枚举相机
3. 显示相机ID、镜头朝向和硬件级别

这是构建完整Camera2应用的第一步。在下一章中，我们将更深入地研究CameraCharacteristics，以了解每个相机能做什么。
