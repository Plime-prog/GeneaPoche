package com.plime.geneapoche

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

// ------------------------------------------------ Base hors ligne des communes
// Chantier 12.4 (v0.88) : communes françaises du COG intégrées à l'APK
// (assets/communes : un fichier par département, lignes "code INSEE|nom").
// Saisie assistée : pendant la frappe d'un lieu, le champ propose les
// communes ; la sélection insère le format Heredis recommandé
// "[Paroisse/lieu-dit] – Commune, Code INSEE, Département, France"
// et remplit le champ "Département".

data class Commune(val code: String, val nom: String, val dept: String, val cle: String)

// Recherche sans accents ni casse.
private fun normaliserCle(s: String): String =
    Normalizer.normalize(s.uppercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

object BaseCommunes {
    @Volatile private var chargees = false
    private var liste: List<Commune> = emptyList()

    // La Corse (2A/2B) est absente de NomsDepartements.
    private val nomsCorse = linkedMapOf("2A" to "Corse-du-Sud", "2B" to "Haute-Corse")

    fun pret(): Boolean = chargees

    fun nomDept(dept: String): String =
        nomsCorse[dept] ?: NomsDepartements[dept.toIntOrNull() ?: -1] ?: ""

    // Numéro à écrire dans le champ "Département" : "01" -> "1", "2A" -> "2A".
    fun numeroDept(dept: String): String {
        val n = dept.toIntOrNull()
        return if (n != null) n.toString() else dept
    }

    // Charge une seule fois, en tâche de fond, les 101 fichiers du COG.
    suspend fun charger(ctx: Context) {
        if (chargees) return
        synchronized(this) {
            if (chargees) return
            val l = withContext(Dispatchers.IO) {
                val res = mutableListOf<Commune>()
                val noms = ctx.assets.list("communes") ?: arrayOf()
                for (f in noms.sorted()) {
                    if (!f.endsWith(".txt")) continue
                    val dept = f.removePrefix("dept_").removeSuffix(".txt")
                    ctx.assets.open("communes/" + f).bufferedReader(Charsets.UTF_8).use { r ->
                        var ligne = r.readLine()
                        while (ligne != null) {
                            val i = ligne.indexOf('|')
                            if (i > 0 && ligne.length > i + 1) {
                                val nom = ligne.substring(i + 1)
                                res.add(Commune(ligne.substring(0, i), nom, dept, normaliserCle(nom)))
                            }
                            ligne = r.readLine()
                        }
                    }
                }
                res
            }
            liste = l
            chargees = true
        }
    }

    // Suggestions : par code INSEE (ex "17010", "2A004"), puis par début
    // de nom, puis n'importe où dans le nom. Six au maximum.
    fun suggestions(q: String, max: Int = 6): List<Commune> {
        if (!chargees) return emptyList()
        val t = q.trim()
        if (t.length < 2) return emptyList()
        val n = normaliserCle(t)
        if (n.isEmpty()) return emptyList()
        if (Regex("^\\d{5}$").matches(n) || Regex("^2[AB]\\d{3}$").matches(n)) {
            val parCode = liste.filter { it.code == n || it.code.startsWith(n) }
            if (parCode.isNotEmpty()) return parCode.take(max)
        }
        val debut = liste.filter { it.cle.startsWith(n) }
        if (debut.size >= max) return debut.take(max)
        val ailleurs = liste.filter { it.cle.contains(n) && !debut.contains(it) }
        return (debut + ailleurs).take(max)
    }
}

// Partie "commune" en cours de frappe : ce qui suit le dernier
// séparateur (tiret long ou crochet fermant), le lieu-dit/paroisse
// saisi devant étant conservé.
fun partieSaisie(v: String): String {
    val f = v.indexOfLast { it == '\\u2013' }
    if (f >= 0) return v.substring(f + 1).trim()
    val b = v.indexOfLast { it == ']' }
    if (b >= 0) return v.substring(b + 1).trim()
    return v.trim()
}

// Insère la commune choisie au format Heredis, derrière le lieu-dit.
fun insererLieu(v: String, c: Commune): String {
    val f = v.indexOfLast { it == '\\u2013' }
    val b = v.indexOfLast { it == ']' }
    val prefixe = when {
        f >= 0 -> v.substring(0, f + 1).trim() + " "
        b >= 0 -> v.substring(0, b + 1) + " – "
        else -> ""
    }
    return prefixe + c.nom + ", " + c.code + ", " + BaseCommunes.nomDept(c.dept) + ", France"
}
