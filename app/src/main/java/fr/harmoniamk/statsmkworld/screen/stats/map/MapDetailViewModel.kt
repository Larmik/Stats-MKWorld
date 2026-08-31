package fr.harmoniamk.statsmkworld.screen.stats.map

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
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DataStoreRepositoryInterface
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Fiche détail d'un CIRCUIT (`map` du prototype, pôle Classements #27). Deux modes
 * (rule 11, `MKSegmentedSelector`) : **Équipe** (toutes les manches jouées sur ce circuit)
 * et **Individuel** (les manches du joueur courant). Le mode est un état interne réactif
 * ([isIndiv]) semé par [initialUserId] ; le toggle bascule les données SANS re-navigation.
 * 12p uniquement.
 *
 * [trackIndex] identifie le circuit (liste d'index de map — 1 pour un circuit classique).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = MapDetailViewModel.Factory::class)
class MapDetailViewModel @AssistedInject constructor(
    @Assisted val trackIndex: List<Int>,
    @Assisted("initialUserId") val initialUserId: String?,
    private val databaseRepository: DatabaseRepositoryInterface,
    private val dataStoreRepository: DataStoreRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            trackIndex: List<Int>,
            @Assisted("initialUserId") initialUserId: String?
        ): MapDetailViewModel
    }

    /** Un pilote de l'équipe classé sur ce circuit. */
    data class PilotRanking(
        val player: PlayerEntity,
        // Score perso moyen (points) sur le circuit — critère de TRI **et** valeur affichée.
        val averageScore: Int,
        // Position moyenne réelle (1..12) sur le circuit — info secondaire affichée.
        val averagePosition: Int,
        // Nombre de manches courues par le pilote sur ce circuit (seuil MIN_RANKING_SAMPLE).
        val played: Int,
        val winrate: Int
    )

    /**
     * Un baggeur de l'équipe classé sur ce circuit (#69) : part de shocks = ses shocks sur
     * ce circuit / total shocks de l'ÉQUIPE sur ce circuit (ratio TOTAL/TOTAL — critère de
     * TRI et valeur affichée). [shockCount] = nb de shocks ; [played] = nb de manches courues.
     */
    data class BaggerRanking(
        val player: PlayerEntity,
        val shockShare: Int,
        val shockCount: Int,
        val played: Int
    )

    /** Un adversaire rencontré sur ce circuit (12p, opposant unique). */
    data class OpponentRanking(
        val team: TeamEntity,
        // Score moyen de l'ÉQUIPE sur le circuit face à cet adversaire — critère de TRI et
        // valeur affichée (via `trackScoreToDiff` à l'affichage).
        val averageTeamScore: Int,
        // Nombre de manches jouées sur ce circuit contre cet adversaire (seuil MIN_RANKING_SAMPLE).
        val played: Int,
        val winrate: Int
    )

    data class State(
        val loading: Boolean = true,
        val isIndiv: Boolean = false,
        val maps: List<Maps> = listOf(),
        val mapStats: MapStats? = null,
        // « Scores moyens » — indépendants du mode (point 4) :
        // score moyen de l'ÉQUIPE et position moyenne du JOUEUR courant sur ce circuit.
        val teamScore: Int = 0,
        val playerPositionLabel: String = "-",
        // Nombre de shocks joués — DYNAMIQUE (suit le mode Indiv/Équipe).
        val shockCount: Int = 0,
        // Classement des pilotes sur ce circuit (du meilleur au pire score moyen), MEMBRES
        // uniquement (alliés exclus).
        val pilots: List<PilotRanking> = listOf(),
        // Classement des baggeurs sur ce circuit par part de shocks (#69), MEMBRES uniquement,
        // indépendant du mode (affiché en mode ÉQUIPE côté UI).
        val baggers: List<BaggerRanking> = listOf(),
        // Classement des adversaires rencontrés sur ce circuit (du meilleur au pire score
        // moyen de l'équipe face à eux) — indépendant du mode.
        val opponents: List<OpponentRanking> = listOf()
    )

    private val _state = MutableStateFlow(State(isIndiv = initialUserId != null))
    private val isIndiv = MutableStateFlow(initialUserId != null)

    private val trackKey = trackIndex.map { it.toString() }

    val state = databaseRepository.getWars()
        .map { wars ->
            wars
                // 12p uniquement (24p relève d'un ticket dédié).
                .filter { it.teamOpponent.size == 1 }
                .map { WarDetails(War(it)) }
        }
        .combine(isIndiv) { warDetails, indiv -> warDetails to indiv }
        .map { (warDetails, indiv) ->
            // Joueur courant : toujours résolu (nécessaire pour la position moyenne du
            // JOUEUR affichée en permanence dans « Scores moyens », point 4). En mode
            // Équipe, il ne scope pas les sections (userId de scope = null).
            val currentUserId = dataStoreRepository.mkcPlayer.firstOrNull()?.id?.toString()
            val scopeUserId = if (indiv) currentUserId else null
            // Manches jouées sur ce circuit (toutes les manches, pour le scope équipe et le
            // classement pilotes ; en indiv on ne garde que celles où le joueur a couru).
            val allTrackDetails = mutableListOf<MapDetails>()
            warDetails.forEach { war ->
                war.warTracks.filter { it.index == trackKey }.forEach { track ->
                    allTrackDetails.add(MapDetails(war = war, warTrack = track, position = null))
                }
            }
            val scopedDetails = when (scopeUserId) {
                null -> allTrackDetails
                else -> allTrackDetails.filter { it.warTrack.track.positions.any { pos -> pos.playerId == scopeUserId } }
            }
            if (allTrackDetails.isEmpty()) {
                _state.value.copy(loading = false, isIndiv = indiv)
            } else {
                // Sections détaillées (distribution/Top-Bot) + shocks : scopées au mode.
                val mapStats = MapStats(list = scopedDetails, userId = scopeUserId, is24p = false)
                val maps = allTrackDetails.first().warTrack.track.index.map { Maps.entries[it.toInt()] }
                // « Scores moyens » figés (point 4) : score d'ÉQUIPE + position du JOUEUR
                // courant, calculés sur TOUTES les manches (indépendants du mode).
                val teamMapStats = MapStats(list = allTrackDetails, userId = currentUserId, is24p = false)
                _state.value.copy(
                    loading = false,
                    isIndiv = indiv,
                    maps = maps,
                    mapStats = mapStats,
                    teamScore = teamMapStats.teamScore,
                    playerPositionLabel = currentUserId
                        ?.let { teamMapStats.averagePlayerPosLabel }
                        ?: (teamMapStats.teamAveragePosition?.toString() ?: "-"),
                    shockCount = mapStats.shockCount,
                    // Classement des pilotes : toutes les manches, MEMBRES uniquement (alliés
                    // exclus), indépendant du mode (classement par pilote).
                    pilots = computePilots(allTrackDetails),
                    // Classement des baggeurs sur ce circuit (#69) : part de shocks (total/total),
                    // toutes les manches, MEMBRES uniquement, indépendant du mode.
                    baggers = computeBaggers(allTrackDetails),
                    // Classement des adversaires rencontrés sur ce circuit (toutes les manches),
                    // indépendant du mode (classement par adversaire).
                    opponents = computeOpponents(allTrackDetails)
                )
            }
        }
        .mergeWith(_state)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /** Bascule Indiv/Équipe (rule 11). */
    fun onModeChange(indiv: Boolean) {
        isIndiv.value = indiv
    }

    /**
     * Classement des pilotes de l'équipe sur ce circuit, **du meilleur au pire score perso
     * moyen** (points 12p) — critère de tri ET valeur affichée (transparence). Winrate perso
     * = manches en top 6 (points > 6) / total. Nom résolu via le cache local des joueurs.
     * **Alliés exclus** (rosterId « -1 ») : seuls les MEMBRES figurent. **Seuil**
     * [Stats.MIN_RANKING_SAMPLE] : un pilote avec trop peu de manches sur ce circuit ne fausse
     * pas le classement (aligné sur les autres rankings).
     */
    private suspend fun computePilots(details: List<MapDetails>): List<PilotRanking> {
        val positionsByPlayer = details
            .flatMap { it.warTrack.track.positions }
            .groupBy({ it.playerId }, { it.position })
        if (positionsByPlayer.isEmpty()) return listOf()

        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        return positionsByPlayer
            .mapNotNull { (playerId, positions) ->
                val player = players.firstOrNull { it.id == playerId } ?: return@mapNotNull null
                // Exclure les alliés (rosterId sentinelle « -1 ») — membres uniquement.
                if (player.rosterId == "-1") return@mapNotNull null
                // Seuil d'échantillon : au moins MIN_RANKING_SAMPLE manches sur ce circuit.
                if (positions.size < Stats.MIN_RANKING_SAMPLE) return@mapNotNull null
                val averageScore = positions.sumOf { it.positionToPoints(false) } / positions.size
                val averagePosition = positions.sum() / positions.size
                val wonCount = positions.count { it.positionToPoints(false) > 6 }
                val winrate = (wonCount * 100) / positions.size
                PilotRanking(
                    player = player,
                    averageScore = averageScore,
                    averagePosition = averagePosition,
                    played = positions.size,
                    winrate = winrate
                )
            }
            .sortedByDescending { it.averageScore }
    }

    /**
     * Classement des baggeurs de l'équipe sur ce circuit (#69) : part de shocks de chaque
     * membre = ses shocks sur ce circuit / total shocks de l'ÉQUIPE sur ce circuit (ratio
     * TOTAL/TOTAL, jamais une moyenne). **Alliés exclus** (rosterId « -1 ») ; on ne garde que
     * les baggeurs ayant au moins un shock. Rule 32 : logique mono-consommateur, non extraite.
     */
    private suspend fun computeBaggers(details: List<MapDetails>): List<BaggerRanking> {
        val allShocks = details.flatMap { it.warTrack.track.shocks.orEmpty() }
        val totalTeamShocks = allShocks.sumOf { it.count }.takeIf { it > 0 } ?: return listOf()
        // Nb de manches courues par chaque joueur sur ce circuit (info « joué »).
        val runsByPlayer = details
            .flatMap { it.warTrack.track.positions }
            .groupingBy { it.playerId }
            .eachCount()
        val shocksByPlayer = allShocks
            .groupBy { it.playerId }
            .mapValues { (_, shocks) -> shocks.sumOf { it.count } }

        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        return shocksByPlayer
            .mapNotNull { (playerId, shockCount) ->
                if (shockCount == 0) return@mapNotNull null
                val player = players.firstOrNull { it.id == playerId } ?: return@mapNotNull null
                // Membres uniquement (alliés = rosterId sentinelle « -1 »).
                if (player.rosterId == "-1") return@mapNotNull null
                BaggerRanking(
                    player = player,
                    shockShare = shockCount * 100 / totalTeamShocks,
                    shockCount = shockCount,
                    played = runsByPlayer[playerId] ?: 0
                )
            }
            .sortedByDescending { it.shockShare }
    }

    /**
     * Classement des adversaires rencontrés sur ce circuit (12p, opposant unique), **du
     * meilleur au pire score moyen d'équipe** face à eux — critère de tri ET valeur affichée
     * (via `trackScoreToDiff` à l'affichage, transparence). Winrate = manches gagnées
     * (`trackOutcome > 0`) / total. Nom/tag du roster + logo de l'équipe parente résolus via
     * le cache local (rule 12, adversaire non résolu dégradé en « Équipe inconnue »).
     * **Seuil** [Stats.MIN_RANKING_SAMPLE] aligné sur le classement pilotes.
     */
    private suspend fun computeOpponents(details: List<MapDetails>): List<OpponentRanking> {
        // 12p : chaque war a un opposant unique. On groupe les manches du circuit par opposant.
        val tracksByOpponent = details
            .mapNotNull { detail -> detail.war.war.teamOpponent.firstOrNull()?.let { it to detail.warTrack } }
            .groupBy({ it.first }, { it.second })
        if (tracksByOpponent.isEmpty()) return listOf()

        return tracksByOpponent
            .mapNotNull { (opponentId, tracks) ->
                if (tracks.size < Stats.MIN_RANKING_SAMPLE) return@mapNotNull null
                val averageTeamScore = tracks.sumOf { it.teamScore } / tracks.size
                val wonCount = tracks.count { it.trackOutcome() > 0 }
                val winrate = (wonCount * 100) / tracks.size
                // Rule 12 : nom/tag du roster, logo de l'équipe parente ; non résolu → dégradé.
                val team = databaseRepository.getTeam(opponentId)?.let { resolved ->
                    val roster = resolved.rosters.firstOrNull { it.id == opponentId }
                    resolved.copy(
                        id = opponentId,
                        name = roster?.name ?: resolved.name,
                        tag = roster?.tag ?: resolved.tag
                    )
                } ?: TeamEntity(id = opponentId, name = "Équipe inconnue", tag = "???", color = null, logo = null)
                OpponentRanking(
                    team = team,
                    averageTeamScore = averageTeamScore,
                    played = tracks.size,
                    winrate = winrate
                )
            }
            .sortedByDescending { it.averageTeamScore }
    }
}
