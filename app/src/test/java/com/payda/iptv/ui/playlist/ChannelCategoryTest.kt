package com.payda.iptv.ui.playlist

import com.payda.iptv.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelCategoryTest {
    @Test
    fun buildsAllCategoryAndCountsTrimmedGroups() {
        val categories = buildChannelCategories(
            listOf(
                channel("La 1", " España "),
                channel("Teledeporte", "Deportes"),
                channel("Canal Extra", "España"),
                channel("Sin grupo", null),
                channel("Vacio", "   "),
            ),
        )

        assertEquals(
            listOf(
                ChannelCategory(AllCategoryName, 5),
                ChannelCategory("España", 2),
                ChannelCategory("Deportes", 1),
                ChannelCategory(UncategorizedName, 2),
            ),
            categories,
        )
    }

    @Test
    fun preservesCategoryCaseDifferences() {
        val categories = buildChannelCategories(
            listOf(
                channel("News A", "News"),
                channel("News B", "news"),
            ),
        )

        assertEquals(
            listOf(
                ChannelCategory(AllCategoryName, 2),
                ChannelCategory("News", 1),
                ChannelCategory("news", 1),
            ),
            categories,
        )
    }

    private fun channel(name: String, group: String?): Channel = Channel(
        name = name,
        streamUrl = "https://example.com/$name.m3u8",
        group = group,
    )
}
