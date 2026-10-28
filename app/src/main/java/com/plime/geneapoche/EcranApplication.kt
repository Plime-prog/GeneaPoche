package com.plime.geneapoche

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ Application deverrouillee

private val PlafondPersonnes = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranApplication(
    personnes: List<Personne>,
    pistes: List<Piste>,
    charge: Boolean,
    message: String,
    onMessage: (String) -> Unit,
    idOuvert: Long,
    onIdOuvert: (Long) -> Unit,
    onglet: Int,
    onOnglet: (Int) -> Unit,
    editionOuverte: Boolean,
    onEditionOuverte: (Boolean) -> Unit,
    editionCible: Personne?,
    onEditionCible: (Personne?) -> Unit,
    pickerRole: String,
    onPickerRole: (String) -> Unit,
    confirmSuppression: Personne?,
    onConfirmSuppression: (Personne?) -> Unit,
    enregistrer: () -> Unit,
    supprimer: (Personne) -> Unit,
    lier: (String, Long) -> Unit,
    delier: (String, Long) -> Unit,
    definirCujus: (Personne) -> Unit,
    renumeroter: (Long) -> Unit,
    dialoguePiste: Boolean,
    onDialoguePiste: (Boolean) -> Unit,
    pistePersonnePre: Long,
    onPistePersonnePre: (Long) -> Unit,
    ajouterPiste: (String, Long) -> Unit,
    basculerPiste: (Piste) -> Unit,
    supprimerPiste: (Piste) -> Unit,
    actes: List<Acte>,
    ajouterActe: (Long, Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    noterRecherche: (Piste) -> Unit,
    arbreCentre: Long,
    onArbreCentre: (Long) -> Unit,
    exporterGedcom: (Uri, Set<Long>?) -> Unit,
    importerGedcom: (Uri) -> Unit,
    ouvrirLien: (String) -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit
) {
    BackHandler(enabled = idOuvert != 0L || editionOuverte || pickerRole.isNotEmpty() || dialoguePiste) {
        when {
            dialoguePiste -> onDialoguePiste(false)
            pickerRole.isNotEmpty() -> onPickerRole("")
            editionOuverte -> onEditionOuverte(false)
            else -> onIdOuvert(0L)
        }
    }

    val ouverte = personneParId(personnes, idOuvert)

    if (ouverte == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("G\u00E9n\u00E9aPoche", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Ma g\u00E9n\u00E9alogie dans ma poche",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD8EBD4)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PaletteGeneaPoche.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = onglet == 0,
                        onClick = { onOnglet(0) },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Personnes") }
                    )
                    NavigationBarItem(
                        selected = onglet == 1,
                        onClick = { onOnglet(1) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("Journal") }
                    )
                    NavigationBarItem(
                        selected = onglet == 2,
                        onClick = { onOnglet(2) },
                        icon = { Icon(painterResource(R.drawable.ic_arbre), contentDescription = null) },
                        label = { Text("Arbre") }
                    )
                }
            },
            floatingActionButton = {
                if (onglet == 0) {
                    FloatingActionButton(onClick = { onEditionCible(null); onEditionOuverte(true) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                    }
                } else if (onglet == 1) {
                    FloatingActionButton(onClick = { onPistePersonnePre(0L); onDialoguePiste(true) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nouvelle piste")
                    }
                }
            }
        ) { marges ->
            var exportDialogue by remember { mutableStateOf(false) }
            var exportBrancheId by remember { mutableStateOf(0L) }
            var exportTout by remember { mutableStateOf(true) }
            var pickerBranche by remember { mutableStateOf(false) }

            val lanceurGedcom = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) exporterGedcom(uri, null)
            }
            val lanceurGedcomBranche = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) {
                    val sel = exportBrancheId
                    exporterGedcom(uri, if (sel != 0L) brancheDe(personnes, sel) else null)
                }
            }
            val lanceurImport = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) importerGedcom(uri)
            }
            if (!charge) {
                Column(Modifier.fillMaxSize().padding(marges).padding(16.dp)) {
                    Text("Chargement...")
                }
            } else if (onglet == 0) {
                if (personnes.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(marges).padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Aucune personne pour le moment.")
                        Text("Touchez le bouton + pour ajouter la premi\u00E8re personne (de cujus, Sosa 1).")
                    }
                } else {
                    val triees = personnes.sortedWith(
                        compareBy(
                            { it.nom.trim().uppercase() },
                            { it.prenoms.trim().uppercase() },
                            { anneeNaissance(it) }
                        )
                    )
                    LazyColumn(Modifier.fillMaxSize().padding(marges).padding(vertical = 4.dp)) {
                        item {
                            Text(
                                personnes.size.toString() + " / " + PlafondPersonnes.toString() + " personnes",
                                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(triees) { p ->
                            CartePersonne(
                                p,
                                onOuvrir = { onIdOuvert(p.id) },
                                onSupprimer = { onConfirmSuppression(p) }
                            )
                        }
                        item {
                            OutlinedButton(
                                onClick = { exportDialogue = true },
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("Exporter en GEDCOM (Heredis)")
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { lanceurImport.launch(arrayOf("*/*")) },
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text("Importer un GEDCOM (remplace la base)")
                            }
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            } else if (onglet == 2) {
                EcranArbre(
                    personnes = personnes,
                    idCentre = arbreCentre,
                    onOuvrir = { onIdOuvert(it); onArbreCentre(it) },
                    onCentrerSur = { onArbreCentre(it) }
                )
            } else {
                EcranJournal(
                    pistes = pistes,
                    personnes = personnes,
                    actes = actes,
                    onBasculer = { basculerPiste(it) },
                    onSupprimer = { supprimerPiste(it) },
                    onOuvrirPersonne = { onIdOuvert(it) },
                    noterRecherche = { noterRecherche(it) },
                    onNouvellePiste = { onDialoguePiste(true) },
                    ouvrirLien = ouvrirLien,
                    onSauver = enregistrer
                )
            }

            if (exportDialogue) {
                AlertDialog(
                    onDismissRequest = { exportDialogue = false },
                    title = { Text("Exporter en GEDCOM") },
                    text = {
                        Column {
                            Text("Quelles personnes exporter ?")
                            Spacer(Modifier.height(8.dp))
                            Row {
                                FilterChip(
                                    selected = exportTout,
                                    onClick = { exportTout = true },
                                    label = { Text("Toutes (" + personnes.size.toString() + ")") }
                                )
                                Spacer(Modifier.width(6.dp))
                                FilterChip(
                                    selected = !exportTout,
                                    onClick = { exportTout = false },
                                    label = { Text("Une branche") }
                                )
                            }
                            if (!exportTout) {
                                val c = personneParId(personnes, exportBrancheId)
                                OutlinedButton(onClick = { pickerBranche = true }, Modifier.padding(top = 8.dp)) {
                                    Text(if (c == null) "Choisir la personne centrale" else "Branche de " + c.titre())
                                }
                            }
                            Text(
                                "Le fichier .ged cr\u00E9\u00E9 est importable dans Heredis (GEDCOM 5.5.1).",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            exportDialogue = false
                            if (exportTout) {
                                lanceurGedcom.launch("geneapoche.ged")
                            } else if (exportBrancheId != 0L) {
                                lanceurGedcomBranche.launch("geneapoche-branche.ged")
                            } else {
                                onMessage("Choisissez une personne centrale.")
                            }
                        }) { Text("Exporter") }
                    },
                    dismissButton = {
                        TextButton(onClick = { exportDialogue = false }) { Text("Annuler") }
                    }
                )
            }
            if (pickerBranche) {
                AlertDialog(
                    onDismissRequest = { pickerBranche = false },
                    title = { Text("Branche de quelle personne ?") },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            personnes.sortedBy { it.titre() }.forEach { c ->
                                TextButton(onClick = {
                                    exportBrancheId = c.id
                                    pickerBranche = false
                                }) { Text(c.titre()) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { pickerBranche = false }) { Text("Fermer") }
                    }
                )
            }
        }
    } else {
        val p = ouverte
        EcranPersonne(
            personnes = personnes,
            pistes = pistes,
            actes = actes,
            p = p,
            enregistrer = enregistrer,
            onOuvrir = { onIdOuvert(it) },
            onRetour = { onIdOuvert(0L) },
            onModifier = { onEditionCible(p); onEditionOuverte(true) },
            onSupprimer = { onConfirmSuppression(p) },
            onChoisir = { onPickerRole(it) },
            onRetirer = { role, enfantId -> delier(role, enfantId) },
            onEtat = { section, valeur ->
                p.changerEtat(section, valeur)
                enregistrer()
            },
            onDefinirCujus = { definirCujus(p) },
            onNouvellePiste = { onPistePersonnePre(p.id); onDialoguePiste(true) },
            onBasculerPiste = { basculerPiste(it) },
            onAjouterActe = { uri, type, titre -> ajouterActe(p.id, uri, type, titre) },
            onSupprimerActe = { onSupprimerActe(it) },
            onVoirActe = { onVoirActe(it) },
            ouvrirEtatCivil = ouvrirEtatCivil
        )
    }

    if (message.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onMessage("") },
            title = { Text("Information") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { onMessage("") }) { Text("OK") }
            }
        )
    }

    if (editionOuverte) {
        val prochainId = (personnes.maxOfOrNull { it.id } ?: 0L) + 1L
        DialogueEdition(
            cible = editionCible,
            idNouvelle = prochainId,
            onValider = { p2 ->
                if (editionCible == null) {
                    if (personnes.size >= PlafondPersonnes) {
                        onMessage("Limite de " + PlafondPersonnes.toString() + " personnes atteinte.")
                    } else {
                        (personnes as? MutableList<Personne>)?.add(p2)
                        if (pickerRole.isNotEmpty()) {
                            lier(pickerRole, p2.id)
                            return@DialogueEdition
                        }
                    }
                }
                enregistrer()
                onEditionOuverte(false)
            },
            onAnnuler = { onEditionOuverte(false) }
        )
    }

    if (dialoguePiste) {
        DialoguePiste(
            personnes = personnes,
            personnePredefinie = pistePersonnePre,
            onValider = { texte, personneId -> ajouterPiste(texte, personneId) },
            onAnnuler = { onDialoguePiste(false) }
        )
    }

    if (pickerRole.isNotEmpty() && ouverte != null) {
        val p = ouverte
        val role = pickerRole
        val candidats = personnes
            .filter { q ->
                q.id != p.id && when (role) {
                    "pere" -> q.id != p.mere && q.id != p.conjoint && !p.conjointsSupp.contains(q.id)
                    "mere" -> q.id != p.pere && q.id != p.conjoint && !p.conjointsSupp.contains(q.id)
                    "conjoint" -> q.id != p.pere && q.id != p.mere && !p.conjointsSupp.contains(q.id)
                    "conjointsupp" -> q.id != p.conjoint && !p.conjointsSupp.contains(q.id) &&
                        q.id != p.pere && q.id != p.mere
                    "enfant" -> q.id != p.pere && q.id != p.mere && q.id != p.conjoint &&
                        !p.conjointsSupp.contains(q.id) && !p.enfants.contains(q.id)
                    else -> false
                }
            }
            .sortedBy { it.titre() }
        val titrePicker = when (role) {
            "pere" -> "Choisir le p\u00E8re"
            "mere" -> "Choisir la m\u00E8re"
            "conjoint" -> "Choisir le conjoint"
            "conjointsupp" -> "Ajouter un autre conjoint"
            else -> "Ajouter un enfant"
        }
        AlertDialog(
            onDismissRequest = { onPickerRole("") },
            title = { Text(titrePicker) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (role == "enfant") {
                        OutlinedButton(onClick = {
                            onEditionCible(null)
                            onEditionOuverte(true)
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("  Cr\u00E9er une nouvelle personne")
                        }
                        Text(
                            "Ou choisir une personne existante :",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    if (candidats.isEmpty()) {
                        Text("Aucune autre personne disponible. Cr\u00E9ez-la avec le bouton + de la liste.")
                    } else {
                        candidats.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { lier(role, c.id) }
                                    .padding(vertical = 10.dp)
                            ) {
                                val num = c.numeroAffiche()
                                if (num.isNotEmpty()) {
                                    Text(num + "  ", fontWeight = FontWeight.Bold)
                                }
                                Text(c.titre())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onPickerRole("") }) { Text("Annuler") }
            }
        )
    }

    if (confirmSuppression != null) {
        val cible = confirmSuppression
        AlertDialog(
            onDismissRequest = { onConfirmSuppression(null) },
            title = { Text("Supprimer ?") },
            text = { Text("Supprimer d\u00E9finitivement " + (cible?.titre() ?: "") + " ? Les liens de parent\u00E9 seront retir\u00E9s.") },
            confirmButton = {
                TextButton(onClick = {
                    cible?.let { supprimer(it) }
                    onConfirmSuppression(null)
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmSuppression(null) }) { Text("Annuler") }
            }
        )
    }
}

// ---------------------------------------------------------------- Liste : carte personne

@Composable
fun CartePersonne(p: Personne, onOuvrir: () -> Unit, onSupprimer: () -> Unit) {
    Card(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable { onOuvrir() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initiale = p.prenoms.trim().firstOrNull() ?: p.nom.trim().firstOrNull() ?: '?'
            val couleurAvatar = when (p.sexe) {
                "F" -> Color(0xFF55875F)
                "M" -> Color(0xFF2E5D3A)
                else -> Color(0xFF6E7F6E)
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(couleurAvatar),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initiale.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.titre(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    val num = p.numeroAffiche()
                    if (num.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                num,
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                val n = p.dateNaissance.trim()
                val d = p.dateDeces.trim()
                if (n.isNotEmpty() || d.isNotEmpty()) {
                    val morceaux = mutableListOf<String>()
                    if (n.isNotEmpty()) morceaux.add("N. " + n)
                    if (d.isNotEmpty()) morceaux.add("D. " + d)
                    Text(morceaux.joinToString("  \u2013  "), style = MaterialTheme.typography.bodySmall)
                }
                if (p.aVerifier()) {
                    Text("\u00C0 v\u00E9rifier", color = CouleurRouge, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onSupprimer) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer")
            }
        }
    }
}
