package com.payda.iptv.data

data class Series(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val year: String? = null,
    val backdropUrl: String? = null,
    val youtubeTrailer: String? = null,
    val favoriteId: String,
)

data class SeriesCategory(
    val id: String,
    val name: String,
)

data class SeriesCatalog(
    val series: List<Series>,
    val categories: List<SeriesCategory>,
)

data class Season(
    val id: String,
    val number: Int?,
    val name: String,
    val posterUrl: String? = null,
)

data class Episode(
    val id: String,
    val seriesId: String,
    val seasonId: String,
    val seasonNumber: Int?,
    val number: Int?,
    val title: String,
    val streamUrl: String,
    val extension: String,
    val plot: String? = null,
    val duration: String? = null,
    val imageUrl: String? = null,
    val releaseDate: String? = null,
    val progressId: String,
)

data class SeriesDetail(
    val series: Series,
    val seasons: List<Season>,
    val episodesBySeasonId: Map<String, List<Episode>>,
)

data class EpisodeProgress(
    val episodeId: String,
    val positionMillis: Long,
    val durationMillis: Long,
    val watched: Boolean = false,
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
) {
    fun shouldResume(): Boolean = !watched &&
        positionMillis >= MinimumResumeMillis &&
        durationMillis > 0 &&
        positionMillis < durationMillis * WatchedThreshold

    companion object {
        const val MinimumResumeMillis = 30_000L
        const val WatchedThreshold = 0.95
    }
}
