package com.plime.geneapoche

// ================================================================ v0.87
// Boite a outils - outil 3 : calculateur de parente entre deux personnes.
// Logique PURE (aucune dependance Android ni Personne) : testee en JVM.
//
// Vocabulaire : pour deux personnes X et Y, "a" = nombre de generations de X
// jusqu'a l'ancetre commun le plus proche, "b" = de Y jusqu'a ce meme
// ancetre. Frere/soeur = (1,1) ; oncle = (2,1) ; cousin germain = (2,2).
// Le degre civil (code civil) est a + b.

// Noeud minimal d'un arbre (copie des liens d'une Personne).
data class NoeudParente(
    val id: Long,
    val pere: Long,
    val mere: Long,
    val sexe: String, // "M", "F" ou ""
    val conjoints: List<Long>
)

data class ResultatParente(
    val trouve: Boolean,
    val lien: String,          // terme accorde au sexe de Y : "cousine germaine"
    val phrase: String,        // "Marie est la cousine germaine de Pierre"
    val phraseInverse: String, // "Pierre est le cousin germain de Marie"
    val degreCivil: Int,       // 0 si non applicable (alliance, inconnu)
    val ancetresCommuns: List<Long>,
    val alliance: Boolean,
    val remarque: String
)

private data class Terme(val m: String, val f: String, val n: String)

private data class Relation(
    val terme: Terme,
    val alliance: Boolean,
    val degre: Int,
    val communs: List<Long>,
    val remarque: String
)

private class LienSang(val a: Int, val b: Int, val communs: List<Long>)

// Ancetres (et la personne elle-meme) avec leur distance minimale en
// generations ; une boucle dans des donnees corrompues ne bloque rien.
private fun ancetres(id: Long, noeuds: Map<Long, NoeudParente>): Map<Long, Int> {
    val res = HashMap<Long, Int>()
    res[id] = 0
    var front: List<Long> = listOf(id)
    var g = 0
    while (front.isNotEmpty() && g < 80) {
        g++
        val suivant = ArrayList<Long>()
        for (x in front) {
            val n = noeuds[x] ?: continue
            for (p in listOf(n.pere, n.mere)) {
                if (p != 0L && noeuds.containsKey(p) && !res.containsKey(p)) {
                    res[p] = g
                    suivant.add(p)
                }
            }
        }
        front = suivant
    }
    return res
}

private fun lienDeSang(x: Long, y: Long, noeuds: Map<Long, NoeudParente>): LienSang? {
    val ax = ancetres(x, noeuds)
    val ay = ancetres(y, noeuds)
    val communs = ax.keys.filter { ay.containsKey(it) }
    if (communs.isEmpty()) return null
    val somme = communs.minOf { ax.getValue(it) + ay.getValue(it) }
    val meilleurs = communs.filter { ax.getValue(it) + ay.getValue(it) == somme }
    // Ex aequo : le lien le plus direct (ecart de generations le plus faible).
    val ref = meilleurs.minByOrNull { Math.abs(ax.getValue(it) - ay.getValue(it)) } ?: return null
    val a = ax.getValue(ref)
    val b = ay.getValue(ref)
    val memeNiveau = meilleurs.filter { ax.getValue(it) == a && ay.getValue(it) == b }
    return LienSang(a, b, memeNiveau.sorted())
}

private fun arriere(k: Int): String = "arri\u00e8re-".repeat(if (k > 0) k else 0)

private fun terme(m: String, f: String): Terme = Terme(m, f, "$m/$f")

private fun termeDescendant(b: Int): Terme = when {
    b == 1 -> Terme("fils", "fille", "enfant")
    b == 2 -> Terme("petit-fils", "petite-fille", "petit-enfant")
    b <= 5 -> Terme(
        arriere(b - 2) + "petit-fils", arriere(b - 2) + "petite-fille", arriere(b - 2) + "petit-enfant"
    )
    else -> Terme(
        "descendant direct \u00e0 la " + b + "e g\u00e9n\u00e9ration",
        "descendante directe \u00e0 la " + b + "e g\u00e9n\u00e9ration",
        "descendant(e) direct(e) \u00e0 la " + b + "e g\u00e9n\u00e9ration"
    )
}

private fun termeAscendant(a: Int): Terme = when {
    a == 1 -> Terme("p\u00e8re", "m\u00e8re", "parent")
    a == 2 -> Terme("grand-p\u00e8re", "grand-m\u00e8re", "grand-parent")
    a <= 5 -> Terme(
        arriere(a - 2) + "grand-p\u00e8re", arriere(a - 2) + "grand-m\u00e8re", arriere(a - 2) + "grand-parent"
    )
    else -> Terme(
        "ascendant direct \u00e0 la " + a + "e g\u00e9n\u00e9ration",
        "ascendante directe \u00e0 la " + a + "e g\u00e9n\u00e9ration",
        "ascendant(e) direct(e) \u00e0 la " + a + "e g\u00e9n\u00e9ration"
    )
}

// Y est neveu/niece de X : (a = 1, b >= 2).
private fun termeNeveu(b: Int): Terme = when {
    b == 2 -> terme("neveu", "ni\u00e8ce")
    b <= 5 -> terme(arriere(b - 3) + "petit-neveu", arriere(b - 3) + "petite-ni\u00e8ce")
    else -> Terme(
        "neveu \u00e9loign\u00e9 (" + (b - 1) + "e g\u00e9n\u00e9ration)",
        "ni\u00e8ce \u00e9loign\u00e9e (" + (b - 1) + "e g\u00e9n\u00e9ration)",
        "neveu/ni\u00e8ce \u00e9loign\u00e9(e) (" + (b - 1) + "e g\u00e9n\u00e9ration)"
    )
}

// Y est oncle/tante de X : (a >= 2, b = 1).
private fun termeOncle(a: Int): Terme = when {
    a == 2 -> terme("oncle", "tante")
    a == 3 -> terme("grand-oncle", "grand-tante")
    a <= 5 -> terme(arriere(a - 3) + "grand-oncle", arriere(a - 3) + "grand-tante")
    else -> Terme(
        "oncle \u00e9loign\u00e9 (" + a + "e g\u00e9n\u00e9ration)",
        "tante \u00e9loign\u00e9e (" + a + "e g\u00e9n\u00e9ration)",
        "oncle/tante \u00e9loign\u00e9(e) (" + a + "e g\u00e9n\u00e9ration)"
    )
}

private fun termeCousin(a: Int, b: Int): Terme {
    val k = Math.min(a, b) - 1
    val ecart = Math.abs(a - b)
    if (ecart == 0) {
        return when (k) {
            1 -> Terme("cousin germain", "cousine germaine", "cousin(e) germain(e)")
            2 -> Terme("cousin issu de germain", "cousine issue de germain", "cousin(e) issu(e) de germain")
            else -> Terme("cousin de rang $k", "cousine de rang $k", "cousin(e) de rang $k")
        }
    }
    if (ecart == 1 && k == 1) {
        return if (a > b) {
            Terme("oncle \u00e0 la mode de Bretagne", "tante \u00e0 la mode de Bretagne", "oncle/tante \u00e0 la mode de Bretagne")
        } else {
            Terme("neveu \u00e0 la mode de Bretagne", "ni\u00e8ce \u00e0 la mode de Bretagne", "neveu/ni\u00e8ce \u00e0 la mode de Bretagne")
        }
    }
    val sens = if (a > b) "plus \u00e2g\u00e9(e) de $ecart g\u00e9n\u00e9ration(s)" else "plus jeune de $ecart g\u00e9n\u00e9ration(s)"
    return Terme(
        "cousin de rang $k ($sens)", "cousine de rang $k ($sens)", "cousin(e) de rang $k ($sens)"
    )
}

private fun relationSang(x: Long, y: Long, noeuds: Map<Long, NoeudParente>): Relation? {
    val l = lienDeSang(x, y, noeuds) ?: return null
    val a = l.a
    val b = l.b
    val nx = noeuds[x]
    val ny = noeuds[y]
    var remarque = ""
    val t: Terme = when {
        a == 0 && b >= 1 -> termeDescendant(b)
        b == 0 && a >= 1 -> termeAscendant(a)
        a == 1 && b == 1 -> {
            if (l.communs.size >= 2) {
                terme("fr\u00e8re", "s\u0153ur")
            } else if (nx != null && ny != null && nx.pere != 0L && nx.mere != 0L && ny.pere != 0L && ny.mere != 0L) {
                terme("demi-fr\u00e8re", "demi-s\u0153ur")
            } else {
                remarque = "Un seul parent commun connu : demi-fr\u00e8re ou demi-s\u0153ur possible."
                terme("fr\u00e8re", "s\u0153ur")
            }
        }
        a == 1 && b >= 2 -> termeNeveu(b)
        b == 1 && a >= 2 -> termeOncle(a)
        else -> termeCousin(a, b)
    }
    if (remarque.isEmpty() && l.communs.size == 1 && a >= 1 && b >= 1 && !(a == 1 && b == 1)) {
        remarque = "Un seul anc\u00eatre commun connu (l'autre n'est peut-\u00eatre pas saisi)."
    }
    return Relation(t, false, a + b, l.communs, remarque)
}

private fun relationAlliance(x: Long, y: Long, noeuds: Map<Long, NoeudParente>): Relation? {
    val nx = noeuds[x] ?: return null
    val ny = noeuds[y] ?: return null
    if (ny.id in nx.conjoints) return Relation(terme("\u00e9poux", "\u00e9pouse"), true, 0, emptyList(), "")
    // Y est parent, frere/soeur ou enfant (d'un autre lit) du conjoint de X.
    for (s in nx.conjoints) {
        val l = lienDeSang(s, y, noeuds) ?: continue
        if (l.a == 1 && l.b == 0) return Relation(terme("beau-p\u00e8re", "belle-m\u00e8re"), true, 0, emptyList(), "")
        if (l.a == 1 && l.b == 1) return Relation(terme("beau-fr\u00e8re", "belle-s\u0153ur"), true, 0, emptyList(), "")
        if (l.a == 0 && l.b == 1) return Relation(terme("beau-fils", "belle-fille"), true, 0, emptyList(), "")
    }
    // Y est le conjoint d'un enfant, d'un parent ou d'un frere/soeur de X.
    for (s in ny.conjoints) {
        val l = lienDeSang(x, s, noeuds) ?: continue
        if (l.a == 0 && l.b == 1) return Relation(terme("gendre", "bru"), true, 0, emptyList(), "")
        if (l.a == 1 && l.b == 0) return Relation(terme("beau-p\u00e8re", "belle-m\u00e8re"), true, 0, emptyList(), "")
        if (l.a == 1 && l.b == 1) return Relation(terme("beau-fr\u00e8re", "belle-s\u0153ur"), true, 0, emptyList(), "")
    }
    return null
}

private fun accord(t: Terme, sexe: String): String = when (sexe) {
    "M" -> t.m
    "F" -> t.f
    else -> t.n
}

private fun commenceParVoyelle(s: String): Boolean {
    val c = sansAccents(s).lowercase().firstOrNull() ?: return false
    return c in "aeiouyh"
}

// "Marie est la cousine germaine de Pierre" ; "Paul est l'oncle d'Anne".
private fun phraseRelation(sujet: String, sexeSujet: String, t: Terme, objet: String): String {
    val mot = accord(t, sexeSujet)
    val article = when (sexeSujet) {
        "M" -> if (commenceParVoyelle(mot)) "l'" else "le "
        "F" -> if (commenceParVoyelle(mot)) "l'" else "la "
        else -> ""
    }
    val de = if (commenceParVoyelle(objet)) "d'" else "de "
    return "$sujet est $article$mot $de$objet"
}

fun calculerParente(
    x: Long,
    y: Long,
    noeuds: Map<Long, NoeudParente>,
    nom: (Long) -> String
): ResultatParente {
    val nx = noeuds[x]
    val ny = noeuds[y]
    if (nx == null || ny == null) {
        return ResultatParente(false, "", "Personne introuvable.", "", 0, emptyList(), false, "")
    }
    if (x == y) {
        return ResultatParente(false, "", "C'est la m\u00eame personne.", "", 0, emptyList(), false, "")
    }
    val dir = relationSang(x, y, noeuds) ?: relationAlliance(x, y, noeuds)
    if (dir == null) {
        return ResultatParente(
            false, "",
            "Aucun lien de parent\u00e9 connu entre " + nom(x) + " et " + nom(y) + " dans cet arbre.",
            "", 0, emptyList(), false,
            "Seuls les liens saisis (parents, conjoints) sont suivis."
        )
    }
    val inv = relationSang(y, x, noeuds) ?: relationAlliance(y, x, noeuds)
    val aussiConjoints = !dir.alliance && ny.id in nx.conjoints
    var phrase = phraseRelation(nom(y), ny.sexe, dir.terme, nom(x))
    var inverse = if (inv != null) phraseRelation(nom(x), nx.sexe, inv.terme, nom(y)) else ""
    if (aussiConjoints) {
        phrase += " (ils sont aussi conjoints)"
        if (inverse.isNotEmpty()) inverse += " (ils sont aussi conjoints)"
    }
    return ResultatParente(
        trouve = true,
        lien = accord(dir.terme, ny.sexe),
        phrase = phrase,
        phraseInverse = inverse,
        degreCivil = dir.degre,
        ancetresCommuns = dir.communs,
        alliance = dir.alliance,
        remarque = dir.remarque
    )
}
