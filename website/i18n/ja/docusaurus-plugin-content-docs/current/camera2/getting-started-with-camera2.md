---
sidebar_position: 5
title: "第5章：Camera2を始めよう"
description: Android Camera2 APIのエントリーポイントであるCameraManagerについて学び、カメラの列挙と特性へのアクセス方法を理解します。
keywords: [CameraManager, Camera2 API, Androidカメラ, カメラ列挙]
---

シリーズのコーディングパートへようこそ。まずは基礎であるCameraManagerから始めましょう。

## はじめに

カメラを使用する前に、カメラを検出してアクセスする方法が必要です。そこで登場するのが**CameraManager**です。

CameraManagerはCamera2 APIへのゲートウェイです。Camera2アプリケーションで最初に使用するクラスになります。

## CameraManagerとは何ですか？

CameraManagerは、Androidデバイス上のすべてのカメラデバイスを管理するシステムサービスです。カメラのディレクトリまたはレジストリと考えてください。

主な責務は以下の通りです：
1. **カメラの列挙** — 利用可能なすべてのカメラを一覧表示する
2. **カメラ特性の取得** — 各カメラの詳細情報を取得する
3. **カメラを開く** — キャプチャ用のCameraDeviceを作成する

## CameraManagerの取得

Androidでは、システムサービスは`Context`を通じて取得します。CameraManagerを取得する方法は以下の通りです：

```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
```

以上です。デバイス上のすべてのカメラにアクセスするためのコードは1行です。

## まずはパーミッションから

CameraManagerを使用する前に、カメラパーミッションをリクエストする必要があります。`AndroidManifest.xml`に以下を追加してください：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />
```

そして、アクティビティでランタイムパーミッションをリクエストします：

```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED
) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        CAMERA_PERMISSION_REQUEST
    )
}
```

カメラにアクセスする前に、常にパーミッションを確認してください。

## CameraManagerのメソッド

CameraManagerには、主に3つのメソッドがあります：

### 1. `getCameraIdList()`

カメラID文字列の配列を返します。各IDはカメラデバイスを表します。

```kotlin
val cameraIds = cameraManager.cameraIdList
cameraIds.forEach { id ->
    Log.d("Camera", "カメラが見つかりました: $id")
}
```

出力例：
```
Camera 0
Camera 1
Camera 2
```

### 2. `getCameraCharacteristics(cameraId: String)`

特定のカメラに関するすべての詳細を含む`CameraCharacteristics`オブジェクトを返します。

```kotlin
val characteristics = cameraManager.getCameraCharacteristics("0")
```

CameraCharacteristicsには、カメラの機能を説明する何百ものパラメータが含まれています。

### 3. `openCamera(cameraId: String, stateCallback: CameraDevice.StateCallback, handler: Handler?)`

カメラを開き、コールバックを通じて`CameraDevice`を返します。これについては後で詳しく説明します。

## カメラIDの再確認

第4章で学んだように、Androidはカメラに数値IDを割り当てます。IDはデバイス間や再起動間で一貫しているとは限りません。

一般的なパターン：
- **Camera 0** — 通常は背面広角カメラ
- **Camera 1** — 多くの場合前面カメラ
- **Camera 2** — 通常は超広角または望遠カメラ
- それ以上の番号 — 追加のカメラ（マクロ、深度など）

しかし、カメラIDの意味を**決して推測しないでください**。常にカメラ特性を確認して、以下を判断してください：
- レンズの向き（前面/背面/外部）
- 焦点距離
- 機能

## CameraManagerが重要な理由

CameraManagerは、Camera2で行うすべての基礎となります：

1. **検出** — カメラを使用する前に、カメラを見つける必要があります
2. **情報** — カメラを開く前に、その機能を知る必要があります
3. **アクセス** — CameraManagerは、カメラデバイスを開く唯一の方法を提供します

## 簡単な例

すべてを組み合わせた簡単な例を見てみましょう：

```kotlin
class CameraDiscoveryActivity : AppCompatActivity() {
    
    private lateinit var cameraManager: CameraManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        if (hasCameraPermission()) {
            discoverCameras()
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
    
    private fun discoverCameras() {
        val cameraIds = cameraManager.cameraIdList
        
        Log.d("CameraDiscovery", "${cameraIds.size}台のカメラが見つかりました")
        
        cameraIds.forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            val lensFacingStr = when (lensFacing) {
                CameraCharacteristics.LENS_FACING_FRONT -> "前面"
                CameraCharacteristics.LENS_FACING_BACK -> "背面"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "外部"
                else -> "不明"
            }
            
            Log.d("CameraDiscovery", "カメラ $cameraId: $lensFacingStr")
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
                discoverCameras()
            } else {
                Toast.makeText(this, "カメラパーミッションが必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}
```

このシンプルなアクティビティは、すべてのカメラを検出し、それらのIDとレンズの向きをログに記録します。

## 重要なポイント

- **CameraManager**はCamera2へのエントリーポイントです
- `getCameraIdList()`を使用してすべてのカメラを見つけます
- `getCameraCharacteristics()`を使用して詳細情報を取得します
- 常に最初にカメラパーミッションをリクエストしてください
- カメラIDの意味を推測しないでください — 特性を確認してください

## 次の章

CameraManagerについて理解したので、最初の本格的なCamera2プログラムを作成しましょう。次の章では、以下を行います：

1. シンプルなAndroidアプリを作成する
2. 利用可能なすべてのカメラを一覧表示する
3. カメラ情報をユーザーに表示する

最初のCamera2コードを書いて、実際の結果を見てみましょう！

## まとめ

CameraManagerはCamera2の基礎です。以下へのアクセスを提供します：
- カメラの列挙
- カメラ特性
- カメラを開く機能

CameraManagerを使用すると、利用可能なカメラを発見し、それらを開く前に機能について学ぶことができます。

次の章では、デバイス上のすべてのカメラを一覧表示する最初のCamera2プログラムを作成します。
