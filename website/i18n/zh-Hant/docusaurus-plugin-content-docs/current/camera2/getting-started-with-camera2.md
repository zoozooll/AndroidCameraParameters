---
sidebar_position: 5
title: "第 5 章：Camera2 入門"
description: 瞭解 CameraManager，它是 Android Camera2 API 的進入點，可讓你列舉攝像頭並存取其特性。
keywords: [CameraManager, Camera2 API, Android 攝像頭, 攝像頭列舉]
---

歡迎來到本系列的程式設計部分。讓我們從基礎開始：CameraManager。

## 簡介

在使用任何攝像頭之前，你需要一種方法來發現和存取它。這就是 **CameraManager** 的作用。

CameraManager 是 Camera2 API 的入口。它是你在任何 Camera2 應用程式中使用的第一個類別。

## 什麼是 CameraManager？

CameraManager 是一個系統服務，負責管理 Android 裝置上的所有攝像頭裝置。將其視為攝像頭的目錄或註冊表。

其主要職責是：
1. **列舉攝像頭** — 列出所有可用的攝像頭
2. **取得攝像頭特性** — 擷取每個攝像頭的詳細資訊
3. **開啟攝像頭** — 建立用於拍攝的 CameraDevice

## 取得 CameraManager

在 Android 中，系統服務是透過 `Context` 取得的。以下是取得 CameraManager 的方法：

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

就是這樣。只需一行程式碼即可存取裝置上的所有攝像頭。

## 權限優先

在使用 CameraManager 之前，你需要請求攝像頭權限。將這些權限加入到你的 `AndroidManifest.xml` 中：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

並在你的 Activity 中請求執行階段權限：

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

存取攝像頭前請務必先檢查權限。

## CameraManager 方法

CameraManager 有三個你將使用的主要方法：

### 1. `getCameraIdList()`

傳回攝像頭 ID 字串陣列。每個 ID 代表一個攝像頭裝置。

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "找到攝像頭：$id")
}
```

這可能會輸出：
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

傳回一個 `CameraCharacteristics` 物件，其中包含有關特定攝像頭的所有詳細資訊。

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics 包含數百個描述攝像頭功能的參數。

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

開啟攝像頭並透過回呼傳回 `CameraDevice`。我們稍後會詳細介紹這一點。

## 重新認識攝像頭 ID

還記得第 4 章提到 Android 會為攝像頭分配數字 ID 嗎？這些 ID 不保證在不同裝置之間甚至重新開機後保持一致。

常見模式：
- **攝像頭 0** — 通常是後置廣角攝像頭
- **攝像頭 1** — 通常是前置攝像頭
- **攝像頭 2** — 通常是超廣角或長焦攝像頭
- 更高的數字 — 額外的攝像頭（微距、深度等）

但**永遠不要假設**攝像頭 ID 的含義。請務必檢查攝像頭特性以確定：
- 鏡頭方向（前置/後置/外置）
- 焦距
- 功能

## 為什麼 CameraManager 很重要

CameraManager 是我們使用 Camera2 進行所有操作的基礎：

1. **探索** — 在使用攝像頭之前，你需要先找到它
2. **資訊** — 在開啟攝像頭之前，你需要了解其功能
3. **存取** — CameraManager 提供了開啟攝像頭裝置的唯一方法

## 簡單範例

讓我們透過一個簡單的範例來整合所有內容：

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
        
        Log.d("CameraDiscovery", "找到 ${cameraIds.size} 個攝像頭")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "前置"
                CameraCharacteristics.LENS_FACING_BACK -> "後置"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "外置"
                else -> "未知"
            }
            
            Log.d("CameraDiscovery", "攝像頭 $cameraId：$lensFacingStr")
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
                Toast.makeText(this, "需要攝像頭權限", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

這個簡單的 Activity 會探索所有攝像頭，並記錄它們的 ID 和鏡頭方向。

## 重點整理

- **CameraManager** 是 Camera2 的進入點
- 使用 `getCameraIdList()` 尋找所有攝像頭
- 使用 `getCameraCharacteristics()` 取得詳細資訊
- 永遠先請求攝像頭權限
- 永遠不要假設攝像頭 ID 的含義 — 請檢查特性

## 下一章

既然你已經了解 CameraManager，是時候編寫你的第一個真正的 Camera2 程式了。在下一章中，我們將：

1. 建立一個簡單的 Android 應用程式
2. 列出所有可用的攝像頭
3. 向使用者顯示攝像頭資訊

你將編寫你的第一個 Camera2 程式碼並看到真實的結果！

## 總結

CameraManager 是 Camera2 的基礎。它提供了對以下內容的存取：
- 攝像頭列舉
- 攝像頭特性
- 攝像頭開啟

透過 CameraManager，你可以發現有哪些可用的攝像頭，並在開啟它們之前了解它們的功能。

在下一章中，我們將編寫我們的第一個 Camera2 程式，列出裝置上的所有攝像頭。
