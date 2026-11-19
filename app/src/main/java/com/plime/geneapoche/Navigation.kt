package com.plime.geneapoche

// v0.141 : pile de retour arriere. Un ecran = (onglet, sous-ecran, fiche ouverte)
// code dans un seul Long ((onglet * 100 + sous-ecran) * 10^10 + identifiant de
// fiche ; 0 = aucune fiche). v0.143 : le sous-ecran (puce du Journal, vue Stat.,
// vue de l'arbre, outil de la Boite a outils) fait partie de l'ecran.

private const val FACTEUR_ECRAN = 10_000_000_000L
const val TAILLE_MAX_PILE_ECRANS = 40

fun codeEcran(onglet: Int, fiche: Long, sous: Int = 0): Long =
    (onglet.toLong() * 100L + sous.toLong()) * FACTEUR_ECRAN + fiche
fun ongletDeCode(code: Long): Int = (code / FACTEUR_ECRAN / 100L).toInt()
fun sousDeCode(code: Long): Int = ((code / FACTEUR_ECRAN) % 100L).toInt()
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
