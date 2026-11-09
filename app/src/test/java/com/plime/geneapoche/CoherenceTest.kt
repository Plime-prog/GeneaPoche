package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.88 : tests JVM des "Anomalies a verifier".
class CoherenceTest {

    private fun n(
        id: Long, nom: String, sexe: String = "", naiss: String = "", deces: String = "",
        mar: String = "", bap: String = "", sep: String = "", pere: Long = 0, mere: Long = 0,
        conj: List<Long> = emptyList(), libres: List<Pair<String, String>> = emptyList(),
        lieux: List<EvLieu> = emptyList()
    ) = NoeudCoh(id, nom, sexe, naiss, bap, mar, deces, sep, pere, mere, conj, libres, lieux)

    private fun types(l: List<NoeudCoh>): List<String> = anomaliesAVerifier(l).map { it.type }

    @Test
    fun intervallesDeDates() {
        val j = intervalleDe("05/10/1798")!!
        assertTrue(j.exact)
        val m = intervalleDe("mars 1842")!!
        assertEquals(java.time.LocalDate.of(1842, 3, 31), m.fin)
        val a = intervalleDe("1750")!!
        assertEquals(java.time.LocalDate.of(1750, 1, 1), a.debut)
        assertEquals(java.time.LocalDate.of(1749, 12, 31), intervalleDe("avant 1750")!!.fin)
        assertEquals(java.time.LocalDate.of(1751, 1, 1), intervalleDe("apr\u00e8s 1750")!!.debut)
        assertEquals(java.time.LocalDate.of(1747, 1, 1), intervalleDe("vers 1750")!!.debut)
        assertEquals(java.time.LocalDate.of(1798, 10, 5), intervalleDe("14 Vend\u00e9miaire an VII")!!.debut)
        assertEquals(null, intervalleDe("inconnu"))
        assertEquals(null, intervalleDe(""))
    }

    @Test
    fun exempleNeQuatreMoisApresLeDecesDuPere() {
        val l = listOf(
            n(1, "Pierre Dupont", "M", "01/01/1800", "10/03/1840"),
            n(2, "Jean Dupont", "M", "10/07/1840", pere = 1)
        )
        val a = anomaliesAVerifier(l)
        assertEquals(1, a.size)
        assertEquals("naissance-posthume", a[0].type)
        assertEquals("Jean Dupont est n\u00e9 4 mois apr\u00e8s le d\u00e9c\u00e8s suppos\u00e9 de son p\u00e8re Pierre Dupont (naissance posthume possible).", a[0].texte)
        assertEquals(2, a[0].niveau)
        assertEquals(2, a[0].details.size)
    }

    @Test
    fun naissanceTresLongtempsApresLeDecesDuPere() {
        val l = listOf(
            n(1, "Pierre", "M", "1800", "10/03/1840"),
            n(2, "Jean", "M", "10/07/1841", pere = 1)
        )
        assertEquals(listOf("naissance-longtemps-apres-deces-pere"), types(l))
    }

    @Test
    fun pasDAlerteSiNaissanceAvantDecesOuSiPrecisionInsuffisante() {
        val l = listOf(
            n(1, "Pierre", "M", "1800", "1840"),
            n(2, "Jean", "M", "1840", pere = 1)
        )
        assertTrue(anomaliesAVerifier(l).isEmpty())
        val l2 = listOf(
            n(1, "Pierre", "M", "1800", "10/03/1840"),
            n(2, "Jean", "M", "10/02/1840", pere = 1)
        )
        assertTrue(anomaliesAVerifier(l2).isEmpty())
    }

    @Test
    fun enfantAvantSesParents() {
        val l = listOf(
            n(1, "Pere", "M", "1850"), n(2, "Mere", "F", "1852"),
            n(3, "Enfant", "M", "1845", pere = 1, mere = 2)
        )
        val t = types(l)
        assertTrue(t.contains("enfant-avant-pere"))
        assertTrue(t.contains("enfant-avant-mere"))
    }

    @Test
    fun ageDesParents() {
        val l = listOf(
            n(1, "Pere", "M", "1800"), n(2, "Mere", "F", "1800"),
            n(3, "Tardif", "M", "1890", pere = 1, mere = 2),
            n(4, "Jeune", "F", "1805", pere = 1, mere = 2)
        )
        val t = types(l)
        assertTrue(t.contains("pere-trop-age"))
        assertTrue(t.contains("mere-trop-agee"))
        assertTrue(t.contains("pere-trop-jeune"))
        assertTrue(t.contains("mere-trop-jeune"))
        // 1800 -> 1850 : 49 ans a 50 ans au plus : pas de signalement.
        val ok = listOf(n(1, "M", "F", "1800"), n(2, "E", "M", "1850", mere = 1))
        assertTrue(anomaliesAVerifier(ok).isEmpty())
    }

    @Test
    fun meredecedeeAvantLaNaissance() {
        val l = listOf(
            n(1, "Mere", "F", "1800", "01/01/1830"),
            n(2, "Enfant", "M", "01/03/1830", mere = 1)
        )
        assertEquals(listOf("naissance-apres-deces-mere"), types(l))
        val mort = listOf(n(1, "Mere", "F", "1800", "01/03/1830"), n(2, "Enfant", "M", "01/03/1830", mere = 1))
        assertTrue(anomaliesAVerifier(mort).isEmpty())
    }

    @Test
    fun evenementsIncompatiblesDUneMemePersonne() {
        val l = listOf(
            n(1, "A", "M", "1800", "1790"),
            n(2, "B", "F", "1800", "1850", mar = "1860"),
            n(3, "C", "M", "1800", "1850", bap = "1790", sep = "1840"),
            n(4, "D", "M", "1800", libres = listOf("Service" to "1790")),
            n(5, "E", "M", "1800", mar = "1805")
        )
        val t = types(l)
        assertTrue(t.contains("deces-avant-naissance"))
        assertTrue(t.contains("mariage-apres-deces"))
        assertTrue(t.contains("bapteme-avant-naissance"))
        assertTrue(t.contains("sepulture-avant-deces"))
        assertTrue(t.contains("evenement-avant-naissance"))
        assertTrue(t.contains("mariage-trop-jeune"))
    }

    @Test
    fun mariageEtConjoint() {
        val l = listOf(
            n(1, "Lui", "M", "1800", mar = "1840", conj = listOf(2)),
            n(2, "Elle", "F", "1805", "1835", mar = "1840", conj = listOf(1))
        )
        val t = types(l)
        assertEquals(1, t.count { it == "mariage-apres-deces-conjoint" })
        val l2 = listOf(
            n(1, "Lui", "M", "1800", mar = "01/01/1830", conj = listOf(2)),
            n(2, "Elle", "F", "1805", mar = "02/01/1830", conj = listOf(1))
        )
        assertEquals(listOf("mariage-dates-differentes"), types(l2))
        // Plusieurs conjoints : la date de mariage est ambigue, pas de contr\u00f4le croise.
        val l3 = listOf(
            n(1, "Lui", "M", "1800", mar = "1840", conj = listOf(2, 3)),
            n(2, "Elle", "F", "1805", "1835"),
            n(3, "Autre", "F", "1810")
        )
        assertTrue(anomaliesAVerifier(l3).isEmpty())
    }

    @Test
    fun enfantAvantLeMariageDesParents() {
        val l = listOf(
            n(1, "Pere", "M", "1800", mar = "01/06/1830", conj = listOf(2)),
            n(2, "Mere", "F", "1805", mar = "01/06/1830", conj = listOf(1)),
            n(3, "Enfant", "M", "01/02/1830", pere = 1, mere = 2)
        )
        val a = anomaliesAVerifier(l)
        assertEquals(1, a.size)
        assertEquals("enfant-avant-mariage", a[0].type)
        assertTrue(a[0].texte.contains("avant le mariage de ses parents"))
    }

    @Test
    fun sexeIncompatibleAvecLesRelations() {
        val l = listOf(
            n(1, "Paul", "F"), n(2, "Anne", "M"), n(3, "Enfant1", pere = 1), n(4, "Enfant2", mere = 2),
            n(5, "Camille", "", pere = 0), n(6, "E3", pere = 5), n(7, "E4", mere = 5)
        )
        val t = types(l)
        assertTrue(t.contains("sexe-pere"))
        assertTrue(t.contains("sexe-mere"))
        assertTrue(t.contains("sexe-incompatible"))
    }

    @Test
    fun fratrieTropRapprochee() {
        val l = listOf(
            n(1, "Mere", "F", "1800"),
            n(2, "A", "M", "01/01/1830", mere = 1),
            n(3, "B", "F", "01/04/1830", mere = 1),
            n(4, "Jumeau", "M", "01/01/1830", mere = 1),
            n(5, "C", "M", "01/01/1832", mere = 1)
        )
        val a = anomaliesAVerifier(l).filter { it.type == "fratrie-intervalle" }
        // A-B et Jumeau-B seulement (A-Jumeau : jumeaux, C : 2 ans apres).
        assertEquals(2, a.size)
    }

    @Test
    fun lieuxImpossibles() {
        val l = listOf(
            n(1, "A", "M", "01/01/1800", lieux = listOf(EvLieu("naissance", "01/01/1800", "100"))),
            n(2, "B", "M", "05/05/1800", "05/05/1850", lieux = listOf(
                EvLieu("naissance", "05/05/1800", "59"), EvLieu("bapt\u00eame", "05/05/1800", "62")))
        )
        val a = anomaliesAVerifier(l) { d -> d.toIntOrNull()?.let { it in 1..95 } ?: true }
        val t = a.map { it.type }
        assertTrue(t.contains("lieu-inconnu"))
        assertTrue(t.contains("lieux-incompatibles"))
    }

    @Test
    fun anomalieAVerifierPortePersonneEtCleStable() {
        val l = listOf(n(1, "A", "M", "1800", "1790"))
        val a1 = anomaliesAVerifier(l)[0]
        val a2 = anomaliesAVerifier(l)[0]
        assertEquals(a1.cle, a2.cle)
        assertEquals(1L, a1.personneId)
        assertNotNull(a1.explication)
        val l2 = listOf(n(1, "A", "M", "1800", "1780"))
        assertFalse(a1.cle == anomaliesAVerifier(l2)[0].cle)
    }

    @Test
    fun donneesCorrompuesNeBouclentPas() {
        val l = listOf(n(1, "A", "M", "1800", pere = 2), n(2, "B", "M", "1780", pere = 1), n(3, "C", "F", "1800", mere = 3))
        anomaliesAVerifier(l)
    }

    @Test
    fun durees() {
        assertEquals("9 jours", dureeTexte(9))
        assertEquals("4 mois", dureeTexte(120))
        assertEquals("2 ans", dureeTexte(800))
        assertEquals("1 jour", dureeTexte(1))
    }
}
