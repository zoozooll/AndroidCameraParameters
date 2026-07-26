---
sidebar_position: 14
title: "Bab 14: Fitur Profesional - Video Kecepatan Tinggi"
description: Pelajari cara merekam video kecepatan tinggi dan bekerja dengan pengaturan multi-kamera di Camera2.
keywords: [video kecepatan tinggi, multi-kamera, Camera2, kecepatan tinggi terkendali, kamera logis]
---

Fitur profesional membuka kemungkinan kreatif baru. Mari kita jelajahi video kecepatan tinggi dan multi-kamera.

## Pendahuluan

Camera2 mendukung banyak fitur profesional di luar pengambilan foto dasar. Dalam bab ini, kita akan mempelajari tentang:

1. **Video kecepatan tinggi** — Merekam video gerak lambat
2. **Multi-kamera** — Bekerja dengan kamera logis dan fisik

## Video Kecepatan Tinggi

Video kecepatan tinggi memungkinkan Anda merekam video pada kecepatan bingkai yang lebih tinggi dari standar 30fps:
- 120fps — Gerak lambat halus
- 240fps — Gerak lambat standar
- 480fps — Gerak lambat ekstrem
- 960fps — Gerak lambat super

### Persyaratan

Untuk merekam video kecepatan tinggi, kamera Anda harus:
1. Mendukung kemampuan `CONSTRAINED_HIGH_SPEED_VIDEO`
2. Memiliki tingkat perangkat keras yang tepat

Periksa CameraCharacteristics:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsHighSpeed = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
) ?: false
```

### Ukuran Video Kecepatan Tinggi

Video kecepatan tinggi menggunakan resolusi yang berbeda dari video standar:

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val highSpeedSizes = configMap?.getHighSpeedVideoSizes()
```

### Merekam Video Kecepatan Tinggi

Video kecepatan tinggi memerlukan sesi pengambilan khusus:

```kotlin
// Buat sesi pengambilan kecepatan tinggi
val surfaces = listOf(previewSurface, videoSurface)

cameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
    override fun onConfigured(session: CameraCaptureSession) {
        captureSession = session
        startHighSpeedPreview()
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Toast.makeText(this@HighSpeedActivity, "Sesi kecepatan tinggi gagal", Toast.LENGTH_SHORT).show()
    }
}, null)
```

### Memulai Pratinjau Kecepatan Tinggi

```kotlin
private fun startHighSpeedPreview() {
    val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    captureRequestBuilder?.addTarget(previewSurface)
    
    // Dapatkan rentang fps kecepatan tinggi yang didukung
    val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
    
    // Temukan rentang kecepatan tinggi (misalnya, 120fps)
    val highSpeedFpsRange = fpsRanges?.find { it.upper >= 120 }
    
    captureRequestBuilder?.apply {
        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange)
        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
    }
    
    captureRequestBuilder?.let { builder ->
        captureSession?.setRepeatingRequest(builder.build(), null, null)
    }
}
```

## Multi-Kamera

Ponsel modern memiliki banyak kamera. Camera2 memperlakukannya sebagai:
- **Kamera fisik** — Sensor kamera individu
- **Kamera logis** — Kombinasi kamera fisik

### Kamera Logis vs Fisik

| Jenis | Deskripsi |
| --- | --- |
| **Fisik** | Sensor kamera tunggal (lebar, telefoto, ultra-lebar) |
| **Logis** | Kamera virtual yang menggabungkan beberapa kamera fisik |

### Mengidentifikasi Jenis Kamera

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Periksa apakah itu kamera logis
val isLogical = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
    ?: false

// Dapatkan ID kamera fisik (untuk kamera logis)
val physicalIds = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_PHYSICAL_CAMERA_IDS)
```

### Beralih Antar Kamera

Untuk beralih kamera, Anda perlu:
1. Tutup kamera saat ini
2. Buka kamera baru
3. Buat sesi pengambilan baru

```kotlin
private fun switchCamera(newCameraId: String) {
    // Tutup kamera saat ini
    captureSession?.close()
    cameraDevice?.close()
    
    // Buka kamera baru
    cameraManager.openCamera(newCameraId, cameraStateCallback, null)
}
```

### Kamera Bersamaan

Beberapa perangkat mendukung pembukaan beberapa kamera secara bersamaan:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsConcurrent = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONCURRENT_CAMERA) ?: false
```

## Ekstensi Kamera

Ekstensi kamera memungkinkan Anda menggunakan fitur kamera khusus produsen:

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
val supportsExtension = capabilities?.contains(
    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CAMERA_EXTENSION) ?: false

// Dapatkan ekstensi yang tersedia
val extensions = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EXTENSIONS)
```

Ekstensi umum:
- `EXTENSION_BOKEH` — Mode potret
- `EXTENSION_HDR` — Mode HDR
- `EXTENSION_NIGHT` — Mode malam
- `EXTENSION_AUTO` — Mode otomatis

## Contoh Multi-Kamera

```kotlin
class MultiCameraActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_camera)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        setupCameraSelector()
    }

    private fun setupCameraSelector() {
        val cameraIds = cameraManager.cameraIdList
        val spinner = findViewById<Spinner>(R.id.cameraSpinner)
        
        val cameraNames = cameraIds.map { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Kamera Depan ($id)"
                CameraCharacteristics.LENS_FACING_BACK -> "Kamera Belakang ($id)"
                else -> "Kamera $id"
            }
        }
        
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraNames)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val newCameraId = cameraIds[position]
                if (newCameraId != currentCameraId) {
                    switchCamera(newCameraId)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun switchCamera(newCameraId: String) {
        captureSession?.close()
        cameraDevice?.close()
        currentCameraId = newCameraId
        openCamera()
    }

    private fun openCamera() {
        try {
            cameraManager.openCamera(currentCameraId, cameraStateCallback, null)
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
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MultiCameraActivity, "Sesi gagal", Toast.LENGTH_SHORT).show()
            }
        }, null)
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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
    }
}
```

## Praktik Terbaik

1. **Periksa kemampuan** — Selalu verifikasi sebelum menggunakan fitur profesional
2. **Tangani transisi** — Tutup/buka dengan lancar saat beralih kamera
3. **Kelola sumber daya** — Video kecepatan tinggi mengonsumsi lebih banyak sumber daya
4. **Kembali dengan anggun** — Berikan alternatif ketika fitur tidak tersedia

## Bab Selanjutnya

Di bab terakhir, kita akan menjelajahi Ensiklopedia CameraCharacteristics — pendalaman mendalam ke parameter kamera yang paling penting.

## Ringkasan

Fitur profesional memperluas kemungkinan kreatif Anda:

1. **Video kecepatan tinggi** — Rekam gerak lambat pada 120-960fps
2. **Multi-kamera** — Bekerja dengan kamera logis dan fisik
3. **Ekstensi kamera** — Gunakan fitur khusus produsen
4. **Kamera bersamaan** — Buka beberapa kamera secara bersamaan

Di bab selanjutnya, kita akan mendalami CameraCharacteristics — ensiklopedia parameter kamera.
