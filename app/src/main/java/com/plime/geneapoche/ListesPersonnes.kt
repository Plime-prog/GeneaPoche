package com.plime.geneapoche

// v0.109 : fonctions pures de tri et de diagnostic de la liste des personnes,
// extraites de l'interface pour etre plus rapides et testees.
//
// Principe : les cles de tri (majuscules, numero decoupe) sont calculees UNE
// fois par personne, au lieu d'etre recalculees a chaque comparaison
// (O(n log n) allocations auparavant). L'ordre obtenu est strictement le meme
// que l'ancien (tris stables, memes cles) : voir ListesPersonnesTest.

private class CleNom(val p: Personne, val nom: String, val prenoms: String, val annee: Int)

// Ordre A-Z : nom, prenoms, annee de naissance (inconnue = en dernier).
fun trierParNom(personnes: List<Personne>): List<Personne> {
    val cles = personnes.map {
        CleNom(it, it.nom.trim().uppercase(), it.prenoms.trim().uppercase(), anneeNaissance(it))
    }
    return cles.sortedWith(
        compareBy<CleNom>({ it.nom }, { it.prenoms }, { it.annee })
    ).map { it.p }
}

private class CleNumero(val p: Personne, val numero: List<Int>, val nom: String, val prenoms: String)

// (comparerNumeros, de Journal.kt, compare deux numeros decoupes : segments
// un a un, le plus court d'abord en cas de prefixe identique.)

// Ordre Aboville / Branches : numero croissant, puis nom, puis prenoms.
fun trierParAboville(personnes: List<Personne>): List<Personne> {
    val cles = personnes.map {
        CleNumero(it, cleNumero(it), it.nom.trim().uppercase(), it.prenoms.trim().uppercase())
    }
    return cles.sortedWith(Comparator { a, b ->
        val r = comparerNumeros(a.numero, b.numero)
        if (r != 0) r else {
            val rn = a.nom.compareTo(b.nom)
            if (rn != 0) rn else a.prenoms.compareTo(b.prenoms)
        }
    }).map { it.p }
}

// Personnes sans aucun lien (ni parent, conjoint, enfant) et que personne
// d'autre ne cite. Meme resultat que filtrer avec brancheIsolee(p, personnes),
// mais en O(n) au lieu de O(n^2) : les identifiants cites sont collectes une
// seule fois.
fun personnesIsolees(personnes: List<Personne>): List<Personne> {
    val cites = HashSet<Long>()
    for (q in personnes) {
        if (q.pere != q.id) cites.add(q.pere)
        if (q.mere != q.id) cites.add(q.mere)
        if (q.conjoint != q.id) cites.add(q.conjoint)
        for (c in q.conjointsSupp) if (c != q.id) cites.add(c)
        for (e in q.enfants) if (e != q.id) cites.add(e)
    }
    return personnes.filter {
        it.pere == 0L && it.mere == 0L && it.conjoint == 0L &&
            it.conjointsSupp.isEmpty() && it.enfants.isEmpty() &&
            it.id !in cites
    }
}

// v0.124 : recherche de texte insensible a la casse ET aux accents
// ("lefevre" trouve "Lef\u00E8vre", "francois" trouve "Fran\u00E7ois").
fun contientSansAccents(texte: String, motif: String): Boolean =
    sansAccents(texte).contains(sansAccents(motif), ignoreCase = true)

// v0.137 : personnes proposees pour un role de parent / conjoint / enfant dans
// la fenetre "Choisir...". Le pere n'est choisi que parmi les hommes (sexe M),
// la mere que parmi les femmes (sexe F) ; les autres roles restent libres.
fun candidatsPourRole(personnes: List<Personne>, p: Personne, role: String): List<Personne> =
    personnes.filter { q ->
        q.id != p.id && when (role) {
            "pere" -> q.sexe == "M" && q.id != p.mere && q.id != p.conjoint && !p.conjointsSupp.contains(q.id)
            "mere" -> q.sexe == "F" && q.id != p.pere && q.id != p.conjoint && !p.conjointsSupp.contains(q.id)
            "conjoint" -> q.id != p.pere && q.id != p.mere && !p.conjointsSupp.contains(q.id)
            "conjointsupp" -> q.id != p.conjoint && !p.conjointsSupp.contains(q.id) &&
                q.id != p.pere && q.id != p.mere
            "enfant" -> q.id != p.pere && q.id != p.mere && q.id != p.conjoint &&
                !p.conjointsSupp.contains(q.id) && !p.enfants.contains(q.id)
            else -> false
        }
    }.sortedBy { it.titre() }
