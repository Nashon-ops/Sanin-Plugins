package com.sanin.plugins.anipm

import kotlinx.serialization.Serializable

@Serializable
data class AniPMListResponse(
    val items: List<AniPMTitle>? = null,
    val hasNextPage: Boolean? = null,
)

@Serializable
data class AniPMTitle(
    val id: Int? = null,
    val title: String? = null,
    val poster: String? = null,
    val banner: String? = null,
    val year: Int? = null,
    val score: Double? = null,
    val rating: String? = null,
    val status: String? = null,
    val type: String? = null,
    val genres: List<String>? = null,
    val subCount: Int? = null,
    val dubCount: Int? = null,
    val synopsis: String? = null,
    val malId: String? = null,
    val anilistId: String? = null,
)

@Serializable
data class AniPMLatestItem(
    val id: Int? = null,
    val title: String? = null,
    val poster: String? = null,
    val sub: Boolean? = null,
    val dub: Boolean? = null,
)

@Serializable
data class AniPMSeries(
    val id: Int? = null,
    val title: String? = null,
    val poster: String? = null,
    val banner: String? = null,
    val year: Int? = null,
    val score: Double? = null,
    val rating: String? = null,
    val duration: String? = null,
    val status: String? = null,
    val type: String? = null,
    val genres: List<String>? = null,
    val synopsis: String? = null,
    val malId: String? = null,
    val anilistId: String? = null,
    val episodes: List<AniPMEpisode>? = null,
)

@Serializable
data class AniPMEpisode(
    val number: Int? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val description: String? = null,
    val sub: Boolean? = null,
    val dub: Boolean? = null,
)
