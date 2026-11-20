# Sécurité de GénéaPoche

## Modèle de sécurité

- **Application strictement locale : aucune connexion, nulle part.** Un test automatique (CI) échoue si la permission Internet ou un composant réseau (WebView, URLConnection, OkHttp…) apparaît.
- Aucune permission Internet dans le manifeste : l'application elle-même ne transmet aucune donnée.
- Stockage local chiffré AES-256/GCM ; clé maître aléatoire de 256 bits liée au code PIN (double enveloppe PBKDF2-HmacSHA256 puis AndroidKeyStore).
- Sans le code PIN, aucune donnée ne peut être déchiffrée, même en accédant directement au stockage de l'application.
- Blocage progressif après 5 codes incorrects (30 s / 2 min / 10 min / 1 h), insensible au changement de l'heure du téléphone.
- Aucune sauvegarde cloud ni transfert d'appareil (règles d'exclusion explicites dans le manifeste).
- Aucun lien, aucune recherche vers Internet : l'application ne peut ouvrir ni navigateur ni lien (supprimé en v0.33). Le seul échange avec l'extérieur est le fichier GEDCOM ou la sauvegarde que vous exportez vous-même.
- Les sauvegardes .gpbak et exports .gpged sont chiffrés AES-256/GCM avec une clé dérivée de votre phrase secrète (PBKDF2-HmacSHA256, 600 000 itérations depuis la v0.108 ; les fichiers plus anciens, à 210 000, restent lisibles). L'import borne la taille lue (64 Mo pour une sauvegarde, 10 Mo pour un GEDCOM).
- Les exports GEDCOM et les images exportées sont en clair.

## Signaler une vulnérabilité

Signalez en privé tout problème de sécurité via les advisories GitHub du dépôt (Security → Report a vulnerability). Merci de ne pas ouvrir de ticket public pour une vulnérabilité. Le délai de réponse visé est de 7 jours.
