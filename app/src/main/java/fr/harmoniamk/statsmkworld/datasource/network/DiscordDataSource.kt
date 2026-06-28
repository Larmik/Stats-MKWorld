package fr.harmoniamk.statsmkworld.datasource.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.BuildConfig
import fr.harmoniamk.statsmkworld.api.DiscordApi
import fr.harmoniamk.statsmkworld.api.RetrofitUtils
import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import fr.harmoniamk.statsmkworld.model.network.discord.DiscordUser
import fr.harmoniamk.statsmkworld.model.network.discord.TokenResponse
import okhttp3.Credentials
import javax.inject.Inject
import javax.inject.Singleton

interface DiscordDataSourceInterface {
    suspend fun getToken(code: String): NetworkResponse<TokenResponse>
    suspend fun getUser(token: String): NetworkResponse<DiscordUser>
    suspend fun revokeToken(token: String): NetworkResponse<TokenResponse>
}

@Module
@InstallIn(SingletonComponent::class)
interface DiscordDataSourceModule {

    @Binds
    @Singleton
    fun bind(impl: DiscordDataSource): DiscordDataSourceInterface
}


class DiscordDataSource @Inject constructor() : DiscordDataSourceInterface {

    override suspend fun getToken(code: String): NetworkResponse<TokenResponse> {
        val credentials = Credentials.basic(BuildConfig.DISCORD_API_CLIENT, BuildConfig.DISCORD_API_SECRET)
        return RetrofitUtils.createRetrofit(
            DiscordApi::class.java,
            DiscordApi.baseUrl,
            timeout = 60
        ).getToken(code = code, authorization = credentials)
    }

    override suspend fun getUser(token: String): NetworkResponse<DiscordUser> = RetrofitUtils.createRetrofit(
            DiscordApi::class.java,
            DiscordApi.baseUrl,
            timeout = 60
        ).getCurrentUser(authorization = "Bearer $token")


    override suspend fun revokeToken(token: String): NetworkResponse<TokenResponse> {
        val credentials = Credentials.basic(BuildConfig.DISCORD_API_CLIENT, BuildConfig.DISCORD_API_SECRET)
        return RetrofitUtils.createRetrofit(
            DiscordApi::class.java,
            DiscordApi.baseUrl,
            timeout = 60
        ).revokeToken(token = token, authorization = credentials)
    }

}
