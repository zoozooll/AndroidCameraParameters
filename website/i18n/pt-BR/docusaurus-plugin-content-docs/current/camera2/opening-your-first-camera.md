---
sidebar_position: 8
title: "Capítulo 8: Abrindo Sua Primeira Câmera"
description: Aprenda como abrir um CameraDevice usando CameraManager e lidar com o ciclo de vida da câmera com callbacks de estado.
keywords: [CameraDevice, openCamera, ciclo de vida da câmera, CameraManager]
---

É hora de abrir sua primeira câmera! Vamos aprender sobre CameraDevice.

## Introdução

Até agora, aprendemos como descobrir câmeras e examinar suas características. Agora daremos o próximo passo: **abrir uma câmera**.

Abrir uma câmera dá a você acesso ao hardware real da câmera. Uma vez aberta, você pode criar sessões de captura, exibir pré-visualizações e capturar fotos.

## O que é CameraDevice?

CameraDevice representa uma única câmera conectada ao dispositivo Android. Ele fornece métodos para:
- Criar sessões de captura
- Capturar imagens fixas
- Iniciar e parar a pré-visualização

Você não cria CameraDevice diretamente. Em vez disso, você o obtém do CameraManager chamando `openCamera()`.

## Abrindo uma Câmera

Veja como abrir uma câmera:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // Câmera está pronta para uso
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // Câmera foi desconectada
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // Ocorreu um erro na câmera
        camera.close()
    }
}, null)
```

Vamos analisar isso detalhadamente.

### O StateCallback

CameraDevice usa um padrão de callback porque abrir uma câmera é assíncrono. O callback tem três métodos principais:

#### 1. `onOpened(camera: CameraDevice)`

Chamado quando a câmera é aberta com sucesso. É aqui que você obtém sua instância do CameraDevice.

#### 2. `onDisconnected(camera: CameraDevice)`

Chamado quando a câmera é desconectada. Isso pode acontecer se a câmera for usada por outro aplicativo ou se o dispositivo for desligado. Sempre feche a câmera neste callback.

#### 3. `onError(camera: CameraDevice, error: Int)`

Chamado quando ocorre um erro. Códigos de erro comuns:
- `ERROR_CAMERA_IN_USE` — Câmera já está em uso
- `ERROR_MAX_CAMERAS_IN_USE` — Muitas câmeras abertas
- `ERROR_CAMERA_DISABLED` — Câmera está desabilitada
- `ERROR_CAMERA_DEVICE` — Erro de hardware da câmera
- `ERROR_CAMERA_SERVICE` — Erro do serviço de câmera

### O Handler

O terceiro parâmetro é um `Handler`. Se você passar `null`, o callback será executado no looper da thread chamadora. Para atualizações de UI, você pode querer passar um handler que execute na thread principal.

## Um Exemplo Completo

Vamos criar uma activity que abre uma câmera:

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
            Toast.makeText(this, "Nenhuma câmera disponível", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // Abre a primeira câmera
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "ID da câmera inválido", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "Câmera aberta com sucesso!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "Câmera ${camera.id} aberta")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "Câmera desconectada", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "Câmera está em uso"
                ERROR_MAX_CAMERAS_IN_USE -> "Muitas câmeras abertas"
                ERROR_CAMERA_DISABLED -> "Câmera está desabilitada"
                ERROR_CAMERA_DEVICE -> "Erro de hardware da câmera"
                ERROR_CAMERA_SERVICE -> "Erro do serviço de câmera"
                else -> "Erro desconhecido"
            }
            
            Toast.makeText(this@CameraOpenActivity, "Erro na câmera: $errorMessage", Toast.LENGTH_SHORT).show()
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
        cameraDevice?.close()
    }
}
```

## O Ciclo de Vida da Câmera

Entender o ciclo de vida da câmera é crucial:

1. **Abrir** — Chame `openCamera()` para obter um CameraDevice
2. **Usar** — Crie sessões de captura, capture fotos
3. **Fechar** — Chame `close()` quando terminar
4. **Liberar** — A câmera fica disponível para outros aplicativos

Sempre feche a câmera quando sua activity for destruída para evitar vazamentos de recursos.

## Melhores Práticas

1. **Feche quando terminar** — Sempre feche a câmera em `onDestroy()`
2. **Lide com erros** — Não ignore callbacks de `onError()`
3. **Verifique permissões** — Sempre verifique as permissões antes de abrir
4. **Use try-catch** — Lide com `SecurityException` e `IllegalArgumentException`
5. **Não mantenha referências** — Libere a referência ao CameraDevice quando fechado

## Problemas Comuns

### Câmera está em uso
- Certifique-se de que nenhum outro aplicativo está usando a câmera
- Verifique se você está fechando a câmera corretamente

### Permissão negada
- Verifique as permissões no manifest
- Confira se a permissão em tempo de execução foi concedida

### ID da câmera não encontrado
- Sempre obtenha os IDs da câmera de `getCameraIdList()`
- Não hardcode os IDs da câmera

## Próximo Capítulo

Agora que você pode abrir uma câmera, o próximo passo é exibir uma pré-visualização. No próximo capítulo, vamos:

1. Aprender sobre TextureView
2. Criar uma Surface para pré-visualização
3. Criar uma CameraCaptureSession
4. Exibir a pré-visualização da câmera na tela

## Resumo

Abrir uma câmera é o primeiro passo para capturar imagens:

1. Use `CameraManager.openCamera()` para obter um CameraDevice
2. Lide com o StateCallback para `onOpened()`, `onDisconnected()` e `onError()`
3. Sempre feche a câmera quando terminar
4. Siga o ciclo de vida da câmera: abrir → usar → fechar → liberar

No próximo capítulo, criaremos uma pré-visualização da câmera para que você possa ver o que a câmera vê.
