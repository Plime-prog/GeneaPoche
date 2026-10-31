# Sécurité de GénéaPoche

## Modèle de sécurité

- Aucune permission Internet dans le manifeste : l'application elle-même ne transmet aucune donnée.
- Stockage local chiffré AES-256/GCM ; clé maître aléatoire de 256 bits liée au code PIN (double enveloppe PBKDF2-HmacSHA256 puis AndroidKeyStore).
- Sans le code PIN, aucune donnée ne peut être déchiffrée, même en accédant directement au stockage de l'application.
- Blocage progressif après 5 codes incorrects (30 s / 2 min / 10 min / 1 h), insensible au changement de l'heure du téléphone.
- Aucune sauvegarde cloud ni transfert d'appareil (règles d'exclusion explicites dans le manifeste).
- La recherche d'archives départementales passe par le navigateur : commune, année et type d'acte sont envoyés au moteur de recherche.
- Les exports GEDCOM et les images exportées sont en clair.

## Signaler une vulnérabilité

Signalez en privé tout problème de sécurité via les advisories GitHub du dépôt (Security → Report a vulnerability). Merci de ne pas ouvrir de ticket public pour une vulnérabilité. Le délai de réponse visé est de 7 jours.
