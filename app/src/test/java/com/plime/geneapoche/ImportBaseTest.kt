package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

// Fusion d'un GEDCOM importe dans la base : les identifiants du fichier (1..n,
// attribues par le lecteur) n'ont aucun rapport avec ceux de la base ; tous
// les liens doivent etre convertis. Donnees entierement fictives.
class ImportBaseTest {

    // Base existante : les ids 1 et 2 existent AUSSI dans le fichier (collision voulue).
    private fun base(): MutableList<Personne> = mutableListOf(
        Personne(id = 1L, nom = "DURAND", prenoms = "Paul", dateNaissance = "1850", profession = "meunier"),
        Personne(id = 2L, nom = "MARTIN", prenoms = "Anne"),
        Personne(id = 3L, nom = "AUTRE", prenoms = "Zed")
    )

    // Fichier : Paul (deja dans la base), ses enfants Luc et Eve, son epouse
    // Jeanne, et Bebe, fils de Luc. Ids du fichier : 1 a 5.
    private fun fichier(): List<Personne> {
        val paul = Personne(
            id = 1L, nom = "Durand", prenoms = "paul", dateNaissance = "12 mars 1850",
            lieuNaissance = "Bourges", profession = "laboureur"
        )
        val luc = Personne(id = 2L, nom = "DURAND", prenoms = "Luc", dateNaissance = "1880")
        val eve = Personne(id = 3L, nom = "DURAND", prenoms = "Eve", dateNaissance = "1882")
        val jeanne = Personne(id = 4L, nom = "LEROY", prenoms = "Jeanne", dateNaissance = "1855")
        val bebe = Personne(id = 5L, nom = "DURAND", prenoms = "Bebe", dateNaissance = "1905")
        paul.conjoint = 4L; paul.enfants.addAll(listOf(2L, 3L))
        jeanne.conjoint = 1L; jeanne.enfants.addAll(listOf(2L, 3L))
        luc.pere = 1L; luc.mere = 4L; luc.enfants.add(5L)
        luc.temoinsM.add(3L); luc.signatairesM.addAll(listOf(0L, 3L))
        eve.pere = 1L; eve.mere = 4L
        bebe.pere = 2L
        return listOf(paul, luc, eve, jeanne, bebe)
    }

    private val compteur = AtomicLong(99L)
    private val nouvelId: () -> Long = { compteur.incrementAndGet() }

    private fun par(l: List<Personne>, prenoms: String) = l.first { it.prenoms == prenoms }

    // Toute reference doit viser une personne existante (0 = aucune).
    private fun liensInvalides(l: List<Personne>): List<String> {
        val ids = l.map { it.id }.toSet()
        val pb = mutableListOf<String>()
        fun ok(v: Long, quoi: String) { if (v != 0L && v !in ids) pb.add("$quoi -> $v") }
        for (p in l) {
            ok(p.pere, "pere de ${p.id}"); ok(p.mere, "mere de ${p.id}"); ok(p.conjoint, "conjoint de ${p.id}")
            p.conjointsSupp.forEach { ok(it, "conjointSupp de ${p.id}") }
            p.enfants.forEach { ok(it, "enfant de ${p.id}") }
            for (t in listOf("N", "B", "M", "D", "S")) {
                p.temoinsDe(t).forEach { ok(it, "temoin $t de ${p.id}") }
                p.signatairesDe(t).forEach { ok(it, "signataire $t de ${p.id}") }
            }
        }
        return pb
    }

    @Test
    fun les_personnes_absentes_sont_ajoutees_les_presentes_completees() {
        val l = base()
        val r = fusionnerImportees(l, fichier(), nouvelId)
        assertEquals(1, r.completees)
        assertEquals(4, r.ajoutees)
        assertEquals(7, l.size)
        assertEquals(l.size, l.map { it.id }.toSet().size) // aucun identifiant en double
    }

    @Test
    fun une_personne_presente_est_completee_sans_jamais_ecraser() {
        val l = base()
        fusionnerImportees(l, fichier(), nouvelId)
        val paul = l.first { it.id == 1L }
        assertEquals("meunier", paul.profession)      // deja renseignee : non ecrasee
        assertEquals("1850", paul.dateNaissance)      // idem
        assertEquals("Bourges", paul.lieuNaissance)   // vide : completee
    }

    @Test
    fun tous_les_liens_des_personnes_ajoutees_sont_convertis_vers_la_base() {
        val l = base()
        fusionnerImportees(l, fichier(), nouvelId)
        val luc = par(l, "Luc"); val eve = par(l, "Eve")
        val jeanne = par(l, "Jeanne"); val bebe = par(l, "Bebe")
        assertEquals(1L, luc.pere)                       // Paul : deja present, id 1 de la base
        assertEquals(jeanne.id, luc.mere)                // nouvel id de Jeanne
        assertEquals(listOf(bebe.id), luc.enfants)
        assertEquals(listOf(eve.id), luc.temoinsM)
        assertEquals(listOf(0L, eve.id), luc.signatairesM) // 0 = "le pere" reste 0
        assertEquals(1L, eve.pere)
        assertEquals(jeanne.id, eve.mere)
        assertEquals(luc.id, bebe.pere)
        assertEquals(1L, jeanne.conjoint)
        assertEquals(listOf(luc.id, eve.id), jeanne.enfants)
    }

    @Test
    fun la_personne_presente_recoit_ses_nouveaux_liens() {
        val l = base()
        fusionnerImportees(l, fichier(), nouvelId)
        val paul = l.first { it.id == 1L }
        assertEquals(par(l, "Jeanne").id, paul.conjoint)
        assertEquals(listOf(par(l, "Luc").id, par(l, "Eve").id), paul.enfants)
    }

    @Test
    fun aucun_lien_ne_pointe_vers_une_personne_inexistante() {
        val l = base()
        fusionnerImportees(l, fichier(), nouvelId)
        assertTrue(liensInvalides(l).toString(), liensInvalides(l).isEmpty())
    }

    @Test
    fun les_ids_du_fichier_ne_s_infiltrent_pas_dans_les_personnes_de_la_base() {
        // L'id 2 du fichier (Luc) est aussi l'id de MARTIN Anne dans la base :
        // Anne ne doit etre ni mere ni parent de qui que ce soit par erreur.
        val l = base()
        fusionnerImportees(l, fichier(), nouvelId)
        val anne = l.first { it.id == 2L }
        assertTrue(anne.enfants.isEmpty())
        assertEquals(0L, anne.pere); assertEquals(0L, anne.conjoint)
        assertNotEquals(2L, par(l, "Bebe").pere)
        val zed = l.first { it.id == 3L }
        assertEquals(0L, zed.pere); assertEquals(0L, zed.mere); assertTrue(zed.enfants.isEmpty())
    }

    @Test
    fun refusionner_le_meme_fichier_n_ajoute_rien_et_ne_double_aucun_lien() {
        val l = base()
        fusionnerImportees(l, fichier(), nouvelId)
        val taille = l.size
        val r = fusionnerImportees(l, fichier(), nouvelId)
        assertEquals(5, r.completees)
        assertEquals(0, r.ajoutees)
        assertEquals(taille, l.size)
        for (p in l) assertEquals("enfants de ${p.id}", p.enfants.distinct(), p.enfants)
        assertEquals(2, l.first { it.id == 1L }.enfants.size)
        assertTrue(liensInvalides(l).isEmpty())
    }

    @Test
    fun une_date_de_naissance_incompatible_n_est_pas_un_doublon() {
        val l = base()
        val autrePaul = Personne(id = 1L, nom = "DURAND", prenoms = "Paul", dateNaissance = "1901")
        val r = fusionnerImportees(l, listOf(autrePaul), nouvelId)
        assertEquals(0, r.completees)
        assertEquals(1, r.ajoutees)
        assertEquals(2, l.count { it.nom == "DURAND" && it.prenoms == "Paul" })
    }

    @Test
    fun les_listes_ajoutees_ne_sont_pas_partagees_avec_le_fichier() {
        val l = base()
        val f = fichier()
        fusionnerImportees(l, f, nouvelId)
        val luc = par(l, "Luc")
        f.first { it.prenoms == "Luc" }.enfants.clear()
        assertEquals(1, luc.enfants.size)
    }

    @Test
    fun une_reference_inconnue_du_fichier_disparait() {
        val l = base()
        val orphelin = Personne(id = 1L, nom = "ISOLE", prenoms = "Marc")
        orphelin.enfants.add(99L); orphelin.pere = 77L
        orphelin.temoinsN.add(98L); orphelin.signatairesN.addAll(listOf(0L, 97L))
        fusionnerImportees(l, listOf(orphelin), nouvelId)
        val m = par(l, "Marc")
        assertTrue(m.enfants.isEmpty())
        assertEquals(0L, m.pere)
        assertTrue(m.temoinsN.isEmpty())
        assertEquals(listOf(0L), m.signatairesN)
        assertTrue(liensInvalides(l).isEmpty())
    }

    @Test
    fun un_fichier_vide_ne_change_rien() {
        val l = base()
        val r = fusionnerImportees(l, emptyList(), nouvelId)
        assertEquals(0, r.completees); assertEquals(0, r.ajoutees)
        assertEquals(3, l.size)
    }
}
