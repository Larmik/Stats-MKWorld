package fr.harmoniamk.statsmkworld.model.local

import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.extension.pointsToPosition
import fr.harmoniamk.statsmkworld.extension.positionToPoints
import fr.harmoniamk.statsmkworld.extension.safeSubList
import fr.harmoniamk.statsmkworld.extension.sizeOrOne
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
    private val mapsAboveThreshold: List<TrackStats> = maps.filter { it.totalPlayed >= 2 }

    val highestScore: WarScore? = warScores.maxByOrNull { it.score }
    val lowestScore: WarScore? = warScores.minByOrNull { it.score }
    val bestMap: TrackStats? =
        mapsAboveThreshold.maxByOrNull { it.teamScore ?: 0 }
    val worstMap: TrackStats? =
        mapsAboveThreshold.minByOrNull { it.teamScore ?: 0 }
    val bestPlayerMap: TrackStats? =
        mapsAboveThreshold.maxByOrNull { it.playerScore ?: 0 }
    val worstPlayerMap: TrackStats? =
        mapsAboveThreshold.minByOrNull { it.playerScore ?: 0 }
    val mostPlayedMap: TrackStats? =
        mapsAboveThreshold.maxByOrNull { it.totalPlayed }
    val averagePoints: Int =
        warScores.sumOf { it.score } / warScores.sizeOrOne()
    val averagePointsLabel: String = averagePoints.warScoreToDiff(warStats.is24p)
    val averageMapPoints: Int =
        (averageForMaps.map { it.teamScore }.sum() / averageForMaps.sizeOrOne())
    val averagePlayerPosition: List<Int> =
        (averageForMaps.map { it.playerScore }.sum() / averageForMaps.sizeOrOne())
            .pointsToPosition(warStats.is24p)

    val averagePlayerPosLabel = when (val single = averagePlayerPosition.singleOrNull()) {
        null -> "${averagePlayerPosition.firstOrNull()} - ${averagePlayerPosition.lastOrNull()}"
        else -> single.toString()
    }

    val mapsWon = averageForMaps.takeIf { it.isNotEmpty() }?.let {
        "${(it.filter { (it.teamScore ?: 0) > 41 }.size * 100 / it.size)}%"
    }

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
    val teamScore = list.map { it.warTrack.teamScore }.sum() / list.sizeOrOne()
    val playerPosition: List<Int> =
        (playerScoreList.sum() / playerScoreList.sizeOrOne()).pointsToPosition(is24p)

    val averagePlayerPosLabel = when (val single = playerPosition.singleOrNull()) {
        null -> "${playerPosition.firstOrNull()} - ${playerPosition.lastOrNull()}"
        else -> single.toString()
    }
    // Tables d'équipe : pour chaque top/bottom N, on compte les manches où les N
    // meilleures/pires positions sont toutes dans le seuil. Une seule passe sur la liste.
    val topsTable = when {
        isIndiv -> (6 downTo 2).map { "Top $it" to 0 }
        else -> (6 downTo 2).map { n ->
            "Top $n" to list.count { it.warTrack.track.positions.count { pos -> pos.position <= n } == n }
        }
    }
    val bottomsTable = when {
        isIndiv -> (6 downTo 2).map { "Bot $it" to 0 }
        else -> (6 downTo 2).map { n ->
            // seuil bas : Bot 6 -> >=7, Bot 5 -> >=8, … Bot 2 -> >=11
            val threshold = 13 - n
            "Bot $n" to list.count { it.warTrack.track.positions.count { pos -> pos.position >= threshold } == n }
        }
    }

    // Tables individuelles : nombre de manches où le joueur a fini à la position N.
    val indivTopsTable = (1..6).map { n ->
        n.toString() to when {
            isIndiv -> list.count { it.warTrack.track.positions.singleOrNull { pos -> pos.position == n }?.playerId == userId }
            else -> 0
        }
    }
    val indivBottomsTable = (7..12).map { n ->
        n.toString() to when {
            isIndiv -> list.count { it.warTrack.track.positions.singleOrNull { pos -> pos.position == n }?.playerId == userId }
            else -> 0
        }
    }
    val shockCount = list.map {
        it.warTrack.track.shocks?.filter { (isIndiv && it.playerId == userId) || !isIndiv }
            ?.sumOf { it.count }
    }.sum()

}