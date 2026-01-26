package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.OldWar
import fr.harmoniamk.statsmkworld.model.firebase.OldWarTrack
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import java.io.Serializable
import java.util.Date


data class WarDetails(val war: War): Serializable {

    constructor(oldWar: OldWar) : this(
        war = War(
            oldWar.id,
            oldWar.teamHost,
            listOf(oldWar.teamOpponent),
            oldWar.tracks.map { WarTrack(it) },
            oldWar.penalties,
            listOf()
        )
    )

    val date = Date(war.id).displayedString("dd/MM/yyyy")

    val warTracks = war.tracks.map { WarTrackDetails(it, war.teamOpponent.size > 1) }


    /**
     *  12 players
     */
    val scoreHost = warTracks.sumOf { it.teamScore }
    val scoreOpponent = (82 * warTracks.size) - scoreHost
    val scoreHostWithPenalties = scoreHost - war.penalties.filter { it.teamId == war.teamHost }.sumOf { it.amount }
    val scoreOpponentWithPenalties = scoreOpponent - war.penalties.filter { war.teamOpponent.contains(it.teamId) }.sumOf { it.amount }

    val displayedScore: String
        get() = "$scoreHostWithPenalties - $scoreOpponentWithPenalties"

    val displayedDiff: String
        get() {
            val diff = scoreHostWithPenalties - scoreOpponentWithPenalties
            return if (diff > 0) "+$diff" else "$diff"
        }

    /**
     *  24 players
     */

    val scores = when (war.scores.isEmpty()) {
        true -> (listOf(war.teamHost) + war.teamOpponent).map { WarScore(teamId = it, score = 0) }
        else -> war.scores.sortedByDescending { it.score }
    }


    val diffs =
        when (war.scores.isEmpty()) {
            true -> listOf("0", "0", "0")
            else -> scores.mapIndexedNotNull { index, current ->
                scores.getOrNull(index + 1)?.let { next ->
                    "+${current.score - next.score}"
                }
        }
    }
}

data class WarTrackDetails(val track: WarTrack, val is24p: Boolean): Serializable {

    val index
        get() = track.index

    val teamScore: Int
        get() = track.positions.sumOf { it.position.positionToPoints(is24p) }

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