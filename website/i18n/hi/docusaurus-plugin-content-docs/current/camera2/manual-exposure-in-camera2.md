---
sidebar_position: 14
title: "अध्याय 14: Camera2 में मैनुअल एक्सपोज़र"
description: Android Camera2 API के साथ एक्सपोज़र पर पूर्ण नियंत्रण लें। ऑटो-एक्सपोज़र को अक्षम करना, SENSOR_SENSITIVITY के माध्यम से मैनुअल ISO सेट करना और नैनोसेकंड में शटर स्पीड नियंत्रित करना सीखें।
keywords: [android camera2 manual exposure, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME, CONTROL_MODE_OFF, CONTROL_AE_MODE_OFF, exposure bracketing, long exposure camera2, timelapse camera2, हिंदी]
---

# अध्याय 14: Camera2 में मैनुअल एक्सपोज़र (Manual Exposure in Camera2)

अध्याय 13 के फोटोग्राफिक सिद्धांतों के साथ, अब अवधारणाओं को कोड में बदलने का समय है। इस अध्याय में, आप सीखेंगे कि कैमरे के ऑटो-एक्सपोज़र (AE) सिस्टम को **पूरी तरह से अपने हाथ में कैसे लें** और Camera2 API के साथ मैन्युअल रूप से ISO और शटर स्पीड कैसे सेट करें।

[Android Camera Parameters ऐप](https://github.com/zoozooll/AndroidCameraParameters) इस अध्याय की हर तकनीक को प्रदर्शित करता है — आप ऐप में मैनुअल मोड पर स्विच करके और वास्तविक समय के परिणामों को देखने के लिए ISO और शटर स्लाइडर को एडजस्ट करके लाइव फॉलो कर सकते हैं।

---

## बड़ा स्विच: AUTO → MANUAL तक

डिफ़ॉल्ट रूप से, आपके द्वारा सबमिट की गई प्रत्येक `CaptureRequest` कैमरे की बिल्ट-इन 3A ऑटो-पाइपलाइन (Auto Exposure, Auto Focus, Auto White Balance) के तहत चलती है। मैनुअल होने के लिए, आपको पाइपलाइन को **स्पष्ट रूप से अक्षम (explicitly disable)** करना होगा।

ओवरराइड के दो स्तर हैं:

| स्तर | सेटिंग | क्या होता है |
|-------|---------|-------------|
| 1. केवल AE को अक्षम करें | `CONTROL_AE_MODE = OFF` | ISO + शटर मैनुअल हो जाते हैं; AF और AWB अभी भी ऑटो-रन होते हैं |
| 2. पूरी 3A पाइपलाइन बंद करें | `CONTROL_MODE = OFF` | **सभी** 3A एल्गोरिदम रुक जाते हैं; हर 3A पैरामीटर मैन्युअल रूप से सेट किया जाना चाहिए |

विश्वसनीय मैनुअल एक्सपोज़र के लिए, **दोनों** सेट करें। `CONTROL_MODE = OFF` सेट करना सबसे साफ और सबसे अनुमानित तरीका है।

**ट्रांज़िशन लेटेंसी (Transition Latency):** जब आप मैनुअल कैप्चर रिक्वेस्ट सबमिट करते हैं, तो नए ISO/शटर मान *अगले* फ्रेम पर तुरंत दिखाई नहीं देते हैं। CMOS सेंसर में पाइपलाइन लेटेंसी होती है — मानों के स्थिर होने से पहले **3–5 फ्रेम के ट्रांज़िशन** की अपेक्षा करें।

---

## Camera2 API में मैनुअल कंट्रोल

### SENSOR_SENSITIVITY (ISO)

Camera2 ISO को `CaptureRequest.SENSOR_SENSITIVITY` के रूप में व्यक्त करता है। **हमेशा मान्य रेंज (valid range) को क्वेरी करें**, मानों को हार्डकोड न करें:

```kotlin
val sensorSensitivityRange = characteristics.get(
    CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
)
val minIso = sensorSensitivityRange?.lower ?: 100
val maxIso = sensorSensitivityRange?.upper ?: 6400
```

### SENSOR_EXPOSURE_TIME (नैनोसेकंड में शटर)

यहाँ पहला "पेंच (gotcha)" है: **शटर स्पीड नैनोसेकंड (ns) में स्टोर की जाती है, सेकंड में नहीं।**

| मानवीय शटर (Human Shutter) | नैनोसेकंड (ns) |
|--------------|-------------------|
| 1/1000s | 1,000,000 |
| 1/60s | 16,666,666 |
| 1s | 1,000,000,000 |
| 30s | 30,000,000,000 |

**हार्डवेयर रेंज क्वेरी करें:**

```kotlin
val exposureTimeRange = characteristics.get(
    CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
)
val maxShutterNs = exposureTimeRange?.upper ?: 10_000_000_000L // 10s
```

---

## ⚠️ महत्वपूर्ण: मैनुअल मोड में क्वालिटी में गिरावट

जब आप `CONTROL_MODE = OFF` सेट करते हैं, तो आप केवल एल्गोरिदम को बंद नहीं कर रहे होते हैं — आप **OEM की प्रोप्रायटरी कंप्यूटेशनल पोस्ट-प्रोसेसिंग को भी अक्षम कर रहे होते हैं**।

| प्रोसेसिंग स्टेप | AUTO मोड | MANUAL मोड |
|-----------------|-----------|-----------------------------------|
| नॉइज़ रिडक्शन | ✓ सक्रिय | ✗ बंद — सेंसर का रॉ शोर दिखेगा |
| HDR मर्ज / टोन-मैपिंग | ✓ सक्रिय | ✗ बंद — सिंगल-फ्रेम कर्व |

**परिणाम:** ISO 3200 पर एक मैनुअल फोटो उसी ISO पर AUTO मोड में ली गई फोटो की तुलना में **ज्यादा शोर वाली (noisier)** दिखेगी।

---

## उदाहरण 1: टाइमलैप्स के लिए लॉक किया गया एक्सपोज़र

टाइमलैप्स में, AUTO मोड के कारण हर फ्रेम में एक्सपोज़र थोड़ा बदल सकता है, जिससे वीडियो झिलमिलाता (flicker) है। ISO + शटर लॉक करने से यह खत्म हो जाता है।

**लक्ष्य:** ISO 100, 1/60s (16,666,666 ns) — हर फ्रेम के लिए फिक्स्ड।

```kotlin
val requestBuilder = captureSession.device.createCaptureRequest(
    CameraDevice.TEMPLATE_STILL_CAPTURE
).apply {
    // कुंजी लाइनें: 3A अक्षम करें और मान सेट करें
    set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF)
    set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
    set(CaptureRequest.SENSOR_SENSITIVITY, 100)
    set(CaptureRequest.SENSOR_EXPOSURE_TIME, 16666666L)
}
```

---

## उदाहरण 2: नाइट फोटोग्राफी के लिए लॉन्ग एक्सपोज़र

**लक्ष्य:** ISO 3200, 2 सेकंड (2,000,000,000 ns) — बहते पानी के निशान, ब्राइट नाइट स्काई।

**टिप:** लॉन्ग एक्सपोज़र के लिए **OIS बंद करें** और ट्राइपॉड का उपयोग करें।

---

## उदाहरण 3: 3-शॉट एक्सपोज़र ब्रैकेटिंग (Exposure Bracketing)

**लक्ष्य:** एक ही ISO पर -1 EV, 0 EV, +1 EV के 3 अलग एक्सपोज़र।

इसके लिए `captureBurst()` का उपयोग करें ताकि तीनों फोटो बिना किसी गैप के एक साथ लिए जा सकें। यह HDR फोटो बनाने के लिए आधार का काम करता है।

---

## सारांश (Summary)

- **3A पाइपलाइन अक्षम करें**: `CONTROL_MODE = OFF` + `CONTROL_AE_MODE = OFF`।
- **ISO**: `SENSOR_SENSITIVITY` (1:1 मैपिंग)।
- **शटर**: `SENSOR_EXPOSURE_TIME` (नैनोसेकंड में)।
- **क्वालिटी**: मैनुअल मोड में OEM नॉइज़ रिडक्शन बंद हो जाता है, इसलिए फोटो में शोर अधिक हो सकता है।

## आगे क्या है

एक्सपोज़र ब्राइटनेस को नियंत्रित करता है। **फोकस शार्पनेस को नियंत्रित करता है।** **अध्याय 15: फोकस** में, हम AF स्टेट मशीन और मैनुअल फोकस के बारे में सीखेंगे।
