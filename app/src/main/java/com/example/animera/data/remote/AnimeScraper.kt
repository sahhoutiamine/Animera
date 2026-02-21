package com.example.animera.data.remote

import com.example.animera.data.model.Anime
import com.example.animera.data.model.AnimePage
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class AnimeScraper {

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

    companion object {
        private const val BASE_URL = "https://wb.animeluxe.org/anime/"
    }

    fun fetchAnimePage(page: Int): AnimePage {
        val url = if (page == 1) BASE_URL else "${BASE_URL}page/$page/"

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw Exception("Empty response body")
        response.close()

        return parseHtml(html, page)
    }

    private fun parseHtml(html: String, page: Int): AnimePage {
        val doc = Jsoup.parse(html)

        val animeList = mutableListOf<Anime>()

        // Parse each anime card: .box-5x1 .anime-card
        val cards = doc.select(".box-5x1 .anime-card")
        for (card in cards) {
            // Title & URL
            val titleElement = card.selectFirst("div.info a h3") ?: continue
            val title = titleElement.text().trim()

            val linkElement = card.selectFirst("div.info a") ?: continue
            val detailUrl = linkElement.attr("href").trim()

            // Image URL – prefer data-src (lazy loaded), fallback to style background-image
            val imageElement = card.selectFirst("a.image")
            var imageUrl = imageElement?.attr("data-src")?.trim() ?: ""
            if (imageUrl.isEmpty()) {
                // Try to parse from style attribute: background-image: url("...")
                val style = imageElement?.attr("style") ?: ""
                val regex = Regex("""url\("?([^"')]+)"?\)""")
                imageUrl = regex.find(style)?.groupValues?.get(1) ?: ""
            }

            // Type and Year
            val typeElement = card.selectFirst("span.anime-type")
            val yearElement = card.selectFirst("span.anime-aired")
            val type = typeElement?.text()?.trim() ?: ""
            val year = yearElement?.text()?.trim() ?: ""

            // Status
            val statusElement = card.selectFirst("div.anime-status a")
            val status = statusElement?.text()?.trim() ?: ""
            val statusClass = when {
                statusElement?.hasClass("success-bg") == true -> "airing"
                statusElement?.hasClass("danger-bg") == true -> "finished"
                else -> "unknown"
            }

            animeList.add(
                Anime(
                    title = title,
                    imageUrl = imageUrl,
                    detailUrl = detailUrl,
                    type = type,
                    year = year,
                    status = status,
                    statusClass = statusClass
                )
            )
        }

        // Check if there's a next page in the pagination
        val pagination = doc.selectFirst("ul.pagination")
        val hasNextPage = pagination?.selectFirst("a[href*=\"/page/\"]") != null ||
                pagination?.children()?.any { el ->
                    el.selectFirst("a") != null &&
                            el.selectFirst("a")?.attr("href")?.contains("/page/") == true
                } == true

        // More precise check: look for a page number greater than current
        val hasNext = pagination?.select("a")?.any { a ->
            val href = a.attr("href")
            val pageMatch = Regex("""/page/(\d+)/""").find(href)
            pageMatch?.groupValues?.get(1)?.toIntOrNull()?.let { it > page } ?: false
        } ?: false

        return AnimePage(
            animeList = animeList,
            currentPage = page,
            hasNextPage = hasNext || hasNextPage
        )
    }
}
