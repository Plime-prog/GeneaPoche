package com.plime.geneapoche

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- v0.146
// Deux vues de plus dans le menu Arbre : la DESCENDANCE (liste depliable de tous
// les descendants) et la COMPLETUDE des ascendants (jauges par generation).

class LigneDescendance(val p: Personne, val niveau: Int, val nbEnfants: Int, val deplie: Boolean)

// Lignes visibles de la descendance : une personne n'est listee qu'une fois
// (garde-fou contre les boucles) ; ses enfants ne le sont que si elle est depliee.
fun lignesDescendance(centre: Personne, personnes: List<Personne>, deplies: Set<Long>, profMax: Int = 15): List<LigneDescendance> {
    val res = mutableListOf<LigneDescendance>()
    val vus = HashSet<Long>()
    fun visiter(p: Personne, niveau: Int) {
        if (!vus.add(p.id)) return
        val enf = enfantsDe(p, personnes)
        val dep = p.id in deplies
        res.add(LigneDescendance(p, niveau, enf.size, dep))
        if (dep && niveau < profMax) enf.forEach { visiter(it, niveau + 1) }
    }
    visiter(centre, 0)
    return res
}

// Identifiants de tous les descendants (sans la personne elle-meme).
fun tousDescendants(p: Personne, personnes: List<Personne>): Set<Long> {
    val vus = LinkedHashSet<Long>()
    val file = ArrayDeque<Personne>()
    file.add(p)
    while (file.isNotEmpty()) {
        val q = file.removeFirst()
        enfantsDe(q, personnes).forEach { e -> if (e.id != p.id && vus.add(e.id)) file.add(e) }
    }
    return vus
}

class GenerationComplete(val generation: Int, val connus: Int, val possibles: Int)

// Pour chaque generation d'ascendants (1 = parents) : combien de places sont
// occupees sur 2^g possibles (un ancetre present plusieurs fois compte autant de fois).
fun completudeAscendance(centre: Personne, personnes: List<Personne>, generations: Int = 8): List<GenerationComplete> {
    val res = mutableListOf<GenerationComplete>()
    var courant: List<Personne?> = listOf(centre)
    for (g in 1..generations) {
        courant = courant.flatMap { q ->
            listOf(q?.let { personneParId(personnes, it.pere) }, q?.let { personneParId(personnes, it.mere) })
        }
        res.add(GenerationComplete(g, courant.count { it != null }, courant.size))
    }
    return res
}

// Parents a rechercher : pour chaque personne connue sur les premieres
// generations (en commencant par la personne elle-meme), "pere" ou "mere"
// manquants, dans l'ordre des generations.
fun ancetresManquants(centre: Personne, personnes: List<Personne>, generations: Int = 5): List<Pair<Personne, String>> {
    val res = mutableListOf<Pair<Personne, String>>()
    var courant: List<Personne> = listOf(centre)
    val vus = HashSet<Long>()
    for (g in 0 until generations) {
        val suivant = mutableListOf<Personne>()
        courant.forEach { q ->
            if (!vus.add(q.id)) return@forEach
            val pe = personneParId(personnes, q.pere)
            val me = personneParId(personnes, q.mere)
            if (pe == null) res.add(Pair(q, "père")) else suivant.add(pe)
            if (me == null) res.add(Pair(q, "mère")) else suivant.add(me)
        }
        courant = suivant
    }
    return res
}

fun nomGeneration(g: Int): String = when (g) {
    1 -> "Parents"
    2 -> "Grands-parents"
    3 -> "Arrière-grands-parents"
    4 -> "Arrière-arrière-grands-parents"
    else -> "Génération " + g.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EcranDescendance(
    personnes: List<Personne>,
    idCentre: Long,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit
) {
    val centre = personneParId(personnes, idCentre)
    if (centre == null) {
        Text("Aucune personne : ajoutez la première personne depuis l'onglet Personnes.", Modifier.padding(12.dp))
        return
    }
    var deplies by rememberMemo("desc_deplies", setOf<Long>())
    val tous = remember(centre, personnes) { tousDescendants(centre, personnes) }
    val ouverts = deplies + centre.id
    val lignes = remember(centre, personnes, ouverts) { lignesDescendance(centre, personnes, ouverts) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Descendance de " + centre.titre(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            if (tous.isEmpty()) "Aucun descendant connu." else tous.size.toString() + " descendant" + (if (tous.size > 1) "s" else "") + " connu" + (if (tous.size > 1) "s" else "") + ".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { deplies = tous + centre.id }) { Text("Tout déplier") }
            OutlinedButton(onClick = { deplies = emptySet() }) { Text("Tout replier") }
        }
        lignes.forEach { l ->
            Row(
                Modifier.fillMaxWidth().padding(start = (l.niveau * 18).dp).padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (l.nbEnfants == 0) "•" else if (l.deplie) "▼" else "▶",
                    Modifier.width(26.dp).clickable(enabled = l.nbEnfants > 0 && l.p.id != centre.id) {
                        deplies = if (l.deplie) deplies - l.p.id else deplies + l.p.id
                    },
                    color = CouleurVertFonce
                )
                Column(
                    Modifier.weight(1f).combinedClickable(
                        onClick = { onCentrerSur(l.p.id) },
                        onLongClick = { onOuvrir(l.p.id) }
                    )
                ) {
                    Text(
                        l.p.titre() + (if (l.nbEnfants > 0) "  (" + l.nbEnfants.toString() + ")" else ""),
                        fontSize = 14.sp,
                        fontWeight = if (l.niveau == 0) FontWeight.Bold else FontWeight.Normal
                    )
                    val a = anneesCourtes(l.p)
                    if (a.isNotEmpty()) Text(a, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Touchez la flèche pour déplier ou replier une branche. Touchez un nom : la personne devient le centre. Appui long : sa fiche. Le nombre entre parenthèses est celui des enfants.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EcranCompletude(
    personnes: List<Personne>,
    idCentre: Long,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit
) {
    val centre = personneParId(personnes, idCentre)
    if (centre == null) {
        Text("Aucune personne : ajoutez la première personne depuis l'onglet Personnes.", Modifier.padding(12.dp))
        return
    }
    val gens = remember(centre, personnes) { completudeAscendance(centre, personnes) }
    val manquants = remember(centre, personnes) { ancetresManquants(centre, personnes) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Complétude des ancêtres de " + centre.titre(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        gens.forEach { g ->
            val part = if (g.possibles == 0) 0f else g.connus.toFloat() / g.possibles.toFloat()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(nomGeneration(g.generation), Modifier.weight(1f), fontSize = 13.sp)
                Text(g.connus.toString() + " / " + g.possibles.toString() + " (" + (part * 100).toInt().toString() + " %)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { part },
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
                color = if (part >= 0.75f) CouleurVertFonce else if (part >= 0.35f) MaterialTheme.colorScheme.tertiary else CouleurRouge
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("Parents à rechercher en priorité", fontWeight = FontWeight.Bold, color = CouleurVertFonce)
        if (manquants.isEmpty()) {
            Text("Aucun parent manquant sur les 5 premières générations : bravo !", fontSize = 13.sp)
        }
        manquants.take(12).forEach { (q, role) ->
            Text(
                "• " + role.replaceFirstChar { it.uppercase() } + " de " + q.titre(),
                Modifier.fillMaxWidth().clickable { onCentrerSur(q.id) }.padding(vertical = 5.dp),
                fontSize = 14.sp
            )
        }
        if (manquants.size > 12) Text("… et " + (manquants.size - 12).toString() + " autres.", fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Un ancêtre présent plusieurs fois (mariage entre cousins) compte autant de fois qu'il apparaît. Touchez une ligne : la personne devient le centre de l'arbre.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
