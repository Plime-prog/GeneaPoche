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

// Couleurs des pastilles
val CouleurVert = Color(0xFF2E7D32)
val CouleurRouge = Color(0xFFC62828)

private val formatJour = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

fun formaterDate(ts: Long): String =
    if (ts > 0L) formatJour.format(Date(ts)) else ""

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
            MaterialTheme {
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
    var onglet by remember { mutableStateOf(0) }
    var editionOuverte by remember { mutableStateOf(false) }
    var editionCible by remember { mutableStateOf<Personne?>(null) }
    var pickerRole by remember { mutableStateOf("") }
    var confirmSuppression by remember { mutableStateOf<Personne?>(null) }
    var dialoguePiste by remember { mutableStateOf(false) }
    var pistePersonnePre by remember { mutableStateOf(0L) }
    var acteVisionne by remember { mutableStateOf<Acte?>(null) }
    var pisteRecherche by remember { mutableStateOf<Piste?>(null) }

    fun enregistrer(): Boolean {
        val ok1 = Stockage.sauver(ctx, personnes)
        val ok2 = Stockage.sauverJournal(ctx, pistes)
        val ok3 = Stockage.sauverActes(ctx, actes)
        message = if (ok1 && ok2 && ok3) "Enregistr\u00E9." else "Echec de l'enregistrement."
        return ok1 && ok2 && ok3
    }

    fun supprimer(p: Personne) {
        personnes.remove(p)
        personnes.forEach { q ->
            if (q.pere == p.id) q.pere = 0L
            if (q.mere == p.id) q.mere = 0L
            if (q.conjoint == p.id) q.conjoint = 0L
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

    fun noterDepuisSuggestion(personne: Personne, s: Suggestion) {
        val nouvelId = (pistes.maxOfOrNull { it.id } ?: 0L) + 1L
        val np = Piste(
            id = nouvelId,
            texte = s.titre,
            personneId = personne.id,
            creation = System.currentTimeMillis(),
            type = "A",
            lieu = s.lieu,
            debut = s.debut,
            fin = s.fin
        )
        pistes.add(np)
        enregistrer()
        pisteRecherche = np
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
            charge = true
        }
    }
    LaunchedEffect(Unit) { aPin = Stockage.aPin(ctx) }

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
            noterSuggestion = { personne, sug -> noterDepuisSuggestion(personne, sug) }
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
            OutlinedButton(
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

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text("G\u00E9n\u00E9aPoche est verrouill\u00E9", style = MaterialTheme.typography.titleLarge)
            Text("Entrez votre code PIN.", Modifier.padding(vertical = 8.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text("Code PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = erreur.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            if (erreur.isNotEmpty()) {
                Text(erreur, color = CouleurRouge, modifier = Modifier.padding(vertical = 6.dp))
            }
            if (blocage > 0L) {
                Text(
                    "Trop d'essais. R\u00E9essayez dans " + blocage + " s.",
                    color = CouleurRouge,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    if (Stockage.verifierPin(ctx, pin)) {
                        onDeverrouille()
                    } else {
                        Stockage.enregistrerEchec(ctx)
                        blocage = Stockage.secondesBlocage(ctx)
                        erreur = "Code incorrect."
                    }
                    pin = ""
                },
                enabled = pin.isNotEmpty() && blocage == 0L
            ) { Text("D\u00E9verrouiller") }
        }
    }
}

