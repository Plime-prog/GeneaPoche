package com.plime.geneapoche

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------ Creation du PIN

// G1-12 : validation du PIN : 6 chiffres minimum, codes triviaux refuses
// (000000, 123456, chiffres identiques), ou phrase secrete alphanumerique
// d'au moins 8 caracteres.
fun erreurPin(p: String, phrase: Boolean): String? {
    if (phrase) {
        if (p.length < 8) return "La phrase secr\u00E8te doit contenir au moins 8 caract\u00E8res."
        return null
    }
    if (p.length < 6) return "Le code doit contenir au moins 6 chiffres."
    if (p.toSet().size == 1) return "Code trop simple : chiffres tous identiques."
    if (p == "012345" || p == "123456" || p == "1234567" || p == "12345678" || p == "654321") {
        return "Code trop simple : suite \u00E9vidente."
    }
    return null
}

@Composable
fun DialogueCreationPin(onCree: (String) -> Unit) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf("") }
    var phraseMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var travail by remember { mutableStateOf(false) }
    fun creer(p: String) {
        val e = erreurPin(p, phraseMode)
        if (e != null) {
            erreur = e
            return
        }
        erreur = ""
        travail = true
        // G1-12 : le PBKDF2 (600 000 iterations) tourne HORS du thread
        // principal, l'ecran ne fige jamais.
        scope.launch(Dispatchers.Default) {
            onCree(p)
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text("Bienvenue dans G\u00E9n\u00E9aPoche", style = MaterialTheme.typography.titleLarge)
            Text("D\u00E9finissez un code PIN pour prot\u00E9ger vos donn\u00E9es.", Modifier.padding(vertical = 8.dp))
            OutlinedTextField(
                value = pin1,
                onValueChange = { if (phraseMode) { if (it.length <= 64 && it.all { c -> c.isLetterOrDigit() }) pin1 = it } else if (it.length <= 8 && it.all { c -> c.isDigit() }) pin1 = it },
                label = { Text(if (phraseMode) "Phrase secr\u00E8te (lettres et chiffres)" else "Code PIN (6 \u00E0 8 chiffres)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = if (phraseMode) KeyboardType.Password else KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin2,
                onValueChange = { if (phraseMode) { if (it.length <= 64 && it.all { c -> c.isLetterOrDigit() }) pin2 = it } else if (it.length <= 8 && it.all { c -> c.isDigit() }) pin2 = it },
                label = { Text("Confirmez") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (phraseMode) KeyboardType.Password else KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (pin1 == pin2) creer(pin1) else erreur = "Les deux codes ne correspondent pas."
                }),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            if (erreur.isNotEmpty()) {
                Text(erreur, color = CouleurRouge, modifier = Modifier.padding(vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite })
            }
            // G1-19 : dire la verite sur la vie privee.
            Text(
                "Vos donn\u00E9es sont chiffr\u00E9es avec une cl\u00E9 li\u00E9e \u00E0 ce code : sans lui, elles sont ind\u00E9chiffrables (aucune r\u00E9cup\u00E9ration possible). " +
                    "La recherche d'archives, elle, passe par votre navigateur et lui transmet la commune, l'ann\u00E9e et le type d'acte.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            TextButton(onClick = { phraseMode = !phraseMode; pin1 = ""; pin2 = ""; erreur = "" }) {
                Text(if (phraseMode) "Utiliser plut\u00F4t un code PIN (6 \u00E0 8 chiffres)" else "Utiliser plut\u00F4t une phrase secr\u00E8te (lettres et chiffres)")
            }
            Button(
                onClick = {
                    if (pin1 != pin2) {
                        erreur = "Les deux codes ne correspondent pas."
                    } else {
                        creer(pin1)
                    }
                },
                enabled = pin1.isNotEmpty() && pin2.isNotEmpty() && !travail
            ) { Text(if (travail) "Chiffrement en cours..." else "Cr\u00E9er le code") }
        }
    }
}

// ------------------------------------------------------------ Ecran de deverrouillage

// G3-12 : pastille de saisie (une par caractere du code entre).
@Composable
fun PastilleSaisie(pleine: Boolean, rouge: Boolean) {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .size(16.dp)
            .clip(CircleShape)
            .background(
                if (pleine) {
                    if (rouge) CouleurRouge else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
    )
}

// G3-12 : pave numerique a l'ecran, retour haptique sur chaque touche.
@Composable
fun PaveNumerique(
    onTouche: (Char) -> Unit,
    onEffacer: () -> Unit,
    onValider: () -> Unit,
    validerActif: Boolean
) {
    val vue = androidx.compose.ui.platform.LocalView.current
    fun tactile() { vue.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) }
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9')
        ).forEach { ligne ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ligne.forEach { chiffre ->
                    OutlinedButton(
                        onClick = { tactile(); onTouche(chiffre) },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(chiffre.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(
                onClick = { tactile(); onEffacer() },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.Clear, contentDescription = "Effacer le dernier caract\u00E8re")
            }
            OutlinedButton(
                onClick = { tactile(); onTouche('0') },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { tactile(); onValider() },
                enabled = validerActif,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "D\u00E9verrouiller")
            }
        }
    }
}

@Composable
fun EcranPin(ctx: Context, onDeverrouille: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf("") }
    var blocage by remember { mutableStateOf(0L) }
    // G3-12 : pave numerique par defaut ; le champ texte reste disponible
    // pour les phrases secretes alphanumeriques (G1-12).
    var modeClavier by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            blocage = Stockage.secondesBlocage(ctx)
            delay(1000L)
        }
    }

    val scope = rememberCoroutineScope()
    var travail by remember { mutableStateOf(false) }

    fun essayer() {
        if (pin.isEmpty() || blocage > 0L || travail) return
        travail = true
        // G1-12 : le PBKDF2 (600 000 iterations) tourne HORS du thread
        // principal. Deverrouillage complet : PIN verifie PUIS cle maitre
        // ouverte (les donnees restent indechiffrables sans le PIN, G1-01).
        scope.launch(Dispatchers.Default) {
            val ok = Stockage.deverrouiller(ctx, pin)
            if (ok) {
                onDeverrouille()
            } else {
                Stockage.enregistrerEchec(ctx)
                blocage = Stockage.secondesBlocage(ctx)
                erreur = "Code incorrect."
            }
            pin = ""
            travail = false
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text("G\u00E9n\u00E9aPoche est verrouill\u00E9", style = MaterialTheme.typography.titleLarge)
            Text("Entrez votre code PIN.", Modifier.padding(vertical = 8.dp))
            if (modeClavier) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 64 && it.all { c -> c.isLetterOrDigit() }) pin = it },
                        label = { Text("Code PIN ou phrase secr\u00E8te") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { essayer() }),
                        isError = erreur.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { essayer() },
                        enabled = pin.isNotEmpty() && blocage == 0L && !travail
                    ) { Text("D\u00E9verrouiller") }
                }
            } else {
                // G3-12 : pastilles de saisie, rouges en cas d'erreur.
                Row(Modifier.padding(vertical = 12.dp)) {
                    repeat(8) { i ->
                        PastilleSaisie(pleine = i < pin.length, rouge = erreur.isNotEmpty())
                    }
                }
                PaveNumerique(
                    onTouche = { c -> if (pin.length < 64) pin += c },
                    onEffacer = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                    onValider = { essayer() },
                    validerActif = pin.isNotEmpty() && blocage == 0L && !travail
                )
            }
            if (erreur.isNotEmpty()) {
                Text(erreur, color = CouleurRouge, modifier = Modifier.padding(vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (blocage > 0L) {
                Text(
                    "Trop d'essais. R\u00E9essayez dans " + blocage.toString() + " s.",
                    color = CouleurRouge,
                    modifier = Modifier.padding(vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            TextButton(onClick = { modeClavier = !modeClavier; erreur = "" }) {
                Text(if (modeClavier) "Afficher le pav\u00E9 num\u00E9rique" else "Saisir une phrase secr\u00E8te au clavier")
            }
        }
    }
}
