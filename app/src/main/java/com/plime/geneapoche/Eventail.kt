package com.plime.geneapoche

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Eventail (v0.142)
// Demi-eventail des ascendants sur le MEME nombre de generations que l'arbre
// classique (parents, grands-parents, arriere-grands-parents = 3 anneaux) :
// personne centrale au milieu, pere a gauche, mere a droite, chaque anneau
// double le nombre de secteurs (2, 4, 8). Meme comportement qu'au toucher de
// l'arbre : TOUCHER = centrer, APPUI LONG = ouvrir la fiche.

const val GENERATIONS_EVENTAIL = 3

// Anneau k (1..generations) = 2^k personnes (null si inconnue), de gauche a
// droite : pere, mere puis leurs parents, etc. (meme ordre que l'arbre).
fun ancetresEventail(
    centre: Personne,
    personnes: List<Personne>,
    generations: Int = GENERATIONS_EVENTAIL
): List<List<Personne?>> {
    val anneaux = mutableListOf<List<Personne?>>()
    var courant: List<Personne?> = listOf(centre)
    repeat(generations) {
        courant = courant.flatMap { q ->
            listOf(q?.let { personneParId(personnes, it.pere) }, q?.let { personneParId(personnes, it.mere) })
        }
        anneaux.add(courant)
    }
    return anneaux
}

// Secteur touche : (anneau, indice) ; anneau 0 = personne centrale ; null si
// hors de l'eventail (dy >= 0 = moitie basse non dessinee).
fun secteurEventail(dx: Float, dy: Float, r0: Float, epaisseur: Float, generations: Int = GENERATIONS_EVENTAIL): Pair<Int, Int>? {
    if (dy >= 0f) return null
    val r = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    if (r <= r0) return Pair(0, 0)
    val k = ((r - r0) / epaisseur).toInt() + 1
    if (k > generations) return null
    val n = 1 shl k
    var ang = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())) + 360.0
    if (ang < 180.0) ang = 180.0
    val i = ((ang - 180.0) / (180.0 / n)).toInt().coerceIn(0, n - 1)
    return Pair(k, i)
}

fun anneesCourtes(p: Personne): String {
    val n = anneeDe(p.dateNaissance.ifBlank { p.naissanceCalculee })
    val d = if (decesAffichable(p)) anneeDe(p.dateDeces) else null
    return when {
        n != null && d != null -> n.toString() + "–" + d.toString()
        n != null -> "° " + n.toString()
        d != null -> "† " + d.toString()
        else -> ""
    }
}

@Composable
fun EcranEventail(
    personnes: List<Personne>,
    idCentre: Long,
    onOuvrir: (Long) -> Unit,
    onCentrerSur: (Long) -> Unit
) {
    val centre = personneParId(personnes, idCentre)
    var echelleZoom by remember(idCentre) { mutableStateOf(1f) }
    var deplacementZoom by remember(idCentre) { mutableStateOf(Offset.Zero) }
    val etatZoom = rememberTransformableState { zoom, pan, _ ->
        echelleZoom = (echelleZoom * zoom).coerceIn(0.5f, 3f)
        deplacementZoom += pan
    }
    if (centre == null) {
        Text("Aucune personne : ajoutez la première personne depuis l'onglet Personnes.", Modifier.padding(12.dp))
        return
    }
    val anneaux = remember(centre, personnes) { ancetresEventail(centre, personnes) }
    val mesure = rememberTextMeasurer()
    val couleurTexte = MaterialTheme.colorScheme.onSurface
    val couleurCentre = MaterialTheme.colorScheme.primaryContainer
    val couleurInconnu = MaterialTheme.colorScheme.surfaceVariant
    val couleurSurface = MaterialTheme.colorScheme.surface
    val couleurSepar = MaterialTheme.colorScheme.background

    BoxWithConstraints(Modifier.fillMaxSize().transformable(etatZoom)) {
        val rayon = (maxWidth - 16.dp) / 2
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
                .padding(8.dp)
        ) {
            Canvas(
                Modifier
                    .size(rayon * 2, rayon + 8.dp)
                    .pointerInput(anneaux) {
                        val r = size.width / 2f
                        val r0 = r * 0.2f
                        val ep = (r - r0) / GENERATIONS_EVENTAIL
                        fun trouver(o: Offset): Personne? {
                            val s = secteurEventail(o.x - r, o.y - r, r0, ep) ?: return null
                            return if (s.first == 0) centre else anneaux[s.first - 1][s.second]
                        }
                        detectTapGestures(
                            onTap = { o -> trouver(o)?.let { onCentrerSur(it.id) } },
                            onLongPress = { o -> trouver(o)?.let { onOuvrir(it.id) } }
                        )
                    }
            ) {
                val r = size.width / 2f
                val cx = r
                val cy = r
                val r0 = r * 0.2f
                val ep = (r - r0) / GENERATIONS_EVENTAIL
                val separ = 1.5.dp.toPx()
                fun couleurDe(p: Personne?): Color = when {
                    p == null -> couleurInconnu
                    p.sexe == "F" -> CouleurRosePale
                    p.sexe == "M" -> CouleurBleuPale
                    else -> couleurSurface
                }
                for (k in 1..GENERATIONS_EVENTAIL) {
                    val n = 1 shl k
                    val rm = r0 + (k - 0.5f) * ep
                    val balayage = 180f / n
                    for (i in 0 until n) {
                        val p = anneaux[k - 1][i]
                        drawArc(
                            color = couleurDe(p),
                            startAngle = 180f + i * balayage,
                            sweepAngle = balayage,
                            useCenter = false,
                            topLeft = Offset(cx - rm, cy - rm),
                            size = Size(2 * rm, 2 * rm),
                            style = Stroke(width = ep)
                        )
                        // separateur radial
                        val a = Math.toRadians((180f + i * balayage).toDouble())
                        drawLine(
                            couleurSepar,
                            Offset(cx + (r0 + (k - 1) * ep) * Math.cos(a).toFloat(), cy + (r0 + (k - 1) * ep) * Math.sin(a).toFloat()),
                            Offset(cx + (r0 + k * ep) * Math.cos(a).toFloat(), cy + (r0 + k * ep) * Math.sin(a).toFloat()),
                            separ
                        )
                        if (p != null) {
                            val theta = 180f + (i + 0.5f) * balayage
                            val t = Math.toRadians(theta.toDouble())
                            val px = cx + rm * Math.cos(t).toFloat()
                            val py = cy + rm * Math.sin(t).toFloat()
                            if (k == 1) {
                                texteSecteur(mesure, p, Offset(px, py), 0f, 2 * rm * 0.62f, 10f, couleurTexte)
                            } else {
                                val rot = if (Math.cos(t) >= 0.0) theta else theta - 180f
                                val larg = ep - 4.dp.toPx()
                                texteSecteur(mesure, p, Offset(px, py), rot, larg, if (k == 2) 9f else 8f, couleurTexte)
                            }
                        }
                    }
                }
                // Personne centrale
                drawCircle(couleurCentre, radius = r0, center = Offset(cx, cy))
                texteSecteur(mesure, centre, Offset(cx, cy - r0 * 0.45f), 0f, r0 * 1.7f, 9f, couleurTexte)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Touchez un secteur : la personne devient le centre. Appui long : sa fiche. Pincez pour zoomer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Ecrit NOM / Prenoms / annees, centre sur "centre", tourne de "rotation"
// degres, chaque ligne tronquee a "largeur" pixels.
private fun DrawScope.texteSecteur(
    mesure: TextMeasurer,
    p: Personne,
    centre: Offset,
    rotation: Float,
    largeur: Float,
    taille: Float,
    couleur: Color
) {
    if (largeur < 8f) return
    val lignes = listOf(
        Triple(p.nom.trim(), taille, true),
        Triple(p.prenoms.trim().substringBefore(" ").ifEmpty { p.prenoms.trim() }, taille, false),
        Triple(anneesCourtes(p), taille - 1f, false)
    ).filter { it.first.isNotEmpty() }
    val mises = lignes.map { (t, s, gras) ->
        mesure.measure(
            text = t,
            style = TextStyle(fontSize = s.sp, fontWeight = if (gras) FontWeight.Bold else FontWeight.Normal, color = couleur),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = Constraints(maxWidth = largeur.toInt().coerceAtLeast(1))
        )
    }
    val hauteur = mises.sumOf { it.size.height.toDouble() }.toFloat()
    withTransform({
        translate(centre.x, centre.y)
        rotate(rotation, Offset.Zero)
    }) {
        var y = -hauteur / 2f
        mises.forEach { m ->
            drawText(m, topLeft = Offset(-m.size.width / 2f, y))
            y += m.size.height
        }
    }
}
