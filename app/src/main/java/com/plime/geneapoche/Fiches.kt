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
// ---------------------------------------------------------------- Ecran fiche
// v0.33 : fiche refondue.
// - En-tete fige : pastille au numero, nom et prenoms, compteur des actes.
//   Appui long sur l'en-tete = edition nom / prenoms / profession.
// - Cadres FIGES (visibles meme en defilant) : profession (si renseignee)
//   puis pere et mere dans le MEME cadre, affiches comme les enfants
//   (pastille au numero, meme police), toucher = leur fiche, appui long =
//   choisir ou creer.
// - CHRONOLOGIE automatique : tous les evenements ET les enfants ranges par
//   date, age entre parentheses pour chaque evenement. Toucher un evenement
//   ouvre son bloc ; appui long = saisie d'acte pre-remplie (menu Saisie).
// - Etiquette verte "Ajouter un evenement" (formulaire du menu Saisie,
//   pre-renseigne) et "Ajouter un enfant".
// - Plus de bandeau de boutons en bas de fiche.

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
    // v0.33 : ouvre le formulaire de saisie d'acte (menu Saisie) pour cette
    // personne, pre-rempli avec ses informations connues.
    onOuvrirSaisie: (String) -> Unit,
    // Coche de validation d'une information : (section, valide).
    // Une coche negative cree automatiquement une recherche dans le journal.
    onValider: (String, Boolean) -> Unit,
    // Recherches ouvertes de cette personne (bloc "Recherches" de la fiche)
    // et suppression directe d'une recherche.
    pistes: List<Piste> = emptyList(),
    onSupprimerPiste: (Piste) -> Unit = {}
) {
    // Blocs ouverts : par defaut tout est replie ; toucher ouvre le bloc.
    var ouverts by remember { mutableStateOf(setOf<String>()) }
    // Dialogues : mariage, enfant, parent, temoin.
    var mariageOuvert by remember { mutableStateOf(false) }
    var enfantOuvert by remember { mutableStateOf(false) }
    var menuEnfant by remember { mutableStateOf(false) }
    var parentRole by remember { mutableStateOf("") }
    var temoinType by remember { mutableStateOf("") }
    // Appui long sur un parent connu : choisir ou creer son remplacant.
    var parentMenu by remember { mutableStateOf("") }

    val decesAffiche = decesAffichable(p, actes)
    val anneeN = anneeDe(p.dateNaissance)

    // CHRONOLOGIE : chaque entree = un evenement ou un enfant, avec sa cle
    // de tri (annee). Les evenements sans annee vont en fin de liste, sauf
    // la naissance sans date, premier evenement de la vie, en tete.
    val entrees = mutableListOf<Triple<Int, String, Long>>()
    fun cleEvent(date: String, type: String): Int {
        val a = anneeDe(date)
        return a ?: if (type == "N") Int.MIN_VALUE else Int.MAX_VALUE
    }
    fun ajouterEvenement(type: String, existe: Boolean) {
        if (!existe) return
        entrees.add(Triple(cleEvent(champsEvenement(p, type).date, type), type, 0L))
    }
    ajouterEvenement("N", true)
    ajouterEvenement("B", p.blocsOuverts.contains("bapteme") || p.dateBapteme.isNotBlank() || p.heureBapteme.isNotBlank() || p.lieuBapteme.isNotBlank() || actes.any { it.personneId == p.id && it.type == "B" })
    ajouterEvenement("M", p.blocsOuverts.contains("mariage") || p.dateMariage.isNotBlank() || p.heureMariage.isNotBlank() || p.lieuMariage.isNotBlank() || p.conjoint != 0L || p.conjointsSupp.isNotEmpty() || p.enfants.isNotEmpty() || actes.any { it.personneId == p.id && it.type == "M" })
    if (decesAffiche) {
        ajouterEvenement("S", p.blocsOuverts.contains("sepulture") || p.dateSepulture.isNotBlank() || p.heureSepulture.isNotBlank() || p.lieuSepulture.isNotBlank() || actes.any { it.personneId == p.id && it.type == "S" })
        ajouterEvenement("D", true)
    }
    // Les enfants entrent dans la chronologie a leur date de naissance.
    p.enfants.forEach { idE ->
        val e = personneParId(personnes, idE)
        entrees.add(Triple(e?.let { anneeDe(it.dateNaissance) } ?: Int.MAX_VALUE, "E", idE))
    }
    entrees.sortBy { it.first }

    // Compteur des actes stockes de la personne (en-tete).
    val nbActes = actes.count { it.personneId == p.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // En-tete : pastille au numero, nom et prenoms, compteur
                    // des actes. Appui long : edition nom / prenoms /
                    // profession.
                    Row(
                        Modifier.combinedClickable(onClick = {}, onLongClick = onModifier, onLongClickLabel = "Modifier le nom, les pr\u00E9noms et la profession"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PastilleNumero(p = p, diametre = 36)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            p.titre() + "  \u00B7  " + nbActes.toString() + " acte" + (if (nbActes > 1) "s" else ""),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                    IconButton(onClick = onSupprimer) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                    }
                }
            )
        }
    ) { marges ->
        Column(Modifier.fillMaxSize().padding(marges)) {
            // Cadres FIGES : profession (seulement si renseignee) puis pere
            // et mere dans le meme cadre. Ils restent visibles pendant que
            // la chronologie defile.
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                if (p.profession.trim().isNotEmpty()) {
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, couleurPastille(p.sexe))
                    ) {
                        Text(
                            p.profession.trim(),
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                CarteParents(
                    p = p,
                    personnes = personnes,
                    onOuvrir = onOuvrir,
                    onChoisir = { role -> onChoisir(role) },
                    onCreer = { role -> parentRole = role },
                    onMenu = { role -> parentMenu = role },
                    onRetirer = onRetirer
                )
            }
            // CHRONOLOGIE : evenements et enfants ranges par date.
            LazyColumn(
                Modifier.fillMaxSize().weight(1f).padding(horizontal = 12.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onOuvrirSaisie("") },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CouleurVert),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ajouter un \u00E9v\u00E9nement", color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { menuEnfant = true },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CouleurVert),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ajouter un enfant", color = Color.White, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        entrees.forEach { entree ->
                            val t = entree.second
                            if (t != "E") {
                                val cEv = champsEvenement(p, t)
                                val dEv = cEv.date.trim()
                                val lEv = cEv.lieu.trim()
                                val dpEv = cEv.dept.trim()
                                val numDept = dpEv.toIntOrNull() ?: numeroDepartement(dpEv) ?: numeroDepartement(lEv)
                                // Couleur du bloc selon les informations remplies :
                                // rouge (rien), orange (partiel), vert (complet).
                                val remplisEv = listOf(dEv, cEv.heure.trim(), lEv, dpEv).count { it.isNotEmpty() }
                                val couleurBlocEv = when {
                                    remplisEv >= 4 -> CouleurVert
                                    remplisEv >= 1 -> CouleurOrange
                                    else -> CouleurRouge
                                }
                                // Age de la personne au moment de l'evenement,
                                // entre parentheses dans l'en-tete du bloc.
                                val ageTxt = if (anneeN != null) {
                                    val aEv = anneeDe(dEv)
                                    if (aEv != null) " (" + (aEv - anneeN).coerceAtLeast(0).toString() + " ans)" else ""
                                } else ""
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
                                                    onLongClick = { onOuvrirSaisie(t) },
                                                    onLongClickLabel = "Modifier cet \u00E9v\u00E9nement"
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(10.dp).clip(CircleShape)
                                                    .background(couleurBlocEv)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            val morceaux = mutableListOf<String>()
                                            if (dEv.isNotEmpty()) morceaux.add(dateAffichee(dEv))
                                            if (lEv.isNotEmpty()) morceaux.add(lEv + (if (numDept != null) " (" + numDept.toString() + ")" else ""))
                                            Text(
                                                libelleType(t) + " : " + if (morceaux.isEmpty()) "\u2014" else morceaux.joinToString(" \u00E0 ") + ageTxt,
                                                Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = if (morceaux.isEmpty()) GrisManquant else Color.Unspecified
                                            )
                                            Text(
                                                if (remplisEv >= 4) "complet" else if (remplisEv >= 1) "partiel" else "\u00E0 compl\u00E9ter",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (remplisEv >= 4) CouleurVert else if (remplisEv >= 1) CouleurOrange else CouleurRouge
                                            )
                                        }
                                        // Ouverture animee du bloc.
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
                                                onEditer = { onOuvrirSaisie(t) },
                                                onOuvrir = onOuvrir,
                                                onChoisir = onChoisir,
                                                onRetirer = onRetirer,
                                                onOuvrirMariage = { mariageOuvert = true },
                                                onOuvrirParent = { parentRole = it },
                                                onOuvrirTemoin = { temoinType = t },
                                                onRetirerTemoin = onRetirerTemoin,
                                                onBasculerSignature = onBasculerSignature,
                                                onAjouterTemoinExistant = { type, id -> onNouveauTemoin(type, id, "", "") },
                                                onAjouterActe = onAjouterActe,
                                                onSupprimerActe = onSupprimerActe,
                                                onVoirActe = onVoirActe,
                                                onEnregistrer = enregistrer,
                                                onValider = { v -> onValider(sectionDe(t), v) },
                                                pistes = pistes,
                                                modifier = Modifier
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Un bloc par enfant : fiche resumee de l'enfant,
                                // rangee dans la chronologie a sa naissance.
                                val idE = entree.third
                                val e = personneParId(personnes, idE)
                                Card(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
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
                        }
                        // Evenements de la famille (ascendants, fratrie, enfants).
                        EvenementsFamille(p = p, personnes = personnes)
                        // Bloc SANS TITRE : saisie libre (notes quelconques).
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
                                    placeholder = { Text("Notes libres", style = MaterialTheme.typography.bodySmall) },
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                TextButton(onClick = { p.notesLibres = libre.trim(); enregistrer() }) {
                                    Text("Enregistrer", fontSize = 12.sp)
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
                                        // Sur la FICHE, le nom de la personne concernee
                                        // ne se repete pas.
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
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
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

    // Appui long sur un parent connu : remplacer en choisissant une fiche
    // existante ou en creant une nouvelle.
    if (parentMenu.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { parentMenu = "" },
            title = { Text(if (parentMenu == "pere") "Changer le p\u00E8re" else "Changer la m\u00E8re") },
            text = {
                Column {
                    Text(
                        "Le lien actuel sera remplac\u00E9 ; aucune fiche n'est supprim\u00E9e.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { val role = parentMenu; parentMenu = ""; onChoisir(role) },
                        Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text("Choisir une fiche existante") }
                    OutlinedButton(
                        onClick = { val role = parentMenu; parentMenu = ""; parentRole = role },
                        Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) { Text("Saisir un nouveau parent") }
                }
            },
            confirmButton = {
                TextButton(onClick = { parentMenu = "" }) { Text("Fermer") }
            }
        )
    }
}

// Cadre PERE ET MERE (v0.33) : meme cadre, FIGE en haut de la fiche.
// Affichage identique aux blocs enfants : pastille au numero, meme police,
// lignes N. et D. (deces seulement si la regle des 100 ans l'autorise).
// Toucher = ouvrir sa fiche ; appui long = choisir ou creer un remplacant ;
// croix = retirer le lien (sans supprimer la fiche).
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteParents(
    p: Personne,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit,
    onChoisir: (String) -> Unit,
    onCreer: (String) -> Unit,
    onMenu: (String) -> Unit,
    onRetirer: (String, Long) -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurBordVert)
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            listOf("P\u00E8re" to p.pere, "M\u00E8re" to p.mere).forEach { (libelle, idParent) ->
                val q = personneParId(personnes, idParent)
                if (q == null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            libelle + " : \u2014",
                            Modifier.weight(1f),
                            color = GrisManquant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            onClick = { onChoisir(if (libelle == "P\u00E8re") "pere" else "mere") },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("Choisir", fontSize = 11.sp) }
                        TextButton(
                            onClick = { onCreer(if (libelle == "P\u00E8re") "pere" else "mere") },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("Cr\u00E9er", fontSize = 11.sp) }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .combinedClickable(
                                onClick = { onOuvrir(q.id) },
                                onClickLabel = "Ouvrir la fiche",
                                onLongClick = { onMenu(if (libelle == "P\u00E8re") "pere" else "mere") },
                                onLongClickLabel = "Changer ce parent"
                            )
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PastilleNumero(p = q, diametre = 28)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                q.titre(),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onRetirer(if (libelle == "P\u00E8re") "pere" else "mere", 0L) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Retirer ce parent", modifier = Modifier.size(14.dp))
                            }
                        }
                        // Lignes N. et D. : identiques aux blocs enfants.
                        val lignesParent = mutableListOf(
                            "N" to Triple(q.dateNaissance, q.lieuNaissance, q.deptNaissance)
                        )
                        if (decesAffichable(q, emptyList())) lignesParent.add(
                            "D" to Triple(q.dateDeces, q.lieuDeces, q.deptDeces)
                        )
                        lignesParent.forEach { paire ->
                            val tPar = paire.first
                            val dPar = paire.second.first.trim()
                            val lPar = paire.second.second.trim()
                            val dpPar = paire.second.third.trim()
                            val numPar = dpPar.toIntOrNull() ?: numeroDepartement(dpPar) ?: numeroDepartement(lPar)
                            val morceauxPar = mutableListOf<String>()
                            if (dPar.isNotEmpty()) morceauxPar.add(dateAffichee(dPar))
                            if (lPar.isNotEmpty()) morceauxPar.add(lPar + (if (numPar != null) " (" + numPar.toString() + ")" else ""))
                            if (morceauxPar.isNotEmpty()) {
                                Text(
                                    tPar + ". " + morceauxPar.joinToString(" \u00E0 "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
        return an to (dateAffichee(d) + " " + texte + (if (lt.isNotEmpty()) " \u00E0 " + lt else ""))
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
            "Mariage de " + lienFr + " " + fr.titre() + conjointDeTxt(fr, personnes)
        )?.let { res.add(it) }
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
    onOuvrirTemoin: () -> Unit,
    onRetirerTemoin: (String, Long) -> Unit,
    onBasculerSignature: (String, Long) -> Unit,
    onAjouterTemoinExistant: (String, Long) -> Unit,
    onAjouterActe: (Uri, String, String) -> Unit,
    onSupprimerActe: (Acte) -> Unit,
    onVoirActe: (Acte) -> Unit,
    onEnregistrer: () -> Unit,
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
        // Entete : titre + coche + photo d'acte. Plus de crayon (reponse
        // utilisateur) : l'edition du bloc ouvert se fait par APPUI LONG sur
        // cette entete (ou sur la ligne repliee de la frise).
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onEditer, onLongClickLabel = "Modifier ce bloc"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titre, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Checkbox(
                checked = etatActe == "vert",
                onCheckedChange = { onValider(it) },
                modifier = Modifier.size(32.dp)
            )
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
                        Text("Choisir", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onOuvrirParent("pere") }) {
                        Text("Cr\u00E9er", fontSize = 12.sp)
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
                        Text("Choisir", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onOuvrirParent("mere") }) {
                        Text("Cr\u00E9er", fontSize = 12.sp)
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
                        Text("Choisir", fontSize = 12.sp)
                    }
                    TextButton(onClick = onOuvrirMariage) {
                        Text("Saisir", fontSize = 12.sp)
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
                    Text("Autre conjoint", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onOuvrirMariage, Modifier.weight(1f)) {
                    Text("Saisir un mariage", fontSize = 12.sp)
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
                        Modifier.weight(1f).heightIn(min = 48.dp).clickable { onOuvrir(idT) },
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
        // Etiquette VERTE avec icone ; le menu propose de choisir une fiche
        // existante ou d en creer une (reponse utilisateur).
        var menuTemoin by remember { mutableStateOf(false) }
        var pickerTemoin by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            Button(
                onClick = { menuTemoin = true },
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CouleurVert),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ajouter un t\u00E9moin", color = Color.White, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (menuTemoin) {
            AlertDialog(
                onDismissRequest = { menuTemoin = false },
                title = { Text("Ajouter un t\u00E9moin") },
                text = {
                    Column {
                        Text("Choisir une personne existante, ou cr\u00E9er une nouvelle fiche :", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { menuTemoin = false; pickerTemoin = true }, Modifier.fillMaxWidth()) {
                            Text("Choisir une personne existante")
                        }
                        TextButton(onClick = { menuTemoin = false; onOuvrirTemoin() }, Modifier.fillMaxWidth()) {
                            Text("Cr\u00E9er / saisir une nouvelle fiche")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { menuTemoin = false }) { Text("Annuler") } }
            )
        }
        if (pickerTemoin) {
            AlertDialog(
                onDismissRequest = { pickerTemoin = false },
                title = { Text("Choisir le t\u00E9moin") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        personnes.filter { it.id != p.id }.sortedBy { it.titre() }.forEach { c ->
                            Text(
                                c.titre(),
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                                    pickerTemoin = false
                                    onAjouterTemoinExistant(typeActe, c.id)
                                }.padding(vertical = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pickerTemoin = false }) { Text("Annuler") } }
            )
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
    onVoir: () -> Unit,
    onSupprimer: () -> Unit,
    onEnregistrer: () -> Unit
) {
    var pageOuvert by remember { mutableStateOf(false) }
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
                    Modifier.weight(1f).combinedClickable(onClick = onVoir, onClickLabel = "Voir l'image de l'acte", onLongClick = { pageOuvert = true }, onLongClickLabel = "Voir les pages de l'acte"),
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
            Modifier.weight(1f).combinedClickable(onClick = onVoir, onClickLabel = "Voir l'image de l'acte", onLongClick = { pageOuvert = true }, onLongClickLabel = "Voir les pages de l'acte"),
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



