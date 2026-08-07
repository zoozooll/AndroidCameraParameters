---
sidebar_position: 16
title: "第16章：ホワイトバランスと色"
description: Android Camera2におけるオートホワイトバランスプリセットと手動の色補正について学びます。AWBモード、色温度 (2000K–10000K)、3×3色変換行列、COLOR_CORRECTION_GAINS、および暖色系の夕暮れプリセットや完全な手動ホワイトバランスのためのKotlinコードについて解説します。
keywords: [android camera2 ホワイトバランス, CONTROL_AWB_MODE, COLOR_CORRECTION_GAINS, COLOR_CORRECTION_TRANSFORM, 色温度, 色補正行列, Rec.709対DCI-P3 camera2]
---

# 第16章：ホワイトバランスと色

明るさ（露出）と鮮明さ（フォーカス）をマスターしました。次は画像の**見た目**、つまり*色調*を制御する番です。

温かみのある白熱灯の下で白い紙を撮影すると、ランプの黄色やオレンジ色の光が紙に当たり、センサーはそれをオレンジ色として捉えます。人間の脳はこれを瞬時に補正して「白い紙」として認識し続けますが、センサーの生データにはオレンジ色として記録されます。

**ホワイトバランス (White Balance, WB)** は、光源の色を補正して、無彩色の白が白く見えるようにするカメラの処理プロセスです。これを誤ると、写真全体に不自然な色かぶり（オレンジすぎ、青すぎ、緑すぎなど）が発生します。

[Android Camera Parameters アプリ](https://github.com/zoozooll/AndroidCameraParameters)では、すべての AWB プリセットをライブグリッドビューで確認したり、マニュアルゲインのスライダーを操作したりできます。この章で実装する内容をアプリで実際に体験してみてください。

---

## 色温度：暖色から寒色へのスペクトル

光源は、ケルビン (K) 単位の**色温度**で表されます。このスケールは、同じ色で発光する理論上の「黒体放射体」の温度を表しています。

```mermaid
graph LR
    A["1800K<br/>キャンドル"] --> B["2800K<br/>白熱灯"]
    B --> C[3500K<br/>温白色蛍光灯]
    C --> D[4500K<br/>白色蛍光灯]
    D --> E[5500K<br/>昼光 / フラッシュ]
    E --> F[6500K<br/>曇天]
    F --> G[8000K<br/>日陰]
    G --> H[10000K+<br/>青空 / 深い日陰]
    style A fill:#e67e22,color:#fff
    style B fill:#f39c12,color:#fff
    style C fill:#f1c40f,color:#333
    style D fill:#f9e79f,color:#333
    style E fill:#ffffff,color:#333,stroke:#333
    style F fill:#d4e6f1,color:#333
    style G fill:#85c1e9,color:#333
    style H fill:#3498db,color:#fff
```

**直感とは逆のルール：** 暖色系の光 = *低い*数値（1800Kのキャンドルは非常にオレンジ）。寒色系の光 = *高い*数値（10000Kの空は非常に青）。

オートホワイトバランスの役割は、シーンの統計情報から光源を推測し、その色かぶりを*差し引いて*、白い物体が白く見えるようにすることです。

---

## Camera2 におけるオートホワイトバランス (AWB) モード

`CaptureRequest.CONTROL_AWB_MODE` を介して設定します。

| モード (CONTROL_AWB_MODE_*) | 効果 | ユースケース |
|:---|:---|:---|
| `OFF` | 手動ホワイトバランスのみ。`COLOR_CORRECTION_GAINS` などを明示的に使用。 | プロモード、RAW 現像 |
| `AUTO` | デフォルト。光源検出を継続的に実行。 | 一般的な撮影 |
| `INCANDESCENT` | 約 2800K。温かみのある白熱灯のオレンジを打ち消すための強い青ゲイン。 | 屋内の電球下 |
| `DAYLIGHT` | 約 5500K。標準的な正午の太陽光プロファイル。 | 晴天の屋外 |
| `SHADE` | 約 7500K。日陰の強い青みを抑えるための赤ゲイン。 | 屋外の日陰 |

**まずサポートされているモードを確認する：** すべてのデバイスが 9 つのプリセットすべてをサポートしているわけではありません。

```kotlin
val availableAwbModes = characteristics.get(
    CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
) ?: intArrayOf()
Log.d("AWB", "使用可能なモード: ${availableAwbModes.toList()}")
```

---

## ホワイトバランス補正の仕組み：その裏側

AWB はセンサーの RGB から表示用の sRGB へ変換するために、2 つの色変換を適用します。

### ステップ 1：チャンネルゲイン (白点補正)

まず、各カラーチャンネルにゲイン（増幅率）を掛け、グレーのターゲットで R, G, B が等しくなるようにします。

Camera2 では、これは **`CaptureRequest.COLOR_CORRECTION_GAINS`** として公開されています。これは **[R, Geven, B, Godd]** の順序で並んだ 4 つの浮動小数点配列です。

```kotlin
// [ R ゲイン, G-even ゲイン, B ゲイン, G-odd ゲイン ]
val warmGains = floatArrayOf(1.0f, 1.2f, 0.8f, 1.2f)  // 暖色系：赤を強調し、青を抑える
```

### ステップ 2：3×3 色変換行列 (ガマットマッピング)

チャンネルゲインは*白点*を補正するだけです。しかし、センサーによって色の特性が異なるため、標準的な色空間（sRGB や DCI-P3 など）にマッピングするために **3×3 色補正行列 (CCM)** が使用されます。

Camera2 では **`COLOR_CORRECTION_TRANSFORM`** を介して、`Rational[9]` 配列（行優先）で設定します。

---

## 実装例：完全な手動 AWB — 夕暮れ用の暖色ゲイン

究極のクリエイティブな制御のために、AWB を完全に無効にして独自のゲインを書き込みます。

```kotlin
fun applyManualGains(gains: FloatArray) {
    val request = captureSession.device.createCaptureRequest(
        CameraDevice.TEMPLATE_PREVIEW
    ).apply {
        addTarget(previewSurface)

        // 1) AWB を完全に無効にする
        set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)

        // 2) 4 つのチャンネルゲインを適用する
        set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)

        // 3) モードを設定（ゲインのみ手動、行列は HAL に任せる場合）
        set(CaptureRequest.COLOR_CORRECTION_MODE,
            CameraMetadata.COLOR_CORRECTION_MODE_FAST)
    }

    captureSession.setRepeatingRequest(request.build(), null, null)
}
```

---

## まとめ

- **色温度 (K)**: 低いとオレンジ、高いと青。AWB はこれを打ち消してニュートラルにします。
- **AWB モード**: `AUTO` や `OFF` のほか、光源に合わせた各種プリセットがあります。
- **マニュアル制御**: `COLOR_CORRECTION_GAINS` で 4 つのチャンネルの増幅率を直接指定します。
- **CCM 行列**: センサーの固有の色空間を標準の出力空間（sRGB など）へマッピングします。

## 次のステップ

**露出、フォーカス、ホワイトバランス**の個別の制御方法を学びました。**第17章：3Aパイプライン**では、これら 3 つを組み合わせて、プロのカメラアプリがシャッターを押すたびに実行している一連のキャプチャシーケンス（3A Orchestration）を構築します。
