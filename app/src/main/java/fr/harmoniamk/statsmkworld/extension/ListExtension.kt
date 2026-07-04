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

    // Comptage O(n) des adversaires (top 1 par catégorie).
    // 12p : victoire/défaite dérivée du diff de score ; 24p : selon le rang de
    // l'équipe hôte parmi les 3 équipes (top 2 = gagné, bottom 2 = perdu).
    val warsWon = when (is24p) {
        false -> warList.filterNot { it.displayedDiff.contains('-') }
        true -> warList.filter { it.war.scores.sortedByDescending { s -> s.score }.safeSubList(0, 2).map { s -> s.teamId }.contains(it.war.teamHost) }
    }
    val warsLost = when (is24p) {
        false -> warList.filter { it.displayedDiff.contains('-') }
        true -> warList.filter { it.war.scores.sortedBy { s -> s.score }.safeSubList(0, 2).map { s -> s.teamId }.contains(it.war.teamHost) }
    }

    val mostPlayedTeams = warList.topOpponentByCount()
    val mostDefeatedTeams = warsWon.topOpponentByCount()
    val lessDefeatedTeams = warsLost.topOpponentByCount()

    return flowOf(
        Stats(
            warStats = WarStats(this, is24p = is24p),
            warScores = warScores,
            maps = maps,
            averageForMaps = averageForMaps,
            mostPlayedTeam = null,
            mostDefeatedTeam = null,
            lessDefeatedTeam = null
        )
    ).map { stats ->
        // Une seule lecture de la table des équipes, puis indexation en mémoire.
        // On résout un id d'adversaire aussi bien par teamId (wars legacy /
        // normalisées) que par rosterId (granularité roster) → équipe parente.
        val teams = databaseRepository.getTeams().firstOrNull().orEmpty()
        val teamsById = teams.associateBy { it.id }
        val teamByRosterId = teams.flatMap { team -> team.rosters.map { it.id to team } }.toMap()

        fun List<Pair<String, Int>>.toTeamStats() = map { (id, count) ->
            TeamStats(teamsById[id] ?: teamByRosterId[id], count)
        }

        stats.copy(
            mostPlayedTeam = mostPlayedTeams.toTeamStats(),
            mostDefeatedTeam = mostDefeatedTeams.toTeamStats(),
            lessDefeatedTeam = lessDefeatedTeams.toTeamStats()
        )
    }
}

/**
 * Compte le nombre de wars par adversaire et renvoie le plus fréquent (top 1),
 * sous forme de liste de [Pair] (id adversaire, nombre de wars). O(n) au lieu du
 * O(n²) d'un `filter` réévalué par adversaire.
 */
private fun List<WarDetails>.topOpponentByCount(): List<Pair<String, Int>> =
    this.flatMap { it.war.teamOpponent }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .safeSubList(0, 1)
        .map { it.key to it.value }

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


