package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeclarationTest {
    private fun jeu(): List<Personne> {
        val defunt = Personne(id = 1, nom = "DUPONT", prenoms = "Jean", sexe = "M", dateDeces = "10/03/1890", lieuDeces = "Bourges")
        val fils = Personne(id = 2, nom = "DUPONT", prenoms = "Pierre", sexe = "M", pere = 1, dateNaissance = "01/01/1845", profession = "cultivateur")
        val epouse = Personne(id = 3, nom = "DUPONT", prenoms = "Marie", sexe = "F", conjoint = 1, dateNaissance = "01/01/1820")
        val frere = Personne(id = 4, nom = "DUPONT", prenoms = "Paul", sexe = "M", dateNaissance = "01/01/1815")
        val voisin = Personne(id = 5, nom = "MARTIN", prenoms = "Luc", sexe = "M", dateNaissance = "01/01/1850")
        val mort = Personne(id = 6, nom = "VIEUX", prenoms = "Ancien", sexe = "M", dateDeces = "01/01/1880")
        val petit = Personne(id = 7, nom = "DUPONT", prenoms = "Bébé", sexe = "M", dateNaissance = "01/01/1888", pere = 2)
        defunt.enfants.add(2)
        defunt.conjoint = 3
        val pere = Personne(id = 8, nom = "DUPONT", prenoms = "Ancêtre", sexe = "M")
        defunt.pere = 8
        frere.pere = 8
        return listOf(defunt, fils, epouse, frere, voisin, mort, petit, pere)
    }

    @Test fun relations() {
        val l = jeu()
        val d = l[0]
        assertEquals("fils", relationDeclarant(d, l[1], l))
        assertEquals("épouse", relationDeclarant(d, l[2], l))
        assertEquals("frère", relationDeclarant(d, l[3], l))
        assertEquals("", relationDeclarant(d, l[4], l))
        assertEquals("petit-fils", relationDeclarant(d, l[6], l))
        assertEquals("père", relationDeclarant(d, l[7], l))
    }

    @Test fun possessif() {
        assertEquals("son fils", avecPossessif("fils"))
        assertEquals("sa fille", avecPossessif("fille"))
        assertEquals("", avecPossessif(""))
    }

    @Test fun candidatsClassesEtFiltres() {
        val l = jeu()
        val c = candidatsDeclarant(l[0], l, 1890)
        val ids = c.map { it.first.id }
        assertFalse(6L in ids)   // decede avant
        assertFalse(7L in ids)   // trop jeune
        assertFalse(1L in ids)   // le defunt lui-meme
        assertEquals(2L, ids.first())  // le fils d'abord
        assertTrue(5L in ids)    // un voisin peut declarer
    }

    @Test fun ageAuto() {
        val l = jeu()
        assertEquals("45", ageDeclarantAuto(l[1], 1890))
        assertEquals("", ageDeclarantAuto(l[4], null))
    }

    @Test fun phrases() {
        val l = jeu()
        val d = l[0]
        d.declarantId = 2; d.declarantAge = "45"; d.declarantRelation = "fils"
        d.declarantProfession = "cultivateur"; d.declDDate = "11/03/1890"
        val a = phraseADeclare(l[1], d)
        assertTrue(a, a.contains("45 ans"))
        assertTrue(a, a.contains("a déclaré le décès de"))
        assertTrue(a, a.contains("son fils"))
        assertTrue(a, a.contains("mairie de Bourges"))
        val b = phraseDecesDeclarePar(d, l[1])!!
        assertTrue(b, b.startsWith("Son décès a été déclaré par"))
        val r = recitBiographie(l[1], l)
        assertTrue(r.any { it.contains("a déclaré le décès de") })
        assertTrue(recitBiographie(d, l).any { it.contains("Son décès a été déclaré") })
    }

    @Test fun sansDeclarant() {
        val l = jeu()
        assertEquals(null, phraseDecesDeclarePar(l[0], null))
    }

    @Test fun jsonRoundTrip() {
        val p = Personne(id = 9, nom = "X", declarantId = 4, declarantAge = "50", declarantRelation = "gendre")
        val q = Personne.depuisJson(p.versJson())
        assertEquals(4L, q.declarantId)
        assertEquals("50", q.declarantAge)
        assertEquals("gendre", q.declarantRelation)
    }
}
