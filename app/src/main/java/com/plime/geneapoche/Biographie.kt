package com.plime.geneapoche

// ---------------------------------------------------------------- Biographie (v0.102)
// Logique PURE de l'onglet Biographie d'une fiche : un r\u00E9cit en phrases et
// une frise qui m\u00EAle la vie de la personne, sa famille et l'histoire.

// genre : "vie" (\u00E9v\u00E9nements de la personne), "famille", "histoire", "regime".
class LigneBio(
    val cle: Long,
    val quand: String,
    val texte: String,
    val genre: String,
    val alerte: String = ""
)

private fun deux(n: Int): String = if (n < 10) "0" + n.toString() else n.toString()

fun quandBio(annee: Int, mois: Int, jour: Int): String = when {
    mois > 0 && jour > 0 -> deux(jour) + "/" + deux(mois) + "/" + annee.toString()
    mois > 0 -> deux(mois) + "/" + annee.toString()
    else -> annee.toString()
}

private fun estVie(libelle: String): Boolean =
    libelle.startsWith("Naissance (") || libelle == "Naissance" || libelle == "D\u00E9c\u00E8s" ||
        libelle == "Bapt\u00EAme" || libelle == "S\u00E9pulture" || libelle.startsWith("Mariage") ||
        !(libelle.startsWith("Naissance ") || libelle.startsWith("D\u00E9c\u00E8s ") ||
            libelle.startsWith("Remariage"))

// Derni\u00E8re ann\u00E9e prise en compte pour les \u00E9v\u00E9nements historiques.
private fun anneeFinBio(p: Personne, anneeN: Int?): Int? {
    val d = anneeDe(p.dateDeces)
    if (d != null) return d
    return if (anneeN != null) minOf(anneeN + 100, 2026) else null
}

// Frise : vie + famille + histoire (nationale et locale) + r\u00E9gimes.
fun friseBiographie(p: Personne, personnes: List<Personne>, avecHistoire: Boolean = true): List<LigneBio> {
    val res = mutableListOf<LigneBio>()
    val anneeN = anneeDe(p.dateNaissance.ifBlank { p.naissanceCalculee })
    chronologieFamille(p, personnes, true).forEach { e ->
        val age = if (anneeN != null && e.annee >= anneeN && e.libelle != "Naissance" && !e.libelle.startsWith("Naissance (")) " (" + (e.annee - anneeN).toString() + " ans)" else ""
        res.add(
            LigneBio(e.cle, quandBio(e.annee, e.mois, e.jour), e.libelle + age,
                if (estVie(e.libelle)) "vie" else "famille", e.alerte)
        )
    }
    val fin = anneeFinBio(p, anneeN)
    if (avecHistoire && anneeN != null && fin != null) {
        val dept = deptDePersonne(p)
        EvenementsHistoriques.forEach { h ->
            if (h.annee in anneeN..fin && (h.depts.isEmpty() || (dept.isNotEmpty() && dept in h.depts))) {
                res.add(LigneBio(h.annee * 10000L + h.mois * 100L + h.jour, quandBio(h.annee, h.mois, h.jour), h.titre, "histoire"))
            }
        }
        PeriodesHistoriques.forEach { r ->
            if (r.debut.year in anneeN..fin) {
                res.add(LigneBio(r.debut.year * 10000L + r.debut.monthValue * 100L + r.debut.dayOfMonth,
                    quandBio(r.debut.year, r.debut.monthValue, r.debut.dayOfMonth), "D\u00E9but : " + r.nom, "regime"))
            }
        }
    }
    val ordre = listOf("vie", "famille", "histoire", "regime")
    return res.sortedWith(compareBy({ it.cle }, { ordre.indexOf(it.genre) }))
}

fun detail(date: String, lieu: String): String {
    val d = dateAffichee(date.trim())
    val l = lieuAffiche(lieu.trim())
    return (if (d.isNotEmpty()) " le " + d else "") + (if (l.isNotEmpty()) " \u00E0 " + l else "")
}

// R\u00E9cit : quelques paragraphes en phrases compl\u00E8tes.
fun recitBiographie(p: Personne, personnes: List<Personne>): List<String> {
    val res = mutableListOf<String>()
    val f = p.sexe == "F"
    val il = if (f) "Elle" else "Il"
    val parId = personnes.associateBy { it.id }
    val sb = StringBuilder()
    sb.append(p.titre())
    val nais = detail(p.dateNaissance, p.lieuNaissance)
    if (nais.isNotEmpty()) {
        sb.append(if (f) " est n\u00E9e" else " est n\u00E9").append(nais)
    } else if (p.naissanceCalculee.isNotBlank()) {
        sb.append(if (f) " est n\u00E9e" else " est n\u00E9").append(" vers ").append(anneeDe(p.naissanceCalculee)?.toString() ?: p.naissanceCalculee)
    } else {
        sb.append(" : naissance inconnue")
    }
    val pere = parId[p.pere]
    val mere = parId[p.mere]
    if (pere != null || mere != null) {
        sb.append(if (p.sexe == "F") ", fille de " else if (p.sexe == "M") ", fils de " else ", enfant de ")
        sb.append(listOfNotNull(pere?.titre(), mere?.titre()).joinToString(" et de "))
    }
    sb.append(".")
    res.add(sb.toString())
    if (p.profession.isNotBlank()) res.add(il + " exer\u00E7ait le m\u00E9tier de " + p.profession.trim() + ".")
    val conjoints = p.tousConjoints().mapNotNull { parId[it] }
    if (conjoints.isNotEmpty()) {
        val m = detail(p.dateMariage, p.lieuMariage)
        res.add(
            il + (if (f) " s'est mari\u00E9e" else " s'est mari\u00E9") + m + " avec " + conjoints.first().titre() +
                (if (conjoints.size > 1) ", puis avec " + conjoints.drop(1).joinToString(", ") { it.titre() } else "") + "."
        )
    }
    val enfants = p.enfants.mapNotNull { parId[it] }
    if (enfants.isNotEmpty()) {
        res.add(
            il + " a eu " + enfants.size.toString() + (if (enfants.size == 1) " enfant : " else " enfants : ") +
                enfants.joinToString(", ") { it.titre() } + "."
        )
    }
    val dec = detail(p.dateDeces, p.lieuDeces)
    if (dec.isNotEmpty()) {
        val an = anneeDe(p.dateNaissance)
        val ad = anneeDe(p.dateDeces)
        val age = if (an != null && ad != null && ad >= an) " \u00E0 l'\u00E2ge d'environ " + (ad - an).toString() + " ans" else ""
        res.add(il + (if (f) " est d\u00E9c\u00E9d\u00E9e" else " est d\u00E9c\u00E9d\u00E9") + dec + age + ".")
    }
    return res
}
