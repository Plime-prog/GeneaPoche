package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Test

// Import / export des dates GEDCOM avec qualificatifs : une date approximative
// ne doit jamais revenir comme une date exacte. Donnees fictives.
class DatesGedcomQualifTest {

    @Test
    fun import_qualificatif_devant_date_complete_est_conserve() {
        assertEquals("vers 1850", dateFrancaise("ABT 12 MAR 1850"))
        assertEquals("avant 1900", dateFrancaise("BEF 5 JUN 1900"))
        assertEquals("après 1900", dateFrancaise("AFT MAR 1900"))
        assertEquals("vers 1750", dateFrancaise("EST 1750"))
        assertEquals("vers 1750", dateFrancaise("CAL 1750"))
        assertEquals("vers 1750", dateFrancaise("about 1750"))
        assertEquals("entre 1850 et 1851", dateFrancaise("BET 12 MAR 1850 AND 3 APR 1851"))
    }

    @Test
    fun import_dates_exactes_inchangees() {
        assertEquals("27/10/1890", dateFrancaise("27 OCT 1890"))
        assertEquals("05/10/1890", dateFrancaise("5 OCT 1890"))
        assertEquals("1890", dateFrancaise("1890"))
        assertEquals("vers 1890", dateFrancaise("ABT 1890"))
        assertEquals("entre 1850 et 1860", dateFrancaise("BET 1850 AND 1860"))
    }

    @Test
    fun import_mois_en_minuscules_est_reconnu() {
        assertEquals("27/10/1890", dateFrancaise("27 oct 1890"))
    }

    @Test
    fun import_date_incomprehensible_reste_telle_quelle() {
        assertEquals("inconnue", dateFrancaise("inconnue"))
        assertEquals("", dateFrancaise("  "))
    }

    @Test
    fun export_qualificatif_devant_date_complete_est_conserve() {
        assertEquals("ABT 1850", dateGedcom("vers 12/03/1850"))
        assertEquals("BEF 1900", dateGedcom("avant 05/06/1900"))
        assertEquals("AFT 1900", dateGedcom("après 1900"))
        assertEquals("ABT 1890", dateGedcom("vers 1890"))
    }

    @Test
    fun export_date_exacte_inchangee() {
        assertEquals("12 MAR 1850", dateGedcom("12/03/1850"))
        assertEquals("27 OCT 1890", dateGedcom("27/10/1890"))
        assertEquals("1850", dateGedcom("31/02/1850"))
    }
}
