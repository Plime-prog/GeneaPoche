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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun decesAffichable(p: Personne, actes: List<Acte> = emptyList()): Boolean {
    if (p.dateDeces.isNotBlank() || p.heureDeces.isNotBlank() || p.lieuDeces.isNotBlank()) return true
    if (p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank()) return true
    if (actes.any { it.personneId == p.id && (it.type == "D" || it.type == "S") }) return true
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
            if (nm != null && n - nm < 13) {
                l.add(Anomalie(t + " : m\u00E8re \u00E2g\u00E9e de " + (n - nm) + " ans \u00E0 la naissance, \u00E0 v\u00E9rifier.", p.id))
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

// Un acte du type avec une IMAGE (les actes transcrits sans image ont un contenu).
fun aUneImage(actes: List<Acte>, personneId: Long, type: String): Boolean {
    return actes.any { it.personneId == personneId && it.type == type && it.contenu.isBlank() }
}

// Un acte du type avec une TRANSCRIPTION (saisie par acte).
fun aUneTranscription(actes: List<Acte>, personneId: Long, type: String): Boolean {
    return actes.any { it.personneId == personneId && it.type == type && it.contenu.isNotBlank() }
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
fun suggestionsPour(p: Personne, personnes: List<Personne>, actes: List<Acte>): List<Suggestion> {
    val res = mutableListOf<Suggestion>()

    fun suffixe(lieu: String, dept: String): String {
        var s = ""
        if (lieu.isNotBlank()) s = " \u00E0 " + lieu.trim()
        if (dept.isNotBlank()) s = s + " (" + dept.trim() + ")"
        return s
    }

    fun ajouter(cle: String, titre: String, type: String, lieu: String = "", dept: String = "", debut: String = "", fin: String = "") {
        res.add(Suggestion(cle, titre, type, lieu, dept, debut, fin))
    }

    // --- Un evenement : date, heure, lieu, departement, copie de l'acte.
    // debut/fin : plage d'annees visee (regle des 75 ans et tri du journal).
    fun evenement(cleBase: String, type: String, lib: LibellesEv, ch: EvChamps, ouvert: Boolean, debut: String = "", fin: String = "") {
        if (!ouvert) return
        val img = aUneImage(actes, p.id, type)
        val trans = aUneTranscription(actes, p.id, type)
        // Reponse utilisateur : PLUS AUCUNE recherche sur un acte
        // manquant ; seules les donnees d un acte DEJA localise (date ou
        // image) completent encore le journal.
        if (ch.date.isBlank() && !img) return
        // Acte localise : chaque champ manquant est une recherche a part entiere.
        // (L'HEURE manquante ne cree PAS de recherche : information annexe.)
        if (ch.lieu.isBlank()) {
            ajouter(cleBase + "_lieu", "Compl\u00E9ter " + lib.lieu + " de " + titreCourt(p), type, "", "", debut, fin)
        }
        if (ch.dept.isBlank()) {
            ajouter(cleBase + "_dept", "Compl\u00E9ter " + lib.dept + " de " + titreCourt(p), type, "", "", debut, fin)
        }
        if (!img && !trans) {
            ajouter(cleBase + "_copie", "Photographier ou transcrire " + lib.acte + " de " + titreCourt(p), type, ch.lieu.trim(), ch.dept.trim(), debut, fin)
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
    val decesPossible = decesAffichable(p, actes)

    evenement("naissance", "N", LibellesNaissance, champsEvenement(p, "N"), true, strN, strN)
    evenement("bapteme", "B", LibellesBapteme, champsEvenement(p, "B"), p.blocsOuverts.contains("bapteme") || p.dateBapteme.isNotBlank() || p.heureBapteme.isNotBlank() || p.lieuBapteme.isNotBlank() || aUneImage(actes, p.id, "B") || aUneTranscription(actes, p.id, "B"), strN, strN)
    evenement("mariage", "M", LibellesMariage, champsEvenement(p, "M"), true, mDebut, mFin)
    // Deces et sepulture : uniquement si la personne peut avoir plus de 100 ans.
    evenement("deces", "D", LibellesDeces, champsEvenement(p, "D"), decesPossible, dDebut, dFin)
    evenement("sepulture", "S", LibellesSepulture, champsEvenement(p, "S"), decesPossible && (p.blocsOuverts.contains("sepulture") || p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank() || aUneImage(actes, p.id, "S") || aUneTranscription(actes, p.id, "S")), dDebut, dFin)

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

    // --- Conjoint et enfants : tous les enfants, toujours.
    if (p.conjoint == 0L && p.conjointsSupp.isEmpty()) {
        ajouter("conjoint", "Rechercher le conjoint de " + titreCourt(p), "C", "", "", mDebut, mFin)
    }
    if (p.enfants.isEmpty()) {
        val anneeM = anneeDe(p.dateMariage)
        val debut = anneeM?.toString() ?: ""
        val fin = anneeM?.let { (it + 25).toString() } ?: ""
        ajouter("enfants", "Rechercher les enfants de " + titreCourt(p) + (if (debut.isNotEmpty()) { " (apr\u00E8s le mariage de " + debut + ")" } else { "" }), "A", debut = debut, fin = fin)
    }

    return res
}

// Recalcule le texte d'une recherche a partir de l'etat actuel des donnees
// (les donnees saisies entre-temps sont immediatement refl\u00E9t\u00E9es).
fun textePisteActuel(piste: Piste, personnes: List<Personne>, actes: List<Acte>): String {
    if (piste.type != "A" || piste.categorie.isEmpty()) return piste.texte
    val pers = personneParId(personnes, piste.personneId) ?: return piste.texte
    val suggestion = suggestionsPour(pers, personnes, actes).firstOrNull { it.cle == piste.categorie }
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

@Composable
fun EcranJournal(
    pistes: List<Piste>,
    personnes: List<Personne>,
    actes: List<Acte>,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    onSauver: () -> Unit
) {
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
    val anomalies = anomaliesPour(personnes)

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        if (anomalies.isNotEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, Color(0xFFE6A23C))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            "Coh\u00E9rence : " + anomalies.size + " anomalie(s)",
                            fontWeight = FontWeight.Bold, color = Color(0xFF9C5A00)
                        )
                        anomalies.take(5).forEach { an ->
                            // Touchez l'anomalie : la fiche a corriger s'ouvre directement.
                            Text(
                                an.texte,
                                style = MaterialTheme.typography.bodySmall,
                                color = CouleurVertFonce,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { onOuvrirPersonne(an.personneId) }
                            )
                        }
                        if (anomalies.size > 5) {
                            Text(
                                "+ " + (anomalies.size - 5) + " autre(s), dans la suite du journal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
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
                    actes = actes,
                    onSupprimer = onSupprimer,
                    onOuvrirPersonne = onOuvrirPersonne,
                    onSauver = onSauver
                )
            }
        }

        // Une recherche effectuee est supprimee sans archive ni historique.

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------- Carte d'une recherche
// Rappel, fiche de la personne, icone de recherche Internet (AD), zone de
// texte libre pour la recherche en cours. Pas de validation.

@Composable
fun CarteRecherche(
    piste: Piste,
    personnes: List<Personne>,
    actes: List<Acte>,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    onSauver: () -> Unit
) {
    val pers = personneParId(personnes, piste.personneId)
    val texte = textePisteActuel(piste, personnes, actes)
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
            OutlinedTextField(
                value = piste.enCours,
                onValueChange = { piste.enCours = it },
                label = { Text("Recherche : pages, c\u00F4tes, notes...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { onSauver() }) {
                    Text("Enregistrer la note")
                }
            }
        }
    }
}


