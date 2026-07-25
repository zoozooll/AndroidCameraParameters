---
sidebar_position: 1
title: "Capítulo 1: Bienvenido a Android Camera2"
description: Descubra por qué es importante Android Camera2, en qué se diferencia de CameraX y qué cubrirá esta serie.
keywords: [Android Camera2, CameraX, capacidades de cámara, desarrollo de cámara en Android]
---

Antes de controlar la cámara, necesitamos entender el sistema de la cámara.

## Introducción

Casi todos los smartphones actuales tienen un sistema de cámara potente. Un teléfono moderno puede:

- Capturar fotos con aspecto profesional
- Grabar vídeos en 4K y 8K
- Crear efectos de retrato
- Disparar con luz extremadamente baja
- Capturar vídeos a cámara lenta
- Generar información de profundidad
- Combinar varias cámaras juntas

Pero cuando abres la aplicación de cámara predeterminada, solo ves una interfaz sencilla: un botón de obturador, un control de zoom y unos pocos modos de disparo.

Detrás de esta interfaz sencilla hay un sistema sorprendentemente complejo. La aplicación de cámara se comunica con componentes de hardware, procesadores de imagen y marcos de trabajo de Android para producir cada fotograma.

Como desarrolladores de Android, es posible que queramos crear aplicaciones que vayan más allá de la aplicación de cámara predeterminada, como:

- Una aplicación de fotografía manual
- Una herramienta de prueba de cámara
- Una aplicación de visión por computadora
- Una aplicación de escaneo 3D
- Un grabador de vídeo profesional
- Un analizador de capacidades de cámara

Para crear estas aplicaciones, necesitamos entender la API Android Camera2.

## ¿Qué es Android Camera2?

Android Camera2 es el marco de trabajo de cámara moderno introducido por Google en Android 5.0 (API nivel 21). Reemplazó a la API de cámara original de Android.

La antigua API de cámara fue diseñada para un mundo más sencillo: una cámara, captura de fotos básica y grabación de vídeo sencilla. Las cámaras de los smartphones han evolucionado drásticamente desde entonces. Los dispositivos modernos pueden contener varias cámaras traseras, lentes gran angular, teleobjetivos, sensores de profundidad y cámaras externas.

También admiten funciones avanzadas:

- Exposición y enfoque manuales
- Captura de imágenes RAW
- Vídeo de alta velocidad
- Procesamiento HDR
- Estabilización óptica

Camera2 fue creada para dar a los desarrolladores un control mucho más profundo sobre el hardware de la cámara.

## Camera2 frente a CameraX

Camera2 y CameraX resuelven problemas diferentes.

### CameraX

CameraX es una biblioteca de nivel superior diseñada para facilitar las tareas comunes de la cámara, incluyendo:

- Mostrar una vista previa
- Tomar fotos
- Grabar vídeos
- Manejar la compatibilidad de dispositivos

La mayoría de las aplicaciones deberían empezar con CameraX.

### Camera2

Camera2 es el marco de trabajo de nivel inferior. Proporciona a los desarrolladores acceso directo a las capacidades de la cámara, incluyendo información del sensor, ajustes de exposición, controles de enfoque, metadatos de la cámara, capacidades de hardware y soporte RAW.

Camera2 es más compleja, pero proporciona mucho más control.

| | CameraX | Camera2 |
| --- | --- | --- |
| Nivel | Biblioteca de nivel superior | API de nivel inferior |
| Dificultad | Más fácil | Más compleja |
| Control | Limitado | Extenso |
| Ideal para | Aplicaciones de cámara normales | Aplicaciones de cámara avanzadas |

Esta serie de tutoriales se centra en Camera2 porque entenderla nos ayuda a comprender cómo funcionan realmente las cámaras de Android.

## ¿Por qué aprender Camera2?

Te preguntarás: *¿Por qué debería aprender Camera2 cuando ya existe CameraX?*

### Entender qué puede hacer realmente el dispositivo

Cada teléfono Android es diferente. Un dispositivo puede admitir la captura RAW, vídeo 4K a 60 fps y controles manuales; otro puede que no. Camera2 permite a las aplicaciones descubrir estas capacidades.

### Crear aplicaciones de cámara profesionales

Las aplicaciones que necesitan funciones de cámara avanzadas suelen necesitar Camera2. Algunos ejemplos son las aplicaciones de cámara profesional, las aplicaciones de imágenes científicas, las aplicaciones de RA, los sistemas de visión por computadora y las herramientas de producción de vídeo.

### Entender la fotografía con smartphones

Muchas funciones de las cámaras modernas se basan en conceptos expuestos a través de Camera2:

- Exposición
- ISO
- Enfoque
- Balance de blancos
- HDR
- Múltiples cámaras

Aprender Camera2 también te enseña cómo funcionan las cámaras de los smartphones.

## ¿Qué aprenderás en esta serie?

Esta serie está diseñada para llevarte de principiante a avanzado.

### Parte 1: Entender las cámaras

Aprenderás cómo funcionan las cámaras de los smartphones, qué contiene el hardware de la cámara, cómo representa Android las cámaras y cómo inspeccionar tu propio dispositivo.

### Parte 2: Tu primera aplicación con Camera2

Aprenderás a buscar y abrir cámaras, crear una vista previa y capturar imágenes.

### Parte 3: Controles de la cámara

Aprenderás sobre la exposición, el ISO, el enfoque, el balance de blancos, el flash y el zoom.

### Parte 4: Funciones avanzadas de la cámara

Aprenderás sobre la captura RAW, el vídeo de alta velocidad, los dispositivos multicámara, las cámaras lógicas y físicas, las extensiones de cámara y las funciones HDR.

### Parte 5: Inmersión profunda en los metadatos de la cámara

Explorarás parámetros importantes de Camera2, incluyendo:

- `android.sensor.info.activeArraySize`
- `android.scaler.availableMaxDigitalZoom`
- `android.control.aeAvailableModes`
- `android.request.availableCapabilities`

Entenderás no solo lo que significan estos parámetros, sino también por qué existen.

## Aprender con Android Camera Parameters

Leer la documentación es útil, pero las capacidades de la cámara son más fáciles de entender cuando puedes ver datos reales de un teléfono real. A lo largo de esta serie, utilizaremos [Android Camera Parameters](/) para explorar información real de la cámara.

Puedes usar la aplicación para descubrir:

- Cámaras disponibles
- Resoluciones admitidas
- Tasas de fotogramas
- Información del sensor
- Soporte de control manual
- Capacidad RAW
- Nivel de hardware

En lugar de aprender con ejemplos abstractos, puedes investigar directamente tu propio dispositivo.

## ¿Para quién es este tutorial?

Esta serie está diseñada para:

- **Desarrolladores de Android** que quieran entender el sistema de cámara más allá de las API básicas.
- **Desarrolladores de aplicaciones de cámara** que necesiten funciones avanzadas.
- **Desarrolladores de visión por computadora** que necesiten acceder a los fotogramas y metadatos de la cámara.
- **Desarrolladores curiosos** que quieran entender cómo funcionan realmente las cámaras de los smartphones.

## Antes de empezar a programar

Camera2 no es difícil porque la API esté mal diseñada. Es difícil porque las cámaras modernas son extremadamente potentes.

Una cámara de smartphone ya no es solo un sensor que captura imágenes. Es un sistema de imagen completo que involucra:

- Hardware
- Firmware
- Procesamiento ISP
- Marco de trabajo de Android
- Software de aplicación

Camera2 expone esta complejidad a los desarrolladores. Nuestro objetivo en esta serie es entenderla paso a paso.

## Próximo capítulo

En el próximo capítulo, **Entender el hardware de la cámara del smartphone**, dejaremos Android por un momento y exploraremos la cámara en sí. Aprenderás qué hace un sensor de cámara, por qué los sensores más grandes producen mejores imágenes, qué significan realmente las lentes, cómo funciona el procesamiento ISP y por qué dos teléfonos con recuentos de megapíxeles similares pueden producir fotos completamente diferentes.

Una vez que entiendas el hardware, los conceptos de Camera2 serán mucho más fáciles.

## Resumen

Android Camera2 es la base para crear aplicaciones de cámara avanzadas en Android. Proporciona acceso directo a las capacidades y controles de la cámara que están ocultos detrás de las aplicaciones de cámara normales.

Esta serie te guiará desde la comprensión de las cámaras de los smartphones hasta la creación de aplicaciones Camera2 de nivel profesional. Comencemos el viaje.
