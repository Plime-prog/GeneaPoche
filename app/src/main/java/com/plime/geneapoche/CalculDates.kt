package com.plime.geneapoche

import java.text.Normalizer
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// ================================================================ v0.87
// Boite a outils - outil 2 : calculateur de dates (age, ecart entre deux
// evenements, date +/- une duree, intervalle d'annees).
// Logique PURE (aucune dependance Android) : testee en JVM.

fun sansAccents(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

private val MoisSansAccent = listOf(
    "janvier", "fevrier", "mars", "avril", "mai", "juin",
    "juillet", "aout", "septembre", "octobre", "novembre", "decembre"
)

// Numero (1..12) d'un mois ecrit en lettres, abrege ou non, accents ou non
// ("oct.", "Octobre", "fevr", "juil"). Un debut ambigu ("jui") est refuse.
fun moisGregorienEnLettres(mot: String): Int? {
    val t = sansAccents(mot).lowercase().replace(".", "").trim()
    if (t.length < 3) return null
    val exact = MoisSansAccent.indexOf(t)
    if (exact >= 0) return exact + 1
    val candidats = MoisSansAccent.withIndex().filter { it.value.startsWith(t) }
    return if (candidats.size == 1) candidats[0].index + 1 else null
}

private fun dateOuNull(an: Int, mois: Int, jour: Int): LocalDate? =
    try {
        LocalDate.of(an, mois, jour)
    } catch (e: DateTimeException) {
        null
    }

// "1er", "2e", "3\u00e8me" -> le jour seul.
private fun sansSuffixeJour(t: String): String =
    t.replace(Regex("^(\\d{1,2})\\s*(?:er|re|\u00e8re|e|\u00e8me)(?=\\s|$)", RegexOption.IGNORE_CASE), "$1")

// Lit une date COMPLETE : "05/10/1798", "5.10.1798", "5 octobre 1798",
// "1er mai 1843", ou date republicaine ("14 Vend\u00e9miaire an VII").
fun lireDate(s: String): LocalDate? {
    val t = sansSuffixeJour(s.trim())
    if (t.isEmpty()) return null
    Regex("^(\\d{1,2})[./\\- ](\\d{1,2})[./\\- ](\\d{4})$").find(t)?.let { m ->
        return dateOuNull(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
    }
    Regex("^(\\d{1,2})\\s+([\\p{L}.]+)\\s+(\\d{4})$").find(t)?.let { m ->
        val mois = moisGregorienEnLettres(m.groupValues[2])
        if (mois != null) {
            return dateOuNull(m.groupValues[3].toInt(), mois, m.groupValues[1].toInt())
        }
    }
    val g = repubAGregorien(sansAccents(t)) ?: return null
    return Regex("^(\\d{2})/(\\d{2})/(\\d{4})$").find(g)?.let { m ->
        dateOuNull(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
    }
}

// Date eventuellement incomplete : annee seule, mois et annee, ou date entiere.
data class DatePartielle(val annee: Int, val mois: Int?, val jour: Int?) {
    fun enLocalDate(): LocalDate = LocalDate.of(annee, mois ?: 1, jour ?: 1)
}

fun lireDatePartielle(s: String): DatePartielle? {
    val t = s.trim()
    if (t.isEmpty()) return null
    lireDate(t)?.let { return DatePartielle(it.year, it.monthValue, it.dayOfMonth) }
    Regex("^(\\d{4})$").find(t)?.let { return DatePartielle(it.groupValues[1].toInt(), null, null) }
    Regex("^(\\d{1,2})[./\\- ](\\d{4})$").find(t)?.let { m ->
        val mois = m.groupValues[1].toInt()
        return if (mois in 1..12) DatePartielle(m.groupValues[2].toInt(), mois, null) else null
    }
    Regex("^([\\p{L}.]+)\\s+(\\d{4})$").find(t)?.let { m ->
        val mois = moisGregorienEnLettres(m.groupValues[1]) ?: return null
        return DatePartielle(m.groupValues[2].toInt(), mois, null)
    }
    return null
}

// precision : "jour" (dates entieres), "mois" (mois et annee), "annee".
data class Duree(
    val annees: Int,
    val mois: Int,
    val jours: Int,
    val totalJours: Long?,
    val precision: String,
    val inverse: Boolean
)

private fun avant(a: DatePartielle, b: DatePartielle): Boolean =
    a.enLocalDate().isBefore(b.enLocalDate())

// Ecart de `depart` a `arrivee`. Si l'arrivee precede le depart, l'ecart est
// calcule a l'endroit et `inverse` vaut true (utile pour reperer une
// incoherence : un mariage avant la naissance, par exemple).
fun ecartDates(depart: DatePartielle, arrivee: DatePartielle): Duree {
    val inverse = avant(arrivee, depart)
    val tot = if (inverse) arrivee else depart
    val fin = if (inverse) depart else arrivee
    return when {
        tot.mois != null && tot.jour != null && fin.mois != null && fin.jour != null -> {
            val d1 = tot.enLocalDate()
            val d2 = fin.enLocalDate()
            val p = Period.between(d1, d2)
            Duree(p.years, p.months, p.days, ChronoUnit.DAYS.between(d1, d2), "jour", inverse)
        }
        tot.mois != null && fin.mois != null -> {
            val p = Period.between(
                YearMonth.of(tot.annee, tot.mois).atDay(1),
                YearMonth.of(fin.annee, fin.mois).atDay(1)
            )
            Duree(p.years, p.months, 0, null, "mois", inverse)
        }
        else -> Duree(fin.annee - tot.annee, 0, 0, null, "annee", inverse)
    }
}

private fun pluriel(n: Int, un: String, plusieurs: String): String =
    n.toString() + " " + (if (n > 1) plusieurs else un)

// "24 ans, 3 mois, 26 jours" ; les unites a zero sont omises.
fun libelleDuree(d: Duree): String {
    val parts = mutableListOf<String>()
    if (d.annees > 0) parts.add(pluriel(d.annees, "an", "ans"))
    if (d.mois > 0 && d.precision != "annee") parts.add(d.mois.toString() + " mois")
    if (d.jours > 0 && d.precision == "jour") parts.add(pluriel(d.jours, "jour", "jours"))
    if (parts.isNotEmpty()) return parts.joinToString(", ")
    return when (d.precision) {
        "jour" -> "0 jour"
        "mois" -> "0 mois"
        else -> "0 an"
    }
}

// Texte pret a coller dans une note :
// "De la naissance de Pierre (14/03/1842) au mariage (27/09/1867) : 25 ans, 6 mois, 13 jours (9 328 jours)".
fun texteEcart(depart: String, arrivee: String, d: Duree): String {
    val sb = StringBuilder()
    sb.append("De ").append(depart).append(" \u00e0 ").append(arrivee).append(" : ").append(libelleDuree(d))
    if (d.totalJours != null) sb.append(" (").append(separerMilliers(d.totalJours)).append(" jours)")
    if (d.precision != "jour") sb.append(" [dates incompl\u00e8tes : pr\u00e9cision " + (if (d.precision == "mois") "au mois" else "\u00e0 l'ann\u00e9e") + "]")
    if (d.inverse) sb.append(" - attention : la seconde date pr\u00e9c\u00e8de la premi\u00e8re")
    return sb.toString()
}

// 9336 -> "9 336".
fun separerMilliers(n: Long): String {
    val s = Math.abs(n).toString()
    val sb = StringBuilder()
    if (n < 0) sb.append('-')
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('\u00a0')
        sb.append(c)
    }
    return sb.toString()
}

// Date +/- une duree. signe = 1 (ajouter) ou -1 (retirer). Les mois sont
// ajoutes d'abord (31 janvier + 1 mois = 28 ou 29 fevrier), puis les jours.
fun ajouterDuree(date: LocalDate, signe: Int, annees: Int, mois: Int, jours: Int): LocalDate? =
    try {
        val s = if (signe < 0) -1L else 1L
        date.plusYears(s * annees).plusMonths(s * mois).plusDays(s * jours)
    } catch (e: DateTimeException) {
        null
    }
