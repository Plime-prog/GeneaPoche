package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Onglet Biographie (v0.102)
// En-tete de synthese, famille cliquable (parents, conjoints, enfants),
// recit en phrases, puis frise melant vie, famille et histoire.

@Composable
private fun LienFamille(libelle: String, q: Personne, onOuvrir: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onOuvrir(q.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(libelle, Modifier.width(72.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PastilleNumero(p = q, diametre = 26)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(q.titre(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val nd = listOf(
                if (q.dateNaissance.isNotBlank()) "N. " + dateAffichee(q.dateNaissance.trim()) else "",
                if (q.dateDeces.isNotBlank()) "D. " + dateAffichee(q.dateDeces.trim()) else ""
            ).filter { it.isNotEmpty() }
            if (nd.isNotEmpty()) Text(nd.joinToString(" \u2013 "), fontSize = 12.sp)
        }
        Text("\u25B6", fontSize = 12.sp, color = CouleurVertFonce)
    }
}

@Composable
fun OngletBiographie(p: Personne, personnes: List<Personne>, onOuvrir: (Long) -> Unit) {
    var avecHistoire by remember { mutableStateOf(true) }
    val recit = remember(p, personnes) { recitBiographie(p, personnes) }
    val frise = remember(p, personnes, avecHistoire) { friseBiographie(p, personnes, avecHistoire) }
    val parents = listOf("P\u00E8re" to p.pere, "M\u00E8re" to p.mere)
        .mapNotNull { (l, id) -> personneParId(personnes, id)?.let { l to it } }
    val conjoints = p.tousConjoints().mapNotNull { personneParId(personnes, it) }
    val enfants = p.enfants.mapNotNull { personneParId(personnes, it) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item {
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CouleurGrisBord)
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text("Famille", fontWeight = FontWeight.Bold, color = CouleurVertFonce)
                    parents.forEach { (l, q) -> LienFamille(l, q, onOuvrir) }
                    conjoints.forEach { q -> LienFamille(if (q.sexe == "F") "\u00C9pouse" else "\u00C9poux", q, onOuvrir) }
                    enfants.forEach { q -> LienFamille(if (q.sexe == "F") "Fille" else if (q.sexe == "M") "Fils" else "Enfant", q, onOuvrir) }
                    if (parents.isEmpty() && conjoints.isEmpty() && enfants.isEmpty()) {
                        Text("Aucun lien familial renseign\u00E9.", fontSize = 13.sp, color = GrisManquant)
                    }
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CouleurGrisBord)
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text("Sa vie", fontWeight = FontWeight.Bold, color = CouleurVertFonce)
                    recit.forEach { Text(it, Modifier.padding(top = 4.dp), fontSize = 14.sp) }
                    phrasesHistoriquesPersonne(p).forEach {
                        Text(it, Modifier.padding(top = 4.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Chronologie", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = CouleurVertFonce)
                FilterChip(selected = avecHistoire, onClick = { avecHistoire = !avecHistoire }, label = { Text("Histoire") })
            }
        }
        items(frise.size) { i ->
            val l = frise[i]
            val couleur = when (l.genre) {
                "vie" -> CouleurVertFonce
                "famille" -> CouleurOrange
                "regime" -> CouleurRouge
                else -> CouleurGrisBord
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                Text(l.quand, Modifier.width(84.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.padding(top = 5.dp).size(10.dp).clip(CircleShape).background(couleur))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        l.texte, fontSize = 13.sp,
                        fontWeight = if (l.genre == "vie") FontWeight.Bold else FontWeight.Normal,
                        color = if (l.genre == "histoire" || l.genre == "regime") MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                    )
                    if (l.alerte.isNotEmpty()) Text("\u26A0 " + l.alerte, fontSize = 12.sp, color = CouleurRouge)
                }
            }
        }
        item { Spacer(Modifier.size(24.dp)) }
    }
}
