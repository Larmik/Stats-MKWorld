package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.selectors.PenaltySelector
import fr.harmoniamk.statsmkworld.model.selectors.PenaltyType
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CurrentWarActionsViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface
) : ViewModel() {

    private val _backToWelcome = MutableSharedFlow<Unit>()
    val backToWelcome = _backToWelcome.asSharedFlow()


    data class State(
        val war: War? = null,
        val players: List<PlayerEntity>? = null,
        val penalties: List<PenaltySelector>? = null,
        val teamHost: String? = null,
        val teamOpponent: String? = null
    )

    private val _state = MutableStateFlow(State())

    val state = flowOf(Unit)
        .mapNotNull {
            val war = dataStoreRepository.war.firstOrNull()
            val players = databaseRepository.getPlayers().firstOrNull()
            val teamHost =
                databaseRepository.getTeam(war?.teamHost.orEmpty()).firstOrNull()?.name.orEmpty()
            val teamOpponent = databaseRepository.getTeam(war?.teamOpponent.orEmpty())
                .firstOrNull()?.name.orEmpty()
            State(
                war = war,
                players = players,
                penalties = PenaltyType.entries.map { PenaltySelector(it, false) },
                teamHost = teamHost,
                teamOpponent = teamOpponent
            )
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onPenaltySelected(penalty: PenaltySelector) {
        val newList = mutableListOf<PenaltySelector>()
        state.value.penalties?.forEach {
            when {
                it.penalty == penalty.penalty -> newList.add(penalty.copy(isSelected = true))
                it.isSelected -> newList.add(it.copy(isSelected = false))
                else -> newList.add(it)
            }
        }
        _state.value = state.value.copy(penalties = newList)
    }

    fun onPenaltyValidated() {
        state.value.penalties?.singleOrNull { it.isSelected }?.penalty?.let { penaltyType ->
            val penalty = when (penaltyType) {
                PenaltyType.REPICK_HOST -> WarPenalty(teamId = state.value.war?.teamHost.orEmpty(), 20)
                PenaltyType.INTERMISSION_HOST -> WarPenalty(teamId = state.value.war?.teamHost.orEmpty(), 15)
                PenaltyType.REPICK_OPPONENT -> WarPenalty(teamId = state.value.war?.teamOpponent.orEmpty(), 20)
                PenaltyType.INTERMISSION_OPPONENT -> WarPenalty(teamId = state.value.war?.teamOpponent.orEmpty(), 15)
            }
            state.value.war?.let {
                val penalties = mutableListOf<WarPenalty>()
                penalties.addAll(it.penalties)
                penalties.add(penalty)
                val war = it.copy(penalties = penalties)
                firebaseRepository.writeCurrentWar(war)
                    .onEach {
                        dataStoreRepository.setCurrentWar(war)
                        clearPenalties()
                        _state.value = state.value.copy(war = war)
                    }.launchIn(viewModelScope)
            }


        }

    }

    fun clearPenalties() {
        _state.value = state.value.copy(penalties = PenaltyType.entries.map { PenaltySelector(it, false) })
    }

    fun cancelWar() {
        flowOf(Unit)
            .mapNotNull {
                state.value.players?.filter { it.currentWar == state.value.war?.id.toString() }
                    ?.forEach {
                        databaseRepository.updateUser(it.id, "").firstOrNull()
                        firebaseRepository.writeUser(
                            teamId = state.value.war?.teamHost.orEmpty(),
                            user = User(
                                id = it.id,
                                currentWar = "",
                                role = it.role
                            )
                        ).firstOrNull()
                    }
                state.value.war
            }
            .flatMapLatest { firebaseRepository.deleteCurrentWar(it.teamHost) }
            .onEach {
                dataStoreRepository.deleteCurrentWar()
                _backToWelcome.emit(Unit)
            }
            .launchIn(viewModelScope)


    }

}