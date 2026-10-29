package com.plime.geneapoche

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Couleurs : blanc, verts pale et fonce, rouge doux pour "a verifier".
val CouleurVert = Color(0xFF2E7D32)
val CouleurVertFonce = Color(0xFF1F4F26)
val CouleurRouge = Color(0xFFB45046)
val CouleurVertPale = Color(0xFFE7F0E5)
val CouleurNeutrePale = Color(0xFFEFF4EE)

val PaletteGeneaPoche = lightColorScheme(
    primary = Color(0xFF2E5D3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8EBD4),
    onPrimaryContainer = Color(0xFF10240F),
    secondary = Color(0xFF4E6B52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1EDE0),
    onSecondaryContainer = Color(0xFF152418),
    tertiary = Color(0xFF3A6B4A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE8F0E7),
    onSurfaceVariant = Color(0xFF44503F),
    outline = Color(0xFF798577),
    // Conteneurs thirdaires et d'erreur : verts/rouge doux, AUCUN violet Material3 par defaut.
    outlineVariant = Color(0xFFC9D2C2),
    tertiaryContainer = Color(0xFFD8EBD4),
    onTertiaryContainer = Color(0xFF10240F),
    error = Color(0xFFB45046),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF6DDD8),
    onErrorContainer = Color(0xFF3A1110),
    surfaceTint = Color(0xFF2E5D3A),
    // Surfaces "container" : blancs verdatres (les defauts Material3 sont VIOLETS :
    // NavigationBar du bas, dialogues, cartes). Tout est remplace ici.
    surfaceDim = Color(0xFFDCE2D5),
    surfaceBright = Color(0xFFFDFEF8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFCF7),
    surfaceContainer = Color(0xFFF4F8F1),
    surfaceContainerHigh = Color(0xFFF7FAF4),
    surfaceContainerHighest = Color(0xFFEFF3E9)
)

val FormesGeneaPoche = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val formatJour = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
private val formatJourHeure = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

fun formaterDate(ts: Long): String =
    if (ts > 0L) formatJour.format(Date(ts)) else ""

fun formaterDateHeure(ts: Long): String =
    if (ts > 0L) formatJourHeure.format(Date(ts)) else ""

// ---------------------------------------------------------------- Aides numerotation

fun personneParId(liste: List<Personne>, id: Long): Personne? =
    liste.firstOrNull { it.id == id }

fun sosaParent(p: Personne, mere: Boolean): String {
    val s = p.sosa.trim().toLongOrNull() ?: return ""
    return (if (mere) 2L * s + 1L else 2L * s).toString()
}

fun prefixeLateral(p: Personne): String {
    val l = p.lateral.trim()
    if (l.isNotEmpty()) return l
    return p.sosa.trim()
}

fun sosaValide(s: String): Boolean {
    val t = s.trim()
    if (t.isEmpty()) return true
    val n = t.toLongOrNull() ?: return false
    return n >= 1L
}

fun lateralValide(s: String): Boolean {
    val t = s.trim()
    if (t.isEmpty()) return true
    return Regex("^[0-9]+(\\.[0-9]+)*$").matches(t)
}

// Annee de naissance pour le tri (9999 si inconnue).
fun anneeNaissance(p: Personne): Int =
    Regex("[0-9]{4}").find(p.dateNaissance)?.value?.toIntOrNull() ?: 9999

// Renum\u00E9rotation complete depuis un de cujus :
// Sosa (pere = 2n, mere = 2n+1) remontee en largeur, puis d'Aboville en descendant
// (enfant k du porteur "x" = "x.k"). Les personnes non atteignables perdent leur num\u00E9ro.
// Numerotation complete a partir du de cujus, dont le n Sosa n'est pas forcement 1 :
// pere = 2n, mere = 2n+1, descendants en d'Aboville (pref = n du cujus).
fun renumeroter(personnes: MutableList<Personne>, cujusId: Long, sosaDeBase: Long = 1L) {
    personnes.forEach { it.sosa = ""; it.lateral = "" }
    val cujus = personneParId(personnes, cujusId) ?: return
    cujus.sosa = sosaDeBase.toString()
    val attente = ArrayDeque<Long>()
    attente.addLast(cujus.id)
    while (attente.isNotEmpty()) {
        val id = attente.removeFirst()
        val p = personneParId(personnes, id) ?: continue
        val s = p.sosa.trim().toLongOrNull() ?: continue
        val pp = personneParId(personnes, p.pere)
        if (pp != null && pp.sosa.isEmpty()) {
            pp.sosa = (2L * s).toString()
            attente.addLast(pp.id)
        }
        val pm = personneParId(personnes, p.mere)
        if (pm != null && pm.sosa.isEmpty()) {
            pm.sosa = (2L * s + 1L).toString()
            attente.addLast(pm.id)
        }
    }
    val descendants = ArrayDeque<Pair<Long, String>>()
    descendants.addLast(cujus.id to sosaDeBase.toString())
    while (descendants.isNotEmpty()) {
        val (id, pref) = descendants.removeFirst()
        val p = personneParId(personnes, id) ?: continue
        var k = 0
        p.enfants.forEach { idE ->
            val e = personneParId(personnes, idE) ?: return@forEach
            if (e.sosa.isEmpty() && e.lateral.isEmpty()) {
                k += 1
                e.lateral = pref + "." + k.toString()
                descendants.addLast(e.id to e.lateral)
            }
        }
    }
}

// ---------------------------------------------------------------- Etat de verrouillage

class Verrou {
    var deverrouille by mutableStateOf(false)
    var dernierArret by mutableStateOf(0L)
}

// ---------------------------------------------------------------- Activite

class MainActivity : ComponentActivity() {
    val verrou = Verrou()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FLAG_SECURE : captures d'ecran et apercu dans les recents interdits
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme(colorScheme = PaletteGeneaPoche, shapes = FormesGeneaPoche) {
                AppEcran(this, verrou)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        verrou.dernierArret = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        val d = verrou.dernierArret
        if (d > 0L && System.currentTimeMillis() - d > 60_000L) {
            verrou.deverrouille = false
        }
    }
}

// ---------------------------------------------------------------- Ecran principal

@Composable
fun AppEcran(ctx: Context, verrou: Verrou) {
    val personnes = remember { mutableStateListOf<Personne>() }
    val pistes = remember { mutableStateListOf<Piste>() }
    val actes = remember { mutableStateListOf<Acte>() }
    var charge by remember { mutableStateOf(false) }
    var aPin by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var idOuvert by remember { mutableStateOf(0L) }
    var onglet by remember { mutableStateOf(1) }
    var editionOuverte by remember { mutableStateOf(false) }
    var editionCible by remember { mutableStateOf<Personne?>(null) }
    var pickerRole by remember { mutableStateOf("") }
    var confirmSuppression by remember { mutableStateOf<Personne?>(null) }
    var dialoguePiste by remember { mutableStateOf(false) }
    var pistePersonnePre by remember { mutableStateOf(0L) }
    var acteVisionne by remember { mutableStateOf<Acte?>(null) }
    var pisteRecherche by remember { mutableStateOf<Piste?>(null) }
    var arbreCentre by remember { mutableStateOf(0L) }
    var sosaCujus by remember { mutableStateOf(1L) }
    var cujusDialogue by remember { mutableStateOf(false) }
    var cujusPreselec by remember { mutableStateOf<Personne?>(null) }
    var importConfirme by remember { mutableStateOf<List<Personne>?>(null) }
    var cujasOuvert by remember { mutableStateOf(false) }

    // Recopie structurelle des listes : force Compose a rafraichir l'affichage
    // meme quand une Personne est modifiee en place (correctif mise a jour immediate).
    fun rafraichirTout() {
        val cp = personnes.toList()
        personnes.clear()
        personnes.addAll(cp)
        val cq = pistes.toList()
        pistes.clear()
        pistes.addAll(cq)
        val ca = actes.toList()
        actes.clear()
        actes.addAll(ca)
    }

    // Complete le journal : jusqu'a 5 recherches AUTOMATIQUES en cours (type "A"),
    // en faisant tourner les personnes (celles qui ont le moins de recherches ouvertes d'abord).
    // La piste memorise la CLE de la suggestion : le texte est recalculé en direct
    // et une meme recherche ne peut pas etre proposee deux fois (correctif doublons).
    fun completerJournal() {
        if (!charge) return
        var gardeFou = 0
        while (pistes.count { !it.faite && it.type == "A" } < 5 && gardeFou < 200) {
            gardeFou += 1
            val enCours = pistes.filter { !it.faite && it.type == "A" }
            val comptage = personnes.associate { p -> p.id to pistes.count { !it.faite && it.personneId == p.id } }
            var choisie: Personne? = null
            var sugChoisie: Suggestion? = null
            for (p in personnes.sortedWith(compareBy({ comptage[it.id] ?: 0 }, { it.titre() }))) {
                val deja = pistes.filter { !it.faite && it.personneId == p.id }.map { it.categorie }.toSet()
                val s = suggestionsPour(p, personnes, actes).firstOrNull { it.cle !in deja }
                if (s != null) {
                    choisie = p
                    sugChoisie = s
                    break
                }
            }
            if (choisie == null || sugChoisie == null) break
            val nouvelId = (pistes.maxOfOrNull { it.id } ?: 0L) + 1L
            pistes.add(
                Piste(
                    id = nouvelId,
                    texte = sugChoisie.titre,
                    personneId = choisie.id,
                    creation = System.currentTimeMillis(),
                    type = "A",
                    categorie = sugChoisie.cle,
                    lieu = sugChoisie.lieu,
                    dept = sugChoisie.dept,
                    debut = sugChoisie.debut,
                    fin = sugChoisie.fin
                )
            )
        }
    }

    fun enregistrer(): Boolean {
        completerJournal()
        val ok1 = Stockage.sauver(ctx, personnes)
        val ok2 = Stockage.sauverJournal(ctx, pistes)
        val ok3 = Stockage.sauverActes(ctx, actes)
        rafraichirTout()
        message = if (ok1 && ok2 && ok3) "" else "Echec de l'enregistrement."
        return ok1 && ok2 && ok3
    }

    fun supprimer(p: Personne) {
        personnes.remove(p)
        personnes.forEach { q ->
            if (q.pere == p.id) q.pere = 0L
            if (q.mere == p.id) q.mere = 0L
            if (q.conjoint == p.id) q.conjoint = 0L
            q.conjointsSupp.remove(p.id)
            q.enfants.remove(p.id)
        }
        pistes.forEach { if (it.personneId == p.id) it.personneId = 0L }
        val actesASupprimer = actes.filter { it.personneId == p.id }
        actes.removeAll(actesASupprimer)
        actesASupprimer.forEach { Stockage.supprimerImage(ctx, it.id) }
        enregistrer()
        if (idOuvert == p.id) idOuvert = 0L
    }

    fun lier(role: String, cibleId: Long) {
        val p = personneParId(personnes, idOuvert) ?: return
        val cible = personneParId(personnes, cibleId) ?: return
        when (role) {
            "pere" -> {
                p.pere = cibleId
                if (cible.sosa.isBlank() && cible.lateral.isBlank()) {
                    val s = sosaParent(p, false)
                    if (s.isNotEmpty()) cible.sosa = s
                }
            }
            "mere" -> {
                p.mere = cibleId
                if (cible.sosa.isBlank() && cible.lateral.isBlank()) {
                    val s = sosaParent(p, true)
                    if (s.isNotEmpty()) cible.sosa = s
                }
            }
            "conjoint" -> {
                p.conjoint = cibleId
                cible.conjoint = p.id
            }
            "conjointsupp" -> {
                if (cibleId != p.conjoint && !p.conjointsSupp.contains(cibleId)) {
                    p.conjointsSupp.add(cibleId)
                }
                if (p.id != cible.conjoint && !cible.conjointsSupp.contains(p.id)) {
                    cible.conjointsSupp.add(p.id)
                }
            }
            "enfant" -> {
                p.enfants.add(cibleId)
                if (p.sexe == "F") {
                    if (cible.mere == 0L) cible.mere = p.id
                    if (cible.pere == 0L && p.conjoint != 0L) cible.pere = p.conjoint
                } else {
                    if (cible.pere == 0L) cible.pere = p.id
                    if (cible.mere == 0L && p.conjoint != 0L) cible.mere = p.conjoint
                }
                if (cible.sosa.isBlank() && cible.lateral.isBlank()) {
                    val pref = prefixeLateral(p)
                    if (pref.isNotEmpty()) {
                        cible.lateral = pref + "." + p.enfants.size.toString()
                    }
                }
            }
        }
        enregistrer()
        pickerRole = ""
    }

    fun delier(role: String, enfantId: Long) {
        val p = personneParId(personnes, idOuvert) ?: return
        when (role) {
            "pere" -> p.pere = 0L
            "mere" -> p.mere = 0L
            "conjoint" -> {
                val c = personneParId(personnes, p.conjoint)
                p.conjoint = 0L
                if (c != null) c.conjoint = 0L
            }
            "conjointsupp" -> {
                p.conjointsSupp.remove(enfantId)
                personneParId(personnes, enfantId)?.conjointsSupp?.remove(p.id)
            }
            "enfant" -> {
                p.enfants.remove(enfantId)
                personneParId(personnes, enfantId)?.let { e ->
                    if (e.pere == p.id) e.pere = 0L
                    if (e.mere == p.id) e.mere = 0L
                }
            }
        }
        enregistrer()
    }

    fun ajouterPiste(texte: String, personneId: Long) {
        if (pistes.count { !it.faite && it.type == "P" } >= 5) {
            message = "5 recherches libres en cours maximum : validez-en une d'abord."
            dialoguePiste = false
            return
        }
        val nouvelId = (pistes.maxOfOrNull { it.id } ?: 0L) + 1L
        pistes.add(Piste(nouvelId, texte, personneId, System.currentTimeMillis(), false, 0L))
        enregistrer()
        dialoguePiste = false
    }

    fun basculerPiste(piste: Piste) {
        piste.faite = !piste.faite
        piste.cloture = if (piste.faite) System.currentTimeMillis() else 0L
        enregistrer()
    }

    // Definit le de cujus et son n Sosa (pas forcement 1) : toute la numerotation
    // Sosa et d'Aboville en decoule automatiquement.
    fun definirCujus(p: Personne, sosa: Long) {
        val s = if (sosa >= 1L) sosa else 1L
        renumeroter(personnes, p.id, s)
        sosaCujus = s
        Stockage.definirCujus(ctx, p.id, s)
        arbreCentre = p.id
        enregistrer()
    }

    // Identifiant du de cujus : memorise en preferences, sinon par n Sosa, sinon 1re personne.
    fun idDuCujus(): Long {
        val idMemorise = Stockage.lireIdCujus(ctx)
        if (personnes.any { it.id == idMemorise }) return idMemorise
        return personnes.firstOrNull { it.sosa.trim() == sosaCujus.toString() }?.id
            ?: personnes.firstOrNull()?.id
            ?: 0L
    }

    fun exporterGedcom(uri: Uri, sousEnsemble: Set<Long>?) {
        try {
            val texte = construireGedcom(personnes, sousEnsemble)
            ctx.contentResolver.openOutputStream(uri)?.use { sortie ->
                sortie.write(texte.toByteArray(Charsets.UTF_8))
            }
            val n = if (sousEnsemble == null) personnes.size else texte.split("\n").count { it.endsWith(" INDI") }
            message = "GEDCOM export\u00E9 (" + n.toString() + " personnes) : importable dans Heredis."
        } catch (e: Exception) {
            message = "Echec de l'export GEDCOM."
        }
    }

    fun lireGedcom(uri: Uri) {
        try {
            val texte = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (texte.isNullOrBlank()) {
                message = "Fichier GEDCOM illisible."
                return
            }
            val importees = importerGedcom(texte)
            if (importees.isEmpty()) {
                message = "Aucune personne trouv\u00E9e dans ce GEDCOM."
                return
            }
            importConfirme = importees
        } catch (e: Exception) {
            message = "Echec de la lecture du GEDCOM."
        }
    }

    fun remplacerBase(importees: List<Personne>) {
        personnes.clear()
        personnes.addAll(importees)
        pistes.clear()
        actes.forEach { Stockage.supprimerImage(ctx, it.id) }
        actes.clear()
        // De cujus apres import : la personne au plus petit n Sosa du fichier, sinon la 1re.
        val avecSosa = personnes.filter { it.sosa.trim().toLongOrNull() != null }
        val premier = avecSosa.minByOrNull { it.sosa.trim().toLong() }
            ?: personnes.firstOrNull()
        if (premier != null) {
            if (premier.sosa.isBlank()) {
                renumeroter(personnes, premier.id, 1L)
            }
            sosaCujus = premier.sosa.trim().toLongOrNull() ?: 1L
            Stockage.definirCujus(ctx, premier.id, sosaCujus)
        }
        arbreCentre = premier?.id ?: 0L
        enregistrer()
        message = if (importees.size >= 50) {
            "Import limit\u00E9 \u00E0 50 personnes (" + importees.size.toString() + " import\u00E9es)."
        } else {
            importees.size.toString() + " personnes import\u00E9es."
        }
    }

    // Vide entierement la base (personnes, journal, actes et images), apres confirmation.
    fun viderBase() {
        personnes.clear()
        pistes.clear()
        actes.forEach { Stockage.supprimerImage(ctx, it.id) }
        actes.clear()
        idOuvert = 0L
        arbreCentre = 0L
        enregistrer()
        message = "Base vid\u00E9e."
    }

    fun ajouterActe(personneId: Long, uri: Uri, type: String, titre: String) {
        val jpg = compresserVersJpeg(ctx, uri)
        if (jpg == null) {
            message = "Impossible de lire cette image."
            return
        }
        val nouvelId = (actes.maxOfOrNull { it.id } ?: 0L) + 1L
        if (Stockage.sauverImage(ctx, nouvelId, jpg)) {
            actes.add(Acte(nouvelId, personneId, type, titre, System.currentTimeMillis()))
            enregistrer()
        } else {
            message = "Echec de l'enregistrement de l'image."
        }
    }

    // Chargement uniquement apres deverrouillage (rien ne s'affiche sans PIN)
    LaunchedEffect(verrou.deverrouille) {
        if (verrou.deverrouille && !charge) {
            personnes.addAll(Stockage.charger(ctx))
            pistes.addAll(Stockage.chargerJournal(ctx))
            actes.addAll(Stockage.chargerActes(ctx))
            sosaCujus = Stockage.lireSosaCujus(ctx)
            charge = true
            completerJournal()
        }
    }
    LaunchedEffect(Unit) { aPin = Stockage.aPin(ctx) }

    // Ouverture sur le de cujus des le deverrouillage
    LaunchedEffect(charge, verrou.deverrouille) {
        if (charge && verrou.deverrouille && !cujasOuvert) {
            cujasOuvert = true
            idDuCujus().takeIf { it != 0L }?.let { idOuvert = it }
        }
    }

    // ------------------------------------------------------------ Porte PIN

    if (!aPin) {
        DialogueCreationPin(onCree = { pin ->
            if (Stockage.definirPin(ctx, pin)) {
                aPin = true
                verrou.deverrouille = true
            } else {
                message = "Echec de la cr\u00E9ation du PIN."
            }
        })
    } else if (!verrou.deverrouille) {
        EcranPin(
            ctx = ctx,
            onDeverrouille = {
                Stockage.reinitialiserEchecs(ctx)
                verrou.deverrouille = true
            }
        )
    } else {
        EcranApplication(
            personnes = personnes,
            pistes = pistes,
            charge = charge,
            message = message,
            onMessage = { message = it },
            idOuvert = idOuvert,
            onIdOuvert = { idOuvert = it },
            onglet = onglet,
            onOnglet = { onglet = it },
            editionOuverte = editionOuverte,
            onEditionOuverte = { editionOuverte = it },
            editionCible = editionCible,
            onEditionCible = { editionCible = it },
            pickerRole = pickerRole,
            onPickerRole = { pickerRole = it },
            confirmSuppression = confirmSuppression,
            onConfirmSuppression = { confirmSuppression = it },
            enregistrer = { enregistrer() },
            supprimer = { supprimer(it) },
            lier = { role, id -> lier(role, id) },
            delier = { role, id -> delier(role, id) },
            definirCujus = { cujusPreselec = it; cujusDialogue = true },
            choisirCujus = { cujusPreselec = null; cujusDialogue = true },
            renumeroter = { renumeroter(personnes, it, sosaCujus) },
            dialoguePiste = dialoguePiste,
            onDialoguePiste = { dialoguePiste = it },
            pistePersonnePre = pistePersonnePre,
            onPistePersonnePre = { pistePersonnePre = it },
            ajouterPiste = { t, id -> ajouterPiste(t, id) },
            basculerPiste = { basculerPiste(it) },
            supprimerPiste = {
                pistes.remove(it)
                enregistrer()
            },
            actes = actes,
            ajouterActe = { personneId, uri, type, titre -> ajouterActe(personneId, uri, type, titre) },
            onSupprimerActe = {
                Stockage.supprimerImage(ctx, it.id)
                actes.remove(it)
                enregistrer()
            },
            onVoirActe = { acteVisionne = it },
            noterRecherche = { pisteRecherche = it },
            arbreCentre = if (arbreCentre != 0L) {
                arbreCentre
            } else {
                idDuCujus()
            },
            onArbreCentre = { arbreCentre = it },
            exporterGedcom = { uri, sel -> exporterGedcom(uri, sel) },
            importerGedcom = { uri -> lireGedcom(uri) },
            viderBase = { viderBase() },
            ouvrirLien = { url -> ouvrirLien(ctx, url)?.let { m -> message = m } },
            ouvrirEtatCivil = { commune, dept, type, annee ->
                ouvrirLien(ctx, lienEtatCivil(commune, dept, type, annee))?.let { m -> message = m }
            }
        )
    }

    acteVisionne?.let { a ->
        VisionneuseActe(ctx = ctx, acte = a, onFermer = { acteVisionne = null })
    }

    pisteRecherche?.let { pr ->
        DialogueRecherche(
            onValider = { r ->
                pr.historique.add(0, r)
                enregistrer()
                pisteRecherche = null
            },
            onAnnuler = { pisteRecherche = null }
        )
    }

    // Dialogue : choix du de cujus et de son n Sosa (pas forcement 1).
    // Toute la numerotation Sosa / d'Aboville en decoule automatiquement.
    if (cujusDialogue) {
        var choixCujus by remember { mutableStateOf(cujusPreselec) }
        var numCujus by remember {
            mutableStateOf((choixCujus?.sosa?.trim()?.toLongOrNull() ?: sosaCujus).toString())
        }
        val numValide = (numCujus.trim().toLongOrNull() ?: 0L) >= 1L
        AlertDialog(
            onDismissRequest = { cujusDialogue = false },
            title = { Text("De cujus") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Choisissez la personne de r\u00E9f\u00E9rence et son num\u00E9ro Sosa (pas forc\u00E9ment 1). Toute la num\u00E9rotation Sosa et d'Aboville sera recalcul\u00E9e automatiquement \u00E0 partir de l\u00E0 : aucun num\u00E9ro ne se saisit ailleurs.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = numCujus,
                        onValueChange = { t -> numCujus = t.filter { it.isDigit() } },
                        label = { Text("N\u00B0 Sosa du de cujus") },
                        isError = !numValide,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Text(
                        "Personne : " + (choixCujus?.titre() ?: "\u00E0 choisir"),
                        Modifier.padding(top = 10.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                        personnes.sortedWith(
                            compareBy({ it.nom.trim().uppercase() }, { it.prenoms.trim().uppercase() })
                        ).forEach { cand ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { choixCujus = cand }
                                    .background(
                                        if (choixCujus?.id == cand.id) CouleurVertPale
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(cand.titre(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (choixCujus != null && choixCujus?.id != idDuCujus()) {
                        Text(
                            "Toute la num\u00E9rotation sera recalcul\u00E9e.",
                            Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = CouleurRouge
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val c = choixCujus
                        val n = numCujus.trim().toLongOrNull() ?: 1L
                        if (c != null && n >= 1L) {
                            definirCujus(c, n)
                        }
                        cujusDialogue = false
                    },
                    enabled = choixCujus != null && numValide
                ) { Text("D\u00E9finir") }
            },
            dismissButton = {
                TextButton(onClick = { cujusDialogue = false }) { Text("Annuler") }
            }
        )
    }

    importConfirme?.let { importees ->
        AlertDialog(
            onDismissRequest = { importConfirme = null },
            title = { Text("Importer ce GEDCOM ?") },
            text = {
                Text(
                    "La base actuelle (" + personnes.size.toString() + " personnes, pistes et actes) sera REMPLAC\u00C9E par les " + importees.size.toString() + " personnes du fichier. Pensez \u00E0 exporter la branche actuelle en GEDCOM avant de continuer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    remplacerBase(importees)
                    importConfirme = null
                }) { Text("Remplacer") }
            },
            dismissButton = {
                TextButton(onClick = { importConfirme = null }) { Text("Annuler") }
            }
        )
    }
}

// ------------------------------------------------------------ Creation du PIN

@Composable
fun DialogueCreationPin(onCree: (String) -> Unit) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text("Bienvenue dans G\u00E9n\u00E9aPoche", style = MaterialTheme.typography.titleLarge)
            Text("D\u00E9finissez un code PIN pour prot\u00E9ger vos donn\u00E9es.", Modifier.padding(vertical = 8.dp))
            OutlinedTextField(
                value = pin1,
                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin1 = it },
                label = { Text("Code PIN (4 \u00E0 8 chiffres)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin2,
                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin2 = it },
                label = { Text("Confirmez le code") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (pin1.length >= 4 && pin1 == pin2) onCree(pin1)
                }),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            if (erreur.isNotEmpty()) {
                Text(erreur, color = CouleurRouge, modifier = Modifier.padding(vertical = 8.dp))
            }
            Text(
                "Attention : sans ce code, vos donn\u00E9es sont ind\u00E9chiffrables (aucune r\u00E9cup\u00E9ration possible).",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = {
                    if (pin1.length < 4) {
                        erreur = "Le code doit contenir au moins 4 chiffres."
                    } else if (pin1 != pin2) {
                        erreur = "Les deux codes ne correspondent pas."
                    } else {
                        erreur = ""
                        onCree(pin1)
                    }
                },
                enabled = pin1.isNotEmpty() && pin2.isNotEmpty()
            ) { Text("Cr\u00E9er le code") }
        }
    }
}

// ------------------------------------------------------------ Ecran de deverrouillage

@Composable
fun EcranPin(ctx: Context, onDeverrouille: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf("") }
    var blocage by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            blocage = Stockage.secondesBlocage(ctx)
            delay(1000L)
        }
    }

    fun essayer() {
        if (pin.isEmpty() || blocage > 0L) return
        if (Stockage.verifierPin(ctx, pin)) {
            onDeverrouille()
        } else {
            Stockage.enregistrerEchec(ctx)
            blocage = Stockage.secondesBlocage(ctx)
            erreur = "Code incorrect."
        }
        pin = ""
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text("G\u00E9n\u00E9aPoche est verrouill\u00E9", style = MaterialTheme.typography.titleLarge)
            Text("Entrez votre code PIN.", Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("Code PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { essayer() }),
                    isError = erreur.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { essayer() },
                    enabled = pin.isNotEmpty() && blocage == 0L
                ) { Text("D\u00E9verrouiller") }
            }
            if (erreur.isNotEmpty()) {
                Text(erreur, color = CouleurRouge, modifier = Modifier.padding(vertical = 6.dp))
            }
            if (blocage > 0L) {
                Text(
                    "Trop d'essais. R\u00E9essayez dans " + blocage.toString() + " s.",
                    color = CouleurRouge,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}



