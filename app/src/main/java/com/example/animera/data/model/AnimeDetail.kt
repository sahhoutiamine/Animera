package com.example.animera.data.model

data class AnimeDetail(
    val title: String,
    val altTitle: String,
    val coverImageUrl: String,
    val status: String,
    val statusClass: String,       // "airing" | "finished"
    val type: String,
    val year: String,
    val season: String,
    val source: String,
    val studio: String,
    val genres: List<String>,
    val synopsis: String,
    val trailerUrl: String,
    val malUrl: String,
    val episodes: List<Episode>,
    val relatedAnime: List<RelatedAnime>
)
