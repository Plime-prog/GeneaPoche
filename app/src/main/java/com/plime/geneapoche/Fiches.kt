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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
// Blocs modifiables par appui long uniquement. Les informations manquantes
// sont affichees en gris : elles alimentent le journal de recherches.

val GrisManquant = Color(0xFF9AA099)

@OptIn(ExperimentalMaterial3Api::class)
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
            item { CarteIdentite(p = p, onModifier = onModifier) }
            item {
                CarteEvenement(
                    titre = "Naissance",
                    typeActe = "N",
                    p = p,
                    personnes = personnes,
                    actes = actes.filter { it.personneId == p.id && it.type == "N" },
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
                        typeActe = "B",
                        p = p,
                        personnes = personnes,
                        actes = actes.filter { it.personneId == p.id && it.type == "B" },
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
                    typeActe = "M",
                    p = p,
                    personnes = personnes,
                    actes = actes.filter { it.personneId == p.id && it.type == "M" },
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
                    typeActe = "D",
                    p = p,
                    personnes = personnes,
                    actes = actes.filter { it.personneId == p.id && it.type == "D" },
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
                        typeActe = "S",
                        p = p,
                        personnes = personnes,
                        actes = actes.filter { it.personneId == p.id && it.type == "S" },
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
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ---------------------------------------------------------------- Carte identite
// Rappel du nom et prenom, puis profession dessous. Appui long : modifier.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteIdentite(p: Personne, onModifier: () -> Unit) {
    Card(
        Modifier.fillMaxWidth()
            .padding(vertical = 5.dp)
            .combinedClickable(onClick = {}, onLongClick = onModifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(p.titre(), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = CouleurVertFonce)
            val num = p.numeroAffiche()
            if (num.isNotEmpty()) {
                Text(num, style = MaterialTheme.typography.bodySmall)
            }
            LigneG("Profession", p.profession.trim())
            if (p.sexe == "M" || p.sexe == "F") {
                LigneG("Sexe", if (p.sexe == "M") "Masculin" else "F\u00E9minin")
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

fun anneeDe(s: String): Int? = Regex("[0-9]{4}").find(s)?.value?.toIntOrNull()

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
fun LignePersonneG(libelle: String, q: Personne?, anneeActe: Int?, ageActeContenu: Int?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        if (q == null) {
            Text("\u2014", style = MaterialTheme.typography.bodyMedium, color = GrisManquant)
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
            Text(m.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Ligne toujours affichee : valeur ou em-dash gris si manquante.
@Composable
fun LigneG(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(libelle + " : ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        if (valeur.isBlank()) {
            Text("\u2014", style = MaterialTheme.typography.bodyMedium, color = GrisManquant)
        } else {
            Text(valeur, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteEvenement(
    titre: String,
    typeActe: String,
    p: Personne,
    personnes: List<Personne>,
    actes: List<Acte>,
    onModifier: () -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    onEnregistrer: () -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit
) {
    val c = champsEvenement(p, typeActe)
    val d = c.date.trim()
    val l = c.lieu.trim()
    val dp = c.dept.trim()
    val anneeActe = anneeDe(d)

    Card(
        Modifier.fillMaxWidth()
            .padding(vertical = 5.dp)
            .combinedClickable(onClick = {}, onLongClick = onModifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            // Entete : titre + icone photo d'acte + icone recherche Internet (AD).
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(titre, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                AjoutActeIcone(typeActe, onAjouterActe)
                val numDept = dp.toIntOrNull() ?: numeroDepartement(l) ?: numeroDepartement(dp)
                IconButton(
                    onClick = {
                        ouvrirEtatCivil(l, numDept, libelleType(typeActe), anneeActe?.toString() ?: "")
                    },
                    enabled = l.isNotEmpty() || numDept != null
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Rechercher cet acte aux AD")
                }
            }

            LigneG("Date", dateAffichee(d))
            LigneG("Heure", heureAffichee(c.heure.trim()))
            LigneG("Lieu", l)
            LigneG("D\u00E9partement", dp)
            LigneG(
                "R\u00E9f\u00E9rence",
                actes.filter { it.personneId == p.id && it.type == typeActe && it.reference.isNotBlank() }
                    .maxByOrNull { it.creation }?.reference?.trim() ?: ""
            )

            // Peres et meres (naissance et deces) : fiche liee si elle existe,
            // sinon age/deduction depuis la transcription de l'acte.
            if (typeActe == "N" || typeActe == "D") {
                LignePersonneG(
                    libelle = "P\u00E8re",
                    q = personneParId(personnes, p.pere),
                    anneeActe = anneeActe,
                    ageActeContenu = ageDansContenu(actes, "P\u00E8re : ")
                )
                LignePersonneG(
                    libelle = "M\u00E8re",
                    q = personneParId(personnes, p.mere),
                    anneeActe = anneeActe,
                    ageActeContenu = ageDansContenu(actes, "M\u00E8re : ")
                )
            }

            // Conjoint pour le mariage, avec ses renseignements.
            if (typeActe == "M") {
                LignePersonneG(
                    libelle = "Conjoint(e)",
                    q = personneParId(personnes, p.conjoint),
                    anneeActe = anneeActe,
                    ageActeContenu = ageDansContenu(actes, "Mari\u00E9e : ")
                )
            }

            // Temoins, depuis la transcription du dernier acte de ce type.
            LigneG("T\u00E9moins", partieContenu(actes, "T\u00E9moins : "))
            LigneG("Signataires", partieContenu(actes, "Signataires : "))

            // Actes stockes pour cet evenement (images + transcriptions).
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

