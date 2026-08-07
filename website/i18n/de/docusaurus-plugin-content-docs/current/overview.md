---
sidebar_position: 1
slug: /
description: Übersicht über das Android Camera Parameters Dashboard und seine wichtigsten Diagnosefunktionen, einschließlich Hardware-Level-Erkennung und Echtzeit-Feature-Tracking.
keywords: [Android Kamera Dashboard, Hardware-Level-Erkennung, Kameradiagnose]
---

# App-Übersicht

Diese Seite bietet eine detaillierte Aufschlüsselung des Dashboards und der wichtigsten Funktionen der Anwendung.

![App-Übersicht](/img/camera_params_feature_graph.png)

## Dashboard-Komponenten

### 1. Navigation & Auswahl
- **Menü-Drawer**: Zugriff auf Datenschutzbestimmungen, App bewerten und Informationen über das Menü-Symbol oben links.
- **Kamera-Auswahl**: Tippen Sie auf den Kameranamen oder das ID-Badge (z. B. "0"), um ein Dropdown-Menü zu öffnen und zwischen den verfügbaren Objektiven (Rückseite, Vorderseite, Ultraweitwinkel usw.) zu wechseln.
- **Untere Navigation**: Wechseln Sie nahtlos zwischen **Übersicht**, **Kategorien**, **Roh-JSON** und **Favoriten**.

### 2. Zusammenfassungskarte
Die Zusammenfassungskarte oben liefert die kritischsten Informationen:
- **Hardware-Level**: Die Unterstützungsstufe der Camera2-API (LEGACY, LIMITED, FULL oder LEVEL_3). Dies bestimmt die allgemeinen Fähigkeiten des Objektivs.

### 3. Raster der Hauptmerkmale
Ein visuelles Raster, das den sofortigen Status für professionelle Funktionen liefert:
- **Auflösung & Sensorgröße**: Physikalische Eigenschaften des Sensors.
- **Max. Video-FPS**: Spitzenwerte der Bildrate.
- **RAW-Unterstützung**: Gibt an, ob der Sensor unkomprimierte Daten ausgeben kann.
- **OIS (Optische Bildstabilisierung)**: Verfügbarkeit der physischen Objektivstabilisierung.
- **Manuelle Steuerung**: Status der Unterstützung für manuelle Belichtung und manuellen Fokus.
- **Verarbeitung**: Unterstützung für HDR, Gesichtserkennung und Rote-Augen-Reduzierung.

### 4. Kategorisierte Parameter (Registerkarte Kategorien)
Erkunden Sie die vollständige Liste der CameraCharacteristics, die in logischen Gruppen organisiert sind:
- **Sensor**: Auflösung, physikalische Größe, Empfindlichkeitsbereiche.
- **Objektiv**: Brennweite, Blende, Stabilisierungsmodi.
- **AE/AF/AWB**: Detaillierte Steuerungsmodi für Belichtung, Fokus und Weißabgleich.
- **Suche**: Verwenden Sie die integrierte Suchleiste, um schnell bestimmte API-Schlüssel oder Werte zu finden.
