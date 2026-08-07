---
sidebar_position: 28
title: "第28章：Camera2アーキテクチャ"
description: "Camera2の集大成となるアーキテクチャ。KotlinアプリからBinder IPC、Framework、ネイティブのCameraService、Camera3Device、camera3_device_tを備えたHAL3、V4L2カーネルドライバ、そして最終的な物理センサー、ISP、VCMレンズ、フラッシュハードウェアまでのフルスタックを網羅します。Treble HAL要件、LEGACY HAL1ラッパー、Android 15のCameraDeviceSetupも含まれます。"
keywords: [camera2 アーキテクチャ, hal3, camera3_device_t, cameraservice, binder ipc, v4l2 ドライバ, mipi csi-2, camera devicesetup, android treble hal, legacy hal1 ラッパー, カーネルカメラドライバ, カメラ isp, vcm ボイスコイル]
---

# 第28章：Camera2アーキテクチャ

## まとめ

これは、あなたがこれまでの学習を通じて勝ち取った章です。第1章から第27章までに、`CameraManager`、`CameraCharacteristics`、`CaptureRequest`、`CaptureResult`、`CameraCaptureSession`、`ImageReader`、CameraX、NDKネイティブスタック、コルーチンラッパー、そしてテストモックを使用してきました。あなたはすべての公開APIサーフェスを知っています。今、私たちはそれらすべての抽象化を順番に剥ぎ取っていきます。あなたが書くKotlinのコードから、センサーとSoCの間でMIPI CSI-2バスを横切る個々の電子、レンズ群を10マイクロメートル動かすボイスコイルモーター（VCM）、そしてセンサーのローリングシャッターとマイクロ秒単位で同期してパルスを発するフラッシュLEDコントローラーに至るまでを辿ります。

この章を読み終える頃には、あらゆる `CaptureRequest` を見て、その各部分がどこへ行き、誰が変換し、誰が検証し、誰が最終的にシリコン上で実行するのかを、レイヤーごとにマッピングできるようになるでしょう。また、Android 15 (API 35) の `CameraDeviceSetup` 抽象化についても、10年にわたるアーキテクチャ上のトレンドの一例として理解できるようになります。それは、アプリがセンサーやISPを起動するために必要な約300mWの電力を消費することなくカメラを調査できるように、*機能クエリ* を *ハードウェアの電源状態* から段階的に分離していくというトレンドです。

実機の正確な機能を調査し、ここで説明するアーキテクチャレイヤーと照らし合わせるには、**Android Camera Parameters**（[Google Play](https://play.google.com/store/apps/details?id=com.minininja.cameraparams)、[GitHub](https://github.com/zoozooll/AndroidCameraParameters)）をインストールしてください。このアプリは、下層のレイヤーが公開APIに公開しているすべての `CameraCharacteristics` キーを読み取ります。

---

## フルスタックレイヤー図

これは本書全体で最も重要な図です。ここから下の各レイヤーは、Android Open Source Project (AOSP) に実在するコード、実在の所有者、そして実在のBinderまたは関数呼び出しの境界を持っています。各レイヤーを上から下へと進み、過去10年間のスタックの進化を示し、皆さんの学習の旅を各レイヤーにマッピングします。

```mermaid
graph TB
    subgraph APP["App Layer (your code)"]
        direction TB
        A1["Kotlin / Java / C++ NDK<br/>cameraManager.openCamera(id, cb, handler)<br/>session.capture(request, cb, handler)<br/>captureResult.get(SENSOR_EXPOSURE_TIME)"]
    end
    subgraph FRAME["Java/Kotlin Framework Layer — android.hardware.camera2.*"]
        direction TB
        F1["CameraManager · CameraCharacteristics<br/>CaptureRequest.Builder · CaptureResult<br/>CameraDevice · CameraCaptureSession"]
        F2["CameraBinderWrapper (AOSP frameworks/base)<br/>Translates Java objects → AIDL Binder parcel"]
    end
    subgraph BIND["IPC Layer — Binder / HwBinder"]
        direction TB
        B1["AIDL ICameraService (AOSP)<br/>Framework ↔ CameraService"]
        B2["HIDL / AIDL HAL Binder (Treble)<br/>CameraService ↔ Vendor HAL"]
    end
    subgraph NS["Native Mediaserver Layer (system/bin/cameraserver)"]
        direction TB
        N1["CameraService (frameworks/av/services/camera/)"]
        N2["Camera3Device<br/>frameworks/av/services/camera/libcameraservice/device3/<br/>— Validates request vs session outputs<br/>— Builds camera3_capture_request_t<br/>— Parses camera3_capture_result_t"]
        N3["CameraProviderManager (frameworks/av)<br/>Enumerates vendor HAL implementations"]
    end
    subgraph HAL["Vendor HAL Layer (OEM / SoC code)"]
        direction TB
        H1["HAL3 Interface: camera3_device_t<br/>— process_capture_request()<br/>— process_capture_result()<br/>— flush()"]
        H2["HAL1 Wrapper (Legacy)<br/>camera2compat::Camera2Compat<br/>Translates HAL3 request→HAL1 CameraParameters<br/>for < INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED"]
        H3["Vendor Implementation<br/>Qualcomm QCamera2 · MediaTek CamHAL · Samsung Exynos Camera HAL"]
    end
    subgraph K["Kernel Layer (Linux)"]
        direction TB
        K1["/dev/videoX — V4L2 Video Capture Driver<br/>VIDIOC_S_FMT · VIDIOC_REQBUFS · VIDIOC_QBUF / DQBUF"]
        K2["ISP Driver (Qualcomm CAMSS / MediaTek ISP Driver)<br/>Memory-to-memory processing V4L2 m2m node"]
        K3["Sensor Subdev Driver<br/>I2C writes for mode / exposure / gain / VCM"]
        K4["MIPI CSI-2 Receiver Driver (SoC)<br/>Lane configuration, LP/HS transitions, ECC/CRC check"]
    end
    subgraph HW["Physical Hardware Layer"]
        direction TB
        HW1["Lens Assembly<br/>VCM Voice Coil Motor (I2C)<br/>Moves lens group for focus / OIS"]
        HW2["Camera Sensor Pixel Array<br/>CMOS sensor (Sony IMX / Samsung ISOCELL / OmniVision)<br/>Expose → Readout → A/D"]
        HW3["MIPI CSI-2 Physical Bus<br/>2/4/8 differential pairs at 1.5 – 2.5 Gbps/lane"]
        HW4["ISP Image Signal Processor (on SoC)<br/>Demosaic · Noise Reduction · Sharpen · HDR merge · Face detect in hardware"]
        HW5["Flash LED Controller (I2C)<br/>Xenon strobe or LED current sink<br/>Synced to sensor EXRST pin"]
    end

    APP -->|function call| FRAME
    FRAME -->|AIDL parcel| BIND
    BIND -->|HwBinder| NS
    NS -->|HAL3 AIDL/HIDL| HAL
    HAL -->|ioctl() syscalls| K
    K -->|I2C writes + MIPI lane signals + ISP command queues| HW

    style APP fill:#e6d9f2
    style FRAME fill:#d9e6f2
    style BIND fill:#fff3cd,stroke:#ffc107,stroke-width:2px
    style NS fill:#d9f2e6
    style HAL fill:#f2d9e6
    style K fill:#e6d9e6
    style HW fill:#f2e6d9,stroke:#d4a373,stroke-width:2px
```

順を追って見ていきましょう。

---

## 第1レイヤー — アプリレイヤー（あなたのコード）

これはあなたが書いたコードです。 `cameraManager.openCamera(id, stateCallback, cameraHandler)` など。あなたはこのレイヤーを熟知しています。まだ内部化していないかもしれない2つの事実があります：
- `CaptureRequest.Builder.set(key, value)` を呼び出すたびに、`system/media/camera/include/system/camera_metadata.h` にあるC構造体 `camera_metadata_t` と正確に一致する、*タグ付きメタデータエントリ* がパース可能な構造体に追加されます。Kotlinの `CaptureRequest` とHALのリクエストの間に魔法のような変換はありません。それらは同じバイナリメタデータ形式であり、単に異なる言語バインディングでラップされているだけです。
- `CaptureResult.get(key)` を呼び出すたびに、HALがレスポンスバッファに書き込んだ正確なバイトを読み取ります。特定のOTAビルドでHALが露出時間を誤って報告した場合、アプリはその誤った値をそのまま読み取ります。HALの上位に、ベンダーのエラーを修正するフレームワークレベルの検証レイヤーは存在しません。そのため、第27章の実機サニティテストが存在するのです。

---

## 第2レイヤー — Java/Kotlin フレームワークレイヤー (`android.hardware.camera2.*`)

フレームワークレイヤー（AOSP `frameworks/base/core/java/android/hardware/camera2/`）は、次の2つのことだけを行います：
1. あなたが呼び出す公開APIサーフェス（`CameraManager`、`CameraDevice` など）を公開する。
2. `CaptureRequest` / `CaptureResult` のJavaオブジェクトと、それらのBinderパース可能な電送表現との間を変換する。

HALの上位でのポリシー適用は行いません。メタデータの書き換えも行いません。リクエストを「修正」することもありません。これは、デバイスの起動時にカメラIDごとに一度だけ取得される不変の `CameraCharacteristics` ブロブのキャッシュに加えて、薄い変換レイヤーとして機能します。

Binder境界は `CameraManager` → `ICameraService` AIDLにあり、これが次のレイヤーです。

---

## 第3レイヤー — IPCレイヤー：Binder / HwBinder (Treble)

これは、Project Treble（Android 8.0, 2017）が確定させた重要なアーキテクチャ上の契約です。2つのBinderドメインが関与しています：

| Binderドメイン | 接続先 | プロトコル | ABI安定性の強制 |
|:---|:---|:---|:---|
| `/dev/binder` | フレームワーク ↔ cameraserver (system_server側) | AIDL | プラットフォーム（同一パーティションビルド） |
| `/dev/hwbinder` | cameraserver ↔ ベンダーカメラHAL | HIDL / AIDL HAL | Treble（安定したベンダーインターフェース） |

Treble以前は、HALは `.so` として `cameraserver` のプロセスに直接dlopenされていました。OEMによるすべてのOTAは、カメラとフレームワークを一緒に再構築する必要がありました。TrebleのHwBinder分割により、ベンダーHALは独自のプロセス、独自のパーティション、独自の3年間のセキュリティアップデートタイムラインを持ち、`cameraserver` との契約はバージョン管理され、デバイスの寿命期間中凍結されます。アプリ開発者にとって、これは Camera2 API の動作がOTAを越えて予測可能である最大の理由です。HALインターフェースはTreble準拠テストを壊さずに変更することは事実上不可能です。

LEGACY HAL1ラッパーはこの境界の下、ベンダーHALプロセスの中に存在するため、`INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY` を通じて以外、アプリレイヤーからは見えません。

---

## 第4レイヤー — ネイティブメディアサーバーレイヤー：`CameraService` / `Camera3Device`

`/system/bin/cameraserver` は、起動時に `init.rc` によって開始されるネイティブデーモンです。常に実行されており、デバイス上のすべてのオープンなカメラを所有し、どのアプリがカメラアクセス権を得るかの唯一の裁定者です（最前面のアプリが勝ち、それ以外はすべて切断されます）。

その2つの最も重要なクラス：

1. **`CameraService`** (`frameworks/av/services/camera/libcameraservice/CameraService.cpp`):
   - フレームワークに `ICameraService` AIDLを公開します。
   - すべてのbinder呼び出しに対して `android.permission.CAMERA` 権限チェックを強制します（権限のないアプリの呼び出しは、HALに届く前に `cameraserver` で拒否されます）。
   - 同時オープンの裁定を処理します（2つのアプリが同じカメラを要求した場合、最前面のアクティビティが取得し、バックグラウンドアプリは `onDisconnected` を受け取ります）。
   - ベンダーHALモジュールを列挙するための `CameraProviderManager` を管理します。

2. **`Camera3Device`** (`frameworks/av/services/camera/libcameraservice/device3/Camera3Device.cpp`):
   - パイプラインの心臓部です。
   - キャプチャリクエスト内のすべての出力Surfaceが、実際にセッションの構成済み出力セットの一部であることを検証します（ここでフレームワークが `IllegalArgumentException: Surface not in configured outputs` をスローします）。
   - パースされた `CaptureRequest` をHAL3の `camera3_capture_request_t` 構造体にパッケージ化します。
   - `process_capture_request(request)` を介して、リクエストを1つずつHALにストリーミングします。
   - HALから `camera3_capture_result_t` を受け取り、メタデータなどをパースして、Binderチェーンを遡ってあなたの `CaptureCallback.onCaptureCompleted` へ転送します。
   - `flush()`、エラーパス、`notify()` シャッターおよびエラーコールバック、そしてEGL/Vulkan相互運用のための出力バッファリリースフェンスを処理します。

`Camera3Device` は約15,000行のC++コードであり、スタック全体で最も集中的にテストされている部分です。もし「このリクエストキーが Camera2 NDK では機能するが Java Camera2 では機能しない」というバグ報告があれば、その原因のほとんどは `Camera3Device` 内部の検証または変換パスの欠落にあります。

---

## 第5レイヤー — ベンダーHALレイヤー：HAL3 (`camera3_device_t`)

ここでOEMの差別化が実際に行われます。すべてのSoCベンダーが独自のHAL3実装を出荷しています（Qualcomm QCamera, MediaTek CamHAL, Samsung Exynos Camera HAL, Pixel の Google Camera HAL など）。

HAL3の契約（`hardware/libhardware/include/hardware/camera3.h` で定義）は、オープンされたデバイスに対する正確に4つのコア操作です：構成、リクエスト処理、ベンダータグ取得、フラッシュ、ダンプ。

HALはリクエストを受け取り、結果と出力バッファを生成します。それだけです。このリクエスト/レスポンスモデルがHAL3の特徴です。HAL1は単一の `CameraParameters` 文字列ブロブ（`"preview-size=1920x1080;..."`）であり、フレームごとの制御ができないため業界全体で嫌われていました。HAL3のリクエスト/レスポンスモデルこそが、本書で使用してきたすべての高度な機能（フレームごとのマニュアル露出、RAWキャプチャ、マルチカメラ、再処理、ZSLなど）を可能にしているのです。

### LEGACY HAL1 ラッパー

`INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY` は、ベンダーがまだHAL1の `.so` しか出荷しておらず、デバイスが AOSP の `camera2compat::Camera2Compat` シムを使用して、HAL3のリクエスト/レスポンス呼び出しを古い `CameraParameters` ブロブ + `startPreview()`/`takePicture()` などのHAL1エントリポイントに変換していることを意味します。この変換レイヤーがあるため、第24章で警告したように、`CONTROL_MODE_OFF` は `LEGACY` デバイスでは何もしません。HAL1には変換先のフレームごとの `CONTROL_MODE` という概念がないため、シムはそのメタデータエントリを破棄します。

---

## 第6レイヤー — カーネルレイヤー：V4L2 + MIPI CSI-2 + センサープロセッサ

HAL3プロセスは、`ioctl()` システムコールを介してのみLinuxカーネルを呼び出します。4つのカテゴリのカーネルドライバが相互作用して1つのフレームを処理します：

1. **MIPI CSI-2 レシーバドライバ** (`/dev/v4l-subdevX`): 物理的なレーン構成やデータレートを管理し、パケットのECC/CRC検証を行い、受信したピクセルラインをISPの入力リングバッファにDMA転送します。
2. **センサーサブデバイスドライバ** (`/dev/v4l-subdevY`, I2C制御): I2C（低速なサイドバンドバス）を介してセンサーレジスタを書き込みます。露出時間、アナログゲイン、バイニングモードなどを設定し、VCM（ボイスコイルモーター）を介してフォーカスを制御します。
3. **V4L2 ビデオキャプチャノード** (`/dev/video0` など): HALが `VIDIOC_REQBUFS` を呼び出してバッファを割り当て、`VIDIOC_QBUF` でエンキューします。フレームが到着すると `VIDIOC_DQBUF` でデキューし、`Camera3Device` へ送ります。
4. **ISP Memory-to-Memory ドライバ** (`/dev/videoN m2m` ノード): キャプチャパスとは別に、HALは再処理用入力バッファ（ZSLなど）をISPのキューに入れ、デモザイク、デノイズ、HDR合成などを実行します。

---

## 第7レイヤー — 物理ハードウェアレイヤー

最終的に、電子の世界です。これより上のレイヤーはすべてSoC上で実行されるコードです。ハードウェアレイヤーは光子が電子に変換され、処理される場所です。

- **レンズとVCM**: レンズを10µm動かすことがフォーカスの1ステップです。OIS（光学式手ぶれ補正）は、ジャイロフィードバックに基づいてレンズを毎秒500〜5000回微調整します。
- **センサーピクセルアレイ**: フォトダイオードが光子の数に比例した電荷を蓄積します。読み出しはローリングシャッター（上から下へ一行ずつ）であり、そのため露出の変更には数フレームのレイテンシが生じます。
- **MIPI CSI-2 バス**: 差動ペアを使用して最大20Gbpsの高速通信を行います。
- **ISP**: 影のヒーローです。デモザイク、ノイズ除去、シャープネス処理を毎秒1ギガピクセル以上の速度で行い、CPUの負荷を軽減します。
- **フラッシュコントローラー**: フラッシュのパルスは、照射すべきフレームのローリングシャッター露出窓と *正確に* 同期して発光する必要があります。

---

## アーキテクチャの進化：AndroidバージョンごとのCamera2

Camera2は一日にして成らず。2〜3つのAndroidバージョンごとに、開発者のための新しいアーキテクチャ上のプリミティブが追加されてきました。

- **Android 5.0 (2014)**: Camera2 公開APIの開始。
- **Android 8.0 (2017)**: Project Treble。HwBinder 分割。
- **Android 9.0 (2018)**: 論理マルチカメラ。
- **Android 12 (2021)**: Camera Extensions API。
- **Android 14 (2023)**: JPEG_R Ultra HDR フォーマット。
- **Android 15 (2024)**: `CameraDeviceSetup`。軽量な機能クエリ。センサーを起動せずに機能を調査可能。

進化の傾向は明確です：**デカップリング（切り離し）**。Android 15の `CameraDeviceSetup` は、機能クエリをハードウェアの電力状態から切り離した、この傾向の最も純粋な例です。

---

## 学習の旅とアーキテクチャレイヤーのマッピング

（Mermaidダイアグラムは維持。テキストは日本語化）

この章を最後に読むことで、アーキテクチャと実践を一致させることができました。あなたは初日にHAL3を抽象的に学んで実コードとのマッピングに苦労するのではなく、「開く → 構成 → キャプチャ → 結果」というプロセスを27章にわたって *実践* することで学び、最後にカーテンを引き開けて、すべての呼び出しに誰が実際に応答していたのかを確認したのです。

---

## まとめ

Camera2は7つのレイヤーで構成されています：アプリ → フレームワーク → Binder/HwBinder IPC → ネイティブCameraService/Camera3Device → ベンダーHAL3 → V4L2カーネルドライバ → 物理ハードウェア。Project TrebleはHwBinderを介して契約を固定し、長期的な安定性を確保しました。Android 15の `CameraDeviceSetup` は、センサーを起動せずに機能を照会できるデカップリングの集大成です。あなたは今、第14章のマニュアルISOから第23章のZSL、第25章のネイティブVulkanゼロコピーに至るまで、すべての機能をそれを実行する正確なレイヤーに関連付けることができました。

## 次のステップ：第VII部 — カメラメタデータ百科事典

これで第VI部「現代的なAndroidカメラ開発」を終了します。残されたフロンティアは、これまでの28章で使用してきたすべての `CameraCharacteristics`、`CaptureRequest`、`CaptureResult` メタデータキーに関する詳細な百科事典的リファレンスです。
