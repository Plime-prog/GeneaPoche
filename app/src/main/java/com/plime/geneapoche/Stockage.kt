package com.plime.geneapoche

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

// Stockage chiffre AES-256/GCM (cle AndroidKeyStore, jamais exportable).
// Deux fichiers chiffres : personnes.bin et journal.bin (+ .bak/.tmp de securite).
// PIN : PBKDF2-HmacSHA256 (150 000 iterations, sel aleatoire 128 bits), comparaison temps constant.
// Blocage : 5 erreurs -> paliers 30 s / 2 min / 10 min / 1 h.
// Aucune permission Internet dans le manifeste : rien ne peut etre transmis.

object Stockage {
    private const val FICHIER_PERSONNES = "personnes.bin"
    private const val FICHIER_JOURNAL = "journal.bin"
    private const val FICHIER_ACTES = "actes.bin"
    private const val DOSSIER_IMAGES = "actes_img"
    private const val ANCIEN_FICHIER_CLAIR = "personnes.json"
    private const val ALIAS_CLE = "geneapoche_cle"
    private const val PREFS = "geneapoche_secu"
    private const val CLE_SEL = "pin_sel"
    private const val CLE_HASH = "pin_hash"
    private const val CLE_ERREURS = "pin_erreurs"
    private const val CLE_NIVEAU = "blocage_niveau"
    private const val CLE_JUSQUA = "blocage_jusqua"
    private const val CLE_ID_CUJUS = "id_cujus"
    private const val CLE_SOSA_CUJUS = "sosa_cujus"
    private val DUREES_BLOCAGE = longArrayOf(30_000L, 120_000L, 600_000L, 3_600_000L)
    private const val ERREURS_AVANT_PALIER = 5
    private const val ITERATIONS_PIN = 150_000

    // ------------------------------------------------------------ Preferences

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ------------------------------------------------------------ De cujus (personne de reference et son n Sosa)

    fun lireIdCujus(ctx: Context): Long = prefs(ctx).getLong(CLE_ID_CUJUS, 0L)

    fun lireSosaCujus(ctx: Context): Long = prefs(ctx).getLong(CLE_SOSA_CUJUS, 1L)

    fun definirCujus(ctx: Context, id: Long, sosa: Long) {
        prefs(ctx).edit()
            .putLong(CLE_ID_CUJUS, id)
            .putLong(CLE_SOSA_CUJUS, sosa)
            .apply()
    }

    // ------------------------------------------------------------ Cle de chiffrement (AndroidKeyStore)

    private fun cle(): javax.crypto.SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val entree = ks.getEntry(ALIAS_CLE, null) as? KeyStore.SecretKeyEntry
        if (entree != null) return entree.secretKey
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(
                ALIAS_CLE,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    private fun chiffrer(octets: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, cle())
        // Format : [12 octets IV][donnees chiffrees + tag GCM]
        return cipher.iv + cipher.doFinal(octets)
    }

    private fun dechiffrer(donnees: ByteArray): ByteArray? {
        if (donnees.size < 13) return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, cle(), GCMParameterSpec(128, donnees, 0, 12))
            cipher.doFinal(donnees, 12, donnees.size - 12)
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------ PIN

    fun aPin(ctx: Context): Boolean = prefs(ctx).contains(CLE_HASH)

    fun definirPin(ctx: Context, pin: String): Boolean {
        val sel = ByteArray(16)
        SecureRandom().nextBytes(sel)
        val hash = hacherPin(pin, sel)
        return prefs(ctx).edit()
            .putString(CLE_SEL, Base64.encodeToString(sel, Base64.NO_WRAP))
            .putString(CLE_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(CLE_ERREURS, 0)
            .putInt(CLE_NIVEAU, 0)
            .putLong(CLE_JUSQUA, 0L)
            .commit()
    }

    fun verifierPin(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        val hashStocke = p.getString(CLE_HASH, null) ?: return false
        val selStocke = p.getString(CLE_SEL, null) ?: return false
        val calcule = hacherPin(pin, Base64.decode(selStocke, Base64.NO_WRAP))
        return MessageDigest.isEqual(calcule, Base64.decode(hashStocke, Base64.NO_WRAP))
    }

    private fun hacherPin(pin: String, sel: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), sel, ITERATIONS_PIN, 256)
        val fabrique = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = fabrique.generateSecret(spec).encoded
        java.util.Arrays.fill(spec.password, ' ')
        return hash
    }

    // ------------------------------------------------------------ Blocage apres erreurs

    fun enregistrerEchec(ctx: Context) {
        val p = prefs(ctx)
        var erreurs = p.getInt(CLE_ERREURS, 0) + 1
        val niveau = p.getInt(CLE_NIVEAU, 0)
        if (erreurs >= ERREURS_AVANT_PALIER) {
            erreurs = 0
            val duree = if (niveau < DUREES_BLOCAGE.size) DUREES_BLOCAGE[niveau] else DUREES_BLOCAGE.last()
            p.edit()
                .putLong(CLE_JUSQUA, System.currentTimeMillis() + duree)
                .putInt(CLE_NIVEAU, if (niveau < DUREES_BLOCAGE.size) niveau + 1 else niveau)
                .apply()
        }
        p.edit().putInt(CLE_ERREURS, erreurs).apply()
    }

    fun reinitialiserEchecs(ctx: Context) {
        prefs(ctx).edit()
            .putInt(CLE_ERREURS, 0)
            .putInt(CLE_NIVEAU, 0)
            .putLong(CLE_JUSQUA, 0L)
            .apply()
    }

    fun secondesBlocage(ctx: Context): Long {
        val fin = prefs(ctx).getLong(CLE_JUSQUA, 0L)
        val reste = (fin - System.currentTimeMillis()) / 1000L
        return if (reste > 0L) reste else 0L
    }

    // ------------------------------------------------------------ Personnes (chiffre)

    fun charger(ctx: Context): MutableList<Personne> {
        val liste = mutableListOf<Personne>()
        // Migration : ancien fichier en clair de la v0.1/v0.2 -> chiffre puis supprime
        try {
            val legacy = File(ctx.filesDir, ANCIEN_FICHIER_CLAIR)
            if (legacy.exists()) {
                ajouterPersonnesDepuisTexte(liste, String(legacy.readBytes()))
                if (sauver(ctx, liste)) legacy.delete()
                return liste
            }
        } catch (e: Exception) {
            // on continue sur la lecture chiffree
        }
        val octets = lireFichier(ctx, FICHIER_PERSONNES) ?: return liste
        val clair = dechiffrer(octets) ?: return liste
        ajouterPersonnesDepuisTexte(liste, String(clair))
        return liste
    }

    fun sauver(ctx: Context, liste: List<Personne>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            ecrireFichier(ctx, FICHIER_PERSONNES, chiffrer(tableau.toString().toByteArray()))
        } catch (e: Exception) {
            false
        }
    }

    private fun ajouterPersonnesDepuisTexte(liste: MutableList<Personne>, texte: String) {
        try {
            val tableau = JSONArray(texte)
            for (i in 0 until tableau.length()) {
                liste.add(Personne.depuisJson(tableau.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // donnees illisibles : base vide
        }
    }

    // ------------------------------------------------------------ Journal (chiffre)

    fun chargerJournal(ctx: Context): MutableList<Piste> {
        val liste = mutableListOf<Piste>()
        val octets = lireFichier(ctx, FICHIER_JOURNAL) ?: return liste
        val clair = dechiffrer(octets) ?: return liste
        try {
            val tableau = JSONArray(String(clair))
            for (i in 0 until tableau.length()) {
                liste.add(Piste.depuisJson(tableau.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // journal illisible : vide
        }
        return liste
    }

    fun sauverJournal(ctx: Context, liste: List<Piste>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            ecrireFichier(ctx, FICHIER_JOURNAL, chiffrer(tableau.toString().toByteArray()))
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------ Actes (index chiffre + images chiffrees)

    fun chargerActes(ctx: Context): MutableList<Acte> {
        val liste = mutableListOf<Acte>()
        val octets = lireFichier(ctx, FICHIER_ACTES) ?: return liste
        val clair = dechiffrer(octets) ?: return liste
        try {
            val tableau = JSONArray(String(clair))
            for (i in 0 until tableau.length()) {
                liste.add(Acte.depuisJson(tableau.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // actes illisibles : vide
        }
        return liste
    }

    fun sauverActes(ctx: Context, liste: List<Acte>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            ecrireFichier(ctx, FICHIER_ACTES, chiffrer(tableau.toString().toByteArray()))
        } catch (e: Exception) {
            false
        }
    }

    // Images chiffrees, une par acte : actes_img/acte_<id>.bin
    fun sauverImage(ctx: Context, id: Long, octets: ByteArray): Boolean {
        return try {
            val dossier = File(ctx.filesDir, DOSSIER_IMAGES)
            if (!dossier.exists()) dossier.mkdirs()
            File(dossier, "acte_" + id + ".bin").writeBytes(chiffrer(octets))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun lireImage(ctx: Context, id: Long): ByteArray? {
        return try {
            val f = File(File(ctx.filesDir, DOSSIER_IMAGES), "acte_" + id + ".bin")
            if (!f.exists()) null else dechiffrer(f.readBytes())
        } catch (e: Exception) {
            null
        }
    }

    fun supprimerImage(ctx: Context, id: Long) {
        try {
            File(File(ctx.filesDir, DOSSIER_IMAGES), "acte_" + id + ".bin").delete()
        } catch (e: Exception) {
            // rien a faire
        }
    }

    // ------------------------------------------------------------ Fichiers chiffres generiques

    // Ecriture securisee : tmp -> relecture + comparaison -> bak -> renommage
    private fun ecrireFichier(ctx: Context, nom: String, octets: ByteArray): Boolean {
        return try {
            val tmp = File(ctx.filesDir, nom + ".tmp")
            val principal = File(ctx.filesDir, nom)
            val bak = File(ctx.filesDir, nom + ".bak")
            tmp.writeBytes(octets)
            if (!MessageDigest.isEqual(tmp.readBytes(), octets)) {
                tmp.delete()
                return false
            }
            bak.delete()
            if (principal.exists()) {
                if (!principal.renameTo(bak)) {
                    tmp.delete()
                    return false
                }
            }
            if (!tmp.renameTo(principal)) {
                if (bak.exists()) bak.renameTo(principal)
                tmp.delete()
                return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun lireFichier(ctx: Context, nom: String): ByteArray? {
        return try {
            val f = File(ctx.filesDir, nom)
            if (f.exists()) {
                f.readBytes()
            } else {
                val bak = File(ctx.filesDir, nom + ".bak")
                if (bak.exists()) bak.readBytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

