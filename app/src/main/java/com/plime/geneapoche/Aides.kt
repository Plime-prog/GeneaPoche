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


// ================================================================ G\u00E9n\u00E9aPoche 3
// Compteurs de renseignements, couleur rouge-\u00E0-vert, p\u00E9riode r\u00E9aliste
// et contr\u00F4les de coh\u00E9rence des dates.

// Date COMPL\u00C8TE : jour + mois + ann\u00E9e (ou date r\u00E9publicaine, convertie
// \u00E0 l'affichage). Une ann\u00E9e seule (aaaa) est INCOMPL\u00C8TE.
fun dateComplete(s: String): Boolean {
    val t = s.trim()
    if (t.isEmpty()) return false
    if (Regex("^(\\d{1,2})[./\\- ](\\d{1,2})[./\\- ](\\d{4})$").matches(t)) return true
    return repubAGregorien(t) != null
}

// D\u00E9partement IDENTIFIABLE : num\u00E9ro saisi, nom de d\u00E9partement, ou
// d\u00E9partement contenu dans le libell\u00E9 du lieu.
fun deptIdentifiable(dept: String, lieu: String): Int? =
    dept.trim().toIntOrNull() ?: numeroDepartement(dept.trim()) ?: numeroDepartement(lieu.trim())

// Lieu COMPLET : commune renseign\u00E9e ET d\u00E9partement identifiable.
fun lieuComplet(lieu: String, dept: String): Boolean =
    lieu.trim().isNotEmpty() && deptIdentifiable(dept, lieu) != null

// Compteur n/4 de la liste Personnes : date et lieu complets de
// NAISSANCE et de D\u00C9C\u00C8S (une ann\u00E9e seule ne compte pas).
fun renseignementsN4(p: Personne): Int {
    var n = 0
    if (dateComplete(p.dateNaissance)) n++
    if (lieuComplet(p.lieuNaissance, p.deptNaissance)) n++
    if (dateComplete(p.dateDeces)) n++
    if (lieuComplet(p.lieuDeces, p.deptDeces)) n++
    return n
}

// Compteur n/6 de la fiche : date, commune et d\u00E9partement de la
// naissance, puis du d\u00E9c\u00E8s.
fun renseignementsN6(p: Personne): Int {
    var n = 0
    if (dateComplete(p.dateNaissance)) n++
    if (p.lieuNaissance.trim().isNotEmpty()) n++
    if (deptIdentifiable(p.deptNaissance, p.lieuNaissance) != null) n++
    if (dateComplete(p.dateDeces)) n++
    if (p.lieuDeces.trim().isNotEmpty()) n++
    if (deptIdentifiable(p.deptDeces, p.lieuDeces) != null) n++
    return n
}

// Couleur du compteur : interpolation du rouge (0) au vert (total).
fun couleurCompteur(n: Int, total: Int): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.lerp(
        CouleurRouge,
        CouleurVert,
        if (total <= 0) 1f else (n.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    )

// \u00C2ge de la personne \u00E0 la date de l'\u00E9v\u00E9nement, affich\u00E9 entre
// parenth\u00E8ses dans la chronologie. Vide si incalculable.
fun ageAffiche(dateNaissance: String, dateEvenement: String): String {
    val an = anneeDe(dateNaissance) ?: return ""
    val ae = anneeDe(dateEvenement) ?: return ""
    val age = ae - an
    return when {
        age < 0 -> ""
        age == 0 -> "(moins d'un an)"
        else -> "(" + age.toString() + " ans)"
    }
}

// P\u00E9riode r\u00E9aliste d'un \u00E9v\u00E9nement, indiqu\u00E9e sous le champ date de
// la saisie. Purement indicative, jamais bloquante.
fun periodeRealisteTxt(type: String, p: Personne?): String {
    val an = anneeDe(p?.dateNaissance ?: "")
    val deces = anneeDe(p?.dateDeces ?: "")
    return when (type) {
        "B" -> if (an == null) "Bapt\u00EAme : dans les 2 ans suivant la naissance" else "Bapt\u00EAme : entre " + an.toString() + " et " + (an + 2).toString()
        "M" -> if (an == null) "Mariage : entre 16 et 40 ans" else "Mariage : entre " + (an + 16).toString() + " et " + (an + 40).toString()
        "S" -> "S\u00E9pulture : dans l'ann\u00E9e suivant le d\u00E9c\u00E8s"
        "D" -> if (an == null) "D\u00E9c\u00E8s : apr\u00E8s la naissance" else "D\u00E9c\u00E8s : apr\u00E8s " + an.toString()
        "E" -> if (an == null) "" else "\u00C9v\u00E9nement : entre " + an.toString() + (if (deces != null) " et " + deces.toString() else " et aujourd'hui")
        else -> ""
    }
}

// Contr\u00F4le de coh\u00E9rence : renonce un AVERTISSEMENT si la date saisie
// sort de la p\u00E9riode r\u00E9aliste. Jamais bloquant : l'utilisateur peut
// confirmer. Renvoie "" si tout est coh\u00E9rent.
fun avertissementCoherence(
    type: String,
    date: String,
    p: Personne?,
    conjoint: Personne?,
    parentPourEnfant: Personne?
): String {
    if (p == null) return ""
    val ae = anneeDe(date) ?: return ""
    val an = anneeDe(p.dateNaissance)
    val deces = anneeDe(p.dateDeces)
    when (type) {
        "B" -> if (an != null && (ae < an || ae > an + 2)) {
            return "Bapt\u00EAme plus de 2 ans apr\u00E8s la naissance : confirmer cette date ?"
        }
        "M" -> {
            if (an != null && (ae < an + 16 || ae > an + 40)) {
                return "Mariage en dehors de 16-40 ans : confirmer cette date ?"
            }
            if (deces != null && ae > deces) {
                return "Mariage apr\u00E8s le d\u00E9c\u00E8s : confirmer cette date ?"
            }
            conjoint?.let { c ->
                val ac = anneeDe(c.dateNaissance)
                if (ac != null && (ae < ac + 16 || ae > ac + 40)) {
                    return "Mariage en dehors de 16-40 ans pour le conjoint : confirmer ?"
                }
                val dc = anneeDe(c.dateDeces)
                if (dc != null && ae > dc) {
                    return "Mariage apr\u00E8s le d\u00E9c\u00E8s du conjoint : confirmer ?"
                }
            }
        }
        "S" -> if (deces != null && ae > deces + 1) {
            return "S\u00E9pulture plus d'un an apr\u00E8s le d\u00E9c\u00E8s : confirmer cette date ?"
        }
        "D" -> if (an != null && ae < an) {
            return "D\u00E9c\u00E8s avant la naissance : confirmer cette date ?"
        }
        "E" -> {
            if (an != null && ae < an) {
                return "\u00C9v\u00E9nement avant la naissance : confirmer cette date ?"
            }
            if (deces != null && ae > deces) {
                return "\u00C9v\u00E9nement apr\u00E8s le d\u00E9c\u00E8s : confirmer cette date ?"
            }
        }
        "N" -> parentPourEnfant?.let { par ->
            val ap = anneeDe(par.dateNaissance)
            if (ap != null && ae < ap) {
                return "Naissance avant celle du parent : confirmer cette date ?"
            }
            if (par.sexe == "F" && ap != null && ae > ap + 45) {
                return "Naissance apr\u00E8s 45 ans pour la m\u00E8re : confirmer cette date ?"
            }
            val dp = anneeDe(par.dateDeces)
            if (dp != null && ae > dp) {
                return "Naissance apr\u00E8s le d\u00E9c\u00E8s du parent : confirmer ?"
            }
            ""
        } ?: ""
    }
    return ""
}
