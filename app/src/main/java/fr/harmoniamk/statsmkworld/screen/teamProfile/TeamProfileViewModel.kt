package fr.harmoniamk.statsmkworld.screen.teamProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.datasource.network.MKCentralDataSourceInterface
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = TeamProfileViewModel.Factory::class)
class TeamProfileViewModel @AssistedInject constructor(@Assisted val id: String, dataStoreRepository: DataStoreRepositoryInterface, mkCentralDataSource: MKCentralDataSourceInterface, databaseRepository: DatabaseRepositoryInterface) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): TeamProfileViewModel
    }

    data class State(
        val team: MKCTeam? = null,
        val allyList: List<PlayerEntity> = listOf()
    )

    val state =  when (id) {
        "me" -> dataStoreRepository.mkcTeam
        else -> mkCentralDataSource.getTeam(id)
    }
        .map {
            val allyList = when (id) {
                "me" -> databaseRepository.getPlayers().firstOrNull()?.filter { it.isAlly }.orEmpty()
                else -> listOf()
            }
            State(team = it, allyList = allyList)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())
}
