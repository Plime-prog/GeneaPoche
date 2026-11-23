package com.plime.geneapoche

// v0.150 : "X, 45 ans, a ete temoin de l'acte de mariage de Y et Z, le D".
// Les temoins d'un acte sont deja relies a la fiche des personnes concernees
// (temoinsN/B/M/D/S) ; ici on relit ce lien depuis la fiche du TEMOIN.

class TemoignageActe(val type: String, val date: String, val lieu: String, val noms: List<String>)

private fun evenementDe(q: Personne, type: String): Pair<String, String> = when (type) {
    "N" -> q.dateNaissance to q.lieuNaissance
    "B" -> q.dateBapteme to q.lieuBapteme
    "M" -> q.dateMariage to q.lieuMariage
    "D" -> q.dateDeces to q.lieuDeces
    else -> q.dateSepulture to q.lieuSepulture
}

private fun libelleActe(type: String): String = when (type) {
    "N" -> "naissance"
    "B" -> "baptême"
    "M" -> "mariage"
    "D" -> "décès"
    else -> "sépulture"
}

// Actes dont p est temoin, un par acte (les deux epoux d'un mariage sont regroupes).
fun temoignagesDe(p: Personne, personnes: List<Personne>): List<TemoignageActe> {
    val groupes = LinkedHashMap<String, Pair<Triple<String, String, String>, MutableList<String>>>()
    personnes.forEach { q ->
        if (q.id != p.id) listOf("N", "B", "M", "D", "S").forEach { t ->
            if (q.temoinsDe(t).contains(p.id)) {
                val (d, l) = evenementDe(q, t)
                val cle = t + "|" + d.trim() + "|" + l.trim()
                val g = groupes.getOrPut(cle) { Triple(t, d.trim(), l.trim()) to mutableListOf() }
                if (q.titre() !in g.second) g.second.add(q.titre())
            }
        }
    }
    return groupes.values.map { (k, noms) -> TemoignageActe(k.first, k.second, k.third, noms) }
        .sortedBy { if (anneeDe(it.date) == null) Long.MAX_VALUE else cleDate(it.date) }
}

private fun listeNoms(noms: List<String>): String =
    if (noms.size <= 1) noms.joinToString("") else noms.dropLast(1).joinToString(", ") + " et " + noms.last()

fun phraseTemoin(p: Personne, t: TemoignageActe): String {
    val anActe = anneeDe(t.date)
    val nais = anneeDe(p.dateNaissance) ?: Regex("[0-9]{4}").find(p.naissanceCalculee)?.value?.toIntOrNull()
    val age = if (anActe != null && nais != null && anActe - nais in 0..120) (anActe - nais).toString() + " ans" else ""
    val domicile = p.domicile.trim().let { if (it.isNotEmpty()) (if (p.sexe == "F") "domiciliée à " else "domicilié à ") + lieuAffiche(it) else "" }
    val identite = listOf(p.titre(), age, p.profession.trim(), domicile).filter { it.isNotEmpty() }.joinToString(", ")
    val lieu = lieuAffiche(t.lieu).let { if (it.isNotEmpty()) " à " + it else "" }
    val quand = dateAffichee(t.date).let { if (it.isNotEmpty()) " le " + it else "" }
    return identite + " a été témoin de l'acte de " + libelleActe(t.type) + " de " + listeNoms(t.noms) + lieu + quand + "."
}

// Libelle court pour la chronologie.
fun libelleTemoin(t: TemoignageActe): String = "Témoin de l'acte de " + libelleActe(t.type) + " de " + listeNoms(t.noms)
