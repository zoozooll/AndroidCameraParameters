---
sidebar_position: 24
title: "अध्याय 24: CameraX"
description: "CameraX में महारत हासिल करें, Jetpack की लाइफसाइकिल-अवेयर कैमरा लाइब्रेरी। UseCase आर्किटेक्चर और Camera2Interop के बारे में जानें।"
keywords: [camerax, jetpack camera, camerax architecture, usecase model, camera2interop, processcameraprovider, preview usecase, imagecapture, imageanalysis, videocapture, camerax vs camera2, हिंदी]
---

# अध्याय 24: CameraX (Jetpack Camera)

## सारांश (Summary)

इस अध्याय तक पहुँचने तक, आपने रॉ Camera2 API में महारत हासिल कर ली है: `CameraDevice` खोलना, `CaptureRequest.Builder` बनाना, और लाइफसाइकिल को मैनेज करना। अब हम एक कदम पीछे हटते हैं और पूछते हैं: क्या होगा अगर यह सारा बॉयलरप्लेट (boilerplate) कोड गायब हो जाए?

CameraX Google की Jetpack लाइब्रेरी है जो Camera2 को एक लाइफसाइकिल-अवेयर (lifecycle-aware) और घोषणात्मक (declarative) API में लपेटती है। यह Camera2 को रिप्लेस नहीं करती है, बल्कि इसके हुड के नीचे Camera2 ही काम करता है।

---

## CameraX आर्किटेक्चर

CameraX का मुख्य आधार **UseCase मॉडल** है — यहाँ आप सरफेस और सेशन के बजाय इस बारे में सोचते हैं कि *आप कैमरे से क्या करवाना चाहते हैं*।

### UseCase मॉडल

| UseCase | उद्देश्य |
|------------------|----------------------------------------------------------------|
| `Preview` | स्क्रीन पर लाइव प्रीव्यू दिखाना। |
| `ImageAnalysis` | बैकग्राउंड थ्रेड पर इमेज फ्रेम का विश्लेषण करना (जैसे ML Kit के लिए)। |
| `ImageCapture` | फोटो लेना (JPEG/RAW)। |
| `VideoCapture` | वीडियो रिकॉर्ड करना। |

इन चारों को एक साथ बाइंड करना पूरी तरह से संभव है। CameraX खुद चेक करता है कि डिवाइस कौन से कॉम्बिनेशन सपोर्ट करता है।

### ProcessCameraProvider और लाइफसाइकिल जागरूकता

CameraX का सबसे बड़ा फायदा इसकी लाइफसाइकिल जागरूकता है। जब आपकी एक्टिविटी `ON_STOP` पर पहुँचती है, तो CameraX अपने आप कैमरा बंद कर देता है।

```kotlin
val cameraProvider = cameraProviderFuture.get()
cameraProvider.bindToLifecycle(
    lifecycleOwner,
    cameraSelector,
    preview,
    imageCapture
)
```

---

## Camera2Interop: CameraX में Camera2 पैरामीटर इंजेक्ट करना

CameraX उन 80% मामलों के लिए बेहतरीन है जहाँ आपको सामान्य कैमरा फीचर्स चाहिए। लेकिन अगर आपको किसी खास Camera2 कुंजी (जैसे `SENSOR_SENSITIVITY`) को मैन्युअल रूप से सेट करना है, तो आप **`Camera2Interop`** का उपयोग कर सकते हैं।

### उदाहरण: CameraX में मैनुअल ISO और एक्सपोज़र

```kotlin
val imageCapture = ImageCapture.Builder()
    .apply {
        val interop = Camera2Interop.Extender(this)
        interop.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        interop.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 400)
        interop.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 16666666L)
    }
    .build()
```

---

## CameraX बनाम Camera2 का चयन

| परिदृश्य | CameraX | Raw Camera2 |
|-----------------------------------------------------------------------|:-------:|:-----------:|
| इंस्टाग्राम जैसा प्रीव्यू + फोटो + वीडियो | ✅ | ⛔ |
| QR कोड / बारकोड स्कैनिंग | ✅ | ⛔ |
| मैनुअल 3A स्टेट मशीन (प्रो लेवल) | ⛔ | ✅ |
| ज़ीरो शटर लैग (ZSL) या RAW फोटोग्राफी | ⛔ | ✅ |
| हाई-स्पीड 120/240fps वीडियो | ⛔ | ✅ |

---

## सारांश (Summary)

CameraX लाइफसाइकिल-अवेयर façade के साथ Camera2 ही है। यह सैकड़ों लाइनों के मैन्युअल सेटअप को `bindToLifecycle()` कॉल से रिप्लेस कर देता है। उन फीचर्स के लिए जिन्हें CameraX सीधे उजागर नहीं करता, `Camera2Interop` रॉ कुंजियों को इंजेक्ट करने का रास्ता देता है।

## आगे क्या है

CameraX अभी भी Java/Kotlin कोड है। अगर आपको AR इंजन के लिए और भी अधिक परफॉर्मेंस चाहिए, तो अध्याय 25 में हम NDK के माध्यम से C++ में नेटिव कैमरा विकास के बारे में जानेंगे।
