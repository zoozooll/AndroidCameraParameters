---
sidebar_position: 9
title: "Capítulo 9: Exibindo a Pré-visualização da Câmera"
description: Aprenda como exibir a pré-visualização da câmera usando TextureView, Surface e CameraCaptureSession no Camera2.
keywords: [pré-visualização da câmera, TextureView, Surface, CameraCaptureSession, Camera2]
---

Finalmente, você verá a pré-visualização da câmera! Vamos conectar tudo junto.

## Introdução

Abrir uma câmera é ótimo, mas você ainda não consegue ver nada. Para exibir o que a câmera vê, você precisa:

1. Criar um TextureView para exibir a pré-visualização
2. Obter um Surface do TextureView
3. Criar uma CameraCaptureSession
4. Iniciar a pré-visualização

É aqui que as peças se encaixam.

## TextureView

TextureView é uma view que pode exibir um `SurfaceTexture`. É perfeita para mostrar pré-visualizações de câmera porque:
- Pode ser transformada (escalada, rotacionada)
- Suporta aceleração por hardware
- Funciona bem com animações e transições

Adicione um TextureView ao seu layout:

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

Um Surface é um buffer que pode receber dados de imagem. Para exibir uma pré-visualização da câmera:
1. Obtenha o SurfaceTexture do TextureView
2. Crie um Surface a partir do SurfaceTexture
3. Passe o Surface para a CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession gerencia o processo de captura. Ela conecta o dispositivo de câmera a um ou mais Surfaces.

Para criar uma sessão:
1. Prepare uma lista de Surfaces (para pré-visualização, captura de foto, etc.)
2. Chame `createCaptureSession()` no CameraDevice
3. Lide com o callback

## Um Exemplo Completo de Pré-visualização

Vamos criar uma activity que mostra uma pré-visualização da câmera:

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
            // Lide com mudanças de tamanho se necessário
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Chamado quando a pré-visualização é atualizada
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
            Toast.makeText(this, "Nenhuma câmera disponível", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // Obter tamanhos de pré-visualização
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // Escolher um tamanho de pré-visualização
        previewSize = previewSizes?.get(0) // Usar o primeiro tamanho disponível

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
            Toast.makeText(this@CameraPreviewActivity, "Erro na câmera", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "Falha na configuração da sessão", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Permissão da câmera é necessária", Toast.LENGTH_SHORT).show()
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

## Como Funciona

Vamos rastrear o fluxo:

1. **TextureView disponível** — `onSurfaceTextureAvailable()` é chamado
2. **Abrir câmera** — Obtemos um CameraDevice
3. **Criar sessão de captura** — Conectar a câmera ao Surface
4. **Iniciar pré-visualização** — Enviar uma solicitação de captura repetitiva

## CaptureRequest

CaptureRequest define o que a câmera deve capturar:
- `TEMPLATE_PREVIEW` — Para modo de pré-visualização
- `TEMPLATE_STILL_CAPTURE` — Para fotos fixas
- `TEMPLATE_RECORD` — Para gravação de vídeo
- `TEMPLATE_VIDEO_SNAPSHOT` — Para captura instantânea durante o vídeo

## O Loop de Pré-visualização

Quando você chama `setRepeatingRequest()`, a câmera envia quadros continuamente para o Surface. Isso cria a pré-visualização ao vivo.

## Notas Importantes

1. **Surface deve estar disponível** — Aguarde `onSurfaceTextureAvailable()` antes de abrir a câmera
2. **Fechar recursos** — Sempre feche a sessão de captura e o dispositivo de câmera
3. **Lide com a orientação** — A pré-visualização pode precisar de rotação dependendo da orientação do dispositivo
4. **Tamanho importa** — Escolha um tamanho de pré-visualização que corresponda às dimensões do seu TextureView

## Sucesso!

Quando você executar este aplicativo, deverá ver uma pré-visualização ao vivo da câmera na sua tela. Parabéns! Você construiu seu primeiro aplicativo de pré-visualização Camera2.

## Próximo Capítulo

Agora que você pode exibir uma pré-visualização, o próximo passo é capturar fotos. Na Parte III, aprenderemos sobre:

1. ImageReader para capturar fotos
2. Captura JPEG e RAW
3. CaptureRequest e CaptureResult

## Resumo

Exibir uma pré-visualização da câmera envolve:

1. **TextureView** — O componente de UI para exibir a pré-visualização
2. **Surface** — O buffer que recebe os quadros da câmera
3. **CameraCaptureSession** — Gerencia o processo de captura
4. **CaptureRequest** — Define o que capturar
5. **setRepeatingRequest()** — Inicia o loop contínuo de pré-visualização

Você agora concluiu a Parte II desta série. Você pode:
- Descobrir câmeras
- Examinar características da câmera
- Abrir uma câmera
- Exibir uma pré-visualização

Na Parte III, aprenderemos como capturar fotos com Camera2.
