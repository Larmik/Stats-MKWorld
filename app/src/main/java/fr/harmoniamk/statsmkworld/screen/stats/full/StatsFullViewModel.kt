package fr.harmoniamk.statsmkworld.screen.stats.full

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
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
     * (winrate, nb de matchs).
     */
    data class NamedTile(val name: String? = null, val labelRes: Int? = null, val value: String? = null)

    data class State(
        val loading: Boolean = true,
        // Nom/tag/pastille pour l'en-tête (joueur ou équipe selon l'onglet).
        val playerName: String? = null,
        val teamName: String? = null,
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
        val teamMostPlayedTrack: NamedTile? = null
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

            State(
                loading = false,
                playerName = playerName,
                teamName = teamName,
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
                teamMostPlayedTrack = teamStats?.mostPlayedMap?.mapLabelRes()?.let { NamedTile(labelRes = it) }
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
            mostPlayed?.let { NamedTile(name = opponentName(it.key), value = "${it.value.size}×") },
            mostBeaten?.let { NamedTile(name = opponentName(it.key), value = "${it.value}%") },
            leastBeaten?.let { NamedTile(name = opponentName(it.key), value = "${it.value}%") }
        ).map { it ?: NamedTile(name = "-") }
    }

    /** Nom d'affichage d'un adversaire (roster si résoluble, sinon équipe) — rule 12. */
    private suspend fun opponentName(opponentId: String): String {
        val resolved = databaseRepository.getTeam(opponentId)
        val roster = resolved?.rosters?.firstOrNull { it.id == opponentId }
        return roster?.name ?: resolved?.name ?: "Équipe inconnue"
    }

    private fun Stats.toSummary() = ModeSummary(
        winrate = allTimeForm?.winrate,
        averageScore = averagePoints
    )
}

/** Filtre les wars par mode (24 j = ≥ 2 adversaires, 12 j = 1 adversaire). */
private fun List<fr.harmoniamk.statsmkworld.database.entities.WarEntity>.filterByMode(is24p: Boolean) =
    filter { (is24p && it.teamOpponent.size > 1) || (!is24p && it.teamOpponent.size == 1) }

/** Ressource string du circuit d'un TrackStats (première map), null si non résolu. */
private fun fr.harmoniamk.statsmkworld.model.local.TrackStats?.mapLabelRes(): Int? =
    this?.map?.firstOrNull()?.label

/** Tuile circuit (winrate + libellé de map) à partir d'un TrackStats classé. */
private fun fr.harmoniamk.statsmkworld.model.local.TrackStats?.toTrackTile(): StatsFullViewModel.NamedTile? =
    this?.mapLabelRes()?.let { StatsFullViewModel.NamedTile(labelRes = it, value = "${this.winRate ?: 0}%") }
