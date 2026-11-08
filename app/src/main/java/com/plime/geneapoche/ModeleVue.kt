package com.plime.geneapoche

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// ---------------------------------------------------------------- Modele de vue
// G2-07 : l'ETAT de l'application vit ICI, pas dans des remember() de
// l'ecran : le modele de vue SURVIT a la rotation et au changement de
// theme (les donnees, la fiche ouverte et la pile de navigation ne sont
// pas perdues). Au verrouillage, AppEcran vide ces memes listes (G1-14).

// G3-19 : instantane des liens d'une personne avant sa suppression,
// pour pouvoir annuler (Snackbar "Annuler").
class LienAvant(
    val id: Long,
    val pere: Long,
    val mere: Long,
    val conjoint: Long,
    val conjointsSupp: List<Long>,
    val enfants: List<Long>
)

// G3-19 : tout ce que la suppression d'une personne a retire, pour
// pouvoir la restaurer integralement (personne, liens, pistes ; depuis
// la v0.83 les actes ne sont plus stockes dans l'application).
class AnnulationSuppression(
    val personne: Personne,
    val pistes: List<Piste>,
    val liens: List<LienAvant>
)

class ModeleVueGeneaPoche : ViewModel() {
    val personnes = mutableStateListOf<Personne>()
    val pistes = mutableStateListOf<Piste>()
    // Pile de navigation des fiches : retour = fiche precedemment ouverte
    // (la liste n'apparait qu'une fois la pile vide).
    val pileFiches = mutableStateListOf<Long>()
    // Reponse utilisateur (la croix ne fonctionnait pas) : une recherche
    // supprimee manuellement NE REVIENT PAS ; sa cle (personne|categorie)
    // est ignoree jusqu a ce que la donnee redevienne manquante.
    val categoriesIgnorees = mutableStateListOf<String>()
    var charge by mutableStateOf(false)
    var message by mutableStateOf("")
    var idOuvert by mutableStateOf(0L)
    var onglet by mutableStateOf(1)
    var editionOuverte by mutableStateOf(false)
    var editionCible by mutableStateOf<Personne?>(null)
    var pickerRole by mutableStateOf("")
    var confirmSuppression by mutableStateOf<Personne?>(null)
    var acteAConfirmer by mutableStateOf<ActeSaisie?>(null)
    var arbreCentre by mutableStateOf(0L)
    var sosaCujus by mutableStateOf(1L)
    var cujusDialogue by mutableStateOf(false)
    var cujusPreselec by mutableStateOf<Personne?>(null)
    // G2-04 : ce que l import GEDCOM a ignore (affiche avant remplacement).
    var importConfirme by mutableStateOf<List<Personne>?>(null)
    var importRapport by mutableStateOf(listOf<String>())
    var cujusEdite by mutableStateOf<Personne?>(null)
    var cujasOuvert by mutableStateOf(false)
    // G1-04 : fichiers dont NI le contenu NI la copie .bak ne se lisent :
    // l'enregistrement est bloque jusqu'a restauration (ne rien ecraser).
    var fichiersIllisibles by mutableStateOf(listOf<String>())
    var avertissementLectureFerme by mutableStateOf(false)
    var dialoguePin by mutableStateOf(false)
    var dialogueDelai by mutableStateOf(false)
    // G2-06 : indicateur d'occupation (image, GEDCOM, sauvegarde).
    var occupation by mutableStateOf("")
    // G3-19 : derniere suppression annulable (Snackbar "Annuler").
    var annulation by mutableStateOf<AnnulationSuppression?>(null)
}
