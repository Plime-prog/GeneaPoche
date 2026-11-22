package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
}

class CommuneSourceTest {
    @Test fun communeLisible() {
        assertEquals("Bourges", communeLisible("Bourges, 18033, Cher"))
        assertEquals("Ville inconnue", communeLisible(" Ville inconnue "))
        assertEquals("Ajaccio", communeLisible("Ajaccio, 2A004, Corse-du-Sud"))
    }

    @Test fun communeAVerifier() {
        org.junit.Assert.assertTrue(communeAVerifier("Bourge"))
        org.junit.Assert.assertFalse(communeAVerifier("Bourges, 18033, Cher"))
        org.junit.Assert.assertFalse(communeAVerifier(""))
    }
}

class GeographieUtiliseeTest {
    @Test fun departementsUtilises() {
        val p = Personne(id = 1, nom = "A")
        val r = mapOf("37" to listOf(p), "18" to listOf(p), "2A" to listOf(p), "99" to emptyList<Personne>())
        assertEquals(listOf("18", "37", "2A").sorted(), departementsUtilises(r).sorted())
        assertEquals("18", departementsUtilises(r).first())
        assertEquals(3, departementsUtilises(r).size)
    }

    @Test fun biographieEnfantsChrono() {
        val pere = Personne(id = 1, nom = "P", sexe = "M")
        val e1 = Personne(id = 2, nom = "P", prenoms = "Tard", pere = 1, dateNaissance = "1910")
        val e2 = Personne(id = 3, nom = "P", prenoms = "Tot", pere = 1, dateNaissance = "1900")
        pere.enfants.addAll(listOf(2L, 3L))
        val r = recitBiographie(pere, listOf(pere, e1, e2)).first { it.contains("enfant") }
        assertTrue(r, r.indexOf("Tot") in 0 until r.indexOf("Tard"))
    }
}
