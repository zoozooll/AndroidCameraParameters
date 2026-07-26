---
sidebar_position: 6
title: "अध्याय 6: कैमरों की सूची बनाना"
description: अपना पहला Camera2 प्रोग्राम लिखें जो Android डिवाइस पर उपलब्ध सभी कैमरों को खोजता है और उनकी सूची बनाता है।
keywords: [कैमरों की सूची, CameraManager, कैमरा गणना, Android Camera2]
---

अब समय आ गया है कि आप अपना पहला Camera2 प्रोग्राम लिखें! चलिए एक ऐप बनाते हैं जो सभी कैमरों की सूची बनाता है।

## परिचय

इस अध्याय में, आप अपना पहला वास्तविक Camera2 एप्लिकेशन लिखेंगे। लक्ष्य सरल है:

> डिवाइस पर सभी कैमरों को खोजें और उनकी जानकारी प्रदर्शित करें।

यह एक छोटा लेकिन महत्वपूर्ण कदम है। किसी कैमरे का उपयोग करने से पहले, आपको उसे खोजना होगा।

## प्रोजेक्ट बनाना

चलिए एक नया Android प्रोजेक्ट बनाकर शुरू करते हैं:

1. Android Studio खोलें
2. "Empty Activity" के साथ एक नया प्रोजेक्ट बनाएं
3. इसका नाम "Camera2List" रखें
4. भाषा के रूप में Kotlin चुनें
5. न्यूनतम SDK को API 21 पर सेट करें (Camera2 को API 21 में पेश किया गया था)

## अनुमतियां जोड़ना

`AndroidManifest.xml` में कैमरा अनुमति जोड़ें:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## लेआउट

एक सरल लेआउट बनाएं जो कैमरों की सूची प्रदर्शित करता हो। `activity_main.xml` को अपडेट करें:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="उपलब्ध कैमरे"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## Activity

अब चलिए मुख्य activity लिखते हैं। यहीं पर Camera2 कोड जाएगा:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraListView: ListView
    private val cameraInfoList = mutableListOf<String>()

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraListView = findViewById(R.id.cameraListView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (hasCameraPermission()) {
            listCameras()
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

    private fun listCameras() {
        cameraInfoList.clear()
        
        try {
            val cameraIds = cameraManager.cameraIdList
            
            if (cameraIds.isEmpty()) {
                cameraInfoList.add("कोई कैमरा नहीं मिला")
            } else {
                cameraInfoList.add("${cameraIds.size} कैमरा मिले:")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "सामने का (Front)"
                        CameraCharacteristics.LENS_FACING_BACK -> "पीछे का (Back)"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "बाहरी (External)"
                        else -> "अज्ञात"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "अज्ञात"
                    }
                    
                    cameraInfoList.add("कैमरा $index (ID: $cameraId)")
                    cameraInfoList.add("  - लेंस: $lensFacingStr")
                    cameraInfoList.add("  - हार्डवेयर स्तर: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("त्रुटि: कैमरा अनुमति अस्वीकृत")
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
                listCameras()
            } else {
                Toast.makeText(this, "कैमरा अनुमति आवश्यक है", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("कैमरा अनुमति अस्वीकृत")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## यह कोड क्या करता है

चलिए समझते हैं कि क्या हो रहा है:

1. **CameraManager प्राप्त करें** — हम CameraManager सिस्टम सेवा प्राप्त करते हैं
2. **अनुमतियां जांचें** — हम जांचते हैं कि कैमरा अनुमति प्रदान की गई है या नहीं
3. **कैमरों की सूची बनाएं** — हम सभी कैमरा ID प्राप्त करने के लिए `getCameraIdList()` का उपयोग करते हैं
4. **विशेषताएं प्राप्त करें** — प्रत्येक कैमरे के लिए, हम उसकी विशेषताएं प्राप्त करते हैं
5. **जानकारी प्रदर्शित करें** — हम कैमरा ID, लेंस की दिशा और हार्डवेयर स्तर दिखाते हैं

## अपेक्षित आउटपुट

जब आप ऐप चलाते हैं, तो आपको कुछ ऐसा दिखना चाहिए:

```
3 कैमरा मिले:

कैमरा 0 (ID: 0)
  - लेंस: पीछे का (Back)
  - हार्डवेयर स्तर: FULL

कैमरा 1 (ID: 1)
  - लेंस: सामने का (Front)
  - हार्डवेयर स्तर: LIMITED

कैमरा 2 (ID: 2)
  - लेंस: पीछे का (Back)
  - हार्डवेयर स्तर: FULL
```

## सफलता!

आपने अभी-अभी अपना पहला Camera2 प्रोग्राम लिखा है! यह सरल लग सकता है, लेकिन यह सब कुछ की नींव है जो हम आगे करेंगे।

## समस्या निवारण

यदि आपको समस्याएं आती हैं:

1. **अनुमति अस्वीकृत** — सुनिश्चित करें कि आपने कैमरा अनुमति प्रदान की है
2. **कोई कैमरा नहीं मिला** — जांचें कि आपके डिवाइस में कैमरा है या नहीं
3. **SecurityException** — सुनिश्चित करें कि अनुमतियां manifest में घोषित हैं
4. **API स्तर बहुत कम** — Camera2 के लिए API 21 या उच्चतर की आवश्यकता है

## आगे क्या है?

अब जब आप कैमरों की सूची बना सकते हैं, तो अगला कदम उनकी विशेषताओं को अधिक विस्तार से जांचना है। अगले अध्याय में, हम:

1. CameraCharacteristics का अन्वेषण करेंगे
2. लेंस की दिशा के बारे में सीखेंगे
3. हार्डवेयर स्तर को समझेंगे
4. सेंसर जानकारी की जांच करेंगे

## सारांश

इस अध्याय में, आपने अपना पहला Camera2 प्रोग्राम लिखा। ऐप:

1. कैमरा अनुमतियों का अनुरोध करता है
2. कैमरों की गणना करने के लिए CameraManager का उपयोग करता है
3. कैमरा ID, लेंस की दिशा और हार्डवेयर स्तर प्रदर्शित करता है

यह पूर्ण Camera2 एप्लिकेशन बनाने की दिशा में पहला कदम है। अगले अध्याय में, हम यह समझने के लिए CameraCharacteristics में गहराई से जाएंगे कि प्रत्येक कैमरा क्या कर सकता है।
