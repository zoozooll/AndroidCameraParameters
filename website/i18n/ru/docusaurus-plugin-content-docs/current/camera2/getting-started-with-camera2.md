---
sidebar_position: 5
title: "Глава 5: Начало работы с Camera2"
description: Узнайте о CameraManager — точке входа в Android Camera2 API, которая позволяет перечислять камеры и получать доступ к их характеристикам.
keywords: [CameraManager, Camera2 API, Android камера, перечисление камер]
---

Добро пожаловать в практическую часть этой серии. Начнём с основ: CameraManager.

## Введение

Прежде чем вы сможете использовать какую-либо камеру, вам нужен способ её обнаружения и доступа к ней. Именно для этого существует **CameraManager**.

CameraManager — это шлюз к Camera2 API. Это первый класс, который вы будете использовать в любом приложении Camera2.

## Что такое CameraManager?

CameraManager — это системный сервис, который управляет всеми устройствами камеры на Android-устройстве. Думайте о нём как о каталоге или реестре камер.

Его основные обязанности:
1. **Перечисление камер** — вывод списка всех доступных камер
2. **Получение характеристик камеры** — получение подробной информации о каждой камере
3. **Открытие камер** — создание CameraDevice для съёмки

## Получение CameraManager

В Android системные сервисы получают через `Context`. Вот как получить CameraManager:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

Вот и всё. Одна строка кода для получения доступа ко всем камерам на устройстве.

## Сначала разрешения

Перед использованием CameraManager необходимо запросить разрешения на камеру. Добавьте их в ваш `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

И запросите разрешение во время выполнения в вашей Activity:

```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED
) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        CAMERA_PERMISSION_REQUEST
    )
}
```

Всегда проверяйте разрешения перед доступом к камере.

## Методы CameraManager

В CameraManager есть три основных метода, которые вы будете использовать:

### 1. `getCameraIdList()`

Возвращает массив строк с идентификаторами камер. Каждый идентификатор представляет устройство камеры.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Найдена камера: $id")
}
```

Это может вывести:
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

Возвращает объект `CameraCharacteristics`, содержащий все сведения о конкретной камере.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics содержит сотни параметров, описывающих возможности камеры.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

Открывает камеру и возвращает `CameraDevice` через обратный вызов. Мы подробно рассмотрим это позже.

## Повторно об идентификаторах камер

Помните из Главы 4, что Android присваивает камерам числовые идентификаторы. Идентификаторы не гарантируют согласованность между устройствами или даже между перезагрузками.

Распространённые шаблоны:
- **Camera 0** — обычно задняя широкая камера
- **Camera 1** — часто фронтальная камера
- **Camera 2** — обычно сверхширокоугольная или телеобъектив
- Более высокие номера — дополнительные камеры (макро, глубина и т.д.)

Но **никогда не предполагайте** значение идентификатора камеры. Всегда проверяйте характеристики камеры, чтобы определить:
- положение объектива (фронтальный/задний/внешний)
- фокусное расстояние
- возможности

## Почему CameraManager важен

CameraManager — это основа для всего, что мы будем делать с Camera2:

1. **Обнаружение** — прежде чем использовать камеру, её нужно найти
2. **Информация** — прежде чем открыть камеру, нужно знать её возможности
3. **Доступ** — CameraManager предоставляет единственный способ открыть устройство камеры

## Простой пример

Давайте соберём всё вместе в простом примере:

```kotlin
class CameraDiscoveryActivity : AppCompatActivity() {
    
    private lateinit var cameraManager: CameraManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            discoverCameras()
        } else {
            requestCameraPermission()
        }
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }
    
    private fun discoverCameras() {
        val cameraIds = cameraManager.cameraIdList
        
        Log.d("CameraDiscovery", "Найдено камер: ${cameraIds.size}")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Фронтальная"
                CameraCharacteristics.LENS_FACING_BACK -> "Задняя"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Внешняя"
                else -> "Неизвестно"
            }
            
            Log.d("CameraDiscovery", "Камера $cameraId: $lensFacingStr")
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                discoverCameras()
            } else {
                Toast.makeText(this, "Требуется разрешение на камеру", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

Эта простая Activity обнаруживает все камеры и записывает в журнал их идентификаторы и направления объективов.

## Основные выводы

- **CameraManager** — точка входа в Camera2
- Используйте `getCameraIdList()` для поиска всех камер
- Используйте `getCameraCharacteristics()` для получения подробной информации
- Всегда сначала запрашивайте разрешения на камеру
- Никогда не предполагайте значения идентификаторов камер — проверяйте характеристики

## Следующая глава

Теперь, когда вы понимаете CameraManager, пришло время написать вашу первую настоящую программу на Camera2. В следующей главе мы:

1. Создадим простое Android-приложение
2. Выведем список всех доступных камер
3. Отобразим информацию о камерах пользователю

Вы напишете свой первый код на Camera2 и увидите реальные результаты!

## Резюме

CameraManager — это основа Camera2. Он предоставляет доступ к:
- перечислению камер
- характеристикам камер
- открытию камер

С помощью CameraManager вы можете обнаружить доступные камеры и узнать об их возможностях перед их открытием.

В следующей главе мы напишем нашу первую программу на Camera2, которая выводит список всех камер на устройстве.
