---
sidebar_position: 8
title: "अध्याय 8: अपना पहला कैमरा खोलना"
description: सीखें कि CameraManager का उपयोग करके CameraDevice कैसे खोलें और स्टेट कॉलबैक के साथ कैमरा लाइफसाइकल को कैसे संभालें।
keywords: [CameraDevice, openCamera, कैमरा लाइफसाइकल, CameraManager]
---

अब समय आ गया है कि आप अपना पहला कैमरा खोलें! चलिए CameraDevice के बारे में जानते हैं।

## परिचय

अब तक, हमने सीखा है कि कैमरों को कैसे खोजें और उनकी विशेषताओं का परीक्षण कैसे करें। अब हम अगला कदम उठाएंगे: **कैमरा खोलना**।

कैमरा खोलने से आपको वास्तविक कैमरा हार्डवेयर तक पहुंच मिलती है। एक बार खोलने के बाद, आप कैप्चर सेशन बना सकते हैं, पूर्वावलोकन प्रदर्शित कर सकते हैं, और फोटो कैप्चर कर सकते हैं।

## CameraDevice क्या है?

CameraDevice Android डिवाइस से जुड़े एक कैमरे का प्रतिनिधित्व करता है। यह इन कार्यों के लिए तरीके प्रदान करता है:
- कैप्चर सेशन बनाना
- स्टिल इमेज कैप्चर करना
- पूर्वावलोकन शुरू करना और बंद करना

आप CameraDevice को सीधे नहीं बनाते हैं। इसके बजाय, आप इसे CameraManager से `openCamera()` कॉल करके प्राप्त करते हैं।

## कैमरा खोलना

यहां बताया गया है कि कैमरा कैसे खोलें:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // कैमरा उपयोग के लिए तैयार है
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // कैमरा डिस्कनेक्ट हो गया था
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // कैमरा त्रुटि आई
        camera.close()
    }
}, null)
```

चलिए इसे समझते हैं।

### StateCallback

CameraDevice एक कॉलबैक पैटर्न का उपयोग करता है क्योंकि कैमरा खोलना असिंक्रोनस होता है। कॉलबैक में तीन मुख्य तरीके हैं:

#### 1. `onOpened(camera: CameraDevice)`

कैमरा सफलतापूर्वक खुलने पर कॉल किया जाता है। यहीं से आपको अपना CameraDevice इंस्टेंस मिलता है।

#### 2. `onDisconnected(camera: CameraDevice)`

कैमरे के डिस्कनेक्ट होने पर कॉल किया जाता है। यह तब हो सकता है जब कैमरा किसी दूसरे ऐप द्वारा उपयोग में हो या डिवाइस बंद हो जाए। इस कॉलबैक में हमेशा कैमरा बंद करें।

#### 3. `onError(camera: CameraDevice, error: Int)`

त्रुटि आने पर कॉल किया जाता है। सामान्य त्रुटि कोड:
- `ERROR_CAMERA_IN_USE` — कैमरा पहले से उपयोग में है
- `ERROR_MAX_CAMERAS_IN_USE` — बहुत सारे कैमरे खुले हैं
- `ERROR_CAMERA_DISABLED` — कैमरा अक्षम है
- `ERROR_CAMERA_DEVICE` — कैमरा हार्डवेयर त्रुटि
- `ERROR_CAMERA_SERVICE` — कैमरा सेवा त्रुटि

### Handler

तीसरा पैरामीटर एक `Handler` है। यदि आप `null` पास करते हैं, तो कॉलबैक कॉलिंग थ्रेड के लूपर पर चलेगा। UI अपडेट के लिए, आप एक हैंडलर पास करना चाह सकते हैं जो मेन थ्रेड पर चले।

## एक पूर्ण उदाहरण

चलिए एक activity बनाते हैं जो कैमरा खोलती है:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraOpenActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            openCamera()
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        
        if (cameraIds.isEmpty()) {
            Toast.makeText(this, "कोई कैमरा उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // पहला कैमरा खोलें
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "कैमरा अनुमति अस्वीकृत", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "अमान्य कैमरा ID", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "कैमरा सफलतापूर्वक खुला!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "कैमरा ${camera.id} खुला")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "कैमरा डिस्कनेक्ट हो गया", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "कैमरा उपयोग में है"
                ERROR_MAX_CAMERAS_IN_USE -> "बहुत सारे कैमरे खुले हैं"
                ERROR_CAMERA_DISABLED -> "कैमरा अक्षम है"
                ERROR_CAMERA_DEVICE -> "कैमरा हार्डवेयर त्रुटि"
                ERROR_CAMERA_SERVICE -> "कैमरा सेवा त्रुटि"
                else -> "अज्ञात त्रुटि"
            }
            
            Toast.makeText(this@CameraOpenActivity, "कैमरा त्रुटि: $errorMessage", Toast.LENGTH_SHORT).show()
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
                openCamera()
            } else {
                Toast.makeText(this, "कैमरा अनुमति आवश्यक है", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## कैमरा लाइफसाइकल

कैमरा लाइफसाइकल को समझना महत्वपूर्ण है:

1. **खोलें** — CameraDevice प्राप्त करने के लिए `openCamera()` कॉल करें
2. **उपयोग करें** — कैप्चर सेशन बनाएं, फोटो कैप्चर करें
3. **बंद करें** — काम होने पर `close()` कॉल करें
4. **रिलीज़ करें** — कैमरा अन्य ऐप्स के लिए उपलब्ध हो जाता है

रिसोर्स लीक से बचने के लिए जब आपकी activity नष्ट हो तो हमेशा कैमरा बंद करें।

## सर्वोत्तम प्रथाएं

1. **काम होने पर बंद करें** — `onDestroy()` में हमेशा कैमरा बंद करें
2. **त्रुटियों को संभालें** — `onError()` कॉलबैक को अनदेखा न करें
3. **अनुमतियां जांचें** — खोलने से पहले हमेशा अनुमतियां सत्यापित करें
4. **try-catch का उपयोग करें** — `SecurityException` और `IllegalArgumentException` को संभालें
5. **संदर्भ न रखें** — बंद होने पर CameraDevice संदर्भ रिलीज़ करें

## सामान्य समस्याएं

### कैमरा उपयोग में है
- सुनिश्चित करें कि कोई अन्य ऐप कैमरा उपयोग नहीं कर रहा है
- जांचें कि आप कैमरा को सही तरीके से बंद कर रहे हैं

### अनुमति अस्वीकृत
- manifest में अनुमतियां सत्यापित करें
- जांचें कि रनटाइम अनुमति दी गई है

### कैमरा ID नहीं मिला
- हमेशा `getCameraIdList()` से कैमरा ID प्राप्त करें
- कैमरा ID हार्डकोड न करें

## अगला अध्याय

अब जब आप कैमरा खोल सकते हैं, तो अगला कदम पूर्वावलोकन प्रदर्शित करना है। अगले अध्याय में, हम:

1. TextureView के बारे में सीखेंगे
2. पूर्वावलोकन के लिए एक Surface बनाएंगे
3. एक CameraCaptureSession बनाएंगे
4. स्क्रीन पर कैमरा पूर्वावलोकन प्रदर्शित करेंगे

## सारांश

छवियां कैप्चर करने की दिशा में कैमरा खोलना पहला कदम है:

1. CameraDevice प्राप्त करने के लिए `CameraManager.openCamera()` का उपयोग करें
2. `onOpened()`, `onDisconnected()`, और `onError()` के लिए StateCallback संभालें
3. काम होने पर हमेशा कैमरा बंद करें
4. कैमरा लाइफसाइकल का पालन करें: खोलें → उपयोग करें → बंद करें → रिलीज़ करें

अगले अध्याय में, हम एक कैमरा पूर्वावलोकन बनाएंगे ताकि आप देख सकें कि कैमरा क्या देखता है।
