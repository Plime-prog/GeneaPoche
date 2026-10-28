package com.plime.geneapoche

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Donnees des pistes

data class Suggestion(
    val cle: String,       // cle technique
    val libelle: String,   // texte complet affichable
    val type: String,      // code evenement : N B M D S P(pere) ME(mere) A(autre)
    val categorie: String  // libelle court pour le dialogue de resultat
)

fun dateVersAnnee(date: String): Int? {
    val m = Regex("[0-9]{4}").find(date) ?: return null
    return m.value.toIntOrNull()
}

fun titreCourt(p: Personne): String {
    val t = p.titre().trim()
    return if (t.isEmpty()) "(sans nom)" else t
}

// Toutes les pistes deduites de ce qui est deja saisi et verifie pour une personne.
fun suggestionsPour(p: Personne, personnes: List<Personne>, actes: List<Acte>): List<Suggestion> {
    val res = mutableListOf<Suggestion>()
    fun anneeDe(s: String): Int? = dateVersAnnee(s)

    // --- Evenements de la personne
    if (p.dateNaissance.isEmpty() || p.etatDe("naissance") != "vert") {
        res.add(Suggestion("naissance", "Rechercher l'acte de naissance de " + titreCourt(p) + (if (p.lieuNaissance.isNotEmpty()) { " \u00E0 " + p.lieuNaissance } else {""}) + (anneeDe(p.dateNaissance)?.let { " (" + it.toString() + ")" } ?: ""), "N", "naissance"))
    }
    if (p.dateBapteme.isEmpty() || p.etatDe("bapteme") != "vert") {
        res.add(Suggestion("bapteme", "Rechercher le bapt\u00EAme de " + titreCourt(p), "B", "bapt\u00EAme"))
    }
    if (p.dateMariage.isEmpty() || p.etatDe("mariage") != "vert") {
        res.add(Suggestion("mariage", "Rechercher le mariage de " + titreCourt(p), "M", "mariage"))
    }
    if (p.dateDeces.isEmpty() || p.etatDe("deces") != "vert") {
        res.add(Suggestion("deces", "Rechercher l'acte de d\u00E9c\u00E8s de " + titreCourt(p), "D", "d\u00E9c\u00E8s"))
    }
    if (p.dateSepulture.isEmpty() || p.etatDe("sepulture") != "vert") {
        res.add(Suggestion("sepulture", "Rechercher la s\u00E9pulture de " + titreCourt(p), "S", "s\u00E9pulture"))
    }

    // --- Parents deduits : plages d'annees
    val anneeN = anneeDe(p.dateNaissance)
    if (p.pere <= 0L && p.nom.isNotEmpty()) {
        val intitule = "Rechercher son p\u00E8re : un " + p.nom.trim() + (anneeN?.let { a -> " n\u00E9 entre " + (a - 45).toString() + " et " + (a - 20).toString() } ?: "") + (if (p.lieuNaissance.isNotEmpty()) { " \u00E0 " + p.lieuNaissance } else {""})
        res.add(Suggestion("pere", intitule, "P", "naissance du p\u00E8re"))
    }
    if (p.mere <= 0L) {
        val intitule = "Rechercher sa m\u00E8re" + (anneeN?.let { a -> " : n\u00E9e entre " + (a - 40).toString() + " et " + (a - 15).toString() } ?: "") + (if (p.lieuNaissance.isNotEmpty()) { " \u00E0 " + p.lieuNaissance } else {""})
        res.add(Suggestion("mere", intitule, "ME", "naissance de la m\u00E8re"))
    }

    // --- Conjoints / enfants
    if (p.conjoint <= 0L && p.conjointsSupp.isEmpty()) {
        res.add(Suggestion("conjoint", "Rechercher le conjoint de " + titreCourt(p), "C", "mariage (conjoint)"))
    }
    if (p.enfants.isEmpty()) {
        val anneeM = anneeDe(p.dateMariage)
        res.add(Suggestion("enfants", "Rechercher les enfants de " + titreCourt(p) + (anneeM?.let { m -> " apr\u00E8s " + m.toString() } ?: "") + (anneeM?.let { m -> " (entre " + m.toString() + " et " + (m + 25).toString() + ")" } ?: ""), "A", "enfants"))
    }

    // --- Arriere-plongee : si le mariage est connu, le conjoint a des parents
    if (p.pere > 0L) {
        val per = personneParId(personnes, p.pere)
        if (per != null) {
            if (per.dateNaissance.isEmpty() || per.etatDe("naissance") != "vert") {
                res.add(Suggestion("pere_naissance", "Rechercher l'acte de naissance du p\u00E8re : " + titreCourt(per), "N", "naissance"))
            }
            if (per.pere <= 0L && per.nom.isNotEmpty()) {
                val gp = anneeDe(per.dateNaissance)
                res.add(Suggestion("pere_pere", "Rechercher le grand-p\u00E8re paternel : un " + per.nom.trim() + (gp?.let { a -> " n\u00E9 entre " + (a - 45).toString() + " et " + (a - 20).toString() } ?: ""), "P", "naissance du grand-p\u00E8re paternel"))
            }
            if (per.mere <= 0L) {
                val gm = anneeDe(per.dateNaissance)
                res.add(Suggestion("pere_mere", "Rechercher la grand-m\u00E8re paternelle" + (gm?.let { a -> " : n\u00E9e entre " + (a - 40).toString() + " et " + (a - 15).toString() } ?: ""), "ME", "naissance de la grand-m\u00E8re paternelle"))
            }
        }
    }
    if (p.mere > 0L) {
        val mer = personneParId(personnes, p.mere)
        if (mer != null && (mer.dateNaissance.isEmpty() || mer.etatDe("naissance") != "vert")) {
            res.add(Suggestion("mere_naissance", "Rechercher l'acte de naissance de la m\u00E8re : " + titreCourt(mer), "N", "naissance"))
        }
    }
    return res
}

// Recalcule le texte d'une piste automatique a partir de l'etat actuel des donnees.
fun textePisteActuel(piste: Piste, personnes: List<Personne>, actes: List<Acte>): String {
    val pers = personneParId(personnes, piste.personneId) ?: return piste.texte
    val suggestion = suggestionsPour(pers, personnes, actes).firstOrNull { it.cle == piste.categorie }
    return suggestion?.libelle ?: piste.texte
}

fun horodatage(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())

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
    noterRecherche: (Piste, Recherche) -> Unit,
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
                        if (ouvertes.size < 5) { "Compl\u00E9t\u00E9 automatiquement selon les informations manquantes." } else { "5 recherches en cours maximum : validez-en une pour en recevoir une nouvelle." },
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

        // --- Recherches terminees (compactes, rappels)
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
    noterRecherche: (Piste, Recherche) -> Unit,
    ouvrirLien: (String) -> Unit,
    onSauver: () -> Unit
) {
    val pers = personneParId(personnes, piste.personneId)
    var demandeConfirmation by remember { mutableStateOf(false) }
    var resultatOuvert by remember { mutableStateOf(false) }
    var enCoursOuvert by remember { mutableStateOf(false) }

    val texte = if (piste.type == "A" && piste.categorie.isNotEmpty()) {
        textePisteActuel(piste, personnes, actes)
    } else {
        piste.texte
    }

    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                TextButton(onClick = { resultatOuvert = true }) {
                    Text("Noter un r\u00E9sultat")
                }
                TextButton(onClick = { enCoursOuvert = true }) {
                    Text("En cours")
                }
            }

            // --- Bloc b. Rappels de ce qui a ete fait (horodatage)
            if (piste.historique.isNotEmpty()) {
                Text(
                    "b. Rappels :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                piste.historique.forEach { r ->
                    TexteAvecLiens(
                        "  \u2022 " + r.jour + " \u2013 " + libelleRecherche(r),
                        ouvrirLien
                    )
                }
            }

            // --- Bloc c. Recherche en cours
            if (piste.encours.isNotEmpty()) {
                Text(
                    "c. Recherche en cours :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                TexteAvecLiens("  \u2022 " + piste.encours, ouvrirLien)
                OutlinedButton(onClick = { enCoursOuvert = true }, Modifier.padding(start = 24.dp)) {
                    Icon(Icons.Filled.Create, contentDescription = null, Modifier.size(14.dp))
                    Text("  Basculer vers les rappels et vider")
                }
            }

            // --- Bloc d. Reste a chercher
            if (pers != null) {
                val manquantes = suggestionsPour(pers, personnes, actes)
                if (manquantes.isNotEmpty()) {
                    Text(
                        "d. Reste \u00E0 rechercher pour " + titreCourt(pers) + " :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    manquantes.take(6).forEach { s ->
                        Text("  \u2022 " + s.libelle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    // Confirmation de validation
    if (demandeConfirmation) {
        AlertDialog(
            onDismissRequest = { demandeConfirmation = false },
            title = { Text("Valider cette recherche ?") },
            text = { Text("Elle sera marqu\u00E9e comme termin\u00E9e et effac\u00E9e des 5 en cours apr\u00E8s confirmation. Une nouvelle recherche prendra sa place \u00E0 la g\u00E9n\u00E9ration suivante.") },
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

    // Noter un resultat de recherche
    if (resultatOuvert) {
        DialogueRecherche(
            categorie = if (piste.categorie.isNotEmpty()) piste.categorie else "",
            personneLiee = pers,
            onNoter = { r ->
                noterRecherche(piste, r)
                resultatOuvert = false
            },
            onAnnuler = { resultatOuvert = false }
        )
    }

    // Modifier / basculer la recherche en cours
    if (enCoursOuvert) {
        DialogueEnCours(
            piste = piste,
            onSauver = onSauver,
            onFermer = { enCoursOuvert = false }
        )
    }
}

fun libelleRecherche(r: Recherche): String {
    return when (r.resultat) {
        "T" -> "TROUV\u00C9 \u2013 " + r.detail
        "X" -> r.detail
        else -> "Rien trouv\u00E9" +
            (if (r.numPage.isNotEmpty()) { " jusqu'\u00E0 la page " + r.numPage } else {""}) +
            (if (r.villeTd.isNotEmpty()) { " des tables d\u00E9cennales de " + r.villeTd } else {""}) +
            (if (r.anneeReprise.isNotEmpty()) { ", reprendre \u00E0 partir de " + r.anneeReprise } else {""}) +
            (if (r.detail.isNotEmpty()) { " \u2013 " + r.detail } else {""})
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
    val texte = if (piste.type == "A" && piste.categorie.isNotEmpty()) {
        textePisteActuel(piste, personnes, actes)
    } else {
        piste.texte
    }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
}

// ---------------------------------------------------------------- Dialogue recherche en cours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueEnCours(
    piste: Piste,
    onSauver: () -> Unit,
    onFermer: () -> Unit
) {
    var texte by remember { mutableStateOf(piste.encours) }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Recherche en cours") },
        text = {
            Column {
                Text("Ex : vu jusqu'\u00E0 la page 230 des TD de Roubaix, reprendre \u00E0 partir de 1887.", style = MaterialTheme.typography.bodySmall)
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
                        piste.historique.add(0, Recherche(horodatage(), "X", texte.trim()))
                    }
                    piste.encours = ""
                    onSauver()
                    onFermer()
                }) { Text("Basculer aux rappels") }
                TextButton(onClick = {
                    piste.encours = texte
                    onSauver()
                    onFermer()
                }) { Text("Enregistrer en cours") }
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Dialogue : noter un resultat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueRecherche(
    categorie: String,
    personneLiee: Personne?,
    onNoter: (Recherche) -> Unit,
    onAnnuler: () -> Unit
) {
    var rienTrouve by remember { mutableStateOf(true) }
    var trouve by remember { mutableStateOf(false) }
    var numPage by remember { mutableStateOf("") }
    var villeTd by remember { mutableStateOf(personneLiee?.lieuNaissance ?: "") }
    var anneeReprise by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Noter le r\u00E9sultat" + (if (categorie.isNotEmpty()) { " \u2013 " + categorie } else {""})) },
        text = {
            Column(Modifier.verticalScrollAble()) {
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
                        value = numPage,
                        onValueChange = { numPage = it },
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
                if (trouve) {
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it },
                        label = { Text("R\u00E9f\u00E9rence de l'acte, cote, URL...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Note libre") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val resultat = if (trouve) "T" else "R"
                onNoter(
                    Recherche(
                        jour = horodatage(),
                        resultat = resultat,
                        numPage = numPage.trim(),
                        villeTd = villeTd.trim(),
                        anneeReprise = anneeReprise.trim(),
                        detail = detail.trim()
                    )
                )
            }) { Text("Noter") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

fun Modifier.verticalScrollAble(): Modifier = this.then(
    androidx.compose.foundation.verticalScroll(androidx.compose.foundation.rememberScrollState())
)

// ---------------------------------------------------------------- Texte avec URL cliquables

@Composable
fun TexteAvecLiens(texte: String, ouvrirLien: (String) -> Unit) {
    val motif = Regex("https?://\\S+|www\\.\\S+")
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annote: AnnotatedString = buildAnnotatedString {
        append(texte)
        for (m in motif.findAll(texte)) {
            val deb = m.range.first
            val fin = m.range.last + 1
            addStyle(SpanStyle(color = CouleurVertFonce, textDecoration = TextDecoration.Underline), deb, fin)
            addStringAnnotation("lien", m.value, deb, fin)
        }
    }
    androidx.compose.foundation.text.ClickableText(
        text = annote,
        style = MaterialTheme.typography.bodySmall,
        onTextLayout = { layout = it },
        onClick = { offset ->
            annote.getStringAnnotations("lien", offset, offset).firstOrNull()?.let { a ->
                ouvrirLien(a.item)
            }
        }
    )
}
