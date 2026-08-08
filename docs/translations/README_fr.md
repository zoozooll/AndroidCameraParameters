# Paramètres de Caméra Android (Android Camera Parameters)

<a href='https://play.google.com/store/apps/details?id=com.minininja.cameraparams&pcampaignid=pcampaignidMKT-Other-global-all-screenshots-pipeline'><img alt='Disponible sur Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="240"/></a>
<a href='https://zoozooll.github.io/AndroidCameraParameters/category/mastering-android-camera2-api'><img alt='Tutoriels' src='https://img.shields.io/badge/Tutoriels-Mastering%20Camera2-blue?style=for-the-badge&logo=gitbook' height="45"/></a>

[English](../../README.md) | [简体中文](README_zh-CN.md) | [繁體中文](README_zh-TW.md) | [Español](README_es.md) | [Português](README_pt-BR.md) | [Français] | [Deutsch](README_de.md) | [Русский](README_ru.md) | [हिन्दी](README_hi.md) | [Bahasa Indonesia](README_in.md) | [日本語](README_ja.md) | [한국어](README_ko.md)

Android Camera Parameters est un outil de diagnostic puissant permettant aux développeurs et aux passionnés d'explorer les capacités techniques approfondies des caméras de leurs appareils. Il exploite l'API Android Camera2 pour fournir des informations détaillées sur chaque objectif de votre appareil.

![Aperçu de l'application](../../website/static/img/camera_params_feature_graph.png)

## Caractéristiques Principales

*   **Diagnostics Détaillés** : Inspectez les `CameraCharacteristics` pour tous les objectifs (arrière, avant, externe).
*   **Détection du Niveau Matériel** : Voyez instantanément si votre appareil prend en charge les fonctionnalités `LEGACY`, `LIMITED`, `FULL` ou `LEVEL_3`.
*   **Suivi des Fonctionnalités en Temps Réel** : Vérifiez la prise en charge de la capture RAW, de la stabilisation optique de l'image (OIS), de l'exposition manuelle, de la mise au point manuelle et plus encore via un tableau de bord intuitif.
*   **Exploration Catégorisée** : Des centaines de paramètres organisés par catégories Capteur, Objectif, AE/AF/AWB et Traitement avec une fonctionnalité de **recherche intégrée**.
*   **Favoris (Bientôt disponible)** : Marquez les paramètres fréquemment consultés pour un accès rapide.
*   **Exportation de Données Brutes** : Affichez le profil complet de la caméra sous forme de JSON structuré.
*   **Support Multilingue** : Entièrement localisé dans plus de 12 langues, dont le chinois, l'espagnol, le japonais et plus encore.

## Langues Prises en Charge

L'application est localisée pour s'adresser à un public mondial :
- 🇺🇸 Anglais
- 🇨🇳 Chinois (Simplifié)
- 🇹🇼/🇭🇰 Chinoise (Traditionnel)
- 🇪🇸 Espagnol
- 🇧🇷 Portugais (Brésil)
- 🇫🇷 Français
- 🇩🇪 Allemand
- 🇷🇺 Russe
- 🇮🇳 Hindi
- 🇮🇩 Indonésien
- 🇯🇵 Japonais
- 🇰🇷 Coréen

## Pile Technique

- **Langage** : Kotlin
- **Framework UI** : Jetpack Compose
- **Système de Design** : Material 3
- **Architecture** : MVVM
- **Bibliothèques** :
    - [Camera2 API](https://developer.android.com/training/camera2) : Interaction principale avec la caméra.
    - [Gson](https://github.com/google/gson) : Sérialisation JSON pour l'exportation de données brutes.
    - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) : Navigation dans l'application.

## Structure du Projet

- `app/` : Module principal de l'application Android.
    - `com.aaron.cameraparams.ui` : Écrans et composants UI basés sur Compose.
    - `com.aaron.cameraparams.camera` : Logique d'interaction avec le CameraManager et de récupération des caractéristiques.
- `camera_parameters/` : Exemples de vidages JSON de paramètres de caméra de divers appareils (Pixel 3, Samsung S10+, etc.).
- `docs/` : Documentation supplémentaire et captures d'écran.

## Commencer

### Prérequis

- Android Studio Koala ou plus récent.
- Android SDK 37 (Compilation/Cible).
- Un appareil Android physique (recommandé) ou un émulateur avec support Camera2.

### Construire et Exécuter

1. Cloner le dépôt :
   ```bash
   git clone https://github.com/zoozooll/AndroidCameraParameters.git
   ```
2. Ouvrir le projet dans Android Studio.
3. Construire le projet :
   ```bash
   ./gradlew assembleDebug
   ```
4. Installer et exécuter sur votre appareil.

## Licence

Ce projet est sous **Licence MIT**. Voir le fichier [LICENSE](../../LICENSE) pour plus de détails.

## Support ou Contact

E-mail : kangkang365@gmail.com
Site du Projet : [Documentation d'Android Camera Parameters](https://zoozooll.github.io/AndroidCameraParameters/)
