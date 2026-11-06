package com.plime.geneapoche

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Saisie par acte
// G\u00E9n\u00E9aPoche 3 : ce formulaire alimente TOUTE l'application (fiche,
// arbre, journal, num\u00E9rotation). Depuis le menu Saisie (7 entr\u00E9es) ou
// depuis une fiche (pr\u00E9-rempli). Les contr\u00F4les de coh\u00E9rence affichent
// la p\u00E9riode r\u00E9aliste et demandent une confirmation, sans jamais bloquer.

data class TemoinSaisie(
    val nom: String = "",
    val prenoms: String = "",
    val profession: String = ""
)

data class ActeSaisie(
    // Id de l'acte modifi\u00E9 (0 = nouvel acte). Depuis la fiche, le formulaire
    // est pr\u00E9-rempli et l'enregistrement MET A JOUR l'acte existant.
    val acteId: Long = 0L,
    // GénéaPoche 10 : ouvert par appui long sur une ligne NBMDS de la
    // fiche = EDITION des champs de la personne (même sans acte stocké).
    val editionFiche: Boolean = false,
    val type: String = "N",
    val commune: String = "",
    val dept: String = "",
    // R\u00E9f\u00E9rence de l'acte aux Archives d\u00E9partementales (cote)
    val reference: String = "",
    // G\u00E9n\u00E9aPoche 11 (11.4) : source et niveau de preuve.
    val depot: String = "",
    val dateConsultation: String = "",
    val fiabilite: Int = -1,
    val date: String = "",
    val heure: String = "",
    // G\u00E9n\u00E9aPoche 3 : intitul\u00E9 libre d'un \u00E9v\u00E9nement (type "E").
    val intitule: String = "",
    // G\u00E9n\u00E9aPoche 3 : naissance d'un ENFANT : parent existant choisi.
    val parentPourEnfant: Long = 0L,
    // Personne concern\u00E9e
    val personneId: Long = 0L,
    val nom: String = "",
    val prenoms: String = "",
    val sexe: String = "",
    val profession: String = "",
    // P\u00E8re de la personne concern\u00E9e
    val pereNom: String = "", val perePrenoms: String = "",
    val pereNe: String = "", val pereLieu: String = "", val pereDept: String = "",
    val pereProf: String = "", val pereDeces: String = "", val pereLieuDeces: String = "",
    // M\u00E8re de la personne concern\u00E9e
    val mereNom: String = "", val merePrenoms: String = "",
    val mereNe: String = "", val mereLieu: String = "", val mereDept: String = "",
    val mereProf: String = "", val mereDeces: String = "", val mereLieuDeces: String = "",
    // Mari\u00E9e (mariage uniquement)
    val marieeId: Long = 0L,
    val marieeNom: String = "", val marieePrenoms: String = "", val marieeProf: String = "",
    val pMarieeNom: String = "", val pMarieePrenoms: String = "",
    val pMarieeNe: String = "", val pMarieeLieu: String = "", val pMarieeDept: String = "",
    val pMarieeProf: String = "", val pMarieeDeces: String = "", val pMarieeLieuDeces: String = "",
    val mMarieeNom: String = "", val mMarieePrenoms: String = "",
    val mMarieeNe: String = "", val mMarieeLieu: String = "", val mMarieeDept: String = "",
    val mMarieeProf: String = "", val mMarieeDeces: String = "", val mMarieeLieuDeces: String = "",
    // T\u00E9moins et signataires
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
    val age = ageActe(dateActe, e.ne)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Champ("Nom", e.nom, { e.nom = it }, modifier = Modifier.weight(1f))
        Champ("Pr\u00E9noms", e.prenoms, { e.prenoms = it }, modifier = Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Champ(
        "N\u00E9(e) le (jj/mm/aaaa)", e.ne, { e.ne = it },
        aide = if (age.isNotEmpty()) "\u00C2ge \u00E0 l'acte : " + age else "\u00C2ge calcul\u00E9 automatiquement",
        modifier = Modifier.weight(1f)
    )
    Champ("Lieu de naissance", e.lieu, { e.lieu = it }, modifier = Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Champ("D\u00E9partement (ex 59)", e.dept, { e.dept = it }, modifier = Modifier.weight(1f))
        Champ("Profession", e.profession, { e.profession = it }, modifier = Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Champ("D\u00E9c\u00E9d\u00E9(e) le", e.deces, { e.deces = it }, modifier = Modifier.weight(1f))
        Champ("Lieu de d\u00E9c\u00E8s", e.lieuDeces, { e.lieuDeces = it }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun EcranSaisieActe(
    personnes: List<Personne>,
    onEnregistrer: (ActeSaisie) -> Unit,
    // Pre-remplissage (ouverture depuis la fiche d'une personne).
    initial: ActeSaisie? = null,
    // Fermeture du formulaire (plein ecran depuis la fiche) ; null = onglet Saisie.
    onFermer: (() -> Unit)? = null,
    // Actes existants : permet de PRE-REMPLIR avec l'acte deja saisi quand
    // on choisit une personne existante (aucun doublon, edition de l'acte).
    actes: List<Acte> = emptyList(),
    // GeneaPoche 4 : type impose par l'appui long sur un bloc de la fiche.
    typeImpose: String? = null
) {
    var type by remember(initial) { mutableStateOf(typeImpose ?: initial?.type ?: "N") }
    var commune by remember(initial) { mutableStateOf(initial?.commune ?: "") }
    var dept by remember(initial) { mutableStateOf(initial?.dept ?: "") }
    var reference by remember(initial) { mutableStateOf(initial?.reference ?: "") }
    var depot by remember(initial) { mutableStateOf(initial?.depot ?: "") }
    var dateConsultation by remember(initial) { mutableStateOf(initial?.dateConsultation ?: "") }
    var fiabilite by remember(initial) { mutableStateOf(initial?.fiabilite ?: -1) }
    var date by remember(initial) { mutableStateOf(initial?.date ?: "") }
    var heure by remember(initial) { mutableStateOf(initial?.heure ?: "") }
    var intitule by remember(initial) { mutableStateOf(initial?.intitule ?: "") }
    var acteId by remember(initial) { mutableStateOf(initial?.acteId ?: 0L) }
    // Parent choisi pour la naissance d'un enfant (menu Saisie).
    var parentPourEnfantId by remember(initial) { mutableStateOf(initial?.parentPourEnfant ?: 0L) }
    var personneId by remember(initial) { mutableStateOf(initial?.personneId ?: 0L) }
    var nom by remember(initial) { mutableStateOf(initial?.nom ?: "") }
    var prenoms by remember(initial) { mutableStateOf(initial?.prenoms ?: "") }
    var sexe by remember(initial) { mutableStateOf(initial?.sexe ?: "") }
    var profession by remember(initial) { mutableStateOf(initial?.profession ?: "") }
    val pere = remember(initial) {
        ChampsParent().apply {
            initial?.let { a -> nom = a.pereNom; prenoms = a.perePrenoms; ne = a.pereNe; lieu = a.pereLieu; dept = a.pereDept; profession = a.pereProf; deces = a.pereDeces; lieuDeces = a.pereLieuDeces }
        }
    }
    val mere = remember(initial) {
        ChampsParent().apply {
            initial?.let { a -> nom = a.mereNom; prenoms = a.merePrenoms; ne = a.mereNe; lieu = a.mereLieu; dept = a.mereDept; profession = a.mereProf; deces = a.mereDeces; lieuDeces = a.mereLieuDeces }
        }
    }
    var marieeId by remember(initial) { mutableStateOf(initial?.marieeId ?: 0L) }
    var marieeNom by remember(initial) { mutableStateOf(initial?.marieeNom ?: "") }
    var marieePrenoms by remember(initial) { mutableStateOf(initial?.marieePrenoms ?: "") }
    var marieeProf by remember(initial) { mutableStateOf(initial?.marieeProf ?: "") }
    val pereMariee = remember(initial) {
        ChampsParent().apply {
            initial?.let { a -> nom = a.pMarieeNom; prenoms = a.pMarieePrenoms; ne = a.pMarieeNe; lieu = a.pMarieeLieu; dept = a.pMarieeDept; profession = a.pMarieeProf; deces = a.pMarieeDeces; lieuDeces = a.pMarieeLieuDeces }
        }
    }
    val mereMariee = remember(initial) {
        ChampsParent().apply {
            initial?.let { a -> nom = a.mMarieeNom; prenoms = a.mMarieePrenoms; ne = a.mMarieeNe; lieu = a.mMarieeLieu; dept = a.mMarieeDept; profession = a.mMarieeProf; deces = a.mMarieeDeces; lieuDeces = a.mMarieeLieuDeces }
        }
    }
    val temoins = remember(initial) { mutableStateListOf<TemoinSaisie>().also { l -> initial?.temoins?.forEach { l.add(it) } } }
    val signataires = remember(initial) { mutableStateListOf<String>().also { l -> initial?.signataires?.forEach { l.add(it) } } }
    var pickerCible by remember { mutableStateOf(0) } // 0 = personne concernee, 1 = mariee
    var pickerOuvert by remember { mutableStateOf(false) }
    // G\u00E9n\u00E9aPoche 3 : confirmation d'une date hors p\u00E9riode r\u00E9aliste.
    var confirme by remember { mutableStateOf(false) }
    var avertissement by remember { mutableStateOf<String?>(null) }
    var acteEnAttente by remember { mutableStateOf<ActeSaisie?>(null) }

    val exPersonne = personnes.firstOrNull { it.id == personneId }
    val exMariee = personnes.firstOrNull { it.id == marieeId }
    val parentEnfant = personnes.firstOrNull { it.id == parentPourEnfantId }

    // Re-remplit tout le formulaire depuis un ActeSaisie (personne choisie
    // dans le picker : ses informations connues et son acte existant).
    fun remplirDepuis(a: ActeSaisie) {
        acteId = a.acteId
        commune = a.commune
        dept = a.dept
        reference = a.reference
        depot = a.depot
        dateConsultation = a.dateConsultation
        fiabilite = a.fiabilite
        date = a.date
        heure = a.heure
        intitule = a.intitule
        personneId = a.personneId
        nom = a.nom
        prenoms = a.prenoms
        sexe = a.sexe
        profession = a.profession
        pere.nom = a.pereNom; pere.prenoms = a.perePrenoms; pere.ne = a.pereNe
        pere.lieu = a.pereLieu; pere.dept = a.pereDept; pere.profession = a.pereProf
        pere.deces = a.pereDeces; pere.lieuDeces = a.pereLieuDeces
        mere.nom = a.mereNom; mere.prenoms = a.merePrenoms; mere.ne = a.mereNe
        mere.lieu = a.mereLieu; mere.dept = a.mereDept; mere.profession = a.mereProf
        mere.deces = a.mereDeces; mere.lieuDeces = a.mereLieuDeces
        marieeId = a.marieeId
        marieeNom = a.marieeNom; marieePrenoms = a.marieePrenoms; marieeProf = a.marieeProf
        pereMariee.nom = a.pMarieeNom; pereMariee.prenoms = a.pMarieePrenoms; pereMariee.ne = a.pMarieeNe
        pereMariee.lieu = a.pMarieeLieu; pereMariee.dept = a.pMarieeDept; pereMariee.profession = a.pMarieeProf
        pereMariee.deces = a.pMarieeDeces; pereMariee.lieuDeces = a.pMarieeLieuDeces
        mereMariee.nom = a.mMarieeNom; mereMariee.prenoms = a.mMarieePrenoms; mereMariee.ne = a.mMarieeNe
        mereMariee.lieu = a.mMarieeLieu; mereMariee.dept = a.mMarieeDept; mereMariee.profession = a.mMarieeProf
        mereMariee.deces = a.mMarieeDeces; mereMariee.lieuDeces = a.mMarieeLieuDeces
        temoins.clear(); a.temoins.forEach { temoins.add(it) }
        signataires.clear(); a.signataires.forEach { signataires.add(it) }
    }

    val parties: List<String> = buildList {
        add("Concerne(e)")
        if (pere.nom.isNotBlank()) add("P\u00E8re")
        if (mere.nom.isNotBlank()) add("M\u00E8re")
        if (marieeNom.isNotBlank() || marieeId != 0L) add(if (type == "M") "Mari\u00E9e" else "Conjoint(e)")
        temoins.forEachIndexed { i, t -> if (t.nom.isNotBlank()) add("T\u00E9moin " + (i + 1).toString()) }
    }

    // v0.58 : formulaire COMPACT. Le contenu defile, mais le bouton
    // Enregistrer reste TOUJOURS visible en bas, hors du defilement.
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onFermer != null) {
                IconButton(onClick = onFermer) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer")
                }
            }
            Text(
                "Saisie d'un acte",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CouleurVertFonce
            )
        }

        // v0.63 : la personne CONCERNÉE s'affiche en TÊTE du formulaire,
        // impossible à manquer, et se change en une touche. Si le
        // pré-remplissage part de la mauvaise fiche, « Changer » recharge
        // tout le formulaire sur la bonne personne.
        Card(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (exPersonne != null) CouleurCarte else Color.White
            ),
            border = BorderStroke(1.dp, if (exPersonne != null) CouleurBordVert else CouleurOrange)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (exPersonne != null)
                        (if (acteId != 0L) "Modification : " else "Pour : ") +
                            libelleType(type) + " de " + exPersonne.titre()
                    else
                        "Aucune personne s\u00E9lectionn\u00E9e",
                    Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (exPersonne != null) CouleurVertFonce else CouleurRouge
                )
                TextButton(onClick = { pickerCible = 0; pickerOuvert = true }) {
                    Text(if (exPersonne == null) "Choisir" else "Changer", fontSize = 12.sp)
                }
            }
        }

        SectionTitre("Type d'acte")
        // GeneaPoche 4 : si le type est impose (appui long sur un bloc),
        // il ne peut pas etre change : seul le type concerne est modifiable.
        if (typeImpose == null) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "N" to "Naissance", "B" to "Bapt\u00EAme", "M" to "Mariage",
                    "D" to "D\u00E9c\u00E8s", "S" to "S\u00E9pulture",
                    "E" to "\u00C9v\u00E9nement"
                ).forEach { (t, l) ->
                    FilterChip(selected = type == t, onClick = { type = t; confirme = false }, label = { Text(l) })
                }
            }
        } else {
            Text(
                libelleType(type),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CouleurVertFonce
            )
        }
        if (type == "E") {
            // \u00C9v\u00E9nement \u00E0 intitul\u00E9 libre : rang\u00E9 dans la chronologie.
            Champ("Intitul\u00E9 (profession, d\u00E9m\u00E9nagement\u2026)", intitule, { intitule = it })
        }

        // v0.58 : champs compactes, deux par ligne.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ("Commune", commune, { commune = it }, modifier = Modifier.weight(1f))
            Champ("D\u00E9partement (ex 59)", dept, { dept = it }, modifier = Modifier.weight(1f))
        }
        Champ("D\u00E9p\u00F4t (ex : Archives d\u00E9partementales du Nord)", depot, { depot = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ("R\u00E9f. (cote AD)", reference, { reference = it }, modifier = Modifier.weight(1f))
            Champ("Consult\u00E9 le (jj/mm/aaaa)", dateConsultation, { dateConsultation = it }, modifier = Modifier.weight(1f))
        }
        Text(
            "Niveau de preuve :",
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf(0, 1, 2, 3).forEach { n ->
                FilterChip(
                    selected = fiabilite == n,
                    onClick = { fiabilite = if (fiabilite == n) -1 else n },
                    label = { Text(n.toString() + " \u00B7 " + libelleFiabilite(n), fontSize = 11.sp) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ(
                "Date (jj/mm/aaaa, aaaa ou r\u00E9publicain)", date, { date = it; confirme = false },
                erreur = !dateValide(date),
                aide = periodeRealisteTxt(type, exPersonne),
                modifier = Modifier.weight(1f)
            )
            Champ("Heure (hh:mm)", heure, { heure = it }, modifier = Modifier.weight(1f))
        }

        SectionTitre("Personne concern\u00E9e")
        if (exPersonne != null) {
            // v0.62 : en modification d'un acte rattaché à une fiche, la
            // personne concernée est AFFICHÉE en clair et le bouton de choix
            // DISPARAIT : impossible de saisir ou corriger l'acte au nom
            // de quelqu'un d'autre sans s'en rendre compte.
            Text(
                (if (acteId != 0L) "Modification : " else "Saisie pour : ") +
                    libelleType(type) + " de " + exPersonne.titre(),
                color = CouleurVertFonce, fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            // v0.63 : voie de réparation directe — changer de personne
            // recharge le formulaire (remplirDepuis) sur la bonne fiche.
            TextButton(onClick = { pickerCible = 0; pickerOuvert = true }) {
                Text("Ce n'est pas la bonne personne ? Changer", fontSize = 11.sp)
            }
        } else {
            OutlinedButton(onClick = { pickerCible = 0; pickerOuvert = true }) {
                Text("Choisir une personne existante")
            }
        }
        if (parentEnfant != null && type == "N") {
            Text(
                "Enfant de : " + parentEnfant.titre(),
                color = CouleurVertFonce, fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ("Nom", nom, { nom = it }, modifier = Modifier.weight(1f))
            Champ("Pr\u00E9noms", prenoms, { prenoms = it }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = sexe == "M", onClick = { sexe = "M" }, label = { Text("M") })
            FilterChip(selected = sexe == "F", onClick = { sexe = "F" }, label = { Text("F") })
            Champ("Profession", profession, { profession = it }, modifier = Modifier.weight(1f))
        }

        BlocParent("P\u00E8re", pere, date)
        BlocParent("M\u00E8re", mere, date)

        if (type == "M" || marieeId != 0L || marieeNom.isNotBlank()) {
            SectionTitre(if (type == "M") "Mari\u00E9e" else "Conjoint(e)")
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Champ("Nom", marieeNom, { marieeNom = it }, modifier = Modifier.weight(1f))
                Champ("Pr\u00E9noms", marieePrenoms, { marieePrenoms = it }, modifier = Modifier.weight(1f))
            }
            Champ("Profession", marieeProf, { marieeProf = it })
            BlocParent(if (type == "M") "P\u00E8re de la mari\u00E9e" else "P\u00E8re du conjoint", pereMariee, date)
            BlocParent(if (type == "M") "M\u00E8re de la mari\u00E9e" else "M\u00E8re du conjoint", mereMariee, date)
        }

        SectionTitre("T\u00E9moins")
        temoins.forEachIndexed { i, t ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Champ("Nom du t\u00E9moin", t.nom, { temoins[i] = t.copy(nom = it) }, modifier = Modifier.weight(1f))
                        Champ("Pr\u00E9noms", t.prenoms, { temoins[i] = t.copy(prenoms = it) }, modifier = Modifier.weight(1f))
                        Champ("Profession", t.profession, { temoins[i] = t.copy(profession = it) }, modifier = Modifier.weight(1f))
                    }
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

        }
        // v0.58 : bouton TOUJOURS visible en bas, hors du defilement.
        Button(
            onClick = {
                val acte = ActeSaisie(
                    acteId = acteId,
                    type = type,
                    commune = commune.trim(),
                    dept = dept.trim(),
                    reference = reference.trim(),
                    depot = depot.trim(),
                    dateConsultation = dateConsultation.trim(),
                    fiabilite = fiabilite,
                    date = date.trim(),
                    heure = heure.trim(),
                    intitule = intitule.trim(),
                    parentPourEnfant = parentPourEnfantId,
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
                // Contr\u00F4le de coh\u00E9rence : AVERTISSEMENT non bloquant,
                // l'utilisateur peut confirmer ("Enregistrer quand m\u00EAme").
                val avrt = avertissementCoherence(type, date, exPersonne, exMariee, parentEnfant)
                if (avrt.isNotEmpty() && !confirme) {
                    avertissement = avrt
                    acteEnAttente = acte
                } else {
                    onEnregistrer(acte)
                    onFermer?.invoke()
                }
            },
            enabled = (personneId != 0L || nom.isNotBlank()) && dateValide(date) && heureValide(heure),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) { Text("Enregistrer") }
        Spacer(Modifier.height(12.dp))
    }

    if (avertissement != null && acteEnAttente != null) {
        AlertDialog(
            onDismissRequest = { avertissement = null; acteEnAttente = null },
            title = { Text("Coh\u00E9rence des dates") },
            text = { Text(avertissement ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    confirme = true
                    avertissement = null
                    val a = acteEnAttente
                    acteEnAttente = null
                    if (a != null) onEnregistrer(a)
                    onFermer?.invoke()
                }) { Text("Enregistrer quand m\u00EAme") }
            },
            dismissButton = {
                TextButton(onClick = { avertissement = null; acteEnAttente = null }) { Text("Modifier") }
            }
        )
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
                                    // G\u00E9n\u00E9aPoche 3 : la personne existante est
                                    // PROPOSEE avec tout ce qui est connu d'elle ;
                                    // si son acte de ce type existe deja, il est
                                    // EDITE (aucun doublon).
                                    remplirDepuis(prefillActe(cand, personnes, actes, type))
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

// ---------------------------------------------------------------- Menu Saisie
// G\u00E9n\u00E9aPoche 3 : menu unique \u00E0 7 entr\u00E9es avec pastilles color\u00E9es.
// Chaque entr\u00E9e ouvre le m\u00EAme formulaire, pr\u00E9-rempli depuis la fiche.
// "Enfant" choisit d'abord le parent, puis saisit la naissance.

@Composable
fun EcranSaisie(
    personnes: List<Personne>,
    onOuvrir: (String) -> Unit,
    onEnfant: (Long) -> Unit
) {
    var pickerParent by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Text(
            "Saisie",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CouleurVertFonce
        )
        Text(
            "Toute saisie alimente directement la fiche, l'arbre et le journal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        // v0.61 : menu en PUCES compactes (meme gabarit que le niveau de
        // preuve) - une seule ligne deroulante au lieu des grandes cartes.
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf(
                "N" to "Naissance",
                "B" to "Bapt\u00EAme",
                "M" to "Mariage",
                "D" to "D\u00E9c\u00E8s",
                "S" to "S\u00E9pulture",
                "E" to "\u00C9v\u00E9nement"
            ).forEach { (t, l) ->
                FilterChip(
                    selected = false,
                    onClick = { onOuvrir(t) },
                    label = { Text(t + " \u00B7 " + l, fontSize = 11.sp) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            // ENFANT : choisir le parent existant puis saisir sa naissance.
            FilterChip(
                selected = false,
                onClick = { pickerParent = true },
                label = { Text("Enfant", fontSize = 11.sp) }
            )
        }
        Text(
            "Une personne existante est syst\u00E9matiquement propos\u00E9e : si son acte existe d\u00E9j\u00E0, il est \u00E9dit\u00E9, jamais dupliqu\u00E9.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(16.dp))
    }

    if (pickerParent) {
        AlertDialog(
            onDismissRequest = { pickerParent = false },
            title = { Text("Enfant de quelle personne ?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    personnes.sortedWith(
                        compareBy({ it.nom.trim().uppercase() }, { it.prenoms.trim().uppercase() })
                    ).forEach { cand ->
                        TextButton(
                            onClick = {
                                pickerParent = false
                                onEnfant(cand.id)
                            },
                            Modifier.fillMaxWidth()
                        ) { Text(cand.titre()) }
                    }
                    if (personnes.isEmpty()) {
                        Text("Aucune personne : saisissez le nom dans le formulaire.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pickerParent = false
                    onEnfant(0L)
                }) { Text("Sans parent connu") }
            },
            dismissButton = {
                TextButton(onClick = { pickerParent = false }) { Text("Annuler") }
            }
        )
    }
}



