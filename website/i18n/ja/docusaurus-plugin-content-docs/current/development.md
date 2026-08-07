---
sidebar_position: 2
description: Android Camera Parametersアプリのテクニカルアーキテクチャ。MVVMパターンの詳細、Jetpack Compose UI構造、および開発ガイドラインが含まれます。
keywords: [android開発, mvvm, jetpack compose, camera2 api チュートリアル]
---

# 開発者ドキュメント

このドキュメントでは、**Android Camera Parameters**アプリケーションの技術的な概要、アーキテクチャ、および開発ガイドラインを提供します。

## プロジェクトの概要

このアプリケーションは、Android Camera2の`CameraCharacteristics`を検査するための診断ツールです。デバイス上のすべてのカメラレンズのハードウェアレベル、機能、および生のパラメータ値を探索するための、モダンでユーザーフレンドリーなインターフェースを提供します。

## アーキテクチャ

プロジェクトは**MVVM (Model-View-ViewModel)**アーキテクチャパターンに従い、UIレイヤーには**Jetpack Compose**を使用して構築されています。

### 主要コンポーネント

#### `CameraParamsActivity`
アプリケーションの唯一のエントリポイントです。
- 実行時の権限（CAMERA）を処理します。
- `setContent`を介してCompose UIを初期化します。
- `CameraParamsTheme`をホストします。

#### `CameraViewModel`
UIの中心的な状態マネージャーです。
- カメラのリスト、選択されたインデックス、カテゴリ分けされたパラメータ、検索クエリを含む`UiState`を保持します。
- **機能検出**: `detectFeatureFlags()`に、RAWサポート、OIS、マニュアル露出などのハードウェア機能を動的に判断するロジックが含まれています。
- **カテゴリ分け**: 読みやすさを高めるために、何百ものCamera2キーを論理的なセクション（センサー、レンズなど）にグループ化します。

#### `CameraParamsHelper`
Androidの`CameraManager`をラップするユーティリティです。
- 特定のIDの`CameraCharacteristics`を取得します。
- 複雑なカメラタイプ（`IntArray`モードを人間が読める文字列に変換するなど）に対して特別なフォーマットを提供します。

## UIレイヤー (Jetpack Compose)

UIは**Material 3**を使用して構築されており、ダークテーマが厳密に適用されています。

### ナビゲーション構造

アプリは`MainScreen.kt`で管理される`androidx.navigation.compose`を使用しています。

| 画面 | 役割 |
| :--- | :--- |
| **[Overview](overview.md)** | サマリーカード、ハードウェアレベル、主要機能チップを表示するハイレベルなダッシュボード。 |
| **Categories** | セクションごとにグループ化された、検索フィルタリング付きの全パラメータの展開可能なリスト。 |
| **Raw (JSON)** | すべてのカメラプロパティの構文ハイライトされたJSON表現。 |
| **Detail** | フォーマットされた値と生データを表示する、単一パラメータのフォーカスビュー。 |

### スタイリング

- **テーマ**: `Theme.kt`で定義されています。
- **カラー**: ハイライトや主要なアクションに使用されるプライマリカラー `#7B61FF` (バイオレット)。
- **サーフェス**: ダーク背景 `#121417` と、カード用の `#1E1F23` バリアント。

## 主要なロジック

### 動的な機能検出

ダッシュボードの「主要機能」チップは静的ではありません。これらは`CameraViewModel.detectFeatureFlags()`で計算されます。

- **RAW**: `REQUEST_AVAILABLE_CAPABILITIES_RAW`を介してチェックされます。
- **OIS**: `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION`に`ON`が含まれている場合に検出されます。
- **Manual Exp**: `CONTROL_AE_MODE_OFF`がサポートされている場合に使用可能です。
- **Manual Focus**: `LENS_INFO_MINIMUM_FOCUS_DISTANCE`が0より大きい場合に有効になります。

## 開発ガイド

### 前提条件
- Android Studio Ladybug（またはそれ以降）。
- Kotlin 2.0+（プロジェクトは新しいCompose Compiler Gradleプラグインを使用しています）。
- 最小SDK: 21 (Android 5.0)。

### 新しいカテゴリの追加
パラメータのグループ化を追加または変更するには、`CameraViewModel.kt`の`getCategoryForKey()`メソッドを更新してください。カメラキー名との文字列一致を使用して、カテゴリを割り当てます。

### テーマの更新
`Color.kt`で色を調整できます。アプリはダークモードで最適に見えるように設計されています。ライトパレットへの変更は慎重にテストする必要があります。
