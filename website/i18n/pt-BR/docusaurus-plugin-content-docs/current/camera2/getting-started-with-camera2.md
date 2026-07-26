---
sidebar_position: 5
title: "Capítulo 5: Introdução ao Camera2"
description: Saiba sobre o CameraManager, o ponto de entrada da Android Camera2 API que permite enumerar câmeras e acessar suas características.
keywords: [CameraManager, Camera2 API, câmera Android, enumeração de câmeras]
---

Bem-vindo à parte de codificação desta série. Vamos começar com a base: CameraManager.

## Introdução

Antes de poder usar qualquer câmera, você precisa de uma maneira de descobri-la e acessá-la. É aí que entra o **CameraManager**.

CameraManager é a porta de entrada para a Camera2 API. É a primeira classe que você usará em qualquer aplicação Camera2.

## O que é CameraManager?

CameraManager é um serviço do sistema que gerencia todos os dispositivos de câmera em um dispositivo Android. Pense nele como um diretório ou registro de câmeras.

Suas principais responsabilidades são:
1. **Enumerar câmeras** — Listar todas as câmeras disponíveis
2. **Obter características da câmera** — Recuperar informações detalhadas sobre cada câmera
3. **Abrir câmeras** — Criar um CameraDevice para captura

## Obtendo CameraManager

No Android, os serviços do sistema são obtidos através do `Context`. Veja como obter o CameraManager:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

É isso. Uma linha de código para ter acesso a todas as câmeras do dispositivo.

## Permissões Primeiro

Antes de usar o CameraManager, você precisa solicitar permissões de câmera. Adicione estas ao seu `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

E solicite a permissão em tempo de execução na sua atividade:

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

Sempre verifique as permissões antes de acessar a câmera.

## Métodos do CameraManager

CameraManager tem três métodos principais que você usará:

### 1. `getCameraIdList()`

Retorna um array de strings de ID de câmera. Cada ID representa um dispositivo de câmera.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Câmera encontrada: $id")
}
```

Isso pode gerar:
```
Câmera 0
Câmera 1
Câmera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

Retorna um objeto `CameraCharacteristics` contendo todos os detalhes sobre uma câmera específica.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics contém centenas de parâmetros que descrevem as capacidades da câmera.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

Abre uma câmera e retorna um `CameraDevice` através do callback. Cobriremos isso em detalhe mais tarde.

## IDs de Câmera Revisitados

Lembre-se do Capítulo 4 que o Android atribui IDs numéricos às câmeras. Os IDs não são garantidos como consistentes entre dispositivos ou mesmo entre reinicializações.

Padrões comuns:
- **Câmera 0** — Tipicamente a câmera traseira grande angular
- **Câmera 1** — Geralmente a câmera frontal
- **Câmera 2** — Normalmente uma câmera ultra-grande angular ou telefoto
- Números maiores — Câmeras adicionais (macro, profundidade, etc.)

Mas **nunca assuma** o significado de um ID de câmera. Sempre verifique as características da câmera para determinar:
- Orientação da lente (frontal/traseira/externa)
- Distância focal
- Capacidades

## Por que o CameraManager é Importante

CameraManager é a base para tudo o que faremos com o Camera2:

1. **Descoberta** — Antes de usar uma câmera, você precisa encontrá-la
2. **Informação** — Antes de abrir uma câmera, você precisa saber suas capacidades
3. **Acesso** — CameraManager fornece a única maneira de abrir um dispositivo de câmera

## Um Exemplo Simples

Vamos juntar tudo em um exemplo simples:

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
        
        Log.d("CameraDiscovery", "Encontrada(s) ${cameraIds.size} câmera(s)")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
                CameraCharacteristics.LENS_FACING_BACK -> "Traseira"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externa"
                else -> "Desconhecida"
            }
            
            Log.d("CameraDiscovery", "Câmera $cameraId: $lensFacingStr")
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
                Toast.makeText(this, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

Esta atividade simples descobre todas as câmeras e registra seus IDs e orientações de lente.

## Principais Conclusões

- **CameraManager** é o ponto de entrada para o Camera2
- Use `getCameraIdList()` para encontrar todas as câmeras
- Use `getCameraCharacteristics()` para obter informações detalhadas
- Sempre solicite permissões de câmera primeiro
- Nunca assuma significados de ID de câmera — verifique as características

## Próximo Capítulo

Agora que você entende o CameraManager, é hora de escrever seu primeiro programa Camera2 de verdade. No próximo capítulo, vamos:

1. Criar um app Android simples
2. Listar todas as câmeras disponíveis
3. Exibir informações da câmera para o usuário

Você escreverá seu primeiro código Camera2 e verá resultados reais!

## Resumo

CameraManager é a base do Camera2. Ele fornece acesso a:
- Enumeração de câmeras
- Características da câmera
- Abertura de câmera

Com o CameraManager, você pode descobrir quais câmeras estão disponíveis e aprender sobre suas capacidades antes de abri-las.

No próximo capítulo, escreveremos nosso primeiro programa Camera2 que lista todas as câmeras do dispositivo.
