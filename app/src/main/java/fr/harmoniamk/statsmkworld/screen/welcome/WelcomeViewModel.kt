package fr.harmoniamk.statsmkworld.screen.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.extension.filterBySeason
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.FirebaseRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepositoryInterface,
    firebaseRepository: FirebaseRepositoryInterface,
    databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    data class State(
        // true au 1er chargement ET au changement de saison (#73), pour un ressenti immédiat
        // pendant le compute off-main ; le compute émet ensuite `loading = false`.
        val loading: Boolean = true,
        val teamName: String? = null,
        val teamLogo: String? = null,
        val teamColor: Long? = null,
        val playerName: String? = null,
        val playerLogo: String? = null,
        val currentWar: War? = null,
        // Deux vues de stats 12p calculées d'emblée ; le segmenté Moi/Équipe choisit sans recalcul.
        val playerStats: Stats? = null,
        val teamStats: Stats? = null,
        // 3 dernières wars 12p (résultats récents).
        val recentResults: List<WarDetails> = listOf(),
        // Filtre par saison (#70) : liste + sélection (null = tout, défaut = saison en cours).
        val seasons: List<SeasonEntity> = listOf(),
        val selectedSeasonNumber: Int? = null
    )

    /**
     * Sélection de saison (#70), même modèle que Stats/Classements/Wars. [Default] = saison
     * en cours (résolue après chargement) ; [AllTime] = tout l'historique ; [Specific] = passée.
     */
    sealed interface SeasonFilter {
        data object Default : SeasonFilter
        data object AllTime : SeasonFilter
        data class Specific(val number: Int) : SeasonFilter
    }

    // Sélection de saison courante (#70) : recompute déclenché à chaque changement via combine.
    private val _seasonFilter = MutableStateFlow<SeasonFilter>(SeasonFilter.Default)

    private val _state = MutableStateFlow(State())

    val state = combine(dataStoreRepository.mkcPlayer, _seasonFilter, databaseRepository.getSeasons()) { player, seasonFilter, seasons ->
            val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
            val rosterId = player.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()
            dataStoreRepository.mkcTeam.firstOrNull()?.let { team ->
                // Saisons observées en Flow réactif (#73) ; résolution de la saison effective
                // (défaut = saison en cours ; null = tout l'historique).
                val activeSeason = when (seasonFilter) {
                    is SeasonFilter.AllTime -> null
                    is SeasonFilter.Specific -> seasons.firstOrNull { it.number == seasonFilter.number }
                    is SeasonFilter.Default -> seasons.lastOrNull { it.end == null }
                }
                // Firebase résolu sur le collecteur, HORS du `withContext(Default)` (rule 21, #73).
                val currentWar = firebaseRepository.getCurrentWar(rosterId.orEmpty())
                // Toute la partie CPU-lourde (construction de `wars`, `withFullStats`,
                // `recentResults`) déportée sur `Dispatchers.Default` via `withContext` (pas
                // `flowOn` — rule 21, #73) ; métadonnées et `seasons` restent sur le collecteur.
                val (teamStats, playerStats, recentResults) = withContext(Dispatchers.Default) {
                    // Dashboard 12p uniquement, filtre par saison (#70) appliqué en premier.
                    val wars = databaseRepository.getWars()
                        .firstOrNull()
                        ?.filterBySeason(activeSeason)
                        ?.filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
                        ?.map { War(it) }
                        ?.map { WarDetails(it) }
                        ?.filter { it.war.teamOpponent.size == 1 }
                        ?.sortedByDescending { it.war.id }
                        .orEmpty()
                    // Vues équipe (userId = null) et joueur calculées d'emblée sur les wars de la saison.
                    val teamStats = wars.takeIf { it.isNotEmpty() }
                        ?.withFullStats(databaseRepository, is24p = false)
                        ?.firstOrNull()
                    val playerStats = wars.takeIf { it.isNotEmpty() }
                        ?.withFullStats(databaseRepository, userId = player.id.toString(), is24p = false)
                        ?.firstOrNull()
                    Triple(teamStats, playerStats, wars.safeSubList(0, 3))
                }

                State(
                    loading = false,
                    teamName = team.name,
                    teamLogo = team.logo?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    teamColor = team.color.takeIf { it != 0L },
                    playerName = player.name,
                    playerLogo = player.userSettings?.avatar?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    currentWar = currentWar,
                    teamStats = teamStats,
                    playerStats = playerStats,
                    recentResults = recentResults,
                    seasons = seasons,
                    selectedSeasonNumber = activeSeason?.number
                )
            }
        }
        .mapNotNull { it }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    /** Sélection de saison (null = tout l'historique). Pose `loading` immédiatement (#73) ; le
     * `combine` émet ensuite `loading = false`. */
    fun onSeasonSelected(number: Int?) {
        _state.value = _state.value.copy(loading = true)
        _seasonFilter.value = number?.let { SeasonFilter.Specific(it) } ?: SeasonFilter.AllTime
    }

    init {
        dataStoreRepository.mkcPlayer
            .mapNotNull { it.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString() }
            .flatMapLatest { firebaseRepository.listenToCurrentWar(it) }
            .onEach {
                // Réhydrate le DataStore du créateur si vide (restaure ses droits d'édition).
                firebaseRepository.restoreCurrentWarIfHost(it)
                _state.value = state.value.copy(currentWar = it)
            }
            .launchIn(viewModelScope)
    }

}
