package com.plime.geneapoche

import android.content.Context
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

// v0.111 : boites de dialogue de l'ecran principal, extraites de
// MainActivity.kt (AppEcran). Aucun changement de comportement : chaque
// dialogue recoit son etat et ses actions en parametres ; l'etat reste
// possede par AppEcran.

// G1-15 : changement de code PIN (l'ancien est exige).
@Composable
fun DialogueChangerPin(
    ctx: Context,
    onMessage: (String) -> Unit,
    onFermer: () -> Unit
) {
    var ancienPin by remember { mutableStateOf("") }
    var nouveauPin1 by remember { mutableStateOf("") }
    var nouveauPin2 by remember { mutableStateOf("") }
    AlertDialog(
            onDismissRequest = { onFermer() },
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
                            onMessage("Le nouveau code doit contenir au moins 4 chiffres.")
                        } else if (nouveauPin1 != nouveauPin2) {
                            onMessage("Les deux nouveaux codes ne correspondent pas.")
                        } else if (Stockage.changerPin(ctx, ancienPin, nouveauPin1)) {
                            onMessage("Code PIN chang\u00E9 : utilisez le nouveau code au prochain d\u00E9verrouillage.")
                            onFermer()
                        } else {
                            onMessage("Ancien code incorrect.")
                        }
                    },
                    enabled = ancienPin.isNotEmpty() && nouveauPin1.isNotEmpty() && nouveauPin2.isNotEmpty()
                ) { Text("Changer") }
            },
            dismissButton = {
                TextButton(onClick = { onFermer() }) { Text("Annuler") }
            }
        )
}

// G1-14 : delai de reverrouillage reglable.
@Composable
fun DialogueDelaiVerrouillage(
    ctx: Context,
    onMessage: (String) -> Unit,
    onFermer: () -> Unit
) {
    AlertDialog(
            onDismissRequest = { onFermer() },
            title = { Text("D\u00E9lai de verrouillage") },
            text = {
                Column {
                    Text("Verrouillage apr\u00E8s un passage en arri\u00E8re-plan ou l'extinction de l'\u00E9cran :", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { Stockage.definirDelaiVerrouillage(ctx, 0L); onFermer(); onMessage("Verrouillage imm\u00E9diat.") }, Modifier.fillMaxWidth()) { Text("Imm\u00E9diat (par d\u00E9faut)") }
                    TextButton(onClick = { Stockage.definirDelaiVerrouillage(ctx, 60_000L); onFermer(); onMessage("Verrouillage apr\u00E8s 1 minute.") }, Modifier.fillMaxWidth()) { Text("Apr\u00E8s 1 minute") }
                    TextButton(onClick = { Stockage.definirDelaiVerrouillage(ctx, 300_000L); onFermer(); onMessage("Verrouillage apr\u00E8s 5 minutes.") }, Modifier.fillMaxWidth()) { Text("Apr\u00E8s 5 minutes") }
                }
            },
            confirmButton = {
                TextButton(onClick = { onFermer() }) { Text("Fermer") }
            }
        )
}

// G3-02 : choix du theme, memorise : Systeme (suit le mode sombre du
// telephone), Clair ou Sombre. Applique immediatement.
@Composable
fun DialogueTheme(
    ctx: Context,
    onChoisi: (String) -> Unit,
    onFermer: () -> Unit
) {
    AlertDialog(
            onDismissRequest = { onFermer() },
            title = { Text("Th\u00E8me de l'application") },
            text = {
                Column {
                    Text("Choisissez le th\u00E8me ; le choix est m\u00E9moris\u00E9 :", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        Stockage.definirTheme(ctx, "systeme")
                        onChoisi("systeme")
                        onFermer()
                    }, Modifier.fillMaxWidth()) { Text("Syst\u00E8me (par d\u00E9faut)") }
                    TextButton(onClick = {
                        Stockage.definirTheme(ctx, "clair")
                        onChoisi("clair")
                        onFermer()
                    }, Modifier.fillMaxWidth()) { Text("Clair") }
                    TextButton(onClick = {
                        Stockage.definirTheme(ctx, "sombre")
                        onChoisi("sombre")
                        onFermer()
                    }, Modifier.fillMaxWidth()) { Text("Sombre") }
                }
            },
            confirmButton = {
                TextButton(onClick = { onFermer() }) { Text("Fermer") }
            }
        )
}

// G1-04 : donnees illisibles -> affichage + restauration depuis la copie .bak
@Composable
fun DialogueDonneesIllisibles(
    nombre: Int,
    onRestaurer: () -> Unit,
    onFermer: () -> Unit
) {
    AlertDialog(
            onDismissRequest = { },
            title = { Text("Donn\u00E9es illisibles") },
            text = {
                Text(
                    nombre.toString() + " fichier(s) de donn\u00E9es ne se lisent plus, pas plus que leur copie .bak. L'enregistrement est bloqu\u00E9 pour ne rien \u00E9craser : restaurez la derni\u00E8re copie valide ou fermez sans rien modifier."
                )
            },
            confirmButton = {
                TextButton(onClick = { onRestaurer() }) { Text("Restaurer la copie .bak") }
            },
            dismissButton = {
                TextButton(onClick = { onFermer() }) { Text("Fermer") }
            }
        )
}

// Confirmation : noms saisis inconnus -> creer de nouvelles fiches
// (numerotation deduite automatiquement) ou annuler.
@Composable
fun DialogueNomsInconnus(
    nouveaux: List<String>,
    onCreer: () -> Unit,
    onAnnuler: () -> Unit
) {
    AlertDialog(
            onDismissRequest = { onAnnuler() },
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
                    onCreer()
                }) { Text("Cr\u00E9er les fiches") }
            },
            dismissButton = {
                TextButton(onClick = { onAnnuler() }) { Text("Annuler") }
            }
        )
}

// Confirmation d'un import GEDCOM : remplacer ou fusionner.
@Composable
fun DialogueImportGedcom(
    nbActuel: Int,
    nbImportees: Int,
    rapport: List<String>,
    onFusionner: () -> Unit,
    onRemplacer: () -> Unit,
    onAnnuler: () -> Unit
) {
    AlertDialog(
            onDismissRequest = { onAnnuler() },
            title = { Text("Importer ce GEDCOM ?") },
            text = {
                Column {
                    Text(
                        "Importer ce fichier : REMPLACER toute la base (" + nbActuel.toString() + " personnes) par les " + nbImportees.toString() + " personnes du fichier, ou FUSIONNER sans aucun doublon (les fiches existantes sont compl\u00E9t\u00E9es, jamais dupliqu\u00E9es). Une sauvegarde horodat\u00E9e sera cr\u00E9\u00E9e avant le remplacement (les 5 derni\u00E8res sont conserv\u00E9es et restaurables).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // G2-04 : avertissement listant ce que l import ignore.
                    if (rapport.isNotEmpty()) {
                        Text(
                            "\u00C9l\u00E9ments ignor\u00E9s par l import (aucune perte silencieuse) :",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        rapport.take(6).forEach { ligneIgn ->
                            Text("\u2022 " + ligneIgn, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        onFusionner()
                    }) { Text("Fusionner", color = CouleurVertFonce, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = {
                        onRemplacer()
                    }) { Text("Remplacer", color = CouleurRouge, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = {
                TextButton(onClick = { onAnnuler() }) { Text("Annuler") }
            }
        )
}

// 12.2 : phrase secrete d'un GEDCOM chiffre (.gpged) importe.
@Composable
fun DialogueImportChiffre(
    phrase: String,
    onPhrase: (String) -> Unit,
    onOuvrir: () -> Unit,
    onAnnuler: () -> Unit
) {
    AlertDialog(
            onDismissRequest = { onAnnuler() },
            title = { Text("GEDCOM chiffr\u00E9 (.gpged)") },
            text = {
                Column {
                    Text(
                        "Ce fichier est chiffr\u00E9 : saisissez la phrase secr\u00E8te choisie \u00E0 son export pour l'ouvrir.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = onPhrase,
                        label = { Text("Phrase secr\u00E8te (6 caract\u00E8res min.)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onOuvrir()
                    },
                    enabled = phrase.length >= 6
                ) { Text("Ouvrir") }
            },
            dismissButton = {
                TextButton(onClick = { onAnnuler() }) { Text("Annuler") }
            }
        )
}

// 12.2 : rappel de suppression du dernier fichier exporte/partage,
// a chaque ouverture tant qu'il n'est pas confirme supprime.
@Composable
fun DialogueRappelPartage(
    nomFichier: String,
    dateMs: Long,
    onSupprime: () -> Unit,
    onPlusTard: () -> Unit
) {
    AlertDialog(
                onDismissRequest = { onPlusTard() },
                title = { Text("Fichier export\u00E9") },
                text = {
                    Text(
                        "Le fichier " + nomFichier + " a \u00E9t\u00E9 export\u00E9 le " +
                            java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(dateMs)) +
                            ". Une fois le fichier envoy\u00E9, pensez \u00E0 le SUPPRIMER de votre appareil (T\u00E9l\u00E9chargements, messages, e-mails envoy\u00E9s...) : il contient vos donn\u00E9es.",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onSupprime()
                    }) { Text("Fichier supprim\u00E9", color = CouleurVertFonce, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { onPlusTard() }) { Text("Me le rappeler plus tard") }
                }
            )
}

// Dialogue : choix du de cujus et de son n Sosa (pas forcement 1).
// Toute la numerotation Sosa / d'Aboville en decoule automatiquement.
// onOuverture : executee une fois a l'ouverture (preselection eventuelle).
@Composable
fun DialogueChoixCujus(
    personnes: List<Personne>,
    idCujus: Long,
    onOuverture: () -> Unit,
    onChoisir: (Personne) -> Unit,
    onFermer: () -> Unit
) {
    LaunchedEffect(Unit) { onOuverture() }
    AlertDialog(
                onDismissRequest = { onFermer() },
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
                                        .clickable { onChoisir(cand) }
                                        .background(
                                            if (idCujus == cand.id) CouleurVertPale
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
                    TextButton(onClick = { onFermer() }) { Text("Fermer") }
                }
            )
}

// Edition du n Sosa du de cujus choisi (apercu pur, rien n'est modifie avant
// Enregistrer).
@Composable
fun DialogueEditionCujus(
    cible: Personne,
    sosaCujus: Long,
    onEnregistrer: (Long) -> Unit,
    onAnnuler: () -> Unit
) {
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
            onAnnuler()
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
                        onEnregistrer(numSaisi)
                    },
                    enabled = numValide
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { annulerCujus() }) { Text("Annuler") }
            }
        )
}
