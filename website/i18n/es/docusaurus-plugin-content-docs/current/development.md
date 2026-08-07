---
sidebar_position: 2
description: Arquitectura técnica de la aplicación Android Camera Parameters, incluyendo detalles del patrón MVVM, estructura de la interfaz de usuario con Jetpack Compose y pautas de desarrollo.
keywords: [desarrollo android, mvvm, jetpack compose, tutorial api camera2]
---

# Documentación para Desarrolladores

Este documento proporciona una descripción técnica de la aplicación **Android Camera Parameters**, su arquitectura y pautas de desarrollo.

## Resumen del Proyecto

La aplicación es una herramienta de diagnóstico para inspeccionar las `CameraCharacteristics` de Android Camera2. Proporciona una interfaz moderna y fácil de usar para explorar niveles de hardware, capacidades y valores de parámetros sin procesar para todas las lentes de cámara de un dispositivo.

## Arquitectura

El proyecto sigue el patrón arquitectónico **MVVM (Model-View-ViewModel)** y está construido utilizando **Jetpack Compose** para la capa de interfaz de usuario.

### Componentes Principales

#### `CameraParamsActivity`
El único punto de entrada de la aplicación.
- Maneja los permisos en tiempo de ejecución (CAMERA).
- Inicializa la interfaz de usuario de Compose a través de `setContent`.
- Aloja el `CameraParamsTheme`.

#### `CameraViewModel`
El gestor de estado central para la interfaz de usuario.
- Mantiene el `UiState`, que incluye la lista de cámaras, el índice seleccionado, los parámetros categorizados y la consulta de búsqueda.
- **Detección de Funciones**: Contiene lógica en `detectFeatureFlags()` para determinar dinámicamente las capacidades del hardware como soporte RAW, OIS y Exposición Manual.
- **Categorización**: Agrupa cientos de claves de Camera2 en secciones lógicas (Sensor, Lente, etc.) para una mejor legibilidad.

#### `CameraParamsHelper`
Un envoltorio de utilidad alrededor del `CameraManager` de Android.
- Recupera las `CameraCharacteristics` para IDs específicos.
- Proporciona formato especializado para tipos de cámara complejos (por ejemplo, convirtiendo modos de `IntArray` en cadenas legibles por humanos).

## Capa de Interfaz de Usuario (Jetpack Compose)

La interfaz de usuario está construida utilizando **Material 3** con un tema oscuro estrictamente aplicado.

### Estructura de Navegación

La aplicación utiliza `androidx.navigation.compose` gestionado en `MainScreen.kt`.

| Pantalla | Responsabilidad |
| :--- | :--- |
| **[Resumen](overview.md)** | Panel de alto nivel que muestra la Tarjeta de Resumen, el Nivel de Hardware y los chips de funciones clave. |
| **Categorías** | Lista desplegable de todos los parámetros agrupados por sección con filtrado de búsqueda. |
| **Raw (JSON)** | Representación JSON con resaltado de sintaxis de todas las propiedades de la cámara. |
| **Detalle** | Vista enfocada para un solo parámetro, que muestra el valor formateado y los datos sin procesar. |

### Estilo

- **Tema**: Definido en `Theme.kt`.
- **Colores**: Color primario `#7B61FF` (Violeta) utilizado para resaltados y acciones principales.
- **Superficie**: Fondo oscuro `#121417` con variantes `#1E1F23` para tarjetas.

## Lógica Clave

### Detección Dinámica de Funciones

Los chips de "Funciones clave" en el panel no son estáticos. Se calculan en `CameraViewModel.detectFeatureFlags()`:

- **RAW**: Se comprueba a través de `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS**: Se detecta si `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` contiene `ON`.
- **Exp Manual**: Disponible si se admite `CONTROL_AE_MODE_OFF`.
- **Enfoque Manual**: Habilitado si `LENS_INFO_MINIMUM_FOCUS_DISTANCE` es mayor que 0.

## Guía de Desarrollo

### Requisitos Previos
- Android Studio Ladybug (o más reciente).
- Kotlin 2.0+ (El proyecto utiliza el nuevo plugin de Gradle del compilador de Compose).
- SDK Mínimo: 21 (Android 5.0).

### Añadir una Nueva Categoría
Para añadir o modificar la agrupación de parámetros, actualice el método `getCategoryForKey()` en `CameraViewModel.kt`. Utiliza la coincidencia de cadenas en los nombres de las claves de la cámara para asignarlas a las categorías.

### Actualizar el Tema
Los colores se pueden ajustar en `Color.kt`. La aplicación está diseñada para verse mejor en modo oscuro; cualquier cambio en la paleta clara debe probarse cuidadosamente.
