---
sidebar_position: 4
title: "Capítulo 4: Explorando la cámara de tu teléfono"
description: Usa la aplicación Android Camera Parameters para explorar las capacidades de la cámara de tu dispositivo, incluyendo IDs de cámara, resoluciones, FPS, nivel de hardware, soporte RAW y más.
keywords: [parámetros de cámara, IDs de cámara, nivel de hardware, soporte RAW, FPS, resolución]
---

Aquí es donde el aprendizaje se vuelve interactivo. ¡Exploremos la cámara de tu teléfono!

## Introducción

Leer sobre cámaras es útil, pero nada supera ver datos reales de tu propio dispositivo. Ahí es donde entra Android Camera Parameters.

Este capítulo se trata de **exploración**. Aún no hay programación. Solo curiosidad.

## Instala Android Camera Parameters

Si aún no lo has hecho, instala la aplicación Android Camera Parameters en tu dispositivo Android:

1. Abre Google Play Store
2. Busca "Android Camera Parameters"
3. Instala la aplicación
4. Ábrela y otorga permisos de cámara

## Conociendo la interfaz

Cuando abras la aplicación, verás varias secciones:

### Pantalla de resumen
Muestra información básica de la cámara de un vistazo:
- IDs de cámara
- Orientación del lente
- Nivel de hardware
- Tamaño del sensor
- Capacidades disponibles

### Pantalla de categorías
Organiza los parámetros de la cámara en grupos lógicos:
- Información de la cámara
- Sensor
- Lente
- Control
- Escalador
- Flash
- Y más

### Pantalla RAW JSON
Muestra las CameraCharacteristics completas en formato JSON crudo para usuarios avanzados.

## Vamos a explorar

Revisemos la información clave que debes buscar.

### IDs de cámara

Android asigna a cada cámara un número de ID único. Busca:
- **Cámara 0** — Usualmente la cámara trasera gran angular
- **Cámara 1** — Podría ser la cámara frontal u otra cámara trasera
- **Cámara 2** — A menudo la cámara ultra gran angular o telefoto
- **Cámara 3+** — Cámaras adicionales (macro, profundidad, etc.)

Cada ID representa un dispositivo de cámara independiente con sus propias características.

### Nivel de hardware

Esta es una de las piezas de información más importantes:

| Nivel | Descripción |
| --- | --- |
| **LEGACY** | Dispositivos antiguos, soporte limitado de Camera2 |
| **LIMITED** | Características básicas de Camera2 |
| **FULL** | Controles manuales completos, soporte RAW |
| **LEVEL_3** | Características avanzadas como el reprocesamiento YUV |

Comprueba qué nivel de hardware admite tu teléfono. Esto determina qué características de Camera2 están disponibles.

### Información del sensor

Busca:
- **Tamaño del sensor** — Dimensiones físicas del sensor
- **Tamaño de matriz activa** — El área real utilizada para capturar imágenes
- **Tamaño de matriz de píxeles** — Total de píxeles en el sensor
- **Duración máxima de fotograma** — Tiempo mínimo entre fotogramas
- **Formatos de salida** — JPEG, RAW, YUV, etc.

### Opciones de resolución

Las cámaras admiten múltiples resoluciones. Comprueba:
- **Tamaños de vista previa** — Resoluciones disponibles para visualización
- **Tamaños de foto** — Resoluciones disponibles para captura de fotos
- **Tamaños de video** — Resoluciones disponibles para grabación de video

Nota las diferentes relaciones de aspecto: 4:3, 16:9, 1:1.

### Frecuencia de fotogramas (FPS)

Busca:
- **Rango de FPS de vista previa** — Fotogramas por segundo para la vista previa
- **Rango de FPS de captura** — Fotogramas por segundo para captura fija
- **Video de alta velocidad** — Modos especiales de alta frecuencia de fotogramas

Un FPS más alto significa video más fluido y enfoque automático más receptivo.

### Soporte RAW

Comprueba si tu cámara admite captura RAW:
- **Formatos RAW** — RAW_SENSOR, RAW10, RAW12, RAW16
- **Tamaños RAW** — Resoluciones disponibles para captura RAW

El soporte RAW requiere al menos el nivel de hardware FULL.

### Capacidades del flash

Busca:
- **Modo de flash** — OFF, ON, AUTO, TORCH
- **Modos disponibles** — Qué características de flash son compatibles
- **Información del flash** — Intensidad y capacidades del flash

### Zoom

Comprueba:
- **Zoom digital máximo** — Cuánto puedes hacer zoom digitalmente
- **Distancias focales disponibles** — Diferentes lentes y sus distancias focales
- **Soporte de zoom suave** — Si el zoom se puede ajustar de forma fluida

### Modos de enfoque

Busca:
- **Modos de enfoque disponibles** — AUTO, FIXED, MACRO, CONTINUOUS_VIDEO, CONTINUOUS_PICTURE, EDGE
- **Rango de distancia de enfoque** — Distancia de enfoque mínima y máxima
- **Regiones AF** — Número de regiones de enfoque automático admitidas

### Control de exposición

Comprueba:
- **Modos AE** — AUTO, ON, OFF
- **Modos AE disponibles** — Qué modos de exposición son compatibles
- **Rango de exposición** — Tiempos de exposición mínimos y máximos
- **Rango ISO** — Valores ISO admitidos

## Tu turno

Ahora es tu turno de explorar. Responde estas preguntas sobre tu teléfono:

1. ¿Cuántas cámaras tiene tu teléfono?
2. ¿Qué nivel de hardware admiten?
3. ¿Qué cámara admite RAW?
4. ¿Cuál es la resolución más alta disponible?
5. ¿Alguna cámara admite video 4K?
6. ¿Cuál es el nivel de zoom máximo?
7. ¿Tu teléfono tiene cámara telefoto o ultra gran angular?

## Por qué importa esto

Podrías preguntarte por qué estamos explorando antes de programar. Aquí está el porqué:

1. **Cada teléfono es diferente** — Lo que funciona en un dispositivo puede no funcionar en otro
2. **Camera2 requiere adaptación** — Las buenas aplicaciones de Camera2 consultan las capacidades, no las asumen
3. **La comprensión construye intuición** — Cuando ves datos reales, los conceptos abstractos se vuelven concretos

Cuando empecemos a programar, ya sabrás qué esperar de tu dispositivo.

## Compara con amigos

Si tienes amigos con diferentes teléfonos, compara tus hallazgos:
- ¿El teléfono insignia tiene un mejor nivel de hardware?
- ¿Los teléfonos económicos carecen de soporte RAW?
- ¿Cómo varía el número de cámaras?

Esto te ayuda a comprender el ecosistema de cámaras Android.

## Descubrimientos comunes

Aquí hay algunas cosas comunes que la gente descubre:

- Los **teléfonos insignia** a menudo tienen nivel de hardware FULL o LEVEL_3
- Los **teléfonos económicos** a menudo tienen nivel de hardware LIMITED o LEGACY
- La **mayoría de los teléfonos** admiten captura JPEG
- El **soporte RAW** aún no es universal
- Las **múltiples cámaras** son estándar en los teléfonos modernos
- Las **cámaras frontales** generalmente tienen menor resolución que las traseras

## Próximo capítulo

Ahora que has explorado la cámara de tu teléfono, ¡estás listo para empezar a programar! En el próximo capítulo, presentaremos la primera clase de Camera2: **CameraManager**.

CameraManager es el punto de entrada a la API de Camera2. Te permite:
- Enumerar las cámaras disponibles
- Obtener las características de la cámara
- Abrir cámaras

¡Comencemos!

## Resumen

Explorar la cámara de tu teléfono es la mejor manera de entender lo que puede hacer Camera2. Android Camera Parameters lo hace fácil al mostrar todas las capacidades de la cámara de forma organizada.

Cosas clave que debes buscar:
- IDs de cámara y sus funciones
- Nivel de hardware (LEGACY, LIMITED, FULL, LEVEL_3)
- Opciones de resolución
- Soporte RAW
- Capacidades de flash y zoom
- Controles de enfoque y exposición

Esta exploración práctica sienta las bases para escribir aplicaciones Camera2.
