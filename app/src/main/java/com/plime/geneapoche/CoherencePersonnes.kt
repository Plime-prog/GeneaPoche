package com.plime.geneapoche

// v0.88 : adaptateur Personne -> NoeudCoh pour les "Anomalies a verifier".

fun noeudsCoh(personnes: List<Personne>): List<NoeudCoh> = personnes.map { p ->
    NoeudCoh(
        id = p.id,
        nom = p.titre().trim(),
        sexe = p.sexe,
        naissance = p.dateNaissance,
        bapteme = p.dateBapteme,
        mariage = p.dateMariage,
        deces = p.dateDeces,
        sepulture = p.dateSepulture,
        pere = p.pere,
        mere = p.mere,
        conjoints = p.tousConjoints(),
        evenementsLibres = p.evenementsLibres.map { it.intitule to it.date },
        lieux = listOf(
            EvLieu("naissance", p.dateNaissance, p.deptNaissance),
            EvLieu("bapt\u00EAme", p.dateBapteme, p.deptBapteme),
            EvLieu("mariage", p.dateMariage, p.deptMariage),
            EvLieu("d\u00E9c\u00E8s", p.dateDeces, p.deptDeces),
            EvLieu("s\u00E9pulture", p.dateSepulture, p.deptSepulture)
        )
    )
}

// Un numero saisi n'est refuse que s'il est hors de toute numerotation
// connue (1-95 et departements historiques jusqu'a 130, outre-mer 970-989) ;
// un texte (nom de departement, 2A, 2B...) n'est jamais conteste.
fun deptNumeroConnu(d: String): Boolean {
    val n = d.trim().toIntOrNull() ?: return true
    return n in 1..130 || n in 970..989
}

// Anomalies a verifier, hors celles que l'utilisateur a decide d'ignorer.
fun anomaliesDeLArbre(personnes: List<Personne>, ignorees: Set<String>): List<AnomalieAVerifier> =
    anomaliesAVerifier(noeudsCoh(personnes), ::deptNumeroConnu).filter { !ignorees.contains(it.cle) }
