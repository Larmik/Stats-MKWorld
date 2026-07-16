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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
 * Le toggle 12 j / 24 j est un état réactif ([onWarTypeSwitch]) : la bascule
 * recompute les Stats du mode sélectionné (rule 11, aucune re-navigation). Le
 * comparatif 12/24 est fourni par les résumés de l'AUTRE mode ([ModeSummary]).
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

    /** Résumé d'un mode (12 j / 24 j) pour le comparatif : winrate + score/points moyen. */
    data class ModeSummary(val winrate: Int?, val averageScore: Int?)

    /** Un contributeur du roster (vue Équipe) : joueur, part de points, winrate. */
    data class Contributor(
        val player: PlayerEntity,
        val pointsShare: Int,
        val winrate: Int,
        val isMe: Boolean
    )

    /**
     * Tuile nommée (adversaire / circuit). Le nom vient soit d'une chaîne résolue
     * (adversaire, résolu en base), soit d'une ressource string ([labelRes], circuit
     * — résolue à l'affichage car nécessite un Context). [value] = métrique annexe
     * (winrate, nb de matchs). Vignette : [logo] (chemin logo MKCentral d'une équipe)
     * OU [pictureRes] (illustration `@DrawableRes` d'un circuit).
     */
    data class NamedTile(
        val name: String? = null,
        val labelRes: Int? = null,
        val value: String? = null,
        val logo: String? = null,
        val pictureRes: Int? = null
    )

    data class State(
        val loading: Boolean = true,
        // Nom/tag/pastille pour l'en-tête (joueur ou équipe selon l'onglet).
        val playerName: String? = null,
        val teamName: String? = null,
        // Id du joueur affiché (résolu : soit userId, soit joueur courant) — sert à
        // reconstruire un StatsType pour les cellules ui/stats/* réutilisées.
        val targetUserId: String? = null,
        // Stats du mode courant.
        val playerStats: Stats? = null,
        val teamStats: Stats? = null,
        // Comparatif : résumé de l'AUTRE mode (celui non affiché).
        val playerOtherMode: ModeSummary? = null,
        val teamOtherMode: ModeSummary? = null,
        val is24p: Boolean = false,
        // Vue Équipe.
        val contributors: List<Contributor> = listOf(),
        val mostPlayedOpponent: NamedTile? = null,
        val mostBeatenOpponent: NamedTile? = null,
        val leastBeatenOpponent: NamedTile? = null,
        // Meilleure / pire course du joueur (points sur une manche).
        val bestCourse: NamedTile? = null,
        val worstCourse: NamedTile? = null,
        // Circuits perso : meilleur / pire winrate du joueur.
        val bestPlayerTrack: NamedTile? = null,
        val worstPlayerTrack: NamedTile? = null,
        // Circuits ÉQUIPE : le + joué / meilleur / pire (winrate).
        val teamBestTrack: NamedTile? = null,
        val teamWorstTrack: NamedTile? = null,
        val teamMostPlayedTrack: NamedTile? = null,
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

    private val _state = MutableStateFlow(State())

    val state = dataStoreRepository.is24PEnabled
        .flatMapLatest { is24p -> compute(is24p) }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    fun onWarTypeSwitch(index: Int) {
        viewModelScope.launch {
            dataStoreRepository.set24PEnabled(index == 1)
        }
    }

    private suspend fun compute(is24p: Boolean) = databaseRepository.getWars()
        .map { warEntities ->
            val currentPlayer = dataStoreRepository.mkcPlayer.firstOrNull()
            val targetUserId = userId ?: currentPlayer?.id?.toString()
            val team = dataStoreRepository.mkcTeam.firstOrNull()
            val multiRosterEnabled = dataStoreRepository.multiRosterEnabled.firstOrNull() == true
            val rosterId = currentPlayer?.rosters
                ?.firstOrNull { it.game == "mkworld" }?.rosterID?.toString()

            // Wars de l'équipe (host = notre roster, sauf multi-roster qui prend tout).
            val teamWarsAll = warEntities
                .filter { (!multiRosterEnabled && it.teamHost == rosterId) || multiRosterEnabled }
            val teamWarsMode = teamWarsAll.filterByMode(is24p).map { WarDetails(War(it)) }
            val teamWarsOther = teamWarsAll.filterByMode(!is24p).map { WarDetails(War(it)) }

            val playerName = targetUserId
                ?.let { databaseRepository.getPlayer(it).firstOrNull()?.name }
            val teamName = team?.name

            // Stats mode courant (player + team) + résumé de l'autre mode.
            val playerStats = teamWarsMode
                .withFullStats(databaseRepository, userId = targetUserId, is24p = is24p)
                .firstOrNull()
            val teamStats = teamWarsMode
                .withFullStats(databaseRepository, is24p = is24p)
                .firstOrNull()
            val playerOther = teamWarsOther
                .withFullStats(databaseRepository, userId = targetUserId, is24p = !is24p)
                .firstOrNull()
                ?.toSummary()
            val teamOther = teamWarsOther
                .withFullStats(databaseRepository, is24p = !is24p)
                .firstOrNull()
                ?.toSummary()

            // Contributeurs du roster (vue Équipe) : chaque membre, part de points +
            // winrate, sur les wars du mode courant.
            val contributors = computeContributors(teamWarsMode, targetUserId, is24p)

            // Adversaires : le + joué / + vaincu / − vaincu (vue Équipe).
            val opponents = computeOpponentTiles(teamWarsMode)

            // Classements top3/flop3 adversaires (winrate ET score), au périmètre de
            // la vue : équipe (tous adversaires) ET joueur (adversaires affrontés du
            // point de vue du joueur). Calcul dans le VM (mono-consommateur, rule 32).
            val currentTeamId = team?.id?.toString()
            val teamRankings = computeOpponentRankings(teamWarsMode, currentTeamId, userId = null, is24p = is24p)
            val playerRankings = computeOpponentRankings(teamWarsMode, currentTeamId, userId = targetUserId, is24p = is24p)

            State(
                loading = false,
                playerName = playerName,
                teamName = teamName,
                targetUserId = targetUserId,
                playerStats = playerStats,
                teamStats = teamStats,
                playerOtherMode = playerOther,
                teamOtherMode = teamOther,
                is24p = is24p,
                contributors = contributors,
                mostPlayedOpponent = opponents.getOrNull(0),
                mostBeatenOpponent = opponents.getOrNull(1),
                leastBeatenOpponent = opponents.getOrNull(2),
                bestCourse = playerStats?.bestCoursePoints?.let { NamedTile(name = "+$it") },
                worstCourse = playerStats?.worstCoursePoints?.let { NamedTile(name = it.toString()) },
                // « Tes circuits » : meilleur/pire winrate perso (vue joueur).
                bestPlayerTrack = playerStats?.bestMapByWinrate.toTrackTile(),
                worstPlayerTrack = playerStats?.worstMapByWinrate.toTrackTile(),
                teamBestTrack = teamStats?.bestMapByWinrate.toTrackTile(),
                teamWorstTrack = teamStats?.worstMapByWinrate.toTrackTile(),
                teamMostPlayedTrack = teamStats?.mostPlayedMap.toTrackTile(),
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
        userId: String?,
        is24p: Boolean
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
        meId: String?,
        is24p: Boolean
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

    /** Adversaires : le + joué, le + vaincu (winrate haut), le − vaincu (winrate bas),
     * seuil ≥ [Stats.MIN_RANKING_SAMPLE] pour les vaincus. Résout nom via TeamEntity. */
    private suspend fun computeOpponentTiles(wars: List<WarDetails>): List<NamedTile> {
        if (wars.isEmpty()) return listOf()
        // Regroupe par identifiant d'opposant (rosterId/teamId).
        val byOpponent = wars.flatMap { war -> war.war.teamOpponent.map { it to war } }
            .groupBy({ it.first }, { it.second })
        if (byOpponent.isEmpty()) return listOf()

        val mostPlayed = byOpponent.maxByOrNull { it.value.size }
        val winrates = byOpponent
            .filter { it.value.size >= Stats.MIN_RANKING_SAMPLE }
            .mapValues { (_, opponentWars) ->
                (opponentWars.count { it.displayedDiff.contains('+') } * 100) / opponentWars.size
            }
        val mostBeaten = winrates.maxByOrNull { it.value }
        val leastBeaten = winrates.minByOrNull { it.value }

        return listOf(
            mostPlayed?.let { opponentTile(it.key, value = "${it.value.size}×") },
            mostBeaten?.let { opponentTile(it.key, value = "${it.value}%") },
            leastBeaten?.let { opponentTile(it.key, value = "${it.value}%") }
        ).map { it ?: NamedTile(name = "-") }
    }

    /** Tuile d'un adversaire : nom roster>équipe (rule 12) + logo de l'équipe parente. */
    private suspend fun opponentTile(opponentId: String, value: String): NamedTile {
        val resolved = databaseRepository.getTeam(opponentId)
        val roster = resolved?.rosters?.firstOrNull { it.id == opponentId }
        return NamedTile(
            name = roster?.name ?: resolved?.name ?: "Équipe inconnue",
            value = value,
            logo = resolved?.logo
        )
    }

    private fun Stats.toSummary() = ModeSummary(
        winrate = allTimeForm?.winrate,
        averageScore = averagePoints
    )
}

/** Filtre les wars par mode (24 j = ≥ 2 adversaires, 12 j = 1 adversaire). */
private fun List<fr.harmoniamk.statsmkworld.database.entities.WarEntity>.filterByMode(is24p: Boolean) =
    filter { (is24p && it.teamOpponent.size > 1) || (!is24p && it.teamOpponent.size == 1) }

/** Première map d'un TrackStats (porte label + illustration), null si non résolue. */
private fun fr.harmoniamk.statsmkworld.model.local.TrackStats?.firstMap() =
    this?.map?.firstOrNull()

/** Tuile circuit (winrate + libellé + illustration de map) à partir d'un TrackStats classé. */
private fun fr.harmoniamk.statsmkworld.model.local.TrackStats?.toTrackTile(): StatsFullViewModel.NamedTile? =
    firstMap()?.let {
        StatsFullViewModel.NamedTile(labelRes = it.label, value = "${this?.winRate ?: 0}%", pictureRes = it.picture)
    }
