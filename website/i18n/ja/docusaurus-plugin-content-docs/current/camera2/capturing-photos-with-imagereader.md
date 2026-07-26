---
sidebar_position: 10
title: "第10章: ImageReaderで写真を撮影する"
description: Camera2から画像データを受け取るための主要コンポーネントであるImageReaderを使用して静止画を撮影する方法を学びます。
keywords: [ImageReader, 写真撮影, JPEG, Camera2, キャプチャリクエスト]
---

プレビューを表示できるようになったので、次は写真を撮影しましょう。ImageReaderについて学びます。

## はじめに

Camera2で写真を撮影するには、画像データを受け取る方法が必要です。そこで登場するのが**ImageReader**です。

ImageReaderはカメラとアプリケーションの間のバッファとして機能します。カメラから画像データを受け取り、処理や保存のためにアプリに提供します。

## ImageReaderとは？

ImageReaderはAndroidのクラスで、以下のことができます：
- カメラから画像データを受け取る
- 最新の撮影画像にアクセスする
- 画像フォーマットとサイズを設定する
- バッファする画像の最大数を設定する

特定のフォーマットとサイズでImageReaderを作成します：

```kotlin
val imageReader = ImageReader.newInstance(
    width,      // 画像の幅
    height,     // 画像の高さ
    format,     // 画像フォーマット（例：ImageFormat.JPEG）
    maxImages   // バッファする画像の最大数
)
```

## 画像フォーマット

Camera2はいくつかの画像フォーマットをサポートしています：

| フォーマット | 説明 |
| --- | --- |
| `ImageFormat.JPEG` | 標準の圧縮画像フォーマット |
| `ImageFormat.RAW_SENSOR` | （ISP処理前の）生センサーデータ |
| `ImageFormat.YUV_420_888` | 非圧縮YUVフォーマット |
| `ImageFormat.RAW10` | 10ビットRAWフォーマット |
| `ImageFormat.RAW12` | 12ビットRAWフォーマット |

ほとんどのアプリケーションでは、写真撮影にJPEGが最適な選択肢です。

## ImageReaderの作成

JPEG撮影用のImageReaderを作成する方法は次のとおりです：

```kotlin
val imageReader = ImageReader.newInstance(
    pictureSize.width,
    pictureSize.height,
    ImageFormat.JPEG,
    2  // バッファに最大2枚の画像を保持
)

imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage()
    // 画像を処理する
    image.close()
}, null)
```

## 写真の撮影

写真を撮影するには、以下の手順が必要です：
1. ImageReaderを作成する
2. そのSurfaceをキャプチャセッションに追加する
3. `TEMPLATE_STILL_CAPTURE`でキャプチャリクエストを作成する
4. リクエストをカメラに送信する

## 写真撮影の完全な例

プレビューアプリを拡張して写真を撮影できるようにしましょう：

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoCaptureActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null
    private var pictureSize: Size? = null
    private var imageReader: ImageReader? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_capture)
        
        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.captureButton)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
        
        captureButton.setOnClickListener {
            takePhoto()
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) return

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
        
        previewSize = previewSizes?.get(0)
        pictureSize = pictureSizes?.get(0)

        // 写真撮影用のImageReaderを作成
        imageReader = ImageReader.newInstance(
            pictureSize?.width ?: 0,
            pictureSize?.height ?: 0,
            ImageFormat.JPEG,
            2
        )
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            saveImage(image)
            image.close()
        }, null)

        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "カメラ権限が拒否されました", Toast.LENGTH_SHORT).show()
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
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val previewSurface = Surface(texture)
        val readerSurface = imageReader?.surface ?: return
        
        val surfaces = listOf(previewSurface, readerSurface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@PhotoCaptureActivity, "セッション設定に失敗しました", Toast.LENGTH_SHORT).show()
        }
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

    private fun takePhoto() {
        val readerSurface = imageReader?.surface ?: return
        
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder?.addTarget(readerSurface)
        
        // オートフォーカスをシングルショットに設定
        captureRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        
        captureRequestBuilder?.let { builder ->
            captureSession?.stopRepeating()
            captureSession?.capture(builder.build(), captureCallback, null)
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            startPreview() // 撮影後にプレビューを再開
        }
    }

    private fun saveImage(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, fileName)
        
        try {
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            Toast.makeText(this, "写真を保存しました: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "写真の保存に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureSession?.stopRepeating()
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
    }
}
```

## レイアウト

レイアウトにキャプチャボタンを追加します：

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextureView
        android:id="@+id/textureView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

    <Button
        android:id="@+id/captureButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="撮影"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="32dp"
        android:paddingHorizontal="32dp"
        android:paddingVertical="16dp"
        android:textSize="18sp"/>

</FrameLayout>
```

## 仕組み

1. **ImageReaderの作成** — JPEG画像を受け取るように設定
2. **Surfaceをセッションに追加** — カメラがこのSurfaceに写真を送信
3. **キャプチャリクエストの作成** — 写真用に`TEMPLATE_STILL_CAPTURE`を使用
4. **キャプチャリクエストの送信** — プレビューを停止し、写真を撮影し、プレビューを再開
5. **画像の保存** — JPEGデータをファイルに書き込む

## 写真用のCaptureRequest

静止画撮影には`TEMPLATE_STILL_CAPTURE`を使用します。このテンプレートは以下のために設定を最適化します：
- 高解像度
- 高画質
- シングルショットオートフォーカス

## 画像の取り扱い

常に以下のことを忘れないでください：
1. **画像を取得する** — `acquireLatestImage()`を使用
2. **処理する** — 画像を保存または表示
3. **閉じる** — リソースを解放するために常に`image.close()`を呼び出す

## 次の章

次の章では、RAW撮影とさまざまな画像フォーマットの扱い方について学びます。

## まとめ

Camera2で写真を撮影するには：

1. **ImageReader** — カメラから画像データを受け取る
2. **Surface** — 写真出力用にキャプチャセッションに追加
3. **TEMPLATE_STILL_CAPTURE** — 最適化されたキャプチャリクエストテンプレート
4. **CaptureCallback** — 撮影完了時に通知
5. **画像処理** — 撮影した画像を保存または表示

基本的な写真の撮影方法を学びました。次の章では、RAW撮影と高度な写真機能について詳しく見ていきます。
