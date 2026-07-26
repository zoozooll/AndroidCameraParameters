---
sidebar_position: 11
title: "अध्याय 11: RAW कैप्चर और CaptureRequest"
description: RAW इमेज कैप्चर करने का तरीका सीखें और उन्नत कैमरा नियंत्रण के लिए CaptureRequest और CaptureResult को समझें।
keywords: [RAW कैप्चर, CaptureRequest, CaptureResult, Camera2, मैनुअल नियंत्रण]
---

RAW कैप्चर आपको इमेज प्रोसेसिंग पर पूर्ण नियंत्रण देता है। आइए इसे CaptureRequest और CaptureResult के साथ एक्सप्लोर करें।

## परिचय

पिछले अध्याय में, आपने JPEG फोटो कैप्चर करना सीखा था। अब हम एक्सप्लोर करेंगे:

1. **RAW कैप्चर** — अप्रोसेस्ड सेंसर डेटा कैप्चर करना
2. **CaptureRequest** — प्रत्येक कैप्चर के लिए कैमरा सेटिंग्स कॉन्फिगर करना
3. **CaptureResult** — पूर्ण हुए कैप्चर के बारे में मेटाडेटा प्राप्त करना

## RAW क्या है?

RAW इमेज में ISP प्रोसेसिंग से पहले सेंसर द्वारा कैप्चर किया गया सारा डेटा होता है। इसका मतलब है:

- कोई नॉइज़ रिडक्शन लागू नहीं
- कोई व्हाइट बैलेंस करेक्शन नहीं
- कोई शार्पनिंग नहीं
- पूर्ण डायनेमिक रेंज

RAW फाइलें बड़ी होती हैं लेकिन अप्रतिम संपादन लचीलापन प्रदान करती हैं।

## RAW कैप्चर आवश्यकताएं

RAW इमेज कैप्चर करने के लिए, आपके कैमरा को चाहिए:
1. **FULL** या **LEVEL_3** हार्डवेयर लेवल होना चाहिए
2. `RAW` क्षमता को सपोर्ट करना चाहिए

CameraCharacteristics जांचें:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## RAW इमेज कैप्चर करना

RAW फॉर्मेट के साथ ImageReader बनाएं:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // या ImageFormat.RAW10/RAW12
    2
)
```

फिर एक साथ कैप्चर करने के लिए JPEG और RAW दोनों सर्फेस को कैप्चर सेशन में जोड़ें:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest एक कैप्चर के लिए सभी सेटिंग्स को परिभाषित करता है। आप कॉन्फिगर कर सकते हैं:

### ऑटो नियंत्रण
- `CONTROL_AF_MODE` — ऑटोफोकस मोड
- `CONTROL_AE_MODE` — ऑटो-एक्सपोजर मोड
- `CONTROL_AWB_MODE` — ऑटो-व्हाइट बैलेंस मोड

### मैनुअल नियंत्रण
- `SENSOR_SENSITIVITY` — ISO मान
- `SENSOR_EXPOSURE_TIME` — नैनोसेकंड में एक्सपोजर समय
- `LENS_FOCUS_DISTANCE` — फोकस दूरी
- `LENS_APERTURE` — एपर्चर (यदि उपलब्ध हो)

### आउटपुट सेटिंग्स
- `JPEG_QUALITY` — JPEG कम्प्रेशन क्वालिटी
- `JPEG_ORIENTATION` — इमेज ओरिएंटेशन
- `COLOR_CORRECTION_MODE` — कलर करेक्शन मोड

### CaptureRequest बनाना

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// ऑटो नियंत्रण सेट करें
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// JPEG क्वालिटी सेट करें
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// टारगेट जोड़ें
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// रिक्वेस्ट बनाएं
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult में पूर्ण हुए कैप्चर के बारे में मेटाडेटा होता है। इसमें शामिल हैं:

- **वास्तविक सेटिंग्स का उपयोग किया गया** — कैमरा ने वास्तव में क्या लागू किया
- **स्टेटिस्टिक्स** — एक्सपोजर, फोकस और कलर जानकारी
- **टाइमस्टैम्प** — कैप्चर कब हुआ

### CaptureResult प्राप्त करना

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // वास्तविक एक्सपोजर समय प्राप्त करें
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // वास्तविक ISO प्राप्त करें
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // फोकस स्टेट प्राप्त करें
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // AE स्टेट प्राप्त करें
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "एक्सपोजर: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### सामान्य CaptureResult कीज

| की | विवरण |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | उपयोग किया गया वास्तविक एक्सपोजर समय |
| `SENSOR_SENSITIVITY` | उपयोग किया गया वास्तविक ISO |
| `CONTROL_AF_STATE` | ऑटोफोकस स्टेट |
| `CONTROL_AE_STATE` | ऑटो-एक्सपोजर स्टेट |
| `CONTROL_AWB_STATE` | ऑटो-व्हाइट बैलेंस स्टेट |
| `SCALER_CROP_REGION` | उपयोग किया गया क्रॉप क्षेत्र |
| `COLOR_CORRECTION_GAINS` | कलर करेक्शन गेन |

## एक पूर्ण RAW कैप्चर उदाहरण

```kotlin
private fun configureDualCapture() {
    // JPEG ImageReader बनाएं
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // RAW ImageReader बनाएं
    val rawReader = ImageReader.newInstance(
        rawSize.width, rawSize.height,
        ImageFormat.RAW_SENSOR, 2
    )
    
    jpegReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveJpeg(image)
        image.close()
    }, null)
    
    rawReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveRaw(image)
        image.close()
    }, null)
    
    // सर्फेस बनाएं
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // सभी सर्फेस के साथ कैप्चर सेशन बनाएं
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // दोनों सर्फेस को टारगेट के रूप में जोड़ें
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // नियंत्रण कॉन्फिगर करें
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.JPEG_QUALITY, 100)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.capture(builder.build(), captureCallback, null)
    }
}

private fun saveRaw(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val fileName = "RAW_${System.currentTimeMillis()}.dng"
    val file = File(externalCacheDir, fileName)
    
    FileOutputStream(file).use { fos ->
        fos.write(bytes)
    }
}
```

## RAW बनाम JPEG

| विशेषता | RAW | JPEG |
| --- | --- | --- |
| फाइल का आकार | बड़ा (20-50MB) | छोटा (2-10MB) |
| संपादन लचीलापन | अधिकतम | सीमित |
| नॉइज़ | संरक्षित | कम किया गया |
| व्हाइट बैलेंस | समायोज्य | निश्चित |
| डायनेमिक रेंज | पूर्ण | संकुचित |

## सर्वोत्तम अभ्यास

1. **RAW सपोर्ट जांचें** — RAW कैप्चर का प्रयास करने से पहले हमेशा सत्यापित करें
2. **दोहरा कैप्चर** — लचीलापन के लिए JPEG और RAW दोनों कैप्चर करें
3. **इमेज बंद करें** — प्रोसेसिंग के बाद हमेशा `image.close()` कॉल करें
4. **विभिन्न फॉर्मेट संभालें** — RAW_SENSOR, RAW10 और RAW12 के बाइट लेआउट अलग-अलग होते हैं

## अगला अध्याय

अगले अध्याय में, हम भाग III में आपने जो सीखा है उसका सारांश देंगे और भाग IV: मैनुअल कैमरा नियंत्रण के लिए तैयार होंगे।

## सारांश

इस अध्याय में, आपने इनके बारे में सीखा:

1. **RAW कैप्चर** — अधिकतम संपादन लचीलापन के लिए अप्रोसेस्ड सेंसर डेटा कैप्चर करना
2. **CaptureRequest** — प्रत्येक कैप्चर के लिए कैमरा सेटिंग्स कॉन्फिगर करना
3. **CaptureResult** — पूर्ण हुए कैप्चर के बारे में मेटाडेटा प्राप्त करना

RAW कैप्चर के लिए FULL या LEVEL_3 हार्डवेयर लेवल की आवश्यकता होती है। आप कैप्चर सेशन में दोनों सर्फेस जोड़कर एक साथ JPEG और RAW दोनों कैप्चर कर सकते हैं।

CaptureRequest आपको ऑटोफोकस, ऑटो-एक्सपोजर, व्हाइट बैलेंस और ISO और एक्सपोजर समय जैसे मैनुअल नियंत्रण कॉन्फिगर करने की अनुमति देता है। CaptureResult आपको बताता है कि कैमरा द्वारा वास्तव में कौन सी सेटिंग्स का उपयोग किया गया था।

भाग IV में, हम मैनुअल कैमरा नियंत्रण में गहराई से जाएंगे: ISO, एक्सपोजर, फोकस और व्हाइट बैलेंस।
