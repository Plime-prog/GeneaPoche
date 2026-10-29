package com.plime.geneapoche

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Export / import GEDCOM 5.5.1

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
    Regex("avant\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return "BEF " + m.groupValues[1]
    }
    Regex("apr\\u00E8s\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return "AFT " + m.groupValues[1]
    }
    Regex("(\\d{1,2})\\s*(janvier|fevrier|f\\u00E9vrier|mars|avril|mai|juin|juillet|aout|ao\\u00FBt|septembre|octobre|novembre|decembre|d\\u00E9cembre)\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        val nom = m.groupValues[2].lowercase(Locale.FRANCE)
        val index = when (nom) {
            "janvier" -> 1; "fevrier", "f\u00E9vrier" -> 2; "mars" -> 3; "avril" -> 4; "mai" -> 5
            "juin" -> 6; "juillet" -> 7; "aout", "ao\u00FBt" -> 8; "septembre" -> 9
            "octobre" -> 10; "novembre" -> 11; else -> 12
        }
        if (index in 1..12) return m.groupValues[1] + " " + moisGedcom(index) + " " + m.groupValues[3]
    }
    Regex("(\\d{4})").find(t)?.let { m ->
        return m.groupValues[1]
    }
    return null
}

// Heure GEDCOM ("2 TIME hh:mm") si l'heure est renseignee.
fun heureGedcom(s: String): String? {
    val m = Regex("(\\d{1,2}):(\\d{2})").find(s.trim()) ?: return null
    return m.groupValues[1] + ":" + m.groupValues[2]
}

// Ensemble des personnes lies a une personne donnee (parents, conjoints, enfants),
// pour exporter une branche complete.
fun brancheDe(personnes: List<Personne>, idCentre: Long): Set<Long> {
    val res = mutableSetOf<Long>()
    if (personneParId(personnes, idCentre) == null) return res
    val attente = ArrayDeque<Long>()
    attente.addLast(idCentre)
    res.add(idCentre)
    while (attente.isNotEmpty()) {
        val id = attente.removeFirst()
        val p = personneParId(personnes, id) ?: continue
        val proches = listOf(p.pere, p.mere, p.conjoint) + p.conjointsSupp + p.enfants
        proches.forEach { idP ->
            if (idP != 0L && !res.contains(idP) && personneParId(personnes, idP) != null) {
                res.add(idP)
                attente.addLast(idP)
            }
        }
    }
    return res
}

// Lieu normalise pour le GEDCOM (format Heredis) : "Roubaix, 59, Nord".
// Le numero de departement peut venir du champ dept, du lieu lui-meme ("Roubaix (59)"),
// ou du nom du departement ("Nord" -> 59).
fun lieuGedcom(commune: String, dept: String): String {
    val c = commune.trim()
    val n = dept.trim().toIntOrNull() ?: numeroDepartement(dept) ?: numeroDepartement(c)
    if (n == null) return c
    return listOf(c, n.toString(), NomsDepartements[n] ?: "").filter { it.isNotEmpty() }.joinToString(", ")
}

// Tag GEDCOM d'un type d'acte (SOUR DATA/EVEN).
fun tagEvenement(type: String): String = when (type) {
    "N" -> "BIRT"
    "B" -> "BAPM"
    "M" -> "MARR"
    "D" -> "DEAT"
    "S" -> "BURI"
    else -> "EVEN"
}

fun construireGedcom(toutes: List<Personne>, sousEnsemble: Set<Long>? = null, actes: List<Acte> = listOf()): String {
    val personnes = if (sousEnsemble == null) toutes else toutes.filter { sousEnsemble.contains(it.id) }
    fun cleCouple(a: Long, b: Long): Pair<Long, Long> = if (a < b) a to b else b to a
    fun xrefIndi(id: Long): String = "@I" + id.toString() + "@"
    fun xrefFam(c: Pair<Long, Long>): String = "@F" + c.first.toString() + "_" + c.second.toString() + "@"
    fun xrefSour(id: Long): String = "@S" + id.toString() + "@"

    // Actes des personnes exportees : chacun devient une SOURCE GEDCOM (Heredis).
    val actesExportes = actes.filter { a -> personnes.any { it.id == a.personneId } }
    fun sourcesDe(p: Personne, type: String): List<Acte> =
        actesExportes.filter { it.personneId == p.id && it.type == type }

    // Familles : couples (personne, conjoint) et couples (pere, mere), avec leurs enfants.
    val familles = mutableMapOf<Pair<Long, Long>, MutableList<Long>>()
    personnes.forEach { p ->
        p.tousConjoints().forEach { co ->
            if (personnes.any { it.id == co }) {
                val liste = familles.getOrPut(cleCouple(p.id, co)) { mutableListOf() }
                p.enfants.forEach { e -> if (!liste.contains(e)) liste.add(e) }
            }
        }
        if (p.pere != 0L && p.mere != 0L &&
            personnes.any { it.id == p.pere } && personnes.any { it.id == p.mere }
        ) {
            val liste = familles.getOrPut(cleCouple(p.pere, p.mere)) { mutableListOf() }
            if (!liste.contains(p.id)) liste.add(p.id)
        }
    }

    val lignes = mutableListOf<String>()
    lignes.add("0 HEAD")
    lignes.add("1 SOUR GENEAPOCHE")
    lignes.add("2 NAME GeneaPoche")
    lignes.add("2 VERS 0.9")
    lignes.add("1 CHAR UTF-8")
    lignes.add("1 GEDC")
    lignes.add("2 VERS 5.5.1")
    lignes.add("2 FORM LINEAGE-LINKED")
    lignes.add("1 DATE " + SimpleDateFormat("d MMM yyyy", Locale.US).format(Date()))

    // Un evenement de la personne, avec ses sources (actes rattaches).
    fun evenement(p: Personne, tag: String, typeActe: String, date: String, heure: String, lieu: String, dept: String) {
        val d = dateGedcom(date)
        val l = lieuGedcom(lieu, dept)
        val h = heureGedcom(heure)
        val src = sourcesDe(p, typeActe)
        if (d != null || l.isNotEmpty() || src.isNotEmpty()) {
            lignes.add("1 " + tag)
            if (d != null) lignes.add("2 DATE " + d)
            if (h != null) lignes.add("2 TIME " + h)
            if (l.isNotEmpty()) lignes.add("2 PLAC " + l)
            src.forEach { a ->
                lignes.add("2 SOUR " + xrefSour(a.id))
                if (a.numPage.isNotEmpty()) {
                    lignes.add("3 PAGE p. " + a.numPage + "/" + a.totalPage)
                }
            }
        }
    }

    personnes.forEach { p ->
        lignes.add("0 " + xrefIndi(p.id) + " INDI")
        lignes.add("1 NAME " + p.prenoms.trim() + " /" + p.nom.trim() + "/")
        if (p.sexe.isNotEmpty()) lignes.add("1 SEX " + p.sexe)
        evenement(p, "BIRT", "N", p.dateNaissance, p.heureNaissance, p.lieuNaissance, p.deptNaissance)
        evenement(p, "BAPM", "B", p.dateBapteme, p.heureBapteme, p.lieuBapteme, p.deptBapteme)
        evenement(p, "MARR", "M", p.dateMariage, p.heureMariage, p.lieuMariage, p.deptMariage)
        evenement(p, "DEAT", "D", p.dateDeces, p.heureDeces, p.lieuDeces, p.deptDeces)
        evenement(p, "BURI", "S", p.dateSepulture, p.heureSepulture, p.lieuSepulture, p.deptSepulture)
        if (p.profession.trim().isNotEmpty()) lignes.add("1 OCCU " + p.profession.trim())
        if (p.sosa.trim().isNotEmpty()) lignes.add("1 RFN Sosa " + p.sosa.trim())
        if (p.pere != 0L && p.mere != 0L &&
            familles.containsKey(cleCouple(p.pere, p.mere))
        ) {
            lignes.add("1 FAMC " + xrefFam(cleCouple(p.pere, p.mere)))
        }
        p.tousConjoints().forEach { co ->
            if (familles.containsKey(cleCouple(p.id, co))) {
                lignes.add("1 FAMS " + xrefFam(cleCouple(p.id, co)))
            }
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
        val hm = epoux?.let { heureGedcom(it.heureMariage) }
        val lm = epoux?.let { lieuGedcom(it.lieuMariage, it.deptMariage) } ?: ""
        val srcM = epoux?.let { sourcesDe(it, "M") } ?: listOf()
        if (dm != null || lm.isNotEmpty() || srcM.isNotEmpty()) {
            lignes.add("1 MARR")
            if (dm != null) lignes.add("2 DATE " + dm)
            if (hm != null) lignes.add("2 TIME " + hm)
            if (lm.isNotEmpty()) lignes.add("2 PLAC " + lm)
            srcM.forEach { sa ->
                lignes.add("2 SOUR " + xrefSour(sa.id))
                if (sa.numPage.isNotEmpty()) {
                    lignes.add("3 PAGE p. " + sa.numPage + "/" + sa.totalPage)
                }
            }
        }
        enfants.forEach { e -> if (personnes.any { it.id == e }) lignes.add("1 CHIL " + xrefIndi(e)) }
    }

    // Sources : un enregistrement SOUR par acte, facon Heredis
    // (TITL + REFN cote + DATA/EVEN + DATE + PLAC + NOTE transcription).
    actesExportes.forEach { a ->
        val pers = personnes.firstOrNull { it.id == a.personneId }
        lignes.add("0 " + xrefSour(a.id) + " SOUR")
        val t = a.titre.trim()
        lignes.add("1 TITL " + (if (t.isNotEmpty()) t else libelleType(a.type) + (if (pers != null) " de " + pers.titre() else "")))
        if (a.reference.trim().isNotEmpty()) {
            lignes.add("1 REFN " + a.reference.trim())
        }
        if (pers != null) {
            val ch = champsEvenement(pers, a.type)
            val d = dateGedcom(ch.date)
            val l = lieuGedcom(ch.lieu, ch.dept)
            if (d != null || l.isNotEmpty()) {
                lignes.add("1 DATA")
                lignes.add("2 EVEN " + tagEvenement(a.type))
                if (d != null) lignes.add("3 DATE " + d)
                if (l.isNotEmpty()) lignes.add("3 PLAC " + l)
            }
        }
        if (a.contenu.isNotBlank()) {
            val morceaux = a.contenu.split("\n")
            lignes.add("1 NOTE " + morceaux.first().trim())
            morceaux.drop(1).forEach { suite ->
                lignes.add("2 CONT " + suite.trim())
            }
        }
    }

    lignes.add("0 TRLR")
    return lignes.joinToString("\n") + "\n"
}

// ---------------------------------------------------------------- Import GEDCOM (Heredis, etc.)

// Convertit une date GEDCOM en date lisible :
// "27 OCT 1890" -> "27/10/1890", "ABT 1890" -> "vers 1890", "1890" -> "1890".
fun dateFrancaise(s: String): String {
    val t = s.trim()
    if (t.isEmpty()) return ""
    val moisAnglais = mapOf(
        "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4, "MAY" to 5, "JUN" to 6,
        "JUL" to 7, "AUG" to 8, "SEP" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12
    )
    Regex("(\\d{1,2})\\s+([A-Z]{3})\\s+(\\d{4})").find(t)?.let { m ->
        val mois = moisAnglais[m.groupValues[2]]
        if (mois != null) return m.groupValues[1] + "/" + mois.toString() + "/" + m.groupValues[3]
    }
    Regex("ABT\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return "vers " + m.groupValues[1]
    }
    Regex("BEF\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return "avant " + m.groupValues[1]
    }
    Regex("AFT\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return "apr\u00e8s " + m.groupValues[1]
    }
    Regex("BET\\s+(\\d{4})\\s+AND\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return "entre " + m.groupValues[1] + " et " + m.groupValues[2]
    }
    Regex("^(\\d{4})$").find(t)?.let { m ->
        return m.groupValues[1]
    }
    Regex("(\\d{4})").find(t)?.let { m ->
        return m.groupValues[1]
    }
    return t
}

// Parseur d'un fichier GEDCOM 5.5.1 (Heredis, Geneatique...) :
// personnes (INDI), familles (FAM), dates N/BAPM/M/D/BURI, heures TIME, conjoints multiples.
// Retourne au maximum 50 personnes.
fun importerGedcom(texte: String): List<Personne> {
    val lignes = texte.split("\n").map { it.trimEnd('\r') }

    class Ind {
        var nom = ""
        var prenoms = ""
        var sexe = ""
        var occu = ""
        var nD = ""; var nH = ""; var nL = ""
        var bD = ""; var bH = ""; var bL = ""
        var mD = ""; var mH = ""; var mL = ""
        var dD = ""; var dH = ""; var dL = ""
        var sD = ""; var sH = ""; var sL = ""
        val fams = mutableListOf<String>()
        var famc = ""
    }
    class Fam {
        var husb = ""
        var wife = ""
        val chil = mutableListOf<String>()
        var marrD = ""
        var marrL = ""
    }

    val indivs = LinkedHashMap<String, Ind>()
    val famsMap = LinkedHashMap<String, Fam>()
    var indCourant: Ind? = null
    var famCourante: Fam? = null
    var blocActuel = "" // tag de niveau 1 en cours ("BIRT", "DEAT"...)
    var dateTampon = ""

    lignes.forEach { ligne ->
        val m = Regex("^(\\d+)\\s+(.*)$").find(ligne) ?: return@forEach
        val niveau = m.groupValues[1].toIntOrNull() ?: return@forEach
        val contenu = m.groupValues[2].trim()
        if (niveau == 0) {
            blocActuel = ""
            if (Regex("^@.*@\\s+INDI$").containsMatchIn(contenu)) {
                val xref = Regex("^(@.*@)").find(contenu)?.groupValues?.get(1) ?: return@forEach
                val ind = Ind()
                indivs[xref] = ind
                indCourant = ind
                famCourante = null
            } else if (Regex("^@.*@\\s+FAM$").containsMatchIn(contenu)) {
                val xref = Regex("^(@.*@)").find(contenu)?.groupValues?.get(1) ?: return@forEach
                val fam = Fam()
                famsMap[xref] = fam
                famCourante = fam
                indCourant = null
            } else {
                indCourant = null
                famCourante = null
            }
            return@forEach
        }
        val ind = indCourant
        val fam = famCourante
        if (niveau == 1) {
            blocActuel = contenu.split(" ").firstOrNull() ?: ""
            dateTampon = ""
            if (ind != null) {
                when (blocActuel) {
                    "NAME" -> {
                        val valeur = contenu.removePrefix("NAME").trim()
                        val nomFamille = Regex("/([^/]*)/").find(valeur)?.groupValues?.get(1) ?: ""
                        val reste = valeur.replace(Regex("/[^/]*/"), " ").trim()
                        ind.nom = nomFamille.trim()
                        ind.prenoms = reste.trim()
                    }
                    "SEX" -> ind.sexe = contenu.removePrefix("SEX").trim()
                    "OCCU" -> ind.occu = contenu.removePrefix("OCCU").trim()
                    "FAMS" -> ind.fams.add(contenu.removePrefix("FAMS").trim())
                    "FAMC" -> ind.famc = contenu.removePrefix("FAMC").trim()
                }
            } else if (fam != null) {
                when (blocActuel) {
                    "HUSB" -> fam.husb = contenu.removePrefix("HUSB").trim()
                    "WIFE" -> fam.wife = contenu.removePrefix("WIFE").trim()
                    "CHIL" -> fam.chil.add(contenu.removePrefix("CHIL").trim())
                }
            }
        } else if (niveau == 2) {
            val tag = contenu.split(" ").firstOrNull() ?: ""
            val valeur = contenu.removePrefix(tag).trim()
            if (ind != null) {
                when (blocActuel) {
                    "BIRT" -> when (tag) {
                        "DATE" -> ind.nD = dateFrancaise(valeur)
                        "TIME" -> ind.nH = valeur
                        "PLAC" -> ind.nL = valeur
                    }
                    "BAPM", "CHR", "BAPT" -> when (tag) {
                        "DATE" -> ind.bD = dateFrancaise(valeur)
                        "TIME" -> ind.bH = valeur
                        "PLAC" -> ind.bL = valeur
                    }
                    "MARR" -> when (tag) {
                        "DATE" -> ind.mD = dateFrancaise(valeur)
                        "TIME" -> ind.mH = valeur
                        "PLAC" -> ind.mL = valeur
                    }
                    "DEAT" -> when (tag) {
                        "DATE" -> ind.dD = dateFrancaise(valeur)
                        "TIME" -> ind.dH = valeur
                        "PLAC" -> ind.dL = valeur
                    }
                    "BURI", "BUR" -> when (tag) {
                        "DATE" -> ind.sD = dateFrancaise(valeur)
                        "TIME" -> ind.sH = valeur
                        "PLAC" -> ind.sL = valeur
                    }
                }
            } else if (fam != null && blocActuel == "MARR") {
                when (tag) {
                    "DATE" -> fam.marrD = dateFrancaise(valeur)
                    "PLAC" -> fam.marrL = valeur
                }
            }
        }
    }

    // Attribution des identifiants
    val ids = mutableMapOf<String, Long>()
    var prochain = 1L
    indivs.keys.forEach { x -> ids[x] = prochain; prochain += 1L }

    val personnes = mutableListOf<Personne>()
    indivs.forEach { (x, ind) ->
        personnes.add(
            Personne(
                id = ids[x] ?: 0L,
                nom = ind.nom,
                prenoms = ind.prenoms,
                sexe = ind.sexe,
                profession = ind.occu,
                dateNaissance = ind.nD, heureNaissance = ind.nH, lieuNaissance = ind.nL,
                dateBapteme = ind.bD, heureBapteme = ind.bH, lieuBapteme = ind.bL,
                dateMariage = ind.mD, heureMariage = ind.mH, lieuMariage = ind.mL,
                dateDeces = ind.dD, heureDeces = ind.dH, lieuDeces = ind.dL,
                dateSepulture = ind.sD, heureSepulture = ind.sH, lieuSepulture = ind.sL
            )
        )
    }

    fun idDe(x: String): Long = ids[x] ?: 0L

    // Liens de famille
    famsMap.values.forEach { fam ->
        val h = idDe(fam.husb)
        val w = idDe(fam.wife)
        val ph = personnes.firstOrNull { it.id == h }
        val pw = personnes.firstOrNull { it.id == w }
        if (ph != null && pw != null) {
            if (ph.conjoint == 0L) ph.conjoint = w else if (!ph.conjointsSupp.contains(w) && ph.conjoint != w) ph.conjointsSupp.add(w)
            if (pw.conjoint == 0L) pw.conjoint = h else if (!pw.conjointsSupp.contains(h) && pw.conjoint != h) pw.conjointsSupp.add(h)
            if (ph.dateMariage.isEmpty() && fam.marrD.isNotEmpty()) ph.dateMariage = fam.marrD
            if (pw.dateMariage.isEmpty() && fam.marrD.isNotEmpty()) pw.dateMariage = fam.marrD
            if (ph.lieuMariage.isEmpty() && fam.marrL.isNotEmpty()) ph.lieuMariage = fam.marrL
            if (pw.lieuMariage.isEmpty() && fam.marrL.isNotEmpty()) pw.lieuMariage = fam.marrL
        }
        fam.chil.forEach { xc ->
            val c = idDe(xc)
            val pc = personnes.firstOrNull { it.id == c } ?: return@forEach
            if (ph != null && pc.pere == 0L) pc.pere = ph.id
            if (pw != null && pc.mere == 0L) pc.mere = pw.id
            if (ph != null && !ph.enfants.contains(pc.id)) ph.enfants.add(pc.id)
            if (pw != null && !pw.enfants.contains(pc.id)) pw.enfants.add(pc.id)
        }
    }

    return personnes.take(50)
}


