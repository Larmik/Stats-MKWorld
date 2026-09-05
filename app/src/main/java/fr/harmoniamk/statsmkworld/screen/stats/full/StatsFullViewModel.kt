package fr.harmoniamk.statsmkworld.screen.stats.full

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.extension.filterBySeason
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.totalShocks
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.extension.withFullTeamStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.ranking.RankingItem
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

    /**
     * Une ligne de classement du roster (vue Équipe) : joueur, part de points
     * (« Contributeurs »), part de shocks (« Meilleurs baggeurs », #69) et winrate.
     * Le MÊME modèle alimente les deux classements ; seul le critère de tri diffère
     * (pointsShare vs shockShare). part de shocks = total shocks joueur / total shocks
     * équipe (ratio TOTAL/TOTAL, jamais une moyenne).
     */
    data class Contributor(
        val player: PlayerEntity,
        val pointsShare: Int,
        val shockShare: Int,
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
        // Stats (12p uniquement — 24p retiré, ticket #37), déclinées par FENÊTRE de période
        // (#68) : 0 = all-time, 1 = 5 dernières, 2 = 10 dernières. Le sélecteur de période
        // GLOBAL de l'écran choisit l'index ; toutes les sections lisent la même fenêtre.
        val playerStatsByWindow: Map<Int, Stats> = mapOf(),
        val teamStatsByWindow: Map<Int, Stats> = mapOf(),
        // Taux de participation du joueur (#78) PAR FENÊTRE (0 = all-time, 1 = 5, 2 = 10) :
        // % de wars de l'équipe (dénominateur = wars de la fenêtre) où le joueur est présent.
        // Numérateur et dénominateur sur la MÊME fenêtre filtrée (saison + all-time/5/10).
        val participationRateByWindow: Map<Int, Int> = mapOf(),
        // Tables Top/Bot 2→6 (toutes manches, vue équipe) : équipe ET adversaire (complément
        // des positions), alimentées par un MapStats par fenêtre (userId null, 12p).
        // Réutilisées par les StatCard « Top/Bot équipe » / « Top/Bot adversaire ».
        val teamMapStatsByWindow: Map<Int, MapStats> = mapOf(),
        // Vue Équipe : contributeurs du roster par fenêtre (0 = all-time, 1 = 5, 2 = 10).
        val contributorsByWindow: Map<Int, List<Contributor>> = mapOf(),
        // Vue Équipe : MÊMES lignes que les contributeurs, mais triées par part de SHOCKS
        // (« Meilleurs baggeurs », #69). Part de shocks = total shocks joueur / total équipe.
        val baggersByWindow: Map<Int, List<Contributor>> = mapOf(),
        // Classements adversaires top3/flop3 (occurrences, winrate ET score) PAR FENÊTRE, au
        // périmètre de la vue (équipe = tous ; individuelles = du point de vue du joueur).
        val teamOpponentsByWindow: Map<Int, OpponentPodiums> = mapOf(),
        val playerOpponentsByWindow: Map<Int, OpponentPodiums> = mapOf(),
        // Filtre par saison (#70) : liste des saisons (ordre chrono) + saison sélectionnée
        // (`selectedSeasonNumber` null = tout l'historique, défaut = saison en cours). Les
        // wars sont filtrées sur l'intervalle [start, end] AVANT tout calcul d'agrégat.
        val seasons: List<SeasonEntity> = listOf(),
        val selectedSeasonNumber: Int? = null
    )

    /** Les 6 classements adversaires d'un podium (top/flop × occurrences/winrate/score) +
     * la **liste complète** ([all]) pour le classement entier scopé (#67 round 3). */
    data class OpponentPodiums(
        val topByCount: List<RankingItem.OpponentRanking> = listOf(),
        val flopByCount: List<RankingItem.OpponentRanking> = listOf(),
        val topByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val flopByWinrate: List<RankingItem.OpponentRanking> = listOf(),
        val topByScore: List<RankingItem.OpponentRanking> = listOf(),
        val flopByScore: List<RankingItem.OpponentRanking> = listOf(),
        // Liste complète des adversaires (non tronquée) au périmètre de la vue — triée par
        // le classement entier selon son propre sélecteur. `rankable` = ≥ MIN_RANKING_SAMPLE.
        val all: List<RankingItem.OpponentRanking> = listOf()
    )

    /**
     * Sélection de saison (#70) hissée dans le VM (rule 11 : état, pas de re-nav).
     * [Default] = première ouverture → la saison EN COURS ; [AllTime] = tout l'historique ;
     * [Specific] = une saison passée précise. Distinct de « saison courante » car le
     * défaut doit se résoudre APRÈS chargement des saisons (numéro pas connu d'avance).
     */
    sealed interface SeasonFilter {
        data object Default : SeasonFilter
        data object AllTime : SeasonFilter
        data class Specific(val number: Int) : SeasonFilter
    }

    // 24p retiré (ticket #37) : l'écran ne calcule que le 12p.
    private val is24p = false

    // Fenêtres de période du sélecteur GLOBAL (#68) : (indexUI, N dernières wars) — 0 =
    // all-time (null), 1 = 5, 2 = 10. Source unique partagée par tout le fenêtrage du VM
    // (stats/tops-bots/adversaires/contributeurs) pour rester cohérent entre sections.
    private val windowSizes = listOf(0 to null, 1 to 5, 2 to 10)

    // Sélection de saison courante (#70). Recompute déclenché à chaque changement via
    // `combine` avec le flux de wars → recalcul à la volée (stratégie recommandée du ticket).
    private val _seasonFilter = MutableStateFlow<SeasonFilter>(SeasonFilter.Default)

    private val _state = MutableStateFlow(State())

    val state = compute()
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    /** Sélection de saison depuis l'UI : `number` null = tout l'historique. */
    fun onSeasonSelected(number: Int?) {
        _seasonFilter.value = number?.let { SeasonFilter.Specific(it) } ?: SeasonFilter.AllTime
    }

    private fun compute() = combine(databaseRepository.getWars(), _seasonFilter, databaseRepository.getSeasons()) { warEntities, seasonFilter, seasons ->
            // Saisons (cache Room) observées en Flow réactif (#73) : le sélecteur apparaît dès
            // que l'hydratation eager écrit les saisons. Liste pour le sélecteur + résolution
            // de la saison effective (défaut = saison en cours). `null` = tout l'historique.
            val currentSeason = seasons.lastOrNull { it.end == null }
            val activeSeason = when (seasonFilter) {
                is SeasonFilter.AllTime -> null
                is SeasonFilter.Specific -> seasons.firstOrNull { it.number == seasonFilter.number }
                is SeasonFilter.Default -> currentSeason
            }
            // Filtre saison appliqué AVANT tout autre filtre/calcul (calculé sur war.id).
            val seasonWars = warEntities.filterBySeason(activeSeason)
            computeState(seasonWars, seasons, activeSeason?.number)
        }

    // Le calcul des agrégats (fenêtres, contributeurs, adversaires) est CPU-lourd : déporté sur
    // `Dispatchers.Default` via `withContext` — et NON `flowOn` (#73), qui, sur une chaîne passant
    // par `mergeWith`/`flattenMerge`, provoquait une course cross-thread laissant gagner l'état vide
    // (dropdown de saison disparu). `seasons`/`activeSeasonNumber` sont résolus sur le collecteur et
    // toujours reportés dans le State, quel que soit le résultat du calcul. Cf. rule 21.
    private suspend fun computeState(
        warEntities: List<WarEntity>,
        seasons: List<SeasonEntity>,
        activeSeasonNumber: Int?
    ) = withContext(Dispatchers.Default) { warEntities
        .let { warEntities ->
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

            // Wars de l'équipe triées chronologiquement (war.id = timestamp) : base commune
            // du fenêtrage de TOUTES les sections (#68). La fenêtre N = N dernières wars de
            // l'équipe, cohérente entre stats/contributeurs/adversaires/tops-bots.
            val chronologicalWars = teamWarsMode.sortedBy { it.war.id }
            val currentTeamId = team?.id?.toString()

            // Chaque section est déclinée sur les 3 fenêtres (0 = all-time, 1 = 5, 2 = 10)
            // et keyée par index. Le sélecteur de période GLOBAL de l'écran choisit l'index ;
            // toutes les sections lisent alors la même fenêtre. Fenêtrage à la demande sur
            // `takeLast(n)` des wars chrono (rule 13 : justesse ; pas de valeur en dur).
            val playerStatsByWindow = mutableMapOf<Int, Stats>()
            val teamStatsByWindow = mutableMapOf<Int, Stats>()
            val teamMapStatsByWindow = mutableMapOf<Int, MapStats>()
            val teamOpponentsByWindow = mutableMapOf<Int, OpponentPodiums>()
            val playerOpponentsByWindow = mutableMapOf<Int, OpponentPodiums>()
            // Taux de participation (#78) par fenêtre : wars du joueur / wars de l'équipe (même
            // fenêtre). Garde-fou dénominateur nul → 0 %. Calculé dans le VM (mono-consommateur,
            // rule 32) sur `windowWars`, la même base filtrée que les autres agrégats.
            val participationRateByWindow = mutableMapOf<Int, Int>()
            windowSizes.forEach { (index, lastN) ->
                val windowWars = lastN?.let { chronologicalWars.takeLast(it) } ?: chronologicalWars
                participationRateByWindow[index] = when (val teamCount = windowWars.size) {
                    0 -> 0
                    else -> windowWars.count { it.war.hasPlayer(targetUserId) } * 100 / teamCount
                }
                windowWars.withFullStats(databaseRepository, userId = targetUserId, is24p = is24p)
                    .firstOrNull()?.let { playerStatsByWindow[index] = it }
                windowWars.withFullStats(databaseRepository, is24p = is24p)
                    .firstOrNull()?.let { teamStatsByWindow[index] = it }
                // MapStats (toutes manches, vue équipe = userId null) : tables Top/Bot 2→6
                // d'équipe ET adversaire (détail 2→6 que RecordsTilesCard n'a pas).
                teamMapStatsByWindow[index] = MapStats(
                    list = windowWars.flatMap { war ->
                        war.warTracks.map { track -> MapDetails(war = war, warTrack = track, position = null) }
                    },
                    userId = null,
                    is24p = is24p
                )
                // Classements top3/flop3 adversaires (occurrences/winrate/score), au périmètre
                // de la vue : équipe (tous adversaires) ET joueur (adversaires affrontés du
                // point de vue du joueur). Calcul dans le VM (mono-consommateur, rule 32).
                teamOpponentsByWindow[index] = computeOpponentRankings(windowWars, currentTeamId, userId = null)
                playerOpponentsByWindow[index] = computeOpponentRankings(windowWars, currentTeamId, userId = targetUserId)
            }

            // Contributeurs du roster (vue Équipe) par fenêtre (all-time / 5 / 10).
            val contributorsByWindow = computeContributorsByWindow(teamWarsMode, targetUserId)
            // Meilleurs baggeurs (#69) : MÊMES lignes triées par part de SHOCKS décroissante.
            val baggersByWindow = contributorsByWindow.mapValues { (_, contributors) ->
                contributors.sortedByDescending { it.shockShare }
            }
            // NB (#69, retour PR #75) : la section indiv « Ta contribution » NE recalcule
            // PLUS sa part de points/shocks. Elle LIT la ligne du joueur (`isMe`) dans
            // `contributorsByWindow` (source de vérité UNIQUE), garantissant un % IDENTIQUE
            // à celui du classement équipe pour un même joueur/fenêtre. L'ancien calcul indiv
            // (`playerShockShareByWindow`, `Stats.playerContribution`) divergeait car il
            // agrégeait sur un SOUS-ENSEMBLE de wars (celles du joueur) → dénominateur ≠.

            State(
                loading = false,
                playerName = playerName,
                teamName = teamName,
                playerLogo = playerLogo,
                teamLogo = teamLogo,
                targetUserId = targetUserId,
                playerStatsByWindow = playerStatsByWindow,
                teamStatsByWindow = teamStatsByWindow,
                participationRateByWindow = participationRateByWindow,
                teamMapStatsByWindow = teamMapStatsByWindow,
                contributorsByWindow = contributorsByWindow,
                baggersByWindow = baggersByWindow,
                teamOpponentsByWindow = teamOpponentsByWindow,
                playerOpponentsByWindow = playerOpponentsByWindow,
                seasons = seasons,
                selectedSeasonNumber = activeSeasonNumber
            )
        }
    }

    /**
     * Top3/flop3 adversaires par **occurrences** (nb de confrontations), winrate ET
     * score moyen, au périmètre des [wars] déjà filtrées. `userId` non-null ⇒ point de
     * vue du joueur. winrate/score filtrent à ≥ [Stats.MIN_RANKING_SAMPLE] ; les
     * occurrences classent TOUS les adversaires (« le moins joué » a du sens sous le
     * seuil). Réutilise `withFullTeamStats` (comme StatsViewModel, rule 32).
     */
    private suspend fun computeOpponentRankings(
        wars: List<WarDetails>,
        currentTeamId: String?,
        userId: String?
    ): OpponentPodiums {
        if (wars.isEmpty()) return OpponentPodiums()
        val teams = databaseRepository.getTeams().firstOrNull()
            .orEmpty()
            .filterNot { it.id == currentTeamId }
        val warEntities = wars.map { WarEntity(it.war) }
        val all = teams
            .withFullTeamStats(wars = warEntities, databaseRepository = databaseRepository, userId = userId, is24p = is24p)
            .firstOrNull()
            .orEmpty()
            .map { RankingItem.OpponentRanking(it.first, it.second) }
        val rankable = all.filter { it.stats.warStats.warsPlayed >= Stats.MIN_RANKING_SAMPLE }
        return OpponentPodiums(
            topByCount = all.sortedByDescending { it.stats.warStats.warsPlayed }.take(3),
            flopByCount = all.sortedBy { it.stats.warStats.warsPlayed }.take(3),
            topByWinrate = rankable.sortedByDescending { it.winrate }.take(3),
            flopByWinrate = rankable.sortedBy { it.winrate }.take(3),
            topByScore = rankable.sortedByDescending { it.stats.averagePoints }.take(3),
            flopByScore = rankable.sortedBy { it.stats.averagePoints }.take(3),
            // Liste complète des adversaires réellement affrontés (au périmètre de la vue),
            // pour le classement entier scopé (#67 round 3). Tri par défaut = occurrences.
            all = all.filter { it.stats.warStats.warsPlayed > 0 }
                .sortedByDescending { it.stats.warStats.warsPlayed }
        )
    }

    /**
     * Contributeurs du roster pour les **3 fenêtres** (all-time / 5 / 10 dernières wars),
     * keyées par index (0/1/2) pour le sélecteur de la section. Chaque membre : part de
     * ses points (÷ total cumulé des membres), part de ses shocks (÷ total shocks équipe)
     * et winrate, sur la fenêtre. Trié décroissant.
     *
     * **Source de vérité UNIQUE** (retour PR #75) : la section indiv « Ta contribution »
     * lit la ligne du joueur (`isMe`) ici même — mêmes wars, même dénominateur — au lieu
     * de recalculer sur un sous-ensemble, ce qui garantit des % identiques indiv ↔ équipe.
     */
    private suspend fun computeContributorsByWindow(
        wars: List<WarDetails>,
        meId: String?
    ): Map<Int, List<Contributor>> {
        val roster = dataStoreRepository.mkcTeam.firstOrNull()?.rosters
        val members = databaseRepository.getPlayers().firstOrNull()
            .orEmpty()
            // Membres du roster mkworld courant uniquement (les alliés n'ont pas de rosterId matché).
            .filter { player -> roster?.any { it.id.toString() == player.rosterId } == true }
        return windowSizes.associate { (index, lastN) ->
            index to computeContributors(wars, members, meId, lastN)
        }
    }

    /** Contributeurs sur une fenêtre : [lastN] null = all-time, sinon N dernières wars
     * de l'ÉQUIPE (fenêtre commune à tous les membres, triée chrono par war.id). */
    private suspend fun computeContributors(
        wars: List<WarDetails>,
        members: List<PlayerEntity>,
        meId: String?,
        lastN: Int?
    ): List<Contributor> {
        // Fenêtre commune : N dernières wars de l'équipe (chrono), puis part de chaque
        // membre DANS cette fenêtre.
        val windowWars = wars.sortedBy { it.war.id }
            .let { list -> lastN?.let { list.takeLast(it) } ?: list }
        val perPlayer = members.mapNotNull { player ->
            windowWars.filter { it.war.hasPlayer(player.id) }
                .withFullStats(databaseRepository, userId = player.id, is24p = is24p)
                .firstOrNull()
                ?.takeIf { it.warStats.warsPlayed > 0 }
                ?.let { player to it }
        }
        val totalPoints = perPlayer.sumOf { it.second.warScores.sumOf { score -> score.score } }
            .takeIf { it > 0 } ?: return listOf()
        // Dénominateur SHOCKS (#69) : total des shocks de l'ÉQUIPE sur la fenêtre (tous
        // joueurs). Part de chaque membre = ses shocks / ce total (ratio TOTAL/TOTAL).
        val totalTeamShocks = windowWars.totalShocks()
        return perPlayer
            .map { (player, stats) ->
                Contributor(
                    player = player,
                    pointsShare = (stats.warScores.sumOf { it.score } * 100) / totalPoints,
                    shockShare = if (totalTeamShocks > 0) windowWars.totalShocks(player.id) * 100 / totalTeamShocks else 0,
                    winrate = stats.allTimeForm?.winrate ?: 0,
                    isMe = player.id == meId
                )
            }
            .sortedByDescending { it.pointsShare }
    }
}
