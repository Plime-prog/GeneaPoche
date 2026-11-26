# Repères généalogiques (v0.154)

GénéaPoche embarque, **en local et hors ligne**, deux sortes de repères pour comprendre la vie d'une personne. Rien n'est téléchargé ni envoyé.

## 1. Repères datés (genre « genea »)

Ajoutés à `EvenementsHistoriques` (`Histoire.kt`) : édit de 1556, concile de Trente (1563), ordonnances de 1639 et 1667, loi du 20 septembre 1792, tables décennales (1793), loi Jourdan-Delbrel (1798), Code civil (1804), abolition du divorce (1816), recensements nominatifs (1836), loi Naquet (1884), loi de 1889, lois de 1907, 1938, 1945, 1965, 1970, 1972, 1974, fin de la conscription (1997), mariage à 18 ans (2006).

Ils apparaissent dans l'outil **Histoire** (couleur verte) mais **pas** dans la Biographie ni dans la chronologie d'une personne, pour garder une lecture légère.

## 2. Repères calculés (bloc replié « Repères généalogiques » de la fiche)

`reperesGenealogiquesDe(p, personnes)` (`ReperesGenealogiques.kt`) produit des phrases à partir des dates de la personne, sans rien enregistrer :

- **Majorité civile** : 21 ans (1792-1974), 18 ans ensuite (loi du 5 juillet 1974).
- **Mariage sous la majorité matrimoniale** : 30 ans (H) / 25 ans (F) avant 1792 ; 21 ans de 1792 à 1804 ; 25 ans (H) / 21 ans (F) de 1804 à 1907 ; 21 ans de 1907 à 1974 ; 18 ans ensuite. En dessous, le consentement des parents était requis ; avant 21 ans (1804-1973) le mariage émancipait.
- **Conscription (hommes)** : classe = année des 20 ans ; tirage au sort jusqu'en 1888, service obligatoire ensuite.
- **Recensements nominatifs** : tous les 5 ans de 1836 à 1936 pendant la vie.
- **Veuvage d'une femme** : délai de viduité de 300 jours (Code civil).

## Limites

Les règles varient selon les époques, les coutumes et les régions ; ce sont des **indications**, à vérifier dans les sources. Les dates et âges calculés restent signalés comme tels. Les repères locaux par département (champ `depts`) sont possibles pour de futurs ajouts.
