package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Ligne de vie
// GénéaPoche 8 (v0.47) : remplace l'ancienne frise verticale (colonnes
// de toutes les vivantes de la branche, illisible sur téléphone).
// Chaque membre de la famille proche de la personne choisie
// (grands-parents, parents, frères et sœurs, enfants) occupe UNE LIGNE :
// son trait de vie horizontal va de sa naissance à son décès, ou à
// aujourd'hui si elle est vivante (règle des 100 ans respectée : un
// décès non certain n'est jamais tracé). Un point marque chaque
// événement connu (N, B, M, S, D, événements libres). Les années se
// lisent en haut, une ligne fine par décennie ; le trait vert vertical
// marque aujourd'hui. Tout tient à l'écran, sans zoom ni déplacement :
// toucher une ligne ouvre la fiche.

// Un membre affiché : la personne et son lien avec la personne choisie.
class MembreVie(val p: Personne, val lien: String)

// La famille proche d'une personne, dans l'ordre des générations :
// grands-parents, parents, fratrie (par année de naissance), la
// personne elle-même, puis ses enfants (par année de naissance).
fun membresLigneDeVie(choisie: Personne, personnes: List<Personne>): List<MembreVie> {
    val res = mutableListOf<MembreVie>()
    fun ajouter(id: Long, lien: String) {
        if (id == 0L || res.any { it.p.id == id }) return
        personneParId(personnes, id)?.let { res.add(MembreVie(it, lien)) }
    }
    val pere = personneParId(personnes, choisie.pere)
    val mere = personneParId(personnes, choisie.mere)
    ajouter(pere?.pere ?: 0L, "Grand-père pat.")
    ajouter(pere?.mere ?: 0L, "Grand-mère pat.")
    ajouter(mere?.pere ?: 0L, "Grand-père mat.")
    ajouter(mere?.mere ?: 0L, "Grand-mère mat.")
    ajouter(choisie.pere, "Père")
    ajouter(choisie.mere, "Mère")
    // Fratrie : même père ou même mère (déjà renseigné), hors soi-même.
    personnes.filter { q ->
        q.id != choisie.id &&
            ((choisie.pere != 0L && q.pere == choisie.pere) ||
                (choisie.mere != 0L && q.mere == choisie.mere))
    }.sortedBy { anneeDe(it.dateNaissance) ?: 9999 }.forEach { q ->
        if (res.none { it.p.id == q.id }) {
            res.add(MembreVie(q, if (q.sexe == "F") "Sœur" else "Frère"))
        }
    }
    res.add(MembreVie(choisie, ""))
    choisie.enfants.sortedBy { idE -> personneParId(personnes, idE)?.let { anneeDe(it.dateNaissance) } ?: 9999 }
        .forEach { idE ->
            if (res.none { it.p.id == idE }) {
                personneParId(personnes, idE)?.let { e ->
                    res.add(MembreVie(e, if (e.sexe == "F") "Fille" else "Fils"))
                }
            }
        }
    return res
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

    val membres = membresLigneDeVie(choisie, personnes)
    fun anneeNai(m: MembreVie): Int? = anneeDe(m.p.dateNaissance)
    // Décès affichable seulement (règle des 100 ans).
    fun anneeFin(m: MembreVie): Int? {
        val d = anneeDe(m.p.dateDeces)
        return if (d != null && decesAffichable(m.p)) d else null
    }
    // Fin du trait : décès connu, sinon aujourd'hui si vivante, sinon
    // naissance + 100 ans.
    fun finBarre(m: MembreVie): Int? {
        anneeFin(m)?.let { return it }
        val n = anneeNai(m) ?: return null
        return if (estVivante(m.p, anneeCourante)) anneeCourante else n + 100
    }

    val anneeMin = (membres.mapNotNull { anneeNai(it) }.minOrNull() ?: anneeCourante)
        .coerceAtMost(anneeCourante)
    val anneeMax = ((membres.mapNotNull { finBarre(it) } + anneeCourante).maxOrNull() ?: anneeCourante)
        .coerceAtLeast(anneeMin + 1)
    val etendue = (anneeMax - anneeMin).toFloat()
    val aujourdhuiVisible = anneeCourante in anneeMin..anneeMax
    val decennies = (anneeMin..anneeMax).filter { it % 10 == 0 }

    BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
        val largeurNoms = 84.dp
        val largeurAxe = maxWidth - largeurNoms
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // En-tête : personne choisie.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pickerOuvert = true }) {
                    Text("Personne : " + choisie.titre(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                "Grands-parents, parents, fratrie et enfants : une ligne de vie par personne.",
                Modifier.padding(vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Axe des années, en haut : une étiquette par décennie.
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Spacer(Modifier.width(largeurNoms))
                Box(Modifier.width(largeurAxe).height(14.dp)) {
                    decennies.forEach { d ->
                        Text(
                            d.toString(),
                            Modifier.padding(start = largeurAxe * (((d - anneeMin).toFloat() / etendue))).padding(end = 4.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CouleurVertFonce,
                            maxLines = 1
                        )
                    }
                }
            }
            // Une ligne de vie par membre.
            membres.forEach { m ->
                LigneVie(
                    m = m,
                    ligneChoisie = m.p.id == choisie.id,
                    anneeMin = anneeMin,
                    anneeMax = anneeMax,
                    etendue = etendue,
                    anneeCourante = anneeCourante,
                    largeurAxe = largeurAxe,
                    anneeNai = anneeNai(m),
                    anneeFin = anneeFin(m),
                    finBarre = finBarre(m),
                    onOuvrir = onOuvrir
                )
            }
            if (membres.mapNotNull { anneeNai(it) }.isEmpty()) {
                Text(
                    "Ajoutez des dates de naissance pour voir les lignes de vie.",
                    Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Touchez une ligne pour ouvrir la fiche. Points : N naissance, B baptême, M mariage, S sépulture, D décès, verticaux verts : aujourd'hui.",
                Modifier.padding(top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (pickerOuvert) {
        AlertDialog(
            onDismissRequest = { pickerOuvert = false },
            title = { Text("Ligne de vie de quelle personne ?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    personnes.sortedBy { it.titre() }.forEach { c ->
                        // v0.79 : identifiant unique, compact, aligne a gauche.
                        Text(
                            libelleUnique(c),
                            Modifier.fillMaxWidth().clickable {
                                onChoisir(c.id); pickerOuvert = false
                            }.padding(vertical = 0.dp),
                            fontSize = 13.sp, lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerOuvert = false }) { Text("Fermer") }
            }
        )
    }
}

// Une ligne de vie : à gauche le nom et le lien, à droite le trait de
// vie horizontal (naissance -> fin) sur l'axe des années, avec un point
// par événement. La ligne de la personne choisie est plus haute : ses
// événements portent leur lettre (N, B, M, S, D ou l'initiale de
// l'événement libre).
@Composable
fun LigneVie(
    m: MembreVie,
    ligneChoisie: Boolean,
    anneeMin: Int,
    anneeMax: Int,
    etendue: Float,
    anneeCourante: Int,
    largeurAxe: Dp,
    anneeNai: Int?,
    anneeFin: Int?,
    finBarre: Int?,
    onOuvrir: (Long) -> Unit
) {
    fun x(an: Int): Dp = largeurAxe * (((an - anneeMin).coerceIn(0, anneeMax - anneeMin)).toFloat() / etendue)
    val hauteur = if (ligneChoisie) 46.dp else 32.dp
    val couleurTrait = if (m.p.sexe == "F") CouleurRose else CouleurBleu
    val decennies = (anneeMin..anneeMax).filter { it % 10 == 0 }

    Row(Modifier.fillMaxWidth().height(hauteur).clickable { onOuvrir(m.p.id) }) {
        // Nom et lien, à gauche.
        Column(Modifier.width(84.dp).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(couleurTrait))
                Spacer(Modifier.width(4.dp))
                Text(
                    m.p.titre(),
                    Modifier.weight(1f),
                    fontSize = 10.sp,
                    fontWeight = if (ligneChoisie) FontWeight.Bold else FontWeight.Normal,
                    color = if (ligneChoisie) CouleurVertFonce else Color.Unspecified,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (m.lien.isNotEmpty()) {
                Text(
                    m.lien,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        // La ligne de vie, à droite, sur l'axe des années.
        Box(Modifier.width(largeurAxe).fillMaxHeight()) {
            // Une ligne fine par décennie.
            decennies.forEach { d ->
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = x(d))
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(CouleurGrisBord)
                )
            }
            // Le trait vertical vert de l'année en cours.
            if (anneeCourante in anneeMin..anneeMax) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = x(anneeCourante))
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(CouleurVert)
                )
            }
            if (anneeNai == null || finBarre == null) {
                Text(
                    "sans date de naissance",
                    Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
                    fontSize = 9.sp,
                    color = GrisManquant
                )
            } else {
                // Le trait de vie, de la naissance à la fin.
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = x(anneeNai))
                        .width(x(finBarre) - x(anneeNai))
                        .height(if (ligneChoisie) 12.dp else 8.dp)
                        .background(couleurTrait, RoundedCornerShape(4.dp))
                )
                // Un point par événement connu.
                val evts = mutableListOf<Pair<Int, String>>()
                evts.add(anneeNai to "N")
                anneeDe(m.p.dateBapteme)?.let { evts.add(it to "B") }
                anneeDe(m.p.dateMariage)?.let { evts.add(it to "M") }
                anneeDe(m.p.dateSepulture)?.let { evts.add(it to "S") }
                anneeFin?.let { evts.add(it to "D") }
                m.p.evenementsLibres.forEach { ev ->
                    anneeDe(ev.date)?.let { a ->
                        evts.add(a to (ev.intitule.trim().firstOrNull()?.uppercase() ?: "?"))
                    }
                }
                evts.distinctBy { it.first.toString() + it.second }.forEach { (an, lettre) ->
                    if (ligneChoisie) {
                        Surface(
                            Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = x(an) - 7.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, couleurTrait)
                        ) {
                            Text(
                                lettre,
                                Modifier.padding(2.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CouleurVertFonce
                            )
                        }
                    } else {
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = x(an) - 4.dp)
                                .size(8.dp)
                                .border(1.dp, couleurTrait, CircleShape)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
