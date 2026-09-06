package fr.harmoniamk.statsmkworld.screen.stats.ranking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.filterBySeason
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.extension.withFullTeamStats
import fr.harmoniamk.statsmkworld.extension.withTrackStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Onglets du pôle Classements (#26) : Joueurs / Adversaires / Circuits. */
enum class RankingTab { PLAYERS, OPPONENTS, TRACKS }

/**
 * Critères de tri (ordre = ordre des chips, COUNT défaut, tri décroissant). COUNT =
 * Participation (Joueurs, #78) · Occurrences · Fréquence selon l'onglet.
 */
enum class SortType { COUNT, WINRATE, AVERAGE }

sealed interface RankingItem {

    /** Winrate en % (0 si aucune war jouée) — base du tri/insight winrate. */
    val winratePercent: Int

    /** Nombre de matchs de l'entrée (wars / confrontations / fois jouée) — base du seuil. */
    val sampleSize: Int

    /**
     * [participationRate] (#78) : % de wars de l'équipe où le joueur est présent
     * (`warsPlayed × 100 / total wars équipe`). Calculé dans le VM (rule 32), absent de [Stats].
     */
    class PlayerRanking(
        val player: PlayerEntity,
        val stats: Stats,
        val participationRate: Int
    ) : RankingItem {

        override val sampleSize: Int
            get() = stats.warStats.warsPlayed

        override val winratePercent: Int
            get() = when (stats.warStats.warsPlayed) {
                0 -> 0
                else -> (stats.warStats.warsWon * 100) / stats.warStats.warsPlayed
            }

        val averageLabel: String
            get() = stats.averagePoints.toString()

        val warsPlayedLabel: String
            get() = stats.warStats.warsPlayed.toString()

        val winrateLabel: String
            get() = "$winratePercent %"

        val participationRateLabel: String
            get() = "$participationRate %"
    }

    class OpponentRanking(val team: TeamEntity, val stats: Stats) : RankingItem {

        override val sampleSize: Int
            get() = stats.warStats.warsPlayed

        val averageLabel: String
            get() = stats.averagePointsLabel

        val warsPlayedLabel: String
            get() = stats.warStats.warsPlayed.toString()

        val winrate: Int
            get() = (stats.warStats.warsWon * 100) / stats.warStats.warsPlayed

        override val winratePercent: Int
            get() = when (stats.warStats.warsPlayed) {
                0 -> 0
                else -> winrate
            }

        val winrateLabel: String
            get() = "$winratePercent %"
    }

    class TrackRanking(val stats: TrackStats) : RankingItem {
        override val sampleSize: Int
            get() = stats.totalPlayed
        override val winratePercent: Int
            get() = stats.winRate ?: 0
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class StatsRankingViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Section de l'onglet Joueurs : membres vs alliés. Un allié a un `rosterId` non résolvable
     * parmi les rosters mkworld de l'équipe.
     */
    data class PlayerSection(val titleRes: Int, val players: List<RankingItem.PlayerRanking>)

    data class State(
        // Chargement de la zone de classements : true au 1er chargement et au changement de
        // saison (#73), le temps du recompute off-main. Interactions légères ne le touchent pas.
        val loading: Boolean = true,
        val tab: RankingTab = RankingTab.PLAYERS,
        val sort: SortType = SortType.COUNT,
        val search: String = "",
        // Onglet Joueurs : deux sections (Membres / Alliés), chacune triée.
        val playerSections: List<PlayerSection> = listOf(),
        // Onglets Adversaires / Circuits : entrées triées.
        val opponents: List<RankingItem.OpponentRanking> = listOf(),
        val tracks: List<RankingItem.TrackRanking> = listOf(),
        // Filtre « occurrences minimum » (slider) : min = 1, max = plus haut compteur de
        // l'onglet courant ; la liste ne montre que les entrées à sampleSize >= min.
        val minOccurrences: Int = 1,
        val maxOccurrences: Int = 1,
        val currentUserId: String? = null,
        val is24PEnabled: Boolean? = null,
        // Filtre saison (#70) : `selectedSeasonNumber` null = tout l'historique, défaut = saison
        // en cours. Rankings recalculés à la volée sur l'intervalle.
        val seasons: List<SeasonEntity> = listOf(),
        val selectedSeasonNumber: Int? = null
    )

    /**
     * Sélection de saison (#70), même modèle que StatsFullViewModel. [Default] = saison en
     * cours (résolue après chargement) ; [AllTime] = tout l'historique ; [Specific] = saison
     * passée précise.
     */
    sealed interface SeasonFilter {
        data object Default : SeasonFilter
        data object AllTime : SeasonFilter
        data class Specific(val number: Int) : SeasonFilter
    }

    // Sélection de saison (#70) : `combine` avec les wars → recompute à la volée.
    private val _seasonFilter = MutableStateFlow<SeasonFilter>(SeasonFilter.Default)

    private val _state = MutableStateFlow(State())
    private var currentUser: MKCPlayer? = null
    // Listes brutes mémorisées pour re-filtrer à chaque interaction (membres / alliés).
    private var allMembers: List<RankingItem.PlayerRanking> = listOf()
    private var allAllies: List<RankingItem.PlayerRanking> = listOf()
    private var allOpponents: List<RankingItem.OpponentRanking> = listOf()
    private var allTracks: List<RankingItem.TrackRanking> = listOf()
    // Saisons + sélection (#70) mémorisées pour ré-injection à chaque recompute() : sinon une
    // interaction émet un `_state` aux `seasons` vides → le dropdown disparaîtrait.
    private var loadedSeasons: List<SeasonEntity> = listOf()
    private var loadedSelectedSeasonNumber: Int? = null

    val state = combine(databaseRepository.getWars(), _seasonFilter, databaseRepository.getSeasons()) { warEntities, seasonFilter, seasons ->
            // Saisons observées en Flow (#73) : le dropdown apparaît dès l'hydratation eager.
            currentUser = dataStoreRepository.mkcPlayer.firstOrNull()
            val is24p = dataStoreRepository.is24PEnabled.firstOrNull() == true
            val activeSeason = when (seasonFilter) {
                is SeasonFilter.AllTime -> null
                is SeasonFilter.Specific -> seasons.firstOrNull { it.number == seasonFilter.number }
                is SeasonFilter.Default -> seasons.lastOrNull { it.end == null }
            }
            loadedSeasons = seasons
            loadedSelectedSeasonNumber = activeSeason?.number
            // Rankings recalculés à la volée sur les wars filtrées par saison (remplace le cache
            // all-time de StatsRepository). Filtres alignés sur InitStatsWorker : host/roster + 12p/24p.
            computeRankings(warEntities.filterBySeason(activeSeason), is24p)
            _state.value.copy(
                loading = false,
                currentUserId = currentUser?.id.toString(),
                is24PEnabled = is24p
            ).recompute(resetOccurrences = true)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /**
     * Recalcule les listes brutes (membres/alliés/adversaires/circuits) sur les [wars] filtrées
     * par saison. Réplique `InitStatsWorker` (rule 32) : filtre host/roster + 12p/24p.
     */
    private suspend fun computeRankings(warEntities: List<WarEntity>, is24p: Boolean) = withContext(Dispatchers.Default) {
        val currentPlayer = currentUser
        val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
        val rosterId = currentPlayer?.rosters?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()
        val currentTeam = dataStoreRepository.mkcTeam.firstOrNull()
        val rosters = currentTeam?.rosters

        val warList = warEntities
            .filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
            .filter { (is24p && it.teamOpponent.size > 1) || (!is24p && it.teamOpponent.size == 1) }
        val warDetailsList = warList.map { WarDetails(War(it)) }

        // Circuits (équipe).
        allTracks = warList.withTrackStats().map { RankingItem.TrackRanking(it) }

        // Joueurs : groupés membre (rattaché à un roster mkworld) / allié (Pair(1,"Allies")).
        val userList = databaseRepository.getPlayers().firstOrNull().orEmpty().sortedBy { it.name }
        // Dénominateur du taux de participation (#78) = wars de l'équipe ; garde-fou nul → 0 %.
        val teamWarsCount = warDetailsList.size
        val playersByGroup = userList
            .mapNotNull { user ->
                warDetailsList
                    .filter { it.war.hasPlayer(user.id) }
                    .withFullStats(databaseRepository, userId = user.id, is24p = is24p)
                    .firstOrNull()
                    ?.takeIf { it.warStats.warsPlayed > 0 }
                    ?.let { stats ->
                        val participationRate = when (teamWarsCount) {
                            0 -> 0
                            else -> stats.warStats.warsPlayed * 100 / teamWarsCount
                        }
                        RankingItem.PlayerRanking(user, stats, participationRate)
                    }
            }
            .groupBy { ranking ->
                when (rosters?.firstOrNull { it.id.toString() == ranking.player.rosterId }) {
                    null -> 1 // allié
                    else -> 0 // membre
                }
            }
        allMembers = playersByGroup[0].orEmpty()
        allAllies = playersByGroup[1].orEmpty()

        // Adversaires (perspective équipe, comme le prototype : pas de switch indiv/équipe).
        val teams = databaseRepository.getTeams().firstOrNull().orEmpty()
            .filterNot { it.id == currentTeam?.id.toString() }
            .sortedBy { it.name }
        allOpponents = teams
            .withFullTeamStats(wars = warList, databaseRepository = databaseRepository, is24p = is24p)
            .firstOrNull()
            .orEmpty()
            .sortedByDescending { it.second.warStats.warsPlayed }
            .map { RankingItem.OpponentRanking(it.first, it.second) }
    }

    /** Sélection de saison (`number` null = tout l'historique). Pose `loading` via `recompute()`
     * pendant le recompute off-main (#73) ; la branche `combine` émet ensuite `loading = false`. */
    fun onSeasonSelected(number: Int?) {
        _state.value = _state.value.copy(loading = true).recompute()
        _seasonFilter.value = number?.let { SeasonFilter.Specific(it) } ?: SeasonFilter.AllTime
    }

    // Interactions légères (onglet/tri/recherche/curseur) : re-filtrage instantané → posent
    // explicitement `loading = false` (la branche `combine` n'écrit jamais dans `_state`, qui
    // resterait sinon à `true` et masquerait la liste, #73). Seul `onSeasonSelected` pose `true`.
    fun onTabSelected(index: Int) {
        val tab = RankingTab.entries.getOrElse(index) { RankingTab.PLAYERS }
        // Nouvel onglet : tri par défaut, recherche vide, curseur réinitialisé.
        _state.value = _state.value.copy(loading = false, tab = tab, sort = SortType.COUNT, search = "")
            .recompute(resetOccurrences = true)
    }

    fun onSortSelected(index: Int) {
        val sort = SortType.entries.getOrElse(index) { SortType.COUNT }
        _state.value = _state.value.copy(loading = false, sort = sort).recompute()
    }

    fun onSearch(search: String) {
        _state.value = _state.value.copy(loading = false, search = search).recompute()
    }

    /** Valeur du curseur « occurrences minimum » (bornée [1, maxOccurrences]). */
    fun onMinOccurrencesChange(value: Int) {
        _state.value = _state.value.copy(
            loading = false,
            minOccurrences = value.coerceIn(1, _state.value.maxOccurrences)
        ).recompute()
    }

    /**
     * Recherche + filtre « occurrences min » + tri sur les listes brutes de l'onglet courant.
     * [resetOccurrences] recalcule le max du curseur et remet le min à 1 (changement d'onglet / (re)chargement).
     */
    private fun State.recompute(resetOccurrences: Boolean = false): State {
        val query = search.trim().lowercase()
        // Compteur max de l'onglet (borne haute du curseur), sur les données non filtrées.
        val newMax = when (tab) {
            RankingTab.PLAYERS -> (allMembers + allAllies)
            RankingTab.OPPONENTS -> allOpponents
            RankingTab.TRACKS -> allTracks
        }.maxOfOrNull { it.sampleSize }?.coerceAtLeast(1) ?: 1
        val newMin = if (resetOccurrences) 1 else minOccurrences.coerceIn(1, newMax)

        // Saisons + sélection ré-injectées à chaque recompute → dropdown toujours renseigné.
        val base = copy(seasons = loadedSeasons, selectedSeasonNumber = loadedSelectedSeasonNumber)
        return when (tab) {
            RankingTab.PLAYERS -> {
                val sections = listOf(
                    PlayerSection(R.string.rankings_section_members,
                        allMembers.finalize(sort, query, newMin, { it.player.name }, { it.stats.averagePoints }, { it.participationRate })),
                    PlayerSection(R.string.rankings_section_allies,
                        allAllies.finalize(sort, query, newMin, { it.player.name }, { it.stats.averagePoints }, { it.participationRate }))
                ).filter { it.players.isNotEmpty() }
                base.copy(playerSections = sections, opponents = listOf(), tracks = listOf(),
                    minOccurrences = newMin, maxOccurrences = newMax)
            }

            RankingTab.OPPONENTS -> base.copy(
                playerSections = listOf(),
                opponents = allOpponents.finalize(sort, query, newMin, { it.team.name }, { it.stats.averagePoints }),
                tracks = listOf(),
                minOccurrences = newMin, maxOccurrences = newMax
            )

            RankingTab.TRACKS -> base.copy(
                playerSections = listOf(),
                opponents = listOf(),
                tracks = allTracks.finalize(sort, query, newMin, { it.trackName() }, { it.stats.teamScore ?: 0 }),
                minOccurrences = newMin, maxOccurrences = newMax
            )
        }
    }

    /**
     * Recherche (par [name]) + filtre occurrences (`sampleSize >= min`) + tri [sort]. [count] =
     * valeur du tri COUNT (défaut `sampleSize` ; Joueurs = taux de participation #78).
     */
    private fun <T : RankingItem> List<T>.finalize(
        sort: SortType,
        query: String,
        min: Int,
        name: (T) -> String,
        average: (T) -> Int,
        count: (T) -> Int = { it.sampleSize }
    ): List<T> = this
        .filter { query.isEmpty() || name(it).lowercase().contains(query) }
        .filter { it.sampleSize >= min }
        .let { list ->
            when (sort) {
                SortType.WINRATE -> list.sortedByDescending { it.winratePercent }
                SortType.AVERAGE -> list.sortedByDescending(average)
                SortType.COUNT -> list.sortedByDescending(count)
            }
        }

    private fun RankingItem.TrackRanking.trackName(): String =
        stats.map?.joinToString { context.getString(it.label) }.orEmpty()
}
