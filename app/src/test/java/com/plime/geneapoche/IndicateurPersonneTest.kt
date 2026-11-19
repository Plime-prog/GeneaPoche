package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Test

// v0.141 : indicateurs d'une personne.
class IndicateurPersonneTest {
    private val an = java.time.LocalDate.now().year

    @Test
    fun vivant_de_moins_de_100_ans_avec_conjoint_est_sur_4() {
        val p = Personne(id = 1, nom = "A", prenoms = "B", sexe = "M",
            dateNaissance = "01/01/" + (an - 60).toString(), lieuNaissance = "Bourges (18033)", conjoint = 2)
        assertEquals(4, indicateurPersonne(p).second)
    }

    @Test
    fun vivant_de_moins_de_100_ans_sans_conjoint_ne_compte_pas_le_mariage() {
        val p = Personne(id = 1, nom = "A", prenoms = "B", sexe = "M",
            dateNaissance = "01/01/" + (an - 60).toString())
        assertEquals(2, indicateurPersonne(p).second)
    }

    @Test
    fun personne_nee_il_y_a_plus_de_100_ans_reste_sur_6() {
        val p = Personne(id = 1, nom = "A", prenoms = "B", sexe = "M",
            dateNaissance = "01/01/" + (an - 150).toString(), conjoint = 2)
        assertEquals(6, indicateurPersonne(p).second)
    }

    @Test
    fun personne_decedee_reste_sur_6() {
        val p = Personne(id = 1, nom = "A", prenoms = "B", sexe = "M",
            dateNaissance = "01/01/" + (an - 80).toString(), dateDeces = "01/01/" + (an - 10).toString(), conjoint = 2)
        assertEquals(6, indicateurPersonne(p).second)
    }

    @Test
    fun vivant_de_moins_de_30_ans_reste_sur_2() {
        val p = Personne(id = 1, nom = "A", prenoms = "B", sexe = "F",
            dateNaissance = "01/01/" + (an - 20).toString(), conjoint = 2)
        assertEquals(2, indicateurPersonne(p).second)
    }
}
