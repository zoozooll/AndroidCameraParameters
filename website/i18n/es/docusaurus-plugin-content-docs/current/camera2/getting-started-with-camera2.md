---
sidebar_position: 5
title: "Capítulo 5: Introducción a Camera2"
description: Aprende sobre CameraManager, el punto de entrada a la API de Android Camera2 que te permite enumerar cámaras y acceder a sus características.
keywords: [CameraManager, Camera2 API, cámara Android, enumeración de cámaras]
---

Bienvenido a la parte de codificación de esta serie. Comencemos con los fundamentos: CameraManager.

## Introducción

Antes de poder usar cualquier cámara, necesitas una forma de descubrirla y acceder a ella. Ahí es donde entra **CameraManager**.

CameraManager es la puerta de entrada a la API de Camera2. Es la primera clase que usarás en cualquier aplicación de Camera2.

## ¿Qué es CameraManager?

CameraManager es un servicio del sistema que gestiona todos los dispositivos de cámara en un dispositivo Android. Piénsalo como un directorio o registro de cámaras.

Sus responsabilidades principales son:
1. **Enumerar cámaras** — Listar todas las cámaras disponibles
2. **Obtener características de la cámara** — Recuperar información detallada sobre cada cámara
3. **Abrir cámaras** — Crear un CameraDevice para la captura

## Obteniendo CameraManager

En Android, los servicios del sistema se obtienen a través del `Context`. Así es como se obtiene CameraManager:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

Eso es todo. Una línea de código para obtener acceso a todas las cámaras del dispositivo.

## Permisos Primero

Antes de usar CameraManager, necesitas solicitar permisos de cámara. Agrega estos a tu `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

Y solicita el permiso en tiempo de ejecución en tu actividad:

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

Siempre verifica los permisos antes de acceder a la cámara.

## Métodos de CameraManager

CameraManager tiene tres métodos principales que usarás:

### 1. `getCameraIdList()`

Devuelve un array de cadenas de ID de cámara. Cada ID representa un dispositivo de cámara.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Cámara encontrada: $id")
}
```

Esto podría generar:
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

Devuelve un objeto `CameraCharacteristics` que contiene todos los detalles sobre una cámara específica.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics contiene cientos de parámetros que describen las capacidades de la cámara.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

Abre una cámara y devuelve un `CameraDevice` a través del callback. Cubriremos esto en detalle más adelante.

## IDs de Cámara Revisitados

Recuerda del Capítulo 4 que Android asigna IDs numéricos a las cámaras. No se garantiza que los IDs sean consistentes entre dispositivos o incluso entre reinicios.

Patrones comunes:
- **Cámara 0** — Típicamente la cámara trasera gran angular
- **Cámara 1** — A menudo la cámara frontal
- **Cámara 2** — Usualmente una cámara ultra gran angular o telefoto
- Números mayores — Cámaras adicionales (macro, profundidad, etc.)

Pero **nunca asumas** el significado de un ID de cámara. Siempre verifica las características de la cámara para determinar:
- Orientación del lente (frontal/trasera/externa)
- Distancia focal
- Capacidades

## Por qué es Importante CameraManager

CameraManager es la base de todo lo que haremos con Camera2:

1. **Descubrimiento** — Antes de usar una cámara, necesitas encontrarla
2. **Información** — Antes de abrir una cámara, necesitas conocer sus capacidades
3. **Acceso** — CameraManager proporciona la única forma de abrir un dispositivo de cámara

## Un Ejemplo Simple

Pongamos todo junto en un ejemplo simple:

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
        
        Log.d("CameraDiscovery", "Encontradas ${cameraIds.size} cámara(s)")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
                CameraCharacteristics.LENS_FACING_BACK -> "Trasera"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externa"
                else -> "Desconocida"
            }
            
            Log.d("CameraDiscovery", "Cámara $cameraId: $lensFacingStr")
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
                Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

Esta actividad simple descubre todas las cámaras y registra sus IDs y direcciones de orientación del lente.

## Conclusiones Clave

- **CameraManager** es el punto de entrada a Camera2
- Usa `getCameraIdList()` para encontrar todas las cámaras
- Usa `getCameraCharacteristics()` para obtener información detallada
- Siempre solicita permisos de cámara primero
- Nunca asumas significados de ID de cámara — verifica las características

## Próximo Capítulo

Ahora que entiendes CameraManager, es hora de escribir tu primer programa real de Camera2. En el próximo capítulo, vamos a:

1. Crear una aplicación Android simple
2. Listar todas las cámaras disponibles
3. Mostrar información de la cámara al usuario

¡Escribirás tu primer código de Camera2 y verás resultados reales!

## Resumen

CameraManager es la base de Camera2. Proporciona acceso a:
- Enumeración de cámaras
- Características de la cámara
- Apertura de cámara

Con CameraManager, puedes descubrir qué cámaras están disponibles y conocer sus capacidades antes de abrirlas.

En el próximo capítulo, escribiremos nuestro primer programa de Camera2 que lista todas las cámaras en el dispositivo.
