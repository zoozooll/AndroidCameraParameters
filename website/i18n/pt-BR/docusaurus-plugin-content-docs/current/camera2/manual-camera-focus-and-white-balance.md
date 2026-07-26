---
sidebar_position: 13
title: "Capítulo 13: Câmera Manual - Foco e Balanço de Branco"
description: Aprenda a controlar manualmente a distância focal e o balanço de branco para fotografia profissional com Camera2.
keywords: [foco, balanço de branco, câmera manual, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

Controle manual completo com foco e balanço de branco.

## Introdução

No capítulo anterior, você aprendeu a controlar ISO e exposição. Agora vamos adicionar:

1. **Foco** — Controle manual da distância focal
2. **Balanço de branco** — Controle manual da temperatura de cor

Com esses, você tem controle criativo completo sobre suas fotos.

## Foco Manual

O foco determina qual parte da cena está nítida. O foco manual permite que você:
- Foque em objetos específicos
- Crie desfoque intencional (bokeh)
- Garanta foco crítico na fotografia macro

### Modos de Foco

O Camera2 suporta vários modos de foco:

| Modo | Descrição |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Foco manual |
| `CONTROL_AF_MODE_AUTO` | Foco automático único |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Foco automático contínuo para fotos |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Foco automático contínuo para vídeo |
| `CONTROL_AF_MODE_MACRO` | Foco macro |

### Distância Focal

A distância focal é medida em dioptrias (1/metro). Um valor de 0 significa infinito.

```kotlin
// Obter faixa de distância focal
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### Configurando o Foco Manual

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Desativar AF para foco manual
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// Definir distância focal manual (em dioptrias)
// 0.0 = infinito
// 1.0 = 1 metro
// 2.0 = 0,5 metros
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Regiões de Foco

Você também pode especificar regiões de AF para foco automático seletivo:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // Centro X nas coordenadas do sensor (0-1000)
        centerY,    // Centro Y nas coordenadas do sensor (0-1000)
        width,      // Largura da região
        height,     // Altura da região
        weight      // Prioridade (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## Balanço de Branco Manual

O balanço de branco (WB) ajusta a temperatura de cor da imagem. Diferentes fontes de luz têm diferentes temperaturas de cor:

- **Luz do dia** — ~5500K (azulada)
- **Nublado** — ~6500K (mais fria)
- **Tungstênio** — ~2800K (quente/amarela)
- **Fluorescente** — ~4000K (esverdeada)

### Modos de Balanço de Branco

| Modo | Descrição |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Balanço de branco manual |
| `CONTROL_AWB_MODE_AUTO` | Balanço de branco automático |
| `CONTROL_AWB_MODE_INCANDESCENT` | Iluminação de tungstênio |
| `CONTROL_AWB_MODE_FLUORESCENT` | Iluminação fluorescente |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluorescente quente |
| `CONTROL_AWB_MODE_DAYLIGHT` | Luz do dia |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Nublado |

### Configurando o Balanço de Branco Manual

Para definir o balanço de branco manual, você precisa definir os ganhos de correção de cor:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Desativar AWB para controle manual
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// Definir ganhos de correção de cor (R, G, B)
// Os valores são normalizados (1.0 = sem correção)
// Valores maiores tornam essa cor mais proeminente
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Temperatura de Cor

Você também pode definir o balanço de branco usando a temperatura de cor:

```kotlin
// Obter faixa de temperatura de cor suportada
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// Definir temperatura de cor (em Kelvin)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // Luz do dia
```

## Um Exemplo Completo de Câmera Manual

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // Controles manuais
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // Infinito
    private var currentWhiteBalance = 5500 // Luz do dia

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // Controle de ISO
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Controle de exposição
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Controle de foco
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Converter progresso (0-100) para distância focal (0.0 a 2.0 dioptrias)
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Controle de balanço de branco
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (tungstênio) a 6500K (nublado)
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
            
            // Exposição manual
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Foco manual
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // Balanço de branco manual
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (código de configuração da câmera)
}
```

## Melhores Práticas

1. **Verifique o suporte** — Nem todas as câmeras suportam foco manual ou WB
2. **Comece no automático** — Deixe os controles automáticos estabelecerem uma linha de base
3. **Use foco por picos** — Adicione feedback visual para precisão do foco
4. **Calibre o WB** — Use um cartão cinza para balanço de branco preciso
5. **Combine os controles** — As configurações manuais funcionam melhor juntas

## Próximo Capítulo

No próximo capítulo, exploraremos recursos profissionais como vídeo de alta velocidade e câmeras múltiplas.

## Resumo

O controle manual de foco e balanço de branco completa seu kit de ferramentas de câmera:

1. **Foco** — Controle o que está nítido na imagem
2. **Balanço de branco** — Controle a temperatura de cor
3. **Modos manuais** — Desative AF/AWB e defina valores diretamente
4. **Regiões de foco** — Direcione áreas específicas para foco automático

Com ISO, exposição, foco e balanço de branco sob seu controle, você pode criar fotos de qualidade profissional. Na Parte V, exploraremos recursos avançados como vídeo de alta velocidade e suporte a múltiplas câmeras.
