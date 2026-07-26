---
sidebar_position: 5
title: "अध्याय 5: Camera2 के साथ आरंभ करें"
description: CameraManager के बारे में जानें, जो Android Camera2 API का प्रवेश बिंदु है जो आपको कैमरों को गिनने और उनकी विशेषताओं तक पहुंचने की अनुमति देता है।
keywords: [CameraManager, Camera2 API, Android camera, कैमरा गणना]
---

इस श्रृंखला के कोडिंग भाग में आपका स्वागत है। आइए नींव से शुरू करें: CameraManager।

## परिचय

किसी भी कैमरे का उपयोग करने से पहले, आपको इसे खोजने और एक्सेस करने का एक तरीका चाहिए। यहीं पर **CameraManager** काम आता है।

CameraManager Camera2 API का प्रवेश द्वार है। यह पहली कक्षा है जिसका आप किसी भी Camera2 एप्लिकेशन में उपयोग करेंगे।

## CameraManager क्या है?

CameraManager एक सिस्टम सेवा है जो Android डिवाइस पर सभी कैमरा डिवाइस का प्रबंधन करती है। इसे कैमरों की एक निर्देशिका या रजिस्ट्री के रूप में सोचें।

इसकी मुख्य जिम्मेदारियाँ हैं:
1. **कैमरों की गणना करें** — सभी उपलब्ध कैमरों को सूचीबद्ध करें
2. **कैमरा विशेषताएँ प्राप्त करें** — प्रत्येक कैमरे के बारे में विस्तृत जानकारी प्राप्त करें
3. **कैमरे खोलें** — कैप्चर के लिए CameraDevice बनाएं

## CameraManager प्राप्त करना

Android में, सिस्टम सेवाएँ `Context` के माध्यम से प्राप्त की जाती हैं। यहां बताया गया है कि CameraManager कैसे प्राप्त करें:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

बस। डिवाइस पर सभी कैमरों तक पहुंच प्राप्त करने के लिए कोड की एक पंक्ति।

## सबसे पहले अनुमतियाँ

CameraManager का उपयोग करने से पहले, आपको कैमरा अनुमतियों का अनुरोध करना होगा। इन्हें अपने `AndroidManifest.xml` में जोड़ें:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

और अपनी एक्टिविटी में रनटाइम अनुमति का अनुरोध करें:

```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED
) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        CAMERA_PERMISSION_REQUEST
    )
}
```

कैमरे तक पहुंचने से पहले हमेशा अनुमतियों की जाँच करें।

## CameraManager के तरीके

CameraManager के तीन मुख्य तरीके हैं जिनका आप उपयोग करेंगे:

### 1. `getCameraIdList()`

कैमरा ID स्ट्रिंग्स की एक सरणी लौटाता है। प्रत्येक ID एक कैमरा डिवाइस का प्रतिनिधित्व करता है।

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "कैमरा मिला: $id")
}
```

यह आउटपुट दे सकता है:
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

एक `CameraCharacteristics` ऑब्जेक्ट लौटाता है जिसमें किसी विशिष्ट कैमरे के बारे में सभी विवरण होते हैं।

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics में कैमरे की क्षमताओं का वर्णन करने वाले सैकड़ों पैरामीटर होते हैं।

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

एक कैमरा खोलता है और कॉलबैक के माध्यम से `CameraDevice` लौटाता है। हम बाद में इसे विस्तार से कवर करेंगे।

## कैमरा ID पुनर्विचार

अध्याय 4 से याद रखें कि Android कैमरों को संख्यात्मक ID आवंटित करता है। ID डिवाइसों में या यहां तक कि रीबूट में भी सुसंगत होने की गारंटी नहीं है।

सामान्य पैटर्न:
- **Camera 0** — आमतौर पर रियर वाइड कैमरा
- **Camera 1** — अक्सर फ्रंट कैमरा
- **Camera 2** — आमतौर पर एक अल्ट्रा-वाइड या टेलीफोटो कैमरा
- उच्च संख्याएँ — अतिरिक्त कैमरे (मैक्रो, डेप्थ, आदि)

लेकिन कभी भी कैमरा ID के अर्थ को **न मानें**। यह निर्धारित करने के लिए हमेशा कैमरा विशेषताओं की जाँच करें:
- लेंस की दिशा (फ्रंट/रियर/एक्सटर्नल)
- फोकल लंबाई
- क्षमताएँ

## CameraManager क्यों महत्वपूर्ण है

CameraManager Camera2 के साथ हम जो कुछ भी करेंगे उसकी नींव है:

1. **खोज** — कैमरे का उपयोग करने से पहले, आपको इसे खोजना होगा
2. **जानकारी** — कैमरा खोलने से पहले, आपको इसकी क्षमताओं को जानना होगा
3. **एक्सेस** — CameraManager कैमरा डिवाइस खोलने का एकमात्र तरीका प्रदान करता है

## एक सरल उदाहरण

आइए इसे एक सरल उदाहरण में एक साथ रखें:

```kotlin
class CameraDiscoveryActivity : AppCompatActivity() {
    
    private lateinit var cameraManager: CameraManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            discoverCameras()
        } else {
            requestCameraPermission()
        }
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }
    
    private fun discoverCameras() {
        val cameraIds = cameraManager.cameraIdList
        
        Log.d("CameraDiscovery", "${cameraIds.size} कैमरा मिले")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "सामने"
                CameraCharacteristics.LENS_FACING_BACK -> "पीछे"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "बाहरी"
                else -> "अज्ञात"
            }
            
            Log.d("CameraDiscovery", "कैमरा $cameraId: $lensFacingStr")
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                discoverCameras()
            } else {
                Toast.makeText(this, "कैमरा अनुमति आवश्यक है", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

यह सरल एक्टिविटी सभी कैमरों को खोजती है और उनके ID और लेंस की दिशा को लॉग करती है।

## मुख्य बातें

- **CameraManager** Camera2 का प्रवेश बिंदु है
- सभी कैमरों को खोजने के लिए `getCameraIdList()` का उपयोग करें
- विस्तृत जानकारी प्राप्त करने के लिए `getCameraCharacteristics()` का उपयोग करें
- हमेशा पहले कैमरा अनुमतियों का अनुरोध करें
- कभी भी कैमरा ID के अर्थ को न मानें — विशेषताओं की जाँच करें

## अगला अध्याय

अब जब आप CameraManager को समझ गए हैं, तो अब आपका पहला वास्तविक Camera2 प्रोग्राम लिखने का समय है। अगले अध्याय में, हम:

1. एक सरल Android ऐप बनाएंगे
2. सभी उपलब्ध कैमरों को सूचीबद्ध करेंगे
3. उपयोगकर्ता को कैमरा जानकारी प्रदर्शित करेंगे

आप अपना पहला Camera2 कोड लिखेंगे और वास्तविक परिणाम देखेंगे!

## सारांश

CameraManager Camera2 की नींव है। यह तक पहुंच प्रदान करता है:
- कैमरा गणना
- कैमरा विशेषताएँ
- कैमरा खोलना

CameraManager के साथ, आप यह पता लगा सकते हैं कि कौन से कैमरे उपलब्ध हैं और उन्हें खोलने से पहले उनकी क्षमताओं के बारे में जान सकते हैं।

अगले अध्याय में, हम अपना पहला Camera2 प्रोग्राम लिखेंगे जो डिवाइस पर सभी कैमरों को सूचीबद्ध करता है।
