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

## Fiches (v0.10)

- Blocs **Naissance, Mariage, Décès** toujours présents ; blocs **Baptême et Sépulture créables à la demande** (bouton « + Baptême » / « + Sépulture »).
- Chaque bloc : date, **heure**, lieu (commune), **département** séparés, et **un seul bouton Vérifié Oui/Non** (Non par défaut, Oui automatiquement dès qu'une date ou une image est renseignée).
- **Actes (images) dans chaque bloc** : ajout via le séélecteur de photos, chiffrés sur l'appareil, **page x / total** mémorisée, visionneuse avec **zoom pincement et déplacement**.
- **Lien direct vers l'état civil des AD** depuis chaque acte, selon la commune, le département, le type d'acte et la date.
- **Appui long sur un bloc** : modification directe de la fiche.
- Famille : père, mère, conjoint(e) + **autres conjoints**, tous les enfants ; création d'une nouvelle personne possible directement depuis l'ajout d'un enfant.

## Journal de recherches (v0.10)

- **5 recherches automatiques** (dédupliquées, déduites des données saisies) + **5 recherches de libre saisie**, ouverture sur le journal au démarrage.
- Chaque recherche affiche **3 blocs** :
  - **a. Rappel de la recherche à faire** (texte recalculé en direct, ex : « Rechercher son père Georges MARTIN entre 1857 et 1897 à Roubaix (59) ») ;
  - **b. Recherche en cours** avec bouton **Valider l'hypothèse** (horodate le point et le déplace en archives) ;
  - **c. Recherches archivées** horodatées, URLs cliquables.
- Validation d'une recherche **avec confirmation** : elle s'efface du journal et une nouvelle apparaît. Les 3 blocs se synchronisent avec la fiche de la personne.
- Onglet PERSONNES : vignettes **bleu pâle (hommes) / rose pâle (femmes)** affichant le **n° Sosa ou d'Aboville**, **nom de famille en premier**, dates N/M/D, cartes compactes et **index alphabétique** en haut.

## Arbre

3 g\u00E9n\u00E9rations d'ascendance + conjoint + enfants, cartes cliquables avec statut vert/rouge, conjoints multiples g\u00E9r\u00E9s.

## GEDCOM (v0.10)

- **Bouton GEDCOM à droite** de la barre de titre : **exporter (tout ou une branche), importer, vider la base**.
- Export **5.5.1** (N/BAPM/M/DEAT/BURI + heures TIME), import depuis Heredis avec confirmation, plafond 100 personnes, compteur affiché en permanence.

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
9. \u2705 Refonte v0.9 (liste) : palette blanc/verts, logo, tri alphab\u00E9tique, de cujus \u00E0 l'ouverture, appui long, conjoints multiples, 5 recherches num\u00E9rot\u00E9es en 4 blocs.
10. \u2705 Refonte v0.10 : d\u00E9marrage sur le journal, bouton GEDCOM (export/import/vider), vignettes bleu/rose avec n\u00B0 Sosa/d'Aboville, index alphab\u00E9tique, fiche sans bloc Identit\u00E9, blocs B et S \u00E0 la demande, bouton V\u00E9rifi\u00E9 unique par bloc, journal 3 blocs + correctif doublons.
