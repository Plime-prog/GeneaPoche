undefined

// ---------------------------------------------------------------- Dialogue : nouvelle piste manuelle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialoguePiste(
    personnes: List<Personne>,
    personnePredefinie: Long,
    onValider: (String, Long) -> Unit,
    onAnnuler: () -> Unit
) {
    var texte by remember { mutableStateOf("") }
    var personneId by remember { mutableStateOf(personnePredefinie) }
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text("Nouvelle recherche") },
        text = {
            Column {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Que faut-il rechercher ?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Personne concern\u00E9e :", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp)) {
                    TextButton(onClick = { personneId = 0L }) {
                        Text(if (personneId == 0L) "Aucune (g\u00E9n\u00E9rale)" else "Aucune (g\u00E9n\u00E9rale)")
                    }
                    personnes.sortedBy { it.titre() }.forEach { p ->
                        TextButton(onClick = { personneId = p.id }) {
                            Text(if (personneId == p.id) { " \u25CF " + p.titre() } else { "   " + p.titre() })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(texte.trim(), personneId) },
                enabled = texte.isNotBlank()
            ) { Text("Ajouter") }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}
