package fr.harmoniamk.statsmkworld.screen.addTrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.application.MainApplication
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.model.ScoringConstants
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.opponentTeams
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.Shock
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.WarDetails
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = AddTrackViewModel.Factory::class)
class AddTrackViewModel @AssistedInject constructor(
    @Assisted val is24p: Boolean,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(is24p: Boolean): AddTrackViewModel
    }

    data class State(
        // Étape courante (pilotée dans le VM, rule 11) : 3 étapes en 12p, 4 en 24p (Intermission
        // intercalée). Indexer via [stepCircuit]/[stepIntermission]/[stepPositions]/[stepSummary].
        val step: Int = 0,
        // Mode courant (déterminé par le nombre d'adversaires). Pilote l'indexation des étapes.
        val is24p: Boolean = false,
        val mapList: List<Maps> = Maps.entries,
        val mapSelected: Maps? = null,
        val intermissionList: List<Maps>? = null,
        val intermissionSelected: Maps? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: List<TeamEntity>? = null,
        val players: List<PlayerEntity> = listOf(),
        val currentPlayer: PlayerEntity? = null,
        val selectedPositions: List<PlayerPosition> = listOf(),
        val teamHostWarScore: Int? = null,
        val teamHostTrackScore: Int? = null,
        val shocks: Map<String, Int> = mutableMapOf(),
        val rosterName: String? = null,
        val rosterId: String? = null,
        val totalPositions: Int? = null,
        val scores: List<WarScore>? = null,
        //12 players
        val teamOpponentScore: Int? = null,
        val trackScore: String? = null,
        val trackDiff: String? = null,
    ) {
        /** Le circuit est choisi → l'étape Intermission/Positions devient accessible. */
        val mapPicked: Boolean get() = mapSelected != null

        /** Line-up complète (tous les joueurs ont une position) → le Résumé est accessible. */
        val positionsComplete: Boolean get() = players.isNotEmpty() && selectedPositions.size == players.size

        // Index sémantiques des étapes. Intermission en 24p seulement (-1 en 12p, jamais atteint).
        val stepCircuit: Int get() = 0
        val stepIntermission: Int get() = if (is24p) 1 else -1
        val stepPositions: Int get() = if (is24p) 2 else 1
        val stepSummary: Int get() = if (is24p) 3 else 2
    }

    private val _state = MutableStateFlow(State())

    private val _backToWar = MutableSharedFlow<Unit>()

    private val positions = mutableListOf<PlayerPosition>()
    val backToWar = _backToWar.asSharedFlow()
    private var details: WarDetails? = null

    val state = dataStoreRepository.war
        .filterNotNull()
        .map { WarDetails(it) }
        .zip(dataStoreRepository.mkcTeam) { details, teamHost  ->
            this.details = details
            val teamOpponents = details.war.opponentTeams(databaseRepository)
            val hostRoster = teamHost.rosters.singleOrNull { it.id.toString() == details.war.teamHost }
            val rosterName = hostRoster?.name ?: teamHost.name
            val rosterId = hostRoster?.id ?: teamHost.id
            val players = databaseRepository.getPlayers().firstOrNull()
                ?.filter { it.currentWar == details.war.id.toString() }?.sortedBy { it.name }.orEmpty()
            _state.value.copy(
                // Mode dérivé du nombre d'adversaires (source de vérité, aligné sur totalPositions).
                is24p = teamOpponents.size > 1,
                // Avatar de l'équipe, nom/tag du roster hôte.
                teamHost = TeamEntity(teamHost).copy(name = rosterName, tag = hostRoster?.tag ?: teamHost.tag),
                teamOpponent = teamOpponents,
                players = players,
                teamHostWarScore = details.war.scores.firstOrNull { it.teamId == rosterId.toString() }?.score,
                currentPlayer = players.firstOrNull(),
                rosterName = rosterName,
                rosterId = rosterId.toString(),
                totalPositions = when (teamOpponents.size > 1) {
                    true -> 24
                    else -> 12
                },
                scores = details.war.scores
            )
        }
        .onEach { _state.value = it }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onSearch(searched: String) {
        _state.value = _state.value.copy(mapList = Maps.entries.filter {
            it.name.lowercase()
                .contains(searched.lowercase()) || MainApplication.instance?.applicationContext?.getString(
                it.label
            )?.lowercase()?.contains(searched.lowercase()) != false
        })
    }

    /**
     * Choix du circuit : réinitialise la line-up (circuit changé → saisie vierge) et avance à
     * l'étape suivante (Intermission en 24p, Positions en 12p).
     */
    fun onMapSelected(map: Maps) {
        positions.clear()
        _state.value = state.value.copy(
            // 24p → Intermission ; 12p → Positions (l'Intermission n'existe pas en 12p).
            step = if (state.value.is24p) state.value.stepIntermission else state.value.stepPositions,
            mapSelected = map,
            intermissionList = Maps.intermissionsTo(map),
            intermissionSelected = null,
            selectedPositions = listOf(),
            currentPlayer = state.value.players.firstOrNull(),
            trackScore = null,
            trackDiff = null,
            teamHostTrackScore = null,
            teamOpponentScore = null
        )
    }

    /** Choix (optionnel) de l'intermission — un 2ᵉ circuit enchaîné (24p uniquement). `null` = aucune. */
    fun onIntermissionSelected(map: Maps?) {
        val mapSelected = state.value.mapSelected
        _state.value = state.value.copy(intermissionSelected = map?.takeIf { it != mapSelected })
    }

    /**
     * Navigation entre étapes (rule 11) : un retour en arrière annule la sélection de l'étape
     * rejointe (Circuit = reset complet, Intermission = 2ᵉ circuit + positions, Positions =
     * line-up) ; aller en avant (ou rester) ne réinitialise rien.
     */
    fun onStepChange(step: Int) {
        val current = state.value.step
        when {
            step >= current -> _state.value = state.value.copy(step = step)
            step == state.value.stepCircuit -> resetTrack()
            step == state.value.stepIntermission -> resetIntermission()
            else -> resetPositions()
        }
    }

    /**
     * Retour à l'Intermission (24p) : réinitialise le 2ᵉ circuit ET les positions qui en
     * dépendent. Le circuit principal est conservé.
     */
    private fun resetIntermission() {
        positions.clear()
        _state.value = state.value.copy(
            step = state.value.stepIntermission,
            intermissionSelected = null,
            selectedPositions = listOf(),
            currentPlayer = state.value.players.firstOrNull(),
            shocks = mapOf(),
            trackScore = null,
            trackDiff = null,
            teamHostTrackScore = null,
            teamOpponentScore = null
        )
    }

    /** Remise à zéro complète (retour à la 1ʳᵉ étape Circuit) : on repart d'un choix vierge. */
    private fun resetTrack() {
        positions.clear()
        _state.value = state.value.copy(
            step = state.value.stepCircuit,
            mapList = Maps.entries,
            mapSelected = null,
            intermissionList = null,
            intermissionSelected = null,
            selectedPositions = listOf(),
            currentPlayer = state.value.players.firstOrNull(),
            shocks = mapOf(),
            trackScore = null,
            trackDiff = null,
            teamHostTrackScore = null,
            teamOpponentScore = null
        )
    }

    /** Vide la line-up de positions (retour arrière vers l'étape Positions). */
    private fun resetPositions() {
        positions.clear()
        _state.value = state.value.copy(
            step = state.value.stepPositions,
            selectedPositions = listOf(),
            currentPlayer = state.value.players.firstOrNull(),
            shocks = mapOf(),
            trackScore = null,
            trackDiff = null,
            teamHostTrackScore = null,
            teamOpponentScore = null
        )
    }

    fun onPositionClick(position: Int) {
        val pos = PlayerPosition(
            player = _state.value.currentPlayer,
            position = WarPosition(
                id = System.currentTimeMillis(),
                position = position,
                playerId = _state.value.currentPlayer?.id.orEmpty()
            )

        )
        positions.add(pos)
        _state.value = _state.value.copy(selectedPositions = positions.sortedBy { it.position.position })
        when {
            // Line-up complète : calcul du score de manche et passage au Résumé.
            positions.size == _state.value.players.size -> {
                // Score hôte = somme des points (positionToPoints) ; adverse = complément au
                // max de manche. Barème existant réutilisé (justesse, rule 13).
                val scoreHost = _state.value.selectedPositions.map { it.position }.sumOf { it.position.positionToPoints(is24p) }
                val maxPointsPerTrack = when (is24p) {
                    true -> ScoringConstants.MAX_POINTS_PER_TRACK_24P
                    else -> ScoringConstants.MAX_POINTS_PER_TRACK_12P
                }
                val scoreOpponent = maxPointsPerTrack - scoreHost
                _state.value = _state.value.copy(
                    step = _state.value.stepSummary,
                    trackScore = "$scoreHost - $scoreOpponent",
                    teamHostTrackScore = scoreHost,
                    teamOpponentScore = scoreOpponent,
                    trackDiff = when {
                        (scoreHost - scoreOpponent) > 0 -> "+${scoreHost - scoreOpponent}"
                        else -> "${scoreHost - scoreOpponent}"
                    }
                )
            }

            else -> _state.value = _state.value.copy(
                currentPlayer = _state.value.players.getOrNull(
                    positions.indexOf(pos) + 1
                )
            )

        }
    }

    fun onAddShock(id: String) {
        val shocks = state.value.shocks.toMutableMap()
        shocks[id]?.let { shocks[id] = it + 1 } ?: run { shocks[id] = 1 }
        _state.value = state.value.copy(shocks = shocks)
    }


    fun onRemoveShock(id: String) {
        val shocks = state.value.shocks.toMutableMap()
        shocks[id]?.let { shocks[id] = it - 1 }
        _state.value = state.value.copy(shocks = shocks)
    }


    fun onValidate() {
        details?.war?.let {
            val shockList = state.value.shocks.map { Shock(it.key, it.value) }
            val track = WarTrack(
                id = System.currentTimeMillis(),
                index = listOfNotNull(state.value.intermissionSelected, state.value.mapSelected).map { it.ordinal.toString() },
                positions = _state.value.selectedPositions.map { it.position },
                shocks = shockList
            )
            val tracks = mutableListOf<WarTrack>()
            tracks.addAll(it.tracks)
            tracks.add(track)
            // Score de war hôte = score courant + score de manche. Recalculé à la validation
            // (non accumulé) → insensible aux retours arrière (justesse, rule 13).
            val newHostWarScore = (state.value.teamHostWarScore ?: 0) + (state.value.teamHostTrackScore ?: 0)
            val newWar = when (state.value.teamOpponent.orEmpty().size > 1) {
                true -> it.copy(tracks = tracks, scores = listOf(WarScore(teamId = it.teamHost, score = newHostWarScore)))
                else -> it.copy(tracks = tracks, scores = listOf(
                    WarScore(teamId = it.teamHost, score = newHostWarScore),
                    WarScore(teamId = it.teamOpponent.firstOrNull().orEmpty(), score = state.value.teamOpponentScore ?: 0)
                ))
            }
            viewModelScope.launch {
                firebaseRepository.writeCurrentWar(newWar)
                dataStoreRepository.setCurrentWar(newWar)
                _backToWar.emit(Unit)
            }

        }

    }


}