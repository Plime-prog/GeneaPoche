# Signature des APK et mises à jour

## Le problème (corrigé en v0.140)

Android n'accepte une mise à jour que si le nouvel APK est signé **avec la même clé** que celui déjà installé. Jusqu'à la v0.139, la CI GitHub signait avec la clé « debug » du serveur de build, **régénérée à chaque exécution** : chaque version avait donc un certificat différent et l'installation par-dessus échouait (« conflit de package », « application non installée »). Constaté sur les APK publiés : les certificats de la v0.138 et de la v0.139 étaient différents.

## La solution

- Une clé stable est versionnée dans le dépôt : `app/geneapoche-signature.jks` (alias `geneapoche`, valable jusqu'en 2056). Elle signe **les deux** APK (debug et release) quand aucun secret GitHub n'est défini : on peut donc passer de l'un à l'autre sans désinstaller.
- Si les secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` existent, ils restent prioritaires pour la version release (changer de clé impose alors **une** désinstallation).
- La CI vérifie à chaque build, avec `apksigner`, que les deux APK portent bien le certificat stable, et fait échouer le build sinon.

## Limite (à connaître)

La clé est dans un dépôt public : elle n'est **pas secrète**. Pour une application personnelle installée hors boutique, c'est acceptable ; pour une publication sur le Play Store, il faudrait une clé privée gardée en secret. Quiconque possède cette clé pourrait fabriquer un APK que votre téléphone accepterait comme mise à jour : n'installez que les APK téléchargés depuis la page Releases du dépôt.

## Passage de la v0.139 (ou antérieure) à la v0.140

La signature change **une seule fois** : l'installation par-dessus l'ancienne version sera refusée. Procédure :
1. Dans l'ancienne version : Menu → exporter une **sauvegarde `.gpbak`** (et noter la phrase secrète), en la copiant hors de l'application (Téléchargements, Drive…).
2. Désinstaller l'ancienne GénéaPoche (les données de l'application sont effacées).
3. Installer la v0.140 depuis la page Releases.
4. Menu → restaurer la sauvegarde `.gpbak`.
À partir de la v0.140, toutes les mises à jour s'installent par-dessus sans manipulation.
