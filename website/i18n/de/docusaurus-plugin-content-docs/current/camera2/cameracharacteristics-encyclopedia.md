---
sidebar_position: 15
title: "Kapitel 15: CameraCharacteristics Enzyklopädie"
description: Ein umfassender Leitfaden zu den wichtigsten Camera2 Characteristics, einschließlich ihrer Bedeutung, ihres Zwecks und ihrer Verwendung.
keywords: [CameraCharacteristics, Kameraparameter, Kamerafähigkeiten, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

Willkommen bei der CameraCharacteristics Enzyklopädie — dein Leitfaden zum Verständnis jedes Kameraparameters.

## Einleitung

CameraCharacteristics enthält Hunderte von Parametern, die die Fähigkeiten einer Kamera beschreiben. In diesem Kapitel werden wir die wichtigsten eingehend untersuchen:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — Was kann die Kamera?
2. `REQUEST_AVAILABLE_CAPABILITIES` — Welche Funktionen sind verfügbar?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — Wie groß ist der Sensor?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — Wie viel Zoom?
5. `CONTROL_AE_AVAILABLE_MODES` — Welche Belichtungsmodi?

Und viele mehr...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**Was bedeutet es?**  
Dies ist die wichtigste Characteristic. Sie definiert das allgemeine Fähigkeitsniveau des Kamerageräts.

**Warum gibt es sie?**  
Verschiedene Android-Geräte haben unterschiedliche Kamerafähigkeiten. Dieser Parameter hilft Anwendungen zu verstehen, was sie tun können.

**Unterstützte Werte:**

| Wert | API-Level | Beschreibung |
| --- | --- | --- |
| `LEGACY` | 21 | Alte Geräte, Camera2 API ist ein Wrapper über der alten Camera API |
| `LIMITED` | 21 | Grundlegende Camera2-Funktionen, keine manuellen Steuerungen |
| `FULL` | 21 | Volle manuelle Steuerungen, RAW-Aufnahme, Burst-Aufnahme |
| `LEVEL_3` | 24 | Erweiterte Funktionen wie YUV-Neuverarbeitung, 10-Bit HDR |

**Wie wird sie verwendet?**  
Überprüfe dies, bevor du fortgeschrittene Operationen durchführst:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // Eingeschränkte Funktionalität
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // Nur grundlegende Funktionen
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // Volle manuelle Steuerungen verfügbar
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // Erweiterte Funktionen verfügbar
    }
}
```

**Wie man mit Android Camera Parameters überprüft:**  
Öffne die App und suche nach „Hardware Level" im Abschnitt Camera Info.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**Was bedeutet es?**  
Dieses Array listet alle von der Kamera unterstützten Fähigkeiten auf.

**Warum gibt es sie?**  
Selbst innerhalb desselben Hardware-Levels können verschiedene Geräte unterschiedliche Funktionen unterstützen.

**Häufige Fähigkeiten:**

| Fähigkeit | Beschreibung |
| --- | --- |
| `BACKWARD_COMPATIBLE` | Grundlegender Kompatibilitätsmodus |
| `MANUAL_SENSOR` | Manuelle ISO- und Belichtungssteuerung |
| `MANUAL_POST_PROCESSING` | Manuelle Farbkorrektur und Rauschunterdrückung |
| `RAW` | RAW-Bildaufnahme |
| `BURST_CAPTURE` | Hochgeschwindigkeits-Burst-Aufnahme |
| `YUV_REPROCESSING` | YUV-Bild-Neuverarbeitung |
| `DEPTH_OUTPUT` | Tiefenkartenausgabe |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | Hochgeschwindigkeits-Videoaufnahme |
| `LOGICAL_MULTI_CAMERA` | Logische Kamera, die mehrere physische Kameras kombiniert |
| `CONCURRENT_CAMERA` | Mehrere Kameras können gleichzeitig geöffnet werden |
| `CAMERA_EXTENSION` | Herstellerspezifische Erweiterungen (Porträt, Nachtmodus) |

**Wie wird sie verwendet?**  
Überprüfe Fähigkeiten, bevor du eine Funktion verwendest:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // RAW-Aufnahme aktivieren
}
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Available Capabilities" im Abschnitt Camera Info.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**Was bedeutet es?**  
Das aktive Array ist der tatsächliche Bereich des Sensors, der für die Bildaufnahme verwendet wird.

**Warum gibt es sie?**  
Der Sensor kann Pixel an den Rändern haben, die für die Kalibrierung reserviert sind. Das aktive Array stellt den nutzbaren Bereich dar.

**Wie wird sie verwendet?**  
Dies sagt dir die maximale Auflösung für die Aufnahme:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Aktives Array: ${width}x$height")
```

**Zugehörige Characteristics:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — Gesamtpixel auf dem Sensor (kann größer als das aktive Array sein)
- `SENSOR_INFO_SENSOR_SIZE` — Physische Abmessungen in Millimetern

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Active Array Size" und „Sensor Size" im Abschnitt Sensor.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**Was bedeutet es?**  
Der maximale digitale Zoomfaktor, der von der Kamera unterstützt wird.

**Warum gibt es sie?**  
Digitaler Zoom beschneidet und vergrößert das Bild, was die Qualität verringert. Die Kenntnis des Maximums hilft, die Erwartungen der Benutzer zu steuern.

**Wie wird sie verwendet?**  
Stelle die Zoomstufe in CaptureRequests ein:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// Zoom einstellen (1.0 = kein Zoom, maxZoom = maximaler Zoom)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**Zugehörige Characteristics:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — Physische Brennweiten (für optischen Zoom)

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Max Digital Zoom" im Abschnitt Scaler.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**Was bedeutet es?**  
Verfügbare Auto-Belichtungsmodi.

**Warum gibt es sie?**  
Verschiedene Geräte unterstützen unterschiedliche AE-Strategien.

**Häufige Modi:**

| Modus | Beschreibung |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | Manuelle Belichtungssteuerung |
| `CONTROL_AE_MODE_ON` | Auto-Belichtung |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | Auto-Belichtung mit immer eingeschaltetem Blitz |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | Auto-Belichtung mit automatischem Blitz |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | Auto-Belichtung mit Reduktion roter Augen |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | Auto-Belichtung mit externem Blitz |

**Wie wird sie verwendet?**  
Stelle den AE-Modus in CaptureRequests ein:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „AE Available Modes" im Abschnitt Control.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**Was bedeutet es?**  
Verfügbare Autofokus-Modi.

**Warum gibt es sie?**  
Verschiedene Fokusstrategien für unterschiedliche Szenarien.

**Häufige Modi:**

| Modus | Beschreibung |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Manueller Fokus |
| `CONTROL_AF_MODE_AUTO` | Einzelbild-Autofokus |
| `CONTROL_AF_MODE_MACRO` | Makrofokus |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Kontinuierlicher Autofokus für Video |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Kontinuierlicher Autofokus für Fotos |
| `CONTROL_AF_MODE_EDGE` | Rand-Autofokus |
| `CONTROL_AF_MODE_FIXED` | Fester Fokus (kein AF) |

**Wie wird sie verwendet?**  
Stelle den AF-Modus basierend auf deinem Anwendungsfall ein:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „AF Available Modes" im Abschnitt Control.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**Was bedeutet es?**  
Verfügbare Auto-Weißabgleich-Modi.

**Häufige Modi:**

| Modus | Beschreibung |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Manueller Weißabgleich |
| `CONTROL_AWB_MODE_AUTO` | Automatisch |
| `CONTROL_AWB_MODE_INCANDESCENT` | Glühlichtbeleuchtung |
| `CONTROL_AWB_MODE_FLUORESCENT` | Leuchtstoffbeleuchtung |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Warmes Leuchtstofflicht |
| `CONTROL_AWB_MODE_DAYLIGHT` | Tageslicht |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Bewölkt |

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „AWB Available Modes" im Abschnitt Control.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**Was bedeutet es?**  
Verfügbare Brennweiten für das/die Objektiv(e).

**Warum gibt es sie?**  
Mehrere Werte deuten auf optische Zoomfähigkeiten hin.

**Wie wird sie verwendet?**  
Bestimme, welche Objektive verfügbar sind:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Brennweite: ${fl}mm")
}
```

**Häufige Brennweiten:**
- 2,4 mm — Weitwinkel (häufig)
- 4,8 mm — Tele (2-facher optischer Zoom)
- 1,8 mm — Ultraweitwinkel

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Available Focal Lengths" im Abschnitt Lens.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**Was bedeutet es?**  
Die kürzeste Entfernung, auf die das Objektiv fokussieren kann.

**Warum gibt es sie?**  
Niedrigere Werte bedeuten eine bessere Makrofähigkeit.

**Wie wird sie verwendet?**  
Überprüfe, ob Makrofotografie möglich ist:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// Ein Wert von 0,1 m (10 cm) oder weniger zeigt eine gute Makrofähigkeit an
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Minimum Focus Distance" im Abschnitt Lens.

---

## 10. FLASH_INFO_AVAILABLE

**Was bedeutet es?**  
Ob die Kamera einen Blitz hat.

**Warum gibt es sie?**  
Nicht alle Kameras haben einen Blitz (insbesondere Frontkameras).

**Wie wird sie verwendet?**  
Überprüfe vor der Verwendung des Blitzes:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // Blitzfunktionen aktivieren
}
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Flash Available" im Abschnitt Flash.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**Was bedeutet es?**  
Die unterstützte minimale und maximale Belichtungszeit.

**Warum gibt es sie?**  
Bestimmt die Schwachlichtfähigkeit und die Fähigkeit, Bewegungen einzufrieren.

**Wie wird sie verwendet?**  
Überprüfe den Belichtungsbereich für die manuelle Steuerung:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// Zur Anzeige in Sekunden umrechnen
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Exposure Time Range" im Abschnitt Sensor.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**Was bedeutet es?**  
Die unterstützten minimalen und maximalen ISO-Werte.

**Warum gibt es sie?**  
Bestimmt die Schwachlichtfähigkeit und das Rauschverhalten.

**Wie wird sie verwendet?**  
Überprüfe den ISO-Bereich für die manuelle Steuerung:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Sensitivity Range" im Abschnitt Sensor.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**Was bedeutet es?**  
Alle unterstützten Ausgabegrößen und -formate.

**Warum gibt es sie?**  
Bestimmt, welche Auflösungen und Formate du verwenden kannst.

**Wie wird sie verwendet?**  
Erhalte unterstützte Größen für verschiedene Anwendungsfälle:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// Vorschau-Größen
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// Foto-Größen
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// Video-Größen
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// RAW-Größen
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Preview Sizes", „Picture Sizes" usw. im Abschnitt Scaler.

---

## 14. LENS_FACING

**Was bedeutet es?**  
In welche Richtung das Objektiv zeigt.

**Warum gibt es sie?**  
Bestimmt, ob es sich um eine Front-, Rück- oder externe Kamera handelt.

**Werte:**
- `LENS_FACING_FRONT` — Selfie-Kamera
- `LENS_FACING_BACK` — Rückkamera
- `LENS_FACING_EXTERNAL` — Externe Kamera

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Lens Facing" im Abschnitt Camera Info.

---

## 15. CONTROL_MAX_REGIONS_AE

**Was bedeutet es?**  
Maximale Anzahl von AE-Messbereichen.

**Warum gibt es sie?**  
Bestimmt, wie präzise die Belichtungsmessung sein kann.

**Wie wird sie verwendet?**  
Begrenze die Anzahl der von dir erstellten AE-Bereiche:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// Erstelle nicht mehr als maxAERegions
```

**Wie man mit Android Camera Parameters überprüft:**  
Suche nach „Max Regions AE" im Abschnitt Control.

---

## Fazit

CameraCharacteristics ist dein Fenster zu den Fähigkeiten der Kamera. Indem du diese Parameter verstehst, kannst du:

1. **Geräteunabhängige Apps erstellen** — Überprüfe Fähigkeiten, bevor du Funktionen verwendest
2. **Bessere Benutzererlebnisse bieten** — Zeige nur verfügbare Funktionen an
3. **Leistung optimieren** — Wähle geeignete Auflösungen und Formate
4. **Professionelle Anwendungen erstellen** — Entfalte das volle Potenzial der Kamera

## Wie man mehr lernt

1. **Android Camera Parameters App** — Erkunde echte Daten von deinem Gerät
2. **Android-Dokumentation** — Lies die offizielle CameraCharacteristics-Dokumentation
3. **Experimentiere** — Schreibe kleine Test-Apps, um verschiedene Parameter auszuprobieren
4. **Quellcode** — Schau dir den Camera2-Quellcode für ein tieferes Verständnis an

## Zusammenfassung

Dieses Kapitel behandelte die wichtigsten CameraCharacteristics:

1. **Hardware Level** — Allgemeine Fähigkeit
2. **Capabilities** — Verfügbare spezifische Funktionen
3. **Active Array** — Sensorauflösung
4. **Digital Zoom** — Zoomfähigkeiten
5. **AE/AF/AWB-Modi** — Automatische Steuerungsmodi
6. **Brennweiten** — Objektivfähigkeiten
7. **Fokusdistanz** — Makrofähigkeit
8. **Blitz** — Blitzverfügbarkeit
9. **Belichtungs-/ISO-Bereich** — Grenzen der manuellen Steuerung
10. **Stream Configuration** — Unterstützte Größen und Formate

Mit diesem Wissen bist du bereit, fortgeschrittene Camera2-Anwendungen zu erstellen!

---

## Letzte Worte

Glückwunsch! Du hast diese Android Camera2-Reihe abgeschlossen. Du verstehst jetzt:

- **Wie Smartphone-Kameras funktionieren** — Objektive, Sensoren, ISP
- **Wie Camera2 funktioniert** — CameraManager, CameraDevice, CaptureSession
- **Wie man Fotos aufnimmt** — JPEG, RAW, ImageReader
- **Wie man die Kamera steuert** — ISO, Belichtung, Fokus, Weißabgleich
- **Professionelle Funktionen** — Hochgeschwindigkeitsvideo, Multi-Kamera
- **Camera Characteristics** — Die Enzyklopädie der Kamerafähigkeiten

Die Android Camera Parameters App ist ein großartiges Werkzeug, um weiter zu lernen. Erkunde die Fähigkeiten deines Geräts und experimentiere mit verschiedenen Einstellungen.

Viel Spaß beim Coden! 📸
