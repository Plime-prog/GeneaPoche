package com.plime.geneapoche

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ---------------------------------------------------------------- Fiche personne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPersonne(
    personnes: List<Personne>,
    pistes: List<Piste>,
    actes: List<Acte>,
    p: Personne,
    enregistrer: () -> Unit,
    onOuvrir: (Long) -> Unit,
    onRetour: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit,
    onEtat: (String, String) -> Unit,
    onDefinirCujus: () -> Unit,
    onNouvellePiste: () -> Unit,
    onBasculerPiste: (Piste) -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(p.titre(), fontSize = 18.sp)
                        val num = p.numeroAffiche()
                        if (num.isNotEmpty()) {
                            Text(num, fontSize = 11.sp, color = Color(0xFFD8EBD4))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaletteGeneaPoche.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onModifier) {
                        Icon(Icons.Filled.Create, contentDescription = "Modifier")
                    }
                    IconButton(onClick = onSupprimer) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                    }
                }
            )
        }
    ) { marges ->
        LazyColumn(
            Modifier.fillMaxSize().padding(marges).padding(horizontal = 12.dp)
        ) {
            item {
                CarteEvenement(
                    titre = "Naissance",
                    section = "naissance",
                    typeActe = "N",
                    date = p.dateNaissance, heure = p.heureNaissance,
                    lieu = p.lieuNaissance, dept = p.deptNaissance,
                    etat = p.etatDe("naissance"),
                    actes = actes.filter { it.personneId == p.id && it.type == "N" },
                    onEtat = { v -> onEtat("naissance", v) },
                    onModifier = onModifier,
                    onAjouterActe = onAjouterActe,
                    onSupprimerActe = onSupprimerActe,
                    onVoirActe = onVoirActe,
                    onEnregistrer = enregistrer,
                    ouvrirEtatCivil = ouvrirEtatCivil
                )
            }
            if (p.blocsOuverts.contains("bapteme") || p.dateBapteme.isNotBlank() || p.heureBapteme.isNotBlank() || p.lieuBapteme.isNotBlank() || actes.any { it.personneId == p.id && it.type == "B" }) {
                item {
                    CarteEvenement(
                        titre = "Bapt\u00EAme",
                        section = "bapteme",
                        typeActe = "B",
                        date = p.dateBapteme, heure = p.heureBapteme,
                        lieu = p.lieuBapteme, dept = p.deptBapteme,
                        etat = p.etatDe("bapteme"),
                        actes = actes.filter { it.personneId == p.id && it.type == "B" },
                        onEtat = { v -> onEtat("bapteme", v) },
                        onModifier = onModifier,
                        onAjouterActe = onAjouterActe,
                        onSupprimerActe = onSupprimerActe,
                        onVoirActe = onVoirActe,
                        onEnregistrer = enregistrer,
                        ouvrirEtatCivil = ouvrirEtatCivil
                    )
                }
            }
            item {
                CarteEvenement(
                    titre = "Mariage",
                    section = "mariage",
                    typeActe = "M",
                    date = p.dateMariage, heure = p.heureMariage,
                    lieu = p.lieuMariage, dept = p.deptMariage,
                    etat = p.etatDe("mariage"),
                    actes = actes.filter { it.personneId == p.id && it.type == "M" },
                    onEtat = { v -> onEtat("mariage", v) },
                    onModifier = onModifier,
                    onAjouterActe = onAjouterActe,
                    onSupprimerActe = onSupprimerActe,
                    onVoirActe = onVoirActe,
                    onEnregistrer = enregistrer,
                    ouvrirEtatCivil = ouvrirEtatCivil
                )
            }
            item {
                CarteEvenement(
                    titre = "D\u00E9c\u00E8s",
                    section = "deces",
                    typeActe = "D",
                    date = p.dateDeces, heure = p.heureDeces,
                    lieu = p.lieuDeces, dept = p.deptDeces,
                    etat = p.etatDe("deces"),
                    actes = actes.filter { it.personneId == p.id && it.type == "D" },
                    onEtat = { v -> onEtat("deces", v) },
                    onModifier = onModifier,
                    onAjouterActe = onAjouterActe,
                    onSupprimerActe = onSupprimerActe,
                    onVoirActe = onVoirActe,
                    onEnregistrer = enregistrer,
                    ouvrirEtatCivil = ouvrirEtatCivil
                )
            }
            if (p.blocsOuverts.contains("sepulture") || p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank() || actes.any { it.personneId == p.id && it.type == "S" }) {
                item {
                    CarteEvenement(
                        titre = "S\u00E9pulture",
                        section = "sepulture",
                        typeActe = "S",
                        date = p.dateSepulture, heure = p.heureSepulture,
                        lieu = p.lieuSepulture, dept = p.deptSepulture,
                        etat = p.etatDe("sepulture"),
                        actes = actes.filter { it.personneId == p.id && it.type == "S" },
                        onEtat = { v -> onEtat("sepulture", v) },
                        onModifier = onModifier,
                        onAjouterActe = onAjouterActe,
                        onSupprimerActe = onSupprimerActe,
                        onVoirActe = onVoirActe,
                        onEnregistrer = enregistrer,
                        ouvrirEtatCivil = ouvrirEtatCivil
                    )
                }
            }
            if (!p.blocsOuverts.contains("bapteme") || !p.blocsOuverts.contains("sepulture")) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        if (!p.blocsOuverts.contains("bapteme")) {
                            OutlinedButton(onClick = { p.blocsOuverts.add("bapteme"); enregistrer() }, Modifier.weight(1f)) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text("  Bapt\u00EAme")
                            }
                        }
                        if (!p.blocsOuverts.contains("sepulture")) {
                            OutlinedButton(onClick = { p.blocsOuverts.add("sepulture"); enregistrer() }, Modifier.weight(1f)) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text("  S\u00E9pulture")
                            }
                        }
                    }
                }
            }
            item {
                CarteFamille(
                    personnes = personnes,
                    p = p,
                    onOuvrir = onOuvrir,
                    onChoisir = onChoisir,
                    onRetirer = onRetirer,
                    onDefinirCujus = onDefinirCujus
                )
            }
            item {
                CartePistesPersonne(
                    pistes = pistes,
                    personnes = personnes,
                    actes = actes,
                    p = p,
                    onNouvellePiste = onNouvellePiste,
                    onBasculer = onBasculerPiste
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ---------------------------------------------------------------- Bloc d'evenement (avec actes et lien AD)

fun libelleType(t: String): String = when (t) {
    "N" -> "Naissance"
    "B" -> "Bapt\u00EAme"
    "M" -> "Mariage"
    "D" -> "D\u00E9c\u00E8s"
    "S" -> "S\u00E9pulture"
    else -> "Autre"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteEvenement(
    titre: String,
    section: String,
    typeActe: String,
    date: String,
    heure: String,
    lieu: String,
    dept: String,
    etat: String,
    actes: List<Acte>,
    onEtat: (String) -> Unit,
    onModifier: () -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    onEnregistrer: () -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit
) {
    val d = date.trim()
    val l = lieu.trim()
    val h = heure.trim()
    val dp = dept.trim()
    val rien = d.isEmpty() && l.isEmpty() && h.isEmpty() && dp.isEmpty() && etat.isEmpty() && actes.isEmpty()

    Card(
        Modifier.fillMaxWidth()
            .padding(vertical = 5.dp)
            .combinedClickable(onClick = {}, onLongClick = { onModifier() }),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            // Verification Oui/Non du bloc : Oui automatiquement si une date ou une image existe.
            val verifie = etat == "vert" || date.isNotBlank() || actes.isNotEmpty()
            EnteteVerifie(titre, verifie, onVerifie = { v -> onEtat(if (v) "vert" else "") })
            Ligne("Date", dateAffichee(d))
            Ligne("Heure", heureAffichee(h))
            Ligne("Lieu", l)
            Ligne("D\u00E9partement", dp)

            // Lien direct vers l'etat civil des AD selon la date et le type d'acte
            val numDept = dp.toIntOrNull() ?: numeroDepartement(l) ?: numeroDepartement(dp)
            if ((l.isNotEmpty() || numDept != null) && (d.isNotEmpty() || l.isNotEmpty())) {
                OutlinedButton(
                    onClick = {
                        ouvrirEtatCivil(l, numDept, libelleType(typeActe), Regex("[0-9]{4}").find(d)?.value ?: "")
                    },
                    Modifier.padding(top = 4.dp)
                ) {
                    Text("Rechercher cet acte aux AD")
                }
            }

            // Actes stockes pour cet evenement
            if (actes.isNotEmpty()) {
                Text(
                    "Actes stock\u00E9s (" + actes.size.toString() + ")",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
                actes.forEach { acte ->
                    LigneActe(
                        acte = acte,
                        onVoir = { onVoirActe(acte) },
                        onSupprimer = { onSupprimerActe(acte) },
                        onEnregistrer = onEnregistrer
                    )
                }
            }
            AjoutActeBloc(typeActe, onAjouterActe)
        }
    }
}

@Composable
fun AjoutActeBloc(typeActe: String, onAjouterActe: (Uri, String, String) -> Unit) {
    var dialogueOuvert by remember { mutableStateOf(false) }
    val lanceur = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAjouterActe(uri, typeActe, "")
    }
    OutlinedButton(onClick = { dialogueOuvert = true }, Modifier.padding(top = 4.dp)) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text("  Ajouter un acte")
    }
    if (dialogueOuvert) {
        AlertDialog(
            onDismissRequest = { dialogueOuvert = false },
            title = { Text("Ajouter un acte : " + libelleType(typeActe)) },
            text = {
                Text(
                    "L'image reste sur l'appareil, chiffr\u00E9e. Rien n'est transmis.\nChoisissez l'image \u00E0 associer \u00E0 cet \u00E9v\u00E9nement."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dialogueOuvert = false
                    lanceur.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Choisir l'image") }
            },
            dismissButton = {
                TextButton(onClick = { dialogueOuvert = false }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LigneActe(
    acte: Acte,
    onVoir: () -> Unit,
    onSupprimer: () -> Unit,
    onEnregistrer: () -> Unit
) {
    var pageOuvert by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (acte.titre.trim().isNotEmpty()) acte.titre.trim() else "(sans titre)",
            Modifier.weight(1f).combinedClickable(onClick = onVoir, onLongClick = { pageOuvert = true }),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (acte.numPage.isNotEmpty()) {
            Text(
                "p. " + acte.numPage + "/" + acte.totalPage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        TextButton(onClick = { pageOuvert = true }) { Text("Page") }
        IconButton(onClick = onSupprimer) {
            Icon(Icons.Filled.Close, contentDescription = "Supprimer")
        }
    }
    if (pageOuvert) {
        DialoguePageActe(
            acte = acte,
            onValider = { num, total ->
                acte.numPage = num
                acte.totalPage = total
                onEnregistrer()
                pageOuvert = false
            },
            onAnnuler = { pageOuvert = false }
        )
    }
}

@Composable
fun DialoguePageActe(
    acte: Acte,
    onValider: (String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var num by remember { mutableStateOf(acte.numPage) }
    var total by remember { mutableStateOf(acte.totalPage) }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("O\u00F9 en est la consultation ?") },
        text = {
            Column {
                OutlinedTextField(
                    value = num,
                    onValueChange = { num = it },
                    label = { Text("Num\u00E9ro de la page consult\u00E9e") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it },
                    label = { Text("sur un total de ... pages (facultatif)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
                Text(
                    "Ex : page 230 sur 487. Sert de rappel pour reprendre la recherche.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(num.trim(), total.trim()) }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Pastilles

@Composable
fun EnteteVerifie(titre: String, verifie: Boolean, onVerifie: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(titre, Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("V\u00E9rifi\u00E9 :", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(4.dp))
        FilterChip(
            selected = verifie,
            onClick = { onVerifie(!verifie) },
            label = { Text(if (verifie) "Oui" else "Non") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = CouleurNeutrePale,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = CouleurVertPale,
                selectedLabelColor = CouleurVertFonce
            )
        )
    }
}

@Composable
fun Ligne(libelle: String, valeur: String) {
    if (valeur.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(valeur, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------- Famille

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteFamille(
    personnes: List<Personne>,
    p: Personne,
    onOuvrir: (Long) -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit,
    onDefinirCujus: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Famille", fontWeight = FontWeight.Bold)
            Ligne("Profession", p.profession.trim())
            if (p.sosa.trim() != "1") {
                OutlinedButton(onClick = onDefinirCujus, Modifier.padding(top = 4.dp)) {
                    Text("D\u00E9finir comme de cujus")
                }
            }
            Spacer(Modifier.height(6.dp))

            LienParent(
                libelle = "P\u00E8re",
                cible = p.pere,
                personnes = personnes,
                onOuvrir = onOuvrir,
                onChoisir = { onChoisir("pere") },
                onRetirer = { onRetirer("pere", 0L) }
            )
            LienParent(
                libelle = "M\u00E8re",
                cible = p.mere,
                personnes = personnes,
                onOuvrir = onOuvrir,
                onChoisir = { onChoisir("mere") },
                onRetirer = { onRetirer("mere", 0L) }
            )
            LienParent(
                libelle = "Conjoint(e)",
                cible = p.conjoint,
                personnes = personnes,
                onOuvrir = onOuvrir,
                onChoisir = { onChoisir("conjoint") },
                onRetirer = { onRetirer("conjoint", 0L) }
            )
            p.conjointsSupp.forEach { idC ->
                LienParent(
                    libelle = "Autre conjoint(e)",
                    cible = idC,
                    personnes = personnes,
                    onOuvrir = onOuvrir,
                    onChoisir = { onChoisir("conjointsupp") },
                    onRetirer = { onRetirer("conjointsupp", idC) }
                )
            }
            OutlinedButton(onClick = { onChoisir("conjointsupp") }, Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Ajouter un autre conjoint")
            }

            Spacer(Modifier.height(6.dp))
            Text("Enfants (" + p.enfants.size.toString() + ")", fontWeight = FontWeight.Bold)
            if (p.enfants.isEmpty()) {
                Text("Aucun enfant renseign\u00E9.", style = MaterialTheme.typography.bodySmall)
            }
            p.enfants.forEach { idEnfant ->
                val e = personneParId(personnes, idEnfant)
                val nomEnfant = e?.titre() ?: "(fiche supprim\u00E9e)"
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        nomEnfant,
                        Modifier.weight(1f).combinedClickable(onClick = { if (e != null) onOuvrir(idEnfant) }, onLongClick = {}),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onRetirer("enfant", idEnfant) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer")
                    }
                }
            }
            OutlinedButton(onClick = { onChoisir("enfant") }, Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Ajouter un enfant")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LienParent(
    libelle: String,
    cible: Long,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit,
    onChoisir: () -> Unit,
    onRetirer: () -> Unit
) {
    val q = personneParId(personnes, cible)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        if (q == null) {
            OutlinedButton(onClick = onChoisir) { Text("Choisir") }
        } else {
            Text(
                q.titre(),
                Modifier.weight(1f).combinedClickable(onClick = { onOuvrir(cible) }, onLongClick = {}),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onRetirer) {
                Icon(Icons.Filled.Close, contentDescription = "Retirer")
            }
        }
    }
}

// ---------------------------------------------------------------- Pistes de la personne
// Les 3 blocs du journal (a faire / en cours / archives) synchronises sur la fiche.

@Composable
fun CartePistesPersonne(
    pistes: List<Piste>,
    personnes: List<Personne>,
    actes: List<Acte>,
    p: Personne,
    onNouvellePiste: () -> Unit,
    onBasculer: (Piste) -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Recherches", fontWeight = FontWeight.Bold)
            val deCettePersonne = pistes.filter { it.personneId == p.id && !it.faite }
            val restantes = suggestionsPour(p, personnes, actes).size
            Text(
                if (restantes > 0) {
                    deCettePersonne.size.toString() + " en cours \u2013 " + restantes.toString() + " information(s) encore \u00E0 trouver pour cette personne."
                } else {
                    "Tout est renseign\u00E9 et v\u00E9rifi\u00E9 pour cette personne."
                },
                style = MaterialTheme.typography.bodySmall
            )
            deCettePersonne.forEach { piste ->
                var confirme by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    // a. Rappel de la recherche a faire
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { confirme = true },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CouleurVert,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            textePisteActuel(piste, personnes, actes),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    // b. Recherche en cours
                    if (piste.enCours.isNotBlank()) {
                        Text("b. En cours : " + piste.enCours, style = MaterialTheme.typography.bodySmall)
                    }
                    // c. Recherches archivees (horodatees)
                    if (piste.historique.isNotEmpty()) {
                        Text("c. Archives :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        piste.historique.take(3).forEach { r ->
                            Text(
                                "   \u2022 " + formaterDateHeure(r.date) + " \u2013 " + libelleRecherche(r),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (confirme) {
                    AlertDialog(
                        onDismissRequest = { confirme = false },
                        title = { Text("Valider cette recherche ?") },
                        text = { Text("Elle sera marqu\u00E9e comme termin\u00E9e. Retrouvez le d\u00E9tail dans le journal.") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirme = false
                                onBasculer(piste)
                            }) { Text("Oui, termin\u00E9e") }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirme = false }) { Text("Pas encore") }
                        }
                    )
                }
            }
            OutlinedButton(onClick = onNouvellePiste, Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Nouvelle recherche pour cette personne")
            }
        }
    }
}

// ---------------------------------------------------------------- Actes : image + zoom

// Import d'une image (capture d'ecran ou photo) : redimensionnee puis compressee en JPEG.
fun compresserVersJpeg(ctx: Context, uri: Uri): ByteArray? {
    return try {
        val donnees = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(donnees, 0, donnees.size, bornes)
        var ech = 1
        while ((bornes.outWidth / (ech * 2)) >= 1200 || (bornes.outHeight / (ech * 2)) >= 1200) {
            ech *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = ech }
        val brut = BitmapFactory.decodeByteArray(donnees, 0, donnees.size, opts) ?: return null
        val sortie = java.io.ByteArrayOutputStream()
        brut.compress(Bitmap.CompressFormat.JPEG, 85, sortie)
        brut.recycle()
        sortie.toByteArray()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VisionneuseActe(ctx: Context, acte: Acte, onFermer: () -> Unit) {
    val bmp = remember(acte.id) {
        val octets = Stockage.lireImage(ctx, acte.id)
        if (octets != null) BitmapFactory.decodeByteArray(octets, 0, octets.size) else null
    }
    var echelle by remember { mutableStateOf(1f) }
    var decalage by remember { mutableStateOf(Offset.Zero) }
    val etat = rememberTransformableState { zoom, pan, _ ->
        echelle = (echelle * zoom).coerceIn(1f, 6f)
        decalage = if (echelle > 1f) decalage + pan else Offset.Zero
    }
    Dialog(
        onDismissRequest = onFermer,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val t = acte.titre.trim()
                    Text(
                        if (t.isNotEmpty()) libelleType(acte.type) + " \u2013 " + t else libelleType(acte.type),
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onFermer) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }
                if (bmp == null) {
                    Text("Image illisible.", color = CouleurRouge)
                } else {
                    Text(
                        "Pincez pour zoomer, glissez pour d\u00E9placer.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .transformable(etat)
                            .graphicsLayer {
                                scaleX = echelle
                                scaleY = echelle
                                translationX = decalage.x
                                translationY = decalage.y
                            }
                    )
                }
            }
        }
    }
}




