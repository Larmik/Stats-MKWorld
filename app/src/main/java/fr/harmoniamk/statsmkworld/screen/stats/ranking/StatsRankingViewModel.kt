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

    /** Une entrée « En bref » (insight) : libellé + nom + winrate. */
    data class Insight(val label: Int, val name: String, val winrate: Int)

    data class State(
        val tab: RankingTab = RankingTab.PLAYERS,
        val sort: SortType = SortType.WINRATE,
        val search: String = "",
        // Onglet Joueurs : lignes triées à plat (le regroupement roster/allies n'est
        // plus affiché dans le prototype, une simple liste triée suffit).
        val players: List<RankingItem.PlayerRanking> = listOf(),
        // Onglets Adversaires / Circuits : entrées triées.
        val opponents: List<RankingItem.OpponentRanking> = listOf(),
        val tracks: List<RankingItem.TrackRanking> = listOf(),
        // Cartes « En bref » (adversaires : domine/bête noire ; circuits : meilleur/pire).
        val bestInsight: Insight? = null,
        val worstInsight: Insight? = null,
        val currentUserId: String? = null,
        val is24PEnabled: Boolean? = null
    )

    private val _state = MutableStateFlow(State())
    private var currentUser: MKCPlayer? = null
    // Listes brutes (non filtrées/triées) mémorisées pour re-filtrer à chaque recherche/tri.
    private var allPlayers: List<RankingItem.PlayerRanking> = listOf()
    private var allOpponents: List<RankingItem.OpponentRanking> = listOf()
    private var allTracks: List<RankingItem.TrackRanking> = listOf()

    val state = flowOf(Unit)
        .map {
            currentUser = dataStoreRepository.mkcPlayer.firstOrNull()
            val is24p = dataStoreRepository.is24PEnabled.firstOrNull()
            allPlayers = statsRepository.playersRankList.values.flatten()
                .filter { it.stats.warStats.warsPlayed > 0 }
            // Perspective ÉQUIPE pour adversaires/circuits (« On domine », winrate
            // global de l'équipe), conforme au prototype qui n'a pas de switch indiv/équipe.
            allOpponents = statsRepository.opponentRankList.mapNotNull { it as? RankingItem.OpponentRanking }
            allTracks = statsRepository.trackRankList.mapNotNull { it as? RankingItem.TrackRanking }
            _state.value.copy(
                currentUserId = currentUser?.id.toString(),
                is24PEnabled = is24p
            ).recompute()
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun onTabSelected(index: Int) {
        val tab = RankingTab.entries.getOrElse(index) { RankingTab.PLAYERS }
        // Nouvel onglet : on repart du tri par défaut (Winrate) et d'une recherche vide.
        _state.value = _state.value.copy(tab = tab, sort = SortType.WINRATE, search = "").recompute()
    }

    fun onSortSelected(index: Int) {
        val sort = SortType.entries.getOrElse(index) { SortType.WINRATE }
        _state.value = _state.value.copy(sort = sort).recompute()
    }

    fun onSearch(search: String) {
        _state.value = _state.value.copy(search = search).recompute()
    }

    /** Applique recherche + tri + insight sur les listes brutes selon l'onglet courant. */
    private fun State.recompute(): State {
        val query = search.trim().lowercase()
        return when (tab) {
            RankingTab.PLAYERS -> {
                val filtered = allPlayers.filter {
                    query.isEmpty() || it.player.name.lowercase().contains(query)
                }
                copy(
                    players = filtered.sortedByRanking(sort) { it.stats.averagePoints },
                    opponents = listOf(),
                    tracks = listOf(),
                    // Pas de carte « En bref » sur l'onglet Joueurs (absente du prototype).
                    bestInsight = null,
                    worstInsight = null
                )
            }

            RankingTab.OPPONENTS -> {
                val filtered = allOpponents.filter {
                    query.isEmpty() || it.team.name.lowercase().contains(query)
                }
                copy(
                    players = listOf(),
                    opponents = filtered.sortedByRanking(sort) { it.stats.averagePoints },
                    tracks = listOf(),
                    bestInsight = allOpponents.bestByWinrate()
                        ?.let { Insight(R.string.rankings_insight_dominate, it.team.name, it.winratePercent) },
                    worstInsight = allOpponents.worstByWinrate()
                        ?.let { Insight(R.string.rankings_insight_nemesis, it.team.name, it.winratePercent) }
                )
            }

            RankingTab.TRACKS -> {
                val filtered = allTracks.filter {
                    query.isEmpty() || it.trackName().lowercase().contains(query)
                }
                copy(
                    players = listOf(),
                    opponents = listOf(),
                    tracks = filtered.sortedByRanking(sort) { it.stats.teamScore ?: 0 },
                    bestInsight = allTracks.bestByWinrate()
                        ?.let { Insight(R.string.rankings_insight_best, it.trackName(), it.winratePercent) },
                    worstInsight = allTracks.worstByWinrate()
                        ?.let { Insight(R.string.rankings_insight_worst, it.trackName(), it.winratePercent) }
                )
            }
        }
    }

    private fun RankingItem.TrackRanking.trackName(): String =
        stats.map?.joinToString { context.getString(it.label) }.orEmpty()

    // Tri : WINRATE (seuil MIN_RANKING_SAMPLE appliqué, petits échantillons rejetés en
    // fin de liste) / AVERAGE (score moyen) / COUNT (nb de matchs).
    private fun <T : RankingItem> List<T>.sortedByRanking(sort: SortType, average: (T) -> Int): List<T> =
        when (sort) {
            SortType.WINRATE -> sortedWith(
                compareByDescending<T> { it.sampleSize >= Stats.MIN_RANKING_SAMPLE }
                    .thenByDescending { it.winratePercent }
            )
            SortType.AVERAGE -> sortedByDescending(average)
            SortType.COUNT -> sortedByDescending { it.sampleSize }
        }

    // Insight winrate : uniquement les entrées ayant atteint le seuil d'échantillon
    // (MIN_RANKING_SAMPLE), pour éviter le biais des petites confrontations.
    private fun <T : RankingItem> List<T>.bestByWinrate(): T? =
        filter { it.sampleSize >= Stats.MIN_RANKING_SAMPLE }.maxByOrNull { it.winratePercent }

    private fun <T : RankingItem> List<T>.worstByWinrate(): T? =
        filter { it.sampleSize >= Stats.MIN_RANKING_SAMPLE }.minByOrNull { it.winratePercent }
}
