---
sidebar_position: 15
title: "अध्याय 15: CameraCharacteristics विश्वकोश"
description: सबसे महत्वपूर्ण Camera2 विशेषताओं की एक व्यापक गाइड, जिसमें उनका अर्थ, वे क्यों मौजूद हैं, और उनका उपयोग कैसे करें।
keywords: [CameraCharacteristics, कैमरा पैरामीटर, कैमरा क्षमताएं, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

CameraCharacteristics विश्वकोश में आपका स्वागत है — हर कैमरा पैरामीटर को समझने के लिए आपका गाइड।

## परिचय

CameraCharacteristics में सैकड़ों पैरामीटर होते हैं जो कैमरे की क्षमताओं का वर्णन करते हैं। इस अध्याय में, हम सबसे महत्वपूर्ण पैरामीटरों की गहराई से खोज करेंगे:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — कैमरा क्या कर सकता है?
2. `REQUEST_AVAILABLE_CAPABILITIES` — कौन सी सुविधाएं उपलब्ध हैं?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — सेंसर का आकार क्या है?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — कितना ज़ूम?
5. `CONTROL_AE_AVAILABLE_MODES` — कौन से एक्सपोजर मोड?

और भी कई...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**इसका क्या मतलब है?**  
यह सबसे महत्वपूर्ण विशेषता है। यह कैमरा डिवाइस के समग्र क्षमता स्तर को परिभाषित करता है।

**यह क्यों मौजूद है?**  
विभिन्न Android डिवाइसों में विभिन्न कैमरा क्षमताएं होती हैं। यह पैरामीटर एप्लिकेशन को यह समझने में मदद करता है कि वे क्या कर सकते हैं।

**समर्थित मान:**

| मान | API स्तर | विवरण |
| --- | --- | --- |
| `LEGACY` | 21 | पुराने डिवाइस, Camera2 API पुराने Camera API पर एक रैपर है |
| `LIMITED` | 21 | बुनियादी Camera2 सुविधाएं, कोई मैनुअल नियंत्रण नहीं |
| `FULL` | 21 | पूर्ण मैनुअल नियंत्रण, RAW कैप्चर, बर्स्ट कैप्चर |
| `LEVEL_3` | 24 | उन्नत सुविधाएं जैसे YUV रीप्रोसेसिंग, 10-bit HDR |

**इसका उपयोग कैसे किया जाता है?**  
कोई भी उन्नत ऑपरेशन करने से पहले इसे जांचें:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // सीमित कार्यक्षमता
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // केवल बुनियादी सुविधाएं
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // पूर्ण मैनुअल नियंत्रण उपलब्ध
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // उन्नत सुविधाएं उपलब्ध
    }
}
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
एप्लिकेशन खोलें और Camera Info सेक्शन में "Hardware Level" खोजें।

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**इसका क्या मतलब है?**  
यह ऐरे कैमरे द्वारा समर्थित सभी क्षमताओं को सूचीबद्ध करता है।

**यह क्यों मौजूद है?**  
एक ही हार्डवेयर स्तर के भीतर भी, विभिन्न डिवाइस विभिन्न सुविधाओं का समर्थन कर सकते हैं।

**सामान्य क्षमताएं:**

| क्षमता | विवरण |
| --- | --- |
| `BACKWARD_COMPATIBLE` | बुनियादी संगतता मोड |
| `MANUAL_SENSOR` | मैनुअल ISO और एक्सपोजर नियंत्रण |
| `MANUAL_POST_PROCESSING` | मैनुअल कलर करेक्शन और नॉइज़ रिडक्शन |
| `RAW` | RAW इमेज कैप्चर |
| `BURST_CAPTURE` | हाई-स्पीड बर्स्ट कैप्चर |
| `YUV_REPROCESSING` | YUV इमेज रीप्रोसेसिंग |
| `DEPTH_OUTPUT` | डेप्थ मैप आउटपुट |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | हाई-स्पीड वीडियो कैप्चर |
| `LOGICAL_MULTI_CAMERA` | कई भौतिक कैमरों को जोड़ने वाला लॉजिकल कैमरा |
| `CONCURRENT_CAMERA` | एक साथ कई कैमरे खोले जा सकते हैं |
| `CAMERA_EXTENSION` | निर्माता-विशिष्ट एक्सटेंशन (पोर्ट्रेट, नाइट मोड) |

**इसका उपयोग कैसे किया जाता है?**  
किसी सुविधा का उपयोग करने से पहले क्षमताओं की जांच करें:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // RAW कैप्चर सक्षम करें
}
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Camera Info सेक्शन में "Available Capabilities" खोजें।

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**इसका क्या मतलब है?**  
एक्टिव ऐरे सेंसर का वह वास्तविक क्षेत्र है जिसका उपयोग इमेज कैप्चर करने के लिए किया जाता है।

**यह क्यों मौजूद है?**  
सेंसर के किनारों के चारों ओर पिक्सेल हो सकते हैं जो कैलिब्रेशन के लिए आरक्षित होते हैं। एक्टिव ऐरे उपयोग योग्य क्षेत्र का प्रतिनिधित्व करता है।

**इसका उपयोग कैसे किया जाता है?**  
यह आपको बताता है कि कैप्चर के लिए अधिकतम रिज़ॉल्यूशन क्या उपलब्ध है:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**संबंधित विशेषताएं:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — सेंसर पर कुल पिक्सेल (एक्टिव ऐरे से बड़ा हो सकता है)
- `SENSOR_INFO_SENSOR_SIZE` — मिलीमीटर में भौतिक आयाम

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Sensor सेक्शन में "Active Array Size" और "Sensor Size" खोजें।

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**इसका क्या मतलब है?**  
कैमरे द्वारा समर्थित अधिकतम डिजिटल ज़ूम फैक्टर।

**यह क्यों मौजूद है?**  
डिजिटल ज़ूम इमेज को क्रॉप और बड़ा करता है, जिससे गुणवत्ता कम हो जाती है। अधिकतम जानना उपयोगकर्ता की अपेक्षाओं को प्रबंधित करने में मदद करता है।

**इसका उपयोग कैसे किया जाता है?**  
कैप्चर अनुरोधों में ज़ूम स्तर सेट करें:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// ज़ूम सेट करें (1.0 = कोई ज़ूम नहीं, maxZoom = अधिकतम ज़ूम)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**संबंधित विशेषताएं:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — भौतिक फोकल लेंथ (ऑप्टिकल ज़ूम के लिए)

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Scaler सेक्शन में "Max Digital Zoom" खोजें।

---

## 5. CONTROL_AE_AVAILABLE_MODES

**इसका क्या मतलब है?**  
उपलब्ध ऑटो-एक्सपोजर मोड।

**यह क्यों मौजूद है?**  
विभिन्न डिवाइस विभिन्न AE रणनीतियों का समर्थन करते हैं।

**सामान्य मोड:**

| मोड | विवरण |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | मैनुअल एक्सपोजर नियंत्रण |
| `CONTROL_AE_MODE_ON` | ऑटो-एक्सपोजर |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | हमेशा फ्लैश ऑन के साथ ऑटो-एक्सपोजर |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | स्वचालित फ्लैश के साथ ऑटो-एक्सपोजर |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | रेड-ई रिडक्शन के साथ ऑटो-एक्सपोजर |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | बाहरी फ्लैश के साथ ऑटो-एक्सपोजर |

**इसका उपयोग कैसे किया जाता है?**  
कैप्चर अनुरोधों में AE मोड सेट करें:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Control सेक्शन में "AE Available Modes" खोजें।

---

## 6. CONTROL_AF_AVAILABLE_MODES

**इसका क्या मतलब है?**  
उपलब्ध ऑटोफोकस मोड।

**यह क्यों मौजूद है?**  
विभिन्न परिदृश्यों के लिए विभिन्न फोकस रणनीतियाँ।

**सामान्य मोड:**

| मोड | विवरण |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | मैनुअल फोकस |
| `CONTROL_AF_MODE_AUTO` | सिंगल-शॉट ऑटोफोकस |
| `CONTROL_AF_MODE_MACRO` | मैक्रो फोकस |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | वीडियो के लिए निरंतर ऑटोफोकस |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | तस्वीरों के लिए निरंतर ऑटोफोकस |
| `CONTROL_AF_MODE_EDGE` | एज ऑटोफोकस |
| `CONTROL_AF_MODE_FIXED` | फिक्स्ड फोकस (कोई AF नहीं) |

**इसका उपयोग कैसे किया जाता है?**  
अपने उपयोग के मामले के आधार पर AF मोड सेट करें:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Control सेक्शन में "AF Available Modes" खोजें।

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**इसका क्या मतलब है?**  
उपलब्ध ऑटो-व्हाइट बैलेंस मोड।

**सामान्य मोड:**

| मोड | विवरण |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | मैनुअल व्हाइट बैलेंस |
| `CONTROL_AWB_MODE_AUTO` | स्वचालित |
| `CONTROL_AWB_MODE_INCANDESCENT` | टंगस्टन लाइटिंग |
| `CONTROL_AWB_MODE_FLUORESCENT` | फ्लोरोसेंट लाइटिंग |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | गर्म फ्लोरोसेंट |
| `CONTROL_AWB_MODE_DAYLIGHT` | दिन की रोशनी |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | बादल |

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Control सेक्शन में "AWB Available Modes" खोजें।

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**इसका क्या मतलब है?**  
लेंस(के लिए उपलब्ध फोकल लेंथ।

**यह क्यों मौजूद है?**  
कई मान ऑप्टिकल ज़ूम क्षमताओं को इंगित करते हैं।

**इसका उपयोग कैसे किया जाता है?**  
निर्धारित करें कि कौन से लेंस उपलब्ध हैं:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**सामान्य फोकल लेंथ:**
- 2.4mm — वाइड-एंगल (सामान्य)
- 4.8mm — टेलीफोटो (2x ऑप्टिकल ज़ूम)
- 1.8mm — अल्ट्रा-वाइड

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Lens सेक्शन में "Available Focal Lengths" खोजें।

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**इसका क्या मतलब है?**  
निकटतम दूरी जिस पर लेंस फोकस कर सकता है।

**यह क्यों मौजूद है?**  
कम मान बेहतर मैक्रो क्षमता का मतलब है।

**इसका उपयोग कैसे किया जाता है?**  
जांचें कि मैक्रो फोटोग्राफी संभव है या नहीं:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// 0.1m (10cm) या उससे कम का मान अच्छी मैक्रो क्षमता को इंगित करता है
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Lens सेक्शन में "Minimum Focus Distance" खोजें।

---

## 10. FLASH_INFO_AVAILABLE

**इसका क्या मतलब है?**  
कैमरे में फ्लैश है या नहीं।

**यह क्यों मौजूद है?**  
सभी कैमरों में फ्लैश नहीं होता है (विशेषकर फ्रंट कैमरे)।

**इसका उपयोग कैसे किया जाता है?**  
फ्लैश का उपयोग करने से पहले जांचें:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // फ्लैश सुविधाएं सक्षम करें
}
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Flash सेक्शन में "Flash Available" खोजें।

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**इसका क्या मतलब है?**  
समर्थित न्यूनतम और अधिकतम एक्सपोजर समय।

**यह क्यों मौजूद है?**  
कम रोशनी की क्षमता और मोशन फ्रीज क्षमता निर्धारित करता है।

**इसका उपयोग कैसे किया जाता है?**  
मैनुअल नियंत्रण के लिए एक्सपोजर रेंज जांचें:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// प्रदर्शन के लिए सेकंड में कनवर्ट करें
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Sensor सेक्शन में "Exposure Time Range" खोजें।

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**इसका क्या मतलब है?**  
समर्थित न्यूनतम और अधिकतम ISO मान।

**यह क्यों मौजूद है?**  
कम रोशनी की क्षमता और नॉइज़ प्रदर्शन निर्धारित करता है।

**इसका उपयोग कैसे किया जाता है?**  
मैनुअल नियंत्रण के लिए ISO रेंज जांचें:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Sensor सेक्शन में "Sensitivity Range" खोजें।

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**इसका क्या मतलब है?**  
सभी समर्थित आउटपुट आकार और प्रारूप।

**यह क्यों मौजूद है?**  
निर्धारित करता है कि आप कौन से रिज़ॉल्यूशन और प्रारूप उपयोग कर सकते हैं।

**इसका उपयोग कैसे किया जाता है?**  
विभिन्न उपयोग के मामलों के लिए समर्थित आकार प्राप्त करें:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// पूर्वावलोकन आकार
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// तस्वीर के आकार
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// वीडियो आकार
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// RAW आकार
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Scaler सेक्शन में "Preview Sizes", "Picture Sizes", आदि खोजें।

---

## 14. LENS_FACING

**इसका क्या मतलब है?**  
लेंस किस दिशा की ओर है।

**यह क्यों मौजूद है?**  
निर्धारित करता है कि यह फ्रंट, बैक, या बाहरी कैमरा है।

**मान:**
- `LENS_FACING_FRONT` — सेल्फी कैमरा
- `LENS_FACING_BACK` — रियर कैमरा  
- `LENS_FACING_EXTERNAL` — बाहरी कैमरा

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Camera Info सेक्शन में "Lens Facing" खोजें।

---

## 15. CONTROL_MAX_REGIONS_AE

**इसका क्या मतलब है?**  
AE मीटरिंग क्षेत्रों की अधिकतम संख्या।

**यह क्यों मौजूद है?**  
निर्धारित करता है कि एक्सपोजर मीटरिंग कितनी सटीक हो सकती है।

**इसका उपयोग कैसे किया जाता है?**  
आपके द्वारा बनाए गए AE क्षेत्रों की संख्या को सीमित करें:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// maxAERegions से अधिक नहीं बनाएं
```

**Android Camera Parameters के साथ कैसे सत्यापित करें:**  
Control सेक्शन में "Max Regions AE" खोजें।

---

## निष्कर्ष

CameraCharacteristics कैमरे की क्षमताओं में आपकी खिड़की है। इन पैरामीटरों को समझकर, आप:

1. **डिवाइस-अज्ञेय एप्लिकेशन बनाएं** — सुविधाओं का उपयोग करने से पहले क्षमताओं की जांच करें
2. **बेहतर उपयोगकर्ता अनुभव प्रदान करें** — केवल उपलब्ध सुविधाएं दिखाएं
3. **प्रदर्शन का अनुकूलन करें** — उपयुक्त रिज़ॉल्यूशन और प्रारूप चुनें
4. **पेशेवर एप्लिकेशन बनाएं** — कैमरे की पूरी क्षमता को अनलॉक करें

## और अधिक सीखने के लिए

1. **Android Camera Parameters एप्लिकेशन** — अपने डिवाइस से वास्तविक डेटा देखें
2. **Android डॉक्यूमेंटेशन** — आधिकारिक CameraCharacteristics दस्तावेज़ पढ़ें
3. **प्रयोग करें** — विभिन्न पैरामीटरों को आज़माने के लिए छोटे टेस्ट एप्लिकेशन लिखें
4. **स्रोत कोड** — गहरी समझ के लिए Camera2 स्रोत कोड देखें

## सारांश

इस अध्याय में सबसे महत्वपूर्ण CameraCharacteristics को शामिल किया गया है:

1. **Hardware Level** — समग्र क्षमता
2. **Capabilities** — विशिष्ट सुविधाएं उपलब्ध
3. **Active Array** — सेंसर रिज़ॉल्यूशन
4. **Digital Zoom** — ज़ूम क्षमताएं
5. **AE/AF/AWB Modes** — ऑटो-नियंत्रण मोड
6. **Focal Lengths** — लेंस क्षमताएं
7. **Focus Distance** — मैक्रो क्षमता
8. **Flash** — फ्लैश उपलब्धता
9. **Exposure/ISO Range** — मैनुअल नियंत्रण सीमाएं
10. **Stream Configuration** — समर्थित आकार और प्रारूप

इस ज्ञान के साथ, आप उन्नत Camera2 एप्लिकेशन बनाने के लिए तैयार हैं!

---

## अंतिम शब्द

बधाई हो! आपने इस Android Camera2 श्रृंखला को पूरा कर लिया है। अब आप समझते हैं:

- **स्मार्टफोन कैमरे कैसे काम करते हैं** — लेंस, सेंसर, ISP
- **Camera2 कैसे काम करता है** — CameraManager, CameraDevice, CaptureSession
- **तस्वीरें कैसे कैप्चर करें** — JPEG, RAW, ImageReader
- **कैमरे को कैसे नियंत्रित करें** — ISO, एक्सपोजर, फोकस, व्हाइट बैलेंस
- **पेशेवर सुविधाएं** — हाई-स्पीड वीडियो, मल्टी-कैमरा
- **कैमरा विशेषताएं** — कैमरा क्षमताओं का विश्वकोश

Android Camera Parameters एप्लिकेशन सीखना जारी रखने का एक बढ़िया टूल है। अपने डिवाइस की क्षमताओं का अन्वेषण करें और विभिन्न सेटिंग्स के साथ प्रयोग करें।

सुखद कोडिंग! 📸
