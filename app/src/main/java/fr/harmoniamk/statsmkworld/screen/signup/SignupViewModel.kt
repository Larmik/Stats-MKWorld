package fr.harmoniamk.statsmkworld.screen.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.datasource.network.DiscordDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.emit
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.NotificationRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.SeasonRepositoryInterface
import fr.harmoniamk.statsmkworld.usecase.FetchUseCaseInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SignupViewModel.Factory::class)
class SignupViewModel @AssistedInject constructor(
    @Assisted val code: String,
    private val authDataSource: DiscordDataSourceInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val notificationRepository: NotificationRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val fetchUseCase: FetchUseCaseInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val seasonRepository: SeasonRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(code: String): SignupViewModel
    }

    data class State(
        val launched: Boolean = false,
        val currentPage: Int = 0
    )

    private val _state = MutableStateFlow(State())
    private val _showNotif = MutableSharedFlow<Unit>()
    private val _onNext = MutableSharedFlow<Unit>()

    val showNotif = _showNotif.asSharedFlow()
    val onNext = _onNext.asSharedFlow()

    val state = flowOf(Unit)
        .mapNotNull {
            when {
                code.isNotEmpty() -> {
                    val token = authDataSource.getToken(code)
                        .successResponse?.accessToken
                        ?.takeIf { it.isNotEmpty() }
                    token?.let {
                        _state.value = _state.value.copy(currentPage = 4)
                        dataStoreRepository.setAccessToken(it)
                    }
                    token
                }

                //Récupération du token en local (user déjà connecté)
                else -> dataStoreRepository.accessToken.filterNot { it.isEmpty() }.firstOrNull()
            }
        }
        .map { authDataSource.getUser(it) }
        .mapNotNull {
            if (it.successResponse == null && code.isNotEmpty())
                 _state.value = _state.value.copy(currentPage = 6)
            it.successResponse?.id
        }
        //On recherche dans le registre avec l'ID Discord, puis on récupère le fullPlayer avec l'ID du résultat
        .map { mkCentralDataSource.findPlayer(it).successResponse }
        .mapNotNull { it?.playerList?.firstOrNull() }
        .map { mkCentralDataSource.getPlayer(it.id.toString()) }
        .mapNotNull { it.successResponse }
        .map {
            val teamId = it.rosters?.firstOrNull { it.game == "mkworld" }?.teamID?.toString()
            val rosterId = it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()

            // Connexion anonyme Firebase (accès RTDB) à chaque login (UID non stable après
            // réinstallation). Échec réseau toléré : log Crashlytics, on poursuit.
            if (!firebaseRepository.signInAnonymously())
                FirebaseCrashlytics.getInstance().log("signInAnonymously failed")

            //Set player dans datastore puis écriture sur Firebase (si non existant)
            dataStoreRepository.setMKCPlayer(it)
            val user = User(it.id.toString(), discordId = it.discord?.discordID.orEmpty(), name = it.name)


            //Fetch classique, puis affichage du succès, MAJ de la date et redirection home
            fetchUseCase.fetchTeam(teamId.toString())
            fetchUseCase.fetchAllies(teamId.toString())
            fetchUseCase.fetchTeams()
            // Hydratation eager des saisons (#73) : InitStatsWorker tourne avant que le player
            // existe → les écrire ici (séquence awaited) pour les avoir dès Home. `fetchSeasons`
            // self-seed l'historique si le nœud RTDB est vide.
            seasonRepository.fetchSeasons(teamId.toString())
            val team = dataStoreRepository.mkcTeam.firstOrNull()
            val rosters = team?.rosters?.filter { it.game == "mkworld" }
            val player = rosters?.flatMap { it.players }?.singleOrNull { it.playerId == user.id }
            val role = when (player?.leader) {
                true -> 2
                else -> 0
            }

            firebaseRepository.writeUser(teamId.toString(), user.copy(role = role))
            player?.let { player ->
                databaseRepository.writePlayer(PlayerEntity(
                    player = player,
                    rosterId = rosters.singleOrNull { it.players.map { it.playerId }.contains(player.playerId) }?.id.toString()
                ))
            }
            rosters?.map { it.id.toString() }?.forEach {
                fetchUseCase.fetchWars(it)
            }


            dataStoreRepository.setLastUpdate(Date().time)
            _state.value = _state.value.copy(currentPage = 5)
            delay(2000.milliseconds)
            _onNext.emit(Unit)
            _state.value.copy(launched = true)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun requestNotifications() {
        viewModelScope.launch {
            if (!notificationRepository.requestAuthorization()) _state.value =
                _state.value.copy(currentPage = 3)
            else _showNotif.emit(Unit, viewModelScope)
        }
    }


    fun onRetry() {
        viewModelScope.launch {
            dataStoreRepository.setAccessToken("")
            _state.value = _state.value.copy(currentPage = 3)
        }
    }
}