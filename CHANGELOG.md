# Historique des versions — GénéaPoche

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
- **Journal : nouvel encadré « À corriger »** : il liste TOUTES les anciennes saisies non conformes — lieux sans code INSEE (bouton **Corriger** quand la partie « commune » matche une seule entrée de la base : normalisation en un clic, champ Département mis à jour ; sinon bouton **Fiche** pour ressaisir) et dates invalides (accès direct à la fiche).
- Le champ Département est conservé tel quel (auto-rempli par la commune choisie).
- Tests unitaires étendus (couperLieu avec code INSEE et France, sansPays, lieuGedcom, lieuAffiche).
- Bumps v0.90 (gradle 90/« 0.90 », export GEDCOM « 2 VERS 0.90 », badge de saisie « v0.90 », écran d'application).
## v0.89 – Certification conservée en GEDCOM, conjoints dans la ligne de vie

- **La coche « personne certifiée » survit à l'export/réimport GEDCOM** : la certification manuelle (coche ou croix de l'en-tête de fiche) était stockée dans la base chiffrée mais jamais écrite dans le fichier GEDCOM — elle repassait donc à « auto » au réimport. Désormais chaque fiche concernée emporte un tag dédié (`_GPVERIF`), relu à l'import ; les autres logiciels (Heredis…) ignorent simplement ce tag. Le mode « auto » reste recalculé automatiquement.
- **Ligne de vie : les conjoints sont affichés** — tous les conjoints de la personne choisie (Épouse/Époux, numérotés s'il y en a plusieurs) occupent leur ligne de vie juste avant la sienne, avec leurs propres événements.
- Bumps v0.89 (gradle 89/« 0.89 », export GEDCOM « 2 VERS 0.89 », badge de saisie « v0.89 », écran d'application).


## v0.88 – Base des communes (12.4) et anomalies à vérifier

### 12.4 – Base hors ligne des communes et saisie assistée

- **Base hors ligne des communes françaises** : les 34 015 communes du code officiel géographique (métropole, Corse 2A/2B, DOM) sont intégrées dans l'application — un fichier texte par département (« code INSEE|nom »), chargé en tâche de fond à la première saisie d'un lieu. Aucune permission, aucun accès réseau.
- **Saisie assistée des lieux** : pendant la frappe d'un champ de lieu (naissance, baptême, mariage, décès, sépulture, événement libre, bloc édité depuis la fiche, et dans le formulaire d'acte : commune de l'acte, lieu de naissance et lieu de décès des personnes citées), jusqu'à six communes sont proposées — recherche insensible aux accents, par début de nom, n'importe où dans le nom, ou par code INSEE.
- **Format Heredis à la sélection** : le champ reçoit « Commune, Code INSEE, Département, France » (ex. « Arvert, 17010, Charente-Maritime, France ») et le champ « Département » est rempli automatiquement ; le lieu-dit ou la paroisse se tape devant la commune, entre crochets (« [St Étienne] – Arvert, … ») et est conservé à chaque nouvelle sélection.
- **Département reconnu depuis le code INSEE** : un code INSEE ou postal à 5 chiffres dans un lieu (« 17010 » → 17) est désormais identifié comme département (recherche et suggestions du journal, alertes locales, cohérence des fiches).
- Anciens noms et fusions de communes : reportés à une prochaine version (décision).

### Anomalies à vérifier (cohérence de l'arbre)

- **Nouvelle section « Anomalies à vérifier »** dans le diagnostic de l'arbre (onglet Journal), qui remplace l'ancien contrôle « Cohérence ». Le mot « erreur » est volontairement évité : ce sont des points à vérifier, et c'est vous qui jugez. Exemple : « ⚠️ Jean Dupont est né 4 mois après le décès supposé de son père Pierre Dupont (naissance posthume possible). »
- **Trois boutons sous chaque anomalie** : **Ignorer** (l'anomalie disparaît, choix mémorisé ; « Réafficher les anomalies ignorées » les rétablit ; elle revient d'elle-même si les dates changent), **Corriger** (ouvre la fiche concernée) et **Examiner** (explication de la règle, dates saisies de chaque personne concernée, accès à chaque fiche).
- **Contrôles ajoutés** : enfant né avant son père ou sa mère ; mère trop jeune (moins de 15 ans) ou trop âgée (plus de 50 ans) ; père trop jeune (moins de 14 ans) ou trop âgé (plus de 80 ans) ; naissance après le décès de la mère ; naissance posthume possible (jusqu'à 300 jours après le décès du père) ou impossible au-delà ; mariage après le décès de la personne ou de son conjoint ; mariage avant la naissance de la personne ou de son conjoint ; mariage à un âge improbable (moins de 15 ans, plus de 85 ans) ; décès avant naissance ; baptême avant la naissance ou après le décès ; sépulture avant le décès ; événement libre avant la naissance ; âge de plus de 110 ans ; deux dates de mariage différentes sur les fiches d'un même couple ; enfant né avant le mariage de ses parents ; frères et sœurs de même mère nés à moins de 7 mois d'intervalle (jumeaux exclus) ; personne à la fois père et mère, ou père de sexe féminin, mère de sexe masculin ; numéro de département inexistant ; deux événements le même jour dans deux départements différents.
- **Pas de fausse alerte par imprécision** : une anomalie n'est signalée que si la contradiction est certaine compte tenu de la précision des dates (une année seule couvre toute l'année, « avant 1850 », « après 1850 » et « vers 1750 » sont compris, les dates républicaines sont converties).
- **Doublons potentiels mieux repérés** : comparaison insensible aux accents, à la casse et à la ponctuation, et même nom + même premier prénom + même année de naissance.
- 16 nouveaux tests unitaires JVM. Les anomalies ignorées sont mémorisées sous forme de clés techniques (aucun nom, aucune date).
- Bumps v0.88 (gradle 88/« 0.88 », export GEDCOM « 2 VERS 0.88 », badge de saisie « v0.88 », écran d'application).

## v0.87 – Boîte à outils du généalogiste

- **Nouvelle « Boîte à outils »** (paramétrages ▸ « Boîte à outils ») : trois outils de recherche, entièrement locaux (aucun accès réseau, aucune nouvelle permission).
  - **Calendrier républicain ↔ grégorien** : saisir « 14 Vendémiaire an VII » ou « 5 octobre 1798 » donne l'autre date, le jour de la semaine (vendredi), le nom du jour républicain, la décade, la fête du jour (noms de la table officielle), l'an sextile ou commun, les années bissextiles et la date du calendrier julien (24 septembre 1798). Jours complémentaires gérés, bornes réelles du calendrier (22/09/1792 – 31/12/1805).
  - **Calculateur de dates** : âge ou écart entre deux événements (« 12 février 1843 → 7 juin 1867 : 24 ans, 3 mois, 26 jours », avec le total en jours), dates incomplètes acceptées (mois et année, année seule : « 1821 → 1874 = 53 ans »), alerte si la seconde date précède la première, date ± une durée (« 12 mars 1842 + 73 ans, 4 mois, 12 jours »), avec jour de la semaine et conversions républicaine et julienne du résultat. Le bouton « Fiche » reprend directement une date de naissance, baptême, mariage, décès, sépulture ou événement libre.
  - **Calculateur de parenté** : entre deux personnes de l'arbre, donne le lien exact (« Marie est la cousine germaine de Pierre »), le lien inverse, le degré civil, les ancêtres communs ; demi-frères et sœurs, oncles/tantes, neveux/nièces, cousins de rang n, « à la mode de Bretagne », et alliances (époux, beau-père, beau-frère, gendre, bru…).
  - **« Ajouter ce résultat à la note de recherche »** sur chaque outil : le texte (modifiable) est ajouté à la fin de la note libre de la fiche choisie.
- **Correction** : le début de l'an XII (24/09/1803) et de l'an XIII (23/09/1804) était décalé d'un jour dans le convertisseur républicain existant.
- **Correction** : les noms de mois républicains saisis avec accents (Vendémiaire, Pluviôse, Ventôse, Floréal…) étaient refusés ; ils sont maintenant acceptés.
- Nouveaux tests unitaires JVM (conversion sur les 4 848 jours du calendrier, âges, écarts, parenté).
- Bumps v0.87 (gradle 87/« 0.87 », export GEDCOM « 2 VERS 0.87 », badge de saisie « v0.87 », écran d'application).

## v0.86 – Alertes locales (12.3)

- Anniversaires du jour : naissances, mariages et décès tombant un jour comme aujourd'hui (jour et mois identiques, quelle que soit l'année) ; les dates républicaines sont converties pour la comparaison.
- Onglet Journal : encadré vert « Ce jour : N événement(s) » en tête, chaque ligne (avec l'âge de l'événement) est cliquable et ouvre la fiche.
- Annonce à l'ouverture de l'application, combinée aux messages existants (diagnostic de l'arbre, rappels de recherche).
- Rappels de recherche (acte manquant sans coche « Enregistré », identités à vérifier, règle des 75 ans) : déjà couverts par le journal et le diagnostic, rappelés à chaque ouverture.

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

