---
sidebar_position: 3
---

# Guide de Sécurité des Données

Ce document décrit les pratiques de collecte de données et de confidentialité de l'application Android Camera Parameters.

## Aperçu

Android Camera Parameters est conçu avec une approche "La confidentialité avant tout". En tant qu'outil de diagnostic, il doit accéder aux informations matérielles pour fonctionner, mais il ne collecte ni ne transmet de données personnelles.

## Autorisations

### Autorisation de Caméra (`android.permission.CAMERA`)
- **Exigence** : Nécessaire pour accéder au `CameraManager` et récupérer les `CameraCharacteristics`.
- **Utilisation** : L'application lit uniquement les métadonnées matérielles. Elle **n'enregistre pas** de vidéo et ne prend pas de photos sans action explicite de l'utilisateur (par exemple, dans les versions futures si des tests de capture d'image sont ajoutés).

## Collecte de Données

- **Informations Personnelles** : L'application **ne collecte pas** de noms, d'adresses e-mail, de numéros de téléphone ou tout autre identifiant personnel.
- **Données de Localisation** : L'application **n'accède pas** à votre GPS ou à votre localisation réseau.
- **Métadonnées Matérielles** : L'application lit les spécifications techniques de vos objectifs de caméra (résolution, distance focale, modes pris en charge). Ces données restent sur votre appareil, sauf si vous utilisez explicitement la fonction "Exporter JSON" pour les partager.

## Partage des Données

L'application **ne partage aucune donnée** avec des tiers. Aucun SDK de suivi (comme Firebase Analytics ou Facebook SDK) n'est intégré à l'application principale.

## Contrôle de l'Utilisateur

- **Exportation JSON** : Les utilisateurs peuvent choisir de copier ou de partager le JSON des paramètres bruts de la caméra. Ceci est entièrement initié par l'utilisateur.
- **Autorisations** : Vous pouvez révoquer l'autorisation de caméra à tout moment via les paramètres du système Android, bien que l'application ne puisse pas afficher les détails de la caméra sans celle-ci.
