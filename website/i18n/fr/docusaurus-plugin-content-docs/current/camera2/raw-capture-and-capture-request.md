---
sidebar_position: 11
title: "Chapitre 11 : Capture RAW et CaptureRequest"
description: Apprenez à capturer des images RAW et à comprendre CaptureRequest et CaptureResult pour un contrôle avancé de la caméra.
keywords: [capture RAW, CaptureRequest, CaptureResult, Camera2, contrôles manuels]
---

La capture RAW vous donne un contrôle complet sur le traitement d'image. Explorons-la avec CaptureRequest et CaptureResult.

## Introduction

Dans le chapitre précédent, vous avez appris à capturer des photos JPEG. Maintenant, nous allons explorer :

1. **Capture RAW** — Capturer des données de capteur non traitées
2. **CaptureRequest** — Configurer les paramètres de la caméra pour chaque capture
3. **CaptureResult** — Obtenir les métadonnées sur les captures terminées

## Qu'est-ce que RAW ?

Les images RAW contiennent toutes les données capturées par le capteur avant le traitement de l'ISP. Cela signifie :

- Aucune réduction de bruit appliquée
- Aucune correction de balance des blancs
- Aucun renforcement de la netteté
- Plage dynamique complète

Les fichiers RAW sont plus volumineux mais offrent une flexibilité d'édition inégalée.

## Conditions requises pour la capture RAW

Pour capturer des images RAW, votre caméra doit :
1. Disposer d'un niveau matériel **FULL** ou **LEVEL_3**
2. Prendre en charge la fonctionnalité `RAW`

Vérifiez CameraCharacteristics :

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## Capturer des images RAW

Créez un ImageReader avec un format RAW :

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // ou ImageFormat.RAW10/RAW12
    2
)
```

Ajoutez ensuite les surfaces JPEG et RAW à la session de capture pour une capture simultanée :

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest définit tous les paramètres pour une capture unique. Vous pouvez configurer :

### Contrôles automatiques
- `CONTROL_AF_MODE` — Mode de mise au point automatique
- `CONTROL_AE_MODE` — Mode d'exposition automatique
- `CONTROL_AWB_MODE` — Mode de balance des blancs automatique

### Contrôles manuels
- `SENSOR_SENSITIVITY` — Valeur ISO
- `SENSOR_EXPOSURE_TIME` — Temps d'exposition en nanosecondes
- `LENS_FOCUS_DISTANCE` — Distance de mise au point
- `LENS_APERTURE` — Ouverture (si disponible)

### Paramètres de sortie
- `JPEG_QUALITY` — Qualité de compression JPEG
- `JPEG_ORIENTATION` — Orientation de l'image
- `COLOR_CORRECTION_MODE` — Mode de correction des couleurs

### Créer un CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// Définir les contrôles automatiques
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// Définir la qualité JPEG
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// Ajouter les cibles
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// Construire la requête
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult contient les métadonnées sur une capture terminée. Il inclut :

- **Paramètres réels utilisés** — Ce que la caméra a réellement appliqué
- **Statistiques** — Informations d'exposition, de mise au point et de couleur
- **Horodatage** — Moment où la capture a eu lieu

### Obtenir CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // Obtenir le temps d'exposition réel
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // Obtenir l'ISO réel
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // Obtenir l'état de la mise au point
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // Obtenir l'état AE
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "Exposition : ${exposureTime ?: 0}ns, ISO : $iso")
    }
}
```

### Clés CaptureResult courantes

| Clé | Description |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | Temps d'exposition réel utilisé |
| `SENSOR_SENSITIVITY` | ISO réel utilisé |
| `CONTROL_AF_STATE` | État de la mise au point automatique |
| `CONTROL_AE_STATE` | État de l'exposition automatique |
| `CONTROL_AWB_STATE` | État de la balance des blancs automatique |
| `SCALER_CROP_REGION` | Région de recadrage utilisée |
| `COLOR_CORRECTION_GAINS` | Gains de correction des couleurs |

## Un exemple complet de capture RAW

```kotlin
private fun configureDualCapture() {
    // Créer l'ImageReader JPEG
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // Créer l'ImageReader RAW
    val rawReader = ImageReader.newInstance(
        rawSize.width, rawSize.height,
        ImageFormat.RAW_SENSOR, 2
    )
    
    jpegReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveJpeg(image)
        image.close()
    }, null)
    
    rawReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveRaw(image)
        image.close()
    }, null)
    
    // Créer les surfaces
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // Créer la session de capture avec toutes les surfaces
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // Ajouter les deux surfaces comme cibles
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // Configurer les contrôles
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.JPEG_QUALITY, 100)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.capture(builder.build(), captureCallback, null)
    }
}

private fun saveRaw(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val fileName = "RAW_${System.currentTimeMillis()}.dng"
    val file = File(externalCacheDir, fileName)
    
    FileOutputStream(file).use { fos ->
        fos.write(bytes)
    }
}
```

## RAW vs JPEG

| Fonctionnalité | RAW | JPEG |
| --- | --- | --- |
| Taille du fichier | Volumineux (20-50 Mo) | Petit (2-10 Mo) |
| Flexibilité d'édition | Maximale | Limitée |
| Bruit | Préservé | Réduit |
| Balance des blancs | Ajustable | Fixe |
| Plage dynamique | Complète | Compressée |

## Bonnes pratiques

1. **Vérifier la prise en charge RAW** — Vérifiez toujours avant de tenter une capture RAW
2. **Capture double** — Capturez à la fois JPEG et RAW pour plus de flexibilité
3. **Fermer les images** — Appelez toujours `image.close()` après le traitement
4. **Gérer les différents formats** — RAW_SENSOR, RAW10 et RAW12 ont des dispositions d'octets différentes

## Chapitre suivant

Dans le prochain chapitre, nous résumerons ce que vous avez appris dans la Partie III et nous préparerons la Partie IV : Contrôles manuels de la caméra.

## Résumé

Dans ce chapitre, vous avez appris :

1. **Capture RAW** — Capturer des données de capteur non traitées pour une flexibilité d'édition maximale
2. **CaptureRequest** — Configurer les paramètres de la caméra pour chaque capture
3. **CaptureResult** — Obtenir les métadonnées sur les captures terminées

La capture RAW nécessite un niveau matériel FULL ou LEVEL_3. Vous pouvez capturer à la fois JPEG et RAW simultanément en ajoutant les deux surfaces à la session de capture.

CaptureRequest vous permet de configurer la mise au point automatique, l'exposition automatique, la balance des blancs et les contrôles manuels comme l'ISO et le temps d'exposition. CaptureResult vous indique quels paramètres ont été réellement utilisés par la caméra.

Dans la Partie IV, nous plongerons profondément dans les contrôles manuels de la caméra : ISO, exposition, mise au point et balance des blancs.
