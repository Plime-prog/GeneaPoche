package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MariageTest {
    private fun p(id: Long, sexe: String) = Personne(id = id, nom = "X", prenoms = "P" + id.toString(), sexe = sexe)

    @Test
    fun epouxSontLiesDansLesDeuxSens() {
        val m = p(1, "M")
        val f = p(2, "F")
        assertTrue(lierConjointsActe(m, f))
        assertEquals(2L, m.conjoint)
        assertEquals(1L, f.conjoint)
        assertFalse(lierConjointsActe(m, f))
    }

    @Test
    fun secondMariageEnConjointSupplementaire() {
        val m = p(1, "M")
        val f1 = p(2, "F")
        val f2 = p(3, "F")
        lierConjointsActe(m, f1)
        assertTrue(lierConjointsActe(m, f2))
        assertEquals(2L, m.conjoint)
        assertTrue(m.conjointsSupp.contains(3L))
        assertEquals(1L, f2.conjoint.takeIf { it != 0L } ?: f2.conjointsSupp.firstOrNull())
    }

    @Test
    fun sexesDuMariage() {
        assertNotNull(refusSexeMariage("F", ""))
        assertNotNull(refusSexeMariage("M", "M"))
        assertNull(refusSexeMariage("M", "F"))
        assertNull(refusSexeMariage("", ""))
    }
}
