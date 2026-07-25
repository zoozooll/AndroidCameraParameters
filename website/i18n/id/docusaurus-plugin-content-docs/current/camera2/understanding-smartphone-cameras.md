---
sidebar_position: 2
title: "Bab 2: Memahami Kamera Smartphone"
description: Pelajari tentang komponen kamera smartphone termasuk lensa, sensor, ISP, dan bagaimana foto dibuat sebelum mempelajari Camera2.
keywords: [kamera smartphone, lensa kamera, sensor gambar, ISP, perangkat keras kamera]
---

Sebelum mempelajari Camera2, mari kita pahami dulu kamera yang Anda kendalikan.

## Pendahuluan

Lihat bagian belakang smartphone Anda.

Anda mungkin melihat satu kamera.

Atau dua.

Atau mungkin tiga atau bahkan lima lensa kamera.

Kamera smartphone modern sangat kuat. Beberapa dapat merekam video 8K. Beberapa dapat mengambil foto malam yang menakjubkan. Yang lain dapat menangkap gambar RAW untuk pengeditan profesional.

Tapi pernahkah Anda bertanya apa yang sebenarnya terjadi setelah Anda menekan tombol rana?

Apakah kamera hanya mengambil foto?

Jauh dari itu.

Menangkap satu foto membutuhkan beberapa komponen perangkat keras bekerja sama dalam sepersekian detik. Memahami komponen-komponen ini akan membuat pembelajaran Camera2 menjadi jauh lebih mudah.

## Kamera smartphone lebih dari sekadar lensa

Banyak orang mengira lingkaran hitam di bagian belakang ponsel adalah "kamera".

Sebenarnya, lingkaran itu hanyalah lensa. Kamera smartphone yang lengkap terdiri dari beberapa komponen utama.

```
Cahaya
│
▼
Lensa
│
▼
Sensor Gambar
│
▼
ISP (Image Signal Processor)
│
▼
Memori
│
▼
Kerangka Kamera Android
│
▼
Aplikasi Anda
```

Setiap foto mengikuti alur ini. Mari kita periksa setiap komponen.

## Lensa

Lensa adalah bagian pertama dari kamera. Tugasnya sederhana:

> Mengumpulkan cahaya dan memfokuskannya ke sensor gambar.

Lensa yang berbeda menghasilkan gambar yang berbeda. Misalnya:

- **Lensa sudut lebar** — Fotografi sehari-hari standar
- **Lensa ultra-lebar** — Menangkap lebih banyak pemandangan
- **Lensa telefoto** — Membuat objek jauh tampak lebih dekat
- **Lensa makro** — Memfokus pada objek hanya beberapa sentimeter jauhnya

Setiap lensa dirancang untuk tujuan yang berbeda.

Camera2 dapat memberitahu kita lensa apa yang dimiliki ponsel. Nanti dalam seri ini kita akan belajar bagaimana Android mengidentifikasi mereka.

## Sensor Gambar

Di belakang lensa terdapat sensor gambar. Di sinilah cahaya menjadi informasi digital.

Jutaan piksel kecil menutupi permukaan sensor. Setiap piksel mengukur jumlah cahaya yang mencapaiinya. Semakin terang cahaya, semakin besar sinyal listrik yang dihasilkan.

Kamera kemudian mengubah sinyal listrik ini menjadi nilai digital. Ini adalah gambar "raw" yang dihasilkan sensor.

**Fakta penting:** Sensor gambar menangkap cahaya, bukan warna. Kami akan menjelaskan mengapa segera.

### Mengapa sensor yang lebih besar biasanya menghasilkan foto yang lebih baik

Produsen suka mengiklankan megapiksel. Anda mungkin pernah melihat ponsel dengan:

- 48 MP
- 64 MP
- 108 MP
- 200 MP

Tetapi megapiksel hanya sebagian dari cerita.

Bayangkan dua ember mengumpulkan hujan. Ember yang lebih besar mengumpulkan lebih banyak air daripada yang lebih kecil.

Piksel bekerja sama cara. Piksel yang lebih besar mengumpulkan lebih banyak cahaya. Lebih banyak cahaya biasanya berarti:

- **Noise gambar lebih rendah**
- **Kinerja low-light yang lebih baik**
- **Rentang dinamis yang lebih tinggi**

Ini adalah alasan mengapa ponsel flagship sering menghasilkan gambar yang jauh lebih baik daripada ponsel anggaran, bahkan ketika mereka mengiklankan jumlah megapiksel yang serupa.

## ISP — Pahlawan tersembunyi

Sebagian besar orang belum pernah mendengar tentang ISP. ISP adalah singkatan dari **Image Signal Processor**. Ini adalah salah satu komponen paling penting di dalam smartphone.

Pikirkan sebagai editor foto kamera. ISP menerima data mentah dari sensor dan melakukan banyak langkah pemrosesan, termasuk:

- **Demosaicing** — Merekonstruksi warna dari piksel individu
- **Pengurangan noise** — Mengurangi butiran pada foto
- **White balance** — Memperbaiki suhu warna
- **Penyesuaian eksposur** — Mencerahkan atau menggelapkan gambar
- **Pemotongan tajam** — Meningkatkan detail
- **Penggabungan HDR** — Menggabungkan beberapa eksposur
- ** Koreksi warna** — Menyesuaikan warna untuk penampilan alami
- ** Koreksi distorsi lensa** — Memperbaiki distorsi barrel atau pincushion

Tanpa ISP, foto seringkali tampak gelap, berisik, dan tidak alami.

Dalam banyak situasi, kualitas gambar tergantung pada ISP sama seperti pada sensor kamera itu sendiri.

## Mengapa gambar RAW terlihat aneh

Tadi kita mengatakan sensor menangkap cahaya, bukan warna. Bagaimana itu mungkin?

Setiap piksel sensor hanya dapat mengukur intensitas cahaya yang masuk. Untuk merekam warna, sebagian besar sensor menggunakan **Bayer Color Filter Array**.

Setiap piksel hanya merekam satu warna:

- **Merah**
- **Hijau**
- **Biru**

ISP menggabungkan piksel tetangga untuk merekonstruksi gambar berwarna penuh. Proses ini disebut **demosaicing**.

Gambar RAW ditangkap sebelum sebagian besar pemrosesan ini terjadi. Itulah mengapa foto RAW seringkali tampak datar, lebih gelap, dan kurang berwarna dibandingkan gambar JPEG. Perangkat lunak pengeditan profesional melakukan pemrosesan yang tersisa nanti.

## Beberapa kamera menjadi standar

Sekarang banyak ponsel berisi beberapa kamera. Misalnya:

| Kamera | Tujuan umum |
| --- | --- |
| **Sudut lebar** | Fotografi sehari-hari |
| **Ultra-lebar** | Lanskap dan arsitektur |
| **Telefoto** | Zoom dan potret |
| **Makro** | Fotografi dekat |
| **Kedalaman** | Estimasi kedalaman |

Setiap kamera memiliki:

- **Lensa**
- **Sensor**
- **Karakteristik**
- **Kapabilitas**

Android Camera2 menganggap setiap kamera sebagai perangkat terpisah. Kita akan melihat ini di bab-bab selanjutnya ketika kita menjelajahi ID kamera.

## Bagaimana foto dibuat

Sekarang mari kita gabungkan semuanya. Ketika Anda menekan tombol rana:

1. **Cahaya masuk ke lensa**
2. **Lensa memfokuskan cahaya ke sensor**
3. **Sensor mengubah cahaya menjadi sinyal listrik**
4. **ISP memproses data mentah**
5. **Android menerima gambar yang diproses**
6. **Aplikasi Anda menampilkan atau menyimpan hasilnya**

Meskipun seluruh proses ini biasanya membutuhkan waktu kurang dari satu detik, banyak operasi kompleks terjadi di balik layar.

## Apa yang dapat dikontrol Camera2

Tidak setiap bagian alur kamera dikontrol oleh Android. Namun, Camera2 memungkinkan aplikasi untuk memengaruhi banyak pengaturan penting. Misalnya:

- **Eksposur** — Berapa lama sensor mengumpulkan cahaya
- **ISO** — Sensitivitas sensor
- **Fokus** — Di mana kamera memfokuskan
- **White balance** — Penyesuaian suhu warna
- **Flash** — Mengontrol flash
- **Zoom** — Zoom digital dan optik
- **Tingkat bingkai** — Tingkat bingkai video
- **Format gambar** — JPEG, RAW, YUV
- **Resolusi output** — Dimensi gambar

Selama seri ini, kita akan belajar bagaimana pengaturan ini mempengaruhi kualitas gambar.

## Jelajahi dengan Android Camera Parameters

Sebelum menulis kode apa pun, cobalah menjelajahi ponsel Anda sendiri. Buka Android Camera Parameters dan cari:

- **ID Kamera** — Bagaimana Android mengidentifikasi setiap kamera
- **Arah lensa** — Depan, belakang, atau eksternal
- **Ukuran sensor** — Dimensi fisik
- **Jarak fokus yang tersedia** — Lensa yang berbeda
- **Tingkat dukungan perangkat keras** — LEGACY, LIMITED, FULL, atau LEVEL_3
- **Zoom digital maksimum** — Kapabilitas zoom
- **Ukuran output yang didukung** — Resolusi yang tersedia

Jangan khawatir jika beberapa istilah ini tidak familiar. Pada akhir buku ini, Anda akan memahami setiap satu dari mereka.

## Bab Selanjutnya

Di bab selanjutnya, kita akan menjawab pertanyaan penting lainnya:

> Mengapa ponsel Android yang berbeda mendukung fitur kamera yang berbeda?

Anda akan belajar tentang:

- **Tingkat perangkat keras kamera** — LEGACY, LIMITED, FULL, LEVEL_3
- **Fitur opsional** — Apa yang mungkin atau tidak tersedia
- **Kapabilitas perangkat** — Menanyakan apa yang didukung kamera
- **Mengapa beberapa ponsel mendukung RAW sedangkan yang lain tidak**
- **Mengapa Camera2 berperilaku berbeda di berbagai perangkat**

Pengetahuan ini akan membantu Anda memahami mengapa aplikasi Camera2 harus selalu meminta kapabilitas kamera daripada membuat asumsi.

## Ringkasan

Kamera smartphone jauh lebih dari sekadar lensa. Ini adalah sistem pemotretan yang canggih yang terdiri dari lensa, sensor, pemroses gambar, memori, dan perangkat lunak yang bekerja sama untuk menghasilkan setiap foto.

API Camera2 memberi pengembang akses ke banyak bagian sistem ini, tetapi memahami perangkat keras dulu membuat perangkat lunak jauh lebih mudah untuk dipelajari.

Sekarang Anda tahu bagaimana kamera smartphone membuat gambar, Anda siap untuk mengetahui mengapa perangkat Android yang berbeda mengekspos kapabilitas kamera yang berbeda.