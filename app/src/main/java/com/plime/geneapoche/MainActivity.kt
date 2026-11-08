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
import kotlin.reflect.KMutableProperty1
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
    // v0.43 : un selecteur de fichiers (GEDCOM, .gpbak) est ouvert : le
    // retour dans l app NE REVERROUILLE PAS, sinon la purge memoire (G1-14)
    // viderait la base et l export ecrirait un fichier a 0 personne.
    var pickerOuvert = false
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
        if (!verrou.pickerOuvert &&
            (delai == 0L || (d > 0L && System.currentTimeMillis() - d > delai))
        ) {
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
    // v0.78 : paires de doublons "gardees les deux" (decision definitive,
    // persistee) : elles restent hors du diagnostic de l'arbre.
    var doublonsGardes by remember { mutableStateOf(Stockage.lireDoublonsGardes(ctx)) }
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
        // G\u00E9n\u00E9aPoche 5 : communes et d\u00E9partements normalis\u00E9s
        // syst\u00E9matiquement \u00E0 chaque enregistrement (saisie, \u00E9dition,
        // import : aucune \u00E9chapatoire).
        personnes.forEach { normaliserLieux(it) }
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
                // G\u00E9n\u00E9aPoche 10 : COH\u00C9RENCE, l'enfant ne na\u00EEt pas avant son parent.
                // GeneaPoche 11 : UNE SEULE regle (incoherenceParente),
                // le conjoint connu est teste au meme titre.
                incoherenceParente(p, cible.dateNaissance)?.let { message = it; pickerRole = ""; return }
                val conjointL = personneParId(personnes, p.conjoint)
                if (conjointL != null) incoherenceParente(conjointL, cible.dateNaissance)?.let { message = it; pickerRole = ""; return }
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
    // v0.61 : cle de comparaison d'un nom - insensible a la casse, aux
    // accents et aux separateurs (espace, tiret, apostrophe). FINI les
    // doublons crees par une simple correction d'orthographe.
    fun cle(s: String): String =
        java.text.Normalizer.normalize(s.trim().uppercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("[\\p{Mn}\\p{Cf}']"), " ")
            .replace("-", " ")
            .split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    // Prenoms compares SANS ORDRE ("Marie Jeanne" = "Jeanne Marie").
    fun clePrenoms(s: String): String = cle(s).split(" ").filter { it.isNotEmpty() }.sorted().joinToString(" ")

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
            cle(q.nom) == cle(n) &&
            clePrenoms(q.prenoms) == clePrenoms(pr)
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
            cle(q.nom) == cle(n) &&
            clePrenoms(q.prenoms) == clePrenoms(pr) &&
            // v0.77 : vrai doublon = aussi la même date de naissance ; une
            // date différente = homonyme, une nouvelle fiche est créée.
            (q.dateNaissance.isBlank() || dateNaissance.isBlank() ||
                dateGreg(dateNaissance) == q.dateNaissance)
        }
        // GeneaPoche 11 (bug enfant ne avant parent) : coherence verifiee
        // AVANT toute creation - aucun refus ne laisse de fiche orpheline,
        // l'enfant EXISTANT deja date avant le parent est refuse aussi,
        // et le CONJOINT connu est teste au meme titre que la personne.
        val dateEff = existant?.dateNaissance?.takeIf { it.isNotBlank() } ?: dateGreg(dateNaissance)
        incoherenceParente(p, dateEff)?.let { message = it; return }
        val autreParentAj = personneParId(personnes, p.conjoint)
        if (autreParentAj != null) incoherenceParente(autreParentAj, dateEff)?.let { message = it; return }
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
        if (enfant.dateNaissance.isBlank() && dateNaissance.isNotBlank()) enfant.dateNaissance = dateGreg(dateNaissance)
        if (!p.enfants.contains(enfant.id)) p.enfants.add(enfant.id)
        if (p.sexe == "F") {
            if (enfant.mere == 0L) enfant.mere = p.id
            if (enfant.pere == 0L && p.conjoint != 0L) enfant.pere = p.conjoint
        } else {
            if (enfant.pere == 0L) enfant.pere = p.id
            if (enfant.mere == 0L && p.conjoint != 0L) enfant.mere = p.conjoint
        }
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
            "pere" -> {
                personneParId(personnes, p.pere)?.enfants?.remove(p.id)
                p.pere = 0L
            }
            "mere" -> {
                personneParId(personnes, p.mere)?.enfants?.remove(p.id)
                p.mere = 0L
            }
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
            cle(q.nom) == cle(n) &&
            clePrenoms(q.prenoms) == clePrenoms(pr)
        }
        // GeneaPoche 11 : coherence AVANT rattachement (et avant toute
        // creation de fiche) - un parent ne apres son enfant est refuse.
        if (existante != null) incoherenceParente(existante, p.dateNaissance)?.let { message = it; return }
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
                cle(q.nom) == cle(n) &&
                clePrenoms(q.prenoms) == clePrenoms(pr)
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

    // G\u00E9n\u00E9aPoche 11 (11.0) : coche de certification de l'identit\u00E9.
    // Etats : "auto" (1/6 minimum = coche, sinon croix), "coche", "croix".
    fun validerIdentite(p: Personne, etat: String) {
        p.identiteVerifiee = etat
        enregistrer(false, setOf(p.id))
        message = when (etat) {
            "coche" -> "Identit\u00E9 certifi\u00E9e : " + p.titre()
            "croix" -> "Identit\u00E9 marqu\u00E9e \u00E0 v\u00E9rifier : " + p.titre()
            else -> "Certification automatique (coche si 1/6 minimum) : " + p.titre()
        }
    }

    // G\u00E9n\u00E9aPoche 11 (11.2) : FUSION GUID\u00C9E d'un doublon. Toutes les
    // informations et tous les liens de [supprime] sont report\u00E9s sur
    // [garde] (sans jamais \u00E9craser une donn\u00E9e d\u00E9j\u00E0 pr\u00E9sente), puis la
    // fiche [supprime] dispara\u00EEt de la base.
    fun fusionnerPersonnes(gardeId: Long, supprimeId: Long) {
        if (gardeId == supprimeId) return
        val garde = personnes.firstOrNull { it.id == gardeId } ?: return
        val supprime = personnes.firstOrNull { it.id == supprimeId } ?: return
        // 1. Champs texte : recopi\u00E9s seulement s'ils sont vides chez garde.
        fun recopier(prop: KMutableProperty1<Personne, String>) {
            if (prop.get(garde).isBlank() && prop.get(supprime).isNotBlank()) {
                prop.set(garde, prop.get(supprime))
            }
        }
        listOf(
            Personne::profession, Personne::sexe, Personne::notesLibres,
            Personne::dateNaissance, Personne::heureNaissance, Personne::lieuNaissance, Personne::deptNaissance,
            Personne::dateBapteme, Personne::heureBapteme, Personne::lieuBapteme, Personne::deptBapteme,
            Personne::dateMariage, Personne::heureMariage, Personne::lieuMariage, Personne::deptMariage,
            Personne::dateDeces, Personne::heureDeces, Personne::lieuDeces, Personne::deptDeces,
            Personne::dateSepulture, Personne::heureSepulture, Personne::lieuSepulture, Personne::deptSepulture
        ).forEach { recopier(it) }
        // 2. Listes : union sans doublon.
        fun union(dest: MutableList<Long>, src: MutableList<Long>) {
            src.filterNot { it == supprimeId || dest.contains(it) }.forEach { dest.add(it) }
        }
        union(garde.enfants, supprime.enfants)
        union(garde.conjointsSupp, supprime.conjointsSupp)
        listOf("N", "B", "M", "D", "S").forEach { t ->
            union(garde.temoinsDe(t), supprime.temoinsDe(t))
            union(garde.signatairesDe(t), supprime.signatairesDe(t))
        }
        supprime.evenementsLibres.filterNot { ev -> garde.evenementsLibres.any { it.id == ev.id } }
            .forEach { garde.evenementsLibres.add(it) }
        supprime.blocsOuverts.filterNot { garde.blocsOuverts.contains(it) }.forEach { garde.blocsOuverts.add(it) }
        // 3. R\u00F4les : pris seulement s'ils sont inconnus chez garde.
        if (garde.pere == 0L && supprime.pere != 0L) garde.pere = supprime.pere
        if (garde.mere == 0L && supprime.mere != 0L) garde.mere = supprime.mere
        if (garde.conjoint == 0L && supprime.conjoint != 0L) garde.conjoint = supprime.conjoint
        // 4. Partout dans la base : toute r\u00E9f\u00E9rence au doublon vise garde.
        personnes.filter { it.id != supprimeId }.forEach { q ->
            fun remplace(v: Long): Long = if (v == supprimeId) gardeId else v
            q.pere = remplace(q.pere)
            q.mere = remplace(q.mere)
            q.conjoint = remplace(q.conjoint)
            q.conjointsSupp.replaceAll { remplace(it) }
            q.enfants.replaceAll { remplace(it) }
            listOf("N", "B", "M", "D", "S").forEach { t ->
                q.temoinsDe(t).replaceAll { remplace(it) }
                q.signatairesDe(t).replaceAll { remplace(it) }
            }
        }
        // D\u00E9doublonnage des listes de garde apr\u00E8s remplacement.
        fun dedupe(l: MutableList<Long>) {
            val vus = mutableSetOf<Long>()
            l.removeAll { !vus.add(it) }
        }
        dedupe(garde.enfants); dedupe(garde.conjointsSupp)
        listOf("N", "B", "M", "D", "S").forEach { t ->
            dedupe(garde.temoinsDe(t)); dedupe(garde.signatairesDe(t))
        }
        // 5. Actes et recherches du doublon : rattach\u00E9s \u00E0 garde.
        actes.filter { it.personneId == supprimeId }.forEach { it.personneId = gardeId }
        pistes.filter { it.personneId == supprimeId }.forEach { it.personneId = gardeId }
        // 6. Suppression effective du doublon.
        personnes.remove(supprime)
        pileFiches.removeAll { it == supprimeId }
        if (idOuvert == supprimeId) idOuvert = gardeId
        if (arbreCentre == supprimeId) arbreCentre = gardeId
        if (Stockage.lireIdCujus(ctx) == supprimeId) {
            Stockage.definirCujus(ctx, gardeId, sosaCujus)
        }
        enregistrer()
        message = "Fusion effectu\u00E9e : " + supprime.titre() + " a \u00E9t\u00E9 regroup\u00E9(e) avec " + garde.titre() + "."
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

    fun exporterGedcom(uri: Uri, sousEnsemble: Set<Long>?, exclureVivants: Boolean = false, format: String = "5.5.1") {
        // G2-06 : export HORS du thread principal, indicateur a l'ecran.
        occupation = "Export GEDCOM en cours..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                // G1-16 : option exclure les personnes vivantes (sans deces
                // connu, nees il y a moins de 100 ans).
                val annee = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val exportables = if (exclureVivants) personnes.filter { !estVivante(it, annee) } else personnes
                val sous = if (exclureVivants && sousEnsemble != null) sousEnsemble.filter { id -> exportables.any { it.id == id } }.toSet() else sousEnsemble
                val texte = construireGedcom(exportables, sous, actes, format)
                val n = if (sous == null) exportables.size else texte.split("\n").count { it.endsWith(" INDI") }
                // v0.41 : JAMAIS de fichier GEDCOM vide (base vide, branche
                // vide ou vivants exclus qui retirent tout) : export refuse.
                if (n == 0) {
                    withContext(Dispatchers.Main) { message = "Aucune personne \u00E0 exporter : export refus\u00E9, aucun fichier cr\u00E9\u00E9 (base vide, ou liste effac\u00E9e de la m\u00E9moire par un verrouillage pendant le choix du fichier : d\u00E9verrouillez puis recommencez)." }
                    return@launch
                }
                ctx.contentResolver.openOutputStream(uri)?.use { sortie ->
                    sortie.write(texte.toByteArray(Charsets.UTF_8))
                }
                // v0.39 : avertissement si le fichier contient moins de
                // personnes que la base (branche, vivants exclus...).
                val alerte = if (n < personnes.size) " ATTENTION : seulement " + n.toString() + " sur " + personnes.size.toString() + " personnes de la base." else ""
                // R1 : etat Compose JAMAIS modifie depuis le thread IO.
                withContext(Dispatchers.Main) {
                    message = "GEDCOM export\u00E9 (" + n.toString() + " personnes) : importable dans Heredis." + (if (exclureVivants) " Personnes vivantes exclues." else "") + alerte
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
                if (octets == null) {
                    withContext(Dispatchers.Main) { message = "Fichier GEDCOM illisible (ou trop volumineux)." }
                    return@launch
                }
                // v0.43 : un fichier de 0 octet est presque toujours un ancien
                // export refuse ou ecrase a vide : message explicite.
                if (octets.isEmpty()) {
                    withContext(Dispatchers.Main) { message = "Fichier vide (0 octet) : ancien export refus\u00E9 ou vide. Relancez un export depuis la base." }
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
        message = importees.size.toString() + " personnes import\u00E9es. Sauvegarde de la base pr\u00E9c\u00E9dente conserv\u00E9e (menu \u00AB Restaurer la base d'avant import \u00BB)."
    }

    // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements libres de la chronologie (intitul\u00E9
    // libre), modifi\u00E9s ou supprim\u00E9s depuis la fiche.
    fun modifierEvenementLibre(p: Personne, ev: EvenementLibre) {
        val i = p.evenementsLibres.indexOfFirst { it.id == ev.id }
        if (i >= 0) p.evenementsLibres[i] = ev else p.evenementsLibres.add(ev)
        enregistrer(false, setOf(p.id))
    }

    fun supprimerEvenementLibre(p: Personne, idEv: Long) {
        p.evenementsLibres.removeAll { it.id == idEv }
        enregistrer(false, setOf(p.id))
    }

    // G\u00E9n\u00E9aPoche 3 : import par FUSION intelligente. Les personnes
    // d\u00E9j\u00E0 pr\u00E9sentes (m\u00EAme nom et pr\u00E9noms, date de naissance
    // compatible) sont COMPL\u00C9T\u00C9ES champ par champ (jamais \u00E9cras\u00E9es),
    // jamais dupliqu\u00E9es ; les autres sont ajout\u00E9es avec de nouveaux ids ;
    // tous les liens (p\u00E8re, m\u00E8re, conjoint, enfants) sont remapp\u00E9s.
    fun fusionnerBase(importees: List<Personne>) {
        val correspondances = mutableMapOf<Long, Long>()
        fun anneesCompatibles(d1: String, d2: String): Boolean {
            val a1 = anneeDe(d1)
            val a2 = anneeDe(d2)
            return a1 == null || a2 == null || a1 == a2
        }
        // Passe 1 : reconnaissance des personnes existantes et compl\u00E9tion
        // de leurs champs ENCORE VIDES uniquement.
        importees.forEach { imp ->
            val doublon = personnes.firstOrNull { q ->
                q.nom.trim().uppercase() == imp.nom.trim().uppercase() &&
                q.prenoms.trim().uppercase() == imp.prenoms.trim().uppercase() &&
                anneesCompatibles(q.dateNaissance, imp.dateNaissance)
            }
            if (doublon != null) {
                correspondances[imp.id] = doublon.id
                if (doublon.sosa.isBlank() && imp.sosa.isNotBlank()) doublon.sosa = imp.sosa
                if (doublon.sexe.isBlank() && imp.sexe.isNotBlank()) doublon.sexe = imp.sexe
                if (doublon.dateNaissance.isBlank()) doublon.dateNaissance = imp.dateNaissance
                if (doublon.heureNaissance.isBlank()) doublon.heureNaissance = imp.heureNaissance
                if (doublon.lieuNaissance.isBlank()) doublon.lieuNaissance = imp.lieuNaissance
                if (doublon.deptNaissance.isBlank()) doublon.deptNaissance = imp.deptNaissance
                if (doublon.dateBapteme.isBlank()) doublon.dateBapteme = imp.dateBapteme
                if (doublon.heureBapteme.isBlank()) doublon.heureBapteme = imp.heureBapteme
                if (doublon.lieuBapteme.isBlank()) doublon.lieuBapteme = imp.lieuBapteme
                if (doublon.deptBapteme.isBlank()) doublon.deptBapteme = imp.deptBapteme
                if (doublon.dateMariage.isBlank()) doublon.dateMariage = imp.dateMariage
                if (doublon.heureMariage.isBlank()) doublon.heureMariage = imp.heureMariage
                if (doublon.lieuMariage.isBlank()) doublon.lieuMariage = imp.lieuMariage
                if (doublon.deptMariage.isBlank()) doublon.deptMariage = imp.deptMariage
                if (doublon.dateDeces.isBlank()) doublon.dateDeces = imp.dateDeces
                if (doublon.heureDeces.isBlank()) doublon.heureDeces = imp.heureDeces
                if (doublon.lieuDeces.isBlank()) doublon.lieuDeces = imp.lieuDeces
                if (doublon.deptDeces.isBlank()) doublon.deptDeces = imp.deptDeces
                if (doublon.dateSepulture.isBlank()) doublon.dateSepulture = imp.dateSepulture
                if (doublon.heureSepulture.isBlank()) doublon.heureSepulture = imp.heureSepulture
                if (doublon.lieuSepulture.isBlank()) doublon.lieuSepulture = imp.lieuSepulture
                if (doublon.deptSepulture.isBlank()) doublon.deptSepulture = imp.deptSepulture
                if (doublon.profession.isBlank()) doublon.profession = imp.profession
                if (doublon.notesLibres.isBlank()) doublon.notesLibres = imp.notesLibres
                imp.evenementsLibres.forEach { evI ->
                    if (doublon.evenementsLibres.none {
                        it.intitule.trim().uppercase() == evI.intitule.trim().uppercase() &&
                        it.date.trim() == evI.date.trim()
                    }) {
                        doublon.evenementsLibres.add(evI.copy(id = Stockage.prochainId(ctx)))
                    }
                }
            }
        }
        // Passe 2 : ajout des personnes ABSENTES avec de nouveaux ids.
        var ajoutees = 0
        val idsNouvelles = mutableMapOf<Long, Long>()
        importees.filter { !correspondances.containsKey(it.id) }.forEach { imp ->
            val nid = Stockage.prochainId(ctx)
            idsNouvelles[imp.id] = nid
            personnes.add(imp.copy(id = nid))
            ajoutees += 1
        }
        // Passe 3 : remap des liens vers la base fusionn\u00E9e (aucun doublon).
        fun cible(idImp: Long): Long = correspondances[idImp] ?: idsNouvelles[idImp] ?: 0L
        importees.forEach { imp ->
            val q = personneParId(personnes, cible(imp.id)) ?: return@forEach
            if (q.pere == 0L && imp.pere != 0L) q.pere = cible(imp.pere)
            if (q.mere == 0L && imp.mere != 0L) q.mere = cible(imp.mere)
            if (q.conjoint == 0L && imp.conjoint != 0L) q.conjoint = cible(imp.conjoint)
            imp.enfants.forEach { idEnf ->
                val idCible = cible(idEnf)
                if (idCible != 0L && !q.enfants.contains(idCible)) {
                    val enfImp = personneParId(personnes, idCible)
                    if (enfImp == null || incoherenceParente(q, enfImp.dateNaissance) == null) q.enfants.add(idCible)
                }
            }
        }
        verifierLiensEtRapporter()
        enregistrer()
        message = "Fusion termin\u00E9e : " + correspondances.size.toString() + " personne(s) compl\u00E9t\u00E9e(s), " + ajoutees.toString() + " ajout\u00E9e(s). Aucun doublon."
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
        // v0.41 : RESCUE - les 5 sauvegardes d'avant import sont essayees de
        // la plus recente a la plus ancienne ; la premiere qui rend une base
        // NON VIDE est gardee (une sauvegarde vide est sautee).
        val slots = Stockage.slotsImport(ctx)
        if (slots.isEmpty() && !Stockage.aSauvegardeImport(ctx)) {
            message = "Aucune sauvegarde d'import disponible."
            return
        }
        for (s in slots) {
            if (!Stockage.restaurerSauvegardeImportSlot(ctx, s)) continue
            val essai = Stockage.charger(ctx)
            if (essai.liste.isEmpty()) continue
            derniersJson.clear()
            personnes.clear()
            pistes.clear()
            actes.clear()
            val rp = essai
            val rj = Stockage.chargerJournal(ctx)
            val ra = Stockage.chargerActes(ctx)
            personnes.addAll(rp.liste)
            pistes.addAll(rj.liste)
            actes.addAll(ra.liste)
            fichiersIllisibles = listOf(rp, rj, ra).filter { it.illisible }.map { it.nom }
            pileFiches.clear()
            idOuvert = 0L
            verifierLiensEtRapporter()
            message = "Base d'avant import restaur\u00E9e : " + personnes.size.toString() + " personne(s) (sauvegarde n\u00B0" + s.toString() + ")."
            rafraichirTout()
            return
        }
        // Aucun slot exploitable : repli sur l'ancienne sauvegarde plate.
        if (Stockage.aSauvegardeImport(ctx) && Stockage.restaurerSauvegardeImport(ctx)) {
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
            message = if (personnes.isEmpty()) "Aucune sauvegarde ne contient de personnes." else "Base d'avant import restaur\u00E9e : " + personnes.size.toString() + " personne(s)."
            rafraichirTout()
            return
        }
        message = "Aucune sauvegarde exploitable."
    }

    // v0.42 : RESCUE .bak - restaure les copies de rotation des trois
    // fichiers (personnes/journal/actes), dernier recours apres une base
    // videe, meme si les fichiers actuels sont lisibles. Sans effet si la
    // copie .bak est vide aussi : rien n'est perdu en l'essayant.
    fun restaurerSecoursBak() {
        listOf("personnes.bin", "journal.bin", "actes.bin").forEach { nom ->
            Stockage.restaurerBak(ctx, nom)
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
        if (personnes.isEmpty()) {
            message = "La copie de secours ne contient aucune personne non plus."
        } else {
            verifierLiensEtRapporter()
            completerJournal()
            message = "Base restaur\u00E9e depuis la copie de secours : " + personnes.size.toString() + " personne(s)."
        }
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
            cle(it.nom) == cle(n) &&
            clePrenoms(it.prenoms) == clePrenoms(pr)
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
        // v0.73 : coherence ETENDUE - les temoins (parrain, marraine,
        // pretre, officier d'etat civil, temoin) sont verifies aussi.
        a.temoins.forEach { t ->
            if ((t.nom.isNotBlank() || t.prenoms.isNotBlank()) && !nomDejaConnu(t.nom, t.prenoms)) {
                res.add(
                    (if (t.role.isNotBlank()) t.role.trim().lowercase() + " : " else "t\u00E9moin : ") +
                        (t.prenoms.trim() + " " + t.nom.trim()).trim()
                )
            }
        }
        return res
    }

    // Synchronisation avec les fiches existantes, ou proposition de creation.
    fun enregistrerActe(a: ActeSaisie, confirme: Boolean = false) {
        // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nement \u00E0 intitul\u00E9 LIBRE (menu Saisie ou
        // fiche) : aucun acte cr\u00E9\u00E9, l'\u00E9v\u00E9nement est rang\u00E9 dans la
        // chronologie de la personne (evenementsLibres), tri\u00E9 par date.
        if (a.type == "E") {
            val cible = personneParId(personnes, a.personneId)
            if (cible == null) {
                message = "\u00C9v\u00E9nement non enregistr\u00E9 : personne concern\u00E9e non identifi\u00E9e."
                return
            }
            val idEv = if (a.acteId != 0L) a.acteId else Stockage.prochainId(ctx)
            val ev = EvenementLibre(
                id = idEv,
                intitule = a.intitule.trim(),
                date = a.date.trim(), heure = a.heure.trim(),
                lieu = a.commune.trim(), dept = a.dept.trim()
            )
            val i = cible.evenementsLibres.indexOfFirst { it.id == idEv }
            if (i >= 0) cible.evenementsLibres[i] = ev else cible.evenementsLibres.add(ev)
            enregistrer(false, setOf(cible.id))
            message = "\u00C9v\u00E9nement enregistr\u00E9 : " + a.intitule.trim().ifEmpty { "sans intitul\u00E9" }
            return
        }
        val nouveaux = nomsNouveaux(a)
        if (nouveaux.isNotEmpty() && !confirme) {
            acteAConfirmer = a
            return
        }
        acteAConfirmer = null
        var crees = 0

        // G\u00E9n\u00E9aPoche 2 : edition d'un acte existant depuis la fiche.
        // Le formulaire etait pre-rempli ; on MET A JOUR l'acte et les champs
        // de la fiche (un champ laisse vide = effac\\u00E9 volontairement).
        val acteExistant = if (a.acteId != 0L) actes.firstOrNull { it.id == a.acteId } else null
        val edition = acteExistant != null || a.editionFiche  // G\u00E9n\u00E9aPoche 10 : appui long = edition des champs
        val ancienContenu = acteExistant?.contenu ?: ""

        // Trouve une personne existante (par id ou nom+premoms), sinon la cree.
        fun trouverOuCreer(id: Long, nomSaisi: String, prenomsSaisis: String, sexeDefaut: String,
            dateNaiss: String = "", forcer: Boolean = false): Personne? {
            personneParId(personnes, id)?.let { return it }
            val n = nomSaisi.trim()
            val pr = prenomsSaisis.trim()
            if (n.isEmpty() && pr.isEmpty()) return null
            // v0.77 : un VRAI doublon = même nom, mêmes prénoms ET même
            // date de naissance. Une date différente (ou le drapeau "Créer
            // une nouvelle fiche") = homonyme : une nouvelle fiche est créée.
            if (!forcer) personnes.firstOrNull {
                cle(it.nom) == cle(n) &&
                clePrenoms(it.prenoms) == clePrenoms(pr) &&
                (it.dateNaissance.isBlank() || dateNaiss.isBlank() || dateGreg(dateNaiss) == it.dateNaissance)
            }?.let { return it }
            val np = Personne(
                id = Stockage.prochainId(ctx),
                nom = n, prenoms = pr, sexe = if (sexeDefaut.isNotEmpty()) sexeDefaut else ""
            )
            personnes.add(np)
            crees += 1
            return np
        }

        // Renseigne les champs du parent : toute valeur saisie non vide
        // REMPLACE celle de la fiche (v0.60 : synchronisation totale).
        fun renseigner(c: Personne?, ne: String, lieu: String, dept: String, prof: String, dec: String, lieuDec: String) {
            if (c == null) return
            if (edition) {
                // v0.65 : en EDITION, le formulaire est la r\u00E9f\u00E9rence
                // compl\u00E8te : une valeur vide EFFACE le champ du parent.
                c.dateNaissance = dateGreg(ne)
                c.lieuNaissance = lieu.trim()
                c.deptNaissance = dept.trim()
                c.profession = prof.trim()
                c.dateDeces = dateGreg(dec)
                c.lieuDeces = lieuDec.trim()
            } else {
                if (ne.isNotBlank()) c.dateNaissance = dateGreg(ne)
                if (lieu.isNotBlank()) c.lieuNaissance = lieu.trim()
                if (dept.isNotBlank()) c.deptNaissance = dept.trim()
                if (prof.isNotBlank()) c.profession = prof.trim()
                if (dec.isNotBlank()) c.dateDeces = dateGreg(dec)
                if (lieuDec.isNotBlank()) c.lieuDeces = lieuDec.trim()
            }
        }

        fun lierParent(enfant: Personne, parent: Personne?, estMere: Boolean) {
            if (parent == null) return
            if (estMere) {
                if (enfant.mere == 0L) enfant.mere = parent.id
            } else {
                if (enfant.pere == 0L) enfant.pere = parent.id
            }
            if (!parent.enfants.contains(enfant.id) && incoherenceParente(parent, enfant.dateNaissance) == null) parent.enfants.add(enfant.id)
        }

        val p = trouverOuCreer(a.personneId, a.nom, a.prenoms, a.sexe, if (a.type == "N") a.date else "", a.persNouvelle)
        if (p == null) {
            message = "Acte non enregistr\u00E9 : personne concern\u00E9e non identifi\u00E9e."
            return
        }


        // v0.60 : synchronisation TOTALE - le formulaire est la reference
        // pour tout champ non vide, identite comprise.
        // v0.65 : tout changement d'IDENTITE est signal\u00E9 dans le message
        // final (un m\u00E9lange de champs ne peut plus passer inaper\u00E7u).
        var identiteTxt = ""
        if (a.nom.isNotBlank() && cle(p.nom) != cle(a.nom.trim()))
            identiteTxt += "Nom modifi\u00E9 : " + p.nom + " \u2192 " + a.nom.trim() + ". "
        if (a.prenoms.isNotBlank() && clePrenoms(p.prenoms) != clePrenoms(a.prenoms.trim()))
            identiteTxt += "Pr\u00E9noms modifi\u00E9s : " + p.prenoms + " \u2192 " + a.prenoms.trim() + ". "
        if (a.profession.isNotBlank() && p.profession.trim() != a.profession.trim())
            identiteTxt += "Profession modifi\u00E9e : " + p.profession + " \u2192 " + a.profession.trim() + ". "
        if (a.nom.isNotBlank()) p.nom = a.nom.trim()
        if (a.prenoms.isNotBlank()) p.prenoms = a.prenoms.trim()
        // Date, heure et lieu de l'acte selon son type (fiche de la personne concernee).
        // Nouvel acte : ne remplit que les champs encore vides. Edition : le
        // formulaire pre-rempli devient la reference (champ vide = efface).
        // v0.73 : bug CORRIGE - en edition, une COMMUNE ou un
        // DEPARTEMENT vide EFFACE la valeur de la fiche (la correction
        // reste desormais enregistree) ; le departement est normalise
        // partout (2 chiffres).
        when (a.type) {
            "N" -> {
                if (edition || a.date.isNotBlank()) p.dateNaissance = dateGreg(a.date)
                if (edition || a.heure.isNotBlank()) p.heureNaissance = a.heure
                if (edition || a.commune.isNotBlank()) p.lieuNaissance = a.commune.trim()
                if (edition || a.dept.isNotBlank()) p.deptNaissance = normaliserDept(a.dept)
            }
            "B" -> {
                if (edition || a.date.isNotBlank()) p.dateBapteme = dateGreg(a.date)
                if (edition || a.heure.isNotBlank()) p.heureBapteme = a.heure
                if (edition || a.commune.isNotBlank()) p.lieuBapteme = a.commune.trim()
                if (edition || a.dept.isNotBlank()) p.deptBapteme = normaliserDept(a.dept)
            }
            "M" -> {
                if (edition || a.date.isNotBlank()) p.dateMariage = dateGreg(a.date)
                if (edition || a.heure.isNotBlank()) p.heureMariage = a.heure
                if (edition || a.commune.isNotBlank()) p.lieuMariage = a.commune.trim()
                if (edition || a.dept.isNotBlank()) p.deptMariage = normaliserDept(a.dept)
            }
            "D" -> {
                if (edition || a.date.isNotBlank()) p.dateDeces = dateGreg(a.date)
                if (edition || a.heure.isNotBlank()) p.heureDeces = a.heure
                if (edition || a.commune.isNotBlank()) p.lieuDeces = a.commune.trim()
                if (edition || a.dept.isNotBlank()) p.deptDeces = normaliserDept(a.dept)
            }
            "S" -> {
                if (edition || a.date.isNotBlank()) p.dateSepulture = dateGreg(a.date)
                if (edition || a.heure.isNotBlank()) p.heureSepulture = a.heure
                if (edition || a.commune.isNotBlank()) p.lieuSepulture = a.commune.trim()
                if (edition || a.dept.isNotBlank()) p.deptSepulture = normaliserDept(a.dept)
            }
        }
        if (a.profession.isNotBlank()) p.profession = a.profession.trim()

        // G\u00E9n\u00E9aPoche 3 : "Enfant" du menu Saisie : la naissance est
        // rattach\u00E9e au parent choisi (et \u00E0 son conjoint). Aucun doublon.
        // GeneaPoche 12 : BUG CORRIGE - le refus de lien ne peut plus etre
        // ecrase par le message final "Acte enregistre" (trompeur) : le
        // refus est memorise et rappele a la fin de l'enregistrement.
        var refusLienTxt = ""
        if (a.type == "N" && a.parentPourEnfant != 0L) {
            val parentChoisi = personneParId(personnes, a.parentPourEnfant)
            if (parentChoisi != null && parentChoisi.id != p.id) {
                // G\u00E9n\u00E9aPoche 10 : COH\u00C9RENCE, l'enfant ne na\u00EEt pas avant son parent.
                // GeneaPoche 11 : UNE SEULE regle (incoherenceParente),
                // le conjoint du parent est teste au meme titre.
                val refusN = incoherenceParente(parentChoisi, p.dateNaissance)
                    ?: personneParId(personnes, parentChoisi.conjoint)?.let { co -> incoherenceParente(co, p.dateNaissance) }
                if (refusN != null) {
                    refusLienTxt = refusN
                } else {
                    if (parentChoisi.sexe == "F") {
                        if (p.mere == 0L) p.mere = parentChoisi.id
                    } else {
                        if (p.pere == 0L) p.pere = parentChoisi.id
                    }
                    if (!parentChoisi.enfants.contains(p.id)) parentChoisi.enfants.add(p.id)
                    val conjointDuParent = personneParId(personnes, parentChoisi.conjoint)
                    if (conjointDuParent != null && !conjointDuParent.enfants.contains(p.id) && incoherenceParente(conjointDuParent, p.dateNaissance) == null) {
                        conjointDuParent.enfants.add(p.id)
                    }
                }
            }
        }

        // Pere et mere de la personne concernee.
        // v0.65 : l'identifiant du parent li\u00E9 n'est gard\u00E9 QUE si le nom
        // saisi est rest\u00E9 le sien : un homonyme ne d\u00E9route plus le lien,
        // une correction de nom ne force plus l'ancienne fiche.
        val pereLie = personneParId(personnes, p.pere)
        val idPere = if (pereLie != null && a.pereNom.isNotBlank() &&
            cle(pereLie.nom) == cle(a.pereNom) &&
            clePrenoms(pereLie.prenoms) == clePrenoms(a.perePrenoms)) pereLie.id else 0L
        val pere = trouverOuCreer(idPere, a.pereNom, a.perePrenoms, "M", a.pereNe)
        renseigner(pere, a.pereNe, a.pereLieu, a.pereDept, a.pereProf, a.pereDeces, a.pereLieuDeces)
        lierParent(p, pere, false)
        val mereLie = personneParId(personnes, p.mere)
        val idMere = if (mereLie != null && a.mereNom.isNotBlank() &&
            cle(mereLie.nom) == cle(a.mereNom) &&
            clePrenoms(mereLie.prenoms) == clePrenoms(a.merePrenoms)) mereLie.id else 0L
        val mere = trouverOuCreer(idMere, a.mereNom, a.merePrenoms, "F", a.mereNe)
        renseigner(mere, a.mereNe, a.mereLieu, a.mereDept, a.mereProf, a.mereDeces, a.mereLieuDeces)
        lierParent(p, mere, true)

        // v0.60 : le nom saisi pour un parent est TOUJOURS la reference :
        // s'il correspond a une AUTRE fiche que le lien actuel, le lien de
        // parente est REMPLACE (reparation des liens errones, ex une mere
        // pointant sur la belle-mere). En EDITION seulement, un nom laisse
        // vide RETIRE le lien. La coherence des dates est respectee.
        // v0.64 : plus AUCUN remplacement de lien silencieux. Un refus
        // (incoherenceParente) est EXPLIQUE a l'utilisateur, un remplacement
        // ou un retrait est CONFIRME en clair dans le message final.
        var retourLiens = ""
        var refusParentTxt = ""
        val ancienPere = personneParId(personnes, p.pere)
        if (pere == null) {
            // v0.65 : le lien n'est retir\u00E9 que si un nom rempli \u00E0
            // l'ouverture a \u00E9t\u00E9 vid\u00E9 (action explicite).
            if (edition && a.pereNomInit.isNotBlank()) {
                ancienPere?.enfants?.remove(p.id)
                p.pere = 0L
                retourLiens += "Lien du p\u00E8re retir\u00E9. "
            }
        } else if (pere.id != p.pere) {
            val motifP = incoherenceParente(pere, p.dateNaissance)
            if (motifP == null) {
                ancienPere?.enfants?.remove(p.id)
                p.pere = pere.id
                retourLiens += "P\u00E8re de la fiche remplac\u00E9 : " + (ancienPere?.titre() ?: "aucun") + " \u2192 " + pere.titre() + ". "
            } else {
                refusParentTxt += "P\u00E8re NON remplac\u00E9 : " + motifP + " "
            }
        }
        val ancienneMere = personneParId(personnes, p.mere)
        if (mere == null) {
            // v0.65 : le lien n'est retir\u00E9 que si un nom rempli \u00E0
            // l'ouverture a \u00E9t\u00E9 vid\u00E9 (action explicite).
            if (edition && a.mereNomInit.isNotBlank()) {
                ancienneMere?.enfants?.remove(p.id)
                p.mere = 0L
                retourLiens += "Lien de la m\u00E8re retir\u00E9. "
            }
        } else if (mere.id != p.mere) {
            val motifM = incoherenceParente(mere, p.dateNaissance)
            if (motifM == null) {
                ancienneMere?.enfants?.remove(p.id)
                p.mere = mere.id
                retourLiens += "M\u00E8re de la fiche remplac\u00E9e : " + (ancienneMere?.titre() ?: "aucune") + " \u2192 " + mere.titre() + ". "
            } else {
                refusParentTxt += "M\u00E8re NON remplac\u00E9e : " + motifM + " "
            }
        }

        // Mariee et ses parents (mariage uniquement).
        var mariee: Personne? = null
        if (a.type == "M") {
            mariee = trouverOuCreer(a.marieeId, a.marieeNom, a.marieePrenoms, "F")
            if (mariee != null) {
                if (a.date.isNotBlank()) mariee.dateMariage = dateGreg(a.date)
                if (a.heure.isNotBlank()) mariee.heureMariage = a.heure
                if (a.commune.isNotBlank()) mariee.lieuMariage = a.commune
                if (a.dept.isNotBlank()) mariee.deptMariage = a.dept
                if (a.marieeProf.isNotBlank()) mariee.profession = a.marieeProf.trim()
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
        // v0.80 : sur un D\u00C9C\u00C8S ou une S\u00C9PULTURE, le conjoint est EN VIE
        // (d\u00E9clarant possible) ou D\u00C9C\u00C9D\u00C9 AVANT (veuf/veuve) : le choix du
        // formulaire est PERSIST\u00C9 dans la transcription de l'acte.
        if (a.type == "D" || a.type == "S") {
            val nomConj = (a.marieePrenoms.trim() + " " + a.marieeNom.trim()).trim()
                .ifEmpty { personneParId(personnes, a.marieeId)?.titre() ?: "" }
            if (nomConj.isNotEmpty()) {
                lignes.add((if (a.conjointEnVie) "Conjoint survivant : " else "Conjoint d\u00E9c\u00E9d\u00E9 avant : ") + nomConj)
                // v0.80 : un conjoint D\u00C9C\u00C9D\u00C9 AVANT sans date de d\u00E9c\u00E8s connue
                // re\u00E7oit "avant aaaa" sur sa fiche (qualification valide).
                if (!a.conjointEnVie) {
                    val conjFiche = personneParId(personnes, a.marieeId)
                    val anneeConj = anneeDe(a.date)
                    if (conjFiche != null && conjFiche.dateDeces.isBlank() && anneeConj != null) {
                        conjFiche.dateDeces = "avant " + anneeConj.toString()
                    }
                }
            }
        }
        if (mariee != null) {
            lignes.add("Mari\u00E9e : " + mariee.titre() + (if (a.marieeProf.isNotBlank()) ", " + a.marieeProf else ""))
            descriptionParent((a.pMarieeNom + " " + a.pMarieePrenoms).trim(), a.pMarieeNe, a.pMarieeLieu, a.pMarieeDept, a.pMarieeProf, a.pMarieeDeces, a.pMarieeLieuDeces)
                .takeIf { it.isNotEmpty() }?.let { lignes.add("P\u00E8re de la mari\u00E9e : " + it) }
            descriptionParent((a.mMarieeNom + " " + a.mMarieePrenoms).trim(), a.mMarieeNe, a.mMarieeLieu, a.mMarieeDept, a.mMarieeProf, a.mMarieeDeces, a.mMarieeLieuDeces)
                .takeIf { it.isNotEmpty() }?.let { lignes.add("M\u00E8re de la mari\u00E9e : " + it) }
        }
        val temoinsTexte = a.temoins.filter { it.nom.isNotBlank() }.joinToString(" ; ") { t ->
            // v0.72 : le PR\u00CATRE ou l'OFFICIER D'\u00C9TAT CIVIL figure avec son r\u00F4le.
            (if (t.role.isNotBlank()) t.role.trim() + " : " else "") +
                (t.prenoms.trim() + " " + t.nom.trim()).trim() + (if (t.profession.isNotBlank()) " (" + t.profession.trim() + ")" else "")
        }
        if (temoinsTexte.isNotEmpty()) lignes.add((if (a.type == "B") "Parrain et marraine : " else "T\u00E9moins : ") + temoinsTexte)
        val signatairesNoms = a.signataires.mapNotNull { l ->
            when {
                l == "Concerne(e)" -> p.titre()
                l == "P\u00E8re" -> (a.pereNom + " " + a.perePrenoms).trim().ifEmpty { pere?.titre() }
                l == "M\u00E8re" -> (a.mereNom + " " + a.merePrenoms).trim().ifEmpty { mere?.titre() }
                l == "Mari\u00E9e" -> (a.marieeNom + " " + a.marieePrenoms).trim().ifEmpty { mariee?.titre() }
                l == "Conjoint(e)" -> (a.marieeNom + " " + a.marieePrenoms).trim().ifEmpty { personneParId(personnes, a.marieeId)?.titre() ?: "" }
                // v0.72 : coches \u00E9largies - parents de la mari\u00E9e, pr\u00EAtre,
                // officier d'\u00E9tat civil.
                l == "P\u00E8re de la mari\u00E9e" -> (a.pMarieeNom + " " + a.pMarieePrenoms).trim()
                l == "M\u00E8re de la mari\u00E9e" -> (a.mMarieeNom + " " + a.mMarieePrenoms).trim()
                // v0.80 : l'enfant DECLARANT d'un d\u00E9c\u00E8s.
                l == "Enfant d\u00E9clarant" -> personneParId(personnes, a.declarantEnfantId)?.titre() ?: ""
                l == "Pr\u00EAtre" || l == "Officier d'\u00E9tat civil" -> {
                    val t = a.temoins.firstOrNull { it.role == l } ?: return@mapNotNull null
                    (t.prenoms.trim() + " " + t.nom.trim()).trim()
                }
                // v0.73 : sur un bapteme, parrain et marraine signent.
                l == "Parrain" -> {
                    val t = a.temoins.getOrNull(0) ?: return@mapNotNull null
                    (t.prenoms.trim() + " " + t.nom.trim()).trim()
                }
                l == "Marraine" -> {
                    val t = a.temoins.getOrNull(1) ?: return@mapNotNull null
                    (t.prenoms.trim() + " " + t.nom.trim()).trim()
                }
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
        // Edition : l'acte existant est mis a jour (pas de doublon), ainsi que
        // son jumeau chez le conjoint (reconnu par son ancien contenu).
        if (acteExistant != null) {
            acteExistant.titre = entete
            acteExistant.contenu = contenu
            acteExistant.reference = a.reference.trim()
            acteExistant.depot = a.depot.trim()
            acteExistant.dateConsultation = a.dateConsultation.trim()
            acteExistant.fiabilite = a.fiabilite
            if (mariee != null) {
                val jumeau = actes.firstOrNull {
                    it.personneId == mariee.id && it.type == a.type &&
                    it.contenu == ancienContenu && it.id != acteExistant.id
                }
                if (jumeau != null) {
                    jumeau.titre = entete
                    jumeau.contenu = contenu
                    jumeau.reference = a.reference.trim()
                    jumeau.depot = a.depot.trim()
                    jumeau.dateConsultation = a.dateConsultation.trim()
                    jumeau.fiabilite = a.fiabilite
                } else {
                    actes.add(
                        Acte(
                            id = Stockage.prochainId(ctx), personneId = mariee.id, type = a.type,
                            titre = entete, creation = System.currentTimeMillis(),
                            contenu = contenu, reference = a.reference.trim(),
                depot = a.depot.trim(),
                dateConsultation = a.dateConsultation.trim(),
                fiabilite = a.fiabilite
                        )
                    )
                }
            }
        } else {
        var idActe = Stockage.prochainId(ctx)
        actes.add(
            Acte(
                id = idActe, personneId = p.id, type = a.type, titre = entete,
                creation = System.currentTimeMillis(), contenu = contenu,
                reference = a.reference.trim(),
                depot = a.depot.trim(),
                dateConsultation = a.dateConsultation.trim(),
                fiabilite = a.fiabilite
            )
        )
        if (mariee != null) {
            idActe = Stockage.prochainId(ctx)
            actes.add(
                Acte(
                    id = idActe, personneId = mariee.id, type = a.type, titre = entete,
                    creation = System.currentTimeMillis(), contenu = contenu,
                    reference = a.reference.trim(),
                depot = a.depot.trim(),
                dateConsultation = a.dateConsultation.trim(),
                fiabilite = a.fiabilite
                )
            )
        }
        }

        // v0.73 : COHERENCE - chaque temoin (parrain, marraine, pretre,
        // officier, temoin) est rattache a une FICHE : la personne
        // existante est reutilisee, sinon une fiche est creee, et le lien
        // temoin de l'acte est enregistre.
        a.temoins.filter { it.nom.isNotBlank() }.forEach { t ->
            val ficheT = trouverOuCreer(0L, t.nom, t.prenoms, "", "", t.nouveau) ?: return@forEach
            if (t.profession.isNotBlank() && ficheT.profession.isBlank()) ficheT.profession = t.profession.trim()
            if (!p.temoinsDe(a.type).contains(ficheT.id)) p.temoinsDe(a.type).add(ficheT.id)
            if (a.type == "M" && mariee != null && !mariee.temoinsDe(a.type).contains(ficheT.id)) {
                mariee.temoinsDe(a.type).add(ficheT.id)
            }
        }

        // v0.80 : l'enfant DECLARANT d'un d\u00E9c\u00E8s est m\u00E9moris\u00E9 comme
        // signataire de la fiche (la coche revient \u00E0 l'\u00E9dition) ; le
        // formulaire est la r\u00E9f\u00E9rence : un enfant retir\u00E9 dispara\u00EEt.
        if (a.type == "D") {
            val listeS = p.signatairesDe("D")
            listeS.toList().forEach { id ->
                val q = personneParId(personnes, id)
                if (id != 0L && q != null && q.id != p.id &&
                    (q.pere == p.id || q.mere == p.id || p.enfants.contains(id))) {
                    listeS.remove(id)
                }
            }
            if (a.declarantEnfantId != 0L && !listeS.contains(a.declarantEnfantId)) {
                listeS.add(a.declarantEnfantId)
            }
        }

        enregistrer()
        // GeneaPoche 12 : message final EXACT - le lien refuse est rappele
        // (l'acte est conserve, SANS le lien de parente).
        // v0.64 : message COMPLET - refus parentPourEnfant, refus ou
        // remplacement de lien pere/mere, puis le resume de l'acte.
        val resumeActe =
            (if (edition) "Acte modifi\u00E9 : " else "Acte enregistr\u00E9 : ") + libelleType(a.type) +
                (if (crees > 0) " ; " + crees.toString() + " personne(s) cr\u00E9\u00E9e(s)" else "")
        message =
            (if (refusLienTxt.isNotEmpty()) refusLienTxt + " L'acte reste enregistr\u00E9, SANS le lien de parent\u00E9. " else "") +
            refusParentTxt +
            identiteTxt +
            (if (retourLiens.isNotEmpty()) retourLiens.trim() + " " else "") +
            resumeActe
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
                // v0.81 : la reconstruction des 3 JSON tourne HORS du thread
                // principal (avec un instantane des listes, aucune mutation
                // concurrente) : le premier ecran s'affiche sans attendre,
                // la preparation du journal suit des que le calcul est fini.
                try {
                    val instantaneP = personnes.toList()
                    val instantaneJ = pistes.toList()
                    val instantaneA = actes.toList()
                    val (chaineP, chaineJ, chaineA) = withContext(Dispatchers.Default) {
                        val tp = org.json.JSONArray(); instantaneP.forEach { tp.put(it.versJson()) }
                        val tj = org.json.JSONArray(); instantaneJ.forEach { tj.put(it.versJson()) }
                        val ta = org.json.JSONArray(); instantaneA.forEach { ta.put(it.versJson()) }
                        Triple(tp.toString(), tj.toString(), ta.toString())
                    }
                    derniersJson["personnes"] = chaineP
                    derniersJson["journal"] = chaineJ
                    derniersJson["actes"] = chaineA
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
            // GeneaPoche 12 : diagnostic de l'arbre annonce a l'ouverture de
            // l'application (anomalies + doublons + branches isolees).
            val nbDiag = anomaliesPour(personnes).size +
                doublonsPotentiels(personnes, doublonsGardes).size +
                personnes.filter { brancheIsolee(it, personnes) }.size
            if (nbDiag > 0) {
                message = "Diagnostic de l'arbre : " + nbDiag.toString() +
                    " point(s) \u00E0 examiner (onglet Journal)."
            }
            // G\u00E9n\u00E9aPoche 11 (11.1) : rappels de recherches \u00E0 faire,
            // annonc\u00E9s \u00E0 l'ouverture comme le diagnostic (r\u00E8gle des 75 ans).
            val limiteAlertes = anneeCourante() - 75
            val nbRech = pistes.count { ps ->
                !ps.faite && (anneeCiblePiste(ps) == null || anneeCiblePiste(ps)!! <= limiteAlertes)
            }
            if (nbRech > 0) {
                val alerte = "Rappels : " + nbRech.toString() +
                    " recherche(s) prioritaire(s) \u00E0 faire (onglet Journal)."
                message = if (nbDiag > 0) message + " " + alerte else alerte
            }
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
                annulationPossible = modele.annulation != null,
                // v0.78 : paires de doublons "gardees les deux".
                doublonsGardes = doublonsGardes
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
                // G\u00E9n\u00E9aPoche 3 : \u00E9v\u00E9nements libres de la chronologie.
                majEvenementLibre = { pers, ev -> modifierEvenementLibre(pers, ev) },
                supprimerEvenementLibre = { pers, idEv -> supprimerEvenementLibre(pers, idEv) },
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
                exporterGedcom = { uri, sel, ev, fmt -> exporterGedcom(uri, sel, ev, fmt) },
                importerGedcom = { uri -> lireGedcom(uri) },
                viderBase = { viderBase() },
                enregistrerActe = { a -> enregistrerActe(a) },
                sauvegarderBase = { uri, ph -> sauvegarderBase(uri, ph) },
                restaurerSauvegarde = { uri, ph -> restaurerSauvegardeBase(uri, ph) },
                restaurerImport = { restaurerSauvegardeImport() },
                restaurerSecours = { restaurerSecoursBak() },
                picker = { actif -> verrou.pickerOuvert = actif },
                changerPin = { dialoguePin = true },
                reglagesVerrouillage = { dialogueDelai = true },
                reglagesTheme = { dialogueTheme = true },
                prochainId = { Stockage.prochainId(ctx) },
                validerInfo = { pers, section, valide -> validerInfo(pers, section, valide) },
                // G\u00E9n\u00E9aPoche 11 : coche de certification + fusion guid\u00E9e.
                validerIdentite = { pers, etat -> validerIdentite(pers, etat) },
                fusionner = { gardeId, supprimeId -> fusionnerPersonnes(gardeId, supprimeId) },
                // v0.78 : garder les deux fiches d'une paire (definitif).
                garderDoublon = { a, b ->
                    Stockage.garderDoublon(ctx, a, b)
                    doublonsGardes = doublonsGardes + cleDoublon(a, b)
                },
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
                                    .heightIn(min = 32.dp)
                                    .clickable { cujusEdite = cand }
                                    .background(
                                        if (idDuCujus() == cand.id) CouleurVertPale
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                // v0.79 : identifiant unique (titre + N. + D.), compact.
                                Text(
                                    libelleUnique(cand),
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
                        "Importer ce fichier : REMPLACER toute la base (" + personnes.size.toString() + " personnes) par les " + importees.size.toString() + " personnes du fichier, ou FUSIONNER sans aucun doublon (les fiches existantes sont compl\u00E9t\u00E9es, jamais dupliqu\u00E9es). Une sauvegarde horodat\u00E9e sera cr\u00E9\u00E9e avant le remplacement (les 5 derni\u00E8res sont conserv\u00E9es et restaurables).",
                        style = MaterialTheme.typography.bodySmall
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
                Row {
                    TextButton(onClick = {
                        fusionnerBase(importees)
                        importConfirme = null
                    }) { Text("Fusionner", color = CouleurVertFonce, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = {
                        remplacerBase(importees)
                        importConfirme = null
                    }) { Text("Remplacer", color = CouleurRouge, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = {
                TextButton(onClick = { importConfirme = null }) { Text("Annuler") }
            }
        )
    }
}







