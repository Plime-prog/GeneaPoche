package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.137 : une note contenant des lignes vides (notes multiples recollees
// avec "\n\n" a l'import, ou simple saut de ligne dans le bloc sans titre)
// faisait echouer TOUT l'export GEDCOM (NoSuchElementException dans
// ajouterValeur sur une valeur vide) : message "Echec de l'export GEDCOM."
// et fichier de 0 Ko. Donnees entierement fictives.
class ExportNoteVideTest {

    @Test
    fun note_avec_lignes_vides_n_bloque_plus_l_export() {
        val p = Personne(
            id = 1L, nom = "Test", prenoms = "Note", sexe = "F",
            notesLibres = "Premiere note.\n\nDeuxieme note.\n\n\nTroisieme note."
        )
        val ged = construireGedcom(listOf(p))
        assertTrue(ged.contains("1 NOTE Premiere note."))
        assertTrue(ged.contains("2 CONT Deuxieme note."))
        assertTrue(ged.contains("2 CONT Troisieme note."))
        assertEquals(1, Regex("^0 @I1@ INDI$", RegexOption.MULTILINE).findAll(ged).count())
    }

    @Test
    fun aller_retour_avec_notes_multiples_separees_par_ligne_vide() {
        val p = Personne(
            id = 1L, nom = "Test", prenoms = "Note", sexe = "F",
            notesLibres = "Premiere note.\n\nDeuxieme note."
        )
        val l = importerGedcomRapporte(construireGedcom(listOf(p))).personnes
        assertEquals(1, l.size)
        assertTrue(l[0].notesLibres.contains("Premiere note."))
        assertTrue(l[0].notesLibres.contains("Deuxieme note."))
    }

    @Test
    fun note_de_synthese_avec_plusieurs_champs_n_bloque_pas() {
        val p = Personne(
            id = 1L, nom = "Test", prenoms = "Synthese", sexe = "M",
            situation = "Veuf", domicile = "Rue fictive 1",
            declDDate = "01/02/1900", declarantNom = "Fils"
        )
        val ged = construireGedcom(listOf(p))
        assertTrue(ged.contains("1 NOTE G"))
    }
}
