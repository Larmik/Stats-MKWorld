package fr.harmoniamk.statsmkworld.screen.stats.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.harmoniamk.statsmkworld.database.entities.PlayerEntity
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.local.MapDetails
import fr.harmoniamk.statsmkworld.model.local.MapStats
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Fiche détail d'un CIRCUIT (`map` du prototype, pôle Classements #27). Vue au périmètre
 * ÉQUIPE (toutes les manches jouées sur ce circuit), 12p. Agrège le [MapStats] existant
 * (winrate, V/N/D de manche, scores moyens, Top6/Bot6) et calcule le « meilleur pilote
 * ici » (winrate perso le plus élevé sur ce circuit).
 *
 * [trackIndex] identifie le circuit (liste d'index de map — 1 pour un circuit classique).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = MapDetailViewModel.Factory::class)
class MapDetailViewModel @AssistedInject constructor(
    @Assisted val trackIndex: List<Int>,
    private val databaseRepository: DatabaseRepositoryInterface
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(trackIndex: List<Int>): MapDetailViewModel
    }

    /** Meilleur pilote de l'équipe sur ce circuit (winrate perso). */
    data class BestPilot(
        val player: PlayerEntity,
        val winrate: Int,
        // Score perso moyen (points) sur le circuit.
        val averageScore: Int
    )

    data class State(
        val loading: Boolean = true,
        val maps: List<Maps> = listOf(),
        val mapStats: MapStats? = null,
        val bestPilot: BestPilot? = null
    )

    private val _state = MutableStateFlow(State())

    private val trackKey = trackIndex.map { it.toString() }

    val state = databaseRepository.getWars()
        .map { wars ->
            wars
                // 12p uniquement (24p relève d'un ticket dédié).
                .filter { it.teamOpponent.size == 1 }
                .map { WarDetails(War(it)) }
        }
        .map { warDetails ->
            // Manches jouées sur ce circuit.
            val mapDetails = mutableListOf<MapDetails>()
            warDetails.forEach { war ->
                war.warTracks.filter { it.index == trackKey }.forEach { track ->
                    mapDetails.add(MapDetails(war = war, warTrack = track, position = null))
                }
            }
            if (mapDetails.isEmpty()) {
                _state.value.copy(loading = false)
            } else {
                val mapStats = MapStats(list = mapDetails, userId = null, is24p = false)
                val maps = mapDetails.first().warTrack.track.index.map { Maps.entries[it.toInt()] }
                _state.value.copy(
                    loading = false,
                    maps = maps,
                    mapStats = mapStats,
                    bestPilot = computeBestPilot(mapDetails)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    /**
     * Meilleur pilote sur le circuit : pour chaque joueur ayant couru ≥
     * [Stats.MIN_RANKING_SAMPLE] manches, winrate perso = manches où sa position est en
     * moitié haute (points > 6 en 12p, cf. définition « top 6 »), départage par score
     * perso moyen. Nom résolu via le cache local des joueurs.
     */
    private suspend fun computeBestPilot(mapDetails: List<MapDetails>): BestPilot? {
        // (playerId -> liste des positions du joueur sur ce circuit).
        val positionsByPlayer = mapDetails
            .flatMap { it.warTrack.track.positions }
            .groupBy({ it.playerId }, { it.position })
            .filter { it.value.size >= Stats.MIN_RANKING_SAMPLE }
        if (positionsByPlayer.isEmpty()) return null

        val players = databaseRepository.getPlayers().firstOrNull().orEmpty()
        return positionsByPlayer
            .mapNotNull { (playerId, positions) ->
                val player = players.firstOrNull { it.id == playerId } ?: return@mapNotNull null
                // Manche gagnée côté joueur = position en top 6 (points > 6 en 12p : P1..P6).
                val wonCount = positions.count { it.positionToPoints(false) > 6 }
                val winrate = (wonCount * 100) / positions.size
                val averageScore = positions.sumOf { it.positionToPoints(false) } / positions.size
                BestPilot(player = player, winrate = winrate, averageScore = averageScore)
            }
            .maxWithOrNull(compareBy({ it.winrate }, { it.averageScore }))
    }
}
