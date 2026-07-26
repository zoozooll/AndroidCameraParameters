---
sidebar_position: 9
title: "Kapitel 9: Kamera-Vorschau anzeigen"
description: Erfahren Sie, wie Sie die Kamera-Vorschau mit TextureView, Surface und CameraCaptureSession in Camera2 anzeigen.
keywords: [Kamera-Vorschau, TextureView, Surface, CameraCaptureSession, Camera2]
---

Endlich sehen Sie die Kamera-Vorschau! Lassen Sie uns alles miteinander verbinden.

## Einleitung

Eine Kamera zu öffnen ist toll, aber Sie können noch nichts sehen. Um anzuzeigen, was die Kamera sieht, müssen Sie:

1. Eine TextureView erstellen, um die Vorschau anzuzeigen
2. Ein Surface von der TextureView abrufen
3. Eine CameraCaptureSession erstellen
4. Die Vorschau starten

Hier kommen die Teile zusammen.

## TextureView

TextureView ist eine View, die eine `SurfaceTexture` anzeigen kann. Sie eignet sich perfekt für die Anzeige von Kamera-Vorschauen, weil:
- Sie transformiert werden kann (skaliert, gedreht)
- Sie Hardwarebeschleunigung unterstützt
- Sie gut mit Animationen und Übergängen funktioniert

Fügen Sie eine TextureView zu Ihrem Layout hinzu:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</FrameLayout>
```

## Surface

Ein Surface ist ein Puffer, der Bilddaten empfangen kann. Um eine Kamera-Vorschau anzuzeigen:
1. Holen Sie sich die SurfaceTexture von der TextureView
2. Erstellen Sie ein Surface aus der SurfaceTexture
3. Übergeben Sie das Surface an die CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession verwaltet den Aufnahmeprozess. Sie verbindet das Kameragerät mit einem oder mehreren Surfaces.

So erstellen Sie eine Sitzung:
1. Bereiten Sie eine Liste von Surfaces vor (für Vorschau, Fotoaufnahme usw.)
2. Rufen Sie `createCaptureSession()` auf CameraDevice auf
3. Behandeln Sie den Callback

## Ein vollständiges Vorschau-Beispiel

Lassen Sie uns eine Activity erstellen, die eine Kamera-Vorschau anzeigt:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Collections

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            // Größenänderungen bei Bedarf behandeln
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Wird aufgerufen, wenn die Vorschau aktualisiert wird
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

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // Vorschaugrößen abrufen
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // Eine Vorschaugröße wählen
        previewSize = previewSizes?.get(0) // Die erste verfügbare Größe verwenden

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
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
            Toast.makeText(this@CameraPreviewActivity, "Kamerafehler", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val surface = Surface(texture)
        val surfaces = listOf(surface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@CameraPreviewActivity, "Sitzungskonfiguration fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
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
                Toast.makeText(this, "Kameraberechtigung ist erforderlich", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.stopRepeating()
        captureSession?.close()
        cameraDevice?.close()
    }
}
```

## Wie es funktioniert

Lassen Sie uns den Ablauf verfolgen:

1. **TextureView verfügbar** — `onSurfaceTextureAvailable()` wird aufgerufen
2. **Kamera öffnen** — Wir erhalten eine CameraDevice
3. **Aufnahmesitzung erstellen** — Verbinden Sie die Kamera mit dem Surface
4. **Vorschau starten** — Senden Sie eine wiederholte Aufnahmeanforderung

## CaptureRequest

CaptureRequest definiert, was die Kamera aufnehmen soll:
- `TEMPLATE_PREVIEW` — Für den Vorschaumodus
- `TEMPLATE_STILL_CAPTURE` — Für Standfotos
- `TEMPLATE_RECORD` — Für Videoaufzeichnungen
- `TEMPLATE_VIDEO_SNAPSHOT` — Für Schnappschüsse während des Videos

## Die Vorschau-Schleife

Wenn Sie `setRepeatingRequest()` aufrufen, sendet die Kamera kontinuierlich Frames an das Surface. Dadurch entsteht die Live-Vorschau.

## Wichtige Hinweise

1. **Surface muss verfügbar sein** — Warten Sie auf `onSurfaceTextureAvailable()`, bevor Sie die Kamera öffnen
2. **Ressourcen schließen** — Schließen Sie immer die Aufnahmesitzung und das Kameragerät
3. **Ausrichtung behandeln** — Die Vorschau muss je nach Geräteausrichtung gedreht werden
4. **Größe ist wichtig** — Wählen Sie eine Vorschaugröße, die zu den Abmessungen Ihrer TextureView passt

## Erfolg!

Wenn Sie diese App ausführen, sollten Sie eine Live-Kamera-Vorschau auf Ihrem Bildschirm sehen. Herzlichen Glückwunsch! Sie haben Ihre erste Camera2-Vorschau-App erstellt.

## Nächstes Kapitel

Da Sie nun eine Vorschau anzeigen können, besteht der nächste Schritt darin, Fotos aufzunehmen. In Teil III lernen wir:

1. ImageReader zum Aufnehmen von Fotos
2. JPEG- und RAW-Aufnahme
3. CaptureRequest und CaptureResult

## Zusammenfassung

Das Anzeigen einer Kamera-Vorschau umfasst:

1. **TextureView** — Die UI-Komponente zum Anzeigen der Vorschau
2. **Surface** — Der Puffer, der Kamerabilder empfängt
3. **CameraCaptureSession** — Verwaltet den Aufnahmeprozess
4. **CaptureRequest** — Definiert, was aufgenommen werden soll
5. **setRepeatingRequest()** — Startet die kontinuierliche Vorschau-Schleife

Sie haben nun Teil II dieser Serie abgeschlossen. Sie können:
- Kameras entdecken
- Kameramerkmale untersuchen
- Eine Kamera öffnen
- Eine Vorschau anzeigen

In Teil III lernen wir, wie man mit Camera2 Fotos aufnimmt.
