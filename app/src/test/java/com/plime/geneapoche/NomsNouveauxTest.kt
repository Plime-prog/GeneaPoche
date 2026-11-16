package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Garde-fou de la creation de fiches depuis un acte : seuls les noms qui ne
// correspondent a aucune fiche sont proposes a la creation. Donnees fictives.
class NomsNouveauxTest {

    private val base = listOf(
        Personne(id = 1L, nom = "DURAND", prenoms = "Marie Jeanne"),
        Personne(id = 2L, nom = "Lefèvre", prenoms = "Paul")
    )

    @Test
    fun nom_connu_malgre_casse_accents_et_ordre_des_prenoms() {
        assertTrue(nomDejaConnu(base, "durand", "jeanne marie"))
        assertTrue(nomDejaConnu(base, "LEFEVRE", "paul"))
        assertFalse(nomDejaConnu(base, "Durand", "Pierre"))
    }

    @Test
    fun nom_et_prenoms_vides_comptent_comme_connus() {
        assertTrue(nomDejaConnu(base, "  ", ""))
    }

    @Test
    fun acte_avec_personnes_connues_ne_propose_rien() {
        val a = ActeSaisie(
            type = "N", nom = "Durand", prenoms = "Marie Jeanne",
            pereNom = "Lefevre", perePrenoms = "Paul"
        )
        assertTrue(nomsNouveaux(base, a).isEmpty())
    }

    @Test
    fun personne_concernee_inconnue_puis_parents_inconnus() {
        val a = ActeSaisie(
            type = "N", nom = "Martin", prenoms = "Luc",
            pereNom = "Martin", perePrenoms = "Jean", mereNom = "Roux", merePrenoms = "Anne"
        )
        assertEquals(
            listOf("Luc Martin", "père : Jean Martin", "mère : Anne Roux"),
            nomsNouveaux(base, a)
        )
    }

    @Test
    fun personne_concernee_deja_choisie_par_id_n_est_pas_proposee() {
        val a = ActeSaisie(type = "N", personneId = 1L, nom = "Inconnu", prenoms = "Xavier")
        assertTrue(nomsNouveaux(base, a).isEmpty())
    }

    @Test
    fun mariage_propose_mariee_et_ses_parents_inconnus() {
        val a = ActeSaisie(
            type = "M", personneId = 2L,
            marieeNom = "Bernard", marieePrenoms = "Louise",
            pMarieeNom = "Bernard", pMarieePrenoms = "Louis",
            mMarieeNom = "Petit", mMarieePrenoms = "Rose"
        )
        assertEquals(
            listOf(
                "mariée : Louise Bernard",
                "père de la mariée : Louis Bernard",
                "mère de la mariée : Rose Petit"
            ),
            nomsNouveaux(base, a)
        )
    }

    @Test
    fun les_champs_de_mariee_sont_ignores_hors_mariage() {
        val a = ActeSaisie(type = "N", personneId = 1L, marieeNom = "Bernard", marieePrenoms = "Louise")
        assertTrue(nomsNouveaux(base, a).isEmpty())
    }

    @Test
    fun temoins_inconnus_sont_proposes_avec_leur_role() {
        val a = ActeSaisie(
            type = "N", personneId = 1L,
            temoins = listOf(
                TemoinSaisie(nom = "Garnier", prenoms = "Hugues", role = "Prêtre"),
                TemoinSaisie(nom = "Durand", prenoms = "Marie Jeanne"),
                TemoinSaisie(nom = "Colin", prenoms = "Eva"),
                TemoinSaisie()
            )
        )
        assertEquals(
            listOf("prêtre : Hugues Garnier", "témoin : Eva Colin"),
            nomsNouveaux(base, a)
        )
    }
}
