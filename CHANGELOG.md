# Historique des versions

## v0.140 — Fin du « conflit de package » à l'installation (signature stable)

- **Cause constatée** : les APK v0.138 et v0.139 publiés portent des **certificats de signature différents** (empreintes vérifiées). Sans secrets de signature, la CI signait avec la clé « debug » du serveur, régénérée à chaque build ; Android refusait donc toute mise à jour par-dessus la version installée.
- **Correction** : une clé de signature stable (`app/geneapoche-signature.jks`) signe désormais les APK debug **et** release ; la CI vérifie avec `apksigner` que les deux portent ce certificat et échoue sinon. Voir `docs/SIGNATURE.md` (limite : clé non secrète, dépôt public).
- **À faire une seule fois en passant à la v0.140** : l'ancienne version doit être désinstallée (signature différente). **Avant**, exporter une sauvegarde `.gpbak` hors de l'application ; après installation, la restaurer. Les mises à jour suivantes s'installent par-dessus.
- 315 tests ; version 0.140.

## v0.139 — Le bloc Notes fait partie des sauvegardes .gpbak

- Le bloc **Notes** de l'onglet Recherches (v0.138) est maintenant écrit dans les sauvegardes `.gpbak` (manuelles et automatiques) et relu à la restauration. Une ancienne sauvegarde sans notes n'efface rien ; si des notes existent déjà, celles de la sauvegarde sont ajoutées à la suite sous « — Notes de la sauvegarde — » : aucun texte n'est jamais perdu.
- 315 tests (4 SauvegardeNotesTest) ; version 0.139. Non essayé sur appareil : faire une sauvegarde puis une restauration d'essai.

## v0.138 — Retours d'utilisation : versions, fiche, Recherches, chronologie

- **Version** : le titre « Ma généalogie dans ma poche » affiche la vraie version (il était figé à un ancien numéro) ; l'ancien « v0.107 » de l'écran Saisie d'un acte est supprimé.
- **Anomalie « sexe non renseigné »** : une fiche sans sexe est signalée à vérifier (niveau 2).
- **Père / mère existants** : la fenêtre « Choisir le père » ne propose que des hommes (sexe M), « Choisir la mère » que des femmes (sexe F). Une fiche sans sexe n'y apparaît pas : renseigner son sexe d'abord (la nouvelle anomalie la signale).
- **Fiche** : deux blocs distincts **Conjoint(s)** et **Enfant(s)**, chaque ligne avec une croix pour retirer le lien (après confirmation ; la fiche de la personne n'est jamais supprimée).
- **Recherches** : premier bloc « Notes » entièrement libre (chiffré, enregistré après une pause de frappe ; *pas encore inclus dans les sauvegardes .gpbak*) ; recherche avancée plus compacte ; **toutes** les recherches en liste compacte repliée (plus de plafond de 10 ni de règle des 75 ans ; touche pour déplier) ; un acte localisé mais non « enregistré » n'est plus une recherche (sans date connue, « Rechercher l'acte de … » reste proposé) ; nouvel onglet **5 du jour** : cinq recherches variées (une par personne, au plus deux du même genre), différentes chaque jour, stables toute la journée.
- **Chronologie** : défaut confirmé — dès que N, M ou D n'était pas daté, tous les actes passaient en tête et les autres événements (enfants, événements libres, famille) venaient après sans s'entremêler. Une seule règle maintenant : tout ce qui est daté est rangé par date ; un acte sans date prend sa place naturelle (N au début, B après N, M après B, D à la fin, S après D).
- 311 tests ; version 0.138. Non testé sur appareil : écrans de fiche, Recherches et 5 du jour à vérifier à la main (sauvegarde .gpbak avant).
## v0.137 — Export GEDCOM : l'échec sur des notes contenant des lignes vides est corrigé

- **Cause exacte du fichier à 0 ko retrouvée** : une note contenant des lignes vides (notes multiples importées recollées par une ligne vide, ou simple saut de ligne dans le bloc « sans titre ») faisait planter la construction du GEDCOM — aucune ligne n'était écrite et le message affichait seulement « Échec de l'export GEDCOM. », sans explication. C'était précisément le cas des bases reconstruites par import (v0.132 avait bien noté que rien n'était reproducible avec des données de test).
- Corrigé : les lignes vides d'une note sont ignorées à l'export ; une valeur vide ne peut plus faire échouer l'écriture.
- De plus, tout échec non anticipé affiche désormais le **type et le détail** de l'exception (chemins filtrés), finis les messages sans explication.
- 295 tests (287 + 3 ExportNoteVideTest + 5 MessageErreurTest) ; version 0.137.

## v0.136 — Export : la taille du fichier écrit est affichée

- Après un export GEDCOM réussi, le message indique maintenant la **taille réellement écrite** (ex. « 120 personnes, 85 Ko écrits et vérifiés »). Si le fichier apparaît à 0 Ko ailleurs alors que ce message annonce une taille, le défaut vient du stockage ou de l'application qui l'affiche, pas de GénéaPoche : utile pour cerner le signalement des « 0 Ko au-delà de 50 personnes », toujours non reproduit.
- Recherche faite : aucune limite de 50 dans le code, aucun partage par intention (pas de plafond de taille de ce côté).
- 287 tests ; version 0.136. **Retour d'essai (17 novembre) : export puis réimport réussis sur l'appareil de l'utilisateur.**

## v0.135 — Import GEDCOM : les sources d'un divorce (ou d'un autre événement de famille) ne sont plus perdues

- **Défaut confirmé par un test** : dans un GEDCOM externe, la source citée sous un divorce, des fiançailles ou une annulation de mariage était ignorée ; seules celles du mariage étaient lues. Elle est maintenant rattachée aux deux conjoints (en source générale, la fiche n'ayant pas de case « divorce » pour les citations).
- Vérifié sans défaut : citations de naissance, baptême, décès, sépulture, mariage et événements libres (résidence…).
- 286 tests ; version 0.135. Pas essayé avec un vrai fichier Heredis ni sur appareil : sauvegarde .gpbak avant import.

## v0.134 — Import GEDCOM : les notes ne sont plus perdues

- **Trois pertes confirmées par des tests** sur des fichiers GEDCOM d'autres logiciels : (1) les **notes partagées** (`1 NOTE @N1@` + enregistrement `0 @N1@ NOTE`) donnaient le texte brut « @N1@ » et la vraie note était ignorée ; (2) quand une personne avait **plusieurs notes**, seule la dernière était gardée ; (3) les **notes d'un événement** (sous naissance, décès, mariage…) étaient supprimées.
- Désormais : le texte des notes partagées est lu, toutes les notes sont conservées (séparées par une ligne vide) et une note d'événement est ajoutée à « Notes » avec son libellé (ex. « Naissance : né à la ferme du Moulin »). Une note partagée qui n'est rattachée à aucune fiche reste signalée dans le rapport d'import.
- 284 tests ; version 0.134. Pas encore essayé avec un vrai fichier Heredis ni sur appareil : faire une sauvegarde .gpbak avant d'importer un gros fichier.

## v0.133 — Le numéro de version s'affiche enfin (et figure dans les fichiers téléchargés)

- **Défaut corrigé** : le menu de l'application affichait « Version 0.107 » **écrit en dur** depuis la v0.107 : le vrai numéro n'apparaissait nulle part. Le numéro réel (`versionName` de l'APK installé) s'affiche maintenant en bas du menu **et sur l'écran de verrouillage** (visible sans déverrouiller). Un test interdit désormais tout numéro de version écrit en dur dans un texte affiché.
- **Fichiers téléchargés depuis les artefacts de la CI** (page Actions) : ils s'appelaient `app-debug.apk` / `app-release.apk` dans des ZIP nommés `geneapoche-apk` / `geneapoche-release-apk` ; le numéro n'y figurait pas. Ils s'appellent maintenant `GeneaPoche-v0.133-release.apk` (ZIP `geneapoche-v0.133-release`), comme les fichiers des Releases (inchangés).
- 279 tests ; version 0.133. Non vérifié sur appareil : à contrôler en ouvrant le menu (en haut à droite) puis en verrouillant l'application.

## v0.132 — Export : plus de fichier vide (0 Ko), sans aucune limite de taille

- **Signalement** : au-delà de 50 personnes, le fichier exporté faisait 0 Ko.
- **Ce que j'ai vérifié** : le code d'export **ne contient aucune limite** de nombre de personnes. Un test exporte désormais des bases de 1 à 3 000 personnes (arbre profond, très large, export partiel, tous formats, version chiffrée) : tout sort en entier. La cause n'est donc **pas** dans la fabrication du GEDCOM, mais dans l'écriture du fichier côté Android, que je n'ai **pas pu reproduire** sans appareil.
- **Ce qui pouvait produire un fichier de 0 Ko, corrigé** : (1) le sélecteur de fichiers crée le fichier **vide avant** l'export ; si le stockage choisi refusait le flux d'écriture, l'application affichait **« GEDCOM exporté »** sans rien avoir écrit ; (2) quitter l'écran ou verrouiller pendant l'export **annulait** le travail, fichier vide laissé ; (3) l'export lisait la liste des personnes **en direct** depuis un autre fil : un verrouillage ou une modification pendant l'export pouvait la perturber.
- **Désormais** : la base est **copiée** au lancement de l'export ; l'écriture est **non annulable**, **forcée sur disque**, **vérifiée** (taille relue) ; en cas d'échec un message clair s'affiche (« Export GEDCOM échoué : … ») et le **fichier vide est supprimé** au lieu d'être laissé à 0 Ko. La sauvegarde `.gpbak` utilise la même écriture fiable.
- **Si le problème persiste** : notez le message affiché et l'emplacement choisi (Téléchargements, Drive, carte SD…), et essayez « Téléchargements ». Non vérifié sur appareil. 277 tests ; version 0.132.

## v0.131 — GEDCOM : remariages et divorces ne sont plus perdus ni mal attribués

- **Perte corrigée à l'import (remariages)** : le modèle n'a qu'**un** emplacement de mariage par personne. Avec deux mariages, seul le premier fichier-famille lu était gardé ; la date et le lieu du **second** mariage disparaissaient en silence. Le second mariage est maintenant conservé comme événement libre « Mariage (avec Claire PETIT) » chez la personne concernée ; chaque conjoint garde la date de **sa** famille dans son emplacement. Un mariage décrit à la fois chez la personne et dans la famille n'est pas dupliqué.
- **Divorce et autres événements de famille** (`DIV`, annulation, fiançailles, bans, contrat et licence de mariage), que les logiciels écrivent dans la **famille**, étaient ignorés : ils sont maintenant donnés aux **deux conjoints** comme événements libres (« Divorce (avec Anne ROUX) », date, lieu).
- **Défaut corrigé à l'export** : la date de mariage d'une personne ayant plusieurs conjoints était écrite dans **toutes** ses familles (le premier mariage de Henri sortait aussi dans sa famille avec Claire). Elle n'est plus écrite que dans sa famille principale ; l'autre famille utilise la date de l'autre conjoint si elle existe.
- 5 nouveaux tests (`FamillesGedcomTest`) : divorce, remariage, absence de doublon, aller-retour de deux mariages, export. **Limite** : à la réexportation, ces événements sortent en `EVEN` + `TYPE` sous chaque personne (pas en balise de famille). Un divorce n'est pas encore un statut du couple dans l'application. 272 tests ; version 0.131. Non vérifié avec un vrai fichier Heredis ni sur appareil.

## v0.130 — Import GEDCOM : résidences, recensements, émigration… conservés

- **Perte de données évitée à l'import** : un fichier GEDCOM d'un autre logiciel contient souvent des événements datés autres que N/B/M/D/S (résidence `RESI`, recensement `CENS`, émigration/immigration, naturalisation, diplôme, études, retraite, testament, confirmation, première communion, ordination, adoption, bénédiction, crémation). Ils étaient **ignorés** (seulement comptés dans le rapport). Ils sont maintenant conservés comme **événements libres** de la chronologie de la personne, avec leur date, leur heure, leur lieu et leur département ; une balise `TYPE` précise le libellé (« Résidence (Caserne) »). Les dates approximatives gardent leur qualificatif (« vers 1912 »).
- Le comportement de `EVEN` + `TYPE` (événement générique) est inchangé. 5 nouveaux tests (`ImportEvenementsTest`).
- **Limites** : le divorce (`DIV`) est écrit par la plupart des logiciels dans la **famille**, pas chez la personne : il reste ignoré (à traiter dans une version ultérieure). À la réexportation, ces événements sortent en `EVEN` + `TYPE` (le libellé est conservé, pas la balise d'origine). 267 tests ; version 0.130. Non vérifié avec un vrai fichier Heredis ni sur appareil.

## v0.129 — Import GEDCOM externe : nom de naissance conservé, lieux avec département

- **Défaut corrigé** : un fichier GEDCOM peut donner **plusieurs noms** à une personne (nom de naissance, puis nom d'épouse ou surnom). Le **dernier** écrasait le premier : une femme importée avec `1 NAME Marie /DUPONT/` puis `1 NAME Marie /MARTIN/` (`2 TYPE married`) devenait « MARTIN » au lieu de son **nom de naissance** « DUPONT », ce qui est le pire résultat en généalogie française. Désormais le **premier NAME** (le principal selon GEDCOM 5.5.1) est conservé ; les suivants sont ignorés.
- **Lieux « Commune, Département, France »** (sans code postal, courant dans les exports) : le département n'était pas reconnu (le lieu restait entier avec un département vide, donc absent des statistiques et de la carte). Il est maintenant reconnu par son **nom** : « Roubaix, Nord, France » → commune « Roubaix », département 59.
- **Tests d'import externe** (`ImportGedcomExterneTest`, 7 tests, fichiers fictifs écrits à la main, fins de ligne Windows) : structure de base, lieu avec code postal, lignes mal formées et balises inconnues ignorées sans casser l'import, enfant inexistant sans lien orphelin, note `CONC`/`CONT` recollée, plusieurs conjoints (`FAMS` multiples) avec la bonne mère pour chaque enfant.
- Limite : un seul nom est conservé par personne (les autres noms ne sont pas gardés dans la fiche). 262 tests ; version 0.129. Non vérifié avec un vrai fichier Heredis ni sur appareil.

## v0.128 — Dates GEDCOM : « vers / avant / après » ne deviennent plus des dates exactes

- **Défaut corrigé (import)** : un fichier GEDCOM avec une date approximative **complète**, par exemple `ABT 12 MAR 1850` (« vers le 12 mars 1850 »), était lu comme la date **exacte** `12/03/1850` : le qualificatif disparaissait et une incertitude devenait une certitude. Même sort pour `BEF`, `AFT`, `BET … AND …` suivis d'un jour et d'un mois. Désormais le qualificatif est conservé avec l'année (le format interne ne porte « vers / avant / après / entre » qu'avec une année) : `vers 1850`, `avant 1900`, `après 1900`, `entre 1850 et 1851` ; `EST`, `CAL`, `ABOUT` valent « vers ». Les mois en minuscules (`27 oct 1890`) sont reconnus.
- **Même défaut corrigé à l'export** : une date saisie « vers 12/03/1850 » était exportée comme date exacte `12 MAR 1850` ; elle sort maintenant `ABT 1850`.
- **Compromis assumé** : le jour et le mois sont perdus quand il y a un qualificatif (on préfère « vers 1850 » à une fausse précision). Les dates exactes, `ABT 1890`, `BET 1850 AND 1860` sont inchangées.
- 6 nouveaux tests ; 255 tests ; version 0.128. Non vérifié avec un vrai fichier Heredis ni sur appareil.

## v0.127 — Export GEDCOM : enfants rangés dans la bonne famille (remariages)

- **Défaut corrigé (export GEDCOM)** : pour une personne ayant eu **plusieurs conjoints**, tous ses enfants étaient écrits dans **chacune** de ses familles. Un enfant du second mariage apparaissait donc aussi chez le premier conjoint ; à la lecture (dans GénéaPoche comme dans Heredis), il pouvait recevoir la **mauvaise mère** (ou le mauvais père) et être compté comme enfant de deux unions. Désormais chaque enfant n'est écrit que dans **sa** famille : celle de ses deux parents connus ; s'il n'a qu'un parent connu, chez le conjoint principal (une seule fois), ou chez l'autre parent s'il est précisé. Les fiches elles-mêmes n'étaient pas touchées : seul le **fichier exporté** l'était. **Si vous avez déjà exporté une famille recomposée vers Heredis, refaites l'export avec cette version.**
- **Version écrite dans l'en-tête GEDCOM** : le fichier annonçait « 0.107 » depuis la v0.107 ; elle suit maintenant la version réelle, et un test vérifie l'accord avec `versionName` (la dérive ne peut plus passer inaperçue).
- **Aller-retour GEDCOM testé de bout en bout** (`AllerRetourGedcomTest`, 11 tests) : champs, accents, date partielle, note multiligne avec « @ » et très longue (lignes ≤ 255 caractères), liens père/mère/conjoint/enfants cohérents sans lien orphelin, export partiel, double aller-retour stable, plusieurs conjoints.
- Limite : un enfant dont la fiche donne deux parents différents de ceux qui le listent comme enfant (donnée incohérente) n'est plus écrit chez le parent qui le listait ; la fiche de l'enfant reste exacte. Non vérifié dans Heredis ni sur appareil. 249 tests ; version 0.127.

## v0.126 — Blocage du code PIN : politique testée, déverrouillage robuste

- **Relecture** du déverrouillage (`Stockage.deverrouiller`, `EcranPin`) et du blocage après erreurs : la logique est saine (échec enregistré de façon synchrone, horloge `elapsedRealtime` insensible au changement d'heure, repli sur l'horloge murale après redémarrage, un seul essai à la fois).
- **Politique extraite et testée** (`BlocagePin.kt`, 7 tests, comportement identique) : 5 erreurs → 30 s, puis 2 min, 10 min, 1 h, puis 1 h sans fin ; compteur remis à zéro à chaque palier ; changer l'heure du téléphone sans redémarrer n'a aucun effet ; après redémarrage, repli sur l'horloge murale.
- **Durcissement de l'écran PIN** : une erreur technique inattendue pendant le déverrouillage (Keystore, mémoire…) faisait **fermer l'application** (exception non attrapée dans la coroutine) ou laissait l'écran figé. Elle affiche maintenant « Erreur technique… réessayez » sans compter comme un code faux ni bloquer l'écran. (Non reproduit sur appareil : correctif préventif, de lecture.)
- **Limite connue, inchangée** : après un redémarrage, remettre l'horloge du téléphone en avance annule le reste d'un blocage (repli sur l'horloge murale). Seule parade complète : un secret côté matériel, hors périmètre.
- 237 tests ; version 0.126. Non vérifié sur appareil.

## v0.125 — Fin des options de compatibilité AGP 9 dépréciées

- Le projet utilisait deux options de **retour en arrière** annoncées comme dépréciées par AGP 9 (`android.builtInKotlin=false`, `android.newDsl=false`), qui produisaient des avertissements à chaque build (et seront supprimées d'une future version d'AGP). Elles sont retirées : Kotlin est désormais celui **intégré à AGP** (plugin `org.jetbrains.kotlin.android` retiré du module) et la configuration utilise le **nouveau DSL**.
- Validation : lint, **230 tests JVM**, APK debug et release sur CI, **zéro avertissement de build** ; le code de l'application est inchangé. Le plugin de compilation Compose et la version de Kotlin (2.4.21) restent épinglés.
- Pas de fichier `lint.xml` ajouté : lint passe sans réglage particulier, un fichier de réglages à l'aveugle n'apporterait rien. Non essayé sur un appareil (même code, même R8 ; si un comportement diffère, la v0.124 est la version de repli).

## v0.124 — Recherche et anti-doublon insensibles aux accents

Suite de la v0.123 : revue de toutes les comparaisons de noms du code.

- **Recherche de la liste des personnes** : « lefevre » ne trouvait pas « Lefèvre » (comparaison sensible aux accents). Elle ignore désormais casse et accents (nom, prénoms, lieux).
- **Fenêtre « nouvelle fiche »** (fiches existantes proposées, et « création proposée seulement si aucune fiche ne correspond ») : même correction, et l'égalité exacte utilise la clé de nom commune (casse, accents, tirets, ordre des prénoms).
- **Anti-doublon à la création d'une fiche** (même nom, mêmes prénoms, même date de naissance) : utilisait une simple mise en majuscules ; utilise maintenant la même clé que la saisie d'acte, pour qu'« Anne-Marie » / « Anne Marie » ou « Lefèvre » / « Lefevre » soient reconnus.
- Revus **sans changement** : détection de doublons du journal (déjà sans accents), calendrier, tris. **Volontairement laissé** : la fusion d'un GEDCOM (`ImportBase.kt`) compare nom et prénoms en majuscules seulement ; un écart d'accent y crée un doublon à fusionner ensuite plutôt qu'une fusion à tort.
- Nouvelle fonction pure `contientSansAccents` (testée). 230 tests ; version 0.124. Non vérifié sur appareil.

## v0.123 — Découpage : garde-fou « fiches à créer » d'un acte extrait et testé

- Relevé du découpage de `MainActivity.kt` : l'interface (écrans, onglets) est **déjà** dans `EcranApplication.kt` et `DialoguesApp.kt` ; ce qui reste dans `AppEcran` (≈ 1 900 lignes) est de l'**état** et des **actions** qui le modifient. On ne peut plus déplacer de gros blocs mécaniquement : il faudrait créer un « porteur d'état » partagé, refonte risquée sans pouvoir tester sur appareil. On continue donc par **petites extractions de logique pure**.
- **Défaut corrigé, révélé par ces tests** : la clé de comparaison des noms (`cleNom`) transformait chaque accent en **espace** (« Lefèvre » → « LEFE VRE », « Françoise » → « FRAN OISE »), alors que son commentaire annonçait « insensible aux accents ». Conséquence : « Lefèvre » et « Lefevre » (ou « François » et « Francois ») n'étaient pas reconnus comme la même personne dans la saisie d'acte, la fusion et la détection de doublons (les accents en début ou fin de mot passaient). Désormais les accents sont supprimés ; les tirets et apostrophes restent des séparateurs. Les clés ne sont jamais enregistrées (comparaison au vol) : aucune donnée à migrer, seulement des rapprochements en plus.
- Extrait vers `LogiqueActe.kt` (corps identique, paramètre `personnes` explicite) : `nomDejaConnu` et `nomsNouveaux`, la fonction qui décide **quelles fiches l'enregistrement d'un acte va créer** et les fait confirmer. **9 tests** (casse, accents, ordre des prénoms, parents, mariée et ses parents, témoins avec rôle, champs ignorés hors mariage). Aucun défaut trouvé.
- 229 tests ; version 0.123.

## v0.122 — Mise à jour : Android Gradle Plugin 9.5.1

- Plugin Android (AGP) **9.2.0 → 9.5.1** (reprise de la PR Dependabot n° 60), seul, après la correction de Gradle en v0.121. Le code de l'application est identique à la v0.121 ; seule la chaîne de compilation change.
- Validation : lint, 220 tests JVM, APK debug sur CI, puis release (APK signé et debug). Non essayé sur un appareil : si un comportement inattendu apparaît, la v0.121 est la version de repli.
- Termine le backlog Dependabot (PR n° 2, 4, 56, 57, 58, 60 reprises).

## v0.121 — Gradle 9.8.2 réellement utilisé par la CI (correction de la v0.120)

- **Correction d'une affirmation inexacte de la v0.120** : j'y annonçais Gradle 9.8.2 « validé sur CI ». C'était faux : le fichier de la CI (`apk.yml`) installe et régénère le wrapper avec **9.4.1 en dur** (deux endroits par job), ce qui écrasait le fichier du dépôt. Les APK v0.120 ont donc été construits avec Gradle 9.4.1. Cette erreur a été découverte en essayant AGP 9.5.1, qui exige un Gradle plus récent (erreur `NoClassDefFoundError ... ProjectTypeBinding` au premier pas de build).
- Désormais la CI utilise **Gradle 9.8.2** (aux 4 endroits), identique au wrapper du dépôt. Le code de l'application est inchangé.
- AGP 9.5.1 sera tenté seul dans la version suivante.

## v0.120 — Mise à jour : Gradle 9.8.2

- Outil de build **Gradle 9.4.1 → 9.8.2** (wrapper et scripts `gradlew` / `gradlew.bat` régénérés, reprise de la PR Dependabot n° 4). Le code de l'application est identique à la v0.119 ; seule la chaîne de build change.
- Validation : lint, 220 tests JVM, APK debug sur CI, puis release produite avec ce Gradle (APK signé et debug). Non essayé sur un appareil. AGP 9.5.1 reste à traiter, seul, dans une version suivante.

## v0.119 — Mise à jour des actions GitHub de la CI

- `actions/checkout` 5.1.0 → **7.0.1**, `actions/setup-java` 5.0.0 → **6.2.0**, `gradle/actions/setup-gradle` 5.0.0 → **6.4.1** (reprise des PR Dependabot n° 56, 57, 58), toujours épinglées par empreinte (SHA) de commit.
- Ne touche **que la chaîne de build** : le code de l'application est identique à la v0.118. Validation : CI de la branche verte, puis release produite avec ces versions (APK signé et debug).

## v0.118 — Mise à jour : BOM Compose 2026.09.00

- Bibliothèques d'interface (Jetpack Compose) alignées sur le BOM **2026.09.00** (au lieu de 2026.08.00), reprenant la proposition automatique Dependabot n° 2. Aucun changement de code : lint, 220 tests JVM et compilation passent sur CI.
- Non vérifié sur un appareil. Les autres mises à jour en attente (AGP 9.5.1, Gradle 9.8.2, actions GitHub) restent à traiter **une par une**, une version chacune.

## v0.117 — Saisie d'acte de mariage : département de l'épouse normalisé

- Relecture complète de `enregistrerActe` (≈500 lignes, déjà en grande partie déléguée à des fonctions pures testées depuis la v0.99).
- **Petit défaut corrigé** : sur un acte de mariage, le département et la commune de l'**épouse** étaient copiés tels quels, alors que ceux de la personne principale sont normalisés (2 chiffres, espaces retirés) depuis la v0.73. Un « 8 » ou un lieu avec espace parasite pouvait donc ne pas correspondre aux statistiques et à la recherche par département. Même traitement désormais pour les deux époux.
- Pas d'autre défaut trouvé. Cette fonction reste dans `MainActivity.kt` (très liée à l'état de l'écran) : son extraction complète serait risquée sans pouvoir tester sur appareil ; non recommandée tant que ce n'est pas possible. Pas de test automatique pour cette retouche ; 220 tests ; version 0.117.

## v0.116 — Restauration d'une sauvegarde .gpbak : copie de sécurité de la base actuelle

- **Risque corrigé** (relecture de `restaurerSauvegardeBase`) : restaurer un fichier `.gpbak` **remplaçait directement** la base actuelle, sans aucune copie de sa version courante. Une sauvegarde ancienne (ou vide) choisie par erreur détruisait donc le travail récent sans retour possible.
- Désormais la base actuelle est copiée dans l'emplacement tournant « avant import » (5 emplacements) **avant** remplacement ; si cette copie échoue, la restauration est annulée et rien n'est modifié. Le message de réussite indique où retrouver la base précédente (menu « Restaurer la base d'avant import »).
- Relues sans changement : `restaurerSauvegardeImport` (essaie les 5 copies, saute les vides) et `restaurerSecoursBak`. Non modifiées volontairement : y ajouter une copie ferait tourner les emplacements et pourrait écraser la copie que l'on cherche à restaurer.
- Cette modification touche le stockage et l'interface (Android) : **pas de test automatique** possible ici, non vérifiée sur appareil. 220 tests ; version 0.116.

## v0.115 — Remplacement de la base : analyse, choix du de cujus testé

- Relecture de « Remplacer la base » (`remplacerBase`) : sauvegarde automatique préalable, carnet de sources remplacé par celui du fichier, compteur d'identifiants réaligné sur le maximum existant (`verifierLiensEtRapporter`), rapport d'intégrité consigné au journal. **Aucun défaut de données trouvé** (contrairement à la fusion, les identifiants du fichier deviennent ceux de la base : pas de conversion à faire).
- Seule partie pure extraite et testée (`choisirDeCujusApresImport`, 4 tests) : plus petit Sosa numérique du fichier, sinon première personne. Comportement identique. Le reste dépend de l'état de l'écran et du stockage : il reste dans `MainActivity.kt`, sans test automatique.
- 220 tests ; version 0.115.

## v0.114 — Import GEDCOM par fusion : liens des personnes ajoutées convertis et testés

- **Correctif (menu d'import, option « Fusionner »)** : d'après la lecture du code, les personnes **ajoutées** par une fusion étaient copiées avec leurs liens (père, mère, conjoint, enfants, témoins, signataires) restés dans la **numérotation du fichier importé** (1 à n), qui n'a aucun rapport avec celle de votre base : ces liens pouvaient désigner de **mauvaises personnes** de la base. La copie partageait en outre sa liste d'enfants avec la personne importée, modifiée pendant son propre parcours (risque d'erreur à l'import). Désormais tous les identifiants du fichier sont convertis : vers la personne déjà présente quand elle est reconnue, sinon vers son nouvel identifiant ; les références inconnues disparaissent ; les listes ne sont plus partagées.
- **Logique extraite et testée** (`ImportBase.kt`, 11 tests) : personnes présentes complétées sans jamais écraser, personnes absentes ajoutées avec liens convertis (collision volontaire des identifiants fichier/base), aucun lien vers une personne inexistante, re-fusion du même fichier sans doublon ni lien en double, date de naissance incompatible = pas un doublon. `MainActivity.kt` : 2 538 → 2 465 lignes.
- Reste de l'import (« Remplacer la base », lecture du fichier) inchangé ; 216 tests ; version 0.114.

## v0.113 — Suppression et fusion de fiches testées ; correctif des enfants en double après fusion

- **Correctif (fusion de doublons)** : lorsque le doublon et la fiche conservée figuraient tous deux dans une même liste (cas typique : les deux sont enfants du même parent, ou témoins du même acte), la liste du parent finissait avec **la même personne en double** après la fusion ; seules les listes de la fiche conservée étaient dédoublonnées. Toute liste qui citait le doublon est maintenant dédoublonnée après remplacement ; les doublons préexistants sans rapport avec la fusion ne sont pas touchés.
- **Logique extraite et testée** (`ActionsFiches.kt`, 15 tests) : capture des liens avant suppression, détachement des références, restauration par « Annuler » (aller-retour exact vérifié), et cœur de la fusion (champs vides recopiés sans jamais écraser, listes unies sans doublon, rôles repris seulement s'ils sont inconnus, références redirigées partout, témoins et signataires des cinq actes, test de charge sur 1 000 personnes). `MainActivity.kt` appelle ces fonctions : 2 607 → 2 538 lignes.
- Comportement inchangé hors le correctif ci-dessus ; 205 tests ; version 0.113.

## v0.112 — Découpage de `MainActivity.kt` (2e étape) et liste des branches à nettoyer

- **Aucun changement visible** : les deux dialogues « de cujus » (choix de la personne de référence, édition de son n° Sosa avec aperçu) quittent `MainActivity.kt` pour `DialoguesApp.kt`. Même principe que la v0.111 : état et actions passés en paramètres, textes et ordre des actions conservés ; l'action exécutée à l'ouverture (présélection) est transmise telle quelle.
- `MainActivity.kt` : 2 696 → 2 607 lignes (2 879 avant la v0.111).
- **Dépôt** : 186 branches sans travail perdu sont identifiées comme supprimables (déjà dans `main`, PR fusionnée, ou documentation seule) ; liste, raisons et restauration par SHA dans `docs/BRANCHES-A-SUPPRIMER-2026-11-15.md`. Suppression à exécuter par le propriétaire (non permise depuis l'environnement de l'assistant).
- 190 tests inchangés ; version 0.112.

## v0.111 — Découpage de `MainActivity.kt` (1re étape)

- **Aucun changement visible** : huit boîtes de dialogue de l'écran principal (changement de PIN, délai de verrouillage, thème, données illisibles, noms inconnus, import GEDCOM, GEDCOM chiffré, rappel de fichier exporté) sont **déplacées dans `DialoguesApp.kt`**. Chacune reçoit son état et ses actions en paramètres ; l'état reste possédé par l'écran principal. Textes, ordre des actions et comportements conservés (déplacement mécanique).
- `MainActivity.kt` passe de 2 879 à 2 696 lignes. La grosse fonction `AppEcran` reste à découper (≈ 55 fonctions d'actions locales, puis les écrans) : étapes suivantes.
- Pas de test automatique supplémentaire (interface) ; 190 tests inchangés ; version 0.111.

## v0.110 — Filet de sécurité : tests de l'écriture, de la récupération et de l'enveloppe de clé

- **Aucun changement visible** : cette version protège vos données en testant ce qui n'était pas testé. La logique de fichiers de `Stockage.kt` (écriture atomique, rotation de la copie `.bak`, lecture « tout ou rien », restauration) et l'enveloppe de la clé maître (PIN puis AndroidKeyStore) sont **extraites dans `FichiersSurs.kt`** sans dépendance Android ; `Stockage` ne fait plus que déléguer. Comportement identique à la v0.109.
- **Tests ajoutés** (`FichiersSursTest.kt`, `EnveloppeCleTest.kt`) : rotation des copies, principal corrompu → bascule sur la copie, deux fichiers corrompus signalés illisibles (rien n'est effacé), liste jamais partielle, coupure en cours d'écriture (`.tmp` incomplet ignoré), restauration, mauvais PIN / mauvaise clé / enveloppe altérée refusés, changement de PIN qui conserve la clé maître.
- **Reste non testé automatiquement** : le blocage progressif après erreurs de PIN et l'accès réel à AndroidKeyStore (dépendent d'Android) ; à vérifier sur téléphone.
- 190 tests ; version 0.110.

## v0.109 — Vitesse : onglet Personnes et diagnostic de l'arbre

- **Onglet Personnes plus fluide** : les deux tris complets de la base (A-Z et Aboville) étaient refaits **à chaque recomposition**, donc à chaque frappe dans la recherche, même quand le tri affiché (Sosa par défaut) ne s'en servait pas, avec une mise en majuscules à chaque comparaison. Ils sont désormais **calculés seulement quand le tri affiché les utilise**, et leurs clés (nom, prénoms, année, numéro découpé) sont **calculées une seule fois par personne** au lieu d'à chaque comparaison. L'ordre obtenu est strictement identique (tests d'équivalence avec l'ancien code sur des bases fictives).
- **Branches isolées en O(n)** : la détection (diagnostic à l'ouverture et encadré du Journal) comparait chaque personne à toutes les autres ; les identifiants cités sont maintenant collectés une seule fois. Même résultat (test d'équivalence avec `brancheIsolee`).
- **Dépendances** : androidx.core:core-ktx 1.19.1, org.json 20260814 (tests).
- Nouveau fichier `ListesPersonnes.kt` (fonctions pures). 165 tests (5 nouveaux : `ListesPersonnesTest.kt`) ; version 0.109.

## v0.108 — Audit technique : sauvegardes renforcées, documentation remise à jour

- **Correctif (sauvegardes .gpbak / exports .gpged)** : le nombre d'itérations PBKDF2 écrit dans l'en-tête du fichier était lu mais **ignoré au déchiffrement** (une constante servait de clé) : il était donc impossible de renforcer le chiffrement sans rendre les anciens fichiers illisibles. Le déchiffrement utilise désormais la valeur de l'en-tête.
- **Chiffrement renforcé** : tout nouveau fichier est dérivé avec **600 000 itérations** (au lieu de 210 000). Les sauvegardes et exports créés avant la v0.108 **restent lisibles** (test dédié).
- **Import de sauvegarde borné** : lecture par flux plafonnée à 64 Mo (comme le GEDCOM l'est déjà à 10 Mo), plus de lecture intégrale d'un fichier arbitraire en mémoire.
- **Documentation corrigée** : SECURITY.md et PRIVACY.md décrivaient encore une recherche d'archives par navigateur, supprimée depuis la v0.33 (l'application n'a aucune permission Internet) ; README (version, nombre de tests) mis à jour ; nouveau dossier `docs/` (architecture, bilan d'audit).
- 160 tests (12 nouveaux : `SauvegardeTest.kt`) ; version 0.108.

## v0.107 — 14 novembre 2026

- **Mariage ouvert depuis une fiche de femme** : elle devient la mariée (bug de préremplissage corrigé), le mari reste à choisir/saisir ; signataires permutés en conséquence.
- **Proposition systématique de fiche existante** (nom + dates, prénoms tolérés) : personne complète, saisie d'acte (concernée, mariée, parents, témoins, parrain/marraine), nouveau mariage/enfant/parent, édition de personne.
- **Événement allégé** (plus de conjoint ni parents) ; ajout de la **Situation militaire** (hommes : classe, matricule, régiment, grade, notes — rangés dans l'intitulé, GEDCOM sans perte) et du **Recensement de population**. — GénéaPoche

## v0.106 — Mariage : les époux sont rattachés, marié = homme, mariée = femme

- **Rattachement à l'arbre** : cause trouvée — l'acte de mariage renseignait la date et le lieu mais **ne liait jamais le marié et la mariée** (le conjoint n'était posé que par la mention marginale d'une naissance), si bien qu'une personne saisie par son mariage restait isolée de l'arbre. Les deux époux deviennent désormais **conjoints l'un de l'autre** (conjoint principal, ou conjoint supplémentaire en cas de remariage) ; la numérotation est recalculée à l'enregistrement.
- **Marié = homme, mariée = femme** : à la saisie d'un acte de mariage, le sexe du marié est imposé (puce « Marié (homme) »), le sélecteur de personne concernée n'offre que les hommes et celui de la mariée que les femmes ; un enregistrement avec une femme comme marié, ou un homme comme mariée, est refusé avec un message, avant toute création de fiche. La correction d'un mariage existant depuis une fiche n'est pas concernée. Sexes inconnus complétés (M / F).
- 145 tests ; version 0.106.

## v0.105 — Communes repliées, sélecteur de personne uniforme, correction et suppression d'actes

- **Champs Commune (partout)** : la liste de suggestions ne s'affiche que champ actif et lieu non encore choisi ; elle **se replie** au choix d'une commune, à la perte du focus, et quand le lieu porte déjà son code INSEE ; un lien « ✕ Effacer la commune » permet de repartir à zéro. Un seul composant : la correction vaut pour tous les champs Commune (acte, mentions marginales, témoins, domicile, cimetière, bloc d'événement…).
- **Personne concernée** : le menu devient le **sélecteur commun de l'application, avec recherche** (Personne concernée, Mariée, Conjoint de la mention marginale, Enfant déclarant, Enfant de quelle personne ?).
- **Actes** : cause trouvée de « l'acte revient comme avant » — le formulaire n'indiquait pas qu'il corrigeait un acte existant, donc un champ vidé ou corrigé n'écrasait pas la fiche. L'**édition est désormais transmise** (aussi depuis l'onglet Saisie quand l'acte existe déjà) ; la profession effacée est elle aussi enregistrée. Nouveau bouton **Supprimer cet acte** (confirmation) : efface date, heure, lieu et département de l'acte.
- **Enregistrement** : le bouton Enregistrer reste accessible clavier ouvert (la demi-image se réduit, marge clavier) ; quand il est grisé, **la raison s'affiche** (personne manquante, heure invalide).
- 142 tests ; version 0.105.

## v0.104 — Stabilité de la saisie (image à transcrire) et trace d'incident

- **Cause la plus probable du ralentissement puis de l'arrêt brutal** : l'image à transcrire (onglet Saisie) était décodée **sur le thread principal** avec une réduction fixe ×2 : une photo ou un scan de grande taille occupait des dizaines de Mo, figeait l'écran puis provoquait un manque de mémoire ou un « bitmap trop grand » (arrêt immédiat). Le décodage se fait désormais **hors du thread principal**, l'image est **ramenée à 3072 px maximum** (taille lue d'abord, nouvel essai plus réduit en cas de mémoire insuffisante, aucune exception ne remonte), et le filtre lumière/contraste n'est plus reconstruit à chaque recomposition.
- **Trace d'incident locale** : en cas d'arrêt brutal, la trace technique (sans aucune donnée généalogique) est conservée dans un fichier privé et s'affiche au lancement suivant dans le menu GEDCOM (« Dernier incident »), avec bouton Effacer ; elle permettra d'identifier la cause exacte si le problème persistait.
- Version 0.104.

## v0.103 — Fiche allégée en trois onglets : Fiche / Biographie / Recherches

- **Règle partout** : aucun événement familial (décès du conjoint, enfants, fratrie, parents, parrainages…) n'est affiché **avant la naissance ni après le décès** de la personne (fiche, biographie ; l'outil « Chronologie d'une famille » garde ses alertes d'incohérence, c'est son rôle).
- **Fiche** : **en-tête de synthèse** (années de vie, âge, naissance / mariage / décès avec lieux, métier, domicile, pastilles identité, renseignement, recherches) ; cadres **repliés par défaut** : Conjoint(s) et enfants (chaque ligne ouvre la fiche, bouton Ajouter un enfant), Sources, Notes ; les parents restent touchables.
- **Biographie** : tous les événements de l'ancienne fiche y sont déplacés, **toujours touchables** (blocs N/B/M/D/S, enfants, événements libres, ajout d'événement) ; **âge** sur les événements familiaux ; repères historiques insérés dans la chronologie (filtre Histoire) ; bloc Entourage (parents, conjoints) cliquable ; récit.
- **Recherches** : nouvel onglet qui reçoit les recherches de la fiche, chacune **repliée** (flèche pour la note).
- 140 tests ; version 0.103.

## v0.102 — Fiche en deux onglets : Fiche / Biographie

- **Personnes** : la fiche s'ouvre sur deux onglets, **Fiche** (inchangée : parents figés en tête, événements, sources, notes) et **Biographie**, une lecture sans les cadres de saisie.
- **Biographie** : bloc **Famille** (parents, époux/épouse, enfants) dont chaque ligne **se touche pour ouvrir la fiche** ; **récit en phrases** (naissance, parents, métier, mariage, enfants, décès et âge) suivi des rapprochements historiques ; **chronologie** mêlant la vie de la personne (âge à chaque événement), sa famille, les **événements historiques** de sa vie (nationaux et locaux selon son département) et les changements de régime, avec alertes d'incohérence. Le filtre **Histoire** les masque.
- Logique pure testée (3 tests, 138 au total) ; version 0.102.

## v0.101 — Saisie en onglet, menu Autres, boîte à outils historique

- **Personnes** : nouveau menu **Autres (n)** : toutes les personnes **sans numéro** (ni Sosa ni d'Aboville), classées A–Z ; la rangée de tris défile horizontalement.
- **Saisie = 6e onglet** (Personnes / Journal / Arbre / Outils / Sources / **Saisie**) : le bouton ➕ disparaît de tous les onglets. Sur l'onglet Saisie le **titre est masqué** (seule la barre des onglets reste) pour un maximum de place. L'écran reste **scindé haut / bas** : en haut un demi-écran **complètement blanc** destiné à l'image à transcrire (bouton « Image à transcrire » au centre, zoom, rotation, luminosité et contraste), en bas la saisie.
- **Entrées uniformisées** : « Personne complète » et « Source » ont le même gabarit (boutons de même taille) ; la Source ouvre le Carnet de sources par-dessus la saisie, sans perdre l'image ni le formulaire. Le retour système revient d'abord au menu des actes, puis à l'onglet Personnes. Toute saisie passe par la même procédure d'enregistrement : fiches, arbre, journal, sources et statistiques restent synchronisés.
- **Boîte à outils, 4 nouveaux outils** (hors ligne, résultat ajoutable à la note de recherche) :
  1. **Histoire** (calendriers historiques) : base locale d'une cinquantaine de repères (Villers-Cotterêts, Révolution, Républiques, Empires, Code civil, Waterloo, 1870, Commune, 1914-1918, 1939-1945…) et régimes politiques ; rapprochement avec une personne (« Paul est né 12 jours après « Proclamation de la Première République » », sinon « sous la Monarchie de Juillet »). Quelques événements **locaux** (Vendée, Alsace-Moselle, Verdun, Normandie…) liés aux départements.
  2. **Parenté : arbre du chemin** : sous le résultat du calculateur, le chemin qui relie les deux personnes (↑ fils de, ↓ père de, ⚭ époux de), ancêtres au bord gauche, ancêtre commun repéré.
  3. **Année** (« Que s'est-il passé cette année ? ») : changements de régime, repères historiques et locaux, naissances, mariages, décès, événements libres et âge des ancêtres ; toucher une ligne ouvre la fiche.
  4. **Famille** (chronologie) : la personne choisie (jusqu'à trois, **juxtaposées par année**) avec naissance, mariages, remariage, naissances et décès des enfants, décès du conjoint, événements libres, décès ; les **incohérences** apparaissent en rouge (événement avant la naissance ou après le décès, enfant né après le décès de la mère ou plus d'un an après celui du père).
- Nouveau code : `Histoire.kt` (fonctions pures), `OutilsHistoire.kt`. 135 tests unitaires JVM (nouveaux : `HistoireTest.kt`).
- **À enrichir** : la base historique est volontairement courte. **Reporté** : refonte plus profonde de la saisie (formulaires d'acte et fiche complète).
- Bumps v0.101 (gradle 101/« 0.101 »).

## v0.100 — Recherches : Journal en trois vues, recherche avancée, statistiques, naissance approximative visible

- **Naissance approximative (correctif)** : la date « vers aaaa » calculée d'après un âge était bien enregistrée mais invisible. Elle s'affiche maintenant **à la ligne Date de l'encadré N** (« vers 1879 (calculée) », en italique orange), la ligne d'origine « calculée d'après tel acte de telle personne » reste dans la zone N, et elle est utilisée pour l'âge « (~34 ans) » affiché dans la chronologie, pour le tri par ancienneté et pour les chronologies. Elle ne remplace jamais une date exacte.
- **Journal à trois chips** : **Synthèse / Recherches / Statistiques**, toujours visibles en haut. L'onglet « Stat. » disparaît de la barre inférieure (cinq onglets).
- **Synthèse** : tout le journal sauf les recherches (alertes, anniversaires, diagnostic, cohérence, anomalies), mêmes cadres et couleurs, plus un cadre **« Recherches en cours : N »** = nombre de **trous généalogiques** (somme des renseignements manquants des indicateurs n/6, n/4, n/2 de toute la base : date et lieu de N, M, D). Toucher le cadre ouvre les recherches.
- **Recherches** : toutes les recherches dans leur format actuel + **Recherche avancée (12.7)** : texte (nom, prénom, métier, lieu), lieu, **période** (année de/à), **statut de vérification** (certifiée / non), **acte manquant** (N, B, M, D, S, au moins un). Résultats **condensés** : une ligne par personne avec compteur n/6 et petit triangle à droite ; le toucher déplie un bloc (N, M, D, actes à compléter, statut, bouton « Ouvrir la fiche »).
- **Statistiques** : deux chips, **Base** et **Géographie** (l'écran existant). Base : « À compléter » (renseignements manquants, complétude), l'arbre en chiffres (individus, familles, ancêtres, descendants, générations, communes, patronymes, métiers), démographie (durée de vie moyenne, âge moyen au mariage, enfants par parent, période couverte, ancêtre le plus ancien, lieu le plus fréquent, patronymes et métiers les plus portés) et **qualité de la documentation** avec barres (dates de naissance documentées, lieux de naissance complets, décès documentés hors vivants présumés, mariages datés, **mariages avec une source**, personnes sourcées).
- Nouveau code : `Statistiques.kt` (fonctions pures), `EcranStatistiques.kt`, `RechercheAvancee.kt`. 127 tests unitaires JVM (nouveaux : `StatistiquesTest.kt`).
- **À suivre** : refonte du mode de saisie ; retrait du bouton ➕ sur tous les onglets, qui deviendra le 6e onglet « Saisie » (Personnes / Journal / Arbre / Outils / Sources / Saisie).
- Bumps v0.100 (gradle 100/« 0.100 »).

## v0.99 — Premier bilan : logique d'acte extraite et testée, index des personnes, saisie rapide, journal en 2 vues

- **Logique d'acte extraite (refactoring)** : nouveau fichier `LogiqueActe.kt` regroupant les fonctions pures de l'enregistrement d'un acte (`cleNom`, `clePrenomsNom`, `matchPersonneActe` — l'anti-doublon v0.77 —, `majChamp98`, `renseignerChampsActe`, `lierParentActe`). `enregistrerActe` dans MainActivity délègue désormais à ces fonctions ; le découpage complet par onglets continuera plus tard.
- **20 nouveaux tests JVM** (`TestsLogiqueActe.kt`) sur cette logique : clés de comparaison, anti-doublons, fill-if-blank / edition-efface, naissance calculée « vers aaaa », liaison parent avec cohérence. Le total passe de 90 à **110 tests unitaires**.
- **Correctif de cohérence** : `lierParentActe` refuse désormais l'incohérence parentale dans les DEUX sens (le lien enfant → parent n'est plus posé quand l'âge l'interdit, pas seulement l'entrée dans la liste enfants).
- **Index des personnes (performances)** : `personneParId` n'est plus une recherche linéaire (~120 appels dans le code) — nouvel `IndexPersonnes` (Map id → personne), invalidé à chaque mutation de la base (ajouts, suppressions, renumérotation).
- **Détection de doublons dé-O(n²)** : `doublonsPotentiels` groupe par clé de nom au lieu de comparer toutes les paires.
- **Test de charge** : base synthétique de 10 000 personnes — chaque personne est retrouvée par l'index, la détection de doublons reste mesurée en moins de 5 s (les vrais doublons restent détectés).
- **Saisie rapide (SaisieActe)** : bascule « Rapide » / « Compléter » — en Rapide, seuls les champs essentiels (type, commune/département, date/heure, personne concernée, conjoint, sources) ; les champs v0.98, mentions marginales, témoins/parrain-marraine et officier passent en « Compléter ».
- **Journal en 2 vues** : bascule « À faire » / « À vérifier » — les recherches prioritaires d'un côté, le diagnostic (« À corriger », anomalies, anomalies ignorées) de l'autre. La vue « Historique » suivra.
- **README réécrit, court et visuel** : liste à puces des fonctions principales, paragraphes de version condensés (les détails restent dans ce CHANGELOG), nombre de tests exact (118).
- **Mention marginale de mariage nominative** (acte de naissance, mode « Compléter ») : on précise le **nom et les prénoms** du conjoint ; la fiche existante est réutilisée (anti-doublon) ou créée, liée comme conjoint, et sa date/lieu de mariage sont complétés d'après la mention. La fiche rappelle « — avec Prénom Nom ».
- **Naissance approximative automatique, partout où un âge est saisi** : père, mère, témoins (N/B), et désormais la **personne concernée** (mariage, décès, sépulture) et la **mariée** (champ « Âge à l'acte »). La fiche reçoit « vers aaaa » avec, dans la zone N, la ligne « N. calculé : vers 1879 (calculée d'après l'acte de décès de Paul Durand) ». Jamais d'écriture si la date exacte existe (une ancienne date calculée devient alors obsolète et disparaît).
- **Contrôle de cohérence immédiat** : sous le champ Âge dès la saisie (écart de plus de 2 ans avec la date exacte de la fiche) et à l'enregistrement : deuxième âge contredisant une date déjà calculée, naissance calculée après le décès, mariage à moins de 12 ans. Le message d'incohérence est distinct (il n'est plus mêlé au message « SANS le lien de parenté »).
- **Correctif** : la ligne « N. calculé » n'est plus affichée quand la date de naissance exacte est connue.
- Tests : 118 tests unitaires JVM.
- Bumps v0.99 (gradle 99/« 0.99 »).

## v0.98 — GEDCOM sans perte, champs B/M/D/S, six corrections

- **GEDCOM sans perte (round-trip complet)** : tous les champs propres à GénéaPoche, actuels et à venir, sont écrits en balises privées `_GP*` (à la manière de `_GPVERIF`) et relus à l'import — un export puis un réimport ne perd plus rien : situation, domicile, naissance calculée + source, mentions marginales, coches « Enregistré », états de vérification (vert/rouge), témoins et signataires (avec remappage des identifiants), blocs ouverts, conjoint en vie/décédé. Toute future évolution de champ sera annoncée avec son traitement GEDCOM (conservé ou signalé comme à risque de perte).
- **NOTE de synthèse Heredis** : l'export ajoute sur chaque fiche une note « GénéaPoche - … » regroupant les renseignements transversaux (situation, domicile, naissance calculée), visibles dans Heredis.
- **Acte de naissance enrichi** : date et heure de la DÉCLARATION (distinctes de la naissance).
- **Acte de baptême** : légitime / naturel (chips), la ligne âge/situation/domicile des témoins s'applique aussi au parrain et à la marraine.
- **Acte de mariage** : contrat de mariage (nom de l'office) et dispenses.
- **Acte de décès** : date et heure de la DÉCLARATION, déclarant (nom, profession, domicile).
- **Acte de sépulture** : date de la cérémonie (distincte de l'inhumation), cimetière, concession, ministre du culte.
- **Fiches** : les nouveaux champs s'affichent dans les cartes d'événement (N, B, M, D, S) et sont pré-remplis à la saisie ; situation et domicile visibles dans la carte d'identité.
- **Source mémorisée à la ré-édition** : rouvrir un acte recharge la source, le n° d'acte, la vue et la fiabilité depuis la citation existante (plus de champs perdus).
- **Âge des parents mémorisé** : l'âge à l'acte du père et de la mère est recalculé depuis leur fiche au pré-remplissage.
- **« vers aaaa » généralisé** : l'âge donné par un acte (naissance, baptême, mariage, décès, sépulture, témoins) calcule « vers aaaa » sur toute fiche sans date de naissance (jamais écrasé si déjà renseigné).
- **Coche « Enregistré » vs journal** : cocher « Enregistré » retire bien la recherche de l'acte du journal, même sans date.
- **Zoom de l'arbre réparé** : retour au geste multi-touch dédié (transformable), compatible avec le défilement vertical.
- **Saisie assistée des communes** : tous les champs Domicile (parents, témoins, déclarant) proposent désormais les suggestions de communes (le bug « Douai » introuvable est corrigé).
- 14 nouveaux champs persistés, inclus dans le stockage chiffré, les sauvegardes .gpbak et le round-trip GEDCOM ; nouveau test unitaire de round-trip des balises _GP*.
- Bumps v0.98 (gradle 98/« 0.98 », export GEDCOM « 2 VERS 0.98 », badge de saisie « v0.98 », écran d'application).

## v0.97 — GénéaPoche 12 : arbre paisible, acte de naissance enrichi

- **Arbre : toucher une carte la centre seulement** — plus de bascule automatique vers la fiche ; la fiche s'ouvre en basculant vers l'onglet Personnes (la personne touchée y est ouverte) ou par **appui long** sur la carte, comme avant.
- **Étiquettes d'arbre : les lignes N. et D. redeviennent visibles** — le lineHeight par défaut (16 sp) rognait les deux dernières lignes de la carte de 64 dp ; corrigé (interligne adapté).
- **Acte de naissance enrichi** :
  - **Père, mère et témoins** : âge, situation et domicile ajoutés à la saisie et reportés sur les fiches.
  - **Mentions marginales Mariage et Décès** (actes récents) : date et lieu saisies, complétées automatiquement sur les fiches si les blocs correspondants sont vides.
- **Dates de naissance calculées** : quand l'acte donne un âge (1 à 120 ans) sans date de naissance connue, la fiche du père, de la mère ou du témoin reçoit « vers aaaa » (ex. « vers 1879 »), avec la mention **« N. calculé »** et la source (« d'après l'acte de naissance de … »). Effacé si une vraie date est saisie ensuite.
- **Fiches** : « né(e) vers 1879 (calculé) » dans le bloc Naissance ; mentions marginales affichées ; les baptêmes où la personne est **témoin** apparaissent dans la frise (« Témoin (naissance) », distinct de « Parrain ou marraine ») ; la saisie d'acte pré-remplit les nouveaux champs.
- 9 nouveaux champs persistés (situation, domicile, naissance calculée + source, mentions marginales) inclus dans le stockage chiffré, les sauvegardes .gpbak et l'export/import GEDCOM.
- Bumps v0.97 (gradle 97/« 0.97 », export GEDCOM « 2 VERS 0.97 », badge de saisie « v0.97 », écran d'application).

## v0.96 — GénéaPoche 12 : arbre refondu, saisie scindée maîtrisée

- **Statistiques raccourci en « Stat. »** dans la barre d'onglets.
- **Étiquettes d'arbre refondues** : le symbole Homme/Femme disparaît au profit du **n° de numérotation** (Sosa ou d'Aboville) en tête de carte, puis Nom (1re ligne), Prénoms (2e ligne), « N. date » (3e ligne), « D. date » (4e ligne, règle des 100 ans).
- **Troncature du nom et des prénoms** sur une ligne (…) ; police réduite à 8 sp, lisible sur téléphone.
- **Zoom et déplacement adaptés** : déplacer, pincer, zoomer — le texte s'adapte à la taille de l'étiquette.
- **Synchronisation arbre ↔ fiche** : toucher une carte la centre ET ouvre sa fiche dans l'onglet Personnes.
- **Écran scindé de saisie maîtrisé** : l'image reste bornée à la demi-zone haute (plus de débordement plein écran au zoom) ; le **bandeau du haut se masque dès qu'une image est choisie** pour agrandir la visualisation.
- **L'image ne vit que pendant la saisie** : elle est effacée dès que l'on change d'écran ou d'onglet (elle ne « fuyait » plus sur les autres écrans).
- **Pivoter** l'image par pas de 90° et **régler lumière et contraste** (curseurs) pour les registres peu lisibles ; réglages réinitialisés à chaque nouvelle image.
- Bumps v0.96 (gradle 96/« 0.96 », export GEDCOM « 2 VERS 0.96 », badge de saisie, écran d'application).

## v0.95 — GénéaPoche 12 : réorganisation (2) — saisie scindée

- **Tri « Sosa » par défaut** dans la liste Personnes : les puces Sosa et Aboville sont inversées (Sosa d'abord) et le tri à l'ouverture est Sosa.
- **L'onglet « Ligne de vie » disparaît de la barre** : arbre et ligne de vie sont réunis dans l'onglet « Arbre », avec un menu en haut (puces Arbre / Ligne de vie) comme le menu de la liste Personnes.
- **Ligne de vie : toucher une ligne recentre** la ligne de vie sur cette personne (plus de renvoi vers la fiche) ; la personne choisie est synchronisée partout (arbre, ligne de vie, statistiques).
- **Arbre : cadres uniformes** (hauteur fixe, même si des informations manquent), police réduite, tout sur une ligne avec troncature « … » ; intitulés Centre / Conjoint(e) / Enfant / Père / Mère / AGP / GP supprimés.
- **La Boîte à outils devient un onglet** (3e position) : elle quitte le bouton +.
- **Géographie devient « Statistiques »**, dernier onglet (6e).
- **Le bouton + ouvre un écran scindé** : en haut, l'image de l'acte à transcrire (choix dans la galerie, zoom 1-5×, déplacement au doigt — elle reste affichée pendant la saisie) ; en bas, la saisie (menu d'actes N/B/M/S/D/Événement/Enfant, « Personne complète », « Source »), champs compactés.
- **Carnet de sources compacté** : plus de fenêtre plein écran, affichage comme les autres onglets (barre de navigation toujours visible), champs et espacements réduits.
- Barre d'onglets : Personnes · Journal · Arbre · Outils · Sources · Statistiques.
- Bumps v0.95 (gradle 95/« 0.95 », export GEDCOM « 2 VERS 0.95 », badge de saisie, écran d'application).
## v0.94 — GénéaPoche 12 : réorganisation (post v0.93)

- **Tri « Sosa » dans la liste Personnes** : nouvelle puce à côté d'A-Z, d'Aboville et Branches — seuls les ancêtres de la ligne directe (n° Sosa renseigné et non nul), numéros croissants, même présentation que le tri d'Aboville.
- **L'onglet « Saisie » disparaît de la barre** : la saisie par acte (menu à 7 entrées N/B/M/S/D/Événement/Enfant, puis formulaire pré-rempli) s'ouvre désormais en plein écran depuis le bouton **+** ; le bouton retour du système ferme aussi la saisie.
- **Le bouton + ouvre un menu** : Nouvelle personne / Nouvel acte / Nouvelle source (ouvre le carnet) / Boîte à outils.
- **Boîte à outils déplacée** : elle quitte le menu des paramétrages et s'ouvre depuis le bouton +.
- **Nouvel onglet « Sources »** (dernier, après Géographie) : le Carnet de sources quitte les paramétrages pour son propre onglet, avec sa propre icône (livre).
- **Arbre : étiquettes de parenté homogènes** sur toutes les cartes du mini-arbre (AGP, GP, Père, Mère, Centre, Conjoint(e), Enfant) — elles étaient vides jusqu'ici ; « Inconnu » harmonisé en petit corps.
- Barre d'onglets : Personnes · Journal · Arbre · Ligne de vie · Géographie · Sources.
- Bumps v0.94 (gradle 94/« 0.94 », export GEDCOM « 2 VERS 0.94 », badge de saisie, écran d'application).
## v0.93 — Répartition géographique (12.5) : onglet Géographie, damier des départements, voyage de la lignée

- **Nouvel onglet « Géographie »** (6e onglet) : un damier des 98 départements (métropole, Corse 2A/2B, DOM 971-976), chaque case colorée selon le nombre d'ancêtres qui y ont vécu — du beige clair (aucun) au rouge foncé (le plus peuplé). Le nom du département et son effectif s'affichent au toucher.
- **Clic sur un département** : liste des personnes concernées (identifiant unique, cliquable vers la fiche), avec le lieu et l'événement qui les y rattache ; naissance comptée en premier.
- **Top 3 communes** de la lignée, en pastilles vertes cliquables vers les fiches.
- **« Voyage de la lignée »** : pour la personne ouverte, la succession chronologique de ses lieux (naissance, baptême, mariages, décès, sépulture, événements libres) — chaque changement de lieu est marqué d'une flèche rouge : les migrations se lisent d'un coup d'œil, génération après génération.
- **Aucune carte, aucun réseau** : tout est calculé localement depuis les lieux normalisés (code INSEE prioritaire, repli sur l'ancien format « Roubaix, 59, Nord » et sur le département stocké).
- **Correctif Corse (v0.88 → v0.92)** : les codes INSEE 2A/2B (ex « 2A004 ») étaient corrompus en minuscules (« 2a004 ») par la normalisation des communes — un ancêtre corse perdait son département. Les codes sont réparés et la lecture tolère les anciennes bases : la correction se fait toute seule à la prochaine ouverture.
- 5 nouveaux tests unitaires (répartition, repli ancien format, communes, ordre chronologique du voyage, noms des départements).
- Bumps v0.93 (gradle 93/« 0.93 », export GEDCOM « 2 VERS 0.93 », badge de saisie, écran d'application).

## v0.92 — Carnet de sources (Source réutilisable / Citation), compatible GEDCOM et Heredis

- **Fiche Source** réutilisable : titre, type (naissance, baptême, mariage, décès, sépulture, recensement, notarié, militaire, autre), commune, période, dépôt, cote, lien Internet **facultatif**, transcription, notes, niveau de fiabilité (0-3, échelle QUAY). Une source se décrit une fois.
- **Citation** : relie une source à une personne et à ce qu'elle documente (identité, naissance, baptême, mariage, décès, sépulture ou événement libre), avec n° d'acte, **vue n° sur le total de vues**, autre précision (f°, page), extrait, notes et fiabilité propre. Une source peut être citée par plusieurs personnes et plusieurs événements.
- **Écran « Carnet de sources »** (paramétrages) : recherche, création, fiche détaillée, liste des « personnes concernées » avec accès aux fiches, suppression avec avertissement du nombre de citations. **Bloc « Sources »** sur chaque fiche. **Formulaire d'acte** : choix de la source, n° d'acte, vue n° sur total ; la citation est créée (et mise à jour) à l'enregistrement, aussi pour la mariée sur un mariage.
- **GEDCOM 5.5.1 / Heredis** : enregistrements `SOUR` (TITL, REPO/CALN, DATA/EVEN/DATE/PLAC, TEXT, NOTE) et `REPO` ; citations `SOUR @Sn@` avec PAGE, QUAY, DATA/TEXT, NOTE sous INDI, NAME/BIRT/BAPM/DEAT/BURI/OCCU, événements libres et MARR. Champs propres à GénéaPoche en balises `_GP…` (ignorées par les autres logiciels, aller-retour sans perte pour GénéaPoche). Le lien Internet est écrit en ligne `URL : …` de la NOTE. L'import lit aussi les sources écrites par Heredis (NOTE par pointeur, REPO, SOUR en ligne, SOUR sous FAM).
- **Correctif** : à l'import GEDCOM, les notes sur plusieurs lignes (CONT/CONC) étaient perdues faute de l'option « le point couvre les retours à la ligne » dans l'expression de lecture.
- Sources et citations incluses dans le stockage chiffré, les sauvegardes `.gpbak`, la sauvegarde automatique, l'annulation d'une suppression de personne et la fusion de doublons (les citations suivent la fiche conservée).
- **Limite connue** : l'import GEDCOM en mode « fusion » n'apporte pas encore les sources.
- Bumps v0.92 (gradle 92/« 0.92 », export GEDCOM « 2 VERS 0.92 », badge de saisie, écran d'application). 14 nouveaux tests unitaires.

## v0.91 — Champ département supprimé

- **Plus de champ Département** : supprimé de la fiche, du conjoint, du bloc-événement, de l'événement libre et de la saisie d'acte. Le département reste **stocké** et est désormais **déduit automatiquement du code INSEE** (17010 → 17, 2A004 → 2A) à chaque enregistrement — l'information reste disponible pour de futures statistiques par département.
- **Saisie tolérante** : « Saint Denis » (avec espace) trouve désormais « Saint-Denis » — les clés de recherche ignorent tirets, espaces et ponctuation. Jusqu'à 8 suggestions, triées par pertinence (correspondance exacte d'abord).
- **Enregistrement débloqué** : un acte à date hors format (ex « 27/10/90 ») peut de nouveau être enregistré — les boutons Enregistrer n'exigent plus une date valide, seule l'heure reste contrôlée. Les corrections d'actes ne sont plus perdues.
- **Journal « À corriger »** : la proposition de lieu corrigée s'affiche en vert avant le clic ; plus de suggestion « Compléter le département ».

## v0.90 – Communes 2 : lieu-dit à la virgule, « France » retiré du stockage, code INSEE caché, journal « À corriger »

- **Lieu-dit à la virgule** : le lieu-dit ou la paroisse se sépare désormais de la commune par une simple virgule — « Le Moulin, arv » suggère Arvert et insère « Le Moulin, Arvert, 17010, Charente-Maritime » (les crochets et le tiret long restent acceptés).
- **« France » n'est plus stocké dans les lieux** : le pays est ajouté UNIQUEMENT à l'écriture du GEDCOM (« Arvert, 17010, Charente-Maritime, France ») — fini le « France » en double après un aller-retour export/import. Les anciens lieux contenant « France » sont nettoyés automatiquement à l'enregistrement.
- **Import GEDCOM** : un PLAC « Commune, code INSEE, département, France » est converti au format interne sans France, avec le département déduit du code INSEE ; le lieu-dit devant la commune est conservé.
- **Code INSEE caché partout** : fiche, chronologie, cartes d'événements, description d'acte, journal — « Arvert, 17010, Charente-Maritime » s'affiche « Arvert, Charente-Maritime ». En saisie aussi, une transformation visuelle masque le segment code sans jamais modifier la valeur stockée (il reste en base pour l'export GEDCOM).
