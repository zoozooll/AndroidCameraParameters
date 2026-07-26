---
sidebar_position: 11
title: "Глава 11: RAW Capture и CaptureRequest"
description: Узнайте, как захватывать RAW-изображения и понимать CaptureRequest и CaptureResult для продвинутого управления камерой.
keywords: [RAW capture, CaptureRequest, CaptureResult, Camera2, ручное управление]
---

RAW-съёмка даёт вам полный контроль над обработкой изображений. Давайте изучим её вместе с CaptureRequest и CaptureResult.

## Введение

В предыдущей главе вы научились захватывать JPEG-фотографии. Теперь мы изучим:

1. **RAW-съёмка** — Захват необработанных данных сенсора
2. **CaptureRequest** — Настройка параметров камеры для каждого захвата
3. **CaptureResult** — Получение метаданных о завершённых захватах

## Что такое RAW?

RAW-изображения содержат все данные, захваченные сенсором до обработки ISP. Это означает:

- Шумоподавление не применяется
- Коррекция баланса белого отсутствует
- Повышение чёткости отсутствует
- Полный динамический диапазон

RAW-файлы больше по размеру, но предлагают непревзойдённую гибкость редактирования.

## Требования для RAW-съёмки

Для захвата RAW-изображений ваша камера должна:
1. Иметь аппаратный уровень **FULL** или **LEVEL_3**
2. Поддерживать возможность `RAW`

Проверьте CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## Захват RAW-изображений

Создайте ImageReader с RAW-форматом:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // или ImageFormat.RAW10/RAW12
    2
)
```

Затем добавьте как JPEG, так и RAW-поверхности в сессию захвата для одновременной съёмки:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest определяет все настройки для отдельного захвата. Вы можете настроить:

### Автоматические режимы
- `CONTROL_AF_MODE` — Режим автофокуса
- `CONTROL_AE_MODE` — Режим автоэкспозиции
- `CONTROL_AWB_MODE` — Режим автоматического баланса белого

### Ручное управление
- `SENSOR_SENSITIVITY` — Значение ISO
- `SENSOR_EXPOSURE_TIME` — Время экспозиции в наносекундах
- `LENS_FOCUS_DISTANCE` — Фокусное расстояние
- `LENS_APERTURE` — Диафрагма (если доступна)

### Настройки вывода
- `JPEG_QUALITY` — Качество сжатия JPEG
- `JPEG_ORIENTATION` — Ориентация изображения
- `COLOR_CORRECTION_MODE` — Режим цветокоррекции

### Создание CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// Установить автоматические режимы
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// Установить качество JPEG
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// Добавить цели
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// Построить запрос
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult содержит метаданные о завершённом захвате. Он включает:

- **Фактически использованные настройки** — Что камера действительно применила
- **Статистика** — Информация об экспозиции, фокусе и цвете
- **Временная метка** — Когда произошёл захват

### Получение CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // Получить фактическое время экспозиции
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // Получить фактическое ISO
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // Получить состояние фокуса
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // Получить состояние AE
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "Exposure: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### Распространённые ключи CaptureResult

| Ключ | Описание |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | Фактически использованное время экспозиции |
| `SENSOR_SENSITIVITY` | Фактически использованное ISO |
| `CONTROL_AF_STATE` | Состояние автофокуса |
| `CONTROL_AE_STATE` | Состояние автоэкспозиции |
| `CONTROL_AWB_STATE` | Состояние автоматического баланса белого |
| `SCALER_CROP_REGION` | Использованная область кадрирования |
| `COLOR_CORRECTION_GAINS` | Коэффициенты цветокоррекции |

## Полный пример RAW-съёмки

```kotlin
private fun configureDualCapture() {
    // Создать JPEG ImageReader
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // Создать RAW ImageReader
    val rawReader = ImageReader.newInstance(
        rawSize.width, rawSize.height,
        ImageFormat.RAW_SENSOR, 2
    )
    
    jpegReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveJpeg(image)
        image.close()
    }, null)
    
    rawReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveRaw(image)
        image.close()
    }, null)
    
    // Создать поверхности
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // Создать сессию захвата со всеми поверхностями
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // Добавить обе поверхности в качестве целей
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // Настроить режимы управления
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.JPEG_QUALITY, 100)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.capture(builder.build(), captureCallback, null)
    }
}

private fun saveRaw(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val fileName = "RAW_${System.currentTimeMillis()}.dng"
    val file = File(externalCacheDir, fileName)
    
    FileOutputStream(file).use { fos ->
        fos.write(bytes)
    }
}
```

## RAW против JPEG

| Характеристика | RAW | JPEG |
| --- | --- | --- |
| Размер файла | Большой (20-50MB) | Маленький (2-10MB) |
| Гибкость редактирования | Максимальная | Ограниченная |
| Шум | Сохраняется | Уменьшается |
| Баланс белого | Настраиваемый | Фиксированный |
| Динамический диапазон | Полный | Сжатый |

## Рекомендации

1. **Проверяйте поддержку RAW** — Всегда проверяйте перед попыткой RAW-съёмки
2. **Двойная съёмка** — Захватывайте одновременно и JPEG, и RAW для гибкости
3. **Закрывайте изображения** — Всегда вызывайте `image.close()` после обработки
4. **Обрабатывайте разные форматы** — RAW_SENSOR, RAW10 и RAW12 имеют разную структуру байтов

## Следующая глава

В следующей главе мы подведём итог тому, что вы узнали в Части III, и подготовимся к Части IV: Ручное управление камерой.

## Итоги

В этой главе вы узнали о:

1. **RAW-съёмка** — Захват необработанных данных сенсора для максимальной гибкости редактирования
2. **CaptureRequest** — Настройка параметров камеры для каждого захвата
3. **CaptureResult** — Получение метаданных о завершённых захватах

RAW-съёмка требует аппаратного уровня FULL или LEVEL_3. Вы можете захватывать одновременно и JPEG, и RAW, добавляя обе поверхности в сессию захвата.

CaptureRequest позволяет настраивать автофокус, автоэкспозицию, баланс белого и ручное управление, такое как ISO и время экспозиции. CaptureResult сообщает вам, какие настройки реально использовала камера.

В Части IV мы глубоко погрузимся в ручное управление камерой: ISO, экспозицию, фокус и баланс белого.
