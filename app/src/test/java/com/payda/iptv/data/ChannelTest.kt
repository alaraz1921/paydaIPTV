package com.payda.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelTest {
    @Test
    fun stableFavoriteIdCombinesTvgIdAndStreamUrl() {
        val channel = Channel(
            name = "Demo",
            streamUrl = "http://example.com/demo.m3u8",
            tvgId = " demo.id ",
        )

        assertEquals("demo.id|http://example.com/demo.m3u8", channel.stableFavoriteId())
    }

    @Test
    fun stableFavoriteIdFallsBackToNormalizedNameAndStreamUrl() {
        val channel = Channel(
            name = " Demo Channel ",
            streamUrl = " http://example.com/demo.m3u8 ",
        )

        assertEquals("demo channel|http://example.com/demo.m3u8", channel.stableFavoriteId())
    }

    @Test
    fun stableFavoriteIdDistinguishesVariantsWithSameTvgId() {
        val hd = Channel(
            name = "Canal HD",
            streamUrl = "http://example.com/canal-hd.m3u8",
            tvgId = "canal",
        )
        val fhd = Channel(
            name = "Canal FHD",
            streamUrl = "http://example.com/canal-fhd.m3u8",
            tvgId = "canal",
        )
        val fourK = Channel(
            name = "Canal 4K",
            streamUrl = "http://example.com/canal-4k.m3u8",
            tvgId = "canal",
        )

        assertEquals(
            setOf(
                "canal|http://example.com/canal-hd.m3u8",
                "canal|http://example.com/canal-fhd.m3u8",
                "canal|http://example.com/canal-4k.m3u8",
            ),
            setOf(hd.stableFavoriteId(), fhd.stableFavoriteId(), fourK.stableFavoriteId()),
        )
    }

    @Test
    fun stableFavoriteIdUsesExplicitSourceIdentifier() {
        val channel = Channel(
            name = "Xtream Channel",
            streamUrl = "http://example.com/live/user/pass/123.ts",
            favoriteId = "xtream|http://example.com|123",
        )

        assertEquals("xtream|http://example.com|123", channel.stableFavoriteId())
    }
}
