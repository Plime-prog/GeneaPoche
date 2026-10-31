package com.plime.geneapoche

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// G1-08 : sauvegarde exportable/importable, chiffree AES-256/GCM avec une
// PHRASE SECRETE (PBKDF2-HmacSHA256, 210 000 iterations). Contient toute la
// base : personnes, journal, actes, notes libres et images des actes.
// Format .gpbak : [6 octets "GPBAK1"][16 octets sel][4 octets iterations]
// [12 octets IV][donnees chiffrees + tag GCM]. Aucune permission Internet.
object Sauvegarde {
    private const val ITERATIONS = 210_000
    private val MAGIQUE = "GPBAK1".toByteArray(Charsets.US_ASCII)

    private fun cleDepuisPhrase(phrase: String, sel: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(phrase.toCharArray(), sel, ITERATIONS, 256)
        val fabrique = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = fabrique.generateSecret(spec).encoded
        java.util.Arrays.fill(spec.password, ' ')
        return SecretKeySpec(hash, "AES")
    }

    // Exporte toute la base (images des actes comprises) chiffree par la phrase.
    fun exporter(ctx: Context, uri: Uri, phrase: String, personnes: List<Personne>, pistes: List<Piste>, actes: List<Acte>): Boolean {
        return try {
            if (phrase.length < 6) return false
            val racine = JSONObject()
            val jp = JSONArray()
            personnes.forEach { jp.put(it.versJson()) }
            val jj = JSONArray()
            pistes.forEach { jj.put(it.versJson()) }
            val ja = JSONArray()
            actes.forEach { ja.put(it.versJson()) }
            val ji = JSONObject()
            actes.forEach { a ->
                Stockage.lireImage(ctx, a.id)?.let { octets ->
                    ji.put(a.id.toString(), android.util.Base64.encodeToString(octets, android.util.Base64.NO_WRAP))
                }
            }
            racine.put("personnes", jp)
            racine.put("journal", jj)
            racine.put("actes", ja)
            racine.put("images", ji)
            val clair = racine.toString().toByteArray(Charsets.UTF_8)
            val sel = ByteArray(16)
            SecureRandom().nextBytes(sel)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, cleDepuisPhrase(phrase, sel))
            val chiffre = cipher.iv + cipher.doFinal(clair)
            val sortie = ByteArrayOutputStream()
            sortie.write(MAGIQUE)
            sortie.write(sel)
            sortie.write(byteArrayOf((ITERATIONS ushr 24).toByte(), (ITERATIONS ushr 16).toByte(), (ITERATIONS ushr 8).toByte(), ITERATIONS.toByte()))
            sortie.write(chiffre)
            val flux = ctx.contentResolver.openOutputStream(uri) ?: return false
            flux.use { it.write(sortie.toByteArray()) }
            true
        } catch (e: Exception) {
            false
        }
    }

    class ContenuSauvegarde(
        val personnes: MutableList<Personne>,
        val pistes: MutableList<Piste>,
        val actes: MutableList<Acte>,
        val images: Map<Long, ByteArray>
    )

    // Importe un fichier .gpbak : null si fichier invalide OU phrase erronee
    // (le tag GCM echoue) - aucune liste partielle n'est jamais produite.
    fun importer(ctx: Context, uri: Uri, phrase: String): ContenuSauvegarde? {
        return try {
            if (phrase.length < 6) return null
            val octets = ctx.contentResolver.openInputStream(uri)?.use { s -> s.readBytes() } ?: return null
            if (octets.size < 6 + 16 + 4 + 13) return null
            for (i in MAGIQUE.indices) {
                if (octets[i] != MAGIQUE[i]) return null
            }
            val sel = octets.copyOfRange(6, 22)
            var iterations = 0
            for (i in 0 until 4) {
                iterations = (iterations shl 8) or (octets[22 + i].toInt() and 0xFF)
            }
            if (iterations < 100_000 || iterations > 2_000_000) return null
            val corps = octets.copyOfRange(26, octets.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, cleDepuisPhrase(phrase, sel), GCMParameterSpec(128, corps, 0, 12))
            val clair = cipher.doFinal(corps, 12, corps.size - 12)
            val racine = JSONObject(String(clair, Charsets.UTF_8))
            val personnes = mutableListOf<Personne>()
            val jp = racine.getJSONArray("personnes")
            for (i in 0 until jp.length()) personnes.add(Personne.depuisJson(jp.getJSONObject(i)))
            val pistes = mutableListOf<Piste>()
            val jj = racine.getJSONArray("journal")
            for (i in 0 until jj.length()) pistes.add(Piste.depuisJson(jj.getJSONObject(i)))
            val actes = mutableListOf<Acte>()
            val ja = racine.getJSONArray("actes")
            for (i in 0 until ja.length()) actes.add(Acte.depuisJson(ja.getJSONObject(i)))
            val images = mutableMapOf<Long, ByteArray>()
            val ji = racine.optJSONObject("images")
            if (ji != null) {
                val cles = ji.keys()
                while (cles.hasNext()) {
                    val k = cles.next()
                    images[k.toLong()] = android.util.Base64.decode(ji.getString(k), android.util.Base64.NO_WRAP)
                }
            }
            ContenuSauvegarde(personnes, pistes, actes, images)
        } catch (e: Exception) {
            null
        }
    }
}
