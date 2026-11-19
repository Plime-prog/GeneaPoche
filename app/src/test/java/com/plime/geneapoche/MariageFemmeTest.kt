package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Test

// v0.141 : acte de mariage ouvert depuis / pour une FEMME : elle est la mariee,
// son conjoint est le marie (avec SES parents), rien n'est inverse.
class MariageFemmeTest {
    private val pereMari = Personne(id = 1, nom = "Durand", prenoms = "Louis", sexe = "M")
    private val mereMari = Personne(id = 2, nom = "Petit", prenoms = "Rose", sexe = "F")
    private val mari = Personne(id = 3, nom = "Durand", prenoms = "Paul", sexe = "M", pere = 1, mere = 2, conjoint = 5)
    private val pereEpouse = Personne(id = 4, nom = "Martin", prenoms = "Jean", sexe = "M")
    private val epouse = Personne(id = 5, nom = "Martin", prenoms = "Anne", sexe = "F", pere = 4, conjoint = 3)
    private val tous = listOf(pereMari, mereMari, mari, pereEpouse, epouse)

    @Test
    fun femme_choisie_devient_la_mariee_et_son_conjoint_le_marie() {
        val a = prefillActe(epouse, tous, "M")
        val ecarts = StringBuilder()
        fun v(nom: String, attendu: Any?, obtenu: Any?) {
            if (attendu != obtenu) ecarts.append(nom).append("=").append(obtenu).append(" ")
        }
        v("marieeId", 5L, a.marieeId)
        v("marieePrenoms", "Anne", a.marieePrenoms)
        v("pMarieeNom", "Martin", a.pMarieeNom)
        v("personneId", 3L, a.personneId)
        v("prenoms", "Paul", a.prenoms)
        v("perePrenoms", "Louis", a.perePrenoms)
        v("merePrenoms", "Rose", a.merePrenoms)
        v("sexe", "M", a.sexe)
        assertEquals("", ecarts.toString())
    }

    @Test
    fun homme_choisi_reste_le_marie() {
        val a = prefillActe(mari, tous, "M")
        assertEquals(3L, a.personneId)
        assertEquals("Louis", a.perePrenoms)
        assertEquals(5L, a.marieeId)
    }

    @Test
    fun pourSexe_garde_le_sexe_voulu_et_les_fiches_sans_sexe() {
        val inconnu = Personne(id = 9, nom = "X", prenoms = "Y", sexe = "")
        val l = (tous + inconnu).pourSexe("F")
        assertEquals(setOf(2L, 5L, 9L), l.map { it.id }.toSet())
    }
}
