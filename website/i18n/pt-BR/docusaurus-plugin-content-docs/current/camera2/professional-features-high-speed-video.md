---
sidebar_position: 14
title: "Capítulo 14: Recursos Profissionais - Vídeo em Alta Velocidade"
description: Aprenda a capturar vídeo em alta velocidade e trabalhar com configurações de múltiplas câmeras no Camera2.
keywords: [vídeo em alta velocidade, múltiplas câmeras, Camera2, constrained high speed, logical camera]
---

Recursos profissionais abrem novas possibilidades criativas. Vamos explorar vídeo em alta velocidade e múltiplas câmeras.

## Introdução

O Camera2 suporta muitos recursos profissionais além da captura básica de fotos. Neste capítulo, aprenderemos sobre:

1. **Vídeo em alta velocidade** — Capturando vídeo em câmera lenta
2. **Múltiplas câmeras** — Trabalhando com câmeras lógicas e físicas

## Vídeo em Alta Velocidade

O vídeo em alta velocidade permite capturar vídeo em taxas de quadros maiores que os 30fps padrão:
- 120fps — Câmera lenta suave
- 240fps — Câmera lenta padrão
- 480fps — Câmera lenta extrema
- 960fps — Super câmera lenta

### Requisitos

Para capturar vídeo em alta velocidade, sua câmera deve:
1. Suportar a capacidade `CONSTRAINED_HIGH_SPEED_VIDEO`
2. Ter o nível de hardware adequado

Verifique o CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### Tamanhos de Vídeo em Alta Velocidade

O vídeo em alta velocidade usa resoluções diferentes do vídeo padrão:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### Capturando Vídeo em Alta Velocidade

O vídeo em alta velocidade requer uma sessão de captura especial:

```kotlin
// Cria uma sessão de captura em alta velocidade
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "Sessão em alta velocidade falhou", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### Iniciando a Pré-visualização em Alta Velocidade

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // Obtém os intervalos de fps suportados em alta velocidade
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // Encontra um intervalo de alta velocidade (ex: 120fps)
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

## Múltiplas Câmeras

Os celulares modernos têm várias câmeras. O Camera2 as trata como:
- **Câmeras físicas** — Sensores de câmera individuais
- **Câmeras lógicas** — Combinações de câmeras físicas

### Câmeras Lógicas vs Físicas

| Tipo | Descrição |
| --- | --- |
| **Física** | Sensor de câmera único (grande angular, telefoto, ultra grande angular) |
| **Lógica** | Câmera virtual que combina várias câmeras físicas |

### Identificando Tipos de Câmera

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Verifica se é uma câmera lógica
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// Obtém os IDs das câmeras físicas (para câmeras lógicas)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### Alternando Entre Câmeras

Para alternar câmeras, você precisa:
1. Fechar a câmera atual
2. Abrir a nova câmera
3. Criar uma nova sessão de captura

```kotlin
private fun switchCamera(newCameraId: String) {
    // Fecha a câmera atual
    captureSession?.close()
    cameraDevice?.close()
    
    // Abre a nova câmera
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### Câmera Concorrente

Alguns dispositivos suportam abrir várias câmeras simultaneamente:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## Extensão de Câmera

A extensão de câmera permite usar recursos de câmera específicos do fabricante:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// Obtém as extensões disponíveis
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

Extensões comuns:
- `EXTENSION_BOKEH` — Modo retrato
- `EXTENSION_HDR` — Modo HDR
- `EXTENSION_NIGHT` — Modo noturno
- `EXTENSION_AUTO` — Modo automático

## Um Exemplo de Múltiplas Câmeras

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
                CameraCharacteristics.LENS_FACING_FRONT -> "Câmera Frontal ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "Câmera Traseira ($id)"
                else -> "Câmera $id"
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
            Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@MultiCameraActivity, "Sessão falhou", Toast.LENGTH_SHORT).show()
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

## Melhores Práticas

1. **Verifique as capacidades** — Sempre verifique antes de usar recursos profissionais
2. **Lide com as transições** — Feche/abra suavemente ao alternar câmeras
3. **Gerencie os recursos** — O vídeo em alta velocidade consome mais recursos
4. **Forneça alternativas com elegância** — Ofereça alternativas quando os recursos não estiverem disponíveis

## Próximo Capítulo

No capítulo final, exploraremos a Enciclopédia CameraCharacteristics — um mergulho profundo nos parâmetros de câmera mais importantes.

## Resumo

Os recursos profissionais expandem suas possibilidades criativas:

1. **Vídeo em alta velocidade** — Capture câmera lenta a 120-960fps
2. **Múltiplas câmeras** — Trabalhe com câmeras lógicas e físicas
3. **Extensão de câmera** — Use recursos específicos do fabricante
4. **Câmera concorrente** — Abra várias câmeras simultaneamente

No próximo capítulo, mergulharemos fundo no CameraCharacteristics — a enciclopédia dos parâmetros de câmera.
