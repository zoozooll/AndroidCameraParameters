---
sidebar_position: 7
title: "अध्याय 7: CameraCharacteristics को समझना"
description: CameraCharacteristics का अन्वेषण करें और लेंस की दिशा, हार्डवेयर स्तर, सेंसर का आकार और अन्य महत्वपूर्ण कैमरा क्षमताओं के बारे में जानें।
keywords: [CameraCharacteristics, lens facing, hardware level, sensor size, camera capabilities]
---

CameraCharacteristics कैमरे की आत्मा में आपकी खिड़की है। आइए इसका अन्वेषण करें।

## परिचय

पिछले अध्याय में, आपने सीखा कि कैसे कैमरों की सूची बनाएं और बुनियादी जानकारी प्राप्त करें। अब हम **CameraCharacteristics** में गहराई से उतरेंगे — जो कि कैमरे की क्षमताओं का व्यापक विवरण है।

CameraCharacteristics में सैकड़ों पैरामीटर होते हैं। इस अध्याय में, हम सबसे महत्वपूर्ण पैरामीटरों पर ध्यान केंद्रित करेंगे।

## CameraCharacteristics क्या है?

CameraCharacteristics एक अपरिवर्तनीय ऑब्जेक्ट है जिसमें कैमरा डिवाइस के बारे में सभी मेटाडेटा होता है। यह वर्णन करता है:

- **हार्डवेयर गुण** — सेंसर का आकार, लेंस विशेषताएं
- **क्षमताएं** — कैमरा क्या कर सकता है
- **मोड** — उपलब्ध फोकस, एक्सपोजर और व्हाइट बैलेंस मोड
- **आउटपुट विकल्प** — समर्थित रिज़ॉल्यूशन और प्रारूप
- **प्रदर्शन** — फ्रेम दर, एक्सपोजर रेंज

आप CameraManager से CameraCharacteristics प्राप्त करते हैं:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## प्रमुख CameraCharacteristics कीज

आइए सबसे महत्वपूर्ण विशेषताओं का अन्वेषण करें।

### 1. लेंस की दिशा (Lens Facing)

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

संभावित मान:
- `LENS_FACING_FRONT` — फ्रंट-फेसिंग कैमरा (सेल्फी)
- `LENS_FACING_BACK` — रियर-फेसिंग कैमरा
- `LENS_FACING_EXTERNAL` — बाहरी कैमरा

### 2. हार्डवेयर स्तर (Hardware Level)

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

यह सबसे महत्वपूर्ण विशेषताओं में से एक है:

| स्तर | API स्तर | विशेषताएं |
| --- | --- | --- |
| **LEGACY** | 21 | सीमित Camera2 समर्थन, पुराने Camera API की लपेट |
| **LIMITED** | 21 | बुनियादी Camera2 विशेषताएं, कोई मैनुअल नियंत्रण नहीं |
| **FULL** | 21 | पूर्ण मैनुअल नियंत्रण, RAW कैप्चर |
| **LEVEL_3** | 24 | YUV रीप्रोसेसिंग जैसी उन्नत विशेषताएं |

### 3. सेंसर का आकार (Sensor Size)

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width और sensorSize.height आयाम देते हैं
```

सेंसर का आकार आपको बताता है कि सेंसर में कितने पिक्सेल हैं। यह इमेज रिज़ॉल्यूशन से अलग है — सेंसर में एक कैप्चर में उपयोग किए जाने वाले पिक्सेल से अधिक पिक्सेल हो सकते हैं।

### 4. सक्रिय ऐरे का आकार (Active Array Size)

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

सक्रिय ऐरे सेंसर का वह वास्तविक क्षेत्र है जिसका उपयोग इमेज कैप्चर करने के लिए किया जाता है। यह आमतौर पर पिक्सेल ऐरे से थोड़ा छोटा होता है क्योंकि कुछ पिक्सेल कैलिब्रेशन के लिए आरक्षित होते हैं।

### 5. उपलब्ध क्षमताएं (Available Capabilities)

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

यह ऐरे आपको बताता है कि कैमरा कौन सी विशेषताएं सपोर्ट करता है:
- `BACKWARD_COMPATIBLE` — बुनियादी संगतता
- `MANUAL_SENSOR` — मैनुअल सेंसर नियंत्रण
- `MANUAL_POST_PROCESSING` — मैनुअल पोस्ट-प्रोसेसिंग
- `RAW` — RAW कैप्चर समर्थन
- `BURST_CAPTURE` — बर्स्ट कैप्चर
- `YUV_REPROCESSING` — YUV रीप्रोसेसिंग
- `DEPTH_OUTPUT` — डेप्थ आउटपुट
- `CONSTRAINED_HIGH_SPEED_VIDEO` — हाई-स्पीड वीडियो

### 6. आउटपुट प्रारूप (Output Formats)

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

स्ट्रीम कॉन्फ़िगरेशन मैप में कैमरा द्वारा समर्थित सभी आउटपुट प्रारूप और आकार होते हैं:
- `ImageFormat.JPEG` — मानक JPEG
- `ImageFormat.RAW_SENSOR` — RAW सेंसर डेटा
- `ImageFormat.YUV_420_888` — YUV प्रारूप
- `ImageFormat.RAW10` — 10-बिट RAW
- `ImageFormat.RAW12` — 12-बिट RAW

### 7. समर्थित पूर्वावलोकन आकार (Supported Preview Sizes)

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

यह आपको कैमरे के लिए सभी उपलब्ध पूर्वावलोकन रिज़ॉल्यूशन देता है।

### 8. समर्थित पिक्चर आकार (Supported Picture Sizes)

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

ये स्टिल इमेज कैप्चर के लिए उपलब्ध रिज़ॉल्यूशन हैं।

### 9. फोकल लंबाई (Focal Lengths)

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

इस ऐरे में लेंस की फोकल लंबाई (मिलीमीटर में) होती है। एकाधिक मान ऑप्टिकल ज़ूम क्षमताओं को इंगित करते हैं।

### 10. फोकस दूरी सीमा (Focus Distance Range)

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

न्यूनतम फोकस दूरी आपको बताती है कि कैमरा कितना करीब फोकस कर सकता है। कम मान का मतलब बेहतर मैक्रो क्षमता है।

## एक व्यावहारिक उदाहरण

आइए एक अधिक विस्तृत कैमरा जानकारी ऐप बनाएं:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Lens facing
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        else -> "External"
    }
    
    // Hardware level
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Unknown"
    }
    
    // Sensor size
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Active array size
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Focal lengths
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Unknown"
    
    // Available capabilities
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
            else -> "Unknown capability"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Camera $cameraId ===")
    Log.d("CameraDetails", "Lens Facing: $lensFacingStr")
    Log.d("CameraDetails", "Hardware Level: $hardwareLevelStr")
    Log.d("CameraDetails", "Sensor Size: $sensorSizeStr")
    Log.d("CameraDetails", "Active Array: $activeArrayStr")
    Log.d("CameraDetails", "Focal Lengths: $focalLengthsStr")
    Log.d("CameraDetails", "Capabilities: ${capabilitiesList.joinToString(", ")}")
}
```

## आउटपुट उदाहरण

```
=== Camera 0 ===
Lens Facing: Back
Hardware Level: FULL
Sensor Size: 4032 x 3024
Active Array: 4000 x 3000
Focal Lengths: 2.4mm, 4.8mm
Capabilities: Backward Compatible, Manual Sensor, RAW Capture, Burst Capture
```

## CameraCharacteristics क्यों महत्वपूर्ण है

कैमरा खोलने या कैप्चर सेशन बनाने से पहले, आपको CameraCharacteristics की जांच **करनी ही चाहिए**:

1. **क्षमताओं को सत्यापित करें** — यह मानकर न चलें कि कोई विशेषता समर्थित है
2. **सही कैमरा चुनें** — लेंस की दिशा, हार्डवेयर स्तर आदि के आधार पर चयन करें
3. **आउटपुट कॉन्फ़िगर करें** — समर्थित रिज़ॉल्यूशन और प्रारूप का उपयोग करें
4. **डिवाइस के अंतर को संभालें** — जो एक डिवाइस पर काम करता है वह दूसरे पर काम नहीं कर सकता

## Android Camera Parameters के साथ अन्वेषण करें

Android Camera Parameters ऐप खोलें और विशेषताओं को ब्राउज़ करें। आपको श्रेणी के अनुसार व्यवस्थित सैकड़ों पैरामीटर दिखाई देंगे:

- **कैमरा जानकारी** — बुनियादी कैमरा जानकारी
- **सेंसर** — सेंसर विशेषताएं
- **लेंस** — लेंस गुण
- **नियंत्रण** — ऑटो-एक्सपोजर, ऑटो-फोकस, व्हाइट बैलेंस
- **स्केलर** — आउटपुट आकार और प्रारूप
- **फ्लैश** — फ्लैश क्षमताएं
- **आंकड़े** — आंकड़े आउटपुट

यह आपको अपने कैमरे की क्षमताओं का संपूर्ण चित्र प्रदान करता है।

## अगला अध्याय

अब जब आप CameraCharacteristics को समझ गए हैं, तो आप अपना पहला कैमरा खोलने के लिए तैयार हैं! अगले अध्याय में, हम:

1. CameraDevice के बारे में सीखेंगे
2. CameraManager का उपयोग करके कैमरा खोलेंगे
3. कैमरा स्टेट कॉलबैक्स को संभालेंगे
4. कैमरा लाइफसाइकल को समझेंगे

## सारांश

CameraCharacteristics में वह सभी जानकारी है जो आपको कैमरे की क्षमताओं को समझने के लिए चाहिए:

- **लेंस की दिशा** — फ्रंट, बैक या एक्सटर्नल
- **हार्डवेयर स्तर** — LEGACY, LIMITED, FULL, LEVEL_3
- **सेंसर का आकार** — भौतिक आयाम
- **सक्रिय ऐरे** — कैप्चर क्षेत्र
- **फोकल लंबाई** — लेंस क्षमताएं
- **क्षमताएं** — समर्थित विशेषताएं
- **आउटपुट प्रारूप** — उपलब्ध इमेज प्रारूप

कैमरा उपयोग करने से पहले हमेशा CameraCharacteristics की जांच करें। यह सुनिश्चित करता है कि आपका ऐप अलग-अलग डिवाइसों पर काम करता है।

अगले अध्याय में, हम CameraDevice का उपयोग करके अपना पहला कैमरा खोलेंगे।
