---
sidebar_position: 13
title: "Глава 13: Ручная камера — Фокус и баланс белого"
description: Узнайте, как вручную управлять расстоянием фокусировки и балансом белого для профессиональной фотографии с помощью Camera2.
keywords: [фокус, баланс белого, ручная камера, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

Полное ручное управление с фокусом и балансом белого.

## Введение

В предыдущей главе вы научились управлять ISO и выдержкой. Теперь мы добавим:

1. **Фокус** — ручное управление расстоянием фокусировки
2. **Баланс белого** — ручное управление цветовой температурой

С ними у вас будет полный творческий контроль над фотографиями.

## Ручной фокус

Фокус определяет, какая часть сцены будет резкой. Ручной фокус позволяет:
- Фокусироваться на конкретных объектах
- Создавать намеренное размытие (боке)
- Обеспечивать точную фокусировку в макрофотографии

### Режимы фокусировки

Camera2 поддерживает несколько режимов фокусировки:

| Режим | Описание |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Ручной фокус |
| `CONTROL_AF_MODE_AUTO` | Одиночная автофокусировка |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Непрерывная автофокусировка для фото |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Непрерывная автофокусировка для видео |
| `CONTROL_AF_MODE_MACRO` | Макрофокус |

### Расстояние фокусировки

Расстояние фокусировки измеряется в диоптриях (1/метр). Значение 0 означает бесконечность.

```kotlin
// Получить диапазон расстояний фокусировки
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### Установка ручного фокуса

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Отключить AF для ручного фокуса
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// Установить расстояние ручного фокуса (в диоптриях)
// 0.0 = бесконечность
// 1.0 = 1 метр
// 2.0 = 0.5 метра
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Области фокусировки

Вы также можете указать области AF для выборочной автофокусировки:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // Центр X в координатах сенсора (0-1000)
        centerY,    // Центр Y в координатах сенсора (0-1000)
        width,      // Ширина области
        height,     // Высота области
        weight      // Приоритет (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## Ручной баланс белого

Баланс белого (ББ) регулирует цветовую температуру изображения. Разные источники света имеют разную цветовую температуру:

- **Дневной свет** — ~5500K (голубоватый)
- **Облачно** — ~6500K (холоднее)
- **Лампа накаливания** — ~2800K (тёплый/жёлтый)
- **Люминесцентная лампа** — ~4000K (зеленоватый)

### Режимы баланса белого

| Режим | Описание |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Ручной баланс белого |
| `CONTROL_AWB_MODE_AUTO` | Автоматический баланс белого |
| `CONTROL_AWB_MODE_INCANDESCENT` | Накальное освещение |
| `CONTROL_AWB_MODE_FLUORESCENT` | Люминесцентное освещение |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Тёплая люминесцентная лампа |
| `CONTROL_AWB_MODE_DAYLIGHT` | Дневной свет |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Облачно |

### Установка ручного баланса белого

Чтобы установить ручной баланс белого, нужно задать коэффициенты цветокоррекции:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Отключить AWB для ручного управления
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// Установить коэффициенты цветокоррекции (R, G, B)
// Значения нормализованы (1.0 = без коррекции)
// Более высокие значения делают соответствующий цвет более заметным
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Цветовая температура

Вы также можете установить баланс белого с помощью цветовой температуры:

```kotlin
// Получить поддерживаемый диапазон цветовой температуры
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// Установить цветовую температуру (в Кельвинах)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // Дневной свет
```

## Полный пример ручной камеры

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // Ручные настройки
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s
    private var currentFocus = 0.0f // Бесконечность
    private var currentWhiteBalance = 5500 // Дневной свет

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // Управление ISO
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Управление выдержкой
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Управление фокусом
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Преобразовать прогресс (0-100) в расстояние фокуса (0.0 до 2.0 диоптрий)
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Управление балансом белого
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (лампа накаливания) до 6500K (облачно)
                currentWhiteBalance = 2800 + (progress * 37)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // Ручная выдержка
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Ручной фокус
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // Ручной баланс белого
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (код настройки камеры)
}
```

## Рекомендации

1. **Проверяйте поддержку** — не все камеры поддерживают ручной фокус или ББ
2. **Начинайте с автоматического** — пусть автонастройки установят базовые значения
3. **Используйте фокус-пикинг** — добавляйте визуальную обратную связь для точности фокуса
4. **Калибруйте ББ** — используйте серую карту для точного баланса белого
5. **Комбинируйте настройки** — ручные параметры работают лучше вместе

## Следующая глава

В следующей главе мы рассмотрим профессиональные функции, такие как скоростная съёмка видео и мультикамера.

## Итоги

Ручное управление фокусом и балансом белого завершает ваш инструментарий камеры:

1. **Фокус** — управляйте тем, что будет резким на изображении
2. **Баланс белого** — управляйте цветовой температурой
3. **Ручные режимы** — отключите AF/AWB и задавайте значения напрямую
4. **Области фокусировки** — выделяйте определённые области для автофокуса

Контролируя ISO, выдержку, фокус и баланс белого, вы можете создавать фотографии профессионального качества. В Части V мы рассмотрим расширенные функции, такие как скоростная съёмка видео и поддержка мультикамеры.
