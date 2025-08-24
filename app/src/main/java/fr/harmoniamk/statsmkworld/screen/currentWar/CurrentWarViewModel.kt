package fr.harmoniamk.statsmkworld.screen.currentWar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.extension.withPlayersList
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
import kotlinx.coroutines.flow.filterNotNull
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

    init {
        dataStoreRepository.mkcTeam
            .flatMapLatest { firebaseRepository.listenToCurrentWar(it.id.toString()) }
            .filterNotNull()
            .onEach {
                _state.value = state.value.copy(
                    details = WarDetails(it),
                    players = it.withPlayersList(databaseRepository, firebaseRepository),
                    isOver = it.tracks.size == 12
                )
            }.launchIn(viewModelScope)
    }


    fun onResume() {
        dataStoreRepository.mkcTeam
            .mapNotNull { it.id }
            .mapNotNull {
                val datastoreWar = dataStoreRepository.war.firstOrNull()
                (datastoreWar ?: firebaseRepository.getCurrentWar(it.toString())
                    .firstOrNull())?.let { war ->
                    val teamHost = databaseRepository.getTeam(war.teamHost).firstOrNull()
                    val teamOpponent = databaseRepository.getTeam(war.teamOpponent).firstOrNull()
                    val buttonsVisible = datastoreWar != null
                    State(
                        details = WarDetails(war),
                        players = war.withPlayersList(databaseRepository, firebaseRepository),
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
                                role = it.role,
                                name = it.name,
                                discordId = it.discordId
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