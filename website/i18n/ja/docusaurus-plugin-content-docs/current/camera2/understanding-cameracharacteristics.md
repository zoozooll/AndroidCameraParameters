---
sidebar_position: 7
title: "第7章: CameraCharacteristicsを理解する"
description: CameraCharacteristicsを探索して、レンズの向き、ハードウェアレベル、センサーサイズ、その他の重要なカメラ機能について学びます。
keywords: [CameraCharacteristics, レンズの向き, ハードウェアレベル, センサーサイズ, カメラ機能]
---

CameraCharacteristicsは、カメラの本質を知るための窓口です。一緒に探索していきましょう。

## はじめに

前の章では、カメラを一覧表示して基本的な情報を取得する方法を学びました。今回は、カメラの機能を包括的に記述する**CameraCharacteristics**をさらに深く掘り下げていきます。

CameraCharacteristicsには数百のパラメータが含まれています。この章では、最も重要なものに焦点を当てます。

## CameraCharacteristicsとは？

CameraCharacteristicsは、カメラデバイスに関するすべてのメタデータを含む不変のオブジェクトです。以下のことを記述しています：

- **ハードウェアプロパティ** — センサーサイズ、レンズ特性
- **機能** — カメラが何ができるか
- **モード** — 使用可能なフォーカス、露出、ホワイトバランスモード
- **出力オプション** — サポートされる解像度とフォーマット
- **パフォーマンス** — フレームレート、露出範囲

CameraManagerからCameraCharacteristicsを取得します：

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)
```

## 主要なCameraCharacteristicsキー

最も重要な特性を見ていきましょう。

### 1. レンズの向き

```kotlin
val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
```

考えられる値：
- `LENS_FACING_FRONT` — フロントカメラ（自撮り）
- `LENS_FACING_BACK` — リアカメラ
- `LENS_FACING_EXTERNAL` — 外部カメラ

### 2. ハードウェアレベル

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
```

これは最も重要な特性の1つです：

| レベル | APIレベル | 機能 |
| --- | --- | --- |
| **LEGACY** | 21 | 限定的なCamera2サポート、古いCamera APIのラッパー |
| **LIMITED** | 21 | 基本的なCamera2機能、マニュアル制御なし |
| **FULL** | 21 | フルマニュアル制御、RAWキャプチャ |
| **LEVEL_3** | 24 | YUV再処理などの高度な機能 |

### 3. センサーサイズ

```kotlin
val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
// sensorSize.width と sensorSize.height が寸法を示します
```

センサーサイズは、センサーが何ピクセル持っているかを示します。これは画像の解像度とは異なります—センサーには、1回のキャプチャで使用されるよりも多くのピクセルが存在する場合があります。

### 4. アクティブアレイサイズ

```kotlin
val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
```

アクティブアレイは、画像のキャプチャに使用されるセンサーの実際の領域です。一部のピクセルはキャリブレーション用に予約されているため、通常はピクセルアレイよりわずかに小さくなります。

### 5. 使用可能な機能

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
```

この配列は、カメラがどの機能をサポートしているかを示します：
- `BACKWARD_COMPATIBLE` — 基本的な互換性
- `MANUAL_SENSOR` — マニュアルセンサー制御
- `MANUAL_POST_PROCESSING` — マニュアルポストプロセッシング
- `RAW` — RAWキャプチャサポート
- `BURST_CAPTURE` — 連写キャプチャ
- `YUV_REPROCESSING` — YUV再処理
- `DEPTH_OUTPUT` — 深度出力
- `CONSTRAINED_HIGH_SPEED_VIDEO` — 高速ビデオ

### 6. 出力フォーマット

```kotlin
val outputFormats = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.outputFormats
```

ストリーム構成マップには、カメラがサポートするすべての出力フォーマットとサイズが含まれています：
- `ImageFormat.JPEG` — 標準JPEG
- `ImageFormat.RAW_SENSOR` — RAWセンサーデータ
- `ImageFormat.YUV_420_888` — YUVフォーマット
- `ImageFormat.RAW10` — 10ビットRAW
- `ImageFormat.RAW12` — 12ビットRAW

### 7. サポートされるプレビューサイズ

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)
```

これにより、カメラで使用可能なすべてのプレビュー解像度が取得できます。

### 8. サポートされる撮影サイズ

```kotlin
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)
```

これらは静止画キャプチャで使用可能な解像度です。

### 9. 焦点距離

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
```

この配列には、レンズの焦点距離（ミリメートル単位）が含まれています。複数の値がある場合は、光学ズーム機能があることを示します。

### 10. フォーカス距離範囲

```kotlin
val focusRange = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
```

最小フォーカス距離は、カメラがどれだけ近くにピントを合わせられるかを示します。値が小さいほど、マクロ機能が優れています。

## 実践的な例

より詳細なカメラ情報アプリを作成してみましょう：

```kotlin
private fun displayCameraDetails(cameraId: String) {
    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    
    // レンズの向き
    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
    val lensFacingStr = when (lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> "フロント"
        CameraCharacteristics.LENS_FACING_BACK -> "バック"
        else -> "外部"
    }
    
    // ハードウェアレベル
    val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    val hardwareLevelStr = when (hardwareLevel) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        else -> "不明"
    }
    
    // センサーサイズ
    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    val sensorSizeStr = "${sensorSize?.width} x ${sensorSize?.height}"
    
    // アクティブアレイサイズ
    val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    val activeArrayStr = "${activeArray?.width} x ${activeArray?.height}"
    
    // 焦点距離
    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    val focalLengthsStr = focalLengths?.joinToString(", ") { "%.1fmm".format(it) } ?: "不明"
    
    // 使用可能な機能
    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
    val capabilitiesList = capabilities?.map { capability ->
        when (capability) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "後方互換"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "マニュアルセンサー"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAWキャプチャ"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "連写キャプチャ"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV再処理"
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "深度出力"
            else -> "不明な機能"
        }
    } ?: emptyList()
    
    Log.d("CameraDetails", "=== カメラ $cameraId ===")
    Log.d("CameraDetails", "レンズの向き: $lensFacingStr")
    Log.d("CameraDetails", "ハードウェアレベル: $hardwareLevelStr")
    Log.d("CameraDetails", "センサーサイズ: $sensorSizeStr")
    Log.d("CameraDetails", "アクティブアレイ: $activeArrayStr")
    Log.d("CameraDetails", "焦点距離: $focalLengthsStr")
    Log.d("CameraDetails", "機能: ${capabilitiesList.joinToString(", ")}")
}
```

## 出力例

```
=== カメラ 0 ===
レンズの向き: バック
ハードウェアレベル: FULL
センサーサイズ: 4032 x 3024
アクティブアレイ: 4000 x 3000
焦点距離: 2.4mm, 4.8mm
機能: 後方互換, マニュアルセンサー, RAWキャプチャ, 連写キャプチャ
```

## CameraCharacteristicsが重要な理由

カメラを開いたりキャプチャセッションを作成したりする前に、**必ず**CameraCharacteristicsを確認する必要があります：

1. **機能を確認する** — 機能がサポートされていると思い込まない
2. **適切なカメラを選ぶ** — レンズの向き、ハードウェアレベルなどに基づいて選択する
3. **出力を構成する** — サポートされている解像度とフォーマットを使用する
4. **デバイスの違いに対応する** — あるデバイスで動作するものが、別のデバイスでも動作するとは限らない

## Android Camera Parametersで探索する

Android Camera Parametersアプリを開いて、特性を参照してください。カテゴリ別に整理された数百のパラメータが表示されます：

- **カメラ情報** — 基本的なカメラ情報
- **センサー** — センサー特性
- **レンズ** — レンズプロパティ
- **制御** — 自動露出、オートフォーカス、ホワイトバランス
- **スケーラー** — 出力サイズとフォーマット
- **フラッシュ** — フラッシュ機能
- **統計** — 統計出力

これにより、カメラの機能の全体像が把握できます。

## 次の章

CameraCharacteristicsを理解したので、最初のカメラを開く準備が整いました！次の章では：

1. CameraDeviceについて学ぶ
2. CameraManagerを使用してカメラを開く
3. カメラ状態コールバックを処理する
4. カメラのライフサイクルを理解する

## まとめ

CameraCharacteristicsには、カメラの機能を理解するために必要なすべての情報が含まれています：

- **レンズの向き** — フロント、バック、または外部
- **ハードウェアレベル** — LEGACY、LIMITED、FULL、LEVEL_3
- **センサーサイズ** — 物理的な寸法
- **アクティブアレイ** — キャプチャ領域
- **焦点距離** — レンズ機能
- **機能** — サポートされる機能
- **出力フォーマット** — 使用可能な画像フォーマット

カメラを使用する前には、常にCameraCharacteristicsを確認してください。これにより、アプリがさまざまなデバイスで確実に動作するようになります。

次の章では、CameraDeviceを使用して最初のカメラを開きます。
