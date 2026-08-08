# Parámetros de Cámara de Android (Android Camera Parameters)

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Consíguelo en Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>
<a href='https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api'><img alt='Tutoriales' src='https://img.shields.io/badge/Tutoriales-Mastering%20Camera2-blue?style=for-the-badge&logo=gitbook' height="45"/></a>

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | [Español] | [Português](README_pt-BR.md) | [Français](README_fr.md) | [Deutsch](README_de.md) | [Русский](README_ru.md) | [हिन्दी](README_hi.md) | [Bahasa Indonesia](README_in.md) | [日本語](README_ja.md) | [한국어](README_ko.md)

Android Camera Parameters es una potente herramienta de diagnóstico para que desarrolladores y entusiastas exploren las capacidades técnicas profundas de las cámaras de sus dispositivos. Aprovecha la API Android Camera2 para proporcionar información detallada sobre cada lente de su dispositivo.

![Resumen de la aplicación](../../website/static/img/camera_params_feature_graph.png)

## Características Principales

*   **Diagnóstico Detallado**: Inspeccione `CameraCharacteristics` para todas las lentes (Trasera, Frontal, Externa).
*   **Detección de Nivel de Hardware**: Vea instantáneamente si su dispositivo admite funciones `LEGACY`, `LIMITED`, `FULL` o `LEVEL_3`.
*   **Seguimiento de Funciones en Tiempo Real**: Verifique el soporte para captura RAW, Estabilización Óptica de Imagen (OIS), Exposición Manual, Enfoque Manual y más a través de un panel intuitivo.
*   **Exploración Categorizada**: Cientos de parámetros organizados por categorías de Sensor, Lente, AE/AF/AWB y Procesamiento con funcionalidad de **búsqueda integrada**.
*   **Favoritos (Próximamente)**: Marque los parámetros consultados con frecuencia para un acceso rápido.
*   **Exportación de Datos Raw**: Vea el perfil completo de la cámara como un JSON estructurado.
*   **Soporte Multilingüe**: Completamente localizado en más de 12 idiomas, incluidos chino, español, japonés y más.

## Idiomas Soportados

La aplicación está localizada para soportar una audiencia global:
- 🇺🇸 Inglés
- 🇨🇳 Chino (Simplificado)
- 🇹🇼/🇭🇰 Chino (Tradicional)
- 🇪🇸 Español
- 🇧🇷 Portugués (Brasil)
- 🇫🇷 Francés
- 🇩🇪 Alemán
- 🇷🇺 Ruso
- 🇮🇳 Hindi
- 🇮🇩 Indonesio
- 🇯🇵 Japonés
- 🇰🇷 Coreano

## Stack Tecnológico

- **Lenguaje**: Kotlin
- **Framework de UI**: Jetpack Compose
- **Sistema de Diseño**: Material 3
- **Arquitectura**: MVVM
- **Bibliotecas**:
    - [Camera2 API](https://developer.android.com/training/camera2): Interacción central con la cámara.
    - [Gson](https://github.com/google/gson): Serialización JSON para exportación de datos raw.
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): Navegación de la aplicación.

## Estructura del Proyecto

- `app/`: Módulo principal de la aplicación Android.
    - `com.aaron.cameraparams.ui`: Pantallas y componentes de UI basados en Compose.
    - `com.aaron.cameraparams.camera`: Lógica para interactuar con CameraManager y recuperar características.
- `camera_parameters/`: Muestras de volcados JSON de parámetros de cámara de varios dispositivos (Pixel 3, Samsung S10+, etc.).
- `docs/`: Documentación adicional y capturas de pantalla.

## Documentación

La documentación completa está disponible en [https://zoozooll.github.io/AndroidCameraParameters/](https://zoozooll.github.io/AndroidCameraParameters/).

*   **[Tutoriales: Dominando la API Android Camera2](https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api)** 📚
*   [Resumen de la aplicación](https://zoozooll.github.io/AndroidCameraParameters/)
*   [Guía del Desarrollador](https://zoozooll.github.io/AndroidCameraParameters/development)
*   [Seguridad de Datos](https://zoozooll.github.io/AndroidCameraParameters/data-safety)

## Primeros Pasos

### Requisitos Previos

- Android Studio Koala o más reciente.
- Android SDK 37 (Compilación/Objetivo).
- Un dispositivo Android físico (recomendado) o un emulador con soporte para Camera2.

### Construir y Ejecutar

1. Clone el repositorio:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. Abra el proyecto en Android Studio.
3. Construya el proyecto:
   ```bash
   ./gradlew assembleDebug
   ```
4. Instale y ejecute en su dispositivo.

## Licencia

Este proyecto está bajo la **Licencia MIT**. Consulte el archivo [LICENSE](../../LICENSE) para más detalles.

## Soporte o Contacto

Correo electrónico: kangkang365@gmail.com
Sitio del Proyecto: [Documentación de Android Camera Parameters](https://zoozooll.github.io/AndroidCameraParameters/)
