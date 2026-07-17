package fr.harmoniamk.statsmkworld.screen.stats.full

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.extension.withFullTeamStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel de l'écran Statistiques (pôle Stats, ticket #25) : porte À LA FOIS la
 * vue **Individuelles** (perf du joueur [userId]) et la vue **Équipe** (perf
 * collective). Réutilise `withFullStats` (calculs dans [Stats]) — rien n'est
 * recalculé côté UI.
 *
 * - [userId] non-null (ex. `statsfull` pour un membre du roster) ⇒ écran centré sur
 *   ce joueur ; [showTabs] = false ⇒ seul le rendu Individuelles est montré (variante
 *   « pour un joueur donné » de la vue Individuelles, mutualisée).
 * - [userId] null ⇒ le joueur courant (« mes stats ») ; [showTabs] = true ⇒ onglets
 *   Individuelles / Équipe.
 *
 * Le support 24 j est **temporairement retiré** (ticket #37) : l'écran ne présente
 * que le 12p (`is24p` figé à `false`). Pas de toggle, pas de comparatif 12/24.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = StatsFullViewModel.Factory::class)
class StatsFullViewModel @AssistedInject constructor(
    @Assisted("userId") val userId: String?,
    @Assisted val showTabs: Boolean,
    private val dataStoreRepository: DataStoreRepositoryInterface,
    private val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("userId") userId: String?, showTabs: Boolean): StatsFullViewModel
    }

    /** Un contributeur du roster (vue Équipe) : joueur, part de points, winrate. */
    data class Contributor(
        val player: PlayerEntity,
        val pointsShare: Int,
        val winrate: Int,
        val isMe: Boolean
    )

    data class State(
        val loading: Boolean = true,
        // Nom/pastille pour l'en-tête (joueur ou équipe selon l'onglet).
        val playerName: String? = null,
        val teamName: String? = null,
        // Vignettes d'en-tête : avatar MKCentral du joueur (Individuelles) et logo de
        // l'équipe (Équipe), déjà préfixés par l'hôte MKCentral. Fallback à l'affichage.
        val playerLogo: String? = null,
        val teamLogo: String? = null,
        // Id du joueur affiché (résolu : soit userId, soit joueur courant) — sert à
        // reconstruire un StatsType pour les cellules ui/stats/* réutilisées.
        val targetUserId: String? = null,
        // Stats (12p uniquement — 24p retiré, ticket #37).
        val playerStats: Stats? = null,
        val teamStats: Stats? = null,
        // Vue Équipe : contributeurs du roster.
        val contributors: List<Contributor> = listOf(),
        // Classements adversaires top3/flop3 (winrate ET score), au périmètre de la
        // vue (équipe = tous ; individuelles = du point de vue du joueur affiché).
        val topOpponentsByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val flopOpponentsByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val topOpponentsByScore: List<RankingItem.OpponentRanking> = listOf(),
        val flopOpponentsByScore: List<RankingItem.OpponentRanking> = listOf(),
        val playerTopOpponentsByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val playerFlopOpponentsByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val playerTopOpponentsByScore: List<RankingItem.OpponentRanking> = listOf(),
        val playerFlopOpponentsByScore: List<RankingItem.OpponentRanking> = listOf()
    )

    // 24p retiré (ticket #37) : l'écran ne calcule que le 12p.
    private val is24p = false

    private val _state = MutableStateFlow(State())

    val state = compute()
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    private fun compute() = databaseRepository.getWars()
        .map { warEntities ->
            val currentPlayer = dataStoreRepository.mkcPlayer.firstOrNull()
            val targetUserId = userId ?: currentPlayer?.id?.toString()
            val team = dataStoreRepository.mkcTeam.firstOrNull()
            val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
            val rosterId = currentPlayer?.rosters
                ?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()

            // Wars 12p de l'équipe (host = notre roster, sauf multi-roster qui prend tout).
            val teamWarsMode = warEntities
                .filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
                .filter { it.teamOpponent.size == 1 }
                .map { WarDetails(War(it)) }

            val playerName = targetUserId
                ?.let { databaseRepository.getPlayer(it).firstOrNull()?.name }
            val teamName = team?.name
            // Avatar joueur (comme WelcomeViewModel) : seulement résoluble pour le
            // joueur COURANT (userSettings.avatar en DataStore). Pour un autre joueur
            // (statsfull via userId), fallback initiales (avatar non caché localement).
            val isCurrentPlayer = userId == null || userId == currentPlayer?.id?.toString()
            val playerLogo = currentPlayer?.userSettings?.avatar
                ?.takeIf { it.isNotEmpty() && isCurrentPlayer }
                ?.let { "https://mkcentral.com$it" }
            val teamLogo = team?.logo
                ?.takeIf { it.isNotEmpty() }
                ?.let { "https://mkcentral.com$it" }

            // Stats 12p (player + team).
            val playerStats = teamWarsMode
                .withFullStats(databaseRepository, userId = targetUserId, is24p = is24p)
                .firstOrNull()
            val teamStats = teamWarsMode
                .withFullStats(databaseRepository, is24p = is24p)
                .firstOrNull()

            // Contributeurs du roster (vue Équipe) : chaque membre, part de points +
            // winrate, sur les wars.
            val contributors = computeContributors(teamWarsMode, targetUserId)

            // Classements top3/flop3 adversaires (winrate ET score), au périmètre de
            // la vue : équipe (tous adversaires) ET joueur (adversaires affrontés du
            // point de vue du joueur). Calcul dans le VM (mono-consommateur, rule 32).
            val currentTeamId = team?.id?.toString()
            val teamRankings = computeOpponentRankings(teamWarsMode, currentTeamId, userId = null)
            val playerRankings = computeOpponentRankings(teamWarsMode, currentTeamId, userId = targetUserId)

            State(
                loading = false,
                playerName = playerName,
                teamName = teamName,
                playerLogo = playerLogo,
                teamLogo = teamLogo,
                targetUserId = targetUserId,
                playerStats = playerStats,
                teamStats = teamStats,
                contributors = contributors,
                topOpponentsByWinrate = teamRankings.topByWinrate,
                flopOpponentsByWinrate = teamRankings.flopByWinrate,
                topOpponentsByScore = teamRankings.topByScore,
                flopOpponentsByScore = teamRankings.flopByScore,
                playerTopOpponentsByWinrate = playerRankings.topByWinrate,
                playerFlopOpponentsByWinrate = playerRankings.flopByWinrate,
                playerTopOpponentsByScore = playerRankings.topByScore,
                playerFlopOpponentsByScore = playerRankings.flopByScore
            )
        }

    /** Regroupe les 4 classements adversaires (top/flop × winrate/score). */
    private data class OpponentRankings(
        val topByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val flopByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val topByScore: List<RankingItem.OpponentRanking> = listOf(),
        val flopByScore: List<RankingItem.OpponentRanking> = listOf()
    )

    /**
     * Top3/flop3 adversaires par winrate ET score moyen, au périmètre des [wars] déjà
     * filtrées. `userId` non-null ⇒ point de vue du joueur. Seuil ≥
     * [Stats.MIN_RANKING_SAMPLE]. Réutilise `withFullTeamStats` (comme StatsViewModel,
     * rule 32 : mono-consommateur, hors cache worker car dépend de la vue).
     */
    private suspend fun computeOpponentRankings(
        wars: List<WarDetails>,
        currentTeamId: String?,
        userId: String?
    ): OpponentRankings {
        if (wars.isEmpty()) return OpponentRankings()
        val teams = databaseRepository.getTeams().firstOrNull()
            .orEmpty()
            .filterNot { it.id == currentTeamId }
        val warEntities = wars.map { WarEntity(it.war) }
        val rankable = teams
            .withFullTeamStats(wars = warEntities, databaseRepository = databaseRepository, userId = userId, is24p = is24p)
            .firstOrNull()
            .orEmpty()
            .map { RankingItem.OpponentRanking(it.first, it.second) }
            .filter { it.stats.warStats.warsPlayed >= Stats.MIN_RANKING_SAMPLE }
        return OpponentRankings(
            topByWinrate = rankable.sortedByDescending { it.winrate }.take(3),
            flopByWinrate = rankable.sortedBy { it.winrate }.take(3),
            topByScore = rankable.sortedByDescending { it.stats.averagePoints }.take(3),
            flopByScore = rankable.sortedBy { it.stats.averagePoints }.take(3)
        )
    }

    /** Contributeurs : chaque membre du roster, part de ses points sur le total
     * cumulé des membres + winrate perso. Trié par part décroissante. */
    private suspend fun computeContributors(
        wars: List<WarDetails>,
        meId: String?
    ): List<StatsFullViewModel.Contributor> {
        val roster = dataStoreRepository.mkcTeam.firstOrNull()?.rosters
        val members = databaseRepository.getPlayers().firstOrNull()
            .orEmpty()
            // Membres du roster mkworld courant uniquement (les alliés n'ont pas de rosterId matché).
            .filter { player -> roster?.any { it.id.toString() == player.rosterId } == true }
        val perPlayer = members.mapNotNull { player ->
            wars.filter { it.war.hasPlayer(player.id) }
                .withFullStats(databaseRepository, userId = player.id, is24p = is24p)
                .firstOrNull()
                ?.takeIf { it.warStats.warsPlayed > 0 }
                ?.let { player to it }
        }
        val totalPoints = perPlayer.sumOf { it.second.warScores.sumOf { score -> score.score } }
            .takeIf { it > 0 } ?: return listOf()
        return perPlayer
            .map { (player, stats) ->
                val playerPoints = stats.warScores.sumOf { it.score }
                Contributor(
                    player = player,
                    pointsShare = (playerPoints * 100) / totalPoints,
                    winrate = stats.allTimeForm?.winrate ?: 0,
                    isMe = player.id == meId
                )
            }
            .sortedByDescending { it.pointsShare }
    }
}
