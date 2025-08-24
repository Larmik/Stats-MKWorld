package fr.harmoniamk.statsmkworld.screen.addWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.selectors.PlayerSelector
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddWarViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
) : ViewModel() {

    data class State(
        val teamList: List<TeamEntity> = listOf(),
        val playerList: List<PlayerSelector> = listOf(),
        val teamSelected: TeamEntity? = null,
        val buttonEnabled: Boolean = false,
        val warName: String? = null
    )

    private val _state = MutableStateFlow(State())
    private var teams = listOf<TeamEntity>()
    private var players = listOf<PlayerEntity>()
    private var currentTeam: MKCTeam? = null

    private val _goToCurrent = MutableSharedFlow<Unit>()
    val goToCurrent = _goToCurrent.asSharedFlow()

    val state = databaseRepository.getTeams()
        .zip(databaseRepository.getPlayers()) { teams, players ->
            this.teams = teams
            this.players = players
            this.currentTeam = dataStoreRepository.mkcTeam.firstOrNull()
            State(
                teamList = teams,
                playerList = players.map { PlayerSelector(it, false) }
            )
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onSearchTeam(search: String) {
        _state.value = state.value.copy(teamList = teams.filter {
            it.tag.lowercase()
                .contains(search.lowercase()) || it.name.lowercase().contains(search.lowercase())
        }.sortedBy { it.name })
    }

    fun onTeamSelected(team: TeamEntity) {
        _state.value = state.value.copy(
            teamSelected = team,
            warName = "${currentTeam?.tag} - ${team.tag}"
            )
    }

    fun onPlayerSelected(player: PlayerEntity) {
        val newValues = mutableListOf<PlayerSelector>()
        state.value.playerList.forEach {
            when (it.player.id == player.id) {
                true -> newValues.add(it.copy(isSelected = !it.isSelected))
                else -> newValues.add(it)
            }
        }
        _state.value = state.value.copy(
            playerList = newValues,
            buttonEnabled = newValues.filter { it.isSelected }.size == 6
        )
    }

    fun createWar() {
        flowOf(currentTeam?.id)
            .filterNotNull()
            .map {
                War(
                    id = System.currentTimeMillis(),
                    teamHost = it.toString(),
                    teamOpponent = _state.value.teamSelected?.id.orEmpty(),
                    tracks = listOf(),
                    penalties = listOf()
                )
            }
            .onEach { war ->
                _state.value.playerList.filter { it.isSelected }.forEach {
                    firebaseRepository.writeUser(
                        teamId = currentTeam?.id.toString(),
                        user = User(
                            id = it.player.id,
                            currentWar = war.id.toString(),
                            role = it.player.role,
                            name = it.player.name,
                            discordId = it.player.discordId
                        )
                    ).firstOrNull()
                    databaseRepository.updateUser(it.player.id, war.id.toString()).firstOrNull()
                }
            }
            .onEach {  dataStoreRepository.setCurrentWar(it) }
            .flatMapLatest { firebaseRepository.writeCurrentWar(it) }
            .onEach { _goToCurrent.emit(Unit) }
            .launchIn(viewModelScope)
    }

}
