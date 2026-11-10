package com.plime.geneapoche

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as itemsGrille
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Onglet Geographie (v0.93, 12.5)
// Damier de France (une case par departement, coloree par nombre
// d'ancetres), tops communes, et "voyage de la lignee" de la personne
// choisie. 100 % hors ligne, sans carte vectorielle.

// Couleur du damier : beige neutre a 0 ancetre, rouge de plus en plus
// fonce selon la part du maximum.
fun couleurIntensite(effectif: Int, max: Int): Color {
    if (effectif <= 0) return Color(0xFFF0EDE9)
    val part = if (max <= 1) 1f else (effectif.toFloat() / max.toFloat()).coerceIn(0.15f, 1f)
    return lerp(Color(0xFFF2B8B5), Color(0xFF8B1A1A), part)
}

@Composable
fun EcranGeographie(
    personnes: List<Personne>,
    idChoisie: Long,
    onOuvrir: (Long) -> Unit,
    onChoisir: (Long) -> Unit
) {
    val repartition = remember(personnes) { repartitionDepartements(personnes) }
    val communes = remember(personnes) { repartitionCommunes(personnes) }
    val maxDept = repartition.values.maxOfOrNull { it.size } ?: 0
    var deptChoisi by remember { mutableStateOf<String?>(null) }
    val choisi = personnes.firstOrNull { it.id == idChoisie }

    if (personnes.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text("Aucune personne pour le moment.")
            Text("La r\u00E9partition g\u00E9ographique appara\u00EEtra d\u00E8s la premi\u00E8re fiche cr\u00E9\u00E9e.")
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "R\u00E9partition g\u00E9ographique",
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            personnes.size.toString() + " personnes \u2013 " +
                repartition.size.toString() + " d\u00E9partement" + (if (repartition.size > 1) "s" else "") + " repr\u00E9sent\u00E9" + (if (repartition.size > 1) "s" else ""),
            Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // ---- Damier de France : une case par departement.
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(220.dp)
        ) {
            itemsGrille(CodesDepartements) { code ->
                val effectif = repartition[code]?.size ?: 0
                val actif = deptChoisi == code
                Box(
                    Modifier
                        .padding(2.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (effectif > 0) couleurIntensite(effectif, maxDept) else Color(0xFFF0EDE9))
                        .clickable(enabled = effectif > 0) { deptChoisi = if (actif) null else code },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            code,
                            fontSize = if (code.length >= 3) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (effectif > 0) Color.White else Color(0xFF9E9489)
                        )
                        if (effectif > 0) {
                            Text(
                                effectif.toString(),
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        // ---- Detail du departement clique.
        if (deptChoisi != null) {
            val code = deptChoisi!!
            val liste = repartition[code] ?: emptyList()
            Text(
                "D\u00E9partement " + code + (if (nomDepartement(code).isNotEmpty()) " \u2013 " + nomDepartement(code) else "") + " : " + liste.size.toString() + " personne" + (if (liste.size > 1) "s" else ""),
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                color = CouleurRouge
            )
            LazyColumn(Modifier.fillMaxWidth().weight(0.35f).padding(horizontal = 8.dp)) {
                items(liste, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOuvrir(p.id) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            libelleUnique(p),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        // ---- Top communes de naissance.
        val topCommunes = communes.entries.sortedByDescending { it.value.size }.take(3)
        if (topCommunes.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Communes les plus repr\u00E9sent\u00E9es : ", style = MaterialTheme.typography.bodySmall)
                topCommunes.forEach { (commune, liste) ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CouleurVertPale,
                        modifier = Modifier.padding(horizontal = 2.dp).clickable { onOuvrir(liste.first().id) }
                    ) {
                        Text(
                            "  " + commune + " (" + liste.size.toString() + ")  ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CouleurVertFonce
                        )
                    }
                }
            }
        }
        // ---- Voyage de la lignee de la personne choisie.
        if (choisi != null) {
            val etapes = remember(choisi.id, choisi.evenementsLibres.size) { etapesVoyage(choisi) }
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors()
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        "Voyage de la lign\u00E9e \u2013 " + choisi.titre(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (etapes.isEmpty()) {
                        Text(
                            "Aucun lieu connu pour cette personne.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        var precedent: String? = null
                        etapes.forEach { e ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                                val prec = precedent
                                val changement = prec != null && prec.isNotEmpty() && e.lieu.isNotEmpty() && prec != e.lieu
                                Text(
                                    (if (changement) "\u2192 " else "   ") + e.intitule,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (changement) CouleurRouge else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    (if (e.date.isNotBlank()) dateAffichee(e.date) + " " else "") + e.lieu,
                                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (e.lieu.isNotEmpty()) precedent = e.lieu
                        }
                    }
                }
            }
        } else {
            Text(
                "Touchez une personne (liste, arbre ou ligne de vie) pour voir son voyage.",
                Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
