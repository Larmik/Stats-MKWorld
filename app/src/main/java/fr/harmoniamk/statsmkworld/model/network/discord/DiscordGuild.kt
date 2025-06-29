package fr.harmoniamk.statsmkworld.model.network.discord

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscordGuild (
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "icon") val icon: String,
    @field:Json(name = "banner") val banner: String?,
    @field:Json(name = "owner") val owner: Boolean,
    @field:Json(name = "permissions") val permissions: String,
    @field:Json(name = "features") val features: List<String>,
    @field:Json(name = "approximate_member_count") val approximateMemberCount: Long?,
    @field:Json(name = "approximate_presence_count") val approximatePresenceCount: Long?
)