package com.plime.geneapoche

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

// ------------------------------------------------ Base hors ligne des communes
// Chantier 12.4 (v0.88) : communes françaises du COG intégrées à l'APK
// (assets/communes : un fichier par département, lignes "code INSEE|nom").
// Saisie assistée : pendant la frappe d'un lieu, le champ propose les
// communes ; la sélection insère le format interne
// "[Paroisse/lieu-dit], Commune, Code INSEE, Département" (v0.90 : la
// virgule sépare aussi le lieu-dit de la commune, et "France" n'est plus
// stocké : il est ajouté uniquement à l'export GEDCOM) et remplit le
// champ "Département".

data class Commune(val code: String, val nom: String, val dept: String, val cle: String)

// Recherche sans accents ni casse ni séparateurs : "Saint Denis",
// "Saint-Denis" et "saintdenis" trouvent tous la commune "Saint-Denis".
private fun normaliserCle(s: String): String =
    Normalizer.normalize(s.uppercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^A-Z0-9]"), "")

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
        if (!chargees) {
            liste = l
            chargees = true
        }
    }

    // Suggestions : par code INSEE (ex "17010", "2A004"), puis par nom
    // (egalite exacte d'abord, debut de nom, puis n'importe ou). Huit
    // au maximum, pour retrouver aussi les homonymes ("Saint-Denis"
    // 93 et 974 cote a cote).
    fun suggestions(q: String, max: Int = 8): List<Commune> {
        if (!chargees) return emptyList()
        val t = q.trim()
        if (t.length < 2) return emptyList()
        val n = normaliserCle(t)
        if (n.isEmpty()) return emptyList()
        if (Regex("^\\d{5}$").matches(n) || Regex("^2[AB]\\d{3}$").matches(n)) {
            val parCode = liste.filter { it.code == n || it.code.startsWith(n) }
            if (parCode.isNotEmpty()) return parCode.take(max)
        }
        val exactes = liste.filter { it.cle == n }
        val debut = liste.filter { it.cle.startsWith(n) && it.cle != n }
        val ailleurs = liste.filter { it.cle.contains(n) && !it.cle.startsWith(n) }
        return (exactes + debut + ailleurs).take(max)
    }
}

// Partie "commune" en cours de frappe : ce qui suit le dernier
// séparateur (virgule, tiret long ou crochet fermant), le lieu-dit/
// paroisse saisi devant étant conservé.
fun partieSaisie(v: String): String {
    val c = v.indexOfLast { it == ',' }
    if (c >= 0) return v.substring(c + 1).trim()
    val f = v.indexOfLast { it == '\u2013' }
    if (f >= 0) return v.substring(f + 1).trim()
    val b = v.indexOfLast { it == ']' }
    if (b >= 0) return v.substring(b + 1).trim()
    return v.trim()
}

// Insère la commune choisie au format interne, derrière le lieu-dit.
fun insererLieu(v: String, c: Commune): String {
    val co = v.indexOfLast { it == ',' }
    val f = v.indexOfLast { it == '\u2013' }
    val b = v.indexOfLast { it == ']' }
    val prefixe = when {
        co >= 0 -> v.substring(0, co + 1).trim() + " "
        f >= 0 -> v.substring(0, f + 1).trim() + " "
        b >= 0 -> v.substring(0, b + 1) + " \u2013 "
        else -> ""
    }
    return prefixe + c.nom + ", " + c.code + ", " + BaseCommunes.nomDept(c.dept)
}

// v0.105 : le lieu porte deja une commune CHOISIE (format interne
// "Commune, code INSEE, Departement") : la liste de suggestions se replie.
fun lieuDejaChoisi(v: String): Boolean =
    Regex(", (\\d{5}|2[AB]\\d{3}), [^,]+$").containsMatchIn(v.trim())
