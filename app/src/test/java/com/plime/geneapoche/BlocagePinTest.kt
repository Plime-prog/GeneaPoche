package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Test

// Politique de blocage du PIN. Heures fictives en millisecondes.
class BlocagePinTest {

    // Rejoue n echecs de suite depuis un etat vierge ; renvoie les durees de blocage declenchees.
    private fun rejouer(n: Int): List<Long> {
        var erreurs = 0
        var niveau = 0
        val durees = mutableListOf<Long>()
        repeat(n) {
            val e = BlocagePin.apresEchec(erreurs, niveau)
            erreurs = e.erreurs
            niveau = e.niveau
            if (e.dureeMs > 0L) durees.add(e.dureeMs)
        }
        return durees
    }

    @Test
    fun quatre_erreurs_ne_bloquent_pas() {
        assertEquals(emptyList<Long>(), rejouer(4))
        val e = BlocagePin.apresEchec(3, 0)
        assertEquals(4, e.erreurs)
        assertEquals(0L, e.dureeMs)
    }

    @Test
    fun cinquieme_erreur_bloque_30_secondes_et_remet_le_compteur_a_zero() {
        val e = BlocagePin.apresEchec(4, 0)
        assertEquals(30_000L, e.dureeMs)
        assertEquals(0, e.erreurs)
        assertEquals(1, e.niveau)
    }

    @Test
    fun paliers_30s_2min_10min_1h_puis_1h_a_vie() {
        assertEquals(
            listOf(30_000L, 120_000L, 600_000L, 3_600_000L, 3_600_000L, 3_600_000L),
            rejouer(30)
        )
    }

    @Test
    fun le_niveau_plafonne_au_dernier_palier() {
        val e = BlocagePin.apresEchec(4, 4)
        assertEquals(3_600_000L, e.dureeMs)
        assertEquals(4, e.niveau)
    }

    @Test
    fun reste_calcule_sur_l_horloge_ecoulee_sans_redemarrage() {
        // blocage ecrit a t=1000 pour 30 s ; maintenant t=11000 -> 20 s
        assertEquals(20L, BlocagePin.secondesRestantes(11_000, 1_000, 31_000, 5_000_000, 0))
        // modifier l'heure murale du telephone n'a aucun effet
        assertEquals(20L, BlocagePin.secondesRestantes(11_000, 1_000, 31_000, 999_999_999, 123))
    }

    @Test
    fun blocage_termine_ou_inexistant_donne_zero() {
        assertEquals(0L, BlocagePin.secondesRestantes(40_000, 1_000, 31_000, 0, 0))
        assertEquals(0L, BlocagePin.secondesRestantes(5_000, 0, 0, 0, 0))
    }

    @Test
    fun apres_redemarrage_on_se_replie_sur_l_horloge_murale() {
        // horloge ecoulee repartie de zero (2 000 < 90 000 ecrit) : mur = 1 000 000 + 60 s
        assertEquals(40L, BlocagePin.secondesRestantes(2_000, 90_000, 150_000, 1_020_000, 1_060_000))
        assertEquals(0L, BlocagePin.secondesRestantes(2_000, 90_000, 150_000, 1_100_000, 1_060_000))
    }
}
