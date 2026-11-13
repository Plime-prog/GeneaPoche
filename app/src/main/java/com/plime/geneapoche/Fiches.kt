package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    // Chantier 12.0 : coche "Enregistré" par type d'acte (l'acte est
    // conservé ailleurs, plus rien n'est stocké dans l'app).
    onBasculerEnregistre: (String) -> Unit = {},
    // Coche de validation d'une information : (section, valide).
    // Une coche negative cree automatiquement une recherche dans le journal.
    onValider: (String, Boolean) -> Unit,
    // Recherches ouvertes de cette personne (bloc "Recherches" de la fiche)
    // et suppression directe d'une recherche.
    pistes: List<Piste> = emptyList(),
    onSupprimerPiste: (Piste) -> Unit = {},
    // G\u00E9n\u00E9aPoche 11 (11.0) : coche de certification de l'identit\u00E9
    // (\u00E9tat "auto" / "coche" / "croix").
    onValiderIdentite: (String) -> Unit = {},
    // G\u00E9n\u00E9aPoche 3 : \u00E9dition directe de l'identit\u00E9 (nom, pr\u00E9noms,
    // profession) depuis la fiche, et gestion des \u00E9v\u00E9nements libres
    // (intitul\u00E9 libre) de la chronologie.
    onEditerIdentite: () -> Unit = {},
    onModifierEvenementLibre: (EvenementLibre) -> Unit = {},
    onSupprimerEvenementLibre: (Long) -> Unit = {},
    // v0.92 : carnet de sources.
    sources: List<Source> = emptyList(),
    citations: List<Citation> = emptyList(),
    onEnregistrerCitation: (Citation) -> Unit = {},
    onSupprimerCitation: (Long) -> Unit = {},
    onCreerSource: (Source) -> Long = { 0L }
) {
    // GÃ©nÃ©aPoche 5 : tous les cadres REPLIÃS sur une ligne Ã  l'ouverture
    // de la fiche ; le toucher d'un cadre le dÃ©veloppe, l'appui long le modifie.
    var ouverts by remember { mutableStateOf(setOf<String>()) }
    // Dialogues : mariage, enfant, parent, temoin.
    var mariageOuvert by remember { mutableStateOf(false) }
    var parentRole by remember { mutableStateOf("") }    // G\u00E9n\u00E9aPoche 2 : formulaire de saisie d'acte ouvert en plein \u00E9cran
    // depuis la fiche. null = ferm\u00E9 ; "" = nouvel \u00E9v\u00E9nement ; "N"/"B"/"M"/"S"/"D"
    // = bloc de la frise \u00E9dit\u00E9 par appui long (formulaire pr\u00E9-rempli).
    var saisieOuverte by remember { mutableStateOf<String?>(null) }
    // v0.58 : chaque ouverture du formulaire crée une instance NEUVE
    // (compteur dans la clé key) : plus aucun état fantôme d'une saisie
    // précédente (belle-mère ou autre).
    var saisieCompteur by remember { mutableStateOf(0) }
    fun ouvrirSaisie(t: String?) { saisieCompteur++; saisieOuverte = t }
    // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nement libre en cours d'\u00E9dition (son id).
    var evLibreEdite by remember { mutableStateOf<String?>(null) }
    // G\u00E9n\u00E9aPoche 7 : ajout d'un enfant depuis la fiche (choisir une
    // fiche existante ou saisir un nouvel enfant).
    var menuEnfant by remember { mutableStateOf(false) }
    var enfantSaisie by remember { mutableStateOf(false) }

    // v0.84 : regles d'affichage des blocs d'evenement de la fiche :
    // - vivant de moins de 30 ans aujourd'hui : N seulement ;
    // - personne decedee avant 25 ans : pas de M ;
    // - tous les autres : N, M et D systematiques. Un bloc NMD
    //   n'apparait que s'il porte une information (date, heure, commune,
    //   departement) ou la coche "Enregistr\u00E9" ; B et S apparaissent
    //   des qu'une information existe.
    val actePresent = { t: String -> p.enregistreDe(t) }
    fun blocRenseigne(t: String): Boolean {
        val c = champsEvenement(p, t)
        return c.date.isNotBlank() || c.heure.isNotBlank() || c.lieu.isNotBlank() ||
            c.dept.isNotBlank() || actePresent(t)
    }
    val jeuneVivant = vivantMoinsDe30(p)
    val jeuneDecede = decedeAvant25(p)
    val typesPresents = mutableListOf("N")
    if (blocRenseigne("B")) typesPresents.add("B")
    if (blocRenseigne("M") && !jeuneVivant && !jeuneDecede) typesPresents.add("M")
    if (blocRenseigne("D")) typesPresents.add("D")
    if (blocRenseigne("S")) typesPresents.add("S")
    // v0.84 : les evenements sont ranges dans l'ordre CHRONOLOGIQUE
    // (dates completes converties en cle comparable) ; si l'un des
    // evenements N, M ou D est sans date, retour a l'ordre naturel NBMDS.
    val ordreNaturel = listOf("N", "B", "M", "D", "S")
    val ordreTypes = ordreNaturel.filter { typesPresents.contains(it) }
    val nmdComplets = listOf("N", "M", "D").all { champsEvenement(p, it).date.isNotBlank() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // G\u00E9n\u00E9aPoche 3 : pastille + titre + compteur des
                    // renseignements essentiels (date + commune de N, M, D
                    // au maximum, selon l'\u00E2ge de la personne), rouge
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
                        // v0.84 : indicateur identique fiche/liste - sur 6
                        // (N, M, D : date + commune) pour tous, sur 4 (N, D)
                        // pour un d\u00E9c\u00E9d\u00E9 avant 25 ans, sur 2 (N) pour un
                        // vivant de moins de 30 ans.
                        val (nInd, totalInd) = indicateurPersonne(p)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = couleurCompteur(nInd, totalInd)
                        ) {
                            Text(
                                "  " + nInd.toString() + "/" + totalInd.toString() + "  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // G\u00E9n\u00E9aPoche 11 (11.0) : coche de certification
                        // de l'identit\u00E9, \u00E0 droite du n/6. Toucher = choix
                        // manuel (auto -> coche -> croix -> auto).
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                onValiderIdentite(
                                    when (p.identiteVerifiee) {
                                        "auto" -> "coche"
                                        "coche" -> "croix"
                                        else -> "auto"
                                    }
                                )
                            },
                            Modifier.size(36.dp)
                        ) {
                            if (identiteCertifiee(p)) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Identit\u00E9 certifi\u00E9e",
                                    Modifier.size(24.dp),
                                    tint = Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Identit\u00E9 non certifi\u00E9e",
                                    Modifier.size(24.dp),
                                    tint = CouleurRouge
                                )
                            }
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
                    // G\u00E9n\u00E9aPoche 9 : bouton Arbre SUPPRIM\u00E9 : l'arbre
                    // (et la ligne de vie) se synchronisent automatiquement
                    // avec la personne de la fiche ouverte.
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
            // v0.69 : BANDEAU ROUGE si une date de la fiche est hors format
            // (ancien GEDCOM ou champ libre) : la liste precise ce qu'il
            // faut corriger et ou.
            item {
                val datesKO = datesInvalides(p)
                if (datesKO.isNotEmpty()) {
                    Surface(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = CouleurRouge
                    ) {
                        Text(
                            "Date \u00E0 corriger : " + datesKO.joinToString(", "),
                            Modifier.padding(6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // G\u00E9n\u00E9aPoche 3 : chronologie UNIQUE. Tous les \u00E9v\u00E9nements
            // (N/B/M/S/D et \u00E9v\u00E9nements libres) ET les enfants sont rang\u00E9s
            // par date. Chaque \u00E9v\u00E9nement affiche l'\u00E2ge entre parenth\u00E8ses.
            item {
                Column(Modifier.fillMaxWidth()) {
                    val evsFamille = evenementsFamille(p, personnes)
                    val entrees = mutableListOf<Pair<Long, String>>()
                    ordreTypes.forEach { t ->
                        entrees.add(cleDate(champsEvenement(p, t).date) to ("ev:" + t))
                    }
                    p.evenementsLibres.forEach { ev ->
                        entrees.add((anneeDe(ev.date)?.toLong() ?: -1L) * 10000L to ("evl:" + ev.id.toString()))
                    }
                    p.enfants.forEach { idE ->
                        val eE = personneParId(personnes, idE)
                        entrees.add(((eE?.let { anneeDe(it.dateNaissance) } ?: -1).toLong() * 10000L) to ("enf:" + idE.toString()))
                    }
                    personnes.filter { it.temoinsB.contains(p.id) }.forEach { b ->
                        entrees.add(
                            ((anneeDe(b.dateBapteme) ?: anneeDe(b.dateNaissance)) ?: -1).toLong() * 10000L to ("par:" + b.id.toString())
                        )
                    }
                    evsFamille.forEachIndexed { iFam, ef ->
                        entrees.add(ef.first.toLong() * 10000L to ("fam:" + iFam.toString()))
                    }
                    // v0.84 : clefs de tri en Long (aaaammjj) ; les
                    // evenements de famille/temoins, hors de la vie de la
                    // personne, restent masques comme avant.
                    val anneeN = (anneeDe(p.dateNaissance) ?: -1).toLong() * 10000L
                    val anneeD = (anneeDe(p.dateDeces) ?: -1).toLong() * 10000L
                    val filtreParasites = { e: Pair<Long, String> ->
                        if (!e.second.startsWith("fam:") && !e.second.startsWith("enf:")) true
                        else (anneeN < 0L || e.first < 0L || e.first >= anneeN) &&
                            (anneeD < 0L || e.first < 0L || e.first <= anneeD)
                    }
                    // v0.84 : si N, M et D sont dates, TOUT est range dans
                    // l'ordre chronologique (tri stable) ; sinon les blocs
                    // gardent l'ordre naturel NBMDS et le reste suit par date.
                    val ordonnees = if (nmdComplets) {
                        entrees.filter(filtreParasites).sortedBy { it.first }
                    } else {
                        val blocsActe = entrees.filter { it.second.startsWith("ev:") }
                            .sortedBy { ordreNaturel.indexOf(it.second.removePrefix("ev:")) }
                        val autres = entrees.filter { !it.second.startsWith("ev:") }
                            .filter(filtreParasites).sortedBy { it.first }
                        blocsActe + autres
                    }
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
                                            val lTxtEvl = if (ev.lieu.trim().isNotEmpty()) " \u00E0 " + lieuAffiche(ev.lieu.trim()) else ""
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
                                                val lTxtEnf = if (lEnf.isNotEmpty()) lieuAffiche(lEnf) else ""
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
                            cle.startsWith("par:") -> {
                                // v0.76 : bapt\u00eame o\u00f9 p est parrain ou marraine.
                                val idB = cle.removePrefix("par:").toLongOrNull() ?: 0L
                                val b = personneParId(personnes, idB)
                                if (b != null) {
                                    Card(
                                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, CouleurOrange)
                                    ) {
                                        // v0.77 : m\u00EAme gabarit que les autres \u00E9v\u00E9nements :
                                        // date en t\u00EAte, \u00E2ge entre parenth\u00E8ses.
                                        val dTxtPar = dateLigne(b.dateBapteme.trim()).ifEmpty { "  /  /    " }
                                        val aTxtPar = ageAffiche(p.dateNaissance, b.dateBapteme)
                                        Text(
                                            dTxtPar +
                                                (if (aTxtPar.isNotEmpty()) " " + aTxtPar else "") +
                                                " B \u2013 Bapt\u00EAme de " + b.titre() +
                                                (if (b.temoinsB.indexOf(p.id) == 0) " (parrain)" else " (marraine)"),
                                            Modifier.fillMaxWidth().padding(6.dp).clickable { onOuvrir(b.id) },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CouleurVertFonce
                                        )
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
                                                    onLongClick = { ouvrirSaisie(t) },
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
                                        val lTxt = if (lEv.isNotEmpty()) lieuAffiche(lEv) else ""
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
                                            onEditer = { saisieOuverte = t },
                                            onOuvrir = onOuvrir,
                                            onChoisir = onChoisir,
                                            onRetirer = onRetirer,
                                            onOuvrirMariage = { mariageOuvert = true },
                                            onOuvrirParent = { parentRole = it },
                                            onBasculerEnregistre = onBasculerEnregistre,
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
                    // supprimables directement. G\u00E9n\u00E9aPoche 9 : le texte est
                    // recalcul\u00E9 comme dans le journal (m\u00EAme synchronisation),
                    // chaque recherche est CLIQUABLE (elle ouvre la saisie ou
                    // le picker correspondant) et la note de recherche est
                    // la M\u00CAME que celle du journal.
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
                                    val texteFiche = textePisteActuel(ps, personnes)
                                        .replace(titreCourt(p), "")
                                        .replace(Regex("\\s+de\\s*$"), "")
                                        .replace(Regex("\\s{2,}"), " ")
                                        .trim()
                                    // G\u00E9n\u00E9aPoche 9 : toucher une recherche ouvre
                                    // directement la saisie correspondante.
                                    val actionPiste: (() -> Unit)? = when {
                                        ps.categorie.startsWith("naissance") -> { { ouvrirSaisie("N") } }
                                        ps.categorie.startsWith("bapteme") -> { { ouvrirSaisie("B") } }
                                        ps.categorie.startsWith("mariage") -> { { ouvrirSaisie("M") } }
                                        ps.categorie.startsWith("deces") -> { { ouvrirSaisie("D") } }
                                        ps.categorie.startsWith("sepulture") -> { { ouvrirSaisie("S") } }
                                        ps.categorie.startsWith("pere") -> { { onChoisir("pere") } }
                                        ps.categorie.startsWith("mere") -> { { onChoisir("mere") } }
                                        ps.categorie.startsWith("conjoint") -> { { ouvrirSaisie("M") } }
                                        ps.categorie.startsWith("enfants") -> { { menuEnfant = true } }
                                        ps.categorie.startsWith("sexe") -> { { onModifier() } }
                                        else -> null
                                    }
                                    Column(Modifier.fillMaxWidth()) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                texteFiche.ifEmpty { ps.texte },
                                                Modifier.weight(1f).then(
                                                    if (actionPiste != null) Modifier.clickable { actionPiste() } else Modifier
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (actionPiste != null) CouleurVertFonce else Color.Unspecified,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(onClick = { onSupprimerPiste(ps) }, Modifier.size(28.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = "Supprimer cette recherche", Modifier.size(14.dp))
                                            }
                                        }
                                        // G\u00E9n\u00E9aPoche 9 : la note saisie ici est la
                                        // m\u00EAme que celle du journal (piste.enCours).
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            var notePiste by remember(ps.id) { mutableStateOf(ps.enCours) }
                                            // v0.49 : etat local, la liaison directe ne rafraichissait pas le cadre.
                                            OutlinedTextField(
                                                value = notePiste,
                                                onValueChange = { notePiste = it },
                                                modifier = Modifier.weight(1f),
                                                placeholder = { Text("Recherche : pages, cotes, notes...", style = MaterialTheme.typography.bodySmall) },
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                            TextButton(onClick = { ps.enCours = notePiste; enregistrer() }) {
                                                Text("Enregistrer", fontSize = 12.sp)
                                            }
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
                                .clickable { ouvrirSaisie("") },
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
                    // v0.92 : bloc SOURCES (citations de cette personne).
                    BlocSourcesFiche(
                        p = p, personnes = personnes, sources = sources, citations = citations,
                        onEnregistrerCitation = onEnregistrerCitation,
                        onSupprimerCitation = onSupprimerCitation,
                        onCreerSource = onCreerSource
                    )
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
                key(p.id, tPre, saisieCompteur) {
                EcranSaisieActe(
                    personnes = personnes,
                    initial = remember(saisieCompteur, tPre, p.id) { prefillActe(p, personnes, tPre.ifEmpty { null }, editionFiche = tPre.isNotEmpty(), citations = citations) },
                    // GeneaPoche 4 : appui long sur un bloc = SEUL ce type d'acte.
                    typeImpose = tPre.ifEmpty { null },
                    sources = sources,
                    citations = citations,
                    onCreerSource = onCreerSource,
                    onEnregistrer = { a ->                        onEnregistrerActe(a)
                        saisieOuverte = null
                    },
                    onFermer = { saisieOuverte = null }
                )
                }
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
            // v0.98 : situation et domicile affiches dans la fiche
            // (transversal, exportes aussi en NOTE Heredis).
            if (p.situation.trim().isNotEmpty()) {
                Text("Situation : " + p.situation.trim(), style = MaterialTheme.typography.bodySmall)
            }
            if (p.domicile.trim().isNotEmpty()) {
                Text("Domicile : " + p.domicile.trim(), style = MaterialTheme.typography.bodySmall)
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
    var intitule by remember(initial) { mutableStateOf(initial.intitule) }
    var date by remember(initial) { mutableStateOf(initial.date) }
    var heure by remember(initial) { mutableStateOf(initial.heure) }
    var lieu by remember(initial) { mutableStateOf(initial.lieu) }
    var dept by remember(initial) { mutableStateOf(initial.dept) }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("\u00C9v\u00E9nement") },
        text = {
            Column {
                OutlinedTextField(value = intitule, onValueChange = { intitule = it }, label = { Text("Intitul\u00E9") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (jj/mm/aaaa)") }, isError = !dateValide(date), supportingText = { if (!dateValide(date)) Text("jj/mm/aaaa ou aaaa", style = MaterialTheme.typography.bodySmall) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = heure, onValueChange = { heure = it }, label = { Text("Heure") }, modifier = Modifier.fillMaxWidth())
                Champ("Commune", lieu, { lieu = it }, communes = true, onDept = { dept = it })
            }
        },
        confirmButton = {
            // v0.91 : plus jamais de blocage par la date.
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

fun prefillActe(p: Personne, personnes: List<Personne>, t: String?, editionFiche: Boolean = false,
                citations: List<Citation> = emptyList()): ActeSaisie {
    val type = t ?: "N"
    val c = if (t == null || type == "E") null else champsEvenement(p, type)
    val pere = personneParId(personnes, p.pere)
    val mere = personneParId(personnes, p.mere)
    // GÃ©nÃ©aPoche 10 : le conjoint (et ses parents) sont proposÃ©s pour
    // TOUS les types d'acte, pas seulement le mariage.
    val conj = personneParId(personnes, p.conjoint)
    val pConj = if (conj != null) personneParId(personnes, conj.pere) else null
    val mConj = if (conj != null) personneParId(personnes, conj.mere) else null
    val temoinsIds = p.temoinsDe(type)
    val signataires = p.signatairesDe(type).mapNotNull { id ->
        when {
            id == 0L -> "P\u00E8re"
            id == p.id -> "Concerne(e)"
            mere != null && id == mere.id -> "M\u00E8re"
            conj != null && id == conj.id -> "Mari\u00E9e"
            pConj != null && id == pConj.id -> "P\u00E8re de la mari\u00E9e"
            mConj != null && id == mConj.id -> "M\u00E8re de la mari\u00E9e"
            else -> {
                // v0.73 : sur un bapt\u00EAme, les coches de signature sont
                // PARRAIN et MARRAINE (pas "T\u00E9moin 1/2").
                val i = temoinsIds.indexOf(id)
                when {
                    i < 0 -> null
                    type == "B" && i == 0 -> "Parrain"
                    type == "B" && i == 1 -> "Marraine"
                    else -> "T\u00E9moin " + (i + 1).toString()
                }
            }
        }
    }
    // v0.80 : conjoint du d\u00E9funt EN VIE ou D\u00C9C\u00C9D\u00C9 AVANT (veuf/veuve),
    // relu depuis la transcription de l'acte puis l'\u00E9tat de la fiche ;
    // enfant D\u00C9CLARANT d'un d\u00E9c\u00E8s (coche de signature persist\u00E9e).
    val conjointEnVie = if (type == "D" || type == "S") p.conjointEnVieDeces
        else conj == null || conj.dateDeces.isBlank()
    val declarantEnfantId = if (type == "D") p.signatairesDe(type).firstOrNull { id ->
        id != 0L && id != p.id &&
            (p.enfants.contains(id) || personnes.firstOrNull { it.id == id }?.let { q -> q.pere == p.id || q.mere == p.id } == true)
    } ?: 0L else 0L
    val signatairesListe = signataires.toMutableList()
    if (declarantEnfantId != 0L) signatairesListe.add("Enfant d\u00E9clarant")
    // v0.98 : la citation de l'acte (source, num\u00E9ro, vue, fiabilit\u00E9)
    // est RELUE pour pr\u00E9-remplir le formulaire : la source \u00E9tait
    // perdue \u00E0 chaque r\u00E9ouverture (bug v0.98).
    val cit = if (t == null || type == "E") null
        else citations.firstOrNull { it.personneId == p.id && it.cible == type }
    // v0.98 : \u00E2ge \u00E0 l'acte RECALCUL\u00C9 depuis la fiche du parent
    // (ann\u00E9e de l'acte - ann\u00E9e de naissance, ou naissance calcul\u00E9e
    // "vers aaaa") au lieu d'\u00EAtre remis \u00E0 vide (bug v0.98).
    val anneeActe = anneeDe(c?.date ?: "")
    fun ageALActe(q: Personne?): String {
        if (q == null || anneeActe == null) return ""
        val an = anneeDe(q.dateNaissance)
            ?: Regex("[0-9]{4}").find(q.naissanceCalculee)?.value?.toIntOrNull()
            ?: return ""
        val age = anneeActe - an
        return if (age in 0..120) age.toString() else ""
    }
    // v0.98 : le d\u00E9clarant d'un d\u00E9c\u00E8s est pr\u00E9-rempli depuis la
    // fiche de l'enfant d\u00E9clarant connu.
    val declarant = if (type == "D" && declarantEnfantId != 0L) personneParId(personnes, declarantEnfantId) else null
    // v0.99 : conjoint de la mention marginale de mariage (pre-remplissage
    // nominatif) : la fiche mariee ce jour-la, sinon le conjoint lie.
    val conjMarg = if (p.mentionMDate.isNotBlank())
        personnes.firstOrNull { it.dateMariage == p.mentionMDate && (it.conjoint == p.id || it.conjointsSupp.contains(p.id)) }
            ?: personneParId(personnes, p.conjoint)
    else null
    return ActeSaisie(
        acteId = 0L,
        editionFiche = editionFiche,
        type = type,
        commune = c?.lieu?.trim() ?: "",
        dept = c?.dept?.trim() ?: "",
        enregistre = p.enregistreDe(type),
        date = c?.date?.trim() ?: "",
        heure = c?.heure?.trim() ?: "",
        personneId = p.id,
        pereId = pere?.id ?: 0L,
        mereId = mere?.id ?: 0L,
        pereNomInit = pere?.nom ?: "",
        mereNomInit = mere?.nom ?: "",
        nom = p.nom, prenoms = p.prenoms, sexe = p.sexe, profession = p.profession,
        pereNom = pere?.nom ?: "", perePrenoms = pere?.prenoms ?: "",
        pereNe = pere?.dateNaissance ?: "", pereLieu = pere?.lieuNaissance ?: "",
        pereDept = pere?.deptNaissance ?: "", pereProf = pere?.profession ?: "",
        pereDeces = pere?.dateDeces ?: "", pereLieuDeces = pere?.lieuDeces ?: "",
        pereAge = ageALActe(pere), pereSituation = pere?.situation ?: "", pereDomicile = pere?.domicile ?: "",
        mereNom = mere?.nom ?: "", merePrenoms = mere?.prenoms ?: "",
        mereNe = mere?.dateNaissance ?: "", mereLieu = mere?.lieuNaissance ?: "",
        mereDept = mere?.deptNaissance ?: "", mereProf = mere?.profession ?: "",
        mereDeces = mere?.dateDeces ?: "", mereLieuDeces = mere?.lieuDeces ?: "",
        mereAge = ageALActe(mere), mereSituation = mere?.situation ?: "", mereDomicile = mere?.domicile ?: "",
        mentionMDate = p.mentionMDate, mentionMLieu = p.mentionMLieu,
        mentionMNom = conjMarg?.nom ?: "", mentionMPrenoms = conjMarg?.prenoms ?: "",
        mentionDDate = p.mentionDDate, mentionDLieu = p.mentionDLieu,
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
            personneParId(personnes, id)?.let { q -> TemoinSaisie(q.nom, q.prenoms, q.profession, situation = q.situation, domicile = q.domicile) }
        },
        signataires = signatairesListe,
        conjointEnVie = conjointEnVie,
        declarantEnfantId = declarantEnfantId,
        // v0.98 : source relue depuis la citation + nouveaux champs B/M/D/S
        // PRE-REMPLIS depuis la fiche (une edition ne doit jamais effacer).
        sourceId = cit?.sourceId ?: 0L,
        numeroActe = cit?.numeroActe ?: "",
        vue = cit?.vue ?: 0,
        vueTotal = cit?.vueTotal ?: 0,
        fiabiliteSource = cit?.fiabilite ?: -1,
        declNDate = p.declNDate, declNHeure = p.declNHeure,
        bLegitime = p.bLegitime,
        contratMariage = p.contratMariage, dispenses = p.dispenses,
        declDDate = p.declDDate, declDHeure = p.declDHeure,
        declarantNom = p.declarantNom.ifBlank { declarant?.nom ?: "" },
        declarantProfession = p.declarantProfession.ifBlank { declarant?.profession ?: "" },
        declarantDomicile = p.declarantDomicile.ifBlank { declarant?.domicile ?: "" },
        ceremonieSDate = p.ceremonieSDate,
        cimetiere = p.cimetiere, concession = p.concession, ministreCulte = p.ministreCulte
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

// v0.84 : cle de tri chronologique : date COMPLETE convertie en nombre
// comparable (aaaammjj) ; une annee seule vaut aaaammjj = aaaa0000.
fun cleDate(date: String): Long {
    val t = date.trim()
    val g = repubAGregorien(t) ?: t
    val m = Regex("^(\\d{2})/(\\d{2})/(\\d{4})$").find(g)
    if (m != null) {
        return m.groupValues[3].toLong() * 10000L +
            m.groupValues[2].toLong() * 100L + m.groupValues[1].toLong()
    }
    return (anneeDe(t)?.toLong() ?: -1L) * 10000L
}

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
    // v0.91 : le d\u00E9partement n'est plus jamais affich\u00E9 ; le lieu
    // appara\u00EEt sans son code INSEE ("Roubaix, 59512, Nord" ->
    // "Roubaix, Nord").
    fun lieuAvecDept(lieu: String, dept: String): String = lieuAffiche(lieu)
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
// Ligne d'une personne liee (pere, mere, conjoint) : nom, profession, naissance,
// age a l'acte, deces. Grisee si absente.
@Composable
fun LignePersonneG(
    libelle: String,
    q: Personne?,
    anneeActe: Int?,
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
            }
            // v0.97 : pas de date de naissance connue mais une date CALCULEE
            // d'apres un age a l'acte (ex "vers 1879").
            if (an == null && q.naissanceCalculee.isNotBlank()) {
                m.add("n\u00E9(e) " + q.naissanceCalculee + " (calcul\u00e9)")
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
    onEditer: () -> Unit,
    onOuvrir: (Long) -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit,
    onOuvrirMariage: () -> Unit,
    onOuvrirParent: (String) -> Unit,
    // Chantier 12.0 : coche "Enregistré" dans l'en-tête du bloc.
    onBasculerEnregistre: (String) -> Unit = {},
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
    // Le bloc est maintenant affiche DANS la carte de l'evenement (v0.28) :
    // simple colonne, plus de carte imbriquee.
    Column(modifier.fillMaxWidth().padding(top = 4.dp)) {
        // Entete : titre + coche "Enregistré" (chantier 12.0 : l'acte n'est
        // plus stocké dans l'app, la coche marque un acte conservé ailleurs).
        // Plus de crayon (reponse utilisateur) : l'edition du bloc ouvert se
        // fait par APPUI LONG sur cette entete (ou sur la ligne repliee).
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onEditer, onLongClickLabel = "Modifier ce bloc"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titre, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Checkbox(
                checked = p.enregistreDe(typeActe),
                onCheckedChange = { onBasculerEnregistre(typeActe) }
            )
            Text("Enregistr\u00E9", fontSize = 12.sp)
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
                    lieuAffiche(l),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // (Chantier 12.0 : plus aucune cote AD stockee - l'acte est conserve ailleurs.)

        // v0.98 : nouveaux champs B/M/D/S affiches dans le bloc.
        when (typeActe) {
            "N" -> {
                if (p.declNDate.isNotBlank() || p.declNHeure.isNotBlank()) {
                    LigneG("D\u00E9claration", (if (p.declNDate.isNotBlank()) dateAffichee(p.declNDate) else "") +
                        (if (p.declNHeure.isNotBlank()) (if (p.declNDate.isNotBlank()) " \u00e0 " else "") + heureAffichee(p.declNHeure) else ""))
                }
            }
            "B" -> {
                if (p.bLegitime.isNotBlank()) LigneG("L\u00E9gitimit\u00E9", p.bLegitime)
            }
            "M" -> {
                if (p.contratMariage.isNotBlank()) LigneG("Contrat de mariage", p.contratMariage)
                if (p.dispenses.isNotBlank()) LigneG("Dispenses", p.dispenses)
            }
            "D" -> {
                if (p.declDDate.isNotBlank() || p.declDHeure.isNotBlank()) {
                    LigneG("D\u00E9claration", (if (p.declDDate.isNotBlank()) dateAffichee(p.declDDate) else "") +
                        (if (p.declDHeure.isNotBlank()) (if (p.declDDate.isNotBlank()) " \u00e0 " else "") + heureAffichee(p.declDHeure) else ""))
                }
                if (p.declarantNom.isNotBlank()) {
                    val det = listOf(p.declarantProfession.trim(), p.declarantDomicile.trim()).filter { it.isNotEmpty() }
                    LigneG("D\u00E9clarant", p.declarantNom.trim() + (if (det.isNotEmpty()) ", " + det.joinToString(", ") else ""))
                }
            }
            "S" -> {
                if (p.ceremonieSDate.isNotBlank()) LigneG("C\u00E9r\u00E9monie", dateAffichee(p.ceremonieSDate))
                if (p.cimetiere.isNotBlank()) LigneG("Cimeti\u00E8re", p.cimetiere)
                if (p.concession.isNotBlank()) LigneG("Concession", p.concession)
                if (p.ministreCulte.isNotBlank()) LigneG("Ministre du culte", p.ministreCulte)
            }
        }

        // Naissance : pere (avec coche de signature) et mere, choisies parmi
        // les fiches existantes ou creees ; la synchronisation a lieu a la
        // validation dans MainActivity.
        if (typeActe == "N") {
            // v0.97 : date de naissance CALCULEE d'apres un age lu sur un
            // acte (affichee seulement si pas de date de naissance connue).
            if (p.naissanceCalculee.isNotBlank()) {
                LigneG("N. calcul\u00e9", p.naissanceCalculee + " (" + p.naissanceCalculeeSource + ")")
            }
            LignePersonneG(
                libelle = "P\u00E8re",
                q = personneParId(personnes, p.pere),
                anneeActe = anneeActe,
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
                } else {
                    TextButton(onClick = { onRetirer("pere", p.pere) }) {
                        Text("Retirer ce lien", fontSize = 12.sp)
                    }
                }
            }
            LignePersonneG(
                libelle = "M\u00E8re",
                q = personneParId(personnes, p.mere),
                anneeActe = anneeActe,
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
                } else {
                    TextButton(onClick = { onRetirer("mere", p.mere) }) {
                        Text("Retirer ce lien", fontSize = 12.sp)
                    }
                }
            }
            // v0.97 : mentions marginales portees par l'acte de naissance.
            // v0.99 : la mention de mariage est NOMINATIVE : le nom de la
            // fiche du conjoint (existante ou creee) est rappele.
            if (p.mentionMDate.isNotBlank() || p.mentionMLieu.isNotBlank()) {
                val conjMargFiche = personnes.firstOrNull { it.dateMariage == p.mentionMDate && (it.conjoint == p.id || it.conjointsSupp.contains(p.id)) }
                    ?: personneParId(personnes, p.conjoint)
                LigneG("Mention marg. mariage", (if (p.mentionMDate.isNotBlank()) dateAffichee(p.mentionMDate) else "") + (if (p.mentionMLieu.isNotBlank()) (if (p.mentionMDate.isNotBlank()) " \u00e0 " else "") + lieuAffiche(p.mentionMLieu) else "") + (if (conjMargFiche != null) " \u2014 avec " + conjMargFiche.titre() else ""))
            }
            if (p.mentionDDate.isNotBlank() || p.mentionDLieu.isNotBlank()) {
                LigneG("Mention marg. d\u00e9c\u00e8s", (if (p.mentionDDate.isNotBlank()) dateAffichee(p.mentionDDate) else "") + (if (p.mentionDLieu.isNotBlank()) (if (p.mentionDDate.isNotBlank()) " \u00e0 " else "") + lieuAffiche(p.mentionDLieu) else ""))
            }
        }

        // v0.97 : l'acte apparait AUSSI dans la fiche des temoins (comme
        // le parrain et la marraine pour le bapteme).
        if (typeActe == "N" || typeActe == "B") {
            personnes.filter { it.temoinsDe(typeActe).contains(p.id) }.forEach { q ->
                LigneG(
                    if (typeActe == "B") "Parrain ou marraine" else "T\u00e9moin (naissance)",
                    q.titre() + (if (q.dateNaissance.isNotBlank()) ", le " + dateAffichee(q.dateNaissance) else "")
                )
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














