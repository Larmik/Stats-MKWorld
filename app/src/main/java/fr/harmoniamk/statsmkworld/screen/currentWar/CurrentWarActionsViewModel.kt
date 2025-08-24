package fr.harmoniamk.statsmkworld.screen.currentWar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withPlayersList
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.local.PlayerScoreForTab
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.selectors.PenaltySelector
import fr.harmoniamk.statsmkworld.model.selectors.PenaltyType
import fr.harmoniamk.statsmkworld.model.selectors.PlayerSelector
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.PDFRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CurrentWarActionsViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val pdfRepository: PDFRepositoryInterface
) : ViewModel() {

    private val _backToWelcome = MutableSharedFlow<Unit>()
    private val _onBack = MutableSharedFlow<Unit>()
    val backToWelcome = _backToWelcome.asSharedFlow()
    val onBack = _onBack.asSharedFlow()

    private val _rows = MutableStateFlow(6)
    private val _uri = MutableSharedFlow<Uri?>()
    private val _toast = MutableSharedFlow<String?>()

    val rows = _rows.asStateFlow()
    val uri = _uri.asSharedFlow()
    val toast = _toast.asSharedFlow()


    data class State(
        val war: War? = null,
        val players: List<PlayerEntity>? = null,
        val penalties: List<PenaltySelector>? = null,
        val teamHost: String? = null,
        val teamOpponent: String? = null,
        val currentPlayers: List<PlayerSelector>? = null,
        val otherPlayers: List<PlayerSelector>? = null,
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
                teamOpponent = teamOpponent,
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

    fun onOldPlayerSelected(player: PlayerEntity) {
        val newList = state.value.currentPlayers.orEmpty().map { it.copy(isSelected = it.player.id == player.id) }
        _state.value = state.value.copy(currentPlayers = newList)
    }

    fun onNewPlayerSelected(player: PlayerEntity) {
        val newList = state.value.otherPlayers.orEmpty().map { it.copy(isSelected = it.player.id == player.id) }
        _state.value = state.value.copy(otherPlayers = newList)
    }

    fun onSub() {
        flowOf(Unit)
            .mapNotNull {
                val oldPlayer = state.value.currentPlayers?.singleOrNull { it.isSelected }
                val newPlayer = state.value.otherPlayers?.singleOrNull { it.isSelected }
                oldPlayer?.player?.let {
                    databaseRepository.updateUser(it.id, "").firstOrNull()
                    firebaseRepository.writeUser(
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
                newPlayer?.player?.let {
                    databaseRepository.updateUser(it.id, state.value.war?.id.toString()).firstOrNull()
                    firebaseRepository.writeUser(
                        teamId = state.value.war?.teamHost.orEmpty(),
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
            .onEach { _onBack.emit(Unit) }
            .launchIn(viewModelScope)
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
                                role = it.role,
                                name = it.name,
                                discordId = it.discordId
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


    fun onManageRows(isAdding: Boolean) {
        when (isAdding) {
            true -> _rows.value += 1
            else -> _rows.value -= 1
        }
    }

    fun onGenerate(players: List<String>, scores: List<String>) {
        val filename = "war_" + Date().time.toString()
        dataStoreRepository.war
            .filterNotNull()
            .mapNotNull {
                val details = WarDetails(it)
                if (scores.mapNotNull { it.toIntOrNull() }.sum() == details.scoreOpponent) {
                    var teamWin: TeamEntity? = null
                    var teamLose: TeamEntity? = null
                    val playerScores = it.withPlayersList(databaseRepository, firebaseRepository).map { PlayerScoreForTab(it) }
                    val opponentScores = players.mapIndexed { index, player -> PlayerScoreForTab(player, scores[index].toInt()) }
                    if (details.scoreHostWithPenalties >= details.scoreOpponentWithPenalties) {
                        teamWin = databaseRepository.getTeam(it.teamHost).firstOrNull()
                        teamLose = databaseRepository.getTeam(it.teamOpponent).firstOrNull()
                    }
                    else {
                        teamWin = databaseRepository.getTeam(it.teamOpponent).firstOrNull()
                        teamLose = databaseRepository.getTeam(it.teamHost).firstOrNull()
                    }
                    pdfRepository.generatePdf(details, teamWin, teamLose, playerScores, opponentScores)
                } else {
                    val diff = scores.mapNotNull { it.toIntOrNull() }.sum() - details.scoreOpponent
                    val secondaryLabel = when  {
                        diff > 0 -> "$diff points en trop"
                        else -> "${0-diff} points manquants"
                    }
                    _toast.emit("Les scores des joueurs sont incorrects ($secondaryLabel)")
                    null
                }
            }
            .flatMapLatest { pdfRepository.write(it, filename) }
            .onEach { uri -> _uri.emit(uri) }
            .launchIn(scope = viewModelScope)
    }

}