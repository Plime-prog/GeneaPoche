# Politique de confidentialité — GénéaPoche

*Application Android à usage personnel, sans compte, sans serveur, sans publicité.*

## Ce que l'application stocke

Toutes vos données généalogiques (personnes, liens familiaux, actes d'état civil, images d'actes, journal de recherches, notes libres, bloc Notes de l'onglet Recherches) sont stockées **uniquement sur votre appareil**, dans le stockage privé de l'application, **chiffrées en AES-256/GCM**. La clé maître est elle-même chiffrée avec une clé dérivée de votre code PIN (PBKDF2) et la clé matérielle AndroidKeyStore. **Sans votre code PIN, ces données sont indéchiffrables, y compris pour vous** : il n'existe aucune procédure de récupération.

## Ce qui sort de l'application — et pourquoi

- **Une seule fonction utilise Internet (v0.147)** : le bouton « Site Internet » de la Saisie affiche une page web (https uniquement) dans la zone du haut. GénéaPoche n'envoie alors aucune donnée de votre arbre ; seule l'adresse que vous tapez est contactée, comme dans un navigateur. Cookies et données du site sont effacés à la fermeture. Sans cette fonction, rien ne se connecte.
- **Fichiers que vous exportez vous-même** : GEDCOM, images et sauvegardes .gpbak (cette dernière étant chiffrée avec votre phrase secrète). Les fichiers GEDCOM et images sont **en clair** : c'est à vous de les protéger.

Rien d'autre ne quitte jamais l'application : elle n'utilise la permission Internet que pour afficher la page web demandée par vous ; aucune donnée de l'application n'est transmise.

## Ce que l'application ne fait pas

- Aucune télémétrie, aucune mesure d'audience, aucun journal envoyé.
- Aucune publicité, aucun traceur, aucune bibliothèque tierce de mesure.
- Aucune sauvegarde cloud, aucun transfert automatique vers un nouvel appareil (backup Android explicitement exclu dans le manifeste).
- Aucune collecte de contacts, de position ou d'identifiants.

## Données fictives uniquement dans le dépôt

Le dépôt source ne contient aucune donnée personnelle réelle : tests, captures et exemples sont entièrement fictifs.

## Contact

Signalement de confidentialité ou de sécurité : voir [SECURITY.md](SECURITY.md).
