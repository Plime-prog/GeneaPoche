package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Journal de recherches
// Les donnees manquantes (grisees dans les fiches) forment les recherches
// a faire. D\u00E8s qu'une donnee est saisie ou qu'une image est ajoutee,
// la recherche est archivee automatiquement (horodatee). Aucune validation.

// Une recherche a faire, deduite des donnees manquantes d'une personne.
data class Suggestion(
    val cle: String,        // cle technique (stockee dans la piste)
    val titre: String,      // texte complet affichable
    val type: String,      // code evenement : N B M D S P(pere) ME(mere) C(conjoint) A(autre)
    val lieu: String = "",
    val dept: String = "",
    val debut: String = "", // debut de la plage d'annees ciblee
    val fin: String = ""
)

fun dateVersAnnee(date: String): Int? {
    return Regex("[0-9]{4}").find(date)?.value?.toIntOrNull()
}

fun titreCourt(p: Personne): String {
    val t = p.titre().trim()
    return if (t.isEmpty()) "(sans nom)" else t
}

// Un acte du type avec une IMAGE (les actes transcrits sans image ont un contenu).
fun aUneImage(actes: List<Acte>, personneId: Long, type: String): Boolean {
    return actes.any { it.personneId == personneId && it.type == type && it.contenu.isBlank() }
}

// Toutes les donnees manquantes d'une personne : acte absent (ni date ni image),
// parents, conjoint, enfants.
fun suggestionsPour(p: Personne, personnes: List<Personne>, actes: List<Acte>): List<Suggestion> {
    val res = mutableListOf<Suggestion>()

    fun suffixe(lieu: String, dept: String): String {
        var s = ""
        if (lieu.isNotBlank()) s = " \u00E0 " + lieu.trim()
        if (dept.isNotBlank()) s = s + " (" + dept.trim() + ")"
        return s
    }

    val anneeN = dateVersAnnee(p.dateNaissance)
    val lieuN = p.lieuNaissance.trim()
    val deptN = p.deptNaissance.trim()

    // --- Actes de la personne : manquants = pas de date ET pas d'image
    if (p.dateNaissance.isBlank() && !aUneImage(actes, p.id, "N")) {
        res.add(Suggestion("naissance", "Rechercher l'acte de naissance de " + titreCourt(p) + (anneeN?.let { " (" + it.toString() + ")" } ?: "") + suffixe(lieuN, deptN), "N", lieuN, deptN))
    }
    if ((p.blocsOuverts.contains("bapteme") || p.dateBapteme.isNotBlank()) && p.dateBapteme.isBlank() && !aUneImage(actes, p.id, "B")) {
        res.add(Suggestion("bapteme", "Rechercher le bapt\u00EAme de " + titreCourt(p) + suffixe(p.lieuBapteme.trim(), p.deptBapteme.trim()), "B", p.lieuBapteme.trim(), p.deptBapteme.trim()))
    }
    if (p.dateMariage.isBlank() && !aUneImage(actes, p.id, "M")) {
        res.add(Suggestion("mariage", "Rechercher le mariage de " + titreCourt(p) + suffixe(p.lieuMariage.trim(), p.deptMariage.trim()), "M", p.lieuMariage.trim(), p.deptMariage.trim()))
    }
    if (p.dateDeces.isBlank() && !aUneImage(actes, p.id, "D")) {
        res.add(Suggestion("deces", "Rechercher l'acte de d\u00E9c\u00E8s de " + titreCourt(p) + suffixe(p.lieuDeces.trim(), p.deptDeces.trim()), "D", p.lieuDeces.trim(), p.deptDeces.trim()))
    }
    if ((p.blocsOuverts.contains("sepulture") || p.dateSepulture.isNotBlank()) && p.dateSepulture.isBlank() && !aUneImage(actes, p.id, "S")) {
        res.add(Suggestion("sepulture", "Rechercher la s\u00E9pulture de " + titreCourt(p) + suffixe(p.lieuSepulture.trim(), p.deptSepulture.trim()), "S", p.lieuSepulture.trim(), p.deptSepulture.trim()))
    }

    // --- Parents : plage d'annees deduite de la naissance de l'enfant
    if (p.pere == 0L && p.nom.isNotBlank()) {
        val debut = anneeN?.let { (it - 45).toString() } ?: ""
        val fin = anneeN?.let { (it - 20).toString() } ?: ""
        res.add(Suggestion("pere", "Rechercher son p\u00E8re : un " + p.nom.trim() + (if (debut.isNotEmpty()) { " n\u00E9 entre " + debut + " et " + fin } else { "" }) + suffixe(lieuN, deptN), "P", lieuN, deptN, debut, fin))
    }
    if (p.mere == 0L) {
        val debut = anneeN?.let { (it - 40).toString() } ?: ""
        val fin = anneeN?.let { (it - 15).toString() } ?: ""
        res.add(Suggestion("mere", "Rechercher sa m\u00E8re" + (if (debut.isNotEmpty()) { " : n\u00E9e entre " + debut + " et " + fin } else { "" }) + suffixe(lieuN, deptN), "ME", lieuN, deptN, debut, fin))
    }

    // --- Conjoint et enfants
    if (p.conjoint == 0L && p.conjointsSupp.isEmpty()) {
        res.add(Suggestion("conjoint", "Rechercher le conjoint de " + titreCourt(p), "C"))
    }
    if (p.enfants.isEmpty()) {
        val anneeM = dateVersAnnee(p.dateMariage)
        val debut = anneeM?.toString() ?: ""
        val fin = anneeM?.let { (it + 25).toString() } ?: ""
        res.add(Suggestion("enfants", "Rechercher les enfants de " + titreCourt(p) + (if (debut.isNotEmpty()) { " (apr\u00E8s le mariage de " + debut + ")" } else { "" }), "A", debut = debut, fin = fin))
    }

    return res
}

// Recalcule le texte d'une recherche a partir de l'etat actuel des donnees
// (les donnees saisies entre-temps sont immediatement refl\u00E9t\u00E9es).
fun textePisteActuel(piste: Piste, personnes: List<Personne>, actes: List<Acte>): String {
    if (piste.type != "A" || piste.categorie.isEmpty()) return piste.texte
    val pers = personneParId(personnes, piste.personneId) ?: return piste.texte
    val suggestion = suggestionsPour(pers, personnes, actes).firstOrNull { it.cle == piste.categorie }
    return suggestion?.titre ?: piste.texte
}

// Libelle d'une note archivee.
fun libelleRecherche(r: Recherche): String {
    return when (r.resultat) {
        "T" -> "TROUV\u00C9 \u2013 " + r.note
        "X" -> r.note
        else -> "Rien trouv\u00E9" +
            (if (r.page.isNotBlank()) { " jusqu'\u00E0 la page " + r.page } else { "" }) +
            (if (r.repriseVille.isNotBlank()) { ", TD de " + r.repriseVille } else { "" }) +
            (if (r.repriseAnnee.isNotBlank()) { ", reprendre \u00E0 partir de " + r.repriseAnnee } else { "" }) +
            (if (r.note.isNotBlank()) { " \u2013 " + r.note } else { "" })
    }
}

// ---------------------------------------------------------------- Ecran Journal

@Composable
fun EcranJournal(
    pistes: List<Piste>,
    personnes: List<Personne>,
    actes: List<Acte>,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    ouvrirLien: (String) -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit,
    onSauver: () -> Unit
) {
    val ouvertes = pistes.filter { !it.faite }
        .sortedWith(compareBy({ it.personneId }, { it.creation }))
    val archivees = pistes.filter { it.faite }.sortedByDescending { it.cloture }.take(30)

    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Journal de recherches", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CouleurVertFonce)
                Text(
                    "Les informations manquantes (en gris dans les fiches) sont list\u00E9es ici. D\u00E8s qu'une donn\u00E9e est saisie ou qu'une image est ajout\u00E9e, la recherche est archiv\u00E9e.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (ouvertes.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF9CBF9A))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Tout est renseign\u00E9 !", fontWeight = FontWeight.Bold)
                        Text(
                            "Aucune information manquante pour le moment.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        ouvertes.forEachIndexed { index, piste ->
            item(key = "ouverte" + piste.id.toString()) {
                CarteRecherche(
                    piste = piste,
                    numero = index + 1,
                    personnes = personnes,
                    actes = actes,
                    onSupprimer = onSupprimer,
                    onOuvrirPersonne = onOuvrirPersonne,
                    ouvrirLien = ouvrirLien,
                    ouvrirEtatCivil = ouvrirEtatCivil,
                    onSauver = onSauver
                )
            }
        }

        if (archivees.isNotEmpty()) {
            item {
                Text(
                    "Recherches archiv\u00E9es (" + archivees.size.toString() + ")",
                    Modifier.padding(top = 12.dp, bottom = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = CouleurVertFonce
                )
            }
            archivees.forEach { ps ->
                item(key = "archivee" + ps.id.toString()) {
                    val pers = personneParId(personnes, ps.personneId)
                    TexteAvecLiens(
                        "\u2022 " + formaterDateHeure(ps.cloture) + " \u2013 " + ps.texte +
                            (if (pers != null) " (" + titreCourt(pers) + ")" else ""),
                        ouvrirLien
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------- Carte d'une recherche
// Rappel, fiche de la personne, icone de recherche Internet (AD), zone de
// texte libre pour la recherche en cours. Pas de validation.

@Composable
fun CarteRecherche(
    piste: Piste,
    numero: Int,
    personnes: List<Personne>,
    actes: List<Acte>,
    onSupprimer: (Piste) -> Unit,
    onOuvrirPersonne: (Long) -> Unit,
    ouvrirLien: (String) -> Unit,
    ouvrirEtatCivil: (String, Int?, String, String) -> Unit,
    onSauver: () -> Unit
) {
    val pers = personneParId(personnes, piste.personneId)
    val texte = textePisteActuel(piste, personnes, actes)

    // Categories qui correspondent a un acte : la recherche AD a un sens.
    val categorieActe = piste.categorie in listOf("naissance", "bapteme", "mariage", "deces", "sepulture")
    val libelleActe = when (piste.categorie) {
        "naissance" -> "Naissance"
        "bapteme" -> "Bapt\u00EAme"
        "mariage" -> "Mariage"
        "deces" -> "D\u00E9c\u00E8s"
        "sepulture" -> "S\u00E9pulture"
        else -> ""
    }

    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF9CBF9A))
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Pastille numerotee
                Box(
                    Modifier.size(28.dp).background(CouleurVert, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(numero.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    texte,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (categorieActe) {
                    IconButton(
                        onClick = {
                            ouvrirEtatCivil(
                                piste.lieu,
                                piste.dept.toIntOrNull() ?: numeroDepartement(piste.lieu),
                                libelleActe,
                                piste.debut
                            )
                        },
                        enabled = piste.lieu.isNotBlank() || piste.dept.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Rechercher aux AD")
                    }
                }
                IconButton(onClick = { onSupprimer(piste) }, Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", Modifier.size(16.dp))
                }
            }

            // Personne concernee
            if (pers != null) {
                TextButton(onClick = { onOuvrirPersonne(piste.personneId) }) {
                    Text("Fiche de " + titreCourt(pers), style = MaterialTheme.typography.labelMedium)
                }
            }

            // Zone de texte pour la recherche en cours
            var note by remember(piste.id) { mutableStateOf(piste.enCours) }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Recherche en cours : pages, c\u00F4tes, notes...") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { piste.enCours = note.trim(); onSauver() }) {
                    Text("Enregistrer la note")
                }
            }

            // Notes archivees horodatees de cette recherche
            piste.historique.take(3).forEach { r ->
                TexteAvecLiens("   \u2022 " + formaterDateHeure(r.date) + " \u2013 " + libelleRecherche(r), ouvrirLien)
            }
        }
    }
}

// ---------------------------------------------------------------- Texte avec URL cliquables

@Composable
fun TexteAvecLiens(texte: String, ouvrirLien: (String) -> Unit) {
    val motif = Regex("https?://\\S+|www\\.\\S+")
    val annote: AnnotatedString = buildAnnotatedString {
        append(texte)
        for (m in motif.findAll(texte)) {
            val deb = m.range.first
            val fin = m.range.last + 1
            addStyle(SpanStyle(color = CouleurVertFonce, textDecoration = TextDecoration.Underline), deb, fin)
            addStringAnnotation("lien", m.value, deb, fin)
        }
    }
    ClickableText(
        text = annote,
        style = MaterialTheme.typography.bodySmall,
        onClick = { offset ->
            annote.getStringAnnotations("lien", offset, offset).firstOrNull()?.let { a ->
                ouvrirLien(a.item)
            }
        }
    )
}
