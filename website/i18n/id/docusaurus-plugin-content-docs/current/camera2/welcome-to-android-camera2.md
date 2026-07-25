---
sidebar_position: 1
title: "Bab 1: Selamat Datang di Android Camera2"
description: Pelajari mengapa Android Camera2 penting, apa perbedaannya dengan CameraX, dan apa yang akan dibahas dalam seri ini.
keywords: [Android Camera2, CameraX, kemampuan kamera, pengembangan kamera Android]
---

Sebelum mengontrol kamera, kita perlu memahami sistem kamera.

## Pendahuluan

Hampir setiap smartphone saat ini memiliki sistem kamera yang kuat. Ponsel modern dapat:

- Mengambil foto yang terlihat profesional
- Merekam video 4K dan 8K
- Membuat efek potret
- Memotret dalam cahaya yang sangat redup
- Mengambil video gerak lambat
- Menghasilkan informasi kedalaman
- Menggabungkan beberapa kamera secara bersamaan

Tetapi ketika Anda membuka aplikasi kamera bawaan, Anda hanya melihat antarmuka yang sederhana: tombol rana, kontrol zoom, dan beberapa mode pemotretan.

Di balik antarmuka yang sederhana ini terdapat sistem yang sangat kompleks. Aplikasi kamera berkomunikasi dengan komponen perangkat keras, prosesor gambar, dan framework Android untuk menghasilkan setiap bingkai.

Sebagai pengembang Android, kita mungkin ingin membangun aplikasi yang melampaui aplikasi kamera bawaan, seperti:

- Aplikasi fotografi manual
- Alat pengujian kamera
- Aplikasi visi komputer
- Aplikasi pemindaian 3D
- Perekam video profesional
- Penganalisis kemampuan kamera

Untuk membangun aplikasi ini, kita perlu memahami API Android Camera2.

## Apa itu Android Camera2?

Android Camera2 adalah framework kamera modern yang diperkenalkan oleh Google di Android 5.0 (API level 21). Ini menggantikan API Android Camera yang asli.

API Camera yang lama dirancang untuk dunia yang lebih sederhana: satu kamera, pengambilan foto dasar, dan perekaman video sederhana. Kamera smartphone telah berevolusi secara dramatis sejak saat itu. Perangkat modern mungkin berisi beberapa kamera belakang, lensa sudut lebar, lensa telefoto, sensor kedalaman, dan kamera eksternal.

Mereka juga mendukung fitur-fitur canggih:

- Eksposur dan fokus manual
- Pengambilan gambar RAW
- Video berkecepatan tinggi
- Pemrosesan HDR
- Stabilisasi optik

Camera2 dibuat untuk memberi pengembang kontrol yang jauh lebih dalam atas perangkat keras kamera.

## Camera2 vs. CameraX

Camera2 dan CameraX menyelesaikan masalah yang berbeda.

### CameraX

CameraX adalah pustaka tingkat tinggi yang dirancang untuk mempermudah tugas kamera umum, termasuk:

- Menampilkan pratinjau
- Mengambil foto
- Merekam video
- Menangani kompatibilitas perangkat

Sebagian besar aplikasi harus dimulai dengan CameraX.

### Camera2

Camera2 adalah framework tingkat rendah. Ini memberi pengembang akses langsung ke kemampuan kamera, termasuk informasi sensor, pengaturan eksposur, kontrol fokus, metadata kamera, kemampuan perangkat keras, dan dukungan RAW.

Camera2 lebih kompleks, tetapi memberikan kontrol yang jauh lebih besar.

| | CameraX | Camera2 |
| --- | --- | --- |
| Tingkat | Pustaka tingkat tinggi | API tingkat rendah |
| Kesulitan | Lebih mudah | Lebih kompleks |
| Kontrol | Terbatas | Luas |
| Terbaik untuk | Aplikasi kamera normal | Aplikasi kamera tingkat lanjut |

Seri tutorial ini berfokus pada Camera2 karena memahaminya membantu kita memahami bagaimana kamera Android sebenarnya bekerja.

## Mengapa mempelajari Camera2?

Anda mungkin bertanya: *Mengapa saya harus mempelajari Camera2 padahal CameraX sudah ada?*

### Memahami apa yang sebenarnya bisa dilakukan perangkat

Setiap ponsel Android berbeda. Satu perangkat mungkin mendukung pengambilan gambar RAW, video 4K 60 fps, dan kontrol manual; perangkat lain mungkin tidak. Camera2 memungkinkan aplikasi untuk menemukan kemampuan ini.

### Membangun aplikasi kamera profesional

Aplikasi yang membutuhkan fitur kamera canggih biasanya membutuhkan Camera2. Contohnya termasuk aplikasi kamera profesional, aplikasi pencitraan ilmiah, aplikasi AR, sistem visi komputer, dan alat produksi video.

### Memahami fotografi smartphone

Banyak fitur kamera modern didasarkan pada konsep yang diekspos melalui Camera2:

- Eksposur
- ISO
- Fokus
- Keseimbangan putih
- HDR
- Beberapa kamera

Mempelajari Camera2 juga mengajarkan Anda bagaimana kamera smartphone bekerja.

## Apa yang akan Anda pelajari dalam seri ini?

Seri ini dirancang untuk membawa Anda dari tingkat pemula ke tingkat lanjut.

### Bagian 1: Memahami kamera

Anda akan mempelajari cara kerja kamera smartphone, apa saja yang ada dalam perangkat keras kamera, bagaimana Android merepresentasikan kamera, dan cara memeriksa perangkat Anda sendiri.

### Bagian 2: Aplikasi Camera2 pertama Anda

Anda akan mempelajari cara menemukan dan membuka kamera, membuat pratinjau, dan mengambil gambar.

### Bagian 3: Kontrol kamera

Anda akan mempelajari tentang eksposur, ISO, fokus, keseimbangan putih, lampu kilat, dan zoom.

### Bagian 4: Fitur kamera canggih

Anda akan mempelajari tentang pengambilan gambar RAW, video berkecepatan tinggi, perangkat multi-kamera, kamera logis dan fisik, ekstensi kamera, dan fitur HDR.

### Bagian 5: Pendalaman metadata kamera

Anda akan mengeksplorasi parameter Camera2 yang penting, termasuk:

- `android.sensor.info.activeArraySize`
- `android.scaler.availableMaxDigitalZoom`
- `android.control.aeAvailableModes`
- `android.request.availableCapabilities`

Anda akan memahami tidak hanya apa arti parameter ini, tetapi juga mengapa parameter tersebut ada.

## Belajar dengan Android Camera Parameters

Membaca dokumentasi memang berguna, tetapi kemampuan kamera lebih mudah dipahami ketika Anda dapat melihat data nyata dari ponsel nyata. Di sepanjang seri ini, kita akan menggunakan [Android Camera Parameters](/) untuk mengeksplorasi informasi kamera yang sebenarnya.

Anda dapat menggunakan aplikasi ini untuk menemukan:

- Kamera yang tersedia
- Resolusi yang didukung
- Kecepatan bingkai
- Informasi sensor
- Dukungan kontrol manual
- Kemampuan RAW
- Tingkat perangkat keras

Alih-alih belajar dari contoh abstrak, Anda dapat langsung menyelidiki perangkat Anda sendiri.

## Untuk siapa tutorial ini?

Seri ini dirancang untuk:

- **Pengembang Android** yang ingin memahami sistem kamera di luar API dasar.
- **Pengembang aplikasi kamera** yang membutuhkan fitur kamera canggih.
- **Pengembang visi komputer** yang membutuhkan akses ke bingkai dan metadata kamera.
- **Pengembang yang ingin tahu** yang ingin memahami bagaimana kamera smartphone sebenarnya bekerja.

## Sebelum kita mulai membuat kode

Camera2 tidak sulit karena desain API-nya yang buruk. Camera2 sulit karena kamera modern sangatlah kuat.

Kamera smartphone bukan lagi sekadar sensor yang menangkap gambar. Ini adalah sistem pencitraan lengkap yang melibatkan:

- Perangkat keras
- Firmware
- Pemrosesan ISP
- Framework Android
- Perangkat lunak aplikasi

Camera2 mengekspos kompleksitas ini kepada pengembang. Tujuan kita dalam seri ini adalah untuk memahaminya selangkah demi selangkah.

## Bab selanjutnya

Di bab selanjutnya, **Memahami Perangkat Keras Kamera Smartphone**, kita akan meninggalkan Android sejenak dan menjelajahi kamera itu sendiri. Anda akan mempelajari apa yang dilakukan sensor kamera, mengapa sensor yang lebih besar menghasilkan gambar yang lebih baik, apa arti sebenarnya dari lensa, cara kerja pemrosesan ISP, dan mengapa dua ponsel dengan jumlah megapiksel yang sama dapat menghasilkan foto yang sangat berbeda.

Setelah Anda memahami perangkat kerasnya, konsep Camera2 akan menjadi jauh lebih mudah.

## Ringkasan

Android Camera2 adalah fondasi untuk membangun aplikasi kamera canggih di Android. Ini memberikan akses langsung ke kemampuan dan kontrol kamera yang tersembunyi di balik aplikasi kamera biasa.

Seri ini akan memandu Anda dari memahami kamera smartphone hingga membangun aplikasi Camera2 tingkat profesional. Mari kita mulai perjalanannya.
