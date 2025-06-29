package fr.harmoniamk.statsmkworld.model.network.discord

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscordUser (
    @field:Json(name = "id") val id: String,
    @field:Json(name = "username") val username: String,
    @field:Json(name = "discriminator") val discriminator: String,
    @field:Json(name = "avatar") val avatar: String,
    @field:Json(name = "verified") val verified: Boolean?,
    @field:Json(name = "email") val email: String?,
    @field:Json(name = "flags") val flags: Long?,
    @field:Json(name = "banner") val banner: String?,
    @field:Json(name = "accent_color") val accentColor: Long?,
    @field:Json(name = "premium_type") val premiumType: Long?,
    @field:Json(name = "public_flags") val publicFlags: Long?,
    @field:Json(name = "avatar_decoration_data") val avatarDecorationData: AvatarDecorationData?
)

@JsonClass(generateAdapter = true)
data class AvatarDecorationData (
    @field:Json(name = "sku_id") val skuID: String,
    @field:Json(name = "asset") val asset: String
)
