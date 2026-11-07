package com.plime.geneapoche

import java.util.Locale

// ---------------------------------------------------------------- Export / import GEDCOM 5.5.1

fun moisGedcom(m: Int): String = when (m) {
    1 -> "JAN"; 2 -> "FEB"; 3 -> "MAR"; 4 -> "APR"; 5 -> "MAY"; 6 -> "JUN"
    7 -> "JUL"; 8 -> "AUG"; 9 -> "SEP"; 10 -> "OCT"; 11 -> "NOV"; else -> "DEC"
}

// Convertit une date saisie en date GEDCOM :
// "27/10/1890" -> "27 OCT 1890", "vers 1890" -> "ABT 1890", "1890" -> "1890".
// G1-16 : une personne est vivante si elle n'a pas de deces connu ET
// (sans annee de naissance exploitable ou) nee il y a moins de 100 ans.
fun estVivante(p: Personne, anneeCourante: Int): Boolean {
    if (p.dateDeces.isNotBlank()) return false
    val annee = Regex("\\d{4}").find(p.dateNaissance)?.value?.toIntOrNull() ?: return true
    return annee > anneeCourante - 100
}

fun dateGedcom(s: String): String? {
    val t = s.trim()
    if (t.isEmpty()) return null
    Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})").find(t)?.let { m ->
        val mois = m.groupValues[2].toIntOrNull() ?: return null
        val jour = m.groupValues[1].toIntOrNull() ?: return null
        val annee = m.groupValues[3].toIntOrNull() ?: return null
        // G2-01 : date verifiee avec java.time.LocalDate avant export ; une
        // date impossible (31/02, 29/02/1900) retombe sur l annee seule plus bas.
        val reelle = try {
            java.time.LocalDate.of(annee, mois, jour)
            true
        } catch (e: java.time.DateTimeException) {
            false
        }
        if (reelle) return jour.toString() + " " + moisGedcom(mois) + " " + m.groupValues[3]
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
        // G2-01 : date verifiee avec java.time.LocalDate avant export.
        if (index in 1..12) {
            val jourL = m.groupValues[1].toIntOrNull() ?: return null
            val anneeL = m.groupValues[3].toIntOrNull() ?: return null
            try {
                java.time.LocalDate.of(anneeL, index, jourL)
            } catch (e: java.time.DateTimeException) {
                return null
            }
            return jourL.toString() + " " + moisGedcom(index) + " " + m.groupValues[3]
        }
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

// G2-02 : texte GEDCOM SUR : tout "@" d'une valeur est DOUBLE ("@@"),
// sinon il serait interprete comme un pointeur a l'import.
fun texteGedcom(s: String): String = s.replace("@", "@@")

// G2-02 : une ligne GEDCOM ne doit jamais depasser 255 caracteres : les
// valeurs longues sont coupees en CONC (suite de la MEME ligne), au
// niveau de la valeur + 1.
fun ajouterValeur(lignes: MutableList<String>, niveau: Int, tag: String, valeur: String) {
    val morceaux = texteGedcom(valeur).chunked(200)
    lignes.add(niveau.toString() + " " + tag + " " + morceaux.first())
    morceaux.drop(1).forEach { c ->
        lignes.add((niveau + 1).toString() + " CONC " + c)
    }
}

// G2-02 : note multi-lignes : premiere ligne en NOTE, suivantes en CONT,
// chaque ligne coupee en CONC si trop longue.
fun ajouterNote(lignes: MutableList<String>, niveau: Int, tag: String, texte: String) {
    val morceaux = texte.split("\n")
    ajouterValeur(lignes, niveau, tag, morceaux.first().trim())
    morceaux.drop(1).forEach { suite ->
        ajouterValeur(lignes, niveau + 1, "CONT", suite.trim())
    }
}

fun construireGedcom(toutes: List<Personne>, sousEnsemble: Set<Long>? = null, actes: List<Acte> = listOf(), version: String = "5.5.1"): String {
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
    lignes.add("2 VERS 0.79") // G2-02 : version REELLE de l'application (a maintenir a chaque versionName)
    lignes.add("1 CHAR UTF-8")
    lignes.add("1 LANG fr")
    // G\u00E9n\u00E9aPoche 3 : double format d'export. 5.5.1 pour Heredis ;
    // 7.0.18 (derni\u00E8re version du standard au 01/11/2026) sur demande.
    if (version.startsWith("7")) {
        lignes.add("1 GEDC")
        lignes.add("2 LINEAGE-LINKED")
        lignes.add("3 VERS 7.0.18")
    } else {
        lignes.add("1 GEDC")
        lignes.add("2 VERS 5.5.1")
        lignes.add("2 FORM LINEAGE-LINKED")
    }
    lignes.add("1 SUBM @SUB1@") // G2-02 : reference a l'enregistrement SOUMIS (exige par la norme)
    // G2-20 : SimpleDateFormat remplace par DateTimeFormatter (sur entre
    // threads, pas d API obsolete).
    lignes.add("1 DATE " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)))

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
            if (l.isNotEmpty()) lignes.add("2 PLAC " + texteGedcom(l))
            src.forEach { a ->
                lignes.add("2 SOUR " + xrefSour(a.id))
                if (a.numPage.isNotEmpty()) {
                    lignes.add("3 PAGE p. " + a.numPage + "/" + a.totalPage)
                }
                // G\u00E9n\u00E9aPoche 11 (11.4) : niveau de preuve, \u00E9chelle QUAY
                // (0 indirecte \u00E0 3 acte original), standard Heredis/Gramps.
                if (a.fiabilite in 0..3) {
                    lignes.add("3 QUAY " + a.fiabilite)
                }
            }
        }
    }

    personnes.forEach { p ->
        lignes.add("0 " + xrefIndi(p.id) + " INDI")
        lignes.add("1 NAME " + texteGedcom(p.prenoms.trim()) + " /" + texteGedcom(p.nom.trim()) + "/")
        if (p.sexe.isNotEmpty()) lignes.add("1 SEX " + p.sexe)
        evenement(p, "BIRT", "N", p.dateNaissance, p.heureNaissance, p.lieuNaissance, p.deptNaissance)
        evenement(p, "BAPM", "B", p.dateBapteme, p.heureBapteme, p.lieuBapteme, p.deptBapteme)
        evenement(p, "MARR", "M", p.dateMariage, p.heureMariage, p.lieuMariage, p.deptMariage)
        evenement(p, "DEAT", "D", p.dateDeces, p.heureDeces, p.lieuDeces, p.deptDeces)
        evenement(p, "BURI", "S", p.dateSepulture, p.heureSepulture, p.lieuSepulture, p.deptSepulture)
        // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements libres (intitul\u00E9 libre) de la
        // chronologie, export\u00E9s comme EVEN + TYPE (les deux formats).
        p.evenementsLibres.forEach { ev ->
            val dEv = dateGedcom(ev.date)
            val hEv = heureGedcom(ev.heure)
            val lEv = lieuGedcom(ev.lieu, ev.dept)
            if (ev.intitule.trim().isNotEmpty() || dEv != null || lEv.isNotEmpty()) {
                lignes.add("1 EVEN")
                lignes.add("2 TYPE " + texteGedcom(ev.intitule.trim()))
                if (dEv != null) lignes.add("2 DATE " + dEv)
                if (hEv != null) lignes.add("2 TIME " + hEv)
                if (lEv.isNotEmpty()) lignes.add("2 PLAC " + texteGedcom(lEv))
            }
        }
        if (p.profession.trim().isNotEmpty()) lignes.add("1 OCCU " + texteGedcom(p.profession.trim()))
        if (p.sosa.trim().isNotEmpty()) lignes.add("1 RFN Sosa " + p.sosa.trim())
        // G2-02 : le bloc "sans titre" de la fiche part en NOTE (relue a l'import).
        if (p.notesLibres.trim().isNotEmpty()) ajouterNote(lignes, 1, "NOTE", p.notesLibres.trim())
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
                // G\u00E9n\u00E9aPoche 11 (11.4) : niveau de preuve QUAY.
                if (sa.fiabilite in 0..3) {
                    lignes.add("3 QUAY " + sa.fiabilite)
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
        lignes.add("1 TITL " + texteGedcom(if (t.isNotEmpty()) t else libelleType(a.type) + (if (pers != null) " de " + pers.titre() else "")))
        if (a.reference.trim().isNotEmpty()) {
            lignes.add("1 REFN " + texteGedcom(a.reference.trim()))
        }
        // G\u00E9n\u00E9aPoche 11 (11.4) : d\u00E9p\u00F4t de conservation (AGNC) et
        // date de consultation de la source.
        if (a.depot.trim().isNotEmpty()) {
            lignes.add("1 AGNC " + texteGedcom(a.depot.trim()))
        }
        if (a.dateConsultation.trim().isNotEmpty()) {
            ajouterNote(lignes, 1, "NOTE", "Consult\u00E9 le " + a.dateConsultation.trim())
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
            ajouterNote(lignes, 1, "NOTE", a.contenu)
        }
    }

    // G2-02 : enregistrement SUBM reference par le HEAD.
    lignes.add("0 @SUB1@ SUBM")
    lignes.add("1 NAME Utilisateur GeneaPoche")
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
        // v0.71 : ZERO D'OFFICE aussi a l'import : "5 OCT 1890" doit revenir
        // en "05/10/1890" (jj/mm/aaaa), sinon le probleme reviendrait apres
        // chaque export/reimport.
        if (mois != null) return m.groupValues[1].padStart(2, '0') + "/" + mois.toString().padStart(2, '0') + "/" + m.groupValues[3]
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

// Reponse utilisateur : un PLAC exporte par GeneaPoche ("Roubaix, 59, Nord")
// doit se REIMPORTER en commune + numero de departement separes, pas en
// un seul bloc avec une virgule et un mauvais departement.
fun couperLieu(plac: String): Pair<String, String> {
    val parties = plac.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parties.size >= 2 && parties[1].toIntOrNull() != null) {
        return parties[0] to parties[1]
    }
    return plac.trim() to ""
}

// Parseur d'un fichier GEDCOM 5.5.1 (Heredis, Geneatique...) :
// personnes (INDI), familles (FAM), dates N/BAPM/M/D/BURI, heures TIME, conjoints multiples.
// Aucun plafond de personnes depuis la G1-10 (decision de l auteur).
// G2-04 : resultat d import avec rapport de ce qui a ete ignore
// (aucune donnee personnelle n y figure, seulement des noms de balises).
data class ResultatImport(val personnes: List<Personne>, val ignores: List<String>)

// G2-04 : detecter l encodage d un fichier GEDCOM : BOM UTF-16 LE/BE ou
// UTF-8, sinon UTF-8 STRICT, avec repli ANSI (windows-1252).
fun decoderGedcom(octets: ByteArray): String {
    if (octets.size >= 2 && octets[0] == 0xFF.toByte() && octets[1] == 0xFE.toByte()) {
        return String(octets, 2, octets.size - 2, Charsets.UTF_16LE)
    }
    if (octets.size >= 2 && octets[0] == 0xFE.toByte() && octets[1] == 0xFF.toByte()) {
        return String(octets, 2, octets.size - 2, Charsets.UTF_16BE)
    }
    val corps = if (octets.size >= 3 && octets[0] == 0xEF.toByte() && octets[1] == 0xBB.toByte() && octets[2] == 0xBF.toByte()) {
        octets.copyOfRange(3, octets.size)
    } else octets
    return try {
        val dec = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        dec.decode(java.nio.ByteBuffer.wrap(corps)).toString()
    } catch (e: Exception) {
        String(corps, Charsets.ISO_8859_1)
    }
}

// G2-04 : recoller les suites CONC (meme ligne) et CONT (nouvelle ligne)
// AVANT l analyse : les valeurs coupees par l exportateur sont restituees.
fun recollerConcCont(brutes: List<String>): List<String> {
    val res = mutableListOf<String>()
    var cible = -1
    brutes.forEach { ligne ->
        val m = Regex("^(\\d+)\\s+(CONC|CONT)(?:\\s(.*))?$").find(ligne.trim())
        if (m == null || cible < 0) {
            res.add(ligne)
            cible = res.size - 1
            return@forEach
        }
        if (m.groupValues[2] == "CONT") {
            res[cible] = res[cible] + "\n" + m.groupValues[3]
        } else {
            res[cible] = res[cible] + m.groupValues[3]
        }
    }
    return res
}

fun importerGedcomRapporte(texte: String): ResultatImport {
    val ignoresRapport = mutableListOf<String>()
    val enregistrementsIgnores = mutableMapOf<String, Int>()
    val balisesIgnorees = mutableMapOf<String, Int>()
    fun ignorer(carte: MutableMap<String, Int>, cle: String) {
        carte[cle] = (carte[cle] ?: 0) + 1
    }
    val lignes = recollerConcCont(texte.split("\n").map { it.trimEnd('\r') })

    class Ind {
        var nom = ""
        var prenoms = ""
        var sexe = ""
        var occu = ""
        var rfn = ""
        var note = ""
        var nD = ""; var nH = ""; var nL = ""; var nDpt = ""
        var bD = ""; var bH = ""; var bL = ""; var bDpt = ""
        var mD = ""; var mH = ""; var mL = ""; var mDpt = ""
        var dD = ""; var dH = ""; var dL = ""; var dDpt = ""
        var sD = ""; var sH = ""; var sL = ""; var sDpt = ""
        // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements libres (EVEN + TYPE/DATE/TIME/PLAC).
        val evLibres = mutableListOf<EvenementLibre>()
        val fams = mutableListOf<String>()
        var famc = ""
    }
    class Fam {
        var husb = ""
        var wife = ""
        val chil = mutableListOf<String>()
        var marrD = ""
        var marrL = ""
        var marrDept = ""
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
                // G2-04 : comptage des enregistrements non importes (SOUR,
                // NOTE, REPO...) pour le rapport, sans perte silencieuse.
                val balise = Regex("^(@.*@\\s+)?([A-Za-z_]+)").find(contenu)?.groupValues?.get(2) ?: "?"
                if (balise != "HEAD" && balise != "TRLR") ignorer(enregistrementsIgnores, balise)
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
                    // G2-02 : GeneaPoche exporte "RFN Sosa 12" ; le numero
                    // Sosa (ou d'Aboville) est desormais RELU a l'import.
                    "RFN" -> {
                        val numR = Regex("\\d+(\\.\\d+)*").find(contenu.removePrefix("RFN"))?.value
                        if (numR != null) ind.rfn = numR
                    }
                    "NOTE" -> ind.note = contenu.removePrefix("NOTE").trim().replace("@@", "@")
                    // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nement libre (intitul\u00E9 en 2 TYPE).
                    "EVEN" -> ind.evLibres.add(EvenementLibre())
                    else -> if (blocActuel !in listOf("BIRT", "BAPM", "CHR", "BAPT", "MARR", "DEAT", "BURI", "BUR")) {
                        ignorer(balisesIgnorees, blocActuel)
                    }
                }
            } else if (fam != null) {
                when (blocActuel) {
                    "HUSB" -> fam.husb = contenu.removePrefix("HUSB").trim()
                    "WIFE" -> fam.wife = contenu.removePrefix("WIFE").trim()
                    "CHIL" -> fam.chil.add(contenu.removePrefix("CHIL").trim())
                    else -> ignorer(balisesIgnorees, blocActuel)
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
                        "PLAC" -> { val cd = couperLieu(valeur); ind.nL = cd.first; ind.nDpt = cd.second }
                    }
                    "BAPM", "CHR", "BAPT" -> when (tag) {
                        "DATE" -> ind.bD = dateFrancaise(valeur)
                        "TIME" -> ind.bH = valeur
                        "PLAC" -> { val cd = couperLieu(valeur); ind.bL = cd.first; ind.bDpt = cd.second }
                    }
                    "MARR" -> when (tag) {
                        "DATE" -> ind.mD = dateFrancaise(valeur)
                        "TIME" -> ind.mH = valeur
                        "PLAC" -> { val cd = couperLieu(valeur); ind.mL = cd.first; ind.mDpt = cd.second }
                    }
                    "DEAT" -> when (tag) {
                        "DATE" -> ind.dD = dateFrancaise(valeur)
                        "TIME" -> ind.dH = valeur
                        "PLAC" -> { val cd = couperLieu(valeur); ind.dL = cd.first; ind.dDpt = cd.second }
                    }
                    "BURI", "BUR" -> when (tag) {
                        "DATE" -> ind.sD = dateFrancaise(valeur)
                        "TIME" -> ind.sH = valeur
                        "PLAC" -> { val cd = couperLieu(valeur); ind.sL = cd.first; ind.sDpt = cd.second }
                    }
                    // G2-02 : relire les notes coupees en CONC/CONT.
                    "NOTE" -> when (tag) {
                        "CONT" -> ind.note += "\n" + valeur.replace("@@", "@")
                        "CONC" -> ind.note += valeur.replace("@@", "@")
                    }
                    // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nement libre de la chronologie.
                    "EVEN" -> {
                        val dernier = ind.evLibres.lastOrNull()
                        if (dernier != null) when (tag) {
                            "TYPE" -> dernier.intitule = valeur.replace("@@", "@")
                            "DATE" -> dernier.date = dateFrancaise(valeur)
                            "TIME" -> dernier.heure = valeur
                            "PLAC" -> { val cd = couperLieu(valeur); dernier.lieu = cd.first; dernier.dept = cd.second }
                        }
                    }
                }
            } else if (fam != null && blocActuel == "MARR") {
                when (tag) {
                    "DATE" -> fam.marrD = dateFrancaise(valeur)
                    "PLAC" -> { val cd = couperLieu(valeur); fam.marrL = cd.first; fam.marrDept = cd.second }
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
                sosa = ind.rfn,
                notesLibres = ind.note,
                dateNaissance = ind.nD, heureNaissance = ind.nH, lieuNaissance = ind.nL, deptNaissance = ind.nDpt,
                dateBapteme = ind.bD, heureBapteme = ind.bH, lieuBapteme = ind.bL, deptBapteme = ind.bDpt,
                dateMariage = ind.mD, heureMariage = ind.mH, lieuMariage = ind.mL, deptMariage = ind.mDpt,
                dateDeces = ind.dD, heureDeces = ind.dH, lieuDeces = ind.dL, deptDeces = ind.dDpt,
                dateSepulture = ind.sD, heureSepulture = ind.sH, lieuSepulture = ind.sL, deptSepulture = ind.sDpt,
                // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements libres relus (ids s\u00E9quentiels).
                evenementsLibres = mutableListOf<EvenementLibre>().also { l ->
                    ind.evLibres.forEachIndexed { i, ev -> l.add(ev.copy(id = (i + 1).toLong())) }
                }
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
            if (ph.deptMariage.isEmpty() && fam.marrDept.isNotEmpty()) ph.deptMariage = fam.marrDept
            if (pw.lieuMariage.isEmpty() && fam.marrL.isNotEmpty()) pw.lieuMariage = fam.marrL
            if (pw.deptMariage.isEmpty() && fam.marrDept.isNotEmpty()) pw.deptMariage = fam.marrDept
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

    // G2-04 : rapport de ce qui a ete ignore (noms de balises seulement).
    enregistrementsIgnores.forEach { (balise, n) ->
        ignoresRapport.add(n.toString() + " enregistrement(s) " + balise + " non import\u00E9(s)")
    }
    balisesIgnorees.forEach { (balise, n) ->
        ignoresRapport.add(n.toString() + " balise(s) " + balise + " non import\u00E9e(s)")
    }
    return ResultatImport(personnes, ignoresRapport)
}

fun importerGedcom(texte: String): List<Personne> = importerGedcomRapporte(texte).personnes







