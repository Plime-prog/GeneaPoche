package com.plime.geneapoche

// v0.151 : bloc "Observations" de la fiche : ce qui a ete DEDUIT ou cree
// d'apres d'autres fiches, avec la fiche dont l'acte a servi (lien tactile).

class Observation(val texte: String, val ficheId: Long)

private fun libelleActeObs(type: String): String = when (type) {
    "N" -> "naissance"
    "B" -> "baptême"
    "M" -> "mariage"
    "D" -> "décès"
    "S" -> "sépulture"
    else -> "acte"
}

// "Identité créée d'après l'acte de naissance de X le 01/02/1900".
fun origineCreation(type: String, nomConcerne: String, epouse: String, date: String): String =
    "Identité créée d'après l'acte de " + libelleActeObs(type) + " de " + nomConcerne + epouse +
        (if (date.isNotBlank()) " le " + date.trim() else "")

fun observationsDe(p: Personne): List<Observation> {
    val res = mutableListOf<Observation>()
    if (p.creeDepuis.isNotBlank()) res.add(Observation(p.creeDepuis.trim(), if (p.creeDepuisFiche != p.id) p.creeDepuisFiche else 0L))
    if (p.naissanceCalculee.isNotBlank() && p.dateNaissance.isBlank()) {
        val ne = if (p.sexe == "F") "N\u00E9e" else if (p.sexe == "M") "N\u00E9" else "N\u00E9(e)"
        val src = p.naissanceCalculeeSource.trim()
        val an = anneeDe(p.naissanceCalculee)?.let { "vers $it" } ?: p.naissanceCalculee.trim()
        val fin = when {
            src.startsWith("estim") -> ", " + src
            src.isEmpty() -> ""
            else -> " d'apr\u00E8s " + src.removePrefix("calcul\u00e9e d'apr\u00e8s ").trim()
        }
        res.add(Observation(ne + " " + an + fin + ".", if (p.naissanceCalculeeFiche != p.id) p.naissanceCalculeeFiche else 0L))
    }
    if (p.decesCalcule.isNotBlank() && p.dateDeces.isBlank()) {
        val dec = if (p.sexe == "F") "D\u00E9c\u00E9d\u00E9e" else if (p.sexe == "M") "D\u00E9c\u00E9d\u00E9" else "D\u00E9c\u00E8s"
        val src = p.decesCalculeSource.trim()
        res.add(Observation(dec + " " + p.decesCalcule.trim() + (if (src.isNotEmpty()) ", d'apr\u00E8s " + src else "") + ".",
            if (p.decesCalculeFiche != p.id) p.decesCalculeFiche else 0L))
    }
    return res
}
