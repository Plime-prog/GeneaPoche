package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemoignageTest {
    private fun jeu(): List<Personne> {
        val t = Personne(id = 1, nom = "TEMOIN", prenoms = "Paul", sexe = "M", dateNaissance = "01/01/1840", profession = "meunier", domicile = "Bourges")
        val a = Personne(id = 2, nom = "DUPONT", prenoms = "Jean", sexe = "M", dateMariage = "10/05/1885", lieuMariage = "Bourges")
        val b = Personne(id = 3, nom = "MARTIN", prenoms = "Anne", sexe = "F", dateMariage = "10/05/1885", lieuMariage = "Bourges")
        a.temoinsM.add(1); b.temoinsM.add(1)
        return listOf(t, a, b)
    }

    @Test fun regroupeLesDeuxEpoux() {
        val l = jeu()
        val g = temoignagesDe(l[0], l)
        assertEquals(1, g.size)
        assertEquals(2, g[0].noms.size)
    }

    @Test fun phraseAvecAgeEtActe() {
        val l = jeu()
        val s = phraseTemoin(l[0], temoignagesDe(l[0], l)[0])
        assertTrue(s, s.contains("45 ans"))
        assertTrue(s, s.contains("meunier"))
        assertTrue(s, s.contains("a été témoin de l'acte de mariage de"))
        assertTrue(s, s.contains(" et "))
        assertTrue(s, s.contains("10"))
    }

    @Test fun chronologieDeLaFicheMaisPasBiographie() {
        val l = jeu()
        assertTrue(recitBiographie(l[0], l).none { it.contains("témoin de l'acte") })
        assertTrue(chronologieFamille(l[0], l).none { it.libelle.startsWith("Témoin") })
        assertTrue(evenementsFamille(l[0], l).any { it.second.contains("Témoin de l'acte de mariage") })
    }

    @Test fun aucunTemoignage() {
        val l = jeu()
        assertTrue(temoignagesDe(l[1], l).isEmpty())
    }
}

class HomonymesTest {
    private fun base(): MutableList<Personne> = mutableListOf(
        Personne(id = 1, nom = "DUPONT", prenoms = "Jean", sexe = "M"),
        Personne(id = 2, nom = "DUPONT", prenoms = "Jean", sexe = "M")
    )

    @Test fun deuxHomonymesAlerte() {
        val a = ActeSaisie(type = "M", nom = "Martin", prenoms = "Luc", pereNom = "Dupont", perePrenoms = "Jean", date = "10/05/1885")
        val r = alertesHomonymes(base(), a)
        assertEquals(1, r.size)
        assertEquals("pere", r[0].cle)
        assertTrue(r[0].plusieurs)
    }

    @Test fun choixExpliciteSupprimeAlerte() {
        val l = base()
        assertTrue(alertesHomonymes(l, ActeSaisie(type = "M", nom = "Martin", prenoms = "Luc", pereNom = "Dupont", perePrenoms = "Jean", pereId = 2L)).isEmpty())
        assertTrue(alertesHomonymes(l, ActeSaisie(type = "M", nom = "Martin", prenoms = "Luc", pereNom = "Dupont", perePrenoms = "Jean", nouveauxForces = listOf("pere"))).isEmpty())
    }

    @Test fun unSeulHomonymeAgeIncompatible() {
        val l = mutableListOf(Personne(id = 1, nom = "DUPONT", prenoms = "Jean", sexe = "M", dateNaissance = "01/01/1900"))
        val a = ActeSaisie(type = "M", nom = "Martin", prenoms = "Luc", date = "10/05/1885",
            temoins = listOf(TemoinSaisie(nom = "Dupont", prenoms = "Jean", age = "40")))
        val r = alertesHomonymes(l, a)
        assertEquals(1, r.size)
        assertFalse(r[0].plusieurs)
    }

    @Test fun unSeulHomonymeCompatiblePasDAlerte() {
        val l = mutableListOf(Personne(id = 1, nom = "DUPONT", prenoms = "Jean", sexe = "M", dateNaissance = "01/01/1845"))
        val a = ActeSaisie(type = "M", nom = "Martin", prenoms = "Luc", date = "10/05/1885",
            temoins = listOf(TemoinSaisie(nom = "Dupont", prenoms = "Jean", age = "40")))
        assertTrue(alertesHomonymes(l, a).isEmpty())
    }

    @Test fun nomInconnuPasDAlerte() {
        val a = ActeSaisie(type = "M", nom = "Martin", prenoms = "Luc", pereNom = "Inconnu", perePrenoms = "Paul")
        assertTrue(alertesHomonymes(base(), a).isEmpty())
    }
}

class ObservationsTest {
    @Test fun creationEtNaissanceCalculee() {
        val p = Personne(id = 5, nom = "DUPONT", prenoms = "Jean", sexe = "M",
            naissanceCalculee = "vers 1845", naissanceCalculeeSource = "calculée d'après l'acte de mariage de Luc MARTIN et de Anne DUPONT du 10/05/1905",
            naissanceCalculeeFiche = 9L, creeDepuis = origineCreation("N", "Jean DUPONT", "", "01/02/1900"), creeDepuisFiche = 9L)
        val o = observationsDe(p)
        assertEquals(2, o.size)
        assertEquals("Identité créée d'après l'acte de naissance de Jean DUPONT le 01/02/1900", o[0].texte)
        assertEquals(9L, o[0].ficheId)
        assertEquals("Né vers 1845 d'après l'acte de mariage de Luc MARTIN et de Anne DUPONT du 10/05/1905.", o[1].texte)
        assertEquals(9L, o[1].ficheId)
    }

    @Test fun rienAObserver() {
        assertTrue(observationsDe(Personne(id = 1, nom = "A", prenoms = "B")).isEmpty())
    }

    @Test fun naissanceCalculeeMemorise() {
        val c = Personne(id = 2, nom = "X", prenoms = "Y")
        naissanceDepuisAge(c, "10/05/1905", "60", "l'acte de mariage de Z", 7L)
        assertEquals("vers 1845", c.naissanceCalculee)
        assertEquals(7L, c.naissanceCalculeeFiche)
        assertTrue(c.naissanceCalculeeSource.endsWith("du 10/05/1905"))
    }

    @Test fun jsonConserveLesChamps() {
        val p = Personne(id = 3, nom = "A", prenoms = "B", creeDepuis = "x", creeDepuisFiche = 4L, naissanceCalculeeFiche = 6L)
        val q = Personne.depuisJson(p.versJson())
        assertEquals(4L, q.creeDepuisFiche)
        assertEquals(6L, q.naissanceCalculeeFiche)
        assertEquals("x", q.creeDepuis)
    }
}

class DatesApproximativesTest {
    private fun famille(): List<Personne> {
        val m = Personne(id = 1, nom = "DUPONT", prenoms = "Anne", sexe = "F", dateNaissance = "01/01/1850")
        val e = Personne(id = 2, nom = "DUPONT", prenoms = "Luc", sexe = "M", mere = 1L)
        m.enfants.add(2)
        return listOf(m, e)
    }

    @Test fun enfantRecoitUneNaissanceEstimee() {
        val l = famille()
        completerDatesApproximatives(l)
        assertEquals("vers 1878", l[1].naissanceCalculee)
        assertTrue(l[1].naissanceCalculeeSource.startsWith("estimée d'après la naissance de sa mère"))
        assertEquals(1L, l[1].naissanceCalculeeFiche)
        assertTrue(observationsDe(l[1]).any { it.texte.startsWith("Né vers 1878, estimée d'après") && it.ficheId == 1L })
    }

    @Test fun mereAvecEnfantDecesApres() {
        val l = famille()
        l[1].dateNaissance = "05/05/1880"
        completerDatesApproximatives(l)
        assertEquals("après 1880", l[0].decesCalcule)
        assertTrue(observationsDe(l[0]).any { it.texte.startsWith("Décédée après 1880") })
    }

    @Test fun neJamaisEcraserUneDateSaisie() {
        val l = famille()
        l[1].dateNaissance = "05/05/1880"; l[0].dateDeces = "01/01/1900"
        completerDatesApproximatives(l)
        assertEquals("", l[1].naissanceCalculee)
        assertEquals("", l[0].decesCalcule)
    }

    @Test fun estimationDisparaitQuandOnSaisitLaDate() {
        val l = famille()
        completerDatesApproximatives(l)
        l[1].dateNaissance = "05/05/1880"
        completerDatesApproximatives(l)
        assertEquals("", l[1].naissanceCalculee)
    }

    @Test fun unAgeRemplaceUneEstimation() {
        val l = famille()
        completerDatesApproximatives(l)
        naissanceDepuisAge(l[1], "10/05/1905", "30", "l'acte de mariage de Luc")
        assertEquals("vers 1875", l[1].naissanceCalculee)
        completerDatesApproximatives(l)
        assertEquals("vers 1875", l[1].naissanceCalculee)
    }

    @Test fun mariageDonneNaissanceEtTemoinDonneDeces() {
        val a = Personne(id = 1, nom = "A", prenoms = "A", dateMariage = "10/05/1885")
        val t = Personne(id = 2, nom = "T", prenoms = "T")
        a.temoinsM.add(2)
        completerDatesApproximatives(listOf(a, t))
        assertEquals("vers 1860", a.naissanceCalculee)
        assertEquals("après 1885", t.decesCalcule)
    }
}
