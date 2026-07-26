---
sidebar_position: 6
title: "Kapitel 6: Kameras auflisten"
description: Schreibe dein erstes Camera2-Programm, das alle verfügbaren Kameras auf einem Android-Gerät entdeckt und auflistet.
keywords: [Kameras auflisten, CameraManager, Kamera-Aufzählung, Android Camera2]
---

Es ist Zeit, dein erstes Camera2-Programm zu schreiben! Lass uns eine App erstellen, die alle Kameras auflistet.

## Einleitung

In diesem Kapitel schreibst du deine erste echte Camera2-Anwendung. Das Ziel ist einfach:

> Entdecke alle Kameras auf dem Gerät und zeige ihre Informationen an.

Dies ist ein kleiner, aber wichtiger Schritt. Bevor du eine Kamera verwenden kannst, musst du sie finden.

## Projekt erstellen

Lass uns zunächst ein neues Android-Projekt erstellen:

1. Android Studio öffnen
2. Ein neues Projekt mit „Empty Activity“ erstellen
3. Nenne es „Camera2List“
4. Wähle Kotlin als Sprache aus
5. Setze das minimale SDK auf API 21 (Camera2 wurde in API 21 eingeführt)

## Berechtigungen hinzufügen

Füge die Kamera-Berechtigung zu `AndroidManifest.xml` hinzu:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## Das Layout

Erstelle ein einfaches Layout, das eine Liste von Kameras anzeigt. Aktualisiere `activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Verfügbare Kameras"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## Die Activity

Jetzt schreiben wir die Main Activity. Hier kommt der Camera2-Code hin:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraListView: ListView
    private val cameraInfoList = mutableListOf<String>()

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraListView = findViewById(R.id.cameraListView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (hasCameraPermission()) {
            listCameras()
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

    private fun listCameras() {
        cameraInfoList.clear()
        
        try {
            val cameraIds = cameraManager.cameraIdList
            
            if (cameraIds.isEmpty()) {
                cameraInfoList.add("Keine Kameras gefunden")
            } else {
                cameraInfoList.add("${cameraIds.size} Kamera(s) gefunden:")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                        CameraCharacteristics.LENS_FACING_BACK -> "Rückseite"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "Extern"
                        else -> "Unbekannt"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "Unbekannt"
                    }
                    
                    cameraInfoList.add("Kamera $index (ID: $cameraId)")
                    cameraInfoList.add("  - Objektiv: $lensFacingStr")
                    cameraInfoList.add("  - Hardware-Level: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("Fehler: Kamera-Berechtigung verweigert")
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
                listCameras()
            } else {
                Toast.makeText(this, "Kamera-Berechtigung ist erforderlich", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("Kamera-Berechtigung verweigert")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## Was dieser Code tut

Lass uns aufschlüsseln, was passiert:

1. **CameraManager abrufen** — Wir holen uns den CameraManager-Systemdienst
2. **Berechtigungen prüfen** — Wir prüfen, ob die Kamera-Berechtigung erteilt ist
3. **Kameras auflisten** — Wir verwenden `getCameraIdList()`, um alle Kamera-IDs zu erhalten
4. **Merkmale abrufen** — Für jede Kamera rufen wir ihre Merkmale ab
5. **Informationen anzeigen** — Wir zeigen die Kamera-ID, die Objektivausrichtung und das Hardware-Level an

## Erwartete Ausgabe

Wenn du die App ausführst, solltest du so etwas sehen:

```
3 Kamera(s) gefunden:

Kamera 0 (ID: 0)
  - Objektiv: Rückseite
  - Hardware-Level: FULL

Kamera 1 (ID: 1)
  - Objektiv: Front
  - Hardware-Level: LIMITED

Kamera 2 (ID: 2)
  - Objektiv: Rückseite
  - Hardware-Level: FULL
```

## Erfolg!

Du hast gerade dein erstes Camera2-Programm geschrieben! Es mag einfach erscheinen, aber dies ist die Grundlage für alles, was wir als Nächstes tun werden.

## Fehlerbehebung

Wenn du auf Probleme stößt:

1. **Berechtigung verweigert** — Stelle sicher, dass du die Kamera-Berechtigung erteilt hast
2. **Keine Kameras gefunden** — Prüfe, ob dein Gerät eine Kamera hat
3. **SecurityException** — Stelle sicher, dass Berechtigungen im Manifest deklariert sind
4. **API-Level zu niedrig** — Camera2 erfordert API 21 oder höher

## Was kommt als Nächstes?

Jetzt, da du Kameras auflisten kannst, besteht der nächste Schritt darin, ihre Merkmale genauer zu untersuchen. Im nächsten Kapitel werden wir:

1. CameraCharacteristics erkunden
2. Mehr über Objektivausrichtung erfahren
3. Hardware-Levels verstehen
4. Sensorinformationen prüfen

## Zusammenfassung

In diesem Kapitel hast du dein erstes Camera2-Programm geschrieben. Die App:

1. Fordert Kamera-Berechtigungen an
2. Verwendet CameraManager, um Kameras aufzuzählen
3. Zeigt Kamera-ID, Objektivausrichtung und Hardware-Level an

Dies ist der erste Schritt zum Erstellen vollständiger Camera2-Anwendungen. Im nächsten Kapitel werden wir tiefer in CameraCharacteristics eintauchen, um zu verstehen, was jede Kamera kann.
