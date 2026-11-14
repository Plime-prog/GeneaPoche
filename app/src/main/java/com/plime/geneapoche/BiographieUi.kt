package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Composants de la fiche (v0.103)
// En-tete de synthese, cadres pliables (replies par defaut), famille
// cliquable et recit de l'onglet Biographie.

@Composable
fun LienFamille(libelle: String, q: Personne, onOuvrir: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onOuvrir(q.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(libelle, Modifier.width(72.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PastilleNumero(p = q, diametre = 26)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(q.titre(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val nd = listOf(
                if (q.dateNaissance.isNotBlank()) "N. " + dateAffichee(q.dateNaissance.trim()) else "",
                if (q.dateDeces.isNotBlank()) "D. " + dateAffichee(q.dateDeces.trim()) else ""
            ).filter { it.isNotEmpty() }
            if (nd.isNotEmpty()) Text(nd.joinToString(" \u2013 "), fontSize = 12.sp)
        }
        Text("\u25B6", fontSize = 12.sp, color = CouleurVertFonce)
    }
}

// Cadre replie par defaut : le toucher de l'en-tete le deplie.
@Composable
fun CadrePliable(
    titre: String,
    resume: String = "",
    cle: Any = titre,
    contenu: @Composable () -> Unit
) {
    var ouvert by remember(cle) { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurBordVert)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ouvert = !ouvert },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titre, fontWeight = FontWeight.Bold, color = CouleurVertFonce)
                if (resume.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        resume, Modifier.weight(1f), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Text(if (ouvert) "\u25B2" else "\u25BC", color = CouleurVertFonce)
            }
            if (ouvert) contenu()
        }
    }
}

// En-tete de synthese de la fiche.
@Composable
fun EnteteSynthese(p: Personne, nbRecherches: Int) {
    val an = anneeDe(p.dateNaissance.ifBlank { p.naissanceCalculee })
    val ad = anneeDe(p.dateDeces)
    val vivant = presumeVivant(p)
    val plage = (an?.toString() ?: "?") + " \u2013 " + (ad?.toString() ?: if (vivant) "" else "?")
    val age = if (an != null && ad != null && ad >= an) " \u00B7 " + (ad - an).toString() + " ans" else ""
    val f = p.sexe == "F"
    val lignes = mutableListOf<String>()
    val n = detail(p.dateNaissance, p.lieuNaissance).trim()
    if (n.isNotEmpty()) lignes.add((if (f) "N\u00E9e " else "N\u00E9 ") + n)
    val m = detail(p.dateMariage, p.lieuMariage).trim()
    if (m.isNotEmpty()) lignes.add((if (f) "Mari\u00E9e " else "Mari\u00E9 ") + m)
    val d = detail(p.dateDeces, p.lieuDeces).trim()
    if (d.isNotEmpty()) lignes.add((if (f) "D\u00E9c\u00E9d\u00E9e " else "D\u00E9c\u00E9d\u00E9 ") + d)
    val (nInd, totalInd) = indicateurPersonne(p)
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        border = BorderStroke(1.dp, CouleurBordVert)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(plage + age + (if (vivant && ad == null) "  (vivant pr\u00E9sum\u00E9)" else ""),
                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CouleurVertFonce)
            lignes.forEach { Text(it, Modifier.padding(top = 2.dp), fontSize = 13.sp) }
            if (p.profession.isNotBlank()) Text(p.profession.trim(), Modifier.padding(top = 2.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (p.domicile.isNotBlank()) Text("Domicile : " + p.domicile.trim(), Modifier.padding(top = 2.dp), fontSize = 12.sp)
            if (p.situation.isNotBlank()) Text("Situation : " + p.situation.trim(), Modifier.padding(top = 2.dp), fontSize = 12.sp)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pastille(if (identiteCertifiee(p)) "Identit\u00E9 certifi\u00E9e" else "Identit\u00E9 \u00E0 v\u00E9rifier",
                    if (identiteCertifiee(p)) CouleurVert else CouleurRouge)
                Pastille(nInd.toString() + "/" + totalInd.toString() + " renseign\u00E9s", couleurCompteur(nInd, totalInd))
                if (nbRecherches > 0) Pastille(nbRecherches.toString() + " recherche" + (if (nbRecherches > 1) "s" else ""), CouleurOrange)
            }
        }
    }
}

@Composable
private fun Pastille(texte: String, couleur: Color) {
    Surface(shape = RoundedCornerShape(50), color = couleur) {
        Text(
            "  " + texte + "  ", style = MaterialTheme.typography.labelSmall,
            color = Color.White, fontWeight = FontWeight.Bold
        )
    }
}

// Conjoints et enfants (fiche) : chaque ligne ouvre la fiche.
@Composable
fun CarteUnionsEnfants(
    p: Personne,
    personnes: List<Personne>,
    onOuvrir: (Long) -> Unit,
    onAjouterEnfant: () -> Unit
) {
    val conjoints = p.tousConjoints().mapNotNull { personneParId(personnes, it) }
    val enfants = p.enfants.mapNotNull { personneParId(personnes, it) }
    val resume = conjoints.size.toString() + (if (conjoints.size > 1) " unions" else " union") +
        " \u00B7 " + enfants.size.toString() + (if (enfants.size > 1) " enfants" else " enfant")
    CadrePliable("Conjoint(s) et enfants", resume, cle = p.id) {
        conjoints.forEach { q -> LienFamille(if (q.sexe == "F") "\u00C9pouse" else "\u00C9poux", q, onOuvrir) }
        enfants.forEach { q -> LienFamille(if (q.sexe == "F") "Fille" else if (q.sexe == "M") "Fils" else "Enfant", q, onOuvrir) }
        if (conjoints.isEmpty() && enfants.isEmpty()) {
            Text("Aucun conjoint ni enfant renseign\u00E9.", fontSize = 13.sp, color = GrisManquant)
        }
        TextButton(onClick = onAjouterEnfant) { Text("Ajouter un enfant", fontWeight = FontWeight.Bold) }
    }
}

// Biographie : parents et conjoints (les enfants sont dans la chronologie).
@Composable
fun CarteFamilleBio(p: Personne, personnes: List<Personne>, onOuvrir: (Long) -> Unit) {
    val parents = listOf("P\u00E8re" to p.pere, "M\u00E8re" to p.mere)
        .mapNotNull { (l, id) -> personneParId(personnes, id)?.let { l to it } }
    val conjoints = p.tousConjoints().mapNotNull { personneParId(personnes, it) }
    if (parents.isEmpty() && conjoints.isEmpty()) return
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurGrisBord)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text("Entourage", fontWeight = FontWeight.Bold, color = CouleurVertFonce)
            parents.forEach { (l, q) -> LienFamille(l, q, onOuvrir) }
            conjoints.forEach { q -> LienFamille(if (q.sexe == "F") "\u00C9pouse" else "\u00C9poux", q, onOuvrir) }
        }
    }
}

@Composable
fun CarteRecitBio(p: Personne, personnes: List<Personne>) {
    val recit = remember(p, personnes) { recitBiographie(p, personnes) }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CouleurGrisBord)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text("Sa vie", fontWeight = FontWeight.Bold, color = CouleurVertFonce)
            recit.forEach { Text(it, Modifier.padding(top = 4.dp), fontSize = 14.sp) }
            phrasesHistoriquesPersonne(p).forEach {
                Text(it, Modifier.padding(top = 4.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
