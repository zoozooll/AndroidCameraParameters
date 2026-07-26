---
sidebar_position: 6
title: "Capítulo 6: Listando Câmeras"
description: Escreva seu primeiro programa Camera2 que descobre e lista todas as câmeras disponíveis em um dispositivo Android.
keywords: [listar câmeras, CameraManager, enumeração de câmeras, Android Camera2]
---

Está na hora de escrever seu primeiro programa Camera2! Vamos criar um aplicativo que lista todas as câmeras.

## Introdução

Neste capítulo, você escreverá sua primeira aplicação Camera2 real. O objetivo é simples:

> Descobrir todas as câmeras do dispositivo e exibir suas informações.

Este é um passo pequeno, mas importante. Antes de usar uma câmera, você precisa encontrá-la.

## Criando o Projeto

Vamos começar criando um novo projeto Android:

1. Abra o Android Studio
2. Crie um novo projeto com "Empty Activity"
3. Nomeie-o como "Camera2List"
4. Selecione Kotlin como a linguagem
5. Defina o SDK mínimo como API 21 (o Camera2 foi introduzido na API 21)

## Adicionando Permissões

Adicione a permissão de câmera ao `AndroidManifest.xml`:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## O Layout

Crie um layout simples que exiba uma lista de câmeras. Atualize o `activity_main.xml`:

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
        android:text="Câmeras Disponíveis"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## A Activity

Agora vamos escrever a activity principal. É aqui que o código Camera2 vai:

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
                cameraInfoList.add("Nenhuma câmera encontrada")
            } else {
                cameraInfoList.add("Encontrada(s) ${cameraIds.size} câmera(s):")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
                        CameraCharacteristics.LENS_FACING_BACK -> "Traseira"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externa"
                        else -> "Desconhecida"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "Desconhecido"
                    }
                    
                    cameraInfoList.add("Câmera $index (ID: $cameraId)")
                    cameraInfoList.add("  - Lente: $lensFacingStr")
                    cameraInfoList.add("  - Nível de Hardware: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("Erro: Permissão de câmera negada")
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
                Toast.makeText(this, "Permissão de câmera é necessária", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("Permissão de câmera negada")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## O Que Este Código Faz

Vamos analisar o que está acontecendo:

1. **Obter o CameraManager** — Obtemos o serviço de sistema CameraManager
2. **Verificar Permissões** — Verificamos se a permissão de câmera foi concedida
3. **Listar Câmeras** — Usamos `getCameraIdList()` para obter todos os IDs das câmeras
4. **Obter Características** — Para cada câmera, obtemos suas características
5. **Exibir Informações** — Mostramos o ID da câmera, a orientação da lente e o nível de hardware

## Saída Esperada

Quando você executar o aplicativo, deverá ver algo como:

```
Encontrada(s) 3 câmera(s):

Câmera 0 (ID: 0)
  - Lente: Traseira
  - Nível de Hardware: FULL

Câmera 1 (ID: 1)
  - Lente: Frontal
  - Nível de Hardware: LIMITED

Câmera 2 (ID: 2)
  - Lente: Traseira
  - Nível de Hardware: FULL
```

## Sucesso!

Você acabou de escrever seu primeiro programa Camera2! Pode parecer simples, mas esta é a base para tudo o que faremos a seguir.

## Solução de Problemas

Se você encontrar problemas:

1. **Permissão negada** — Certifique-se de ter concedido a permissão de câmera
2. **Nenhuma câmera encontrada** — Verifique se seu dispositivo possui uma câmera
3. **SecurityException** — Certifique-se de que as permissões estão declaradas no manifest
4. **Nível de API muito baixo** — O Camera2 requer API 21 ou superior

## Qual é o Próximo Passo?

Agora que você pode listar câmeras, o próximo passo é examinar suas características com mais detalhes. No próximo capítulo, nós:

1. Exploraremos o CameraCharacteristics
2. Aprenderemos sobre a orientação da lente
3. Entenderemos os níveis de hardware
4. Verificaremos informações do sensor

## Resumo

Neste capítulo, você escreveu seu primeiro programa Camera2. O aplicativo:

1. Solicita permissões de câmera
2. Usa o CameraManager para enumerar câmeras
3. Exibe o ID da câmera, a orientação da lente e o nível de hardware

Este é o primeiro passo para construir aplicativos Camera2 completos. No próximo capítulo, mergulharemos mais profundamente no CameraCharacteristics para entender o que cada câmera pode fazer.
