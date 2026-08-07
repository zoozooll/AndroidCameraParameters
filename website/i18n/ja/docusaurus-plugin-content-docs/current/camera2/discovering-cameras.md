---
sidebar_position: 6
title: "第6章：カメラの検出"
description: CameraCharacteristicsを使用してデバイス上のすべてのカメラを列挙し、照会します。カメラIDのセマンティクス、レンズの向き（前面/背面/外部）、外部USB OTGカメラ、およびLEGACYからLEVEL_3までのハードウェアレベルの階層について学びます。
keywords: [CameraCharacteristics, LENS_FACING, カメラ列挙, INFO_SUPPORTED_HARDWARE_LEVEL, 外部USBカメラ]
---

第5章では `CameraManager` の初期化に成功し、カメラIDのリストを取得しました。しかし、`"0"` や `"2"` といった文字列だけでは、そのカメラが実際に何であるかはわかりません。背面広角カメラなのか？ 自撮り用カメラなのか？ それともOTG経由で接続された外部USBウェブカメラなのか？ この章では、カメラデバイスのあらゆる機能を記述するメタデータコンテナである `CameraCharacteristics` を使用して、これらの疑問に答える方法を学びます。

カメラの列挙と特性の検査に関するプロダクション級のリファレンス実装については、**Android Camera Parameters** アプリ（[GitHub](https://github.com/zoozooll/AndroidCameraParameters)、[Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams)）を参考にしてください。このアプリは、デバイス上のすべてのカメラに対して `CameraCharacteristics` の全キーを調査し、検索・フィルタリング可能なUIで表示します。ハードウェア固有の Camera2 の問題をデバッグする際に非常に役立つツールです。

## カメラIDを理解する

特性の詳細に入る前に、Camera2 の初心者にとって混乱の元となる根本的な問題、つまり**数値のカメラID文字列は何を意味しているのか？** という点について説明します。

`cameraManager.cameraIdList` を呼び出すと、`Array<String>`（例：`["0", "1", "2", "3", "4"]`）が返されます。ここで、以下のような仮定をハードコードしたくなるかもしれません：
- `"0"` = 背面メインカメラ
- `"1"` = 前面カメラ
- `"2"` = 望遠カメラ

**これは絶対に行わないでください。** ID と物理カメラのマッピングは以下の理由で変動します：
1. **デバイス依存**: Pixel 8 では前面カメラに ID `"1"` を使用するかもしれませんが、Galaxy S24 では ID `"3"` を使用するかもしれません。
2. **バージョン依存**: OEM の OTA アップデートにより、出荷後に ID リストが変更される可能性があります。
3. **動的な構成**: 一部のマルチカメラ論理デバイスでは、モードに基づいて基盤となる物理カメラを動的に表示または非表示にします。

唯一の正しいアプローチは、**すべての ID の特性を照会し、関心のあるプロパティ（レンズの向き、ハードウェアレベル、焦点距離の範囲など）に基づいてカメラを選択すること**です。これは、適切に作成された Camera2 アプリが行う標準的なパターンであり、ここでもそれを実装します。

## カメラ検出のフロー

カメラを検出するアルゴリズムは表面上は単純ですが、エラー処理に関しては重要なエッジケースがあります。まずプロセスをフローチャートで確認し、次にコードで実装しましょう。

```mermaid
flowchart TD
    A["開始: CameraManagerの準備完了"] --> B["cameraIdList 配列を取得"]
    B --> C{リストは空か？}
    C -->|Yes| D[エラー: デバイスにカメラが見つからない]
    C -->|No| E[空のカメラ情報リストを初期化]
    E --> F[ループ: リスト内の各 cameraId に対して]
    F --> G[getCameraCharacteristics(cameraId)]
    G --> H{CameraAccessException が発生したか？}
    H -->|Yes| I[エラーをログに記録し、このカメラをスキップ]
    H -->|No| J[LENS_FACING 特性を照会]
    J --> K[INFO_SUPPORTED_HARDWARE_LEVEL を照会]
    K --> L[必要に応じて追加のキーを照会]
    L --> M[カメラ情報をリストに保存]
    M --> N{リストにまだカメラはあるか？}
    N -->|Yes| F
    N -->|No| O[検出された全カメラの要約をログ出力]
    O --> P[開くカメラを選択して続行]

    style A fill:#e3f2fd
    style C fill:#fff3e0
    style H fill:#fff3e0
    style N fill:#fff3e0
    style D fill:#ffcdd2
    style I fill:#ffecb3
    style G fill:#e8f5e9
    style J fill:#e8f5e9
    style K fill:#e8f5e9
    style L fill:#e8f5e9
    style O fill:#f3e5f5
    style P fill:#00c853,color:#fff
```

フローチャートの重要なポイント：
1. **空の ID リストを常に処理する**: スマートフォンでは稀ですが、Android TV、ヘッドレスデバイス、または仮想カメラのないエミュレータでは一般的です。
2. **`getCameraCharacteristics` は常に try/catch で囲む**: 列挙の途中でカメラが切断される可能性（特に外部 USB カメラ）や、デバイスのポリシーにより特定のカメラへのアクセスが制限されている場合があります。
3. **完全に反復してから選択する**: まずすべての候補を収集し、その中から基準に最適なものを選択します。「良さそうな」最初のカメラで止めてしまうと、より適切なカメラを見逃す可能性があります。

## CameraCharacteristics の紹介

`CameraCharacteristics` は、カメラのハードウェアレベルの機能を記述する、不変で読み取り専用のキー値マップです。レンズの焦点距離からセンサーのピクセルアレイサイズ、サポートされている出力形式まで、数百のキーが含まれています。

以下のようにして特性オブジェクトを取得します：
```kotlin
val characteristics: CameraCharacteristics =
    cameraManager.getCameraCharacteristics(cameraId)
```

そして、汎用的な `get` メソッドを使用して個々のキーを照会します：
```kotlin
val lensFacing: Int? = characteristics.get(CameraCharacteristics.LENS_FACING)
```

一部のキーはオプションであり、すべてのデバイスに存在するわけではないため、戻り値の型は Null 許容型（この場合は `Int?`）になります。

:::note
この章では意図的に `LENS_FACING` と `INFO_SUPPORTED_HARDWARE_LEVEL` のみを扱います。`CameraCharacteristics` のより深い内部（センサー特性、出力構成など）については、第III部 第10章「CameraCharacteristics 百科事典」で詳しく説明します。ここでは、開くカメラを選択するために必要な最小限の情報に焦点を絞ります。
:::

## キー 1: LENS_FACING — 前面、背面、または外部

ほとんどすべてのカメラアプリが最初に知る必要があるのは、レンズがどの方向を向いているかです。Camera2 では 3 つの定数が定義されています：

| 定数 | 値 | 意味 | 一般的なユースケース |
|:---|:---|:---|:---|
| `LENS_FACING_BACK` | `0` | カメラはデバイスの背面にあり、ユーザーとは反対側を向いている | 写真撮影、風景動画、AR |
| `LENS_FACING_FRONT` | `1` | カメラはデバイスの前面にあり、ユーザー側を向いている | 自撮り、ビデオ通話 |
| `LENS_FACING_EXTERNAL` | `2` | カメラはデバイスの外部にある（例：USB OTG ウェブカメラ） | 外部アクセサリ、特殊カメラ |

生の整数値を人間が読める文字列に変換する方法は以下の通りです：

```kotlin
fun lensFacingToString(facing: Int?): String = when (facing) {
    CameraCharacteristics.LENS_FACING_BACK -> "背面 (LENS_FACING_BACK)"
    CameraCharacteristics.LENS_FACING_FRONT -> "前面 (LENS_FACING_FRONT)"
    CameraCharacteristics.LENS_FACING_EXTERNAL -> "外部 / USB OTG (LENS_FACING_EXTERNAL)"
    null -> "不明 (null)"
    else -> "不明 (値=$facing)"
}
```

### 特例：外部カメラ (USB OTG)

`LENS_FACING_EXTERNAL` は API 23 (Marshmallow) で追加されました。外部カメラを扱う際は以下を考慮してください：
- **USB ホスト機能の宣言**: マニフェストに `<uses-feature android:name="android.hardware.usb.host" />` を追加します。
- **権限**: 多くの場合 `CAMERA` 権限のみでアクセス可能ですが、一部のチップセットでは `UsbManager` を介した追加の確認が必要な場合があります。
- **ハードウェアレベル**: 外部カメラは通常、後述の `INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL` を報告し、機能が制限されます。

## キー 2: INFO_SUPPORTED_HARDWARE_LEVEL — そのカメラに何ができるか？

ハードウェアレベルは、Camera2 において最も重要な機能分類です。カメラのハードウェアと HAL (Hardware Abstraction Layer) が、完全な Camera2 パイプラインを実装しているか、あるいは古い Camera API の互換性ラッパーを使用しているかを示します。

| レベル | 値 | 意味 | 実デバイスの例 |
|:---|:---|:---|:---|
| `LEGACY` | `2` | レガシー HAL モード。古い API のシム上で動作。機能が非常に制限され、マニュアル制御や RAW は不可。 | 低価格帯、古いデバイス、エミュレータ |
| `LIMITED` | `0` | 限定的な HAL3 サポート。基本的なキャプチャは可能だが、高度な機能は欠落。 | 中価格帯、フラッグシップの前面カメラ |
| `FULL` | `1` | 完全な HAL3 サポート。マニュアル制御、RAW 出力、再処理が可能。 | フラッグシップの背面メインカメラ |
| `LEVEL_3` | `3` | 拡張 HAL3 サポート。YUV 再処理、高速構成などを追加。 | 最新フラッグシップ、Pixel 6以降のメインカメラ |
| `EXTERNAL` | `4` | 外部カメラ (USB/OTG)。UVC クラスのデバイス。 | USB ウェブカメラなど |

階層構造は `LEGACY → LIMITED → FULL → LEVEL_3` となっており、上位のレベルは下位のすべての機能を包含します。

:::tip
高度な機能を使いたい場合は、必ずハードウェアレベルをチェックしてください。Android Camera Parameters アプリでは、各カメラのハードウェアレベルが目立つバッジとして表示されるため、デバイスが何をサポートしているか一目で確認できます。
:::

## 完全な Kotlin コード：カメラ検出ユーティリティ

（MainActivity.kt のコード例、コメントやメッセージを日本語化して提供）

```kotlin
// ... (imports)

class MainActivity : AppCompatActivity() {
    // ... (前章までのコード)

    // -------------------------------------------------------------------------
    // 📸 第6章 追加部分: カメラ検出と特性の照会
    // -------------------------------------------------------------------------
    data class CameraInfo(
        val id: String,
        val lensFacing: Int?,
        val hardwareLevel: Int?
    ) {
        fun description(): String = buildString {
            append("カメラ ID: $id | ")
            append("向き: ${lensFacingToString(lensFacing)} | ")
            append("HW レベル: ${hardwareLevelToString(hardwareLevel)}")
        }
    }

    private fun discoverAndLogCameras() {
        val cameraIdList: Array<String> = try {
            cameraManager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.e(TAG, "カメラ ID リストの取得に失敗しました", e)
            Toast.makeText(this, "カメラサービスを利用できません", Toast.LENGTH_LONG).show()
            return
        }

        if (cameraIdList.isEmpty()) {
            Log.w(TAG, "このデバイスにカメラは見つかりませんでした")
            return
        }

        val discoveredCameras = mutableListOf<CameraInfo>()
        Log.i(TAG, "═══════════════════════════════════════════")
        Log.i(TAG, "カメラ検出を開始します (${cameraIdList.size} 台のカメラ)")
        Log.i(TAG, "═══════════════════════════════════════════")

        for ((index, cameraId) in cameraIdList.withIndex()) {
            try {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val hardwareLevel =
                    characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

                val info = CameraInfo(cameraId, lensFacing, hardwareLevel)
                discoveredCameras.add(info)

                Log.i(TAG, "── カメラ $index ──")
                Log.i(TAG, info.description())

            } catch (e: CameraAccessException) {
                Log.e(TAG, "カメラ $cameraId の特性へのアクセスに失敗しました", e)
            }
        }
        // ... (サマリーのログ出力と Toast)
    }

    // ... (ヘルパー関数)
}
```

## まとめ

この章では、意味のないカメラ ID 文字列を、デバイスのカメラハードウェアに関する実用的な情報へと変換しました。

1. **カメラ ID のセマンティクス**: ID をハードコードしてはいけない理由。
2. **CameraCharacteristics の基礎**: `get` メソッドを使用した特性の照会方法。
3. **LENS_FACING**: 背面、前面、外部の 3 つの向きの理解。
4. **INFO_SUPPORTED_HARDWARE_LEVEL**: `LEGACY` から `LEVEL_3` までの機能階層。
5. **堅牢なカメラ検出**: 適切なエラー処理を備えたカメラ列挙の実装。

これで、本物の Camera2 メタデータがアプリケーション内を流れるようになりました。これは大きなマイルストーンです。

## 次のステップ

カメラを選択できたら、次はいよいよハードウェアの電源を入れて接続します。**第7章：カメラを開く**では、`CameraDevice` のライフサイクル管理、状態コールバックの実装、および発生し得るエラーへの対処方法について学びます。
