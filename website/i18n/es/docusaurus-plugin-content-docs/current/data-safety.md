---
sidebar_position: 3
description: Prácticas de privacidad y seguridad de datos para la aplicación Android Camera Parameters. Obtenga información sobre los permisos de la cámara y cómo se manejan los metadatos del hardware.
keywords: [seguridad de datos, política de privacidad, permisos de android]
---

# Guía de Seguridad de Datos

Este documento describe las prácticas de recopilación de datos y privacidad de la aplicación Android Camera Parameters.

## Resumen

Android Camera Parameters está diseñado con un enfoque de "Privacidad Primero". Como herramienta de diagnóstico, necesita acceder a la información del hardware para funcionar, pero no recopila ni transmite datos personales.

## Permisos

### Permiso de Cámara (`android.permission.CAMERA`)
- **Requisito**: Necesario para acceder al `CameraManager` y recuperar las `CameraCharacteristics`.
- **Uso**: La aplicación solo lee metadatos de hardware. **No graba** video ni toma fotos sin una acción explícita del usuario (por ejemplo, en futuras versiones si se agrega la prueba de captura de imágenes).

## Recopilación de Datos

- **Información Personal**: La aplicación **no recopila** nombres, direcciones de correo electrónico, números de teléfono ni ningún otro identificador personal.
- **Datos de Ubicación**: La aplicación **no accede** a su GPS ni a su ubicación de red.
- **Metadatos de Hardware**: La aplicación lee las especificaciones técnicas de las lentes de su cámara (resolución, distancia focal, modos admitidos). Estos datos permanecen en su dispositivo a menos que use explícitamente la función "Exportar JSON" para compartirlos.

## Intercambio de Datos

La aplicación **no comparte** ningún dato con terceros. No hay SDKs de seguimiento (como Firebase Analytics o Facebook SDK) integrados en la aplicación principal.

## Control del Usuario

- **Exportación JSON**: Los usuarios pueden optar por copiar o compartir el JSON de parámetros de cámara sin procesar. Esto es iniciado enteramente por el usuario.
- **Permisos**: Puede revocar el permiso de Cámara en cualquier momento a través de la Configuración del Sistema de Android, aunque la aplicación no podrá mostrar los detalles de la cámara sin él.
