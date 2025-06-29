package fr.harmoniamk.statsmkworld.model.network.discord

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscordMember (
    @field:Json(name = "user") val user: DiscordUser,
    @field:Json(name = "nick") val nick: String,
    @field:Json(name = "avatar") val avatar: String?,
    @field:Json(name = "banner") val banner: String?,
    @field:Json(name = "roles") val roles: List<String?>,
    @field:Json(name = "joined_at") val joinedAt: String,
    @field:Json(name = "deaf") val deaf: Boolean,
    @field:Json(name = "mute") val mute: Boolean
)