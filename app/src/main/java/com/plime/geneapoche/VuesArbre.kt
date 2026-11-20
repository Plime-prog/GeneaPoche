package com.plime.geneapoche

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- v0.145
// Deux vues de plus dans le menu Arbre : le SABLIER (ancetres au-dessus,
// descendants en dessous) et le CHEMIN DE PARENTE entre deux personnes.
// Meme comportement que l'arbre : toucher = centrer, appui long = fiche.

// Enfants d'une personne : liste de la fiche + fiches qui la designent comme
// pere ou mere, sans doublon, par annee de naissance.
fun enfantsDe(p: Personne, personnes: List<Personne>): List<Personne> {
    val ids = LinkedHashSet<Long>()
    p.enfants.forEach { ids.add(it) }
    personnes.forEach { q -> if (q.id != p.id && (q.pere == p.id || q.mere == p.id)) ids.add(q.id) }
    return ids.mapNotNull { personneParId(personnes, it) }
        .let { enfantsChrono(it) }
}

// Descendants sur 2 generations : chaque enfant avec ses propres enfants.
fun descendantsSablier(centre: Personne, personnes: List<Personne>): List<Pair<Personne, List<Personne>>> =
    enfantsDe(centre, personnes).map { e -> Pair(e, enfantsDe(e, personnes)) }

@Composable
fun EcranSablier(
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
    val anneaux = remember(centre, personnes) { ancetresEventail(centre, personnes) }
    val enfants = remember(centre, personnes) { descendantsSablier(centre, personnes) }
    val conjoints = remember(centre, personnes) { centre.tousConjoints().mapNotNull { personneParId(personnes, it) } }
    val largeurCarte = 86.dp
    val maxCartes = maxOf(8, enfants.size, enfants.maxOfOrNull { it.second.size } ?: 0, 1 + conjoints.size)
    val total = largeurCarte * maxCartes + 16.dp

    @Composable
    fun etiquette(t: String) {
        Text(
            t, Modifier.padding(top = 8.dp, bottom = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    @Composable
    fun rangee(l: List<Personne?>) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            l.forEach { q -> CarteMiniArbre(q, onOuvrir, onCentrerSur, modifier = Modifier.width(largeurCarte)) }
        }
    }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
        Column(Modifier.width(total).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            etiquette("Arrière-grands-parents")
            rangee(anneaux[2])
            etiquette("Grands-parents")
            rangee(anneaux[1])
            etiquette("Parents")
            rangee(anneaux[0])
            etiquette(if (conjoints.isEmpty()) "Personne" else "Personne et conjoint(s)")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CarteMiniArbre(centre, onOuvrir, onCentrerSur, centre = true, modifier = Modifier.width(largeurCarte))
                conjoints.forEach { c -> CarteMiniArbre(c, onOuvrir, onCentrerSur, modifier = Modifier.width(largeurCarte)) }
            }
            if (enfants.isNotEmpty()) {
                etiquette("Enfants (" + enfants.size.toString() + ")")
                rangee(enfants.map { it.first })
                enfants.filter { it.second.isNotEmpty() }.forEach { (e, pe) ->
                    etiquette("Enfants de " + e.prenoms.trim().substringBefore(" ").ifEmpty { e.nom })
                    rangee(pe)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Sablier : 3 générations d'ancêtres au-dessus, 2 générations de descendants en dessous. Touchez une carte : elle devient le centre. Appui long : sa fiche. Faites glisser pour défiler.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun EcranParenteVue(
    personnes: List<Personne>,
    idCentre: Long,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit
) {
    val x = personneParId(personnes, idCentre)
    var idY by rememberMemo("parente_y", 0L)
    var choix by remember { mutableStateOf(false) }
    val y = personneParId(personnes, idY)
    if (x == null) {
        Text("Aucune personne : ajoutez la première personne depuis l'onglet Personnes.", Modifier.padding(12.dp))
        return
    }
    if (choix) {
        DialogueChoixPersonne("Relier à quelle personne ?", personnes, { idY = it.id; choix = false }, { choix = false })
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Chemin de parenté", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            "Départ : " + x.titre() + " (la personne au centre de l'arbre).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { choix = true }, Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(if (y == null) "Choisir la personne à relier" else "Arrivée : " + y.titre())
        }
        if (y != null) {
            val noeuds = remember(personnes, x.id, y.id) {
                personnes.associate { p -> p.id to NoeudParente(p.id, p.pere, p.mere, p.sexe, p.tousConjoints()) }
            }
            val r = remember(personnes, x.id, y.id) {
                calculerParente(x.id, y.id, noeuds) { id -> personneParId(personnes, id)?.titre() ?: "?" }
            }
            Text(r.phrase, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            if (r.phraseInverse.isNotEmpty()) Text(r.phraseInverse, style = MaterialTheme.typography.bodyMedium)
            if (r.trouve && !r.alliance && r.degreCivil > 0) {
                Text("Degré de parenté (code civil) : " + r.degreCivil.toString(), style = MaterialTheme.typography.bodySmall)
            }
            val chemin = remember(personnes, x.id, y.id) { cheminParente(x.id, y.id, personnes) }
            if (chemin.size >= 2) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    chemin.forEachIndexed { i, e ->
                        val q = personneParId(personnes, e.personneId)
                        if (i > 0) {
                            val prec = personneParId(personnes, chemin[i - 1].personneId)
                            val f = prec?.sexe == "F"
                            val m = prec?.sexe == "M"
                            val lien = when (e.lien) {
                                "parent" -> if (f) "↑ fille de" else if (m) "↑ fils de" else "↑ enfant de"
                                "enfant" -> if (f) "↓ mère de" else if (m) "↓ père de" else "↓ parent de"
                                else -> if (f) "⚭ épouse de" else if (m) "⚭ époux de" else "⚭ conjoint de"
                            }
                            Text(lien, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                        CarteMiniArbre(
                            q, onOuvrir, onCentrerSur,
                            centre = i == 0 || i == chemin.size - 1,
                            modifier = Modifier.width(170.dp)
                        )
                        if (e.personneId in r.ancetresCommuns) {
                            Text("ancêtre commun", fontSize = 11.sp, color = CouleurRouge)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Seuls les liens saisis (parents, conjoints) sont suivis. Touchez une carte : elle devient le départ. Appui long : sa fiche.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
