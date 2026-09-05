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
        val teamName: String? = null,
        val teamLogo: String? = null,
        // Couleur d'équipe (ARGB, source MKCentral) : fond de la pastille joueur.
        val teamColor: Long? = null,
        val playerName: String? = null,
        val playerLogo: String? = null,
        val currentWar: War? = null,
        // Les deux vues de stats 12p sont calculées une seule fois ; le segmenté
        // Moi/Équipe du dashboard choisit celle affichée (pas de recalcul au switch).
        val playerStats: Stats? = null,
        val teamStats: Stats? = null,
        // 3 dernières wars 12p (résultats récents → WarDetails). Réutilise WarCell.
        val recentResults: List<WarDetails> = listOf(),
        // Filtre par saison (#70) : liste des saisons + saison sélectionnée (null = tout
        // l'historique, défaut = saison en cours). Filtre TOUS les agrégats du dashboard
        // (momentum, séries, records, chiffres clés, derniers résultats).
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
                // Saisons (cache Room) observées en Flow réactif (#73) : le dropdown apparaît
                // dès que l'hydratation eager (InitStatsWorker/Signup) écrit les saisons, sans
                // redémarrage. Liste pour le dropdown + résolution de la saison effective
                // (défaut = saison en cours ; null = tout l'historique).
                val activeSeason = when (seasonFilter) {
                    is SeasonFilter.AllTime -> null
                    is SeasonFilter.Specific -> seasons.firstOrNull { it.number == seasonFilter.number }
                    is SeasonFilter.Default -> seasons.lastOrNull { it.end == null }
                }
                // Dashboard 12p uniquement (le support 24p relève de tickets dédiés) : wars à un
                // seul adversaire. Filtre par SAISON (#70) appliqué en premier ⇒ momentum, séries,
                // records, chiffres clés et derniers résultats reflètent la saison choisie (ou
                // tout l'historique en mode « Tout l'historique »).
                val wars = databaseRepository.getWars()
                    .firstOrNull()
                    ?.filterBySeason(activeSeason)
                    ?.filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
                    ?.map { War(it) }
                    ?.map { WarDetails(it) }
                    ?.filter { it.war.teamOpponent.size == 1 }
                    ?.sortedByDescending { it.war.id }
                    .orEmpty()

                // Firebase (potentiellement main-affine) résolu sur le collecteur, HORS du
                // `withContext(Default)` — cf. rule 21 (#73).
                val currentWar = firebaseRepository.getCurrentWar(rosterId.orEmpty())
                // SEULE la partie CPU-lourde (agrégats `withFullStats` équipe + joueur) est
                // déportée sur `Dispatchers.Default` via `withContext` (et NON `flowOn`, qui,
                // sur une chaîne passant par `mergeWith`/`flattenMerge`, laissait gagner l'état
                // vide → dropdown de saison disparu). `seasons` reste peuplé (calculé au-dessus).
                val (teamStats, playerStats) = withContext(Dispatchers.Default) {
                    val teamStats = wars.takeIf { it.isNotEmpty() }
                        ?.withFullStats(databaseRepository, is24p = false)
                        ?.firstOrNull()
                    val playerStats = wars.takeIf { it.isNotEmpty() }
                        ?.withFullStats(databaseRepository, userId = player.id.toString(), is24p = false)
                        ?.firstOrNull()
                    teamStats to playerStats
                }

                State(
                    teamName = team.name,
                    teamLogo = team.logo?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    teamColor = team.color.takeIf { it != 0L },
                    playerName = player.name,
                    playerLogo = player.userSettings?.avatar?.takeIf { it.isNotEmpty() }?.let { "https://mkcentral.com$it" },
                    currentWar = currentWar,
                    // Vue équipe (userId = null) et vue joueur (userId = id MKCentral
                    // du joueur courant) calculées d'emblée, sur les wars de la saison.
                    teamStats = teamStats,
                    playerStats = playerStats,
                    recentResults = wars.safeSubList(0, 3),
                    seasons = seasons,
                    selectedSeasonNumber = activeSeason?.number
                )
            }
        }
        .mapNotNull { it }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    /** Sélection de saison depuis l'UI : `number` null = tout l'historique. */
    fun onSeasonSelected(number: Int?) {
        _seasonFilter.value = number?.let { SeasonFilter.Specific(it) } ?: SeasonFilter.AllTime
    }

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
