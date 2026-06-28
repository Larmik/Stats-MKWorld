package fr.harmoniamk.statsmkworld.model.firebase

import android.os.Parcelable
import fr.harmoniamk.statsmkworld.model.ScoringConstants
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.local.DatastoreWarTrack
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class WarTrack(
    val id: Long,
    val index: List<String>,
    val positions: List<WarPosition>,
    var shocks: List<Shock>? = null

): Parcelable, Serializable {
    constructor(track: DatastoreWarTrack) : this(
        id = track.id,
        index = track.index.map { it.toString() },
        positions = track.positions,
        shocks = track.shocks
    )

    fun hasPlayer(playerId: String?) = positions.any { pos -> pos.playerId == playerId }

    fun diffScore(is24p: Boolean = false): Int {
        val maxPointsPerTrack = when (is24p) {
            true -> ScoringConstants.MAX_POINTS_PER_TRACK_24P
            else -> ScoringConstants.MAX_POINTS_PER_TRACK_12P
        }
        val teamScore = positions.sumOf { it.position.positionToPoints(is24p) }
        val opponentScore = teamScore.takeIf { it != 0 }?.let {
            maxPointsPerTrack - it
        } ?: 0
        opponentScore.takeIf { it != 0 }?.let {
            return teamScore - it
        }
        return 0
    }




}