---
sidebar_position: 4
title: "Chapitre 4 : Explorer l'appareil photo de votre téléphone"
description: Utilisez l'application Android Camera Parameters pour explorer les capacités de l'appareil photo de votre appareil, notamment les ID de caméra, les résolutions, les FPS, le niveau matériel, la prise en charge RAW, et bien plus encore.
keywords: [paramètres de l'appareil photo, ID de caméra, niveau matériel, prise en charge RAW, FPS, résolution]
---

C'est ici que l'apprentissage devient interactif. Explorons l'appareil photo de votre téléphone !

## Introduction

Lire des articles sur les appareils photo est utile, mais rien ne vaut la visualisation de données réelles provenant de votre propre appareil. C'est là qu'intervient Android Camera Parameters.

Ce chapitre est consacré à **l'exploration**. Pas de codage pour l'instant. Juste de la curiosité.

## Installer Android Camera Parameters

Si ce n'est pas déjà fait, installez l'application Android Camera Parameters sur votre appareil Android :

1. Ouvrez le Google Play Store
2. Recherchez « Android Camera Parameters »
3. Installez l'application
4. Ouvrez-la et accordez les autorisations d'accès à l'appareil photo

## Comprendre l'interface

Lorsque vous ouvrez l'application, vous voyez plusieurs sections :

### Écran Aperçu
Affiche les informations de base de l'appareil photo en un coup d'œil :
- ID de caméra
- Orientation de l'objectif
- Niveau matériel
- Taille du capteur
- Capacités disponibles

### Écran Catégories
Organise les paramètres de l'appareil photo en groupes logiques :
- Informations sur la caméra
- Capteur
- Objectif
- Contrôle
- Scaler
- Flash
- Et bien plus

### Écran RAW JSON
Affiche l'intégralité de CameraCharacteristics au format JSON brut pour les utilisateurs avancés.

## Explorons

Passons en revue les informations clés que vous devez rechercher.

### ID de caméra

Android attribue à chaque caméra un numéro d'identification unique. Recherchez :
- **Caméra 0** — Généralement la caméra arrière grand angle
- **Caméra 1** — Il peut s'agir de la caméra frontale ou d'une autre caméra arrière
- **Caméra 2** — Souvent la caméra ultra-grand angle ou téléobjectif
- **Caméra 3+** — Caméras supplémentaires (macro, profondeur, etc.)

Chaque ID représente un périphérique de caméra distinct avec ses propres caractéristiques.

### Niveau matériel

C'est l'une des informations les plus importantes :

| Niveau | Description |
| --- | --- |
| **LEGACY** | Anciens appareils, prise en charge limitée de Camera2 |
| **LIMITED** | Fonctionnalités de base de Camera2 |
| **FULL** | Contrôles manuels complets, prise en charge RAW |
| **LEVEL_3** | Fonctionnalités avancées comme le retraitement YUV |

Vérifiez quel niveau matériel votre téléphone prend en charge. Cela détermine les fonctionnalités Camera2 disponibles.

### Informations sur le capteur

Recherchez :
- **Taille du capteur** — Dimensions physiques du capteur
- **Taille du tableau actif** — La zone réellement utilisée pour capturer des images
- **Taille du réseau de pixels** — Total des pixels sur le capteur
- **Durée maximale d'image** — Temps minimum entre deux images
- **Formats de sortie** — JPEG, RAW, YUV, etc.

### Options de résolution

Les caméras prennent en charge plusieurs résolutions. Vérifiez :
- **Tailles d'aperçu** — Résolutions disponibles pour l'affichage
- **Tailles de photo** — Résolutions disponibles pour la capture de photos
- **Tailles vidéo** — Résolutions disponibles pour l'enregistrement vidéo

Remarquez les différents rapports d'aspect : 4:3, 16:9, 1:1.

### Fréquence d'images (FPS)

Recherchez :
- **Plage FPS d'aperçu** — Images par seconde pour l'aperçu
- **Plage FPS de capture** — Images par seconde pour la capture fixe
- **Vidéo haute vitesse** — Modes spéciaux à haute fréquence d'images

Un FPS plus élevé signifie une vidéo plus fluide et une autofocus plus réactive.

### Prise en charge RAW

Vérifiez si votre caméra prend en charge la capture RAW :
- **Formats RAW** — RAW_SENSOR, RAW10, RAW12, RAW16
- **Tailles RAW** — Résolutions disponibles pour la capture RAW

La prise en charge RAW nécessite au moins un niveau matériel FULL.

### Capacités du flash

Recherchez :
- **Mode flash** — OFF, ON, AUTO, TORCH
- **Modes disponibles** — Quelles fonctionnalités de flash sont prises en charge
- **Informations sur le flash** — Puissance et capacités du flash

### Zoom

Vérifiez :
- **Zoom numérique maximal** — Dans quelle mesure vous pouvez zoomer numériquement
- **Longueurs focales disponibles** — Différents objectifs et leurs longueurs focales
- **Prise en charge du zoom fluide** — Si le zoom peut être ajusté en douceur

### Modes de mise au point

Recherchez :
- **Modes de mise au point disponibles** — AUTO, FIXED, MACRO, CONTINUOUS_VIDEO, CONTINUOUS_PICTURE, EDGE
- **Plage de distance de mise au point** — Distance de mise au point minimale et maximale
- **Régions AF** — Nombre de régions d'autofocus prises en charge

### Contrôle de l'exposition

Vérifiez :
- **Modes AE** — AUTO, ON, OFF
- **Modes AE disponibles** — Quels modes d'exposition sont pris en charge
- **Plage d'exposition** — Temps d'exposition minimum et maximum
- **Plage ISO** — Valeurs ISO prises en charge

## À vous de jouer

C'est maintenant à votre tour d'explorer. Répondez à ces questions sur votre téléphone :

1. Combien de caméras votre téléphone possède-t-il ?
2. Quel niveau matériel prennent-elles en charge ?
3. Quelle caméra prend en charge RAW ?
4. Quelle est la résolution maximale disponible ?
5. Une caméra prend-elle en charge la vidéo 4K ?
6. Quel est le niveau de zoom maximal ?
7. Votre téléphone a-t-il une caméra téléobjectif ou ultra-grand angle ?

## Pourquoi c'est important

Vous vous demandez peut-être pourquoi nous explorons avant de coder. Voici pourquoi :

1. **Chaque téléphone est différent** — Ce qui fonctionne sur un appareil peut ne pas fonctionner sur un autre
2. **Camera2 nécessite une adaptation** — Les bonnes applications Camera2 interrogent les capacités, ne les supposent pas
3. **La compréhension développe l'intuition** — Lorsque vous voyez des données réelles, les concepts abstraits deviennent concrets

Lorsque nous commencerons à coder, vous saurez déjà à quoi vous attendre de votre appareil.

## Comparez avec des amis

Si vous avez des amis avec des téléphones différents, comparez vos découvertes :
- Le téléphone phare a-t-il un meilleur niveau matériel ?
- Les téléphones d'entrée de gamme manquent-ils de prise en charge RAW ?
- Comment le nombre de caméras varie-t-il ?

Cela vous aide à comprendre l'écosystème photo Android.

## Découvertes courantes

Voici quelques découvertes courantes que les gens font :

- **Les téléphones phares** ont souvent un niveau matériel FULL ou LEVEL_3
- **Les téléphones d'entrée de gamme** ont souvent un niveau matériel LIMITED ou LEGACY
- **La plupart des téléphones** prennent en charge la capture JPEG
- **La prise en charge RAW** n'est pas encore universelle
- **Plusieurs caméras** sont standard sur les téléphones modernes
- **Les caméras frontales** ont généralement une résolution inférieure à celle des caméras arrière

## Chapitre suivant

Maintenant que vous avez exploré l'appareil photo de votre téléphone, vous êtes prêt à commencer à coder ! Dans le prochain chapitre, nous présenterons la première classe Camera2 : **CameraManager**.

CameraManager est le point d'entrée de l'API Camera2. Il vous permet de :
- Énumérer les caméras disponibles
- Obtenir les caractéristiques de la caméra
- Ouvrir des caméras

Commençons !

## Résumé

Explorer l'appareil photo de votre téléphone est le meilleur moyen de comprendre ce que Camera2 peut faire. Android Camera Parameters facilite cette tâche en affichant toutes les capacités de l'appareil photo de manière organisée.

Éléments clés à rechercher :
- ID de caméra et leurs rôles
- Niveau matériel (LEGACY, LIMITED, FULL, LEVEL_3)
- Options de résolution
- Prise en charge RAW
- Capacités de flash et de zoom
- Contrôles de mise au point et d'exposition

Cette exploration pratique jette les bases du développement d'applications Camera2.
