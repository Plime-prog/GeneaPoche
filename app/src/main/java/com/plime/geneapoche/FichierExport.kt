package com.plime.geneapoche

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.FileOutputStream

// v0.132 : ecriture FIABLE d'un export dans le fichier choisi par l'utilisateur.
//
// Le selecteur de fichiers cree le fichier (vide, 0 Ko) AVANT que l'application
// n'y ecrive. Toute sortie anticipee (erreur, annulation, flux refuse par le
// fournisseur de stockage) laissait donc un fichier de 0 Ko, parfois avec un
// message de reussite. Ici : tout est ecrit d'un coup, force sur disque (sync),
// la taille est verifiee, et le fichier vide est supprime en cas d'echec.
// v0.136 : taille affichee apres un export, pour reperer tout de suite un fichier vide.
fun tailleLisible(octets: Long): String =
    if (octets < 1024L) octets.toString() + " octets"
    else if (octets < 1024L * 1024L) ((octets + 512L) / 1024L).toString() + " Ko"
    else ((octets * 10L / 1048576L) / 10.0).toString().replace('.', ',') + " Mo"

object FichierExport {

    // Renvoie null si tout est ecrit et verifie, sinon un message d'erreur lisible.
    fun ecrire(ctx: Context, uri: Uri, octets: ByteArray): String? {
        var erreur: String? = null
        // 1) descripteur de fichier ("wt" = tronque), avec sync sur disque.
        try {
            val pfd = ctx.contentResolver.openFileDescriptor(uri, "wt")
            if (pfd != null) {
                pfd.use { d ->
                    FileOutputStream(d.fileDescriptor).use { sortie ->
                        sortie.write(octets)
                        sortie.flush()
                        try { sortie.fd.sync() } catch (e: Exception) { /* tube : pas de sync */ }
                    }
                }
                erreur = verifier(ctx, uri, octets.size.toLong())
                if (erreur == null) return null
            }
        } catch (e: Exception) {
            erreur = "Écriture impossible (" + (e.message ?: e.javaClass.simpleName) + ")."
        }
        // 2) repli : flux classique (certains fournisseurs refusent le descripteur).
        try {
            val flux = ctx.contentResolver.openOutputStream(uri, "wt")
                ?: ctx.contentResolver.openOutputStream(uri)
            if (flux == null) {
                return erreur ?: "Le stockage choisi refuse l'écriture du fichier."
            }
            flux.use { it.write(octets); it.flush() }
            return verifier(ctx, uri, octets.size.toLong())
        } catch (e: Exception) {
            return "Écriture impossible (" + (e.message ?: e.javaClass.simpleName) + ")."
        }
    }

    // La taille lue doit correspondre ; inconnue (-1) = on ne conclut pas.
    private fun verifier(ctx: Context, uri: Uri, attendu: Long): String? {
        val lu = try {
            ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (e: Exception) { -1L }
        return if (lu in 0L until attendu)
            "Fichier incomplet (" + lu + " octets écrits sur " + attendu + ") : réessayez, ou choisissez un autre emplacement (par exemple Téléchargements)."
        else null
    }

    // Supprime le fichier vide laisse par le selecteur apres un echec (au mieux).
    fun supprimerSiVide(ctx: Context, uri: Uri) {
        try {
            val taille = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            if (taille == 0L) DocumentsContract.deleteDocument(ctx.contentResolver, uri)
        } catch (e: Exception) { /* au mieux */ }
    }
}
