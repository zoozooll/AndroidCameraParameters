---
sidebar_position: 15
title: "第15章：CameraCharacteristics大辞典"
description: 最も重要なCamera2特性に関する包括的なガイド。それらが何を意味するのか、なぜ存在するのか、そしてどのように使用するのかを説明します。
keywords: [CameraCharacteristics, カメラパラメータ, カメラ機能, Android Camera2, SENSOR_INFO_ACTIVE_ARRAY_SIZE]
---

CameraCharacteristics大辞典へようこそ — あらゆるカメラパラメータを理解するためのガイドです。

## はじめに

CameraCharacteristicsには、カメラの機能を説明する何百ものパラメータが含まれています。この章では、最も重要なものを詳しく探っていきます：

1. `INFO_SUPPORTED_HARDWARE_LEVEL` — カメラは何ができるのか？
2. `REQUEST_AVAILABLE_CAPABILITIES` — どのような機能が利用可能か？
3. `SENSOR_INFO_ACTIVE_ARRAY_SIZE` — センサーサイズはどれくらいか？
4. `SCALER_AVAILABLE_MAX_DIGITAL_ZOOM` — ズームはどれくらいできるか？
5. `CONTROL_AE_AVAILABLE_MODES` — どのような露出モードがあるか？

その他多数...

## 1. INFO_SUPPORTED_HARDWARE_LEVEL

**意味：**  
これは最も重要な特性です。カメラデバイスの全体的な機能レベルを定義します。

**なぜ存在するのか：**  
Androidデバイスによってカメラ機能は異なります。このパラメータは、アプリケーションが何ができるかを理解するのに役立ちます。

**サポートされている値：**

| 値 | APIレベル | 説明 |
| --- | --- | --- |
| `LEGACY` | 21 | 旧デバイス、Camera2 APIは旧Camera APIのラッパー |
| `LIMITED` | 21 | 基本的なCamera2機能、マニュアル制御なし |
| `FULL` | 21 | フルマニュアル制御、RAWキャプチャ、バーストキャプチャ |
| `LEVEL_3` | 24 | YUV再処理、10-bit HDRなどの高度な機能 |

**使用方法：**  
高度な操作を試行する前にこれを確認してください：

```kotlin
val hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

when (hardwareLevel) {
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
        // 機能が制限されています
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
        // 基本機能のみ
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
        // フルマニュアル制御が利用可能
    }
    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
        // 高度な機能が利用可能
    }
}
```

**Android Camera Parametersでの確認方法：**  
アプリを開き、Camera Infoセクションの「Hardware Level」を探してください。

---

## 2. REQUEST_AVAILABLE_CAPABILITIES

**意味：**  
この配列は、カメラがサポートするすべての機能をリストします。

**なぜ存在するのか：**  
同じハードウェアレベル内でも、デバイスによってサポートする機能が異なる場合があります。

**一般的な機能：**

| 機能 | 説明 |
| --- | --- |
| `BACKWARD_COMPATIBLE` | 基本互換モード |
| `MANUAL_SENSOR` | マニュアルISOおよび露出制御 |
| `MANUAL_POST_PROCESSING` | マニュアル色補正およびノイズリダクション |
| `RAW` | RAW画像キャプチャ |
| `BURST_CAPTURE` | 高速バーストキャプチャ |
| `YUV_REPROCESSING` | YUV画像再処理 |
| `DEPTH_OUTPUT` | 深度マップ出力 |
| `CONSTRAINED_HIGH_SPEED_VIDEO` | 高速ビデオキャプチャ |
| `LOGICAL_MULTI_CAMERA` | 複数の物理カメラを組み合わせた論理カメラ |
| `CONCURRENT_CAMERA` | 複数のカメラを同時に開くことができる |
| `CAMERA_EXTENSION` | メーカー固有の拡張機能（ポートレート、ナイトモード） |

**使用方法：**  
機能を使用する前に機能を確認してください：

```kotlin
val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) ?: false
val supportsManualSensor = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) ?: false

if (supportsRaw) {
    // RAWキャプチャを有効にする
}
```

**Android Camera Parametersでの確認方法：**  
Camera Infoセクションの「Available Capabilities」を探してください。

---

## 3. SENSOR_INFO_ACTIVE_ARRAY_SIZE

**意味：**  
アクティブアレイは、画像キャプチャに使用されるセンサーの実際の領域です。

**なぜ存在するのか：**  
センサーのエッジ周辺には、キャリブレーション用に予約されたピクセルがある場合があります。アクティブアレイは使用可能な領域を表します。

**使用方法：**  
これにより、キャプチャに利用可能な最大解像度がわかります：

```kotlin
val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
val width = activeArray?.width ?: 0
val height = activeArray?.height ?: 0

Log.d("Camera", "Active array: ${width}x$height")
```

**関連する特性：**
- `SENSOR_INFO_PIXEL_ARRAY_SIZE` — センサーの総ピクセル数（アクティブアレイより大きい場合があります）
- `SENSOR_INFO_SENSOR_SIZE` — ミリメートル単位の物理的な寸法

**Android Camera Parametersでの確認方法：**  
Sensorセクションの「Active Array Size」と「Sensor Size」を探してください。

---

## 4. SCALER_AVAILABLE_MAX_DIGITAL_ZOOM

**意味：**  
カメラがサポートする最大デジタルズーム倍率。

**なぜ存在するのか：**  
デジタルズームは画像をトリミングして拡大するため、品質が低下します。最大値を知ることで、ユーザーの期待を管理するのに役立ちます。

**使用方法：**  
キャプチャリクエストでズームレベルを設定します：

```kotlin
val maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

// ズームを設定（1.0 = ズームなし、maxZoom = 最大ズーム）
captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, Rect(0, 0, width, height))
```

**関連する特性：**
- `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` — 物理的な焦点距離（光学ズーム用）

**Android Camera Parametersでの確認方法：**  
Scalerセクションの「Max Digital Zoom」を探してください。

---

## 5. CONTROL_AE_AVAILABLE_MODES

**意味：**  
利用可能な自動露出モード。

**なぜ存在するのか：**  
デバイスによってサポートするAE戦略が異なります。

**一般的なモード：**

| モード | 説明 |
| --- | --- |
| `CONTROL_AE_MODE_OFF` | マニュアル露出制御 |
| `CONTROL_AE_MODE_ON` | 自動露出 |
| `CONTROL_AE_MODE_ON_ALWAYS_FLASH` | 常時フラッシュ付き自動露出 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH` | オートフラッシュ付き自動露出 |
| `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` | 赤目軽減付き自動露出 |
| `CONTROL_AE_MODE_ON_EXTERNAL_FLASH` | 外部フラッシュ付き自動露出 |

**使用方法：**  
キャプチャリクエストでAEモードを設定します：

```kotlin
val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)

if (aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true) {
    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
}
```

**Android Camera Parametersでの確認方法：**  
Controlセクションの「AE Available Modes」を探してください。

---

## 6. CONTROL_AF_AVAILABLE_MODES

**意味：**  
利用可能なオートフォーカスモード。

**なぜ存在するのか：**  
シナリオに応じて異なるフォーカス戦略を使用するためです。

**一般的なモード：**

| モード | 説明 |
| --- | --- |
| `CONTROL_AF_MODE_OFF` | マニュアルフォーカス |
| `CONTROL_AF_MODE_AUTO` | 単発オートフォーカス |
| `CONTROL_AF_MODE_MACRO` | マクロフォーカス |
| `CONTROL_AF_MODE_CONTINUOUS_VIDEO` | ビデオ用連続オートフォーカス |
| `CONTROL_AF_MODE_CONTINUOUS_PICTURE` | 写真用連続オートフォーカス |
| `CONTROL_AF_MODE_EDGE` | エッジオートフォーカス |
| `CONTROL_AF_MODE_FIXED` | 固定フォーカス（AFなし） |

**使用方法：**  
ユースケースに基づいてAFモードを設定します：

```kotlin
captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
```

**Android Camera Parametersでの確認方法：**  
Controlセクションの「AF Available Modes」を探してください。

---

## 7. CONTROL_AWB_AVAILABLE_MODES

**意味：**  
利用可能なオートホワイトバランスモード。

**一般的なモード：**

| モード | 説明 |
| --- | --- |
| `CONTROL_AWB_MODE_OFF` | マニュアルホワイトバランス |
| `CONTROL_AWB_MODE_AUTO` | 自動 |
| `CONTROL_AWB_MODE_INCANDESCENT` | タングステン照明 |
| `CONTROL_AWB_MODE_FLUORESCENT` | 蛍光灯照明 |
| `CONTROL_AWB_MODE_WARM_FLUORESCENT` | 温白色蛍光灯 |
| `CONTROL_AWB_MODE_DAYLIGHT` | 昼光 |
| `CONTROL_AWB_MODE_CLOUDY_DAYLIGHT` | 曇り |

**Android Camera Parametersでの確認方法：**  
Controlセクションの「AWB Available Modes」を探してください。

---

## 8. LENS_INFO_AVAILABLE_FOCAL_LENGTHS

**意味：**  
レンズで利用可能な焦点距離。

**なぜ存在するのか：**  
複数の値は光学ズーム機能を示します。

**使用方法：**  
どのレンズが利用可能かを確認します：

```kotlin
val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)

focalLengths?.forEach { fl ->
    Log.d("Camera", "Focal length: ${fl}mm")
}
```

**一般的な焦点距離：**
- 2.4mm — 広角（一般的）
- 4.8mm — 望遠（2倍光学ズーム）
- 1.8mm — 超広角

**Android Camera Parametersでの確認方法：**  
Lensセクションの「Available Focal Lengths」を探してください。

---

## 9. LENS_INFO_MINIMUM_FOCUS_DISTANCE

**意味：**  
レンズがフォーカスできる最も近い距離。

**なぜ存在するのか：**  
値が小さいほどマクロ機能が優れています。

**使用方法：**  
マクロ写真撮影が可能かどうかを確認します：

```kotlin
val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

// 0.1m（10cm）以下の値は良好なマクロ機能を示します
val hasGoodMacro = minFocusDistance != null && minFocusDistance <= 0.1f
```

**Android Camera Parametersでの確認方法：**  
Lensセクションの「Minimum Focus Distance」を探してください。

---

## 10. FLASH_INFO_AVAILABLE

**意味：**  
カメラにフラッシュがあるかどうか。

**なぜ存在するのか：**  
すべてのカメラにフラッシュがあるわけではありません（特にフロントカメラ）。

**使用方法：**  
フラッシュを使用する前に確認してください：

```kotlin
val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

if (hasFlash) {
    // フラッシュ機能を有効にする
}
```

**Android Camera Parametersでの確認方法：**  
Flashセクションの「Flash Available」を探してください。

---

## 11. SENSOR_INFO_EXPOSURE_TIME_RANGE

**意味：**  
サポートされる最小および最大露出時間。

**なぜ存在するのか：**  
低光量での性能と動体の鮮明さを決定します。

**使用方法：**  
マニュアル制御のために露出範囲を確認します：

```kotlin
val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
val minExposure = exposureRange?.lower ?: 1
val maxExposure = exposureRange?.upper ?: 1000000000L

// 表示用に秒に変換
val minExposureSeconds = minExposure / 1_000_000_000.0
val maxExposureSeconds = maxExposure / 1_000_000_000.0
```

**Android Camera Parametersでの確認方法：**  
Sensorセクションの「Exposure Time Range」を探してください。

---

## 12. SENSOR_INFO_SENSITIVITY_RANGE

**意味：**  
サポートされる最小および最大ISO値。

**なぜ存在するのか：**  
低光量での性能とノイズ性能を決定します。

**使用方法：**  
マニュアル制御のためにISO範囲を確認します：

```kotlin
val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
val minIso = isoRange?.lower ?: 100
val maxIso = isoRange?.upper ?: 3200
```

**Android Camera Parametersでの確認方法：**  
Sensorセクションの「Sensitivity Range」を探してください。

---

## 13. SCALER_STREAM_CONFIGURATION_MAP

**意味：**  
サポートされるすべての出力サイズとフォーマット。

**なぜ存在するのか：**  
どの解像度とフォーマットを使用できるかを決定します。

**使用方法：**  
さまざまなユースケースでサポートされているサイズを取得します：

```kotlin
val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

// プレビューサイズ
val previewSizes = configMap?.getOutputSizes(SurfaceTexture::class.java)

// 写真サイズ
val pictureSizes = configMap?.getOutputSizes(ImageFormat.JPEG)

// ビデオサイズ
val videoSizes = configMap?.getOutputSizes(MediaRecorder::class.java)

// RAWサイズ
val rawSizes = configMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
```

**Android Camera Parametersでの確認方法：**  
Scalerセクションの「Preview Sizes」、「Picture Sizes」などを探してください。

---

## 14. LENS_FACING

**意味：**  
レンズがどの方向を向いているか。

**なぜ存在するのか：**  
フロント、バック、または外部カメラのどれであるかを決定します。

**値：**
- `LENS_FACING_FRONT` — 自撮りカメラ
- `LENS_FACING_BACK` — リアカメラ
- `LENS_FACING_EXTERNAL` — 外部カメラ

**Android Camera Parametersでの確認方法：**  
Camera Infoセクションの「Lens Facing」を探してください。

---

## 15. CONTROL_MAX_REGIONS_AE

**意味：**  
AE測光領域の最大数。

**なぜ存在するのか：**  
露出測光の精度を決定します。

**使用方法：**  
作成するAE領域の数を制限します：

```kotlin
val maxAERegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1

// maxAERegionsを超えないように作成
```

**Android Camera Parametersでの確認方法：**  
Controlセクションの「Max Regions AE」を探してください。

---

## まとめ

CameraCharacteristicsは、カメラの機能を知るための窓です。これらのパラメータを理解することで、以下のことができます：

1. **デバイスに依存しないアプリを構築する** — 機能を使用する前に機能を確認する
2. **より良いユーザー体験を提供する** — 利用可能な機能のみを表示する
3. **パフォーマンスを最適化する** — 適切な解像度とフォーマットを選択する
4. **プロフェッショナルなアプリケーションを作成する** — カメラの可能性を最大限に引き出す

## さらに学ぶために

1. **Android Camera Parametersアプリ** — お使いのデバイスの実際のデータを調べる
2. **Androidドキュメント** — 公式のCameraCharacteristicsドキュメントを読む
3. **実験する** — 小さなテストアプリを書いてさまざまなパラメータを試す
4. **ソースコード** — より深い理解のためにCamera2ソースコードを見る

## あゆみ

この章では、最も重要なCameraCharacteristicsについて説明しました：

1. **ハードウェアレベル** — 全体的な機能
2. **機能** — 利用可能な特定の機能
3. **アクティブアレイ** — センサー解像度
4. **デジタルズーム** — ズーム機能
5. **AE/AF/AWBモード** — 自動制御モード
6. **焦点距離** — レンズ機能
7. **フォーカス距離** — マクロ機能
8. **フラッシュ** — フラッシュの可用性
9. **露出/ISO範囲** — マニュアル制御の制限
10. **ストリーム構成** — サポートされているサイズとフォーマット

この知識があれば、高度なCamera2アプリケーションを構築する準備が整っています！

---

## 最後に

おめでとうございます！Android Camera2シリーズを完了しました。これで以下のことが理解できるようになりました：

- **スマートフォンカメラの仕組み** — レンズ、センサー、ISP
- **Camera2の仕組み** — CameraManager、CameraDevice、CaptureSession
- **写真の撮影方法** — JPEG、RAW、ImageReader
- **カメラの制御方法** — ISO、露出、フォーカス、ホワイトバランス
- **プロフェッショナルな機能** — 高速ビデオ、マルチカメラ
- **カメラ特性** — カメラ機能の大辞典

Android Camera Parametersアプリは、学び続けるための素晴らしいツールです。お使いのデバイスの機能を調べ、さまざまな設定を試してみてください。

ハッピーコーディング！ 📸