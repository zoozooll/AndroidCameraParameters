---
sidebar_position: 6
title: "第6章: カメラの一覧表示"
description: Androidデバイス上で利用可能なすべてのカメラを検出して一覧表示する、最初のCamera2プログラムを作成します。
keywords: [カメラ一覧, CameraManager, カメラ列挙, Android Camera2]
---

最初のCamera2プログラムを作成する準備が整いました！すべてのカメラを一覧表示するアプリを作成しましょう。

## はじめに

この章では、最初の本格的なCamera2アプリケーションを作成します。目標は単純です：

> デバイス上のすべてのカメラを検出し、それらの情報を表示する。

これは小さなステップですが、重要なステップです。カメラを使用する前に、カメラを見つける必要があります。

## プロジェクトの作成

新しいAndroidプロジェクトを作成することから始めましょう：

1. Android Studioを開く
2. "Empty Activity"で新しいプロジェクトを作成する
3. プロジェクト名を"Camera2List"にする
4. 言語としてKotlinを選択する
5. 最小SDKをAPI 21に設定する（Camera2はAPI 21で導入されました）

## パーミッションの追加

`AndroidManifest.xml`にカメラパーミッションを追加します：

```xml
<manifest ...>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    
    <application ...>
        ...
    </application>
</manifest>
```

## レイアウト

カメラの一覧を表示する単純なレイアウトを作成します。`activity_main.xml`を更新してください：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="利用可能なカメラ"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ListView
        android:id="@+id/cameraListView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

## アクティビティ

次に、メインアクティビティを作成しましょう。ここにCamera2のコードを記述します：

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraListView: ListView
    private val cameraInfoList = mutableListOf<String>()

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraListView = findViewById(R.id.cameraListView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (hasCameraPermission()) {
            listCameras()
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

    private fun listCameras() {
        cameraInfoList.clear()
        
        try {
            val cameraIds = cameraManager.cameraIdList
            
            if (cameraIds.isEmpty()) {
                cameraInfoList.add("カメラが見つかりません")
            } else {
                cameraInfoList.add("${cameraIds.size}台のカメラが見つかりました：")
                cameraInfoList.add("")
                
                cameraIds.forEachIndexed { index, cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val lensFacingStr = when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "フロント"
                        CameraCharacteristics.LENS_FACING_BACK -> "バック"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "外部"
                        else -> "不明"
                    }
                    
                    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hardwareLevelStr = when (hardwareLevel) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        else -> "不明"
                    }
                    
                    cameraInfoList.add("カメラ $index (ID: $cameraId)")
                    cameraInfoList.add("  - レンズ: $lensFacingStr")
                    cameraInfoList.add("  - ハードウェアレベル: $hardwareLevelStr")
                    cameraInfoList.add("")
                }
            }
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
            cameraListView.adapter = adapter
            
        } catch (e: SecurityException) {
            cameraInfoList.add("エラー: カメラパーミッションが拒否されました")
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
                listCameras()
            } else {
                Toast.makeText(this, "カメラパーミッションが必要です", Toast.LENGTH_SHORT).show()
                cameraInfoList.add("カメラパーミッションが拒否されました")
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cameraInfoList)
                cameraListView.adapter = adapter
            }
        }
    }
}
```

## このコードの処理内容

何が行われているかを分解して見てみましょう：

1. **CameraManagerの取得** — CameraManagerシステムサービスを取得します
2. **パーミッションの確認** — カメラパーミッションが付与されているかを確認します
3. **カメラの一覧表示** — `getCameraIdList()`を使用してすべてのカメラIDを取得します
4. **特性の取得** — 各カメラの特性を取得します
5. **情報の表示** — カメラID、レンズの向き、ハードウェアレベルを表示します

## 期待される出力

アプリを実行すると、次のような表示がされるはずです：

```
3台のカメラが見つかりました：

カメラ 0 (ID: 0)
  - レンズ: バック
  - ハードウェアレベル: FULL

カメラ 1 (ID: 1)
  - レンズ: フロント
  - ハードウェアレベル: LIMITED

カメラ 2 (ID: 2)
  - レンズ: バック
  - ハードウェアレベル: FULL
```

## 成功です！

最初のCamera2プログラムを作成しました！単純に見えるかもしれませんが、これはこれから行うすべてのことの基礎となります。

## トラブルシューティング

問題が発生した場合：

1. **パーミッションが拒否される** — カメラパーミッションを付与したことを確認してください
2. **カメラが見つからない** — デバイスにカメラがあるか確認してください
3. **SecurityException** — マニフェストにパーミッションが宣言されていることを確認してください
4. **APIレベルが低すぎる** — Camera2にはAPI 21以上が必要です

## 次のステップ

カメラを一覧表示できるようになったので、次のステップはそれらの特性をより詳しく調べることです。次の章では：

1. CameraCharacteristicsを探索する
2. レンズの向きについて学ぶ
3. ハードウェアレベルを理解する
4. センサー情報を確認する

## まとめ

この章では、最初のCamera2プログラムを作成しました。このアプリは：

1. カメラパーミッションを要求する
2. CameraManagerを使用してカメラを列挙する
3. カメラID、レンズの向き、ハードウェアレベルを表示する

これは完全なCamera2アプリケーションを構築するための最初のステップです。次の章では、CameraCharacteristicsをさらに深く掘り下げて、各カメラが何ができるかを理解していきます。
