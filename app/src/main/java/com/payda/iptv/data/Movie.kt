package com.payda.iptv.data

data class Movie(
    val id: String,
    val name: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val containerExtension: String = "mp4",
    val rating: String? = null,
    val year: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val duration: String? = null,
    val favoriteId: String,
)

data class MovieCategory(
    val id: String,
    val name: String,
)

data class MovieCatalog(
    val movies: List<Movie>,
    val categories: List<MovieCategory>,
)

data class MovieProgress(
    val movieId: String,
    val positionMillis: Long,
    val durationMillis: Long,
) {
    fun shouldResume(): Boolean = positionMillis >= MinimumResumeMillis &&
        durationMillis > 0 &&
        positionMillis < durationMillis * WatchedThreshold

    companion object {
        const val MinimumResumeMillis = 30_000L
        const val WatchedThreshold = 0.95
    }
}
