package fr.harmoniamk.statsmkworld.screen.trackDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = TrackDetailsViewModel.Factory::class)
class TrackDetailsViewModel @AssistedInject constructor(
    @Assisted val details: WarTrackDetails?,
    @Assisted val editing: Boolean,
    dataStoreRepository: DataStoreRepositoryInterface,
    val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(details: WarTrackDetails?, editing: Boolean): TrackDetailsViewModel
    }

    data class State(
        val track: WarTrackDetails? = null,
        val positions: List<PlayerPosition> = listOf(),
        val score: String? = null,
        val diff: String? = null,
        val buttonVisible: Boolean = false

        )
    val state = flowOf(details)
        .filterNotNull()
        .map {
            val buttonsVisible = dataStoreRepository.war.firstOrNull() != null
            val scoreHost = it.track.positions.map { it.position }.sumOf { it.positionToPoints() }
            val scoreOpponent = 82 - scoreHost
            val players = mutableListOf<PlayerPosition>()
            it.track.positions.forEach { pos ->
                databaseRepository.getPlayer(pos.playerId).firstOrNull()?.let {
                    players.add(PlayerPosition(it, pos))
                }
            }
            State(
                track = it,
                score = "$scoreHost - $scoreOpponent",
                diff = when {
                    (scoreHost - scoreOpponent) > 0 -> "+${scoreHost - scoreOpponent}"
                    else -> "${scoreHost - scoreOpponent}"
                },
                positions = players,
                buttonVisible = buttonsVisible && editing
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}