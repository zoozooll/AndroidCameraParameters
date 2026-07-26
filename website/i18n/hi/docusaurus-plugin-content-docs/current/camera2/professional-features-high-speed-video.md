---
sidebar_position: 14
title: "अध्याय 14: प्रोफेशनल फीचर्स - हाई-स्पीड वीडियो"
description: Camera2 में हाई-स्पीड वीडियो कैप्चर करना और मल्टी-कैमरा सेटअप के साथ काम करना सीखें।
keywords: [high-speed video, multi-camera, Camera2, constrained high speed, logical camera]
---

प्रोफेशनल फीचर्स नई क्रिएटिव संभावनाएं खोलते हैं। आइए हाई-स्पीड वीडियो और मल्टी-कैमरा के बारे में जानें।

## परिचय

Camera2 बुनियादी फोटो कैप्चर से आगे कई प्रोफेशनल फीचर्स सपोर्ट करता है। इस अध्याय में, हम सीखेंगे:

1. **हाई-स्पीड वीडियो** — स्लो-मोशन वीडियो कैप्चर करना
2. **मल्टी-कैमरा** — लॉजिकल और फिजिकल कैमरों के साथ काम करना

## हाई-स्पीड वीडियो

हाई-स्पीड वीडियो आपको मानक 30fps से अधिक फ्रेम दरों पर वीडियो कैप्चर करने की अनुमति देता है:
- 120fps — स्मूथ स्लो मोशन
- 240fps — स्टैंडर्ड स्लो मोशन
- 480fps — एक्सट्रीम स्लो मोशन
- 960fps — सुपर स्लो मोशन

### आवश्यकताएं

हाई-स्पीड वीडियो कैप्चर करने के लिए, आपके कैमरे में होना चाहिए:
1. `CONSTRAINED_HIGH_SPEED_VIDEO` क्षमता का समर्थन
2. सही हार्डवेयर स्तर

CameraCharacteristics जांचें:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### हाई-स्पीड वीडियो साइज़

हाई-स्पीड वीडियो मानक वीडियो की तुलना में अलग रिजोल्यूशन का उपयोग करता है:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### हाई-स्पीड वीडियो कैप्चर करना

हाई-स्पीड वीडियो के लिए एक विशेष कैप्चर सेशन की आवश्यकता होती है:

```kotlin
// हाई-स्पीड कैप्चर सेशन बनाएं
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "हाई-स्पीड सेशन विफल", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### हाई-स्पीड प्रीव्यू शुरू करना

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // समर्थित हाई-स्पीड fps रेंज प्राप्त करें
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // हाई-स्पीड रेंज खोजें (उदाहरण के लिए, 120fps)
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

## मल्टी-कैमरा

आधुनिक फोन में कई कैमरे होते हैं। Camera2 उन्हें इस प्रकार मानता है:
- **फिजिकल कैमरे** — अलग-अलग कैमरा सेंसर
- **लॉजिकल कैमरे** — फिजिकल कैमरों के संयोजन

### लॉजिकल बनाम फिजिकल कैमरे

| प्रकार | विवरण |
| --- | --- |
| **फिजिकल** | एकल कैमरा सेंसर (वाइड, टेलीफोटो, अल्ट्रा-वाइड) |
| **लॉजिकल** | वर्चुअल कैमरा जो कई फिजिकल कैमरों को जोड़ता है |

### कैमरा प्रकारों की पहचान करना

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// जांचें कि यह लॉजिकल कैमरा है या नहीं
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// फिजिकल कैमरा ID प्राप्त करें (लॉजिकल कैमरों के लिए)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### कैमरों के बीच स्विच करना

कैमरों के बीच स्विच करने के लिए, आपको:
1. वर्तमान कैमरा बंद करें
2. नया कैमरा खोलें
3. नया कैप्चर सेशन बनाएं

```kotlin
private fun switchCamera(newCameraId: String) {
    // वर्तमान कैमरा बंद करें
    captureSession?.close()
    cameraDevice?.close()
    
    // नया कैमरा खोलें
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### समवर्ती कैमरा

कुछ डिवाइस एक साथ कई कैमरे खोलने का समर्थन करते हैं:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## कैमरा एक्सटेंशन

कैमरा एक्सटेंशन आपको निर्माता-विशिष्ट कैमरा फीचर्स का उपयोग करने की अनुमति देता है:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// उपलब्ध एक्सटेंशन प्राप्त करें
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

सामान्य एक्सटेंशन:
- `EXTENSION_BOKEH` — पोर्ट्रेट मोड
- `EXTENSION_HDR` — HDR मोड
- `EXTENSION_NIGHT` — नाइट मोड
- `EXTENSION_AUTO` — ऑटोमैटिक मोड

## एक मल्टी-कैमरा उदाहरण

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
                CameraCharacteristics.LENS_FACING_FRONT -> "फ्रंट कैमरा ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "बैक कैमरा ($id)"
                else -> "कैमरा $id"
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
            Toast.makeText(this, "कैमरा परमिशन अस्वीकृत", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@MultiCameraActivity, "सेशन विफल", Toast.LENGTH_SHORT).show()
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

## सर्वोत्तम अभ्यास

1. **क्षमताएं जांचें** — प्रोफेशनल फीचर्स का उपयोग करने से पहले हमेशा सत्यापित करें
2. **संक्रमण को संभालें** — कैमरे स्विच करते समय स्मूथली बंद/खोलें
3. **संसाधनों का प्रबंधन करें** — हाई-स्पीड वीडियो अधिक संसाधनों की खपत करता है
4. **सुंदर तरीके से फॉलबैक करें** — फीचर्स उपलब्ध नहीं होने पर विकल्प प्रदान करें

## अगला अध्याय

अंतिम अध्याय में, हम CameraCharacteristics एनसाइक्लोपीडिया का पता लगाएंगे — सबसे महत्वपूर्ण कैमरा पैरामीटर्स की गहराई से जानकारी।

## सारांश

प्रोफेशनल फीचर्स आपकी क्रिएटिव संभावनाओं का विस्तार करते हैं:

1. **हाई-स्पीड वीडियो** — 120-960fps पर स्लो-मोशन कैप्चर करें
2. **मल्टी-कैमरा** — लॉजिकल और फिजिकल कैमरों के साथ काम करें
3. **कैमरा एक्सटेंशन** — निर्माता-विशिष्ट फीचर्स का उपयोग करें
4. **समवर्ती कैमरा** — एक साथ कई कैमरे खोलें

अगले अध्याय में, हम CameraCharacteristics — कैमरा पैरामीटर्स के विश्वकोश में गहराई से जाएंगे।
