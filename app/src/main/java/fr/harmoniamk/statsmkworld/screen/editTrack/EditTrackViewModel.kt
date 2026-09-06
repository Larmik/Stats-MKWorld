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
        // Line-up éditable : une position par joueur, modifiable via ±, pré-remplie avec les positions actuelles.
        val selectedPositions: List<PlayerPosition> = listOf(),
        val buttonEnabled: Boolean = false,
        val shocks: Map<String, Int> = mutableMapOf(),
        val is24p: Boolean = false
    ) {
        /** Nombre max de positions selon le mode (12p → 12, 24p → 24). */
        val maxPosition: Int get() = if (is24p) 24 else 12

        /** Positions toutes distinctes (aucun doublon) — prérequis pour activer « Confirmer ». */
        val positionsAllDistinct: Boolean
            get() = selectedPositions.map { it.position.position }.let { it.size == it.toSet().size }
    }

    private val _state = MutableStateFlow(State())
    // Vrai dès qu'une édition a eu lieu : « Confirmer » n'a de sens qu'après une modification.
    private var edited = false

    private val _backToCurrent = MutableSharedFlow<Unit>()
    val backToCurrent = _backToCurrent.asSharedFlow()

    val state = dataStoreRepository.war
        .filterNotNull()
        .map { war ->
            val players = databaseRepository.getPlayers().firstOrNull()
                ?.filter { it.currentWar == war.id.toString() }?.sortedBy { it.name }.orEmpty()

            // Line-up triée par position de départ UNE SEULE FOIS ; ensuite l'ordre est figé
            // (`onPositionChange` ne re-trie pas → les cellules ne bougent pas pendant l'édition).
            val positions = players.mapNotNull { player ->
                details?.track?.positions.orEmpty().singleOrNull { it.playerId == player.id }?.let {
                    PlayerPosition(player = player, position = it)
                }
            }.sortedBy { it.position.position }
            State(
                players = players,
                selectedPositions = positions,
                // Pré-remplir circuit courant et shocks existants, pour partir de l'état réel.
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
        edited = true
        _state.value = state.value.copy(mapSelected = map, buttonEnabled = state.value.positionsAllDistinct)
    }

    /**
     * Édite la position d'un joueur par pas de [delta], bornée à 1..[State.maxPosition].
     * « Confirmer » se réactive si les positions restent distinctes (score recalculé via [updateWar]).
     */
    fun onPositionChange(playerId: String, delta: Int) {
        val updated = state.value.selectedPositions.map { playerPosition ->
            when (playerPosition.player?.id == playerId) {
                true -> {
                    val newPosition = (playerPosition.position.position + delta)
                        .coerceIn(1, state.value.maxPosition)
                    playerPosition.copy(position = playerPosition.position.copy(position = newPosition))
                }
                else -> playerPosition
            }
        }
        // Aucun re-tri : l'ordre d'affichage des cellules reste stable pendant l'édition.
        edited = true
        _state.value = state.value.copy(
            selectedPositions = updated,
            buttonEnabled = updated.map { it.position.position }.let { it.size == it.toSet().size }
        )
    }

    fun onAddShock(id: String) {
        val shocks = state.value.shocks.toMutableMap()
        shocks[id]?.let { shocks[id] = it + 1 } ?: run { shocks[id] = 1 }
        edited = true
        _state.value = state.value.copy(shocks = shocks, buttonEnabled = state.value.positionsAllDistinct)
    }

    fun onRemoveShock(id: String) {
        val shocks = state.value.shocks.toMutableMap()
        shocks[id]?.let { shocks[id] = (it - 1).coerceAtLeast(0) }
        edited = true
        _state.value = state.value.copy(shocks = shocks, buttonEnabled = state.value.positionsAllDistinct)
    }

    fun onValidate() {
        // Garde-fou : ne valider que si une modification a eu lieu et que les positions sont distinctes.
        if (!edited || !state.value.positionsAllDistinct) return
        viewModelScope.launch {
            dataStoreRepository.war.firstOrNull()?.let { war ->
                val tracks = war.tracks.toMutableList()
                details?.track?.let { track ->
                    war.tracks.map { it.id }.indexOf(track.id).takeIf { it != -1 }?.let { index ->
                        val shocks = state.value.shocks.filterValues { it > 0 }.map { Shock(it.key, it.value) }
                        val editedTrack = track.copy(
                            index = state.value.mapSelected?.map { it.ordinal.toString() } ?: track.index,
                            positions = state.value.selectedPositions.map { it.position },
                            shocks = shocks.takeIf { it.isNotEmpty() }
                        )
                        tracks[index] = editedTrack
                        updateWar(war, tracks)
                    }
                }
            }
        }
    }

    /**
     * Réécrit la war et recalcule le score hôte (justesse, rule 13) = somme des points
     * (`positionToPoints`) de toutes les positions de toutes les manches. En 12p seul le score
     * hôte est stocké (adverse dérivé) ; en 24p les scores adverses saisis sont préservés. Les
     * pénalités sont conservées.
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
