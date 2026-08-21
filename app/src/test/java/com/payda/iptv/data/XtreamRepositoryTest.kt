package com.payda.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamRepositoryTest {
    @Test
    fun normalizesXtreamServerWithoutChangingProtocol() {
        assertEquals(
            "http://provider.test:8080",
            normalizeXtreamServer(" http://provider.test:8080/ "),
        )
        assertEquals(
            "https://provider.test",
            normalizeXtreamServer("https://provider.test/"),
        )
    }

    @Test
    fun rejectsInvalidXtreamServer() {
        val result = runCatching { normalizeXtreamServer("provider.test:8080") }

        assertTrue(result.isFailure)
        assertEquals("Servidor Xtream invalido.", result.exceptionOrNull()?.message)
    }
}
