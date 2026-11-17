package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// L'export ne doit avoir AUCUNE limite de taille : bases de 49 a 3000
// personnes, arbres profonds et larges, tous formats. Donnees fictives.
class ExportGrandeBaseTest {

    // Famille en chaine : chaque personne est le parent de la suivante (arbre PROFOND).
    private fun chaine(n: Int): List<Personne> {
        val l = (1..n).map { i ->
            Personne(
                id = i.toLong(), nom = "Nom" + (i % 17), prenoms = "Prenom" + i,
                sexe = if (i % 2 == 0) "F" else "M",
                dateNaissance = (1500 + i % 400).toString(), lieuNaissance = "Lieu" + (i % 31),
                notesLibres = "Note de la personne " + i + "\nsur deux lignes avec @ arobase"
            )
        }
        for (i in 0 until n - 1) {
            l[i].enfants.add(l[i + 1].id)
            if (l[i].sexe == "M") l[i + 1].pere = l[i].id else l[i + 1].mere = l[i].id
        }
        return l
    }

    // Une grande fratrie : 1 couple, n-2 enfants (arbre LARGE).
    private fun fratrie(n: Int): List<Personne> {
        val p = Personne(id = 1L, nom = "Pere", prenoms = "Un", sexe = "M", conjoint = 2L)
        val m = Personne(id = 2L, nom = "Mere", prenoms = "Deux", sexe = "F", conjoint = 1L)
        val l = mutableListOf(p, m)
        for (i in 3..n) {
            val e = Personne(id = i.toLong(), nom = "Pere", prenoms = "Enfant" + i, pere = 1L, mere = 2L)
            p.enfants.add(e.id); m.enfants.add(e.id)
            l.add(e)
        }
        return l
    }

    private fun compter(ged: String) = ged.lines().count { it.endsWith(" INDI") }

    @Test
    fun aucune_limite_autour_de_50_personnes() {
        for (n in listOf(1, 49, 50, 51, 52, 60, 99, 100, 101)) {
            val ged = construireGedcom(chaine(n))
            assertEquals("chaine " + n, n, compter(ged))
            assertTrue(ged.length > 100 * n.coerceAtMost(20))
            assertEquals("fratrie " + n, maxOf(n, 2), compter(construireGedcom(fratrie(maxOf(n, 2)))))
        }
    }

    @Test
    fun grande_base_profonde_et_large_s_exporte_en_entier() {
        val profonde = chaine(3000)
        val gp = construireGedcom(profonde)
        assertEquals(3000, compter(gp))
        assertTrue(gp.trimEnd().endsWith("0 TRLR"))
        val large = construireGedcom(fratrie(3000))
        assertEquals(3000, compter(large))
        assertTrue(large.trimEnd().endsWith("0 TRLR"))
    }

    @Test
    fun aller_retour_de_150_personnes_les_retrouve_toutes() {
        val l = importerGedcomRapporte(construireGedcom(chaine(150))).personnes
        assertEquals(150, l.size)
    }

    @Test
    fun export_chiffre_d_une_grande_base_est_complet_et_relisible() {
        val clair = construireGedcom(chaine(500)).toByteArray(Charsets.UTF_8)
        val chiffre = Sauvegarde.chiffrerOctets("phrase-secrete-fictive", clair, Sauvegarde.MAGIQUE_GPGED)!!
        assertTrue(chiffre.size > clair.size)
        val relu = Sauvegarde.dechiffrerOctets("phrase-secrete-fictive", chiffre, Sauvegarde.MAGIQUE_GPGED)!!
        assertEquals(clair.size, relu.size)
    }

    @Test
    fun exports_partiels_et_formats_d_une_base_de_120_personnes() {
        val l = chaine(120)
        val sous = (1L..60L).toSet()
        assertEquals(60, compter(construireGedcom(l, sousEnsemble = sous)))
        for (format in listOf("5.5.1", "5.5", "7.0")) {
            assertEquals(format, 120, compter(construireGedcom(l, null, format)))
        }
    }
}
