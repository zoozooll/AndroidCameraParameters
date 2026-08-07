---
sidebar_position: 9
title: "अध्याय 9: फोटो लेना"
description: Camera2 के साथ ImageReader (JPEG), प्रीकैप्चर AE ट्रिगर, और CaptureCallback स्टेट मशीन का उपयोग करके उच्च-गुणवत्ता वाली स्थिर फोटो कैप्चर करें।
keywords: [ImageReader, JPEG capture, precapture AE trigger, MediaStore Scoped Storage, CaptureCallback still photo, हिंदी]
---

# अध्याय 9: फोटो लेना (Taking Photos)

अध्‍याय 9 तक, आपके ऐप में अनुमति हैंडलिंग, थ्रेडिंग, कैमरा डिस्कवरी, और लाइव प्रीव्यू है। अब इसमें **शटर बटन टैप करने और फोटो को सुरक्षित रखने** की क्षमता जोड़ने का समय है।

अंतिम अध्याय के अंत तक, आपका ट्यूटोरियल प्रोजेक्ट एक वास्तविक कैमरा एप्लिकेशन बन जाएगा: शटर टैप करें, एक स्थिर इमेज कैप्चर होगी, उसे गैलरी में सेव किया जाएगा और प्रीव्यू फिर से शुरू हो जाएगा।

## फोटो लेना प्रीव्यू से अधिक जटिल क्यों है?

1. **रेज़ोल्यूशन**: प्रीव्यू ~2 MP का होता है, जबकि फोटो सेंसर के **अधिकतम** रेज़ोल्यूशन (अक्सर 50+ MP) पर होनी चाहिए।
2. **एक्सपोज़र**: `TEMPLATE_STILL_CAPTURE` सर्वोत्तम गुणवत्ता के लिए ISP प्रोसेसिंग को ऑप्टिमाइज़ करता है।
3. **3A कन्वर्जेंस**: फोटो लेने से पहले, कैमरे को एक्सपोज़र, फोकस और व्हाइट बैलेंस को "लॉक" करने के लिए कहना पड़ता है। इसे **प्रीकैप्चर ट्रिगर (precapture trigger)** अनुक्रम कहते हैं।
4. **स्टोरेज**: फोटो को डिस्क पर JPEG फाइल के रूप में लिखना पड़ता है।

यह एक **मल्टी-स्टेज असिंक्रोनस स्टेट मशीन** है।

## ImageReader: CPU-सुलभ फ्रेम सिंक (CPU-Accessible Frame Sink)

प्रीव्यू के लिए हमने `SurfaceTexture` (GPU सिंक) का उपयोग किया था। फोटो कैप्चर के लिए हमें `ImageReader` की आवश्यकता है ताकि हम JPEG बाइट्स को डिस्क पर लिख सकें।

```kotlin
val imageReader = ImageReader.newInstance(
    width,           // सेंसर का अधिकतम JPEG साइज
    height,
    ImageFormat.JPEG,// JPEG फॉर्मेट
    2                // बफर काउंट
)
```

### ⚠️ महत्वपूर्ण नियम: हमेशा Image को बंद करें
यदि आप `image.close()` कॉल करना भूल जाते हैं, तो बफर स्थायी रूप से ब्लॉक हो जाएगा और आपका कैमरा दोबारा फोटो नहीं ले पाएगा। हमेशा `finally` ब्लॉक में `image.close()` का उपयोग करें।

## प्रीकैप्चर AE स्टेट मशीन (The Precapture AE State Machine)

1. **प्रीव्यू रोकें**: `captureSession.stopRepeating()`.
2. **प्रीकैप्चर ट्रिगर फायर करें**: एक `CaptureRequest` सबमिट करें जिसमें `CONTROL_AE_PRECAPTURE_TRIGGER` को `START` पर सेट किया गया हो।
3. **कन्वर्जेंस का इंतज़ार करें**: `CaptureResult.CONTROL_AE_STATE` के `CONVERGED` होने का इंतज़ार करें।
4. **फोटो कैप्चर करें**: `TEMPLATE_STILL_CAPTURE` रिक्वेस्ट सबमिट करें जो `ImageReader` को टारगेट करे।
5. **इमेज सेव करें**: JPEG बाइट्स प्राप्त करें और उन्हें डिस्क पर लिखें।
6. **अनलॉक करें और फिर से शुरू करें**: AE ट्रिगर को `CANCEL` करें और प्रीव्यू फिर से शुरू करें।

## Scoped Storage और MediaStore (Android 10+)

Android 10 से, ऐप्स सीधे `/sdcard/Pictures` में फाइलें नहीं लिख सकते। इसके बजाय हमें `MediaStore` API का उपयोग करना चाहिए:

```kotlin
val values = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${timestamp}.jpg")
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Camera2Tutorial")
    put(MediaStore.Images.Media.IS_PENDING, 1)
}
val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
// बाइट्स लिखें और IS_PENDING को 0 करें
```

## पूर्ण अध्याय 9 कोड — फोटो कैप्चर

यहाँ `MainActivity.kt` में जोड़ा गया नया लॉजिक है:

```kotlin
// ... (ImageReader सेटअप और शटर बटन क्लिक)

    private fun captureStillPicture() {
        val stillBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(imageReader.surface)
            set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)
            set(CaptureRequest.JPEG_QUALITY, 95.toByte())
        }

        captureSession.stopRepeating()
        captureSession.capture(stillBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(s: CameraCaptureSession, req: CaptureRequest, res: TotalCaptureResult) {
                // फोटो कैप्चर हो गया
            }
        }, backgroundHandler)
    }

    private val onJpegAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            savePhotoToStorage(bytes) // फोटो सेव करें
        } finally {
            image?.close() // ⚠️ बेहद जरूरी!
        }
        unlockFocusAndResumePreview()
    }
```

## सारांश (Summary)

इस अध्याय में, आपने सीखा:
1. **ImageReader**: JPEG फोटो के लिए CPU-सुलभ बफर।
2. **स्टेट मशीन**: फोटो लेने से पहले एक्सपोज़र को कैसे लॉक करें।
3. **JPEG सेटिंग्स**: क्वालिटी और ओरिएंटेशन कैसे सेट करें।
4. **Scoped Storage**: आधुनिक Android पर फोटो कैसे सेव करें।

## आगे क्या है

भाग II यहाँ समाप्त होता है। अब आपके पास एक काम करने वाला कैमरा ऐप है। **भाग III (अध्याय 10-12)** में, हम Camera2 के आंतरिक आर्किटेक्चर और उन्नत मेटाडेटा (CameraCharacteristics) में गहराई से उतरेंगे।
