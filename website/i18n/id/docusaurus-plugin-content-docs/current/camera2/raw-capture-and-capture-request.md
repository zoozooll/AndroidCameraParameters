---
sidebar_position: 11
title: "Bab 11: RAW Capture dan CaptureRequest"
description: Pelajari cara mengambil gambar RAW dan memahami CaptureRequest serta CaptureResult untuk kontrol kamera tingkat lanjut.
keywords: [RAW capture, CaptureRequest, CaptureResult, Camera2, kontrol manual]
---

RAW capture memberikan Anda kendali penuh atas pemrosesan gambar. Mari kita jelajahi bersama CaptureRequest dan CaptureResult.

## Pendahuluan

Pada bab sebelumnya, Anda telah mempelajari cara mengambil foto JPEG. Sekarang kita akan menjelajahi:

1. **RAW capture** — Mengambil data sensor yang belum diproses
2. **CaptureRequest** — Mengonfigurasi pengaturan kamera untuk setiap pengambilan gambar
3. **CaptureResult** — Mendapatkan metadata tentang pengambilan gambar yang telah selesai

## Apa itu RAW?

Gambar RAW berisi semua data yang diambil oleh sensor sebelum pemrosesan ISP. Ini berarti:

- Tidak ada pengurangan noise yang diterapkan
- Tidak ada koreksi white balance
- Tidak ada penajaman
- Rentang dinamis penuh

File RAW lebih besar tetapi menawarkan fleksibilitas pengeditan yang tak tertandingi.

## Persyaratan RAW Capture

Untuk mengambil gambar RAW, kamera Anda harus:
1. Memiliki tingkat perangkat keras **FULL** atau **LEVEL_3**
2. Mendukung kemampuan `RAW`

Periksa CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
```

## Mengambil Gambar RAW

Buat ImageReader dengan format RAW:

```kotlin
val rawImageReader = ImageReader.newInstance(
    rawSize.width,
    rawSize.height,
    ImageFormat.RAW_SENSOR,  // atau ImageFormat.RAW10/RAW12
    2
)
```

Kemudian tambahkan permukaan JPEG dan RAW ke sesi pengambilan gambar untuk pengambilan bersamaan:

```kotlin
val jpegSurface = jpegImageReader.surface
val rawSurface = rawImageReader.surface
val previewSurface = Surface(textureView.surfaceTexture)

val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
cameraDevice.createCaptureSession(surfaces, callback, null)
```

## CaptureRequest

CaptureRequest mendefinisikan semua pengaturan untuk satu pengambilan gambar. Anda dapat mengonfigurasi:

### Kontrol Otomatis
- `CONTROL_AF_MODE` — Mode autofokus
- `CONTROL_AE_MODE` — Mode paparan otomatis
- `CONTROL_AWB_MODE` — Mode white balance otomatis

### Kontrol Manual
- `SENSOR_SENSITIVITY` — Nilai ISO
- `SENSOR_EXPOSURE_TIME` — Waktu paparan dalam nanodetik
- `LENS_FOCUS_DISTANCE` — Jarak fokus
- `LENS_APERTURE` — Apertur (jika tersedia)

### Pengaturan Output
- `JPEG_QUALITY` — Kualitas kompresi JPEG
- `JPEG_ORIENTATION` — Orientasi gambar
- `COLOR_CORRECTION_MODE` — Mode koreksi warna

### Membuat CaptureRequest

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

// Atur kontrol otomatis
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)

// Atur kualitas JPEG
captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, 100)

// Tambahkan target
captureRequestBuilder.addTarget(jpegSurface)
captureRequestBuilder.addTarget(rawSurface)

// Bangun permintaan
val captureRequest = captureRequestBuilder.build()
```

## CaptureResult

CaptureResult berisi metadata tentang pengambilan gambar yang telah selesai. Ini mencakup:

- **Pengaturan aktual yang digunakan** — Apa yang sebenarnya diterapkan kamera
- **Statistik** — Informasi paparan, fokus, dan warna
- **Stempel waktu** — Kapan pengambilan gambar terjadi

### Mendapatkan CaptureResult

```kotlin
val captureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        
        // Dapatkan waktu paparan aktual
        val exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
        
        // Dapatkan ISO aktual
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
        
        // Dapatkan status fokus
        val focusState = result.get(CaptureResult.CONTROL_AF_STATE)
        
        // Dapatkan status AE
        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
        
        Log.d("CaptureResult", "Paparan: ${exposureTime ?: 0}ns, ISO: $iso")
    }
}
```

### Kunci CaptureResult yang Umum

| Kunci | Deskripsi |
| --- | --- |
| `SENSOR_EXPOSURE_TIME` | Waktu paparan aktual yang digunakan |
| `SENSOR_SENSITIVITY` | ISO aktual yang digunakan |
| `CONTROL_AF_STATE` | Status autofokus |
| `CONTROL_AE_STATE` | Status paparan otomatis |
| `CONTROL_AWB_STATE` | Status white balance otomatis |
| `SCALER_CROP_REGION` | Wilayah potong yang digunakan |
| `COLOR_CORRECTION_GAINS` | Penguatan koreksi warna |

## Contoh Lengkap RAW Capture

```kotlin
private fun configureDualCapture() {
    // Buat JPEG ImageReader
    val jpegReader = ImageReader.newInstance(
        jpegSize.width, jpegSize.height,
        ImageFormat.JPEG, 2
    )
    
    // Buat RAW ImageReader
    val rawReader = ImageReader.newInstance(
        rawSize.width, rawSize.height,
        ImageFormat.RAW_SENSOR, 2
    )
    
    jpegReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveJpeg(image)
        image.close()
    }, null)
    
    rawReader.setOnImageAvailableListener({ reader ->
        val image = reader.acquireLatestImage()
        saveRaw(image)
        image.close()
    }, null)
    
    // Buat permukaan
    val previewSurface = Surface(textureView.surfaceTexture)
    val jpegSurface = jpegReader.surface
    val rawSurface = rawReader.surface
    
    // Buat sesi pengambilan gambar dengan semua permukaan
    val surfaces = listOf(previewSurface, jpegSurface, rawSurface)
    cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
}

private fun captureDual() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
    
    captureRequestBuilder?.apply {
        // Tambahkan kedua permukaan sebagai target
        addTarget(jpegReader.surface)
        addTarget(rawReader.surface)
        
        // Konfigurasikan kontrol
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        set(CaptureRequest.JPEG_QUALITY, 100)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.capture(builder.build(), captureCallback, null)
    }
}

private fun saveRaw(image: Image) {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val fileName = "RAW_${System.currentTimeMillis()}.dng"
    val file = File(externalCacheDir, fileName)
    
    FileOutputStream(file).use { fos ->
        fos.write(bytes)
    }
}
```

## RAW vs JPEG

| Fitur | RAW | JPEG |
| --- | --- | --- |
| Ukuran file | Besar (20-50MB) | Kecil (2-10MB) |
| Fleksibilitas pengeditan | Maksimum | Terbatas |
| Noise | Dipertahankan | Dikurangi |
| White balance | Dapat disesuaikan | Tetap |
| Rentang dinamis | Penuh | Terkompresi |

## Praktik Terbaik

1. **Periksa dukungan RAW** — Selalu verifikasi sebelum mencoba RAW capture
2. **Pengambilan ganda** — Ambil JPEG dan RAW secara bersamaan untuk fleksibilitas
3. **Tutup gambar** — Selalu panggil `image.close()` setelah diproses
4. **Tangani format yang berbeda** — RAW_SENSOR, RAW10, dan RAW12 memiliki tata letak byte yang berbeda

## Bab Berikutnya

Pada bab berikutnya, kita akan merangkum apa yang telah Anda pelajari di Bagian III dan bersiap untuk Bagian IV: Kontrol Kamera Manual.

## Ringkasan

Dalam bab ini, Anda mempelajari tentang:

1. **RAW capture** — Mengambil data sensor yang belum diproses untuk fleksibilitas pengeditan maksimum
2. **CaptureRequest** — Mengonfigurasi pengaturan kamera untuk setiap pengambilan gambar
3. **CaptureResult** — Mendapatkan metadata tentang pengambilan gambar yang telah selesai

RAW capture memerlukan tingkat perangkat keras FULL atau LEVEL_3. Anda dapat mengambil JPEG dan RAW secara bersamaan dengan menambahkan kedua permukaan ke sesi pengambilan gambar.

CaptureRequest memungkinkan Anda mengonfigurasi autofokus, paparan otomatis, white balance, dan kontrol manual seperti ISO dan waktu paparan. CaptureResult memberi tahu Anda pengaturan apa yang sebenarnya digunakan oleh kamera.

Di Bagian IV, kita akan mendalami kontrol kamera manual: ISO, paparan, fokus, dan white balance.
