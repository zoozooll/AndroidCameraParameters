# Android Camera Parameters

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>

[English] | [简体中文](docs/translations/README_zh-CN.md) | [繁體中文](docs/translations/README_zh-TW.md) | [日本語](docs/translations/README_ja.md) | [한국어](docs/translations/README_ko.md)

Android Camera Parameters is a powerful diagnostic tool for developers and enthusiasts to explore the deep technical capabilities of their device's cameras. It leverages the Android Camera2 API to provide detailed insights into every lens on your device.

![App Overview](docs/designer/page1.png)

## Key Features

*   **Detailed Diagnostics**: Inspect `CameraCharacteristics` for all lenses (Rear, Front, External).
*   **Hardware Level Detection**: Instantly see if your device supports `LEGACY`, `LIMITED`, `FULL`, or `LEVEL_3` features.
*   **Real-time Feature Tracking**: Check support for RAW capture, Optical Image Stabilization (OIS), Manual Exposure, Manual Focus, and more.
*   **Categorized Exploration**: Hundreds of parameters organized by Sensor, Lens, AE/AF/AWB, and Processing categories.
*   **Raw Data Export**: View the complete camera profile as a structured JSON.
*   **Multi-language Support**: Fully localized into 12+ languages including Chinese, Spanish, Japanese, and more.

## Supported Languages

The app is localized to support a global audience:
- 🇺🇸 English
- 🇨🇳 Chinese (Simplified)
- 🇹🇼/🇭🇰 Chinese (Traditional)
- 🇪🇸 Spanish
- 🇧🇷 Portuguese (Brazil)
- 🇫🇷 French
- 🇩🇪 German
- 🇷🇺 Russian
- 🇮🇳 Hindi
- 🇮🇩 Indonesian
- 🇯🇵 Japanese
- 🇰🇷 Korean

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3
- **Architecture**: MVVM
- **Libraries**:
    - [Camera2 API](https://developer.android.com/training/camera2): Core camera interaction.
    - [Gson](https://github.com/google/gson): JSON serialization for raw data export.
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): App navigation.

## Project Structure

- `app/`: Main Android application module.
    - `com.aaron.cameraparams.ui`: Compose-based UI screens and components.
    - `com.aaron.cameraparams.camera`: Logic for interacting with the CameraManager and retrieving characteristics.
- `camera_parameters/`: Sample JSON dumps of camera parameters from various devices (Pixel 3, Samsung S10+, etc.).
- `docs/`: Additional documentation and screenshots.

## Getting Started

### Prerequisites

- Android Studio Koala or newer.
- Android SDK 37 (Compile/Target).
- A physical Android device (recommended) or Emulator with Camera2 support.

### Build and Run

1. Clone the repository:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. Open the project in Android Studio.
3. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install and run on your device.

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

## Support or Contact

Email: kangkang365@gmail.com
Project Site: [GitHub Pages](https://zoozooll.github.io/AndroidCameraParameters/)

## Official Google Play Release
The only official Android application for this open-source project is published by [MiniNinjaDiff](https://play.google.com/store/apps/developer?id=MiniNinjaDiff).
Note for verification: The official Google Play developer contact email is minininjalee@gmail.com.
This account is fully authorized and managed by the original repository owner, regardless of the different email address used for GitHub commits.
