package com.plime.geneapoche

// v0.138 : "Les 5 du jour". Cinq recherches ouvertes, choisies pour la VARIETE
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

/**
 * v0.141 : action a declencher sur la fiche pour une categorie de recherche
 * (code de l'acte N/B/M/D/S, "pere", "mere", "enfant", "modifier"), ou null.
 * Fonction pure partagee par la fiche, le journal et les "autres recherches".
 */
fun actionPourCategorie(categorie: String): String? = when {
    categorie.startsWith("naissance") -> "N"
    categorie.startsWith("bapteme") -> "B"
    categorie.startsWith("mariage") -> "M"
    categorie.startsWith("deces") -> "D"
    categorie.startsWith("sepulture") -> "S"
    categorie.startsWith("pere") -> "pere"
    categorie.startsWith("mere") -> "mere"
    categorie.startsWith("conjoint") -> "M"
    categorie.startsWith("enfants") -> "enfant"
    categorie.startsWith("sexe") -> "modifier"
    else -> null
}

/**
 * v0.141 : "Autres recherches". La selection du jour est FIGEE jusqu'a minuit :
 * si le jour memorise est celui d'aujourd'hui, on garde les memes identifiants
 * (une recherche terminee disparait et n'est PAS remplacee) ; sinon on tire une
 * selection neuve. Renvoie (jour, identifiants a memoriser).
 */
fun selectionDuJour(
    jourMemorise: Long,
    idsMemorises: List<Long>,
    ouvertes: List<Piste>,
    jour: Long
): Pair<Long, List<Long>> =
    if (jourMemorise == jour) Pair(jour, idsMemorises)
    else Pair(jour, cinqDuJour(ouvertes, jour).map { it.id })

/**
 * v0.142 : bouton "Autres recherches" : 5 NOUVELLES recherches, de preference
 * differentes de celles affichees (si assez d'autres existent), tirees avec la
 * meme regle de variete que celle de minuit.
 */
fun autresRecherches(ouvertes: List<Piste>, jour: Long, actuelles: List<Long>): List<Long> {
    val autres = ouvertes.filter { it.id !in actuelles }
    val base = if (autres.size >= 5) autres else ouvertes
    val graine = jour * 31L + actuelles.sum() + 7L
    return cinqDuJour(base, graine).map { it.id }
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
