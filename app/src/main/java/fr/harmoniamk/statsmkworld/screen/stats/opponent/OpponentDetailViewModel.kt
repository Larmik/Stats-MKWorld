package fr.harmoniamk.statsmkworld.screen.stats.opponent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Fiche détail d'un ADVERSAIRE (`opp` du prototype, pôle Classements #27). Vue au
 * périmètre ÉQUIPE (toutes les wars face à cet adversaire), 12p. Réutilise le calcul
 * [withFullStats] (V/N/D, séries, score moyen, meilleurs circuits) déjà partagé, et
 * dérive ici les éléments spécifiques à la fiche : dernière rencontre, 5 dernières
 * confrontations, score moyen pour/contre, meilleur circuit contre eux, historique.
 *
 * Le [teamId] est un identifiant d'opposant (rosterId, ou teamId legacy). L'affichage
 * du nom/tag suit le roster ciblé et l'avatar l'équipe parente (rule 12).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = OpponentDetailViewModel.Factory::class)
class OpponentDetailViewModel @AssistedInject constructor(
    @Assisted val teamId: String,
    private val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(teamId: String): OpponentDetailViewModel
    }

    data class State(
        val loading: Boolean = true,
        val team: TeamEntity? = null,
        val stats: Stats? = null,
        // Date de la dernière confrontation (la plus récente).
        val lastMeeting: String? = null,
        // 5 dernières confrontations, plus récente en dernier (V=1 / N=0 / D=-1).
        val recentOutcomes: List<Int> = listOf(),
        // Score moyen d'équipe pour / contre (points, pénalités incluses).
        val averageScoreFor: Int = 0,
        val averageScoreAgainst: Int = 0,
        // Meilleur circuit face à eux (par winrate, seuil MIN_RANKING_SAMPLE).
        val bestTrack: TrackStats? = null,
        // Stats de manche (équipe) sur toutes les manches face à eux : Top/Bot 2→6,
        // distribution des positions, shocks — mêmes calculs que MapStats (scopé adversaire).
        val mapStats: MapStats? = null,
        // Historique des wars face à eux (plus récente en premier).
        val history: List<WarDetails> = listOf()
    )

    private val _state = MutableStateFlow(State())

    val state = databaseRepository.getWars()
        .map { wars ->
            wars
                .filter { it.hasTeam(teamId) }
                // 12p uniquement (24p relève d'un ticket dédié).
                .filter { it.teamOpponent.size == 1 }
                .map { WarDetails(War(it)) }
        }
        .flatMapLatest { wars ->
            wars.withFullStats(databaseRepository, teamId = teamId).map { stats -> wars to stats }
        }
        .map { (wars, stats) ->
            // teamId peut être un rosterId : avatar de l'équipe parente, nom/tag du roster.
            val team = databaseRepository.getTeam(teamId)?.let { resolved ->
                val roster = resolved.rosters.firstOrNull { it.id == teamId }
                resolved.copy(
                    id = teamId,
                    name = roster?.name ?: resolved.name,
                    tag = roster?.tag ?: resolved.tag
                )
            } ?: TeamEntity(id = teamId, name = "Équipe inconnue", tag = "???", color = null, logo = null)

            // Wars triées chronologiquement (war.id = timestamp) pour dernière rencontre,
            // 5 dernières et score pour/contre.
            val chronological = wars.sortedBy { it.war.id }
            val recentOutcomes = chronological.takeLast(5).map { war ->
                when {
                    war.displayedDiff.contains('+') -> 1
                    war.displayedDiff.contains('-') -> -1
                    else -> 0
                }
            }
            val averageFor = chronological
                .map { it.scoreHostWithPenalties }
                .takeIf { it.isNotEmpty() }?.let { it.sum() / it.size } ?: 0
            val averageAgainst = chronological
                .map { it.scoreOpponentWithPenalties }
                .takeIf { it.isNotEmpty() }?.let { it.sum() / it.size } ?: 0

            // Stats de manche (équipe) sur toutes les manches face à eux : mêmes calculs
            // que MapStats, mais sur l'ensemble des circuits (Top/Bot 2→6, distribution, shocks).
            val mapDetails = chronological.flatMap { war ->
                war.warTracks.map { track -> MapDetails(war = war, warTrack = track, position = null) }
            }
            val mapStats = mapDetails.takeIf { it.isNotEmpty() }
                ?.let { MapStats(list = it, userId = null, is24p = false) }

            _state.value.copy(
                loading = false,
                team = team,
                stats = stats,
                lastMeeting = chronological.lastOrNull()?.date,
                recentOutcomes = recentOutcomes,
                averageScoreFor = averageFor,
                averageScoreAgainst = averageAgainst,
                bestTrack = stats.bestMapByWinrate,
                mapStats = mapStats,
                history = chronological.reversed()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)
}
