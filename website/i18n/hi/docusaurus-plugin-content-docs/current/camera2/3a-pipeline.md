---
sidebar_position: 17
title: "अध्याय 17: 3A पाइपलाइन"
description: ऑटो एक्सपोज़र (AE), ऑटो फोकस (AF), और ऑटो व्हाइट बैलेंस (AWB) को एक विश्वसनीय फोटो कैप्चर अनुक्रम में समन्वित करें।
keywords: [android camera2 3a pipeline, precapture trigger, CONTROL_AE_PRECAPTURE_TRIGGER, AE_STATE_PRECAPTURE, CONTROL_AF_TRIGGER_START, flash modes camera2, auto focus auto exposure auto white balance, हिंदी]
---

# अध्याय 17: 3A पाइपलाइन (The 3A Pipeline)

हमने **AE** (Auto Exposure), **AF** (Auto Focus), और **AWB** (Auto White Balance) का स्वतंत्र प्रणालियों के रूप में अध्ययन किया है। वास्तविक फोटोग्राफी ऐप्स को हर शटर प्रेस से पहले इन तीनों को समन्वित (coordinate) करना चाहिए — और उनका **क्रम और समय (order and timing)** बहुत मायने रखता है।

3A पाइपलाइन शटर दबाने पर आने वाले हरे रंग के टिंट, धुंधले फोकस या खराब फ्लैश एक्सपोज़र जैसी समस्याओं को खत्म करती है।

---

## पूर्ण 3A ऑर्केस्ट्रेशन अनुक्रम (Overview)

एक वास्तविक उत्पादन अनुक्रम (production sequence) इस प्रकार दिखता है:

```mermaid
sequenceDiagram
    actor User
    participant App as CaptureController
    participant HAL as Camera2 HAL
    participant AE as AE Engine
    participant AF as AF Engine
    participant AWB as AWB Engine

    User->>App: "Capture" बटन टैप करता है
    App->>HAL: AF_MODE = AUTO सेट करता है
    App->>HAL: CONTROL_AF_TRIGGER = START
    Note over HAL,AF: फोकस स्कैन शुरू

    loop हर प्रीव्यू फ्रेम
        HAL-->>App: CaptureResult
        App->>App: AF_STATE चेक करें
    end

    AF-->>HAL: AF लॉक प्राप्त हुआ
    HAL-->>App: AF_STATE = FOCUSED_LOCKED ✓
    Note over App,AE: फोकस स्थिर → AE प्रीकैप्चर पर आगे बढ़ें

    App->>HAL: CONTROL_AE_PRECAPTURE_TRIGGER = START
    Note over HAL,AE: प्रीकैप्चर मीटरिंग स्वीप

    loop हर प्रीव्यू फ्रेम
        HAL-->>App: CaptureResult
        App->>App: AE_STATE और FLASH_STATE चेक करें
    end

    AE-->>HAL: AE कन्वर्ज हुआ; अंतिम एक्सपोज़र तय
    HAL-->>App: AE_STATE = CONVERGED ✓
    Note over App: सभी 3A कन्वर्ज हुए! कैप्चर करना सुरक्षित है

    App->>HAL: Still Capture रिक्वेस्ट (TEMPLATE_STILL_CAPTURE)
    HAL->>HAL: ज़रूरत पड़ने पर मेन फ्लैश फायर करें
    HAL->>HAL: सेंसर एक्सपोज़ करें, फ्रेम पढ़ें
    HAL-->>App: JPEG / RAW फ्रेम ImageReader के माध्यम से डिलीवर

    App->>HAL: CONTROL_AF_TRIGGER = CANCEL
    App->>HAL: वापस सामान्य प्रीव्यू मोड पर जाएं
```

**हर कदम ब्लॉक करने वाला (blocking) है।** आप अगले चरण पर तब तक नहीं जाते जब तक कि पिछला चरण सफल न हो जाए।

---

## AE (Auto Exposure) में गहराई

### AE मोड: CONTROL_AE_MODE

| मोड | व्यवहार |
|------|----------|
| `ON` | ऑटो एक्सपोज़र, **फ्लैश बंद** |
| `ON_AUTO_FLASH` | **ऑटो-फ्लैश** — कम रोशनी में खुद फ्लैश चलाता है |
| `ON_ALWAYS_FLASH` | **फ्लैश हमेशा चलता है** (बैकलिट पोर्ट्रेट के लिए) |

### प्रीकैप्चर ट्रिगर (Precapture Trigger) क्यों जरूरी है?

प्रीव्यू के दौरान चलने वाला AE इंजन अनुमानित (approximate) होता है। `CONTROL_AE_PRECAPTURE_TRIGGER = START` HAL को बताता है: "मैं एक वास्तविक फोटो लेने वाला हूँ। पूरी सटीकता के साथ मीटरिंग चलाएं और फ्लैश की शक्ति का कैलकुलेशन करें।" **इसे छोड़ने से फ्लैश वाली फोटो खराब आ सकती हैं।**

---

## AWB: तिकड़ी का मूक साथी

AWB आमतौर पर जल्दी कन्वर्ज हो जाता है, लेकिन रंगों की सटीकता के लिए इसका `CONVERGED` या `LOCKED` होना ज़रूरी है।

---

## पूर्ण 3A कैप्चर कंट्रोलर (Kotlin)

यहाँ एक क्लास है जो इस पूरे अनुक्रम को हैंडल करती है:

```kotlin
class ThreeACaptureController(...) {
    // फेज 1: AF ट्रिगर करें, FOCUSED_LOCKED का इंतज़ार करें
    // फेज 2: AE प्रीकैप्चर ट्रिगर करें, CONVERGED का इंतज़ार करें
    // फेज 3: वास्तविक फोटो लें (TEMPLATE_STILL_CAPTURE)
    // फेज 4: सफाई (Cleanup) और प्रीव्यू फिर से शुरू करें
}
```

**जरूरी बात**: लो-लाइट में या बजट फोन पर AF/AE कभी-कभी कन्वर्ज नहीं होते। इसलिए हमेशा ~3.5 सेकंड का **टाइमआउट (timeout)** रखें ताकि ऐप अटक न जाए।

---

## 3A पाइपलाइन की समस्याओं का समाधान (Troubleshooting)

| लक्षण | मूल कारण | सुधार |
|---------|-----------|-----|
| फ्लैश वाली फोटो खराब आ रही हैं | प्रीकैप्चर ट्रिगर छोड़ दिया | कैप्चर से पहले `AE_PRECAPTURE_TRIGGER` चलाएं |
| फोटो धुंधली (soft) आ रही हैं | `FOCUSED_LOCKED` से पहले फोटो ले ली | AF स्टेट के लॉक होने का इंतज़ार करें |
| कैमरा अटक जाता है | कोई टाइमआउट नहीं है | 3500ms का टाइमआउट और बेस्ट-एफर्ट फॉलबैक जोड़ें |

---

## सारांश (Summary)

यह अध्याय एक्सपोज़र, फोकस और व्हाइट बैलेंस को एक भरोसेमंद **3A कैप्चर पाइपलाइन** में जोड़ता है:

1. **फेज 1 (AF):** फोकस लॉक करें।
2. **फेज 2 (AE):** प्रीकैप्चर मीटरिंग चलाएं (फ्लैश के लिए ज़रूरी)।
3. **फेज 3 (Still Capture):** फोटो लें।
4. **फेज 4 (Cleanup):** सब कुछ रीसेट करें।

## आगे क्या है

बधाई हो! अब आप प्रो-मोड कैमरा ऐप बनाने में सक्षम हैं। अगले अध्यायों में, हम कैप्चर **क्वालिटी** पर ध्यान देंगे — RAW कैप्चर, DNG सेविंग और HDR तकनीकों के बारे में जानेंगे।
