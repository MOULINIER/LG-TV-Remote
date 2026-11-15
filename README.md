# LG Remote — télécommande Android pour TV LG webOS

Application Android moderne (Kotlin + Jetpack Compose, **Material Design 3** avec couleurs dynamiques Material You, thème clair/sombre) pour piloter une TV LG webOS via le Wi-Fi.

<p>
<img src="docs/remote_dark.png" width="260"> <img src="docs/remote_light.png" width="260"> <img src="docs/connect_dark.png" width="260">
</p>

## Installation

Téléchargez **[`apk/LG-Remote-1.0.apk`](apk/LG-Remote-1.0.apk)** sur le téléphone, ouvrez-le et autorisez l'installation d'applications de sources inconnues. Android 8.0 ou plus récent est requis.

## Fonctionnalités

- **Détection automatique** des TV sur le réseau (SSDP), ou saisie manuelle de l'adresse IP
- **Appairage** sécurisé : la TV demande l'autorisation la première fois, puis la clé est mémorisée et la reconnexion est automatique
- **Croix directionnelle + OK**, Retour, Accueil, Réglages, avec appui long qui répète la commande
- **Pavé tactile** pour piloter le pointeur (Magic Remote) et une bande de défilement
- **Volume / Chaînes** (bascules), Muet, Infos, et les **touches volume du téléphone** qui contrôlent la TV
- **Lecture multimédia** : lecture, pause, stop, retour et avance rapides
- **Applications** : grille des applis installées avec leurs icônes, lancement en un tap
- **Sources** : changement d'entrée HDMI / TV
- **Clavier** : saisie de texte directement dans les champs de la TV
- **Pavé numérique** + touches de couleur, menu rapide, liste des chaînes
- **Extinction**, et **allumage par Wake-on-LAN** (adresse MAC détectée automatiquement, modifiable dans les réglages)
- Retour haptique sur chaque touche

## Prérequis côté TV

- Téléphone et TV sur le même réseau
- Pour l'allumage à distance, activez sur la TV : *Paramètres → Général → Appareils → Allumer via Wi-Fi* (ou *Mobile TV On*). L'intitulé varie selon la version de webOS.

## Compilation

```bash
./gradlew assembleRelease          # → app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest -Proborazzi.test.record=true   # régénère les captures dans docs/
```

L'APK de release est signé avec la clé de debug pour pouvoir l'installer directement. Pour une publication sur le Play Store, configurez votre propre `signingConfig`.

## Technique

Protocole LG SSAP sur WebSocket (`wss://<tv>:3001`, repli sur `ws://<tv>:3000`). Les boutons passent par le socket d'entrée du pointeur (`getPointerInputSocket`). Code principal : `app/src/main/java/com/moulinier/lgremote/tv/WebOsClient.kt`.
