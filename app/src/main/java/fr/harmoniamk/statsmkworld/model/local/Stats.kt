package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.extension.sum
import fr.harmoniamk.statsmkworld.extension.trackScoreToDiff
import fr.harmoniamk.statsmkworld.extension.warScoreToDiff
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

data class Stats(
    val warStats: WarStats,
    val mostPlayedTeam: List<TeamStats>?,
    val mostDefeatedTeam: List<TeamStats>?,
    val lessDefeatedTeam: List<TeamStats>?,
    val warScores: List<WarScore>,
    val maps: List<TrackStats>,
    val averageForMaps: List<TrackStats>,
) {
    val highestScore: WarScore? = warScores.maxByOrNull { it.score }
    val lowestScore: WarScore? = warScores.minByOrNull { it.score }
    val bestMap: TrackStats? =
        maps.filter { it.totalPlayed >= 2 }.maxByOrNull { it.teamScore ?: 0 }
    val worstMap: TrackStats? =
        maps.filter { it.totalPlayed >= 2 }.minByOrNull { it.teamScore ?: 0 }
    val bestPlayerMap: TrackStats? =
        maps.filter { it.totalPlayed >= 2 }.maxByOrNull { it.playerScore ?: 0 }
    val worstPlayerMap: TrackStats? =
        maps.filter { it.totalPlayed >= 2 }.minByOrNull { it.playerScore ?: 0 }
    val mostPlayedMap: TrackStats? =
        maps.filter { it.totalPlayed >= 2 }.maxByOrNull { it.totalPlayed }
    val averagePoints: Int =
        warScores.sumOf { it.score } / (warScores.takeIf { it.isNotEmpty() }?.size ?: 1)
    val averagePointsLabel: String = averagePoints.warScoreToDiff()
    val averageMapPoints: Int =
        (averageForMaps.map { it.teamScore }.sum() / (averageForMaps.takeIf { it.isNotEmpty() }?.size ?: 1))
    val averagePlayerPosition: List<Int> =
        (averageForMaps.map { it.playerScore }.sum() / (averageForMaps.takeIf { it.isNotEmpty() }?.size
            ?: 1)).pointsToPosition(warStats.is24p)

    val averagePlayerPosLabel = when (val single = averagePlayerPosition.singleOrNull()) {
        null -> "${averagePlayerPosition.firstOrNull()} - ${averagePlayerPosition.lastOrNull()}"
        else -> single.toString()
    }

    val averageMapPointsLabel = averageMapPoints.trackScoreToDiff()
    val mapsWon = "${averageForMaps.filter { (it.teamScore ?: 0) > 41 }.size} / ${averageForMaps.size}"
    val shockCount = averageForMaps.map { it.shockCount }.sum()
    var highestPlayerScore: Pair<Int, String?>? = null
    var lowestPlayerScore: Pair<Int, String?>? = null
}

class WarScore(
    val war: WarDetails,
    val score: Int
)

data class TrackStats(
    val stats: Stats? = null,
    val map: List<Maps>? = null,
    val trackIndex: List<Int>? = null,
    val teamScore: Int? = null,
    val playerScore: Int? = null,
    val totalPlayed: Int = 0,
    val winRate: Int? = null,
    val shockCount: Int? = null
)

data class TeamStats(val team: TeamEntity?, val totalPlayed: Int?)

data class WarStats(val list: List<WarDetails>, val is24p: Boolean = false) {
    val warsPlayed = list.count()
    val warsWon = when (is24p) {
        true -> list.count { it.war.scores.sortedByDescending { it.score }.safeSubList(0, 2).map { it.teamId }.contains(it.war.teamHost) }
        else -> list.count { war -> war.displayedDiff.contains('+') }
    }

    val warsTied = list.count { war -> war.displayedDiff == "0" }
    val warsLoss = when (is24p) {
        true -> list.count { it.war.scores.sortedBy { it.score }.safeSubList(0, 2).map { it.teamId }.contains(it.war.teamHost) }
        else -> list.count { war -> war.displayedDiff.contains('-') }
    }
    val highestVictory = when (is24p) {
        true -> list.maxByOrNull { details ->  details.war.scores.firstOrNull { it.teamId == details.war.teamHost }?.score ?: 0 }
        else -> list.maxByOrNull { war -> war.scoreHost }.takeIf { it?.displayedDiff?.contains("+") == true }

    }
    val loudestDefeat = when (is24p) {
        true -> list.minByOrNull { details ->  details.war.scores.firstOrNull { it.teamId == details.war.teamHost }?.score ?: 0 }
        else ->  list.minByOrNull { war -> war.scoreHost }.takeIf { it?.displayedDiff?.contains("-") == true }
    }

}


data class MapDetails(
    val war: WarDetails,
    val warTrack: WarTrackDetails,
    val position: Int?
)

@FlowPreview
@ExperimentalCoroutinesApi
class MapStats(
    val list: List<MapDetails>,
    val userId: String? = null,
    val is24p: Boolean
) {

    private val isIndiv = userId != null
    private val playerScoreList = list
        .filter { pair -> pair.war.warTracks.any { it.track.hasPlayer(userId) } }
        .map { it.warTrack.track.positions }
        .map { it.singleOrNull { it.playerId == userId } }
        .mapNotNull { it?.position.positionToPoints(is24p) }
    val trackPlayed =
        list.filter { (isIndiv && it.war.warTracks.any { it.track.hasPlayer(userId) }) || !isIndiv }.size
    val trackWon = list
        .filter { pair -> pair.warTrack.displayedDiff.contains('+') }
        .filter { (isIndiv && it.war.warTracks.any { it.track.hasPlayer(userId) }) || !isIndiv }
        .size
    val trackTie = list
        .filter { pair -> pair.warTrack.displayedDiff == "0" }.count {
            (isIndiv && it.war.warTracks.any {
                it.track.hasPlayer(userId)
            }) || !isIndiv
        }
    val trackLoss = list
        .filter { pair -> pair.warTrack.displayedDiff.contains('-') }.count {
            (isIndiv && it.war.warTracks.any {
                it.track.hasPlayer(userId)
            }) || !isIndiv
        }
    val teamScore = list.map { pair -> pair.warTrack }.map { it.teamScore }.sum() / (list.takeIf { it.isNotEmpty() }?.size ?: 1)
    val playerPosition: List<Int> =  (playerScoreList.sum() / (playerScoreList.takeIf { it.isNotEmpty() }?.size
        ?: 1)).pointsToPosition(is24p)

    val averagePlayerPosLabel = when (val single = playerPosition.singleOrNull()) {
        null -> "${playerPosition.firstOrNull()} - ${playerPosition.lastOrNull()}"
        else -> single.toString()
    }
    val topsTable = listOf(
        Pair(
            "Top 6",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it <= 6 }.size == 6
            }.size
        ),
        Pair(
            "Top 5",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it <= 5 }.size == 5
            }.size
        ),
        Pair(
            "Top 4",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it <= 4 }.size == 4
            }.size
        ),
        Pair(
            "Top 3",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it <= 3 }.size == 3
            }.size
        ),
        Pair(
            "Top 2",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it <= 2 }.size == 2
            }.size
        ),
    )
    val bottomsTable = listOf(
        Pair(
            "Bot 6",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it >= 7 }.size == 6
            }.size
        ),
        Pair(
            "Bot 5",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it >= 8 }.size == 5
            }.size
        ),
        Pair(
            "Bot 4",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it >= 9 }.size == 4
            }.size
        ),
        Pair(
            "Bot 3",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it >= 10 }.size == 3
            }.size
        ),
        Pair(
            "Bot 2",
            list.filter {
                !isIndiv && it.warTrack.track.positions.map { it.position }.filter { it >= 11 }.size == 2
            }.size
        ),
    )

    val indivTopsTable = listOf(
        Pair(
            "1",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 1 }?.playerId == userId }.size
        ),
        Pair(
            "2",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 2 }?.playerId == userId }.size
        ),
        Pair(
            "3",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 3 }?.playerId == userId }.size
        ),
        Pair(
            "4",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 4 }?.playerId == userId }.size
        ),
        Pair(
            "5",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 5 }?.playerId == userId }.size
        ),
        Pair(
            "6",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 6 }?.playerId == userId }.size
        ),
    )
    val indivBottomsTable = listOf(
        Pair(
            "7",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 7 }?.playerId == userId }.size
        ),
        Pair(
            "8",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 8 }?.playerId == userId }.size
        ),
        Pair(
            "9",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 9 }?.playerId == userId }.size
        ),
        Pair(
            "10",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 10 }?.playerId == userId }.size
        ),
        Pair(
            "11",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 11 }?.playerId == userId }.size
        ),
        Pair(
            "12",
            list.filter { isIndiv && it.warTrack.track.positions.singleOrNull { it.position == 12 }?.playerId == userId }.size
        ),
    )
    val shockCount = list.map {
        it.warTrack.track.shocks?.filter { (isIndiv && it.playerId == userId) || !isIndiv }
            ?.sumOf { it.count }
    }.sum()

}