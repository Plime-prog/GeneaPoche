package com.plime.geneapoche

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

// ---------------------------------------------------------------- Image a transcrire (v0.104)
// Decodage PRUDENT : dimensions lues d'abord, puis reduction pour que le
// plus grand cote ne depasse pas `cote` pixels (memoire bornee, pas de
// "bitmap trop grand" au dessin). A appeler HORS du thread principal.
fun decoderImageReduite(ctx: Context, uri: Uri, cote: Int = 3072): Bitmap? {
    return try {
        val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bornes) }
        val grand = maxOf(bornes.outWidth, bornes.outHeight)
        var ech = 1
        while (grand / ech > cote) ech *= 2
        var resultat: Bitmap? = null
        var essais = 0
        while (resultat == null && essais < 3) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = ech }
                resultat = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                if (resultat == null) return null
            } catch (e: OutOfMemoryError) {
                ech *= 2
                essais++
            }
        }
        resultat
    } catch (e: Exception) {
        null
    }
}

// Rapport d'incident LOCAL : la trace de l'arret brutal est conservee dans
// un fichier prive de l'application (aucune donnee genealogique) pour
// diagnostiquer un plantage au lancement suivant.
object Incidents {
    private const val NOM = "dernier_incident.txt"

    fun installer(ctx: Context) {
        val appCtx = ctx.applicationContext
        val ancien = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val trace = android.util.Log.getStackTraceString(e)
                val rt = Runtime.getRuntime()
                val texte = "Fil : " + t.name + "\nMemoire utilisee : " +
                    ((rt.totalMemory() - rt.freeMemory()) / 1048576L).toString() + " Mo / max " +
                    (rt.maxMemory() / 1048576L).toString() + " Mo\n\n" + trace.take(6000)
                java.io.File(appCtx.filesDir, NOM).writeText(texte)
            } catch (x: Throwable) {
            }
            ancien?.uncaughtException(t, e)
        }
    }

    fun lire(ctx: Context): String? = try {
        val f = java.io.File(ctx.filesDir, NOM)
        if (f.exists()) f.readText() else null
    } catch (e: Exception) {
        null
    }

    fun effacer(ctx: Context) {
        try {
            java.io.File(ctx.filesDir, NOM).delete()
        } catch (e: Exception) {
        }
    }
}
