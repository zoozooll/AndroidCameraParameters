---
sidebar_position: 8
title: "Capítulo 8: Abrir tu primera cámara"
description: Aprende a abrir un CameraDevice usando CameraManager y a manejar el ciclo de vida de la cámara con callbacks de estado.
keywords: [CameraDevice, openCamera, ciclo de vida de la cámara, CameraManager]
---

¡Es hora de abrir tu primera cámara! Aprendamos sobre CameraDevice.

## Introducción

Hasta ahora, hemos aprendido a descubrir cámaras y examinar sus características. Ahora daremos el siguiente paso: **abrir una cámara**.

Abrir una cámara te da acceso al hardware real de la cámara. Una vez abierta, puedes crear sesiones de captura, mostrar vistas previas y capturar fotos.

## ¿Qué es CameraDevice?

CameraDevice representa una sola cámara conectada al dispositivo Android. Proporciona métodos para:
- Crear sesiones de captura
- Capturar imágenes fijas
- Iniciar y detener la vista previa

No creas CameraDevice directamente. En su lugar, lo obtienes de CameraManager llamando a `openCamera()`.

## Abrir una cámara

Así es como se abre una cámara:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // La cámara está lista para usar
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // La cámara fue desconectada
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // Ocurrió un error en la cámara
        camera.close()
    }
}, null)
```

Vamos a desglosar esto.

### El StateCallback

CameraDevice usa un patrón de callback porque abrir una cámara es asíncrono. El callback tiene tres métodos principales:

#### 1. `onOpened(camera: CameraDevice)`

Llamado cuando la cámara se abre exitosamente. Aquí es donde obtienes tu instancia de CameraDevice.

#### 2. `onDisconnected(camera: CameraDevice)`

Llamado cuando la cámara se desconecta. Esto puede suceder si la cámara es usada por otra aplicación o si el dispositivo se apaga. Siempre cierra la cámara en este callback.

#### 3. `onError(camera: CameraDevice, error: Int)`

Llamado cuando ocurre un error. Códigos de error comunes:
- `ERROR_CAMERA_IN_USE` — La cámara ya está en uso
- `ERROR_MAX_CAMERAS_IN_USE` — Demasiadas cámaras abiertas
- `ERROR_CAMERA_DISABLED` — La cámara está deshabilitada
- `ERROR_CAMERA_DEVICE` — Error de hardware de la cámara
- `ERROR_CAMERA_SERVICE` — Error del servicio de cámara

### El Handler

El tercer parámetro es un `Handler`. Si pasas `null`, el callback se ejecutará en el looper del hilo llamador. Para actualizaciones de UI, es posible que quieras pasar un handler que se ejecute en el hilo principal.

## Un ejemplo completo

Vamos a crear una actividad que abre una cámara:

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
            Toast.makeText(this, "No hay cámaras disponibles", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // Abrir la primera cámara
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "ID de cámara inválido", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "¡Cámara abierta exitosamente!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "Cámara ${camera.id} abierta")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "Cámara desconectada", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "La cámara está en uso"
                ERROR_MAX_CAMERAS_IN_USE -> "Demasiadas cámaras abiertas"
                ERROR_CAMERA_DISABLED -> "La cámara está deshabilitada"
                ERROR_CAMERA_DEVICE -> "Error de hardware de la cámara"
                ERROR_CAMERA_SERVICE -> "Error del servicio de cámara"
                else -> "Error desconocido"
            }
            
            Toast.makeText(this@CameraOpenActivity, "Error de cámara: $errorMessage", Toast.LENGTH_SHORT).show()
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
        cameraDevice?.close()
    }
}
```

## El ciclo de vida de la cámara

Entender el ciclo de vida de la cámara es crucial:

1. **Abrir** — Llama a `openCamera()` para obtener un CameraDevice
2. **Usar** — Crea sesiones de captura, captura fotos
3. **Cerrar** — Llama a `close()` cuando termines
4. **Liberar** — La cámara está disponible para otras aplicaciones

Siempre cierra la cámara cuando tu actividad se destruya para evitar fugas de recursos.

## Mejores prácticas

1. **Cerrar cuando termines** — Siempre cierra la cámara en `onDestroy()`
2. **Manejar errores** — No ignores los callbacks de `onError()`
3. **Verificar permisos** — Siempre verifica los permisos antes de abrir
4. **Usar try-catch** — Maneja `SecurityException` e `IllegalArgumentException`
5. **No mantener referencias** — Libera la referencia de CameraDevice cuando se cierra

## Problemas comunes

### La cámara está en uso
- Asegúrate de que ninguna otra aplicación esté usando la cámara
- Verifica que estés cerrando la cámara correctamente

### Permiso denegado
- Verifica los permisos en el manifest
- Comprueba que el permiso en tiempo de ejecución esté otorgado

### ID de cámara no encontrado
- Siempre obtén los IDs de cámara de `getCameraIdList()`
- No hardcodees los IDs de cámara

## Próximo capítulo

Ahora que puedes abrir una cámara, el siguiente paso es mostrar una vista previa. En el próximo capítulo:

1. Aprenderemos sobre TextureView
2. Crearemos un Surface para la vista previa
3. Crearemos una CameraCaptureSession
4. Mostraremos la vista previa de la cámara en pantalla

## Resumen

Abrir una cámara es el primer paso para capturar imágenes:

1. Usa `CameraManager.openCamera()` para obtener un CameraDevice
2. Maneja el StateCallback para `onOpened()`, `onDisconnected()` y `onError()`
3. Siempre cierra la cámara cuando termines
4. Sigue el ciclo de vida de la cámara: abrir → usar → cerrar → liberar

En el próximo capítulo, crearemos una vista previa de la cámara para que puedas ver lo que ve la cámara.
