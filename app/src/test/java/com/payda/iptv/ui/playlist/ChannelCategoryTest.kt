package com.payda.iptv.ui.playlist

import com.payda.iptv.data.Channel
import com.payda.iptv.data.stableFavoriteId
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelCategoryTest {
    @Test
    fun buildsAllCategoryAndCountsTrimmedGroups() {
        val favoriteId = "http://example.com/la-1.m3u8"
        val categories = buildChannelCategories(
            channels = listOf(
                channel("La 1", " España "),
                channel("Teledeporte", "Deportes"),
                channel("Canal Extra", "España"),
                channel("Sin grupo", null),
                channel("Vacio", "   "),
            ),
            favoriteChannelIds = setOf(favoriteId),
        )

        assertEquals(
            listOf(
                ChannelCategory(AllCategoryName, 5),
                ChannelCategory(FavoriteCategoryName, 1),
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
                ChannelCategory(FavoriteCategoryName, 0),
                ChannelCategory("News", 1),
                ChannelCategory("news", 1),
            ),
            categories,
        )
    }

    @Test
    fun filtersByCategoryAndSearchQuery() {
        val channels = listOf(
            channel("Real Madrid TV", "Deportes"),
            channel("Barca TV", "Deportes"),
            channel("Real Cinema", "Cine"),
        )

        val filteredChannels = filterChannels(
            channels = channels,
            selectedCategoryName = "Deportes",
            searchQuery = " real ",
            favoriteChannelIds = emptySet(),
        )

        assertEquals(listOf("Real Madrid TV"), filteredChannels.map { it.name })
    }

    @Test
    fun filtersFavoriteCategoryBeforeSearch() {
        val favorite = channel("Real Madrid TV", "Deportes")
        val channels = listOf(
            favorite,
            channel("Real Cinema", "Cine"),
        )

        val filteredChannels = filterChannels(
            channels = channels,
            selectedCategoryName = FavoriteCategoryName,
            searchQuery = "real",
            favoriteChannelIds = setOf(favorite.stableFavoriteId()),
        )

        assertEquals(listOf("Real Madrid TV"), filteredChannels.map { it.name })
    }

    private fun channel(name: String, group: String?): Channel = Channel(
        name = name,
        streamUrl = "http://example.com/${name.lowercase().replace(" ", "-")}.m3u8",
        group = group,
    )
}
