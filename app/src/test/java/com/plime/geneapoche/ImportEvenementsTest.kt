package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Evenements datables d'un GEDCOM externe (residence, recensement...) :
// conserves comme evenements libres. Fichiers fictifs.
class ImportEvenementsTest {

    private fun ged(vararg l: String) = l.joinToString("\r\n") + "\r\n"

    @Test
    fun residence_recensement_emigration_deviennent_des_evenements_libres() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/", "1 SEX M",
            "1 RESI", "2 DATE 1 JAN 1901", "2 PLAC Lille, 59, Nord, France",
            "1 CENS", "2 DATE 1906", "2 PLAC Roubaix",
            "1 EMIG", "2 DATE ABT 1912", "2 PLAC Anvers, Belgique",
            "0 TRLR"
        ))
        val ev = r.personnes.single().evenementsLibres
        assertEquals(listOf("Résidence", "Recensement", "Émigration"), ev.map { it.intitule })
        assertEquals("01/01/1901", ev[0].date)
        assertEquals("Lille", ev[0].lieu)
        assertEquals("59", ev[0].dept)
        assertEquals("1906", ev[1].date)
        assertEquals("vers 1912", ev[2].date)
        assertEquals("Anvers, Belgique", ev[2].lieu)
    }

    @Test
    fun type_precise_le_libelle_d_un_evenement_connu() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/",
            "1 RESI", "2 TYPE Caserne", "2 DATE 1910"
        ))
        assertEquals("Résidence (Caserne)", r.personnes.single().evenementsLibres.single().intitule)
    }

    @Test
    fun evenement_generique_even_type_reste_pris_en_charge() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/",
            "1 EVEN", "2 TYPE Service militaire", "2 DATE 1912"
        ))
        val e = r.personnes.single().evenementsLibres.single()
        assertEquals("Service militaire", e.intitule)
        assertEquals("1912", e.date)
    }

    @Test
    fun evenements_connus_ne_sont_plus_signales_comme_ignores() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/", "1 RESI", "2 DATE 1901"
        ))
        assertTrue(r.ignores.none { it.contains("RESI") })
    }

    @Test
    fun evenements_et_evenements_naissance_ne_se_melangent_pas() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Jean /DUPONT/",
            "1 BIRT", "2 DATE 5 MAR 1880", "2 PLAC Paris",
            "1 RESI", "2 DATE 1901", "2 PLAC Lille",
            "1 DEAT", "2 DATE 1950"
        ))
        val p = r.personnes.single()
        assertEquals("05/03/1880", p.dateNaissance)
        assertEquals("Paris", p.lieuNaissance)
        assertEquals("1950", p.dateDeces)
        assertEquals(1, p.evenementsLibres.size)
        assertEquals("Lille", p.evenementsLibres[0].lieu)
    }
}
