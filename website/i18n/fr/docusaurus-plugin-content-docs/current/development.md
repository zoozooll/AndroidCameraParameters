---
sidebar_position: 2
---

# Documentation pour les Développeurs

Ce document fournit un aperçu technique de l'application **Android Camera Parameters**, de son architecture et de ses directives de développement.

## Aperçu du Projet

L'application est un outil de diagnostic pour inspecter les `CameraCharacteristics` d'Android Camera2. Elle fournit une interface moderne et conviviale pour explorer les niveaux matériels, les capacités et les valeurs de paramètres bruts de tous les objectifs de caméra d'un appareil.

## Architecture

Le projet suit le modèle architectural **MVVM (Model-View-ViewModel)** et est construit avec **Jetpack Compose** pour la couche UI.

### Composants Principaux

#### `CameraParamsActivity`
Le point d'entrée unique de l'application.
- Gère les autorisations d'exécution (CAMERA).
- Initialise l'interface utilisateur Compose via `setContent`.
- Héberge le `CameraParamsTheme`.

#### `CameraViewModel`
Le gestionnaire d'état central pour l'interface utilisateur.
- Maintient `UiState` qui inclut la liste des caméras, l'index sélectionné, les paramètres catégorisés et la requête de recherche.
- **Détection de Fonctionnalités** : Contient la logique dans `detectFeatureFlags()` pour déterminer dynamiquement les capacités matérielles telles que le support RAW, l'OIS et l'exposition manuelle.
- **Catégorisation** : Regroupe des centaines de clés Camera2 en sections logiques (Capteur, Objectif, etc.) pour une meilleure lisibilité.

#### `CameraParamsHelper`
Un wrapper utilitaire autour du `CameraManager` d'Android.
- Récupère les `CameraCharacteristics` pour des identifiants spécifiques.
- Fournit un formatage spécialisé pour les types de caméras complexes (par exemple, conversion des modes `IntArray` en chaînes lisibles par l'homme).

## Couche UI (Jetpack Compose)

L'interface utilisateur est construite avec **Material 3** avec un thème sombre strictement appliqué.

### Structure de Navigation

L'application utilise `androidx.navigation.compose` géré dans `MainScreen.kt`.

| Écran | Responsabilité |
| :--- | :--- |
| **[Aperçu](overview.md)** | Tableau de bord de haut niveau montrant la carte de résumé, le niveau matériel et les puces des fonctionnalités clés. |
| **Catégories** | Liste extensible de tous les paramètres regroupés par section avec filtrage de recherche. |
| **Brut (JSON)** | Représentation JSON avec coloration syntaxique de toutes les propriétés de la caméra. |
| **Détail** | Vue ciblée pour un paramètre unique, montrant la valeur formatée et les données brutes. |

### Style

- **Thème** : Défini dans `Theme.kt`.
- **Couleurs** : Couleur primaire `#7B61FF` (Violet) utilisée pour les mises en évidence et les actions principales.
- **Surface** : Fond sombre `#121417` avec des variantes `#1E1F23` pour les cartes.

## Logique Clé

### Détection Dynamique des Fonctionnalités

Les puces "Fonctionnalités Clés" sur le tableau de bord ne sont pas statiques. Elles sont calculées dans `CameraViewModel.detectFeatureFlags()` :

- **RAW** : Vérifié via `REQUEST_AVAILABLE_CAPABILITIES_RAW`.
- **OIS** : Détecté si `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` contient `ON`.
- **Exp. Manuelle** : Disponible si `CONTROL_AE_MODE_OFF` est pris en charge.
- **Mise au Point Manuelle** : Activée si `LENS_INFO_MINIMUM_FOCUS_DISTANCE` est supérieur à 0.

## Guide de Développement

### Prérequis
- Android Studio Ladybug (ou plus récent).
- Kotlin 2.0+ (Le projet utilise le nouveau plugin Gradle Compose Compiler).
- SDK Minimum : 21 (Android 5.0).

### Ajouter une Nouvelle Catégorie
Pour ajouter ou modifier le regroupement des paramètres, mettez à jour la méthode `getCategoryForKey()` dans `CameraViewModel.kt`. Elle utilise la correspondance de chaînes sur les noms de clés de caméra pour les affecter à des catégories.

### Mise à jour du Thème
Les couleurs peuvent être ajustées dans `Color.kt`. L'application est conçue pour être optimale en mode sombre ; tout changement apporté à la palette claire doit être testé avec soin.
