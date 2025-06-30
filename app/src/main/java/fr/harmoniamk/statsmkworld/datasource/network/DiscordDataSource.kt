package fr.harmoniamk.statsmkworld.datasource.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.BuildConfig
import fr.harmoniamk.statsmkworld.api.DiscordApi
import fr.harmoniamk.statsmkworld.api.RetrofitUtils
import fr.harmoniamk.statsmkworld.application.Constants
import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import fr.harmoniamk.statsmkworld.model.network.discord.DiscordUser
import fr.harmoniamk.statsmkworld.model.network.discord.TokenResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Credentials
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

interface DiscordDataSourceInterface {
    fun getToken(code: String): Flow<NetworkResponse<TokenResponse>>
    fun getUser(token: String): Flow<NetworkResponse<DiscordUser>>
    fun revokeToken(token: String): Flow<Unit?>
}

@Module
@InstallIn(SingletonComponent::class)
interface DiscordDataSourceModule {

    @Binds
    @Singleton
    fun bind(impl: DiscordDataSource): DiscordDataSourceInterface
}


class DiscordDataSource @Inject constructor(

) : DiscordDataSourceInterface, CoroutineScope {


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun getToken(code: String): Flow<NetworkResponse<TokenResponse>> = callbackFlow {
        val credentials = Credentials.basic(Constants.authClientId, BuildConfig.DISCORD_API_SECRET)
        val call = RetrofitUtils.createRetrofit(
            DiscordApi::class.java,
            DiscordApi.baseUrl,
            timeout = 60
        ).getToken(code = code, authorization = credentials)

        call.enqueue(object : retrofit2.Callback<TokenResponse> {

            override fun onResponse(
                call: retrofit2.Call<TokenResponse>,
                response: retrofit2.Response<TokenResponse>
            ) {
                val result = response.body()
                val error = response.errorBody()

                when {
                    result != null -> trySend(NetworkResponse.Success(result))
                    error != null -> trySend(NetworkResponse.Error(error.string()))
                    else -> trySend(NetworkResponse.Error("Erreur inconnue"))
                }
            }

            override fun onFailure(call: retrofit2.Call<TokenResponse>, t: Throwable) {
                trySend(NetworkResponse.Error(t.message ?: "Erreur inconnnue"))
            }
        })

        awaitClose { }
    }

    override fun getUser(token: String): Flow<NetworkResponse<DiscordUser>> = callbackFlow {
        val call = RetrofitUtils.createRetrofit(
            DiscordApi::class.java,
            DiscordApi.baseUrl,
            timeout = 60
        ).getCurrentUser(authorization = "Bearer $token")

        call.enqueue(object : retrofit2.Callback<DiscordUser> {

            override fun onResponse(
                call: retrofit2.Call<DiscordUser>,
                response: retrofit2.Response<DiscordUser>
            ) {
                val result = response.body()
                val error = response.errorBody()

                when {
                    result != null -> trySend(NetworkResponse.Success(result))
                    error != null -> trySend(NetworkResponse.Error(error.string()))
                    else -> trySend(NetworkResponse.Error("Erreur inconnue"))
                }
            }

            override fun onFailure(call: retrofit2.Call<DiscordUser>, t: Throwable) {
                trySend(NetworkResponse.Error(t.message ?: "Erreur inconnnue"))
            }
        })

        awaitClose { }
    }

    override fun revokeToken(token: String): Flow<Unit?> = callbackFlow {
        val credentials = Credentials.basic(Constants.authClientId, BuildConfig.DISCORD_API_SECRET)
        val call = RetrofitUtils.createRetrofit(
            DiscordApi::class.java,
            DiscordApi.baseUrl,
            timeout = 60
        ).revokeToken(token = token, authorization = credentials)

        call.enqueue(object : retrofit2.Callback<TokenResponse> {

            override fun onResponse(
                call: retrofit2.Call<TokenResponse>,
                response: retrofit2.Response<TokenResponse>
            ) {
                val result = response.body()

                when {
                    result != null -> trySend(Unit)
                    else -> trySend(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<TokenResponse>, t: Throwable) {
                trySend(null)
            }
        })

        awaitClose { }

    }


}
