package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// v0.86 : tests unitaires JVM de la boite a outils (calendrier republicain,
// calculateur de dates, parente). Donnees STRICTEMENT fictives.

class OutilsTest {

    // ---------------------------------------------------- Calendrier republicain

    @Test
    fun exempleDuGeneanlogiste() {
        // 14 Vendemiaire an VII = vendredi 5 octobre 1798 (julien : 24 septembre).
        val d = republicainVersGregorien(DateRepublicaine(7, 1, 14))
        assertEquals(LocalDate.of(1798, 10, 5), d)
        val i = infoJour(d!!)
        assertEquals("vendredi", i.jourSemaine)
        assertEquals("14 Vend\u00e9miaire an VII", i.republicain)
        assertEquals("Quartidi", i.jourDeDecade)
        assertEquals(2, i.decade)
        assertEquals("R\u00e9s\u00e9da", i.fete)
        assertEquals("24 septembre 1798", i.julien)
        assertTrue(i.anSextileRepub)
        assertFalse(i.gregorienBissextile)
    }

    @Test
    fun inverseDuGregorienVersLeRepublicain() {
        val r = gregorienVersRepublicain(LocalDate.of(1798, 10, 5))
        assertEquals(DateRepublicaine(7, 1, 14), r)
        assertEquals(DateRepublicaine(1, 1, 1), gregorienVersRepublicain(LocalDate.of(1792, 9, 22)))
        assertNull(gregorienVersRepublicain(LocalDate.of(1792, 9, 21)))
        assertNull(gregorienVersRepublicain(LocalDate.of(1806, 1, 1)))
    }

    @Test
    fun anneesXIIetXIIIDebutentLes24Et23Septembre() {
        // Correction v0.86 : an XII = 24/09/1803, an XIII = 23/09/1804.
        assertEquals(LocalDate.of(1803, 9, 24), debutDeLAn(12))
        assertEquals(LocalDate.of(1804, 9, 23), debutDeLAn(13))
        assertEquals(LocalDate.of(1803, 9, 23), republicainVersGregorien(DateRepublicaine(11, 13, 6)))
    }

    @Test
    fun dureeDesAnsRespecteLesSextiles() {
        for (an in 1..13) {
            assertEquals("an " + an, if (anSextile(an)) 366 else 365, dureeDeLAn(an))
        }
    }

    @Test
    fun allerRetourSurTousLesJoursDuCalendrier() {
        var d = LocalDate.of(1792, 9, 22)
        val fin = LocalDate.of(1805, 12, 31)
        var n = 0
        while (!d.isAfter(fin)) {
            val r = gregorienVersRepublicain(d)
            assertEquals("aller " + d, d, republicainVersGregorien(r!!))
            // Coherence avec l'analyseur de saisie existant (mois 1 a 12).
            if (r.mois <= 12) {
                val saisie = r.jour.toString() + " " + MoisRepubliAffiches[r.mois - 1] + " an " + anRomain(r.an)
                assertEquals("saisie " + saisie, dateCourte(d), repubAGregorien(saisie))
            }
            d = d.plusDays(1)
            n++
        }
        assertEquals(4848, n)
    }

    @Test
    fun jourComplementaireEtSextile() {
        assertEquals(LocalDate.of(1799, 9, 22), republicainVersGregorien(DateRepublicaine(7, 13, 6)))
        assertNull(republicainVersGregorien(DateRepublicaine(8, 13, 6)))
        assertNull(republicainVersGregorien(DateRepublicaine(8, 13, 7)))
        val i = infoJour(LocalDate.of(1799, 9, 22))
        assertEquals("6e jour compl\u00e9mentaire an VII", i.republicain)
        assertEquals("F\u00eate de la R\u00e9volution", i.fete)
        assertEquals(0, i.decade)
    }

    @Test
    fun nomsDesJoursComplets() {
        for (mois in 1..12) {
            for (jour in 1..30) {
                assertTrue("mois " + mois + " jour " + jour, feteRepubli(mois, jour).isNotEmpty())
            }
        }
        assertEquals("Raisin", feteRepubli(1, 1))
        assertEquals("Tonneau", feteRepubli(1, 30))
        assertEquals("Primev\u00e8re", feteRepubli(7, 1))
        assertEquals("Panier", feteRepubli(12, 30))
        assertEquals("F\u00eate de la Vertu", feteRepubli(13, 1))
    }

    @Test
    fun analyseurAccepteLesAccents() {
        // Correction v0.86 : "Vend\u00e9miaire", "Pluvi\u00f4se"... etaient refuses.
        assertEquals("05/10/1798", repubAGregorien("14 Vend\u00e9miaire an VII"))
        assertEquals("05/10/1798", repubAGregorien("14 Vendemiaire an VII"))
        assertTrue(dateValide("3 Pluvi\u00f4se an V"))
        assertTrue(dateValide("12 Vent\u00f4se an X"))
        assertTrue(dateValide("5 Flor\u00e9al an III"))
    }

    // ---------------------------------------------------- Calculateur de dates

    @Test
    fun lectureDeDatesVariees() {
        assertEquals(LocalDate.of(1798, 10, 5), lireDate("05/10/1798"))
        assertEquals(LocalDate.of(1798, 10, 5), lireDate("5.10.1798"))
        assertEquals(LocalDate.of(1798, 10, 5), lireDate("5 octobre 1798"))
        assertEquals(LocalDate.of(1843, 5, 1), lireDate("1er mai 1843"))
        assertEquals(LocalDate.of(1843, 2, 12), lireDate("12 f\u00e9vr. 1843"))
        assertEquals(LocalDate.of(1798, 10, 5), lireDate("14 Vend\u00e9miaire an VII"))
        assertNull(lireDate("31/02/1800"))
        assertNull(lireDate("5 jui 1800"))
        assertNull(lireDate(""))
        assertEquals(DatePartielle(1821, null, null), lireDatePartielle("1821"))
        assertEquals(DatePartielle(1842, 3, null), lireDatePartielle("03/1842"))
        assertEquals(DatePartielle(1842, 3, null), lireDatePartielle("mars 1842"))
        assertEquals(DatePartielle(1842, 3, 14), lireDatePartielle("14/03/1842"))
    }

    @Test
    fun ageEtEcartDesExemples() {
        val a = ecartDates(lireDatePartielle("12/02/1843")!!, lireDatePartielle("07/06/1867")!!)
        assertEquals("24 ans, 3 mois, 26 jours", libelleDuree(a))
        val b = ecartDates(lireDatePartielle("14/03/1842")!!, lireDatePartielle("27/09/1867")!!)
        assertEquals("25 ans, 6 mois, 13 jours", libelleDuree(b))
        assertFalse(b.inverse)
        assertEquals(9328L, b.totalJours)
    }

    @Test
    fun intervalleEtDatesIncompletes() {
        val an = ecartDates(lireDatePartielle("1821")!!, lireDatePartielle("1874")!!)
        assertEquals("53 ans", libelleDuree(an))
        assertEquals("annee", an.precision)
        val mois = ecartDates(lireDatePartielle("03/1842")!!, lireDatePartielle("09/1867")!!)
        assertEquals("25 ans, 6 mois", libelleDuree(mois))
        val mixte = ecartDates(lireDatePartielle("14/03/1842")!!, lireDatePartielle("1867")!!)
        assertEquals("25 ans", libelleDuree(mixte))
    }

    @Test
    fun ordreInverseEstSignale() {
        val d = ecartDates(lireDatePartielle("07/06/1867")!!, lireDatePartielle("12/02/1843")!!)
        assertTrue(d.inverse)
        assertEquals("24 ans, 3 mois, 26 jours", libelleDuree(d))
        assertTrue(texteEcart("A", "B", d).contains("pr\u00e9c\u00e8de"))
    }

    @Test
    fun dateMoinsPlusUneDuree() {
        val d = LocalDate.of(1842, 3, 12)
        assertEquals(LocalDate.of(1915, 7, 24), ajouterDuree(d, 1, 73, 4, 12))
        assertEquals(LocalDate.of(1768, 10, 31), ajouterDuree(d, -1, 73, 4, 12))
        assertEquals(LocalDate.of(1900, 2, 28), ajouterDuree(LocalDate.of(1900, 1, 31), 1, 0, 1, 0))
        assertEquals("0 jour", libelleDuree(ecartDates(DatePartielle(1900, 1, 1), DatePartielle(1900, 1, 1))))
        assertEquals("1 an, 1 mois, 1 jour",
            libelleDuree(ecartDates(DatePartielle(1900, 1, 1), DatePartielle(1901, 2, 2))))
    }

    @Test
    fun jourDeLaSemaineEtMilliers() {
        assertEquals("lundi", jourSemaineDe(LocalDate.of(1842, 3, 14)))
        assertEquals("9\u00a0328", separerMilliers(9328L))
        assertEquals("123", separerMilliers(123L))
        assertEquals("1\u00a0000\u00a0000", separerMilliers(1000000L))
    }

    @Test
    fun calendrierJulienEtBissextile() {
        val i = infoJour(LocalDate.of(1800, 3, 1))
        assertEquals("18 f\u00e9vrier 1800", i.julien)
        assertFalse(i.gregorienBissextile)
        assertTrue(i.julienBissextile)
        assertEquals("19 d\u00e9cembre 1582", infoJour(LocalDate.of(1582, 12, 29)).julien)
    }

    // ---------------------------------------------------- Parente

    private fun arbre(): Map<Long, NoeudParente> {
        val l = listOf(
            NoeudParente(1, 0, 0, "M", listOf(2)),        // Jean (grand-pere)
            NoeudParente(2, 0, 0, "F", listOf(1)),        // Ines (grand-mere)
            NoeudParente(10, 1, 2, "M", listOf(12)),      // Paul (pere de Pierre)
            NoeudParente(11, 1, 2, "F", listOf(13)),      // Anne (tante)
            NoeudParente(12, 0, 0, "F", listOf(10)),      // Rose (mere de Pierre)
            NoeudParente(13, 0, 0, "M", listOf(11)),      // Luc (mari d'Anne)
            NoeudParente(14, 0, 0, "F", listOf(10)),      // Eve (autre epouse de Paul)
            NoeudParente(20, 10, 12, "M", listOf(50)),    // Pierre
            NoeudParente(21, 10, 12, "F", emptyList()),   // Julie (soeur)
            NoeudParente(22, 10, 14, "M", emptyList()),   // Marc (demi-frere)
            NoeudParente(30, 13, 11, "F", emptyList()),   // Marie (cousine germaine)
            NoeudParente(40, 0, 30, "F", emptyList()),    // Lea (fille de Marie)
            NoeudParente(50, 51, 0, "F", listOf(20)),     // Claire (epouse de Pierre)
            NoeudParente(51, 0, 0, "M", emptyList()),     // Hugo (pere de Claire)
            NoeudParente(70, 20, 50, "F", listOf(71)),    // Lou (fille de Pierre)
            NoeudParente(71, 0, 0, "M", listOf(70)),      // Theo (mari de Lou)
            NoeudParente(99, 0, 0, "M", emptyList())      // Inconnu
        )
        return l.associateBy { it.id }
    }

    private val noms = mapOf(
        1L to "Jean", 2L to "In\u00e8s", 10L to "Paul", 11L to "Anne", 12L to "Rose", 13L to "Luc",
        14L to "\u00c8ve", 20L to "Pierre", 21L to "Julie", 22L to "Marc", 30L to "Marie", 40L to "L\u00e9a",
        50L to "Claire", 51L to "Hugo", 70L to "Lou", 71L to "Th\u00e9o", 99L to "Inconnu"
    )

    private fun parente(x: Long, y: Long) = calculerParente(x, y, arbre()) { noms[it] ?: "?" }

    @Test
    fun cousineGermaine() {
        val r = parente(20, 30)
        assertTrue(r.trouve)
        assertEquals("cousine germaine", r.lien)
        assertEquals("Marie est la cousine germaine de Pierre", r.phrase)
        assertEquals("Pierre est le cousin germain de Marie", r.phraseInverse)
        assertEquals(4, r.degreCivil)
        assertEquals(listOf(1L, 2L), r.ancetresCommuns)
    }

    @Test
    fun familleProche() {
        assertEquals("Julie est la s\u0153ur de Pierre", parente(20, 21).phrase)
        assertEquals("Marc est le demi-fr\u00e8re de Pierre", parente(20, 22).phrase)
        assertEquals("Anne est la tante de Pierre", parente(20, 11).phrase)
        assertEquals("Pierre est le neveu d'Anne", parente(20, 11).phraseInverse)
        assertEquals("Jean est le grand-p\u00e8re de Pierre", parente(20, 1).phrase)
        assertEquals("Pierre est le petit-fils de Jean", parente(20, 1).phraseInverse)
        assertEquals("Paul est le p\u00e8re de Pierre", parente(20, 10).phrase)
        assertEquals("Lou est la fille de Pierre", parente(20, 70).phrase)
        assertEquals(2, parente(20, 21).degreCivil)
        assertEquals(3, parente(20, 11).degreCivil)
    }

    @Test
    fun modeDeBretagne() {
        assertEquals("L\u00e9a est la ni\u00e8ce \u00e0 la mode de Bretagne de Pierre", parente(20, 40).phrase)
        assertEquals("Pierre est l'oncle \u00e0 la mode de Bretagne de L\u00e9a", parente(20, 40).phraseInverse)
    }

    @Test
    fun parenteParAlliance() {
        assertEquals("Claire est l'\u00e9pouse de Pierre", parente(20, 50).phrase)
        assertEquals("Hugo est le beau-p\u00e8re de Pierre", parente(20, 51).phrase)
        assertEquals("Th\u00e9o est le gendre de Pierre", parente(20, 71).phrase)
        assertTrue(parente(20, 51).alliance)
        assertEquals(0, parente(20, 51).degreCivil)
    }

    @Test
    fun aucunLienEtMemePersonne() {
        val r = parente(20, 99)
        assertFalse(r.trouve)
        assertTrue(r.phrase.contains("Aucun lien"))
        assertEquals("C'est la m\u00eame personne.", parente(20, 20).phrase)
        assertFalse(calculerParente(20, 12345, arbre()) { "?" }.trouve)
    }

    @Test
    fun descendanceEtAscendanceLointaines() {
        val l = ArrayList<NoeudParente>()
        // Chaine de 8 generations : id 1 est l'ancetre, id 8 le dernier descendant.
        for (i in 1L..8L) l.add(NoeudParente(i, 0, if (i == 1L) 0 else i - 1, "F", emptyList()))
        val m = l.associateBy { it.id }
        val r1 = calculerParente(8, 4, m) { "n" + it }
        assertEquals("n4 est l'arri\u00e8re-arri\u00e8re-grand-m\u00e8re de n8", r1.phrase)
        val r2 = calculerParente(8, 1, m) { "n" + it }
        assertTrue(r2.lien.contains("7e g\u00e9n\u00e9ration"))
        val r3 = calculerParente(1, 4, m) { "n" + it }
        assertEquals("n4 est l'arri\u00e8re-petite-fille de n1", r3.phrase)
    }

    @Test
    fun donneesCorrompuesNeBouclentPas() {
        val m = mapOf(
            1L to NoeudParente(1, 2, 0, "M", emptyList()),
            2L to NoeudParente(2, 1, 0, "M", emptyList()),
            3L to NoeudParente(3, 3, 0, "F", emptyList())
        )
        assertTrue(calculerParente(1, 3, m) { "x" }.phrase.isNotEmpty())
        assertTrue(calculerParente(3, 1, m) { "x" }.phrase.isNotEmpty())
    }
}
