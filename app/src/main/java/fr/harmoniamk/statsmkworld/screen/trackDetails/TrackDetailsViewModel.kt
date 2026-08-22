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
 * Relecture **en lecture seule** d'une course jouée (pôle Wars, écran `trackdetails` de la
 * maquette 5 pôles, ticket #47). Fournit à l'écran :
 * - le circuit ([WarTrackDetails.index]) + son score de manche (`hôte - adverse`) et la diff
 *   signée, pour la carte en-tête « Course N · Score X (±diff) » ;
 * - la liste des joueurs avec leur **position** et leur **nombre de shocks** (grille
 *   « Positions & shocks », lecture seule) ;
 * - la visibilité du bouton « Éditer la course ».
 *
 * **Numéro de course** ([courseNumber]) est calculé au site de navigation (CurrentWar /
 * WarDetails, où la liste ordonnée des courses est connue).
 *
 * Bouton « Éditer la course » : visible tant que la war **n'est pas validée** (encore en cours en
 * local, `dataStoreRepository.war != null`) et que l'appelant autorise l'édition ([editing]).
 * **Toutes** les courses restent éditables tant que la war n'est pas validée (y compris la
 * dernière). Depuis WarDetails (war validée/historique), [editing] vaut `false` → bouton masqué
 * naturellement — cf. ticket #47.
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
            // Une course reste éditable tant que la war n'est pas validée (encore en cours en
            // local) et que l'appelant autorise l'édition (ticket #47).
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
