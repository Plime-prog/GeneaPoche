package com.plime.geneapoche

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

// v0.133 : numero de version REEL de l'application installee (versionName du
// build). Remplace le texte "Version 0.107" ecrit en dur, reste affiche depuis
// la v0.107 quelle que soit la version installee.
@Suppress("DEPRECATION")
fun versionApplication(ctx: Context): String = try {
    val info = if (Build.VERSION.SDK_INT >= 33) {
        ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0L))
    } else {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    }
    info.versionName ?: "?"
} catch (e: Exception) {
    "?"
}

// v0.150 : le Journal s'affiche au PREMIER lancement apres une installation, une
// mise a jour ou un changement de JOUR ; sinon l'application reprend le dernier
// ecran. jour = jours depuis 1970 (heure locale).
fun reprendreEcran(jourSauve: Long, versionSauvee: String, jourActuel: Long, versionActuelle: String): Boolean =
    jourSauve == jourActuel && versionSauvee == versionActuelle

fun jourCourant(): Long = java.time.LocalDate.now().toEpochDay()
