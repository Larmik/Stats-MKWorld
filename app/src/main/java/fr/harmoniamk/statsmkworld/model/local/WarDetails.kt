package fr.harmoniamk.statsmkworld.model.local

import android.os.Parcelable
import fr.harmoniamk.statsmkworld.model.ScoringConstants
import fr.harmoniamk.statsmkworld.extension.displayedString
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarScore
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.util.Date


@Parcelize
data class WarDetails(val war: War): Serializable, Parcelable {



    val date = Date(war.id).displayedString("dd/MM/yyyy")

    val warTracks = war.tracks.map { WarTrackDetails(it, war.teamOpponent.size > 1) }


    /**
     *  12 players
     */
    val scoreHost = warTracks.sumOf { it.teamScore }
    val scoreOpponent = (ScoringConstants.MAX_POINTS_PER_TRACK_12P * warTracks.size) - scoreHost
    val scoreHostWithPenalties = scoreHost - war.penalties.filter { it.teamId == war.teamHost }.sumOf { it.amount }
    val scoreOpponentWithPenalties = scoreOpponent - war.penalties.filter { war.teamOpponent.contains(it.teamId) }.sumOf { it.amount }

    val displayedScore: String = "$scoreHostWithPenalties - $scoreOpponentWithPenalties"

    val displayedDiff: String = (scoreHostWithPenalties - scoreOpponentWithPenalties)
        .let { diff -> if (diff > 0) "+$diff" else "$diff" }

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

    /**
     * Écart de score de la war du point de vue de l'hôte (signé : >0 victoire,
     * <0 défaite, 0 nul), avec pénalités. Sert aux marges moyennes
     * victoire/défaite ([Stats.averageWinMargin] / [Stats.averageLossMargin]).
     *
     * - 12p : écart hôte − adversaire unique (somme des points de positions).
     * - 24p : le score vient de [War.scores] (saisi via WarScore, 3 équipes) ;
     *   la marge est l'écart entre le score de l'hôte et le MEILLEUR score
     *   adverse (référence de podium), pénalités nettées par équipe — cohérent
     *   avec la détermination victoire/défaite de [WarStats] (podium top/bottom 2).
     */
    fun scoreMargin(is24p: Boolean = false): Int = when (is24p) {
        false -> scoreHostWithPenalties - scoreOpponentWithPenalties
        true -> {
            val hostScore = war.scores.firstOrNull { it.teamId == war.teamHost }
                ?.let { penaltyAdjustedScore(it) } ?: 0
            val bestOpponent = war.scores
                .filter { war.teamOpponent.contains(it.teamId) }
                .maxOfOrNull { penaltyAdjustedScore(it) } ?: 0
            hostScore - bestOpponent
        }
    }

    /** Score d'une équipe (24p) net de ses pénalités. */
    private fun penaltyAdjustedScore(score: WarScore): Int =
        score.score - war.penalties.filter { it.teamId == score.teamId }.sumOf { it.amount }
}

@Parcelize
data class WarTrackDetails(val track: WarTrack, val is24p: Boolean): Parcelable, Serializable {

    val index
        get() = track.index

    val teamScore: Int = track.positions.sumOf { it.position.positionToPoints(is24p) }

    private val opponentScore: Int = run {
        val maxPointsPerTrack = when (is24p) {
            true -> ScoringConstants.MAX_POINTS_PER_TRACK_24P
            else -> ScoringConstants.MAX_POINTS_PER_TRACK_12P
        }
        teamScore.takeIf { it != 0 }?.let { maxPointsPerTrack - it } ?: 0
    }

    private val diffScore: Int =
        opponentScore.takeIf { it != 0 }?.let { teamScore - it } ?: 0

    val displayedResult: String = "$teamScore - $opponentScore"

    val displayedDiff: String = if (diffScore > 0) "+$diffScore" else "$diffScore"

    /**
     * Résultat de la manche 12p du point de vue de l'équipe hôte : +1 gagnée,
     * -1 perdue, 0 égalité (ou manche sans score). Sert au calcul des séries
     * par circuit ([Stats.streaksByTrack]).
     */
    fun trackOutcome(): Int {
        val opponent = teamScore.takeIf { it != 0 }
            ?.let { ScoringConstants.MAX_POINTS_PER_TRACK_12P - it } ?: return 0
        return when {
            teamScore > opponent -> 1
            teamScore < opponent -> -1
            else -> 0
        }
    }
}