---
sidebar_position: 12
title: "第12章：CameraCharacteristics ディープダイブ"
description: カメラを開く前に、そのカメラを記述する不変の静的メタデータである CameraCharacteristics をマスターします。ハードウェアレベル (LEGACY, LIMITED, FULL, LEVEL_3, EXTERNAL)、機能フラグ、メタデータキーの構成、および実行時の機能照会について学びます。
keywords: [CameraCharacteristics, ハードウェアレベル, INFO_SUPPORTED_HARDWARE_LEVEL, LEGACY, LIMITED, FULL, LEVEL_3, EXTERNAL, REQUEST_AVAILABLE_CAPABILITIES, MANUAL_SENSOR, RAW, メタデータキー]
---

## 12.1 ポケットの中のスペックシート

カメラを開く前に、Camera2 で最初に行うべきこと、それが `CameraCharacteristics` の照会です。これは、センサーに電源を入れることなく、カメラが「何ができるか」をすべて確認できる不変のスペックシートです。

Android には 1 万モデル以上のデバイスが存在します。マニュアル露出ができるか、RAW で撮れるか、1080p のプレビューができるか。これらを勝手に想定してはいけません。必ず `CameraCharacteristics` に聞く必要があります。

[Android Camera Parameters アプリ](https://github.com/zoozooll/AndroidCameraParameters)は、いわばこの `CameraCharacteristics` のブラウザです。アプリを開くと、この章で説明するすべてのキーがカテゴリ別に整理されているのを確認できます。

## 12.2 CameraCharacteristics の実体

- **不変 (Immutable)**: 一度取得したオブジェクトの内容は変わりません。
- **省電力**: センサーを起動せずに情報を取得できるため、アプリの起動時に呼んでもバッテリーを消費しません。
- **キーと型の安全性**: `<Key<T>> get(Key<T> key)` 形式でアクセスし、型が保証されています。

---

## 12.3 ハードウェアレベル：INFO_SUPPORTED_HARDWARE_LEVEL

最も重要なキーです。そのカメラが Camera2 の機能をどの程度実装しているかを示す「格付け」です。

| レベル | 意味 | 実デバイスの例 |
|:---|:---|:---|
| **LEGACY** | 旧 Camera API のラッパー。マニュアル操作はほぼ不可。 | 非常に古い機種、エミュレータ |
| **LIMITED** | 基本的な機能は動くが、マニュアル制御や RAW は不可。 | 低価格帯、ミドルレンジの前面カメラ |
| **FULL** | **プロ向け。** マニュアル露出、RAW 出力、バースト撮影を完全サポート。 | フラッグシップ機の背面メインカメラ |
| **LEVEL_3** | **最高峰。** RAW 再処理や高度な同期機能を備える。 | 最新フラッグシップ (Pixel 7+, Galaxy S23+ 等) |
| **EXTERNAL** | USB カメラなど。機能は接続されるデバイスに依存。 | 外付けウェブカメラ |

:::important
ハードウェアレベルは「努力目標」ではなく「保証」です。`FULL` と報告されていれば、Google のテストをパスした完全な機能が使えることを意味します。
:::

---

## 12.4 機能：REQUEST_AVAILABLE_CAPABILITIES

ハードウェアレベルよりも細かい、個別の機能フラグです。

- **`MANUAL_SENSOR`**: 露出時間や ISO、フォーカスを手動で操れる。
- **`RAW`**: センサーの生データ (DNG) を出力できる。
- **`LOGICAL_MULTI_CAMERA`**: 複数の物理レンズを組み合わせて一つのカメラとして振る舞える。
- **`DEPTH_OUTPUT`**: 深度（奥行き）情報を出力できる。

---

## 12.5 メタデータの構成：名前空間

キーは `android.sensor.*` や `android.lens.*` のように、制御する対象ごとにグループ化されています。

- **`sensor`**: 露出、ISO、感度、タイムスタンプなど。
- **`lens`**: 焦点距離、絞り、手ぶれ補正など。
- **`control`**: オートフォーカス (AF)、露出 (AE)、ホワイトバランス (AWB) などのアルゴリズム。
- **`scaler`**: デジタルズーム、出力サイズ、回転など。

---

## まとめ

- **ハードウェアレベル**: カメラの全体的な能力階層（FULL が開発のターゲット）。
- **機能フラグ**: RAW やマニュアル操作など、個別の機能の有無。
- **不変のスペックシート**: センサーを起動せずにすべての情報を引き出せる。

## 次のステップ

カメラが「何ができるか」を理解しました。次は、その能力を使って実際に「光」を操りましょう。**第13章：露出**では、ISO、シャッタースピード、絞りの関係と、Camera2 でこれらを手動設定する基礎を学びます。
