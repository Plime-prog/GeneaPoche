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
        .replace(Regex("[\\p{Mn}\\p{Cf}']"), " ")
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
) {
    if (c == null) return
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
    naissanceDepuisAge(c, dateActe, age, sourceCalcul)
}

// ---------------------------------------------------------- Naissance approximative (v0.99)

// PARTOUT ou un age est saisi (personne concernee, parents, temoins,
// mariee...), la fiche recoit une date de naissance approximative
// "vers aaaa" SI elle n'a ni date exacte ni date calculee. Si la fiche a
// deja une date EXACTE, le calcul n'ecrit rien mais VERIFIE la coherence :
// un ecart de plus de 2 ans retourne un message d'incoherence (affiche au
// moment de la saisie et rappele a l'enregistrement).
fun naissanceDepuisAge(c: Personne?, dateActe: String, age: String, source: String): String? {
    if (c == null) return null
    val t = age.trim()
    if (t.isBlank()) return null
    val anActe = anneeDe(dateActe) ?: return null
    val ansAge = Regex("[0-9]{1,3}").find(t)?.value?.toIntOrNull() ?: return null
    if (ansAge !in 1..120) return null
    val anCalc = anActe - ansAge
    val exacte = anneeDe(c.dateNaissance)
    if (exacte != null) {
        return if (kotlin.math.abs(exacte - anCalc) > 2)
            "Incoh\u00e9rence : " + ansAge + " ans en " + anActe +
                " donne une naissance vers " + anCalc + ", la fiche de " + c.titre() +
                " dit " + c.dateNaissance + "."
        else null
    }
    if (c.naissanceCalculee.isBlank()) {
        c.naissanceCalculee = "vers " + anCalc.toString()
        c.naissanceCalculeeSource = "calcul\u00e9e d'apr\u00e8s " + source
    }
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
