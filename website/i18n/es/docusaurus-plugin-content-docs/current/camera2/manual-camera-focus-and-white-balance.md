---
sidebar_position: 13
title: "Capítulo 13: Cámara Manual - Enfoque y Balance de Blancos"
description: Aprende a controlar manualmente la distancia de enfoque y el balance de blancos para fotografía profesional con Camera2.
keywords: [enfoque, balance de blancos, cámara manual, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

Control manual completo con enfoque y balance de blancos.

## Introducción

En el capítulo anterior, aprendiste a controlar el ISO y la exposición. Ahora añadiremos:

1. **Enfoque** — Control manual de la distancia de enfoque
2. **Balance de blancos** — Control manual de la temperatura de color

Con estos, tienes control creativo completo sobre tus fotos.

## Enfoque Manual

El enfoque determina qué parte de la escena está nítida. El enfoque manual te permite:
- Enfocar objetos específicos
- Crear desenfoque intencional (bokeh)
- Asegurar un enfoque crítico en fotografía macro

### Modos de Enfoque

Camera2 soporta varios modos de enfoque:

| Modo | Descripción |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Enfoque manual |
| `CONTROL_AF_MODE_AUTO` | Enfoque automático único |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Enfoque automático continuo para fotos |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Enfoque automático continuo para video |
| `CONTROL_AF_MODE_MACRO` | Enfoque macro |

### Distancia de Enfoque

La distancia de enfoque se mide en dioptrías (1/metro). Un valor de 0 significa infinito.

```kotlin
// Obtener rango de distancia de enfoque
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### Configurar el Enfoque Manual

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Desactivar AF para enfoque manual
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// Configurar distancia de enfoque manual (en dioptrías)
// 0.0 = infinito
// 1.0 = 1 metro
// 2.0 = 0.5 metros
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Regiones de Enfoque

También puedes especificar regiones de AF para enfoque automático selectivo:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // Centro X en coordenadas del sensor (0-1000)
        centerY,    // Centro Y en coordenadas del sensor (0-1000)
        width,      // Ancho de la región
        height,     // Alto de la región
        weight      // Prioridad (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## Balance de Blancos Manual

El balance de blancos (WB) ajusta la temperatura de color de la imagen. Diferentes fuentes de luz tienen diferentes temperaturas de color:

- **Luz de día** — ~5500K (azulada)
- **Nublado** — ~6500K (más frío)
- **Tungsteno** — ~2800K (cálido/amarillo)
- **Fluorescente** — ~4000K (verdoso)

### Modos de Balance de Blancos

| Modo | Descripción |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Balance de blancos manual |
| `CONTROL_AWB_MODE_AUTO` | Balance de blancos automático |
| `CONTROL_AWB_MODE_INCANDESCENT` | Iluminación de tungsteno |
| `CONTROL_AWB_MODE_FLUORESCENT` | Iluminación fluorescente |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluorescente cálido |
| `CONTROL_AWB_MODE_DAYLIGHT` | Luz de día |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Nublado |

### Configurar el Balance de Blancos Manual

Para configurar el balance de blancos manual, necesitas establecer las ganancias de corrección de color:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Desactivar AWB para control manual
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// Configurar ganancias de corrección de color (R, G, B)
// Los valores están normalizados (1.0 = sin corrección)
// Valores más altos hacen que ese color sea más prominente
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Temperatura de Color

También puedes configurar el balance de blancos usando la temperatura de color:

```kotlin
// Obtener rango de temperatura de color soportado
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// Configurar temperatura de color (en Kelvin)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // Luz de día
```

## Un Ejemplo Completo de Cámara Manual

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // Controles manuales
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // Infinito
    private var currentWhiteBalance = 5500 // Luz de día

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // Control ISO
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Control de exposición
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Control de enfoque
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Convertir progreso (0-100) a distancia de enfoque (0.0 a 2.0 dioptrías)
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Control de balance de blancos
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (tungsteno) a 6500K (nublado)
                currentWhiteBalance = 2800 + (progress * 37)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // Exposición manual
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Enfoque manual
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // Balance de blancos manual
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (código de configuración de la cámara)
}
```

## Mejores Prácticas

1. **Verifica el soporte** — No todas las cámaras soportan enfoque manual o WB
2. **Comienza con automático** — Deja que los controles automáticos establezcan una base
3. **Usa el enfoque punteado** — Añade retroalimentación visual para la precisión del enfoque
4. **Calibra el WB** — Usa una tarjeta gris para un balance de blancos preciso
5. **Combina controles** — Los ajustes manuales funcionan mejor juntos

## Próximo Capítulo

En el próximo capítulo, exploraremos funciones profesionales como video de alta velocidad y cámara múltiple.

## Resumen

El control manual del enfoque y el balance de blancos completa tu kit de herramientas de cámara:

1. **Enfoque** — Controla qué está nítido en la imagen
2. **Balance de blancos** — Controla la temperatura de color
3. **Modos manuales** — Desactiva AF/AWB y establece valores directamente
4. **Regiones de enfoque** — Apunta a áreas específicas para el enfoque automático

Con el ISO, la exposición, el enfoque y el balance de blancos bajo tu control, puedes crear fotos de calidad profesional. En la Parte V, exploraremos funciones avanzadas como video de alta velocidad y soporte de cámara múltiple.
