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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import java.util.Date

@HiltViewModel(assistedFactory = WarCellViewModel.Factory::class)
class WarCellViewModel @AssistedInject constructor(
    @Assisted val details: WarDetails,
    databaseRepository: DatabaseRepositoryInterface,
    dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(details: WarDetails): WarCellViewModel
    }

    data class State(
        val details: WarDetails? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: List<TeamEntity>? = null,
        val score: String? = null,
        val diff: String? = null,
        val date: String? = null,
        val mapsWon: Int? = null,
        val rosterName: String? = null,
        val rosterId: String? = null
    )

    val state = dataStoreRepository.mkcTeam
        .map { host ->
            val opponents = details.war.teamOpponent.mapNotNull { databaseRepository.getTeam(it).firstOrNull() }
            val mapsWon = details.warTracks.filter { it.displayedDiff.startsWith("+") }.size
            val rosterName = host.rosters.singleOrNull { it.id.toString() == details.war.teamHost }?.name ?: host.name
            val rosterId = host.rosters.singleOrNull { it.id.toString() == details.war.teamHost }?.id ?: host.id

            State(
                details = details,
                teamHost = TeamEntity(host),
                teamOpponent = opponents,
                score = details.displayedScore,
                diff = details.displayedDiff,
                date = Date(details.war.id).displayedString("dd/MM/yyyy"),
                mapsWon = mapsWon,
                rosterName = rosterName,
                rosterId = rosterId.toString()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}