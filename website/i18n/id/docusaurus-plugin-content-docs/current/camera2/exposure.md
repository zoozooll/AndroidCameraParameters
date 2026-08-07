---
sidebar_position: 13
title: "Bab 13: Eksposur"
description: Kuasai dasar-dasar eksposur fotografi—Segitiga Eksposur dari ISO, kecepatan rana, dan bukaan (aperture). Pahami stop EV, aturan Sunny 16, dan bagaimana berbagai kombinasi menciptakan eksposur yang sama dengan pertukaran kreatif.
keywords: [android camera2, segitiga eksposur, ISO, kecepatan rana, bukaan, aperture, nilai eksposur, aturan sunny 16, dasar fotografi]
---

# Bab 13: Eksposur

## Segitiga Eksposur: Tiga Kenop, Satu Tujuan

Saat Anda mengambil foto dengan kamera smartphone, Anda sedang menangkap cahaya. *Jumlah* cahaya yang mencapai sensor menentukan apakah foto Anda terlalu gelap (underexposed), terlalu terang (overexposed), atau pas (terekspos dengan benar). Tiga kontrol fundamental mengatur hal ini — bersama-sama mereka membentuk **Segitiga Eksposur**.

```mermaid
graph TD
    A["Eksposur<br/>Cahaya Mencapai Sensor"] --> B["Kecepatan Rana<br/>Waktu Cahaya Masuk"]
    A --> C[ISO<br/>Sensitivitas Sensor]
    A --> D[Bukaan (Aperture)<br/>Ukuran Lubang]
    B <--> C[Eksposur Setara<br/>Pertukaran (Tradeoffs)]
    C <--> D
    B <--> D
    style A fill:#e74c3c,color:#fff
    style B fill:#3498db,color:#fff
    style C fill:#2ecc71,color:#fff
    style D fill:#f39c12,color:#fff
```

**Ide inti:** Setiap sudut segitiga mengontrol cahaya, tetapi masing-masing juga memperkenalkan *pertukaran kreatif*. Anda dapat mencapai eksposur total yang *sama* dengan kombinasi berbeda dari ketiga pengaturan tersebut — tetapi setiap kombinasi menghasilkan *tampilan* yang berbeda pada foto Anda.

Sebelum kita mendalami spesifikasi API Android Camera2 di bab berikutnya, mari kita bangun fondasi intuitif yang kuat untuk setiap elemen.

---

## ISO: Sensitivitas Sensor (Kontrol Gain)

Pada zaman film, **ISO** menjelaskan *sensitivitas stok film terhadap cahaya* — film ISO 100 bersifat "lambat" dan membutuhkan cahaya terang, sedangkan film ISO 800 bersifat "cepat" dan bisa memotret di dalam ruangan.

**Dalam fotografi digital (termasuk kamera smartphone), ISO adalah sensor gain / amplifikasi elektronik.** Saat Anda menggandakan nilai ISO, Anda secara efektif menggandakan amplifikasi yang diterapkan pada sinyal analog sensor sebelum didigitalkan.

### Cara Kerja ISO

Bayangkan sumur piksel sensor mengumpulkan foton (partikel cahaya). Setelah periode eksposur berakhir:

1. Setiap piksel mengubah foton yang terkumpul menjadi muatan listrik kecil
2. Sebuah **penguat gain analog (analog gain amplifier)** mengalikan sinyal ini dengan faktor yang sesuai dengan pengaturan ISO Anda
3. Sinyal yang dikuatkan diubah dari analog ke digital (ADC)
4. Pemrosesan digital kemudian menerapkan pemrosesan lebih lanjut (pengurangan noise, pemetaan nada)

**ISO 100 = basis / penguatan terendah.** Sinyal dikuatkan paling sedikit, jadi:
- Foto *bersih* dengan noise digital (bintik-bintik) minimal
- Rentang dinamis (perbedaan antara nada terekam paling terang dan paling gelap) paling tinggi
- Warna paling akurat

**ISO 3200 = penguatan tinggi.** Sinyal dikuatkan 32×:
- Anda dapat memotret di pemandangan yang lebih redup tanpa menambah waktu rana
- Tetapi Anda mendapatkan *noise yang terlihat* (bintik warna, bintik luminans)
- Rentang dinamis dan akurasi warna menurun secara signifikan

### Rentang ISO Smartphone yang Khas

| Rentang ISO | Karakteristik | Kasus Penggunaan |
|-----------|---------------|----------|
| 50–200 | ISO basis, gambar paling bersih | Cahaya siang terang, pencahayaan studio |
| 200–800 | Penguatan moderat, noise minor | Hari mendung, area teduh |
| 800–3200 | Noise terlihat, masih bisa digunakan | Pencahayaan dalam ruangan, senja |
| 3200–12800+ | Noise berat / pengurangan noise berat diterapkan | Pemandangan malam, acara rendah cahaya |

> **Catatan Realitas Smartphone:** Ponsel unggulan sering kali menerapkan pengurangan noise komputasional yang berat pada nilai ISO tinggi (pemrosesan "mode malam" khusus vendor). Saat Anda nanti menonaktifkan pipeline otomatis di Camera2, Anda akan *kehilangan* banyak optimalisasi OEM ini — sebuah peringatan kritis yang akan kita bahas di Bab 14.

---

## Kecepatan Rana (Waktu Eksposur)

**Kecepatan rana (shutter speed)** hanyalah *seberapa lama sensor terkena cahaya*. Pada kamera tradisional, rana mekanis membuka dan menutup secara fisik. Di smartphone, hampir selalu merupakan **rana elektronik** — sensor direset, dibiarkan mengumpulkan foton untuk durasi yang tepat, lalu dibaca.

Kecepatan rana diukur dalam **detik**, biasanya dinyatakan sebagai pecahan:

| Kecepatan Rana | Apa Kegunaannya | Penggunaan Umum |
|--------------|-------------|-------------|
| 1/2000d – 1/1000d | Eksposur sangat pendek, membekukan semua gerakan | Olahraga, burung, kendaraan cepat |
| 1/500d – 1/250d | Membekukan gerakan manusia biasa | Orang berjalan, anak-anak bermain |
| 1/125d – 1/60d | Kecepatan genggam "aman" dengan stabilisasi | Fotografi umum dengan tangan stabil |
| 1/30d – 1/15d | Sedikit buram gerakan (motion blur) terlihat, butuh tripod | Gerakan kreatif, cahaya rendah |
| 1d – 30d | Eksposur panjang, buram gerakan berat | Air terjun, jejak bintang, air halus |
| 30d+ | Eksposur sangat panjang (khusus) | Astrofotografi, lukisan cahaya |

### Efek Buram Gerakan (Motion Blur)

Ada **dua** alasan untuk sengaja memilih kecepatan rana tertentu di luar "cahaya yang cukup":

1. **Membekukan aksi:** Burung yang terbang pada 1/1000 detik menunjukkan setiap helai bulu dengan tajam karena burung tersebut bergerak hampir nol jarak selama eksposur.

2. **Menciptakan buram gerakan:** Air terjun pada 2 detik merender air yang bergerak sebagai jejak putih yang halus dan sutra — karena setiap tetesan air menempuh perjalanan melintasi banyak piksel pada sensor saat terekspos.

Anggap saja seperti lukisan eksposur panjang: *apa pun yang bergerak saat rana terbuka akan menjadi garis.*

**Penting untuk video:** Saat merekam video 30fps, setiap bingkai terekspos selama *maksimal* ~1/30 detik. Sinematografer mengikuti **aturan rana 180°**: atur kecepatan rana menjadi dua kali frame rate → 1/60 detik untuk video 30fps. Ini memberikan buram gerakan yang alami dan "seperti film" tanpa terlalu patah-patah atau terlalu kabur.

---

## Bukaan (Aperture)

**Bukaan (aperture)** adalah ukuran lubang di lensa yang dilewati cahaya. Ini diukur dalam **f-stop** (f/1.4, f/2.0, f/2.8, f/4.0, f/5.6, f/8.0, dll.) — sebuah *skala berlawanan dengan intuisi di mana angka yang lebih kecil = lubang yang lebih lebar*.

```
  f/1.4     f/2.0     f/2.8     f/4.0     f/5.6     f/8.0
█████████████████████████████████████████████████████████
█████████████████                              █████████
███████████████                                  ███████
█████████████                                    ██████
████████████                                      █████
███████████                                      ██████
```

**Mengurangi cahaya separuh di setiap stop:** Berpindah dari f/1.4 → f/2.0 → f/2.8 → f/4.0 masing-masing *mengurangi separuh* luas lubang, jadi separuh dari total cahaya yang masuk. Ini adalah satu "stop" lebih gelap per langkah.

### Pertukaran Bukaan (Kreatif & Praktis)

1. **Kedalaman Bidang (Depth of Field / DoF):** Bukaan lebar (f/1.8) = DoF *dangkal* — hanya bidang sempit yang fokus; segala sesuatu di depan/belakang menjadi kabur (bokeh). Bukaan sempit (f/8) = DoF *dalam* — segala sesuatu dari latar depan hingga latar belakang tajam.

2. **Pengumpulan cahaya:** f/1.4 mengumpulkan cahaya 4× lebih banyak daripada f/2.8. Inilah sebabnya "lensa cepat" (bukaan maksimum lebar) sangat dihargai untuk pemotretan cahaya rendah.

3. **Difraksi:** Pada bukaan yang sangat sempit (f/11+), gelombang cahaya berbelok di sekitar bilah bukaan, sedikit melembutkan gambar. Ini biasanya tidak relevan pada smartphone.

### Cek Realitas Smartphone

Kebanyakan smartphone memiliki **lensa bukaan tetap** — Anda tidak dapat mengubah f-stop. Ponsel anggaran mungkin memiliki f/2.4–f/2.8; ponsel unggulan sering mencapai f/1.4–f/1.8. [Aplikasi Android Camera Parameters](https://play.google.com/store/apps/details?id=com.minininja.cameraparams) memungkinkan Anda memeriksa bukaan tetap lensa Anda di `CameraCharacteristics`.

Beberapa ponsel premium (misalnya, Samsung Galaxy S23 Ultra, seri Xperia) menawarkan mekanisme *bukaan ganda* yang secara mekanis beralih di antara dua stop (misalnya, f/1.5 dan f/2.4). Di Camera2, kueri `LENS_INFO_AVAILABLE_APERTURES` untuk melihat apakah perangkat Anda mendukung banyak bukaan.

**Kesimpulan praktis:** Untuk sebagian besar pengembangan Android Camera2, bukaan bersifat *tetap*, jadi Anda mengontrol eksposur melalui **ISO + kecepatan rana saja**. Dua kenop alih-alih tiga — yang sebenarnya menyederhanakan segalanya!

---

## EV: Nilai Eksposur (Skala Logaritmik)

Saat fotografer berkata "sesuaikan satu stop," yang mereka maksud adalah **gandakan atau kurangi separuh cahaya total**. Untuk membuat pemikiran berbasis stop menjadi presisi, industri menstandardisasi pada **Exposure Value (EV)**.

**EV 0** didefinisikan sebagai kombinasi eksposur yang menghasilkan kecerahan referensi standar: **1 detik eksposur, bukaan f/1.0, ISO 100**.

Setiap **+1 EV menggandakan cahaya** (lebih terang). Setiap **−1 EV mengurangi separuh cahaya** (lebih gelap):

| Perubahan EV | Arti |
|-----------|---------|
| +3 EV | 8× cahaya lebih banyak (2³) |
| +2 EV | 4× cahaya lebih banyak |
| +1 EV | 2× cahaya lebih banyak |
| 0 EV | Referensi: 1d @ f/1.0 ISO 100 |
| −1 EV | ½ cahaya |
| −2 EV | ¼ cahaya |
| −3 EV | ⅛ cahaya |

Hal yang indah: **setiap kombinasi ISO + rana + bukaan yang berjumlah nilai EV yang sama akan menghasilkan eksposur total yang sama**. Ini adalah prinsip *eksposur setara* yang menghubungkan ketiga sudut segitiga.

### Kombinasi EV dan ISO/Rana

Dengan bukaan tetap, persamaan EV menjadi jauh lebih sederhana. Untuk smartphone pada f/1.8:

| Adegan | EV Tipikal | Rana ISO 100 | Rana ISO 400 | Rana ISO 1600 |
|-------|-----------|----------------|-----------------|------------------|
| Pantai cerah terik | 15 | 1/4000d | 1/1000d | 1/250d |
| Hari berkabut / mendung | 12 | 1/500d | 1/125d | 1/30d |
| Kantor dalam ruangan terang | 8 | 1/30d | 1/8d | 1/2d |
| Ruang tamu di malam hari | 4 | 2d | 0,5d | 1/8d |
| Adegan malam berbintang | −2 | 30d | 8d | 2d |

### Aturan Sunny 16 yang Terkenal

Sebelum matrix metering dan algoritma autoexposure yang canggih, fotografer mengandalkan aturan praktis untuk mendapatkan eksposur siang hari yang tepat tanpa meteran:

> **Pada hari yang cerah, atur bukaan ke f/16, kecepatan rana ke 1/ISO detik.**

| Sunny 16 (f/16) | Setara pada f/1.8 (Smartphone) |
|-----------------|----------------------------------|
| ISO 100, 1/100d, f/16 → EV 15 | ISO 100, 1/4000d, f/1.8 → EV 15 ✓ |
| ISO 200, 1/200d, f/16 → EV 15 | ISO 200, 1/8000d, f/1.8 → EV 15 ✓ |

Matematikanya cocok: f/1.8 sekitar **6⅓ stop lebih lebar** daripada f/16. Setiap stop empat kali lipat? Tidak — setiap stop *menggandakan* area cahaya. 2^(6,33) ≈ 80× cahaya lebih banyak. Jadi rana harus 80× lebih cepat untuk mengompensasi: 1/100d ÷ 80 ≈ 1/8000d (pada ISO 200). Cukup dekat untuk pekerjaan lapangan.

---

## Tampilan Underexposed / Benar / Overexposed

Mari kita bandingkan secara mental tiga bidikan dari adegan yang sama (misalnya, seseorang di luar ruangan dengan langit di belakangnya):

**Underexposed (−2 EV):** Subjek terlalu gelap. Bayangan *hancur (crushed)* menjadi hitam pekat tanpa detail. Dalam histogram, semua data menumpuk di sisi kiri (gelap). Langit mungkin terlihat bagus, tetapi orang tersebut tampak sebagai siluet. Anda *bisa* mencoba "mendorong" data mentah yang underexposed dalam pemrosesan pasca, tetapi bayangan akan menunjukkan noise yang berat karena Anda memperkuat sinyal yang lemah.

**Eksposur Benar (0 EV):** Nada tengah menunjukkan tekstur yang tepat. Wajah orang tersebut memiliki detail kulit yang terlihat, kerutan baju, tangkapan cahaya di mata. Histogram memiliki data yang tersebar di seluruh rentang tanpa pemotongan keras (hard clipping) di kedua ujungnya. Pada ponsel dengan rentang dinamis terbatas, ini mungkin berarti *beberapa* sorotan langit yang cerah terpotong menjadi putih (tidak ada detail biru) — itu adalah pertukaran klasik vs. membuat subjek menjadi underexposed.

**Overexposed (+2 EV):** Sorotan *terbakar (blown out)* menjadi putih murni tanpa bisa dipulihkan. Langit adalah bidang putih seragam; kancing baju yang cerah dan pantulan spekular terpotong. Wajah orang tersebut mungkin terlihat menawan (kulit cerah), tetapi Anda telah kehilangan semua detail sorotan secara permanen. Berbeda dengan bayangan underexposed (yang sering kali dapat Anda pulihkan sebagian dengan noise), *sorotan yang terbakar hilang selamanya* — sama sekali tidak ada data dalam piksel tersebut.

**Mantra Fotografer:** *Atur eksposur untuk sorotan, pulihkan bayangan.* Dalam pengambilan gambar RAW (yang akan kita bahas nanti), ini sangat ampuh karena RAW 14-bit menyimpan cukup detail bayangan untuk ditarik +2 EV atau lebih tanpa noise yang membawa bencana.

---

## Tabel Referensi EV Dunia Nyata

Menghafal beberapa nilai EV patokan memungkinkan Anda memperkirakan eksposur di mana saja:

| Adegan | EV Tipikal (pada ISO 100) | Perkiraan Rana @ f/1.8, ISO 400 |
|-------|------------------------|--------------------------------|
| Lanskap salju di bawah matahari langsung | 16 | 1/4000d |
| Pantai cerah, hari yang cerah | 15 | 1/2000d |
| Hari cerah yang khas | 14 | 1/1000d |
| Hari mendung / berawan | 12 | 1/250d |
| Sangat mendung / hujan | 11 | 1/125d |
| Teduh terbuka (orang di bayangan, latar belakang disinari matahari) | 9 | 1/30d |
| Matahari terbenam / golden hour | 7 | 1/8d |
| Kantor dalam ruangan yang terang | 8 | 1/15d |
| Ruang tamu rumah, hanya lampu | 4 | 1/2d |
| Interior restoran gelap | 2 | 2d |
| Jalanan kota di malam hari (tanda neon) | 1 | 4d |
| Lanskap malam, lampu kota yang jauh | −2 | 30d |
| Lanskap bermandikan cahaya bulan (bulan purnama) | −3 | 1 menit |
| Langit berbintang, tanpa bulan | −6 | 8 menit |

Anda dapat memverifikasi perkiraan ini terhadap apa yang sebenarnya dipilih oleh auto-exposure ponsel Anda. Luncurkan [aplikasi Android Camera Parameters](https://github.com/zoozooll/AndroidCameraParameters), buka Live Preview, dan amati `SENSOR_EXPOSURE_TIME` dan `SENSOR_SENSITIVITY` saat Anda berjalan dari matahari cerah ke ruangan gelap — Anda akan melihat nilai nyata yang memetakan secara kasar ke tabel ini.

---

## Menyatukan Semuanya: Eksposur Setara

Katakanlah Anda menginginkan *eksposur total yang sama* (EV 12 = hari mendung, smartphone f/1.8). Berikut adalah tiga kombinasi valid yang menghasilkan kecerahan sensor yang identik:

| Kombinasi | ISO | Kecepatan Rana | Tampilan & Nuansa |
|-------------|-----|---------------|-------------|
| Bersih & Tajam | 100 | 1/500d | Noise paling bersih, pembekuan gerakan paling tajam |
| Jalan Tengah | 400 | 1/125d | Noise minor, keseimbangan yang baik |
| Gerakan Halus | 1600 | 1/30d | Noise terlihat; sedikit buram pada subjek yang bergerak |

Ketiganya mendarat di EV yang sama. Ketiganya *terlihat sama cerahnya*. Tetapi *tekstur* (bintik noise) dan *penggambaran gerakan* sangat berbeda. **Itulah seni eksposur.**

### Bagaimana Jika Anda Membutuhkan Keduanya?

Di sinilah fotografi komputasional bersinar. Ponsel dalam "mode malam" tidak mengambil *satu* bidikan 2 detik — ia menangkap *lusinan* bingkai 1/60 detik (membekukan gerakan di setiap bingkai), lalu menyelaraskan dan merata-ratakannya secara komputasional. Hasilnya mendekati pengumpulan cahaya dari eksposur panjang tanpa penalti buram gerakan.

Setelah Anda memahami eksposur manual di tingkat Camera2, Anda dapat mengimplementasikan teknik seperti ini sendiri.

---

## Ringkasan

Dalam bab ini, kita membahas *dasar-dasar fotografi* tanpa menyentuh satu baris kode Android pun:

- **Segitiga Eksposur:** Kecepatan Rana (waktu), ISO (penguatan sensor), dan Bukaan (ukuran lubang) bergabung untuk mengontrol cahaya total. Masing-masing memiliki pertukaran kreatif.
- **ISO** dalam fotografi digital = penguatan sensor analog. ISO rendah = bersih, ISO tinggi = ber-noise. Smartphone umumnya mendukung ISO 100–6400+ dengan pengurangan noise OEM.
- **Kecepatan Rana** adalah waktu eksposur dalam detik. Rana cepat (1/1000d) membekukan aksi; rana lambat (1d+) menciptakan buram gerakan. Aturan rana 180° berlaku untuk video.
- **Bukaan (Aperture)** adalah lubang lensa yang dikontrol f-stop. Kebanyakan smartphone memiliki bukaan tetap, jadi kita mengandalkan ISO + rana saja.
- **EV (Exposure Value)** adalah skala stop logaritmik di mana setiap langkah ±1 menggandakan/mengurangi separuh cahaya. EV 0 = 1d @ f/1.0 ISO 100.
- **Aturan Sunny 16** dan tabel referensi EV memungkinkan Anda memperkirakan eksposur tanpa meteran.
- **Eksposur yang benar** menyeimbangkan detail nada tengah, menghindari bayangan yang hancur dan sorotan yang terbakar. RAW menjaga ruang pemulihan.

## Apa Selanjutnya

Dalam **Bab 14: Eksposur Manual di Camera2**, kita menerjemahkan seluruh model konseptual ini menjadi panggilan API Camera2 yang konkret. Anda akan mempelajari:

- Cara menonaktifkan pipeline auto-exposure (`CONTROL_MODE = OFF`, `CONTROL_AE_MODE = OFF`)
- Cara menerjemahkan nilai ISO ke `SENSOR_SENSITIVITY`
- Cara mengubah detik yang dapat dibaca manusia ↔ nanodetik untuk `SENSOR_EXPOSURE_TIME`
- Kode Kotlin yang berfungsi lengkap untuk eksposur timelapse tetap, eksposur malam panjang, dan seri bracketing eksposur 3 bidikan
- Peringatan kritis tentang pengurangan noise OEM yang dinonaktifkan saat Anda mematikan 3A

Siapkan pikiran Anda — kode dimulai selanjutnya.
