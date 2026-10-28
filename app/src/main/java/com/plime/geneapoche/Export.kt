package com.plime.geneapoche

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Export GEDCOM 5.5.1

// Construction d'un fichier GEDCOM 5.5.1 (LINEAGE-LINKED, UTF-8) importable
// dans Heredis, Geneatique, Ancestry, FamilySearch, etc.

fun moisGedcom(m: Int): String = when (m) {
    1 -> "JAN"; 2 -> "FEB"; 3 -> "MAR"; 4 -> "APR"; 5 -> "MAY"; 6 -> "JUN"
    7 -> "JUL"; 8 -> "AUG"; 9 -> "SEP"; 10 -> "OCT"; 11 -> "NOV"; else -> "DEC"
}

// Convertit une date saisie en date GEDCOM :
// "27/10/1890" -> "27 OCT 1890", "vers 1890" -> "ABT 1890", "1890" -> "1890".
fun dateGedcom(s: String): String? {
    val t = s.trim()
    if (t.isEmpty()) return null
    Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})").find(t)?.let { m ->
        val mois = m.groupValues[2].toIntOrNull() ?: return null
        if (mois in 1..12) return m.groupValues[1] + " " + moisGedcom(mois) + " " + m.groupValues[3]
    }
    Regex("vers\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return "ABT " + m.groupValues[1]
    }
    Regex("(\\d{1,2})\\s*(janvier|fevrier|mars|avril|mai|juin|juillet|aout|septembre|octobre|novembre|decembre)\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        val nom = m.groupValues[2].lowercase(Locale.FRANCE)
        val mois = listOf("janvier", "fevrier", "mars", "avril", "mai", "juin", "juillet", "aout", "septembre", "octobre", "novembre", "decembre").indexOf(nom) + 1
        if (mois in 1..12) return m.groupValues[1] + " " + moisGedcom(mois) + " " + m.groupValues[3]
    }
    Regex("(\\d{4})").find(t)?.let { m ->
        return m.groupValues[1]
    }
    return null
}

fun construireGedcom(personnes: List<Personne>): String {
    fun cleCouple(a: Long, b: Long): Pair<Long, Long> = if (a < b) a to b else b to a
    fun xrefIndi(id: Long): String = "@I" + id.toString() + "@"
    fun xrefFam(c: Pair<Long, Long>): String = "@F" + c.first.toString() + "_" + c.second.toString() + "@"

    // Familles : couples (pere, mere) et couples (personne, conjoint), avec leurs enfants.
    val familles = mutableMapOf<Pair<Long, Long>, MutableList<Long>>()
    personnes.forEach { p ->
        if (p.conjoint != 0L) {
            val liste = familles.getOrPut(cleCouple(p.id, p.conjoint)) { mutableListOf() }
            p.enfants.forEach { e -> if (!liste.contains(e)) liste.add(e) }
        }
        if (p.pere != 0L && p.mere != 0L) {
            val liste = familles.getOrPut(cleCouple(p.pere, p.mere)) { mutableListOf() }
            if (!liste.contains(p.id)) liste.add(p.id)
        }
    }

    val lignes = mutableListOf<String>()
    lignes.add("0 HEAD")
    lignes.add("1 SOUR GENEAPOCHE")
    lignes.add("2 NAME GeneaPoche")
    lignes.add("2 VERS 0.8")
    lignes.add("1 CHAR UTF-8")
    lignes.add("1 GEDC")
    lignes.add("2 VERS 5.5.1")
    lignes.add("2 FORM LINEAGE-LINKED")
    lignes.add("1 DATE " + SimpleDateFormat("d MMM yyyy", Locale.US).format(Date()))

    personnes.forEach { p ->
        lignes.add("0 " + xrefIndi(p.id) + " INDI")
        lignes.add("1 NAME " + p.prenoms.trim() + " /" + p.nom.trim() + "/")
        if (p.sexe.isNotEmpty()) lignes.add("1 SEX " + p.sexe)
        val dn = dateGedcom(p.dateNaissance)
        val ln = p.lieuNaissance.trim()
        if (dn != null || ln.isNotEmpty()) {
            lignes.add("1 BIRT")
            if (dn != null) lignes.add("2 DATE " + dn)
            if (ln.isNotEmpty()) lignes.add("2 PLAC " + ln)
        }
        val dd = dateGedcom(p.dateDeces)
        val ld = p.lieuDeces.trim()
        if (dd != null || ld.isNotEmpty()) {
            lignes.add("1 DEAT")
            if (dd != null) lignes.add("2 DATE " + dd)
            if (ld.isNotEmpty()) lignes.add("2 PLAC " + ld)
        }
        if (p.profession.trim().isNotEmpty()) lignes.add("1 OCCU " + p.profession.trim())
        if (p.sosa.trim().isNotEmpty()) lignes.add("1 _SOSA " + p.sosa.trim())
        if (p.pere != 0L && p.mere != 0L) {
            lignes.add("1 FAMC " + xrefFam(cleCouple(p.pere, p.mere)))
        }
        if (p.conjoint != 0L) {
            lignes.add("1 FAMS " + xrefFam(cleCouple(p.id, p.conjoint)))
        }
    }

    familles.forEach { (cle, enfants) ->
        lignes.add("0 " + xrefFam(cle) + " FAM")
        val a = personnes.firstOrNull { it.id == cle.first }
        val b = personnes.firstOrNull { it.id == cle.second }
        if (a != null && a.sexe == "F") {
            lignes.add("1 WIFE " + xrefIndi(cle.first))
            lignes.add("1 HUSB " + xrefIndi(cle.second))
        } else {
            lignes.add("1 HUSB " + xrefIndi(cle.first))
            lignes.add("1 WIFE " + xrefIndi(cle.second))
        }
        val epoux = personnes.firstOrNull { c ->
            (c.id == cle.first || c.id == cle.second) &&
                (c.dateMariage.trim().isNotEmpty() || c.lieuMariage.trim().isNotEmpty())
        }
        val dm = epoux?.let { dateGedcom(it.dateMariage) }
        val lm = epoux?.lieuMariage?.trim() ?: ""
        if (dm != null || lm.isNotEmpty()) {
            lignes.add("1 MARR")
            if (dm != null) lignes.add("2 DATE " + dm)
            if (lm.isNotEmpty()) lignes.add("2 PLAC " + lm)
        }
        enfants.forEach { e -> lignes.add("1 CHIL " + xrefIndi(e)) }
    }

    lignes.add("0 TRLR")
    return lignes.joinToString("\n") + "\n"
}
