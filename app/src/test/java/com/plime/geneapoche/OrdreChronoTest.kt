package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrdreChronoTest {
    @Test fun enfantsParNaissanceInconnuAlaFin() {
        val a = Personne(id = 1, nom = "A", dateNaissance = "05/06/1900")
        val b = Personne(id = 2, nom = "B", dateNaissance = "")
        val c = Personne(id = 3, nom = "C", dateNaissance = "01/02/1900")
        assertEquals(listOf(3L, 1L, 2L), enfantsChrono(listOf(a, b, c)).map { it.id })
    }

    @Test fun conjointsParMariage() {
        val q1 = Personne(id = 2, nom = "X")
        val q2 = Personne(id = 3, nom = "Y", conjoint = 1, dateMariage = "01/01/1920")
        val q3 = Personne(id = 4, nom = "Z")
        val p = Personne(id = 1, nom = "P", conjoint = 2, dateMariage = "01/01/1930")
        p.conjointsSupp.addAll(listOf(3L, 4L))
        val l = conjointsChrono(p, listOf(p, q1, q2, q3))
        assertEquals(listOf(3L, 2L, 4L), l.map { it.id })
    }

    @Test fun urlNormalisee() {
        assertEquals("https://archives.exemple.fr", normaliserUrl(" archives.exemple.fr "))
        assertEquals("https://a.fr/x", normaliserUrl("http://a.fr/x"))
        assertEquals("https://a.fr:8080/x", normaliserUrl("a.fr:8080/x"))
        assertNull(normaliserUrl("javascript:alert(1)"))
        assertNull(normaliserUrl("file:///sdcard/x"))
        assertNull(normaliserUrl("deux mots.fr"))
        assertNull(normaliserUrl("localhost"))
        assertNull(normaliserUrl(""))
    }
}
