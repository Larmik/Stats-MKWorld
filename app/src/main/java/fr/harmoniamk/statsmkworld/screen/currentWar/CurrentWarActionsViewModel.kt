package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.OldWar
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.selectors.PenaltySelector
import fr.harmoniamk.statsmkworld.model.selectors.PenaltyType
import fr.harmoniamk.statsmkworld.model.selectors.PlayerSelector
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
    private val _onBack = MutableSharedFlow<Unit>()
    val backToWelcome = _backToWelcome.asSharedFlow()
    val onBack = _onBack.asSharedFlow()

    data class State(
        val war: War? = null,
        val players: List<PlayerEntity>? = null,
        val penalties: List<PenaltySelector>? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: List<TeamEntity>? = null,
        val currentPlayers: List<PlayerSelector>? = null,
        val otherPlayers: List<PlayerSelector>? = null,
    )

    private val _state = MutableStateFlow(State())

    val state = flowOf(Unit)
        .mapNotNull {
            val war = dataStoreRepository.war.firstOrNull()
            val players = databaseRepository.getPlayers().firstOrNull()
            val roster = dataStoreRepository.mkcTeam.firstOrNull()?.rosters?.singleOrNull { it.id.toString() == war?.teamHost }
            val teamHost = roster?.let { TeamEntity(it) }
            val teamOpponents = war?.teamOpponent.orEmpty().mapNotNull { databaseRepository.getTeam(it).firstOrNull() }

            State(
                war = war,
                players = players,
                penalties = (listOfNotNull(roster?.id).map { it.toString() } + teamOpponents.map { it.id })
                    .flatMap { team -> listOf(
                        PenaltyType.Minus10(team),
                        PenaltyType.Minus15(team),
                        PenaltyType.Minus20(team)
                    ) }
                    .map { PenaltySelector(it, false) },
                teamHost = teamHost,
                teamOpponent = teamOpponents,
                currentPlayers = players?.filter { it.currentWar == war?.id.toString() }?.map { PlayerSelector(it, false) },
                otherPlayers = players?.filterNot { it.currentWar == war?.id.toString() }?.map { PlayerSelector(it, false) }
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
                is PenaltyType.Minus10 -> WarPenalty(teamId = penaltyType.teamId, 10)
                is PenaltyType.Minus15 -> WarPenalty(teamId = penaltyType.teamId, 15)
                is PenaltyType.Minus20 -> WarPenalty(teamId = penaltyType.teamId, 20)
             }
            state.value.war?.let {
                val penalties = mutableListOf<WarPenalty>()
                val scores = mutableListOf<WarScore>()
                penalties.addAll(it.penalties)
                penalties.add(penalty)
                it.scores.forEach { score ->
                    when (score.teamId == penalty.teamId) {
                        true -> scores.add(score.copy(score = score.score - penalty.amount))
                        else -> scores.add(score)
                    }
                }

                val war = it.copy(penalties = penalties, scores = scores)
                firebaseRepository.writeCurrentWar(war)
                    .onEach {
                        dataStoreRepository.setCurrentWar(war)
                        clearPenalties()
                        _state.value = state.value.copy(war = war)
                    }.launchIn(viewModelScope)
            }
        }
    }

    fun onOldPlayerSelected(player: PlayerEntity) {
        val newList = state.value.currentPlayers.orEmpty().map { it.copy(isSelected = it.player.id == player.id) }
        _state.value = state.value.copy(currentPlayers = newList)
    }

    fun onNewPlayerSelected(player: PlayerEntity) {
        val newList = state.value.otherPlayers.orEmpty().map { it.copy(isSelected = it.player.id == player.id) }
        _state.value = state.value.copy(otherPlayers = newList)
    }

    fun onSub() {
        dataStoreRepository.mkcTeam
            .mapNotNull { team ->
                val oldPlayer = state.value.currentPlayers?.singleOrNull { it.isSelected }
                val newPlayer = state.value.otherPlayers?.singleOrNull { it.isSelected }
                oldPlayer?.player?.let {
                    databaseRepository.updateUser(it.id, "").firstOrNull()
                    when (it.rosterId) {
                        "-1" -> firebaseRepository.writeAlly(
                            teamId = team.id.toString(),
                            user = User(
                                id = it.id,
                                currentWar = "",
                                role = it.role,
                                name = it.name,
                                discordId = it.discordId
                            )
                        ).firstOrNull()
                        else -> firebaseRepository.writeUser(
                            teamId = team.id.toString(),
                            user = User(
                                id = it.id,
                                currentWar = "",
                                role = it.role,
                                name = it.name,
                                discordId = it.discordId
                            )
                        ).firstOrNull()
                    }

                }
                newPlayer?.player?.let {
                    databaseRepository.updateUser(it.id, state.value.war?.id.toString()).firstOrNull()
                    when (it.rosterId) {
                        "-1" ->  firebaseRepository.writeAlly(
                            teamId = team.id.toString(),
                            user = User(
                                id = it.id,
                                currentWar = state.value.war?.id.toString(),
                                role = it.role,
                                name = it.name,
                                discordId = it.discordId
                            )
                        ).firstOrNull()
                        else -> firebaseRepository.writeUser(
                            teamId = team.id.toString(),
                            user = User(
                                id = it.id,
                                currentWar = state.value.war?.id.toString(),
                                role = it.role,
                                name = it.name,
                                discordId = it.discordId
                            )
                        ).firstOrNull()
                    }
                }
            }
            .onEach { _onBack.emit(Unit) }
            .launchIn(viewModelScope)
    }

    fun clearPenalties() {
        _state.value = state.value.copy(
            penalties = (listOf(state.value.teamHost) + state.value.teamOpponent.orEmpty())
            .filterNotNull()
            .flatMap { team -> listOf(
                PenaltyType.Minus10(team.id),
                PenaltyType.Minus15(team.id),
                PenaltyType.Minus20(team.id)
            ) }
            .map { PenaltySelector(it, false) })
    }

    fun cancelWar() {
        flowOf(Unit)
            .mapNotNull {
                state.value.players?.filter { it.currentWar == state.value.war?.id.toString() }?.forEach {
                        databaseRepository.updateUser(it.id, "").firstOrNull()
                        when (it.rosterId) {
                            "-1" -> firebaseRepository.writeAlly(
                                teamId = state.value.war?.teamHost.orEmpty(),
                                user = User(
                                    id = it.id,
                                    currentWar = "",
                                    role = it.role,
                                    name = it.name,
                                    discordId = it.discordId
                                )
                            ).firstOrNull()
                            else -> firebaseRepository.writeUser(
                                teamId = state.value.war?.teamHost.orEmpty(),
                                user = User(
                                    id = it.id,
                                    currentWar = "",
                                    role = it.role,
                                    name = it.name,
                                    discordId = it.discordId
                                )
                            ).firstOrNull()
                        }
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