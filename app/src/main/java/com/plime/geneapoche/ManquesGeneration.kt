package com.plime.geneapoche

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// v0.155 : SUIVI DES MANQUES PAR GENERATION (ascendants du de cujus).
// Pour chaque generation : combien d'ancetres connus sur combien possibles
// (2 puissance n), et pour chaque ancetre ce qu'il reste a trouver. Calcul pur,
// local, a l'affichage ; rien n'est enregistre. Les conjoints sont ignores.

class LigneManque(val personne: Personne, val manques: List<String>)

class GenerationManques(
    val numero: Int,
    val attendus: Long,
    val connus: Int,
    val lignes: List<LigneManque>
) {
    val pourcent: Int get() = if (attendus <= 0L) 0 else ((connus * 100L) / attendus).toInt()
}

fun manquesDe(p: Personne): List<String> {
    val m = mutableListOf<String>()
    if (p.dateNaissance.isBlank()) m.add("naissance")
    if (p.dateMariage.isBlank()) m.add("mariage")
    if (p.dateDeces.isBlank() && decesAffichable(p)) m.add("décès")
    if (p.pere == 0L) m.add("père")
    if (p.mere == 0L) m.add("mère")
    return m
}

fun manquesParGeneration(personnes: List<Personne>, maxGenerations: Int = 12): List<GenerationManques> {
    val racine = personnes.filter { it.sosa.trim().toLongOrNull() != null }
        .minByOrNull { it.sosa.trim().toLong() } ?: return emptyList()
    val parId = personnes.associateBy { it.id }
    val vus = hashSetOf(racine.id)
    var niveau = listOf(racine)
    val res = mutableListOf<GenerationManques>()
    var g = 0
    while (niveau.isNotEmpty() && g < maxGenerations) {
        val lignes = niveau.map { LigneManque(it, manquesDe(it)) }.filter { it.manques.isNotEmpty() }
            .sortedBy { it.personne.sosa.trim().toLongOrNull() ?: Long.MAX_VALUE }
        res.add(GenerationManques(g, 1L shl g, niveau.size, lignes))
        niveau = niveau.flatMap { listOf(it.pere, it.mere) }
            .filter { it != 0L && it !in vus }.distinct().mapNotNull { parId[it] }
        vus.addAll(niveau.map { it.id })
        g += 1
    }
    return res
}

fun nomGeneration(g: Int): String = when (g) {
    0 -> "De cujus"
    1 -> "Parents"
    2 -> "Grands-parents"
    3 -> "Arrière-grands-parents"
    else -> "Génération " + (g + 1).toString() + " (" + (if (g < 7) "x" else "") + "arrière" + ")"
}

@Composable
fun OutilManques(personnes: List<Personne>, onOuvrirPersonne: (Long) -> Unit) {
    val generations = manquesParGeneration(personnes)
    Text("Suivi des manques", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Text(
        "Pour chaque génération d'ancêtres : combien sont connus, et ce qu'il reste à chercher. Touchez un ancêtre pour ouvrir sa fiche. Le contrôle de cohérence est dans le Journal (Diagnostic).",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    if (generations.isEmpty()) {
        Text("Définissez d'abord le de cujus (n° Sosa) pour suivre les manques.")
        return
    }
    generations.forEach { gen ->
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                "G" + gen.numero.toString() + " – " + (if (gen.numero <= 3) nomGeneration(gen.numero) else "Ancêtres") +
                    " : " + gen.connus.toString() + " sur " + gen.attendus.toString() + " (" + gen.pourcent.toString() + " %)",
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { (gen.connus.toFloat() / gen.attendus.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            gen.lignes.forEach { l ->
                Text(
                    (l.personne.numeroBrut().ifBlank { "?" }) + " · " + l.personne.titre() + " — à trouver : " + l.manques.joinToString(", "),
                    Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable { onOuvrirPersonne(l.personne.id) }.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
