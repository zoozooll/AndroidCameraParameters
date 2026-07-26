---
sidebar_position: 10
title: "Глава 10: Съёмка фотографий с помощью ImageReader"
description: Узнайте, как снимать неподвижные фотографии с помощью ImageReader — ключевого компонента для получения данных изображения из Camera2.
keywords: [ImageReader, съёмка фотографий, JPEG, Camera2, запрос на съёмку]
---

Теперь, когда вы можете отображать предпросмотр, пришло время снимать фотографии. Давайте узнаем о ImageReader.

## Введение

Чтобы сфотографировать с помощью Camera2, вам нужен способ получения данных изображения. Для этого и существует **ImageReader**.

ImageReader действует как буфер между камерой и вашим приложением. Он получает данные изображения от камеры и предоставляет их вашему приложению для обработки или сохранения.

## Что такое ImageReader?

ImageReader — это класс Android, который позволяет вам:
- Получать данные изображения от камеры
- Получать доступ к последнему сделанному изображению
- Настраивать формат и размер изображения
- Устанавливать максимальное количество изображений для буферизации

Вы создаёте ImageReader с определённым форматом и размером:

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // Ширина изображения
    height,     // Высота изображения
    format,     // Формат изображения (например, ImageFormat.JPEG)
    maxImages   // Максимальное количество изображений для буферизации
)
```

## Форматы изображений

Camera2 поддерживает несколько форматов изображений:

| Формат | Описание |
| --- | --- |
| `ImageFormat.JPEG` | Стандартный формат сжатых изображений |
| `ImageFormat.RAW_SENSOR` | Необработанные данные сенсора (до обработки ISP) |
| `ImageFormat.YUV_420_888` | Несжатый формат YUV |
| `ImageFormat.RAW10` | 10-битный необработанный формат |
| `ImageFormat.RAW12` | 12-битный необработанный формат |

Для большинства приложений JPEG — лучший выбор для съёмки фотографий.

## Создание ImageReader

Вот как создать ImageReader для съёмки в формате JPEG:

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // Хранить до 2 изображений в буфере
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // Обработать изображение
    image.close()
}, null)
```

## Съёмка фотографии

Чтобы сфотографировать, вам нужно:
1. Создать ImageReader
2. Добавить его Surface в сессию захвата
3. Создать запрос на съёмку с `TEMPLATE_STILL_CAPTURE`
4. Отправить запрос камере

## Полный пример съёмки фотографии

Давайте дополним наше приложение с предпросмотром возможностью съёмки фотографий:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoCaptureActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null
    private var pictureSize: Size? = null
    private var imageReader: ImageReader? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_capture)
        
        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.captureButton)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) return

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
        
        previewSize = previewSizes?.get(0)
        pictureSize = pictureSizes?.get(0)

        // Создать ImageReader для съёмки фотографий
        imageReader = ImageReader.newInstance(
            pictureSize?.width ?: 0,
            pictureSize?.height ?: 0,
            ImageFormat.JPEG,
            2
        )
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            saveImage(image)
            image.close()
        }, null)

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
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val previewSurface = Surface(texture)
        val readerSurface = imageReader?.surface ?: return
        
        val surfaces = listOf(previewSurface, readerSurface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@PhotoCaptureActivity, "Ошибка конфигурации сессии", Toast.LENGTH_SHORT).show()
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

    private fun takePhoto() {
        val readerSurface = imageReader?.surface ?: return
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder?.addTarget(readerSurface)
        
        // Установить автофокус в режим одиночного снимка
        captureRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        
        captureRequestBuilder?.let { builder ->
            captureSession?.stopRepeating()
            captureSession?.capture(builder.build(), captureCallback, null)
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            startPreview() // Возобновить предпросмотр после съёмки
        }
    }

    private fun saveImage(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, fileName)
        
        try {
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            Toast.makeText(this, "Фото сохранено: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось сохранить фото", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
        imageReader?.close()
    }
}
```

## Разметка

Добавьте кнопку съёмки в вашу разметку:

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

    <Button
        android:id="@+id/captureButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Съёмка"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## Как это работает

1. **Создать ImageReader** — настроить на получение изображений JPEG
2. **Добавить Surface в сессию** — камера отправляет фотографии на эту поверхность
3. **Создать запрос на съёмку** — использовать `TEMPLATE_STILL_CAPTURE` для фотографий
4. **Отправить запрос на съёмку** — остановить предпросмотр, сфотографировать, возобновить предпросмотр
5. **Сохранить изображение** — записать данные JPEG в файл

## CaptureRequest для фотографий

Для неподвижной съёмки используйте `TEMPLATE_STILL_CAPTURE`. Этот шаблон оптимизирует настройки для:
- Более высокого разрешения
- Лучшего качества изображения
- Одиночного автофокуса

## Работа с изображениями

Всегда не забывайте:
1. **Получить изображение** — используйте `acquireLatestImage()`
2. **Обработать его** — сохраните или отобразите изображение
3. **Закрыть его** — всегда вызывайте `image.close()` для освобождения ресурсов

## Следующая глава

В следующей главе мы узнаем о съёмке в формате RAW и о том, как работать с разными форматами изображений.

## Резюме

Съёмка фотографий с помощью Camera2 включает:

1. **ImageReader** — получает данные изображения от камеры
2. **Surface** — добавляется в сессию захвата для вывода фотографий
3. **TEMPLATE_STILL_CAPTURE** — оптимизированный шаблон запроса на съёмку
4. **CaptureCallback** — уведомляет о завершении съёмки
5. **Обработка изображения** — сохранение или отображение сделанного изображения

Теперь вы научились снимать базовые фотографии. В следующей главе мы рассмотрим съёмку в формате RAW и продвинутые функции фотосъёмки.
