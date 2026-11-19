package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationTest {
    @Test
    fun code_aller_retour() {
        val c = codeEcran(3, 123456L)
        assertEquals(3, ongletDeCode(c))
        assertEquals(123456L, idDeCode(c))
        assertEquals(0L, idDeCode(codeEcran(1, 0L)))
    }

    @Test
    fun retour_vers_l_ecran_precedent_dans_l_ordre() {
        val pile = mutableListOf<Long>()
        // Journal -> Personnes -> fiche 7 -> Arbre
        ajouterEcran(pile, codeEcran(1, 0)); ajouterEcran(pile, codeEcran(0, 0)); ajouterEcran(pile, codeEcran(0, 7))
        var courant = codeEcran(2, 0)
        courant = depilerEcran(pile, courant)!!
        assertEquals(codeEcran(0, 7), courant)
        courant = depilerEcran(pile, courant)!!
        assertEquals(codeEcran(0, 0), courant)
        courant = depilerEcran(pile, courant)!!
        assertEquals(codeEcran(1, 0), courant)
        assertNull(depilerEcran(pile, courant))
    }

    @Test
    fun pas_de_doublon_consecutif_et_taille_bornee() {
        val pile = mutableListOf<Long>()
        ajouterEcran(pile, 5L); ajouterEcran(pile, 5L)
        assertEquals(1, pile.size)
        for (i in 1..100) ajouterEcran(pile, i.toLong() + 10)
        assertEquals(TAILLE_MAX_PILE_ECRANS, pile.size)
        assertEquals(110L, pile.last())
    }

    @Test
    fun les_ecrans_identiques_au_courant_sont_sautes() {
        val pile = mutableListOf(codeEcran(1, 0), codeEcran(0, 0), codeEcran(0, 0))
        assertEquals(codeEcran(1, 0), depilerEcran(pile, codeEcran(0, 0)))
    }

    @Test
    fun sous_ecran_dans_le_code() {
        val c = codeEcran(1, 42L, 21)
        assertEquals(1, ongletDeCode(c))
        assertEquals(21, sousDeCode(c))
        assertEquals(42L, idDeCode(c))
        assertEquals(0, sousDeCode(codeEcran(3, 5L)))
        // deux sous-ecrans du meme onglet sont deux ecrans differents
        assert(codeEcran(3, 0L, 1) != codeEcran(3, 0L, 4))
    }
}
