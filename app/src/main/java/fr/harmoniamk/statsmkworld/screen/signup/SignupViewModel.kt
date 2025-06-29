package fr.harmoniamk.statsmkworld.screen.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.datasource.network.DiscordDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.emit
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.User
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.NotificationRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.WorkerRepositoryInterface
import fr.harmoniamk.statsmkworld.worker.FindPlayerWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TutorialItem(
    val title: String,
    val text: String,
    val image: Int? = null,
    val lottie: Int? = null,
    val buttonText: String? = null,
    val secondText: String? = null
) {
    START(
        title = "Bienvenue sur Stats MKWorld !",
        text = "Tu t'apprêtes à entrer dans le monde merveilleux des statistiques compétitives sur le nouveau jeu Mario Kart World. \n \n Cet application est un outil afin de connaître la progression d'un joueur ainsi que son équipe à travers des stats récoltées grâce à la saisie des matchs. Ces stats seront détaillées entre différents aspects (joueurs, adversaires, circuits, scores...) et donneront accès à des moyennes, des records ainsi que des indicateurs de progression.",
        buttonText = "Suivant",
        secondText = "Avant de commencer, il existe des prérequis quant à son utilisation : \n \n - Le projet est lié à la plateforme MKCentral, il est obligatoire d'être inscrit en tant que joueur sur le site. \n \n - Il est aussi obligatoire d'avoir un compte Discord lié à son profil MKCentral. \n \n  Si l'une de ces conditions n'est pas remplie, il te sera impossible de t'inscrire sur l'application."
    ),
    OPEN_APP(
        title = "Autoriser l'ouverture des liens",
        text = "Avant toute chose, l'application a besoin d'avoir accès à quelques autorisations, à commencer par l'ouverture des liens dédiés. Cela te permettra de t'authentifier plus facilement avec Discord. \n \n Cette autorisation est nécessaire sur les appareils à partir de Android 12.",
        buttonText = "Réglage des liens",
        lottie = R.raw.link,
        secondText = "Une fois dans les paramètres, il te suffit de sélectionner \"Ajouter un lien\" et d'ajouter le lien \"statsmkworld.com\" proposé afin d'autoriser son ouverture dans l'application."
    ),
    NOTIFICATIONS(
        title = "Autoriser les notifications",
        text = "L'application a également besoin de t'envoyer des notifications pour te tenir informé de l'avancée de certains processus. \n  \n Cette autorisation est nécessaire sur les appareils à partir de Android 13.",
        buttonText = "Activer",
        lottie = R.raw.notif,
        secondText = "Ne t'inquiète pas, l'application ne te spammera sous aucun prétexte."
    ),
    COUNTRY(
        title = "Sélection du pays",
        text = "Sélectionne ton pays tel qu'indiqué sur ton compte MKCentral.",
        buttonText = "Continuer"
    ),
    AUTH(
        title = "Connexion via Discord",
        text = "Parfait ! C'est le moment de t'authentifier à l'aide de ton compte Discord.",
        buttonText = "Connexion",
        lottie = R.raw.discordanim
    ),
    FIND_PLAYER(
        title = "Recherche du profil",
        text = "Afin de te proposer la meilleure expérience possible, l'appli va récupérer tes données MKCentral. Patiente le temps de la recherche de ton profil, cela peut prendre jusqu'à 10 minutes.",
        image = R.drawable.mkcentralpic,
        lottie = R.raw.search,
        secondText = "Tu peux faire autre chose : Si tu as permis les notifications, une notification t'avertira lorsque l'application sera prête."
    ),
    WELCOME(
        title = "Tout est prêt !",
        text = "Il ne te reste plus qu'à profiter de toutes les fonctionnalités qu'offre l'application. Bonne chance sur les circuits de MKWorld !",
        lottie = R.raw.finishanim
    ),
    ERROR(
        title = "Oups !",
        text = "Une erreur est survenue durant la recherche de ton profil MKCentral. Il y'a deux raisons principales :\n \n -Tu n'as pas sélectionné le bon pays. \n \n -Ton compte Discord n'est pas lié à MKCentral.",
        buttonText = "Recommencer",
        secondText = "Note: Tu dois être inscrit sur MKCentral afin de pouvoir utiliser l'application.",
        lottie = R.raw.fail
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SignupViewModel.Factory::class)
class SignupViewModel @AssistedInject constructor(
    @Assisted val code: String,
    private val authDataSource: DiscordDataSourceInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val workerRepository: WorkerRepositoryInterface,
    private val notificationRepository: NotificationRepositoryInterface,
    private val firebaseRepository: FirebaseRepositoryInterface
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

    private var player: MKCPlayer? = null
    private var currentPage: Int? = null

    init {
        dataStoreRepository.page
            .distinctUntilChanged()
            .onEach {
                _state.value = _state.value.copy(currentPage = it)
                currentPage = it
            }.launchIn(viewModelScope)
    }

    val state = when {
        code.isNotEmpty() -> authDataSource.getToken(code)
            .onEach {
                it.errorResponse?.let {
                    firebaseRepository.log(it, "SignupViewModel").firstOrNull()
                }
            }
            .mapNotNull { it.successResponse?.accessToken }
            .filterNot { it.isEmpty() }
            .onEach { dataStoreRepository.setAccessToken(it) }
        else -> dataStoreRepository.accessToken.filterNot { it.isEmpty() }
    }
        .onEach {
            player = dataStoreRepository.mkcPlayer.firstOrNull()
            currentPage = dataStoreRepository.page.firstOrNull()
        }
        .flatMapLatest { authDataSource.getUser(it) }
        .onEach {
            it.errorResponse?.let {
                firebaseRepository.log(it, "SignupViewModel").firstOrNull()
            }
        }
        .mapNotNull {
            val launchedBefore = dataStoreRepository.launchedBefore.firstOrNull()
            when {
                it.successResponse == null && launchedBefore != true -> dataStoreRepository.setPage(7)
                player?.id != 0L ->  {
                    flowOf(Unit)
                        .mapNotNull { dataStoreRepository.mkcPlayer.firstOrNull() }
                        .onEach {
                            if (firebaseRepository.getUser(it.id.toString()).firstOrNull() == null) {
                                val user = User(it.id.toString())
                                firebaseRepository.writeUser(user)
                            }
                            dataStoreRepository.setLaunchedBefore(true)
                        }.launchIn(viewModelScope)
                }
                (currentPage ?: 0) < 7 && code.isNotEmpty() -> dataStoreRepository.setPage(5)
            }
            firebaseRepository.log("Launch task with id ${it.successResponse?.id}, already launched: ${_state.value.launched}, mkcPlayerId: ${dataStoreRepository.mkcPlayer.firstOrNull()?.id}", "SignupViewModel").firstOrNull()
            it.successResponse?.id
        }
        .filterNot { _state.value.launched && dataStoreRepository.mkcPlayer.firstOrNull()?.id != 0L}
        .map {
            if (currentPage == 5) {
                val country = dataStoreRepository.country.firstOrNull()
                val data = Data.Builder()
                    .putString("discord_id", it)
                    .putString("country", country.orEmpty())
                    .build()
                firebaseRepository.log("Launch bg task", "SignupViewModel").firstOrNull()
                workerRepository.launchBackgroundTask(
                    FindPlayerWorker::class.java,
                    "findPlayer",
                    data
                )
            }
            _state.value.copy(currentPage = currentPage, launched = true)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun requestNotifications() {
        if (!notificationRepository.requestAuthorization) _state.value =
            _state.value.copy(currentPage = 3)
        else _showNotif.emit(Unit, viewModelScope)
    }

    fun onCountrySelected(country: String) {
        viewModelScope.launch {
            dataStoreRepository.setCountry(country)
        }
    }


    fun onRetry() {
        viewModelScope.launch {
            dataStoreRepository.setAccessToken("")
            dataStoreRepository.setCountry("")
            dataStoreRepository.setPage(3)
        }
    }
}