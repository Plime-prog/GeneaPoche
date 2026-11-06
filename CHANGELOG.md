## v0.70 – Saisie ultra-compacte, mère non signataire, conjoint veuf ou en vie, B/S masqués

Quatre demandes utilisateur à la suite de v0.69 : le formulaire ne tient toujours pas sur une page (cadres trop grands, département en tête) ; la mère ne signe jamais un acte de naissance ; le conjoint d'un défunt peut être décédé avant lui ou encore en vie ; ne pas afficher B et S sur la fiche par défaut.

- **Champs ULTRA-COMPACTS partout** : le composable Champ est réécrit — fini le grand OutlinedTextField (~64 dp), place à un cadre mince (~36 dp) : libellé 10 sp au-dessus, texte 14 sp, bordure fine arrondie, interligne 1 dp. Environ 25-30 dp gagnés PAR champ : un acte de naissance complet (personne + parents + témoins + signataires) tient enfin sur une page.
- **N : la mère ne signe jamais l'acte** — la puce « Mère » disparaît des signataires possibles sur un acte de naissance, et toute signature « Mère » préexistante est filtrée à l'enregistrement.
- **D/S : conjoint veuf ou en vie** — le bloc « Conjoint survivant » devient « Conjoint » avec deux puces : « En vie » (survivant, déclarant) ou « Décédé avant (veuf/veuve) » ; le libellé du champ de nom s'adapte.
- **Fiche : B et S masqués par défaut** — les blocs Baptême et Sépulture ne s'affichent que si l'acte porte une donnée (date, heure, lieu ou département).
- Bumps 0.70 (gradle 70/« 0.70 », export GEDCOM « 2 VERS 0.70 », badge de saisie « v0.70 », écran d'application).

## v0.69 – Saisie compacte sur une page + alertes dates hors format partout

Les deux demandes restantes de la revue NBMDS : formulaire plus compact (« tout voir pratiquement sur une page »), département toujours sur 2 chiffres, et contrôle du format de date PARTOUT (ancien GEDCOM ou champ non contrôlé).

- **Badge de version en tête du formulaire** : un petit « v0.69 » figure à côté du titre « Saisie d'un acte » — preuve immédiate de la version installée.
- **Formulaire resserré** : marges générales 12 → 8 dp, espacement des titres de section 12 → 6 dp, interligne des champs 3 → 2 dp — l'essentiel tient sur une page.
- **Département normalisé à 2 chiffres partout** : « Nord » → « 59 », 5 → « 05 », à l'enregistrement de l'acte (commune, père, mère, parents du conjoint) et dans le dialogue d'édition de bloc.
- **Contrôle du format de date PARTOUT** : nouvelle fonction 'datesInvalides(fiche)' qui liste les blocs (naissance, baptême, mariage, décès, sépulture, événements libres) dont la date ne respecte pas le format défini (jj/mm/aaaa, aaaa seul ou date républicaine) :
  - **Liste Personnes** : pastille rouge « Date ! » sur toute fiche concernée ;
  - **Fiche** : bandeau rouge « Date à corriger : … » listant les blocs fautifs ;
  - **Saisie d'acte** : les champs de dates de la personne concernée (naissance, décès, consulté le) passent en rouge si le format est invalide.
- Bumps 0.69 (gradle 69/« 0.69 », export GEDCOM « 2 VERS 0.69 », écran d'application).

## v0.68 – Conjoint survivant (D/S) et parrain/marraine (B)

Demandes utilisateur à la suite de la revue NBMDS de v0.67 :

- **Actes de décès et sépulture** : champ « Conjoint survivant » — le « veuf/veuve de X » que porte l'acte — nom et prénoms sur une ligne avec sa pastille d'état. Le bloc complet du mariage (profession, parents du conjoint) n'apparaît pas.
- **Acte de baptême** : la section « Témoins » devient « Parrain et marraine » — mêmes champs (nom, prénoms, profession), le bon libellé d'acte.
- Bumps 0.68.

## v0.67 – Revue NBMDS : les champs réels de chaque type d'acte

Reprise utilisateur : le formulaire montrait le conjoint et ses parents sur tout type d'acte dès qu'ils existaient sur la fiche. Chaque type d'acte ne porte désormais que ses propres renseignements :

- **Conjoint et ses parents : MARIAGE uniquement** (et événement libre). Un acte de naissance n'a ni conjoint ni beaux-parents — la section ne s'affiche plus pour N/B/D/S.
- **Blocs Père/Mère adaptés au type** : acte de naissance ou baptême = identité des parents en une seule ligne (nom, prénoms, profession) ; décès ou sépulture = sans naissance des parents (décès mentionné) ; mariage/événement = bloc complet (naissance, lieu, département, profession, décès).
- **Profession de la personne concernée masquée sur les actes N et B** : un nouveau-né n'a pas de profession.
- Effet immédiat : l'acte de naissance se réduit à la personne + père/mère + témoins, sans les blocs du conjoint.
- Bumps 0.67.

## v0.66 – Nettoyage après victoire : formulaire de saisie compact à pastilles

Le bug « belle-mère » est corrigé (v0.65) : la confirmation utilisateur est arrivée, place au nettoyage demandé.

- **Boutons « Changer » supprimés** : dans la bannière de tête (le bouton « Choisir » ne reste que si aucune personne n'est sélectionnée) et le lien « Ce n'est pas la bonne personne ? Changer ». La bannière verte d'affichage (« Pour : … de X » / « Modification : … ») reste.
- **Formulaire beaucoup plus compact** : blocs Père / Mère / parents de la mariée condensés de 4 à 3 lignes de champs, libellés raccourcis (Né(e) le, Lieu, Dép., Déc. le…), champs principaux resserrés (Date, Heure, Réf. AD, Consulté le, Dépôt).
- **Pastilles d'état** sur les titres de blocs (Personne concernée, Père, Mère, Mariée/Conjoint(e), Témoins) : verte si le bloc est rempli, grise s'il est vide — l'état du formulaire se lit d'un coup d'œil.
- Bumps 0.66 (gradle 66/« 0.66 », export GEDCOM « 2 VERS 0.66 », écran d'application).

## v0.65 – Correctif « belle-mère » : champs parents fiabilisés (remplir() unique)

Sur la base du diagnostic d'un professionnel, le remplissage des blocs parents de la saisie d'acte est refait proprement :

- **Une seule fonction de remplissage** `ChampsParent.remplir(...)` : les quatre blocs (père, mère, parents du conjoint) sont remplis au même endroit — un champ ne peut plus « fuir » d'un parent vers un autre. `remplirDepuis` utilise la même fonction.
- **Identifiants des parents transmis** (`pereId`/`mereId`) : le formulaire sait pour quelle fiche parent il est ouvert ; `trouverOuCreer` garde le lien existant si le nom saisi est resté celui de la personne liée (fin des détournements d'homonymes — la belle-mère ne peut plus être choisie à la place de la mère).
- **Plus de détachement involontaire** : un lien de parenté n'est retiré que si un nom de parent rempli à l'ouverture a été explicitement vidé.
- **Tout changement d'identité signalé** : si le nom, les prénoms ou la profession de la fiche changent à l'enregistrement, le message final le dit (« Nom modifié : X → Y. »).
- **Édition : effacer un champ le vide réellement** (`renseigner` écrit aussi les valeurs vides en mode édition).
- **Tests unitaires** (JVM) : le remplissage d'un parent ne touche jamais les autres champs ; `prefillActe` reste fidèle à la fiche (nom de la personne, mère du conjoint, identifiants).
- Bumps 0.65.

## v0.64 – Correctif « belle-mère » : plus aucun remplacement de lien silencieux

Le bug persistant de la saisie de naissance (département, noms et prénoms de la belle-mère affichés à la place de ceux de l'homme, corrections jamais enregistrées) avait une double cause, démasquée par la bannière de la v0.63 :

- Le formulaire vise bien la bonne personne : c'est la **fiche** qui porte un lien de père/mère pollué (héritage du bug d'avant-v0.58). Le formulaire recopiait fidèlement la fiche.
- Surtout : corriger le nom du parent ne **remplaçait** le lien que si les dates étaient cohérentes ; en cas de refus, **RIEN n'était dit**. L'enregistrement semblait ne rien faire.

**Correctifs v0.64 :**
- **Aucun remplacement de lien silencieux** : un refus affiche son motif (« Mère NON remplacée : l'enfant ne naît pas avant sa mère » par exemple), un remplacement ou un retrait est confirmé en clair (« Mère de la fiche remplacée : A → B. ») dans le message final.
- **Liens de la fiche affichés dans le formulaire**, sous les blocs Père/Mère : « Liens de la fiche — Père : X · Mère : Y ». Un lien erroné (mère pointant sur la belle-mère) devient visible immédiatement.
- Bumps 0.64.

## v0.63

- **Personne concernée en tête du formulaire** : bannière vert/gras « Pour : Naissance de X » ou « Modification : … » (rouge si aucune personne) dès l'ouverture, sans défiler.
- **Touche « Changer »** : si le formulaire part de la mauvaise fiche, une touche recharge tout le formulaire sur la bonne personne.
- Le formulaire depuis une fiche transmet désormais les actes (le changement de personne retrouve l'acte existant, aucun doublon).

## v0.62

- **Saisie d'acte** : en modification d'un acte rattaché à une fiche, bannière claire « Modification : … de X » et suppression du bouton « Choisir une personne existante ».
- **Réparation des actes mal rattachés** : dans chaque bloc d'acte stocké d'une fiche, bouton « Cet acte concerne une autre personne » pour réattribuer l'acte à la bonne fiche.
- Fix du sous-titre (tiret cadratin manquant).

## v0.61
- Fin des doublons : la reconnaissance des noms est desormais insensible a la casse, aux accents, aux tirets/apostrophes et a l'ORDRE des prenoms ("Marie Jeanne" = "Jeanne Marie"). Corriger l'orthographe d'un nom (pere, mere, conjoint, personne) retrouve la fiche existante au lieu d'en creer une nouvelle.
- Menu Saisie : les grandes cartes Naissance/Bapteme/Mariage/... sont remplacees par des PUCES compactes (meme gabarit que le niveau de preuve), sur une seule ligne deroulante.

## v0.60
- Synchronisation totale : tout champ non vide de la saisie d'un acte ECRASE la fiche (identite, dates, lieux, departements, profession, parents, epoux/epouse) ; fini le remplissage "si vide" qui ignorait les corrections.
- Liens parents reparables : un nom saisi qui correspond a une AUTRE fiche REMPLACE le lien erronne (ex. mere pointant sur la belle-mere) ; nouveau bouton "Retirer ce lien" dans le bloc Naissance de la fiche ; delier nettoie aussi la liste enfants de l'ancien parent.
- Onglet Saisie : demarre pre-rempli sur la derniere fiche ouverte (plus de formulaire vierge).
- Formulaire de fiche : valeurs initiales capturees une seule fois a l'ouverture (plus de reset pendant la saisie).
- Menu Saisie compacte (meme gabarit que les puces de niveau de preuve).
- Accent de "republicain" restaure ; libelle de date unifie (jj/mm/aaaa, aaaa ou republicain).

## v0.59 – Correctif : liens parentaux et corrections enfin enregistrées (saisie d'acte)

- **Bug corrigé** : corriger la naissance d'une personne via la saisie d'acte montrait parfois un mauvais parent (ex la belle-mère) avec son département, et la correction ne s'enregistrait jamais.
- **Corrections désormais enregistrées** : en édition d'un acte, les valeurs saisies pour le père et la mère (département, lieu, dates, profession) REMPLACENT celles de la fiche du parent (un champ laissé vide ne change rien).
- **Réparation des liens erronés** : en édition, le nom saisi pour le père ou la mère est la référence — s'il correspond à une autre fiche, le lien de parenté est REMPLACÉ (l'ancien parent perd l'enfant, le nouveau le gagne, cohérence des dates vérifiée) ; un nom vidé retire le lien.
- versionCode 59.

## v0.58 – Correctif : saisie d'acte (personne erronée) + formulaire compact

- **Correctif important** : lors d'une correction via « Saisie d'un acte », le formulaire pouvait réutiliser les états d'une saisie précédente (intitulé, personne, département… appartenant à une autre fiche), et la modification était enregistrée sur le mauvais acte. Les états sont désormais resynchronisés à chaque ouverture, et une instance neuve du formulaire est créée à chaque saisie (compteur d'ouverture).
- **Formulaire compact** : bouton « Enregistrer » toujours visible en bas, hors défilement ; champs regroupés deux par ligne (commune + département, date + heure, nom + prénoms, profession, conjoint, parents, témoins).
- versionCode 58.

## v0.57 (correctifs : pré-remplissage en modification et dates invalides)

- **Modification d'une fiche : plus aucune donnée d'une autre personne** : les 24 champs du dialogue d'édition sont mémorisés par l'identifiant de la fiche (`remember(cible?.id)`) et non plus par l'égalité de contenu ; le dialogue est entièrement reconstruit à chaque ouverture ou changement de cible (`key`) ; `construire()` ne mute plus la fiche en place (il travaille sur une copie) et la fiche modifiée est réellement remplacée dans la base à l'enregistrement.
- **Saisie d'acte : intitulé propre à chaque acte** : ouvrir un autre acte ne conserve plus l'intitulé (profession, déménagement…) d'un acte précédent dont le champ serait vide.
- **Dates invalides refusées partout (mois 00 compris)** : dans l'édition d'une fiche, la saisie d'acte et l'événement libre, toute date ou heure saisie mais irréelle (31/02, 29/02 d'une année non bissextile, jour ou mois 00) marque le champ en rouge avec une aide et désactive Enregistrer ; une date vide reste admise. GEDCOM : version réelle 0.57.

## v0.56 – Correctif : pré-remplissage de l'édition

- **Bug corrigé** : à l'ouverture de la modification d'une fiche existante, les champs pré-remplis pouvaient appartenir à une autre personne (états `remember` sans clé, conservés d'une ouverture à l'autre du dialogue).
- Dialogue d'édition : les 24 champs sont désormais liés à la personne ciblée (`remember(cible)`) ; même correction pour le dialogue d'édition d'un bloc (date, heure, commune, département, référence) et pour l'édition d'un événement libre de la chronologie.
- Saisie d'acte ouverte depuis la fiche (appui long sur un bloc NBMDS) : le formulaire est recréé pour la personne et le type d'acte demandés (clé personne + type) ; plus aucun mélange entre deux fiches.
- Création d'une personne depuis « Ajouter un enfant » : le dialogue d'édition se FERME après le rattachement (il restait ouvert, encore rempli des données saisies).
- Version 0.56 (versionCode 56).

## v0.55 – GénéaPoche 11 (11.1) : alertes locales

- Chantier 11.1 : rappels de recherches à faire annoncés à l'ouverture, sur le même principe que le diagnostic.
- À l'ouverture (après déverrouillage) : « Rappels : N recherche(s) prioritaire(s) à faire (onglet Journal). » (même règle des 75 ans que le Journal).
- Si le diagnostic a aussi des points à examiner, les deux annonces sont combinées en un seul message.

## v0.54 – GénéaPoche 12 (2e salve)

- Coche et croix de certification d'identité PLUS GRANDES sur la fiche (barre du haut).
- Liste Personnes : coche verte ✔ (identité certifiée) ou croix rouge (à vérifier) rappelée directement sur le nom de chaque personne.
- Tris réordonnés : Aboville à gauche et PAR DÉFAUT, puis Branches, puis A–Z à droite.
- Diagnostic de l'arbre : le compteur « N point(s) à examiner » inclut désormais aussi les identités non certifiées (fini le « 0 point(s) » alors qu'il restait des identités à certifier).

## v0.53 – GénéaPoche 12 : correctifs et diagnostic à la demande

- **Sélecteur d'images réparé** : l'icône « + photo d'acte » est de retour dans l'en-tête de chaque bloc d'événement de la fiche ; le sélecteur ne re-verrouille plus l'application au retour (garde-fou picker réparé).
- **Message « Lien refusé » complet** : quand un acte de naissance est refusé pour incohérence de parenté, le message complet s'affiche désormais (« Impossible : l'enfant… L'acte reste enregistré, SANS le lien de parenté. ») au lieu d'être écrasé par « Acte enregistré ».
- **Diagnostic de l'arbre corrigé** : l'encadré du journal ne disparaît plus alors qu'il reste des points à corriger — les identités non certifiées sont désormais comptées comme les anomalies, doublons et branches isolées.
- **Bouton « Actualiser le diagnostic »** en tête du journal : le diagnostic complet de l'arbre se refait à la demande.
- **Annonce à l'ouverture** : si l'arbre compte des points à examiner, un message « Diagnostic de l'arbre : N point(s) à examiner (onglet Journal). » s'affiche au démarrage de l'application.
- Version 0.53 (versionCode 53).

## v0.52 - Correctif : un enfant ne peut plus etre ne avant son parent

- Bug reproduit : un enfant ne avant la naissance de la personne selectionnee pouvait encore etre ajoute.
- UNE SEULE regle anti-incoherence : incoherenceParente(parent, dateEnfant) dans Numerotation.kt. Annees seules et dates republicaines comprises (anneeDe) ; une date inconnue ne bloque jamais rien ; meme annee acceptee.
- Toutes les voies d'ajout couvertes : saisie d'un enfant (verifie AVANT toute creation : aucun refus ne laisse de fiche orpheline, l'enfant EXISTANT deja date est couvert), choix d'une fiche existante, nouveau parent (refuse s'il serait ne apres l'enfant), acte de naissance rattache au parent (et au conjoint du parent), import GEDCOM fusionne, rattachement pere/mere d'un acte.
- Le CONJOINT connu est teste au meme titre que la personne dans chaque voie d'ajout.
- Journal - Diagnostic de l'arbre : l'age negatif de la mere n'affiche plus "agee de -5 ans, a verifier" (condition n >= nm) ; les anomalies "ne(e) avant son pere / sa mere" restent signalees pour les dates modifiees apres coup.
- Version 0.52 (versionCode 52).

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
