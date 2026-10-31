package com.plime.geneapoche

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

// Stockage chiffre AES-256/GCM. Depuis la v0.29, les donnees sont chiffrees
// avec une CLE MAITRE aleatoire de 256 bits LIEE AU PIN : la cle maitre est
// chiffree par une cle derivee du PIN (PBKDF2-HmacSHA256), PUIS le tout par
// la cle AndroidKeyStore (jamais exportable). Sans le PIN, aucune donnee ne
// se dechiffre, meme en appelant Stockage directement ; la cle maitre n'est
// en memoire QUE pendant le deverrouillage.
// Fichiers chiffres : personnes.bin, journal.bin, actes.bin (+ .bak/.tmp)
// et images actes_img/*.bin. Migration v0.28 -> v0.29 automatique au premier
// deverrouillage (anciens fichiers conserves en .old apres verification).
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
    private const val CLE_ECRIT = "blocage_ecrit"
    private const val CLE_JUSQUA_MUR = "blocage_jusqua_mur"
    private const val CLE_ID_CUJUS = "id_cujus"
    private const val CLE_SOSA_CUJUS = "sosa_cujus"
    private const val CLE_MAITRE = "cle_maitre"
    private const val CLE_SEL_MAITRE = "cle_maitre_sel"
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

    // G1-14 : delai de reverrouillage reglable (0 = immediat, 60000, 300000).
    fun lireDelaiVerrouillage(ctx: Context): Long = prefs(ctx).getLong("delai_verrouillage", 0L)

    fun definirDelaiVerrouillage(ctx: Context, ms: Long) {
        prefs(ctx).edit().putLong("delai_verrouillage", ms).commit()
    }

    // ----------------------------------------- Cle maitre (liee au PIN, v0.29)

    // Cle maitre en memoire UNIQUEMENT pendant le deverrouillage : sans elle,
    // aucun chiffrage ni dechiffrage n'est possible.
    @Volatile
    private var cleMaitre: javax.crypto.SecretKey? = null

    fun deverrouilleStockage(): Boolean = cleMaitre != null

    fun verrouillerStockage() { cleMaitre = null }

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

    // Chiffre avec la cle maitre (en memoire) : null si app verrouillee.
    private fun chiffrer(octets: ByteArray): ByteArray? {
        val m = cleMaitre ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, m)
            // Format : [12 octets IV][donnees chiffrees + tag GCM]
            cipher.iv + cipher.doFinal(octets)
        } catch (e: Exception) {
            null
        }
    }

    // Dechiffre avec la cle maitre : null si verrouille ou donnees invalides.
    private fun dechiffrer(donnees: ByteArray): ByteArray? {
        if (donnees.size < 13) return null
        val m = cleMaitre ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, m, GCMParameterSpec(128, donnees, 0, 12))
            cipher.doFinal(donnees, 12, donnees.size - 12)
        } catch (e: Exception) {
            null
        }
    }

    // Dechiffre avec l'ancienne cle AndroidKeyStore seule (migration v0.28).
    private fun dechiffrerAncien(donnees: ByteArray): ByteArray? {
        if (donnees.size < 13) return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, cle(), GCMParameterSpec(128, donnees, 0, 12))
            cipher.doFinal(donnees, 12, donnees.size - 12)
        } catch (e: Exception) {
            null
        }
    }

    // Enveloppe de la cle maitre : PBKDF2(PIN) PUIS AndroidKeyStore.
    // Chaque couche : [12 octets IV][chiffre].
    private fun envelopperCleMaitre(ctx: Context, pin: String, maitre: javax.crypto.SecretKey): Boolean {
        return try {
            val sel = ByteArray(16)
            SecureRandom().nextBytes(sel)
            val kek = javax.crypto.spec.SecretKeySpec(hacherPin(pin, sel), "AES")
            val c1 = Cipher.getInstance("AES/GCM/NoPadding")
            c1.init(Cipher.ENCRYPT_MODE, kek)
            val blob = c1.iv + c1.doFinal(maitre.encoded)
            val c2 = Cipher.getInstance("AES/GCM/NoPadding")
            c2.init(Cipher.ENCRYPT_MODE, cle())
            val emb = c2.iv + c2.doFinal(blob)
            prefs(ctx).edit()
                .putString(CLE_SEL_MAITRE, Base64.encodeToString(sel, Base64.NO_WRAP))
                .putString(CLE_MAITRE, Base64.encodeToString(emb, Base64.NO_WRAP))
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    private fun desenvelopperCleMaitre(ctx: Context, pin: String): javax.crypto.SecretKey? {
        return try {
            val p = prefs(ctx)
            val emb64 = p.getString(CLE_MAITRE, null) ?: return null
            val sel64 = p.getString(CLE_SEL_MAITRE, null) ?: return null
            val emb = Base64.decode(emb64, Base64.NO_WRAP)
            val c2 = Cipher.getInstance("AES/GCM/NoPadding")
            c2.init(Cipher.DECRYPT_MODE, cle(), GCMParameterSpec(128, emb, 0, 12))
            val blob = c2.doFinal(emb, 12, emb.size - 12)
            val kek = javax.crypto.spec.SecretKeySpec(hacherPin(pin, Base64.decode(sel64, Base64.NO_WRAP)), "AES")
            val c1 = Cipher.getInstance("AES/GCM/NoPadding")
            c1.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, blob, 0, 12))
            return javax.crypto.spec.SecretKeySpec(c1.doFinal(blob, 12, blob.size - 12), "AES")
        } catch (e: Exception) {
            null
        }
    }

    // Migration v0.28 -> v0.29 : les fichiers etaient chiffres avec la seule
    // cle AndroidKeyStore. Re-chiffrement avec la cle maitre, VERIFICATION par
    // relecture + dechiffrage, anciens fichiers conserves en .old.
    private fun migrerVersCleMaitre(ctx: Context) {
        listOf(FICHIER_PERSONNES, FICHIER_JOURNAL, FICHIER_ACTES).forEach { nom ->
            try {
                val f = File(ctx.filesDir, nom)
                if (!f.exists()) return@forEach
                val anciens = f.readBytes()
                val clair = dechiffrerAncien(anciens) ?: return@forEach
                val nouveau = chiffrer(clair) ?: return@forEach
                if (!ecrireFichier(ctx, nom, nouveau)) return@forEach
                val relu = lireFichier(ctx, nom) ?: return@forEach
                if (MessageDigest.isEqual(relu, nouveau) && dechiffrer(relu) != null) {
                    File(ctx.filesDir, nom + ".old").writeBytes(anciens)
                }
            } catch (e: Exception) {
                // fichier suivant
            }
        }
        // Images chiffrees : meme re-chiffrement, une par une, avec .old.
        try {
            val dossier = File(ctx.filesDir, DOSSIER_IMAGES)
            dossier.listFiles()?.forEach { f ->
                try {
                    val anciens = f.readBytes()
                    val clair = dechiffrerAncien(anciens) ?: return@forEach
                    val nouveau = chiffrer(clair) ?: return@forEach
                    val tmp = File(dossier, f.name + ".tmp")
                    tmp.writeBytes(nouveau)
                    if (MessageDigest.isEqual(tmp.readBytes(), nouveau) && dechiffrer(tmp.readBytes()) != null) {
                        File(dossier, f.name + ".old").writeBytes(anciens)
                        if (!tmp.renameTo(f)) tmp.delete()
                    } else {
                        tmp.delete()
                    }
                } catch (e: Exception) {
                    // image suivante
                }
            }
        } catch (e: Exception) {
            // pas de dossier images
        }
    }

    // ------------------------------------------------------------ PIN

    fun aPin(ctx: Context): Boolean = prefs(ctx).contains(CLE_HASH)

    fun definirPin(ctx: Context, pin: String): Boolean {
        // Cle maitre aleatoire de 256 bits, liee au PIN des la creation (v0.29).
        val octets = ByteArray(32)
        SecureRandom().nextBytes(octets)
        val maitre = javax.crypto.spec.SecretKeySpec(octets, "AES")
        val sel = ByteArray(16)
        SecureRandom().nextBytes(sel)
        val hash = hacherPin(pin, sel)
        val ok = prefs(ctx).edit()
            .putString(CLE_SEL, Base64.encodeToString(sel, Base64.NO_WRAP))
            .putString(CLE_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(CLE_ERREURS, 0)
            .putInt(CLE_NIVEAU, 0)
            .putLong(CLE_JUSQUA, 0L)
            .commit()
        if (!ok || !envelopperCleMaitre(ctx, pin, maitre)) return false
        cleMaitre = maitre
        migrerVersCleMaitre(ctx)
        return true
    }

    // Deverrouillage : verification du PIN PUIS ouverture de la cle maitre.
    // Premier lancement depuis la v0.28 : la cle maitre n'existe pas encore,
    // on la cree et on migre les fichiers chiffres avec l'ancienne cle.
    fun deverrouiller(ctx: Context, pin: String): Boolean {
        if (!verifierPin(ctx, pin)) return false
        val maitre = desenvelopperCleMaitre(ctx, pin)
        if (maitre != null) {
            cleMaitre = maitre
            return true
        }
        val octets = ByteArray(32)
        SecureRandom().nextBytes(octets)
        val nouvelle = javax.crypto.spec.SecretKeySpec(octets, "AES")
        cleMaitre = nouvelle
        migrerVersCleMaitre(ctx)
        if (!envelopperCleMaitre(ctx, pin, nouvelle)) {
            cleMaitre = null
            return false
        }
        return true
    }

    fun verifierPin(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        val hashStocke = p.getString(CLE_HASH, null) ?: return false
        val selStocke = p.getString(CLE_SEL, null) ?: return false
        val calcule = hacherPin(pin, Base64.decode(selStocke, Base64.NO_WRAP))
        return MessageDigest.isEqual(calcule, Base64.decode(hashStocke, Base64.NO_WRAP))
    }

    // G1-15 : changement de PIN. L'ancien PIN est exige ; la cle maitre est
    // re-enveloppee avec le nouveau PIN, les donnees restent intactes.
    fun changerPin(ctx: Context, ancien: String, nouveau: String): Boolean {
        if (!verifierPin(ctx, ancien)) return false
        val m = cleMaitre ?: return false
        val sel = ByteArray(16)
        SecureRandom().nextBytes(sel)
        val hash = hacherPin(nouveau, sel)
        val ok = prefs(ctx).edit()
            .putString(CLE_SEL, Base64.encodeToString(sel, Base64.NO_WRAP))
            .putString(CLE_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .commit()
        if (!ok) return false
        return envelopperCleMaitre(ctx, nouveau, m)
    }

    private fun hacherPin(pin: String, sel: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), sel, ITERATIONS_PIN, 256)
        val fabrique = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = fabrique.generateSecret(spec).encoded
        java.util.Arrays.fill(spec.password, ' ')
        return hash
    }

    // ------------------------------------------------------------ Blocage apres erreurs

    // G1-11 : blocage sur l'horloge elapsedRealtime (immunise contre tout
    // changement de l'heure du telephone), avec repli sur l'horloge murale
    // apres un redemarrage. L'echec est enregistre de facon SYNCHRONE (commit)
    // pour etre deja pris en compte au verification suivante du PIN.
    fun enregistrerEchec(ctx: Context) {
        val p = prefs(ctx)
        var erreurs = p.getInt(CLE_ERREURS, 0) + 1
        val niveau = p.getInt(CLE_NIVEAU, 0)
        if (erreurs >= ERREURS_AVANT_PALIER) {
            erreurs = 0
            val duree = if (niveau < DUREES_BLOCAGE.size) DUREES_BLOCAGE[niveau] else DUREES_BLOCAGE.last()
            val e = android.os.SystemClock.elapsedRealtime()
            p.edit()
                .putLong(CLE_ECRIT, e)
                .putLong(CLE_JUSQUA, e + duree)
                .putLong(CLE_JUSQUA_MUR, System.currentTimeMillis() + duree)
                .putInt(CLE_NIVEAU, if (niveau < DUREES_BLOCAGE.size) niveau + 1 else niveau)
                .commit()
        }
        p.edit().putInt(CLE_ERREURS, erreurs).commit()
    }

    fun reinitialiserEchecs(ctx: Context) {
        prefs(ctx).edit()
            .putInt(CLE_ERREURS, 0)
            .putInt(CLE_NIVEAU, 0)
            .putLong(CLE_JUSQUA, 0L)
            .apply()
    }

    fun secondesBlocage(ctx: Context): Long {
        val p = prefs(ctx)
        val e = android.os.SystemClock.elapsedRealtime()
        val reste = if (e >= p.getLong(CLE_ECRIT, 0L)) {
            (p.getLong(CLE_JUSQUA, 0L) - e) / 1000L
        } else {
            // Redemarrage du telephone : repli sur l'horloge murale.
            (p.getLong(CLE_JUSQUA_MUR, 0L) - System.currentTimeMillis()) / 1000L
        }
        return if (reste > 0L) reste else 0L
    }

    // ------------------------------------------------------------ Personnes (chiffre)

    fun charger(ctx: Context): ResultatLecture<Personne> {
        // Migration : ancien fichier en clair de la v0.1/v0.2 -> chiffre puis supprime
        try {
            val legacy = File(ctx.filesDir, ANCIEN_FICHIER_CLAIR)
            if (legacy.exists()) {
                val liste = mutableListOf<Personne>()
                ajouterPersonnesDepuisTexte(liste, String(legacy.readBytes()))
                if (sauver(ctx, liste)) legacy.delete()
                return ResultatLecture(FICHIER_PERSONNES, liste, false)
            }
        } catch (e: Exception) {
            // on continue sur la lecture chiffree
        }
        return lireChiffreToutOuRien(ctx, FICHIER_PERSONNES) { texte ->
            val tableau = JSONArray(texte)
            val liste = mutableListOf<Personne>()
            for (i in 0 until tableau.length()) liste.add(Personne.depuisJson(tableau.getJSONObject(i)))
            liste
        }
    }

    fun sauver(ctx: Context, liste: List<Personne>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            val chiffre = chiffrer(tableau.toString().toByteArray()) ?: return false
            ecrireFichier(ctx, FICHIER_PERSONNES, chiffre)
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

    fun chargerJournal(ctx: Context): ResultatLecture<Piste> =
        lireChiffreToutOuRien(ctx, FICHIER_JOURNAL) { texte ->
            val tableau = JSONArray(texte)
            val liste = mutableListOf<Piste>()
            for (i in 0 until tableau.length()) liste.add(Piste.depuisJson(tableau.getJSONObject(i)))
            liste
        }

    fun sauverJournal(ctx: Context, liste: List<Piste>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            val chiffre = chiffrer(tableau.toString().toByteArray()) ?: return false
            ecrireFichier(ctx, FICHIER_JOURNAL, chiffre)
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------ Actes (index chiffre + images chiffrees)

    fun chargerActes(ctx: Context): ResultatLecture<Acte> =
        lireChiffreToutOuRien(ctx, FICHIER_ACTES) { texte ->
            val tableau = JSONArray(texte)
            val liste = mutableListOf<Acte>()
            for (i in 0 until tableau.length()) liste.add(Acte.depuisJson(tableau.getJSONObject(i)))
            liste
        }

    fun sauverActes(ctx: Context, liste: List<Acte>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            val chiffre = chiffrer(tableau.toString().toByteArray()) ?: return false
            ecrireFichier(ctx, FICHIER_ACTES, chiffre)
        } catch (e: Exception) {
            false
        }
    }

    // Images chiffrees, une par acte : actes_img/acte_<id>.bin
    fun sauverImage(ctx: Context, id: Long, octets: ByteArray): Boolean {
        return try {
            val dossier = File(ctx.filesDir, DOSSIER_IMAGES)
            if (!dossier.exists()) dossier.mkdirs()
            val chiffre = chiffrer(octets) ?: return false
            remplacerFichier(File(dossier, "acte_" + id + ".bin"), chiffre)
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

    // G1-09 : sauvegarde automatique avant un import GEDCOM. Copies chiffrees
    // (cle maitre) des trois fichiers, restaurables tant que le PIN est inchange.
    fun sauvegardeAvantImport(ctx: Context): Boolean {
        return try {
            var ok = true
            listOf(FICHIER_PERSONNES, FICHIER_JOURNAL, FICHIER_ACTES).forEach { nom ->
                val src = File(ctx.filesDir, nom)
                if (src.exists()) {
                    val dst = File(ctx.filesDir, nom + ".importbak")
                    if (dst.exists()) dst.delete()
                    if (!src.renameTo(dst)) ok = false
                }
            }
            ok
        } catch (e: Exception) {
            false
        }
    }

    fun aSauvegardeImport(ctx: Context): Boolean =
        File(ctx.filesDir, FICHIER_PERSONNES + ".importbak").exists()

    fun restaurerSauvegardeImport(ctx: Context): Boolean {
        return try {
            var ok = true
            listOf(FICHIER_PERSONNES, FICHIER_JOURNAL, FICHIER_ACTES).forEach { nom ->
                val bak = File(ctx.filesDir, nom + ".importbak")
                if (bak.exists()) {
                    val principal = File(ctx.filesDir, nom)
                    if (principal.exists()) principal.delete()
                    if (!bak.renameTo(principal)) ok = false
                }
            }
            ok
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------ Fichiers chiffres generiques

    // Resultat d'une lecture chiffree : illisible = vrai si NI le fichier
    // principal NI la copie .bak n'ont pu etre dechiffres/parses (G1-04/G1-05).
    class ResultatLecture<T>(val nom: String, val liste: MutableList<T>, val illisible: Boolean)

    // G1-05 : lecture TOUT OU RIEN - un element invalide ne produit JAMAIS une
    // liste partielle (le parse entier echoue, la liste reste vide et illisible).
    // G1-06 : si le fichier principal est corrompu (pas seulement absent), la
    // copie .bak est lue a la place.
    private fun <T> lireChiffreToutOuRien(ctx: Context, nom: String, parse: (String) -> MutableList<T>): ResultatLecture<T> {
        val principal = File(ctx.filesDir, nom)
        val bak = File(ctx.filesDir, nom + ".bak")
        if (principal.exists()) {
            try {
                val octets = principal.readBytes()
                val clair = dechiffrer(octets)
                if (clair != null) {
                    val liste = try { parse(String(clair)) } catch (e: Exception) { null }
                    if (liste != null) return ResultatLecture(nom, liste, false)
                }
            } catch (e: Exception) {
                // principal illisible : on tente la copie .bak
            }
            if (bak.exists()) {
                try {
                    val octets = bak.readBytes()
                    val clair = dechiffrer(octets)
                    if (clair != null) {
                        val liste = try { parse(String(clair)) } catch (e: Exception) { null }
                        if (liste != null) return ResultatLecture(nom, liste, false)
                    }
                } catch (e: Exception) {
                    // .bak illisible aussi
                }
            }
            return ResultatLecture(nom, mutableListOf(), true)
        }
        if (bak.exists()) {
            try {
                val octets = bak.readBytes()
                val clair = dechiffrer(octets)
                if (clair != null) {
                    val liste = try { parse(String(clair)) } catch (e: Exception) { null }
                    if (liste != null) return ResultatLecture(nom, liste, false)
                }
            } catch (e: Exception) {
                // .bak illisible
            }
            return ResultatLecture(nom, mutableListOf(), true)
        }
        return ResultatLecture(nom, mutableListOf(), false)
    }

    // G1-04 : restauration explicite depuis la copie .bak (jamais de perte).
    fun restaurerBak(ctx: Context, nom: String): Boolean {
        return try {
            val principal = File(ctx.filesDir, nom)
            val bak = File(ctx.filesDir, nom + ".bak")
            if (!bak.exists()) return false
            val contenu = bak.readBytes()
            val tmp = File(ctx.filesDir, nom + ".tmp")
            if (!ecrireOctetsDurables(tmp, contenu)) return false
            if (!MessageDigest.isEqual(tmp.readBytes(), contenu)) {
                tmp.delete()
                return false
            }
            principal.delete()
            if (!tmp.renameTo(principal)) {
                tmp.delete()
                return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // G1-07 : ecriture durable sur disque (flush + fd.sync).
    private fun ecrireOctetsDurables(f: File, octets: ByteArray): Boolean {
        return try {
            FileOutputStream(f).use { fos ->
                fos.write(octets)
                fos.flush()
                fos.fd.sync()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // G1-07 : remplacement atomique par fichier temporaire + verification.
    private fun remplacerFichier(f: File, octets: ByteArray): Boolean {
        val tmp = File(f.parentFile, f.name + ".tmp")
        if (!ecrireOctetsDurables(tmp, octets)) return false
        if (!MessageDigest.isEqual(tmp.readBytes(), octets)) {
            tmp.delete()
            return false
        }
        if (!tmp.renameTo(f)) {
            tmp.delete()
            return false
        }
        return true
    }

    // Ecriture securisee (G1-06) : ecriture durable du tmp, relecture et
    // comparaison, PUIS rotation bak ; l'ancien .bak n'est supprime qu'apres
    // ecriture ET relecture reussies du nouveau contenu. Une coupure entre
    // les deux renommages laisse toujours une copie lisible (.bak ou .tmp).
    private fun ecrireFichier(ctx: Context, nom: String, octets: ByteArray): Boolean {
        return try {
            val tmp = File(ctx.filesDir, nom + ".tmp")
            val principal = File(ctx.filesDir, nom)
            val bak = File(ctx.filesDir, nom + ".bak")
            if (!ecrireOctetsDurables(tmp, octets)) {
                tmp.delete()
                return false
            }
            if (!MessageDigest.isEqual(tmp.readBytes(), octets)) {
                tmp.delete()
                return false
            }
            if (principal.exists()) {
                if (!bak.delete() && bak.exists()) {
                    tmp.delete()
                    return false
                }
                if (!principal.renameTo(bak)) {
                    tmp.delete()
                    return false
                }
            } else {
                bak.delete()
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

