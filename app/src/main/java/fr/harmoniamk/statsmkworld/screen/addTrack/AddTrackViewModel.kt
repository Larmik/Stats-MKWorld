package fr.harmoniamk.statsmkworld.screen.addTrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.application.MainApplication
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddTrackViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
) : ViewModel() {

    data class State(
        val mapList: List<Maps> = Maps.entries,
        val mapSelected: Maps? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val players: List<PlayerEntity> = listOf(),
        val currentPlayer: PlayerEntity? = null,
        val selectedPositions: List<PlayerPosition> = listOf(),
        val score: String? = null,
        val diff: String? = null,
        val trackScore: String? = null,
        val trackDiff: String? = null,
        val trackOrder: Int? = null
    )

    private val _state = MutableStateFlow(State())
    private val _onBack = MutableSharedFlow<Unit>()
    private val _onNext = MutableSharedFlow<Unit>()
    private val _backToWar = MutableSharedFlow<Unit>()

    private val positions = mutableListOf<PlayerPosition>()
    val onBack = _onBack.asSharedFlow()
    val onNext = _onNext.asSharedFlow()
    val backToWar = _backToWar.asSharedFlow()
    private var details: WarDetails? = null

    val state = dataStoreRepository.war
        .filterNotNull()
        .map { WarDetails(it) }
        .map { details ->
            this.details = details
            val teamHost = databaseRepository.getTeam(details.war.teamHost).firstOrNull()
            val teamOpponent = databaseRepository.getTeam(details.war.teamOpponent).firstOrNull()
            val players = databaseRepository.getPlayers().firstOrNull()
                ?.filter { it.currentWar == details.war.id.toString() }?.sortedBy { it.name }.orEmpty()
            _state.value.copy(
                teamHost = teamHost,
                teamOpponent = teamOpponent,
                score = details.displayedScore,
                diff = details.displayedDiff,
                players = players,
                currentPlayer = players.firstOrNull(),
                trackOrder = details.warTracks.size + 1
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
            )?.lowercase()?.contains(searched.lowercase()) ?: true
        })
    }

    fun onMapSelected(map: Maps) {
        _state.value = _state.value.copy(mapSelected = map)
    }

    fun onBack() {
        when {
            _state.value.selectedPositions.isNotEmpty() && _state.value.trackScore == null -> {
                positions.remove(positions.last())
                _state.value = _state.value.copy(
                    selectedPositions = positions.sortedBy { it.position.position },
                    currentPlayer = _state.value.players.getOrNull(positions.size)
                )
            }
            _state.value.trackScore != "0 - 0" -> {
                positions.clear()
                _state.value = _state.value.copy(
                    selectedPositions = listOf(),
                    currentPlayer = _state.value.players.first(),
                    trackScore = null
                )
                viewModelScope.launch {
                    _onBack.emit(Unit)
                }
            }

            else -> viewModelScope.launch {
                _onBack.emit(Unit)
            }
        }

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
            positions.size == _state.value.players.size -> {
                val scoreHost = _state.value.selectedPositions.map { it.position }.sumOf { it.position.positionToPoints() }
                val scoreOpponent = 82 - scoreHost
                _state.value = _state.value.copy(
                    trackScore = "$scoreHost - $scoreOpponent",
                    trackDiff = when {
                        (scoreHost - scoreOpponent) > 0 -> "+${scoreHost - scoreOpponent}"
                        else -> "${scoreHost - scoreOpponent}"
                    }
                )
                viewModelScope.launch {
                    _onNext.emit(Unit)
                }

            }

            else -> _state.value = _state.value.copy(
                currentPlayer = _state.value.players.getOrNull(
                    positions.indexOf(pos) + 1
                )
            )

        }
    }

    fun onValidate() {
        details?.war?.let {
            val track = WarTrack(
                id = System.currentTimeMillis(),
                index = _state.value.mapSelected?.ordinal ?: 0,
                positions = _state.value.selectedPositions.map { it.position }
            )
            val tracks = mutableListOf<WarTrack>()
            tracks.addAll(it.tracks)
            tracks.add(track)
            val newWar = it.copy(tracks = tracks)
            firebaseRepository.writeCurrentWar(newWar)
                .onEach {
                    dataStoreRepository.setCurrentWar(newWar)
                    _backToWar.emit(Unit)
                }
                .launchIn(viewModelScope)
        }

    }


}