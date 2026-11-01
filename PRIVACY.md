# Politique de confidentialité — GénéaPoche

*Application Android à usage personnel, sans compte, sans serveur, sans publicité.*

## Ce que l'application stocke

Toutes vos données généalogiques (personnes, liens familiaux, actes d'état civil, images d'actes, journal de recherches, notes libres) sont stockées **uniquement sur votre appareil**, dans le stockage privé de l'application, **chiffrées en AES-256/GCM**. La clé maître est elle-même chiffrée avec une clé dérivée de votre code PIN (PBKDF2) et la clé matérielle AndroidKeyStore. **Sans votre code PIN, ces données sont indéchiffrables, y compris pour vous** : il n'existe aucune procédure de récupération.

## Ce qui sort de l'application — et pourquoi

- **Recherche d'archives** : lorsque vous touchez la loupe d'un acte, GénéaPoche ouvre votre navigateur et lui transmet la **commune, l'année et le type d'acte** pour la recherche. Ces éléments transitent alors par votre navigateur et le moteur de recherche, hors de l'application.
- **Fichiers que vous exportez vous-même** : GEDCOM, images et sauvegardes .gpbak (cette dernière étant chiffrée avec votre phrase secrète). Les fichiers GEDCOM et images sont **en clair** : c'est à vous de les protéger.

Rien d'autre ne quitte jamais l'application : elle n'a **aucune permission Internet** dans son manifeste et ne peut donc rien transmettre d'elle-même.

## Ce que l'application ne fait pas

- Aucune télémétrie, aucune mesure d'audience, aucun journal envoyé.
- Aucune publicité, aucun traceur, aucune bibliothèque tierce de mesure.
- Aucune sauvegarde cloud, aucun transfert automatique vers un nouvel appareil (backup Android explicitement exclu dans le manifeste).
- Aucune collecte de contacts, de position ou d'identifiants.

## Données fictives uniquement dans le dépôt

Le dépôt source ne contient aucune donnée personnelle réelle : tests, captures et exemples sont entièrement fictifs.

## Contact

Signalement de confidentialité ou de sécurité : voir [SECURITY.md](SECURITY.md).
