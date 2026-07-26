---
sidebar_position: 9
title: "第9章: カメラプレビューの表示"
description: Camera2でTextureView、Surface、CameraCaptureSessionを使用してカメラプレビューを表示する方法を学びます。
keywords: [カメラプレビュー, TextureView, Surface, CameraCaptureSession, Camera2]
---

ついに、カメラプレビューが表示されます！すべてをつなぎ合わせましょう。

## はじめに

カメラを開くことは素晴らしいことですが、まだ何も見えません。カメラが捉えているものを表示するには、以下の手順が必要です：

1. プレビューを表示するTextureViewを作成する
2. TextureViewからSurfaceを取得する
3. CameraCaptureSessionを作成する
4. プレビューを開始する

ここで、各パーツがつながります。

## TextureView

TextureViewは`SurfaceTexture`を表示できるビューです。以下の理由から、カメラプレビューの表示に最適です：
- 変形（拡大縮小、回転）が可能
- ハードウェアアクセラレーションをサポート
- アニメーションやトランジションとの相性が良い

レイアウトにTextureViewを追加します：

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

</FrameLayout>
```

## Surface

Surfaceは画像データを受け取ることができるバッファです。カメラプレビューを表示するには：
1. TextureViewからSurfaceTextureを取得する
2. SurfaceTextureからSurfaceを作成する
3. SurfaceをCameraCaptureSessionに渡す

```kotlin
val surfaceTexture = textureView.surfaceTexture
surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
val surface = Surface(surfaceTexture)
```

## CameraCaptureSession

CameraCaptureSessionはキャプチャプロセスを管理します。カメラデバイスを1つ以上のSurfaceに接続します。

セッションを作成するには：
1. Surfaceのリストを準備する（プレビュー、写真撮影など用）
2. CameraDeviceで`createCaptureSession()`を呼び出す
3. コールバックを処理する

## 完全なプレビュー例

カメラプレビューを表示するアクティビティを作成しましょう：

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Collections

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSize: Size? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            // 必要に応じてサイズ変更を処理する
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // プレビューが更新されたときに呼び出される
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        if (cameraIds.isEmpty()) {
            Toast.makeText(this, "利用可能なカメラがありません", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraId = cameraIds[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        
        // プレビューサイズを取得する
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
        
        // プレビューサイズを選択する
        previewSize = previewSizes?.get(0) // 最初の利用可能なサイズを使用する

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
            Toast.makeText(this@CameraPreviewActivity, "カメラエラー", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCaptureSession() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize?.width ?: 0, previewSize?.height ?: 0)
        
        val surface = Surface(texture)
        val surfaces = listOf(surface)
        
        cameraDevice?.createCaptureSession(surfaces, captureSessionCallback, null)
    }

    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            startPreview()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Toast.makeText(this@CameraPreviewActivity, "セッション設定に失敗しました", Toast.LENGTH_SHORT).show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
    }
}
```

## 仕組み

フローを追ってみましょう：

1. **TextureViewが利用可能** — `onSurfaceTextureAvailable()`が呼び出される
2. **カメラを開く** — CameraDeviceを取得する
3. **キャプチャセッションを作成** — カメラをSurfaceに接続する
4. **プレビューを開始** — 繰り返しキャプチャリクエストを送信する

## CaptureRequest

CaptureRequestは、カメラが何をキャプチャすべきかを定義します：
- `TEMPLATE_PREVIEW` — プレビューモード用
- `TEMPLATE_STILL_CAPTURE` — 静止画用
- `TEMPLATE_RECORD` — ビデオ録画用
- `TEMPLATE_VIDEO_SNAPSHOT` — ビデオ中のスナップショット用

## プレビューループ

`setRepeatingRequest()`を呼び出すと、カメラはフレームをSurfaceに継続的に送信します。これによりライブプレビューが作成されます。

## 重要な注意点

1. **Surfaceが利用可能である必要があります** — カメラを開く前に`onSurfaceTextureAvailable()`を待つ
2. **リソースを閉じる** — 常にキャプチャセッションとカメラデバイスを閉じる
3. **向きを処理する** — デバイスの向きに応じてプレビューを回転する必要がある場合がある
4. **サイズが重要** — TextureViewの寸法に一致するプレビューサイズを選択する

## 成功！

このアプリを実行すると、画面にライブカメラプレビューが表示されるはずです。おめでとうございます！最初のCamera2プレビューアプリを作成しました。

## 次の章

プレビューを表示できるようになったので、次のステップは写真を撮影することです。パートIIIでは、以下について学びます：

1. 写真撮影用のImageReader
2. JPEGとRAWキャプチャ
3. CaptureRequestとCaptureResult

## まとめ

カメラプレビューの表示には、以下が含まれます：

1. **TextureView** — プレビューを表示するUIコンポーネント
2. **Surface** — カメラフレームを受け取るバッファ
3. **CameraCaptureSession** — キャプチャプロセスを管理する
4. **CaptureRequest** — 何をキャプチャするかを定義する
5. **setRepeatingRequest()** — 連続プレビューループを開始する

これで、このシリーズのパートIIが完了しました。以下のことができるようになりました：
- カメラを検出する
- カメラ特性を調べる
- カメラを開く
- プレビューを表示する

パートIIIでは、Camera2で写真を撮影する方法を学びます。
