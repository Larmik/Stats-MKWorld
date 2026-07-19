package fr.harmoniamk.statsmkworld.screen.stats.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.mergeWith
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Maps
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
        // Score perso moyen (points) sur le circuit — critère de TRI.
        val averageScore: Int,
        // Position moyenne réelle (1..12) sur le circuit — valeur AFFICHÉE.
        val averagePosition: Int,
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
        val pilots: List<PilotRanking> = listOf()
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
                    pilots = computePilots(allTrackDetails)
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
     * moyen** (points 12p). Winrate perso = manches en top 6 (points > 6) / total. Nom
     * résolu via le cache local des joueurs. **Alliés exclus** (rosterId « -1 ») : seuls les
     * MEMBRES figurent. Sans seuil d'échantillon : tous les membres ayant couru le circuit.
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
                val averageScore = positions.sumOf { it.positionToPoints(false) } / positions.size
                val averagePosition = positions.sum() / positions.size
                val wonCount = positions.count { it.positionToPoints(false) > 6 }
                val winrate = (wonCount * 100) / positions.size
                PilotRanking(player = player, averageScore = averageScore, averagePosition = averagePosition, winrate = winrate)
            }
            .sortedByDescending { it.averageScore }
    }
}
