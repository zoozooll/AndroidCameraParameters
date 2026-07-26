---
sidebar_position: 10
title: "Chapitre 10 : Capturer des photos avec ImageReader"
description: Apprenez à capturer des photos fixes à l'aide d'ImageReader, le composant clé pour recevoir des données d'image depuis Camera2.
keywords: [ImageReader, capture de photo, JPEG, Camera2, demande de capture]
---

Maintenant que vous pouvez afficher un aperçu, il est temps de capturer des photos. Découvrons ImageReader.

## Introduction

Pour capturer une photo avec Camera2, vous avez besoin d'un moyen de recevoir les données d'image. C'est là qu'intervient **ImageReader**.

ImageReader agit comme un tampon entre l'appareil photo et votre application. Il reçoit les données d'image de l'appareil photo et les fournit à votre application pour traitement ou sauvegarde.

## Qu'est-ce qu'ImageReader ?

ImageReader est une classe Android qui vous permet de :
- Recevoir des données d'image de l'appareil photo
- Accéder à la dernière image capturée
- Configurer le format et la taille de l'image
- Définir un nombre maximum d'images à mettre en tampon

Vous créez un ImageReader avec un format et une taille spécifiques :

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // Largeur de l'image
    height,     // Hauteur de l'image
    format,     // Format de l'image (par ex., ImageFormat.JPEG)
    maxImages   // Nombre maximum d'images à mettre en tampon
)
```

## Formats d'image

Camera2 prend en charge plusieurs formats d'image :

| Format | Description |
| --- | --- |
| `ImageFormat.JPEG` | Format d'image compressé standard |
| `ImageFormat.RAW_SENSOR` | Données brutes du capteur (avant traitement ISP) |
| `ImageFormat.YUV_420_888` | Format YUV non compressé |
| `ImageFormat.RAW10` | Format brut 10 bits |
| `ImageFormat.RAW12` | Format brut 12 bits |

Pour la plupart des applications, JPEG est le meilleur choix pour la capture de photos.

## Créer un ImageReader

Voici comment créer un ImageReader pour la capture JPEG :

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // Conserver jusqu'à 2 images dans le tampon
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // Traiter l'image
    image.close()
}, null)
```

## Capturer une photo

Pour capturer une photo, vous devez :
1. Créer un ImageReader
2. Ajouter sa Surface à la session de capture
3. Créer une demande de capture avec `TEMPLATE_STILL_CAPTURE`
4. Envoyer la demande à l'appareil photo

## Un exemple complet de capture de photo

Étendons notre application d'aperçu pour capturer des photos :

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

        // Créer un ImageReader pour la capture de photo
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
            Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@PhotoCaptureActivity, "Échec de la configuration de la session", Toast.LENGTH_SHORT).show()
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
        
        // Définir l'autofocus sur prise de vue unique
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
            startPreview() // Reprendre l'aperçu après la capture
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
            Toast.makeText(this, "Photo sauvegardée : $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Échec de la sauvegarde de la photo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "La permission caméra est requise", Toast.LENGTH_SHORT).show()
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

## La mise en page

Ajoutez un bouton de capture à votre mise en page :

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
        android:text="Capturer"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## Comment ça marche

1. **Créer un ImageReader** — Configuré pour recevoir des images JPEG
2. **Ajouter une Surface à la session** — L'appareil photo envoie les photos vers cette surface
3. **Créer une demande de capture** — Utiliser `TEMPLATE_STILL_CAPTURE` pour les photos
4. **Envoyer la demande de capture** — Arrêter l'aperçu, capturer la photo, reprendre l'aperçu
5. **Sauvegarder l'image** — Écrire les données JPEG dans un fichier

## CaptureRequest pour les photos

Pour la capture fixe, utilisez `TEMPLATE_STILL_CAPTURE`. Ce modèle optimise les paramètres pour :
- Une résolution plus élevée
- Une meilleure qualité d'image
- Un autofocus à prise de vue unique

## Gestion des images

N'oubliez jamais de :
1. **Acquérir l'image** — Utiliser `acquireLatestImage()`
2. **La traiter** — Sauvegarder ou afficher l'image
3. **La fermer** — Toujours appeler `image.close()` pour libérer les ressources

## Chapitre suivant

Dans le prochain chapitre, nous découvrirons la capture RAW et comment travailler avec différents formats d'image.

## Résumé

Capturer des photos avec Camera2 implique :

1. **ImageReader** — Reçoit les données d'image de l'appareil photo
2. **Surface** — Ajoutée à la session de capture pour la sortie photo
3. **TEMPLATE_STILL_CAPTURE** — Modèle de demande de capture optimisé
4. **CaptureCallback** — Notifie lorsque la capture est terminée
5. **Traitement d'image** — Sauvegarder ou afficher l'image capturée

Vous avez maintenant appris à capturer des photos de base. Dans le prochain chapitre, nous explorerons la capture RAW et les fonctionnalités photo avancées.
