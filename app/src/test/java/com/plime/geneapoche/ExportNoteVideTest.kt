package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.137 : reproduction de l'echec d'export « Echec de l'export GEDCOM »
// (fichier de 0 Ko) signale avec une base reelle : une note multi-lignes
// contenant une ligne VIDE faisait lever NoSuchElementException dans
// ajouterValeur ("a\n\nb" -> deuxieme CONT avec valeur vide ->
// chunked() sur chaine vide -> first() sur liste vide).
// Aucun test n'avait couvert la ligne vide, d'ou le « non reproduit »
// des v0.132/v0.136.
class ExportNoteVideTest {

    private fun personne(note: String) = Personne(
        id = 10L, sosa = "1", nom = "Dupont", prenoms = "Jean", sexe = "M",
        dateNaissance = "27/10/1890",
        dateDeces = "14/03/1961",
        notesLibres = note
    )

    @Test
    fun exporte_une_note_avec_ligne_vide_sans_echec() {
        val p = personne("Note avant.\n\nNote apres la ligne vide.")
        val ged = construireGedcom(listOf(p))
        assertTrue(ged.contains("1 NOTE"))
        // Le retour du GEDCOM ne leve pas non plus d'exception a l'import.
        val l = importerGedcomRapporte(ged).personnes
        assertEquals(p.notesLibres, l.first().notesLibres)
    }

    @Test
    fun exporte_une_note_avec_plusieurs_lignes_vides_consecutives() {
        val p = personne("A\n\n\nB\n\nC")
        val ged = construireGedcom(listOf(p))
        val l = importerGedcomRapporte(ged).personnes
        assertEquals(1, l.size)
        assertEquals(p.notesLibres, l.first().notesLibres)
    }

    @Test
    fun ligne_vide_dans_une_source_et_une_citation_ne_bloque_pas_lexport() {
        val s = Source(1, titre = "Registre d'etat civil")
        s.transcription = "Transcription ligne 1.\n\nTranscription apres ligne vide."
        s.notes = "Notes ligne 1.\n\nNotes apres ligne vide."
        val c = Citation(1, 1L, 10L, "P")
        c.extrait = "Extrait ligne 1.\n\nExtrait apres ligne vide."
        c.notes = "Citation ligne 1.\n\nCitation apres ligne vide."
        val ged = construireGedcom(listOf(personne("Note.\n\nSuite.")), sources = listOf(s), citations = listOf(c))
        assertTrue(ged.contains("@S1@"))
        assertTrue(ged.contains("1 NOTE"))
    }
}
