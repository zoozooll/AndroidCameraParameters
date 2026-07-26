---
sidebar_position: 8
title: "第8章: 最初のカメラを開く"
description: CameraManagerを使用してCameraDeviceを開き、ステートコールバックでカメラのライフサイクルを処理する方法を学びます。
keywords: [CameraDevice, openCamera, カメラライフサイクル, CameraManager]
---

最初のカメラを開く準備が整いました！CameraDeviceについて学びましょう。

## はじめに

これまで、カメラを検出して特性を調べる方法を学びました。次のステップに進みましょう：**カメラを開く**ことです。

カメラを開くと、実際のカメラハードウェアにアクセスできるようになります。開いた後は、キャプチャセッションの作成、プレビューの表示、写真の撮影ができます。

## CameraDeviceとは？

CameraDeviceは、Androidデバイスに接続された1台のカメラを表します。以下のメソッドを提供します：
- キャプチャセッションの作成
- 静止画の撮影
- プレビューの開始と停止

CameraDeviceを直接作成することはできません。代わりに、CameraManagerから`openCamera()`を呼び出して取得します。

## カメラを開く

カメラを開く方法は以下の通りです：

```kotlin
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // カメラが使用可能になりました
        val cameraDevice = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        // カメラが切断されました
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        // カメラエラーが発生しました
        camera.close()
    }
}, null)
```

詳しく見ていきましょう。

### StateCallback

カメラを開く処理は非同期であるため、CameraDeviceはコールバックパターンを使用します。コールバックには3つの主要なメソッドがあります：

#### 1. `onOpened(camera: CameraDevice)`

カメラが正常に開かれたときに呼び出されます。ここでCameraDeviceインスタンスを取得します。

#### 2. `onDisconnected(camera: CameraDevice)`

カメラが切断されたときに呼び出されます。これは他のアプリがカメラを使用している場合や、デバイスがシャットダウンされた場合に発生します。このコールバックでは必ずカメラを閉じてください。

#### 3. `onError(camera: CameraDevice, error: Int)`

エラーが発生したときに呼び出されます。一般的なエラーコード：
- `ERROR_CAMERA_IN_USE` — カメラはすでに使用中です
- `ERROR_MAX_CAMERAS_IN_USE` — 開いているカメラが多すぎます
- `ERROR_CAMERA_DISABLED` — カメラは無効になっています
- `ERROR_CAMERA_DEVICE` — カメラハードウェアエラー
- `ERROR_CAMERA_SERVICE` — カメラサービスエラー

### Handler

3番目のパラメータは`Handler`です。`null`を渡すと、コールバックは呼び出し元のスレッドのルーパーで実行されます。UIを更新する場合は、メインスレッドで実行されるハンドラーを渡すとよいでしょう。

## 完全な例

カメラを開くアクティビティを作成しましょう：

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraOpenActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            openCamera()
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

    private fun openCamera() {
        val cameraIds = cameraManager.cameraIdList
        
        if (cameraIds.isEmpty()) {
            Toast.makeText(this, "利用可能なカメラがありません", Toast.LENGTH_SHORT).show()
            return
        }
        
        val cameraId = cameraIds[0] // 最初のカメラを開く
        
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, null)
        } catch (e: SecurityException) {
            Toast.makeText(this, "カメラパーミッションが拒否されました", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "無効なカメラIDです", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            Toast.makeText(this@CameraOpenActivity, "カメラが正常に開かれました！", Toast.LENGTH_SHORT).show()
            Log.d("CameraOpen", "カメラ ${camera.id} が開かれました")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice = null
            camera.close()
            Toast.makeText(this@CameraOpenActivity, "カメラが切断されました", Toast.LENGTH_SHORT).show()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice = null
            camera.close()
            
            val errorMessage = when (error) {
                ERROR_CAMERA_IN_USE -> "カメラは使用中です"
                ERROR_MAX_CAMERAS_IN_USE -> "開いているカメラが多すぎます"
                ERROR_CAMERA_DISABLED -> "カメラは無効になっています"
                ERROR_CAMERA_DEVICE -> "カメラハードウェアエラー"
                ERROR_CAMERA_SERVICE -> "カメラサービスエラー"
                else -> "不明なエラー"
            }
            
            Toast.makeText(this@CameraOpenActivity, "カメラエラー: $errorMessage", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "カメラパーミッションが必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
    }
}
```

## カメラのライフサイクル

カメラのライフサイクルを理解することは重要です：

1. **オープン** — `openCamera()`を呼び出してCameraDeviceを取得する
2. **使用** — キャプチャセッションを作成し、写真を撮影する
3. **クローズ** — 終了したら`close()`を呼び出す
4. **解放** — カメラが他のアプリから使用可能になる

リソースリークを避けるため、アクティビティが破棄されるときは必ずカメラを閉じてください。

## ベストプラクティス

1. **終了したら閉じる** — `onDestroy()`で必ずカメラを閉じる
2. **エラーを処理する** — `onError()`コールバックを無視しない
3. **パーミッションを確認する** — 開く前に必ずパーミッションを確認する
4. **try-catchを使用する** — `SecurityException`と`IllegalArgumentException`を処理する
5. **参照を保持しない** — 閉じたらCameraDeviceの参照を解放する

## よくある問題

### カメラが使用中です
- 他のアプリがカメラを使用していないことを確認してください
- カメラを適切に閉じていることを確認してください

### パーミッションが拒否される
- マニフェストのパーミッションを確認してください
- 実行時パーミッションが付与されていることを確認してください

### カメラIDが見つからない
- 必ず`getCameraIdList()`からカメラIDを取得してください
- カメラIDをハードコードしないでください

## 次の章

カメラを開けるようになったので、次のステップはプレビューを表示することです。次の章では：

1. TextureViewについて学ぶ
2. プレビュー用のSurfaceを作成する
3. CameraCaptureSessionを作成する
4. カメラプレビューを画面に表示する

## まとめ

カメラを開くことは、画像を撮影するための最初のステップです：

1. `CameraManager.openCamera()`を使用してCameraDeviceを取得する
2. `onOpened()`、`onDisconnected()`、`onError()`のStateCallbackを処理する
3. 終了したら必ずカメラを閉じる
4. カメラのライフサイクルに従う：オープン → 使用 → クローズ → 解放

次の章では、カメラプレビューを作成して、カメラが見ているものを表示できるようにします。
