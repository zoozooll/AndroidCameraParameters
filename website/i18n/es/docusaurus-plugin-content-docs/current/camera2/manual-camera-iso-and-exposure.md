---
sidebar_position: 12
title: "Capítulo 12: Cámara Manual - ISO y Exposición"
description: Aprende a controlar manualmente el ISO y el tiempo de exposición para fotografía profesional con Camera2.
keywords: [ISO, tiempo de exposición, cámara manual, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

El control manual es donde brilla el verdadero poder de Camera2. Aprendamos sobre ISO y exposición.

## Introducción

Hasta ahora, hemos estado usando controles automáticos. Ahora tomaremos el control total sobre:

1. **ISO** — Sensibilidad del sensor a la luz
2. **Tiempo de exposición** — Cuánto tiempo el sensor recolecta luz

Estos dos ajustes afectan directamente el brillo y la calidad de la imagen.

## ¿Qué es ISO?

ISO mide la sensibilidad del sensor a la luz. Un ISO bajo significa:
- Menos sensible a la luz
- Menor ruido
- Mejor calidad de imagen

Un ISO alto significa:
- Más sensible a la luz
- Mayor ruido (grano)
- Menor calidad de imagen

Valores comunes de ISO: 100, 200, 400, 800, 1600, 3200, 6400

## ¿Qué es el Tiempo de Exposición?

El tiempo de exposición (también llamado velocidad de obturación) es cuánto tiempo el sensor recolecta luz. Una exposición corta significa:
- Menos luz capturada
- Congela el movimiento
- Acción más rápida

Una exposición larga significa:
- Más luz capturada
- Desenfoque de movimiento
- Mejor rendimiento con poca luz

El tiempo de exposición se mide en segundos o fracciones de segundo:
- 1/1000s — Acción rápida
- 1/125s — Normal
- 1/30s — Lento
- 1s — Exposición larga

## Requisitos de Control Manual

Para usar controles manuales, tu cámara debe tener:
1. Nivel de hardware **FULL** o **LEVEL_3**
2. Capacidad **MANUAL_SENSOR**

Verifica CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## Obtener Rangos Soportados

Antes de establecer valores manuales, verifica qué soporta la cámara:

```kotlin
// Obtener rango ISO
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// Obtener rango de exposición
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## Configurar ISO y Exposición Manuales

Para usar controles manuales, necesitas:
1. Desactivar la exposición automática (AE)
2. Configurar ISO manual
3. Configurar tiempo de exposición manual

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Desactivar AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// Configurar ISO manual
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// Configurar tiempo de exposición manual (en nanosegundos)
// 1/125s = 8,000,000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// Añadir destino
captureRequestBuilder.addTarget(surface)

// Iniciar vista previa con controles manuales
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## El Triángulo de Exposición

ISO, tiempo de exposición y apertura forman el "triángulo de exposición":

- **ISO** — Sensibilidad a la luz
- **Tiempo de exposición** — Duración de la captura de luz
- **Apertura** — Cantidad de luz que entra (raramente ajustable en teléfonos)

Cambiar uno afecta a los demás. Por ejemplo:
- Si aumentas el ISO, puedes usar una velocidad de obturación más rápida
- Si disminuyes el tiempo de exposición, puede que necesites aumentar el ISO

## Un Ejemplo Completo de Control Manual

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO: 100-3200 en pasos de 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // Exposición: 1/1000s a 1/10s
        exposureSeekBar.progress = 5
        
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Convertir progreso a tiempo de exposición (en nanosegundos)
                // 0: 1/1000s = 1,000,000ns
                // 9: 1/10s = 100,000,000ns
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
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
            
            // Desactivar AE para control manual
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // Configurar ISO manual
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // Configurar tiempo de exposición manual
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Mantener AWB activado
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (resto del código de configuración de la cámara)
    
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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
    
    // ... (apertura de cámara, creación de sesión, etc.)
}
```

## El Diseño

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="ISO:"/>
            
            <SeekBar
                android:id="@+id/isoSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/isoValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="100"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="16dp">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="Exp:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60s"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## Mejores Prácticas

1. **Verifica el soporte** — Siempre verifica el soporte de sensor manual
2. **Comienza con AE** — Deja que la exposición automática establezca los valores iniciales, luego cambia a manual
3. **Evita ISO extremo** — El ISO alto introduce ruido
4. **Usa la exposición más corta** — Evita el desenfoque de movimiento cuando sea posible
5. **Monitorea el histograma** — Usa CaptureResult para verificar la exposición

## Próximo Capítulo

En el próximo capítulo, aprenderemos sobre el enfoque manual y el control de balance de blancos.

## Resumen

El control manual de ISO y exposición te ofrece fotografía de nivel profesional:

1. **ISO** — Controla la sensibilidad del sensor (rango 100-3200+)
2. **Tiempo de exposición** — Controla cuánto tiempo se recolecta la luz (en nanosegundos)
3. **Desactiva AE** — Debes desactivar la exposición automática para el control manual
4. **Verifica rangos** — Siempre verifica los rangos de ISO y exposición soportados

El triángulo de exposición (ISO, tiempo de exposición, apertura) determina el brillo y la calidad de la imagen. En el próximo capítulo, exploraremos el enfoque manual y el balance de blancos.
