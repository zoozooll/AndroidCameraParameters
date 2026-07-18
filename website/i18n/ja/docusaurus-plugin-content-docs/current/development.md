---
sidebar_position: 2
---

# 開発者ドキュメント

このドキュメントでは、**Android カメラパラメータ (Android Camera Parameters)** アプリケーションの技術的な概要、そのアーキテクチャ、および開発ガイドラインについて説明します。

## プロジェクトの概要

このアプリケーションは、Android Camera2 `CameraCharacteristics` を検査するための診断ツールです。デバイス上のすべてのカメラレンズのハードウェアレベル、機能、および生のパラメータ値を探索するための、モダンでユーザーフレンドリーなインターフェースを提供します。

## アーキテクチャ

このプロジェクトは **MVVM (Model-View-ViewModel)** アーキテクチャパターンに従っており、UI レイヤーには **Jetpack Compose** を使用して構築されています。

### コアコンポーネント

#### `CameraParamsActivity`
アプリケーションの唯一のエントリポイント。
- 実行時の権限 (CAMERA) を処理します。
- `setContent` を介して Compose UI を初期化します。
- `CameraParamsTheme` をホストします。

#### `CameraViewModel`
UI の中央状態マネージャー。
- カメラリスト、選択されたインデックス、カテゴリ別のパラメータ、および検索クエリを含む `UiState` を維持します。
- **機能検出**: `detectFeatureFlags()` にロジックを含み、RAW サポート、OIS、手動露出などのハードウェア機能を動的に決定します。
- **カテゴリ分け**: 読みやすさを向上させるために、数百の Camera2 キーを論理的なセクション (センサー、レンズなど) にグループ化します。

#### `CameraParamsHelper`
Android `CameraManager` のユーティリティラッパー。
- 特定の ID の `CameraCharacteristics` を取得します。
- 複雑なカメラタイプに対して、専用のフォーマットを提供します (例: `IntArray` モードを人間が読める文字列に変換)。

## UI レイヤー (Jetpack Compose)

UI は **Material 3** を使用して構築されており、ダークテーマが厳密に適用されています。

### ナビゲーション構造

アプリは `MainScreen.kt` で管理される `androidx.navigation.compose` を使用します。

| 画面 | 責任 |
| :--- | :--- |
| **[概要](overview.md)** | サマリーカード、ハードウェアレベル、および主な機能チップを表示するハイレベルなダッシュボード。 |
| **カテゴリ** | セクションごとにグループ化されたすべてのパラメータの展開可能なリスト、検索フィルタリング付き。 |
| **生データ (JSON)** | すべてのカメラプロパティの構文ハイライトされた JSON 表現。 |
| **詳細** | フォーマットされた値と生データを表示する、単一のパラメータに焦点を当てたビュー。 |

### スタイリング

- **テーマ**: `Theme.kt` で定義。
- **色**: プライマリカラー `#7B61FF` (バイオレット) は、ハイライトとプライマリアクションに使用されます。
- **サーフェス**: ダーク背景 `#121417`、カードには `#1E1F23` バリアントを使用。

## 主要なロジック

### 動的な機能検出

ダッシュボードの「主な機能」チップは静的ではありません。これらは `CameraViewModel.detectFeatureFlags()` で計算されます。

- **RAW**: `REQUEST_AVAILABLE_CAPABILITIES_RAW` を介してチェックされます。
- **OIS**: `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` に `ON` が含まれている場合に検出されます。
- **手動露出**: `CONTROL_AE_MODE_OFF` がサポートされている場合に使用可能です。
- **マニュアルフォーカス**: `LENS_INFO_MINIMUM_FOCUS_DISTANCE` が 0 より大きい場合に有効になります。

## 開発ガイド

### 前提条件
- Android Studio Ladybug (またはそれ以降)。
- Kotlin 2.0+ (プロジェクトは新しい Compose Compiler Gradle プラグインを使用しています)。
- 最小 SDK: 21 (Android 5.0)。

### 新しいカテゴリの追加
パラメータのグループ化を追加または変更するには、`CameraViewModel.kt` の `getCategoryForKey()` メソッドを更新してください。カメラキー名に文字列一致を使用して、カテゴリを割り当てます。

### テーマの更新
色は `Color.kt` で調整できます。アプリはダークモードで最適に見えるように設計されています。ライトパレットへの変更は慎重にテストする必要があります。
