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
import javax.inject.Inject
import javax.inject.Singleton

interface MKCentralDataSourceInterface {
    suspend fun findPlayer(discordId: String): NetworkResponse<MKCPlayerResponse>
    suspend fun getPlayer(playerId: String): NetworkResponse<MKCPlayer>
    suspend fun getTeam(teamId: String): NetworkResponse<MKCTeam>
    suspend fun getTeams(page: Int): NetworkResponse<MKCTeamResponse>
    suspend fun searchPlayers(page: Int, term: String): NetworkResponse<MKCPlayerResponse>
}

@Module
@InstallIn(SingletonComponent::class)
interface MKCentralDataSourceModule {
    @Binds
    @Singleton
    fun bind(impl: MKCentralDataSource): MKCentralDataSourceInterface
}

class MKCentralDataSource @Inject constructor() : MKCentralDataSourceInterface {

    override suspend fun findPlayer(discordId: String): NetworkResponse<MKCPlayerResponse> =  RetrofitUtils.createRetrofit(
        MKCentralApi::class.java,
        MKCentralApi.baseUrl,
        timeout = 5
    ).findPlayer(discordId)

    override suspend fun getPlayer(playerId: String): NetworkResponse<MKCPlayer>  = RetrofitUtils.createRetrofit(
        MKCentralApi::class.java,
        MKCentralApi.baseUrl,
        timeout = 5
    ).getPlayer(playerId)

    override suspend fun getTeam(teamId: String): NetworkResponse<MKCTeam> = RetrofitUtils.createRetrofit(
        MKCentralApi::class.java,
        MKCentralApi.baseUrl,
        timeout = 60
    ).getTeam(teamId)

    override suspend fun getTeams(page: Int): NetworkResponse<MKCTeamResponse>   = RetrofitUtils.createRetrofit(
        MKCentralApi::class.java,
        MKCentralApi.baseUrl,
        timeout = 60
    ).getTeams(page)

    override suspend fun searchPlayers(page: Int, term: String): NetworkResponse<MKCPlayerResponse> = RetrofitUtils.createRetrofit(
        MKCentralApi::class.java,
        MKCentralApi.baseUrl,
        timeout = 60
    ).searchPlayers(page, term)

}
