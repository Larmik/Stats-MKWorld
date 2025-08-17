package fr.harmoniamk.statsmkworld.screen.stats.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import javax.inject.Inject

@HiltViewModel
class StatsMenuViewModel @Inject constructor(dataStoreRepositoryInterface: DataStoreRepositoryInterface): ViewModel() {

    data class State(
        val currentTeamId: String? = null,
        val currentPlayerId: String? = null
    )

    val state = dataStoreRepositoryInterface.mkcPlayer.zip(dataStoreRepositoryInterface.mkcTeam) { player, team ->
        State(currentTeamId = team.id.toString(), currentPlayerId = player.id.toString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}