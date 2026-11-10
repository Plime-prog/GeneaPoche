package com.plime.geneapoche

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

// ------------------------------------------------ Tests Geographie (v0.93, 12.5)
// Fonctions pures de repartition geographique : departement d'une
// personne (code INSEE prioritaire, dept stocke en secours), commune
// d'un lieu, voyage chronologique.

class TestsGeographie {

    @Test
    fun repartitionParDepartement() {
        val a = Personne(id = 1L, nom = "MARTIN", lieuNaissance = "Arvert, 17010, Charente-Maritime")
        val b = Personne(id = 2L, nom = "DUPONT", lieuNaissance = "Ajaccio, 2A004, Corse-du-Sud")
        // Ancienne saisie : pas de code INSEE, mais departement stocke.
        val c = Personne(id = 3L, nom = "LEGRAND", deptNaissance = "59")
        // Personne sans lieu identifie : ignoree du denombrement.
        val d = Personne(id = 4L, nom = "SANS")
        val r = repartitionDepartements(listOf(a, b, c, d))
        assertEquals(listOf(a), r["17"])
        assertEquals(listOf(b), r["2A"])
        assertEquals(listOf(c), r["59"])
        assertFalse(r.containsKey(""))
    }

    @Test
    fun departementSecoursDepuisEvenementLibre() {
        // Naissance sans lieu, mais un evenement libre localise.
        val p = Personne(id = 1L, nom = "MARTIN")
        p.evenementsLibres.add(
            EvenementLibre(id = 1L, intitule = "D\u00E9m\u00E9nagement", date = "12/05/1920", lieu = "Roubaix, 59, Nord")
        )
        assertEquals("59", deptDePersonne(p))
    }

    @Test
    fun communeDuLieuAvecEtSansCodeInsee() {
        assertEquals("Arvert", communeDuLieu("Arvert, 17010, Charente-Maritime"))
        // Lieu-dit devant : la commune est le segment AVANT le code.
        assertEquals("Arvert", communeDuLieu("Le Moulin, Arvert, 17010, Charente-Maritime"))
        // Ancienne saisie sans code INSEE : premier segment.
        assertEquals("Roubaix", communeDuLieu("Roubaix, 59, Nord"))
        assertEquals("", communeDuLieu(""))
    }

    @Test
    fun voyageChronologique() {
        val p = Personne(id = 1L, nom = "MARTIN", dateNaissance = "05/07/1890", lieuNaissance = "Arvert, 17010, Charente-Maritime")
        p.dateDeces = "03/02/1960"
        p.lieuDeces = "Royan, 17352, Charente-Maritime"
        val etapes = etapesVoyage(p)
        assertEquals(2, etapes.size)
        assertEquals("Naissance", etapes[0].intitule)
        assertEquals("Arvert, Charente-Maritime", etapes[0].lieu)
        assertEquals("D\u00E9c\u00E8s", etapes[1].intitule)
        assertEquals("Royan, Charente-Maritime", etapes[1].lieu)
    }

    @Test
    fun nomDepartementDepuisCode() {
        assertEquals("Charente-Maritime", nomDepartement("17"))
        assertEquals("Corse-du-Sud", nomDepartement("2A"))
        assertEquals("", nomDepartement("XX"))
    }
}
