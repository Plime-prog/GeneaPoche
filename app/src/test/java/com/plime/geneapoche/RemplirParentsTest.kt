package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.65 : tests du correctif des champs meles (diagnostic externe).
// remplir un parent ne touche QUE ce parent ; prefillActe est fidele
// a la fiche (fonction pure).
class RemplirParentsTest {

    @Test
    fun remplirUnParentNeTouchePasLesAutres() {
        val a = ChampsParent()
        val b = ChampsParent()
        a.remplir("DUPONT", "Jean", "01/01/1850", "Roubaix", "59", "tisserand", "02/02/1920", "Lille")
        assertEquals("DUPONT", a.nom)
        assertEquals("Jean", a.prenoms)
        assertEquals("01/01/1850", a.ne)
        assertEquals("Roubaix", a.lieu)
        assertEquals("59", a.dept)
        assertEquals("tisserand", a.profession)
        assertEquals("02/02/1920", a.deces)
        assertEquals("Lille", a.lieuDeces)
        // Le second parent reste VIDE : aucun champ n'a fui d'un bloc a l'autre.
        assertEquals("", b.nom)
        assertEquals("", b.prenoms)
        assertEquals("", b.ne)
        assertEquals("", b.lieu)
        assertEquals("", b.dept)
        assertEquals("", b.profession)
        assertEquals("", b.deces)
        assertEquals("", b.lieuDeces)
    }

    @Test
    fun remplirEcraseToutesLesValeursPrecedentes() {
        val c = ChampsParent()
        c.remplir("DUPONT", "Jean", "01/01/1850", "Roubaix", "59", "tisserand", "02/02/1920", "Lille")
        c.remplir("MARTIN", "Marie", "", "Tourcoing", "59", "", "", "")
        assertEquals("MARTIN", c.nom)
        assertEquals("Marie", c.prenoms)
        assertEquals("", c.ne)
        assertEquals("Tourcoing", c.lieu)
        assertEquals("", c.deces)
    }

    @Test
    fun prefillActeFideleALaFiche() {
        val pere = Personne(id = 11, nom = "DUPONT", prenoms = "Pierre", sexe = "M")
        val mere = Personne(id = 12, nom = "MARTIN", prenoms = "Marie", sexe = "F", deptNaissance = "59")
        val belleMere = Personne(id = 13, nom = "DURAND", prenoms = "Brigitte", sexe = "F")
        val epouse = Personne(id = 14, nom = "LEFEBVRE", prenoms = "Elise", sexe = "F", mere = 13)
        val homme = Personne(
            id = 10, nom = "HOMME", prenoms = "Henri", sexe = "M",
            pere = 11, mere = 12, conjoint = 14
        )
        val tous = listOf(homme, pere, mere, belleMere, epouse)
        val a = prefillActe(homme, tous, "N")
        // La personne concernee reste ELLE-MEME.
        assertEquals("HOMME", a.nom)
        assertEquals("Henri", a.prenoms)
        // Les parents de la personne sont ceux de SA fiche.
        assertEquals("DUPONT", a.pereNom)
        assertEquals("Pierre", a.perePrenoms)
        assertEquals("MARTIN", a.mereNom)
        assertEquals("Marie", a.merePrenoms)
        assertEquals("59", a.mereDept)
        assertEquals(11L, a.pereId)
        assertEquals(12L, a.mereId)
        assertEquals("DUPONT", a.pereNomInit)
        assertEquals("MARTIN", a.mereNomInit)
        // La mere du CONJOINT reste dans SON bloc, jamais dans celui de la mere.
        assertEquals("DURAND", a.mMarieeNom)
    }
}
