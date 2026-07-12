package fr.harmoniamk.statsmkworld.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.extension.withFullTeamStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.Serializable

sealed class StatsType(val title: Int, val is24PEnabled: Boolean): Serializable {
    class PlayerStats(val userId: String, val is24p: Boolean) : StatsType(R.string.statistiques_du_joueur, is24p)
    class TeamStats(val is24p: Boolean) : StatsType(R.string.statistiques_de_l_quipe, is24p)
    class OpponentStats(
        val teamId: String,
        val userId: String? = null,
        val is24p: Boolean
    ) : StatsType(R.string.statistiques_de_l_adversaire, is24p)

    class MapStats(
        val userId: String? = null,
        val teamId: String? = null,
        val trackIndex: List<Int>? = null,
        val is24p: Boolean
    ) : StatsType(R.string.statistiques_du_circuit, is24p)
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = StatsViewModel.Factory::class)
class StatsViewModel @AssistedInject constructor(
    @Assisted val type: StatsType?,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(type: StatsType?): StatsViewModel
    }

    data class State(
        val stats: Stats? = null,
        val mapStats: MapStats? = null,
        val team: TeamEntity? = null,
        val player: PlayerEntity? = null,
        val map: List<Maps>? = null,
        // Lot B/C — top3/flop3 adversaires par winrate/score, calculés dans le VM
        // au périmètre de la vue (équipe = tous ; joueur = du point de vue du
        // joueur affiché). Seuil ≥ 3 matchs (Stats.MIN_RANKING_SAMPLE).
        val topOpponentsByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val flopOpponentsByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val topOpponentsByScore: List<RankingItem.OpponentRanking> = listOf(),
        val flopOpponentsByScore: List<RankingItem.OpponentRanking> = listOf()
    )

    private val wars = mutableListOf<WarDetails>()
    private var team: MKCTeam? = null

    private val _state = MutableStateFlow(State())

    val state = databaseRepository.getWars()
        .map {
            team = dataStoreRepository.mkcTeam.firstOrNull()
            val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
            val rosterId = dataStoreRepository.mkcPlayer.firstOrNull()?.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()

            when {
                type is StatsType.PlayerStats -> it.filter { war -> war.hasPlayer(type.userId) }
                type is StatsType.TeamStats -> it.filter { war -> (!multiRosterEnabled && war.hasTeam(rosterId)) || multiRosterEnabled }
                type is StatsType.OpponentStats -> it
                    .filter { war -> war.hasTeam(type.teamId) }
                    .filter { war -> (type.userId != null && war.hasPlayer(type.userId)) || type.userId == null }
                type is StatsType.MapStats -> it
                    .filter { war -> (type.teamId != null && war.hasTeam(type.teamId)) || type.teamId == null }
                    .filter { war -> (type.userId != null && war.hasPlayer(type.userId)) || type.userId == null }
                else -> it
            }
        }
        .map {
            it.filter { war ->
                (type?.is24PEnabled == true && war.teamOpponent.size > 1)
                        ||(type?.is24PEnabled != true && war.teamOpponent.size == 1)
            }
        }
        .filterNot { it.isEmpty() }
        .map { it.map { WarDetails(War(it)) } }
        .flatMapLatest { wars ->
            this.wars.clear()
            this.wars.addAll(wars)
            when {
                type is StatsType.PlayerStats -> wars.withFullStats(databaseRepository, userId = type.userId, is24p = type.is24p)
                type is StatsType.OpponentStats -> wars.withFullStats(databaseRepository, teamId = type.teamId, userId = type.userId, is24p = type.is24p)
                type is StatsType.MapStats -> wars.withFullStats(databaseRepository, teamId = type.teamId, userId = type.userId, is24p = type.is24p)
                else -> wars.withFullStats(databaseRepository, is24p = type?.is24PEnabled == true)
            }
        }
        .map { stats ->
            val userId = (type as? StatsType.OpponentStats)?.userId
                ?: (type as? StatsType.MapStats)?.userId
                ?: (type as? StatsType.PlayerStats)?.userId

            val teamId = (type as? StatsType.OpponentStats)?.teamId
                ?: (type as? StatsType.MapStats)?.teamId
                ?: (type as? StatsType.TeamStats)?.let { team?.id.toString() }

            val player = userId?.let { databaseRepository.getPlayer(it).firstOrNull() }
            // teamId peut être un rosterId (classement par roster) : on remonte à
            // l'équipe (avatar) puis on affiche le nom/tag du roster ciblé.
            val team = teamId?.let { id ->
                databaseRepository.getTeam(id)?.let { resolved ->
                    val roster = resolved.rosters.firstOrNull { it.id == id }
                    resolved.copy(
                        id = id,
                        name = roster?.name ?: resolved.name,
                        tag = roster?.tag ?: resolved.tag
                    )
                }
            }

            // Lot B/C — top3/flop3 adversaires par winrate/score, au périmètre de
            // la vue : stats d'équipe (tous adversaires) ET stats joueur/individuelles
            // (adversaires affrontés du point de vue de ce joueur). Non pertinent
            // pour l'écran d'un adversaire précis (OpponentStats).
            val opponentRankings = when (type) {
                is StatsType.TeamStats -> computeOpponentRankings(userId = null)
                is StatsType.PlayerStats -> computeOpponentRankings(userId = type.userId)
                else -> OpponentRankings()
            }

            when (type) {
                is StatsType.PlayerStats, is StatsType.TeamStats, is StatsType.OpponentStats -> _state.value = _state.value.copy(
                    stats = stats,
                    topOpponentsByWinrate = opponentRankings.topByWinrate,
                    flopOpponentsByWinrate = opponentRankings.flopByWinrate,
                    topOpponentsByScore = opponentRankings.topByScore,
                    flopOpponentsByScore = opponentRankings.flopByScore
                )
                is StatsType.MapStats -> {
                    val finalList = mutableListOf<MapDetails>()
                    wars.forEach { mkWar ->
                        mkWar.warTracks.filter { track -> track.index == type.trackIndex?.map { it.toString() } }.forEach { track ->
                            val position = track.track.positions.singleOrNull { it.playerId == type.userId }?.position?.takeIf { type.userId != null }
                            finalList.add(
                                MapDetails(
                                    war = mkWar,
                                    warTrack = track,
                                    position = position
                                )
                            )
                        }
                    }
                    val mapDetailsList = mutableListOf<MapDetails>()
                    mapDetailsList.addAll(finalList)
                    _state.value = _state.value.copy(
                        mapStats = MapStats(
                            list = mapDetailsList,
                            userId = type.userId,
                            is24p = type.is24p
                        ),
                        map = mapDetailsList.first().warTrack.track.index.map { Maps.entries[it.toInt()] }
                    )
                }
                else -> {}
            }
            _state.value.copy(team = team, player = player)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /** Regroupe les 4 classements adversaires (top/flop × winrate/score). */
    private data class OpponentRankings(
        val topByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val flopByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val topByScore: List<RankingItem.OpponentRanking> = listOf(),
        val flopByScore: List<RankingItem.OpponentRanking> = listOf()
    )

    /**
     * Calcule les top3/flop3 adversaires par winrate ET score moyen, au périmètre
     * de la vue courante (les wars déjà filtrées dans [wars]). `userId` non-null
     * ⇒ point de vue du joueur (adversaires qu'IL a affrontés). Seuil d'échantillon
     * ≥ [Stats.MIN_RANKING_SAMPLE] matchs. Calcul dans le VM (mono-consommateur) et
     * non dans le cache worker, car le périmètre dépend de la vue (rule 32).
     */
    private suspend fun computeOpponentRankings(userId: String?): OpponentRankings {
        val currentTeamId = team?.id.toString()
        val teams = databaseRepository.getTeams().firstOrNull()
            .orEmpty()
            .filterNot { it.id == currentTeamId }
        val warEntities = wars.map { WarEntity(it.war) }
        val rankable = teams
            .withFullTeamStats(wars = warEntities, databaseRepository = databaseRepository, userId = userId)
            .firstOrNull()
            .orEmpty()
            .map { RankingItem.OpponentRanking(it.first, it.second) }
            .filter { it.stats.warStats.warsPlayed >= Stats.MIN_RANKING_SAMPLE }
        return OpponentRankings(
            topByWinrate = rankable.sortedByDescending { it.winrate }.take(3),
            flopByWinrate = rankable.sortedBy { it.winrate }.take(3),
            topByScore = rankable.sortedByDescending { it.stats.averagePoints }.take(3),
            flopByScore = rankable.sortedBy { it.stats.averagePoints }.take(3)
        )
    }

}