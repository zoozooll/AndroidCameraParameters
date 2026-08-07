---
sidebar_position: 5
title: "第5章：初めてのCamera2プロジェクトを作成する"
description: Camera2プロジェクトを一からセットアップします。カメラの権限、CameraManagerの初期化、HandlerThreadによる背景スレッド管理、およびTextureViewのハードウェアアクセラレーション設定について学びます。
keywords: [Camera2 プロジェクト セットアップ, Android カメラ権限, HandlerThread, CameraManager, TextureView ハードウェアアクセラレーション]
---

いよいよ実践編です。前の章で学んだ理論をコードに落とし込みます。この章が終わる頃には、Camera2 API を初期化し、カメラデバイスを列挙するための「CameraManager」にアクセスできるプロジェクトが完成しています。

[Android Camera Parameters アプリ](https://github.com/zoozooll/AndroidCameraParameters)は、このチュートリアルで構築する機能をさらに高度にした実例です。興味があれば、そのソースコードも参考にしてください。

## なぜプロジェクトのセットアップから始めるのか？

Camera2 は低レベルでパフォーマンスに敏感な API です。セットアップを疎かにすると、原因不明のクラッシュや、プレビューが動かないといった問題に直面します。正しいセットアップには以下の 3 つの柱があります：

1. **権限 (Permissions)**: アプリがカメラを使うための「お許し」をマニフェストと実行時に得ること。
2. **スレッド管理 (Threading)**: カメラの重い処理で画面を止めないための「背景スレッド」を用意すること。
3. **ハードウェアアクセラレーション**: 映像を滑らかに表示するための設定。

---

## ステップ 1：プロジェクトの作成と権限の宣言

Android Studio で「Empty Activity」プロジェクトを作成してください。言語は **Kotlin**、最小 SDK は **API 21 (Lollipop)** 以上を選択します。

### マニフェストの設定 (AndroidManifest.xml)
カメラを使うことをシステムに宣言します。

```xml
<!-- カメラの使用権限 -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- カメラがないデバイスへのインストールを制限（任意） -->
<uses-feature android:name="android.hardware.camera" android:required="true" />

<application ...>
    <activity android:name=".MainActivity"
        android:hardwareAccelerated="true"> <!-- プレビュー表示に必須 -->
        ...
    </activity>
</application>
```

---

## ステップ 2：実行時の権限リクエスト

Android 6.0 以降、マニフェストに書くだけでは不十分です。アプリ起動時にユーザーに許可を求める必要があります。

```kotlin
if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
} else {
    initializeCamera()
}
```

---

## ステップ 3：背景スレッドの用意 (HandlerThread)

Camera2 API は、多くの処理を非同期で行います。これらの処理（コールバック）をメインスレッド（UI スレッド）で受けてしまうと、画面がカクついたり、最悪の場合はアプリがフリーズしたりします。そのため、カメラ専用のスレッドを用意するのが鉄則です。

```kotlin
private lateinit var backgroundThread: HandlerThread
private lateinit var backgroundHandler: Handler

private fun startBackgroundThread() {
    backgroundThread = HandlerThread("CameraBackground").apply { start() }
    backgroundHandler = Handler(backgroundThread.looper)
}

private fun stopBackgroundThread() {
    backgroundThread.quitSafely()
    backgroundThread.join()
}
```

---

## ステップ 4：CameraManager の初期化

最後に、カメラ全体を統括する `CameraManager` にアクセスします。

```kotlin
private fun initializeCamera() {
    val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraIdList = manager.cameraIdList // デバイス上のカメラ ID の一覧を取得
    Log.d("Camera2", "見つかったカメラの数: ${cameraIdList.size}")
}
```

---

## まとめ

おめでとうございます！これで Camera2 開発の土台が整いました。
- **権限**: マニフェストと実行時の両方で設定。
- **背景スレッド**: HandlerThread で UI への影響を回避。
- **CameraManager**: カメラを操作するための入り口を確保。

## 次のステップ

土台ができたので、次はいよいよ「どんなカメラが載っているのか」を詳しく調べます。**第 6 章：カメラの検出**では、背面・前面カメラの判別や、それぞれの性能（ハードウェアレベル）を詳しくログに出力する方法を学びます。
