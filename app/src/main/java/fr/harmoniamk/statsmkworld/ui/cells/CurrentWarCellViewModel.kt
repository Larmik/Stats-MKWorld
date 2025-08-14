package fr.harmoniamk.statsmkworld.ui.cells

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CurrentWarCellViewModel.Factory::class)
class CurrentWarCellViewModel @AssistedInject constructor(
    @Assisted val currentWar: War?,
    databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(currentWar: War?): CurrentWarCellViewModel
    }

    data class State(
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val score: String? = null,
        val diff: String? = null,
        val remaining: Int? = null
    )

    val state = flowOf(currentWar)
            .filterNotNull()
            .map { war ->
                val details = WarDetails(war)
                val teamHost = databaseRepository.getTeam(details.war.teamHost).firstOrNull()
                val teamOpponent =
                    databaseRepository.getTeam(details.war.teamOpponent).firstOrNull()
                State(
                    teamHost = teamHost,
                    teamOpponent = teamOpponent,
                    score = details.displayedScore,
                    diff = details.displayedDiff,
                    remaining = 12 - war.tracks.size
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())


}