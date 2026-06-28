package fr.harmoniamk.statsmkworld.api

import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import fr.harmoniamk.statsmkworld.model.network.discord.DiscordUser
import fr.harmoniamk.statsmkworld.model.network.discord.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface DiscordApi {
    companion object {
        const val baseUrl: String = "https://discord.com/"
    }

    @FormUrlEncoded
    @POST("api/oauth2/token")
    suspend fun getToken(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/x-www-form-urlencoded",
        @Field("redirect_uri") redirectUri: String = "https://statsmkworld.com",
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
    ): NetworkResponse<TokenResponse>

    @FormUrlEncoded
    @POST("api/oauth2/token/revoke")
    suspend fun revokeToken(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/x-www-form-urlencoded",
        @Field("token") token: String,
        @Field("token_type_hint") type: String = "access_token"
    ): NetworkResponse<TokenResponse>

    @GET("api/users/@me")
    suspend fun getCurrentUser(@Header("Authorization") authorization: String): NetworkResponse<DiscordUser>

}