---
sidebar_position: 5
title: "Chapitre 5 : Prise en main de Camera2"
description: Apprenez-en plus sur CameraManager, le point d'entrée de l'API Android Camera2 qui vous permet d'énumérer les caméras et d'accéder à leurs caractéristiques.
keywords: [CameraManager, Camera2 API, caméra Android, énumération de caméras]
---

Bienvenue dans la partie codage de cette série. Commençons par les fondations : CameraManager.

## Introduction

Avant de pouvoir utiliser une caméra, vous avez besoin d'un moyen de la découvrir et d'y accéder. C'est là qu'intervient **CameraManager**.

CameraManager est la passerelle vers l'API Camera2. C'est la première classe que vous utiliserez dans toute application Camera2.

## Qu'est-ce que CameraManager ?

CameraManager est un service système qui gère tous les périphériques de caméra sur un appareil Android. Voyez-le comme un annuaire ou un registre des caméras.

Ses principales responsabilités sont :
1. **Énumérer les caméras** — Lister toutes les caméras disponibles
2. **Obtenir les caractéristiques de la caméra** — Récupérer des informations détaillées sur chaque caméra
3. **Ouvrir les caméras** — Créer un CameraDevice pour la capture

## Obtention de CameraManager

Sous Android, les services système sont obtenus via le `Context`. Voici comment obtenir CameraManager :

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

C'est tout. Une ligne de code pour accéder à toutes les caméras de l'appareil.

## Les autorisations d'abord

Avant d'utiliser CameraManager, vous devez demander les autorisations de caméra. Ajoutez-les à votre `AndroidManifest.xml` :

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

Et demandez l'autorisation au moment de l'exécution dans votre activité :

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

Vérifiez toujours les autorisations avant d'accéder à la caméra.

## Méthodes de CameraManager

CameraManager dispose de trois méthodes principales que vous utiliserez :

### 1. `getCameraIdList()`

Retourne un tableau de chaînes d'identifiants de caméra. Chaque identifiant représente un périphérique de caméra.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Caméra trouvée : $id")
}
```

Cela peut afficher :
```
Caméra 0
Caméra 1
Caméra 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

Retourne un objet `CameraCharacteristics` contenant tous les détails sur une caméra spécifique.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics contient des centaines de paramètres décrivant les capacités de la caméra.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

Ouvre une caméra et retourne un `CameraDevice` via le rappel. Nous aborderons cela en détail plus tard.

## Les identifiants de caméra revisités

Rappelez-vous du Chapitre 4 qu'Android attribue des identifiants numériques aux caméras. Ces identifiants ne sont pas garantis d'être cohérents d'un appareil à l'autre, ni même d'un redémarrage à l'autre.

Modèles courants :
- **Caméra 0** — Généralement la caméra arrière grand-angle
- **Caméra 1** — Souvent la caméra frontale
- **Caméra 2** — Habituellement une caméra ultra-grand-angle ou téléobjectif
- Numéros supérieurs — Caméras supplémentaires (macro, profondeur, etc.)

Mais **ne supposez jamais** la signification d'un identifiant de caméra. Vérifiez toujours les caractéristiques de la caméra pour déterminer :
- L'orientation de l'objectif (frontale/arrière/externe)
- La distance focale
- Les capacités

## Pourquoi CameraManager est important

CameraManager est le fondement de tout ce que nous ferons avec Camera2 :

1. **Découverte** — Avant d'utiliser une caméra, vous devez la trouver
2. **Information** — Avant d'ouvrir une caméra, vous devez connaître ses capacités
3. **Accès** — CameraManager fournit le seul moyen d'ouvrir un périphérique de caméra

## Un exemple simple

Mettons tout cela en œuvre dans un exemple simple :

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
        
        Log.d("CameraDiscovery", "${cameraIds.size} caméra(s) trouvée(s)")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Frontale"
                CameraCharacteristics.LENS_FACING_BACK -> "Arrière"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externe"
                else -> "Inconnue"
            }
            
            Log.d("CameraDiscovery", "Caméra $cameraId : $lensFacingStr")
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
                Toast.makeText(this, "Autorisation de caméra requise", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

Cette activité simple découvre toutes les caméras et enregistre leurs identifiants et l'orientation de leur objectif.

## Points clés à retenir

- **CameraManager** est le point d'entrée de Camera2
- Utilisez `getCameraIdList()` pour trouver toutes les caméras
- Utilisez `getCameraCharacteristics()` pour obtenir des informations détaillées
- Demandez toujours les autorisations de caméra en premier
- Ne supposez jamais la signification des identifiants de caméra — vérifiez les caractéristiques

## Chapitre suivant

Maintenant que vous comprenez CameraManager, il est temps d'écrire votre premier vrai programme Camera2. Dans le prochain chapitre, nous allons :

1. Créer une application Android simple
2. Lister toutes les caméras disponibles
3. Afficher les informations de la caméra à l'utilisateur

Vous écrirez votre premier code Camera2 et verrez de vrais résultats !

## Résumé

CameraManager est le fondement de Camera2. Il fournit un accès à :
- L'énumération des caméras
- Les caractéristiques des caméras
- L'ouverture des caméras

Avec CameraManager, vous pouvez découvrir quelles caméras sont disponibles et apprendre leurs capacités avant de les ouvrir.

Dans le prochain chapitre, nous écrirons notre premier programme Camera2 qui liste toutes les caméras de l'appareil.
