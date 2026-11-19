package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VuesArbre2Test {
    private val a = Personne(id = 1, nom = "A", prenoms = "a")
    private val b = Personne(id = 2, nom = "A", prenoms = "b", pere = 1)
    private val c = Personne(id = 3, nom = "A", prenoms = "c", pere = 1)
    private val d = Personne(id = 4, nom = "A", prenoms = "d", pere = 2)
    private val tous = listOf(a, b, c, d)

    @Test
    fun descendance_repliee_puis_depliee() {
        assertEquals(listOf(1L), lignesDescendance(a, tous, emptySet()).map { it.p.id })
        val l = lignesDescendance(a, tous, setOf(1L))
        assertEquals(listOf(1L, 2L, 3L), l.map { it.p.id })
        assertEquals(listOf(0, 1, 1), l.map { it.niveau })
        val t = lignesDescendance(a, tous, setOf(1L, 2L)).map { it.p.id }
        assertEquals(listOf(1L, 2L, 4L, 3L), t)
    }

    @Test
    fun tous_les_descendants_sans_la_personne() {
        assertEquals(setOf(2L, 3L, 4L), tousDescendants(a, tous))
        assertTrue(tousDescendants(d, tous).isEmpty())
    }

    @Test
    fun boucle_de_donnees_sans_blocage() {
        val x = Personne(id = 10, nom = "X", prenoms = "x", pere = 11)
        val y = Personne(id = 11, nom = "Y", prenoms = "y", pere = 10)
        assertEquals(setOf(11L), tousDescendants(x, listOf(x, y)))
        assertTrue(lignesDescendance(x, listOf(x, y), setOf(10L, 11L)).size <= 2)
    }

    @Test
    fun completude_par_generation() {
        val g = completudeAscendance(d, tous)
        assertEquals(8, g.size)
        assertEquals(1, g[0].connus)   // seul le pere (b) est connu
        assertEquals(2, g[0].possibles)
        assertEquals(1, g[1].connus)   // grand-pere a
        assertEquals(4, g[1].possibles)
        assertEquals(0, g[2].connus)
        assertEquals(256, g[7].possibles)
    }

    @Test
    fun parents_manquants_dans_l_ordre_des_generations() {
        val m = ancetresManquants(d, tous)
        assertEquals("mère", m[0].second)      // mere de d
        assertEquals(4L, m[0].first.id)
        assertTrue(m.any { it.first.id == 1L && it.second == "père" })
    }

    @Test
    fun noms_de_generation() {
        assertEquals("Parents", nomGeneration(1))
        assertEquals("Génération 6", nomGeneration(6))
    }
}
