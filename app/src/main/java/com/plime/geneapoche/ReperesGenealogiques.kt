package com.plime.geneapoche

// v0.154 : "Reperes genealogiques" d'une personne : phrases CALCULEES depuis ses
// dates (majorite, mariage sans consentement, conscription, recensements,
// veuvage), dans l'esprit des blogs de genealogie. Bloc replie dans la fiche :
// la chronologie reste legere. Tout est local ; aucune donnee n'est enregistree.

private fun anneeNaissanceRepere(p: Personne): Pair<Int, Boolean>? {
    anneeDe(p.dateNaissance)?.let { return it to false }
    if (p.naissanceCalculee.isNotBlank()) RX_ANNEE4.find(p.naissanceCalculee)?.value?.toIntOrNull()?.let { return it to true }
    return null
}

// Majorite matrimoniale (age sous lequel le consentement des parents est requis).
fun majoriteMatrimoniale(annee: Int, femme: Boolean): Int? = when {
    annee < 1556 -> null
    annee < 1792 -> if (femme) 25 else 30
    annee < 1804 -> 21
    annee < 1907 -> if (femme) 21 else 25
    annee < 1974 -> 21
    else -> 18
}

fun reperesGenealogiquesDe(p: Personne, personnes: List<Personne>): List<String> {
    val naiss = anneeNaissanceRepere(p) ?: return emptyList()
    val an = naiss.first
    val vers = if (naiss.second) "vers " else ""
    val femme = p.sexe == "F"
    val fin = anneeDe(p.dateDeces)
    val res = mutableListOf<String>()
    val prenom = p.prenoms.trim().split(" ").firstOrNull().orEmpty().ifBlank { "La personne" }
    val ne = if (femme) "Née" else "Né"

    // Majorite civile.
    val majeur = when {
        an + 21 < 1792 -> null
        an + 21 <= 1974 -> an + 21 to 21
        an + 18 >= 1974 -> an + 18 to 18
        else -> 1974 to 18
    }
    if (majeur != null && (fin == null || fin >= majeur.first))
        res.add((if (femme) "Majeure" else "Majeur") + " en " + vers + majeur.first + " (" + majeur.second + " ans" +
            (if (majeur.second == 21) ", Code civil de 1804" else ", loi du 5 juillet 1974") + ").")

    // Mariage : consentement des parents et emancipation.
    val anM = anneeDe(p.dateMariage)
    if (anM != null && anM >= 1556) {
        val age = anM - an
        val seuil = majoriteMatrimoniale(anM, femme)
        val minimum = if (anM >= 1804) (if (femme) 15 else 18) else 0
        if (seuil != null && age >= minimum && age < seuil) {
            res.add("À son mariage (" + anM + "), " + prenom + " a " + (if (naiss.second) "environ " else "") + age +
                " ans : en dessous de la majorité matrimoniale (" + seuil + " ans), le consentement de ses parents était requis" +
                (if (anM in 1804..1973 && age < 21) "; le mariage l'émancipait de plein droit" else "") + ".")
        }
    }

    // Conscription (hommes) : tirage au sort de 1798 a 1888, puis service obligatoire.
    if (p.sexe == "M" && an + 20 in 1798..1996 && (fin == null || fin >= an + 20)) {
        res.add(ne + " en " + vers + an + ", il appartient à la classe " + (an + 20) +
            (if (an + 20 < 1889) " : soumis au tirage au sort de la conscription à 20 ans." else " : service militaire obligatoire."))
    }

    // Recensements nominatifs (tous les 5 ans, 1836-1936) pendant sa vie.
    val debut = maxOf(an, 1836)
    val bout = minOf(fin ?: (an + 90), 1936)
    val recensements = (debut..bout).filter { it >= 1836 && (it - 1836) % 5 == 0 }
    if (recensements.isNotEmpty()) {
        res.add("Peut figurer aux recensements nominatifs de " + recensements.first() +
            (if (recensements.size > 1) " à " + recensements.last() + " (" + recensements.size + " en tout, tous les 5 ans)" else "") + ".")
    }

    // Veuvage d'une femme : delai de viduite de 300 jours (Code civil, art. 228).
    if (femme) {
        val veuvage = p.tousConjoints().mapNotNull { id -> personnes.firstOrNull { it.id == id } }
            .mapNotNull { anneeDe(it.dateDeces) }.maxOrNull()
        if (veuvage != null && veuvage in 1804..2004)
            res.add("Veuve en " + veuvage + " : elle ne pouvait pas se remarier avant 300 jours (délai de viduité, Code civil).")
    }
    return res
}
