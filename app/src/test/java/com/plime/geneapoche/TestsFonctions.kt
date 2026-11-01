package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// G2-10 : tests unitaires JVM des fonctions pures de l'application.
// Donnees STRICTEMENT fictives.

class TestsFonctions {

    // ---------------------------------------------------------- Validation des dates (G2-01)

    @Test
    fun dateImpossibleRefusee() {
        assertFalse(dateValide("31/02/2024"))
        assertFalse(dateValide("29/02/1900"))
        assertFalse(dateValide("00/03/2024"))
        assertFalse(dateValide("15/13/2024"))
    }

    @Test
    fun dateReelleAcceptee() {
        assertTrue(dateValide("29/02/2024"))
        assertTrue(dateValide("27/10/1890"))
        assertTrue(dateValide(""))
        assertTrue(dateValide("1890"))
        assertTrue(dateValide("22 Germinal An II"))
    }

    @Test
    fun heureValideTouchee() {
        assertTrue(heureValide("14:05"))
        assertTrue(heureValide(""))
        assertFalse(heureValide("24:00"))
        assertFalse(heureValide("12:60"))
        assertFalse(heureValide("14h05"))
    }

    // ---------------------------------------------------------- Affichage des dates

    @Test
    fun dateAfficheeCompteLesZeros() {
        assertEquals("01/02/1890", dateAffichee("1/2/1890"))
        assertEquals("27/10/1890", dateAffichee("27/10/1890"))
    }

    @Test
    fun dateRepubliquerAfficheeAvecConversion() {
        val s = dateAffichee("22 Germinal An II")
        assertTrue(s.contains("(11/04/1794)"))
        assertEquals("1/2/1890", dateAffichee("1.2.1890"))
    }

    @Test
    fun heureAfficheeNormalisee() {
        assertEquals("14:05", heureAffichee("14:05"))
        assertEquals("09:15", heureAffichee("9h15"))
    }

    // ---------------------------------------------------------- Calendrier republicain (G2-03)

    @Test
    fun repubAGregorienConversions() {
        assertEquals("11/04/1794", repubAGregorien("22 Germinal An II"))
        assertEquals("22/09/1792", repubAGregorien("1 Vendemiaire An I"))
        assertNull(repubAGregorien("32 Germinal An II"))
        assertNull(repubAGregorien("10 complementaire An II"))
        assertNull(repubAGregorien("pas une date"))
    }

    @Test
    fun anneeDeComprendLesRepublicaines() {
        assertEquals(1794, anneeDe("22 Germinal An II"))
        assertEquals(1890, anneeDe("27/10/1890"))
        assertEquals(1890, anneeDe("1890"))
        assertNull(anneeDe(""))
        assertNull(anneeDe("sans chiffre"))
    }

    @Test
    fun anneeNaissancePersonne() {
        val p = Personne(id = 1L, dateNaissance = "22 Germinal An II")
        assertEquals(1794, anneeNaissance(p))
        assertEquals(9999, anneeNaissance(Personne(id = 2L)))
    }

    // ---------------------------------------------------------- GEDCOM : export (G2-01, G2-02)

    @Test
    fun dateGedcomExport() {
        assertEquals("27 OCT 1890", dateGedcom("27/10/1890"))
        assertEquals("2024", dateGedcom("31/02/2024")) // date impossible : annee seule
        assertEquals("ABT 1890", dateGedcom("vers 1890"))
        assertEquals("BEF 1890", dateGedcom("avant 1890"))
        assertEquals("1890", dateGedcom("1890"))
        assertNull(dateGedcom(""))
        assertNull(dateGedcom("inconnu"))
    }

    @Test
    fun dateFrancaiseImport() {
        assertEquals("27/10/1890", dateFrancaise("27 OCT 1890"))
        assertEquals("vers 1890", dateFrancaise("ABT 1890"))
        assertEquals("entre 1890 et 1892", dateFrancaise("BET 1890 AND 1892"))
    }

    @Test
    fun texteGedcomDoubleArobase() {
        assertEquals("a@@b", texteGedcom("a@b"))
    }

    @Test
    fun couperLieuPlac() {
        assertEquals("Roubaix" to "59", couperLieu("Roubaix, 59, Nord"))
        assertEquals("Roubaix" to "", couperLieu("Roubaix"))
    }

    @Test
    fun decoderGedcomEncodages() {
        assertEquals("0 HEAD", decoderGedcom("0 HEAD".toByteArray()))
        assertEquals("0 HEAD", decoderGedcom(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "0 HEAD".toByteArray()))
        val u16le = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "0 HEAD".toByteArray(Charsets.UTF_16LE)
        assertEquals("0 HEAD", decoderGedcom(u16le))
        val u16be = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + "0 HEAD".toByteArray(Charsets.UTF_16BE)
        assertEquals("0 HEAD", decoderGedcom(u16be))
    }

    @Test
    fun recollerConcCont() {
        val res = recollerConcCont(listOf("1 NOTE debut de", "2 CONC note", "2 CONT suite"))
        assertEquals(listOf("1 NOTE debut denote\nsuite"), res)
    }

    @Test
    fun estVivanteRegleDesCentAns() {
        assertTrue(estVivante(Personne(id = 1L, dateNaissance = "2000"), 2026))
        assertTrue(estVivante(Personne(id = 2L, dateNaissance = "donnee inconnue"), 2026))
        assertFalse(estVivante(Personne(id = 3L, dateNaissance = "1920"), 2026))
        assertFalse(estVivante(Personne(id = 4L, dateNaissance = "2000", dateDeces = "2020"), 2026))
    }

    // ---------------------------------------------------------- Aller-retour GEDCOM (G2-02, G2-04)

    @Test
    fun allerRetourGedcom() {
        val cujus = Personne(
            id = 1L, nom = "Dupont", prenoms = "Jean", sexe = "M",
            dateNaissance = "27/10/1890", lieuNaissance = "Roubaix", deptNaissance = "59",
            notesLibres = "note libre", pere = 2L
        )
        val pere = Personne(id = 2L, nom = "Dupont", prenoms = "Pierre", sexe = "M")
        val ged = construireGedcom(listOf(cujus, pere), actes = listOf())
        val import = importerGedcomRapporte(ged)
        assertEquals(2, import.personnes.size)
        val j = import.personnes.first { it.prenoms == "Jean" }
        assertEquals("DUPONT", j.nom)
        assertEquals("M", j.sexe)
        assertEquals("27/10/1890", j.dateNaissance)
        assertEquals("Roubaix", j.lieuNaissance)
        assertEquals("59", j.deptNaissance)
        assertEquals("1", j.sosa)
        assertEquals("note libre", j.notesLibres)
        // Le lien pere/enfant est recre via la FAM.
        val pt = import.personnes.first { it.prenoms == "Pierre" }
        assertEquals(pt.id, j.pere)
        assertTrue(pt.enfants.contains(j.id))
    }

    // ---------------------------------------------------------- Departements

    @Test
    fun numeroDepartementReconnu() {
        assertEquals(59, numeroDepartement("Roubaix (59)"))
        assertEquals(59, numeroDepartement("Nord"))
        assertNull(numeroDepartement("Roubaix"))
        assertNull(numeroDepartement("Ajaccio (2A)"))
    }

    // ---------------------------------------------------------- Numerotation (Sosa / d'Aboville)

    @Test
    fun renumeroterSosaEtAboville() {
        val cujus = Personne(id = 1L, nom = "A")
        val papa = Personne(id = 2L, nom = "B")
        val maman = Personne(id = 3L, nom = "C")
        val frere = Personne(id = 4L, nom = "D")
        val liste = mutableListOf(cujus, papa, maman, frere)
        cujus.pere = 2L; cujus.mere = 3L
        papa.enfants.add(4L)
        renumeroter(liste, 1L)
        assertEquals("1", cujus.sosa)
        assertEquals("2", papa.sosa)
        assertEquals("3", maman.sosa)
        assertEquals("2.1", frere.lateral)
    }

    @Test
    fun comparateurAbovilleCroissant() {
        val a = Personne(id = 1L, lateral = "1.2")
        val b = Personne(id = 2L, lateral = "1.10")
        val c = Personne(id = 3L, lateral = "2")
        assertTrue(ComparateurAboville.compare(a, b) < 0)
        assertTrue(ComparateurAboville.compare(b, c) < 0)
        assertTrue(ComparateurAboville.compare(c, Personne(id = 4L)) < 0)
    }

    @Test
    fun validationNumeros() {
        assertTrue(sosaValide(""))
        assertTrue(sosaValide("12"))
        assertFalse(sosaValide("0"))
        assertFalse(sosaValide("abc"))
        assertTrue(lateralValide("7.2.1"))
        assertFalse(lateralValide("7..2"))
        assertEquals("4", sosaParent(Personne(id = 1L, sosa = "2"), false))
        assertEquals("5", sosaParent(Personne(id = 1L, sosa = "2"), true))
    }

    // ---------------------------------------------------------- Normalisation

    @Test
    fun normalisationIdentite() {
        assertEquals("DUPONT", normaliserNom("dupont"))
        assertEquals("Jean-Marie", normaliserPrenoms("jean-marie"))
        assertEquals("Jean", normaliserPrenoms("JEAN"))
    }

    @Test
    fun personneNormaliseALaConstruction() {
        val p = Personne(id = 1L, nom = "dupont", prenoms = "jean")
        assertEquals("DUPONT", p.nom)
        assertEquals("Jean", p.prenoms)
        assertEquals("DUPONT Jean", p.titre())
    }

    // ---------------------------------------------------------- Messages d'erreur (G2-05)

    @Test
    fun messageErreurSelonType() {
        assertEquals("Fichier introuvable.", messageErreur(java.io.FileNotFoundException("x"), "defaut"))
        assertEquals("Acc\u00E8s refus\u00E9 au fichier.", messageErreur(SecurityException(), "defaut"))
        assertEquals("Donn\u00E9es illisibles : fichier corrompu ?", messageErreur(org.json.JSONException("x"), "defaut"))
        assertEquals("defaut", messageErreur(IllegalStateException(), "defaut"))
    }
}
