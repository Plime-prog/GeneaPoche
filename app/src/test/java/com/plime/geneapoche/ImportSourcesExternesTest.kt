package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Sources d'un GEDCOM externe : citations au niveau des evenements. Fichier fictif.
class ImportSourcesExternesTest {

    private fun ged(vararg l: String) = l.joinToString("\r\n") + "\r\n"

    private val base = ged(
        "0 HEAD",
        "0 @S1@ SOUR", "1 TITL Etat civil de Bourges",
        "0 @I1@ INDI", "1 NAME Jean /DUPONT/", "1 SEX M",
        "1 BIRT", "2 DATE 5 MAR 1880", "2 SOUR @S1@", "3 PAGE acte 12", "3 QUAY 3",
        "1 DEAT", "2 DATE 1 JAN 1950", "2 SOUR @S1@", "3 PAGE acte 99",
        "1 RESI", "2 DATE 1900", "2 SOUR @S1@", "3 PAGE recensement",
        "1 FAMS @F1@",
        "0 @I2@ INDI", "1 NAME Marie /MARTIN/", "1 SEX F", "1 FAMS @F1@",
        "0 @F1@ FAM", "1 HUSB @I1@", "1 WIFE @I2@",
        "1 MARR", "2 DATE 1905", "2 SOUR @S1@", "3 PAGE mariage 4",
        "1 DIV", "2 DATE 1920", "2 SOUR @S1@", "3 PAGE divorce 7",
        "0 TRLR"
    )

    @Test
    fun citations_d_evenements_sont_lues() {
        val r = importerGedcomRapporte(base)
        assertEquals(1, r.sources.size)
        val pages = r.citations.map { it.cible + ":" + it.page }
        assertTrue(pages.toString(), "N:acte 12" in pages)
        assertTrue(pages.toString(), "D:acte 99" in pages)
        assertTrue(pages.toString(), "M:mariage 4" in pages)
        assertEquals(4 + 0, r.citations.size.coerceAtLeast(4))
    }

    @Test
    fun citations_de_residence_et_divorce_ne_sont_pas_perdues() {
        val r = importerGedcomRapporte(base)
        val pages = r.citations.map { it.page }
        assertTrue(pages.toString(), "recensement" in pages)
        assertTrue(pages.toString(), "divorce 7" in pages)
    }
}
