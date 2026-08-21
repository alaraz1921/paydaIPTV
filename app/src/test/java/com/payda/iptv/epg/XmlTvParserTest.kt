package com.payda.iptv.epg

import com.payda.iptv.data.Channel
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class XmlTvParserTest {
    private val parser = XmlTvParser()

    @Test
    fun parsesChannelsAndProgrammesWithOffset() {
        val epgData = parser.parse(
            """
            <tv>
              <channel id="La1.es">
                <display-name>La 1</display-name>
                <icon src="https://example.com/la1.png" />
              </channel>
              <programme channel="La1.es" start="20260820140000 +0200" stop="20260820150000 +0200">
                <title>Telediario</title>
                <desc>Noticias</desc>
                <category>Noticias</category>
              </programme>
            </tv>
            """.trimIndent(),
        )

        assertEquals("La 1", epgData.channels["La1.es"]?.displayName)
        val programme = epgData.programmesByChannelId.getValue("La1.es").first()
        assertEquals("Telediario", programme.title)
        assertEquals(Instant.parse("2026-08-20T12:00:00Z"), programme.start)
        assertEquals(Instant.parse("2026-08-20T13:00:00Z"), programme.stop)
    }

    @Test
    fun returnsCurrentAndNextProgrammeByTvgId() {
        val epgData = parser.parse(
            """
            <tv>
              <programme channel="La1.es" start="20260820140000 +0200" stop="20260820150000 +0200">
                <title>Telediario</title>
              </programme>
              <programme channel="La1.es" start="20260820150000 +0200" stop="20260820160000 +0200">
                <title>La Moderna</title>
              </programme>
            </tv>
            """.trimIndent(),
        )
        val channel = Channel(
            name = "La 1",
            streamUrl = "https://example.com/la1.m3u8",
            tvgId = "La1.es",
        )

        val epgInfo = epgData.programmeFor(
            channel = channel,
            now = Instant.parse("2026-08-20T12:30:00Z"),
        )

        assertEquals("Telediario", epgInfo?.current?.title)
        assertEquals("La Moderna", epgInfo?.next?.title)
    }

    @Test
    fun returnsNullWhenChannelHasNoTvgMatch() {
        val epgData = parser.parse(
            """
            <tv>
              <programme channel="Other.es" start="20260820140000 +0200" stop="20260820150000 +0200">
                <title>Other</title>
              </programme>
            </tv>
            """.trimIndent(),
        )

        val epgInfo = epgData.programmeFor(
            channel = Channel(name = "La 1", streamUrl = "https://example.com/la1.m3u8", tvgId = "La1.es"),
            now = Instant.parse("2026-08-20T12:30:00Z"),
        )

        assertNull(epgInfo)
    }

    @Test
    fun throwsForMalformedXml() {
        assertThrows(XmlTvParseException::class.java) {
            parser.parse("<tv><programme></tv>")
        }
    }
}
