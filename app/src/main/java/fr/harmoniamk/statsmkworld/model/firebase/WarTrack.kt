package fr.harmoniamk.statsmkworld.model.firebase

import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.local.DatastoreWarTrack

data class WarTrack(
    val id: Long,
    val index: Int,
    val positions: List<WarPosition>,
    var shocks: List<Shock>? = null

) {
    constructor(track: DatastoreWarTrack) : this(
        id = track.id,
        index = track.index,
        positions = track.positions,
        shocks = track.shocks
    )

    fun hasPlayer(playerId: String?) = positions.any { pos -> pos.playerId == playerId }

    val teamScore: Int
        get() = positions.sumOf { it.position.positionToPoints() }


    private val opponentScore: Int
        get() {
            teamScore.takeIf { it != 0 }?.let {
                return 82 - it
            }
            return 0
        }

    val diffScore: Int
        get() {
            opponentScore.takeIf { it != 0 }?.let {
                return teamScore - it
            }
            return 0
        }

    val displayedResult: String
        get() = "$teamScore - $opponentScore"

    val displayedDiff: String
        get() = if (diffScore > 0) "+$diffScore" else "$diffScore"


}