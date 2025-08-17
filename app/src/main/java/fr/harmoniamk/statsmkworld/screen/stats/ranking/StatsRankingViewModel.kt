package fr.harmoniamk.statsmkworld.screen.stats.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
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

        val winrateLabel: String
            get() = when (stats.warStats.warsPlayed) {
                0 -> "0 %"
                else -> "${(stats.warStats.warsWon * 100) / stats.warStats.warsPlayed} %"
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
    private val statsRepository: StatsRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(type: StatsType?): StatsRankingViewModel
    }

    data class State(
        val title: String? = null,
        val userId: String? = null,
        val teamId: String? = null,
        val list: List<RankingItem> = listOf(),
        val index: Int = 0,
        val currentUserId: String? = null
    )

    private val _state = MutableStateFlow(State())
    private var currentUser: MKCPlayer? = null

    val state = databaseRepository.getWars()
        .map { warList ->
            val title = when (type) {
                is StatsType.TeamStats -> "Statistiques des joueurs"
                is StatsType.OpponentStats -> "Statistiques des adversaires"
                else -> "Statistiques des circuits"
            }
            currentUser = dataStoreRepository.mkcPlayer.firstOrNull()
            when (type) {
                is StatsType.TeamStats -> _state.value.copy(
                    title = title,
                    list = statsRepository.playersRankList
                )

                is StatsType.OpponentStats -> _state.value.copy(
                    list = statsRepository.playerOpponentRankList
                        .mapNotNull { it as? RankingItem.OpponentRanking }
                        .filter { vm ->
                            (type.userId == null && vm.stats.warStats.list.any { war ->
                                war.war.hasTeam(type.teamId)
                            }) || (type.userId != null && vm.stats.warStats.list.any { war ->
                                war.war.hasPlayer(
                                    type.userId
                                )
                            })
                        },
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
        _state.value = state.value.copy(list = list, index = index, currentUserId = currentUser?.id.toString().takeIf { index == 0 })
    }
}