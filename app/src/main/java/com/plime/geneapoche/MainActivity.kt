package com.plime.geneapoche

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

    fun enregistrer(): Boolean {
        val ok1 = Stockage.sauver(ctx, personnes)
        val ok2 = Stockage.sauverJournal(ctx, pistes)
        message = if (ok1 && ok2) "Enregistr\u00E9." else "Echec de l'enregistrement."
        return ok1 && ok2
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

    // Chargement uniquement apres deverrouillage (rien ne s'affiche sans PIN)
    LaunchedEffect(verrou.deverrouille) {
        if (verrou.deverrouille && !charge) {
            personnes.addAll(Stockage.charger(ctx))
            pistes.addAll(Stockage.chargerJournal(ctx))
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

// ------------------------------------------------------------ Application deverrouillee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranApplication(
    personnes: List<Personne>,
    pistes: List<Piste>,
    charge: Boolean,
    message: String,
    onMessage: (String) -> Unit,
    idOuvert: Long,
    onIdOuvert: (Long) -> Unit,
    onglet: Int,
    onOnglet: (Int) -> Unit,
    editionOuverte: Boolean,
    onEditionOuverte: (Boolean) -> Unit,
    editionCible: Personne?,
    onEditionCible: (Personne?) -> Unit,
    pickerRole: String,
    onPickerRole: (String) -> Unit,
    confirmSuppression: Personne?,
    onConfirmSuppression: (Personne?) -> Unit,
    enregistrer: () -> Unit,
    supprimer: (Personne) -> Unit,
    lier: (String, Long) -> Unit,
    delier: (String, Long) -> Unit,
    dialoguePiste: Boolean,
    onDialoguePiste: (Boolean) -> Unit,
    pistePersonnePre: Long,
    onPistePersonnePre: (Long) -> Unit,
    ajouterPiste: (String, Long) -> Unit,
    basculerPiste: (Piste) -> Unit,
    supprimerPiste: (Piste) -> Unit
) {
    BackHandler(enabled = idOuvert != 0L || editionOuverte || pickerRole.isNotEmpty() || dialoguePiste) {
        when {
            dialoguePiste -> onDialoguePiste(false)
            pickerRole.isNotEmpty() -> onPickerRole("")
            editionOuverte -> onEditionOuverte(false)
            else -> onIdOuvert(0L)
        }
    }

    val ouverte = personneParId(personnes, idOuvert)

    if (ouverte == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("G\u00E9n\u00E9aPoche") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = onglet == 0,
                        onClick = { onOnglet(0) },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Personnes") }
                    )
                    NavigationBarItem(
                        selected = onglet == 1,
                        onClick = { onOnglet(1) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("Journal") }
                    )
                }
            },
            floatingActionButton = {
                if (onglet == 0) {
                    FloatingActionButton(onClick = { onEditionCible(null); onEditionOuverte(true) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                    }
                } else {
                    FloatingActionButton(onClick = { onPistePersonnePre(0L); onDialoguePiste(true) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nouvelle piste")
                    }
                }
            }
        ) { marges ->
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
                        Text("Touchez le bouton + pour ajouter la premi\u00E8re personne (de cujus, Sosa 1).")
                    }
                } else {
                    val triees = personnes.sortedWith(
                        compareBy({ it.sosa.trim().toLongOrNull() ?: Long.MAX_VALUE }, { it.titre() })
                    )
                    LazyColumn(Modifier.fillMaxSize().padding(marges).padding(vertical = 4.dp)) {
                        items(triees) { p ->
                            CartePersonne(
                                p,
                                onOuvrir = { onIdOuvert(p.id) },
                                onSupprimer = { onConfirmSuppression(p) }
                            )
                        }
                    }
                }
            } else {
                EcranJournal(
                    pistes = pistes,
                    personnes = personnes,
                    onBasculer = { basculerPiste(it) },
                    onSupprimer = { supprimerPiste(it) },
                    onOuvrirPersonne = { onIdOuvert(it) }
                )
            }
        }
    } else {
        val p = ouverte
        EcranPersonne(
            personnes = personnes,
            pistes = pistes,
            p = p,
            onOuvrir = { onIdOuvert(it) },
            onRetour = { onIdOuvert(0L) },
            onModifier = { onEditionCible(p); onEditionOuverte(true) },
            onSupprimer = { onConfirmSuppression(p) },
            onChoisir = { onPickerRole(it) },
            onRetirer = { role, enfantId -> delier(role, enfantId) },
            onEtat = { section, valeur ->
                p.changerEtat(section, valeur)
                enregistrer()
            },
            onNouvellePiste = { onPistePersonnePre(p.id); onDialoguePiste(true) },
            onBasculerPiste = { basculerPiste(it) }
        )
    }

    if (message.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { onMessage("") },
            title = { Text("Information") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { onMessage("") }) { Text("OK") }
            }
        )
    }

    if (editionOuverte) {
        val prochainId = (personnes.maxOfOrNull { it.id } ?: 0L) + 1L
        DialogueEdition(
            cible = editionCible,
            idNouvelle = prochainId,
            onValider = { p2 ->
                if (editionCible == null) {
                    (personnes as? MutableList<Personne>)?.add(p2)
                }
                enregistrer()
                onEditionOuverte(false)
            },
            onAnnuler = { onEditionOuverte(false) }
        )
    }

    if (dialoguePiste) {
        DialoguePiste(
            personnes = personnes,
            personnePredefinie = pistePersonnePre,
            onValider = { texte, personneId -> ajouterPiste(texte, personneId) },
            onAnnuler = { onDialoguePiste(false) }
        )
    }

    if (pickerRole.isNotEmpty() && ouverte != null) {
        val p = ouverte
        val role = pickerRole
        val candidats = personnes
            .filter { q ->
                q.id != p.id && when (role) {
                    "pere" -> q.id != p.mere
                    "mere" -> q.id != p.pere
                    "conjoint" -> q.id != p.conjoint
                    "enfant" -> q.id != p.pere && q.id != p.mere && !p.enfants.contains(q.id)
                    else -> false
                }
            }
            .sortedBy { it.titre() }
        val titrePicker = when (role) {
            "pere" -> "Choisir le p\u00E8re"
            "mere" -> "Choisir la m\u00E8re"
            "conjoint" -> "Choisir le conjoint"
            else -> "Ajouter un enfant"
        }
        AlertDialog(
            onDismissRequest = { onPickerRole("") },
            title = { Text(titrePicker) },
            text = {
                if (candidats.isEmpty()) {
                    Text("Aucune autre personne disponible. Cr\u00E9ez-la d'abord depuis la liste avec le bouton +.")
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        candidats.forEach { c ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { lier(role, c.id) }
                                    .padding(vertical = 10.dp)
                            ) {
                                val num = c.numeroAffiche()
                                if (num.isNotEmpty()) {
                                    Text(num + "  ", fontWeight = FontWeight.Bold)
                                }
                                Text(c.titre())
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

// ---------------------------------------------------------------- Liste : carte personne

@Composable
fun CartePersonne(p: Personne, onOuvrir: () -> Unit, onSupprimer: () -> Unit) {
    Card(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable { onOuvrir() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(p.titre(), style = MaterialTheme.typography.titleMedium)
                val num = p.numeroAffiche()
                if (num.isNotEmpty()) {
                    Text(num, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                val n = p.dateNaissance.trim()
                val d = p.dateDeces.trim()
                if (n.isNotEmpty() || d.isNotEmpty()) {
                    val morceaux = mutableListOf<String>()
                    if (n.isNotEmpty()) morceaux.add("N. " + n)
                    if (d.isNotEmpty()) morceaux.add("D. " + d)
                    Text(morceaux.joinToString("  \u2013  "), style = MaterialTheme.typography.bodySmall)
                }
                if (p.aVerifier()) {
                    Text("\u00C0 v\u00E9rifier", color = CouleurRouge, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onSupprimer) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer")
            }
        }
    }
}

// ---------------------------------------------------------------- Journal

@Composable
fun EcranJournal(
    pistes: List<Piste>,
    personnes: List<Personne>,
    onBasculer: (Piste) -> Unit,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit
) {
    if (pistes.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Aucune piste de recherche.")
            Text("Touchez le bouton + pour noter une recherche \u00E0 faire : rien ne sera oubli\u00E9.")
        }
    } else {
        val triees = pistes.sortedWith(compareBy({ it.faite }, { -it.creation }))
        LazyColumn(Modifier.fillMaxSize().padding(vertical = 4.dp)) {
            items(triees) { piste ->
                CartePiste(
                    piste = piste,
                    personnes = personnes,
                    onBasculer = { onBasculer(piste) },
                    onSupprimer = { onSupprimer(piste) },
                    onOuvrirPersonne = onOuvrirPersonne
                )
            }
        }
    }
}

@Composable
fun CartePiste(
    piste: Piste,
    personnes: List<Personne>,
    onBasculer: () -> Unit,
    onSupprimer: () -> Unit,
    onOuvrirPersonne: (Long) -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = piste.faite,
                onCheckedChange = { onBasculer() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CouleurVert,
                    checkmarkColor = Color.White
                )
            )
            Column(Modifier.weight(1f)) {
                Text(
                    piste.texte,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (piste.faite) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                )
                val c = personneParId(personnes, piste.personneId)
                if (c != null) {
                    Text(
                        "\u2192 " + c.titre(),
                        Modifier.clickable { onOuvrirPersonne(piste.personneId) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val dates = mutableListOf<String>()
                dates.add("cr\u00E9\u00E9e le " + formaterDate(piste.creation))
                if (piste.faite) dates.add("faite le " + formaterDate(piste.cloture))
                Text(dates.joinToString(" \u2013 "), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onSupprimer) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer")
            }
        }
    }
}

@Composable
fun DialoguePiste(
    personnes: List<Personne>,
    personnePredefinie: Long,
    onValider: (String, Long) -> Unit,
    onAnnuler: () -> Unit
) {
    var texte by remember { mutableStateOf("") }
    var personneId by remember { mutableStateOf(personnePredefinie) }
    var pickerOuvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvelle piste de recherche") },
        text = {
            Column {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Quoi rechercher ? (acte, ann\u00E9e, lieu...)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                val liee = personneParId(personnes, personneId)
                OutlinedButton(onClick = { pickerOuvert = true }, Modifier.padding(top = 8.dp)) {
                    Text(
                        if (liee == null) "Lier une personne (facultatif)"
                        else "Personne : " + liee.titre()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(texte.trim(), personneId) },
                enabled = texte.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )

    if (pickerOuvert) {
        AlertDialog(
            onDismissRequest = { pickerOuvert = false },
            title = { Text("Personne concern\u00E9e") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { personneId = 0L; pickerOuvert = false }) {
                        Text("Aucune personne")
                    }
                    personnes.sortedBy { it.titre() }.forEach { c ->
                        TextButton(onClick = { personneId = c.id; pickerOuvert = false }) {
                            Text(c.titre())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerOuvert = false }) { Text("Fermer") }
            }
        )
    }
}

// ---------------------------------------------------------------- Fiche personne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPersonne(
    personnes: List<Personne>,
    pistes: List<Piste>,
    p: Personne,
    onOuvrir: (Long) -> Unit,
    onRetour: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit,
    onEtat: (String, String) -> Unit,
    onNouvellePiste: () -> Unit,
    onBasculerPiste: (Piste) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(p.titre()) },
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
