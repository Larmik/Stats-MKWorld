package fr.harmoniamk.statsmkworld.api

import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayerResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MKCentralApi {
    companion object {
        const val baseUrl: String = "https://mkcentral.com/api/"
    }

    @GET("registry/players")
    fun findPlayer(
        @Query("discord_id") discordId: String
    ): Call<MKCPlayerResponse>

    @GET("registry/players?detailed=true&is_banned=false&is_hidden=false&matching_fcs_only=true&is_shadow=false")
    fun searchPlayers(
        @Query("page") page: Int,
        @Query("name_or_fc") term: String
    ): Call<MKCPlayerResponse>

    @GET("registry/players/{playerId}")
    fun getPlayer(
        @Path("playerId") playerId: String
    ): Call<MKCPlayer>

    @GET("registry/teams/{teamId}")
    fun getTeam(
        @Path("teamId") teamId: String
    ): Call<MKCTeam>

    @GET("registry/teams?game=mkworld&mode=150cc&is_historical=false&is_active=true")
    fun getTeams(@Query("page") page: Int): Call<MKCTeamResponse>

    @GET("registry/teams?game=mk8dx&mode=150cc&is_historical=false&is_active=true")
    fun getMK8Teams(@Query("page") page: Int): Call<MKCTeamResponse>

}