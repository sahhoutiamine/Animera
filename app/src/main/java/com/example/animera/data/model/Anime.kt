package com.example.animera.data.model

data class Anime(
    val title: String,
    val imageUrl: String,
    val detailUrl: String,
    val type: String,
    val year: String,
    val status: String,
    val statusClass: String, // "airing" or "finished"
    val rating: String       // e.g. "8.7"
)
