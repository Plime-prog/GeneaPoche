package com.plime.geneapoche

// v0.141 : pile de retour arriere. Un ecran = (onglet, fiche ouverte) code dans
// un seul Long (onglet * 10^10 + identifiant de fiche ; 0 = aucune fiche).

private const val FACTEUR_ECRAN = 10_000_000_000L
const val TAILLE_MAX_PILE_ECRANS = 40

fun codeEcran(onglet: Int, fiche: Long): Long = onglet.toLong() * FACTEUR_ECRAN + fiche
fun ongletDeCode(code: Long): Int = (code / FACTEUR_ECRAN).toInt()
fun idDeCode(code: Long): Long = code % FACTEUR_ECRAN

// Empile l'ecran quitte (pas deux fois de suite le meme, taille bornee).
fun ajouterEcran(pile: MutableList<Long>, quitte: Long) {
    if (pile.lastOrNull() == quitte) return
    pile.add(quitte)
    while (pile.size > TAILLE_MAX_PILE_ECRANS) pile.removeAt(0)
}

// Depile jusqu'au premier ecran DIFFERENT de l'ecran courant ; null s'il n'y en a plus.
fun depilerEcran(pile: MutableList<Long>, courant: Long): Long? {
    while (pile.isNotEmpty()) {
        val c = pile.removeAt(pile.size - 1)
        if (c != courant) return c
    }
    return null
}
