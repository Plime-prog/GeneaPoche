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
    val sources = modele.sources
    val citations = modele.citations
    val pileFiches = modele.pileFiches
    // v0.78 : paires de doublons "gardees les deux" (decision definitive,
    // persistee) : elles restent hors du diagnostic de l'arbre.
    var doublonsGardes by remember { mutableStateOf(Stockage.lireDoublonsGardes(ctx)) }
    // v0.88 : anomalies a verifier que l'utilisateur a choisi d'ignorer.
    var anomaliesIgnorees by remember { mutableStateOf(Stockage.lireAnomaliesIgnorees(ctx)) }
    // G\u00E9n\u00E9aPoche 12 (12.1) : sauvegarde automatique .gpbak dans un
    // dossier choisi apr\u00E8s N modifications + cl\u00E9 de r\u00E9cup\u00E9ration.
    var dossierSauvegarde by remember { mutableStateOf(Stockage.lireDossierSauvegarde(ctx)) }
    var seuilSauvegarde by remember { mutableStateOf(Stockage.lireSeuilSauvegarde(ctx)) }
    var cleRecuperation by remember { mutableStateOf("") }
    // G\u00E9n\u00E9aPoche 12 (12.2) : import d'un GEDCOM chiffr\u00E9 (.gpged) et
    // rappel de suppression du dernier fichier export\u00E9/partag\u00E9.
    var importChiffreUri by remember { mutableStateOf<Uri?>(null) }
    var phraseImportChiffre by remember { mutableStateOf("") }
    var rappelPartageFerme by remember { mutableStateOf(false) }
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
        val cs = sources.toList()
        sources.clear()
        sources.addAll(cs)
        val cc = citations.toList()
        citations.clear()
        citations.addAll(cc)
    }

    // v0.90 : remplace le carnet de sources par le contenu lu au stockage.
    fun appliquerCatalogue(rs: Stockage.ResultatLecture<Any>) {
        sources.clear()
        citations.clear()
        rs.liste.forEach { e ->
            if (e is Source) sources.add(e) else if (e is Citation) citations.add(e)
        }
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
                suggestionsPour(pers, personnes).any { it.cle == ps.categorie }
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
            suggestionsPour(p, personnes).forEach { s ->
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

    // G\u00E9n\u00E9aPoche 12 (12.1) : sauvegarde automatique. La copie .gpbak
    // est chiffr\u00E9e par la CL\u00C9 DE R\u00C9CUP\u00C9RATION g\u00E9n\u00E9r\u00E9e par
    // l'application (20 caract\u00E8res sans caract\u00E8res ambigus), affich\u00E9e
    // une fois \u00E0 l'activation et \u00E0 imprimer hors de l'appareil.
    fun genererCleRecuperation(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val alea = java.security.SecureRandom()
        return (0 until 4).map {
            (0 until 5).map { alphabet[alea.nextInt(alphabet.length)] }.joinToString("")
        }.joinToString("-")
    }

    // Copie .gpbak automatique dans le dossier choisi (DocumentsContract :
    // l'URI du dossier b\u00E9n\u00E9ficie d'une permission persistable, aucune
    // autorisation manifeste). Lanc\u00E9e \u00E0 l'activation et au franchissement
    // du seuil de N modifications.
    fun sauvegardeAuto() {
        val dossierTxt = dossierSauvegarde
        if (dossierTxt.isEmpty()) return
        val phrase = Stockage.lirePhraseSauvegarde(ctx)
        if (phrase.isEmpty()) return
        val persC = personnes.toList()
        val pisC = pistes.toList()
        val srcC = sources.toList()
        val citC = citations.toList()
        occupation = "Sauvegarde automatique..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                val arbre = Uri.parse(dossierTxt)
                val horodatage = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-Hmm"))
                val uri = android.provider.DocumentsContract.createDocument(
                    ctx.contentResolver, arbre, "application/octet-stream",
                    "GeneaPoche-sauvegarde-" + horodatage + ".gpbak"
                )
                var ok = false
                if (uri != null) {
                    ok = Sauvegarde.exporter(ctx, uri, phrase, persC, pisC, srcC, citC)
                }
                withContext(Dispatchers.Main) {
                    Stockage.razCompteurSauvegarde(ctx)
                    message = if (ok)
                        "Sauvegarde automatique cr\u00E9\u00E9e dans le dossier choisi (restauration : cl\u00E9 de r\u00E9cup\u00E9ration)."
                    else
                        "\u00C9chec de la sauvegarde automatique (dossier inaccessible ?)."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { message = messageErreur(e, "\u00C9chec de la sauvegarde automatique.") }
            } finally {
                withContext(Dispatchers.Main) { occupation = "" }
            }
        }
    }

    // Activation depuis le dialogue : permission persistable sur le dossier,
    // g\u00E9n\u00E9ration de la cl\u00E9 de r\u00E9cup\u00E9ration si elle n'existe pas encore,
    // puis PREMI\u00C8RE sauvegarde imm\u00E9diate (l'utilisateur voit que \u00E7a marche).
    fun activerSauvegardeAuto(uri: Uri) {
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Permission d\u00E9j\u00E0 prise ou non persistable : on continue.
        }
        Stockage.ecrireDossierSauvegarde(ctx, uri.toString())
        dossierSauvegarde = uri.toString()
        var phrase = Stockage.lirePhraseSauvegarde(ctx)
        if (phrase.isEmpty()) {
            phrase = genererCleRecuperation()
            if (!Stockage.ecrirePhraseSauvegarde(ctx, phrase)) {
                message = "\u00C9chec de la cr\u00E9ation de la cl\u00E9 de r\u00E9cup\u00E9ration."
                return
            }
            cleRecuperation = phrase
            message = "Sauvegarde automatique activ\u00E9e : IMPRIMEZ la cl\u00E9 de r\u00E9cup\u00E9ration affich\u00E9e."
        }
        Stockage.razCompteurSauvegarde(ctx)
        sauvegardeAuto()
    }

    fun desactiverSauvegardeAuto() {
        Stockage.effacerDossierSauvegarde(ctx)
        dossierSauvegarde = ""
        cleRecuperation = ""
        message = "Sauvegarde automatique d\u00E9sactiv\u00E9e (la cl\u00E9 de r\u00E9cup\u00E9ration reste valable pour les fichiers d\u00E9j\u00E0 cr\u00E9\u00E9s)."
    }

    fun reglerSeuilSauvegarde(n: Int) {
        val v = n.coerceIn(5, 500)
        Stockage.ecrireSeuilSauvegarde(ctx, v)
        seuilSauvegarde = v
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
        // 12.1 : vrai seulement si un fichier a REELLEMENT ete reecrit
        // (le compteur de modifications ne compte pas les coches sans ecriture).
        var ecritFichier = false
        fun ecrireSiChange(cle: String, texte: String?, ecrire: (String) -> Boolean): Boolean {
            if (texte == null) return false
            if (derniersJson[cle] == texte) return true
            val ok = ecrire(texte)
            if (ok) {
                derniersJson[cle] = texte
                ecritFichier = true
            }
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
        // v0.90 : carnet de sources ; les citations dont la personne ou la
        // source n'existe plus sont purgees a l'ecriture.
        val ok3 = ecrireSiChange(
            "sources",
            try {
                catalogueVersTexte(
                    sources.toList(),
                    citationsValides(citations.toList(), sources.toList(), personnes.toList())
                )
            } catch (e: Exception) {
                null
            }
        ) { Stockage.sauverTexteSources(ctx, it) }
        rafraichirTout()
        // G2-05 : message distinct selon le fichier en echec.
        message = when {
            ok1 && ok2 && ok3 -> ""
            !ok1 -> "\u00C9chec de l'enregistrement des personnes."
            !ok2 -> "\u00C9chec de l'enregistrement du journal."
            !ok3 -> "\u00C9chec de l'enregistrement des sources."
            else -> ""
        }
        // 12.1 : compteur de modifications -> sauvegarde automatique .gpbak
        // dans le dossier choisi, ou simple rappel si aucun dossier.
        if (ok1 && ok2 && ok3 && ecritFichier) {
            Stockage.incrementerCompteurSauvegarde(ctx)
            if (Stockage.lireCompteurSauvegarde(ctx) >= Stockage.lireSeuilSauvegarde(ctx)) {
                if (dossierSauvegarde.isNotEmpty()) {
                    sauvegardeAuto()
                } else {
                    Stockage.razCompteurSauvegarde(ctx)
                    message = "Modifications enregistr\u00E9es : pensez \u00E0 faire une sauvegarde chiffr\u00E9e (.gpbak, param\u00E9trages)."
                }
            }
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
            pistes.maxOfOrNull { it.id } ?: 0L,
            maxIdCatalogue(sources.toList(), citations.toList())
        )
        Stockage.alignerCompteur(ctx, maxId)
        var rapports = verifierLiens(personnes)
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
        val rp = Stockage.charger(ctx)
        val rj = Stockage.chargerJournal(ctx)
        val rs = Stockage.chargerSources(ctx)
        personnes.addAll(rp.liste)
        pistes.addAll(rj.liste)
        appliquerCatalogue(rs)
        fichiersIllisibles = listOf(rp, rj, rs).filter { it.illisible }.map { it.nom }
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
        // v0.90 : les citations de la personne partent avec elle (Annuler les rend).
        val citationsRetirees = citations.filter { it.personneId == p.id }.toList()
        modele.annulation = AnnulationSuppression(
            personne = p,
            pistes = pistesRetirees,
            liens = liensAvant,
            citations = citationsRetirees
        )
        citations.removeAll(citationsRetirees)
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
        pistes.addAll(a.pistes)
        citations.addAll(a.citations)
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
        // G5 : seul le bloc d'evenement a change : numerotation et journal
        // limites a cette personne.
        enregistrer(false, setOf(p.id))
    }

    // Chantier 12.0 : coche "Enregistr\u00E9" par type d'acte sur la fiche
    // (l'acte est conserv\u00E9 ailleurs, plus rien n'est stock\u00E9 dans l'app).
    fun basculerEnregistre(type: String) {
        val p = personneParId(personnes, idOuvert) ?: return
        p.definirEnregistre(type, !p.enregistreDe(type))
        // Miroir : la mari\u00E9e porte la m\u00EAme coche de mariage.
        if (type == "M") {
            personneParId(personnes, p.conjoint)?.let { q -> q.definirEnregistre("M", p.enregistreDe("M")) }
        }
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
        // 5. Recherches du doublon : rattachees a garde.
        pistes.filter { it.personneId == supprimeId }.forEach { it.personneId = gardeId }
        // v0.90 : les citations (sources) du doublon passent a la fiche conservee.
        val citationsApresFusion = citations.toMutableList()
        transfererCitations(citationsApresFusion, supprimeId, gardeId)
        citations.clear()
        citations.addAll(citationsApresFusion)
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

    // v0.90 : correction en un clic d'un lieu d'ancienne saisie depuis le
    // journal : la valeur normalis\u00E9e "Commune, code INSEE, d\u00E9partement"
    // remplace l'ancienne, et le champ "D\u00E9partement" suit le code INSEE.
    fun corrigerLieu(personneId: Long, champ: String, valeur: String) {
        val p = personnes.firstOrNull { it.id == personneId } ?: return
        val code = Regex("(\\d{5}|2[AB]\\d{3})").find(valeur)?.value ?: ""
        val num = if (code.isNotEmpty()) code.take(2) else ""
        when (champ) {
            "naissance" -> { p.lieuNaissance = valeur; p.deptNaissance = num }
            "bapteme" -> { p.lieuBapteme = valeur; p.deptBapteme = num }
            "mariage" -> { p.lieuMariage = valeur; p.deptMariage = num }
            "deces" -> { p.lieuDeces = valeur; p.deptDeces = num }
            "sepulture" -> { p.lieuSepulture = valeur; p.deptSepulture = num }
            else -> if (champ.startsWith("evl:")) {
                val idEv = champ.removePrefix("evl:").toLongOrNull()
                p.evenementsLibres.firstOrNull { it.id == idEv }?.let { ev ->
                    ev.lieu = valeur; ev.dept = num
                }
            }
        }
        enregistrer(false, setOf(personneId))
        message = "Lieu normalis\u00E9."
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

    fun exporterGedcom(uri: Uri, sousEnsemble: Set<Long>?, exclureVivants: Boolean = false, format: String = "5.5.1", phrase: String = "") {
        // G2-06 : export HORS du thread principal, indicateur a l'ecran.
        occupation = "Export GEDCOM en cours..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
                // G1-16 : option exclure les personnes vivantes (sans deces
                // connu, nees il y a moins de 100 ans).
                val annee = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val exportables = if (exclureVivants) personnes.filter { !estVivante(it, annee) } else personnes
                val sous = if (exclureVivants && sousEnsemble != null) sousEnsemble.filter { id -> exportables.any { it.id == id } }.toSet() else sousEnsemble
                val texte = construireGedcom(exportables, sous, format, sources.toList(), citations.toList())
                val n = if (sous == null) exportables.size else texte.split("\n").count { it.endsWith(" INDI") }
                // v0.41 : JAMAIS de fichier GEDCOM vide (base vide, branche
                // vide ou vivants exclus qui retirent tout) : export refuse.
                if (n == 0) {
                    withContext(Dispatchers.Main) { message = "Aucune personne \u00E0 exporter : export refus\u00E9, aucun fichier cr\u00E9\u00E9 (base vide, ou liste effac\u00E9e de la m\u00E9moire par un verrouillage pendant le choix du fichier : d\u00E9verrouillez puis recommencez)." }
                    return@launch
                }
                // 12.2 : export chiffr\u00E9 (.gpged) si une phrase secr\u00E8te est
                // fournie ; le fichier ne s'ouvre que dans GeneaPoche.
                val octetsSortie = if (phrase.isNotEmpty()) {
                    Sauvegarde.chiffrerOctets(phrase, texte.toByteArray(Charsets.UTF_8), Sauvegarde.MAGIQUE_GPGED)
                } else {
                    texte.toByteArray(Charsets.UTF_8)
                }
                if (octetsSortie == null) {
                    withContext(Dispatchers.Main) { message = "\u00C9chec du chiffrement : export annul\u00E9, aucun fichier cr\u00E9\u00E9." }
                    return@launch
                }
                ctx.contentResolver.openOutputStream(uri)?.use { sortie ->
                    sortie.write(octetsSortie)
                }
                // v0.39 : avertissement si le fichier contient moins de
                // personnes que la base (branche, vivants exclus...).
                val alerte = if (n < personnes.size) " ATTENTION : seulement " + n.toString() + " sur " + personnes.size.toString() + " personnes de la base." else ""
                // 12.2 : rappel de suppression du fichier partag\u00E9 (GEDCOM
                // clair ou .gpged chiffr\u00E9).
                Stockage.noterFichierPartage(ctx, uri.lastPathSegment?.substringAfterLast('/') ?: "fichier export\u00E9")
                // R1 : etat Compose JAMAIS modifie depuis le thread IO.
                withContext(Dispatchers.Main) {
                    message = if (phrase.isNotEmpty())
                        "GEDCOM chiffr\u00E9 export\u00E9 (" + n.toString() + " personnes) : fichier .gpged ouvrable uniquement dans GeneaPoche (Importer un GEDCOM) avec la phrase secr\u00E8te. Une fois le fichier envoy\u00E9, pensez \u00E0 le supprimer."
                    else
                        "GEDCOM export\u00E9 (" + n.toString() + " personnes) : importable dans Heredis." + (if (exclureVivants) " Personnes vivantes exclues." else "") + alerte + " Une fois le fichier envoy\u00E9, pensez \u00E0 le supprimer."
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
                // 12.2 : export GEDCOM chiffr\u00E9 (.gpged) : la phrase secr\u00E8te
                // est demand\u00E9e avant l'analyse du contenu.
                if (Sauvegarde.estGpged(octets)) {
                    withContext(Dispatchers.Main) { importChiffreUri = uri }
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
                    modele.importSources = resultat.sources
                    modele.importCitations = resultat.citations
                }
            } catch (e: Exception) {
                // G2-05 : message distinct selon le type d'echec.
                withContext(Dispatchers.Main) { message = messageErreur(e, "\u00C9chec de la lecture du GEDCOM.") }
            } finally {
                withContext(Dispatchers.Main) { occupation = "" }
            }
        }
    }

    // 12.2 : ouverture d'un GEDCOM chiffr\u00E9 (.gpged) avec la phrase secr\u00E8te
    // choisie \u00E0 l'export. Phrase erronee = rien n'est import\u00E9.
    fun lireGedcomChiffre(uri: Uri, phrase: String) {
        if (phrase.isEmpty()) return
        occupation = "Import GEDCOM chiffr\u00E9 en cours..."
        scopeTravail.launch(Dispatchers.IO) {
            try {
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
                if (octets == null || !Sauvegarde.estGpged(octets)) {
                    withContext(Dispatchers.Main) { message = "Fichier .gpged illisible." }
                    return@launch
                }
                val clair = Sauvegarde.dechiffrerOctets(phrase, octets, Sauvegarde.MAGIQUE_GPGED)
                if (clair == null) {
                    withContext(Dispatchers.Main) { message = "Phrase secr\u00E8te erron\u00E9e (ou fichier .gpged invalide) : import annul\u00E9." }
                    return@launch
                }
                val resultat = importerGedcomRapporte(decoderGedcom(clair))
                if (resultat.personnes.isEmpty()) {
                    withContext(Dispatchers.Main) { message = "Aucune personne trouv\u00E9e dans ce GEDCOM." }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    importConfirme = resultat.personnes
                    importRapport = resultat.ignores
                    modele.importSources = resultat.sources
                    modele.importCitations = resultat.citations
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
        // v0.90 : le carnet de sources devient celui du GEDCOM importe
        // (les identifiants de fiches ont change : les anciennes citations
        // ne designeraient plus les bonnes personnes).
        sources.clear()
        sources.addAll(modele.importSources)
        citations.clear()
        citations.addAll(modele.importCitations)
        modele.importSources = emptyList()
        modele.importCitations = emptyList()
        pileFiches.clear()
        // De cujus apres import : la personne au plus petit n Sosa du fichier, sinon la 1re.
        val avecSosa = personnes.filter { it.sosa.trim().toLongOrNull() != null }
        val premier = avecSosa.minByOrNull { it.sosa.trim().toLong() }
            ?: personnes.firstOrNull()
        if (premier != null) {
            if (premier.sosa.isBlank()) {
                renumeroter(personnes, premier.id, 1L)
            }
            sosaCujus = premier.sosa.trim().toLongOrNull() ?: 1L
