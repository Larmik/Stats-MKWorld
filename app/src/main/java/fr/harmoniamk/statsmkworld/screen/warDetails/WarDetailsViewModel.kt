package fr.harmoniamk.statsmkworld.screen.warDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.extension.withPlayersList
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = WarDetailsViewModel.Factory::class)
class WarDetailsViewModel @AssistedInject constructor(
    @Assisted val warDetails: WarDetails?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(warDetails: WarDetails?): WarDetailsViewModel
    }

    data class State(
        val details: WarDetails? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val players: List<PlayerScore> = listOf()
    )

    private val _state = MutableStateFlow(State())

    val state = flowOf(warDetails)
        .filterNotNull()
        .map { details ->
            val teamHost = databaseRepository.getTeam(details.war.teamHost).firstOrNull()
            val teamOpponent = databaseRepository.getTeam(details.war.teamOpponent).firstOrNull()
            State(
                details = details,
                players = details.war.withPlayersList(databaseRepository, firebaseRepository),
                teamHost = teamHost,
                teamOpponent = teamOpponent,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)



}