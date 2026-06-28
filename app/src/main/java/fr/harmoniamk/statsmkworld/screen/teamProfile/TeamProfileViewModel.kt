package fr.harmoniamk.statsmkworld.screen.teamProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayerList
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = TeamProfileViewModel.Factory::class)
class TeamProfileViewModel @AssistedInject constructor(
    @Assisted val id: String,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): TeamProfileViewModel
    }

    data class State(
        val team: MKCTeam? = null,
        val allyList: List<PlayerEntity> = listOf(),
        val playerList: List<MKCPlayer> = listOf(),
        val addAllyVisible: Boolean = false
    )

    private val _state = MutableSharedFlow<State>()
    private val _onDismiss = MutableSharedFlow<Unit>()

    val onDismiss = _onDismiss.asSharedFlow()

    private suspend fun searchPlayers(page: Int, term: String): Pair<Int?, MKCPlayerList?> {
        val players = mkCentralDataSource.searchPlayers(page, term).successResponse
        return Pair(players?.pageCount, players?.playerList)
    }

    fun onSearchPlayers(term: String) {
        viewModelScope.launch {
            if (term.length >= 3) {
                var page = 1
                val playerList = mutableListOf<MKCPlayer>()
                _state.emit(state.value.copy(playerList = listOf()))
                var player: Pair<Int?, List<MKCPlayer>?>? = searchPlayers(page, term)

                playerList.addAll(player?.second.orEmpty())
                while (page < (player?.first ?: 0)) {
                    page++
                    player = searchPlayers(page, term)
                    playerList.addAll(player?.second?.filterNot { state.value.allyList.map { it.id }.contains(it.id.toString()) }.orEmpty())
                }
                _state.emit(state.value.copy(playerList = playerList))
            } else _state.emit(state.value.copy(playerList = listOf()))
        }

    }

    val state =  flowOf(Unit)
        .mapNotNull {
            when (id) {
                "me" -> dataStoreRepository.mkcTeam.firstOrNull()
                else -> mkCentralDataSource.getTeam(id).successResponse
            }
        }
        .map {
            val allyList = when (id) {
                "me" -> databaseRepository.getPlayers().firstOrNull()?.filter { it.rosterId == "-1" }.orEmpty()
                else -> listOf()
            }
            val buttonVisible = firebaseRepository
                .getUser(it.id.toString(), dataStoreRepository.mkcPlayer.firstOrNull()?.id.toString())
                .map { it?.role ?: 0 }
                .map { it > 0 }
                .firstOrNull()
            State(team = it, addAllyVisible = buttonVisible == true, allyList = allyList)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    fun addAlly(player: MKCPlayer) {
        dataStoreRepository.mkcTeam
            .onEach { team ->
                val user = User(player)
                firebaseRepository.writeAlly(team.id.toString(), user).firstOrNull()
            }
            .map { PlayerEntity(player = player, isAlly = true) }
            .flatMapLatest { databaseRepository.addAlly(it) }
            .onEach {
                val allyList = when (id) {
                    "me" -> databaseRepository.getPlayers().firstOrNull()?.filter { it.rosterId == "-1" }.orEmpty()
                    else -> listOf()
                }
                _state.emit(state.value.copy(allyList = allyList))
                _onDismiss.emit(Unit)
            }
            .launchIn(viewModelScope)

    }
}
