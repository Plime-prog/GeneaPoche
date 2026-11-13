package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Outils historiques (v0.101)
// Calendriers historiques, "Que s'est-il passe cette annee ?", chronologie
// d'une famille (plusieurs personnes juxtaposees) et arbre du chemin de
// parente. Les donnees sont embarquees (Histoire.kt) : 100 % hors ligne.

@Composable
private fun TitreOutilH(titre: String, aide: String) {
    Text(titre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Text(aide, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CarteH(titre: String, contenu: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurGrisBord)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text(titre, fontWeight = FontWeight.Bold, color = CouleurVertFonce, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            contenu()
        }
    }
}

private fun couleurGenre(g: String): Color = when (g) {
    "regime" -> Color(0xFF6A1B9A)
    "guerre" -> Color(0xFFB71C1C)
    "loi" -> Color(0xFF1565C0)
    else -> Color(0xFF2E7D32)
}

private fun dateEvenement(e: EvenementHistorique): String =
    (if (e.jour > 0) e.jour.toString().padStart(2, '0') + "/" else "") +
        (if (e.mois > 0) e.mois.toString().padStart(2, '0') + "/" else "") + e.annee.toString()

// ---------------------------------------------------------------- 1. Calendriers historiques
@Composable
fun OutilHistoire(personnes: List<Personne>, demanderNote: (String, Long) -> Unit) {
    var filtre by remember { mutableStateOf("") }
    var choix by remember { mutableStateOf(false) }
    var idP by remember { mutableStateOf(0L) }
    TitreOutilH(
        "Calendriers historiques",
        "Rep\u00E8res de l'Histoire embarqu\u00E9s (hors ligne), et rapprochement avec les dates de vos anc\u00EAtres."
    )
    // Rapprochement pour une personne.
    val p = personneParId(personnes, idP)
    OutlinedButton(onClick = { choix = true }, Modifier.fillMaxWidth()) {
        Text("Personne : " + (if (p != null) p.titre() else "choisir..."))
    }
    if (choix) {
        DialogueChoixPersonne("Personne", personnes, { idP = it.id; choix = false }, { choix = false })
    }
    if (p != null) {
        val phrases = phrasesHistoriquesPersonne(p)
        Spacer(Modifier.height(6.dp))
        if (phrases.isEmpty()) {
            Text("Aucune date compl\u00E8te \u00E0 rapprocher (naissance, mariage, d\u00E9c\u00E8s).", style = MaterialTheme.typography.bodySmall)
        } else {
            phrases.forEach { Text(it, Modifier.padding(vertical = 2.dp), fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(6.dp))
            Button(onClick = { demanderNote(phrases.joinToString("\n"), p.id) }, Modifier.fillMaxWidth()) {
                Text("Ajouter ce r\u00E9sultat \u00E0 la note de recherche")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = filtre, onValueChange = { filtre = it },
        label = { Text("Filtrer (ann\u00E9e ou mot)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(6.dp))
    val f = sansAccents(filtre.trim()).lowercase()
    val liste = EvenementsHistoriques.filter {
        f.isEmpty() || it.annee.toString().contains(f) || sansAccents(it.titre).lowercase().contains(f)
    }
    if (f.isEmpty()) {
        Text("R\u00E9gimes politiques", fontWeight = FontWeight.Bold, color = CouleurVertFonce)
        PeriodesHistoriques.forEach { per ->
            Text(
                per.debut.year.toString() + " \u2013 " + (if (per.fin.year >= 2100) "aujourd'hui" else per.fin.year.toString()) + "  " + per.nom,
                fontSize = 13.sp, modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
    Text("\u00C9v\u00E9nements (" + liste.size.toString() + ")", fontWeight = FontWeight.Bold, color = CouleurVertFonce)
    liste.forEach { e ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
            Text("\u25CF ", color = couleurGenre(e.genre), fontSize = 12.sp)
            Text(dateEvenement(e) + "  ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                e.titre + (if (e.depts.isNotEmpty()) "  (local : " + e.depts.joinToString(", ") + ")" else ""),
                fontSize = 13.sp, modifier = Modifier.weight(1f)
            )
        }
    }
    if (liste.isEmpty()) Text("Aucun \u00E9v\u00E9nement.", style = MaterialTheme.typography.bodySmall)
    Text(
        "Base volontairement courte, \u00E0 enrichir.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

// ---------------------------------------------------------------- 3. Que s'est-il passe cette annee ?
@Composable
fun OutilAnnee(personnes: List<Personne>, onOuvrir: (Long) -> Unit, demanderNote: (String, Long) -> Unit) {
    var texte by remember { mutableStateOf("1850") }
    val annee = texte.trim().toIntOrNull()
    TitreOutilH(
        "Que s'est-il pass\u00E9 cette ann\u00E9e ?",
        "Pour une ann\u00E9e : naissances, mariages, d\u00E9c\u00E8s, \u00E2ge des anc\u00EAtres, histoire et changements de r\u00E9gime."
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { if (annee != null) texte = (annee - 1).toString() }) { Text("\u25C0") }
        OutlinedTextField(
            value = texte, onValueChange = { texte = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text("Ann\u00E9e") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { if (annee != null) texte = (annee + 1).toString() }) { Text("\u25B6") }
    }
    if (annee == null) return
    Spacer(Modifier.height(6.dp))
    val depts = remember(personnes.size) { personnes.map { deptDePersonne(it) }.filter { it.isNotEmpty() }.toSet() }
    val histoire = EvenementsHistoriques.filter {
        it.annee == annee && (it.depts.isEmpty() || it.depts.any { d -> d in depts })
    }
    val regimes = regimesDeLAnnee(annee)
    val lignes = remember(personnes.size, annee) { ligneAnnee(personnes, annee) }
    if (histoire.isNotEmpty() || regimes.isNotEmpty()) {
        CarteH("Histoire") {
            regimes.forEach { Text("\u25CF Changement de r\u00E9gime : " + it.nom + " (" + dateCourte(it.debut) + ")", fontSize = 13.sp, color = couleurGenre("regime")) }
            histoire.forEach { e ->
                Text("\u25CF " + dateEvenement(e) + "  " + e.titre + (if (e.depts.isNotEmpty()) " (local)" else ""), fontSize = 13.sp, color = couleurGenre(e.genre))
            }
        }
    } else {
        Text("Aucun rep\u00E8re historique embarqu\u00E9 pour " + annee.toString() + ".", style = MaterialTheme.typography.bodySmall)
    }
    val sections = listOf(
        "N" to "Naissances", "M" to "Mariages", "D" to "D\u00E9c\u00E8s", "E" to "\u00C9v\u00E9nements", "A" to "\u00C2ge des anc\u00EAtres"
    )
    var note = "Ann\u00E9e " + annee.toString() + " :"
    sections.forEach { (g, titre) ->
        val l = lignes.filter { it.genre == g }
        if (l.isNotEmpty()) {
            CarteH(titre + " (" + l.size.toString() + ")") {
                l.take(40).forEach { ln ->
                    Text(ln.texte, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { onOuvrir(ln.personneId) }.padding(vertical = 3.dp))
                }
                if (l.size > 40) Text("\u2026 et " + (l.size - 40).toString() + " autres", fontSize = 12.sp)
            }
            note += "\n" + titre + " : " + l.size.toString()
        }
    }
    if (lignes.isEmpty()) Text("Rien dans votre arbre pour cette ann\u00E9e.", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(6.dp))
    Button(
        onClick = { demanderNote(note + (if (histoire.isNotEmpty()) "\n" + histoire.joinToString("\n") { it.titre } else ""), 0L) },
        Modifier.fillMaxWidth()
    ) { Text("Ajouter ce r\u00E9sultat \u00E0 la note de recherche") }
}

// ---------------------------------------------------------------- 4. Chronologie d'une famille
@Composable
fun OutilFamille(personnes: List<Personne>, demanderNote: (String, Long) -> Unit) {
    var ids by remember { mutableStateOf(listOf(0L, 0L, 0L)) }
    var choix by remember { mutableStateOf(-1) }
    TitreOutilH(
        "Chronologie d'une famille",
        "Choisissez une personne (puis jusqu'\u00E0 deux autres) : leurs \u00E9v\u00E9nements sont juxtapos\u00E9s par ann\u00E9e, les incoh\u00E9rences apparaissent en rouge."
    )
    for (i in 0..2) {
        val p = personneParId(personnes, ids[i])
        OutlinedButton(onClick = { choix = i }, Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text("Personne " + (i + 1).toString() + (if (i > 0) " (facultative)" else "") + " : " + (if (p != null) p.titre() else "choisir..."))
        }
    }
    if (choix >= 0) {
        DialogueChoixPersonne(
            "Personne " + (choix + 1).toString(), personnes,
            { sel -> ids = ids.toMutableList().also { it[choix] = sel.id }; choix = -1 },
            { choix = -1 }
        )
    }
    val choisies = ids.mapNotNull { personneParId(personnes, it) }
    if (choisies.isEmpty()) return
    val chronos = choisies.map { chronologieFamille(it, personnes) }
    val annees = chronos.flatMap { c -> c.map { it.annee } }.toSortedSet().toList()
    Spacer(Modifier.height(8.dp))
    // En-tetes
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(44.dp))
        choisies.forEach { p ->
            Text(p.titre(), Modifier.weight(1f).padding(horizontal = 2.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CouleurVertFonce)
        }
    }
    var nbAlertes = 0
    var note = "Chronologie : " + choisies.joinToString(" / ") { it.titre() }
    annees.forEachIndexed { idx, a ->
        if (idx > 0) {
            Row(Modifier.fillMaxWidth()) {
                Text("\u2502", Modifier.width(44.dp), color = Color(0xFFBDBDBD), fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(a.toString(), Modifier.width(44.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            chronos.forEach { c ->
                Column(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                    c.filter { it.annee == a }.forEach { e ->
                        val rouge = e.alerte.isNotEmpty()
                        if (rouge) nbAlertes++
                        Text(
                            (if (rouge) "\u26A0 " else "") + e.libelle + (if (rouge) " (" + e.alerte + ")" else ""),
                            fontSize = 12.sp,
                            color = if (rouge) CouleurRouge else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
    chronos.forEachIndexed { i, c ->
        note += "\n" + choisies[i].titre() + " :"
        c.forEach { e -> note += "\n  " + e.annee.toString() + " " + e.libelle + (if (e.alerte.isNotEmpty()) " [" + e.alerte + "]" else "") }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        if (nbAlertes == 0) "Aucune incoh\u00E9rence \u00E9vidente d\u00E9tect\u00E9e." else nbAlertes.toString() + " incoh\u00E9rence(s) \u00E0 v\u00E9rifier.",
        fontWeight = FontWeight.Bold, color = if (nbAlertes == 0) CouleurVertFonce else CouleurRouge
    )
    Spacer(Modifier.height(6.dp))
    Button(onClick = { demanderNote(note, choisies[0].id) }, Modifier.fillMaxWidth()) {
        Text("Ajouter ce r\u00E9sultat \u00E0 la note de recherche")
    }
}

// ---------------------------------------------------------------- 2. Arbre du chemin de parente
// Affiche le chemin entre deux personnes : les anc\u00EAtres au bord gauche,
// les descendants de plus en plus d\u00E9cal\u00E9s vers la droite.
@Composable
fun CheminParenteArbre(personnes: List<Personne>, x: Long, y: Long, communs: List<Long>) {
    val chemin = remember(personnes.size, x, y) { cheminParente(x, y, personnes) }
    if (chemin.size < 2) return
    // Niveau cumule : +1 en montant vers un parent, -1 en descendant.
    val niveaux = ArrayList<Int>()
    var n = 0
    chemin.forEachIndexed { i, e ->
        if (i > 0) {
            if (e.lien == "parent") n++ else if (e.lien == "enfant") n--
        }
        niveaux.add(n)
    }
    val maxN = niveaux.maxOrNull() ?: 0
    CarteH("Chemin de parent\u00E9") {
        chemin.forEachIndexed { i, e ->
            val p = personneParId(personnes, e.personneId)
            val decalage = ((maxN - niveaux[i]) * 18).dp
            if (i > 0) {
                val prec = personneParId(personnes, chemin[i - 1].personneId)
                val lien = when (e.lien) {
                    "parent" -> if (prec?.sexe == "F") "\u2191 fille de" else if (prec?.sexe == "M") "\u2191 fils de" else "\u2191 enfant de"
                    "enfant" -> if (prec?.sexe == "F") "\u2193 m\u00E8re de" else if (prec?.sexe == "M") "\u2193 p\u00E8re de" else "\u2193 parent de"
                    else -> if (prec?.sexe == "F") "\u26AD \u00E9pouse de" else if (prec?.sexe == "M") "\u26AD \u00E9poux de" else "\u26AD conjoint de"
                }
                val decPrec = ((maxN - niveaux[i - 1]) * 18).dp
                Row(Modifier.fillMaxWidth().padding(start = if (decPrec < decalage) decPrec else decalage)) {
                    Text("\u2502 ", color = Color(0xFFBDBDBD))
                    Text(lien, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = decalage), verticalAlignment = Alignment.CenterVertically) {
                Text("\u25CF ", color = if (e.personneId in communs) CouleurRouge else CouleurVertFonce, fontSize = 12.sp)
                Text(p?.titre() ?: "?", fontSize = 13.sp, fontWeight = if (i == 0 || i == chemin.size - 1) FontWeight.Bold else FontWeight.Normal)
                if (e.personneId in communs) Text("  (anc\u00EAtre commun)", fontSize = 11.sp, color = CouleurRouge)
            }
        }
    }
}
