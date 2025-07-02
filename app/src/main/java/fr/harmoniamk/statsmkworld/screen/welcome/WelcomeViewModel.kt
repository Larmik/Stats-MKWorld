package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(dataStoreRepository: DataStoreRepositoryInterface, firebaseRepository: FirebaseRepositoryInterface, databaseRepository: DatabaseRepositoryInterface) : ViewModel() {

    data class State(
        val teamName: String? = null,
        val teamLogo: String? = null,
        val playerName: String? = null,
        val playerLogo: String? = null,
        val buttonVisible: Boolean = false,
        val currentWar: War? = null,
        var wars: List<WarDetails> = listOf()
    )

    val state = dataStoreRepository.mkcPlayer
        .mapNotNull { player ->
            dataStoreRepository.mkcTeam.firstOrNull()?.let { team ->
                val buttonVisible = firebaseRepository
                    .getUser(team.id.toString(), player.id.toString())
                    .map { it?.role ?: 0 }
                    .map { it > 0 }
                    .firstOrNull()

                val wars = databaseRepository.getWars()
                    .firstOrNull()
                    ?.map { War(it) }
                    ?.map { WarDetails(it) }
                    ?.sortedByDescending { it.war.id }
                    ?.safeSubList(0, 5)
                    .orEmpty()
                State(
                    teamName = team.name,
                    teamLogo = team.logo?.let { "https://mkcentral.com$it" },
                    playerName = player.name,
                    playerLogo = player.userSettings?.avatar?.let { "https://mkcentral.com$it" },
                    buttonVisible = buttonVisible == true,
                    currentWar = firebaseRepository.getCurrentWar(team.id.toString()).firstOrNull(),
                    wars = wars
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

}
