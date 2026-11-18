package com.plime.geneapoche

// v0.137 : ordre de la chronologie unique d'une fiche.
//
// Entrees : (cle de tri aaaammjj ou aaaa0000, identifiant) ; l'identifiant
// commence par "ev:" pour les blocs N, B, M, D, S ("ev:N"...), sinon c'est un
// evenement libre, un enfant, un parrainage, la famille ou un repere historique.
// Une cle negative = date inconnue.
//
// Avant : des que N, M ou D n'etait pas date, TOUS les blocs d'actes passaient en
// tete et le reste suivait, sans jamais s'entremeler. Maintenant :
//  - tout ce qui est date est range par date (a date egale, N B M D S d'abord) ;
//  - un acte sans date s'insere a sa place naturelle : N au debut, B apres N,
//    M apres B, D a la fin, S apres D ;
//  - les autres evenements sans date viennent apres ceux qui sont dates, avant
//    un deces sans date.

private val ORDRE_ACTES = listOf("N", "B", "M", "D", "S")

private fun rangActe(cle: String): Int = ORDRE_ACTES.indexOf(cle.removePrefix("ev:"))

fun ordonnerChronologie(entrees: List<Pair<Long, String>>): List<Pair<Long, String>> {
    val actesSansDate = entrees.filter { it.second.startsWith("ev:") && it.first < 0L }
    val autresSansDate = entrees.filter { !it.second.startsWith("ev:") && it.first < 0L }
    val res = entrees.filter { it.first >= 0L }
        .sortedWith(compareBy<Pair<Long, String>>({ it.first }, { if (it.second.startsWith("ev:")) rangActe(it.second) else 9 }))
        .toMutableList()
    fun indice(t: String): Int = res.indexOfFirst { it.second == "ev:" + t }
    fun sansDate(t: String): Pair<Long, String>? = actesSansDate.firstOrNull { it.second == "ev:" + t }

    sansDate("N")?.let { res.add(0, it) }
    sansDate("B")?.let { res.add(indice("N") + 1, it) }
    sansDate("M")?.let { res.add(maxOf(indice("N"), indice("B")) + 1, it) }
    res.addAll(autresSansDate)
    sansDate("D")?.let { res.add(it) }
    sansDate("S")?.let { s ->
        val d = indice("D")
        if (d >= 0) res.add(d + 1, s) else res.add(s)
    }
    return res
}
