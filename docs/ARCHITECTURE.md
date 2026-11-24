# Architecture de GénéaPoche

*Mise à jour : 24 novembre 2026 (v0.151). Document de référence pour les contributeurs ; l'historique fonctionnel est dans [CHANGELOG.md](../CHANGELOG.md).*

## Vue d'ensemble

Application Android mono-module (`:app`), Kotlin + Jetpack Compose (Material 3). Aucune permission Android, aucune bibliothèque réseau ou de mesure. Données stockées localement, chiffrées. Code dans `app/src/main/java/com/plime/geneapoche/` (41 fichiers, ~20 000 lignes) ; tests JVM dans `app/src/test/…` (12 fichiers, 315 tests).

## Couches

| Couche | Fichiers principaux | Rôle |
|---|---|---|
| Modèles | `Modeles.kt`, `ModeleVue.kt` | `Personne`, `Piste` (journal), événements ; sérialisation JSON champ par champ (pas de réflexion, donc pas de règle R8 spécifique) |
| Stockage et sécurité | `Stockage.kt`, `FichiersSurs.kt`, `EcranPin.kt`, `Sauvegarde.kt` | Clé maître AES-256 liée au PIN (enveloppe et fichiers atomiques dans `FichiersSurs.kt`, testés sur JVM) (PBKDF2 600 000 + AndroidKeyStore), écritures atomiques avec `.bak`/`.tmp`, blocage progressif, sauvegardes `.gpbak` |
| Logique pure (testée) | `LogiqueActe.kt`, `Coherence.kt`, `Numerotation.kt`, `Parente.kt`, `CalculDates.kt`, `CalendrierRepublicain.kt`, `Histoire.kt`, `Statistiques.kt`, `RechercheAvancee.kt`, `Biographie.kt`, `ListesPersonnes.kt`, `ActionsFiches.kt`, `ImportBase.kt`, `Chronologie.kt` (ordre de la chronologie), `RecherchesDuJour.kt` (les 5 du jour) | Règles métier sans dépendance Android : à privilégier pour tout nouveau code |
| Échange | `Export.kt`, `SourcesGedcom.kt` | GEDCOM 5.5.1 (compatible Heredis), balises `_GP*` pour ne rien perdre |
| Interface | `MainActivity.kt`, `DialoguesApp.kt`, `EcranApplication.kt`, `Fiches.kt`, `SaisieActe.kt`, `Journal.kt`, `SourcesUi.kt`, `Edition.kt`, `Outils*.kt`, `Arbre.kt`, `Frise.kt`, … | Écrans Compose ; 6 onglets Personnes / Journal / Arbre / Outils / Sources / Saisie |
| Données embarquées | `app/src/main/assets/communes/` (101 fichiers) | Communes et codes INSEE, hors ligne |

## Format des fichiers chiffrés

- **Stockage interne** : `[12 octets IV][données + tag GCM]`, clé maître en mémoire uniquement pendant le déverrouillage. Fichiers : `personnes.bin`, `journal.bin` (recherches), `sources.bin` (sources et citations), `actes.bin`, et depuis la v0.138 `notes_libres.bin` (bloc Notes de l'onglet Recherches ; inclus dans les sauvegardes `.gpbak` depuis la v0.139, clé `notes`).
- **Sauvegarde `.gpbak` / export `.gpged`** : `[6 octets magie "GPBAK1" ou "GPGED1"][16 octets sel][4 octets itérations, grand-boutiste][12 octets IV][données + tag GCM]`. Depuis la v0.108, les itérations de l'en-tête sont **réellement utilisées** au déchiffrement (bornes acceptées : 100 000 à 2 000 000) ; les nouveaux fichiers en portent 600 000.

## Construction et CI

- **Signature** : clé stable versionnée `app/geneapoche-signature.jks` (v0.140) pour debug et release ; secrets `KEYSTORE_*` prioritaires s'ils existent ; contrôle `apksigner` en CI ; détails et limites dans `docs/SIGNATURE.md`.
- AGP 9.5.1, Kotlin 2.4.21, compileSdk 37, targetSdk 36, minSdk 26, JDK 17, Gradle 9.8.2.
- Workflow `.github/workflows/apk.yml` : `lintDebug testDebugUnitTest assembleDebug assembleRelease`, artefacts debug et release ; actions épinglées par SHA.
- Le `gradle-wrapper.jar` n'est pas versionné : la CI le régénère (`gradle wrapper`). En local, lancer d'abord la même commande.
- **Release** : pousser une branche `release-X.Y` (ou un tag `vX.Y`) déclenche le job `release`, qui publie `GeneaPoche-vX.Y-release.apk` et `-debug.apk`. L'APK release est signé avec les secrets `KEYSTORE_*` s'ils existent, sinon avec la clé debug.
- Procédure de publication : mettre à jour `versionCode`/`versionName` dans `app/build.gradle.kts`, CHANGELOG, README ; PR vers `main` ; CI verte ; pousser `release-X.Y`.

## Conventions

Voir [CONTRIBUTING.md](../CONTRIBUTING.md). Rappels : jamais de permission, jamais de donnée réelle dans le dépôt, accents en `\uXXXX` dans les fichiers Kotlin.

## Performance : règles pratiques

- Ne jamais trier ni filtrer toute la base directement dans un composable : extraire une fonction pure (voir `ListesPersonnes.kt`), pré-calculer les clés de tri une fois par élément, et ne calculer que ce que l'affichage courant utilise (`by lazy`).
- Toute fonction de diagnostic qui compare chaque personne à toutes les autres doit être réécrite en collectant d'abord les identifiants dans un `HashSet` (O(n)).
- Un test d'équivalence avec l'ancienne implémentation doit accompagner toute optimisation.

## Découpage de MainActivity (en cours)

`AppEcran` (MainActivity.kt) concentre l'état et ≈ 55 fonctions d'actions locales (enregistrer, fusionner, importer, restaurer…) qui capturent cet état. Plan, par petites versions vérifiées en CI :
1. *(fait, v0.111)* dialogues autonomes → `DialoguesApp.kt` ;
2. *(fait, v0.112)* dialogues « de cujus » ;
3. actions → fonctions pures testables : *(fait, v0.113)* suppression avec Annuler et fusion de fiches (`ActionsFiches.kt`) ; *(fait, v0.114)* fusion d'un GEDCOM dans la base (`ImportBase.kt`) ; reste `remplacerBase`, la restauration depuis `.bak` et l'enregistrement d'acte ;
4. écrans (onglets) → fichiers dédiés recevant état et callbacks.
Règle : un déplacement mécanique par version, jamais de changement de logique en même temps.

> Note : la version de Gradle est fixée à **deux endroits** : `gradle/wrapper/gradle-wrapper.properties` et `.github/workflows/apk.yml` (la CI régénère le wrapper). Les changer ensemble.

> Build : Kotlin intégré à AGP (pas de plugin `kotlin.android`), nouveau DSL, `buildscript` épingle KGP/Compose 2.4.21 ; aucune option `android.*=false` de compatibilité (retirées en v0.125).

> Bilan d'étape complet (défauts trouvés, risques restants, actions du propriétaire) : [BILAN-2026-11-17.md](BILAN-2026-11-17.md).

## Ajouts v0.141
- `Navigation.kt` : pile d'écrans (codes d'onglet/fiche), `ajouterEcran`/`depilerEcran` ; `EcranApplication` unifie `retourArriere()` (flèche + BackHandler) ; dernier écran mémorisé via `Stockage.lireEcran/sauverEcran`.
- `RecherchesDuJour.kt` : `actionPourCategorie` (recherche → action sur la fiche, transmise par `actionInitiale` à `EcranPersonne`), `selectionDuJour` (sélection figée, mémorisée par `Stockage.lireAutresRecherches`).
- `ListesPersonnes.kt` : `pourSexe` ; `Fiches.kt` : `prefillActe` d'un mariage pour une femme = mariée + son conjoint marié.
- `EcranGeographie.kt` : communes du département touché, bloc des personnes replié ; `onOuvrirBio` ouvre la biographie.

## Ajouts v0.142
- `Eventail.kt` : demi-éventail dessiné en Canvas (anneaux de 2, 4, 8 secteurs = mêmes 3 générations d'ascendants que l'arbre) ; fonctions pures `ancetresEventail`, `secteurEventail` (détection du secteur touché), `anneesCourtes` ; toucher = centrer, appui long = fiche. Menu Arbre : Arbre / Ligne de vie / Éventail.
- `prefillActe` : le mariage ouvert depuis une femme place toujours la femme en mariée (même en modification) ; `autresRecherches` : nouveau tirage de 5 recherches.

## Ajouts v0.143
- Pile de retour : le code d'un écran contient maintenant le **sous-écran** ((onglet*100+sous)*10^10+fiche) ; sous-écrans hissés dans `EcranApplication` (`vueJournal`, `vueStat`, `outilOuvert`, `vueArbre`). `MemoireEcrans.kt` (`rememberMemo`) conserve département/commune/bloc déployé (Géographie) et la source ouverte (Sources) quand l'écran quitte la composition.
- `conjointPourMariage` (Fiches.kt) : conjoint tolérant (liens à sens unique, date de mariage commune) ; une personne sans sexe dont le conjoint est un homme est traitée comme mariée.
- `Eventail.kt` : conjoint et enfants (cartes `CarteMiniArbre`) sous l'éventail.

## Ajouts v0.144
- `SaisieActe.kt` : le choix du type « Mariage » dans le formulaire (ouvert via « Ajouter un événement », donc préempli en naissance) re-préremplit le formulaire avec `prefillActe(..., "M")` quand la personne est une femme (échange marié/mariée).

## Ajouts v0.145
- `VuesArbre.kt` : `EcranSablier` (3 générations d'ancêtres via `ancetresEventail`, conjoints, enfants et petits-enfants via `enfantsDe`/`descendantsSablier`) et `EcranParenteVue` (chemin entre la personne centrale et une personne choisie : `calculerParente` + `cheminParente`).
- `Frise.kt` : `EcranFrise(histoire = true)` = « Frise historique » (bandeau des régimes `PeriodesHistoriques`, repères `reperesHistoriques`). Menu Arbre : Arbre / Ligne de vie / Éventail / Sablier / Parenté / Frise historique (sous-écrans 0 à 5 dans la pile de retour).

## Ajouts v0.146
- `VuesArbre2.kt` : `EcranDescendance` (`lignesDescendance`, `tousDescendants`, branches dépliées mémorisées par `rememberMemo`) et `EcranCompletude` (`completudeAscendance`, `ancetresManquants`). Menu Arbre : sous-écrans 6 et 7.

## Ajouts v0.147

- `OrdreChrono.kt` : `conjointsChrono`, `enfantsChrono`, `dateMariageEntre` (fonctions pures testées) ; `enfantsDe` s'appuie dessus.
- Principe maintenu : application **uniquement locale**, manifeste sans permission INTERNET ; aucun code réseau (pas de WebView). Toute demande d'affichage de site a été refusée pour protéger les données.

## Ajouts v0.148

- `Declaration.kt` : `relationDeclarant`, `candidatsDeclarant`, `peutDeclarer`, `ageDeclarantAuto`, phrases de biographie (fonctions pures testées). `Personne` gagne `declarantId`, `declarantAge`, `declarantRelation` (JSON ; GEDCOM `_GPDNA`/`_GPDNL`).
- `ImageReduite.kt` : `DiagnosticBlocage` (lecture de `ApplicationExitInfo` ANR + sentinelle du fil principal, écriture locale dans `dernier_incident.txt`), `extraireFilPrincipal`.
- `Edition.kt` : `DialogueEdition(enLigne = true)` ; `EcranApplication` affiche Personne complète et Carnet de sources dans la demi-zone basse de la Saisie.
- Principe maintenu : application strictement locale, aucune permission.

## Ajouts v0.149

- `decoderImageReduite` : 2048 px par défaut ; `android:largeHeap` ; `resumeMemoire`, `extraireFils` dans `DiagnosticBlocage`.
- `departementsUtilises` (Geographie.kt) : damier limité aux départements présents.
- Biographie : enfants via `enfantsChrono`.

## Ajouts v0.151

- Le Journal s'affiche à chaque ouverture : plus aucun écran n'est mémorisé ni restauré (`lireEcran`, `sauverEcran`, `reprendreEcran` supprimés).
- Témoin d'un acte : `temoignagesDe` alimente la **chronologie** de la fiche (`evenementsFamille`), plus le récit ni la frise de la Biographie. Le préremplissage d'un acte (`prefillActe`) recalcule l'âge du témoin depuis sa fiche.
- Bloc « Observations » (`Observations.kt`) : `observationsDe(p)` reconstruit les lignes « Identité créée d'après l'acte de… » (`creeDepuis`, `creeDepuisFiche`) et « Né vers N d'après l'acte de… » (`naissanceCalculee`, `naissanceCalculeeSource`, `naissanceCalculeeFiche`). Toucher une ligne ouvre la fiche dont l'acte a servi. Champs en JSON et GEDCOM (`_GPNCF`, `_GPCRD`, `_GPCRF`, ids remappés à l'import). Tout reste local.

## Ajouts v0.150

- `LogiqueActe.kt` : `fichesRattachables`, `incompatibleAvecActe`, `alertesHomonymes` (pures, testées) ; `ActeSaisie.nouveauxForces` / `mMarieeId`, `TemoinSaisie.id`. Le formulaire d'acte ouvre `DialogueHomonymes` (avec explication) avant `onEnregistrer`.
- `Temoignage.kt` : `temoignagesDe`, `phraseTemoin`, `libelleTemoin` (récit de la biographie et chronologie).
- `VersionApp.kt` : `reprendreEcran` (Journal au premier lancement après installation, mise à jour ou changement de jour) ; `Stockage.lireEcran/sauverEcran` mémorisent jour et version.
- Principe maintenu : application strictement locale, aucune permission.
