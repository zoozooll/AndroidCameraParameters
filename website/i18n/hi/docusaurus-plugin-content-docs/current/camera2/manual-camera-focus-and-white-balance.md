---
sidebar_position: 13
title: "अध्याय 13: मैनुअल कैमरा - फोकस और व्हाइट बैलेंस"
description: Camera2 के साथ प्रोफेशनल फोटोग्राफी के लिए फोकस डिस्टेंस और व्हाइट बैलेंस को मैन्युअली कंट्रोल करना सीखें।
keywords: [फोकस, व्हाइट बैलेंस, मैनुअल कैमरा, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

फोकस और व्हाइट बैलेंस के साथ पूर्ण मैनुअल कंट्रोल।

## परिचय

पिछले अध्याय में, आपने ISO और एक्सपोजर को कंट्रोल करना सीखा था। अब हम जोड़ेंगे:

1. **फोकस** — मैनुअल फोकस डिस्टेंस कंट्रोल
2. **व्हाइट बैलेंस** — मैनुअल कलर टेम्परेचर कंट्रोल

इनके साथ, आपके पास अपनी तस्वीरों पर पूर्ण क्रिएटिव कंट्रोल है।

## मैनुअल फोकस

फोकस निर्धारित करता है कि सीन का कौन सा हिस्सा स्पष्ट है। मैनुअल फोकस आपको यह करने की अनुमति देता है:
- विशिष्ट वस्तुओं पर फोकस करें
- जानबूझकर धुंधलापन बनाएं (बोकेह)
- मैक्रो फोटोग्राफी में महत्वपूर्ण फोकस सुनिश्चित करें

### फोकस मोड

Camera2 कई फोकस मोड सपोर्ट करता है:

| मोड | विवरण |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | मैनुअल फोकस |
| `CONTROL_AF_MODE_AUTO` | सिंगल ऑटो-फोकस |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | तस्वीरों के लिए कंटीन्यूअस ऑटोफोकस |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | वीडियो के लिए कंटीन्यूअस ऑटोफोकस |
| `CONTROL_AF_MODE_MACRO` | मैक्रो फोकस |

### फोकस डिस्टेंस

फोकस डिस्टेंस डायोप्टर (1/मीटर) में मापा जाता है। 0 का मतलब इनफिनिटी है।

```kotlin
// फोकस डिस्टेंस रेंज प्राप्त करें
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### मैनुअल फोकस सेट करना

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// मैनुअल फोकस के लिए AF डिसेबल करें
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// मैनुअल फोकस डिस्टेंस सेट करें (डायोप्टर में)
// 0.0 = इनफिनिटी
// 1.0 = 1 मीटर
// 2.0 = 0.5 मीटर
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### फोकस रीजन

आप सिलेक्टिव ऑटोफोकस के लिए AF रीजन भी निर्दिष्ट कर सकते हैं:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // सेंसर कोऑर्डिनेट में सेंटर X (0-1000)
        centerY,    // सेंसर कोऑर्डिनेट में सेंटर Y (0-1000)
        width,      // रीजन चौड़ाई
        height,     // रीजन ऊंचाई
        weight      // प्राथमिकता (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## मैनुअल व्हाइट बैलेंस

व्हाइट बैलेंस (WB) छवि के कलर टेम्परेचर को एडजस्ट करता है। अलग-अलग प्रकाश स्रोतों के अलग-अलग कलर टेम्परेचर होते हैं:

- **डेलाइट** — ~5500K (नीलापन)
- **क्लाउडी** — ~6500K (ठंडा)
- **टंगस्टन** — ~2800K (गर्म/पीला)
- **फ्लूरोसेंट** — ~4000K (हरापन)

### व्हाइट बैलेंस मोड

| मोड | विवरण |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | मैनुअल व्हाइट बैलेंस |
| `CONTROL_AWB_MODE_AUTO` | ऑटोमैटिक व्हाइट बैलेंस |
| `CONTROL_AWB_MODE_INCANDESCENT` | टंगस्टन लाइटिंग |
| `CONTROL_AWB_MODE_FLUORESCENT` | फ्लूरोसेंट लाइटिंग |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | गर्म फ्लूरोसेंट |
| `CONTROL_AWB_MODE_DAYLIGHT` | डेलाइट |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | क्लाउडी |

### मैनुअल व्हाइट बैलेंस सेट करना

मैनुअल व्हाइट बैलेंस सेट करने के लिए, आपको कलर करेक्शन गेन सेट करने होंगे:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// मैनुअल कंट्रोल के लिए AWB डिसेबल करें
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// कलर करेक्शन गेन सेट करें (R, G, B)
// मान सामान्यीकृत हैं (1.0 = कोई करेक्शन नहीं)
// उच्च मान उस रंग को अधिक प्रमुख बनाते हैं
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### कलर टेम्परेचर

आप कलर टेम्परेचर का उपयोग करके व्हाइट बैलेंस भी सेट कर सकते हैं:

```kotlin
// सपोर्टेड कलर टेम्परेचर रेंज प्राप्त करें
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// कलर टेम्परेचर सेट करें (केल्विन में)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // डेलाइट
```

## एक पूर्ण मैनुअल कैमरा उदाहरण

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // मैनुअल कंट्रोल
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // इनफिनिटी
    private var currentWhiteBalance = 5500 // डेलाइट

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // ISO कंट्रोल
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // एक्सपोजर कंट्रोल
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // फोकस कंट्रोल
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // प्रगति (0-100) को फोकस डिस्टेंस (0.0 से 2.0 डायोप्टर) में कनवर्ट करें
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // व्हाइट बैलेंस कंट्रोल
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (टंगस्टन) से 6500K (क्लाउडी)
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
            
            // मैनुअल एक्सपोजर
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // मैनुअल फोकस
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // मैनुअल व्हाइट बैलेंस
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (कैमरा सेटअप कोड)
}
```

## सर्वोत्तम प्रथाएं

1. **सपोर्ट की जांच करें** — सभी कैमरे मैनुअल फोकस या WB सपोर्ट नहीं करते
2. **ऑटोमैटिक से शुरू करें** — ऑटो-कंट्रोल को बेसलाइन स्थापित करने दें
3. **फोकस पीकिंग का उपयोग करें** — फोकस सटीकता के लिए विजुअल फीडबैक जोड़ें
4. **WB कैलिब्रेट करें** — सटीक व्हाइट बैलेंस के लिए ग्रे कार्ड का उपयोग करें
5. **कंट्रोल को कंबाइन करें** — मैनुअल सेटिंग्स साथ में सबसे अच्छा काम करती हैं

## अगला अध्याय

अगले अध्याय में, हम हाई-स्पीड वीडियो और मल्टी-कैमरा जैसी प्रोफेशनल फीचर्स का अन्वेषण करेंगे।

## सारांश

फोकस और व्हाइट बैलेंस का मैनुअल कंट्रोल आपके कैमरा टूलकिट को पूरा करता है:

1. **फोकस** — नियंत्रित करें कि छवि में क्या स्पष्ट है
2. **व्हाइट बैलेंस** — कलर टेम्परेचर कंट्रोल करें
3. **मैनुअल मोड** — AF/AWB डिसेबल करें और सीधे मान सेट करें
4. **फोकस रीजन** — ऑटोफोकस के लिए विशिष्ट क्षेत्रों को लक्षित करें

ISO, एक्सपोजर, फोकस और व्हाइट बैलेंस के आपके कंट्रोल में होने से, आप प्रोफेशनल-क्वालिटी की तस्वीरें बना सकते हैं। भाग V में, हम हाई-स्पीड वीडियो और मल्टी-कैमरा सपोर्ट जैसी एडवांस्ड फीचर्स का अन्वेषण करेंगे।
