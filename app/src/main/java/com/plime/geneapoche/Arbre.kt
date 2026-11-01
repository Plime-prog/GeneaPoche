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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Arbre généalogique
// GénéaPoche 3 (v0.35) : arbre COMPLET et VERTICAL, navigable au doigt
// (pincement = zoom, glissement = déplacement) :
//   - en haut, TOUTES les générations d'ascendants (jusqu'à la plus haute
//     renseignée), aïeux reliés façon généalogique ;
//   - sous le couple racine, TOUS les descendants, génération par
//     génération, avec leurs conjoints et tous les latéraux (chaque
//     enfant de chaque couple apparaît).
// L'ancienne vue "4 générations" horizontale est supprimée.
// Toucher une carte ouvre la fiche et recentre l'arbre sur elle.


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

// Enfants d'un couple (id du père, id de la mère ; 0 = inconnu) :
// toute personne dont le père OU la mère est l'un des deux partenaires,
// rangée par année de naissance (les sans-date en dernier).
fun enfantsDuCouple(
    personnes: List<Personne>,
    pereId: Long,
    mereId: Long,
    exclus: Set<Long>
): List<Personne> {
    return personnes.filter { p ->
        p.id !in exclus && p.id != pereId && p.id != mereId &&
            ((pereId != 0L && p.pere == pereId) || (mereId != 0L && p.mere == mereId))
    }.sortedWith(
        compareBy({ anneeNaissance(it) ?: 9999 }, { it.id })
    )
}

// Plan d'une sous-famille : la personne, son conjoint, ses sous-familles
// (une par enfant) et la largeur du bloc en "unités" (1 par feuille).
// La récursion s'arrête sur les personnes déjà développées (garde-fou
// contre les liens circulaires) et au-delà de 25 générations.
class PlanFamille(
    val personne: Personne,
    val conjoint: Personne?,
    val sousFamilles: List<PlanFamille>,
    val largeur: Int
)

fun planFamille(
    p: Personne,
    personnes: List<Personne>,
    vus: MutableSet<Long>,
    profondeur: Int
): PlanFamille {
    if (p.id in vus || profondeur > 25) return PlanFamille(p, null, emptyList(), 1)
    vus.add(p.id)
    val conjoint = personneParId(personnes, p.conjoint)
    val exclus = mutableSetOf(p.id)
    if (conjoint != null) exclus.add(conjoint.id)
    val enfants = enfantsDuCouple(personnes, p.id, conjoint?.id ?: 0L, exclus)
    val sous = enfants.map { e -> planFamille(e, personnes, vus, profondeur + 1) }
    val largeur = sous.fold(0) { acc, s -> acc + s.largeur }.coerceAtLeast(1)
    return PlanFamille(p, conjoint, sous, largeur)
}

// Largeur d'une carte d'arbre en dp (une unité de plan).
val UniteArbre = 168.dp

// Rend un bloc famille : le couple en haut, les liaisons, puis une
// colonne par enfant contenant sa propre famille (récursif).
@Composable
fun FamilleArbre(
    plan: PlanFamille,
    personnes: List<Personne>,
    idCentre: Long,
    onOuvrir: (Long) -> Unit
) {
    val total = plan.largeur
    Column(Modifier.width(UniteArbre * total), horizontalAlignment = Alignment.CenterHorizontally) {
        // Le couple : les deux cartes unies par une petite barre.
        Row(verticalAlignment = Alignment.CenterVertically) {
            CarteMiniArbre(
                plan.personne, "", onOuvrir,
                centre = plan.personne.id == idCentre,
                modifier = Modifier.width(74.dp)
            )
            if (plan.conjoint != null) {
                Box(Modifier.width(14.dp).height(2.dp).background(CouleurLiaison))
                CarteMiniArbre(plan.conjoint, "", onOuvrir, modifier = Modifier.width(74.dp))
            }
        }
        if (plan.sousFamilles.isNotEmpty()) {
            // Centres des colonnes enfants, en fractions de la largeur.
            var cumul = 0
            val centres = plan.sousFamilles.map { s ->
                val c = (cumul + s.largeur / 2).toFloat() / total.toFloat()
                cumul += s.largeur
                c
            }
            LiaisonsArbre(
                listOf(0.5f),
                listOf(centres.first() to centres.last()),
                centres
            )
            Row(Modifier.fillMaxWidth()) {
                plan.sousFamilles.forEach { s ->
                    Box(Modifier.width(UniteArbre * s.largeur)) {
                        FamilleArbre(s, personnes, idCentre, onOuvrir)
                    }
                }
            }
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
    // Zoom (pincement) et déplacement (glissement) : l'arbre complet est
    // souvent plus large que l'écran, on navigue dedans au doigt.
    var echelleZoom by remember { mutableStateOf(1f) }
    var deplacementZoom by remember { mutableStateOf(Offset.Zero) }
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { pickerOuvert = true }) {
                        Text("Centré sur : " + centre.titre(), maxLines = 1)
                    }
                    OutlinedButton(onClick = {
                        echelleZoom = 1f
                        deplacementZoom = Offset.Zero
                    }) {
                        Text("Vue initiale")
                    }
                }
                ArbreComplet(centre = centre, personnes = personnes, onOuvrir = onOuvrir)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Pincez pour zoomer, glissez pour déplacer l'arbre. Touchez une carte pour ouvrir la fiche.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
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

// L'arbre complet d'une personne :
//   - rangées d'ancêtres (toutes les générations jusqu'à la plus haute
//     renseignée), chaque rangée large de n cases égales ;
//   - puis le bloc famille récursif du couple racine (les parents, ou la
//     personne elle-même) qui déroule TOUS les descendants et latéraux.
@Composable
fun ArbreComplet(
    centre: Personne,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit
) {
    val pere = personneParId(personnes, centre.pere)
    val mere = personneParId(personnes, centre.mere)

    // ---- Ancêtres au-dessus du couple racine : depuis les grands-parents.
    // Chaque rangée garde TOUTES ses cases (même inconnues) pour préserver
    // l'alignement des couples ; la rangée du dessus double la largeur.
    fun slotsParents(slots: List<Personne?>): List<Personne?> =
        slots.flatMap { p ->
            if (p == null) listOf(null, null)
            else listOf(personneParId(personnes, p.pere), personneParId(personnes, p.mere))
        }
    val rangees = mutableListOf<List<Personne?>>()
    var courante: List<Personne?> = listOf(
        pere?.let { personneParId(personnes, it.pere) },
        pere?.let { personneParId(personnes, it.mere) },
        mere?.let { personneParId(personnes, it.pere) },
        mere?.let { personneParId(personnes, it.mere) }
    )
    var gardeFou = 0
    while (courante.any { it != null } && gardeFou < 10) {
        gardeFou += 1
        rangees.add(courante)
        if (courante.size >= 64) break
        courante = slotsParents(courante)
    }

    // Centres des cases d'une rangée de n cellules (grille régulière).
    fun centres(n: Int): List<Float> = (0 until n).map { (2 * it + 1).toFloat() / (2 * n) }
    // Barres d'union des couples d'une rangée de n cellules : la case 2j
    // est unie à la case 2j+1.
    fun barres(n: Int): List<Pair<Float, Float>> =
        (0 until n / 2).map { j -> (4 * j + 1).toFloat() / (2 * n) to (4 * j + 3).toFloat() / (2 * n) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Les rangées d'ancêtres, de la plus haute (haut de l'écran) vers
        // la plus basse : on affiche la liste renversée.
        val affichees = rangees.reversed()
        affichees.forEachIndexed { i, rangee ->
            val n = rangee.size.coerceAtLeast(1)
            Column(Modifier.width(84.dp * n)) {
                Row(Modifier.fillMaxWidth()) {
                    rangee.forEach { a ->
                        CarteMiniArbre(a, "", onOuvrir, modifier = Modifier.weight(1f))
                    }
                }
                if (i < affichees.size - 1) {
                    // Vers la rangée du dessous : verticales + barres de
                    // couples + descente vers les cellules enfants.
                    val m = affichees[i + 1].size.coerceAtLeast(1)
                    LiaisonsArbre(centres(n), barres(n), centres(m))
                } else {
                    // Dernière rangée d'ancêtres : descente vers le couple
                    // racine (père et mère, centrés dans l'arbre).
                    LiaisonsArbre(centres(n), barres(n), listOf(0.25f, 0.75f))
                }
            }
        }

        // ---- Bloc famille récursif : couple racine puis tous les
        // descendants (latéraux compris).
        val racine = pere ?: mere ?: centre
        val plan = remember(racine.id, personnes) {
            planFamille(racine, personnes, mutableSetOf(), 0)
        }
        Spacer(Modifier.height(4.dp))
        FamilleArbre(plan, personnes, centre.id, onOuvrir)
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
