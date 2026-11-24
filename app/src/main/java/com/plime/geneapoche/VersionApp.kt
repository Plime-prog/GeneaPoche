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

fun jourCourant(): Long = java.time.LocalDate.now().toEpochDay()
