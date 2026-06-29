package fr.harmoniamk.statsmkworld.screen.addWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = AddWarViewModel.Factory::class)
class AddWarViewModel @AssistedInject constructor(
    @Assisted val is24p: Boolean,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(is24p: Boolean): AddWarViewModel
    }

    data class State(
        val teamList: List<TeamEntity> = listOf(),
        val playerList: Map<String, List<PlayerSelector>> = mapOf(),
        val teamSelected: List<TeamEntity>? = null,
        val buttonEnabled: Boolean = false,
        val nextButtonEnabled: Boolean = false,
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
        val selectedTeams = state.value.teamSelected.orEmpty().toMutableList().apply { add(team) }
        val buttonEnabled = when (is24p) {
            true -> selectedTeams.size == 3
            else -> selectedTeams.size == 1
        }
        _state.value = state.value.copy(
            teamList = when (buttonEnabled) {
                false -> teams.filterNot { selectedTeams.contains(it) }
                else -> listOf()
            },
            teamSelected = selectedTeams,
            nextButtonEnabled = buttonEnabled,
            warName = "${currentTeam?.tag} - ${selectedTeams.joinToString(" - ") { it.tag }}"
        )
    }

    fun onRemoveTeam() {
        val selectedTeams = state.value.teamSelected.orEmpty().toMutableList().apply { removeAt(lastIndex) }
        val buttonEnabled = when (is24p) {
            true -> selectedTeams.size == 3
            else -> selectedTeams.size == 1
        }
        _state.value = state.value.copy(
            teamList = teams.filterNot { selectedTeams.contains(it) },
            teamSelected = selectedTeams,
            nextButtonEnabled = buttonEnabled,
            warName = "${currentTeam?.tag} - ${selectedTeams.joinToString(" - ") { it.tag }}"
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
        viewModelScope.launch {
            val roster = dataStoreRepository.mkcPlayer.firstOrNull()
                ?.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() ?: return@launch
            rosterId = roster
            val teams = listOf(roster) + state.value.teamSelected?.map { it.id }.orEmpty()
            val war = War(
                id = System.currentTimeMillis(),
                teamHost = roster,
                teamOpponent = state.value.teamSelected?.map { it.id }.orEmpty(),
                tracks = listOf(),
                penalties = listOf(),
                scores = teams.map { WarScore(teamId = it, score = 0) }
            )
            val team = dataStoreRepository.mkcTeam.firstOrNull() ?: return@launch
            _state.value.playerList.flatMap { it.value }.filter { it.isSelected }.forEach {
                when (it.player.rosterId) {
                    "-1" -> firebaseRepository.updateAllyCurrentWar(
                        teamId = team.id.toString(),
                        user = User(
                            id = it.player.id,
                            currentWar = war.id.toString(),
                            role = it.player.role,
                            name = it.player.name,
                            discordId = it.player.discordId
                        )
                    )
                    else -> firebaseRepository.updateUserCurrentWar(
                        teamId = team.id.toString(),
                        user = User(
                            id = it.player.id,
                            currentWar = war.id.toString(),
                            role = it.player.role,
                            name = it.player.name,
                            discordId = it.player.discordId
                        )
                    )
                }
                databaseRepository.updateUser(it.player.id, war.id.toString())
            }
            dataStoreRepository.setCurrentWar(war)
            firebaseRepository.writeCurrentWar(war)
            _goToCurrent.emit(Unit)
        }
    }

}
