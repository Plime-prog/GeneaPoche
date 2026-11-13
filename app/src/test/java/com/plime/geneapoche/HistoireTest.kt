package com.plime.geneapoche

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.101 : base historique locale, annee, chronologie de famille, chemin de
// parente et onglet Autres. Donnees fictives.

class HistoireTest {

    private fun p(id: Long, nom: String, prenoms: String, ne: String = "", de: String = "", sexe: String = ""): Personne =
        Personne(id = id, nom = nom, prenoms = prenoms, dateNaissance = ne, dateDeces = de, sexe = sexe)

    @Test
    fun ecartsEnClair() {
        assertEquals("le jour de", libelleEcart(0))
        assertEquals("12 jours après", libelleEcart(12))
        assertEquals("1 jour avant", libelleEcart(-1))
        assertEquals("3 jours avant", libelleEcart(-3))
    }

    @Test
    fun periodeEtRapprochement() {
        assertEquals("Première République – Convention", periodeDe(LocalDate.of(1793, 3, 1))!!.nom)
        assertEquals("Troisième République", periodeDe(LocalDate.of(1900, 1, 1))!!.nom)
        assertNull(periodeDe(LocalDate.of(1600, 1, 1)))
        val r = rapprochementHistorique(LocalDate.of(1792, 10, 4))
        assertNotNull(r)
        assertTrue(r!!.startsWith("12 jours après"))
        assertTrue(r.contains("Première République"))
        // Evenement local : ignore sans departement concerne.
        assertNull(rapprochementHistorique(LocalDate.of(1793, 3, 12), "75", 5))
        assertNotNull(rapprochementHistorique(LocalDate.of(1793, 3, 12), "85", 5))
    }

    @Test
    fun phrasesDUnePersonne() {
        val c = p(1L, "Durand", "Paul", "04/10/1792", sexe = "M")
        val l = phrasesHistoriquesPersonne(c)
        assertEquals(1, l.size)
        assertTrue(l[0].startsWith("Paul est né 12 jours après"))
    }

    @Test
    fun evenementsEtRegimesDeLAnnee() {
        assertTrue(evenementsDeLAnnee(1871).any { it.titre.contains("Francfort") }.not())
        assertTrue(evenementsDeLAnnee(1871, "67").any { it.titre.contains("Francfort") })
        assertTrue(evenementsDeLAnnee(1804).any { it.titre.contains("Code civil") })
        assertEquals(1, regimesDeLAnnee(1804).size)
        assertTrue(regimesDeLAnnee(1850).isEmpty())
    }

    @Test
    fun ligneAnneeNaissancesDecesEtAges() {
        val a = p(1L, "Durand", "Paul", "01/01/1820", "05/05/1889")
        a.dateMariage = "1843"
        val b = p(2L, "Martin", "Marie", "03/03/1845")
        val l = ligneAnnee(listOf(a, b), 1845)
        assertTrue(l.any { it.genre == "N" && it.personneId == 2L })
        assertTrue(l.any { it.genre == "A" && it.texte.contains("25 ans") })
        assertEquals(1, ligneAnnee(listOf(a, b), 1843).count { it.genre == "M" })
        assertEquals(1, ligneAnnee(listOf(a, b), 1889).count { it.genre == "D" })
        // Plus d'age apres le deces.
        assertTrue(ligneAnnee(listOf(a, b), 1890).none { it.genre == "A" && it.personneId == 1L })
    }

    @Test
    fun chronologieFamilleEtIncoherences() {
        val pere = p(1L, "Durand", "Paul", "01/01/1821", "05/05/1889", "M")
        val mere = p(2L, "Martin", "Marie", "01/01/1823", "02/02/1860", "F")
        val fils = p(3L, "Durand", "Jean", "01/06/1844", sexe = "M")
        val fille = p(4L, "Durand", "Anne", "01/06/1865", sexe = "F")
        pere.conjoint = 2L; mere.conjoint = 1L
        pere.dateMariage = "10/10/1843"; mere.dateMariage = "10/10/1843"
        pere.enfants.addAll(listOf(3L, 4L)); mere.enfants.addAll(listOf(3L, 4L))
        val tout = listOf(pere, mere, fils, fille)
        val cp = chronologieFamille(pere, tout)
        assertEquals("Naissance", cp.first().libelle)
        assertEquals("Décès", cp.last().libelle)
        assertTrue(cp.none { it.alerte.isNotEmpty() })
        // La mere ne peut pas avoir un enfant en 1865 (morte en 1860).
        val cm = chronologieFamille(mere, tout)
        assertTrue(cm.any { it.libelle.startsWith("Naissance de la fille") && it.alerte.isNotEmpty() })
        // Ordre chronologique.
        assertEquals(cp.map { it.cle }.sorted(), cp.map { it.cle })
    }

    @Test
    fun cheminDeParente() {
        val gp = p(1L, "Durand", "Louis")
        val pe = p(2L, "Durand", "Jean")
        val en = p(3L, "Durand", "Paul")
        val ep = p(4L, "Martin", "Anne")
        gp.enfants.add(2L); pe.pere = 1L; pe.enfants.add(3L); en.pere = 2L
        en.conjoint = 4L; ep.conjoint = 3L
        val tout = listOf(gp, pe, en, ep)
        val ch = cheminParente(1L, 3L, tout)
        assertEquals(listOf(1L, 2L, 3L), ch.map { it.personneId })
        assertEquals("enfant", ch[1].lien)
        // Par alliance : passe par le conjoint seulement si pas de lien de sang.
        val a = cheminParente(1L, 4L, tout)
        assertEquals(listOf(1L, 2L, 3L, 4L), a.map { it.personneId })
        assertEquals("conjoint", a.last().lien)
        // Aucun lien.
        assertTrue(cheminParente(1L, 99L, tout).isEmpty())
        assertEquals(1, cheminParente(1L, 1L, tout).size)
        assertFalse(cheminParente(2L, 1L, tout).isEmpty())
        assertEquals("parent", cheminParente(2L, 1L, tout)[1].lien)
    }

    @Test
    fun autresSontLesPersonnesSansNumero() {
        val a = p(1L, "A", "a"); a.sosa = "1"
        val b = p(2L, "B", "b"); b.lateral = "1.2"
        val c = p(3L, "C", "c")
        val autres = listOf(a, b, c).filter { it.numeroBrut().isEmpty() }
        assertEquals(listOf(3L), autres.map { it.id })
    }
}
