---
sidebar_position: 2
description: Technische Architektur der Android Camera Parameters App, einschließlich Details zum MVVM-Muster, Jetpack Compose UI-Struktur und Entwicklungsrichtlinien.
keywords: [Android-Entwicklung, MVVM, Jetpack Compose, Camera2 API Tutorial]
---

# Entwicklerdokumentation

Dieses Dokument bietet einen technischen Überblick über die Anwendung **Android Camera Parameters**, ihre Architektur und Entwicklungsrichtlinien.

## Projektübersicht

Die Anwendung ist ein Diagnosetool zur Inspektion von Android Camera2 `CameraCharacteristics`. Sie bietet eine moderne, benutzerfreundliche Oberfläche zum Erkunden von Hardware-Levels, Funktionen und rohen Parameterwerten für alle Kameraobjektive auf einem Gerät.

## Architektur

Das Projekt folgt dem Architekturmuster **MVVM (Model-View-ViewModel)** und wurde mit **Jetpack Compose** für die UI-Schicht erstellt.

### Kernkomponenten

#### `CameraParamsActivity`
Der einzige Einstiegspunkt der Anwendung.
- Handhabt Laufzeitberechtigungen (CAMERA).
- Initialisiert die Compose-UI über `setContent`.
- Beherbergt das `CameraParamsTheme`.

#### `CameraViewModel`
Der zentrale Zustandsmanager für die UI.
- Verwaltet den `UiState`, der die Liste der Kameras, den ausgewählten Index, kategorisierte Parameter und die Suchanfrage enthält.
- **Funktionserkennung**: Enthält Logik in `detectFeatureFlags()`, um Hardwarefunktionen wie RAW-Unterstützung, OIS und manuelle Belichtung dynamisch zu bestimmen.
- **Kategorisierung**: Gruppiert Hunderte von Camera2-Schlüsseln in logische Abschnitte (Sensor, Objektiv usw.) für eine bessere Lesbarkeit.

#### `CameraParamsHelper`
Ein Utility-Wrapper um den Android `CameraManager`.
- Ruft `CameraCharacteristics` für bestimmte IDs ab.
- Bietet spezialisierte Formatierung für komplexe Kameratypen (z. B. Konvertierung von `IntArray`-Modi in menschenlesbare Zeichenfolgen).

## UI-Schicht (Jetpack Compose)

Die UI ist mit **Material 3** und einem streng erzwungenen dunklen Thema aufgebaut.

### Navigationsstruktur

Die App verwendet `androidx.navigation.compose`, verwaltet in `MainScreen.kt`.

| Bildschirm | Verantwortung |
| :--- | :--- |
| **[Übersicht](overview.md)** | High-Level-Dashboard mit Zusammenfassungskarte, Hardware-Level und Chips für Hauptmerkmale. |
| **Kategorien** | Erweiterbare Liste aller Parameter, gruppiert nach Abschnitten mit Suchfilterung. |
| **Roh (JSON)** | Syntaxhervorgehobene JSON-Darstellung aller Kameraeigenschaften. |
| **Details** | Fokusansicht für einen einzelnen Parameter, die den formatierten Wert und die Rohdaten anzeigt. |

### Styling

- **Theme**: Definiert in `Theme.kt`.
- **Farben**: Primärfarbe `#7B61FF` (Violett), verwendet für Highlights und primäre Aktionen.
- **Oberfläche**: Dunkler Hintergrund `#121417` mit `#1E1F23`-Varianten für Karten.

## Wichtige Logik

### Dynamische Funktionserkennung

Die "Hauptmerkmale"-Chips auf dem Dashboard sind nicht statisch. Sie werden in `CameraViewModel.detectFeatureFlags()` berechnet:

- **RAW**: Überprüft über `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS**: Erkannt, wenn `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` den Wert `ON` enthält.
- **Manuelle Belichtung**: Verfügbar, wenn `CONTROL_AE_MODE_OFF` unterstützt wird.
- **Manueller Fokus**: Aktiviert, wenn `LENS_INFO_MINIMUM_FOCUS_DISTANCE` größer als 0 ist.

## Entwicklungsleitfaden

### Voraussetzungen
- Android Studio Ladybug (or neuer).
- Kotlin 2.0+ (Das Projekt verwendet das neue Compose Compiler Gradle-Plugin).
- Mindest-SDK: 21 (Android 5.0).

### Hinzufügen einer neuen Kategorie
Um die Gruppierung von Parametern hinzuzufügen oder zu ändern, aktualisieren Sie die Methode `getCategoryForKey()` in `CameraViewModel.kt`. Sie verwendet String-Matching auf den Namen der Kameraschlüssel, um sie Kategorien zuzuweisen.

### Aktualisieren des Themes
Farben können in `Color.kt` angepasst werden. Die App ist so konzipiert, dass sie im dunklen Modus am besten aussieht; Änderungen an der hellen Palette sollten sorgfältig getestet werden.
