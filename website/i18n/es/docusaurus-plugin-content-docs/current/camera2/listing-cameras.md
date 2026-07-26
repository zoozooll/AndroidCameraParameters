---
sidebar_position: 6
title: "Capítulo 6: Listado de Cámaras"
description: Escribe tu primer programa Camera2 que descubre y lista todas las cámaras disponibles en un dispositivo Android.
keywords: [listar cámaras, CameraManager, enumeración de cámaras, Android Camera2]
---

¡Es hora de escribir tu primer programa Camera2! Vamos a crear una app que lista todas las cámaras.

## Introducción

En este capítulo, escribirás tu primera aplicación real de Camera2. El objetivo es simple:

> Descubrir todas las cámaras del dispositivo y mostrar su información.

Este es un paso pequeño pero importante. Antes de poder usar una cámara, necesitas encontrarla.

## Creando el Proyecto

Empecemos creando un nuevo proyecto de Android:

1. Abre Android Studio
2. Crea un nuevo proyecto con "Empty Activity"
3. Nómbralo "Camera2List"
4. Selecciona Kotlin como lenguaje
5. Establece el SDK mínimo en API 21 (Camera2 se introdujo en API 21)

## Añadiendo Permisos

Añade el permiso de cámara a `AndroidManifest.xml`:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## El Layout

Crea un layout simple que muestre una lista de cámaras. Actualiza `activity_main.xml`:

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
        android:text="Cámaras Disponibles"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## La Activity

Ahora escribamos la activity principal. Aquí es donde va el código de Camera2:

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
                cameraInfoList.add("No se encontraron cámaras")
            } else {
                cameraInfoList.add("Se encontraron ${cameraIds.size} cámara(s):")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
                        CameraCharacteristics.LENS_FACING_BACK -> "Trasera"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externa"
                        else -> "Desconocida"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "Desconocido"
                    }
                    
                    cameraInfoList.add("Cámara $index (ID: $cameraId)")
                    cameraInfoList.add("  - Lente: $lensFacingStr")
                    cameraInfoList.add("  - Nivel de Hardware: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("Error: Permiso de cámara denegado")
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
                Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("Permiso de cámara denegado")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## Qué Hace Este Código

Vamos a desglosar lo que está sucediendo:

1. **Obtener CameraManager** — Obtenemos el servicio del sistema CameraManager
2. **Verificar Permisos** — Comprobamos si el permiso de cámara está concedido
3. **Listar Cámaras** — Usamos `getCameraIdList()` para obtener todos los IDs de cámara
4. **Obtener Características** — Para cada cámara, obtenemos sus características
5. **Mostrar Información** — Mostramos el ID de la cámara, la orientación del lente y el nivel de hardware

## Salida Esperada

Cuando ejecutes la app, deberías ver algo como:

```
Se encontraron 3 cámara(s):

Cámara 0 (ID: 0)
  - Lente: Trasera
  - Nivel de Hardware: FULL

Cámara 1 (ID: 1)
  - Lente: Frontal
  - Nivel de Hardware: LIMITED

Cámara 2 (ID: 2)
  - Lente: Trasera
  - Nivel de Hardware: FULL
```

## ¡Éxito!

¡Acabas de escribir tu primer programa Camera2! Puede parecer simple, pero esta es la base para todo lo que haremos después.

## Solución de Problemas

Si encuentras problemas:

1. **Permiso denegado** — Asegúrate de haber concedido el permiso de cámara
2. **No se encontraron cámaras** — Comprueba si tu dispositivo tiene cámara
3. **SecurityException** — Asegúrate de que los permisos estén declarados en el manifest
4. **Nivel de API demasiado bajo** — Camera2 requiere API 21 o superior

## ¿Qué Sigue?

Ahora que puedes listar las cámaras, el siguiente paso es examinar sus características con más detalle. En el próximo capítulo, vamos a:

1. Explorar CameraCharacteristics
2. Aprender sobre la orientación del lente
3. Entender los niveles de hardware
4. Consultar la información del sensor

## Resumen

En este capítulo, escribiste tu primer programa Camera2. La app:

1. Solicita permisos de cámara
2. Usa CameraManager para enumerar las cámaras
3. Muestra el ID de la cámara, la orientación del lente y el nivel de hardware

Este es el primer paso para construir aplicaciones completas de Camera2. En el próximo capítulo, profundizaremos en CameraCharacteristics para entender qué puede hacer cada cámara.
