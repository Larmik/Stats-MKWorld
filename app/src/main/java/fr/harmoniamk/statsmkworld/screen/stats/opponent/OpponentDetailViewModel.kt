package fr.harmoniamk.statsmkworld.screen.stats.opponent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.extension.totalShocks
import fr.harmoniamk.statsmkworld.extension.withFullStats
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import fr.harmoniamk.statsmkworld.screen.stats.ranking.SortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Fiche détail d'un ADVERSAIRE (`opp` du prototype, pôle Classements #27). Deux modes
 * (rule 11, sélecteur `MKSegmentedSelector`) : **Équipe** (toutes les wars face à cet
 * adversaire) et **Individuel** (les wars du joueur courant face à eux). Le mode est un
 * état interne réactif ([isIndiv]) semé par [initialUserId] (non-null ⇒ Individuel) ; le
 * toggle bascule les données SANS re-navigation (l'écran reste monté). 12p uniquement.
 *
 * Le [teamId] est un identifiant d'opposant (rosterId, ou teamId legacy). L'affichage du
 * nom/tag suit le roster ciblé et l'avatar l'équipe parente (rule 12).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = OpponentDetailViewModel.Factory::class)
class OpponentDetailViewModel @AssistedInject constructor(
    @Assisted val teamId: String,
    @Assisted("initialUserId") val initialUserId: String?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            teamId: String,
            @Assisted("initialUserId") initialUserId: String?
        ): OpponentDetailViewModel
    }

    /** Un pilote de l'équipe classé face à cet adversaire (12p). */
    data class PilotRanking(
        val player: PlayerEntity,
        // Score perso moyen (points) face à l'adversaire — critère de TRI et valeur affichée.
        val averageScore: Int,
        // Position moyenne réelle (1..12) face à l'adversaire — info secondaire affichée.
        val averagePosition: Int,
        // Nombre de manches courues face à l'adversaire (seuil MIN_RANKING_SAMPLE).
        val played: Int,
        val winrate: Int
    )

    /**
     * Un baggeur de l'équipe classé face à cet adversaire (#69) : part de shocks = ses
     * shocks / total shocks de l'équipe face à eux (ratio TOTAL/TOTAL — critère de TRI et
     * valeur affichée). [shockCount] = nb de shocks obtenus ; [played] = nb de wars vs eux.
     */
    data class BaggerRanking(
        val player: PlayerEntity,
        val shockShare: Int,
        val shockCount: Int,
        val played: Int
    )

    data class State(
        val loading: Boolean = true,
        // Mode courant : true = Individuel (joueur courant), false = Équipe.
        val isIndiv: Boolean = false,
        val team: TeamEntity? = null,
        val stats: Stats? = null,
        // Date de la dernière confrontation (la plus récente).
        val lastMeeting: String? = null,
        // 5 dernières confrontations, plus récente en dernier (V=1 / N=0 / D=-1).
        val recentOutcomes: List<Int> = listOf(),
        // Différence de score moyenne (pour − contre) par war, pénalités incluses (mode Équipe).
        val averageScoreDiff: Int = 0,
        // Score moyen du JOUEUR courant contre cet adversaire (points) — mode Individuel.
        val playerAverageScore: Int = 0,
        // Nombre de shocks obtenus (par le joueur en indiv, par l'équipe sinon).
        val shockCount: Int = 0,
        // Ratio shocks obtenus / war (affiché entre parenthèses).
        val shocksPerWar: Float = 0f,
        // Tri courant des circuits (Occurrences / Winrate / Score moy. — comme Classements).
        val tracksSort: SortType = SortType.COUNT,
        // Top3 / Flop3 des circuits joués contre eux (selon [tracksSort]).
        val topTracks: List<TrackStats> = listOf(),
        val flopTracks: List<TrackStats> = listOf(),
        // Classement complet des circuits joués contre eux (selon [tracksSort]).
        val allTracks: List<TrackStats> = listOf(),
        // Stats de manche (Top/Bot 2→6, distribution) scopées à l'adversaire et au mode.
        val mapStats: MapStats? = null,
        // Classement des pilotes (MEMBRES) ayant joué contre cet adversaire (du meilleur au
        // pire score perso moyen) — affiché en mode ÉQUIPE uniquement (#67).
        val pilots: List<PilotRanking> = listOf(),
        // Classement des baggeurs (MEMBRES) face à cet adversaire par part de shocks (#69),
        // affiché en mode ÉQUIPE uniquement.
        val baggers: List<BaggerRanking> = listOf(),
        // Historique des wars face à eux (plus récente en premier).
        val history: List<WarDetails> = listOf()
    )

    private val _state = MutableStateFlow(State(isIndiv = initialUserId != null))
    // Mode réactif (rule 11) : semé par initialUserId, basculé par onModeChange.
    private val isIndiv = MutableStateFlow(initialUserId != null)
    // Tri réactif des circuits (Occurrences par défaut, comme l'écran Classements).
    private val tracksSort = MutableStateFlow(SortType.COUNT)

    val state = databaseRepository.getWars()
        .map { wars ->
            wars
                .filter { it.hasTeam(teamId) }
                // 12p uniquement (24p relève d'un ticket dédié).
                .filter { it.teamOpponent.size == 1 }
                .map { WarDetails(War(it)) }
        }
        .combine(isIndiv) { wars, indiv -> wars to indiv }
        .flatMapLatest { (wars, indiv) ->
            // userId courant seulement en mode Individuel.
            val userId = when (indiv) {
                true -> dataStoreRepository.mkcPlayer.firstOrNull()?.id?.toString()
                else -> null
            }
            // En indiv, ne garder que les wars où le joueur courant a joué.
            val scopedWars = when (userId) {
                null -> wars
                else -> wars.filter { it.war.hasPlayer(userId) }
            }
            scopedWars.withFullStats(databaseRepository, teamId = teamId, userId = userId)
                .map { stats -> Triple(scopedWars, indiv, Pair(userId, stats)) }
        }
        .combine(tracksSort) { data, sort -> data to sort }
        .map { (data, sort) ->
            // Calcul CPU-lourd (MapStats, tri des circuits, classements pilotes/baggeurs,
            // historique) déporté sur `Dispatchers.Default` via `withContext` — et NON `flowOn`
            // (cf. rule 21, #73). `getTeam` (Room) n'est pas main-affine, sûr sur Default.
            withContext(Dispatchers.Default) {
            val (wars, indiv, userIdAndStats) = data
            val (userId, stats) = userIdAndStats
            // teamId peut être un rosterId : avatar de l'équipe parente, nom/tag du roster.
            val team = databaseRepository.getTeam(teamId)?.let { resolved ->
                val roster = resolved.rosters.firstOrNull { it.id == teamId }
                resolved.copy(
                    id = teamId,
                    name = roster?.name ?: resolved.name,
                    tag = roster?.tag ?: resolved.tag
                )
            } ?: TeamEntity(id = teamId, name = "Équipe inconnue", tag = "???", color = null, logo = null)

            // Wars triées chronologiquement (war.id = timestamp).
            val chronological = wars.sortedBy { it.war.id }
            val recentOutcomes = chronological.takeLast(5).map { war ->
                when {
                    war.displayedDiff.contains('+') -> 1
                    war.displayedDiff.contains('-') -> -1
                    else -> 0
                }
            }
            // Score moyen = DIFFÉRENCE (pour − contre) par war, pénalités incluses.
            val averageDiff = chronological
                .map { it.scoreHostWithPenalties - it.scoreOpponentWithPenalties }
                .takeIf { it.isNotEmpty() }?.let { it.sum() / it.size } ?: 0

            // Stats de manche (équipe OU joueur selon le mode) sur toutes les manches face
            // à eux : Top/Bot 2→6, distribution des positions, shocks obtenus.
            val mapDetails = chronological.flatMap { war ->
                war.warTracks.map { track -> MapDetails(war = war, warTrack = track, position = null) }
            }
            val mapStats = mapDetails.takeIf { it.isNotEmpty() }
                ?.let { MapStats(list = it, userId = userId, is24p = false) }

            // Shocks obtenus (scopés au mode par MapStats) + ratio par war.
            val shockCount = mapStats?.shockCount ?: 0
            val warsPlayed = chronological.size.takeIf { it > 0 } ?: 1
            val shocksPerWar = shockCount.toFloat() / warsPlayed

            // Circuits triés selon le sélecteur (Occurrences / Winrate / Score moy.), même
            // logique que l'écran Classements (rule 16). Score = perso en indiv, équipe sinon.
            val sortedTracks = stats.maps.sortedWith(trackComparator(sort, userId != null))

            // Classement des pilotes (MEMBRES) face à cet adversaire — indépendant du mode
            // (classement par pilote), affiché en mode ÉQUIPE uniquement côté UI (#67).
            val pilots = computePilots(chronological)
            // Classement des baggeurs (MEMBRES) face à cet adversaire (#69) — part de shocks
            // (total/total), indépendant du mode, affiché en mode ÉQUIPE uniquement côté UI.
            val baggers = computeBaggers(chronological)

            _state.value.copy(
                loading = false,
                isIndiv = indiv,
                team = team,
                stats = stats,
                lastMeeting = chronological.lastOrNull()?.date,
                recentOutcomes = recentOutcomes,
                averageScoreDiff = averageDiff,
                // En indiv, stats.averagePoints = score moyen du JOUEUR (withFullStats userId).
                playerAverageScore = stats.averagePoints,
                shockCount = shockCount,
                shocksPerWar = shocksPerWar,
                tracksSort = sort,
                topTracks = sortedTracks.take(3),
                flopTracks = sortedTracks.takeLast(3).reversed(),
                allTracks = sortedTracks,
                mapStats = mapStats,
                pilots = pilots,
                baggers = baggers,
                history = chronological.reversed()
            )
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /** Bascule Indiv/Équipe (rule 11) : met à jour l'état réactif, l'écran se recompose. */
    fun onModeChange(indiv: Boolean) {
        isIndiv.value = indiv
    }

    /** Change le tri des circuits (index du sélecteur = ordre de `SortType.entries`). */
    fun onTracksSortSelected(index: Int) {
        tracksSort.value = SortType.entries.getOrElse(index) { SortType.COUNT }
    }

    /**
     * Classement des pilotes de l'équipe face à cet adversaire — **calculé UNIQUEMENT sur les
     * wars jouées contre cet adversaire** ([wars] est déjà filtré `hasTeam(teamId)` + 12p, cf.
     * #67 round 3). Pour CHAQUE pilote, on n'agrège que SES manches dans ces wars :
     * - `played` = **nombre de wars** (distinctes) vs cet adversaire où le pilote a couru ;
     * - `winrate` = manches en top 6 (points > 6) / total de SES manches vs cet adversaire ;
     * - `averagePosition` = position moyenne sur SES manches vs cet adversaire ;
     * - `averageScore` = score perso moyen (critère de tri).
     *
     * **Alliés exclus** (rosterId « -1 ») : membres uniquement. **Seuil** [Stats.MIN_RANKING_SAMPLE]
     * (en manches) aligné sur les autres rankings. Rule 32 : logique mono-consommateur, non extraite.
     */
    private suspend fun computePilots(wars: List<WarDetails>): List<PilotRanking> {
        if (wars.isEmpty()) return listOf()
        // Positions du pilote (manches) ET nb de wars distinctes, agrégées SUR CES WARS
        // (déjà restreintes à l'adversaire) — pas de fuite hors-adversaire possible.
        val positionsByPlayer = mutableMapOf<String, MutableList<Int>>()
        val warsByPlayer = mutableMapOf<String, Int>()
        wars.forEach { war ->
            val playersInWar = mutableSetOf<String>()
            war.warTracks.forEach { track ->
                track.track.positions.forEach { position ->
                    positionsByPlayer.getOrPut(position.playerId) { mutableListOf() }.add(position.position)
                    playersInWar.add(position.playerId)
                }
            }
            playersInWar.forEach { playerId -> warsByPlayer[playerId] = (warsByPlayer[playerId] ?: 0) + 1 }
        }
        if (positionsByPlayer.isEmpty()) return listOf()

        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        return positionsByPlayer
            .mapNotNull { (playerId, positions) ->
                val player = players.firstOrNull { it.id == playerId } ?: return@mapNotNull null
                // Exclure les alliés (rosterId sentinelle « -1 ») — membres uniquement.
                if (player.rosterId == "-1") return@mapNotNull null
                if (positions.size < Stats.MIN_RANKING_SAMPLE) return@mapNotNull null
                val averageScore = positions.sumOf { it.positionToPoints(false) } / positions.size
                val averagePosition = positions.sum() / positions.size
                val wonCount = positions.count { it.positionToPoints(false) > 6 }
                val winrate = (wonCount * 100) / positions.size
                PilotRanking(
                    player = player,
                    averageScore = averageScore,
                    averagePosition = averagePosition,
                    // Nombre de WARS vs cet adversaire (distinctes) où le pilote a couru.
                    played = warsByPlayer[playerId] ?: 0,
                    winrate = winrate
                )
            }
            .sortedByDescending { it.averageScore }
    }

    /**
     * Classement des baggeurs de l'équipe face à cet adversaire (#69) — **calculé sur les
     * wars jouées contre cet adversaire** ([wars] déjà filtré `hasTeam(teamId)` + 12p). Part
     * de shocks de chaque membre = ses shocks / total shocks de l'ÉQUIPE face à eux (ratio
     * TOTAL/TOTAL, jamais une moyenne). **Alliés exclus** (rosterId « -1 ») ; on ne garde que
     * les baggeurs ayant au moins un shock (0 % n'a pas de sens dans un classement de bag).
     * Rule 32 : logique mono-consommateur, non extraite.
     */
    private suspend fun computeBaggers(wars: List<WarDetails>): List<BaggerRanking> {
        val totalTeamShocks = wars.totalShocks().takeIf { it > 0 } ?: return listOf()
        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        // Nb de wars vs cet adversaire où chaque joueur a couru (pour l'info « joué »).
        val warsByPlayer = mutableMapOf<String, Int>()
        wars.forEach { war ->
            war.war.tracks
                .flatMap { it.positions }
                .map { it.playerId }
                .toSet()
                .forEach { playerId -> warsByPlayer[playerId] = (warsByPlayer[playerId] ?: 0) + 1 }
        }
        return warsByPlayer.keys
            .mapNotNull { playerId ->
                val player = players.firstOrNull { it.id == playerId } ?: return@mapNotNull null
                // Membres uniquement (alliés = rosterId sentinelle « -1 »).
                if (player.rosterId == "-1") return@mapNotNull null
                val shockCount = wars.totalShocks(playerId)
                if (shockCount == 0) return@mapNotNull null
                BaggerRanking(
                    player = player,
                    shockShare = shockCount * 100 / totalTeamShocks,
                    shockCount = shockCount,
                    played = warsByPlayer[playerId] ?: 0
                )
            }
            .sortedByDescending { it.shockShare }
    }

    /**
     * Comparateur de circuits selon le tri courant (décroissant) — aligné sur l'écran
     * Classements : Occurrences = nb de fois joué, Winrate = winRate, Score = score perso
     * (indiv) ou d'équipe (équipe).
     */
    private fun trackComparator(sort: SortType, isIndiv: Boolean): Comparator<TrackStats> = when (sort) {
        SortType.WINRATE -> compareByDescending { it.winRate ?: 0 }
        SortType.AVERAGE -> compareByDescending { (if (isIndiv) it.playerScore else it.teamScore) ?: 0 }
        SortType.COUNT -> compareByDescending { it.totalPlayed }
    }
}
