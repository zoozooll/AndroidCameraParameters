---
sidebar_position: 1
slug: /
description: Aperçu du tableau de bord d'Android Camera Parameters et de ses principales fonctions de diagnostic, notamment la détection du niveau matériel et le suivi des fonctionnalités en temps réel.
keywords: [tableau de bord caméra android, détection du niveau matériel, diagnostic caméra]
---

# Vue d'ensemble de l'application

Cette page fournit une analyse détaillée du tableau de bord et des fonctionnalités clés de l'application.

![App Overview](/img/camera_params_feature_graph.png)

## Composants du tableau de bord

### 1. Navigation et Sélection
- **Menu latéral** : Accédez à la politique de confidentialité, à l'évaluation de l'application et aux informations à propos via l'icône de menu en haut à gauche.
- **Sélection de la caméra** : Appuyez sur le nom de la caméra ou sur le badge d'identification (par exemple, "0") pour ouvrir un menu déroulant et basculer entre les objectifs disponibles (arrière, avant, ultra-grand angle, etc.).
- **Navigation inférieure** : Basculez en toute transparence entre **Aperçu**, **Catégories**, **JSON Brut** et **Favoris**.

### 2. Carte de résumé
La carte de résumé en haut fournit l'information la plus critique :
- **Niveau matériel** : Le niveau de support de l'API Camera2 (LEGACY, LIMITED, FULL ou LEVEL_3). Cela détermine les capacités globales de l'objectif.

### 3. Grille des fonctionnalités clés
Une grille visuelle fournissant l'état instantané des fonctionnalités de qualité professionnelle :
- **Résolution et taille du capteur** : Caractéristiques physiques du capteur.
- **FPS Vidéo Max** : Capacités de fréquence d'images maximale.
- **Support RAW** : Indique si le capteur peut produire des données non compressées.
- **OIS (Stabilisation optique de l'image)** : Disponibilité de la stabilisation physique de l'objectif.
- **Contrôle manuel** : État du support de l'exposition manuelle et de la mise au point manuelle.
- **Traitement** : Support du HDR, de la détection de visage et de la réduction des yeux rouges.

### 4. Paramètres catégorisés (Onglet Catégories)
Explorez la liste complète des CameraCharacteristics organisées en groupes logiques :
- **Capteur** : Résolution, taille physique, plages de sensibilité.
- **Objectif** : Distance focale, ouverture, modes de stabilisation.
- **AE/AF/AWB** : Modes de contrôle détaillés pour l'exposition, la mise au point et la balance des blancs.
- **Recherche** : Utilisez la barre de recherche intégrée pour trouver rapidement des clés ou des valeurs d'API spécifiques.
