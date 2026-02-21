package com.example.animera.data.repository

import com.example.animera.data.model.AnimePage
import com.example.animera.data.remote.AnimeScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnimeRepository {

    private val scraper = AnimeScraper()

    suspend fun getAnimePage(page: Int): Result<AnimePage> {
        return withContext(Dispatchers.IO) {
            try {
                val animePage = scraper.fetchAnimePage(page)
                Result.success(animePage)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
