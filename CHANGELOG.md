# Historique des versions — GénéaPoche

## v0.85 – Exports chiffrés et nettoyage après partage (12.2)

- **Export GEDCOM chiffré (.gpged)** : nouvelle coche « Chiffrer le fichier (.gpged, phrase secrète) » dans le dialogue « Exporter en GEDCOM » (toutes les personnes ou une branche). La phrase secrète (6 caractères min.) est demandée avant le choix du fichier ; le fichier est chiffré AES-256/GCM + PBKDF2 (210 000 itérations), comme une sauvegarde .gpbak.
- **Un .gpged ne s'ouvre que dans GeneaPoche** : à l'import GEDCOM, la phrase secrète est demandée automatiquement ; une phrase erronée n'importe strictement rien. L'avertissement « EN CLAIR » n'apparaît plus quand la coche est activée.
- **Rappel de suppression du fichier envoyé** : après chaque export réussi (GEDCOM .ged ou .gpged, sauvegarde .gpbak), le fichier est mémorisé (nom + date) et le message rappelle de le supprimer une fois envoyé ; à chaque ouverture, un dialogue le rappelle tant que le fichier n'est pas confirmé supprimé (« Fichier supprimé » définitif, « Me le rappeler plus tard » réaffiche à la prochaine ouverture).
- Bumps v0.85 (gradle 85/« 0.85 », export GEDCOM « 2 VERS 0.85 », badge de saisie « v0.85 », écran d'application).

## v0.84 – Ajustements fiches et journal

- **Ordre chronologique réel des événements** : sur la fiche, tous les événements (blocs N/B/M/D/S, événements libres, enfants, mariages et décès de la famille) sont rangés par date complète convertie en clé comparable (aaaammjj, dates républicaines comprises) ; si l'un des événements N, M ou D est sans date, retour à l'ordre naturel NBMDS.
- **Blocs NMD affichés seulement s'ils portent une information** (date, heure, commune, département ou coche « Enregistré ») ; B et S restent affichés dès qu'une donnée existe. La règle des 100 ans ne masque plus les blocs de la fiche.
- **Vivant de moins de 30 ans aujourd'hui** : la fiche n'affiche que la naissance (mariage et décès masqués même si renseignés).
- **Personne décédée avant 25 ans** : pas de bloc mariage sur la fiche.
- **Indicateurs identiques fiche et liste** : sur 6 (naissance, mariage, décès : date + commune complète) pour tous, sur 4 (naissance et décès) pour une personne décédée avant 25 ans, sur 2 (naissance) pour un vivant de moins de 30 ans ; le mariage est retiré du compte (total 4) s'il n'y a pas de conjoint.
- **Journal** : plus de recherche de mariage, ni de conjoint, ni d'enfants pour un vivant de moins de 30 ans ni pour une personne décédée avant 25 ans.
- Bumps v0.84 (gradle 84/« 0.84 », export GEDCOM « 2 VERS 0.84 », badge de saisie « v0.84 », écran d'application).

## v0.83 – Actes : coche « Enregistré » (12.0)

- **Le stockage des actes est supprimé de l'application** : plus de photos d'actes, de transcriptions ni de fichiers actes.bin chiffrés — les documents restent conservés ailleurs (archives, PC, cloud). Les anciens fichiers sont purgés proprement à la migration.
- **Coche « Enregistré » par type d'événement (N, B, M, D, S)** : sur la fiche, dans l'en-tête de chaque bloc d'événement ; et dans le formulaire de saisie. Elle signale un acte conservé ailleurs, sans encombrer la poche.
- **Migration automatique** : à la mise à jour, toute personne qui avait un acte stocké reçoit la coche « Enregistré » du type correspondant ; l'import d'une ancienne sauvegarde .gpbak (avec actes) pose aussi les coches.
- **Journal** : l'absence de coche devient une recherche « Enregistrer [type] de … » ; la recherche « acte manquant » s'appuie désormais sur la coche.
- **Champs de preuve supprimés** (dépôt, côte AD, consulté le, niveau de preuve) : le système complet des sources sera revu plus tard.
- **Correctif « Signe »** : la coche Signe du formulaire de saisie disparaissait après enregistrement — les signataires (père, prêtre, témoin…) sont désormais réellement persistés sur les fiches, comme l'enfant déclarant l'était déjà.
- Bumps v0.83 (gradle 83/« 0.83 », export GEDCOM « 2 VERS 0.83 », badge de saisie « v0.83 », écran d'application).

## v0.82 – Sauvegarde automatique et clé de récupération (12.1)

- **Sauvegarde automatique .gpbak** : nouvelle entrée « Sauvegarde automatique… » dans la feuille des paramétrages — choisissez un dossier, et une copie chiffrée de toute la base y est créée automatiquement après N modifications enregistrées (25 par défaut, réglable de 5 à 500). Sans dossier choisi, un simple rappel s'affiche au franchissement du seuil.
- **Clé de récupération à imprimer** : générée par l'application à l'activation (20 caractères sans caractères ambigus, en groupes de 5), affichée dans le dialogue avec un bouton « Copier la clé », à conserver hors de l'appareil. Elle protège toutes les sauvegardes automatiques : sans elle, personne ne peut les déchiffrer, même sur un autre appareil. Elle n'est jamais stockée en clair (chiffrée avec la clé maître) et reste consultable via « Afficher la clé de récupération ».
- Première sauvegarde immédiate dès l'activation ; restauration par l'entrée existante « Restaurer une sauvegarde (.gpbak) » en saisissant cette clé.
- Le compteur n'est incrémenté que par les enregistrements qui réécrivent réellement un fichier (une simple consultation ne compte pas).
- Bumps v0.82 (gradle 82/« 0.82 », export GEDCOM « 2 VERS 0.82 », badge de saisie « v0.82 », écran d'application, README).

## v0.81 – Déverrouillage accéléré

- **Ouverture environ deux fois plus rapide après la saisie du code PIN** : la clé maître est désormais ouverte en premier (un seul calcul PBKDF2 de 600 000 itérations) — si son déchiffrement réussit, le code est prouvé correct, le second calcul de vérification est économisé ; en cas d'échec, l'ancien chemin complet (vérification puis migration éventuelle) reprend exactement comme avant, sans aucune perte de sécurité.
- **Premier écran affiché sans attendre** : la reconstruction du cache d'enregistrement (les trois fichiers JSON) passe en arrière-plan au déverrouillage, au lieu de bloquer le premier rendu.
- **Indicateur « Déverrouillage en cours… »** sur l'écran du PIN pendant le calcul.
- Bumps v0.81 (gradle 81/« 0.81 », export GEDCOM « 2 VERS 0.81 », badge de saisie « v0.81 », écran d'application, README).

## v0.80 – Suspend levés : conjoint veuf/en vie persisté, enfant déclarant sur décès

- le choix « veuf / en vie » du conjoint sur un acte de décès ou de sépulture est désormais mémorisé : la transcription affiche « Conjoint survivant : … » ou « Conjoint décédé avant : … », et si le conjoint est décédé avant, sa fiche reçoit « avant aaaa » comme date de décès (qualification BEF à l'export GEDCOM) ;
- « Enfant déclarant » : sur un acte de décès, un picker compact (identifiant unique) liste les enfants de la personne ; l'enfant choisi est mémorisé comme signataire du décès sur sa fiche et relu à la ré-édition de l'acte ;
- correctif v0.77 : la nouvelle fiche créée au fil d'une saisie d'acte (persNouvelle) n'était jamais transmise à l'enregistrement — corrigé.

## v0.79 – Listes de choix : identifiant unique partout

- Toutes les listes de choix de l'application (choisir le conjoint, un enfant, le père, la mère, le de cujus, rattacher un acte, branche d'export, ligne de vie, fenêtre d'homonymes) affichent désormais l'identifiant UNIQUE : nom, prénoms, N. date de naissance, D. date de décès (règle des 100 ans respectée) — deux homonymes ne se ressemblent plus.
- Listes très compactes (une ligne par personne, interligne 16 sp, padding nul) et alignées à gauche, avec un gabarit unique (helper libelleUnique) pour toute l'application.

## v0.78 – Doublons du journal comparables

- Diagnostic de l'arbre (onglet Journal) : chaque doublon affiche désormais les dates de naissance (N.) et de décès (D.) des DEUX fiches, ligne par ligne, pour comparer avant de décider ; chaque ligne est cliquable vers la fiche.
- Décision de l'utilisateur : « Fusionner (garder 1) » ou « Fusionner (garder 2) » (fusion guidée sans perte, inchangée), ou « Garder les deux » — décision définitive et mémorisée, la paire ne réapparaît plus dans le diagnostic.

## v0.77 – Homonymes, frise B, interlignes

- Un VRAI doublon = même nom, mêmes prénoms ET même date de naissance : deux dates différentes = homonyme, et une nouvelle fiche peut enfin être créée partout (fenêtre de choix « Créer une nouvelle fiche » : personne concernée, parrain/marraine, témoins, dialogue +, enfant).
- Frise : le baptême où la personne est parrain ou marraine affiche la date en tête et l'âge entre parenthèses, comme tous les autres événements.
- Listes de choix : interlignes réduits, toujours alignées à gauche (enfants, homonymes, personnes).

## v0.76 – Parrains, homonymes et coche Signe

- Les baptêmes où la personne est parrain ou marraine apparaissent désormais dans la frise de sa fiche (carte orange « B – Baptême de … (parrain/marraine) », cliquable pour ouvrir l'acte).
- Fini la rafale de puces : un homonyme (même nom ET mêmes prénoms) est proposé dans une fenêtre de choix avec 
ses dates – choisir la fiche existante ou créer une nouvelle fiche (personne concernée, parrain/marraine et témoins).
- Liste des enfants (picker) condensée et alignée à gauche.
- La coche « Signe » est enfin alignée sur le nom : colonne calquée sur la hauteur du champ (56 dp), libellé en tête, sur parents, parrain/marraine, témoins et prêtre/officier.

## v0.75 — Urgences

- Un acte de baptême, décès ou sépulture ENREGISTRÉ reste désormais visible dans la frise des événements de la fiche, même si sa date n'est pas reportée sur les champs de la fiche (bug : un acte B enregistré pouvait disparaître de la frise).
- Tous les homonymes sont proposés systématiquement avec leur date de naissance (et de décès) : nouveau bloc de puces sous les champs Nom/Prénoms de la personne concernée, puces parrain/marraine et témoins élargies, listes de choix enrichies des dates — aucun doublon ne se crée en silence.
- Coche « Signe » descendue d'une ligne pour être alignée sur le nom (parents, parrain/marraine, témoins, prêtre/officier d'état civil).

## v0.74 - Officier d'état civil sur naissance, mariage et décès

- L'officier d'état civil figure désormais sur les actes de NAISSANCE, MARIAGE et DÉCÈS (il signe tout acte d'état civil), avec nom, prénoms et profession ; le PRÊTRE reste sur le baptême et la sépulture (sans profession).
- Le rôle officiel (prêtre ou officier) est PRÉ-REMPLI à l'édition d'un acte existant (il n'était pas rechargé).
- Cohérence et lien de témoin déjà en place (v0.73) s'appliquent à l'officier : fiche existante réutilisée ou créée, liée à l'acte.
- README mis à jour avant la mise à disposition de la release.

## v0.73 - Petites retouches (2e salve)

- Ordre NBMDS prioritaire : sur une fiche, les blocs N, B, M, D, S s'affichent TOUJOURS dans cet ordre (le tri chronologique de la v0.72 avait repris la main).
- Indicateurs synchronisés entre la fiche et la liste des noms (sur 6, sur 4 pour une personne supposée en vie, sur 2 pour un mineur).
- Mineur : l'indic
ateur sur 2 compte désormais TOUTE date de naissance renseignée (même incomplète) avec la commune.
- Saisie : case département réduite (2 chiffres) et « Consulté le » raccourci (jj/mm/aaaa), la commune s'élargit sur la même ligne.
- Coche « Signe » : intitulé affiché, cadre aligné sur la ligne nom/prénoms/profession de chaque personne citée.
- Bug corrigé : la correction d'une commune ou d'un département en modification d'acte reste désormais enregistrée (champ vide = efface, comme documenté).
- Acte de baptême refait : deux lignes fixes Parrain et Marraine (nom + prénoms, sans profession), prêtre avec nom et prénoms sans profession.
- Acte de sépulture : prêtre avec nom et prénoms, sans profession (libellé « PrÊtre » corrigé).
- Cohérence étendue : les témoins, parrain/marraine, prêtre et officier sont vérifiés dans la base ; une fiche existante correspondante est proposée dans la saisie, sinon la fiche est créée et liée à l'acte.
- README mis à jour avant la mise à disposition de la release.

## v0.72 – Petites retouches : ordre NBMDS, indicateurs 4/2, coches de signature

Sept demandes utilisateur après la v0.71 :

- **Ordre NBMDS prioritaire** : sur une fiche, les blocs suivent désormais Naissance, Baptême, Mariage, Décès, Sépulture même si le classement chronologique disposerait autrement — l'ordre NBMDS l'emporte.
- **Fin des informations parasites** : les entrées de chronologie (mariages et naissances des frères et soeurs) antérieures à la naissance ou postérieures au décès de la personne ne s'affichent plus sur sa fiche.
- **Indicateur sur 4 (et non plus 6) pour les personnes supposées en vie** (moins de 100 ans, pas de décès renseigné) : date de naissance, lieu de naissance complet, date de mariage, lieu de mariage complet.
- **Indicateur sur 2 pour les mineurs supposés en vie** (moins de 18 ans) : date de naissance et commune de naissance seulement.
- **Case département rétrécie** : la colonne département (2 chiffres) est resserrée au profit de la commune,
 sur la même ligne.
- **La zone « Qui signe ? » disparaît de tous les actes** : à la place, une coche « Signe » derrière chaque personne citée — père (N/B), mère, prêtre, parrain et marraine (B), époux, épouse, les quatre parents et les témoins (M), officier d'état civil avec sa profession (M, géré comme un témoin portant son rôle), conjoint survivant déclarant et témoins (D), prêtre et témoins (S).
- **Liste « Personne concernée » resserrée et alignée à gauche** dans le sélecteur.
- Bumps 0.72 (gradle 72/« 0.72 », export GEDCOM « 2 VERS 0.72 », badge de saisie « v0.72 », écran d'application).

## v0.71 – Zéro d'office sur les dates : le format tient dans le temps

La détection de dates invalides (v0.69) était la bonne idée mais ne tenait pas dans le temps : après correction, sauvegarde, export GEDCOM puis réimport, le problème revenait. Cause : l'import GEDCOM fabriquait « 5/10/1890 » sans le zéro d'office, aussitôt re-signalé invalide au tour suivant.

- **Zéro d'office, TOUTE l'application** : nouvelle fonction normaliserDate — « 5/7/1890 » ou « 5.7.1890 » devient « 05/07/1890 » dès l'enregistrement ; normaliserHeure fait de même (« 9:05 » → « 09:05 »).
- **Trois points de passage obligatoires couvrent tout** : dateGreg (toute date d'acte), normaliserLieux (les cinq événements + événements libres, à chaque enregistrement : les dates incomplètes déjà présentes dans la base se réparent d'elles-mêmes) et dateFrancaise (« 5 OCT 1890 » → « 05/10/1890 » à l'import GEDCOM, fin de la boucle infernale).
- **Dates consultées comprises** : dateConsultation des actes normalisée aussi à l'enregistrement et au chargement JSON.
- **Qualificateurs GEDCOM légitimes** : « vers 1890 », « avant 1890 », « après 1890 » et « entre 1890 et 1895 » (ABT/BEF/AFT/BET relus de l'export) ne sont plus signalés comme des dates fausses : la norme GEDCOM est respectée.
- Bumps 0.71 (gradle 71/« 0.71 », export GEDCOM « 2 VERS 0.71 », badge de saisie « v0.71 », écran d'application).

## v0.70 – Sai
sie ultra-compacte, mère non signataire, conjoint veuf ou en vie, B/S masqués

Quatre demandes utilisateur à la suite de v0.69 : le formulaire ne tient toujours pas sur une page (cadres trop grands, département en tête) ; la mère ne signe jamais un acte de naissance ; le conjoint d'un défunt peut être décédé avant lui ou encore en vie ; ne pas afficher B et S sur la fiche par défaut.

- **Champs ULTRA-COMPACTS partout** : le composable Champ est réécrit — fini le grand OutlinedTextField (~64 dp), place à un cadre mince (~36 dp) : libellé 10 sp au-dessus, texte 14 sp, bordure fine arrondie, interligne 1 dp. Environ 25-30 dp gagnés PAR champ : un acte de naissance complet (personne + parents + témoins + signataires) tient enfin sur une page.
- **N : la mère ne signe jamais l'acte** — la puce « Mère » disparaît des signataires possibles sur un acte de naissance, et toute signature « Mère » préexistante est filtrée à l'enregistrement.
- **D/S : conjoint veuf ou en vie** — le bloc « Conjoint survivant » devient « Conjoint » avec deux puces : « En vie » (survivant, déclarant) ou « Décédé avant (veuf/veuve) » ; le libellé du champ de nom s'adapte.
- **Fiche : B et S masqués par défaut** — les blocs Baptême et Sépulture ne s'affichent que si l'acte porte une donnée (date, heure, lieu ou département).
- Bumps 0.70 (gradle 70/« 0.70 », export GEDCOM « 2 VERS 0.70 », badge de saisie « v0.70 », écran d'application).

## v0.69 – Saisie compacte sur une page + alertes dates hors format partout

Les deux demandes restantes de la revue NBMDS : formulaire plus compact (« tout voir pratiquement sur une page »), département toujours sur 2 chiffres, et contrôle du format de date PARTOUT (ancien GEDCOM ou champ non contrôlé).

- **Badge de version en tête du formulaire** : un petit « v0.69 » figure à côté du titre « Saisie d'un acte » — preuve immédiate de la version installée.
- **Formulaire resserré** : marges générales 12 → 8 dp, espacement des titres de section 12 → 6 dp, interligne
 des champs 3 → 2 dp — l'essentiel tient sur une page.
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
- **Blocs Père/Mère adaptés au type** : acte de naissance ou baptême = identité des parents en une seule ligne (nom, prénoms, profe
ssion) ; décès ou sépulture = sans naissance des parents (décès mentionné) ; mariage/événement = bloc complet (naissance, lieu, département, profession, décès).
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
- **Identifiants des parents transmis** (`pereId`/`mereId`) : le formulaire sait pour quelle fiche parent il est ouvert ; `trouverO
uCreer` garde le lien existant si le nom saisi est resté celui de la personne liée (fin des détournements d'homonymes — la belle-mère ne peut plus être choisie à la place de la mère).
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
- Formulaire de fiche : valeurs initiales capturee
s une seule fois a l'ouverture (plus de reset pendant la saisie).
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

- **Modification d'une fiche : plus aucune donnée d'une autre personne** : les 24 champs du dialogue d'édition sont mémorisés par l'identifian
t de la fiche (`remember(cible?.id)`) et non plus par l'égalité de contenu ; le dialogue est entièrement reconstruit à chaque ouverture ou changement de cible (`key`) ; `construire()` ne mute plus la fiche en place (il travaille sur une copie) et la fiche modifiée est réellement remplacée dans la base à l'enregistrement.
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
- À l'ouverture (après déverrouillage) : « Rappels 
: N recherche(s) prioritaire(s) à faire (onglet Journal). » (même règle des 75 ans que le Journal).
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

- Bug reproduit : un enfant ne avant la naissance de la personne 
selectionnee pouvait encore etre ajoute.
- UNE SEULE regle anti-incoherence : incoherenceParente(parent, dateEnfant) dans Numerotation.kt. Annees seules et dates republicaines comprises (anneeDe) ; une date inconnue ne bloque jamais rien ; meme annee acceptee.
- Toutes les voies d'ajout couvertes : saisie d'un enfant (verifie AVANT toute creation : aucun refus ne laisse de fiche orpheline, l'enfant EXISTANT deja date est couvert), choix d'une fiche existante, nouveau parent (refuse s'il serait ne apres l'enfant), acte de naissance rattache au parent (et au conjoint du parent), import GEDCOM fusionne, rattachement pere/mere d'un acte.
- Le CONJOINT connu est teste au meme titre que la personne dans chaque voie d'ajout.
- Journal - Diagnostic de l'arbre : l'age negatif de la mere n'affiche plus "agee de -5 ans, a verifier" (condition n >= nm) ; les anomalies "ne(e) avant son pere / sa mere" restent signalees pour les dates modifiees apres coup.
- Version 0.52 (versionCode 52).

## v0.51 – GénéaPoche 11 : coche d'identité, diagnostic de l'arbre, sources et preuves

- **11.0 Vérification identité** : coche sur chaque fiche, à droite du nom et du score n/6. Coche automatique si au moins 1 renseignement sur 6, croix rouge sinon ; le choix manuel reste possible (auto → coche → croix en touchant l'icône), même si l'acte est incomplet. Le journal liste les identités non certifiées : la coche sert aussi aux recherches.
- **11.2 Diagnostic complet de l'arbre** : nouvel encadré « Diagnostic de l'arbre » dans le journal — anomalies de cohérence (décès avant naissance, âges impossibles…), **doublons** (même nom et mêmes prénoms) avec **fusion guidée** : garder l'une ou l'autre fiche, toutes les informations et tous les liens (parents, conjoints, enfants, témoins, actes, recherches) sont reportés sans jamais écraser une donnée déjà présente ; **branches isolées** (personnes sans aucun lien de parenté) ; tout est cliquable vers la fiche.
- **11.4 Sources et niveaux de preuve** : su
r chaque acte, en plus de la côte AD — dépôt de conservation, date de consultation et niveau de preuve 0–3 (échelle QUAY standard, comme Heredis et Gramps : 0 indirecte, 1 plausible, 2 fiable, 3 acte original). Affichés dans le sous-bloc de l'acte sur la fiche et exportés en GEDCOM (`AGNC` dépôt, NOTE date de consultation, `QUAY` sur chaque citation).

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
- Version 0.49 
(versionCode 49).

## v0.48 – GénéaPoche 9 : synchronisation fiche / arbre / recherches

- 9.1 L'icône Arbre est supprimée de la fiche.
- 9.2 L'arbre généalogique (et la ligne de vie) se synchronisent AUTOMATIQUEMENT avec la personne de la fiche ouverte : plus besoin d'aucun bouton.
- 9.3 Le bloc Recherches de la fiche est synchronisé avec le journal : le texte de chaque recherche est recalculé exactement comme dans le journal.
- 9.4 Chaque recherche de la fiche est CLIQUABLE : toucher « Compléter le lieu de mariage » ouvre directement la saisie du mariage ; « Rechercher son père » ouvre le choix du père ; « Rechercher les enfants » ouvre « Ajouter un enfant », etc.
- 9.5 La note de recherche saisie dans la fiche (bloc Recherches et sous-blocs d'actes) est la MÊME que celle du journal : liaison directe, toute saisie d'un côté se retrouve de l'autre après enregistrement.
- Version 0.48 (versionCode 48).

## v0.47 – GénéaPoche 8 : onglet « Ligne de vie »

- Le menu Frise est refondu intégralement et devient « Ligne de vie » : une ligne de vie horizontale par membre de la famille proche de la personne choisie (grands-parents, parents, frères et sœurs, enfants), sur un axe d'années commun — tout tient à l'écran, sans zoom ni déplacement.
- Chaque trait va de la naissance au décès, ou jusqu'à aujourd'hui si la personne est vivante (règle des 100 ans respectée : un décès non certain n'est jamais tracé).
- Un point marque chaque événement connu (N, B, M, S, D, événements libres) ; lignes fines par décennie et trait vert vertical à l'année en cours ; toucher une ligne ouvre la fiche.
- Numéro de version désormais affiché dans l'application : sous le titre de la barre du haut et dans les paramétrages.
- Version 0.47 (versionCode 47).

## v0.46 – GénéaPoche 7 : chronologie unique de la fiche

- 7.1 Bouton « Ajouter un enfant » sur la fiche : choisir une fiche existante ou saisir un nouvel enfant.
- 7.2 Ordre STRICTEMENT chronologique de TOUS les événements de la fiche (N/B/M/S/D, événements libres, événements de la famille, naissances et décès des enfants) dans une seule et même frise.
- 7.3 Âge entre parenthèses sur chaque événement (précision jours/mois/ans), y compris l'âge du parent pour les enfants.
- 7.4 La date figure en TÊTE de chaque ligne, au format jj/mm/aaaa ; les événements de famille prennent le même format.
- 7.5 Dates grégoriennes alignées : une année seule s'affiche «   /  /aaaa » ; les dates républicaines sont converties en grégorien avec l'original entre parenthèses.
- Bloc séparé événements de famille supprimé (fusionné dans la chronologie).
- Version 0.46 (versionCode 46).

## v0.45 – APK de release nommés avec la version

- Les fichiers téléchargeables d'une release s'appellent désormais **GeneaPoche-v0.45-release.apk** (a installer) et **GeneaPoche-v0.45-debug.apk**, le numéro de version étant déduit automatiquement du tag.
- Aucun changement dans l'application elle-même (même code que la v0.44).

## v0.44 – GénéaPoche 6 : l'arbre montre les enfants

- **Arbre** : rangée des enfants sous le couple central (liaisons en T), la même présentation que les autres rangées.
- Toujours 4 générations (arrière-grands-parents, grands-parents, parents, couple central) + les enfants ; toucher une carte la rend centrale immédiatement (zoom/pan réinitialisés), appui long = fiche.
- **Nouveau bouton Arbre** dans la barre du haut de la fiche : la personne devient le centre de l'arbre et l'onglet Arbre s'ouvre, synchronisation instantanée.

## v0.43 - Export GEDCOM verrouillé par le sélecteur de fichiers (fix export à 0 personne)
- **Correctif majeur** : lancer un export GEDCOM ouvrait le sélecteur de fichiers, ce qui verrouillait l'app et vidait la mémoire (purge de sécurité) ; l'export s'exécutait alors sur une liste VIDE : « Aucune personne à exporter » et fichier créé vide (0 Ko). Un sélecteur de fichiers ouvert (GEDCOM, .gpbak) suspend désormais le re-verrouillage automatique.
- Export : refus AVANT d'ouvrir le sélecteur si la base est vide (plus jamais de fichier 0 Ko créé) ; message de refus explicite.
- Import : message distinct « Fichier vide (0 octet) : ancien export refusé ou vide » au lieu d'« illisible ».
- Version 0.43 (versionCode 43).

## v0.42 - Secours copie de secours (.bak)
- Nouveau menu « Restaurer la copie de secours (.bak) » : dernier recours quand la base est vide — restaure les copies de rotation personnes.bin.bak / journal.bin.bak / actes.bin.bak, sans aucun risque pour les fichiers actuels.
- Version 0.42 (versionCode 42).

## v0.41 - Secours base vide
- Restauration RESCUE : « Restaurer la base d’avant import » essaie les 5 sauvegardes tournantes de la plus récente à la plus ancienne et garde la première qui contient des personnes.
- Export GEDCOM : plus jamais de fichier vide — un export de 0 personne est refusé avec un message explicite.
- Version 0.41 (versionCode 41).

## v0.40 – Fiche et arbre corrigés (2 novembre 2026)

- **Fiche** : le cadre **NAISSANCE est TOUJOURS en tête**, quelles que soient les dates (la chronologie générale le re-triait par année).
- **Fiche** : **tous les cadres sont REPLIÉS sur une ligne** à l'ouverture de la fiche ; toucher un cadre le développe, l'appui long ouvre la saisie pré-remplie.
- **Arbre** : à chaque toucher d'une carte, la personne devient le centre ET la vue (zoom/déplacement) se réinitialise — l'arbre se régénère nettement sur 4 générations. Appui long = ouverture de la fiche.

## v0.39 (2 novembre 2026)

- **Import/export GEDCOM fiabilisé** : sauvegardes d'avant import tournantes (les 5 dernières), avertissement avant tout export ou remplacement, dialogue d'import clarifié.
- **Fiche** : le cadre NAISSANCE est TOUJOURS en tête ; tous les cadres sont REPLIÉS sur une ligne à l'ouverture (toucher = développer, appui long = modifier via la saisie pré-remplie).
- **Arbre** : à chaque toucher d'une carte, la personne devient le centre et la vue (zoom/déplacement) se réinitialise : l'arbre se régénère nettement sur 4 générations. Appui long = fiche.

## v0.38 (« GénéaPoche 5 » : normes d'écriture, arbre 4 générations)

- **Communes normalisées** : initiale en majuscules (norme française), automatiquement à la saisie, à l'édition, à l'import et au chargement (« le havre » → « Le Havre »).
- **Départements au format numérique** (norme GEDCOM), 2 ou 3 chiffres : « Nord » → « 59 », « 5 » → « 05 ».
- **Icône de paramétrages** : l'intitulé « GEDCOM » de la barre du haut est remplacé par une roue crantée.
- **Personnes** : pastille VIDE si la personne n'est ni Sosa ni d'Aboville ; étiquette à droite n/6 (N date complète, N commune, N département, D date complète, D commune, D département), rouge (0) → vert (6).
- **Fiche** : cadre de la personne supprimé (l'identité est dans la barre de titre) ; profession des parents sans étiquette dans le cadre des parents ; bloc Naissance toujours en tête, croix père/mère supprimées ; bloc Mariage allégé (plus de croix, ni « Autre conjoint », ni « Saisir un mariage ») ; le bloc « Biographie » devient **« Notes »** ; modification par appui long = le menu de Saisie pré-rempli (parents et conjoint inclus).
- **Arbre : retour à 4 générations** (8 arrière-grands-parents → 4 grands-parents → parents → personne centrale + conjoint) ; toucher une carte la rend immédiatement centrale ; appui long ouvre la fiche ; zoom et déplacement conservés.

## v0.37 (arbre épuré et illimité)

- **Arbre : suppression des boutons « Centré sur… » et « Vue initiale »** : l'arbre s'affiche directement, la navigation reste au doigt (pincement = zoom, glissement = déplacement).
- **Arbre : plus aucune limite** dans le plan des familles (fin du plafond de 25 générations de descendants) : toutes les personnes de l'arbre apparaissent, en plus de toutes les générations d'ascendants déjà affichées depuis la v0.36.
- **Fiche : le bloc Naissance est toujours en tête** de la frise chronologique, quelles que soient les dates.
- **Fiche : le bloc de saisie libre devient le bloc « Biographie »**, placé tout en bas de la fiche.
- GEDCOM : version réelle 0.37.

## v0.36 (« GénéaPoche 4 » : fiche épurée et arbre complet)

- **Fiche : le bloc Naissance est toujours affiché avant le bloc Mariage**, quelles que soient les dates.
- **Plus d'âge à la naissance** dans la ligne de la frise (l'âge reste indiqué pour les autres événements).
- **Bloc « Ajouter un enfant » supprimé** (doublon avec « Ajouter un événement » et le menu +Saisie).
- **Blocs Naissance, Mariage et Décès ouverts par défaut** à l'ouverture de la fiche.
- **Blocs épurés** : suppression de la case de validation, de l'icône + (photo), de la coche « Signe » et de la section des témoins — les témoins, signatures et photos se saisissent uniquement par le formulaire d'acte (appui long).
- **Appui long sur un bloc = formulaire pré-rempli** (noms et prénoms des parents, date, heure, commune, département) et **limité à ce seul type d'acte** : modifier la ligne d'un décès n'ouvre que le formulaire décès.
- **Écran « Page et référence de l'acte » supprimé**.
- **Frise familiale** : « Mariage de… » remplace « Assiste au mariage de… » (plus de présomption de présence).
- **Arbre : TOUTES les générations affichées**, sans plafond (fin de la limite de profondeur).
- GEDCOM : version réelle 0.36.

## v0.35 (arbre complet et frise chronologique)

- **Arbre absolument complet, version verticale** : toutes les générations d'ancêtres et **tous les latéraux** (fratries, descendants, conjoints) sur un seul grand arbre déroulant à partir de la personne choisie ; **navigation par zoom (pincement) et déplacement** (glissement), bouton « Vue initiale » pour revenir au centre ; garde-fous anti-cycles et profondeur maximale pour rester fluide.
- **Suppression de l'ancienne vue horizontale sur 4 générations**, devenue obsolète.
- **Nouvel onglet « Frise »** : frise chronologique verticale, **une colonne par personne vivante de la branche** (la personne choisie garde toujours sa colonne, même décédée) ; axe vertical des années de 10 en 10 avec **ligne fine par décennie** ; échelle basée sur la vie : naissance tout en bas, décès ou naissance + 100 ans en haut.
- **Étiquette pour chaque événement** de la personne choisie (naissance, baptême, mariage, sépulture, décès, événements libres, naissances des enfants) placée à la bonne hauteur dans sa colonne.
- Trait de vie **bleu pour les hommes, rose pour les femmes** ; nom au sommet de chaque trait ; zoom et déplacement identiques à l'arbre.
- GEDCOM : version réelle 0.35.

## v0.34 (« GénéaPoche 3 » : chronologie unique, menu Saisie refondu, compteur de renseignements)

- **Bandeau réduit et figé** sur tous les écrans : icône et titre plus compacts, toujours visibles pendant le défilement.
- **Compteur de renseignements dans la liste Personnes** : « n/4 » tout à droite du nom, couleur du rouge (rien) au vert (complet) ; une date complète exige le jour, le mois et l'année (une année seule est incomplète) et un lieu complet exige la commune avec un département identifiable.
- **Fiche : cartouche identité + parents figé** (il reste visible en haut pendant le défilement), badge « n/6 » dans le titre (naissance et décès : date, commune, département).
- **Édition par appui long** de l'identité (nom, prénoms, profession) ; suppression de l'indice « Appui long sur… » et de l'étiquette « Identité ».
- **Père et mère dans un seul cadre**, affichés comme les enfants (même police, numéros cliquables) ; la pastille renvoie vers leur fiche ; appui long = choisir ou créer, avec synchronisation complète.
- **Chronologie unique** : naissance, baptême, mariage, décès, sépulture, événements libres et enfants sont tous rangés par date, avec **l'âge entre parenthèses** pour chaque événement.
- **Mariage** : plus de « présent et consentant » automatique ; seules les mentions réellement transcrites sur l'acte sont enregistrées.
- **Menu Saisie refondu** : 7 entrées avec icônes (N, B, M, S, D, Événement, Enfant) ; la personne existante est proposée systématiquement et l'acte existant est édité (aucun doublon) ; le mariage traite bien les deux conjoints.
- **Périodes réalistes indiquées** sous le champ date (baptême dans les 2 ans, mariage 16-40 ans, sépulture dans l'année du décès…) et **avertissements de cohérence non bloquants** : « Enregistrer quand même » reste possible.
- **Événements libres** : intitulé, date, heure, commune, département, édition et suppression.
- **GEDCOM** : export au choix 5.5.1 ou 7.0.18 ; import avec **fusion sans doublon** (complétion des fiches existantes, ajout des absents, liens recalés) ou remplacement ; version réelle 0.34.

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
