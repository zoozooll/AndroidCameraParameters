---
sidebar_position: 12
title: "12장: 수동 카메라 - ISO와 노출"
description: Camera2로 전문 사진 촬영을 위해 ISO와 노출 시간을 수동으로 제어하는 방법을 알아보세요.
keywords: [ISO, 노출 시간, 수동 카메라, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

수동 제어는 Camera2의 진정한 힘이 빛나는 부분입니다. ISO와 노출에 대해 알아봅시다.

## 소개

지금까지 자동 제어를 사용해 왔습니다. 이제 다음 항목들을 완전히 제어해 보겠습니다:

1. **ISO** — 빛에 대한 센서 감도
2. **노출 시간** — 센서가 빛을 모으는 시간

이 두 가지 설정은 이미지 밝기와 품질에 직접적인 영향을 미칩니다.

## ISO란 무엇인가요?

ISO는 빛에 대한 센서의 감도를 측정합니다. ISO가 낮을수록:
- 빛에 덜 민감함
- 노이즈가 적음
- 이미지 품질이 더 좋음

ISO가 높을수록:
- 빛에 더 민감함
- 노이즈(입자)가 많음
- 이미지 품질이 낮음

일반적인 ISO 값: 100, 200, 400, 800, 1600, 3200, 6400

## 노출 시간이란 무엇인가요?

노출 시간(셔터 속도라고도 함)은 센서가 빛을 모으는 시간입니다. 노출이 짧을수록:
- 적은 빛이 캡처됨
- 움직임을 정지시킴
- 더 빠른 액션

노출이 길수록:
- 더 많은 빛이 캡처됨
- 움직임 흐림 발생
- 저조도 성능이 더 좋음

노출 시간은 초 또는 초의 분수로 측정됩니다:
- 1/1000초 — 빠른 액션
- 1/125초 — 보통
- 1/30초 — 느림
- 1초 — 장노출

## 수동 제어 요구 사항

수동 제어를 사용하려면 카메라에 다음이 있어야 합니다:
1. **FULL** 또는 **LEVEL_3** 하드웨어 레벨
2. **MANUAL_SENSOR** 기능

CameraCharacteristics를 확인하세요:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## 지원되는 범위 가져오기

수동 값을 설정하기 전에 카메라가 지원하는 것을 확인하세요:

```kotlin
// ISO 범위 가져오기
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// 노출 범위 가져오기
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## 수동 ISO와 노출 설정

수동 제어를 사용하려면 다음을 수행해야 합니다:
1. 자동 노출(AE) 비활성화
2. 수동 ISO 설정
3. 수동 노출 시간 설정

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// AE 비활성화
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// 수동 ISO 설정
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// 수동 노출 시간 설정 (나노초 단위)
// 1/125초 = 8,000,000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// 타겟 추가
captureRequestBuilder.addTarget(surface)

// 수동 제어로 프리뷰 시작
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## 노출 삼각형

ISO, 노출 시간, 조리개는 "노출 삼각형"을 형성합니다:

- **ISO** — 빛에 대한 감도
- **노출 시간** — 빛 캡처 지속 시간
- **조리개** — 들어오는 빛의 양 (휴대폰에서는 조정 가능한 경우가 드뭄)

하나를 변경하면 다른 것들에 영향을 미칩니다. 예를 들어:
- ISO를 높이면 더 빠른 셔터 속도를 사용할 수 있습니다
- 노출 시간을 줄이면 ISO를 높여야 할 수 있습니다

## 완전한 수동 제어 예제

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60초

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO: 100단위로 100-3200
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // 노출: 1/1000초 ~ 1/10초
        exposureSeekBar.progress = 5
        
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 진행률을 노출 시간으로 변환 (나노초 단위)
                // 0: 1/1000초 = 1,000,000ns
                // 9: 1/10초 = 100,000,000ns
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // 수동 제어를 위해 AE 비활성화
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // 수동 ISO 설정
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // 수동 노출 시간 설정
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // AWB는 켜둠
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (나머지 카메라 설정 코드)
    
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
        }
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
    
    // ... (카메라 열기, 세션 생성 등)
}
```

## 레이아웃

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="ISO:"/>
            
            <SeekBar
                android:id="@+id/isoSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/isoValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="100"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="16dp">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="노출:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60초"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## 모범 사례

1. **지원 여부 확인** — 항상 수동 센서 지원을 확인하세요
2. **AE로 시작** — 자동 노출로 초기 값을 설정한 다음 수동으로 전환하세요
3. **극단적인 ISO 피하기** — 높은 ISO는 노이즈를 유발합니다
4. **가장 짧은 노출 사용** — 가능하면 움직임 흐림을 피하세요
5. **히스토그램 모니터링** — CaptureResult를 사용하여 노출을 확인하세요

## 다음 장

다음 장에서는 수동 초점과 화이트 밸런스 제어에 대해 알아보겠습니다.

## 요약

ISO와 노출의 수동 제어는 전문가 수준의 사진 촬영을 가능하게 합니다:

1. **ISO** — 센서 감도 제어 (100-3200+ 범위)
2. **노출 시간** — 빛을 모으는 시간 제어 (나노초 단위)
3. **AE 비활성화** — 수동 제어를 위해 자동 노출을 꺼야 합니다
4. **범위 확인** — 항상 지원되는 ISO와 노출 범위를 확인하세요

노출 삼각형(ISO, 노출 시간, 조리개)은 이미지 밝기와 품질을 결정합니다. 다음 장에서는 수동 초점과 화이트 밸런스를 살펴보겠습니다.
