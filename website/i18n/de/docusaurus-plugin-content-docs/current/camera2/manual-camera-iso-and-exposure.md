---
sidebar_position: 12
title: "Kapitel 12: Manuelle Kamera - ISO und Belichtung"
description: Erfahre, wie du ISO und Belichtungszeit manuell für professionelle Fotografie mit Camera2 steuerst.
keywords: [ISO, Belichtungszeit, manuelle Kamera, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

Manuelle Steuerung ist dort, wo die wahre Stärke von Camera2 zum Tragen kommt. Lass uns etwas über ISO und Belichtung lernen.

## Einleitung

Bisher haben wir automatische Steuerungen verwendet. Jetzt übernehmen wir die vollständige Kontrolle über:

1. **ISO** — Sensorempfindlichkeit gegenüber Licht
2. **Belichtungszeit** — Wie lange der Sensor Licht sammelt

Diese zwei Einstellungen wirken sich direkt auf Bildhelligkeit und -qualität aus.

## Was ist ISO?

ISO misst die Empfindlichkeit des Sensors gegenüber Licht. Niedrigere ISO bedeutet:
- Weniger empfindlich gegenüber Licht
- Weniger Rauschen
- Bessere Bildqualität

Höhere ISO bedeutet:
- Empfindlicher gegenüber Licht
- Höheres Rauschen (Korn)
- Niedrigere Bildqualität

Gängige ISO-Werte: 100, 200, 400, 800, 1600, 3200, 6400

## Was ist Belichtungszeit?

Belichtungszeit (auch Verschlusszeit genannt) ist die Dauer, während der der Sensor Licht sammelt. Kürzere Belichtung bedeutet:
- Weniger eingefangenes Licht
- Friert Bewegung ein
- Schnellere Aktion

Längere Belichtung bedeutet:
- Mehr eingefangenes Licht
- Bewegungsunschärfe
- Bessere Schwachlichtleistung

Belichtungszeit wird in Sekunden oder Sekundenbruchteilen gemessen:
- 1/1000s — Schnelle Aktion
- 1/125s — Normal
- 1/30s — Langsam
- 1s — Langzeitbelichtung

## Voraussetzungen für manuelle Steuerung

Um manuelle Steuerungen verwenden zu können, muss deine Kamera Folgendes haben:
1. **FULL**- oder **LEVEL_3**-Hardware-Level
2. **MANUAL_SENSOR**-Fähigkeit

Überprüfe CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## Unterstützte Bereiche ermitteln

Bevor du manuelle Werte einstellst, prüfe, was die Kamera unterstützt:

```kotlin
// ISO-Bereich abrufen
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// Belichtungsbereich abrufen
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## Manuelle ISO und Belichtung einstellen

Um manuelle Steuerungen zu verwenden, musst du:
1. Automatische Belichtung (AE) deaktivieren
2. Manuelle ISO einstellen
3. Manuelle Belichtungszeit einstellen

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// AE deaktivieren
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// Manuelle ISO einstellen
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// Manuelle Belichtungszeit einstellen (in Nanosekunden)
// 1/125s = 8.000.000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// Ziel hinzufügen
captureRequestBuilder.addTarget(surface)

// Vorschau mit manuellen Steuerungen starten
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## Das Belichtungsdreieck

ISO, Belichtungszeit und Blende bilden das "Belichtungsdreieck":

- **ISO** — Empfindlichkeit gegenüber Licht
- **Belichtungszeit** — Dauer der Lichtaufnahme
- **Blende** — Menge des einfallenden Lichts (auf Telefonen selten einstellbar)

Das Ändern eines Wertes wirkt sich auf die anderen aus. Zum Beispiel:
- Wenn du die ISO erhöhst, kannst du eine schnellere Verschlusszeit verwenden
- Wenn du die Belichtungszeit verringerst, musst du möglicherweise die ISO erhöhen

## Ein vollständiges Beispiel für manuelle Steuerung

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
        
        isoSeekBar.max = 31 // ISO: 100-3200 in Schritten von 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // Belichtung: 1/1000s bis 1/10s
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
                // Fortschritt in Belichtungszeit umwandeln (in Nanosekunden)
                // 0: 1/1000s = 1.000.000ns
                // 9: 1/10s = 100.000.000ns
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
            
            // AE für manuelle Steuerung deaktivieren
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // Manuelle ISO einstellen
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // Manuelle Belichtungszeit einstellen
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // AWB eingeschaltet lassen
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (Rest des Kamera-Setup-Codes)
    
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
    
    // ... (Kamera öffnen, Sitzungserstellung usw.)
}
```

## Das Layout

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
                android:text="Bel:"/>
            
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

## Best Practices

1. **Unterstützung prüfen** — Überprüfe immer die Unterstützung für manuellen Sensor
2. **Mit AE beginnen** — Lass die automatische Belichtung Anfangswerte festlegen, dann auf manuell umschalten
3. **Extreme ISO vermeiden** — Hohe ISO führt zu Rauschen
4. **Kürzeste Belichtung verwenden** — Vermeide Bewegungsunschärfe, wenn möglich
5. **Histogramm überwachen** — Verwende CaptureResult, um die Belichtung zu prüfen

## Nächstes Kapitel

Im nächsten Kapitel lernen wir etwas über manuelle Fokussierung und Weißabgleichssteuerung.

## Zusammenfassung

Die manuelle Steuerung von ISO und Belichtung ermöglicht dir professionelle Fotografie:

1. **ISO** — Steuert die Sensorempfindlichkeit (Bereich 100-3200+)
2. **Belichtungszeit** — Steuert, wie lange Licht gesammelt wird (in Nanosekunden)
3. **AE deaktivieren** — Automatische Belichtung muss für manuelle Steuerung ausgeschaltet werden
4. **Bereiche prüfen** — Überprüfe immer die unterstützten ISO- und Belichtungsbereiche

Das Belichtungsdreieck (ISO, Belichtungszeit, Blende) bestimmt Bildhelligkeit und -qualität. Im nächsten Kapitel erkunden wir manuelle Fokussierung und Weißabgleich.
