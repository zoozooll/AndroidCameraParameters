---
sidebar_position: 3
---

# Panduan Keamanan Data

Dokumen ini menguraikan pengumpulan data dan praktik privasi untuk aplikasi Android Camera Parameters.

## Ringkasan

Android Camera Parameters dirancang dengan pendekatan "Privasi Utama". Sebagai alat diagnostik, ia perlu mengakses informasi perangkat keras agar dapat berfungsi, tetapi tidak mengumpulkan atau mengirimkan data pribadi.

## Izin

### Izin Kamera (`android.permission.CAMERA`)
- **Persyaratan**: Diperlukan untuk mengakses `CameraManager` dan mengambil `CameraCharacteristics`.
- **Penggunaan**: Aplikasi hanya membaca metadata perangkat keras. Aplikasi **tidak merekam** video atau mengambil foto tanpa tindakan eksplisit dari pengguna (misalnya, di versi mendatang jika pengujian pengambilan gambar ditambahkan).

## Pengumpulan Data

- **Informasi Pribadi**: Aplikasi **tidak mengumpulkan** nama, alamat email, nomor telepon, atau pengidentifikasi pribadi lainnya.
- **Data Lokasi**: Aplikasi **tidak mengakses** GPS atau lokasi jaringan Anda.
- **Metadata Perangkat Keras**: Aplikasi membaca spesifikasi teknis lensa kamera Anda (resolusi, panjang fokus, mode yang didukung). Data ini tetap ada di perangkat Anda kecuali Anda secara eksplisit menggunakan fitur "Ekspor JSON" untuk membagikannya.

## Berbagi Data

Aplikasi **tidak membagikan** data apa pun dengan pihak ketiga. Tidak ada SDK pelacakan (seperti Firebase Analytics atau Facebook SDK) yang terintegrasi ke dalam aplikasi inti.

## Kontrol Pengguna

- **Ekspor JSON**: Pengguna dapat memilih untuk menyalin atau membagikan JSON parameter kamera mentah. Ini sepenuhnya dimulai oleh pengguna.
- **Izin**: Anda dapat mencabut izin Kamera kapan saja melalui Pengaturan Sistem Android, meskipun aplikasi tidak akan dapat menampilkan detail kamera tanpanya.
