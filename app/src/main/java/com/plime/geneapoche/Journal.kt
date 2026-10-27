package com.plime.geneapoche

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Journal

@Composable
fun EcranJournal(
    pistes: List<Piste>,
    personnes: List<Personne>,
    actes: List<Acte>,
    onBasculer: (Piste) -> Unit,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    noterRecherche: (Piste) -> Unit,
    noterSuggestion: (Personne, Suggestion) -> Unit,
    onNouvellePiste: () -> Unit
) {
    val ouvertes = pistes.filter { !it.faite }.sortedByDescending { it.creation }
    val terminees = pistes.filter { it.faite }.sortedByDescending { it.cloture }
    val suggerees = mutableListOf<Pair<Personne, Suggestion>>()
    personnes.forEach { p ->
        suggestionsPour(p, personnes, actes).forEach { s -> suggerees.add(p to s) }
    }

    LazyColumn(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Journal de recherches", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Jusqu'\u00E0 5 recherches sugg\u00E9r\u00E9es par personne, tant qu'une information manque ou n'est pas pass\u00E9e au vert.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (suggerees.isEmpty()) {
            item {
                Text(
                    "Aucune recherche sugg\u00E9r\u00E9e : tout est renseign\u00E9 et v\u00E9rifi\u00E9.",
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            item {
                Text(
                    "Recherches sugg\u00E9r\u00E9es",
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(suggerees) { (p, s) ->
                CarteSuggestion(
                    p = p,
                    s = s,
                    onNoter = { noterSuggestion(p, s) },
                    onOuvrirPersonne = { onOuvrirPersonne(p.id) }
                )
            }
        }
        item {
            Text(
                "Pistes en cours (" + ouvertes.size.toString() + ")",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (ouvertes.isEmpty()) {
            item {
                Text(
                    "Aucune piste en cours.",
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        items(ouvertes) { piste ->
            CartePiste(
                piste = piste,
                personnes = personnes,
                onBasculer = { onBasculer(piste) },
                onSupprimer = { onSupprimer(piste) },
                onOuvrirPersonne = onOuvrirPersonne,
                noterRecherche = noterRecherche
            )
        }
        item {
            Text(
                "Pistes termin\u00E9es (" + terminees.size.toString() + ")",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (terminees.isEmpty()) {
            item {
                Text(
                    "Aucune piste termin\u00E9e.",
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        items(terminees) { piste ->
            CartePiste(
                piste = piste,
                personnes = personnes,
                onBasculer = { onBasculer(piste) },
                onSupprimer = { onSupprimer(piste) },
                onOuvrirPersonne = onOuvrirPersonne,
                noterRecherche = noterRecherche
            )
        }
        item {
            OutlinedButton(
                onClick = onNouvellePiste,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Nouvelle piste libre")
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ------------------------------------------------------------ Moteur de suggestions

data class Suggestion(
    val categorie: String, // "N", "M", "D", "P" (pere), "ME" (mere)
    val titre: String,
    val lieu: String,
    val debut: String,
    val fin: String
)

fun libelleSuggestion(c: String): String = when (c) {
    "N" -> "Acte de naissance"
    "M" -> "Acte de mariage"
    "D" -> "Acte de d\u00E9c\u00E8s"
    "P" -> "P\u00E8re"
    "ME" -> "M\u00E8re"
    else -> "Recherche"
}

fun anneeDe(s: String): Int? =
    Regex("[0-9]{4}").find(s.trim())?.value?.toIntOrNull()

fun textePlage(debut: String, fin: String): String = when {
    debut.isNotEmpty() && fin.isNotEmpty() -> "entre " + debut + " et " + fin
    debut.isNotEmpty() -> "\u00E0 partir de " + debut
    fin.isNotEmpty() -> "jusqu'en " + fin
    else -> "inconnue"
}

// Regles intelligentes : plage d'annees deduite de la naissance connue
// (pere ne entre 20 et 45 ans plus tot, mere entre 15 et 40 ans plus tot),
// lieu pressenti = dernier lieu connu, et "pas d'acte enregistre -> recherche".
fun suggestionsPour(p: Personne, personnes: List<Personne>, actes: List<Acte>): List<Suggestion> {
    val res = mutableListOf<Suggestion>()
    val nAn = anneeDe(p.dateNaissance)
    val lieuN = p.lieuNaissance.trim()
    val lieuM = p.lieuMariage.trim()
    val lieuD = p.lieuDeces.trim()
    val lieuRef = when {
        lieuN.isNotEmpty() -> lieuN
        lieuM.isNotEmpty() -> lieuM
        else -> lieuD
    }
    val nomComplet = p.titre()
    fun plage(deltaDebut: Int, deltaFin: Int): Pair<String, String> =
        if (nAn != null) (nAn + deltaDebut).toString() to (nAn + deltaFin).toString() else "" to ""

    val acteN = actes.any { it.personneId == p.id && it.type == "N" }
    if (!acteN || p.dateNaissance.isBlank() || lieuN.isEmpty()) {
        res.add(Suggestion("N", "Rechercher l'acte de naissance de " + nomComplet, lieuRef, nAn?.toString() ?: "", ""))
    }
    if (p.pere == 0L) {
        val pl = plage(-45, -20)
        res.add(Suggestion("P", "Rechercher le p\u00E8re de " + nomComplet + " (n\u00E9 entre 20 et 45 ans avant lui)", lieuRef, pl.first, pl.second))
    }
    if (p.mere == 0L) {
        val pl = plage(-40, -15)
        res.add(Suggestion("ME", "Rechercher la m\u00E8re de " + nomComplet + " (n\u00E9e entre 15 et 40 ans avant elle)", lieuRef, pl.first, pl.second))
    }
    if (p.conjoint != 0L) {
        val conjoint = personnes.firstOrNull { it.id == p.conjoint }
        val nAnC = if (conjoint != null) anneeDe(conjoint.dateNaissance) else null
        val debut = when {
            nAn != null -> (nAn + 15).toString()
            nAnC != null -> (nAnC + 15).toString()
            else -> ""
        }
        val fin = when {
            nAnC != null -> (nAnC + 50).toString()
            nAn != null -> (nAn + 50).toString()
            else -> ""
        }
        val lieuConjoint = if (conjoint != null) conjoint.lieuNaissance.trim() else ""
        val lieuMariage = when {
            lieuN.isNotEmpty() -> lieuN
            lieuM.isNotEmpty() -> lieuM
            lieuConjoint.isNotEmpty() -> lieuConjoint
            else -> ""
        }
        val acteM = actes.any { it.personneId == p.id && it.type == "M" }
        if (p.dateMariage.isBlank() || lieuM.isEmpty() || !acteM) {
            res.add(Suggestion("M", "Rechercher le mariage de " + nomComplet, lieuMariage, debut, fin))
        }
    }
    val acteD = actes.any { it.personneId == p.id && it.type == "D" }
    if (p.dateDeces.isBlank() || lieuD.isEmpty() || !acteD) {
        val pl = plage(40, 110)
        val lieuDeces = if (lieuD.isNotEmpty()) lieuD else lieuRef
        res.add(Suggestion("D", "Rechercher l'acte de d\u00E9c\u00E8s de " + nomComplet, lieuDeces, pl.first, pl.second))
    }
    return res.take(5)
}

fun texteRecherche(r: Recherche): String {
    val b = StringBuilder()
    b.append(formaterDate(r.date))
    b.append(" : ")
    b.append(if (r.resultat == "T") "trouv\u00E9" else "rien trouv\u00E9")
    if (r.page.isNotEmpty()) b.append(", jusqu'\u00E0 la page ").append(r.page)
    if (r.repriseVille.isNotEmpty() || r.repriseAnnee.isNotEmpty()) {
        b.append(". Reprendre les TD")
        if (r.repriseVille.isNotEmpty()) b.append(" de ").append(r.repriseVille)
        if (r.repriseAnnee.isNotEmpty()) b.append(" \u00E0 partir de ").append(r.repriseAnnee)
    }
    if (r.note.isNotEmpty()) b.append(". ").append(r.note)
    return b.toString()
}

@Composable
fun CarteSuggestion(
    p: Personne,
    s: Suggestion,
    onNoter: () -> Unit,
    onOuvrirPersonne: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onNoter() }
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    libelleSuggestion(s.categorie),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    p.titre(),
                    Modifier.clickable { onOuvrirPersonne() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(s.titre, style = MaterialTheme.typography.bodyMedium)
            val lignes = mutableListOf<String>()
            if (s.lieu.isNotEmpty()) lignes.add("Lieu pressenti : " + s.lieu)
            if (s.debut.isNotEmpty() || s.fin.isNotEmpty()) lignes.add("P\u00E9riode : " + textePlage(s.debut, s.fin))
            lignes.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onNoter) { Text("Noter une recherche") }
        }
    }
}

@Composable
fun CartePiste(
    piste: Piste,
    personnes: List<Personne>,
    onBasculer: () -> Unit,
    onSupprimer: () -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    noterRecherche: (Piste) -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 4.dp, end = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = piste.faite,
                    onCheckedChange = { onBasculer() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CouleurVert,
                        checkmarkColor = Color.White
                    )
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        piste.texte,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (piste.faite) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                    )
                    val c = personneParId(personnes, piste.personneId)
                    if (c != null) {
                        Text(
                            "\u2192 " + c.titre(),
                            Modifier.clickable { onOuvrirPersonne(piste.personneId) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val details = mutableListOf<String>()
                    if (piste.lieu.isNotEmpty()) details.add("lieu : " + piste.lieu)
                    if (piste.debut.isNotEmpty() || piste.fin.isNotEmpty()) {
                        details.add("p\u00E9riode : " + textePlage(piste.debut, piste.fin))
                    }
                    if (details.isNotEmpty()) {
                        Text(details.joinToString(" \u2013 "), style = MaterialTheme.typography.bodySmall)
                    }
                    val dates = mutableListOf<String>()
                    dates.add("cr\u00E9\u00E9e le " + formaterDate(piste.creation))
                    if (piste.faite) dates.add("faite le " + formaterDate(piste.cloture))
                    Text(dates.joinToString(" \u2013 "), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onSupprimer) {
                    Icon(Icons.Filled.Close, contentDescription = "Supprimer")
                }
            }
            piste.historique.forEach { r ->
                Text(
                    texteRecherche(r),
                    Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = { noterRecherche(piste) }, Modifier.padding(start = 8.dp)) {
                Text("Noter une recherche")
            }
        }
    }
}

@Composable
fun DialogueRecherche(
    onValider: (Recherche) -> Unit,
    onAnnuler: () -> Unit
) {
    var resultat by remember { mutableStateOf("R") }
    var page by remember { mutableStateOf("") }
    var ville by remember { mutableStateOf("") }
    var annee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Recherche aux archives") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Recherch\u00E9 le " + formaterDate(System.currentTimeMillis()), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = resultat == "R", onClick = { resultat = "R" }, label = { Text("Rien trouv\u00E9") })
                    FilterChip(selected = resultat == "T", onClick = { resultat = "T" }, label = { Text("Trouv\u00E9") })
                }
                OutlinedTextField(
                    value = page,
                    onValueChange = { page = it },
                    label = { Text("Derni\u00E8re page consult\u00E9e (facultatif)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text("Pour reprendre plus tard :", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = ville,
                    onValueChange = { ville = it },
                    label = { Text("Ville des tables d\u00E9cennales") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
                OutlinedTextField(
                    value = annee,
                    onValueChange = { annee = it },
                    label = { Text("\u00C0 partir de l'ann\u00E9e") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note libre (facultatif)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValider(
                    Recherche(
                        date = System.currentTimeMillis(),
                        resultat = resultat,
                        page = page.trim(),
                        repriseVille = ville.trim(),
                        repriseAnnee = annee.trim(),
                        note = note.trim()
                    )
                )
            }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

@Composable
fun DialoguePiste(
    personnes: List<Personne>,
    personnePredefinie: Long,
    onValider: (String, Long) -> Unit,
    onAnnuler: () -> Unit
) {
    var texte by remember { mutableStateOf("") }
    var personneId by remember { mutableStateOf(personnePredefinie) }
    var pickerOuvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvelle piste de recherche") },
        text = {
            Column {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Quoi rechercher ? (acte, ann\u00E9e, lieu...)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                val liee = personneParId(personnes, personneId)
                OutlinedButton(onClick = { pickerOuvert = true }, Modifier.padding(top = 8.dp)) {
                    Text(
                        if (liee == null) "Lier une personne (facultatif)"
                        else "Personne : " + liee.titre()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(texte.trim(), personneId) },
                enabled = texte.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )

    if (pickerOuvert) {
        AlertDialog(
            onDismissRequest = { pickerOuvert = false },
            title = { Text("Personne concern\u00E9e") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { personneId = 0L; pickerOuvert = false }) {
                        Text("Aucune personne")
                    }
                    personnes.sortedBy { it.titre() }.forEach { c ->
                        TextButton(onClick = { personneId = c.id; pickerOuvert = false }) {
                            Text(c.titre())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerOuvert = false }) { Text("Fermer") }
            }
        )
    }
}

