package fr.harmoniamk.statsmkworld.extension

import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.firebase.Shock
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
import fr.harmoniamk.statsmkworld.model.firebase.OldWarTrack
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TeamStats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarScore
import fr.harmoniamk.statsmkworld.model.local.WarStats
import fr.harmoniamk.statsmkworld.repository.DatabaseRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Suppress("UNCHECKED_CAST")
fun Any?.toMapList(): List<Map<*, *>>? = this as? List<Map<*, *>>

@Deprecated("24 players")
fun List<Map<*, *>>?.parseOldTracks(): List<OldWarTrack>? =
    this?.map { track ->
        OldWarTrack(
            id = track["id"].toString().toLong(),
            index = track["index"].toString().toInt(),
            positions = (track["positions"]?.toMapList())
                ?.map {
                    WarPosition(
                        id = it["id"].toString().toLong(),
                        playerId = it["playerId"].toString(),
                        position = it["position"].toString().toInt()
                    )
                }.orEmpty(),
            shocks = (track["shocks"]?.toMapList())
                ?.map {
                    Shock(
                        playerId = it["playerId"].toString(),
                        count = it["count"].toString().toInt()
                    )
                }
        )
    }

fun List<Map<*, *>>?.parseTracks(): List<WarTrack>? =
    this?.map { track ->
        WarTrack(
            id = track["id"].toString().toLong(),
            index = track["index"] as? List<String> ?: listOf(),
            positions = (track["positions"]?.toMapList())
                ?.map {
                    WarPosition(
                        id = it["id"].toString().toLong(),
                        playerId = it["playerId"].toString(),
                        position = it["position"].toString().toInt()
                    )
                }.orEmpty(),
            shocks = (track["shocks"]?.toMapList())
                ?.map {
                    Shock(
                        playerId = it["playerId"].toString(),
                        count = it["count"].toString().toInt()
                    )
                }
        )
    }

fun List<Map<*, *>>?.parsePenalties(): List<WarPenalty>? =
    this?.map { item ->
        WarPenalty(
            teamId = item["teamId"].toString(),
            amount = item["amount"].toString().toInt()
        )
    }
fun List<Map<*, *>>?.parseScores(): List<fr.harmoniamk.statsmkworld.model.firebase.WarScore>? =
    this?.map { item ->
        fr.harmoniamk.statsmkworld.model.firebase.WarScore(
            teamId = item["teamId"].toString(),
            score = item["score"].toString().toInt()
        )
    }

fun <T> List<T>.safeSubList(from: Int, to: Int): List<T> = when {
    this.size < to -> this
    to < from -> listOf()
    else -> this.subList(from, to)
}

fun List<Int?>?.sum(): Int {
    this?.filterNotNull()?.let { list -> return list.sumOf { it } }
    return 0
}

@OptIn(ExperimentalCoroutinesApi::class)
fun List<WarDetails>.withFullStats(databaseRepository: DatabaseRepositoryInterface, userId: String? = null, teamId: String? = null, is24p: Boolean = false): Flow<Stats> {

    val warScores = mutableListOf<WarScore>()
    val averageForMaps = mutableListOf<TrackStats>()

    val warList = this
        .filter { (userId != null && it.war.hasPlayer(userId)) || userId == null }
        .filter { (teamId != null && it.war.hasTeam(teamId)) || teamId == null }
    warList
        .map { Pair(it, it.warTracks) }
        .forEach {
            var currentPoints = 0
            val is24p = it.first.war.teamOpponent.size > 1
            it.second.forEach { track ->
                val playerScoreForTrack = track.track.positions
                    .singleOrNull { pos -> pos.playerId == userId }
                    ?.position.positionToPoints(is24p)
                var teamScoreForTrack = 0
                track.track.positions.map { it.position.positionToPoints(is24p) }.forEach {
                    teamScoreForTrack += it
                }
                currentPoints += when (userId != null) {
                    true -> playerScoreForTrack
                    else -> teamScoreForTrack
                }
                var shockCount = 0
                track.track.shocks?.filter { userId == null || it.playerId == userId }
                    ?.map { it.count }?.forEach {
                        shockCount += it
                    }
                averageForMaps.add(
                    TrackStats(
                        trackIndex = track.index.map { it.toInt() },
                        teamScore = teamScoreForTrack,
                        playerScore = playerScoreForTrack,
                        shockCount = shockCount
                    )
                )
            }
            warScores.add(WarScore(it.first, currentPoints))
        }

    val maps = when  {
        userId != null && teamId != null -> this.map { WarEntity(it.war) }
            .filter { it.hasTeam(teamId) }
            .filter { it.hasPlayer(userId) }
            .withTrackStats(teamId = teamId, userId = userId)
        userId != null -> this.map { WarEntity(it.war) }
            .filter { it.hasPlayer(userId) }
            .withTrackStats(userId = userId)
        teamId != null -> this.map { WarEntity(it.war) }
            .filter { it.hasTeam(teamId) }
            .withTrackStats(teamId = teamId)
        else -> this.map { WarEntity(it.war) }
            .withTrackStats(userId = userId)

    }

    val flow = when (is24p) {
        false -> {
            val warsPlayed = warList
                .flatMap { it.war.teamOpponent.map { teamId ->
                    Pair(
                        teamId,
                        warList.filter { it.war.teamOpponent.contains(teamId) }
                    )
                } }

            val warsWon = warList
                .filterNot { it.displayedDiff.contains('-') }
                .flatMap { it.war.teamOpponent.map { teamId ->
                    Pair(
                        teamId,
                        warList
                            .filterNot { it.displayedDiff.contains('-') }
                            .filter { it.war.teamOpponent.contains(teamId) }
                    )
                } }

            val warsLost = warList
                .filter { it.displayedDiff.contains('-') }
                .flatMap { it.war.teamOpponent.map { teamId ->
                    Pair(
                        teamId,
                        warList
                            .filter { it.displayedDiff.contains('-') }
                            .filter { it.war.teamOpponent.contains(teamId) }
                    )
                } }


            val mostPlayedTeams = warsPlayed
                .groupBy { it.first }
                .toList()
                .sortedByDescending { it.second.size }
                .distinctBy { it.first }
                .safeSubList(0, 1)

            val mostDefeatedTeams = warsWon
                .toList()
                .sortedByDescending { it.second.size }
                .distinctBy { it.first }
                .safeSubList(0, 1)


            val lessDefeatedTeams = warsLost
                .toList()
                .sortedByDescending { it.second.size }
                .distinctBy { it.first }
                .safeSubList(0, 1)

            flowOf(
                Stats(
                    warStats = WarStats(this),
                    warScores = warScores,
                    maps = maps,
                    averageForMaps = averageForMaps,
                    mostPlayedTeam = null,
                    mostDefeatedTeam = null,
                    lessDefeatedTeam = null
                )
            ).map { stats ->
                val mostPlayedTeam = mostPlayedTeams.map {
                    TeamStats(
                        databaseRepository.getTeam(it.first).firstOrNull(),
                        it.second.size
                    )
                }
                val mostDefeatedTeam = mostDefeatedTeams.map {
                    TeamStats(
                        databaseRepository.getTeam(it.first).firstOrNull(),
                        it.second.size
                    )
                }
                val lessDefeatedTeam = lessDefeatedTeams.map {
                    TeamStats(
                        databaseRepository.getTeam(it.first).firstOrNull(),
                        it.second.size
                    )
                }
                stats.copy(
                    mostPlayedTeam = mostPlayedTeam,
                    mostDefeatedTeam = mostDefeatedTeam,
                    lessDefeatedTeam = lessDefeatedTeam
                )
        }

    }
        else -> {
            val warsWon = warList.filter { it.war.scores.sortedByDescending { it.score }.subList(0, 2).map { it.teamId }.contains(it.war.teamHost) }
            val warsLost = warList.filter { it.war.scores.sortedBy { it.score }.subList(0, 2).map { it.teamId }.contains(it.war.teamHost) }
            val mostPlayedTeams = warList.flatMap { it.war.teamOpponent }.map { id ->  Pair(id, warList.filter { it.war.teamOpponent.contains(id) }) }.sortedByDescending { it.second.size }.distinctBy { it.first }.safeSubList(0, 1)
            val mostDefeatedTeams = warsWon.flatMap { it.war.teamOpponent }.map { id ->  Pair(id, warsWon.filter { it.war.teamOpponent.contains(id) }) }.sortedByDescending { it.second.size }.distinctBy { it.first }.safeSubList(0, 1)
            val lessDefeatedTeams = warsLost.flatMap { it.war.teamOpponent }.map { id ->  Pair(id, warsLost.filter { it.war.teamOpponent.contains(id) }) }.sortedByDescending { it.second.size }.distinctBy { it.first }.safeSubList(0, 1)
            flowOf(
                Stats(
                    warStats = WarStats(this, is24p = true),
                    warScores = warScores,
                    maps = maps,
                    averageForMaps = averageForMaps,
                    mostPlayedTeam = null,
                    mostDefeatedTeam = null,
                    lessDefeatedTeam = null,

                )
            ).map { stats ->
                val mostPlayedTeam = mostPlayedTeams.map {
                    TeamStats(
                        databaseRepository.getTeam(it.first).firstOrNull(),
                        it.second.size
                    )
                }
                val mostDefeatedTeam = mostDefeatedTeams.map {
                    TeamStats(
                        databaseRepository.getTeam(it.first).firstOrNull(),
                        it.second.size
                    )
                }
                val lessDefeatedTeam = lessDefeatedTeams.map {
                    TeamStats(
                        databaseRepository.getTeam(it.first).firstOrNull(),
                        it.second.size
                    )
                }
                stats.copy(
                    mostPlayedTeam = mostPlayedTeam,
                    mostDefeatedTeam = mostDefeatedTeam,
                    lessDefeatedTeam = lessDefeatedTeam
                )
            }
        }
    }
    return flow

}

fun List<TeamEntity>.withFullTeamStats(
    wars: List<WarEntity>,
    databaseRepository: DatabaseRepositoryInterface,
    userId: String? = null
) = flow {
    val temp = mutableListOf<Pair<TeamEntity, Stats>>()
    this@withFullTeamStats.forEach { team ->
        wars
            .filter { it.hasTeam(team.id) }
            .filter { (userId != null && it.hasPlayer(userId)) || userId == null }
            .map { WarDetails(War(it)) }
            .withFullStats(databaseRepository, userId)
            .firstOrNull()
            ?.let {
                if (it.warStats.list.isNotEmpty())
                    temp.add(Pair(team, it))
            }
    }
    emit(temp)
}

fun List<WarEntity>.withTrackStats(userId: String? = null, teamId: String? = null): List<TrackStats> {
    var is24p = false
    return this
        .filter { (teamId != null && it.hasTeam(teamId) || teamId == null) }
        .filter { (userId != null && it.hasPlayer(userId) || userId == null) }
        .flatMap {
            is24p = it.teamOpponent.size > 1
            it.warTracks.orEmpty()
        }
        .groupBy { it.index }.toList()
        .sortedByDescending { it.second.size }
        .mapNotNull {
            var teamScoreForTrack = 0
            var playerScoreForTrack = 0
            var shockCount = 0

            it.second.forEach { track ->
                playerScoreForTrack += track.positions
                    .singleOrNull { pos -> pos.playerId == userId }
                    ?.position.positionToPoints(is24p)
                track.positions.map { it.position.positionToPoints(is24p) }.forEach {
                    teamScoreForTrack += it
                }
                track.shocks?.map { it.count }?.forEach {
                    shockCount += it
                }
            }
            //Map classique trois tours
            it.first.singleOrNull()?.let { index ->

                TrackStats(
                    stats = null,
                    map = listOf(Maps.entries[index.toInt()]),
                    trackIndex = listOf(index.toInt()),
                    totalPlayed = it.second.size,
                    winRate = (it.second.filter { it.diffScore > 0 }.size * 100) / it.second.size,
                    teamScore = teamScoreForTrack / it.second.size,
                    shockCount = shockCount,
                    playerScore = playerScoreForTrack / it.second.size
                )
            } ?: it.first.takeIf { it.size == 2 }?.let { indexes ->
                TrackStats(
                    stats = null,
                    map = indexes.mapNotNull { it.toIntOrNull() }.mapNotNull { Maps.entries.getOrNull(it) },
                    trackIndex = indexes.mapNotNull { it.toIntOrNull() },
                    totalPlayed = it.second.size,
                    winRate = (it.second.filter { it.diffScore > 0 }.size * 100) / it.second.size,
                    teamScore = teamScoreForTrack,
                    shockCount = shockCount,
                    playerScore = playerScoreForTrack / it.second.size
                )
            }
        }

}


