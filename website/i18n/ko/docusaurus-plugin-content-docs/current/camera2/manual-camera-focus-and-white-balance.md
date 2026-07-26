---
sidebar_position: 13
title: "제13장: 수동 카메라 - 초점과 화이트 밸런스"
description: Camera2로 전문 사진 촬영을 위해 초점 거리와 화이트 밸런스를 수동으로 제어하는 방법을 알아보세요.
keywords: [초점, 화이트 밸런스, 수동 카메라, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

초점과 화이트 밸런스를 이용한 완전한 수동 제어.

## 소개

이전 장에서는 ISO와 노출을 제어하는 방법을 배웠습니다. 이제 다음을 추가하겠습니다:

1. **초점** — 수동 초점 거리 제어
2. **화이트 밸런스** — 수동 색온도 제어

이를 통해 사진에 대한 완전한 창의적 제어권을 갖게 됩니다.

## 수동 초점

초점은 장면의 어느 부분이 선명하게 보일지 결정합니다. 수동 초점을 사용하면 다음을 할 수 있습니다:
- 특정 피사체에 초점 맞추기
- 의도적인 흐림(보케) 만들기
- 매크로 사진에서 중요한 초점 확보하기

### 초점 모드

Camera2는 여러 초점 모드를 지원합니다:

| 모드 | 설명 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | 수동 초점 |
| `CONTROL_AF_MODE_AUTO` | 단일 자동 초점 |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 사진용 연속 자동 초점 |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | 동영상용 연속 자동 초점 |
| `CONTROL_AF_MODE_MACRO` | 매크로 초점 |

### 초점 거리

초점 거리는 디옵터(1/미터)로 측정됩니다. 값이 0이면 무한대를 의미합니다.

```kotlin
// 초점 거리 범위 가져오기
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### 수동 초점 설정하기

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 수동 초점을 위해 AF 비활성화
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// 수동 초점 거리 설정 (디옵터 단위)
// 0.0 = 무한대
// 1.0 = 1미터
// 2.0 = 0.5미터
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### 초점 영역

선택적 자동 초점을 위해 AF 영역을 지정할 수도 있습니다:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // 센서 좌표계의 중심 X (0-1000)
        centerY,    // 센서 좌표계의 중심 Y (0-1000)
        width,      // 영역 너비
        height,     // 영역 높이
        weight      // 우선순위 (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## 수동 화이트 밸런스

화이트 밸런스(WB)는 이미지의 색온도를 조정합니다. 광원에 따라 색온도가 다릅니다:

- **주간** — ~5500K (푸르스름함)
- **흐림** — ~6500K (더 차가움)
- **텅스텐** — ~2800K (따뜻함/노랑)
- **형광등** — ~4000K (녹색빛)

### 화이트 밸런스 모드

| 모드 | 설명 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | 수동 화이트 밸런스 |
| `CONTROL_AWB_MODE_AUTO` | 자동 화이트 밸런스 |
| `CONTROL_AWB_MODE_INCANDESCENT` | 백열등 조명 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 형광등 조명 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 따뜻한 형광등 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 주간 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 흐린 날 |

### 수동 화이트 밸런스 설정하기

수동 화이트 밸런스를 설정하려면 색 보정 게인을 설정해야 합니다:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// 수동 제어를 위해 AWB 비활성화
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// 색 보정 게인 설정 (R, G, B)
// 값은 정규화됨 (1.0 = 보정 없음)
// 값이 높을수록 해당 색상이 더 두드러짐
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### 색온도

색온도를 사용하여 화이트 밸런스를 설정할 수도 있습니다:

```kotlin
// 지원되는 색온도 범위 가져오기
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// 색온도 설정 (켈빈 단위)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // 주간
```

## 완전한 수동 카메라 예제

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // 수동 컨트롤
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60초
    private var currentFocus = 0.0f // 무한대
    private var currentWhiteBalance = 5500 // 주간

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // ISO 컨트롤
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 노출 컨트롤
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 초점 컨트롤
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 진행률(0-100)을 초점 거리(0.0 ~ 2.0 디옵터)로 변환
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // 화이트 밸런스 컨트롤
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K(텅스텐) ~ 6500K(흐림)
                currentWhiteBalance = 2800 + (progress * 37)
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
            
            // 수동 노출
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // 수동 초점
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // 수동 화이트 밸런스
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (카메라 설정 코드)
}
```

## 모범 사례

1. **지원 여부 확인** — 모든 카메라가 수동 초점이나 WB를 지원하지는 않습니다
2. **자동으로 시작** — 자동 제어로 기준선을 설정하세요
3. **초점 피킹 사용** — 초점 정확도를 위한 시각적 피드백을 추가하세요
4. **WB 보정** — 정확한 화이트 밸런스를 위해 회색 카드를 사용하세요
5. **컨트롤 결합** — 수동 설정은 함께 사용할 때 가장 좋습니다

## 다음 장

다음 장에서는 고속 동영상과 멀티 카메라 같은 전문 기능을 살펴보겠습니다.

## 요약

초점과 화이트 밸런스의 수동 제어로 카메라 툴킷이 완성됩니다:

1. **초점** — 이미지에서 선명하게 보일 부분을 제어합니다
2. **화이트 밸런스** — 색온도를 제어합니다
3. **수동 모드** — AF/AWB를 비활성화하고 값을 직접 설정합니다
4. **초점 영역** — 자동 초점을 위해 특정 영역을 대상으로 합니다

ISO, 노출, 초점, 화이트 밸런스를 제어할 수 있게 되면서 전문가 수준의 사진을 만들 수 있습니다. 제5부에서는 고속 동영상과 멀티 카메라 지원 같은 고급 기능을 살펴보겠습니다.
