---
sidebar_position: 6
title: "6장: 카메라 목록화"
description: Android 기기에서 사용 가능한 모든 카메라를 찾아 목록으로 표시하는 첫 번째 Camera2 프로그램을 작성해 보세요.
keywords: [카메라 목록, CameraManager, 카메라 열거, Android Camera2]
---

이제 첫 번째 Camera2 프로그램을 작성할 시간입니다! 모든 카메라를 목록으로 표시하는 앱을 만들어 봅시다.

## 소개

이 장에서는 첫 번째 실제 Camera2 애플리케이션을 작성합니다. 목표는 간단합니다:

> 기기의 모든 카메라를 찾아 정보를 표시합니다.

이것은 작지만 중요한 단계입니다. 카메라를 사용하려면 먼저 카메라를 찾아야 합니다.

## 프로젝트 생성

새로운 Android 프로젝트를 생성하는 것으로 시작해 봅시다:

1. Android Studio를 엽니다
2. "Empty Activity"로 새 프로젝트를 생성합니다
3. 이름을 "Camera2List"로 지정합니다
4. 언어로 Kotlin을 선택합니다
5. 최소 SDK를 API 21로 설정합니다 (Camera2는 API 21에서 도입되었습니다)

## 권한 추가

`AndroidManifest.xml`에 카메라 권한을 추가합니다:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## 레이아웃

카메라 목록을 표시하는 간단한 레이아웃을 만듭니다. `activity_main.xml`을 업데이트합니다:

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
        android:text="사용 가능한 카메라"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## 액티비티

이제 메인 액티비티를 작성해 봅시다. 여기에 Camera2 코드가 들어갑니다:

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
                cameraInfoList.add("카메라를 찾을 수 없습니다")
            } else {
                cameraInfoList.add("${cameraIds.size}개의 카메라를 찾았습니다:")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "전면"
                        CameraCharacteristics.LENS_FACING_BACK -> "후면"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "외장"
                        else -> "알 수 없음"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "알 수 없음"
                    }
                    
                    cameraInfoList.add("카메라 $index (ID: $cameraId)")
                    cameraInfoList.add("  - 렌즈: $lensFacingStr")
                    cameraInfoList.add("  - 하드웨어 레벨: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("오류: 카메라 권한이 거부되었습니다")
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
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("카메라 권한이 거부되었습니다")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## 이 코드의 동작

무슨 일이 일어나는지 분석해 봅시다:

1. **CameraManager 가져오기** — CameraManager 시스템 서비스를 얻습니다
2. **권한 확인** — 카메라 권한이 부여되었는지 확인합니다
3. **카메라 목록화** — `getCameraIdList()`를 사용하여 모든 카메라 ID를 가져옵니다
4. **특성 가져오기** — 각 카메라에 대해 특성을 가져옵니다
5. **정보 표시** — 카메라 ID, 렌즈 방향, 하드웨어 레벨을 보여줍니다

## 예상 출력

앱을 실행하면 다음과 같은 내용이 표시됩니다:

```
3개의 카메라를 찾았습니다:

카메라 0 (ID: 0)
  - 렌즈: 후면
  - 하드웨어 레벨: FULL

카메라 1 (ID: 1)
  - 렌즈: 전면
  - 하드웨어 레벨: LIMITED

카메라 2 (ID: 2)
  - 렌즈: 후면
  - 하드웨어 레벨: FULL
```

## 성공!

첫 번째 Camera2 프로그램을 방금 작성했습니다! 간단해 보일 수 있지만, 이것은 앞으로 할 모든 일의 기초입니다.

## 문제 해결

문제가 발생하면 다음을 확인하세요:

1. **권한 거부됨** — 카메라 권한을 부여했는지 확인하세요
2. **카메라를 찾을 수 없음** — 기기에 카메라가 있는지 확인하세요
3. **SecurityException** — 매니페스트에 권한이 선언되어 있는지 확인하세요
4. **API 레벨이 너무 낮음** — Camera2는 API 21 이상이 필요합니다

## 다음은 무엇인가요?

이제 카메라를 목록화할 수 있으니, 다음 단계는 카메라의 특성을 더 자세히 살펴보는 것입니다. 다음 장에서는 다음을 수행합니다:

1. CameraCharacteristics 탐구
2. 렌즈 방향에 대해 알아보기
3. 하드웨어 레벨 이해하기
4. 센서 정보 확인하기

## 요약

이 장에서는 첫 번째 Camera2 프로그램을 작성했습니다. 이 앱은 다음을 수행합니다:

1. 카메라 권한 요청
2. CameraManager를 사용하여 카메라 열거
3. 카메라 ID, 렌즈 방향, 하드웨어 레벨 표시

이것은 완전한 Camera2 애플리케이션을 구축하기 위한 첫 번째 단계입니다. 다음 장에서는 CameraCharacteristics를 더 깊이 탐구하여 각 카메라가 무엇을 할 수 있는지 이해해 보겠습니다.
