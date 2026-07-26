---
sidebar_position: 13
title: "Bab 13: Kamera Manual - Fokus dan White Balance"
description: Pelajari cara mengontrol jarak fokus dan white balance secara manual untuk fotografi profesional dengan Camera2.
keywords: [fokus, white balance, kamera manual, Camera2, LENS_FOCUS_DISTANCE, COLOR_CORRECTION]
---

Kontrol manual lengkap dengan fokus dan white balance.

## Pendahuluan

Pada bab sebelumnya, Anda belajar mengontrol ISO dan eksposur. Sekarang kita akan menambahkan:

1. **Fokus** — Kontrol jarak fokus manual
2. **White balance** — Kontrol suhu warna manual

Dengan ini, Anda memiliki kontrol kreatif penuh atas foto Anda.

## Fokus Manual

Fokus menentukan bagian mana dari pemandangan yang tajam. Fokus manual memungkinkan Anda untuk:
- Fokus pada objek tertentu
- Membuat buram yang disengaja (bokeh)
- Memastikan fokus kritis dalam fotografi makro

### Mode Fokus

Camera2 mendukung beberapa mode fokus:

| Mode | Deskripsi |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | Fokus manual |
| `CONTROL_AF_MODE_AUTO` | Fokus otomatis tunggal |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | Fokus otomatis berkelanjutan untuk foto |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | Fokus otomatis berkelanjutan untuk video |
| `CONTROL_AF_MODE_MACRO` | Fokus makro |

### Jarak Fokus

Jarak fokus diukur dalam dioptri (1/meter). Nilai 0 berarti tak terhingga.

```kotlin
// Dapatkan rentang jarak fokus
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
val hyperfocalDistance = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)
```

### Mengatur Fokus Manual

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Nonaktifkan AF untuk fokus manual
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)

// Atur jarak fokus manual (dalam dioptri)
// 0.0 = tak terhingga
// 1.0 = 1 meter
// 2.0 = 0.5 meter
captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Wilayah Fokus

Anda juga dapat menentukan wilayah AF untuk fokus otomatis selektif:

```kotlin
val afRegions = listOf(
    MeteringRectangle(
        centerX,    // Pusat X dalam koordinat sensor (0-1000)
        centerY,    // Pusat Y dalam koordinat sensor (0-1000)
        width,      // Lebar wilayah
        height,     // Tinggi wilayah
        weight      // Prioritas (0-1000)
    )
)

captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions)
```

## White Balance Manual

White balance (WB) menyesuaikan suhu warna gambar. Sumber cahaya yang berbeda memiliki suhu warna yang berbeda:

- **Siang hari** — ~5500K (kebiruan)
- **Berawan** — ~6500K (lebih dingin)
- **Tungsten** — ~2800K (hangat/kuning)
- **Fluoresen** — ~4000K (kehijauan)

### Mode White Balance

| Mode | Deskripsi |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | White balance manual |
| `CONTROL_AWB_MODE_AUTO` | White balance otomatis |
| `CONTROL_AWB_MODE_INCANDESCENT` | Pencahayaan tungsten |
| `CONTROL_AWB_MODE_FLUORESCENT` | Pencahayaan fluoresen |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | Fluoresen hangat |
| `CONTROL_AWB_MODE_DAYLIGHT` | Siang hari |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | Berawan |

### Mengatur White Balance Manual

Untuk mengatur white balance manual, Anda perlu mengatur gain koreksi warna:

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Nonaktifkan AWB untuk kontrol manual
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)

// Atur gain koreksi warna (R, G, B)
// Nilai dinormalisasi (1.0 = tidak ada koreksi)
// Nilai yang lebih tinggi membuat warna tersebut lebih menonjol
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)

val gains = RggbChannelVector(1.2f, 1.0f, 0.8f, 1.0f)
captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

captureRequestBuilder.addTarget(surface)
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

### Suhu Warna

Anda juga dapat mengatur white balance menggunakan suhu warna:

```kotlin
// Dapatkan rentang suhu warna yang didukung
val tempRange = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_COLOR_TEMPERATURES)

// Atur suhu warna (dalam Kelvin)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_TEMPERATURE, 5500) // Siang hari
```

## Contoh Kamera Manual Lengkap

```kotlin
class FullManualCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    
    // Kontrol manual
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60d
    private var currentFocus = 0.0f // Tak terhingga
    private var currentWhiteBalance = 5500 // Siang hari

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        // Kontrol ISO
        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Kontrol eksposur
        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Kontrol fokus
        findViewById<SeekBar>(R.id.focusSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Ubah progress (0-100) menjadi jarak fokus (0.0 hingga 2.0 dioptri)
                currentFocus = (progress / 50.0f).coerceAtMost(2.0f)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        // Kontrol white balance
        findViewById<SeekBar>(R.id.wbSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 2800K (tungsten) hingga 6500K (berawan)
                currentWhiteBalance = 2800 + (progress * 37)
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun updatePreview() {
        val texture = textureView.surfaceTexture ?: return
        val surface = Surface(texture)
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        
        captureRequestBuilder?.apply {
            addTarget(surface)
            
            // Eksposur manual
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Fokus manual
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, currentFocus)
            
            // White balance manual
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            set(CaptureRequest.CONTROL_AWB_TEMPERATURE, currentWhiteBalance)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (kode pengaturan kamera)
}
```

## Praktik Terbaik

1. **Periksa dukungan** — Tidak semua kamera mendukung fokus manual atau WB
2. **Mulai dengan otomatis** — Biarkan kontrol otomatis menetapkan dasar
3. **Gunakan focus peaking** — Tambahkan umpan balik visual untuk akurasi fokus
4. **Kalibrasi WB** — Gunakan kartu abu-abu untuk white balance yang akurat
5. **Gabungkan kontrol** — Pengaturan manual bekerja paling baik bersama-sama

## Bab Berikutnya

Pada bab berikutnya, kita akan menjelajahi fitur profesional seperti video kecepatan tinggi dan multi-kamera.

## Ringkasan

Kontrol manual fokus dan white balance melengkapi perangkat kamera Anda:

1. **Fokus** — Kontrol apa yang tajam dalam gambar
2. **White balance** — Kontrol suhu warna
3. **Mode manual** — Nonaktifkan AF/AWB dan atur nilai secara langsung
4. **Wilayah fokus** — Targetkan area tertentu untuk fokus otomatis

Dengan ISO, eksposur, fokus, dan white balance di bawah kendali Anda, Anda dapat membuat foto berkualitas profesional. Di Bagian V, kita akan menjelajahi fitur lanjutan seperti video kecepatan tinggi dan dukungan multi-kamera.
