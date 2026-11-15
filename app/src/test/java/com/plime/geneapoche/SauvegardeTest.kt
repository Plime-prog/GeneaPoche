package com.plime.geneapoche

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// v0.108 : tests du chiffrement des sauvegardes (.gpbak / .gpged), logique
// pure JVM. Donnees entierement fictives.
class SauvegardeTest {

    private val phrase = "phrase-secrete-fictive"
    private val clair = "{\"personnes\":[],\"journal\":[]}".toByteArray(Charsets.UTF_8)

    // Fabrique un fichier au format .gpbak avec un nombre d'iterations donne,
    // comme le faisait l'application avant la v0.108 (210 000).
    private fun fichierAvecIterations(iterations: Int, donnees: ByteArray): ByteArray {
        val sel = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(phrase.toCharArray(), sel, iterations, 256)
        val cle = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(cle, "AES"))
        val chiffre = cipher.iv + cipher.doFinal(donnees)
        val sortie = ByteArrayOutputStream()
        sortie.write("GPBAK1".toByteArray(Charsets.US_ASCII))
        sortie.write(sel)
        sortie.write(
            byteArrayOf(
                (iterations ushr 24).toByte(), (iterations ushr 16).toByte(),
                (iterations ushr 8).toByte(), iterations.toByte()
            )
        )
        sortie.write(chiffre)
        return sortie.toByteArray()
    }

    @Test
    fun aller_retour_conserve_les_donnees() {
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)
        assertNotNull(fichier)
        assertArrayEquals(clair, Sauvegarde.dechiffrerOctets(phrase, fichier!!))
    }

    @Test
    fun nouveau_fichier_porte_600000_iterations_dans_son_entete() {
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)!!
        var lu = 0
        for (i in 0 until 4) lu = (lu shl 8) or (fichier[22 + i].toInt() and 0xFF)
        assertEquals(600_000, lu)
        assertEquals(Sauvegarde.ITERATIONS, lu)
    }

    @Test
    fun ancienne_sauvegarde_210000_iterations_reste_lisible() {
        val ancien = fichierAvecIterations(210_000, clair)
        assertArrayEquals(clair, Sauvegarde.dechiffrerOctets(phrase, ancien))
    }

    @Test
    fun mauvaise_phrase_ne_produit_rien() {
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)!!
        assertNull(Sauvegarde.dechiffrerOctets("autre-phrase-fausse", fichier))
    }

    @Test
    fun octet_modifie_est_detecte_par_le_tag_gcm() {
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)!!
        fichier[fichier.size - 1] = (fichier[fichier.size - 1].toInt() xor 0x01).toByte()
        assertNull(Sauvegarde.dechiffrerOctets(phrase, fichier))
    }

    @Test
    fun entete_modifie_est_refuse() {
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)!!
        // iterations falsifiees dans l'entete : la cle derivee change, GCM echoue
        fichier[25] = (fichier[25].toInt() xor 0x01).toByte()
        assertNull(Sauvegarde.dechiffrerOctets(phrase, fichier))
    }

    @Test
    fun iterations_hors_bornes_sont_refusees() {
        val tropPeu = fichierAvecIterations(1_000, clair)
        assertNull(Sauvegarde.dechiffrerOctets(phrase, tropPeu))
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)!!
        // 0x7FFFFFFF dans l'entete : au-dessus du maximum, refuse sans calcul
        fichier[22] = 0x7F; fichier[23] = 0xFF.toByte(); fichier[24] = 0xFF.toByte(); fichier[25] = 0xFF.toByte()
        assertNull(Sauvegarde.dechiffrerOctets(phrase, fichier))
    }

    @Test
    fun magie_incorrecte_ou_fichier_tronque_est_refuse() {
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)!!
        assertNull(Sauvegarde.dechiffrerOctets(phrase, fichier, Sauvegarde.MAGIQUE_GPGED))
        assertNull(Sauvegarde.dechiffrerOctets(phrase, fichier.copyOf(30)))
        assertNull(Sauvegarde.dechiffrerOctets(phrase, ByteArray(0)))
    }

    @Test
    fun phrase_trop_courte_est_refusee() {
        assertNull(Sauvegarde.chiffrerOctets("court", clair))
        val fichier = Sauvegarde.chiffrerOctets(phrase, clair)!!
        assertNull(Sauvegarde.dechiffrerOctets("court", fichier))
    }

    @Test
    fun gpged_est_reconnu_et_distinct_de_gpbak() {
        val ged = Sauvegarde.chiffrerOctets(phrase, clair, Sauvegarde.MAGIQUE_GPGED)!!
        assertTrue(Sauvegarde.estGpged(ged))
        assertFalse(Sauvegarde.estGpged(Sauvegarde.chiffrerOctets(phrase, clair)!!))
        assertArrayEquals(clair, Sauvegarde.dechiffrerOctets(phrase, ged, Sauvegarde.MAGIQUE_GPGED))
    }

    @Test
    fun deux_chiffrements_du_meme_contenu_different() {
        val a = Sauvegarde.chiffrerOctets(phrase, clair)!!
        val b = Sauvegarde.chiffrerOctets(phrase, clair)!!
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun lecture_bornee_accepte_sous_la_limite_et_refuse_au_dessus() {
        val donnees = ByteArray(200_000) { (it % 251).toByte() }
        assertArrayEquals(donnees, Sauvegarde.lireBorne(ByteArrayInputStream(donnees), 200_000))
        assertNull(Sauvegarde.lireBorne(ByteArrayInputStream(donnees), 199_999))
        assertEquals(0, Sauvegarde.lireBorne(ByteArrayInputStream(ByteArray(0)), 10)!!.size)
    }
}
