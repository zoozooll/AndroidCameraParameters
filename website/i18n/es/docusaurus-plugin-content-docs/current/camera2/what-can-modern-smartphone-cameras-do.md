---
sidebar_position: 3
title: "Capítulo 3: ¿Qué pueden hacer las cámaras de los smartphones modernos?"
description: Descubre las increíbles capacidades de las cámaras de los smartphones modernos, incluyendo modo retrato, HDR, modo nocturno, captura RAW y más.
keywords: [características de cámara de smartphone, modo retrato, HDR, modo nocturno, RAW, vídeo 8K]
---

Los smartphones modernos son capaces de realizar fotografías que antes solo eran posibles con cámaras profesionales.

## Introducción

Mira lo que puede hacer una cámara de smartphone hoy en día:

- **Fotos de retrato** con desenfoque de fondo de aspecto profesional
- **Modo nocturno** que captura detalles en condiciones de casi oscuridad
- **HDR** que conserva tanto los reflejos brillantes como las sombras oscuras
- **Grabación de vídeo 8K**
- **Cámara lenta** a cientos de fotogramas por segundo
- **Captura de imágenes RAW** para edición profesional
- **Fotografía macro** de objetos diminutos
- **Zoom telefoto** de hasta 10x o más
- **Tomas de paisaje gran angular**

Estas características no son magia. Son el resultado de hardware y software sofisticados trabajando juntos. Y Camera2 te da control sobre muchas de ellas.

## Modo Retrato

El modo retrato crea un hermoso desenfoque de fondo, también conocido como **bokeh**. Esto hace que el sujeto destaque de forma espectacular.

¿Cómo funciona?

La mayoría de los teléfonos utilizan **múltiples cámaras** para crear información de profundidad. Una cámara captura la imagen principal mientras otra calcula la profundidad. El software luego combina estas para desenfocar el fondo de forma selectiva.

Camera2 permite a las aplicaciones:
- Acceder a múltiples cámaras simultáneamente
- Consultar capacidades de profundidad
- Controlar los efectos de bokeh

## HDR — Alto Rango Dinámico

Los ojos humanos pueden ver un rango de brillo mucho más amplio que un sensor de cámara. HDR resuelve este problema capturando múltiples exposiciones y combinándolas.

- **Exposición brillante** captura los reflejos
- **Exposición oscura** captura las sombras
- **Exposición media** captura los detalles

El ISP fusiona estas en una sola imagen con un rango dinámico increíble.

Camera2 proporciona:
- Modos de captura HDR
- Controles de horquillado de exposición
- Ajustes manuales de exposición

## Modo Nocturno

El modo nocturno es una de las características más impresionantes de los smartphones modernos. Puede capturar fotos nítidas y detalladas en condiciones de casi oscuridad.

La técnica es simple en concepto:
1. Capturar múltiples fotogramas
2. Alinearlos para reducir el desenfoque por movimiento
3. Combinarlos para aumentar la sensibilidad a la luz
4. Aplicar reducción de ruido

Algunos teléfonos incluso pueden capturar fotos en entornos tan oscuros que los humanos apenas pueden ver.

## Cámara Lenta

El vídeo en cámara lenta te permite ver momentos que ocurren demasiado rápido para el ojo humano:
- Gotas de agua
- Explotaciones de globos
- Acciones deportivas
- Eventos de la naturaleza

Los teléfonos pueden grabar a:
- 120 fps — Cámara lenta suave
- 240 fps — Cámara lenta estándar
- 480 fps — Cámara lenta extrema
- 960 fps — Súper cámara lenta

Camera2 expone las capacidades de vídeo de alta velocidad a través de:
- Configuraciones de alta velocidad de fotogramas
- Solicitudes de captura especiales
- Requisitos de nivel de hardware

## Vídeo 8K

El vídeo 8K tiene cuatro veces la resolución del 4K y 16 veces la resolución del HD. Esto permite:
- Recortar y reencuadrar sin pérdida de calidad
- Primer planos detallados
- Producción de vídeo profesional

No todos los teléfonos admiten 8K, pero Camera2 te permite descubrir cuáles sí lo hacen.

## Captura RAW

Las imágenes RAW contienen todos los datos capturados por el sensor antes del procesamiento del ISP. Esto da a los fotógrafos control total sobre:
- Exposición
- Balance de blancos
- Contraste
- Saturación
- Reducción de ruido

Los archivos RAW son más grandes pero ofrecen una flexibilidad de edición inigualable.

Camera2 permite:
- Captura de imágenes RAW
- Selección de formato RAW
- Controles manuales para obtener resultados RAW óptimos

## Fotografía Macro

Los lentes macro enfocan objetos muy cerca de la cámara — a veces a solo unos centímetros de distancia. Esto abre todo un nuevo mundo de fotografía:
- Flores e insectos
- Texturas y patrones
- Objetos diminutos

Camera2 te permite:
- Cambiar entre cámaras incluyendo la macro
- Controlar la distancia de enfoque
- Ajustar la iluminación para primeros planos

## Telefoto

Los lentes telefoto acercan objetos distantes sin moverse físicamente. Los teléfonos insignia suelen tener:
- Zoom óptico 2x
- Zoom óptico 3x
- Zoom híbrido de hasta 10x
- Zoom digital de hasta 100x

Camera2 proporciona:
- Controles de zoom
- Información de distancia focal
- Detección de zoom óptico vs digital

## Gran Angular

Los lentes gran angular capturan un campo de visión mucho más amplio que los lentes estándar. Son perfectos para:
- Paisajes
- Arquitectura
- Fotos de grupo
- Espacios interiores

Algunos lentes gran angular pueden capturar 120 grados o más.

## Modo Manual

El modo manual da a los usuarios control total sobre:
- **ISO** — Sensibilidad del sensor
- **Tiempo de exposición** — Cuánto tiempo el sensor recopila luz
- **Enfoque** — Control manual de enfoque
- **Balance de blancos** — Temperatura de color

Camera2 es la base para todas las aplicaciones de cámara manuales.

## Camera2 Puede Controlarlo Todo

¿La parte emocionante? **Puedes controlar todas estas características con Camera2.**

Al final de esta serie, entenderás:
- Qué características admite tu teléfono
- Cómo habilitarlas programáticamente
- Cómo combinarlas para obtener resultados impresionantes

## Explora con Android Camera Parameters

Antes de empezar a programar, comprueba qué admite tu teléfono:

1. Abre Android Camera Parameters
2. Busca:
   - Capacidades disponibles
   - Niveles de zoom
   - Velocidades de fotogramas
   - Nivel de hardware
   - Soporte RAW
   - Capacidades de flash

¡Puede que te sorprenda lo que tu dispositivo puede hacer!

## Siguiente Capítulo

En el próximo capítulo, profundizaremos en la exploración de las capacidades de la cámara de tu teléfono. Aprenderás a usar Android Camera Parameters para descubrir:

- IDs de cámara y sus significados
- Opciones de resolución
- Capacidades de velocidad de fotogramas
- Nivel de hardware
- Y mucho más

Esta exploración práctica hará que programar con Camera2 sea mucho más fácil.

## Resumen

Las cámaras de los smartphones modernos son increíblemente potentes. Pueden capturar fotos de retrato, imágenes HDR, tomas nocturnas, vídeo en cámara lenta, vídeo 8K, imágenes RAW y mucho más.

Camera2 proporciona las herramientas para controlar estas características programáticamente. En el próximo capítulo, exploraremos las capacidades específicas de tu teléfono usando Android Camera Parameters.
