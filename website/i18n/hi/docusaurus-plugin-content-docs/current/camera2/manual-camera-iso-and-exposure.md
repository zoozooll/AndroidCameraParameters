---
sidebar_position: 12
title: "अध्याय 12: मैनुअल कैमरा - ISO और एक्सपोजर"
description: Camera2 के साथ प्रोफेशनल फोटोग्राफी के लिए ISO और एक्सपोजर समय को मैनुअली कंट्रोल करना सीखें।
keywords: [ISO, exposure time, manual camera, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

मैनुअल कंट्रोल में Camera2 की असली ताकत दिखती है। आइए ISO और एक्सपोजर के बारे में सीखें।

## परिचय

अब तक, हम ऑटोमैटिक कंट्रोल्स का उपयोग कर रहे थे। अब हम पूर्ण नियंत्रण लेंगे:

1. **ISO** — प्रकाश के प्रति सेंसर की संवेदनशीलता
2. **एक्सपोजर समय** — सेंसर कितनी देर प्रकाश एकत्र करता है

ये दो सेटिंग्स छवि की चमक और गुणवत्ता को सीधे प्रभावित करती हैं।

## ISO क्या है?

ISO सेंसर की प्रकाश के प्रति संवेदनशीलता को मापता है। कम ISO का मतलब:
- प्रकाश के प्रति कम संवेदनशील
- कम शोर
- बेहतर छवि गुणवत्ता

अधिक ISO का मतलब:
- प्रकाश के प्रति अधिक संवेदनशील
- अधिक शोर (दानेदार)
- कम छवि गुणवत्ता

सामान्य ISO मान: 100, 200, 400, 800, 1600, 3200, 6400

## एक्सपोजर समय क्या है?

एक्सपोजर समय (जिसे शटर स्पीड भी कहा जाता है) सेंसर कितनी देर प्रकाश एकत्र करता है। कम एक्सपोजर का मतलब:
- कम प्रकाश कैप्चर
- गति को फ्रीज करता है
- तेज़ एक्शन

अधिक एक्सपोजर का मतलब:
- अधिक प्रकाश कैप्चर
- मोशन ब्लर
- बेहतर कम-प्रकाश प्रदर्शन

एक्सपोजर समय सेकंड या सेकंड के अंशों में मापा जाता है:
- 1/1000s — तेज़ एक्शन
- 1/125s — सामान्य
- 1/30s — धीमा
- 1s — लंबा एक्सपोजर

## मैनुअल कंट्रोल आवश्यकताएं

मैनुअल कंट्रोल्स का उपयोग करने के लिए, आपके कैमरे में होना चाहिए:
1. **FULL** या **LEVEL_3** हार्डवेयर स्तर
2. **MANUAL_SENSOR** क्षमता

CameraCharacteristics जांचें:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## समर्थित रेंज प्राप्त करना

मैनुअल मान सेट करने से पहले, जांचें कि कैमरा क्या समर्थन करता है:

```kotlin
// ISO रेंज प्राप्त करें
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// एक्सपोजर रेंज प्राप्त करें
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## मैनुअल ISO और एक्सपोजर सेट करना

मैनुअल कंट्रोल्स का उपयोग करने के लिए, आपको:
1. ऑटो-एक्सपोजर (AE) डिसेबल करें
2. मैनुअल ISO सेट करें
3. मैनुअल एक्सपोजर समय सेट करें

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// AE डिसेबल करें
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// मैनुअल ISO सेट करें
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// मैनुअल एक्सपोजर समय सेट करें (नैनोसेकंड में)
// 1/125s = 8,000,000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// टारगेट जोड़ें
captureRequestBuilder.addTarget(surface)

// मैनुअल कंट्रोल्स के साथ प्रीव्यू शुरू करें
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## एक्सपोजर त्रिकोण

ISO, एक्सपोजर समय, और एपर्चर "एक्सपोजर त्रिकोण" बनाते हैं:

- **ISO** — प्रकाश के प्रति संवेदनशीलता
- **एक्सपोजर समय** — प्रकाश कैप्चर की अवधि
- **एपर्चर** — प्रवेश करने वाले प्रकाश की मात्रा (फोन में शायद ही एडजस्टेबल हो)

एक को बदलने से दूसरों पर असर पड़ता है। उदाहरण के लिए:
- यदि आप ISO बढ़ाते हैं, तो आप तेज़ शटर स्पीड का उपयोग कर सकते हैं
- यदि आप एक्सपोजर समय घटाते हैं, तो आपको ISO बढ़ाने की आवश्यकता हो सकती है

## एक पूर्ण मैनुअल कंट्रोल उदाहरण

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s

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

    // कंट्रोल्स सेटअप करें
    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO: 100-3200, 100 के स्टेप्स में
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // एक्सपोजर: 1/1000s से 1/10s तक
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
                // प्रगति को एक्सपोजर समय में कनवर्ट करें (नैनोसेकंड में)
                // 0: 1/1000s = 1,000,000ns
                // 9: 1/10s = 100,000,000ns
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    // प्रीव्यू अपडेट करें
    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // मैनुअल कंट्रोल के लिए AE डिसेबल करें
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // मैनुअल ISO सेट करें
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // मैनुअल एक्सपोजर समय सेट करें
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // AWB को ऑन रखें
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (बाकी कैमरा सेटअप कोड)
    
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
        }
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }
    
    // कैमरा परमिशन है या नहीं
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    // कैमरा परमिशन का अनुरोध करें
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
    
    // ... (कैमरा खोलना, सेशन बनाना, आदि)
}
```

## लेआउट

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
                android:text="एक्सपोजर:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60s"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## सर्वोत्तम अभ्यास

1. **समर्थन जांचें** — हमेशा मैनुअल सेंसर समर्थन सत्यापित करें
2. **AE से शुरू करें** — ऑटो-एक्सपोजर को प्रारंभिक मान सेट करने दें, फिर मैनुअल पर स्विच करें
3. **अत्यधिक ISO से बचें** — उच्च ISO शोर पैदा करता है
4. **सबसे कम एक्सपोजर का उपयोग करें** — संभव हो तो मोशन ब्लर से बचें
5. **हिस्टोग्राम मॉनिटर करें** — एक्सपोजर जांचने के लिए CaptureResult का उपयोग करें

## अगला अध्याय

अगले अध्याय में, हम मैनुअल फोकस और व्हाइट बैलेंस कंट्रोल के बारे में सीखेंगे।

## सारांश

ISO और एक्सपोजर का मैनुअल नियंत्रण आपको प्रोफेशनल-स्तर की फोटोग्राफी देता है:

1. **ISO** — सेंसर संवेदनशीलता को नियंत्रित करता है (100-3200+ रेंज)
2. **एक्सपोजर समय** — नियंत्रित करता है कि प्रकाश कितनी देर एकत्र किया जाता है (नैनोसेकंड में)
3. **AE डिसेबल करें** — मैनुअल कंट्रोल के लिए ऑटो-एक्सपोजर बंद करना जरूरी है
4. **रेंज जांचें** — हमेशा समर्थित ISO और एक्सपोजर रेंज सत्यापित करें

एक्सपोजर त्रिकोण (ISO, एक्सपोजर समय, एपर्चर) छवि की चमक और गुणवत्ता निर्धारित करता है। अगले अध्याय में, हम मैनुअल फोकस और व्हाइट बैलेंस का पता लगाएंगे।
