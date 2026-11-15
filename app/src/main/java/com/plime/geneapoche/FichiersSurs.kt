package com.plime.geneapoche

import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// v0.110 : logique de fichiers et d'enveloppe de cle EXTRAITE de Stockage.kt
// (sans aucune dependance Android) pour pouvoir etre testee sur JVM. Le
// comportement est celui de la v0.109, a l'identique ; Stockage ne fait plus
// que deleguer a ces fonctions.

// Ecriture, relecture et recuperation de fichiers dans un dossier.
object FichiersSurs {

    // G1-07 : ecriture durable sur disque (flush + fd.sync).
    fun ecrireDurable(f: File, octets: ByteArray): Boolean {
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

    // Ecriture securisee (G1-06) : ecriture durable du tmp, relecture et
    // comparaison, PUIS rotation bak ; l'ancien .bak n'est supprime qu'apres
    // ecriture ET relecture reussies du nouveau contenu. Une coupure entre
    // les deux renommages laisse toujours une copie lisible (.bak ou .tmp).
    fun ecrireSecurise(dossier: File, nom: String, octets: ByteArray): Boolean {
        return try {
            val tmp = File(dossier, nom + ".tmp")
            val principal = File(dossier, nom)
            val bak = File(dossier, nom + ".bak")
            if (!ecrireDurable(tmp, octets)) {
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

    // Contenu du fichier principal, sinon de la copie .bak, sinon null.
    fun lireFichier(dossier: File, nom: String): ByteArray? {
        return try {
            val f = File(dossier, nom)
            if (f.exists()) {
                f.readBytes()
            } else {
                val bak = File(dossier, nom + ".bak")
                if (bak.exists()) bak.readBytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // G1-05 : lecture TOUT OU RIEN - un element invalide ne produit JAMAIS une
    // liste partielle (le parse entier echoue, la liste reste vide et illisible).
    // G1-06 : si le fichier principal est corrompu (pas seulement absent), la
    // copie .bak est lue a la place. Ni principal ni .bak : liste vide, non
    // illisible. Les deux presents mais inutilisables : illisible = vrai.
    fun <T> lireToutOuRien(
        dossier: File,
        nom: String,
        dechiffrer: (ByteArray) -> ByteArray?,
        parse: (String) -> MutableList<T>
    ): Stockage.ResultatLecture<T> {
        val principal = File(dossier, nom)
        val bak = File(dossier, nom + ".bak")
        if (!principal.exists() && !bak.exists()) {
            return Stockage.ResultatLecture(nom, mutableListOf(), false)
        }
        for (f in listOf(principal, bak)) {
            if (!f.exists()) continue
            try {
                val clair = dechiffrer(f.readBytes())
                if (clair != null) {
                    val liste = try { parse(String(clair, Charsets.UTF_8)) } catch (e: Exception) { null }
                    if (liste != null) return Stockage.ResultatLecture(nom, liste, false)
                }
            } catch (e: Exception) {
                // fichier illisible : on tente la copie suivante
            }
        }
        return Stockage.ResultatLecture(nom, mutableListOf(), true)
    }

    // G1-04 : restauration explicite depuis la copie .bak (jamais de perte).
    fun restaurerBak(dossier: File, nom: String): Boolean {
        return try {
            val principal = File(dossier, nom)
            val bak = File(dossier, nom + ".bak")
            if (!bak.exists()) return false
            val contenu = bak.readBytes()
            val tmp = File(dossier, nom + ".tmp")
            if (!ecrireDurable(tmp, contenu)) return false
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
}

// Enveloppe de la cle maitre : PBKDF2(PIN) PUIS cle systeme (AndroidKeyStore
// en production). Chaque couche : [12 octets IV][chiffre + tag GCM].
object EnveloppeCle {

    fun hacherPin(pin: String, sel: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), sel, iterations, 256)
        val fabrique = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = fabrique.generateSecret(spec).encoded
        java.util.Arrays.fill(spec.password, ' ')
        return hash
    }

    fun envelopper(maitre: ByteArray, pin: String, sel: ByteArray, iterations: Int, cleSysteme: SecretKey): ByteArray {
        val kek = SecretKeySpec(hacherPin(pin, sel, iterations), "AES")
        val c1 = Cipher.getInstance("AES/GCM/NoPadding")
        c1.init(Cipher.ENCRYPT_MODE, kek)
        val blob = c1.iv + c1.doFinal(maitre)
        val c2 = Cipher.getInstance("AES/GCM/NoPadding")
        c2.init(Cipher.ENCRYPT_MODE, cleSysteme)
        return c2.iv + c2.doFinal(blob)
    }

    // null si le PIN est faux, si la cle systeme ne correspond pas, ou si
    // l'enveloppe est alteree (le tag GCM echoue : jamais de cle partielle).
    fun ouvrir(emb: ByteArray, pin: String, sel: ByteArray, iterations: Int, cleSysteme: SecretKey): ByteArray? {
        return try {
            val c2 = Cipher.getInstance("AES/GCM/NoPadding")
            c2.init(Cipher.DECRYPT_MODE, cleSysteme, GCMParameterSpec(128, emb, 0, 12))
            val blob = c2.doFinal(emb, 12, emb.size - 12)
            val kek = SecretKeySpec(hacherPin(pin, sel, iterations), "AES")
            val c1 = Cipher.getInstance("AES/GCM/NoPadding")
            c1.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, blob, 0, 12))
            c1.doFinal(blob, 12, blob.size - 12)
        } catch (e: Exception) {
            null
        }
    }
}
