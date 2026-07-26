---
sidebar_position: 9
title: "Глава 9: Отображение превью камеры"
description: Узнайте, как отображать превью камеры с помощью TextureView, Surface и CameraCaptureSession в Camera2.
keywords: [превью камеры, TextureView, Surface, CameraCaptureSession, Camera2]
---

Наконец-то вы увидите превью камеры! Давайте соединим всё вместе.

## Введение

Открыть камеру — это хорошо, но вы пока ничего не видите. Чтобы отобразить то, что видит камера, вам нужно:

1. Создать TextureView для отображения превью
2. Получить Surface из TextureView
3. Создать CameraCaptureSession
4. Запустить превью

Здесь все части собираются воедино.

## TextureView

TextureView — это представление, которое может отображать `SurfaceTexture`. Оно идеально подходит для показа превью камеры, потому что:
- Его можно трансформировать (масштабировать, поворачивать)
- Оно поддерживает аппаратное ускорение
- Оно хорошо работает с анимациями и переходами

Добавьте TextureView в ваш макет:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</FrameLayout>
```

## Surface

Surface — это буфер, который может принимать данные изображения. Для отображения превью камеры:
1. Получите SurfaceTexture из TextureView
2. Создайте Surface из SurfaceTexture
3. Передайте Surface в CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession управляет процессом захвата. Он подключает устройство камеры к одному или нескольким Surface.

Чтобы создать сессию:
1. Подготовьте список Surface (для превью, захвата фото и т.д.)
2. Вызовите `createCaptureSession()` на CameraDevice
3. Обработайте обратный вызов

## Полный пример превью

Давайте создадим activity, которое показывает превью камеры:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Collections

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            // Обработка изменений размера при необходимости
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Вызывается при обновлении превью
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

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // Получить размеры превью
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // Выбрать размер превью
        previewSize = previewSizes?.get(0) // Использовать первый доступный размер

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
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
            Toast.makeText(this@CameraPreviewActivity, "Ошибка камеры", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val surface = Surface(texture)
        val surfaces = listOf(surface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@CameraPreviewActivity, "Ошибка конфигурации сессии", Toast.LENGTH_SHORT).show()
        }
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
        captureSession?.stopRepeating()
        captureSession?.close()
        cameraDevice?.close()
    }
}
```

## Как это работает

Давайте проследим поток:

1. **TextureView доступен** — вызывается `onSurfaceTextureAvailable()`
2. **Открытие камеры** — мы получаем CameraDevice
3. **Создание сессии захвата** — подключение камеры к Surface
4. **Запуск превью** — отправка повторяющегося запроса захвата

## CaptureRequest

CaptureRequest определяет, что камера должна захватывать:
- `TEMPLATE_PREVIEW` — для режима превью
- `TEMPLATE_STILL_CAPTURE` — для фотографий
- `TEMPLATE_RECORD` — для видеозаписи
- `TEMPLATE_VIDEO_SNAPSHOT` — для снимка во время видео

## Цикл превью

Когда вы вызываете `setRepeatingRequest()`, камера непрерывно отправляет кадры на Surface. Это создаёт живое превью.

## Важные замечания

1. **Surface должен быть доступен** — дождитесь `onSurfaceTextureAvailable()` перед открытием камеры
2. **Закрывайте ресурсы** — всегда закрывайте сессию захвата и устройство камеры
3. **Обрабатывайте ориентацию** — превью может потребоваться поворот в зависимости от ориентации устройства
4. **Размер имеет значение** — выбирайте размер превью, соответствующий размерам вашего TextureView

## Успех!

Когда вы запустите это приложение, вы должны увидеть живое превью камеры на экране. Поздравляем! Вы создали своё первое приложение с превью Camera2.

## Следующая глава

Теперь, когда вы можете отображать превью, следующий шаг — захват фотографий. В Части III мы узнаем о:

1. ImageReader для захвата фотографий
2. Захват JPEG и RAW
3. CaptureRequest и CaptureResult

## Резюме

Отображение превью камеры включает:

1. **TextureView** — компонент UI для отображения превью
2. **Surface** — буфер, который принимает кадры камеры
3. **CameraCaptureSession** — управляет процессом захвата
4. **CaptureRequest** — определяет, что захватывать
5. **setRepeatingRequest()** — запускает непрерывный цикл превью

Вы завершили Часть II этой серии. Теперь вы можете:
- Обнаруживать камеры
- Изучать характеристики камеры
- Открывать камеру
- Отображать превью

В Части III мы узнаем, как захватывать фотографии с помощью Camera2.
