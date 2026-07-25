---
sidebar_position: 1
title: "Chapitre 1 : Bienvenue dans Android Camera2"
description: Découvrez pourquoi Android Camera2 est important, en quoi il diffère de CameraX et ce que cette série couvrira.
keywords: [Android Camera2, CameraX, capacités de l'appareil photo, développement photo Android]
---

Avant de contrôler l'appareil photo, nous devons comprendre le système de l'appareil photo.

## Introduction

Presque tous les smartphones d'aujourd'hui disposent d'un système d'appareil photo puissant. Un téléphone moderne peut :

- Capturer des photos de qualité professionnelle
- Enregistrer des vidéos en 4K et 8K
- Créer des effets de portrait
- Photographier dans des conditions de très faible luminosité
- Capturer des vidéos au ralenti
- Générer des informations de profondeur
- Combiner plusieurs caméras ensemble

Mais lorsque vous ouvrez l'application photo par défaut, vous ne voyez qu'une interface simple : un déclencheur, un contrôle du zoom et quelques modes de prise de vue.

Derrière cette interface simple se cache un système d'une complexité surprenante. L'application photo communique avec des composants matériels, des processeurs d'image et des frameworks Android pour produire chaque image.

En tant que développeurs Android, nous pouvons vouloir créer des applications qui vont au-delà de l'application photo par défaut, telles que :

- Une application de photographie manuelle
- Un outil de test d'appareil photo
- Une application de vision par ordinateur
- Une application de numérisation 3D
- Un enregistreur vidéo professionnel
- Un analyseur de capacités d'appareil photo

Pour construire ces applications, nous devons comprendre l'API Android Camera2.

## Qu'est-ce qu'Android Camera2 ?

Android Camera2 est le framework photo moderne introduit par Google dans Android 5.0 (API niveau 21). Il a remplacé l'API Android Camera originale.

L'ancienne API Camera a été conçue pour un monde plus simple : un seul appareil photo, une capture photo de base et un enregistrement vidéo simple. Les appareils photo des smartphones ont considérablement évolué depuis lors. Les appareils modernes peuvent contenir plusieurs caméras arrière, des objectifs grand angle, des téléobjectifs, des capteurs de profondeur et des caméras externes.

Ils prennent également en charge des fonctionnalités avancées :

- Exposition et mise au point manuelles
- Capture d'images RAW
- Vidéo à haute vitesse
- Traitement HDR
- Stabilisation optique

Camera2 a été créé pour donner aux développeurs un contrôle beaucoup plus approfondi sur le matériel de l'appareil photo.

## Camera2 vs CameraX

Camera2 et CameraX résolvent des problèmes différents.

### CameraX

CameraX est une bibliothèque de plus haut niveau conçue pour faciliter les tâches photo courantes, notamment :

- Affichage d'un aperçu
- Prise de photos
- Enregistrement de vidéos
- Gestion de la compatibilité des appareils

La plupart des applications devraient commencer par CameraX.

### Camera2

Camera2 est le framework de plus bas niveau. Il donne aux développeurs un accès direct aux capacités de l'appareil photo, y compris les informations du capteur, les paramètres d'exposition, les contrôles de mise au point, les métadonnées de l'appareil photo, les capacités matérielles et la prise en charge du format RAW.

Camera2 est plus complexe, mais offre beaucoup plus de contrôle.

| | CameraX | Camera2 |
| --- | --- | --- |
| Niveau | Bibliothèque de haut niveau | API de bas niveau |
| Difficulté | Plus facile | Plus complexe |
| Contrôle | Limité | Étendu |
| Idéal pour | Applications photo normales | Applications photo avancées |

Cette série de tutoriels se concentre sur Camera2 car sa compréhension nous aide à comprendre comment les appareils photo Android fonctionnent réellement.

## Pourquoi apprendre Camera2 ?

Vous vous demandez peut-être : *Pourquoi devrais-je apprendre Camera2 alors que CameraX existe déjà ?*

### Comprendre ce que l'appareil peut réellement faire

Chaque téléphone Android est différent. Un appareil peut prendre en charge la capture RAW, la vidéo 4K à 60 fps et les contrôles manuels ; un autre non. Camera2 permet aux applications de découvrir ces capacités.

### Créer des applications photo professionnelles

Les applications qui nécessitent des fonctionnalités photo avancées ont généralement besoin de Camera2. Les exemples incluent les applications photo professionnelles, les applications d'imagerie scientifique, les applications de réalité augmentée, les systèmes de vision par ordinateur et les outils de production vidéo.

### Comprendre la photographie sur smartphone

De nombreuses fonctionnalités photo modernes sont basées sur des concepts exposés via Camera2 :

- Exposition
- ISO
- Mise au point
- Balance des blancs
- HDR
- Plusieurs caméras

L'apprentissage de Camera2 vous apprend également comment fonctionnent les appareils photo des smartphones.

## Qu'allez-vous apprendre dans cette série ?

Cette série est conçue pour vous amener du niveau débutant au niveau avancé.

### Partie 1 : Comprendre les appareils photo

Vous apprendrez comment fonctionnent les appareils photo des smartphones, ce que contient le matériel photo, comment Android représente les appareils photo et comment inspecter votre propre appareil.

### Partie 2 : Votre première application Camera2

Vous apprendrez comment trouver et ouvrir des caméras, créer un aperçu et capturer des images.

### Partie 3 : Commandes de l'appareil photo

Vous découvrirez l'exposition, l'ISO, la mise au point, la balance des blancs, le flash et le zoom.

### Partie 4 : Fonctionnalités photo avancées

Vous apprendrez la capture RAW, la vidéo haute vitesse, les appareils multi-caméras, les caméras logiques et physiques, les extensions de caméra et les fonctionnalités HDR.

### Partie 5 : Plongée au cœur des métadonnées de l'appareil photo

Vous explorerez les paramètres importants de Camera2, notamment :

- `android.sensor.info.activeArraySize`
- `android.scaler.availableMaxDigitalZoom`
- `android.control.aeAvailableModes`
- `android.request.availableCapabilities`

Vous comprendrez non seulement ce que signifient ces paramètres, mais aussi pourquoi ils existent.

## Apprendre avec Android Camera Parameters

La lecture de la documentation est utile, mais les capacités des appareils photo sont plus faciles à comprendre lorsque vous pouvez voir des données réelles provenant d'un vrai téléphone. Tout au long de cette série, nous utiliserons [Android Camera Parameters](/) pour explorer les informations réelles de l'appareil photo.

Vous pouvez utiliser l'application pour découvrir :

- Les caméras disponibles
- Les résolutions prises en charge
- Les taux de rafraîchissement
- Les informations du capteur
- La prise en charge du contrôle manuel
- La capacité RAW
- Le niveau matériel

Au lieu d'apprendre à partir d'exemples abstraits, vous pouvez directement étudier votre propre appareil.

## À qui s'adresse ce tutoriel ?

Cette série est conçue pour :

- Les **développeurs Android** qui souhaitent comprendre le système photo au-delà des API de base.
- Les **développeurs d'applications photo** qui ont besoin de fonctionnalités avancées.
- Les **développeurs en vision par ordinateur** qui ont besoin d'accéder aux images et aux métadonnées de la caméra.
- Les **développeurs curieux** qui veulent comprendre comment fonctionnent réellement les appareils photo des smartphones.

## Avant de commencer à coder

Camera2 n'est pas difficile parce que l'API est mal conçue. C'est difficile parce que les appareils photo modernes sont extrêmement puissants.

L'appareil photo d'un smartphone n'est plus seulement un capteur qui capture des images. C'est un système d'imagerie complet impliquant :

- Le matériel
- Le firmware
- Le traitement ISP
- Le framework Android
- Le logiciel applicatif

Camera2 expose cette complexité aux développeurs. Notre objectif dans cette série est de la comprendre étape par étape.

## Chapitre suivant

Dans le prochain chapitre, **Comprendre le matériel photo des smartphones**, nous quitterons Android un instant pour explorer l'appareil photo lui-même. Vous apprendrez ce que fait un capteur photo, pourquoi les capteurs plus grands produisent de meilleures images, ce que signifient réellement les objectifs, comment fonctionne le traitement ISP et pourquoi deux téléphones avec des nombres de mégapixels similaires peuvent produire des photos complètement différentes.

Une fois que vous aurez compris le matériel, les concepts de Camera2 deviendront beaucoup plus faciles.

## Résumé

Android Camera2 est la base pour créer des applications photo avancées sur Android. Il offre un accès direct aux capacités et aux commandes de l'appareil photo qui sont cachées derrière les applications photo classiques.

Cette série vous guidera de la compréhension des appareils photo des smartphones à la création d'applications Camera2 de niveau professionnel. Commençons le voyage.
