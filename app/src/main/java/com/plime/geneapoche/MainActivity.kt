package com.plime.geneapoche

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import java.io.ByteArrayOutputStream



// ---------------------------------------------------------------- Etat de verrouillage

class Verrou {
    var deverrouille by mutableStateOf(false)
    var dernierArret by mutableStateOf(0L)
}

// ---------------------------------------------------------------- Activite

// G3-17 : calculateWindowSizeClass est experimental pour l'instant.
@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    val verrou = Verrou()

    // G1-14 : verrouillage aussi a l'extinction de l'ecran.
    private val recepteurEcran = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                verrou.deverrouille = false
                Stockage.verrouillerStockage()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // G3-05 : ecran de demarrage aux couleurs de l'application,
        // puis bascule vers Theme.GeneaPoche (postSplashScreenTheme).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // G3-03 : affichage bord a bord ; les ecrans gerent les marges
        // (barre d'etat, barre de navigation, clavier) via les insets.
        enableEdgeToEdge()
        // FLAG_SECURE : captures d'ecran et apercu dans les recents interdits
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        registerReceiver(recepteurEcran, android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF))
        setContent {
            // G3-02 : theme clair/sombre selon le choix memorise, le mode
            // systeme suit isSystemInDarkTheme().
            val etatTheme = remember { mutableStateOf(Stockage.lireTheme(this)) }
            // G3-17 : classe de taille de fenetre pour la mise en page
            // adaptative (liste sur deux colonnes en paysage/tablette).
            val classeTaille = androidx.compose.material3.windowsizeclass.calculateWindowSizeClass(this)
            val sombre = when (etatTheme.value) {
                "clair" -> false
                "sombre" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MaterialTheme(
                colorScheme = if (sombre) PaletteGeneaPocheSombre else PaletteGeneaPoche,
                shapes = FormesGeneaPoche,
                typography = TypographieGeneaPoche
            ) {
                AppEcran(this, verrou, etatTheme, classeTaille)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        verrou.dernierArret = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        // G1-14 : delai de reverrouillage reglable (immediat par defaut).
        val delai = Stockage.lireDelaiVerrouillage(this)
        val d = verrou.dernierArret
        if (delai == 0L || (d > 0L && System.currentTimeMillis() - d > delai)) {
            verrou.deverrouille = false
            // La cle maitre quitte la memoire des le verrouillage (G1-01).
            Stockage.verrouillerStockage()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(recepteurEcran)
        } catch (e: Exception) {
            // deja desenregistre
        }
    }
}

// ---------------------------------------------------------------- Ecran principal

@Composable
fun AppEcran(
    ctx: Context,
    verrou: Verrou,
    themeChoisi: androidx.compose.runtime.MutableState<String>,
    classeTaille: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    // G2-07 : l'ETAT de l'application vit dans le modele de vue, qui SURVIT
    // a la rotation et au changement de theme ; les alias locaux ci-dessous
    // gardent les memes noms que le reste du code utilise deja.
    val modele: ModeleVueGeneaPoche = viewModel()
    val personnes = modele.personnes
    val pistes = modele.pistes
    val actes = modele.actes
    val pileFiches = modele.pileFiches
    val categoriesIgnorees = modele.categoriesIgnorees
    var charge by modele::charge
    var aPin by remember { mutableStateOf(false) }
    var message by modele::message
    var idOuvert by modele::idOuvert
    var onglet by modele::onglet
    var editionOuverte by modele::editionOuverte
    var editionCible by modele::editionCible
    var pickerRole by modele::pickerRole
    var confirmSuppression by modele::confirmSuppression
    var acteVisionne by modele::acteVisionne
    var acteAConfirmer by modele::acteAConfirmer
    var arbreCentre by modele::arbreCentre
    var sosaCujus by modele::sosaCujus
    var cujusDialogue by modele::cujusDialogue
    var cujusPreselec by modele::cujusPreselec
    var importConfirme by modele::importConfirme
    var importRapport by modele::importRapport
    var cujusEdite by modele::cujusEdite
    var cujasOuvert by modele::cujasOuvert
    var fichiersIllisibles by modele::fichiersIllisibles
    var avertissementLectureFerme by modele::avertissementLectureFerme
    var dialoguePin by modele::dialoguePin
    var dialogueDelai by modele::dialogueDelai
    // G3-02 : dialogue de choix du theme (clair / sombre / systeme).
    var dialogueTheme by remember { mutableStateOf(false) }
    var occupation by modele::occupation
    val scopeTravail = rememberCoroutineScope()

    // G5 : cache des derniers textes JSON ecrits sur disque. enregistrer()
    // compare le texte courant au cache et ne reecrit (chiffrement AES, ecriture
    // durable, relecture, rotation .bak) QUE les fichiers reellement changes :
    // une simple coche de temoin ne recalcule plus rien et ne reecrit rien.
    val derniersJson = remember { mutableMapOf<String, String>() }

    // Recopie structurelle des listes : force Compose a rafraichir l'affichage
    // meme quand une Personne est modifiee en place (correctif mise a jour immediate).
    fun rafraichirTout() {
        val cp = personnes.toList()
        personnes.clear()
        personnes.addAll(cp)
        val cq = pistes.toList()
        pistes.clear()
        pistes.addAll(cq)
        val ca = actes.toList()
        actes.clear()
        actes.addAll(ca)
    }

    // G1-13 : une recherche resolsee portant une note ne disparait jamais sans
    // filet : la note est recopiee dans les notes libres de la personne.
    // Seules les recherches SANS note sont supprimees sans question.
    fun conserverNote(piste: Piste) {
        val note = piste.enCours.trim()
        if (note.isEmpty()) return
        val pers = personneParId(personnes, piste.personneId) ?: return
        pers.notesLibres = (if (pers.notesLibres.isBlank()) note else pers.notesLibres + "\n" + note).trim()
    }

    // Synchronise le journal avec les donnees manquantes (grisees dans les fiches) :
    // une recherche dont la donnee est maintenant presente (saisie ou image) est
    // SUPPRIMEE (plus aucun archivage, plus aucun historique) ; chaque donnee
    // manquante sans recherche ouverte en obtient une.
    fun completerJournal(personnesTouchees: Set<Long>? = null) {
        if (!charge) return
        val maintenant = System.currentTimeMillis()
        val aRetirer = mutableListOf<Piste>()
        pistes.filter { !it.faite }.forEach { ps ->
            // G2-17 : les rapports d'integrite restent dans le journal
            // jusqu'a leur suppression manuelle.
            if (ps.categorie == "controle") return@forEach
            // G5 : synchronisation limitee aux personnes touchees quand
            // l'appelant le precise (une coche ne rescanne pas toute la base).
            if (personnesTouchees != null && ps.personneId !in personnesTouchees) return@forEach
            val pers = personneParId(personnes, ps.personneId)
            // Les pistes de validation (coche negative d'une information)
            // restent ouvertes jusqu'a re-validation manuelle.
            if (ps.categorie.startsWith("verif_") && pers != null) return@forEach
            val encoreManquante = pers != null &&
                suggestionsPour(pers, personnes, actes).any { it.cle == ps.categorie }
            if (pers == null || !encoreManquante) {
                aRetirer.add(ps)
            }
        }
        aRetirer.forEach { conserverNote(it) }
        pistes.removeAll(aRetirer)
        // G5 : creation PAR LOT (une seule ecriture de preferences pour tous
        // les identifiants, au lieu d'un commit par piste sur le thread UI).
        val aCreer = mutableListOf<Pair<Long, Suggestion>>()
        personnes.filter { personnesTouchees == null || it.id in personnesTouchees!! }.forEach { p ->
            val existantes = pistes.filter { !it.faite && it.personneId == p.id }.map { it.categorie }.toSet()
            suggestionsPour(p, personnes, actes).forEach { s ->
                if (categoriesIgnorees.contains(p.id.toString() + "|" + s.cle)) return@forEach
                if (s.cle !in existantes) aCreer.add(p.id to s)
            }
        }
        val ids = if (aCreer.isEmpty()) listOf() else Stockage.prochainIds(ctx, aCreer.size)
        aCreer.forEachIndexed { i, (personneId, s) ->
            pistes.add(
                Piste(
                    id = ids[i],
                    texte = s.titre,
                    personneId = personneId,
                    creation = maintenant,
                    type = "A",
                    categorie = s.cle,
                    lieu = s.lieu,
                    dept = s.dept,
                    debut = s.debut,
                    fin = s.fin
                )
            )
        }
    }

    fun enregistrer(recalculNumerotation: Boolean = true, personnesTouchees: Set<Long>? = null): Boolean {
        // G1-04 : donnees illisibles -> AUCUNE sauvegarde, ne rien ecraser.
        if (fichiersIllisibles.isNotEmpty()) {
            message = "Donn\u00E9es illisibles : enregistrement bloqu\u00E9 (restaurez la copie .bak)."
            return false
        }
        // Numerotation Sosa / d'Aboville recalculee seulement quand la
        // structure des liens peut avoir change (creation, rattachement,
        // suppression) : une simple coche n'est plus un recalcul complet (G5).
        if (recalculNumerotation) {
            val cujusId = Stockage.lireIdCujus(ctx).takeIf { id -> personnes.any { it.id == id } }
                ?: personnes.firstOrNull { it.sosa.trim() == sosaCujus.toString() }?.id
                ?: personnes.firstOrNull()?.id
                ?: 0L
            renumeroter(personnes, cujusId, sosaCujus)
        }
        completerJournal(personnesTouchees)
        // G5 : serialisation puis comparaison au cache : seuls les fichiers
        // dont le texte a change sont rechiffres et reecrits sur le disque.
        fun texteJson(serialiser: (org.json.JSONArray) -> Unit): String? {
            return try {
                val tableau = org.json.JSONArray()
                serialiser(tableau)
                tableau.toString()
            } catch (e: Exception) {
                null
            }
        }
        fun ecrireSiChange(cle: String, texte: String?, ecrire: (String) -> Boolean): Boolean {
            if (texte == null) return false
            if (derniersJson[cle] == texte) return true
            val ok = ecrire(texte)
            if (ok) derniersJson[cle] = texte
            return ok
        }
        val ok1 = ecrireSiChange(
            "personnes",
            texteJson { t -> personnes.forEach { t.put(it.versJson()) } }
        ) { Stockage.sauverTextePersonnes(ctx, it) }
        val ok2 = ecrireSiChange(
            "journal",
            texteJson { t -> pistes.forEach { t.put(it.versJson()) } }
        ) { Stockage.sauverTexteJournal(ctx, it) }
        val ok3 = ecrireSiChange(
            "actes",
            texteJson { t -> actes.forEach { t.put(it.versJson()) } }
        ) { Stockage.sauverTexteActes(ctx, it) }
        rafraichirTout()
        // G2-05 : message distinct selon le fichier en echec.
        message = when {
            ok1 && ok2 && ok3 -> ""
            !ok1 -> "\u00C9chec de l'enregistrement des personnes."
            !ok2 -> "\u00C9chec de l'enregistrement du journal."
            else -> "\u00C9chec de l'enregistrement des actes."
        }
        return ok1 && ok2 && ok3
    }

    // G2-17 : controle d'integrite des liens familiaux (chargement, import,
    // restauration). Les anomalies sont reparees par verifierLiens() et le
    // RAPPORT est consigne dans le journal de recherches (categorie
    // "controle", supprimable a la main, jamais purge automatiquement).
    // G2-18 : le compteur d'identifiants est aligne sur le maximum existant
    // (import, restauration) et les images chiffrees orphelines sont purges.
    fun verifierLiensEtRapporter() {
        val maxId = maxOf(
            personnes.maxOfOrNull { it.id } ?: 0L,
            maxOf(pistes.maxOfOrNull { it.id } ?: 0L, actes.maxOfOrNull { it.id } ?: 0L)
        )
        Stockage.alignerCompteur(ctx, maxId)
        val purgees = Stockage.supprimerImagesOrphelines(ctx, actes.map { it.id }.toHashSet())
        var rapports = verifierLiens(personnes)
        if (purgees > 0) {
            rapports = rapports + purgees.toString() + " image(s) d'acte orpheline(s) supprim\u00E9e(s)."
        }
        rapports.forEach { r ->
            val nouvelId = Stockage.prochainId(ctx)
            pistes.add(
                Piste(
                    id = nouvelId,
                    texte = r,
                    creation = System.currentTimeMillis(),
                    type = "A",
                    categorie = "controle"
                )
            )
        }
    }

    // G1-04 : restauration explicite depuis la copie .bak des fichiers
    // illisibles, puis rechargement complet. L'enregistrement redevient
    // possible seulement si tout est de nouveau lisible.
    fun restaurerDepuisBak() {
        fichiersIllisibles.forEach { nom -> Stockage.restaurerBak(ctx, nom) }
        derniersJson.clear()
        personnes.clear()
        pistes.clear()
        actes.clear()
        val rp = Stockage.charger(ctx)
        val rj = Stockage.chargerJournal(ctx)
        val ra = Stockage.chargerActes(ctx)
        personnes.addAll(rp.liste)
        pistes.addAll(rj.liste)
        actes.addAll(ra.liste)
        fichiersIllisibles = listOf(rp, rj, ra).filter { it.illisible }.map { it.nom }
        if (fichiersIllisibles.isEmpty()) {
            verifierLiensEtRapporter()
            completerJournal()
            message = "Base restaur\u00E9e depuis la copie .bak."
        } else {
            message = "Restauration impossible : aucune copie .bak lisible."
        }
        rafraichirTout()
    }

    fun supprimer(p: Personne) {
        // G3-19 : capture complete avant suppression, pour le Snackbar
        // "Annuler" (la restauration remet tout, images chiffrees comprise).
        val liensAvant = personnes.filter {
            it.pere == p.id || it.mere == p.id || it.conjoint == p.id ||
                it.conjointsSupp.contains(p.id) || it.enfants.contains(p.id)
        }.map { LienAvant(it.id, it.pere, it.mere, it.conjoint, it.conjointsSupp.toList(), it.enfants.toList()) }
        val pistesRetirees = pistes.filter { it.personneId == p.id }.toList()
        val actesASupprimer = actes.filter { it.personneId == p.id }.toList()
        val imagesAvant = mutableMapOf<Long, ByteArray>()
        actesASupprimer.forEach { a ->
            Stockage.lireImage(ctx, a.id)?.let { octets -> imagesAvant[a.id] = octets }
        }
        modele.annulation = AnnulationSuppression(
            personne = p,
            actes = actesASupprimer,
            images = imagesAvant,
            pistes = pistesRetirees,
            liens = liensAvant
        )
        personnes.remove(p)
        personnes.forEach { q ->
            if (q.pere == p.id) q.pere = 0L
            if (q.mere == p.id) q.mere = 0L
            if (q.conjoint == p.id) q.conjoint = 0L
            q.conjointsSupp.remove(p.id)
            q.enfants.remove(p.id)
        }
        // G2-19 : les pistes de la personne supprimee disparaissent avec
        // elle (aucune reference orpheline ne subsiste dans le journal).
        pistes.removeAll { it.personneId == p.id }
        actes.removeAll(actesASupprimer)
        actesASupprimer.forEach { Stockage.supprimerImage(ctx, it.id) }
        enregistrer()
        pileFiches.removeAll { it == p.id }
        if (idOuvert == p.id) idOuvert = 0L
        // G3-19 : la chaine exacte declenche le bouton "Annuler" du Snackbar.
        message = "Personne supprim\u00E9e."
    }

    // G3-19 : restaure integralement la personne supprimee si l'utilisateur
    // touche "Annuler" dans le Snackbar (meme identifiants : le compteur
    // G2-18 est strictement croissant, aucun conflit possible).
    fun annulerSuppression() {
        val a = modele.annulation ?: return
        personnes.add(a.personne)
        a.liens.forEach { l ->
            personneParId(personnes, l.id)?.let { q ->
                q.pere = l.pere
                q.mere = l.mere
                q.conjoint = l.conjoint
                q.conjointsSupp.clear()
                q.conjointsSupp.addAll(l.conjointsSupp)
                q.enfants.clear()
                q.enfants.addAll(l.enfants)
            }
        }
        actes.addAll(a.actes)
        a.images.forEach { (idActe, octets) -> Stockage.sauverImage(ctx, idActe, octets) }
        pistes.addAll(a.pistes)
        modele.annulation = null
        enregistrer()
        message = "Suppression annul\u00E9e."
    }

    fun lier(role: String, cibleId: Long) {
        val p = personneParId(personnes, idOuvert) ?: return
        val cible = personneParId(personnes, cibleId) ?: return
        when (role) {
            // Aucun numero n'est affecte a la main ici : la numerotation
            // Sosa / d'Aboville est recalculee en entier par enregistrer()
            // -> renumeroter() (sources uniques, mise a jour immediate).
            "pere" -> p.pere = cibleId
            "mere" -> p.mere = cibleId
            "conjoint" -> {
                p.conjoint = cibleId
                cible.conjoint = p.id
            }
            "conjointsupp" -> {
                if (cibleId != p.conjoint && !p.conjointsSupp.contains(cibleId)) {
                    p.conjointsSupp.add(cibleId)
                }
                if (p.id != cible.conjoint && !cible.conjointsSupp.contains(p.id)) {
                    cible.conjointsSupp.add(p.id)
                }
            }
            "enfant" -> {
                p.enfants.add(cibleId)
                if (p.sexe == "F") {
                    if (cible.mere == 0L) cible.mere = p.id
                    if (cible.pere == 0L && p.conjoint != 0L) cible.pere = p.conjoint
                } else {
                    if (cible.pere == 0L) cible.pere = p.id
                    if (cible.mere == 0L && p.conjoint != 0L) cible.mere = p.conjoint
                }
            }
        }
        enregistrer()
        pickerRole = ""
    }

    // Saisie d'un nouveau mariage depuis la fiche : cree/ouvre le bloc
    // Mariage, renseigne date et lieu, puis rattache le conjoint.
    // CONTROLE ANTI-DOUBLON : si le nom + pr\u00E9noms matchent une fiche
    // existante (insensible \u00E0 la casse), elle est synchronis\u00E9e, jamais
    // recr\u00E9\u00E9e ; si le conjoint est d\u00E9j\u00E0 li\u00E9, rien ne se duplique.
    fun ajouterMariage(nom: String, prenoms: String, date: String, commune: String, dept: String) {
        val p = personneParId(personnes, idOuvert) ?: return
        if (!p.blocsOuverts.contains("mariage")) p.blocsOuverts.add("mariage")
        if (p.dateMariage.isBlank() && date.isNotBlank()) p.dateMariage = dateGreg(date)
        if (p.lieuMariage.isBlank() && commune.isNotBlank()) p.lieuMariage = commune
        if (p.deptMariage.isBlank() && dept.isNotBlank()) p.deptMariage = dept
        val n = nom.trim()
        val pr = prenoms.trim()
        if (n.isEmpty() && pr.isEmpty()) {
            enregistrer()
            return
        }
        val existante = personnes.firstOrNull { q ->
            q.id != p.id &&
            q.nom.trim().uppercase() == n.uppercase() &&
            q.prenoms.trim().uppercase() == pr.uppercase()
        }
        var cible: Personne
        if (existante != null) {
            if (existante.id == p.conjoint || p.conjointsSupp.contains(existante.id)) {
                message = "Ce conjoint est d\u00E9j\u00E0 li\u00E9 \u00E0 " + p.titre() + " : fiche synchronis\u00E9e, aucun doublon cr\u00E9\u00E9."
                enregistrer()
                return
            }
            cible = existante
        } else {
            val nouvelle = Personne(
                id = Stockage.prochainId(ctx),
                nom = n,
                prenoms = pr,
                sexe = if (p.sexe == "M") "F" else if (p.sexe == "F") "M" else ""
            )
            personnes.add(nouvelle)
            cible = nouvelle
        }
        if (p.conjoint == 0L) {
            p.conjoint = cible.id
            cible.conjoint = p.id
        } else {
            if (!p.conjointsSupp.contains(cible.id)) p.conjointsSupp.add(cible.id)
            if (!cible.conjointsSupp.contains(p.id)) cible.conjointsSupp.add(p.id)
        }
        if (cible.dateMariage.isBlank() && p.dateMariage.isNotBlank()) cible.dateMariage = p.dateMariage
        if (cible.lieuMariage.isBlank() && p.lieuMariage.isNotBlank()) cible.lieuMariage = p.lieuMariage
        if (cible.deptMariage.isBlank() && p.deptMariage.isNotBlank()) cible.deptMariage = p.deptMariage
        enregistrer()
    }

    // Saisie d'un nouvel enfant depuis la fiche : CONTROLE ANTI-DOUBLON
    // (nom + pr\u00E9noms, insensible \u00E0 la casse) et synchronisation avec la
    // fiche existante, sinon cr\u00E9ation et rattachement p\u00E8re/m\u00E8re.
    fun ajouterEnfant(nom: String, prenoms: String, sexe: String, dateNaissance: String) {
        val p = personneParId(personnes, idOuvert) ?: return
        val n = nom.trim()
        val pr = prenoms.trim()
        if (n.isEmpty() && pr.isEmpty()) return
        val existant = personnes.firstOrNull { q ->
            q.id != p.id &&
            q.nom.trim().uppercase() == n.uppercase() &&
            q.prenoms.trim().uppercase() == pr.uppercase()
        }
        val enfant: Personne
        if (existant != null) {
            if (p.enfants.contains(existant.id)) {
                message = "Cet enfant est d\u00E9j\u00E0 li\u00E9 \u00E0 " + p.titre() + " : fiche synchronis\u00E9e, aucun doublon cr\u00E9\u00E9."
                return
            }
            enfant = existant
        } else {
            enfant = Personne(
                id = Stockage.prochainId(ctx),
                nom = n,
                prenoms = pr,
                sexe = sexe
            )
            personnes.add(enfant)
        }
        if (!p.enfants.contains(enfant.id)) p.enfants.add(enfant.id)
        if (p.sexe == "F") {
            if (enfant.mere == 0L) enfant.mere = p.id
            if (enfant.pere == 0L && p.conjoint != 0L) enfant.pere = p.conjoint
        } else {
            if (enfant.pere == 0L) enfant.pere = p.id
            if (enfant.mere == 0L && p.conjoint != 0L) enfant.mere = p.conjoint
        }
        if (enfant.dateNaissance.isBlank() && dateNaissance.isNotBlank()) enfant.dateNaissance = dateGreg(dateNaissance)
        // L'autre parent (conjoint connu) voit aussi l'enfant dans sa fiche.
        val autre = personneParId(personnes, if (p.sexe == "F") enfant.pere else enfant.mere)
        if (autre != null && autre.id != p.id && !autre.enfants.contains(enfant.id)) {
            autre.enfants.add(enfant.id)
        }
        enregistrer()
    }

    fun delier(role: String, enfantId: Long) {
        val p = personneParId(personnes, idOuvert) ?: return
        when (role) {
            "pere" -> p.pere = 0L
            "mere" -> p.mere = 0L
            "conjoint" -> {
                val c = personneParId(personnes, p.conjoint)
                p.conjoint = 0L
                if (c != null) c.conjoint = 0L
            }
            "conjointsupp" -> {
                p.conjointsSupp.remove(enfantId)
                personneParId(personnes, enfantId)?.conjointsSupp?.remove(p.id)
            }
            "enfant" -> {
                p.enfants.remove(enfantId)
                personneParId(personnes, enfantId)?.let { e ->
                    if (e.pere == p.id) e.pere = 0L
                    if (e.mere == p.id) e.mere = 0L
                }
            }
        }
        enregistrer()
    }

    // Saisie d'un parent depuis le bloc Naissance : anti-doublon strict sur
    // nom + prenoms (insensible a la casse) ; si la fiche existe, on la lie,
    // sinon on la cree. L'enfant est ajoute a la liste des enfants du parent,
    // puis tout est resynchronise (numeros, journal) par enregistrer().
    fun nouveauParent(role: String, nom: String, prenoms: String) {
        val p = personneParId(personnes, idOuvert) ?: return
        val n = nom.trim()
        val pr = prenoms.trim()
        if (n.isEmpty() && pr.isEmpty()) return
        val existante = personnes.firstOrNull { q ->
            q.nom.trim().uppercase() == n.uppercase() &&
            q.prenoms.trim().uppercase() == pr.uppercase()
        }
        var cible: Personne
        if (existante != null) {
            cible = existante
        } else {
            val nouvelle = Personne(
                id = Stockage.prochainId(ctx),
                nom = n,
                prenoms = pr,
                sexe = if (role == "pere") "M" else "F"
            )
            personnes.add(nouvelle)
            cible = nouvelle
        }
        if (cible.id == p.id) {
            message = "La fiche ne peut pas \u00EAtre son propre parent."
            enregistrer()
            return
        }
        if (role == "pere") p.pere = cible.id else p.mere = cible.id
        if (!cible.enfants.contains(p.id)) cible.enfants.add(p.id)
        enregistrer()
    }

    // Ajout d'un temoin a un bloc d'evenement : id != 0 = fiche choisie dans
    // la liste ; sinon recherche anti-doublon puis creation si necessaire.
    fun nouveauTemoin(type: String, id: Long, nom: String, prenoms: String) {
        val p = personneParId(personnes, idOuvert) ?: return
        val n = nom.trim()
        val pr = prenoms.trim()
        var cible: Personne?
        if (id != 0L) {
            cible = personneParId(personnes, id)
        } else if (n.isEmpty() && pr.isEmpty()) {
            return
        } else {
            cible = personnes.firstOrNull { q ->
                q.nom.trim().uppercase() == n.uppercase() &&
                q.prenoms.trim().uppercase() == pr.uppercase()
            }
        }
        if (cible == null) {
            val nouvelle = Personne(
                id = Stockage.prochainId(ctx),
                nom = n,
                prenoms = pr
            )
            personnes.add(nouvelle)
            cible = nouvelle
        }
        if (cible.id == p.id) {
            message = "Une personne ne peut pas \u00EAtre t\u00E9moin de son propre acte."
            enregistrer()
            return
        }
        if (!p.temoinsDe(type).contains(cible.id)) p.temoinsDe(type).add(cible.id)
        enregistrer()
    }

    // Retire un temoin (et sa coche de signature) d'un bloc d'evenement.
    fun retirerTemoin(type: String, id: Long) {
        val p = personneParId(personnes, idOuvert) ?: return
        p.temoinsDe(type).remove(id)
        p.signatairesDe(type).remove(id)
        // G5 : pas de renumerotation ni de rescan complet pour une coche.
        enregistrer(false, setOf(idOuvert))
    }

    // Coche/decoche la signature d'une personne sur un acte (0L = le pere).
    fun basculerSignature(type: String, id: Long) {
        val p = personneParId(personnes, idOuvert) ?: return
        val liste = p.signatairesDe(type)
        if (liste.contains(id)) liste.remove(id) else liste.add(id)
        // G5 : pas de renumerotation ni de rescan complet pour une coche.
        enregistrer(false, setOf(idOuvert))
    }

    // Modification d'un bloc d'evenement depuis son dialogue d'edition :
    // date, heure, commune, departement et reference de la cote AD.
    // Tout est resynchronise (numeros, coherence, journal) par enregistrer().
    fun majEvenement(type: String, date: String, heure: String, commune: String, dept: String, ref: String) {
        val p = personneParId(personnes, idOuvert) ?: return
        when (type) {
            "N" -> { p.dateNaissance = dateGreg(date); p.heureNaissance = heure; p.lieuNaissance = commune; p.deptNaissance = dept }
            "B" -> { p.dateBapteme = dateGreg(date); p.heureBapteme = heure; p.lieuBapteme = commune; p.deptBapteme = dept }
            "M" -> { p.dateMariage = dateGreg(date); p.heureMariage = heure; p.lieuMariage = commune; p.deptMariage = dept }
            "D" -> { p.dateDeces = dateGreg(date); p.heureDeces = heure; p.lieuDeces = commune; p.deptDeces = dept }
            "S" -> { p.dateSepulture = dateGreg(date); p.heureSepulture = heure; p.lieuSepulture = commune; p.deptSepulture = dept }
        }
        // Date saisie dans le bloc : la coche de l'evenement passe au vert
        // directement (plus besoin de cocher a la main).
        if (date.isNotBlank()) p.changerEtat(sectionDe(type), "vert")
        if (ref.isNotBlank()) {
            val acte = actes.filter { it.personneId == p.id && it.type == type }
                .maxByOrNull { it.creation }
            if (acte != null) acte.reference = ref
        }
        // G5 : seul le bloc d'evenement a change : numerotation et journal
        // limites a cette personne.
        enregistrer(false, setOf(p.id))
    }

    fun supprimerPiste(piste: Piste) {
        if (piste.categorie.isNotEmpty()) {
            val cle = piste.personneId.toString() + "|" + piste.categorie
            if (!categoriesIgnorees.contains(cle)) categoriesIgnorees.add(cle)
        }
        pistes.remove(piste)
        // G5 : seule la liste des pistes a change.
        enregistrer(false)
    }

    // Coche de validation d'une information (identite ou acte) : une coche
    // NEGATIVE cree automatiquement une recherche dans le journal ; une coche
    // positive l'archive.
    fun validerInfo(p: Personne, section: String, valide: Boolean) {
        p.changerEtat(section, if (valide) "vert" else "rouge")
        val cle = "verif_" + section
        val maintenant = System.currentTimeMillis()
        if (!valide) {
            val existe = pistes.any { !it.faite && it.personneId == p.id && it.categorie == cle }
            if (!existe) {
                pistes.add(
                    Piste(
                        id = Stockage.prochainId(ctx),
                        texte = "V\u00E9rifier " + libelleSection(section) + " " + titreCourt(p),
                        personneId = p.id,
                        creation = maintenant,
                        type = "P",
                        categorie = cle
                    )
                )
            }
        } else {
            // Coche positive : la recherche de verification correspondante est
            // supprimee (plus d'archivage, plus d'historique) ; G1-13 : sa note
            // eventuelle est conservee dans les notes libres de la personne.
            pistes.filter { !it.faite && it.personneId == p.id && it.categorie == cle }.forEach { conserverNote(it) }
            pistes.removeAll { !it.faite && it.personneId == p.id && it.categorie == cle }
        }
        // G5 : numerotation et journal limites a cette personne.
        enregistrer(false, setOf(p.id))
    }

    // Definit le de cujus et son n Sosa (pas forcement 1) : toute la numerotation
    // Sosa et d'Aboville en decoule automatiquement.
    fun definirCujus(p: Personne, sosa: Long) {
        val s = if (sosa >= 1L) sosa else 1L
        renumeroter(personnes, p.id, s)
        sosaCujus = s
        Stockage.definirCujus(ctx, p.id, s)
        arbreCentre = p.id
        // Retour visible : la numerotation a ete recalculee et enregistree.
        if (enregistrer()) {
            message = "De cujus : " + p.titre() + " (Sosa " + s.toString() + "). Num\u00E9rotation recalcul\u00E9e et enregistr\u00E9e."
        }
    }

    // Identifiant du de cujus : memorise en preferences, sinon par n Sosa, sinon 1re personne.
    fun idDuCujus(): Long {
        val idMemorise = Stockage.lireIdCujus(ctx)
        if (personnes.any { it.id == idMemorise }) return idMemorise
        return personnes.firstOrNull { it.sosa.trim() == sosaCujus.toString() }?.id
            ?: personnes.firstOrNull()?.id
            ?: 0L
    }

    fun exporterGedcom(uri: Uri, sousEnsemble: Set<Long>?, exclureVivants: Boolean = false) {
        // G2-06 : export HORS du thread principal, indicateur a l'ecran.
        occupation = "Export GEDCOM en cours..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                // G1-16 : option exclure les personnes vivantes (sans deces
                // connu, nees il y a moins de 100 ans).
                val annee = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val exportables = if (exclureVivants) personnes.filter { !estVivante(it, annee) } else personnes
                val sous = if (exclureVivants && sousEnsemble != null) sousEnsemble.filter { id -> exportables.any { it.id == id } }.toSet() else sousEnsemble
                val texte = construireGedcom(exportables, sous, actes)
                ctx.contentResolver.openOutputStream(uri)?.use { sortie ->
                    sortie.write(texte.toByteArray(Charsets.UTF_8))
                }
                val n = if (sous == null) exportables.size else texte.split("\n").count { it.endsWith(" INDI") }
                // R1 : etat Compose JAMAIS modifie depuis le thread IO.
                withContext(Dispatchers.Main) {
                    message = "GEDCOM export\u00E9 (" + n.toString() + " personnes) : importable dans Heredis." + (if (exclureVivants) " Personnes vivantes exclues." else "")
                }
            } catch (e: Exception) {
                // G2-05 : message distinct selon le type d'echec.
                withContext(Dispatchers.Main) { message = messageErreur(e, "\u00C9chec de l'export GEDCOM.") }
            } finally {
                withContext(Dispatchers.Main) { occupation = "" }
            }
        }
    }

    fun lireGedcom(uri: Uri) {
        // G2-06 : lecture et analyse HORS du thread principal.
        occupation = "Import GEDCOM en cours..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                // G1-17 : taille limite (10 Mo) et lecture par flux borne,
                // jamais de readText() sans limite sur un fichier arbitraire.
                val taille = try {
                    ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                } catch (e: Exception) { -1L }
                if (taille > 10_000_000L) {
                    withContext(Dispatchers.Main) { message = "Fichier trop volumineux (plus de 10 Mo) : import refus\u00E9." }
                    return@launch
                }
                // G2-04 : lecture par octets born\u00E9e puis d\u00E9tection de l encodage
                // (BOM UTF-16 LE/BE, UTF-8 strict, repli ANSI) avant l analyse.
                val octets = ctx.contentResolver.openInputStream(uri)?.use { entree ->
                    val tampon = ByteArrayOutputStream()
                    val morceau = ByteArray(64 * 1024)
                    while (true) {
                        val n = entree.read(morceau)
                        if (n <= 0) break
                        tampon.write(morceau, 0, n)
                        if (tampon.size() > 10_000_000) return@use null
                    }
                    tampon.toByteArray()
                }
                if (octets == null || octets.isEmpty()) {
                    withContext(Dispatchers.Main) { message = "Fichier GEDCOM illisible (ou trop volumineux)." }
                    return@launch
                }
                val resultat = importerGedcomRapporte(decoderGedcom(octets))
                if (resultat.personnes.isEmpty()) {
                    withContext(Dispatchers.Main) { message = "Aucune personne trouv\u00E9e dans ce GEDCOM." }
                    return@launch
                }
                // R1 : etat Compose modifie uniquement sur le thread principal.
                withContext(Dispatchers.Main) {
                    importConfirme = resultat.personnes
                    importRapport = resultat.ignores
                }
            } catch (e: Exception) {
                // G2-05 : message distinct selon le type d'echec.
                withContext(Dispatchers.Main) { message = messageErreur(e, "\u00C9chec de la lecture du GEDCOM.") }
            } finally {
                withContext(Dispatchers.Main) { occupation = "" }
            }
        }
    }

    fun remplacerBase(importees: List<Personne>) {
        // G1-09 : sauvegarde automatique de la base avant remplacement.
        Stockage.sauvegardeAvantImport(ctx)
        personnes.clear()
        personnes.addAll(importees)
        pistes.clear()
        pileFiches.clear()
        // R2 : les images de l'ancienne base ne sont supprimees qu'apres un
        // enregistrement reussi de la nouvelle.
        val idsActes = actes.map { it.id }
        actes.clear()
        // De cujus apres import : la personne au plus petit n Sosa du fichier, sinon la 1re.
        val avecSosa = personnes.filter { it.sosa.trim().toLongOrNull() != null }
        val premier = avecSosa.minByOrNull { it.sosa.trim().toLong() }
            ?: personnes.firstOrNull()
        if (premier != null) {
            if (premier.sosa.isBlank()) {
                renumeroter(personnes, premier.id, 1L)
            }
            sosaCujus = premier.sosa.trim().toLongOrNull() ?: 1L
            Stockage.definirCujus(ctx, premier.id, sosaCujus)
        }
        arbreCentre = premier?.id ?: 0L
        // G2-17 : liens de l'import verifies, rapports consignes au journal.
        verifierLiensEtRapporter()
        derniersJson.clear()
        if (enregistrer()) idsActes.forEach { Stockage.supprimerImage(ctx, it) }
        message = importees.size.toString() + " personnes import\u00E9es."
    }

    // Vide entierement la base (personnes, journal, actes et images), apres confirmation.
    fun viderBase() {
        // R2 : les images chiffrees ne sont supprimees qu'apres un
        // enregistrement reussi de la base vide.
        val idsActes = actes.map { it.id }
        personnes.clear()
        pistes.clear()
        actes.clear()
        idOuvert = 0L
        pileFiches.clear()
        arbreCentre = 0L
        Stockage.definirCujus(ctx, 0L, 1L)
        sosaCujus = 1L
        derniersJson.clear()
        if (enregistrer()) idsActes.forEach { Stockage.supprimerImage(ctx, it) }
        message = "Base vid\u00E9e."
    }

    // G1-08 : sauvegarde chiffree exportable (.gpbak) protegee par phrase secrete.
    fun sauvegarderBase(uri: Uri, phrase: String) {
        // G2-06 : chiffrement PBKDF2 HORS du thread principal.
        occupation = "Sauvegarde .gpbak en cours..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                if (Sauvegarde.exporter(ctx, uri, phrase, personnes, pistes, actes)) {
                    withContext(Dispatchers.Main) { message = "Sauvegarde .gpbak cr\u00E9\u00E9e : conservez-la et notez la phrase secr\u00E8te." }
                } else {
                    withContext(Dispatchers.Main) { message = "\u00C9chec de la sauvegarde .gpbak (v\u00E9rifiez le fichier et la phrase)." }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { message = messageErreur(e, "\u00C9chec de la sauvegarde .gpbak.") }
            } finally {
                withContext(Dispatchers.Main) { occupation = "" }
            }
        }
    }

    // G1-08 : restauration d'une sauvegarde .gpbak (phrase secrete exige).
    fun restaurerSauvegardeBase(uri: Uri, phrase: String) {
        // G2-06 : dechiffrement HORS du thread principal.
        occupation = "Restauration .gpbak en cours..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                val contenu = Sauvegarde.importer(ctx, uri, phrase)
                if (contenu == null) {
                    withContext(Dispatchers.Main) { message = "Sauvegarde illisible : v\u00E9rifiez le fichier et la phrase secr\u00E8te." }
                    return@launch
                }
                // R1 : toute mutation d'etat Compose et toute sauvegarde se font
                // sur le thread principal ; R2 : les anciennes images chiffrees
                // ne sont supprimees qu'APRES un enregistrement reussi.
                withContext(Dispatchers.Main) {
                    val idsAvant = actes.map { it.id }.toSet()
                    personnes.clear()
                    personnes.addAll(contenu.personnes)
                    pistes.clear()
                    pistes.addAll(contenu.pistes)
                    actes.clear()
                    actes.addAll(contenu.actes)
                    val ecrits = mutableSetOf<Long>()
                    contenu.actes.forEach { a ->
                        contenu.images[a.id]?.let { octets ->
                            if (Stockage.sauverImage(ctx, a.id, octets)) ecrits.add(a.id)
                        }
                    }
                    pileFiches.clear()
                    idOuvert = 0L
                    derniersJson.clear()
                    val premier = personnes.filter { it.sosa.trim().toLongOrNull() != null }.minByOrNull { it.sosa.trim().toLong() }
                    if (premier != null) {
                        sosaCujus = premier.sosa.trim().toLongOrNull() ?: 1L
                        Stockage.definirCujus(ctx, premier.id, sosaCujus)
                        arbreCentre = premier.id
                    }
                    verifierLiensEtRapporter()
                    if (enregistrer()) {
                        // R2 : suppression des anciennes images seulement apres
                        // un enregistrement reussi du nouvel etat.
                        idsAvant.filter { it !in ecrits }.forEach { Stockage.supprimerImage(ctx, it) }
                        message = "Sauvegarde restaur\u00E9e : " + contenu.personnes.size.toString() + " personnes."
                    } else {
                        // Echec d'ecriture : on ne detruit rien.
                        message = "\u00C9chec de l'enregistrement apr\u00E8s restauration : rien n'a \u00E9t\u00E9 supprim\u00E9."
                    }
                }
            } catch (e: Exception) {
                // G2-05 : message distinct selon le type d'echec.
                withContext(Dispatchers.Main) { message = messageErreur(e, "\u00C9chec de la restauration .gpbak.") }
            } finally {
                withContext(Dispatchers.Main) { occupation = "" }
            }
        }
    }

    // G1-09 : restauration de la base sauvegardee automatiquement avant un import GEDCOM.
    fun restaurerSauvegardeImport() {
        if (!Stockage.aSauvegardeImport(ctx)) {
            message = "Aucune sauvegarde d'import disponible."
            return
        }
        if (!Stockage.restaurerSauvegardeImport(ctx)) {
            message = "\u00C9chec de la restauration."
            return
        }
        derniersJson.clear()
        personnes.clear()
        pistes.clear()
        actes.clear()
        val rp = Stockage.charger(ctx)
        val rj = Stockage.chargerJournal(ctx)
        val ra = Stockage.chargerActes(ctx)
        personnes.addAll(rp.liste)
        pistes.addAll(rj.liste)
        actes.addAll(ra.liste)
        fichiersIllisibles = listOf(rp, rj, ra).filter { it.illisible }.map { it.nom }
        pileFiches.clear()
        idOuvert = 0L
        verifierLiensEtRapporter()
        message = "Base d'avant import restaur\u00E9e."
        rafraichirTout()
    }

    // ------------------------------------------------------------ Saisie par acte
    // Transcription d'un acte N/B/M/D/S : alimente la fiche Personne (dates, lieux,
    // professions, liens pere/mere/conjoint), cree les personnes absentes, et la
    // numerotation est recalculee automatiquement par enregistrer().
    // Les temoins et signataires sont memorises dans le contenu de l'acte.

    // Un nom correspond-il deja a une fiche existante (nom + prenoms, casse ignoree) ?
    fun nomDejaConnu(nomSaisi: String, prenomsSaisis: String): Boolean {
        val n = nomSaisi.trim()
        val pr = prenomsSaisis.trim()
        if (n.isEmpty() && pr.isEmpty()) return true
        return personnes.any {
            it.nom.trim().uppercase() == n.uppercase() &&
            it.prenoms.trim().uppercase() == pr.uppercase()
        }
    }

    // Noms saisis qui ne correspondent a aucune fiche : creation a proposer.
    fun nomsNouveaux(a: ActeSaisie): List<String> {
        val res = mutableListOf<String>()
        if (a.personneId == 0L && !nomDejaConnu(a.nom, a.prenoms) && (a.nom.isNotBlank() || a.prenoms.isNotBlank())) {
            res.add((a.prenoms.trim() + " " + a.nom.trim()).trim())
        }
        if (!nomDejaConnu(a.pereNom, a.perePrenoms)) {
            res.add("p\u00E8re : " + (a.perePrenoms.trim() + " " + a.pereNom.trim()).trim())
        }
        if (!nomDejaConnu(a.mereNom, a.merePrenoms)) {
            res.add("m\u00E8re : " + (a.merePrenoms.trim() + " " + a.mereNom.trim()).trim())
        }
        if (a.type == "M") {
            if (a.marieeId == 0L && !nomDejaConnu(a.marieeNom, a.marieePrenoms) && (a.marieeNom.isNotBlank() || a.marieePrenoms.isNotBlank())) {
                res.add("mari\u00E9e : " + (a.marieePrenoms.trim() + " " + a.marieeNom.trim()).trim())
            }
            if (!nomDejaConnu(a.pMarieeNom, a.pMarieePrenoms)) {
                res.add("p\u00E8re de la mari\u00E9e : " + (a.pMarieePrenoms.trim() + " " + a.pMarieeNom.trim()).trim())
            }
            if (!nomDejaConnu(a.mMarieeNom, a.mMarieePrenoms)) {
                res.add("m\u00E8re de la mari\u00E9e : " + (a.mMarieePrenoms.trim() + " " + a.mMarieeNom.trim()).trim())
            }
        }
        return res
    }

    // Synchronisation avec les fiches existantes, ou proposition de creation.
    fun enregistrerActe(a: ActeSaisie, confirme: Boolean = false) {
        val nouveaux = nomsNouveaux(a)
        if (nouveaux.isNotEmpty() && !confirme) {
            acteAConfirmer = a
            return
        }
        acteAConfirmer = null
        var crees = 0

        // Trouve une personne existante (par id ou nom+premoms), sinon la cree.
        fun trouverOuCreer(id: Long, nomSaisi: String, prenomsSaisis: String, sexeDefaut: String): Personne? {
            personneParId(personnes, id)?.let { return it }
            val n = nomSaisi.trim()
            val pr = prenomsSaisis.trim()
            if (n.isEmpty() && pr.isEmpty()) return null
            personnes.firstOrNull {
                it.nom.trim().uppercase() == n.uppercase() &&
                it.prenoms.trim().uppercase() == pr.uppercase()
            }?.let { return it }
            val np = Personne(
                id = Stockage.prochainId(ctx),
                nom = n, prenoms = pr, sexe = if (sexeDefaut.isNotEmpty()) sexeDefaut else ""
            )
            personnes.add(np)
            crees += 1
            return np
        }

        // Renseigne un champ seulement s'il est encore vide (l'acte complete la fiche).
        fun renseigner(c: Personne?, ne: String, lieu: String, dept: String, prof: String, dec: String, lieuDec: String) {
            if (c == null) return
            if (c.dateNaissance.isBlank() && ne.isNotBlank()) c.dateNaissance = dateGreg(ne)
            if (c.lieuNaissance.isBlank() && lieu.isNotBlank()) c.lieuNaissance = lieu.trim()
            if (c.deptNaissance.isBlank() && dept.isNotBlank()) c.deptNaissance = dept.trim()
            if (c.profession.isBlank() && prof.isNotBlank()) c.profession = prof.trim()
            if (c.dateDeces.isBlank() && dec.isNotBlank()) c.dateDeces = dateGreg(dec)
            if (c.lieuDeces.isBlank() && lieuDec.isNotBlank()) c.lieuDeces = lieuDec.trim()
        }

        fun lierParent(enfant: Personne, parent: Personne?, estMere: Boolean) {
            if (parent == null) return
            if (estMere) {
                if (enfant.mere == 0L) enfant.mere = parent.id
            } else {
                if (enfant.pere == 0L) enfant.pere = parent.id
            }
            if (!parent.enfants.contains(enfant.id)) parent.enfants.add(enfant.id)
        }

        val p = trouverOuCreer(a.personneId, a.nom, a.prenoms, a.sexe)
        if (p == null) {
            message = "Acte non enregistr\u00E9 : personne concern\u00E9e non identifi\u00E9e."
            return
        }

        // Date, heure et lieu de l'acte selon son type (fiche de la personne concernee).
        when (a.type) {
            "N" -> {
                if (p.dateNaissance.isBlank()) p.dateNaissance = dateGreg(a.date)
                if (p.heureNaissance.isBlank()) p.heureNaissance = a.heure
                if (p.lieuNaissance.isBlank()) p.lieuNaissance = a.commune
                if (p.deptNaissance.isBlank()) p.deptNaissance = a.dept
            }
            "B" -> {
                if (p.dateBapteme.isBlank()) p.dateBapteme = dateGreg(a.date)
                if (p.heureBapteme.isBlank()) p.heureBapteme = a.heure
                if (p.lieuBapteme.isBlank()) p.lieuBapteme = a.commune
                if (p.deptBapteme.isBlank()) p.deptBapteme = a.dept
            }
            "M" -> {
                if (p.dateMariage.isBlank()) p.dateMariage = dateGreg(a.date)
                if (p.heureMariage.isBlank()) p.heureMariage = a.heure
                if (p.lieuMariage.isBlank()) p.lieuMariage = a.commune
                if (p.deptMariage.isBlank()) p.deptMariage = a.dept
            }
            "D" -> {
                if (p.dateDeces.isBlank()) p.dateDeces = dateGreg(a.date)
                if (p.heureDeces.isBlank()) p.heureDeces = a.heure
                if (p.lieuDeces.isBlank()) p.lieuDeces = a.commune
                if (p.deptDeces.isBlank()) p.deptDeces = a.dept
            }
            "S" -> {
                if (p.dateSepulture.isBlank()) p.dateSepulture = dateGreg(a.date)
                if (p.heureSepulture.isBlank()) p.heureSepulture = a.heure
                if (p.lieuSepulture.isBlank()) p.lieuSepulture = a.commune
                if (p.deptSepulture.isBlank()) p.deptSepulture = a.dept
            }
        }
        if (p.profession.isBlank() && a.profession.isNotBlank()) p.profession = a.profession

        // Pere et mere de la personne concernee.
        val pere = trouverOuCreer(0L, a.pereNom, a.perePrenoms, "M")
        renseigner(pere, a.pereNe, a.pereLieu, a.pereDept, a.pereProf, a.pereDeces, a.pereLieuDeces)
        lierParent(p, pere, false)
        val mere = trouverOuCreer(0L, a.mereNom, a.merePrenoms, "F")
        renseigner(mere, a.mereNe, a.mereLieu, a.mereDept, a.mereProf, a.mereDeces, a.mereLieuDeces)
        lierParent(p, mere, true)

        // Mariee et ses parents (mariage uniquement).
        var mariee: Personne? = null
        if (a.type == "M") {
            mariee = trouverOuCreer(a.marieeId, a.marieeNom, a.marieePrenoms, "F")
            if (mariee != null) {
                if (mariee.dateMariage.isBlank()) mariee.dateMariage = dateGreg(a.date)
                if (mariee.heureMariage.isBlank()) mariee.heureMariage = a.heure
                if (mariee.lieuMariage.isBlank()) mariee.lieuMariage = a.commune
                if (mariee.deptMariage.isBlank()) mariee.deptMariage = a.dept
                if (mariee.profession.isBlank() && a.marieeProf.isNotBlank()) mariee.profession = a.marieeProf
                if (p.conjoint == 0L) p.conjoint = mariee.id
                if (mariee.conjoint == 0L) mariee.conjoint = p.id
                val pMar = trouverOuCreer(0L, a.pMarieeNom, a.pMarieePrenoms, "M")
                renseigner(pMar, a.pMarieeNe, a.pMarieeLieu, a.pMarieeDept, a.pMarieeProf, a.pMarieeDeces, a.pMarieeLieuDeces)
                lierParent(mariee, pMar, false)
                val mMar = trouverOuCreer(0L, a.mMarieeNom, a.mMarieePrenoms, "F")
                renseigner(mMar, a.mMarieeNe, a.mMarieeLieu, a.mMarieeDept, a.mMarieeProf, a.mMarieeDeces, a.mMarieeLieuDeces)
                lierParent(mariee, mMar, true)
            }
        }

        // Contenu memorise dans l'acte : description, ages, temoins, signataires.
        fun descriptionParent(nomComplet: String, ne: String, lieu: String, dept: String, prof: String, dec: String, lieuDec: String): String {
            if (nomComplet.isBlank()) return ""
            val m = mutableListOf<String>()
            m.add(nomComplet.trim())
            if (ne.isNotBlank()) {
                val age = ageActe(a.date, ne)
                m.add("n\u00E9(e) le " + dateAffichee(ne) + (if (age.isNotEmpty()) " (" + age + " \u00E0 l'acte)" else ""))
            }
            if (lieu.isNotBlank()) m.add("\u00E0 " + lieu.trim() + (if (dept.isNotBlank()) " (" + dept.trim() + ")" else ""))
            if (prof.isNotBlank()) m.add(prof.trim())
            if (dec.isNotBlank()) m.add("d\u00E9c\u00E9d\u00E9(e) le " + dateAffichee(dec) + (if (lieuDec.isNotBlank()) " \u00E0 " + lieuDec.trim() else ""))
            return m.joinToString(", ")
        }

        val lignes = mutableListOf<String>()
        var entete = libelleType(a.type) + " de " + p.titre()
        if (a.date.isNotBlank()) entete += " le " + dateAffichee(a.date) + (if (a.heure.isNotBlank()) " \u00E0 " + heureAffichee(a.heure) else "")
        if (a.commune.isNotBlank()) entete += ", \u00E0 " + a.commune + (if (a.dept.isNotBlank()) " (" + a.dept + ")" else "")
        lignes.add(entete)
        if (a.profession.isNotBlank()) lignes.add("Profession : " + a.profession)
        descriptionParent((a.pereNom + " " + a.perePrenoms).trim(), a.pereNe, a.pereLieu, a.pereDept, a.pereProf, a.pereDeces, a.pereLieuDeces)
            .takeIf { it.isNotEmpty() }?.let { lignes.add("P\u00E8re : " + it) }
        descriptionParent((a.mereNom + " " + a.merePrenoms).trim(), a.mereNe, a.mereLieu, a.mereDept, a.mereProf, a.mereDeces, a.mereLieuDeces)
            .takeIf { it.isNotEmpty() }?.let { lignes.add("M\u00E8re : " + it) }
        if (mariee != null) {
            lignes.add("Mari\u00E9e : " + mariee.titre() + (if (a.marieeProf.isNotBlank()) ", " + a.marieeProf else ""))
            descriptionParent((a.pMarieeNom + " " + a.pMarieePrenoms).trim(), a.pMarieeNe, a.pMarieeLieu, a.pMarieeDept, a.pMarieeProf, a.pMarieeDeces, a.pMarieeLieuDeces)
                .takeIf { it.isNotEmpty() }?.let { lignes.add("P\u00E8re de la mari\u00E9e : " + it) }
            descriptionParent((a.mMarieeNom + " " + a.mMarieePrenoms).trim(), a.mMarieeNe, a.mMarieeLieu, a.mMarieeDept, a.mMarieeProf, a.mMarieeDeces, a.mMarieeLieuDeces)
                .takeIf { it.isNotEmpty() }?.let { lignes.add("M\u00E8re de la mari\u00E9e : " + it) }
        }
        val temoinsTexte = a.temoins.filter { it.nom.isNotBlank() }.joinToString(" ; ") { t ->
            (t.prenoms.trim() + " " + t.nom.trim()).trim() + (if (t.profession.isNotBlank()) " (" + t.profession.trim() + ")" else "")
        }
        if (temoinsTexte.isNotEmpty()) lignes.add("T\u00E9moins : " + temoinsTexte)
        val signatairesNoms = a.signataires.mapNotNull { l ->
            when {
                l == "Concerne(e)" -> p.titre()
                l == "P\u00E8re" -> (a.pereNom + " " + a.perePrenoms).trim().ifEmpty { pere?.titre() }
                l == "M\u00E8re" -> (a.mereNom + " " + a.merePrenoms).trim().ifEmpty { mere?.titre() }
                l == "Mari\u00E9e" -> (a.marieeNom + " " + a.marieePrenoms).trim().ifEmpty { mariee?.titre() }
                l.startsWith("T\u00E9moin ") -> {
                    val i = l.removePrefix("T\u00E9moin ").toIntOrNull() ?: return@mapNotNull null
                    val t = a.temoins.getOrNull(i - 1) ?: return@mapNotNull null
                    (t.prenoms.trim() + " " + t.nom.trim()).trim()
                }
                else -> null
            }
        }.filter { it.isNotBlank() }
        if (signatairesNoms.isNotEmpty()) lignes.add("Signataires : " + signatairesNoms.joinToString(", "))
        val contenu = lignes.joinToString("\n")

        // Acte rattache a la personne concernee (et a la mariee pour un mariage).
        var idActe = Stockage.prochainId(ctx)
        actes.add(
            Acte(
                id = idActe, personneId = p.id, type = a.type, titre = entete,
                creation = System.currentTimeMillis(), contenu = contenu,
                reference = a.reference.trim()
            )
        )
        if (mariee != null) {
            idActe = Stockage.prochainId(ctx)
            actes.add(
                Acte(
                    id = idActe, personneId = mariee.id, type = a.type, titre = entete,
                    creation = System.currentTimeMillis(), contenu = contenu,
                    reference = a.reference.trim()
                )
            )
        }

        enregistrer()
        message = "Acte enregistr\u00E9 : " + libelleType(a.type) +
            (if (crees > 0) " ; " + crees.toString() + " personne(s) cr\u00E9\u00E9e(s)" else "")
    }

    fun ajouterActe(personneId: Long, uri: Uri, type: String, titre: String) {
        // G2-06 : compression d'une grande image HORS du thread principal.
        occupation = "Traitement de l'image..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                val jpg = compresserVersJpeg(ctx, uri)
                if (jpg == null) {
                    withContext(Dispatchers.Main) { message = "Impossible de lire cette image." }
                    return@launch
                }
                val nouvelId = Stockage.prochainId(ctx)
                if (Stockage.sauverImage(ctx, nouvelId, jpg)) {
                    // R1 : mutation d'etat Compose et enregistrement sur le
                    // thread principal ; G5 : journal limite a cette personne.
                    withContext(Dispatchers.Main) {
                        actes.add(Acte(nouvelId, personneId, type, titre, System.currentTimeMillis()))
                        enregistrer(false, setOf(personneId))
                    }
                } else {
                    withContext(Dispatchers.Main) { message = "\u00C9chec de l'enregistrement de l'image." }
                }
            } catch (e: Exception) {
                // G2-05 : message distinct selon le type d'echec.
                withContext(Dispatchers.Main) { message = messageErreur(e, "\u00C9chec de l'enregistrement de l'image.") }
            } finally {
                withContext(Dispatchers.Main) { occupation = "" }
            }
        }
    }

    // Chargement uniquement apres deverrouillage (rien ne s'affiche sans PIN).
    // G1-04/G1-05/G1-06 : lecture tout-ou-rien avec repli sur la copie .bak ;
    // tout fichier illisible BLOQUE l'enregistrement (rien n'est ecrase).
    LaunchedEffect(verrou.deverrouille) {
        if (verrou.deverrouille && !charge) {
            // G2-06 : dechiffrement HORS du thread principal.
            occupation = "Chargement des donn\u00E9es..."
            val charges = withContext(Dispatchers.IO) {
                Triple(Stockage.charger(ctx), Stockage.chargerJournal(ctx), Stockage.chargerActes(ctx))
            }
            val rp = charges.first
            val rj = charges.second
            val ra = charges.third
            personnes.addAll(rp.liste)
            pistes.addAll(rj.liste)
            actes.addAll(ra.liste)
            sosaCujus = Stockage.lireSosaCujus(ctx)
            fichiersIllisibles = listOf(rp, rj, ra).filter { it.illisible }.map { it.nom }
            charge = true
            occupation = ""
            if (fichiersIllisibles.isEmpty()) {
                // G5 : le cache part du contenu exact du disque : le premier
                // enregistrement ne reecrit que ce qui a vraiment change.
                try {
                    val tp = org.json.JSONArray(); personnes.forEach { tp.put(it.versJson()) }
                    val tj = org.json.JSONArray(); pistes.forEach { tj.put(it.versJson()) }
                    val ta = org.json.JSONArray(); actes.forEach { ta.put(it.versJson()) }
                    derniersJson["personnes"] = tp.toString()
                    derniersJson["journal"] = tj.toString()
                    derniersJson["actes"] = ta.toString()
                } catch (e: Exception) {
                    derniersJson.clear()
                }
                verifierLiensEtRapporter()
                completerJournal()
            }
        }
        // G1-14 : au verrouillage, PLUS AUCUNE donnee ne reste en memoire
        // (personnes, journal, actes, fiche ouverte, pile de navigation).
        if (!verrou.deverrouille && charge) {
            personnes.clear()
            pistes.clear()
            actes.clear()
            pileFiches.clear()
            idOuvert = 0L
            charge = false
        }
    }
    LaunchedEffect(Unit) { aPin = Stockage.aPin(ctx) }

    // Ouverture sur le de cujus des le deverrouillage
    LaunchedEffect(charge, verrou.deverrouille) {
        if (charge && verrou.deverrouille && !cujasOuvert) {
            cujasOuvert = true
            idDuCujus().takeIf { it != 0L }?.let { idOuvert = it }
        }
    }

    // ------------------------------------------------------------ Porte PIN

    if (!aPin) {
        DialogueCreationPin(onCree = { pin ->
            if (Stockage.definirPin(ctx, pin)) {
                aPin = true
                verrou.deverrouille = true
            } else {
                message = "Echec de la cr\u00E9ation du PIN."
            }
        })
    } else if (!verrou.deverrouille) {
        EcranPin(
            ctx = ctx,
            onDeverrouille = {
                Stockage.reinitialiserEchecs(ctx)
                verrou.deverrouille = true
            }
        )
    } else {
        Box {
        EcranApplication(
            etat = EtatApplication(
                personnes = personnes,
                cujus = personneParId(personnes, idDuCujus()),
                pistes = pistes,
                actes = actes,
                charge = charge,
                message = message,
                idOuvert = idOuvert,
                onglet = onglet,
                editionOuverte = editionOuverte,
                editionCible = editionCible,
                pickerRole = pickerRole,
                confirmSuppression = confirmSuppression,
                arbreCentre = if (arbreCentre != 0L) arbreCentre else idDuCujus(),
                // G3-17 : largeur etendue (tablette/paysage).
                deuxColonnes = classeTaille.widthSizeClass != androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact,
                // G3-19 : une suppression peut etre annulee.
                annulationPossible = modele.annulation != null
            ),
            actions = ActionsApplication(
                onMessage = { message = it },
                onIdOuvert = { id ->
                    // Empile la fiche courante avant d'en ouvrir une autre
                    // (retour arriere = depilement).
                    if (id != 0L && idOuvert != 0L) pileFiches.add(idOuvert)
                    idOuvert = id
                },
                onRetourFiche = {
                    idOuvert = if (pileFiches.isNotEmpty()) pileFiches.removeAt(pileFiches.size - 1) else 0L
                },
                onOnglet = { onglet = it },
                onEditionOuverte = { editionOuverte = it },
                onEditionCible = { editionCible = it },
                onPickerRole = { pickerRole = it },
                onConfirmSuppression = { confirmSuppression = it },
                enregistrer = { enregistrer() },
                supprimer = { supprimer(it) },
                lier = { role, id -> lier(role, id) },
                delier = { role, id -> delier(role, id) },
                ajouterMariage = { nom, prenoms, date, commune, dept ->
                    ajouterMariage(nom, prenoms, date, commune, dept)
                },
                nouveauParent = { role, nom, prenoms -> nouveauParent(role, nom, prenoms) },
                nouveauTemoin = { type, id, nom, prenoms -> nouveauTemoin(type, id, nom, prenoms) },
                retirerTemoin = { type, id -> retirerTemoin(type, id) },
                basculerSignature = { type, id -> basculerSignature(type, id) },
                majEvenement = { type, d, h, co, de, rf -> majEvenement(type, d, h, co, de, rf) },
                ajouterEnfant = { nom, prenoms, sexe, date ->
                    ajouterEnfant(nom, prenoms, sexe, date)
                },
                definirCujus = { cujusPreselec = it; cujusDialogue = true },
                choisirCujus = { cujusPreselec = null; cujusDialogue = true },
                renumeroter = { renumeroter(personnes, it, sosaCujus) },
                supprimerPiste = { supprimerPiste(it) },
                ajouterActe = { personneId, uri, type, titre -> ajouterActe(personneId, uri, type, titre) },
                onSupprimerActe = {
                    // R2 : l'image chiffree n'est supprimee qu'apres un
                    // enregistrement reussi (sinon perte irreversible).
                    actes.remove(it)
                    if (enregistrer(false, setOf(it.personneId))) {
                        Stockage.supprimerImage(ctx, it.id)
                    }
                },
                onVoirActe = { acteVisionne = it },
                onArbreCentre = { arbreCentre = it },
                exporterGedcom = { uri, sel, ev -> exporterGedcom(uri, sel, ev) },
                importerGedcom = { uri -> lireGedcom(uri) },
                viderBase = { viderBase() },
                enregistrerActe = { a -> enregistrerActe(a) },
                ouvrirSaisieActe = { pers, t ->
                    modele.saisieFicheId = pers.id; modele.saisieType = t
                },
                fermerSaisieActe = {
                    modele.saisieFicheId = 0L; modele.saisieType = ""
                },
                sauvegarderBase = { uri, ph -> sauvegarderBase(uri, ph) },
                restaurerSauvegarde = { uri, ph -> restaurerSauvegardeBase(uri, ph) },
                restaurerImport = { restaurerSauvegardeImport() },
                changerPin = { dialoguePin = true },
                reglagesVerrouillage = { dialogueDelai = true },
                reglagesTheme = { dialogueTheme = true },
                prochainId = { Stockage.prochainId(ctx) },
                validerInfo = { pers, section, valide -> validerInfo(pers, section, valide) },
                // G3-19 : annulation d'une suppression via le Snackbar.
                onAnnulerSuppression = { annulerSuppression() },
                onPurgerAnnulation = { modele.annulation = null }
            )
        )
        // G2-06 : indicateur d'occupation (image, GEDCOM, sauvegarde).
        if (occupation.isNotEmpty()) {
            Surface(
                Modifier.fillMaxWidth(),
                color = CouleurVertPale
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(occupation, style = MaterialTheme.typography.labelSmall, color = CouleurVertFonce)
                }
            }
        }
        }
    }

    // G1-15 : changement de code PIN (l'ancien est exige).
    if (dialoguePin) {
        var ancienPin by remember { mutableStateOf("") }
        var nouveauPin1 by remember { mutableStateOf("") }
        var nouveauPin2 by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialoguePin = false },
            title = { Text("Changer le code PIN") },
            text = {
                Column {
                    Text("L'ancien code est exig\u00E9 ; la base est re-chiffr\u00E9e avec le nouveau code, sans rien perdre.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = ancienPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) ancienPin = it },
                        label = { Text("Ancien code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = nouveauPin1,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) nouveauPin1 = it },
                        label = { Text("Nouveau code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    OutlinedTextField(
                        value = nouveauPin2,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) nouveauPin2 = it },
                        label = { Text("Confirmez le nouveau code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nouveauPin1.length < 4) {
                            message = "Le nouveau code doit contenir au moins 4 chiffres."
                        } else if (nouveauPin1 != nouveauPin2) {
                            message = "Les deux nouveaux codes ne correspondent pas."
                        } else if (Stockage.changerPin(ctx, ancienPin, nouveauPin1)) {
                            message = "Code PIN chang\u00E9 : utilisez le nouveau code au prochain d\u00E9verrouillage."
                            dialoguePin = false
                        } else {
                            message = "Ancien code incorrect."
                        }
                    },
                    enabled = ancienPin.isNotEmpty() && nouveauPin1.isNotEmpty() && nouveauPin2.isNotEmpty()
                ) { Text("Changer") }
            },
            dismissButton = {
                TextButton(onClick = { dialoguePin = false }) { Text("Annuler") }
            }
        )
    }

    // G1-14 : delai de reverrouillage reglable.
    if (dialogueDelai) {
        AlertDialog(
            onDismissRequest = { dialogueDelai = false },
            title = { Text("D\u00E9lai de verrouillage") },
            text = {
                Column {
                    Text("Verrouillage apr\u00E8s un passage en arri\u00E8re-plan ou l'extinction de l'\u00E9cran :", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { Stockage.definirDelaiVerrouillage(ctx, 0L); dialogueDelai = false; message = "Verrouillage imm\u00E9diat." }, Modifier.fillMaxWidth()) { Text("Imm\u00E9diat (par d\u00E9faut)") }
                    TextButton(onClick = { Stockage.definirDelaiVerrouillage(ctx, 60_000L); dialogueDelai = false; message = "Verrouillage apr\u00E8s 1 minute." }, Modifier.fillMaxWidth()) { Text("Apr\u00E8s 1 minute") }
                    TextButton(onClick = { Stockage.definirDelaiVerrouillage(ctx, 300_000L); dialogueDelai = false; message = "Verrouillage apr\u00E8s 5 minutes." }, Modifier.fillMaxWidth()) { Text("Apr\u00E8s 5 minutes") }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogueDelai = false }) { Text("Fermer") }
            }
        )
    }

    // G3-02 : choix du theme, memorise : Syst\u00E8me (suit le mode sombre du
    // t\u00E9l\u00E9phone), Clair ou Sombre. Appliqu\u00E9 imm\u00E9diatement.
    if (dialogueTheme) {
        AlertDialog(
            onDismissRequest = { dialogueTheme = false },
            title = { Text("Th\u00E8me de l'application") },
            text = {
                Column {
                    Text("Choisissez le th\u00E8me ; le choix est m\u00E9moris\u00E9 :", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        Stockage.definirTheme(ctx, "systeme")
                        themeChoisi.value = "systeme"
                        dialogueTheme = false
                    }, Modifier.fillMaxWidth()) { Text("Syst\u00E8me (par d\u00E9faut)") }
                    TextButton(onClick = {
                        Stockage.definirTheme(ctx, "clair")
                        themeChoisi.value = "clair"
                        dialogueTheme = false
                    }, Modifier.fillMaxWidth()) { Text("Clair") }
                    TextButton(onClick = {
                        Stockage.definirTheme(ctx, "sombre")
                        themeChoisi.value = "sombre"
                        dialogueTheme = false
                    }, Modifier.fillMaxWidth()) { Text("Sombre") }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogueTheme = false }) { Text("Fermer") }
            }
        )
    }

    // G1-04 : donnees illisibles -> affichage + restauration depuis la copie .bak
    if (fichiersIllisibles.isNotEmpty() && !avertissementLectureFerme) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Donn\u00E9es illisibles") },
            text = {
                Text(
                    fichiersIllisibles.size.toString() + " fichier(s) de donn\u00E9es ne se lisent plus, pas plus que leur copie .bak. L'enregistrement est bloqu\u00E9 pour ne rien \u00E9craser : restaurez la derni\u00E8re copie valide ou fermez sans rien modifier."
                )
            },
            confirmButton = {
                TextButton(onClick = { restaurerDepuisBak() }) { Text("Restaurer la copie .bak") }
            },
            dismissButton = {
                TextButton(onClick = { avertissementLectureFerme = true }) { Text("Fermer") }
            }
        )
    }

    acteVisionne?.let { a ->
        VisionneuseActe(ctx = ctx, acte = a, onFermer = { acteVisionne = null })
    }

    // Confirmation : noms saisis inconnus -> creer de nouvelles fiches
    // (numerotation deduite automatiquement) ou annuler.
    acteAConfirmer?.let { a ->
        val nouveaux = nomsNouveaux(a)
        AlertDialog(
            onDismissRequest = { acteAConfirmer = null },
            title = { Text("Noms inconnus") },
            text = {
                Column {
                    Text("Ces noms ne correspondent \u00E0 aucune fiche existante :")
                    nouveaux.forEach { n ->
                        Text("\u2022 " + n, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Cr\u00E9er de nouvelles fiches (la num\u00E9rotation sera d\u00E9duite automatiquement) ? Pour synchroniser avec une fiche existante, annulez et choisissez-la dans la saisie.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    acteAConfirmer = null
                    enregistrerActe(a, true)
                }) { Text("Cr\u00E9er les fiches") }
            },
            dismissButton = {
                TextButton(onClick = { acteAConfirmer = null }) { Text("Annuler") }
            }
        )
    }

    // Dialogue : choix du de cujus et de son n Sosa (pas forcement 1).
    // Toute la numerotation Sosa / d'Aboville en decoule automatiquement.
    // Dialogue : choix du de cujus (liste des personnes avec leur numero).
    // Toucher une personne ouvre le dialogue d'edition de son n Sosa.
    if (cujusDialogue) {
        LaunchedEffect(cujusDialogue) {
            val pre = cujusPreselec
            if (cujusDialogue && pre != null) {
                cujusEdite = pre
                cujusPreselec = null
            }
        }
        AlertDialog(
            onDismissRequest = { cujusDialogue = false },
            title = { Text("De cujus") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Touchez la personne de r\u00E9f\u00E9rence : son n\u00B0 Sosa (pas forc\u00E9ment 1) sera demand\u00E9, puis toute la num\u00E9rotation Sosa et d'Aboville sera recalcul\u00E9e.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        personnes.sortedWith(
                            compareBy({ it.nom.trim().uppercase() }, { it.prenoms.trim().uppercase() })
                        ).forEach { cand ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { cujusEdite = cand }
                                    .background(
                                        if (idDuCujus() == cand.id) CouleurVertPale
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                // Une seule ligne, tronquee a l'ellipsis ; plus de numero.
                                Text(
                                    cand.titre(),
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { cujusDialogue = false }) { Text("Fermer") }
            }
        )
    }

    // Dialogue d'edition du n Sosa du de cujus choisi : APERCU PUR pendant la
    // saisie (aucune donnee n'est modifiee avant Enregistrer), puis recalcule
    // complet + sauvegarde a l'enregistrement, avec message de confirmation.
    if (cujusEdite != null) {
        val cible = cujusEdite!!
        var numCujus by remember(cible.id) {
            mutableStateOf((cible.sosa.trim().toLongOrNull() ?: sosaCujus).toString())
        }
        val numSaisi = numCujus.trim().toLongOrNull() ?: 0L
        val numValide = numSaisi >= 1L
        // Apercu SANS toucher aux donnees : le numero du de cujus, puis les
        // numeros qui en decouleront (pere 2n, mere 2n+1, enfants n.1, n.2...).
        fun apercuNumerotation(n: Long): String {
            val m = mutableListOf<String>()
            m.add(cible.titre() + " : Sosa " + n.toString())
            if (cible.pere != 0L) m.add("P\u00E8re : Sosa " + (2L * n).toString())
            if (cible.mere != 0L) m.add("M\u00E8re : Sosa " + (2L * n + 1L).toString())
            if (cible.enfants.isNotEmpty()) {
                m.add("Enfants : d'Aboville " + n.toString() + ".1 \u00E0 " + n.toString() + "." + cible.enfants.size.toString())
            }
            return m.joinToString("\n")
        }
        fun annulerCujus() {
            cujusEdite = null
        }
        AlertDialog(
            onDismissRequest = { annulerCujus() },
            title = { Text("De cujus : " + cible.titre()) },
            text = {
                Column {
                    Text(
                        "N\u00B0 Sosa du de cujus : l'aper\u00E7u se met \u00E0 jour en direct ; Enregistrer recalcule et applique toute la num\u00E9rotation.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = numCujus,
                        onValueChange = { t -> numCujus = t.filter { it.isDigit() } },
                        label = { Text("N\u00B0 Sosa (chiffres)") },
                        isError = !numValide,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    if (numValide) {
                        Text(
                            apercuNumerotation(numSaisi),
                            style = MaterialTheme.typography.bodySmall,
                            color = CouleurVertFonce,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        definirCujus(cible, numSaisi)
                        cujusEdite = null
                        cujusDialogue = false
                    },
                    enabled = numValide
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { annulerCujus() }) { Text("Annuler") }
            }
        )
    }

    importConfirme?.let { importees ->
        AlertDialog(
            onDismissRequest = { importConfirme = null },
            title = { Text("Importer ce GEDCOM ?") },
            text = {
                Column {
                    Text(
                        "La base actuelle (" + personnes.size.toString() + " personnes, pistes et actes) sera REMPLAC\u00C9E par les " + importees.size.toString() + " personnes du fichier. Une sauvegarde automatique (restaurable depuis le menu GEDCOM) sera cr\u00E9\u00E9e avant le remplacement."
                    )
                    // G2-04 : avertissement listant ce que l import ignore.
                    if (importRapport.isNotEmpty()) {
                        Text(
                            "\u00C9l\u00E9ments ignor\u00E9s par l import (aucune perte silencieuse) :",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        importRapport.take(6).forEach { ligneIgn ->
                            Text("\u2022 " + ligneIgn, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    remplacerBase(importees)
                    importConfirme = null
                }) { Text("Remplacer") }
            },
            dismissButton = {
                TextButton(onClick = { importConfirme = null }) { Text("Annuler") }
            }
        )
    }
}


