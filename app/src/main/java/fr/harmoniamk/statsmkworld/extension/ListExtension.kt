package fr.harmoniamk.statsmkworld.extension

import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.firebase.Shock
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
import fr.harmoniamk.statsmkworld.model.firebase.War
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
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

/** Taille de la liste, ou 1 si elle est vide — évite une division par zéro dans les moyennes. */
fun List<*>.sizeOrOne(): Int = size.takeIf { it > 0 } ?: 1

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
            val warIs24p = it.first.war.teamOpponent.size > 1
            it.second.forEach { track ->
                val trackPositions = track.track.positions
                val playerScoreForTrack = trackPositions
                    .singleOrNull { pos -> pos.playerId == userId }
                    ?.position.positionToPoints(warIs24p)
                val teamScoreForTrack = trackPositions.sumOf { it.position.positionToPoints(warIs24p) }
                currentPoints += when (userId != null) {
                    true -> playerScoreForTrack
                    else -> teamScoreForTrack
                }
                val shockCount = track.track.shocks
                    ?.filter { userId == null || it.playerId == userId }
                    ?.sumOf { it.count } ?: 0
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

    return flowOf(
        Stats(
            warStats = WarStats(this, is24p = is24p),
            warScores = warScores,
            maps = maps,
            averageForMaps = averageForMaps,
            userId = userId
        )
    )
}

fun List<TeamEntity>.withFullTeamStats(
    wars: List<WarEntity>,
    databaseRepository: DatabaseRepositoryInterface,
    userId: String? = null,
    is24p: Boolean = false
) = flow {
    val temp = mutableListOf<Pair<TeamEntity, Stats>>()

    // Calcule les stats d'un adversaire pour un identifiant d'opposant donné
    // (rosterId ou teamId legacy). `display` porte la vue affichée dans le
    // classement (id = identifiant d'opposant, nom/tag du roster, avatar équipe).
    suspend fun addRankingItem(display: TeamEntity, opponentId: String) {
        wars
            .filter { it.hasTeam(opponentId) }
            .filter { (userId != null && it.hasPlayer(userId)) || userId == null }
            .map { WarDetails(War(it)) }
            .withFullStats(databaseRepository, userId, is24p = is24p)
            .firstOrNull()
            ?.let {
                if (it.warStats.list.isNotEmpty())
                    temp.add(Pair(display, it))
            }
    }

    this@withFullTeamStats.forEach { team ->
        // Un item par ROSTER : chaque roster produit son propre classement (ses
        // wars où l'opposant = ce rosterId), affiché avec le nom/tag du roster et
        // l'avatar de l'équipe parente. On ne fusionne plus les rosters d'une même
        // équipe sous l'équipe.
        team.rosters.forEach { roster ->
            addRankingItem(
                display = team.copy(id = roster.id, name = roster.name, tag = roster.tag),
                opponentId = roster.id
            )
        }
        // Item de niveau ÉQUIPE pour les wars legacy (opposant = teamId, avant la
        // granularité roster) : elles n'ont pas de rosterId → conservées à part.
        addRankingItem(display = team, opponentId = team.id)
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
                val positions = track.positions
                playerScoreForTrack += positions
                    .singleOrNull { pos -> pos.playerId == userId }
                    ?.position.positionToPoints(is24p)
                teamScoreForTrack += positions.sumOf { it.position.positionToPoints(is24p) }
                shockCount += track.shocks?.sumOf { it.count } ?: 0
            }
            val played = it.second.size
            val wonCount = it.second.count { track -> track.diffScore(is24p) > 0 }
            //Map classique trois tours
            it.first.singleOrNull()?.let { index ->

                TrackStats(
                    stats = null,
                    map = listOf(Maps.entries[index.toInt()]),
                    trackIndex = listOf(index.toInt()),
                    totalPlayed = played,
                    winRate = (wonCount * 100) / played,
                    teamScore = teamScoreForTrack / played,
                    shockCount = shockCount,
                    playerScore = playerScoreForTrack / played
                )
            } ?: it.first.takeIf { it.size == 2 }?.let { indexes ->
                TrackStats(
                    stats = null,
                    map = indexes.mapNotNull { it.toIntOrNull() }.mapNotNull { Maps.entries.getOrNull(it) },
                    trackIndex = indexes.mapNotNull { it.toIntOrNull() },
                    totalPlayed = played,
                    winRate = (wonCount * 100) / played,
                    teamScore = teamScoreForTrack,
                    shockCount = shockCount,
                    playerScore = playerScoreForTrack / played
                )
            }
        }

}


