package com.payda.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelTest {
    @Test
    fun stableFavoriteIdPrefersTvgId() {
        val channel = Channel(
            name = "Demo",
            streamUrl = "http://example.com/demo.m3u8",
            tvgId = " demo.id ",
        )

        assertEquals("demo.id", channel.stableFavoriteId())
    }

    @Test
    fun stableFavoriteIdFallsBackToStreamUrl() {
        val channel = Channel(
            name = "Demo",
            streamUrl = " http://example.com/demo.m3u8 ",
        )

        assertEquals("http://example.com/demo.m3u8", channel.stableFavoriteId())
    }
}
