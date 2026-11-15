package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.113 : tests de la suppression (avec Annuler) et de la fusion de fiches.
// Familles entierement fictives.
class ActionsFichesTest {

    // Famille : 1 = pere, 2 = mere (conjoints), 3 et 4 = leurs enfants,
    // 5 = conjoint de 3, 6 = autre conjoint (supplementaire) de 1.
    private fun famille(): MutableList<Personne> {
        val pere = Personne(id = 1L, nom = "DURAND", prenoms = "Paul")
        val mere = Personne(id = 2L, nom = "MARTIN", prenoms = "Anne")
        val e1 = Personne(id = 3L, nom = "DURAND", prenoms = "Luc")
        val e2 = Personne(id = 4L, nom = "DURAND", prenoms = "Eve")
        val conj = Personne(id = 5L, nom = "PETIT", prenoms = "Zoe")
        val autre = Personne(id = 6L, nom = "BERNARD", prenoms = "Lea")
        pere.conjoint = 2L; pere.enfants.addAll(listOf(3L, 4L)); pere.conjointsSupp.add(6L)
        mere.conjoint = 1L; mere.enfants.addAll(listOf(3L, 4L))
        e1.pere = 1L; e1.mere = 2L; e1.conjoint = 5L
        e2.pere = 1L; e2.mere = 2L
        conj.conjoint = 3L
        autre.conjoint = 1L
        return mutableListOf(pere, mere, e1, e2, conj, autre)
    }

    private fun lien(l: List<Personne>, id: Long) = l.first { it.id == id }

    // ------------------------------------------------------------ Suppression

    @Test
    fun capturerLiensAvant_ne_prend_que_les_personnes_qui_citent_la_cible() {
        val l = famille()
        val liens = capturerLiensAvant(l, 1L)
        // 1 est cite par 2 (conjoint), 3 et 4 (pere), 6 (conjoint) ; pas par 5
        assertEquals(setOf(2L, 3L, 4L, 6L), liens.map { it.id }.toSet())
    }

    @Test
    fun capturerLiensAvant_copie_les_listes() {
        val l = famille()
        val liens = capturerLiensAvant(l, 3L)
        val mere = liens.first { it.id == 2L }
        lien(l, 2L).enfants.clear()
        assertEquals(listOf(3L, 4L), mere.enfants)
    }

    @Test
    fun detacherReferences_efface_toute_citation_de_la_personne_supprimee() {
        val l = famille()
        val cible = lien(l, 1L)
        l.remove(cible)
        detacherReferences(l, 1L)
        assertEquals(0L, lien(l, 2L).conjoint)
        assertEquals(0L, lien(l, 3L).pere)
        assertEquals(0L, lien(l, 4L).pere)
        assertEquals(0L, lien(l, 6L).conjoint)
        // les autres liens ne bougent pas
        assertEquals(2L, lien(l, 3L).mere)
        assertEquals(5L, lien(l, 3L).conjoint)
        assertEquals(listOf(3L, 4L), lien(l, 2L).enfants)
    }

    @Test
    fun detacherReferences_retire_aussi_les_enfants_et_conjoints_supplementaires() {
        val l = famille()
        l.remove(lien(l, 3L))
        detacherReferences(l, 3L)
        assertEquals(listOf(4L), lien(l, 1L).enfants)
        assertEquals(listOf(4L), lien(l, 2L).enfants)
        assertEquals(0L, lien(l, 5L).conjoint)
        l.remove(lien(l, 6L))
        detacherReferences(l, 6L)
        assertTrue(lien(l, 1L).conjointsSupp.isEmpty())
    }

    @Test
    fun supprimer_puis_annuler_remet_exactement_les_liens_d_origine() {
        val l = famille()
        val avant = famille()
        val cible = lien(l, 1L)
        // suppression
        val liens = capturerLiensAvant(l, cible.id)
        l.remove(cible)
        detacherReferences(l, cible.id)
        assertFalse(l.any { it.pere == 1L || it.conjoint == 1L })
        // annulation
        l.add(cible)
        restaurerLiens(l, liens)
        for (q in avant) {
            val r = lien(l, q.id)
            assertEquals("pere de ${q.id}", q.pere, r.pere)
            assertEquals("mere de ${q.id}", q.mere, r.mere)
            assertEquals("conjoint de ${q.id}", q.conjoint, r.conjoint)
            assertEquals("conjointsSupp de ${q.id}", q.conjointsSupp, r.conjointsSupp)
            assertEquals("enfants de ${q.id}", q.enfants, r.enfants)
        }
    }

    @Test
    fun restaurerLiens_ignore_une_personne_devenue_introuvable() {
        val l = famille()
        val liens = capturerLiensAvant(l, 1L)
        l.removeAll { it.id == 6L }
        restaurerLiens(l, liens) // ne doit pas lever d'exception
        assertEquals(1L, lien(l, 2L).conjoint)
    }

    // ------------------------------------------------------------ Fusion

    @Test
    fun fusion_recopie_les_champs_vides_sans_ecraser_ceux_presents() {
        val garde = Personne(id = 10L, nom = "DUPONT", prenoms = "Jean", profession = "meunier")
        val doublon = Personne(
            id = 11L, nom = "DUPONT", prenoms = "Jean",
            profession = "laboureur", sexe = "M", dateNaissance = "12 mars 1850", lieuNaissance = "Bourges"
        )
        fusionnerFiches(listOf(garde, doublon), garde, doublon)
        assertEquals("meunier", garde.profession)        // non ecrase
        assertEquals("M", garde.sexe)                     // vide -> recopie
        assertEquals("12 mars 1850", garde.dateNaissance)
        assertEquals("Bourges", garde.lieuNaissance)
    }

    @Test
    fun fusion_ne_recopie_pas_un_champ_blanc() {
        val garde = Personne(id = 10L, profession = "")
        val doublon = Personne(id = 11L, profession = "   ")
        fusionnerFiches(listOf(garde, doublon), garde, doublon)
        assertEquals("", garde.profession)
    }

    @Test
    fun fusion_reprend_les_roles_seulement_s_ils_sont_inconnus_chez_garde() {
        val garde = Personne(id = 10L)
        val doublon = Personne(id = 11L)
        garde.pere = 1L
        doublon.pere = 2L; doublon.mere = 3L; doublon.conjoint = 4L
        fusionnerFiches(listOf(garde, doublon), garde, doublon)
        assertEquals(1L, garde.pere)   // garde l'emporte
        assertEquals(3L, garde.mere)   // inconnu -> repris
        assertEquals(4L, garde.conjoint)
    }

    @Test
    fun fusion_unit_les_listes_sans_doublon_et_sans_la_cible() {
        val garde = Personne(id = 10L)
        val doublon = Personne(id = 11L)
        garde.enfants.addAll(listOf(1L, 2L))
        doublon.enfants.addAll(listOf(2L, 3L, 11L))   // 11 = lui-meme : jamais ajoute
        garde.conjointsSupp.add(5L)
        doublon.conjointsSupp.addAll(listOf(5L, 6L))
        garde.blocsOuverts.add("a")
        doublon.blocsOuverts.addAll(listOf("a", "b"))
        fusionnerFiches(listOf(garde, doublon), garde, doublon)
        assertEquals(listOf(1L, 2L, 3L), garde.enfants)
        assertEquals(listOf(5L, 6L), garde.conjointsSupp)
        assertEquals(listOf("a", "b"), garde.blocsOuverts)
    }

    @Test
    fun fusion_unit_temoins_et_signataires_de_chaque_acte() {
        val garde = Personne(id = 10L)
        val doublon = Personne(id = 11L)
        for (t in listOf("N", "B", "M", "D", "S")) {
            garde.temoinsDe(t).add(1L)
            doublon.temoinsDe(t).addAll(listOf(1L, 2L))
            doublon.signatairesDe(t).add(7L)
        }
        fusionnerFiches(listOf(garde, doublon), garde, doublon)
        for (t in listOf("N", "B", "M", "D", "S")) {
            assertEquals("temoins $t", listOf(1L, 2L), garde.temoinsDe(t))
            assertEquals("signataires $t", listOf(7L), garde.signatairesDe(t))
        }
    }

    @Test
    fun fusion_redirige_toutes_les_references_au_doublon_vers_garde() {
        val garde = Personne(id = 10L, nom = "DUPONT")
        val doublon = Personne(id = 11L, nom = "DUPONT")
        val enfant = Personne(id = 20L); enfant.pere = 11L
        val fille = Personne(id = 21L); fille.mere = 11L
        val epouse = Personne(id = 22L); epouse.conjoint = 11L; epouse.conjointsSupp.add(11L)
        val parent = Personne(id = 23L); parent.enfants.addAll(listOf(11L, 10L))
        val voisin = Personne(id = 24L)
        voisin.temoinsDe("M").add(11L); voisin.signatairesDe("D").add(11L)
        val tous = listOf(garde, doublon, enfant, fille, epouse, parent, voisin)
        fusionnerFiches(tous, garde, doublon)
        assertEquals(10L, enfant.pere)
        assertEquals(10L, fille.mere)
        assertEquals(10L, epouse.conjoint)
        assertEquals(listOf(10L), epouse.conjointsSupp)
        // 11 et 10 etaient tous deux enfants de 23 : un seul 10 apres dedoublonnage
        assertEquals(listOf(10L), parent.enfants)
        assertEquals(listOf(10L), voisin.temoinsDe("M"))
        assertEquals(listOf(10L), voisin.signatairesDe("D"))
    }

    @Test
    fun fusion_ne_retire_pas_le_doublon_de_la_liste() {
        val garde = Personne(id = 10L)
        val doublon = Personne(id = 11L)
        val liste = mutableListOf(garde, doublon)
        fusionnerFiches(liste, garde, doublon)
        assertEquals(2, liste.size) // c'est l'appelant qui retire le doublon
    }

    @Test
    fun fusion_ne_dedoublonne_pas_une_liste_qui_ne_citait_pas_le_doublon() {
        val garde = Personne(id = 10L)
        val doublon = Personne(id = 11L)
        val autre = Personne(id = 12L)
        autre.enfants.addAll(listOf(7L, 7L, 8L)) // doublons preexistants sans rapport : laisses tels quels
        fusionnerFiches(listOf(garde, doublon, autre), garde, doublon)
        assertEquals(listOf(7L, 7L, 8L), autre.enfants)
    }

    @Test
    fun fusion_sur_une_base_de_mille_personnes_reste_rapide() {
        val l = (1L..1000L).map { Personne(id = it) }
        l.forEach { it.pere = 500L; it.enfants.add(500L) }
        val garde = l[0]; val doublon = l[499] // id 500
        val debut = System.nanoTime()
        fusionnerFiches(l, garde, doublon)
        val ms = (System.nanoTime() - debut) / 1_000_000
        assertTrue("trop lent : $ms ms", ms < 2000)
        assertEquals(1L, l[10].pere)
    }
}
