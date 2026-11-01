package com.plime.geneapoche

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// G2-20 : SimpleDateFormat statique n est pas sur entre threads ; remplace
// par DateTimeFormatter (immuable, sur partout).
private val formatJour: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.FRANCE)
private val formatJourHeure: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withLocale(Locale.FRANCE)

fun formaterDate(ts: Long): String =
    if (ts > 0L) formatJour.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())) else ""

fun formaterDateHeure(ts: Long): String =
    if (ts > 0L) formatJourHeure.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())) else ""

// Formats d'affichage partout dans l'application :
// dates "jj/mm/aaaa", heures "14h05" (les valeurs saisies restent libres).
fun dateAffichee(s: String): String {
    val t = s.trim()
    Regex("^(\\d{1,2})[./\\- ](\\d{1,2})[./\\- ](\\d{4})$").find(t)?.let { m ->
        return m.groupValues[1].padStart(2, '0') + "/" + m.groupValues[2].padStart(2, '0') + "/" + m.groupValues[3]
    }
    // Date du calendrier republicain : affichee telle quelle, completee de sa
    // conversion gregorienne entre parentheses.
    repubAGregorien(t)?.let { return t + " (" + it + ")" }
    return t
}

fun heureAffichee(s: String): String {
    val t = s.trim()
    Regex("^(\\d{1,2})[:hH](\\d{2})$").find(t)?.let { m ->
        return m.groupValues[1].padStart(2, '0') + ":" + m.groupValues[2]
    }
    return t
}

// Format unique impose : jj/mm/aaaa (ou, a defaut, l'annee seule aaaa),
// ou une date du calendrier republicain (ex "22 Germinal An II"), gardee
// telle quelle et completee de sa conversion gregorienne a l'affichage.
fun dateValide(s: String): Boolean {
    val t = s.trim()
    if (t.isEmpty()) return true
    Regex("^(\\d{2})/(\\d{2})/(\\d{4})$").find(t)?.let { m ->
        val j = m.groupValues[1].toInt()
        val mois = m.groupValues[2].toInt()
        // G2-01 : date reelle verifiee avec java.time.LocalDate (31/02,
        // 29/02 d une annee non bissextile, jour ou mois 00 refuses).
        return try {
            java.time.LocalDate.of(m.groupValues[3].toInt(), mois, j)
            true
        } catch (e: java.time.DateTimeException) {
            false
        }
    }
    if (Regex("^\\d{4}$").matches(t)) return true
    return repubAGregorien(t) != null
}

// Heure au format unique hh:mm (00 a 23, 00 a 59).
fun heureValide(s: String): Boolean {
    val t = s.trim()
    if (t.isEmpty()) return true
    val m = Regex("^(\\d{1,2}):(\\d{2})$").find(t) ?: return false
    return m.groupValues[1].toInt() in 0..23 && m.groupValues[2].toInt() in 0..59
}

// G2-05 : chaque echec affiche un message DISTINCT : le TYPE de l'exception
// est traduit en fran\u00E7ais, sans jamais reveler de donnee personnelle.
fun messageErreur(e: Exception, defaut: String): String = when (e) {
    is java.io.FileNotFoundException -> "Fichier introuvable."
    is SecurityException -> "Acc\u00E8s refus\u00E9 au fichier."
    is java.io.IOException -> "Erreur de lecture ou d'\u00E9criture du fichier."
    is org.json.JSONException -> "Donn\u00E9es illisibles : fichier corrompu ?"
    else -> defaut
}
