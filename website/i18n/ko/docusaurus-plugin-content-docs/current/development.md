---
sidebar_position: 2
---

# 개발자 문서

이 문서는 **Android Camera Parameters** 애플리케이션의 기술적 개요, 아키텍처 및 개발 지침을 제공합니다.

## 프로젝트 개요

이 애플리케이션은 Android Camera2 `CameraCharacteristics`를 검사하기 위한 진단 도구입니다. 기기의 모든 카메라 렌즈에 대한 하드웨어 레벨, 기능 및 원시 파라미터 값을 탐색할 수 있는 현대적이고 사용자 친화적인 인터페이스를 제공합니다.

## 아키텍처

이 프로젝트는 **MVVM (Model-View-ViewModel)** 아키텍처 패턴을 따르며 UI 레이어에는 **Jetpack Compose**를 사용하여 구축되었습니다.

### 핵심 구성 요소

#### `CameraParamsActivity`
애플리케이션의 단일 진입점입니다.
- 런타임 권한(CAMERA)을 처리합니다.
- `setContent`를 통해 Compose UI를 초기화합니다.
- `CameraParamsTheme`을 호스트합니다.

#### `CameraViewModel`
UI를 위한 중앙 상태 관리자입니다.
- 카메라 목록, 선택된 인덱스, 카테고리별 파라미터 및 검색 쿼리를 포함하는 `UiState`를 유지합니다.
- **기능 감지**: `detectFeatureFlags()`에 RAW 지원, OIS 및 수동 노출과 같은 하드웨어 기능을 동적으로 결정하는 로직이 포함되어 있습니다.
- **카테고리화**: 가독성을 높이기 위해 수백 개의 Camera2 키를 논리적 섹션(센서, 렌즈 등)으로 그룹화합니다.

#### `CameraParamsHelper`
Android `CameraManager`를 감싸는 유틸리티 래퍼입니다.
- 특정 ID에 대한 `CameraCharacteristics`를 가져옵니다.
- 복잡한 카메라 유형에 대해 전문화된 포맷을 제공합니다(예: `IntArray` 모드를 읽기 쉬운 문자열로 변환).

## UI 레이어 (Jetpack Compose)

UI는 다크 테마가 엄격하게 적용된 **Material 3**를 사용하여 구축되었습니다.

### 탐색 구조

앱은 `MainScreen.kt`에서 관리되는 `androidx.navigation.compose`를 사용합니다.

| 화면 | 책임 |
| :--- | :--- |
| **[개요](overview.md)** | 요약 카드, 하드웨어 레벨 및 주요 기능 칩을 보여주는 상위 수준 대시보드입니다. |
| **카테고리** | 검색 필터링 기능이 있는 섹션별로 그룹화된 모든 파라미터의 확장 가능한 목록입니다. |
| **원시 데이터 (JSON)** | 모든 카메라 속성의 구문 강조가 적용된 JSON 표현입니다. |
| **세부 정보** | 단일 파라미터에 집중된 보기로, 포맷된 값과 원시 데이터를 보여줍니다. |

### 스타일링

- **테마**: `Theme.kt`에 정의되어 있습니다.
- **색상**: 하이라이트 및 기본 액션에 사용되는 기본 색상 `#7B61FF`(보라색)입니다.
- **표면**: 다크 배경 `#121417`이며 카드에는 `#1E1F23` 변형이 사용됩니다.

## 주요 로직

### 동적 기능 감지

대시보드의 "주요 기능" 칩은 정적이지 않습니다. `CameraViewModel.detectFeatureFlags()`에서 계산됩니다.

- **RAW**: `REQUEST_AVAILABLE_CAPABILITIES_RAW`를 통해 확인됩니다.
- **OIS**: `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION`에 `ON`이 포함되어 있으면 감지됩니다.
- **수동 노출**: `CONTROL_AE_MODE_OFF`가 지원되는 경우 사용 가능합니다.
- **수동 초점**: `LENS_INFO_MINIMUM_FOCUS_DISTANCE`가 0보다 크면 활성화됩니다.

## 개발 가이드

### 필수 조건
- Android Studio Ladybug (또는 최신 버전).
- Kotlin 2.0+ (이 프로젝트는 새로운 Compose Compiler Gradle 플러그인을 사용합니다).
- 최소 SDK: 21 (Android 5.0).

### 새로운 카테고리 추가
파라미터 그룹화를 추가하거나 수정하려면 `CameraViewModel.kt`의 `getCategoryForKey()` 메서드를 업데이트하세요. 카메라 키 이름에 문자열 일치를 사용하여 카테고리에 할당합니다.

### 테마 업데이트
색상은 `Color.kt`에서 조정할 수 있습니다. 이 앱은 다크 모드에서 가장 잘 보이도록 설계되었으므로 라이트 팔레트에 대한 변경 사항은 신중하게 테스트해야 합니다.
