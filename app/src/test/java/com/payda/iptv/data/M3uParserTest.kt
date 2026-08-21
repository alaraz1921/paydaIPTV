package com.payda.iptv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {
    private val parser = M3uParser()

    @Test
    fun parsesValidPlaylistWithAttributes() {
        val channels = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="demo.id" tvg-name="Demo TV" tvg-logo="https://example.com/logo.png" group-title="News" unknown="ignored",Demo Channel
            https://example.com/live/demo.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("Demo Channel", channels[0].name)
        assertEquals("https://example.com/live/demo.m3u8", channels[0].streamUrl)
        assertEquals("News", channels[0].group)
        assertEquals("https://example.com/logo.png", channels[0].logoUrl)
        assertEquals("demo.id", channels[0].tvgId)
        assertEquals("Demo TV", channels[0].tvgName)
    }

    @Test
    fun toleratesMissingLogoAndGroup() {
        val channels = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="plain.id",Plain Channel
            https://example.com/plain.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("Plain Channel", channels[0].name)
        assertNull(channels[0].logoUrl)
        assertNull(channels[0].group)
    }

    @Test
    fun usesTvgNameWhenExtinfNameIsMissing() {
        val channels = parser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-name="Fallback Name"
            https://example.com/fallback.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("Fallback Name", channels[0].name)
    }

    @Test
    fun ignoresUrlWithoutPendingExtinf() {
        val channels = parser.parse(
            """
            #EXTM3U
            https://example.com/orphan.m3u8
            #EXTINF:-1,Real Channel
            https://example.com/real.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("Real Channel", channels[0].name)
    }

    @Test
    fun returnsEmptyListWhenNoValidChannelsExist() {
        val channels = parser.parse(
            """
            #EXTM3U
            #PLAYLIST:Empty
            #EXTGRP:Unused
            """.trimIndent(),
        )

        assertEquals(emptyList<Channel>(), channels)
    }

    @Test
    fun parsesPlaylistWithoutEpgMetadata() {
        val playlist = parser.parsePlaylist(
            """
            #EXTM3U
            #EXTINF:-1,No EPG Channel
            https://example.com/no-epg.m3u8
            """.trimIndent(),
        )

        assertEquals(1, playlist.channels.size)
        assertNull(playlist.metadata.epgUrlDetected)
        assertNull(playlist.metadata.playlistName)
    }

    @Test
    fun detectsXTvGUrlFromHeader() {
        val playlist = parser.parsePlaylist(
            """
            #EXTM3U x-tvg-url="https://epg.example.test/xmltv.xml" playlist-name="Demo Playlist"
            #EXTINF:-1 tvg-id="demo",Demo Channel
            https://example.com/demo.m3u8
            """.trimIndent(),
        )

        assertEquals("https://epg.example.test/xmltv.xml", playlist.metadata.epgUrlDetected)
        assertEquals("Demo Playlist", playlist.metadata.playlistName)
    }

    @Test
    fun detectsUrlTvgFromHeader() {
        val playlist = parser.parsePlaylist(
            """
            #EXTM3U url-tvg="https://epg.example.test/guide.xml"
            #EXTINF:-1 tvg-id="demo",Demo Channel
            https://example.com/demo.m3u8
            """.trimIndent(),
        )

        assertEquals("https://epg.example.test/guide.xml", playlist.metadata.epgUrlDetected)
    }
}
