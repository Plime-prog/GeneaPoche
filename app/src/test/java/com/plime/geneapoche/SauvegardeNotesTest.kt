package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.139 : le bloc Notes (onglet Recherches) fait partie des sauvegardes .gpbak.
class SauvegardeNotesTest {

    @Test
    fun les_notes_font_l_aller_retour() {
        val json = Sauvegarde.jsonSauvegarde(emptyList(), emptyList(), emptyList(), emptyList(), "Piste : registres de Bourges\n\nA relire")
        val c = Sauvegarde.contenuDepuisJson(json)
        assertNotNull(c)
        assertEquals("Piste : registres de Bourges\n\nA relire", c!!.notes)
    }

    @Test
    fun ancienne_sauvegarde_sans_notes_donne_null() {
        val c = Sauvegarde.contenuDepuisJson("{\"personnes\":[],\"journal\":[]}")
        assertNotNull(c)
        assertNull(c!!.notes)
    }

    @Test
    fun notes_vides_sont_ecrites_en_chaine_vide() {
        val c = Sauvegarde.contenuDepuisJson(Sauvegarde.jsonSauvegarde(emptyList(), emptyList(), emptyList(), emptyList(), ""))
        assertEquals("", c!!.notes)
    }

    @Test
    fun fusion_ne_perd_jamais_de_texte() {
        assertEquals("a", Sauvegarde.fusionnerNotes("a", null))
        assertEquals("a", Sauvegarde.fusionnerNotes("a", "  "))
        assertEquals("b", Sauvegarde.fusionnerNotes("", "b"))
        assertEquals("a", Sauvegarde.fusionnerNotes("a", " a "))
        val m = Sauvegarde.fusionnerNotes("actuelle", "ancienne")
        assertTrue(m, m.startsWith("actuelle") && m.endsWith("ancienne") && m.contains("Notes de la sauvegarde"))
    }
}
