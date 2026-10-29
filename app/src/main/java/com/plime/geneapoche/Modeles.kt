package com.plime.geneapoche

import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------- Modele Personne
// Convention : id = 0 signifie "non renseigne" pour les references (pere, mere, conjoint).
// Sosa : numerotation Sosa-Stradonitz (de cujus = 1, pere = 2n, mere = 2n+1).
// Lateral : numerotation d'Aboville pour les descendants (ex "1.3").
// Etat : "" (neutre), "vert" (verifie), "rouge" (a verifier) par section de la fiche.
// Sections : identite, naissance, bapteme, mariage, deces, sepulture.

// Plafond de l'application : une genealogie de poche, 3 generations + lateraux.
val PlafondPersonnes = 50

data class Personne(
    val id: Long,
    var sosa: String = "",
    var lateral: String = "",
    var nom: String = "",
    var prenoms: String = "",
    var sexe: String = "",
    var profession: String = "",
    var dateNaissance: String = "",
    var heureNaissance: String = "",
    var lieuNaissance: String = "",
    var deptNaissance: String = "",
    var dateBapteme: String = "",
    var heureBapteme: String = "",
    var lieuBapteme: String = "",
    var deptBapteme: String = "",
    var dateMariage: String = "",
    var heureMariage: String = "",
    var lieuMariage: String = "",
    var deptMariage: String = "",
    var dateDeces: String = "",
    var heureDeces: String = "",
    var lieuDeces: String = "",
    var deptDeces: String = "",
    var dateSepulture: String = "",
    var heureSepulture: String = "",
    var lieuSepulture: String = "",
    var deptSepulture: String = "",
    var etatIdentite: String = "",
    var etatNaissance: String = "",
    var etatBapteme: String = "",
    var etatMariage: String = "",
    var etatDeces: String = "",
    var etatSepulture: String = "",
    var pere: Long = 0L,
    var mere: Long = 0L,
    var conjoint: Long = 0L,
    var conjointsSupp: MutableList<Long> = mutableListOf(),
    var enfants: MutableList<Long> = mutableListOf(),
    // Blocs facultatifs crees par l'utilisateur : "bapteme", "sepulture"
    var blocsOuverts: MutableList<String> = mutableListOf()
) {
    fun titre(): String {
        return listOf(nom.trim(), prenoms.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    fun numeroAffiche(): String {
        val s = sosa.trim()
        if (s.isNotEmpty()) return "Sosa " + s
        val l = lateral.trim()
        if (l.isNotEmpty()) return "LatÃ©ral " + l
        return ""
    }

    fun aVerifier(): Boolean {
        return etatIdentite == "rouge" || etatNaissance == "rouge" ||
            etatBapteme == "rouge" || etatMariage == "rouge" ||
            etatDeces == "rouge" || etatSepulture == "rouge"
    }

    fun etatDe(section: String): String = when (section) {
        "identite" -> etatIdentite
        "naissance" -> etatNaissance
        "bapteme" -> etatBapteme
        "mariage" -> etatMariage
        "sepulture" -> etatSepulture
        else -> etatDeces
    }

    fun changerEtat(section: String, valeur: String) {
        when (section) {
            "identite" -> etatIdentite = valeur
            "naissance" -> etatNaissance = valeur
            "bapteme" -> etatBapteme = valeur
            "mariage" -> etatMariage = valeur
            "sepulture" -> etatSepulture = valeur
            else -> etatDeces = valeur
        }
    }

    fun tousConjoints(): List<Long> {
        val res = mutableListOf<Long>()
        if (conjoint != 0L) res.add(conjoint)
        conjointsSupp.forEach { if (it != 0L && !res.contains(it)) res.add(it) }
        return res
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
        o.put("heureNaissance", heureNaissance)
        o.put("lieuNaissance", lieuNaissance)
        o.put("deptNaissance", deptNaissance)
        o.put("dateBapteme", dateBapteme)
        o.put("heureBapteme", heureBapteme)
        o.put("lieuBapteme", lieuBapteme)
        o.put("deptBapteme", deptBapteme)
        o.put("dateMariage", dateMariage)
        o.put("heureMariage", heureMariage)
        o.put("lieuMariage", lieuMariage)
        o.put("deptMariage", deptMariage)
        o.put("dateDeces", dateDeces)
        o.put("heureDeces", heureDeces)
        o.put("lieuDeces", lieuDeces)
        o.put("deptDeces", deptDeces)
        o.put("dateSepulture", dateSepulture)
        o.put("heureSepulture", heureSepulture)
        o.put("lieuSepulture", lieuSepulture)
        o.put("deptSepulture", deptSepulture)
        o.put("etatIdentite", etatIdentite)
        o.put("etatNaissance", etatNaissance)
        o.put("etatBapteme", etatBapteme)
        o.put("etatMariage", etatMariage)
        o.put("etatDeces", etatDeces)
        o.put("etatSepulture", etatSepulture)
        o.put("pere", pere)
        o.put("mere", mere)
        o.put("conjoint", conjoint)
        val supp = JSONArray()
        conjointsSupp.forEach { supp.put(it) }
        o.put("conjointsSupp", supp)
        val tableau = JSONArray()
        enfants.forEach { tableau.put(it) }
        o.put("enfants", tableau)
        val blocs = JSONArray()
        blocsOuverts.forEach { blocs.put(it) }
        o.put("blocsOuverts", blocs)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Personne {
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
                heureNaissance = o.optString("heureNaissance", ""),
                lieuNaissance = o.optString("lieuNaissance", ancienneNaissance),
                deptNaissance = o.optString("deptNaissance", ""),
                dateBapteme = o.optString("dateBapteme", ""),
                heureBapteme = o.optString("heureBapteme", ""),
                lieuBapteme = o.optString("lieuBapteme", ""),
                deptBapteme = o.optString("deptBapteme", ""),
                dateMariage = o.optString("dateMariage", ""),
                heureMariage = o.optString("heureMariage", ""),
                lieuMariage = o.optString("lieuMariage", ""),
                deptMariage = o.optString("deptMariage", ""),
                dateDeces = o.optString("dateDeces", ""),
                heureDeces = o.optString("heureDeces", ""),
                lieuDeces = o.optString("lieuDeces", ancienDeces),
                deptDeces = o.optString("deptDeces", ""),
                dateSepulture = o.optString("dateSepulture", ""),
                heureSepulture = o.optString("heureSepulture", ""),
                lieuSepulture = o.optString("lieuSepulture", ""),
                deptSepulture = o.optString("deptSepulture", ""),
                etatIdentite = o.optString("etatIdentite", ""),
                etatNaissance = o.optString("etatNaissance", ""),
                etatBapteme = o.optString("etatBapteme", ""),
                etatMariage = o.optString("etatMariage", ""),
                etatDeces = o.optString("etatDeces", ""),
                etatSepulture = o.optString("etatSepulture", ""),
                pere = o.optLong("pere", 0L),
                mere = o.optLong("mere", 0L),
                conjoint = o.optLong("conjoint", 0L),
                conjointsSupp = mutableListOf<Long>().apply {
                    val supp = o.optJSONArray("conjointsSupp")
                    if (supp != null) {
                        for (i in 0 until supp.length()) {
                            val v = supp.optLong(i, 0L)
                            if (v != 0L) add(v)
                        }
                    }
                },
                enfants = mutableListOf<Long>().apply {
                    val tableau = o.optJSONArray("enfants")
                    if (tableau != null) {
                        for (i in 0 until tableau.length()) {
                            add(tableau.optLong(i, 0L))
                        }
                    }
                },
                blocsOuverts = mutableListOf<String>().apply {
                    val blocs = o.optJSONArray("blocsOuverts")
                    if (blocs != null) {
                        for (i in 0 until blocs.length()) {
                            add(blocs.optString(i, ""))
                        }
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------- Modele Piste (journal de recherches)

// Compte-rendu horodate d'une recherche effectuee aux archives :
// "le JJ/MM/AAAA : rien trouve, jusqu'a la page X. Reprendre les TD de VILLE a partir de ANNEE."
data class Recherche(
    val date: Long = 0L,
    val resultat: String = "R", // "R" rien, "T" trouve
    val page: String = "",
    val repriseVille: String = "",
    val repriseAnnee: String = "",
    val note: String = ""
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("date", date)
        o.put("resultat", resultat)
        o.put("page", page)
        o.put("repriseVille", repriseVille)
        o.put("repriseAnnee", repriseAnnee)
        o.put("note", note)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Recherche = Recherche(
            date = o.optLong("date", 0L),
            resultat = o.optString("resultat", "R"),
            page = o.optString("page", ""),
            repriseVille = o.optString("repriseVille", ""),
            repriseAnnee = o.optString("repriseAnnee", ""),
            note = o.optString("note", "")
        )
    }
}

data class Piste(
    val id: Long,
    var texte: String = "",
    var personneId: Long = 0L,
    var creation: Long = 0L,
    var faite: Boolean = false,
    var cloture: Long = 0L,
    var type: String = "P", // "P" piste libre, "A" issue d'une suggestion
    var categorie: String = "", // categorie de suggestion ("N", "BP", "M", "D", "S", "P", "ME")
    var lieu: String = "",
    var dept: String = "",
    var debut: String = "",
    var fin: String = "",
    var enCours: String = "", // ce qui est en cours de recherche (bloc c)
    var historique: MutableList<Recherche> = mutableListOf()
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("texte", texte)
        o.put("personneId", personneId)
        o.put("creation", creation)
        o.put("faite", faite)
        o.put("cloture", cloture)
        o.put("type", type)
        o.put("categorie", categorie)
        o.put("lieu", lieu)
        o.put("dept", dept)
        o.put("debut", debut)
        o.put("fin", fin)
        o.put("enCours", enCours)
        val tableau = JSONArray()
        historique.forEach { tableau.put(it.versJson()) }
        o.put("historique", tableau)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Piste = Piste(
            id = o.optLong("id", 0L),
            texte = o.optString("texte", ""),
            personneId = o.optLong("personneId", 0L),
            creation = o.optLong("creation", 0L),
            faite = o.optBoolean("faite", false),
            cloture = o.optLong("cloture", 0L),
            type = o.optString("type", "P"),
            categorie = o.optString("categorie", ""),
            lieu = o.optString("lieu", ""),
            dept = o.optString("dept", ""),
            debut = o.optString("debut", ""),
            fin = o.optString("fin", ""),
            enCours = o.optString("enCours", ""),
            historique = mutableListOf<Recherche>().apply {
                val tableau = o.optJSONArray("historique")
                if (tableau != null) {
                    for (i in 0 until tableau.length()) {
                        add(Recherche.depuisJson(tableau.getJSONObject(i)))
                    }
                }
            }
        )
    }
}

// ---------------------------------------------------------------- Modele Acte (image chiffree liee a une personne)

data class Acte(
    val id: Long,
    var personneId: Long = 0L,
    var type: String = "N",
    var titre: String = "",
    var creation: Long = 0L,
    var numPage: String = "",
    var totalPage: String = "",
    // Transcription integrale (saisie par acte) : ages, temoins, signataires.
    var contenu: String = "",
    // Reference de l'acte aux Archives departementales (cote, ex "1890-N-123").
    var reference: String = ""
) {
    fun versJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("personneId", personneId)
        o.put("type", type)
        o.put("titre", titre)
        o.put("creation", creation)
        o.put("numPage", numPage)
        o.put("totalPage", totalPage)
        o.put("contenu", contenu)
        o.put("reference", reference)
        return o
    }

    companion object {
        fun depuisJson(o: JSONObject): Acte = Acte(
            id = o.optLong("id", 0L),
            personneId = o.optLong("personneId", 0L),
            type = o.optString("type", "N"),
            titre = o.optString("titre", ""),
            creation = o.optLong("creation", 0L),
            numPage = o.optString("numPage", ""),
            totalPage = o.optString("totalPage", ""),
            contenu = o.optString("contenu", ""),
            reference = o.optString("reference", "")
        )
    }
}



