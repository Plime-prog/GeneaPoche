# GénéaPoche

Application Android de généalogie de poche : recherches généalogiques en France (actes de naissance, mariage, décès), sans rien oublier.

## Objectif

Un outil léger pour :
- saisir au fil de l'eau des personnes (père, mère, époux/épouse, **tous** les enfants) ;
- savoir quelles futures recherches faire (journal de recherches) ;
- stocker captures d'écran et photos d'actes ;
- accéder directement aux Archives départementales ;
- suivre ce qui est vérifié (pastilles vertes) ou à vérifier (rouge) ;
- **exporter en GEDCOM 5.5.1** pour alimenter Heredis.

Périmètre : ~100 antécédents max, France uniquement, actes N/M/D.

## Sécurité

- **Aucune permission** dans le manifeste : pas d'Internet, aucune donnée ne peut être transmise.
- Stockage local **chiffré AES-256/GCM**, clé dans AndroidKeyStore (jamais exportable), sauvegarde vérifiée avec copie `.bak`.
- **Code PIN** : PBKDF2-HmacSHA256 (150 000 itérations, sel aléatoire), blocage progressif (5 erreurs → 30 s / 2 min / 10 min / 1 h).
- **FLAG_SECURE** : captures d'écran et aperçu dans les applications récentes interdits.
- Reverrouillage automatique après plus de 60 secondes en arrière-plan.
- ⚠️ Sans le code PIN, les données sont indéchiffrables : aucune récupération possible.

## Numérotation

- **Sosa-Stradonitz** : 1 = de cujus (personne de référence), père = 2n, mère = 2n+1. Attribution automatique quand on lie un père ou une mère depuis une fiche Sosa.
- **d'Aboville** pour les latéraux (descendants hors ligne directe) : prérempli quand on ajoute un enfant (ex : enfant n°2 du Sosa 1 → 1.2).

## Installation

1. Onglet **Actions** → dernier run vert → artefact **geneapoche-apk**.
2. Décompresser le ZIP, installer `app-debug.apk` sur le téléphone (autoriser les sources inconnues).

## Développement

- Android Kotlin + Jetpack Compose, Material 3.
- AGP 9.2.0, Gradle 9.4.1, Kotlin 2.4.21, compileSdk 37, targetSdk 36.
- Chaque push compile automatiquement l'APK debug via GitHub Actions.

## Feuille de route

1. ✅ Squelette : fiches personnes + stockage local.
2. ✅ Fiche complète : champs séparés date/lieu (naissance, mariage, décès), sexe, profession, père, mère, conjoint(e), tous les enfants ; numéro **Sosa** + numérotation **d'Aboville** pour les latéraux.
3. ✅ Pastilles vertes (vérifié) / rouges (à vérifier) par section de fiche, signalées dans la liste.
4. ✅ Journal de recherches : pistes à creuser cochables (case verte), datées (création/clôture), liées à une personne ; onglets Personnes / Journal.
5. ✅ Stockage d'actes : images et captures d'écran liées à une personne, importées via le sélecteur de photos du système (aucune permission), redimensionnées, compressées et **chiffrées sur l'appareil** ; visionneuse plein écran.
6. ⬜ Liens directs vers les Archives départementales.
7. ⬜ Arbre simple (ascendance/descendance) centré sur une personne.
8. ⬜ Export GEDCOM 5.5.1 (importable dans Heredis).
