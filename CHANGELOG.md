## v0.51 – GénéaPoche 11 : coche d'identité, diagnostic de l'arbre, sources et preuves

- **11.0 Vérification identité** : coche sur chaque fiche, à droite du nom et du score n/6. Coche automatique si au moins 1 renseignement sur 6, croix rouge sinon ; le choix manuel reste possible (auto → coche → croix en touchant l'icône), même si l'acte est incomplet. Le journal liste les identités non certifiées : la coche sert aussi aux recherches.
- **11.2 Diagnostic complet de l'arbre** : nouvel encadré « Diagnostic de l'arbre » dans le journal — anomalies de cohérence (décès avant naissance, âges impossibles…), **doublons** (même nom et mêmes prénoms) avec **fusion guidée** : garder l'une ou l'autre fiche, toutes les informations et tous les liens (parents, conjoints, enfants, témoins, actes, recherches) sont reportés sans jamais écraser une donnée déjà présente ; **branches isolées** (personnes sans aucun lien de parenté) ; tout est cliquable vers la fiche.
- **11.4 Sources et niveaux de preuve** : sur chaque acte, en plus de la côte AD — dépôt de conservation, date de consultation et niveau de preuve 0–3 (échelle QUAY standard, comme Heredis et Gramps : 0 indirecte, 1 plausible, 2 fiable, 3 acte original). Affichés dans le sous-bloc de l'acte sur la fiche et exportés en GEDCOM (`AGNC` dépôt, NOTE date de consultation, `QUAY` sur chaque citation).

Les chantiers 11.1 (rappels et notifications), 11.3 (carte), 11.5 (vues étendues), 11.6 (retouche d'images), 11.7 (séance aux archives), 11.8 (statistiques) et 11.9 (sauvegarde planifiée) restent au programme des prochaines salves.

## v0.50 – GénéaPoche 10 : cohérence NBMDS, pré-remplissage complet, édition corrigée

- **Cohérence enfant/parent** : impossible d'ajouter ou de lier un enfant né avant la naissance de son parent (message explicite, personne non créée si nécessaire).
- **Ordre NBMDS toujours respecté** : correction du bug qui classait la Sépulture AVANT le Décès ; ordre N, B, M, D, S même si les informations sont vides ; B/M jamais avant N, S jamais après... jamais avant D.
- **Journal** : nouvelles anomalies “né(e) avant son père / sa mère” et “sépulture avant le décès”.
- **Pré-remplissage complet** : depuis une ligne NBMDS de la fiche, la saisie propose désormais toutes les informations connues, y compris le conjoint (et les parents du conjoint) pour N et D, pas seulement pour M.
- **Correction** : l'édition par appui long d'une ligne NBMDS reste maintenant sur la ligne après Enregistrer (mode édition forcé même sans acte stocké).

## v0.49 – Correction : la saisie revient dans les cadres Recherche

- Correction du bug de la v0.48 : rien ne s'écrivait dans les cadres Recherche (bloc Recherches de la fiche, sous-bloc de chaque acte, carte du journal). La liaison directe au texte de la piste ne rafraîchissait pas l'écran.
- La saisie est de nouveau immédiate dans les trois endroits ; le bouton Enregistrer conserve la note dans la recherche, qui reste partagée entre la fiche et le journal.
- Version 0.49 (versionCode 49).

## v0.48 – GénéaPoche 9 : synchronisation fiche / arbre / recherches

- 9.1 L'icône Arbre est supprimée de la fiche.
- 9.2 L'arbre généalogique (et la ligne de vie) se synchronisent AUTOMATIQUEMENT avec la personne de la fiche ouverte : plus besoin d'aucun bouton.
- 9.3 Le bloc Recherches de la fiche est synchronisé avec le journal : le texte de chaque recherche est recalculé exactement comme dans le journal.
- 9.4 Chaque recherche de la fiche est CLIQUABLE : toucher « Compléter le lieu de mariage » ouvre directement la saisie du mariage ; « Rechercher son père » ouvre le choix du père ; « Rechercher les enfants » ouvre « Ajouter un enfant », etc.
- 9.5 La note de recherche saisie dans la fiche (bloc Recherches et sous-blocs d'actes) est la MÊME que celle du journal : liaison directe, toute saisie d'un côté se retrouve de l'autre après enregistrement.
- Version 0.48 (versionCode 48).

# Historique des versions — GénéaPoche

## v0.33 (« GénéaPoche 2 » : fiche par événements et saisie d'acte intégrée)

- **Fiche : N, M et D toujours regroupés**, même sans information (B et S apparaissent dès qu'une donnée existe) ; tous les blocs sont rangés dans l'**ordre chronologique** ; règle des 100 ans conservée pour le décès et la sépulture.
- **Appui long sur un bloc d'événement = la saisie d'acte du menu +Saisie**, pré-remplie avec tout ce qui est connu (identité, père et mère avec dates/lieux/professions, conjoint et ses parents pour un mariage, témoins, signataires, date, heure, commune, département, côte AD).
- **Suppression des deux rangées de boutons** en bas de fiche, remplacées par un **bloc encadré vert « Ajouter un événement »** (icône +) qui ouvre la même saisie pré-remplie, et un bloc **« Ajouter un enfant »** après les enfants.
- **L'enregistrement met à jour l'acte existant** en mode édition (aucun doublon) et synchronise son jumeau chez le conjoint ; fiche, numérotation, cohérence et journal recalculés.
- **Nouvelle fiche depuis n'importe quelle page** : le bouton + est visible sur tous les onglets.
- **Zéro Internet** : plus aucun lien ni recherche vers les archives départementales ; l'application n'accède à aucun réseau, le seul échange externe reste le fichier GEDCOM.
- GEDCOM : version réelle 0.33.

## v0.31 (audit « 3 × 20 points »)

- **Groupe 1 (sécurité et données, 20 points)** : clé maître aléatoire de 256 bits liée au PIN (double enveloppe PBKDF2 + AndroidKeyStore) avec migration automatique ; build release signé (clé dans les secrets GitHub, R8 activé) ; jamais d'écrasement de données illisibles (blocage + restauration .bak) ; lecture JSON « tout ou rien » ; deux copies valides et écritures atomiques durables (fd.sync()) ; sauvegarde chiffrée .gpbak exportable ; sauvegarde automatique avant « Remplacer la base » ; plafond de 50 personnes levé à l'import ; blocage anti-force-brute insensible au changement d'heure ; PIN durci (6 chiffres, phrase secrète, codes triviaux refusés, PBKDF2 ≥ 600 000 itérations) ; purge mémoire au verrouillage et à l'extinction de l'écran ; changement de PIN dans l'application ; export GEDCOM « exclure les personnes vivantes » ; import GEDCOM limité à 10 Mo ; seuls http/https ouverts ; transparence vie privée (README, écran PIN) ; manifeste durci (backup exclu, cleartext interdit) + SECURITY.md, secret scanning, Dependabot.
- **Groupe 2 (robustesse, 20 points)** : dates réelles validées (LocalDate), dates républicaines converties partout (anneeDe), export GEDCOM conforme (@ doublé, CONT/CONC, SUBM, vraie version), import tolérant (CONT/CONC, BET, ANSI/UTF-16), 18 catch muets remplacés par des messages précis, stockage hors du thread principal, ViewModel (état qui survit à la rotation), fichiers découpés (moins de 400 lignes), ~25 tests unitaires JVM (dont aller-retour GEDCOM) exécutés dans la CI avec lintDebug, R8 release avec règles minimales, dépendances à jour (BOM Compose 2026.08.00), workflow fiabilisé (actions épinglées par SHA, concurrency, release sur tag v*, gradlew au dépôt), vérification des liens familiaux au chargement et après import (rapport dans le journal), compteur d'identifiants strictement croissant + purge des images orphelines, suppression de personne complète et testée, zéro avertissement du compilateur.
- **Groupe 3 (design et accessibilité, 20 points)** : thème sans flash blanc en mode sombre ; mode sombre complet avec choix Clair/Sombre/Système ; bord à bord (enableEdgeToEdge) ; geste retour prédictif ; icône adaptative monochrome + écran de démarrage ; design centralisé dans Theme.kt ; typographie Material 3 (aucun texte sous 12 sp) ; zones tactiles ≥ 48 dp ; descriptions TalkBack sur toutes les icônes porteuses de sens ; contrastes ≥ 4,5:1 et statuts jamais signalés par la couleur seule ; écran PIN à pavé numérique avec retour haptique ; barre du bas Material 3 et menus en ModalBottomSheet ; recherche par nom et lieu, clés stables, en-têtes de lettres collants ; fiche par blocs animés avec indice des gestes ; arbre avec zoom/déplacement et symbole de sexe ; mise en page adaptative (WindowSizeClass, deux colonnes) ; Snackbar avec « Annuler » après suppression ; app_name dans strings.xml et .editorconfig UTF-8 ; documentation refondue (README court, CHANGELOG, CONTRIBUTING, SECURITY, politique de confidentialité).

## v0.28 (batch des 20 demandes)

- **Un bloc encadré par événement** dans la frise : chaque N/B/M/D/S a son propre encadré (bordure à la couleur du sexe), toucher l'en-tête ouvre le bloc, **appui long sur l'en-tête ouvre directement l'édition**.
- **Sous-bloc pour chaque acte stocké** : référence (côte AD), pages « p. x/y », zone de recherche libre et loupe vers les archives départementales regroupés au même endroit ; toucher le titre affiche l'image, appui long ouvre pages/référence.
- **Identité par bouton de couleur** : rouge (aucune info) → orange → vert selon les 4 informations (nom, prénoms, date et lieu de naissance) ; le toucher confirme, le crayon disparaît (édition par appui long sur le bloc identité).
- **Retour en arrière = fiche précédente** : la flèche retour dépile les fiches visitées ; toucher un nom (père, mère, conjoint, témoin, enfant) ouvre directement sa fiche.
- **Plus d'historique nulle part** : les recherches résolues sont supprimées ; suppression des recherches archivées du journal et des fiches.
- **Boutons du bas immédiats** : Baptême/Mariage/Sépulture ouvrent le bloc dès l'appui ; la coche d'un événement passe au vert dès qu'une date est saisie.
- **Calendrier républicain affiché tel quel** : une date républicaine saisie reste républicaine et s'affiche avec sa conversion grégorienne entre parenthèses (ex : « 22 Germinal An II (11/04/1794) »).
- **Affichage épuré partout** : vignettes à l'initiale, noms tronqués sur une ligne (ellipsis), interlignes et paddings réduits, plus de mots « Sosa » ou « d'Aboville » hors des menus, sélecteur de sexe F/M sans troisième choix, bloc « Notes libres » sans titre et bloc « Recherches » sur chaque fiche.

## v0.27 (numérotation du de cujus fiable)

- **Correction du bug de mise à jour** : modifier le n° Sosa du de cujus renumérote désormais toute la base à coup sûr — l'ancienne renumérotation « en direct » pendant la frappe est supprimée.
- **Aperçu en direct, application à l'enregistrement** : le dialogue affiche les numéros qui en découleront (père 2n, mère 2n+1, enfants n.1…) sans toucher aux données ; « Enregistrer » applique et sauvegarde.
- **Message de confirmation** après chaque enregistrement. **Annuler ne modifie plus rien.**

## v0.26 (accents corrigés, listes figées et 4 branches)

- **Accents de la page Arbre corrigés** : la page est écrite en UTF-8 réel, accents parfaits (« première », « aîeule »).
- **Personnes : menus figés** : compteur, puces de tri et index alphabétique restent toujours visibles en haut, seule la liste des noms défile.
- **A–Z = filtre direct** : toucher une lettre n'affiche que les personnes dont le nom de famille commence par elle ; re-toucher efface le filtre.
- **Branches = les 4 grands-parents du de cujus** ; choisir un nom affiche toutes les personnes de cette branche, même si elles portent un autre nom.
- **Dialogue « de cujus » compact** : interlignes réduits, nom et prénoms tronqués sur une seule ligne.

## v0.25 (règle des 100 ans, journal priorisé et coches de validation)

- **Règle des 100 ans** : décès et sépulture ne s'affichent nulle part tant que la personne ne peut pas avoir plus de 100 ans, sauf si son décès est déjà renseigné.
- **Journal priorisé, règle des 75 ans** : seules les recherches dont l'événement visé est survenu il y a plus de 75 ans sont affichées, de la plus récente à la plus ancienne, 10 au maximum.
- **Numéro d'Aboville partout dans le journal** (pastille de chaque recherche).
- **Arbre relié et centré façon généalogique** : grands-parents, parents, couple central uni, enfants centrés, lignes de parenté.
- **Coche « validé » sur chaque acte et information** : coche négative = recherche automatique, coche positive = archivage.

## v0.24 (menus fixes, Aboville et calendrier républicain)

- **Menus de navigation toujours visibles en bas**, même fiche ouverte.
- **Tri par d'Aboville croissant** dans l'onglet Personnes (avec A–Z et branches).
- **Frise à ordre fixe** : Naissance, Mariage, Décès — même sans information ; Baptême et Sépulture s'intercalent dès leur création.
- **Calendrier républicain** : saisie possible (ex : 22 Germinal An II), convertie automatiquement en grégorien à l'enregistrement ; débuts d'année historiques et sextiles respectés.
- **Noms normalisés** : nom de famille en MAJUSCULES, prénoms avec initiale majuscule seulement, y compris composés.
- **Numérotation corrigée** : le recalcul Sosa/d'Aboville suit chaque changement de de cujus (graines latérales par n° Sosa croissant, rang de fratrie par année de naissance).
- **Fiche : 5 boutons fixes en bas** avec icônes, toujours visibles.

## v0.23 (formats uniques et fiche allégée)

- **Un seul format de date et d'heure** : jj/mm/aaaa et hh:mm contrôlés à la saisie ; l'année seule aaaa est acceptée.
- **Dialogue d'édition du de cujus** : choix de la personne puis édition de son n° Sosa, synchronisation en direct, Annuler restaure.
- **Cartouche allégé, sans édition** ; l'édition de l'identité passe par le crayon de la barre de titre.
- **Frise repliée à l'ouverture** ; plus de crayon sur les lignes, la correction se fait dans le bloc ouvert.
- **Quatre boutons toujours visibles sous la frise** : Baptême, Mariage, Sépulture, Enfant.

## v0.22 (cujus instantané et blocs dédiés)

- **De cujus à synchronisation instantanée** : le numéro Sosa se recalcule en direct pendant la saisie ; Annuler restaure la numérotation précédente.
- **Un encadré par événement ouvert** : père, mère, conjoint, référence, témoins et signataires, photo de l'acte et lien AD n'apparaissent que dans ce bloc.

## v0.21 (frise légère et fiches enfants)

- **« Définir comme de cujus » retiré du cartouche** ; frise sans titre.
- **Une seule ligne par événement avec l'essentiel** (« N. 12/03/1857 à Roubaix (59) ») ; toucher la ligne ouvre/ferme le bloc ; crayon en bout de ligne = édition.
- **Boutons Enfant ajoutés à la frise** (choisir / saisir) ; un bloc par enfant = la fiche résumée de l'enfant, croix pour détacher.

## v0.20 (fiche personne v2)

- **Cartouche identité** : pastille au numéro (bleu/rose, vert si sexe absent), nom et prénoms, profession, rappel du père et de la mère avec leurs dates N. et D. (cliquables).
- **Frise chronologique de haut en bas** ; édition par bloc uniquement (icône crayon).
- **Naissance : parents choisis** parmi les fiches existantes ou créés (anti-doublon, synchronisation) ; coche « Signe » derrière le père.
- **Mariage** : conjoint + autres conjoints + enfants, chacun choisi dans la base ou créé.
- **Témoins** : fiches existantes proposées d'abord, création seulement si personne ne correspond ; coche de signature.
- **Chaque validation resynchronise tout** : numéros, contrôle de cohérence, journal.

## v0.19 (fiche personne refondue)

- **Titre identique à la liste** ; blocs resserrés, Naissance et Décès seulement par défaut ; bouton BMS (+ Baptême, + Mariage, + Sépulture).
- **Naissance : père et mère avec âge à l'acte** ; Décès : sans les parents.
- **Saisir un mariage / un enfant** : contrôle anti-doublon (nom + prénoms, insensible à la casse), synchronisation avec la fiche existante, autres mariages et enfants illimités.
- **Boutons Supprimer, Annuler, Enregistrer** dans le dialogue d'édition.

## v0.18 (numérotation et affichage)

- **Numérotation immédiate et complète** : plus aucun numéro à la main ; Sosa-Stradonitz puis d'Aboville pour tous les enfants non-Sosa de toute personne numérotée ; tout rattachement met les numéros à jour immédiatement.
- **Pastille corrigée** : le rond affiche le numéro brut, taille de police adaptée aux numéros d'Aboville longs.
- **Encodage UTF-8 robuste** : tout le code est ASCII pur (accents en séquences d'échappement).

## v0.17 (sources Heredis et cohérence)

- **Référence (côte) de chaque acte** : champ dans l'onglet Saisie et dans le dialogue Page de chaque acte, affichée sur la fiche dans chaque bloc.
- **Export GEDCOM façon Heredis** : chaque acte devient un enregistrement SOUR (TITL, REFN côte, DATA/EVEN, DATE, PLAC, NOTE transcription) ; les événements et mariages y renvoient via SOUR @S…@ + PAGE.
- **Lieux normalisés** : PLAC au format « commune, numéro, département » (ex : Roubaix, 59, Nord).
- **Contrôle de cohérence** : encadré orange en tête du journal signalant les incohérences d'années.

## v0.16 (journal exhaustif)

- **Une recherche par information manquante** (date, heure, lieu, département, profession, sexe, père, mère, conjoint, enfants, copie d'acte), regroupé par personne avec compteurs ; dès qu'une donnée est saisie, la recherche est archivée automatiquement (horodatée). Chaque recherche garde une zone de texte libre (pages, côtes, notes).

## v0.15 (fiche et journal synchronisés)

- **Fiche personne par blocs modifiables au toucher long** ; chaque bloc ramasse tout : date, heure, lieu, département, père et mère (âge à l'acte), conjoint, témoins et signataires.
- **Informations manquantes affichées en gris (—)**, synchronisées avec le journal de recherches.
- **Nouveau nom saisi** : synchronisation avec la fiche existante, sinon proposition de création.

## v0.14 (saisie par acte)

- **Onglet Saisie** : transcription directe d'un acte d'état civil — type N/B/M/D/S, commune + département, date et heure.
- **Personne concernée** choisie ou nouvelle ; père ET mère (âge à l'acte calculé), lieu, profession, décès ; mariée et ses parents ; jusqu'à 4 témoins et puces « Qui signe ? ».
- **Tout alimente la fiche** et la numérotation ; transcription intégrale mémorisée dans l'acte et affichée dans la visionneuse en l'absence d'image.

## v0.13 (refonte)

- Numérotation recalculée à chaque enregistrement ; dates jj/mm/aaaa et heures 14h05 partout ; alphabet resserré sur une ligne ; icônes du menu GEDCOM alignées à gauche ; arbre bleu/rose selon le sexe.

## v0.12 (refonte)

- Fin du violet Material3 (surfaces container surchargées : barre d'onglets du bas, dialogues, cartes) ; encadrés verts sur fond blanc pour la fiche, le journal et l'arbre.

## v0.11 (refonte)

- **Plafond 50 personnes** ; de cujus au n° Sosa libre (père = 2n, mère = 2n+1, d'Aboville pour les latéraux) ; palette sans violet ; menu GEDCOM à icônes ; alphabet complet grisé ; encadrés rose/bleu ; classement par branche ; blocs B et S à la demande ; bouton Vérifié unique par bloc.

## v0.10 (refonte)

- Démarrage sur le journal ; bouton GEDCOM (export/import/vider) ; vignettes bleu/rose avec n° Sosa/d'Aboville ; index alphabétique ; journal 3 blocs + correctif doublons ; 5 recherches automatiques + 5 de libre saisie.

## v0.9 (refonte de la liste)

- Palette blanc/verts, logo, tri alphabétique, de cujus à l'ouverture, appui long, conjoints multiples, 5 recherches numérotées en 4 blocs.

## v0.1 – v0.8

- Squelette : fiches personnes + stockage local chiffré ; champs séparés date/heure/lieu/département ; Sosa + d'Aboville automatiques ; sécurité (PIN, chiffrement, FLAG_SECURE) ; pastilles vertes/rouges ; journal de recherches intelligent ; stockage d'actes chiffrés avec zoom ; liens directs vers l'état civil des Archives départementales ; arbre simple ; export GEDCOM 5.5.1 + import Heredis.
