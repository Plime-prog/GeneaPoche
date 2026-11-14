package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiographieTest {
    private fun p(id: Long, nom: String, pre: String, ne: String = "", de: String = "", sexe: String = "") =
        Personne(id = id, nom = nom, prenoms = pre, dateNaissance = ne, dateDeces = de, sexe = sexe)

    @Test
    fun quandFormate() {
        assertEquals("05/03/1850", quandBio(1850, 3, 5))
        assertEquals("03/1850", quandBio(1850, 3, 0))
        assertEquals("1850", quandBio(1850, 0, 0))
    }

    @Test
    fun friseMelleVieEtHistoire() {
        val a = p(1, "DUPONT", "Paul", "01/01/1780", "01/01/1830", "M")
        val f = friseBiographie(a, listOf(a))
        assertEquals("vie", f.first().genre)
        assertTrue(f.any { it.genre == "histoire" || it.genre == "regime" })
        assertEquals(f.map { it.cle }, f.map { it.cle }.sorted())
        val sans = friseBiographie(a, listOf(a), false)
        assertTrue(sans.none { it.genre == "histoire" || it.genre == "regime" })
    }

    @Test
    fun recitMentionneParentsEtDeces() {
        val pere = p(1, "DUPONT", "Jean", "01/01/1800", "", "M")
        val fils = p(2, "DUPONT", "Paul", "01/01/1830", "01/01/1900", "M")
        fils.pere = 1
        val r = recitBiographie(fils, listOf(pere, fils))
        assertTrue(r.first().contains("fils de"))
        assertTrue(r.last().contains("70 ans"))
    }
}
