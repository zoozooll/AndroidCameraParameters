---
sidebar_position: 10
title: "Capítulo 10: Capturar Fotos con ImageReader"
description: Aprende a capturar fotos fijas usando ImageReader, el componente clave para recibir datos de imagen de Camera2.
keywords: [ImageReader, captura de fotos, JPEG, Camera2, solicitud de captura]
---

Ahora que puedes mostrar una vista previa, es hora de capturar fotos. Aprendamos sobre ImageReader.

## Introducción

Para capturar una foto con Camera2, necesitas una forma de recibir los datos de la imagen. Ahí es donde entra **ImageReader**.

ImageReader actúa como un búfer entre la cámara y tu aplicación. Recibe datos de imagen de la cámara y los proporciona a tu app para su procesamiento o guardado.

## ¿Qué es ImageReader?

ImageReader es una clase de Android que te permite:
- Recibir datos de imagen de la cámara
- Acceder a la última imagen capturada
- Configurar el formato y tamaño de la imagen
- Establecer un número máximo de imágenes para almacenar en búfer

Creas un ImageReader con un formato y tamaño específicos:

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // Ancho de la imagen
    height,     // Alto de la imagen
    format,     // Formato de imagen (ej., ImageFormat.JPEG)
    maxImages   // Número máximo de imágenes en búfer
)
```

## Formatos de Imagen

Camera2 admite varios formatos de imagen:

| Formato | Descripción |
| --- | --- |
| `ImageFormat.JPEG` | Formato de imagen comprimido estándar |
| `ImageFormat.RAW_SENSOR` | Datos brutos del sensor (antes del procesamiento ISP) |
| `ImageFormat.YUV_420_888` | Formato YUV sin comprimir |
| `ImageFormat.RAW10` | Formato raw de 10 bits |
| `ImageFormat.RAW12` | Formato raw de 12 bits |

Para la mayoría de las aplicaciones, JPEG es la mejor opción para la captura de fotos.

## Crear un ImageReader

Así es como se crea un ImageReader para captura JPEG:

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // Mantener hasta 2 imágenes en el búfer
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // Procesar la imagen
    image.close()
}, null)
```

## Capturar una Foto

Para capturar una foto, necesitas:
1. Crear un ImageReader
2. Agregar su Surface a la sesión de captura
3. Crear una solicitud de captura con `TEMPLATE_STILL_CAPTURE`
4. Enviar la solicitud a la cámara

## Un Ejemplo Completo de Captura de Fotos

Vamos a ampliar nuestra app de vista previa para capturar fotos:

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

        // Crear ImageReader para la captura de fotos
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
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@PhotoCaptureActivity, "Error en la configuración de la sesión", Toast.LENGTH_SHORT).show()
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
        
        // Establecer enfoque automático en modo de disparo único
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
            startPreview() // Reanudar vista previa después de la captura
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
            Toast.makeText(this, "Foto guardada: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show()
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

## El Diseño

Agrega un botón de captura a tu diseño:

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
        android:text="Capturar"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## Cómo Funciona

1. **Crear ImageReader** — Configurado para recibir imágenes JPEG
2. **Agregar Surface a la sesión** — La cámara envía las fotos a esta surface
3. **Crear solicitud de captura** — Usa `TEMPLATE_STILL_CAPTURE` para fotos
4. **Enviar solicitud de captura** — Detener vista previa, capturar foto, reanudar vista previa
5. **Guardar imagen** — Escribir los datos JPEG en un archivo

## CaptureRequest para Fotos

Para la captura fija, usa `TEMPLATE_STILL_CAPTURE`. Esta plantilla optimiza la configuración para:
- Mayor resolución
- Mejor calidad de imagen
- Enfoque automático de disparo único

## Manejar Imágenes

Recuerda siempre:
1. **Adquirir la imagen** — Usa `acquireLatestImage()`
2. **Procesarla** — Guardar o mostrar la imagen
3. **Cerrarla** — Llama siempre a `image.close()` para liberar recursos

## Próximo Capítulo

En el próximo capítulo, aprenderemos sobre la captura RAW y cómo trabajar con diferentes formatos de imagen.

## Resumen

Capturar fotos con Camera2 implica:

1. **ImageReader** — Recibe datos de imagen de la cámara
2. **Surface** — Agregada a la sesión de captura para la salida de fotos
3. **TEMPLATE_STILL_CAPTURE** — Plantilla de solicitud de captura optimizada
4. **CaptureCallback** — Notifica cuando la captura está completa
5. **Procesamiento de imagen** — Guardar o mostrar la imagen capturada

Ahora has aprendido a capturar fotos básicas. En el próximo capítulo, exploraremos la captura RAW y las funciones avanzadas de fotografía.
