package com.plime.geneapoche

import org.json.JSONArray
import org.json.JSONObject

// ================================================================ v0.90
// Carnet de sources : SOURCE reutilisable et CITATION.
//
//  - Une SOURCE decrit un document, une fois pour toutes : "Archives
//    departementales du Cher, etat civil de Bourges, 1872" (depot, cote,
//    commune, periode, transcription...). Elle peut servir a autant de
//    personnes et d'evenements qu'on veut.
//  - Une CITATION relie UNE source a UNE personne et a UN evenement, avec
//    le detail de l'endroit precis : "Naissance de Marie Dupont, acte n 43,
//    vue 17/312".
//
// Compatibilite GEDCOM 5.5.1 (Heredis, Geneanet, Gramps...) : la source part
// en enregistrement SOUR (TITL, REPO/CALN, DATA/EVEN, TEXT, NOTE), la
// citation en "n SOUR @Sx@" sous l'evenement (PAGE, QUAY, DATA/TEXT, NOTE).
// Logique PURE (aucune dependance Android) : testee en JVM.

// Types de document ; les 5 premiers correspondent a un evenement GEDCOM.
val TypesSource: List<Pair<String, String>> = listOf(
    "naissance" to "Acte de naissance",
    "bapteme" to "Acte de bapt\u00eame",
    "mariage" to "Acte de mariage",
    "deces" to "Acte de d\u00e9c\u00e8s",
    "sepulture" to "Acte de s\u00e9pulture",
    "recensement" to "Recensement",
    "notarie" to "Acte notari\u00e9",
    "militaire" to "Registre militaire",
    "autre" to "Autre document"
)

fun libelleTypeSource(code: String): String =
    TypesSource.firstOrNull { it.first == code }?.second ?: ""

// Cibles d'une citation : "P" = la personne (identite) ; N B M D S = les
// cinq evenements de la fiche ; "E<id>" = un evenement libre.
val CiblesCitation: List<Pair<String, String>> = listOf(
    "P" to "Identit\u00e9",
    "N" to "Naissance",
    "B" to "Bapt\u00eame",
    "M" to "Mariage",
    "D" to "D\u00e9c\u00e8s",
    "S" to "S\u00e9pulture"
)

class Source(
    val id: Long,
    var titre: String = "",
    var type: String = "",
    var commune: String = "",
    var dept: String = "",
    var periode: String = "",
    var depot: String = "",
    var cote: String = "",
    var url: String = "",
    var transcription: String = "",
    var notes: String = "",
    // Niveau de fiabilite par defaut des citations de cette source :
    // -1 = non renseigne ; 0 a 3 = echelle QUAY du GEDCOM.
    var fiabilite: Int = -1
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("titre", titre)
        o.put("type", type)
        o.put("commune", commune)
        o.put("dept", dept)
        o.put("periode", periode)
        o.put("depot", depot)
        o.put("cote", cote)
        o.put("url", url)
        o.put("transcription", transcription)
        o.put("notes", notes)
        o.put("fiabilite", fiabilite)
        return o
    }

    fun copie(): Source = Source(
        id, titre, type, commune, dept, periode, depot, cote, url, transcription, notes, fiabilite
    )

    companion object {
        fun depuisJson(o: JSONObject): Source = Source(
            id = o.optLong("id", 0L),
            titre = o.optString("titre", ""),
            type = o.optString("type", ""),
            commune = o.optString("commune", ""),
            dept = o.optString("dept", ""),
            periode = o.optString("periode", ""),
            depot = o.optString("depot", ""),
            cote = o.optString("cote", ""),
            url = o.optString("url", ""),
            transcription = o.optString("transcription", ""),
            notes = o.optString("notes", ""),
            fiabilite = o.optInt("fiabilite", -1)
        )
    }
}

class Citation(
    val id: Long,
    var sourceId: Long,
    var personneId: Long,
    var cible: String = "P",
    // Detail libre complementaire ("f\u00b0 12 v\u00b0", "page 3").
    var page: String = "",
    var numeroActe: String = "",
    // Numero de la vue (image) et nombre total de vues de la cote.
    var vue: Int = 0,
    var vueTotal: Int = 0,
    var extrait: String = "",
    var notes: String = "",
    // -1 = reprend la fiabilite de la source ; 0 a 3 = echelle QUAY.
    var fiabilite: Int = -1
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("sourceId", sourceId)
        o.put("personneId", personneId)
        o.put("cible", cible)
        o.put("page", page)
        o.put("numeroActe", numeroActe)
        o.put("vue", vue)
        o.put("vueTotal", vueTotal)
        o.put("extrait", extrait)
        o.put("notes", notes)
        o.put("fiabilite", fiabilite)
        return o
    }

    fun copie(): Citation = Citation(
        id, sourceId, personneId, cible, page, numeroActe, vue, vueTotal, extrait, notes, fiabilite
    )

    companion object {
        fun depuisJson(o: JSONObject): Citation = Citation(
            id = o.optLong("id", 0L),
            sourceId = o.optLong("sourceId", 0L),
            personneId = o.optLong("personneId", 0L),
            cible = o.optString("cible", "P"),
            page = o.optString("page", ""),
            numeroActe = o.optString("numeroActe", ""),
            vue = o.optInt("vue", 0),
            vueTotal = o.optInt("vueTotal", 0),
            extrait = o.optString("extrait", ""),
            notes = o.optString("notes", ""),
            fiabilite = o.optInt("fiabilite", -1)
        )
    }
}

fun sourcesVersJson(sources: List<Source>): JSONArray {
    val a = JSONArray()
    sources.forEach { a.put(it.versJson()) }
    return a
}

fun citationsVersJson(citations: List<Citation>): JSONArray {
    val a = JSONArray()
    citations.forEach { a.put(it.versJson()) }
    return a
}

fun sourcesDepuisJson(a: JSONArray?): MutableList<Source> {
    val l = mutableListOf<Source>()
    if (a != null) for (i in 0 until a.length()) l.add(Source.depuisJson(a.getJSONObject(i)))
    return l
}

fun citationsDepuisJson(a: JSONArray?): MutableList<Citation> {
    val l = mutableListOf<Citation>()
    if (a != null) for (i in 0 until a.length()) l.add(Citation.depuisJson(a.getJSONObject(i)))
    return l
}

// Le catalogue est stocke dans UN fichier chiffre : un tableau JSON dont
// chaque element porte "k" = "s" (source) ou "c" (citation).
fun catalogueVersTexte(sources: List<Source>, citations: List<Citation>): String {
    val a = JSONArray()
    sources.forEach { val o = it.versJson(); o.put("k", "s"); a.put(o) }
    citations.forEach { val o = it.versJson(); o.put("k", "c"); a.put(o) }
    return a.toString()
}

class Catalogue(val sources: MutableList<Source>, val citations: MutableList<Citation>)

// Elements (Source ou Citation) d'un texte de catalogue ; leve une exception
// si le texte est invalide (lecture tout-ou-rien du stockage).
fun elementsCatalogue(texte: String): MutableList<Any> {
    val a = JSONArray(texte)
    val l = mutableListOf<Any>()
    for (i in 0 until a.length()) {
        val o = a.getJSONObject(i)
        when (o.optString("k", "")) {
            "s" -> l.add(Source.depuisJson(o))
            "c" -> l.add(Citation.depuisJson(o))
        }
    }
    return l
}

fun catalogueDepuisTexte(texte: String): Catalogue {
    val sources = mutableListOf<Source>()
    val citations = mutableListOf<Citation>()
    elementsCatalogue(texte).forEach { e ->
        if (e is Source) sources.add(e) else if (e is Citation) citations.add(e)
    }
    return Catalogue(sources, citations)
}

// ---------------------------------------------------------------- Libelles

// Echelle QUAY (GEDCOM) : 0 a 3. Les libelles de l'application restent
// ceux deja utilises par libelleFiabilite().
fun libelleQualite(n: Int): String = if (n in 0..3) n.toString() + " \u2013 " + libelleFiabilite(n) else "non renseign\u00e9e"

// "acte n\u00b0 43, vue 17/312, f\u00b0 12" : le DETAIL d'une citation.
fun detailCitation(c: Citation): String {
    val l = mutableListOf<String>()
    if (c.numeroActe.isNotBlank()) l.add("acte n\u00b0 " + c.numeroActe.trim())
    if (c.vue > 0) l.add("vue " + c.vue.toString() + (if (c.vueTotal > 0) "/" + c.vueTotal.toString() else ""))
    if (c.page.isNotBlank()) l.add(c.page.trim())
    return l.joinToString(", ")
}

// Fiabilite effective d'une citation : la sienne, sinon celle de sa source.
fun qualiteEffective(c: Citation, s: Source?): Int =
    if (c.fiabilite in 0..3) c.fiabilite else if (s != null && s.fiabilite in 0..3) s.fiabilite else -1

fun libelleCible(cible: String, p: Personne?): String {
    CiblesCitation.firstOrNull { it.first == cible }?.let { return it.second }
    if (cible.startsWith("E")) {
        val id = cible.substring(1).toLongOrNull()
        val ev = p?.evenementsLibres?.firstOrNull { it.id == id }
        return "\u00c9v\u00e9nement" + (if (ev != null && ev.intitule.isNotBlank()) " : " + ev.intitule.trim() else "")
    }
    return cible
}

// "Naissance de Marie Dupont, acte n\u00b0 43, vue 17/312"
fun libelleCitation(c: Citation, p: Personne?): String {
    val base = libelleCible(c.cible, p) + (if (p != null) " de " + p.titre() else "")
    val d = detailCitation(c)
    return if (d.isEmpty()) base else base + ", " + d
}

// Reference courte d'une source : titre, sinon depot + cote.
fun libelleSource(s: Source): String {
    if (s.titre.isNotBlank()) return s.titre.trim()
    val l = listOf(s.depot.trim(), s.cote.trim()).filter { it.isNotEmpty() }
    return if (l.isEmpty()) "(source sans titre)" else l.joinToString(" \u2014 ")
}

// Titre propose pour une nouvelle source :
// "Archives d\u00e9partementales du Cher, Acte de naissance, Bourges, 1872".
fun titreSuggere(s: Source): String {
    val l = mutableListOf<String>()
    if (s.depot.isNotBlank()) l.add(s.depot.trim())
    val t = libelleTypeSource(s.type)
    if (t.isNotEmpty()) l.add(t)
    if (s.commune.isNotBlank()) l.add(s.commune.trim())
    if (s.periode.isNotBlank()) l.add(s.periode.trim())
    return l.joinToString(", ")
}

// ---------------------------------------------------------------- Requetes

fun citationsDe(citations: List<Citation>, personneId: Long): List<Citation> =
    citations.filter { it.personneId == personneId }

fun citationsDeSource(citations: List<Citation>, sourceId: Long): List<Citation> =
    citations.filter { it.sourceId == sourceId }

fun sourceParId(sources: List<Source>, id: Long): Source? = sources.firstOrNull { it.id == id }

// Une citation identique (meme source, meme personne, meme cible, meme
// detail) n'est jamais ajoutee deux fois.
fun citationExiste(citations: List<Citation>, c: Citation): Boolean =
    citations.any {
        it.sourceId == c.sourceId && it.personneId == c.personneId && it.cible == c.cible &&
            detailCitation(it) == detailCitation(c)
    }

// Fusion de deux fiches : les citations de la fiche supprimee passent a la
// fiche conservee (sans doublon).
fun transfererCitations(citations: MutableList<Citation>, deId: Long, versId: Long) {
    val aTransferer = citations.filter { it.personneId == deId }
    for (c in aTransferer) {
        val doublon = citations.any {
            it.personneId == versId && it.sourceId == c.sourceId && it.cible == c.cible &&
                detailCitation(it) == detailCitation(c)
        }
        if (doublon) citations.remove(c) else c.personneId = versId
    }
}

// Retire toutes les citations d'une personne (suppression de la fiche) ;
// renvoie ce qui a ete retire (pour pouvoir annuler).
fun retirerCitationsDe(citations: MutableList<Citation>, personneId: Long): List<Citation> {
    val retirees = citations.filter { it.personneId == personneId }
    citations.removeAll(retirees)
    return retirees
}

// Plus grand identifiant utilise (sources et citations) : sert a aligner le
// compteur d'identifiants apres un import ou une restauration.
fun maxIdCatalogue(sources: List<Source>, citations: List<Citation>): Long =
    maxOf(sources.maxOfOrNull { it.id } ?: 0L, citations.maxOfOrNull { it.id } ?: 0L)

// Une source n'est jamais supprimee en silence : combien de citations ?
fun nombreCitations(citations: List<Citation>, sourceId: Long): Int =
    citations.count { it.sourceId == sourceId }

// Citations dont la source ou la personne n'existe plus (donnees corrompues,
// restauration partielle) : ignorees a l'affichage, purgees a l'ecriture.
fun citationsValides(
    citations: List<Citation>,
    sources: List<Source>,
    personnes: List<Personne>
): List<Citation> {
    val ids = sources.map { it.id }.toSet()
    val pers = personnes.map { it.id }.toSet()
    return citations.filter { ids.contains(it.sourceId) && pers.contains(it.personneId) }
}
