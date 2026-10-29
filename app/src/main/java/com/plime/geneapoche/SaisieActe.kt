package com.plime.geneapoche

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Saisie par acte
// Transcription directe d'un acte d'etat civil N/B/M/D/S.
// Alimente la fiche Personne (dates, lieux, professions, liens pere/mere/conjoint),
// la numerotation est recalculee automatiquement, et le contenu integral
// (ages, temoins, signataires) est memorise dans l'acte lui-meme.

data class TemoinSaisie(
    val nom: String = "",
    val prenoms: String = "",
    val profession: String = ""
)

data class ActeSaisie(
    val type: String = "N",
    val commune: String = "",
    val dept: String = "",
    val date: String = "",
    val heure: String = "",
    // Personne concernee
    val personneId: Long = 0L,
    val nom: String = "",
    val prenoms: String = "",
    val sexe: String = "",
    val profession: String = "",
    // Pere de la personne concernee
    val pereNom: String = "", val perePrenoms: String = "",
    val pereNe: String = "", val pereLieu: String = "", val pereDept: String = "",
    val pereProf: String = "", val pereDeces: String = "", val pereLieuDeces: String = "",
    // Mere de la personne concernee
    val mereNom: String = "", val merePrenoms: String = "",
    val mereNe: String = "", val mereLieu: String = "", val mereDept: String = "",
    val mereProf: String = "", val mereDeces: String = "", val mereLieuDeces: String = "",
    // Mariee (mariage uniquement)
    val marieeId: Long = 0L,
    val marieeNom: String = "", val marieePrenoms: String = "", val marieeProf: String = "",
    val pMarieeNom: String = "", val pMarieePrenoms: String = "",
    val pMarieeNe: String = "", val pMarieeLieu: String = "", val pMarieeDept: String = "",
    val pMarieeProf: String = "", val pMarieeDeces: String = "", val pMarieeLieuDeces: String = "",
    val mMarieeNom: String = "", val mMarieePrenoms: String = "",
    val mMarieeNe: String = "", val mMarieeLieu: String = "", val mMarieeDept: String = "",
    val mMarieeProf: String = "", val mMarieeDeces: String = "", val mMarieeLieuDeces: String = "",
    // Temoins et signataires
    val temoins: List<TemoinSaisie> = listOf(),
    val signataires: List<String> = listOf()
)

// Champs d'un parent (pere ou mere), observes par Compose.
class ChampsParent {
    var nom by mutableStateOf("")
    var prenoms by mutableStateOf("")
    var ne by mutableStateOf("")
    var lieu by mutableStateOf("")
    var dept by mutableStateOf("")
    var profession by mutableStateOf("")
    var deces by mutableStateOf("")
    var lieuDeces by mutableStateOf("")
}

// Age au moment de l'acte (calcule a partir des annees).
fun ageActe(dateActe: String, naissance: String): String {
    val a = Regex("[0-9]{4}").find(dateActe)?.value?.toIntOrNull() ?: return ""
    val n = Regex("[0-9]{4}").find(naissance)?.value?.toIntOrNull() ?: return ""
    if (a < n) return ""
    return (a - n).toString() + " ans"
}

@Composable
fun SectionTitre(t: String) {
    Text(
        t,
        Modifier.padding(top = 12.dp),
        fontWeight = FontWeight.Bold,
        color = CouleurVertFonce
    )
}

// Bloc Pere / Mere : naissance (age calcule), lieu, profession, deces.
@Composable
fun BlocParent(libelle: String, e: ChampsParent, dateActe: String) {
    SectionTitre(libelle)
    Champ("Nom", e.nom), { e.nom = it }
    Champ("Pr\u00E9noms", e.prenoms), { e.prenoms = it }
    val age = ageActe(dateActe, e.ne)
    Champ(
        "N\u00E9(e) le (jj/mm/aaaa)", e.ne, { e.ne = it },
        aide = if (age.isNotEmpty()) "\u00C2ge \u00E0 l'acte : " + age else "\u00C2ge calcul\u00E9 automatiquement"
    )
    Champ("Lieu de naissance", e.lieu), { e.lieu = it }
    Champ("D\u00E9partement (ex 59)", e.dept), { e.dept = it }
    Champ("Profession", e.profession), { e.profession = it }
    Champ("D\u00E9c\u00E9d\u00E9(e) le", e.deces), { e.deces = it }
    Champ("Lieu de d\u00E9c\u00E8s", e.lieuDeces), { e.lieuDeces = it }
}

@Composable
fun EcranSaisieActe(
    personnes: List<Personne>,
    onEnregistrer: (ActeSaisie) -> Unit
) {
    var type by remember { mutableStateOf("N") }
    var commune by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var heure by remember { mutableStateOf("") }
    var personneId by remember { mutableStateOf(0L) }
    var nom by remember { mutableStateOf("") }
    var prenoms by remember { mutableStateOf("") }
    var sexe by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    val pere = remember { ChampsParent() }
    val mere = remember { ChampsParent() }
    var marieeId by remember { mutableStateOf(0L) }
    var marieeNom by remember { mutableStateOf("") }
    var marieePrenoms by remember { mutableStateOf("") }
    var marieeProf by remember { mutableStateOf("") }
    val pereMariee = remember { ChampsParent() }
    val mereMariee = remember { ChampsParent() }
    val temoins = remember { mutableStateListOf<TemoinSaisie>() }
    val signataires = remember { mutableStateListOf<String>() }
    var pickerCible by remember { mutableStateOf(0) } // 0 = personne concernee, 1 = mariee
    var pickerOuvert by remember { mutableStateOf(false) }

    val exPersonne = personnes.firstOrNull { it.id == personneId }
    val exMariee = personnes.firstOrNull { it.id == marieeId }

    val parties: List<String> = buildList {
        add("Concerne(e)")
        if (pere.nom.isNotBlank()) add("P\u00E8re")
        if (mere.nom.isNotBlank()) add("M\u00E8re")
        if (type == "M" && (marieeNom.isNotBlank() || marieeId != 0L)) add("Mari\u00E9e")
        temoins.forEachIndexed { i, t -> if (t.nom.isNotBlank()) add("T\u00E9moin " + (i + 1).toString()) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Text(
            "Saisie d'un acte",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CouleurVertFonce
        )

        SectionTitre("Type d'acte")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "N" to "Naissance", "B" to "Bapt\u00EAme", "M" to "Mariage",
                "D" to "D\u00E9c\u00E8s", "S" to "S\u00E9pulture"
            ).forEach { (t, l) ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(l) })
            }
        }

        Champ("Commune", commune), { commune = it }
        Champ("D\u00E9partement (ex 59)", dept), { dept = it }
        Champ("Date (jj/mm/aaaa)", date), { date = it }
        Champ("Heure (hh:mm)", heure), { heure = it }

        SectionTitre("Personne concern\u00E9e")
        if (exPersonne != null) {
            Text(
                "Personne existante : " + exPersonne.titre(),
                color = CouleurVertFonce, fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        OutlinedButton(onClick = { pickerCible = 0; pickerOuvert = true }) {
            Text("Choisir une personne existante")
        }
        Champ("Nom", nom), { nom = it }
        Champ("Pr\u00E9noms", prenoms), { prenoms = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = sexe == "M", onClick = { sexe = "M" }, label = { Text("M") })
            FilterChip(selected = sexe == "F", onClick = { sexe = "F" }, label = { Text("F") })
        }
        Champ("Profession", profession), { profession = it }

        BlocParent("P\u00E8re", pere, date)
        BlocParent("M\u00E8re", mere, date)

        if (type == "M") {
            SectionTitre("Mari\u00E9e")
            if (exMariee != null) {
                Text(
                    "Personne existante : " + exMariee.titre(),
                    color = CouleurVertFonce, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            OutlinedButton(onClick = { pickerCible = 1; pickerOuvert = true }) {
                Text("Choisir la mari\u00E9e parmi les personnes")
            }
            Champ("Nom", marieeNom), { marieeNom = it }
            Champ("Pr\u00E9noms", marieePrenoms), { marieePrenoms = it }
            Champ("Profession", marieeProf), { marieeProf = it }
            BlocParent("P\u00E8re de la mari\u00E9e", pereMariee, date)
            BlocParent("M\u00E8re de la mari\u00E9e", mereMariee, date)
        }

        SectionTitre("T\u00E9moins")
        temoins.forEachIndexed { i, t ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Champ("Nom du t\u00E9moin", t.nom) { temoins[i] = t.copy(nom = it) }
                    Champ("Pr\u00E9noms du t\u00E9moin", t.prenoms) { temoins[i] = t.copy(prenoms = it) }
                    Champ("Profession du t\u00E9moin", t.profession) { temoins[i] = t.copy(profession = it) }
                }
                IconButton(onClick = {
                    signataires.remove("T\u00E9moin " + (i + 1).toString())
                    temoins.removeAt(i)
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Retirer")
                }
            }
        }
        if (temoins.size < 4) {
            OutlinedButton(onClick = { temoins.add(TemoinSaisie()) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Ajouter un t\u00E9moin")
            }
        }

        SectionTitre("Qui signe ?")
        if (parties.size <= 1) {
            Text(
                "Saisissez d'abord les personnes et les t\u00E9moins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            parties.forEach { l ->
                FilterChip(
                    selected = signataires.contains(l),
                    onClick = {
                        if (signataires.contains(l)) signataires.remove(l) else signataires.add(l)
                    },
                    label = { Text(l) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onEnregistrer(
                    ActeSaisie(
                        type = type,
                        commune = commune.trim(),
                        dept = dept.trim(),
                        date = date.trim(),
                        heure = heure.trim(),
                        personneId = personneId,
                        nom = nom.trim(),
                        prenoms = prenoms.trim(),
                        sexe = sexe,
                        profession = profession.trim(),
                        pereNom = pere.nom.trim(), perePrenoms = pere.prenoms.trim(),
                        pereNe = pere.ne.trim(), pereLieu = pere.lieu.trim(), pereDept = pere.dept.trim(),
                        pereProf = pere.profession.trim(), pereDeces = pere.deces.trim(), pereLieuDeces = pere.lieuDeces.trim(),
                        mereNom = mere.nom.trim(), merePrenoms = mere.prenoms.trim(),
                        mereNe = mere.ne.trim(), mereLieu = mere.lieu.trim(), mereDept = mere.dept.trim(),
                        mereProf = mere.profession.trim(), mereDeces = mere.deces.trim(), mereLieuDeces = mere.lieuDeces.trim(),
                        marieeId = marieeId,
                        marieeNom = marieeNom.trim(), marieePrenoms = marieePrenoms.trim(),
                        marieeProf = marieeProf.trim(),
                        pMarieeNom = pereMariee.nom.trim(), pMarieePrenoms = pereMariee.prenoms.trim(),
                        pMarieeNe = pereMariee.ne.trim(), pMarieeLieu = pereMariee.lieu.trim(), pMarieeDept = pereMariee.dept.trim(),
                        pMarieeProf = pereMariee.profession.trim(), pMarieeDeces = pereMariee.deces.trim(), pMarieeLieuDeces = pereMariee.lieuDeces.trim(),
                        mMarieeNom = mereMariee.nom.trim(), mMarieePrenoms = mereMariee.prenoms.trim(),
                        mMarieeNe = mereMariee.ne.trim(), mMarieeLieu = mereMariee.lieu.trim(), mMarieeDept = mereMariee.dept.trim(),
                        mMarieeProf = mereMariee.profession.trim(), mMarieeDeces = mereMariee.deces.trim(), mMarieeLieuDeces = mereMariee.lieuDeces.trim(),
                        temoins = temoins.toList(),
                        signataires = signataires.toList()
                    )
                )
            },
            enabled = personneId != 0L || nom.isNotBlank(),
            Modifier.fillMaxWidth()
        ) { Text("Enregistrer l'acte") }
        Spacer(Modifier.height(24.dp))
    }

    if (pickerOuvert) {
        AlertDialog(
            onDismissRequest = { pickerOuvert = false },
            title = { Text(if (pickerCible == 0) "Personne concern\u00E9e" else "Mari\u00E9e") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (personnes.isEmpty()) {
                        Text("Aucune personne. Saisissez le nom ci-dessous pour la cr\u00E9er.")
                    }
                    personnes.sortedWith(
                        compareBy({ it.nom.trim().uppercase() }, { it.prenoms.trim().uppercase() })
                    ).forEach { cand ->
                        TextButton(
                            onClick = {
                                if (pickerCible == 0) {
                                    personneId = cand.id
                                    nom = cand.nom
                                    prenoms = cand.prenoms
                                    sexe = cand.sexe
                                } else {
                                    marieeId = cand.id
                                    marieeNom = cand.nom
                                    marieePrenoms = cand.prenoms
                                }
                                pickerOuvert = false
                            },
                            Modifier.fillMaxWidth()
                        ) { Text(cand.titre()) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerOuvert = false }) { Text("Fermer") }
            }
        )
    }
}
