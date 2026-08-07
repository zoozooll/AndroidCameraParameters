---
sidebar_position: 5
title: "अध्याय 5: अपना पहला Camera2 प्रोजेक्ट बनाना"
description: स्क्रैच से एक पूर्ण Android Camera2 प्रोजेक्ट सेटअप करें। कैमरा अनुमतियों, CameraManager और बैकग्राउंड थ्रेडिंग के बारे में जानें।
keywords: [Camera2 project setup, Android camera permissions, HandlerThread, CameraManager, TextureView hardwareAccelerated, हिंदी]
---

# अध्याय 5: अपना पहला Camera2 प्रोजेक्ट बनाना (Creating Your First Project)

अब हाथ गंदे करने और असली कोड लिखने का समय है। इस अध्याय के अंत तक, आपके पास एक काम करने वाला Android प्रोजेक्ट होगा जो Camera2 API को सफलतापूर्वक शुरू कर देगा और `CameraManager` सर्विस तक पहुँच प्राप्त कर लेगा।

यदि आप एक पूर्ण उदाहरण देखना चाहते हैं, तो **Android Camera Parameters** ऐप देखें ([GitHub](https://github.com/zoozooll/AndroidCameraParameters) / [Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams))।

## प्रोजेक्ट सेटअप क्यों?

Camera2 एक लो-लेवल और परफॉर्मेंस-सेंसिटिव API है। इसके लिए सही सेटअप ज़रूरी है, वरना ऐप क्रैश हो सकता है या 'ANR' (ऐप रिस्पॉन्स नहीं कर रहा) दे सकता है। सही सेटअप के तीन स्तंभ हैं:

1. **अनुमतियाँ (Permissions)**: ऐप को कैमरा इस्तेमाल करने की इजाज़त।
2. **थ्रेडिंग (Threading)**: कैमरा कॉलबैक कभी भी मेन थ्रेड को ब्लॉक नहीं करने चाहिए।
3. **व्यू कॉन्फ़िगरेशन**: प्रीव्यू रेंडर करने के लिए हार्डवेयर एक्सीलरेशन।

## स्टेप 1: नया Android Studio प्रोजेक्ट बनाना

- **Template**: Empty Activity
- **Language**: Kotlin
- **Minimum SDK**: API 21 (Lollipop)

:::tip
आपको किसी बाहरी लाइब्रेरी की ज़रूरत नहीं है। `android.hardware.camera2` Android फ्रेमवर्क का ही हिस्सा है।
:::

## स्टेप 2: AndroidManifest.xml में अनुमतियाँ

`AndroidManifest.xml` में कैमरा परमिशन और हार्डवेयर फीचर्स को डिक्लेयर करें:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />

<application ...>
    <activity android:name=".MainActivity"
              android:hardwareAccelerated="true"> <!-- ज़रूरी है -->
    </activity>
</application>
```

## स्टेप 3: रनटाइम अनुमति अनुरोध (Runtime Permission)

सिर्फ मेनिफेस्ट में लिखना काफी नहीं है, आपको यूजर से परमिशन मांगनी होगी। `MainActivity.kt` में:

1. `checkSelfPermission` से चेक करें।
2. अगर नहीं है, तो `requestPermissions` कॉल करें।
3. `onRequestPermissionsResult` में रिजल्ट को हैंडल करें।

## स्टेप 4: बैकग्राउंड थ्रेड (HandlerThread)

Camera2 API कई कॉलबैक देता है (जैसे हर फ्रेम के लिए एक कॉलबैक)। अगर ये सब मेन थ्रेड पर चलेंगे, तो आपका ऐप 'लैग' (lag) करेगा। इसके लिए हम एक **`HandlerThread`** का उपयोग करते हैं।

इसे `onResume` में शुरू करें और `onPause` में बंद करें ताकि बैटरी बचे।

## स्टेप 5: पूर्ण इनिशियलाइजेशन फ्लो

1. **Activity onCreate**: अनुमतियाँ चेक करें।
2. **अनुमति मिली**: बैकग्राउंड थ्रेड शुरू करें।
3. **CameraManager**: `getSystemService(Context.CAMERA_SERVICE)` को कॉल करें।
4. **ID लिस्ट**: `cameraManager.cameraIdList` से कैमरों की संख्या पता करें।

## सत्यापन: रन करें और देखें

जब आप ऐप रन करेंगे:
1. परमिशन डायलॉग दिखेगा।
2. Logcat में आपको कैमरों की लिस्ट दिखेगी, जैसे: `Successfully accessed CameraManager. Found 4 camera(s).`

## सारांश (Summary)

इस अध्याय में आपने Camera2 ऐप की नींव रखी:
- प्रोजेक्ट सेटअप और SDK वर्जन।
- मेनिफेस्ट और रनटाइम परमिशन।
- परफॉर्मेंस के लिए बैकग्राउंड थ्रेडिंग।
- `CameraManager` के साथ पहला संपर्क।

## आगे क्या है

अब जब `CameraManager` तैयार है, तो अगला कदम प्रत्येक कैमरे की क्षमताओं को जानना है। **अध्याय 6: कैमरों की खोज** में हम `CameraCharacteristics` के बारे में सीखेंगे।
