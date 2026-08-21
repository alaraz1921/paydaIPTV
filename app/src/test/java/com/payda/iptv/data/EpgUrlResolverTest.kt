package com.payda.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgUrlResolverTest {
    @Test
    fun returnsNoEpgWhenManualAndDetectedUrlsAreMissing() {
        val resolution = resolveEpgUrl(
            manualUrl = "",
            manualConfigured = false,
            detectedUrl = null,
        )

        assertNull(resolution.url)
        assertNull(resolution.source)
        assertFalse(resolution.ignoredManualUrl)
    }

    @Test
    fun usesDetectedEpgUrlWhenManualUrlIsEmpty() {
        val resolution = resolveEpgUrl(
            manualUrl = "",
            manualConfigured = false,
            detectedUrl = "https://provider.test/xmltv.xml",
        )

        assertEquals("https://provider.test/xmltv.xml", resolution.url)
        assertEquals(EpgUrlSource.AUTO, resolution.source)
    }

    @Test
    fun manualEpgUrlHasPriorityWhenExplicitlyConfigured() {
        val resolution = resolveEpgUrl(
            manualUrl = "https://manual.test/xmltv.xml",
            manualConfigured = true,
            detectedUrl = "https://auto.test/xmltv.xml",
        )

        assertEquals("https://manual.test/xmltv.xml", resolution.url)
        assertEquals(EpgUrlSource.MANUAL, resolution.source)
    }

    @Test
    fun ignoresBlankManualUrlEvenWhenItWasConfigured() {
        val resolution = resolveEpgUrl(
            manualUrl = "   ",
            manualConfigured = true,
            detectedUrl = "https://auto.test/xmltv.xml",
        )

        assertEquals("https://auto.test/xmltv.xml", resolution.url)
        assertEquals(EpgUrlSource.AUTO, resolution.source)
        assertFalse(resolution.ignoredManualUrl)
    }

    @Test
    fun ignoresPlaceholderUrls() {
        assertFalse(isUsableEpgUrl("http://"))
        assertFalse(isUsableEpgUrl("https://"))
        assertFalse(isUsableEpgUrl("<epg-url>"))
        assertFalse(isUsableEpgUrl("https://example.com/xmltv.xml"))
    }

    @Test
    fun reportsIgnoredManualUrlWhenNoDetectedFallbackExists() {
        val resolution = resolveEpgUrl(
            manualUrl = "http://",
            manualConfigured = true,
            detectedUrl = null,
        )

        assertNull(resolution.url)
        assertNull(resolution.source)
        assertTrue(resolution.ignoredManualUrl)
    }

    @Test
    fun ignoresInvalidAutomaticEpgUrl() {
        val resolution = resolveEpgUrl(
            manualUrl = "",
            manualConfigured = false,
            detectedUrl = "https://",
        )

        assertNull(resolution.url)
        assertNull(resolution.source)
    }
}
