package com.plime.geneapoche

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Arbre simple

// Vue centree sur une personne : grands-parents, parents, conjoint, enfants.
// Toucher une carte ouvre la fiche et recentre l'arbre sur elle.

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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Arbre", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (centre == null) {
            Spacer(Modifier.height(12.dp))
            Text("Aucune personne : ajoutez la premi\u00E8re personne (de cujus, Sosa 1) depuis l'onglet Personnes.")
        } else {
            OutlinedButton(onClick = { pickerOuvert = true }, Modifier.padding(top = 8.dp)) {
                Text("Centr\u00E9 sur : " + centre.titre())
            }
            val pere = personneParId(personnes, centre.pere)
            val mere = personneParId(personnes, centre.mere)
            val conjoint = personneParId(personnes, centre.conjoint)

            Spacer(Modifier.height(10.dp))
            Text(
                "Grands-parents",
                Modifier.padding(top = 6.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                CarteMiniArbre(personneParId(personnes, pere?.pere ?: 0L), "A\u00EFeul pat.", onOuvrir)
                CarteMiniArbre(personneParId(personnes, pere?.mere ?: 0L), "A\u00EFeule pat.", onOuvrir)
                CarteMiniArbre(personneParId(personnes, mere?.pere ?: 0L), "A\u00EFeul mat.", onOuvrir)
                CarteMiniArbre(personneParId(personnes, mere?.mere ?: 0L), "A\u00EFeule mat.", onOuvrir)
            }
            Text(
                "Parents",
                Modifier.padding(top = 6.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                CarteMiniArbre(pere, "P\u00E8re", onOuvrir)
                CarteMiniArbre(mere, "M\u00E8re", onOuvrir)
            }
            Text(
                "Personne et conjoint",
                Modifier.padding(top = 6.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                CarteMiniArbre(centre, "", onOuvrir,centre = true)
                CarteMiniArbre(conjoint, "Conjoint(e)", onOuvrir)
            }
            Text(
                "Enfants",
                Modifier.padding(top = 6.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (centre.enfants.isEmpty()) {
                Text("Aucun enfant renseign\u00E9.", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                centre.enfants.forEach { idEnfant ->
                    CarteMiniArbre(personneParId(personnes, idEnfant), "Enfant", onOuvrir)
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
    centre: Boolean = false
) {
    Card(
        Modifier
            .width(150.dp)
            .padding(4.dp)
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
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            if (libelle.isNotEmpty()) {
                Text(
                    libelle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (p == null) {
                Text(
                    "Inconnu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(p.titre(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                val num = p.numeroAffiche()
                if (num.isNotEmpty()) {
                    Text(
                        num,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val n = dateAffichee(p.dateNaissance.trim())
                val d = dateAffichee(p.dateDeces.trim())
                val morceaux = mutableListOf<String>()
                if (n.isNotEmpty()) morceaux.add("N. " + n)
                if (d.isNotEmpty()) morceaux.add("D. " + d)
                if (morceaux.isNotEmpty()) {
                    Text(morceaux.joinToString(" \u2013 "), style = MaterialTheme.typography.bodySmall)
                }
                if (p.aVerifier()) {
                    Text("\u00C0 v\u00E9rifier", color = CouleurRouge, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}


