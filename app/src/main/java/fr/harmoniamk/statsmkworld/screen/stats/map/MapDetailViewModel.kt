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

    /** Un pilote de l'équipe classé sur ce circuit (score perso moyen + winrate). */
    data class PilotRanking(
        val player: PlayerEntity,
        // Score perso moyen (points) sur le circuit — critère de tri.
        val averageScore: Int,
        val winrate: Int
    )

    data class State(
        val loading: Boolean = true,
        val isIndiv: Boolean = false,
        val maps: List<Maps> = listOf(),
        val mapStats: MapStats? = null,
        // Position moyenne du joueur (indiv) OU de l'équipe (équipe) sur ce circuit.
        val averagePositionLabel: String = "-",
        // Score affiché (ton score perso en indiv, score équipe en équipe).
        val averageScore: Int = 0,
        // Classement des pilotes sur ce circuit (du meilleur au pire score moyen).
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
            val userId = when (indiv) {
                true -> dataStoreRepository.mkcPlayer.firstOrNull()?.id?.toString()
                else -> null
            }
            // Manches jouées sur ce circuit (toutes les manches, pour le scope équipe et le
            // classement pilotes ; en indiv on ne garde que celles où le joueur a couru).
            val allTrackDetails = mutableListOf<MapDetails>()
            warDetails.forEach { war ->
                war.warTracks.filter { it.index == trackKey }.forEach { track ->
                    allTrackDetails.add(MapDetails(war = war, warTrack = track, position = null))
                }
            }
            val scopedDetails = when (userId) {
                null -> allTrackDetails
                else -> allTrackDetails.filter { it.warTrack.track.positions.any { pos -> pos.playerId == userId } }
            }
            if (allTrackDetails.isEmpty()) {
                _state.value.copy(loading = false, isIndiv = indiv)
            } else {
                val mapStats = MapStats(list = scopedDetails, userId = userId, is24p = false)
                val maps = allTrackDetails.first().warTrack.track.index.map { Maps.entries[it.toInt()] }
                // Score/position affichés selon le mode.
                val averagePositionLabel = when (userId) {
                    null -> mapStats.teamAveragePosition?.toString() ?: "-"
                    else -> mapStats.averagePlayerPosLabel
                }
                val averageScore = when (userId) {
                    null -> mapStats.teamScore
                    else -> playerAverageScore(scopedDetails, userId)
                }
                _state.value.copy(
                    loading = false,
                    isIndiv = indiv,
                    maps = maps,
                    mapStats = mapStats,
                    averagePositionLabel = averagePositionLabel,
                    averageScore = averageScore,
                    // Le classement des pilotes est TOUJOURS calculé sur toutes les manches
                    // du circuit (indépendant du mode : c'est un classement par pilote).
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

    /** Score perso moyen (points 12p) du joueur sur les manches fournies. */
    private fun playerAverageScore(details: List<MapDetails>, userId: String): Int {
        val points = details
            .mapNotNull { detail -> detail.warTrack.track.positions.firstOrNull { it.playerId == userId }?.position }
            .map { it.positionToPoints(false) }
        return points.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size } ?: 0
    }

    /**
     * Classement des pilotes de l'équipe sur ce circuit, **du meilleur au pire score perso
     * moyen** (points 12p). Winrate perso = manches en top 6 (points > 6) / total. Nom
     * résolu via le cache local des joueurs. Sans seuil d'échantillon : tous les pilotes
     * ayant couru le circuit apparaissent (le classement complet est cherchable).
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
                val averageScore = positions.sumOf { it.positionToPoints(false) } / positions.size
                val wonCount = positions.count { it.positionToPoints(false) > 6 }
                val winrate = (wonCount * 100) / positions.size
                PilotRanking(player = player, averageScore = averageScore, winrate = winrate)
            }
            .sortedByDescending { it.averageScore }
    }
}
