package com.plime.geneapoche

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------- Modeles

data class Personne(
    val id: Long,
    var nom: String,
    var prenom: String,
    var naissance: String,
    var deces: String
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("nom", nom)
        o.put("prenom", prenom)
        o.put("naissance", naissance)
        o.put("deces", deces)
        return o
    }

    fun titre(): String {
        val n = nom.trim()
        val p = prenom.trim()
        return listOf(p, n).filter { it.isNotEmpty() }.joinToString(" ")
    }

    companion object {
        fun depuisJson(o: JSONObject): Personne = Personne(
            o.optLong("id", 0L),
            o.optString("nom", ""),
            o.optString("prenom", ""),
            o.optString("naissance", ""),
            o.optString("deces", "")
        )
    }
}

// ---------------------------------------------------------------- Stockage local

object Stockage {
    private const val FICHIER = "personnes.json"

    fun charger(ctx: Context): MutableList<Personne> {
        val liste = mutableListOf<Personne>()
        try {
            val flux = ctx.openFileInput(FICHIER)
            val octets = flux.readBytes()
            flux.close()
            val tableau = JSONArray(String(octets))
            for (i in 0 until tableau.length()) {
                liste.add(Personne.depuisJson(tableau.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // Premier lancement ou fichier illisible : base vide.
        }
        return liste
    }

    fun sauver(ctx: Context, liste: List<Personne>): Boolean {
        return try {
            val tableau = JSONArray()
            liste.forEach { tableau.put(it.versJson()) }
            ctx.openFileOutput(FICHIER, Context.MODE_PRIVATE).use { sortie ->
                sortie.write(tableau.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

// ---------------------------------------------------------------- Activite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppEcran(this)
            }
        }
    }
}

// ---------------------------------------------------------------- Ecran principal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppEcran(ctx: Context) {
    val personnes = remember { mutableStateListOf<Personne>() }
    var charge by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var dialogueOuvert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        personnes.addAll(Stockage.charger(ctx))
        charge = true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("G\u00E9n\u00E9aPoche") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogueOuvert = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter")
            }
        }
    ) { marges ->
        if (!charge) {
            Column(Modifier.fillMaxSize().padding(marges).padding(16.dp)) {
                Text("Chargement...")
            }
        } else if (personnes.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(marges).padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aucune personne pour le moment.")
                Text("Touchez le bouton + pour ajouter votre premi\u00E8re personne.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(marges)) {
                items(personnes) { p ->
                    CartePersonne(
                        p,
                        onSupprimer = {
                            personnes.remove(p)
                            message = if (Stockage.sauver(ctx, personnes)) {
                                "Personne supprim\u00E9e."
                            } else {
                                "Echec de l'enregistrement."
                            }
                        }
                    )
                }
            }
        }
    }

    if (message.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            title = { Text("Information") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { message = "" }) { Text("OK") }
            }
        )
    }

    if (dialogueOuvert) {
        DialoguePersonne(
            onEnregistrer = { nom, prenom, naissance, deces ->
                val nouvelId = (personnes.maxOfOrNull { it.id } ?: 0L) + 1L
                personnes.add(Personne(nouvelId, nom, prenom, naissance, deces))
                message = if (Stockage.sauver(ctx, personnes)) {
                    "Personne enregistr\u00E9e."
                } else {
                    "Echec de l'enregistrement."
                }
                dialogueOuvert = false
            },
            onAnnuler = { dialogueOuvert = false }
        )
    }
}

// ---------------------------------------------------------------- Carte personne

@Composable
fun CartePersonne(p: Personne, onSupprimer: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(p.titre(), style = MaterialTheme.typography.titleMedium)
                if (p.naissance.isNotBlank()) {
                    Text("N\u00E9(e) : " + p.naissance, style = MaterialTheme.typography.bodySmall)
                }
                if (p.deces.isNotBlank()) {
                    Text("D\u00E9c\u00E9d\u00E9(e) : " + p.deces, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onSupprimer) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer")
            }
        }
    }
}

// ---------------------------------------------------------------- Dialogue ajout

@Composable
fun DialoguePersonne(
    onEnregistrer: (String, String, String, String) -> Unit,
    onAnnuler: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var naissance by remember { mutableStateOf("") }
    var deces by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Ajouter une personne") },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                )
                OutlinedTextField(
                    value = prenom,
                    onValueChange = { prenom = it },
                    label = { Text("Pr\u00E9nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                )
                OutlinedTextField(
                    value = naissance,
                    onValueChange = { naissance = it },
                    label = { Text("Naissance (date, lieu)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                )
                OutlinedTextField(
                    value = deces,
                    onValueChange = { deces = it },
                    label = { Text("D\u00E9c\u00E8s (date, lieu)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEnregistrer(nom, prenom, naissance, deces) },
                enabled = (nom.isNotBlank() || prenom.isNotBlank())
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}
