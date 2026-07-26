---
sidebar_position: 15
title: "Chapitre 15 : Encyclopédie CameraCharacteristics"
description: Un guide complet des caractéristiques Camera2 les plus importantes, incluant ce qu'elles signifient, pourquoi elles existent et comment les utiliser.
keywords: [CameraCharacteristics, paramètres de caméra, capacités de caméra, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

Bienvenue dans l'Encyclopédie CameraCharacteristics — votre guide pour comprendre chaque paramètre de caméra.

## Introduction

CameraCharacteristics contient des centaines de paramètres qui décrivent les capacités d'une caméra. Dans ce chapitre, nous explorerons les plus importantes en profondeur :

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — Que peut faire la caméra ?
2. `REQUEST_AVAILABLE_CAPABILITIES` — Quelles fonctionnalités sont disponibles ?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — Quelle est la taille du capteur ?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — Combien de zoom ?
5. `CONTROL_AE_AVAILABLE_MODES` — Quels modes d'exposition ?

Et bien d'autres...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**Que signifie-t-elle ?**  
C'est la caractéristique la plus importante. Elle définit le niveau de capacité global de l'appareil photo.

**Pourquoi existe-t-elle ?**  
Différents appareils Android ont différentes capacités de caméra. Ce paramètre aide les applications à comprendre ce qu'elles peuvent faire.

**Valeurs prises en charge :**

| Valeur | Niveau API | Description |
| --- | --- | --- |
| `LEGACY` | 21 | Anciens appareils, l'API Camera2 est une surcouche de l'ancienne API Camera |
| `LIMITED` | 21 | Fonctionnalités Camera2 de base, pas de contrôles manuels |
| `FULL` | 21 | Contrôles manuels complets, capture RAW, capture en rafale |
| `LEVEL_3` | 24 | Fonctionnalités avancées comme le retraitement YUV, le HDR 10 bits |

**Comment est-elle utilisée ?**  
Vérifiez ceci avant de tenter toute opération avancée :

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // Fonctionnalités limitées
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // Fonctionnalités de base uniquement
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // Contrôles manuels complets disponibles
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // Fonctionnalités avancées disponibles
    }
}
```

**Comment vérifier avec Android Camera Parameters :**  
Ouvrez l'application et cherchez "Hardware Level" dans la section Camera Info.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**Que signifie-t-elle ?**  
Ce tableau liste toutes les capacités prises en charge par la caméra.

**Pourquoi existe-t-elle ?**  
Même au sein du même niveau matériel, différents appareils peuvent prendre en charge différentes fonctionnalités.

**Capacités courantes :**

| Capacité | Description |
| --- | --- |
| `BACKWARD_COMPATIBLE` | Mode de compatibilité de base |
| `MANUAL_SENSOR` | Contrôle manuel de l'ISO et de l'exposition |
| `MANUAL_POST_PROCESSING` | Correction de couleur et réduction de bruit manuelles |
| `RAW` | Capture d'image RAW |
| `BURST_CAPTURE` | Capture en rafale haute vitesse |
| `YUV_REPROCESSING` | Retraitement d'image YUV |
| `DEPTH_OUTPUT` | Sortie de carte de profondeur |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | Capture vidéo haute vitesse |
| `LOGICAL_MULTI_CAMERA` | Caméra logique combinant plusieurs caméras physiques |
| `CONCURRENT_CAMERA` | Plusieurs caméras peuvent être ouvertes simultanément |
| `CAMERA_EXTENSION` | Extensions spécifiques au fabricant (portrait, mode nuit) |

**Comment est-elle utilisée ?**  
Vérifiez les capacités avant d'utiliser une fonctionnalité :

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // Activer la capture RAW
}
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Available Capabilities" dans la section Camera Info.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**Que signifie-t-elle ?**  
La matrice active est la zone réelle du capteur utilisée pour capturer des images.

**Pourquoi existe-t-elle ?**  
Le capteur peut avoir des pixels sur les bords réservés pour l'étalonnage. La matrice active représente la zone utilisable.

**Comment est-elle utilisée ?**  
Cela vous indique la résolution maximale disponible pour la capture :

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**Caractéristiques associées :**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — Total des pixels sur le capteur (peut être plus grand que la matrice active)
- `SENSOR_INFO_SENSOR_SIZE` — Dimensions physiques en millimètres

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Active Array Size" et "Sensor Size" dans la section Sensor.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**Que signifie-t-elle ?**  
Le facteur de zoom numérique maximal pris en charge par la caméra.

**Pourquoi existe-t-elle ?**  
Le zoom numérique recadre et agrandit l'image, réduisant la qualité. Connaître le maximum aide à gérer les attentes de l'utilisateur.

**Comment est-elle utilisée ?**  
Définir le niveau de zoom dans les requêtes de capture :

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// Définir le zoom (1.0 = pas de zoom, maxZoom = zoom maximum)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**Caractéristiques associées :**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — Longueurs focales physiques (pour le zoom optique)

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Max Digital Zoom" dans la section Scaler.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**Que signifie-t-elle ?**  
Modes d'exposition automatique disponibles.

**Pourquoi existe-t-elle ?**  
Différents appareils prennent en charge différentes stratégies AE.

**Modes courants :**

| Mode | Description |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | Contrôle d'exposition manuel |
| `CONTROL_AE_MODE_ON` | Exposition automatique |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | Exposition automatique avec flash toujours allumé |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | Exposition automatique avec flash automatique |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | Exposition automatique avec réduction des yeux rouges |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | Exposition automatique avec flash externe |

**Comment est-elle utilisée ?**  
Définir le mode AE dans les requêtes de capture :

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "AE Available Modes" dans la section Control.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**Que signifie-t-elle ?**  
Modes de mise au point automatique disponibles.

**Pourquoi existe-t-elle ?**  
Différentes stratégies de mise au point pour différents scénarios.

**Modes courants :**

| Mode | Description |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Mise au point manuelle |
| `CONTROL_AF_MODE_AUTO` | Mise au point automatique à prise unique |
| `CONTROL_AF_MODE_MACRO` | Mise au point macro |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Mise au point automatique continue pour la vidéo |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Mise au point automatique continue pour les photos |
| `CONTROL_AF_MODE_EDGE` | Mise au point automatique sur les bords |
| `CONTROL_AF_MODE_FIXED` | Mise au point fixe (pas d'AF) |

**Comment est-elle utilisée ?**  
Définir le mode AF en fonction de votre cas d'utilisation :

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "AF Available Modes" dans la section Control.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**Que signifie-t-elle ?**  
Modes de balance des blancs automatique disponibles.

**Modes courants :**

| Mode | Description |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Balance des blancs manuelle |
| `CONTROL_AWB_MODE_AUTO` | Automatique |
| `CONTROL_AWB_MODE_INCANDESCENT` | Éclairage tungstène |
| `CONTROL_AWB_MODE_FLUORESCENT` | Éclairage fluorescent |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluorescent chaud |
| `CONTROL_AWB_MODE_DAYLIGHT` | Lumière du jour |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Ciel nuageux |

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "AWB Available Modes" dans la section Control.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**Que signifie-t-elle ?**  
Longueurs focales disponibles pour le(s) objectif(s).

**Pourquoi existe-t-elle ?**  
Plusieurs valeurs indiquent des capacités de zoom optique.

**Comment est-elle utilisée ?**  
Déterminer quels objectifs sont disponibles :

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**Longueurs focales courantes :**
- 2.4mm — Grand-angle (courant)
- 4.8mm — Téléobjectif (zoom optique 2x)
- 1.8mm — Ultra grand-angle

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Available Focal Lengths" dans la section Lens.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**Que signifie-t-elle ?**  
La distance la plus proche à laquelle l'objectif peut faire la mise au point.

**Pourquoi existe-t-elle ?**  
Des valeurs plus basses signifient une meilleure capacité macro.

**Comment est-elle utilisée ?**  
Vérifier si la photographie macro est possible :

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// Une valeur de 0,1 m (10 cm) ou moins indique une bonne capacité macro
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Minimum Focus Distance" dans la section Lens.

---

## 10. FLASH_INFO_AVAILABLE

**Que signifie-t-elle ?**  
Si la caméra dispose d'un flash.

**Pourquoi existe-t-elle ?**  
Toutes les caméras n'ont pas de flash (en particulier les caméras frontales).

**Comment est-elle utilisée ?**  
Vérifier avant d'utiliser le flash :

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // Activer les fonctionnalités de flash
}
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Flash Available" dans la section Flash.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**Que signifie-t-elle ?**  
Le temps d'exposition minimum et maximum pris en charge.

**Pourquoi existe-t-elle ?**  
Détermine la capacité en basse lumière et la capacité de figer le mouvement.

**Comment est-elle utilisée ?**  
Vérifier la plage d'exposition pour le contrôle manuel :

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// Convertir en secondes pour l'affichage
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Exposure Time Range" dans la section Sensor.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**Que signifie-t-elle ?**  
Les valeurs ISO minimum et maximum prises en charge.

**Pourquoi existe-t-elle ?**  
Détermine la capacité en basse lumière et les performances de bruit.

**Comment est-elle utilisée ?**  
Vérifier la plage ISO pour le contrôle manuel :

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Sensitivity Range" dans la section Sensor.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**Que signifie-t-elle ?**  
Toutes les tailles et formats de sortie pris en charge.

**Pourquoi existe-t-elle ?**  
Détermine quelles résolutions et formats vous pouvez utiliser.

**Comment est-elle utilisée ?**  
Obtenir les tailles prises en charge pour différents cas d'utilisation :

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// Tailles d'aperçu
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// Tailles de photo
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// Tailles vidéo
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// Tailles RAW
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Preview Sizes", "Picture Sizes", etc. dans la section Scaler.

---

## 14. LENS_FACING

**Que signifie-t-elle ?**  
Dans quelle direction l'objectif est orienté.

**Pourquoi existe-t-elle ?**  
Détermine s'il s'agit d'une caméra frontale, arrière ou externe.

**Valeurs :**
- `LENS_FACING_FRONT` — Caméra selfie
- `LENS_FACING_BACK` — Caméra arrière
- `LENS_FACING_EXTERNAL` — Caméra externe

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Lens Facing" dans la section Camera Info.

---

## 15. CONTROL_MAX_REGIONS_AE

**Que signifie-t-elle ?**  
Nombre maximum de zones de mesure AE.

**Pourquoi existe-t-elle ?**  
Détermine la précision de la mesure d'exposition.

**Comment est-elle utilisée ?**  
Limiter le nombre de zones AE que vous créez :

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// Ne pas créer plus de maxAERegions
```

**Comment vérifier avec Android Camera Parameters :**  
Cherchez "Max Regions AE" dans la section Control.

---

## Conclusion

CameraCharacteristics est votre fenêtre sur les capacités de la caméra. En comprenant ces paramètres, vous pouvez :

1. **Construire des applications indépendantes de l'appareil** — Vérifiez les capacités avant d'utiliser les fonctionnalités
2. **Offrir de meilleures expériences utilisateur** — Affichez uniquement les fonctionnalités disponibles
3. **Optimiser les performances** — Choisissez des résolutions et des formats appropriés
4. **Créer des applications professionnelles** — Libérez tout le potentiel de la caméra

## Comment en savoir plus

1. **Application Android Camera Parameters** — Explorez les données réelles de votre appareil
2. **Documentation Android** — Lisez la documentation officielle de CameraCharacteristics
3. **Expérimentez** — Écrivez de petites applications de test pour essayer différents paramètres
4. **Code source** — Regardez le code source de Camera2 pour une compréhension plus approfondie

## Résumé

Ce chapitre a couvert les CameraCharacteristics les plus importantes :

1. **Hardware Level** — Capacité globale
2. **Capabilities** — Fonctionnalités spécifiques disponibles
3. **Active Array** — Résolution du capteur
4. **Digital Zoom** — Capacités de zoom
5. **Modes AE/AF/AWB** — Modes de contrôle automatique
6. **Focal Lengths** — Capacités de l'objectif
7. **Focus Distance** — Capacité macro
8. **Flash** — Disponibilité du flash
9. **Plage Exposure/ISO** — Limites de contrôle manuel
10. **Stream Configuration** — Tailles et formats pris en charge

Avec ces connaissances, vous êtes prêt à construire des applications Camera2 avancées !

---

## Mots finaux

Félicitations ! Vous avez terminé cette série Android Camera2. Vous comprenez maintenant :

- **Comment fonctionnent les caméras de smartphone** — Objectifs, capteurs, ISP
- **Comment fonctionne Camera2** — CameraManager, CameraDevice, CaptureSession
- **Comment capturer des photos** — JPEG, RAW, ImageReader
- **Comment contrôler la caméra** — ISO, exposition, mise au point, balance des blancs
- **Fonctionnalités professionnelles** — Vidéo haute vitesse, multi-caméra
- **Caractéristiques de la caméra** — L'encyclopédie des capacités de la caméra

L'application Android Camera Parameters est un excellent outil pour continuer à apprendre. Explorez les capacités de votre appareil et expérimentez avec différents paramètres.

Bon codage ! 📸
