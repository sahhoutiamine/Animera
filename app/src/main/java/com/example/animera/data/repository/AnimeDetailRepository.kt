package com.example.animera.data.repository

import com.example.animera.data.model.AnimeDetail
import com.example.animera.data.remote.AnimeDetailScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnimeDetailRepository {

    private val scraper = AnimeDetailScraper()

    suspend fun getAnimeDetail(url: String): Result<AnimeDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val detail = scraper.fetchDetail(url)
                Result.success(detail)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
