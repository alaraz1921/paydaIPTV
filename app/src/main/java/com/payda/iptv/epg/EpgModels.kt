package com.payda.iptv.epg

import com.payda.iptv.data.Channel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class EpgChannel(
    val id: String,
    val displayName: String?,
    val iconUrl: String?,
)

data class EpgProgramme(
    val channelId: String,
    val start: Instant,
    val stop: Instant,
    val title: String,
    val description: String? = null,
    val category: String? = null,
)

data class EpgData(
    val channels: Map<String, EpgChannel>,
    val programmesByChannelId: Map<String, List<EpgProgramme>>,
) {
    fun programmeFor(
        channel: Channel,
        now: Instant = Instant.now(),
    ): ChannelEpgInfo? {
        val channelId = channel.tvgId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val programmes = programmesByChannelId[channelId].orEmpty()
        if (programmes.isEmpty()) return null

        val current = programmes.firstOrNull { programme ->
            !programme.start.isAfter(now) && programme.stop.isAfter(now)
        }
        val next = if (current != null) {
            programmes.firstOrNull { it.start >= current.stop }
        } else {
            programmes.firstOrNull { it.start.isAfter(now) }
        }

        return if (current == null && next == null) {
            null
        } else {
            ChannelEpgInfo(current = current, next = next)
        }
    }
}

data class ChannelEpgInfo(
    val current: EpgProgramme?,
    val next: EpgProgramme?,
)

fun EpgProgramme.timeRangeText(
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zoneId)
    return "${formatter.format(start)} - ${formatter.format(stop)}"
}
