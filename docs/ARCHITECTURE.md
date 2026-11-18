# Architecture de GénéaPoche

*Mise à jour : 15 novembre 2026 (v0.133). Document de référence pour les contributeurs ; l'historique fonctionnel est dans [CHANGELOG.md](../CHANGELOG.md).*

## Vue d'ensemble

Application Android mono-module (`:app`), Kotlin + Jetpack Compose (Material 3). Aucune permission Android, aucune bibliothèque réseau ou de mesure. Données stockées localement, chiffrées. Code dans `app/src/main/java/com/plime/geneapoche/` (41 fichiers, ~20 000 lignes) ; tests JVM dans `app/src/test/…` (12 fichiers, 311 tests).

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

- **Stockage interne** : `[12 octets IV][données + tag GCM]`, clé maître en mémoire uniquement pendant le déverrouillage.
- **Sauvegarde `.gpbak` / export `.gpged`** : `[6 octets magie "GPBAK1" ou "GPGED1"][16 octets sel][4 octets itérations, grand-boutiste][12 octets IV][données + tag GCM]`. Depuis la v0.108, les itérations de l'en-tête sont **réellement utilisées** au déchiffrement (bornes acceptées : 100 000 à 2 000 000) ; les nouveaux fichiers en portent 600 000.

## Construction et CI

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
