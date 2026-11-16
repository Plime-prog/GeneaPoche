package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Aller-retour GEDCOM complet (export puis import) sur une famille fictive :
// ce qui sort de l'application doit revenir identique. Donnees entierement
// fictives.
class AllerRetourGedcomTest {

    private val noteLongue = "Ligne un avec @ arobase et éèç accents.\n" +
        "Deuxième ligne après un saut. " + "mot ".repeat(120).trim() + "\nFin."

    private fun famille(): List<Personne> {
        val jean = Personne(
            id = 10L, sosa = "1", nom = "Dupont", prenoms = "Jean Pierre", sexe = "M",
            profession = "tisserand",
            dateNaissance = "27/10/1890", lieuNaissance = "Roubaix", deptNaissance = "59",
            dateBapteme = "28/10/1890", lieuBapteme = "Roubaix", deptBapteme = "59",
            dateMariage = "02/06/1912", lieuMariage = "Lille", deptMariage = "59",
            dateDeces = "14/03/1961", lieuDeces = "Tourcoing", deptDeces = "59",
            notesLibres = noteLongue,
            pere = 11L, mere = 12L, conjoint = 13L
        )
        val pere = Personne(id = 11L, nom = "Dupont", prenoms = "Louis", sexe = "M", enfants = mutableListOf(10L, 14L))
        val mere = Personne(id = 12L, nom = "Verbeke", prenoms = "Rosalie", sexe = "F", enfants = mutableListOf(10L, 14L))
        val epouse = Personne(
            id = 13L, nom = "Leclerc", prenoms = "Thérèse", sexe = "F",
            dateNaissance = "1893", conjoint = 10L, enfants = mutableListOf(15L)
        )
        val frere = Personne(id = 14L, nom = "Dupont", prenoms = "Alfred", sexe = "M", pere = 11L, mere = 12L)
        val fils = Personne(id = 15L, nom = "Dupont", prenoms = "Marcel", sexe = "M", pere = 10L, mere = 13L)
        jean.enfants.add(15L)
        return listOf(jean, pere, mere, epouse, frere, fils)
    }

    private fun aller(): List<Personne> = importerGedcomRapporte(construireGedcom(famille())).personnes

    private fun par(l: List<Personne>, prenoms: String) = l.first { it.prenoms == prenoms }

    @Test
    fun effectif_et_champs_simples_reviennent() {
        val l = aller()
        assertEquals(6, l.size)
        val j = par(l, "Jean Pierre")
        assertEquals("DUPONT", j.nom)
        assertEquals("M", j.sexe)
        assertEquals("tisserand", j.profession)
        assertEquals("27/10/1890", j.dateNaissance)
        assertEquals("Roubaix", j.lieuNaissance)
        assertEquals("28/10/1890", j.dateBapteme)
        assertEquals("14/03/1961", j.dateDeces)
        assertEquals("Tourcoing", j.lieuDeces)
        assertEquals("59", j.deptDeces)
        assertEquals("02/06/1912", j.dateMariage)
        assertEquals("Lille", j.lieuMariage)
    }

    @Test
    fun accents_et_date_partielle_reviennent() {
        val e = par(aller(), "Thérèse")
        assertEquals("LECLERC", e.nom)
        assertEquals("1893", e.dateNaissance)
    }

    @Test
    fun note_longue_multiligne_avec_arobase_revient_identique() {
        assertEquals(noteLongue, par(aller(), "Jean Pierre").notesLibres)
    }

    @Test
    fun aucune_ligne_gedcom_ne_depasse_255_caracteres() {
        val ged = construireGedcom(famille())
        val trop = ged.lines().filter { it.length > 255 }
        assertTrue("lignes trop longues : " + trop.size, trop.isEmpty())
    }

    @Test
    fun liens_parents_conjoint_enfants_sont_recrees_de_facon_coherente() {
        val l = aller()
        val j = par(l, "Jean Pierre")
        val louis = par(l, "Louis")
        val rosalie = par(l, "Rosalie")
        val e = par(l, "Thérèse")
        val alfred = par(l, "Alfred")
        val marcel = par(l, "Marcel")
        assertEquals(louis.id, j.pere)
        assertEquals(rosalie.id, j.mere)
        assertEquals(e.id, j.conjoint)
        assertEquals(j.id, e.conjoint)
        assertEquals(louis.id, alfred.pere)
        assertEquals(j.id, marcel.pere)
        assertEquals(e.id, marcel.mere)
        assertTrue(louis.enfants.containsAll(listOf(j.id, alfred.id)))
        assertTrue(j.enfants.contains(marcel.id))
        assertTrue(e.enfants.contains(marcel.id))
    }

    @Test
    fun aucun_lien_ne_designe_une_personne_absente_ni_elle_meme() {
        val l = aller()
        val ids = l.map { it.id }.toSet()
        for (p in l) {
            val liens = listOf(p.pere, p.mere, p.conjoint) + p.enfants + p.conjointsSupp
            for (x in liens) {
                if (x == 0L) continue
                assertTrue("lien orphelin de " + p.prenoms, ids.contains(x))
                assertFalse("auto-lien de " + p.prenoms, x == p.id)
            }
        }
    }

    @Test
    fun deuxieme_aller_retour_est_stable() {
        val un = construireGedcom(aller())
        val deux = construireGedcom(importerGedcomRapporte(un).personnes)
        assertEquals(
            importerGedcomRapporte(un).personnes.map { it.prenoms + "|" + it.dateNaissance + "|" + it.notesLibres }.sorted(),
            importerGedcomRapporte(deux).personnes.map { it.prenoms + "|" + it.dateNaissance + "|" + it.notesLibres }.sorted()
        )
    }

    @Test
    fun export_partiel_n_emporte_que_le_sous_ensemble_sans_lien_orphelin() {
        val ged = construireGedcom(famille(), sousEnsemble = setOf(10L, 13L, 15L))
        val l = importerGedcomRapporte(ged).personnes
        assertEquals(3, l.size)
        val ids = l.map { it.id }.toSet()
        for (p in l) for (x in listOf(p.pere, p.mere, p.conjoint) + p.enfants) {
            if (x != 0L) assertTrue(ids.contains(x))
        }
    }
}
