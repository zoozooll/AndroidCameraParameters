---
sidebar_position: 11
title: "Kapitel 11: RAW-Aufnahme und CaptureRequest"
description: Erfahren Sie, wie Sie RAW-Bilder aufnehmen und CaptureRequest sowie CaptureResult für erweiterte Kamerasteuerung verstehen.
keywords: [RAW-Aufnahme, CaptureRequest, CaptureResult, Camera2, manuelle Steuerungen]
---

RAW-Aufnahme gibt Ihnen die vollständige Kontrolle über die Bildverarbeitung. Lassen Sie uns dies zusammen mit CaptureRequest und CaptureResult erkunden.

## Einleitung

Im vorherigen Kapitel haben Sie gelernt, wie Sie JPEG-Fotos aufnehmen. Nun erkunden wir:

1. **RAW-Aufnahme** — Aufnahme unverarbeiteter Sensordaten
2. **CaptureRequest** — Konfigurieren von Kameraeinstellungen für jede Aufnahme
3. **CaptureResult** — Abrufen von Metadaten zu abgeschlossenen Aufnahmen

## Was ist RAW?

RAW-Bilder enthalten alle Daten, die vom Sensor vor der ISP-Verarbeitung erfasst wurden. Das bedeutet:

- Keine Rauschunterdrückung angewendet
- Keine Weißabgleichskorrektur
- Keine Schärfung
- Vollständiger Dynamikbereich

RAW-Dateien sind größer, bieten aber unvergleichliche Bearbeitungsflexibilität.

## Anforderungen für RAW-Aufnahmen

Um RAW-Bilder aufzunehmen, muss Ihre Kamera:
1. **FULL**- oder **LEVEL_3**-Hardwareebene haben
2. Die `RAW`-Fähigkeit unterstützen

Überprüfen Sie CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## RAW-Bilder aufnehmen

Erstellen Sie einen ImageReader mit einem RAW-Format:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // oder ImageFormat.RAW10/RAW12
    2
)
```

Fügen Sie dann sowohl JPEG- als auch RAW-Oberflächen zur Aufnahmesitzung für gleichzeitige Aufnahme hinzu:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest definiert alle Einstellungen für eine einzelne Aufnahme. Sie können konfigurieren:

### Automatische Steuerungen
- `CONTROL_AF_MODE` — Autofokus-Modus
- `CONTROL_AE_MODE` — Auto-Belichtungs-Modus
- `CONTROL_AWB_MODE` — Auto-Weißabgleichs-Modus

### Manuelle Steuerungen
- `SENSOR_SENSITIVITY` — ISO-Wert
- `SENSOR_EXPOSURE_TIME` — Belichtungszeit in Nanosekunden
- `LENS_FOCUS_DISTANCE` — Fokusdistanz
- `LENS_APERTURE` — Blende (falls verfügbar)

### Ausgabeeinstellungen
- `JPEG_QUALITY` — JPEG-Komprimierungsqualität
- `JPEG_ORIENTATION` — Bildausrichtung
- `COLOR_CORRECTION_MODE` — Farbkorrekturmodus

### Erstellen eines CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// Automatische Steuerungen setzen
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// JPEG-Qualität setzen
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// Ziele hinzufügen
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// Anfrage erstellen
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult enthält Metadaten zu einer abgeschlossenen Aufnahme. Es umfasst:

- **Tatsächlich verwendete Einstellungen** — Was die Kamera tatsächlich angewendet hat
- **Statistiken** — Belichtungs-, Fokus- und Farbinformationen
- **Zeitstempel** — Wann die Aufnahme erfolgte

### CaptureResult abrufen

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // Tatsächliche Belichtungszeit abrufen
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // Tatsächliches ISO abrufen
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // Fokusstatus abrufen
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // AE-Status abrufen
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "Belichtung: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### Häufige CaptureResult-Schlüssel

| Schlüssel | Beschreibung |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | Tatsächlich verwendete Belichtungszeit |
| `SENSOR_SENSITIVITY` | Tatsächlich verwendetes ISO |
| `CONTROL_AF_STATE` | Autofokus-Status |
| `CONTROL_AE_STATE` | Auto-Belichtungs-Status |
| `CONTROL_AWB_STATE` | Auto-Weißabgleichs-Status |
| `SCALER_CROP_REGION` | Verwendeter Ausschnittsbereich |
| `COLOR_CORRECTION_GAINS` | Farbkorrekturverstärkungen |

## Ein vollständiges RAW-Aufnahmebeispiel

```kotlin
private fun configureDualCapture() {
    // JPEG ImageReader erstellen
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // RAW ImageReader erstellen
    val rawReader = ImageReader.newInstance(
        rawSize.width, rawSize.height,
        ImageFormat.RAW_SENSOR, 2
    )
    
    jpegReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveJpeg(image)
        image.close()
    }, null)
    
    rawReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveRaw(image)
        image.close()
    }, null)
    
    // Oberflächen erstellen
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // Aufnahmesitzung mit allen Oberflächen erstellen
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // Beide Oberflächen als Ziele hinzufügen
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // Steuerungen konfigurieren
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.JPEG_QUALITY, 100)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.capture(builder.build(), captureCallback, null)
    }
}

private fun saveRaw(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val fileName = "RAW_${System.currentTimeMillis()}.dng"
    val file = File(externalCacheDir, fileName)
    
    FileOutputStream(file).use { fos ->
        fos.write(bytes)
    }
}
```

## RAW vs. JPEG

| Merkmal | RAW | JPEG |
| --- | --- | --- |
| Dateigröße | Groß (20-50MB) | Klein (2-10MB) |
| Bearbeitungsflexibilität | Maximal | Begrenzt |
| Rauschen | Erhalten | Reduziert |
| Weißabgleich | Anpassbar | Festgelegt |
| Dynamikbereich | Vollständig | Komprimiert |

## Best Practices

1. **RAW-Unterstützung prüfen** — Immer vor dem Versuch einer RAW-Aufnahme überprüfen
2. **Doppelaufnahme** — Sowohl JPEG als auch RAW für Flexibilität aufnehmen
3. **Bilder schließen** — Immer `image.close()` nach der Verarbeitung aufrufen
4. **Verschiedene Formate handhaben** — RAW_SENSOR, RAW10 und RAW12 haben unterschiedliche Byte-Layouts

## Nächstes Kapitel

Im nächsten Kapitel fassen wir zusammen, was Sie in Teil III gelernt haben, und bereiten uns auf Teil IV vor: Manuelle Kamerasteuerungen.

## Zusammenfassung

In diesem Kapitel haben Sie gelernt über:

1. **RAW-Aufnahme** — Aufnahme unverarbeiteter Sensordaten für maximale Bearbeitungsflexibilität
2. **CaptureRequest** — Konfigurieren von Kameraeinstellungen für jede Aufnahme
3. **CaptureResult** — Abrufen von Metadaten zu abgeschlossenen Aufnahmen

RAW-Aufnahme erfordert FULL- oder LEVEL_3-Hardwareebene. Sie können sowohl JPEG als auch RAW gleichzeitig aufnehmen, indem Sie beide Oberflächen zur Aufnahmesitzung hinzufügen.

CaptureRequest ermöglicht es Ihnen, Autofokus, Auto-Belichtung, Weißabgleich und manuelle Steuerungen wie ISO und Belichtungszeit zu konfigurieren. CaptureResult zeigt Ihnen, welche Einstellungen tatsächlich von der Kamera verwendet wurden.

In Teil IV tauchen wir tief in manuelle Kamerasteuerungen ein: ISO, Belichtung, Fokus und Weißabgleich.
