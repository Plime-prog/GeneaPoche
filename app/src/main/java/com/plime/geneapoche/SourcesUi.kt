package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ================================================================ v0.92
// Carnet de sources : ecran des sources reutilisables, bloc "Sources" de la
// fiche, dialogue de citation, champs "source / vue" du formulaire d'acte.
// Tout est LOCAL ; les donnees vivent dans le catalogue chiffre (Sources.kt)
// et s'exportent en GEDCOM standard (SourcesGedcom.kt, compatible Heredis).

private fun rechercheSource(s: String): String = sansAccents(s).lowercase()

private fun resumeSource(s: Source): String {
    val l = mutableListOf<String>()
    val t = libelleTypeSource(s.type)
    if (t.isNotEmpty()) l.add(t)
    if (s.commune.isNotBlank()) l.add(s.commune.trim())
    if (s.periode.isNotBlank()) l.add(s.periode.trim())
    if (s.cote.isNotBlank()) l.add("cote " + s.cote.trim())
    return l.joinToString(" \u00b7 ")
}

private fun intOuZero(s: String): Int = s.trim().toIntOrNull() ?: 0

// ---------------------------------------------------------------- Ecran
@Composable
fun EcranSources(
    personnes: List<Personne>,
    sources: List<Source>,
    citations: List<Citation>,
    onCreerSource: (Source) -> Long,
    onEnregistrerSource: (Source) -> Unit,
    onSupprimerSource: (Long) -> Unit,
    onEnregistrerCitation: (Citation) -> Unit,
    onSupprimerCitation: (Long) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    onFermer: (() -> Unit)? = null
) {
    var recherche by remember { mutableStateOf("") }
    // 0 = liste ; -1 = nouvelle source ; sinon identifiant de la source ouverte.
    var ouverte by remember { mutableStateOf(0L) }
    // v0.95 (12) : plus de Dialog plein ecran : l'ecran s'affiche comme
    // les autres onglets, la barre de navigation reste visible.
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Carnet de sources", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (ouverte != 0L) {
                        TextButton(onClick = { ouverte = 0L }) {
                            Text("Retour")
                        }
                    }
                }
                if (ouverte == 0L) {
                    Text(
                        "Une source se d\u00e9crit une fois (d\u00e9p\u00f4t, cote, commune, p\u00e9riode) et se cite autant de fois que n\u00e9cessaire, pour chaque personne et chaque \u00e9v\u00e9nement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = recherche,
                            onValueChange = { recherche = it },
                            label = { Text("Rechercher") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { ouverte = -1L }) { Text("Nouvelle") }
                    }
                    Spacer(Modifier.height(4.dp))
                    val f = rechercheSource(recherche.trim())
                    val liste = sources
                        .filter {
                            f.isEmpty() || rechercheSource(
                                libelleSource(it) + " " + it.commune + " " + it.depot + " " + it.cote + " " + it.periode
                            ).contains(f)
                        }
                        .sortedBy { rechercheSource(libelleSource(it)) }
                    if (liste.isEmpty()) {
                        Text(
                            if (sources.isEmpty()) "Aucune source pour l'instant. Touchez \u00ab Nouvelle \u00bb pour en cr\u00e9er une."
                            else "Aucune source ne correspond."
                        )
                    }
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(liste, key = { it.id }) { s ->
                            val n = nombreCitations(citations, s.id)
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { ouverte = s.id },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFBDBDBD))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(6.dp)) {
                                    Text(libelleSource(s), fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                    val r = resumeSource(s)
                                    if (r.isNotEmpty()) Text(r, fontSize = 12.sp, color = Color(0xFF424242))
                                    Text(
                                        n.toString() + " citation(s)" +
                                            (if (s.fiabilite in 0..3) " \u00b7 fiabilit\u00e9 " + s.fiabilite.toString() + "/3" else ""),
                                        fontSize = 12.sp, color = Color(0xFF616161)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val existante = if (ouverte > 0L) sourceParId(sources, ouverte) else null
                    if (ouverte > 0L && existante == null) {
                        ouverte = 0L
                    } else {
                        FicheSource(
                            source = existante,
                            personnes = personnes,
                            citations = if (existante != null) citationsDeSource(citations, existante.id) else emptyList(),
                            sources = sources,
                            onEnregistrer = { s ->
                                if (s.id == 0L) {
                                    val nouvelId = onCreerSource(s)
                                    ouverte = if (nouvelId > 0L) nouvelId else 0L
                                } else {
                                    onEnregistrerSource(s)
                                }
                            },
                            onSupprimer = { id -> onSupprimerSource(id); ouverte = 0L },
                            onEnregistrerCitation = onEnregistrerCitation,
                            onSupprimerCitation = onSupprimerCitation,
                            onCreerSource = onCreerSource,
                            onOuvrirPersonne = { id -> onOuvrirPersonne(id) }
                        )
                    }
                }
            }
}

// ---------------------------------------------------------------- Fiche source
@Composable
private fun FicheSource(
    source: Source?,
    personnes: List<Personne>,
    citations: List<Citation>,
    sources: List<Source>,
    onEnregistrer: (Source) -> Unit,
    onSupprimer: (Long) -> Unit,
    onEnregistrerCitation: (Citation) -> Unit,
    onSupprimerCitation: (Long) -> Unit,
    onCreerSource: (Source) -> Long,
    onOuvrirPersonne: (Long) -> Unit
) {
    val id = source?.id ?: 0L
    var titre by remember(id) { mutableStateOf(source?.titre ?: "") }
    var type by remember(id) { mutableStateOf(source?.type ?: "") }
    var commune by remember(id) { mutableStateOf(source?.commune ?: "") }
    var periode by remember(id) { mutableStateOf(source?.periode ?: "") }
    var depot by remember(id) { mutableStateOf(source?.depot ?: "") }
    var cote by remember(id) { mutableStateOf(source?.cote ?: "") }
    var url by remember(id) { mutableStateOf(source?.url ?: "") }
    var transcription by remember(id) { mutableStateOf(source?.transcription ?: "") }
    var notes by remember(id) { mutableStateOf(source?.notes ?: "") }
    var fiabilite by remember(id) { mutableStateOf(source?.fiabilite ?: -1) }
    var confirmeSuppression by remember { mutableStateOf(false) }
    var choixPersonne by remember { mutableStateOf(false) }
    var personneChoisie by remember { mutableStateOf<Personne?>(null) }
    var citationEnEdition by remember { mutableStateOf<Citation?>(null) }

    fun construire(): Source {
        val s = Source(
            id = id, titre = titre.trim(), type = type, commune = commune.trim(),
            dept = if (commune.isBlank()) "" else deptDuLieu(commune.trim()).ifEmpty { source?.dept ?: "" },
            periode = periode.trim(), depot = depot.trim(), cote = cote.trim(), url = url.trim(),
            transcription = transcription.trim(), notes = notes.trim(), fiabilite = fiabilite
        )
        return s
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            if (source == null) "Nouvelle source" else "Source",
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = titre, onValueChange = { titre = it },
            label = { Text("Titre (r\u00e9f\u00e9rence compl\u00e8te)") },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { titre = titreSuggere(construire()) }) {
            Text("Sugg\u00e9rer le titre \u00e0 partir des champs", fontSize = 12.sp)
        }
        Text("Type de document", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            TypesSource.forEach { (code, lib) ->
                FilterChip(
                    selected = type == code,
                    onClick = { type = if (type == code) "" else code },
                    label = { Text(lib) }
                )
                Spacer(Modifier.width(6.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = commune, onValueChange = { commune = it },
                label = { Text("Commune") }, singleLine = true, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = periode, onValueChange = { periode = it },
                label = { Text("P\u00e9riode (1872, 1860-1872)") }, singleLine = true, modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = depot, onValueChange = { depot = it },
            label = { Text("D\u00e9p\u00f4t (Archives d\u00e9partementales du Cher...)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = cote, onValueChange = { cote = it },
            label = { Text("Cote (4 E 1234)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("Lien Internet (facultatif, jamais n\u00e9cessaire)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = transcription, onValueChange = { transcription = it },
            label = { Text("Transcription") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text("Niveau de fiabilit\u00e9 (par d\u00e9faut pour les citations)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelecteurFiabilite(fiabilite, "Non renseign\u00e9e") { fiabilite = it }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { onEnregistrer(construire()) },
            enabled = titre.isNotBlank() || depot.isNotBlank() || cote.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enregistrer la source") }

        if (source != null) {
            Spacer(Modifier.height(8.dp))
            Text("Personnes concern\u00e9es (" + citations.size.toString() + ")", fontWeight = FontWeight.Bold)
            if (citations.isEmpty()) {
                Text("Aucune citation : cette source n'est encore rattach\u00e9e \u00e0 personne.", fontSize = 12.sp)
            }
            citations.forEach { c ->
                val p = personneParId(personnes, c.personneId)
                if (p != null) {
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFBDBDBD))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(libelleCitation(c, p), fontSize = 13.sp)
                            val q = qualiteEffective(c, source)
                            if (q >= 0) Text("Fiabilit\u00e9 : " + libelleQualite(q), fontSize = 11.sp, color = Color(0xFF616161))
                            Row {
                                TextButton(onClick = { onOuvrirPersonne(p.id) }) { Text("Ouvrir la fiche", fontSize = 12.sp) }
                                TextButton(onClick = { citationEnEdition = c }) { Text("Modifier", fontSize = 12.sp) }
                                TextButton(onClick = { onSupprimerCitation(c.id) }) { Text("Retirer", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { choixPersonne = true }, Modifier.fillMaxWidth()) {
                Text("Ajouter une personne qui cite cette source")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirmeSuppression = true }) {
                Text("Supprimer cette source", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (choixPersonne) {
        DialogueChoixPersonne(
            titre = "Qui cite cette source ?",
            personnes = personnes,
            onChoisi = { personneChoisie = it; choixPersonne = false },
            onFermer = { choixPersonne = false }
        )
    }
    personneChoisie?.let { p ->
        DialogueCitation(
            personne = p,
            sources = sources,
            citation = null,
            sourceInitialeId = id,
            onEnregistrer = { c -> onEnregistrerCitation(c); personneChoisie = null },
            onCreerSource = onCreerSource,
            onFermer = { personneChoisie = null }
        )
    }
    citationEnEdition?.let { c ->
        val p = personneParId(personnes, c.personneId)
        if (p != null) {
            DialogueCitation(
                personne = p,
                sources = sources,
                citation = c,
                sourceInitialeId = c.sourceId,
                onEnregistrer = { nc -> onEnregistrerCitation(nc); citationEnEdition = null },
                onCreerSource = onCreerSource,
                onFermer = { citationEnEdition = null }
            )
        } else {
            citationEnEdition = null
        }
    }
    if (confirmeSuppression && source != null) {
        AlertDialog(
            onDismissRequest = { confirmeSuppression = false },
            title = { Text("Supprimer cette source ?") },
            text = {
                Text(
                    "Les " + citations.size.toString() + " citation(s) qui s'y rattachent seront supprim\u00e9es aussi. Les fiches des personnes ne sont pas touch\u00e9es."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmeSuppression = false; onSupprimer(source.id) }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmeSuppression = false }) { Text("Annuler") } }
        )
    }
}

// Choix du niveau de fiabilite (echelle QUAY 0-3) ; `neutre` = valeur -1.
@Composable
private fun SelecteurFiabilite(valeur: Int, neutre: String, onChoix: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        FilterChip(selected = valeur !in 0..3, onClick = { onChoix(-1) }, label = { Text(neutre) })
        for (n in 0..3) {
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = valeur == n, onClick = { onChoix(n) }, label = { Text(libelleQualite(n)) })
        }
    }
}

// ---------------------------------------------------------------- Citation
@Composable
fun DialogueCitation(
    personne: Personne,
    sources: List<Source>,
    citation: Citation?,
    sourceInitialeId: Long,
    cibleInitiale: String = "",
    onEnregistrer: (Citation) -> Unit,
    onCreerSource: (Source) -> Long,
    onFermer: () -> Unit
) {
    var sourceId by remember { mutableStateOf(sourceInitialeId) }
    var cible by remember { mutableStateOf(citation?.cible ?: cibleInitiale.ifEmpty { "P" }) }
    var numeroActe by remember { mutableStateOf(citation?.numeroActe ?: "") }
    var vue by remember { mutableStateOf(if ((citation?.vue ?: 0) > 0) citation!!.vue.toString() else "") }
    var vueTotal by remember { mutableStateOf(if ((citation?.vueTotal ?: 0) > 0) citation!!.vueTotal.toString() else "") }
    var page by remember { mutableStateOf(citation?.page ?: "") }
    var extrait by remember { mutableStateOf(citation?.extrait ?: "") }
    var notes by remember { mutableStateOf(citation?.notes ?: "") }
    var fiabilite by remember { mutableStateOf(citation?.fiabilite ?: -1) }
    var choixSource by remember { mutableStateOf(false) }
    val source = sourceParId(sources, sourceId)
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(if (citation == null) "Citer une source" else "Modifier la citation") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(personne.titre(), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { choixSource = true }, Modifier.fillMaxWidth()) {
                    Text(if (source != null) libelleSource(source) else "Choisir une source...")
                }
                Spacer(Modifier.height(4.dp))
                Text("Ce que la source documente", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    CiblesCitation.forEach { (code, lib) ->
                        FilterChip(selected = cible == code, onClick = { cible = code }, label = { Text(lib) })
                        Spacer(Modifier.width(6.dp))
                    }
                    personne.evenementsLibres.forEach { ev ->
                        val code = "E" + ev.id.toString()
                        FilterChip(
                            selected = cible == code, onClick = { cible = code },
                            label = { Text(ev.intitule.trim().ifEmpty { "\u00c9v\u00e9nement" }) }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = numeroActe, onValueChange = { numeroActe = it },
                    label = { Text("N\u00b0 d'acte") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = vue, onValueChange = { vue = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("Vue n\u00b0") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text("  sur  ")
                    OutlinedTextField(
                        value = vueTotal, onValueChange = { vueTotal = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("Total de vues") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = page, onValueChange = { page = it },
                    label = { Text("Autre pr\u00e9cision (f\u00b0, page...)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = extrait, onValueChange = { extrait = it },
                    label = { Text("Extrait / transcription de l'acte") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Fiabilit\u00e9 de cette citation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelecteurFiabilite(fiabilite, "Comme la source") { fiabilite = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = source != null,
                onClick = {
                    onEnregistrer(
                        Citation(
                            id = citation?.id ?: 0L, sourceId = sourceId, personneId = personne.id, cible = cible,
                            page = page.trim(), numeroActe = numeroActe.trim(),
                            vue = intOuZero(vue), vueTotal = intOuZero(vueTotal),
                            extrait = extrait.trim(), notes = notes.trim(), fiabilite = fiabilite
                        )
                    )
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
    if (choixSource) {
        DialogueChoixSource(
            sources = sources,
            onChoisie = { sourceId = it; choixSource = false },
            onCreerSource = { s -> val nid = onCreerSource(s); if (nid > 0L) { sourceId = nid }; choixSource = false },
            onFermer = { choixSource = false }
        )
    }
}

// Choix d'une source reutilisable, ou creation rapide d'une nouvelle.
@Composable
fun DialogueChoixSource(
    sources: List<Source>,
    onChoisie: (Long) -> Unit,
    onCreerSource: (Source) -> Unit,
    onFermer: () -> Unit
) {
    var filtre by remember { mutableStateOf("") }
    var nouvelle by remember { mutableStateOf(false) }
    val f = rechercheSource(filtre.trim())
    val liste = sources
        .filter { f.isEmpty() || rechercheSource(libelleSource(it) + " " + it.commune + " " + it.cote).contains(f) }
        .sortedBy { rechercheSource(libelleSource(it)) }
        .take(40)
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Choisir une source") },
        text = {
            Column {
                OutlinedTextField(
                    value = filtre, onValueChange = { filtre = it },
                    label = { Text("Rechercher") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    liste.forEach { s ->
                        Column(Modifier.fillMaxWidth().clickable { onChoisie(s.id) }.padding(vertical = 8.dp)) {
                            Text(libelleSource(s), fontSize = 14.sp)
                            val r = resumeSource(s)
                            if (r.isNotEmpty()) Text(r, fontSize = 11.sp, color = Color(0xFF616161))
                        }
                    }
                    if (liste.isEmpty()) Text("Aucune source.")
                }
            }
        },
        confirmButton = { TextButton(onClick = { nouvelle = true }) { Text("Nouvelle source") } },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
    if (nouvelle) {
        DialogueNouvelleSource(onCreer = { s -> nouvelle = false; onCreerSource(s) }, onFermer = { nouvelle = false })
    }
}

// Creation rapide d'une source (les autres champs se completent ensuite
// dans le Carnet de sources).
@Composable
private fun DialogueNouvelleSource(onCreer: (Source) -> Unit, onFermer: () -> Unit) {
    var titre by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var commune by remember { mutableStateOf("") }
    var periode by remember { mutableStateOf("") }
    var depot by remember { mutableStateOf("") }
    var cote by remember { mutableStateOf("") }
    fun brouillon() = Source(
        0L, titre.trim(), type, commune.trim(), "", periode.trim(), depot.trim(), cote.trim()
    )
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Nouvelle source") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    TypesSource.forEach { (code, lib) ->
                        FilterChip(selected = type == code, onClick = { type = if (type == code) "" else code }, label = { Text(lib) })
                        Spacer(Modifier.width(6.dp))
                    }
                }
                OutlinedTextField(value = commune, onValueChange = { commune = it }, label = { Text("Commune") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = periode, onValueChange = { periode = it }, label = { Text("P\u00e9riode") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = depot, onValueChange = { depot = it }, label = { Text("D\u00e9p\u00f4t") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cote, onValueChange = { cote = it }, label = { Text("Cote") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = titre, onValueChange = { titre = it }, label = { Text("Titre (sinon propos\u00e9)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = titre.isNotBlank() || depot.isNotBlank() || cote.isNotBlank() || commune.isNotBlank(),
                onClick = {
                    val b = brouillon()
                    if (b.titre.isEmpty()) b.titre = titreSuggere(b)
                    b.dept = if (b.commune.isBlank()) "" else deptDuLieu(b.commune)
                    onCreer(b)
                }
            ) { Text("Cr\u00e9er") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
}

// ---------------------------------------------------------------- Bloc fiche
@Composable
fun BlocSourcesFiche(
    p: Personne,
    personnes: List<Personne>,
    sources: List<Source>,
    citations: List<Citation>,
    onEnregistrerCitation: (Citation) -> Unit,
    onSupprimerCitation: (Long) -> Unit,
    onCreerSource: (Source) -> Long
) {
    val miennes = citationsDe(citations, p.id).filter { sourceParId(sources, it.sourceId) != null }
    var ouvert by remember(p.id) { mutableStateOf(false) }
    var ajout by remember { mutableStateOf(false) }
    var edition by remember { mutableStateOf<Citation?>(null) }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFBDBDBD))
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { ouvert = !ouvert },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sources (" + miennes.size.toString() + ")", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                Text(if (ouvert) "\u25b2" else "\u25bc", color = Color(0xFF1B5E20))
            }
            if (ouvert) {
                miennes.forEach { c ->
                    val s = sourceParId(sources, c.sourceId)
                    if (s != null) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(libelleCitation(c, null), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(libelleSource(s), fontSize = 12.sp, color = Color(0xFF424242))
                            val q = qualiteEffective(c, s)
                            if (q >= 0) Text("Fiabilit\u00e9 : " + libelleQualite(q), fontSize = 11.sp, color = Color(0xFF616161))
                            if (c.extrait.isNotBlank()) Text("\u00ab " + c.extrait.trim() + " \u00bb", fontSize = 11.sp, color = Color(0xFF616161))
                            Row {
                                TextButton(onClick = { edition = c }) { Text("Modifier", fontSize = 12.sp) }
                                TextButton(onClick = { onSupprimerCitation(c.id) }) { Text("Retirer", fontSize = 12.sp) }
                            }
                        }
                    }
                }
                if (miennes.isEmpty()) {
                    Text("Aucune source cit\u00e9e pour cette personne.", fontSize = 12.sp, color = Color(0xFF616161))
                }
                OutlinedButton(onClick = { ajout = true }, Modifier.fillMaxWidth()) { Text("Ajouter une source") }
            }
        }
    }
    if (ajout) {
        DialogueCitation(
            personne = p, sources = sources, citation = null, sourceInitialeId = 0L,
            onEnregistrer = { c -> onEnregistrerCitation(c); ajout = false },
            onCreerSource = onCreerSource, onFermer = { ajout = false }
        )
    }
    edition?.let { c ->
        DialogueCitation(
            personne = p, sources = sources, citation = c, sourceInitialeId = c.sourceId,
            onEnregistrer = { nc -> onEnregistrerCitation(nc); edition = null },
            onCreerSource = onCreerSource, onFermer = { edition = null }
        )
    }
}

// ---------------------------------------------------------------- Saisie d'acte
// Champs "source" du formulaire d'acte : la source reutilisable, le numero
// d'acte et la vue n sur le total de vues. Tout est facultatif.
@Composable
fun ChampsSourceActe(
    sources: List<Source>,
    sourceId: Long,
    onSource: (Long) -> Unit,
    numeroActe: String,
    onNumeroActe: (String) -> Unit,
    vue: String,
    onVue: (String) -> Unit,
    vueTotal: String,
    onVueTotal: (String) -> Unit,
    fiabilite: Int,
    onFiabilite: (Int) -> Unit,
    onCreerSource: (Source) -> Long
) {
    var choix by remember { mutableStateOf(false) }
    val source = sourceParId(sources, sourceId)
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text("Source de l'acte (facultatif)", fontSize = 12.sp, color = Color(0xFF424242))
        OutlinedButton(onClick = { choix = true }, Modifier.fillMaxWidth()) {
            Text(if (source != null) libelleSource(source) else "Choisir une source...")
        }
        if (source != null) {
            TextButton(onClick = { onSource(0L) }) { Text("Retirer la source", fontSize = 12.sp) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = numeroActe, onValueChange = onNumeroActe,
                    label = { Text("N\u00b0 d'acte") }, singleLine = true, modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vue, onValueChange = { onVue(it.filter { c -> c.isDigit() }.take(6)) },
                    label = { Text("Vue n\u00b0") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text("  sur  ")
                OutlinedTextField(
                    value = vueTotal, onValueChange = { onVueTotal(it.filter { c -> c.isDigit() }.take(6)) },
                    label = { Text("Total de vues") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Fiabilit\u00e9", fontSize = 12.sp, color = Color(0xFF424242))
            SelecteurFiabilite(fiabilite, "Comme la source", onFiabilite)
        }
    }
    if (choix) {
        DialogueChoixSource(
            sources = sources,
            onChoisie = { onSource(it); choix = false },
            onCreerSource = { s -> val nid = onCreerSource(s); if (nid > 0L) onSource(nid); choix = false },
            onFermer = { choix = false }
        )
    }
}
