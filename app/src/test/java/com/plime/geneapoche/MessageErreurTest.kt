package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.137 : un echec non anticipe ne doit plus etre silencieux : le type et
// le message de l'exception accompagnent le libelle par defaut, chemins
// censures (aucune donnee personnelle ni arborescence affichee).
class MessageErreurTest {

    @Test
    fun exceptionConnexeGardeSonLibelleFrancaisSeul() {
        assertEquals("Erreur de lecture ou d'\u00E9criture du fichier.",
            messageErreur(java.io.IOException("boom"), "Echec."))
        assertEquals("Fichier introuvable.",
            messageErreur(java.io.FileNotFoundException("x"), "Echec."))
        assertEquals("Acc\u00E8s refus\u00E9 au fichier.",
            messageErreur(SecurityException("x"), "Echec."))
    }

    @Test
    fun exceptionInconnueAfficheTypeEtMessage() {
        val m = messageErreur(IllegalStateException("base vide sur le thread IO"), "Echec de l'export GEDCOM.")
        assertTrue(m.startsWith("Echec de l'export GEDCOM."))
        assertTrue(m.contains("IllegalStateException"))
        assertTrue(m.contains("base vide sur le thread IO"))
    }

    @Test
    fun exceptionInconnueSansMessageAfficheLeTypeSeul() {
        val m = messageErreur(NoSuchElementException(), "Echec.")
        assertTrue(m.contains("NoSuchElementException"))
        assertFalse(m.endsWith(" : "))
    }

    @Test
    fun lesCheminsSontCensures() {
        val m = messageErreur(RuntimeException("echec sur /data/user/0/com.plime.geneapoche/fichiers/x.bin"), "Echec.")
        assertFalse(m.contains("com.plime.geneapoche"))
        assertTrue(m.contains("[chemin]"))
        val u = messageErreur(RuntimeException("flux content://com.android.providers.downloads.documents/document/45 refuse"), "Echec.")
        assertFalse(u.contains("document/45"))
        assertTrue(u.contains("[chemin]"))
    }

    @Test
    fun messageTropLongEstPlafonne() {
        val ex = RuntimeException("x".repeat(400))
        val m = messageErreur(ex, "Echec.")
        assertTrue(m.length < 160)
    }
}
