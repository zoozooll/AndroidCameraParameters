---
sidebar_position: 5
title: "Kapitel 5: Erste Schritte mit Camera2"
description: Erfahre mehr über CameraManager, der Einstiegspunkt zur Android Camera2 API, mit dem du Kameras aufzählen und auf ihre Eigenschaften zugreifen kannst.
keywords: [CameraManager, Camera2 API, Android-Kamera, Kameraaufzählung]
---

Willkommen zum Codierungsteil dieser Serie. Beginnen wir mit der Grundlage: CameraManager.

## Einleitung

Bevor du irgendeine Kamera verwenden kannst, brauchst du eine Möglichkeit, sie zu entdecken und darauf zuzugreifen. Hier kommt **CameraManager** ins Spiel.

CameraManager ist das Tor zur Camera2 API. Es ist die erste Klasse, die du in jeder Camera2-Anwendung verwendest.

## Was ist CameraManager?

CameraManager ist ein Systemdienst, der alle Kamerageräte auf einem Android-Gerät verwaltet. Stell es dir als Verzeichnis oder Register von Kameras vor.

Seine Hauptaufgaben sind:
1. **Kameras aufzählen** — Alle verfügbaren Kameras auflisten
2. **Kameraeigenschaften abrufen** — Detaillierte Informationen zu jeder Kamera erhalten
3. **Kameras öffnen** — Ein CameraDevice für die Aufnahme erstellen

## CameraManager abrufen

Unter Android werden Systemdienste über den `Context` bezogen. So erhältst du CameraManager:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

Das ist alles. Eine Codezeile, um Zugriff auf alle Kameras auf dem Gerät zu erhalten.

## Zuerst die Berechtigungen

Bevor du CameraManager verwendest, musst du Kameraberechtigungen anfordern. Füge diese zu deiner `AndroidManifest.xml` hinzu:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

Und fordere die Laufzeitberechtigung in deiner Aktivität an:

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

Überprüfe immer die Berechtigungen, bevor du auf die Kamera zugreifst.

## CameraManager-Methoden

CameraManager hat drei Hauptmethoden, die du verwenden wirst:

### 1. `getCameraIdList()`

Gibt ein Array von Kamera-ID-Strings zurück. Jede ID repräsentiert ein Kameragerät.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Kamera gefunden: $id")
}
```

Das könnte folgende Ausgabe erzeugen:
```
Kamera 0
Kamera 1
Kamera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

Gibt ein `CameraCharacteristics`-Objekt zurück, das alle Details zu einer bestimmten Kamera enthält.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics enthält Hunderte von Parametern, die die Fähigkeiten der Kamera beschreiben.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

Öffnet eine Kamera und gibt ein `CameraDevice` über den Callback zurück. Wir werden dies später detailliert behandeln.

## Kamera-IDs nochmals betrachtet

Erinnerst du dich aus Kapitel 4, dass Android Kameras numerische IDs zuweist. Die IDs sind nicht garantiert konsistent über verschiedene Geräte oder sogar über Neustarts hinweg.

Häufige Muster:
- **Kamera 0** — Typischerweise die hintere Weitwinkelkamera
- **Kamera 1** — Oft die Frontkamera
- **Kamera 2** — Normalerweise eine Ultra-Weitwinkel- oder Telekamera
- Höhere Zahlen — Zusätzliche Kameras (Makro, Tiefenwahrnehmung usw.)

Aber **niemals voraussetzen**, was eine Kamera-ID bedeutet. Überprüfe immer die Kameraeigenschaften, um Folgendes zu bestimmen:
- Linseausrichtung (vorne/hinten/extern)
- Brennweite
- Fähigkeiten

## Warum CameraManager wichtig ist

CameraManager ist die Grundlage für alles, was wir mit Camera2 machen werden:

1. **Entdeckung** — Bevor du eine Kamera verwendest, musst du sie finden
2. **Information** — Bevor du eine Kamera öffnest, musst du ihre Fähigkeiten kennen
3. **Zugriff** — CameraManager bietet den einzigen Weg, ein Kameragerät zu öffnen

## Ein einfaches Beispiel

Lass uns alles in einem einfachen Beispiel zusammenfügen:

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
        
        Log.d("CameraDiscovery", "${cameraIds.size} Kamera(s) gefunden")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Vorne"
                CameraCharacteristics.LENS_FACING_BACK -> "Hinten"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Extern"
                else -> "Unbekannt"
            }
            
            Log.d("CameraDiscovery", "Kamera $cameraId: $lensFacingStr")
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
                Toast.makeText(this, "Kameraberechtigung erforderlich", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

Diese einfache Aktivität entdeckt alle Kameras und protokolliert ihre IDs und Linseausrichtungen.

## Wichtige Erkenntnisse

- **CameraManager** ist der Einstiegspunkt zu Camera2
- Verwende `getCameraIdList()`, um alle Kameras zu finden
- Verwende `getCameraCharacteristics()`, um detaillierte Informationen zu erhalten
- Fordere immer zuerst Kameraberechtigungen an
- Niemals Kamera-ID-Bedeutungen voraussetzen — überprüfe die Eigenschaften

## Nächstes Kapitel

Nachdem du CameraManager verstanden hast, ist es Zeit, dein erstes echtes Camera2-Programm zu schreiben. Im nächsten Kapitel werden wir:

1. Eine einfache Android-App erstellen
2. Alle verfügbaren Kameras auflisten
3. Kamera-Informationen dem Benutzer anzeigen

Du wirst deinen ersten Camera2-Code schreiben und echte Ergebnisse sehen!

## Zusammenfassung

CameraManager ist die Grundlage von Camera2. Es bietet Zugriff auf:
- Kameraaufzählung
- Kameraeigenschaften
- Kameraöffnung

Mit CameraManager kannst du entdecken, welche Kameras verfügbar sind, und mehr über ihre Fähigkeiten erfahren, bevor du sie öffnest.

Im nächsten Kapitel schreiben wir unser erstes Camera2-Programm, das alle Kameras auf dem Gerät auflistet.
