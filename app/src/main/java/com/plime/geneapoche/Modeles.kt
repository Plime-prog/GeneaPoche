package com.plime.geneapoche

import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------- Modele Personne
// Convention : id = 0 signifie "non renseigne" pour les references (pere, mere, conjoint).
// Sosa : numerotation Sosa-Stradonitz (de cujus = 1, pere = 2n, mere = 2n+1).
// Lateral : numerotation d'Aboville pour les descendants (ex "1.3").
// Etat : "" (neutre), "vert" (verifie), "rouge" (a verifier) par section de la fiche.
// Sections : identite, naissance, bapteme, mariage, deces, sepulture.

// ---------------------------------------------------- Noms et prenoms systematiques
// Nom de famille : TOUJOURS en majuscules ("dupont" -> "DUPONT").
// Prenoms : seule l'initiale de chaque element en majuscule, y compris
// apres un tiret ou une apostrophe ("marie-anne" -> "Marie-Anne",
// "JEAN BAPTISTE" -> "Jean Baptiste").
fun normaliserNom(s: String): String = s.trim().uppercase()

// G\u00E9n\u00E9aPoche 5 : commune transcrite avec une initiale en MAJUSCULES
// (norme fran\u00E7aise) : chaque mot est en minuscules sauf sa premiere
// lettre, y compris apres un tiret ou une apostrophe
// ("le havre" -> "Le Havre", "LA ROCHE-SUR-YON" -> "La Roche-sur-Yon").
fun normaliserCommune(s: String): String {
    val t = s.trim().lowercase()
    if (t.isEmpty()) return ""
    val sb = StringBuilder()
    var capitale = true
    t.forEach { c ->
        if (c == ' ' || c == '-' || c == '\'') {
            capitale = true
            sb.append(c)
        } else if (capitale) {
            sb.append(c.uppercaseChar())
            capitale = false
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}

// G\u00E9n\u00E9aPoche 5 : d\u00E9partement au format NUM\u00C9RIQUE (norme GEDCOM),
// 2 ou 3 chiffres : "5" -> "05", "Nord" -> "59", "971" -> "971".
// Une valeur non identifiable est laiss\u00E9e telle quelle.
fun normaliserDept(s: String): String {
    val t = s.trim()
    if (t.isEmpty()) return ""
    val n = t.toIntOrNull()
    if (n != null) {
        if (NomsDepartements.containsKey(n)) {
            return if (n < 10) "0" + n.toString() else n.toString()
        }
        return t
    }
    val num = numeroDepartement(t)
    if (num != null) return num.toString()
    return t
}

// G\u00E9n\u00E9aPoche 5 : applique les deux normes (communes, d\u00E9partements)
// aux cinq \u00E9v\u00E9nements d'une personne. Appele a TOUTE construction
// (chargement, import, creation) ET a chaque enregistrement.
fun normaliserLieux(p: Personne) {
    // v0.71 : ZERO D'OFFICE sur les dates et heures des cinq evenements
    // et des evenements libres. Appele a toute construction et a chaque
    // enregistrement : les dates incompletes deja presentes dans la base
    // (ancien GEDCOM, champ libre) se reparent d'elles-memes, pour de bon.
    p.dateNaissance = normaliserDate(p.dateNaissance); p.heureNaissance = normaliserHeure(p.heureNaissance)
    p.dateBapteme = normaliserDate(p.dateBapteme); p.heureBapteme = normaliserHeure(p.heureBapteme)
    p.dateMariage = normaliserDate(p.dateMariage); p.heureMariage = normaliserHeure(p.heureMariage)
    p.dateDeces = normaliserDate(p.dateDeces); p.heureDeces = normaliserHeure(p.heureDeces)
    p.dateSepulture = normaliserDate(p.dateSepulture); p.heureSepulture = normaliserHeure(p.heureSepulture)
    p.evenementsLibres.forEach { ev ->
        ev.date = normaliserDate(ev.date); ev.heure = normaliserHeure(ev.heure)
    }
    p.lieuNaissance = normaliserCommune(p.lieuNaissance)
    p.deptNaissance = normaliserDept(p.deptNaissance)
    p.lieuBapteme = normaliserCommune(p.lieuBapteme)
    p.deptBapteme = normaliserDept(p.deptBapteme)
    p.lieuMariage = normaliserCommune(p.lieuMariage)
    p.deptMariage = normaliserDept(p.deptMariage)
    p.lieuDeces = normaliserCommune(p.lieuDeces)
    p.deptDeces = normaliserDept(p.deptDeces)
    p.lieuSepulture = normaliserCommune(p.lieuSepulture)
    p.deptSepulture = normaliserDept(p.deptSepulture)
}

fun normaliserPrenoms(s: String): String {
    val t = StringBuilder()
    var majuscule = true
    for (c in s.trim()) {
        if (c == ' ' || c == '-' || c == '\'') {
            t.append(c)
            majuscule = true
        } else {
            t.append(if (majuscule) c.uppercaseChar() else c.lowercaseChar())
            majuscule = false
        }
    }
    return t.toString()
}

// ---------------------------------------------------- Calendrier republicain
// Convertisseur calendrier republicain -> gregorien (annees I a XIV,
// du 22/09/1792 au 31/12/1805, bornes reelles observees a Paris).
// Mois : Vendemiaire, Brumaire, Frimaire, Nivose, Pluviose, Ventose,
// Germinal, Floreal, Prairial, Messidor, Thermidor, Fructidor, puis
// jours complementaires (1 a 6 selon l'an). Saisie acceptee :
// "22 Germinal An II", "22 germinal 2", "3 complementaire an III"...
// Renvoie "jj/mm/aaaa" gregorien, ou null si non convertible.
val MoisRepubli = listOf(
    "vendemiaire", "brumaire", "frimaire", "nivose", "pluviose", "ventose",
    "germinal", "floreal", "prairial", "messidor", "thermidor", "fructidor"
)
val RomainsRepubli = listOf(
    "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
    "XI", "XII", "XIII", "XIV"
)
// Debut gregorien (jour, mois, annee) de chaque annee republicaine :
// l'an debute a l'equinoxe d'automne observe a Paris (donnee historique).
val DebutAnRepubli = listOf(
    Triple(22, 9, 1792), Triple(22, 9, 1793), Triple(22, 9, 1794), Triple(23, 9, 1795),
    Triple(22, 9, 1796), Triple(22, 9, 1797), Triple(22, 9, 1798), Triple(23, 9, 1799),
    Triple(23, 9, 1800), Triple(23, 9, 1801), Triple(23, 9, 1802), Triple(23, 9, 1803),
    Triple(22, 9, 1804), Triple(23, 9, 1805)
)

fun anneeRepubli(s: String): Int? {
    val t = s.trim().uppercase()
    if (t.isEmpty()) return null
    RomainsRepubli.forEachIndexed { i, r -> if (t == r) return i + 1 }
    val n = t.toIntOrNull() ?: return null
    if (n in 1..14) return n
    // Saisie de l'annee gregorienne equivalente (1792 -> an I).
    if (n in 1792..1805) return n - 1791
    return null
}

fun moisRepubli(s: String): Int? {
    val t = s.trim().lowercase().replace("[^a-z]".toRegex(), "")
    if (t.length < 3) return null
    MoisRepubli.forEachIndexed { i, m -> if (m.startsWith(t)) return i + 1 }
    // Jours complementaires (sansculottides) : "complementaire", "sansculottide".
    if (t.startsWith("com") || t.startsWith("san") || t.startsWith("jou")) return 13
    return null
}

fun repubAGregorien(s: String): String? {
    val t = s.trim()
    if (t.isEmpty()) return null
    // "22 germinal an II" : jour, mois (lettres), puis "an" facultatif,
    // numero d'an en chiffres arabes ou romains.
    val m = Regex("^(\\d{1,2})\\s+(\\p{L}+)\\s+(?:an\\s+)?(\\p{L}+|\\d{1,4})$", RegexOption.IGNORE_CASE).find(t) ?: return null
    val jour = m.groupValues[1].toIntOrNull() ?: return null
    val mois = moisRepubli(m.groupValues[2]) ?: return null
    val an = anneeRepubli(m.groupValues[3]) ?: return null
    if (jour < 1 || jour > 30) return null
    if (mois == 13 && jour > 6) return null
    val debut = DebutAnRepubli[an - 1]
    val base = java.time.LocalDate.of(debut.third, debut.second, debut.first)
    val res = base.plusDays(((mois - 1) * 30 + jour - 1).toLong())
    // Bornes : la date doit rester DANS l'an (le 6e jour complementaire
    // n'existe que les ans sextiles III, VII, XI ; l'an XIV s'arrete
    // au 31/12/1805, fin historique du calendrier).
    val fin = if (an < 14) {
        val d = DebutAnRepubli[an]
        java.time.LocalDate.of(d.third, d.second, d.first).minusDays(1)
    } else {
        java.time.LocalDate.of(1805, 12, 31)
    }
    if (res.isBefore(base) || res.isAfter(fin)) return null
    return res.dayOfMonth.toString().padStart(2, '0') + "/" +
        res.monthValue.toString().padStart(2, '0') + "/" + res.year.toString()
}

// Date systematique : une saisie republicaine est GARDEE TELLE QUELLE
// (l'affichage la complete de sa conversion gregorienne), toute autre
// valeur est conservee telle quelle (jj/mm/aaaa ou aaaa).
// v0.71 : toute date d'acte enregistree recoit le ZERO D'OFFICE :
// "5/7/1890" devient "05/07/1890" - le format jj/mm/aaaa tient apres
// sauvegarde, export et reimport GEDCOM.
fun dateGreg(s: String): String = normaliserDate(s.trim())

// G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nement \u00E0 intitul\u00E9 LIBRE (profession,
// d\u00E9m\u00E9nagement\u2026) rang\u00E9 dans la chronologie de la fiche.
data class EvenementLibre(
    var id: Long = 0L,
    var intitule: String = "",
    var date: String = "",
    var heure: String = "",
    var lieu: String = "",
    var dept: String = ""
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("intitule", intitule)
        o.put("date", date)
        o.put("heure", heure)
        o.put("lieu", lieu)
        o.put("dept", dept)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): EvenementLibre = EvenementLibre(
            id = o.optLong("id", 0L),
            intitule = o.optString("intitule", ""),
            date = o.optString("date", ""),
            heure = o.optString("heure", ""),
            lieu = o.optString("lieu", ""),
            dept = o.optString("dept", "")
        )
    }
}

data class Personne(
    val id: Long,
    var sosa: String = "",
    var lateral: String = "",
    var nom: String = "",
    var prenoms: String = "",
    var sexe: String = "",
    var profession: String = "",
    // Bloc sans titre de saisie libre sur la fiche (notes personnelles).
    var notesLibres: String = "",
    // G\u00E9n\u00E9aPoche 11 (11.0) : certification de l'identit\u00E9.
    // "auto" = coche si au moins 1 renseignement sur 6, croix sinon ;
    // "coche" / "croix" = choix manuel (possible m\u00EAme si incomplet).
    var identiteVerifiee: String = "auto",
    var dateNaissance: String = "",
    var heureNaissance: String = "",
    var lieuNaissance: String = "",
    var deptNaissance: String = "",
    var dateBapteme: String = "",
    var heureBapteme: String = "",
    var lieuBapteme: String = "",
    var deptBapteme: String = "",
    var dateMariage: String = "",
    var heureMariage: String = "",
    var lieuMariage: String = "",
    var deptMariage: String = "",
    var dateDeces: String = "",
    var heureDeces: String = "",
    var lieuDeces: String = "",
    var deptDeces: String = "",
    var dateSepulture: String = "",
    var heureSepulture: String = "",
    var lieuSepulture: String = "",
    var deptSepulture: String = "",
    var etatIdentite: String = "",
    var etatNaissance: String = "",
    var etatBapteme: String = "",
    var etatMariage: String = "",
    var etatDeces: String = "",
    var etatSepulture: String = "",
    var pere: Long = 0L,
    var mere: Long = 0L,
    var conjoint: Long = 0L,
    var conjointsSupp: MutableList<Long> = mutableListOf(),
    var enfants: MutableList<Long> = mutableListOf(),
    // Blocs facultatifs crees par l'utilisateur : "bapteme", "sepulture"
    var blocsOuverts: MutableList<String> = mutableListOf(),
    // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements \u00E0 intitul\u00E9 libre (profession,
    // d\u00E9m\u00E9nagement, etc.) rang\u00E9s dans la chronologie de la fiche.
    var evenementsLibres: MutableList<EvenementLibre> = mutableListOf(),
    // Temoins de chaque acte (ids de fiches). Aucun par defaut.
    var temoinsN: MutableList<Long> = mutableListOf(),
    var temoinsB: MutableList<Long> = mutableListOf(),
    var temoinsM: MutableList<Long> = mutableListOf(),
    var temoinsD: MutableList<Long> = mutableListOf(),
    var temoinsS: MutableList<Long> = mutableListOf(),
    // Signataires de l'acte : 0L = le pere, sinon l'id d'un temoin.
    var signatairesN: MutableList<Long> = mutableListOf(),
    var signatairesB: MutableList<Long> = mutableListOf(),
    var signatairesM: MutableList<Long> = mutableListOf(),
    var signatairesD: MutableList<Long> = mutableListOf(),
    var signatairesS: MutableList<Long> = mutableListOf()
) {
    // Normalisation systematique a TOUTE construction (chargement, import,
    // creation) : nom en majuscules, prenoms a l'initiale pres, dates
    // republicaines GARDEES TELLES QUELLES (conversion gregorienne affichee,
    // jamais stockee).
    init {
        nom = normaliserNom(nom)
        prenoms = normaliserPrenoms(prenoms)
        // G\u00E9n\u00E9aPoche 5 : communes (initiale en majuscules) et
        // d\u00E9partements (format num\u00E9rique) normalis\u00E9s partout.
        normaliserLieux(this)
        dateNaissance = dateGreg(dateNaissance)
        dateBapteme = dateGreg(dateBapteme)
        dateMariage = dateGreg(dateMariage)
        dateDeces = dateGreg(dateDeces)
        dateSepulture = dateGreg(dateSepulture)
    }

    fun titre(): String {
        return listOf(nom.trim(), prenoms.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    // Numero affichable SANS etiquette : juste le numero ("12" ou "7.2").
    // Les mots Sosa et d'Aboville ne restent que dans les menus (de cujus, tri).
    fun numeroAffiche(): String {
        val s = sosa.trim()
        if (s.isNotEmpty()) return s
        return lateral.trim()
    }

    fun numeroBrut(): String {
        val s = sosa.trim()
        if (s.isNotEmpty()) return s
        return lateral.trim()
    }

    // Temoins et signataires selon le type d'acte (N/B/M/D/S).
    fun temoinsDe(type: String): MutableList<Long> = when (type) {
        "N" -> temoinsN
        "B" -> temoinsB
        "M" -> temoinsM
        "S" -> temoinsS
        else -> temoinsD
    }

    fun signatairesDe(type: String): MutableList<Long> = when (type) {
        "N" -> signatairesN
        "B" -> signatairesB
        "M" -> signatairesM
        "S" -> signatairesS
        else -> signatairesD
    }

    // v0.83 (12.0) : les actes ne sont plus stockés dans l'application ; une
    // coche « Enregistré » PAR TYPE d'événement signale un acte conservé
    // ailleurs. Le journal recherche son absence comme un acte manquant.
    var enregistreN: Boolean = false
    var enregistreB: Boolean = false
    var enregistreM: Boolean = false
    var enregistreS: Boolean = false
    var enregistreD: Boolean = false
    // v0.83 : état EN VIE / DÉCÉDÉ du conjoint au décès (persisté sur la
    // fiche, plus dans une transcription d'acte).
    var conjointEnVieDeces: Boolean = true

    fun enregistreDe(type: String): Boolean = when (type) {
        "N" -> enregistreN
        "B" -> enregistreB
        "M" -> enregistreM
        "S" -> enregistreS
        "D" -> enregistreD
        else -> false
    }

    fun definirEnregistre(type: String, valeur: Boolean) {
        when (type) {
            "N" -> enregistreN = valeur
            "B" -> enregistreB = valeur
            "M" -> enregistreM = valeur
            "S" -> enregistreS = valeur
            "D" -> enregistreD = valeur
        }
    }

    fun aVerifier(): Boolean {
        return etatIdentite == "rouge" || etatNaissance == "rouge" ||
            etatBapteme == "rouge" || etatMariage == "rouge" ||
            etatDeces == "rouge" || etatSepulture == "rouge"
    }

    fun etatDe(section: String): String = when (section) {
        "identite" -> etatIdentite
        "naissance" -> etatNaissance
        "bapteme" -> etatBapteme
        "mariage" -> etatMariage
        "sepulture" -> etatSepulture
        else -> etatDeces
    }

    fun changerEtat(section: String, valeur: String) {
        when (section) {
            "identite" -> etatIdentite = valeur
            "naissance" -> etatNaissance = valeur
            "bapteme" -> etatBapteme = valeur
            "mariage" -> etatMariage = valeur
            "sepulture" -> etatSepulture = valeur
            else -> etatDeces = valeur
        }
    }

    fun tousConjoints(): List<Long> {
        val res = mutableListOf<Long>()
        if (conjoint != 0L) res.add(conjoint)
        conjointsSupp.forEach { if (it != 0L && !res.contains(it)) res.add(it) }
        return res
    }

    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("sosa", sosa)
        o.put("lateral", lateral)
        o.put("nom", nom)
        o.put("prenoms", prenoms)
        o.put("sexe", sexe)
        o.put("profession", profession)
        o.put("notesLibres", notesLibres)
        o.put("identiteVerifiee", identiteVerifiee)
        o.put("dateNaissance", dateNaissance)
        o.put("heureNaissance", heureNaissance)
        o.put("lieuNaissance", lieuNaissance)
        o.put("deptNaissance", deptNaissance)
        o.put("dateBapteme", dateBapteme)
        o.put("heureBapteme", heureBapteme)
        o.put("lieuBapteme", lieuBapteme)
        o.put("deptBapteme", deptBapteme)
        o.put("dateMariage", dateMariage)
        o.put("heureMariage", heureMariage)
        o.put("lieuMariage", lieuMariage)
        o.put("deptMariage", deptMariage)
        o.put("dateDeces", dateDeces)
        o.put("heureDeces", heureDeces)
        o.put("lieuDeces", lieuDeces)
        o.put("deptDeces", deptDeces)
        o.put("dateSepulture", dateSepulture)
        o.put("heureSepulture", heureSepulture)
        o.put("lieuSepulture", lieuSepulture)
        o.put("deptSepulture", deptSepulture)
        o.put("etatIdentite", etatIdentite)
        o.put("etatNaissance", etatNaissance)
        o.put("etatBapteme", etatBapteme)
        o.put("etatMariage", etatMariage)
        o.put("etatDeces", etatDeces)
        o.put("etatSepulture", etatSepulture)
        o.put("pere", pere)
        o.put("mere", mere)
        o.put("conjoint", conjoint)
        val supp = JSONArray()
        conjointsSupp.forEach { supp.put(it) }
        o.put("conjointsSupp", supp)
        val tableau = JSONArray()
        enfants.forEach { tableau.put(it) }
        o.put("enfants", tableau)
        val blocs = JSONArray()
        blocsOuverts.forEach { blocs.put(it) }
        o.put("blocsOuverts", blocs)
        // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements libres de la chronologie.
        val evLibres = JSONArray()
        evenementsLibres.forEach { evLibres.put(it.versJson()) }
        o.put("evenementsLibres", evLibres)
        fun ecrire(nom: String, liste: List<Long>) {
            val t = JSONArray()
            liste.forEach { t.put(it) }
            o.put(nom, t)
        }
        ecrire("temoinsN", temoinsN)
        ecrire("temoinsB", temoinsB)
        ecrire("temoinsM", temoinsM)
        ecrire("temoinsD", temoinsD)
        ecrire("temoinsS", temoinsS)
        ecrire("signatairesN", signatairesN)
        ecrire("signatairesB", signatairesB)
        ecrire("signatairesM", signatairesM)
        ecrire("signatairesD", signatairesD)
        ecrire("signatairesS", signatairesS)
        o.put("enregistreN", enregistreN)
        o.put("enregistreB", enregistreB)
        o.put("enregistreM", enregistreM)
        o.put("enregistreS", enregistreS)
        o.put("enregistreD", enregistreD)
        o.put("conjointEnVieDeces", conjointEnVieDeces)
        return o
    }

    companion object {
        // Lit un tableau JSON de nombres (temoins, signataires).
        fun lireLongs(o: JSONObject, cle: String): MutableList<Long> {
            val t = o.optJSONArray(cle) ?: return mutableListOf()
            val r = mutableListOf<Long>()
            for (i in 0 until t.length()) {
                r.add(t.optLong(i, 0L))
            }
            return r
        }

        fun depuisJson(o: JSONObject): Personne {
            val ancienneNaissance = o.optString("naissance", "")
            val ancienDeces = o.optString("deces", "")
            val p = Personne(
                id = o.optLong("id", 0L),
                sosa = o.optString("sosa", ""),
                lateral = o.optString("lateral", ""),
                nom = o.optString("nom", ""),
                prenoms = o.optString("prenoms", o.optString("prenom", "")),
                sexe = o.optString("sexe", ""),
                profession = o.optString("profession", ""),
                notesLibres = o.optString("notesLibres", ""),
                identiteVerifiee = o.optString("identiteVerifiee", "auto"),
                dateNaissance = o.optString("dateNaissance", ""),
                heureNaissance = o.optString("heureNaissance", ""),
                lieuNaissance = o.optString("lieuNaissance", ancienneNaissance),
                deptNaissance = o.optString("deptNaissance", ""),
                dateBapteme = o.optString("dateBapteme", ""),
                heureBapteme = o.optString("heureBapteme", ""),
                lieuBapteme = o.optString("lieuBapteme", ""),
                deptBapteme = o.optString("deptBapteme", ""),
                dateMariage = o.optString("dateMariage", ""),
                heureMariage = o.optString("heureMariage", ""),
                lieuMariage = o.optString("lieuMariage", ""),
                deptMariage = o.optString("deptMariage", ""),
                dateDeces = o.optString("dateDeces", ""),
                heureDeces = o.optString("heureDeces", ""),
                lieuDeces = o.optString("lieuDeces", ancienDeces),
                deptDeces = o.optString("deptDeces", ""),
                dateSepulture = o.optString("dateSepulture", ""),
                heureSepulture = o.optString("heureSepulture", ""),
                lieuSepulture = o.optString("lieuSepulture", ""),
                deptSepulture = o.optString("deptSepulture", ""),
                etatIdentite = o.optString("etatIdentite", ""),
                etatNaissance = o.optString("etatNaissance", ""),
                etatBapteme = o.optString("etatBapteme", ""),
                etatMariage = o.optString("etatMariage", ""),
                etatDeces = o.optString("etatDeces", ""),
                etatSepulture = o.optString("etatSepulture", ""),
                pere = o.optLong("pere", 0L),
                mere = o.optLong("mere", 0L),
                conjoint = o.optLong("conjoint", 0L),
                conjointsSupp = mutableListOf<Long>().apply {
                    val supp = o.optJSONArray("conjointsSupp")
                    if (supp != null) {
                        for (i in 0 until supp.length()) {
                            val v = supp.optLong(i, 0L)
                            if (v != 0L) add(v)
                        }
                    }
                },
                enfants = mutableListOf<Long>().apply {
                    val tableau = o.optJSONArray("enfants")
                    if (tableau != null) {
                        for (i in 0 until tableau.length()) {
                            add(tableau.optLong(i, 0L))
                        }
                    }
                },
                blocsOuverts = mutableListOf<String>().apply {
                    val blocs = o.optJSONArray("blocsOuverts")
                    if (blocs != null) {
                        for (i in 0 until blocs.length()) {
                            add(blocs.optString(i, ""))
                        }
                    }
                },
                evenementsLibres = mutableListOf<EvenementLibre>().apply {
                    val evLibres = o.optJSONArray("evenementsLibres")
                    if (evLibres != null) {
                        for (i in 0 until evLibres.length()) {
                            val eo = evLibres.optJSONObject(i)
                            if (eo != null) add(EvenementLibre.depuisJson(eo))
                        }
                    }
                },
                temoinsN = lireLongs(o, "temoinsN"),
                temoinsB = lireLongs(o, "temoinsB"),
                temoinsM = lireLongs(o, "temoinsM"),
                temoinsD = lireLongs(o, "temoinsD"),
                temoinsS = lireLongs(o, "temoinsS"),
                signatairesN = lireLongs(o, "signatairesN"),
                signatairesB = lireLongs(o, "signatairesB"),
                signatairesM = lireLongs(o, "signatairesM"),
                signatairesD = lireLongs(o, "signatairesD"),
                signatairesS = lireLongs(o, "signatairesS")
            )
            // v0.83 : coches « Enregistré » (corps de classe, pas constructeur).
            p.enregistreN = o.optBoolean("enregistreN", false)
            p.enregistreB = o.optBoolean("enregistreB", false)
            p.enregistreM = o.optBoolean("enregistreM", false)
            p.enregistreS = o.optBoolean("enregistreS", false)
            p.enregistreD = o.optBoolean("enregistreD", false)
            p.conjointEnVieDeces = o.optBoolean("conjointEnVieDeces", true)
            return p
        }
    }
}

// ---------------------------------------------------------------- Modele Piste (journal de recherches)

// Compte-rendu horodate d'une recherche effectuee aux archives :
// "le JJ/MM/AAAA : rien trouve, jusqu'a la page X. Reprendre les TD de VILLE a partir de ANNEE."
data class Recherche(
    val date: Long = 0L,
    val resultat: String = "R", // "R" rien, "T" trouve
    val page: String = "",
    val repriseVille: String = "",
    val repriseAnnee: String = "",
    val note: String = ""
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("date", date)
        o.put("resultat", resultat)
        o.put("page", page)
        o.put("repriseVille", repriseVille)
        o.put("repriseAnnee", repriseAnnee)
        o.put("note", note)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Recherche = Recherche(
            date = o.optLong("date", 0L),
            resultat = o.optString("resultat", "R"),
            page = o.optString("page", ""),
            repriseVille = o.optString("repriseVille", ""),
            repriseAnnee = o.optString("repriseAnnee", ""),
            note = o.optString("note", "")
        )
    }
}

data class Piste(
    val id: Long,
    var texte: String = "",
    var personneId: Long = 0L,
    var creation: Long = 0L,
    var faite: Boolean = false,
    var cloture: Long = 0L,
    var type: String = "P", // "P" piste libre, "A" issue d'une suggestion
    var categorie: String = "", // categorie de suggestion ("N", "BP", "M", "D", "S", "P", "ME")
    var lieu: String = "",
    var dept: String = "",
    var debut: String = "",
    var fin: String = "",
    var enCours: String = "", // ce qui est en cours de recherche (bloc c)
    var historique: MutableList<Recherche> = mutableListOf()
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("texte", texte)
        o.put("personneId", personneId)
        o.put("creation", creation)
        o.put("faite", faite)
        o.put("cloture", cloture)
        o.put("type", type)
        o.put("categorie", categorie)
        o.put("lieu", lieu)
        o.put("dept", dept)
        o.put("debut", debut)
        o.put("fin", fin)
        o.put("enCours", enCours)
        val tableau = JSONArray()
        historique.forEach { tableau.put(it.versJson()) }
        o.put("historique", tableau)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Piste = Piste(
            id = o.optLong("id", 0L),
            texte = o.optString("texte", ""),
            personneId = o.optLong("personneId", 0L),
            creation = o.optLong("creation", 0L),
            faite = o.optBoolean("faite", false),
            cloture = o.optLong("cloture", 0L),
            type = o.optString("type", "P"),
            categorie = o.optString("categorie", ""),
            lieu = o.optString("lieu", ""),
            dept = o.optString("dept", ""),
            debut = o.optString("debut", ""),
            fin = o.optString("fin", ""),
            enCours = o.optString("enCours", ""),
            historique = mutableListOf<Recherche>().apply {
                val tableau = o.optJSONArray("historique")
                if (tableau != null) {
                    for (i in 0 until tableau.length()) {
                        add(Recherche.depuisJson(tableau.getJSONObject(i)))
                    }
                }
            }
        )
    }
}








