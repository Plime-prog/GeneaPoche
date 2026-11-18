package com.plime.geneapoche

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.138 : un acte localise mais non "enregistre" n'est plus une recherche.
class ActeNonEnregistreTest {

    @Test
    fun acte_localise_non_enregistre_n_est_plus_une_recherche() {
        val p = Personne(
            id = 1, nom = "Dupont", prenoms = "Jean", sexe = "M",
            dateNaissance = "05/03/1850", lieuNaissance = "Bourges (18033)"
        )
        val cles = suggestionsPour(p, listOf(p)).map { it.cle }
        assertFalse(cles.toString(), cles.any { it.endsWith("_enregistrer") })
    }

    @Test
    fun sans_date_l_acte_reste_a_retrouver() {
        val p = Personne(id = 1, nom = "Dupont", prenoms = "Jean", sexe = "M")
        val cles = suggestionsPour(p, listOf(p)).map { it.cle }
        assertTrue(cles.toString(), "naissance_acte" in cles)
        assertFalse(cles.toString(), cles.any { it.endsWith("_enregistrer") })
    }
}
