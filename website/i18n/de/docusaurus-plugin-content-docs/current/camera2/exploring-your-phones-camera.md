---
sidebar_position: 4
title: "Kapitel 4: Die Kamera deines Telefons erkunden"
description: Verwende die Android Camera Parameters App, um die Kamerafähigkeiten deines Geräts zu erkunden, einschließlich Kamera-IDs, Auflösungen, FPS, Hardware-Level, RAW-Unterstützung und mehr.
keywords: [Kameraparameter, Kamera-IDs, Hardware-Level, RAW-Unterstützung, FPS, Auflösung]
---

Hier wird das Lernen interaktiv. Lass uns die Kamera deines Telefons erkunden!

## Einleitung

Über Kameras zu lesen ist hilfreich, aber nichts geht über echte Daten von deinem eigenen Gerät. Hier kommt Android Camera Parameters ins Spiel.

In diesem Kapitel geht es um **Erkundung**. Noch kein Programmieren. Nur Neugier.

## Android Camera Parameters installieren

Wenn du es noch nicht getan hast, installiere die Android Camera Parameters App auf deinem Android-Gerät:

1. Öffne den Google Play Store
2. Suche nach „Android Camera Parameters"
3. Installiere die App
4. Öffne sie und erteile Kameraberechtigungen

## Die Benutzeroberfläche verstehen

Wenn du die App öffnest, siehst du mehrere Bereiche:

### Übersichtsbildschirm
Zeigt grundlegende Kamerainformationen auf einen Blick:
- Kamera-IDs
- Objektivausrichtung
- Hardware-Level
- Sensorgröße
- Verfügbare Fähigkeiten

### Kategorien-Bildschirm
Organisiert Kameraparameter in logische Gruppen:
- Kamera-Info
- Sensor
- Objektiv
- Steuerung
- Skalierer
- Blitz
- Und mehr

### RAW JSON-Bildschirm
Zeigt die vollständigen CameraCharacteristics im rohen JSON-Format für fortgeschrittene Benutzer an.

## Lass uns erkunden

Lass uns die wichtigsten Informationen durchgehen, nach denen du suchen solltest.

### Kamera-IDs

Android weist jeder Kamera eine eindeutige ID-Nummer zu. Suche nach:
- **Kamera 0** — Normalerweise die hintere Weitwinkelkamera
- **Kamera 1** — Könnte die Frontkamera oder eine weitere Rückkamera sein
- **Kamera 2** — Oft die Ultraweitwinkel- oder Telekamera
- **Kamera 3+** — Zusätzliche Kameras (Makro, Tiefe, etc.)

Jede ID repräsentiert ein separates Kameragerät mit eigenen Eigenschaften.

### Hardware-Level

Dies ist eine der wichtigsten Informationen:

| Level | Beschreibung |
| --- | --- |
| **LEGACY** | Alte Geräte, begrenzte Camera2-Unterstützung |
| **LIMITED** | Grundlegende Camera2-Funktionen |
| **FULL** | Volle manuelle Steuerung, RAW-Unterstützung |
| **LEVEL_3** | Erweiterte Funktionen wie YUV-Nachbearbeitung |

Prüfe, welchen Hardware-Level dein Telefon unterstützt. Dies bestimmt, welche Camera2-Funktionen verfügbar sind.

### Sensor-Informationen

Suche nach:
- **Sensorgröße** — Physische Abmessungen des Sensors
- **Aktive Array-Größe** — Der tatsächliche Bereich, der für die Bildaufnahme verwendet wird
- **Pixel-Array-Größe** — Gesamtpixel auf dem Sensor
- **Maximale Framedauer** — Mindestzeit zwischen Frames
- **Ausgabeformate** — JPEG, RAW, YUV, etc.

### Auflösungsoptionen

Kameras unterstützen mehrere Auflösungen. Prüfe:
- **Vorschaugrößen** — Auflösungen für die Anzeige
- **Fotogrößen** — Auflösungen für die Fotoaufnahme
- **Videogrößen** — Auflösungen für die Videoaufzeichnung

Beachte die unterschiedlichen Seitenverhältnisse: 4:3, 16:9, 1:1.

### Bildrate (FPS)

Suche nach:
- **Vorschau-FPS-Bereich** — Bilder pro Sekunde für die Vorschau
- **Aufnahme-FPS-Bereich** — Bilder pro Sekunde für die Standbildaufnahme
- **Hochgeschwindigkeitsvideo** — Spezielle Modi mit hoher Bildrate

Höhere FPS bedeuten flüssigeres Video und reaktionsschnelleren Autofokus.

### RAW-Unterstützung

Prüfe, ob deine Kamera RAW-Aufnahme unterstützt:
- **RAW-Formate** — RAW_SENSOR, RAW10, RAW12, RAW16
- **RAW-Größen** — Verfügbare Auflösungen für die RAW-Aufnahme

RAW-Unterstützung erfordert mindestens FULL Hardware-Level.

### Blitzfähigkeiten

Suche nach:
- **Blitzmodus** — OFF, ON, AUTO, TORCH
- **Verfügbare Modi** — Welche Blitzfunktionen unterstützt werden
- **Blitz-Info** — Blitzstärke und Fähigkeiten

### Zoom

Prüfe:
- **Maximaler digitaler Zoom** — Wie stark du digital zoomen kannst
- **Verfügbare Brennweiten** — Verschiedene Objektive und ihre Brennweiten
- **Sanfte Zoom-Unterstützung** — Ob der Zoom sanft angepasst werden kann

### Fokusmodi

Suche nach:
- **Verfügbare Fokusmodi** — AUTO, FIXED, MACRO, CONTINUOUS_VIDEO, CONTINUOUS_PICTURE, EDGE
- **Fokusabstandsbereich** — Mindest- und Höchstabstand
- **AF-Regionen** — Anzahl der unterstützten Autofokus-Regionen

### Belichtungssteuerung

Prüfe:
- **AE-Modi** — AUTO, ON, OFF
- **Verfügbare AE-Modi** — Welche Belichtungsmodi unterstützt werden
- **Belichtungsbereich** — Mindest- und Höchstbelichtungszeiten
- **ISO-Bereich** — Unterstützte ISO-Werte

## Jetzt bist du dran

Jetzt bist du dran zu erkunden. Beantworte diese Fragen über dein Telefon:

1. Wie viele Kameras hat dein Telefon?
2. Welchen Hardware-Level unterstützen sie?
3. Welche Kamera unterstützt RAW?
4. Was ist die höchste verfügbare Auflösung?
5. Unterstützt eine Kamera 4K-Video?
6. Was ist die maximale Zoomstufe?
7. Hat dein Telefon eine Tele- oder Ultraweitwinkelkamera?

## Warum das wichtig ist

Du fragst dich vielleicht, warum wir vor dem Programmieren erkunden. Hier ist der Grund:

1. **Jedes Telefon ist anders** — Was auf einem Gerät funktioniert, funktioniert möglicherweise nicht auf einem anderen
2. **Camera2 erfordert Anpassung** — Gute Camera2-Apps fragen Fähigkeiten ab, anstatt sie anzunehmen
3. **Verstehen baut Intuition auf** — Wenn du echte Daten siehst, werden abstrakte Konzepte konkret

Wenn wir anfangen zu programmieren, weißt du bereits, was du von deinem Gerät erwarten kannst.

## Vergleich mit Freunden

Wenn du Freunde mit anderen Telefonen hast, vergleiche deine Ergebnisse:
- Hat das Flaggschiff-Telefon einen besseren Hardware-Level?
- Fehlt bei Budget-Telefonen die RAW-Unterstützung?
- Wie unterscheidet sich die Anzahl der Kameras?

Dies hilft dir, das Android-Kamera-Ökosystem zu verstehen.

## Häufige Entdeckungen

Hier sind einige häufige Dinge, die Leute entdecken:

- **Flaggschiff-Telefone** haben oft FULL oder LEVEL_3 Hardware-Level
- **Budget-Telefone** haben oft LIMITED oder LEGACY Hardware-Level
- **Die meisten Telefone** unterstützen JPEG-Aufnahme
- **RAW-Unterstützung** ist noch nicht universell
- **Mehrere Kameras** sind bei modernen Telefonen Standard
- **Frontkameras** haben normalerweise eine niedrigere Auflösung als Rückkameras

## Nächstes Kapitel

Nachdem du die Kamera deines Telefons erkundet hast, bist du bereit, mit dem Programmieren zu beginnen! Im nächsten Kapitel stellen wir die erste Camera2-Klasse vor: **CameraManager**.

CameraManager ist der Einstiegspunkt zur Camera2 API. Sie ermöglicht dir:
- Verfügbare Kameras aufzuzählen
- Kameramerkmale abzurufen
- Kameras zu öffnen

Lass uns beginnen!

## Zusammenfassung

Die Kamera deines Telefons zu erkunden, ist der beste Weg, um zu verstehen, was Camera2 kann. Android Camera Parameters macht dies einfach, indem es alle Kamerafähigkeiten übersichtlich anzeigt.

Wichtige Dinge, nach denen du suchen solltest:
- Kamera-IDs und ihre Rollen
- Hardware-Level (LEGACY, LIMITED, FULL, LEVEL_3)
- Auflösungsoptionen
- RAW-Unterstützung
- Blitz- und Zoomfähigkeiten
- Fokus- und Belichtungssteuerung

Diese praktische Erkundung baut die Grundlage für das Schreiben von Camera2-Anwendungen auf.
