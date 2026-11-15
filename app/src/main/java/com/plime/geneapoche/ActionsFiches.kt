package com.plime.geneapoche

import kotlin.reflect.KMutableProperty1

// v0.113 : coeur des actions les plus destructrices (suppression d'une
// personne avec Annuler, fusion de deux fiches), extrait de AppEcran en
// fonctions pures, testees sur JVM. Corps deplaces tels quels : seul le
// contexte (etat de l'ecran, stockage, messages) reste dans MainActivity.

// Liens (parents, conjoints, enfants) des personnes qui citent [id], captures
// AVANT une suppression pour pouvoir l'annuler (les listes sont copiees).
fun capturerLiensAvant(personnes: List<Personne>, id: Long): List<LienAvant> =
    personnes.filter {
        it.pere == id || it.mere == id || it.conjoint == id ||
            it.conjointsSupp.contains(id) || it.enfants.contains(id)
    }.map { LienAvant(it.id, it.pere, it.mere, it.conjoint, it.conjointsSupp.toList(), it.enfants.toList()) }

// Apres la suppression de la personne [id] : plus aucune fiche ne la cite
// comme pere, mere, conjoint, autre conjoint ou enfant.
fun detacherReferences(personnes: List<Personne>, id: Long) {
    personnes.forEach { q ->
        if (q.pere == id) q.pere = 0L
        if (q.mere == id) q.mere = 0L
        if (q.conjoint == id) q.conjoint = 0L
        q.conjointsSupp.remove(id)
        q.enfants.remove(id)
    }
}

// Annuler : remet les liens captures par capturerLiensAvant (la personne
// supprimee doit avoir ete rajoutee a [personnes] auparavant).
fun restaurerLiens(personnes: List<Personne>, liens: List<LienAvant>) {
    liens.forEach { l ->
        personneParId(personnes, l.id)?.let { q ->
            q.pere = l.pere
            q.mere = l.mere
            q.conjoint = l.conjoint
            q.conjointsSupp.clear()
            q.conjointsSupp.addAll(l.conjointsSupp)
            q.enfants.clear()
            q.enfants.addAll(l.enfants)
        }
    }
}

// Fusion guidee d'un doublon : toutes les informations et tous les liens de
// [supprime] sont reportes sur [garde] (sans jamais ecraser une donnee deja
// presente) et toute reference a [supprime] dans [personnes] vise [garde].
// NE retire PAS [supprime] de la liste (l'appelant le fait, avec recherches,
// citations, index et enregistrement).
fun fusionnerFiches(personnes: List<Personne>, garde: Personne, supprime: Personne) {
    val gardeId = garde.id
    val supprimeId = supprime.id
    // 1. Champs texte : recopi\u00E9s seulement s'ils sont vides chez garde.
    fun recopier(prop: KMutableProperty1<Personne, String>) {
        if (prop.get(garde).isBlank() && prop.get(supprime).isNotBlank()) {
            prop.set(garde, prop.get(supprime))
        }
    }
    listOf(
        Personne::profession, Personne::sexe, Personne::notesLibres,
        Personne::dateNaissance, Personne::heureNaissance, Personne::lieuNaissance, Personne::deptNaissance,
        Personne::dateBapteme, Personne::heureBapteme, Personne::lieuBapteme, Personne::deptBapteme,
        Personne::dateMariage, Personne::heureMariage, Personne::lieuMariage, Personne::deptMariage,
        Personne::dateDeces, Personne::heureDeces, Personne::lieuDeces, Personne::deptDeces,
        Personne::dateSepulture, Personne::heureSepulture, Personne::lieuSepulture, Personne::deptSepulture
    ).forEach { recopier(it) }
    // 2. Listes : union sans doublon.
    fun union(dest: MutableList<Long>, src: MutableList<Long>) {
        src.filterNot { it == supprimeId || dest.contains(it) }.forEach { dest.add(it) }
    }
    union(garde.enfants, supprime.enfants)
    union(garde.conjointsSupp, supprime.conjointsSupp)
    listOf("N", "B", "M", "D", "S").forEach { t ->
        union(garde.temoinsDe(t), supprime.temoinsDe(t))
        union(garde.signatairesDe(t), supprime.signatairesDe(t))
    }
    supprime.evenementsLibres.filterNot { ev -> garde.evenementsLibres.any { it.id == ev.id } }
        .forEach { garde.evenementsLibres.add(it) }
    supprime.blocsOuverts.filterNot { garde.blocsOuverts.contains(it) }.forEach { garde.blocsOuverts.add(it) }
    // 3. R\u00F4les : pris seulement s'ils sont inconnus chez garde.
    if (garde.pere == 0L && supprime.pere != 0L) garde.pere = supprime.pere
    if (garde.mere == 0L && supprime.mere != 0L) garde.mere = supprime.mere
    if (garde.conjoint == 0L && supprime.conjoint != 0L) garde.conjoint = supprime.conjoint
    // 4. Partout dans la base : toute r\u00E9f\u00E9rence au doublon vise garde.
    // v0.113 : si une liste citait AUSSI garde (cas typique : le doublon et la
    // fiche conservee sont tous deux enfants du meme parent), elle est
    // dedoublonnee apres remplacement ; auparavant seules les listes de garde
    // l'etaient, et le parent se retrouvait avec la meme personne en double.
    fun dedupe(l: MutableList<Long>) {
        val vus = mutableSetOf<Long>()
        l.removeAll { !vus.add(it) }
    }
    personnes.filter { it.id != supprimeId }.forEach { q ->
        fun remplace(v: Long): Long = if (v == supprimeId) gardeId else v
        fun remplacerListe(l: MutableList<Long>) {
            val citait = l.contains(supprimeId)
            l.replaceAll { remplace(it) }
            if (citait) dedupe(l)
        }
        q.pere = remplace(q.pere)
        q.mere = remplace(q.mere)
        q.conjoint = remplace(q.conjoint)
        remplacerListe(q.conjointsSupp)
        remplacerListe(q.enfants)
        listOf("N", "B", "M", "D", "S").forEach { t ->
            remplacerListe(q.temoinsDe(t))
            remplacerListe(q.signatairesDe(t))
        }
    }
    // D\u00E9doublonnage des listes de garde apr\u00E8s remplacement.
    dedupe(garde.enfants); dedupe(garde.conjointsSupp)
    listOf("N", "B", "M", "D", "S").forEach { t ->
        dedupe(garde.temoinsDe(t)); dedupe(garde.signatairesDe(t))
    }
}
