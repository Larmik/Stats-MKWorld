package fr.harmoniamk.statsmkworld.screen.warList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.extension.format
import fr.harmoniamk.statsmkworld.extension.get
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WarListViewModel @Inject constructor(
    firebaseRepository: FirebaseRepositoryInterface,
    databaseRepository: DatabaseRepositoryInterface,
    dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    data class State(
        // Historique groupé par mois (sticky headers) — TOUS les modes (12j ET 24j).
        val wars: List<Pair<String, List<WarDetails>>> = listOf(),
        // Nombre total de wars affichées (sous-titre « N wars »).
        val warCount: Int = 0,
        // War en cours (bannière « En direct ») ; null → CTA « Nouvelle war ».
        val currentWar: War? = null
    )

    private val _state = MutableStateFlow(State())

    val state = dataStoreRepository.mkcPlayer
        .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
        .zip(databaseRepository.getWars()) { rosterId, wars ->
            val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
            val currentWar = firebaseRepository.getCurrentWar(rosterId)
            // Aucun filtre par mode (12/24) : l'historique mélange tous les modes,
            // conformément à la maquette. Seul le filtre par roster hôte demeure.
            val details = wars
                .filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
                .map { War(it) }
                .map { WarDetails(it) }
                .sortedByDescending { it.war.id }
            val grouped = details
                .groupBy { war ->
                    val date = Date(war.war.id)
                    val month = date.get(Calendar.MONTH)
                    val year = date.get(Calendar.YEAR)
                    month.toString() + year.toString()
                }.mapNotNull {
                    it.value.firstOrNull()?.war?.id?.let { id ->
                        val date = Date(id)
                        Pair(date.format("MMMM yyyy"), it.value)
                    }
                }
            State(wars = grouped, warCount = details.size, currentWar = currentWar)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    init {
        dataStoreRepository.mkcPlayer
            .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
            .flatMapLatest { firebaseRepository.listenToCurrentWar(it) }
            .onEach { _state.value = state.value.copy(currentWar = it) }
            .launchIn(viewModelScope)
    }

}
