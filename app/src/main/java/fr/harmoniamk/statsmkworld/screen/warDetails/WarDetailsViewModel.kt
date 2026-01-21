package fr.harmoniamk.statsmkworld.screen.warDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.withPlayersList
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = WarDetailsViewModel.Factory::class)
class WarDetailsViewModel @AssistedInject constructor(
    @Assisted val warDetails: WarDetails?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(warDetails: WarDetails?): WarDetailsViewModel
    }

    data class State(
        val details: WarDetails? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val players: List<PlayerScore> = listOf(),
        val roster: MKCTeamRoster? = null
    )

    private val _state = MutableStateFlow(State())

    val state = flowOf(warDetails)
        .filterNotNull()
        .zip(dataStoreRepository.mkcTeam) { details, teamHost ->
            val teamOpponent = databaseRepository.getTeam(details.war.teamOpponent).firstOrNull()
            val roster = teamHost.rosters.singleOrNull { it.id.toString() == details.war.teamHost }
            State(
                details = details,
                players = details.war.withPlayersList(databaseRepository, firebaseRepository),
                teamHost = TeamEntity(teamHost),
                teamOpponent = teamOpponent,
                roster = roster
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)



}