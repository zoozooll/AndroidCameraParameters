---
sidebar_position: 7
title: "Capítulo 7: Entendendo CameraCharacteristics"
description: Explore CameraCharacteristics para aprender sobre lente frontal, nível de hardware, tamanho do sensor e outras capacidades importantes da câmera.
keywords: [CameraCharacteristics, lente frontal, nível de hardware, tamanho do sensor, capacidades da câmera]
---

CameraCharacteristics é sua janela para a alma da câmera. Vamos explorá-la.

## Introdução

No capítulo anterior, você aprendeu como listar câmeras e obter informações básicas. Agora vamos mergulhar mais fundo no **CameraCharacteristics** — a descrição abrangente das capacidades de uma câmera.

CameraCharacteristics contém centenas de parâmetros. Neste capítulo, focaremos nos mais importantes.

## O que é CameraCharacteristics?

CameraCharacteristics é um objeto imutável que contém todos os metadados sobre um dispositivo de câmera. Ele descreve:

- **Propriedades de hardware** — Tamanho do sensor, características da lente
- **Capacidades** — O que a câmera pode fazer
- **Modos** — Modos de foco, exposição e balanço de branco disponíveis
- **Opções de saída** — Resoluções e formatos suportados
- **Desempenho** — Taxas de quadros, intervalos de exposição

Você obtém CameraCharacteristics do CameraManager:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## Principais Chaves de CameraCharacteristics

Vamos explorar as características mais importantes.

### 1. Lente Frontal

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

Valores possíveis:
- `LENS_FACING_FRONT` — Câmera frontal (selfie)
- `LENS_FACING_BACK` — Câmera traseira
- `LENS_FACING_EXTERNAL` — Câmera externa

### 2. Nível de Hardware

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

Esta é uma das características mais importantes:

| Nível | Nível da API | Recursos |
| --- | --- | --- |
| **LEGACY** | 21 | Suporte limitado ao Camera2, envolvendo a antiga API de Câmera |
| **LIMITED** | 21 | Recursos básicos do Camera2, sem controles manuais |
| **FULL** | 21 | Controles manuais completos, captura RAW |
| **LEVEL_3** | 24 | Recursos avançados como reprocessamento YUV |

### 3. Tamanho do Sensor

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width e sensorSize.height dão as dimensões
```

O tamanho do sensor informa quantos pixels o sensor possui. Isso é diferente da resolução da imagem — o sensor pode ter mais pixels do que os usados em uma única captura.

### 4. Tamanho do Array Ativo

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

O array ativo é a área real do sensor usada para capturar imagens. Geralmente é ligeiramente menor que o array de pixels porque alguns pixels são reservados para calibração.

### 5. Capacidades Disponíveis

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

Este array informa quais recursos a câmera suporta:
- `BACKWARD_COMPATIBLE` — Compatibilidade básica
- `MANUAL_SENSOR` — Controles manuais do sensor
- `MANUAL_POST_PROCESSING` — Pós-processamento manual
- `RAW` — Suporte a captura RAW
- `BURST_CAPTURE` — Captura em rajada
- `YUV_REPROCESSING` — Reprocessamento YUV
- `DEPTH_OUTPUT` — Saída de profundidade
- `CONSTRAINED_HIGH_SPEED_VIDEO` — Vídeo em alta velocidade

### 6. Formatos de Saída

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

O mapa de configuração de fluxo contém todos os formatos e tamanhos de saída suportados pela câmera:
- `ImageFormat.JPEG` — JPEG padrão
- `ImageFormat.RAW_SENSOR` — Dados brutos do sensor
- `ImageFormat.YUV_420_888` — Formato YUV
- `ImageFormat.RAW10` — RAW de 10 bits
- `ImageFormat.RAW12` — RAW de 12 bits

### 7. Tamanhos de Pré-visualização Suportados

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

Isso fornece todas as resoluções de pré-visualização disponíveis para a câmera.

### 8. Tamanhos de Foto Suportados

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

Estas são as resoluções disponíveis para captura de imagens fixas.

### 9. Distâncias Focais

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

Este array contém as distâncias focais (em milímetros) da lente. Vários valores indicam capacidades de zoom óptico.

### 10. Intervalo de Distância de Foco

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

A distância mínima de foco informa o quão perto a câmera pode focar. Um valor menor significa melhor capacidade macro.

## Um Exemplo Prático

Vamos criar um aplicativo de informações de câmera mais detalhado:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Lente frontal
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
        CameraCharacteristics.LENS_FACING_BACK -> "Traseira"
        else -> "Externa"
    }
    
    // Nível de hardware
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Desconhecido"
    }
    
    // Tamanho do sensor
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Tamanho do array ativo
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Distâncias focais
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Desconhecido"
    
    // Capacidades disponíveis
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Compatibilidade com versões anteriores"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Sensor Manual"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "Captura RAW"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Captura em Rajada"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "Reprocessamento YUV"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Saída de Profundidade"
            else -> "Capacidade desconhecida"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Câmera $cameraId ===")
    Log.d("CameraDetails", "Lente: $lensFacingStr")
    Log.d("CameraDetails", "Nível de Hardware: $hardwareLevelStr")
    Log.d("CameraDetails", "Tamanho do Sensor: $sensorSizeStr")
    Log.d("CameraDetails", "Array Ativo: $activeArrayStr")
    Log.d("CameraDetails", "Distâncias Focais: $focalLengthsStr")
    Log.d("CameraDetails", "Capacidades: ${capabilitiesList.joinToString(", ")}")
}
```

## Exemplo de Saída

```
=== Câmera 0 ===
Lente: Traseira
Nível de Hardware: FULL
Tamanho do Sensor: 4032 x 3024
Array Ativo: 4000 x 3000
Distâncias Focais: 2.4mm, 4.8mm
Capacidades: Compatibilidade com versões anteriores, Sensor Manual, Captura RAW, Captura em Rajada
```

## Por que CameraCharacteristics é Importante

Antes de abrir uma câmera ou criar uma sessão de captura, você **deve** verificar CameraCharacteristics:

1. **Verifique as capacidades** — Não assuma que um recurso é suportado
2. **Escolha a câmera certa** — Selecione com base na lente, nível de hardware, etc.
3. **Configure as saídas** — Use resoluções e formatos suportados
4. **Lide com diferenças entre dispositivos** — O que funciona em um dispositivo pode não funcionar em outro

## Explore com Android Camera Parameters

Abra o aplicativo Android Camera Parameters e navegue pelas características. Você verá centenas de parâmetros organizados por categoria:

- **Info da Câmera** — Informações básicas da câmera
- **Sensor** — Características do sensor
- **Lente** — Propriedades da lente
- **Controle** — Exposição automática, foco automático, balanço de branco
- **Scaler** — Tamanhos e formatos de saída
- **Flash** — Capacidades do flash
- **Estatísticas** — Saída de estatísticas

Isso oferece uma visão completa das capacidades da sua câmera.

## Próximo Capítulo

Agora que você entende CameraCharacteristics, está pronto para abrir sua primeira câmera! No próximo capítulo, vamos:

1. Aprender sobre CameraDevice
2. Abrir uma câmera usando CameraManager
3. Lidar com callbacks de estado da câmera
4. Entender o ciclo de vida da câmera

## Resumo

CameraCharacteristics contém todas as informações que você precisa para entender as capacidades de uma câmera:

- **Lente** — Frontal, traseira ou externa
- **Nível de hardware** — LEGACY, LIMITED, FULL, LEVEL_3
- **Tamanho do sensor** — Dimensões físicas
- **Array ativo** — Área de captura
- **Distâncias focais** — Capacidades da lente
- **Capacidades** — Recursos suportados
- **Formatos de saída** — Formatos de imagem disponíveis

Sempre verifique CameraCharacteristics antes de usar uma câmera. Isso garante que seu aplicativo funcione em diferentes dispositivos.

No próximo capítulo, abriremos nossa primeira câmera usando CameraDevice.
