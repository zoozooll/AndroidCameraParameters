---
sidebar_position: 8
title: "8장: 첫 번째 카메라 열기"
description: CameraManager를 사용하여 CameraDevice를 여는 방법과 상태 콜백으로 카메라 라이프사이클을 처리하는 방법을 알아보세요.
keywords: [CameraDevice, openCamera, 카메라 라이프사이클, CameraManager]
---

이제 첫 번째 카메라를 열어 볼 시간입니다! CameraDevice에 대해 알아봅시다.

## 소개

지금까지 카메라를 찾고 특성을 확인하는 방법을 배웠습니다. 이제 다음 단계로 넘어갑니다: **카메라 열기**.

카메라를 열면 실제 카메라 하드웨어에 접근할 수 있습니다. 카메라를 열면 캡처 세션을 생성하고, 프리뷰를 표시하고, 사진을 촬영할 수 있습니다.

## CameraDevice란 무엇인가요?

CameraDevice는 Android 기기에 연결된 단일 카메라를 나타냅니다. 다음과 같은 메서드를 제공합니다:
- 캡처 세션 생성
- 정지 이미지 캡처
- 프리뷰 시작 및 중지

CameraDevice를 직접 생성하지는 않습니다. 대신 CameraManager에서 `openCamera()`를 호출하여 가져옵니다.

## 카메라 열기

카메라를 여는 방법은 다음과 같습니다:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // 카메라를 사용할 준비가 됨
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // 카메라 연결이 끊어짐
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // 카메라 오류 발생
        camera.close()
    }
}, null)
```

자세히 살펴봅시다.

### StateCallback

카메라를 여는 것은 비동기 작업이므로 CameraDevice는 콜백 패턴을 사용합니다. 콜백에는 세 가지 주요 메서드가 있습니다:

#### 1. `onOpened(camera: CameraDevice)`

카메라가 성공적으로 열렸을 때 호출됩니다. 여기서 CameraDevice 인스턴스를 얻습니다.

#### 2. `onDisconnected(camera: CameraDevice)`

카메라 연결이 끊어졌을 때 호출됩니다. 다른 앱이 카메라를 사용하거나 기기가 종료될 때 발생할 수 있습니다. 이 콜백에서는 항상 카메라를 닫아야 합니다.

#### 3. `onError(camera: CameraDevice, error: Int)`

오류가 발생했을 때 호출됩니다. 일반적인 오류 코드:
- `ERROR_CAMERA_IN_USE` — 카메라가 이미 사용 중
- `ERROR_MAX_CAMERAS_IN_USE` — 너무 많은 카메라가 열려 있음
- `ERROR_CAMERA_DISABLED` — 카메라가 비활성화됨
- `ERROR_CAMERA_DEVICE` — 카메라 하드웨어 오류
- `ERROR_CAMERA_SERVICE` — 카메라 서비스 오류

### Handler

세 번째 매개변수는 `Handler`입니다. `null`을 전달하면 콜백은 호출 스레드의 looper에서 실행됩니다. UI 업데이트를 위해서는 메인 스레드에서 실행되는 핸들러를 전달하는 것이 좋습니다.

## 완전한 예제

카메라를 여는 액티비티를 만들어 봅시다:

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
            Toast.makeText(this, "사용 가능한 카메라가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // 첫 번째 카메라 열기
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "카메라 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "잘못된 카메라 ID", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "카메라가 성공적으로 열렸습니다!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "카메라 ${camera.id} 열림")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "카메라 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "카메라가 사용 중입니다"
                ERROR_MAX_CAMERAS_IN_USE -> "너무 많은 카메라가 열려 있습니다"
                ERROR_CAMERA_DISABLED -> "카메라가 비활성화되었습니다"
                ERROR_CAMERA_DEVICE -> "카메라 하드웨어 오류"
                ERROR_CAMERA_SERVICE -> "카메라 서비스 오류"
                else -> "알 수 없는 오류"
            }
            
            Toast.makeText(this@CameraOpenActivity, "카메라 오류: $errorMessage", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## 카메라 라이프사이클

카메라 라이프사이클을 이해하는 것은 매우 중요합니다:

1. **열기** — `openCamera()`를 호출하여 CameraDevice 얻기
2. **사용** — 캡처 세션 생성, 사진 촬영
3. **닫기** — 작업이 끝나면 `close()` 호출
4. **해제** — 카메라가 다른 앱에서 사용 가능해짐

리소스 누수를 방지하려면 액티비티가 파괴될 때 항상 카메라를 닫아야 합니다.

## 모범 사례

1. **작업이 끝나면 닫기** — `onDestroy()`에서 항상 카메라 닫기
2. **오류 처리** — `onError()` 콜백을 무시하지 마세요
3. **권한 확인** — 열기 전에 항상 권한을 확인하세요
4. **try-catch 사용** — `SecurityException`과 `IllegalArgumentException` 처리
5. **참조를 보관하지 않기** — 닫을 때 CameraDevice 참조 해제

## 일반적인 문제

### 카메라가 사용 중입니다
- 다른 앱이 카메라를 사용하고 있지 않은지 확인하세요
- 카메라를 제대로 닫고 있는지 확인하세요

### 권한 거부됨
- 매니페스트의 권한을 확인하세요
- 런타임 권한이 부여되었는지 확인하세요

### 카메라 ID를 찾을 수 없음
- 항상 `getCameraIdList()`에서 카메라 ID를 가져오세요
- 카메라 ID를 하드코딩하지 마세요

## 다음 장

이제 카메라를 열 수 있으니, 다음 단계는 프리뷰를 표시하는 것입니다. 다음 장에서는 다음을 다룹니다:

1. TextureView 알아보기
2. 프리뷰용 Surface 만들기
3. CameraCaptureSession 만들기
4. 화면에 카메라 프리뷰 표시하기

## 요약

카메라를 여는 것은 이미지 캡처를 위한 첫 단계입니다:

1. `CameraManager.openCamera()`를 사용하여 CameraDevice 얻기
2. `onOpened()`, `onDisconnected()`, `onError()`에 대한 StateCallback 처리
3. 작업이 끝나면 항상 카메라 닫기
4. 카메라 라이프사이클 따르기: 열기 → 사용 → 닫기 → 해제

다음 장에서는 카메라 프리뷰를 만들어 카메라가 보는 것을 직접 확인해 봅시다.
