---
sidebar_position: 15
title: "Глава 15: Энциклопедия CameraCharacteristics"
description: Полное руководство по наиболее важным характеристикам Camera2, включая их значение, причину существования и способы использования.
keywords: [CameraCharacteristics, параметры камеры, возможности камеры, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

Добро пожаловать в энциклопедию CameraCharacteristics — ваше руководство по пониманию каждого параметра камеры.

## Введение

CameraCharacteristics содержит сотни параметров, описывающих возможности камеры. В этой главе мы подробно рассмотрим самые важные из них:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — Что может делать камера?
2. `REQUEST_AVAILABLE_CAPABILITIES` — Какие функции доступны?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — Каков размер сенсора?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — Насколько большой зум?
5. `CONTROL_AE_AVAILABLE_MODES` — Какие режимы экспозиции?

И многие другие...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**Что это означает?**  
Это самая важная характеристика. Она определяет общий уровень возможностей устройства камеры.

**Зачем она существует?**  
Разные Android-устройства имеют разные возможности камеры. Этот параметр помогает приложениям понять, что они могут делать.

**Поддерживаемые значения:**

| Значение | Уровень API | Описание |
| --- | --- | --- |
| `LEGACY` | 21 | Старые устройства, Camera2 API является обёрткой над старым Camera API |
| `LIMITED` | 21 | Базовые функции Camera2, без ручных настроек |
| `FULL` | 21 | Полные ручные настройки, съёмка в RAW, серийная съёмка |
| `LEVEL_3` | 24 | Расширенные функции, такие как повторная обработка YUV, 10-битный HDR |

**Как используется?**  
Проверяйте это перед попыткой выполнения любых расширенных операций:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // Ограниченная функциональность
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // Только базовые функции
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // Доступны полные ручные настройки
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // Доступны расширенные функции
    }
}
```

**Как проверить с помощью Android Camera Parameters:**  
Откройте приложение и найдите «Hardware Level» в разделе Camera Info.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**Что это означает?**  
Этот массив перечисляет все возможности, поддерживаемые камерой.

**Зачем она существует?**  
Даже на одном уровне оборудования разные устройства могут поддерживать разные функции.

**Распространённые возможности:**

| Возможность | Описание |
| --- | --- |
| `BACKWARD_COMPATIBLE` | Базовый режим совместимости |
| `MANUAL_SENSOR` | Ручное управление ISO и экспозицией |
| `MANUAL_POST_PROCESSING` | Ручная цветокоррекция и шумоподавление |
| `RAW` | Съёмка изображений в RAW |
| `BURST_CAPTURE` | Высокоскоростная серийная съёмка |
| `YUV_REPROCESSING` | Повторная обработка изображений YUV |
| `DEPTH_OUTPUT` | Вывод карты глубины |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | Высокоскоростная видеосъёмка |
| `LOGICAL_MULTI_CAMERA` | Логическая камера, объединяющая несколько физических камер |
| `CONCURRENT_CAMERA` | Несколько камер можно открыть одновременно |
| `CAMERA_EXTENSION` | Расширения производителя (портрет, ночной режим) |

**Как используется?**  
Проверяйте возможности перед использованием функции:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // Включить съёмку в RAW
}
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Available Capabilities» в разделе Camera Info.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**Что это означает?**  
Активная матрица — это фактическая область сенсора, используемая для съёмки изображений.

**Зачем она существует?**  
Сенсор может иметь пиксели по краям, зарезервированные для калибровки. Активная матрица представляет собой используемую область.

**Как используется?**  
Это показывает максимальное разрешение, доступное для съёмки:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Активная матрица: ${width}x$height")
```

**Связанные характеристики:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — Общее количество пикселей на сенсоре (может быть больше активной матрицы)
- `SENSOR_INFO_SENSOR_SIZE` — Физические размеры в миллиметрах

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Active Array Size» и «Sensor Size» в разделе Sensor.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**Что это означает?**  
Максимальный коэффициент цифрового зума, поддерживаемый камерой.

**Зачем она существует?**  
Цифровой зум обрезает и увеличивает изображение, снижая качество. Знание максимального значения помогает управлять ожиданиями пользователя.

**Как используется?**  
Установите уровень зума в запросах съёмки:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// Установить зум (1.0 = без зума, maxZoom = максимальный зум)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**Связанные характеристики:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — Физические фокусные расстояния (для оптического зума)

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Max Digital Zoom» в разделе Scaler.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**Что это означает?**  
Доступные режимы автоэкспозиции.

**Зачем она существует?**  
Разные устройства поддерживают разные стратегии AE.

**Распространённые режимы:**

| Режим | Описание |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | Ручное управление экспозицией |
| `CONTROL_AE_MODE_ON` | Автоэкспозиция |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | Автоэкспозиция с постоянно включённой вспышкой |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | Автоэкспозиция с автоматической вспышкой |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | Автоэкспозиция с уменьшением эффекта красных глаз |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | Автоэкспозиция с внешней вспышкой |

**Как используется?**  
Установите режим AE в запросах съёмки:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «AE Available Modes» в разделе Control.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**Что это означает?**  
Доступные режимы автофокусировки.

**Зачем она существует?**  
Разные стратегии фокусировки для разных сценариев.

**Распространённые режимы:**

| Режим | Описание |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Ручная фокусировка |
| `CONTROL_AF_MODE_AUTO` | Одиночная автофокусировка |
| `CONTROL_AF_MODE_MACRO` | Макрофокусировка |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Непрерывная автофокусировка для видео |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Непрерывная автофокусировка для фото |
| `CONTROL_AF_MODE_EDGE` | Краевая автофокусировка |
| `CONTROL_AF_MODE_FIXED` | Фиксированная фокусировка (без AF) |

**Как используется?**  
Установите режим AF в зависимости от вашего сценария использования:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «AF Available Modes» в разделе Control.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**Что это означает?**  
Доступные режимы автоматического баланса белого.

**Распространённые режимы:**

| Режим | Описание |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Ручной баланс белого |
| `CONTROL_AWB_MODE_AUTO` | Автоматический |
| `CONTROL_AWB_MODE_INCANDESCENT` | Лампа накаливания |
| `CONTROL_AWB_MODE_FLUORESCENT` | Люминесцентное освещение |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Тёплое люминесцентное |
| `CONTROL_AWB_MODE_DAYLIGHT` | Дневной свет |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Облачно |

**Как проверить с помощью Android Camera Parameters:**  
Найдите «AWB Available Modes» в разделе Control.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**Что это означает?**  
Доступные фокусные расстояния для объектива(ов).

**Зачем она существует?**  
Несколько значений указывают на наличие оптического зума.

**Как используется?**  
Определите, какие объективы доступны:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Фокусное расстояние: ${fl}мм")
}
```

**Распространённые фокусные расстояния:**
- 2.4мм — Широкоугольный (распространённый)
- 4.8мм — Телеобъектив (2x оптический зум)
- 1.8мм — Сверхширокоугольный

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Available Focal Lengths» в разделе Lens.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**Что это означает?**  
Минимальное расстояние, на которое может фокусироваться объектив.

**Зачем она существует?**  
Более низкие значения означают лучшую макроспособность.

**Как используется?**  
Проверьте, возможна ли макросъёмка:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// Значение 0.1м (10см) или меньше указывает на хорошую макроспособность
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Minimum Focus Distance» в разделе Lens.

---

## 10. FLASH_INFO_AVAILABLE

**Что это означает?**  
Есть ли у камеры вспышка.

**Зачем она существует?**  
Не все камеры имеют вспышку (особенно фронтальные).

**Как используется?**  
Проверьте перед использованием вспышки:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // Включить функции вспышки
}
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Flash Available» в разделе Flash.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**Что это означает?**  
Поддерживаемые минимальное и максимальное время экспозиции.

**Зачем она существует?**  
Определяет способность к съёмке при низкой освещённости и способность замораживать движение.

**Как используется?**  
Проверьте диапазон экспозиции для ручного управления:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// Преобразовать в секунды для отображения
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Exposure Time Range» в разделе Sensor.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**Что это означает?**  
Поддерживаемые минимальные и максимальные значения ISO.

**Зачем она существует?**  
Определяет способность к съёмке при низкой освещённости и уровень шумовых характеристик.

**Как используется?**  
Проверьте диапазон ISO для ручного управления:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Sensitivity Range» в разделе Sensor.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**Что это означает?**  
Все поддерживаемые размеры и форматы вывода.

**Зачем она существует?**  
Определяет, какие разрешения и форматы вы можете использовать.

**Как используется?**  
Получите поддерживаемые размеры для разных сценариев использования:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// Размеры превью
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// Размеры фото
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// Размеры видео
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// Размеры RAW
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Preview Sizes», «Picture Sizes» и т.д. в разделе Scaler.

---

## 14. LENS_FACING

**Что это означает?**  
В каком направлении обращён объектив.

**Зачем она существует?**  
Определяет, является ли камера фронтальной, основной или внешней.

**Значения:**
- `LENS_FACING_FRONT` — Фронтальная камера (селфи)
- `LENS_FACING_BACK` — Основная камера
- `LENS_FACING_EXTERNAL` — Внешняя камера

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Lens Facing» в разделе Camera Info.

---

## 15. CONTROL_MAX_REGIONS_AE

**Что это означает?**  
Максимальное количество областей замера AE.

**Зачем она существует?**  
Определяет, насколько точным может быть замер экспозиции.

**Как используется?**  
Ограничьте количество создаваемых областей AE:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// Создавайте не более maxAERegions
```

**Как проверить с помощью Android Camera Parameters:**  
Найдите «Max Regions AE» в разделе Control.

---

## Заключение

CameraCharacteristics — это ваше окно в возможности камеры. Понимая эти параметры, вы можете:

1. **Создавать приложения, не зависящие от устройства** — Проверяйте возможности перед использованием функций
2. **Обеспечивать лучший пользовательский опыт** — Показывайте только доступные функции
3. **Оптимизировать производительность** — Выбирайте подходящие разрешения и форматы
4. **Создавать профессиональные приложения** — Раскрывайте полный потенциал камеры

## Как узнать больше

1. **Приложение Android Camera Parameters** — Изучайте реальные данные с вашего устройства
2. **Документация Android** — Читайте официальную документацию CameraCharacteristics
3. **Экспериментируйте** — Пишите небольшие тестовые приложения, чтобы опробовать разные параметры
4. **Исходный код** — Изучайте исходный код Camera2 для более глубокого понимания

## Резюме

В этой главе рассмотрены наиболее важные характеристики CameraCharacteristics:

1. **Уровень оборудования** — Общие возможности
2. **Возможности** — Доступные конкретные функции
3. **Активная матрица** — Разрешение сенсора
4. **Цифровой зум** — Возможности зума
5. **Режимы AE/AF/AWB** — Режимы автоматического управления
6. **Фокусные расстояния** — Возможности объектива
7. **Расстояние фокусировки** — Макроспособность
8. **Вспышка** — Наличие вспышки
9. **Диапазон экспозиции/ISO** — Пределы ручного управления
10. **Конфигурация потоков** — Поддерживаемые размеры и форматы

С этими знаниями вы готовы создавать расширенные приложения Camera2!

---

## Заключительные слова

Поздравляем! Вы завершили эту серию по Android Camera2. Теперь вы понимаете:

- **Как работают камеры смартфонов** — Объективы, сенсоры, ISP
- **Как работает Camera2** — CameraManager, CameraDevice, CaptureSession
- **Как снимать фотографии** — JPEG, RAW, ImageReader
- **Как управлять камерой** — ISO, экспозиция, фокус, баланс белого
- **Профессиональные функции** — Высокоскоростное видео, мультикамера
- **Характеристики камеры** — Энциклопедия возможностей камеры

Приложение Android Camera Parameters — отличный инструмент для продолжения обучения. Изучайте возможности вашего устройства и экспериментируйте с разными настройками.

Удачи в кодировании! 📸