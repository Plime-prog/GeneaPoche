package com.plime.geneapoche

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Frise chronologique
// GénéaPoche 3 (v0.35) : frise VERTICALE des personnes VIVANTES de la
// branche, une colonne par personne. L'axe vertical porte les années de
// 10 en 10 (une ligne fine par décennie) ; l'échelle est la vie de
// chacun : de sa naissance (tout en bas) à son décès, ou à sa naissance
// + 100 ans si le décès n'est pas connu. Les événements de la personne
// choisie sont étiquetés sur sa colonne. Design bleu et rose.

// Un événement étiqueté sur la colonne de la personne choisie.
class EvenementFrise(
    val annee: Int,
    val libelle: String
)

// Les événements d'une personne, pour la frise : naissance, baptême,
// mariage, décès, sépulture, événements libres et naissances des enfants.
fun evenementsFrise(p: Personne, personnes: List<Personne>): List<EvenementFrise> {
    val res = mutableListOf<EvenementFrise>()
    anneeDe(p.dateNaissance)?.let { res.add(EvenementFrise(it, "Naissance")) }
    anneeDe(p.dateBapteme)?.let { res.add(EvenementFrise(it, "Baptême")) }
    anneeDe(p.dateMariage)?.let { res.add(EvenementFrise(it, "Mariage")) }
    anneeDe(p.dateDeces)?.let { res.add(EvenementFrise(it, "Décès")) }
    anneeDe(p.dateSepulture)?.let { res.add(EvenementFrise(it, "Sépulture")) }
    p.evenementsLibres.forEach { ev ->
        val a = anneeDe(ev.date) ?: return@forEach
        val t = ev.intitule.trim()
        res.add(EvenementFrise(a, if (t.isNotEmpty()) t else "Événement"))
    }
    p.enfants.forEach { idE ->
        val e = personneParId(personnes, idE) ?: return@forEach
        val a = anneeDe(e.dateNaissance) ?: return@forEach
        res.add(EvenementFrise(a, "Naissance de " + (e.prenoms.trim().ifEmpty { e.nom.trim() })))
    }
    return res.sortedBy { it.annee }
}

@Composable
fun EcranFrise(
    personnes: List<Personne>,
    idChoisie: Long,
    onOuvrir: (Long) -> Unit,
    onChoisir: (Long) -> Unit
) {
    val choisie = personneParId(personnes, idChoisie) ?: personnes.firstOrNull()
    var pickerOuvert by remember { mutableStateOf(false) }
    val anneeCourante = remember { java.time.LocalDate.now().year }

    if (choisie == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Aucune personne pour le moment.")
        }
        return
    }

    // Personnes de la branche de la personne choisie (racine la plus
    // haute + tous ses descendants et conjoints)...
    val racine = racineBranche(choisie, personnes)
    val membres = brancheDe(racine.id, personnes)
        .mapNotNull { id -> personneParId(personnes, id) }
    // ... dont seules les VIVANTES ont une colonne (règle des 100 ans :
    // pas de décès connu et née il y a moins de 100 ans, ou sans date).
    val vivantes = membres.filter { estVivante(it, anneeCourante) }
        .sortedBy { anneeDe(it.dateNaissance) ?: 9999 }

    // La personne choisie a toujours sa colonne (même décédée) : c'est
    // elle qui porte les étiquettes d'événements.
    val colonnes = if (vivantes.any { it.id == choisie.id }) vivantes else listOf(choisie) + vivantes

    // ---- Échelle de la frise : de la naissance la plus ancienne à la
    // fin de vie la plus récente (décès, ou naissance + 100 ans).
    val naissances = colonnes.mapNotNull { anneeDe(it.dateNaissance) }
    val fins = colonnes.map { p ->
        anneeDe(p.dateDeces) ?: (anneeDe(p.dateNaissance)?.plus(100) ?: anneeCourante)
    }
    val anneeMin = (naissances.minOrNull() ?: (anneeCourante - 100)).coerceAtMost(anneeCourante)
    val anneeMax = (fins.maxOrNull() ?: anneeCourante).coerceAtLeast(anneeMin + 10)
    val pxAn = 14.dp
    val hauteurFrise = pxAn * (anneeMax - anneeMin + 1)
    val decades = (anneeMin / 10 * 10..anneeMax step 10).toList()

    // Navigation au doigt : pincement = zoom, glissement = déplacement.
    var echelleZoom by remember { mutableStateOf(1f) }
    var deplacementZoom by remember { mutableStateOf(Offset.Zero) }

    fun yAn(annee: Int): Int = ((anneeMax - annee).coerceAtLeast(0)) * pxAn.value.toInt()

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    echelleZoom = (echelleZoom * zoom).coerceIn(0.25f, 3f)
                    deplacementZoom += pan
                }
            }
    ) {
        // En-tête : personne choisie, nombre de vivantes, légende.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { pickerOuvert = true }) {
                Text("Personne : " + choisie.titre(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    vivantes.size.toString() + " vivante(s) dans la branche",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Trait bleu : homme, trait rose : femme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // La frise : axe des années + colonnes des vivantes.
        Row(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = echelleZoom
                    scaleY = echelleZoom
                    translationX = deplacementZoom.x
                    translationY = deplacementZoom.y
                }
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            // Ordonnée : les années de 10 en 10.
            Box(Modifier.width(44.dp).height(hauteurFrise)) {
                decades.forEach { d ->
                    Text(
                        d.toString(),
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(top = (yAn(d) - 7).coerceAtLeast(0).dp)
                            .padding(end = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CouleurVertFonce,
                        maxLines = 1
                    )
                }
            }
            // Les colonnes, sur un fond quadrillé d'une ligne fine par
            // décennie.
            Box(Modifier.height(hauteurFrise)) {
                decades.forEach { d ->
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(top = yAn(d).dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CouleurGrisBord)
                    )
                }
                Row(Modifier.align(Alignment.TopStart)) {
                    colonnes.forEach { p ->
                        ColonneFrise(
                            p = p,
                            choisie = p.id == choisie.id,
                            anneeMin = anneeMin,
                            anneeMax = anneeMax,
                            pxAn = pxAn,
                            personnes = personnes,
                            onOuvrir = onOuvrir
                        )
                    }
                }
            }
        }
        Text(
            "Pincez pour zoomer, glissez pour déplacer la frise. Touchez un nom pour ouvrir la fiche.",
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (pickerOuvert) {
        AlertDialog(
            onDismissRequest = { pickerOuvert = false },
            title = { Text("Frise de quelle personne ?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    personnes.sortedBy { it.titre() }.forEach { c ->
                        TextButton(onClick = { onChoisir(c.id); pickerOuvert = false }) {
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

// Une colonne de la frise : le trait de vie (de la naissance tout en
// bas jusqu'au décès ou + 100 ans), le nom au sommet du trait, et pour
// la personne choisie une étiquette par événement la concernant.
@Composable
fun ColonneFrise(
    p: Personne,
    choisie: Boolean,
    anneeMin: Int,
    anneeMax: Int,
    pxAn: androidx.compose.ui.unit.Dp,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit
) {
    val largeur = if (choisie) 250.dp else 72.dp
    val couleurTrait = when (p.sexe) {
        "F" -> CouleurRose
        else -> CouleurBleu
    }
    val nai = anneeDe(p.dateNaissance)
    val fin = anneeDe(p.dateDeces) ?: (nai?.plus(100))
    val hauteurFrise = pxAn * (anneeMax - anneeMin + 1)

    Box(Modifier.width(largeur).height(hauteurFrise)) {
        if (nai != null && fin != null && fin > nai) {
            // Le trait de vie : tout en bas la naissance, vers le haut la
            // fin de vie.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = pxAn * ((nai - anneeMin).coerceAtLeast(0)))
                    .width(if (choisie) 10.dp else 8.dp)
                    .height(pxAn * (fin - nai))
                    .background(couleurTrait, RoundedCornerShape(4.dp))
            )
            // Le nom au sommet du trait de vie.
            Text(
                p.titre(),
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = pxAn * ((anneeMax - fin).coerceAtLeast(0)))
                    .clickable { onOuvrir(p.id) }
                    .padding(horizontal = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = CouleurVertFonce
            )
        } else {
            // Sans année de naissance : le nom en bas de colonne.
            Text(
                p.titre(),
                Modifier
                    .align(Alignment.BottomCenter)
                    .clickable { onOuvrir(p.id) }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = CouleurVertFonce
            )
        }
        if (choisie) {
            // Étiquettes : un petit cartouche par événement de la
            // personne choisie, à la hauteur de son année.
            val evts = evenementsFrise(p, personnes)
            val parAnnee = mutableMapOf<Int, Int>()
            evts.forEach { ev ->
                val rang = parAnnee.getOrDefault(ev.annee, 0)
                parAnnee[ev.annee] = rang + 1
                val y = pxAn * ((anneeMax - ev.annee).coerceAtLeast(0)) + (rang * 20).dp
                Surface(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(top = y)
                        .padding(start = 40.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = when (p.sexe) {
                        "F" -> CouleurRosePale
                        else -> CouleurBleuPale
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, couleurTrait)
                ) {
                    Text(
                        ev.libelle + " " + ev.annee.toString(),
                        Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = CouleurVertFonce
                    )
                }
            }
        }
    }
}
