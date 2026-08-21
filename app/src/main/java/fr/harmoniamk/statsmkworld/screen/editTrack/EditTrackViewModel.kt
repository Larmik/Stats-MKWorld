package fr.harmoniamk.statsmkworld.screen.editTrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.application.MainApplication
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.Shock
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = EditTrackViewModel.Factory::class)
class EditTrackViewModel @AssistedInject constructor(
    @Assisted val details: WarTrackDetails?,
    @Assisted val is24p: Boolean,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(details: WarTrackDetails?, is24p: Boolean): EditTrackViewModel
    }

    data class State(
        val mapList: List<Maps> = Maps.entries,
        val mapSelected: List<Maps>? = null,
        val players: List<PlayerEntity> = listOf(),
        val currentPlayer: PlayerEntity? = null,
        val selectedPositions: List<PlayerPosition> = listOf(),
        val initialPositions: List<PlayerPosition> = listOf(),
        val buttonEnabled: Boolean = false,
        val shocks: Map<String, Int> = mutableMapOf(),
        val is24p: Boolean = false
    )

    private val positions = mutableListOf<PlayerPosition>()
    private val _state = MutableStateFlow(State())

    private val _backToCurrent = MutableSharedFlow<Unit>()
    val backToCurrent = _backToCurrent.asSharedFlow()

    val state = dataStoreRepository.war
        .filterNotNull()
        .map { war ->
            val players = databaseRepository.getPlayers().firstOrNull()
                ?.filter { it.currentWar == war.id.toString() }?.sortedBy { it.name }.orEmpty()

            val positions = players.map { player ->
                details?.track?.positions.orEmpty().singleOrNull { it.playerId == player.id }?.let {
                    PlayerPosition(
                        player = player,
                        position = it
                    )
                }
            }
            State(
                players = players,
                currentPlayer = players.firstOrNull(),
                initialPositions = positions.filterNotNull().sortedBy { it.position.position },
                // Pré-remplir le circuit courant (liseré dans l'onglet Circuit) et les shocks
                // existants (affichés dans l'onglet Shocks), pour que l'édition parte de l'état réel.
                mapSelected = details?.track?.index.orEmpty().mapNotNull { it.toIntOrNull()?.let(Maps.entries::getOrNull) },
                shocks = details?.track?.shocks.orEmpty().associate { it.playerId to it.count },
                is24p = is24p
            )
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /** Filtre la grille de circuits par nom/label (onglet Circuit), comme AddTrack. */
    fun onSearch(searched: String) {
        _state.value = state.value.copy(mapList = Maps.entries.filter {
            it.name.lowercase().contains(searched.lowercase()) ||
                    MainApplication.instance?.applicationContext?.getString(it.label)
                        ?.lowercase()?.contains(searched.lowercase()) != false
        })
    }

    fun onMapSelected(map: List<Maps>) {
        _state.value = state.value.copy(
            mapSelected = map,
            buttonEnabled = positions.isEmpty() || positions.size == 6
        )
    }

    fun onPositionClick(position: Int) {
        val currentPlayer = state.value.currentPlayer ?: return
        val originalPosition = details?.track?.positions?.firstOrNull { it.playerId == currentPlayer.id }
        val newPos = PlayerPosition(
            player = currentPlayer,
            position = originalPosition?.copy(position = position)
                ?: WarPosition(
                    id = System.currentTimeMillis(),
                    playerId = currentPlayer.id,
                    position = position
                )
        )
        positions.add(newPos)
        _state.value = state.value.copy(
            selectedPositions = positions.sortedBy { it.position.position },
            currentPlayer = state.value.players.getOrNull(positions.size),
            buttonEnabled = state.value.mapSelected != null && (positions.isEmpty() || positions.size == 6)
                    || state.value.mapSelected == null && positions.size == 6
        )
    }

    fun onValidate() {
        viewModelScope.launch {
            dataStoreRepository.war.firstOrNull()?.let { war ->
                val tracks = war.tracks.toMutableList()
                details?.track?.let { track ->
                    war.tracks.map { it.id }.indexOf(track.id).takeIf { it != -1 }?.let { index ->
                        val shocks = state.value.shocks.map { Shock(it.key, it.value) }
                        val trackWithShock = when (shocks.isEmpty()) {
                            true -> track
                            else -> track.copy(shocks = shocks)
                        }
                        when {
                            _state.value.mapSelected != null && _state.value.selectedPositions.isEmpty() -> {
                                tracks.add(
                                    index, trackWithShock.copy(
                                        index = _state.value.mapSelected?.map { it.ordinal.toString() } ?: track.index,
                                    )
                                )
                                tracks.removeAt(index+1)
                                updateWar(war, tracks)
                            }
                            _state.value.mapSelected != null && _state.value.selectedPositions.size == 6 -> {
                                tracks.add(
                                    index, trackWithShock.copy(
                                        index = _state.value.mapSelected?.map { it.ordinal.toString() } ?: track.index,
                                        positions = _state.value.selectedPositions.map { it.position }
                                    )
                                )
                                tracks.removeAt(index+1)
                                updateWar(war, tracks)
                            }
                            _state.value.mapSelected == null && _state.value.selectedPositions.size == 6 -> {
                                tracks.add(
                                    index, trackWithShock.copy(
                                        positions = _state.value.selectedPositions.map { it.position }
                                    )
                                )
                                tracks.removeAt(index+1)
                                updateWar(war, tracks)
                            }
                            _state.value.shocks.isNotEmpty() -> {
                                tracks.add(index, trackWithShock)
                                tracks.removeAt(index+1)
                                updateWar(war, tracks)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onAddShock(id: String) {
        val shocks = state.value.shocks.toMutableMap()
        shocks[id]?.let { shocks[id] = it + 1 } ?: run { shocks[id] = 1 }
        _state.value = state.value.copy(
            shocks = shocks,
            buttonEnabled = true
        )
    }

    fun onRemoveShock(id: String) {
        val shocks = state.value.shocks.toMutableMap()
        shocks[id]?.let { shocks[id] = it - 1 }
        _state.value = state.value.copy(
            shocks = shocks,
            buttonEnabled = true
        )
    }

    /**
     * Réécrit la war en cours avec les [tracks] édités et **recalcule le score hôte**
     * (justesse prioritaire, rule 13). Le score de war hôte est la somme des points
     * (barème `positionToPoints`) de **toutes** les positions de **toutes** les manches :
     * il reflète donc immédiatement l'édition d'un circuit (barème 12p/24p), d'une position
     * ou d'un shock (les shocks restent hors score).
     *
     * - **12p** : seul le score hôte est stocké ; le score adverse est dérivé (complément
     *   au barème) à l'affichage. On ne conserve donc qu'un [WarScore] hôte.
     * - **24p** : les scores adverses sont saisis ailleurs et stockés explicitement dans
     *   `war.scores` — on les **préserve** (on ne remplace que l'entrée hôte).
     *
     * Les **pénalités** (`war.penalties`) sont conservées telles quelles (champ distinct,
     * inchangé par `war.copy`).
     */
    private fun updateWar(war: War, tracks: List<WarTrack>) {
        val is24p = war.teamOpponent.size > 1
        val hostScore = WarScore(
            teamId = war.teamHost,
            score = tracks.flatMap { it.positions }.sumOf { it.position.positionToPoints(is24p) }
        )
        // 12p : score hôte seul (adverse dérivé). 24p : préserver les scores adverses saisis.
        val scores = when (is24p) {
            true -> listOf(hostScore) + war.scores.filter { it.teamId != war.teamHost }
            else -> listOf(hostScore)
        }
        val warToUpdate = war.copy(tracks = tracks, scores = scores)
        _state.value = State()
        viewModelScope.launch {
            firebaseRepository.writeCurrentWar(warToUpdate)
            dataStoreRepository.setCurrentWar(warToUpdate)
            _backToCurrent.emit(Unit)
        }
    }
}