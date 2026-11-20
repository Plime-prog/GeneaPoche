package com.plime.geneapoche

// v0.147 : ordre chronologique des conjoints (date de mariage) et des enfants
// (date de naissance). Date inconnue : en fin de liste, ordre d'origine conserve.

private fun cleChrono(date: String): Long =
    if (anneeDe(date) == null) Long.MAX_VALUE else cleDate(date)

// Date du mariage entre p et q : celle de p pour son conjoint principal, sinon celle de q.
fun dateMariageEntre(p: Personne, q: Personne): String {
    if (p.conjoint == q.id && p.dateMariage.isNotBlank()) return p.dateMariage
    if (q.conjoint == p.id && q.dateMariage.isNotBlank()) return q.dateMariage
    return ""
}

fun conjointsChrono(p: Personne, personnes: List<Personne>): List<Personne> =
    p.tousConjoints().mapNotNull { personneParId(personnes, it) }
        .sortedBy { cleChrono(dateMariageEntre(p, it)) }

fun enfantsChrono(enfants: List<Personne>): List<Personne> =
    enfants.sortedBy { cleChrono(it.dateNaissance) }
