# GénéaPoche

Application Android de généalogie de poche : recherches généalogiques en France (actes N/B/M/D/S), sans rien oublier.

## Présentation

Un outil léger pour :
- saisir au fil de l'eau des personnes (père, mère, époux/épouse, **tous** les enfants) ;
- savoir quelles futures recherches faire (journal de recherches intelligent) ;
- stocker captures d'écran et photos d'actes **dans chaque bloc d'événement** (chiffrées sur l'appareil) ;
- suivre ce qui est vérifié (pastilles vertes) ou à vérifier (rouge doux) ;
- **exporter en GEDCOM 5.5.1** pour alimenter Heredis, et importer depuis Heredis.

Périmètre : France uniquement, actes N/B/M/D/S.

Principales fonctions : numérotation Sosa-Stradonitz et d'Aboville entièrement automatique (rien ne se saisit), fiche par événements (N/B/M/D/S regroupés dans l'ordre généalogique NBMDS) éditable par appui long via la saisie d'acte pré-remplie, saisie par acte d'état civil (témoins, coche « Signe » derrière chaque personne citée — père, mère, parrain et marraine sur deux lignes fixes au baptême, prêtre (nom et prénoms, sans profession), officier d'état civil (naissance, mariage et décès, avec sa profession), époux, parents, témoins —, transcription), conjoint survivant ou décédé avant persisté sur les actes D/S, enfant déclarant coché sur un décès, ordre NBMDS prioritaire sur le classement chronologique de la frise, un acte enregistré (B/D/S) restant toujours visible même si sa date n'est pas reportée, tout comme les baptêmes où la personne est parrain ou marraine sur la fiche, cohérence étendue aux témoins (un homonyme (même nom et mêmes prénoms) est proposé dans une fenêtre de choix, avec ses dates, pour reprendre la fiche existante ou en créer une nouvelle : toutes les listes de choix (conjoint, enfant, parents, de cujus, etc.) compactes et alignées à gauche avec l'identifiant unique — nom, prénoms, N. et D. — pour distinguer les homonymes), calendrier républicain converti, contrôle de cohérence étendu à toutes les voies d'ajout (
un enfant né avant son parent est refusé partout : saisie, choix de fiche, nouveau parent, acte de naissance, import fusionné — aucune fiche orpheline), arbre 4 générations avec les enfants du couple central, recherche par nom et lieu, mode sombre, mise en page adaptative tablette/paysage.

GénéaPoche 11 : coche de vérification d'identité dans l'en-tête de chaque fiche (indicateur automatique sur 6 renseignements, sur 4 pour une personne supposée en vie — naissance et mariage —, sur 2 pour un mineur, synchronisé entre la fiche et la liste des noms ; ✘ si rien, choix manuel possible au toucher), diagnostic de l'arbre dans le journal (anomalies, doublons avec fusion guidée « Garder A / Garder B » sans aucune perte, branches isolées, identités non certifiées), sources et preuves sur chaque acte (dépôt d'archives, date de consultation, fiabilité 0-3 échelle QUAY) exportées en GEDCOM (AGNC, NOTE, QUAY).

## Modèle de sécurité — exact, avec ses limites

- **Aucune permission Internet** dans le manifeste : l'application elle-même ne transmet rien.
- ✅ **Zéro Internet** : plus aucun lien ni recherche vers les archives départementales (supprimés en v0.33) ; l'application n'accède à aucun réseau, le seul échange externe reste le fichier GEDCOM.
- Stockage local **chiffré AES-256/GCM** avec une **clé maître aléatoire de 256 bits liée au code PIN** (double enveloppe PBKDF2 + AndroidKeyStore) : sans le PIN, aucune donnée ne se déchiffre, même en accédant directement au stockage.
- **Code PIN** : PBKDF2-HmacSHA256 (600 000 itérations, calcul hors du thread principal), déverrouillage accéléré (un seul calcul, l'ouverture de la clé prouve le code), codes triviaux refusés, phrase secrète alphanumérique possible, blocage progressif après 5 erreurs (30 s / 2 min / 10 min / 1 h) insensible au changement de l'heure.
- **FLAG_SECURE** : captures d'écran et aperçu dans les applications récentes interdits.
- **Reverrouillage** automatique (délai réglable, immédiat par défaut) et à l'extinction de l'écran ; purge complète de la mémoire au verrouillage.
- **Sauvegarde .gpbak chiffrée** exportable/importable avec phrase secrète ; copie .bak locale vérifiée ; écritures atomiques et durables.

**Limites, en toute honnêteté :**
- ⚠️ Les exports GEDCOM et images sont en clair, à manipuler avec prudence.
- ⚠️ Sans le code PIN, la base est indéchiffrable : **aucune récupération possible**.

Détails dans [SECURITY.md](SECURITY.md) et [PRIVACY.md](PRIVACY.md) (politique de confidentialité en français).

## Installation

**Téléchargement direct de la dernière version (v0.81)** : page [Releases](https://github.com/Plime-prog/GeneaPoche/releases/latest) — APK `GeneaPoche-v0.81-release.apk` (signé) ou `GeneaPoche-v0.81-debug.apk`.

Sinon, depuis les sources :

1. Onglet **Actions** → dernier run vert → artefact **geneapoche-apk** (debug) ; les versions taguées v* publient aussi l'APK release signé.
2. Décompresser le ZIP, installer l'APK sur le téléphone (autoriser les sources inconnues).
3. Premier lancement : créer un code PIN (6 chiffres minimum ou phrase secrète). Ne l'oubliez pas.

## Développement

- Android Kotlin + Jetpack Compose, Material 3 (thème clair/sombre, palette verte).
- AGP 9.2.0, Kotlin 2.4.21, compileSdk 37, targetSdk 36, JDK 17, BOM Compose 2026.08.00.
- CI GitHub Actions : lint, tests unitaires JVM (~25), APK debug et release signé à chaque push.
- Contribuer : voir [CONTRIBUTING.md](CONTRIBUTING.md). Historique détaillé des versions : [CHANGELOG.md](CHANGELOG.md).
- Licence : MIT (voir [LICENSE](LICENSE)).
