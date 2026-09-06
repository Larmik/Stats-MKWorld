package fr.harmoniamk.statsmkworld.screen.trackDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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

/**
 * Relecture en lecture seule d'une course jouée (#47) : circuit + score de manche, joueurs
 * (position + shocks), et visibilité du bouton « Éditer ». [courseNumber] est calculé au site de
 * navigation. Bouton « Éditer » visible tant que la war n'est pas validée (`war != null`) ET que
 * [editing] est vrai (false depuis WarDetails → masqué).
 */
@HiltViewModel(assistedFactory = TrackDetailsViewModel.Factory::class)
class TrackDetailsViewModel @AssistedInject constructor(
    @Assisted val details: WarTrackDetails?,
    @Assisted val editing: Boolean,
    @Assisted val courseNumber: Int,
    dataStoreRepository: DataStoreRepositoryInterface,
    val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            details: WarTrackDetails?,
            editing: Boolean,
            courseNumber: Int
        ): TrackDetailsViewModel
    }

    data class State(
        val track: WarTrackDetails? = null,
        val courseNumber: Int = 0,
        val positions: List<PlayerPosition> = listOf(),
        // Nombre de shocks par joueur (playerId → count), pour la grille « Positions & shocks ».
        val shocks: Map<String, Int> = mapOf(),
        val buttonVisible: Boolean = false
    )

    val state = flowOf(details)
        .filterNotNull()
        .map { track ->
            // Éditable tant que la war n'est pas validée et que l'appelant l'autorise (#47).
            val hasCurrentWar = dataStoreRepository.war.firstOrNull() != null
            val players = mutableListOf<PlayerPosition>()
            track.track.positions.forEach { position ->
                databaseRepository.getPlayer(position.playerId).firstOrNull()?.let {
                    players.add(PlayerPosition(it, position))
                }
            }
            State(
                track = track,
                courseNumber = courseNumber,
                positions = players,
                shocks = track.track.shocks.orEmpty().associate { it.playerId to it.count },
                buttonVisible = hasCurrentWar && editing
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}
