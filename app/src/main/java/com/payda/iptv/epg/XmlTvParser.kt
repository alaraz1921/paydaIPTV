package com.payda.iptv.epg

import java.io.StringReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

class XmlTvParser {
    fun parse(content: String): EpgData {
        val handler = XmlTvSaxHandler()

        try {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
            }
            factory.newSAXParser().parse(InputSource(StringReader(content)), handler)
        } catch (error: Exception) {
            throw XmlTvParseException("XMLTV invalido.", error)
        }

        val programmesByChannel = handler.programmes
            .sortedBy { it.start }
            .groupBy { it.channelId }

        return EpgData(
            channels = handler.channels,
            programmesByChannelId = programmesByChannel,
        )
    }

    private class XmlTvSaxHandler : DefaultHandler() {
        val channels = linkedMapOf<String, EpgChannel>()
        val programmes = mutableListOf<EpgProgramme>()

        private var currentChannelId: String? = null
        private var currentChannelDisplayName: String? = null
        private var currentChannelIconUrl: String? = null
        private var currentProgrammeBuilder: ProgrammeBuilder? = null
        private var currentTextTarget: TextTarget? = null
        private val textBuffer = StringBuilder()

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String,
            attributes: Attributes,
        ) {
            textBuffer.setLength(0)
            when (qName) {
                "channel" -> {
                    currentChannelId = attributes.getValue("id")?.trim()?.takeIf { it.isNotEmpty() }
                    currentChannelDisplayName = null
                    currentChannelIconUrl = null
                }
                "programme" -> {
                    currentProgrammeBuilder = ProgrammeBuilder(
                        channelId = attributes.getValue("channel")?.trim()?.takeIf { it.isNotEmpty() },
                        start = attributes.getValue("start")?.let(::parseXmlTvInstant),
                        stop = attributes.getValue("stop")?.let(::parseXmlTvInstant),
                    )
                }
                "display-name" -> currentTextTarget = TextTarget.CHANNEL_DISPLAY_NAME
                "icon" -> {
                    if (currentChannelId != null) {
                        currentChannelIconUrl = attributes.getValue("src")
                    }
                }
                "title" -> currentTextTarget = TextTarget.PROGRAMME_TITLE
                "desc" -> currentTextTarget = TextTarget.PROGRAMME_DESCRIPTION
                "category" -> currentTextTarget = TextTarget.PROGRAMME_CATEGORY
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (currentTextTarget != null) {
                textBuffer.append(ch, start, length)
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            val text = textBuffer.toString().trim().takeIf { it.isNotEmpty() }
            when (qName) {
                "display-name" -> currentChannelDisplayName = text
                "title" -> currentProgrammeBuilder?.title = text
                "desc" -> currentProgrammeBuilder?.description = text
                "category" -> currentProgrammeBuilder?.category = text
                "channel" -> {
                    val id = currentChannelId
                    if (id != null) {
                        channels[id] = EpgChannel(
                            id = id,
                            displayName = currentChannelDisplayName,
                            iconUrl = currentChannelIconUrl,
                        )
                    }
                    currentChannelId = null
                    currentChannelDisplayName = null
                    currentChannelIconUrl = null
                }
                "programme" -> {
                    currentProgrammeBuilder?.build()?.let { programmes += it }
                    currentProgrammeBuilder = null
                }
            }

            if (qName in TextTags) {
                currentTextTarget = null
                textBuffer.setLength(0)
            }
        }

        override fun error(e: SAXParseException) {
            throw e
        }

        override fun fatalError(e: SAXParseException) {
            throw e
        }
    }

    private data class ProgrammeBuilder(
        val channelId: String?,
        val start: Instant?,
        val stop: Instant?,
        var title: String? = null,
        var description: String? = null,
        var category: String? = null,
    ) {
        fun build(): EpgProgramme? {
            val validChannelId = channelId ?: return null
            val validStart = start ?: return null
            val validStop = stop ?: return null
            return EpgProgramme(
                channelId = validChannelId,
                start = validStart,
                stop = validStop,
                title = title?.takeIf { it.isNotBlank() } ?: "Programa sin titulo",
                description = description?.takeIf { it.isNotBlank() },
                category = category?.takeIf { it.isNotBlank() },
            )
        }
    }

    private enum class TextTarget {
        CHANNEL_DISPLAY_NAME,
        PROGRAMME_TITLE,
        PROGRAMME_DESCRIPTION,
        PROGRAMME_CATEGORY,
    }

    private companion object {
        val TextTags = setOf("display-name", "title", "desc", "category")
        val CompactDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val XmlTvDateFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmss")
            .optionalStart()
            .appendOffset("+HHMM", "Z")
            .optionalEnd()
            .toFormatter()

        fun parseXmlTvInstant(value: String): Instant? {
            val normalizedValue = value.trim().replace(Regex("\\s+(?=[+-]\\d{4}$)"), "")
            return runCatching {
                OffsetDateTime.parse(normalizedValue, XmlTvDateFormatter).toInstant()
            }.getOrElse {
                runCatching {
                    LocalDateTime.parse(normalizedValue.take(14), CompactDateTimeFormatter)
                        .toInstant(ZoneOffset.UTC)
                }.getOrNull()
            }
        }
    }
}

class XmlTvParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
