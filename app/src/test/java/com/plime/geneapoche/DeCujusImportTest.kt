package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Choix du de cujus apres un remplacement de base. Donnees fictives.
class DeCujusImportTest {

    @Test
    fun plus_petit_sosa_numerique_gagne() {
        val l = listOf(
            Personne(id = 1L, sosa = "12"),
            Personne(id = 2L, sosa = "3"),
            Personne(id = 3L, sosa = "7")
        )
        assertEquals(2L, choisirDeCujusApresImport(l)!!.id)
    }

    @Test
    fun sosa_non_numerique_ou_vide_est_ignore() {
        val l = listOf(
            Personne(id = 1L, sosa = ""),
            Personne(id = 2L, sosa = "abc"),
            Personne(id = 3L, sosa = " 5 ")
        )
        assertEquals(3L, choisirDeCujusApresImport(l)!!.id)
    }

    @Test
    fun sans_aucun_sosa_la_premiere_personne_est_choisie() {
        val l = listOf(Personne(id = 9L), Personne(id = 4L))
        assertEquals(9L, choisirDeCujusApresImport(l)!!.id)
    }

    @Test
    fun liste_vide_ne_donne_personne() {
        assertNull(choisirDeCujusApresImport(emptyList()))
    }
}
