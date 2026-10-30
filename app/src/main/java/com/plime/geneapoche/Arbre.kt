package com.plime.geneapoche

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Arbre genealogique
// Vue centree sur une personne, RELIEE et CENTREE facon genealogique :
// grands-parents, parents, personne et conjoint centres, enfants centres
// sous le couple. Chaque rangee est une grille reguliere, les liaisons sont
// tracees entre les cartes (verticales + barres horizontales).
// Toucher une carte ouvre la fiche et recentre l'arbre sur elle.

val CouleurLiaison = Color(0xFF4C7A50)

// Trace les liaisons entre deux rangees : verticales depuis la rangee du
// haut, une barre horizontale au milieu, verticales vers la rangee du bas.
// Les positions sont des fractions de la largeur (grille reguliere).
@Composable
fun LiaisonsArbre(
    verticalesHaut: List<Float>,
    barres: List<Pair<Float, Float>>,
    verticalesBas: List<Float>
) {
    Canvas(Modifier.fillMaxWidth().height(18.dp)) {
        val ep = 2.dp.toPx()
        val m = size.height / 2f
        verticalesHaut.forEach { f ->
            drawLine(CouleurLiaison, Offset(size.width * f, 0f), Offset(size.width * f, m), ep)
        }
        barres.forEach { p ->
            drawLine(CouleurLiaison, Offset(size.width * p.first, m), Offset(size.width * p.second, m), ep)
        }
        verticalesBas.forEach { f ->
            drawLine(CouleurLiaison, Offset(size.width * f, m), Offset(size.width * f, size.height), ep)
        }
    }
}

@Composable
fun EcranArbre(
    personnes: List<Personne>,
    idCentre: Long,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit
) {
    val centre = personneParId(personnes, idCentre)
    var pickerOuvert by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)
    ) {
        Text("Arbre", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (centre == null) {
            Spacer(Modifier.height(12.dp))
            Text("Aucune personne : ajoutez la premiu00E8re personne (de cujus, Sosa 1) depuis l'onglet Personnes.")
        } else {
            OutlinedButton(onClick = { pickerOuvert = true }, Modifier.padding(top = 8.dp)) {
                Text("Centru00E9 sur : " + centre.titre())
            }
            val pere = personneParId(personnes, centre.pere)
            val mere = personneParId(personnes, centre.mere)
            val conjoint = personneParId(personnes, centre.conjoint)

            // --- Grands-parents : 4 cases egales (centres a 1/8, 3/8, 5/8, 7/8).
            Text(
                "Grands-parents",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth()) {
                CarteMiniArbre(personneParId(personnes, pere?.pere ?: 0L), "Au00EEeul pat.", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(personneParId(personnes, pere?.mere ?: 0L), "Au00EEeule pat.", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(personneParId(personnes, mere?.pere ?: 0L), "Au00EEeul mat.", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(personneParId(personnes, mere?.mere ?: 0L), "Au00EEeule mat.", onOuvrir, modifier = Modifier.weight(1f))
            }
            // Aieux -> parents : union de chaque couple, descente au parent.
            LiaisonsArbre(
                listOf(0.125f, 0.375f, 0.625f, 0.875f),
                listOf(0.125f to 0.375f, 0.625f to 0.875f),
                listOf(0.25f, 0.75f)
            )

            // --- Parents : 2 cases egales (centres a 1/4 et 3/4).
            Row(Modifier.fillMaxWidth()) {
                CarteMiniArbre(pere, "Pu00E8re", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(mere, "Mu00E8re", onOuvrir, modifier = Modifier.weight(1f))
            }
            // Parents -> couple central : barre d'union au centre (1/2).
            LiaisonsArbre(
                listOf(0.25f, 0.75f),
                listOf(0.25f to 0.75f),
                listOf(0.5f)
            )

            // --- Personne et conjoint : le couple est CENTRE, uni par une barre.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarteMiniArbre(centre, "", onOuvrir, centre = true, modifier = Modifier.width(150.dp))
                Box(Modifier.width(14.dp).height(2.dp).background(CouleurLiaison))
                CarteMiniArbre(conjoint, "Conjoint(e)", onOuvrir, modifier = Modifier.width(150.dp))
            }

            // --- Enfants : centru00E9s sous le couple, relies par leurs centres.
            val nb = centre.enfants.size
            Text(
                "Enfants",
                Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (nb == 0) {
                Text("Aucun enfant renseignu00E9.", style = MaterialTheme.typography.bodySmall)
            } else {
                val centres = (0 until nb).map { i -> (2 * i + 1).toFloat() / (2 * nb) }
                LiaisonsArbre(
                    listOf(0.5f),
                    listOf(centres.first() to centres.last()),
                    centres
                )
                Row(Modifier.fillMaxWidth()) {
                    centre.enfants.forEach { idEnfant ->
                        CarteMiniArbre(personneParId(personnes, idEnfant), "Enfant", onOuvrir, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Touchez une carte pour ouvrir la fiche : l'arbre se recentre sur cette personne.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickerOuvert) {
        AlertDialog(
            onDismissRequest = { pickerOuvert = false },
            title = { Text("Centrer l'arbre sur...") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    personnes.sortedBy { it.titre() }.forEach { c ->
                        TextButton(onClick = { onCentrerSur(c.id); pickerOuvert = false }) {
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

@Composable
fun CarteMiniArbre(
    p: Personne?,
    libelle: String,
    onOuvrir: (Long) -> Unit,
    centre: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier
            .padding(2.dp)
            .clickable { if (p != null) onOuvrir(p.id) },
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF9CBF9A)),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (centre) MaterialTheme.colorScheme.primaryContainer
            else if (p == null) MaterialTheme.colorScheme.surfaceVariant
            else when (p.sexe) {
                "F" -> androidx.compose.ui.graphics.Color(0xFFF8DDE7) // rose pale
                "M" -> androidx.compose.ui.graphics.Color(0xFFDDE9F6) // bleu pale
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp)) {
            if (libelle.isNotEmpty()) {
                Text(
                    libelle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (p == null) {
                Text(
                    "Inconnu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(p.titre(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                val num = p.numeroAffiche()
                if (num.isNotEmpty()) {
                    Text(
                        num,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                val n = dateAffichee(p.dateNaissance.trim())
                val d = dateAffichee(p.dateDeces.trim())
                val morceaux = mutableListOf<String>()
                if (n.isNotEmpty()) morceaux.add("N. " + n)
                // R\u00E8gle des 100 ans : d\u00E9c\u00E8s affich\u00E9 seulement si connu ou possible.
                if (d.isNotEmpty() && decesAffichable(p)) morceaux.add("D. " + d)
                if (morceaux.isNotEmpty()) {
                    Text(morceaux.joinToString(" \u2013 "), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
