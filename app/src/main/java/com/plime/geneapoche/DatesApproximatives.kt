package com.plime.geneapoche

// v0.152 : dates APPROXIMATIVES deduites des autres fiches, des que les
// informations existent (creation d'un enfant, acte, mariage...). Elles sont
// toujours signalees comme calculees (bloc Observations) et ne remplacent
// JAMAIS une date saisie. Tout se passe en local.

const val PREFIXE_ESTIMEE = "estim\u00E9e d'apr\u00E8s "
private const val AGE_MERE = 28
private const val AGE_PERE = 32
private const val AGE_MARIAGE = 25

private fun estEstimee(q: Personne) = q.naissanceCalculeeSource.startsWith("estim")

// Annee de naissance SURE : date saisie, ou calculee d'apres un age (pas estimee).
private fun anneeSure(q: Personne): Int? =
    anneeDe(q.dateNaissance)
        ?: if (q.naissanceCalculee.isNotBlank() && !estEstimee(q)) RX3.find(q.naissanceCalculee)?.value?.toIntOrNull() else null

// v0.153 : annee de naissance, sure OU estimee (pour descendre aux enfants).
private fun anneeApprox(q: Personne): Int? =
    anneeDe(q.dateNaissance) ?: if (q.naissanceCalculee.isNotBlank()) RX_ANNEE4.find(q.naissanceCalculee)?.value?.toIntOrNull() else null

class DateDeduite(val texte: String, val source: String, val fiche: Long)

private fun nomCourt(q: Personne): String = q.titre()

fun estimerNaissance(p: Personne, parId: Map<Long, Personne>): DateDeduite? {
    val mere = parId[p.mere]
    val pere = parId[p.pere]
    mere?.let { m -> anneeApprox(m)?.let { return DateDeduite("vers " + (it + AGE_MERE), PREFIXE_ESTIMEE + "la naissance de sa m\u00E8re " + nomCourt(m) + " (" + it + ")", m.id) } }
    pere?.let { f -> anneeApprox(f)?.let { return DateDeduite("vers " + (it + AGE_PERE), PREFIXE_ESTIMEE + "la naissance de son p\u00E8re " + nomCourt(f) + " (" + it + ")", f.id) } }
    val decalage = if (p.sexe == "F") AGE_MERE else AGE_PERE
    val enfant = p.enfants.mapNotNull { parId[it] }.mapNotNull { e -> anneeSure(e)?.let { e to it } }.minByOrNull { it.second }
    if (enfant != null) return DateDeduite("vers " + (enfant.second - decalage), PREFIXE_ESTIMEE + "la naissance de son enfant " + nomCourt(enfant.first) + " (" + enfant.second + ")", enfant.first.id)
    anneeDe(p.dateMariage)?.let { return DateDeduite("vers " + (it - AGE_MARIAGE), PREFIXE_ESTIMEE + "son mariage (" + it + ")", 0L) }
    return null
}

// Dernier evenement connu de la vie de p -> "apres AAAA".
fun estimerDeces(p: Personne, parId: Map<Long, Personne>, evenementsTiers: Map<Long, DateDeduite>): DateDeduite? {
    var meilleur: Pair<Int, DateDeduite>? = null
    fun voir(an: Int?, source: String, fiche: Long) {
        if (an != null && (meilleur == null || an > meilleur!!.first)) meilleur = an to DateDeduite("apr\u00E8s $an", source, fiche)
    }
    voir(anneeDe(p.dateMariage), "son mariage", 0L)
    p.enfants.mapNotNull { parId[it] }.forEach { e ->
        val an = anneeDe(e.dateNaissance)
        // Une mere vit au moins jusqu'a la naissance ; un pere, a un an pres.
        voir(an, "la naissance de son enfant " + nomCourt(e), e.id)
    }
    evenementsTiers[p.id]?.let { t -> voir(anneeDe(t.texte), t.source, t.fiche) }
    return meilleur?.second
}

// Une passe sur toute la base : renvoie le nombre de fiches modifiees.
fun completerDatesApproximatives(personnes: List<Personne>): Int {
    if (personnes.isEmpty()) return 0
    val parId = personnes.associateBy { it.id }
    // Evenements tiers (une seule passe) : declarant d'un deces, temoin d'un acte.
    val tiers = HashMap<Long, DateDeduite>()
    fun noter(id: Long, date: String, source: String, fiche: Long) {
        val an = anneeDe(date) ?: return
        val ex = tiers[id]
        if (ex == null || an > (anneeDe(ex.texte) ?: -1)) tiers[id] = DateDeduite(date, source, fiche)
    }
    personnes.forEach { q ->
        if (q.declarantId != 0L) noter(q.declarantId, q.dateDeces, "la d\u00E9claration du d\u00E9c\u00E8s de " + q.titre(), q.id)
        listOf("N", "B", "M", "D", "S").forEach { t ->
            val date = when (t) { "N" -> q.dateNaissance; "B" -> q.dateBapteme; "M" -> q.dateMariage; "D" -> q.dateDeces; else -> q.dateSepulture }
            q.temoinsDe(t).forEach { id -> noter(id, date, "l'acte de ${nomActe(t)} de " + q.titre() + " (t\u00E9moin)", q.id) }
        }
    }
    var modifs = 0
    // Plusieurs passes : enfants, petits-enfants... se deduisent de proche en proche.
    var passe = 0
    var avant = -1
    while (passe < 10 && modifs != avant) {
        avant = modifs
        passe++
        personnes.forEach { p ->
            // Une date saisie ensuite fait disparaitre l'estimation.
            if (p.dateNaissance.isNotBlank() && estEstimee(p)) { p.naissanceCalculee = ""; p.naissanceCalculeeSource = ""; p.naissanceCalculeeFiche = 0L; modifs++ }
            if (p.dateDeces.isNotBlank() && p.decesCalcule.isNotBlank()) { p.decesCalcule = ""; p.decesCalculeSource = ""; p.decesCalculeFiche = 0L; modifs++ }
            if (p.dateNaissance.isBlank() && (p.naissanceCalculee.isBlank() || estEstimee(p))) {
                val d = estimerNaissance(p, parId)
                if (d != null && (d.texte != p.naissanceCalculee || d.source != p.naissanceCalculeeSource)) {
                    p.naissanceCalculee = d.texte; p.naissanceCalculeeSource = d.source; p.naissanceCalculeeFiche = d.fiche; modifs++
                } else if (d == null && estEstimee(p)) {
                    p.naissanceCalculee = ""; p.naissanceCalculeeSource = ""; p.naissanceCalculeeFiche = 0L; modifs++
                }
            }
            if (p.dateDeces.isBlank()) {
                val d = estimerDeces(p, parId, tiers)
                if (d != null && (d.texte != p.decesCalcule || d.source != p.decesCalculeSource)) {
                    p.decesCalcule = d.texte; p.decesCalculeSource = d.source; p.decesCalculeFiche = d.fiche; modifs++
                } else if (d == null && p.decesCalcule.isNotBlank()) {
                    p.decesCalcule = ""; p.decesCalculeSource = ""; p.decesCalculeFiche = 0L; modifs++
                }
            }
        }
    }
    return modifs
}

private fun nomActe(t: String) = when (t) { "N" -> "naissance"; "B" -> "bapt\u00EAme"; "M" -> "mariage"; "D" -> "d\u00E9c\u00E8s"; else -> "s\u00E9pulture" }
