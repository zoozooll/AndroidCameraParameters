---
sidebar_position: 2
description: Technische Architektur der Android Camera Parameters App, einschließlich Details zum MVVM-Muster, Jetpack Compose UI-Struktur und Entwicklungsrichtlinien.
keywords: [android entwicklung, mvvm, jetpack compose, camera2 api tutorial]
---

# Entwicklerdokumentation

Dieses Dokument bietet einen technischen Überblick über die Anwendung **Android Camera Parameters**, ihre Architektur und Entwicklungsrichtlinien.

## Projektübersicht

Die Anwendung ist ein Diagnosetool zur Überprüfung der `CameraCharacteristics` von Android Camera2. Sie bietet eine moderne, benutzerfreundliche Oberfläche zur Erkundung von Hardware-Levels, Funktionen und Rohparameterwerten für alle Kameraobjektive auf einem Gerät.

## Architektur

Das Projekt folgt dem Architekturmuster **MVVM (Model-View-ViewModel)** und wurde mit **Jetpack Compose** für die UI-Ebene erstellt.

### Kernkomponenten

#### `CameraParamsActivity`
Der einzige Einstiegspunkt der Anwendung.
- Verarbeitet Laufzeitberechtigungen (CAMERA).
- Initialisiert die Compose-UI über `setContent`.
- Hostet das `CameraParamsTheme`.

#### `CameraViewModel`
Der zentrale Zustandsmanager für die UI.
- Verwaltet den `UiState`, der die Liste der Kameras, den ausgewählten Index, kategorisierte Parameter und die Suchanfrage enthält.
- **Funktionserkennung**: Enthält Logik in `detectFeatureFlags()`, um Hardwarefunktionen wie RAW-Unterstützung, OIS und manuelle Belichtung dynamisch zu bestimmen.
- **Kategorisierung**: Gruppiert Hunderte von Camera2-Keys in logische Abschnitte (Sensor, Objektiv usw.) für eine bessere Lesbarkeit.

#### `CameraParamsHelper`
Ein Utility-Wrapper um den Android `CameraManager`.
- Ruft `CameraCharacteristics` für bestimmte IDs ab.
- Bietet spezialisierte Formatierung für komplexe Kameratypen (z. B. Konvertierung von `IntArray`-Modi in menschenlesbare Strings).

## UI-Ebene (Jetpack Compose)

Die Benutzeroberfläche wurde mit **Material 3** und einem strikt angewendeten dunklen Theme erstellt.

### Navigationsstruktur

Die App verwendet `androidx.navigation.compose`, verwaltet in `MainScreen.kt`.

| Bildschirm | Aufgabe |
| :--- | :--- |
| **[Übersicht](overview.md)** | High-Level-Dashboard mit Zusammenfassungskarte, Hardware-Level und Chips für Hauptfunktionen. |
| **Kategorien** | Aufklappbare Liste aller nach Abschnitten gruppierten Parameter mit Suchfilterung. |
| **Rohdaten (JSON)** | Syntax-hervorgehobene JSON-Darstellung aller Kameraeigenschaften. |
| **Detail** | Fokussierte Ansicht für einen einzelnen Parameter, die den formatierten Wert und die Rohdaten anzeigt. |

### Styling

- **Theme**: Definiert in `Theme.kt`.
- **Farben**: Primärfarbe `#7B61FF` (Violett) für Highlights und primäre Aktionen.
- **Oberfläche**: Dunkler Hintergrund `#121417` mit `#1E1F23`-Varianten für Karten.

## Kernlogik

### Dynamische Funktionserkennung

Die Chips für die "Hauptfunktionen" auf dem Dashboard sind nicht statisch. Sie werden in `CameraViewModel.detectFeatureFlags()` berechnet:

- **RAW**: Geprüft über `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS**: Erkannt, wenn `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` den Wert `ON` enthält.
- **Manuelle Belichtung**: Verfügbar, wenn `CONTROL_AE_MODE_OFF` unterstützt wird.
- **Manueller Fokus**: Aktiviert, wenn `LENS_INFO_MINIMUM_FOCUS_DISTANCE` größer als 0 ist.

## Entwicklungsleitfaden

### Voraussetzungen
- Android Studio Ladybug (oder neuer).
- Kotlin 2.0+ (Das Projekt verwendet das neue Compose Compiler Gradle-Plugin).
- Minimum-SDK: 21 (Android 5.0).

### Eine neue Kategorie hinzufügen
Um die Parametergruppierung hinzuzufügen oder zu ändern, aktualisieren Sie die Methode `getCategoryForKey()` in `CameraViewModel.kt`. Sie verwendet String-Matching für die Namen der Kameratasten, um sie Kategorien zuzuweisen.

### Das Theme aktualisieren
Farben können in `Color.kt` angepasst werden. Die App ist so konzipiert, dass sie im dunklen Modus am besten aussieht; Änderungen an der hellen Palette sollten sorgfältig getestet werden.
