# Параметры камеры Android (Android Camera Parameters)

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Доступно в Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>
<a href='https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api'><img alt='Учебные пособия' src='https://img.shields.io/badge/Tutorials-Mastering%20Camera2-blue?style=for-the-badge&logo=gitbook' height="45"/></a>

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | [Español](README_es.md) | [Português](README_pt-BR.md) | [Français](README_fr.md) | [Deutsch](README_de.md) | [Русский] | [हिन्दी](README_hi.md) | [Bahasa Indonesia](README_in.md) | [日本語](README_ja.md) | [한국어](README_ko.md)

Android Camera Parameters — это мощный диагностический инструмент для разработчиков и энтузиастов, позволяющий изучить глубокие технические возможности камер их устройств. Он использует Android Camera2 API для предоставления подробной информации о каждом объективе вашего устройства.

![Обзор приложения](../../website/static/img/camera_params_feature_graph.png)

## Ключевые особенности

*   **Подробная диагностика**: Проверяйте `CameraCharacteristics` для всех объективов (задних, передних, внешних).
*   **Определение уровня оборудования**: Мгновенно узнайте, поддерживает ли ваше устройство уровни функций `LEGACY`, `LIMITED`, `FULL` или `LEVEL_3`.
*   **Отслеживание функций в реальном времени**: Проверяйте поддержку захвата RAW, оптической стабилизации изображения (OIS), ручной экспозиции, ручного фокуса и многого другого через интуитивно понятную панель управления.
*   **Категоризированное исследование**: Сотни параметров, организованных по категориям «Сенсор», «Объектив», «AE/AF/AWB» и «Обработка», со **встроенной функцией поиска**.
*   **Избранное (скоро)**: Добавляйте часто проверяемые параметры в закладки для быстрого доступа.
*   **Экспорт необработанных данных**: Просматривайте полный профиль камеры в виде структурированного JSON.
*   **Многоязычная поддержка**: Полностью локализовано на более чем 12 языков, включая китайский, испанский, японский и другие.

## Поддерживаемые языки

Приложение локализовано для поддержки глобальной аудитории:
- 🇺🇸 Английский
- 🇨🇳 Китайский (упрощенный)
- 🇹🇼/🇭🇰 Китайский (традиционный)
- 🇪🇸 Испанский
- 🇧🇷 Португальский (Бразилия)
- 🇫🇷 Французский
- 🇩🇪 Немецкий
- 🇷🇺 Русский
- 🇮🇳 Хинди
- 🇮🇩 Индонезийский
- 🇯🇵 Японский
- 🇰🇷 Корейский

## Технологический стек

- **Язык**: Kotlin
- **UI Framework**: Jetpack Compose
- **Дизайн-система**: Material 3
- **Архитектура**: MVVM
- **Библиотеки**:
    - [Camera2 API](https://developer.android.com/training/camera2): Основное взаимодействие с камерой.
    - [Gson](https://github.com/google/gson): Сериализация JSON для экспорта необработанных данных.
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): Навигация в приложении.

## Структура проекта

- `app/`: Основной модуль Android-приложения.
    - `com.aaron.cameraparams.ui`: Экраны и компоненты пользовательского интерфейса на базе Compose.
    - `com.aaron.cameraparams.camera`: Логика взаимодействия с CameraManager и получения характеристик.
- `camera_parameters/`: Примеры дампов JSON параметров камеры с различных устройств (Pixel 3, Samsung S10+ и т. д.).
- `docs/`: Дополнительная документация и скриншоты.

## Начало работы

### Предварительные условия

- Android Studio Koala или новее.
- Android SDK 37 (Compile/Target).
- Физическое устройство Android (рекомендуется) или эмулятор с поддержкой Camera2.

### Сборка и запуск

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. Откройте проект в Android Studio.
3. Соберите проект:
   ```bash
   ./gradlew assembleDebug
   ```
4. Установите и запустите на своем устройстве.

## Лицензия

Этот проект лицензируется по **лицензии MIT**. Подробности см. в файле [LICENSE](../../LICENSE).

## Поддержка или контакты

Электронная почта: kangkang365@gmail.com
Сайт проекта: [Документация по параметрам камеры Android](https://zoozooll.github.io/AndroidCameraParameters/)
