package com.example.animera.data.model

data class EpisodePlayerInfo(
    val title: String,
    val servers: List<VideoServer>,
    val nextEpisodeUrl: String?,
    val prevEpisodeUrl: String?,
    val animeDetailUrl: String?,
    val episodes: List<EpisodeNavigation>
)

data class EpisodeNavigation(
    val title: String,
    val url: String,
    val isActive: Boolean
)
