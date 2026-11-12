package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.99 : tests JVM de la logique d'enregistrement d'un acte extraite dans
// LogiqueActe.kt, plus test de CHARGE (10 000 personnes synthetiques) sur
// l'index des personnes et la detection de doublons. Donnees fictives.

class TestsLogiqueActe {

    private fun p(id: Long, nom: String, prenoms: String, ne: String = ""): Personne =
        Personne(id = id, nom = nom, prenoms = prenoms, dateNaissance = ne)

    // ---------------------------------------------------------- Cles de comparaison

    @Test
    fun cleNomIgnoreAccentsCassePonctuation() {
        assertEquals(cleNom("DUPONT"), cleNom("dupont"))
        assertEquals(cleNom("Dupont"), cleNom("DUPONT"))
        assertEquals(cleNom("Du Pont"), cleNom("Du-Pont"))
        assertEquals(cleNom("L'Heureux"), cleNom("L Heureux"))
    }

    @Test
    fun clePrenomsIgnoreOrdre() {
        assertEquals(clePrenomsNom("Marie Jeanne"), clePrenomsNom("Jeanne Marie"))
        assertEquals(clePrenomsNom("Jean-Marie"), clePrenomsNom("Jean Marie"))
    }

    // ---------------------------------------------------------- Anti-doublons

    @Test
    fun matchExactMemeDate() {
        val base = listOf(p(1L, "Dupont", "Jean", "12/03/1857"), p(2L, "Martin", "Marie"))
        val m = matchPersonneActe(base, "dupont", "Jean", "12/03/1857")
        assertNotNull(m)
        assertEquals(1L, m!!.id)
    }

    @Test
    fun matchSansOrdreDesPrenoms() {
        val base = listOf(p(1L, "Dupont", "Marie Jeanne"))
        assertNotNull(matchPersonneActe(base, "DUPONT", "Jeanne Marie", ""))
    }

    @Test
    fun dateDifferenteAucunMatch() {
        val base = listOf(p(1L, "Dupont", "Jean", "12/03/1857"))
        assertNull(matchPersonneActe(base, "Dupont", "Jean", "01/01/1860"))
    }

    @Test
    fun saisieVideAucunMatch() {
        val base = listOf(p(1L, "Dupont", "Jean"))
        assertNull(matchPersonneActe(base, "", "", ""))
    }

    // ---------------------------------------------------------- majChamp98

    @Test
    fun nouvelActeCompleteSansEcraser() {
        assertEquals("1890", majChamp98("1890", "1901", edition = false))
        assertEquals("1901", majChamp98("", "1901", edition = false))
    }

    @Test
    fun editionEffaceSiVide() {
        assertEquals("", majChamp98("1890", "", edition = true))
        assertEquals("1901", majChamp98("1890", "1901", edition = true))
    }

    @Test
    fun conversionGregorienne() {
        // 22 Germinal An II = 11/04/1794.
        assertEquals("11/04/1794", majChamp98("", "22 Germinal An II", edition = false, greg = true))
    }

    // ---------------------------------------------------------- renseignerChampsActe

    @Test
    fun renseignerRemplitChampsVides() {
        val c = p(1L, "Durand", "Paul")
        renseignerChampsActe(c, "1850", "Lille", "59", "tisserand", "", "", "1875", edition = false)
        assertEquals("1850", c.dateNaissance)
        assertEquals("Lille", c.lieuNaissance)
        assertEquals("59", c.deptNaissance)
        assertEquals("tisserand", c.profession)
    }

    @Test
    fun renseignerNouvelActeNeEcrasePas() {
        val c = p(1L, "Durand", "Paul", ne = "1840")
        renseignerChampsActe(c, "1850", "", "", "", "", "", "1875", edition = false)
        assertEquals("1840", c.dateNaissance)
    }

    @Test
    fun renseignerEditionEfface() {
        val c = p(1L, "Durand", "Paul", ne = "1840")
        renseignerChampsActe(c, "", "", "", "", "", "", "1875", edition = true)
        assertEquals("", c.dateNaissance)
    }

    @Test
    fun naissanceCalculeeDepuisAge() {
        val c = p(1L, "Durand", "Paul")
        renseignerChampsActe(c, "", "", "", "", "", "", "1913", edition = false,
            age = "34", sourceCalcul = "l'acte de mariage")
        assertEquals("vers 1879", c.naissanceCalculee)
        assertTrue(c.naissanceCalculeeSource.contains("acte de mariage"))
    }

    @Test
    fun naissanceCalculeeJamaisEcrasee() {
        val c = p(1L, "Durand", "Paul")
        c.naissanceCalculee = "vers 1880"
        // Date de naissance deja calculee -> la fiche n'est plus "sans date" :
        // en fill-if-blank la date saisie non vide la remplace, mais la
        // naissance calculee existante n'est pas retouchee par un age.
        c.dateNaissance = "vers 1880"
        renseignerChampsActe(c, "", "", "", "", "", "", "1913", edition = false, age = "34")
        assertEquals("vers 1880", c.naissanceCalculee)
    }

    // ---------------------------------------------------------- lierParentActe

    @Test
    fun lienParentPoseSiAbsent() {
        val enfant = p(1L, "Durand", "Paul", "1860")
        val pere = p(2L, "Durand", "Pierre", "1830")
        lierParentActe(enfant, pere, estMere = false)
        assertEquals(2L, enfant.pere)
        assertTrue(pere.enfants.contains(1L))
    }

    @Test
    fun lienParentRefuseSiIncoherent() {
        // Pere ne APRES l'enfant : liaison refusee.
        val enfant = p(1L, "Durand", "Paul", "1860")
        val pere = p(2L, "Durand", "Pierre", "1900")
        lierParentActe(enfant, pere, estMere = false)
        assertEquals(0L, enfant.pere)
        assertFalse(pere.enfants.contains(1L))
    }

    @Test
    fun lienParentNeDupliquePasEnfants() {
        val enfant = p(1L, "Durand", "Paul", "1860")
        val mere = p(2L, "Durand", "Marie", "1835")
        mere.enfants.add(1L)
        lierParentActe(enfant, mere, estMere = true)
        assertEquals(1, mere.enfants.size)
    }

    // ---------------------------------------------------------- Test de charge (10 000 personnes)

    private fun baseSynthetique(n: Int): List<Personne> =
        (1L..n.toLong()).map { i ->
            p(i, "Nom" + (i % 500).toString(), "Prenom" + i.toString(),
              (1700 + (i % 250)).toString() + "/0" + (1 + (i % 9)).toString() + "/1" + (i % 9).toString())
        }

    @Test
    fun indexRetrouveChaquePersonneSurGrosseBase() {
        val base = baseSynthetique(10000)
        IndexPersonnes.invalider()
        // Toutes les personnes sont retrouvees par id.
        for (id in longArrayOf(1L, 5000L, 10000L)) {
            assertEquals(id, personneParId(base, id)!!.id)
        }
        assertNull(personneParId(base, 0L))
        assertNull(personneParId(base, 99999L))
        // Apres invalidation, l'index est reconstruit et reste correct.
        IndexPersonnes.invalider()
        assertEquals(5000L, personneParId(base, 5000L)!!.id)
    }

    @Test
    fun doublonsPotentielsResteRapideSurGrosseBase() {
        val base = baseSynthetique(10000)
        IndexPersonnes.invalider()
        val debut = System.nanoTime()
        val d = doublonsPotentiels(base)
        val dureeMs = (System.nanoTime() - debut) / 1_000_000L
        // Les 10 000 personnes ont toutes un nom/prenom distinct : aucun doublon.
        assertTrue(d.isEmpty())
        // Detection terminee en moins de 5 s (en pratique < 1 s grace au
        // groupement par nom ; l'ancienne version comparait 50 millions de paires).
        assertTrue("doublonsPotentiels trop lent : " + dureeMs + " ms", dureeMs < 5000L)
    }

    @Test
    fun doublonsPotentielsDetecteToujoursLesVrais() {
        val base = baseSynthetique(2000).toMutableList()
        base.add(p(90001L, "Nom42", "Prenom42", base[41].dateNaissance))
        IndexPersonnes.invalider()
        val d = doublonsPotentiels(base)
        assertEquals(1, d.size)
        assertEquals(90001L, maxOf(d[0].idA, d[0].idB))
    }
}
