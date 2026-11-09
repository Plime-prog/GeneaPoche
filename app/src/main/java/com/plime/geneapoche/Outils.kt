package com.plime.geneapoche

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

// ================================================================ v0.87
// Boite a outils : convertisseur republicain / gregorien, calculateur de
// dates, calculateur de parente. Tout est LOCAL (aucun reseau). Chaque
// resultat peut etre ajoute a la note libre d'une fiche.

private fun versTexteRecherche(s: String): String = sansAccents(s).lowercase()

// Nom d'une personne pour les listes et les notes.
private fun nomListe(p: Personne): String = libelleUnique(p).ifEmpty { "(sans nom)" }

// Evenements datables d'une fiche : (intitule, date telle que saisie).
private fun evenementsDatesDe(p: Personne): List<Pair<String, String>> {
    val l = mutableListOf<Pair<String, String>>()
    if (p.dateNaissance.isNotBlank()) l.add("naissance" to p.dateNaissance.trim())
    if (p.dateBapteme.isNotBlank()) l.add("bapt\u00eame" to p.dateBapteme.trim())
    if (p.dateMariage.isNotBlank()) l.add("mariage" to p.dateMariage.trim())
    if (p.dateDeces.isNotBlank()) l.add("d\u00e9c\u00e8s" to p.dateDeces.trim())
    if (p.dateSepulture.isNotBlank()) l.add("s\u00e9pulture" to p.dateSepulture.trim())
    p.evenementsLibres.forEach {
        if (it.date.isNotBlank()) l.add(it.intitule.trim().ifEmpty { "\u00e9v\u00e9nement" } to it.date.trim())
    }
    return l
}

@Composable
fun EcranOutils(
    personnes: List<Personne>,
    onAjouterNote: (Long, String) -> Unit,
    onFermer: () -> Unit
) {
    var outil by remember { mutableStateOf(0) }
    var noteTexte by remember { mutableStateOf("") }
    var noteDefaut by remember { mutableStateOf(0L) }
    var noteOuverte by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf("") }
    val demanderNote: (String, Long) -> Unit = { texte, defaut ->
        noteTexte = texte
        noteDefaut = defaut
        confirmation = ""
        noteOuverte = true
    }
    Dialog(
        onDismissRequest = onFermer,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bo\u00eete \u00e0 outils", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onFermer) { Text("Fermer") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    FilterChip(selected = outil == 0, onClick = { outil = 0 }, label = { Text("Calendrier") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = outil == 1, onClick = { outil = 1 }, label = { Text("Dates") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = outil == 2, onClick = { outil = 2 }, label = { Text("Parent\u00e9") })
                }
                Spacer(Modifier.height(8.dp))
                if (confirmation.isNotEmpty()) {
                    Text(confirmation, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    when (outil) {
                        0 -> OutilCalendrier(demanderNote)
                        1 -> OutilDates(personnes, demanderNote)
                        else -> OutilParente(personnes, demanderNote)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
    if (noteOuverte) {
        DialogueNoteOutil(
            texteInitial = noteTexte,
            personnes = personnes,
            defautId = noteDefaut,
            onAjouter = { id, texte ->
                onAjouterNote(id, texte)
                val p = personneParId(personnes, id)
                confirmation = "Ajout\u00e9 \u00e0 la note de " + (if (p != null) p.titre() else "la fiche") + "."
                noteOuverte = false
            },
            onFermer = { noteOuverte = false }
        )
    }
}

@Composable
private fun TitreOutil(titre: String, aide: String) {
    Text(titre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Text(aide, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BoutonNote(texte: String, defautId: Long, demanderNote: (String, Long) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Button(onClick = { demanderNote(texte, defautId) }, Modifier.fillMaxWidth()) {
        Text("Ajouter ce r\u00e9sultat \u00e0 la note de recherche")
    }
}

// ---------------------------------------------------------------- Outil 1
@Composable
private fun OutilCalendrier(demanderNote: (String, Long) -> Unit) {
    var saisie by remember { mutableStateOf("") }
    TitreOutil(
        "Calendrier r\u00e9publicain \u2194 gr\u00e9gorien",
        "Saisissez une date gr\u00e9gorienne (5 octobre 1798, 05/10/1798) ou r\u00e9publicaine (14 Vend\u00e9miaire an VII)."
    )
    OutlinedTextField(
        value = saisie,
        onValueChange = { saisie = it },
        label = { Text("Date") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    val d: LocalDate? = lireDate(saisie)
    if (saisie.isBlank()) return
    if (d == null) {
        Text("Date non reconnue.", color = MaterialTheme.colorScheme.error)
        return
    }
    val lignes = lignesInfoJour(infoJour(d))
    lignes.forEach { Text(it, Modifier.padding(vertical = 2.dp)) }
    val note = "Calendrier : " + saisie.trim() + "\n" + lignes.joinToString("\n")
    BoutonNote(note, 0L, demanderNote)
}

// ---------------------------------------------------------------- Outil 2
@Composable
private fun OutilDates(personnes: List<Personne>, demanderNote: (String, Long) -> Unit) {
    var mode by remember { mutableStateOf(0) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("\u00c2ge / \u00e9cart") })
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Date \u00b1 dur\u00e9e") })
    }
    Spacer(Modifier.height(8.dp))
    if (mode == 0) EcartDates(personnes, demanderNote) else DateAvecDuree(demanderNote)
}

@Composable
private fun ChampDateFiche(
    etiquette: String,
    valeur: String,
    onValeur: (String) -> Unit,
    personnes: List<Personne>,
    onFiche: (String, Long) -> Unit
) {
    var choixPersonne by remember { mutableStateOf(false) }
    var personneChoisie by remember { mutableStateOf<Personne?>(null) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = valeur,
            onValueChange = onValeur,
            label = { Text(etiquette) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { choixPersonne = true }) { Text("Fiche") }
    }
    if (choixPersonne) {
        DialogueChoixPersonne(
            titre = etiquette + " : choisir une fiche",
            personnes = personnes,
            onChoisi = { personneChoisie = it; choixPersonne = false },
            onFermer = { choixPersonne = false }
        )
    }
    personneChoisie?.let { p ->
        val evts = evenementsDatesDe(p)
        AlertDialog(
            onDismissRequest = { personneChoisie = null },
            title = { Text(p.titre()) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (evts.isEmpty()) Text("Aucune date renseign\u00e9e sur cette fiche.")
                    evts.forEach { e ->
                        TextButton(
                            onClick = {
                                onValeur(e.second)
                                onFiche(e.first + " de " + p.titre(), p.id)
                                personneChoisie = null
                            },
                            Modifier.fillMaxWidth()
                        ) {
                            Text(e.first + " : " + e.second, Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { personneChoisie = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun EcartDates(personnes: List<Personne>, demanderNote: (String, Long) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var libA by remember { mutableStateOf("") }
    var libB by remember { mutableStateOf("") }
    var idA by remember { mutableStateOf(0L) }
    TitreOutil(
        "\u00c2ge et \u00e9cart entre deux dates",
        "Dates compl\u00e8tes, mois et ann\u00e9e (mars 1842) ou ann\u00e9e seule (1821). Le bouton Fiche reprend une date de la base."
    )
    ChampDateFiche(
        "D\u00e9part (naissance...)", a,
        { a = it; libA = "" },
        personnes,
        { l, id -> libA = l; idA = id }
    )
    Spacer(Modifier.height(8.dp))
    ChampDateFiche(
        "Arriv\u00e9e (mariage, \u00e9v\u00e9nement...)", b,
        { b = it; libB = "" },
        personnes,
        { l, _ -> libB = l }
    )
    Spacer(Modifier.height(12.dp))
    val pa = lireDatePartielle(a)
    val pb = lireDatePartielle(b)
    if (a.isBlank() || b.isBlank()) return
    if (pa == null || pb == null) {
        Text("Date non reconnue.", color = MaterialTheme.colorScheme.error)
        return
    }
    val duree = ecartDates(pa, pb)
    val etiqA = if (libA.isEmpty()) a.trim() else libA + " (" + a.trim() + ")"
    val etiqB = if (libB.isEmpty()) b.trim() else libB + " (" + b.trim() + ")"
    val texte = texteEcart(etiqA, etiqB, duree)
    Text(libelleDuree(duree), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
    if (duree.totalJours != null) {
        Text(separerMilliers(duree.totalJours) + " jours au total")
    }
    if (duree.precision != "jour") {
        Text("Dates incompl\u00e8tes : pr\u00e9cision " + (if (duree.precision == "mois") "au mois." else "\u00e0 l'ann\u00e9e."))
    }
    if (duree.inverse) {
        Text("Attention : la seconde date pr\u00e9c\u00e8de la premi\u00e8re.", color = MaterialTheme.colorScheme.error)
    }
    lireDate(a)?.let { Text("D\u00e9part : " + jourSemaineDe(it) + " " + dateLongue(it)) }
    lireDate(b)?.let { Text("Arriv\u00e9e : " + jourSemaineDe(it) + " " + dateLongue(it)) }
    BoutonNote(texte, idA, demanderNote)
}

@Composable
private fun DateAvecDuree(demanderNote: (String, Long) -> Unit) {
    var date by remember { mutableStateOf("") }
    var retirer by remember { mutableStateOf(false) }
    var ans by remember { mutableStateOf("") }
    var mois by remember { mutableStateOf("") }
    var jours by remember { mutableStateOf("") }
    TitreOutil("Date \u00b1 une dur\u00e9e", "Ex. 12 mars 1842 + 73 ans, 4 mois, 12 jours.")
    OutlinedTextField(
        value = date,
        onValueChange = { date = it },
        label = { Text("Date de d\u00e9part") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Row {
        FilterChip(selected = !retirer, onClick = { retirer = false }, label = { Text("+ ajouter") })
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = retirer, onClick = { retirer = true }, label = { Text("\u2212 retirer") })
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = ans,
            onValueChange = { ans = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text("Ans") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = mois,
            onValueChange = { mois = it.filter { c -> c.isDigit() }.take(5) },
            label = { Text("Mois") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = jours,
            onValueChange = { jours = it.filter { c -> c.isDigit() }.take(6) },
            label = { Text("Jours") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))
    if (date.isBlank()) return
    val d = lireDate(date)
    if (d == null) {
        Text("Date non reconnue.", color = MaterialTheme.colorScheme.error)
        return
    }
    val na = ans.toIntOrNull() ?: 0
    val nm = mois.toIntOrNull() ?: 0
    val nj = jours.toIntOrNull() ?: 0
    if (na == 0 && nm == 0 && nj == 0) {
        Text("Indiquez une dur\u00e9e.")
        return
    }
    val res = ajouterDuree(d, if (retirer) -1 else 1, na, nm, nj)
    if (res == null) {
        Text("Date hors limites.", color = MaterialTheme.colorScheme.error)
        return
    }
    val dureeTxt = libelleDuree(Duree(na, nm, nj, null, "jour", false))
    Text(
        jourSemaineDe(res) + " " + dateLongue(res),
        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall
    )
    val lignes = lignesInfoJour(infoJour(res))
    lignes.forEach { Text(it, Modifier.padding(vertical = 2.dp)) }
    val texte = dateCourte(d) + (if (retirer) " \u2212 " else " + ") + dureeTxt + " = " +
        dateCourte(res) + " (" + jourSemaineDe(res) + ")\n" + lignes.joinToString("\n")
    BoutonNote(texte, 0L, demanderNote)
}

// ---------------------------------------------------------------- Outil 3
@Composable
private fun OutilParente(personnes: List<Personne>, demanderNote: (String, Long) -> Unit) {
    var idX by remember { mutableStateOf(0L) }
    var idY by remember { mutableStateOf(0L) }
    var choixX by remember { mutableStateOf(false) }
    var choixY by remember { mutableStateOf(false) }
    TitreOutil(
        "Calculateur de parent\u00e9",
        "Choisissez deux personnes : le lien est calcul\u00e9 \u00e0 partir des parents et conjoints saisis."
    )
    val px = personneParId(personnes, idX)
    val py = personneParId(personnes, idY)
    OutlinedButton(onClick = { choixX = true }, Modifier.fillMaxWidth()) {
        Text("Personne 1 : " + (if (px != null) px.titre() else "choisir..."))
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { choixY = true }, Modifier.fillMaxWidth()) {
        Text("Personne 2 : " + (if (py != null) py.titre() else "choisir..."))
    }
    Spacer(Modifier.height(12.dp))
    if (choixX) {
        DialogueChoixPersonne("Personne 1", personnes, { idX = it.id; choixX = false }, { choixX = false })
    }
    if (choixY) {
        DialogueChoixPersonne("Personne 2", personnes, { idY = it.id; choixY = false }, { choixY = false })
    }
    if (px == null || py == null) return
    val noeuds = remember(personnes.size, idX, idY) {
        personnes.associate { p -> p.id to NoeudParente(p.id, p.pere, p.mere, p.sexe, p.tousConjoints()) }
    }
    val r = calculerParente(px.id, py.id, noeuds) { id -> personneParId(personnes, id)?.titre() ?: "?" }
    Text(r.phrase, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    if (r.phraseInverse.isNotEmpty()) Text(r.phraseInverse)
    if (r.trouve && !r.alliance && r.degreCivil > 0) {
        Text("Degr\u00e9 de parent\u00e9 (code civil) : " + r.degreCivil.toString())
    }
    if (r.ancetresCommuns.isNotEmpty()) {
        Text(
            (if (r.ancetresCommuns.size > 1) "Anc\u00eatres communs : " else "Anc\u00eatre commun : ") +
                r.ancetresCommuns.joinToString(", ") { id -> personneParId(personnes, id)?.titre() ?: "?" }
        )
    }
    if (r.remarque.isNotEmpty()) {
        Text(r.remarque, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (r.trouve) {
        var texte = "Parent\u00e9 : " + r.phrase
        if (r.phraseInverse.isNotEmpty()) texte += " ; " + r.phraseInverse
        if (!r.alliance && r.degreCivil > 0) texte += " (degr\u00e9 civil " + r.degreCivil.toString() + ")"
        BoutonNote(texte, px.id, demanderNote)
    }
}

// ---------------------------------------------------------------- Dialogues
@Composable
private fun DialogueChoixPersonne(
    titre: String,
    personnes: List<Personne>,
    onChoisi: (Personne) -> Unit,
    onFermer: () -> Unit
) {
    var filtre by remember { mutableStateOf("") }
    val f = versTexteRecherche(filtre.trim())
    val liste = personnes
        .filter { f.isEmpty() || versTexteRecherche(it.titre()).contains(f) }
        .sortedBy { versTexteRecherche(it.titre()) }
        .take(40)
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(titre) },
        text = {
            Column {
                OutlinedTextField(
                    value = filtre,
                    onValueChange = { filtre = it },
                    label = { Text("Rechercher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    liste.forEach { p ->
                        Text(
                            nomListe(p),
                            Modifier.fillMaxWidth().clickable { onChoisi(p) }.padding(vertical = 10.dp)
                        )
                    }
                    if (liste.isEmpty()) Text("Aucune fiche.")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
}

@Composable
private fun DialogueNoteOutil(
    texteInitial: String,
    personnes: List<Personne>,
    defautId: Long,
    onAjouter: (Long, String) -> Unit,
    onFermer: () -> Unit
) {
    var texte by remember { mutableStateOf(texteInitial) }
    var cible by remember { mutableStateOf(defautId) }
    var choix by remember { mutableStateOf(false) }
    val p = personneParId(personnes, cible)
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Note de recherche") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Le texte sera ajout\u00e9 \u00e0 la fin de la note libre de la fiche choisie.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { choix = true }, Modifier.fillMaxWidth()) {
                    Text("Fiche : " + (if (p != null) p.titre() else "choisir..."))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (p != null && texte.isNotBlank()) onAjouter(p.id, texte.trim()) },
                enabled = p != null && texte.isNotBlank()
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
    if (choix) {
        DialogueChoixPersonne("Choisir la fiche", personnes, { cible = it.id; choix = false }, { choix = false })
    }
}
