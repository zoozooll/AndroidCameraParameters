---
sidebar_position: 2
title: "Chapitre 2 : Comprendre les caméras de smartphones"
description: Apprenez les composants des caméras de smartphones, y compris les objectifs, les capteurs, l'ISP et la création des photos avant de plonger dans Camera2.
keywords: [caméra smartphone, objectif caméra, capteur d'image, ISP, matériel caméra]
---

Avant d'apprendre Camera2, comprenons d'abord la caméra que vous contrôlez.

## Introduction

Regardez le dos de votre smartphone.

Vous pouvez voir une caméra.

Ou deux.

Ou peut-être trois ou même cinq objectifs photo.

Les caméras de smartphones modernes sont incroyablement puissantes. Certaines peuvent enregistrer des vidéos 8K. D'autres peuvent capturer des photos nocturnes époustouflantes. D'autres encore peuvent capturer des images RAW pour un montage professionnel.

Mais vous êtes-vous déjà demandé ce qui se passe réellement après avoir appuyé sur le bouton de déclenchement ?

La caméra prend-elle simplement une photo ?

Pas du tout.

Capturer une seule photo nécessite que plusieurs composants matériels travaillent ensemble en une fraction de seconde. Comprendre ces composants rendra l'apprentissage de Camera2 beaucoup plus facile.

## Une caméra de smartphone est plus qu'un objectif

Beaucoup de gens pensent que les cercles noirs sur le dos d'un téléphone sont "la caméra".

En réalité, ces cercles ne sont que les objectifs. Une caméra de smartphone complète se compose de plusieurs composants majeurs.

```
Lumière
│
▼
Objectif
│
▼
Capteur d'image
│
▼
ISP (Processeur de signal d'image)
│
▼
Mémoire
│
▼
Framework de caméra Android
│
▼
Votre application
```

Chaque photo suit ce pipeline. Examinons chaque composant.

## L'objectif

L'objectif est la première partie de la caméra. Son travail est simple :

> Collecter la lumière et la focaliser sur le capteur d'image.

Différents objectifs produisent différentes images. Par exemple :

- **Objectif grand angle** — Photographie quotidienne standard
- **Objectif ultra-grand angle** — Capture beaucoup plus de la scène
- **Objectif téléphoto** — Rapproche les objets éloignés
- **Objectif macro** — Se concentre sur des objets à seulement quelques centimètres

Chaque objectif est conçu pour un usage différent.

Camera2 peut nous dire quels objectifs possède un téléphone. Plus tard dans cette série, nous apprendrons comment Android les identifie.

## Le capteur d'image

Derrière l'objectif se trouve le capteur d'image. C'est ici que la lumière devient de l'information numérique.

Des millions de pixels minuscules recouvrent la surface du capteur. Chaque pixel mesure la quantité de lumière qui l'atteint. Plus la lumière est vive, plus le signal électrique généré est important.

La caméra convertit ensuite ces signaux électriques en valeurs numériques. C'est l'image "raw" produite par le capteur.

**Un fait important :** Le capteur d'image capture la lumière, pas la couleur. Nous allons expliquer pourquoi bientôt.

### Pourquoi les capteurs plus grands produisent généralement de meilleures photos

Les fabricants adorent annoncer les mégapixels. Vous avez peut-être vu des téléphones avec :

- 48 MP
- 64 MP
- 108 MP
- 200 MP

Mais les mégapixels ne sont qu'une partie de l'histoire.

Imaginez deux seaux collectant de la pluie. Un seau plus grand collecte plus d'eau qu'un plus petit.

Les pixels fonctionnent de la même manière. Les pixels plus grands collectent plus de lumière. Plus de lumière signifie généralement :

- **Moins de bruit d'image**
- **Meilleure performance en basse lumière**
- **Plus de dynamique**

C'est une raison pour laquelle les téléphones haut de gamme produisent souvent des images beaucoup meilleures que les téléphones budget, même quand ils annoncent des nombres de mégapixels similaires.

## L'ISP — Le héros caché

La plupart des gens n'ont jamais entendu parler de l'ISP. ISP signifie **Processeur de Signal d'Image** (Image Signal Processor). C'est l'un des composants les plus importants à l'intérieur d'un smartphone.

Pensez-y comme à l'éditeur de photos de la caméra. L'ISP reçoit les données brutes du capteur et effectue de nombreuses étapes de traitement, notamment :

- **Démosaïquage** — Reconstruire la couleur à partir de pixels individuels
- **Réduction de bruit** — Réduire le grain dans les photos
- **Balance des blancs** — Corriger la température de couleur
- **Ajustement d'exposition** — Éclaircir ou assombrir l'image
- **Netteté** — Améliorer les détails
- **Fusion HDR** — Combiner plusieurs expositions
- **Correction de couleur** — Ajuster les couleurs pour un aspect naturel
- **Correction de distorsion d'objectif** — Corriger la distorsion en baril ou en coussin

Sans l'ISP, les photos sembleraient souvent sombres, bruyantes et antinaturelles.

Dans de nombreuses situations, la qualité de l'image dépend autant de l'ISP que du capteur de caméra lui-même.

## Pourquoi les images RAW semblent étranges

Plus tôt, nous avons dit que le capteur capture la lumière, pas la couleur. Comment est-ce possible ?

Chaque pixel du capteur ne peut mesurer que l'intensité de la lumière incidente. Pour enregistrer les couleurs, la plupart des capteurs utilisent un **filtre de couleur Bayer**.

Chaque pixel enregistre une seule couleur :

- **Rouge**
- **Vert**
- **Bleu**

L'ISP combine les pixels voisins pour reconstruire une image en couleurs complètes. Ce processus s'appelle le **démosaïquage**.

Une image RAW est capturée avant que la plupart de ce traitement ne se produise. C'est pourquoi les photos RAW apparaissent souvent plates, plus sombres et moins colorées que les images JPEG. Le logiciel de montage professionnel effectue le traitement restant plus tard.

## Plusieurs caméras deviennent standard

Beaucoup de téléphones contiennent maintenant plusieurs caméras. Par exemple :

| Caméra | Usage typique |
| --- | --- |
| **Grand angle** | Photographie quotidienne |
| **Ultra-grand angle** | Paysages et architecture |
| **Téléphoto** | Zoom et portraits |
| **Macro** | Photographie de près |
| **Profondeur** | Estimation de profondeur |

Chaque caméra a ses propres :

- **Objectif**
- **Capteur**
- **Caractéristiques**
- **Capacités**

Android Camera2 traite chaque caméra comme un appareil séparé. Nous verrons cela dans les chapitres suivants lorsque nous explorerons les ID de caméra.

## Comment une photo est créée

Maintenant, mettons tout ensemble. Lorsque vous appuyez sur le bouton de déclenchement :

1. **La lumière entre dans l'objectif**
2. **L'objectif focalise la lumière sur le capteur**
3. **Le capteur convertit la lumière en signaux électriques**
4. **L'ISP traite les données brutes**
5. **Android reçoit l'image traitée**
6. **Votre application affiche ou enregistre le résultat**

Bien que tout ce processus dure généralement moins d'une seconde, de nombreuses opérations complexes se produisent en arrière-plan.

## Ce que Camera2 peut contrôler

Toutes les parties du pipeline de la caméra ne sont pas contrôlées par Android. Cependant, Camera2 permet aux applications d'influencer de nombreux paramètres importants. Par exemple :

- **Exposition** — Durée de collecte de la lumière par le capteur
- **ISO** — Sensibilité du capteur
- **Focalisation** — Où la caméra se focalise
- **Balance des blancs** — Ajustement de la température de couleur
- **Flash** — Contrôle du flash
- **Zoom** — Zoom numérique et optique
- **Vitesse d'images** — Vitesses d'images vidéo
- **Format d'image** — JPEG, RAW, YUV
- **Résolution de sortie** — Dimensions de l'image

Tout au long de cette série, nous apprendrons comment ces paramètres affectent la qualité de l'image.

## Explorer avec Android Camera Parameters

Avant d'écrire du code, essayez d'explorer votre propre téléphone. Ouvrez Android Camera Parameters et recherchez :

- **ID de caméra** — Comment Android identifie chaque caméra
- **Orientation de l'objectif** — Avant, arrière ou externe
- **Taille du capteur** — Dimensions physiques
- **Distances focales disponibles** — Différents objectifs
- **Niveau de support matériel** — LEGACY, LIMITED, FULL ou LEVEL_3
- **Zoom numérique maximal** — Capacités de zoom
- **Tailles de sortie prises en charge** — Résolutions disponibles

Ne vous inquiétez pas si certains de ces termes vous sont inconnus. À la fin de ce livre, vous les comprendrez tous.

## Prochain chapitre

Dans le prochain chapitre, nous répondrons à une autre question importante :

> Pourquoi différents téléphones Android prennent-ils en charge différentes fonctionnalités de caméra ?

Vous apprendrez :

- **Niveaux de matériel de caméra** — LEGACY, LIMITED, FULL, LEVEL_3
- **Fonctionnalités optionnelles** — Ce qui peut ou non être disponible
- **Capacités de l'appareil** — Vérifier ce que la caméra prend en charge
- **Pourquoi certains téléphones prennent-ils en charge le RAW et d'autres non ?**
- **Pourquoi Camera2 se comporte-t-il différemment selon les appareils ?**

Ces connaissances vous aideront à comprendre pourquoi les applications Camera2 doivent toujours vérifier les capacités de la caméra plutôt que de faire des suppositions.

## Résumé

Une caméra de smartphone est bien plus qu'un objectif. C'est un système d'imagerie sophistiqué composé d'objectifs, de capteurs, de processeurs d'image, de mémoire et de logiciels qui travaillent ensemble pour produire chaque photographie.

L'API Camera2 donne aux développeurs accès à de nombreuses parties de ce système, mais comprendre le matériel d'abord rend le logiciel beaucoup plus facile à apprendre.

Maintenant que vous savez comment une caméra de smartphone crée une image, vous êtes prêt à découvrir pourquoi différents appareils Android exposent différentes capacités de caméra.