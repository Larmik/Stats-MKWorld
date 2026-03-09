package fr.harmoniamk.statsmkworld.datasource.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.harmoniamk.statsmkworld.api.MKCentralApi
import fr.harmoniamk.statsmkworld.api.RetrofitUtils
import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayerResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

interface MKCentralDataSourceInterface {
    fun findPlayer(discordId: String): Flow<MKCPlayerResponse?>
    fun getPlayer(playerId: String): Flow<NetworkResponse<MKCPlayer>>
    fun getTeam(teamId: String): Flow<MKCTeam?>
    fun getTeams(page: Int): Flow<MKCTeamResponse?>
    fun getMK8Teams(page: Int): Flow<MKCTeamResponse?>
    fun searchPlayers(page: Int, term: String): Flow<MKCPlayerResponse?>
}

@Module
@InstallIn(SingletonComponent::class)
interface MKCentralDataSourceModule {
    @Binds
    @Singleton
    fun bind(impl: MKCentralDataSource): MKCentralDataSourceInterface
}

class MKCentralDataSource @Inject constructor() : MKCentralDataSourceInterface {
    override fun findPlayer(discordId: String): Flow<MKCPlayerResponse?> = callbackFlow {
        val call = RetrofitUtils.createRetrofit(
            MKCentralApi::class.java,
            MKCentralApi.baseUrl,
            timeout = 5
        ).findPlayer(discordId)

        call.enqueue(object : retrofit2.Callback<MKCPlayerResponse> {

            override fun onResponse(
                call: retrofit2.Call<MKCPlayerResponse>,
                response: retrofit2.Response<MKCPlayerResponse>
            ) {
                val result = response.body()

                when {
                    result != null -> trySend(result)
                    else -> trySend(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<MKCPlayerResponse>, t: Throwable) {
                trySend(null)
            }
        })

        awaitClose {  }
    }

    override fun getPlayer(playerId: String): Flow<NetworkResponse<MKCPlayer>>  = callbackFlow {
        val call = RetrofitUtils.createRetrofit(
            MKCentralApi::class.java,
            MKCentralApi.baseUrl,
            timeout = 5
        ).getPlayer(playerId)

        call.enqueue(object : retrofit2.Callback<MKCPlayer> {

            override fun onResponse(
                call: retrofit2.Call<MKCPlayer>,
                response: retrofit2.Response<MKCPlayer>
            ) {
                val result = response.body()
                val error = response.errorBody()

                when {
                    result != null -> trySend(NetworkResponse.Success(result))
                    error != null -> trySend(NetworkResponse.Error(error.string()))
                    else -> trySend(NetworkResponse.Error("Erreur inconnue"))
                }
            }

            override fun onFailure(call: retrofit2.Call<MKCPlayer>, t: Throwable) {
                trySend(NetworkResponse.Error(t.message ?: "Erreur inconnnue"))
            }
        })

        awaitClose {  }
    }

    override fun getTeam(teamId: String): Flow<MKCTeam?> = callbackFlow {
        val call = RetrofitUtils.createRetrofit(
            MKCentralApi::class.java,
            MKCentralApi.baseUrl,
            timeout = 60
        ).getTeam(teamId)

        call.enqueue(object : retrofit2.Callback<MKCTeam> {

            override fun onResponse(
                call: retrofit2.Call<MKCTeam>,
                response: retrofit2.Response<MKCTeam>
            ) {
                val result = response.body()

                when {
                    result != null -> trySend(result)
                    else -> trySend(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<MKCTeam>, t: Throwable) {
                trySend(null)
            }
        })

        awaitClose {  }
    }

    override fun getTeams(page: Int): Flow<MKCTeamResponse?>  = callbackFlow {
        val call = RetrofitUtils.createRetrofit(
            MKCentralApi::class.java,
            MKCentralApi.baseUrl,
            timeout = 60
        ).getTeams(page)

        call.enqueue(object : retrofit2.Callback<MKCTeamResponse> {

            override fun onResponse(
                call: retrofit2.Call<MKCTeamResponse>,
                response: retrofit2.Response<MKCTeamResponse>
            ) {
                val result = response.body()

                when {
                    result != null -> trySend(result)
                    else -> trySend(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<MKCTeamResponse>, t: Throwable) {
                trySend(null)
            }
        })
        awaitClose {  }
    }

    override fun getMK8Teams(page: Int): Flow<MKCTeamResponse?> = callbackFlow {
        val call = RetrofitUtils.createRetrofit(
            MKCentralApi::class.java,
            MKCentralApi.baseUrl,
            timeout = 60
        ).getMK8Teams(page)

        call.enqueue(object : retrofit2.Callback<MKCTeamResponse> {

            override fun onResponse(
                call: retrofit2.Call<MKCTeamResponse>,
                response: retrofit2.Response<MKCTeamResponse>
            ) {
                val result = response.body()

                when {
                    result != null -> trySend(result)
                    else -> trySend(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<MKCTeamResponse>, t: Throwable) {
                trySend(null)
            }
        })
        awaitClose {  }
    }

    override fun searchPlayers(page: Int, term: String): Flow<MKCPlayerResponse?>  = callbackFlow {
        val call = RetrofitUtils.createRetrofit(
            MKCentralApi::class.java,
            MKCentralApi.baseUrl,
            timeout = 60
        ).searchPlayers(page, term)

        call.enqueue(object : retrofit2.Callback<MKCPlayerResponse> {

            override fun onResponse(
                call: retrofit2.Call<MKCPlayerResponse>,
                response: retrofit2.Response<MKCPlayerResponse>
            ) {
                val result = response.body()

                when {
                    result != null -> trySend(result)
                    else -> trySend(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<MKCPlayerResponse>, t: Throwable) {
                trySend(null)
            }
        })
        awaitClose {  }
    }

}
