package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Random

// v0.109 : les nouvelles fonctions (cles pre-calculees, detection O(n)) doivent
// donner EXACTEMENT les memes resultats que l'ancien code. Donnees fictives.
class ListesPersonnesTest {

    private val noms = listOf("Dupont", "DUPONT", " martin", "Bernard", "Émile", "Durand", "Petit", "")
    private val prenoms = listOf("Paul", "paul", "Marie", "Jean Pierre", " Anne", "")
    private val dates = listOf("", "12 mars 1850", "1901", "22 Germinal An II", "vers 1879", "3 juin 1720")
    private val numeros = listOf("", "1", "2", "3", "10", "2.1", "2.10", "2.3", "7.2", "x", "1.2.3")

    private fun fictives(n: Int, graine: Long): List<Personne> {
        val r = Random(graine)
        val liste = (1..n).map { i ->
            Personne(
                id = i.toLong(),
                nom = noms[r.nextInt(noms.size)],
                prenoms = prenoms[r.nextInt(prenoms.size)],
                dateNaissance = dates[r.nextInt(dates.size)],
                sosa = if (r.nextBoolean()) numeros[r.nextInt(numeros.size)] else "",
                lateral = numeros[r.nextInt(numeros.size)]
            )
        }
        // liens aleatoires, certains vides, un auto-lien occasionnel
        liste.forEach { p ->
            if (r.nextInt(3) == 0) p.pere = (1 + r.nextInt(n)).toLong()
            if (r.nextInt(3) == 0) p.mere = (1 + r.nextInt(n)).toLong()
            if (r.nextInt(6) == 0) p.conjoint = (1 + r.nextInt(n)).toLong()
            if (r.nextInt(10) == 0) p.conjointsSupp.add((1 + r.nextInt(n)).toLong())
            if (r.nextInt(5) == 0) p.enfants.add((1 + r.nextInt(n)).toLong())
        }
        return liste
    }

    // Ancien code (v0.108), reproduit tel quel comme reference.
    private fun ancienParNom(l: List<Personne>) = l.sortedWith(
        compareBy(
            { it.nom.trim().uppercase() },
            { it.prenoms.trim().uppercase() },
            { anneeNaissance(it) }
        )
    )

    private fun ancienParAboville(l: List<Personne>) = l.sortedWith(
        ComparateurAboville
            .thenComparator { a, b -> a.nom.trim().uppercase().compareTo(b.nom.trim().uppercase()) }
            .thenComparator { a, b -> a.prenoms.trim().uppercase().compareTo(b.prenoms.trim().uppercase()) }
    )

    @Test
    fun trierParNom_donne_le_meme_ordre_que_l_ancien_code() {
        for (graine in 1L..30L) {
            val l = fictives(60, graine)
            assertEquals(ancienParNom(l).map { it.id }, trierParNom(l).map { it.id })
        }
    }

    @Test
    fun trierParAboville_donne_le_meme_ordre_que_l_ancien_code() {
        for (graine in 1L..30L) {
            val l = fictives(60, graine)
            assertEquals(ancienParAboville(l).map { it.id }, trierParAboville(l).map { it.id })
        }
    }

    @Test
    fun personnesIsolees_donne_le_meme_resultat_que_brancheIsolee() {
        for (graine in 1L..60L) {
            val l = fictives(40, graine)
            IndexPersonnes.invalider()
            val attendu = l.filter { brancheIsolee(it, l) }.map { it.id }
            assertEquals(attendu, personnesIsolees(l).map { it.id })
        }
    }

    @Test
    fun personnesIsolees_ignore_l_auto_citation_et_repere_les_citees() {
        val seul = Personne(id = 1L)
        val cite = Personne(id = 2L)
        val parent = Personne(id = 3L, enfants = mutableListOf(2L))
        val soiMeme = Personne(id = 4L, enfants = mutableListOf(4L))
        val l = listOf(seul, cite, parent, soiMeme)
        // 1 : isole ; 2 : cite comme enfant de 3 ; 3 : a un enfant ;
        // 4 : un enfant (lui-meme) -> a un lien propre, donc non isole.
        assertEquals(listOf(1L), personnesIsolees(l).map { it.id })
    }

    @Test
    fun tris_sur_dix_mille_personnes_restent_rapides() {
        val l = fictives(10_000, 7L)
        val debut = System.nanoTime()
        trierParNom(l)
        trierParAboville(l)
        personnesIsolees(l)
        val ms = (System.nanoTime() - debut) / 1_000_000
        assertTrue("trop lent : $ms ms", ms < 3000)
    }

    @Test
    fun recherche_insensible_aux_accents_et_a_la_casse() {
        assertTrue(contientSansAccents("Lef\u00E8vre Fran\u00E7ois \u00C9tampes", "lefevre"))
        assertTrue(contientSansAccents("Lef\u00E8vre Fran\u00E7ois \u00C9tampes", "FRANCOIS"))
        assertTrue(contientSansAccents("Lefevre Francois Etampes", "\u00E9tampes"))
        assertTrue(contientSansAccents("Durand", ""))
        assertFalse(contientSansAccents("Durand", "martin"))
    }
}
