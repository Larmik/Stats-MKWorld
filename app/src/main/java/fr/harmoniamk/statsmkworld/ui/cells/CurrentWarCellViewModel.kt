package fr.harmoniamk.statsmkworld.ui.cells

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.firebase.OldWar
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
        val teamHost: TeamEntity? = null,
        val teamOpponent: List<TeamEntity>? = null,
        val score: String? = null,
        val diff: String? = null,
        val remaining: Int? = null,
        val rosterName: String? = null
    )

    val state = flowOf(currentWar)
            .filterNotNull()
            .zip(dataStoreRepository.mkcTeam) { war, teamHost ->
                val details = WarDetails(war)
                val teamOpponents = details.war.teamOpponent.mapNotNull { databaseRepository.getTeam(it).firstOrNull() }

                val rosterName = teamHost.rosters.singleOrNull { it.id.toString() == war.teamHost }?.name ?: teamHost.name
                State(
                    teamHost = TeamEntity(teamHost),
                    teamOpponent = teamOpponents,
                    score = details.displayedScore,
                    diff = details.displayedDiff,
                    remaining = 12 - war.tracks.size,
                    rosterName = rosterName
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())


}