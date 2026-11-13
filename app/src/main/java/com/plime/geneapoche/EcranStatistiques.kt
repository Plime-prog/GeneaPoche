package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Ecran Statistiques (v0.100)
// Deux chips : Base (chiffres de l'arbre et qualite de la documentation)
// et Geographie (damier des departements, communes, voyage de la lignee).

private fun un(d: Double?): String =
    if (d == null) "\u2014" else (Math.round(d * 10.0) / 10.0).toString().replace('.', ',')

@Composable
fun EcranStatistiques(
    personnes: List<Personne>,
    citations: List<Citation>,
    idChoisie: Long,
    onOuvrir: (Long) -> Unit,
    onChoisir: (Long) -> Unit
) {
    var vue by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(selected = vue == 0, onClick = { vue = 0 }, label = { Text("Base") })
            FilterChip(selected = vue == 1, onClick = { vue = 1 }, label = { Text("G\u00E9ographie") })
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (vue == 0) {
                StatistiquesBase(personnes, citations, onOuvrir)
            } else {
                EcranGeographie(
                    personnes = personnes,
                    idChoisie = idChoisie,
                    onOuvrir = onOuvrir,
                    onChoisir = onChoisir
                )
            }
        }
    }
}

@Composable
private fun LigneStat(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(valeur, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

// Ligne "x sur y (n %)" avec une barre de progression.
@Composable
private fun LigneTaux(libelle: String, t: Taux) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(libelle, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                t.nombre.toString() + " / " + t.base.toString() + "  (" + t.pourcent.toString() + " %)",
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE6E2DC))
        ) {
            Box(
                Modifier.fillMaxWidth(t.fraction).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(couleurCompteur(t.nombre, t.base))
            )
        }
    }
}

@Composable
private fun CarteStat(titre: String, contenu: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurGrisBord)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(titre, fontWeight = FontWeight.Bold, color = CouleurVertFonce, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            contenu()
        }
    }
}

@Composable
private fun StatistiquesBase(personnes: List<Personne>, citations: List<Citation>, onOuvrir: (Long) -> Unit) {
    if (personnes.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text("Aucune personne pour le moment.")
            Text("Les statistiques appara\u00EEtront d\u00E8s la premi\u00E8re fiche cr\u00E9\u00E9e.")
        }
        return
    }
    val s = remember(personnes, citations, personnes.size) { statsBase(personnes, citations) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp)) {
        CarteStat("\u00C0 compl\u00E9ter") {
            LigneStat("Renseignements manquants (date + lieu de N, M, D)", s.trousManquants.toString())
            LigneTaux(
                "Compl\u00E9tude de l'arbre",
                Taux(s.trousAttendus - s.trousManquants, s.trousAttendus)
            )
        }
        CarteStat("L'arbre en chiffres") {
            LigneStat("Individus", s.individus.toString())
            LigneStat("Familles (couples)", s.familles.toString())
            LigneStat("Anc\u00EAtres (num\u00E9rot\u00E9s)", s.ancetres.toString())
            LigneStat("Descendants", s.descendants.toString())
            LigneStat("G\u00E9n\u00E9rations", s.generations.toString())
            LigneStat("Communes", s.communes.toString())
            LigneStat("Patronymes", s.patronymes.toString())
            LigneStat("M\u00E9tiers", s.metiers.toString())
        }
        CarteStat("D\u00E9mographie") {
            LigneStat("Dur\u00E9e de vie moyenne", if (s.vieMoyenne == null) "\u2014" else un(s.vieMoyenne) + " ans")
            LigneStat("\u00C2ge moyen au mariage", if (s.ageMariageMoyen == null) "\u2014" else un(s.ageMariageMoyen) + " ans")
            LigneStat("Enfants par parent", un(s.enfantsParCouple))
            LigneStat(
                "P\u00E9riode couverte",
                if (s.anneeDebut == null || s.anneeFin == null) "\u2014" else s.anneeDebut.toString() + " \u2013 " + s.anneeFin.toString()
            )
            val a = s.plusAncien
            if (a != null) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        .then(Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anc\u00EAtre le plus ancien", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.material3.TextButton(onClick = { onOuvrir(a.id) }) {
                        Text(a.titre() + " (" + (anneeDe(a.dateNaissance) ?: 0).toString() + ")", fontSize = 13.sp)
                    }
                }
            } else {
                LigneStat("Anc\u00EAtre le plus ancien", "\u2014")
            }
            val l = s.lieuFrequent
            LigneStat("Lieu le plus fr\u00E9quent", if (l == null) "\u2014" else l.first + " (" + l.second.toString() + ")")
            if (s.patronymesTop.isNotEmpty()) {
                LigneStat("Patronymes les plus port\u00E9s", s.patronymesTop.joinToString(", ") { it.first + " (" + it.second.toString() + ")" })
            }
            if (s.metiersTop.isNotEmpty()) {
                LigneStat("M\u00E9tiers les plus fr\u00E9quents", s.metiersTop.joinToString(", ") { it.first + " (" + it.second.toString() + ")" })
            }
        }
        CarteStat("Qualit\u00E9 de la documentation") {
            LigneTaux("Dates de naissance document\u00E9es", s.naissances)
            LigneTaux("Lieux de naissance complets", s.lieuxNaissance)
            LigneTaux("D\u00E9c\u00E8s document\u00E9s (hors vivants pr\u00E9sum\u00E9s)", s.deces)
            LigneTaux("Mariages dat\u00E9s", s.mariages)
            LigneTaux("Mariages avec une source", s.mariagesSources)
            LigneTaux("Personnes avec au moins une source", s.personnesSourcees)
        }
        Spacer(Modifier.height(16.dp))
    }
}
