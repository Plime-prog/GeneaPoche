package com.plime.geneapoche

import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------- Modele Personne
// Convention : id = 0 signifie "non renseigne" pour les references (pere, mere, conjoint).
// Sosa : numerotation Sosa-Stradonitz (de cujus = 1, pere = 2n, mere = 2n+1).
// Lateral : numerotation d'Aboville pour les descendants hors ligne directe (ex "1.3").
// Etat : "" (neutre), "vert" (verifie), "rouge" (a verifier) par section de la fiche.

data class Personne(
    val id: Long,
    var sosa: String = "",
    var lateral: String = "",
    var nom: String = "",
    var prenoms: String = "",
    var sexe: String = "",
    var profession: String = "",
    var dateNaissance: String = "",
    var lieuNaissance: String = "",
    var dateMariage: String = "",
    var lieuMariage: String = "",
    var dateDeces: String = "",
    var lieuDeces: String = "",
    var etatIdentite: String = "",
    var etatNaissance: String = "",
    var etatMariage: String = "",
    var etatDeces: String = "",
    var pere: Long = 0L,
    var mere: Long = 0L,
    var conjoint: Long = 0L,
    var enfants: MutableList<Long> = mutableListOf()
) {
    fun titre(): String {
        return listOf(prenoms.trim(), nom.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    fun numeroAffiche(): String {
        val s = sosa.trim()
        if (s.isNotEmpty()) return "Sosa " + s
        val l = lateral.trim()
        if (l.isNotEmpty()) return "Lat\u00E9ral " + l
        return ""
    }

    fun aVerifier(): Boolean {
        return etatIdentite == "rouge" || etatNaissance == "rouge" ||
            etatMariage == "rouge" || etatDeces == "rouge"
    }

    fun etatDe(section: String): String = when (section) {
        "identite" -> etatIdentite
        "naissance" -> etatNaissance
        "mariage" -> etatMariage
        else -> etatDeces
    }

    fun changerEtat(section: String, valeur: String) {
        when (section) {
            "identite" -> etatIdentite = valeur
            "naissance" -> etatNaissance = valeur
            "mariage" -> etatMariage = valeur
            else -> etatDeces = valeur
        }
    }

    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("sosa", sosa)
        o.put("lateral", lateral)
        o.put("nom", nom)
        o.put("prenoms", prenoms)
        o.put("sexe", sexe)
        o.put("profession", profession)
        o.put("dateNaissance", dateNaissance)
        o.put("lieuNaissance", lieuNaissance)
        o.put("dateMariage", dateMariage)
        o.put("lieuMariage", lieuMariage)
        o.put("dateDeces", dateDeces)
        o.put("lieuDeces", lieuDeces)
        o.put("etatIdentite", etatIdentite)
        o.put("etatNaissance", etatNaissance)
        o.put("etatMariage", etatMariage)
        o.put("etatDeces", etatDeces)
        o.put("pere", pere)
        o.put("mere", mere)
        o.put("conjoint", conjoint)
        val tableau = JSONArray()
        enfants.forEach { tableau.put(it) }
        o.put("enfants", tableau)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Personne {
            // Migration depuis la v0.1 : anciens champs texte libre naissance / deces
            val ancienneNaissance = o.optString("naissance", "")
            val ancienDeces = o.optString("deces", "")
            return Personne(
                id = o.optLong("id", 0L),
                sosa = o.optString("sosa", ""),
                lateral = o.optString("lateral", ""),
                nom = o.optString("nom", ""),
                prenoms = o.optString("prenoms", o.optString("prenom", "")),
                sexe = o.optString("sexe", ""),
                profession = o.optString("profession", ""),
                dateNaissance = o.optString("dateNaissance", ""),
                lieuNaissance = o.optString("lieuNaissance", ancienneNaissance),
                dateMariage = o.optString("dateMariage", ""),
                lieuMariage = o.optString("lieuMariage", ""),
                dateDeces = o.optString("dateDeces", ""),
                lieuDeces = o.optString("lieuDeces", ancienDeces),
                etatIdentite = o.optString("etatIdentite", ""),
                etatNaissance = o.optString("etatNaissance", ""),
                etatMariage = o.optString("etatMariage", ""),
                etatDeces = o.optString("etatDeces", ""),
                pere = o.optLong("pere", 0L),
                mere = o.optLong("mere", 0L),
                conjoint = o.optLong("conjoint", 0L),
                enfants = mutableListOf<Long>().apply {
                    val tableau = o.optJSONArray("enfants")
                    if (tableau != null) {
                        for (i in 0 until tableau.length()) {
                            add(tableau.optLong(i, 0L))
                        }
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------- Modele Piste (journal de recherches)

data class Piste(
    val id: Long,
    var texte: String = "",
    var personneId: Long = 0L,
    var creation: Long = 0L,
    var faite: Boolean = false,
    var cloture: Long = 0L
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("texte", texte)
        o.put("personneId", personneId)
        o.put("creation", creation)
        o.put("faite", faite)
        o.put("cloture", cloture)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Piste = Piste(
            id = o.optLong("id", 0L),
            texte = o.optString("texte", ""),
            personneId = o.optLong("personneId", 0L),
            creation = o.optLong("creation", 0L),
            faite = o.optBoolean("faite", false),
            cloture = o.optLong("cloture", 0L)
        )
    }
}

// ---------------------------------------------------------------- Modele Acte (image chiffree liee a une personne)

data class Acte(
    val id: Long,
    var personneId: Long = 0L,
    var type: String = "N",
    var titre: String = "",
    var creation: Long = 0L
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("personneId", personneId)
        o.put("type", type)
        o.put("titre", titre)
        o.put("creation", creation)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Acte = Acte(
            id = o.optLong("id", 0L),
            personneId = o.optLong("personneId", 0L),
            type = o.optString("type", "N"),
            titre = o.optString("titre", ""),
            creation = o.optLong("creation", 0L)
        )
    }
}
