---
sidebar_position: 8
title: "Kapitel 8: Deine erste Kamera öffnen"
description: Erfahre, wie du ein CameraDevice mit CameraManager öffnest und den Kamera-Lebenszyklus mit Zustandsrückrufen handhabst.
keywords: [CameraDevice, openCamera, Kamera-Lebenszyklus, CameraManager]
---

Es ist Zeit, deine erste Kamera zu öffnen! Lass uns mehr über CameraDevice erfahren.

## Einleitung

Bisher haben wir gelernt, wie man Kameras entdeckt und ihre Merkmale untersucht. Jetzt machen wir den nächsten Schritt: **eine Kamera öffnen**.

Das Öffnen einer Kamera gibt dir Zugriff auf die tatsächliche Kamera-Hardware. Sobald sie geöffnet ist, kannst du Aufnahmesitzungen erstellen, Vorschauen anzeigen und Fotos aufnehmen.

## Was ist CameraDevice?

CameraDevice repräsentiert eine einzelne Kamera, die mit dem Android-Gerät verbunden ist. Es bietet Methoden zum:
- Erstellen von Aufnahmesitzungen
- Aufnehmen von Standbildern
- Starten und Stoppen der Vorschau

Du erstellst CameraDevice nicht direkt. Stattdessen erhältst du es von CameraManager, indem du `openCamera()` aufrufst.

## Eine Kamera öffnen

So öffnest du eine Kamera:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // Kamera ist einsatzbereit
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // Kamera wurde getrennt
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // Kamera-Fehler ist aufgetreten
        camera.close()
    }
}, null)
```

Lass uns das genauer betrachten.

### Der StateCallback

CameraDevice verwendet ein Rückruf-Muster, da das Öffnen einer Kamera asynchron ist. Der Rückruf hat drei Hauptmethoden:

#### 1. `onOpened(camera: CameraDevice)`

Wird aufgerufen, wenn die Kamera erfolgreich geöffnet wurde. Hier erhältst du deine CameraDevice-Instanz.

#### 2. `onDisconnected(camera: CameraDevice)`

Wird aufgerufen, wenn die Kamera getrennt wird. Dies kann passieren, wenn die Kamera von einer anderen App verwendet wird oder wenn das Gerät heruntergefahren wird. Schließe die Kamera immer in diesem Rückruf.

#### 3. `onError(camera: CameraDevice, error: Int)`

Wird aufgerufen, wenn ein Fehler auftritt. Häufige Fehlercodes:
- `ERROR_CAMERA_IN_USE` — Kamera ist bereits in Verwendung
- `ERROR_MAX_CAMERAS_IN_USE` — Zu viele Kameras geöffnet
- `ERROR_CAMERA_DISABLED` — Kamera ist deaktiviert
- `ERROR_CAMERA_DEVICE` — Kamera-Hardware-Fehler
- `ERROR_CAMERA_SERVICE` — Kamera-Dienst-Fehler

### Der Handler

Der dritte Parameter ist ein `Handler`. Wenn du `null` übergibst, läuft der Rückruf im Looper des aufrufenden Threads. Für UI-Updates möchtest du möglicherweise einen Handler übergeben, der im Hauptthread läuft.

## Ein vollständiges Beispiel

Lass uns eine Activity erstellen, die eine Kamera öffnet:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraOpenActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            openCamera()
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        
        if (cameraIds.isEmpty()) {
            Toast.makeText(this, "Keine Kameras verfügbar", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // Öffne die erste Kamera
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Kamera-Berechtigung verweigert", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Ungültige Kamera-ID", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "Kamera erfolgreich geöffnet!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "Kamera ${camera.id} geöffnet")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "Kamera getrennt", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "Kamera ist in Verwendung"
                ERROR_MAX_CAMERAS_IN_USE -> "Zu viele Kameras geöffnet"
                ERROR_CAMERA_DISABLED -> "Kamera ist deaktiviert"
                ERROR_CAMERA_DEVICE -> "Kamera-Hardware-Fehler"
                ERROR_CAMERA_SERVICE -> "Kamera-Dienst-Fehler"
                else -> "Unbekannter Fehler"
            }
            
            Toast.makeText(this@CameraOpenActivity, "Kamera-Fehler: $errorMessage", Toast.LENGTH_SHORT).show()
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
                openCamera()
            } else {
                Toast.makeText(this, "Kamera-Berechtigung ist erforderlich", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## Der Kamera-Lebenszyklus

Das Verständnis des Kamera-Lebenszyklus ist entscheidend:

1. **Öffnen** — Rufe `openCamera()` auf, um ein CameraDevice zu erhalten
2. **Verwenden** — Erstelle Aufnahmesitzungen, nimm Fotos auf
3. **Schließen** — Rufe `close()` auf, wenn du fertig bist
4. **Freigeben** — Die Kamera ist für andere Apps verfügbar

Schließe die Kamera immer, wenn deine Activity zerstört wird, um Ressourcenlecks zu vermeiden.

## Best Practices

1. **Schließen wenn fertig** — Schließe die Kamera immer in `onDestroy()`
2. **Fehler handhaben** — Ignoriere keine `onError()`-Rückrufe
3. **Berechtigungen prüfen** — Überprüfe Berechtigungen immer vor dem Öffnen
4. **Try-Catch verwenden** — Behandle `SecurityException` und `IllegalArgumentException`
5. **Keine Referenzen halten** — Gib die CameraDevice-Referenz frei, wenn sie geschlossen ist

## Häufige Probleme

### Kamera ist in Verwendung
- Stelle sicher, dass keine andere App die Kamera verwendet
- Überprüfe, ob du die Kamera richtig schließt

### Berechtigung verweigert
- Überprüfe Berechtigungen im Manifest
- Prüfe, ob die Laufzeitberechtigung erteilt wurde

### Kamera-ID nicht gefunden
- Erhalte Kamera-IDs immer von `getCameraIdList()`
- Kodiere Kamera-IDs nicht hart

## Nächstes Kapitel

Jetzt, da du eine Kamera öffnen kannst, ist der nächste Schritt, eine Vorschau anzuzeigen. Im nächsten Kapitel werden wir:

1. Mehr über TextureView erfahren
2. Eine Surface für die Vorschau erstellen
3. Eine CameraCaptureSession erstellen
4. Die Kameravorschau auf dem Bildschirm anzeigen

## Zusammenfassung

Das Öffnen einer Kamera ist der erste Schritt zum Aufnehmen von Bildern:

1. Verwende `CameraManager.openCamera()`, um ein CameraDevice zu erhalten
2. Behandle den StateCallback für `onOpened()`, `onDisconnected()` und `onError()`
3. Schließe die Kamera immer, wenn du fertig bist
4. Folge dem Kamera-Lebenszyklus: öffnen → verwenden → schließen → freigeben

Im nächsten Kapitel erstellen wir eine Kameravorschau, damit du sehen kannst, was die Kamera sieht.
