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
import androidx.compose.material3.Checkbox
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
    val profession: String = "",
    // v0.72 : rôle du témoin officiel ("PrÊtre" ou "Officier d'état civil").
    val role: String = "",
    // v0.77 : vrai homonyme (autre date de naissance) : la fenêtre
    // "Créer une nouvelle fiche" force une fiche DISTINCTE.
    val nouveau: Boolean = false,
    // v0.97 : renseignements portes par l'acte pour CE temoin.
    val age: String = "",
    val situation: String = "",
    val domicile: String = ""
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
    // Chantier 12.0 : l'acte n'est plus stocke dans l'app ; une coche
    // "Enregistr\u00E9" par type (l'utilisateur garde ses actes ailleurs).
    val enregistre: Boolean = false,
    // v0.92 : source reutilisable de l'acte (0 = aucune), numero d'acte,
    // numero de la vue sur le total de vues (citation creee a l'enregistrement).
    val sourceId: Long = 0L,
    val numeroActe: String = "",
    val vue: Int = 0,
    val vueTotal: Int = 0,
    val fiabiliteSource: Int = -1,
    val date: String = "",
    val heure: String = "",
    // G\u00E9n\u00E9aPoche 3 : intitul\u00E9 libre d'un \u00E9v\u00E9nement (type "E").
    val intitule: String = "",
    // G\u00E9n\u00E9aPoche 3 : naissance d'un ENFANT : parent existant choisi.
    val parentPourEnfant: Long = 0L,
    // Personne concern\u00E9e
    val personneId: Long = 0L,
    // v0.65 : identifiants des parents lies sur la fiche (fiabilit\u00E9 des
    // liens : un homonyme ne peut plus d\u00E9router le lien, une correction
    // de nom ne force pas l'ancienne fiche).
    val pereId: Long = 0L,
    val mereId: Long = 0L,
    // v0.65 : noms pr\u00E9sents \u00E0 l'OUVERTURE du formulaire : un lien de
    // parent\u00E9 n'est retir\u00E9 que si un nom rempli \u00E0 l'ouverture a \u00E9t\u00E9 vid\u00E9.
    val pereNomInit: String = "",
    val mereNomInit: String = "",
    val nom: String = "",
    val prenoms: String = "",
    val sexe: String = "",
    val profession: String = "",
    // P\u00E8re de la personne concern\u00E9e
    val pereNom: String = "", val perePrenoms: String = "",
    val pereNe: String = "", val pereLieu: String = "", val pereDept: String = "",
    val pereProf: String = "", val pereDeces: String = "", val pereLieuDeces: String = "",
    // v0.97 : \u00E2ge \u00E0 l'acte (saisi), situation, domicile du p\u00E8re.
    val pereAge: String = "", val pereSituation: String = "", val pereDomicile: String = "",
    // M\u00E8re de la personne concern\u00E9e
    val mereNom: String = "", val merePrenoms: String = "",
    val mereNe: String = "", val mereLieu: String = "", val mereDept: String = "",
    val mereProf: String = "", val mereDeces: String = "", val mereLieuDeces: String = "",
    // v0.97 : \u00E2ge, situation, domicile de la m\u00E8re.
    val mereAge: String = "", val mereSituation: String = "", val mereDomicile: String = "",
    // v0.97 : mentions marginales d'un acte de naissance (mariage, d\u00E9c\u00E8s).
    val mentionMDate: String = "", val mentionMLieu: String = "",
    // v0.99 : mention marginale de mariage NOMINATIVE : nom et pr\u00E9noms
    // du conjoint (fiche existante choisie, ou fiche cr\u00E9\u00E9e \u00E0 l'enregistrement).
    val mentionMNom: String = "", val mentionMPrenoms: String = "",
    val mentionDDate: String = "", val mentionDLieu: String = "",
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
    val signataires: List<String> = listOf(),
    // v0.77 : "Créer une nouvelle fiche" choisi dans la fenêtre d'homonymes
    // pour la personne concernée (vrai homonyme, autre date de naissance).
    val persNouvelle: Boolean = false,
    // v0.80 : le conjoint du défunt est ENCORE EN VIE (déclarant possible)
    // ou DÉJÀ DÉCÉDÉ avant lui (veuf/veuve) ; sur un décès, un ENFANT peut
    // être déclarant. Les deux choix sont désormais PERSISTÉS.
    val conjointEnVie: Boolean = true,
    // v0.99 : age a l'acte de la personne concernee (M, D, S) et de la
    // mariee : date de naissance approximative calculee sur les fiches.
    val ageConcerne: String = "",
    val marieeAge: String = "",
    val declarantEnfantId: Long = 0L,
    // v0.98 : champs valides B/M/D/S.
    // N : jour et heure de la declaration.
    val declNDate: String = "", val declNHeure: String = "",
    // B : enfant legitime ou naturel.
    val bLegitime: String = "",
    // M : contrat de mariage (nom de l'office) et dispenses.
    val contratMariage: String = "", val dispenses: String = "",
    // D : declaration (date/heure) et declarant (nom, profession, domicile).
    val declDDate: String = "", val declDHeure: String = "",
    val declarantNom: String = "", val declarantProfession: String = "", val declarantDomicile: String = "",
    // S : ceremonie, cimetiere, concession, ministre du culte.
    val ceremonieSDate: String = "", val cimetiere: String = "",
    val concession: String = "", val ministreCulte: String = ""
)

// Champs d'un parent (pere ou mere), observes par Compose.
class ChampsParent {
    var id: Long = 0L
    var nom by mutableStateOf("")
    var prenoms by mutableStateOf("")
    var ne by mutableStateOf("")
    var lieu by mutableStateOf("")
    var dept by mutableStateOf("")
    var profession by mutableStateOf("")
    var deces by mutableStateOf("")
    var lieuDeces by mutableStateOf("")
    // v0.97 : \u00E2ge \u00E0 l'acte (saisi, ex "34"), situation, domicile.
    var age by mutableStateOf("")
    var situation by mutableStateOf("")
    var domicile by mutableStateOf("")
}

// v0.65 (diagnostic externe) : UN SEUL endroit remplit les champs d'un
// parent. Extension top-level testable : remplir un parent ne peut plus
// toucher les champs d'un autre bloc.
fun ChampsParent.remplir(nom: String, prenoms: String, ne: String, lieu: String,
    dept: String, profession: String, deces: String, lieuDeces: String,
    age: String = "", situation: String = "", domicile: String = "") {
    this.nom = nom; this.prenoms = prenoms; this.ne = ne; this.lieu = lieu
    this.dept = dept; this.profession = profession; this.deces = deces; this.lieuDeces = lieuDeces
    this.age = age; this.situation = situation; this.domicile = domicile
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
        // v0.69 : formulaire compact - sections resserrees.
        Modifier.padding(top = 6.dp),
        fontWeight = FontWeight.Bold,
        color = CouleurVertFonce
    )
}

// v0.66 : titre de bloc avec PASTILLE d'etat — verte si le bloc est
// rempli, grise s'il est vide : lecture immediate du formulaire.
@Composable
fun TitrePastille(t: String, rempli: Boolean) {
    Row(
        Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(end = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(if (rempli) CouleurVert else MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(t, fontWeight = FontWeight.Bold, color = CouleurVertFonce)
    }
}

// v0.67 : REVUE NBMDS — chaque type d'acte ne porte pas les m\u00EAmes
// renseignements. N/B : identit\u00E9 des parents seulement (une ligne) ;
// M : tout ; D/S : ni naissance ni naissance des parents, d\u00E9c\u00E8s oui ;
// E : tout. avecNaissance/avecDeces s\u00E9lectionnent les lignes r\u00E9elles.
@Composable
fun BlocParent(
    libelle: String, e: ChampsParent, dateActe: String,
    avecNaissance: Boolean = true, avecDeces: Boolean = true,
    // v0.72 : coche "signe l'acte" derrière ce parent (null = pas de coche).
    cocheSigne: Boolean = false,
    onCocheSigne: (() -> Unit)? = null,
    // v0.99 : fiche existante du parent (si identifiable) pour le controle
    // de coherence IMMEDIAT de l'age saisi.
    fiche: Personne? = null
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TitrePastille(libelle, e.nom.isNotBlank())
    }
    val age = ageActe(dateActe, e.ne)
    if (!avecNaissance && !avecDeces) {
        // Acte N/B : une seule ligne, ce que la d\u00E9claration dit des parents.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Champ("Nom", e.nom, { e.nom = it }, modifier = Modifier.weight(1f))
            Champ("Pr\u00E9noms", e.prenoms, { e.prenoms = it }, modifier = Modifier.weight(1f))
            Champ("Profession", e.profession, { e.profession = it }, modifier = Modifier.weight(1f))
            if (onCocheSigne != null) {
                // v0.76 : colonne Signe CALQUÉE sur le champ "Nom" : hauteur
                // 56.dp identique à l'OutlinedTextField, libellé "Signe" en
                // tête comme le libellé du champ, coche centrée dessous.
                Column(Modifier.width(58.dp).height(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Signe", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Checkbox(checked = cocheSigne, onCheckedChange = { onCocheSigne() }, modifier = Modifier.size(40.dp))
                }
            }
        }
        // v0.97 : l'acte porte l'âge, la situation et le domicile du parent.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Champ("Âge", e.age, { e.age = it }, erreur = erreurAge(fiche?.dateNaissance ?: "", dateActe, e.age) != null, aide = erreurAge(fiche?.dateNaissance ?: "", dateActe, e.age) ?: "", modifier = Modifier.weight(0.7f))
            Champ("Situation", e.situation, { e.situation = it }, modifier = Modifier.weight(1f))
            Champ("Domicile", e.domicile, { e.domicile = it }, modifier = Modifier.weight(1.3f), communes = true)
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Champ("Nom", e.nom, { e.nom = it }, modifier = Modifier.weight(1f))
            Champ("Pr\u00E9noms", e.prenoms, { e.prenoms = it }, modifier = Modifier.weight(1f))
            if (onCocheSigne != null) {
                // v0.76 : colonne Signe CALQUÉE sur le champ "Nom" : hauteur
                // 56.dp identique à l'OutlinedTextField, libellé "Signe" en
                // tête comme le libellé du champ, coche centrée dessous.
                Column(Modifier.width(58.dp).height(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Signe", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Checkbox(checked = cocheSigne, onCheckedChange = { onCocheSigne() }, modifier = Modifier.size(40.dp))
                }
            }
        }
        if (avecNaissance) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Champ(
                    "N\u00E9(e) le", e.ne, { e.ne = it },
                    erreur = !dateValide(e.ne),
                    aide = if (age.isNotEmpty()) "\u00C2ge \u00E0 l'acte : " + age else "\u00C2ge calcul\u00E9 automatiquement",
                    modifier = Modifier.weight(1.1f)
                )
                Champ("Lieu", e.lieu, { e.lieu = it }, modifier = Modifier.weight(1f), communes = true, onDept = { e.dept = it })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (avecNaissance) {
                Champ("Profession", e.profession, { e.profession = it }, modifier = Modifier.weight(1.2f))
            }
            if (avecDeces) {
                Champ("D\u00E9c. le", e.deces, { e.deces = it }, erreur = !dateValide(e.deces), modifier = Modifier.weight(1f))
                Champ("Lieu d\u00E9c\u00E8s", e.lieuDeces, { e.lieuDeces = it }, modifier = Modifier.weight(1f), communes = true)
            }
        }
        // v0.97 : \u00e2ge \u00e0 l'acte, situation et domicile (tous les types).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Champ("\u00c2ge", e.age, { e.age = it }, erreur = erreurAge(fiche?.dateNaissance ?: "", dateActe, e.age) != null, aide = erreurAge(fiche?.dateNaissance ?: "", dateActe, e.age) ?: "", modifier = Modifier.weight(0.7f))
            Champ("Situation", e.situation, { e.situation = it }, modifier = Modifier.weight(1f))
            Champ("Domicile", e.domicile, { e.domicile = it }, modifier = Modifier.weight(1.3f), communes = true)
        }
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
    // GeneaPoche 4 : type impose par l'appui long sur un bloc de la fiche.
    typeImpose: String? = null,
    // v0.92 : carnet de sources (facultatif).
    sources: List<Source> = emptyList(),
    // v0.98 : citations relues pour conserver la source au changement
    // de personne (homonyme, d\u00E9clarant).
    citations: List<Citation> = emptyList(),
    onCreerSource: (Source) -> Long = { 0L }
) {
    var sourceActeId by remember(initial) { mutableStateOf(initial?.sourceId ?: 0L) }
    var numeroActe by remember(initial) { mutableStateOf(initial?.numeroActe ?: "") }
    var vueActe by remember(initial) { mutableStateOf(if ((initial?.vue ?: 0) > 0) initial!!.vue.toString() else "") }
    var vueTotalActe by remember(initial) { mutableStateOf(if ((initial?.vueTotal ?: 0) > 0) initial!!.vueTotal.toString() else "") }
    var fiabiliteActe by remember(initial) { mutableStateOf(initial?.fiabiliteSource ?: -1) }
    var type by remember(initial) { mutableStateOf(typeImpose ?: initial?.type ?: "N") }
    var commune by remember(initial) { mutableStateOf(initial?.commune ?: "") }
    var dept by remember(initial) { mutableStateOf(initial?.dept ?: "") }
    var enregistre by remember(initial) { mutableStateOf(initial?.enregistre ?: false) }
    var date by remember(initial) { mutableStateOf(initial?.date ?: "") }
    var heure by remember(initial) { mutableStateOf(initial?.heure ?: "") }
    // v0.97 : mentions marginales d'un acte de naissance (mariage, deces).
    var mentionMDate by remember(initial) { mutableStateOf(initial?.mentionMDate ?: "") }
    var mentionMLieu by remember(initial) { mutableStateOf(initial?.mentionMLieu ?: "") }
    // v0.99 : mention marginale de mariage nominative.
    var mentionMNom by remember(initial) { mutableStateOf(initial?.mentionMNom ?: "") }
    var mentionMPrenoms by remember(initial) { mutableStateOf(initial?.mentionMPrenoms ?: "") }
    var pickerMentionM by remember(initial) { mutableStateOf(false) }
    var mentionDDate by remember(initial) { mutableStateOf(initial?.mentionDDate ?: "") }
    var mentionDLieu by remember(initial) { mutableStateOf(initial?.mentionDLieu ?: "") }
    // v0.98 : champs valides B/M/D/S.
    var declNDate by remember(initial) { mutableStateOf(initial?.declNDate ?: "") }
    var declNHeure by remember(initial) { mutableStateOf(initial?.declNHeure ?: "") }
    var bLegitime by remember(initial) { mutableStateOf(initial?.bLegitime ?: "") }
    var contratMariage by remember(initial) { mutableStateOf(initial?.contratMariage ?: "") }
    var dispenses by remember(initial) { mutableStateOf(initial?.dispenses ?: "") }
    var declDDate by remember(initial) { mutableStateOf(initial?.declDDate ?: "") }
    var declDHeure by remember(initial) { mutableStateOf(initial?.declDHeure ?: "") }
    var declarantNom by remember(initial) { mutableStateOf(initial?.declarantNom ?: "") }
    var declarantProfession by remember(initial) { mutableStateOf(initial?.declarantProfession ?: "") }
    var declarantDomicile by remember(initial) { mutableStateOf(initial?.declarantDomicile ?: "") }
    var ceremonieSDate by remember(initial) { mutableStateOf(initial?.ceremonieSDate ?: "") }
    var cimetiere by remember(initial) { mutableStateOf(initial?.cimetiere ?: "") }
    var concession by remember(initial) { mutableStateOf(initial?.concession ?: "") }
    var ministreCulte by remember(initial) { mutableStateOf(initial?.ministreCulte ?: "") }
    var intitule by remember(initial) { mutableStateOf(initial?.intitule ?: "") }
    var acteId by remember(initial) { mutableStateOf(initial?.acteId ?: 0L) }
    // Parent choisi pour la naissance d'un enfant (menu Saisie).
    var parentPourEnfantId by remember(initial) { mutableStateOf(initial?.parentPourEnfant ?: 0L) }
    var personneId by remember(initial) { mutableStateOf(initial?.personneId ?: 0L) }
    var nom by remember(initial) { mutableStateOf(initial?.nom ?: "") }
    var prenoms by remember(initial) { mutableStateOf(initial?.prenoms ?: "") }
    var sexe by remember(initial) { mutableStateOf(initial?.sexe ?: "") }
    var profession by remember(initial) { mutableStateOf(initial?.profession ?: "") }
    var ageConcerne by remember(initial) { mutableStateOf(initial?.ageConcerne ?: "") }
    var marieeAge by remember(initial) { mutableStateOf(initial?.marieeAge ?: "") }
    val pere = remember(initial) {
        ChampsParent().also { c ->
            initial?.let { a ->
                c.remplir(a.pereNom, a.perePrenoms, a.pereNe, a.pereLieu, a.pereDept, a.pereProf, a.pereDeces, a.pereLieuDeces, a.pereAge, a.pereSituation, a.pereDomicile)
                c.id = a.pereId
            }
        }
    }
    val mere = remember(initial) {
        ChampsParent().also { c ->
            initial?.let { a ->
                c.remplir(a.mereNom, a.merePrenoms, a.mereNe, a.mereLieu, a.mereDept, a.mereProf, a.mereDeces, a.mereLieuDeces, a.mereAge, a.mereSituation, a.mereDomicile)
                c.id = a.mereId
            }
        }
    }
    var marieeId by remember(initial) { mutableStateOf(initial?.marieeId ?: 0L) }
    var marieeNom by remember(initial) { mutableStateOf(initial?.marieeNom ?: "") }
    var marieePrenoms by remember(initial) { mutableStateOf(initial?.marieePrenoms ?: "") }
    var marieeProf by remember(initial) { mutableStateOf(initial?.marieeProf ?: "") }
    val pereMariee = remember(initial) {
        ChampsParent().also { c ->
            initial?.let { a ->
                c.remplir(a.pMarieeNom, a.pMarieePrenoms, a.pMarieeNe, a.pMarieeLieu, a.pMarieeDept, a.pMarieeProf, a.pMarieeDeces, a.pMarieeLieuDeces)
            }
        }
    }
    val mereMariee = remember(initial) {
        ChampsParent().also { c ->
            initial?.let { a ->
                c.remplir(a.mMarieeNom, a.mMarieePrenoms, a.mMarieeNe, a.mMarieeLieu, a.mMarieeDept, a.mMarieeProf, a.mMarieeDeces, a.mMarieeLieuDeces)
            }
        }
    }
    val temoins = remember(initial) { mutableStateListOf<TemoinSaisie>().also { l -> initial?.temoins?.filter { it.role.isBlank() }?.forEach { l.add(it) } } }
    val signataires = remember(initial) { mutableStateListOf<String>().also { l -> initial?.signataires?.forEach { l.add(it) } } }
    // v0.72 : PRÊTRE (baptême, sépulture) ou OFFICIER D'ÉTAT CIVIL
    // (mariage) : un témoin de plus avec son rôle. La coche de signature
    // est désormais DERRIÈRE chaque personne citée, la zone "Qui signe ?"
    // disparaît.
    var roleOfficielNom by remember(initial) { mutableStateOf(initial?.temoins?.firstOrNull { it.role.isNotBlank() }?.nom ?: "") }
    // v0.73 : le pr\u00EAtre/officier a des PR\u00C9NOMS (plus de profession
    // pour le pr\u00EAtre, seul l'officier du mariage garde la sienne).
    var roleOfficielPrenoms by remember(initial) { mutableStateOf(initial?.temoins?.firstOrNull { it.role.isNotBlank() }?.prenoms ?: "") }
    var roleOfficielProf by remember(initial) { mutableStateOf(initial?.temoins?.firstOrNull { it.role.isNotBlank() }?.profession ?: "") }
    var pickerCible by remember { mutableStateOf(0) } // 0 = personne concernee, 1 = mariee
    var pickerOuvert by remember { mutableStateOf(false) }
    // v0.76 : fenêtre d'homonymes (nom + prénoms) : "pers" personne
    // concernée, "pm:<i>" parrain/marraine, "t:<i>" témoin.
    var homonymesOuvert by remember { mutableStateOf<String?>(null) }
    // v0.76 : un homonyme = même nom ET mêmes prénoms (plus de simple patronyme).
    val homonymesPers = personnes.filter { q ->
        nom.isNotBlank() && prenoms.isNotBlank() && q.id != personneId &&
            q.nom.trim().uppercase() == nom.trim().uppercase() &&
            q.prenoms.trim().uppercase() == prenoms.trim().uppercase()
    }.sortedBy { it.dateNaissance }
    // v0.77 : drapeau "Créer une nouvelle fiche" pour la personne concernée ;
    // il retombe tout seul si le nom ou les prénoms changent.
    var persNouvelle by remember(nom, prenoms) { mutableStateOf(false) }
    // v0.70 : le conjoint du defunt est-il ENCORE EN VIE (declarant de
    // l'acte) ou deja DECEDE avant lui (veuf/veuve) ?
    // v0.80 : le choix est PRE-REMPLI depuis l'acte edite et PERSISTE.
    var conjointEnVie by remember(initial) { mutableStateOf(initial?.conjointEnVie ?: true) }
    // v0.80 : enfant DECLARANT d'un acte de deces (fiche existante).
    var declarantEnfantId by remember(initial) { mutableStateOf(initial?.declarantEnfantId ?: 0L) }
    var pickerEnfantDecl by remember { mutableStateOf(false) }
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
        enregistre = a.enregistre
        date = a.date
        heure = a.heure
        intitule = a.intitule
        personneId = a.personneId
        nom = a.nom
        prenoms = a.prenoms
        sexe = a.sexe
        profession = a.profession
        pere.remplir(a.pereNom, a.perePrenoms, a.pereNe, a.pereLieu, a.pereDept, a.pereProf, a.pereDeces, a.pereLieuDeces, a.pereAge, a.pereSituation, a.pereDomicile); pere.id = a.pereId
        mere.remplir(a.mereNom, a.merePrenoms, a.mereNe, a.mereLieu, a.mereDept, a.mereProf, a.mereDeces, a.mereLieuDeces, a.mereAge, a.mereSituation, a.mereDomicile); mere.id = a.mereId
        marieeId = a.marieeId
        marieeNom = a.marieeNom; marieePrenoms = a.marieePrenoms; marieeProf = a.marieeProf
        ageConcerne = a.ageConcerne; marieeAge = a.marieeAge
        // v0.80 : les deux suspends PERSISTENT entre deux ouvertures du formulaire.
        conjointEnVie = a.conjointEnVie
        declarantEnfantId = a.declarantEnfantId
        mentionMDate = a.mentionMDate; mentionMLieu = a.mentionMLieu
        // v0.98 : recharger les nouveaux champs B/M/D/S.
        declNDate = a.declNDate; declNHeure = a.declNHeure
        bLegitime = a.bLegitime
        contratMariage = a.contratMariage; dispenses = a.dispenses
        declDDate = a.declDDate; declDHeure = a.declDHeure
        declarantNom = a.declarantNom; declarantProfession = a.declarantProfession; declarantDomicile = a.declarantDomicile
        ceremonieSDate = a.ceremonieSDate
        cimetiere = a.cimetiere; concession = a.concession; ministreCulte = a.ministreCulte
        mentionDDate = a.mentionDDate; mentionDLieu = a.mentionDLieu
        pereMariee.remplir(a.pMarieeNom, a.pMarieePrenoms, a.pMarieeNe, a.pMarieeLieu, a.pMarieeDept, a.pMarieeProf, a.pMarieeDeces, a.pMarieeLieuDeces)
        mereMariee.remplir(a.mMarieeNom, a.mMarieePrenoms, a.mMarieeNe, a.mMarieeLieu, a.mMarieeDept, a.mMarieeProf, a.mMarieeDeces, a.mMarieeLieuDeces)
        temoins.clear(); a.temoins.forEach { temoins.add(it) }
        signataires.clear(); a.signataires.forEach { signataires.add(it) }
        // v0.74 : le prêtre/officier de l'acte est PRÉ-REMPLI en édition.
        val tRole = a.temoins.firstOrNull { it.role.isNotBlank() }
        roleOfficielNom = tRole?.nom ?: ""
        roleOfficielPrenoms = tRole?.prenoms ?: ""
        roleOfficielProf = tRole?.profession ?: ""
    }


    // v0.99 : mode SAISIE RAPIDE — les champs essentiels d'abord (type,
    // acte, personne concern\u00e9e, conjoint, sources) ; le reste (details
    // par type, mentions marginales, temoins, officier) derriere "Completer".
    var rapide by remember { mutableStateOf(true) }

    // v0.58 : formulaire COMPACT. Le contenu defile, mais le bouton
    // Enregistrer reste TOUJOURS visible en bas, hors du defilement.
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(4.dp)
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
            // v0.69 : preuve de version VISIBLE en tete du formulaire.
            // Si ce numero n'apparait pas, l'application installee n'est
            // pas la derniere build : reinstallater le dernier APK.
            Text(
                "  v0.102",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                if (exPersonne == null) TextButton(onClick = { pickerCible = 0; pickerOuvert = true }) {
                    Text("Choisir", fontSize = 12.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = rapide, onClick = { rapide = true }, label = { Text("Rapide") })
            FilterChip(selected = !rapide, onClick = { rapide = false }, label = { Text("Compl\u00E9ter") })
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
            Champ("Commune", commune, { commune = it }, modifier = Modifier.weight(1f), communes = true, onDept = { dept = it })
        }
        // Chantier 12.0 : coche "Enregistr\u00E9" (l'acte est conserve ailleurs,
        // plus aucun stockage d'image/transcription dans l'application).
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = enregistre, onCheckedChange = { enregistre = it })
            Text("Enregistr\u00E9", fontSize = 13.sp)
        }
        ChampsSourceActe(
            sources = sources, sourceId = sourceActeId, onSource = { sourceActeId = it },
            numeroActe = numeroActe, onNumeroActe = { numeroActe = it },
            vue = vueActe, onVue = { vueActe = it },
            vueTotal = vueTotalActe, onVueTotal = { vueTotalActe = it },
            fiabilite = fiabiliteActe, onFiabilite = { fiabiliteActe = it },
            onCreerSource = onCreerSource
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ(
                "Date (jj/mm/aaaa)", date, { date = it; confirme = false },
                erreur = !dateValide(date),
                aide = periodeRealisteTxt(type, exPersonne),
                modifier = Modifier.weight(1f)
            )
            Champ("Heure", heure, { heure = it }, modifier = Modifier.weight(0.6f))
        }

        // v0.98 : champs valides B/M/D/S, adaptes au type d'acte.
        // v0.99 : masques en mode saisie rapide.
        if (!rapide) when (type) {
            "N" -> {
                SectionTitre("Déclaration")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Champ("Déclarée le (jj/mm/aaaa)", declNDate, { declNDate = it }, erreur = declNDate.isNotBlank() && !dateValide(declNDate), modifier = Modifier.weight(1f))
                    Champ("à", declNHeure, { declNHeure = it }, modifier = Modifier.weight(0.6f))
                }
            }
            "B" -> {
                SectionTitre("Enfant")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = bLegitime == "légitime", onClick = { bLegitime = "légitime" }, label = { Text("Légitime") })
                    FilterChip(selected = bLegitime == "naturel", onClick = { bLegitime = "naturel" }, label = { Text("Naturel") })
                    if (bLegitime.isNotBlank()) {
                        TextButton(onClick = { bLegitime = "" }) { Text("Effacer") }
                    }
                }
            }
            "M" -> {
                SectionTitre("Contrat et dispenses")
                Champ("Contrat de mariage (nom de l'office)", contratMariage, { contratMariage = it })
                Champ("Dispenses", dispenses, { dispenses = it })
            }
            "D" -> {
                SectionTitre("Déclaration de décès")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Champ("Déclarée le (jj/mm/aaaa)", declDDate, { declDDate = it }, erreur = declDDate.isNotBlank() && !dateValide(declDDate), modifier = Modifier.weight(1f))
                    Champ("à", declDHeure, { declDHeure = it }, modifier = Modifier.weight(0.6f))
                }
                Champ("Déclarant : nom", declarantNom, { declarantNom = it })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Champ("Profession", declarantProfession, { declarantProfession = it }, modifier = Modifier.weight(1f))
                    Champ("Domicile", declarantDomicile, { declarantDomicile = it }, modifier = Modifier.weight(1.3f), communes = true)
                }
            }
            "S" -> {
                SectionTitre("Inhumation")
                Champ("Cérémonie le (jj/mm/aaaa)", ceremonieSDate, { ceremonieSDate = it }, erreur = ceremonieSDate.isNotBlank() && !dateValide(ceremonieSDate))
                Champ("Cimetière (commune)", cimetiere, { cimetiere = it }, communes = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Champ("Concession", concession, { concession = it }, modifier = Modifier.weight(1f))
                    Champ("Ministre du culte", ministreCulte, { ministreCulte = it }, modifier = Modifier.weight(1.3f))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TitrePastille("Personne concern\u00E9e", exPersonne != null || nom.isNotBlank())
            // v0.72 : sur un mariage, l'époux (= personne concernée) signe.
            if (type == "M") {
                Spacer(Modifier.weight(1f))
                Checkbox(
                    checked = signataires.contains("Concerne(e)"),
                    onCheckedChange = { if (signataires.contains("Concerne(e)")) signataires.remove("Concerne(e)") else signataires.add("Concerne(e)") }
                )
            }
        }
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
        // v0.76 : homonyme = même nom ET mêmes prénoms ; plus de liste sous
        // les champs : un bouton ouvre une FENÊTRE pour reprendre la fiche
        // existante ou en créer une nouvelle.
        if (homonymesPers.isNotEmpty()) {
            OutlinedButton(onClick = { homonymesOuvert = "pers" }) {
                Text("Homonymes : " + homonymesPers.size.toString() + " – choisir ou créer")
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = sexe == "M", onClick = { sexe = "M" }, label = { Text("M") })
            FilterChip(selected = sexe == "F", onClick = { sexe = "F" }, label = { Text("F") })
            // v0.67 : un nouveau-n\u00E9 n'a pas de profession (N/B).
            if (type != "N" && type != "B") {
                Champ("Profession", profession, { profession = it }, modifier = Modifier.weight(1f))
            }
        }
        // v0.99 : age a l'acte (mariage, deces, sepulture) -> naissance
        // approximative calculee, controle immediat avec la fiche.
        if (type == "M" || type == "D" || type == "S") {
            Champ("\u00c2ge \u00e0 l'acte", ageConcerne, { ageConcerne = it },
                erreur = erreurAge(exPersonne?.dateNaissance ?: "", date, ageConcerne) != null,
                aide = erreurAge(exPersonne?.dateNaissance ?: "", date, ageConcerne) ?: "",
                modifier = Modifier.width(140.dp))
        }

        // v0.67 : N/B = parents en une ligne ; D/S = sans naissance des
        // parents ; M/E = tout. Le formulaire ne montre que ce que l'acte porte.
        val parentsNaissance = type == "M" || type == "E"
        val parentsDeces = type != "N" && type != "B"
        // v0.72 : coches de signature - le père sur N/B/M, la mère sur M.
        // v0.99 : fiche existante du parent (par id, sinon par nom) pour le
        // controle de coherence immediat de l'age saisi.
        val fichePere = personnes.firstOrNull { it.id == pere.id }
            ?: personnes.firstOrNull { cleNom(it.nom) == cleNom(pere.nom) && clePrenomsNom(it.prenoms) == clePrenomsNom(pere.prenoms) }
        val ficheMere = personnes.firstOrNull { it.id == mere.id }
            ?: personnes.firstOrNull { cleNom(it.nom) == cleNom(mere.nom) && clePrenomsNom(it.prenoms) == clePrenomsNom(mere.prenoms) }
        BlocParent("P\u00E8re", pere, date, parentsNaissance, parentsDeces,
            cocheSigne = signataires.contains("P\u00E8re"),
            onCocheSigne = if (type == "N" || type == "B" || type == "M") {
                { if (signataires.contains("P\u00E8re")) signataires.remove("P\u00E8re") else signataires.add("P\u00E8re") }
            } else null,
            fiche = fichePere)
        BlocParent("M\u00E8re", mere, date, parentsNaissance, parentsDeces,
            cocheSigne = signataires.contains("M\u00E8re"),
            onCocheSigne = if (type == "M") {
                { if (signataires.contains("M\u00E8re")) signataires.remove("M\u00E8re") else signataires.add("M\u00E8re") }
            } else null,
            fiche = ficheMere)
        // v0.97 : mentions marginales d'un acte de naissance : le mariage
        // et le deces portes en marge, avec date et lieu.
        // v0.99 : masquees en mode saisie rapide.
        if (type == "N" && !rapide) {
            TitrePastille("Mentions marginales", mentionMDate.isNotBlank() || mentionDDate.isNotBlank())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Champ("Marg. M : date", mentionMDate, { mentionMDate = it }, erreur = mentionMDate.isNotBlank() && !dateValide(mentionMDate), modifier = Modifier.weight(1f))
                Champ("lieu", mentionMLieu, { mentionMLieu = it }, modifier = Modifier.weight(1f), communes = true)
            }
            // v0.99 : mention marginale de mariage NOMINATIVE : nom et
            // pr\u00E9noms du conjoint, fiche existante \u00E0 choisir ou fiche
            // cr\u00E9\u00E9e \u00E0 l'enregistrement (anti-doublon).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Champ("Marg. M : nom", mentionMNom, { mentionMNom = it }, modifier = Modifier.weight(1f))
                Champ("pr\u00E9noms", mentionMPrenoms, { mentionMPrenoms = it }, modifier = Modifier.weight(1f))
            }
            if (mentionMNom.isNotBlank() || mentionMPrenoms.isNotBlank()) {
                OutlinedButton(onClick = { pickerMentionM = true }) {
                    Text("Choisir une fiche existante (sinon fiche cr\u00E9\u00E9e \u00E0 l'enregistrement)", fontSize = 12.sp)
                }
            }
            if (pickerMentionM) {
                AlertDialog(
                    onDismissRequest = { pickerMentionM = false },
                    title = { Text("Conjoint de la mention marginale") },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            personnes.sortedWith(
                                compareBy({ it.nom.trim().uppercase() }, { it.prenoms.trim().uppercase() })
                            ).forEach { cand ->
                                Text(
                                    libelleUnique(cand),
                                    Modifier.fillMaxWidth().clickable {
                                        pickerMentionM = false
                                        mentionMNom = cand.nom
                                        mentionMPrenoms = cand.prenoms
                                    }.padding(vertical = 0.dp),
                                    fontSize = 13.sp, lineHeight = 16.sp
                                )
                            }
                            if (personnes.isEmpty()) {
                                Text("Aucune fiche : le nom saisi cr\u00E9era la fiche du conjoint.")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { pickerMentionM = false }) { Text("Fermer") }
                    }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Champ("Marg. D : date", mentionDDate, { mentionDDate = it }, erreur = mentionDDate.isNotBlank() && !dateValide(mentionDDate), modifier = Modifier.weight(1f))
                Champ("lieu", mentionDLieu, { mentionDLieu = it }, modifier = Modifier.weight(1f), communes = true)
            }
        }

        // v0.64 : liens ACTUELS de la fiche en clair. Si la fiche porte un
        // lien errone (ex une mere qui pointe sur la belle-mere), c'est ICI
        // qu'on le voit : le formulaire recopie la fiche.
        if (exPersonne != null) {
            val pereFiche = personnes.firstOrNull { it.id == exPersonne.pere }
            val mereFiche = personnes.firstOrNull { it.id == exPersonne.mere }
            Text(
                "Liens de la fiche \u2014 P\u00E8re : " + (pereFiche?.titre() ?: "aucun") +
                    " \u00B7 M\u00E8re : " + (mereFiche?.titre() ?: "aucune"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // v0.67 : le conjoint et ses parents n'existent que sur un acte de
        // MARIAGE. Un acte de naissance n'a ni conjoint ni beaux-parents :
        // la section ne s'affiche plus pour N/B/D/S.
        // v0.68 : acte de d\u00E9c\u00E8s/s\u00E9pulture — le conjoint survivant
        // (\u00AB veuf/veuve de X \u00BB) tient sur une seule ligne.
        if (type == "D" || type == "S") {
            // v0.70 : le conjoint peut etre ENCORE EN VIE (declarant) ou
            // deja decede avant le defunt ("veuf/veuve de X") : l'acte
            // porte l'un ou l'autre, pas seulement un survivant.
            TitrePastille("Conjoint", marieeNom.isNotBlank())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = conjointEnVie, onClick = { conjointEnVie = true }, label = { Text("En vie") })
                FilterChip(selected = !conjointEnVie, onClick = { conjointEnVie = false }, label = { Text("D\u00E9c\u00E9d\u00E9 avant (veuf/veuve)") })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Champ(
                    if (conjointEnVie) "Nom du conjoint survivant" else "Nom du conjoint d\u00E9c\u00E9d\u00E9",
                    marieeNom, { marieeNom = it }, modifier = Modifier.weight(1f)
                )
                Champ("Pr\u00E9noms", marieePrenoms, { marieePrenoms = it }, modifier = Modifier.weight(1f))
            }
            // v0.72 : le conjoint survivant renseigné peut signer (déclarant).
            if (marieeNom.isNotBlank()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Signe l'acte (d\u00E9clarant)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Checkbox(
                        checked = signataires.contains("Conjoint(e)"),
                        onCheckedChange = { if (signataires.contains("Conjoint(e)")) signataires.remove("Conjoint(e)") else signataires.add("Conjoint(e)") }
                    )
                }
            }
            // v0.80 : sur un DÉCÈS, un ENFANT du défunt peut être le
            // déclarant (fiche existante, coche de signature automatique).
            if (type == "D" && exPersonne != null) {
                val enfantsDecl = personnes.filter { q ->
                    q.pere == exPersonne.id || q.mere == exPersonne.id || exPersonne.enfants.contains(q.id)
                }
                if (enfantsDecl.isNotEmpty()) {
                    val enfChoisi = enfantsDecl.firstOrNull { it.id == declarantEnfantId }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (enfChoisi != null) "Enfant d\u00E9clarant : " + enfChoisi.titre()
                            else "Enfant d\u00E9clarant (signe l'acte)",
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall
                        )
                        if (enfChoisi == null) {
                            OutlinedButton(onClick = { pickerEnfantDecl = true }) { Text("Choisir", fontSize = 12.sp) }
                        } else {
                            TextButton(onClick = {
                                declarantEnfantId = 0L
                                signataires.remove("Enfant d\u00E9clarant")
                            }) { Text("Retirer", fontSize = 12.sp) }
                        }
                    }
                    if (pickerEnfantDecl) {
                        AlertDialog(
                            onDismissRequest = { pickerEnfantDecl = false },
                            title = { Text("Enfant d\u00E9clarant") },
                            text = {
                                Column(Modifier.verticalScroll(rememberScrollState())) {
                                    enfantsDecl.sortedWith(
                                        compareBy({ it.nom.trim().uppercase() }, { it.prenoms.trim().uppercase() })
                                    ).forEach { cand ->
                                        // v0.79 : identifiant unique, liste compacte alignee a gauche.
                                        Text(
                                            libelleUnique(cand),
                                            Modifier.fillMaxWidth().clickable {
                                                declarantEnfantId = cand.id
                                                if (!signataires.contains("Enfant d\u00E9clarant")) signataires.add("Enfant d\u00E9clarant")
                                                pickerEnfantDecl = false
                                            }.padding(vertical = 0.dp),
                                            fontSize = 13.sp, lineHeight = 16.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = { TextButton(onClick = { pickerEnfantDecl = false }) { Text("Fermer") } }
                        )
                    }
                }
            }
        }
        if (type == "M" || type == "E") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TitrePastille(if (type == "M") "Mari\u00E9e" else "Conjoint(e)", marieeNom.isNotBlank() || marieeId != 0L)
                // v0.72 : l'épouse signe le mariage.
                if (type == "M") {
                    Spacer(Modifier.weight(1f))
                    Checkbox(
                        checked = signataires.contains("Mari\u00E9e"),
                        onCheckedChange = { if (signataires.contains("Mari\u00E9e")) signataires.remove("Mari\u00E9e") else signataires.add("Mari\u00E9e") }
                    )
                }
            }
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
            if (type == "M") {
                Champ("\u00c2ge \u00e0 l'acte", marieeAge, { marieeAge = it },
                    erreur = erreurAge(exMariee?.dateNaissance ?: "", date, marieeAge) != null,
                    aide = erreurAge(exMariee?.dateNaissance ?: "", date, marieeAge) ?: "",
                    modifier = Modifier.width(140.dp))
            }
            // v0.72 : les 4 parents signent le mariage - coches derrière chacun.
            BlocParent(if (type == "M") "P\u00E8re de la mari\u00E9e" else "P\u00E8re du conjoint", pereMariee, date,
                cocheSigne = signataires.contains("P\u00E8re de la mari\u00E9e"),
                onCocheSigne = if (type == "M") {
                    { if (signataires.contains("P\u00E8re de la mari\u00E9e")) signataires.remove("P\u00E8re de la mari\u00E9e") else signataires.add("P\u00E8re de la mari\u00E9e") }
                } else null)
            BlocParent(if (type == "M") "M\u00E8re de la mari\u00E9e" else "M\u00E8re du conjoint", mereMariee, date,
                cocheSigne = signataires.contains("M\u00E8re de la mari\u00E9e"),
                onCocheSigne = if (type == "M") {
                    { if (signataires.contains("M\u00E8re de la mari\u00E9e")) signataires.remove("M\u00E8re de la mari\u00E9e") else signataires.add("M\u00E8re de la mari\u00E9e") }
                } else null)
        }

        // v0.68 : sur un bapt\u00EAme, les t\u00E9moins sont le parrain et la marraine.
        // v0.99 : temoins et officier masques en mode saisie rapide.
        if (!rapide) {
        TitrePastille(if (type == "B") "Parrain et marraine" else "T\u00E9moins", temoins.any { it.nom.isNotBlank() })
        // v0.73 : sur un bapt\u00EAme, PARRAIN et MARRAINE - deux lignes
        // fixes (nom + pr\u00E9noms, SANS profession) avec leur coche "Signe" ;
        // sur les autres actes, t\u00E9moins avec profession. Une fiche
        // existante qui correspond au nom saisi est propos\u00E9e (coh\u00E9rence).
        if (type == "B") {
            while (temoins.size < 2) temoins.add(TemoinSaisie())
            listOf("Parrain", "Marraine").forEachIndexed { i, libelle ->
                val t = temoins[i]
                // v0.76 : homonymes = même nom ET mêmes prénoms, proposés dans
                // une FENÊTRE (une ligne compacte alignée à gauche, avec dates).
                val homosPm = personnes.filter { q ->
                    t.nom.isNotBlank() && t.prenoms.isNotBlank() &&
                        q.nom.trim().uppercase() == t.nom.trim().uppercase() &&
                        q.prenoms.trim().uppercase() == t.prenoms.trim().uppercase()
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Champ(libelle, t.nom, { temoins[i] = t.copy(nom = it) }, modifier = Modifier.weight(1f))
                            Champ("Pr\u00E9noms", t.prenoms, { temoins[i] = t.copy(prenoms = it) }, modifier = Modifier.weight(1f))
                            // v0.76 : colonne Signe CALQUÉE sur le champ (même
                            // hauteur que l'OutlinedTextField, libellé en tête).
                            Column(Modifier.width(58.dp).height(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Signe", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Checkbox(
                                    checked = signataires.contains(libelle),
                                    onCheckedChange = {
                                        if (signataires.contains(libelle)) signataires.remove(libelle) else signataires.add(libelle)
                                    },
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        if (homosPm.isNotEmpty()) {
                            OutlinedButton(onClick = { homonymesOuvert = "pm:" + i.toString() }) {
                                Text("Homonymes : " + homosPm.size.toString() + " – choisir ou créer")
                            }
                        }
                    }
                }
                if (homonymesOuvert == "pm:" + i.toString()) {
                    DialogueHomonymes(
                        titre = libelle + " : fiches homonymes",
                        candidats = homosPm,
                        onChoisir = { q ->
                            temoins[i] = t.copy(nom = q.nom, prenoms = q.prenoms, nouveau = false)
                            homonymesOuvert = null
                        },
                        onCreer = { temoins[i] = t.copy(nouveau = true); homonymesOuvert = null },
                        onFermer = { homonymesOuvert = null }
                    )
                }
            }
        } else {
        temoins.forEachIndexed { i, t ->
            // v0.76 : homonymes = même nom ET mêmes prénoms (fenêtre de choix).
            val homosT = personnes.filter { q ->
                t.nom.isNotBlank() && t.prenoms.isNotBlank() &&
                    q.nom.trim().uppercase() == t.nom.trim().uppercase() &&
                    q.prenoms.trim().uppercase() == t.prenoms.trim().uppercase()
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Champ("Nom du t\u00E9moin", t.nom, { temoins[i] = t.copy(nom = it) }, modifier = Modifier.weight(1f))
                        Champ("Pr\u00E9noms", t.prenoms, { temoins[i] = t.copy(prenoms = it) }, modifier = Modifier.weight(1f))
                        Champ("Profession", t.profession, { temoins[i] = t.copy(profession = it) }, modifier = Modifier.weight(1f))
                        Column(Modifier.width(58.dp).height(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Signe", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Checkbox(
                                checked = signataires.contains("T\u00E9moin " + (i + 1).toString()),
                                onCheckedChange = {
                                    if (signataires.contains("T\u00E9moin " + (i + 1).toString())) signataires.remove("T\u00E9moin " + (i + 1).toString())
                                    else signataires.add("T\u00E9moin " + (i + 1).toString())
                                },
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    // v0.97 : sur un acte de naissance, l'acte porte l'age,
                    // la situation et le domicile de chaque temoin.
                    // v0.98 : idem pour le parrain/marraine d'un baptême.
                    if (type == "N" || type == "B") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // v0.99 : controle de coherence immediat de l'age
                            // du temoin (fiche existante retrouvee par nom).
                            Champ("Âge", t.age, { temoins[i] = t.copy(age = it) },
                                erreur = erreurAge(
                                    personnes.firstOrNull { cleNom(it.nom) == cleNom(t.nom) && clePrenomsNom(it.prenoms) == clePrenomsNom(t.prenoms) }?.dateNaissance ?: "",
                                    date, t.age) != null,
                                aide = erreurAge(
                                    personnes.firstOrNull { cleNom(it.nom) == cleNom(t.nom) && clePrenomsNom(it.prenoms) == clePrenomsNom(t.prenoms) }?.dateNaissance ?: "",
                                    date, t.age) ?: "",
                                modifier = Modifier.weight(0.7f))
                            Champ("Situation", t.situation, { temoins[i] = t.copy(situation = it) }, modifier = Modifier.weight(1f))
                            Champ("Domicile", t.domicile, { temoins[i] = t.copy(domicile = it) }, modifier = Modifier.weight(1.3f), communes = true)
                        }
                    }
                    if (homosT.isNotEmpty()) {
                        OutlinedButton(onClick = { homonymesOuvert = "t:" + i.toString() }) {
                            Text("Homonymes : " + homosT.size.toString() + " – choisir ou créer")
                        }
                    }
                }
                IconButton(onClick = {
                    signataires.remove("T\u00E9moin " + (i + 1).toString())
                    temoins.removeAt(i)
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Retirer")
                }
            }
            if (homonymesOuvert == "t:" + i.toString()) {
                DialogueHomonymes(
                    titre = "T\u00E9moin : fiches homonymes",
                    candidats = homosT,
                    onChoisir = { q ->
                        temoins[i] = t.copy(nom = q.nom, prenoms = q.prenoms, nouveau = false)
                        homonymesOuvert = null
                    },
                    onCreer = { temoins[i] = t.copy(nouveau = true); homonymesOuvert = null },
                    onFermer = { homonymesOuvert = null }
                )
            }
        }
        }
        if (temoins.size < 4) {
            OutlinedButton(onClick = { temoins.add(TemoinSaisie()) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Ajouter un t\u00E9moin")
            }
        }

        // v0.74 : PRÊTRE (baptême, sépulture) ou OFFICIER D'ÉTAT CIVIL
        // (naissance, mariage, décès - il signe tout acte d'état civil) :
        // un témoin de plus avec son rôle, coche s'il signe.
        if (type != "E") {
            // v0.73 : PR\u00CATRE corrig\u00E9 (l'affichage b\u00E2tard cassait
            // aussi le mapping des signataires) ; le pr\u00EAtre porte nom et
            // pr\u00E9noms, SANS profession ; seul l'officier d'\u00E9tat civil du
            // mariage garde la sienne. Coche "Signe" align\u00E9e sur les champs.
            val roleOfficiel = if (type == "B" || type == "S") "Pr\u00EAtre" else "Officier d'\u00E9tat civil"
            TitrePastille(roleOfficiel, roleOfficielNom.isNotBlank())
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Champ("Nom", roleOfficielNom, { roleOfficielNom = it }, modifier = Modifier.weight(1f))
                Champ("Pr\u00E9noms", roleOfficielPrenoms, { roleOfficielPrenoms = it }, modifier = Modifier.weight(1f))
                if (type != "B" && type != "S") {
                    Champ("Profession", roleOfficielProf, { roleOfficielProf = it }, modifier = Modifier.weight(1f))
                }
                // v0.76 : colonne Signe CALQUÉE sur le champ (hauteur 56.dp).
                Column(Modifier.width(58.dp).height(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Signe", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Checkbox(
                        checked = signataires.contains(roleOfficiel),
                        onCheckedChange = { if (signataires.contains(roleOfficiel)) signataires.remove(roleOfficiel) else signataires.add(roleOfficiel) },
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        } // fin if (!rapide) temoins + officier


        }
        // v0.58 : bouton TOUJOURS visible en bas, hors du defilement.
        Button(
            onClick = {
                val acte = ActeSaisie(
                    acteId = acteId,
                    type = type,
                    commune = commune.trim(),
                    dept = normaliserDept(dept),
                    enregistre = enregistre,
                    sourceId = sourceActeId,
                    numeroActe = numeroActe.trim(),
                    vue = vueActe.toIntOrNull() ?: 0,
                    vueTotal = vueTotalActe.toIntOrNull() ?: 0,
                    fiabiliteSource = fiabiliteActe,
                    date = date.trim(),
                    heure = heure.trim(),
                    intitule = intitule.trim(),
                    parentPourEnfant = parentPourEnfantId,
                    personneId = personneId,
                    pereId = pere.id,
                    mereId = mere.id,
                    pereNomInit = initial?.pereNom ?: "",
                    mereNomInit = initial?.mereNom ?: "",
                    nom = nom.trim(),
                    prenoms = prenoms.trim(),
                    sexe = sexe,
                    profession = profession.trim(),
                    pereNom = pere.nom.trim(), perePrenoms = pere.prenoms.trim(),
                    pereNe = pere.ne.trim(), pereLieu = pere.lieu.trim(), pereDept = normaliserDept(pere.dept),
                    pereProf = pere.profession.trim(), pereDeces = pere.deces.trim(), pereLieuDeces = pere.lieuDeces.trim(),
                    pereAge = pere.age.trim(), pereSituation = pere.situation.trim(), pereDomicile = pere.domicile.trim(),
                    mereNom = mere.nom.trim(), merePrenoms = mere.prenoms.trim(),
                    mereNe = mere.ne.trim(), mereLieu = mere.lieu.trim(), mereDept = normaliserDept(mere.dept),
                    mereProf = mere.profession.trim(), mereDeces = mere.deces.trim(), mereLieuDeces = mere.lieuDeces.trim(),
                    mereAge = mere.age.trim(), mereSituation = mere.situation.trim(), mereDomicile = mere.domicile.trim(),
                    mentionMDate = mentionMDate.trim(), mentionMLieu = mentionMLieu.trim(),
                    mentionMNom = mentionMNom.trim(), mentionMPrenoms = mentionMPrenoms.trim(),
                    mentionDDate = mentionDDate.trim(), mentionDLieu = mentionDLieu.trim(),
                    marieeId = marieeId,
                    marieeNom = marieeNom.trim(), marieePrenoms = marieePrenoms.trim(),
                    marieeProf = marieeProf.trim(),
                    ageConcerne = ageConcerne.trim(), marieeAge = marieeAge.trim(),
                    pMarieeNom = pereMariee.nom.trim(), pMarieePrenoms = pereMariee.prenoms.trim(),
                    pMarieeNe = pereMariee.ne.trim(), pMarieeLieu = pereMariee.lieu.trim(), pMarieeDept = normaliserDept(pereMariee.dept),
                    pMarieeProf = pereMariee.profession.trim(), pMarieeDeces = pereMariee.deces.trim(), pMarieeLieuDeces = pereMariee.lieuDeces.trim(),
                    mMarieeNom = mereMariee.nom.trim(), mMarieePrenoms = mereMariee.prenoms.trim(),
                    mMarieeNe = mereMariee.ne.trim(), mMarieeLieu = mereMariee.lieu.trim(), mMarieeDept = normaliserDept(mereMariee.dept),
                    mMarieeProf = mereMariee.profession.trim(), mMarieeDeces = mereMariee.deces.trim(), mMarieeLieuDeces = mereMariee.lieuDeces.trim(),
                    temoins = (if (type == "B") temoins.take(2).toList() else temoins.toList()) + listOfNotNull(
                        if (type != "E" && roleOfficielNom.isNotBlank())
                            TemoinSaisie(nom = roleOfficielNom.trim(), prenoms = roleOfficielPrenoms.trim(), profession = roleOfficielProf.trim(),
                                role = if (type == "B" || type == "S") "Pr\u00EAtre" else "Officier d'état civil") else null
                    ),
                    signataires = if (type == "N") signataires.filter { it != "M\u00E8re" } else signataires.toList(),
                    // v0.77 : drapeau "Créer une nouvelle fiche" ENFIN passé
                    // (il manquait : le choix était perdu à l'enregistrement).
                    // v0.80 : les deux suspends persistent dans l'acte.
                    persNouvelle = persNouvelle,
                    conjointEnVie = conjointEnVie,
                    declarantEnfantId = declarantEnfantId,
                    // v0.98 : nouveaux champs B/M/D/S.
                    declNDate = declNDate.trim(), declNHeure = declNHeure.trim(),
                    bLegitime = bLegitime.trim(),
                    contratMariage = contratMariage.trim(), dispenses = dispenses.trim(),
                    declDDate = declDDate.trim(), declDHeure = declDHeure.trim(),
                    declarantNom = declarantNom.trim(), declarantProfession = declarantProfession.trim(), declarantDomicile = declarantDomicile.trim(),
                    ceremonieSDate = ceremonieSDate.trim(),
                    cimetiere = cimetiere.trim(), concession = concession.trim(), ministreCulte = ministreCulte.trim()
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
            // v0.91 : une date ancienne hors format ne bloque plus la
            // sauvegarde de l'acte (champ date en rouge + journal).
            enabled = (personneId != 0L || nom.isNotBlank()) && heureValide(heure),
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

    if (homonymesOuvert == "pers") {
        DialogueHomonymes(
            titre = "Personne concern\u00E9e : fiches homonymes",
            candidats = homonymesPers,
            onChoisir = { q ->
                remplirDepuis(prefillActe(q, personnes, type, citations = citations))
                persNouvelle = false
                homonymesOuvert = null
            },
            onCreer = { persNouvelle = true; homonymesOuvert = null },
            onFermer = { homonymesOuvert = null }
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
                        // v0.72 : liste RESSENREE et ALIGNEE A GAUCHE
                        // (une ligne compacte par personne).
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    if (pickerCible == 0) {
                                        // G\u00E9n\u00E9aPoche 3 : la personne existante est
                                        // PROPOSEE avec tout ce qui est connu d'elle ;
                                        // si son acte de ce type existe deja, il est
                                        // EDITE (aucun doublon).
                                        remplirDepuis(prefillActe(cand, personnes, type, citations = citations))
                                    } else {
                                        marieeId = cand.id
                                        marieeNom = cand.nom
                                        marieePrenoms = cand.prenoms
                                    }
                                    pickerOuvert = false
                                }
                                .padding(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                libelleUnique(cand),
                                fontSize = 13.sp, lineHeight = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerOuvert = false }) { Text("Fermer") }
            }
        )
    }
}

// v0.79 : identifiant UNIQUE de toutes les listes de choix de l'application :
// titre, puis N. et D. (r\u00E8gle des 100 ans respect\u00E9e) \u2014 deux homonymes
// ne se ressemblent plus dans un picker.
fun libelleUnique(p: Personne): String =
    p.titre() +
        (if (p.dateNaissance.trim().isNotEmpty()) " \u2013 N. " + dateAffichee(p.dateNaissance.trim()) else "") +
        (if (decesAffichable(p)) " \u2013 D. " + dateAffichee(p.dateDeces.trim()) else "")

// ---------------------------------------------------------------- Fenêtre d'homonymes
// v0.76 : un homonyme = même nom ET mêmes prénoms. Une seule FENÊTRE
// propose les fiches existantes (une ligne compacte alignée à gauche,
// avec dates de naissance et de décès) ; "Créer une nouvelle fiche"
// laisse la saisie telle quelle.
@Composable
fun DialogueHomonymes(
    titre: String,
    candidats: List<Personne>,
    onChoisir: (Personne) -> Unit,
    // v0.77 : "Créer une nouvelle fiche" force une fiche distincte
    // (vrai homonyme : date de naissance différente).
    onCreer: (() -> Unit)? = null,
    onFermer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(titre) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "M\u00EAme nom et m\u00EAmes pr\u00E9noms : touchez la fiche \u00E0 reprendre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                candidats.forEach { q ->
                    Text(
                        libelleUnique(q),
                        Modifier.fillMaxWidth().clickable { onChoisir(q) }.padding(vertical = 0.dp),
                        fontSize = 13.sp, lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (onCreer != null) onCreer() else onFermer() }) { Text("Cr\u00E9er une nouvelle fiche") } },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)
    ) {
        Text(
            "Actes",
            style = MaterialTheme.typography.titleMedium,
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
                        // v0.76 : liste condensée et alignée à gauche.
                        Text(
                            libelleUnique(cand),
                            Modifier.fillMaxWidth().clickable {
                                pickerParent = false
                                onEnfant(cand.id)
                            }.padding(vertical = 0.dp),
                            fontSize = 13.sp, lineHeight = 16.sp
                        )
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




