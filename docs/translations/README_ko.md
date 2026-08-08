# 안드로이드 카메라 파라미터 (Android Camera Parameters)

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Google Play에서 다운로드' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>
<a href='https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api'><img alt='튜토리얼' src='https://img.shields.io/badge/Tutorials-Mastering%20Camera2-blue?style=for-the-badge&logo=gitbook' height="45"/></a>

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | [Español](README_es.md) | [Português](README_pt-BR.md) | [Français](README_fr.md) | [Deutsch](README_de.md) | [Русский](README_ru.md) | [हिन्दी](README_hi.md) | [Bahasa Indonesia](README_in.md) | [日本語](README_ja.md) | [한국어]

안드로이드 카메라 파라미터는 개발자와 매니아가 기기 카메라의 심층적인 기술적 기능을 탐색할 수 있는 강력한 진단 도구입니다. 안드로이드 Camera2 API를 활용하여 기기의 모든 렌즈에 대한 상세한 정보를 제공합니다.

![앱 개요](../../website/static/img/camera_params_feature_graph.png)

## 주요 기능

*   **상세 진단**: 모든 렌즈(후면, 전면, 외부)에 대한 `CameraCharacteristics`를 검사합니다.
*   **하드웨어 레벨 감지**: 기기가 `LEGACY`, `LIMITED`, `FULL` 또는 `LEVEL_3` 기능을 지원하는지 즉시 확인합니다.
*   **실시간 기능 추적**: 직관적인 대시보드를 통해 RAW 캡처, 광학 이미지 안정화(OIS), 수동 노출, 수동 초점 등의 지원 여부를 확인합니다.
*   **카테고리별 탐색**: 센서, 렌즈, AE/AF/AWB 및 프로세싱 카테고리별로 정리된 수백 개의 파라미터를 **기본 제공 검색** 기능과 함께 제공합니다.
*   **즐겨찾기 (출시 예정)**: 자주 확인하는 파라미터를 북마크하여 빠르게 액세스합니다.
*   **원시 데이터 내보내기**: 전체 카메라 프로필을 구조화된 JSON으로 확인합니다.
*   **다국어 지원**: 중국어, 스페인어, 일본어 등을 포함한 12개 이상의 언어로 완벽하게 번역되었습니다.

## 지원 언어

이 앱은 글로벌 사용자를 지원하기 위해 현지화되었습니다:
- 🇺🇸 영어 (English)
- 🇨🇳 중국어 (간체)
- 🇹🇼/🇭🇰 중국어 (번체)
- 🇪🇸 스페인어 (Spanish)
- 🇧🇷 포르투갈어 (브라질)
- 🇫🇷 프랑스어 (French)
- 🇩🇪 독일어 (German)
- 🇷🇺 러시아어 (Russian)
- 🇮🇳 힌디어 (Hindi)
- 🇮🇩 인도네시아어 (Indonesian)
- 🇯🇵 일본어 (Japanese)
- 🇰🇷 한국어 (Korean)

## 기술 스택

- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **디자인 시스템**: Material 3
- **아키텍처**: MVVM
- **라이브러리**:
    - [Camera2 API](https://developer.android.com/training/camera2): 핵심 카메라 상호작용.
    - [Gson](https://github.com/google/gson): 원시 데이터 내보내기를 위한 JSON 직렬화.
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): 앱 내비게이션.

## 프로젝트 구조

- `app/`: 메인 안드로이드 애플리케이션 모듈.
    - `com.aaron.cameraparams.ui`: Compose 기반 UI 화면 및 구성 요소.
    - `com.aaron.cameraparams.camera`: CameraManager와 상호작용하고 특성을 검색하는 로직.
- `camera_parameters/`: 다양한 기기(Pixel 3, Samsung S10+ 등)의 카메라 파라미터 샘플 JSON 덤프.
- `docs/`: 추가 문서 및 스크린샷.

## 시작하기

### 사전 요구 사항

- Android Studio Koala 이상.
- Android SDK 37 (Compile/Target).
- 물리적 안드로이드 기기(권장) 또는 Camera2 지원 에뮬레이터.

### 빌드 및 실행

1. 저장소 클론:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. 안드로이드 스튜디오에서 프로젝트 열기.
3. 프로젝트 빌드:
   ```bash
   ./gradlew assembleDebug
   ```
4. 기기에 설치 및 실행.

## 라이선스

이 프로젝트는 **MIT 라이선스**에 따라 라이선스가 부여됩니다. 자세한 내용은 [LICENSE](../../LICENSE) 파일을 참조하세요.

## 지원 또는 문의

이메일: kangkang365@gmail.com
프로젝트 사이트: [안드로이드 카메라 파라미터 문서](https://zoozooll.github.io/AndroidCameraParameters/)
