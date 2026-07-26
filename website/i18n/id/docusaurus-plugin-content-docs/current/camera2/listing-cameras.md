---
sidebar_position: 6
title: "Bab 6: Mendaftarkan Kamera"
description: Tulis program Camera2 pertama Anda yang menemukan dan mendaftarkan semua kamera yang tersedia di perangkat Android.
keywords: [daftar kamera, CameraManager, enumerasi kamera, Android Camera2]
---

Saatnya menulis program Camera2 pertama Anda! Mari kita buat aplikasi yang mendaftarkan semua kamera.

## Pendahuluan

Dalam bab ini, Anda akan menulis aplikasi Camera2 nyata pertama Anda. Tujuannya sederhana:

> Temukan semua kamera di perangkat dan tampilkan informasinya.

Ini adalah langkah kecil namun penting. Sebelum Anda dapat menggunakan kamera, Anda perlu menemukannya.

## Membuat Proyek

Mari kita mulai dengan membuat proyek Android baru:

1. Buka Android Studio
2. Buat proyek baru dengan "Empty Activity"
3. Beri nama "Camera2List"
4. Pilih Kotlin sebagai bahasanya
5. Atur minimum SDK ke API 21 (Camera2 diperkenalkan pada API 21)

## Menambahkan Izin

Tambahkan izin kamera ke `AndroidManifest.xml`:

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## Tata Letak

Buat tata letak sederhana yang menampilkan daftar kamera. Perbarui `activity_main.xml`:

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
        android:text="Kamera Tersedia"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## Aktivitas

Sekarang mari kita tulis aktivitas utama. Di sinilah kode Camera2 berada:

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
                cameraInfoList.add("Tidak ada kamera ditemukan")
            } else {
                cameraInfoList.add("Ditemukan ${cameraIds.size} kamera:")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Depan"
                        CameraCharacteristics.LENS_FACING_BACK -> "Belakang"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "Eksternal"
                        else -> "Tidak Diketahui"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "Tidak Diketahui"
                    }
                    
                    cameraInfoList.add("Kamera $index (ID: $cameraId)")
                    cameraInfoList.add("  - Lensa: $lensFacingStr")
                    cameraInfoList.add("  - Tingkat Perangkat Keras: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("Kesalahan: Izin kamera ditolak")
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
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("Izin kamera ditolak")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## Apa yang Dilakukan Kode Ini

Mari kita uraikan apa yang terjadi:

1. **Dapatkan CameraManager** — Kita mendapatkan layanan sistem CameraManager
2. **Periksa Izin** — Kita memeriksa apakah izin kamera diberikan
3. **Daftarkan Kamera** — Kita menggunakan `getCameraIdList()` untuk mendapatkan semua ID kamera
4. **Dapatkan Karakteristik** — Untuk setiap kamera, kita mendapatkan karakteristiknya
5. **Tampilkan Informasi** — Kita menampilkan ID kamera, arah lensa, dan tingkat perangkat keras

## Output yang Diharapkan

Saat Anda menjalankan aplikasi, Anda akan melihat sesuatu seperti:

```
Ditemukan 3 kamera:

Kamera 0 (ID: 0)
  - Lensa: Belakang
  - Tingkat Perangkat Keras: FULL

Kamera 1 (ID: 1)
  - Lensa: Depan
  - Tingkat Perangkat Keras: LIMITED

Kamera 2 (ID: 2)
  - Lensa: Belakang
  - Tingkat Perangkat Keras: FULL
```

## Berhasil!

Anda baru saja menulis program Camera2 pertama Anda! Mungkin terlihat sederhana, tetapi ini adalah fondasi untuk semua yang akan kita lakukan selanjutnya.

## Pemecahan Masalah

Jika Anda mengalami masalah:

1. **Izin ditolak** — Pastikan Anda memberikan izin kamera
2. **Tidak ada kamera ditemukan** — Periksa apakah perangkat Anda memiliki kamera
3. **SecurityException** — Pastikan izin dideklarasikan dalam manifes
4. **Tingkat API terlalu rendah** — Camera2 memerlukan API 21 atau lebih tinggi

## Apa Selanjutnya?

Sekarang setelah Anda dapat mendaftarkan kamera, langkah selanjutnya adalah memeriksa karakteristiknya secara lebih mendetail. Di bab selanjutnya, kita akan:

1. Menjelajahi CameraCharacteristics
2. Mempelajari tentang arah lensa
3. Memahami tingkat perangkat keras
4. Memeriksa informasi sensor

## Ringkasan

Dalam bab ini, Anda menulis program Camera2 pertama Anda. Aplikasi tersebut:

1. Meminta izin kamera
2. Menggunakan CameraManager untuk menghitung kamera
3. Menampilkan ID kamera, arah lensa, dan tingkat perangkat keras

Ini adalah langkah pertama menuju membangun aplikasi Camera2 lengkap. Di bab selanjutnya, kita akan menyelami lebih dalam CameraCharacteristics untuk memahami apa yang dapat dilakukan setiap kamera.
