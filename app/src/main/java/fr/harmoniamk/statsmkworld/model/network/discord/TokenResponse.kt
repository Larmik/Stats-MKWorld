package fr.harmoniamk.statsmkworld.model.network.discord

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @field:Json(name = "access_token") val accessToken: String?,
    @field:Json(name = "token_type") val tokenType: String?,
    @field:Json(name = "expires_in") val expiresIn: Long?,
    @field:Json(name = "refresh_token") val refreshToken: String?,
    @field:Json(name = "scope") val scope: String?,
)