package com.plime.geneapoche

// ---------------------------------------------------------------- Geographie (v0.93, chantier 12.5)
// Repartition geographique des ancetres : par departement (damier de
// France) et par commune, plus le "voyage de la lignee" (frise des lieux
// d'une personne). Toutes les fonctions sont PURES et hors ligne : elles
// exploitent le code INSEE des lieux ("Commune, code, Departement") et
// le departement interne (v0.91 : deduit automatiquement du code INSEE,
// jamais saisi).

// Un lieu de vie "significant" : un des 5 evenements ou un evenement libre.
data class EtapeVoyage(
    val intitule: String, // "Naissance", "Mariage"...
    val date: String,
    val lieu: String, // lieu AFFICHE (sans code INSEE)
    val dept: String
)

// Departement d'un lieu d'ancienne saisie ("Roubaix, 59, Nord") : le
// segment numerique court (2-3 chiffres) ou la Corse "2A"/"2B".
fun deptLegacyDuLieu(l: String): String {
    sansPays(l).split(",").map { it.trim() }.forEach { seg ->
        if (Regex("^\\d{2,3}$").matches(seg)) return normaliserDept(seg)
        if (seg == "2A" || seg == "2B") return seg
    }
    return ""
}

// Departement d'une personne : naissance d'abord (le lieu ou l'ancetre
// est ne est le plus parlant), puis bapteme, mariage, deces, sepulture,
// puis evenements libres. Chaque fois : code INSEE du lieu prioritaire,
// ancienne saisie a l'ancienne, departement interne stocke en secours.
fun deptDePersonne(p: Personne): String {
    val couples = listOf(
        p.lieuNaissance to p.deptNaissance,
        p.lieuBapteme to p.deptBapteme,
        p.lieuMariage to p.deptMariage,
        p.lieuDeces to p.deptDeces,
        p.lieuSepulture to p.deptSepulture
    ) + p.evenementsLibres.map { it.lieu to it.dept }
    couples.forEach { (lieu, dept) ->
        if (lieu.isNotBlank()) {
            val d = deptDuLieu(lieu).ifEmpty { deptLegacyDuLieu(lieu) }.ifEmpty { normaliserDept(dept) }
            if (d.isNotEmpty()) return d
        }
        if (dept.isNotBlank()) {
            val d = normaliserDept(dept)
            if (d.isNotEmpty()) return d
        }
    }
    return ""
}

// Commune d'un lieu : le segment qui PRECEDE le code INSEE (le lieu-dit
// reste devant, on prend la commune juste avant le code). Sans code
// INSEE (ancienne saisie "Roubaix, 59, Nord"), le premier segment.
fun communeDuLieu(l: String): String {
    val t = sansPays(l).trim()
    if (t.isEmpty()) return ""
    val m = Regex("(^|, )([^,]+), (\\d{5}|2[AB]\\d{3})(,|$)").find(t)
    if (m != null) return m.groupValues[2].trim()
    return t.split(",").first().trim()
}

// Repartition par departement : une personne compte dans SON
// departement de rattachement (deptDePersonne), pas dans tous ceux ou
// elle a vecu. Les personnes sans lieu identifie sont ignorees du
// denombrement (elles restent dans "non localisees").
fun repartitionDepartements(personnes: List<Personne>): Map<String, List<Personne>> {
    val res = mutableMapOf<String, MutableList<Personne>>()
    personnes.forEach { p ->
        val d = deptDePersonne(p)
        if (d.isNotEmpty()) {
            res.getOrPut(d) { mutableListOf() }.add(p)
        }
    }
    return res
}

// Repartition par commune (meme regle : lieu de naissance d'abord).
fun repartitionCommunes(personnes: List<Personne>): Map<String, List<Personne>> {
    val res = mutableMapOf<String, MutableList<Personne>>()
    personnes.forEach { p ->
        val lieu = p.lieuNaissance.ifEmpty { p.lieuBapteme }.ifEmpty { p.lieuMariage }
            .ifEmpty { p.lieuDeces }.ifEmpty { p.lieuSepulture }
            .ifEmpty { p.evenementsLibres.firstOrNull { it.lieu.isNotBlank() }?.lieu ?: "" }
        val c = communeDuLieu(lieu)
        if (c.isNotEmpty()) {
            res.getOrPut(c) { mutableListOf() }.add(p)
        }
    }
    return res
}

// Voyage d'une personne : ses lieux dans l'ordre chronologique.
// Les dates hors format sont rangees par l'annee 4 chiffres extraite,
// sinon en fin de liste (ordre de saisie).
fun etapesVoyage(p: Personne): List<EtapeVoyage> {
    val brutes = mutableListOf<EtapeVoyage>()
    if (p.lieuNaissance.isNotBlank() || p.dateNaissance.isNotBlank())
        brutes.add(EtapeVoyage("Naissance", p.dateNaissance, lieuAffiche(p.lieuNaissance), deptDuLieu(p.lieuNaissance).ifEmpty { normaliserDept(p.deptNaissance) }))
    if (p.lieuBapteme.isNotBlank() || p.dateBapteme.isNotBlank())
        brutes.add(EtapeVoyage("Bapt\u00EAme", p.dateBapteme, lieuAffiche(p.lieuBapteme), deptDuLieu(p.lieuBapteme).ifEmpty { normaliserDept(p.deptBapteme) }))
    if (p.lieuMariage.isNotBlank() || p.dateMariage.isNotBlank())
        brutes.add(EtapeVoyage("Mariage", p.dateMariage, lieuAffiche(p.lieuMariage), deptDuLieu(p.lieuMariage).ifEmpty { normaliserDept(p.deptMariage) }))
    if (p.lieuDeces.isNotBlank() || p.dateDeces.isNotBlank())
        brutes.add(EtapeVoyage("D\u00E9c\u00E8s", p.dateDeces, lieuAffiche(p.lieuDeces), deptDuLieu(p.lieuDeces).ifEmpty { normaliserDept(p.deptDeces) }))
    if (p.lieuSepulture.isNotBlank() || p.dateSepulture.isNotBlank())
        brutes.add(EtapeVoyage("S\u00E9pulture", p.dateSepulture, lieuAffiche(p.lieuSepulture), deptDuLieu(p.lieuSepulture).ifEmpty { normaliserDept(p.deptSepulture) }))
    p.evenementsLibres.forEach { ev ->
        if (ev.lieu.isNotBlank() || ev.date.isNotBlank())
            brutes.add(EtapeVoyage(ev.intitule, ev.date, lieuAffiche(ev.lieu), deptDuLieu(ev.lieu).ifEmpty { normaliserDept(ev.dept) }))
    }
    fun anneeDe(s: String): Int? = Regex("(\\d{4})").find(s)?.groupValues?.[1]?.toIntOrNull()
    return brutes.sortedWith(compareBy({ anneeDe(it.date) ?: 9999 }, { it.intitule }))
}

// Ordre de lecture du damier metropolitain puis d'outre-mer.
val CodesDepartements: List<String> = listOf(
    "01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19",
    "2A","2B","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37",
    "38","39","40","41","42","43","44","45","46","47","48","49","50","51","52","53","54","55","56",
    "57","58","59","60","61","62","63","64","65","66","67","68","69","70","71","72","73","74","75",
    "76","77","78","79","80","81","82","83","84","85","86","87","88","89","90","91","92","93","94","95",
    "971","972","973","974","976"
)

// Nom affichable d'un departement a partir de son code ("17" ->
// "Charente-Maritime"), vide si inconnu. La Corse (2A/2B) est couverte
// par BaseCommunes.nomDept.
fun nomDepartement(code: String): String = BaseCommunes.nomDept(code)
