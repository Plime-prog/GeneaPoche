# GénéaPoche

Application Android de généalogie de poche : recherches généalogiques en France (actes N/B/M/D/S), sans rien oublier.

## Présentation

Un outil léger, complet et **autonome** pour :
- **saisir vite** : au fil de l'eau (père, mère, époux/épouse, **tous** les enfants) ou par acte d'état civil, en mode **saisie rapide** puis « Compléter » pour les détails ;
- **savoir quoi chercher ensuite** : journal de recherches intelligent, en deux vues claires (À faire / À vérifier) ;
- **conserver les preuves** : captures d'écran et photos d'actes dans chaque bloc d'événement (chiffrées sur l'appareil), carnet de sources citées acte par acte ;
- **suivre ce qui est sûr** : pastilles vertes (vérifié) ou rouge doux (à vérifier) ;
- **échanger** : export/import **GEDCOM 5.5.1 compatible Heredis**, sans perte des champs propres à GénéaPoche.

Périmètre : France uniquement, actes N/B/M/D/S.

## Fonctions principales

- **Fiches par événements** : N/B/M/D/S regroupés dans l'ordre généalogique NBMDS, éditable par appui long via la saisie d'acte pré-remplie depuis la fiche.
- **Saisie d'acte** : témoins, coche « Signe » derrière chaque personne citée, prêtre, officier d'état civil, transcription, conjoint survivant ou décédé persisté sur les actes D/S, enfant déclarant sur un décès ; actes enrichis (déclaration, légitimité, contrat de mariage, cérémonie, cimetière…).
- **Numérotation automatique** : Sosa-Stradonitz et d'Aboville, rien ne se saisit.
- **Journal de recherches** : recherches à mener, anomalies à vérifier (enfant né après le décès de son père, mère trop jeune, fratrie trop rapprochée…), doublons avec fusion guidée « Garder A / Garder B » sans aucune perte.
- **Cohérence partout** : un enfant né avant son parent est refusé sur toutes les voies d'ajout (saisie, choix de fiche, nouveau parent, acte, import fusionné) ; homonymes proposés avec leurs dates.
- **Boîte à outils** (100 % locale) : calendrier républicain ↔ grégorien (julien, fêtes, décades), calculateur de dates (âges, écarts, durées), calculateur de parenté (« Marie est la cousine germaine de Pierre »).
- **Carnet de sources** : une source décrite une seule fois, citée autant de fois que nécessaire (n° d'acte, vue n°/total, fiabilité 0-3), exportée en GEDCOM (SOUR, QUAY, PAGE).
- **GEDCOM sans perte** : les champs propres à GénéaPoche (balises `_GP*`) survivent à l'export puis au réimport, plus une NOTE de synthèse lisible dans Heredis.
- **Arbre 4 générations** avec les enfants du couple central, zoom tactile ; **recherche** par nom et lieu ; **statistiques et géographie** (répartition par département, voyage chronologique) ; mode sombre ; mise en page adaptative tablette/paysage.

Historique détaillé version par version : [CHANGELOG.md](CHANGELOG.md).

## Modèle de sécurité — exact, avec ses limites

- **Aucune permission Internet** dans le manifeste : l'application elle-même ne transmet rien.
- ✅ **Zéro Internet** : plus aucun lien ni recherche vers les archives départementales (supprimés en v0.33) ; le seul échange externe reste le fichier GEDCOM.
- Stockage local **chiffré AES-256/GCM** avec une **clé maître aléatoire de 256 bits liée au code PIN** (double enveloppe PBKDF2 + AndroidKeyStore) : sans le PIN, aucune donnée ne se déchiffre.
- **Code PIN** : PBKDF2-HmacSHA256 (600 000 itérations), codes triviaux refusés, phrase secrète possible, blocage progressif après 5 erreurs (30 s / 2 min / 10 min / 1 h) insensible au changement de l'heure.
- **FLAG_SECURE** : captures d'écran et aperçu dans les applications récentes interdits.
- **Reverrouillage** automatique (délai réglable, immédiat par défaut) et à l'extinction de l'écran ; purge de la mémoire au verrouillage.
- **Sauvegarde .gpbak chiffrée** exportable/importable avec phrase secrète ; écritures atomiques et durables.

**Limites, en toute honnêteté :**
- ⚠️ Les exports GEDCOM et images sont en clair, à manipuler avec prudence.
- ⚠️ Sans le code PIN, la base est indéchiffrable : **aucune récupération possible**.

Détails dans [SECURITY.md](SECURITY.md) et [PRIVACY.md](PRIVACY.md) (politique de confidentialité en français).

## Installation

**Téléchargement direct de la dernière version (v0.98)** : page [Releases](https://github.com/Plime-prog/GeneaPoche/releases/latest) — APK `GeneaPoche-v0.98-release.apk` (signé) ou `GeneaPoche-v0.98-debug.apk`.

Sinon, depuis les sources :

1. Onglet **Actions** → dernier run vert → artefact **geneapoche-apk** (debug) ; les versions taguées v* publient aussi l'APK release signé.
2. Décompresser le ZIP, installer l'APK sur le téléphone (autoriser les sources inconnues).
3. Premier lancement : créer un code PIN (6 chiffres minimum ou phrase secrète). Ne l'oubliez pas.

## Développement

- Android Kotlin + Jetpack Compose, Material 3 (thème clair/sombre, palette verte).
- AGP 9.2.0, Kotlin 2.4.21, compileSdk 37, targetSdk 36, JDK 17, BOM Compose 2026.08.00.
- CI GitHub Actions : lint, tests unitaires JVM (110), APK debug et release signé à chaque push.
- Contribuer : voir [CONTRIBUTING.md](CONTRIBUTING.md). Historique détaillé des versions : [CHANGELOG.md](CHANGELOG.md).
- Licence : MIT (voir [LICENSE](LICENSE)).
