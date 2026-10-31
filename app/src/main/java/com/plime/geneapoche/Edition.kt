package com.plime.geneapoche

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- Dialogue d'edition d'une personne
// Sert a la creation et a la modification (appui long sur un bloc de la fiche).
// Champs date/heure/lieu/departement separes pour chaque acte N/B/M/D/S.

@Composable
fun DialogueEdition(
    cible: Personne?,
    idNouvelle: Long,
    onValider: (Personne) -> Unit,
    onAnnuler: () -> Unit,
    onSupprimer: (() -> Unit)? = null
) {
    var nom by remember { mutableStateOf(cible?.nom ?: "") }
    var prenoms by remember { mutableStateOf(cible?.prenoms ?: "") }
    var sexe by remember { mutableStateOf(cible?.sexe ?: "") }
    var profession by remember { mutableStateOf(cible?.profession ?: "") }
    var dateNaissance by remember { mutableStateOf(cible?.dateNaissance ?: "") }
    var heureNaissance by remember { mutableStateOf(cible?.heureNaissance ?: "") }
    var lieuNaissance by remember { mutableStateOf(cible?.lieuNaissance ?: "") }
    var deptNaissance by remember { mutableStateOf(cible?.deptNaissance ?: "") }
    var dateBapteme by remember { mutableStateOf(cible?.dateBapteme ?: "") }
    var heureBapteme by remember { mutableStateOf(cible?.heureBapteme ?: "") }
    var lieuBapteme by remember { mutableStateOf(cible?.lieuBapteme ?: "") }
    var deptBapteme by remember { mutableStateOf(cible?.deptBapteme ?: "") }
    var dateMariage by remember { mutableStateOf(cible?.dateMariage ?: "") }
    var heureMariage by remember { mutableStateOf(cible?.heureMariage ?: "") }
    var lieuMariage by remember { mutableStateOf(cible?.lieuMariage ?: "") }
    var deptMariage by remember { mutableStateOf(cible?.deptMariage ?: "") }
    var dateDeces by remember { mutableStateOf(cible?.dateDeces ?: "") }
    var heureDeces by remember { mutableStateOf(cible?.heureDeces ?: "") }
    var lieuDeces by remember { mutableStateOf(cible?.lieuDeces ?: "") }
    var deptDeces by remember { mutableStateOf(cible?.deptDeces ?: "") }
    var dateSepulture by remember { mutableStateOf(cible?.dateSepulture ?: "") }
    var heureSepulture by remember { mutableStateOf(cible?.heureSepulture ?: "") }
    var lieuSepulture by remember { mutableStateOf(cible?.lieuSepulture ?: "") }
    var deptSepulture by remember { mutableStateOf(cible?.deptSepulture ?: "") }

    fun construire(): Personne {
        val base = cible ?: Personne(id = idNouvelle)
        // Sosa et lateral ne sont jamais saisis : calcules a partir du de cujus.
        base.nom = normaliserNom(nom)
        base.prenoms = normaliserPrenoms(prenoms)
        base.sexe = sexe
        base.profession = profession.trim()
        base.dateNaissance = dateGreg(dateNaissance)
        base.heureNaissance = heureNaissance.trim()
        base.lieuNaissance = lieuNaissance.trim()
        base.deptNaissance = deptNaissance.trim()
        base.dateBapteme = dateGreg(dateBapteme)
        base.heureBapteme = heureBapteme.trim()
        base.lieuBapteme = lieuBapteme.trim()
        base.deptBapteme = deptBapteme.trim()
        base.dateMariage = dateGreg(dateMariage)
        base.heureMariage = heureMariage.trim()
        base.lieuMariage = lieuMariage.trim()
        base.deptMariage = deptMariage.trim()
        base.dateDeces = dateGreg(dateDeces)
        base.heureDeces = heureDeces.trim()
        base.lieuDeces = lieuDeces.trim()
        base.deptDeces = deptDeces.trim()
        base.dateSepulture = dateGreg(dateSepulture)
        base.heureSepulture = heureSepulture.trim()
        base.lieuSepulture = lieuSepulture.trim()
        base.deptSepulture = deptSepulture.trim()
        return base
    }

    val formulaireValide =
        (nom.isNotBlank() || prenoms.isNotBlank())

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(if (cible == null) "Ajouter une personne" else "Modifier la personne") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Identit\u00E9", fontWeight = FontWeight.Bold)
                Text(
                    "N\u00B0 Sosa et d'Aboville : calcul\u00E9s automatiquement (menu GEDCOM \u2192 D\u00E9finir le de cujus).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Champ("Nom", nom, { nom = it })
                Champ("Pr\u00E9noms", prenoms, { prenoms = it })
                // Sexe : F ou M seulement (pas de troisieme choix affiche).
                // Re-toucher la puce selectionnee remet le sexe non renseigne
                // (information manquante, deduite par la couleur verte).
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sexe == "M", onClick = { sexe = if (sexe == "M") "" else "M" }, label = { Text("M") })
                    FilterChip(selected = sexe == "F", onClick = { sexe = if (sexe == "F") "" else "F" }, label = { Text("F") })
                }
                Champ("Profession", profession, { profession = it })

                Spacer(Modifier.height(10.dp))
                Text("Naissance", fontWeight = FontWeight.Bold)
                Champ("Date (JJ/MM/AAAA)", dateNaissance, { dateNaissance = it })
                Champ("Heure (HH:mm)", heureNaissance, { heureNaissance = it })
                Champ("Lieu (commune)", lieuNaissance, { lieuNaissance = it })
                Champ("D\u00E9partement (ex 59)", deptNaissance, { deptNaissance = it })

                Spacer(Modifier.height(10.dp))
                Text("Bapt\u00EAme", fontWeight = FontWeight.Bold)
                Champ("Date", dateBapteme, { dateBapteme = it })
                Champ("Heure", heureBapteme, { heureBapteme = it })
                Champ("Lieu", lieuBapteme, { lieuBapteme = it })
                Champ("D\u00E9partement", deptBapteme, { deptBapteme = it })

                Spacer(Modifier.height(10.dp))
                Text("Mariage", fontWeight = FontWeight.Bold)
                Champ("Date", dateMariage, { dateMariage = it })
                Champ("Heure", heureMariage, { heureMariage = it })
                Champ("Lieu", lieuMariage, { lieuMariage = it })
                Champ("D\u00E9partement", deptMariage, { deptMariage = it })

                Spacer(Modifier.height(10.dp))
                Text("D\u00E9c\u00E8s", fontWeight = FontWeight.Bold)
                Champ("Date", dateDeces, { dateDeces = it })
                Champ("Heure", heureDeces, { heureDeces = it })
                Champ("Lieu", lieuDeces, { lieuDeces = it })
                Champ("D\u00E9partement", deptDeces, { deptDeces = it })

                Spacer(Modifier.height(10.dp))
                Text("S\u00E9pulture", fontWeight = FontWeight.Bold)
                Champ("Date", dateSepulture, { dateSepulture = it })
                Champ("Heure", heureSepulture, { heureSepulture = it })
                Champ("Lieu", lieuSepulture, { lieuSepulture = it })
                Champ("D\u00E9partement", deptSepulture, { deptSepulture = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(construire()) },
                enabled = formulaireValide
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (cible != null && onSupprimer != null) {
                    TextButton(onClick = onSupprimer) {
                        Text("Supprimer", color = androidx.compose.ui.graphics.Color(0xFFB23A2E))
                    }
                }
                TextButton(onClick = onAnnuler) { Text("Annuler") }
            }
        }
    )
}

// ---------------------------------------------------------------- Saisie d'un nouveau mariage
// Depuis la fiche : cree le bloc Mariage et le conjoint. La verification
// anti-doublon (nom + prenoms) et la synchronisation avec la fiche existante
// sont faites par l'appelant (MainActivity.ajouterMariage).

@Composable
fun DialogueNouveauMariage(
    onValider: (String, String, String, String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenoms by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var commune by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Saisir un mariage") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Si le conjoint existe d\u00E9j\u00E0 dans la base, sa fiche sera synchronis\u00E9e : aucun doublon ne sera cr\u00E9\u00E9.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Champ("Nom du conjoint", nom, { nom = it })
                Champ("Pr\u00E9noms du conjoint", prenoms, { prenoms = it })
                Champ("Date du mariage (jj/mm/aaaa, aaaa ou 22 Germinal An II)", date, { date = it }, !dateValide(date), "jj/mm/aaaa, aaaa ou 22 Germinal An II")
                Champ("Commune", commune, { commune = it })
                Champ("D\u00E9partement (ex 59)", dept, { dept = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValider(nom.trim(), prenoms.trim(), date.trim(), commune.trim(), dept.trim())
                },
                enabled = (date.isNotBlank() || commune.isNotBlank() || nom.isNotBlank() || prenoms.isNotBlank()) && dateValide(date)
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Saisie d'un nouvel enfant
// Depuis la fiche : cree l'enfant et le rattache. Verification anti-doublon
// et synchronisation par l'appelant (MainActivity.ajouterEnfant).

@Composable
fun DialogueNouvelEnfant(
    onValider: (String, String, String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenoms by remember { mutableStateOf("") }
    var sexe by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Saisir un enfant") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Si cet enfant existe d\u00E9j\u00E0 dans la base, sa fiche sera synchronis\u00E9e : aucun doublon ne sera cr\u00E9\u00E9.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Champ("Nom", nom, { nom = it })
                Champ("Pr\u00E9noms", prenoms, { prenoms = it })
                // Sexe : Garcon ou Fille seulement (pas de troisieme choix affiche).
                // Re-toucher la puce selectionnee remet le sexe non renseigne.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sexe == "M", onClick = { sexe = if (sexe == "M") "" else "M" }, label = { Text("Gar\u00E7on") })
                    FilterChip(selected = sexe == "F", onClick = { sexe = if (sexe == "F") "" else "F" }, label = { Text("Fille") })
                }
                Champ("Naissance (jj/mm/aaaa, aaaa ou 22 Germinal An II)", date, { date = it }, !dateValide(date), "jj/mm/aaaa, aaaa ou 22 Germinal An II")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(nom.trim(), prenoms.trim(), sexe, date.trim()) },
                enabled = (nom.isNotBlank() || prenoms.isNotBlank()) && dateValide(date)
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

@Composable
fun Champ(
    libelle: String,
    valeur: String,
    onChange: (String) -> Unit,
    erreur: Boolean = false,
    aide: String = ""
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(libelle) },
        isError = erreur,
        singleLine = true,
        supportingText = if (aide.isNotEmpty()) { { Text(aide, style = MaterialTheme.typography.bodySmall) } } else null,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}


// ---------------------------------------------------------------- Edition d'un bloc d'evenement
// Date, heure, commune, departement et reference de la cote d'archives.
// La validation declenche le controle de coherence et la synchronisation
// cote MainActivity.

@Composable
fun DialogueEditionBloc(
    titre: String,
    dateInit: String,
    heureInit: String,
    communeInit: String,
    deptInit: String,
    refInit: String,
    onValider: (String, String, String, String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var date by remember { mutableStateOf(dateInit) }
    var heure by remember { mutableStateOf(heureInit) }
    var commune by remember { mutableStateOf(communeInit) }
    var dept by remember { mutableStateOf(deptInit) }
    var ref by remember { mutableStateOf(refInit) }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(titre) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (jj/mm/aaaa, aaaa ou 22 Germinal An II)") },
                        isError = !dateValide(date),
                        singleLine = true,
                        supportingText = { Text("jj/mm/aaaa, aaaa ou 22 Germinal An II", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heure,
                        onValueChange = { heure = it },
                        label = { Text("Heure (hh:mm)") },
                        isError = !heureValide(heure),
                        singleLine = true,
                        supportingText = { Text("hh:mm", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Champ("Commune", commune, { commune = it })
                Champ("D\u00E9partement (ex 59)", dept, { dept = it })
                Champ("R\u00E9f\u00E9rence (cote AD)", ref, { ref = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValider(date.trim(), heure.trim(), commune.trim(), dept.trim(), ref.trim())
                },
                enabled = dateValide(date) && heureValide(heure)
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Saisie d'un parent
// Depuis le bloc Naissance : cree le pere ou la mere. L'appelant verifie
// d'abord si une fiche identique existe (aucun doublon) et rattache l'enfant.

@Composable
fun DialogueNouveauParent(
    role: String,
    onValider: (String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenoms by remember { mutableStateOf("") }
    val titreDialogue = if (role == "pere") "Saisir le p\u00E8re" else "Saisir la m\u00E8re"

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(titreDialogue) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Si cette personne existe d\u00E9j\u00E0 dans la base, sa fiche existante sera utilis\u00E9e : aucun doublon ne sera cr\u00E9\u00E9.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Champ("Nom", nom, { nom = it })
                Champ("Pr\u00E9noms", prenoms, { prenoms = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(nom.trim(), prenoms.trim()) },
                enabled = nom.isNotBlank() || prenoms.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}

// ---------------------------------------------------------------- Choix ou creation d'un temoin
// Aucun temoin par defaut. On propose d'abord les fiches existantes qui
// correspondent (choix), et l'on ne propose la creation que si aucune fiche
// ne correspond exactement : pas de doublon dans la base.

@Composable
fun DialogueNouveauTemoin(
    personnes: List<Personne>,
    onChoisir: (Long) -> Unit,
    onCreer: (String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenoms by remember { mutableStateOf("") }
    val motNom = nom.trim().uppercase()
    val motPrenoms = prenoms.trim().uppercase()

    // Fiches existantes dont le nom ou les prenoms contiennent la saisie.
    val correspondances = if (motNom.isEmpty() && motPrenoms.isEmpty()) {
        listOf()
    } else {
        personnes.filter { q ->
            (motNom.isNotEmpty() && q.nom.trim().uppercase().contains(motNom)) ||
                (motPrenoms.isNotEmpty() && q.prenoms.trim().uppercase().contains(motPrenoms))
        }.take(6)
    }
    // Creation proposee seulement si aucune fiche ne correspond exactement.
    val correspondanceExacte = personnes.any { q ->
        q.nom.trim().uppercase() == motNom && q.prenoms.trim().uppercase() == motPrenoms
    }
    val creationPossible = (motNom.isNotEmpty() || motPrenoms.isNotEmpty()) && !correspondanceExacte

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Ajouter un t\u00E9moin") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Champ("Nom", nom, { nom = it })
                Champ("Pr\u00E9noms", prenoms, { prenoms = it })
                if (correspondances.isNotEmpty()) {
                    Text(
                        "Fiches existantes correspondantes :",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    correspondances.forEach { q ->
                        Text(
                            q.titre(),
                            Modifier.fillMaxWidth()
                                .clickable { onChoisir(q.id) }
                                .padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (creationPossible) {
                    OutlinedButton(
                        onClick = { onCreer(nom.trim(), prenoms.trim()) },
                        modifier = Modifier.padding(top = 6.dp)
                    ) { Text("Cr\u00E9er la fiche") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAnnuler) { Text("Fermer") }
        }
    )
}
