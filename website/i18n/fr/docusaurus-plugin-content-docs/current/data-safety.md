---
sidebar_position: 3
description: Pratiques de sécurité des données et de confidentialité pour l'application Android Camera Parameters. Découvrez les autorisations de la caméra et la gestion des métadonnées matérielles.
keywords: [sécurité des données, politique de confidentialité, autorisations android]
---

# Guide de sécurité des données

Ce document décrit les pratiques de collecte de données et de confidentialité de l'application Android Camera Parameters.

## Aperçu

Android Camera Parameters est conçu avec une approche "La vie privée d'abord". En tant qu'outil de diagnostic, il doit accéder aux informations matérielles pour fonctionner, mais il ne collecte ni ne transmet de données personnelles.

## Autorisations

### Autorisation de la caméra (`android.permission.CAMERA`)
- **Exigence** : Nécessaire pour accéder au `CameraManager` et récupérer les `CameraCharacteristics`.
- **Utilisation** : L'application lit uniquement les métadonnées matérielles. Elle **n'enregistre pas** de vidéo et ne prend pas de photos sans action explicite de l'utilisateur (par exemple, dans les versions futures si des tests de capture d'image sont ajoutés).

## Collecte de données

- **Informations personnelles** : L'application **ne collecte pas** de noms, d'adresses e-mail, de numéros de téléphone ou tout autre identifiant personnel.
- **Données de localisation** : L'application **n'accède pas** à votre position GPS ou réseau.
- **Métadonnées matérielles** : L'application lit les spécifications techniques de vos objectifs de caméra (résolution, distance focale, modes supportés). Ces données restent sur votre appareil, sauf si vous utilisez explicitement la fonction "Exporter JSON" pour les partager.

## Partage de données

L'application **ne partage aucune donnée** avec des tiers. Aucun SDK de suivi (comme Firebase Analytics ou le SDK Facebook) n'est intégré au cœur de l'application.

## Contrôle de l'utilisateur

- **Exportation JSON** : Les utilisateurs peuvent choisir de copier ou de partager le JSON brut des paramètres de la caméra. Cette action est entièrement initiée par l'utilisateur.
- **Autorisations** : Vous pouvez révoquer l'autorisation de la caméra à tout moment via les paramètres du système Android, bien que l'application ne puisse pas afficher les détails de la caméra sans celle-ci.
