---
sidebar_position: 12
title: "Capítulo 12: Câmera Manual - ISO e Exposição"
description: Aprenda a controlar manualmente o ISO e o tempo de exposição para fotografia profissional com Camera2.
keywords: [ISO, tempo de exposição, câmera manual, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

O controle manual é onde o verdadeiro poder do Camera2 brilha. Vamos aprender sobre ISO e exposição.

## Introdução

Até agora, temos usado controles automáticos. Agora vamos ter controle total sobre:

1. **ISO** — Sensibilidade do sensor à luz
2. **Tempo de exposição** — Quanto tempo o sensor coleta luz

Essas duas configurações afetam diretamente o brilho e a qualidade da imagem.

## O que é ISO?

O ISO mede a sensibilidade do sensor à luz. ISO baixo significa:
- Menos sensível à luz
- Menor ruído
- Melhor qualidade de imagem

ISO alto significa:
- Mais sensível à luz
- Maior ruído (grãos)
- Menor qualidade de imagem

Valores comuns de ISO: 100, 200, 400, 800, 1600, 3200, 6400

## O que é Tempo de Exposição?

O tempo de exposição (também chamado de velocidade do obturador) é quanto tempo o sensor coleta luz. Exposição mais curta significa:
- Menos luz capturada
- Congela o movimento
- Ação mais rápida

Exposição mais longa significa:
- Mais luz capturada
- Borrão de movimento
- Melhor desempenho em pouca luz

O tempo de exposição é medido em segundos ou frações de segundo:
- 1/1000s — Ação rápida
- 1/125s — Normal
- 1/30s — Lento
- 1s — Exposição longa

## Requisitos para Controle Manual

Para usar controles manuais, sua câmera deve ter:
1. Nível de hardware **FULL** ou **LEVEL_3**
2. Capacidade **MANUAL_SENSOR**

Verifique CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## Obtendo Intervalos Suportados

Antes de definir valores manuais, verifique o que a câmera suporta:

```kotlin
// Obtém o intervalo de ISO
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// Obtém o intervalo de exposição
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## Definindo ISO e Exposição Manuais

Para usar controles manuais, você precisa:
1. Desativar a exposição automática (AE)
2. Definir o ISO manual
3. Definir o tempo de exposição manual

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Desativa o AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// Define o ISO manual
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// Define o tempo de exposição manual (em nanossegundos)
// 1/125s = 8.000.000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// Adiciona o alvo
captureRequestBuilder.addTarget(surface)

// Inicia a pré-visualização com controles manuais
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## O Triângulo da Exposição

ISO, tempo de exposição e abertura formam o "triângulo da exposição":

- **ISO** — Sensibilidade à luz
- **Tempo de exposição** — Duração da captura de luz
- **Abertura** — Quantidade de luz que entra (raramente ajustável em celulares)

Mudar um afeta os outros. Por exemplo:
- Se você aumentar o ISO, pode usar uma velocidade do obturador mais rápida
- Se você diminuir o tempo de exposição, pode precisar aumentar o ISO

## Um Exemplo Completo de Controle Manual

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
        
        isoSeekBar.max = 31 // ISO: 100-3200 em passos de 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // Exposição: 1/1000s a 1/10s
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
                // Converte o progresso para tempo de exposição (em nanossegundos)
                // 0: 1/1000s = 1.000.000ns
                // 9: 1/10s = 100.000.000ns
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
            
            // Desativa o AE para controle manual
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // Define o ISO manual
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // Define o tempo de exposição manual
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Mantém o AWB ativado
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (resto do código de configuração da câmera)
    
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
    
    // ... (abertura da câmera, criação da sessão, etc.)
}
```

## O Layout

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

## Melhores Práticas

1. **Verifique o suporte** — Sempre verifique o suporte a sensor manual
2. **Comece com o AE** — Deixe a exposição automática definir os valores iniciais, depois mude para manual
3. **Evite ISO extremo** — ISO alto introduz ruído
4. **Use a exposição mais curta** — Evite borrão de movimento quando possível
5. **Monitore o histograma** — Use CaptureResult para verificar a exposição

## Próximo Capítulo

No próximo capítulo, aprenderemos sobre foco manual e controle de balanço de branco.

## Resumo

O controle manual de ISO e exposição oferece fotografia de nível profissional:

1. **ISO** — Controla a sensibilidade do sensor (intervalo de 100-3200+)
2. **Tempo de exposição** — Controla por quanto tempo a luz é coletada (em nanossegundos)
3. **Desative o AE** — É necessário desligar a exposição automática para controle manual
4. **Verifique os intervalos** — Sempre verifique os intervalos suportados de ISO e exposição

O triângulo da exposição (ISO, tempo de exposição, abertura) determina o brilho e a qualidade da imagem. No próximo capítulo, exploraremos foco manual e balanço de branco.
