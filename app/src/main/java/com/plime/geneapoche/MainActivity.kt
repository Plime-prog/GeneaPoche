package com.plime.geneapoche

import android.content.Context
import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- Aides numerotation

fun personneParId(liste: List<Personne>, id: Long): Personne? =
    liste.firstOrNull { it.id == id }

// Sosa du parent d'une personne : pere = 2n, mere = 2n+1 (si sosa connu).
fun sosaParent(p: Personne, mere: Boolean): String {
    val s = p.sosa.trim().toLongOrNull() ?: return ""
    return (if (mere) 2L * s + 1L else 2L * s).toString()
}

// Prefixe d'Aboville d'une personne = son propre lateral, sinon son sosa.
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

// ---------------------------------------------------------------- Activite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppEcran(this)
            }
        }
    }
}

// ---------------------------------------------------------------- Ecran principal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppEcran(ctx: Context) {
    val personnes = remember { mutableStateListOf<Personne>() }
    var charge by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var idOuvert by remember { mutableStateOf(0L) }
    var editionOuverte by remember { mutableStateOf(false) }
    var editionCible by remember { mutableStateOf<Personne?>(null) }
    var pickerRole by remember { mutableStateOf("") }
    var confirmSuppression by remember { mutableStateOf<Personne?>(null) }

    fun enregistrer(): Boolean {
        val ok = Stockage.sauver(ctx, personnes)
        message = if (ok) "Enregistr\u00E9." else "Echec de l'enregistrement."
        return ok
    }

    fun supprimer(p: Personne) {
        personnes.remove(p)
        personnes.forEach { q ->
            if (q.pere == p.id) q.pere = 0L
            if (q.mere == p.id) q.mere = 0L
            if (q.conjoint == p.id) q.conjoint = 0L
            q.enfants.remove(p.id)
        }
        enregistrer()
        if (idOuvert == p.id) idOuvert = 0L
    }

    // Lier une personne (role pere / mere / conjoint / enfant) a la personne ouverte
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
                        cible.lateral = pref + "." + (p.enfants.size - 1 + 1).toString()
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

    LaunchedEffect(Unit) {
        personnes.addAll(Stockage.charger(ctx))
        charge = true
    }

    BackHandler(enabled = idOuvert != 0L || editionOuverte || pickerRole.isNotEmpty()) {
        when {
            pickerRole.isNotEmpty() -> pickerRole = ""
            editionOuverte -> editionOuverte = false
            else -> idOuvert = 0L
        }
    }

    val ouverte = personneParId(personnes, idOuvert)

    if (ouverte == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("G\u00E9n\u00E9aPoche") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { editionCible = null; editionOuverte = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                }
            }
        ) { marges ->
            if (!charge) {
                Column(Modifier.fillMaxSize().padding(marges).padding(16.dp)) {
                    Text("Chargement...")
                }
            } else if (personnes.isEmpty()) {
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
                            onOuvrir = { idOuvert = p.id },
                            onSupprimer = { confirmSuppression = p }
                        )
                    }
                }
            }
        }
    } else {
        val p = ouverte
        EcranPersonne(
            personnes = personnes,
            p = p,
            onOuvrir = { idOuvert = it },
            onRetour = { idOuvert = 0L },
            onModifier = { editionCible = p; editionOuverte = true },
            onSupprimer = { confirmSuppression = p },
            onChoisir = { pickerRole = it },
            onRetirer = { role, enfantId -> delier(role, enfantId) }
        )
    }

    if (message.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            title = { Text("Information") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { message = "" }) { Text("OK") }
            }
        )
    }

    if (editionOuverte) {
        val prochainId = (personnes.maxOfOrNull { it.id } ?: 0L) + 1L
        DialogueEdition(
            cible = editionCible,
            idNouvelle = prochainId,
            onValider = { p ->
                if (editionCible == null) personnes.add(p)
                enregistrer()
                editionOuverte = false
            },
            onAnnuler = { editionOuverte = false }
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
            onDismissRequest = { pickerRole = "" },
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
                TextButton(onClick = { pickerRole = "" }) { Text("Annuler") }
            }
        )
    }

    if (confirmSuppression != null) {
        val cible = confirmSuppression
        AlertDialog(
            onDismissRequest = { confirmSuppression = null },
            title = { Text("Supprimer ?") },
            text = { Text("Supprimer d\u00E9finitivement " + cible.titre() + " ? Les liens de parent\u00E9 seront retir\u00E9s.") },
            confirmButton = {
                TextButton(onClick = {
                    supprimer(cible)
                    confirmSuppression = null
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSuppression = null }) { Text("Annuler") }
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
            }
            IconButton(onClick = onSupprimer) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer")
            }
        }
    }
}

// ---------------------------------------------------------------- Fiche personne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPersonne(
    personnes: List<Personne>,
    p: Personne,
    onOuvrir: (Long) -> Unit,
    onRetour: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit,
    onChoisir: (String) -> Unit,
    onRetirer: (String, Long) -> Unit
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
            item { CarteIdentite(p) }
            item { CarteEvenement("Naissance", p.dateNaissance, p.lieuNaissance) }
            item { CarteEvenement("Mariage", p.dateMariage, p.lieuMariage) }
            item { CarteEvenement("D\u00E9c\u00E8s", p.dateDeces, p.lieuDeces) }
            item {
                CarteFamille(
                    personnes = personnes,
                    p = p,
                    onOuvrir = onOuvrir,
                    onChoisir = onChoisir,
                    onRetirer = onRetirer
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun CarteIdentite(p: Personne) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Identit\u00E9", fontWeight = FontWeight.Bold)
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
fun CarteEvenement(titre: String, date: String, lieu: String) {
    val d = date.trim()
    val l = lieu.trim()
    if (d.isEmpty() && l.isEmpty()) return
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(titre, fontWeight = FontWeight.Bold)
            Ligne("Date", d)
            Ligne("Lieu", l)
        }
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
