---
sidebar_position: 3
---

# Datensicherheitsleitfaden

Dieses Dokument beschreibt die Datenerfassungs- und Datenschutzpraktiken für die Anwendung Android Camera Parameters.

## Übersicht

Android Camera Parameters wurde mit einem "Privacy First"-Ansatz entwickelt. Als Diagnosetool muss es auf Hardwareinformationen zugreifen, um zu funktionieren, erfasst oder überträgt jedoch keine personenbezogenen Daten.

## Berechtigungen

### Kameraberechtigung (`android.permission.CAMERA`)
- **Anforderung**: Notwendig für den Zugriff auf den `CameraManager` und den Abruf von `CameraCharacteristics`.
- **Verwendung**: Die App liest nur Hardware-Metadaten. Sie zeichnet **keine** Videos auf und macht keine Fotos ohne ausdrückliche Aktion des Benutzers (z. B. in zukünftigen Versionen, falls Bildaufnahmetests hinzugefügt werden).

## Datenerfassung

- **Personenbezogene Daten**: Die App **erfasst keine** Namen, E-Mail-Adressen, Telefonnummern oder andere persönliche Identifikationsmerkmale.
- **Standortdaten**: Die App **greift nicht** auf Ihr GPS oder Ihren Netzwerkstandort zu.
- **Hardware-Metadaten**: Die App liest technische Spezifikationen Ihrer Kameraobjektive (Auflösung, Brennweite, unterstützte Modi). Diese Daten verbleiben auf Ihrem Gerät, es sei denn, Sie verwenden ausdrücklich die Funktion "JSON exportieren", um sie zu teilen.

## Datenaustausch

Die Anwendung **teilt keine** Daten mit Dritten. Es sind keine Tracking-SDKs (wie Firebase Analytics oder Facebook-SDK) in die Kern-App integriert.

## Benutzerkontrolle

- **JSON-Export**: Benutzer können wählen, das Rohdaten-JSON der Kameraparameter zu kopieren oder zu teilen. Dies wird vollständig vom Benutzer initiiert.
- **Berechtigungen**: Sie können die Kameraberechtigung jederzeit über die Android-Systemeinstellungen widerrufen, obwohl die App ohne diese keine Kameradetails anzeigen kann.
