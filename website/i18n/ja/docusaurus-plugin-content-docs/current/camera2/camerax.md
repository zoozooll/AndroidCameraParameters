---
sidebar_position: 24
title: "第24章：CameraX"
description: "Jetpackのライフサイクル対応カメラライブラリであるCameraXをマスターします。UseCaseアーキテクチャ、手動パラメータを注入するためのCamera2Interop、およびCameraXとCamera2の選択基準について学びます。"
keywords: [camerax, jetpack camera, camerax アーキテクチャ, usecase モデル, camera2interop, processcameraprovider, プレビュー usecase, imagecapture, imageanalysis, videocapture, camerax 対 camera2]
---

# 第24章：CameraX

## まとめ

この章に到達するまでに、あなたは生の Camera2 API を使いこなし、デバイスのオープンからセッション構成、複雑なコールバックの処理までをマスターしてきました。今、一歩引いて考えてみましょう。もし、その膨大なボイラープレートコード（定型文）の 80% を消し去ることができたら？

**CameraX** は、Camera2 をライフサイクル対応の宣言的な API でラップした Google の Jetpack ライブラリです。Camera2 を置き換えるものではなく、内部で Camera2 を使用しています。CameraX が解決するのは、数百行に及ぶセッション構成、デバイスごとの「癖 (Quirks)」への対処、そして手動のライフサイクル管理です。この章では、CameraX のアーキテクチャ、`UseCase` モデル、そして `Camera2Interop` を使用して CameraX に生の Camera2 パラメータを注入する方法を学びます。

[Android Camera Parameters アプリ](https://github.com/zoozooll/AndroidCameraParameters)を使用して、対象のデバイスの機能を事前に確認し、CameraX と Camera2 のどちらをターゲットにすべきか判断してください。

---

## CameraX アーキテクチャ

CameraX の中心にあるのは **UseCase（ユースケース）** モデルです。Surface や Session といった低レベルな概念ではなく、「カメラで何をしたいか」という目的ベースで考えます。

### UseCase モデル

主に 4 つのユースケースを組み合わせて使用します：

- **`Preview`**: 画面にカメラ映像を表示する。
- **`ImageAnalysis`**: 背景スレッドでフレームごとの画像解析（顔検出など）を行う。
- **`ImageCapture`**: 静止画を撮影し、保存する。
- **`VideoCapture`**: ビデオを録画する。

### ライフサイクルへの対応

CameraX の最大の利点の一つは、Android のライフサイクル（Activity/Fragment）に自動的に同期することです。

```kotlin
val cameraProvider = cameraProviderFuture.get()
val camera = cameraProvider.bindToLifecycle(
    lifecycleOwner,
    cameraSelector,
    preview,
    imageCapture
)
```

これだけで、Activity が停止すればカメラが閉じられ、再開されれば自動的に復帰します。リソースリークの心配が劇的に減ります。

---

## Camera2Interop：CameraX に Camera2 の力を

CameraX は便利ですが、Camera2 で可能だった「ISO や露出をミリ秒単位で手動制御する」といった詳細な操作が隠されています。これを解決するのが **`Camera2Interop`** です。

これを使うと、CameraX のユースケースを構築する際に、生の Camera2 キーを「注入」することができます。

```kotlin
val builder = ImageCapture.Builder()
val interop = Camera2Interop.Extender(builder)
// 生の Camera2 キーを設定
interop.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 400)
interop.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 16666666L)
```

---

## CameraX か Camera2 か？

どちらを使うべきかの判断基準は明確です：

| シナリオ | 推奨 |
|:---|:---:|
| 一般的なプレビュー、写真撮影、ビデオ録画 | **CameraX** |
| QRコード読み取り、ML Kit を使った解析 | **CameraX** |
| デバイスの互換性が最優先事項 | **CameraX** |
| RAW (DNG) 撮影、プロモードの実装 | **Camera2** |
| ゼロシャッターラグ (ZSL)、特殊なマルチカメラ制御 | **Camera2** |
| ネイティブ C++ (NDK) での高速処理 | **Camera2** |

---

## まとめ

CameraX は「ほとんどのケース」における正解です。Google が提供する膨大なデバイス互換性データベースを活用できるため、機種固有のバグに悩まされる時間を大幅に削減できます。一方で、カメラの極限の性能を引き出したい場合や、プロ仕様の機能を実装したい場合には、これまで学んできた Camera2 の知識が必要不可欠となります。

## 次のステップ

CameraX も依然として Java/Kotlin のマネージドコード層で動作します。もし、AR エンジンのように 1 フレーム 16ms という極限の予算で動作させる必要があるなら、境界を越えて C++ の世界へ向かう必要があります。**第25章：ネイティブカメラ開発**では、NDK を使用した最高速のカメラパイプラインを解説します。
