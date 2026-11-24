package com.plime.geneapoche

// v0.99 : premiere base d'extraction de la logique d'enregistrement d'un
// acte (enregistrerActe dans MainActivity.kt, ~1 100 lignes) vers des
// fonctions PURES, testables sur JVM (TestsLogiqueActe.kt). enregistrerActe
// delegue desormais a ces fonctions ; le reste du chantier (decoupage complet
// par etapes) continuera en v0.100+.

// ---------------------------------------------------------- Cles de comparaison

// Cle de comparaison de noms : majuscules, accents retires, tirets et
// apostrophes ramenes a des espaces, espaces compresses.
fun cleNom(s: String): String =
    java.text.Normalizer.normalize(s.trim().uppercase(), java.text.Normalizer.Form.NFD)
        // v0.123 : les accents (marques combinantes apres NFD) sont SUPPRIMES ;
        // avant, ils devenaient une espace ("Lefèvre" -> "LEFE VRE") et ne
        // correspondaient plus a "Lefevre".
        .replace(Regex("[\\p{Mn}\\p{Cf}]"), "")
        .replace("'", " ")
        .replace("-", " ")
        .split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

// Prenoms compares SANS ORDRE ("Marie Jeanne" = "Jeanne Marie").
fun clePrenomsNom(s: String): String =
    cleNom(s).split(" ").filter { it.isNotEmpty() }.sorted().joinToString(" ")

// ---------------------------------------------------------- Anti-doublons (v0.77)

// Un VRAI doublon = meme nom, memes prenoms (sans ordre) ET meme date de
// naissance. Une date differente (ou absente d'un cote) = homonyme possible :
// aucun match (l'appelant proposera de creer une fiche).
fun matchPersonneActe(
    personnes: List<Personne>,
    nomSaisi: String,
    prenomsSaisis: String,
    dateNaissance: String
): Personne? {
    val n = nomSaisi.trim()
    val pr = prenomsSaisis.trim()
    if (n.isEmpty() && pr.isEmpty()) return null
    return personnes.firstOrNull {
        cleNom(it.nom) == cleNom(n) &&
            clePrenomsNom(it.prenoms) == clePrenomsNom(pr) &&
            (it.dateNaissance.isBlank() || dateNaissance.isBlank() || dateGreg(dateNaissance) == it.dateNaissance)
    }
}

// ---------------------------------------------------------- Mise a jour des champs (v0.98)

// Nouvel acte : le formulaire ne fait que COMPLETER la fiche (champ deja
// rempli = conserve). Edition : le formulaire pre-rempli est la REFERENCE
// (champ vide = efface). greg = la valeur saisie est une date a convertir
// en gregorien (calendrier republicain accepte).
fun majChamp98(actuel: String, saisi: String, edition: Boolean, greg: Boolean = false): String {
    if (!edition && actuel.isNotBlank()) return actuel
    return if (greg) dateGreg(saisi) else saisi.trim()
}

// ---------------------------------------------------------- Renseignement d'une fiche parent / temoin

// Renseigne les champs d'identite/naissance/deces d'une fiche depuis la
// saisie d'un acte. Toute valeur saisie non vide REMPLACE celle de la fiche
// (v0.60 : synchronisation totale) ; en EDITION, une valeur vide EFFACE le
// champ (v0.65 : le formulaire est la reference complete). Si la fiche n'a
// pas de date de naissance et que l'acte donne un age, une date "vers aaaa"
// CALCULEE est posee (v0.97), jamais ecrasee si deja presente.
fun renseignerChampsActe(
    c: Personne?,
    ne: String,
    lieu: String,
    dept: String,
    prof: String,
    dec: String,
    lieuDec: String,
    dateActe: String,
    edition: Boolean,
    age: String = "",
    situation: String = "",
    domicile: String = "",
    sourceCalcul: String = ""
): String? {
    if (c == null) return null
    if (edition) {
        c.dateNaissance = dateGreg(ne)
        c.lieuNaissance = lieu.trim()
        c.deptNaissance = dept.trim()
        c.profession = prof.trim()
        c.dateDeces = dateGreg(dec)
        c.lieuDeces = lieuDec.trim()
    } else {
        if (ne.isNotBlank()) c.dateNaissance = dateGreg(ne)
        if (lieu.isNotBlank()) c.lieuNaissance = lieu.trim()
        if (dept.isNotBlank()) c.deptNaissance = dept.trim()
        if (prof.isNotBlank()) c.profession = prof.trim()
        if (dec.isNotBlank()) c.dateDeces = dateGreg(dec)
        if (lieuDec.isNotBlank()) c.lieuDeces = lieuDec.trim()
    }
    if (situation.isNotBlank()) c.situation = situation.trim()
    if (domicile.isNotBlank()) c.domicile = domicile.trim()
    // v0.99 : date de naissance approximative automatique (jamais si la
    // date exacte existe), avec controle de coherence centralise.
    return naissanceDepuisAge(c, dateActe, age, sourceCalcul)
}

// ---------------------------------------------------------- Naissance approximative (v0.99)

// PARTOUT ou un age est saisi (personne concernee, parents, temoins,
// mariee...), la fiche recoit une date de naissance approximative
// "vers aaaa" SI elle n'a ni date exacte ni date calculee. Si la fiche a
// deja une date EXACTE, le calcul n'ecrit rien mais VERIFIE la coherence :
// un ecart de plus de 2 ans retourne un message d'incoherence (affiche au
// moment de la saisie et rappele a l'enregistrement).
fun naissanceDepuisAge(c: Personne?, dateActe: String, age: String, source: String, ficheSource: Long = 0L): String? {
    if (c == null) return null
    val t = age.trim()
    if (t.isBlank()) return null
    val anActe = anneeDe(dateActe) ?: return null
    val ansAge = Regex("[0-9]{1,3}").find(t)?.value?.toIntOrNull() ?: return null
    if (ansAge !in 1..120) return null
    val anCalc = anActe - ansAge
    val exacte = anneeDe(c.dateNaissance)
    if (exacte != null) {
        // Une date exacte existe : une ancienne date calculee n'a plus lieu d'etre.
        c.naissanceCalculee = ""
        c.naissanceCalculeeSource = ""
        c.naissanceCalculeeFiche = 0L
        return if (kotlin.math.abs(exacte - anCalc) > 2)
            "Incoh\u00e9rence : " + ansAge + " ans en " + anActe +
                " donne une naissance vers " + anCalc + ", la fiche de " + c.titre() +
                " dit " + c.dateNaissance + "."
        else null
    }
    val dejaCalc = Regex("[0-9]{4}").find(c.naissanceCalculee)?.value?.toIntOrNull()
    if (dejaCalc == null) {
        c.naissanceCalculee = "vers " + anCalc.toString()
        // v0.151 : la date de l'acte et la fiche source alimentent le bloc Observations.
        c.naissanceCalculeeSource = "calcul\u00e9e d'apr\u00e8s " + source + (if (dateActe.isNotBlank()) " du " + dateActe.trim() else "")
        c.naissanceCalculeeFiche = ficheSource
    } else if (kotlin.math.abs(dejaCalc - anCalc) > 2) {
        // Deuxieme age qui contredit la date deja calculee : on ne l'ecrase pas.
        return "Incoh\u00e9rence : " + ansAge + " ans en " + anActe + " donne une naissance vers " +
            anCalc + ", mais " + c.titre() + " est d\u00e9j\u00e0 calcul\u00e9(e) " + c.naissanceCalculee +
            " (" + c.naissanceCalculeeSource + ")."
    }
    // Controle avec les autres evenements de la fiche (decès, mariage).
    val an = Regex("[0-9]{4}").find(c.naissanceCalculee)?.value?.toIntOrNull() ?: anCalc
    val anDeces = anneeDe(c.dateDeces)
    if (anDeces != null && an - anDeces > 2)
        return "Incoh\u00e9rence : naissance " + c.naissanceCalculee + " apr\u00e8s le d\u00e9c\u00e8s de " + c.titre() + " (" + c.dateDeces + ")."
    val anMar = anneeDe(c.dateMariage)
    if (anMar != null && anMar - an < 12)
        return "Incoh\u00e9rence : " + c.titre() + " se serait mari\u00e9(e) en " + anMar + ", \u00e0 moins de 12 ans (naissance " + c.naissanceCalculee + ")."
    return null
}

// Controle de coherence IMMEDIAT, SANS modifier la fiche (affichage sous
// le champ Age du formulaire) : message ou null.
fun erreurAge(dateNaissance: String, dateActe: String, age: String): String? {
    if (age.isBlank() || dateNaissance.isBlank()) return null
    val anActe = anneeDe(dateActe) ?: return null
    val ansAge = Regex("[0-9]{1,3}").find(age.trim())?.value?.toIntOrNull() ?: return null
    val exacte = anneeDe(dateNaissance) ?: return null
    val calc = anActe - ansAge
    return if (kotlin.math.abs(exacte - calc) > 2)
        "Incoh\u00e9rence : " + ansAge + " ans en " + anActe + " \u2192 naissance vers " +
            calc + ", la fiche dit " + dateNaissance
    else null
}

// ---------------------------------------------------------- Liaison parent / enfant

// Lie un parent a sa fiche enfant (lien pose seulement s'il est absent) et
// reciproquement, REFUSE si l'age rend la liaison impossible (controle de
// coherence centralise, applique partout).
fun lierParentActe(enfant: Personne, parent: Personne?, estMere: Boolean) {
    if (parent == null) return
    // v0.99 : le refus de coherence porte sur les DEUX sens du lien
    // (enfant.pere/mere ET parent.enfants), pas seulement sur la liste.
    if (incoherenceParente(parent, enfant.dateNaissance) != null) return
    if (estMere) {
        if (enfant.mere == 0L) enfant.mere = parent.id
    } else {
        if (enfant.pere == 0L) enfant.pere = parent.id
    }
    if (!parent.enfants.contains(enfant.id)) parent.enfants.add(enfant.id)
}

// ---------------------------------------------------------- Index des personnes (performances)

// v0.99 : personneParId etait une recherche lineaire (~120 appels). L'index
// id -> personne est construit UNE FOIS par version de la liste et reutilise.
// Invalidation : compteur de generation incremente a chaque mutation de la
// base (enregistrer, rafraichirTout, renumeroter, ajouts/suppressions).
object IndexPersonnes {
    var generation = 0L
        private set

    private var liste: List<Personne>? = null
    private var gen: Long = -1L
    private var map: Map<Long, Personne> = emptyMap()

    fun invalider() { generation += 1L }

    fun de(liste: List<Personne>): Map<Long, Personne> {
        if (liste !== this.liste || gen != generation) {
            map = liste.associateBy { it.id }
            this.liste = liste
            gen = generation
        }
        return map
    }
}

fun personneParId(liste: List<Personne>, id: Long): Personne? =
    if (id == 0L) null else IndexPersonnes.de(liste)[id]

// ---------------------------------------------------------- Mariage (v0.106)

// Lie deux epoux dans les DEUX sens (conjoint principal s'il est libre des
// deux cotes, sinon conjoint supplementaire). Retourne vrai si un lien a
// ete ajoute.
fun lierConjointsActe(a: Personne?, b: Personne?): Boolean {
    if (a == null || b == null || a.id == b.id) return false
    val dejaA = a.conjoint == b.id || a.conjointsSupp.contains(b.id)
    val dejaB = b.conjoint == a.id || b.conjointsSupp.contains(a.id)
    if (dejaA && dejaB) return false
    if (a.conjoint == 0L && b.conjoint == 0L) {
        a.conjoint = b.id
        b.conjoint = a.id
        return true
    }
    if (!dejaA) a.conjointsSupp.add(b.id)
    if (!dejaB) b.conjointsSupp.add(a.id)
    return true
}

// Acte de mariage : le marie est un homme, la mariee une femme. Retourne le
// message de refus, ou null si la saisie est admise (sexe non renseigne = admis).
fun refusSexeMariage(sexeMarie: String, sexeMariee: String): String? {
    if (sexeMarie.trim() == "F") {
        return "Acte de mariage : la personne concern\u00E9e est le MARI\u00C9 (un homme). " +
            "Choisissez ou saisissez le mari\u00E9 ; la mari\u00E9e se renseigne dans son cadre."
    }
    if (sexeMariee.trim() == "M") {
        return "Acte de mariage : la MARI\u00C9E est toujours une femme ; la fiche choisie est celle d'un homme."
    }
    return null
}

// ---------------------------------------------------------- Correspondance par nom + dates (v0.107)

// Deux dates de naissance sont COMPATIBLES si l'une est inconnue ou si leur
// ANNEE est la meme (le jour peut différer : sources imprécises, une année
// seule, une date républicaine...).
fun anneesNaissanceCompatibles(a: String, b: String): Boolean {
    val annee = { s: String -> Regex("[0-9]{4}").find(s)?.value }
    val aa = annee(a) ?: return true
    val bb = annee(b) ?: return true
    return aa == bb
}

// v0.107 : candidats "s'agit-il d'une personne existante ?" proposes
// SYSTEMATIQUEMENT des que le nom est saisi, MEME si les prenoms ne sont
// pas parfaitement identiques : la correspondance est determinee par les
// DATES (annee de naissance compatible, une date inconnue ne bloque pas).
// Les prenoms exacts (sans ordre) et la meme annee exacte remontent en tete.
fun candidatsCorrespondants(
    personnes: List<Personne>,
    nom: String,
    prenoms: String,
    dateNaissance: String,
    excluId: Long = 0L,
    max: Int = 6
): List<Personne> {
    val n = cleNom(nom)
    if (n.isEmpty()) return emptyList()
    val pr = clePrenomsNom(prenoms)
    return personnes
        .filter { q ->
            q.id != excluId && cleNom(q.nom) == n &&
                anneesNaissanceCompatibles(q.dateNaissance, dateNaissance)
        }
        .sortedWith(
            compareBy(
                { if (pr.isNotEmpty() && clePrenomsNom(it.prenoms) == pr) 0 else 1 },
                { if (dateNaissance.isNotBlank() && it.dateNaissance == dateNaissance) 0 else 1 }
            )
        )
        .take(max)
}

// ------------------------------------------- Noms a creer (v0.123)
// Extrait de MainActivity (comportement identique) : noms saisis dans un acte
// qui ne correspondent a aucune fiche -> creation a confirmer.
// Un nom correspond-il deja a une fiche existante (nom + prenoms, casse ignoree) ?
fun nomDejaConnu(personnes: List<Personne>, nomSaisi: String, prenomsSaisis: String): Boolean {
    val n = nomSaisi.trim()
    val pr = prenomsSaisis.trim()
    if (n.isEmpty() && pr.isEmpty()) return true
    return personnes.any {
        cleNom(it.nom) == cleNom(n) &&
        clePrenomsNom(it.prenoms) == clePrenomsNom(pr)
    }
}

// Noms saisis qui ne correspondent a aucune fiche : creation a proposer.
fun nomsNouveaux(personnes: List<Personne>, a: ActeSaisie): List<String> {
    val res = mutableListOf<String>()
    if (a.personneId == 0L && !nomDejaConnu(personnes, a.nom, a.prenoms) && (a.nom.isNotBlank() || a.prenoms.isNotBlank())) {
        res.add((a.prenoms.trim() + " " + a.nom.trim()).trim())
    }
    if (!nomDejaConnu(personnes, a.pereNom, a.perePrenoms)) {
        res.add("p\u00E8re : " + (a.perePrenoms.trim() + " " + a.pereNom.trim()).trim())
    }
    if (!nomDejaConnu(personnes, a.mereNom, a.merePrenoms)) {
        res.add("m\u00E8re : " + (a.merePrenoms.trim() + " " + a.mereNom.trim()).trim())
    }
    if (a.type == "M") {
        if (a.marieeId == 0L && !nomDejaConnu(personnes, a.marieeNom, a.marieePrenoms) && (a.marieeNom.isNotBlank() || a.marieePrenoms.isNotBlank())) {
            res.add("mari\u00E9e : " + (a.marieePrenoms.trim() + " " + a.marieeNom.trim()).trim())
        }
        if (!nomDejaConnu(personnes, a.pMarieeNom, a.pMarieePrenoms)) {
            res.add("p\u00E8re de la mari\u00E9e : " + (a.pMarieePrenoms.trim() + " " + a.pMarieeNom.trim()).trim())
        }
        if (!nomDejaConnu(personnes, a.mMarieeNom, a.mMarieePrenoms)) {
            res.add("m\u00E8re de la mari\u00E9e : " + (a.mMarieePrenoms.trim() + " " + a.mMarieeNom.trim()).trim())
        }
    }
    // v0.73 : coherence ETENDUE - les temoins (parrain, marraine,
    // pretre, officier d'etat civil, temoin) sont verifies aussi.
    a.temoins.forEach { t ->
        if ((t.nom.isNotBlank() || t.prenoms.isNotBlank()) && !nomDejaConnu(personnes, t.nom, t.prenoms)) {
            res.add(
                (if (t.role.isNotBlank()) t.role.trim().lowercase() + " : " else "t\u00E9moin : ") +
                    (t.prenoms.trim() + " " + t.nom.trim()).trim()
            )
        }
    }
    return res
}

// ------------------------------------------------ Homonymes (v0.150)
// Un nom saisi dans un acte peut viser PLUSIEURS fiches de meme nom et prenoms,
// ou une seule dont les dates ne concordent pas avec l'acte. Rien n'est rattache
// sans confirmation : l'application signale et demande de choisir une fiche ou
// d'en creer une nouvelle.

data class AlerteHomonyme(
    val cle: String,            // emplacement : "pers", "pere", "mere", "mariee", "mMariee", "t:0"...
    val libelle: String,        // "Père", "Témoin : Jean DUPONT"...
    val candidats: List<Personne>,
    val plusieurs: Boolean,
    val raison: String
)

// Fiches que l'enregistrement rattacherait au nom saisi (meme regle que
// matchPersonneActe, mais TOUTES, pas seulement la premiere).
fun fichesRattachables(personnes: List<Personne>, nom: String, prenoms: String, dateNaissance: String): List<Personne> {
    val n = nom.trim()
    val pr = prenoms.trim()
    if (n.isEmpty() && pr.isEmpty()) return emptyList()
    return personnes.filter {
        cleNom(it.nom) == cleNom(n) &&
            clePrenomsNom(it.prenoms) == clePrenomsNom(pr) &&
            (it.dateNaissance.isBlank() || dateNaissance.isBlank() || dateGreg(dateNaissance) == it.dateNaissance)
    }
}

// Raison pour laquelle une fiche ne peut pas etre la personne de l'acte (ou null).
// morteAvant : controler aussi un deces anterieur a l'acte (temoin, officier, marie(e)).
fun incompatibleAvecActe(q: Personne, dateActe: String, age: String, morteAvant: Boolean, adulte: Boolean): String? {
    erreurAge(q.dateNaissance, dateActe, age)?.let { return it }
    val an = anneeDe(dateActe) ?: return null
    val nais = anneeDe(q.dateNaissance)
    if (nais != null && nais > an) return "n\u00E9(e) en " + nais.toString() + ", apr\u00E8s l'acte (" + an.toString() + ")."
    val dec = anneeDe(q.dateDeces)
    if (morteAvant && dec != null && dec < an) return "d\u00E9c\u00E9d\u00E9(e) en " + dec.toString() + ", avant l'acte (" + an.toString() + ")."
    if (adulte && nais != null && an - nais < 15) return "n\u00E9(e) en " + nais.toString() + ", trop jeune \u00E0 la date de l'acte (" + an.toString() + ")."
    return null
}

fun alertesHomonymes(personnes: List<Personne>, a: ActeSaisie): List<AlerteHomonyme> {
    val res = mutableListOf<AlerteHomonyme>()
    fun verifier(cle: String, libelle: String, id: Long, nom: String, prenoms: String, forcee: Boolean,
                 naissance: String, age: String, morteAvant: Boolean, adulte: Boolean) {
        if (id != 0L || forcee || (nom.isBlank() && prenoms.isBlank())) return
        val l = fichesRattachables(personnes, nom, prenoms, naissance)
        val nomComplet = (prenoms.trim() + " " + nom.trim()).trim()
        if (l.size >= 2) {
            res.add(AlerteHomonyme(cle, libelle, l.take(8), true,
                nomComplet + " correspond \u00E0 " + l.size.toString() + " fiches : choisissez laquelle, ou cr\u00E9ez une nouvelle fiche."))
        } else if (l.size == 1) {
            val r = incompatibleAvecActe(l[0], a.date, age, morteAvant, adulte)
            if (r != null) {
                res.add(AlerteHomonyme(cle, libelle, l, false,
                    "Une seule fiche porte le nom " + nomComplet + ", mais elle ne concorde pas avec l'acte : " + r +
                        " Rattacher l'acte \u00E0 cette fiche, ou cr\u00E9er une nouvelle fiche ?"))
            }
        }
    }
    val concerne = a.type
    verifier("pers", if (concerne == "M") "Mari\u00E9" else "Personne concern\u00E9e", a.personneId, a.nom, a.prenoms, a.persNouvelle || "pers" in a.nouveauxForces,
        if (concerne == "N") a.date else "", a.ageConcerne, concerne == "M", false)
    verifier("pere", "P\u00E8re", a.pereId, a.pereNom, a.perePrenoms, "pere" in a.nouveauxForces, a.pereNe, a.pereAge, false, false)
    verifier("mere", "M\u00E8re", a.mereId, a.mereNom, a.merePrenoms, "mere" in a.nouveauxForces, a.mereNe, a.mereAge, false, false)
    if (concerne == "M") {
        verifier("mariee", "Mari\u00E9e", a.marieeId, a.marieeNom, a.marieePrenoms, "mariee" in a.nouveauxForces, "", a.marieeAge, true, false)
        verifier("mMariee", "M\u00E8re de la mari\u00E9e", a.mMarieeId, a.mMarieeNom, a.mMarieePrenoms, "mMariee" in a.nouveauxForces, a.mMarieeNe, "", false, false)
    }
    a.temoins.forEachIndexed { i, t ->
        verifier("t:" + i.toString(),
            (if (t.role.isNotBlank()) t.role.trim() else "T\u00E9moin") + " : " + (t.prenoms.trim() + " " + t.nom.trim()).trim(),
            t.id, t.nom, t.prenoms, t.nouveau, "", t.age, true, true)
    }
    return res
}
