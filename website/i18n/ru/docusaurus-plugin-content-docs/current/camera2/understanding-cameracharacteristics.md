---
sidebar_position: 7
title: "Глава 7: Понимание CameraCharacteristics"
description: Изучите CameraCharacteristics, чтобы узнать о направлении объектива, уровне оборудования, размере сенсора и других важных возможностях камеры.
keywords: [CameraCharacteristics, направление объектива, уровень оборудования, размер сенсора, возможности камеры]
---

CameraCharacteristics — это ваше окно в душу камеры. Давайте изучим его.

## Введение

В предыдущей главе вы узнали, как перечислять камеры и получать базовую информацию. Теперь мы углубимся в **CameraCharacteristics** — подробное описание возможностей камеры.

CameraCharacteristics содержит сотни параметров. В этой главе мы сосредоточимся на самых важных из них.

## Что такое CameraCharacteristics?

CameraCharacteristics — это неизменяемый объект, который содержит все метаданные об устройстве камеры. Он описывает:

- **Аппаратные свойства** — Размер сенсора, характеристики объектива
- **Возможности** — Что может делать камера
- **Режимы** — Доступные режимы фокусировки, экспозиции и баланса белого
- **Варианты вывода** — Поддерживаемые разрешения и форматы
- **Производительность** — Частота кадров, диапазоны экспозиции

Вы получаете CameraCharacteristics из CameraManager:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## Ключевые ключи CameraCharacteristics

Давайте изучим самые важные характеристики.

### 1. Направление объектива

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

Возможные значения:
- `LENS_FACING_FRONT` — Фронтальная камера (селфи)
- `LENS_FACING_BACK` — Основная камера
- `LENS_FACING_EXTERNAL` — Внешняя камера

### 2. Уровень оборудования

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

Это одна из самых важных характеристик:

| Уровень | Уровень API | Возможности |
| --- | --- | --- |
| **LEGACY** | 21 | Ограниченная поддержка Camera2, обёртка над старым Camera API |
| **LIMITED** | 21 | Базовые функции Camera2, без ручных настроек |
| **FULL** | 21 | Полные ручные настройки, съёмка в RAW |
| **LEVEL_3** | 24 | Расширенные функции, такие как повторная обработка YUV |

### 3. Размер сенсора

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width и sensorSize.height дают размеры
```

Размер сенсора показывает, сколько пикселей содержит сенсор. Это отличается от разрешения изображения — у сенсора может быть больше пикселей, чем используется при одной съёмке.

### 4. Размер активной матрицы

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

Активная матрица — это фактическая область сенсора, используемая для съёмки изображений. Обычно она немного меньше пиксельной матрицы, потому что некоторые пиксели зарезервированы для калибровки.

### 5. Доступные возможности

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

Этот массив показывает, какие функции поддерживает камера:
- `BACKWARD_COMPATIBLE` — Базовая совместимость
- `MANUAL_SENSOR` — Ручное управление сенсором
- `MANUAL_POST_PROCESSING` — Ручная постобработка
- `RAW` — Поддержка съёмки в RAW
- `BURST_CAPTURE` — Серийная съёмка
- `YUV_REPROCESSING` — Повторная обработка YUV
- `DEPTH_OUTPUT` — Вывод глубины
- `CONSTRAINED_HIGH_SPEED_VIDEO` — Высокоскоростное видео

### 6. Форматы вывода

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

Карта конфигурации потоков содержит все форматы и размеры вывода, поддерживаемые камерой:
- `ImageFormat.JPEG` — Стандартный JPEG
- `ImageFormat.RAW_SENSOR` — Данные сенсора RAW
- `ImageFormat.YUV_420_888` — Формат YUV
- `ImageFormat.RAW10` — 10-битный RAW
- `ImageFormat.RAW12` — 12-битный RAW

### 7. Поддерживаемые размеры превью

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

Это даёт вам все доступные разрешения превью для камеры.

### 8. Поддерживаемые размеры снимков

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

Это доступные разрешения для съёмки неподвижных изображений.

### 9. Фокусные расстояния

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

Этот массив содержит фокусные расстояния (в миллиметрах) объектива. Несколько значений указывают на наличие оптического зума.

### 10. Диапазон фокусировки

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

Минимальное расстояние фокусировки показывает, насколько близко камера может фокусироваться. Меньшее значение означает лучшие макро-возможности.

## Практический пример

Давайте создадим более подробное приложение с информацией о камере:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Направление объектива
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Фронтальная"
        CameraCharacteristics.LENS_FACING_BACK -> "Основная"
        else -> "Внешняя"
    }
    
    // Уровень оборудования
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Неизвестно"
    }
    
    // Размер сенсора
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Размер активной матрицы
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Фокусные расстояния
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Неизвестно"
    
    // Доступные возможности
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Обратная совместимость"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Ручной сенсор"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "Съёмка в RAW"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Серийная съёмка"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "Повторная обработка YUV"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Вывод глубины"
            else -> "Неизвестная возможность"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Камера $cameraId ===")
    Log.d("CameraDetails", "Направление объектива: $lensFacingStr")
    Log.d("CameraDetails", "Уровень оборудования: $hardwareLevelStr")
    Log.d("CameraDetails", "Размер сенсора: $sensorSizeStr")
    Log.d("CameraDetails", "Активная матрица: $activeArrayStr")
    Log.d("CameraDetails", "Фокусные расстояния: $focalLengthsStr")
    Log.d("CameraDetails", "Возможности: ${capabilitiesList.joinToString(", ")}")
}
```

## Пример вывода

```
=== Камера 0 ===
Направление объектива: Основная
Уровень оборудования: FULL
Размер сенсора: 4032 x 3024
Активная матрица: 4000 x 3000
Фокусные расстояния: 2.4mm, 4.8mm
Возможности: Обратная совместимость, Ручной сенсор, Съёмка в RAW, Серийная съёмка
```

## Почему важны CameraCharacteristics

Перед открытием камеры или созданием сессии съёмки вы **должны** проверить CameraCharacteristics:

1. **Проверяйте возможности** — Не предполагайте, что функция поддерживается
2. **Выбирайте подходящую камеру** — Выбирайте по направлению объектива, уровню оборудования и т.д.
3. **Настраивайте вывод** — Используйте поддерживаемые разрешения и форматы
4. **Обрабатывайте различия устройств** — То, что работает на одном устройстве, может не работать на другом

## Изучите с помощью Android Camera Parameters

Откройте приложение Android Camera Parameters и просмотрите характеристики. Вы увидите сотни параметров, организованных по категориям:

- **Camera Info** — Базовая информация о камере
- **Sensor** — Характеристики сенсора
- **Lens** — Свойства объектива
- **Control** — Автоэкспозиция, автофокус, баланс белого
- **Scaler** — Размеры и форматы вывода
- **Flash** — Возможности вспышки
- **Statistics** — Статистика вывода

Это даёт вам полное представление о возможностях вашей камеры.

## Следующая глава

Теперь, когда вы понимаете CameraCharacteristics, вы готовы открыть свою первую камеру! В следующей главе мы:

1. Изучим CameraDevice
2. Откроем камеру с помощью CameraManager
3. Обработаем обратные вызовы состояния камеры
4. Поймём жизненный цикл камеры

## Резюме

CameraCharacteristics содержит всю информацию, необходимую для понимания возможностей камеры:

- **Направление объектива** — Фронтальная, основная или внешняя
- **Уровень оборудования** — LEGACY, LIMITED, FULL, LEVEL_3
- **Размер сенсора** — Физические размеры
- **Активная матрица** — Область съёмки
- **Фокусные расстояния** — Возможности объектива
- **Возможности** — Поддерживаемые функции
- **Форматы вывода** — Доступные форматы изображений

Всегда проверяйте CameraCharacteristics перед использованием камеры. Это гарантирует, что ваше приложение работает на разных устройствах.

В следующей главе мы откроем нашу первую камеру с помощью CameraDevice.
