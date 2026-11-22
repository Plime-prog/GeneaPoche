package com.plime.geneapoche

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticBlocageTest {
    @Test fun extraitLeFilPrincipal() {
        val trace = "----- pid 1 -----\n\"Signal Catcher\" daemon\n  at x\n\n\"main\" prio=5 tid=1 Blocked\n  at a.b.C.d(C.kt:1)\n  at e.F.g(F.kt:2)\n\n\"autre\" prio=5\n  at z\n"
        val r = extraireFilPrincipal(trace)
        assertTrue(r.startsWith("\"main\""))
        assertTrue(r.contains("a.b.C.d"))
        assertTrue(!r.contains("autre"))
    }

    @Test fun traceSansFilPrincipal() {
        assertEquals("", extraireFilPrincipal("rien"))
    }
}
