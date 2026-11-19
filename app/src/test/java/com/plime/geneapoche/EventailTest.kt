package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventailTest {
    private val gp = Personne(id = 1, nom = "A", prenoms = "a", sexe = "M")
    private val pere = Personne(id = 2, nom = "B", prenoms = "b", sexe = "M", pere = 1)
    private val mere = Personne(id = 3, nom = "C", prenoms = "c", sexe = "F")
    private val moi = Personne(id = 4, nom = "D", prenoms = "d", sexe = "M", pere = 2, mere = 3)
    private val tous = listOf(gp, pere, mere, moi)

    @Test
    fun anneaux_2_4_8_dans_l_ordre_pere_mere() {
        val a = ancetresEventail(moi, tous)
        assertEquals(listOf(2, 4, 8), a.map { it.size })
        assertEquals(2L, a[0][0]?.id)
        assertEquals(3L, a[0][1]?.id)
        assertEquals(1L, a[1][0]?.id)
        assertNull(a[1][1])
        assertNull(a[2][0])
    }

    @Test
    fun secteur_touche() {
        // r0 = 20, epaisseur = 40 : anneau 1 = 20..60, 2 = 60..100, 3 = 100..140
        assertEquals(Pair(0, 0), secteurEventail(0f, -5f, 20f, 40f))
        assertEquals(Pair(1, 0), secteurEventail(-30f, -10f, 20f, 40f))   // gauche = pere
        assertEquals(Pair(1, 1), secteurEventail(30f, -10f, 20f, 40f))    // droite = mere
        assertEquals(Pair(2, 3), secteurEventail(80f, -10f, 20f, 40f))
        assertEquals(Pair(3, 0), secteurEventail(-120f, -5f, 20f, 40f))
        assertNull(secteurEventail(0f, 10f, 20f, 40f))     // moitie basse
        assertNull(secteurEventail(0f, -200f, 20f, 40f))   // hors eventail
    }

    @Test
    fun annees_courtes() {
        assertEquals("1850\u20131920", anneesCourtes(Personne(id = 9, dateNaissance = "01/02/1850", dateDeces = "03/04/1920")))
        assertEquals("", anneesCourtes(Personne(id = 10)))
    }
}
