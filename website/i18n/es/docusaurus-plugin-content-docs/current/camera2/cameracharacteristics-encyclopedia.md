---
sidebar_position: 15
title: "Capítulo 15: Enciclopedia de CameraCharacteristics"
description: Una guía completa de las características más importantes de Camera2, incluyendo qué significan, por qué existen y cómo usarlas.
keywords: [CameraCharacteristics, parámetros de cámara, capacidades de cámara, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

Bienvenido a la Enciclopedia de CameraCharacteristics — tu guía para entender cada parámetro de la cámara.

## Introducción

CameraCharacteristics contiene cientos de parámetros que describen las capacidades de una cámara. En este capítulo, exploraremos los más importantes en profundidad:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — ¿Qué puede hacer la cámara?
2. `REQUEST_AVAILABLE_CAPABILITIES` — ¿Qué características están disponibles?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — ¿Cuál es el tamaño del sensor?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — ¿Cuánto zoom?
5. `CONTROL_AE_AVAILABLE_MODES` — ¿Qué modos de exposición?

Y muchos más...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**¿Qué significa?**  
Esta es la característica más importante. Define el nivel de capacidad general del dispositivo de cámara.

**¿Por qué existe?**  
Diferentes dispositivos Android tienen diferentes capacidades de cámara. Este parámetro ayuda a las aplicaciones a entender qué pueden hacer.

**Valores admitidos:**

| Valor | Nivel de API | Descripción |
| --- | --- | --- |
| `LEGACY` | 21 | Dispositivos antiguos, la API Camera2 es una envoltura sobre la antigua API Camera |
| `LIMITED` | 21 | Características básicas de Camera2, sin controles manuales |
| `FULL` | 21 | Controles manuales completos, captura RAW, captura en ráfaga |
| `LEVEL_3` | 24 | Características avanzadas como reprocesamiento YUV, HDR de 10 bits |

**¿Cómo se usa?**  
Verifica esto antes de intentar cualquier operación avanzada:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // Funcionalidad limitada
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // Solo características básicas
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // Controles manuales completos disponibles
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // Características avanzadas disponibles
    }
}
```

**¿Cómo verificar con Android Camera Parameters?**  
Abre la aplicación y busca "Hardware Level" en la sección Camera Info.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**¿Qué significa?**  
Este arreglo lista todas las capacidades admitidas por la cámara.

**¿Por qué existe?**  
Incluso dentro del mismo nivel de hardware, diferentes dispositivos pueden admitir diferentes características.

**Capacidades comunes:**

| Capacidad | Descripción |
| --- | --- |
| `BACKWARD_COMPATIBLE` | Modo de compatibilidad básica |
| `MANUAL_SENSOR` | Control manual de ISO y exposición |
| `MANUAL_POST_PROCESSING` | Corrección de color y reducción de ruido manuales |
| `RAW` | Captura de imágenes RAW |
| `BURST_CAPTURE` | Captura en ráfaga de alta velocidad |
| `YUV_REPROCESSING` | Reprocesamiento de imágenes YUV |
| `DEPTH_OUTPUT` | Salida de mapa de profundidad |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | Captura de video de alta velocidad |
| `LOGICAL_MULTI_CAMERA` | Cámara lógica que combina múltiples cámaras físicas |
| `CONCURRENT_CAMERA` | Múltiples cámaras pueden abrirse simultáneamente |
| `CAMERA_EXTENSION` | Extensiones específicas del fabricante (retrato, modo nocturno) |

**¿Cómo se usa?**  
Verifica las capacidades antes de usar una característica:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // Habilitar captura RAW
}
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Available Capabilities" en la sección Camera Info.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**¿Qué significa?**  
El arreglo activo es el área real del sensor utilizada para capturar imágenes.

**¿Por qué existe?**  
El sensor puede tener píxeles alrededor de los bordes que están reservados para calibración. El arreglo activo representa el área utilizable.

**¿Cómo se usa?**  
Esto te indica la resolución máxima disponible para captura:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**Características relacionadas:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — Total de píxeles en el sensor (puede ser mayor que el arreglo activo)
- `SENSOR_INFO_SENSOR_SIZE` — Dimensiones físicas en milímetros

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Active Array Size" y "Sensor Size" en la sección Sensor.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**¿Qué significa?**  
El factor máximo de zoom digital admitido por la cámara.

**¿Por qué existe?**  
El zoom digital recorta y amplía la imagen, reduciendo la calidad. Conocer el máximo ayuda a gestionar las expectativas del usuario.

**¿Cómo se usa?**  
Establece el nivel de zoom en las solicitudes de captura:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// Establecer zoom (1.0 = sin zoom, maxZoom = zoom máximo)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**Características relacionadas:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — Longitudes focales físicas (para zoom óptico)

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Max Digital Zoom" en la sección Scaler.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**¿Qué significa?**  
Modos de exposición automática disponibles.

**¿Por qué existe?**  
Diferentes dispositivos admiten diferentes estrategias de AE.

**Modos comunes:**

| Modo | Descripción |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | Control de exposición manual |
| `CONTROL_AE_MODE_ON` | Exposición automática |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | Exposición automática con flash siempre encendido |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | Exposición automática con flash automático |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | Exposición automática con reducción de ojos rojos |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | Exposición automática con flash externo |

**¿Cómo se usa?**  
Establece el modo AE en las solicitudes de captura:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "AE Available Modes" en la sección Control.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**¿Qué significa?**  
Modos de enfoque automático disponibles.

**¿Por qué existe?**  
Diferentes estrategias de enfoque para diferentes escenarios.

**Modos comunes:**

| Modo | Descripción |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Enfoque manual |
| `CONTROL_AF_MODE_AUTO` | Enfoque automático de una sola toma |
| `CONTROL_AF_MODE_MACRO` | Enfoque macro |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Enfoque automático continuo para video |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Enfoque automático continuo para fotos |
| `CONTROL_AF_MODE_EDGE` | Enfoque automático de bordes |
| `CONTROL_AF_MODE_FIXED` | Enfoque fijo (sin AF) |

**¿Cómo se usa?**  
Establece el modo AF según tu caso de uso:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "AF Available Modes" en la sección Control.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**¿Qué significa?**  
Modos de balance de blancos automático disponibles.

**Modos comunes:**

| Modo | Descripción |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Balance de blancos manual |
| `CONTROL_AWB_MODE_AUTO` | Automático |
| `CONTROL_AWB_MODE_INCANDESCENT` | Iluminación de tungsteno |
| `CONTROL_AWB_MODE_FLUORESCENT` | Iluminación fluorescente |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluorescente cálido |
| `CONTROL_AWB_MODE_DAYLIGHT` | Luz de día |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Nublado |

**¿Cómo verificar con Android Camera Parameters?**  
Busca "AWB Available Modes" en la sección Control.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**¿Qué significa?**  
Longitudes focales disponibles para la(s) lente(s).

**¿Por qué existe?**  
Múltiples valores indican capacidades de zoom óptico.

**¿Cómo se usa?**  
Determina qué lentes están disponibles:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**Longitudes focales comunes:**
- 2.4mm — Gran angular (común)
- 4.8mm — Telefoto (zoom óptico 2x)
- 1.8mm — Ultra gran angular

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Available Focal Lengths" en la sección Lens.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**¿Qué significa?**  
La distancia más cercana a la que la lente puede enfocar.

**¿Por qué existe?**  
Valores más bajos significan mejor capacidad macro.

**¿Cómo se usa?**  
Verifica si la fotografía macro es posible:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// Un valor de 0.1m (10cm) o menos indica buena capacidad macro
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Minimum Focus Distance" en la sección Lens.

---

## 10. FLASH_INFO_AVAILABLE

**¿Qué significa?**  
Si la cámara tiene flash.

**¿Por qué existe?**  
No todas las cámaras tienen flash (especialmente las cámaras frontales).

**¿Cómo se usa?**  
Verifica antes de usar el flash:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // Habilitar características de flash
}
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Flash Available" en la sección Flash.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**¿Qué significa?**  
El tiempo de exposición mínimo y máximo admitido.

**¿Por qué existe?**  
Determina la capacidad de poca luz y la capacidad de congelar movimiento.

**¿Cómo se usa?**  
Verifica el rango de exposición para control manual:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// Convertir a segundos para visualización
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Exposure Time Range" en la sección Sensor.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**¿Qué significa?**  
Los valores mínimo y máximo de ISO admitidos.

**¿Por qué existe?**  
Determina la capacidad de poca luz y el rendimiento de ruido.

**¿Cómo se usa?**  
Verifica el rango de ISO para control manual:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Sensitivity Range" en la sección Sensor.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**¿Qué significa?**  
Todos los tamaños y formatos de salida admitidos.

**¿Por qué existe?**  
Determina qué resoluciones y formatos puedes usar.

**¿Cómo se usa?**  
Obtén los tamaños admitidos para diferentes casos de uso:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// Tamaños de vista previa
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// Tamaños de foto
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// Tamaños de video
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// Tamaños RAW
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Preview Sizes", "Picture Sizes", etc. en la sección Scaler.

---

## 14. LENS_FACING

**¿Qué significa?**  
Hacia qué dirección está orientada la lente.

**¿Por qué existe?**  
Determina si es una cámara frontal, trasera o externa.

**Valores:**
- `LENS_FACING_FRONT` — Cámara de selfie
- `LENS_FACING_BACK` — Cámara trasera  
- `LENS_FACING_EXTERNAL` — Cámara externa

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Lens Facing" en la sección Camera Info.

---

## 15. CONTROL_MAX_REGIONS_AE

**¿Qué significa?**  
Número máximo de regiones de medición AE.

**¿Por qué existe?**  
Determina qué tan precisa puede ser la medición de exposición.

**¿Cómo se usa?**  
Limita el número de regiones AE que creas:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// No crear más de maxAERegions
```

**¿Cómo verificar con Android Camera Parameters?**  
Busca "Max Regions AE" en la sección Control.

---

## Conclusión

CameraCharacteristics es tu ventana a las capacidades de la cámara. Al entender estos parámetros, puedes:

1. **Crear aplicaciones independientes del dispositivo** — Verifica las capacidades antes de usar las características
2. **Proporcionar mejores experiencias de usuario** — Muestra solo las características disponibles
3. **Optimizar el rendimiento** — Elige resoluciones y formatos apropiados
4. **Crear aplicaciones profesionales** — Desbloquea todo el potencial de la cámara

## Cómo aprender más

1. **Aplicación Android Camera Parameters** — Explora datos reales de tu dispositivo
2. **Documentación de Android** — Lee la documentación oficial de CameraCharacteristics
3. **Experimenta** — Escribe pequeñas aplicaciones de prueba para probar diferentes parámetros
4. **Código fuente** — Mira el código fuente de Camera2 para una comprensión más profunda

## Resumen

Este capítulo cubrió las CameraCharacteristics más importantes:

1. **Nivel de hardware** — Capacidad general
2. **Capacidades** — Características específicas disponibles
3. **Arreglo activo** — Resolución del sensor
4. **Zoom digital** — Capacidades de zoom
5. **Modos AE/AF/AWB** — Modos de control automático
6. **Longitudes focales** — Capacidades de la lente
7. **Distancia de enfoque** — Capacidad macro
8. **Flash** — Disponibilidad de flash
9. **Rango de exposición/ISO** — Límites de control manual
10. **Configuración de flujo** — Tamaños y formatos admitidos

¡Con este conocimiento, estás listo para crear aplicaciones avanzadas de Camera2!

---

## Palabras finales

¡Felicidades! Has completado esta serie de Android Camera2. Ahora entiendes:

- **Cómo funcionan las cámaras de los smartphones** — Lentes, sensores, ISP
- **Cómo funciona Camera2** — CameraManager, CameraDevice, CaptureSession
- **Cómo capturar fotos** — JPEG, RAW, ImageReader
- **Cómo controlar la cámara** — ISO, exposición, enfoque, balance de blancos
- **Características profesionales** — Video de alta velocidad, multicámara
- **Características de la cámara** — La enciclopedia de las capacidades de la cámara

La aplicación Android Camera Parameters es una gran herramienta para seguir aprendiendo. Explora las capacidades de tu dispositivo y experimenta con diferentes configuraciones.

¡Feliz codificación! 📸
