package com.plime.geneapoche

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// v0.110 : tests de l'ecriture atomique, de la rotation .bak, de la lecture
// tout ou rien et de la restauration (logique extraite de Stockage). Le
// "chiffrement" de test est un simple marqueur '#' en tete : seule la logique
// de fichiers est testee ici (le vrai AES-GCM l'est dans SauvegardeTest et
// EnveloppeCleTest). Donnees entierement fictives.
class FichiersSursTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val nom = "personnes.bin"

    private fun valide(texte: String) = ("#" + texte).toByteArray(Charsets.UTF_8)

    private val dechiffrer: (ByteArray) -> ByteArray? = { b ->
        if (b.isNotEmpty() && b[0] == '#'.code.toByte()) b.copyOfRange(1, b.size) else null
    }

    // Parse tout ou rien : un element "BAD" fait echouer le parse entier.
    private val parse: (String) -> MutableList<String> = { t ->
        val l = t.split(",").filter { it.isNotEmpty() }
        if (l.contains("BAD")) throw IllegalArgumentException("element invalide")
        l.toMutableList()
    }

    private fun dossier(): File = tmp.root

    private fun ecrire(contenu: ByteArray) = FichiersSurs.ecrireSecurise(dossier(), nom, contenu)

    // ------------------------------------------------------------ Ecriture

    @Test
    fun premiere_ecriture_cree_le_principal_sans_tmp_ni_bak() {
        assertTrue(ecrire(valide("a,b")))
        assertTrue(File(dossier(), nom).exists())
        assertFalse(File(dossier(), "$nom.tmp").exists())
        assertFalse(File(dossier(), "$nom.bak").exists())
    }

    @Test
    fun deuxieme_ecriture_garde_l_ancien_contenu_en_bak() {
        ecrire(valide("v1"))
        ecrire(valide("v2"))
        assertArrayEquals(valide("v2"), File(dossier(), nom).readBytes())
        assertArrayEquals(valide("v1"), File(dossier(), "$nom.bak").readBytes())
        assertFalse(File(dossier(), "$nom.tmp").exists())
    }

    @Test
    fun la_rotation_ne_conserve_qu_une_copie_la_precedente() {
        ecrire(valide("v1"))
        ecrire(valide("v2"))
        ecrire(valide("v3"))
        assertArrayEquals(valide("v3"), File(dossier(), nom).readBytes())
        assertArrayEquals(valide("v2"), File(dossier(), "$nom.bak").readBytes())
    }

    @Test
    fun ecriture_dans_un_dossier_inexistant_echoue_proprement() {
        val absent = File(dossier(), "nexiste/pas")
        assertFalse(FichiersSurs.ecrireSecurise(absent, nom, valide("x")))
    }

    // ------------------------------------------------------------ lireFichier

    @Test
    fun lireFichier_prefere_le_principal_puis_le_bak_puis_null() {
        assertNull(FichiersSurs.lireFichier(dossier(), nom))
        File(dossier(), "$nom.bak").writeBytes(valide("copie"))
        assertArrayEquals(valide("copie"), FichiersSurs.lireFichier(dossier(), nom))
        File(dossier(), nom).writeBytes(valide("principal"))
        assertArrayEquals(valide("principal"), FichiersSurs.lireFichier(dossier(), nom))
    }

    // ------------------------------------------------------------ Lecture tout ou rien

    @Test
    fun aucun_fichier_donne_une_liste_vide_non_illisible() {
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertTrue(r.liste.isEmpty())
        assertFalse(r.illisible)
    }

    @Test
    fun principal_valide_est_lu() {
        ecrire(valide("a,b,c"))
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertEquals(listOf("a", "b", "c"), r.liste)
        assertFalse(r.illisible)
    }

    @Test
    fun principal_corrompu_bascule_sur_le_bak() {
        ecrire(valide("ancien"))
        ecrire(valide("nouveau"))
        // le principal est corrompu (plus de marqueur) : le .bak ("ancien") sert
        File(dossier(), nom).writeBytes("xxxxxx".toByteArray())
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertEquals(listOf("ancien"), r.liste)
        assertFalse(r.illisible)
    }

    @Test
    fun principal_absent_et_bak_valide_est_lu() {
        File(dossier(), "$nom.bak").writeBytes(valide("x,y"))
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertEquals(listOf("x", "y"), r.liste)
        assertFalse(r.illisible)
    }

    @Test
    fun un_element_invalide_ne_donne_jamais_une_liste_partielle() {
        // "a" est valide mais "BAD" invalide : tout le fichier est refuse
        File(dossier(), nom).writeBytes(valide("a,BAD,c"))
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertTrue(r.liste.isEmpty())
        assertTrue(r.illisible)
    }

    @Test
    fun parse_invalide_sur_le_principal_bascule_sur_le_bak_valide() {
        File(dossier(), nom).writeBytes(valide("a,BAD"))
        File(dossier(), "$nom.bak").writeBytes(valide("sain1,sain2"))
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertEquals(listOf("sain1", "sain2"), r.liste)
        assertFalse(r.illisible)
    }

    @Test
    fun principal_et_bak_corrompus_sont_signales_illisibles() {
        File(dossier(), nom).writeBytes("abc".toByteArray())
        File(dossier(), "$nom.bak").writeBytes("def".toByteArray())
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertTrue(r.liste.isEmpty())
        assertTrue(r.illisible)
    }

    @Test
    fun principal_corrompu_sans_bak_est_illisible_et_rien_n_est_efface() {
        val corrompu = "abc".toByteArray()
        File(dossier(), nom).writeBytes(corrompu)
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertTrue(r.illisible)
        assertArrayEquals(corrompu, File(dossier(), nom).readBytes())
    }

    @Test
    fun coupure_en_cours_d_ecriture_un_tmp_partiel_est_ignore() {
        // Etat laisse par une coupure : principal absent, .tmp incomplet,
        // copie .bak saine.
        File(dossier(), "$nom.bak").writeBytes(valide("sain"))
        File(dossier(), "$nom.tmp").writeBytes("#sa".toByteArray())
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertEquals(listOf("sain"), r.liste)
        assertFalse(r.illisible)
    }

    // ------------------------------------------------------------ Restauration

    @Test
    fun restaurerBak_remet_la_copie_en_principal() {
        ecrire(valide("ancien"))
        ecrire(valide("nouveau"))
        assertTrue(FichiersSurs.restaurerBak(dossier(), nom))
        assertArrayEquals(valide("ancien"), File(dossier(), nom).readBytes())
        assertFalse(File(dossier(), "$nom.tmp").exists())
        // le .bak est conserve : la restauration est une copie, jamais une perte
        assertTrue(File(dossier(), "$nom.bak").exists())
    }

    @Test
    fun restaurerBak_sans_copie_renvoie_faux_et_ne_touche_a_rien() {
        File(dossier(), nom).writeBytes(valide("seul"))
        assertFalse(FichiersSurs.restaurerBak(dossier(), nom))
        assertArrayEquals(valide("seul"), File(dossier(), nom).readBytes())
    }

    @Test
    fun cycle_complet_ecriture_corruption_restauration_lecture() {
        ecrire(valide("a"))
        ecrire(valide("a,b"))
        File(dossier(), nom).writeBytes("corrompu".toByteArray())
        assertTrue(FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse).liste == listOf("a"))
        assertTrue(FichiersSurs.restaurerBak(dossier(), nom))
        val r = FichiersSurs.lireToutOuRien(dossier(), nom, dechiffrer, parse)
        assertEquals(listOf("a"), r.liste)
        assertFalse(r.illisible)
    }
}
