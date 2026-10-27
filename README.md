# GénéaPoche

Application Android **privée** de généalogie de poche : recherches généalogiques en France (actes de naissance, mariage, décès), sans rien oublier.

## Objectif

Un outil léger pour :
- saisir au fil de l'eau des personnes (père, mère, époux/épouse, **tous** les enfants) ;
- savoir quelles futures recherches faire (journal de recherches) ;
- stocker captures d'écran et photos d'actes ;
- accéder directement aux Archives départementales ;
- suivre ce qui est vérifié (pastilles vertes) ou à vérifier (rouge) ;
- **exporter en GEDCOM 5.5.1** pour alimenter Heredis.

Périmètre : ~100 antécédents max, France uniquement, actes N/M/D.

## Installation

1. Onglet **Actions** → dernier run vert → artefact **geneapoche-apk**.
2. Décompresser le ZIP, installer `app-debug.apk` sur le téléphone (autoriser les sources inconnues).

## Développement

- Android Kotlin + Jetpack Compose, Material 3.
- AGP 9.2.0, Gradle 9.4.1, Kotlin 2.4.21, compileSdk 37, targetSdk 36.
- Chaque push compile automatiquement l'APK debug via GitHub Actions.

## Feuille de route

1. ✅ Squelette : fiches personnes (nom, prénom, naissance, décès) + stockage local JSON.
2. ⬜ Fiche complète : tous les champs d'acte N/M/D, père, mère, époux(se), tous les enfants, métier, lieux.
3. ⬜ Pastilles vertes/rouges sur chaque donnée (vérifié / à vérifier).
4. ⬜ Journal de recherches (pistes à creuser, cochables, datées).
5. ⬜ Stockage d'actes : images et captures d'écran liées à une personne.
6. ⬜ Liens directs vers les Archives départementales.
7. ⬜ Arbre simple (ascendance/descendance) centré sur une personne.
8. ⬜ Export GEDCOM 5.5.1 (importable dans Heredis).
