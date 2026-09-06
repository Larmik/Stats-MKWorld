package fr.harmoniamk.statsmkworld.screen.stats.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.filterBySeason
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Fiche détail d'un circuit (#27). Deux modes (rule 11) : Équipe (toutes les manches) et Individuel
 * (celles du joueur courant). Mode = état réactif ([isIndiv]) semé par [initialUserId], toggle sans
 * re-nav. 12p uniquement. [trackIndex] identifie le circuit.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = MapDetailViewModel.Factory::class)
class MapDetailViewModel @AssistedInject constructor(
    @Assisted val trackIndex: List<Int>,
    @Assisted("initialUserId") val initialUserId: String?,
    // Saison d'origine (#91 pt.5) : null = tout l'historique. Filtre les wars avant tout calcul.
    @Assisted val seasonNumber: Int?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            trackIndex: List<Int>,
            @Assisted("initialUserId") initialUserId: String?,
            seasonNumber: Int?
        ): MapDetailViewModel
    }

    /** Un pilote de l'équipe classé sur ce circuit. */
    data class PilotRanking(
        val player: PlayerEntity,
        // Score perso moyen (points) — critère de tri et valeur affichée.
        val averageScore: Int,
        // Position moyenne réelle (1..12).
        val averagePosition: Int,
        // Nb de manches courues (seuil MIN_RANKING_SAMPLE).
        val played: Int,
        val winrate: Int
    )

    /**
     * Un baggeur classé sur ce circuit (#69) : shockShare = ses shocks / total shocks équipe sur
     * ce circuit (critère de tri et valeur affichée). [shockCount] = nb shocks ; [played] = nb manches.
     */
    data class BaggerRanking(
        val player: PlayerEntity,
        val shockShare: Int,
        val shockCount: Int,
        val played: Int
    )

    /** Un adversaire rencontré sur ce circuit (12p, opposant unique). */
    data class OpponentRanking(
        val team: TeamEntity,
        // Score moyen d'équipe face à cet adversaire — critère de tri et valeur affichée (`trackScoreToDiff`).
        val averageTeamScore: Int,
        // Nb de manches contre cet adversaire (seuil MIN_RANKING_SAMPLE).
        val played: Int,
        val winrate: Int
    )

    data class State(
        val loading: Boolean = true,
        val isIndiv: Boolean = false,
        val maps: List<Maps> = listOf(),
        val mapStats: MapStats? = null,
        // « Scores moyens » indépendants du mode : score équipe + position du joueur courant.
        val teamScore: Int = 0,
        val playerPositionLabel: String = "-",
        // Shocks obtenus — suit le mode Indiv/Équipe.
        val shockCount: Int = 0,
        // Pilotes classés (membres uniquement), indépendant du mode.
        val pilots: List<PilotRanking> = listOf(),
        // Baggeurs par part de shocks (#69), membres uniquement.
        val baggers: List<BaggerRanking> = listOf(),
        // Adversaires rencontrés, indépendant du mode.
        val opponents: List<OpponentRanking> = listOf()
    )

    private val _state = MutableStateFlow(State(isIndiv = initialUserId != null))
    private val isIndiv = MutableStateFlow(initialUserId != null)

    private val trackKey = trackIndex.map { it.toString() }

    val state = databaseRepository.getWars()
        .combine(databaseRepository.getSeasons()) { wars, seasons ->
            // Filtre saison (#91 pt.5) avant tout ; `seasonNumber` null → tout l'historique.
            val season = seasonNumber?.let { number -> seasons.firstOrNull { it.number == number } }
            wars.filterBySeason(season)
                .filter { it.teamOpponent.size == 1 }  // 12p uniquement
                .map { WarDetails(War(it)) }
        }
        .combine(isIndiv) { warDetails, indiv -> warDetails to indiv }
        .map { (warDetails, indiv) ->
            // Joueur courant toujours résolu (position moyenne joueur affichée en permanence) ;
            // en mode Équipe il ne scope pas les sections (scope = null).
            val currentUserId = dataStoreRepository.mkcPlayer.firstOrNull()?.id?.toString()
            // Calcul CPU-lourd déporté sur `Dispatchers.Default` — pas `flowOn` (rule 21, #73).
            withContext(Dispatchers.Default) {
            val scopeUserId = if (indiv) currentUserId else null
            // Toutes les manches du circuit (scope équipe + pilotes) ; en indiv, filtrées ensuite.
            val allTrackDetails = mutableListOf<MapDetails>()
            warDetails.forEach { war ->
                war.warTracks.filter { it.index == trackKey }.forEach { track ->
                    allTrackDetails.add(MapDetails(war = war, warTrack = track, position = null))
                }
            }
            val scopedDetails = when (scopeUserId) {
                null -> allTrackDetails
                else -> allTrackDetails.filter { it.warTrack.track.positions.any { pos -> pos.playerId == scopeUserId } }
            }
            if (allTrackDetails.isEmpty()) {
                _state.value.copy(loading = false, isIndiv = indiv)
            } else {
                // Sections détaillées (distribution/Top-Bot) + shocks : scopées au mode.
                val mapStats = MapStats(list = scopedDetails, userId = scopeUserId, is24p = false)
                val maps = allTrackDetails.first().warTrack.track.index.map { Maps.entries[it.toInt()] }
                // « Scores moyens » figés : score équipe + position joueur, sur toutes les manches.
                val teamMapStats = MapStats(list = allTrackDetails, userId = currentUserId, is24p = false)
                _state.value.copy(
                    loading = false,
                    isIndiv = indiv,
                    maps = maps,
                    mapStats = mapStats,
                    teamScore = teamMapStats.teamScore,
                    playerPositionLabel = currentUserId
                        ?.let { teamMapStats.averagePlayerPosLabel }
                        ?: (teamMapStats.teamAveragePosition?.toString() ?: "-"),
                    shockCount = mapStats.shockCount,
                    // Pilotes : toutes les manches, membres uniquement.
                    pilots = computePilots(allTrackDetails),
                    // Baggeurs (#69) : part de shocks (total/total), membres uniquement.
                    baggers = computeBaggers(allTrackDetails),
                    // Adversaires rencontrés, toutes les manches.
                    opponents = computeOpponents(allTrackDetails)
                )
            }
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /** Bascule Indiv/Équipe (rule 11). */
    fun onModeChange(indiv: Boolean) {
        isIndiv.value = indiv
    }

    /**
     * Pilotes de l'équipe sur ce circuit, triés par score perso moyen décroissant. Winrate =
     * manches en top 6 (points > 6) / total. Alliés exclus (rosterId « -1 »). Seuil
     * [Stats.MIN_RANKING_SAMPLE] pour ne pas fausser le classement.
     */
    private suspend fun computePilots(details: List<MapDetails>): List<PilotRanking> {
        val positionsByPlayer = details
            .flatMap { it.warTrack.track.positions }
            .groupBy({ it.playerId }, { it.position })
        if (positionsByPlayer.isEmpty()) return listOf()

        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        return positionsByPlayer
            .mapNotNull { (playerId, positions) ->
                val player = players.firstOrNull { it.id == playerId } ?: return@mapNotNull null
                // Exclure les alliés (rosterId sentinelle « -1 ») — membres uniquement.
                if (player.rosterId == "-1") return@mapNotNull null
                // Seuil d'échantillon : au moins MIN_RANKING_SAMPLE manches sur ce circuit.
                if (positions.size < Stats.MIN_RANKING_SAMPLE) return@mapNotNull null
                val averageScore = positions.sumOf { it.positionToPoints(false) } / positions.size
                val averagePosition = positions.sum() / positions.size
                val wonCount = positions.count { it.positionToPoints(false) > 6 }
                val winrate = (wonCount * 100) / positions.size
                PilotRanking(
                    player = player,
                    averageScore = averageScore,
                    averagePosition = averagePosition,
                    played = positions.size,
                    winrate = winrate
                )
            }
            .sortedByDescending { it.averageScore }
    }

    /**
     * Baggeurs de l'équipe sur ce circuit (#69) : shockShare = ses shocks / total shocks équipe
     * (total/total). Alliés exclus (rosterId « -1 »), baggeurs avec ≥ 1 shock seulement.
     */
    private suspend fun computeBaggers(details: List<MapDetails>): List<BaggerRanking> {
        val allShocks = details.flatMap { it.warTrack.track.shocks.orEmpty() }
        val totalTeamShocks = allShocks.sumOf { it.count }.takeIf { it > 0 } ?: return listOf()
        // Nb de manches courues par joueur.
        val runsByPlayer = details
            .flatMap { it.warTrack.track.positions }
            .groupingBy { it.playerId }
            .eachCount()
        val shocksByPlayer = allShocks
            .groupBy { it.playerId }
            .mapValues { (_, shocks) -> shocks.sumOf { it.count } }

        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        return shocksByPlayer
            .mapNotNull { (playerId, shockCount) ->
                if (shockCount == 0) return@mapNotNull null
                val player = players.firstOrNull { it.id == playerId } ?: return@mapNotNull null
                // Membres uniquement (alliés = rosterId sentinelle « -1 »).
                if (player.rosterId == "-1") return@mapNotNull null
                BaggerRanking(
                    player = player,
                    shockShare = shockCount * 100 / totalTeamShocks,
                    shockCount = shockCount,
                    played = runsByPlayer[playerId] ?: 0
                )
            }
            .sortedByDescending { it.shockShare }
    }

    /**
     * Adversaires rencontrés sur ce circuit (12p), triés par score moyen d'équipe décroissant.
     * Winrate = manches gagnées (`trackOutcome > 0`) / total. Rule 12 (non résolu → « Équipe
     * inconnue »). Seuil [Stats.MIN_RANKING_SAMPLE].
     */
    private suspend fun computeOpponents(details: List<MapDetails>): List<OpponentRanking> {
        // 12p : opposant unique par war → groupe les manches du circuit par opposant.
        val tracksByOpponent = details
            .mapNotNull { detail -> detail.war.war.teamOpponent.firstOrNull()?.let { it to detail.warTrack } }
            .groupBy({ it.first }, { it.second })
        if (tracksByOpponent.isEmpty()) return listOf()

        return tracksByOpponent
            .mapNotNull { (opponentId, tracks) ->
                if (tracks.size < Stats.MIN_RANKING_SAMPLE) return@mapNotNull null
                val averageTeamScore = tracks.sumOf { it.teamScore } / tracks.size
                val wonCount = tracks.count { it.trackOutcome() > 0 }
                val winrate = (wonCount * 100) / tracks.size
                // Rule 12 : nom/tag du roster, logo de l'équipe parente ; non résolu → dégradé.
                val team = databaseRepository.getTeam(opponentId)?.let { resolved ->
                    val roster = resolved.rosters.firstOrNull { it.id == opponentId }
                    resolved.copy(
                        id = opponentId,
                        name = roster?.name ?: resolved.name,
                        tag = roster?.tag ?: resolved.tag
                    )
                } ?: TeamEntity(id = opponentId, name = "Équipe inconnue", tag = "???", color = null, logo = null)
                OpponentRanking(
                    team = team,
                    averageTeamScore = averageTeamScore,
                    played = tracks.size,
                    winrate = winrate
                )
            }
            .sortedByDescending { it.averageTeamScore }
    }
}
