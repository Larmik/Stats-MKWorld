package fr.harmoniamk.statsmkworld.screen.registry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class RegistryViewModel @Inject constructor(private val mkCentralDataSource: MKCentralDataSourceInterface, databaseRepository: DatabaseRepositoryInterface) : ViewModel(), CoroutineScope {

    data class State(
        val playerList: List<MKCPlayer> = listOf(),
        val teamList: List<TeamEntity> = listOf()
    )

    private val _state = MutableStateFlow(State())
    private var teams = listOf<TeamEntity>()

    val state = databaseRepository.getTeams()
        .onEach { this.teams = it }
        .map { State(teamList = it) }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private fun searchPlayers(page: Int, term: String) = mkCentralDataSource.searchPlayers(page, term)
        .map { Pair(it?.pageCount, it?.playerList) }
        .shareIn(this, SharingStarted.Eagerly)

    fun onSearchPlayers(term: String) {
        if (term.length >= 3)
            viewModelScope.launch {
                var page = 1
                val playerList = mutableListOf<MKCPlayer>()
                var player: Pair<Int?, List<MKCPlayer>?>? = searchPlayers(page, term).firstOrNull()

                playerList.addAll(player?.second.orEmpty())
                while (page < (player?.first ?: 0)) {
                    page++
                    player = searchPlayers(page, term).firstOrNull()
                    playerList.addAll(player?.second.orEmpty())
                }
                _state.value = _state.value.copy(playerList = playerList)
            }
        else _state.value = _state.value.copy(playerList = listOf())
    }

    fun onSearchTeams(term: String) {
       val filteredList = when (term.isEmpty()) {
           true -> teams
           else -> teams.filter { it.name.lowercase().contains(term.lowercase()) || it.tag.lowercase().contains(term.lowercase()) }
       }
        _state.value = state.value.copy(teamList = filteredList)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

}
