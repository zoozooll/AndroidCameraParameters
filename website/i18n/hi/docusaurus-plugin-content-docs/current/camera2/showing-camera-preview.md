---
sidebar_position: 9
title: "अध्याय 9: कैमरा पूर्वावलोकन दिखाना"
description: सीखें कि Camera2 में TextureView, Surface, और CameraCaptureSession का उपयोग करके कैमरा पूर्वावलोकन कैसे प्रदर्शित करें।
keywords: [कैमरा पूर्वावलोकन, TextureView, Surface, CameraCaptureSession, Camera2]
---

अंत में, आप कैमरा पूर्वावलोकन देखेंगे! चलिए सब कुछ एक साथ जोड़ते हैं।

## परिचय

कैमरा खोलना अच्छा है, लेकिन आप अभी कुछ भी नहीं देख सकते हैं। कैमरा जो देखता है उसे प्रदर्शित करने के लिए, आपको यह करना होगा:

1. पूर्वावलोकन प्रदर्शित करने के लिए एक TextureView बनाएं
2. TextureView से एक Surface प्राप्त करें
3. एक CameraCaptureSession बनाएं
4. पूर्वावलोकन शुरू करें

यह वह जगह है जहां सभी टुकड़े एक साथ आते हैं।

## TextureView

TextureView एक view है जो `SurfaceTexture` प्रदर्शित कर सकता है। यह कैमरा पूर्वावलोकन दिखाने के लिए एकदम सही है क्योंकि:
- इसे ट्रांसफॉर्म किया जा सकता है (स्केल, रोटेट)
- यह हार्डवेयर एक्सीलरेशन सपोर्ट करता है
- यह एनीमेशन और ट्रांजीशन के साथ अच्छी तरह से काम करता है

अपने लेआउट में एक TextureView जोड़ें:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</FrameLayout>
```

## Surface

एक Surface एक बफर है जो इमेज डेटा प्राप्त कर सकता है। कैमरा पूर्वावलोकन प्रदर्शित करने के लिए:
1. TextureView से SurfaceTexture प्राप्त करें
2. SurfaceTexture से एक Surface बनाएं
3. Surface को CameraCaptureSession को पास करें

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession कैप्चर प्रक्रिया का प्रबंधन करता है। यह कैमरा डिवाइस को एक या अधिक Surfaces से कनेक्ट करता है।

एक सेशन बनाने के लिए:
1. Surfaces की एक लिस्ट तैयार करें (पूर्वावलोकन, फोटो कैप्चर, आदि के लिए)
2. CameraDevice पर `createCaptureSession()` कॉल करें
3. कॉलबैक को संभालें

## एक पूर्ण पूर्वावलोकन उदाहरण

चलिए एक activity बनाते हैं जो कैमरा पूर्वावलोकन दिखाती है:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Collections

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            // आवश्यकता होने पर आकार परिवर्तन को संभालें
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // पूर्वावलोकन अपडेट होने पर कॉल किया जाता है
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

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // पूर्वावलोकन आकार प्राप्त करें
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // एक पूर्वावलोकन आकार चुनें
        previewSize = previewSizes?.get(0) // पहला उपलब्ध आकार उपयोग करें

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "कैमरा अनुमति अस्वीकृत", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
            Toast.makeText(this@CameraPreviewActivity, "कैमरा त्रुटि", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val surface = Surface(texture)
        val surfaces = listOf(surface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@CameraPreviewActivity, "सेशन कॉन्फ़िगरेशन विफल", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(surface)
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
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
        captureSession?.stopRepeating()
        captureSession?.close()
        cameraDevice?.close()
    }
}
```

## यह कैसे काम करता है

चलिए फ्लो को ट्रेस करते हैं:

1. **TextureView उपलब्ध** — `onSurfaceTextureAvailable()` कॉल किया जाता है
2. **कैमरा खोलें** — हमें एक CameraDevice मिलता है
3. **कैप्चर सेशन बनाएं** — कैमरा को Surface से कनेक्ट करें
4. **पूर्वावलोकन शुरू करें** — एक रिपीटिंग कैप्चर रिक्वेस्ट भेजें

## CaptureRequest

CaptureRequest परिभाषित करता है कि कैमरा क्या कैप्चर करेगा:
- `TEMPLATE_PREVIEW` — पूर्वावलोकन मोड के लिए
- `TEMPLATE_STILL_CAPTURE` — स्टिल फोटो के लिए
- `TEMPLATE_RECORD` — वीडियो रिकॉर्डिंग के लिए
- `TEMPLATE_VIDEO_SNAPSHOT` — वीडियो के दौरान स्नैपशॉट के लिए

## पूर्वावलोकन लूप

जब आप `setRepeatingRequest()` कॉल करते हैं, तो कैमरा लगातार Surface पर फ्रेम भेजता है। यह लाइव पूर्वावलोकन बनाता है।

## महत्वपूर्ण नोट्स

1. **Surface उपलब्ध होना चाहिए** — कैमरा खोलने से पहले `onSurfaceTextureAvailable()` का इंतजार करें
2. **रिसोर्स बंद करें** — हमेशा कैप्चर सेशन और कैमरा डिवाइस बंद करें
3. **ओरिएंटेशन संभालें** — डिवाइस ओरिएंटेशन के आधार पर पूर्वावलोकन को रोटेशन की आवश्यकता हो सकती है
4. **आकार मायने रखता है** — एक ऐसा पूर्वावलोकन आकार चुनें जो आपके TextureView आयामों से मेल खाता हो

## सफलता!

जब आप यह ऐप चलाते हैं, तो आपको अपनी स्क्रीन पर एक लाइव कैमरा पूर्वावलोकन दिखाई देना चाहिए। बधाई हो! आपने अपना पहला Camera2 पूर्वावलोकन ऐप बनाया है।

## अगला अध्याय

अब जब आप पूर्वावलोकन प्रदर्शित कर सकते हैं, तो अगला कदम फोटो कैप्चर करना है। भाग III में, हम इनके बारे में सीखेंगे:

1. फोटो कैप्चर करने के लिए ImageReader
2. JPEG और RAW कैप्चर
3. CaptureRequest और CaptureResult

## सारांश

कैमरा पूर्वावलोकन प्रदर्शित करने में शामिल हैं:

1. **TextureView** — पूर्वावलोकन प्रदर्शित करने के लिए UI कंपोनेंट
2. **Surface** — वह बफर जो कैमरा फ्रेम प्राप्त करता है
3. **CameraCaptureSession** — कैप्चर प्रक्रिया का प्रबंधन करता है
4. **CaptureRequest** — परिभाषित करता है कि क्या कैप्चर करना है
5. **setRepeatingRequest()** — निरंतर पूर्वावलोकन लूप शुरू करता है

अब आपने इस सीरीज़ का भाग II पूरा कर लिया है। आप:
- कैमरों की खोज कर सकते हैं
- कैमरा विशेषताओं का परीक्षण कर सकते हैं
- एक कैमरा खोल सकते हैं
- एक पूर्वावलोकन प्रदर्शित कर सकते हैं

भाग III में, हम सीखेंगे कि Camera2 के साथ फोटो कैसे कैप्चर करें।
