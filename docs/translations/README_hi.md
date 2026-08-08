# एंड्रॉइड कैमरा पैरामीटर्स (Android Camera Parameters)

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Google Play पर प्राप्त करें' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>
<a href='https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api'><img alt='ट्यूटोरियल' src='https://img.shields.io/badge/Tutorials-Mastering%20Camera2-blue?style=for-the-badge&logo=gitbook' height="45"/></a>

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | [Español](README_es.md) | [Português](README_pt-BR.md) | [Français](README_fr.md) | [Deutsch](README_de.md) | [Русский](README_ru.md) | [हिन्दी] | [Bahasa Indonesia](README_in.md) | [日本語](README_ja.md) | [한국어](README_ko.md)

एंड्रॉइड कैमरा पैरामीटर्स डेवलपर्स और उत्साही लोगों के लिए अपने डिवाइस के कैमरों की गहरी तकनीकी क्षमताओं का पता लगाने के लिए एक शक्तिशाली नैदानिक उपकरण है। यह आपके डिवाइस पर प्रत्येक लेंस के बारे में विस्तृत जानकारी प्रदान करने के लिए एंड्रॉइड कैमरा2 एपीआई (Android Camera2 API) का लाभ उठाता है।

![ऐप अवलोकन](../../website/static/img/camera_params_feature_graph.png)

## प्रमुख विशेषताएं

*   **विस्तृत निदान**: सभी लेंसों (रियर, फ्रंट, एक्सटर्नल) के लिए `CameraCharacteristics` का निरीक्षण करें।
*   **हार्डवेयर स्तर का पता लगाना**: तुरंत देखें कि आपका डिवाइस `LEGACY`, `LIMITED`, `FULL`, या `LEVEL_3` सुविधाओं का समर्थन करता है या नहीं।
*   **रीयल-टाइम फीचर ट्रैकिंग**: एक सहज डैशबोर्ड के माध्यम से रॉ कैप्चर (RAW capture), ऑप्टिकल इमेज स्टेबिलाइजेशन (OIS), मैनुअल एक्सपोज़र, मैनुअल फ़ोकस और बहुत कुछ के लिए समर्थन की जाँच करें।
*   **वर्गीकृत अन्वेषण**: **अंतर्निहित खोज** कार्यक्षमता के साथ सेंसर, लेंस, AE/AF/AWB और प्रोसेसिंग श्रेणियों द्वारा व्यवस्थित सैकड़ों पैरामीटर्स।
*   **पसंदीदा (जल्द ही आ रहा है)**: त्वरित पहुंच के लिए अक्सर चेक किए जाने वाले पैरामीटर्स को बुकमार्क करें।
*   **रॉ डेटा एक्सपोर्ट**: पूर्ण कैमरा प्रोफ़ाइल को संरचित JSON के रूप में देखें।
*   **बहु-भाषा समर्थन**: चीनी, स्पेनिश, जापानी और अन्य सहित 12+ भाषाओं में पूरी तरह से स्थानीयकृत।

## समर्थित भाषाएँ

ऐप को वैश्विक दर्शकों का समर्थन करने के लिए स्थानीयकृत किया गया है:
- 🇺🇸 अंग्रेजी
- 🇨🇳 चीनी (सरलीकृत)
- 🇹🇼/🇭🇰 चीनी (पारंपरिक)
- 🇪🇸 स्पेनिश
- 🇧🇷 पुर्तगाली (ब्राजील)
- 🇫🇷 फ्रेंच
- 🇩🇪 जर्मन
- 🇷🇺 रूसी
- 🇮🇳 हिन्दी
- 🇮🇩 इंडोनेशियाई
- 🇯🇵 जापानी
- 🇰🇷 कोरियाई

## टेक स्टैक

- **भाषा**: Kotlin
- **UI फ्रेमवर्क**: Jetpack Compose
- **डिज़ाइन सिस्टम**: Material 3
- **आर्किटेक्चर**: MVVM
- **लाइब्रेरी**:
    - [Camera2 API](https://developer.android.com/training/camera2): मुख्य कैमरा इंटरेक्शन।
    - [Gson](https://github.com/google/gson): रॉ डेटा एक्सपोर्ट के लिए JSON सीरियलाइजेशन।
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): ऐप नेविगेशन।

## प्रोजेक्ट संरचना

- `app/`: मुख्य एंड्रॉइड एप्लिकेशन मॉड्यूल।
    - `com.aaron.cameraparams.ui`: कंपोज़-आधारित UI स्क्रीन और घटक।
    - `com.aaron.cameraparams.camera`: कैमरा मैनेजर (CameraManager) के साथ बातचीत करने और विशेषताओं को प्राप्त करने के लिए लॉजिक।
- `camera_parameters/`: विभिन्न उपकरणों (Pixel 3, Samsung S10+, आदि) से कैमरा पैरामीटर्स के नमूना JSON डंप।
- `docs/`: अतिरिक्त दस्तावेज़ और स्क्रीनशॉट।

## शुरू करना

### पूर्वापेक्षाएँ

- Android Studio Koala या नया।
- Android SDK 37 (Compile/Target)।
- एक भौतिक एंड्रॉइड डिवाइस (अनुशंसित) या कैमरा2 समर्थन वाला एमुलेटर।

### निर्माण और रन

1. रिपॉजिटरी को क्लोन करें:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. एंड्रॉइड स्टूडियो में प्रोजेक्ट खोलें।
3. प्रोजेक्ट का निर्माण करें:
   ```bash
   ./gradlew assembleDebug
   ```
4. अपने डिवाइस पर इंस्टॉल करें और चलाएं।

## लाइसेंस

यह प्रोजेक्ट **MIT लाइसेंस** के तहत लाइसेंस प्राप्त है। विवरण के लिए [LICENSE](../../LICENSE) फ़ाइल देखें।

## समर्थन या संपर्क

ईमेल: kangkang365@gmail.com
प्रोजेक्ट साइट: [एंड्रॉइड कैमरा पैरामीटर्स दस्तावेज़ीकरण](https://zoozooll.github.io/AndroidCameraParameters/)
