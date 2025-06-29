package fr.harmoniamk.statsmkworld.screen.warDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.PositionDetails
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = WarDetailsViewModel.Factory::class)
class WarDetailsViewModel @AssistedInject constructor(
    @Assisted val warDetails: WarDetails?,
    private val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(warDetails: WarDetails?): WarDetailsViewModel
    }



    data class State(
        val details: WarDetails? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val players: List<PlayerScore> = listOf()
    )

    private val _state = MutableStateFlow(State())

    val state = flowOf(warDetails)
        .filterNotNull()
        .map { details ->
            val teamHost = databaseRepository.getTeam(details.war.teamHost).firstOrNull()
            val teamOpponent = databaseRepository.getTeam(details.war.teamOpponent).firstOrNull()
            State(
                details = details,
                players = initPlayersList(details.war),
                teamHost = teamHost,
                teamOpponent = teamOpponent,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)



    private suspend fun initPlayersList(war: War): List<PlayerScore> {
        val players = databaseRepository.getPlayers().firstOrNull()
            ?.filter { player -> war.tracks.any { it.positions.any { it.playerId == player.id } } }
            ?.map { PlayerScore(it, 0) }
            .orEmpty()
        val trackList = war.tracks
        val finalList = mutableListOf<PlayerScore>()
        val positions = mutableListOf<Pair<PlayerEntity?, Int>>()
        trackList.forEach {
            it.positions.takeIf { it.isNotEmpty() }?.let { warPositions ->
                val trackPositions = mutableListOf<PositionDetails>()
                warPositions.forEach { position ->
                    trackPositions.add(
                        PositionDetails(
                            position = position.position,
                            player = players.map { it.player }.singleOrNull { it?.id == position.playerId }
                        )
                    )

                }
                trackPositions.groupBy { it.player }.entries.forEach { entry ->
                    positions.add(
                        Pair(
                            entry.key,
                            entry.value.sumOf { pos -> pos.position.positionToPoints() }
                        )
                    )
                }
            }
        }
        val temp = positions.groupBy { it.first }
            .map { Pair(it.key, it.value.map { it.second }.sum()) }
            .sortedByDescending { it.second }
        temp.forEach { pair ->
            finalList.add(
                PlayerScore(
                player = pair.first,
                score = pair.second,
            )
            )
        }
        players
            .filter { !finalList.map { it.player?.id }.contains(it.player?.id) }
            .forEach { finalList.add(it) }
        return finalList
    }





}