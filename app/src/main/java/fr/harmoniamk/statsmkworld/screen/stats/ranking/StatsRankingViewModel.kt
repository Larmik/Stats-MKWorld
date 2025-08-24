package fr.harmoniamk.statsmkworld.screen.stats.ranking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.StatsRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SortType(val label: String) {
    OCCURENCES("Occurences"),
    NAME("Nom"),
    WINRATE("Winrate"),
    AVERAGE("Score/Position"),
}

sealed interface RankingItem {

    class PlayerRanking(val player: PlayerEntity, val stats: Stats) : RankingItem {

        val averageLabel: String
            get() = stats.averagePoints.toString()

        val warsPlayedLabel: String
            get() = stats.warStats.warsPlayed.toString()

        val winrateLabel: String
            get() = when (stats.warStats.warsPlayed) {
                0 -> "0 %"
                else -> "${(stats.warStats.warsWon * 100) / stats.warStats.warsPlayed} %"
            }
    }

    class OpponentRanking(val team: TeamEntity, val stats: Stats) : RankingItem {

        val averageLabel: String
            get() = stats.averagePointsLabel

        val warsPlayedLabel: String
            get() = stats.warStats.warsPlayed.toString()

        val winrate: Int
            get() = (stats.warStats.warsWon * 100) / stats.warStats.warsPlayed

        val winrateLabel: String
            get() = when (stats.warStats.warsPlayed) {
                0 -> "0 %"
                else -> "$winrate %"
            }
    }

    class TrackRanking(val stats: TrackStats) : RankingItem
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = StatsRankingViewModel.Factory::class)
class StatsRankingViewModel @AssistedInject constructor(
    @Assisted val type: StatsType?,
    databaseRepository: DatabaseRepositoryInterface,
    dataStoreRepository: DataStoreRepositoryInterface,
    private val statsRepository: StatsRepositoryInterface,
    @ApplicationContext private val context: Context
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(type: StatsType?): StatsRankingViewModel
    }

    data class State(
        val title: Int? = null,
        val userId: String? = null,
        val teamId: String? = null,
        val list: List<RankingItem> = listOf(),
        val index: Int = 0,
        val currentUserId: String? = null,
        val sortItems: List<Pair<SortType, Boolean>> = listOf(
            Pair(SortType.OCCURENCES, true),
            Pair(SortType.NAME, false),
            Pair(SortType.WINRATE, false),
            Pair(SortType.AVERAGE, false)
        )
    )

    private val _state = MutableStateFlow(State())
    private var currentUser: MKCPlayer? = null


    val state = databaseRepository.getWars()
        .map { warList ->
            val title = when (type) {
                is StatsType.TeamStats -> R.string.statistiques_des_joueurs
                is StatsType.OpponentStats -> R.string.statistiques_des_adversaires
                else -> R.string.statistiques_des_circuits
            }
            currentUser = dataStoreRepository.mkcPlayer.firstOrNull()
            when (type) {
                is StatsType.TeamStats -> _state.value.copy(
                    title = title,
                    list = statsRepository.playersRankList
                )

                is StatsType.OpponentStats -> _state.value.copy(
                    list = statsRepository.playerOpponentRankList,
                    title = title,
                    userId = type.userId,
                    teamId = type.teamId,
                    currentUserId = currentUser?.id.toString().takeIf { _state.value.index == 0 }
                )

                is StatsType.MapStats -> _state.value.copy(
                    list = statsRepository.playerTrackRankList,
                    title = title,
                    userId = type.userId,
                    teamId = type.teamId,
                    currentUserId = currentUser?.id.toString().takeIf { _state.value.index == 0 }
                )

                else -> _state.value.copy(title = title)
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onIndivSwitch(index: Int) {
        val isIndiv = index == 0
        val list = when (type) {
            is StatsType.OpponentStats ->
                when (isIndiv) {
                    true -> statsRepository.playerOpponentRankList
                    else -> statsRepository.opponentRankList
                }

            is StatsType.MapStats ->
                when (isIndiv) {
                    true -> statsRepository.playerTrackRankList
                    else -> statsRepository.trackRankList
                }

            else -> listOf()
        }
        _state.value = state.value.copy(
            list = list,
            index = index,
            sortItems = listOf(
                Pair(SortType.OCCURENCES, true),
                Pair(SortType.NAME, false),
                Pair(SortType.WINRATE, false),
                Pair(SortType.AVERAGE, false)
            ),
            currentUserId = currentUser?.id.toString().takeIf { isIndiv })
    }

    fun onSearch(search: String) {
        val opponentList = when (state.value.currentUserId) {
            null -> statsRepository.opponentRankList
            else -> statsRepository.playerOpponentRankList
        }.filter {
            when (search.isEmpty()) {
                true -> true
                else -> (it as? RankingItem.OpponentRanking)?.team?.name?.lowercase()
                    ?.contains(search.lowercase()) == true
            }
        }.takeIf { type is StatsType.OpponentStats }.orEmpty()

        val mapList = when (state.value.currentUserId) {
            null -> statsRepository.trackRankList
            else -> statsRepository.playerTrackRankList
        }.filter {
            when (search.isEmpty()) {
                true -> true
                else -> (it as? RankingItem.TrackRanking)?.stats?.map?.label?.let {
                    context.getString(
                        it
                    ).lowercase().contains(search.lowercase()) == true
                } == true
            }
        }.takeIf { type is StatsType.MapStats }.orEmpty()

        _state.value = state.value.copy(list = opponentList + mapList)
        state.value.sortItems.singleOrNull { it.second }?.let {
            onSortItemSelected(it.first)
        }

    }

    fun onSortItemSelected(sortItem: SortType) {
        val updatedItems = state.value.sortItems.map {
            when (it.first == sortItem) {
                true -> it.copy(second = true)
                else -> it.copy(second = false)
            }
        }

        val updatedOpponents = when (sortItem) {
            SortType.OCCURENCES -> state.value.list
                .mapNotNull { it as? RankingItem.OpponentRanking }
                .sortedByDescending { it.stats.warStats.warsPlayed }

            SortType.NAME -> state.value.list
                .mapNotNull { it as? RankingItem.OpponentRanking }
                .sortedBy { it.team.name }
            SortType.AVERAGE -> state.value.list
                .mapNotNull { it as? RankingItem.OpponentRanking }
                .sortedByDescending {
                    when (state.value.currentUserId) {
                        null -> it.stats.averagePoints
                        else -> -(it.stats.averagePlayerPosition)

                } }

            SortType.WINRATE -> state.value.list
                .mapNotNull { it as? RankingItem.OpponentRanking }
                .sortedByDescending { it.winrate }

        }
            .takeIf { type is StatsType.OpponentStats }
            .orEmpty()

        val updatedMaps = when (sortItem) {
            SortType.OCCURENCES -> state.value.list
                .mapNotNull { it as? RankingItem.TrackRanking }
                .sortedByDescending { it.stats.totalPlayed }
            SortType.NAME -> state.value.list
                .mapNotNull { it as? RankingItem.TrackRanking }
                .sortedBy { it.stats.map?.label?.let { context.getString(it) } ?: "" }
            SortType.AVERAGE -> state.value.list
                .mapNotNull { it as? RankingItem.TrackRanking }
                .sortedByDescending {
                    when (state.value.currentUserId) {
                        null -> it.stats.teamScore?.trackScoreToDiff()?.substringAfter("+")
                            ?.toIntOrNull() ?: 0
                        else -> it.stats.playerScore?.pointsToPosition()?.let { -it }
                    }
                }
            SortType.WINRATE -> state.value.list
                .mapNotNull { it as? RankingItem.TrackRanking }
                .sortedByDescending { it.stats.winRate }
        }

            .takeIf { type is StatsType.MapStats }
            .orEmpty()
        _state.value = state.value.copy(sortItems = updatedItems, list = updatedOpponents + updatedMaps)
    }
}