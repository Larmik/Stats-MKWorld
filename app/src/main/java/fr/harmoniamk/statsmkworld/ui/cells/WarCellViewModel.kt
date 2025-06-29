package fr.harmoniamk.statsmkworld.ui.cells

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import java.util.Date
import javax.inject.Inject

@HiltViewModel(assistedFactory = WarCellViewModel.Factory::class)
class WarCellViewModel @AssistedInject constructor(
    @Assisted val details: WarDetails,
    databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(details: WarDetails): WarCellViewModel
    }

    data class State(
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val score: String? = null,
        val diff: String? = null,
        val date: String? = null,
        val mapsWon: String? = null
    )

    val state = databaseRepository.getTeam(details.war.teamHost)
        .zip(databaseRepository.getTeam(details.war.teamOpponent)) { host, opponent ->

            val mapsWon = details.warTracks.filter { it.displayedDiff.startsWith("+") }.size

            State(
                teamHost = host,
                teamOpponent = opponent,
                score = details.displayedScore,
                diff = details.displayedDiff,
                date = Date(details.war.id).displayedString("dd/MM/yyyy"),
                mapsWon = "Maps gagnées : $mapsWon/12"
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}