package fr.harmoniamk.statsmkworld.screen.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.datasource.network.DiscordDataSourceInterface
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.emit
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.NotificationRepositoryInterface
import fr.harmoniamk.statsmkworld.usecase.FetchUseCaseInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SignupViewModel.Factory::class)
class SignupViewModel @AssistedInject constructor(
    @Assisted val code: String,
    private val authDataSource: DiscordDataSourceInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val mkCentralDataSource: MKCentralDataSourceInterface,
    private val notificationRepository: NotificationRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface,
    private val fetchUseCase: FetchUseCaseInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(code: String): SignupViewModel
    }

    data class State(
        val launched: Boolean = false,
        val country: String? = null,
        val currentPage: Int? = null
    )

    private val _state = MutableStateFlow(State())
    private val _showNotif = MutableSharedFlow<Unit>()
    private val _onNext = MutableSharedFlow<Unit>()

    val showNotif = _showNotif.asSharedFlow()
    val onNext = _onNext.asSharedFlow()

    private var currentPage: Int? = null


    val state = when {
        code.isNotEmpty() ->
            //Récupération du token Discord à partir de la redirection
            authDataSource.getToken(code)
                .mapNotNull { it.successResponse?.accessToken }
                .filterNot { it.isEmpty() }
                .onEach {
                    dataStoreRepository.setAccessToken(it)
                    currentPage = 4

                }
        //Récupération du token en local (user déjà connecté)
        else -> dataStoreRepository.accessToken.filterNot { it.isEmpty() }
    }
        .flatMapLatest { authDataSource.getUser(it) }
        .mapNotNull {
            if (it.successResponse == null)
                 currentPage = 6
            it.successResponse?.id
        }
        //On recherche dans le registre avec l'ID Discord, puis on récupère le fullPlayer avec l'ID du résultat
        .flatMapLatest { mkCentralDataSource.findPlayer(it) }
        .mapNotNull { it?.playerList?.firstOrNull() }
        .flatMapLatest { mkCentralDataSource.getPlayer(it.id.toString()) }
        .mapNotNull { it.successResponse }
        .map {
            val teamId = it.rosters?.firstOrNull()?.teamID
            //Set player dans datastore puis écriture sur Firebase (si non existant)
            dataStoreRepository.setMKCPlayer(it)
            if (firebaseRepository.getUser(teamId.toString(), it.id.toString()).firstOrNull() == null) {
                val user = User(it.id.toString())
                firebaseRepository.writeUser(teamId.toString(), user)
            }
            //Fetch classique, puis affichage du succès, MAJ de la date et redirection home
            fetchUseCase.fetchTeam(teamId.toString())
                .flatMapLatest { fetchUseCase.fetchAllies(teamId.toString()) }
                .flatMapLatest { fetchUseCase.fetchTeams() }
                .flatMapLatest { fetchUseCase.fetchWars(teamId.toString()) }
                .onEach {
                    dataStoreRepository.setLastUpdate(Date().time)
                    currentPage = 5
                    delay(2000)
                    _onNext.emit(Unit)
                }.launchIn(viewModelScope)



            _state.value.copy(currentPage = currentPage, launched = true)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun requestNotifications() {
        if (!notificationRepository.requestAuthorization) _state.value =
            _state.value.copy(currentPage = 3)
        else _showNotif.emit(Unit, viewModelScope)
    }


    fun onRetry() {
        viewModelScope.launch {
            dataStoreRepository.setAccessToken("")
            currentPage = 3
        }
    }
}