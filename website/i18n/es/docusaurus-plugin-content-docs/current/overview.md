---
sidebar_position: 1
slug: /
description: Resumen del panel de Android Camera Parameters y sus funciones clave de diagnóstico, incluyendo detección de nivel de hardware y seguimiento de funciones en tiempo real.
keywords: [panel de cámara android, detección de nivel de hardware, diagnóstico de cámara]
---

# Resumen de la Aplicación

Esta página proporciona un desglose detallado del panel de la aplicación y sus funciones clave.

![Resumen de la Aplicación](/img/camera_params_feature_graph.png)

## Componentes del Panel

### 1. Navegación y Selección
- **Menú lateral**: Acceda a la Política de privacidad, Calificar aplicación e información Sobre nosotros a través del icono de menú en la parte superior izquierda.
- **Selección de cámara**: Toque el nombre de la cámara o la insignia de ID (por ejemplo, "0") para abrir un desplegable y cambiar entre las lentes disponibles (trasera, frontal, ultra gran angular, etc.).
- **Navegación inferior**: Cambie sin problemas entre **Resumen**, **Categorías**, **JSON sin procesar** y **Favoritos**.

### 2. Tarjeta de Resumen
La tarjeta de resumen en la parte superior proporciona la información más crítica:
- **Nivel de hardware**: El nivel de soporte de la API Camera2 (LEGACY, LIMITED, FULL o LEVEL_3). Esto determina las capacidades generales de la lente.

### 3. Cuadrícula de Funciones Clave
Una cuadrícula visual que proporciona el estado instantáneo de funciones de grado profesional:
- **Resolución y tamaño del sensor**: Características físicas del sensor.
- **FPS de video máximo**: Capacidades máximas de velocidad de fotogramas.
- **Soporte RAW**: Indica si el sensor puede emitir datos sin comprimir.
- **OIS (Estabilización Óptica de Imagen)**: Disponibilidad de estabilización física de la lente.
- **Control manual**: Estado del soporte de exposición manual y enfoque manual.
- **Procesamiento**: Soporte para HDR, detección de rostros y reducción de ojos rojos.

### 4. Parámetros categorizados (Pestaña Categorías)
Explore la lista completa de CameraCharacteristics organizada en grupos lógicos:
- **Sensor**: Resolución, tamaño físico, rangos de sensibilidad.
- **Lente**: Distancia focal, apertura, modos de estabilización.
- **AE/AF/AWB**: Modos de control detallados para exposición, enfoque y balance de blancos.
- **Búsqueda**: Utilice la barra de búsqueda integrada para encontrar rápidamente claves o valores específicos de la API.
