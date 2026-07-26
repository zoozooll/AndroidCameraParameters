---
sidebar_position: 11
title: "Capítulo 11: Captura RAW y CaptureRequest"
description: Aprende a capturar imágenes RAW y comprende CaptureRequest y CaptureResult para un control avanzado de la cámara.
keywords: [captura RAW, CaptureRequest, CaptureResult, Camera2, controles manuales]
---

La captura RAW te ofrece control completo sobre el procesamiento de imágenes. Vamos a explorarla junto con CaptureRequest y CaptureResult.

## Introducción

En el capítulo anterior, aprendiste a capturar fotos JPEG. Ahora exploraremos:

1. **Captura RAW** — Captura de datos del sensor sin procesar
2. **CaptureRequest** — Configuración de los ajustes de cámara para cada captura
3. **CaptureResult** — Obtención de metadatos sobre capturas completadas

## ¿Qué es RAW?

Las imágenes RAW contienen todos los datos capturados por el sensor antes del procesamiento del ISP. Esto significa:

- Sin reducción de ruido aplicada
- Sin corrección de balance de blancos
- Sin nitidez (sharpening)
- Rango dinámico completo

Los archivos RAW son más grandes, pero ofrecen una flexibilidad de edición sin igual.

## Requisitos de Captura RAW

Para capturar imágenes RAW, tu cámara debe:
1. Tener nivel de hardware **FULL** o **LEVEL_3**
2. Soportar la capacidad `RAW`

Comprueba CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## Capturando Imágenes RAW

Crea un ImageReader con formato RAW:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // o ImageFormat.RAW10/RAW12
    2
)
```

Luego añade las superficies JPEG y RAW a la sesión de captura para una captura simultánea:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest define todos los ajustes para una sola captura. Puedes configurar:

### Controles Automáticos
- `CONTROL_AF_MODE` — Modo de enfoque automático
- `CONTROL_AE_MODE` — Modo de exposición automática
- `CONTROL_AWB_MODE` — Modo de balance de blancos automático

### Controles Manuales
- `SENSOR_SENSITIVITY` — Valor ISO
- `SENSOR_EXPOSURE_TIME` — Tiempo de exposición en nanosegundos
- `LENS_FOCUS_DISTANCE` — Distancia de enfoque
- `LENS_APERTURE` — Abertura (si está disponible)

### Ajustes de Salida
- `JPEG_QUALITY` — Calidad de compresión JPEG
- `JPEG_ORIENTATION` — Orientación de la imagen
- `COLOR_CORRECTION_MODE` — Modo de corrección de color

### Creando un CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// Establecer controles automáticos
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// Establecer calidad JPEG
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// Añadir destinos
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// Construir la solicitud
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult contiene metadatos sobre una captura completada. Incluye:

- **Ajustes reales utilizados** — Lo que la cámara realmente aplicó
- **Estadísticas** — Información de exposición, enfoque y color
- **Marca de tiempo** — Cuándo ocurrió la captura

### Obteniendo CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // Obtener tiempo de exposición real
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // Obtener ISO real
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // Obtener estado de enfoque
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // Obtener estado de AE
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "Exposición: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### Claves Comunes de CaptureResult

| Clave | Descripción |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | Tiempo de exposición real utilizado |
| `SENSOR_SENSITIVITY` | ISO real utilizado |
| `CONTROL_AF_STATE` | Estado de enfoque automático |
| `CONTROL_AE_STATE` | Estado de exposición automática |
| `CONTROL_AWB_STATE` | Estado de balance de blancos automático |
| `SCALER_CROP_REGION` | Región de recorte utilizada |
| `COLOR_CORRECTION_GAINS` | Ganancias de corrección de color |

## Un Ejemplo Completo de Captura RAW

```kotlin
private fun configureDualCapture() {
    // Crear ImageReader JPEG
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // Crear ImageReader RAW
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
    
    // Crear superficies
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // Crear sesión de captura con todas las superficies
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // Añadir ambas superficies como destinos
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // Configurar controles
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

| Característica | RAW | JPEG |
| --- | --- | --- |
| Tamaño de archivo | Grande (20-50MB) | Pequeño (2-10MB) |
| Flexibilidad de edición | Máxima | Limitada |
| Ruido | Preservado | Reducido |
| Balance de blancos | Ajustable | Fijo |
| Rango dinámico | Completo | Comprimido |

## Mejores Prácticas

1. **Comprueba el soporte RAW** — Verifica siempre antes de intentar la captura RAW
2. **Captura dual** — Captura tanto JPEG como RAW para mayor flexibilidad
3. **Cierra las imágenes** — Llama siempre a `image.close()` después del procesamiento
4. **Maneja diferentes formatos** — RAW_SENSOR, RAW10 y RAW12 tienen diferentes disposiciones de bytes

## Próximo Capítulo

En el próximo capítulo, resumiremos lo que has aprendido en la Parte III y nos prepararemos para la Parte IV: Controles Manuales de Cámara.

## Resumen

En este capítulo, aprendiste sobre:

1. **Captura RAW** — Captura de datos del sensor sin procesar para una máxima flexibilidad de edición
2. **CaptureRequest** — Configuración de los ajustes de cámara para cada captura
3. **CaptureResult** — Obtención de metadatos sobre capturas completadas

La captura RAW requiere nivel de hardware FULL o LEVEL_3. Puedes capturar tanto JPEG como RAW simultáneamente añadiendo ambas superficies a la sesión de captura.

CaptureRequest te permite configurar el enfoque automático, la exposición automática, el balance de blancos y los controles manuales como ISO y el tiempo de exposición. CaptureResult te indica qué ajustes utilizó realmente la cámara.

En la Parte IV, profundizaremos en los controles manuales de cámara: ISO, exposición, enfoque y balance de blancos.
