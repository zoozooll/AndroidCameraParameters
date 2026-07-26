---
sidebar_position: 9
title: "Bab 9: Menampilkan Preview Kamera"
description: Pelajari cara menampilkan preview kamera menggunakan TextureView, Surface, dan CameraCaptureSession di Camera2.
keywords: [preview kamera, TextureView, Surface, CameraCaptureSession, Camera2]
---

Akhirnya, Anda akan melihat preview kamera! Mari kita hubungkan semuanya.

## Pendahuluan

Membuka kamera itu bagus, tapi Anda belum bisa melihat apa pun. Untuk menampilkan apa yang dilihat kamera, Anda perlu:

1. Membuat TextureView untuk menampilkan preview
2. Mendapatkan Surface dari TextureView
3. Membuat CameraCaptureSession
4. Memulai preview

Di sinilah potongan-potongan itu bergabung.

## TextureView

TextureView adalah view yang dapat menampilkan `SurfaceTexture`. Ini sangat cocok untuk menampilkan preview kamera karena:
- Dapat ditransformasi (disesuaikan skala, diputar)
- Mendukung akselerasi perangkat keras
- Bekerja dengan baik dengan animasi dan transisi

Tambahkan TextureView ke layout Anda:

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

Surface adalah buffer yang dapat menerima data gambar. Untuk menampilkan preview kamera:
1. Dapatkan SurfaceTexture dari TextureView
2. Buat Surface dari SurfaceTexture
3. Berikan Surface ke CameraCaptureSession

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSession mengelola proses pengambilan gambar. Ini menghubungkan perangkat kamera ke satu atau lebih Surface.

Untuk membuat sesi:
1. Siapkan daftar Surface (untuk preview, pengambilan foto, dll.)
2. Panggil `createCaptureSession()` pada CameraDevice
3. Tangani callback-nya

## Contoh Preview Lengkap

Mari kita buat activity yang menampilkan preview kamera:

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
            // Tangani perubahan ukuran jika diperlukan
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Dipanggil ketika preview diperbarui
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

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // Dapatkan ukuran preview
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // Pilih ukuran preview
        previewSize = previewSizes?.get(0) // Gunakan ukuran pertama yang tersedia

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "Kesalahan kamera", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this@CameraPreviewActivity, "Konfigurasi sesi gagal", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
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

## Cara Kerjanya

Mari kita telusuri alurnya:

1. **TextureView tersedia** — `onSurfaceTextureAvailable()` dipanggil
2. **Buka kamera** — Kita mendapatkan CameraDevice
3. **Buat capture session** — Hubungkan kamera ke Surface
4. **Mulai preview** — Kirim permintaan capture berulang

## CaptureRequest

CaptureRequest mendefinisikan apa yang harus di-capture oleh kamera:
- `TEMPLATE_PREVIEW` — Untuk mode preview
- `TEMPLATE_STILL_CAPTURE` — Untuk foto diam
- `TEMPLATE_RECORD` — Untuk perekaman video
- `TEMPLATE_VIDEO_SNAPSHOT` — Untuk snapshot selama video

## Loop Preview

Ketika Anda memanggil `setRepeatingRequest()`, kamera terus mengirimkan frame ke Surface. Ini menciptakan preview langsung.

## Catatan Penting

1. **Surface harus tersedia** — Tunggu `onSurfaceTextureAvailable()` sebelum membuka kamera
2. **Tutup sumber daya** — Selalu tutup capture session dan perangkat kamera
3. **Tangani orientasi** — Preview mungkin perlu diputar tergantung pada orientasi perangkat
4. **Ukuran itu penting** — Pilih ukuran preview yang sesuai dengan dimensi TextureView Anda

## Berhasil!

Ketika Anda menjalankan aplikasi ini, Anda akan melihat preview kamera langsung di layar Anda. Selamat! Anda telah membuat aplikasi preview Camera2 pertama Anda.

## Bab Berikutnya

Sekarang setelah Anda dapat menampilkan preview, langkah selanjutnya adalah mengambil foto. Di Bagian III, kita akan mempelajari tentang:

1. ImageReader untuk mengambil foto
2. Capture JPEG dan RAW
3. CaptureRequest dan CaptureResult

## Ringkasan

Menampilkan preview kamera melibatkan:

1. **TextureView** — Komponen UI untuk menampilkan preview
2. **Surface** — Buffer yang menerima frame kamera
3. **CameraCaptureSession** — Mengelola proses capture
4. **CaptureRequest** — Mendefinisikan apa yang akan di-capture
5. **setRepeatingRequest()** — Memulai loop preview berkelanjutan

Anda telah menyelesaikan Bagian II dari seri ini. Anda dapat:
- Menemukan kamera
- Memeriksa karakteristik kamera
- Membuka kamera
- Menampilkan preview

Di Bagian III, kita akan mempelajari cara mengambil foto dengan Camera2.
