package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onChoisir: (Long) -> Unit,
    onOuvrirBio: (Long) -> Unit = onOuvrir
) {
    val repartition = remember(personnes) { repartitionDepartements(personnes) }
    val maxDept = repartition.values.maxOfOrNull { it.size } ?: 0
    var deptChoisi by rememberMemo<String?>("geo_dept", null)
    // v0.141 : aucune commune selectionnee par defaut.
    var communeChoisie by rememberMemo<String?>("geo_commune", null)

    if (personnes.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text("Aucune personne pour le moment.")
            Text("La r\u00E9partition g\u00E9ographique appara\u00EEtra d\u00E8s la premi\u00E8re fiche cr\u00E9\u00E9e.")
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            // v0.141 : intitule "Stat."
            "Stat.",
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
        // ---- Damier : UNIQUEMENT les departements utilises (v0.149).
        val codesUtilises = departementsUtilises(repartition)
        val lignesDamier = (codesUtilises.size + 5) / 6
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height((lignesDamier * 38 + 4).dp),
            userScrollEnabled = false
        ) {
            itemsGrille(codesUtilises) { code ->
                val effectif = repartition[code]?.size ?: 0
                val actif = deptChoisi == code
                Box(
                    Modifier
                        .padding(2.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (effectif > 0) couleurIntensite(effectif, maxDept) else Color(0xFFF0EDE9))
                        .clickable(enabled = effectif > 0) { communeChoisie = null; deptChoisi = if (actif) null else code },
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
        // ---- Detail du departement touche (v0.141) : communes DE CE DEPARTEMENT
        // seulement (aucune commune selectionnee par defaut), puis les personnes
        // concernees dans un bloc replie, deployable ; toucher une personne ouvre
        // sa biographie.
        if (deptChoisi != null) {
            val code = deptChoisi!!
            val liste = repartition[code] ?: emptyList()
            Text(
                "D\u00E9partement " + code + (if (nomDepartement(code).isNotEmpty()) " \u2013 " + nomDepartement(code) else "") + " : " + liste.size.toString() + " personne" + (if (liste.size > 1) "s" else ""),
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                color = CouleurRouge
            )
            val communesDept = remember(code, personnes) {
                repartitionCommunes(liste).entries.sortedByDescending { it.value.size }
            }
            if (communesDept.isNotEmpty()) {
                Text(
                    "Communes de ce d\u00E9partement",
                    Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    communesDept.forEach { (commune, l) ->
                        val actif = communeChoisie == commune
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (actif) CouleurVertFonce else CouleurVertPale,
                            modifier = Modifier.padding(horizontal = 2.dp).clickable { communeChoisie = if (actif) null else commune }
                        ) {
                            Text(
                                "  " + commune + " (" + l.size.toString() + ")  ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (actif) Color.White else CouleurVertFonce
                            )
                        }
                    }
                }
            }
            val concernees = communeChoisie?.let { c -> communesDept.firstOrNull { it.key == c }?.value } ?: liste
            var deploye by rememberMemo("geo_deploye_" + code + "_" + communeChoisie, false)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CouleurGrisBord)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { deploye = !deploye }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Personnes concern\u00E9es : " + concernees.size.toString() +
                                (communeChoisie?.let { " (" + it + ")" } ?: ""),
                            Modifier.weight(1f), fontWeight = FontWeight.Bold, color = CouleurVertFonce
                        )
                        Text(if (deploye) "\u25B2" else "\u25BC", color = CouleurVertFonce)
                    }
                    if (deploye) {
                        concernees.forEach { p ->
                            Text(
                                libelleUnique(p),
                                Modifier.fillMaxWidth().clickable { onOuvrirBio(p.id) }.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
