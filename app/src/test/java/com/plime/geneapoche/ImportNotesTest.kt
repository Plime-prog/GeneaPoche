package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Notes d'un GEDCOM externe : en ligne, partagees (pointeur @N1@), multiples,
// au niveau d'un evenement. Fichiers fictifs.
class ImportNotesTest {

    private fun ged(vararg l: String) = l.joinToString("\r\n") + "\r\n"

    @Test
    fun note_en_ligne_avec_suite_et_continuation() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/",
            "1 NOTE Premiere partie", "2 CONC  suite", "2 CONT Deuxieme ligne"
        ))
        assertEquals("Premiere partie suite\nDeuxieme ligne", r.personnes.single().notesLibres)
    }

    @Test
    fun note_partagee_par_pointeur_est_lue() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/", "1 NOTE @N1@",
            "0 @N1@ NOTE Texte de la note partagee", "1 CONT Seconde ligne",
            "0 TRLR"
        ))
        val n = r.personnes.single().notesLibres
        assertTrue("note = " + n, n.contains("Texte de la note partagee"))
        assertTrue("note = " + n, n.contains("Seconde ligne"))
        assertTrue("pas de pointeur brut : " + n, !n.contains("@N1@"))
    }

    @Test
    fun plusieurs_notes_sont_toutes_conservees() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/",
            "1 NOTE Premiere note", "1 NOTE Seconde note"
        ))
        val n = r.personnes.single().notesLibres
        assertTrue("note = " + n, n.contains("Premiere note"))
        assertTrue("note = " + n, n.contains("Seconde note"))
    }

    @Test
    fun note_d_un_evenement_n_est_pas_perdue() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/",
            "1 BIRT", "2 DATE 5 MAR 1880", "2 NOTE Ne a la ferme du Moulin"
        ))
        val p = r.personnes.single()
        assertEquals("05/03/1880", p.dateNaissance)
        assertTrue("note = " + p.notesLibres, p.notesLibres.contains("Ne a la ferme du Moulin"))
    }

    @Test
    fun notes_dans_un_fichier_sans_note_restent_vides() {
        val r = importerGedcomRapporte(ged("0 @I1@ INDI", "1 NAME Jean /DUPONT/"))
        assertEquals("", r.personnes.single().notesLibres)
    }
}
