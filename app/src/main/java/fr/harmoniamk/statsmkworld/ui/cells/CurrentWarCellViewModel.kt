package fr.harmoniamk.statsmkworld.ui.cells

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CurrentWarCellViewModel @Inject constructor(
    databaseRepository: DatabaseRepositoryInterface,
    dataStoreRepository: DataStoreRepositoryInterface,
    firebaseRepository: FirebaseRepositoryInterface
) : ViewModel() {

    data class State(
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val score: String? = null,
        val diff: String? = null,
        val remaining: Int? = null
    )

    val state = dataStoreRepository.mkcTeam
            .flatMapLatest { firebaseRepository.getCurrentWar(it.id.toString()) }
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