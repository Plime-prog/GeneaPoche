package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Import de fichiers GEDCOM venant d'AUTRES logiciels (Heredis, Geneatique...).
// Fichiers fictifs ecrits a la main, avec fins de ligne Windows.
class ImportGedcomExterneTest {

    private fun ged(vararg l: String) = l.joinToString("\r\n") + "\r\n"

    @Test
    fun fins_de_ligne_windows_et_structure_de_base() {
        val r = importerGedcomRapporte(ged(
            "0 HEAD", "1 SOUR HEREDIS", "1 GEDC", "2 VERS 5.5.1", "1 CHAR UTF-8",
            "0 @I1@ INDI", "1 NAME Jean Pierre /DUPONT/", "1 SEX M",
            "1 BIRT", "2 DATE 27 OCT 1890", "2 PLAC Roubaix, 59100, Nord, Hauts-de-France, France",
            "0 TRLR"
        ))
        assertEquals(1, r.personnes.size)
        val p = r.personnes[0]
        assertEquals("DUPONT", p.nom)
        assertEquals("Jean Pierre", p.prenoms)
        assertEquals("M", p.sexe)
        assertEquals("27/10/1890", p.dateNaissance)
        assertEquals("59", p.deptNaissance)
        assertEquals("Roubaix, 59100, Nord", p.lieuNaissance)
    }

    @Test
    fun premier_nom_est_le_nom_principal_pas_le_nom_d_epouse() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI",
            "1 NAME Marie /DUPONT/", "2 GIVN Marie", "2 SURN DUPONT",
            "1 NAME Marie /MARTIN/", "2 TYPE married",
            "1 NAME La Belle /AUTRE/", "2 TYPE aka",
            "1 SEX F"
        ))
        val p = r.personnes.single()
        assertEquals("DUPONT", p.nom)
        assertEquals("Marie", p.prenoms)
    }

    @Test
    fun lieu_avec_nom_de_departement_sans_code_postal() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Paul /LEROY/",
            "1 DEAT", "2 DATE 3 MAY 1950", "2 PLAC Roubaix, Nord, France"
        ))
        val p = r.personnes.single()
        assertEquals("Roubaix", p.lieuDeces)
        assertEquals("59", p.deptDeces)
    }

    @Test
    fun lieu_sans_departement_reste_entier() {
        assertEquals("Quelque part" to "", couperLieu("Quelque part"))
        assertEquals("Roubaix" to "59", couperLieu("Roubaix, 59"))
    }

    @Test
    fun ligne_mal_formee_ou_balise_inconnue_ne_casse_pas_l_import() {
        val r = importerGedcomRapporte(ged(
            "0 HEAD", "ligne sans niveau", "x y z",
            "0 @I1@ INDI", "1 NAME Luc /MARTIN/", "1 _INCONNU valeur", "2 AUTRE chose",
            "1 CREM", "2 DATE 1 JAN 1900",
            "0 @I2@ INDI", "1 NAME Eva /ROUX/",
            "0 @F1@ FAM", "1 HUSB @I1@", "1 WIFE @I2@", "1 CHIL @I99@",
            "0 TRLR"
        ))
        assertEquals(2, r.personnes.size)
        val luc = r.personnes.first { it.prenoms == "Luc" }
        val eva = r.personnes.first { it.prenoms == "Eva" }
        assertEquals(eva.id, luc.conjoint)
        // enfant inexistant : aucun lien orphelin
        assertTrue(luc.enfants.isEmpty() && eva.enfants.isEmpty())
    }

    @Test
    fun note_avec_conc_et_cont_est_recollee() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Luc /MARTIN/",
            "1 NOTE Premiere partie de la note", "2 CONC  suite collee", "2 CONT Deuxieme ligne"
        ))
        assertEquals("Premiere partie de la note suite collee\nDeuxieme ligne", r.personnes.single().notesLibres)
    }

    @Test
    fun plusieurs_conjoints_fams_multiples_sont_relus() {
        val r = importerGedcomRapporte(ged(
            "0 @I1@ INDI", "1 NAME Henri /MARTIN/", "1 SEX M", "1 FAMS @F1@", "1 FAMS @F2@",
            "0 @I2@ INDI", "1 NAME Anne /ROUX/", "1 SEX F", "1 FAMS @F1@",
            "0 @I3@ INDI", "1 NAME Claire /PETIT/", "1 SEX F", "1 FAMS @F2@",
            "0 @I4@ INDI", "1 NAME Paul /MARTIN/", "1 FAMC @F1@",
            "0 @I5@ INDI", "1 NAME Lucie /MARTIN/", "1 FAMC @F2@",
            "0 @F1@ FAM", "1 HUSB @I1@", "1 WIFE @I2@", "1 CHIL @I4@",
            "0 @F2@ FAM", "1 HUSB @I1@", "1 WIFE @I3@", "1 CHIL @I5@"
        ))
        val henri = r.personnes.first { it.prenoms == "Henri" }
        val anne = r.personnes.first { it.prenoms == "Anne" }
        val claire = r.personnes.first { it.prenoms == "Claire" }
        assertEquals(setOf(anne.id, claire.id), (listOf(henri.conjoint) + henri.conjointsSupp).toSet())
        assertEquals(anne.id, r.personnes.first { it.prenoms == "Paul" }.mere)
        assertEquals(claire.id, r.personnes.first { it.prenoms == "Lucie" }.mere)
    }
}
