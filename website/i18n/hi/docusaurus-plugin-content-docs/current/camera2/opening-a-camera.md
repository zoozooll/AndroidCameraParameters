---
sidebar_position: 7
title: "अध्याय 7: कैमरा खोलना"
description: openCamera() का उपयोग करके कैमरा डिवाइस के साथ लाइव कनेक्शन स्थापित करें। CameraDevice लाइफसाइकिल में महारत हासिल करें और StateCallback लागू करें।
keywords: [CameraDevice, StateCallback, openCamera, CameraAccessException, Semaphore concurrency, हिंदी]
---

# अध्याय 7: कैमरा खोलना (Opening a Camera)

आपने डिवाइस पर सभी कैमरों को एन्युमरेट कर लिया है (अध्याय 6), और आपने उस कैमरे की पहचान कर ली है जिसे आप उपयोग करना चाहते हैं। अगला कदम उस कैमरे को **खोलना** है: कैमरा हार्डवेयर के साथ एक सक्रिय, लो-लेवल कनेक्शन स्थापित करना ताकि आप कैप्चर सेशन कॉन्फ़िगर कर सकें और रिक्वेस्ट सबमिट कर सकें।

यदि आप उत्पादन-ग्रेड कैमरा ओपन/क्लोज़ लाइफसाइकिल कोड देखना चाहते हैं, तो **Android Camera Parameters** ऐप का अध्ययन करें ([GitHub](https://github.com/zoozooll/AndroidCameraParameters))। इसकी `Camera2Controller` क्लास एरर रिकवरी और रिट्राय लॉजिक सहित पूरे `CameraDevice` लाइफसाइकिल प्रबंधन को समाहित करती है।

## CameraDevice क्या है?

`CameraDevice` वह Camera2 क्लास है जो **डिवाइस पर एक विशिष्ट फिजिकल (या लॉजिकल) कैमरे के साथ एक सक्रिय, खुले कनेक्शन का प्रतिनिधित्व करती है**। एक बार कैमरा खुल जाने के बाद, आप यह कर सकते हैं:
- `CameraCaptureSession` बनाना (अध्याय 8)
- `CaptureRequest` सबमिट करना (अध्याय 8 और 9)
- फ्रेम आने पर डायनेमिक `CaptureResult` मेटाडेटा पढ़ना
- पेंडिंग रिक्वेस्ट को फ्लश करना, कैप्चर को अबॉर्ट करना और डिवाइस को बंद करना

`CameraDevice` के दो महत्वपूर्ण गुण हैं:
1. **यह एक सिंगल-यूज़र रिसोर्स है।** एक समय में केवल एक ऐप ही किसी कैमरे को खुला रख सकता है।
2. **इसका एक सख्त, कॉलबैक-संचालित लाइफसाइकिल है।**

## StateCallback: CameraDevice की लाइफसाइकिल मशीन

`CameraDevice.StateCallback` एक एब्स्ट्रैक्ट क्लास है जिसमें तीन मेथड हैं जिन्हें आपको **अवश्य** लागू करना चाहिए।

| मेथड | कब कॉल किया जाता है | क्या करना है |
|---|---|---|
| `onOpened(camera: CameraDevice)` | कैमरा सफलतापूर्वक खुल गया है और उपयोग के लिए तैयार है। | `camera` रेफरेंस को स्टोर करें। एक कैप्चर सेशन कॉन्फ़िगर करने के लिए आगे बढ़ें (अध्याय 8)। |
| `onDisconnected(camera: CameraDevice)` | कैमरा आपके ऐप से छीन लिया गया (जैसे, किसी अन्य ऐप ने इसे खोल लिया)। | तुरंत `camera.close()` कॉल करें। रेफरेंस को हटा दें। |
| `onError(camera: CameraDevice, error: Int)` | ओपन के दौरान या कैमरा सक्रिय होने पर एक घातक त्रुटि हुई। | `camera.close()` कॉल करें। रेफरेंस को हटा दें। |

### onError एरर कोड

| स्थिरांक | अर्थ |
|---|---|
| `ERROR_CAMERA_IN_USE` | कैमरा पहले से ही किसी अन्य ऐप द्वारा खुला है। |
| `ERROR_MAX_CAMERAS_IN_USE` | डिवाइस की एक साथ कैमरा खोलने की सीमा समाप्त हो गई है। |
| `ERROR_CAMERA_DISABLED` | डिवाइस पॉलिसी (MDM) ने कैमरों को अक्षम कर दिया है। |
| `ERROR_CAMERA_DEVICE` | कैमरा हार्डवेयर/फर्मवेयर में एक अपरिवर्तनीय त्रुटि हुई। |
| `ERROR_CAMERA_SERVICE` | सिस्टम-व्यापी कैमरा सर्विस क्रैश हो गई है। |

## Activity onPause/onResume के साथ लाइफसाइकिल एकीकरण

Android Activity लाइफसाइकिल आंतरिक रूप से `CameraDevice` लाइफसाइकिल से जुड़ी हुई है।

### कैमरा कब खोलें (onResume)
1. अनुमतियों की पुष्टि करें।
2. यदि कोई `CameraDevice` पहले से खुला नहीं है, तो `openCamera()` कॉल करें।

### कैमरा कब बंद करें (onPause)
1. रिपीटिंग रिक्वेस्ट रोकें।
2. कैप्चर सेशन बंद करें।
3. `CameraDevice` को बंद करें।
4. सभी रेफरेंस हटा दें।
5. बैकग्राउंड थ्रेड रोकें।

## Semaphore के साथ समवर्ती नियंत्रण (Concurrency Control)

एक सूक्ष्म रेस कंडीशन (race condition) से बचने के लिए कि `openCamera()` पिछले ओपन के खत्म होने से पहले ही फिर से कॉल हो जाए, हम एक **`Semaphore(1)`** (एक बाइनरी लॉक) का उपयोग करते हैं।

- `openCamera()` कॉल करने से पहले, परमिट प्राप्त करें (`tryAcquire`).
- **हर टर्मिनल कॉलबैक** (`onOpened`, `onDisconnected`, `onError`) में, परमिट जारी करें (`release`).
- `onPause` में, कैमरा बंद करने के बाद, परमिट को फिर से जारी करें।

## पूर्ण Kotlin कोड: कैमरा खोलना (Opening a Camera)

```kotlin
// ... (सेटअप और वेरिएबल)

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            Log.d(TAG, "✅ कैमरा सफलतापूर्वक खुल गया: ID=${camera.id}")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice?.close()
            cameraDevice = null
            // ... (यूजर को मैसेज दिखाएं)
        }
    }

    private fun openCamera(cameraId: String) {
        // ... (अनुमति चेक और Semaphore acquire)
        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            cameraOpenCloseLock.release()
            // ... (हैंडल एक्सेप्शन)
        }
    }

    private fun closeCamera() {
        cameraOpenCloseLock.acquire()
        cameraDevice?.close()
        cameraDevice = null
        cameraOpenCloseLock.release()
    }
```

## सारांश (Summary)

इस अध्याय में, आपने सीखा:
1. **CameraDevice क्या है**: फिजिकल कैमरा हार्डवेयर के साथ एक सक्रिय कनेक्शन।
2. **StateCallback**: `onOpened`, `onDisconnected`, और `onError` को कैसे हैंडल करें।
3. **लाइफसाइकिल एकीकरण**: `onResume` में खोलने और `onPause` में बंद करने के नियम।
4. **Semaphore**: समवर्ती ओपन/क्लोज़ रेस को कैसे रोकें।

## आगे क्या है

कैमरा खुला होना आवश्यक है लेकिन स्क्रीन पर पिक्सेल रेंडर करने के लिए पर्याप्त नहीं है। **अध्याय 8: कैमरा प्रीव्यू दिखाना** में, आप लाइव प्रीव्यू शुरू करने के लिए `CameraCaptureSession` बनाने के बारे में सीखेंगे।
