---
sidebar_position: 11
title: "Capítulo 11: Captura RAW e CaptureRequest"
description: Aprenda a capturar imagens RAW e entenda CaptureRequest e CaptureResult para controle avançado de câmera.
keywords: [captura RAW, CaptureRequest, CaptureResult, Camera2, controles manuais]
---

A captura RAW oferece controle total sobre o processamento de imagem. Vamos explorá-la junto com CaptureRequest e CaptureResult.

## Introdução

No capítulo anterior, você aprendeu a capturar fotos JPEG. Agora vamos explorar:

1. **Captura RAW** — Captura de dados do sensor não processados
2. **CaptureRequest** — Configuração das configurações da câmera para cada captura
3. **CaptureResult** — Obtenção de metadados sobre capturas concluídas

## O que é RAW?

Imagens RAW contêm todos os dados capturados pelo sensor antes do processamento do ISP. Isso significa que:

- Nenhuma redução de ruído aplicada
- Nenhuma correção de balanço de branco
- Nenhum nitidez
- Faixa dinâmica completa

Arquivos RAW são maiores, mas oferecem flexibilidade de edição incomparável.

## Requisitos para Captura RAW

Para capturar imagens RAW, sua câmera deve:
1. Ter nível de hardware **FULL** ou **LEVEL_3**
2. Suportar a capacidade `RAW`

Verifique CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## Capturando Imagens RAW

Crie um ImageReader com formato RAW:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // ou ImageFormat.RAW10/RAW12
    2
)
```

Depois adicione as superfícies JPEG e RAW à sessão de captura para captura simultânea:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest define todas as configurações para uma única captura. Você pode configurar:

### Controles Automáticos
- `CONTROL_AF_MODE` — Modo de foco automático
- `CONTROL_AE_MODE` — Modo de exposição automática
- `CONTROL_AWB_MODE` — Modo de balanço de branco automático

### Controles Manuais
- `SENSOR_SENSITIVITY` — Valor ISO
- `SENSOR_EXPOSURE_TIME` — Tempo de exposição em nanossegundos
- `LENS_FOCUS_DISTANCE` — Distância de foco
- `LENS_APERTURE` — Abertura (se disponível)

### Configurações de Saída
- `JPEG_QUALITY` — Qualidade de compressão JPEG
- `JPEG_ORIENTATION` — Orientação da imagem
- `COLOR_CORRECTION_MODE` — Modo de correção de cor

### Criando um CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// Define controles automáticos
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// Define qualidade JPEG
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// Adiciona alvos
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// Constrói a requisição
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult contém metadados sobre uma captura concluída. Inclui:

- **Configurações reais usadas** — O que a câmera realmente aplicou
- **Estatísticas** — Informações de exposição, foco e cor
- **Timestamp** — Quando a captura ocorreu

### Obtendo CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // Obtém tempo de exposição real
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // Obtém ISO real
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // Obtém estado de foco
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // Obtém estado de AE
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "Exposição: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### Chaves Comuns de CaptureResult

| Chave | Descrição |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | Tempo de exposição real usado |
| `SENSOR_SENSITIVITY` | ISO real usado |
| `CONTROL_AF_STATE` | Estado do foco automático |
| `CONTROL_AE_STATE` | Estado da exposição automática |
| `CONTROL_AWB_STATE` | Estado do balanço de branco automático |
| `SCALER_CROP_REGION` | Região de corte usada |
| `COLOR_CORRECTION_GAINS` | Ganhos de correção de cor |

## Um Exemplo Completo de Captura RAW

```kotlin
private fun configureDualCapture() {
    // Cria ImageReader JPEG
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // Cria ImageReader RAW
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
    
    // Cria superfícies
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // Cria sessão de captura com todas as superfícies
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // Adiciona ambas as superfícies como alvos
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // Configura controles
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

| Recurso | RAW | JPEG |
| --- | --- | --- |
| Tamanho do arquivo | Grande (20-50MB) | Pequeno (2-10MB) |
| Flexibilidade de edição | Máxima | Limitada |
| Ruído | Preservado | Reduzido |
| Balanço de branco | Ajustável | Fixo |
| Faixa dinâmica | Completa | Comprimida |

## Melhores Práticas

1. **Verifique o suporte RAW** — Sempre verifique antes de tentar a captura RAW
2. **Captura dupla** — Capture JPEG e RAW para maior flexibilidade
3. **Feche as imagens** — Sempre chame `image.close()` após o processamento
4. **Lide com diferentes formatos** — RAW_SENSOR, RAW10 e RAW12 têm layouts de bytes diferentes

## Próximo Capítulo

No próximo capítulo, resumiremos o que você aprendeu na Parte III e nos prepararemos para a Parte IV: Controles Manuais de Câmera.

## Resumo

Neste capítulo, você aprendeu sobre:

1. **Captura RAW** — Captura de dados do sensor não processados para máxima flexibilidade de edição
2. **CaptureRequest** — Configuração das configurações da câmera para cada captura
3. **CaptureResult** — Obtenção de metadados sobre capturas concluídas

A captura RAW requer nível de hardware FULL ou LEVEL_3. Você pode capturar JPEG e RAW simultaneamente adicionando ambas as superfícies à sessão de captura.

CaptureRequest permite configurar foco automático, exposição automática, balanço de branco e controles manuais como ISO e tempo de exposição. CaptureResult informa quais configurações foram realmente usadas pela câmera.

Na Parte IV, mergulharemos profundamente nos controles manuais da câmera: ISO, exposição, foco e balanço de branco.
