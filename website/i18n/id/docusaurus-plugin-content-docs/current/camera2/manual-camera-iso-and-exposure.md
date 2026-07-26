---
sidebar_position: 12
title: "Bab 12: Kamera Manual - ISO dan Exposure"
description: Pelajari cara mengontrol ISO dan exposure time secara manual untuk fotografi profesional dengan Camera2.
keywords: [ISO, exposure time, kamera manual, Camera2, SENSOR_SENSITIVITY, SENSOR_EXPOSURE_TIME]
---

Kontrol manual adalah di mana kekuatan sebenarnya Camera2 bersinar. Mari kita pelajari tentang ISO dan exposure.

## Pendahuluan

Sejauh ini, kita telah menggunakan kontrol otomatis. Sekarang kita akan mengambil kendali penuh atas:

1. **ISO** — Sensitivitas sensor terhadap cahaya
2. **Exposure time** — Berapa lama sensor mengumpulkan cahaya

Kedua pengaturan ini secara langsung memengaruhi kecerahan dan kualitas gambar.

## Apa itu ISO?

ISO mengukur sensitivitas sensor terhadap cahaya. ISO yang lebih rendah berarti:
- Kurang sensitif terhadap cahaya
- Noise lebih rendah
- Kualitas gambar lebih baik

ISO yang lebih tinggi berarti:
- Lebih sensitif terhadap cahaya
- Noise lebih tinggi (grain)
- Kualitas gambar lebih rendah

Nilai ISO umum: 100, 200, 400, 800, 1600, 3200, 6400

## Apa itu Exposure Time?

Exposure time (juga disebut kecepatan rana) adalah berapa lama sensor mengumpulkan cahaya. Exposure yang lebih singkat berarti:
- Lebih sedikit cahaya yang ditangkap
- Membekukan gerakan
- Aksi lebih cepat

Exposure yang lebih lama berarti:
- Lebih banyak cahaya yang ditangkap
- Buram gerak (motion blur)
- Performa rendah cahaya lebih baik

Exposure time diukur dalam detik atau pecahan detik:
- 1/1000s — Aksi cepat
- 1/125s — Normal
- 1/30s — Lambat
- 1s — Long exposure

## Persyaratan Kontrol Manual

Untuk menggunakan kontrol manual, kamera Anda harus memiliki:
1. Tingkat perangkat keras **FULL** atau **LEVEL_3**
2. Kemampuan **MANUAL_SENSOR**

Periksa CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsManualSensor = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
) ?: false
```

## Mendapatkan Rentang yang Didukung

Sebelum mengatur nilai manual, periksa apa yang didukung kamera:

```kotlin
// Dapatkan rentang ISO
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200

// Dapatkan rentang exposure
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L
```

## Mengatur ISO dan Exposure Manual

Untuk menggunakan kontrol manual, Anda perlu:
1. Menonaktifkan auto-exposure (AE)
2. Mengatur ISO manual
3. Mengatur exposure time manual

```kotlin
val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

// Nonaktifkan AE
captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

// Atur ISO manual
captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 400)

// Atur exposure time manual (dalam nanodetik)
// 1/125s = 8.000.000ns
captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 8000000L)

// Tambahkan target
captureRequestBuilder.addTarget(surface)

// Mulai preview dengan kontrol manual
captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
```

## Segitiga Exposure

ISO, exposure time, dan aperture membentuk "segitiga exposure":

- **ISO** — Sensitivitas terhadap cahaya
- **Exposure time** — Durasi penangkapan cahaya
- **Aperture** — Jumlah cahaya yang masuk (jarang dapat diatur pada ponsel)

Mengubah satu memengaruhi yang lain. Contohnya:
- Jika Anda meningkatkan ISO, Anda bisa menggunakan kecepatan rana yang lebih cepat
- Jika Anda mengurangi exposure time, Anda mungkin perlu meningkatkan ISO

## Contoh Kontrol Manual Lengkap

```kotlin
class ManualControlActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentIso = 100
    private var currentExposure = 16000000L // 1/60s

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupControls()
    }

    private fun setupControls() {
        val isoSeekBar = findViewById<SeekBar>(R.id.isoSeekBar)
        val exposureSeekBar = findViewById<SeekBar>(R.id.exposureSeekBar)
        
        isoSeekBar.max = 31 // ISO: 100-3200 dengan langkah 100
        isoSeekBar.progress = 0
        
        exposureSeekBar.max = 9 // Exposure: 1/1000s sampai 1/10s
        exposureSeekBar.progress = 5
        
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentIso = 100 + (progress * 100)
                updatePreview()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Ubah progress menjadi exposure time (dalam nanodetik)
                // 0: 1/1000s = 1.000.000ns
                // 9: 1/10s = 100.000.000ns
                currentExposure = (1000000 * Math.pow(10.0, progress / 3.0)).toLong()
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
            
            // Nonaktifkan AE untuk kontrol manual
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            
            // Atur ISO manual
            set(CaptureRequest.SENSOR_SENSITIVITY, currentIso)
            
            // Atur exposure time manual
            set(CaptureRequest.SENSOR_EXPOSURE_TIME, currentExposure)
            
            // Biarkan AWB menyala
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        }
        
        captureRequestBuilder?.let { builder ->
            captureSession?.setRepeatingRequest(builder.build(), null, null)
        }
    }

    // ... (sisa kode setup kamera)
    
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) openCamera()
            else requestCameraPermission()
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
    
    // ... (pembukaan kamera, pembuatan sesi, dll.)
}
```

## Layout-nya

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="ISO:"/>
            
            <SeekBar
                android:id="@+id/isoSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/isoValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="100"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="16dp">
            
            <TextView
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="Exp:"/>
            
            <SeekBar
                android:id="@+id/exposureSeekBar"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"/>
            
            <TextView
                android:id="@+id/exposureValue"
                android:layout_width="60dp"
                android:layout_height="wrap_content"
                android:text="1/60s"/>
        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

## Praktik Terbaik

1. **Periksa dukungan** — Selalu verifikasi dukungan manual sensor
2. **Mulai dengan AE** — Biarkan auto-exposure mengatur nilai awal, lalu beralih ke manual
3. **Hindari ISO ekstrem** — ISO tinggi memperkenalkan noise
4. **Gunakan exposure terpendek** — Hindari motion blur jika memungkinkan
5. **Pantau histogram** — Gunakan CaptureResult untuk memeriksa exposure

## Bab Selanjutnya

Pada bab selanjutnya, kita akan mempelajari tentang kontrol fokus manual dan white balance.

## Ringkasan

Kontrol manual ISO dan exposure memberi Anda fotografi tingkat profesional:

1. **ISO** — Mengontrol sensitivitas sensor (rentang 100-3200+)
2. **Exposure time** — Mengontrol berapa lama cahaya dikumpulkan (dalam nanodetik)
3. **Nonaktifkan AE** — Harus mematikan auto-exposure untuk kontrol manual
4. **Periksa rentang** — Selalu verifikasi rentang ISO dan exposure yang didukung

Segitiga exposure (ISO, exposure time, aperture) menentukan kecerahan dan kualitas gambar. Pada bab selanjutnya, kita akan menjelajahi fokus manual dan white balance.
