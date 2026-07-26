---
sidebar_position: 14
title: "14장: 전문가 기능 - 고속 비디오"
description: Camera2에서 고속 비디오를 캡처하고 멀티 카메라 설정을 다루는 방법을 알아봅니다.
keywords: [고속 비디오, 멀티 카메라, Camera2, 제한된 고속, 논리적 카메라]
---

전문가 기능은 새로운 창의적 가능성을 열어줍니다. 고속 비디오와 멀티 카메라에 대해 알아봅시다.

## 소개

Camera2는 기본 사진 촬영 이상의 많은 전문가 기능을 지원합니다. 이 장에서는 다음 내용을 학습합니다:

1. **고속 비디오** — 슬로우 모션 비디오 캡처
2. **멀티 카메라** — 논리적 및 물리적 카메라 다루기

## 고속 비디오

고속 비디오를 사용하면 표준 30fps보다 높은 프레임 레이트로 비디오를 캡처할 수 있습니다:
- 120fps — 부드러운 슬로우 모션
- 240fps — 표준 슬로우 모션
- 480fps — 극단적인 슬로우 모션
- 960fps — 슈퍼 슬로우 모션

### 요구 사항

고속 비디오를 캡처하려면 카메라가 다음을 충족해야 합니다:
1. `CONSTRAINED_HIGH_SPEED_VIDEO` 기능 지원
2. 올바른 하드웨어 레벨 보유

CameraCharacteristics 확인:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### 고속 비디오 크기

고속 비디오는 표준 비디오와 다른 해상도를 사용합니다:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### 고속 비디오 캡처

고속 비디오에는 특수 캡처 세션이 필요합니다:

```kotlin
// 고속 캡처 세션 생성
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "고속 세션 실패", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### 고속 미리보기 시작

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // 지원되는 고속 fps 범위 가져오기
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // 고속 범위 찾기 (예: 120fps)
    val highSpeedFpsRange = fpsRanges?.find { it.upper >= 120 }
    
    captureRequestBuilder?.apply {
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange)
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.setRepeatingRequest(builder.build(), null, null)
    }
}
```

## 멀티 카메라

현대적인 스마트폰에는 여러 카메라가 있습니다. Camera2는 이들을 다음과 같이 취급합니다:
- **물리적 카메라** — 개별 카메라 센서
- **논리적 카메라** — 물리적 카메라의 조합

### 논리적 vs 물리적 카메라

| 유형 | 설명 |
| --- | --- |
| **물리적** | 단일 카메라 센서 (광각, 망원, 초광각) |
| **논리적** | 여러 물리적 카메라를 결합한 가상 카메라 |

### 카메라 유형 식별

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// 논리적 카메라인지 확인
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// 물리적 카메라 ID 가져오기 (논리적 카메라의 경우)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### 카메라 전환

카메라를 전환하려면 다음을 수행해야 합니다:
1. 현재 카메라 닫기
2. 새 카메라 열기
3. 새 캡처 세션 생성

```kotlin
private fun switchCamera(newCameraId: String) {
    // 현재 카메라 닫기
    captureSession?.close()
    cameraDevice?.close()
    
    // 새 카메라 열기
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### 동시 카메라

일부 기기에서는 여러 카메라를 동시에 열 수 있습니다:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## 카메라 확장

카메라 확장을 사용하면 제조업체 고유의 카메라 기능을 사용할 수 있습니다:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// 사용 가능한 확장 기능 가져오기
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

일반적인 확장 기능:
- `EXTENSION_BOKEH` — 인물 모드
- `EXTENSION_HDR` — HDR 모드
- `EXTENSION_NIGHT` — 야간 모드
- `EXTENSION_AUTO` — 자동 모드

## 멀티 카메라 예제

```kotlin
class MultiCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_camera)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupCameraSelector()
    }

    private fun setupCameraSelector() {
        val cameraIds = cameraManager.cameraIdList
        val spinner = findViewById<Spinner>(R.id.cameraSpinner)
        
        val cameraNames = cameraIds.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "전면 카메라 ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "후면 카메라 ($id)"
                else -> "카메라 $id"
            }
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newCameraId = cameraIds[position]
                if (newCameraId != currentCameraId) {
                    switchCamera(newCameraId)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun switchCamera(newCameraId: String) {
        captureSession?.close()
        cameraDevice?.close()
        currentCameraId = newCameraId
        openCamera()
    }

    private fun openCamera() {
        try {
            cameraManager.openCamera(currentCameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "카메라 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "세션 실패", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun startPreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(surface)
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }
}
```

## 모범 사례

1. **기능 확인** — 전문가 기능을 사용하기 전에 항상 확인하세요
2. **전환 처리** — 카메라 전환 시 부드럽게 닫고 열으세요
3. **리소스 관리** — 고속 비디오는 더 많은 리소스를 소비합니다
4. **우아한 폴백** — 기능을 사용할 수 없을 때 대안을 제공하세요

## 다음 장

마지막 장에서는 CameraCharacteristics 백과사전 — 가장 중요한 카메라 파라미터에 대한 심층 탐구를 살펴보겠습니다.

## 요약

전문가 기능은 창의적 가능성을 확장합니다:

1. **고속 비디오** — 120-960fps로 슬로우 모션 캡처
2. **멀티 카메라** — 논리적 및 물리적 카메라 다루기
3. **카메라 확장** — 제조업체 고유 기능 사용
4. **동시 카메라** — 여러 카메라를 동시에 열기

다음 장에서는 카메라 파라미터의 백과사전인 CameraCharacteristics에 대해 심층적으로 알아보겠습니다.
