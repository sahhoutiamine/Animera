package com.example.animera.data.repository

import com.example.animera.data.model.EpisodePlayerInfo
import com.example.animera.data.remote.EpisodeScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EpisodeRepository(private val scraper: EpisodeScraper = EpisodeScraper()) {

    suspend fun getEpisodeInfo(url: String): Result<EpisodePlayerInfo> = withContext(Dispatchers.IO) {
        try {
            val info = scraper.fetchEpisodeInfo(url)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
