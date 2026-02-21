package com.example.animera.data.model

data class AnimePage(
    val animeList: List<Anime>,
    val currentPage: Int,
    val hasNextPage: Boolean
)
