package com.plime.geneapoche

import android.content.Context
import org.json.JSONArray

// Stockage local JSON chiffre plus tard (etape securite). Format simple et verifiable.

object Stockage {
    private const val FICHIER = "personnes.json"

    fun charger(ctx: Context): MutableList<Personne> {
        val liste = mutableListOf<Personne>()
        try {
            val flux = ctx.openFileInput(FICHIER)
            val octets = flux.readBytes()
            flux.close()
            val tableau = JSONArray(String(octets))
            for (i in 0 until tableau.length()) {
                liste.add(Personne.depuisJson(tableau.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // Premier lancement ou fichier illisible : base vide.
        }
        return liste
    }

    fun sauver(ctx: Context, liste: List<Personne>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            ctx.openFileOutput(FICHIER, Context.MODE_PRIVATE).use { sortie ->
                sortie.write(tableau.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
