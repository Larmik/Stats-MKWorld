package fr.harmoniamk.statsmkworld.extension

import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.firebase.Shock
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
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
import kotlin.collections.forEach
import kotlin.collections.get

@Suppress("UNCHECKED_CAST")
fun Any?.toMapList(): List<Map<*, *>>? = this as? List<Map<*, *>>

fun List<Map<*, *>>?.parseTracks(): List<WarTrack>? =
    this?.map { track ->
        WarTrack(
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

fun List<Map<*, *>>?.parsePenalties(): List<WarPenalty>? =
    this?.map { item ->
        WarPenalty(
            teamId = item["teamId"].toString(),
            amount = item["amount"].toString().toInt()
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
fun List<WarDetails>.withFullStats(databaseRepository: DatabaseRepositoryInterface, userId: String? = null): Flow<Stats> {

    val maps = mutableListOf<TrackStats>()
    val warScores = mutableListOf<WarScore>()
    val averageForMaps = mutableListOf<TrackStats>()

    val mostPlayedTeamId = this
        .groupBy { it.war.teamOpponent }
        .toList().maxByOrNull { it.second.size }

    val mostDefeatedTeamId = this
        .filterNot { it.displayedDiff.contains('-') }
        .groupBy { it.war.teamOpponent }
        .toList().maxByOrNull { it.second.size }

    val lessDefeatedTeamId = this
        .filter { it.displayedDiff.contains('-') }
        .groupBy { it.war.teamOpponent }
        .toList().maxByOrNull { it.second.size }

    this.map { Pair(it, it.warTracks) }
        .forEach {
            var currentPoints = 0
            it.second.forEach { track ->
                val playerScoreForTrack = track.track.positions
                    .singleOrNull { pos -> pos.playerId == userId }
                    ?.position.positionToPoints()
                var teamScoreForTrack = 0
                track.track.positions.map { it.position.positionToPoints() }.forEach {
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
                maps.add(
                    TrackStats(
                        trackIndex = track.index,
                        teamScore = teamScoreForTrack,
                        playerScore = playerScoreForTrack,
                        shockCount = shockCount
                    )
                )
            }
            warScores.add(WarScore(it.first, currentPoints))
            currentPoints = 0
        }

    maps.groupBy { it.trackIndex }
        .filter { it.value.isNotEmpty() }
        .forEach { entry ->
            val stats = TrackStats(
                map = Maps.entries[entry.key ?: -1],
                teamScore = (entry.value.map { it.teamScore }
                    .sum() / entry.value.map { it.teamScore }.count()),
                playerScore = (entry.value.map { it.playerScore }
                    .sum() / entry.value.map { it.playerScore }.count()),
                totalPlayed = entry.value.size
            )
            averageForMaps.add(stats)
        }

    return flowOf(
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
        val mostPlayedTeam = databaseRepository.getTeam(mostPlayedTeamId?.first.orEmpty()).firstOrNull()
        val mostDefeatedTeam = databaseRepository.getTeam(mostDefeatedTeamId?.first.orEmpty()).firstOrNull()
        val lessDefeatedTeam = databaseRepository.getTeam(lessDefeatedTeamId?.first.orEmpty()).firstOrNull()
        stats.copy(
            mostPlayedTeam = TeamStats(mostPlayedTeam, mostPlayedTeamId?.second?.size),
            mostDefeatedTeam = TeamStats(mostDefeatedTeam, mostDefeatedTeamId?.second?.size),
            lessDefeatedTeam = TeamStats(lessDefeatedTeam, lessDefeatedTeamId?.second?.size)
        )
    }

}

fun List<TeamEntity>.withFullTeamStats(
    wars: List<WarEntity>,
    databaseRepository: DatabaseRepositoryInterface,
) = flow {
    val temp = mutableListOf<Pair<TeamEntity, Stats>>()
    this@withFullTeamStats.forEach { team ->
        wars
            .filter { it.hasTeam(team.id) }
            .map { WarDetails(War(it)) }
            .withFullStats(databaseRepository)
            .firstOrNull()
            ?.let {
                if (it.warStats.list.isNotEmpty())
                    temp.add(Pair(team, it))
            }
    }
    emit(temp)
}


