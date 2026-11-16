package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Mariages multiples et evenements de famille (divorce...) dans un GEDCOM.
// Donnees entierement fictives.
class FamillesGedcomTest {

    private fun ged(vararg l: String) = l.joinToString("\r\n") + "\r\n"

    private fun par(l: List<Personne>, prenoms: String) = l.first { it.prenoms == prenoms }

    @Test
    fun divorce_de_la_famille_est_donne_aux_deux_conjoints() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Henri /MARTIN/", "1 SEX M", "1 FAMS @F1@",
            "0 @I2@ INDI", "1 NAME Anne /ROUX/", "1 SEX F", "1 FAMS @F1@",
            "0 @F1@ FAM", "1 HUSB @I1@", "1 WIFE @I2@",
            "1 MARR", "2 DATE 12 JUN 1920",
            "1 DIV", "2 DATE 3 MAR 1930", "2 PLAC Lille, Nord, France"
        ))
        val h = par(r.personnes, "Henri")
        val a = par(r.personnes, "Anne")
        assertEquals("Divorce (avec Anne ROUX)", h.evenementsLibres.single().intitule)
        assertEquals("03/03/1930", h.evenementsLibres.single().date)
        assertEquals("Lille", h.evenementsLibres.single().lieu)
        assertEquals("Divorce (avec Henri MARTIN)", a.evenementsLibres.single().intitule)
        assertEquals("12/06/1920", h.dateMariage)
        assertEquals("12/06/1920", a.dateMariage)
        assertTrue(r.ignores.none { it.contains("DIV") })
    }

    @Test
    fun remariage_le_second_mariage_n_est_plus_perdu() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Henri /MARTIN/", "1 SEX M", "1 FAMS @F1@", "1 FAMS @F2@",
            "0 @I2@ INDI", "1 NAME Anne /ROUX/", "1 SEX F", "1 FAMS @F1@",
            "0 @I3@ INDI", "1 NAME Claire /PETIT/", "1 SEX F", "1 FAMS @F2@",
            "0 @F1@ FAM", "1 HUSB @I1@", "1 WIFE @I2@", "1 MARR", "2 DATE 1912",
            "0 @F2@ FAM", "1 HUSB @I1@", "1 WIFE @I3@", "1 MARR", "2 DATE 1925", "2 PLAC Arras"
        ))
        val h = par(r.personnes, "Henri")
        val anne = par(r.personnes, "Anne")
        val claire = par(r.personnes, "Claire")
        assertEquals("1912", h.dateMariage)
        assertEquals("1912", anne.dateMariage)
        assertEquals("1925", claire.dateMariage)
        assertEquals("Arras", claire.lieuMariage)
        val ev = h.evenementsLibres.single()
        assertEquals("Mariage (avec Claire PETIT)", ev.intitule)
        assertEquals("1925", ev.date)
        assertEquals("Arras", ev.lieu)
        assertTrue(anne.evenementsLibres.isEmpty())
        assertTrue(claire.evenementsLibres.isEmpty())
    }

    @Test
    fun mariage_unique_donne_par_indi_et_par_fam_n_est_pas_duplique() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Henri /MARTIN/", "1 SEX M", "1 FAMS @F1@",
            "1 MARR", "2 DATE 12 JUN 1920",
            "0 @I2@ INDI", "1 NAME Anne /ROUX/", "1 SEX F", "1 FAMS @F1@",
            "1 MARR", "2 DATE 12 JUN 1920",
            "0 @F1@ FAM", "1 HUSB @I1@", "1 WIFE @I2@", "1 MARR", "2 DATE 12 JUN 1920"
        ))
        assertTrue(r.personnes.all { it.evenementsLibres.isEmpty() })
        assertTrue(r.personnes.all { it.dateMariage == "12/06/1920" })
    }

    @Test
    fun aller_retour_de_deux_mariages_garde_chaque_date_a_sa_famille() {
        val a = Personne(id = 1L, nom = "Martin", prenoms = "Henri", sexe = "M",
            dateMariage = "1912", conjoint = 2L, conjointsSupp = mutableListOf(3L))
        val b = Personne(id = 2L, nom = "Roux", prenoms = "Anne", sexe = "F", conjoint = 1L)
        val c = Personne(id = 3L, nom = "Petit", prenoms = "Claire", sexe = "F",
            dateMariage = "1925", conjoint = 1L)
        val ged = construireGedcom(listOf(a, b, c))
        val l = importerGedcomRapporte(ged).personnes
        val h = par(l, "Henri")
        assertEquals("1912", h.dateMariage)
        assertEquals("1912", par(l, "Anne").dateMariage)
        assertEquals("1925", par(l, "Claire").dateMariage)
        // le mariage 1925 reste visible chez Henri, une seule fois
        assertEquals(listOf("Mariage (avec Claire Petit)"), h.evenementsLibres.map { it.intitule }.map { it.replace("PETIT", "Petit") })
        assertTrue(par(l, "Anne").evenementsLibres.isEmpty())
        assertTrue(par(l, "Claire").evenementsLibres.isEmpty())
    }

    @Test
    fun export_n_ecrit_pas_la_date_du_premier_mariage_dans_la_seconde_famille() {
        val a = Personne(id = 1L, nom = "Martin", prenoms = "Henri", sexe = "M",
            dateMariage = "1912", conjoint = 2L, conjointsSupp = mutableListOf(3L))
        val b = Personne(id = 2L, nom = "Roux", prenoms = "Anne", sexe = "F", conjoint = 1L)
        val c = Personne(id = 3L, nom = "Petit", prenoms = "Claire", sexe = "F", conjoint = 1L)
        val ged = construireGedcom(listOf(a, b, c))
        val f2 = ged.split(Regex("(?m)^0 ")).first { it.startsWith("@F1_3@") }
        assertTrue(!f2.contains("1912"))
    }
}
