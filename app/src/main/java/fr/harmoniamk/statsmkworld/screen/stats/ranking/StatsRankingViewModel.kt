package fr.harmoniamk.statsmkworld.screen.stats.ranking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCPlayer
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.StatsRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Onglets du pôle Classements (#26) : Joueurs / Adversaires / Circuits, présentés
 * directement sur l'écran (plus de menu intermédiaire). L'ordre = ordre du prototype.
 */
enum class RankingTab { PLAYERS, OPPONENTS, TRACKS }

/**
 * Critères de tri du prototype : Winrate (défaut) / Score moyen / compteur (Wars ·
 * Occurrences · Fréquence selon l'onglet). L'ancien `SortType.NAME` (chip « Nom »)
 * n'apparaît pas dans le prototype → il n'est plus proposé comme chip ; le tri par
 * défaut est WINRATE décroissant.
 */
enum class SortType { WINRATE, AVERAGE, COUNT }

sealed interface RankingItem {

    /** Winrate en % (0 si aucune war jouée) — base du tri/insight winrate. */
    val winratePercent: Int

    /** Nombre de matchs de l'entrée (wars / confrontations / fois jouée) — base du seuil. */
    val sampleSize: Int

    class PlayerRanking(val player: PlayerEntity, val stats: Stats) : RankingItem {

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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsRankingViewModel @Inject constructor(
    dataStoreRepository: DataStoreRepositoryInterface,
    private val statsRepository: StatsRepositoryInterface,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /**
     * Section de l'onglet Joueurs : membres de l'équipe vs alliés (deux en-têtes). Un
     * allié a `rosterId` non résolvable parmi les rosters mkworld de l'équipe (dans le
     * cache `playersRankList`, clé `Pair(1, "Allies")`) ; un membre = `Pair(0, roster)`.
     */
    data class PlayerSection(val titleRes: Int, val players: List<RankingItem.PlayerRanking>)

    data class State(
        val tab: RankingTab = RankingTab.PLAYERS,
        val sort: SortType = SortType.WINRATE,
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
        val is24PEnabled: Boolean? = null
    )

    private val _state = MutableStateFlow(State())
    private var currentUser: MKCPlayer? = null
    // Listes brutes (non filtrées/triées) mémorisées pour re-filtrer à chaque interaction.
    // Joueurs : conservés PAR SECTION (membre = clé Pair(0,…), allié = Pair(1,"Allies")).
    private var allMembers: List<RankingItem.PlayerRanking> = listOf()
    private var allAllies: List<RankingItem.PlayerRanking> = listOf()
    private var allOpponents: List<RankingItem.OpponentRanking> = listOf()
    private var allTracks: List<RankingItem.TrackRanking> = listOf()

    val state = flowOf(Unit)
        .map {
            currentUser = dataStoreRepository.mkcPlayer.firstOrNull()
            val is24p = dataStoreRepository.is24PEnabled.firstOrNull()
            val playersByGroup = statsRepository.playersRankList
                .mapValues { entry -> entry.value.filter { it.stats.warStats.warsPlayed > 0 } }
            // Clé Pair.first : 0 = membre (rattaché à un roster mkworld), 1 = allié.
            allMembers = playersByGroup.filterKeys { it.first == 0 }.values.flatten()
            allAllies = playersByGroup.filterKeys { it.first == 1 }.values.flatten()
            // Perspective ÉQUIPE pour adversaires/circuits (winrate global de l'équipe),
            // conforme au prototype qui n'a pas de switch indiv/équipe.
            allOpponents = statsRepository.opponentRankList.mapNotNull { it as? RankingItem.OpponentRanking }
            allTracks = statsRepository.trackRankList.mapNotNull { it as? RankingItem.TrackRanking }
            _state.value.copy(
                currentUserId = currentUser?.id.toString(),
                is24PEnabled = is24p
            ).recompute(resetOccurrences = true)
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onTabSelected(index: Int) {
        val tab = RankingTab.entries.getOrElse(index) { RankingTab.PLAYERS }
        // Nouvel onglet : tri par défaut (Winrate), recherche vide, curseur réinitialisé.
        _state.value = _state.value.copy(tab = tab, sort = SortType.WINRATE, search = "")
            .recompute(resetOccurrences = true)
    }

    fun onSortSelected(index: Int) {
        val sort = SortType.entries.getOrElse(index) { SortType.WINRATE }
        _state.value = _state.value.copy(sort = sort).recompute()
    }

    fun onSearch(search: String) {
        _state.value = _state.value.copy(search = search).recompute()
    }

    /** Valeur du curseur « occurrences minimum » (bornée [1, maxOccurrences]). */
    fun onMinOccurrencesChange(value: Int) {
        _state.value = _state.value.copy(
            minOccurrences = value.coerceIn(1, _state.value.maxOccurrences)
        ).recompute()
    }

    /**
     * Applique recherche + filtre « occurrences min » + tri sur les listes brutes de
     * l'onglet courant. [resetOccurrences] recalcule le max du curseur (plus haut
     * compteur de l'onglet) et remet le min à 1 (changement d'onglet / (re)chargement).
     */
    private fun State.recompute(resetOccurrences: Boolean = false): State {
        val query = search.trim().lowercase()
        // Compteur max de l'onglet (borne haute du curseur) — sur les données non filtrées.
        val newMax = when (tab) {
            RankingTab.PLAYERS -> (allMembers + allAllies)
            RankingTab.OPPONENTS -> allOpponents
            RankingTab.TRACKS -> allTracks
        }.maxOfOrNull { it.sampleSize }?.coerceAtLeast(1) ?: 1
        val newMin = if (resetOccurrences) 1 else minOccurrences.coerceIn(1, newMax)

        return when (tab) {
            RankingTab.PLAYERS -> {
                val sections = listOf(
                    PlayerSection(R.string.rankings_section_members,
                        allMembers.finalize(sort, query, newMin, { it.player.name }, { it.stats.averagePoints })),
                    PlayerSection(R.string.rankings_section_allies,
                        allAllies.finalize(sort, query, newMin, { it.player.name }, { it.stats.averagePoints }))
                ).filter { it.players.isNotEmpty() }
                copy(playerSections = sections, opponents = listOf(), tracks = listOf(),
                    minOccurrences = newMin, maxOccurrences = newMax)
            }

            RankingTab.OPPONENTS -> copy(
                playerSections = listOf(),
                opponents = allOpponents.finalize(sort, query, newMin, { it.team.name }, { it.stats.averagePoints }),
                tracks = listOf(),
                minOccurrences = newMin, maxOccurrences = newMax
            )

            RankingTab.TRACKS -> copy(
                playerSections = listOf(),
                opponents = listOf(),
                tracks = allTracks.finalize(sort, query, newMin, { it.trackName() }, { it.stats.teamScore ?: 0 }),
                minOccurrences = newMin, maxOccurrences = newMax
            )
        }
    }

    /** Recherche (par [name]) + filtre occurrences (`sampleSize >= min`) + tri [sort]. */
    private fun <T : RankingItem> List<T>.finalize(
        sort: SortType,
        query: String,
        min: Int,
        name: (T) -> String,
        average: (T) -> Int
    ): List<T> = this
        .filter { query.isEmpty() || name(it).lowercase().contains(query) }
        .filter { it.sampleSize >= min }
        .let { list ->
            when (sort) {
                SortType.WINRATE -> list.sortedByDescending { it.winratePercent }
                SortType.AVERAGE -> list.sortedByDescending(average)
                SortType.COUNT -> list.sortedByDescending { it.sampleSize }
            }
        }

    private fun RankingItem.TrackRanking.trackName(): String =
        stats.map?.joinToString { context.getString(it.label) }.orEmpty()
}
