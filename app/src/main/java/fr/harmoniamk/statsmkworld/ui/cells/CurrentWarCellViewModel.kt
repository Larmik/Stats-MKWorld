package fr.harmoniamk.statsmkworld.ui.cells

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.opponentTeams
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CurrentWarCellViewModel.Factory::class)
class CurrentWarCellViewModel @AssistedInject constructor(
    @Assisted val currentWar: War?,
    databaseRepository: DatabaseRepositoryInterface,
    dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(currentWar: War?): CurrentWarCellViewModel
    }

    data class State(
        val details: WarDetails? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: List<TeamEntity>? = null,
        val score: String? = null,
        val diff: String? = null,
        val remaining: Int? = null,
        val rosterName: String? = null,
        val rosterId: String? = null
    )

    val state = flowOf(currentWar)
            .filterNotNull()
            .zip(dataStoreRepository.mkcTeam) { war, teamHost ->
                val details = WarDetails(war)
                val teamOpponents = details.war.opponentTeams(databaseRepository)
                val hostRoster = teamHost.rosters.singleOrNull { it.id.toString() == war.teamHost }
                val rosterName = hostRoster?.name ?: teamHost.name
                val rosterId = hostRoster?.id ?: teamHost.id
                State(
                    details = details,
                    // Avatar de l'équipe, nom/tag du roster hôte.
                    teamHost = TeamEntity(teamHost).copy(name = rosterName, tag = hostRoster?.tag ?: teamHost.tag),
                    teamOpponent = teamOpponents,
                    score = details.displayedScore,
                    diff = details.displayedDiff,
                    remaining = 12 - war.tracks.size,
                    rosterName = rosterName,
                    rosterId = rosterId.toString()
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())


}