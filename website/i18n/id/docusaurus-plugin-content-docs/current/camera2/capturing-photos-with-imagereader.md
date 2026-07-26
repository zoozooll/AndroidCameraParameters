---
sidebar_position: 10
title: "Bab 10: Mengambil Foto dengan ImageReader"
description: Pelajari cara mengambil foto diam menggunakan ImageReader, komponen kunci untuk menerima data gambar dari Camera2.
keywords: [ImageReader, pengambilan foto, JPEG, Camera2, permintaan pengambilan]
---

Sekarang setelah Anda dapat menampilkan pratinjau, saatnya mengambil foto. Mari kita pelajari tentang ImageReader.

## Pendahuluan

Untuk mengambil foto dengan Camera2, Anda membutuhkan cara untuk menerima data gambar. Di situlah **ImageReader** berperan.

ImageReader bertindak sebagai penyangga antara kamera dan aplikasi Anda. Ia menerima data gambar dari kamera dan memberikannya ke aplikasi Anda untuk diproses atau disimpan.

## Apa itu ImageReader?

ImageReader adalah kelas Android yang memungkinkan Anda untuk:
- Menerima data gambar dari kamera
- Mengakses gambar terbaru yang diambil
- Mengonfigurasi format dan ukuran gambar
- Mengatur jumlah maksimum gambar untuk disangga

Anda membuat ImageReader dengan format dan ukuran tertentu:

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // Lebar gambar
    height,     // Tinggi gambar
    format,     // Format gambar (misalnya, ImageFormat.JPEG)
    maxImages   // Jumlah maksimum gambar untuk disangga
)
```

## Format Gambar

Camera2 mendukung beberapa format gambar:

| Format | Deskripsi |
| --- | --- |
| `ImageFormat.JPEG` | Format gambar terkompresi standar |
| `ImageFormat.RAW_SENSOR` | Data sensor mentah (sebelum pemrosesan ISP) |
| `ImageFormat.YUV_420_888` | Format YUV tidak terkompresi |
| `ImageFormat.RAW10` | Format mentah 10-bit |
| `ImageFormat.RAW12` | Format mentah 12-bit |

Untuk sebagian besar aplikasi, JPEG adalah pilihan terbaik untuk pengambilan foto.

## Membuat ImageReader

Berikut cara membuat ImageReader untuk pengambilan JPEG:

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // Simpan hingga 2 gambar di penyangga
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // Proses gambar
    image.close()
}, null)
```

## Mengambil Foto

Untuk mengambil foto, Anda perlu:
1. Membuat ImageReader
2. Menambahkan Surface-nya ke sesi pengambilan
3. Membuat permintaan pengambilan dengan `TEMPLATE_STILL_CAPTURE`
4. Mengirim permintaan ke kamera

## Contoh Lengkap Pengambilan Foto

Mari kita perluas aplikasi pratinjau kita untuk mengambil foto:

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

        // Membuat ImageReader untuk pengambilan foto
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
            Toast.makeText(this@PhotoCaptureActivity, "Konfigurasi sesi gagal", Toast.LENGTH_SHORT).show()
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
        
        // Atur autofokus ke bidikan tunggal
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
            startPreview() // Lanjutkan pratinjau setelah pengambilan
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
            Toast.makeText(this, "Foto disimpan: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menyimpan foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
        imageReader?.close()
    }
}
```

## Tata Letak

Tambahkan tombol pengambil ke tata letak Anda:

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
        android:text="Ambil Foto"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## Cara Kerjanya

1. **Buat ImageReader** — Diatur untuk menerima gambar JPEG
2. **Tambahkan Surface ke sesi** — Kamera mengirim foto ke surface ini
3. **Buat permintaan pengambilan** — Gunakan `TEMPLATE_STILL_CAPTURE` untuk foto
4. **Kirim permintaan pengambilan** — Hentikan pratinjau, ambil foto, lanjutkan pratinjau
5. **Simpan gambar** — Tulis data JPEG ke file

## CaptureRequest untuk Foto

Untuk pengambilan diam, gunakan `TEMPLATE_STILL_CAPTURE`. Template ini mengoptimalkan pengaturan untuk:
- Resolusi yang lebih tinggi
- Kualitas gambar yang lebih baik
- Autofokus bidikan tunggal

## Menangani Gambar

Selalu ingat untuk:
1. **Dapatkan gambar** — Gunakan `acquireLatestImage()`
2. **Proses gambar** — Simpan atau tampilkan gambar
3. **Tutup gambar** — Selalu panggil `image.close()` untuk melepaskan sumber daya

## Bab Berikutnya

Di bab berikutnya, kita akan mempelajari tentang pengambilan RAW dan cara bekerja dengan format gambar yang berbeda.

## Ringkasan

Mengambil foto dengan Camera2 melibatkan:

1. **ImageReader** — Menerima data gambar dari kamera
2. **Surface** — Ditambahkan ke sesi pengambilan untuk output foto
3. **TEMPLATE_STILL_CAPTURE** — Template permintaan pengambilan yang dioptimalkan
4. **CaptureCallback** — Memberi tahu saat pengambilan selesai
5. **Pemrosesan gambar** — Simpan atau tampilkan gambar yang diambil

Anda sekarang telah mempelajari cara mengambil foto dasar. Di bab berikutnya, kita akan menjelajahi pengambilan RAW dan fitur foto tingkat lanjut.
