package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Test

class ChronologieTest {

    private fun ids(l: List<Pair<Long, String>>) = l.map { it.second }
    private val sans = -10000L

    @Test
    fun tout_date_est_range_par_date_actes_et_evenements_meles() {
        val e = listOf(
            sans.let { 18900000L } to "evl:1",       // profession 1890
            18500305L to "ev:N",
            19200101L to "ev:D",
            18800000L to "enf:7",
            18750620L to "ev:M"
        )
        assertEquals(listOf("ev:N", "ev:M", "enf:7", "evl:1", "ev:D"), ids(ordonnerChronologie(e)))
    }

    @Test
    fun deces_sans_date_reste_en_dernier_meme_si_les_enfants_sont_dates() {
        val e = listOf(
            18500305L to "ev:N", 18750620L to "ev:M", sans to "ev:D",
            18800000L to "enf:7", 18900000L to "evl:1"
        )
        assertEquals(listOf("ev:N", "ev:M", "enf:7", "evl:1", "ev:D"), ids(ordonnerChronologie(e)))
    }

    @Test
    fun naissance_sans_date_en_tete_et_bapteme_juste_apres() {
        val e = listOf(
            sans to "ev:N", sans to "ev:B", 18750620L to "ev:M", 18800000L to "enf:7"
        )
        assertEquals(listOf("ev:N", "ev:B", "ev:M", "enf:7"), ids(ordonnerChronologie(e)))
    }

    @Test
    fun mariage_sans_date_apres_la_naissance_avant_les_autres() {
        val e = listOf(18500305L to "ev:N", sans to "ev:M", 18800000L to "enf:7", 18990000L to "ev:D")
        assertEquals(listOf("ev:N", "ev:M", "enf:7", "ev:D"), ids(ordonnerChronologie(e)))
    }

    @Test
    fun sepulture_apres_deces_et_evenements_sans_date_apres_les_dates() {
        val e = listOf(
            18500305L to "ev:N", 19200101L to "ev:D", sans to "ev:S", sans to "evl:2", 18900000L to "evl:1"
        )
        assertEquals(listOf("ev:N", "evl:1", "ev:D", "ev:S", "evl:2"), ids(ordonnerChronologie(e)))
    }

    @Test
    fun a_date_egale_les_actes_passent_avant_les_autres_dans_l_ordre_nbmds() {
        val e = listOf(18500000L to "evl:1", 18500000L to "ev:B", 18500000L to "ev:N")
        assertEquals(listOf("ev:N", "ev:B", "evl:1"), ids(ordonnerChronologie(e)))
    }

    @Test
    fun rien_ou_tout_sans_date() {
        assertEquals(emptyList<String>(), ids(ordonnerChronologie(emptyList())))
        val e = listOf(sans to "ev:S", sans to "ev:D", sans to "ev:M", sans to "ev:N", sans to "evl:1")
        assertEquals(listOf("ev:N", "ev:M", "evl:1", "ev:D", "ev:S"), ids(ordonnerChronologie(e)))
    }
}
