---
sidebar_position: 9
title: "Capítulo 9: Mostrar la Vista Previa de la Cámara"
description: Aprende a mostrar la vista previa de la cámara usando TextureView, Surface y CameraCaptureSession en Camera2.
keywords: [vista previa de cámara, TextureView, Surface, CameraCaptureSession, Camera2]
---

¡Finalmente, verás la vista previa de la cámara! Vamos a conectar todo junto.

## Introducción

Abrir una cámara está bien, pero aún no puedes ver nada. Para mostrar lo que la cámara ve, necesitas:

1. Crear un TextureView para mostrar la vista previa
2. Obtener un Surface del TextureView
3. Crear una CameraCaptureSession
4. Iniciar la vista previa

Aquí es donde las piezas se unen.

## TextureView

TextureView es una vista que puede mostrar un `SurfaceTexture`. Es perfecta para mostrar vistas previas de cámara porque:
- Se puede transformar (escalar, rotar)
- Soporta aceleración por hardware
- Funciona bien con animaciones y transiciones

Agrega un TextureView a tu diseño:

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

Un Surface es un búfer que puede recibir datos de imagen. Para mostrar una vista previa de cámara:
1. Obtén el SurfaceTexture del TextureView
2. Crea un Surface desde el SurfaceTexture
3. Pasa el Surface a la CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession gestiona el proceso de captura. Conecta el dispositivo de cámara a uno o más Surfaces.

Para crear una sesión:
1. Prepara una lista de Surfaces (para vista previa, captura de fotos, etc.)
2. Llama a `createCaptureSession()` en CameraDevice
3. Maneja el callback

## Un Ejemplo Completo de Vista Previa

Vamos a crear una actividad que muestra una vista previa de cámara:

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
            // Manejar cambios de tamaño si es necesario
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Llamado cuando se actualiza la vista previa
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
            Toast.makeText(this, "No hay cámaras disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // Obtener tamaños de vista previa
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // Elegir un tamaño de vista previa
        previewSize = previewSizes?.get(0) // Usar el primer tamaño disponible

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
            Toast.makeText(this@CameraPreviewActivity, "Error de cámara", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "Falló la configuración de la sesión", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show()
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

## Cómo Funciona

Vamos a seguir el flujo:

1. **TextureView disponible** — Se llama a `onSurfaceTextureAvailable()`
2. **Abrir cámara** — Obtenemos una CameraDevice
3. **Crear sesión de captura** — Conectar la cámara al Surface
4. **Iniciar vista previa** — Enviar una solicitud de captura repetitiva

## CaptureRequest

CaptureRequest define qué debe capturar la cámara:
- `TEMPLATE_PREVIEW` — Para modo de vista previa
- `TEMPLATE_STILL_CAPTURE` — Para fotos fijas
- `TEMPLATE_RECORD` — Para grabación de video
- `TEMPLATE_VIDEO_SNAPSHOT` — Para captura instantánea durante video

## El Bucle de Vista Previa

Cuando llamas a `setRepeatingRequest()`, la cámara envía fotogramas continuamente al Surface. Esto crea la vista previa en vivo.

## Notas Importantes

1. **El Surface debe estar disponible** — Espera a `onSurfaceTextureAvailable()` antes de abrir la cámara
2. **Cerrar recursos** — Siempre cierra la sesión de captura y el dispositivo de cámara
3. **Manejar la orientación** — La vista previa puede necesitar rotación dependiendo de la orientación del dispositivo
4. **El tamaño importa** — Elige un tamaño de vista previa que coincida con las dimensiones de tu TextureView

## ¡Éxito!

Cuando ejecutes esta aplicación, deberías ver una vista previa en vivo de la cámara en tu pantalla. ¡Felicidades! Has construido tu primera aplicación de vista previa de Camera2.

## Próximo Capítulo

Ahora que puedes mostrar una vista previa, el siguiente paso es capturar fotos. En la Parte III, aprenderemos sobre:

1. ImageReader para capturar fotos
2. Captura JPEG y RAW
3. CaptureRequest y CaptureResult

## Resumen

Mostrar una vista previa de cámara involucra:

1. **TextureView** — El componente de UI para mostrar la vista previa
2. **Surface** — El búfer que recibe los fotogramas de la cámara
3. **CameraCaptureSession** — Gestiona el proceso de captura
4. **CaptureRequest** — Define qué capturar
5. **setRepeatingRequest()** — Inicia el bucle continuo de vista previa

Ahora has completado la Parte II de esta serie. Puedes:
- Descubrir cámaras
- Examinar características de cámara
- Abrir una cámara
- Mostrar una vista previa

En la Parte III, aprenderemos cómo capturar fotos con Camera2.
