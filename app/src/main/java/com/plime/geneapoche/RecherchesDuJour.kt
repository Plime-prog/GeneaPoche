package com.plime.geneapoche

// v0.137 : "Les 5 du jour". Cinq recherches ouvertes, choisies pour la VARIETE
// (pas dans l'ordre des Sosa) et qui changent chaque jour. Le choix est
// deterministe : un melange dependant seulement du jour et de l'identifiant de
// chaque recherche, donc stable toute la journee ; quand une recherche est faite
// elle disparait et la suivante du melange prend sa place, les autres ne bougent pas.

// Genre d'une recherche, pour varier les propositions.
fun genreRecherche(p: Piste): String {
    val c = p.categorie
    return when {
        c.startsWith("naissance") || c.startsWith("bapteme") -> "naissance"
        c.startsWith("mariage") -> "mariage"
        c.startsWith("deces") || c.startsWith("sepulture") -> "deces"
        c == "pere" || c == "mere" -> "parents"
        c == "conjoint" || c == "enfants" -> "famille"
        else -> "autre"
    }
}

// Petite phrase d'accroche selon le genre : de quoi garder le plaisir de chercher.
fun accrocheRecherche(p: Piste): String = when (genreRecherche(p)) {
    "naissance" -> "Un début d'histoire : retrouver une naissance"
    "mariage" -> "Une union à découvrir"
    "deces" -> "Un dernier chapitre à écrire"
    "parents" -> "Remonter d'une génération"
    "famille" -> "Agrandir la famille"
    else -> "À compléter"
}

// Numero du jour dans le fuseau local (change à minuit local).
fun jourLocal(maintenantMs: Long = System.currentTimeMillis()): Long {
    val decalage = java.util.TimeZone.getDefault().getOffset(maintenantMs)
    return Math.floorDiv(maintenantMs + decalage, 86_400_000L)
}

private fun melange(jour: Long, id: Long): Long {
    var h = jour * -7046029254386353131L + id
    h = h xor (h ushr 33)
    h *= -49064778989728563L
    h = h xor (h ushr 33)
    h *= -4265267296055464877L
    h = h xor (h ushr 33)
    return h
}

fun cinqDuJour(ouvertes: List<Piste>, jour: Long, nombre: Int = 5): List<Piste> {
    val rangees = ouvertes.filter { !it.faite }.sortedBy { melange(jour, it.id) }
    val choix = mutableListOf<Piste>()
    val personnes = mutableSetOf<Long>()
    val parGenre = mutableMapOf<String, Int>()
    // Passe 1 : une recherche par personne, au plus deux par genre.
    for (p in rangees) {
        if (choix.size >= nombre) break
        val g = genreRecherche(p)
        if (p.personneId in personnes || (parGenre[g] ?: 0) >= 2) continue
        choix.add(p); personnes.add(p.personneId); parGenre[g] = (parGenre[g] ?: 0) + 1
    }
    // Passe 2 : on relache la limite par genre (base peu variee).
    for (p in rangees) {
        if (choix.size >= nombre) break
        if (p in choix || p.personneId in personnes) continue
        choix.add(p); personnes.add(p.personneId)
    }
    // Passe 3 : on relache aussi la limite par personne.
    for (p in rangees) {
        if (choix.size >= nombre) break
        if (p !in choix) choix.add(p)
    }
    return choix
}
