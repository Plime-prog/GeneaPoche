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
import androidx.compose.material3.TopAppBarDefaults
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

// ---------------------------------------------------------------- Fiche personne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPersonne(
    personnes: List<Personne>,
    pistes: List<Piste>,
    actes: List<Acte>,
    p: Personne,
    onOuvrir: (Long) -> Unit,
    onRetour: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit,
    onEtat: (String, String) -> Unit,
    onNouvellePiste: () -> Unit,
    onBasculerPiste: (Piste) -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    ouvrirAd: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(p.titre()) },
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
                CarteIdentite(
                    p,
                    onEtat = { v -> onEtat("identite", v) }
                )
            }
            item {
                CarteEvenement(
                    "Naissance", p.dateNaissance, p.lieuNaissance,
                    p.etatDe("naissance"),
                    onEtat = { v -> onEtat("naissance", v) }
                )
            }
            item {
                CarteEvenement(
                    "Mariage", p.dateMariage, p.lieuMariage,
                    p.etatDe("mariage"),
                    onEtat = { v -> onEtat("mariage", v) }
                )
            }
            item {
                CarteEvenement(
                    "D\u00E9c\u00E8s", p.dateDeces, p.lieuDeces,
                    p.etatDe("deces"),
                    onEtat = { v -> onEtat("deces", v) }
                )
            }
            item {
                CarteFamille(
                    personnes = personnes,
                    p = p,
                    onOuvrir = onOuvrir,
                    onChoisir = onChoisir,
                    onRetirer = onRetirer
                )
            }
            item {
                CartePistesPersonne(
                    pistes = pistes,
                    p = p,
                    onNouvellePiste = onNouvellePiste,
                    onBasculer = onBasculerPiste
                )
            }
            item {
                CarteActesPersonne(
                    actes = actes,
                    p = p,
                    onAjouterActe = onAjouterActe,
                    onSupprimerActe = onSupprimerActe,
                    onVoirActe = onVoirActe
                )
            }
            item {
                CarteArchivesPersonne(p = p, onOuvrirAd = ouvrirAd)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun CarteIdentite(p: Personne, onEtat: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            EnteteAvecPastilles("Identit\u00E9", p.etatIdentite, onEtat)
            Ligne("Sosa", p.sosa.trim())
            Ligne("Num\u00E9ro lat\u00E9ral (d'Aboville)", p.lateral.trim())
            Ligne("Nom", p.nom.trim())
            Ligne("Pr\u00E9noms", p.prenoms.trim())
            Ligne("Sexe", when (p.sexe) { "M" -> "Masculin"; "F" -> "F\u00E9minin"; else -> "" })
            Ligne("Profession", p.profession.trim())
        }
    }
}

@Composable
fun CarteEvenement(
    titre: String,
    date: String,
    lieu: String,
    etat: String,
    onEtat: (String) -> Unit
) {
    val d = date.trim()
    val l = lieu.trim()
    if (d.isEmpty() && l.isEmpty() && etat.isEmpty()) return
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            EnteteAvecPastilles(titre, etat, onEtat)
            Ligne("Date", d)
            Ligne("Lieu", l)
        }
    }
}

@Composable
fun EnteteAvecPastilles(titre: String, etat: String, onEtat: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(titre, Modifier.weight(1f), fontWeight = FontWeight.Bold)
        FilterChip(
            selected = etat == "vert",
            onClick = { onEtat(if (etat == "vert") "" else "vert") },
            label = { Text("V\u00E9rifi\u00E9") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = CouleurVert.copy(alpha = 0.15f),
                labelColor = CouleurVert,
                selectedContainerColor = CouleurVert,
                selectedLabelColor = Color.White
            )
        )
        Spacer(Modifier.width(6.dp))
        FilterChip(
            selected = etat == "rouge",
            onClick = { onEtat(if (etat == "rouge") "" else "rouge") },
            label = { Text("\u00C0 v\u00E9rifier") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = CouleurRouge.copy(alpha = 0.15f),
                labelColor = CouleurRouge,
                selectedContainerColor = CouleurRouge,
                selectedLabelColor = Color.White
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

@Composable
fun CarteFamille(
    personnes: List<Personne>,
    p: Personne,
    onOuvrir: (Long) -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Famille", fontWeight = FontWeight.Bold)
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

            Spacer(Modifier.height(6.dp))
            Text("Enfants", fontWeight = FontWeight.Bold)
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
                        Modifier.weight(1f).clickable { if (e != null) onOuvrir(idEnfant) },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onRetirer("enfant", idEnfant) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer")
                    }
                }
            }
            OutlinedButton(onClick = { onChoisir("enfant") }, Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Ajouter un enfant")
            }
        }
    }
}

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
                Modifier.weight(1f).clickable { onOuvrir(cible) },
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onRetirer) {
                Icon(Icons.Filled.Close, contentDescription = "Retirer")
            }
        }
    }
}

@Composable
fun CartePistesPersonne(
    pistes: List<Piste>,
    p: Personne,
    onNouvellePiste: () -> Unit,
    onBasculer: (Piste) -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Pistes de recherche", fontWeight = FontWeight.Bold)
            val deCettePersonne = pistes.filter { it.personneId == p.id }
            if (deCettePersonne.isEmpty()) {
                Text("Aucune piste pour cette personne.", style = MaterialTheme.typography.bodySmall)
            }
            deCettePersonne.forEach { piste ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = piste.faite,
                        onCheckedChange = { onBasculer(piste) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CouleurVert,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        piste.texte,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (piste.faite) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                    )
                }
            }
            OutlinedButton(onClick = onNouvellePiste, Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Nouvelle piste pour cette personne")
            }
        }
    }
}

// ---------------------------------------------------------------- Actes (images chiffrees)

fun libelleType(t: String): String = when (t) {
    "N" -> "Naissance"
    "M" -> "Mariage"
    "D" -> "D\u00E9c\u00E8s"
    else -> "Autre"
}

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

@Composable
fun CarteArchivesPersonne(
    p: Personne,
    onOuvrirAd: (Int) -> Unit
) {
    val lieux = listOf(p.lieuNaissance, p.lieuMariage, p.lieuDeces)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val numeros = lieux.mapNotNull { numeroDepartement(it) }.distinct().sorted()

    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Archives d\u00E9partementales", fontWeight = FontWeight.Bold)
            if (numeros.isEmpty()) {
                Text(
                    "Renseignez un lieu au format \u00AB Ville (n\u00B0 du d\u00E9partement) \u00BB (ex : Roubaix (59)) pour ouvrir directement les archives comp\u00E9tentes.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                numeros.forEach { n ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "AD " + n + " \u2013 " + (NomsDepartements[n] ?: ""),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { onOuvrirAd(n) }) { Text("Ouvrir") }
                    }
                }
                Text(
                    "Le navigateur s'ouvre ; G\u00E9n\u00E9aPoche n'a aucune autorisation Internet et n'envoie aucune donn\u00E9e : seule la recherche publique des AD du d\u00E9partement est lanc\u00E9e.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CarteActesPersonne(
    actes: List<Acte>,
    p: Personne,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit
) {
    var dialogueOuvert by remember { mutableStateOf(false) }
    var typeEnAttente by remember { mutableStateOf("N") }
    var titreEnAttente by remember { mutableStateOf("") }

    val lanceur = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAjouterActe(uri, typeEnAttente, titreEnAttente)
        }
        typeEnAttente = "N"
        titreEnAttente = ""
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Actes et images", fontWeight = FontWeight.Bold)
            val deCettePersonne = actes.filter { it.personneId == p.id }
            if (deCettePersonne.isEmpty()) {
                Text(
                    "Aucun acte stock\u00E9. Importez une capture d'\u00E9cran ou une photo d'acte (elle sera chiffr\u00E9e).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            deCettePersonne.forEach { acte ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        libelleType(acte.type),
                        Modifier.clickable { onVoirActe(acte) },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    val t = acte.titre.trim()
                    Text(
                        if (t.isNotEmpty()) t else "(sans titre)",
                        Modifier.weight(1f).clickable { onVoirActe(acte) },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onSupprimerActe(acte) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Supprimer")
                    }
                }
            }
            OutlinedButton(onClick = { dialogueOuvert = true }, Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Ajouter un acte")
            }
        }
    }

    if (dialogueOuvert) {
        DialogueNouvelActe(
            onValider = { type, titre ->
                typeEnAttente = type
                titreEnAttente = titre
                dialogueOuvert = false
                lanceur.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onAnnuler = { dialogueOuvert = false }
        )
    }
}

@Composable
fun DialogueNouvelActe(
    onValider: (String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var type by remember { mutableStateOf("N") }
    var titre by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvel acte") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = type == "N", onClick = { type = "N" }, label = { Text("Naissance") })
                    FilterChip(selected = type == "M", onClick = { type = "M" }, label = { Text("Mariage") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = type == "D", onClick = { type = "D" }, label = { Text("D\u00E9c\u00E8s") })
                    FilterChip(selected = type == "A", onClick = { type = "A" }, label = { Text("Autre") })
                }
                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it },
                    label = { Text("Titre (facultatif)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    "L'image reste sur l'appareil, chiffr\u00E9e. Rien n'est transmis.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(type, titre.trim()) }) { Text("Choisir l'image") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

@Composable
fun VisionneuseActe(ctx: Context, acte: Acte, onFermer: () -> Unit) {
    val bmp = remember(acte.id) {
        val octets = Stockage.lireImage(ctx, acte.id)
        if (octets != null) BitmapFactory.decodeByteArray(octets, 0, octets.size) else null
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
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Dialogue edition

@Composable
fun DialogueEdition(
    cible: Personne?,
    idNouvelle: Long,
    onValider: (Personne) -> Unit,
    onAnnuler: () -> Unit
) {
    var sosa by remember { mutableStateOf(cible?.sosa ?: "") }
    var lateral by remember { mutableStateOf(cible?.lateral ?: "") }
    var nom by remember { mutableStateOf(cible?.nom ?: "") }
    var prenoms by remember { mutableStateOf(cible?.prenoms ?: "") }
    var sexe by remember { mutableStateOf(cible?.sexe ?: "") }
    var profession by remember { mutableStateOf(cible?.profession ?: "") }
    var dateNaissance by remember { mutableStateOf(cible?.dateNaissance ?: "") }
    var lieuNaissance by remember { mutableStateOf(cible?.lieuNaissance ?: "") }
    var dateMariage by remember { mutableStateOf(cible?.dateMariage ?: "") }
    var lieuMariage by remember { mutableStateOf(cible?.lieuMariage ?: "") }
    var dateDeces by remember { mutableStateOf(cible?.dateDeces ?: "") }
    var lieuDeces by remember { mutableStateOf(cible?.lieuDeces ?: "") }

    fun construire(): Personne {
        val base = cible ?: Personne(id = idNouvelle)
        base.sosa = sosa.trim()
        base.lateral = lateral.trim()
        base.nom = nom.trim()
        base.prenoms = prenoms.trim()
        base.sexe = sexe
        base.profession = profession.trim()
        base.dateNaissance = dateNaissance.trim()
        base.lieuNaissance = lieuNaissance.trim()
        base.dateMariage = dateMariage.trim()
        base.lieuMariage = lieuMariage.trim()
        base.dateDeces = dateDeces.trim()
        base.lieuDeces = lieuDeces.trim()
        return base
    }

    val formulaireValide =
        (nom.isNotBlank() || prenoms.isNotBlank()) &&
        sosaValide(sosa) && lateralValide(lateral)

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(if (cible == null) "Ajouter une personne" else "Modifier la personne") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Identit\u00E9", fontWeight = FontWeight.Bold)
                Champ("Sosa (1 = de cujus)", sosa, { sosa = it },
                    erreur = !sosaValide(sosa), aide = "Num\u00E9ro de la lign\u00E9e directe")
                Champ("Num\u00E9ro lat\u00E9ral (ex 1.3)", lateral, { lateral = it },
                    erreur = !lateralValide(lateral), aide = "Descendants hors lign\u00E9e directe")
                Champ("Nom", nom, { nom = it })
                Champ("Pr\u00E9noms", prenoms, { prenoms = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sexe == "M", onClick = { sexe = "M" }, label = { Text("M") })
                    FilterChip(selected = sexe == "F", onClick = { sexe = "F" }, label = { Text("F") })
                    FilterChip(selected = sexe.isEmpty(), onClick = { sexe = "" }, label = { Text("?") })
                }
                Champ("Profession", profession, { profession = it })

                Spacer(Modifier.height(10.dp))
                Text("Naissance", fontWeight = FontWeight.Bold)
                Champ("Date", dateNaissance, { dateNaissance = it })
                Champ("Lieu", lieuNaissance, { lieuNaissance = it })

                Spacer(Modifier.height(10.dp))
                Text("Mariage", fontWeight = FontWeight.Bold)
                Champ("Date", dateMariage, { dateMariage = it })
                Champ("Lieu", lieuMariage, { lieuMariage = it })

                Spacer(Modifier.height(10.dp))
                Text("D\u00E9c\u00E8s", fontWeight = FontWeight.Bold)
                Champ("Date", dateDeces, { dateDeces = it })
                Champ("Lieu", lieuDeces, { lieuDeces = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(construire()) },
                enabled = formulaireValide
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

@Composable
fun Champ(
    libelle: String,
    valeur: String,
    onChange: (String) -> Unit,
    erreur: Boolean = false,
    aide: String = ""
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(libelle) },
        isError = erreur,
        singleLine = true,
        supportingText = if (aide.isNotEmpty()) { { Text(aide, style = MaterialTheme.typography.bodySmall) } } else null,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}
