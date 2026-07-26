---
sidebar_position: 14
title: "Chapitre 14 : Fonctionnalités Professionnelles - Vidéo Haute Vitesse"
description: Apprenez à capturer de la vidéo haute vitesse et à travailler avec des configurations multi-caméras dans Camera2.
keywords: [vidéo haute vitesse, multi-caméra, Camera2, constrained high speed, logical camera]
---

Les fonctionnalités professionnelles ouvrent de nouvelles possibilités créatives. Explorons la vidéo haute vitesse et la multi-caméra.

## Introduction

Camera2 prend en charge de nombreuses fonctionnalités professionnelles au-delà de la capture photo de base. Dans ce chapitre, nous apprendrons :

1. **Vidéo haute vitesse** — Capturer de la vidéo au ralenti
2. **Multi-caméra** — Travailler avec des caméras logiques et physiques

## Vidéo Haute Vitesse

La vidéo haute vitesse vous permet de capturer de la vidéo à des fréquences d'images supérieures aux 30 fps standard :
- 120 fps — Ralenti fluide
- 240 fps — Ralenti standard
- 480 fps — Ralenti extrême
- 960 fps — Super ralenti

### Prérequis

Pour capturer de la vidéo haute vitesse, votre caméra doit :
1. Prendre en charge la fonctionnalité `CONSTRAINED_HIGH_SPEED_VIDEO`
2. Posséder le bon niveau matériel

Vérifiez CameraCharacteristics :

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### Tailles de Vidéo Haute Vitesse

La vidéo haute vitesse utilise des résolutions différentes de la vidéo standard :

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### Capturer de la Vidéo Haute Vitesse

La vidéo haute vitesse nécessite une session de capture spéciale :

```kotlin
// Créer une session de capture haute vitesse
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "Échec de la session haute vitesse", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### Démarrer l'Aperçu Haute Vitesse

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // Obtenir les plages fps haute vitesse prises en charge
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // Trouver une plage haute vitesse (par ex., 120 fps)
    val highSpeedFpsRange = fpsRanges?.find { it.upper >= 120 }
    
    captureRequestBuilder?.apply {
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange)
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.setRepeatingRequest(builder.build(), null, null)
    }
}
```

## Multi-Caméra

Les téléphones modernes disposent de plusieurs caméras. Camera2 les traite comme :
- **Caméras physiques** — Capteurs de caméra individuels
- **Caméras logiques** — Combinaisons de caméras physiques

### Caméras Logiques vs Physiques

| Type | Description |
| --- | --- |
| **Physique** | Capteur de caméra unique (grand angle, téléobjectif, ultra-grand angle) |
| **Logique** | Caméra virtuelle qui combine plusieurs caméras physiques |

### Identifier les Types de Caméras

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Vérifier s'il s'agit d'une caméra logique
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// Obtenir les ID des caméras physiques (pour les caméras logiques)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### Basculer Entre les Caméras

Pour basculer entre les caméras, vous devez :
1. Fermer la caméra actuelle
2. Ouvrir la nouvelle caméra
3. Créer une nouvelle session de capture

```kotlin
private fun switchCamera(newCameraId: String) {
    // Fermer la caméra actuelle
    captureSession?.close()
    cameraDevice?.close()
    
    // Ouvrir la nouvelle caméra
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### Caméra Simultanée

Certains appareils prennent en charge l'ouverture de plusieurs caméras simultanément :

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## Extension de Caméra

L'extension de caméra vous permet d'utiliser des fonctionnalités de caméra spécifiques au fabricant :

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// Obtenir les extensions disponibles
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

Extensions courantes :
- `EXTENSION_BOKEH` — Mode portrait
- `EXTENSION_HDR` — Mode HDR
- `EXTENSION_NIGHT` — Mode nuit
- `EXTENSION_AUTO` — Mode automatique

## Un Exemple de Multi-Caméra

```kotlin
class MultiCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_camera)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupCameraSelector()
    }

    private fun setupCameraSelector() {
        val cameraIds = cameraManager.cameraIdList
        val spinner = findViewById<Spinner>(R.id.cameraSpinner)
        
        val cameraNames = cameraIds.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Caméra frontale ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "Caméra arrière ($id)"
                else -> "Caméra $id"
            }
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newCameraId = cameraIds[position]
                if (newCameraId != currentCameraId) {
                    switchCamera(newCameraId)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun switchCamera(newCameraId: String) {
        captureSession?.close()
        cameraDevice?.close()
        currentCameraId = newCameraId
        openCamera()
    }

    private fun openCamera() {
        try {
            cameraManager.openCamera(currentCameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Autorisation de caméra refusée", Toast.LENGTH_SHORT).show()
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
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "Échec de la session", Toast.LENGTH_SHORT).show()
            }
        }, null)
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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }
}
```

## Bonnes Pratiques

1. **Vérifiez les fonctionnalités** — Vérifiez toujours avant d'utiliser les fonctionnalités professionnelles
2. **Gérez les transitions** — Fermez/ouvrez en douceur lors du basculement entre caméras
3. **Gérez les ressources** — La vidéo haute vitesse consomme plus de ressources
4. **Dégradez gracieusement** — Fournissez des alternatives lorsque les fonctionnalités ne sont pas disponibles

## Chapitre Suivant

Dans le dernier chapitre, nous explorerons l'Encyclopédie CameraCharacteristics — une plongée profonde dans les paramètres de caméra les plus importants.

## Résumé

Les fonctionnalités professionnelles élargissent vos possibilités créatives :

1. **Vidéo haute vitesse** — Capturer du ralenti à 120-960 fps
2. **Multi-caméra** — Travailler avec des caméras logiques et physiques
3. **Extension de caméra** — Utiliser des fonctionnalités spécifiques au fabricant
4. **Caméra simultanée** — Ouvrir plusieurs caméras simultanément

Dans le prochain chapitre, nous plongerons profondément dans CameraCharacteristics — l'encyclopédie des paramètres de caméra.
