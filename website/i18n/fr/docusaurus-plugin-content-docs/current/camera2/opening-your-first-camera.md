---
sidebar_position: 8
title: "Chapitre 8 : Ouvrir Votre Première Caméra"
description: Apprenez à ouvrir un CameraDevice à l'aide de CameraManager et à gérer le cycle de vie de la caméra avec des rappels d'état.
keywords: [CameraDevice, openCamera, cycle de vie de la caméra, CameraManager]
---

Il est temps d'ouvrir votre première caméra ! Découvrons CameraDevice.

## Introduction

Jusqu'à présent, nous avons appris à découvrir les caméras et à examiner leurs caractéristiques. Maintenant, nous allons passer à l'étape suivante : **ouvrir une caméra**.

Ouvrir une caméra vous donne accès au matériel photo réel. Une fois ouverte, vous pouvez créer des sessions de capture, afficher des aperçus et capturer des photos.

## Qu'est-ce que CameraDevice ?

CameraDevice représente une seule caméra connectée à l'appareil Android. Il fournit des méthodes pour :
- Créer des sessions de capture
- Capturer des images fixes
- Démarrer et arrêter l'aperçu

Vous ne créez pas CameraDevice directement. Au lieu de cela, vous l'obtenez depuis CameraManager en appelant `openCamera()`.

## Ouvrir une Caméra

Voici comment ouvrir une caméra :

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // La caméra est prête à être utilisée
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // La caméra a été déconnectée
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // Une erreur de caméra s'est produite
        camera.close()
    }
}, null)
```

Décomposons cela.

### Le StateCallback

CameraDevice utilise un modèle de rappel car l'ouverture d'une caméra est asynchrone. Le rappel comporte trois méthodes principales :

#### 1. `onOpened(camera: CameraDevice)`

Appelé lorsque la caméra est ouverte avec succès. C'est ici que vous obtenez votre instance CameraDevice.

#### 2. `onDisconnected(camera: CameraDevice)`

Appelé lorsque la caméra est déconnectée. Cela peut se produire si la caméra est utilisée par une autre application ou si l'appareil est éteint. Fermez toujours la caméra dans ce rappel.

#### 3. `onError(camera: CameraDevice, error: Int)`

Appelé lorsqu'une erreur se produit. Codes d'erreur courants :
- `ERROR_CAMERA_IN_USE` — La caméra est déjà utilisée
- `ERROR_MAX_CAMERAS_IN_USE` — Trop de caméras ouvertes
- `ERROR_CAMERA_DISABLED` — La caméra est désactivée
- `ERROR_CAMERA_DEVICE` — Erreur matérielle de la caméra
- `ERROR_CAMERA_SERVICE` — Erreur du service de la caméra

### Le Handler

Le troisième paramètre est un `Handler`. Si vous passez `null`, le rappel s'exécutera sur le looper du thread appelant. Pour les mises à jour de l'interface utilisateur, vous voudrez peut-être passer un gestionnaire qui s'exécute sur le thread principal.

## Un Exemple Complet

Créons une activité qui ouvre une caméra :

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
            Toast.makeText(this, "Aucune caméra disponible", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // Ouvrir la première caméra
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Autorisation de la caméra refusée", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "ID de caméra invalide", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "Caméra ouverte avec succès !", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "Caméra ${camera.id} ouverte")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "Caméra déconnectée", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "La caméra est utilisée"
                ERROR_MAX_CAMERAS_IN_USE -> "Trop de caméras ouvertes"
                ERROR_CAMERA_DISABLED -> "La caméra est désactivée"
                ERROR_CAMERA_DEVICE -> "Erreur matérielle de la caméra"
                ERROR_CAMERA_SERVICE -> "Erreur du service de la caméra"
                else -> "Erreur inconnue"
            }
            
            Toast.makeText(this@CameraOpenActivity, "Erreur de caméra : $errorMessage", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "L'autorisation de la caméra est requise", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## Le Cycle de Vie de la Caméra

Comprendre le cycle de vie de la caméra est crucial :

1. **Ouvrir** — Appelez `openCamera()` pour obtenir un CameraDevice
2. **Utiliser** — Créez des sessions de capture, capturez des photos
3. **Fermer** — Appelez `close()` lorsque vous avez terminé
4. **Libérer** — La caméra est disponible pour d'autres applications

Fermez toujours la caméra lorsque votre activité est détruite pour éviter les fuites de ressources.

## Bonnes Pratiques

1. **Fermer lorsque vous avez terminé** — Fermez toujours la caméra dans `onDestroy()`
2. **Gérer les erreurs** — N'ignorez pas les rappels `onError()`
3. **Vérifier les autorisations** — Vérifiez toujours les autorisations avant d'ouvrir
4. **Utiliser try-catch** — Gérez `SecurityException` et `IllegalArgumentException`
5. **Ne pas conserver de références** — Libérez la référence CameraDevice lorsqu'elle est fermée

## Problèmes Courants

### La caméra est utilisée
- Assurez-vous qu'aucune autre application n'utilise la caméra
- Vérifiez que vous fermez correctement la caméra

### Autorisation refusée
- Vérifiez les autorisations dans le manifeste
- Vérifiez que l'autorisation d'exécution est accordée

### ID de caméra introuvable
- Obtenez toujours les ID de caméra depuis `getCameraIdList()`
- Ne codez pas en dur les ID de caméra

## Chapitre Suivant

Maintenant que vous pouvez ouvrir une caméra, l'étape suivante consiste à afficher un aperçu. Dans le prochain chapitre, nous allons :

1. Découvrir TextureView
2. Créer une Surface pour l'aperçu
3. Créer un CameraCaptureSession
4. Afficher l'aperçu de la caméra à l'écran

## Résumé

Ouvrir une caméra est la première étape vers la capture d'images :

1. Utilisez `CameraManager.openCamera()` pour obtenir un CameraDevice
2. Gérez le StateCallback pour `onOpened()`, `onDisconnected()` et `onError()`
3. Fermez toujours la caméra lorsque vous avez terminé
4. Suivez le cycle de vie de la caméra : ouvrir → utiliser → fermer → libérer

Dans le prochain chapitre, nous créerons un aperçu de caméra afin que vous puissiez voir ce que la caméra voit.
