---
sidebar_position: 6
title: "Глава 6: Список камер"
description: Напишите свою первую программу на Camera2, которая обнаруживает и перечисляет все доступные камеры на устройстве Android.
keywords: [список камер, CameraManager, перечисление камер, Android Camera2]
---

Пришло время написать вашу первую программу на Camera2! Давайте создадим приложение, которое выводит список всех камер.

## Введение

В этой главе вы напишете своё первое настоящее приложение на Camera2. Цель проста:

> Обнаружить все камеры на устройстве и отобразить информацию о них.

Это небольшой, но важный шаг. Прежде чем вы сможете использовать камеру, её нужно найти.

## Создание проекта

Давайте начнём с создания нового Android-проекта:

1. Откройте Android Studio
2. Создайте новый проект с "Empty Activity"
3. Назовите его "Camera2List"
4. Выберите Kotlin в качестве языка
5. Установите минимальный SDK на API 21 (Camera2 была представлена в API 21)

## Добавление разрешений

Добавьте разрешение камеры в `AndroidManifest.xml`:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## Разметка

Создайте простую разметку, которая отображает список камер. Обновите `activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Доступные камеры"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## Активити

Теперь давайте напишем основное активити. Здесь и будет код Camera2:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraListView: ListView
    private val cameraInfoList = mutableListOf<String>()

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraListView = findViewById(R.id.cameraListView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (hasCameraPermission()) {
            listCameras()
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

    private fun listCameras() {
        cameraInfoList.clear()
        
        try {
            val cameraIds = cameraManager.cameraIdList
            
            if (cameraIds.isEmpty()) {
                cameraInfoList.add("Камеры не найдены")
            } else {
                cameraInfoList.add("Найдено ${cameraIds.size} камер(ы):")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Фронтальная"
                        CameraCharacteristics.LENS_FACING_BACK -> "Тыловая"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "Внешняя"
                        else -> "Неизвестно"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "Неизвестно"
                    }
                    
                    cameraInfoList.add("Камера $index (ID: $cameraId)")
                    cameraInfoList.add("  - Объектив: $lensFacingStr")
                    cameraInfoList.add("  - Уровень аппаратной поддержки: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("Ошибка: разрешение на использование камеры отклонено")
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
                listCameras()
            } else {
                Toast.makeText(this, "Требуется разрешение на использование камеры", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("Разрешение на использование камеры отклонено")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## Что делает этот код

Давайте разберём, что происходит:

1. **Получение CameraManager** — Мы получаем системную службу CameraManager
2. **Проверка разрешений** — Мы проверяем, предоставлено ли разрешение на использование камеры
3. **Список камер** — Мы используем `getCameraIdList()` для получения всех идентификаторов камер
4. **Получение характеристик** — Для каждой камеры мы получаем её характеристики
5. **Отображение информации** — Мы показываем ID камеры, положение объектива и уровень аппаратной поддержки

## Ожидаемый результат

Когда вы запустите приложение, вы должны увидеть что-то вроде:

```
Найдено 3 камер(ы):

Камера 0 (ID: 0)
  - Объектив: Тыловая
  - Уровень аппаратной поддержки: FULL

Камера 1 (ID: 1)
  - Объектив: Фронтальная
  - Уровень аппаратной поддержки: LIMITED

Камера 2 (ID: 2)
  - Объектив: Тыловая
  - Уровень аппаратной поддержки: FULL
```

## Успех!

Вы только что написали свою первую программу на Camera2! Это может показаться простым, но это основа для всего, что мы будем делать дальше.

## Устранение неполадок

Если у вас возникнут проблемы:

1. **Разрешение отклонено** — Убедитесь, что вы предоставили разрешение на использование камеры
2. **Камеры не найдены** — Проверьте, есть ли на вашем устройстве камера
3. **SecurityException** — Убедитесь, что разрешения объявлены в манифесте
4. **Слишком низкий уровень API** — Camera2 требует API 21 или выше

## Что дальше?

Теперь, когда вы можете выводить список камер, следующим шагом будет более детальное изучение их характеристик. В следующей главе мы:

1. Изучим CameraCharacteristics
2. Узнаем о положении объектива
3. Поймём уровни аппаратной поддержки
4. Проверим информацию о сенсоре

## Резюме

В этой главе вы написали свою первую программу на Camera2. Приложение:

1. Запрашивает разрешения на использование камеры
2. Использует CameraManager для перечисления камер
3. Отображает ID камеры, положение объектива и уровень аппаратной поддержки

Это первый шаг к созданию полноценных приложений на Camera2. В следующей главе мы погрузимся глубже в CameraCharacteristics, чтобы понять, на что способна каждая камера.
