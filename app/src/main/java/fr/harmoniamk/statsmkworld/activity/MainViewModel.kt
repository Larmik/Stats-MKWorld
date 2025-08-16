package fr.harmoniamk.statsmkworld.activity

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.BuildConfig
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.extension.withFullTeamStats
import fr.harmoniamk.statsmkworld.extension.withTrackStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.RemoteConfigRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.StatsRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepositoryInterface,
    remoteConfigRepository: RemoteConfigRepositoryInterface,
    private val statsRepository: StatsRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    data class State(
        val startDestination: String? = null,
        val code: String = "",
        val currentPage: Int? = null,
        val needUpdate: Boolean = false
    )

    private val _state = MutableStateFlow(State())

    val state = flowOf(Unit)
        .map {
            val player = dataStoreRepository.mkcPlayer.firstOrNull()
            val currentPage = dataStoreRepository.page.firstOrNull()
            when {
                remoteConfigRepository.minimumVersion > BuildConfig.VERSION_CODE -> _state.value.copy(needUpdate = true)
                player?.id != 0L  -> _state.value.copy(startDestination = "Home")
                else -> _state.value.copy(currentPage = currentPage, startDestination = "Signup")
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    fun processIntent(intent: Intent) {
        intent.dataString?.split("?")?.lastOrNull()?.split("=")?.lastOrNull()?.let { code ->
            _state.value = _state.value.copy(code = code, startDestination = "Signup")
        }
    }


    suspend fun initStats() {
        databaseRepository.getWars().firstOrNull()?.let { warList ->
            val currentTeam = dataStoreRepository.mkcTeam.firstOrNull()
            val currentPlayer = dataStoreRepository.mkcPlayer.firstOrNull()

            statsRepository.trackRankList = warList.withTrackStats(currentPlayer?.id.toString()).map { RankingItem.TrackRanking(it) }
            statsRepository.playerTrackRankList = warList.filter { it.hasPlayer(currentPlayer?.id.toString()) == true }.withTrackStats(currentPlayer?.id.toString()).map { RankingItem.TrackRanking(it) }
            databaseRepository.getPlayers()
                .mapNotNull { it.sortedBy { it.name } }
                .firstOrNull()
                ?.let { userList ->
                    val players = mutableListOf<RankingItem>()
                    userList.forEach { user ->
                        warList
                            .filter { war -> war.hasPlayer(user.id) }
                            .map { WarDetails(War(it)) }
                            .withFullStats(databaseRepository, userId = user.id)
                            .map { players.add(RankingItem.PlayerRanking(user, it)) }
                            .firstOrNull()
                    }
                    statsRepository.playersRankList = players
                }
            databaseRepository.getTeams()
                .map { it.filterNot { team ->  team.id == currentTeam?.id.toString() } }
                .mapNotNull { it.sortedBy { it.name } }
                .flatMapLatest {
                    it.withFullTeamStats(
                        wars = warList,
                        databaseRepository = databaseRepository
                    )
                }
                .mapNotNull { it.map { RankingItem.OpponentRanking(it.first, it.second) } }
                .firstOrNull()
                ?.let { opponents -> statsRepository.opponentRankList = opponents }
        }
    }
}

