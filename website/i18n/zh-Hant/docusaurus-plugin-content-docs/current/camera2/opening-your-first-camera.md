---
sidebar_position: 8
title: "第 8 章：打開你的第一個攝像頭"
description: 學習如何使用 CameraManager 打開 CameraDevice，並透過狀態回調處理攝像頭生命週期。
keywords: [CameraDevice, openCamera, 攝像頭生命週期, CameraManager]
---

是時候打開你的第一個攝像頭了！讓我們來瞭解 CameraDevice。

## 簡介

到目前為止，我們已經學習瞭如何發現攝像頭和檢查它們的特性。現在我們將邁出下一步：**打開攝像頭**。

打開攝像頭讓你可以存取實際的攝像頭硬體。打開後，你可以建立捕獲會話、顯示預覽和拍攝照片。

## 什麼是 CameraDevice？

CameraDevice 代表連接到 Android 設備的單個攝像頭。它提供的方法可以：
- 建立捕獲會話
- 拍攝靜止圖像
- 啟動和停止預覽

你不需要直接建立 CameraDevice。相反，你可以透過呼叫 CameraManager 的 `openCamera()` 來獲取它。

## 打開攝像頭

以下是打開攝像頭的方法：

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // 攝像頭已準備就緒
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // 攝像頭已斷開連接
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // 發生攝像頭錯誤
        camera.close()
    }
}, null)
```

讓我們來分解一下。

### StateCallback

CameraDevice 使用回調模式，因為打開攝像頭是非同步的。回調有三個主要方法：

#### 1. `onOpened(camera: CameraDevice)`

當攝像頭成功打開時呼叫。這是你獲取 CameraDevice 實例的地方。

#### 2. `onDisconnected(camera: CameraDevice)`

當攝像頭斷開連接時呼叫。如果攝像頭被另一個應用程式使用，或者設備關閉，就可能發生這種情況。一定要在這個回調中關閉攝像頭。

#### 3. `onError(camera: CameraDevice, error: Int)`

當發生錯誤時呼叫。常見的錯誤代碼：
- `ERROR_CAMERA_IN_USE` — 攝像頭已在使用中
- `ERROR_MAX_CAMERAS_IN_USE` — 打開的攝像頭過多
- `ERROR_CAMERA_DISABLED` — 攝像頭已停用
- `ERROR_CAMERA_DEVICE` — 攝像頭硬體錯誤
- `ERROR_CAMERA_SERVICE` — 攝像頭服務錯誤

### Handler

第三個參數是 `Handler`。如果你傳遞 `null`，回調將在呼叫執行緒的 looper 上執行。對於 UI 更新，你可能需要傳遞一個在主執行緒上執行的 handler。

## 完整範例

讓我們建立一個打開攝像頭的 Activity：

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
            Toast.makeText(this, "沒有可用的攝像頭", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // 打開第一個攝像頭
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "攝像頭權限被拒絕", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "無效的攝像頭 ID", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "攝像頭打開成功！", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "攝像頭 ${camera.id} 已打開")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "攝像頭已斷開連接", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "攝像頭正在使用中"
                ERROR_MAX_CAMERAS_IN_USE -> "打開的攝像頭過多"
                ERROR_CAMERA_DISABLED -> "攝像頭已停用"
                ERROR_CAMERA_DEVICE -> "攝像頭硬體錯誤"
                ERROR_CAMERA_SERVICE -> "攝像頭服務錯誤"
                else -> "未知錯誤"
            }
            
            Toast.makeText(this@CameraOpenActivity, "攝像頭錯誤：$errorMessage", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "需要攝像頭權限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## 攝像頭生命週期

瞭解攝像頭生命週期至關重要：

1. **打開** — 呼叫 `openCamera()` 獲取 CameraDevice
2. **使用** — 建立捕獲會話、拍攝照片
3. **關閉** — 完成後呼叫 `close()`
4. **釋放** — 攝像頭可供其他應用程式使用

當你的 Activity 被銷毀時，一定要關閉攝像頭以避免資源洩漏。

## 最佳實踐

1. **用完即關** — 一定要在 `onDestroy()` 中關閉攝像頭
2. **處理錯誤** — 不要忽略 `onError()` 回調
3. **檢查權限** — 打開前務必驗證權限
4. **使用 try-catch** — 處理 `SecurityException` 和 `IllegalArgumentException`
5. **不要持有引用** — 關閉時釋放 CameraDevice 引用

## 常見問題

### 攝像頭正在使用中
- 確保沒有其他應用程式正在使用攝像頭
- 檢查你是否正確關閉了攝像頭

### 權限被拒絕
- 驗證清單中的權限
- 檢查執行時權限是否已授予

### 找不到攝像頭 ID
- 始終從 `getCameraIdList()` 獲取攝像頭 ID
- 不要硬編碼攝像頭 ID

## 下一章

既然你已經可以打開攝像頭，下一步就是顯示預覽。在下一章中，我們將：

1. 瞭解 TextureView
2. 建立用於預覽的 Surface
3. 建立 CameraCaptureSession
4. 在螢幕上顯示攝像頭預覽

## 總結

打開攝像頭是拍攝圖像的第一步：

1. 使用 `CameraManager.openCamera()` 獲取 CameraDevice
2. 處理 StateCallback 的 `onOpened()`、`onDisconnected()` 和 `onError()`
3. 用完後一定要關閉攝像頭
4. 遵循攝像頭生命週期：打開 → 使用 → 關閉 → 釋放

在下一章中，我們將建立攝像頭預覽，這樣你就可以看到攝像頭看到的畫面。
