package com.plime.geneapoche

// v0.148 : declarant d'un acte de deces = PERSONNE EXISTANTE (pas forcement un
// enfant). Fonctions pures : relation avec le defunt, candidats plausibles,
// phrases de biographie.

private fun idsEnfants(defunt: Personne, personnes: List<Personne>): Set<Long> {
    val s = LinkedHashSet<Long>()
    defunt.enfants.forEach { s.add(it) }
    personnes.forEach { q -> if (q.id != defunt.id && (q.pere == defunt.id || q.mere == defunt.id)) s.add(q.id) }
    return s
}

private fun parentsDe(q: Personne): List<Long> = listOf(q.pere, q.mere).filter { it != 0L }

// Relation de q avec le defunt, au genre de q, sans possessif ("fils", "epouse"...).
// Chaine vide si aucun lien connu.
fun relationDeclarant(defunt: Personne, q: Personne, personnes: List<Personne>): String {
    val f = q.sexe == "F"
    val m = q.sexe == "M"
    fun g(masc: String, fem: String, neutre: String) = if (f) fem else if (m) masc else neutre
    val enfants = idsEnfants(defunt, personnes)
    if (q.id in enfants) return g("fils", "fille", "enfant")
    if (defunt.tousConjoints().contains(q.id) || q.tousConjoints().contains(defunt.id))
        return g("époux", "épouse", "conjoint")
    if (defunt.pere == q.id || defunt.mere == q.id) return g("père", "mère", "parent")
    val pd = parentsDe(defunt)
    val pq = parentsDe(q)
    if (pd.isNotEmpty() && pq.any { it in pd }) return g("frère", "sœur", "frère ou sœur")
    if (pq.any { it in enfants }) return g("petit-fils", "petite-fille", "petit-enfant")
    val parId = personnes.associateBy { it.id }
    if (enfants.any { e -> parId[e]?.let { x -> x.tousConjoints().contains(q.id) || q.tousConjoints().contains(x.id) } == true })
        return g("gendre", "belle-fille", "gendre ou belle-fille")
    // neveu / niece : un parent de q est frere ou soeur du defunt
    if (pq.any { p -> parId[p]?.let { x -> pd.isNotEmpty() && parentsDe(x).any { it in pd } } == true })
        return g("neveu", "nièce", "neveu ou nièce")
    return ""
}

private val FEMININS = setOf("fille", "épouse", "mère", "sœur", "petite-fille", "belle-fille", "nièce")

// "son fils", "sa fille".
fun avecPossessif(relation: String): String {
    val r = relation.trim()
    if (r.isEmpty()) return ""
    return (if (r.lowercase() in FEMININS) "sa " else "son ") + r
}

private fun rang(relation: String): Int = when (relation) {
    "fils", "fille", "enfant" -> 0
    "époux", "épouse", "conjoint" -> 1
    "père", "mère", "parent" -> 2
    "frère", "sœur", "frère ou sœur" -> 3
    "petit-fils", "petite-fille", "petit-enfant" -> 4
    "gendre", "belle-fille", "gendre ou belle-fille" -> 5
    "neveu", "nièce", "neveu ou nièce" -> 6
    else -> 9
}

// Une personne a-t-elle PU declarer le deces ? Exclut : decedee avant l'annee de
// l'acte, ou trop jeune (moins de 15 ans). Annee inconnue = on ne filtre pas.
fun peutDeclarer(q: Personne, anneeActe: Int?): Boolean {
    if (anneeActe == null) return true
    val dc = anneeDe(q.dateDeces)
    if (dc != null && dc < anneeActe) return false
    val n = anneeDe(q.dateNaissance)
    if (n != null && anneeActe - n < 15) return false
    return true
}

// Candidats classes par proximite (enfants, conjoint, parents, fratrie...), puis les
// autres personnes plausibles par ordre alphabetique. Chaque candidat porte sa relation.
fun candidatsDeclarant(defunt: Personne, personnes: List<Personne>, anneeActe: Int?): List<Pair<Personne, String>> =
    personnes.filter { it.id != defunt.id && peutDeclarer(it, anneeActe) }
        .map { it to relationDeclarant(defunt, it, personnes) }
        .sortedWith(compareBy({ rang(it.second) }, { sansAccents(it.first.titre()).lowercase() }))

// Age a l'acte, "45" ou "", depuis la naissance (exacte ou calculee) et l'annee de l'acte.
fun ageDeclarantAuto(q: Personne, anneeActe: Int?): String {
    if (anneeActe == null) return ""
    val n = anneeDe(q.dateNaissance)
        ?: Regex("[0-9]{4}").find(q.naissanceCalculee)?.value?.toIntOrNull() ?: return ""
    val a = anneeActe - n
    return if (a in 0..120) a.toString() else ""
}

private fun ageTexte(age: String): String {
    val n = Regex("[0-9]{1,3}").find(age)?.value ?: return ""
    return n + (if (n == "1") " an" else " ans")
}

// "X, 45 ans, cultivateur" pour le declarant d'un deces.
fun identiteDeclarant(nom: String, age: String, profession: String): String =
    listOf(nom.trim(), ageTexte(age), profession.trim()).filter { it.isNotEmpty() }.joinToString(", ")

private fun lieuMairie(defunt: Personne): String {
    val l = lieuAffiche(defunt.lieuDeces.trim())
    return if (l.isNotEmpty()) " à la mairie de " + l else ""
}

private fun quand(date: String): String {
    val d = dateAffichee(date.trim())
    return if (d.isNotEmpty()) " le " + d else ""
}

// Phrase de la biographie du DEFUNT : "Son deces a ete declare par X, 45 ans, ...".
fun phraseDecesDeclarePar(defunt: Personne, declarant: Personne?): String? {
    val nom = declarant?.titre() ?: defunt.declarantNom.trim()
    if (nom.isEmpty()) return null
    val prof = defunt.declarantProfession.ifBlank { declarant?.profession ?: "" }
    val qui = identiteDeclarant(nom, defunt.declarantAge, prof)
    val rel = avecPossessif(defunt.declarantRelation)
    return "Son décès a été déclaré par " + qui + (if (rel.isNotEmpty()) ", " + rel + "," else "") +
        lieuMairie(defunt) + quand(defunt.declDDate) + "."
}

// Phrase de la biographie du DECLARANT : "X, 45 ans, ... a declare le deces de Y, son pere, ...".
fun phraseADeclare(declarant: Personne, defunt: Personne): String {
    val prof = defunt.declarantProfession.ifBlank { declarant.profession }
    val qui = identiteDeclarant(declarant.titre(), defunt.declarantAge, prof)
    val rel = avecPossessif(defunt.declarantRelation)
    return qui + (if (declarant.sexe == "F") " a déclaré " else " a déclaré ") + "le décès de " + defunt.titre() +
        (if (rel.isNotEmpty()) ", " + rel + "," else "") + lieuMairie(defunt) + quand(defunt.declDDate) + "."
}
