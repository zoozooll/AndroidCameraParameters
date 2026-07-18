---
sidebar_position: 2
---

# Developer Documentation

This document provides a technical overview of the **Android Camera Parameters** application, its architecture, and development guidelines.

## Project Overview

The application is a diagnostic tool for inspecting Android Camera2 `CameraCharacteristics`. It provides a modern, user-friendly interface to explore hardware levels, capabilities, and raw parameter values for all camera lenses on a device.

## Architecture

The project follows the **MVVM (Model-View-ViewModel)** architectural pattern and is built using **Jetpack Compose** for the UI layer.

### Core Components

#### `CameraParamsActivity`
The single entry point of the application.
- Handles runtime permissions (CAMERA).
- Initializes the Compose UI via `setContent`.
- Hosts the `CameraParamsTheme`.

#### `CameraViewModel`
The central state manager for the UI.
- Maintains `UiState` which includes the list of cameras, selected index, categorized parameters, and search query.
- **Feature Detection**: Contains logic in `detectFeatureFlags()` to dynamically determine hardware capabilities like RAW support, OIS, and Manual Exposure.
- **Categorization**: Groups hundreds of Camera2 keys into logical sections (Sensor, Lens, etc.) for better readability.

#### `CameraParamsHelper`
A utility wrapper around the Android `CameraManager`.
- Retrieves `CameraCharacteristics` for specific IDs.
- Provides specialized formatting for complex camera types (e.g., converting `IntArray` modes into human-readable strings).

## UI Layer (Jetpack Compose)

The UI is built using **Material 3** with a strictly enforced dark theme.

### Navigation Structure

The app uses `androidx.navigation.compose` managed in `MainScreen.kt`.

| Screen | Responsibility |
| :--- | :--- |
| **[Overview](overview.md)** | High-level dashboard showing Summary Card, Hardware Level, and Key Feature chips. |
| **Categories** | Expandable list of all parameters grouped by section with search filtering. |
| **Raw (JSON)** | Syntax-highlighted JSON representation of all camera properties. |
| **Detail** | Focused view for a single parameter, showing formatted value and raw data. |

### Styling

- **Theme**: Defined in `Theme.kt`.
- **Colors**: Primary color `#7B61FF` (Violet) used for highlights and primary actions.
- **Surface**: Dark background `#121417` with `#1E1F23` variants for cards.

## Key Logic

### Dynamic Feature Detection

The "Key Features" chips on the dashboard are not static. They are computed in `CameraViewModel.detectFeatureFlags()`:

- **RAW**: Checked via `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS**: Detected if `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` contains `ON`.
- **Manual Exp**: Available if `CONTROL_AE_MODE_OFF` is supported.
- **Manual Focus**: Enabled if `LENS_INFO_MINIMUM_FOCUS_DISTANCE` is greater than 0.

## Development Guide

### Prerequisites
- Android Studio Ladybug (or newer).
- Kotlin 2.0+ (The project uses the new Compose Compiler Gradle plugin).
- Minimum SDK: 21 (Android 5.0).

### Adding a New Category
To add or modify parameter grouping, update the `getCategoryForKey()` method in `CameraViewModel.kt`. It uses string matching on the camera key names to assign them to categories.

### Updating the Theme
Colors can be adjusted in `Color.kt`. The app is designed to look best in dark mode; any changes to the light palette should be tested carefully.
