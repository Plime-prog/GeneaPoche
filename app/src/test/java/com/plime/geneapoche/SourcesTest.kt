package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// v0.92 : carnet de sources (Source reutilisable / Citation) et GEDCOM.
class SourcesTest {

    private fun sourceAD18(id: Long = 1L) = Source(
        id = id,
        titre = "Archives d\u00e9partementales du Cher, \u00e9tat civil de Bourges, 1872",
        type = "naissance", commune = "Bourges", dept = "18", periode = "1872",
        depot = "Archives d\u00e9partementales du Cher", cote = "4 E 1234",
        url = "https://example.org/ad18", transcription = "L'an mil huit cent soixante-douze\nle trois f\u00e9vrier...",
        notes = "Registre en bon \u00e9tat.", fiabilite = 3
    )

    private fun marie(id: Long = 1L) = Personne(
        id = id, nom = "Dupont", prenoms = "Marie", sexe = "F",
        dateNaissance = "03/02/1872", lieuNaissance = "Bourges", deptNaissance = "18"
    )

    @Test
    fun jsonAllerRetour() {
        val s = sourceAD18()
        val c = Citation(7, 1, 1, "N", page = "f\u00b0 12", numeroActe = "43", vue = 17, vueTotal = 312,
            extrait = "n\u00e9e le trois", notes = "ok", fiabilite = 2)
        val texte = catalogueVersTexte(listOf(s), listOf(c))
        val cat = catalogueDepuisTexte(texte)
        assertEquals(1, cat.sources.size)
        assertEquals(s.titre, cat.sources[0].titre)
        assertEquals("https://example.org/ad18", cat.sources[0].url)
        assertEquals("L'an mil huit cent soixante-douze\nle trois f\u00e9vrier...", cat.sources[0].transcription)
        assertEquals(3, cat.sources[0].fiabilite)
        val c2 = cat.citations[0]
        assertEquals(7L, c2.id)
        assertEquals("N", c2.cible)
        assertEquals(17, c2.vue)
        assertEquals(312, c2.vueTotal)
        assertEquals("43", c2.numeroActe)
        assertEquals(2, c2.fiabilite)
    }

    @Test
    fun ancienneDonneeSansChampsRelue() {
        val cat = catalogueDepuisTexte("[{\"k\":\"s\",\"id\":4,\"titre\":\"X\"},{\"k\":\"c\",\"id\":1,\"sourceId\":4,\"personneId\":2}]")
        assertEquals(-1, cat.sources[0].fiabilite)
        assertEquals("P", cat.citations[0].cible)
        assertEquals(0, cat.citations[0].vue)
    }

    @Test
    fun libellesDeCitation() {
        val p = marie()
        val c = Citation(1, 1, 1, "N", numeroActe = "43", vue = 17, vueTotal = 312)
        assertEquals("acte n\u00b0 43, vue 17/312", detailCitation(c))
        assertEquals("Naissance de DUPONT Marie, acte n\u00b0 43, vue 17/312", libelleCitation(c, p))
        assertEquals("vue 5", detailCitation(Citation(2, 1, 1, "N", vue = 5)))
        assertEquals("", detailCitation(Citation(3, 1, 1, "N")))
        p.evenementsLibres.add(EvenementLibre(id = 9, intitule = "Recensement"))
        assertEquals("\u00c9v\u00e9nement : Recensement de DUPONT Marie", libelleCitation(Citation(4, 1, 1, "E9"), p))
        assertEquals(3, qualiteEffective(Citation(5, 1, 1, "N"), sourceAD18()))
        assertEquals(1, qualiteEffective(Citation(6, 1, 1, "N", fiabilite = 1), sourceAD18()))
        assertEquals(-1, qualiteEffective(Citation(7, 1, 1, "N"), null))
        assertEquals("Archives d\u00e9partementales du Cher, Acte de naissance, Bourges, 1872",
            titreSuggere(Source(1, type = "naissance", commune = "Bourges", periode = "1872", depot = "Archives d\u00e9partementales du Cher")))
    }

    @Test
    fun periodesGedcom() {
        assertEquals("1872", periodeGedcom("1872"))
        assertEquals("FROM 1860 TO 1872", periodeGedcom("1860-1872"))
        assertEquals("FROM 1860 TO 1872", periodeGedcom("1860 \u00e0 1872"))
        assertEquals(null, periodeGedcom(""))
        assertEquals("1860-1872", periodeDepuisGedcom("FROM 1860 TO 1872"))
        assertEquals("1860-1872", periodeDepuisGedcom("BET 1860 AND 1872"))
        assertEquals("1872", periodeDepuisGedcom("1872"))
        assertEquals("05/10/1872", periodeDepuisGedcom("5 OCT 1872"))
    }

    @Test
    fun enregistrementSourceGedcom() {
        val l = lignesEnregistrementsSources(listOf(sourceAD18(12)))
        assertEquals("0 @S12@ SOUR", l[0])
        assertTrue(l.contains("1 TITL Archives d\u00e9partementales du Cher, \u00e9tat civil de Bourges, 1872"))
        assertTrue(l.contains("1 _GPTYPE naissance"))
        assertTrue(l.contains("1 REPO @R1@"))
        assertTrue(l.contains("2 CALN 4 E 1234"))
        assertTrue(l.contains("1 DATA"))
        assertTrue(l.contains("2 EVEN BIRT"))
        assertTrue(l.contains("3 DATE 1872"))
        assertTrue(l.any { it.startsWith("3 PLAC Bourges, 18") })
        assertTrue(l.any { it.startsWith("1 TEXT L'an mil") })
        assertTrue(l.contains("2 CONT le trois f\u00e9vrier..."))
        assertTrue(l.contains("2 CONT URL : https://example.org/ad18"))
        assertTrue(l.contains("0 @R1@ REPO"))
        assertTrue(l.contains("1 NAME Archives d\u00e9partementales du Cher"))
    }

    @Test
    fun deuxSourcesPartagentLeMemeDepot() {
        val l = lignesEnregistrementsSources(listOf(sourceAD18(1), sourceAD18(2)))
        assertEquals(1, l.count { it == "0 @R1@ REPO" })
        assertEquals(2, l.count { it == "1 REPO @R1@" })
    }

    @Test
    fun citationGedcom() {
        val s = sourceAD18(1)
        val c = Citation(1, 1, 1, "N", numeroActe = "43", vue = 17, vueTotal = 312, extrait = "Marie", notes = "ok", fiabilite = 2)
        val l = lignesCitations(2, listOf(c), mapOf(1L to s))
        assertEquals("2 SOUR @S1@", l[0])
        assertTrue(l.contains("3 PAGE acte n\u00b0 43, vue 17/312"))
        assertTrue(l.contains("3 QUAY 2"))
        assertTrue(l.contains("3 DATA"))
        assertTrue(l.contains("4 TEXT Marie"))
        assertTrue(l.contains("3 NOTE ok"))
        assertTrue(l.contains("3 _GPVUE 17"))
        assertTrue(l.contains("3 _GPVUETOT 312"))
        // Fiabilite heritee de la source quand la citation n'en a pas.
        val l2 = lignesCitations(1, listOf(Citation(2, 1, 1, "P")), mapOf(1L to s))
        assertTrue(l2.contains("2 QUAY 3"))
    }

    private fun famille(): Triple<List<Personne>, List<Source>, List<Citation>> {
        val marie = marie(1)
        val pere = Personne(id = 2, nom = "Dupont", prenoms = "Jean", sexe = "M")
        val mere = Personne(id = 3, nom = "Martin", prenoms = "Anne", sexe = "F")
        marie.pere = 2; marie.mere = 3
        pere.conjoint = 3; mere.conjoint = 2
        pere.enfants.add(1); mere.enfants.add(1)
        pere.dateMariage = "10/06/1870"; mere.dateMariage = "10/06/1870"
        marie.evenementsLibres.add(EvenementLibre(id = 1, intitule = "Recensement", date = "1876"))
        val s1 = sourceAD18(1)
        val s2 = Source(2, titre = "Registre paroissial de Saint-Etienne", type = "bapteme", depot = "")
        val cits = listOf(
            Citation(1, 1, 1, "N", numeroActe = "43", vue = 17, vueTotal = 312, fiabilite = 3),
            Citation(2, 1, 1, "P", page = "relev\u00e9 personnel"),
            Citation(3, 2, 1, "E1", page = "p. 4"),
            Citation(4, 1, 2, "M", numeroActe = "12"),
            Citation(5, 1, 3, "M", numeroActe = "12")
        )
        return Triple(listOf(marie, pere, mere), listOf(s1, s2), cits)
    }

    @Test
    fun exportPuisReimportConserveSourcesEtCitations() {
        val (pers, srcs, cits) = famille()
        val gedcom = construireGedcom(pers, null, "5.5.1", srcs, cits)
        val res = importerGedcomRapporte(gedcom)
        assertEquals(3, res.personnes.size)
        assertEquals(2, res.sources.size)
        val s = res.sources[0]
        assertEquals(srcs[0].titre, s.titre)
        assertEquals("naissance", s.type)
        assertEquals("Bourges", s.commune)
        assertEquals("18", s.dept)
        assertEquals("1872", s.periode)
        assertEquals("Archives d\u00e9partementales du Cher", s.depot)
        assertEquals("4 E 1234", s.cote)
        assertEquals("https://example.org/ad18", s.url)
        assertEquals("Registre en bon \u00e9tat.", s.notes)
        assertEquals("L'an mil huit cent soixante-douze\nle trois f\u00e9vrier...", s.transcription)
        assertEquals(3, s.fiabilite)
        assertEquals("bapteme", res.sources[1].type)
        // Citations : naissance + identite + evenement libre + mariage des deux epoux.
        val c = res.citations
        val naiss = c.first { it.personneId == 1L && it.cible == "N" }
        assertEquals("43", naiss.numeroActe)
        assertEquals(17, naiss.vue)
        assertEquals(312, naiss.vueTotal)
        assertEquals(3, naiss.fiabilite)
        assertEquals("relev\u00e9 personnel", c.first { it.personneId == 1L && it.cible == "P" }.page)
        val libre = c.first { it.personneId == 1L && it.cible == "E1" }
        assertEquals("p. 4", libre.page)
        assertEquals(res.sources[1].id, libre.sourceId)
        // Le mariage est cite UNE fois sous la famille mais relu pour les deux epoux.
        assertEquals(1, c.count { it.personneId == 2L && it.cible == "M" })
        assertEquals(1, c.count { it.personneId == 3L && it.cible == "M" })
    }

    @Test
    fun exportPartielNEmporteQueLesSourcesCitees() {
        val (pers, srcs, cits) = famille()
        val gedcom = construireGedcom(pers, setOf(2L, 3L), "5.5.1", srcs, cits)
        assertTrue(gedcom.contains("0 @S1@ SOUR"))
        assertFalse(gedcom.contains("0 @S2@ SOUR"))
        assertEquals(1, gedcom.lines().count { it == "0 @R1@ REPO" })
    }

    @Test
    fun exportSansSourceInchange() {
        val (pers, _, _) = famille()
        val g = construireGedcom(pers)
        assertFalse(g.contains(" SOUR @"))
        assertFalse(g.contains(" SOUR\n"))
        assertEquals(3, importerGedcomRapporte(g).personnes.size)
    }

    @Test
    fun evenementSansDateNiLieuPorteSaCitation() {
        val p = Personne(id = 1, nom = "X", prenoms = "Y")
        val g = construireGedcom(listOf(p), null, "5.5.1", listOf(Source(1, titre = "T")), listOf(Citation(1, 1, 1, "D", page = "p")))
        assertTrue(g.contains("1 DEAT\n2 SOUR @S1@"))
        val r = importerGedcomRapporte(g)
        assertEquals("D", r.citations[0].cible)
    }

    private val heredis = """
0 HEAD
1 SOUR HEREDIS
1 CHAR UTF-8
0 @I1@ INDI
1 NAME Pierre /DUPONT/
1 SEX M
1 BIRT
2 DATE 12 FEB 1843
2 PLAC Bourges, 18, Cher
2 SOUR @S7@
3 PAGE Vue 22
3 QUAY 3
3 NOTE @N1@
1 SOUR Mon cahier de famille
1 NOTE Premiere ligne
2 CONT Deuxieme ligne
1 FAMS @F1@
0 @I2@ INDI
1 NAME Louise /MARTIN/
1 SEX F
1 FAMS @F1@
0 @F1@ FAM
1 HUSB @I1@
1 WIFE @I2@
1 MARR
2 DATE 7 JUN 1867
2 SOUR @S7@
3 PAGE Acte 15
1 SOUR @S8@
0 @S7@ SOUR
1 TITL Etat civil de Bourges
1 ABBR AD18 EC Bourges
1 AUTH Archives du Cher
1 PUBL Numerisation 2018
1 REPO @R1@
2 CALN 4 E 1234
1 NOTE @N2@
1 TEXT Transcription
2 CONT multi-ligne
0 @S8@ SOUR
1 ABBR Sans titre long
0 @R1@ REPO
1 NAME AD du Cher
0 @N1@ NOTE Le texte de la citation
0 @N2@ NOTE Note de la source
0 TRLR
""".trimIndent()

    @Test
    fun importDeStyleHeredis() {
        val r = importerGedcomRapporte(heredis)
        assertEquals(2, r.personnes.size)
        assertEquals("Premiere ligne\nDeuxieme ligne", r.personnes[0].notesLibres)
        // S7 + S8 + la source "en ligne" (texte libre).
        assertEquals(3, r.sources.size)
        val s7 = r.sources[0]
        assertEquals("Etat civil de Bourges", s7.titre)
        assertEquals("AD du Cher", s7.depot)
        assertEquals("4 E 1234", s7.cote)
        assertEquals("Transcription\nmulti-ligne", s7.transcription)
        assertTrue(s7.notes.contains("Note de la source"))
        assertTrue(s7.notes.contains("Auteur : Archives du Cher"))
        assertTrue(s7.notes.contains("Publication : Numerisation 2018"))
        assertEquals("Sans titre long", r.sources[1].titre)
        assertEquals("Mon cahier de famille", r.sources[2].titre)
        val naiss = r.citations.first { it.personneId == 1L && it.cible == "N" }
        assertEquals("Vue 22", naiss.page)
        assertEquals(3, naiss.fiabilite)
        assertEquals("Le texte de la citation", naiss.notes)
        assertTrue(r.citations.any { it.personneId == 1L && it.cible == "P" && it.sourceId == r.sources[2].id })
        // Mariage sous la famille : cite pour les deux epoux ; source de famille : les deux aussi.
        assertTrue(r.citations.any { it.personneId == 1L && it.cible == "M" && it.page == "Acte 15" })
        assertTrue(r.citations.any { it.personneId == 2L && it.cible == "M" && it.page == "Acte 15" })
        assertTrue(r.citations.any { it.personneId == 2L && it.cible == "P" && it.sourceId == r.sources[1].id })
        // Les SOUR / REPO ne sont plus signales comme "non importes".
        assertFalse(r.ignores.any { it.contains("SOUR") || it.contains("REPO") })
    }

    @Test
    fun citationVersSourceInconnueIgnoree() {
        val g = "0 HEAD\n0 @I1@ INDI\n1 NAME A /B/\n1 SOUR @S99@\n2 PAGE x\n0 TRLR\n"
        val r = importerGedcomRapporte(g)
        assertEquals(1, r.personnes.size)
        assertTrue(r.citations.isEmpty())
    }

    @Test
    fun catalogueOutils() {
        val c = mutableListOf(
            Citation(1, 1, 5, "N", numeroActe = "1"), Citation(2, 1, 6, "N", numeroActe = "1"),
            Citation(3, 1, 5, "D", numeroActe = "9"), Citation(4, 2, 5, "P")
        )
        assertEquals(2, citationsDe(c, 5).count { it.cible == "N" || it.cible == "D" })
        assertEquals(3, nombreCitations(c, 1))
        // Fusion 6 -> 5 : la citation N identique est dedoublonnee.
        transfererCitations(c, 6, 5)
        assertEquals(3, c.size)
        assertTrue(c.all { it.personneId == 5L })
        val retirees = retirerCitationsDe(c, 5)
        assertEquals(3, retirees.size)
        assertTrue(c.isEmpty())
        assertEquals(4L, maxIdCatalogue(listOf(Source(2)), listOf(Citation(4, 2, 5))))
        val pers = listOf(marie(5))
        val valides = citationsValides(
            listOf(Citation(1, 1, 5), Citation(2, 9, 5), Citation(3, 1, 8)),
            listOf(Source(1)), pers
        )
        assertEquals(1, valides.size)
        assertTrue(citationExiste(listOf(Citation(1, 1, 5, "N", numeroActe = "1")), Citation(2, 1, 5, "N", numeroActe = "1")))
        assertNotNull(sourceParId(listOf(Source(1)), 1))
    }
}
