---
sidebar_position: 10
title: "Kapitel 10: Fotos aufnehmen mit ImageReader"
description: Lerne, wie man Standfotos mit ImageReader aufnimmt — der Schlüsselkomponente zum Empfangen von Bilddaten von Camera2.
keywords: [ImageReader, Fotoaufnahme, JPEG, Camera2, Capture Request]
---

Jetzt, da du eine Vorschau anzeigen kannst, ist es Zeit, Fotos aufzunehmen. Lerne uns etwas über ImageReader.

## Einleitung

Um ein Foto mit Camera2 aufzunehmen, benötigst du eine Möglichkeit, die Bilddaten zu empfangen. Hier kommt **ImageReader** ins Spiel.

ImageReader fungiert als Puffer zwischen der Kamera und deiner Anwendung. Er empfängt Bilddaten von der Kamera und stellt sie deiner App zur Verarbeitung oder Speicherung zur Verfügung.

## Was ist ImageReader?

ImageReader ist eine Android-Klasse, mit der du:
- Bilddaten von der Kamera empfangen
- Auf das zuletzt aufgenommene Bild zugreifen
- Das Bildformat und die Größe konfigurieren
- Eine maximale Anzahl von zu puffernden Bildern festlegen

Du erstellst einen ImageReader mit einem bestimmten Format und einer bestimmten Größe:

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // Bildbreite
    height,     // Bildhöhe
    format,     // Bildformat (z. B. ImageFormat.JPEG)
    maxImages   // Maximale Anzahl von zu puffernden Bildern
)
```

## Bildformate

Camera2 unterstützt mehrere Bildformate:

| Format | Beschreibung |
| --- | --- |
| `ImageFormat.JPEG` | Standardmäßiges komprimiertes Bildformat |
| `ImageFormat.RAW_SENSOR` | Rohe Sensordaten (vor der ISP-Verarbeitung) |
| `ImageFormat.YUV_420_888` | Unkomprimiertes YUV-Format |
| `ImageFormat.RAW10` | 10-Bit-Rohformat |
| `ImageFormat.RAW12` | 12-Bit-Rohformat |

Für die meisten Anwendungen ist JPEG die beste Wahl für die Fotoaufnahme.

## Erstellen eines ImageReaders

So erstellst du einen ImageReader für die JPEG-Aufnahme:

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // Bis zu 2 Bilder im Puffer behalten
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // Bild verarbeiten
    image.close()
}, null)
```

## Ein Foto aufnehmen

Um ein Foto aufzunehmen, musst du:
1. Einen ImageReader erstellen
2. Seine Surface zur Capture-Session hinzufügen
3. Einen Capture-Request mit `TEMPLATE_STILL_CAPTURE` erstellen
4. Die Anfrage an die Kamera senden

## Ein vollständiges Fotoaufnahmebeispiel

Lass uns unsere Vorschau-App erweitern, um Fotos aufzunehmen:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoCaptureActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null
    private var pictureSize: Size? = null
    private var imageReader: ImageReader? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_capture)
        
        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.captureButton)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) return

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
        
        previewSize = previewSizes?.get(0)
        pictureSize = pictureSizes?.get(0)

        // ImageReader für die Fotoaufnahme erstellen
        imageReader = ImageReader.newInstance(
            pictureSize?.width ?: 0,
            pictureSize?.height ?: 0,
            ImageFormat.JPEG,
            2
        )
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            saveImage(image)
            image.close()
        }, null)

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
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val previewSurface = Surface(texture)
        val readerSurface = imageReader?.surface ?: return
        
        val surfaces = listOf(previewSurface, readerSurface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@PhotoCaptureActivity, "Session-Konfiguration fehlgeschlagen", Toast.LENGTH_SHORT).show()
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

    private fun takePhoto() {
        val readerSurface = imageReader?.surface ?: return
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder?.addTarget(readerSurface)
        
        // Autofokus auf Einzelaufnahme setzen
        captureRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        
        captureRequestBuilder?.let { builder ->
            captureSession?.stopRepeating()
            captureSession?.capture(builder.build(), captureCallback, null)
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            startPreview() // Vorschau nach der Aufnahme fortsetzen
        }
    }

    private fun saveImage(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, fileName)
        
        try {
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            Toast.makeText(this, "Foto gespeichert: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Speichern des Fotos fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
        imageReader?.close()
    }
}
```

## Das Layout

Füge deinem Layout eine Aufnahmeschaltfläche hinzu:

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

    <Button
        android:id="@+id/captureButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Aufnehmen"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## Wie es funktioniert

1. **ImageReader erstellen** — Für den Empfang von JPEG-Bildern einrichten
2. **Surface zur Session hinzufügen** — Die Kamera sendet Fotos an diese Surface
3. **Capture-Request erstellen** — `TEMPLATE_STILL_CAPTURE` für Fotos verwenden
4. **Capture-Request senden** — Vorschau anhalten, Foto aufnehmen, Vorschau fortsetzen
5. **Bild speichern** — Die JPEG-Daten in eine Datei schreiben

## CaptureRequest für Fotos

Für Standaufnahmen verwende `TEMPLATE_STILL_CAPTURE`. Diese Vorlage optimiert Einstellungen für:
- Höhere Auflösung
- Bessere Bildqualität
- Einzelbild-Autofokus

## Umgang mit Bildern

Denke immer daran:
1. **Bild abrufen** — Verwende `acquireLatestImage()`
2. **Verarbeiten** — Speichere oder zeige das Bild an
3. **Schließen** — Rufe immer `image.close()` auf, um Ressourcen freizugeben

## Nächstes Kapitel

Im nächsten Kapitel lernen wir etwas über RAW-Aufnahme und den Umgang mit verschiedenen Bildformaten.

## Zusammenfassung

Das Aufnehmen von Fotos mit Camera2 beinhaltet:

1. **ImageReader** — Empfängt Bilddaten von der Kamera
2. **Surface** — Wird zur Capture-Session für die Fotoausgabe hinzugefügt
3. **TEMPLATE_STILL_CAPTURE** — Optimierte Capture-Request-Vorlage
4. **CaptureCallback** — Benachrichtigt, wenn die Aufnahme abgeschlossen ist
5. **Bildverarbeitung** — Speichere oder zeige das aufgenommene Bild an

Du hast nun gelernt, wie man grundlegende Fotos aufnimmt. Im nächsten Kapitel erkunden wir die RAW-Aufnahme und erweiterte Fotofunktionen.
