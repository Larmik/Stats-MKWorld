package fr.harmoniamk.statsmkworld.screen.addWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster
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
    private val mkCentralDataSource: MKCentralDataSourceInterface,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(is24p: Boolean): AddWarViewModel
    }

    /**
     * État de l'étape intermédiaire de choix du roster adverse.
     *
     * N'est renseigné que lorsque l'équipe sélectionnée possède plusieurs
     * rosters mkworld : le bottomSheet s'ouvre alors pour laisser choisir
     * lequel affronter. [selectedRoster] porte la preview de la sélection.
     */
    data class RosterSelection(
        val team: TeamEntity,
        val rosters: List<MKCTeamRoster>,
        val selectedRoster: MKCTeamRoster? = null
    )

    data class State(
        val teamList: List<TeamEntity> = listOf(),
        val playerList: Map<String, List<PlayerSelector>> = mapOf(),
        val teamSelected: List<TeamEntity>? = null,
        // Rosters adverses retenus, alignés sur teamSelected (null = équipe sans
        // roster mkworld). Portent le nom/tag du roster pour l'affichage (preview
        // de l'adversaire), l'avatar restant celui de l'équipe (teamSelected).
        val rostersSelected: List<MKCTeamRoster?> = listOf(),
        val buttonEnabled: Boolean = false,
        val nextButtonEnabled: Boolean = false,
        val warName: String? = null,
        val rosterSelection: RosterSelection? = null
    ) {
        /**
         * Équipe à afficher dans l'emplacement adverse [index] : avatar de l'équipe
         * mais **nom du roster** sélectionné (principe « afficher le roster »).
         */
        fun opponentSlot(index: Int): TeamEntity? {
            val team = teamSelected?.getOrNull(index) ?: return null
            val roster = rostersSelected.getOrNull(index)
            return team.copy(name = roster?.name ?: team.name, tag = roster?.tag ?: team.tag)
        }
    }

    private val _state = MutableStateFlow(State())
    private var teams = listOf<TeamEntity>()
    private var players = listOf<PlayerEntity>()
    private var currentTeam: MKCTeam? = null
    private var rosterId: String? = null

    // rosterId adverse retenu pour chaque équipe sélectionnée (index aligné sur teamSelected).
    private var selectedRosterIds = listOf<String>()

    private val _goToCurrent = MutableSharedFlow<Unit>()
    val goToCurrent = _goToCurrent.asSharedFlow()

    private val _openRosterSheet = MutableSharedFlow<Unit>()
    val openRosterSheet = _openRosterSheet.asSharedFlow()

    private val _dismissRosterSheet = MutableSharedFlow<Unit>()
    val dismissRosterSheet = _dismissRosterSheet.asSharedFlow()

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
        viewModelScope.launch {
            val rosters = mkCentralDataSource.getTeam(team.id).successResponse
                ?.rosters?.filter { it.game == "mkworld" }
                .orEmpty()
            when {
                // Plusieurs rosters mkworld : étape intermédiaire de sélection.
                rosters.size > 1 -> {
                    _state.value = state.value.copy(
                        rosterSelection = RosterSelection(team = team, rosters = rosters)
                    )
                    _openRosterSheet.emit(Unit)
                }
                // Un seul roster mkworld : on retient directement son rosterId.
                // Fallback sur le teamId si l'équipe n'expose aucun roster mkworld.
                else -> commitTeam(team, rosters.firstOrNull())
            }
        }
    }

    /** Preview d'un roster dans le bottomSheet, sans valider la sélection. */
    fun onRosterSelected(roster: MKCTeamRoster) {
        val selection = state.value.rosterSelection ?: return
        _state.value = state.value.copy(
            rosterSelection = selection.copy(selectedRoster = roster)
        )
    }

    /** Valide le roster choisi dans le bottomSheet et ferme celui-ci. */
    fun onRosterValidated() {
        val selection = state.value.rosterSelection ?: return
        val roster = selection.selectedRoster ?: return
        commitTeam(selection.team, roster)
        _state.value = state.value.copy(rosterSelection = null)
        viewModelScope.launch { _dismissRosterSheet.emit(Unit) }
    }

    /** Tag du roster hôte mkworld (à défaut, tag de l'équipe) pour le nom de war. */
    private fun hostTag(): String? =
        currentTeam?.rosters?.firstOrNull { it.game == "mkworld" }?.tag ?: currentTeam?.tag

    // Nom de war : tags des rosters (adversaires) — cf. principe « afficher le roster ».
    private fun warName(rosters: List<MKCTeamRoster?>, fallbackTeams: List<TeamEntity>): String {
        val opponents = rosters.mapIndexed { index, roster ->
            roster?.tag ?: fallbackTeams.getOrNull(index)?.tag.orEmpty()
        }
        return "${hostTag()} - ${opponents.joinToString(" - ")}"
    }

    private fun commitTeam(team: TeamEntity, roster: MKCTeamRoster?) {
        val selectedTeams = state.value.teamSelected.orEmpty().toMutableList().apply { add(team) }
        val selectedRosterMetas = state.value.rostersSelected.toMutableList().apply { add(roster) }
        // rosterId retenu = id du roster mkworld, sinon fallback sur le teamId.
        selectedRosterIds = selectedRosterIds.toMutableList().apply { add(roster?.id?.toString() ?: team.id) }
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
            rostersSelected = selectedRosterMetas,
            nextButtonEnabled = buttonEnabled,
            warName = warName(selectedRosterMetas, selectedTeams)
        )
    }

    fun onRemoveTeam() {
        val selectedTeams = state.value.teamSelected.orEmpty().toMutableList().apply { removeAt(lastIndex) }
        val selectedRosterMetas = state.value.rostersSelected.toMutableList().apply { if (isNotEmpty()) removeAt(lastIndex) }
        selectedRosterIds = selectedRosterIds.toMutableList().apply { if (isNotEmpty()) removeAt(lastIndex) }
        val buttonEnabled = when (is24p) {
            true -> selectedTeams.size == 3
            else -> selectedTeams.size == 1
        }
        _state.value = state.value.copy(
            teamList = teams.filterNot { selectedTeams.contains(it) },
            teamSelected = selectedTeams,
            rostersSelected = selectedRosterMetas,
            nextButtonEnabled = buttonEnabled,
            warName = warName(selectedRosterMetas, selectedTeams)
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
            val opponents = selectedRosterIds
            val teams = listOf(roster) + opponents
            val war = War(
                id = System.currentTimeMillis(),
                teamHost = roster,
                teamOpponent = opponents,
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
