package fr.harmoniamk.statsmkworld.screen.playerProfile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.datasource.network.DiscordDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.usecase.FetchUseCaseInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PlayerProfileViewModel.Factory::class)
class PlayerProfileViewModel @AssistedInject constructor(@Assisted val id: String, mkCentralDataSource: MKCentralDataSourceInterface, private val dataStoreRepository: DataStoreRepositoryInterface, private val firebaseRepository: FirebaseRepositoryInterface, private val databaseRepository: DatabaseRepositoryInterface, private val fetchUseCase: FetchUseCaseInterface, private val authDataSource: DiscordDataSourceInterface) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): PlayerProfileViewModel
    }

    data class State(
        val currentPlayer: MKCPlayer? = null,
        val player: MKCPlayer? = null,
        val buttonVisible: Boolean = false,
        val isAlly: Boolean = false,
        @StringRes val role: Int? = null,
        val lastUpdate: String? = null,
        val showMenu: Boolean = false,
        @StringRes val dialogTitle: Int? = null,
        @StringRes val confirmDialog: Int? = null,
        @StringRes val adminButtonLabel: Int? = null,
        val teamId: String? = null
    )

    private val _state = MutableStateFlow(State())
    private val _backToLogin = MutableSharedFlow<Unit>()

    val backToLogin = _backToLogin.asSharedFlow()




    val state = dataStoreRepository.mkcPlayer
        .flatMapLatest {
            _state.value = _state.value.copy(currentPlayer = it)
            when (id) {
                "me" -> flowOf(it)
                else -> mkCentralDataSource.getPlayer(id).mapNotNull { it.successResponse }
            }
        }
        .filterNotNull()
        .mapNotNull {
            val team = dataStoreRepository.mkcTeam
                .firstOrNull()

            val isLeader =  firebaseRepository.getUser(team?.id.toString(), _state.value.currentPlayer?.id.toString())
                .map { it?.role ?: 0 }
                .map { it == 2 }
                .firstOrNull()

            val roster = team
                ?.rosters
                ?.firstOrNull { it.game == "mkworld" }
                ?.players.orEmpty()

            val canAlly = !roster.map { it.playerId }.contains(id) && id != "me"

            val isAlly = databaseRepository.getPlayers().firstOrNull()
                ?.singleOrNull { it.id == id }
                ?.isAlly

            val role = firebaseRepository.getUser(team?.id.toString(), it.id.toString())
                .map { when (it?.role) {
                    1 -> R.string.admin
                    2 -> R.string.leader
                    else -> R.string.membre
                } }.firstOrNull()
            val lastUpdate = dataStoreRepository.lastUpdate.map { Date(it).displayedString("dd/MM/yyyy - HH:mm") }.firstOrNull().takeIf { id == "me" && it?.startsWith("01/01/1970") != true }
            State(
                player = it,
                buttonVisible = canAlly && isAlly != true,
                isAlly = isAlly == true,
                role = role.takeIf { _ -> it.rosters?.any { it.teamID.toString() == team?.id.toString() } == true },
                showMenu = id == "me",
                lastUpdate = lastUpdate,
                adminButtonLabel = role.takeIf { role ->
                    role != R.string.leader && isLeader == true && id != "me" && id != _state.value.currentPlayer?.id.toString() && it.rosters?.any { it.teamID.toString() == team?.id.toString() } == true
                }?.let {
                    when (it) {
                        R.string.admin -> R.string.basculer_en_tant_que_membre
                        else -> R.string.basculer_en_tant_qu_admin
                    }
                },
                teamId = team?.id.toString()
            )
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onAddAlly() {
        dataStoreRepository.mkcTeam
            .flatMapLatest { firebaseRepository.writeAlly(it.id.toString(), id) }
            .mapNotNull { state.value.player }
            .map { PlayerEntity(it, isAlly = true) }
            .flatMapLatest { databaseRepository.addAlly(it) }
            .onEach { _state.value = state.value.copy(
                buttonVisible = false,
                isAlly = true
            ) }
            .launchIn(viewModelScope)

    }

    fun onSwitchRole() {
        dataStoreRepository.mkcTeam
            .flatMapLatest { firebaseRepository.getUser(it.id.toString(), id) }
            .filterNotNull()
            .map {
                val newRole = when (state.value.role) {
                    R.string.membre -> 1
                    else -> 0
                }
                _state.value = state.value.copy(
                    adminButtonLabel = when (newRole) {
                        0 -> R.string.basculer_en_tant_qu_admin
                        else -> R.string.basculer_en_tant_que_membre
                    },
                    role = when (newRole) {
                        0 -> R.string.membre
                        else -> R.string.admin
                    }
                )
                it.copy(role = newRole)
            }
            .flatMapLatest { firebaseRepository.writeUser(state.value.teamId.orEmpty(), it) }
            .flatMapLatest { databaseRepository.getPlayer(id) }
            .flatMapLatest {
                val newRole = when (_state.value.role) {
                    R.string.admin -> 1
                    else -> 0
                }
                val updatedPlayer = PlayerEntity(
                    id = it.id,
                    name = it.name,
                    country = it.country,
                    role = newRole,
                    currentWar = it.currentWar,
                    isAlly = it.isAlly
                )
                databaseRepository.writePlayer(updatedPlayer)
            }
            .launchIn(viewModelScope)

    }

    init {
        dataStoreRepository.lastUpdate
            .onEach { _state.value = state.value.copy(lastUpdate = Date(it).displayedString("dd/MM/yyyy - HH:mm")) }
            .launchIn(viewModelScope)
    }

    fun onRefresh() {
        viewModelScope.launch {
            dataStoreRepository.mkcPlayer.firstOrNull()?.id?.let {
                _state.value = state.value.copy(dialogTitle = R.string.fetch_player)
                fetchUseCase.fetchPlayer(it.toString())
                    .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" } }
                    .onEach {
                       _state.value = state.value.copy(dialogTitle = R.string.fetch_team)

                    }
                    .flatMapLatest { fetchUseCase.fetchTeam(it.teamID.toString()) }
                    .onEach {
                        _state.value = state.value.copy(dialogTitle = R.string.fetch_allies)

                    }
                    .flatMapLatest { fetchUseCase.fetchAllies(it.id.toString()) }
                    .onEach {
                        _state.value = state.value.copy(dialogTitle = R.string.fetch_opponents)

                    }
                    .flatMapLatest { fetchUseCase.fetchTeams() }
                    .flatMapLatest { dataStoreRepository.mkcTeam }
                    .onEach {
                        _state.value = state.value.copy(dialogTitle = R.string.fetch_Wars)

                    }
                    .flatMapLatest { fetchUseCase.fetchWars(it.id.toString()) }
                    .onEach {
                        _state.value = state.value.copy(dialogTitle = null)

                    }
                    .onEach { dataStoreRepository.setLastUpdate(Date().time) }
                    .launchIn(this)
            }
        }
    }

    fun onLogoutClick() {

        _state.value = state.value.copy(confirmDialog = R.string.logout_confirm)
    }

    fun dismissPopup() {
        _state.value = state.value.copy(confirmDialog = null)

    }

    fun onLogout() {

        databaseRepository.clearTeams()
            .flatMapLatest { databaseRepository.clearPlayers() }
            .flatMapLatest { databaseRepository.clearWars() }
            .flatMapLatest { dataStoreRepository.accessToken }
            .flatMapLatest { authDataSource.revokeToken(it) }
            .onEach {
                dataStoreRepository.clearPlayer()
                dataStoreRepository.clearTeam()
                dataStoreRepository.setPage(3)
                _state.value = state.value.copy(confirmDialog = null)
                _backToLogin.emit(Unit)
            }.launchIn(viewModelScope)


    }
}
