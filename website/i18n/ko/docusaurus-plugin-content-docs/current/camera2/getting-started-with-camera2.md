---
sidebar_position: 5
title: "5장: Camera2 시작하기"
description: 카메라를 열거하고 특성에 접근할 수 있게 해주는 Android Camera2 API의 진입점인 CameraManager에 대해 알아봅니다.
keywords: [CameraManager, Camera2 API, Android camera, 카메라 열거]
---

이 시리즈의 코딩 파트에 오신 것을 환영합니다. 기초부터 시작해 봅시다: CameraManager.

## 소개

카메라를 사용하기 전에, 카메라를 찾아서 접근할 방법이 필요합니다. 바로 여기에 **CameraManager**가 쓰입니다.

CameraManager는 Camera2 API로 가는 관문입니다. 어떤 Camera2 애플리케이션에서든 가장 먼저 사용하게 될 클래스입니다.

## CameraManager란 무엇인가?

CameraManager는 Android 기기의 모든 카메라 장치를 관리하는 시스템 서비스입니다. 카메라의 디렉터리나 레지스트리라고 생각하면 됩니다.

주요 책임은 다음과 같습니다:
1. **카메라 열거** — 사용 가능한 모든 카메라 나열
2. **카메라 특성 가져오기** — 각 카메라에 대한 자세한 정보 검색
3. **카메라 열기** — 촬영을 위한 CameraDevice 생성

## CameraManager 가져오기

Android에서 시스템 서비스는 `Context`를 통해 얻습니다. CameraManager를 가져오는 방법은 다음과 같습니다:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

이게 전부입니다. 기기의 모든 카메라에 접근하는 데 단 한 줄의 코드면 됩니다.

## 먼저 권한부터

CameraManager를 사용하기 전에 카메라 권한을 요청해야 합니다. `AndroidManifest.xml`에 다음을 추가하세요:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

그리고 액티비티에서 런타임 권한을 요청하세요:

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

카메라에 접근하기 전에 항상 권한을 확인하세요.

## CameraManager 메서드

CameraManager에는 세 가지 주요 메서드가 있습니다:

### 1. `getCameraIdList()`

카메라 ID 문자열 배열을 반환합니다. 각 ID는 카메라 장치를 나타냅니다.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "발견된 카메라: $id")
}
```

다음과 같이 출력될 수 있습니다:
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

특정 카메라에 대한 모든 세부 정보를 포함하는 `CameraCharacteristics` 객체를 반환합니다.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics에는 카메라의 기능을 설명하는 수백 가지 매개변수가 포함되어 있습니다.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

카메라를 열고 콜백을 통해 `CameraDevice`를 반환합니다. 이에 대해서는 나중에 자세히 다루겠습니다.

## 카메라 ID 다시 살펴보기

4장에서 Android가 카메라에 숫자 ID를 할당한다는 것을 기억하세요. 이 ID는 기기 간에 또는 재부팅 시에도 일관성이 보장되지 않습니다.

일반적인 패턴:
- **카메라 0** — 일반적으로 후면 광각 카메라
- **카메라 1** — 종종 전면 카메라
- **카메라 2** — 보통 초광각 또는 망원 카메라
- 더 높은 숫자 — 추가 카메라 (매크로, 뎁스 등)

하지만 카메라 ID의 의미를 **절대 가정하지 마세요**. 카메라 특성을 항상 확인하여 다음을 판단하세요:
- 렌즈 방향 (전면/후면/외장)
- 초점 거리
- 기능

## CameraManager가 중요한 이유

CameraManager는 우리가 Camera2로 할 모든 일의 기초입니다:

1. **발견** — 카메라를 사용하기 전에, 찾아야 합니다
2. **정보** — 카메라를 열기 전에, 그 기능을 알아야 합니다
3. **접근** — CameraManager는 카메라 장치를 열 수 있는 유일한 방법을 제공합니다

## 간단한 예제

간단한 예제로 모든 것을 종합해 봅시다:

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
        
        Log.d("CameraDiscovery", "${cameraIds.size}개의 카메라를 발견했습니다")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "전면"
                CameraCharacteristics.LENS_FACING_BACK -> "후면"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "외장"
                else -> "알 수 없음"
            }
            
            Log.d("CameraDiscovery", "카메라 $cameraId: $lensFacingStr")
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
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

이 간단한 액티비티는 모든 카메라를 발견하고 ID와 렌즈 방향을 로그로 기록합니다.

## 주요 요점

- **CameraManager**는 Camera2로 가는 진입점입니다
- `getCameraIdList()`를 사용하여 모든 카메라를 찾으세요
- `getCameraCharacteristics()`를 사용하여 자세한 정보를 얻으세요
- 항상 카메라 권한을 먼저 요청하세요
- 카메라 ID 의미를 가정하지 마세요 — 특성을 확인하세요

## 다음 장

CameraManager를 이해했으니, 이제 첫 번째 진짜 Camera2 프로그램을 작성할 시간입니다. 다음 장에서는 다음을 수행합니다:

1. 간단한 Android 앱 만들기
2. 사용 가능한 모든 카메라 나열하기
3. 사용자에게 카메라 정보 표시하기

첫 번째 Camera2 코드를 작성하고 실제 결과를 보게 될 것입니다!

## 요약

CameraManager는 Camera2의 기초입니다. 다음에 대한 접근을 제공합니다:
- 카메라 열거
- 카메라 특성
- 카메라 열기

CameraManager를 사용하면 사용 가능한 카메라가 무엇인지 발견하고 카메라를 열기 전에 그 기능에 대해 알아볼 수 있습니다.

다음 장에서는 기기의 모든 카메라를 나열하는 첫 번째 Camera2 프로그램을 작성해 보겠습니다.
