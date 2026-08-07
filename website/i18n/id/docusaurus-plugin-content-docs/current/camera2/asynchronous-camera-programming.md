---
sidebar_position: 26
title: "Bab 26: Pemrograman Kamera Asinkron"
description: "Jinakkan callback hell Camera2 menggunakan coroutine Kotlin dan Flow. Pelajari suspendCancellableCoroutine untuk operasi satu kali (openCamera, createCaptureSession, capture), callbackFlow untuk aliran ImageReader dan CaptureResult yang berkelanjutan, operator combine untuk UI reaktif, dan pola keamanan thread untuk mencegah ANR dan deadlock."
keywords: [coroutine kotlin camera2, callback hell, suspendcancellablecoroutine, callbackflow, flow camera2, keamanan thread camera2, shared state mutex, deadlock pipedoutputstream, ui kamera reaktif]
---

# Bab 26: Pemrograman Kamera Asinkron

## Ringkasan

Lihat kembali kode yang Anda tulis untuk Bab 7 hingga 9. `CameraDevice.StateCallback` bersarang di dalam `CameraManager.openCamera`, dengan `CameraCaptureSession.StateCallback` bersarang di dalam `onOpened`, dengan `CaptureCallback` bersarang di dalam `onConfigured`, dengan `ImageReader.OnImageAvailableListener` dipicu pada `HandlerThread` yang Anda buat secara manual dan harus dibongkar dalam urutan yang tepat di setiap jalur kesalahan. Ini adalah callback hell, gaya kamera. Setiap tingkat indentasi adalah kelas callback baru. Setiap kesalahan harus merambat melalui empat lapisan objek anonim. Setiap `close()` yang terlewatkan pada jalur pembongkaran membocorkan kamera hingga mulai ulang (reboot).

Bab ini adalah refaktor yang Anda dambakan. Kita mengubah seluruh hutan callback tersebut menjadi kode Kotlin yang bersih, linear, dapat dibatalkan, dan dapat diuji menggunakan dua primitif coroutine: `suspendCancellableCoroutine` untuk operasi satu kali, dan `callbackFlow` + operator `Flow` untuk aliran berkelanjutan. Anda akan mempelajari aturan keamanan thread untuk coroutine yang berinteraksi dengan Camera2, mengapa memblokir thread utama pada panggilan kamera apa pun adalah ANR yang tinggal menunggu waktu, dan mengapa pola `PipedOutputStream`/`PipedInputStream` yang mungkin Anda coba untuk data ImageWriter menghasilkan deadlock yang dihindari oleh Flow secara alami.

Seperti biasa, validasi kemampuan tingkat perangkat keras yang Anda targetkan dengan **Android Camera Parameters** ([Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams), [GitHub](https://github.com/zoozooll/AndroidCameraParameters)) untuk mengonfirmasi bahwa kemampuan yang dibutuhkan pipeline asinkron Anda (burst berulang, hasil parsial, pemrosesan ulang YUV) benar-benar ada pada perangkat pengujian Anda.

---

## Mengapa Callback Bersarang Disebut "Callback Hell"

Mari kita visualisasikan masalahnya terlebih dahulu. Ini adalah struktur nyata (yang disederhanakan) dari aplikasi Camera2 mentah tingkat produksi sebelum coroutine:

```mermaid
graph TD
    A["onCreateView"] -->|ID kamera dipilih| B["CameraManager.openCamera"]
    B -->|dipicu pada| C[StateCallback.onOpened<br/>lambda 1]
    C -->|memegang cameraDevice| D[createCaptureSession<br/>(output = previewSurface + imageReaderSurface)]
    D -->|dipicu pada| E[Session.StateCallback.onConfigured<br/>lambda 2]
    E -->|memegang sesi| F[session.setRepeatingRequest<br/>+ CaptureCallback lambda 3]
    F -->|dipicu onProgress| G[CaptureCallback.onCaptureProgressed<br/>hasil parsial]
    F -->|dipicu onCompleted| H[CaptureCallback.onCaptureCompleted<br/>TotalCaptureResult]
    H -->|bingkai siap| I[ImageReader.OnImageAvailableListener<br/>lambda 4]
    I -->|byte JPEG| J[panggilan simpan MediaStore<br/>lambda 5]

    style A fill:#e6d9f2
    style C fill:#f2d9e6
    style E fill:#f2d9e6
    style H fill:#f2d9e6
    style I fill:#f2d9e6
    style J fill:#d9f2e6
```

Setiap callback yang diarsir adalah kelas anonim terpisah. Masing-masing menangkap referensi ke sumber daya dua tingkat di atasnya. Setiap jalur kesalahan harus bergelembung dari J kembali ke A, menutup `imageReader → sesi → cameraDevice → handlerThread` dalam urutan terbalik, dan satu saja `close()` yang hilang dalam salah satu dari 16 permutasi kesalahan akan menghasilkan kebocoran kamera permanen hingga perangkat dimulai ulang. Ini adalah definisi tekstual dari callback hell.

Tujuan dari bab ini adalah untuk mengubah spageti tersebut menjadi ini:

```mermaid
flowchart LR
    A["openCameraAwait()"] --> B["createCaptureSessionAwait()"]
    B --> C[setRepeatingRequestFlow()]
    C --> D[aeStateFlow.map().combine(previewFlow)]
    D --> E[Status UI<br/>(emisi tunggal)]
    style A fill:#d9f2e6
    style B fill:#d9f2e6
    style C fill:#d9f2e6
    style D fill:#d9f2e6
    style E fill:#e6d9f2
```

Linear. Dapat dikomposisikan. Dapat diuji. Dapat dibatalkan dengan membatalkan `Job` induk. Setiap tahap adalah fungsi biasa atau operator `Flow`. Lima callback yang sama sekarang hidup dalam pipeline linear 12 baris.

---

## Coroutine Kotlin untuk Operasi Satu Kali: `suspendCancellableCoroutine`

Pola inti untuk membungkus API berbasis callback apa pun sebagai fungsi `suspend` adalah `suspendCancellableCoroutine`. Resepnya selalu identik:

1. Panggil `suspendCancellableCoroutine { cont -> ... }` untuk mendapatkan `CancellableContinuation<T>`.
2. Panggil API asli berbasis callback, berikan implementasi callback anonim kepadanya.
3. Di jalur sukses callback, panggil `cont.resume(value)`.
4. Di setiap jalur kesalahan, panggil `cont.resumeWithException(t)`.
5. Di `cont.invokeOnCancellation { ... }`, lakukan pembersihan: tutup kamera, batalkan permintaan yang tertunda, batalkan pendaftaran listener agar callback tidak pernah dipicu *setelah* coroutine dibatalkan.
6. Bungkus semuanya dalam `withTimeout` di tempat panggilan sehingga HAL yang mati tidak dapat menggantung aplikasi Anda selamanya.

### Contoh 1: `suspend fun openCameraAwait()`

```kotlin
suspend fun CameraManager.openCameraAwait(
    cameraId: String,
    handler: Handler
): CameraDevice = suspendCancellableCoroutine { cont ->
    val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cont.resume(camera) {
                camera.close()
            }
        }
        override fun onDisconnected(camera: CameraDevice) {
            cont.resumeWithException(
                CameraAccessException(
                    CameraAccessException.CAMERA_DISCONNECTED,
                    "Kamera $cameraId terputus saat pembukaan"
                )
            )
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cont.resumeWithException(
                CameraAccessException(error, "Kesalahan kamera $cameraId: $error")
            )
        }
    }

    cont.invokeOnCancellation {
        try {
            // Solusi: openCamera() tidak mengekspos handle yang dapat dibatalkan
            // pada versi pra-API 30. Tutup perangkat jika dibuka di jendela balapan.
        } catch (_: Throwable) { /* abaikan */ }
    }

    openCamera(cameraId, callback, handler)
}
```

**Mengapa ini berhasil.** `openCamera` bersifat kirim-dan-lupakan: Anda memanggilnya, dan pada titik tertentu di masa mendatang salah satu dari tiga metode callback dipicu tepat satu kali. Kontrak tersebut ("dipicu tepat satu kali") adalah yang memungkinkan kita memetakannya satu-ke-satu ke dalam sebuah continuation. Jika coroutine dibatalkan *sebelum* callback apa pun dipicu, `invokeOnCancellation` berjalan dan mencegah kebocoran sumber daya. Jika dibatalkan *setelah* `resume`, blok `resume(value) { camera.close() }` — parameter `onCancellation` dari `resume` — menutup perangkat secara otomatis.

Memanggilnya dengan timeout sangatlah mudah:

```kotlin
val cameraDevice: CameraDevice = withTimeoutOrNull(5_000L) {
    cameraManager.openCameraAwait(cameraId, cameraHandler)
} ?: run {
    Log.w(TAG, "Pembukaan kamera mencapai batas waktu setelah 5 detik")
    return@launch
}
```

Jika HAL macet (umum terjadi pada perangkat `LEGACY` kelas bawah setelah kebocoran kamera dari aplikasi sebelumnya), ini akan gagal dengan cepat dan bersih alih-alih menampilkan dialog "Aplikasi tidak merespons" kepada pengguna.

### Contoh 2: `suspend fun createCaptureSessionAwait()`

Pola yang sama, callback yang berbeda:

```kotlin
suspend fun CameraDevice.createCaptureSessionAwait(
    outputs: List<OutputConfiguration>,
    handler: Handler
): CameraCaptureSession = suspendCancellableCoroutine { cont ->
    val callback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            cont.resume(session) { session.close() }
        }
        override fun onConfigureFailed(session: CameraCaptureSession) {
            cont.resumeWithException(
                IllegalStateException(
                    "Konfigurasi sesi gagal untuk perangkat ${this@createCaptureSessionAwait.id}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        // Tidak dapat membatalkan pembuatan sesi yang sedang berlangsung pada API lama.
        // Sesi akan ditutup jika akhirnya selesai melalui blok onCancellation resume di atas.
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val config = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputs,
            { r -> handler.post(r) },
            callback
        )
        createCaptureSession(config)
    } else {
        @Suppress("DEPRECATION")
        createCaptureSession(outputs.map { it.surface }, callback, handler)
    }
}
```

Ini adalah bentuk yang persis sama. Dua versi API (`createCaptureSession(surfaces, callback, handler)` pra-S vs `SessionConfiguration` S+) ditangani dalam satu pembungkus. Pemanggil tidak perlu tahu.

### Contoh 3: `suspend fun awaitCaptureResult()` untuk Pengambilan Tunggal

```kotlin
suspend fun CameraCaptureSession.captureAwait(
    request: CaptureRequest,
    handler: Handler
): TotalCaptureResult = suspendCancellableCoroutine { cont ->
    val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            cont.resume(result)
        }
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            cont.resumeWithException(
                CaptureFailureException(
                    "Pengambilan gagal: alasan=${failure.reason} bingkai=${failure.frameNumber}"
                )
            )
        }
    }

    cont.invokeOnCancellation {
        try { abortCaptures() } catch (_: Throwable) { /* abaikan */ }
    }

    try {
        capture(request, callback, handler)
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}
```

Ini adalah blok bangunan untuk bracketing multi-bingkai manual gaya Bab 19 — rantaikan 7 panggilan `captureAwait(br[i])` dalam loop `for` dengan `withTimeoutOrNull`, kumpulkan ke-7 `TotalCaptureResult`, dan Anda memiliki urutan bracket HDR lengkap dengan timeout per bingkai dan pembatalan otomatis saat coroutine dibatalkan. Dalam dunia callback, ini adalah ratusan baris mesin status. Sekarang ini adalah loop `for` 12 baris.

---

## Flow untuk Aliran Berkelanjutan

Operasi satu kali mencakup pembukaan kamera, pembuatan sesi, dan pengambilan gambar tunggal. Untuk hal-hal yang berulang — setiap bingkai pratinjau, setiap `TotalCaptureResult`, setiap `Image` dari sebuah `ImageReader` — kita menginginkan sebuah `Flow<T>` sehingga kita dapat melakukan `map`, `filter`, `debounce`, `combine`, dan berbagi aliran di antara pelanggan.

### Contoh 4: ImageReader → `Flow<Image>` via `callbackFlow`

```kotlin
fun ImageReader.imagesFlow(
    lifecycleScope: CoroutineScope
): Flow<Image> = callbackFlow {
    val listener = ImageReader.OnImageAvailableListener { reader ->
        // acquireLatestImage() membuang bingkai lama jika konsumen lebih lambat dari
        // produksi kamera — wajib untuk menghindari macetnya HAL.
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        trySend(image)
    }

    // Harus panggil awaitClose TERLEBIH DAHULU agar pembatalan selalu menghapus listener
    // bahkan jika setOnImageAvailableListener itu sendiri melempar eksepsi.
    awaitClose {
        setOnImageAvailableListener(null, null)
        // JANGAN tutup ImageReader di sini — pemanggil yang memiliki siklus hidupnya.
    }

    val handlerThread = HandlerThread("ImageReaderFlow").apply { start() }
    val handler = Handler(handlerThread.looper)
    setOnImageAvailableListener(listener, handler)
}.buffer(Channel.CONFLATED)
 .onCompletion {
     // Setiap Image yang tidak dikonsumsi oleh pengumpul hilir adalah milik kita untuk ditutup,
     // karena callbackFlow melempar kembali kegagalan setelah emit.
 }
```

**Pilihan desain kritis:**

1. `acquireLatestImage()` daripada `acquireNextImage()`. Jika pemrosesan gambar Anda (inferensi ML, deteksi wajah) memakan waktu 40ms dan kamera menembak pada 30fps (~33ms per bingkai), Anda *akan* tertinggal. `acquireNextImage` mengantrekan bingkai sampai Anda kehabisan buffer gralloc dan kamera membeku. `acquireLatestImage` melompati yang lama dan memberi Anda bingkai tersegar. Ini hampir selalu yang Anda inginkan untuk analisis gambar sisi pratinjau.

2. `buffer(Channel.CONFLATED)`. Buffer yang dikonflasi hanya menyimpan nilai terbaru. Dikombinasikan dengan `acquireLatestImage`, ini adalah jaminan keras bahwa Anda tidak pernah mengantrekan bingkai basi.

3. `awaitClose { setOnImageAvailableListener(null, null) }`. Ini adalah padanan `callbackFlow` dari `cont.invokeOnCancellation`. Batalkan coroutine scope (misalnya, saat Fragment melewati `onDestroyView`) dan listener secara otomatis dibatalkan pendaftarannya serta `HandlerThread` dibersihkan. *Tidak ada* kebocoran.

### Contoh 5: CaptureCallback → `Flow<TotalCaptureResult>`

Pola yang sama:

```kotlin
fun CameraCaptureSession.repeatingResultsFlow(
    repeatingRequest: CaptureRequest,
    handler: Handler
): Flow<TotalCaptureResult> = callbackFlow {
    val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            trySend(result)
        }
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partial: CaptureResult
        ) {
            // Jika Anda butuh hasil parsial, emisi pada channel terpisah
            // atau kirim sebuah sealed class.
        }
    }

    awaitClose {
        try {
            stopRepeating()
            abortCaptures()
        } catch (_: Throwable) { /* abaikan */ }
    }

    setRepeatingRequest(repeatingRequest, callback, handler)
}
```

Sekarang Anda memiliki `Flow<TotalCaptureResult>` dingin yang memulai permintaan berulang saat dikumpulkan, menghentikannya saat pembatalan, memancarkan setiap hasil yang selesai, dan berfungsi dengan setiap operator Flow standar.

### Contoh 6: `combine(previewFlow, aeStateFlow)` untuk UI Reaktif

Kekuatan sebenarnya dari Flow adalah komposisi. Misalkan UI Anda menampilkan:
- FPS pratinjau langsung
- Status AE saat ini (memusat / terpusat / terkunci)
- Indikator "Siap memotret" yang berwarna hijau hanya saat AE terpusat DAN AF terpusat DAN AWB terpusat.

Tanpa Flow Anda menulis mesin status secara manual yang menggabungkan `CaptureCallback` dengan `Choreographer`. Dengan Flow, ini hanya tiga baris:

```kotlin
data class UiCameraState(
    val aeState: Int,
    val afState: Int,
    val awbState: Int,
    val fps: Int,
    val ready: Boolean
)

val resultFlow: Flow<TotalCaptureResult> = session
    .repeatingResultsFlow(previewRequest, cameraHandler)
    .flowOn(cameraDispatcher)   // di luar thread utama, tidak ada jank UI

val aeStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AE_STATE] ?: CaptureResult.CONTROL_AE_STATE_INACTIVE
}
val afStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AF_STATE] ?: CaptureResult.CONTROL_AF_STATE_INACTIVE
}
val awbStateFlow = resultFlow.map {
    it[CaptureResult.CONTROL_AWB_STATE] ?: CaptureResult.CONTROL_AWB_STATE_INACTIVE
}
val fpsFlow = resultFlow
    .map { it[CaptureResult.SENSOR_TIMESTAMP] }
    .runningFold(emptyList<Long>()) { acc, ts ->
        (acc + ts).takeLast(30) // jendela bergerak 30 stempel waktu
    }
    .map { timestamps ->
        if (timestamps.size < 2) 0 else {
            val windowNs = timestamps.last() - timestamps.first()
            1_000_000_000 * (timestamps.size - 1) / windowNs.toInt()
        }
    }
    .debounce(250)  // hanya perbarui label FPS setiap 250ms, hemat baterai

val uiState: Flow<UiCameraState> = combine(
    aeStateFlow, afStateFlow, awbStateFlow, fpsFlow
) { ae, af, awb, fps ->
    val aeConverged = ae in arrayOf(
        CaptureResult.CONTROL_AE_STATE_CONVERGED,
        CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
        CaptureResult.CONTROL_AE_STATE_LOCKED
    )
    val afConverged = af in arrayOf(
        CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
        CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
    )
    val awbConverged = awb in arrayOf(
        CaptureResult.CONTROL_AWB_STATE_CONVERGED,
        CaptureResult.CONTROL_AWB_STATE_LOCKED
    )
    UiCameraState(ae, af, awb, fps, ready = aeConverged && afConverged && awbConverged)
}
```

Kumpulkan `uiState` di Fragment Anda menggunakan `viewLifecycleOwner.lifecycleScope.launchWhenStarted` dan berikan setiap emisi ke UI Compose atau `viewBinding` Anda. Setiap operator — `map`, `runningFold`, `debounce`, dan yang terpenting `combine` — adalah primitif pustaka standar. Tidak ada mesin status kustom. Tidak ada kondisi balapan. Tidak ada peristiwa yang terlewatkan. Batalkan scope dan setiap Flow tunggal di hulu — termasuk permintaan berulang dan listener `ImageReader` — berhenti, membatalkan langganan, dan membersihkan tepat satu kali.

---

## Keamanan Thread

Semua hal di atas tidak berguna jika Anda melanggar aturan keamanan thread Camera2. Berikut adalah intisarinya, disaring dari ratusan laporan bug ANR:

1. **Jangan pernah memanggil API Camera2 apa pun dari thread utama.** `cameraManager.openCamera()` mungkin terlihat cepat pada Pixel 7. Pada perangkat Android Go murah dengan HAL `LEGACY`, ia dapat memblokir selama 1,2 detik. Itu adalah ANR instan. Bahkan panggilan yang *terlihat* murah, seperti `CameraCharacteristics.get()`, dapat mengalokasikan metadata beberapa KB dan menyalinnya — yang pada awal proses yang dingin saat pengguna menggeser antar Fragment sudah cukup untuk membuang 3 bingkai. Kirimkan *semuanya* ke `Dispatchers.Default` atau dispatcher satu thread khusus yang didukung oleh `HandlerThread`.

2. **HandlerThread vs `CoroutineDispatcher.Default` vs `Dispatchers.IO`.**
   - Gunakan **dispatcher satu thread** (misalnya `HandlerThread("cam").asCoroutineDispatcher()`) untuk panggilan API Camera2 yang *sebenarnya*. Tumpukan kamera lama pada banyak perangkat `LEGACY` memiliki titik masuk HAL yang terikat pada thread tertentu (thread-affine). Berpindah thread antara `openCamera` dan `createCaptureSession` memicu bug HAL yang diketahui pada Qualcomm msm8953 dan yang lebih lama.
   - Gunakan `Dispatchers.Default` untuk komputasi murni pada bingkai yang ditangkap (penggabungan HDR, enkode JPEG, deteksi wajah). Ia memiliki thread sebanyak core prosesor.
   - Gunakan `Dispatchers.IO` untuk I/O disk (menyimpan JPEG ke MediaStore). Jangan pernah gunakan `Default` untuk penulisan yang memblokir.

3. **Status yang dapat berubah (shared mutable state) di antara coroutine dan callback harus dilindungi oleh `Mutex`.** Jika sebuah `CaptureCallback` menulis `lastResult` dan klik tombol Compose membacanya, bungkus kedua sisi dengan `mutex.withLock { ... }` atau gunakan `atomicfu`/`@Volatile` untuk tipe primitif. JANGAN mengandalkan "ia hanya menyentuh satu thread." Callback HAL pada perangkat `LEGACY` terkadang dipicu pada thread yang tidak terduga, dan saat itu terjadi, Anda mendapatkan bacaan yang terpecah (torn reads) dari nilai `Long` 64-bit seperti `SENSOR_TIMESTAMP`.

4. **Mengapa Flow menghindari deadlock `PipedOutputStream`.** Jebakan `PipedOutputStream` dalam dokumen penelitian layak mendapatkan contoh konkret. Jika Anda melakukan ini:

   ```kotlin
   // JANGAN LAKUKAN INI
   val pos = PipedOutputStream()
   val pis = PipedInputStream(pos)
   lifecycleScope.launch(Dispatchers.Default) {
       while (true) { image.compressToJpeg(..., pos) }
   }
   lifecycleScope.launch(Dispatchers.IO) {
       // baca pis dan tulis ke file
   }
   ```

   Ini akan deadlock dalam 100 bingkai karena `PipedInputStream` memiliki buffer default 64KB. Jika penulis memproduksi lebih cepat daripada yang dikonsumsi pembaca, penulis akan memblokir pada `pos.write()` dan buffer penuh. Jika pembaca sementara itu diblokir pada hal lain (misalnya transaksi penyisipan massal MediaStore), kedua coroutine akan memblokir selamanya — sebuah tunggu melingkar klasik. Flow dengan `buffer(CONFLATED)` atau `buffer(DROP_OLDEST)` memiliki semantik backpressure eksplisit dan tidak pernah deadlock. Buang bingkai, jangan pernah deadlock. Itulah pertukaran yang tepat untuk pratinjau kamera.

---

## Ringkasan

API berbasis callback milik Camera2, jika disusun secara naif, menghasilkan callback hell bersarang dalam yang rawan kesalahan, rawan kebocoran, dan tidak dapat diuji. Coroutine Kotlin dan Flow memberi Anda dua primitif yang menyederhanakan seluruh desain: `suspendCancellableCoroutine` untuk operasi satu kali (`openCamera`, `createCaptureSession`, satu kali `capture`) dengan dukungan timeout dan pembatalan bawaan, dan `callbackFlow` untuk aliran berkelanjutan (gambar ImageReader, callback `CaptureResult` berulang) dengan backpressure eksplisit. Operator Flow standar — `map`, `filter`, `runningFold`, `debounce`, dan yang terpenting `combine` — memungkinkan Anda membangun pipeline status UI yang reaktif dan aman dari pembatalan dari bagian-bagian yang dapat dikomposisikan. Terapkan keamanan thread dengan dispatcher kamera khusus, lindungi status bersama dengan `Mutex`, dan ganti setiap pemipaan manual gaya `PipedOutputStream` dengan channel Flow untuk menghindari deadlock.

## Apa Selanjutnya

Anda sekarang memiliki alat untuk menulis aplikasi Camera2 yang tangguh dan berkualitas produksi. Namun bagaimana Anda memverifikasi bahwa kode Anda berfungsi di 24.000+ model perangkat Android yang saat ini ada di pasaran, dan bagaimana OEM memvalidasi HAL mereka sebelum dikirimkan? Bab 27 membahas pengujian kamera: Camera ITS, CTS Verifier, dan pengujian instrumentasi menggunakan mock sehingga Anda dapat menjalankan rangkaian pengujian kamera di server CI tanpa perangkat keras fisik apa pun.
