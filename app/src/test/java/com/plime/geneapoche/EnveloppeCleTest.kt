package com.plime.geneapoche

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

// v0.110 : tests de l'enveloppe de la cle maitre (PIN puis cle systeme).
// En production la cle systeme est dans AndroidKeyStore ; ici une cle AES
// ordinaire joue ce role. Iterations reduites pour la vitesse (sauf un test
// a 600 000, valeur de production). Donnees fictives.
class EnveloppeCleTest {

    private val maitre = ByteArray(32) { (it * 7 + 3).toByte() }
    private val sel = ByteArray(16) { (it + 1).toByte() }
    private val systeme = SecretKeySpec(ByteArray(32) { (it + 100).toByte() }, "AES")
    private val pin = "482915"
    private val iter = 20_000

    @Test
    fun aller_retour_avec_les_iterations_de_production() {
        val emb = EnveloppeCle.envelopper(maitre, pin, sel, 600_000, systeme)
        assertArrayEquals(maitre, EnveloppeCle.ouvrir(emb, pin, sel, 600_000, systeme))
    }

    @Test
    fun mauvais_pin_ne_donne_rien() {
        val emb = EnveloppeCle.envelopper(maitre, pin, sel, iter, systeme)
        assertNull(EnveloppeCle.ouvrir(emb, "482916", sel, iter, systeme))
    }

    @Test
    fun mauvaise_cle_systeme_ne_donne_rien() {
        val emb = EnveloppeCle.envelopper(maitre, pin, sel, iter, systeme)
        val autre = SecretKeySpec(ByteArray(32) { 9 }, "AES")
        assertNull(EnveloppeCle.ouvrir(emb, pin, sel, iter, autre))
    }

    @Test
    fun mauvais_sel_ou_mauvaises_iterations_ne_donnent_rien() {
        val emb = EnveloppeCle.envelopper(maitre, pin, sel, iter, systeme)
        assertNull(EnveloppeCle.ouvrir(emb, pin, ByteArray(16), iter, systeme))
        assertNull(EnveloppeCle.ouvrir(emb, pin, sel, iter + 1, systeme))
    }

    @Test
    fun enveloppe_alteree_est_refusee_par_le_tag_gcm() {
        val emb = EnveloppeCle.envelopper(maitre, pin, sel, iter, systeme)
        for (pos in listOf(0, 12, emb.size / 2, emb.size - 1)) {
            val copie = emb.copyOf()
            copie[pos] = (copie[pos].toInt() xor 0x01).toByte()
            assertNull("octet $pos", EnveloppeCle.ouvrir(copie, pin, sel, iter, systeme))
        }
        assertNull(EnveloppeCle.ouvrir(emb.copyOf(10), pin, sel, iter, systeme))
        assertNull(EnveloppeCle.ouvrir(ByteArray(0), pin, sel, iter, systeme))
    }

    @Test
    fun deux_enveloppes_du_meme_maitre_different_mais_s_ouvrent_toutes_deux() {
        val a = EnveloppeCle.envelopper(maitre, pin, sel, iter, systeme)
        val b = EnveloppeCle.envelopper(maitre, pin, sel, iter, systeme)
        assertFalse(a.contentEquals(b))
        assertArrayEquals(maitre, EnveloppeCle.ouvrir(a, pin, sel, iter, systeme))
        assertArrayEquals(maitre, EnveloppeCle.ouvrir(b, pin, sel, iter, systeme))
    }

    @Test
    fun changement_de_pin_conserve_la_cle_maitre_et_invalide_l_ancien_pin() {
        val ancienne = EnveloppeCle.envelopper(maitre, pin, sel, iter, systeme)
        val ouverte = EnveloppeCle.ouvrir(ancienne, pin, sel, iter, systeme)
        assertNotNull(ouverte)
        val nouveauSel = ByteArray(16) { (it + 50).toByte() }
        val nouvelle = EnveloppeCle.envelopper(ouverte!!, "735190", nouveauSel, iter, systeme)
        assertArrayEquals(maitre, EnveloppeCle.ouvrir(nouvelle, "735190", nouveauSel, iter, systeme))
        assertNull(EnveloppeCle.ouvrir(nouvelle, pin, nouveauSel, iter, systeme))
    }

    @Test
    fun hacherPin_est_deterministe_de_32_octets_et_depend_du_sel_et_des_iterations() {
        val h = EnveloppeCle.hacherPin(pin, sel, iter)
        assertEquals(32, h.size)
        assertArrayEquals(h, EnveloppeCle.hacherPin(pin, sel, iter))
        assertFalse(h.contentEquals(EnveloppeCle.hacherPin(pin, ByteArray(16), iter)))
        assertFalse(h.contentEquals(EnveloppeCle.hacherPin(pin, sel, iter + 1)))
        assertFalse(h.contentEquals(EnveloppeCle.hacherPin("482916", sel, iter)))
    }
}
