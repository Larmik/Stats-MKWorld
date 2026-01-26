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
import fr.harmoniamk.statsmkworld.extension.mergeWith
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
import kotlinx.coroutines.flow.launchIn
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
        val mapList: List<Maps> = Maps.entries,
        val mapSelected: List<Maps>? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: List<TeamEntity>? = null,
        val players: List<PlayerEntity> = listOf(),
        val currentPlayer: PlayerEntity? = null,
        val selectedPositions: List<PlayerPosition> = listOf(),
        val teamHostWarScore: Int? = null,
        val teamHostTrackScore: Int? = null,
        val trackOrder: Int? = null,
        val shocks: Map<String, Int> = mutableMapOf(),
        val rosterName: String? = null,
        val rosterId: String? = null,
        val totalPositions: Int? = null,
        val scores: List<WarScore>? = null,
        //12 players
        val teamOpponentScore: Int? = null,
        val trackScore: String? = null,
        val trackDiff: String? = null,
    )

    private val _state = MutableStateFlow(State())

    private val _onBack = MutableSharedFlow<Unit>()
    private val _onNext = MutableSharedFlow<Int>()
    private val _backToWar = MutableSharedFlow<Unit>()

    private val positions = mutableListOf<PlayerPosition>()
    val onBack = _onBack.asSharedFlow()
    val onNext = _onNext.asSharedFlow()
    val backToWar = _backToWar.asSharedFlow()
    private var details: WarDetails? = null

    val state = dataStoreRepository.war
        .filterNotNull()
        .map { WarDetails(it) }
        .zip(dataStoreRepository.mkcTeam) { details, teamHost  ->
            this.details = details
            val teamOpponents = details.war.teamOpponent.mapNotNull { databaseRepository.getTeam(it).firstOrNull() }
            val rosterName = teamHost.rosters.singleOrNull { it.id.toString() == details.war.teamHost }?.name ?: teamHost.name
            val rosterId = teamHost.rosters.singleOrNull { it.id.toString() == details.war.teamHost }?.id ?: teamHost.id
            val players = databaseRepository.getPlayers().firstOrNull()
                ?.filter { it.currentWar == details.war.id.toString() }?.sortedBy { it.name }.orEmpty()
            _state.value.copy(
                teamHost = TeamEntity(teamHost),
                teamOpponent = teamOpponents,
                players = players,
                teamHostWarScore = details.war.scores.firstOrNull { it.teamId == rosterId.toString() }?.score,
                currentPlayer = players.firstOrNull(),
                trackOrder = details.warTracks.size + 1,
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

    fun onMapSelected(map: List<Maps>) {
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
                val scoreHost = _state.value.selectedPositions.map { it.position }.sumOf { it.position.positionToPoints(is24p) }
                val scoreOpponent = 82 - scoreHost
                _state.value = _state.value.copy(
                    trackScore = "$scoreHost - $scoreOpponent",
                    teamHostTrackScore = scoreHost,
                    teamHostWarScore = (state.value.teamHostWarScore ?: 0) + scoreHost,
                    teamOpponentScore = scoreOpponent,
                    trackDiff = when {
                        (scoreHost - scoreOpponent) > 0 -> "+${scoreHost - scoreOpponent}"
                        else -> "${scoreHost - scoreOpponent}"
                    }
                )
                viewModelScope.launch {
                    _onNext.emit(2)
                }

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
                index = _state.value.mapSelected?.map { it.ordinal.toString() } ?: listOf(),
                positions = _state.value.selectedPositions.map { it.position },
                shocks = shockList
            )
            val tracks = mutableListOf<WarTrack>()
            tracks.addAll(it.tracks)
            tracks.add(track)
            val newWar = when (state.value.teamOpponent.orEmpty().size > 1) {
                true -> it.copy(tracks = tracks, scores = listOf(WarScore(teamId = it.teamHost, score = state.value.teamHostWarScore ?: 0)))
                else -> it.copy(tracks = tracks, scores = listOf(
                    WarScore(teamId = it.teamHost, score = state.value.teamHostWarScore ?: 0),
                    WarScore(teamId = it.teamOpponent.firstOrNull().orEmpty(), score = state.value.teamOpponentScore ?: 0)
                ))
            }
            firebaseRepository.writeCurrentWar(newWar)
                .onEach {
                    dataStoreRepository.setCurrentWar(newWar)
                    _backToWar.emit(Unit)
                }
                .launchIn(viewModelScope)

        }

    }


}