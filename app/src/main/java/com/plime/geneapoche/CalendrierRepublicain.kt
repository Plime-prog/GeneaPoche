package com.plime.geneapoche

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ================================================================ v0.86
// Boite a outils - outil 1 : calendrier republicain <-> gregorien <-> julien.
// Logique PURE (aucune dependance Android) : testee en JVM.
//
// Reference des noms de jours : Fondation Napoleon, "Noms des jours du
// calendrier republicain" (orthographe d'epoque, fautes de frappe evidentes
// corrigees). Reference des debuts d'annee : equinoxe d'automne observe a
// Paris (tableau DebutAnRepubli de Modeles.kt, an I = 22/09/1792).

val MoisRepubliAffiches = listOf(
    "Vend\u00e9miaire", "Brumaire", "Frimaire", "Niv\u00f4se", "Pluvi\u00f4se", "Vent\u00f4se",
    "Germinal", "Flor\u00e9al", "Prairial", "Messidor", "Thermidor", "Fructidor"
)

val JoursDecadeRepubli = listOf(
    "Primidi", "Duodi", "Tridi", "Quartidi", "Quintidi",
    "Sextidi", "Septidi", "Octidi", "Nonidi", "D\u00e9cadi"
)

val FetesComplementairesRepubli = listOf(
    "F\u00eate de la Vertu", "F\u00eate du G\u00e9nie", "F\u00eate du Travail",
    "F\u00eate de l'Opinion", "F\u00eate des R\u00e9compenses", "F\u00eate de la R\u00e9volution"
)

private val FetesDesMois: List<List<String>> = listOf(
    "Raisin,Safran,Ch\u00e2taigne,Colchique,Cheval,Balsamine,Carotte,Amaranthe,Panais,Cuve,Pomme de terre,Immortelle,Potiron,R\u00e9s\u00e9da,\u00c2ne,Belle de nuit,Citrouille,Sarrasin,Tournesol,Pressoir,Chanvre,P\u00eache,Navet,Amarillis,B\u0153uf,Aubergine,Piment,Tomate,Orge,Tonneau",
    "Pomme,C\u00e9leri,Poire,Betterave,Oie,H\u00e9liotrope,Figue,Scorson\u00e8re,Alisier,Charrue,Salsifis,Macre,Topinambour,Endive,Dindon,Chervis,Cresson,Dentelaire,Grenade,Herse,Bacchante,Azerole,Garance,Orange,Faisan,Pistache,Macjonc,Coing,Cormier,Rouleau",
    "Raiponce,Turneps,Chicor\u00e9e,N\u00e8fle,Cochon,M\u00e2che,Chou-fleur,Miel,Geni\u00e8vre,Pioche,Cire,Raifort,C\u00e8dre,Sapin,Chevreuil,Ajonc,Cypr\u00e8s,Lierre,Sabine,Hoyau,\u00c9rable sucr\u00e9,Bruy\u00e8re,Roseau,Oseille,Grillon,Pignon,Li\u00e8ge,Truffe,Olive,Pelle",
    "Tourbe,Houille,Bitume,Soufre,Chien,Lave,Terre v\u00e9g\u00e9tale,Fumier,Salp\u00eatre,Fl\u00e9au,Granit,Argile,Ardoise,Gr\u00e8s,Lapin,Silex,Marne,Pierre \u00e0 chaux,Marbre,Van,Pierre \u00e0 pl\u00e2tre,Sel,Fer,Cuivre,Chat,\u00c9tain,Plomb,Zinc,Mercure,Crible",
    "Laur\u00e9ole,Mousse,Fragon,Perce-neige,Taureau,Laurier-thym,Amadouvier,M\u00e9z\u00e9r\u00e9on,Peuplier,Coign\u00e9e,Ell\u00e9bore,Brocoli,Laurier,Avelinier,Vache,Buis,Lichen,If,Pulmonaire,Serpette,Thlaspi,Thim\u00e8le,Chiendent,Trainasse,Li\u00e8vre,Gu\u00e8de,Noisetier,Cyclamen,Ch\u00e9lidoine,Tra\u00eeneau",
    "Tussilage,Cornouiller,Violier,Tro\u00e8ne,Bouc,Asaret,Alaterne,Violette,Marceau,B\u00eache,Narcisse,Orme,Fumeterre,V\u00e9lar,Ch\u00e8vre,\u00c9pinard,Doronic,Mouron,Cerfeuil,Cordeau,Mandragore,Persil,Cochl\u00e9aria,P\u00e2querette,Thon,Pissenlit,Sylve,Capillaire,Fr\u00eane,Plantoir",
    "Primev\u00e8re,Platane,Asperge,Tulipe,Poule,Bette,Bouleau,Jonquille,Aulne,Couvoir,Pervenche,Charme,Morille,H\u00eatre,Abeille,Laitue,M\u00e9l\u00e8ze,Cigu\u00eb,Radis,Ruche,Gainier,Romaine,Marronnier,Roquette,Pigeon,Lilas,An\u00e9mone,Pens\u00e9e,Myrtille,Greffoir",
    "Rose,Ch\u00eane,Foug\u00e8re,Aub\u00e9pine,Rossignol,Ancolie,Muguet,Champignon,Hyacinthe,R\u00e2teau,Rhubarbe,Sainfoin,B\u00e2ton d'or,Chamerops,Ver \u00e0 soie,Consoude,Pimprenelle,Corbeille d'or,Arroche,Sarcloir,Statice,Fritillaire,Bourrache,Val\u00e9riane,Carpe,Fusain,Civette,Buglosse,S\u00e9nev\u00e9,Houlette",
    "Luzerne,H\u00e9m\u00e9rocalle,Tr\u00e8fle,Ang\u00e9lique,Canard,M\u00e9lisse,Fromental,Martagon,Serpolet,Faux,Fraise,B\u00e9toine,Pois,Acacia,Caille,\u0152illet,Sureau,Pavot,Tilleul,Fourche,Barbeau,Camomille,Ch\u00e8vrefeuille,Caille-lait,Tanche,Jasmin,Verveine,Thym,Pivoine,Chariot",
    "Seigle,Avoine,Oignon,V\u00e9ronique,Mulet,Romarin,Concombre,\u00c9chalotte,Absinthe,Faucille,Coriandre,Artichaut,Girofle,Lavande,Chamois,Tabac,Groseille,Gesse,Cerise,Parc,Menthe,Cumin,Haricot,Orcan\u00e8te,Pintade,Sauge,Ail,Vesce,Bl\u00e9,Chal\u00e9mie",
    "\u00c9peautre,Bouillon-blanc,Melon,Ivraie,B\u00e9lier,Pr\u00eale,Armoise,Carthame,M\u00fbre,Arrosoir,Panis,Salicorne,Abricot,Basilic,Brebis,Guimauve,Lin,Amande,Gentiane,\u00c9cluse,Carline,C\u00e2prier,Lentille,Aun\u00e9e,Loutre,Myrte,Colza,Lupin,Coton,Moulin",
    "Prune,Millet,Lycoperdon,Escourgeon,Saumon,Tub\u00e9reuse,Sucrion,Apocyn,R\u00e9glisse,\u00c9chelle,Past\u00e8que,Fenouil,\u00c9pine-vinette,Noix,Truite,Citron,Card\u00e8re,Nerprun,Tagette,Hotte,\u00c9glantier,Noisette,Houblon,Sorgho,\u00c9crevisse,Bigarade,Verge d'or,Ma\u00efs,Marron,Panier"
).map { it.split(",") }

val JoursSemaineFr = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")

val MoisGregorienFr = listOf(
    "janvier", "f\u00e9vrier", "mars", "avril", "mai", "juin",
    "juillet", "ao\u00fbt", "septembre", "octobre", "novembre", "d\u00e9cembre"
)

// Mois 1..12 ; mois 13 = jours complementaires (sansculottides).
data class DateRepublicaine(val an: Int, val mois: Int, val jour: Int)

// Nom du jour (plante, animal, outil...) : "Raisin" pour le 1er Vendemiaire.
fun feteRepubli(mois: Int, jour: Int): String {
    if (mois == 13) return FetesComplementairesRepubli.getOrNull(jour - 1) ?: ""
    return FetesDesMois.getOrNull(mois - 1)?.getOrNull(jour - 1) ?: ""
}

// Ans sextiles historiques (6 jours complementaires) : III, VII et XI.
fun anSextile(an: Int): Boolean = an == 3 || an == 7 || an == 11

fun anRomain(an: Int): String = RomainsRepubli.getOrNull(an - 1) ?: an.toString()

fun debutDeLAn(an: Int): LocalDate? {
    val d = DebutAnRepubli.getOrNull(an - 1) ?: return null
    return LocalDate.of(d.third, d.second, d.first)
}

// Dernier jour de l'an ; l'an XIV s'arrete au 31/12/1805 (fin historique).
fun finDeLAn(an: Int): LocalDate? {
    if (an == 14) return LocalDate.of(1805, 12, 31)
    val suivant = debutDeLAn(an + 1) ?: return null
    return suivant.minusDays(1)
}

fun dureeDeLAn(an: Int): Int? {
    val d = debutDeLAn(an) ?: return null
    val f = finDeLAn(an) ?: return null
    return (ChronoUnit.DAYS.between(d, f) + 1).toInt()
}

fun republicainVersGregorien(r: DateRepublicaine): LocalDate? {
    if (r.an !in 1..14 || r.mois !in 1..13 || r.jour !in 1..30) return null
    if (r.mois == 13 && r.jour > 6) return null
    val debut = debutDeLAn(r.an) ?: return null
    val fin = finDeLAn(r.an) ?: return null
    val res = debut.plusDays(((r.mois - 1) * 30 + r.jour - 1).toLong())
    if (res.isAfter(fin)) return null
    return res
}

fun gregorienVersRepublicain(d: LocalDate): DateRepublicaine? {
    val premier = debutDeLAn(1) ?: return null
    val dernier = finDeLAn(14) ?: return null
    if (d.isBefore(premier) || d.isAfter(dernier)) return null
    var an = 14
    while (an > 1 && d.isBefore(debutDeLAn(an)!!)) an--
    val decalage = ChronoUnit.DAYS.between(debutDeLAn(an)!!, d).toInt()
    return if (decalage >= 360) DateRepublicaine(an, 13, decalage - 360 + 1)
    else DateRepublicaine(an, decalage / 30 + 1, decalage % 30 + 1)
}

private fun rang(n: Int): String = if (n == 1) "1er" else n.toString()

// "14 Vend\u00e9miaire an VII" ; "3e jour compl\u00e9mentaire an III".
fun libelleRepubli(r: DateRepublicaine): String =
    if (r.mois == 13) {
        (if (r.jour == 1) "1er" else r.jour.toString() + "e") + " jour compl\u00e9mentaire an " + anRomain(r.an)
    } else {
        rang(r.jour) + " " + MoisRepubliAffiches[r.mois - 1] + " an " + anRomain(r.an)
    }

// "5 octobre 1798" ; "1er mai 1843".
fun dateLongue(d: LocalDate): String =
    rang(d.dayOfMonth) + " " + MoisGregorienFr[d.monthValue - 1] + " " + d.year.toString()

fun dateCourte(d: LocalDate): String =
    d.dayOfMonth.toString().padStart(2, '0') + "/" + d.monthValue.toString().padStart(2, '0') + "/" +
        d.year.toString().padStart(4, '0')

fun jourSemaineDe(d: LocalDate): String = JoursSemaineFr[d.dayOfWeek.value - 1]

// Calendrier julien : jour, mois, annee correspondant a une date gr\u00e9gorienne
// (algorithme du jour julien ; 5 octobre 1798 gregorien = 24 septembre 1798).
fun dateJulienne(d: LocalDate): Triple<Int, Int, Int> {
    val jdn = d.toEpochDay() + 2440588L
    val c = jdn + 32082L
    val dd = (4L * c + 3L) / 1461L
    val e = c - (1461L * dd) / 4L
    val m = (5L * e + 2L) / 153L
    val jour = e - (153L * m + 2L) / 5L + 1L
    val mois = m + 3L - 12L * (m / 10L)
    val an = dd - 4800L + m / 10L
    return Triple(jour.toInt(), mois.toInt(), an.toInt())
}

fun julienBissextile(an: Int): Boolean = an % 4 == 0

data class InfoJour(
    val date: LocalDate,
    val jourSemaine: String,
    val gregorien: String,
    val repub: DateRepublicaine?,
    val republicain: String,
    val jourDeDecade: String,
    val decade: Int,
    val fete: String,
    val julien: String,
    val anSextileRepub: Boolean,
    val gregorienBissextile: Boolean,
    val julienBissextile: Boolean
)

fun infoJour(d: LocalDate): InfoJour {
    val r = gregorienVersRepublicain(d)
    val j = dateJulienne(d)
    val julien = rang(j.first) + " " + MoisGregorienFr[j.second - 1] + " " + j.third.toString()
    return InfoJour(
        date = d,
        jourSemaine = jourSemaineDe(d),
        gregorien = dateLongue(d),
        repub = r,
        republicain = if (r != null) libelleRepubli(r) else "",
        jourDeDecade = if (r != null && r.mois != 13) JoursDecadeRepubli[(r.jour - 1) % 10] else "",
        decade = if (r != null && r.mois != 13) (r.jour - 1) / 10 + 1 else 0,
        fete = if (r != null) feteRepubli(r.mois, r.jour) else "",
        julien = julien,
        anSextileRepub = r != null && anSextile(r.an),
        gregorienBissextile = d.isLeapYear,
        julienBissextile = julienBissextile(d.year)
    )
}

// Lignes affichables (et copiables dans une note) pour un jour donne.
fun lignesInfoJour(i: InfoJour): List<String> {
    val l = mutableListOf<String>()
    l.add("Gr\u00e9gorien : " + i.jourSemaine + " " + i.gregorien)
    val r = i.repub
    if (r != null) {
        l.add("R\u00e9publicain : " + i.republicain)
        if (r.mois == 13) {
            l.add("Jour compl\u00e9mentaire : " + i.fete)
        } else {
            l.add(i.jourDeDecade + ", " + (if (i.decade == 1) "1re" else i.decade.toString() + "e") +
                " d\u00e9cade \u00b7 f\u00eate du jour : " + i.fete)
        }
    } else {
        l.add("Hors du calendrier r\u00e9publicain (22/09/1792 au 31/12/1805)")
    }
    l.add("Julien : " + i.julien)
    val sb = StringBuilder()
    if (r != null) {
        val duree = dureeDeLAn(r.an)
        sb.append("An " + anRomain(r.an) + " : " +
            (if (i.anSextileRepub) "sextile" else "commun") +
            (if (duree != null && r.an < 14) " (" + duree.toString() + " jours)" else "") + " \u00b7 ")
    }
    sb.append("ann\u00e9e " + i.date.year.toString() + " : " +
        (if (i.gregorienBissextile) "bissextile" else "non bissextile") + " (gr\u00e9gorien)")
    if (i.gregorienBissextile != i.julienBissextile) {
        sb.append(", " + (if (i.julienBissextile) "bissextile" else "non bissextile") + " (julien)")
    }
    l.add(sb.toString())
    return l
}
