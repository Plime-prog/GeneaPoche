# Contribuer à GénéaPoche

## Règles communes

- Traitez les sujets dans l'ordre : la sécurité et les données avant la robustesse, la robustesse avant le design.
- **Aucune donnée personnelle réelle** dans le dépôt, les captures, les tests ou les tickets : uniquement des données fictives.
- **Ne jamais ajouter de permission Android**, notamment INTERNET, ni de bibliothèque qui mesure, journalise ou transmet des données.
- Chaque changement garde le workflow APK **au vert** (lint, tests unitaires, debug et release) et indique le risque corrigé et la façon de le tester sur téléphone.
- Les points marqués **Migration** touchent au format des données : tester d'abord la mise à jour depuis la version précédente avec une base fictive, en gardant les anciens fichiers jusqu'à validation.
- Un point est terminé quand son **critère de test** est vérifié, pas quand le code compile.

## Style de code

- Kotlin + Jetpack Compose, un fichier par écran, moins de 400 lignes par fichier si possible.
- Encodage : UTF-8 (voir `.editorconfig`) ; dans les fichiers Kotlin, les accents restent en séquences d'échappement `\uXXXX` (sauf Arbre.kt, en UTF-8 réel).
- Aucune chaîne d'interface en dur dans un nouveau composant : utilisez `strings.xml` et des pluriels.
- Zéro avertissement du compilateur attendu (icônes AutoMirrored, pas de SimpleDateFormat statique, pas d'API dépréciée).

## Avant de pousser

1. `./gradlew lintDebug testDebugUnitTest assembleDebug assembleRelease` doit passer.
2. Vérifier que la CI est verte avant de pousser le commit suivant.
3. Message de commit : description du point traité (ex : « G2-03 : dates républicaines converties partout via anneeDe »).

## Signaler un problème

- Vulnérabilité : voir [SECURITY.md](SECURITY.md) (signalement privé, jamais de ticket public).
- Bug : ticket GitHub avec des données fictives uniquement et les étapes pour le reproduire.
