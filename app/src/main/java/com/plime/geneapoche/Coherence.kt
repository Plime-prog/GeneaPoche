package com.plime.geneapoche

import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// ================================================================ v0.88
// "Anomalies a verifier" : controles de coherence du diagnostic de l'arbre.
// Logique PURE (aucune dependance Android ni Personne) : testee en JVM.
//
// Principe : une anomalie n'est signalee que si la contradiction est CERTAINE
// compte tenu de la precision des dates (une annee seule couvre toute
// l'annee, "avant 1850" couvre tout ce qui precede, "vers 1750" +/- 3 ans).
// Ce sont des anomalies A VERIFIER, jamais des erreurs : l'utilisateur
// decide (Ignorer, Corriger, Examiner).

data class EvLieu(val type: String, val date: String, val dept: String)

// Copie minimale des donnees d'une Personne utiles aux controles.
data class NoeudCoh(
    val id: Long,
    val nom: String,
    val sexe: String,
    val naissance: String,
    val bapteme: String,
    val mariage: String,
    val deces: String,
    val sepulture: String,
    val pere: Long,
    val mere: Long,
    val conjoints: List<Long>,
    val evenementsLibres: List<Pair<String, String>>,
    val lieux: List<EvLieu>
)

// niveau 1 : contradiction logique ; niveau 2 : improbable, a verifier.
data class AnomalieAVerifier(
    val cle: String,
    val type: String,
    val niveau: Int,
    val texte: String,
    val personneId: Long,
    val concernes: List<Long>,
    val explication: String,
    val details: List<String>
)

// Fourchette de dates possibles d'une date saisie.
class Intervalle(val debut: LocalDate, val fin: LocalDate) {
    val exact: Boolean get() = debut == fin
}

private val TresTot: LocalDate = LocalDate.of(1, 1, 1)
private val TresTard: LocalDate = LocalDate.of(9999, 12, 31)

private fun annee(a: Int): Boolean = a in 100..9990

fun intervalleDe(s: String): Intervalle? {
    val t = s.trim()
    if (t.isEmpty()) return null
    val bas = sansAccents(t).lowercase()
    val an = Regex("[0-9]{4}").find(bas)?.value?.toIntOrNull()
    if (bas.startsWith("avant") || bas.startsWith("bef")) {
        if (an == null || !annee(an)) return null
        return Intervalle(TresTot, LocalDate.of(an, 1, 1).minusDays(1))
    }
    if (bas.startsWith("apres") || bas.startsWith("aft")) {
        if (an == null || !annee(an)) return null
        return Intervalle(LocalDate.of(an, 12, 31).plusDays(1), TresTard)
    }
    if (bas.startsWith("vers") || bas.startsWith("ca ") || bas.startsWith("ca.") ||
        bas.startsWith("env") || bas.startsWith("circa") || bas.startsWith("abt")) {
        if (an == null || !annee(an)) return null
        return Intervalle(LocalDate.of(an - 3, 1, 1), LocalDate.of(an + 3, 12, 31))
    }
    val pd = lireDatePartielle(t)
    if (pd != null && annee(pd.annee)) {
        val m = pd.mois
        val j = pd.jour
        return if (m != null && j != null) {
            val d = LocalDate.of(pd.annee, m, j)
            Intervalle(d, d)
        } else if (m != null) {
            val ym = YearMonth.of(pd.annee, m)
            Intervalle(ym.atDay(1), ym.atEndOfMonth())
        } else {
            Intervalle(LocalDate.of(pd.annee, 1, 1), LocalDate.of(pd.annee, 12, 31))
        }
    }
    if (an != null && annee(an)) return Intervalle(LocalDate.of(an, 1, 1), LocalDate.of(an, 12, 31))
    return null
}

private fun certainementAvant(a: Intervalle, b: Intervalle): Boolean = a.fin.isBefore(b.debut)

// Age minimal / maximal (en annees revolues) a une date d'evenement.
private fun ageMin(naissance: Intervalle, evt: Intervalle): Int =
    if (evt.debut.isAfter(naissance.fin)) Period.between(naissance.fin, evt.debut).years else 0

private fun ageMax(naissance: Intervalle, evt: Intervalle): Int =
    if (evt.fin.isBefore(naissance.debut)) -1 else Period.between(naissance.debut, evt.fin).years

private fun jours(a: LocalDate, b: LocalDate): Long = ChronoUnit.DAYS.between(a, b)

// 120 -> "4 mois" ; 9 -> "9 jours" ; 800 -> "2 ans".
fun dureeTexte(nbJours: Long): String {
    val j = Math.abs(nbJours)
    if (j < 31) return j.toString() + (if (j > 1) " jours" else " jour")
    val mois = Math.round(j / 30.44)
    if (mois < 24) return mois.toString() + " mois"
    val ans = Math.round(j / 365.25)
    return ans.toString() + (if (ans > 1) " ans" else " an")
}

private fun nePar(sexe: String): String = when (sexe) {
    "M" -> "n\u00e9"
    "F" -> "n\u00e9e"
    else -> "n\u00e9(e)"
}

private fun decedePar(sexe: String): String = when (sexe) {
    "M" -> "d\u00e9c\u00e9d\u00e9"
    "F" -> "d\u00e9c\u00e9d\u00e9e"
    else -> "d\u00e9c\u00e9d\u00e9(e)"
}

private fun maryePar(sexe: String): String = when (sexe) {
    "M" -> "mari\u00e9"
    "F" -> "mari\u00e9e"
    else -> "mari\u00e9(e)"
}

private fun lignesDates(p: NoeudCoh): String {
    val l = mutableListOf<String>()
    if (p.naissance.isNotBlank()) l.add("naissance " + p.naissance.trim())
    if (p.bapteme.isNotBlank()) l.add("bapt\u00eame " + p.bapteme.trim())
    if (p.mariage.isNotBlank()) l.add("mariage " + p.mariage.trim())
    if (p.deces.isNotBlank()) l.add("d\u00e9c\u00e8s " + p.deces.trim())
    if (p.sepulture.isNotBlank()) l.add("s\u00e9pulture " + p.sepulture.trim())
    val nom = p.nom.trim().ifEmpty { "(sans nom)" }
    return if (l.isEmpty()) nom + " : aucune date" else nom + " : " + l.joinToString(", ")
}

private fun cleDeptCoh(s: String): String =
    sansAccents(s).lowercase().replace(Regex("[^a-z0-9]+"), "")

fun anomaliesAVerifier(
    noeuds: List<NoeudCoh>,
    deptConnu: (String) -> Boolean = { true }
): List<AnomalieAVerifier> {
    val res = mutableListOf<AnomalieAVerifier>()
    val parId = HashMap<Long, NoeudCoh>()
    noeuds.forEach { parId[it.id] = it }
    val vus = HashSet<String>()

    fun nomDe(p: NoeudCoh): String = p.nom.trim().ifEmpty { "(sans nom)" }

    fun add(
        type: String, niveau: Int, ids: List<Long>, texte: String,
        explication: String, concernes: List<NoeudCoh>, dedoublonne: Boolean = false
    ) {
        val sortedIds = ids.sorted().joinToString(",")
        if (dedoublonne && !vus.add(type + "|" + sortedIds)) return
        res.add(
            AnomalieAVerifier(
                cle = type + "|" + sortedIds + "|" + texte.hashCode().toString(),
                type = type,
                niveau = niveau,
                texte = texte,
                personneId = ids.first(),
                concernes = concernes.map { it.id }.distinct(),
                explication = explication,
                details = concernes.distinctBy { it.id }.map { lignesDates(it) }
            )
        )
    }

    for (p in noeuds) {
        val t = nomDe(p)
        val n = intervalleDe(p.naissance)
        val d = intervalleDe(p.deces)
        val m = intervalleDe(p.mariage)
        val b = intervalleDe(p.bapteme)
        val s = intervalleDe(p.sepulture)

        // --- Contradictions entre les evenements d'une meme personne.
        if (n != null && d != null && certainementAvant(d, n)) {
            add(
                "deces-avant-naissance", 1, listOf(p.id),
                t + " est " + decedePar(p.sexe) + " (" + p.deces.trim() + ") avant sa naissance (" + p.naissance.trim() + ").",
                "Le d\u00e9c\u00e8s ne peut pas pr\u00e9c\u00e9der la naissance.", listOf(p)
            )
        }
        if (n != null && m != null && certainementAvant(m, n)) {
            add(
                "mariage-avant-naissance", 1, listOf(p.id),
                t + " est " + maryePar(p.sexe) + " (" + p.mariage.trim() + ") avant sa naissance (" + p.naissance.trim() + ").",
                "Le mariage ne peut pas pr\u00e9c\u00e9der la naissance.", listOf(p)
            )
        }
        if (d != null && m != null && certainementAvant(d, m)) {
            add(
                "mariage-apres-deces", 1, listOf(p.id),
                t + " est " + maryePar(p.sexe) + " (" + p.mariage.trim() + ") apr\u00e8s son d\u00e9c\u00e8s (" + p.deces.trim() + ").",
                "Le mariage ne peut pas suivre le d\u00e9c\u00e8s de la personne mari\u00e9e.", listOf(p)
            )
        }
        if (n != null && b != null && certainementAvant(b, n)) {
            add(
                "bapteme-avant-naissance", 1, listOf(p.id),
                "Le bapt\u00eame de " + t + " (" + p.bapteme.trim() + ") est avant sa naissance (" + p.naissance.trim() + ").",
                "Le bapt\u00eame suit la naissance.", listOf(p)
            )
        }
        if (d != null && b != null && certainementAvant(d, b)) {
            add(
                "bapteme-apres-deces", 1, listOf(p.id),
                "Le bapt\u00eame de " + t + " (" + p.bapteme.trim() + ") est apr\u00e8s son d\u00e9c\u00e8s (" + p.deces.trim() + ").",
                "Un bapt\u00eame ne peut pas avoir lieu apr\u00e8s le d\u00e9c\u00e8s.", listOf(p)
            )
        }
        if (d != null && s != null && certainementAvant(s, d)) {
            add(
                "sepulture-avant-deces", 1, listOf(p.id),
                "La s\u00e9pulture de " + t + " (" + p.sepulture.trim() + ") est avant son d\u00e9c\u00e8s (" + p.deces.trim() + ").",
                "La s\u00e9pulture suit le d\u00e9c\u00e8s.", listOf(p)
            )
        }
        if (n != null && s != null && d == null && certainementAvant(s, n)) {
            add(
                "sepulture-avant-naissance", 1, listOf(p.id),
                "La s\u00e9pulture de " + t + " (" + p.sepulture.trim() + ") est avant sa naissance (" + p.naissance.trim() + ").",
                "La s\u00e9pulture ne peut pas pr\u00e9c\u00e9der la naissance.", listOf(p)
            )
        }
        if (n != null) {
            for ((intitule, date) in p.evenementsLibres) {
                val e = intervalleDe(date) ?: continue
                if (certainementAvant(e, n)) {
                    add(
                        "evenement-avant-naissance", 1, listOf(p.id),
                        "L'\u00e9v\u00e9nement \u00ab " + intitule.trim().ifEmpty { "sans titre" } + " \u00bb de " + t +
                            " (" + date.trim() + ") est avant sa naissance (" + p.naissance.trim() + ").",
                        "Un \u00e9v\u00e9nement personnel ne peut pas pr\u00e9c\u00e9der la naissance.", listOf(p)
                    )
                }
            }
        }
        if (n != null && d != null && ageMin(n, d) > 110) {
            add(
                "age-extreme", 2, listOf(p.id),
                t + " aurait au moins " + ageMin(n, d).toString() + " ans au d\u00e9c\u00e8s (naissance " + p.naissance.trim() +
                    ", d\u00e9c\u00e8s " + p.deces.trim() + ").",
                "Au-del\u00e0 de 110 ans, une date est probablement fausse.", listOf(p)
            )
        }
        // --- Age au mariage.
        if (n != null && m != null) {
            if (ageMax(n, m) in 0..14) {
                add(
                    "mariage-trop-jeune", 2, listOf(p.id),
                    t + " aurait au plus " + ageMax(n, m).toString() + " an(s) au mariage (" + p.mariage.trim() + ").",
                    "Mariage \u00e0 moins de 15 ans : improbable.", listOf(p)
                )
            }
            if (ageMin(n, m) > 85) {
                add(
                    "mariage-trop-age", 2, listOf(p.id),
                    t + " aurait au moins " + ageMin(n, m).toString() + " ans au mariage (" + p.mariage.trim() + ").",
                    "Mariage \u00e0 plus de 85 ans : improbable.", listOf(p)
                )
            }
        }
        // --- Mariage et conjoint unique (la date de mariage n'est pas ambigue).
        if (m != null && p.conjoints.size == 1) {
            val c = parId[p.conjoints[0]]
            if (c != null) {
                val cd = intervalleDe(c.deces)
                val cn = intervalleDe(c.naissance)
                if (cd != null && certainementAvant(cd, m)) {
                    add(
                        "mariage-apres-deces-conjoint", 1, listOf(p.id, c.id),
                        t + " est " + maryePar(p.sexe) + " (" + p.mariage.trim() + ") apr\u00e8s le d\u00e9c\u00e8s de son conjoint " +
                            nomDe(c) + " (" + c.deces.trim() + ").",
                        "On ne peut pas se marier avec une personne d\u00e9j\u00e0 d\u00e9c\u00e9d\u00e9e.", listOf(p, c), true
                    )
                }
                if (cn != null && certainementAvant(m, cn)) {
                    add(
                        "mariage-avant-naissance-conjoint", 1, listOf(p.id, c.id),
                        "Le mariage de " + t + " (" + p.mariage.trim() + ") est avant la naissance de son conjoint " +
                            nomDe(c) + " (" + c.naissance.trim() + ").",
                        "Le conjoint n'\u00e9tait pas encore n\u00e9.", listOf(p, c), true
                    )
                }
                val cm = intervalleDe(c.mariage)
                if (cm != null && c.conjoints.size == 1 && m.exact && cm.exact && m.debut != cm.debut) {
                    add(
                        "mariage-dates-differentes", 2, listOf(p.id, c.id),
                        "Les deux fiches donnent deux dates de mariage : " + t + " (" + p.mariage.trim() + ") et " +
                            nomDe(c) + " (" + c.mariage.trim() + ").",
                        "Un m\u00eame mariage doit avoir la m\u00eame date sur les deux fiches.", listOf(p, c), true
                    )
                }
            }
        }
        // --- Lieux.
        val dejaUnJour = HashMap<LocalDate, EvLieu>()
        for (ev in p.lieux) {
            val dept = ev.dept.trim()
            if (dept.isNotEmpty() && !deptConnu(dept)) {
                add(
                    "lieu-inconnu", 2, listOf(p.id),
                    "Le d\u00e9partement \u00ab " + dept + " \u00bb (" + ev.type + " de " + t + ") n'existe pas.",
                    "Num\u00e9ro de d\u00e9partement inconnu : lieu \u00e0 v\u00e9rifier.", listOf(p)
                )
            }
            val iv = intervalleDe(ev.date)
            if (iv != null && iv.exact && dept.isNotEmpty()) {
                val autre = dejaUnJour[iv.debut]
                if (autre != null && cleDeptCoh(autre.dept) != cleDeptCoh(dept)) {
                    add(
                        "lieux-incompatibles", 2, listOf(p.id),
                        t + " : " + autre.type + " et " + ev.type + " le m\u00eame jour (" + ev.date.trim() +
                            ") dans deux d\u00e9partements diff\u00e9rents (" + autre.dept.trim() + " et " + dept + ").",
                        "Deux \u00e9v\u00e9nements le m\u00eame jour dans des d\u00e9partements diff\u00e9rents : lieu probablement erron\u00e9.",
                        listOf(p)
                    )
                } else if (autre == null) {
                    dejaUnJour[iv.debut] = ev
                }
            }
        }
        // --- Relations avec les parents.
        if (n != null) {
            val f = parId[p.pere]
            val mo = parId[p.mere]
            if (f != null) {
                val fn = intervalleDe(f.naissance)
                val fd = intervalleDe(f.deces)
                if (fn != null) {
                    if (certainementAvant(n, fn)) {
                        add(
                            "enfant-avant-pere", 1, listOf(p.id, f.id),
                            t + " est " + nePar(p.sexe) + " (" + p.naissance.trim() + ") avant son p\u00e8re " + nomDe(f) +
                                " (" + f.naissance.trim() + ").",
                            "Un enfant ne peut pas na\u00eetre avant son p\u00e8re.", listOf(p, f)
                        )
                    } else {
                        if (ageMax(fn, n) in 0..13) {
                            add(
                                "pere-trop-jeune", 2, listOf(p.id, f.id),
                                "Le p\u00e8re " + nomDe(f) + " aurait au plus " + ageMax(fn, n).toString() +
                                    " an(s) \u00e0 la naissance de " + t + " (" + p.naissance.trim() + ").",
                                "Paternit\u00e9 \u00e0 moins de 14 ans : improbable.", listOf(p, f)
                            )
                        }
                        if (ageMin(fn, n) > 80) {
                            add(
                                "pere-trop-age", 2, listOf(p.id, f.id),
                                "Le p\u00e8re " + nomDe(f) + " aurait au moins " + ageMin(fn, n).toString() +
                                    " ans \u00e0 la naissance de " + t + " (" + p.naissance.trim() + ").",
                                "Paternit\u00e9 \u00e0 plus de 80 ans : improbable.", listOf(p, f)
                            )
                        }
                    }
                }
                if (fd != null) {
                    val ecart = jours(fd.fin, n.debut)
                    if (ecart > 300) {
                        val prefixe = if (fd.exact && n.exact) "" else "au moins "
                        add(
                            "naissance-longtemps-apres-deces-pere", 1, listOf(p.id, f.id),
                            t + " est " + nePar(p.sexe) + " " + prefixe + dureeTexte(ecart) + " apr\u00e8s le d\u00e9c\u00e8s suppos\u00e9 de son p\u00e8re " +
                                nomDe(f) + " (d\u00e9c\u00e8s " + f.deces.trim() + ", naissance " + p.naissance.trim() + ").",
                            "Au-del\u00e0 de 300 jours apr\u00e8s le d\u00e9c\u00e8s du p\u00e8re, une naissance est impossible : date ou filiation \u00e0 v\u00e9rifier.",
                            listOf(p, f)
                        )
                    } else if (ecart > 0) {
                        val prefixe = if (fd.exact && n.exact) "" else "au moins "
                        add(
                            "naissance-posthume", 2, listOf(p.id, f.id),
                            t + " est " + nePar(p.sexe) + " " + prefixe + dureeTexte(ecart) + " apr\u00e8s le d\u00e9c\u00e8s suppos\u00e9 de son p\u00e8re " +
                                nomDe(f) + " (naissance posthume possible).",
                            "Une naissance posthume est possible jusqu'\u00e0 environ 300 jours apr\u00e8s le d\u00e9c\u00e8s ; \u00e0 confirmer par l'acte.",
                            listOf(p, f)
                        )
                    }
                }
            }
            if (mo != null) {
                val mn = intervalleDe(mo.naissance)
                val md = intervalleDe(mo.deces)
                if (mn != null) {
                    if (certainementAvant(n, mn)) {
                        add(
                            "enfant-avant-mere", 1, listOf(p.id, mo.id),
                            t + " est " + nePar(p.sexe) + " (" + p.naissance.trim() + ") avant sa m\u00e8re " + nomDe(mo) +
                                " (" + mo.naissance.trim() + ").",
                            "Un enfant ne peut pas na\u00eetre avant sa m\u00e8re.", listOf(p, mo)
                        )
                    } else {
                        if (ageMax(mn, n) in 0..14) {
                            add(
                                "mere-trop-jeune", 2, listOf(p.id, mo.id),
                                "La m\u00e8re " + nomDe(mo) + " aurait au plus " + ageMax(mn, n).toString() +
                                    " an(s) \u00e0 la naissance de " + t + " (" + p.naissance.trim() + ").",
                                "Maternit\u00e9 \u00e0 moins de 15 ans : improbable.", listOf(p, mo)
                            )
                        }
                        if (ageMin(mn, n) > 50) {
                            add(
                                "mere-trop-agee", 2, listOf(p.id, mo.id),
                                "La m\u00e8re " + nomDe(mo) + " aurait au moins " + ageMin(mn, n).toString() +
                                    " ans \u00e0 la naissance de " + t + " (" + p.naissance.trim() + ").",
                                "Maternit\u00e9 \u00e0 plus de 50 ans : improbable.", listOf(p, mo)
                            )
                        }
                    }
                }
                if (md != null && certainementAvant(md, n)) {
                    val prefixe = if (md.exact && n.exact) "" else "au moins "
                    add(
                        "naissance-apres-deces-mere", 1, listOf(p.id, mo.id),
                        t + " est " + nePar(p.sexe) + " " + prefixe + dureeTexte(jours(md.fin, n.debut)) + " apr\u00e8s le d\u00e9c\u00e8s de sa m\u00e8re " +
                            nomDe(mo) + " (d\u00e9c\u00e8s " + mo.deces.trim() + ", naissance " + p.naissance.trim() + ").",
                        "Une m\u00e8re ne peut pas mettre un enfant au monde apr\u00e8s son d\u00e9c\u00e8s.", listOf(p, mo)
                    )
                }
            }
            // --- Enfant ne avant le mariage de ses parents.
            if (f != null && mo != null) {
                val marDesParents: Intervalle? = when {
                    f.conjoints.size == 1 && f.conjoints[0] == mo.id -> intervalleDe(f.mariage)
                    mo.conjoints.size == 1 && mo.conjoints[0] == f.id -> intervalleDe(mo.mariage)
                    else -> null
                }
                if (marDesParents != null && certainementAvant(n, marDesParents)) {
                    add(
                        "enfant-avant-mariage", 2, listOf(p.id, f.id, mo.id),
                        t + " est " + nePar(p.sexe) + " " + dureeTexte(jours(n.fin, marDesParents.debut)) +
                            " avant le mariage de ses parents " + nomDe(f) + " et " + nomDe(mo) + ".",
                        "Naissance avant le mariage : l\u00e9gitimation, union ant\u00e9rieure ou date erron\u00e9e \u00e0 v\u00e9rifier.",
                        listOf(p, f, mo)
                    )
                }
            }
        }
    }

    // --- Sexe incompatible avec les relations.
    val enfantsDuPere = noeuds.filter { it.pere != 0L }.groupBy { it.pere }
    val enfantsDeLaMere = noeuds.filter { it.mere != 0L }.groupBy { it.mere }
    for (p in noeuds) {
        val commePere = enfantsDuPere[p.id].orEmpty()
        val commeMere = enfantsDeLaMere[p.id].orEmpty()
        if (commePere.isNotEmpty() && commeMere.isNotEmpty()) {
            add(
                "sexe-incompatible", 1, listOf(p.id),
                nomDe(p) + " figure comme p\u00e8re de " + nomDe(commePere[0]) + " et comme m\u00e8re de " + nomDe(commeMere[0]) + ".",
                "Une m\u00eame personne ne peut pas \u00eatre p\u00e8re d'un enfant et m\u00e8re d'un autre.",
                listOf(p, commePere[0], commeMere[0])
            )
        } else if (commePere.isNotEmpty() && p.sexe == "F") {
            add(
                "sexe-pere", 1, listOf(p.id),
                nomDe(p) + " est indiqu\u00e9e comme p\u00e8re de " + nomDe(commePere[0]) + " mais sa fiche est de sexe f\u00e9minin.",
                "Le sexe de la fiche et le r\u00f4le de parent se contredisent.", listOf(p, commePere[0])
            )
        } else if (commeMere.isNotEmpty() && p.sexe == "M") {
            add(
                "sexe-mere", 1, listOf(p.id),
                nomDe(p) + " est indiqu\u00e9 comme m\u00e8re de " + nomDe(commeMere[0]) + " mais sa fiche est de sexe masculin.",
                "Le sexe de la fiche et le r\u00f4le de parent se contredisent.", listOf(p, commeMere[0])
            )
        }
    }

    // --- Fratrie : naissances trop rapprochees d'une meme mere.
    for ((idMere, enfants) in enfantsDeLaMere) {
        if (enfants.size < 2) continue
        val mere = parId[idMere]
        val datees = enfants.mapNotNull { e ->
            val iv = intervalleDe(e.naissance)
            if (iv != null && iv.exact) Pair(e, iv.debut) else null
        }.sortedBy { it.second }
        for (i in 0 until datees.size - 1) {
            for (j in i + 1 until datees.size) {
                val ecart = jours(datees[i].second, datees[j].second)
                if (ecart > 210) break
                if (ecart >= 2) {
                    val a = datees[i].first
                    val bb = datees[j].first
                    add(
                        "fratrie-intervalle", 1, listOf(a.id, bb.id),
                        nomDe(a) + " et " + nomDe(bb) + " (m\u00eame m\u00e8re" + (if (mere != null) " " + nomDe(mere) else "") +
                            ") sont n\u00e9s \u00e0 " + dureeTexte(ecart) + " d'intervalle.",
                        "Deux naissances non g\u00e9mellaires de la m\u00eame m\u00e8re sont s\u00e9par\u00e9es d'au moins 7 mois.",
                        listOf(a, bb), true
                    )
                }
            }
        }
    }

    return res.sortedWith(compareBy<AnomalieAVerifier>({ it.niveau }, { it.texte }))
}
