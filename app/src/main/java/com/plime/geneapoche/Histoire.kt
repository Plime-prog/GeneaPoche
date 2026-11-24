package com.plime.geneapoche

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------- Histoire (v0.101)
// Petite base historique LOCALE (hors ligne) et fonctions pures pour la
// boite a outils : calendriers historiques, "que s'est-il passe cette
// annee", chronologie d'une famille, chemin de parente. A ENRICHIR.

// Evenement historique : mois / jour a 0 si inconnus ; `depts` vide = national,
// sinon liste des departements concernes (evenement LOCAL).
class EvenementHistorique(
    val annee: Int,
    val mois: Int,
    val jour: Int,
    val titre: String,
    val genre: String, // "regime", "guerre", "loi", "societe"
    val depts: Set<String> = emptySet()
) {
    val date: LocalDate? get() = if (mois in 1..12 && jour in 1..31) {
        try { LocalDate.of(annee, mois, jour) } catch (e: Exception) { null }
    } else null
}

// Periode (regime politique) : bornes incluses.
class PeriodeHistorique(val debut: LocalDate, val fin: LocalDate, val nom: String)

private fun ev(a: Int, m: Int, j: Int, t: String, g: String, vararg d: String) =
    EvenementHistorique(a, m, j, t, g, d.toSet())

val EvenementsHistoriques: List<EvenementHistorique> = listOf(
    ev(1539, 8, 15, "Ordonnance de Villers-Cotter\u00EAts (registres de bapt\u00EAmes)", "loi"),
    ev(1579, 5, 1, "Ordonnance de Blois (registres de mariages et de s\u00E9pultures)", "loi"),
    ev(1685, 10, 18, "R\u00E9vocation de l'\u00E9dit de Nantes", "societe"),
    ev(1715, 9, 1, "Mort de Louis XIV", "regime"),
    ev(1789, 7, 14, "Prise de la Bastille \u2013 R\u00E9volution fran\u00E7aise", "regime"),
    ev(1789, 8, 4, "Abolition des privil\u00E8ges", "loi"),
    ev(1791, 9, 3, "Monarchie constitutionnelle", "regime"),
    ev(1792, 4, 20, "Guerre contre l'Autriche", "guerre"),
    ev(1792, 9, 20, "Victoire de Valmy ; l'\u00E9tat civil devient la\u00EFque", "loi"),
    ev(1792, 9, 22, "Proclamation de la Premi\u00E8re R\u00E9publique", "regime"),
    ev(1793, 1, 21, "Ex\u00E9cution de Louis XVI", "regime"),
    ev(1793, 3, 11, "D\u00E9but de la guerre de Vend\u00E9e", "guerre", "44", "49", "79", "85"),
    ev(1793, 10, 5, "Adoption du calendrier r\u00E9publicain", "loi"),
    ev(1794, 7, 27, "9 Thermidor : chute de Robespierre", "regime"),
    ev(1795, 10, 26, "Directoire", "regime"),
    ev(1799, 11, 9, "18 Brumaire : Consulat", "regime"),
    ev(1804, 3, 21, "Code civil des Fran\u00E7ais", "loi"),
    ev(1804, 5, 18, "Napol\u00E9on empereur : Premier Empire", "regime"),
    ev(1805, 12, 2, "Bataille d'Austerlitz", "guerre"),
    ev(1806, 1, 1, "Retour au calendrier gr\u00E9gorien", "loi"),
    ev(1812, 6, 24, "Campagne de Russie", "guerre"),
    ev(1814, 4, 6, "Abdication de Napol\u00E9on : Restauration", "regime"),
    ev(1815, 3, 20, "Les Cent-Jours", "regime"),
    ev(1815, 6, 18, "Waterloo", "guerre"),
    ev(1830, 7, 27, "Trois Glorieuses : Monarchie de Juillet", "regime"),
    ev(1832, 3, 26, "\u00C9pid\u00E9mie de chol\u00E9ra", "societe", "75", "92", "93", "94"),
    ev(1848, 2, 24, "Seconde R\u00E9publique", "regime"),
    ev(1851, 12, 2, "Coup d'\u00C9tat de Louis-Napol\u00E9on", "regime"),
    ev(1852, 12, 2, "Second Empire", "regime"),
    ev(1870, 7, 19, "Guerre franco-prussienne", "guerre"),
    ev(1870, 9, 4, "Chute de l'Empire : Troisi\u00E8me R\u00E9publique", "regime"),
    ev(1871, 3, 18, "Commune de Paris", "regime", "75"),
    ev(1871, 5, 10, "Trait\u00E9 de Francfort : annexion de l'Alsace-Moselle", "guerre", "67", "68", "57"),
    ev(1882, 3, 28, "Lois Jules Ferry : \u00E9cole gratuite, la\u00EFque, obligatoire", "loi"),
    ev(1905, 12, 9, "S\u00E9paration des \u00C9glises et de l'\u00C9tat", "loi"),
    ev(1914, 8, 2, "Mobilisation g\u00E9n\u00E9rale", "guerre"),
    ev(1914, 8, 3, "Premi\u00E8re Guerre mondiale", "guerre"),
    ev(1916, 2, 21, "Bataille de Verdun", "guerre", "55"),
    ev(1918, 11, 11, "Armistice de 1918", "guerre"),
    ev(1936, 5, 3, "Front populaire", "societe"),
    ev(1939, 9, 3, "Seconde Guerre mondiale (d\u00E9claration de guerre)", "guerre"),
    ev(1940, 6, 22, "Armistice de 1940", "guerre"),
    ev(1940, 7, 10, "\u00C9tat fran\u00E7ais (Vichy)", "regime"),
    ev(1944, 6, 6, "D\u00E9barquement en Normandie", "guerre", "14", "50", "61"),
    ev(1944, 8, 25, "Lib\u00E9ration de Paris", "guerre", "75"),
    ev(1945, 5, 8, "Victoire du 8 mai 1945", "guerre"),
    ev(1946, 10, 27, "Quatri\u00E8me R\u00E9publique", "regime"),
    ev(1958, 10, 4, "Cinqui\u00E8me R\u00E9publique", "regime"),
    ev(1962, 3, 19, "Cessez-le-feu en Alg\u00E9rie", "guerre")
).sortedWith(compareBy({ it.annee }, { it.mois }, { it.jour }))

private fun d(a: Int, m: Int, j: Int): LocalDate = LocalDate.of(a, m, j)

val PeriodesHistoriques: List<PeriodeHistorique> = listOf(
    PeriodeHistorique(d(1715, 9, 1), d(1789, 7, 13), "Ancien R\u00E9gime (Louis XV et Louis XVI)"),
    PeriodeHistorique(d(1789, 7, 14), d(1792, 9, 21), "R\u00E9volution fran\u00E7aise (monarchie constitutionnelle)"),
    PeriodeHistorique(d(1792, 9, 22), d(1795, 10, 25), "Premi\u00E8re R\u00E9publique \u2013 Convention"),
    PeriodeHistorique(d(1795, 10, 26), d(1799, 11, 8), "Premi\u00E8re R\u00E9publique \u2013 Directoire"),
    PeriodeHistorique(d(1799, 11, 9), d(1804, 5, 17), "Consulat"),
    PeriodeHistorique(d(1804, 5, 18), d(1814, 4, 5), "Premier Empire"),
    PeriodeHistorique(d(1814, 4, 6), d(1830, 7, 26), "Restauration"),
    PeriodeHistorique(d(1830, 7, 27), d(1848, 2, 23), "Monarchie de Juillet"),
    PeriodeHistorique(d(1848, 2, 24), d(1852, 12, 1), "Seconde R\u00E9publique"),
    PeriodeHistorique(d(1852, 12, 2), d(1870, 9, 3), "Second Empire"),
    PeriodeHistorique(d(1870, 9, 4), d(1940, 7, 9), "Troisi\u00E8me R\u00E9publique"),
    PeriodeHistorique(d(1940, 7, 10), d(1944, 8, 24), "\u00C9tat fran\u00E7ais (Vichy)"),
    PeriodeHistorique(d(1944, 8, 25), d(1946, 10, 26), "Gouvernement provisoire"),
    PeriodeHistorique(d(1946, 10, 27), d(1958, 10, 3), "Quatri\u00E8me R\u00E9publique"),
    PeriodeHistorique(d(1958, 10, 4), d(2100, 12, 31), "Cinqui\u00E8me R\u00E9publique")
)

fun periodeDe(date: LocalDate): PeriodeHistorique? =
    PeriodesHistoriques.firstOrNull { !date.isBefore(it.debut) && !date.isAfter(it.fin) }

// Evenements d'une annee (nationaux + locaux si `dept` correspond), tries.
fun evenementsDeLAnnee(annee: Int, dept: String = ""): List<EvenementHistorique> =
    EvenementsHistoriques.filter { it.annee == annee && (it.depts.isEmpty() || (dept.isNotEmpty() && dept in it.depts)) }

// Changements de regime d'une annee (debut de periode).
fun regimesDeLAnnee(annee: Int): List<PeriodeHistorique> =
    PeriodesHistoriques.filter { it.debut.year == annee }

// "12 jours apres" / "3 jours avant" / "le jour de".
fun libelleEcart(jours: Long): String = when {
    jours == 0L -> "le jour de"
    jours > 0 -> (if (jours == 1L) "1 jour" else jours.toString() + " jours") + " apr\u00E8s"
    else -> (if (jours == -1L) "1 jour" else (-jours).toString() + " jours") + " avant"
}

// Rapprochements d'une date avec l'histoire : evenement NATIONAL ou local
// le plus proche dans la fenetre (+/- `fenetre` jours) puis periode.
// Renvoie par exemple "12 jours apr\u00E8s Proclamation de la Premi\u00E8re R\u00E9publique".
fun rapprochementHistorique(date: LocalDate, dept: String = "", fenetre: Long = 60L): String? {
    var meilleur: EvenementHistorique? = null
    var ecart = Long.MAX_VALUE
    EvenementsHistoriques.forEach { e ->
        val ed = e.date ?: return@forEach
        if (e.depts.isNotEmpty() && (dept.isEmpty() || dept !in e.depts)) return@forEach
        val j = ChronoUnit.DAYS.between(ed, date)
        if (Math.abs(j) <= fenetre && Math.abs(j) < Math.abs(ecart)) {
            meilleur = e
            ecart = j
        }
    }
    val m = meilleur ?: return null
    return libelleEcart(ecart) + " : " + m.titre
}

// Phrases d'une personne : "Paul est n\u00E9 12 jours apr\u00E8s ...", "n\u00E9 sous la Monarchie de Juillet".
fun phrasesHistoriquesPersonne(p: Personne): List<String> {
    val res = mutableListOf<String>()
    val dept = deptDePersonne(p)
    val prenom = p.prenoms.trim().split(" ").firstOrNull().orEmpty().ifEmpty { p.nom.trim() }
    fun un(date: String, lieuDept: String, verbe: String) {
        val ld = lireDate(date) ?: return
        val r = rapprochementHistorique(ld, lieuDept.ifEmpty { dept })
        if (r != null) {
            res.add(prenom + " " + verbe + " " + r.replaceFirst(" : ", " \u00AB ") + " \u00BB.")
        } else {
            val per = periodeDe(ld)
            if (per != null) res.add(prenom + " " + verbe + " sous : " + per.nom + ".")
        }
    }
    un(p.dateNaissance, p.deptNaissance, if (p.sexe == "F") "est n\u00E9e" else "est n\u00E9")
    un(p.dateMariage, p.deptMariage, if (p.sexe == "F") "s'est mari\u00E9e" else "s'est mari\u00E9")
    un(p.dateDeces, p.deptDeces, if (p.sexe == "F") "est d\u00E9c\u00E9d\u00E9e" else "est d\u00E9c\u00E9d\u00E9")
    return res
}

// ---------------------------------------------------------------- Que s'est-il passe cette annee ?

class LigneAnnee(val genre: String, val texte: String, val personneId: Long)

// Vivants (avec age), naissances, mariages, deces de l'annee.
fun ligneAnnee(personnes: List<Personne>, annee: Int): List<LigneAnnee> {
    val res = mutableListOf<LigneAnnee>()
    personnes.forEach { p ->
        val n = anneeDe(p.dateNaissance.ifBlank { p.naissanceCalculee })
        val dc = anneeDe(p.dateDeces)
        val m = anneeDe(p.dateMariage)
        if (n == annee) res.add(LigneAnnee("N", "Naissance de " + p.titre(), p.id))
        if (m == annee) res.add(LigneAnnee("M", "Mariage de " + p.titre(), p.id))
        if (dc == annee) res.add(LigneAnnee("D", "D\u00E9c\u00E8s de " + p.titre(), p.id))
        p.evenementsLibres.forEach { e ->
            if (anneeDe(e.date) == annee) {
                res.add(LigneAnnee("E", e.intitule.trim().ifEmpty { "\u00C9v\u00E9nement" } + " \u2013 " + p.titre(), p.id))
            }
        }
        // Age des ancetres vivants cette annee-la (naissance connue, pas de deces avant).
        if (n != null && n < annee && (dc == null || dc > annee) && annee - n <= 110) {
            res.add(LigneAnnee("A", p.titre() + " : " + (annee - n).toString() + " ans" + (if (p.dateNaissance.isBlank()) " (~)" else ""), p.id))
        }
    }
    return res
}

// ---------------------------------------------------------------- Chronologie d'une famille

// Une ligne : annee (tri), mois/jour (0 si inconnus), libelle, alerte eventuelle.
class EvtFamille(
    val annee: Int,
    val mois: Int,
    val jour: Int,
    val libelle: String,
    val alerte: String = ""
) {
    val cle: Long get() = annee * 10000L + mois * 100L + jour
}

private fun parties(s: String): Triple<Int, Int, Int>? {
    val an = anneeDe(s) ?: return null
    val ld = lireDate(s)
    return if (ld != null) Triple(ld.year, ld.monthValue, ld.dayOfMonth) else Triple(an, 0, 0)
}

// Evenements d'une personne : naissance, mariages (avec le conjoint),
// naissances et deces des enfants, deces du conjoint, evenements libres,
// deces. Signale les incoherences evidentes (apres son deces, avant sa naissance).
fun chronologieFamille(p: Personne, personnes: List<Personne>, masquerHorsVie: Boolean = false): List<EvtFamille> {
    val res = mutableListOf<EvtFamille>()
    val parId = personnes.associateBy { it.id }
    val nais = parties(p.dateNaissance.ifBlank { p.naissanceCalculee })
    val dece = parties(p.dateDeces)
    // ctl : 0 = aucun controle ; 1 = evenement de la personne (ni avant sa
    // naissance ni apres son deces) ; 2 = naissance d'un enfant (pas apres le
    // deces de la mere, ni plus d'un an apres celui du pere).
    fun ajoute(s: String, libelle: String, ctl: Int = 0, famille: Boolean = false) {
        val t = parties(s) ?: return
        // v0.103 : aucun evenement familial avant la naissance ni apres le deces.
        if (masquerHorsVie && famille && ((nais != null && t.first < nais.first) || (dece != null && t.first > dece.first))) return
        var alerte = ""
        if (ctl == 1 && nais != null && t.first < nais.first) alerte = "avant sa naissance"
        if (ctl == 1 && dece != null && t.first > dece.first) alerte = "apr\u00E8s son d\u00E9c\u00E8s"
        if (ctl == 2 && dece != null && t.first > dece.first + (if (p.sexe == "F") 0 else 1)) {
            alerte = "enfant n\u00E9 apr\u00E8s le d\u00E9c\u00E8s du parent"
        }
        res.add(EvtFamille(t.first, t.second, t.third, libelle, alerte))
    }
    if (p.dateNaissance.isNotBlank()) ajoute(p.dateNaissance, "Naissance")
    else if (p.naissanceCalculee.isNotBlank()) ajoute(p.naissanceCalculee, "Naissance (vers, calcul\u00E9e)")
    ajoute(p.dateBapteme, "Bapt\u00EAme", 1)
    val conjoints = p.tousConjoints().mapNotNull { parId[it] }
    if (p.dateMariage.isNotBlank()) {
        val c0 = conjoints.firstOrNull()
        val rang = if (conjoints.size > 1) " (1er)" else ""
        ajoute(p.dateMariage, "Mariage" + (if (c0 != null) " avec " + c0.titre() else "") + rang, 1)
    }
    conjoints.forEachIndexed { i, c ->
        if (i > 0 && c.dateMariage.isNotBlank()) ajoute(c.dateMariage, "Remariage avec " + c.titre(), 0, true)
        if (c.dateDeces.isNotBlank()) ajoute(c.dateDeces, "D\u00E9c\u00E8s de " + (if (c.sexe == "F") "l'\u00E9pouse " else "l'\u00E9poux ") + c.titre(), 0, true)
    }
    p.enfants.mapNotNull { parId[it] }.forEach { e ->
        val qui = if (e.sexe == "F") "de la fille " else if (e.sexe == "M") "du fils " else "de l'enfant "
        ajoute(e.dateNaissance.ifBlank { e.naissanceCalculee }, "Naissance " + qui + e.titre(), 2, true)
        ajoute(e.dateDeces, "D\u00E9c\u00E8s " + qui + e.titre(), 0, true)
    }
    p.evenementsLibres.forEach { e -> ajoute(e.date, e.intitule.trim().ifEmpty { "\u00C9v\u00E9nement" }, 1) }
    ajoute(p.dateSepulture, "S\u00E9pulture")
    if (p.dateDeces.isNotBlank()) ajoute(p.dateDeces, "D\u00E9c\u00E8s")
    return res.sortedWith(compareBy({ it.cle }, { it.libelle }))
}

// ---------------------------------------------------------------- Chemin de parente

// Une etape du chemin : la personne et le lien avec la PRECEDENTE
// ("" pour la premiere) : "parent" (la precedente est enfant de celle-ci),
// "enfant", "conjoint".
class EtapeChemin(val personneId: Long, val lien: String)

// Plus court chemin entre x et y par les liens de sang (parents/enfants) ;
// a defaut, en autorisant les conjoints. Liste vide : aucun chemin.
fun cheminParente(x: Long, y: Long, personnes: List<Personne>): List<EtapeChemin> {
    val parId = personnes.associateBy { it.id }
    if (parId[x] == null || parId[y] == null) return emptyList()
    if (x == y) return listOf(EtapeChemin(x, ""))
    // Enfants de chaque personne (liste de la fiche + liens pere/mere des enfants).
    val enfantsDe = HashMap<Long, MutableSet<Long>>()
    personnes.forEach { q ->
        q.enfants.forEach { e -> enfantsDe.getOrPut(q.id) { LinkedHashSet() }.add(e) }
        if (q.pere != 0L) enfantsDe.getOrPut(q.pere) { LinkedHashSet() }.add(q.id)
        if (q.mere != 0L) enfantsDe.getOrPut(q.mere) { LinkedHashSet() }.add(q.id)
    }
    fun voisins(id: Long, conjoints: Boolean): List<Pair<Long, String>> {
        val p = parId[id] ?: return emptyList()
        val res = mutableListOf<Pair<Long, String>>()
        if (p.pere != 0L) res.add(Pair(p.pere, "parent"))
        if (p.mere != 0L) res.add(Pair(p.mere, "parent"))
        (enfantsDe[id] ?: emptySet<Long>()).forEach { res.add(Pair(it, "enfant")) }
        if (conjoints) p.tousConjoints().forEach { res.add(Pair(it, "conjoint")) }
        return res.filter { parId.containsKey(it.first) }
    }
    for (avecConjoints in listOf(false, true)) {
        val precedent = HashMap<Long, Pair<Long, String>>()
        val vus = HashSet<Long>()
        val file = ArrayDeque<Long>()
        file.addLast(x)
        vus.add(x)
        while (file.isNotEmpty()) {
            val cur = file.removeFirst()
            if (cur == y) break
            voisins(cur, avecConjoints).forEach { (v, lien) ->
                if (vus.add(v)) {
                    precedent[v] = Pair(cur, lien)
                    file.addLast(v)
                }
            }
        }
        if (y in vus) {
            val chemin = ArrayList<EtapeChemin>()
            var cur = y
            while (cur != x) {
                val (pr, lien) = precedent[cur] ?: break
                chemin.add(EtapeChemin(cur, lien))
                cur = pr
            }
            chemin.add(EtapeChemin(x, ""))
            chemin.reverse()
            return chemin
        }
    }
    return emptyList()
}
