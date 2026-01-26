package fr.harmoniamk.statsmkworld.screen.stats.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsMenuViewModel @Inject constructor(private val dataStoreRepository: DataStoreRepositoryInterface): ViewModel() {

    data class State(
        val currentTeamId: String? = null,
        val currentPlayerId: String? = null,
        val is24PEnabled: Boolean? = null
    )

    private val _state = MutableSharedFlow<State>()

    val state = dataStoreRepository.mkcPlayer.zip(dataStoreRepository.mkcTeam) { player, team ->
        val is24p = dataStoreRepository.is24PEnabled.firstOrNull()
        State(currentTeamId = team.id.toString(), currentPlayerId = player.id.toString(), is24PEnabled = is24p)
    }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    fun onWarTypeSwitch(index: Int) {
        viewModelScope.launch {
            val is24p = index == 1
            dataStoreRepository.set24PEnabled(is24p)
            _state.emit(state.value.copy(is24PEnabled = is24p))
        }
    }
}