package com.plime.geneapoche

// v0.126 : politique de blocage du code PIN, extraite de Stockage (comportement
// identique) pour etre testee. Fonctions pures : l'heure est passee en argument.
//
// Regle : 5 erreurs de suite -> blocage de 30 s, puis 2 min, 10 min, 1 h, 1 h...
// (le palier ne redescend qu'apres un deverrouillage reussi).
object BlocagePin {
    const val ERREURS_AVANT_PALIER = 5
    val DUREES_BLOCAGE = longArrayOf(30_000L, 120_000L, 600_000L, 3_600_000L)

    class Etat(
        val erreurs: Int,          // compteur apres cet echec (0 si un palier est atteint)
        val niveau: Int,           // palier apres cet echec
        val dureeMs: Long          // duree du nouveau blocage, 0 si aucun
    )

    fun apresEchec(erreursAvant: Int, niveau: Int): Etat {
        val erreurs = erreursAvant + 1
        if (erreurs < ERREURS_AVANT_PALIER) return Etat(erreurs, niveau, 0L)
        val duree = if (niveau < DUREES_BLOCAGE.size) DUREES_BLOCAGE[niveau] else DUREES_BLOCAGE.last()
        val niveauSuivant = if (niveau < DUREES_BLOCAGE.size) niveau + 1 else niveau
        return Etat(0, niveauSuivant, duree)
    }

    // Secondes restantes. Horloge normale (elapsedRealtime) tant qu'elle n'a pas
    // ete remise a zero ; apres un redemarrage (maintenant < ecrit), repli sur
    // l'horloge murale.
    fun secondesRestantes(
        maintenantElapsed: Long, ecritElapsed: Long, jusquaElapsed: Long,
        maintenantMur: Long, jusquaMur: Long
    ): Long {
        val reste = if (maintenantElapsed >= ecritElapsed) {
            (jusquaElapsed - maintenantElapsed) / 1000L
        } else {
            (jusquaMur - maintenantMur) / 1000L
        }
        return if (reste > 0L) reste else 0L
    }
}
