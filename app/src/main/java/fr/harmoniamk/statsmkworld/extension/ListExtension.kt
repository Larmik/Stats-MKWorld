package fr.harmoniamk.statsmkworld.extension

import fr.harmoniamk.statsmkworld.database.entities.SeasonEntity
import fr.harmoniamk.statsmkworld.database.entities.TeamEntity
import fr.harmoniamk.statsmkworld.database.entities.WarEntity
import fr.harmoniamk.statsmkworld.model.firebase.Shock
import fr.harmoniamk.statsmkworld.model.firebase.WarPenalty
import fr.harmoniamk.statsmkworld.model.firebase.WarPosition
import fr.harmoniamk.statsmkworld.model.firebase.WarTrack
import fr.harmoniamk.statsmkworld.model.local.Maps
import fr.harmoniamk.statsmkworld.model.local.Stats
import fr.harmoniamk.statsmkworld.model.local.TrackStats
import fr.harmoniamk.statsmkworld.model.local.WarDetails
import fr.harmoniamk.statsmkworld.model.local.WarKindFilter
import fr.harmoniamk.statsmkworld.model.local.WarScore
import fr.harmoniamk.statsmkworld.model.local.WarStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

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

/** Circuit représentant une course : le dernier (arrivée) si intermission (`index` = [intermission, circuit]). */
fun List<Maps>.displayedMap(): Maps? = lastOrNull()

/** Tag affiché sous le nom du circuit (#101) ; aucun pour une course avec intermission. */
fun List<Maps>.displayedTag(): String? = singleOrNull()?.name

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

/**
 * Stats (joueur [userId] / face à [teamId] / équipe) des wars. Calcul pur en mémoire, sans
 * lecture Room : à appeler sous `withContext(Dispatchers.Default)`.
 */
fun List<WarDetails>.withFullStats(userId: String? = null, teamId: String? = null, is24p: Boolean = false): Flow<Stats> {

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

    // Circuits sur `warList` (mêmes filtres joueur/adversaire) sans repasser par `WarEntity`
    // (#90 : la conversion formatait la date de chaque war à chaque appel). Mode de la
    // dernière war conservé à l'identique de `withTrackStats` (B31, #120).
    val maps = trackStatsOf(
        tracks = warList.flatMap { it.war.tracks },
        is24p = warList.lastOrNull()?.war?.teamOpponent?.let { it.size > 1 } == true,
        userId = userId
    )

    return flowOf(
        Stats(
            // WarStats sur la liste FILTRÉE (warList) : V/N/D & nb de wars ne comptent que
            // les wars pertinentes (jouées par le joueur / face à l'adversaire). En vue
            // équipe (userId/teamId null), warList == this → comportement inchangé.
            warStats = WarStats(warList, is24p = is24p),
            warScores = warScores,
            maps = maps,
            averageForMaps = averageForMaps,
            userId = userId
        )
    )
}

/**
 * Total de shocks (éclairs obtenus) sur ces wars : somme des `count` de tous les `Shock`
 * des manches, filtrée sur [playerId] si non-null, sinon toute l'équipe hôte. Base des
 * classements « baggeurs » (#69) — ratio TOTAL/TOTAL, jamais une moyenne par war.
 */
fun List<WarDetails>.totalShocks(playerId: String? = null): Int = sumOf { war ->
    war.war.tracks.sumOf { track ->
        track.shocks
            ?.filter { playerId == null || it.playerId == playerId }
            ?.sumOf { it.count } ?: 0
    }
}

/**
 * Part de shocks d'un joueur en % (ses shocks / total équipe). `null` si l'équipe n'a aucun
 * shock (pas de dénominateur). Règle unique des 4 classements « baggeurs » (#69).
 */
fun List<WarDetails>.shockShare(playerId: String): Double? =
    totalShocks().takeIf { it > 0 }?.let { totalShocks(playerId).percentOf(it) }

/**
 * Parts en % de chaque élément sur [total] (défaut : leur somme), au centième, par la méthode du
 * plus grand reste (Hamilton, 10 000 unités) : des parts couvrant tout le total somment exactement
 * à 100 % (#99). Si [total] dépasse la somme, l'écart compte comme une part implicite, non renvoyée
 * (ex. Top 6 / Bot 6 hors positions 13-24, shocks des alliés). Que des 0.0 si [total] == 0.
 */
fun List<Int>.percentShares(total: Int = sum()): List<Double> {
    if (total <= 0) return map { 0.0 }
    val parts = this + (total - sum()).coerceAtLeast(0)
    val hundredths = parts.map { it * 10_000L / total }
    val leftover = (10_000L - hundredths.sum()).toInt().coerceAtLeast(0)
    // À reste égal, la part implicite (dernière) est servie d'abord : des parts visibles égales restent égales.
    val roundedUpIndexes = parts.indices
        .sortedWith(compareByDescending<Int> { parts[it] * 10_000L % total }.thenByDescending { it == parts.lastIndex })
        .take(leftover)
        .toSet()
    return indices.map { index -> (hundredths[index] + if (index in roundedUpIndexes) 1 else 0) / 100.0 }
}

/**
 * Stats par adversaire : un item par ROSTER (wars où l'opposant = ce rosterId, nom/tag du roster
 * + avatar de l'équipe) puis un item ÉQUIPE pour les wars legacy (opposant = teamId). Seuls les
 * adversaires ayant au moins une war (jouée par [userId] si non-null) sont émis.
 */
fun List<TeamEntity>.withFullTeamStats(
    wars: List<WarDetails>,
    userId: String? = null,
    is24p: Boolean = false
) = flow {
    // Index id d'équipe → wars en une passe (même règle que `War.hasTeam` : hôte ou opposant),
    // au lieu de rescanner toutes les wars pour chaque roster/équipe du cache (#90).
    val warsByTeamId = mutableMapOf<String, MutableList<WarDetails>>()
    wars.filter { userId == null || it.war.hasPlayer(userId) }.forEach { war ->
        (war.war.teamOpponent + war.war.teamHost).toSet().forEach { id ->
            warsByTeamId.getOrPut(id) { mutableListOf() }.add(war)
        }
    }
    val rankingItems = this@withFullTeamStats
        .flatMap { team ->
            team.rosters.mapNotNull { roster ->
                warsByTeamId[roster.id]?.let { team.copy(id = roster.id, name = roster.name, tag = roster.tag) to it }
            } + listOfNotNull(warsByTeamId[team.id]?.let { team to it })
        }
        .map { (display, opponentWars) -> display to opponentWars.withFullStats(userId, is24p = is24p).first() }
    emit(rankingItems)
}

fun List<WarEntity>.withTrackStats(userId: String? = null, teamId: String? = null): List<TrackStats> {
    val wars = this
        .filter { (teamId != null && it.hasTeam(teamId) || teamId == null) }
        .filter { (userId != null && it.hasPlayer(userId) || userId == null) }
    // B31 (#120) : le mode de la DERNIÈRE war s'applique à toutes les manches — conservé tel quel.
    return trackStatsOf(
        tracks = wars.flatMap { it.warTracks.orEmpty() },
        is24p = wars.lastOrNull()?.let { it.teamOpponent.size > 1 } == true,
        userId = userId
    )
}

/**
 * Agrégat par circuit (manches groupées par index), partagé par `withTrackStats` et `withFullStats`.
 * Vue joueur 12p ([userId] non-null) : seules les manches courues par le joueur comptent (#102),
 * pour `totalPlayed`, le winrate et les moyennes ; `averagePosition` = vraie moyenne de ses
 * positions. 24p inchangé (toutes les manches, `teamScore` sommé pour un circuit avec
 * intermission) : reporté au ticket 24p (#31, audit B35).
 */
private fun trackStatsOf(tracks: List<WarTrack>, is24p: Boolean, userId: String?): List<TrackStats> =
    tracks
        .filter { userId == null || is24p || it.hasPlayer(userId) }
        .groupBy { it.index }.toList()
        // Circuit classique (1 index) ou avec intermission (2 index : [intermission, circuit]).
        .filter { (indexes, _) -> indexes.size in 1..2 }
        .sortedByDescending { it.second.size }
        .map { (indexes, tracksOfMap) ->
            val played = tracksOfMap.size
            val playerPositions = tracksOfMap.mapNotNull { track ->
                track.positions.singleOrNull { it.playerId == userId }?.position
            }
            val mapIndexes = indexes.mapNotNull { it.toIntOrNull() }
            TrackStats(
                map = mapIndexes.mapNotNull { Maps.entries.getOrNull(it) },
                trackIndex = mapIndexes,
                totalPlayed = played,
                winRate = tracksOfMap.count { it.diffScore(is24p) > 0 }.percentOf(played),
                teamScore = tracksOfMap.sumOf { track -> track.positions.sumOf { it.position.positionToPoints(is24p) } }
                    .let { total -> if (indexes.size == 2) total else total / played },
                shockCount = tracksOfMap.sumOf { track -> track.shocks?.sumOf { it.count } ?: 0 },
                playerScore = playerPositions.sumOf { it.positionToPoints(is24p) } / played,
                averagePosition = playerPositions.takeIf { it.isNotEmpty() }?.average()
            )
        }

/**
 * Circuits du meilleur au pire « score » (#102), critère aligné sur la valeur affichée : position
 * moyenne croissante en vue joueur ([isIndiv]), score d'équipe moyen décroissant sinon.
 */
fun List<TrackStats>.sortedByTrackScore(isIndiv: Boolean): List<TrackStats> = when (isIndiv) {
    true -> sortedBy { it.averagePosition ?: Double.MAX_VALUE }
    else -> sortedByDescending { it.teamScore ?: 0 }
}

/**
 * Flop 3 d'une liste triée du meilleur au pire, pire en premier, privé des éléments du Top 3
 * (`take(3)`) : un élément n'est jamais dans les deux (#102). Moins de 3 éléments si la liste en
 * compte moins de 6.
 */
fun <T> List<T>.flopExcludingTop(): List<T> = drop(3).takeLast(3).reversed()

/**
 * Filtre les wars sur l'intervalle d'une saison (#70). Rattachement calculé (pas de
 * `seasonId` sur la war) : `war.id` = timestamp epoch ms, une war est dans la saison dont
 * `[start, end]` le contient. [season] `null` = tout l'historique ; `end == null` (saison
 * en cours) → borne haute = maintenant.
 */
fun List<WarEntity>.filterBySeason(season: SeasonEntity?): List<WarEntity> {
    season ?: return this
    val upperBound = season.end ?: System.currentTimeMillis()
    // WarEntity.id est le timestamp (epoch ms) stocké en String → conversion pour comparer.
    return filter { war -> war.id.toLongOrNull()?.let { it in season.start..upperBound } == true }
}

/** Filtre Amicaux / Officiels (#103), appliqué avant tout calcul : amical = sans `tournamentId`. */
fun List<WarEntity>.filterByKind(kind: WarKindFilter): List<WarEntity> =
    filter { war -> if (war.tournamentId == null) kind.friendly else kind.official }
