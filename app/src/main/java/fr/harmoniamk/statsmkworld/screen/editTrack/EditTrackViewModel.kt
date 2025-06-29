package fr.harmoniamk.statsmkworld.screen.editTrack

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.WarTrackDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = EditTrackViewModel.Factory::class)
class EditTrackViewModel @AssistedInject constructor(
    @Assisted val details: WarTrackDetails?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(details: WarTrackDetails?): EditTrackViewModel
    }

    data class State(
        val mapList: List<Maps> = Maps.entries,
        val mapSelected: Maps? = null,
        val players: List<PlayerEntity> = listOf(),
        val currentPlayer: PlayerEntity? = null,
        val selectedPositions: List<PlayerPosition> = listOf(),
        val buttonEnabled: Boolean = false
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
            State(
                players = players,
                currentPlayer = players.firstOrNull()
            )
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onMapSelected(map: Maps) {
        _state.value = state.value.copy(
            mapSelected = map,
            buttonEnabled = positions.isEmpty() || positions.size == 6
        )
    }

    fun onPositionClick(position: Int) {
        details?.track?.positions?.firstOrNull { it.playerId == state.value.currentPlayer?.id }
            ?.let { pos ->
                val newPos = PlayerPosition(
                    player = state.value.currentPlayer,
                    position = pos.copy(position = position)
                )
                positions.add(newPos)
                _state.value = state.value.copy(
                    selectedPositions = positions.sortedBy { it.position.position },
                    currentPlayer = state.value.players.getOrNull(positions.indexOf(newPos) + 1),
                    buttonEnabled = state.value.mapSelected != null && (positions.isEmpty() || positions.size == 6)
                            || state.value.mapSelected == null && positions.size == 6
                )
            }
    }

    fun onValidate() {
        viewModelScope.launch {
            dataStoreRepository.war.firstOrNull()?.let { war ->
                val tracks = war.tracks.toMutableList()
                details?.track?.let { track ->
                    war.tracks.indexOf(track).takeIf { it != -1 }?.let { index ->
                        when {
                            _state.value.mapSelected != null && _state.value.selectedPositions.isEmpty() -> {
                                tracks.add(
                                    index, track.copy(
                                        index = _state.value.mapSelected?.ordinal ?: track.index,
                                    )
                                )
                                tracks.removeAt(index+1)
                                updateWar(war, tracks)
                            }
                            _state.value.mapSelected != null && _state.value.selectedPositions.size == 6 -> {
                                tracks.add(
                                    index, track.copy(
                                        index = _state.value.mapSelected?.ordinal ?: track.index,
                                        positions = _state.value.selectedPositions.map { it.position }
                                    )
                                )
                                tracks.removeAt(index+1)
                                updateWar(war, tracks)
                            }
                            _state.value.mapSelected == null && _state.value.selectedPositions.size == 6 -> {
                                tracks.add(
                                    index, track.copy(
                                        positions = _state.value.selectedPositions.map { it.position }
                                    )
                                )
                                tracks.removeAt(index+1)
                                updateWar(war, tracks)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateWar(war: War, tracks: List<WarTrack>) {
        val warToUpdate = war.copy(tracks = tracks)
        _state.value = State()
        firebaseRepository.writeCurrentWar(warToUpdate)
            .onEach {
                dataStoreRepository.setCurrentWar(warToUpdate)
                _backToCurrent.emit(Unit)
            }
            .launchIn(viewModelScope)
    }
}