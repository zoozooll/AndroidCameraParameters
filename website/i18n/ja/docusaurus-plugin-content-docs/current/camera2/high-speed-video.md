---
sidebar_position: 19
title: "第19章：ハイスピードビデオ"
description: "CameraConstrainedHighSpeedCaptureSession、createHighSpeedRequestList、およびStreamConfigurationMapのFPS範囲を使用して、120fpsおよび240fpsのスローモーション撮影を実装します。"
keywords: [Android Camera2, ハイスピードビデオ, スローモーション, 120fps, 240fps, CameraConstrainedHighSpeedCaptureSession, createHighSpeedRequestList, FPS 範囲]
---

# 第19章：ハイスピードビデオ

スローモーションビデオは、人間の目では捉えられない瞬間を記録します。蛇口から落ちる水滴を 120 fps（4倍スロー）で捉えたり、ハチドリの羽ばたきを 240 fps（8倍スロー）で観察したりできます。Camera2 API で高フレームレート撮影を実装するのは、単に FPS の設定値を上げるだけではありません。専用の **`CameraConstrainedHighSpeedCaptureSession`** を使用し、**`createHighSpeedRequestList`** で事前検証されたバーストを送信する必要があります。

[Android Camera Parameters アプリ](https://github.com/zoozooll/AndroidCameraParameters)では、お使いのデバイスがどの解像度でどの FPS 範囲（120, 240 など）をサポートしているかを確認できます。

---

## なぜ標準セッションでは 240 FPS で動作しないのか

30 fps の撮影と 240 fps の撮影では、システムへの負荷が根本的に異なります。

- **スループット**: 1080p/240fps では、毎秒約 720MB もの画素データがメモリを流れます。
- **レイテンシ予算**: 240 fps では、1 フレームの処理に **4.167ms** しか許されません。
- **CPU オーバーヘッド**: フレームごとに個別のリクエストを Java/Kotlin から送信すると、その通信（Binder IPC）だけで CPU を使い果たしてしまいます。

**ハイスピードキャプチャセッション**は、複数のリクエストを 1 つのバーストリストとしてまとめ、HAL（ハードウェア層）のスケジューラに直接渡すことで、これらのオーバーヘッドを劇的に削減します。

---

## サポートされているサイズの確認

ハイスピード撮影では、通常の `getOutputSizes()` ではなく、専用のメソッドを使用する必要があります。

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
// ハイスピード対応のサイズを取得
val highSpeedSizes = configMap?.highSpeedVideoSizes
// そのサイズで利用可能な FPS 範囲を取得
val fpsRanges = configMap?.getHighSpeedVideoFpsRangesFor(size)
```

多くのフラッグシップ機では 1080p で 240 fps をサポートしていますが、ミドルレンジ機では 720p に制限されることがあります。

---

## 実装のポイント

1. **セッションの作成**: `createCaptureSession` ではなく、**`createConstrainedHighSpeedCaptureSession`** を呼び出します。
2. **出力 Surface**: プレビュー用と録画用の最大 2 つまでに制限されます。
3. **リクエストリスト**: `createHighSpeedRequestList(builder.build())` を使用して、HAL が高速に処理できる形式に変換します。
4. **スローモーションの設定**: `MediaRecorder.setCaptureRate(fps)` を呼び出すことで、撮影は高速で行い、再生は 30 fps で行う「スローモーション動画」として保存されます。

```kotlin
// ハイスピードリクエストの送信
val highSpeedRequestList = highSpeedSession.createHighSpeedRequestList(recordBuilder.build())
highSpeedSession.setRepeatingBurst(highSpeedRequestList, null, backgroundHandler)
```

**最適化のヒント：**
240 fps では毎秒大量のメタデータが発生するため、パフォーマンスを優先して `CaptureCallback` を `null` に設定し、フレームごとのコールバックをスキップすることが推奨されます。

---

## まとめ

- **専用 API**: 高フレームレートには `CameraConstrainedHighSpeedCaptureSession` が必須です。
- **制約**: Surface は 2 つまで、テンプレートは `RECORD` または `PREVIEW` のみ、といった厳格なルールがあります。
- **スローモーション**: `setCaptureRate` でキャプチャ速度と再生速度の差を作ります。
- **パフォーマンス**: バインダー通信を最小限に抑えるため、バーストリスト形式でリクエストを処理します。

## 次のステップ

高速撮影をマスターしました。次は、複数のレンズを操る技術です。**第20章：マルチカメラ**では、超広角・広角・望遠レンズをシームレスに切り替えたり、同時にキャプチャしたりする方法を学びます。
