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
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ---------------------------------------------------------------- Fiche personne
// Blocs modifiables par appui long uniquement. Les informations manquantes
// sont affichees en gris : elles alimentent le journal de recherches.

val GrisManquant = Color(0xFF9AA099)

// Couleur de la pastille : rose (F), bleu (M), vert p\u00E2le si sexe absent.
fun couleurPastille(sexe: String): Color = when (sexe) {
    "F" -> Color(0xFFE9C4D2)
    "M" -> Color(0xFFBFD4E8)
    else -> Color(0xFFD9E4D6)
}

// Pastille ronde au num\u00E9ro (ou initiale), identique \u00E0 la liste Personnes :
// taille de police adapt\u00E9e aux num\u00E9ros d'Aboville longs.
@Composable
fun PastilleNumero(p: Personne, diametre: Int) {
    val num = p.numeroBrut()
    val initiale = p.prenoms.trim().firstOrNull() ?: p.nom.trim().firstOrNull() ?: '?'
    val vignette = num.ifEmpty { initiale.toString() }
    Box(
        Modifier.size(diametre.dp).clip(CircleShape).background(couleurPastille(p.sexe)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            vignette,
            color = CouleurVertFonce,
            fontWeight = FontWeight.Bold,
            fontSize = when {
                vignette.length <= 3 -> (diametre * 15 / 40).sp
                vignette.length <= 5 -> (diametre * 12 / 40).sp
                else -> (diametre * 9 / 40).sp
            },
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------- Ecran fiche
// Plus de crayon d'edition globale : seules les informations de chaque bloc
// sont editables, via le dialogue d'edition du bloc. Cartouche identite
// (pastille + parents avec dates N/D), puis frise chronologique des
// evenements : N et D par defaut, B/M/S via le bouton BMS ; toucher un
// evenement ouvre son bloc.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EcranPersonne(
    personnes: List<Personne>,
    actes: List<Acte>,
    p: Personne,
    enregistrer: () -> Unit,
    onOuvrir: (Long) -> Unit,
    onRetour: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit,
    onDefinirCujus: () -> Unit,
    onNouveauMariage: (String, String, String, String, String) -> Unit,
    onNouvelEnfant: (String, String, String, String) -> Unit,
    onNouveauParent: (String, String, String) -> Unit,
    onNouveauTemoin: (String, Long, String, String) -> Unit,
    onRetirerTemoin: (String, Long) -> Unit,
    onBasculerSignature: (String, Long) -> Unit,
    onModifierBloc: (String, String, String, String, String, String) -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit,
    // Coche de validation d'une information : (section, valide).
    // Une coche negative cree automatiquement une recherche dans le journal.
    onValider: (String, Boolean) -> Unit,
    // Recherches ouvertes de cette personne (bloc "Recherches" de la fiche)
    // et suppression directe d'une recherche.
    pistes: List<Piste> = emptyList(),
    onSupprimerPiste: (Piste) -> Unit = {}
) {
    // Blocs ouverts sur la frise : N et D par defaut.
    var ouverts by remember { mutableStateOf(setOf<String>()) }
    // Dialogues : edition d'un bloc, mariage, enfant, parent, temoin.
    var blocEdite by remember { mutableStateOf<String?>(null) }
    var mariageOuvert by remember { mutableStateOf(false) }
    var enfantOuvert by remember { mutableStateOf(false) }
    var menuEnfant by remember { mutableStateOf(false) }
    var menuEvenement by remember { mutableStateOf(false) }
    var parentRole by remember { mutableStateOf("") }
    var temoinType by remember { mutableStateOf("") }

    // Evenements de la frise : ordre FIXE N en haut, puis B, M, S s'ils
    // existent, puis D en dernier, meme si N ou D n'ont pas d'information.
    val ordreTypes = mutableListOf("N")
    if (p.blocsOuverts.contains("bapteme") || p.dateBapteme.isNotBlank() || p.heureBapteme.isNotBlank() || p.lieuBapteme.isNotBlank() || actes.any { it.personneId == p.id && it.type == "B" }) {
        ordreTypes.add("B")
    }
    if (p.blocsOuverts.contains("mariage") || p.dateMariage.isNotBlank() || p.heureMariage.isNotBlank() || p.lieuMariage.isNotBlank() || p.conjoint != 0L || p.conjointsSupp.isNotEmpty() || p.enfants.isNotEmpty() || actes.any { it.personneId == p.id && it.type == "M" }) {
        ordreTypes.add("M")
    }
    // R\u00E8gle des 100 ans : la notion de d\u00E9c\u00E8s ou de s\u00E9pulture ne s'affiche
    // que si la personne peut avoir plus de 100 ans \u00E0 la date du jour (ou si
    // son d\u00E9c\u00E8s est d\u00E9j\u00E0 connu).
    val decesAffiche = decesAffichable(p, actes)
    if (decesAffiche) {
        if (p.blocsOuverts.contains("sepulture") || p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank() || actes.any { it.personneId == p.id && it.type == "S" }) {
            ordreTypes.add("S")
        }
        ordreTypes.add("D")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Pastille au numero + nom et prenoms, comme la liste.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PastilleNumero(p = p, diametre = 36)
                        Spacer(Modifier.width(8.dp))
                        Text(p.titre(), fontSize = 17.sp)
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
                    // Pas de crayon : l'edition s'ouvre par appui long sur le
                    // bloc identite (v0.28).
                    IconButton(onClick = onSupprimer) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                    }
                }
            )
        },
            // Cinq boutons d'action FIXES en bas de la fiche, toujours
            // visibles, texte sur une seule ligne, icone pour chacun.
            bottomBar = {
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { p.blocsOuverts.add("bapteme"); ouverts = ouverts + "B"; enregistrer() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Face, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Bapt\u00EAme", fontSize = 12.sp, maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = { p.blocsOuverts.add("mariage"); ouverts = ouverts + "M"; enregistrer() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Mariage", fontSize = 12.sp, maxLines = 1)
                            }
                            if (decesAffiche) {
                                FilledTonalButton(
                                    onClick = { p.blocsOuverts.add("sepulture"); ouverts = ouverts + "S"; enregistrer() },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Place, contentDescription = null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("S\u00E9pulture", fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { menuEnfant = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Enfant", fontSize = 12.sp, maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = { menuEvenement = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("\u00C9v\u00E9nement", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
    ) { marges ->
        LazyColumn(
            Modifier.fillMaxSize().padding(marges).padding(horizontal = 12.dp)
        ) {
            item {
                CarteIdentite(
                    p = p,
                    personnes = personnes,
                    onOuvrir = onOuvrir,
                    onValider = { v -> onValider("identite", v) },
                    onEditer = onModifier
                )
            }
            // Frise chronologique : UN bloc encadre PAR EVENEMENT (v0.28), puis
            // un bloc par enfant. Toucher l'en-tete ouvre le bloc, appui long
            // ouvre l'edition de l'evenement.
            item {
                Column(Modifier.fillMaxWidth()) {
                    ordreTypes.forEach { t ->
                        val cEv = champsEvenement(p, t)
                        val dEv = cEv.date.trim()
                        val lEv = cEv.lieu.trim()
                        val dpEv = cEv.dept.trim()
                        val numDept = dpEv.toIntOrNull() ?: numeroDepartement(dpEv) ?: numeroDepartement(lEv)
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, couleurPastille(p.sexe))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                        .combinedClickable(
                                            onClick = { ouverts = if (ouverts.contains(t)) ouverts - t else ouverts + t },
                                            onLongClick = { blocEdite = t }
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                Box(
                                    Modifier.size(10.dp).clip(CircleShape)
                                        .background(couleurPastille(p.sexe))
                                )
                                Spacer(Modifier.width(8.dp))
                                val morceaux = mutableListOf<String>()
                                if (dEv.isNotEmpty()) morceaux.add(dateAffichee(dEv))
                                if (lEv.isNotEmpty()) morceaux.add(lEv + (if (numDept != null) " (" + numDept.toString() + ")" else ""))
                                Text(
                                    t + ". " + if (morceaux.isEmpty()) "\u2014" else morceaux.joinToString(" \u00E0 "),
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (morceaux.isEmpty()) GrisManquant else Color.Unspecified
                                )
                            }
                            if (ouverts.contains(t)) {
                                CarteEvenement(
                                    titre = libelleType(t),
                                    typeActe = t,
                                    p = p,
                                    personnes = personnes,
                                    actes = actes.filter { it.personneId == p.id && it.type == t },
                                    onEditer = { blocEdite = t },
                                    onOuvrir = onOuvrir,
                                    onChoisir = onChoisir,
                                    onRetirer = onRetirer,
                                    onOuvrirMariage = { mariageOuvert = true },
                                    onOuvrirParent = { parentRole = it },
                                    onOuvrirTemoin = { temoinType = t },
                                    onRetirerTemoin = onRetirerTemoin,
                                    onBasculerSignature = onBasculerSignature,
                                    onAjouterActe = onAjouterActe,
                                    onSupprimerActe = onSupprimerActe,
                                    onVoirActe = onVoirActe,
                                    onEnregistrer = enregistrer,
                                    ouvrirEtatCivil = ouvrirEtatCivil,
                                    onValider = { v -> onValider(sectionDe(t), v) },
                                    pistes = pistes,
                                    modifier = Modifier
                                )
                            }
                            }
                        }
                        }
                        // Un bloc par enfant : en r\u00E9alit\u00E9 la fiche r\u00E9sum\u00E9e de l'enfant
                        // (pastille au num\u00E9ro, N. et D. avec commune et d\u00E9partement).
                        p.enfants.forEach { idE ->
                            val e = personneParId(personnes, idE)
                            Card(
                                Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, if (e != null) couleurPastille(e.sexe) else GrisManquant)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(6.dp).clickable { onOuvrir(idE) }
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        if (e != null) {
                                            PastilleNumero(p = e, diametre = 28)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(
                                            e?.titre() ?: "(fiche supprim\u00E9e)",
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { onRetirer("enfant", idE) }, Modifier.size(28.dp)) {
                                            Icon(Icons.Filled.Close, contentDescription = "Retirer cet enfant", Modifier.size(14.dp))
                                        }
                                    }
                                    if (e != null) {
                                        // R\u00E8gle des 100 ans : le d\u00E9c\u00E8s d'un enfant
                                        // ne s'affiche que s'il est connu ou possible.
                                        val lignesEnfant = mutableListOf(
                                            "N" to Triple(e.dateNaissance, e.lieuNaissance, e.deptNaissance)
                                        )
                                        if (decesAffichable(e)) lignesEnfant.add(
                                            "D" to Triple(e.dateDeces, e.lieuDeces, e.deptDeces)
                                        )
                                        lignesEnfant.forEach { paire ->
                                            val tEnf = paire.first
                                            val dEnf = paire.second.first.trim()
                                            val lEnf = paire.second.second.trim()
                                            val dpEnf = paire.second.third.trim()
                                            val numEnf = dpEnf.toIntOrNull() ?: numeroDepartement(dpEnf) ?: numeroDepartement(lEnf)
                                            val morceauxEnf = mutableListOf<String>()
                                            if (dEnf.isNotEmpty()) morceauxEnf.add(dateAffichee(dEnf))
                                            if (lEnf.isNotEmpty()) morceauxEnf.add(lEnf + (if (numEnf != null) " (" + numEnf.toString() + ")" else ""))
                                            if (morceauxEnf.isNotEmpty()) {
                                                Text(
                                                    tEnf + ". " + morceauxEnf.joinToString(" \u00E0 "),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Bloc SANS TITRE : saisie libre (notes quelconques).
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Column(Modifier.fillMaxWidth().padding(6.dp)) {
                                var libre by remember(p.id) { mutableStateOf(p.notesLibres) }
                                OutlinedTextField(
                                    value = libre,
                                    onValueChange = { libre = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Notes libres", style = MaterialTheme.typography.bodySmall) },
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                TextButton(onClick = { p.notesLibres = libre.trim(); enregistrer() }) {
                                    Text("Enregistrer", fontSize = 10.sp)
                                }
                            }
                        }
                        // Bloc RECHERCHES : pistes ouvertes de cette personne,
                        // supprimables directement.
                        val pistesPers = pistes.filter { it.personneId == p.id && !it.faite }
                        if (pistesPers.isNotEmpty()) {
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF9CBF9A))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(6.dp)) {
                                    Text(
                                        "Recherches",
                                        fontWeight = FontWeight.Bold,
                                        color = CouleurVertFonce,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    pistesPers.forEach { ps ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                ps.texte,
                                                Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(onClick = { onSupprimerPiste(ps) }, Modifier.size(28.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = "Supprimer cette recherche", Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                // Les boutons d'action (B, M, S, Enfant, Evenement) sont
                // FIXES en bas de la fiche (bottomBar du Scaffold).
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    // Dialogue d'edition du bloc : date, heure, commune, d\u00E9partement,
    // r\u00E9f\u00E9rence. Validation = contr\u00F4le de coh\u00E9rence + synchronisation.
    blocEdite?.let { t ->
        val c = champsEvenement(p, t)
        val ref = actes.filter { it.personneId == p.id && it.type == t && it.reference.isNotBlank() }
            .maxByOrNull { it.creation }?.reference?.trim() ?: ""
        DialogueEditionBloc(
            titre = libelleType(t),
            dateInit = c.date.trim(),
            heureInit = c.heure.trim(),
            communeInit = c.lieu.trim(),
            deptInit = c.dept.trim(),
            refInit = ref,
            onValider = { d, h, co, de, rf ->
                onModifierBloc(t, d, h, co, de, rf)
                blocEdite = null
            },
            onAnnuler = { blocEdite = null }
        )
    }

    if (mariageOuvert) {
        DialogueNouveauMariage(
            onValider = { nom, prenoms, date, commune, dept ->
                onNouveauMariage(nom, prenoms, date, commune, dept)
                mariageOuvert = false
            },
            onAnnuler = { mariageOuvert = false }
        )
    }

    if (enfantOuvert) {
        DialogueNouvelEnfant(
            onValider = { nom, prenoms, sexe, date ->
                onNouvelEnfant(nom, prenoms, sexe, date)
                enfantOuvert = false
            },
            onAnnuler = { enfantOuvert = false }
        )
    }

    if (parentRole.isNotEmpty()) {
        DialogueNouveauParent(
            role = parentRole,
            onValider = { nom, prenoms ->
                onNouveauParent(parentRole, nom, prenoms)
                parentRole = ""
            },
            onAnnuler = { parentRole = "" }
        )
    }

    if (temoinType.isNotEmpty()) {
        DialogueNouveauTemoin(
            personnes = personnes,
            onChoisir = { id ->
                onNouveauTemoin(temoinType, id, "", "")
                temoinType = ""
            },
            onCreer = { nom, prenoms ->
                onNouveauTemoin(temoinType, 0L, nom, prenoms)
                temoinType = ""
            },
            onAnnuler = { temoinType = "" }
        )
    }

    // Menu d'ajout d'un enfant : choisir une fiche existante ou saisir.
    if (menuEnfant) {
        AlertDialog(
            onDismissRequest = { menuEnfant = false },
            title = { Text("Ajouter un enfant") },
            text = {
                Column {
                    Text(
                        "Choisir une fiche existante ou saisir un nouvel enfant : aucun doublon ne sera cr\u00E9\u00E9.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { menuEnfant = false; onChoisir("enfant") },
                        Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text("Choisir un enfant") }
                    OutlinedButton(
                        onClick = { menuEnfant = false; enfantOuvert = true },
                        Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) { Text("Saisir un enfant") }
                }
            },
            confirmButton = {
                TextButton(onClick = { menuEnfant = false }) { Text("Fermer") }
            }
        )
    }

    // Bouton Evenement : choisit l'evenement a ouvrir et corriger.
    // N et D sont toujours dans la frise ; B, M et S y sont ajoutes.
    if (menuEvenement) {
        AlertDialog(
            onDismissRequest = { menuEvenement = false },
            title = { Text("\u00C9v\u00E9nement \u00E0 ouvrir") },
            text = {
                Column {
                    Text(
                        "Touchez un \u00E9v\u00E9nement : son bloc s'ouvre et son dialogue de correction s'affiche.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    listOf(
                        "N" to "Naissance",
                        "B" to "Bapt\u00EAme",
                        "M" to "Mariage",
                        "S" to "S\u00E9pulture",
                        "D" to "D\u00E9c\u00E8s"
                    ).filter { paire -> (paire.first != "S" && paire.first != "D") || decesAffichable(p, actes) }
                        .forEach { paire ->
                        OutlinedButton(
                            onClick = {
                                val t = paire.first
                                if (t == "B" || t == "M" || t == "S") {
                                    if (!p.blocsOuverts.contains(t)) p.blocsOuverts.add(t)
                                    enregistrer()
                                }
                                blocEdite = t
                                menuEvenement = false
                            },
                            Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) { Text(paire.second) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { menuEvenement = false }) { Text("Fermer") }
            }
        )
    }
}

// ---------------------------------------------------------------- Cartouche identite
// Pastille au numero + nom, profession, rappel des parents avec dates
// N. et D. Cartouche teinte selon le sexe. Appui long : edition.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteIdentite(
    p: Personne,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit,
    onValider: (Boolean) -> Unit,
    onEditer: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, couleurPastille(p.sexe))
    ) {
        // Appui long sur le bloc identite : ouverture de l'edition.
        Column(
            Modifier.fillMaxWidth().padding(6.dp)
                .combinedClickable(onClick = {}, onLongClick = onEditer)
        ) {
            // Bouton de couleur d'identification : du rouge (aucune des 4
            // informations) au vert (nom, prenoms, date et lieu de naissance
            // tous renseignes, identite confirmee au toucher).
            val remplis = listOf(p.nom, p.prenoms, p.dateNaissance, p.lieuNaissance).count { it.trim().isNotEmpty() }
            val validee = p.etatDe("identite") == "vert"
            val couleurBouton = if (validee) CouleurVert else when {
                remplis >= 4 -> Color(0xFF9CBF9A)
                remplis >= 2 -> Color(0xFFE8A33D)
                else -> CouleurRouge
            }
            Button(
                onClick = { onValider(!validee) },
                modifier = Modifier.fillMaxWidth().height(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = couleurBouton),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    if (validee) "Identit\u00E9 confirm\u00E9e" else "Identit\u00E9 " + remplis.toString() + "/4",
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            // Nom et numero deja affiches dans le titre : il ne reste ici que
            // la profession et le rappel du pere et de la mere. Pas d'edition.
            if (p.profession.trim().isNotEmpty()) {
                Text(p.profession.trim(), style = MaterialTheme.typography.bodySmall)
            }
            // Rappel des parents, avec leurs dates N. et D.
            listOf("P\u00E8re" to p.pere, "M\u00E8re" to p.mere).forEach { paire ->
                val libelle = paire.first
                val idParent = paire.second
                val q = personneParId(personnes, idParent)
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    if (q == null) {
                        Text("\u2014", style = MaterialTheme.typography.bodySmall, color = GrisManquant)
                    } else {
                        val nd = mutableListOf<String>()
                        val n = dateAffichee(q.dateNaissance.trim())
                        val d = dateAffichee(q.dateDeces.trim())
                        if (n.isNotEmpty()) nd.add("N. " + n)
                        if (d.isNotEmpty()) nd.add("D. " + d)
                        val parenthese = if (nd.isNotEmpty()) " (" + nd.joinToString(" \u2013 ") + ")" else ""
                        Text(
                            q.titre() + parenthese,
                            Modifier.weight(1f).clickable { onOuvrir(idParent) },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Bloc d'evenement
// Tout ramasse : date, heure, lieu, departement, parents (avec age a l'acte
// et date de naissance deduite a l'annee), conjoint, temoins.
// Icones : ajout d'une photo d'acte + recherche Internet aux AD.
// Les donnees manquantes sont grisees et synchronisees avec le journal.

fun libelleType(t: String): String = when (t) {
    "N" -> "Naissance"
    "B" -> "Bapt\u00EAme"
    "M" -> "Mariage"
    "D" -> "D\u00E9c\u00E8s"
    "S" -> "S\u00E9pulture"
    else -> "Autre"
}

// Champs date/heure/lieu/dept d'un evenement selon son type.
data class EvChamps(val date: String, val heure: String, val lieu: String, val dept: String)

fun champsEvenement(p: Personne, type: String): EvChamps = when (type) {
    "N" -> EvChamps(p.dateNaissance, p.heureNaissance, p.lieuNaissance, p.deptNaissance)
    "B" -> EvChamps(p.dateBapteme, p.heureBapteme, p.lieuBapteme, p.deptBapteme)
    "M" -> EvChamps(p.dateMariage, p.heureMariage, p.lieuMariage, p.deptMariage)
    "S" -> EvChamps(p.dateSepulture, p.heureSepulture, p.lieuSepulture, p.deptSepulture)
    else -> EvChamps(p.dateDeces, p.heureDeces, p.lieuDeces, p.deptDeces)
}

// Annee d'une date : gregorienne, ou conversion d'une date republicaine.
fun anneeDe(s: String): Int? =
    Regex("[0-9]{4}").find(s)?.value?.toIntOrNull()
        ?: repubAGregorien(s)?.takeLast(4)?.toIntOrNull()

// Extrait une ligne de la transcription du dernier acte du type (ex "T\u00E9moins : ...").
fun partieContenu(actes: List<Acte>, prefixe: String): String {
    val a = actes.filter { it.contenu.isNotBlank() }.maxByOrNull { it.creation } ?: return ""
    val ligne = a.contenu.split("\n").firstOrNull { it.startsWith(prefixe) } ?: return ""
    return ligne.removePrefix(prefixe).trim()
}

// Age du parent declare dans la transcription ("... (35 ans \u00E0 l'acte)").
fun ageDansContenu(actes: List<Acte>, prefixe: String): Int? {
    val a = actes.filter { it.contenu.isNotBlank() }.maxByOrNull { it.creation } ?: return null
    val ligne = a.contenu.split("\n").firstOrNull { it.startsWith(prefixe) } ?: return null
    return Regex("([0-9]{1,3}) ans \u00E0 l'acte").find(ligne)?.groupValues?.get(1)?.toIntOrNull()
}

// Ligne d'une personne liee (pere, mere, conjoint) : nom, profession, naissance,
// age a l'acte ou date de naissance deduite a l'annee, deces. Grisee si absente.
@Composable
fun LignePersonneG(
    libelle: String,
    q: Personne?,
    anneeActe: Int?,
    ageActeContenu: Int?,
    onOuvrir: (() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        if (q == null) {
            Text("\u2014", style = MaterialTheme.typography.bodySmall, color = GrisManquant)
        } else {
            val m = mutableListOf<String>()
            m.add(q.titre())
            if (q.profession.isNotBlank()) m.add(q.profession.trim())
            val an = anneeDe(q.dateNaissance)
            if (an != null) {
                var s = "n\u00E9(e) le " + dateAffichee(q.dateNaissance)
                if (anneeActe != null) s = s + " (" + (anneeActe - an).coerceAtLeast(0).toString() + " ans \u00E0 l'acte)"
                m.add(s)
            } else if (ageActeContenu != null && anneeActe != null) {
                m.add("n\u00E9(e) vers " + (anneeActe - ageActeContenu).toString())
            }
            if (q.lieuNaissance.isNotBlank()) m.add("\u00E0 " + q.lieuNaissance.trim())
            if (q.dateDeces.isNotBlank()) m.add("d\u00E9c\u00E9d\u00E9(e) le " + dateAffichee(q.dateDeces))
            // Appui direct sur le nom : ouverture de la fiche.
            Text(
                m.joinToString(", "),
                Modifier.weight(1f).clickable(enabled = onOuvrir != null) { onOuvrir?.invoke() },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Ligne toujours affichee : valeur ou em-dash gris si manquante.
@Composable
fun LigneG(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        if (valeur.isBlank()) {
            Text("\u2014", style = MaterialTheme.typography.bodySmall, color = GrisManquant)
        } else {
            Text(valeur, style = MaterialTheme.typography.bodySmall)
        }
    }
}


// ---------------------------------------------------------------- Bloc d'evenement
// Affiche un evenement de la frise : entete avec edition, date et heure sur
// la meme ligne, commune + numero de departement accole, parents choisis ou
// crees (naissance), conjoints et enfants (mariage), temoins avec coche de
// signature. Aucun temoin par defaut ; les doublons sont evites en amont.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteEvenement(
    titre: String,
    typeActe: String,
    p: Personne,
    personnes: List<Personne>,
    actes: List<Acte>,
    onEditer: () -> Unit,
    onOuvrir: (Long) -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit,
    onOuvrirMariage: () -> Unit,
    onOuvrirParent: (String) -> Unit,
    onOuvrirTemoin: () -> Unit,
    onRetirerTemoin: (String, Long) -> Unit,
    onBasculerSignature: (String, Long) -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    onEnregistrer: () -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit,
    // Coche de validation de l'acte : negative => recherche dans le journal.
    onValider: (Boolean) -> Unit,
    // Recherches ouvertes (la premiere de la section alimente le sous-bloc).
    pistes: List<Piste> = emptyList(),
    modifier: Modifier = Modifier
) {
    val c = champsEvenement(p, typeActe)
    val d = c.date.trim()
    val h = c.heure.trim()
    val l = c.lieu.trim()
    val dp = c.dept.trim()
    val anneeActe = anneeDe(d)
    val numDept = dp.toIntOrNull() ?: numeroDepartement(dp) ?: numeroDepartement(l)
    val etatActe = p.etatDe(sectionDe(typeActe))
    val pisteEv = pistes.firstOrNull { it.personneId == p.id && !it.faite && it.categorie.startsWith(sectionDe(typeActe)) }

    // Le bloc est maintenant affiche DANS la carte de l'evenement (v0.28) :
    // simple colonne, plus de carte imbriquee.
    Column(modifier.fillMaxWidth().padding(top = 4.dp)) {
        // Entete : titre + coche + edition du bloc + photo d'acte.
        // (La loupe AD est dans le sous-bloc de chaque acte.)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(titre, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Checkbox(
                checked = etatActe == "vert",
                onCheckedChange = { onValider(it) },
                modifier = Modifier.size(32.dp)
            )
            IconButton(onClick = onEditer) {
                Icon(Icons.Filled.Create, contentDescription = "Modifier ce bloc")
            }
            AjoutActeIcone(typeActe, onAjouterActe)
        }

        // Date et heure sur la meme ligne.
        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Date : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            if (d.isEmpty() && h.isEmpty()) {
                Text("\u2014", style = MaterialTheme.typography.bodySmall, color = GrisManquant)
            } else {
                val s = mutableListOf<String>()
                if (d.isNotEmpty()) s.add(dateAffichee(d))
                if (h.isNotEmpty()) s.add("\u00E0 " + heureAffichee(h))
                Text(s.joinToString(" "), style = MaterialTheme.typography.bodySmall)
            }
        }

        // Commune avec numero de departement accole, ex. Roubaix (59).
        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Commune : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            if (l.isEmpty()) {
                Text("\u2014", style = MaterialTheme.typography.bodySmall, color = GrisManquant)
            } else {
                Text(
                    l + (if (numDept != null) " (" + numDept.toString() + ")" else ""),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // (La reference de la cote AD est dans le sous-bloc de chaque acte.)

        // Naissance : pere (avec coche de signature) et mere, choisies parmi
        // les fiches existantes ou creees ; la synchronisation a lieu a la
        // validation dans MainActivity.
        if (typeActe == "N") {
            LignePersonneG(
                libelle = "P\u00E8re",
                q = personneParId(personnes, p.pere),
                anneeActe = anneeActe,
                ageActeContenu = ageDansContenu(actes, "P\u00E8re : "),
                onOuvrir = { onOuvrir(p.pere) }
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p.pere == 0L) {
                    OutlinedButton(onClick = { onChoisir("pere") }, Modifier.height(32.dp)) {
                        Text("Choisir", fontSize = 10.sp)
                    }
                    TextButton(onClick = { onOuvrirParent("pere") }) {
                        Text("Cr\u00E9er", fontSize = 10.sp)
                    }
                } else {
                    Checkbox(
                        checked = p.signatairesDe(typeActe).contains(0L),
                        onCheckedChange = { onBasculerSignature(typeActe, 0L) },
                        modifier = Modifier.size(28.dp)
                    )
                    Text("Signe", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { onRetirer("pere", 0L) }, Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer le p\u00E8re", Modifier.size(14.dp))
                    }
                }
            }
            LignePersonneG(
                libelle = "M\u00E8re",
                q = personneParId(personnes, p.mere),
                anneeActe = anneeActe,
                ageActeContenu = ageDansContenu(actes, "M\u00E8re : "),
                onOuvrir = { onOuvrir(p.mere) }
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p.mere == 0L) {
                    OutlinedButton(onClick = { onChoisir("mere") }, Modifier.height(32.dp)) {
                        Text("Choisir", fontSize = 10.sp)
                    }
                    TextButton(onClick = { onOuvrirParent("mere") }) {
                        Text("Cr\u00E9er", fontSize = 10.sp)
                    }
                } else {
                    IconButton(onClick = { onRetirer("mere", 0L) }, Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer la m\u00E8re", Modifier.size(14.dp))
                    }
                }
            }
        }

        // Mariage : conjoint et autres conjoints, puis les enfants. Chaque
        // personne est choisie parmi les fiches existantes (sans doublon) ou
        // saisie directement.
        if (typeActe == "M") {
            LignePersonneG(
                libelle = "Conjoint(e)",
                q = personneParId(personnes, p.conjoint),
                anneeActe = anneeActe,
                ageActeContenu = ageDansContenu(actes, "Mari\u00E9e : "),
                onOuvrir = { onOuvrir(p.conjoint) }
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p.conjoint == 0L) {
                    OutlinedButton(onClick = { onChoisir("conjoint") }, Modifier.height(32.dp)) {
                        Text("Choisir", fontSize = 10.sp)
                    }
                    TextButton(onClick = onOuvrirMariage) {
                        Text("Saisir", fontSize = 10.sp)
                    }
                } else {
                    IconButton(onClick = { onRetirer("conjoint", 0L) }, Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer le conjoint", Modifier.size(14.dp))
                    }
                }
            }
            p.conjointsSupp.forEach { idC ->
                LignePersonneG(
                    libelle = "Autre conjoint(e)",
                    q = personneParId(personnes, idC),
                    anneeActe = anneeActe,
                    ageActeContenu = null,
                    onOuvrir = { onOuvrir(idC) }
                )
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onRetirer("conjointsupp", idC) }, Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer ce conjoint", Modifier.size(14.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                OutlinedButton(onClick = { onChoisir("conjointsupp") }, Modifier.weight(1f)) {
                    Text("Autre conjoint", fontSize = 10.sp)
                }
                OutlinedButton(onClick = onOuvrirMariage, Modifier.weight(1f)) {
                    Text("Saisir un mariage", fontSize = 10.sp)
                }
            }

        }
        // Temoins : aucun par defaut, choisis parmi les fiches existantes ou
        // crees ; coche de signature devant chaque temoin.
        val temoins = p.temoinsDe(typeActe)
        if (temoins.isNotEmpty()) {
            Text(
                "T\u00E9moins (" + temoins.size.toString() + ")",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            temoins.forEach { idT ->
                val t = personneParId(personnes, idT)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t?.titre() ?: "(fiche supprim\u00E9e)",
                        Modifier.weight(1f).clickable { onOuvrir(idT) },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Checkbox(
                        checked = p.signatairesDe(typeActe).contains(idT),
                        onCheckedChange = { onBasculerSignature(typeActe, idT) },
                        modifier = Modifier.size(28.dp)
                    )
                    IconButton(onClick = { onRetirerTemoin(typeActe, idT) }, Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer ce t\u00E9moin", Modifier.size(14.dp))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            OutlinedButton(onClick = onOuvrirTemoin) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Ajouter un t\u00E9moin")
            }
        }

        // Actes stockes pour cet evenement (images + transcriptions) :
        // chacun dans son SOUS-BLOC avec sources, recherche et lien AD.
        if (actes.isNotEmpty()) {
            Text(
                "Actes stock\u00E9s (" + actes.size.toString() + ")",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            actes.forEach { acte ->
                SousBlocActe(
                    acte = acte,
                    piste = pisteEv,
                    ouvrirAD = {
                        ouvrirEtatCivil(l, numDept, libelleType(typeActe), anneeActe?.toString() ?: "")
                    },
                    onVoir = { onVoirActe(acte) },
                    onSupprimer = { onSupprimerActe(acte) },
                    onEnregistrer = onEnregistrer
                )
            }
        }
    }
}

// Icone d'ajout d'une photo d'acte dans un bloc.
@Composable
fun AjoutActeIcone(typeActe: String, onAjouterActe: (Uri, String, String) -> Unit) {
    var dialogueOuvert by remember { mutableStateOf(false) }
    val lanceur = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAjouterActe(uri, typeActe, "")
    }
    IconButton(onClick = { dialogueOuvert = true }) {
        Icon(Icons.Filled.Add, contentDescription = "Ajouter une photo d'acte")
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

// Sous-bloc de detail d'un acte (v0.28) : titre (toucher = voir l'image,
// appui long = pages/reference), reference de la cote, zone de recherche
// libre et loupe vers les AD, le tout regroupe au meme endroit.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SousBlocActe(
    acte: Acte,
    piste: Piste?,
    ouvrirAD: () -> Unit,
    onVoir: () -> Unit,
    onSupprimer: () -> Unit,
    onEnregistrer: () -> Unit
) {
    var pageOuvert by remember { mutableStateOf(false) }
    var note by remember(acte.id) { mutableStateOf(piste?.enCours ?: "") }
    Card(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6FAF5)),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
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
                IconButton(onClick = ouvrirAD, Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = "Rechercher cet acte aux AD", Modifier.size(14.dp))
                }
                IconButton(onClick = onSupprimer, Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Supprimer", Modifier.size(14.dp))
                }
            }
            if (acte.reference.isNotBlank()) {
                Text(
                    "R\u00E9f. " + acte.reference.trim(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Zone de saisie libre de la recherche en cours.
            if (piste != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Recherche : pages, cotes, notes...", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { piste.enCours = note.trim(); onEnregistrer() }) {
                        Text("Enregistrer", fontSize = 10.sp)
                    }
                }
            }
        }
    }
    if (pageOuvert) {
        DialoguePageActe(
            acte = acte,
            onValider = { num, total, ref ->
                acte.numPage = num
                acte.totalPage = total
                acte.reference = ref
                onEnregistrer()
                pageOuvert = false
            },
            onAnnuler = { pageOuvert = false }
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
            onValider = { num, total, ref ->
                acte.numPage = num
                acte.totalPage = total
                acte.reference = ref
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
    onValider: (String, String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var num by remember { mutableStateOf(acte.numPage) }
    var total by remember { mutableStateOf(acte.totalPage) }
    var ref by remember { mutableStateOf(acte.reference) }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Page et r\u00E9f\u00E9rence de l'acte") },
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
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("R\u00E9f\u00E9rence de l'acte (cote AD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
                Text(
                    "Cote aux Archives d\u00E9partementales, export\u00E9e vers Heredis (champ Source).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(num.trim(), total.trim(), ref.trim()) }) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// Ligne masquee si vide (liens de la famille).
@Composable
fun Ligne(libelle: String, valeur: String) {
    if (valeur.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(valeur, style = MaterialTheme.typography.bodyMedium)
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
                    if (acte.contenu.isNotBlank()) {
                        // Acte transcrit sans image : la transcription s'affiche ici.
                        Text(
                            "Transcription de l'acte",
                            fontWeight = FontWeight.Bold,
                            color = CouleurVertFonce
                        )
                        Column(
                            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            acte.contenu.split("\n").forEach { ligne ->
                                Text(
                                    ligne,
                                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Text("Image illisible.", color = CouleurRouge)
                    }
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

