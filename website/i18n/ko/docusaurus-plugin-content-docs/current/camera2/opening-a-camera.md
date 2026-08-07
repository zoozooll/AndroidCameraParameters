---
sidebar_position: 7
title: "제7장: 카메라 열기"
description: "CameraManager.openCamera()를 사용하여 물리적 카메라 장치에 연결하는 방법을 배우세요. CameraDevice.StateCallback을 마스터하고, 에러 코드를 처리하며, 안드로이드 수명 주기에 맞춰 카메라를 닫는 올바른 방법을 학습합니다."
keywords: [안드로이드 camera2 openCamera, CameraDevice.StateCallback, 카메라 권한, 카메라 세션, 안드로이드 카메라 수명 주기, 카메라 에러 처리]
---

# 제7장: 카메라 열기

6장에서 카메라의 특징을 파악했다면, 이제는 실제로 카메라를 "점유"하여 프레임을 받을 준비를 할 차례입니다. Camera2에서 카메라를 여는 과정은 하드웨어 자원을 독점적으로 사용하는 행위이므로, 비동기 콜백 처리와 꼼꼼한 수명 주기 관리가 필수적입니다.

카메라를 연다는 것은 안드로이드 시스템으로부터 `CameraDevice` 객체를 전달받는 것을 의미합니다. 이 객체는 우리가 카메라와 대화할 수 있는 유일한 통로입니다.

[Android Camera Parameters 앱](https://github.com/zoozooll/AndroidCameraParameters)의 **Overview** 탭에서 여러분이 열고자 하는 카메라의 `Hardware Level`을 미리 확인해 보세요. `LEGACY` 장치보다 `FULL` 또는 `LEVEL_3` 장치가 더 빠르고 안정적으로 열릴 것입니다.

---

## 1. CameraDevice.StateCallback 준비

카메라를 여는 작업은 시간이 걸리는 비동기 작업입니다. 결과를 받기 위해 `CameraDevice.StateCallback`을 먼저 정의해야 합니다.

```kotlin
private var cameraDevice: CameraDevice? = null

private val stateCallback = object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // 카메라가 성공적으로 열렸습니다!
        // 여기서부터 미리보기를 구성할 수 있습니다 (8장에서 다룸).
        cameraDevice = camera
        Log.d("Camera", "카메라 ID ${camera.id} 열림")
    }

    override fun onDisconnected(camera: CameraDevice) {
        // 카메라 연결이 끊겼습니다 (다른 앱이 사용하거나 장치가 분리됨).
        camera.close()
        cameraDevice = null
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // 에러 발생! (하드웨어 오류, 서비스 중단 등)
        camera.close()
        cameraDevice = null
        val msg = when(error) {
            ERROR_CAMERA_IN_USE -> "이미 사용 중인 카메라입니다."
            ERROR_MAX_CAMERAS_IN_USE -> "카메라 개수 초과입니다."
            ERROR_CAMERA_DISABLED -> "카메라가 비활성화되었습니다."
            ERROR_CAMERA_DEVICE -> "카메라 장치 자체에 오류가 있습니다."
            ERROR_CAMERA_SERVICE -> "카메라 서비스에 오류가 있습니다."
            else -> "알 수 없는 오류: $error"
        }
        Log.e("Camera", "카메라 열기 실패: $msg")
    }
}
```

---

## 2. openCamera() 호출하기

이제 `CameraManager`를 통해 실제로 열기 명령을 내립니다. 이때는 **반드시** `CAMERA` 권한이 필요하며, 5장에서 만든 백그라운드 핸들러를 사용하여 시스템 부하를 줄여야 합니다.

```kotlin
private fun openCamera(cameraId: String) {
    val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    
    try {
        // 권한 확인 (Manifest에 선언했어도 런타임 체크가 필요함)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // 실제 카메라 열기 명령
        manager.openCamera(cameraId, stateCallback, backgroundHandler)
        
    } catch (e: CameraAccessException) {
        Log.e("Camera", "카메라 접근 불가: ${e.message}")
    } catch (e: SecurityException) {
        Log.e("Camera", "보안 문제 발생: ${e.message}")
    }
}
```

---

## 3. 수명 주기(Lifecycle) 준수하기 (가장 중요!)

카메라는 전력을 많이 소모하고 민감한 하드웨어입니다. 앱이 백그라운드로 가면 **반드시** 닫아야 합니다. 닫지 않으면 배터리가 급속히 소모되거나, 다른 앱(카카오톡, 인스타그램 등)에서 카메라를 사용할 수 없게 되어 시스템 전체에 민폐를 끼치게 됩니다.

```kotlin
override fun onStop() {
    super.onStop()
    closeCamera()
}

private fun closeCamera() {
    cameraDevice?.close()
    cameraDevice = null
    Log.d("Camera", "카메라 닫힘")
}
```

---

## 흔히 발생하는 실수: 카메라 가로채기

한 번에 하나의 앱만 하나의 물리적 카메라를 점유할 수 있습니다. 이미 다른 카메라 앱이 실행 중인 상태에서 우리 앱을 실행하면 `onError`의 `ERROR_CAMERA_IN_USE`가 발생합니다. 프로급 앱은 이 시점에 사용자에게 "다른 앱이 카메라를 사용 중입니다. 종료 후 다시 시도해 주세요."라는 메시지를 띄워야 합니다.

---

## 요약

1. **비동기 방식:** `openCamera`를 호출하면 결과는 `StateCallback`으로 옵니다.
2. **권한 필수:** 런타임 권한이 없으면 `SecurityException`이 발생합니다.
3. **수명 주기:** `onStop`이나 `onPause`에서 반드시 `close()`를 호출하여 자원을 반납해야 합니다.

## 다음 단계

카메라 장치를 성공적으로 손에 넣었습니다! 이제 카메라가 보는 세상을 우리 눈으로 직접 확인해 볼 시간입니다. **제8장: 미리보기 보여주기**에서는 `Surface`와 `CaptureSession`을 사용하여 화면에 실시간 영상을 띄우는 방법을 알아봅니다.
