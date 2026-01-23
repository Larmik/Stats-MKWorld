package fr.harmoniamk.statsmkworld.screen.addWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.OldWar
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
        val playerList: Map<String, List<PlayerSelector>> = mapOf(),
        val teamSelected: TeamEntity? = null,
        val buttonEnabled: Boolean = false,
        val warName: String? = null
    )

    private val _state = MutableStateFlow(State())
    private var teams = listOf<TeamEntity>()
    private var players = listOf<PlayerEntity>()
    private var currentTeam: MKCTeam? = null
    private var rosterId: String? = null

    private val _goToCurrent = MutableSharedFlow<Unit>()
    val goToCurrent = _goToCurrent.asSharedFlow()

    val state = databaseRepository.getTeams()
        .zip(databaseRepository.getPlayers()) { teams, players ->
            val team = dataStoreRepository.mkcTeam.firstOrNull()
            this.teams = teams
            this.players = players
            this.currentTeam = team
            State(
                teamList = teams,
                playerList = players.map { PlayerSelector(it, false) }.groupBy { selector ->
                    val roster = team?.rosters?.firstOrNull { it.id.toString() == selector.player.rosterId }
                    roster?.name.orEmpty()
                }
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
        val newValues = mutableMapOf<String, List<PlayerSelector>>()
        state.value.playerList.forEach { pair ->
            val newList = mutableListOf<PlayerSelector>()
            pair.value.forEach {
                when (it.player.id == player.id) {
                    true -> newList.add(it.copy(isSelected = !it.isSelected))
                    else -> newList.add(it)
                }
            }
            newValues[pair.key] = newList
        }
        _state.value = state.value.copy(
            playerList = newValues,
            buttonEnabled = newValues.flatMap { it.value }.filter { it.isSelected }.size == 6
        )
    }

    fun createWar() {
        dataStoreRepository.mkcPlayer
            .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
            .filterNotNull()
            .map {
                this.rosterId = it
                War(
                    id = System.currentTimeMillis(),
                    teamHost = it,
                    teamOpponent = listOf(_state.value.teamSelected?.id.orEmpty()),
                    tracks = listOf(),
                    penalties = listOf()
                )
            }
            .zip(dataStoreRepository.mkcTeam) { war, team ->
                _state.value.playerList.flatMap { it.value }.filter { it.isSelected }.forEach {
                    when (it.player.rosterId) {
                        "-1" -> firebaseRepository.writeAlly(
                            teamId = team.id.toString(),
                            user = User(
                                id = it.player.id,
                                currentWar = war.id.toString(),
                                role = it.player.role,
                                name = it.player.name,
                                discordId = it.player.discordId
                            )
                        ).firstOrNull()
                        else -> firebaseRepository.writeUser(
                            teamId = team.id.toString(),
                            user = User(
                                id = it.player.id,
                                currentWar = war.id.toString(),
                                role = it.player.role,
                                name = it.player.name,
                                discordId = it.player.discordId
                            )
                        ).firstOrNull()
                    }
                    databaseRepository.updateUser(it.player.id, war.id.toString()).firstOrNull()
                }
                war
            }
            .onEach {  dataStoreRepository.setCurrentWar(it) }
            .flatMapLatest { firebaseRepository.writeCurrentWar(it) }
            .onEach { _goToCurrent.emit(Unit) }
            .launchIn(viewModelScope)
    }

}
