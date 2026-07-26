---
sidebar_position: 15
title: "Capítulo 15: Enciclopédia CameraCharacteristics"
description: Um guia completo para as características mais importantes do Camera2, incluindo o que elas significam, por que existem e como usá-las.
keywords: [CameraCharacteristics, parâmetros da câmera, capacidades da câmera, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

Bem-vindo à Enciclopédia CameraCharacteristics — seu guia para entender cada parâmetro da câmera.

## Introdução

CameraCharacteristics contém centenas de parâmetros que descrevem as capacidades de uma câmera. Neste capítulo, exploraremos os mais importantes em profundidade:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — O que a câmera pode fazer?
2. `REQUEST_AVAILABLE_CAPABILITIES` — Quais recursos estão disponíveis?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — Qual é o tamanho do sensor?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — Quanto zoom?
5. `CONTROL_AE_AVAILABLE_MODES` — Quais modos de exposição?

E muitos mais...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**O que significa?**  
Esta é a característica mais importante. Ela define o nível geral de capacidade do dispositivo de câmera.

**Por que existe?**  
Diferentes dispositivos Android têm diferentes capacidades de câmera. Este parâmetro ajuda os aplicativos a entender o que podem fazer.

**Valores suportados:**

| Valor | Nível da API | Descrição |
| --- | --- | --- |
| `LEGACY` | 21 | Dispositivos antigos, a API Camera2 é um wrapper sobre a antiga API Camera |
| `LIMITED` | 21 | Recursos básicos do Camera2, sem controles manuais |
| `FULL` | 21 | Controles manuais completos, captura RAW, captura em rajada |
| `LEVEL_3` | 24 | Recursos avançados como reprocessamento YUV, HDR de 10 bits |

**Como é usado?**  
Verifique isso antes de tentar qualquer operação avançada:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // Funcionalidade limitada
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // Apenas recursos básicos
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // Controles manuais completos disponíveis
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // Recursos avançados disponíveis
    }
}
```

**Como verificar com o Android Camera Parameters:**  
Abra o aplicativo e procure por "Hardware Level" na seção Camera Info.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**O que significa?**  
Este array lista todas as capacidades suportadas pela câmera.

**Por que existe?**  
Mesmo dentro do mesmo nível de hardware, diferentes dispositivos podem suportar recursos diferentes.

**Capacidades comuns:**

| Capacidade | Descrição |
| --- | --- |
| `BACKWARD_COMPATIBLE` | Modo de compatibilidade básica |
| `MANUAL_SENSOR` | Controle manual de ISO e exposição |
| `MANUAL_POST_PROCESSING` | Correção de cor e redução de ruído manuais |
| `RAW` | Captura de imagem RAW |
| `BURST_CAPTURE` | Captura em rajada de alta velocidade |
| `YUV_REPROCESSING` | Reprocessamento de imagem YUV |
| `DEPTH_OUTPUT` | Saída de mapa de profundidade |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | Captura de vídeo em alta velocidade |
| `LOGICAL_MULTI_CAMERA` | Câmera lógica combinando múltiplas câmeras físicas |
| `CONCURRENT_CAMERA` | Múltiplas câmeras podem ser abertas simultaneamente |
| `CAMERA_EXTENSION` | Extensões específicas do fabricante (retrato, modo noturno) |

**Como é usado?**  
Verifique as capacidades antes de usar um recurso:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // Habilitar captura RAW
}
```

**Como verificar com o Android Camera Parameters:**  
Procure por "Available Capabilities" na seção Camera Info.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**O que significa?**  
O array ativo é a área real do sensor usada para capturar imagens.

**Por que existe?**  
O sensor pode ter pixels ao redor das bordas que são reservados para calibração. O array ativo representa a área utilizável.

**Como é usado?**  
Isso informa a resolução máxima disponível para captura:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**Características relacionadas:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — Total de pixels no sensor (pode ser maior que o array ativo)
- `SENSOR_INFO_SENSOR_SIZE` — Dimensões físicas em milímetros

**Como verificar com o Android Camera Parameters:**  
Procure por "Active Array Size" e "Sensor Size" na seção Sensor.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**O que significa?**  
O fator máximo de zoom digital suportado pela câmera.

**Por que existe?**  
O zoom digital recorta e amplia a imagem, reduzindo a qualidade. Saber o máximo ajuda a gerenciar as expectativas do usuário.

**Como é usado?**  
Defina o nível de zoom nas solicitações de captura:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// Definir zoom (1.0 = sem zoom, maxZoom = zoom máximo)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**Características relacionadas:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — Distâncias focais físicas (para zoom óptico)

**Como verificar com o Android Camera Parameters:**  
Procure por "Max Digital Zoom" na seção Scaler.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**O que significa?**  
Modos de exposição automática disponíveis.

**Por que existe?**  
Diferentes dispositivos suportam diferentes estratégias de AE.

**Modos comuns:**

| Modo | Descrição |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | Controle de exposição manual |
| `CONTROL_AE_MODE_ON` | Exposição automática |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | Exposição automática com flash sempre ligado |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | Exposição automática com flash automático |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | Exposição automática com redução de olhos vermelhos |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | Exposição automática com flash externo |

**Como é usado?**  
Defina o modo de AE nas solicitações de captura:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Como verificar com o Android Camera Parameters:**  
Procure por "AE Available Modes" na seção Control.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**O que significa?**  
Modos de foco automático disponíveis.

**Por que existe?**  
Diferentes estratégias de foco para diferentes cenários.

**Modos comuns:**

| Modo | Descrição |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Foco manual |
| `CONTROL_AF_MODE_AUTO` | Foco automático de disparo único |
| `CONTROL_AF_MODE_MACRO` | Foco macro |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Foco automático contínuo para vídeo |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Foco automático contínuo para fotos |
| `CONTROL_AF_MODE_EDGE` | Foco automático de borda |
| `CONTROL_AF_MODE_FIXED` | Foco fixo (sem AF) |

**Como é usado?**  
Defina o modo de AF com base no seu caso de uso:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Como verificar com o Android Camera Parameters:**  
Procure por "AF Available Modes" na seção Control.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**O que significa?**  
Modos de balanço de branco automático disponíveis.

**Modos comuns:**

| Modo | Descrição |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Balanço de branco manual |
| `CONTROL_AWB_MODE_AUTO` | Automático |
| `CONTROL_AWB_MODE_INCANDESCENT` | Iluminação de tungstênio |
| `CONTROL_AWB_MODE_FLUORESCENT` | Iluminação fluorescente |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluorescente quente |
| `CONTROL_AWB_MODE_DAYLIGHT` | Luz do dia |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Nublado |

**Como verificar com o Android Camera Parameters:**  
Procure por "AWB Available Modes" na seção Control.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**O que significa?**  
Distâncias focais disponíveis para a(s) lente(s).

**Por que existe?**  
Vários valores indicam capacidades de zoom óptico.

**Como é usado?**  
Determine quais lentes estão disponíveis:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**Distâncias focais comuns:**
- 2.4mm — Grande angular (comum)
- 4.8mm — Telefoto (2x zoom óptico)
- 1.8mm — Ultra grande angular

**Como verificar com o Android Camera Parameters:**  
Procure por "Available Focal Lengths" na seção Lens.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**O que significa?**  
A distância mais próxima em que a lente pode focar.

**Por que existe?**  
Valores menores significam melhor capacidade macro.

**Como é usado?**  
Verifique se a fotografia macro é possível:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// Um valor de 0.1m (10cm) ou menos indica boa capacidade macro
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Como verificar com o Android Camera Parameters:**  
Procure por "Minimum Focus Distance" na seção Lens.

---

## 10. FLASH_INFO_AVAILABLE

**O que significa?**  
Se a câmera tem flash.

**Por que existe?**  
Nem todas as câmeras têm flash (especialmente câmeras frontais).

**Como é usado?**  
Verifique antes de usar o flash:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // Habilitar recursos de flash
}
```

**Como verificar com o Android Camera Parameters:**  
Procure por "Flash Available" na seção Flash.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**O que significa?**  
O tempo de exposição mínimo e máximo suportado.

**Por que existe?**  
Determina a capacidade de pouca luz e a capacidade de congelamento de movimento.

**Como é usado?**  
Verifique o intervalo de exposição para controle manual:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// Converter para segundos para exibição
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Como verificar com o Android Camera Parameters:**  
Procure por "Exposure Time Range" na seção Sensor.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**O que significa?**  
Os valores mínimo e máximo de ISO suportados.

**Por que existe?**  
Determina a capacidade de pouca luz e o desempenho de ruído.

**Como é usado?**  
Verifique o intervalo de ISO para controle manual:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Como verificar com o Android Camera Parameters:**  
Procure por "Sensitivity Range" na seção Sensor.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**O que significa?**  
Todos os tamanhos e formatos de saída suportados.

**Por que existe?**  
Determina quais resoluções e formatos você pode usar.

**Como é usado?**  
Obtenha tamanhos suportados para diferentes casos de uso:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// Tamanhos de pré-visualização
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// Tamanhos de foto
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// Tamanhos de vídeo
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// Tamanhos RAW
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Como verificar com o Android Camera Parameters:**  
Procure por "Preview Sizes", "Picture Sizes", etc. na seção Scaler.

---

## 14. LENS_FACING

**O que significa?**  
Para qual direção a lente está voltada.

**Por que existe?**  
Determina se é uma câmera frontal, traseira ou externa.

**Valores:**
- `LENS_FACING_FRONT` — Câmera frontal (selfie)
- `LENS_FACING_BACK` — Câmera traseira
- `LENS_FACING_EXTERNAL` — Câmera externa

**Como verificar com o Android Camera Parameters:**  
Procure por "Lens Facing" na seção Camera Info.

---

## 15. CONTROL_MAX_REGIONS_AE

**O que significa?**  
Número máximo de regiões de medição de AE.

**Por que existe?**  
Determina o quão precisa pode ser a medição de exposição.

**Como é usado?**  
Limite o número de regiões de AE que você cria:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// Não crie mais do que maxAERegions
```

**Como verificar com o Android Camera Parameters:**  
Procure por "Max Regions AE" na seção Control.

---

## Conclusão

CameraCharacteristics é sua janela para as capacidades da câmera. Ao entender esses parâmetros, você pode:

1. **Construir aplicativos independentes de dispositivo** — Verifique as capacidades antes de usar recursos
2. **Fornecer melhores experiências ao usuário** — Mostre apenas os recursos disponíveis
3. **Otimizar o desempenho** — Escolha resoluções e formatos apropriados
4. **Criar aplicativos profissionais** — Desbloqueie todo o potencial da câmera

## Como Aprender Mais

1. **Aplicativo Android Camera Parameters** — Explore dados reais do seu dispositivo
2. **Documentação do Android** — Leia a documentação oficial do CameraCharacteristics
3. **Experimente** — Escreva pequenos aplicativos de teste para experimentar diferentes parâmetros
4. **Código-fonte** — Olhe o código-fonte do Camera2 para uma compreensão mais profunda

## Resumo

Este capítulo cobriu as características mais importantes do CameraCharacteristics:

1. **Nível de Hardware** — Capacidade geral
2. **Capacidades** — Recursos específicos disponíveis
3. **Array Ativo** — Resolução do sensor
4. **Zoom Digital** — Capacidades de zoom
5. **Modos AE/AF/AWB** — Modos de controle automático
6. **Distâncias Focais** — Capacidades da lente
7. **Distância de Foco** — Capacidade macro
8. **Flash** — Disponibilidade de flash
9. **Intervalo de Exposição/ISO** — Limites de controle manual
10. **Configuração de Fluxo** — Tamanhos e formatos suportados

Com esse conhecimento, você está pronto para construir aplicativos Camera2 avançados!

---

## Palavras Finais

Parabéns! Você concluiu esta série Android Camera2. Agora você entende:

- **Como funcionam as câmeras de smartphone** — Lentes, sensores, ISP
- **Como funciona o Camera2** — CameraManager, CameraDevice, CaptureSession
- **Como capturar fotos** — JPEG, RAW, ImageReader
- **Como controlar a câmera** — ISO, exposição, foco, balanço de branco
- **Recursos profissionais** — Vídeo em alta velocidade, multi-câmera
- **Características da câmera** — A enciclopédia das capacidades da câmera

O aplicativo Android Camera Parameters é uma ótima ferramenta para continuar aprendendo. Explore as capacidades do seu dispositivo e experimente diferentes configurações.

Boa codificação! 📸
