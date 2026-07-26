---
sidebar_position: 14
title: "Kapitel 14: Professionelle Funktionen - High-Speed Video"
description: Erfahren Sie, wie Sie High-Speed Video aufnehmen und mit Multi-Kamera-Setups in Camera2 arbeiten.
keywords: [High-Speed Video, Multi-Kamera, Camera2, Constrained High Speed, logische Kamera]
---

Professionelle Funktionen eröffnen neue kreative Möglichkeiten. Lassen Sie uns High-Speed Video und Multi-Kamera erkunden.

## Einleitung

Camera2 unterstützt viele professionelle Funktionen über die grundlegende Fotoaufnahme hinaus. In diesem Kapitel lernen wir:

1. **High-Speed Video** — Aufnahme von Zeitlupenvideo
2. **Multi-Kamera** — Arbeiten mit logischen und physischen Kameras

## High-Speed Video

Mit High-Speed Video können Sie Videos mit höheren Bildraten als die standardmäßigen 30 fps aufnehmen:
- 120 fps — Sanfte Zeitlupe
- 240 fps — Standard-Zeitlupe
- 480 fps — Extreme Zeitlupe
- 960 fps — Super-Zeitlupe

### Voraussetzungen

Um High-Speed Video aufzunehmen, muss Ihre Kamera:
1. Die `CONSTRAINED_HIGH_SPEED_VIDEO`-Funktion unterstützen
2. Die richtige Hardware-Ebene haben

Prüfen Sie CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### High-Speed Video-Größen

High-Speed Video verwendet andere Auflösungen als Standardvideo:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### Aufnahme von High-Speed Video

High-Speed Video erfordert eine spezielle Aufnahmesitzung:

```kotlin
// Erstellen einer High-Speed-Aufnahmesitzung
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "High-Speed-Sitzung fehlgeschlagen", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### Starten der High-Speed-Vorschau

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // Unterstützte High-Speed-FPS-Bereiche abrufen
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // Einen High-Speed-Bereich finden (z. B. 120 fps)
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

## Multi-Kamera

Moderne Smartphones haben mehrere Kameras. Camera2 behandelt sie als:
- **Physische Kameras** — Einzelne Kamerasensoren
- **Logische Kameras** — Kombinationen aus physischen Kameras

### Logische vs. physische Kameras

| Typ | Beschreibung |
| --- | --- |
| **Physisch** | Einziger Kamerasensor (Weitwinkel, Telefoto, Ultraweitwinkel) |
| **Logisch** | Virtuelle Kamera, die mehrere physische Kameras kombiniert |

### Erkennen von Kameratypen

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Prüfen, ob es eine logische Kamera ist
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// Physische Kamera-IDs abrufen (für logische Kameras)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### Wechseln zwischen Kameras

Um zwischen Kameras zu wechseln, müssen Sie:
1. Die aktuelle Kamera schließen
2. Die neue Kamera öffnen
3. Eine neue Aufnahmesitzung erstellen

```kotlin
private fun switchCamera(newCameraId: String) {
    // Aktuelle Kamera schließen
    captureSession?.close()
    cameraDevice?.close()
    
    // Neue Kamera öffnen
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### Concurrent Camera

Einige Geräte unterstützen das gleichzeitige Öffnen mehrerer Kameras:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## Camera Extension

Mit Camera Extension können Sie herstellerspezifische Kamerafunktionen nutzen:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// Verfügbare Erweiterungen abrufen
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

Häufige Erweiterungen:
- `EXTENSION_BOKEH` — Porträtmodus
- `EXTENSION_HDR` — HDR-Modus
- `EXTENSION_NIGHT` — Nachtmodus
- `EXTENSION_AUTO` — Automatischer Modus

## Ein Multi-Kamera-Beispiel

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
                CameraCharacteristics.LENS_FACING_FRONT -> "Frontkamera ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "Rückkamera ($id)"
                else -> "Kamera $id"
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
            Toast.makeText(this, "Kameraberechtigung verweigert", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@MultiCameraActivity, "Sitzung fehlgeschlagen", Toast.LENGTH_SHORT).show()
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

## Best Practices

1. **Funktionen prüfen** — Überprüfen Sie immer vor der Verwendung professioneller Funktionen
2. **Übergänge handhaben** — Schließen/öffnen Sie sanft beim Wechseln zwischen Kameras
3. **Ressourcen verwalten** — High-Speed Video verbraucht mehr Ressourcen
4. **Anmutig zurückfallen** — Bieten Sie Alternativen an, wenn Funktionen nicht verfügbar sind

## Nächstes Kapitel

Im letzten Kapitel erkunden wir die CameraCharacteristics-Enzyklopädie — einen tiefen Einblick in die wichtigsten Kameraparameter.

## Zusammenfassung

Professionelle Funktionen erweitern Ihre kreativen Möglichkeiten:

1. **High-Speed Video** — Zeitlupe mit 120-960 fps aufnehmen
2. **Multi-Kamera** — Mit logischen und physischen Kameras arbeiten
3. **Camera Extension** — Herstellerspezifische Funktionen nutzen
4. **Concurrent Camera** — Mehrere Kameras gleichzeitig öffnen

Im nächsten Kapitel tauchen wir tief in CameraCharacteristics ein — die Enzyklopädie der Kameraparameter.
