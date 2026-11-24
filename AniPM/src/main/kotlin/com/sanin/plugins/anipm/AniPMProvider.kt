package com.sanin.plugins.anipm

import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addDubStatus
import com.lagradost.cloudstream3.addEpisodes
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder

object AniPMApi {
    private const val SETTLAR_EMBED = "https://embed.settlar.io"
    private val domains = listOf("https://ani.pm", "https://ani.nz", "https://ani.vg")

    @Volatile
    private var domainIndex = 0

    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    fun url(): String = domains[domainIndex]

    private fun headers(referer: String): Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Accept" to "application/json, text/plain, */*",
        "Referer" to referer,
    )

    private suspend fun getJson(url: String, referer: String): String? = try {
        val response = app.get(
            url,
            headers = headers(referer),
            timeout = 30_000L,
        )
        response.text.takeIf { response.code == 200 && it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private suspend fun getText(path: String): String? {
        domains.indices.forEach { offset ->
            val index = (domainIndex + offset) % domains.size
            val base = domains[index]
            val body = getJson(base + path, "$base/") ?: return@forEach
            domainIndex = index
            return body
        }
        return null
    }

    private suspend inline fun <reified T : Any> getPathObject(path: String): T? {
        val body = getText(path) ?: return null
        return runCatching { parseJson<T>(body) }.getOrNull()
    }

    private suspend inline fun <reified T : Any> getObject(url: String, referer: String): T? {
        val body = getJson(url, referer) ?: return null
        return runCatching { parseJson<T>(body) }.getOrNull()
    }

    fun absolute(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http")) url else url() + url
    }

    suspend fun search(query: String): AniPMListResponse? {
        if (query.length < 2) return null
        val body = getText("/api/anime/search?q=${encode(query)}") ?: return null
        return runCatching { parseJson<AniPMListResponse>(body) }.getOrNull()
    }

    suspend fun browse(sort: String, page: Int, format: String? = null): AniPMListResponse? {
        val path = buildString {
            append("/api/anime/browse?sort=$sort&page=$page&limit=30")
            if (!format.isNullOrBlank()) append("&format=$format")
        }
        val body = getText(path) ?: return null
        return runCatching { parseJson<AniPMListResponse>(body) }.getOrNull()
    }

    suspend fun latest(page: Int): AniPMLatestResponse? {
        val body = getText("/api/anime/latest-episodes?page=$page") ?: return null
        return runCatching { parseJson<AniPMLatestResponse>(body) }.getOrNull()
    }

    suspend fun series(id: Int): AniPMSeries? {
        val body = getText("/api/anime/series/$id?routes=e3") ?: return null
        return runCatching { parseJson<AniPMSeries>(body) }.getOrNull()
    }

    suspend fun settlarSelection(
        seriesId: Int,
        anilistId: String?,
        episode: Int,
        channel: String,
    ): String? {
        val primary = getPathObject<AniPMBootstrap>(
            "/api/anime/playback-bootstrap/settlar/$seriesId?ep=$episode&lang=$channel&backup=1"
        )?.settlarSelection?.takeIf { it.isNotBlank() }
        if (!primary.isNullOrBlank()) return primary
        if (anilistId.isNullOrBlank()) return null
        return getPathObject<AniPMBootstrap>(
            "/api/anime/playback-bootstrap/anilist/$anilistId?ep=$episode&lang=$channel&core=0"
        )?.settlarSelection?.takeIf { it.isNotBlank() }
    }

    suspend fun settlarSession(selection: String, episode: Int, channel: String): String? {
        val path = "/api/anime/settlar/session?selection=${encode(selection)}" +
            "&provider=anipm&ep=$episode&channel=$channel&telemetry=0"
        return getPathObject<AniPMEmbedSession>(path)?.embedUrl?.takeIf { it.startsWith("http") }
    }

    suspend fun settlarResolve(embedUrl: String): SettlarStream? {
        val token = Regex("t=([^&]+)").find(embedUrl)?.groupValues?.getOrNull(1) ?: return null
        return getObject<SettlarStream>(
            "$SETTLAR_EMBED/api/embed/session?t=$token",
            "$SETTLAR_EMBED/embed/v1",
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

class AniPMProvider : MainAPI() {
    override var mainUrl = AniPMApi.url()
    override var name = "AniPM"
    override var lang = "en"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)

    override val mainPage = mainPageOf(
        "trending" to "Trending",
        "popular" to "Popular",
        "score" to "Top Rated",
        "movies" to "Movies",
        "latest" to "Latest Episodes",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        mainUrl = AniPMApi.url()
        return when (request.data) {
            "latest" -> {
                val response = AniPMApi.latest(page)
                val items = response?.items.orEmpty().mapNotNull { item ->
                    val id = item.id ?: return@mapNotNull null
                    val title = item.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    newAnimeSearchResponse(title, "$mainUrl|$id", TvType.Anime, fix = false) {
                        posterUrl = AniPMApi.absolute(item.poster)
                        addDubStatus(dubExist = item.dub == true, subExist = item.sub == true)
                    }
                }
                newHomePageResponse(request.name, items, hasNext = response?.hasNextPage == true)
            }

            "movies" -> browsePage("popular", page, "Movie", request.name)
            else -> browsePage(request.data, page, null, request.name)
        }
    }

    private suspend fun browsePage(
        sort: String,
        page: Int,
        format: String?,
        sectionName: String,
    ): HomePageResponse {
        val response = AniPMApi.browse(sort, page, format)
        val items = response?.items.orEmpty().mapNotNull { titleResponse(it) }
        return newHomePageResponse(sectionName, items, hasNext = response?.hasNextPage == true)
    }

    private fun titleResponse(item: AniPMTitle): SearchResponse? {
        val id = item.id ?: return null
        val title = item.title?.takeIf { it.isNotBlank() } ?: return null
        return newAnimeSearchResponse(title, "$mainUrl|$id", TvType.Anime, fix = false) {
            posterUrl = AniPMApi.absolute(item.poster)
            year = item.year
            addDubStatus(
                dubExist = (item.dubCount ?: 0) > 0,
                subExist = (item.subCount ?: 0) > 0,
            )
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        mainUrl = AniPMApi.url()
        return AniPMApi.search(query)?.items.orEmpty().mapNotNull { titleResponse(it) }
    }

    override suspend fun load(url: String): com.lagradost.cloudstream3.LoadResponse? {
        mainUrl = AniPMApi.url()
        val id = url.substringAfterLast("|").trim().toIntOrNull() ?: return null
        val series = AniPMApi.series(id) ?: return null
        val title = series.title?.takeIf { it.isNotBlank() } ?: return null

        fun episodeData(number: Int, channel: String): String =
            "$mainUrl|$id|$number|$channel|${series.anilistId.orEmpty()}"

        fun episodeList(channel: String): List<Episode> =
            series.episodes.orEmpty().mapNotNull { episode ->
                val number = episode.number ?: return@mapNotNull null
                if (number < 1) return@mapNotNull null
                if (if (channel == "dub") episode.dub != true else episode.sub != true) {
                    return@mapNotNull null
                }
                newEpisode(episodeData(number, channel)) {
                    this.name = episode.title
                        ?.takeIf { it.isNotBlank() && !Regex("""^Episode \d+(\.\d+)?$""").matches(it) }
                        ?: "Episode $number"
                    this.episode = number
                    this.description = episode.description?.takeIf { it.isNotBlank() }
                    this.posterUrl = AniPMApi.absolute(episode.thumbnail)
                }
            }

        val dubEpisodes = episodeList("dub")
        val subEpisodes = episodeList("sub")
        val type = when (series.type?.lowercase()) {
            "movie" -> TvType.AnimeMovie
            "ova", "ona", "special", "music" -> TvType.OVA
            else -> TvType.Anime
        }
        val status = when {
            series.status?.startsWith("releasing", true) == true -> ShowStatus.Ongoing
            series.status?.startsWith("ongoing", true) == true -> ShowStatus.Ongoing
            series.status?.startsWith("finished", true) == true -> ShowStatus.Completed
            series.status?.startsWith("completed", true) == true -> ShowStatus.Completed
            else -> null
        }

        return newAnimeLoadResponse(title, url, type) {
            posterUrl = AniPMApi.absolute(series.poster)
            backgroundPosterUrl = AniPMApi.absolute(series.banner)
            year = series.year
            plot = series.synopsis?.replace(Regex("<[^>]*>"), "")
            tags = series.genres.orEmpty()
            status?.let { showStatus = it }
            if (subEpisodes.isNotEmpty()) addEpisodes(DubStatus.Subbed, subEpisodes)
            if (dubEpisodes.isNotEmpty()) addEpisodes(DubStatus.Dubbed, dubEpisodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val parts = data.split("|")
        val seriesId = parts.getOrNull(1)?.toIntOrNull() ?: return false
        val episode = parts.getOrNull(2)?.toIntOrNull() ?: return false
        val channel = if (parts.getOrNull(3) == "dub") "dub" else "sub"
        val anilistId = parts.getOrNull(4)
        val selection = AniPMApi.settlarSelection(seriesId, anilistId, episode, channel)
            ?: return false
        val embedUrl = AniPMApi.settlarSession(selection, episode, channel) ?: return false
        val stream = AniPMApi.settlarResolve(embedUrl) ?: return false
        val master = stream.source?.takeIf { it.startsWith("https://") } ?: return false

        val headers = mapOf(
            "User-Agent" to AniPMApi.USER_AGENT,
            "Referer" to "https://embed.settlar.io/",
        )
        val seenSubtitles = HashSet<String>()
        val languageMap = mapOf(
            "eng" to "en", "jpn" to "ja", "spa" to "es", "fra" to "fr", "fre" to "fr",
            "deu" to "de", "ger" to "de", "por" to "pt", "ita" to "it", "rus" to "ru",
            "kor" to "ko", "zho" to "zh", "chi" to "zh", "ara" to "ar", "ind" to "id",
            "tha" to "th", "tur" to "tr", "vie" to "vi", "may" to "ms", "msa" to "ms",
        )
        stream.subtitles.orEmpty().forEach { sub ->
            val url = if (sub.pick == "ass") sub.assUrl else sub.url
            if (url.isNullOrBlank() || !url.startsWith("https://") || !seenSubtitles.add(url)) {
                return@forEach
            }
            val label = sub.srclang?.takeIf { it.isNotBlank() }
                ?: sub.label?.takeIf { it.isNotBlank() }
                ?: "Subtitle"
            val code = label.lowercase()
            subtitleCallback(newSubtitleFile(languageMap[code] ?: code.ifBlank { "und" }, url))
        }

        callback(
            newExtractorLink(name, "$name ${channel.uppercase()}", master, ExtractorLinkType.M3U8) {
                referer = "https://embed.settlar.io/"
                this.headers = headers
            }
        )
        return true
    }
}
