package com.plime.geneapoche

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

// Garde-fou de confidentialite : l'application est strictement locale.
class AucuneConnexionTest {
    private fun racine(): File {
        var d: File? = File("").absoluteFile
        while (d != null && !File(d, "src/main/AndroidManifest.xml").exists() &&
            !File(d, "app/src/main/AndroidManifest.xml").exists()) d = d.parentFile
        val base = d ?: error("projet introuvable")
        return if (File(base, "app/src/main").exists()) File(base, "app") else base
    }

    @Test fun manifesteSansPermission() {
        val m = File(racine(), "src/main/AndroidManifest.xml").readText()
        assertFalse("permission Internet interdite", m.contains("android.permission.INTERNET"))
        assertFalse("aucune permission", Regex("<uses-permission(?!-)").containsMatchIn(m.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")))
    }

    @Test fun aucunCodeReseau() {
        val interdits = listOf("WebView", "java.net.URL", "HttpURLConnection", "okhttp", "java.net.Socket", "CookieManager")
        File(racine(), "src/main/java").walkTopDown().filter { it.extension == "kt" }.forEach { f ->
            val code = f.readLines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")
            interdits.forEach { assertFalse(f.name + " utilise " + it, code.contains(it)) }
        }
    }
}
