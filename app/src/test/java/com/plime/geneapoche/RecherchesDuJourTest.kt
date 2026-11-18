package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecherchesDuJourTest {

    private val cats = listOf("naissance_acte", "mariage_acte", "deces_acte", "pere", "mere", "conjoint", "enfants", "sexe")

    private fun pistes(n: Int): List<Piste> = (1..n).map { i ->
        Piste(id = i.toLong(), personneId = (i % 12 + 1).toLong(), type = "A", categorie = cats[i % cats.size])
    }

    @Test
    fun au_plus_cinq_et_toutes_differentes() {
        val c = cinqDuJour(pistes(60), 20_000L)
        assertEquals(5, c.size)
        assertEquals(5, c.map { it.id }.toSet().size)
        assertEquals(5, c.map { it.personneId }.toSet().size) // une par personne si possible
        assertTrue(c.groupBy { genreRecherche(it) }.values.all { it.size <= 2 })
    }

    @Test
    fun stable_dans_la_journee_et_change_d_un_jour_a_l_autre() {
        val p = pistes(60)
        assertEquals(cinqDuJour(p, 20_000L).map { it.id }, cinqDuJour(p, 20_000L).map { it.id })
        val jours = (20_000L..20_009L).map { cinqDuJour(p, it).map { x -> x.id } }.toSet()
        assertTrue("les 5 doivent varier d'un jour a l'autre", jours.size >= 8)
    }

    @Test
    fun une_recherche_faite_disparait_sans_deranger_les_autres() {
        val p = pistes(60)
        val avant = cinqDuJour(p, 20_123L)
        val sans = p.filter { it.id != avant[2].id }
        val apres = cinqDuJour(sans, 20_123L)
        assertFalse(apres.any { it.id == avant[2].id })
        assertEquals(5, apres.size)
        assertTrue(apres.map { it.id }.containsAll(avant.filter { it.id != avant[2].id }.map { it.id }))
    }

    @Test
    fun peu_de_recherches_ou_base_peu_variee() {
        assertEquals(0, cinqDuJour(emptyList(), 1L).size)
        assertEquals(3, cinqDuJour(pistes(3), 1L).size)
        // toutes de la meme personne et du meme genre : on complete quand meme a 5
        val meme = (1..8).map { Piste(id = it.toLong(), personneId = 1L, type = "A", categorie = "naissance_acte") }
        assertEquals(5, cinqDuJour(meme, 5L).size)
        // les recherches deja faites ne sont jamais proposees
        val faites = pistes(10).map { it.copy(faite = true) }
        assertEquals(0, cinqDuJour(faites, 5L).size)
    }

    @Test
    fun jour_local_change_a_minuit_local() {
        val un = jourLocal(1_700_000_000_000L)
        val deux = jourLocal(1_700_000_000_000L + 86_400_000L)
        assertEquals(un + 1, deux)
        assertNotEquals(0L, un)
    }
}
