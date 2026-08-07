---
sidebar_position: 22
title: "第22章：カメラエクステンション"
description: CameraExtensionSessionを使用して、OEMが提供する高速なコンピューテーショナルフォトグラフィ機能（夜景モード、ボケポートレートモード、HDR、美顔モード、オートモード）を利用します。CameraExtensionCharacteristicsの照会、レイテンシ管理、標準セッションとエクステンションセッションのアーキテクチャの違いについて学びます。
keywords: [Android Camera2, カメラエクステンション, CameraExtensionSession, CameraExtensionCharacteristics, 夜景モード, ボケ, ポートレートモード, EXTENSION_NIGHT, EXTENSION_BOKEH, EXTENSION_HDR, EXTENSION_FACE_RETOUCH, EXTENSION_AUTOMATIC, getEstimatedCaptureLatencyRangeMillis]
---

# 第22章：カメラエクステンション

夜景モード、ポートレートのボケ、マルチフレーム HDR といった機能をゼロから実装するには、膨大な開発リソースと高度なアルゴリズムが必要です。**Camera Extensions API** (Android 12+) は、メーカー純正カメラアプリが使用している*ハードウェアアクセラレーション済みの計算パイプライン*を、標準的な 5 つのエクステンションタイプとして公開することで、この問題を解決します。

[Android Camera Parameters アプリ](https://github.com/zoozooll/AndroidCameraParameters)では、お使いのデバイスがどのエクステンションをサポートしているか、また各モードでのサポート解像度を確認できます。

---

## 5 つの標準エクステンション

各エクステンションは、メーカー独自のアルゴリズムを使用しながらも、Camera2 では共通の定数で操作できます。

| エクステンション定数 | 内容 | 期待される効果 |
|:---|:---|:---|
| **`EXTENSION_NIGHT`** | 夜景モード | 複数枚を合成して、暗所でのノイズを劇的に抑える。 |
| **`EXTENSION_BOKEH`** | ポートレート | 深度を推定し、背景を自然にぼかす。 |
| **`EXTENSION_HDR`** | HDR | 露出の異なる複数枚を合成し、白飛びや黒潰れを防ぐ。 |
| **`EXTENSION_FACE_RETOUCH`** | 美顔モード | 肌を滑らかにし、顔立ちを整える。 |
| **`EXTENSION_AUTOMATIC`** | オート | シーンに応じて HAL が最適なエクステンションを自動選択。 |

---

## アーキテクチャの違い

標準の `CameraCaptureSession` と異なり、`CameraExtensionSession` は **EIPP (Extension Intermediate Processing Pipeline)** という、メーカーが管理する中間処理パスを経由します。

- **標準セッション**: センサー → ISP → アプリ (レイテンシ 33ms 程度)
- **エクステンションセッション**: センサー → **OEM 独自の重い処理 (NPU/DSP)** → アプリ (レイテンシ 500ms〜8000ms)

このため、エクステンション使用時は、ユーザーが「アプリが固まった」と誤解しないよう、**`getEstimatedCaptureLatencyRangeMillis()`** を使用して適切なプログレス表示（「処理中…」など）を行うことが不可欠です。

---

## 実装のステップ

1. **サポートの照会**: `CameraExtensionCharacteristics` を使用して、利用可能なモードとサイズを確認します。
2. **セッションの構成**: `ExtensionSessionConfiguration` を作成し、`createExtensionSession` を呼び出します。
3. **プレビューの開始**: エクステンションセッション専用の `setRepeatingRequest` を使用します。
4. **キャプチャ**: 通常と同様に `capture` を呼び出しますが、レイテンシ（処理待ち）が発生することを考慮した UI 設計を行います。

```kotlin
// エクステンションセッションの作成例
val extensionConfig = ExtensionSessionConfiguration(
    CameraExtensionCharacteristics.EXTENSION_BOKEH,
    outputSurfaces,
    executor,
    stateCallback
)
cameraDevice.createExtensionSession(extensionConfig)
```

**注意点：**
- エクステンションによっては、最大解像度をサポートしていない場合があります。
- 夜景モードなどでは、撮影中にデバイスを動かさないようユーザーに促すメッセージを表示するのが一般的です。

---

## まとめ

- **OEM のパワーを活用**: 自前でアルゴリズムを書かずに、メーカー純正の高品質な写真機能を利用できます。
- **CameraExtensionSession**: 処理を専用のパイプラインに委任するためのセッション。
- **レイテンシ管理**: 高度な処理には時間がかかるため、UI での適切なフィードバックが重要です。

## 次のステップ

高度な後処理を学びました。次は、シャッターを押した「瞬間の前」を捉える技術です。**第23章：ゼロシャッターラグと再処理**では、ユーザーがボタンを押した瞬間に、既に過去に撮り終えていた最高画質のフレームを取り出す魔法のような仕組みを解説します。
