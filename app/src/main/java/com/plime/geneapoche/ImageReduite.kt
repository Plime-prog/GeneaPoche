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

// v0.148 : DIAGNOSTIC des blocages ("l'application ne repond pas").
// Tout reste LOCAL : fichier prive, noms de fonctions uniquement, aucune donnee
// genealogique, aucune connexion.

// Extrait le bloc du fil principal ("main") d'une trace ANR complete.
fun extraireFilPrincipal(trace: String, maxLignes: Int = 40): String {
    val lignes = trace.lines()
    val debut = lignes.indexOfFirst { it.startsWith("\"main\"") }
    if (debut < 0) return ""
    val bloc = ArrayList<String>()
    for (i in debut until lignes.size) {
        if (i > debut && lignes[i].isBlank()) break
        bloc.add(lignes[i])
        if (bloc.size >= maxLignes) break
    }
    return bloc.joinToString("\n")
}

object DiagnosticBlocage {
    private const val PREFS = "diagnostic_blocage"

    // Au demarrage : si Android a note un ANR dans l'historique de fermetures
    // (Android 11+), copie la pile du fil principal dans "Dernier incident".
    fun lireHistoriqueAnr(ctx: Context) {
        if (android.os.Build.VERSION.SDK_INT < 30) return
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val dejaVu = prefs.getLong("dernier_anr", 0L)
            val anr = am.getHistoricalProcessExitReasons(ctx.packageName, 0, 10)
                .filter { it.reason == android.app.ApplicationExitInfo.REASON_ANR && it.timestamp > dejaVu }
                .maxByOrNull { it.timestamp } ?: return
            val trace = anr.traceInputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val fil = extraireFilPrincipal(trace)
            val texte = "BLOCAGE Android (ANR) du " +
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date(anr.timestamp)) +
                "\nDescription : " + (anr.description ?: "-") + "\n\n" +
                (if (fil.isEmpty()) "(pile du fil principal non fournie par Android)" else fil)
            java.io.File(ctx.filesDir, "dernier_incident.txt").writeText(texte.take(6000))
            prefs.edit().putLong("dernier_anr", anr.timestamp).apply()
        } catch (e: Throwable) {
        }
    }

    @Volatile private var actif = false
    fun definirActif(v: Boolean) { actif = v }

    // Sentinelle : si le fil principal ne repond plus pendant plus de 4 s alors que
    // l'application est a l'ecran, sa pile est notee (une fois par blocage).
    fun demarrerSentinelle(ctx: Context) {
        val appCtx = ctx.applicationContext
        val principal = android.os.Looper.getMainLooper()
        val h = android.os.Handler(principal)
        val dernier = java.util.concurrent.atomic.AtomicLong(System.nanoTime())
        val t = Thread {
            var note = false
            while (true) {
                try { Thread.sleep(1000) } catch (e: InterruptedException) { return@Thread }
                if (!actif) { dernier.set(System.nanoTime()); note = false; continue }
                h.post { dernier.set(System.nanoTime()) }
                val attente = (System.nanoTime() - dernier.get()) / 1_000_000L
                if (attente > 4000L && !note) {
                    note = true
                    try {
                        val pile = principal.thread.stackTrace.take(40).joinToString("\n") { "  at " + it }
                        java.io.File(appCtx.filesDir, "dernier_incident.txt").writeText(
                            "FIGE (ecran principal bloque plus de 4 s)\n\n" + pile
                        )
                    } catch (e: Throwable) {
                    }
                } else if (attente < 1500L) note = false
            }
        }
        t.isDaemon = true
        t.name = "sentinelle-blocage"
        t.start()
    }
}
