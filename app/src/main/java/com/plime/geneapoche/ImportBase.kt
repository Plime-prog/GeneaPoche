package com.plime.geneapoche

// v0.113+ : fusion intelligente d'un GEDCOM importe dans la base, extraite de
// AppEcran.fusionnerBase en fonction pure (testee sur JVM).
//
// Les personnes d'un fichier importe sont numerotees 1..n PAR LE LECTEUR, dans
// un espace d'identifiants qui n'a rien a voir avec celui de la base. Toute
// reference (pere, mere, conjoint, enfants, temoins, signataires) doit donc
// etre CONVERTIE : vers la personne deja presente quand elle est reconnue,
// sinon vers le nouvel identifiant attribue a la personne ajoutee.

class ResultatFusionImport(val completees: Int, val ajoutees: Int)

// [nouvelId] fournit un identifiant neuf (en production : le compteur global).
fun fusionnerImportees(
    personnes: MutableList<Personne>,
    importees: List<Personne>,
    nouvelId: () -> Long
): ResultatFusionImport {
    val correspondances = mutableMapOf<Long, Long>()
    fun anneesCompatibles(d1: String, d2: String): Boolean {
        val a1 = anneeDe(d1)
        val a2 = anneeDe(d2)
        return a1 == null || a2 == null || a1 == a2
    }
    // Passe 1 : reconnaissance des personnes existantes et compl\u00E9tion
    // de leurs champs ENCORE VIDES uniquement.
    importees.forEach { imp ->
        val doublon = personnes.firstOrNull { q ->
            q.nom.trim().uppercase() == imp.nom.trim().uppercase() &&
            q.prenoms.trim().uppercase() == imp.prenoms.trim().uppercase() &&
            anneesCompatibles(q.dateNaissance, imp.dateNaissance)
        }
        if (doublon != null) {
            correspondances[imp.id] = doublon.id
            if (doublon.sosa.isBlank() && imp.sosa.isNotBlank()) doublon.sosa = imp.sosa
            if (doublon.sexe.isBlank() && imp.sexe.isNotBlank()) doublon.sexe = imp.sexe
            if (doublon.dateNaissance.isBlank()) doublon.dateNaissance = imp.dateNaissance
            if (doublon.heureNaissance.isBlank()) doublon.heureNaissance = imp.heureNaissance
            if (doublon.lieuNaissance.isBlank()) doublon.lieuNaissance = imp.lieuNaissance
            if (doublon.deptNaissance.isBlank()) doublon.deptNaissance = imp.deptNaissance
            if (doublon.dateBapteme.isBlank()) doublon.dateBapteme = imp.dateBapteme
            if (doublon.heureBapteme.isBlank()) doublon.heureBapteme = imp.heureBapteme
            if (doublon.lieuBapteme.isBlank()) doublon.lieuBapteme = imp.lieuBapteme
            if (doublon.deptBapteme.isBlank()) doublon.deptBapteme = imp.deptBapteme
            if (doublon.dateMariage.isBlank()) doublon.dateMariage = imp.dateMariage
            if (doublon.heureMariage.isBlank()) doublon.heureMariage = imp.heureMariage
            if (doublon.lieuMariage.isBlank()) doublon.lieuMariage = imp.lieuMariage
            if (doublon.deptMariage.isBlank()) doublon.deptMariage = imp.deptMariage
            if (doublon.dateDeces.isBlank()) doublon.dateDeces = imp.dateDeces
            if (doublon.heureDeces.isBlank()) doublon.heureDeces = imp.heureDeces
            if (doublon.lieuDeces.isBlank()) doublon.lieuDeces = imp.lieuDeces
            if (doublon.deptDeces.isBlank()) doublon.deptDeces = imp.deptDeces
            if (doublon.dateSepulture.isBlank()) doublon.dateSepulture = imp.dateSepulture
            if (doublon.heureSepulture.isBlank()) doublon.heureSepulture = imp.heureSepulture
            if (doublon.lieuSepulture.isBlank()) doublon.lieuSepulture = imp.lieuSepulture
            if (doublon.deptSepulture.isBlank()) doublon.deptSepulture = imp.deptSepulture
            if (doublon.profession.isBlank()) doublon.profession = imp.profession
            if (doublon.notesLibres.isBlank()) doublon.notesLibres = imp.notesLibres
            imp.evenementsLibres.forEach { evI ->
                if (doublon.evenementsLibres.none {
                    it.intitule.trim().uppercase() == evI.intitule.trim().uppercase() &&
                    it.date.trim() == evI.date.trim()
                }) {
                    doublon.evenementsLibres.add(evI.copy(id = nouvelId()))
                }
            }
        }
    }
    // Passe 2 : les personnes ABSENTES recoivent de nouveaux ids. Tous les ids
    // sont attribues AVANT l'ajout, pour que les liens entre personnes
    // ajoutees puissent etre convertis des l'ajout.
    val idsNouvelles = mutableMapOf<Long, Long>()
    val absentes = importees.filter { !correspondances.containsKey(it.id) }
    absentes.forEach { imp -> idsNouvelles[imp.id] = nouvelId() }
    fun cible(idImp: Long): Long = correspondances[idImp] ?: idsNouvelles[idImp] ?: 0L
    // Liste de references convertie (listes NEUVES : aucune liste n'est
    // partagee avec la personne importee). Une reference inconnue disparait ;
    // dans une liste de signataires, 0 reste 0 ("le pere").
    fun convertir(l: List<Long>, zeroEstPere: Boolean = false): MutableList<Long> {
        val r = mutableListOf<Long>()
        for (v in l) {
            val c = if (v == 0L && zeroEstPere) 0L else cible(v)
            if ((c != 0L || (v == 0L && zeroEstPere)) && !r.contains(c)) r.add(c)
        }
        return r
    }
    absentes.forEach { imp ->
        personnes.add(
            imp.copy(
                id = idsNouvelles.getValue(imp.id),
                pere = cible(imp.pere),
                mere = cible(imp.mere),
                conjoint = cible(imp.conjoint),
                conjointsSupp = convertir(imp.conjointsSupp),
                enfants = convertir(imp.enfants),
                temoinsN = convertir(imp.temoinsN), temoinsB = convertir(imp.temoinsB),
                temoinsM = convertir(imp.temoinsM), temoinsD = convertir(imp.temoinsD),
                temoinsS = convertir(imp.temoinsS),
                signatairesN = convertir(imp.signatairesN, true), signatairesB = convertir(imp.signatairesB, true),
                signatairesM = convertir(imp.signatairesM, true), signatairesD = convertir(imp.signatairesD, true),
                signatairesS = convertir(imp.signatairesS, true),
                blocsOuverts = imp.blocsOuverts.toMutableList(),
                evenementsLibres = imp.evenementsLibres.toMutableList()
            )
        )
        IndexPersonnes.invalider()
    }
    // Passe 3 : remap des liens vers la base fusionn\u00E9e (aucun doublon).
    importees.forEach { imp ->
        val q = personneParId(personnes, cible(imp.id)) ?: return@forEach
        if (q.pere == 0L && imp.pere != 0L) q.pere = cible(imp.pere)
        if (q.mere == 0L && imp.mere != 0L) q.mere = cible(imp.mere)
        if (q.conjoint == 0L && imp.conjoint != 0L) q.conjoint = cible(imp.conjoint)
        imp.enfants.forEach { idEnf ->
            val idCible = cible(idEnf)
            if (idCible != 0L && !q.enfants.contains(idCible)) {
                val enfImp = personneParId(personnes, idCible)
                if (enfImp == null || incoherenceParente(q, enfImp.dateNaissance) == null) q.enfants.add(idCible)
            }
        }
    }
    return ResultatFusionImport(correspondances.size, absentes.size)
}

// v0.115 : choix du de cujus apres un REMPLACEMENT de base (logique extraite
// de MainActivity.remplacerBase, comportement identique) : la personne au plus
// petit numero Sosa numerique du fichier, sinon la premiere de la liste.
fun choisirDeCujusApresImport(personnes: List<Personne>): Personne? {
    val avecSosa = personnes.filter { it.sosa.trim().toLongOrNull() != null }
    return avecSosa.minByOrNull { it.sosa.trim().toLong() } ?: personnes.firstOrNull()
}
