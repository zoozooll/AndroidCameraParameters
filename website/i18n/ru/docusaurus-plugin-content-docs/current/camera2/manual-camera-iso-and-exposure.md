---
sidebar_position: 12
title: "Глава 12: Ручная камера — ISO и экспозиция"
description: Узнайте, как вручную управлять ISO и выдержкой для профессиональной фотосъёмки с помощью Camera2.
keywords: [ISO, выдержка, ручная камера, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

Ручное управление — это то, где проявляется вся мощь Camera2. Давайте изучим ISO и экспозицию.

## Введение

До сих пор мы использовали автоматическое управление. Теперь мы возьмём полный контроль над:

1. **ISO** — чувствительность сенсора к свету
2. **Выдержка** — как долго сенсор собирает свет

Эти два параметра напрямую влияют на яркость и качество изображения.

## Что такое ISO?

ISO измеряет чувствительность сенсора к свету. Низкий ISO означает:
- Меньшая чувствительность к свету
- Меньше шумов
- Лучшее качество изображения

Высокий ISO означает:
- Большая чувствительность к свету
- Больше шумов (зернистость)
- Худшее качество изображения

Распространённые значения ISO: 100, 200, 400, 800, 1600, 3200, 6400

## Что такое выдержка?

Выдержка (также называемая скоростью затвора) — это время, в течение которого сенсор собирает свет. Короткая выдержка означает:
- Меньше захваченного света
- Замораживает движение
- Быстрое действие

Длинная выдержка означает:
- Больше захваченного света
- Размытие движения
- Лучшая работа при низком освещении

Выдержка измеряется в секундах или долях секунды:
- 1/1000с — быстрое действие
- 1/125с — нормально
- 1/30с — медленно
- 1с — длинная выдержка

## Требования для ручного управления

Чтобы использовать ручное управление, ваша камера должна иметь:
1. Уровень оборудования **FULL** или **LEVEL_3**
2. Возможность **MANUAL_SENSOR**

Проверьте CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## Получение поддерживаемых диапазонов

Перед установкой ручных значений проверьте, что поддерживает камера:

```kotlin
// Получить диапазон ISO
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// Получить диапазон выдержки
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## Установка ручного ISO и выдержки

Чтобы использовать ручное управление, вам нужно:
1. Отключить автоэкспозицию (AE)
2. Установить ручной ISO
3. Установить ручную выдержку

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Отключить AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// Установить ручной ISO
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// Установить ручную выдержку (в наносекундах)
// 1/125с = 8 000 000нс
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// Добавить цель
captureRequestBuilder.addTarget(surface)

// Начать предпросмотр с ручным управлением
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## Экспозиционный треугольник

ISO, выдержка и диафрагма образуют «экспозиционный треугольник»:

- **ISO** — чувствительность к свету
- **Выдержка** — длительность захвата света
- **Диафрагма** — количество поступающего света (редко регулируется на телефонах)

Изменение одного параметра влияет на другие. Например:
- Если увеличить ISO, можно использовать более короткую выдержку
- Если уменьшить выдержку, может потребоваться увеличить ISO

## Полный пример ручного управления

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60с

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO: 100-3200 с шагом 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // Выдержка: 1/1000с до 1/10с
        exposureSeekBar.progress = 5
        
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Преобразовать прогресс в выдержку (в наносекундах)
                // 0: 1/1000с = 1 000 000нс
                // 9: 1/10с = 100 000 000нс
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
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
            
            // Отключить AE для ручного управления
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // Установить ручной ISO
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // Установить ручную выдержку
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Оставить AWB включённым
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (остальной код настройки камеры)
    
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
        }
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
    
    // ... (открытие камеры, создание сессии и т.д.)
}
```

## Разметка

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="ISO:"/>
            
            <SeekBar
                android:id="@+id/isoSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/isoValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="100"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="16dp">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="Выд:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60с"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## Лучшие практики

1. **Проверяйте поддержку** — всегда проверяйте поддержку ручного сенсора
2. **Начинайте с AE** — позвольте автоэкспозиции установить начальные значения, затем переключайтесь на ручное управление
3. **Избегайте экстремального ISO** — высокий ISO добавляет шумы
4. **Используйте короткую выдержку** — избегайте размытия движения, когда это возможно
5. **Следите за гистограммой** — используйте CaptureResult для проверки экспозиции

## Следующая глава

В следующей главе мы изучим ручную фокусировку и управление балансом белого.

## Резюме

Ручное управление ISO и выдержкой даёт вам профессиональный уровень фотосъёмки:

1. **ISO** — управляет чувствительностью сенсора (диапазон 100-3200+)
2. **Выдержка** — управляет тем, как долго собирается свет (в наносекундах)
3. **Отключите AE** — необходимо выключить автоэкспозицию для ручного управления
4. **Проверяйте диапазоны** — всегда проверяйте поддерживаемые диапазоны ISO и выдержки

Экспозиционный треугольник (ISO, выдержка, диафрагма) определяет яркость и качество изображения. В следующей главе мы рассмотрим ручную фокусировку и баланс белого.
