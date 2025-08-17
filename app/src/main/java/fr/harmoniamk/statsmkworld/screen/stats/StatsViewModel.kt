package fr.harmoniamk.statsmkworld.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
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

sealed class StatsType(val title: String): Serializable {
    class PlayerStats(val userId: String) : StatsType("Statistiques du joueur")
    class TeamStats : StatsType("Statistiques de l'équipe")
    class OpponentStats(
        val teamId: String,
        val userId: String? = null
    ) : StatsType("Statistiques de l'adversaire")

    class MapStats(
        val userId: String? = null,
        val teamId: String? = null,
        val trackIndex: Int? = null
    ) : StatsType("Statistiques du circuit")
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
        val map: Maps? = null
    )

    private val wars = mutableListOf<WarDetails>()
    private var team: MKCTeam? = null

    private val _state = MutableStateFlow(State())

    val state = databaseRepository.getWars()
        .map {
            team = dataStoreRepository.mkcTeam.firstOrNull()
            when {
                type is StatsType.PlayerStats -> it.filter { war -> war.hasPlayer(type.userId) }
                type is StatsType.TeamStats -> it.filter { war -> war.hasTeam(team?.id.toString()) }
                type is StatsType.OpponentStats -> it
                    .filter { war -> war.hasTeam(type.teamId) }
                    .filter { war -> (type.userId != null && war.hasPlayer(type.userId)) || type.userId == null }
                type is StatsType.MapStats -> it
                    .filter { war -> (type.teamId != null && war.hasTeam(type.teamId)) || type.teamId == null }
                    .filter { war -> (type.userId != null && war.hasPlayer(type.userId)) || type.userId == null }
                else -> it
            }
        }
        .filterNot { it.isEmpty() }
        .map { it.map { WarDetails(War(it)) } }
        .flatMapLatest { wars ->
            this.wars.clear()
            this.wars.addAll(wars)
            when {
                type is StatsType.PlayerStats -> wars.withFullStats(databaseRepository, userId = type.userId)
                type is StatsType.OpponentStats -> wars.withFullStats(databaseRepository, teamId = type.teamId, userId = type.userId)
                type is StatsType.MapStats -> wars.withFullStats(databaseRepository, teamId = type.teamId, userId = type.userId)
                else -> wars.withFullStats(databaseRepository)
            }
        }
        .map { stats ->
            val userId = (type as? StatsType.OpponentStats)?.userId
                ?: (type as? StatsType.MapStats)?.userId
                ?: (type as? StatsType.PlayerStats)?.userId

            val teamId = (type as? StatsType.OpponentStats)?.teamId
                ?: (type as? StatsType.MapStats)?.teamId
                ?: (type as? StatsType.TeamStats)?.let { dataStoreRepository.mkcTeam.firstOrNull()?.id.toString() }

            val player = databaseRepository.getPlayer(userId.orEmpty()).firstOrNull()
            val team = databaseRepository.getTeam(teamId.orEmpty()).firstOrNull()

            when (type) {
                is StatsType.PlayerStats, is StatsType.TeamStats, is StatsType.OpponentStats -> _state.value = _state.value.copy(stats = stats)
                is StatsType.MapStats -> {
                    val finalList = mutableListOf<MapDetails>()
                    wars.forEach { mkWar ->
                        mkWar.warTracks.filter { track -> track.index == type.trackIndex }.forEach { track ->
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
                            userId = type.userId
                        ),
                        map = Maps.entries[mapDetailsList.first().warTrack.track.index]
                    )
                }
                else -> {}
            }
            _state.value.copy(team = team, player = player)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

}