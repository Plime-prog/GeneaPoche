package com.plime.geneapoche

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Suggestions de recherche

// Une recherche a faire, deduite de ce qui est saisi et verifie pour une personne.
data class Suggestion(
    val cle: String,        // cle technique (stockee dans la piste)
    val titre: String,      // texte complet affichable
    val type: String,      // code evenement : N B M D S P(pere) ME(mere) C(conjoint) A(autre)
    val categorie: String, // libelle court pour le dialogue de resultat
    val lieu: String = "",
    val dept: String = "",
    val debut: String = "", // debut de la plage d'annees ciblee
    val fin: String = ""
)

fun dateVersAnnee(date: String): Int? {
    return Regex("[0-9]{4}").find(date)?.value?.toIntOrNull()
}

fun titreCourt(p: Personne): String {
    val t = p.titre().trim()
    return if (t.isEmpty()) "(sans nom)" else t
}

// Toutes les recherches deduites de l'etat actuel d'une personne :
// acte manquant ou non verifie, parents inconnus (avec plage d'annees et lieu),
// conjoint et enfants a trouver.
fun suggestionsPour(p: Personne, personnes: List<Personne>, actes: List<Acte>): List<Suggestion> {
    val res = mutableListOf<Suggestion>()

    fun suffixe(lieu: String, dept: String): String {
        var s = ""
        if (lieu.isNotBlank()) s = " \u00E0 " + lieu.trim()
        if (dept.isNotBlank()) s = s + " (" + dept.trim() + ")"
        return s
    }

    val anneeN = dateVersAnnee(p.dateNaissance)
    val lieuN = p.lieuNaissance.trim()
    val deptN = p.deptNaissance.trim()

    // --- Evenements de la personne
    if (p.dateNaissance.isBlank() || p.etatDe("naissance") != "vert") {
        res.add(Suggestion("naissance", "Rechercher l'acte de naissance de " + titreCourt(p) + (anneeN?.let { " (" + it.toString() + ")" } ?: "") + suffixe(lieuN, deptN), "N", "naissance", lieuN, deptN))
    }
    if (p.dateBapteme.isBlank() || p.etatDe("bapteme") != "vert") {
        res.add(Suggestion("bapteme", "Rechercher le bapt\u00EAme de " + titreCourt(p) + suffixe(p.lieuBapteme.trim(), p.deptBapteme.trim()), "B", "bapt\u00EAme", p.lieuBapteme.trim(), p.deptBapteme.trim()))
    }
    if (p.dateMariage.isBlank() || p.etatDe("mariage") != "vert") {
        res.add(Suggestion("mariage", "Rechercher le mariage de " + titreCourt(p) + suffixe(p.lieuMariage.trim(), p.deptMariage.trim()), "M", "mariage", p.lieuMariage.trim(), p.deptMariage.trim()))
    }
    if (p.dateDeces.isBlank() || p.etatDe("deces") != "vert") {
        res.add(Suggestion("deces", "Rechercher l'acte de d\u00E9c\u00E8s de " + titreCourt(p) + suffixe(p.lieuDeces.trim(), p.deptDeces.trim()), "D", "d\u00E9c\u00E8s", p.lieuDeces.trim(), p.deptDeces.trim()))
    }
    if (p.dateSepulture.isBlank() || p.etatDe("sepulture") != "vert") {
        res.add(Suggestion("sepulture", "Rechercher la s\u00E9pulture de " + titreCourt(p) + suffixe(p.lieuSepulture.trim(), p.deptSepulture.trim()), "S", "s\u00E9pulture", p.lieuSepulture.trim(), p.deptSepulture.trim()))
    }

    // --- Parents : plage d'annees deduite de la naissance de l'enfant
    if (p.pere == 0L && p.nom.isNotBlank()) {
        val debut = anneeN?.let { (it - 45).toString() } ?: ""
        val fin = anneeN?.let { (it - 20).toString() } ?: ""
        res.add(Suggestion("pere", "Rechercher son p\u00E8re : un " + p.nom.trim() + (if (debut.isNotEmpty()) { " n\u00E9 entre " + debut + " et " + fin } else {""}) + suffixe(lieuN, deptN), "P", "p\u00E8re", lieuN, deptN, debut, fin))
    }
    if (p.mere == 0L) {
        val debut = anneeN?.let { (it - 40).toString() } ?: ""
        val fin = anneeN?.let { (it - 15).toString() } ?: ""
        res.add(Suggestion("mere", "Rechercher sa m\u00E8re" + (if (debut.isNotEmpty()) { " : n\u00E9e entre " + debut + " et " + fin } else {""}) + suffixe(lieuN, deptN), "ME", "m\u00E8re", lieuN, deptN, debut, fin))
    }

    // --- Conjoint et enfants
    if (p.conjoint == 0L && p.conjointsSupp.isEmpty()) {
        res.add(Suggestion("conjoint", "Rechercher le conjoint de " + titreCourt(p), "C", "conjoint"))
    }
    if (p.enfants.isEmpty()) {
        val anneeM = dateVersAnnee(p.dateMariage)
        val debut = anneeM?.toString() ?: ""
        val fin = anneeM?.let { (it + 25).toString() } ?: ""
        res.add(Suggestion("enfants", "Rechercher les enfants de " + titreCourt(p) + (if (debut.isNotEmpty()) { " (apr\u00E8s le mariage de " + debut + ")" } else {""}), "A", "enfants", debut = debut, fin = fin))
    }

    // --- Arriere-plongee : parents des parents
    if (p.pere != 0L) {
        val per = personneParId(personnes, p.pere)
        if (per != null) {
            if (per.dateNaissance.isBlank() || per.etatDe("naissance") != "vert") {
                res.add(Suggestion("pere_naissance", "Rechercher l'acte de naissance du p\u00E8re : " + titreCourt(per) + suffixe(per.lieuNaissance.trim(), per.deptNaissance.trim()), "N", "naissance du p\u00E8re", per.lieuNaissance.trim(), per.deptNaissance.trim()))
            }
            if (per.pere == 0L && per.nom.isNotBlank()) {
                val a = dateVersAnnee(per.dateNaissance)
                val debut = a?.let { (it - 45).toString() } ?: ""
                val fin = a?.let { (it - 20).toString() } ?: ""
                res.add(Suggestion("pere_pere", "Rechercher le grand-p\u00E8re paternel : un " + per.nom.trim() + (if (debut.isNotEmpty()) { " n\u00E9 entre " + debut + " et " + fin } else {""}), "P", "grand-p\u00E8re paternel", debut = debut, fin = fin))
            }
            if (per.mere == 0L) {
                val a = dateVersAnnee(per.dateNaissance)
                val debut = a?.let { (it - 40).toString() } ?: ""
                val fin = a?.let { (it - 15).toString() } ?: ""
                res.add(Suggestion("pere_mere", "Rechercher la grand-m\u00E8re paternelle" + (if (debut.isNotEmpty()) { " : n\u00E9e entre " + debut + " et " + fin } else {""}), "ME", "grand-m\u00E8re paternelle", debut = debut, fin = fin))
            }
        }
    }
    if (p.mere != 0L) {
        val mer = personneParId(personnes, p.mere)
        if (mer != null && (mer.dateNaissance.isBlank() || mer.etatDe("naissance") != "vert")) {
            res.add(Suggestion("mere_naissance", "Rechercher l'acte de naissance de la m\u00E8re : " + titreCourt(mer) + suffixe(mer.lieuNaissance.trim(), mer.deptNaissance.trim()), "N", "naissance de la m\u00E8re", mer.lieuNaissance.trim(), mer.deptNaissance.trim()))
        }
    }
    return res
}

// Recalcule le texte d'une piste automatique a partir de l'etat actuel des donnees
// (corrige les textes perimes quand une personne a ete modifiee).
fun textePisteActuel(piste: Piste, personnes: List<Personne>, actes: List<Acte>): String {
    if (piste.type != "A" || piste.categorie.isEmpty()) return piste.texte
    val pers = personneParId(personnes, piste.personneId) ?: return piste.texte
    val suggestion = suggestionsPour(pers, personnes, actes).firstOrNull { it.cle == piste.categorie }
    return suggestion?.titre ?: piste.texte
}

// Libelle d'un compte-rendu de recherche ("b. Rappels").
fun libelleRecherche(r: Recherche): String {
    return when (r.resultat) {
        "T" -> "TROUV\u00C9 \u2013 " + r.note
        "X" -> r.note
        else -> "Rien trouv\u00E9" +
            (if (r.page.isNotBlank()) { " jusqu'\u00E0 la page " + r.page } else {""}) +
            (if (r.repriseVille.isNotBlank()) { ", TD de " + r.repriseVille } else {""}) +
            (if (r.repriseAnnee.isNotBlank()) { ", reprendre \u00E0 partir de " + r.repriseAnnee } else {""}) +
            (if (r.note.isNotBlank()) { " \u2013 " + r.note } else {""})
    }
}

// ---------------------------------------------------------------- Ecran Journal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranJournal(
    pistes: List<Piste>,
    personnes: List<Personne>,
    actes: List<Acte>,
    onBasculer: (Piste) -> Unit,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    noterRecherche: (Piste) -> Unit,
    onNouvellePiste: () -> Unit,
    onSauver: () -> Unit,
    ouvrirLien: (String) -> Unit
) {
    val ouvertes = pistes.filter { !it.faite }.sortedBy { it.creation }.take(5)
    val ouvertesManuelles = pistes.filter { !it.faite && !ouvertes.contains(it) }
    val terminees = pistes.filter { it.faite }.sortedByDescending { it.cloture }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Journal de recherches", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CouleurVertFonce)
                    Text(
                        if (ouvertes.size < 5) {
                            "Compl\u00E9t\u00E9 automatiquement selon les informations manquantes."
                        } else {
                            "5 recherches en cours maximum : validez-en une pour en recevoir une nouvelle."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onNouvellePiste) {
                    Icon(Icons.Filled.Add, contentDescription = "Nouvelle recherche manuelle")
                }
            }
        }

        // --- Recherches ouvertes, numerotees 1 a 5
        ouvertes.forEachIndexed { index, piste ->
            item(key = "ouverte" + piste.id.toString()) {
                CartePiste(
                    piste = piste,
                    numero = index + 1,
                    personnes = personnes,
                    actes = actes,
                    onBasculer = onBasculer,
                    onSupprimer = onSupprimer,
                    onOuvrirPersonne = onOuvrirPersonne,
                    noterRecherche = noterRecherche,
                    ouvrirLien = ouvrirLien,
                    onSauver = onSauver
                )
            }
        }

        // --- Pistes ouvertes au-dela du quota de 5 (en attente)
        if (ouvertesManuelles.isNotEmpty()) {
            item {
                Text(
                    "En attente de place (validez une recherche ci-dessus) :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            ouvertesManuelles.take(3).forEach { piste ->
                item(key = "attente" + piste.id.toString()) {
                    Text(
                        "  \u2022 " + textePisteActuel(piste, personnes, actes),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // --- Recherches terminees (rappels compacts)
        if (terminees.isNotEmpty()) {
            item {
                Text(
                    "Recherches termin\u00E9es (" + terminees.size.toString() + ") \u2013 rappels",
                    fontWeight = FontWeight.Bold,
                    color = CouleurVertFonce,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
            terminees.take(15).forEach { piste ->
                item(key = "finie" + piste.id.toString()) {
                    TermineeCompacte(piste, personnes, actes, onOuvrirPersonne)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------- Carte d'une piste (4 blocs)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartePiste(
    piste: Piste,
    numero: Int,
    personnes: List<Personne>,
    actes: List<Acte>,
    onBasculer: (Piste) -> Unit,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    noterRecherche: (Piste) -> Unit,
    ouvrirLien: (String) -> Unit,
    onSauver: () -> Unit
) {
    val pers = personneParId(personnes, piste.personneId)
    var demandeConfirmation by remember { mutableStateOf(false) }
    var enCoursOuvert by remember { mutableStateOf(false) }

    val texte = textePisteActuel(piste, personnes, actes)

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Pastille numerotee
                Box(
                    Modifier.size(28.dp).background(CouleurVert, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(numero.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

            // Personne concernee
            if (pers != null) {
                TextButton(onClick = { onOuvrirPersonne(piste.personneId) }) {
                    Text("Fiche de " + titreCourt(pers), style = MaterialTheme.typography.labelMedium)
                }
            }

            // --- Bloc a. Recherche a faire
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { demandeConfirmation = true },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CouleurVert,
                        checkmarkColor = Color.White
                    )
                )
                Text(
                    "a. Recherche \u00E0 faire",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { noterRecherche(piste) }) {
                    Text("Noter un r\u00E9sultat")
                }
                TextButton(onClick = { enCoursOuvert = true }) {
                    Text("En cours")
                }
            }

            // --- Bloc b. Rappels horodates de ce qui a ete fait
            if (piste.historique.isNotEmpty()) {
                Text(
                    "b. Rappels :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                piste.historique.take(5).forEach { r ->
                    TexteAvecLiens(
                        "  \u2022 " + formaterDateHeure(r.date) + " \u2013 " + libelleRecherche(r),
                        ouvrirLien
                    )
                }
            }

            // --- Bloc c. Recherche en cours
            if (piste.enCours.isNotBlank()) {
                Text(
                    "c. Recherche en cours :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                TexteAvecLiens("  \u2022 " + piste.enCours, ouvrirLien)
                OutlinedButton(onClick = { enCoursOuvert = true }, Modifier.padding(start = 24.dp)) {
                    Icon(Icons.Filled.Create, contentDescription = null, Modifier.size(14.dp))
                    Text("  Basculer vers les rappels et vider")
                }
            }

            // --- Bloc d. Reste a rechercher pour cette personne
            if (pers != null) {
                val manquantes = suggestionsPour(pers, personnes, actes)
                if (manquantes.isNotEmpty()) {
                    Text(
                        "d. Reste \u00E0 rechercher pour " + titreCourt(pers) + " :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    manquantes.take(6).forEach { s ->
                        Text("  \u2022 " + s.titre, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Validation avec confirmation
    if (demandeConfirmation) {
        AlertDialog(
            onDismissRequest = { demandeConfirmation = false },
            title = { Text("Valider cette recherche ?") },
            text = {
                Text("Elle sera marqu\u00E9e comme termin\u00E9e et effac\u00E9e des 5 en cours. Une nouvelle recherche prendra sa place \u00E0 la sauvegarde suivante.")
            },
            confirmButton = {
                TextButton(onClick = {
                    demandeConfirmation = false
                    onBasculer(piste)
                }) { Text("Oui, termin\u00E9e") }
            },
            dismissButton = {
                TextButton(onClick = { demandeConfirmation = false }) { Text("Pas encore") }
            }
        )
    }

    if (enCoursOuvert) {
        DialogueEnCours(
            piste = piste,
            onSauver = onSauver,
            onFermer = { enCoursOuvert = false }
        )
    }
}

// ---------------------------------------------------------------- Recherche terminee, compacte

@Composable
fun TermineeCompacte(
    piste: Piste,
    personnes: List<Personne>,
    actes: List<Acte>,
    onOuvrirPersonne: (Long) -> Unit
) {
    val pers = personneParId(personnes, piste.personneId)
    val texte = textePisteActuel(piste, personnes, actes)
    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                texte,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pers != null) {
                TextButton(onClick = { onOuvrirPersonne(piste.personneId) }) {
                    Text("Fiche", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Dialogue : recherche en cours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueEnCours(
    piste: Piste,
    onSauver: () -> Unit,
    onFermer: () -> Unit
) {
    var texte by remember { mutableStateOf(piste.enCours) }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Recherche en cours") },
        text = {
            Column {
                Text(
                    "Ex : vu jusqu'\u00E0 la page 230 des TD de Roubaix, reprendre \u00E0 partir de 1887.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Ce qui est en cours") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    if (texte.trim().isNotEmpty()) {
                        piste.historique.add(0, Recherche(date = System.currentTimeMillis(), resultat = "X", note = texte.trim()))
                    }
                    piste.enCours = ""
                    onSauver()
                    onFermer()
                }) { Text("Basculer aux rappels") }
                TextButton(onClick = {
                    piste.enCours = texte
                    onSauver()
                    onFermer()
                }) { Text("Garder en cours") }
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Dialogue : noter un resultat
// Ouvert depuis MainActivity pour la piste en cours (historique horodate).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueRecherche(
    onValider: (Recherche) -> Unit,
    onAnnuler: () -> Unit
) {
    var rienTrouve by remember { mutableStateOf(true) }
    var trouve by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("") }
    var villeTd by remember { mutableStateOf("") }
    var anneeReprise by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Noter le r\u00E9sultat de la recherche") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rienTrouve,
                        onCheckedChange = { rienTrouve = it; if (it) trouve = false },
                        colors = CheckboxDefaults.colors(checkedColor = CouleurRouge)
                    )
                    Text("Rien trouv\u00E9 pour l'instant")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = trouve,
                        onCheckedChange = { trouve = it; if (it) rienTrouve = false },
                        colors = CheckboxDefaults.colors(checkedColor = CouleurVert)
                    )
                    Text("Trouv\u00E9 !")
                }
                if (rienTrouve) {
                    OutlinedTextField(
                        value = page,
                        onValueChange = { page = it },
                        label = { Text("Jusqu'\u00E0 la page ...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        value = villeTd,
                        onValueChange = { villeTd = it },
                        label = { Text("Tables d\u00E9cennales de (ville)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        value = anneeReprise,
                        onValueChange = { anneeReprise = it },
                        label = { Text("Reprendre \u00E0 partir de l'ann\u00E9e ...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (trouve) { "R\u00E9f\u00E9rence de l'acte, cote, URL..." } else { "Note libre" }) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValider(
                    Recherche(
                        date = System.currentTimeMillis(),
                        resultat = if (trouve) "T" else "R",
                        page = page.trim(),
                        repriseVille = villeTd.trim(),
                        repriseAnnee = anneeReprise.trim(),
                        note = note.trim()
                    )
                )
            }) { Text("Noter") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Dialogue : nouvelle piste manuelle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialoguePiste(
    personnes: List<Personne>,
    personnePredefinie: Long,
    onValider: (String, Long) -> Unit,
    onAnnuler: () -> Unit
) {
    var texte by remember { mutableStateOf("") }
    var personneId by remember { mutableStateOf(personnePredefinie) }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvelle recherche") },
        text = {
            Column {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Que faut-il rechercher ?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Personne concern\u00E9e :", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp)) {
                    TextButton(onClick = { personneId = 0L }) {
                        Text(if (personneId == 0L) { "\u25CF Aucune (g\u00E9n\u00E9rale)" } else { "   Aucune (g\u00E9n\u00E9rale)" })
                    }
                    personnes.sortedBy { it.titre() }.forEach { p ->
                        TextButton(onClick = { personneId = p.id }) {
                            Text(if (personneId == p.id) { "\u25CF " + p.titre() } else { "   " + p.titre() })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(texte.trim(), personneId) },
                enabled = texte.isNotBlank()
            ) { Text("Ajouter") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Texte avec URL cliquables

@Composable
fun TexteAvecLiens(texte: String, ouvrirLien: (String) -> Unit) {
    val motif = Regex("https?://\\S+|www\\.\\S+")
    val annote: AnnotatedString = buildAnnotatedString {
        append(texte)
        for (m in motif.findAll(texte)) {
            val deb = m.range.first
            val fin = m.range.last + 1
            addStyle(SpanStyle(color = CouleurVertFonce, textDecoration = TextDecoration.Underline), deb, fin)
            addStringAnnotation("lien", m.value, deb, fin)
        }
    }
    ClickableText(
        text = annote,
        style = MaterialTheme.typography.bodySmall,
        onClick = { offset ->
            annote.getStringAnnotations("lien", offset, offset).firstOrNull()?.let { a ->
                ouvrirLien(a.item)
            }
        }
    )
}
