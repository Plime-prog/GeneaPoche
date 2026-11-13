package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Recherche avancee (v0.100, 12.7)
// Etend la recherche par nom et lieu : periode, lieu, statut de
// verification et acte manquant. Resultats CONDENSES : une ligne par
// personne avec un petit triangle a droite ; le toucher deplie un bloc.

private val TypesManquants = listOf(
    "N" to "Naissance", "B" to "Bapt\u00EAme", "M" to "Mariage", "D" to "D\u00E9c\u00E8s", "S" to "S\u00E9pulture"
)

@Composable
fun RechercheAvancee(personnes: List<Personne>, onOuvrirPersonne: (Long) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    var texte by remember { mutableStateOf("") }
    var lieu by remember { mutableStateOf("") }
    var de by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var statut by remember { mutableStateOf("") }
    var manquants by remember { mutableStateOf(setOf<String>()) }
    var depliees by remember { mutableStateOf(setOf<Long>()) }

    val filtres = FiltresRecherche(
        texte = texte, lieu = lieu,
        anneeDe = de.trim().toIntOrNull(), anneeA = a.trim().toIntOrNull(),
        statut = statut, manquants = manquants
    )
    val resultats = if (filtres.actif) filtrerPersonnes(personnes, filtres)
        .sortedWith { x, y -> comparerNumeros(cleNumero(x), cleNumero(y)) } else emptyList()

    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurGrisBord)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { ouvert = !ouvert },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recherche avanc\u00E9e" + (if (filtres.actif) " (" + resultats.size.toString() + ")" else ""),
                    Modifier.weight(1f), fontWeight = FontWeight.Bold, color = CouleurVertFonce
                )
                Text(if (ouvert) "\u25B2" else "\u25BC", color = CouleurVertFonce)
            }
            if (ouvert) {
                OutlinedTextField(
                    value = texte, onValueChange = { texte = it },
                    label = { Text("Nom, pr\u00E9nom, m\u00E9tier ou lieu") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = lieu, onValueChange = { lieu = it },
                    label = { Text("Lieu (commune)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = de, onValueChange = { de = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Ann\u00E9e de") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = a, onValueChange = { a = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("\u00E0") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("Statut de v\u00E9rification", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = statut == "", onClick = { statut = "" }, label = { Text("Tous") })
                    FilterChip(selected = statut == "certifiee", onClick = { statut = "certifiee" }, label = { Text("Certifi\u00E9e") })
                    FilterChip(selected = statut == "non", onClick = { statut = "non" }, label = { Text("Non certifi\u00E9e") })
                }
                Spacer(Modifier.height(6.dp))
                Text("Acte manquant (au moins un)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypesManquants.forEach { (code, lib) ->
                        FilterChip(
                            selected = manquants.contains(code),
                            onClick = { manquants = if (manquants.contains(code)) manquants - code else manquants + code },
                            label = { Text(lib) }
                        )
                    }
                }
                if (filtres.actif) {
                    TextButton(onClick = {
                        texte = ""; lieu = ""; de = ""; a = ""; statut = ""; manquants = emptySet()
                    }) { Text("Effacer les filtres", fontSize = 12.sp) }
                }
            }
            if (filtres.actif) {
                Spacer(Modifier.height(6.dp))
                Text(
                    resultats.size.toString() + " personne(s)" + (if (resultats.size > 40) " (40 premi\u00E8res)" else ""),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                resultats.take(40).forEach { p ->
                    val deplie = depliees.contains(p.id)
                    val (n, total) = indicateurPersonne(p)
                    Surface(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF7F5F1)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 40.dp)
                                    .clickable { depliees = if (deplie) depliees - p.id else depliees + p.id },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    (if (p.sosa.isNotBlank()) p.sosa.trim() + " \u00B7 " else "") + p.titre(),
                                    Modifier.weight(1f), fontSize = 14.sp
                                )
                                Surface(shape = RoundedCornerShape(50), color = couleurCompteur(n, total)) {
                                    Text(
                                        "  " + n.toString() + "/" + total.toString() + "  ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White, fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(if (deplie) "\u25BC" else "\u25B6", fontSize = 12.sp, color = CouleurVertFonce)
                            }
                            if (deplie) {
                                val lignes = mutableListOf<String>()
                                val nais = p.dateNaissance.ifBlank { if (p.naissanceCalculee.isNotBlank()) p.naissanceCalculee + " (calcul\u00E9e)" else "" }
                                if (nais.isNotBlank() || p.lieuNaissance.isNotBlank())
                                    lignes.add("N. " + (if (nais.isNotBlank()) dateAffichee(nais) else "?") +
                                        (if (p.lieuNaissance.isNotBlank()) " \u00E0 " + lieuAffiche(p.lieuNaissance) else ""))
                                if (p.dateMariage.isNotBlank() || p.lieuMariage.isNotBlank())
                                    lignes.add("M. " + (if (p.dateMariage.isNotBlank()) dateAffichee(p.dateMariage) else "?") +
                                        (if (p.lieuMariage.isNotBlank()) " \u00E0 " + lieuAffiche(p.lieuMariage) else ""))
                                if (p.dateDeces.isNotBlank() || p.lieuDeces.isNotBlank())
                                    lignes.add("D. " + (if (p.dateDeces.isNotBlank()) dateAffichee(p.dateDeces) else "?") +
                                        (if (p.lieuDeces.isNotBlank()) " \u00E0 " + lieuAffiche(p.lieuDeces) else ""))
                                lignes.forEach { Text(it, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)) }
                                val manque = listOf("N", "M", "D").filter { acteManquant(p, it) }
                                if (manque.isNotEmpty()) {
                                    Text(
                                        "\u00C0 compl\u00E9ter : " + manque.joinToString(", ") { t ->
                                            TypesManquants.firstOrNull { it.first == t }?.second ?: t
                                        },
                                        fontSize = 12.sp, color = CouleurRouge, modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Text(
                                    if (identiteCertifiee(p)) "Identit\u00E9 certifi\u00E9e" else "Identit\u00E9 non certifi\u00E9e",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                TextButton(onClick = { onOuvrirPersonne(p.id) }) { Text("Ouvrir la fiche", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
