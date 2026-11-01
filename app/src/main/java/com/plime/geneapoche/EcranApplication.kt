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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ Application deverrouillee

// Barre d'onglets de l'application : TOUJOURS visible en bas, fiche ouverte
// ou non (dans ce cas, choisir un onglet ferme aussi la fiche).
@Composable
fun BarreOnglets(onglet: Int, onChoisir: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = onglet == 0,
            onClick = { onChoisir(0) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Personnes") }
        )
        NavigationBarItem(
            selected = onglet == 1,
            onClick = { onChoisir(1) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Journal") }
        )
        NavigationBarItem(
            selected = onglet == 2,
            onClick = { onChoisir(2) },
            icon = { Icon(painterResource(R.drawable.ic_arbre), contentDescription = null) },
            label = { Text("Arbre") }
        )
        NavigationBarItem(
            selected = onglet == 3,
            onClick = { onChoisir(3) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            label = { Text("Saisie") }
        )
    }
}

// G2-09 : l'etat de l'ecran (donnees affichees) regroupe en une seule classe,
// pour ramener la signature d'EcranApplication a 2 parametres.
class EtatApplication(
    val personnes: List<Personne>,
    val cujus: Personne?,
    val pistes: List<Piste>,
    val actes: List<Acte>,
    val charge: Boolean,
    val message: String,
    val idOuvert: Long,
    val onglet: Int,
    val editionOuverte: Boolean,
    val editionCible: Personne?,
    val pickerRole: String,
    val confirmSuppression: Personne?,
    val arbreCentre: Long,
    // G3-17 : largeur etendue (tablette/paysage) pour une mise en page
    // adaptative (liste sur deux colonnes).
    val deuxColonnes: Boolean = false,
    // G3-19 : une suppression de personne vient d'avoir lieu (Snackbar
    // avec "Annuler").
    val annulationPossible: Boolean = false
)

// G2-09 : toutes les actions de l'ecran regroupees en une seule classe.
class ActionsApplication(
    val onMessage: (String) -> Unit,
    val onIdOuvert: (Long) -> Unit,
    val onRetourFiche: () -> Unit,
    val onOnglet: (Int) -> Unit,
    val onEditionOuverte: (Boolean) -> Unit,
    val onEditionCible: (Personne?) -> Unit,
    val onPickerRole: (String) -> Unit,
    val onConfirmSuppression: (Personne?) -> Unit,
    val enregistrer: () -> Unit,
    val supprimer: (Personne) -> Unit,
    val lier: (String, Long) -> Unit,
    val delier: (String, Long) -> Unit,
    val ajouterMariage: (String, String, String, String, String) -> Unit,
    val nouveauParent: (String, String, String) -> Unit,
    val nouveauTemoin: (String, Long, String, String) -> Unit,
    val retirerTemoin: (String, Long) -> Unit,
    val basculerSignature: (String, Long) -> Unit,
    val majEvenement: (String, String, String, String, String, String) -> Unit,
    val ajouterEnfant: (String, String, String, String) -> Unit,
    val definirCujus: (Personne) -> Unit,
    val choisirCujus: () -> Unit,
    val renumeroter: (Long) -> Unit,
    val supprimerPiste: (Piste) -> Unit,
    val ajouterActe: (Long, Uri, String, String) -> Unit,
    val onSupprimerActe: (Acte) -> Unit,
    val onVoirActe: (Acte) -> Unit,
    val onArbreCentre: (Long) -> Unit,
    val exporterGedcom: (Uri, Set<Long>?, Boolean) -> Unit,
    val importerGedcom: (Uri) -> Unit,
    val viderBase: () -> Unit,
    val enregistrerActe: (ActeSaisie) -> Unit,
    val ouvrirLien: (String) -> Unit,
    val ouvrirEtatCivil: (String, Int?, String, String) -> Unit,
    val sauvegarderBase: (Uri, String) -> Unit,
    val restaurerSauvegarde: (Uri, String) -> Unit,
    val restaurerImport: () -> Unit,
    val changerPin: () -> Unit,
    val reglagesVerrouillage: () -> Unit,
    val reglagesTheme: () -> Unit,
    // G3-19 : annulation d'une suppression via le Snackbar.
    val onAnnulerSuppression: () -> Unit,
    val onPurgerAnnulation: () -> Unit,
    // G2-18 residuel : identifiants strictement croissants aussi pour les
    // nouvelles personnes creees par le dialogue d'edition.
    val prochainId: () -> Long,
    val validerInfo: (Personne, String, Boolean) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EcranApplication(
    etat: EtatApplication,
    actions: ActionsApplication
) {
    with(etat) {
    with(actions) {
    BackHandler(enabled = idOuvert != 0L || editionOuverte || pickerRole.isNotEmpty()) {
        when {
            pickerRole.isNotEmpty() -> onPickerRole("")
            editionOuverte -> onEditionOuverte(false)
            else -> onIdOuvert(0L)
        }
    }

    val ouverte = personneParId(personnes, idOuvert)
    var menuGedcom by remember { mutableStateOf(false) }
    var exclureVivants by remember { mutableStateOf(false) }
    var confirmeVider by remember { mutableStateOf(false) }
    // G3-13 : la barre de titre se replie au defilement des listes.
    val comportementBarre = TopAppBarDefaults.enterAlwaysScrollBehavior()
    // G3-19 : retours utilisateur en Snackbar (plus de dialogue "Information"),
    // avec "Annuler" apres une suppression de personne.
    val etatSnackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            val apresSuppression = message == "Personne supprim\u00E9e." && annulationPossible
            val resultat = etatSnackbar.showSnackbar(
                message,
                actionLabel = if (apresSuppression) "Annuler" else null,
                duration = SnackbarDuration.Short
            )
            onMessage("")
            if (resultat == SnackbarResult.ActionPerformed) {
                onAnnulerSuppression()
            } else if (apresSuppression) {
                onPurgerAnnulation()
            }
        }
    }

    if (ouverte == null) {
        Scaffold(
            modifier = Modifier.nestedScroll(comportementBarre.nestedScrollConnection),
            snackbarHost = { SnackbarHost(etatSnackbar) },
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
                                    color = CouleurSurvol
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
                    ),
                    scrollBehavior = comportementBarre
                )
            },
            bottomBar = { BarreOnglets(onglet, onOnglet) },
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
            // G1-08 : phrase secrete + lanceurs .gpbak (export et import).
            var phraseSecrete by remember { mutableStateOf("") }
            var phraseEnAttente by remember { mutableStateOf("") }
            var demandePhraseExport by remember { mutableStateOf(false) }
            var demandePhraseImport by remember { mutableStateOf(false) }
            val lanceurGpbakExport = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) sauvegarderBase(uri, phraseEnAttente)
            }
            val lanceurGpbakImport = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) restaurerSauvegarde(uri, phraseEnAttente)
            }

            val lanceurGedcom = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) exporterGedcom(uri, null, exclureVivants)
            }
            val lanceurGedcomBranche = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) {
                    val sel = exportBrancheId
                    exporterGedcom(uri, if (sel != 0L) brancheDe(personnes, sel) else null, exclureVivants)
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
                        Text("Touchez le bouton + pour ajouter la premi\u00E8re personne (de cujus, n\u00B0 1).")
                    }
                } else {
                    // Onglet PERSONNES : compteur, tri A-Z / Aboville / Branches, index, cartes.
                    // triPersonnes : 0 = A-Z, 1 = Aboville (n croissant), 2 = Branches.
                    var triPersonnes by remember { mutableStateOf(0) }
                    val triees = personnes.sortedWith(
                        compareBy(
                            { it.nom.trim().uppercase() },
                            { it.prenoms.trim().uppercase() },
                            { anneeNaissance(it) }
                        )
                    )
                    val etatListe = rememberLazyListState()
                    // A-Z : lettre touchee = FILTRE (seuls les noms qui
                    // commencent par elle sont affiches) ; re-toucher efface.
                    var lettreChoisie by remember { mutableStateOf<Char?>(null) }
                    // Branches : id du grand-parent choisi (filtre), null = tout.
                    var brancheChoisie by remember { mutableStateOf<Long?>(null) }
                    // G3-14 : recherche immediate par nom ou lieu.
                    var recherche by remember { mutableStateOf("") }
                    val lettresUtilisees = triees
                        .map { it.nom.trim().uppercase().firstOrNull() ?: '?' }
                        .toSet()

                    // Classement par branche : la racine est l'ancetre le plus haut
                    // (pere, sinon mere) ; la branche porte le nom de cette racine.
                    // Classement Aboville : numeros (Sosa ou d'Aboville) croissants,
                    // segments compares un a un, non numeros en fin de liste.
                    // Les 4 grands-parents du de cujus : une branche chacun.
                    val pereC = personneParId(personnes, cujus?.pere ?: 0L)
                    val mereC = personneParId(personnes, cujus?.mere ?: 0L)
                    val grandsParents = listOfNotNull(
                        personneParId(personnes, pereC?.pere ?: 0L),
                        personneParId(personnes, pereC?.mere ?: 0L),
                        personneParId(personnes, mereC?.pere ?: 0L),
                        personneParId(personnes, mereC?.mere ?: 0L)
                    ).distinctBy { it.id }

                    // Liste affichee selon le tri et les filtres :
                    // A-Z : tout, ou la lettre choisie ; Aboville : n croissant ;
                    // Branches : tout, ou les personnes liees au grand-parent
                    // choisi (par les liens, quel que soit leur nom).
                    val parAboville = personnes.sortedWith(
                        ComparateurAboville
                            .thenComparator { a, b -> a.nom.trim().uppercase().compareTo(b.nom.trim().uppercase()) }
                            .thenComparator { a, b -> a.prenoms.trim().uppercase().compareTo(b.prenoms.trim().uppercase()) }
                    )
                    val entrees: List<Any> = if (triPersonnes == 0) {
                        if (lettreChoisie == null) {
                            triees.toList()
                        } else {
                            triees.filter { (it.nom.trim().uppercase().firstOrNull() ?: '?') == lettreChoisie }
                        }
                    } else if (triPersonnes == 1) {
                        parAboville
                    } else {
                        val membres = brancheChoisie?.let { b -> brancheDe(b, personnes) } ?: emptySet()
                        if (brancheChoisie == null) parAboville else parAboville.filter { it.id in membres }
                    }
                    // G3-14 : la recherche filtre sur nom, prenoms et lieux.
                    val entreesFiltrees: List<Any> = if (recherche.isBlank()) entrees else entrees.filter { entree ->
                        val pers = entree as Personne
                        (pers.nom + " " + pers.prenoms + " " + pers.lieuNaissance + " " + pers.lieuMariage + " " + pers.lieuDeces)
                            .contains(recherche.trim(), ignoreCase = true)
                    }
                    // G3-14 : en tri A-Z, sans filtre ni recherche, un
                    // en-tete collant par lettre guide le defilement.
                    val entreesListe: List<Any> = if (triPersonnes == 0 && lettreChoisie == null && recherche.isBlank()) {
                        val resultat = mutableListOf<Any>()
                        var derniereLettre: Char? = null
                        entreesFiltrees.forEach { entree ->
                            val pers = entree as Personne
                            val lettre = pers.nom.trim().uppercase().firstOrNull() ?: '?'
                            if (lettre != derniereLettre) {
                                resultat.add(lettre)
                                derniereLettre = lettre
                            }
                            resultat.add(pers)
                        }
                        resultat
                    } else {
                        entreesFiltrees
                    }

                    Column(Modifier.fillMaxSize().padding(marges)) {
                        // G3-14 : recherche par nom ou lieu.
                        OutlinedTextField(
                            value = recherche,
                            onValueChange = { recherche = it },
                            placeholder = { Text("Rechercher un nom ou un lieu") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (recherche.isNotEmpty()) {
                                    Icon(Icons.Filled.Close, contentDescription = "Effacer la recherche", Modifier.clickable { recherche = "" })
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        // Compteur et tris FIGES en haut de l'ecran.
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                    personnes.size.toString() + " personnes",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilterChip(
                                    selected = triPersonnes == 0,
                                    onClick = { triPersonnes = 0 },
                                    label = { Text("A\u2013Z") }
                                )
                                Spacer(Modifier.width(6.dp))
                                FilterChip(
                                    selected = triPersonnes == 1,
                                    onClick = { triPersonnes = 1 },
                                    label = { Text("Aboville") }
                                )
                                Spacer(Modifier.width(6.dp))
                                FilterChip(
                                    selected = triPersonnes == 2,
                                    onClick = { triPersonnes = 2 },
                                    label = { Text("Branches") }
                                )
                        }
                        // Index alphabetique ou 4 branches : FIGE aussi.
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                                if (triPersonnes == 0) {
                                    // Alphabet complet, resserre pour tenir d'un coup d'oeil :
                                    // lettres inutilisees grisees.
                                    ('A'..'Z').forEach { lettre ->
                                        val utilisee = lettresUtilisees.contains(lettre)
                                        val active = lettreChoisie == lettre
                                        Text(
                                            lettre.toString(),
                                            Modifier
                                                .heightIn(min = 48.dp)
                                                .clickable(enabled = utilisee) {
                                                    lettreChoisie = if (active) null else lettre
                                                }
                                                .background(if (active) CouleurVert else Color.Transparent)
                                                .padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.White else if (utilisee) CouleurVertFonce else CouleurInactif
                                        )
                                    }
                                } else if (triPersonnes == 2) {
                                    if (grandsParents.isEmpty()) {
                                        Text(
                                            "D\u00E9finissez le de cujus et ses parents pour voir les 4 branches.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    grandsParents.forEach { gp ->
                                        val active = brancheChoisie == gp.id
                                        TextButton(
                                            onClick = { brancheChoisie = if (active) null else gp.id },
                                            contentPadding = PaddingValues(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                gp.nom.trim() + (if (gp.numeroBrut().isNotEmpty()) " (" + gp.numeroBrut() + ")" else ""),
                                                Modifier
                                                    .background(
                                                        if (active) CouleurVertPale else Color.Transparent,
                                                        RoundedCornerShape(50)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) CouleurVertFonce else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        if (deuxColonnes) {
                            // G3-17 : tablette ou paysage, grille a deux
                            // colonnes avec les memes cles stables.
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp)
                            ) {
                                entreesFiltrees.forEach { entree ->
                                    val p = entree as Personne
                                    item(key = p.id, contentType = "personne") {
                                        CartePersonne(
                                            p,
                                            onOuvrir = { onIdOuvert(p.id) },
                                            onSupprimer = { onConfirmSuppression(p) }
                                        )
                                    }
                                }
                            }
                        } else {
                        LazyColumn(
                            Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
                            state = etatListe
                        ) {
                            // G3-14 : cles stables (id) et en-tetes de lettre
                            // collants en tri A-Z.
                            entreesListe.forEach { entree ->
                                when (entree) {
                                    is Char -> stickyHeader(key = "lettre_" + entree, contentType = "lettre") {
                                        Text(
                                            entree.toString(),
                                            Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 12.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    else -> item(key = (entree as Personne).id, contentType = "personne") {
                                        val p = entree as Personne
                                        CartePersonne(
                                            p,
                                            onOuvrir = { onIdOuvert(p.id) },
                                            onSupprimer = { onConfirmSuppression(p) }
                                        )
                                    }
                                }
                            }
                            if (entreesListe.isEmpty()) {
                                item {
                                    Text(
                                        "Aucun r\\u00E9sultat pour \\u00AB " + recherche.trim() + " \\u00BB.",
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }
                        }
                    }
                }
            } else if (onglet == 2) {
                // G3-03 : marges du Scaffold (barre du haut, barre du bas)
                // appliquees aussi a ces onglets.
                Box(Modifier.fillMaxSize().padding(marges)) {
                EcranArbre(
                    personnes = personnes,
                    idCentre = arbreCentre,
                    onOuvrir = { onIdOuvert(it); onArbreCentre(it) },
                    onCentrerSur = { onArbreCentre(it) }
                )
                }
            } else if (onglet == 3) {
                Box(Modifier.fillMaxSize().padding(marges).imePadding()) {
                EcranSaisieActe(
                    personnes = personnes,
                    onEnregistrer = enregistrerActe
                )
                }
            } else {
                Box(Modifier.fillMaxSize().padding(marges)) {
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
                            Text(
                                "\u26A0\uFE0F Le fichier GEDCOM est EN CLAIR, sans chiffrement : ne le partagez qu'avec pr\u00E9caution.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = exclureVivants, onCheckedChange = { exclureVivants = it })
                                Text("Exclure les personnes vivantes (sans d\u00E9c\u00E8s, n\u00E9es il y a moins de 100 ans)", style = MaterialTheme.typography.bodySmall)
                            }
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
                // G3-13 : ModalBottomSheet a la place du dialogue.
                ModalBottomSheet(
                    onDismissRequest = { menuGedcom = false }
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Text("GEDCOM (Heredis)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(personnes.size.toString() + " personnes", fontWeight = FontWeight.Bold)
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
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, Modifier.size(18.dp))
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
                                onClick = { menuGedcom = false; changerPin() },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Changer le code PIN")
                                }
                            }
                            TextButton(
                                onClick = { menuGedcom = false; reglagesVerrouillage() },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("D\u00E9lai de verrouillage")
                                }
                            }
                            // G3-02 : choix du theme clair / sombre / systeme.
                            TextButton(
                                onClick = { menuGedcom = false; reglagesTheme() },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Face, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Th\u00E8me : clair, sombre ou syst\u00E8me")
                                }
                            }
                            TextButton(
                                onClick = { menuGedcom = false; phraseSecrete = ""; demandePhraseExport = true },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sauvegarde chiffr\u00E9e (.gpbak)")
                                }
                            }
                            TextButton(
                                onClick = { menuGedcom = false; phraseSecrete = ""; demandePhraseImport = true },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restaurer une sauvegarde (.gpbak)")
                                }
                            }
                            TextButton(
                                onClick = { menuGedcom = false; restaurerImport() },
                                Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restaurer la base d'avant import")
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
                        TextButton(onClick = { menuGedcom = false }, Modifier.fillMaxWidth()) { Text("Fermer") }
                    }
                }
            }
            // G1-08 : dialogue de phrase secrete (export/import .gpbak).
            if (demandePhraseExport || demandePhraseImport) {
                AlertDialog(
                    onDismissRequest = { demandePhraseExport = false; demandePhraseImport = false; phraseSecrete = "" },
                    title = { Text(if (demandePhraseExport) "Sauvegarde chiffr\u00E9e (.gpbak)" else "Restaurer une sauvegarde (.gpbak)") },
                    text = {
                        Column {
                            Text(
                                if (demandePhraseExport)
                                    "La sauvegarde contiendra toute la base (personnes, journal, actes, notes et images), chiffr\u00E9e par votre phrase secr\u00E8te. Sans elle, le fichier est indechiffrable."
                                else
                                    "Choisissez le fichier .gpbak : la base actuelle sera remplace\u00E9e par son contenu.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = phraseSecrete,
                                onValueChange = { phraseSecrete = it },
                                label = { Text("Phrase secr\u00E8te (6 caract\u00E8res min.)") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                phraseEnAttente = phraseSecrete
                                phraseSecrete = ""
                                if (demandePhraseExport) {
                                    demandePhraseExport = false
                                    lanceurGpbakExport.launch("geneapoche.gpbak")
                                } else {
                                    demandePhraseImport = false
                                    lanceurGpbakImport.launch(arrayOf("*/*"))
                                }
                            },
                            enabled = phraseSecrete.length >= 6
                        ) { Text("Continuer") }
                    },
                    dismissButton = {
                        TextButton(onClick = { demandePhraseExport = false; demandePhraseImport = false; phraseSecrete = "" }) { Text("Annuler") }
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
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                EcranPersonne(
            personnes = personnes,
            actes = actes,
            p = p,
            enregistrer = enregistrer,
            onOuvrir = { onIdOuvert(it) },
            onRetour = onRetourFiche,
            pistes = pistes,
            onSupprimerPiste = { supprimerPiste(it) },
            onModifier = { onEditionCible(p); onEditionOuverte(true) },
            onSupprimer = { onConfirmSuppression(p) },
            onChoisir = { onPickerRole(it) },
            onRetirer = { role, enfantId -> delier(role, enfantId) },
            onDefinirCujus = { definirCujus(p) },
            onNouveauMariage = { nom, prenoms, date, commune, dept ->
                ajouterMariage(nom, prenoms, date, commune, dept)
            },
            onNouveauParent = { role, nom, prenoms -> nouveauParent(role, nom, prenoms) },
            onNouveauTemoin = { type, id, nom, prenoms -> nouveauTemoin(type, id, nom, prenoms) },
            onRetirerTemoin = { type, id -> retirerTemoin(type, id) },
            onBasculerSignature = { type, id -> basculerSignature(type, id) },
            onModifierBloc = { type, d, h, co, de, rf -> majEvenement(type, d, h, co, de, rf) },
            onNouvelEnfant = { nom, prenoms, sexe, date ->
                ajouterEnfant(nom, prenoms, sexe, date)
            },
            onAjouterActe = { uri, type, titre -> ajouterActe(p.id, uri, type, titre) },
            onSupprimerActe = { onSupprimerActe(it) },
            onVoirActe = { onVoirActe(it) },
            ouvrirEtatCivil = ouvrirEtatCivil,
            onValider = { section, valide -> validerInfo(p, section, valide) }
        )
            }
            // Menus de l'application TOUJOURS visibles en bas, meme fiche ouverte.
            // Choisir un onglet ferme la fiche et affiche l'onglet demande.
            SnackbarHost(etatSnackbar)
            BarreOnglets(onglet) { o -> onOnglet(o); onIdOuvert(0L) }
        }
    }

    // G3-19 : le message passe par le Snackbar (declare plus haut).

    if (editionOuverte) {
        // G2-18 residuel : identifiant alloue UNE seule fois par ouverture du
        // dialogue, strictement croissant (compteur persistant).
        val idNouvellePersonne = remember { prochainId() }
        DialogueEdition(
            cible = editionCible,
            idNouvelle = idNouvellePersonne,
            onValider = { p2 ->
                if (editionCible == null) {
                    // Contr\u00F4le anti-doublon : une fiche portant d\u00E9j\u00E0 le m\u00EAme
                    // couple nom + pr\u00E9noms n'est JAMAIS recr\u00E9\u00E9e (insensible \u00E0 la casse).
                    val doublon = personnes.firstOrNull { q ->
                        q.nom.trim().uppercase() == p2.nom.trim().uppercase() &&
                        q.prenoms.trim().uppercase() == p2.prenoms.trim().uppercase()
                    }
                    if (doublon != null) {
                        onMessage(
                            "Cette personne existe d\u00E9j\u00E0 : " + doublon.titre() +
                            ". Ouvrez sa fiche pour la compl\u00E9ter (aucun doublon dans la base)."
                        )
                        return@DialogueEdition
                    }
                    (personnes as? MutableList<Personne>)?.add(p2)
                    if (pickerRole.isNotEmpty()) {
                        lier(pickerRole, p2.id)
                        return@DialogueEdition
                    }
                }
                enregistrer()
                onEditionOuverte(false)
            },
            onAnnuler = { onEditionOuverte(false) },
            onSupprimer = {
                editionCible?.let { onConfirmSuppression(it) }
                onEditionOuverte(false)
            }
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
                                    .heightIn(min = 48.dp)
                                    .clickable { lier(role, c.id) }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(c.titre(), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    }
}

// ---------------------------------------------------------------- Liste : carte personne

// Branche d'un grand-parent : lui, ses conjoints et TOUS ses descendants,
// quel que soit leur nom (les liens font la branche, pas le nom de famille).
fun brancheDe(rootId: Long, personnes: List<Personne>): Set<Long> {
    val racine = personnes.firstOrNull { it.id == rootId }
    val res = mutableSetOf(rootId)
    if (racine != null) {
        racine.tousConjoints().forEach { c -> if (c != 0L) res.add(c) }
    }
    var front = listOf(rootId)
    while (front.isNotEmpty()) {
        val nouveaux = personnes
            .filter { pers -> front.any { f -> pers.pere == f || pers.mere == f } }
            .map { it.id }
            .filter { it !in res }
        res.addAll(nouveaux)
        front = nouveaux
    }
    return res
}

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
        "F" -> CouleurRosePale
        "M" -> CouleurBleuPale
        else -> CouleurNeutrePale
    }
    Card(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable { onOuvrir() },
        colors = CardDefaults.cardColors(containerColor = couleurCarte)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vignette : rond au NUMERO (Sosa ou d'Aboville) ; l'initiale
            // du prenom sert de repli quand la personne n'a pas de numero.
            val initiale = p.prenoms.trim().firstOrNull() ?: p.nom.trim().firstOrNull() ?: '?'
            val vignette = p.numeroAffiche().ifEmpty { initiale.toString() }
            val couleurAvatar = when (p.sexe) {
                "F" -> CouleurRose
                "M" -> CouleurBleu
                else -> CouleurNeutreDoux
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(couleurAvatar),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    vignette,
                    color = CouleurVertFonce,
                    fontWeight = FontWeight.Bold,
                    fontSize = when {
                        vignette.length <= 3 -> 15.sp
                        vignette.length <= 5 -> 12.sp
                        else -> 12.sp
                    },
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.titre(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val n = dateAffichee(p.dateNaissance.trim())
                val m = dateAffichee(p.dateMariage.trim())
                val d = dateAffichee(p.dateDeces.trim())
                if (n.isNotEmpty() || m.isNotEmpty() || d.isNotEmpty()) {
                    val morceaux = mutableListOf<String>()
                    if (n.isNotEmpty()) morceaux.add("N. " + n)
                    if (m.isNotEmpty()) morceaux.add("M. " + m)
                    if (d.isNotEmpty() && decesAffichable(p)) morceaux.add("D. " + d)
                    Text(morceaux.joinToString("  \u2013  "), style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onSupprimer, Modifier.size(30.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer", Modifier.size(16.dp))
            }
        }
    }
}







