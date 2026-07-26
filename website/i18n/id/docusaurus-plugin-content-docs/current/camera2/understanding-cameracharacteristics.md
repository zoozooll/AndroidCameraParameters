---
sidebar_position: 7
title: "Bab 7: Memahami CameraCharacteristics"
description: Jelajahi CameraCharacteristics untuk mempelajari lens facing, hardware level, ukuran sensor, dan kemampuan kamera penting lainnya.
keywords: [CameraCharacteristics, lens facing, hardware level, ukuran sensor, kemampuan kamera]
---

CameraCharacteristics adalah jendela menuju jiwa kamera. Mari kita jelajahi.

## Pendahuluan

Pada bab sebelumnya, Anda mempelajari cara mendaftar kamera dan mendapatkan informasi dasar. Sekarang kita akan menyelam lebih dalam ke **CameraCharacteristics** — deskripsi komprehensif tentang kemampuan sebuah kamera.

CameraCharacteristics berisi ratusan parameter. Dalam bab ini, kita akan fokus pada yang paling penting.

## Apa itu CameraCharacteristics?

CameraCharacteristics adalah objek yang tidak dapat diubah yang berisi semua metadata tentang perangkat kamera. Objek ini mendeskripsikan:

- **Properti perangkat keras** — Ukuran sensor, karakteristik lensa
- **Kemampuan** — Apa yang dapat dilakukan kamera
- **Mode** — Mode fokus, pencahayaan, dan white balance yang tersedia
- **Opsi keluaran** — Resolusi dan format yang didukung
- **Performa** — Frame rate, rentang pencahayaan

Anda mendapatkan CameraCharacteristics dari CameraManager:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## Kunci Utama CameraCharacteristics

Mari kita jelajahi karakteristik yang paling penting.

### 1. Lens Facing

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

Nilai yang mungkin:
- `LENS_FACING_FRONT` — Kamera depan (selfie)
- `LENS_FACING_BACK` — Kamera belakang
- `LENS_FACING_EXTERNAL` — Kamera eksternal

### 2. Hardware Level

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

Ini adalah salah satu karakteristik yang paling penting:

| Level | Tingkat API | Fitur |
| --- | --- | --- |
| **LEGACY** | 21 | Dukungan Camera2 terbatas, membungkus Camera API lama |
| **LIMITED** | 21 | Fitur Camera2 dasar, tanpa kontrol manual |
| **FULL** | 21 | Kontrol manual penuh, penangkapan RAW |
| **LEVEL_3** | 24 | Fitur tingkat lanjut seperti pemrosesan ulang YUV |

### 3. Ukuran Sensor

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width dan sensorSize.height memberikan dimensinya
```

Ukuran sensor memberitahu Anda berapa banyak piksel yang dimiliki sensor. Ini berbeda dari resolusi gambar — sensor mungkin memiliki lebih banyak piksel daripada yang digunakan dalam satu penangkapan.

### 4. Active Array Size

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

Active array adalah area sebenarnya dari sensor yang digunakan untuk menangkap gambar. Ini biasanya sedikit lebih kecil dari piksel array karena beberapa piksel disediakan untuk kalibrasi.

### 5. Kemampuan yang Tersedia

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

Array ini memberitahu Anda fitur apa yang didukung oleh kamera:
- `BACKWARD_COMPATIBLE` — Kompatibilitas dasar
- `MANUAL_SENSOR` — Kontrol sensor manual
- `MANUAL_POST_PROCESSING` — Pemrosesan pasca manual
- `RAW` — Dukungan penangkapan RAW
- `BURST_CAPTURE` — Penangkapan burst
- `YUV_REPROCESSING` — Pemrosesan ulang YUV
- `DEPTH_OUTPUT` — Output kedalaman
- `CONSTRAINED_HIGH_SPEED_VIDEO` — Video kecepatan tinggi

### 6. Format Keluaran

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

Peta konfigurasi aliran berisi semua format dan ukuran keluaran yang didukung oleh kamera:
- `ImageFormat.JPEG` — JPEG standar
- `ImageFormat.RAW_SENSOR` — Data sensor RAW
- `ImageFormat.YUV_420_888` — Format YUV
- `ImageFormat.RAW10` — RAW 10-bit
- `ImageFormat.RAW12` — RAW 12-bit

### 7. Ukuran Pratinjau yang Didukung

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

Ini memberikan semua resolusi pratinjau yang tersedia untuk kamera.

### 8. Ukuran Gambar yang Didukung

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

Ini adalah resolusi yang tersedia untuk penangkapan gambar diam.

### 9. Panjang Fokus

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

Array ini berisi panjang fokus (dalam milimeter) dari lensa. Beberapa nilai menunjukkan kemampuan zoom optik.

### 10. Rentang Jarak Fokus

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

Jarak fokus minimum memberitahu Anda seberapa dekat kamera dapat memfokuskan. Nilai yang lebih kecil berarti kemampuan makro yang lebih baik.

## Contoh Praktis

Mari kita buat aplikasi info kamera yang lebih detail:

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // Lens facing
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        else -> "External"
    }
    
    // Hardware level
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "Unknown"
    }
    
    // Sensor size
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // Active array size
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // Focal lengths
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "Unknown"
    
    // Available capabilities
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
            else -> "Unknown capability"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== Camera $cameraId ===")
    Log.d("CameraDetails", "Lens Facing: $lensFacingStr")
    Log.d("CameraDetails", "Hardware Level: $hardwareLevelStr")
    Log.d("CameraDetails", "Sensor Size: $sensorSizeStr")
    Log.d("CameraDetails", "Active Array: $activeArrayStr")
    Log.d("CameraDetails", "Focal Lengths: $focalLengthsStr")
    Log.d("CameraDetails", "Capabilities: ${capabilitiesList.joinToString(", ")}")
}
```

## Contoh Keluaran

```
=== Camera 0 ===
Lens Facing: Back
Hardware Level: FULL
Sensor Size: 4032 x 3024
Active Array: 4000 x 3000
Focal Lengths: 2.4mm, 4.8mm
Capabilities: Backward Compatible, Manual Sensor, RAW Capture, Burst Capture
```

## Mengapa CameraCharacteristics Penting

Sebelum membuka kamera atau membuat sesi penangkapan, Anda **harus** memeriksa CameraCharacteristics:

1. **Verifikasi kemampuan** — Jangan asumsikan suatu fitur didukung
2. **Pilih kamera yang tepat** — Pilih berdasarkan lens facing, hardware level, dll.
3. **Konfigurasikan keluaran** — Gunakan resolusi dan format yang didukung
4. **Tangani perbedaan perangkat** — Apa yang berfungsi di satu perangkat mungkin tidak berfungsi di perangkat lain

## Jelajahi dengan Android Camera Parameters

Buka aplikasi Android Camera Parameters dan telusuri karakteristiknya. Anda akan melihat ratusan parameter yang disusun berdasarkan kategori:

- **Info Kamera** — Informasi dasar kamera
- **Sensor** — Karakteristik sensor
- **Lensa** — Properti lensa
- **Kontrol** — Auto-exposure, auto-focus, white balance
- **Scaler** — Ukuran dan format keluaran
- **Flash** — Kemampuan flash
- **Statistik** — Output statistik

Ini memberi Anda gambaran lengkap tentang kemampuan kamera Anda.

## Bab Berikutnya

Sekarang setelah Anda memahami CameraCharacteristics, Anda siap untuk membuka kamera pertama Anda! Pada bab berikutnya, kita akan:

1. Mempelajari tentang CameraDevice
2. Membuka kamera menggunakan CameraManager
3. Menangani callback status kamera
4. Memahami siklus hidup kamera

## Ringkasan

CameraCharacteristics berisi semua informasi yang Anda butuhkan untuk memahami kemampuan kamera:

- **Lens facing** — Depan, belakang, atau eksternal
- **Hardware level** — LEGACY, LIMITED, FULL, LEVEL_3
- **Ukuran sensor** — Dimensi fisik
- **Active array** — Area penangkapan
- **Panjang fokus** — Kemampuan lensa
- **Kemampuan** — Fitur yang didukung
- **Format keluaran** — Format gambar yang tersedia

Selalu periksa CameraCharacteristics sebelum menggunakan kamera. Ini memastikan aplikasi Anda berfungsi di berbagai perangkat.

Pada bab berikutnya, kita akan membuka kamera pertama kita menggunakan CameraDevice.
