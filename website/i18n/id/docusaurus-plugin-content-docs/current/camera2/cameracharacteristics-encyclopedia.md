---
sidebar_position: 15
title: "Bab 15: Ensiklopedia CameraCharacteristics"
description: Panduan komprehensif untuk karakteristik Camera2 yang paling penting, termasuk apa artinya, mengapa ada, dan cara menggunakannya.
keywords: [CameraCharacteristics, parameter kamera, kemampuan kamera, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

Selamat datang di Ensiklopedia CameraCharacteristics — panduan Anda untuk memahami setiap parameter kamera.

## Pendahuluan

CameraCharacteristics berisi ratusan parameter yang mendeskripsikan kemampuan kamera. Dalam bab ini, kita akan menjelajahi yang paling penting secara mendalam:

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — Apa yang bisa dilakukan kamera?
2. `REQUEST_AVAILABLE_CAPABILITIES` — Fitur apa saja yang tersedia?
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — Berapa ukuran sensornya?
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — Seberapa besar zoom-nya?
5. `CONTROL_AE_AVAILABLE_MODES` — Mode eksposur apa saja?

Dan masih banyak lagi...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**Apa artinya?**  
Ini adalah karakteristik yang paling penting. Ini mendefinisikan tingkat kemampuan keseluruhan perangkat kamera.

**Mengapa ada?**  
Perangkat Android yang berbeda memiliki kemampuan kamera yang berbeda. Parameter ini membantu aplikasi memahami apa yang bisa mereka lakukan.

**Nilai yang didukung:**

| Nilai | API Level | Deskripsi |
| --- | --- | --- |
| `LEGACY` | 21 | Perangkat lama, Camera2 API adalah pembungkus di atas Camera API lama |
| `LIMITED` | 21 | Fitur Camera2 dasar, tanpa kontrol manual |
| `FULL` | 21 | Kontrol manual penuh, penangkapan RAW, penangkapan burst |
| `LEVEL_3` | 24 | Fitur lanjutan seperti pemrosesan ulang YUV, 10-bit HDR |

**Bagaimana cara menggunakannya?**  
Periksa ini sebelum mencoba operasi lanjutan apa pun:

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // Fungsionalitas terbatas
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // Hanya fitur dasar
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // Kontrol manual penuh tersedia
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // Fitur lanjutan tersedia
    }
}
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Buka aplikasi dan cari "Hardware Level" di bagian Camera Info.

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**Apa artinya?**  
Array ini mencantumkan semua kemampuan yang didukung oleh kamera.

**Mengapa ada?**  
Bahkan dalam tingkat perangkat keras yang sama, perangkat yang berbeda mungkin mendukung fitur yang berbeda.

**Kemampuan umum:**

| Kemampuan | Deskripsi |
| --- | --- |
| `BACKWARD_COMPATIBLE` | Mode kompatibilitas dasar |
| `MANUAL_SENSOR` | Kontrol ISO dan eksposur manual |
| `MANUAL_POST_PROCESSING` | Koreksi warna dan pengurangan noise manual |
| `RAW` | Penangkapan gambar RAW |
| `BURST_CAPTURE` | Penangkapan burst kecepatan tinggi |
| `YUV_REPROCESSING` | Pemrosesan ulang gambar YUV |
| `DEPTH_OUTPUT` | Output peta kedalaman |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | Penangkapan video kecepatan tinggi |
| `LOGICAL_MULTI_CAMERA` | Kamera logis yang menggabungkan beberapa kamera fisik |
| `CONCURRENT_CAMERA` | Beberapa kamera dapat dibuka secara bersamaan |
| `CAMERA_EXTENSION` | Ekstensi khusus produsen (portrait, mode malam) |

**Bagaimana cara menggunakannya?**  
Periksa kemampuan sebelum menggunakan fitur:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // Aktifkan penangkapan RAW
}
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Available Capabilities" di bagian Camera Info.

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**Apa artinya?**  
Array aktif adalah area aktual sensor yang digunakan untuk menangkap gambar.

**Mengapa ada?**  
Sensor mungkin memiliki piksel di sekitar tepi yang dicadangkan untuk kalibrasi. Array aktif mewakili area yang dapat digunakan.

**Bagaimana cara menggunakannya?**  
Ini memberi tahu Anda resolusi maksimum yang tersedia untuk penangkapan:

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**Karakteristik terkait:**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — Total piksel pada sensor (mungkin lebih besar dari array aktif)
- `SENSOR_INFO_SENSOR_SIZE` — Dimensi fisik dalam milimeter

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Active Array Size" dan "Sensor Size" di bagian Sensor.

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**Apa artinya?**  
Faktor zoom digital maksimum yang didukung oleh kamera.

**Mengapa ada?**  
Zoom digital memotong dan memperbesar gambar, mengurangi kualitas. Mengetahui maksimum membantu mengelola ekspektasi pengguna.

**Bagaimana cara menggunakannya?**  
Atur tingkat zoom dalam permintaan penangkapan:

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// Atur zoom (1.0 = tanpa zoom, maxZoom = zoom maksimum)
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**Karakteristik terkait:**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — Panjang fokus fisik (untuk zoom optik)

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Max Digital Zoom" di bagian Scaler.

---

## 5. CONTROL_AE_AVAILABLE_MODES

**Apa artinya?**  
Mode eksposur otomatis yang tersedia.

**Mengapa ada?**  
Perangkat yang berbeda mendukung strategi AE yang berbeda.

**Mode umum:**

| Mode | Deskripsi |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | Kontrol eksposur manual |
| `CONTROL_AE_MODE_ON` | Eksposur otomatis |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | Eksposur otomatis dengan flash selalu menyala |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | Eksposur otomatis dengan flash otomatis |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | Eksposur otomatis dengan pengurangan mata merah |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | Eksposur otomatis dengan flash eksternal |

**Bagaimana cara menggunakannya?**  
Atur mode AE dalam permintaan penangkapan:

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "AE Available Modes" di bagian Control.

---

## 6. CONTROL_AF_AVAILABLE_MODES

**Apa artinya?**  
Mode autofokus yang tersedia.

**Mengapa ada?**  
Strategi fokus yang berbeda untuk skenario yang berbeda.

**Mode umum:**

| Mode | Deskripsi |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Fokus manual |
| `CONTROL_AF_MODE_AUTO` | Autofokus satu bidikan |
| `CONTROL_AF_MODE_MACRO` | Fokus makro |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Autofokus berkelanjutan untuk video |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Autofokus berkelanjutan untuk foto |
| `CONTROL_AF_MODE_EDGE` | Autofokus tepi |
| `CONTROL_AF_MODE_FIXED` | Fokus tetap (tanpa AF) |

**Bagaimana cara menggunakannya?**  
Atur mode AF berdasarkan kasus penggunaan Anda:

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "AF Available Modes" di bagian Control.

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**Apa artinya?**  
Mode keseimbangan putih otomatis yang tersedia.

**Mode umum:**

| Mode | Deskripsi |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | Keseimbangan putih manual |
| `CONTROL_AWB_MODE_AUTO` | Otomatis |
| `CONTROL_AWB_MODE_INCANDESCENT` | Pencahayaan tungsten |
| `CONTROL_AWB_MODE_FLUORESCENT` | Pencahayaan fluoresen |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluoresen hangat |
| `CONTROL_AWB_MODE_DAYLIGHT` | Cahaya matahari |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Berawan |

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "AWB Available Modes" di bagian Control.

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**Apa artinya?**  
Panjang fokus yang tersedia untuk lensa.

**Mengapa ada?**  
Beberapa nilai menunjukkan kemampuan zoom optik.

**Bagaimana cara menggunakannya?**  
Tentukan lensa apa saja yang tersedia:

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**Panjang fokus umum:**
- 2.4mm — Sudut lebar (umum)
- 4.8mm — Telefoto (zoom optik 2x)
- 1.8mm — Ultra lebar

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Available Focal Lengths" di bagian Lens.

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**Apa artinya?**  
Jarak terdekat di mana lensa dapat fokus.

**Mengapa ada?**  
Nilai yang lebih rendah berarti kemampuan makro yang lebih baik.

**Bagaimana cara menggunakannya?**  
Periksa apakah fotografi makro memungkinkan:

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// Nilai 0.1m (10cm) atau kurang menunjukkan kemampuan makro yang baik
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Minimum Focus Distance" di bagian Lens.

---

## 10. FLASH_INFO_AVAILABLE

**Apa artinya?**  
Apakah kamera memiliki flash.

**Mengapa ada?**  
Tidak semua kamera memiliki flash (terutama kamera depan).

**Bagaimana cara menggunakannya?**  
Periksa sebelum menggunakan flash:

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // Aktifkan fitur flash
}
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Flash Available" di bagian Flash.

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**Apa artinya?**  
Waktu eksposur minimum dan maksimum yang didukung.

**Mengapa ada?**  
Menentukan kemampuan cahaya rendah dan kemampuan membekukan gerakan.

**Bagaimana cara menggunakannya?**  
Periksa rentang eksposur untuk kontrol manual:

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// Ubah ke detik untuk ditampilkan
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Exposure Time Range" di bagian Sensor.

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**Apa artinya?**  
Nilai ISO minimum dan maksimum yang didukung.

**Mengapa ada?**  
Menentukan kemampuan cahaya rendah dan performa noise.

**Bagaimana cara menggunakannya?**  
Periksa rentang ISO untuk kontrol manual:

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Sensitivity Range" di bagian Sensor.

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**Apa artinya?**  
Semua ukuran dan format output yang didukung.

**Mengapa ada?**  
Menentukan resolusi dan format apa yang dapat Anda gunakan.

**Bagaimana cara menggunakannya?**  
Dapatkan ukuran yang didukung untuk kasus penggunaan yang berbeda:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// Ukuran pratinjau
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// Ukuran foto
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// Ukuran video
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// Ukuran RAW
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Preview Sizes", "Picture Sizes", dll. di bagian Scaler.

---

## 14. LENS_FACING

**Apa artinya?**  
Arah mana lensa menghadap.

**Mengapa ada?**  
Menentukan apakah itu kamera depan, belakang, atau eksternal.

**Nilai:**
- `LENS_FACING_FRONT` — Kamera selfie
- `LENS_FACING_BACK` — Kamera belakang  
- `LENS_FACING_EXTERNAL` — Kamera eksternal

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Lens Facing" di bagian Camera Info.

---

## 15. CONTROL_MAX_REGIONS_AE

**Apa artinya?**  
Jumlah maksimum wilayah pengukuran AE.

**Mengapa ada?**  
Menentukan seberapa presisi pengukuran eksposur bisa.

**Bagaimana cara menggunakannya?**  
Batasi jumlah wilayah AE yang Anda buat:

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// Buat tidak lebih dari maxAERegions
```

**Cara memverifikasi dengan Android Camera Parameters:**  
Cari "Max Regions AE" di bagian Control.

---

## Kesimpulan

CameraCharacteristics adalah jendela Anda ke dalam kemampuan kamera. Dengan memahami parameter-parameter ini, Anda dapat:

1. **Membangun aplikasi yang agnostik perangkat** — Periksa kemampuan sebelum menggunakan fitur
2. **Memberikan pengalaman pengguna yang lebih baik** — Tampilkan hanya fitur yang tersedia
3. **Mengoptimalkan performa** — Pilih resolusi dan format yang sesuai
4. **Membuat aplikasi profesional** — Buka potensi penuh kamera

## Cara Belajar Lebih Lanjut

1. **Aplikasi Android Camera Parameters** — Jelajahi data nyata dari perangkat Anda
2. **Dokumentasi Android** — Baca dokumentasi resmi CameraCharacteristics
3. **Eksperimen** — Tulis aplikasi uji kecil untuk mencoba parameter yang berbeda
4. **Kode sumber** — Lihat kode sumber Camera2 untuk pemahaman yang lebih dalam

## Ringkasan

Bab ini membahas CameraCharacteristics yang paling penting:

1. **Tingkat Perangkat Keras** — Kemampuan keseluruhan
2. **Kemampuan** — Fitur spesifik yang tersedia
3. **Array Aktif** — Resolusi sensor
4. **Zoom Digital** — Kemampuan zoom
5. **Mode AE/AF/AWB** — Mode kontrol otomatis
6. **Panjang Fokus** — Kemampuan lensa
7. **Jarak Fokus** — Kemampuan makro
8. **Flash** — Ketersediaan flash
9. **Rentang Eksposur/ISO** — Batas kontrol manual
10. **Konfigurasi Aliran** — Ukuran dan format yang didukung

Dengan pengetahuan ini, Anda siap membangun aplikasi Camera2 tingkat lanjut!

---

## Kata Penutup

Selamat! Anda telah menyelesaikan seri Android Camera2 ini. Anda sekarang memahami:

- **Cara kerja kamera smartphone** — Lensa, sensor, ISP
- **Cara kerja Camera2** — CameraManager, CameraDevice, CaptureSession
- **Cara menangkap foto** — JPEG, RAW, ImageReader
- **Cara mengontrol kamera** — ISO, eksposur, fokus, keseimbangan putih
- **Fitur profesional** — Video kecepatan tinggi, multi-kamera
- **Karakteristik kamera** — Ensiklopedia kemampuan kamera

Aplikasi Android Camera Parameters adalah alat yang hebat untuk terus belajar. Jelajahi kemampuan perangkat Anda dan bereksperimenlah dengan pengaturan yang berbeda.

Selamat berkoding! 📸
