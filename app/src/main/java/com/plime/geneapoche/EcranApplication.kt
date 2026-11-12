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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
        // v0.95 (12) : la Boite a outils devient un onglet a part entiere.
        NavigationBarItem(
            selected = onglet == 3,
            onClick = { onChoisir(3) },
            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            label = { Text("Outils") }
        )
        NavigationBarItem(
            selected = onglet == 4,
            onClick = { onChoisir(4) },
            icon = { Icon(painterResource(R.drawable.ic_sources), contentDescription = null) },
            label = { Text("Sources") }
        )
        // v0.95 (12) : Geographie devient Statistiques, dernier onglet.
        NavigationBarItem(
            selected = onglet == 5,
            onClick = { onChoisir(5) },
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Stat.") }
        )
    }
}
// G2-09 : l'etat de l'ecran (donnees affichees) regroupe en une seule classe,
// pour ramener la signature d'EcranApplication a 2 parametres.
class EtatApplication(
    val personnes: List<Personne>,
    val cujus: Personne?,
    val pistes: List<Piste>,
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
    val annulationPossible: Boolean = false,
    // v0.78 : paires de doublons "gard\u00E9es les deux" (diagnostic).
    val doublonsGardes: Set<String> = emptySet(),
    // v0.88 : anomalies a verifier ignorees (cles).
    val anomaliesIgnorees: Set<String> = emptySet(),
    // 12.1 : sauvegarde automatique (.gpbak) dans un dossier choisi.
    val dossierSauvegarde: String = "",
    val seuilSauvegarde: Int = 25,
    val cleRecuperation: String = "",
    // v0.92 : carnet de sources (sources reutilisables + citations).
    val sources: List<Source> = emptyList(),
    val citations: List<Citation> = emptyList()
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
    // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements libres de la chronologie.
    val majEvenementLibre: (Personne, EvenementLibre) -> Unit,
    val supprimerEvenementLibre: (Personne, Long) -> Unit,
    val ajouterEnfant: (String, String, String, String) -> Unit,
    val definirCujus: (Personne) -> Unit,
    val choisirCujus: () -> Unit,
    val renumeroter: (Long) -> Unit,
    val supprimerPiste: (Piste) -> Unit,
    // Chantier 12.0 : coche "Enregistré" par type d'acte sur la fiche.
    val onBasculerEnregistre: (String) -> Unit = {},
    val onArbreCentre: (Long) -> Unit,
    val exporterGedcom: (Uri, Set<Long>?, Boolean, String, String) -> Unit,
    val importerGedcom: (Uri) -> Unit,
    val viderBase: () -> Unit,
    val enregistrerActe: (ActeSaisie) -> Unit,
    val sauvegarderBase: (Uri, String) -> Unit,
    val restaurerSauvegarde: (Uri, String) -> Unit,
    val restaurerImport: () -> Unit,
    // v0.42 : restauration des copies de rotation (.bak), secours base vide.
    val restaurerSecours: () -> Unit,
    // v0.43 : true quand un selecteur de fichiers est ouvert : suspend le
    // reverrouillage automatique (sinon la purge memoire vide la base).
    val picker: (Boolean) -> Unit,
    val changerPin: () -> Unit,
    val reglagesVerrouillage: () -> Unit,
    val reglagesTheme: () -> Unit,
    // G3-19 : annulation d'une suppression via le Snackbar.
    val onAnnulerSuppression: () -> Unit,
    val onPurgerAnnulation: () -> Unit,
    // G2-18 residuel : identifiants strictement croissants aussi pour les
    // nouvelles personnes creees par le dialogue d'edition.
    val prochainId: () -> Long,
    val validerInfo: (Personne, String, Boolean) -> Unit,
    // G\u00E9n\u00E9aPoche 11 : coche de certification de l'identit\u00E9
    // ("auto"/"coche"/"croix") et fusion guid\u00E9e d'un doublon.
    val validerIdentite: (Personne, String) -> Unit,
    val fusionner: (Long, Long) -> Unit,
    // v0.78 : garder les deux fiches d'une paire de doublons (d\u00E9finitif).
    val garderDoublon: (Long, Long) -> Unit,
    // v0.88 : Ignorer une anomalie a verifier, ou les reafficher toutes.
    val ignorerAnomalie: (String) -> Unit,
    val reafficherAnomalies: () -> Unit,
    // v0.90 : corriger en un clic un lieu d'ancienne saisie (journal).
    val corrigerLieu: (Long, String, String) -> Unit = { _, _, _ -> },
    // 12.1 : sauvegarde automatique apr\u00E8s N modifications dans un dossier
    // choisi + cl\u00E9 de r\u00E9cup\u00E9ration \u00E0 imprimer.
    val sauvegardeAuto: (Uri) -> Unit,
    val sauvegardeAutoOff: () -> Unit,
    val reglerSeuilSauvegarde: (Int) -> Unit,
    val afficherCleSauvegarde: () -> Unit,
    // v0.92 : carnet de sources. creerSource renvoie l'id attribue.
    val creerSource: (Source) -> Long = { 0L },
    val enregistrerSource: (Source) -> Unit = {},
    val supprimerSource: (Long) -> Unit = {},
    val enregistrerCitation: (Citation) -> Unit = {},
    val supprimerCitation: (Long) -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EcranApplication(
    etat: EtatApplication,
    actions: ActionsApplication
) {
    with(etat) {
    with(actions) {
    // v0.94 (12) : le menu Saisie quitte la barre d'onglets et devient
    // un plein ecran ouvert depuis le bouton +.
    var saisieType by remember { mutableStateOf<String?>(null) }
    var saisieParentEnfant by remember { mutableStateOf(0L) }
    var saisieOuverte by remember { mutableStateOf(false) }
    // v0.96 : une image est chargee dans l'ecran scinde -> le bandeau du
    // haut (barre de titre de l'application) est masque pour agrandir la
    // zone de visualisation.
    var imageSaisiePresente by remember { mutableStateOf(false) }
    // v0.97 : toucher une carte de l'arbre NE CHANGE PLUS d'onglet : la
    // personne touchee est memorisee et sa fiche s'ouvre seulement quand
    // on bascule vers l'onglet Personnes (l'appui long ouvre toujours).
    var idToucheArbre by remember { mutableStateOf(0L) }
    // v0.96 : changer d'onglet ferme l'ecran scinde (l'image ne doit
    // jamais rester affichee sur un autre ecran).
    val choisirOnglet: (Int) -> Unit = { o ->
        if (saisieOuverte) {
            saisieOuverte = false
            saisieType = null
            imageSaisiePresente = false
        }
        if (o == 0 && idToucheArbre != 0L) {
            onIdOuvert(idToucheArbre)
            idToucheArbre = 0L
        }
        onOnglet(o)
    }
    // v0.95 (12) : menu interne de l'onglet Arbre (0 = arbre, 1 = ligne de vie).
    var vueArbre by remember { mutableStateOf(0) }
    BackHandler(enabled = idOuvert != 0L || editionOuverte || pickerRole.isNotEmpty() || saisieOuverte) {
        when {
            pickerRole.isNotEmpty() -> onPickerRole("")
            editionOuverte -> onEditionOuverte(false)
            saisieOuverte -> { saisieOuverte = false; saisieType = null }
            else -> onIdOuvert(0L)
        }
    }

    val ouverte = personneParId(personnes, idOuvert)
    var menuGedcom by remember { mutableStateOf(false) }
    var exclureVivants by remember { mutableStateOf(false) }
    // G\u00E9n\u00E9aPoche 3 : double export GEDCOM (5.5.1 pour Heredis, ou
    // 7.0.18, derni\u00E8re version du standard au 01/11/2026).
    var formatGedcom by remember { mutableStateOf("5.5.1") }
    // 12.2 : export GEDCOM chiffr\u00E9 (.gpged) avec phrase secr\u00E8te, et
    // rappel de suppression du fichier envoy\u00E9.
    var exportChiffre by remember { mutableStateOf(false) }
    var demandePhraseGedcom by remember { mutableStateOf(false) }
    var phraseSecreteGed by remember { mutableStateOf("") }
    var gedPhraseEnAttente by remember { mutableStateOf("") }
    var gedChiffreBranche by remember { mutableStateOf(false) }
    var confirmeVider by remember { mutableStateOf(false) }
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
            snackbarHost = { SnackbarHost(etatSnackbar) },
            topBar = {
                // v0.96 : le bandeau du haut est masque tant qu'une image
                // est affichee dans l'ecran scinde de saisie (zone de
                // visualisation agrandie).
                if (!(saisieOuverte && imageSaisiePresente)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("G\u00E9n\u00E9aPoche", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Ma g\u00E9n\u00E9alogie dans ma poche \u2013 v0.98",
                                    fontSize = 11.sp,
                                    color = CouleurSurvol
                                )
                            }
                        }
                    },
                    actions = {
                        // G\u00E9n\u00E9aPoche 5 : l'intitul\u00E9 "GEDCOM" est
                        // remplac\u00E9 par une ic\u00F4ne de param\u00E9trages.
                        IconButton(onClick = { menuGedcom = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Param\u00E9trages")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PaletteGeneaPoche.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                )
                }
            },
            bottomBar = { BarreOnglets(onglet, choisirOnglet) },
            floatingActionButton = {
                // G\u00E9n\u00E9aPoche 2 : nouvelle fiche depuis N'IMPORTE QUELLE page.
                FloatingActionButton(onClick = { saisieType = null; saisieParentEnfant = 0L; saisieOuverte = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter")
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
            // 12.1 : dialogue de sauvegarde automatique + dossier choisi.
            var dialogueSauvegardeAuto by remember { mutableStateOf(false) }
            var seuilSaisi by remember(dialogueSauvegardeAuto) { mutableStateOf(seuilSauvegarde.toString()) }
            val lanceurDossierSauvegarde = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                picker(false)
                if (uri != null) sauvegardeAuto(uri)
            }
            val lanceurGpbakExport = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                picker(false)
                if (uri != null) sauvegarderBase(uri, phraseEnAttente)
            }
            val lanceurGpbakImport = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                picker(false)
                if (uri != null) restaurerSauvegarde(uri, phraseEnAttente)
            }

            val lanceurGedcom = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                picker(false)
                if (uri != null) {
                    // 12.2 : phrase en attente = export chiffr\u00E9 (.gpged).
                    val ph = gedPhraseEnAttente
                    gedPhraseEnAttente = ""
                    exporterGedcom(uri, null, exclureVivants, formatGedcom, ph)
                }
            }
            val lanceurGedcomBranche = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                picker(false)
                if (uri != null) {
                    val sel = exportBrancheId
                    // 12.2 : phrase en attente = export chiffr\u00E9 (.gpged).
                    val ph = gedPhraseEnAttente
                    gedPhraseEnAttente = ""
                    exporterGedcom(uri, if (sel != 0L) brancheDe(personnes, sel) else null, exclureVivants, formatGedcom, ph)
                }
            }
            val lanceurImport = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                picker(false)
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
                    // triPersonnes : 0 = A-Z, 1 = Aboville (n croissant), 2 = Branches, 3 = Sosa.
                    // v0.95 (12) : SOSA par d\u00e9faut, puis Aboville, Branches, A-Z.
                    var triPersonnes by remember { mutableStateOf(3) }
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
                    } else if (triPersonnes == 3) {
                        // v0.94 : tri Sosa - seuls les ancetres de la ligne directe,
                        // meme affichage que d'Aboville.
                        // sosa est une chaine : tri numerique (1, 2, 3... 10, pas 1, 10, 2).
                        personnes.filter { it.sosa.trim().isNotEmpty() && it.sosa.trim() != "0" }
                            .sortedBy { it.sosa.trim().toLongOrNull() ?: Long.MAX_VALUE }
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
                                    selected = triPersonnes == 3,
                                    onClick = { triPersonnes = 3 },
                                    label = { Text("Sosa") }
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
                                Spacer(Modifier.width(6.dp))
                                FilterChip(
                                    selected = triPersonnes == 0,
                                    onClick = { triPersonnes = 0 },
                                    label = { Text("A\u2013Z") }
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
                                        "Aucun r\u00E9sultat pour \u00AB " + recherche.trim() + " \u00BB.",
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
                Column(Modifier.fillMaxSize().padding(marges)) {
                    // v0.95 (12) : menu en haut, comme le menu Personnes.
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        FilterChip(
                            selected = vueArbre == 0,
                            onClick = { vueArbre = 0 },
                            label = { Text("Arbre") }
                        )
                        Spacer(Modifier.width(6.dp))
                        FilterChip(
                            selected = vueArbre == 1,
                            onClick = { vueArbre = 1 },
                            label = { Text("Ligne de vie") }
                        )
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (vueArbre == 0) {
                            EcranArbre(
                                personnes = personnes,
                                idCentre = arbreCentre,
                                // v0.97 : toucher une carte centre seulement
                                // (mémorise la personne) ; l'appui long
                                // ouvre la fiche.
                                onOuvrir = { onIdOuvert(it); onOnglet(0) },
                                onCentrerSur = { onArbreCentre(it); idToucheArbre = it }
                            )
                        } else {
                            EcranFrise(
                                personnes = personnes,
                                idChoisie = arbreCentre,
                                onOuvrir = { onIdOuvert(it) },
                                onChoisir = { onArbreCentre(it) }
                            )
                        }
                    }
                }
            } else if (onglet == 3) {
                // v0.95 (12) : la Boite a outils devient un onglet.
                Box(Modifier.fillMaxSize().padding(marges)) {
                EcranOutils(
                    personnes = personnes,
                    onAjouterNote = { id, texte ->
                        val cible = personneParId(personnes, id)
                        if (cible != null) {
                            cible.notesLibres = (if (cible.notesLibres.isBlank()) texte else cible.notesLibres.trim() + "\n" + texte).trim()
                            enregistrer()
                        }
                    },
                    enOnglet = true
                )
                }
            } else if (onglet == 4) {
                // v0.95 (12) : le Carnet de sources s'affiche comme les autres
                // onglets (barre de navigation toujours visible).
                Box(Modifier.fillMaxSize().padding(marges)) {
                EcranSources(
                    personnes = personnes,
                    sources = sources,
                    citations = citations,
                    onCreerSource = creerSource,
                    onEnregistrerSource = enregistrerSource,
                    onSupprimerSource = supprimerSource,
                    onEnregistrerCitation = enregistrerCitation,
                    onSupprimerCitation = supprimerCitation,
                    onOuvrirPersonne = { id -> onIdOuvert(id) }
                )
                }
            } else if (onglet == 5) {
                // v0.95 (12) : Geographie devient Statistiques, dernier onglet.
                Box(Modifier.fillMaxSize().padding(marges)) {
                EcranGeographie(
                    personnes = personnes,
                    idChoisie = arbreCentre,
                    onOuvrir = { onIdOuvert(it); onArbreCentre(it) },
                    onChoisir = { onArbreCentre(it) }
                )
                }
            } else {
                Box(Modifier.fillMaxSize().padding(marges)) {
                EcranJournal(
                    pistes = pistes,
                    personnes = personnes,
                    onSupprimer = { supprimerPiste(it) },
                    onOuvrirPersonne = { onIdOuvert(it) },
                    onSauver = enregistrer,
                    onFusionner = { gardeId, supprimeId -> fusionner(gardeId, supprimeId) },
                    gardes = doublonsGardes,
                    onGarderDeux = { a, b -> garderDoublon(a, b) },
                    anomaliesIgnorees = anomaliesIgnorees,
                    onIgnorerAnomalie = { cle -> ignorerAnomalie(cle) },
                    onReafficherAnomalies = { reafficherAnomalies() },
                    onCorrigerLieu = { id, champ, valeur -> corrigerLieu(id, champ, valeur) }
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
                                "Format GEDCOM :",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Row {
                                FilterChip(
                                    selected = formatGedcom == "5.5.1",
                                    onClick = { formatGedcom = "5.5.1" },
                                    label = { Text("5.5.1 (Heredis)") }
                                )
                                Spacer(Modifier.width(6.dp))
                                FilterChip(
                                    selected = formatGedcom == "7.0.18",
                                    onClick = { formatGedcom = "7.0.18" },
                                    label = { Text("7.0.18") }
                                )
                            }
                            Text(
                                "Le fichier .ged cr\u00E9\u00E9 est importable dans Heredis (GEDCOM 5.5.1) ou compatible GEDCOM 7.0.18.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = exclureVivants, onCheckedChange = { exclureVivants = it })
                                Text("Exclure les personnes vivantes (sans d\u00E9c\u00E8s, n\u00E9es il y a moins de 100 ans)", style = MaterialTheme.typography.bodySmall)
                            }
                            // 12.2 : coche d'export chiffr\u00E9 (.gpged).
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = exportChiffre, onCheckedChange = { exportChiffre = it })
                                Text("Chiffrer le fichier (.gpged, phrase secr\u00E8te)", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                if (exportChiffre)
                                    "Le fichier .gpged sera chiffr\u00E9 : il ne s'ouvre que dans GeneaPoche (Importer un GEDCOM) avec la phrase secr\u00E8te, jamais dans Heredis."
                                else
                                    "\u26A0\uFE0F Le fichier GEDCOM est EN CLAIR, sans chiffrement : ne le partagez qu'avec pr\u00E9caution.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            exportDialogue = false
                            // v0.43 : jamais de fichier cree par un export
                            // lance depuis une base vide.
                            if (personnes.isEmpty()) {
                                onMessage("La base est vide : rien \u00E0 exporter.")
                            } else if (!exportTout && exportBrancheId == 0L) {
                                onMessage("Choisissez une personne centrale.")
                            } else if (exportChiffre) {
                                // 12.2 : phrase secr\u00E8te AVANT le choix du fichier.
                                gedChiffreBranche = !exportTout
                                demandePhraseGedcom = true
                            } else if (exportTout) {
                                picker(true)
                                lanceurGedcom.launch("geneapoche.ged")
                            } else {
                                picker(true)
                                lanceurGedcomBranche.launch("geneapoche-branche.ged")
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
                                Text(
                                    libelleUnique(c),
                                    Modifier.fillMaxWidth().clickable {
                                        exportBrancheId = c.id
                                        pickerBranche = false
                                    }.padding(vertical = 0.dp),
                                    fontSize = 13.sp, lineHeight = 16.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { pickerBranche = false }) { Text("Fermer") }
                    }
                )
            }
            // v0.95 (12) : le bouton + ouvre un ecran SCINDE : en haut
            // l'image a transcrire (zoom + deplacement), en bas la saisie
            // (personne complete, actes, source).
            if (saisieOuverte) {
                var imageTranscription by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                var uriImage by remember { mutableStateOf<Uri?>(null) }
                var zoomImage by remember { mutableStateOf(1f) }
                var decaleImage by remember { mutableStateOf(Offset.Zero) }
                // v0.96 : rotation par pas de 90 degres + reglages
                // luminosite / contraste pour les registres peu lisibles.
                var angleImage by remember { mutableStateOf(0f) }
                var lumiereImage by remember { mutableStateOf(1f) }
                var contrasteImage by remember { mutableStateOf(1f) }
                var reglagesImage by remember { mutableStateOf(false) }
                val contexte = LocalContext.current
                val lanceurImage = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    picker(false)
                    uriImage = uri
                    // v0.96 : remise a zero des reglages a chaque nouvelle image.
                    zoomImage = 1f
                    decaleImage = Offset.Zero
                    angleImage = 0f
                    lumiereImage = 1f
                    contrasteImage = 1f
                    reglagesImage = false
                    imageSaisiePresente = uri != null
                }
                LaunchedEffect(uriImage) {
                    val u = uriImage ?: return@LaunchedEffect
                    imageTranscription = try {
                        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                        contexte.contentResolver.openInputStream(u)?.use { flux ->
                            android.graphics.BitmapFactory.decodeStream(flux, null, opts)
                        }
                    } catch (e2: Exception) { null }
                }
                Column(Modifier.fillMaxSize().padding(marges).imePadding()) {
                    // v0.96 : le bandeau (Fermer / Image a transcrire) est
                    // masque des qu'une image est choisie ; des boutons
                    // compacts restent superposes a l'image (fermer,
                    // changer, pivoter, reglages).
                    if (imageTranscription == null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { saisieOuverte = false; saisieType = null; imageSaisiePresente = false }) { Text("\u2190 Fermer") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                picker(true)
                                lanceurImage.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) { Text("Image \u00e0 transcrire") }
                        }
                    }
                    // Zone HAUTE : l'image de reference. v0.96 : le zoom ne
                    // deborde JAMAIS de la demi-zone (clipToBounds) et la
                    // rotation + les reglages lumiere/contraste sont
                    // superposes a l'image.
                    Box(
                        Modifier.weight(1f).fillMaxWidth()
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomImage = (zoomImage * zoom).coerceIn(1f, 8f)
                                    decaleImage += pan
                                }
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val bmp = imageTranscription
                        if (bmp == null) {
                            Text(
                                "Choisissez une image (acte, capture d'\u00e9cran) : elle reste affich\u00e9e ici, zoomable, pendant que vous saisissez en bas.",
                                Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // v0.96 : luminosite et contraste via une matrice
                            // de couleurs (sortie = contraste * (lumiere * v)
                            // + 255 * (0.5 - 0.5 * contraste)).
                            val s = lumiereImage * contrasteImage
                            val decalage = 255f * (0.5f - 0.5f * contrasteImage)
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Image \u00e0 transcrire",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                    androidx.compose.ui.graphics.ColorMatrix(
                                        floatArrayOf(
                                            s, 0f, 0f, 0f, decalage,
                                            0f, s, 0f, 0f, decalage,
                                            0f, 0f, s, 0f, decalage,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                ),
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoomImage; scaleY = zoomImage
                                    translationX = decaleImage.x; translationY = decaleImage.y
                                    rotationZ = angleImage
                                }
                            )
                            // Boutons compacts superposes (haut droite).
                            Row(
                                Modifier.align(Alignment.TopEnd).padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                                    TextButton(onClick = { angleImage += 90f }) {
                                        Text("Pivoter", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                                    TextButton(onClick = { reglagesImage = !reglagesImage }) {
                                        Text(if (reglagesImage) "Fin r\u00e9glages" else "R\u00e9glages", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                                    TextButton(onClick = {
                                        picker(true)
                                        lanceurImage.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }) {
                                        Text("Changer", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                                    TextButton(onClick = { saisieOuverte = false; saisieType = null; imageSaisiePresente = false }) {
                                        Text("\u2190 Fermer", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                            // Curseurs de reglage (bas de la zone image).
                            if (reglagesImage) {
                                Column(
                                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Lumi\u00e8re", fontSize = 10.sp)
                                    Slider(value = lumiereImage, onValueChange = { lumiereImage = it }, valueRange = 0.5f..2f)
                                    Text("Contraste", fontSize = 10.sp)
                                    Slider(value = contrasteImage, onValueChange = { contrasteImage = it }, valueRange = 0.5f..2f)
                                }
                            }
                        }
                    }
                    // Zone BASSE : la saisie.
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (saisieType == null) {
                            Column(Modifier.fillMaxSize()) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
                                    TextButton(onClick = { onEditionCible(null); onEditionOuverte(true) }) {
                                        Icon(Icons.Filled.Person, contentDescription = null, Modifier.size(16.dp))
                                        Text("  Personne compl\u00e8te")
                                    }
                                    TextButton(onClick = { onOnglet(4) }) {
                                        Icon(painterResource(R.drawable.ic_sources), contentDescription = null, Modifier.size(16.dp))
                                        Text("  Source")
                                    }
                                }
                                Box(Modifier.weight(1f)) {
                                    EcranSaisie(
                                        personnes = personnes,
                                        onOuvrir = { t -> saisieType = t; saisieParentEnfant = 0L },
                                        onEnfant = { idParent -> saisieType = "N"; saisieParentEnfant = idParent }
                                    )
                                }
                            }
                        } else {
                            EcranSaisieActe(
                                personnes = personnes,
                                onEnregistrer = enregistrerActe,
                                sources = sources,
                                citations = citations,
                                onCreerSource = creerSource,
                                initial = if (saisieParentEnfant == 0L && idOuvert != 0L && saisieType != "E") {
                                    personneParId(personnes, idOuvert)?.let { cand -> prefillActe(cand, personnes, saisieType, citations = citations) }
                                        ?: ActeSaisie(type = saisieType ?: "N")
                                } else {
                                    ActeSaisie(type = saisieType ?: "N", parentPourEnfant = saisieParentEnfant)
                                },
                                onFermer = { saisieType = null }
                            )
                        }
                    }
                }
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
                        Spacer(Modifier.height(2.dp))
                        Text("Version 0.98", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                onClick = { menuGedcom = false; picker(true); lanceurImport.launch(arrayOf("*/*")) },
