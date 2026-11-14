package com.plime.geneapoche

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommuneChoisieTest {
    @Test
    fun lieuChoisiSeReplie() {
        assertTrue(lieuDejaChoisi("Saintes, 17415, Charente-Maritime"))
        assertTrue(lieuDejaChoisi("Paroisse Saint-Pierre – Ajaccio, 2A004, Corse-du-Sud"))
        assertTrue(lieuDejaChoisi("Paris, 75056, Paris"))
    }

    @Test
    fun saisieEnCoursResteOuverte() {
        assertFalse(lieuDejaChoisi("Saint"))
        assertFalse(lieuDejaChoisi("Saintes"))
        assertFalse(lieuDejaChoisi(""))
        assertFalse(lieuDejaChoisi("Saintes, 17415"))
    }
}
