package fr.harmoniamk.statsmkworld.screen.trackDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.model.ScoringConstants
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

/**
 * Relecture **en lecture seule** d'une course jouée (pôle Wars, écran `trackdetails` de la
 * maquette 5 pôles, ticket #47). Fournit à l'écran :
 * - le circuit ([WarTrackDetails.index]) + son score de manche (`hôte - adverse`) et la diff
 *   signée, pour la carte en-tête « Course N · Score X (±diff) » ;
 * - la liste des joueurs avec leur **position** et leur **nombre de shocks** (grille
 *   « Positions & shocks », lecture seule) ;
 * - la visibilité du bouton « Éditer la course ».
 *
 * **Numéro de course** ([courseNumber]) et **course finale** ([isFinalCourse]) sont calculés au
 * site de navigation (CurrentWar / WarDetails, où la liste ordonnée des courses est connue).
 *
 * Bouton « Éditer la course » : visible seulement si une war est **en cours** en local
 * (`dataStoreRepository.war != null`), que l'appelant autorise l'édition ([editing]) **et** que
 * la course n'est **pas la course finale** de la war ([isFinalCourse] == false) — cf. ticket #47.
 */
@HiltViewModel(assistedFactory = TrackDetailsViewModel.Factory::class)
class TrackDetailsViewModel @AssistedInject constructor(
    @Assisted val details: WarTrackDetails?,
    @Assisted("editing") val editing: Boolean,
    @Assisted val courseNumber: Int,
    @Assisted("isFinalCourse") val isFinalCourse: Boolean,
    dataStoreRepository: DataStoreRepositoryInterface,
    val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            details: WarTrackDetails?,
            @Assisted("editing") editing: Boolean,
            courseNumber: Int,
            @Assisted("isFinalCourse") isFinalCourse: Boolean
        ): TrackDetailsViewModel
    }

    data class State(
        val track: WarTrackDetails? = null,
        val courseNumber: Int = 0,
        val positions: List<PlayerPosition> = listOf(),
        val shocks: Map<String, Int> = mapOf(),
        // Score de manche de l'équipe hôte (points de positions), affiché dans l'en-tête.
        val hostScore: Int = 0,
        val diff: String? = null,
        val trackScore: Int? = null,
        val buttonVisible: Boolean = false
    )

    val state = flowOf(details)
        .filterNotNull()
        .map { track ->
            // La course ne peut être éditée que pour une war en cours, si l'appelant l'autorise,
            // et jamais sur la course finale de la war (ticket #47).
            val hasCurrentWar = dataStoreRepository.war.firstOrNull() != null
            val is24p = track.is24p
            val scoreHost = track.track.positions.sumOf { it.position.positionToPoints(is24p) }
            val maxPointsPerTrack = when (is24p) {
                true -> ScoringConstants.MAX_POINTS_PER_TRACK_24P
                else -> ScoringConstants.MAX_POINTS_PER_TRACK_12P
            }
            val scoreOpponent = maxPointsPerTrack - scoreHost
            val players = mutableListOf<PlayerPosition>()
            track.track.positions.forEach { position ->
                databaseRepository.getPlayer(position.playerId).firstOrNull()?.let {
                    players.add(PlayerPosition(it, position))
                }
            }
            State(
                track = track,
                courseNumber = courseNumber,
                hostScore = scoreHost,
                diff = when {
                    (scoreHost - scoreOpponent) > 0 -> "+${scoreHost - scoreOpponent}"
                    else -> "${scoreHost - scoreOpponent}"
                },
                positions = players,
                shocks = track.track.shocks.orEmpty().associate { it.playerId to it.count },
                buttonVisible = hasCurrentWar && editing && !isFinalCourse,
                trackScore = scoreHost.takeIf { is24p }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}
