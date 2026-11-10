package com.plime.geneapoche

// ================================================================ v0.90
// Sources et citations <-> GEDCOM 5.5.1 (compatible Heredis).
//
// Export :
//   0 @S1@ SOUR                      1 TITL titre
//   1 REPO @R1@ / 2 CALN cote        (depot et cote)
//   1 DATA / 2 EVEN BIRT / 3 DATE 1872 / 3 PLAC Bourges, 18, Cher
//   1 TEXT transcription             1 NOTE notes (+ "URL : ...")
//   0 @R1@ REPO / 1 NAME depot
//   citation, sous l'evenement :
//   2 SOUR @S1@ / 3 PAGE acte n 43, vue 17/312 / 3 QUAY 3 /
//   3 DATA / 4 TEXT extrait / 3 NOTE notes
// Les champs propres a GeneaPoche sans equivalent GEDCOM (type exact,
// numero d'acte, vue, total de vues) sont ajoutes en balises "_GP..." :
// les autres logiciels les ignorent, GeneaPoche les relit a l'identique.
// Logique PURE : testee en JVM.

private const val DEPOT_INCONNU = "D\u00e9p\u00f4t non pr\u00e9cis\u00e9"

fun xrefSource(id: Long): String = "@S" + id.toString() + "@"

fun tagTypeSource(type: String): String = when (type) {
    "naissance" -> "BIRT"
    "bapteme" -> "BAPM"
    "mariage" -> "MARR"
    "deces" -> "DEAT"
    "sepulture" -> "BURI"
    "recensement" -> "CENS"
    else -> "EVEN"
}

fun typeDepuisTag(tag: String): String = when (tag.trim().uppercase()) {
    "BIRT" -> "naissance"
    "BAPM", "CHR", "BAPT" -> "bapteme"
    "MARR" -> "mariage"
    "DEAT" -> "deces"
    "BURI", "BUR" -> "sepulture"
    "CENS" -> "recensement"
    else -> ""
}

// "1872" -> "1872" ; "1860-1872" -> "FROM 1860 TO 1872" ; date complete
// jj/mm/aaaa -> "5 OCT 1872" ; le reste est conserve en NOTE (null).
fun periodeGedcom(p: String): String? {
    val t = p.trim()
    if (t.isEmpty()) return null
    if (Regex("^\\d{4}$").containsMatchIn(t)) return t
    Regex("^(\\d{4})\\s*(?:-|\u2013|\u2014|/|\u00e0|a|au)\\s*(\\d{4})$").find(t)?.let { m ->
        return "FROM " + m.groupValues[1] + " TO " + m.groupValues[2]
    }
    return dateGedcom(t)
}

fun periodeDepuisGedcom(v: String): String {
    val t = v.trim()
    Regex("^(?:FROM\\s+)?(\\d{4})\\s+TO\\s+(\\d{4})$", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return m.groupValues[1] + "-" + m.groupValues[2]
    }
    Regex("^BET\\s+(\\d{4})\\s+AND\\s+(\\d{4})$", RegexOption.IGNORE_CASE).find(t)?.let { m ->
        return m.groupValues[1] + "-" + m.groupValues[2]
    }
    return dateFrancaise(t)
}

// Depots distincts (nom -> pointeur @Rn@), dans l'ordre des sources. Une
// source qui n'a qu'une cote recoit un depot "non precise" (CALN n'existe
// que sous un REPO).
fun depotsGedcom(sources: List<Source>): LinkedHashMap<String, String> {
    val res = LinkedHashMap<String, String>()
    sources.forEach { s ->
        val nom = nomDepot(s)
        if (nom.isNotEmpty() && !res.containsKey(nom)) res[nom] = "@R" + (res.size + 1).toString() + "@"
    }
    return res
}

private fun nomDepot(s: Source): String =
    if (s.depot.isNotBlank()) s.depot.trim() else if (s.cote.isNotBlank()) DEPOT_INCONNU else ""

// Enregistrements SOUR puis REPO (a placer avant le TRLR).
fun lignesEnregistrementsSources(sources: List<Source>): List<String> {
    val l = mutableListOf<String>()
    val depots = depotsGedcom(sources)
    sources.forEach { s ->
        l.add("0 " + xrefSource(s.id) + " SOUR")
        ajouterValeur(l, 1, "TITL", libelleSource(s))
        if (s.type.isNotEmpty()) l.add("1 _GPTYPE " + s.type)
        val nomD = nomDepot(s)
        if (nomD.isNotEmpty()) {
            l.add("1 REPO " + (depots[nomD] ?: ""))
            if (s.cote.isNotBlank()) ajouterValeur(l, 2, "CALN", s.cote.trim())
        }
        val lieu = lieuGedcom(s.commune, s.dept)
        val periode = periodeGedcom(s.periode)
        if (s.type.isNotEmpty() || lieu.isNotEmpty() || periode != null) {
            l.add("1 DATA")
            l.add("2 EVEN " + tagTypeSource(s.type))
            if (periode != null) l.add("3 DATE " + periode)
            if (lieu.isNotEmpty()) ajouterValeur(l, 3, "PLAC", lieu)
        }
        if (s.transcription.isNotBlank()) ajouterNote(l, 1, "TEXT", s.transcription.trim())
        val notes = mutableListOf<String>()
        if (s.notes.isNotBlank()) notes.add(s.notes.trim())
        if (s.url.isNotBlank()) notes.add("URL : " + s.url.trim())
        if (notes.isNotEmpty()) ajouterNote(l, 1, "NOTE", notes.joinToString("\n"))
        if (s.fiabilite in 0..3) l.add("1 _GPQUAY " + s.fiabilite.toString())
    }
    depots.forEach { (nom, ptr) ->
        l.add("0 " + ptr + " REPO")
        ajouterValeur(l, 1, "NAME", nom)
    }
    return l
}

// Citations d'un evenement, au niveau `niveau` (2 sous un evenement, 1 sous
// l'individu).
fun lignesCitations(niveau: Int, citations: List<Citation>, sources: Map<Long, Source>): List<String> {
    val l = mutableListOf<String>()
    citations.forEach { c ->
        val s = sources[c.sourceId] ?: return@forEach
        l.add(niveau.toString() + " SOUR " + xrefSource(s.id))
        val n1 = niveau + 1
        val detail = detailCitation(c)
        if (detail.isNotEmpty()) ajouterValeur(l, n1, "PAGE", detail)
        val q = qualiteEffective(c, s)
        if (q >= 0) l.add(n1.toString() + " QUAY " + q.toString())
        if (c.extrait.isNotBlank()) {
            l.add(n1.toString() + " DATA")
            ajouterNote(l, n1 + 1, "TEXT", c.extrait.trim())
        }
        if (c.notes.isNotBlank()) ajouterNote(l, n1, "NOTE", c.notes.trim())
        if (c.numeroActe.isNotBlank()) ajouterValeur(l, n1, "_GPACTE", c.numeroActe.trim())
        if (c.vue > 0) l.add(n1.toString() + " _GPVUE " + c.vue.toString())
        if (c.vueTotal > 0) l.add(n1.toString() + " _GPVUETOT " + c.vueTotal.toString())
        if (c.page.isNotBlank()) ajouterValeur(l, n1, "_GPPAGE", c.page.trim())
    }
    return l
}

// ---------------------------------------------------------------- Import

class SourcesLues(val sources: List<Source>, val citations: List<Citation>)

private class SourceBrute {
    var titre = ""
    var abbr = ""
    var auteur = ""
    var publication = ""
    var texte = ""
    val notes = mutableListOf<String>()
    var depotRef = ""
    var cote = ""
    var evenCode = ""
    var date = ""
    var plac = ""
    var gpType = ""
    var gpQuay = -1
}

private class CitationBrute(val cible: String, val cibles: List<Long>, val famille: String, val reference: String) {
    var page = ""
    var quay = -1
    val extraits = mutableListOf<String>()
    val notes = mutableListOf<String>()
    var acte = ""
    var vue = 0
    var vueTotal = 0
    var pageLibre = ""
    var dansData = false
}

private fun deArobase(s: String): String = s.replace("@@", "@")

private val FormeLigne = Regex("^(\\d+)\\s+(?:(@[^@]+@)\\s+)?(\\S+)(?:\\s(.*))?$", RegexOption.DOT_MATCHES_ALL)

private fun cibleDeBloc(bloc: String, indexEven: Int): String = when (bloc) {
    "BIRT" -> "N"
    "BAPM", "CHR", "BAPT" -> "B"
    "MARR" -> "M"
    "DEAT" -> "D"
    "BURI", "BUR" -> "S"
    "EVEN" -> "E" + indexEven.toString()
    else -> "P"
}

// `lignes` : le GEDCOM apres recollerConcCont ; `idPersonne` : pointeur INDI
// -> identifiant de fiche. Les evenements libres sont numerotes dans l'ordre
// d'apparition (1, 2, 3...) comme a l'import des personnes.
fun lireSourcesGedcom(lignes: List<String>, idPersonne: Map<String, Long>): SourcesLues {
    val sourcesBrutes = LinkedHashMap<String, SourceBrute>()
    val depots = HashMap<String, String>()
    val notesEnreg = HashMap<String, String>()
    val citationsBrutes = mutableListOf<CitationBrute>()
    val famillesMembres = HashMap<String, MutableList<String>>()

    var rec = ""
    var recXref = ""
    var bloc1 = ""
    var bloc2 = ""
    var indexEven = 0
    var indiCourant = 0L
    var cit: CitationBrute? = null
    var citNiveau = 0
    var sourceCourante: SourceBrute? = null

    fun cloreCitation() {
        cit?.let { citationsBrutes.add(it) }
        cit = null
    }

    fun ouvrirCitation(niveau: Int, cible: String, valeur: String) {
        val cibles = if (rec == "INDI" && indiCourant != 0L) listOf(indiCourant) else emptyList()
        val fam = if (rec == "FAM") recXref else ""
        cit = CitationBrute(cible, cibles, fam, valeur.trim())
        citNiveau = niveau
    }

    for (ligne in lignes) {
        val m = FormeLigne.find(ligne.trim()) ?: continue
        val niveau = m.groupValues[1].toIntOrNull() ?: continue
        val xref = m.groupValues[2]
        val tag = m.groupValues[3]
        val valeur = m.groupValues[4]
        if (niveau == 0) {
            cloreCitation()
            rec = tag
            recXref = xref
            bloc1 = ""
            bloc2 = ""
            indexEven = 0
            indiCourant = 0L
            sourceCourante = null
            when (tag) {
                "INDI" -> indiCourant = idPersonne[xref] ?: 0L
                "SOUR" -> if (xref.isNotEmpty()) {
                    val b = SourceBrute()
                    sourcesBrutes[xref] = b
                    sourceCourante = b
                }
                "NOTE" -> if (xref.isNotEmpty()) notesEnreg[xref] = deArobase(valeur)
                "FAM" -> famillesMembres[xref] = mutableListOf()
            }
            continue
        }
        // Suite d'une citation en cours.
        val c = cit
        if (c != null) {
            if (niveau > citNiveau) {
                val v = deArobase(valeur)
                when (tag) {
                    "PAGE" -> c.page = v
                    "QUAY" -> c.quay = v.trim().toIntOrNull()?.takeIf { it in 0..3 } ?: -1
                    "DATA" -> c.dansData = true
                    "TEXT" -> c.extraits.add(v)
                    "NOTE" -> c.notes.add(if (valeur.trim().startsWith("@") && valeur.trim().endsWith("@") && valeur.trim().length > 2) valeur.trim() else v)
                    "_GPACTE" -> c.acte = v.trim()
                    "_GPVUE" -> c.vue = v.trim().toIntOrNull() ?: 0
                    "_GPVUETOT" -> c.vueTotal = v.trim().toIntOrNull() ?: 0
                    "_GPPAGE" -> c.pageLibre = v
                }
                continue
            }
            cloreCitation()
        }
        when (rec) {
            "INDI" -> {
                if (niveau == 1) {
                    bloc1 = tag
                    if (tag == "EVEN") indexEven++
                    if (tag == "SOUR") ouvrirCitation(1, "P", valeur)
                } else if (niveau == 2 && tag == "SOUR") {
                    ouvrirCitation(2, cibleDeBloc(bloc1, indexEven), valeur)
                }
            }
            "FAM" -> {
                if (niveau == 1) {
                    bloc1 = tag
                    if (tag == "HUSB" || tag == "WIFE") famillesMembres.getOrPut(recXref) { mutableListOf() }.add(valeur.trim())
                    if (tag == "SOUR") ouvrirCitation(1, "P", valeur)
                } else if (niveau == 2 && tag == "SOUR" && bloc1 == "MARR") {
                    ouvrirCitation(2, "M", valeur)
                }
            }
            "REPO" -> if (niveau == 1 && tag == "NAME" && recXref.isNotEmpty()) depots[recXref] = deArobase(valeur).trim()
            "SOUR" -> {
                val s = sourceCourante
                if (s != null) {
                    if (niveau == 1) {
                        bloc1 = tag
                        bloc2 = ""
                        val v = deArobase(valeur)
                        when (tag) {
                            "TITL" -> s.titre = v.trim()
                            "ABBR" -> s.abbr = v.trim()
                            "AUTH" -> s.auteur = v.trim()
                            "PUBL" -> s.publication = v.trim()
                            "TEXT" -> s.texte = v.trim()
                            "NOTE" -> if (v.isNotBlank()) s.notes.add(if (valeur.trim().startsWith("@") && valeur.trim().endsWith("@") && valeur.trim().length > 2) valeur.trim() else v.trim())
                            "REPO" -> s.depotRef = valeur.trim()
                            "_GPTYPE" -> s.gpType = v.trim()
                            "_GPQUAY" -> s.gpQuay = v.trim().toIntOrNull()?.takeIf { it in 0..3 } ?: -1
                        }
                    } else if (niveau == 2) {
                        bloc2 = tag
                        if (bloc1 == "REPO" && tag == "CALN") s.cote = deArobase(valeur).trim()
                        if (bloc1 == "DATA" && tag == "EVEN") s.evenCode = valeur.trim()
                    } else if (niveau == 3 && bloc1 == "DATA" && bloc2 == "EVEN") {
                        if (tag == "DATE") s.date = valeur.trim()
                        if (tag == "PLAC") s.plac = deArobase(valeur).trim()
                    }
                }
            }
        }
    }
    cloreCitation()

    fun texteNote(v: String): String {
        val t = v.trim()
        return if (t.startsWith("@") && t.endsWith("@") && t.length > 2) notesEnreg[t] ?: "" else t
    }

    // --- Construction des sources.
    val sources = mutableListOf<Source>()
    val idParXref = HashMap<String, Long>()
    var prochain = 1L
    sourcesBrutes.forEach { (xref, b) ->
        val s = Source(prochain)
        prochain++
        s.titre = if (b.titre.isNotEmpty()) b.titre else b.abbr
        s.type = if (b.gpType.isNotEmpty()) b.gpType else typeDepuisTag(b.evenCode)
        if (b.plac.isNotEmpty()) {
            val cd = couperLieu(b.plac)
            s.commune = cd.first
            s.dept = cd.second
        }
        s.periode = if (b.date.isNotEmpty()) periodeDepuisGedcom(b.date) else ""
        val depot = if (b.depotRef.isNotEmpty()) depots[b.depotRef] ?: "" else ""
        s.depot = if (depot == DEPOT_INCONNU) "" else depot
        s.cote = b.cote
        s.transcription = b.texte
        val notes = mutableListOf<String>()
        b.notes.forEach { n -> val t = texteNote(n); if (t.isNotBlank()) notes.add(t) }
        if (b.auteur.isNotEmpty()) notes.add("Auteur : " + b.auteur)
        if (b.publication.isNotEmpty()) notes.add("Publication : " + b.publication)
        val restantes = mutableListOf<String>()
        notes.joinToString("\n").split("\n").forEach { ligne ->
            if (ligne.startsWith("URL : ") && s.url.isEmpty()) s.url = ligne.removePrefix("URL : ").trim()
            else restantes.add(ligne)
        }
        s.notes = restantes.joinToString("\n").trim()
        s.fiabilite = b.gpQuay
        sources.add(s)
        idParXref[xref] = s.id
    }

    // --- Citations.
    val citations = mutableListOf<Citation>()
    var prochaineCit = 1L
    val inlineParTitre = HashMap<String, Long>()
    citationsBrutes.forEach { cb ->
        var sid = idParXref[cb.reference] ?: 0L
        if (sid == 0L && cb.reference.isNotEmpty() && !(cb.reference.startsWith("@") && cb.reference.endsWith("@"))) {
            // Source "en ligne" (texte libre, sans enregistrement SOUR).
            val titre = deArobase(cb.reference)
            sid = inlineParTitre.getOrPut(titre) {
                val s = Source(prochain, titre = titre)
                prochain++
                sources.add(s)
                s.id
            }
        }
        if (sid == 0L) return@forEach
        val cibles = if (cb.famille.isNotEmpty()) {
            (famillesMembres[cb.famille] ?: mutableListOf()).mapNotNull { x -> idPersonne[x]?.takeIf { it != 0L } }
        } else cb.cibles
        val extrait = cb.extraits.joinToString("\n").trim()
        val notes = cb.notes.map { texteNote(it) }.filter { it.isNotBlank() }.joinToString("\n").trim()
        cibles.forEach { pid ->
            val c = Citation(prochaineCit, sid, pid, cb.cible)
            if (cb.acte.isNotEmpty() || cb.vue > 0 || cb.vueTotal > 0 || cb.pageLibre.isNotEmpty()) {
                c.numeroActe = cb.acte
                c.vue = cb.vue
                c.vueTotal = cb.vueTotal
                c.page = cb.pageLibre
            } else {
                c.page = cb.page.trim()
            }
            c.extrait = extrait
            c.notes = notes
            c.fiabilite = cb.quay
            if (!citationExiste(citations, c)) {
                citations.add(c)
                prochaineCit++
            }
        }
    }
    return SourcesLues(sources, citations)
}
