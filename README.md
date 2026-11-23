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

- **Fiche / Biographie / Recherches** (v0.103) : trois onglets par personne ; synthèse et cadres repliés sur la Fiche, événements touchables et repères historiques dans la Biographie, recherches repliées dans leur onglet.
- **Fiches par événements** : N/B/M/D/S regroupés dans l'ordre généalogique NBMDS, éditable par appui long via la saisie d'acte pré-remplie depuis la fiche.
- **Saisie d'acte** : témoins, coche « Signe » derrière chaque personne citée, prêtre, officier d'état civil, transcription, conjoint survivant ou décédé persisté sur les actes D/S, enfant déclarant sur un décès ; actes enrichis (déclaration, légitimité, contrat de mariage, cérémonie, cimetière…).
- **Numérotation automatique** : Sosa-Stradonitz et d'Aboville, rien ne se saisit.
- **v0.150** : avertissement sur les homonymes, témoins/officier (personne existante, âge, résidence, biographie), Journal à la première ouverture du jour.
- **v0.149** : image 2048 px (mémoire), Stat. Géographie : départements utilisés seuls, Biographie : enfants chronologiques.
- **v0.148** : diagnostic des blocages, déclarant d'un décès = personne existante (biographie, âge), Saisie personne/source en demi-écran avec image, sources : commune contrôlée.
- **v0.147** : fiche (conjoints/enfants en ordre chronologique), Descendance alignée, menu Ligne de vie supprimé. L'application reste strictement **locale** : aucune permission Internet, aucune connexion.
- **v0.146** : menu Arbre : **Descendance** (liste dépliable) et **Complétude** (jauges d'ancêtres connus par génération, parents à rechercher).
- **v0.145** : menu Arbre enrichi : **Sablier**, **Parenté** (chemin entre deux personnes), **Frise historique** (régimes et événements sur la ligne de vie).
- **v0.144** : acte de mariage depuis une femme via « Ajouter un événement › Mariage » enfin correct (marié = mari et ses parents, mariée = elle et ses parents).
- **v0.143** : retour arrière corrigé partout (sous-écrans Stat./Géographie, Journal, Boîte à outils, Arbre mémorisés), conjoint et enfants dans l'Éventail, acte de mariage depuis une femme (conjoint retrouvé même si le lien est à sens unique).
- **v0.142** : **Éventail** (menu Arbre) des ascendants sur 3 générations, comme l'arbre ; acte de mariage depuis une femme corrigé ; bouton « Autres recherches ».
- **v0.141** : retour arrière (flèche) partout, rouverture à l'écran quitté, recherches qui ouvrent directement la bonne saisie, « Autres recherches » du jour, acte de mariage corrigé pour une femme, onglet « Stat. » (voir CHANGELOG).
- **Fiche** (v0.140) : blocs distincts **Conjoint(s)** et **Enfant(s)**, chacun avec une croix pour retirer le lien (confirmation, la fiche n'est jamais supprimée) ; père et mère existants choisis parmi les hommes / les femmes seulement ; chronologie unique où les actes sans date prennent leur place naturelle N, B, M, D, S.
- **Onglet Recherches** (v0.138) : bloc **Notes** libre (chiffré), recherche avancée compacte, **toutes** les recherches en liste repliée, et onglet **5 du jour** (cinq recherches variées qui changent chaque jour). Un acte non « enregistré » n'est plus une recherche.
- **Journal de recherches** : recherches à mener, anomalies à vérifier (enfant né après le décès de son père, mère trop jeune, fratrie trop rapprochée…), doublons avec fusion guidée « Garder A / Garder B » sans aucune perte.
- **Cohérence partout** : un enfant né avant son parent est refusé sur toutes les voies d'ajout (saisie, choix de fiche, nouveau parent, acte, import fusionné) ; homonymes proposés avec leurs dates.
- **Boîte à outils** (100 % locale) : calendrier républicain ↔ grégorien (julien, fêtes, décades), calculateur de dates (âges, écarts, durées), calculateur de parenté (« Marie est la cousine germaine de Pierre »). Depuis la v0.101 : **repères historiques** (rapprochement avec les dates des ancêtres), **arbre du chemin de parenté**, **« Que s'est-il passé cette année ? »** et **chronologie d'une famille** (plusieurs personnes juxtaposées, incohérences en rouge). La **Saisie** est le 6e onglet (Personnes / Journal / Arbre / Outils / Sources / Saisie) ; le menu **Autres** liste les personnes sans numéro.
- **Carnet de sources** : une source décrite une seule fois, citée autant de fois que nécessaire (n° d'acte, vue n°/total, fiabilité 0-3), exportée en GEDCOM (SOUR, QUAY, PAGE).
- **GEDCOM sans perte** : les champs propres à GénéaPoche (balises `_GP*`) survivent à l'export puis au réimport, plus une NOTE de synthèse lisible dans Heredis.
- **Arbre 4 générations** avec les enfants du couple central, zoom tactile ; **recherche** par nom et lieu ; **statistiques et géographie** (répartition par département, voyage chronologique) ; mode sombre ; mise en page adaptative tablette/paysage.

Historique détaillé version par version : [CHANGELOG.md](CHANGELOG.md). Architecture et bilan technique : [docs/](docs/) (dernier bilan : [docs/BILAN-2026-11-17.md](docs/BILAN-2026-11-17.md)).

## Modèle de sécurité — exact, avec ses limites

- **Application 100 % locale : aucune connexion Internet, nulle part** (vérifié automatiquement à chaque build).
- **Aucune permission Internet** dans le manifeste : l'application elle-même ne transmet rien.
- ✅ **Zéro Internet** : plus aucun lien ni recherche vers les archives départementales (supprimés en v0.33) ; le seul échange externe reste le fichier GEDCOM.
- Stockage local **chiffré AES-256/GCM** avec une **clé maître aléatoire de 256 bits liée au code PIN** (double enveloppe PBKDF2 + AndroidKeyStore) : sans le PIN, aucune donnée ne se déchiffre.
- **Code PIN** : PBKDF2-HmacSHA256 (600 000 itérations), codes triviaux refusés, phrase secrète possible, blocage progressif après 5 erreurs (30 s / 2 min / 10 min / 1 h) insensible au changement de l'heure.
- **FLAG_SECURE** : captures d'écran et aperçu dans les applications récentes interdits.
- **Reverrouillage** automatique (délai réglable, immédiat par défaut) et à l'extinction de l'écran ; purge de la mémoire au verrouillage.
- **Sauvegarde .gpbak chiffrée** exportable/importable avec phrase secrète (PBKDF2 600 000 itérations depuis la v0.108) ; écritures atomiques et durables.

**Limites, en toute honnêteté :**
- ⚠️ Les exports GEDCOM et images sont en clair, à manipuler avec prudence.
- ⚠️ Sans le code PIN, la base est indéchiffrable : **aucune récupération possible**.

Détails dans [SECURITY.md](SECURITY.md) et [PRIVACY.md](PRIVACY.md) (politique de confidentialité en français).

## Installation

**Téléchargement direct de la dernière version (v0.150)** : page [Releases](https://github.com/Plime-prog/GeneaPoche/releases/latest) — APK `GeneaPoche-v0.150-release.apk` (signé) ou `GeneaPoche-v0.150-debug.apk`. **Mise à jour depuis une version antérieure à la v0.140 : désinstaller une fois l'ancienne version après avoir exporté une sauvegarde `.gpbak` — voir `docs/SIGNATURE.md`.**

Sinon, depuis les sources :

1. Onglet **Actions** → dernier run vert → artefact **geneapoche-vX.Y-release** (APK signé) ou **geneapoche-vX.Y-debug** ; les versions taguées v* publient aussi l'APK release signé.
2. Décompresser le ZIP, installer l'APK sur le téléphone (autoriser les sources inconnues).

**Mise à jour : « Package non installé » (conflit de signature)** : depuis la **v0.140**, tous les APK (debug et release) sont signés par une **clé stable** embarquée dans le dépôt (vérifiée par la CI, voir `docs/SIGNATURE.md`) : les mises à jour s'installent par-dessus l'ancienne, sans désinstallation ni perte. La procédure ci-dessous ne concerne plus **que les mises à jour depuis une version antérieure à la v0.140** (certificats différents) : (1) exporter une sauvegarde **.gpbak** avec phrase secrète et vérifier qu'elle n'est pas vide (par précaution, aussi un export GEDCOM) ; (2) désinstaller ; (3) installer le nouvel APK **release** ; (4) créer le PIN puis **importer la .gpbak** (personnes, sources, actes, images et journal sont restaurés).

3. Premier lancement : créer un code PIN (6 chiffres minimum ou phrase secrète). Ne l'oubliez pas.

## Développement

- Android Kotlin + Jetpack Compose, Material 3 (thème clair/sombre, palette verte).
- AGP 9.5.1, Kotlin 2.4.21, compileSdk 37, targetSdk 36, JDK 17, BOM Compose 2026.09.00.
- CI GitHub Actions : lint, tests unitaires JVM (349), APK debug et release signé à chaque push.
- Contribuer : voir [CONTRIBUTING.md](CONTRIBUTING.md). Historique détaillé des versions : [CHANGELOG.md](CHANGELOG.md).
- Licence : MIT (voir [LICENSE](LICENSE)).
