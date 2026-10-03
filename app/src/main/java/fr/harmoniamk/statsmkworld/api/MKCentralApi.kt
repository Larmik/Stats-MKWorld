package fr.harmoniamk.statsmkworld.api

import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayerResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTournament
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTournamentList
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MKCentralApi {
    companion object {
        const val baseUrl: String = "https://mkcentral.com/api/"
    }

    @GET("registry/players")
    suspend fun findPlayer(
        @Query("discord_id") discordId: String
    ): NetworkResponse<MKCPlayerResponse>

    @GET("registry/players?detailed=true&is_banned=false&is_hidden=false&matching_fcs_only=true&is_shadow=false")
    suspend fun searchPlayers(
        @Query("page") page: Int,
        @Query("name_or_fc") term: String
    ): NetworkResponse<MKCPlayerResponse>

    @GET("registry/players/{playerId}")
    suspend fun getPlayer(
        @Path("playerId") playerId: String
    ): NetworkResponse<MKCPlayer>

    @GET("registry/teams/{teamId}")
    suspend fun getTeam(
        @Path("teamId") teamId: String
    ): NetworkResponse<MKCTeam>

    // Équipes mkworld (synchro registre + diagnostic « Équipe inconnue »). Miroir du
    // filtre par défaut MKCentral « actives, ≥ 6 joueurs » : min_player_count élague
    // l'équipe entière, sans amputer les rosters d'une équipe qualifiante. game figé
    // mkworld (rule 31).
    @GET("registry/teams?game=mkworld&mode=150cc&is_historical=false&is_active=true&min_player_count=6")
    suspend fun getTeams(@Query("page") page: Int): NetworkResponse<MKCTeamResponse>

    // Saisons mkworld d'un tournoi officiel (#152), filtrées par série ou par nom (LIKE). game
    // figé mkworld : seules ces saisons sont lues, même si la série d'origine est mk8dx (rule 31).
    @GET("tournaments/list?game=mkworld")
    suspend fun getTournaments(
        @Query("series_id") seriesId: Int?,
        @Query("name") name: String?
    ): NetworkResponse<MKCTournamentList>

    @GET("tournaments/{tournamentId}")
    suspend fun getTournament(
        @Path("tournamentId") tournamentId: Int
    ): NetworkResponse<MKCTournament>
}
