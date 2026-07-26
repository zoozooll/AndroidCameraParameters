---
sidebar_position: 6
title: "第 6 章：列出相機"
description: 撰寫你的第一個 Camera2 程式，探索並列出 Android 裝置上所有可用的相機。
keywords: [列出相機, CameraManager, 相機列舉, Android Camera2]
---

是時候撰寫你的第一個 Camera2 程式了！讓我們建立一個列出所有相機的應用程式。

## 簡介

在本章中，你將撰寫第一個真正的 Camera2 應用程式。目標很簡單：

> 探索裝置上的所有相機並顯示它們的資訊。

這是一個小而重要的步驟。在使用相機之前，你需要先找到它。

## 建立專案

讓我們先建立一個新的 Android 專案：

1. 開啟 Android Studio
2. 使用「Empty Activity」建立新專案
3. 命名為「Camera2List」
4. 選擇 Kotlin 作為開發語言
5. 將最低 SDK 設定為 API 21（Camera2 是在 API 21 中引入的）

## 加入權限

將相機權限加入 `AndroidManifest.xml`：

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## 版面配置

建立一個顯示相機清單的簡單版面配置。更新 `activity_main.xml`：

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
        android:text="可用相機"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## Activity

現在讓我們撰寫主要的 Activity。這是 Camera2 程式碼所在的地方：

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
                cameraInfoList.add("未找到相機")
            } else {
                cameraInfoList.add("找到 ${cameraIds.size} 部相機：")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "前置"
                        CameraCharacteristics.LENS_FACING_BACK -> "後置"
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
                    
                    cameraInfoList.add("相機 $index (ID: $cameraId)")
                    cameraInfoList.add("  - 鏡頭：$lensFacingStr")
                    cameraInfoList.add("  - 硬體等級：$hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("錯誤：相機權限被拒絕")
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
                Toast.makeText(this, "需要相機權限", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("相機權限被拒絕")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## 程式碼說明

讓我們分解一下發生了什麼：

1. **取得 CameraManager** — 我們取得 CameraManager 系統服務
2. **檢查權限** — 我們檢查是否已授予相機權限
3. **列出相機** — 我們使用 `getCameraIdList()` 取得所有相機 ID
4. **取得特性** — 針對每部相機，我們取得其特性
5. **顯示資訊** — 我們顯示相機 ID、鏡頭方向和硬體等級

## 預期輸出

當你執行應用程式時，你應該會看到類似這樣的內容：

```
找到 3 部相機：

相機 0 (ID: 0)
  - 鏡頭：後置
  - 硬體等級：FULL

相機 1 (ID: 1)
  - 鏡頭：前置
  - 硬體等級：LIMITED

相機 2 (ID: 2)
  - 鏡頭：後置
  - 硬體等級：FULL
```

## 成功！

你剛剛撰寫了第一個 Camera2 程式！這可能看起來很簡單，但這是我們接下來所有工作的基礎。

## 疑難排解

如果你遇到問題：

1. **權限被拒絕** — 確保你已授予相機權限
2. **未找到相機** — 檢查你的裝置是否有相機
3. **SecurityException** — 確保權限已在資訊清單中宣告
4. **API 等級過低** — Camera2 需要 API 21 或更高版本

## 下一步？

既然你可以列出相機，下一步就是更詳細地檢查它們的特性。在下一章中，我們將：

1. 探索 CameraCharacteristics
2. 瞭解鏡頭方向
3. 理解硬體等級
4. 檢查感測器資訊

## 總結

在本章中，你撰寫了第一個 Camera2 程式。該應用程式：

1. 請求相機權限
2. 使用 CameraManager 列舉相機
3. 顯示相機 ID、鏡頭方向和硬體等級

這是建構完整 Camera2 應用程式的第一步。在下一章中，我們將深入探討 CameraCharacteristics，以瞭解每部相機的功能。
