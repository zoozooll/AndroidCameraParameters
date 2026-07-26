---
sidebar_position: 14
title: "Capítulo 14: Características Profesionales - Video de Alta Velocidad"
description: Aprende a capturar video de alta velocidad y trabajar con configuraciones de multicámara en Camera2.
keywords: [video de alta velocidad, multicámara, Camera2, alta velocidad restringida, cámara lógica]
---

Las características profesionales abren nuevas posibilidades creativas. Exploremos el video de alta velocidad y la multicámara.

## Introducción

Camera2 admite muchas características profesionales más allá de la captura básica de fotos. En este capítulo, aprenderemos sobre:

1. **Video de alta velocidad** — Captura de video en cámara lenta
2. **Multicámara** — Trabajar con cámaras lógicas y físicas

## Video de Alta Velocidad

El video de alta velocidad te permite capturar video a velocidades de cuadro superiores a los 30 fps estándar:
- 120 fps — Cámara lenta suave
- 240 fps — Cámara lenta estándar
- 480 fps — Cámara lenta extrema
- 960 fps — Súper cámara lenta

### Requisitos

Para capturar video de alta velocidad, tu cámara debe:
1. Admitir la capacidad `CONSTRAINED_HIGH_SPEED_VIDEO`
2. Tener el nivel de hardware adecuado

Verifica CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### Tamaños de Video de Alta Velocidad

El video de alta velocidad usa resoluciones diferentes al video estándar:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### Capturar Video de Alta Velocidad

El video de alta velocidad requiere una sesión de captura especial:

```kotlin
// Crear una sesión de captura de alta velocidad
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "Sesión de alta velocidad fallida", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### Iniciar Vista Previa de Alta Velocidad

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // Obtener rangos de fps de alta velocidad admitidos
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // Encontrar un rango de alta velocidad (ej., 120 fps)
    val highSpeedFpsRange = fpsRanges?.find { it.upper >= 120 }
    
    captureRequestBuilder?.apply {
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange)
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.setRepeatingRequest(builder.build(), null, null)
    }
}
```

## Multicámara

Los teléfonos modernos tienen múltiples cámaras. Camera2 las trata como:
- **Cámaras físicas** — Sensores de cámara individuales
- **Cámaras lógicas** — Combinaciones de cámaras físicas

### Cámaras Lógicas vs Físicas

| Tipo | Descripción |
| --- | --- |
| **Física** | Sensor de cámara único (gran angular, teleobjetivo, ultra gran angular) |
| **Lógica** | Cámara virtual que combina múltiples cámaras físicas |

### Identificar Tipos de Cámara

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Verificar si es una cámara lógica
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// Obtener IDs de cámara física (para cámaras lógicas)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### Cambiar Entre Cámaras

Para cambiar de cámara, necesitas:
1. Cerrar la cámara actual
2. Abrir la nueva cámara
3. Crear una nueva sesión de captura

```kotlin
private fun switchCamera(newCameraId: String) {
    // Cerrar cámara actual
    captureSession?.close()
    cameraDevice?.close()
    
    // Abrir nueva cámara
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### Cámara Concurrente

Algunos dispositivos admiten abrir múltiples cámaras simultáneamente:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## Extensión de Cámara

La extensión de cámara te permite usar características de cámara específicas del fabricante:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// Obtener extensiones disponibles
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

Extensiones comunes:
- `EXTENSION_BOKEH` — Modo retrato
- `EXTENSION_HDR` — Modo HDR
- `EXTENSION_NIGHT` — Modo nocturno
- `EXTENSION_AUTO` — Modo automático

## Un Ejemplo de Multicámara

```kotlin
class MultiCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_camera)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupCameraSelector()
    }

    private fun setupCameraSelector() {
        val cameraIds = cameraManager.cameraIdList
        val spinner = findViewById<Spinner>(R.id.cameraSpinner)
        
        val cameraNames = cameraIds.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Cámara frontal ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "Cámara trasera ($id)"
                else -> "Cámara $id"
            }
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newCameraId = cameraIds[position]
                if (newCameraId != currentCameraId) {
                    switchCamera(newCameraId)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun switchCamera(newCameraId: String) {
        captureSession?.close()
        cameraDevice?.close()
        currentCameraId = newCameraId
        openCamera()
    }

    private fun openCamera() {
        try {
            cameraManager.openCamera(currentCameraId, cameraStateCallback, null)
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
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "Sesión fallida", Toast.LENGTH_SHORT).show()
            }
        }, null)
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

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
        }
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }
}
```

## Mejores Prácticas

1. **Verifica las capacidades** — Siempre verifica antes de usar características profesionales
2. **Maneja las transiciones** — Cierra/abre suavemente al cambiar de cámara
3. **Gestiona los recursos** — El video de alta velocidad consume más recursos
4. **Proporciona alternativas** — Ofrece alternativas cuando las características no están disponibles

## Próximo Capítulo

En el capítulo final, exploraremos la Enciclopedia de CameraCharacteristics — una inmersión profunda en los parámetros de cámara más importantes.

## Resumen

Las características profesionales amplían tus posibilidades creativas:

1. **Video de alta velocidad** — Captura cámara lenta a 120-960 fps
2. **Multicámara** — Trabaja con cámaras lógicas y físicas
3. **Extensión de cámara** — Usa características específicas del fabricante
4. **Cámara concurrente** — Abre múltiples cámaras simultáneamente

En el próximo capítulo, profundizaremos en CameraCharacteristics — la enciclopedia de los parámetros de cámara.
