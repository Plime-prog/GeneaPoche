package com.plime.geneapoche

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

// ---------------------------------------------------------------- Archives departementales

// Noms des departements (numerotation officielle).
// Les lieux saisis dans les fiches peuvent comporter le numero entre parentheses :
// "Roubaix (59)" -> AD du Nord.
val NomsDepartements: Map<Int, String> = linkedMapOf(
    1 to "Ain", 2 to "Aisne", 3 to "Allier", 4 to "Alpes-de-Haute-Provence", 5 to "Hautes-Alpes",
    6 to "Alpes-Maritimes", 7 to "Ard\u00E8che", 8 to "Ardennes", 9 to "Ari\u00E8ge", 10 to "Aube",
    11 to "Aude", 12 to "Aveyron", 13 to "Bouches-du-Rh\u00F4ne", 14 to "Calvados", 15 to "Cantal",
    16 to "Charente", 17 to "Charente-Maritime", 18 to "Cher", 19 to "Corr\u00E8ze", 20 to "Corse (2A/2B)",
    21 to "C\u00F4te-d'Or", 22 to "C\u00F4tes-d'Armor", 23 to "Creuse", 24 to "Dordogne", 25 to "Doubs",
    26 to "Dr\u00F4me", 27 to "Eure", 28 to "Eure-et-Loir", 29 to "Finist\u00E8re", 30 to "Gard",
    31 to "Haute-Garonne", 32 to "Gers", 33 to "Gironde", 34 to "H\u00E9rault", 35 to "Ille-et-Vilaine",
    36 to "Indre", 37 to "Indre-et-Loire", 38 to "Is\u00E8re", 39 to "Jura", 40 to "Landes",
    41 to "Loir-et-Cher", 42 to "Loire", 43 to "Haute-Loire", 44 to "Loire-Atlantique", 45 to "Loiret",
    46 to "Lot", 47 to "Lot-et-Garonne", 48 to "Loz\u00E8re", 49 to "Maine-et-Loire", 50 to "Manche",
    51 to "Marne", 52 to "Haute-Marne", 53 to "Mayenne", 54 to "Meurthe-et-Moselle", 55 to "Meuse",
    56 to "Morbihan", 57 to "Moselle", 58 to "Ni\u00E8vre", 59 to "Nord", 60 to "Oise",
    61 to "Orne", 62 to "Pas-de-Calais", 63 to "Puy-de-D\u00F4me", 64 to "Pyr\u00E9n\u00E9es-Atlantiques", 65 to "Hautes-Pyr\u00E9n\u00E9es",
    66 to "Pyr\u00E9n\u00E9es-Orientales", 67 to "Bas-Rhin", 68 to "Haut-Rhin", 69 to "Rh\u00F4ne", 70 to "Haute-Sa\u00F4ne",
    71 to "Sa\u00F4ne-et-Loire", 72 to "Sarthe", 73 to "Savoie", 74 to "Haute-Savoie", 75 to "Paris",
    76 to "Seine-Maritime", 77 to "Seine-et-Marne", 78 to "Yvelines", 79 to "Deux-S\u00E8vres", 80 to "Somme",
    81 to "Tarn", 82 to "Tarn-et-Garonne", 83 to "Var", 84 to "Vaucluse", 85 to "Vend\u00E9e",
    86 to "Vienne", 87 to "Haute-Vienne", 88 to "Vosges", 89 to "Yonne", 90 to "Territoire de Belfort",
    91 to "Essonne", 92 to "Hauts-de-Seine", 93 to "Seine-Saint-Denis", 94 to "Val-de-Marne", 95 to "Val-d'Oise",
    971 to "Guadeloupe", 972 to "Martinique", 973 to "Guyane", 974 to "La R\u00E9union", 976 to "Mayotte"
)

// Extrait le numero de departement d'un lieu : "Roubaix (59)" -> 59.
// Sinon, un numero isole "59", ou le nom du departement.
fun numeroDepartement(lieu: String): Int? {
    val t = lieu.trim()
    if (t.isEmpty()) return null
    Regex("\\((\\d{1,3})\\)").find(t)?.let { m ->
        val n = m.groupValues[1].toIntOrNull()
        if (n != null && NomsDepartements.containsKey(n)) return n
    }
    Regex("(?<!\\d)(\\d{1,3})(?!\\d)").find(t)?.let { m ->
        val n = m.groupValues[1].toIntOrNull()
        if (n != null && NomsDepartements.containsKey(n)) return n
    }
    for ((n, nom) in NomsDepartements) {
        if (Regex("(?i)\\b" + Regex.escape(nom) + "\\b").containsMatchIn(t)) return n
    }
    return null
}

// Ouvre le navigateur sur les archives departementales du departement.
// L'application n'a AUCUNE autorisation Internet : c'est le navigateur qui consulte,
// et la requete ne contient aucune donnee personnelle, seulement le numero et le nom du departement.
fun ouvrirArchives(ctx: Context, n: Int): String? {
    val nom = NomsDepartements[n] ?: ""
    val requete = URLEncoder.encode("archives departementales " + n.toString() + " " + nom, "UTF-8")
    return try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + requete)))
        null
    } catch (e: Exception) {
        "Aucun navigateur disponible."
    }
}
