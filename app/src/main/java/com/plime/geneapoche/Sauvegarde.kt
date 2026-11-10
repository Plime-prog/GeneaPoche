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
// base : personnes et journal (v0.83/12.0 : les actes ne sont plus stockes
// dans l'application ; les coches « Enregistré » vivent sur les fiches).
// Format .gpbak : [6 octets "GPBAK1"][16 octets sel][4 octets iterations]
// [12 octets IV][donnees chiffrees + tag GCM]. Aucune permission Internet.
// 12.2 : le meme chassis chiffre aussi un EXPORT GEDCOM (.gpged, magie
// "GPGED1") : le fichier ne s'ouvre que dans GeneaPoche, avec la phrase.
object Sauvegarde {
    private const val ITERATIONS = 210_000
    private val MAGIQUE = "GPBAK1".toByteArray(Charsets.US_ASCII)
    // 12.2 : magie d'un export GEDCOM chiffre.
    val MAGIQUE_GPGED = "GPGED1".toByteArray(Charsets.US_ASCII)

    // 12.2 : reconnait un export GEDCOM chiffre (.gpged) parmi des octets.
    fun estGpged(octets: ByteArray): Boolean {
        if (octets.size < MAGIQUE_GPGED.size) return false
        for (i in MAGIQUE_GPGED.indices) {
            if (octets[i] != MAGIQUE_GPGED[i]) return false
        }
        return true
    }

    private fun cleDepuisPhrase(phrase: String, sel: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(phrase.toCharArray(), sel, ITERATIONS, 256)
        val fabrique = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = fabrique.generateSecret(spec).encoded
        java.util.Arrays.fill(spec.password, ' ')
        return SecretKeySpec(hash, "AES")
    }

    // 12.2 : chiffre des octets quelconques (GEDCOM) avec une phrase secrete.
    // Format identique au .gpbak, seule la magie change.
    fun chiffrerOctets(phrase: String, donnees: ByteArray, magique: ByteArray = MAGIQUE): ByteArray? {
        return try {
            if (phrase.length < 6) return null
            val sel = ByteArray(16)
            SecureRandom().nextBytes(sel)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, cleDepuisPhrase(phrase, sel))
            val chiffre = cipher.iv + cipher.doFinal(donnees)
            val sortie = ByteArrayOutputStream()
            sortie.write(magique)
            sortie.write(sel)
            sortie.write(byteArrayOf((ITERATIONS ushr 24).toByte(), (ITERATIONS ushr 16).toByte(), (ITERATIONS ushr 8).toByte(), ITERATIONS.toByte()))
            sortie.write(chiffre)
            sortie.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    // 12.2 : dechiffre des octets (null si magie, phrase ou fichier invalide ;
    // le tag GCM echoue sur une phrase erronee, sans jamais rien produire).
    fun dechiffrerOctets(phrase: String, donnees: ByteArray, magique: ByteArray = MAGIQUE): ByteArray? {
        return try {
            if (phrase.length < 6) return null
            if (donnees.size < 6 + 16 + 4 + 13) return null
            for (i in magique.indices) {
                if (donnees[i] != magique[i]) return null
            }
            val sel = donnees.copyOfRange(6, 22)
            var iterations = 0
            for (i in 0 until 4) {
                iterations = (iterations shl 8) or (donnees[22 + i].toInt() and 0xFF)
            }
            if (iterations < 100_000 || iterations > 2_000_000) return null
            val corps = donnees.copyOfRange(26, donnees.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, cleDepuisPhrase(phrase, sel), GCMParameterSpec(128, corps, 0, 12))
            cipher.doFinal(corps, 12, corps.size - 12)
        } catch (e: Exception) {
            null
        }
    }

    // Exporte toute la base (personnes + journal) chiffree par la phrase.
    fun exporter(
        ctx: Context, uri: Uri, phrase: String, personnes: List<Personne>, pistes: List<Piste>,
        // v0.90 : carnet de sources (absent des anciennes sauvegardes).
        sources: List<Source> = emptyList(), citations: List<Citation> = emptyList()
    ): Boolean {
        return try {
            if (phrase.length < 6) return false
            val racine = JSONObject()
            val jp = JSONArray()
            personnes.forEach { jp.put(it.versJson()) }
            val jj = JSONArray()
            pistes.forEach { jj.put(it.versJson()) }
            racine.put("personnes", jp)
            racine.put("journal", jj)
            racine.put("sources", sourcesVersJson(sources))
            racine.put("citations", citationsVersJson(citations))
            val clair = racine.toString().toByteArray(Charsets.UTF_8)
            val chiffre = chiffrerOctets(phrase, clair) ?: return false
            val flux = ctx.contentResolver.openOutputStream(uri) ?: return false
            flux.use { it.write(chiffre) }
            true
        } catch (e: Exception) {
            false
        }
    }

    class ContenuSauvegarde(
        val personnes: MutableList<Personne>,
        val pistes: MutableList<Piste>,
        // v0.83 : coches « Enregistré » déduites des ACTES stockés dans une
        // ANCIENNE sauvegarde (personneId -> types d'actes présents).
        val cochesActes: Map<Long, MutableSet<String>>,
        // v0.90 : carnet de sources (vide pour une sauvegarde plus ancienne).
        val sources: MutableList<Source> = mutableListOf(),
        val citations: MutableList<Citation> = mutableListOf()
    )

    // Importe un fichier .gpbak : null si fichier invalide OU phrase erronee
    // (le tag GCM echoue) - aucune liste partielle n'est jamais produite.
    // Les ANCIENNES sauvegardes (avec actes/images) restent lisibles : les
    // actes y sont convertis en coches, les images sont ignorees.
    fun importer(ctx: Context, uri: Uri, phrase: String): ContenuSauvegarde? {
        return try {
            if (phrase.length < 6) return null
            val octets = ctx.contentResolver.openInputStream(uri)?.use { s -> s.readBytes() } ?: return null
            val clair = dechiffrerOctets(phrase, octets) ?: return null
            val racine = JSONObject(String(clair, Charsets.UTF_8))
            val personnes = mutableListOf<Personne>()
            val jp = racine.getJSONArray("personnes")
            for (i in 0 until jp.length()) personnes.add(Personne.depuisJson(jp.getJSONObject(i)))
            val pistes = mutableListOf<Piste>()
            val jj = racine.getJSONArray("journal")
            for (i in 0 until jj.length()) pistes.add(Piste.depuisJson(jj.getJSONObject(i)))
            // v0.83 (12.0) : une ancienne sauvegarde contenait des actes
            // stocks ; chaque acte devient une coche « Enregistré » sur la
            // fiche de sa personne (une par type d'acte).
            val coches = mutableMapOf<Long, MutableSet<String>>()
            val ja = racine.optJSONArray("actes")
            if (ja != null) {
                for (i in 0 until ja.length()) {
                    val a = ja.getJSONObject(i)
                    val pid = a.optLong("personneId", 0L)
                    val type = a.optString("type", "")
                    if (pid != 0L && type.isNotEmpty()) {
                        coches.getOrPut(pid) { mutableSetOf() }.add(type)
                    }
                }
            }
            ContenuSauvegarde(
                personnes, pistes, coches,
                sourcesDepuisJson(racine.optJSONArray("sources")),
                citationsDepuisJson(racine.optJSONArray("citations"))
            )
        } catch (e: Exception) {
            null
        }
    }
}
