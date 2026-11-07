package eu.kanade.tachiyomi.extension.en.eternalclub

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.network.get
import keiyoushi.source.KeiSource
import keiyoushi.utils.asJsoup
import okhttp3.HttpUrl

@Source
abstract class EternalClub : KeiSource() {

    override val supportsLatest = false

    private fun mangaEntry(document: org.jsoup.nodes.Document? = null): SManga =
        SManga.create().apply {
            title = "Eternal Club"
            url = "/"
            status = SManga.ONGOING
            description = document?.selectFirst("meta[name=description]")?.attr("content")
                ?.takeIf(String::isNotBlank)
                ?: "Chen Li gains the ability to buy and sell lifespan and manipulate time, then begins building a Lifespan Club."
            thumbnail_url = document?.selectFirst("meta[property=og:image]")?.attr("content")
                ?.takeIf(String::isNotBlank)
                ?: document?.selectFirst("img")?.absUrl("src")
                ?: ""
        }

    override suspend fun getPopularManga(page: Int): MangasPage {
        val document = client.get(baseUrl, Fetch.DOCUMENT).asJsoup()
        return MangasPage(listOf(mangaEntry(document)), false)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        throw UnsupportedOperationException()

    override suspend fun getSearchMangaList(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        val normalized = query.trim().lowercase()
        if (
            normalized.isNotEmpty() &&
            !listOf("eternal", "lifespan", "club").any { normalized.contains(it) }
        ) {
            return MangasPage(emptyList(), false)
        }
        return getPopularManga(page)
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.host != "eternalclubmanga.com" && url.host != "www.eternalclubmanga.com") {
            return null
        }
        return mangaEntry()
    }

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        if (!fetchDetails && !fetchChapters) return SMangaUpdate(manga, chapters)

        val document = client.get(baseUrl, Fetch.DOCUMENT).asJsoup()
        val updatedManga = if (fetchDetails) mangaEntry(document) else manga
        if (!fetchChapters) return SMangaUpdate(updatedManga, chapters)

        val chapterRegex = Regex("""(?i)chapter[\s_-]*#?([0-9]+(?:\.[0-9]+)?)""")
        val updatedChapters = document.select("a[href*='/manga/']").mapNotNull { element ->
            val href = element.absUrl("href")
            if (href.isBlank() || !href.startsWith(baseUrl)) return@mapNotNull null

            val match = chapterRegex.find(element.text() + " " + href)
                ?: return@mapNotNull null
            val number = match.groupValues[1].toFloatOrNull() ?: return@mapNotNull null

            SChapter.create().apply {
                url = href.removePrefix(baseUrl).ifBlank { "/" }
                name = element.text().trim().ifBlank { "Eternal Club Chapter $number" }
                chapter_number = number
            }
        }.distinctBy { it.url }.sortedByDescending { it.chapter_number }

        return SMangaUpdate(updatedManga, updatedChapters)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val document = client.get(getChapterUrl(chapter), Fetch.DOCUMENT).asJsoup()
        val selectors = listOf(
            "img[src*='cdn.black-clover.org']",
            "img[data-src*='cdn.black-clover.org']",
            "img[data-lazy-src*='cdn.black-clover.org']",
            ".reading-content img",
            ".reader-area img",
            ".chapter-content img",
            ".entry-content img",
            ".page-break img",
        )

        val images = selectors.flatMap(document::select).distinct()
        val urls = images.mapNotNull { image ->
            val attr = listOf("src", "data-src", "data-lazy-src", "data-original")
                .firstOrNull { image.attr(it).isNotBlank() }
                ?: return@mapNotNull null
            val imageUrl = image.absUrl(attr)
            if (
                imageUrl.isBlank() ||
                (!imageUrl.contains("cdn.black-clover.org") &&
                    !imageUrl.matches(Regex(""".+\.(jpg|jpeg|png|webp)(\?.*)?$""", RegexOption.IGNORE_CASE)))
            ) {
                return@mapNotNull null
            }
            imageUrl
        }.distinct()

        return urls.mapIndexed { index, imageUrl -> Page(index, imageUrl = imageUrl) }
    }
}
