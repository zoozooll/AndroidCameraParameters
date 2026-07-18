---
sidebar_position: 2
---

# Dokumentasi Pengembang

Dokumen ini memberikan ringkasan teknis aplikasi **Android Camera Parameters**, arsitekturnya, dan pedoman pengembangan.

## Ringkasan Proyek

Aplikasi ini adalah alat diagnostik untuk memeriksa `CameraCharacteristics` Android Camera2. Aplikasi ini menyediakan antarmuka modern dan ramah pengguna untuk menjelajahi tingkat perangkat keras, kemampuan, dan nilai parameter mentah untuk semua lensa kamera pada perangkat.

## Arsitektur

Proyek ini mengikuti pola arsitektur **MVVM (Model-View-ViewModel)** dan dibangun menggunakan **Jetpack Compose** untuk lapisan UI.

### Komponen Inti

#### `CameraParamsActivity`
Titik masuk tunggal aplikasi.
- Menangani izin runtime (CAMERA).
- Menginisialisasi UI Compose melalui `setContent`.
- Menghosting `CameraParamsTheme`.

#### `CameraViewModel`
Manajer status pusat untuk UI.
- Memelihara `UiState` yang mencakup daftar kamera, indeks yang dipilih, parameter yang dikategorikan, dan kueri pencarian.
- **Deteksi Fitur**: Berisi logika dalam `detectFeatureFlags()` untuk menentukan kemampuan perangkat keras secara dinamis seperti dukungan RAW, OIS, dan Eksposur Manual.
- **Kategorisasi**: Mengelompokkan ratusan kunci Camera2 ke dalam bagian logis (Sensor, Lensa, dll.) untuk keterbacaan yang lebih baik.

#### `CameraParamsHelper`
Pembungkus utilitas di sekitar Android `CameraManager`.
- Mengambil `CameraCharacteristics` untuk ID tertentu.
- Menyediakan pemformatan khusus untuk tipe kamera yang kompleks (misalnya, mengubah mode `IntArray` menjadi string yang dapat dibaca manusia).

## Lapisan UI (Jetpack Compose)

UI dibangun menggunakan **Material 3** dengan tema gelap yang diterapkan secara ketat.

### Struktur Navigasi

Aplikasi menggunakan `androidx.navigation.compose` yang dikelola di `MainScreen.kt`.

| Layar | Tanggung Jawab |
| :--- | :--- |
| **[Ringkasan](overview.md)** | Dasbor tingkat tinggi yang menunjukkan Kartu Ringkasan, Tingkat Perangkat Keras, dan chip Fitur Utama. |
| **Kategori** | Daftar yang dapat diperluas dari semua parameter yang dikelompokkan menurut bagian dengan pemfilteran pencarian. |
| **Mentah (JSON)** | Representasi JSON dengan penyorotan sintaks dari semua properti kamera. |
| **Detail** | Tampilan terfokus untuk satu parameter, menunjukkan nilai yang diformat dan data mentah. |

### Gaya

- **Tema**: Ditentukan dalam `Theme.kt`.
- **Warna**: Warna primer `#7B61FF` (Violet) digunakan untuk sorotan dan tindakan utama.
- **Permukaan**: Latar belakang gelap `#121417` dengan varian `#1E1F23` untuk kartu.

## Logika Utama

### Deteksi Fitur Dinamis

Chip "Fitur Utama" pada dasbor tidak statis. Mereka dihitung dalam `CameraViewModel.detectFeatureFlags()`:

- **RAW**: Diperiksa melalui `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS**: Terdeteksi jika `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` berisi `ON`.
- **Eksposur Manual**: Tersedia jika `CONTROL_AE_MODE_OFF` didukung.
- **Fokus Manual**: Diaktifkan jika `LENS_INFO_MINIMUM_FOCUS_DISTANCE` lebih besar dari 0.

## Panduan Pengembangan

### Prasyarat
- Android Studio Ladybug (atau yang lebih baru).
- Kotlin 2.0+ (Proyek menggunakan plugin Gradle Compose Compiler yang baru).
- SDK Minimum: 21 (Android 5.0).

### Menambahkan Kategori Baru
Untuk menambahkan atau memodifikasi pengelompokan parameter, perbarui metode `getCategoryForKey()` di `CameraViewModel.kt`. Ini menggunakan pencocokan string pada nama kunci kamera untuk menetapkannya ke kategori.

### Memperbarui Tema
Warna dapat disesuaikan di `Color.kt`. Aplikasi dirancang agar terlihat terbaik dalam mode gelap; setiap perubahan pada palet terang harus diuji dengan cermat.
