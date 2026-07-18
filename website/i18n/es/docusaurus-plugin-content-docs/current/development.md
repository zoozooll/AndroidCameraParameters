---
sidebar_position: 2
---

# Documentación para Desarrolladores

Este documento proporciona una descripción técnica de la aplicación **Android Camera Parameters**, su arquitectura y pautas de desarrollo.

## Descripción del Proyecto

La aplicación es una herramienta de diagnóstico para inspeccionar las `CameraCharacteristics` de Android Camera2. Proporciona una interfaz moderna y fácil de usar para explorar los niveles de hardware, las capacidades y los valores de parámetros sin procesar de todas las lentes de cámara de un dispositivo.

## Arquitectura

El proyecto sigue el patrón arquitectónico **MVVM (Model-View-ViewModel)** y está construido utilizando **Jetpack Compose** para la capa de interfaz de usuario.

### Componentes Principales

#### `CameraParamsActivity`
El único punto de entrada de la aplicación.
- Maneja permisos en tiempo de ejecución (CAMERA).
- Inicializa la interfaz de usuario de Compose a través de `setContent`.
- Alberga el `CameraParamsTheme`.

#### `CameraViewModel`
El gestor de estado central para la interfaz de usuario.
- Mantiene el `UiState`, que incluye la lista de cámaras, el índice seleccionado, los parámetros categorizados y la consulta de búsqueda.
- **Detección de Funciones**: Contiene lógica en `detectFeatureFlags()` para determinar dinámicamente capacidades de hardware como soporte RAW, OIS y Exposición Manual.
- **Categorización**: Agrupa cientos de claves Camera2 en secciones lógicas (Sensor, Lente, etc.) para una mejor legibilidad.

#### `CameraParamsHelper`
Un envoltorio de utilidad alrededor del `CameraManager` de Android.
- Recupera `CameraCharacteristics` para IDs específicos.
- Proporciona formateo especializado para tipos de cámara complejos (por ejemplo, convirtiendo modos `IntArray` en cadenas legibles por humanos).

## Capa de UI (Jetpack Compose)

La interfaz de usuario está construida con **Material 3** con un tema oscuro estrictamente aplicado.

### Estructura de Navegação

La aplicación utiliza `androidx.navigation.compose` gestionada en `MainScreen.kt`.

| Pantalla | Responsabilidad |
| :--- | :--- |
| **[Resumen](overview.md)** | Panel de alto nivel que muestra la Tarjeta de Resumen, el Nivel de Hardware y las fichas de Funciones Clave. |
| **Categorías** | Lista desplegable de todos los parámetros agrupados por sección con filtrado de búsqueda. |
| **Raw (JSON)** | Representación JSON con resaltado de sintaxis de todas las propiedades de la cámara. |
| **Detalle** | Vista enfocada para un solo parámetro, que muestra el valor formateado y los datos sin procesar. |

### Estilo

- **Tema**: Definido en `Theme.kt`.
- **Colores**: Color primario `#7B61FF` (Violeta) utilizado para resaltados y acciones principales.
- **Superficie**: Fondo oscuro `#121417` con variantes `#1E1F23` para tarjetas.

## Lógica Clave

### Detección Dinámica de Funciones

Las fichas de "Funciones Clave" en el panel no son estáticas. Se calculan en `CameraViewModel.detectFeatureFlags()`:

- **RAW**: Se verifica a través de `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS**: Se detecta si `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` contiene `ON`.
- **Exp. Manual**: Disponible si se admite `CONTROL_AE_MODE_OFF`.
- **Enfoque Manual**: Habilitado si `LENS_INFO_MINIMUM_FOCUS_DISTANCE` es mayor que 0.

## Guía de Desarrollo

### Requisitos Previos
- Android Studio Ladybug (o más reciente).
- Kotlin 2.0+ (El proyecto utiliza el nuevo complemento Gradle de Compose Compiler).
- SDK Mínimo: 21 (Android 5.0).

### Agregar una Nueva Categoría
Para agregar o modificar la agrupación de parámetros, actualice el método `getCategoryForKey()` en `CameraViewModel.kt`. Utiliza coincidencia de cadenas en los nombres de las claves de la cámara para asignarlas a categorías.

### Actualizar el Tema
Los colores se pueden ajustar en `Color.kt`. La aplicación está diseñada para verse mejor en modo oscuro; cualquier cambio en la paleta clara debe probarse cuidadosamente.
