---
sidebar_position: 12
title: "Chapitre 12 : Caméra manuelle - ISO et exposition"
description: Apprenez à contrôler manuellement l'ISO et le temps d'exposition pour la photographie professionnelle avec Camera2.
keywords: [ISO, temps d'exposition, caméra manuelle, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

Le contrôle manuel est là où le véritable pouvoir de Camera2 se révèle. Découvrons l'ISO et l'exposition.

## Introduction

Jusqu'à présent, nous avons utilisé les contrôles automatiques. Nous allons maintenant prendre le contrôle total sur :

1. **ISO** — Sensibilité du capteur à la lumière
2. **Temps d'exposition** — Durée pendant laquelle le capteur collecte la lumière

Ces deux paramètres affectent directement la luminosité et la qualité de l'image.

## Qu'est-ce que l'ISO ?

L'ISO mesure la sensibilité du capteur à la lumière. Un ISO faible signifie :
- Moins sensible à la lumière
- Moins de bruit
- Meilleure qualité d'image

Un ISO élevé signifie :
- Plus sensible à la lumière
- Plus de bruit (grain)
- Qualité d'image inférieure

Valeurs ISO courantes : 100, 200, 400, 800, 1600, 3200, 6400

## Qu'est-ce que le temps d'exposition ?

Le temps d'exposition (également appelé vitesse d'obturation) correspond à la durée pendant laquelle le capteur collecte la lumière. Une exposition courte signifie :
- Moins de lumière capturée
- Gèle le mouvement
- Action plus rapide

Une exposition longue signifie :
- Plus de lumière capturée
- Flou de mouvement
- Meilleure performance en basse lumière

Le temps d'exposition est mesuré en secondes ou en fractions de seconde :
- 1/1000s — Action rapide
- 1/125s — Normal
- 1/30s — Lent
- 1s — Longue exposition

## Prérequis pour le contrôle manuel

Pour utiliser les contrôles manuels, votre caméra doit disposer de :
1. Un niveau matériel **FULL** ou **LEVEL_3**
2. La fonctionnalité **MANUAL_SENSOR**

Vérifiez CameraCharacteristics :

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## Récupération des plages prises en charge

Avant de définir des valeurs manuelles, vérifiez ce que la caméra prend en charge :

```kotlin
// Récupérer la plage ISO
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// Récupérer la plage d'exposition
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## Définition de l'ISO et de l'exposition manuelles

Pour utiliser les contrôles manuels, vous devez :
1. Désactiver l'exposition automatique (AE)
2. Définir l'ISO manuel
3. Définir le temps d'exposition manuel

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Désactiver l'AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// Définir l'ISO manuel
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// Définir le temps d'exposition manuel (en nanosecondes)
// 1/125s = 8 000 000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// Ajouter la cible
captureRequestBuilder.addTarget(surface)

// Démarrer l'aperçu avec les contrôles manuels
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## Le triangle d'exposition

L'ISO, le temps d'exposition et l'ouverture forment le « triangle d'exposition » :

- **ISO** — Sensibilité à la lumière
- **Temps d'exposition** — Durée de capture de la lumière
- **Ouverture** — Quantité de lumière entrante (rarement réglable sur les téléphones)

Modifier un paramètre affecte les autres. Par exemple :
- Si vous augmentez l'ISO, vous pouvez utiliser une vitesse d'obturation plus rapide
- Si vous diminuez le temps d'exposition, vous devrez peut-être augmenter l'ISO

## Un exemple complet de contrôle manuel

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO : 100-3200 par pas de 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // Exposition : 1/1000s à 1/10s
        exposureSeekBar.progress = 5
        
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Convertir la progression en temps d'exposition (en nanosecondes)
                // 0 : 1/1000s = 1 000 000ns
                // 9 : 1/10s = 100 000 000ns
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
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
            
            // Désactiver l'AE pour le contrôle manuel
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // Définir l'ISO manuel
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // Définir le temps d'exposition manuel
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Garder l'AWB activé
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (reste du code de configuration de la caméra)
    
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
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
    
    // ... (ouverture de la caméra, création de session, etc.)
}
```

## La mise en page

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="ISO:"/>
            
            <SeekBar
                android:id="@+id/isoSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/isoValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="100"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="16dp">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="Exp:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60s"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## Bonnes pratiques

1. **Vérifiez la prise en charge** — Vérifiez toujours la prise en charge du capteur manuel
2. **Commencez avec l'AE** — Laissez l'exposition automatique définir les valeurs initiales, puis passez en manuel
3. **Évitez les ISO extrêmes** — Un ISO élevé introduit du bruit
4. **Utilisez l'exposition la plus courte** — Évitez le flou de mouvement lorsque c'est possible
5. **Surveillez l'histogramme** — Utilisez CaptureResult pour vérifier l'exposition

## Chapitre suivant

Dans le prochain chapitre, nous aborderons le contrôle manuel de la mise au point et de la balance des blancs.

## Résumé

Le contrôle manuel de l'ISO et de l'exposition vous offre une photographie de niveau professionnel :

1. **ISO** — Contrôle la sensibilité du capteur (plage 100-3200+)
2. **Temps d'exposition** — Contrôle la durée de collecte de la lumière (en nanosecondes)
3. **Désactiver l'AE** — Vous devez désactiver l'exposition automatique pour le contrôle manuel
4. **Vérifiez les plages** — Vérifiez toujours les plages ISO et d'exposition prises en charge

Le triangle d'exposition (ISO, temps d'exposition, ouverture) détermine la luminosité et la qualité de l'image. Dans le prochain chapitre, nous explorerons la mise au point manuelle et la balance des blancs.
