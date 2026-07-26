---
sidebar_position: 8
title: "Глава 8: Открытие вашей первой камеры"
description: Узнайте, как открыть CameraDevice с помощью CameraManager и управлять жизненным циклом камеры с помощью колбэков состояния.
keywords: [CameraDevice, openCamera, жизненный цикл камеры, CameraManager]
---

Время открыть вашу первую камеру! Давайте узнаем о CameraDevice.

## Введение

До сих пор мы учились находить камеры и изучать их характеристики. Теперь мы сделаем следующий шаг: **открытие камеры**.

Открытие камеры даёт вам доступ к реальному аппаратному обеспечению камеры. После открытия вы можете создавать сессии захвата, отображать превью и снимать фотографии.

## Что такое CameraDevice?

CameraDevice представляет одну камеру, подключённую к Android-устройству. Он предоставляет методы для:
- Создания сессий захвата
- Съёмки статических изображений
- Запуска и остановки превью

Вы не создаёте CameraDevice напрямую. Вместо этого вы получаете его из CameraManager, вызывая `openCamera()`.

## Открытие камеры

Вот как открыть камеру:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // Камера готова к использованию
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // Камера была отключена
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // Произошла ошибка камеры
        camera.close()
    }
}, null)
```

Давайте разберём это по частям.

### StateCallback

CameraDevice использует паттерн колбэков, потому что открытие камеры является асинхронным. У колбэка есть три основных метода:

#### 1. `onOpened(camera: CameraDevice)`

Вызывается, когда камера успешно открыта. Здесь вы получаете экземпляр CameraDevice.

#### 2. `onDisconnected(camera: CameraDevice)`

Вызывается, когда камера отключена. Это может произойти, если камера используется другим приложением или если устройство выключается. Всегда закрывайте камеру в этом колбэке.

#### 3. `onError(camera: CameraDevice, error: Int)`

Вызывается при возникновении ошибки. Распространённые коды ошибок:
- `ERROR_CAMERA_IN_USE` — Камера уже используется
- `ERROR_MAX_CAMERAS_IN_USE` — Слишком много открытых камер
- `ERROR_CAMERA_DISABLED` — Камера отключена
- `ERROR_CAMERA_DEVICE` — Ошибка аппаратного обеспечения камеры
- `ERROR_CAMERA_SERVICE` — Ошибка сервиса камеры

### Handler

Третий параметр — это `Handler`. Если вы передадите `null`, колбэк будет выполняться в лупере вызывающего потока. Для обновления UI вы можете передать handler, который выполняется в основном потоке.

## Полный пример

Давайте создадим activity, которая открывает камеру:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraOpenActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            openCamera()
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        
        if (cameraIds.isEmpty()) {
            Toast.makeText(this, "Камеры недоступны", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // Открываем первую камеру
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Неверный ID камеры", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "Камера успешно открыта!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "Камера ${camera.id} открыта")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "Камера отключена", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "Камера используется"
                ERROR_MAX_CAMERAS_IN_USE -> "Слишком много открытых камер"
                ERROR_CAMERA_DISABLED -> "Камера отключена"
                ERROR_CAMERA_DEVICE -> "Ошибка аппаратного обеспечения камеры"
                ERROR_CAMERA_SERVICE -> "Ошибка сервиса камеры"
                else -> "Неизвестная ошибка"
            }
            
            Toast.makeText(this@CameraOpenActivity, "Ошибка камеры: $errorMessage", Toast.LENGTH_SHORT).show()
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
                openCamera()
            } else {
                Toast.makeText(this, "Требуется разрешение на камеру", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## Жизненный цикл камеры

Понимание жизненного цикла камеры имеет решающее значение:

1. **Открытие** — Вызовите `openCamera()`, чтобы получить CameraDevice
2. **Использование** — Создавайте сессии захвата, снимайте фотографии
3. **Закрытие** — Вызовите `close()`, когда закончите
4. **Освобождение** — Камера становится доступной для других приложений

Всегда закрывайте камеру при уничтожении activity, чтобы избежать утечек ресурсов.

## Лучшие практики

1. **Закрывайте после использования** — Всегда закрывайте камеру в `onDestroy()`
2. **Обрабатывайте ошибки** — Не игнорируйте колбэки `onError()`
3. **Проверяйте разрешения** — Всегда проверяйте разрешения перед открытием
4. **Используйте try-catch** — Обрабатывайте `SecurityException` и `IllegalArgumentException`
5. **Не храните ссылки** — Освобождайте ссылку на CameraDevice после закрытия

## Распространённые проблемы

### Камера используется
- Убедитесь, что другое приложение не использует камеру
- Проверьте, что вы правильно закрываете камеру

### Разрешение отклонено
- Проверьте разрешения в манифесте
- Убедитесь, что runtime-разрешение предоставлено

### ID камеры не найден
- Всегда получайте ID камер из `getCameraIdList()`
- Не хардкодьте ID камер

## Следующая глава

Теперь, когда вы можете открыть камеру, следующий шаг — отобразить превью. В следующей главе мы:

1. Узнаем о TextureView
2. Создадим Surface для превью
3. Создадим CameraCaptureSession
4. Отобразим превью камеры на экране

## Итог

Открытие камеры — это первый шаг к съёмке изображений:

1. Используйте `CameraManager.openCamera()`, чтобы получить CameraDevice
2. Обрабатывайте StateCallback для `onOpened()`, `onDisconnected()` и `onError()`
3. Всегда закрывайте камеру после использования
4. Следуйте жизненному циклу камеры: открытие → использование → закрытие → освобождение

В следующей главе мы создадим превью камеры, чтобы вы могли видеть то, что видит камера.
