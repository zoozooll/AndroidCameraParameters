---
sidebar_position: 10
title: "Capítulo 10: Capturando Fotos com ImageReader"
description: Aprenda como capturar fotos fixas usando ImageReader, o componente principal para receber dados de imagem do Camera2.
keywords: [ImageReader, captura de foto, JPEG, Camera2, solicitação de captura]
---

Agora que você pode exibir uma pré-visualização, é hora de capturar fotos. Vamos aprender sobre o ImageReader.

## Introdução

Para capturar uma foto com Camera2, você precisa de uma maneira de receber os dados da imagem. É aí que entra o **ImageReader**.

O ImageReader atua como um buffer entre a câmera e seu aplicativo. Ele recebe dados de imagem da câmera e os fornece ao seu app para processamento ou salvamento.

## O que é ImageReader?

ImageReader é uma classe Android que permite que você:
- Receba dados de imagem da câmera
- Acesse a última imagem capturada
- Configure o formato e o tamanho da imagem
- Defina um número máximo de imagens para buffer

Você cria um ImageReader com um formato e tamanho específicos:

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // Largura da imagem
    height,     // Altura da imagem
    format,     // Formato da imagem (ex.: ImageFormat.JPEG)
    maxImages   // Número máximo de imagens para buffer
)
```

## Formatos de Imagem

Camera2 suporta vários formatos de imagem:

| Formato | Descrição |
| --- | --- |
| `ImageFormat.JPEG` | Formato de imagem compactado padrão |
| `ImageFormat.RAW_SENSOR` | Dados brutos do sensor (antes do processamento ISP) |
| `ImageFormat.YUV_420_888` | Formato YUV não compactado |
| `ImageFormat.RAW10` | Formato bruto de 10 bits |
| `ImageFormat.RAW12` | Formato bruto de 12 bits |

Para a maioria dos aplicativos, JPEG é a melhor escolha para captura de fotos.

## Criando um ImageReader

Veja como criar um ImageReader para captura JPEG:

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // Mantém até 2 imagens no buffer
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // Processa a imagem
    image.close()
}, null)
```

## Capturando uma Foto

Para capturar uma foto, você precisa:
1. Criar um ImageReader
2. Adicionar seu Surface à sessão de captura
3. Criar uma solicitação de captura com `TEMPLATE_STILL_CAPTURE`
4. Enviar a solicitação para a câmera

## Um Exemplo Completo de Captura de Foto

Vamos estender nosso aplicativo de pré-visualização para capturar fotos:

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

        // Cria ImageReader para captura de foto
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
            Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@PhotoCaptureActivity, "Falha na configuração da sessão", Toast.LENGTH_SHORT).show()
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
        
        // Define o foco automático para disparo único
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
            startPreview() // Retoma a pré-visualização após a captura
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
            Toast.makeText(this, "Foto salva: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao salvar a foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Permissão da câmera é necessária", Toast.LENGTH_SHORT).show()
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

## O Layout

Adicione um botão de captura ao seu layout:

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
        android:text="Capturar"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## Como Funciona

1. **Criar ImageReader** — Configurado para receber imagens JPEG
2. **Adicionar Surface à sessão** — A câmera envia fotos para este surface
3. **Criar solicitação de captura** — Use `TEMPLATE_STILL_CAPTURE` para fotos
4. **Enviar solicitação de captura** — Parar a pré-visualização, capturar a foto, retomar a pré-visualização
5. **Salvar imagem** — Escrever os dados JPEG em um arquivo

## CaptureRequest para Fotos

Para captura fixa, use `TEMPLATE_STILL_CAPTURE`. Este template otimiza as configurações para:
- Maior resolução
- Melhor qualidade de imagem
- Foco automático de disparo único

## Manipulando Imagens

Lembre-se sempre de:
1. **Adquirir a imagem** — Use `acquireLatestImage()`
2. **Processá-la** — Salve ou exiba a imagem
3. **Fechá-la** — Sempre chame `image.close()` para liberar recursos

## Próximo Capítulo

No próximo capítulo, aprenderemos sobre captura RAW e como trabalhar com diferentes formatos de imagem.

## Resumo

Capturar fotos com Camera2 envolve:

1. **ImageReader** — Recebe dados de imagem da câmera
2. **Surface** — Adicionado à sessão de captura para saída de foto
3. **TEMPLATE_STILL_CAPTURE** — Template de solicitação de captura otimizado
4. **CaptureCallback** — Notifica quando a captura for concluída
5. **Processamento de imagem** — Salvar ou exibir a imagem capturada

Você agora aprendeu como capturar fotos básicas. No próximo capítulo, exploraremos a captura RAW e recursos avançados de foto.
