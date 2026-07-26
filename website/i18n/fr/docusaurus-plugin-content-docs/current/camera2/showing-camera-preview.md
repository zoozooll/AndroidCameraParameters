---
sidebar_position: 9
title: "Chapitre 9 : Affichage de l'aperçu de l'appareil photo"
description: Apprenez à afficher l'aperçu de l'appareil photo à l'aide de TextureView, Surface et CameraCaptureSession dans Camera2.
keywords: [aperçu caméra, TextureView, Surface, CameraCaptureSession, Camera2]
---

Enfin, vous allez voir l'aperçu de l'appareil photo ! Rassemblons tout.

## Introduction

Ouvrir un appareil photo, c'est bien, mais vous ne voyez rien encore. Pour afficher ce que l'appareil photo voit, vous devez :

1. Créer un TextureView pour afficher l'aperçu
2. Récupérer un Surface depuis le TextureView
3. Créer un CameraCaptureSession
4. Démarrer l'aperçu

C'est ici que les éléments s'assemblent.

## TextureView

TextureView est une vue qui peut afficher un `SurfaceTexture`. Il est parfait pour afficher des aperçus d'appareil photo car :
- Il peut être transformé (mis à l'échelle, tourné)
- Il prend en charge l'accélération matérielle
- Il fonctionne bien avec les animations et les transitions

Ajoutez un TextureView à votre mise en page :

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

Un Surface est une mémoire tampon qui peut recevoir des données d'image. Pour afficher un aperçu d'appareil photo :
1. Récupérez le SurfaceTexture depuis le TextureView
2. Créez un Surface à partir du SurfaceTexture
3. Passez le Surface au CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession gère le processus de capture. Il connecte l'appareil photo à un ou plusieurs Surfaces.

Pour créer une session :
1. Préparez une liste de Surfaces (pour l'aperçu, la capture de photo, etc.)
2. Appelez `createCaptureSession()` sur CameraDevice
3. Gérez le rappel

## Un exemple complet d'aperçu

Créons une activité qui affiche un aperçu d'appareil photo :

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
            // Gérer les changements de taille si nécessaire
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Appelé lorsque l'aperçu est mis à jour
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
            Toast.makeText(this, "Aucun appareil photo disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // Récupérer les tailles d'aperçu
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // Choisir une taille d'aperçu
        previewSize = previewSizes?.get(0) // Utiliser la première taille disponible

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
            Toast.makeText(this@CameraPreviewActivity, "Erreur de l'appareil photo", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "Échec de la configuration de la session", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "La permission caméra est requise", Toast.LENGTH_SHORT).show()
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

## Comment ça marche

Suivons le flux :

1. **TextureView disponible** — `onSurfaceTextureAvailable()` est appelé
2. **Ouvrir l'appareil photo** — Nous obtenons un CameraDevice
3. **Créer la session de capture** — Connecter l'appareil photo au Surface
4. **Démarrer l'aperçu** — Envoyer une requête de capture répétée

## CaptureRequest

CaptureRequest définit ce que l'appareil photo doit capturer :
- `TEMPLATE_PREVIEW` — Pour le mode aperçu
- `TEMPLATE_STILL_CAPTURE` — Pour les photos fixes
- `TEMPLATE_RECORD` — Pour l'enregistrement vidéo
- `TEMPLATE_VIDEO_SNAPSHOT` — Pour les captures instantanées pendant la vidéo

## La boucle d'aperçu

Lorsque vous appelez `setRepeatingRequest()`, l'appareil photo envoie continuellement des images au Surface. Cela crée l'aperçu en direct.

## Notes importantes

1. **Le Surface doit être disponible** — Attendez `onSurfaceTextureAvailable()` avant d'ouvrir l'appareil photo
2. **Fermer les ressources** — Fermez toujours la session de capture et l'appareil photo
3. **Gérer l'orientation** — L'aperçu peut nécessiter une rotation selon l'orientation de l'appareil
4. **La taille compte** — Choisissez une taille d'aperçu qui correspond aux dimensions de votre TextureView

## Réussi !

Lorsque vous exécutez cette application, vous devriez voir un aperçu en direct de l'appareil photo sur votre écran. Félicitations ! Vous avez construit votre première application d'aperçu Camera2.

## Chapitre suivant

Maintenant que vous pouvez afficher un aperçu, l'étape suivante consiste à capturer des photos. Dans la Partie III, nous apprendrons :

1. ImageReader pour capturer des photos
2. Capture JPEG et RAW
3. CaptureRequest et CaptureResult

## Résumé

L'affichage d'un aperçu d'appareil photo implique :

1. **TextureView** — Le composant UI pour afficher l'aperçu
2. **Surface** — La mémoire tampon qui reçoit les images de l'appareil photo
3. **CameraCaptureSession** — Gère le processus de capture
4. **CaptureRequest** — Définit ce qu'il faut capturer
5. **setRepeatingRequest()** — Démarre la boucle d'aperçu continue

Vous avez maintenant terminé la Partie II de cette série. Vous savez :
- Découvrir les appareils photo
- Examiner les caractéristiques de l'appareil photo
- Ouvrir un appareil photo
- Afficher un aperçu

Dans la Partie III, nous apprendrons comment capturer des photos avec Camera2.
