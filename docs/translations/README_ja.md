# Androidカメラパラメータ (Android Camera Parameters)

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | 日本語 | [한국어](README_ko.md)

Androidカメラパラメータは、開発者や愛好家がデバイスのカメラの深い技術的能力を探索するための強力な診断ツールです。Android Camera2 APIを活用して、デバイス上のすべてのレンズに関する詳細な洞察を提供します。

![アプリの概要](../../website/static/img/camera_params_feature_graph.png)

## 主な機能

*   **詳細な診断**: すべてのレンズ（背面、前面、外部）の `CameraCharacteristics` を検査します。
*   **ハードウェアレベルの検出**: デバイスが `LEGACY`、`LIMITED`、`FULL`、または `LEVEL_3` 機能をサポートしているかどうかを即座に確認できます。
*   **リアルタイムの機能追跡**: 直感的なダッシュボードを通じて、RAWキャプチャ、光学手ブレ補正 (OIS)、手動露出、マニュアルフォーカスなどのサポートを確認します。
*   **カテゴリ別の探索**: センサー、レンズ、AE/AF/AWB、およびプロセッシングカテゴリごとに整理された数百のパラメータ。**検索機能**も内蔵されています。
*   **お気に入り（近日公開）**: 頻繁にチェックするパラメータをブックマークして素早くアクセスできます。
*   **生データの出力**: 完全なカメラプロファイルを構造化された JSON として表示します。
*   **多言語サポート**: 中国語、スペイン語、日本語など、12以上の言語に完全にローカライズされています。

## サポートされている言語

アプリはグローバルなオーディエンスをサポートするようにローカライズされています。
- 🇺🇸 英語 (English)
- 🇨🇳 中国語 (簡体字)
- 🇹🇼/🇭🇰 中国語 (繁体字)
- 🇪🇸 スペイン語 (Spanish)
- 🇧🇷 ポルトガル語 (ブラジル)
- 🇫🇷 フランス語 (French)
- 🇩🇪 ドイツ語 (German)
- 🇷🇺 ロシア語 (Russian)
- 🇮🇳 ヒンディー語 (Hindi)
- 🇮🇩 インドネシア語 (Indonesian)
- 🇯🇵 日本語 (Japanese)
- 🇰🇷 韓国語 (Korean)

## テクノロジースタック

- **言語**: Kotlin
- **UI フレームワーク**: Jetpack Compose
- **デザインシステム**: Material 3
- **アーキテクチャ**: MVVM
- **ライブラリ**:
    - [Camera2 API](https://developer.android.com/training/camera2): コアカメラインタラクション。
    - [Gson](https://github.com/google/gson): 生データ出力用の JSON シリアル化。
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): アプリナビゲーション。

## プロジェクト構造

- `app/`: メインの Android アプリケーションモジュール。
    - `com.aaron.cameraparams.ui`: Compose ベースの UI 画面とコンポーネント。
    - `com.aaron.cameraparams.camera`: CameraManager と対話し、特性を取得するためのロジック。
- `camera_parameters/`: さまざまなデバイス（Pixel 3、Samsung S10+ など）からのカメラパラメータのサンプル JSON ダンプ。
- `docs/`: 追加のドキュメントとスクリーンショット。

## はじめに

### 前提条件

- Android Studio Koala 以降。
- Android SDK 37 (コンパイル/ターゲット)。
- 物理 Android デバイス（推奨）または Camera2 サポートのあるエミュレーター。

### ビルドと実行

1. リポジトリをクローンする:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. Android Studio でプロジェクトを開く。
3. プロジェクトをビルドする:
   ```bash
   ./gradlew assembleDebug
   ```
4. デバイスにインストールして実行する。

## ライセンス

このプロジェクトは **MIT ライセンス**の下でライセンスされています。詳細は [LICENSE](../../LICENSE) ファイルを参照してください。

## サポートまたは連絡先

メール: kangkang365@gmail.com
プロジェクトサイト: [GitHub Pages](https://zoozooll.github.io/AndroidCameraParameters/)
