---
sidebar_position: 7
title: "Capítulo 7: Entendiendo CameraCharacteristics"
description: Explora CameraCharacteristics para conocer la orientación del lente, el nivel de hardware, el tamaño del sensor y otras capacidades importantes de la cámara.
keywords: [CameraCharacteristics, orientación del lente, nivel de hardware, tamaño del sensor, capacidades de la cámara]
---

CameraCharacteristics es tu ventana al alma de la cámara. Vamos a explorarla.

## Introducción

En el capítulo anterior, aprendiste cómo listar cámaras y obtener información básica. Ahora profundizaremos en **CameraCharacteristics** — la descripción completa de las capacidades de una cámara.

CameraCharacteristics contiene cientos de parámetros. En este capítulo, nos enfocaremos en los más importantes.

## ¿Qué es CameraCharacteristics?

CameraCharacteristics es un objeto inmutable que contiene todos los metadatos sobre un dispositivo de cámara. Describe:

- **Propiedades de hardware** — Tamaño del sensor, características del lente
- **Capacidades** — Lo que la cámara puede hacer
- **Modos** — Modos de enfoque, exposición y balance de blancos disponibles
- **Opciones de salida** — Resoluciones y formatos soportados
- **Rendimiento** — Tasas de fotogramas, rangos de exposición

Obtienes CameraCharacteristics de CameraManager:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## Claves Principales de CameraCharacteristics

Exploremos las características más importantes.

### 1. Orientación del Lente

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

Valores posibles:
- `LENS_FACING_FRONT` — Cámara frontal (selfie)
- `LENS_FACING_BACK` — Cámara trasera
- `LENS_FACING_EXTERNAL` — Cámara externa

### 2. Nivel de Hardware

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

Esta es una de las características más importantes:

| Nivel | Nivel de API | Características |
| --- | --- | --- |
| **LEGACY** | 21 | Soporte limitado de Camera2, envuelve la antigua API de Cámara |
| **LIMITED** | 21 | Características básicas de Camera2, sin controles manuales |
| **FULL** | 21 | Controles manuales completos, captura RAW |
| **LEVEL_3** | 24 | Características avanzadas como reprocesamiento YUV |

### 3. Tamaño del Sensor

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width y sensorSize.height dan las dimensiones
```

El tamaño del sensor te dice cuántos píxeles tiene el sensor. Esto es diferente de la resolución de la imagen — el sensor puede tener más píxeles de los que se usan en una sola captura.

### 4. Tamaño de la Matriz Activa

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

La matriz activa es el área real del sensor utilizada para capturar imágenes. Esto suele ser ligeramente menor que la matriz de píxeles porque algunos píxeles están reservados para calibración.

### 5. Capacidades Disponibles

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

Este arreglo te indica qué características soporta la cámara:
- `BACKWARD_COMPATIBLE` — Compatibilidad básica
- `MANUAL_SENSOR` — Controles manuales del sensor
- `MANUAL_POST_PROCESSING` — Procesamiento posterior manual
- `RAW` — Soporte de captura RAW
- `BURST_CAPTURE` — Captura en ráfaga
- `YUV_REPROCESSING` — Reprocesamiento YUV
- `DEPTH_OUTPUT` — Salida de profundidad
- `CONSTRAINED_HIGH_SPEED_VIDEO` — Video de alta velocidad

### 6. Formatos de Salida

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

El mapa de configuración de flujo contiene todos los formatos y tamaños de salida soportados por la cámara:
- `ImageFormat.JPEG` — JPEG estándar
- `ImageFormat.RAW_SENSOR` — Datos RAW del sensor
- `ImageFormat.YUV_420_888` — Formato YUV
- `ImageFormat.RAW10` — RAW de 10 bits
- `ImageFormat.RAW12` — RAW de 12 bits

### 7. Tamaños de Vista Previa Soportados

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

Esto te da todas las resoluciones de vista previa disponibles para la cámara.

### 8. Tamaños de Imagen Soportados

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

Estas son las resoluciones disponibles para la captura de imágenes fijas.

### 9. Longitudes Focales

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

Este arreglo contiene las longitudes focales (en milímetros) del lente. Múltiples valores indican capacidades de zoom óptico.

### 10. Rango de Distancia de Enfoque

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

La distancia mínima de enfoque te indica qué tan cerca puede enfocar la cámara. Un valor menor significa mejor capacidad macro.

## Un Ejemplo Práctico

Vamos a crear una app de información de cámara más detallada:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Orientación del lente
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
        CameraCharacteristics.LENS_FACING_BACK -> "Trasera"
        else -> "Externa"
    }
    
    // Nivel de hardware
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Desconocido"
    }
    
    // Tamaño del sensor
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Tamaño de la matriz activa
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Longitudes focales
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Desconocido"
    
    // Capacidades disponibles
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Compatible con versiones anteriores"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Sensor manual"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "Captura RAW"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Captura en ráfaga"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "Reprocesamiento YUV"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Salida de profundidad"
            else -> "Capacidad desconocida"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Cámara $cameraId ===")
    Log.d("CameraDetails", "Orientación del lente: $lensFacingStr")
    Log.d("CameraDetails", "Nivel de hardware: $hardwareLevelStr")
    Log.d("CameraDetails", "Tamaño del sensor: $sensorSizeStr")
    Log.d("CameraDetails", "Matriz activa: $activeArrayStr")
    Log.d("CameraDetails", "Longitudes focales: $focalLengthsStr")
    Log.d("CameraDetails", "Capacidades: ${capabilitiesList.joinToString(", ")}")
}
```

## Ejemplo de Salida

```
=== Cámara 0 ===
Orientación del lente: Trasera
Nivel de hardware: FULL
Tamaño del sensor: 4032 x 3024
Matriz activa: 4000 x 3000
Longitudes focales: 2.4mm, 4.8mm
Capacidades: Compatible con versiones anteriores, Sensor manual, Captura RAW, Captura en ráfaga
```

## Por Qué Importa CameraCharacteristics

Antes de abrir una cámara o crear una sesión de captura, **debes** verificar CameraCharacteristics:

1. **Verificar capacidades** — No asumas que una característica está soportada
2. **Elegir la cámara correcta** — Selecciona según la orientación del lente, nivel de hardware, etc.
3. **Configurar salidas** — Usa resoluciones y formatos soportados
4. **Manejar diferencias de dispositivos** — Lo que funciona en un dispositivo puede no funcionar en otro

## Explora con Android Camera Parameters

Abre la app Android Camera Parameters y navega por las características. Verás cientos de parámetros organizados por categoría:

- **Información de la cámara** — Información básica de la cámara
- **Sensor** — Características del sensor
- **Lente** — Propiedades del lente
- **Control** — Exposición automática, enfoque automático, balance de blancos
- **Escalador** — Tamaños y formatos de salida
- **Flash** — Capacidades del flash
- **Estadísticas** — Salida de estadísticas

Esto te da una imagen completa de las capacidades de tu cámara.

## Próximo Capítulo

Ahora que entiendes CameraCharacteristics, ¡estás listo para abrir tu primera cámara! En el próximo capítulo, vamos a:

1. Conocer CameraDevice
2. Abrir una cámara usando CameraManager
3. Manejar callbacks de estado de la cámara
4. Entender el ciclo de vida de la cámara

## Resumen

CameraCharacteristics contiene toda la información que necesitas para entender las capacidades de una cámara:

- **Orientación del lente** — Frontal, trasera o externa
- **Nivel de hardware** — LEGACY, LIMITED, FULL, LEVEL_3
- **Tamaño del sensor** — Dimensiones físicas
- **Matriz activa** — Área de captura
- **Longitudes focales** — Capacidades del lente
- **Capacidades** — Características soportadas
- **Formatos de salida** — Formatos de imagen disponibles

Siempre verifica CameraCharacteristics antes de usar una cámara. Esto asegura que tu app funcione en diferentes dispositivos.

En el próximo capítulo, abriremos nuestra primera cámara usando CameraDevice.
