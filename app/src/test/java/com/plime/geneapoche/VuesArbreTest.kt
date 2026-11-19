package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VuesArbreTest {
    private val gp = Personne(id = 1, nom = "A", prenoms = "a", sexe = "M", enfants = listOf(2L))
    private val pere = Personne(id = 2, nom = "A", prenoms = "b", sexe = "M", pere = 1)
    private val e1 = Personne(id = 3, nom = "A", prenoms = "c", pere = 2, dateNaissance = "1990")
    private val e2 = Personne(id = 4, nom = "A", prenoms = "d", pere = 2, dateNaissance = "1985")
    private val pe = Personne(id = 5, nom = "A", prenoms = "e", pere = 4)
    private val tous = listOf(gp, pere, e1, e2, pe)

    @Test
    fun enfants_par_liens_de_la_fiche_et_des_enfants_sans_doublon_tries_par_naissance() {
        val l = enfantsDe(pere, tous)
        assertEquals(listOf(4L, 3L), l.map { it.id })
        assertEquals(listOf(2L), enfantsDe(gp, tous).map { it.id })
    }

    @Test
    fun sablier_deux_generations_de_descendants() {
        val d = descendantsSablier(pere, tous)
        assertEquals(2, d.size)
        assertEquals(listOf(5L), d.first { it.first.id == 4L }.second.map { it.id })
        assertTrue(d.first { it.first.id == 3L }.second.isEmpty())
    }

    @Test
    fun nom_court_de_periode() {
        assertEquals("Ancien Régime", nomCourtPeriode("Ancien Régime (Louis XV et Louis XVI)"))
        assertEquals("Convention", nomCourtPeriode("Première République – Convention"))
        assertEquals("Consulat", nomCourtPeriode("Consulat"))
    }

    @Test
    fun reperes_historiques_dans_la_plage_nationaux_et_limites() {
        val r = reperesHistoriques(1789, 1815, 5)
        assertTrue(r.size <= 5)
        assertTrue(r.all { it.annee in 1789..1815 && it.depts.isEmpty() })
        assertEquals(r.map { it.annee }.sorted(), r.map { it.annee })
    }
}
