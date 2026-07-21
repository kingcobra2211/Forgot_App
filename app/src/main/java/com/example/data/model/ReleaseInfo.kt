package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReleaseInfo(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String?,
    @Json(name = "body") val body: String?,
    @Json(name = "published_at") val publishedAt: String?,
    @Json(name = "assets") val assets: List<ReleaseAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ReleaseAsset(
    @Json(name = "name") val name: String,
    @Json(name = "size") val size: Long,
    @Json(name = "browser_download_url") val browserDownloadUrl: String
)
