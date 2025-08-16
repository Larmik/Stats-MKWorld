package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import java.io.Serializable

data class WarDetails(val war: War): Serializable {

    val warTracks = war.tracks.map { WarTrackDetails(it) }

    val scoreHost = warTracks.sumOf { it.teamScore }
    private val scoreOpponent = (82 * warTracks.size) - scoreHost
    private val scoreHostWithPenalties = scoreHost - war.penalties.filter { it.teamId == war.teamHost }.sumOf { it.amount }
    private val scoreOpponentWithPenalties = scoreOpponent - war.penalties.filter { it.teamId == war.teamOpponent }.sumOf { it.amount }

    val displayedScore: String
        get() = "$scoreHostWithPenalties - $scoreOpponentWithPenalties"

    val displayedDiff: String
        get() {
            val diff = scoreHostWithPenalties - scoreOpponentWithPenalties
            return if (diff > 0) "+$diff" else "$diff"
        }
}

data class WarTrackDetails(val track: WarTrack): Serializable {

    val index
        get() = track.index

    val teamScore: Int
        get() = track.positions.sumOf { it.position.positionToPoints() }

    private val opponentScore: Int
        get() {
            teamScore.takeIf { it != 0 }?.let {
                return 82 - it
            }
            return 0
        }

    private val diffScore: Int
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