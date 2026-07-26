---
sidebar_position: 6
title: "Chapitre 6 : Lister les caméras"
description: Écrivez votre premier programme Camera2 qui découvre et liste toutes les caméras disponibles sur un appareil Android.
keywords: [lister caméras, CameraManager, énumération caméra, Android Camera2]
---

Il est temps d'écrire votre premier programme Camera2 ! Créons une application qui liste toutes les caméras.

## Introduction

Dans ce chapitre, vous écrirez votre première véritable application Camera2. L'objectif est simple :

> Découvrir toutes les caméras sur l'appareil et afficher leurs informations.

C'est une petite étape mais importante. Avant de pouvoir utiliser une caméra, vous devez la trouver.

## Création du projet

Commençons par créer un nouveau projet Android :

1. Ouvrez Android Studio
2. Créez un nouveau projet avec "Empty Activity"
3. Nommez-le "Camera2List"
4. Sélectionnez Kotlin comme langage
5. Définissez le SDK minimum sur API 21 (Camera2 a été introduit dans API 21)

## Ajout des autorisations

Ajoutez l'autorisation de caméra à `AndroidManifest.xml` :

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## La mise en page

Créez une mise en page simple qui affiche une liste de caméras. Mettez à jour `activity_main.xml` :

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
        android:text="Caméras disponibles"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## L'activité

Écrivons maintenant l'activité principale. C'est ici que le code Camera2 se trouve :

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
                cameraInfoList.add("Aucune caméra trouvée")
            } else {
                cameraInfoList.add("Trouvé ${cameraIds.size} caméra(s) :")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Avant"
                        CameraCharacteristics.LENS_FACING_BACK -> "Arrière"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externe"
                        else -> "Inconnu"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "Inconnu"
                    }
                    
                    cameraInfoList.add("Caméra $index (ID : $cameraId)")
                    cameraInfoList.add("  - Objectif : $lensFacingStr")
                    cameraInfoList.add("  - Niveau matériel : $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("Erreur : Autorisation de caméra refusée")
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
                Toast.makeText(this, "L'autorisation de caméra est requise", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("Autorisation de caméra refusée")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## Ce que fait ce code

Décomposons ce qui se passe :

1. **Obtenir CameraManager** — Nous obtenons le service système CameraManager
2. **Vérifier les autorisations** — Nous vérifions si l'autorisation de caméra est accordée
3. **Lister les caméras** — Nous utilisons `getCameraIdList()` pour obtenir tous les ID de caméra
4. **Obtenir les caractéristiques** — Pour chaque caméra, nous obtenons ses caractéristiques
5. **Afficher les informations** — Nous affichons l'ID de la caméra, l'orientation de l'objectif et le niveau matériel

## Sortie attendue

Lorsque vous exécutez l'application, vous devriez voir quelque chose comme :

```
Trouvé 3 caméra(s) :

Caméra 0 (ID : 0)
  - Objectif : Arrière
  - Niveau matériel : FULL

Caméra 1 (ID : 1)
  - Objectif : Avant
  - Niveau matériel : LIMITED

Caméra 2 (ID : 2)
  - Objectif : Arrière
  - Niveau matériel : FULL
```

## Succès !

Vous venez d'écrire votre premier programme Camera2 ! Cela peut sembler simple, mais c'est la base de tout ce que nous ferons ensuite.

## Dépannage

Si vous rencontrez des problèmes :

1. **Autorisation refusée** — Assurez-vous d'avoir accordé l'autorisation de caméra
2. **Aucune caméra trouvée** — Vérifiez si votre appareil dispose d'une caméra
3. **SecurityException** — Assurez-vous que les autorisations sont déclarées dans le manifest
4. **Niveau API trop bas** — Camera2 nécessite API 21 ou supérieur

## La suite ?

Maintenant que vous pouvez lister les caméras, l'étape suivante consiste à examiner leurs caractéristiques plus en détail. Dans le prochain chapitre, nous :

1. Explorerons CameraCharacteristics
2. Apprendrons l'orientation de l'objectif
3. Comprendrons les niveaux matériels
4. Vérifierons les informations du capteur

## Résumé

Dans ce chapitre, vous avez écrit votre premier programme Camera2. L'application :

1. Demande les autorisations de caméra
2. Utilise CameraManager pour énumérer les caméras
3. Affiche l'ID de la caméra, l'orientation de l'objectif et le niveau matériel

C'est la première étape vers la création d'applications Camera2 complètes. Dans le prochain chapitre, nous plongerons plus profondément dans CameraCharacteristics pour comprendre ce que chaque caméra peut faire.
