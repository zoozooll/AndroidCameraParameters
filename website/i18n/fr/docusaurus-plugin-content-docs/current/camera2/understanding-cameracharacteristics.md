---
sidebar_position: 7
title: "Chapitre 7 : Comprendre CameraCharacteristics"
description: Explorez CameraCharacteristics pour découvrir l'orientation de l'objectif, le niveau matériel, la taille du capteur et d'autres capacités importantes de la caméra.
keywords: [CameraCharacteristics, orientation objectif, niveau matériel, taille capteur, capacités caméra]
---

CameraCharacteristics est votre fenêtre sur l'âme de la caméra. Explorons-la.

## Introduction

Dans le chapitre précédent, vous avez appris à lister les caméras et à obtenir des informations de base. Maintenant, nous allons plonger plus profondément dans **CameraCharacteristics** — la description complète des capacités d'une caméra.

CameraCharacteristics contient des centaines de paramètres. Dans ce chapitre, nous nous concentrerons sur les plus importants.

## Qu'est-ce que CameraCharacteristics ?

CameraCharacteristics est un objet immuable qui contient toutes les métadonnées sur un appareil photo. Il décrit :

- **Propriétés matérielles** — Taille du capteur, caractéristiques de l'objectif
- **Capacités** — Ce que la caméra peut faire
- **Modes** — Modes de mise au point, d'exposition et de balance des blancs disponibles
- **Options de sortie** — Résolutions et formats pris en charge
- **Performance** — Fréquences d'images, plages d'exposition

Vous obtenez CameraCharacteristics depuis CameraManager :

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## Clés principales de CameraCharacteristics

Explorons les caractéristiques les plus importantes.

### 1. Orientation de l'objectif

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

Valeurs possibles :
- `LENS_FACING_FRONT` — Caméra frontale (selfie)
- `LENS_FACING_BACK` — Caméra arrière
- `LENS_FACING_EXTERNAL` — Caméra externe

### 2. Niveau matériel

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

C'est l'une des caractéristiques les plus importantes :

| Niveau | Niveau API | Fonctionnalités |
| --- | --- | --- |
| **LEGACY** | 21 | Prise en charge limitée de Camera2, ancienne API Camera enveloppée |
| **LIMITED** | 21 | Fonctionnalités de base de Camera2, pas de contrôles manuels |
| **FULL** | 21 | Contrôles manuels complets, capture RAW |
| **LEVEL_3** | 24 | Fonctionnalités avancées comme le retraitement YUV |

### 3. Taille du capteur

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width et sensorSize.height donnent les dimensions
```

La taille du capteur indique combien de pixels possède le capteur. C'est différent de la résolution de l'image — le capteur peut avoir plus de pixels que ceux utilisés dans une seule capture.

### 4. Taille du tableau actif

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

Le tableau actif est la zone réelle du capteur utilisée pour capturer des images. C'est généralement légèrement plus petit que le tableau de pixels car certains pixels sont réservés pour l'étalonnage.

### 5. Capacités disponibles

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

Ce tableau vous indique quelles fonctionnalités la caméra prend en charge :
- `BACKWARD_COMPATIBLE` — Compatibilité de base
- `MANUAL_SENSOR` — Contrôles manuels du capteur
- `MANUAL_POST_PROCESSING` — Post-traitement manuel
- `RAW` — Prise en charge de la capture RAW
- `BURST_CAPTURE` — Capture en rafale
- `YUV_REPROCESSING` — Retraitement YUV
- `DEPTH_OUTPUT` — Sortie de profondeur
- `CONSTRAINED_HIGH_SPEED_VIDEO` — Vidéo haute vitesse

### 6. Formats de sortie

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

La carte de configuration de flux contient tous les formats et tailles de sortie pris en charge par la caméra :
- `ImageFormat.JPEG` — JPEG standard
- `ImageFormat.RAW_SENSOR` — Données brutes du capteur
- `ImageFormat.YUV_420_888` — Format YUV
- `ImageFormat.RAW10` — RAW 10 bits
- `ImageFormat.RAW12` — RAW 12 bits

### 7. Tailles de prévisualisation prises en charge

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

Cela vous donne toutes les résolutions de prévisualisation disponibles pour la caméra.

### 8. Tailles de photo prises en charge

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

Ce sont les résolutions disponibles pour la capture d'images fixes.

### 9. Longueurs focales

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

Ce tableau contient les longueurs focales (en millimètres) de l'objectif. Plusieurs valeurs indiquent des capacités de zoom optique.

### 10. Plage de distance de mise au point

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

La distance minimale de mise au point indique à quelle distance la caméra peut faire la mise au point. Une valeur plus petite signifie une meilleure capacité macro.

## Un exemple pratique

Créons une application d'informations sur la caméra plus détaillée :

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Orientation de l'objectif
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Avant"
        CameraCharacteristics.LENS_FACING_BACK -> "Arrière"
        else -> "Externe"
    }
    
    // Niveau matériel
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Inconnu"
    }
    
    // Taille du capteur
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Taille du tableau actif
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Longueurs focales
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Inconnu"
    
    // Capacités disponibles
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Rétrocompatible"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Capteur manuel"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "Capture RAW"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Capture en rafale"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "Retraitement YUV"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Sortie de profondeur"
            else -> "Capacité inconnue"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Caméra $cameraId ===")
    Log.d("CameraDetails", "Objectif : $lensFacingStr")
    Log.d("CameraDetails", "Niveau matériel : $hardwareLevelStr")
    Log.d("CameraDetails", "Taille du capteur : $sensorSizeStr")
    Log.d("CameraDetails", "Tableau actif : $activeArrayStr")
    Log.d("CameraDetails", "Longueurs focales : $focalLengthsStr")
    Log.d("CameraDetails", "Capacités : ${capabilitiesList.joinToString(", ")}")
}
```

## Exemple de sortie

```
=== Caméra 0 ===
Objectif : Arrière
Niveau matériel : FULL
Taille du capteur : 4032 x 3024
Tableau actif : 4000 x 3000
Longueurs focales : 2.4mm, 4.8mm
Capacités : Rétrocompatible, Capteur manuel, Capture RAW, Capture en rafale
```

## Pourquoi CameraCharacteristics est important

Avant d'ouvrir une caméra ou de créer une session de capture, vous **devez** vérifier CameraCharacteristics :

1. **Vérifier les capacités** — Ne supposez pas qu'une fonctionnalité est prise en charge
2. **Choisir la bonne caméra** — Sélectionner en fonction de l'orientation de l'objectif, du niveau matériel, etc.
3. **Configurer les sorties** — Utiliser des résolutions et des formats pris en charge
4. **Gérer les différences d'appareils** — Ce qui fonctionne sur un appareil peut ne pas fonctionner sur un autre

## Explorer avec Android Camera Parameters

Ouvrez l'application Android Camera Parameters et parcourez les caractéristiques. Vous verrez des centaines de paramètres organisés par catégorie :

- **Infos caméra** — Informations de base sur la caméra
- **Capteur** — Caractéristiques du capteur
- **Objectif** — Propriétés de l'objectif
- **Contrôle** — Auto-exposition, auto-focus, balance des blancs
- **Scaler** — Tailles et formats de sortie
- **Flash** — Capacités du flash
- **Statistiques** — Sortie de statistiques

Cela vous donne une image complète des capacités de votre caméra.

## Chapitre suivant

Maintenant que vous comprenez CameraCharacteristics, vous êtes prêt à ouvrir votre première caméra ! Dans le prochain chapitre, nous :

1. Découvrirons CameraDevice
2. Ouvrirons une caméra avec CameraManager
3. Gérerons les rappels d'état de la caméra
4. Comprendrons le cycle de vie de la caméra

## Résumé

CameraCharacteristics contient toutes les informations dont vous avez besoin pour comprendre les capacités d'une caméra :

- **Orientation de l'objectif** — Avant, arrière ou externe
- **Niveau matériel** — LEGACY, LIMITED, FULL, LEVEL_3
- **Taille du capteur** — Dimensions physiques
- **Tableau actif** — Zone de capture
- **Longueurs focales** — Capacités de l'objectif
- **Capacités** — Fonctionnalités prises en charge
- **Formats de sortie** — Formats d'image disponibles

Vérifiez toujours CameraCharacteristics avant d'utiliser une caméra. Cela garantit que votre application fonctionne sur différents appareils.

Dans le prochain chapitre, nous ouvrirons notre première caméra avec CameraDevice.
