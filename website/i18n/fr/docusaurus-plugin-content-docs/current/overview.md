---
sidebar_position: 1
slug: /
description: Aperçu du tableau de bord d'Android Camera Parameters et de ses principales fonctions de diagnostic, notamment la détection du niveau matériel et le suivi des fonctionnalités en temps réel.
keywords: [tableau de bord caméra android, détection du niveau matériel, diagnostic caméra]
---

# Aperçu de l'Application

Cette page fournit une ventilation détaillée du tableau de bord de l'application et de ses fonctionnalités clés.

![Aperçu de l'Application](/img/designer/page1.png)

## Composants du Tableau de Bord

### 1. Navigation et Sélection
- **Menu de Navigation** : Accès rapide aux Paramètres, à l'Exportation/Importation, aux thèmes et aux informations À propos.
- **Sélection de la Caméra** : Cliquez sur le nom de la caméra ou sur le badge d'identification pour basculer entre les objectifs arrière, avant et externes.

### 2. Carte de Résumé
La carte de résumé fournit un aperçu de haut niveau de la caméra sélectionnée :
- **Niveau Matériel** : Indique le niveau de support de l'API Camera2 (LEGACY, LIMITED, FULL, LEVEL_3).
- **Résolution du Capteur** : Le nombre total de mégapixels du capteur.
- **FPS Vidéo Max** : Le taux de rafraîchissement le plus élevé pris en charge pour l'enregistrement vidéo.

### 3. Grille des Fonctionnalités Clés
Une grille d'aperçu rapide montrant le support des fonctionnalités professionnelles critiques :
- **Support RAW** : Capacité à capturer des données de capteur non compressées.
- **Exposition et Mise au Point Manuelles** : Contrôle de niveau professionnel sur la capture d'image.
- **Capacités du Flash** : Support du flash automatique et de la réduction des yeux rouges.
- **OIS (Stabilisation Optique de l'Image)** : Réduction des secousses basée sur le matériel.
- **HDR et Détection de Visage** : Capacités de traitement de scène intelligentes.

### 4. Catégories de Paramètres
Les caractéristiques détaillées de la caméra sont regroupées en catégories logiques pour une exploration facile :
- **Capteur** : Taille de la matrice active, sensibilité, plages de temps d'exposition.
- **Objectif** : Ouverture, distance focale, distance de mise au point.
- **AE/AF/AWB** : Contrôles détaillés pour l'exposition automatique, l'autofocus et la balance des blancs automatique.
- **Sortie** : Tailles JPEG, RAW et YUV prises en charge.
