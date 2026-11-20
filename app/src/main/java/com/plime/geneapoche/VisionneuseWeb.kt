package com.plime.geneapoche

import android.annotation.SuppressLint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- v0.147
// Site Internet dans la zone du haut de la Saisie (registres en ligne, archives
// departementales, arbres publics...), avec zoom, luminosite et contraste comme
// pour une image. Seule fonction de l'application qui utilise Internet : elle
// n'envoie AUCUNE donnee de GenaPoche, ne fait qu'afficher la page demandee
// (https uniquement).

// Adresse saisie -> adresse https exploitable, ou null. Refuse tout autre schema
// (javascript:, file:, content:, http:...) ; "http://" est remonte en https.
fun normaliserUrl(saisie: String): String? {
    val s = saisie.trim()
    if (s.isEmpty() || s.any { it.isWhitespace() }) return null
    val schema = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):").find(s)?.groupValues?.get(1)?.lowercase()
    val complet = when {
        s.startsWith("//") -> "https:" + s
        schema == null -> "https://" + s
        schema == "https" -> s
        schema == "http" && s.lowercase().startsWith("http://") -> "https://" + s.substring(7)
        // "site.fr:8080/x" : un nom d'hote avec port n'est pas un schema
        Regex("^[a-zA-Z0-9.-]+:\\d+(/.*)?$").matches(s) -> "https://" + s
        else -> return null
    }
    val hote = Regex("^https://([^/?#:]+)").find(complet)?.groupValues?.get(1) ?: return null
    if (!hote.contains('.') || hote.startsWith(".") || hote.endsWith(".")) return null
    return complet
}

@Composable
fun DialogueAdresseSite(
    initiale: String,
    onValider: (String) -> Unit,
    onFermer: () -> Unit
) {
    var texte by remember { mutableStateOf(initiale) }
    val url = normaliserUrl(texte)
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Afficher un site Internet") },
        text = {
            Column {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Adresse du site") },
                    placeholder = { Text("exemple : archives.exemple.fr") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (texte.isNotBlank() && url == null) "Adresse non valide (https uniquement)."
                    else "Le site s'affiche dans la zone du haut, avec zoom, lumière et contraste. Cette fonction utilise Internet pour afficher la page ; aucune donnée de GénéaPoche n'est envoyée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (texte.isNotBlank() && url == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { url?.let(onValider) }, enabled = url != null) { Text("Afficher") } },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
}

// Efface cookies et donnees de navigation (rien ne reste apres la fermeture).
fun nettoyerNavigation() {
    try {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    } catch (_: Exception) {
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZoneSite(
    url: String,
    onChangerAdresse: () -> Unit,
    onImage: () -> Unit,
    onFermer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lumiere by remember { mutableStateOf(1f) }
    var contraste by remember { mutableStateOf(1f) }
    var texte by remember { mutableStateOf(1f) }
    var reglages by remember { mutableStateOf(false) }
    Box(modifier.background(androidx.compose.ui.graphics.Color.White)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webViewClient = object : WebViewClient() {
                        // Navigation limitee a https : tout autre schema est refuse.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            request.url.scheme?.lowercase() != "https"
                    }
                    loadUrl(url)
                }
            },
            update = { w ->
                w.settings.textZoom = (texte * 100f).toInt()
                val s = lumiere * contraste
                val decalage = 255f * (0.5f - 0.5f * contraste)
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(
                        ColorMatrix(
                            floatArrayOf(
                                s, 0f, 0f, 0f, decalage,
                                0f, s, 0f, 0f, decalage,
                                0f, 0f, s, 0f, decalage,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    )
                }
                w.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                if (w.url == null) w.loadUrl(url)
            },
            onRelease = { w ->
                w.stopLoading()
                w.destroy()
                nettoyerNavigation()
            }
        )
        Row(Modifier.align(Alignment.TopEnd).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                "Adresse" to onChangerAdresse,
                (if (reglages) "Fin réglages" else "Réglages") to { reglages = !reglages },
                "Image" to onImage,
                "← Fermer" to onFermer
            ).forEach { (l, a) ->
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                    TextButton(onClick = a) { Text(l, fontSize = 11.sp, maxLines = 1) }
                }
            }
        }
        if (reglages) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("Taille du texte", fontSize = 10.sp)
                Slider(value = texte, onValueChange = { texte = it }, valueRange = 0.5f..2.5f)
                Text("Lumière", fontSize = 10.sp)
                Slider(value = lumiere, onValueChange = { lumiere = it }, valueRange = 0.5f..2f)
                Text("Contraste", fontSize = 10.sp)
                Slider(value = contraste, onValueChange = { contraste = it }, valueRange = 0.5f..2f)
            }
        }
    }
}
