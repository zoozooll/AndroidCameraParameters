---
sidebar_position: 8
title: "Bab 8: Membuka Kamera Pertama Anda"
description: Pelajari cara membuka CameraDevice menggunakan CameraManager dan menangani siklus hidup kamera dengan callback status.
keywords: [CameraDevice, openCamera, siklus hidup kamera, CameraManager]
---

Saatnya membuka kamera pertama Anda! Mari pelajari tentang CameraDevice.

## Pendahuluan

Sejauh ini, kita telah belajar cara menemukan kamera dan memeriksa karakteristiknya. Sekarang kita akan melangkah ke tahap berikutnya: **membuka kamera**.

Membuka kamera memberi Anda akses ke perangkat keras kamera yang sebenarnya. Setelah terbuka, Anda dapat membuat sesi penangkapan, menampilkan pratinjau, dan mengambil foto.

## Apa itu CameraDevice?

CameraDevice mewakili satu kamera yang terhubung ke perangkat Android. Ia menyediakan metode untuk:
- Membuat sesi penangkapan
- Menangkap gambar diam
- Memulai dan menghentikan pratinjau

Anda tidak membuat CameraDevice secara langsung. Sebaliknya, Anda mendapatkannya dari CameraManager dengan memanggil `openCamera()`.

## Membuka Kamera

Berikut cara membuka kamera:

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // Kamera siap digunakan
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // Kamera terputus
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // Terjadi kesalahan kamera
        camera.close()
    }
}, null)
```

Mari kita bedah ini.

### StateCallback

CameraDevice menggunakan pola callback karena membuka kamera bersifat asinkron. Callback memiliki tiga metode utama:

#### 1. `onOpened(camera: CameraDevice)`

Dipanggil ketika kamera berhasil dibuka. Di sinilah Anda mendapatkan instance CameraDevice.

#### 2. `onDisconnected(camera: CameraDevice)`

Dipanggil ketika kamera terputus. Ini dapat terjadi jika kamera digunakan oleh aplikasi lain atau jika perangkat dimatikan. Selalu tutup kamera dalam callback ini.

#### 3. `onError(camera: CameraDevice, error: Int)`

Dipanggil ketika terjadi kesalahan. Kode kesalahan umum:
- `ERROR_CAMERA_IN_USE` — Kamera sudah digunakan
- `ERROR_MAX_CAMERAS_IN_USE` — Terlalu banyak kamera yang terbuka
- `ERROR_CAMERA_DISABLED` — Kamera dinonaktifkan
- `ERROR_CAMERA_DEVICE` — Kesalahan perangkat keras kamera
- `ERROR_CAMERA_SERVICE` — Kesalahan layanan kamera

### Handler

Parameter ketiga adalah `Handler`. Jika Anda meneruskan `null`, callback akan berjalan pada looper thread pemanggil. Untuk pembaruan UI, Anda mungkin ingin meneruskan handler yang berjalan di thread utama.

## Contoh Lengkap

Mari buat aktivitas yang membuka kamera:

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
            Toast.makeText(this, "Tidak ada kamera yang tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // Buka kamera pertama
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "ID kamera tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "Kamera berhasil dibuka!", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "Kamera ${camera.id} dibuka")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "Kamera terputus", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "Kamera sedang digunakan"
                ERROR_MAX_CAMERAS_IN_USE -> "Terlalu banyak kamera yang terbuka"
                ERROR_CAMERA_DISABLED -> "Kamera dinonaktifkan"
                ERROR_CAMERA_DEVICE -> "Kesalahan perangkat keras kamera"
                ERROR_CAMERA_SERVICE -> "Kesalahan layanan kamera"
                else -> "Kesalahan tidak diketahui"
            }
            
            Toast.makeText(this@CameraOpenActivity, "Kesalahan kamera: $errorMessage", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## Siklus Hidup Kamera

Memahami siklus hidup kamera sangat penting:

1. **Buka** — Panggil `openCamera()` untuk mendapatkan CameraDevice
2. **Gunakan** — Buat sesi penangkapan, ambil foto
3. **Tutup** — Panggil `close()` setelah selesai
4. **Lepaskan** — Kamera tersedia untuk aplikasi lain

Selalu tutup kamera saat aktivitas Anda dihancurkan untuk menghindari kebocoran sumber daya.

## Praktik Terbaik

1. **Tutup setelah selesai** — Selalu tutup kamera di `onDestroy()`
2. **Tangani kesalahan** — Jangan abaikan callback `onError()`
3. **Periksa izin** — Selalu verifikasi izin sebelum membuka
4. **Gunakan try-catch** — Tangani `SecurityException` dan `IllegalArgumentException`
5. **Jangan simpan referensi** — Lepaskan referensi CameraDevice saat ditutup

## Masalah Umum

### Kamera sedang digunakan
- Pastikan tidak ada aplikasi lain yang menggunakan kamera
- Periksa bahwa Anda menutup kamera dengan benar

### Izin ditolak
- Verifikasi izin di manifes
- Periksa izin runtime telah diberikan

### ID kamera tidak ditemukan
- Selalu dapatkan ID kamera dari `getCameraIdList()`
- Jangan hardcode ID kamera

## Bab Berikutnya

Sekarang setelah Anda dapat membuka kamera, langkah selanjutnya adalah menampilkan pratinjau. Di bab berikutnya, kita akan:

1. Mempelajari tentang TextureView
2. Membuat Surface untuk pratinjau
3. Membuat CameraCaptureSession
4. Menampilkan pratinjau kamera di layar

## Ringkasan

Membuka kamera adalah langkah pertama menuju pengambilan gambar:

1. Gunakan `CameraManager.openCamera()` untuk mendapatkan CameraDevice
2. Tangani StateCallback untuk `onOpened()`, `onDisconnected()`, dan `onError()`
3. Selalu tutup kamera setelah selesai
4. Ikuti siklus hidup kamera: buka → gunakan → tutup → lepaskan

Di bab berikutnya, kita akan membuat pratinjau kamera sehingga Anda dapat melihat apa yang dilihat kamera.
