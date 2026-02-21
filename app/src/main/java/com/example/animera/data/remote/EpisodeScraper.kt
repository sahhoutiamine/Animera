package com.example.animera.data.remote

import android.util.Base64
import com.example.animera.data.model.EpisodeNavigation
import com.example.animera.data.model.EpisodePlayerInfo
import com.example.animera.data.model.VideoServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class EpisodeScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .build()
            chain.proceed(request)
        }
        .build()

    fun fetchEpisodeInfo(url: String): EpisodePlayerInfo {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw Exception("Empty response body")
        response.close()
        return parseEpisodeInfo(html)
    }

    private fun parseEpisodeInfo(html: String): EpisodePlayerInfo {
        val doc = Jsoup.parse(html)

        // ── Title ─────────────────────────────────────────────────────────────
        val title = doc.selectFirst(".episode-info h1")?.text()?.trim() ?: ""

        // ── Servers ───────────────────────────────────────────────────────────
        val servers = mutableListOf<VideoServer>()
        doc.select(".server-list li a").forEach { a ->
            val name = a.text().trim()
            val encodedUrl = a.attr("data-url").trim()
            val type = a.attr("data-type").trim()
            if (encodedUrl.isNotEmpty()) {
                try {
                    val decodedUrl = String(Base64.decode(encodedUrl, Base64.DEFAULT))
                    if (decodedUrl.startsWith("http")) {
                        servers.add(VideoServer(name, decodedUrl, type))
                    }
                } catch (e: Exception) {
                    // Ignore decoding errors
                }
            }
        }

        // ── Navigation (Next/Prev) ───────────────────────────────────────────
        val nextEpisodeUrl = doc.selectFirst("a.ctrl.next")?.attr("href")?.trim()
        val animeDetailUrl = doc.selectFirst("a.ctrl.eps")?.attr("href")?.trim()
        // There is no explicit "prev" button in the provided sample controls, 
        // but let's check if it exists in another form or if we can infer it.
        // Actually, sometimes it's "prev" class.
        val prevEpisodeUrl = doc.selectFirst("a.ctrl.prev")?.attr("href")?.trim()

        // ── Episodes List ─────────────────────────────────────────────────────
        val episodes = mutableListOf<EpisodeNavigation>()
        doc.select("ul.episodes-list li").forEach { li ->
            val a = li.selectFirst("a") ?: return@forEach
            val epUrl = a.attr("href").trim()
            val epTitle = a.ownText().trim()
            val isActive = li.hasClass("active")
            episodes.add(EpisodeNavigation(epTitle, epUrl, isActive))
        }

        return EpisodePlayerInfo(
            title = title,
            servers = servers,
            nextEpisodeUrl = nextEpisodeUrl,
            prevEpisodeUrl = prevEpisodeUrl,
            animeDetailUrl = animeDetailUrl,
            episodes = episodes
        )
    }
}
