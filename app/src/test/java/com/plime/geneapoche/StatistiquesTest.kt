package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.100 : statistiques de base, trous genealogiques, recherche avancee et
// naissance approximative affichee. Donnees fictives.

class StatistiquesTest {

    private fun p(id: Long, nom: String, prenoms: String, ne: String = "", de: String = ""): Personne =
        Personne(id = id, nom = nom, prenoms = prenoms, dateNaissance = ne, dateDeces = de)

    private fun famille(): List<Personne> {
        val a = p(1L, "Durand", "Paul", "12/05/1850", "03/02/1920")
        val b = p(2L, "Martin", "Marie", "01/01/1855", "01/01/1930")
        val c = p(3L, "Durand", "Jean", "10/10/1880")
        a.conjoint = 2L; b.conjoint = 1L
        a.enfants.add(3L); b.enfants.add(3L)
        a.dateMariage = "1878"; b.dateMariage = "1878"
        a.sosa = "2"; b.sosa = "3"; c.sosa = "1"
        a.profession = "tisserand"; b.profession = "Tisserand"
        return listOf(a, b, c)
    }

    @Test
    fun statsBaseCompteLesFondamentaux() {
        val s = statsBase(famille(), emptyList())
        assertEquals(3, s.individus)
        assertEquals(1, s.familles)
        assertEquals(2, s.ancetres)
        assertEquals(2, s.generations)
        assertEquals(2, s.patronymes)
        assertEquals(1, s.metiers)
        assertEquals(1850, s.anneeDebut)
        assertEquals(1930, s.anneeFin)
        assertEquals(1L, s.plusAncien!!.id)
    }

    @Test
    fun dureeDeVieEtAgeAuMariage() {
        val s = statsBase(famille(), emptyList())
        // 70 ans et 75 ans -> 72,5 ; mariage 1878 : 28 ans et 23 ans -> 25,5
        assertEquals(72.5, s.vieMoyenne!!, 0.01)
        assertEquals(25.5, s.ageMariageMoyen!!, 0.01)
        assertEquals(1.0, s.enfantsParCouple!!, 0.01)
    }

    @Test
    fun descendantsDepuisLeDeCujus() {
        val ps = famille()
        // Le de cujus (Sosa 1) est Jean : il n'a pas de descendant.
        assertEquals(0, statsBase(ps, emptyList()).descendants)
    }

    @Test
    fun documentationEtSources() {
        val ps = famille()
        val cit = listOf(Citation(1L, 9L, 1L, "M"))
        val s = statsBase(ps, cit)
        assertEquals(3, s.naissances.nombre)
        assertEquals(100, s.naissances.pourcent)
        assertEquals(1, s.mariages.nombre)
        assertEquals(1, s.mariagesSources.nombre)
        assertEquals(1, s.personnesSourcees.nombre)
        assertEquals(33, s.personnesSourcees.pourcent)
    }

    @Test
    fun tauxSansBaseNeDivisePasParZero() {
        assertEquals(0, Taux(0, 0).pourcent)
        assertEquals(0f, Taux(0, 0).fraction, 0f)
    }

    @Test
    fun trousGenealogiquesSommeLesManquants() {
        val ps = listOf(p(1L, "Durand", "Paul", "12/05/1850", "03/02/1920"), p(2L, "Martin", "Anne"))
        val (manque, attendu) = trousGenealogiques(ps)
        assertTrue(attendu >= 4)
        assertTrue(manque > 0)
        assertTrue(manque <= attendu)
        // Une base vide n'a aucun trou.
        assertEquals(Pair(0, 0), trousGenealogiques(emptyList()))
    }

    @Test
    fun filtreParTexteLieuEtPeriode() {
        val ps = famille()
        ps[0].lieuNaissance = "Bourges, 18033, Cher"
        assertEquals(1, filtrerPersonnes(ps, FiltresRecherche(lieu = "bourges")).size)
        assertEquals(2, filtrerPersonnes(ps, FiltresRecherche(texte = "durand")).size)
        // Periode : evenements entre 1925 et 1935 -> seulement Marie (deces 1930).
        val r = filtrerPersonnes(ps, FiltresRecherche(anneeDe = 1925, anneeA = 1935))
        assertEquals(1, r.size)
        assertEquals(2L, r[0].id)
        assertFalse(FiltresRecherche().actif)
        assertTrue(FiltresRecherche(lieu = "x").actif)
    }

    @Test
    fun filtreActeManquant() {
        val ps = famille()
        // Jean (1880) n'a pas de deces mais n'est pas un vivant presume.
        val r = filtrerPersonnes(ps, FiltresRecherche(manquants = setOf("D")))
        assertTrue(r.any { it.id == 3L })
        // Paul : naissance sans lieu complet -> manquante.
        assertTrue(acteManquant(ps[0], "N"))
    }

    @Test
    fun naissanceApproximativeSeSubstitueAuxAges() {
        val c = p(1L, "Durand", "Paul")
        c.naissanceCalculee = "vers 1879"
        assertEquals(9999, anneeNaissance(p(2L, "X", "Y")))
        assertEquals(1879, anneeNaissance(c))
        assertTrue(ageDePersonne(c, "01/01/1913").contains("~"))
        // Date exacte : pas de tilde.
        c.dateNaissance = "05/05/1879"
        assertFalse(ageDePersonne(c, "01/01/1913").contains("~"))
        assertNotNull(ageDePersonne(c, "01/01/1913"))
        assertNull(null)
    }
}
