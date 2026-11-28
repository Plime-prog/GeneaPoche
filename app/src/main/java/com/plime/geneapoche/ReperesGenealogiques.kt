package com.plime.geneapoche

// v0.154 : "Reperes genealogiques" d'une personne : phrases CALCULEES depuis ses
// dates (majorite, mariage sans consentement, conscription, recensements,
// veuvage), dans l'esprit des blogs de genealogie. Bloc replie dans la fiche :
// la chronologie reste legere. Tout est local ; aucune donnee n'est enregistree.

private fun anneeNaissanceRepere(p: Personne): Pair<Int, Boolean>? {
    anneeDe(p.dateNaissance)?.let { return it to false }
    if (p.naissanceCalculee.isNotBlank()) RX_ANNEE4.find(p.naissanceCalculee)?.value?.toIntOrNull()?.let { return it to true }
    return null
}

// Majorite matrimoniale (age sous lequel le consentement des parents est requis).
fun majoriteMatrimoniale(annee: Int, femme: Boolean): Int? = when {
    annee < 1556 -> null
    annee < 1792 -> if (femme) 25 else 30
    annee < 1804 -> 21
    annee < 1907 -> if (femme) 21 else 25
    annee < 1974 -> 21
    else -> 18
}

fun reperesGenealogiquesDe(p: Personne, personnes: List<Personne>): List<String> {
    val naiss = anneeNaissanceRepere(p) ?: return emptyList()
    val an = naiss.first
    val vers = if (naiss.second) "vers " else ""
    val femme = p.sexe == "F"
    val fin = anneeDe(p.dateDeces)
    val res = mutableListOf<String>()
    val prenom = p.prenoms.trim().split(" ").firstOrNull().orEmpty().ifBlank { "La personne" }
    val ne = if (femme) "Née" else "Né"

    // Majorite civile.
    val majeur = when {
        an + 21 < 1792 -> null
        an + 21 <= 1974 -> an + 21 to 21
        an + 18 >= 1974 -> an + 18 to 18
        else -> 1974 to 18
    }
    if (majeur != null && (fin == null || fin >= majeur.first))
        res.add((if (femme) "Majeure" else "Majeur") + " en " + vers + majeur.first + " (" + majeur.second + " ans" +
            (if (majeur.second == 21) ", Code civil de 1804" else ", loi du 5 juillet 1974") + ").")

    // Mariage : consentement des parents et emancipation.
    val anM = anneeDe(p.dateMariage)
    if (anM != null && anM >= 1556) {
        val age = anM - an
        val seuil = majoriteMatrimoniale(anM, femme)
        val minimum = if (anM >= 1804) (if (femme) 15 else 18) else 0
        if (seuil != null && age >= minimum && age < seuil) {
            res.add("À son mariage (" + anM + "), " + prenom + " a " + (if (naiss.second) "environ " else "") + age +
                " ans : en dessous de la majorité matrimoniale (" + seuil + " ans), le consentement de ses parents était requis" +
                (if (anM in 1804..1973 && age < 21) "; le mariage l'émancipait de plein droit" else "") + ".")
        }
    }

    // Conscription (hommes) : tirage au sort de 1798 a 1888, puis service obligatoire ;
    // duree LEGALE du service pour la classe (indicative) et guerres pendant sa vie.
    if (p.sexe == "M" && an + 20 in 1798..1996 && (fin == null || fin >= an + 20)) {
        val classe = an + 20
        val duree = dureeServiceMilitaire(classe)
        res.add(ne + " en " + vers + an + ", il appartient \u00E0 la classe " + classe +
            (if (classe < 1889) " : soumis au tirage au sort de la conscription \u00E0 20 ans" else " : service militaire obligatoire") +
            (if (duree != null) " ; dur\u00E9e l\u00E9gale du service : " + duree + "." else "."))
    }
    if (p.sexe == "M") {
        GuerresMobilisables.forEach { g ->
            val ageDebut = g.debut.year - an
            val vivant = fin == null || fin >= g.debut.year
            if (ageDebut in g.ageMin..g.ageMax && vivant) {
                res.add("Il avait " + (if (naiss.second) "environ " else "") + ageDebut + " ans au d\u00E9but de " + g.nom + " (" +
                    libelleDureeGuerre(g) + ") : en \u00E2ge d'\u00EAtre mobilis\u00E9.")
            }
        }
    }

    // Recensements nominatifs (tous les 5 ans, 1836-1936) pendant sa vie.
    val debut = maxOf(an, 1836)
    val bout = minOf(fin ?: (an + 90), 1936)
    val recensements = (debut..bout).filter { it >= 1836 && (it - 1836) % 5 == 0 }
    if (recensements.isNotEmpty()) {
        res.add("Peut figurer aux recensements nominatifs de " + recensements.first() +
            (if (recensements.size > 1) " à " + recensements.last() + " (" + recensements.size + " en tout, tous les 5 ans)" else "") + ".")
    }

    // Veuvage d'une femme : delai de viduite de 300 jours (Code civil, art. 228).
    if (femme) {
        val veuvage = p.tousConjoints().mapNotNull { id -> personnes.firstOrNull { it.id == id } }
            .mapNotNull { anneeDe(it.dateDeces) }.maxOrNull()
        if (veuvage != null && veuvage in 1804..2004)
            res.add("Veuve en " + veuvage + " : elle ne pouvait pas se remarier avant 300 jours (délai de viduité, Code civil).")
    }
    return res
}

// v0.155 : duree LEGALE du service actif selon la classe (annee des 20 ans). Indicatif :
// les lois, les dispenses et le tirage au sort ont fait varier la duree reelle.
fun dureeServiceMilitaire(classe: Int): String? = when {
    classe < 1798 -> null
    classe < 1818 -> "dur\u00E9e variable (levées de la R\u00E9volution et de l'Empire)"
    classe < 1832 -> "6 ans (loi de 1818)"
    classe < 1868 -> "7 ans (loi de 1832)"
    classe < 1872 -> "5 ans (loi Niel, 1868)"
    classe < 1889 -> "5 ans (loi de 1872 ; le tirage au sort pouvait la r\u00E9duire \u00E0 moins d'un an)"
    classe < 1905 -> "3 ans (loi de 1889)"
    classe < 1913 -> "2 ans (loi de 1905)"
    classe < 1923 -> "3 ans (loi de 1913)"
    classe < 1928 -> "18 mois (loi de 1923)"
    classe < 1935 -> "1 an (loi de 1928)"
    classe < 1950 -> "2 ans (loi de 1935)"
    classe < 1963 -> "18 mois"
    classe < 1970 -> "16 mois"
    classe < 1992 -> "12 mois"
    classe <= 1996 -> "10 mois"
    else -> null
}

class GuerreMobilisable(val nom: String, val debut: java.time.LocalDate, val fin: java.time.LocalDate, val ageMin: Int, val ageMax: Int)

val GuerresMobilisables: List<GuerreMobilisable> = listOf(
    GuerreMobilisable("la guerre franco-prussienne", java.time.LocalDate.of(1870, 7, 19), java.time.LocalDate.of(1871, 1, 28), 20, 40),
    GuerreMobilisable("la Premi\u00E8re Guerre mondiale", java.time.LocalDate.of(1914, 8, 2), java.time.LocalDate.of(1918, 11, 11), 18, 48),
    GuerreMobilisable("la Seconde Guerre mondiale", java.time.LocalDate.of(1939, 9, 3), java.time.LocalDate.of(1945, 5, 8), 20, 48),
    GuerreMobilisable("la guerre d'Alg\u00E9rie", java.time.LocalDate.of(1954, 11, 1), java.time.LocalDate.of(1962, 3, 19), 18, 30)
)

// "du 2 ao\u00FBt 1914 au 11 novembre 1918, soit 4 ans et 3 mois".
fun libelleDureeGuerre(g: GuerreMobilisable): String {
    val per = java.time.Period.between(g.debut, g.fin.plusDays(1))
    val parts = mutableListOf<String>()
    if (per.years > 0) parts.add(per.years.toString() + (if (per.years > 1) " ans" else " an"))
    if (per.months > 0) parts.add(per.months.toString() + " mois")
    if (per.years == 0 && per.months == 0) parts.add(per.days.toString() + " jours")
    return "du " + g.debut.dayOfMonth + " " + MoisFr[g.debut.monthValue - 1] + " " + g.debut.year + " au " +
        g.fin.dayOfMonth + " " + MoisFr[g.fin.monthValue - 1] + " " + g.fin.year + ", soit " + parts.joinToString(" et ")
}

private val MoisFr = listOf("janvier", "f\u00E9vrier", "mars", "avril", "mai", "juin", "juillet", "ao\u00FBt", "septembre", "octobre", "novembre", "d\u00E9cembre")
