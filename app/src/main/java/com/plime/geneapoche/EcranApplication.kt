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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    choisirCujus: () -> Unit,
    renumeroter: (Long) -> Unit,
    supprimerPiste: (Piste) -> Unit,
    actes: List<Acte>,
    ajouterActe: (Long, Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    arbreCentre: Long,
    onArbreCentre: (Long) -> Unit,
    exporterGedcom: (Uri, Set<Long>?) -> Unit,
    importerGedcom: (Uri) -> Unit,
    viderBase: () -> Unit,
    enregistrerActe: (ActeSaisie) -> Unit,
    ouvrirLien: (String) -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit
) {
    BackHandler(enabled = idOuvert != 0L || editionOuverte || pickerRole.isNotEmpty()) {
        when {
            pickerRole.isNotEmpty() -> onPickerRole("")
            editionOuverte -> onEditionOuverte(false)
            else -> onIdOuvert(0L)
        }
    }

    val ouverte = personneParId(personnes, idOuvert)
    var menuGedcom by remember { mutableStateOf(false) }
    var confirmeVider by remember { mutableStateOf(false) }

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
                                    fontSize = 14.sp,
                                    color = Color(0xFFE3F1DF)
                                )
                            }
                        }
                    },
                    actions = {
                        TextButton(onClick = { menuGedcom = true }) {
                            Text("GEDCOM", color = Color.White, fontWeight = FontWeight.Bold)
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
                    NavigationBarItem(
                        selected = onglet == 3,
                        onClick = { onOnglet(3) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text("Saisie") }
                    )
                }
            },
            floatingActionButton = {
                if (onglet == 0) {
                    FloatingActionButton(onClick = { onEditionCible(null); onEditionOuverte(true) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter")
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
                    // Onglet PERSONNES : compteur, bascule A-Z / Branches, index, cartes.
                    var triAlpha by remember { mutableStateOf(true) }
                    val triees = personnes.sortedWith(
                        compareBy(
                            { it.nom.trim().uppercase() },
                            { it.prenoms.trim().uppercase() },
                            { anneeNaissance(it) }
                        )
                    )
                    val etatListe = rememberLazyListState()
                    val portee = rememberCoroutineScope()
                    val lettresUtilisees = triees
                        .map { it.nom.trim().uppercase().firstOrNull() ?: '?' }
                        .toSet()

                    // Classement par branche : la racine est l'ancetre le plus haut
                    // (pere, sinon mere) ; la branche porte le nom de cette racine.
                    val entrees: List<Any> = if (triAlpha) {
                        triees.toList()
                    } else {
                        val liste = mutableListOf<Any>()
                        personnes.groupBy { racineBranche(it, personnes).nom.trim().uppercase().ifEmpty { "?" } }
                            .toSortedMap()
                            .forEach { (etiquette, membres) ->
                                liste.add(etiquette)
                                membres.sortedWith(
                                    compareBy(
                                        { it.sosa.trim().toLongOrNull() ?: Long.MAX_VALUE },
                                        { it.lateral.trim() },
                                        { it.nom.trim().uppercase() }
                                    )
                                ).forEach { liste.add(it) }
                            }
                        liste
                    }

                    LazyColumn(
                        Modifier.fillMaxSize().padding(marges).padding(vertical = 4.dp),
                        state = etatListe
                    ) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    personnes.size.toString() + " / " + PlafondPersonnes.toString() + " personnes",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilterChip(
                                    selected = triAlpha,
                                    onClick = { triAlpha = true },
                                    label = { Text("A\u2013Z") }
                                )
                                Spacer(Modifier.width(6.dp))
                                FilterChip(
                                    selected = !triAlpha,
                                    onClick = { triAlpha = false },
                                    label = { Text("Branches") }
                                )
                            }
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)
                            ) {
                                if (triAlpha) {
                                    // Alphabet complet, resserre pour tenir d'un coup d'oeil :
                                    // lettres inutilisees grisees.
                                    ('A'..'Z').forEach { lettre ->
                                        val utilisee = lettresUtilisees.contains(lettre)
                                        Text(
                                            lettre.toString(),
                                            Modifier
                                                .clickable(enabled = utilisee) {
                                                    val pos = triees.indexOfFirst {
                                                        (it.nom.trim().uppercase().firstOrNull() ?: '?') == lettre
                                                    }
                                                    if (pos >= 0) portee.launch { etatListe.scrollToItem(2 + pos) }
                                                }
                                                .padding(horizontal = 3.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (utilisee) CouleurVertFonce else Color(0xFFB9C2AE)
                                        )
                                    }
                                } else {
                                    val etiquettes = entrees.filterIsInstance<String>()
                                    etiquettes.forEach { etiquette ->
                                        TextButton(
                                            onClick = {
                                                val pos = entrees.indexOf(etiquette)
                                                if (pos >= 0) portee.launch { etatListe.scrollToItem(2 + pos) }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp)
                                        ) { Text(etiquette, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                        items(entrees.size) { i ->
                            val e = entrees[i]
                            if (e is String) {
                                Text(
                                    e,
                                    Modifier.fillMaxWidth()
                                        .background(CouleurVertPale)
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = CouleurVertFonce
                                )
                            } else {
                                val p = e as Personne
                                CartePersonne(
                                    p,
                                    onOuvrir = { onIdOuvert(p.id) },
                                    onSupprimer = { onConfirmSuppression(p) }
                                )
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
            } else if (onglet == 3) {
                EcranSaisieActe(
                    personnes = personnes,
                    onEnregistrer = enregistrerActe
                )
            } else {
                EcranJournal(
                    pistes = pistes,
                    personnes = personnes,
                    actes = actes,
                    onSupprimer = { supprimerPiste(it) },
                    onOuvrirPersonne = { onIdOuvert(it) },
                    ouvrirLien = ouvrirLien,
                    ouvrirEtatCivil = ouvrirEtatCivil,
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
            if (menuGedcom) {
                AlertDialog(
                    onDismissRequest = { menuGedcom = false },
                    title = { Text("GEDCOM (Heredis)") },
                    text = {
                        Column {
                            Text(personnes.size.toString() + " / " + PlafondPersonnes.toString() + " personnes", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { menuGedcom = false; exportDialogue = true },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Exporter (tout ou une branche)")
                                }
                            }
                            TextButton(
                                onClick = { menuGedcom = false; lanceurImport.launch(arrayOf("*/*")) },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ExitToApp, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Importer un GEDCOM (remplace la base)")
                                }
                            }
                            TextButton(
                                onClick = { menuGedcom = false; choisirCujus() },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("D\u00E9finir le de cujus et son n\u00B0 Sosa")
                                }
                            }
                            TextButton(
                                onClick = { menuGedcom = false; confirmeVider = true },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Vider la base", color = CouleurRouge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { menuGedcom = false }) { Text("Fermer") }
                    }
                )
            }
            if (confirmeVider) {
                AlertDialog(
                    onDismissRequest = { confirmeVider = false },
                    title = { Text("Vider toute la base ?") },
                    text = {
                        Text("Toutes les personnes, le journal, les actes et leurs images seront d\u00E9finitivement supprim\u00E9s. Confirmez seulement si vous avez export\u00E9 une sauvegarde GEDCOM.")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmeVider = false
                            viderBase()
                        }) { Text("Oui, tout vider", color = CouleurRouge) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmeVider = false }) { Text("Annuler") }
                    }
                )
            }
        }
    } else {
        val p = ouverte
        EcranPersonne(
            personnes = personnes,
            actes = actes,
            p = p,
            enregistrer = enregistrer,
            onOuvrir = { onIdOuvert(it) },
            onRetour = { onIdOuvert(0L) },
            onModifier = { onEditionCible(p); onEditionOuverte(true) },
            onSupprimer = { onConfirmSuppression(p) },
            onChoisir = { onPickerRole(it) },
            onRetirer = { role, enfantId -> delier(role, enfantId) },
            onDefinirCujus = { definirCujus(p) },
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

// Racine de branche d'une personne : l'ancetre le plus haut atteignable
// (pere, sinon mere). La branche porte le nom de famille de cette racine.
fun racineBranche(p: Personne, personnes: List<Personne>): Personne {
    var courante = p
    var gardeFou = 0
    while (gardeFou < 60) {
        gardeFou += 1
        val parentId = if (courante.pere != 0L) courante.pere else courante.mere
        if (parentId == 0L) return courante
        val parent = personnes.firstOrNull { it.id == parentId } ?: return courante
        courante = parent
    }
    return courante
}

@Composable
fun CartePersonne(p: Personne, onOuvrir: () -> Unit, onSupprimer: () -> Unit) {
    // Tout l'encadre rose pale (femme) ou bleu pale (homme), neutre sinon.
    val couleurCarte = when (p.sexe) {
        "F" -> Color(0xFFF8DDE7) // rose pale
        "M" -> Color(0xFFDDE9F6) // bleu pale
        else -> Color(0xFFEFF4EE) // neutre verd\u00E2tre pale
    }
    Card(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable { onOuvrir() },
        colors = CardDefaults.cardColors(containerColor = couleurCarte)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vignette : rond au numero Sosa ou d'Aboville, calcule
            // automatiquement a partir du de cujus (jamais saisi).
            val num = p.numeroAffiche().replace("Sosa ", "").replace("Lat\u00E9ral ", "")
            val initiale = p.prenoms.trim().firstOrNull() ?: p.nom.trim().firstOrNull() ?: '?'
            val vignette = num.ifEmpty { initiale.toString() }
            val couleurAvatar = when (p.sexe) {
                "F" -> Color(0xFFE9C4D2) // rose
                "M" -> Color(0xFFBFD4E8) // bleu
                else -> Color(0xFFD9E4D6) // neutre
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(couleurAvatar),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    vignette,
                    color = CouleurVertFonce,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.titre(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val n = dateAffichee(p.dateNaissance.trim())
                val m = dateAffichee(p.dateMariage.trim())
                val d = dateAffichee(p.dateDeces.trim())
                if (n.isNotEmpty() || m.isNotEmpty() || d.isNotEmpty()) {
                    val morceaux = mutableListOf<String>()
                    if (n.isNotEmpty()) morceaux.add("N. " + n)
                    if (m.isNotEmpty()) morceaux.add("M. " + m)
                    if (d.isNotEmpty()) morceaux.add("D. " + d)
                    Text(morceaux.joinToString("  \u2013  "), style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onSupprimer, Modifier.size(30.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer", Modifier.size(16.dp))
            }
        }
    }
}





