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
        // Line-up éditable de la course : une position par joueur, modifiable via ± (source de
        // vérité VM). Pré-remplie avec les positions actuelles de la course.
        val selectedPositions: List<PlayerPosition> = listOf(),
        val buttonEnabled: Boolean = false,
        val shocks: Map<String, Int> = mutableMapOf(),
        val is24p: Boolean = false
    ) {
        /** Nombre max de positions selon le mode (12p → 12, 24p → 24). */
        val maxPosition: Int get() = if (is24p) 24 else 12

        /**
         * Toutes les positions de la line-up sont **distinctes** (aucun doublon). Prérequis
         * pour activer « Confirmer » : deux joueurs ne peuvent partager la même position.
         */
        val positionsAllDistinct: Boolean
            get() = selectedPositions.map { it.position.position }.let { it.size == it.toSet().size }
    }

    private val _state = MutableStateFlow(State())
    // Vrai dès qu'une édition (circuit / position / shock) a eu lieu : « Confirmer » n'a de sens
    // qu'après une modification.
    private var edited = false

    private val _backToCurrent = MutableSharedFlow<Unit>()
    val backToCurrent = _backToCurrent.asSharedFlow()

    val state = dataStoreRepository.war
        .filterNotNull()
        .map { war ->
            val players = databaseRepository.getPlayers().firstOrNull()
                ?.filter { it.currentWar == war.id.toString() }?.sortedBy { it.name }.orEmpty()

            // Line-up initiale triée par POSITION de départ — tri appliqué UNE SEULE FOIS à
            // l'initialisation. Ensuite l'ordre est figé : `onPositionChange` ne re-trie jamais
            // (les cellules ne bougent pas pendant l'édition). Le recalcul du score reste correct
            // (somme indépendante de l'ordre, les WarPosition portent le playerId).
            val positions = players.mapNotNull { player ->
                details?.track?.positions.orEmpty().singleOrNull { it.playerId == player.id }?.let {
                    PlayerPosition(player = player, position = it)
                }
            }.sortedBy { it.position.position }
            State(
                players = players,
                selectedPositions = positions,
                // Pré-remplir le circuit courant (liseré dans l'onglet Circuit) et les shocks
                // existants (affichés dans la section Positions), pour partir de l'état réel.
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
     * Édite la position d'un joueur par pas de [delta] (−1 / +1), bornée à 1..[State.maxPosition]
     * (12p : 1..12, 24p : 1..24). La position se met à jour en direct ; « Confirmer » se réactive
     * si toutes les positions restent distinctes. Le recalcul du score s'appuie sur cette line-up
     * (à la validation, via [updateWar]).
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
     * Réécrit la war en cours avec les [tracks] édités et **recalcule le score hôte**
     * (justesse prioritaire, rule 13). Le score de war hôte est la somme des points
     * (barème `positionToPoints`) de **toutes** les positions de **toutes** les manches :
     * il reflète donc immédiatement l'édition d'un circuit (barème 12p/24p) ou d'une position
     * (les shocks restent hors score).
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
