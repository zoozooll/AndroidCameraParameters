---
sidebar_position: 3
description: Datensicherheits- und Datenschutzpraktiken für die Android Camera Parameters App. Erfahren Sie mehr über Kameraberechtigungen und den Umgang mit Hardware-Metadaten.
keywords: [Datensicherheit, Datenschutzrichtlinie, Android-Berechtigungen]
---

# Datensicherheitshandbuch

Dieses Dokument beschreibt die Datenerfassungs- und Datenschutzpraktiken für die Android Camera Parameters Anwendung.

## Übersicht

Android Camera Parameters wurde mit einem "Privacy First"-Ansatz entwickelt. Als Diagnosetool muss es auf Hardwareinformationen zugreifen, um zu funktionieren, erfasst oder überträgt jedoch keine personenbezogenen Daten.

## Berechtigungen

### Kameraberechtigung (`android.permission.CAMERA`)
- **Anforderung**: Erforderlich, um auf den `CameraManager` zuzugreifen und `CameraCharacteristics` abzurufen.
- **Verwendung**: Die App liest nur Hardware-Metadaten. Sie nimmt **keine** Videos auf und macht keine Fotos ohne explizite Benutzeraktion (z. B. in zukünftigen Versionen, falls Bildaufnahmetests hinzugefügt werden).

## Datenerfassung

- **Personenbezogene Informationen**: Die App **erfasst keine** Namen, E-Mail-Adressen, Telefonnummern oder andere persönliche Identifikatoren.
- **Standortdaten**: Die App **greift nicht** auf Ihren GPS- oder Netzwerkstandort zu.
- **Hardware-Metadaten**: Die App liest technische Spezifikationen Ihrer Kameraobjektive (Auflösung, Brennweite, unterstützte Modi). Diese Daten verbleiben auf Ihrem Gerät, es sei denn, Sie verwenden explizit die Funktion "JSON exportieren", um sie zu teilen.

## Datenweitergabe

Die Anwendung **gibt keine** Daten an Dritte weiter. In der Kern-App sind keine Tracking-SDKs (wie Firebase Analytics oder das Facebook-SDK) integriert.

## Benutzerkontrolle

- **JSON-Export**: Benutzer können wählen, das rohe Kameraparameter-JSON zu kopieren oder zu teilen. Dies wird vollständig vom Benutzer initiiert.
- **Berechtigungen**: Sie können die Kameraberechtigung jederzeit über die Android-Systemeinstellungen widerrufen, wobei die App dann jedoch keine Kameradetails anzeigen kann.
