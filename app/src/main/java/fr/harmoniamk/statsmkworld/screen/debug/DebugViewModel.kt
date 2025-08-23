package fr.harmoniamk.statsmkworld.screen.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.usecase.FetchUseCaseInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.collections.firstOrNull

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DebugViewModel @Inject constructor(private val fetchUseCase: FetchUseCaseInterface, private val mkCentralDataSource: MKCentralDataSourceInterface, private val firebaseRepository: FirebaseRepositoryInterface, private val dataStoreRepository: DataStoreRepositoryInterface, private val databaseRepository: DatabaseRepositoryInterface): ViewModel() {

    private val _sharedToast = MutableSharedFlow<String>()
    private val _sharedLoading = MutableStateFlow<String?>(null)
    val sharedToast = _sharedToast.asSharedFlow()
    val sharedLoading = _sharedLoading.asStateFlow()

    val sharedMatrixMode = dataStoreRepository.matrixMode

    fun onUpdateTags() {
        fetchUseCase.fetchTags().onEach { _sharedToast.emit("Tags mis à jour") }.launchIn(viewModelScope)
    }

    fun onMatrix(playerId: String) {
        when (playerId.isEmpty()) {
            true -> viewModelScope.launch {
                _sharedToast.emit("Il faut un ID de joueur pour cela")
            }
            else -> flowOf(Unit)
                .onEach { _sharedLoading.emit("Entrée dans la matrice...") }
                .flatMapLatest { fetchUseCase.fetchPlayer(playerId) }
                .map {
                   val team = it.rosters?.firstOrNull { it.game == "mkworld" }
                    if (team == null)
                        _sharedLoading.emit(null)
                    team
                }
                .flatMapLatest { fetchUseCase.fetchTeam(it?.teamID.toString()) }
                .flatMapLatest { fetchUseCase.fetchAllies(it.id.toString()) }
                .flatMapLatest { fetchUseCase.fetchTeams() }
                .flatMapLatest { databaseRepository.clearWars() }
                .flatMapLatest { dataStoreRepository.mkcTeam }
                .flatMapLatest { fetchUseCase.fetchWars(it.id.toString()) }
                .onEach { dataStoreRepository.setLastUpdate(Date().time) }
                .onEach {
                    dataStoreRepository.setMatrixMode(true)
                    _sharedLoading.emit(null)}
                .launchIn(viewModelScope)
        }
    }

    fun onMatrixEnd() {
        databaseRepository.clearWars()
            .onEach { _sharedLoading.emit("Sortie de la matrice...") }
            .map { "18595" }
            .flatMapLatest { fetchUseCase.fetchData(it) }
            .onEach {
                dataStoreRepository.setMatrixMode(false)
                _sharedLoading.emit(null)}
            .launchIn(viewModelScope)
    }

    fun onManageTransferts() {
        flowOf(Unit)
            .onEach { _sharedLoading.emit("Transferts en cours...") }
            .flatMapLatest { fetchUseCase.manageTransferts() }
            .onEach {
                _sharedLoading.emit(null)
            }.launchIn(viewModelScope)
    }

    fun onUpdateBotData() {
        dataStoreRepository.mkcTeam
            .onEach {
                firebaseRepository.getUsers(it.id.toString()).firstOrNull()?.forEach { user ->
                    mkCentralDataSource.getPlayer(user.id).firstOrNull()?.let { player ->
                        val newUser = user.copy(discordId = player.successResponse?.discord?.discordID.orEmpty(), name = player.successResponse?.name.orEmpty())
                        firebaseRepository.writeUser(it.id.toString(), newUser).firstOrNull()
                    }
                }
            }.launchIn(viewModelScope)
    }

}