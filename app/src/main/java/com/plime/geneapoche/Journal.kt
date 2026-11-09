package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Journal de recherches
// EXHAUSTIVITE : toute information manquante (gris\u00E9e dans les fiches) est
// une recherche \u00E0 faire. D\u00E8s qu'une donn\u00E9e est saisie ou qu'une image est
// ajout\u00E9e, la recherche est archiv\u00E9e automatiquement (horodat\u00E9e).
// But : tous les actes N/M/D au minimum pour chacun, avec tous les enfants.

// Une recherche a faire, deduite d'une information manquante d'une personne.
data class Suggestion(
    val cle: String,        // cle technique (stockee dans la piste)
    val titre: String,      // texte complet affichable
    val type: String,       // code evenement : N B M D S P(pere) ME(mere) C(conjoint) A(autre)
    val lieu: String = "",
    val dept: String = "",
    val debut: String = "", // debut de la plage d'annees ciblee
    val fin: String = ""
)

// G2-03 : une SEULE fonction d'annee pour tout le code : anneeDe
// (Fiches.kt), qui gere les dates gregoriennes ET republicaines.
// Annee en cours (regle des 75 ans et des 100 ans, a la date du jour).
fun anneeCourante(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

// La notion de deces ou de sepulture ne s'affiche (fiche, frise, boutons,
// journal, arbre) que si la personne peut avoir PLUS de 100 ans a la date du
// jour, ou si son deces est deja connu (date, lieu ou acte rattache).
fun decesAffichable(p: Personne): Boolean {
    if (p.dateDeces.isNotBlank() || p.heureDeces.isNotBlank() || p.lieuDeces.isNotBlank()) return true
    if (p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank()) return true
    if (p.enregistreDe("D") || p.enregistreDe("S")) return true
    val n = anneeDe(p.dateNaissance) ?: return true
    return anneeCourante() - n >= 100
}

// Section d'etat (vert/rouge) correspondant a un type d'evenement.
fun sectionDe(type: String): String = when (type) {
    "N" -> "naissance"
    "B" -> "bapteme"
    "M" -> "mariage"
    "S" -> "sepulture"
    else -> "deces"
}

// Libelle d'une section pour une recherche du journal.
fun libelleSection(s: String): String = when (s) {
    "identite" -> "l'identit\u00E9 de"
    "naissance" -> "l'acte de naissance de"
    "bapteme" -> "l'acte de bapt\u00EAme de"
    "mariage" -> "l'acte de mariage de"
    "sepulture" -> "l'acte de s\u00E9pulture de"
    else -> "l'acte de d\u00E9c\u00E8s de"
}

// Annee visee par une recherche : fin de la plage estimee, sinon debut.
// Sert a appliquer la regle des 75 ans et a trier les recherches.
fun anneeCiblePiste(piste: Piste): Int? {
    return piste.fin.toIntOrNull() ?: piste.debut.toIntOrNull()
}

fun titreCourt(p: Personne): String {
    val t = p.titre().trim()
    return if (t.isEmpty()) "(sans nom)" else t
}

// ---------------------------------------------------------------- Alertes du jour
// G\u00E9n\u00E9aPoche 12 (12.3) : alertes locales. Un \u00E9v\u00E9nement (naissance,
// mariage, d\u00E9c\u00E8s) dont le JOUR et le MOIS tombent \u00E0 la date du jour,
// quelle que soit l'ann\u00E9e (anniversaire). Les dates r\u00E9publicaines sont
// converties pour retrouver le jour et le mois gr\u00E9goriens.
// Jour et mois d'une date "jj/mm/aaaa" (ou r\u00E9publicaine convertie) ;
// null si la date ne permet pas de conna\u00EEtre le jour et le mois.
fun jourMoisDe(date: String): Pair<Int, Int>? {
    val d = date.trim()
    if (d.isEmpty()) return null
    val regex = Regex("^(\\d{1,2})/(\\d{1,2})/(\\d{4})$")
    val m = regex.find(d)
    if (m != null) return Pair(m.groupValues[1].toInt(), m.groupValues[2].toInt())
    val g = repubAGregorien(d) ?: return null
    val mg = regex.find(g) ?: return null
    return Pair(mg.groupValues[1].toInt(), mg.groupValues[2].toInt())
}

data class AlerteJour(val personneId: Long, val texte: String)

// Alertes du jour : naissances, mariages et d\u00E9c\u00E8s tombant un jour comme
// aujourd'hui. R\u00E8gles de la fiche respect\u00E9es : vivant de moins de 30 ans =
// naissance seule ; d\u00E9c\u00E9d\u00E9 avant 25 ans = pas de mariage ; d\u00E9c\u00E8s
// signal\u00E9 seulement s'il est affichable (r\u00E8gle des 100 ans).
fun alertesDuJour(personnes: List<Personne>): List<AlerteJour> {
    val aujourdhui = java.time.LocalDate.now()
    val res = mutableListOf<AlerteJour>()
    for (p in personnes) {
        val jeunesse = vivantMoinsDe30(p) || decedeAvant25(p)
        fun ajoute(date: String, libelle: String) {
            val jm = jourMoisDe(date) ?: return
            if (jm.first != aujourdhui.dayOfMonth || jm.second != aujourdhui.monthValue) return
            val ans = anneeDe(date)?.let { aujourdhui.year - it }
            val suite = if (ans != null && ans > 0) " (il y a " + ans.toString() + " an(s))" else ""
            res.add(AlerteJour(p.id, libelle + " de " + titreCourt(p) + suite))
        }
        ajoute(p.dateNaissance, "N.")
        if (!jeunesse) ajoute(p.dateMariage, "Mariage")
        if (decesAffichable(p)) ajoute(p.dateDeces, "D\u00E9c\u00E8s")
    }
    return res
}

// ---------------------------------------------------------------- Anomalies

// Incoherence detectee entre les dates d'une personne (comparaisons d'annees).
data class Anomalie(val texte: String, val personneId: Long)

fun anomaliesPour(personnes: List<Personne>): List<Anomalie> {
    val l = mutableListOf<Anomalie>()
    fun nom(id: Long): String = personnes.firstOrNull { it.id == id }?.titre()?.trim() ?: "?"
    personnes.forEach { p ->
        val n = anneeDe(p.dateNaissance)
        val d = anneeDe(p.dateDeces)
        val m = anneeDe(p.dateMariage)
        val t = p.titre().trim().ifEmpty { "(sans nom)" }
        if (n != null && d != null && d < n) {
            l.add(Anomalie(t + " : d\u00E9c\u00E8s (" + d + ") avant la naissance (" + n + ").", p.id))
        }
        if (n != null && m != null && m < n) {
            l.add(Anomalie(t + " : mariage (" + m + ") avant la naissance (" + n + ").", p.id))
        }
        // G\u00E9n\u00E9aPoche 10 : la s\u00E9pulture est APR\u00C8S le d\u00E9c\u00E8s.
        val s = anneeDe(p.dateSepulture)
        if (d != null && s != null && s < d) {
            l.add(Anomalie(t + " : s\u00E9pulture (" + s + ") avant le d\u00E9c\u00E8s (" + d + ").", p.id))
        }
        if (n != null && d != null && d - n > 110) {
            l.add(Anomalie(t + " : " + (d - n) + " ans au d\u00E9c\u00E8s, \u00E0 v\u00E9rifier.", p.id))
        }
        if (n != null) {
            val pere = personnes.firstOrNull { it.id == p.pere }
            val mere = personnes.firstOrNull { it.id == p.mere }
            val np = pere?.let { anneeDe(it.dateNaissance) }
            val dp = pere?.let { anneeDe(it.dateDeces) }
            val nm = mere?.let { anneeDe(it.dateNaissance) }
            val dmm = mere?.let { anneeDe(it.dateDeces) }
            if (np != null && n - np > 80) {
                l.add(Anomalie(t + " : p\u00E8re \u00E2g\u00E9 de " + (n - np) + " ans \u00E0 la naissance.", p.id))
            }
            if (dp != null && dp < n) {
                l.add(Anomalie(t + " : p\u00E8re " + nom(p.pere) + " d\u00E9c\u00E9d\u00E9 (" + dp + ") avant la naissance de l'enfant (" + n + ").", p.id))
            }
            if (nm != null && n >= nm && n - nm < 13) {
                l.add(Anomalie(t + " : m\u00E8re \u00E2g\u00E9e de " + (n - nm) + " ans \u00E0 la naissance, \u00E0 v\u00E9rifier.", p.id))
            }
            // G\u00E9n\u00E9aPoche 10 : un enfant ne na\u00EEt JAMAIS avant ses parents.
            if (np != null && n < np) {
                l.add(Anomalie(t + " : n\u00E9(e) en " + n + ", AVANT son p\u00E8re (" + np + ").", p.id))
            }
            if (nm != null && n < nm) {
                l.add(Anomalie(t + " : n\u00E9(e) en " + n + ", AVANT sa m\u00E8re (" + nm + ").", p.id))
            }
            if (nm != null && n - nm > 55) {
                l.add(Anomalie(t + " : m\u00E8re \u00E2g\u00E9e de " + (n - nm) + " ans \u00E0 la naissance, \u00E0 v\u00E9rifier.", p.id))
            }
            if (dmm != null && dmm < n) {
                l.add(Anomalie(t + " : m\u00E8re " + nom(p.mere) + " d\u00E9c\u00E9d\u00E9e (" + dmm + ") avant la naissance de l'enfant (" + n + ").", p.id))
            }
        }
        if (m != null) {
            p.tousConjoints().forEach { co ->
                val c = personnes.firstOrNull { it.id == co }
                val nc = c?.let { anneeDe(it.dateNaissance) }
                if (nc != null && m < nc) {
                    l.add(Anomalie(t + " : conjoint " + nom(co) + " n\u00E9 en " + nc + ", mariage en " + m + " avant sa naissance.", p.id))
                }
            }
        }
    }
    return l
}

// ---------------------------------------------------------------- Diagnostic de l'arbre
// G\u00E9n\u00E9aPoche 11 (11.2) : doublons, branches isol\u00E9es, identit\u00E9s \u00E0 certifier.

// Doublon possible : m\u00EAme nom ET m\u00EAmes pr\u00E9noms (normalis\u00E9s), fusion guid\u00E9e
// propos\u00E9e dans le journal.
data class Doublon(val idA: Long, val idB: Long, val nom: String)

// Cle stable d'une paire de doublons (ids tries), pour "garder les deux".
fun cleDoublon(idA: Long, idB: Long): String =
    minOf(idA, idB).toString() + "-" + maxOf(idA, idB).toString()

// v0.88 : comparaison insensible aux accents, a la casse et a la ponctuation.
private fun cleHomonyme(s: String): String =
    sansAccents(s).lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

fun doublonsPotentiels(personnes: List<Personne>, gardes: Set<String> = emptySet()): List<Doublon> {
    val l = mutableListOf<Doublon>()
    personnes.forEach { p ->
        val nomP = cleHomonyme(p.nom)
        if (nomP.isEmpty()) return@forEach
        val prenomsP = cleHomonyme(p.prenoms)
        val anneeP = anneeDe(p.dateNaissance)
        personnes.filter { it.id > p.id }.forEach { q ->
            if (nomP == cleHomonyme(q.nom)) {
                val prenomsQ = cleHomonyme(q.prenoms)
                val memesPrenoms = prenomsP == prenomsQ
                // Meme nom, meme premier prenom ET meme annee de naissance :
                // doublon probable meme si les autres prenoms different.
                val anneeQ = anneeDe(q.dateNaissance)
                val memePremier = prenomsP.isNotEmpty() &&
                    prenomsP.substringBefore(' ') == prenomsQ.substringBefore(' ') &&
                    anneeP != null && anneeP == anneeQ
                if (memesPrenoms || memePremier) {
                    if (gardes.contains(cleDoublon(p.id, q.id))) return@forEach
                    l.add(Doublon(p.id, q.id, p.titre()))
                }
            }
        }
    }
    return l
}

// Branche isol\u00E9e : personne sans aucun lien de parent\u00E9 dans la base
// (ni parents, ni conjoint, ni enfants, ni r\u00E9f\u00E9renc\u00E9e par quiconque).
fun brancheIsolee(p: Personne, personnes: List<Personne>): Boolean =
    p.pere == 0L && p.mere == 0L && p.conjoint == 0L &&
        p.conjointsSupp.isEmpty() && p.enfants.isEmpty() &&
        personnes.none {
            it.id != p.id && (it.pere == p.id || it.mere == p.id ||
                it.conjoint == p.id || it.conjointsSupp.contains(p.id) ||
                it.enfants.contains(p.id))
        }

// Libelles grammatiquement corrects pour chaque type d'evenement.
data class LibellesEv(
    val acte: String,
    val heure: String,
    val lieu: String,
    val dept: String
)

val LibellesNaissance = LibellesEv("l'acte de naissance", "l'heure de naissance", "le lieu de naissance", "le d\u00E9partement de naissance")
val LibellesBapteme = LibellesEv("l'acte de bapt\u00EAme", "l'heure du bapt\u00EAme", "le lieu du bapt\u00EAme", "le d\u00E9partement du bapt\u00EAme")
val LibellesMariage = LibellesEv("l'acte de mariage", "l'heure du mariage", "le lieu du mariage", "le d\u00E9partement du mariage")
val LibellesDeces = LibellesEv("l'acte de d\u00E9c\u00E8s", "l'heure du d\u00E9c\u00E8s", "le lieu du d\u00E9c\u00E8s", "le d\u00E9partement du d\u00E9c\u00E8s")
val LibellesSepulture = LibellesEv("l'acte de s\u00E9pulture", "l'heure de s\u00E9pulture", "le lieu de s\u00E9pulture", "le d\u00E9partement de s\u00E9pulture")

// TOUTES les informations manquantes d'une personne. Naissance, mariage et
// deces pour tous ; baptismes et sepultures seulement si le bloc est ouvert.
fun suggestionsPour(p: Personne, personnes: List<Personne>): List<Suggestion> {
    val res = mutableListOf<Suggestion>()

    fun suffixe(lieu: String, dept: String): String {
        var s = ""
        if (lieu.isNotBlank()) s = " \u00E0 " + lieuAffiche(lieu)
        return s
    }

    fun ajouter(cle: String, titre: String, type: String, lieu: String = "", dept: String = "", debut: String = "", fin: String = "") {
        res.add(Suggestion(cle, titre, type, lieu, dept, debut, fin))
    }

    // --- Un evenement : date, heure, lieu, departement, copie de l'acte.
    // debut/fin : plage d'annees visee (regle des 75 ans et tri du journal).
    fun evenement(cleBase: String, type: String, lib: LibellesEv, ch: EvChamps, ouvert: Boolean, debut: String = "", fin: String = "") {
        if (!ouvert) return
        if (ch.date.isBlank()) return
        // Acte localise : chaque champ manquant est une recherche a part entiere.
        // (L'HEURE manquante ne cree PAS de recherche : information annexe.)
        if (ch.lieu.isBlank()) {
            ajouter(cleBase + "_lieu", "Compl\u00E9ter " + lib.lieu + " de " + titreCourt(p), type, "", "", debut, fin)
        }
        // v0.91 : plus de recherche "Compl\u00E9ter le d\u00E9partement" : le
        // d\u00E9partement est d\u00E9riv\u00E9 du code INSEE, il n'est plus saisi.
        // Chantier 12.0 : l'acte n'est plus stocke dans l'app ; une coche
        // "Enregistre" par type. Pas de coche = acte manquant = une recherche.
        if (!p.enregistreDe(type)) {
            ajouter(cleBase + "_enregistrer", "Enregistrer " + lib.acte + " de " + titreCourt(p), type, ch.lieu.trim(), ch.dept.trim(), debut, fin)
        }
    }

    // Plages d'annees visees par chaque evenement (pour la regle des 75 ans).
    val anneeN = anneeDe(p.dateNaissance)
    val strN = anneeN?.toString() ?: ""
    val anneeM = anneeDe(p.dateMariage)
    val mDebut = anneeM?.toString() ?: (if (anneeN != null) (anneeN + 20).toString() else "")
    val mFin = anneeM?.toString() ?: (if (anneeN != null) (anneeN + 45).toString() else "")
    val anneeD = anneeDe(p.dateDeces)
    val dDebut = anneeD?.toString() ?: (if (anneeN != null) (anneeN + 50).toString() else "")
    val dFin = anneeD?.toString() ?: (if (anneeN != null) (anneeN + 95).toString() else "")
    val decesPossible = decesAffichable(p)
    // v0.84 : pas de recherche de mariage/enfants pour les jeunes.
    val jeunesse = vivantMoinsDe30(p) || decedeAvant25(p)

    evenement("naissance", "N", LibellesNaissance, champsEvenement(p, "N"), true, strN, strN)
    evenement("bapteme", "B", LibellesBapteme, champsEvenement(p, "B"), p.blocsOuverts.contains("bapteme") || p.dateBapteme.isNotBlank() || p.heureBapteme.isNotBlank() || p.lieuBapteme.isNotBlank(), strN, strN)
    evenement("mariage", "M", LibellesMariage, champsEvenement(p, "M"), !jeunesse, mDebut, mFin)
    // Deces et sepulture : uniquement si la personne peut avoir plus de 100 ans.
    evenement("deces", "D", LibellesDeces, champsEvenement(p, "D"), decesPossible, dDebut, dFin)
    evenement("sepulture", "S", LibellesSepulture, champsEvenement(p, "S"), decesPossible && (p.blocsOuverts.contains("sepulture") || p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank()), dDebut, dFin)

    // --- Identite (plus de recherche sur la profession : demande utilisateur)
    if (p.sexe.isBlank()) {
        ajouter("sexe", "Pr\u00E9ciser le sexe de " + titreCourt(p), "A")
    }

    // --- Parents : plage d'annees deduite de la naissance de l'enfant
    val lieuN = p.lieuNaissance.trim()
    val deptN = p.deptNaissance.trim()
    if (p.pere == 0L && p.nom.isNotBlank()) {
        val debut = anneeN?.let { (it - 45).toString() } ?: ""
        val fin = anneeN?.let { (it - 20).toString() } ?: ""
        ajouter("pere", "Rechercher son p\u00E8re : un " + p.nom.trim() + (if (debut.isNotEmpty()) { " n\u00E9 entre " + debut + " et " + fin } else { "" }) + suffixe(lieuN, deptN), "P", lieuN, deptN, debut, fin)
    }
    if (p.mere == 0L) {
        val debut = anneeN?.let { (it - 40).toString() } ?: ""
        val fin = anneeN?.let { (it - 15).toString() } ?: ""
        ajouter("mere", "Rechercher sa m\u00E8re" + (if (debut.isNotEmpty()) { " : n\u00E9e entre " + debut + " et " + fin } else { "" }) + suffixe(lieuN, deptN), "ME", lieuN, deptN, debut, fin)
    }

    // v0.84 : ni recherche de conjoint ni d'enfants pour un vivant de
    // moins de 30 ans, ni pour une personne decedee avant 25 ans.
    if (!jeunesse) {
        if (p.conjoint == 0L && p.conjointsSupp.isEmpty()) {
            ajouter("conjoint", "Rechercher le conjoint de " + titreCourt(p), "C", "", "", mDebut, mFin)
        }
        if (p.enfants.isEmpty()) {
            val anneeM = anneeDe(p.dateMariage)
            val debut = anneeM?.toString() ?: ""
            val fin = anneeM?.let { (it + 25).toString() } ?: ""
            ajouter("enfants", "Rechercher les enfants de " + titreCourt(p) + (if (debut.isNotEmpty()) { " (apr\u00E8s le mariage de " + debut + ")" } else { "" }), "A", debut = debut, fin = fin)
        }
    }

    return res
}

// Recalcule le texte d'une recherche a partir de l'etat actuel des donnees
// (les donnees saisies entre-temps sont immediatement refl\u00E9t\u00E9es).
fun textePisteActuel(piste: Piste, personnes: List<Personne>): String {
    if (piste.type != "A" || piste.categorie.isEmpty()) return piste.texte
    val pers = personneParId(personnes, piste.personneId) ?: return piste.texte
    val suggestion = suggestionsPour(pers, personnes).firstOrNull { it.cle == piste.categorie }
    return suggestion?.titre ?: piste.texte
}

// Libelle d'une note archivee.
fun libelleRecherche(r: Recherche): String {
    return when (r.resultat) {
        "T" -> "TROUV\u00C9 \u2013 " + r.note
        "X" -> r.note
        else -> "Rien trouv\u00E9" +
            (if (r.page.isNotBlank()) { " jusqu'\u00E0 la page " + r.page } else { "" }) +
            (if (r.repriseVille.isNotBlank()) { ", TD de " + r.repriseVille } else { "" }) +
            (if (r.repriseAnnee.isNotBlank()) { ", reprendre \u00E0 partir de " + r.repriseAnnee } else { "" }) +
            (if (r.note.isNotBlank()) { " \u2013 " + r.note } else { "" })
    }
}

// ---------------------------------------------------------------- Ecran Journal
// Regroupe les recherches par personne, avec compteurs et acces a la fiche.

// Numero de la personne concernee par une piste (pour le tri du journal).
fun comparerNumeros(a: List<Int>, b: List<Int>): Int {
    val n = minOf(a.size, b.size)
    for (i in 0 until n) {
        val r = a[i].compareTo(b[i])
        if (r != 0) return r
    }
    return a.size.compareTo(b.size)
}

fun numeroDePiste(piste: Piste, personnes: List<Personne>): List<Int> {
    val p = personneParId(personnes, piste.personneId) ?: return listOf(Int.MAX_VALUE)
    return cleNumero(p)
}

// v0.90 : "À corriger" — TOUT ce qui est corrigeable dans les ANCIENNES
// saisies : lieu non conforme (sans code INSEE, éventuellement avec un
// "France" résiduel) et dates invalides. La proposition est la
// normalisation automatique quand la partie "commune" du lieu matche
// UNE seule commune de la base ; vide sinon (ressaisie à la main).
data class ErreurSaisie(
    val personneId: Long,
    val champ: String,      // "naissance", "deces", "evl:3"... "" = dates
    val libelle: String,    // "Prénom Nom" ou "événement : ..."
    val valeur: String,     // valeur actuelle
    val proposition: String, // correction proposée, "" si aucune
    val dates: Boolean = false
)

// v0.91 : proposition robuste - la partie "commune" doit ressembler a
// un NOM (pas un numero de departement restant d'une ancienne saisie),
// et le lieu-dit devant est conserve SANS les anciens segments techniques
// (numero ou nom de departement seuls), sinon la correction insere la
// commune derriere des restes qui n'ont rien a voir.
private fun propositionLieu(l: String): String {
    if (!BaseCommunes.pret()) return ""
    val partie = partieSaisie(l).trim()
    if (partie.length < 2) return ""
    // La partie tapee doit contenir des lettres (un "59" ou "Nord" seul
    // n'est pas une commune).
    if (!Regex("[A-Za-z\u00C0-\u017F]").containsMatchIn(partie)) return ""
    val s = BaseCommunes.suggestions(partie, 2)
    if (s.size != 1) return ""
    // Lieu-dit = ce qui precede, nettoye des anciens segments dept.
    val i = l.lastIndexOf(',')
    var prefixe = (if (i >= 0) l.substring(0, i) else "").trim().trimEnd(',', ';')
    val m = Regex("[,; ](\\d{1,3}|2[AB]\\d{0,3}|\\d{5})$").find(prefixe)
    if (m != null) prefixe = prefixe.substring(0, m.range.first).trim().trimEnd(',', ';')
    // ...et un nom de d\u00E9partement seul \u00E0 la fin ("Roubaix, Nord").
    val segs = prefixe.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (segs.size >= 2 && numeroDepartement(segs.last()) != null) prefixe = segs.dropLast(1).joinToString(", ")
    return (if (prefixe.isNotEmpty()) prefixe + ", " else "") +
        s.first().nom + ", " + s.first().code + ", " + BaseCommunes.nomDept(s.first().dept)
}

fun erreursSaisie(personnes: List<Personne>): List<ErreurSaisie> {
    val res = mutableListOf<ErreurSaisie>()
    val normalise = Regex(", (\\d{5}|2[AB]\\d{3})(,|$)")
    for (p in personnes) {
        val champs = listOf(
            "naissance" to p.lieuNaissance,
            "bapteme" to p.lieuBapteme,
            "mariage" to p.lieuMariage,
            "deces" to p.lieuDeces,
            "sepulture" to p.lieuSepulture
        )
        for ((champ, valeur) in champs) {
            val l = valeur.trim()
            if (l.isNotEmpty() && !normalise.containsMatchIn(l)) {
                res.add(ErreurSaisie(p.id, champ, p.titre().trim(), l, propositionLieu(l)))
            }
        }
        p.evenementsLibres.forEach { ev ->
            val l = ev.lieu.trim()
            if (l.isNotEmpty() && !normalise.containsMatchIn(l)) {
                res.add(ErreurSaisie(p.id, "evl:" + ev.id, "événement : " + ev.intitule.trim(), l, propositionLieu(l)))
            }
        }
        val datesKO = datesInvalides(p)
        if (datesKO.isNotEmpty()) {
            res.add(ErreurSaisie(p.id, "", p.titre().trim(), datesKO.joinToString(", "), "", dates = true))
        }
    }
    return res
}

@Composable
fun EcranJournal(
    pistes: List<Piste>,
    personnes: List<Personne>,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    onSauver: () -> Unit,
    // G\u00E9n\u00E9aPoche 11 (11.2) : fusion guid\u00E9e d'un doublon (garder A / garder B).
    onFusionner: (Long, Long) -> Unit = { _, _ -> },
    // v0.78 : paires "gard\u00E9es les deux" (d\u00E9cision d\u00E9finitive de l'utilisateur).
    gardes: Set<String> = emptySet(),
    onGarderDeux: (Long, Long) -> Unit = { _, _ -> },
    // v0.88 : "Anomalies \u00E0 v\u00E9rifier" (Ignorer / Corriger / Examiner).
    anomaliesIgnorees: Set<String> = emptySet(),
    onIgnorerAnomalie: (String) -> Unit = {},
    onReafficherAnomalies: () -> Unit = {},
    // v0.90 : corriger en un clic un lieu des anciennes saisies
    // (personneId, champ, nouvelle valeur).
    onCorrigerLieu: (Long, String, String) -> Unit = { _, _, _ -> }
) {
    // GeneaPoche 12 : bouton d'actualisation du diagnostic A LA DEMANDE.
    // key(rafraichir) force le recalcul complet des listes du diagnostic
    // (les corrections de fiche modifient des champs SANS invalidation
    // automatique de la composition).
    var rafraichir by remember { mutableStateOf(0) }
    // v0.90 : la base des communes doit \u00EAtre charg\u00E9e pour proposer les
    // corrections automatiques de lieux.
    val ctxJournal = LocalContext.current
    LaunchedEffect(Unit) { BaseCommunes.charger(ctxJournal) }
    key(rafraichir) {
    val ouvertes = pistes.filter { !it.faite }
    // REGLE DES 75 ANS : seules les recherches dont l'\u00E9v\u00E9nement vis\u00E9 est
    // survenu il y a PLUS de 75 ans \u00E0 la date du jour sont affich\u00E9es (actes
    // librement communicables). Tri : l'\u00E9v\u00E9nement le plus proche de nous
    // d'abord. Plafond : 10 recherches au total. Chaque carte rappelle le
    // num\u00E9ro d'Aboville de la personne concern\u00E9e.
    val limite = anneeCourante() - 75
    // Reponse utilisateur : ordre CROISSANT des numeros de haut en bas
    // (Sosa puis d Aboville), toujours limite aux evenements de plus de 75 ans.
    val prioritaires = ouvertes
        .filter { ps -> val a = anneeCiblePiste(ps); a == null || a <= limite }
        .sortedWith { a, b -> comparerNumeros(numeroDePiste(a, personnes), numeroDePiste(b, personnes)) }
        .take(10)
    val anomalies = anomaliesDeLArbre(personnes, anomaliesIgnorees)
    // v0.90 : anciennes saisies \u00E0 corriger (lieux, dates).
    val aCorriger = erreursSaisie(personnes)
    // G\u00E9n\u00E9aPoche 11 (11.2) : diagnostic complet de l'arbre.
    val doublons = doublonsPotentiels(personnes, gardes)
    val isolees = personnes.filter { brancheIsolee(it, personnes) }
    val nonCertifiees = personnes.filterNot { identiteCertifiee(it) }
    // G\u00E9n\u00E9aPoche 12 (12.3) : anniversaires du jour (alertes locales).
    val anniversaires = alertesDuJour(personnes)
    // GeneaPoche 12 : BUG CORRIGE - les identites non certifiees comptaient
    // AUSSI dans l'encadre : il ne doit pas disparaitre tant qu'il reste
    // quelque chose a examiner (anomalie, doublon, branche isolee, identite).
    val diagnostic = anomalies.isNotEmpty() || doublons.isNotEmpty() ||
        isolees.isNotEmpty() || nonCertifiees.isNotEmpty()
    // G\u00E9n\u00E9aPoche 11 : fusion guid\u00E9e en attente de confirmation.
    var fusionEnCours by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var anomalieExaminee by remember { mutableStateOf<AnomalieAVerifier?>(null) }
    var toutesLesAnomalies by remember { mutableStateOf(false) }
    fun nomDe(id: Long): String = personnes.firstOrNull { it.id == id }?.titre() ?: "?"

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        // GeneaPoche 12 : actualisation du diagnostic a la demande.
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { rafraichir++ }, Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualiser le diagnostic", Modifier.size(16.dp))
                }
                Text(
                    "Actualiser le diagnostic",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // v0.90 : "\u00C0 corriger" - anciennes saisies non conformes (lieux
        // sans code INSEE, dates invalides), normalisables en un clic
        // quand la commune matche une seule entr\u00E9e de la base.
        if (aCorriger.isNotEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, Color(0xFFE6A23C))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            "\u00C0 corriger : " + aCorriger.size.toString() + " saisie(s) d'ancienne version",
                            fontWeight = FontWeight.Bold, color = Color(0xFF9C5A00)
                        )
                        aCorriger.take(20).forEach { e ->
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        e.libelle + (if (e.dates) " : dates invalides (" + e.valeur + ")" else " : lieu non conforme"),
                                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B4A12)
                                    )
                                    if (!e.dates && e.valeur.isNotEmpty()) {
                                        Text(e.valeur, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B4A12))
                                    }
                                    // v0.91 : la valeur qui sera enregistree
                                    // est montree AVANT le clic.
                                    if (e.proposition.isNotEmpty()) {
                                        Text(
                                            "\u2192 " + e.proposition,
                                            style = MaterialTheme.typography.bodySmall, color = CouleurVertFonce
                                        )
                                    }
                                }
                                if (e.proposition.isNotEmpty()) {
                                    // v0.91 : rafraichir++ force la liste
                                    // "A corriger" a se recalculer aussitot.
                                    TextButton(onClick = { onCorrigerLieu(e.personneId, e.champ, e.proposition); rafraichir++ }) {
                                        Text("Corriger")
                                    }
                                }
                                TextButton(onClick = { onOuvrirPersonne(e.personneId) }) {
                                    Text("Fiche")
                                }
                            }
                        }
                    }
                }
            }
        }
        // G\u00E9n\u00E9aPoche 12 (12.3) : anniversaires du jour (alertes locales),
        // chaque ligne cliquable ouvre la fiche.
        if (anniversaires.isNotEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CouleurVertPale),
                    border = BorderStroke(1.dp, Color(0xFF9CBF9A))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            "Ce jour : " + anniversaires.size.toString() + " \u00E9v\u00E9nement(s)",
                            fontWeight = FontWeight.Bold, color = CouleurVertFonce
                        )
                        anniversaires.forEach { al ->
                            Text(
                                al.texte,
                                style = MaterialTheme.typography.bodySmall,
                                color = CouleurVertFonce,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { onOuvrirPersonne(al.personneId) }
                            )
                        }
                    }
                }
            }
        }
        if (anomaliesIgnorees.isNotEmpty()) {
            item {
                TextButton(onClick = onReafficherAnomalies) {
                    Text("R\u00E9afficher les anomalies ignor\u00E9es", fontSize = 12.sp)
                }
            }
        }
        if (diagnostic) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, Color(0xFFE6A23C))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            "Diagnostic de l'arbre : " +
                                (anomalies.size + doublons.size + isolees.size + nonCertifiees.size).toString() +
                                " point(s) \u00E0 examiner",
                            fontWeight = FontWeight.Bold, color = Color(0xFF9C5A00)
                        )
                        if (anomalies.isNotEmpty()) {
                            Text(
                                "Anomalies \u00E0 v\u00E9rifier : " + anomalies.size,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9C5A00),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Text(
                                "Ce ne sont pas des erreurs : c'est \u00E0 vous d'en juger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B4A12)
                            )
                            val visibles = if (toutesLesAnomalies) anomalies else anomalies.take(5)
                            visibles.forEach { an ->
                                Text(
                                    "\u26A0\uFE0F " + an.texte,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4A3000),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Row {
                                    TextButton(onClick = { onIgnorerAnomalie(an.cle) }) {
                                        Text("Ignorer", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onOuvrirPersonne(an.personneId) }) {
                                        Text("Corriger", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { anomalieExaminee = an }) {
                                        Text("Examiner", fontSize = 12.sp)
                                    }
                                }
                            }
                            if (anomalies.size > 5) {
                                TextButton(onClick = { toutesLesAnomalies = !toutesLesAnomalies }) {
                                    Text(
                                        if (toutesLesAnomalies) "Afficher moins"
                                        else "Afficher les " + (anomalies.size - 5).toString() + " autre(s)",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        // Doublons : fusion guid\u00E9e (garder l'une des deux fiches).
                        if (doublons.isNotEmpty()) {
                            Text(
                                "Doublons : " + doublons.size,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9C5A00),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            doublons.take(4).forEach { d ->
                                // v0.78 : N et D des DEUX fiches pour comparer
                                // avant de fusionner ou de garder les deux.
                                fun ligneND(id: Long): String {
                                    val q = personnes.firstOrNull { it.id == id }
                                    val n = dateAffichee(q?.dateNaissance?.trim() ?: "")
                                    val dd = if (q != null && decesAffichable(q)) dateAffichee(q.dateDeces.trim()) else ""
                                    return (q?.titre() ?: "?") +
                                        " \u2013 N. " + (if (n.isNotEmpty()) n else "?") +
                                        " \u2013 D. " + (if (dd.isNotEmpty()) dd else "?")
                                }
                                Text(
                                    "M\u00EAme nom : " + d.nom,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    "1. " + ligneND(d.idA),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CouleurVertFonce,
                                    modifier = Modifier.padding(start = 10.dp).clickable { onOuvrirPersonne(d.idA) }
                                )
                                Text(
                                    "2. " + ligneND(d.idB),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CouleurVertFonce,
                                    modifier = Modifier.padding(start = 10.dp).clickable { onOuvrirPersonne(d.idB) }
                                )
                                Row {
                                    TextButton(onClick = { fusionEnCours = d.idA to d.idB }) {
                                        Text("Fusionner (garder 1)", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { fusionEnCours = d.idB to d.idA }) {
                                        Text("Fusionner (garder 2)", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onGarderDeux(d.idA, d.idB) }) {
                                        Text("Garder les deux", fontSize = 12.sp)
                                    }
                                }
                            }
                            if (doublons.size > 4) {
                                Text(
                                    "+ " + (doublons.size - 4) + " autre(s) doublon(s).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Branches isol\u00E9es : personne sans aucun lien.
                        if (isolees.isNotEmpty()) {
                            Text(
                                "Branches isol\u00E9es : " + isolees.size + " personne(s) sans lien",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9C5A00),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            isolees.take(4).forEach { iso ->
                                Text(
                                    iso.titre() + " (aucun lien de parent\u00E9)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CouleurVertFonce,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { onOuvrirPersonne(iso.id) }
                                )
                            }
                            if (isolees.size > 4) {
                                Text(
                                    "+ " + (isolees.size - 4) + " autre(s).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // G\u00E9n\u00E9aPoche 11 (11.0) : identit\u00E9s non certifi\u00E9es
                        // (la coche sert aussi aux recherches).
                        if (nonCertifiees.isNotEmpty()) {
                            Text(
                                "Identit\u00E9 non certifi\u00E9e : " + nonCertifiees.size + " personne(s)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9C5A00),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            nonCertifiees.take(4).forEach { nc ->
                                Text(
                                    nc.titre() + " \u2013 \u00E0 certifier depuis sa fiche",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CouleurVertFonce,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { onOuvrirPersonne(nc.id) }
                                )
                            }
                            if (nonCertifiees.size > 4) {
                                Text(
                                    "+ " + (nonCertifiees.size - 4) + " autre(s).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (prioritaires.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF9CBF9A))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Tout est renseign\u00E9 !", fontWeight = FontWeight.Bold)
                        Text(
                            "Aucune recherche prioritaire : aucun \u00E9v\u00E9nement de plus de 75 ans \u00E0 chercher pour le moment.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Liste plate, tri\u00E9e par \u00E9v\u00E9nement le plus proche de nous.
        prioritaires.forEach { ps ->
            item(key = "ouverte" + ps.id.toString()) {
                CarteRecherche(
                    piste = ps,
                    personnes = personnes,
                    onSupprimer = onSupprimer,
                    onOuvrirPersonne = onOuvrirPersonne,
                    onSauver = onSauver
                )
            }
        }

        // Une recherche effectuee est supprimee sans archive ni historique.

        item { Spacer(Modifier.height(24.dp)) }
    }

    // G\u00E9n\u00E9aPoche 11 (11.2) : confirmation de la fusion guid\u00E9e.
    anomalieExaminee?.let { an ->
        AlertDialog(
            onDismissRequest = { anomalieExaminee = null },
            title = { Text("Anomalie \u00E0 v\u00E9rifier") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("\u26A0\uFE0F " + an.texte)
                    Spacer(Modifier.height(8.dp))
                    Text(an.explication, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Dates saisies", fontWeight = FontWeight.Bold)
                    an.details.forEach { ligne ->
                        Text(ligne, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    an.concernes.forEach { id ->
                        TextButton(onClick = { anomalieExaminee = null; onOuvrirPersonne(id) }) {
                            Text("Ouvrir la fiche : " + nomDe(id))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIgnorerAnomalie(an.cle); anomalieExaminee = null }) { Text("Ignorer") }
            },
            dismissButton = { TextButton(onClick = { anomalieExaminee = null }) { Text("Fermer") } }
        )
    }
    if (fusionEnCours != null) {
        val gardeId = fusionEnCours!!.first
        val supprimeId = fusionEnCours!!.second
        AlertDialog(
            onDismissRequest = { fusionEnCours = null },
            title = { Text("Fusionner les deux fiches ?") },
            text = {
                Text(
                    "Toutes les informations et tous les liens de " + nomDe(supprimeId) +
                        " seront report\u00E9s sur " + nomDe(gardeId) +
                        " (aucune donn\u00E9e d\u00E9j\u00E0 pr\u00E9sente ne sera \u00E9cras\u00E9e), puis la fiche " +
                        nomDe(supprimeId) + " dispara\u00EEtra."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    fusionEnCours = null
                    onFusionner(gardeId, supprimeId)
                }) { Text("Fusionner", color = CouleurVertFonce, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { fusionEnCours = null }) { Text("Annuler") }
            }
        )
    }
    }
}

// ---------------------------------------------------------------- Carte d'une recherche
// Rappel, fiche de la personne, icone de recherche Internet (AD), zone de
// texte libre pour la recherche en cours. Pas de validation.

@Composable
fun CarteRecherche(
    piste: Piste,
    personnes: List<Personne>,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    onSauver: () -> Unit
) {
    val pers = personneParId(personnes, piste.personneId)
    val texte = textePisteActuel(piste, personnes)
    // Num\u00E9ro d'Aboville de la personne (ou Sosa) : rappel\u00E9 sur la pastille.
    val numeroAboville = pers?.numeroAffiche()?.ifEmpty { "?" } ?: "?"
    // Type d acte vise par la recherche : N, M, D (B, S aussi).
    val lettreEvenement = when {
        piste.categorie.startsWith("naissance") -> "N"
        piste.categorie.startsWith("mariage") -> "M"
        piste.categorie.startsWith("deces") -> "D"
        piste.categorie.startsWith("bapteme") -> "B"
        piste.categorie.startsWith("sepulture") -> "S"
        else -> ""
    }

    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Reponse utilisateur : pastille = numero + type d acte (NMD)
                Box(
                    Modifier
                        .background(CouleurVert, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        numeroAboville + (if (lettreEvenement.isNotEmpty()) " " + lettreEvenement else ""),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (numeroAboville.length <= 3) 12.sp else 9.sp,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    texte,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { onSupprimer(piste) }, Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", Modifier.size(16.dp))
                }
            }

            // Personne concernee : appui sur le nom = fiche (plus de bouton).
            if (pers != null) {
                Text(
                    titreCourt(pers) + " \u2013 " + pers.numeroAffiche(),
                    Modifier.fillMaxWidth().clickable { onOuvrirPersonne(piste.personneId) },
                    style = MaterialTheme.typography.labelMedium,
                    color = CouleurVertFonce,
                    maxLines = 1
                )
            }

            // Zone de saisie libre unique pour la recherche en cours
            // GénéaPoche 9 : note LIÉE à la piste (piste.enCours),
            // exactement celle de la fiche.
            // v0.49 : etat local, la liaison directe piste.enCours ne rafraichissait pas le cadre.
            var noteJournal by remember(piste.id) { mutableStateOf(piste.enCours) }
            OutlinedTextField(
                value = noteJournal,
                onValueChange = { noteJournal = it },
                label = { Text("Recherche : pages, c\u00F4tes, notes...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { piste.enCours = noteJournal; onSauver() }) {
                    Text("Enregistrer la note")
                }
            }
        }
    }
}




