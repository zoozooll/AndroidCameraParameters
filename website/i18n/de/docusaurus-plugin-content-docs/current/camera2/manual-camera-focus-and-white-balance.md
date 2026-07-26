---
sidebar_position: 13
title: "Kapitel 13: Manuelle Kamera - Fokus und Weißabgleich"
description: Lerne, wie du Fokusdistanz und Weißabgleich für professionelle Fotografie mit Camera2 manuell steuerst.
keywords: [fokus, weißabgleich, manuelle kamera, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

Vollständige manuelle Steuerung mit Fokus und Weißabgleich.

## Einleitung

Im vorherigen Kapitel hast du gelernt, ISO und Belichtung zu steuern. Jetzt fügen wir hinzu:

1. **Fokus** — Manuelle Fokusdistanz-Steuerung
2. **Weißabgleich** — Manuelle Farbtemperatur-Steuerung

Damit hast du die vollständige kreative Kontrolle über deine Fotos.

## Manueller Fokus

Der Fokus bestimmt, welcher Teil der Szene scharf ist. Manueller Fokus ermöglicht es dir:
- Auf bestimmte Objekte zu fokussieren
- Absichtliche Unschärfe (Bokeh) zu erzeugen
- Kritischen Fokus bei Makrofotografie sicherzustellen

### Fokusmodi

Camera2 unterstützt mehrere Fokusmodi:

| Modus | Beschreibung |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Manueller Fokus |
| `CONTROL_AF_MODE_AUTO` | Einfacher Autofokus |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Kontinuierlicher Autofokus für Fotos |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Kontinuierlicher Autofokus für Video |
| `CONTROL_AF_MODE_MACRO` | Makrofokus |

### Fokusdistanz

Die Fokusdistanz wird in Dioptrien (1/Meter) gemessen. Ein Wert von 0 bedeutet Unendlich.

```kotlin
// Fokusdistanz-Bereich ermitteln
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### Manuellen Fokus einstellen

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// AF für manuellen Fokus deaktivieren
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// Manuelle Fokusdistanz festlegen (in Dioptrien)
// 0.0 = Unendlich
// 1.0 = 1 Meter
// 2.0 = 0,5 Meter
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Fokusbereiche

Du kannst auch AF-Bereiche für selektiven Autofokus festlegen:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // Zentrum X in Sensor-Koordinaten (0-1000)
        centerY,    // Zentrum Y in Sensor-Koordinaten (0-1000)
        width,      // Bereichsbreite
        height,     // Bereichshöhe
        weight      // Priorität (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## Manueller Weißabgleich

Der Weißabgleich (WB) passt die Farbtemperatur des Bildes an. Verschiedene Lichtquellen haben unterschiedliche Farbtemperaturen:

- **Tageslicht** — ~5500K (bläulich)
- **Bewölkt** — ~6500K (kälter)
- **Glühlampe** — ~2800K (warm/gelb)
- **Leuchtstoffröhre** — ~4000K (grünlich)

### Weißabgleich-Modi

| Modus | Beschreibung |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Manueller Weißabgleich |
| `CONTROL_AWB_MODE_AUTO` | Automatischer Weißabgleich |
| `CONTROL_AWB_MODE_INCANDESCENT` | Glühlampenlicht |
| `CONTROL_AWB_MODE_FLUORESCENT` | Leuchtstofflicht |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Warmweißes Leuchtstofflicht |
| `CONTROL_AWB_MODE_DAYLIGHT` | Tageslicht |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Bewölktes Tageslicht |

### Manuellen Weißabgleich einstellen

Um den manuellen Weißabgleich einzustellen, musst du Farbkorrektur-Verstärkungen festlegen:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// AWB für manuelle Steuerung deaktivieren
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// Farbkorrektur-Verstärkungen festlegen (R, G, B)
// Werte sind normalisiert (1.0 = keine Korrektur)
// Höhere Werte machen diese Farbe prominenter
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Farbtemperatur

Du kannst den Weißabgleich auch über die Farbtemperatur einstellen:

```kotlin
// Unterstützten Farbtemperatur-Bereich ermitteln
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// Farbtemperatur festlegen (in Kelvin)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // Tageslicht
```

## Ein vollständiges Beispiel für eine manuelle Kamera

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // Manuelle Steuerungen
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // Unendlich
    private var currentWhiteBalance = 5500 // Tageslicht

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // ISO-Steuerung
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Belichtungs-Steuerung
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Fokus-Steuerung
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Fortschritt (0-100) in Fokusdistanz umrechnen (0.0 bis 2.0 Dioptrien)
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Weißabgleich-Steuerung
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (Glühlampe) bis 6500K (bewölkt)
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
            
            // Manuelle Belichtung
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Manueller Fokus
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // Manueller Weißabgleich
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (Kamera-Einrichtungscode)
}
```

## Best Practices

1. **Unterstützung prüfen** — Nicht alle Kameras unterstützen manuellen Fokus oder WB
2. **Automatisch starten** — Lass die Auto-Steuerungen eine Basislinie festlegen
3. **Focus Peaking verwenden** — Visuelles Feedback für Fokusgenauigkeit hinzufügen
4. **WB kalibrieren** — Eine Graukarte für genauen Weißabgleich verwenden
5. **Steuerungen kombinieren** — Manuelle Einstellungen funktionieren am besten zusammen

## Nächstes Kapitel

Im nächsten Kapitel erkunden wir professionelle Funktionen wie High-Speed-Video und Mehrkamera.

## Zusammenfassung

Die manuelle Steuerung von Fokus und Weißabgleich vervollständigt dein Kamera-Toolkit:

1. **Fokus** — Steuere, was im Bild scharf ist
2. **Weißabgleich** — Steuere die Farbtemperatur
3. **Manuelle Modi** — AF/AWB deaktivieren und Werte direkt festlegen
4. **Fokusbereiche** — Bestimmte Bereiche für den Autofokus auswählen

Mit ISO, Belichtung, Fokus und Weißabgleich unter deiner Kontrolle kannst du professionelle Fotos erstellen. In Teil V erkunden wir erweiterte Funktionen wie High-Speed-Video und Mehrkamera-Unterstützung.
