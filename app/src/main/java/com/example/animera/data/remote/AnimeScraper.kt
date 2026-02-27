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
        return fetchFromUrl(url, page)
    }

    fun searchAnime(query: String, page: Int): AnimePage {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = if (page == 1) {
            "https://wb.animeluxe.org/?s=$encodedQuery"
        } else {
            "https://wb.animeluxe.org/page/$page/?s=$encodedQuery"
        }
        return fetchFromUrl(url, page)
    }

    private fun fetchFromUrl(url: String, page: Int): AnimePage {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: throw Exception("Empty response body")
        response.close()
        return parseHtml(html, page)
    }

    private fun parseHtml(html: String, page: Int): AnimePage {
        val doc = Jsoup.parse(html)

        val animeList = mutableListOf<Anime>()

        // Parse each anime card: can be .anime-card (home) or .search-card (search results)
        val cards = doc.select(".anime-card, .search-card")
        for (card in cards) {
            // Title & URL
            val titleElement = card.selectFirst("div.info a h3") ?: continue
            val title = titleElement.text().trim()

            val linkElement = card.selectFirst("div.info a") ?: continue
            val detailUrl = linkElement.attr("href").trim()

            // Image URL – prefer data-src (lazy loaded), fallback to style background-image or src
            var imageUrl = ""
            val imageElement = card.selectFirst("a.image")
            if (imageElement != null) {
                imageUrl = imageElement.attr("data-src").trim()
                if (imageUrl.isEmpty()) {
                    val style = imageElement.attr("style")
                    val regex = Regex("""url\("?([^"')]+)"?\)""")
                    imageUrl = regex.find(style)?.groupValues?.get(1) ?: ""
                }
                if (imageUrl.isEmpty()) {
                    imageUrl = imageElement.selectFirst("img")?.attr("src")?.trim() ?: ""
                }
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

            // Rating
            val ratingElement = card.selectFirst("a.rating")
            val rating = ratingElement?.ownText()?.trim() ?: "N/A"

            animeList.add(
                Anime(
                    title = title,
                    imageUrl = imageUrl,
                    detailUrl = detailUrl,
                    type = type,
                    year = year,
                    status = status,
                    statusClass = statusClass,
                    rating = rating
                )
            )
        }

        // Check if there's a next page in the pagination
        val pagination = doc.selectFirst("ul.pagination, div.menu-pagination")
        val hasNextPage = pagination?.select("a")?.any { a ->
            val text = a.text().lowercase()
            text.contains("next") || text.contains("التالي") || text.contains("»")
        } ?: false

        // fallback to checking for a higher page number
        val hasHigherPage = pagination?.select("a")?.any { a ->
            val href = a.attr("href")
            val pageMatch = Regex("""/page/(\d+)/""").find(href)
            val pNum = pageMatch?.groupValues?.get(1)?.toIntOrNull()
            pNum != null && pNum > page
        } ?: false

        return AnimePage(
            animeList = animeList,
            currentPage = page,
            hasNextPage = hasNextPage || hasHigherPage
        )
    }
}
