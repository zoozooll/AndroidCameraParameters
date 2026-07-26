---
sidebar_position: 5
title: "Bab 5: Memulai dengan Camera2"
description: Pelajari tentang CameraManager, titik masuk ke Android Camera2 API yang memungkinkan Anda mendaftarkan kamera dan mengakses karakteristiknya.
keywords: [CameraManager, Camera2 API, kamera Android, pendaftaran kamera]
---

Selamat datang di bagian pemrograman seri ini. Mari kita mulai dengan dasarnya: CameraManager.

## Pendahuluan

Sebelum Anda dapat menggunakan kamera apa pun, Anda memerlukan cara untuk menemukan dan mengaksesnya. Di situlah **CameraManager** berperan.

CameraManager adalah gerbang menuju Camera2 API. Ini adalah kelas pertama yang akan Anda gunakan dalam aplikasi Camera2 apa pun.

## Apa itu CameraManager?

CameraManager adalah layanan sistem yang mengelola semua perangkat kamera pada perangkat Android. Anggap saja sebagai direktori atau pendaftaran kamera.

Tanggung jawab utamanya adalah:
1. **Mendaftarkan kamera** — Mencantumkan semua kamera yang tersedia
2. **Mendapatkan karakteristik kamera** — Mengambil informasi mendetail tentang setiap kamera
3. **Membuka kamera** — Membuat CameraDevice untuk pengambilan gambar

## Mendapatkan CameraManager

Di Android, layanan sistem diperoleh melalui `Context`. Berikut cara mendapatkan CameraManager:

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

Selesai. Satu baris kode untuk mendapatkan akses ke semua kamera di perangkat.

## Izin Terlebih Dahulu

Sebelum menggunakan CameraManager, Anda perlu meminta izin kamera. Tambahkan ini ke `AndroidManifest.xml` Anda:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

Dan minta izin runtime di aktivitas Anda:

```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED
) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        CAMERA_PERMISSION_REQUEST
    )
}
```

Selalu periksa izin sebelum mengakses kamera.

## Metode CameraManager

CameraManager memiliki tiga metode utama yang akan Anda gunakan:

### 1. `getCameraIdList()`

Mengembalikan array string ID kamera. Setiap ID mewakili sebuah perangkat kamera.

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "Menemukan kamera: $id")
}
```

Ini mungkin menghasilkan output:
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

Mengembalikan objek `CameraCharacteristics` yang berisi semua detail tentang kamera tertentu.

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristics berisi ratusan parameter yang menggambarkan kemampuan kamera.

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

Membuka kamera dan mengembalikan `CameraDevice` melalui callback. Kita akan membahas ini secara mendetail nanti.

## Meninjau Kembali ID Kamera

Ingat dari Bab 4 bahwa Android menetapkan ID numerik ke kamera. ID tersebut tidak dijamin konsisten di seluruh perangkat atau bahkan di seluruh reboot.

Pola umum:
- **Kamera 0** — Biasanya kamera belakang lebar
- **Kamera 1** — Seringkali kamera depan
- **Kamera 2** — Biasanya kamera ultra-lebar atau telefoto
- Angka yang lebih tinggi — Kamera tambahan (makro, kedalaman, dll.)

Namun **jangan pernah mengasumsikan** arti dari ID kamera. Selalu periksa karakteristik kamera untuk menentukan:
- Arah lensa (depan/belakang/eksternal)
- Panjang fokus
- Kemampuan

## Mengapa CameraManager Penting

CameraManager adalah dasar untuk semua yang akan kita lakukan dengan Camera2:

1. **Penemuan** — Sebelum menggunakan kamera, Anda perlu menemukannya
2. **Informasi** — Sebelum membuka kamera, Anda perlu mengetahui kemampuannya
3. **Akses** — CameraManager menyediakan satu-satunya cara untuk membuka perangkat kamera

## Contoh Sederhana

Mari kita gabungkan semuanya dalam contoh sederhana:

```kotlin
class CameraDiscoveryActivity : AppCompatActivity() {
    
    private lateinit var cameraManager: CameraManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            discoverCameras()
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
    
    private fun discoverCameras() {
        val cameraIds = cameraManager.cameraIdList
        
        Log.d("CameraDiscovery", "Menemukan ${cameraIds.size} kamera")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Depan"
                CameraCharacteristics.LENS_FACING_BACK -> "Belakang"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Eksternal"
                else -> "Tidak diketahui"
            }
            
            Log.d("CameraDiscovery", "Kamera $cameraId: $lensFacingStr")
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
                discoverCameras()
            } else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

Aktivitas sederhana ini menemukan semua kamera dan mencatat ID serta arah lensanya.

## Poin Penting

- **CameraManager** adalah titik masuk ke Camera2
- Gunakan `getCameraIdList()` untuk menemukan semua kamera
- Gunakan `getCameraCharacteristics()` untuk mendapatkan informasi mendetail
- Selalu minta izin kamera terlebih dahulu
- Jangan pernah mengasumsikan arti ID kamera — periksa karakteristiknya

## Bab Berikutnya

Sekarang setelah Anda memahami CameraManager, saatnya menulis program Camera2 nyata pertama Anda. Di bab berikutnya, kita akan:

1. Membuat aplikasi Android sederhana
2. Mencantumkan semua kamera yang tersedia
3. Menampilkan informasi kamera kepada pengguna

Anda akan menulis kode Camera2 pertama Anda dan melihat hasil nyata!

## Ringkasan

CameraManager adalah dasar dari Camera2. Ini menyediakan akses ke:
- Pendaftaran kamera
- Karakteristik kamera
- Pembukaan kamera

Dengan CameraManager, Anda dapat menemukan kamera apa saja yang tersedia dan mempelajari kemampuannya sebelum membukanya.

Di bab berikutnya, kita akan menulis program Camera2 pertama kita yang mencantumkan semua kamera di perangkat.
