package fr.harmoniamk.statsmkworld.api

import fr.harmoniamk.statsmkworld.model.network.NetworkResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayerResponse
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamResponse
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

    // Liste des équipes mkworld pour la synchro du registre ET le diagnostic des
    // adversaires « Équipe inconnue ». Miroir du filtre PAR DÉFAUT du site MKCentral
    // « Équipes actives avec plus de 6 joueurs » : is_active=true, is_historical=false
    // et min_player_count=6. Une équipe dont tous les rosters mkworld ont < 6 joueurs
    // est donc exclue (min_player_count élague l'équipe entière, sans amputer les
    // sous-rosters d'une équipe qui qualifie). Domaine exclusivement mkworld (cf.
    // rule 31-mkworld-only) : game figé à mkworld.
    @GET("registry/teams?game=mkworld&mode=150cc&is_historical=false&is_active=true&min_player_count=6")
    suspend fun getTeams(@Query("page") page: Int): NetworkResponse<MKCTeamResponse>

}