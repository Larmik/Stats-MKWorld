package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.PlayerPosition
import fr.harmoniamk.statsmkworld.model.local.PlayerScore
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CurrentWarViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface
) : ViewModel() {

    data class State(
        val details: WarDetails? = null,
        val teamHost: TeamEntity? = null,
        val teamOpponent: TeamEntity? = null,
        val players: List<PlayerScore> = listOf(),
        val isOver: Boolean = false,
        val buttonsVisible: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    private val _backToHome = MutableSharedFlow<Unit>()

    val backToHome = _backToHome.asSharedFlow()

    val state = _state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onResume() {
        dataStoreRepository.mkcTeam
            .mapNotNull { it.id }
            .mapNotNull {
                val datastoreWar = dataStoreRepository.war.firstOrNull()
                (datastoreWar ?: firebaseRepository.getCurrentWar(it.toString()).firstOrNull())?.let { war ->
                    val teamHost = databaseRepository.getTeam(war.teamHost).firstOrNull()
                    val teamOpponent = databaseRepository.getTeam(war.teamOpponent).firstOrNull()
                    val buttonsVisible = datastoreWar != null
                    State(
                        details = WarDetails(war),
                        players = initPlayersList(war),
                        teamHost = teamHost,
                        teamOpponent = teamOpponent,
                        buttonsVisible = buttonsVisible,
                        isOver = war.tracks.size == 12
                    )
                }
            }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)

    }

    private suspend fun initPlayersList(war: War): List<PlayerScore> {

        val localPlayers = databaseRepository.getPlayers().firstOrNull()

        val currentLocalPlayers = localPlayers
            ?.filter { player -> war.tracks.flatMap { it.positions }.any { it.playerId == player.id  } || player.currentWar == war.id.toString() }
            ?.map { PlayerScore(it, 0, 0, 0) }
            .orEmpty()


        val players = when (currentLocalPlayers.isEmpty()) {
            true -> firebaseRepository.getUsers(war.teamHost)
                .firstOrNull()
                ?.filter { player -> war.tracks.flatMap { it.positions }.any { it.playerId == player.id  } ||  player.currentWar == war.id.toString()}
                ?.map { user -> localPlayers?.firstOrNull { it.id == user.id } }
                ?.map { PlayerScore(it, 0, 0, 0) }
                .orEmpty()

            else -> currentLocalPlayers
        }

        val trackList = war.tracks
        val finalList = mutableListOf<PlayerScore>()
        val positions = mutableListOf<Pair<PlayerEntity?, Int>>()
        val shocks =  trackList.flatMap { it.shocks.orEmpty() }
        trackList.forEach {
            it.positions.takeIf { it.isNotEmpty() }?.let { warPositions ->
                val trackPositions = mutableListOf<PlayerPosition>()
                warPositions.forEach { position ->
                        trackPositions.add(
                            PlayerPosition(
                                position = position,
                                player = players.map { it.player }.singleOrNull { it?.id == position.playerId }
                            )
                        )
                }
                trackPositions.groupBy { it.player }.entries.forEach { entry ->
                    positions.add(
                        Pair(
                            entry.key,
                            entry.value.sumOf { playerPos -> playerPos.position.position.positionToPoints() }
                        )
                    )
                }
            }
        }
        val temp = positions.groupBy { it.first }
            .map { Pair(it.key, it.value.sumOf { it.second }) }
            .sortedByDescending { it.second }
        temp.forEach { pair ->
            finalList.add(PlayerScore(
                player = pair.first,
                score = pair.second,
                trackPlayed = trackList.filter { it.positions.any { it.playerId == pair.first?.id } }.size,
                shockCount = shocks.filter { it.playerId == pair.first?.id }.sumOf { it.count }
            ))
        }
        players
            .filter { !finalList.map { it.player?.id }.contains(it.player?.id) }
            .forEach { finalList.add(it) }
        return finalList
    }

    fun onValidateWar() {
        _state.value.details?.war?.let { war ->
            firebaseRepository.writeWar(war)
                .onEach {
                    databaseRepository.writeWar(WarEntity(war)).firstOrNull()
                    val players = databaseRepository.getPlayers().firstOrNull()
                    players?.filter { it.currentWar == war.id.toString() }?.forEach {
                        databaseRepository.updateUser(it.id, "").firstOrNull()
                        firebaseRepository.writeUser(
                            teamId = state.value.details?.war?.teamHost.orEmpty(),
                            user = User(
                                id = it.id,
                                currentWar = "",
                                role = it.role
                            )
                        ).firstOrNull()
                    }
                    dataStoreRepository.deleteCurrentWar()
                }
                .flatMapLatest { firebaseRepository.deleteCurrentWar(war.teamHost) }
                .onEach { _backToHome.emit(Unit) }
                .launchIn(viewModelScope)
        }
    }

}