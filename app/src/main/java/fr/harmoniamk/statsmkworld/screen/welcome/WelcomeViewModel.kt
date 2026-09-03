package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepositoryInterface,
    firebaseRepository: FirebaseRepositoryInterface,
    databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    data class State(
        val teamName: String? = null,
        val teamLogo: String? = null,
        // Couleur d'équipe (ARGB, source MKCentral) : fond de la pastille joueur.
        val teamColor: Long? = null,
        val playerName: String? = null,
        val playerLogo: String? = null,
        // Numéro de la saison en cours (#70) : affiché sur le dashboard Accueil.
        // Null tant que la table `seasons` n'est pas hydratée (aucune saison en cours).
        val currentSeasonNumber: Int? = null,
        val currentWar: War? = null,
        // Les deux vues de stats 12p sont calculées une seule fois ; le segmenté
        // Moi/Équipe du dashboard choisit celle affichée (pas de recalcul au switch).
        val playerStats: Stats? = null,
        val teamStats: Stats? = null,
        // 3 dernières wars 12p (résultats récents → WarDetails). Réutilise WarCell.
        val recentResults: List<WarDetails> = listOf()
    )

    private val _state = MutableStateFlow(State())

    val state = dataStoreRepository.mkcPlayer
        .mapNotNull { player ->
            val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
            val rosterId = player.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()
            dataStoreRepository.mkcTeam.firstOrNull()?.let { team ->
                // Dashboard 12p uniquement (le support 24p relève de tickets dédiés) :
                // on ne garde que les wars à un seul adversaire.
                val wars = databaseRepository.getWars()
                    .firstOrNull()
                    ?.filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
                    ?.map { War(it) }
                    ?.map { WarDetails(it) }
                    ?.filter { it.war.teamOpponent.size == 1 }
                    ?.sortedByDescending { it.war.id }
                    .orEmpty()

                State(
                    teamName = team.name,
                    teamLogo = team.logo?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    teamColor = team.color.takeIf { it != 0L },
                    playerName = player.name,
                    playerLogo = player.userSettings?.avatar?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    // Saison en cours (#70) : celle sans date de fin (end == null), cache Room.
                    currentSeasonNumber = databaseRepository.getCurrentSeason()?.number,
                    currentWar = firebaseRepository.getCurrentWar(rosterId.orEmpty()),
                    // Vue équipe (userId = null) et vue joueur (userId = id MKCentral
                    // du joueur courant) calculées d'emblée.
                    teamStats = wars.takeIf { it.isNotEmpty() }
                        ?.withFullStats(databaseRepository, is24p = false)
                        ?.firstOrNull(),
                    playerStats = wars.takeIf { it.isNotEmpty() }
                        ?.withFullStats(databaseRepository, userId = player.id.toString(), is24p = false)
                        ?.firstOrNull(),
                    recentResults = wars.safeSubList(0, 3)
                )
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    init {
        dataStoreRepository.mkcPlayer
            .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
            .flatMapLatest { firebaseRepository.listenToCurrentWar(it) }
            .onEach {
                // Réhydrate le DataStore du créateur si celui-ci est vide, pour
                // qu'il retrouve ses droits d'édition sur la war courante.
                firebaseRepository.restoreCurrentWarIfHost(it)
                _state.value = state.value.copy(currentWar = it)
            }
            .launchIn(viewModelScope)
    }

}
