package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Test

class TailleLisibleTest {
    @Test
    fun petites_et_grandes_tailles() {
        assertEquals("0 octets", tailleLisible(0))
        assertEquals("1023 octets", tailleLisible(1023))
        assertEquals("1 Ko", tailleLisible(1024))
        assertEquals("120 Ko", tailleLisible(120L * 1024L))
        assertEquals("2,0 Mo", tailleLisible(2L * 1024L * 1024L))
    }
}
