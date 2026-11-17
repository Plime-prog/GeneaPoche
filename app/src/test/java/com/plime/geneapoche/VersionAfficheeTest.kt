package com.plime.geneapoche

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Garde-fou : aucun numero de version ecrit en dur dans l'interface (il
// restait affiche "0.107" quelle que soit la version installee).
class VersionAfficheeTest {

    @Test
    fun aucun_numero_de_version_ecrit_en_dur_dans_un_texte_affiche() {
        val dossier = File("src/main/java")
        assertTrue("dossier des sources introuvable : " + dossier.absolutePath, dossier.isDirectory)
        val motif = Regex("\"[^\"\\n]*Version\\s+0\\.\\d+")
        val fautifs = dossier.walkTopDown().filter { it.extension == "kt" }
            .filter { f -> f.readLines().any { l -> motif.containsMatchIn(l) && !l.trimStart().startsWith("//") } }
            .map { it.name }.toList()
        assertTrue("version en dur dans : " + fautifs, fautifs.isEmpty())
    }

    @Test
    fun l_ecran_de_menu_et_l_ecran_pin_lisent_la_vraie_version() {
        val menu = File("src/main/java/com/plime/geneapoche/EcranApplication.kt").readText()
        val pin = File("src/main/java/com/plime/geneapoche/EcranPin.kt").readText()
        assertTrue(menu.contains("versionApplication("))
        assertTrue(pin.contains("versionApplication("))
    }
}
