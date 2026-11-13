package com.plime.geneapoche

// ---------------------------------------------------------------- Statistiques (v0.100)
// Tableau de bord d'un OUTIL DE RECHERCHE : ce qui est documente, ce qui
// manque, et les grandes mesures de l'arbre. Fonctions PURES (testees sur
// JVM) ; l'ecran est dans EcranStatistiques.kt.

// Un taux "x sur y" : nombre, base, pourcentage entier (0 si base nulle).
class Taux(val nombre: Int, val base: Int) {
    val pourcent: Int get() = if (base <= 0) 0 else (nombre * 100) / base
    val fraction: Float get() = if (base <= 0) 0f else (nombre.toFloat() / base.toFloat()).coerceIn(0f, 1f)
}

class StatsBase(
    val individus: Int,
    val familles: Int,
    val ancetres: Int,
    val descendants: Int,
    val generations: Int,
    val communes: Int,
    val patronymes: Int,
    val metiers: Int,
    val patronymesTop: List<Pair<String, Int>>,
    val metiersTop: List<Pair<String, Int>>,
    val vieMoyenne: Double?,
    val ageMariageMoyen: Double?,
    val enfantsParCouple: Double?,
    val anneeDebut: Int?,
    val anneeFin: Int?,
    val plusAncien: Personne?,
    val lieuFrequent: Pair<String, Int>?,
    // Qualite de la documentation.
    val naissances: Taux,
    val deces: Taux,
    val mariages: Taux,
    val mariagesSources: Taux,
    val personnesSourcees: Taux,
    val lieuxNaissance: Taux,
    // Trous genealogiques : (renseignements manquants, attendus) sur toute la base.
    val trousManquants: Int,
    val trousAttendus: Int
)

// Somme des indicateurs n/6 (n/4, n/2) de la base : ce qui reste a trouver
// pour une genealogie exhaustive (date + lieu de N, M, D).
fun trousGenealogiques(personnes: List<Personne>): Pair<Int, Int> {
    var manque = 0
    var attendu = 0
    personnes.forEach { p ->
        val (n, total) = indicateurPersonne(p)
        manque += (total - n).coerceAtLeast(0)
        attendu += total
    }
    return Pair(manque, attendu)
}

private fun cleNomStat(s: String): String = sansAccents(s.trim()).lowercase()

private fun top(valeurs: List<String>, n: Int): List<Pair<String, Int>> {
    // On garde la graphie la plus frequente pour l'affichage.
    val groupes = valeurs.filter { it.isNotBlank() }.groupBy { cleNomStat(it) }
    return groupes.map { (_, l) ->
        val graphie = l.groupingBy { it.trim() }.eachCount().maxByOrNull { it.value }?.key ?: l.first().trim()
        Pair(graphie, l.size)
    }.sortedWith(compareBy({ -it.second }, { it.first })).take(n)
}

// Couples distincts (liens conjoint + conjoints supplementaires) : paires
// non ordonnees d'identifiants.
fun couplesDe(personnes: List<Personne>): List<Pair<Long, Long>> {
    val vus = LinkedHashSet<Pair<Long, Long>>()
    personnes.forEach { p ->
        val liens = mutableListOf<Long>()
        if (p.conjoint != 0L) liens.add(p.conjoint)
        liens.addAll(p.conjointsSupp)
        liens.forEach { c ->
            if (c != 0L && c != p.id) vus.add(Pair(minOf(p.id, c), maxOf(p.id, c)))
        }
    }
    return vus.toList()
}

fun statsBase(personnes: List<Personne>, citations: List<Citation>): StatsBase {
    val parId = personnes.associateBy { it.id }
    val couples = couplesDe(personnes)

    // De cujus = plus petit numero Sosa ; ancetres = tous les numerotes.
    val numerotes = personnes.mapNotNull { p -> p.sosa.trim().toLongOrNull()?.let { Pair(p, it) } }
    val base = numerotes.minByOrNull { it.second }
    val ancetres = if (base == null) 0 else numerotes.count { it.first.id != base.first.id }
    fun longueur(n: Long): Int = 64 - java.lang.Long.numberOfLeadingZeros(n)
    val generations = if (base == null) 0 else
        (numerotes.maxOf { longueur(it.second) } - longueur(base.second) + 1)
    var descendants = 0
    if (base != null) {
        val vus = HashSet<Long>()
        val file = ArrayDeque<Long>()
        file.addLast(base.first.id)
        vus.add(base.first.id)
        while (file.isNotEmpty()) {
            val p = parId[file.removeFirst()] ?: continue
            p.enfants.forEach { e ->
                if (vus.add(e)) {
                    file.addLast(e)
                    descendants++
                }
            }
        }
    }

    // Communes distinctes (toutes les rubriques) et lieu le plus frequent.
    val lieux = mutableListOf<String>()
    personnes.forEach { p ->
        listOf(p.lieuNaissance, p.lieuBapteme, p.lieuMariage, p.lieuDeces, p.lieuSepulture).forEach { l ->
            if (l.isNotBlank()) {
                val c = communeDuLieu(l)
                if (c.isNotBlank()) lieux.add(c)
            }
        }
    }
    val lieuxTop = top(lieux, 1)

    val patros = top(personnes.map { it.nom }, 3)
    val metiers = top(personnes.map { it.profession }, 3)
    val nbPatros = personnes.map { cleNomStat(it.nom) }.filter { it.isNotEmpty() }.toSet().size
    val nbMetiers = personnes.map { cleNomStat(it.profession) }.filter { it.isNotEmpty() }.toSet().size

    // Durees de vie et ages au mariage (annees seules, bornes de bon sens).
    val vies = personnes.mapNotNull { p ->
        val n = anneeDe(p.dateNaissance)
        val d = anneeDe(p.dateDeces)
        if (n != null && d != null && d - n in 0..120) (d - n).toDouble() else null
    }
    val agesMariage = personnes.mapNotNull { p ->
        val n = anneeDe(p.dateNaissance)
        val m = anneeDe(p.dateMariage)
        if (n != null && m != null && m - n in 12..90) (m - n).toDouble() else null
    }
    val parentsAvecEnfants = personnes.filter { it.enfants.isNotEmpty() }
    val enfantsMoy = if (parentsAvecEnfants.isEmpty()) null
        else parentsAvecEnfants.sumOf { it.enfants.size }.toDouble() / parentsAvecEnfants.size

    // Periode couverte et ancetre le plus ancien.
    val annees = mutableListOf<Int>()
    personnes.forEach { p ->
        listOf(p.dateNaissance, p.dateBapteme, p.dateMariage, p.dateDeces, p.dateSepulture).forEach { d ->
            anneeDe(d)?.let { annees.add(it) }
        }
    }
    val plusAncien = personnes
        .filter { anneeDe(it.dateNaissance) != null }
        .minByOrNull { anneeDe(it.dateNaissance) ?: 9999 }

    // Documentation.
    val avecNaissance = personnes.count { it.dateNaissance.isNotBlank() }
    val attendusDeces = personnes.filter { !presumeVivant(it) }
    val avecDeces = attendusDeces.count { it.dateDeces.isNotBlank() }
    val mariagesDates = couples.count { (a, b) ->
        (parId[a]?.dateMariage?.isNotBlank() == true) || (parId[b]?.dateMariage?.isNotBlank() == true)
    }
    val mariagesSrc = couples.count { (a, b) ->
        citations.any { it.cible == "M" && (it.personneId == a || it.personneId == b) }
    }
    val sourcees = personnes.count { p -> citations.any { it.personneId == p.id } }
    val lieuNaiss = personnes.count { lieuComplet(it.lieuNaissance, it.deptNaissance) }
    val (manque, attendu) = trousGenealogiques(personnes)

    return StatsBase(
        individus = personnes.size,
        familles = couples.size,
        ancetres = ancetres,
        descendants = descendants,
        generations = generations,
        communes = lieux.map { cleNomStat(it) }.toSet().size,
        patronymes = nbPatros,
        metiers = nbMetiers,
        patronymesTop = patros,
        metiersTop = metiers,
        vieMoyenne = if (vies.isEmpty()) null else vies.average(),
        ageMariageMoyen = if (agesMariage.isEmpty()) null else agesMariage.average(),
        enfantsParCouple = enfantsMoy,
        anneeDebut = annees.minOrNull(),
        anneeFin = annees.maxOrNull(),
        plusAncien = plusAncien,
        lieuFrequent = lieuxTop.firstOrNull(),
        naissances = Taux(avecNaissance, personnes.size),
        deces = Taux(avecDeces, attendusDeces.size),
        mariages = Taux(mariagesDates, couples.size),
        mariagesSources = Taux(mariagesSrc, couples.size),
        personnesSourcees = Taux(sourcees, personnes.size),
        lieuxNaissance = Taux(lieuNaiss, personnes.size),
        trousManquants = manque,
        trousAttendus = attendu
    )
}

// ---------------------------------------------------------------- Recherche avancee (v0.100, 12.7)
// Filtres combines (ET) sur les personnes : texte (nom, prenoms, lieu),
// periode, lieu, statut de verification, acte manquant.

class FiltresRecherche(
    val texte: String = "",
    val lieu: String = "",
    val anneeDe: Int? = null,
    val anneeA: Int? = null,
    // "" = tous ; "certifiee" ; "non"
    val statut: String = "",
    // Types d'acte manquants cherches ("N","B","M","D","S") : la personne
    // doit en manquer AU MOINS UN.
    val manquants: Set<String> = emptySet()
) {
    val actif: Boolean
        get() = texte.isNotBlank() || lieu.isNotBlank() || anneeDe != null || anneeA != null ||
            statut.isNotEmpty() || manquants.isNotEmpty()
}

// Evenement "manquant" : pas de date COMPLETE ou pas de lieu complet. Les
// evenements qui n'ont pas lieu d'etre (mariage sans conjoint, deces d'un
// vivant presume...) ne sont jamais manquants. B et S : manquants
// seulement s'ils sont attendus (ici : jamais, ils sont facultatifs),
// sauf si la personne porte deja une information partielle.
fun acteManquant(p: Personne, type: String): Boolean {
    val c = champsEvenement(p, type)
    return when (type) {
        "N" -> !dateComplete(c.date) || !lieuComplet(c.lieu, c.dept)
        "M" -> (p.conjoint != 0L || p.conjointsSupp.isNotEmpty()) && !vivantMoinsDe30(p) &&
            (!dateComplete(c.date) || !lieuComplet(c.lieu, c.dept))
        "D" -> !presumeVivant(p) && !vivantMoinsDe30(p) && (!dateComplete(c.date) || !lieuComplet(c.lieu, c.dept))
        else -> (c.date.isNotBlank() || c.lieu.isNotBlank()) && (!dateComplete(c.date) || c.lieu.isBlank())
    }
}

private fun lieuxDe(p: Personne): List<String> =
    listOf(p.lieuNaissance, p.lieuBapteme, p.lieuMariage, p.lieuDeces, p.lieuSepulture, p.domicile) +
        p.evenementsLibres.map { it.lieu }

private fun anneesDe(p: Personne): List<Int> =
    listOf(p.dateNaissance.ifBlank { p.naissanceCalculee }, p.dateBapteme, p.dateMariage, p.dateDeces, p.dateSepulture)
        .mapNotNull { anneeDe(it) }

fun correspond(p: Personne, f: FiltresRecherche): Boolean {
    if (f.texte.isNotBlank()) {
        val t = cleNomStat(f.texte)
        val champ = cleNomStat(p.nom + " " + p.prenoms + " " + p.profession + " " + lieuxDe(p).joinToString(" "))
        if (!champ.contains(t)) return false
    }
    if (f.lieu.isNotBlank()) {
        val l = cleNomStat(f.lieu)
        if (lieuxDe(p).none { cleNomStat(it).contains(l) }) return false
    }
    if (f.anneeDe != null || f.anneeA != null) {
        val ans = anneesDe(p)
        if (ans.isEmpty()) return false
        if (ans.none { (f.anneeDe == null || it >= f.anneeDe) && (f.anneeA == null || it <= f.anneeA) }) return false
    }
    when (f.statut) {
        "certifiee" -> if (!identiteCertifiee(p)) return false
        "non" -> if (identiteCertifiee(p)) return false
    }
    if (f.manquants.isNotEmpty() && f.manquants.none { acteManquant(p, it) }) return false
    return true
}

fun filtrerPersonnes(personnes: List<Personne>, f: FiltresRecherche): List<Personne> =
    personnes.filter { correspond(it, f) }
