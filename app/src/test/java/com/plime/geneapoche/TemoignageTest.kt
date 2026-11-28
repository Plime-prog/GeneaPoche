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

    @Test fun descendanceEstimeeDeProcheEnProche() {
        val a = Personne(id = 1, nom = "A", prenoms = "A", sexe = "M", dateMariage = "10/05/1885")
        val b = Personne(id = 2, nom = "A", prenoms = "B", sexe = "M", pere = 1L)
        val c = Personne(id = 3, nom = "A", prenoms = "C", sexe = "M", pere = 2L)
        a.enfants.add(2); b.enfants.add(3)
        completerDatesApproximatives(listOf(a, b, c))
        assertEquals("vers 1860", a.naissanceCalculee)
        assertEquals("vers 1892", b.naissanceCalculee)
        assertEquals("vers 1924", c.naissanceCalculee)
    }

    @Test fun agePropose() {
        val p = Personne(id = 1, nom = "A", prenoms = "A", naissanceCalculee = "vers 1860")
        assertEquals("25", ageProposeALActe(p, "10/05/1885"))
        assertEquals("", ageProposeALActe(Personne(id = 2, nom = "B", prenoms = "B"), "10/05/1885"))
    }
}

class ReperesGenealogiquesTest {
    @Test fun majoriteMatrimonialeParEpoque() {
        assertEquals(25, majoriteMatrimoniale(1858, false))
        assertEquals(21, majoriteMatrimoniale(1858, true))
        assertEquals(21, majoriteMatrimoniale(1950, false))
        assertEquals(18, majoriteMatrimoniale(1990, true))
    }

    @Test fun mariageSousLaMajorite() {
        val p = Personne(id = 1, nom = "A", prenoms = "Marie", sexe = "F", dateNaissance = "01/01/1836", dateMariage = "10/05/1858")
        val r = reperesGenealogiquesDe(p, listOf(p))
        assertTrue(r.any { it.startsWith("À son mariage (1858), Marie a 22 ans") }.not())  // 22 >= 21 : majeure
        val q = Personne(id = 2, nom = "A", prenoms = "Anne", sexe = "F", dateNaissance = "01/01/1840", dateMariage = "10/05/1858")
        assertTrue(reperesGenealogiquesDe(q, listOf(q)).any { it.contains("a 18 ans") && it.contains("consentement de ses parents") && it.contains("émancipait") })
    }

    @Test fun conscriptionEtRecensements() {
        val p = Personne(id = 1, nom = "A", prenoms = "Jean", sexe = "M", dateNaissance = "01/01/1817", dateDeces = "01/01/1880")
        val r = reperesGenealogiquesDe(p, listOf(p))
        assertTrue(r.any { it.contains("classe 1837") && it.contains("tirage au sort") })
        assertTrue(r.any { it.startsWith("Peut figurer aux recensements nominatifs de 1836 à 1876") })
        assertTrue(r.any { it.startsWith("Majeur en 1838") })
    }

    @Test fun veuvage() {
        val m = Personne(id = 1, nom = "A", prenoms = "Paul", sexe = "M", dateNaissance = "01/01/1830", dateDeces = "01/01/1860")
        val f = Personne(id = 2, nom = "A", prenoms = "Anne", sexe = "F", dateNaissance = "01/01/1835", conjoint = 1L)
        assertTrue(reperesGenealogiquesDe(f, listOf(m, f)).any { it.startsWith("Veuve en 1860") })
    }

    @Test fun sansNaissanceRien() {
        assertTrue(reperesGenealogiquesDe(Personne(id = 1, nom = "A", prenoms = "B"), emptyList()).isEmpty())
    }

    @Test fun reperesGeneaHorsBiographie() {
        assertTrue(EvenementsHistoriques.any { it.genre == "genea" && it.annee == 1804 })
        val p = Personne(id = 1, nom = "A", prenoms = "B", dateNaissance = "01/01/1800", dateDeces = "01/01/1850")
        assertTrue(friseBiographie(p, listOf(p)).none { it.texte.contains("majorité matrimoniale") })
    }
}

class AgeDecedeTest {
    @Test fun apresLeDecesOuInvraisemblable() {
        val p = Personne(id = 1, nom = "A", prenoms = "B", dateNaissance = "01/01/1800", dateDeces = "01/01/1850")
        assertEquals("(décédé)", ageDePersonne(p, "05/05/1921"))
        assertEquals("(50 ans)", ageDePersonne(p, "01/01/1850"))
        val q = Personne(id = 2, nom = "A", prenoms = "B", dateNaissance = "01/01/1800")
        assertEquals("(décédé)", ageDePersonne(q, "05/05/1921"))
        assertEquals("(60 ans)", ageDePersonne(q, "05/05/1860"))
    }
}

class V155Test {
    @Test fun dureeDuServiceParClasse() {
        assertEquals("7 ans (loi de 1832)", dureeServiceMilitaire(1837))
        assertEquals("3 ans (loi de 1889)", dureeServiceMilitaire(1900))
        assertEquals("2 ans (loi de 1905)", dureeServiceMilitaire(1910))
        assertEquals("1 an (loi de 1928)", dureeServiceMilitaire(1930))
        assertEquals(null, dureeServiceMilitaire(1997))
    }

    @Test fun phraseServiceEtGuerre() {
        val p = Personne(id = 1, nom = "A", prenoms = "Jean", sexe = "M", dateNaissance = "01/01/1890")
        val r = reperesGenealogiquesDe(p, listOf(p))
        assertTrue(r.any { it.contains("classe 1910") && it.contains("durée légale du service : 2 ans (loi de 1905)") })
        assertTrue(r.any { it.contains("Il avait 24 ans au début de la Première Guerre mondiale") && it.contains("soit 4 ans et 3 mois") })
    }

    @Test fun lettreUnion() {
        assertEquals("a", lettreUnion(1)); assertEquals("b", lettreUnion(2)); assertEquals("z", lettreUnion(26)); assertEquals("aa", lettreUnion(27))
    }

    @Test fun conjointsNumerotesParLettre() {
        val x = Personne(id = 1, nom = "X", prenoms = "A", sosa = "1")
        val c1 = Personne(id = 2, nom = "C", prenoms = "A", sexe = "F")
        val c2 = Personne(id = 3, nom = "C", prenoms = "B", sexe = "F")
        val e = Personne(id = 4, nom = "X", prenoms = "E", pere = 1L)
        x.conjoint = 2L; x.conjointsSupp.add(3L); x.enfants.add(4L)
        val l = mutableListOf(x, c1, c2, e)
        renumeroter(l, 1L)
        assertEquals("1.1", e.lateral)
        assertEquals("1a", c1.lateral)
        assertEquals("1b", c2.lateral)
        assertTrue(estConjointNumerote(c1))
        assertTrue(suggestionsPour(c1, l).isEmpty())
        // tri : 1, 1a, 1b, 1.1
        val tri = trierParAboville(l).map { it.numeroBrut() }
        assertEquals(listOf("1", "1a", "1b", "1.1"), tri)
    }

    @Test fun brancheSansConjointsAvecAscendants() {
        val gp = Personne(id = 1, nom = "G", prenoms = "P")
        val pgp = Personne(id = 9, nom = "A", prenoms = "A")
        gp.pere = 9L
        val sp = Personne(id = 2, nom = "S", prenoms = "S")
        val enf = Personne(id = 3, nom = "G", prenoms = "E", pere = 1L, mere = 2L)
        val pe = Personne(id = 4, nom = "P", prenoms = "P", pere = 3L)
        gp.conjoint = 2L; gp.enfants.add(3L)
        val b = brancheDe(1L, listOf(gp, pgp, sp, enf, pe))
        assertEquals(setOf(1L, 9L, 3L, 4L), b)
    }

    @Test fun manquesParGenerationCompte() {
        val c = Personne(id = 1, nom = "C", prenoms = "C", sosa = "1", pere = 2L)
        val pere = Personne(id = 2, nom = "P", prenoms = "P", sosa = "2")
        val g = manquesParGeneration(listOf(c, pere))
        assertEquals(2, g.size)
        assertEquals(1, g[1].connus)
        assertEquals(2L, g[1].attendus)
        assertEquals(50, g[1].pourcent)
        assertTrue(g[1].lignes.first().manques.contains("mère").not())
        assertTrue(g[0].lignes.first().manques.contains("mère"))
    }

    @Test fun coherenceTemoinEtDeclarant() {
        val a = Personne(id = 1, nom = "A", prenoms = "A", dateMariage = "10/05/1900")
        val t = Personne(id = 2, nom = "T", prenoms = "T", dateDeces = "01/01/1890")
        a.temoinsM.add(2L)
        val an = anomaliesDeLArbre(listOf(a, t), emptySet())
        assertTrue(an.any { it.type == "temoin-decede" })
        val v = Personne(id = 3, nom = "V", prenoms = "V", dateNaissance = "01/01/1700", dateDeces = "01/01/1830")
        assertTrue(anomaliesDeLArbre(listOf(v), emptySet()).any { it.type == "deces-trop-age" })
    }
}
