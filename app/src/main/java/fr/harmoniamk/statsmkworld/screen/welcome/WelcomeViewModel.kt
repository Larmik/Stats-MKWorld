package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.StatsType
import fr.harmoniamk.statsmkworld.screen.stats.ranking.SortType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Int

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WelcomeViewModel @Inject constructor(private val dataStoreRepository: DataStoreRepositoryInterface, firebaseRepository: FirebaseRepositoryInterface, databaseRepository: DatabaseRepositoryInterface) : ViewModel() {

    data class State(
        val teamName: String? = null,
        val teamLogo: String? = null,
        val playerName: String? = null,
        val playerLogo: String? = null,
        val buttonVisible: Boolean = false,
        val currentWar: War? = null,
        var wars: List<WarDetails> = listOf(),
        val is24PEnabled: Boolean = false
    )

    private var wars: List<WarDetails> = listOf()

    init {
        dataStoreRepository.mkcPlayer
            .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
            .flatMapLatest { firebaseRepository.listenToCurrentWar(it) }
            .onEach { _state.value = state.value.copy(currentWar = it) }
            .launchIn(viewModelScope)

        dataStoreRepository.is24PEnabled
            .onEach { is24p -> _state.value = state.value.copy(
                wars = wars.filter {
                    (is24p && it.war.teamOpponent.size > 1)
                            || (!is24p && it.war.teamOpponent.size == 1)
                }.safeSubList(0, 5),
                is24PEnabled = is24p
            ) }
            .launchIn(viewModelScope)
    }

    private val _state = MutableStateFlow(State())

    val state = dataStoreRepository.mkcPlayer
        .mapNotNull { player ->
            val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
            val rosterId = player.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()
            val is24PEnabled = dataStoreRepository.is24PEnabled.firstOrNull() == true
            dataStoreRepository.mkcTeam.firstOrNull()?.let { team ->
                val buttonVisible = (firebaseRepository
                    .getUser(team.id.toString(), player.id.toString())
                    ?.role ?: 0) > 0

                val wars = databaseRepository.getWars()
                    .firstOrNull()
                    ?.filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
                    ?.map { War(it) }
                    ?.map { WarDetails(it) }
                    ?.sortedByDescending { it.war.id }
                    .orEmpty()

                val rosterId = player.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID.toString()
                this.wars = wars

                State(
                    teamName = team.name,
                    teamLogo = team.logo?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    playerName = player.name,
                    playerLogo = player.userSettings?.avatar?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    buttonVisible =  buttonVisible || dataStoreRepository.matrixMode.firstOrNull() == true,
                    currentWar = firebaseRepository.getCurrentWar(rosterId),
                    is24PEnabled = is24PEnabled,
                    wars = wars
                        .filter { (!is24PEnabled && it.war.teamOpponent.size == 1) || is24PEnabled && it.war.teamOpponent.size > 1 }
                        .safeSubList(0, 5)

                )
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    fun onWarTypeSwitch(index: Int) {
        viewModelScope.launch {
            val is24p = index == 1
            dataStoreRepository.set24PEnabled(is24p)
            _state.value = state.value.copy(
                wars = wars.filter {
                    (is24p && it.war.teamOpponent.size > 1)
                            || (!is24p && it.war.teamOpponent.size == 1)
                }.safeSubList(0, 5),
                is24PEnabled = is24p
            )
        }

    }

}
