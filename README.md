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

P\u00E9rim\u00E8tre : 50 personnes max (compteur affich\u00E9), France uniquement, actes N/B/M/D/S.

## S\u00E9curit\u00E9

- **Aucune permission** dans le manifeste : pas d'Internet, aucune donn\u00E9e ne peut \u00EAtre transmise.
- Stockage local **chiffr\u00E9 AES-256/GCM**, cl\u00E9 dans AndroidKeyStore (jamais exportable), sauvegarde v\u00E9rifi\u00E9e avec copie `.bak`.
- **Code PIN** : PBKDF2-HmacSHA256 (150 000 it\u00E9rations, sel al\u00E9atoire), blocage progressif (5 erreurs \u2192 30 s / 2 min / 10 min / 1 h). Touche Entr\u00E9e + bouton D\u00E9verrouiller.
- **FLAG_SECURE** : captures d'\u00E9cran et aper\u00E7u dans les applications r\u00E9centes interdits.
- Reverrouillage automatique apr\u00E8s plus de 60 secondes en arri\u00E8re-plan.
- \u26A0\uFE0F Sans le code PIN, les donn\u00E9es sont ind\u00E9chiffrables : aucune r\u00E9cup\u00E9ration possible.

## Numérotation (v0.11)

- **Sosa-Stradonitz à partir du de cujus, dont le n° n'est pas forcément 1** : menu GEDCOM → « Définir le de cujus et son n° Sosa » (choix de la personne + numéro). Père = 2n, mère = 2n+1 : **tout en découle automatiquement**.
- **d'Aboville** pour les latéraux : préfixe = n° du cujus (ex : enfant n° 2 d'un cujus Sosa 7 → 7.2).
- **Aucun numéro ne se saisit nulle part** : plus de champ Sosa ou d'Aboville dans l'édition, tout est recalculé au fil de l'eau (liens père/mère/enfants inclus).

## Fiches (v0.11)

- Blocs **Naissance, Mariage, Décès** toujours présents ; blocs **Baptême et Sépulture créables à la demande** (bouton « + Baptême » / « + Sépulture »).
- Chaque bloc : date, **heure**, lieu (commune), **département** séparés, et **un seul bouton Vérifié Oui/Non** (Non par défaut, Oui automatiquement dès qu'une date ou une image est renseignée).
- **Actes (images) dans chaque bloc** : ajout via le sélecteur de photos, chiffrés sur l'appareil, **page x / total** mémorisée, visionneuse avec **zoom pincement et déplacement**.
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

## Fiche et journal synchronisés (v0.15)

- **Fiche personne par blocs modifiables au toucher long** : carte identité (nom, prénoms, profession dessous), puis blocs Naissance / Baptême / Mariage / Décès / Sépulture.
- Chaque bloc **ramasse tout** : date, heure, lieu, département, **père et mère** (profession, naissance, **âge à l'acte** ou date de naissance déduite à l'année), **conjoint** pour le mariage, **témoins et signataires**.
- **Informations manquantes affichées en gris** (—) : elles sont **synchronisées avec le journal de recherches**.
- Dans chaque bloc : **icône photo** pour ajouter une photo d'acte et **icône recherche** vers l'état civil des AD.
- **Nouveau nom saisi** : synchronisation avec la fiche existante, sinon **proposition de création** (numérotation déduite automatiquement).
- **Journal exhaustif (v0.16)** : **une recherche par information manquante** (date, heure, lieu, département, profession, sexe, père, mère, conjoint, enfants, copie d'acte à photographier), regroupé par personne avec compteurs ; dès qu'une donnée est saisie ou une image ajoutée, **la recherche est archivée automatiquement** (horodatée). Chaque recherche garde une **zone de texte libre** (pages, côtes, notes) et l'icône de recherche Internet.

## Saisie par acte (v0.14)

Nouvel onglet **Saisie** (bouton + en bas) : transcription directe d'un acte d'état civil.
- **Type N/B/M/D/S** en puces, commune + département, date et heure.
- **Personne concernée** choisie parmi les existantes ou nouvelle (nom, prénoms, sexe, profession).
- **Père ET mère** : naissance (âge à l'acte calculé automatiquement), lieu et département de naissance, profession, décès (date et lieu).
- **Mariage : la mariée et ses propres père et mère** avec les mêmes champs.
- **Jusqu'à 4 témoins** (nom, prénoms, profession) et **puces « Qui signe ? »** (concerné(e), père, mère, mariée, témoins).
- **Tout alimente la fiche Personne** (dates, heures, lieux, départements, professions, liens parents) et la **numérotation Sosa/d'Aboville est recalculée immédiatement** ; les personnes absentes sont créées automatiquement (plafond 50 respecté).
- La transcription intégrale (entête, âges, parents, témoins, signataires) est **mémorisée dans l'acte** et affichée dans la visionneuse en l'absence d'image.

## Sources Heredis et coh\u00E9rence (v0.17)

- **R\u00E9f\u00E9rence (c\u00F4te) de chaque acte** : champ \u00AB R\u00E9f\u00E9rence de l'acte (c\u00F4te AD) \u00BB dans l'onglet Saisie et dans le dialogue Page de chaque acte, affich\u00E9e sur la fiche dans chaque bloc.
- **Export GEDCOM fa\u00E7on Heredis** : chaque acte devient un enregistrement **SOUR** (TITL, **REFN c\u00F4te**, DATA/EVEN, DATE, PLAC, NOTE transcription int\u00E9grale) ; les \u00E9v\u00E9nements et mariages y renvoient via `SOUR @S\u2026@` + `PAGE p. x/y`.
- **Lieux normalis\u00E9s** : `PLAC` au format \u00AB commune, num\u00E9ro, d\u00E9partement \u00BB (ex : Roubaix, 59, Nord), le num\u00E9ro \u00E9tant d\u00E9duit du champ d\u00E9partement, du lieu ou du nom de d\u00E9partement.
- **Contr\u00F4le de coh\u00E9rence** : encadr\u00E9 orange en t\u00EAte du journal signalant les incoh\u00E9rences d'ann\u00E9es (d\u00E9c\u00E8s ou mariage avant naissance, \u00E2ge au d\u00E9c\u00E8s > 110 ans, p\u00E8re > 80 ans ou d\u00E9c\u00E9d\u00E9 avant la naissance, m\u00E8re < 13 ou > 55 ans, conjoint mari\u00E9 avant sa naissance).

## Num\u00E9rotation et affichage (v0.18)

- **Num\u00E9rotation imm\u00E9diate et compl\u00E8te** : plus aucun num\u00E9ro n'est affect\u00E9 \u00E0 la main ; Sosa-Stradonitz (p\u00E8re = 2n, m\u00E8re = 2n+1) puis d'Aboville pour **tous les enfants non-Sosa de toute personne num\u00E9rot\u00E9e** (fr\u00E8res, s\u0153urs, oncles, tantes, cousins : pr\u00E9fixe du parent + .rang de naissance). Tout rattachement (p\u00E8re, m\u00E8re, enfant) met tous les num\u00E9ros \u00E0 jour imm\u00E9diatement.
- **Pastille corrig\u00E9e** : le rond affiche le num\u00E9ro brut (plus d'\u00E9tiquette \u00AB Lat\u00E9ral \u00BB), avec une taille de police adapt\u00E9e aux num\u00E9ros d'Aboville longs.
- **Encodage UTF-8 robuste** : tout le code est ASCII pur (accents en s\u00E9quences d'\u00E9chappement \u00E9chap\u00E9es) \u2014 plus aucun \u00E9 mojibake possible.

## Fiche personne refondue (v0.19)

- **Titre identique \u00E0 la liste** : pastille au num\u00E9ro Sosa/d'Aboville, couleur bleu (homme) ou rose (femme), vert p\u00E2le si sexe absent, nom et pr\u00E9noms.
- **Blocs resserr\u00E9s**, **Naissance et D\u00E9c\u00E8s seulement par d\u00E9faut** ; bouton **BMS** (+ Bapt\u00EAme, + Mariage, + S\u00E9pulture) pour ouvrir les autres blocs.
- **Naissance : p\u00E8re et m\u00E8re** avec \u00E2ge \u00E0 l'acte ; **D\u00E9c\u00E8s : sans les parents**.
- **Saisir un mariage / Saisir un enfant** depuis la carte Famille : contr\u00F4le **anti-doublon** (nom + pr\u00E9noms, insensible \u00E0 la casse), **synchronisation avec la fiche existante** au lieu d'une cr\u00E9ation, autres mariages et enfants illimit\u00E9s ; idem pour la cr\u00E9ation d'une personne.
- **Boutons Supprimer, Annuler, Enregistrer** dans le dialogue d'\u00E9dition.

## Fiche personne v2 (v0.20)

- **Cartouche identité** : pastille au numéro (bleu/rose, vert si sexe absent), nom et prénoms, profession ; **rappel du père et de la mère avec leurs dates N. et D.** (cliquables) ; plus de « Sosa x » redondant ; « Définir comme de cujus » si besoin.
- **Frise chronologique de haut en bas** : tous les événements triés par année, **N et D par défaut** ; toucher un événement ouvre/ferme son bloc ; bouton **BMS** pour Baptême, Mariage, Sépulture.
- **Édition par bloc uniquement** (plus de crayon global) : icône crayon dans chaque bloc → date, heure, commune, département, référence (côte AD) ; boutons Enregistrer / Annuler.
- **Date et heure sur la même ligne**, « Commune » avec le **numéro de département accolé** (ex : Roubaix (59)).
- **Naissance : parents choisis** parmi les fiches existantes ou créés (anti-doublon, synchronisation) ; **coche « Signe »** derrière le père.
- **Mariage** : conjoint + autres conjoints + enfants, chacun **choisi dans la base ou créé** (aucun doublon).
- **Témoins** : aucun par défaut ; on saisit le nom, les **fiches existantes correspondantes sont proposées d'abord**, la création n'est proposée que si personne ne correspond ; **coche de signature** derrière chaque témoin.
- **Chaque validation resynchronise tout** : numéros Sosa/d'Aboville, contrôle de cohérence, journal de recherches.

## Frise légère et fiches enfants (v0.21)

- **« Définir comme de cujus » retiré du cartouche** : la fiche reste épurée de haut en bas (cartouche identité → frise).
- **Frise sans titre** : dans l'encadré, **une seule ligne par événement avec l'essentiel** — « N. 12/03/1857 à Roubaix (59) », « D. … » (gris « — » si absent). **Toucher la ligne ouvre/ferme le bloc** correspondant ; **crayon en bout de ligne** = dialogue d'édition (date, heure, commune, département, côte AD).
- **Boutons à police réduite** (Baptême / Mariage / Sépulture / enfants).
- **Boutons Enfant ajoutés à la frise** : « Choisir un enfant » (fiche existante, sans doublon) et « Saisir un enfant » ; la gestion des enfants quitte le bloc Mariage.
- **Un bloc par enfant = la fiche résumée de l'enfant** : pastille au numéro, nom, « N. … à commune (département) », « D. … » ; toucher ouvre sa fiche, croix pour le détacher.
- Chaque validation conserve **numéro, synchronisation, cohérence et anti-doublon**.

## Arbre

3 g\u00E9n\u00E9rations d'ascendance + conjoint + enfants, cartes cliquables avec statut vert/rouge, conjoints multiples g\u00E9r\u00E9s.

## GEDCOM (v0.11)

- **Bouton GEDCOM à droite** de la barre de titre, menu avec **icônes** : Exporter (tout ou une branche), Importer (remplace la base, plafond 50), **Définir le de cujus et son n° Sosa**, Vider la base.
- Export **5.5.1** (N/BAPM/M/DEAT/BURI + heures TIME), import depuis Heredis avec confirmation, compteur affiché en permanence.

## Personnes (v0.11)

- **Alphabet complet A–Z en haut** : les lettres sans personne sont **grisées** ; un appui fait défiler jusqu'à la lettre.
- **Classement au choix A–Z ou par branche** (puces de bascule) : chaque **branche** porte le **nom de famille de son ancêtre le plus haut** — les 4 branches des grands-parents, 3 générations, 7 Sosa + les latéraux.
- **Tout l'encadré rose pâle (femme) ou bleu pâle (homme)**, avec le **rond au n° Sosa ou d'Aboville calculé automatiquement**, nom de famille en premier, dates N./M./D.

## Installation

1. Onglet **Actions** \u2192 dernier run vert \u2192 artefact **geneapoche-apk**.
2. D\u00E9compresser le ZIP, installer `app-debug.apk` sur le t\u00E9l\u00E9phone (autoriser les sources inconnues).

## D\u00E9veloppement

- Android Kotlin + Jetpack Compose, Material 3 (th\u00E8me blanc / vert p\u00E2le / vert fonc\u00E9, palette compl\u00E8te : aucune surface Material3 violette, encadr\u00E9s verts sur fond blanc).
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
11. \u2705 Refonte v0.11 : plafond 50, de cujus au n\u00B0 Sosa libre (tout d\u00E9coule, aucune ressaisie), palette sans violet, menu GEDCOM \u00E0 ic\u00F4nes, alphabet complet grise, encadr\u00E9s rose/bleu, classement par branche.
12. \u2705 Refonte v0.12 : fin du violet Material3 (surfaces container surcharg\u00E9es : barre d'onglets du bas, dialogues, cartes), encadr\u00E9s verts sur fond blanc pour la fiche, le journal et l'arbre.
13. \u2705 Refonte v0.13 : num\u00E9rotation recalcul\u00E9e \u00E0 chaque enregistrement (n\u00B0 mis \u00E0 jour d\u00E8s qu'on rattache quelqu'un), dates jj/mm/aaaa et heures 14h05 partout, alphabet resserr\u00E9 sur une ligne, ic\u00F4nes du menu GEDCOM align\u00E9es \u00E0 gauche, arbre bleu/rose selon le sexe.
14. \u2705 v0.14 : saisie par acte (onglet Saisie) \u2014 type N/B/M/D/S, commune, date/heure, personne, p\u00E8re et m\u00E8re avec \u00E2ge calcul\u00E9, mari\u00E9e + ses parents, 4 t\u00E9moins, signataires ; alimente la fiche et la num\u00E9rotation, transcription m\u00E9moris\u00E9e dans l'acte.
15. \u2705 v0.15 : fiche par blocs complets (parents, t\u00E9moins, \u00E2ges), infos manquantes gris\u00E9es, ic\u00F4nes photo/AD, journal sans validation (archivage automatique).
16. \u2705 v0.16 : journal exhaustif \u2014 une recherche par information manquante, regroupement par personne avec compteurs.
17. \u2705 v0.17 : sources Heredis (c\u00F4te REFN, enregistrements SOUR avec transcription), lieux PLAC normalis\u00E9s \u00AB commune, num\u00E9ro, d\u00E9partement \u00BB, contr\u00F4le de coh\u00E9rence (encadr\u00E9 anomalies dans le journal).
18. \u2705 v0.18 : num\u00E9rotation imm\u00E9diate (d'Aboville pour tous les enfants non-Sosa), pastille corrig\u00E9e, suppression de l'\u00E9tiquette \u00AB Lat\u00E9ral \u00BB, encodage UTF-8 v\u00E9rifi\u00E9 sur toute l'application.
19. \u2705 v0.19 : fiche refondue \u2014 pastille et titre comme la liste, blocs N/D par d\u00E9faut + bouton BMS, naissance avec parents et d\u00E9c\u00E8s sans, saisie mariage/enfant anti-doublon avec synchronisation, boutons Supprimer/Annuler/Enregistrer.
20. ✅ v0.20 : fiche personne v2 — cartouche identité avec parents (N./D.), frise chronologique (N/D par défaut, BMS), édition par bloc avec dialogue dédié, témoins choisis/créés sans doublon avec coche de signature, date+heure sur une ligne, commune avec département accolé.
21. ✅ v0.21 : frise légère (essentiel N/D par ligne, crayon d'édition par ligne), cartouche sans cujus, polices de boutons réduites, boutons Enfant dans la frise, bloc-fiche par enfant.
