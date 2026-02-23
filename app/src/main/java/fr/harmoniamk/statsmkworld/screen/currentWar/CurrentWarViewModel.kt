package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.sum
import fr.harmoniamk.statsmkworld.extension.withPlayersList
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.mapNotNull
import kotlin.collections.orEmpty

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CurrentWarViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface
) : ViewModel() {

    data class State(
        val details: WarDetails? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: List<TeamEntity>? = null,
        val players: List<PlayerScore> = listOf(),
        val isOver: Boolean = false,
        val buttonsVisible: Boolean = false,
        val roster: MKCTeamRoster? = null,
        val opponentsScores: Map<String, Int> = mutableMapOf()

    )

    private val _state = MutableStateFlow(State())
    private val _backToHome = MutableSharedFlow<Unit>()

    private val _onPage = MutableSharedFlow<Int>()
    private val _onToast = MutableSharedFlow<String>()

    val backToHome = _backToHome.asSharedFlow()

    val onPage = _onPage.asSharedFlow()
    val onToast = _onToast.asSharedFlow()

    val state = _state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    init {
        dataStoreRepository.mkcPlayer
            .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
            .flatMapLatest { firebaseRepository.listenToCurrentWar(it) }
            .filterNotNull()
            .onEach {
                dataStoreRepository.mkcTeam.firstOrNull()?.let { teamHost ->
                    val teamOpponents = it.teamOpponent.mapNotNull { databaseRepository.getTeam(it).firstOrNull() }
                    val buttonsVisible = dataStoreRepository.war.firstOrNull() != null
                    val roster = teamHost.rosters.singleOrNull { roster -> roster.id.toString() == it.teamHost }

                    _state.value = state.value.copy(
                        details = WarDetails(it),
                        players = it.withPlayersList(databaseRepository, firebaseRepository),
                        teamHost = TeamEntity(teamHost),
                        teamOpponent = teamOpponents,
                        buttonsVisible = buttonsVisible,
                        isOver = it.tracks.size == 12,
                        roster = roster
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onPageChange(index: Int) {
        viewModelScope.launch {
            _onPage.emit(index)
        }
    }

    fun onValueChange(key: String, value: String) {
        val scoreMap = mutableMapOf<String, Int>()
        if (!state.value.opponentsScores.map { it.key }.contains(key))
            value.toIntOrNull()?.let {
                scoreMap[key] = it
            }
        state.value.opponentsScores.forEach {
            if (it.key == key) {
              value.toIntOrNull()?.let {
                  scoreMap[key] = it
              }
            } else scoreMap[it.key] = it.value
        }

        _state.value = state.value.copy(opponentsScores = scoreMap)
    }

    fun onValidateScore() {
        state.value.details?.scoreHost?.let {
            viewModelScope.launch {
                val scores = state.value.opponentsScores
                val totalOpponentScore = scores.mapNotNull { it.value }.sum()
                val total = it + totalOpponentScore
                val totalTracksPlayed = state.value.details?.war?.tracks.orEmpty().size
                val totalPoints = totalTracksPlayed * 144
                if (total != totalPoints) {
                    val diff = when (total < totalPoints) {
                        true -> "${totalPoints - total} points manquants"
                        else -> "${total - totalPoints} points en trop"
                    }
                    _onToast.emit("Scores incorrects : $diff")
                } else {
                    val warScores = scores.map {
                        val penalty = state.value.details?.war?.penalties?.firstOrNull { penalty ->  penalty.teamId == it.key }?.amount ?: 0
                        WarScore(teamId = it.key, score = it.value - penalty)
                    } + listOf(
                           WarScore(
                               teamId = state.value.roster?.id.toString(),
                               score = state.value.details?.scoreHostWithPenalties ?: 0
                           )
                       )
                    state.value.details?.war?.copy(scores = warScores)?.let {
                        _state.value = state.value.copy(details = WarDetails(it))
                        onValidateWar()
                    }
                }
            }
        }
    }


    fun onValidateWar() {
        _state.value.details?.war?.let { war ->
            firebaseRepository.writeWar(war)
                .onEach {
                    databaseRepository.writeWar(WarEntity(war)).firstOrNull()
                    val players = databaseRepository.getPlayers().firstOrNull()
                    players?.filter { it.currentWar == war.id.toString() }?.forEach {
                        databaseRepository.updateUser(it.id, "").firstOrNull()
                        when (it.rosterId) {
                            "-1" ->  firebaseRepository.writeAlly(
                                teamId = state.value.details?.war?.teamHost.orEmpty(),
                                user = User(
                                    id = it.id,
                                    currentWar = "",
                                    role = it.role,
                                    name = it.name,
                                    discordId = it.discordId
                                )
                            ).firstOrNull()
                            else ->  firebaseRepository.writeUser(
                                teamId = state.value.details?.war?.teamHost.orEmpty(),
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
                    dataStoreRepository.deleteCurrentWar()
                }
                .flatMapLatest { firebaseRepository.deleteCurrentWar(war.teamHost) }
                .onEach { _backToHome.emit(Unit) }
                .launchIn(viewModelScope)
        }
    }

}