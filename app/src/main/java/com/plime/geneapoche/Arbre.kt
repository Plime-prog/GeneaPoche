package com.plime.geneapoche

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- Arbre généalogique
// GénéaPoche 5 : retour à l'arbre à 4 GÉNÉRATIONS (l'arbre complet était
// trop lourd) :
//   - 8 arrière-grands-parents (rangée du haut) ;
//   - 4 grands-parents ;
//   - le père et la mère ;
//   - la personne centrale et son conjoint ;
//   - les enfants de la personne centrale (GénéaPoche 6).
// TOUCHER une carte : la personne touchée devient immédiatement le CENTRE
// de l'arbre (adaptation instantanée).
// APPUI LONG sur une carte : ouverture de la fiche.
// Navigation au doigt : pincement = zoom, glissement = déplacement.

// Trace les liaisons entre deux rangées : verticales depuis la rangée du
// haut, une barre horizontale au milieu, verticales vers la rangée du
// bas. Les positions sont des fractions de la largeur.
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

// Largeur d'une carte d'arbre en dp.
val UniteArbre = 84.dp

@Composable
fun EcranArbre(
    personnes: List<Personne>,
    idCentre: Long,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit
) {
    val centre = personneParId(personnes, idCentre)
    // Zoom (pincement) et déplacement (glissement).
    // GénéaPoche 5 : zoom et déplacement RÉINITIALISÉS à chaque nouvelle
    // personne centrale : l'arbre se régénère nettement sur 4 générations.
    var echelleZoom by remember(idCentre) { mutableStateOf(1f) }
    var deplacementZoom by remember(idCentre) { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    echelleZoom = (echelleZoom * zoom).coerceIn(0.25f, 3f)
                    deplacementZoom += pan
                }
            }
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
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            if (centre == null) {
                Spacer(Modifier.height(12.dp))
                Text("Aucune personne : ajoutez la première personne (de cujus, n° 1) depuis l'onglet Personnes.")
            } else {
                ArbreQuatre(centre = centre, personnes = personnes, onOuvrir = onOuvrir, onCentrerSur = onCentrerSur)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Touchez une carte : elle devient le centre de l'arbre. Appui long : ouvrir la fiche. Pincez pour zoomer, glissez pour déplacer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// L'arbre à 4 générations d'une personne :
//   - rangée des 8 arrière-grands-parents (les cases restent vides si
//     inconnues, pour préserver l'alignement des couples) ;
//   - rangée des 4 grands-parents ;
//   - le couple des parents ;
//   - la personne centrale et son conjoint ;
//   - les enfants de la personne centrale (GénéaPoche 6).
@Composable
fun ArbreQuatre(
    centre: Personne,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit
) {
    val pere = personneParId(personnes, centre.pere)
    val mere = personneParId(personnes, centre.mere)
    val conjoint = personneParId(personnes, centre.conjoint)

    // Les 4 grands-parents (cases nulles si inconnus).
    val gps = listOf(
        pere?.let { personneParId(personnes, it.pere) },
        pere?.let { personneParId(personnes, it.mere) },
        mere?.let { personneParId(personnes, it.pere) },
        mere?.let { personneParId(personnes, it.mere) }
    )
    // Les 8 arrière-grands-parents.
    val agps = gps.flatMap { g ->
        listOf(g?.let { personneParId(personnes, it.pere) }, g?.let { personneParId(personnes, it.mere) })
    }

    // Centres des cases d'une rangée de n cellules (grille régulière).
    fun centres(n: Int): List<Float> = (0 until n).map { (2 * it + 1).toFloat() / (2 * n) }
    // Barres d'union des couples d'une rangée de n cellules : la case 2j
    // est unie à la case 2j+1.
    fun barres(n: Int): List<Pair<Float, Float>> =
        (0 until n / 2).map { j -> (4 * j + 1).toFloat() / (2 * n) to (4 * j + 3).toFloat() / (2 * n) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // ---- Arrière-grands-parents (rangée du haut).
        if (gps.any { it != null }) {
            Column(Modifier.width(UniteArbre * 8)) {
                Row(Modifier.fillMaxWidth()) {
                    agps.forEach { a ->
                        CarteMiniArbre(a, "AGP", onOuvrir, onCentrerSur, modifier = Modifier.weight(1f))
                    }
                }
                LiaisonsArbre(centres(8), barres(8), centres(4))
            }
        }
        // ---- Grands-parents.
        if (pere != null || mere != null) {
            Column(Modifier.width(UniteArbre * 4)) {
                Row(Modifier.fillMaxWidth()) {
                    gps.forEach { g ->
                        CarteMiniArbre(g, "GP", onOuvrir, onCentrerSur, modifier = Modifier.weight(1f))
                    }
                }
                LiaisonsArbre(centres(4), barres(4), centres(2))
            }
        }
        // ---- Le couple des parents.
        if (pere != null || mere != null) {
            Column(Modifier.width(UniteArbre * 2)) {
                Row(Modifier.fillMaxWidth()) {
                    CarteMiniArbre(pere, "P\u00E8re", onOuvrir, onCentrerSur, modifier = Modifier.weight(1f))
                    CarteMiniArbre(mere, "M\u00E8re", onOuvrir, onCentrerSur, modifier = Modifier.weight(1f))
                }
                // Descente vers la carte centrale : à gauche du couple si
                // un conjoint est affiché, au centre sinon.
                LiaisonsArbre(centres(2), barres(2), listOf(if (conjoint == null) 0.5f else 0.25f))
            }
        }
        // ---- La personne centrale et son conjoint.
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CarteMiniArbre(
                centre, "Centre", onOuvrir, onCentrerSur,
                centre = true,
                modifier = Modifier.width(74.dp)
            )
            if (conjoint != null) {
                Box(Modifier.width(14.dp).height(2.dp).background(CouleurLiaison))
                CarteMiniArbre(conjoint, "Conjoint(e)", onOuvrir, onCentrerSur, modifier = Modifier.width(74.dp))
            }
        }
        // ---- Les enfants de la personne centrale (GénéaPoche 6) :
        // rangée sous le couple central, liaisons en T.
        val enfants = centre.enfants.map { personneParId(personnes, it) }.filterNotNull()
        if (enfants.isNotEmpty()) {
            val k = enfants.size
            Column(Modifier.width(UniteArbre * k)) {
                // Descente depuis le couple central (au centre de la
                // rangée) vers chaque enfant.
                LiaisonsArbre(
                    listOf(0.5f),
                    if (k > 1) listOf(centres(k).first() to centres(k).last()) else emptyList(),
                    centres(k)
                )
                Row(Modifier.fillMaxWidth()) {
                    enfants.forEach { e ->
                        CarteMiniArbre(e, "Enfant", onOuvrir, onCentrerSur, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Une carte de l'arbre. TOUCHER : la personne devient le centre de
// l'arbre. APPUI LONG : ouverture de sa fiche.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarteMiniArbre(
    p: Personne?,
    libelle: String,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit,
    centre: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier
            .padding(2.dp)
            .combinedClickable(
                onClick = { if (p != null) onCentrerSur(p.id) },
                onClickLabel = "Centrer l'arbre sur cette personne",
                onLongClick = { if (p != null) onOuvrir(p.id) },
                onLongClickLabel = "Ouvrir la fiche"
            ),
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Le sexe n'est pas signalé par la couleur seule, un
                    // petit symbole le double (lisibilité + TalkBack).
                    Text(
                        when (p.sexe) {
                            "F" -> "♀ "
                            "M" -> "♂ "
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

