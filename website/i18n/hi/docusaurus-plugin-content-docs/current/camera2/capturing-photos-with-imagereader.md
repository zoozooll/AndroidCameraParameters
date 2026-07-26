---
sidebar_position: 10
title: "अध्याय 10: ImageReader के साथ फोटो कैप्चर करना"
description: सीखें कि ImageReader का उपयोग करके स्टिल फोटो कैसे कैप्चर करें, जो Camera2 से इमेज डेटा प्राप्त करने का मुख्य घटक है।
keywords: [ImageReader, फोटो कैप्चर, JPEG, Camera2, कैप्चर रिक्वेस्ट]
---

अब जब आप प्रीव्यू डिस्प्ले कर सकते हैं, तो अब फोटो कैप्चर करने का समय है। आइए ImageReader के बारे में जानें।

## परिचय

Camera2 के साथ फोटो कैप्चर करने के लिए, आपको इमेज डेटा प्राप्त करने का एक तरीका चाहिए। यहीं पर **ImageReader** काम आता है।

ImageReader कैमरा और आपके एप्लिकेशन के बीच एक बफर का काम करता है। यह कैमरे से इमेज डेटा प्राप्त करता है और इसे आपके ऐप को प्रोसेसिंग या सेव करने के लिए प्रदान करता है।

## ImageReader क्या है?

ImageReader एक Android क्लास है जो आपको ये करने की अनुमति देता है:
- कैमरे से इमेज डेटा प्राप्त करना
- कैप्चर की गई नवीनतम इमेज तक पहुंचना
- इमेज फॉर्मेट और साइज़ कॉन्फ़िगर करना
- बफ़र करने के लिए इमेजों की अधिकतम संख्या सेट करना

आप एक विशिष्ट फॉर्मेट और साइज़ के साथ ImageReader बनाते हैं:

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // इमेज की चौड़ाई
    height,     // इमेज की ऊंचाई
    format,     // इमेज फॉर्मेट (उदाहरण के लिए, ImageFormat.JPEG)
    maxImages   // बफ़र करने के लिए इमेजों की अधिकतम संख्या
)
```

## इमेज फॉर्मेट

Camera2 कई इमेज फॉर्मेट सपोर्ट करता है:

| फॉर्मेट | विवरण |
| --- | --- |
| `ImageFormat.JPEG` | मानक संपीड़ित इमेज फॉर्मेट |
| `ImageFormat.RAW_SENSOR` | रॉ सेंसर डेटा (ISP प्रोसेसिंग से पहले) |
| `ImageFormat.YUV_420_888` | असम्पीडित YUV फॉर्मेट |
| `ImageFormat.RAW10` | 10-बिट रॉ फॉर्मेट |
| `ImageFormat.RAW12` | 12-बिट रॉ फॉर्मेट |

अधिकांश एप्लिकेशन के लिए, JPEG फोटो कैप्चर के लिए सबसे अच्छा विकल्प है।

## ImageReader बनाना

यहां JPEG कैप्चर के लिए ImageReader बनाना बताया गया है:

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // बफर में अधिकतम 2 इमेज रखें
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // इमेज प्रोसेस करें
    image.close()
}, null)
```

## फोटो कैप्चर करना

फोटो कैप्चर करने के लिए, आपको ये करना होगा:
1. एक ImageReader बनाएं
2. इसकी Surface को कैप्चर सेशन में जोड़ें
3. `TEMPLATE_STILL_CAPTURE` के साथ एक कैप्चर रिक्वेस्ट बनाएं
4. रिक्वेस्ट को कैमरे में भेजें

## एक पूर्ण फोटो कैप्चर उदाहरण

आइए हमारे प्रीव्यू ऐप को फोटो कैप्चर करने के लिए विस्तारित करें:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoCaptureActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null
    private var pictureSize: Size? = null
    private var imageReader: ImageReader? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_capture)
        
        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.captureButton)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) return

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
        
        previewSize = previewSizes?.get(0)
        pictureSize = pictureSizes?.get(0)

        // फोटो कैप्चर के लिए ImageReader बनाएं
        imageReader = ImageReader.newInstance(
            pictureSize?.width ?: 0,
            pictureSize?.height ?: 0,
            ImageFormat.JPEG,
            2
        )
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            saveImage(image)
            image.close()
        }, null)

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "कैमरा परमिशन अस्वीकृत", Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val previewSurface = Surface(texture)
        val readerSurface = imageReader?.surface ?: return
        
        val surfaces = listOf(previewSurface, readerSurface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@PhotoCaptureActivity, "सेशन कॉन्फ़िगरेशन विफल", Toast.LENGTH_SHORT).show()
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

    private fun takePhoto() {
        val readerSurface = imageReader?.surface ?: return
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder?.addTarget(readerSurface)
        
        // ऑटोफोकस को सिंगल शॉट पर सेट करें
        captureRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        
        captureRequestBuilder?.let { builder ->
            captureSession?.stopRepeating()
            captureSession?.capture(builder.build(), captureCallback, null)
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            startPreview() // कैप्चर के बाद प्रीव्यू फिर से शुरू करें
        }
    }

    private fun saveImage(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, fileName)
        
        try {
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            Toast.makeText(this, "फोटो सेव हुई: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "फोटो सेव करने में विफल", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "कैमरा परमिशन आवश्यक है", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.stopRepeating()
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
    }
}
```

## लेआउट

अपने लेआउट में एक कैप्चर बटन जोड़ें:

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

    <Button
        android:id="@+id/captureButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="कैप्चर करें"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## यह कैसे काम करता है

1. **ImageReader बनाएं** — JPEG इमेज प्राप्त करने के लिए सेट करें
2. **Surface को सेशन में जोड़ें** — कैमरा इस surface पर फोटो भेजता है
3. **कैप्चर रिक्वेस्ट बनाएं** — फोटो के लिए `TEMPLATE_STILL_CAPTURE` का उपयोग करें
4. **कैप्चर रिक्वेस्ट भेजें** — प्रीव्यू रोकें, फोटो कैप्चर करें, प्रीव्यू फिर से शुरू करें
5. **इमेज सेव करें** — JPEG डेटा को फाइल में लिखें

## फोटो के लिए CaptureRequest

स्टिल कैप्चर के लिए, `TEMPLATE_STILL_CAPTURE` का उपयोग करें। यह टेम्पलेट इन सेटिंग्स के लिए अनुकूलित करता है:
- उच्च रिज़ॉल्यूशन
- बेहतर इमेज क्वालिटी
- सिंगल-शॉट ऑटोफोकस

## इमेज हैंडल करना

हमेशा याद रखें:
1. **इमेज प्राप्त करें** — `acquireLatestImage()` का उपयोग करें
2. **इसे प्रोसेस करें** — इमेज सेव करें या डिस्प्ले करें
3. **इसे बंद करें** — संसाधन जारी करने के लिए हमेशा `image.close()` कॉल करें

## अगला अध्याय

अगले अध्याय में, हम RAW कैप्चर और विभिन्न इमेज फॉर्मेट के साथ काम करने के बारे में सीखेंगे।

## सारांश

Camera2 के साथ फोटो कैप्चर करने में शामिल हैं:

1. **ImageReader** — कैमरे से इमेज डेटा प्राप्त करता है
2. **Surface** — फोटो आउटपुट के लिए कैप्चर सेशन में जोड़ा जाता है
3. **TEMPLATE_STILL_CAPTURE** — अनुकूलित कैप्चर रिक्वेस्ट टेम्पलेट
4. **CaptureCallback** — कैप्चर पूरा होने पर सूचित करता है
5. **इमेज प्रोसेसिंग** — कैप्चर की गई इमेज सेव करें या डिस्प्ले करें

अब आप सीख चुके हैं कि बुनियादी फोटो कैसे कैप्चर करें। अगले अध्याय में, हम RAW कैप्चर और उन्नत फोटो फीचर्स का पता लगाएंगे।
