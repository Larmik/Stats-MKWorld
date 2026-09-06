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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    @Assisted initialIs24p: Boolean,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(is24p: Boolean): AddWarViewModel
    }

    // Mode 12/24 : état interne réactif (semé par la nav) ; [onModeChange] le bascule
    // sans re-navigation (rule 11).
    private var is24p: Boolean = initialIs24p

    /** Preview d'un adversaire au récap : nom/tag du roster, avatar de l'équipe parente (rule 12). */
    data class OpponentPreview(
        val name: String,
        val tag: String,
        val logo: String?,
        val color: Int?
    )

    data class State(
        // Étape courante (0 = Adversaire, 1 = Joueurs, 2 = Récap), pilotée dans le VM.
        val step: Int = 0,
        // Mode courant : pilote le nombre d'adversaires (1 en 12p, 3 en 24p).
        val is24p: Boolean = false,
        val teamList: List<TeamEntity> = listOf(),
        val playerList: Map<String, List<PlayerSelector>> = mapOf(),
        val teamSelected: List<TeamEntity>? = null,
        // Rosters adverses retenus, alignés sur teamSelected (null = équipe sans roster
        // mkworld). Portent le nom/tag du roster ; l'avatar reste celui de l'équipe.
        val rostersSelected: List<MKCTeamRoster?> = listOf(),
        val opponentPreviews: List<OpponentPreview> = listOf(),
        val buttonEnabled: Boolean = false,
        val nextButtonEnabled: Boolean = false,
        val warName: String? = null,
        // id de l'équipe dont le sélecteur de roster multi-rosters est déplié inline (null = aucun).
        val expandedRosterTeamId: String? = null,
        val expandedRosters: List<MKCTeamRoster> = listOf(),
        // Photos MKCentral des joueurs (playerId → url préfixée), résolues une fois en
        // parallèle ; initiales en fallback tant qu'absentes (rule 12).
        val playerAvatars: Map<String, String> = emptyMap()
    ) {
        /** Nombre d'adversaires attendus selon le mode (1 en 12p, 3 en 24p). */
        val opponentCount: Int get() = if (is24p) 3 else 1

        /** Nombre de joueurs actuellement sélectionnés (sur les 6 attendus). */
        val selectedPlayerCount: Int get() = playerList.values.flatten().count { it.isSelected }

        /** Joueurs actuellement sélectionnés (pour le récap de l'étape 3). */
        val selectedPlayers: List<PlayerEntity>
            get() = playerList.values.flatten().filter { it.isSelected }.map { it.player }
    }

    private val _state = MutableStateFlow(State(is24p = initialIs24p))
    private var teams = listOf<TeamEntity>()
    private var players = listOf<PlayerEntity>()
    private var currentTeam: MKCTeam? = null
    private var rosterId: String? = null

    // Photos MKCentral résolues (playerId → url), portée hors du `zip` pour survivre à ses
    // ré-émissions. `@Volatile` : écrite depuis une coroutine, lue dans le mapping du flow.
    @Volatile
    private var playerAvatars: Map<String, String> = emptyMap()
    // Garde-fou : résolution des avatars lancée une seule fois.
    private var avatarsRequested = false

    private val _goToCurrent = MutableSharedFlow<Unit>()
    val goToCurrent = _goToCurrent.asSharedFlow()

    val state = databaseRepository.getTeams()
        .zip(databaseRepository.getPlayers()) { teams, players ->
            val team = dataStoreRepository.mkcTeam.firstOrNull()
            this.teams = teams
            this.players = players
            this.currentTeam = team
            // Résout (une fois) les photos de profil des joueurs, en parallèle.
            resolvePlayerAvatars(players)
            State(
                is24p = is24p,
                teamList = teams,
                playerList = players.map { PlayerSelector(it, false) }.groupBy { selector ->
                    val roster = team?.rosters?.firstOrNull { it.id.toString() == selector.player.rosterId }
                    roster?.name.orEmpty()
                },
                playerAvatars = playerAvatars
            )
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /**
     * Résout une fois, en parallèle (rule 30), les photos MKCentral des joueurs et pousse la
     * `Map<playerId, url>` dans `_state` (les cellules passent des initiales à la photo).
     */
    private fun resolvePlayerAvatars(players: List<PlayerEntity>) {
        if (avatarsRequested || players.isEmpty()) return
        avatarsRequested = true
        viewModelScope.launch {
            val resolved = coroutineScope {
                players.map { player ->
                    async {
                        val avatar = mkCentralDataSource.getPlayer(player.id).successResponse
                            ?.userSettings?.avatar?.takeIf { it.isNotEmpty() }
                            ?.let { "https://mkcentral.com$it" }
                        player.id to avatar
                    }
                }.awaitAll()
            }.mapNotNull { (id, url) -> url?.let { id to it } }.toMap()
            if (resolved.isNotEmpty()) {
                playerAvatars = resolved
                _state.value = state.value.copy(playerAvatars = resolved)
            }
        }
    }

    /**
     * Bascule 12↔24 sans re-navigation (rule 11) : le nombre d'adversaires change (1 vs 3),
     * d'où la remise à zéro complète de la sélection (retour à l'étape 1).
     */
    fun onModeChange(is24p: Boolean) {
        if (is24p == this.is24p) return
        this.is24p = is24p
        resetOpponentSelection()
    }

    /**
     * Navigation entre étapes (rule 11) : un retour en arrière annule la sélection de l'étape
     * rejointe (Adversaire = reset complet, Joueurs = line-up) ; aller en avant ne réinitialise rien.
     */
    fun onStepChange(step: Int) {
        val current = state.value.step
        when {
            step >= current -> _state.value = state.value.copy(step = step)
            step == 0 -> resetOpponentSelection()
            else -> resetPlayerSelection()
        }
    }

    /**
     * Retour à l'étape Adversaire = remise à zéro complète (adversaires ET line-up). Mutualisé
     * entre [onModeChange] et [onStepChange] (≥ 2 appelants, rules 30/61).
     */
    private fun resetOpponentSelection() {
        _state.value = state.value.copy(
            step = 0,
            is24p = is24p,
            teamList = teams,
            teamSelected = null,
            rostersSelected = listOf(),
            opponentPreviews = listOf(),
            nextButtonEnabled = false,
            warName = null,
            expandedRosterTeamId = null,
            expandedRosters = listOf(),
            // Repart d'une line-up vierge (retour à la première étape = restart complet).
            playerList = state.value.playerList.mapValues { (_, list) ->
                list.map { it.copy(isSelected = false) }
            },
            buttonEnabled = false
        )
    }

    /** Remet la line-up à zéro (retour arrière vers l'étape Joueurs). */
    private fun resetPlayerSelection() {
        _state.value = state.value.copy(
            step = 1,
            playerList = state.value.playerList.mapValues { (_, list) ->
                list.map { it.copy(isSelected = false) }
            },
            buttonEnabled = false
        )
    }

    /** Replie le sélecteur de roster inline éventuellement déplié (étape 1). */
    fun collapseRosterPicker() {
        _state.value = state.value.copy(expandedRosterTeamId = null, expandedRosters = listOf())
    }

    fun onSearchTeam(search: String) {
        val query = search.lowercase()
        _state.value = state.value.copy(teamList = teams.filter { team ->
            team.tag.lowercase().contains(query)
                    || team.name.lowercase().contains(query)
                    || team.rosters.any {
                it.name.lowercase().contains(query) || it.tag.lowercase().contains(query)
            }
        }.sortedBy { it.name })
    }

    fun onTeamSelected(team: TeamEntity) {
        viewModelScope.launch {
            val rosters = mkCentralDataSource.getTeam(team.id).successResponse
                ?.rosters?.filter { it.game == "mkworld" }
                .orEmpty()
            when {
                // Plusieurs rosters mkworld : déplie le sélecteur inline (2ᵉ clic → replie).
                rosters.size > 1 -> _state.value = state.value.copy(
                    expandedRosterTeamId = team.id.takeIf { it != state.value.expandedRosterTeamId },
                    expandedRosters = rosters
                )
                // Un seul roster (ou aucun → fallback teamId) : retenu directement.
                else -> commitTeam(team, rosters.firstOrNull())
            }
        }
    }

    /** Valide le roster [roster] choisi dans le sélecteur inline d'une équipe multi-rosters. */
    fun onRosterSelected(team: TeamEntity, roster: MKCTeamRoster) {
        commitTeam(team, roster)
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
        val current = state.value
        // Garde d'unicité (#91 pt.9) : pas d'ajout en double (double-clic). État courant =
        // source de vérité unique → teamSelected/rostersSelected restent alignés.
        if (current.teamSelected.orEmpty().any { it.id == team.id }) return
        val selectedTeams = current.teamSelected.orEmpty() + team
        val selectedRosterMetas = current.rostersSelected + roster
        // Preview de l'adversaire : nom/tag du roster (rule 12), avatar de l'équipe.
        val previews = current.opponentPreviews + OpponentPreview(
            name = roster?.name ?: team.name,
            tag = roster?.tag ?: team.tag,
            logo = team.logo,
            color = roster?.color?.toInt() ?: team.color
        )
        val allOpponentsPicked = selectedTeams.size == current.opponentCount
        _state.value = current.copy(
            // À l'issue de la sélection complète, on bascule sur l'étape Joueurs.
            step = if (allOpponentsPicked) 1 else 0,
            teamList = when (allOpponentsPicked) {
                false -> teams.filterNot { selectedTeams.contains(it) }
                else -> listOf()
            },
            teamSelected = selectedTeams,
            rostersSelected = selectedRosterMetas,
            opponentPreviews = previews,
            nextButtonEnabled = allOpponentsPicked,
            warName = warName(selectedRosterMetas, selectedTeams),
            expandedRosterTeamId = null,
            expandedRosters = listOf()
        )
    }

    fun onRemoveTeam() {
        val selectedTeams = state.value.teamSelected.orEmpty().toMutableList().apply { if (isNotEmpty()) removeAt(lastIndex) }
        val selectedRosterMetas = state.value.rostersSelected.toMutableList().apply { if (isNotEmpty()) removeAt(lastIndex) }
        val previews = state.value.opponentPreviews.toMutableList().apply { if (isNotEmpty()) removeAt(lastIndex) }
        _state.value = state.value.copy(
            teamList = teams.filterNot { selectedTeams.contains(it) },
            teamSelected = selectedTeams,
            rostersSelected = selectedRosterMetas,
            opponentPreviews = previews,
            nextButtonEnabled = selectedTeams.size == state.value.opponentCount,
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
        // 6 joueurs → bascule auto sur le Récap ; en retirer un ramène à l'étape Joueurs.
        val complete = newValues.flatMap { it.value }.count { it.isSelected } == 6
        _state.value = state.value.copy(
            step = if (complete) 2 else 1,
            playerList = newValues,
            buttonEnabled = complete
        )
    }

    fun createWar() {
        viewModelScope.launch {
            val roster = dataStoreRepository.mkcPlayer.firstOrNull()
                ?.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() ?: return@launch
            rosterId = roster
            // Ids d'opposant dérivés de l'état (#91 pt.9) : rosterId mkworld retenu, sinon
            // fallback teamId, alignés index par index sur teamSelected.
            val currentState = state.value
            val selectedTeams = currentState.teamSelected.orEmpty()
            val opponents = currentState.rostersSelected.mapIndexed { index, rosterMeta ->
                rosterMeta?.id?.toString() ?: selectedTeams.getOrNull(index)?.id.orEmpty()
            }
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
