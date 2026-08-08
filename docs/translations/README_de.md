# Android Kamera-Parameter (Android Camera Parameters)

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Jetzt bei Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>
<a href='https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api'><img alt='Tutorials' src='https://img.shields.io/badge/Tutorials-Mastering%20Camera2-blue?style=for-the-badge&logo=gitbook' height="45"/></a>

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | [Español](README_es.md) | [Português](README_pt-BR.md) | [Français](README_fr.md) | [Deutsch] | [Русский](README_ru.md) | [हिन्दी](README_hi.md) | [Bahasa Indonesia](README_in.md) | [日本語](README_ja.md) | [한국어](README_ko.md)

Android Camera Parameters ist ein leistungsstarkes Diagnosetool für Entwickler und Enthusiasten, um die tiefen technischen Möglichkeiten der Kameras ihrer Geräte zu erkunden. Es nutzt die Android Camera2 API, um detaillierte Einblicke in jedes Objektiv Ihres Geräts zu geben.

![App-Übersicht](../../website/static/img/camera_params_feature_graph.png)

## Hauptmerkmale

*   **Detaillierte Diagnose**: Überprüfen Sie `CameraCharacteristics` für alle Objektive (Rückseite, Vorderseite, Extern).
*   **Hardware-Level-Erkennung**: Sehen Sie sofort, ob Ihr Gerät `LEGACY`-, `LIMITED`-, `FULL`- oder `LEVEL_3`-Funktionen unterstützt.
*   **Echtzeit-Feature-Tracking**: Überprüfen Sie die Unterstützung für RAW-Aufnahme, optische Bildstabilisierung (OIS), manuelle Belichtung, manuellen Fokus und mehr über ein intuitives Dashboard.
*   **Kategorisierte Erkundung**: Hunderte von Parametern, organisiert nach den Kategorien Sensor, Objektiv, AE/AF/AWB und Verarbeitung mit **integrierter Suchfunktion**.
*   **Favoriten (Demnächst)**: Markieren Sie häufig überprüfte Parameter für den schnellen Zugriff.
*   **Rohdaten-Export**: Zeigen Sie das vollständige Kameraprofil als strukturiertes JSON an.
*   **Mehrsprachige Unterstützung**: Vollständig lokalisiert in über 12 Sprachen, darunter Chinesisch, Spanisch, Japanisch und mehr.

## Unterstützte Sprachen

Die App ist lokalisiert, um ein globales Publikum zu unterstützen:
- 🇺🇸 Englisch
- 🇨🇳 Chinesisch (Vereinfacht)
- 🇹🇼/🇭🇰 Chinesisch (Traditionell)
- 🇪🇸 Spanisch
- 🇧🇷 Portugiesisch (Brasilien)
- 🇫🇷 Französisch
- 🇩🇪 Deutsch
- 🇷🇺 Russisch
- 🇮🇳 Hindi
- 🇮🇩 Indonesisch
- 🇯🇵 Japanisch
- 🇰🇷 Koreanisch

## Tech-Stack

- **Sprache**: Kotlin
- **UI-Framework**: Jetpack Compose
- **Design-System**: Material 3
- **Architektur**: MVVM
- **Bibliotheken**:
    - [Camera2 API](https://developer.android.com/training/camera2): Kern-Kamerainteraktion.
    - [Gson](https://github.com/google/gson): JSON-Serialisierung für den Rohdaten-Export.
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): App-Navigation.

## Projektstruktur

- `app/`: Hauptmodul der Android-Anwendung.
    - `com.aaron.cameraparams.ui`: Compose-basierte UI-Bildschirme und Komponenten.
    - `com.aaron.cameraparams.camera`: Logik für die Interaktion mit dem CameraManager und das Abrufen von Merkmalen.
- `camera_parameters/`: Beispiel-JSON-Dumps von Kameraparametern verschiedener Geräte (Pixel 3, Samsung S10+ usw.).
- `docs/`: Zusätzliche Dokumentation und Screenshots.

## Erste Schritte

### Voraussetzungen

- Android Studio Koala oder neuer.
- Android SDK 37 (Compile/Target).
- Ein physisches Android-Gerät (empfohlen) oder Emulator mit Camera2-Unterstützung.

### Erstellen und Ausführen

1. Klonen Sie das Repository:
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. Öffnen Sie das Projekt in Android Studio.
3. Erstellen Sie das Projekt:
   ```bash
   ./gradlew assembleDebug
   ```
4. Installieren und auf Ihrem Gerät ausführen.

## Lizenz

Dieses Projekt ist unter der **MIT-Lizenz** lizenziert. Siehe die Datei [LICENSE](../../LICENSE) für Details.

## Support oder Kontakt

E-Mail: kangkang365@gmail.com
Projektseite: [Android Kamera-Parameter Dokumentation](https://zoozooll.github.io/AndroidCameraParameters/)
