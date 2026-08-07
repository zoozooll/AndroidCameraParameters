---
sidebar_position: 2
description: Architecture technique de l'application Android Camera Parameters, incluant les détails du modèle MVVM, la structure de l'interface utilisateur Jetpack Compose et les directives de développement.
keywords: [développement android, mvvm, jetpack compose, tutoriel api camera2]
---

# Documentation pour les développeurs

Ce document fournit un aperçu technique de l'application **Android Camera Parameters**, de son architecture et de ses directives de développement.

## Aperçu du projet

L'application est un outil de diagnostic pour inspecter les `CameraCharacteristics` d'Android Camera2. Elle fournit une interface moderne et conviviale pour explorer les niveaux matériels, les capacités et les valeurs de paramètres bruts pour tous les objectifs de caméra sur un appareil.

## Architecture

Le projet suit le modèle architectural **MVVM (Modèle-Vue-VueModèle)** et est construit en utilisant **Jetpack Compose** pour la couche d'interface utilisateur.

### Composants principaux

#### `CameraParamsActivity`
Le point d'entrée unique de l'application.
- Gère les autorisations d'exécution (CAMERA).
- Initialise l'interface utilisateur Compose via `setContent`.
- Héberge le `CameraParamsTheme`.

#### `CameraViewModel`
Le gestionnaire d'état central pour l'interface utilisateur.
- Maintient le `UiState` qui inclut la liste des caméras, l'index sélectionné, les paramètres catégorisés et la requête de recherche.
- **Détection des fonctionnalités** : Contient la logique dans `detectFeatureFlags()` pour déterminer dynamiquement les capacités matérielles telles que le support RAW, l'OIS et l'exposition manuelle.
- **Catégorisation** : Regroupe des centaines de clés Camera2 en sections logiques (Capteur, Objectif, etc.) pour une meilleure lisibilité.

#### `CameraParamsHelper`
Un wrapper utilitaire autour du `CameraManager` Android.
- Récupère les `CameraCharacteristics` pour des ID spécifiques.
- Fournit un formatage spécialisé pour les types de caméras complexes (par exemple, conversion des modes `IntArray` en chaînes lisibles par l'homme).

## Couche UI (Jetpack Compose)

L'interface utilisateur est construite en utilisant **Material 3** avec un thème sombre strictement appliqué.

### Structure de navigation

L'application utilise `androidx.navigation.compose` géré dans `MainScreen.kt`.

| Écran | Responsabilité |
| :--- | :--- |
| **[Aperçu](overview.md)** | Tableau de bord de haut niveau montrant la carte de résumé, le niveau matériel et les puces de fonctionnalités clés. |
| **Catégories** | Liste extensible de tous les paramètres regroupés par section avec filtrage de recherche. |
| **Brut (JSON)** | Représentation JSON colorisée de toutes les propriétés de la caméra. |
| **Détail** | Vue ciblée pour un seul paramètre, montrant la valeur formatée et les données brutes. |

### Style

- **Thème** : Défini dans `Theme.kt`.
- **Couleurs** : Couleur primaire `#7B61FF` (Violet) utilisée pour les mises en évidence et les actions principales.
- **Surface** : Fond sombre `#121417` avec des variantes `#1E1F23` pour les cartes.

## Logique clé

### Détection dynamique des fonctionnalités

Les puces "Fonctionnalités clés" sur le tableau de bord ne sont pas statiques. Elles sont calculées dans `CameraViewModel.detectFeatureFlags()` :

- **RAW** : Vérifié via `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS** : Détecté si `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` contient `ON`.
- **Exposition Manuelle** : Disponible si `CONTROL_AE_MODE_OFF` est supporté.
- **Mise au point Manuelle** : Activée si `LENS_INFO_MINIMUM_FOCUS_DISTANCE` est supérieur à 0.

## Guide de développement

### Prérequis
- Android Studio Ladybug (ou plus récent).
- Kotlin 2.0+ (Le projet utilise le nouveau plugin Gradle Compose Compiler).
- SDK Minimum : 21 (Android 5.0).

### Ajouter une nouvelle catégorie
Pour ajouter ou modifier le regroupement de paramètres, mettez à jour la méthode `getCategoryForKey()` dans `CameraViewModel.kt`. Elle utilise la correspondance de chaînes sur les noms de clés de caméra pour les affecter à des catégories.

### Mise à jour du thème
Les couleurs peuvent être ajustées dans `Color.kt`. L'application est conçue pour être la plus esthétique en mode sombre ; tout changement à la palette claire doit être testé avec soin.
