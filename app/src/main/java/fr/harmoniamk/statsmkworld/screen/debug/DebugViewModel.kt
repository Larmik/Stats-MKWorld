package fr.harmoniamk.statsmkworld.screen.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.local.MissingPlayer
import fr.harmoniamk.statsmkworld.model.local.UnknownOpponentDiagnostic
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DiagnosticRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.PDFRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.WorldRecordsRepositoryInterface
import fr.harmoniamk.statsmkworld.model.ScoringConstants
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val fetchUseCase: FetchUseCaseInterface,
    private val diagnosticRepository: DiagnosticRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val worldRecordsRepository: WorldRecordsRepositoryInterface,
    private val pdfRepository: PDFRepositoryInterface
) : ViewModel() {

    private val _sharedToast = MutableSharedFlow<String>()
    private val _sharedLoading = MutableStateFlow<String?>(null)

    private val _sendNotif = MutableSharedFlow<Unit>()
    private val _diagnostics = MutableStateFlow<List<UnknownOpponentDiagnostic>>(emptyList())
    private val _missingPlayers = MutableStateFlow<List<MissingPlayer>>(emptyList())

    val sendNotif = _sendNotif.asSharedFlow()
    val sharedToast = _sharedToast.asSharedFlow()
    val sharedLoading = _sharedLoading.asStateFlow()
    val diagnostics = _diagnostics.asStateFlow()
    val missingPlayers = _missingPlayers.asStateFlow()

    val sharedMatrixMode = dataStoreRepository.matrixMode

    fun onUpdateTags() {
        viewModelScope.launch {
            fetchUseCase.fetchTags()
            _sharedToast.emit("Tags mis à jour")
        }
    }

    fun onMatrix(playerId: String) {
        var user: User? = null
        viewModelScope.launch {
            when (playerId.isEmpty()) {
                true -> _sharedToast.emit("Il faut un ID de joueur pour cela")

                else -> flowOf(Unit)
                    .onEach { _sharedLoading.emit("Entrée dans la matrice...") }
                    .mapNotNull { fetchUseCase.fetchPlayer(playerId) }
                    .map {
                        user = User(
                            it.id.toString(),
                            discordId = it.discord?.discordID.orEmpty(),
                            name = it.name
                        )
                        val team = it.rosters?.firstOrNull { it.game == "mkworld" }
                        if (team == null)
                            _sharedLoading.emit(null)
                        team
                    }
                    .mapNotNull { fetchUseCase.fetchTeam(it?.teamID.toString()) }
                    .onEach {
                        user?.let { user ->
                            val teamId = it.id.toString()
                            if (firebaseRepository.getUser(teamId, user.id) == null) {
                                val role = when (it.rosters.filter { it.game == "mkworld" }
                                    .flatMap { it.players }
                                    .singleOrNull { it.playerId == user.id }?.leader) {
                                    true -> 2
                                    else -> 0
                                }
                                firebaseRepository.writeUser(teamId, user.copy(role = role))
                            }
                        }
                    }
                    .map { fetchUseCase.fetchAllies(it.id.toString()) }
                    .map { fetchUseCase.fetchTeams() }
                    .onEach { databaseRepository.clearWars() }
                    .mapNotNull {
                        dataStoreRepository.mkcTeam.firstOrNull()?.rosters?.filter { it.game == "mkworld" }
                            ?.map { it.id.toString() }
                    }
                    .map { ids ->
                        ids.forEach {
                            fetchUseCase.fetchWars(it)
                        }
                        dataStoreRepository.setLastUpdate(Date().time)
                        dataStoreRepository.setMatrixMode(true)
                        _sharedLoading.emit(null)
                    }.firstOrNull()
            }


        }
    }

    fun onMatrixEnd() {
        viewModelScope.launch {
            _sharedLoading.emit("Sortie de la matrice...")
            databaseRepository.clearWars()
            fetchUseCase.fetchData(ScoringConstants.DEBUG_PLAYER_ID)
            dataStoreRepository.setMatrixMode(false)
            _sharedLoading.emit(null)
        }
    }

    fun onNotif() {
        viewModelScope.launch {
            if (dataStoreRepository.notifEnabled.firstOrNull() == true)
                _sendNotif.emit(Unit)
        }

    }

    fun onManageTransferts() {
        flowOf(Unit)
            .onEach { _sharedLoading.emit("Transferts en cours...") }
            .flatMapLatest { fetchUseCase.manageTransferts() }
            .onEach {
                _sharedLoading.emit(null)
            }.launchIn(viewModelScope)
    }

    fun onMigrateOpponents() {
        flowOf(Unit)
            .onEach { _sharedLoading.emit("Migration des adversaires en cours...") }
            .flatMapLatest { fetchUseCase.migrateOpponentsToRoster() }
            .onEach {
                _sharedLoading.emit(null)
                _sharedToast.emit("Migration des adversaires terminée")
            }.launchIn(viewModelScope)
    }

    fun onDiagnoseUnknownOpponents() {
        viewModelScope.launch {
            _sharedLoading.emit("Diagnostic des adversaires inconnus...")
            _diagnostics.value = diagnosticRepository.diagnoseUnknownOpponents()
            _sharedLoading.emit(null)
            _sharedToast.emit("${_diagnostics.value.size} war(s) à adversaire inconnu")
        }
    }

    // Réattribution (paquet A) : réécrit teamOpponent vers newId (résolvable
    // localement). Action manuelle, déclenchée après décision humaine.
    fun onReattributeOpponent(hostRosterId: String, warId: Long, rawId: String, newId: String) {
        viewModelScope.launch {
            _sharedLoading.emit("Réattribution en cours...")
            diagnosticRepository.reattributeOpponent(hostRosterId, warId, rawId, newId)
            _diagnostics.value = diagnosticRepository.diagnoseUnknownOpponents()
            _sharedLoading.emit(null)
            _sharedToast.emit("Réattribution effectuée")
        }
    }

    // Suppression (paquet B) : retire la war irrécupérable de Firebase. Action
    // destructive, déclenchée après confirmation dans l'écran.
    fun onDeleteWar(hostRosterId: String, warId: Long) {
        viewModelScope.launch {
            _sharedLoading.emit("Suppression de la war...")
            diagnosticRepository.deleteWar(hostRosterId, warId)
            _diagnostics.value = diagnosticRepository.diagnoseUnknownOpponents()
            _sharedLoading.emit(null)
            _sharedToast.emit("War supprimée")
        }
    }

    fun onDiagnoseMissingPlayers() {
        viewModelScope.launch {
            _sharedLoading.emit("Diagnostic des joueurs manquants...")
            _missingPlayers.value = diagnosticRepository.diagnoseMissingPlayers()
            _sharedLoading.emit(null)
            _sharedToast.emit("${_missingPlayers.value.size} joueur(s) manquant(s)")
        }
    }

    // Ajoute le joueur manquant en allié (local + Firebase newAllies) puis
    // re-diagnostique pour qu'il disparaisse de la liste.
    fun onAddMissingPlayerAsAlly(playerId: String) {
        viewModelScope.launch {
            _sharedLoading.emit("Ajout de l'allié...")
            diagnosticRepository.addMissingPlayerAsAlly(playerId)
            _missingPlayers.value = diagnosticRepository.diagnoseMissingPlayers()
            _sharedLoading.emit(null)
            _sharedToast.emit("Allié ajouté")
        }
    }

    fun loadWRs() {
        viewModelScope.launch {
            worldRecordsRepository.getCurrentWRs()
        }
    }

    fun onUpdateBotData() {
        dataStoreRepository.mkcTeam
            .onEach {
                firebaseRepository.getUsers(it.id.toString()).forEach { user ->
                    mkCentralDataSource.getPlayer(user.id).successResponse?.let { player ->
                        val newUser = user.copy(
                            discordId = player.discord?.discordID.orEmpty(),
                            name = player.name
                        )
                        firebaseRepository.writeUser(it.id.toString(), newUser)
                    }
                }
            }.launchIn(viewModelScope)
    }

}