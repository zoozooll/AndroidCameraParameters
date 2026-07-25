---
sidebar_position: 1
title: "Kapitel 1: Willkommen bei Android Camera2"
description: Erfahren Sie, warum Android Camera2 wichtig ist, wie es sich von CameraX unterscheidet und was diese Serie abdecken wird.
keywords: [Android Camera2, CameraX, Kamerafunktionen, Android Kameraentwicklung]
---

Bevor wir die Kamera steuern, müssen wir das Kamerasystem verstehen.

## Einführung

Fast jedes Smartphone verfügt heute über ein leistungsstarkes Kamerasystem. Ein modernes Telefon kann:

- Professionell aussehende Fotos aufnehmen
- 4K- und 8K-Videos aufzeichnen
- Porträteffekte erstellen
- Bei extrem schwachem Licht fotografieren
- Zeitlupenvideos aufnehmen
- Tiefeninformationen generieren
- Mehrere Kameras miteinander kombinieren

Aber wenn Sie die Standard-Kamera-App öffnen, sehen Sie nur eine einfache Benutzeroberfläche: einen Auslöser, einen Zoomregler und ein paar Aufnahmemodi.

Hinter dieser einfachen Schnittstelle verbirgt sich ein überraschend komplexes System. Die Kamera-App kommuniziert mit Hardwarekomponenten, Bildprozessoren und Android-Frameworks, um jedes Bild zu erzeugen.

Als Android-Entwickler möchten wir möglicherweise Anwendungen erstellen, die über die Standard-Kamera-App hinausgehen, wie zum Beispiel:

- Eine manuelle Fotografie-Anwendung
- Ein Kamera-Testwerkzeug
- Eine Computer-Vision-Anwendung
- Eine 3D-Scanning-Anwendung
- Ein professioneller Videorecorder
- Ein Analysator für Kamerafunktionen

Um diese Anwendungen zu erstellen, müssen wir die Android Camera2 API verstehen.

## Was ist Android Camera2?

Android Camera2 ist das moderne Kamera-Framework, das von Google in Android 5.0 (API-Level 21) eingeführt wurde. Es ersetzte die ursprüngliche Android Camera API.

Die alte Camera API wurde für eine einfachere Welt entwickelt: eine Kamera, einfache Fotoaufnahme und einfache Videoaufzeichnung. Smartphone-Kameras haben sich seitdem dramatisch weiterentwickelt. Moderne Geräte können mehrere Rückkameras, Weitwinkelobjektive, Teleobjektive, Tiefensensoren und externe Kameras enthalten.

Sie unterstützen auch erweiterte Funktionen:

- Manuelle Belichtung und Fokus
- RAW-Bildaufnahme
- Hochgeschwindigkeitsvideo
- HDR-Verarbeitung
- Optische Stabilisierung

Camera2 wurde entwickelt, um Entwicklern eine viel tiefere Kontrolle über die Kamerahardware zu geben.

## Camera2 vs. CameraX

Camera2 und CameraX lösen unterschiedliche Probleme.

### CameraX

CameraX ist eine Bibliothek auf höherer Ebene, die entwickelt wurde, um gängige Kameraaufgaben zu erleichtern, einschließlich:

- Anzeigen einer Vorschau
- Aufnehmen von Fotos
- Aufzeichnen von Videos
- Handhabung der Gerätekompatibilität

Die meisten Anwendungen sollten mit CameraX beginnen.

### Camera2

Camera2 ist das Framework auf niedrigerer Ebene. Es gibt Entwicklern direkten Zugriff auf Kamerafunktionen, einschließlich Sensorinformationen, Belichtungseinstellungen, Fokussteuerung, Kamerametadaten, Hardwarefunktionen und RAW-Unterstützung.

Camera2 ist komplexer, bietet aber viel mehr Kontrolle.

| | CameraX | Camera2 |
| --- | --- | --- |
| Ebene | Bibliothek auf hoher Ebene | Low-Level-API |
| Schwierigkeit | Einfacher | Komplexer |
| Kontrolle | Begrenzt | Umfangreich |
| Am besten für | Normale Kamera-Apps | Fortgeschrittene Kameraanwendungen |

Diese Tutorial-Serie konzentriert sich auf Camera2, da das Verständnis hilft zu begreifen, wie Android-Kameras tatsächlich funktionieren.

## Warum Camera2 lernen?

Sie fragen sich vielleicht: *Warum sollte ich Camera2 lernen, wenn es bereits CameraX gibt?*

### Verstehen, was das Gerät wirklich kann

Jedes Android-Handy ist anders. Ein Gerät unterstützt möglicherweise RAW-Aufnahmen, 4K-Videos mit 60 fps und manuelle Steuerungen; ein anderes vielleicht nicht. Camera2 ermöglicht es Anwendungen, diese Fähigkeiten zu entdecken.

### Professionelle Kameraanwendungen erstellen

Anwendungen, die fortgeschrittene Kamerafunktionen benötigen, brauchen normalerweise Camera2. Beispiele hierfür sind professionelle Kamera-Apps, wissenschaftliche Bildgebungs-Apps, AR-Anwendungen, Computer-Vision-Systeme und Videoproduktionswerkzeuge.

### Smartphone-Fotografie verstehen

Viele moderne Kamerafunktionen basieren auf Konzepten, die über Camera2 zugänglich gemacht werden:

- Belichtung
- ISO
- Fokus
- Weißabgleich
- HDR
- Mehrere Kameras

Das Erlernen von Camera2 lehrt Sie auch, wie Smartphone-Kameras funktionieren.

## Was werden Sie in dieser Serie lernen?

Diese Serie ist darauf ausgelegt, Sie vom Anfänger zum Fortgeschrittenen zu führen.

### Teil 1: Kameras verstehen

Sie erfahren, wie Smartphone-Kameras funktionieren, was die Kamerahardware enthält, wie Android Kameras darstellt und wie Sie Ihr eigenes Gerät untersuchen.

### Teil 2: Ihre erste Camera2-Anwendung

Sie lernen, wie Sie Kameras finden und öffnen, eine Vorschau erstellen und Bilder aufnehmen.

### Teil 3: Kamerasteuerungen

Sie lernen etwas über Belichtung, ISO, Fokus, Weißabgleich, Blitz und Zoom.

### Teil 4: Erweiterte Kamerafunktionen

Sie lernen etwas über RAW-Aufnahme, Hochgeschwindigkeitsvideo, Multi-Kamera-Geräte, logische und physische Kameras, Kameraerweiterungen und HDR-Funktionen.

### Teil 5: Tiefer Einblick in Kamerametadaten

Sie werden wichtige Camera2-Parameter untersuchen, einschließlich:

- `android.sensor.info.activeArraySize`
- `android.scaler.availableMaxDigitalZoom`
- `android.control.aeAvailableModes`
- `android.request.availableCapabilities`

Sie werden nicht nur verstehen, was diese Parameter bedeuten, sondern auch, warum sie existieren.

## Lernen mit Android Camera Parameters

Das Lesen der Dokumentation ist nützlich, aber Kamerafunktionen sind einfacher zu verstehen, wenn Sie echte Daten von einem echten Telefon sehen können. In dieser Serie werden wir [Android Camera Parameters](/) verwenden, um tatsächliche Kamerainformationen zu untersuchen.

Mit der App können Sie folgendes entdecken:

- Verfügbare Kameras
- Unterstützte Auflösungen
- Bildraten
- Sensorinformationen
- Unterstützung für manuelle Steuerung
- RAW-Fähigkeit
- Hardware-Level

Anstatt an abstrakten Beispielen zu lernen, können Sie Ihr eigenes Gerät direkt untersuchen.

## Für wen ist dieses Tutorial gedacht?

Diese Serie ist konzipiert für:

- **Android-Entwickler**, die das Kamerasystem über die Basis-APIs hinaus verstehen wollen.
- **Entwickler von Kamera-Apps**, die fortgeschrittene Kamerafunktionen benötigen.
- **Entwickler für Computer Vision**, die Zugriff auf Kamerabilder und Metadaten benötigen.
- **Neugierige Entwickler**, die verstehen wollen, wie Smartphone-Kameras wirklich funktionieren.

## Bevor wir mit dem Codieren beginnen

Camera2 ist nicht schwierig, weil die API schlecht gestaltet ist. Es ist schwierig, weil moderne Kameras extrem leistungsstark sind.

Eine Smartphone-Kamera ist nicht mehr nur ein Sensor, der Bilder einfängt. Es ist ein komplettes Bildgebungssystem, bestehend aus:

- Hardware
- Firmware
- ISP-Verarbeitung
- Android-Framework
- Anwendungssoftware

Camera2 macht diese Komplexität für Entwickler zugänglich. Unser Ziel in dieser Serie ist es, sie Schritt für Schritt zu verstehen.

## Nächstes Kapitel

Im nächsten Kapitel, **Die Kamerahardware von Smartphones verstehen**, verlassen wir Android für einen Moment und erkunden die Kamera selbst. Sie erfahren, was ein Kamerasensor macht, warum größere Sensoren bessere Bilder liefern, was Objektive eigentlich bedeuten, wie die ISP-Verarbeitung funktioniert und warum zwei Telefone mit ähnlicher Megapixelzahl völlig unterschiedliche Fotos produzieren können.

Sobald Sie die Hardware verstehen, werden die Camera2-Konzepte viel einfacher.

## Zusammenfassung

Android Camera2 ist die Grundlage für die Erstellung fortschrittlicher Kameraanwendungen auf Android. Es bietet direkten Zugriff auf Kamerafunktionen und -steuerungen, die hinter normalen Kameraanwendungen verborgen sind.

Diese Serie wird Sie vom Verständnis der Smartphone-Kameras bis zum Erstellen professioneller Camera2-Anwendungen führen. Beginnen wir die Reise.
