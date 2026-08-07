---
sidebar_position: 1
slug: /
description: Ikhtisar dasbor Android Camera Parameters dan fitur diagnostik utamanya termasuk deteksi level perangkat keras dan pelacakan fitur waktu nyata.
keywords: [dasbor kamera android, deteksi level perangkat keras, diagnostik kamera]
---

# Ikhtisar Aplikasi

Halaman ini memberikan rincian mendalam tentang dasbor dan fitur utama aplikasi.

![App Overview](/img/camera_params_feature_graph.png)

## Komponen Dasbor

### 1. Navigasi & Pemilihan
- **Laci Menu**: Akses Kebijakan Privasi, Beri Nilai Aplikasi, dan informasi Tentang melalui ikon menu di pojok kiri atas.
- **Pemilihan Kamera**: Ketuk Nama Kamera atau Lencana ID (misalnya, "0") untuk membuka menu dropdown dan beralih antar lensa yang tersedia (Belakang, Depan, Ultra-lebar, dll.).
- **Navigasi Bawah**: Beralih dengan mulus antara **Ikhtisar**, **Kategori**, **JSON Mentah**, dan **Favorit**.

### 2. Kartu Ringkasan
Kartu Ringkasan di bagian atas memberikan informasi yang paling penting:
- **Tingkat Perangkat Keras**: Tingkat dukungan API Camera2 (LEGACY, LIMITED, FULL, atau LEVEL_3). Ini menentukan kemampuan keseluruhan lensa.

### 3. Kisi Fitur Utama
Kisi visual yang memberikan status instan untuk fitur tingkat profesional:
- **Resolusi & Ukuran Sensor**: Karakteristik fisik sensor.
- **FPS Video Maksimum**: Kemampuan frame rate puncak.
- **Dukungan RAW**: Menunjukkan apakah sensor dapat mengeluarkan data yang tidak terkompresi.
- **OIS (Stabilisasi Gambar Optik)**: Ketersediaan stabilisasi lensa fisik.
- **Kontrol Manual**: Status dukungan Eksposur Manual dan Fokus Manual.
- **Pemrosesan**: Dukungan untuk HDR, Deteksi Wajah, dan Pengurangan Mata Merah.

### 4. Parameter Terkategorisasi (Tab Kategori)
Jelajahi daftar lengkap CameraCharacteristics yang disusun ke dalam kelompok logis:
- **Sensor**: Resolusi, ukuran fisik, rentang sensitivitas.
- **Lensa**: Panjang fokus, bukaan (aperture), mode stabilisasi.
- **AE/AF/AWB**: Mode kontrol terperinci untuk eksposur, fokus, dan keseimbangan putih.
- **Pencarian**: Gunakan bilah pencarian terintegrasi untuk menemukan kunci API atau nilai spesifik dengan cepat.
