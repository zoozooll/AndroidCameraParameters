---
sidebar_position: 7
title: "Kapitel 7: CameraCharacteristics verstehen"
description: Erkunde CameraCharacteristics, um mehr über Objektivausrichtung, Hardware-Level, Sensorgröße und andere wichtige Kamerafähigkeiten zu erfahren.
keywords: [CameraCharacteristics, Objektivausrichtung, Hardware-Level, Sensorgröße, Kamerafähigkeiten]
---

CameraCharacteristics ist dein Fenster zur Seele der Kamera. Lass uns sie erkunden.

## Einleitung

Im vorherigen Kapitel hast du gelernt, wie man Kameras auflistet und grundlegende Informationen erhält. Jetzt tauchen wir tiefer in **CameraCharacteristics** ein — die umfassende Beschreibung der Fähigkeiten einer Kamera.

CameraCharacteristics enthält Hunderte von Parametern. In diesem Kapitel konzentrieren wir uns auf die wichtigsten.

## Was ist CameraCharacteristics?

CameraCharacteristics ist ein unveränderliches Objekt, das alle Metadaten über ein Kameragerät enthält. Es beschreibt:

- **Hardware-Eigenschaften** — Sensorgröße, Objektivmerkmale
- **Fähigkeiten** — Was die Kamera kann
- **Modi** — Verfügbare Fokus-, Belichtungs- und Weißabgleichsmodi
- **Ausgabeoptionen** — Unterstützte Auflösungen und Formate
- **Leistung** — Bildraten, Belichtungsbereiche

Du erhältst CameraCharacteristics von CameraManager:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## Wichtige CameraCharacteristics-Schlüssel

Lass uns die wichtigsten Merkmale erkunden.

### 1. Objektivausrichtung

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

Mögliche Werte:
- `LENS_FACING_FRONT` — Frontkamera (Selfie)
- `LENS_FACING_BACK` — Rückkamera
- `LENS_FACING_EXTERNAL` — Externe Kamera

### 2. Hardware-Level

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

Dies ist eines der wichtigsten Merkmale:

| Level | API-Level | Funktionen |
| --- | --- | --- |
| **LEGACY** | 21 | Eingeschränkte Camera2-Unterstützung, alte Camera-API eingepackt |
| **LIMITED** | 21 | Grundlegende Camera2-Funktionen, keine manuellen Steuerungen |
| **FULL** | 21 | Volle manuelle Steuerung, RAW-Aufnahme |
| **LEVEL_3** | 24 | Erweiterte Funktionen wie YUV-Neuverarbeitung |

### 3. Sensorgröße

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width und sensorSize.height geben die Dimensionen
```

Die Sensorgröße gibt an, wie viele Pixel der Sensor hat. Dies unterscheidet sich von der Bildauflösung — der Sensor kann mehr Pixel haben, als bei einer einzelnen Aufnahme verwendet werden.

### 4. Aktive Array-Größe

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

Das aktive Array ist der tatsächliche Bereich des Sensors, der für die Bildaufnahme verwendet wird. Dies ist normalerweise etwas kleiner als das Pixel-Array, da einige Pixel für die Kalibrierung reserviert sind.

### 5. Verfügbare Fähigkeiten

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

Dieses Array zeigt dir, welche Funktionen die Kamera unterstützt:
- `BACKWARD_COMPATIBLE` — Grundlegende Kompatibilität
- `MANUAL_SENSOR` — Manuelle Sensorsteuerung
- `MANUAL_POST_PROCESSING` — Manuelle Nachbearbeitung
- `RAW` — RAW-Aufnahmeunterstützung
- `BURST_CAPTURE` — Serienaufnahme
- `YUV_REPROCESSING` — YUV-Neuverarbeitung
- `DEPTH_OUTPUT` — Tiefenausgabe
- `CONSTRAINED_HIGH_SPEED_VIDEO` — Hochgeschwindigkeitsvideo

### 6. Ausgabeformate

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

Die Stream-Konfigurationskarte enthält alle Ausgabeformate und -größen, die von der Kamera unterstützt werden:
- `ImageFormat.JPEG` — Standard-JPEG
- `ImageFormat.RAW_SENSOR` — RAW-Sensordaten
- `ImageFormat.YUV_420_888` — YUV-Format
- `ImageFormat.RAW10` — 10-Bit-RAW
- `ImageFormat.RAW12` — 12-Bit-RAW

### 7. Unterstützte Vorschaugrößen

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

Dies gibt dir alle verfügbaren Vorschauauflösungen für die Kamera.

### 8. Unterstützte Bildgrößen

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

Dies sind die verfügbaren Auflösungen für Standbildaufnahmen.

### 9. Brennweiten

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

Dieses Array enthält die Brennweiten (in Millimetern) des Objektivs. Mehrere Werte weisen auf optische Zoomfähigkeiten hin.

### 10. Fokusdistanzbereich

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

Die minimale Fokusdistanz gibt an, wie nah die Kamera fokussieren kann. Ein kleinerer Wert bedeutet bessere Makrofähigkeit.

## Ein praktisches Beispiel

Lass uns eine detailliertere Kamera-Info-App erstellen:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Objektivausrichtung
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_BACK -> "Rückseite"
        else -> "Extern"
    }
    
    // Hardware-Level
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Unbekannt"
    }
    
    // Sensorgröße
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Aktive Array-Größe
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Brennweiten
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Unbekannt"
    
    // Verfügbare Fähigkeiten
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Rückwärtskompatibel"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manueller Sensor"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW-Aufnahme"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Serienaufnahme"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV-Neuverarbeitung"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Tiefenausgabe"
            else -> "Unbekannte Fähigkeit"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Kamera $cameraId ===")
    Log.d("CameraDetails", "Objektivausrichtung: $lensFacingStr")
    Log.d("CameraDetails", "Hardware-Level: $hardwareLevelStr")
    Log.d("CameraDetails", "Sensorgröße: $sensorSizeStr")
    Log.d("CameraDetails", "Aktives Array: $activeArrayStr")
    Log.d("CameraDetails", "Brennweiten: $focalLengthsStr")
    Log.d("CameraDetails", "Fähigkeiten: ${capabilitiesList.joinToString(", ")}")
}
```

## Beispielausgabe

```
=== Kamera 0 ===
Objektivausrichtung: Rückseite
Hardware-Level: FULL
Sensorgröße: 4032 x 3024
Aktives Array: 4000 x 3000
Brennweiten: 2.4mm, 4.8mm
Fähigkeiten: Rückwärtskompatibel, Manueller Sensor, RAW-Aufnahme, Serienaufnahme
```

## Warum CameraCharacteristics wichtig sind

Bevor du eine Kamera öffnest oder eine Aufnahmesitzung erstellst, **musst** du CameraCharacteristics prüfen:

1. **Fähigkeiten überprüfen** — Gehe nicht davon aus, dass eine Funktion unterstützt wird
2. **Die richtige Kamera wählen** — Auswahl basierend auf Objektivausrichtung, Hardware-Level usw.
3. **Ausgaben konfigurieren** — Verwende unterstützte Auflösungen und Formate
4. **Geräteunterschiede handhaben** — Was auf einem Gerät funktioniert, muss auf einem anderen nicht funktionieren

## Mit Android Camera Parameters erkunden

Öffne die Android Camera Parameters-App und durchsuche die Merkmale. Du wirst Hunderte von Parametern sehen, die nach Kategorien organisiert sind:

- **Kamera-Info** — Grundlegende Kamera-Informationen
- **Sensor** — Sensormerkmale
- **Objektiv** — Objektiveigenschaften
- **Steuerung** — Auto-Belichtung, Auto-Fokus, Weißabgleich
- **Skalierer** — Ausgabegrößen und -formate
- **Blitz** — Blitzfähigkeiten
- **Statistiken** — Statistikausgabe

Dies gibt dir ein vollständiges Bild der Fähigkeiten deiner Kamera.

## Nächstes Kapitel

Jetzt, da du CameraCharacteristics verstehst, bist du bereit, deine erste Kamera zu öffnen! Im nächsten Kapitel werden wir:

1. Mehr über CameraDevice erfahren
2. Eine Kamera mit CameraManager öffnen
3. Kamera-Zustandsrückrufe handhaben
4. Den Kamera-Lebenszyklus verstehen

## Zusammenfassung

CameraCharacteristics enthält alle Informationen, die du benötigst, um die Fähigkeiten einer Kamera zu verstehen:

- **Objektivausrichtung** — Front, Rückseite oder extern
- **Hardware-Level** — LEGACY, LIMITED, FULL, LEVEL_3
- **Sensorgröße** — Physische Dimensionen
- **Aktives Array** — Aufnahmebereich
- **Brennweiten** — Objektivfähigkeiten
- **Fähigkeiten** — Unterstützte Funktionen
- **Ausgabeformate** — Verfügbare Bildformate

Überprüfe immer CameraCharacteristics, bevor du eine Kamera verwendest. Dies stellt sicher, dass deine App auf verschiedenen Geräten funktioniert.

Im nächsten Kapitel öffnen wir unsere erste Kamera mit CameraDevice.
