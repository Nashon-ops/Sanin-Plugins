package com.sanin.plugins.anipm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMListResponse(
    @JsonProperty("items") val items: List<AniPMTitle>? = null,
    @JsonProperty("hasNextPage") val hasNextPage: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMTitle(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("banner") val banner: String? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("score") val score: Double? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("genres") val genres: List<String>? = null,
    @JsonProperty("subCount") val subCount: Int? = null,
    @JsonProperty("dubCount") val dubCount: Int? = null,
    @JsonProperty("synopsis") val synopsis: String? = null,
    @JsonProperty("malId") val malId: String? = null,
    @JsonProperty("anilistId") val anilistId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMLatestResponse(
    @JsonProperty("items") val items: List<AniPMLatestItem>? = null,
    @JsonProperty("hasNextPage") val hasNextPage: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMLatestItem(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("sub") val sub: Boolean? = null,
    @JsonProperty("dub") val dub: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMSeries(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("banner") val banner: String? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("score") val score: Double? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("duration") val duration: String? = null,
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("genres") val genres: List<String>? = null,
    @JsonProperty("synopsis") val synopsis: String? = null,
    @JsonProperty("malId") val malId: String? = null,
    @JsonProperty("anilistId") val anilistId: String? = null,
    @JsonProperty("episodes") val episodes: List<AniPMEpisode>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMEpisode(
    @JsonProperty("number") val number: Int? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("thumbnail") val thumbnail: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("sub") val sub: Boolean? = null,
    @JsonProperty("dub") val dub: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMBootstrap(
    @JsonProperty("settlarSelection") val settlarSelection: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AniPMEmbedSession(
    @JsonProperty("embedUrl") val embedUrl: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SettlarStream(
    @JsonProperty("source") val source: String? = null,
    @JsonProperty("subtitles") val subtitles: List<SettlarSubtitle>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SettlarSubtitle(
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("srclang") val srclang: String? = null,
    @JsonProperty("pick") val pick: String? = null,
    @JsonProperty("assUrl") val assUrl: String? = null,
)
