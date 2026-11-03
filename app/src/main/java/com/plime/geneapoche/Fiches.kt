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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ---------------------------------------------------------------- Fiche personne
// Blocs modifiables par appui long uniquement. Les informations manquantes
// sont affichees en gris : elles alimentent le journal de recherches.


// Couleur de la pastille : rose (F), bleu (M), vert p\u00E2le si sexe absent.
fun couleurPastille(sexe: String): Color = when (sexe) {
    "F" -> CouleurRose
    "M" -> CouleurBleu
    else -> CouleurNeutreDoux
}

// Pastille ronde au num\u00E9ro (ou initiale), identique \u00E0 la liste Personnes :
// taille de police adapt\u00E9e aux num\u00E9ros d'Aboville longs.
@Composable
fun PastilleNumero(p: Personne, diametre: Int) {
    // G\u00E9n\u00E9aPoche 5 : pastille VIDE si la personne n'est ni Sosa ni
    // d'Aboville (plus de repli sur l'initiale).
    val num = p.numeroBrut()
    Box(
        Modifier.size(diametre.dp).clip(CircleShape).background(couleurPastille(p.sexe)),
        contentAlignment = Alignment.Center
    ) {
        if (num.isNotEmpty()) {
            Text(
                num,
                color = CouleurVertFonce,
                fontWeight = FontWeight.Bold,
                fontSize = when {
                    num.length <= 3 -> (diametre * 15 / 40).sp
                    num.length <= 5 -> (diametre * 12 / 40).sp
                    else -> (diametre * 9 / 40).sp
                },
                maxLines = 1
            )
        }
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
    // G\u00E9n\u00E9aPoche 2 : enregistrement d'un acte saisi depuis la fiche
    // (bloc \u00C9v\u00E9nement ou appui long sur un bloc N/B/M/S/D).
    onEnregistrerActe: (ActeSaisie) -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    // Coche de validation d'une information : (section, valide).
    // Une coche negative cree automatiquement une recherche dans le journal.
    onValider: (String, Boolean) -> Unit,
    // Recherches ouvertes de cette personne (bloc "Recherches" de la fiche)
    // et suppression directe d'une recherche.
    pistes: List<Piste> = emptyList(),
    onSupprimerPiste: (Piste) -> Unit = {},
    // G\u00E9n\u00E9aPoche 3 : \u00E9dition directe de l'identit\u00E9 (nom, pr\u00E9noms,
    // profession) depuis la fiche, et gestion des \u00E9v\u00E9nements libres
    // (intitul\u00E9 libre) de la chronologie.
    onEditerIdentite: () -> Unit = {},
    onModifierEvenementLibre: (EvenementLibre) -> Unit = {},
    onSupprimerEvenementLibre: (Long) -> Unit = {},
    // G\u00E9n\u00E9aPoche 6 : bouton Arbre de la fiche : cette personne
    // devient le centre de l'arbre g\u00E9n\u00E9alogique.
    onVoirArbre: () -> Unit = {}
) {
    // GénéaPoche 5 : tous les cadres REPLIÉS sur une ligne à l'ouverture
    // de la fiche ; le toucher d'un cadre le développe, l'appui long le modifie.
    var ouverts by remember { mutableStateOf(setOf<String>()) }
    // Dialogues : mariage, enfant, parent, temoin.
    var mariageOuvert by remember { mutableStateOf(false) }
    var parentRole by remember { mutableStateOf("") }    // G\u00E9n\u00E9aPoche 2 : formulaire de saisie d'acte ouvert en plein \u00E9cran
    // depuis la fiche. null = ferm\u00E9 ; "" = nouvel \u00E9v\u00E9nement ; "N"/"B"/"M"/"S"/"D"
    // = bloc de la frise \u00E9dit\u00E9 par appui long (formulaire pr\u00E9-rempli).
    var saisieOuverte by remember { mutableStateOf<String?>(null) }
    // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nement libre en cours d'\u00E9dition (son id).
    var evLibreEdite by remember { mutableStateOf<String?>(null) }
    // G\u00E9n\u00E9aPoche 7 : ajout d'un enfant depuis la fiche (choisir une
    // fiche existante ou saisir un nouvel enfant).
    var menuEnfant by remember { mutableStateOf(false) }
    var enfantSaisie by remember { mutableStateOf(false) }

    // G\u00E9n\u00E9aPoche 2 : la fiche REGROUPE N, M et D (pr\u00E9sents m\u00EAme sans
    // information) ; B et S apparaissent d\u00E8s qu'une donn\u00E9e existe ; le tout
    // est rang\u00E9 dans l'ORDRE CHRONOLOGIQUE (les \u00E9v\u00E9nements sans ann\u00E9e
    // restent en haut dans l'ordre naturel).
    // R\u00E8gle des 100 ans : la notion de d\u00E9c\u00E8s ou de s\u00E9pulture ne s'affiche
    // que si la personne peut avoir plus de 100 ans \u00E0 la date du jour (ou si
    // son d\u00E9c\u00E8s est d\u00E9j\u00E0 connu).
    val decesAffiche = decesAffichable(p, actes)
    val ordreNaturel = listOf("N", "B", "M", "S", "D")
    val typesPresents = mutableListOf("N", "M")
    if (p.blocsOuverts.contains("bapteme") || p.dateBapteme.isNotBlank() || p.heureBapteme.isNotBlank() || p.lieuBapteme.isNotBlank() || actes.any { it.personneId == p.id && it.type == "B" }) {
        typesPresents.add("B")
    }
    if (decesAffiche) {
        typesPresents.add("D")
        if (p.blocsOuverts.contains("sepulture") || p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank() || actes.any { it.personneId == p.id && it.type == "S" }) {
            typesPresents.add("S")
        }
    }
    val triChronologique = typesPresents.sortedWith(
        compareBy(
            { anneeDe(champsEvenement(p, it).date) ?: ordreNaturel.indexOf(it) },
            { ordreNaturel.indexOf(it) }
        )
    ).toMutableList()
    // G\u00E9n\u00E9aPoche 7 : ordre STRICTEMENT chronologique — le bloc
    // Naissance n'est plus forc\u00E9 en t\u00EAte de la frise.
    val ordreTypes = triChronologique.toList()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // G\u00E9n\u00E9aPoche 3 : pastille + titre + compteur des
                    // renseignements essentiels sur 6 (N date, N commune, N
                    // d\u00E9partement, D date, D commune, D d\u00E9partement), rouge
                    // \u00E0 vert. Appui long : \u00E9dition de l'identit\u00E9.
                    Row(
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onEditerIdentite,
                            onLongClickLabel = "Modifier l'identit\u00E9"
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PastilleNumero(p = p, diametre = 30)
                        Spacer(Modifier.width(8.dp))
                        Text(p.titre(), Modifier.weight(1f, fill = false), fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        val n6 = renseignementsN6(p)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = couleurCompteur(n6, 6)
                        ) {
                            Text(
                                "  " + n6.toString() + "/6  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
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
                    // G\u00E9n\u00E9aPoche 6 : bouton Arbre : la personne de la
                    // fiche devient le centre de l'arbre (synchronisation
                    // instantan\u00E9e).
                    IconButton(onClick = onVoirArbre) {
                        Icon(painterResource(R.drawable.ic_arbre), contentDescription = "Arbre")
                    }
                    // Pas de crayon : l'edition s'ouvre par appui long sur le
                    // bloc identite (v0.28).
                    IconButton(onClick = onSupprimer) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                    }
                }
            )
        },
    ) { marges ->
        LazyColumn(
            // G\u00E9n\u00E9aPoche 3 : cartouche d'identit\u00E9 et parents FIG\u00C9 en t\u00EAte
            // de fiche pendant le d\u00E9filement ; la chronologie d\u00E9file dessous.
            Modifier.fillMaxSize().padding(marges).padding(horizontal = 12.dp)
        ) {
            stickyHeader(key = "tete") {
                Column(Modifier.fillMaxWidth()) {
                    // G\u00E9n\u00E9aPoche 5 : le cadre de la personne est
                    // SUPPRIM\u00C9 (l'identit\u00E9 est dans la barre de titre) ;
                    // seul le cadre des parents reste fig\u00E9 en t\u00EAte.
                    CarteParents(
                        p = p,
                        personnes = personnes,
                        onOuvrir = onOuvrir,
                        onNouveauParent = { parentRole = it }
                    )
                }
            }
            // G\u00E9n\u00E9aPoche 3 : chronologie UNIQUE. Tous les \u00E9v\u00E9nements
            // (N/B/M/S/D et \u00E9v\u00E9nements libres) ET les enfants sont rang\u00E9s
            // par date. Chaque \u00E9v\u00E9nement affiche l'\u00E2ge entre parenth\u00E8ses.
            item {
                Column(Modifier.fillMaxWidth()) {
                    val evsFamille = evenementsFamille(p, personnes)
                    val entrees = mutableListOf<Pair<Int, String>>()
                    ordreTypes.forEach { t ->
                        entrees.add((anneeDe(champsEvenement(p, t).date) ?: -1) to ("ev:" + t))
                    }
                    p.evenementsLibres.forEach { ev ->
                        entrees.add((anneeDe(ev.date) ?: -1) to ("evl:" + ev.id.toString()))
                    }
                    p.enfants.forEach { idE ->
                        val eE = personneParId(personnes, idE)
                        entrees.add((eE?.let { anneeDe(it.dateNaissance) } ?: -1) to ("enf:" + idE.toString()))
                    }
                    // G\u00E9n\u00E9aPoche 7 : les \u00E9v\u00E9nements de la famille
                    // (mariages et d\u00E9c\u00E8s des parents, de la fratrie et des
                    // enfants) entrent dans la M\u00CAME chronologie, tri\u00E9s par
                    // date avec tous les autres \u00E9v\u00E9nements.
                    evsFamille.forEachIndexed { iFam, ef ->
                        entrees.add(ef.first to ("fam:" + iFam.toString()))
                    }
                    // G\u00E9n\u00E9aPoche 7 : ordre STRICTEMENT chronologique
                    // (les \u00E9v\u00E9nements sans ann\u00E9e restent en haut).
                    val ordonnees = entrees.sortedBy { it.first }
                    ordonnees.forEach { entree ->
                        val cle = entree.second
                        when {
                            cle.startsWith("evl:") -> {
                                val ev = p.evenementsLibres.firstOrNull { ("evl:" + it.id.toString()) == cle }
                                if (ev != null) {
                                    Card(
                                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, CouleurOrange)
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().combinedClickable(
                                                onClick = { evLibreEdite = ev.id.toString() },
                                                onClickLabel = "Modifier cet \u00E9v\u00E9nement",
                                                onLongClick = { evLibreEdite = ev.id.toString() },
                                                onLongClickLabel = "Modifier cet \u00E9v\u00E9nement"
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(CouleurOrange))
                                            Spacer(Modifier.width(8.dp))
                                            val dTxtEvl = dateLigne(ev.date.trim()).ifEmpty { "  /  /    " }
                                            val aTxtEvl = ageAffiche(p.dateNaissance, ev.date)
                                            val lTxtEvl = if (ev.lieu.trim().isNotEmpty()) " \u00E0 " + ev.lieu.trim() else ""
                                            Text(
                                                dTxtEvl +
                                                    (if (aTxtEvl.isNotEmpty()) " " + aTxtEvl else "") + " " +
                                                    ev.intitule.trim().ifEmpty { "\u00C9v\u00E9nement" } + lTxtEvl,
                                                Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = if (ev.date.trim().isEmpty() && ev.lieu.trim().isEmpty()) GrisManquant else Color.Unspecified
                                            )
                                            Icon(Icons.Filled.Create, contentDescription = null, Modifier.size(14.dp), tint = GrisManquant)
                                        }
                                    }
                                }
                            }
                            cle.startsWith("fam:") -> {
                                // G\u00E9n\u00E9aPoche 7 : \u00E9v\u00E9nement de la famille
                                // (parents, fratrie, enfants) dans la chronologie.
                                val iFam = cle.removePrefix("fam:").toIntOrNull() ?: 0
                                val ef = evsFamille.getOrNull(iFam)
                                if (ef != null) {
                                    Card(
                                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, CouleurBordVert)
                                    ) {
                                        Text(
                                            ef.second,
                                            Modifier.fillMaxWidth().padding(6.dp),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            cle.startsWith("enf:") -> {
                                val idE = cle.removePrefix("enf:").toLongOrNull() ?: 0L
                                val e = personneParId(personnes, idE)
                                Card(
                                    Modifier.fillMaxWidth().padding(top = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, if (e != null) couleurPastille(e.sexe) else GrisManquant)
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(6.dp).clickable { onOuvrir(idE) }
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
                                                val dTxtEnf = dateLigne(dEnf).ifEmpty { "  /  /    " }
                                                val lTxtEnf = if (lEnf.isNotEmpty()) lEnf + (if (numEnf != null) " (" + numEnf.toString() + ")" else "") else ""
                                                // G\u00E9n\u00E9aPoche 7 : l'\u00E2ge du PARENT \u00E0 la
                                                // naissance ou au d\u00E9c\u00E8s de l'enfant.
                                                val aTxtEnf = ageAffiche(p.dateNaissance, dEnf)
                                                Text(
                                                    dTxtEnf + " " + tEnf +
                                                        (if (lTxtEnf.isNotEmpty()) " \u00E0 " + lTxtEnf else "") +
                                                        (if (aTxtEnf.isNotEmpty()) " " + aTxtEnf else ""),
                                                        style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                val t = cle.removePrefix("ev:")
                                val cEv = champsEvenement(p, t)
                                val dEv = cEv.date.trim()
                                val lEv = cEv.lieu.trim()
                                val dpEv = cEv.dept.trim()
                                val numDept = dpEv.toIntOrNull() ?: numeroDepartement(dpEv) ?: numeroDepartement(lEv)
                                // Couleur du bloc selon les informations remplies : rouge
                                // (rien), orange (partiel), vert (date, heure, lieu, dept).
                                val remplisEv = listOf(dEv, cEv.heure.trim(), lEv, dpEv).count { it.isNotEmpty() }
                                val couleurBlocEv = when {
                                    remplisEv >= 4 -> CouleurVert
                                    remplisEv >= 1 -> CouleurOrange
                                    else -> CouleurRouge
                                }
                                Card(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, couleurBlocEv)
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .padding(vertical = 1.dp)
                                                .combinedClickable(
                                                    onClick = { ouverts = if (ouverts.contains(t)) ouverts - t else ouverts + t },
                                                    onClickLabel = "Ouvrir ou replier ce bloc",
                                                    onLongClick = { saisieOuverte = t },
                                                    onLongClickLabel = "Modifier ce bloc"
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                        Box(
                                            Modifier.size(10.dp).clip(CircleShape)
                                                .background(couleurBlocEv)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val dTxt = dateLigne(dEv).ifEmpty { "  /  /    " }
                                        val aTxt = if (t == "N") "" else ageAffiche(p.dateNaissance, dEv)
                                        val lTxt = if (lEv.isNotEmpty()) lEv + (if (numDept != null) " (" + numDept.toString() + ")" else "") else ""
                                        Text(
                                            dTxt + (if (aTxt.isNotEmpty()) " " + aTxt else "") + " " + t +
                                                (if (t == "M") conjointDeTxt(p, personnes) else "") +
                                                (if (lTxt.isNotEmpty()) " \u00E0 " + lTxt else ""),
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (dEv.isEmpty() && lEv.isEmpty()) GrisManquant else Color.Unspecified
                                        )
                                        // G3-11 : le remplissage n'est pas signale par
                                        // la couleur seule, un mot l'explicite.
                                        Text(
                                            if (remplisEv >= 4) "complet" else if (remplisEv >= 1) "partiel" else "\u00E0 compl\u00E9ter",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (remplisEv >= 4) CouleurVert else if (remplisEv >= 1) CouleurOrange else CouleurRouge
                                        )
                                    }
                                    // G3-15 : la frise s'anime a l'ouverture du bloc.
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = ouverts.contains(t),
                                        enter = androidx.compose.animation.expandVertically(),
                                        exit = androidx.compose.animation.shrinkVertically()
                                    ) {
                                        CarteEvenement(
                                            titre = libelleType(t),
                                            typeActe = t,
                                            p = p,
                                            personnes = personnes,
                                            actes = actes.filter { it.personneId == p.id && it.type == t },
                                            onEditer = { saisieOuverte = t },
                                            onOuvrir = onOuvrir,
                                            onChoisir = onChoisir,
                                            onRetirer = onRetirer,
                                            onOuvrirMariage = { mariageOuvert = true },
                                            onOuvrirParent = { parentRole = it },
                                            onSupprimerActe = onSupprimerActe,
                                            onVoirActe = onVoirActe,
                                            onEnregistrer = enregistrer,
                                            pistes = pistes,                                            modifier = Modifier
                                        )
                                    }
                                    }
                                }
                                }
                            }
                        }
                    // G\u00E9n\u00E9aPoche 7 : les \u00E9v\u00E9nements de la famille figurent
                    // d\u00E9sormais DANS la chronologie unique ci-dessus, plus de
                    // bloc s\u00E9par\u00E9.
                    // Bloc RECHERCHES : pistes ouvertes de cette personne,
                    // supprimables directement.
                    val pistesPers = pistes.filter { it.personneId == p.id && !it.faite }
                    if (pistesPers.isNotEmpty()) {
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CouleurBordVert)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(6.dp)) {
                                Text(
                                    "Recherches",
                                    fontWeight = FontWeight.Bold,
                                    color = CouleurVertFonce,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                pistesPers.forEach { ps ->
                                    // Reponse utilisateur : sur la FICHE, le nom de la
                                    // personne concernee ne se repete pas (dans le
                                    // journal, il reste affiche).
                                    val texteFiche = ps.texte
                                        .replace(titreCourt(p), "")
                                        .replace(Regex("\\s+de\\s*$"), "")
                                        .replace(Regex("\\s{2,}"), " ")
                                        .trim()
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            texteFiche.ifEmpty { ps.texte },
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
                    // G\u00E9n\u00E9aPoche 2 : bloc \u00C9V\u00C9NEMENT encadr\u00E9 sans libell\u00E9
                    // (remplace les deux rang\u00E9es de boutons) : il ouvre la
                    // m\u00EAME saisie que le menu +Saisie, pr\u00E9-rempli avec les
                    // informations connues de la personne.
                    Card(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CouleurBordVert)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(8.dp)
                                .clickable { saisieOuverte = "" },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Add, contentDescription = null,
                                Modifier.size(16.dp), tint = CouleurVertFonce
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Ajouter un \u00E9v\u00E9nement",
                                color = CouleurVertFonce,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // G\u00E9n\u00E9aPoche 7 : bouton Ajouter un enfant (7.1).
                    Card(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CouleurBordVert)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(8.dp)
                                .clickable { menuEnfant = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Person, contentDescription = null,
                                Modifier.size(16.dp), tint = CouleurVertFonce
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Ajouter un enfant",
                                color = CouleurVertFonce,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    }
                    // G\u00E9n\u00E9aPoche 5 : bloc NOTES tout en bas de la fiche.
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CouleurGrisBord)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(6.dp)) {
                            var libre by remember(p.id) { mutableStateOf(p.notesLibres) }
                            OutlinedTextField(
                                value = libre,
                                onValueChange = { libre = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Notes", style = MaterialTheme.typography.bodySmall) },
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { p.notesLibres = libre.trim(); enregistrer() }) {
                                Text("Enregistrer", fontSize = 12.sp)
                            }
                        }
                    }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    // G\u00E9n\u00E9aPoche 2 : formulaire de saisie d'acte EN PLEIN \u00C9CRAN, le m\u00EAme
    // que l'onglet Saisie. Pr\u00E9-rempli avec tout ce qui est connu de la
    // personne (identit\u00E9, parents, conjoint, t\u00E9moins) et de l'\u00E9v\u00E9nement
    // touch\u00E9 par appui long. L'enregistrement met \u00E0 jour la fiche, la
    // num\u00E9rotation, la coh\u00E9rence et le journal.
    saisieOuverte?.let { tPre ->
        Dialog(
            onDismissRequest = { saisieOuverte = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(Modifier.fillMaxSize()) {
                EcranSaisieActe(
                    personnes = personnes,
                    initial = prefillActe(p, personnes, actes, tPre.ifEmpty { null }),
                    // GeneaPoche 4 : appui long sur un bloc = SEUL ce type d'acte.
                    typeImpose = tPre.ifEmpty { null },
                    onEnregistrer = { a ->                        onEnregistrerActe(a)
                        saisieOuverte = null
                    },
                    onFermer = { saisieOuverte = null }
                )
            }
        }
    }

    // G\u00E9n\u00E9aPoche 3 : \u00E9dition d'un \u00E9v\u00E9nement libre de la chronologie.
    if (evLibreEdite != null) {
        val ev = p.evenementsLibres.firstOrNull { it.id.toString() == evLibreEdite }
        if (ev != null) {
            DialogueEvenementLibre(
                initial = ev,
                onValider = { intitule, date, heure, lieu, dept ->
                    onModifierEvenementLibre(ev.copy(intitule = intitule, date = date, heure = heure, lieu = lieu, dept = dept))
                    evLibreEdite = null
                },
                onSupprimer = {
                    onSupprimerEvenementLibre(ev.id)
                    evLibreEdite = null
                },
                onAnnuler = { evLibreEdite = null }
            )
        }
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

    // G\u00E9n\u00E9aPoche 7 : ajout d'un enfant (7.1). Deux chemins :
    // choisir une fiche existante (picker) ou saisir un nouvel enfant.
    if (menuEnfant) {
        AlertDialog(
            onDismissRequest = { menuEnfant = false },
            title = { Text("Ajouter un enfant") },
            text = { Text("Choisir une fiche existante ou saisir un nouvel enfant\u2026") },
            confirmButton = {
                TextButton(onClick = { menuEnfant = false; onChoisir("enfant") }) {
                    Text("Choisir une fiche")
                }
            },
            dismissButton = {
                TextButton(onClick = { menuEnfant = false; enfantSaisie = true }) {
                    Text("Saisir un enfant")
                }
            }
        )
    }
    if (enfantSaisie) {
        DialogueNouvelEnfant(
            onValider = { nom, prenoms, sexe, date ->
                onNouvelEnfant(nom, prenoms, sexe, date)
                enfantSaisie = false
            },
            onAnnuler = { enfantSaisie = false }
        )
    }

}
// ---------------------------------------------------------------- Cartouche identite
// G\u00E9n\u00E9aPoche 3 : identit\u00E9 seule (nom, pr\u00E9noms, profession si
// renseign\u00E9e), sans \u00E9tiquette ni bouton. Appui long : \u00E9dition.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteIdentite(
    p: Personne,
    onEditer: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, couleurPastille(p.sexe))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(6.dp).combinedClickable(
                onClick = {},
                onLongClick = onEditer,
                onLongClickLabel = "Modifier l'identit\u00E9"
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PastilleNumero(p = p, diametre = 28)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.nom.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(p.prenoms, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (p.profession.trim().isNotEmpty()) {
                Text(p.profession.trim(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------------------------------------------------------------- Cadre parents
// G\u00E9n\u00E9aPoche 3 : p\u00E8re et m\u00E8re r\u00E9unis dans un seul cadre, affich\u00E9s
// comme les enfants (pastille, titre, N. et D.). Toucher ouvre leur
// fiche ; appui long : choisir ou cr\u00E9er le parent.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteParents(
    p: Personne,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit,
    onNouveauParent: (String) -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurGrisBord)
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            listOf("P\u00E8re" to p.pere, "M\u00E8re" to p.mere).forEach { paire ->
                val role = if (paire.first == "P\u00E8re") "pere" else "mere"
                val idParent = paire.second
                val q = personneParId(personnes, idParent)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 2.dp)
                        .combinedClickable(
                            onClick = { if (q != null) onOuvrir(idParent) else onNouveauParent(role) },
                            onClickLabel = "Ouvrir la fiche",
                            onLongClick = { onNouveauParent(role) },
                            onLongClickLabel = "Choisir ou cr\u00E9er ce parent"
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(paire.first, Modifier.width(44.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    if (q != null) {
                        PastilleNumero(p = q, diametre = 28)
                    } else {
                        Box(Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            q?.titre() ?: "\u2014",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (q == null) GrisManquant else Color.Unspecified
                        )
                        if (q != null) {
                            val nd = mutableListOf<String>()
                            val nP = dateAffichee(q.dateNaissance.trim())
                            val dP = dateAffichee(q.dateDeces.trim())
                            if (nP.isNotEmpty()) nd.add("N. " + nP)
                            if (dP.isNotEmpty()) nd.add("D. " + dP)
                            if (nd.isNotEmpty()) {
                                Text(nd.joinToString(" \u2013 "), style = MaterialTheme.typography.bodySmall)
                            }
                            // G\u00E9n\u00E9aPoche 5 : la PROFESSION du parent
                            // appara\u00EEt sans \u00E9tiquette, rien si inconnue.
                            if (q.profession.trim().isNotEmpty()) {
                                Text(
                                    q.profession.trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------ Dialogue d'un \u00E9v\u00E9nement libre
// G\u00E9n\u00E9aPoche 3 : intitul\u00E9 libre (profession, d\u00E9m\u00E9nagement, etc.),
// date, heure, commune et d\u00E9partement. Range l'\u00E9v\u00E9nement dans la
// chronologie de la fiche.

@Composable
fun DialogueEvenementLibre(
    initial: EvenementLibre,
    onValider: (String, String, String, String, String) -> Unit,
    onSupprimer: () -> Unit,
    onAnnuler: () -> Unit
) {
    var intitule by remember { mutableStateOf(initial.intitule) }
    var date by remember { mutableStateOf(initial.date) }
    var heure by remember { mutableStateOf(initial.heure) }
    var lieu by remember { mutableStateOf(initial.lieu) }
    var dept by remember { mutableStateOf(initial.dept) }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("\u00C9v\u00E9nement") },
        text = {
            Column {
                OutlinedTextField(value = intitule, onValueChange = { intitule = it }, label = { Text("Intitul\u00E9") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (jj/mm/aaaa)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = heure, onValueChange = { heure = it }, label = { Text("Heure") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lieu, onValueChange = { lieu = it }, label = { Text("Commune") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dept, onValueChange = { dept = it }, label = { Text("D\u00E9partement") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(intitule, date, heure, lieu, dept) }) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSupprimer) { Text("Supprimer", color = CouleurRouge) }
                TextButton(onClick = onAnnuler) { Text("Annuler") }
            }
        }
    )
}

// ---------------------------------------------------------------- Bloc d'evenement
// Tout ramasse : date, heure, lieu, departement, parents (avec age a l'acte
// et date de naissance deduite a l'annee), conjoint, temoins.
// Icones : ajout d'une photo d'acte.
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

// ---------------------------------------------------------------- Pr\u00E9-remplissage
// G\u00E9n\u00E9aPoche 2 : construit l'ActeSaisie qui pr\u00E9-remplit le formulaire
// de saisie d'acte avec tout ce qui est CONNU de la personne (identit\u00E9,
// parents, conjoint, t\u00E9moins, signataires) et de l'\u00E9v\u00E9nement demand\u00E9.
// t = null (nouvel \u00E9v\u00E9nement depuis le bloc vert) : identit\u00E9 seule.

fun prefillActe(p: Personne, personnes: List<Personne>, actes: List<Acte>, t: String?): ActeSaisie {
    val type = t ?: "N"
    val c = if (t == null || type == "E") null else champsEvenement(p, type)
    val acteEdit = actes.filter { it.personneId == p.id && it.type == type }
        .maxByOrNull { it.creation }
    val pere = personneParId(personnes, p.pere)
    val mere = personneParId(personnes, p.mere)
    val conj = if (type == "M") personneParId(personnes, p.conjoint) else null
    val pConj = if (conj != null) personneParId(personnes, conj.pere) else null
    val mConj = if (conj != null) personneParId(personnes, conj.mere) else null
    val temoinsIds = p.temoinsDe(type)
    val signataires = p.signatairesDe(type).mapNotNull { id ->
        when {
            id == 0L -> "P\u00E8re"
            id == p.id -> "Concerne(e)"
            mere != null && id == mere.id -> "M\u00E8re"
            conj != null && id == conj.id -> "Mari\u00E9e"
            else -> {
                val i = temoinsIds.indexOf(id)
                if (i >= 0) "T\u00E9moin " + (i + 1).toString() else null
            }
        }
    }
    return ActeSaisie(
        acteId = acteEdit?.id ?: 0L,
        type = type,
        commune = c?.lieu?.trim() ?: "",
        dept = c?.dept?.trim() ?: "",
        reference = acteEdit?.reference?.trim() ?: "",
        date = c?.date?.trim() ?: "",
        heure = c?.heure?.trim() ?: "",
        personneId = p.id,
        nom = p.nom, prenoms = p.prenoms, sexe = p.sexe, profession = p.profession,
        pereNom = pere?.nom ?: "", perePrenoms = pere?.prenoms ?: "",
        pereNe = pere?.dateNaissance ?: "", pereLieu = pere?.lieuNaissance ?: "",
        pereDept = pere?.deptNaissance ?: "", pereProf = pere?.profession ?: "",
        pereDeces = pere?.dateDeces ?: "", pereLieuDeces = pere?.lieuDeces ?: "",
        mereNom = mere?.nom ?: "", merePrenoms = mere?.prenoms ?: "",
        mereNe = mere?.dateNaissance ?: "", mereLieu = mere?.lieuNaissance ?: "",
        mereDept = mere?.deptNaissance ?: "", mereProf = mere?.profession ?: "",
        mereDeces = mere?.dateDeces ?: "", mereLieuDeces = mere?.lieuDeces ?: "",
        marieeId = conj?.id ?: 0L,
        marieeNom = conj?.nom ?: "", marieePrenoms = conj?.prenoms ?: "",
        marieeProf = conj?.profession ?: "",
        pMarieeNom = pConj?.nom ?: "", pMarieePrenoms = pConj?.prenoms ?: "",
        pMarieeNe = pConj?.dateNaissance ?: "", pMarieeLieu = pConj?.lieuNaissance ?: "",
        pMarieeDept = pConj?.deptNaissance ?: "", pMarieeProf = pConj?.profession ?: "",
        pMarieeDeces = pConj?.dateDeces ?: "", pMarieeLieuDeces = pConj?.lieuDeces ?: "",
        mMarieeNom = mConj?.nom ?: "", mMarieePrenoms = mConj?.prenoms ?: "",
        mMarieeNe = mConj?.dateNaissance ?: "", mMarieeLieu = mConj?.lieuNaissance ?: "",
        mMarieeDept = mConj?.deptNaissance ?: "", mMarieeProf = mConj?.profession ?: "",
        mMarieeDeces = mConj?.dateDeces ?: "", mMarieeLieuDeces = mConj?.lieuDeces ?: "",
        temoins = temoinsIds.mapNotNull { id ->
            personneParId(personnes, id)?.let { q -> TemoinSaisie(q.nom, q.prenoms, q.profession) }
        },
        signataires = signataires
    )
}

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

// Conjoint connu de la personne, affiche apres son titre (" avec X").
fun conjointDeTxt(p: Personne, personnes: List<Personne>): String {
    val c = personneParId(personnes, p.conjoint) ?: return ""
    return " avec " + c.titre()
}

// Evenements lies a la famille (reponse utilisateur) : deces des parents et
// grands-parents, mariages et deces des freres et soeurs, mariages et deces
// des enfants. Chaque ligne : (annee, "jj/mm/aaaa Texte a Lieu (dep)"),
// triee par annee croissante.
fun evenementsFamille(p: Personne, personnes: List<Personne>): List<Pair<Int, String>> {
    fun lieuAvecDept(lieu: String, dept: String): String {
        val l = lieu.trim()
        if (l.isEmpty()) return ""
        val d = dept.trim()
        val num = d.toIntOrNull() ?: numeroDepartement(d) ?: numeroDepartement(l)
        return l + (if (num != null) " (" + num.toString() + ")" else "")
    }
    fun ligne(date: String, lieu: String, dept: String, texte: String): Pair<Int, String>? {
        val d = date.trim()
        if (d.isEmpty()) return null
        val an = anneeDe(d) ?: return null
        val lt = lieuAvecDept(lieu, dept)
        // G\u00E9n\u00E9aPoche 7 : date en t\u00EAte + \u00E2ge de la personne
        // centrale entre parenth\u00E8ses (7.3 et 7.5).
        val a = ageAffiche(p.dateNaissance, d)
        return an to (dateLigne(d) + (if (a.isNotEmpty()) " " + a else "") + " " + texte + (if (lt.isNotEmpty()) " \u00E0 " + lt else ""))
    }
    val res = mutableListOf<Pair<Int, String>>()
    fun decesRelatif(r: Personne, lien: String) {
        // Regle des 100 ans : un deces ne s'affiche que s'il est connu
        // ou possible (la personne est decedee ou nee il y a plus de 100 ans).
        if (!decesAffichable(r)) return
        ligne(r.dateDeces, r.lieuDeces, r.deptDeces, "D\u00E9c\u00E8s de " + lien + " " + r.titre())?.let { res.add(it) }
    }
    // Parents et grands-parents.
    val pere = personneParId(personnes, p.pere)
    val mere = personneParId(personnes, p.mere)
    pere?.let {
        decesRelatif(it, "son p\u00E8re")
        personneParId(personnes, it.pere)?.let { gp -> decesRelatif(gp, "son grand-p\u00E8re paternel") }
        personneParId(personnes, it.mere)?.let { gm -> decesRelatif(gm, "sa grand-m\u00E8re paternelle") }
    }
    mere?.let {
        decesRelatif(it, "sa m\u00E8re")
        personneParId(personnes, it.pere)?.let { gp -> decesRelatif(gp, "son grand-p\u00E8re maternel") }
        personneParId(personnes, it.mere)?.let { gm -> decesRelatif(gm, "sa grand-m\u00E8re maternelle") }
    }
    // Freres et soeurs : autres enfants du pere et/ou de la mere.
    val idsFratrie = mutableSetOf<Long>()
    listOfNotNull(pere, mere).forEach { par -> par.enfants.forEach { id -> if (id != p.id) idsFratrie.add(id) } }
    idsFratrie.mapNotNull { personneParId(personnes, it) }.forEach { fr ->
        val lienFr = if (fr.sexe == "F") "sa soeur" else "son fr\u00E8re"
        ligne(
            fr.dateMariage, fr.lieuMariage, fr.deptMariage,
            "Mariage de " + lienFr + " " + fr.titre() + conjointDeTxt(fr, personnes)        )?.let { res.add(it) }
        decesRelatif(fr, lienFr)
    }
    // Enfants : mariage (la personne y est presente et consentante) et deces.
    p.enfants.mapNotNull { personneParId(personnes, it) }.forEach { e ->
        val lienE = if (e.sexe == "F") "sa fille" else "son fils"
        ligne(e.dateMariage, e.lieuMariage, e.deptMariage, "Mariage de " + lienE + " " + e.titre() + conjointDeTxt(e, personnes))?.let { res.add(it) }
        if (decesAffichable(e)) {
            ligne(e.dateDeces, e.lieuDeces, e.deptDeces, "D\u00E9c\u00E8s de " + lienE + " " + e.titre())?.let { res.add(it) }
        }
    }
    return res.sortedBy { it.first }
}

// Bloc de la frise : evenements de la famille, sans etre repliables.
@Composable
fun EvenementsFamille(p: Personne, personnes: List<Personne>) {
    val evs = remember(p.id, personnes.size) { evenementsFamille(p, personnes) }
    if (evs.isEmpty()) return
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurBordVert)
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            evs.forEach { ev ->
                Text(
                    ev.second,
                    Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

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
                Modifier.weight(1f).heightIn(min = 48.dp).clickable(enabled = onOuvrir != null) { onOuvrir?.invoke() },
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
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    onEnregistrer: () -> Unit,    // Recherches ouvertes (la premiere de la section alimente le sous-bloc).
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
    val pisteEv = pistes.firstOrNull { it.personneId == p.id && !it.faite && it.categorie.startsWith(sectionDe(typeActe)) }
    // Le bloc est maintenant affiche DANS la carte de l'evenement (v0.28) :
    // simple colonne, plus de carte imbriquee.
    Column(modifier.fillMaxWidth().padding(top = 4.dp)) {
        // Entete : titre + coche + photo d'acte. Plus de crayon (reponse
        // utilisateur) : l'edition du bloc ouvert se fait par APPUI LONG sur
        // cette entete (ou sur la ligne repliee de la frise).
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onEditer, onLongClickLabel = "Modifier ce bloc"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titre, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                // G\u00E9n\u00E9aPoche 5 : la croix de retrait du p\u00E8re est
                // SUPPRIM\u00C9E ; les boutons n'apparaissent que si le
                // parent est inconnu.
                if (p.pere == 0L) {
                    OutlinedButton(onClick = { onChoisir("pere") }, Modifier.height(32.dp)) {
                        Text("Choisir", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onOuvrirParent("pere") }) {
                        Text("Cr\u00E9er", fontSize = 12.sp)
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
                // G\u00E9n\u00E9aPoche 5 : la croix de retrait de la m\u00E8re est
                // SUPPRIM\u00C9E.
                if (p.mere == 0L) {
                    OutlinedButton(onClick = { onChoisir("mere") }, Modifier.height(32.dp)) {
                        Text("Choisir", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onOuvrirParent("mere") }) {
                        Text("Cr\u00E9er", fontSize = 12.sp)
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
                // G\u00E9n\u00E9aPoche 5 : la croix de retrait du conjoint est
                // SUPPRIM\u00C9E.
                if (p.conjoint == 0L) {
                    OutlinedButton(onClick = { onChoisir("conjoint") }, Modifier.height(32.dp)) {
                        Text("Choisir", fontSize = 12.sp)
                    }
                    TextButton(onClick = onOuvrirMariage) {
                        Text("Saisir", fontSize = 12.sp)
                    }
                }
            }
            // G\u00E9n\u00E9aPoche 5 : les autres conjoints et les boutons
            // "Autre conjoint" / "Saisir un mariage" sont SUPPRIM\u00C9S du
            // bloc ; tout se saisit par le menu de modification (appui
            // long) ou le menu Saisie.

        }
        // GeneaPoche 4 : temoins et signature retires du bloc ; ils se
        // saisissent uniquement par le formulaire d'acte.
        // Actes stockes pour cet evenement (images + transcriptions) :
        // chacun dans son SOUS-BLOC avec sources et recherche libre.
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
// libre, le tout regroupe au meme endroit.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SousBlocActe(
    acte: Acte,
    piste: Piste?,
    onVoir: () -> Unit,
    onSupprimer: () -> Unit,
    onEnregistrer: () -> Unit
) {
    var note by remember(acte.id) { mutableStateOf(piste?.enCours ?: "") }
    Card(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CouleurCarte),
        border = BorderStroke(1.dp, CouleurBordVert)
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (acte.titre.trim().isNotEmpty()) acte.titre.trim() else "(sans titre)",
                    Modifier.weight(1f).clickable(onClick = onVoir, onClickLabel = "Voir l'image de l'acte"),                    style = MaterialTheme.typography.bodyMedium,
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
                        Text("Enregistrer", fontSize = 12.sp)
                    }
                }
            }
        }
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
    Row(        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (acte.titre.trim().isNotEmpty()) acte.titre.trim() else "(sans titre)",
            Modifier.weight(1f).clickable(onClick = onVoir, onClickLabel = "Voir l'image de l'acte"),            style = MaterialTheme.typography.bodyMedium,
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
        IconButton(onClick = onSupprimer) {            Icon(Icons.Filled.Close, contentDescription = "Supprimer")
        }
    }
}
// GeneaPoche 4 : ecran "Page et reference de l'acte" supprime.// Ligne masquee si vide (liens de la famille).
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
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
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
                        contentDescription = "Image de l'acte " + acte.titre.trim(),
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






