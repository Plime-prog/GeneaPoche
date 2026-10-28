# G\u00E9n\u00E9aPoche

Application Android de g\u00E9n\u00E9alogie de poche : recherches g\u00E9n\u00E9alogiques en France (actes N/B/M/D/S), sans rien oublier.

## Objectif

Un outil l\u00E9ger pour :
- saisir au fil de l'eau des personnes (p\u00E8re, m\u00E8re, \u00E9poux/\u00E9pouse, **tous** les enfants) ;
- savoir quelles futures recherches faire (journal de recherches intelligent) ;
- stocker captures d'\u00E9cran et photos d'actes **dans chaque bloc d'\u00E9v\u00E9nement** ;
- acc\u00E9der directement \u00E0 l'\u00E9tat civil des Archives d\u00E9partementales depuis chaque acte ;
- suivre ce qui est v\u00E9rifi\u00E9 (pastilles vertes) ou \u00E0 v\u00E9rifier (rouge doux) ;
- **exporter en GEDCOM 5.5.1** pour alimenter Heredis, et importer depuis Heredis.

P\u00E9rim\u00E8tre : 100 personnes max (compteur affich\u00E9), France uniquement, actes N/B/M/D/S.

## S\u00E9curit\u00E9

- **Aucune permission** dans le manifeste : pas d'Internet, aucune donn\u00E9e ne peut \u00EAtre transmise.
- Stockage local **chiffr\u00E9 AES-256/GCM**, cl\u00E9 dans AndroidKeyStore (jamais exportable), sauvegarde v\u00E9rifi\u00E9e avec copie `.bak`.
- **Code PIN** : PBKDF2-HmacSHA256 (150 000 it\u00E9rations, sel al\u00E9atoire), blocage progressif (5 erreurs \u2192 30 s / 2 min / 10 min / 1 h). Touche Entr\u00E9e + bouton D\u00E9verrouiller.
- **FLAG_SECURE** : captures d'\u00E9cran et aper\u00E7u dans les applications r\u00E9centes interdits.
- Reverrouillage automatique apr\u00E8s plus de 60 secondes en arri\u00E8re-plan.
- \u26A0\uFE0F Sans le code PIN, les donn\u00E9es sont ind\u00E9chiffrables : aucune r\u00E9cup\u00E9ration possible.

## Num\u00E9rotation

- **Sosa-Stradonitz** : 1 = de cujus (personne de r\u00E9f\u00E9rence), p\u00E8re = 2n, m\u00E8re = 2n+1. Le de cujus s'affiche \u00E0 l'ouverture ; on peut le changer, toute la num\u00E9rotation est recalcul\u00E9e automatiquement.
- **d'Aboville** pour les lat\u00E9raux (descendants hors ligne directe) : pr\u00E9rempli quand on ajoute un enfant (ex : enfant n\u00B02 du Sosa 1 \u2192 1.2).
- Badges Sosa / Lat\u00E9ral affich\u00E9s dans la liste alphab\u00E9tique.

## Fiches compl\u00E8tes (v0.9)

- Blocs **Identit\u00E9, Naissance, Bapt\u00EAme, Mariage, D\u00E9c\u00E8s, S\u00E9pulture** : date, **heure**, lieu (commune), **d\u00E9partement** s\u00E9par\u00E9s, pastilles V\u00E9rifi\u00E9 / \u00C0 v\u00E9rifier (couleurs douces).
- **Appui long sur un bloc** : modification directe de la fiche.
- **Actes (images) dans chaque bloc** : ajout via le s\u00E9lecteur de photos, chiffr\u00E9s sur l'appareil, **page x / total** m\u00E9moris\u00E9e, visionneuse avec **zoom pincement et d\u00E9placement**.
- **Lien direct vers l'\u00E9tat civil des AD** depuis chaque acte, selon la commune, le d\u00E9partement, le type d'acte et la date.
- Famille : p\u00E8re, m\u00E8re, conjoint(e) + **autres conjoints**, tous les enfants ; cr\u00E9ation d'une nouvelle personne possible directement depuis l'ajout d'un enfant.

## Journal de recherches (v0.9)

- **Maximum 5 recherches en cours**, num\u00E9rot\u00E9es 1 \u00E0 5, en **tournus entre les personnes** pour varier les plaisirs.
- Chaque recherche affiche 4 blocs :
  - **a. Recherche \u00E0 faire** (le texte, recalcul\u00E9 en direct avec les donn\u00E9es actuelles) ;
  - **b. Rappels horodat\u00E9s** de ce qui a \u00E9t\u00E9 fait (\u00AB rien trouv\u00E9, jusqu'\u00E0 la page 230, TD de Roubaix, reprendre \u00E0 partir de 1887 \u00BB) ;
  - **c. Recherche en cours** avec bascule vers les rappels (vide le bloc) ;
  - **d. Reste \u00E0 rechercher** pour la personne concern\u00E9e.
- **Recherches intelligentes** d\u00E9duites des donn\u00E9es : acte manquant ou non v\u00E9rifi\u00E9, **p\u00E8re n\u00E9 entre n-45 et n-20, m\u00E8re n\u00E9e entre n-40 et n-15**, enfants apr\u00E8s le mariage, grands-parents, avec **lieu g\u00E9ographique pressenti**.
- Validation d'une recherche **avec confirmation** : elle passe aux rappels et une nouvelle appara\u00E0t. Les **URLs sont cliquables**.

## Arbre

3 g\u00E9n\u00E9rations d'ascendance + conjoint + enfants, cartes cliquables avec statut vert/rouge, conjoints multiples g\u00E9r\u00E9s.

## GEDCOM (v0.9)

- Export **5.5.1** : toutes les personnes ou **une branche compl\u00E8te** au choix (N/BAPM/M/DEAT/BURI + heures TIME).
- **Import depuis Heredis** (et autres logiciels) avec confirmation, plafond 100 personnes.
- Compteur de personnes affich\u00E9 en permanence.

## Installation

1. Onglet **Actions** \u2192 dernier run vert \u2192 artefact **geneapoche-apk**.
2. D\u00E9compresser le ZIP, installer `app-debug.apk` sur le t\u00E9l\u00E9phone (autoriser les sources inconnues).

## D\u00E9veloppement

- Android Kotlin + Jetpack Compose, Material 3 (th\u00E8me blanc / vert p\u00E2le / vert fonc\u00E9).
- AGP 9.2.0, Gradle 9.4.1, Kotlin 2.4.21, compileSdk 37, targetSdk 36.
- Chaque push compile automatiquement l'APK debug via GitHub Actions.

## Feuille de route

1. \u2705 Squelette : fiches personnes + stockage local chiffr\u00E9.
2. \u2705 Fiche compl\u00E8te : champs s\u00E9par\u00E9s date/heure/lieu/d\u00E9partement, Sosa + d'Aboville automatiques.
3. \u2705 S\u00E9curit\u00E9 (PIN, chiffrement, FLAG_SECURE) + pastilles vertes/rouges.
4. \u2705 Journal de recherches intelligent.
5. \u2705 Stockage d'actes chiffr\u00E9s avec zoom, dans chaque bloc N/B/M/D/S.
6. \u2705 Liens directs \u00E9tat civil des Archives d\u00E9partementales depuis chaque acte.
7. \u2705 Arbre simple.
8. \u2705 Export GEDCOM 5.5.1 (tout ou branche) + import Heredis.
9. \u2705 Refonte v0.9 : palette blanc/verts, logo, tri alphab\u00E9tique, de cujus \u00E0 l'ouverture, appui long, conjoints multiples, 5 recherches num\u00E9rot\u00E9es en 4 blocs.
