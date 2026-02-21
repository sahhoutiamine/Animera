package com.example.animera.data.remote

import com.example.animera.data.model.AnimeDetail
import com.example.animera.data.model.Episode
import com.example.animera.data.model.RelatedAnime
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class AnimeDetailScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept-Language", "ar,en;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
            chain.proceed(request)
        }
        .build()

    fun fetchDetail(url: String): AnimeDetail {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw Exception("Empty response body")
        response.close()
        return parseDetail(html)
    }

    private fun parseDetail(html: String): AnimeDetail {
        val doc = Jsoup.parse(html)

        // ── Title & Alt Title ──────────────────────────────────────────────────
        val title = doc.selectFirst("div.media-title h1")?.text()?.trim() ?: ""
        val altTitle = doc.selectFirst("div.media-title h3")?.text()?.trim() ?: ""

        // ── Cover Image ───────────────────────────────────────────────────────
        val coverEl = doc.selectFirst("aside .anime-card .image")
        var coverImageUrl = coverEl?.attr("data-src")?.trim() ?: ""
        if (coverImageUrl.isEmpty()) {
            val style = coverEl?.attr("style") ?: ""
            val regex = Regex("""url\("?([^"')]+)"?\)""")
            coverImageUrl = regex.find(style)?.groupValues?.get(1) ?: ""
        }

        // ── Status ────────────────────────────────────────────────────────────
        val statusEl = doc.selectFirst("aside .status a")
        val status = statusEl?.text()?.trim() ?: ""
        val statusClass = when {
            doc.selectFirst("aside .status.success-bg") != null -> "airing"
            doc.selectFirst("aside .status.danger-bg") != null -> "finished"
            else -> "unknown"
        }

        // ── Media Info (type, year, season, source, studio) ───────────────────
        var type = ""
        var year = ""
        var season = ""
        var source = ""
        var studio = ""
        doc.select("ul.media-info li").forEach { li ->
            val text = li.text()
            val value = li.selectFirst("span, a")?.text()?.trim() ?: ""
            when {
                text.contains("النوع") -> type = value
                text.contains("سنة العرض") -> year = value
                text.contains("الموسم") -> season = value
                text.contains("المصدر") -> source = value
                text.contains("الأستوديو") || text.contains("الاستوديو") -> studio = value
            }
        }

        // ── Genres ────────────────────────────────────────────────────────────
        val genres = doc.select(".genres a.badge").map { it.text().trim() }

        // ── Synopsis ──────────────────────────────────────────────────────────
        val synopsis = doc.selectFirst(".media-story .content p")?.text()?.trim() ?: ""

        // ── Trailer & MAL ─────────────────────────────────────────────────────
        val trailerUrl = doc.selectFirst("a.btn-trailer")?.attr("href")?.trim() ?: ""
        val malUrl = doc.selectFirst("a.btn.mal")?.attr("href")?.trim() ?: ""

        // ── Episodes ──────────────────────────────────────────────────────────
        val episodes = mutableListOf<Episode>()
        doc.select("ul.episodes-lists li").forEach { li ->
            val watchUrl = li.selectFirst("a.read-btn")?.attr("href")?.trim()
                ?: li.selectFirst("a")?.attr("href")?.trim() ?: return@forEach

            val thumbEl = li.selectFirst("a.image")
            var thumbnailUrl = thumbEl?.attr("data-src")?.trim() ?: ""
            if (thumbnailUrl.isEmpty()) {
                val style = thumbEl?.attr("style") ?: ""
                val regex = Regex("""url\("?([^"')]+)"?\)""")
                thumbnailUrl = regex.find(style)?.groupValues?.get(1) ?: ""
            }

            val titleEl = li.selectFirst("a.title h3")
            val rawTitle = titleEl?.text()?.trim() ?: ""
            // Extract episode number from title (e.g. "الحلقة 4")
            val numberMatch = Regex("""(\d+)""").find(rawTitle)
            val number = numberMatch?.groupValues?.get(1) ?: ""
            val epTitle = rawTitle.split(" ").firstOrNull { it.any { c -> c.isDigit() } }
                ?.let { rawTitle } ?: rawTitle

            episodes.add(Episode(number = number, title = rawTitle, thumbnailUrl = thumbnailUrl, watchUrl = watchUrl))
        }

        // ── Related Anime ─────────────────────────────────────────────────────
        val relatedAnime = mutableListOf<RelatedAnime>()
        doc.select(".related-subjects .anime-card").forEach { card ->
            val relTitle = card.selectFirst("div.info a h3")?.text()?.trim() ?: return@forEach
            val relUrl = card.selectFirst("div.info a")?.attr("href")?.trim() ?: return@forEach
            val relImageEl = card.selectFirst("a.image")
            var relImage = relImageEl?.attr("data-src")?.trim() ?: ""
            if (relImage.isEmpty()) {
                val style = relImageEl?.attr("style") ?: ""
                val regex = Regex("""url\("?([^"')]+)"?\)""")
                relImage = regex.find(style)?.groupValues?.get(1) ?: ""
            }
            val relType = card.selectFirst("span.anime-type")?.text()?.trim() ?: ""
            val relYear = card.selectFirst("span.anime-aired")?.text()?.trim() ?: ""
            val relRating = card.selectFirst("a.rating")?.ownText()?.trim() ?: ""

            relatedAnime.add(
                RelatedAnime(
                    title = relTitle,
                    imageUrl = relImage,
                    detailUrl = relUrl,
                    type = relType,
                    year = relYear,
                    rating = relRating
                )
            )
        }

        return AnimeDetail(
            title = title,
            altTitle = altTitle,
            coverImageUrl = coverImageUrl,
            status = status,
            statusClass = statusClass,
            type = type,
            year = year,
            season = season,
            source = source,
            studio = studio,
            genres = genres,
            synopsis = synopsis,
            trailerUrl = trailerUrl,
            malUrl = malUrl,
            episodes = episodes,
            relatedAnime = relatedAnime
        )
    }
}
