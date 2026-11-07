## v0.71 – Zéro d'office sur les dates : le format tient dans le temps

La détection de dates invalides (v0.69) était la bonne idée mais ne tenait pas dans le temps : après correction, sauvegarde, export GEDCOM puis réimport, le problème revenait. Cause : l'import GEDCOM fabriquait « 5/10/1890 » sans le zéro d'office, aussitôt re-signalé invalide au tour suivant.

- **Zéro d'office, TOUTE l'application** : nouvelle fonction normaliserDate — « 5/7/1890 » ou « 5.7.1890 » devient « 05/07/1890 » dès l'enregistrement ; normaliserHeure fait de même (« 9:05 » → « 09:05 »).
- **Trois points de passage obligatoires couvrent tout** : dateGreg (toute date d'acte), normaliserLieux (les cinq événements + événements libres, à chaque enregistrement : les dates incomplètes déjà présentes dans la base se réparent d'elles-mêmes) et dateFrancaise (« 5 OCT 1890 » → « 05/10/1890 » à l'import GEDCOM, fin de la boucle infernale).
- **Dates consultées comprises** : dateConsultation des actes normalisée aussi à l'enregistrement et au chargement JSON.
- **Qualificateurs GEDCOM légitimes** : « vers 1890 », « avant 1890 », « après 1890 » et « entre 1890 et 1895 » (ABT/BEF/AFT/BET relus de l'export) ne sont plus signalés comme des dates fausses : la norme GEDCOM est respectée.
- Bumps 0.71 (gradle 71/« 0.71 », export GEDCOM « 2 VERS 0.71 », badge de saisie « v0.71 », écran d'application).

text