---
sidebar_position: 14
title: "Глава 14: Профессиональные функции — Высокоскоростное видео"
description: Узнайте, как снимать высокоскоростное видео и работать с мультикамерными установками в Camera2.
keywords: [высокоскоростное видео, мультикамера, Camera2, ограниченная высокая скорость, логическая камера]
---

Профессиональные функции открывают новые творческие возможности. Давайте рассмотрим высокоскоростное видео и мультикамеру.

## Введение

Camera2 поддерживает множество профессиональных функций помимо базовой съёмки фотографий. В этой главе мы узнаем о:

1. **Высокоскоростное видео** — Съёмка замедленного видео
2. **Мультикамера** — Работа с логическими и физическими камерами

## Высокоскоростное видео

Высокоскоростное видео позволяет снимать видео с частотой кадров выше стандартных 30 кадров/с:
- 120 кадров/с — Плавное замедление
- 240 кадров/с — Стандартное замедление
- 480 кадров/с — Экстремальное замедление
- 960 кадров/с — Суперзамедление

### Требования

Для съёмки высокоскоростного видео ваша камера должна:
1. Поддерживать возможность `CONSTRAINED_HIGH_SPEED_VIDEO`
2. Иметь соответствующий уровень оборудования

Проверьте CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### Размеры высокоскоростного видео

Высокоскоростное видео использует разрешения, отличные от стандартного видео:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### Съёмка высокоскоростного видео

Высокоскоростное видео требует специальной сессии захвата:

```kotlin
// Создаём высокоскоростную сессию захвата
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "Сбой высокоскоростной сессии", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### Запуск высокоскоростного предпросмотра

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // Получаем поддерживаемые диапазоны fps для высокой скорости
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // Находим диапазон высокой скорости (например, 120fps)
    val highSpeedFpsRange = fpsRanges?.find { it.upper >= 120 }
    
    captureRequestBuilder?.apply {
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange)
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.setRepeatingRequest(builder.build(), null, null)
    }
}
```

## Мультикамера

Современные телефоны имеют несколько камер. Camera2 обрабатывает их как:
- **Физические камеры** — Отдельные сенсоры камеры
- **Логические камеры** — Комбинации физических камер

### Логические и физические камеры

| Тип | Описание |
| --- | --- |
| **Физическая** | Один сенсор камеры (широкоугольный, телефото, сверхширокоугольный) |
| **Логическая** | Виртуальная камера, объединяющая несколько физических камер |

### Определение типов камер

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Проверяем, является ли камера логической
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// Получаем ID физических камер (для логических камер)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### Переключение между камерами

Чтобы переключить камеры, вам нужно:
1. Закрыть текущую камеру
2. Открыть новую камеру
3. Создать новую сессию захвата

```kotlin
private fun switchCamera(newCameraId: String) {
    // Закрываем текущую камеру
    captureSession?.close()
    cameraDevice?.close()
    
    // Открываем новую камеру
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### Параллельная камера

Некоторые устройства поддерживают одновременное открытие нескольких камер:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## Расширение камеры

Расширение камеры позволяет использовать специфичные для производителя функции камеры:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// Получаем доступные расширения
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

Распространённые расширения:
- `EXTENSION_BOKEH` — Портретный режим
- `EXTENSION_HDR` — HDR-режим
- `EXTENSION_NIGHT` — Ночной режим
- `EXTENSION_AUTO` — Автоматический режим

## Пример мультикамеры

```kotlin
class MultiCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_camera)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupCameraSelector()
    }

    private fun setupCameraSelector() {
        val cameraIds = cameraManager.cameraIdList
        val spinner = findViewById<Spinner>(R.id.cameraSpinner)
        
        val cameraNames = cameraIds.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Фронтальная камера ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "Основная камера ($id)"
                else -> "Камера $id"
            }
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newCameraId = cameraIds[position]
                if (newCameraId != currentCameraId) {
                    switchCamera(newCameraId)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun switchCamera(newCameraId: String) {
        captureSession?.close()
        cameraDevice?.close()
        currentCameraId = newCameraId
        openCamera()
    }

    private fun openCamera() {
        try {
            cameraManager.openCamera(currentCameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "Сбой сессии", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun startPreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(surface)
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }
}
```

## Рекомендации

1. **Проверяйте возможности** — Всегда проверяйте перед использованием профессиональных функций
2. **Обрабатывайте переходы** — Плавно закрывайте/открывайте при переключении камер
3. **Управляйте ресурсами** — Высокоскоростное видео потребляет больше ресурсов
4. **Грациозно отказывайте** — Предлагайте альтернативы, когда функции недоступны

## Следующая глава

В заключительной главе мы рассмотрим Энциклопедию CameraCharacteristics — глубокое погружение в наиболее важные параметры камеры.

## Резюме

Профессиональные функции расширяют ваши творческие возможности:

1. **Высокоскоростное видео** — Съёмка замедленного видео на 120-960 кадров/с
2. **Мультикамера** — Работа с логическими и физическими камерами
3. **Расширение камеры** — Использование специфичных для производителя функций
4. **Параллельная камера** — Одновременное открытие нескольких камер

В следующей главе мы глубоко погрузимся в CameraCharacteristics — энциклопедию параметров камеры.
