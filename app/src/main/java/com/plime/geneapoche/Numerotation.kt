package com.plime.geneapoche

// ---------------------------------------------------------------- Aides numerotation

// v0.99 : personneParId deplacee dans LogiqueActe.kt (index id -> personne,
// plus de recherche lineaire a chaque appel).

fun sosaParent(p: Personne, mere: Boolean): String {
    val s = p.sosa.trim().toLongOrNull() ?: return ""
    return (if (mere) 2L * s + 1L else 2L * s).toString()
}

fun prefixeLateral(p: Personne): String {
    val l = p.lateral.trim()
    if (l.isNotEmpty()) return l
    return p.sosa.trim()
}

fun sosaValide(s: String): Boolean {
    val t = s.trim()
    if (t.isEmpty()) return true
    val n = t.toLongOrNull() ?: return false
    return n >= 1L
}

fun lateralValide(s: String): Boolean {
    val t = s.trim()
    if (t.isEmpty()) return true
    return Regex("^[0-9]+(\\.[0-9]+)*$").matches(t)
}

// Annee de naissance pour le tri (9999 si inconnue).
// G2-03 : anneeDe gere AUSSI les dates republicaines ("22 Germinal An II").
fun anneeNaissance(p: Personne): Int =
    anneeDe(p.dateNaissance.ifBlank { p.naissanceCalculee }) ?: 9999

// Cle de tri par numero croissant (Sosa ou d'Aboville) : le numero est
// decoupe en segments compares un a un ("1.3" < "1.10" < "2"), les
// personnes sans numero passent en fin de liste.
fun cleNumero(p: Personne): List<Int> {
    val t = p.numeroBrut().trim()
    if (t.isEmpty()) return listOf(Int.MAX_VALUE)
    return t.split(".").map { seg -> seg.toIntOrNull() ?: Int.MAX_VALUE }
}

// Comparateur de numeros : segments une a une ("1.2" avant "1.10"),
// le plus court d'abord en cas de prefixe identique ("7" avant "7.2").
val ComparateurAboville = Comparator<Personne> { a, b ->
    val ka = cleNumero(a)
    val kb = cleNumero(b)
    val n = minOf(ka.size, kb.size)
    var r = 0
    for (i in 0 until n) {
        r = ka[i].compareTo(kb[i])
        if (r != 0) return@Comparator r
    }
    ka.size.compareTo(kb.size)
}

// Renum\u00E9rotation complete depuis un de cujus :
// Sosa (pere = 2n, mere = 2n+1) remontee en largeur, puis d'Aboville en descendant
// (enfant k du porteur "x" = "x.k"). Les personnes non atteignables perdent leur num\u00E9ro.
// Numerotation complete a partir du de cujus, dont le n Sosa n'est pas forcement 1 :
// pere = 2n, mere = 2n+1 (Sosa-Stradonitz), puis d'Aboville pour TOUS les
// enfants non Sosa de toute personne numerotee (prefixe du parent + .rang).
fun renumeroter(personnes: MutableList<Personne>, cujusId: Long, sosaDeBase: Long = 1L) {
    IndexPersonnes.invalider()
    personnes.forEach { it.sosa = ""; it.lateral = "" }
    val cujus = personneParId(personnes, cujusId) ?: return
    cujus.sosa = sosaDeBase.toString()
    val attente = ArrayDeque<Long>()
    attente.addLast(cujus.id)
    while (attente.isNotEmpty()) {
        val id = attente.removeFirst()
        val p = personneParId(personnes, id) ?: continue
        val s = p.sosa.trim().toLongOrNull() ?: continue
        val pp = personneParId(personnes, p.pere)
        if (pp != null && pp.sosa.isEmpty()) {
            pp.sosa = (2L * s).toString()
            attente.addLast(pp.id)
        }
        val pm = personneParId(personnes, p.mere)
        if (pm != null && pm.sosa.isEmpty()) {
            pm.sosa = (2L * s + 1L).toString()
            attente.addLast(pm.id)
        }
    }
    // Enfants lateraux de TOUTE personne numerotee (Sosa ou d'Aboville) :
    // freres, soeurs, oncles, tantes, cousins... d'Aboville :
    // "prefixe du parent + .rang de naissance" ; un enfant deja Sosa garde
    // son Sosa (il occupe son rang dans la fratrie). Ainsi TOUT
    // rattachement (pere, mere, enfant) met les numeros a jour immediatement
    // via enregistrer() -> renumeroter(), sans affectation manuelle.
    // Correctifs : 1) les parents numerotes sont traites par n Sosa
    // croissant (le pere 2n l'emporte sur la mere 2n+1 pour prefixer un
    // enfant doublement rattache) ; 2) le rang dans la fratrie suit
    // l'annee de naissance, pas l'ordre de saisie.
    val fileLat = ArrayDeque<Pair<Long, String>>()
    personnes.filter { it.sosa.isNotEmpty() }
        .sortedBy { it.sosa.trim().toLongOrNull() ?: Long.MAX_VALUE }
        .forEach { fileLat.addLast(it.id to it.sosa.trim()) }
    personnes.filter { it.lateral.isNotEmpty() }.forEach { fileLat.addLast(it.id to it.lateral.trim()) }
    while (fileLat.isNotEmpty()) {
        val (id, pref) = fileLat.removeFirst()
        val p = personneParId(personnes, id) ?: continue
        val tries = p.enfants.withIndex()
            .sortedWith(compareBy({ anneeNaissance(personneParId(personnes, it.value) ?: Personne(id = 0L)) }, { it.index }))
            .map { it.value }
        tries.forEachIndexed { rang, idE ->
            val e = personneParId(personnes, idE) ?: return@forEachIndexed
            if (e.sosa.isEmpty() && e.lateral.isEmpty()) {
                e.lateral = pref + "." + (rang + 1).toString()
                fileLat.addLast(e.id to e.lateral)
            }
        }
    }
}

// G2-17 : controle et reparation des liens familiaux (identifiants orphelins,
// parent egal a soi-meme, doublons d'enfants, temoins ou signataires disparus).
// Les cycles pere/mere sont signales sans etre casses automatiquement :
// renumeroter les supporte deja (il marque les personnes traitees, donc ne
// boucle jamais). Retourne un RAPPORT, une ligne par anomalie corrigee.
fun verifierLiens(personnes: MutableList<Personne>): List<String> {
    val rapports = mutableListOf<String>()
    val ids = personnes.map { it.id }.toHashSet()
    fun absent(id: Long) = id != 0L && !ids.contains(id)
    personnes.forEach { p ->
        if (p.pere == p.id || absent(p.pere)) {
            if (p.pere != 0L) rapports.add("Lien p\u00E8re invalide retir\u00E9 : " + p.titre() + ".")
            p.pere = 0L
        }
        if (p.mere == p.id || absent(p.mere)) {
            if (p.mere != 0L) rapports.add("Lien m\u00E8re invalide retir\u00E9 : " + p.titre() + ".")
            p.mere = 0L
        }
        if (p.conjoint == p.id || absent(p.conjoint)) {
            if (p.conjoint != 0L) rapports.add("Lien conjoint invalide retir\u00E9 : " + p.titre() + ".")
            p.conjoint = 0L
        }
        val suppInconnus = p.conjointsSupp.filter { absent(it) || it == p.id }
        if (suppInconnus.isNotEmpty()) {
            p.conjointsSupp.removeAll(suppInconnus)
            rapports.add("Conjoint suppl\u00E9mentaire inconnu retir\u00E9 : " + p.titre() + ".")
        }
        val enfantsInconnus = p.enfants.filter { absent(it) || it == p.id }
        if (enfantsInconnus.isNotEmpty()) {
            p.enfants.removeAll(enfantsInconnus)
            rapports.add("Enfant inconnu retir\u00E9 : " + p.titre() + ".")
        }
        if (p.enfants.size != p.enfants.distinct().size) {
            val distinct = p.enfants.distinct()
            p.enfants.clear()
            p.enfants.addAll(distinct)
            rapports.add("Doublon d'enfants corrig\u00E9 : " + p.titre() + ".")
        }
        // Temoins et signataires disparus (0L = convention "pere", conserve).
        listOf(
            "T\u00E9moins inconnus retir\u00E9s" to listOf(
                p.temoinsN, p.temoinsB, p.temoinsM, p.temoinsD, p.temoinsS
            ),
            "Signataires inconnus retir\u00E9s" to listOf(
                p.signatairesN, p.signatairesB, p.signatairesM, p.signatairesD, p.signatairesS
            )
        ).forEach { paire ->
            var nettoyage = false
            paire.second.forEach { liste ->
                val perdus = liste.filter { absent(it) }
                if (perdus.isNotEmpty()) {
                    liste.removeAll(perdus)
                    nettoyage = true
                }
            }
            if (nettoyage) rapports.add(paire.first + " : " + p.titre() + ".")
        }
    }
    // Cycles pere/mere : signales une seule fois par paire, a corriger a la main.
    val vus = mutableSetOf<Long>()
    personnes.forEach { p ->
        val pp = personneParId(personnes, p.pere)
        if (pp != null && pp.pere == p.id && !vus.contains(p.id)) {
            vus.add(p.id); vus.add(pp.id)
            rapports.add("Cycle p\u00E8re \u00E0 corriger : " + p.titre() + " / " + pp.titre() + ".")
        }
        val pm = personneParId(personnes, p.mere)
        if (pm != null && pm.mere == p.id && !vus.contains(p.id)) {
            vus.add(p.id); vus.add(pm.id)
            rapports.add("Cycle m\u00E8re \u00E0 corriger : " + p.titre() + " / " + pm.titre() + ".")
        }
    }
    return rapports.distinct()
}

// ---------------------------------------------------------------- Coherence parent/enfant
// GeneaPoche 11 (bug "enfant ne avant son parent") : UNE SEULE regle,
// appliquee par TOUTES les voies d'ajout (saisie d'enfant, choix de fiche,
// nouveau parent, acte de naissance rattache, import fusionne).
// Renvoie un message si l'enfant serait ne avant son parent, sinon null.
// Compare les annees (dates partielles ou republicaines comprises, via
// anneeDe) ; une date inconnue ne bloque jamais rien.
fun incoherenceParente(parent: Personne, dateEnfant: String): String? {
    val an = anneeDe(dateEnfant) ?: return null
    val ap = anneeDe(parent.dateNaissance) ?: return null
    return if (an < ap)
        "Impossible : l'enfant (" + an + ") serait n\u00E9 avant " +
        parent.titre().trim() + " (" + ap + ")."
    else null
}

