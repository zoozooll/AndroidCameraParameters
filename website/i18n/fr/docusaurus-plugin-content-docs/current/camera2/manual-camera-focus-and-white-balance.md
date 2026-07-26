---
sidebar_position: 13
title: "Chapitre 13 : Caméra manuelle - Mise au point et balance des blancs"
description: Apprenez à contrôler manuellement la distance de mise au point et la balance des blancs pour la photographie professionnelle avec Camera2.
keywords: [mise au point, balance des blancs, caméra manuelle, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

Contrôle manuel complet avec la mise au point et la balance des blancs.

## Introduction

Dans le chapitre précédent, vous avez appris à contrôler l'ISO et l'exposition. Maintenant, nous allons ajouter :

1. **Mise au point** — Contrôle manuel de la distance de mise au point
2. **Balance des blancs** — Contrôle manuel de la température de couleur

Avec ces outils, vous disposez d'un contrôle créatif complet sur vos photos.

## Mise au point manuelle

La mise au point détermine quelle partie de la scène est nette. La mise au point manuelle vous permet de :
- Mettre au point sur des objets spécifiques
- Créer un flou intentionnel (bokeh)
- Assurer une mise au point précise en photographie macro

### Modes de mise au point

Camera2 prend en charge plusieurs modes de mise au point :

| Mode | Description |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Mise au point manuelle |
| `CONTROL_AF_MODE_AUTO` | Mise au point automatique unique |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Mise au point automatique continue pour les photos |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Mise au point automatique continue pour la vidéo |
| `CONTROL_AF_MODE_MACRO` | Mise au point macro |

### Distance de mise au point

La distance de mise au point est mesurée en dioptries (1/mètre). Une valeur de 0 signifie l'infini.

```kotlin
// Obtenir la plage de distance de mise au point
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### Définir la mise au point manuelle

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Désactiver l'AF pour la mise au point manuelle
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// Définir la distance de mise au point manuelle (en dioptries)
// 0.0 = infini
// 1.0 = 1 mètre
// 2.0 = 0.5 mètre
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Zones de mise au point

Vous pouvez également spécifier des zones AF pour la mise au point automatique sélective :

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // Centre X en coordonnées de capteur (0-1000)
        centerY,    // Centre Y en coordonnées de capteur (0-1000)
        width,      // Largeur de la zone
        height,     // Hauteur de la zone
        weight      // Priorité (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## Balance des blancs manuelle

La balance des blancs (WB) ajuste la température de couleur de l'image. Différentes sources de lumière ont des températures de couleur différentes :

- **Lumière du jour** — ~5500K (bleutée)
- **Couvert** — ~6500K (plus froide)
- **Tungstène** — ~2800K (chaud/jaune)
- **Fluorescente** — ~4000K (verdâtre)

### Modes de balance des blancs

| Mode | Description |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Balance des blancs manuelle |
| `CONTROL_AWB_MODE_AUTO` | Balance des blancs automatique |
| `CONTROL_AWB_MODE_INCANDESCENT` | Éclairage tungstène |
| `CONTROL_AWB_MODE_FLUORESCENT` | Éclairage fluorescent |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluorescent chaud |
| `CONTROL_AWB_MODE_DAYLIGHT` | Lumière du jour |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Temps couvert |

### Définir la balance des blancs manuelle

Pour définir la balance des blancs manuelle, vous devez définir les gains de correction des couleurs :

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Désactiver l'AWB pour le contrôle manuel
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// Définir les gains de correction des couleurs (R, G, B)
// Les valeurs sont normalisées (1.0 = pas de correction)
// Des valeurs plus élevées rendent cette couleur plus proéminente
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Température de couleur

Vous pouvez également définir la balance des blancs en utilisant la température de couleur :

```kotlin
// Obtenir la plage de températures de couleur prises en charge
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// Définir la température de couleur (en Kelvin)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // Lumière du jour
```

## Un exemple complet de caméra manuelle

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // Contrôles manuels
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // Infini
    private var currentWhiteBalance = 5500 // Lumière du jour

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // Contrôle ISO
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Contrôle d'exposition
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Contrôle de mise au point
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Convertir la progression (0-100) en distance de mise au point (0.0 à 2.0 dioptries)
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Contrôle de balance des blancs
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (tungstène) à 6500K (couvert)
                currentWhiteBalance = 2800 + (progress * 37)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // Exposition manuelle
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Mise au point manuelle
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // Balance des blancs manuelle
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (code de configuration de la caméra)
}
```

## Bonnes pratiques

1. **Vérifier la prise en charge** — Toutes les caméras ne prennent pas en charge la mise au point manuelle ou la WB
2. **Commencer en automatique** — Laisser les contrôles automatiques établir une base de référence
3. **Utiliser le focus peaking** — Ajouter un retour visuel pour la précision de la mise au point
4. **Étalonner la WB** — Utiliser une carte grise pour une balance des blancs précise
5. **Combiner les contrôles** — Les réglages manuels fonctionnent mieux ensemble

## Chapitre suivant

Dans le prochain chapitre, nous explorerons des fonctionnalités professionnelles comme la vidéo haute vitesse et la multi-caméra.

## Résumé

Le contrôle manuel de la mise au point et de la balance des blancs complète votre boîte à outils de caméra :

1. **Mise au point** — Contrôler ce qui est net dans l'image
2. **Balance des blancs** — Contrôler la température de couleur
3. **Modes manuels** — Désactiver AF/AWB et définir les valeurs directement
4. **Zones de mise au point** — Cibler des zones spécifiques pour la mise au point automatique

Avec l'ISO, l'exposition, la mise au point et la balance des blancs sous votre contrôle, vous pouvez créer des photos de qualité professionnelle. Dans la partie V, nous explorerons des fonctionnalités avancées comme la vidéo haute vitesse et la prise en charge multi-caméra.
