package com.plime.geneapoche

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Arbre généalogique
// Vue centrée sur une personne, RELIÉE et CENTRÉE façon généalogique :
// grands-parents, parents, personne et conjoint centrés, enfants centrés
// sous le couple. Chaque rangée est une grille régulière, les liaisons sont
// tracées entre les cartes (verticales + barres horizontales).
// Toucher une carte ouvre la fiche et recentre l'arbre sur elle.


// Trace les liaisons entre deux rangées : verticales depuis la rangée du
// haut, une barre horizontale au milieu, verticales vers la rangée du bas.
// Les positions sont des fractions de la largeur (grille régulière).
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
    // Réponse utilisateur : vue compacte "Arbre 4 générations", en
    // horizontal, SANS les enfants (quatre générations d'ascendants).
    var vueQuatreGenerations by remember { mutableStateOf(false) }

    // G3-16 : zoom (pincement) et deplacement fluides sur l'ensemble de
    // l'arbre ; le defilement vertical reste libre a un doigt.
    var echelleZoom by remember { mutableStateOf(1f) }
    var deplacementZoom by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    echelleZoom = (echelleZoom * zoom).coerceIn(0.6f, 2.5f)
                    deplacementZoom += pan
                }
            }
            .verticalScroll(rememberScrollState())
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = echelleZoom
                scaleY = echelleZoom
                translationX = deplacementZoom.x
                translationY = deplacementZoom.y
            }
            .padding(10.dp)
    ) {
        if (centre == null) {
            Spacer(Modifier.height(12.dp))
            Text("Aucune personne : ajoutez la première personne (de cujus, n° 1) depuis l'onglet Personnes.")
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { pickerOuvert = true }) {
                    Text("Centré sur : " + centre.titre(), maxLines = 1)
                }
                OutlinedButton(onClick = { vueQuatreGenerations = !vueQuatreGenerations }) {
                    Text(if (vueQuatreGenerations) "Arbre complet" else "Arbre 4 générations")
                }
            }
            if (vueQuatreGenerations) {
                ArbreQuatreGenerations(centre = centre, personnes = personnes, onOuvrir = onOuvrir)
                Spacer(Modifier.height(24.dp))
            } else {
            val pere = personneParId(personnes, centre.pere)
            val mere = personneParId(personnes, centre.mere)
            val conjoint = personneParId(personnes, centre.conjoint)

            // --- Grands-parents : 4 cases égales (centres à 1/8, 3/8, 5/8, 7/8).
            Text(
                "Grands-parents",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth()) {
                CarteMiniArbre(personneParId(personnes, pere?.pere ?: 0L), "Aïeul pat.", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(personneParId(personnes, pere?.mere ?: 0L), "Aïeule pat.", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(personneParId(personnes, mere?.pere ?: 0L), "Aïeul mat.", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(personneParId(personnes, mere?.mere ?: 0L), "Aïeule mat.", onOuvrir, modifier = Modifier.weight(1f))
            }
            // Aïeux -> parents : union de chaque couple, descente au parent.
            LiaisonsArbre(
                listOf(0.125f, 0.375f, 0.625f, 0.875f),
                listOf(0.125f to 0.375f, 0.625f to 0.875f),
                listOf(0.25f, 0.75f)
            )

            // --- Parents : 2 cases égales (centres à 1/4 et 3/4).
            Row(Modifier.fillMaxWidth()) {
                CarteMiniArbre(pere, "Père", onOuvrir, modifier = Modifier.weight(1f))
                CarteMiniArbre(mere, "Mère", onOuvrir, modifier = Modifier.weight(1f))
            }
            // Parents -> couple central : barre d'union au centre (1/2).
            LiaisonsArbre(
                listOf(0.25f, 0.75f),
                listOf(0.25f to 0.75f),
                listOf(0.5f)
            )

            // --- Personne et conjoint : le couple est CENTRÉ, uni par une barre.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarteMiniArbre(centre, "", onOuvrir, centre = true, modifier = Modifier.width(150.dp))
                Box(Modifier.width(14.dp).height(2.dp).background(CouleurLiaison))
                CarteMiniArbre(conjoint, "Conjoint(e)", onOuvrir, modifier = Modifier.width(150.dp))
            }

            // --- Enfants : centrés sous le couple, reliés par leurs centres.
            val nb = centre.enfants.size
            Text(
                "Enfants",
                Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (nb == 0) {
                Text("Aucun enfant renseigné.", style = MaterialTheme.typography.bodySmall)
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
    }
    }
    // Fermeture du Box de zoom (G3-16).

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

// Vue "Arbre 4 générations" : très compacte et HORIZONTALE, sans les
// enfants. De gauche à droite : arrière-grands-parents (8), grands-
// parents (4), parents (2), personne et conjoint. Chaque carte reste
// touchable (ouverture de la fiche).
@Composable
fun ArbreQuatreGenerations(
    centre: Personne,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit
) {
    val pere = personneParId(personnes, centre.pere)
    val mere = personneParId(personnes, centre.mere)
    val conjoint = personneParId(personnes, centre.conjoint)
    val grandsParents = listOfNotNull(
        personneParId(personnes, pere?.pere ?: 0L),
        personneParId(personnes, pere?.mere ?: 0L),
        personneParId(personnes, mere?.pere ?: 0L),
        personneParId(personnes, mere?.mere ?: 0L)
    )
    fun parentsDe(p: Personne?): List<Personne?> {
        if (p == null) return listOf(null, null)
        return listOf(personneParId(personnes, p.pere), personneParId(personnes, p.mere))
    }
    val arrieres = grandsParents.flatMap { gp -> parentsDe(gp) }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colonne 1 : les 8 arrière-grands-parents (génération la plus ancienne).
        Column(Modifier.width(76.dp)) {
            arrieres.forEach { ai ->
                CarteMiniArbre(ai, "", onOuvrir, modifier = Modifier.fillMaxWidth().padding(1.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        // Colonne 2 : les 4 grands-parents.
        Column(Modifier.width(80.dp)) {
            (0 until 4).forEach { i ->
                CarteMiniArbre(grandsParents.getOrNull(i), "", onOuvrir, modifier = Modifier.fillMaxWidth().padding(1.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        // Colonne 3 : père et mère.
        Column(Modifier.width(92.dp)) {
            CarteMiniArbre(pere, "", onOuvrir, modifier = Modifier.fillMaxWidth().padding(1.dp))
            CarteMiniArbre(mere, "", onOuvrir, modifier = Modifier.fillMaxWidth().padding(1.dp))
        }
        Spacer(Modifier.width(6.dp))
        // Colonne 4 : personne et conjoint.
        Column(Modifier.width(104.dp)) {
            CarteMiniArbre(centre, "", onOuvrir, centre = true, modifier = Modifier.fillMaxWidth().padding(1.dp))
            CarteMiniArbre(conjoint, "", onOuvrir, modifier = Modifier.fillMaxWidth().padding(1.dp))
        }
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
        border = androidx.compose.foundation.BorderStroke(1.dp, CouleurBordVert),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (centre) MaterialTheme.colorScheme.primaryContainer
            else if (p == null) MaterialTheme.colorScheme.surfaceVariant
            else when (p.sexe) {
                "F" -> CouleurRosePale
                "M" -> CouleurBleuPale
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // G3-11/G3-16 : le sexe n'est pas signale par la couleur
                    // seule, un petit symbole le double (lisibilite + TalkBack).
                    Text(
                        when (p.sexe) {
                            "F" -> "\u2640 "
                            "M" -> "\u2642 "
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(p.titre(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
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
                // Règle des 100 ans : décès affiché seulement si connu ou possible.
                if (d.isNotEmpty() && decesAffichable(p)) morceaux.add("D. " + d)
                if (morceaux.isNotEmpty()) {
                    Text(morceaux.joinToString(" – "), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
